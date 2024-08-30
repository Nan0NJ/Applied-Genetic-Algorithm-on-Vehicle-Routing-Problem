package main.java.main;
/*
    Project: Using Genetic Algorithm on Vehicle Routing Problem (VRP)
*/
import main.java.main_components.Customer;
import main.java.parallel.VRPGeneticAlgorithmParallel;
import main.java.sequential.VRPGeneticAlgorithm;
import main.java.distributed.Master;

import java.util.List;

public class MainExecution {
/* Class Purpose:
    - Serves as the entry point for the application, with determining how the genetic
    algorithm for solving the VRP will be executed - either sequentially, in parallel or distributed across multiple machines.
*/
    public static void main(String[] args) {
        /*
            We initialize by setting the parameters given below (default values for handling are estimated).

        */
        String basePath = "data/";
        String fileName = "vrp_very_huge_dataset_100000.csv"; // Default dataset CHANGE TO CONVERGE
        String executionType = "sequential"; // Default execution type "sequential" OR "parallel" OR "distributed"
        int populationSize = 100; // Default population size CHANGE TO CONVERGE
        int generations = 100; // Default number of generations
        double crossoverRate = 0.7; // Default crossover rate
        double mutationRate = 0.05; // Default mutation rate
        int numVehicles = 50; // Default number of vehicles
        int vehicleCapacity = 100; // Default vehicle capacity

        // Override default values with provided arguments ~ helpful when use of testing configurations
        if (args.length > 0) executionType = args[0];
        if (args.length > 1) fileName = args[1];
        if (args.length > 2) populationSize = Integer.parseInt(args[2]);
        if (args.length > 3) generations = Integer.parseInt(args[3]);
        if (args.length > 4) crossoverRate = Double.parseDouble(args[4]);
        if (args.length > 5) mutationRate = Double.parseDouble(args[5]);
        if (args.length > 6) numVehicles = Integer.parseInt(args[6]);
        if (args.length > 7) vehicleCapacity = Integer.parseInt(args[7]);

        // Full path to the dataset file
        String filePath = basePath + fileName;
        System.out.println("Dataset path: " + filePath);

        // Execute based on the specified execution type
        if (executionType.equalsIgnoreCase("parallel")) {
            System.out.println("Executing Parallel Genetic Algorithm");
            List<Customer> customers = VRPGeneticAlgorithmParallel.readDataset(filePath);
            VRPGeneticAlgorithmParallel.executeParallel(customers, numVehicles, vehicleCapacity, populationSize, generations, crossoverRate, mutationRate);
        } else if (executionType.equalsIgnoreCase("distributed")) {
            System.out.println("Executing Distributed Genetic Algorithm");
            Master.main(args); // Start the distributed execution
        } else {
            System.out.println("Executing Sequential Genetic Algorithm");
            List<Customer> customers = VRPGeneticAlgorithm.readDataset(filePath);
            VRPGeneticAlgorithm.executeSequential(customers, numVehicles, vehicleCapacity, populationSize, generations, crossoverRate, mutationRate);
        }
    }
}
