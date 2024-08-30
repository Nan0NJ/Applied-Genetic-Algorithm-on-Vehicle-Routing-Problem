package main.java.sequential;

import main.java.main_components.Customer;
import main.java.main_components.Route;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class VRPGeneticAlgorithm { // Implementation of Sequential Genetic Algorithm

    static class Solution {
        /* Purpose:
            - This is an inner class, representing the potential solution for a given VRP,
            consisting of a list of routes and the fitness score.

            Fitness Calculation: The fitness function calculates the total distance of the routes,
            which is directly related to the objective of minimizing travel distance.
        */
        List<Route> routes;
        double fitness;

        public Solution(List<Route> routes) {
            this.routes = routes;
            this.fitness = calculateFitness();
        }

        public double calculateFitness() {
            /*
                To calculate the fitness we sum the distances of all routes in the solution.
                And so the lower the total distance, the better our fitness is.
                So goal being: To minimize the route distance.
            */
            return routes.stream().mapToDouble(Route::calculateDistance).sum();
        }
    }

    public static void executeSequential(List<Customer> customers, int numVehicles, int vehicleCapacity, int populationSize, int generations, double crossoverRate, double mutationRate) {
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
        /* Method: reads the dataset from a CSV file and creates a list of customers.
        Each line in the CSV is expected to contain the coordinates and demand of a customer.
        By parsing through the data and removing the commas we get the id, (x,y) coordinates and the demand.
        Thus, we create our Customer
        */
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
        /*
            Implementation of the Basic Genetic Algorithm
            with having:
            --> Init population
            --> for gen = 0 to finish do
                --> selection
                --> crossover
                --> mutation
        */
        // Generate initial population ~ we create an init solution, with each consisting of a randomly generated route.
        List<Solution> population = generateInitialPopulation(customers, depot, numVehicles, vehicleCapacity, populationSize);

        for (int gen = 0; gen < generations; gen++) {
            // Selection
            List<Solution> selectedSolutions = selection(population);

            // Crossover
            List<Solution> offspring = crossover(selectedSolutions, crossoverRate, depot, numVehicles, vehicleCapacity);

            // Mutation
            mutate(offspring, mutationRate, depot, numVehicles, vehicleCapacity);

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
        /*
            Method: We create an init solution with randomized customer assignments.
            And add the solution to our population.
        */
        List<Solution> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<Route> routes = createInitialSolution(customers, depot, numVehicles, vehicleCapacity);
            population.add(new Solution(routes));
        }
        return population;
    }

    public static List<Route> createInitialSolution(List<Customer> customers, Customer depot, int numVehicles, int vehicleCapacity) {
        /*
            -> Here we initialize the routes for each vehicle.
            Then shuffle the customer list to create a random assignment.
            And we always try to put the customer to the first available route.
            To finalize we return each route to the depot and give the generated routes for this solution.
        */
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

    public static List<Solution> selection(List<Solution> population) {
        // Roulette Wheel Selection - classical approach
        /*
            Ensuring that fitter solutions have a higher chance of being selected for reproduction,
            aligning with the survival of the fittest principle.

            Now here because of the fitness we:
                -> Inverse the function (because goal is minimize).
                -> Then we start with the Roulette process for each individual.
                -> We accumulate probabilities until a selected solution is found.
                -> with that being what we give back.
        */
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

    public static List<Solution> crossover(List<Solution> selectedSolutions, double crossoverRate, Customer depot, int numVehicles, int vehicleCapacity) {
        /*
            The crossover method combines pairs of selected solutions to produce offspring.
            It uses a one-point crossover to exchange routes between parent solutions.

             Now here we start of by iterating over pairs of selected solutions to create the offspring.
             And Perform the crossover.
        */
        Random rand = new Random();
        List<Solution> offspring = new ArrayList<>();

        for (int i = 0; i < selectedSolutions.size(); i += 2) {
            if (i + 1 < selectedSolutions.size() && rand.nextDouble() < crossoverRate) {
                // Perform crossover if the random number is less than the crossover rate.
                Solution parent1 = selectedSolutions.get(i);
                Solution parent2 = selectedSolutions.get(i + 1);

                // One-point crossover: exchange routes between the two parents.
                int crossoverPoint = rand.nextInt(numVehicles);

                List<Route> child1Routes = new ArrayList<>(parent1.routes.subList(0, crossoverPoint));
                child1Routes.addAll(parent2.routes.subList(crossoverPoint, numVehicles));

                List<Route> child2Routes = new ArrayList<>(parent2.routes.subList(0, crossoverPoint));
                child2Routes.addAll(parent1.routes.subList(crossoverPoint, numVehicles));
                // And so we add the new offsprings
                offspring.add(new Solution(child1Routes));
                offspring.add(new Solution(child2Routes));
            } else {
                // If no crossover occurs, add the parents directly to the next generation. ( To have no fluctuations)
                offspring.add(selectedSolutions.get(i));
                if (i + 1 < selectedSolutions.size()) {
                    offspring.add(selectedSolutions.get(i + 1));
                }
            }
        }

        return offspring;
    }

    public static void mutate(List<Solution> offspring, double mutationRate, Customer depot, int numVehicles, int vehicleCapacity) {
        /*
            The mutate method applies random changes to the offspring solutions.
            Mutation introduces variability, which is crucial for avoiding local optima in the search space.

            This happens by first iterating over the offspring, and perform mutation:
            If the random number is less than the mutation rate.
            If so we give a random route!
            Finally, when this is done we recalculate the solutions.
        */
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