package main.java.main_components;

import java.io.Serializable;
public class Customer implements  Serializable{
    /* Purpose:
        - The class represents a customer in a VRP.
        Each customer has
        an identifier (ID),
        with given tuple coordinates (x,y),
        on a given demand.
    */
    private static final long serialVersionUID = 1L;
    public int id;
    public int x;
    public int y;
    public int demand;

    public Customer(int id, int x, int y, int demand) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.demand = demand;
    }

    public double distanceTo(Customer other) {
        /*
            Distance ~ being the Euclidean distance to another customer.
            Used to evaluate the routes total distance. (Impacting the fitness)
        */
        return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }
}
