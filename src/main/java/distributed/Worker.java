package main.java.distributed;

import mpi.MPI;
import mpi.MPIException;

public class Worker {

    public static void execute() throws MPIException {
        int rank = MPI.COMM_WORLD.Rank();
        int masterRank = 0;

        while (true) {
            // Receive control message from master
            byte[] controlMessageBytes = new byte[10]; // Adjust size as needed
            MPI.COMM_WORLD.Recv(controlMessageBytes, 0, controlMessageBytes.length, MPI.BYTE, masterRank, 0);
            String controlMessage = new String(controlMessageBytes).trim();

            if (controlMessage.equals("TERMINATE")) {
                System.out.println("Worker " + rank + " received termination signal. Exiting.");
                break;
            } else if (controlMessage.equals("TASK")) {
                // Receive segment size
                int[] segmentSizeArray = new int[1];
                MPI.COMM_WORLD.Recv(segmentSizeArray, 0, 1, MPI.INT, masterRank, 0);
                int segmentSize = segmentSizeArray[0];

                // Receive segment data
                Solution[] segment = new Solution[segmentSize];
                MPI.COMM_WORLD.Recv(segment, 0, segmentSize, MPI.OBJECT, masterRank, 0);

                // Process each solution in the segment
                for (Solution solution : segment) {
                    if (solution != null) {
                        solution.fitness = solution.calculateFitness();
                    }
                }

                // Send processed segment back to master
                MPI.COMM_WORLD.Send(segment, 0, segmentSize, MPI.OBJECT, masterRank, 0);
            } else {
                System.err.println("Worker " + rank + " received unknown control message: " + controlMessage);
            }
        }
    }
}