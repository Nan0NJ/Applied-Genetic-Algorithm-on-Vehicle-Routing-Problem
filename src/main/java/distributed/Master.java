package main.java.distributed;

import main.java.main_components.Customer;
import main.java.main_components.Route;
import mpi.MPI;
import mpi.MPIException;

import java.io.*;
import java.util.*;

//      .\mpjrun.bat -np 5 -classpath C:\Users\devne\OneDrive\Desktop\VRP-89221061\out\production\VRP-89221061 main.java.distributed.Master
public class Master {

    public static void main(String[] args) throws MPIException {
        // Initialize the MPI environment
        MPI.Init(args);

        // Get the number of processes (workers + master)
        int size = MPI.COMM_WORLD.Size();

        // Get the rank (ID) of this process
        int rank = MPI.COMM_WORLD.Rank();

        // Initialize machine list from args
        List<String> machineList = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            machineList.add(args[i]);
        }

        if (machineList.isEmpty()) {
            System.err.println("Error: Machine list is empty!");
            MPI.Finalize();
            return;
        }

        if (rank == 0) {
            // Master process: responsible for managing the overall process

            // Initialize the genetic algorithm parameters
            int populationSize = 1000;
            int generations = 100;
            double crossoverRate = 0.7;
            double mutationRate = 0.05;
            int numVehicles = 10;
            int vehicleCapacity = 100;

            // Read the dataset and initialize the customers and depot
            String basePath = "data/";
            String fileName = "vrp_very_very_large_dataset_15000.csv"; // Default dataset CHANGE TO CONVERGE
            String filePath = basePath + fileName;
            System.out.println(filePath + "GIVEN");
            List<Customer> customers = readDataset(filePath);
            System.out.println("Running on master node");

            long startTime = System.currentTimeMillis();

            // Initialize depot (assuming first line in the dataset is the depot)
            Customer depot = customers.remove(0); // Remove depot from the customer list

            // Generate initial population
            List<Solution> population = generateInitialPopulation(customers, depot, numVehicles, vehicleCapacity, populationSize);
            System.out.println("Num of GEN "+ generations);
            for (int gen = 0; gen < generations; gen++) {
                System.out.println("Current gen: "+gen);
                // Step 1: Selection - select solutions for crossover
                List<Solution> selectedSolutions = selection(population);
                System.out.println("SELECTED SOLUTIONS:"+ selectedSolutions.toArray().length);

                // Step 2: Crossover - combine selected solutions to create offspring
                List<Solution> offspring = crossover(selectedSolutions, crossoverRate, depot, numVehicles, vehicleCapacity);
                System.out.println("OFFSPRING SOLUTIONS:"+offspring.toArray().length);

                // Step 3: Mutation - introduce mutations into the offspring
                mutate(offspring, mutationRate, depot, numVehicles, vehicleCapacity);
                System.out.println("MUTATE SOLUTIONS:"+offspring.toArray().length);

                // Distribute tasks to workers
                int totalWorkers = size - 1;
                int totalOffspring = offspring.size();
                int baseSegmentSize = totalOffspring / totalWorkers;
                int remainder = totalOffspring % totalWorkers;

                int offset = 0;
                for (int i = 1; i <= totalWorkers; i++) {
                    int currentSegmentSize = baseSegmentSize + (i <= remainder ? 1 : 0);

                    // Prepare segment
                    List<Solution> segmentList = offspring.subList(offset, offset + currentSegmentSize);
                    Solution[] segment = segmentList.toArray(new Solution[currentSegmentSize]);
                    offset += currentSegmentSize;

                    // Send control message
                    String controlMessage = "TASK";
                    MPI.COMM_WORLD.Send(controlMessage.getBytes(), 0, controlMessage.length(), MPI.BYTE, i, 0);

                    // Send segment size
                    MPI.COMM_WORLD.Send(new int[]{currentSegmentSize}, 0, 1, MPI.INT, i, 0);

                    // Send segment data
                    MPI.COMM_WORLD.Send(segment, 0, currentSegmentSize, MPI.OBJECT, i, 0);
                }

                // Collect results from workers
                offset = 0;
                for (int i = 1; i <= totalWorkers; i++) {
                    int currentSegmentSize = baseSegmentSize + (i <= remainder ? 1 : 0);
                    Solution[] resultSegment = new Solution[currentSegmentSize];

                    MPI.COMM_WORLD.Recv(resultSegment, 0, currentSegmentSize, MPI.OBJECT, i, 0);

                    // Update offspring with results
                    for (int j = 0; j < currentSegmentSize; j++) {
                        offspring.set(offset + j, resultSegment[j]);
                    }
                    offset += currentSegmentSize;
                }

                // Replace old population with new offspring
                population = offspring;

                // Find and print the best solution in the current generation
                Solution bestSolution = Collections.min(population, Comparator.comparingDouble(s -> s.fitness));
                System.out.println("Best Fitness in Generation " + gen + ": " + bestSolution.fitness);
            }

            // Send termination signal to workers
            for (int i = 1; i < size; i++) {
                String controlMessage = "TERMINATE";
                MPI.COMM_WORLD.Send(controlMessage.getBytes(), 0, controlMessage.length(), MPI.BYTE, i, 0);
            }

            System.out.println("Genetic Algorithm completed.");

            long endTime = System.currentTimeMillis();
            System.out.println("Total execution time: " + (endTime - startTime) + " ms");
        } else {
            // Worker process
            System.out.println("Running on worker node: " + machineList.get(rank - 1));
            Worker.execute();
        }
        // Finalize the MPI environment
        MPI.Finalize();
    }

    // Method to read dataset from file (actual logic from your provided code)
    public static List<Customer> readDataset(String filePath) {
        List<Customer> customers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 0;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int demand = Integer.parseInt(parts[3]);

                customers.add(new Customer(id++, x, y, demand));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return customers;
    }

    // Generate the initial population
    public static List<Solution> generateInitialPopulation(List<Customer> customers, Customer depot, int numVehicles, int vehicleCapacity, int populationSize) {
        List<Solution> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<Route> routes = createInitialSolution(customers, depot, numVehicles, vehicleCapacity);
            population.add(new Solution(routes));
        }
        return population;
    }

    // Create an initial solution for the population
    public static List<Route> createInitialSolution(List<Customer> customers, Customer depot, int numVehicles, int vehicleCapacity) {
        List<Route> routes = new ArrayList<>();
        for (int i = 0; i < numVehicles; i++) {
            routes.add(new Route(vehicleCapacity));
        }

        Collections.shuffle(customers);  // Randomize for a different starting solution
        for (Customer customer : customers) {
            for (Route route : routes) {
                if (route.currentLoad() + customer.demand <= route.capacity) {
                    route.addCustomer(customer);
                    break;
                }
            }
        }

        // Return to depot (close the loop)
        for (Route route : routes) {
            if (!route.customers.isEmpty()) {
                route.totalDistance += route.customers.get(route.customers.size() - 1).distanceTo(depot);
            }
        }

        return routes;
    }

    // Selection process
    public static List<Solution> selection(List<Solution> population) {
        double totalFitness = population.stream().mapToDouble(s -> 1 / s.fitness).sum();
        List<Solution> selectedSolutions = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < population.size(); i++) {
            double randNum = rand.nextDouble() * totalFitness;
            double runningSum = 0;

            for (Solution solution : population) {
                runningSum += 1 / solution.fitness;
                if (runningSum >= randNum) {
                    selectedSolutions.add(solution);
                    break;
                }
            }
        }

        return selectedSolutions;
    }

    // Crossover process
    public static List<Solution> crossover(List<Solution> selectedSolutions, double crossoverRate, Customer depot, int numVehicles, int vehicleCapacity) {
        Random rand = new Random();
        List<Solution> offspring = new ArrayList<>();

        for (int i = 0; i < selectedSolutions.size(); i += 2) {
            if (i + 1 < selectedSolutions.size() && rand.nextDouble() < crossoverRate) {
                // Perform crossover
                Solution parent1 = selectedSolutions.get(i);
                Solution parent2 = selectedSolutions.get(i + 1);

                // Simple one-point crossover
                int crossoverPoint = rand.nextInt(numVehicles);

                List<Route> child1Routes = new ArrayList<>(parent1.routes.subList(0, crossoverPoint));
                child1Routes.addAll(parent2.routes.subList(crossoverPoint, numVehicles));

                List<Route> child2Routes = new ArrayList<>(parent2.routes.subList(0, crossoverPoint));
                child2Routes.addAll(parent1.routes.subList(crossoverPoint, numVehicles));

                offspring.add(new Solution(child1Routes));
                offspring.add(new Solution(child2Routes));
            } else {
                // No crossover, just pass the parents to the next generation
                offspring.add(selectedSolutions.get(i));
                if (i + 1 < selectedSolutions.size()) {
                    offspring.add(selectedSolutions.get(i + 1));
                }
            }
        }

        return offspring;
    }

    // Mutation process
    public static void mutate(List<Solution> offspring, double mutationRate, Customer depot, int numVehicles, int vehicleCapacity) {
        Random rand = new Random();

        for (Solution solution : offspring) {
            if (rand.nextDouble() < mutationRate) {
                // Perform mutation - Swap two customers in a random route
                int routeIndex = rand.nextInt(numVehicles);
                Route route = solution.routes.get(routeIndex);

                if (route.customers.size() > 1) {
                    int swapIndex1 = rand.nextInt(route.customers.size());
                    int swapIndex2 = rand.nextInt(route.customers.size());

                    Customer temp = route.customers.get(swapIndex1);
                    route.customers.set(swapIndex1, route.customers.get(swapIndex2));
                    route.customers.set(swapIndex2, temp);
                }

                // Recalculate the fitness after mutation
                solution.fitness = solution.calculateFitness();
            }
        }
    }
}