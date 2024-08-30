package main.java.distributed;

import java.io.Serializable;
import java.util.List;
import main.java.main_components.Route;
public class Solution implements Serializable {
    private static final long serialVersionUID = 1L; // Add a serialVersionUID for versioning
    List<Route> routes;
    double fitness;

    public Solution(List<Route> routes) {
        this.routes = routes;
        this.fitness = calculateFitness();
    }

    public double calculateFitness() {
        return routes.stream().mapToDouble(Route::calculateDistance).sum();
    }
}
