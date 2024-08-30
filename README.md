# Genetic Algorithm Applied to the Vehicle Routing Problem (VRP)

This repository contains the implementation of a Genetic Algorithm (GA) designed to solve the Vehicle Routing Problem (VRP). The project is implemented in three modes: sequential, parallel, and distributed, allowing for performance comparisons and scalability testing across different computational environments.

## Table of Contents

- [Introduction](#introduction)
- [Problem Description](#problem-description)
- [Project Structure](#project-structure)
- [Setup and Installation](#setup-and-installation)
- [Running the Project](#running-the-project)
  - [Sequential Implementation](#sequential-implementation)
  - [Parallel Implementation](#parallel-implementation)
  - [Distributed Implementation](#distributed-implementation)
- [Results](#results)
- [Performance Analysis](#performance-analysis)
- [Conclusion](#conclusion)

## Introduction

The Vehicle Routing Problem (VRP) is a complex combinatorial optimization problem commonly encountered in logistics and transportation. This project explores the application of a Genetic Algorithm (GA) to solve the VRP, with implementations in sequential, parallel, and distributed modes.

## Problem Description

The goal of this project is to minimize the total distance traveled by a fleet of vehicles tasked with delivering goods to a set of customers, subject to constraints such as vehicle capacity and customer demand. Due to the NP-hard nature of the VRP, exact solutions are computationally infeasible for large instances, making heuristic methods like GAs particularly suitable.

## Project Structure

├── src/ │ ├── main/ │ │ ├── java/ │ │ │ ├── distributed/ │ │ │ │ ├── Master.java │ │ │ │ ├── Worker.java │ │ │ ├── main_components/ │ │ ├── resources/ │ └── test/ ├── out/ │ ├── production/ │ │ ├── VRP-89221061/ │ ├── test/ ├── DataGenerator.py ├── README.md └── HowToDistribute.txt


## Setup and Installation

### Prerequisites

- **Java**: Ensure that the JAVA_HOME environment variable is set correctly.
- **MPJ Express**: Install MPJ Express and set the MPJ_HOME environment variable.

### Installation Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Nan0NJ/Applied-Genetic-Algorithm-on-Vehicle-Routing-Problem.git
   cd Applied-Genetic-Algorithm-on-Vehicle-Routing-Problem
   ```
2. Set up the environment:
   - Set MPJ_HOME and JAVA_HOME environment variables.
   - Edit the Path environment variable to include %MPJ_HOME%\bin and %JAVA_HOME%\bin.
3. Build the project:
   ```bash
   javac -cp "%MPJ_HOME%/lib/mpj.jar;src" -d out/production/VRP-89221061 src/main/java/distributed/Master.java src/main/java/distributed/Worker.java src/main/java/main_components/*.java
   jar cvf VRP-89221061.jar -C out/production/VRP-89221061 .
   ```
## Running the Project
(You can watch the attached video: Distributing.mp4)
1. Prepare the master and worker machines. Ensure MPJ Express is set up and running on all machines.
2. Create a machines.txt file listing the IP addresses of all worker machines.
3. Run the master node:
   ```bash
   mpjrun.bat -np 2 -dev niodev -machinesfile machines.txt -cp "C:\mpj\lib\mpj.jar;C:\VRP-89221061\out\production\VRP-89221061" main.java.distributed.Master
   ```
For more details on setting up refer to the HowToDistribute.txt file.

## Results
The results from the sequential, parallel, and distributed implementations demonstrate significant performance improvements with parallelization and distribution, particularly with larger datasets. Detailed performance metrics can be found in the Examination.txt file in the repository.

## Performance Analysis
### 1. Sequential Implementation: Serves as a baseline and performs well with small datasets.
### 2. Parallel Implementation: Provides substantial performance improvements by utilizing multiple cores.
### 3. Distributed Implementation: Offers the best performance with large datasets, despite communication overhead between nodes.

## Conclusion
This project showcases the application of a Genetic Algorithm to solve the VRP using different computational approaches. The distributed implementation proves to be the most efficient for large-scale problems, highlighting the importance of selecting the appropriate computational strategy based on the problem's scale.
