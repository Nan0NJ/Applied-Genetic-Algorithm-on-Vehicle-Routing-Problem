package main.java.main_components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Route implements Serializable {
    /* Purpose:
        - The class models a single vehicle route, by
        keeping track of the customer it serves,
        the total distance traveled, and
        the capacity of the vehicle.
    */
    private static final long serialVersionUID = 1L; // Used for versioning

    public List<Customer> customers;
    public int capacity;
    public double totalDistance;

    public Route(int capacity) {
        this.customers = new ArrayList<>();
        this.capacity = capacity;
        this.totalDistance = 0;
    }

    public void addCustomer(Customer customer) {
        /*
            Adding a customer routes if the total demand doesn't exceed the vehicle cap.
            The distance is updated based on the addition of the new customer ( so it is affected).
            This ensures that the route is optimized in terms of distance.
        */
        if (currentLoad() + customer.demand <= capacity) {
            if (!customers.isEmpty()) {
                totalDistance += customers.get(customers.size() - 1).distanceTo(customer);
            }
            customers.add(customer);
        }
    }

    public int currentLoad() {
        /*
            Calculating the current load of the vehicle by summing up the demands of all customer in the route.
            This helps in deciding whether a new customer can be added without violating the capacity constraints.
        */
        return customers.stream().mapToInt(c -> c.demand).sum();
    }

    public double calculateDistance() {
        /*
            Calculating the total distance of the route by summing up the distances between consecutive customers.
            This calculation is vital for evaluating the fitness of a solution.
        */
        double distance = 0;
        for (int i = 0; i < customers.size() - 1; i++) {
            distance += customers.get(i).distanceTo(customers.get(i + 1));
        }
        return distance;
    }
}