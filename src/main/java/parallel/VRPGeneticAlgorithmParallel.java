package main.java.parallel;

import main.java.main_components.Customer;
import main.java.main_components.Route;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class VRPGeneticAlgorithmParallel {
    /*
        Here we utilize the Fork/Join framework with Recursive tasks.
        So we utilize the divide-and-conquer-algorithms and split a given load in smaller chunks to the processes.
        With such a simple usage we minimize complexity, traditional concurrency control problems and synchronization.
        By internally managing the tasks we give scalable batches to each process and ensure a fully utilized performance from each core.
        This makes it a robust and efficient solution for solving large-scale VRP problems.
    */

    private static final ForkJoinPool forkJoinPool = new ForkJoinPool();

    static class Solution {
        List<Route> routes;
        double fitness;

        public Solution(List<Route> routes) {
            this.routes = routes;
            this.fitness = Parallel.calculateFitness(routes);
        }
    }

    public static void executeParallel(List<Customer> customers, int numVehicles, int vehicleCapacity, int populationSize, int generations, double crossoverRate, double mutationRate) {
        long startTime = System.currentTimeMillis();

        // Initialize depot (assuming first line is the depot)
        Customer depot = customers.remove(0);

        // Run the genetic algorithm
        Solution bestSolution = geneticAlgorithm(customers, depot, numVehicles, vehicleCapacity, populationSize, generations, crossoverRate, mutationRate);

        long endTime = System.currentTimeMillis();
        System.out.println("Best Solution Fitness: " + bestSolution.fitness);
        System.out.println("Total execution time: " + (endTime - startTime) + " ms");
    }

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

    public static Solution geneticAlgorithm(List<Customer> customers, Customer depot, int numVehicles, int vehicleCapacity, int populationSize, int generations, double crossoverRate, double mutationRate) {
        // Generate initial population
        List<Solution> population = generateInitialPopulation(customers, depot, numVehicles, vehicleCapacity, populationSize);

        for (int gen = 0; gen < generations; gen++) {
            double totalFitness = population.stream().mapToDouble(s -> 1 / s.fitness).sum();

            // Parallel Selection
            List<Solution> selectedSolutions = Parallel.selection(population, totalFitness);

            // Parallel Crossover
            List<Solution> offspring = Parallel.crossover(selectedSolutions, crossoverRate, depot, numVehicles, vehicleCapacity);

            // Parallel Mutation
            Parallel.mutate(offspring, mutationRate, depot, numVehicles, vehicleCapacity);

            // Replace population with offspring
            population = offspring;

            // Find the best solution in the current population
            Solution bestSolution = population.stream().min((s1, s2) -> Double.compare(s1.fitness, s2.fitness)).orElse(null);
            System.out.println("Generation " + gen + " - Best Fitness: " + bestSolution.fitness);
        }

        // Return the best solution found
        return population.stream().min((s1, s2) -> Double.compare(s1.fitness, s2.fitness)).orElse(null);
    }

    public static List<Solution> generateInitialPopulation(List<Customer> customers, Customer depot, int numVehicles, int vehicleCapacity, int populationSize) {
        List<Solution> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<Route> routes = createInitialSolution(customers, depot, numVehicles, vehicleCapacity);
            population.add(new Solution(routes));
        }
        return population;
    }

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

    static class Parallel {

        // the fitness calculation being the most time expensive task we reduce this accordingly with ForkJoinPool
        public static double calculateFitness(List<Route> routes) {
            return forkJoinPool.invoke(new FitnessTask(routes, 0, routes.size()));
        }

        public static List<Solution> selection(List<Solution> population, double totalFitness) {
            return forkJoinPool.invoke(new SelectionTask(population, totalFitness, 0, population.size()));
        }

        public static List<Solution> crossover(List<Solution> selectedSolutions, double crossoverRate, Customer depot, int numVehicles, int vehicleCapacity) {
            return forkJoinPool.invoke(new CrossoverTask(selectedSolutions, crossoverRate, depot, numVehicles, vehicleCapacity, 0, selectedSolutions.size()));
        }

        public static void mutate(List<Solution> offspring, double mutationRate, Customer depot, int numVehicles, int vehicleCapacity) {
            forkJoinPool.invoke(new MutationTask(offspring, mutationRate, depot, numVehicles, vehicleCapacity, 0, offspring.size()));
        }

        static class FitnessTask extends RecursiveTask<Double> {
            /*
                The FitnessTask splits the number of routes recursively until they are small enough for the processes to handle.
                That being the THRESHOLD.

                This reduces the overall time required to compute the fitness of the entire population,
                especially when the number of routes or population size is large.
            */
            private static final int THRESHOLD = 10;
            private final List<Route> routes;
            private final int start;
            private final int end;

            public FitnessTask(List<Route> routes, int start, int end) {
                this.routes = routes;
                this.start = start;
                this.end = end;
            }

            @Override
            protected Double compute() {
                if (end - start <= THRESHOLD) {
                    // Sequential cacl when the task is small enough
                    double totalFitness = 0;
                    for (int i = start; i < end; i++) {
                        totalFitness += routes.get(i).calculateDistance();
                    }
                    return totalFitness;
                } else {
                    // if it's not we split it to two smaller tasks
                    int mid = start + (end - start) / 2;
                    FitnessTask leftTask = new FitnessTask(routes, start, mid);
                    FitnessTask rightTask = new FitnessTask(routes, mid, end);
                    leftTask.fork(); // So we execute the task asynchronously
                    double rightResult = rightTask.compute(); // Execute the right task synchronously
                    double leftResult = leftTask.join(); // Wait until the left task is finished
                    return leftResult + rightResult; // and finally combine them to get RESULT
                }
            }
        }

        static class SelectionTask extends RecursiveTask<List<Solution>> {
            /*
                Applies the same recursive approach as in the FitnessTask,
                because here we utilize on splitting the process for selecting individuals to crossover.

                By parallelizing the selection process, multiple segments of the population can be processed concurrently.
                This speeds up the selection process, especially when the population size is large.
            */
            private static final int THRESHOLD = 10;
            private final List<Solution> population;
            private final double totalFitness;
            private final int start;
            private final int end;

            public SelectionTask(List<Solution> population, double totalFitness, int start, int end) {
                this.population = population;
                this.totalFitness = totalFitness;
                this.start = start;
                this.end = end;
            }

            @Override
            protected List<Solution> compute() {
                if (end - start <= THRESHOLD) {
                    List<Solution> selectedSolutions = new ArrayList<>();
                    Random rand = new Random();

                    for (int i = start; i < end; i++) {
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
                } else {
                    int mid = start + (end - start) / 2;
                    SelectionTask leftTask = new SelectionTask(population, totalFitness, start, mid);
                    SelectionTask rightTask = new SelectionTask(population, totalFitness, mid, end);
                    leftTask.fork();
                    List<Solution> rightResult = rightTask.compute();
                    List<Solution> leftResult = leftTask.join();
                    leftResult.addAll(rightResult);
                    return leftResult;
                }
            }
        }

        static class CrossoverTask extends RecursiveTask<List<Solution>> {
            /*
                The creation of offspring is distributed across multiple threads,
                which reduces the time required to generate the next generation.
            */
            private static final int THRESHOLD = 10;
            private final List<Solution> selectedSolutions;
            private final double crossoverRate;
            private final Customer depot;
            private final int numVehicles;
            private final int vehicleCapacity;
            private final int start;
            private final int end;

            public CrossoverTask(List<Solution> selectedSolutions, double crossoverRate, Customer depot, int numVehicles, int vehicleCapacity, int start, int end) {
                this.selectedSolutions = selectedSolutions;
                this.crossoverRate = crossoverRate;
                this.depot = depot;
                this.numVehicles = numVehicles;
                this.vehicleCapacity = vehicleCapacity;
                this.start = start;
                this.end = end;
            }

            @Override
            protected List<Solution> compute() {
                if (end - start <= THRESHOLD) {
                    List<Solution> offspring = new ArrayList<>();
                    Random rand = new Random();

                    for (int i = start; i < end; i += 2) {
                        if (i + 1 < selectedSolutions.size() && rand.nextDouble() < crossoverRate) {
                            Solution parent1 = selectedSolutions.get(i);
                            Solution parent2 = selectedSolutions.get(i + 1);

                            int crossoverPoint = rand.nextInt(numVehicles);

                            List<Route> child1Routes = new ArrayList<>(parent1.routes.subList(0, crossoverPoint));
                            child1Routes.addAll(parent2.routes.subList(crossoverPoint, numVehicles));

                            List<Route> child2Routes = new ArrayList<>(parent2.routes.subList(0, crossoverPoint));
                            child2Routes.addAll(parent1.routes.subList(crossoverPoint, numVehicles));

                            offspring.add(new Solution(child1Routes));
                            offspring.add(new Solution(child2Routes));
                        } else {
                            offspring.add(selectedSolutions.get(i));
                            if (i + 1 < selectedSolutions.size()) {
                                offspring.add(selectedSolutions.get(i + 1));
                            }
                        }
                    }

                    return offspring;
                } else {
                    int mid = start + (end - start) / 2;
                    CrossoverTask leftTask = new CrossoverTask(selectedSolutions, crossoverRate, depot, numVehicles, vehicleCapacity, start, mid);
                    CrossoverTask rightTask = new CrossoverTask(selectedSolutions, crossoverRate, depot, numVehicles, vehicleCapacity, mid, end);
                    leftTask.fork();
                    List<Solution> rightResult = rightTask.compute();
                    List<Solution> leftResult = leftTask.join();
                    leftResult.addAll(rightResult);
                    return leftResult;
                }
            }
        }

        static class MutationTask extends RecursiveTask<Void> {
            /*
                Mutation introduces variability into the population, and
                by parallelizing it, we can efficiently apply mutations to a large set of offspring.
            */
            private static final int THRESHOLD = 10;
            private final List<Solution> offspring;
            private final double mutationRate;
            private final Customer depot;
            private final int numVehicles;
            private final int vehicleCapacity;
            private final int start;
            private final int end;

            public MutationTask(List<Solution> offspring, double mutationRate, Customer depot, int numVehicles, int vehicleCapacity, int start, int end) {
                this.offspring = offspring;
                this.mutationRate = mutationRate;
                this.depot = depot;
                this.numVehicles = numVehicles;
                this.vehicleCapacity = vehicleCapacity;
                this.start = start;
                this.end = end;
            }

            @Override
            protected Void compute() {
                if (end - start <= THRESHOLD) {
                    Random rand = new Random();

                    for (int i = start; i < end; i++) {
                        Solution solution = offspring.get(i);
                        if (rand.nextDouble() < mutationRate) {
                            int routeIndex = rand.nextInt(numVehicles);
                            Route route = solution.routes.get(routeIndex);

                            if (route.customers.size() > 1) {
                                int swapIndex1 = rand.nextInt(route.customers.size());
                                int swapIndex2 = rand.nextInt(route.customers.size());

                                Customer temp = route.customers.get(swapIndex1);
                                route.customers.set(swapIndex1, route.customers.get(swapIndex2));
                                route.customers.set(swapIndex2, temp);
                            }

                            solution.fitness = Parallel.calculateFitness(solution.routes);
                        }
                    }

                    return null;
                } else {
                    int mid = start + (end - start) / 2;
                    MutationTask leftTask = new MutationTask(offspring, mutationRate, depot, numVehicles, vehicleCapacity, start, mid);
                    MutationTask rightTask = new MutationTask(offspring, mutationRate, depot, numVehicles, vehicleCapacity, mid, end);
                    leftTask.fork();
                    rightTask.compute();
                    leftTask.join();
                    return null;
                }
            }
        }
    }
}
