import random
import csv

num_customers = 200000
num_vehicles = 100
vehicle_capacity = 200
max_demand = 20  # Max demand per customer
grid_size = 100  # Grid size for x, y coordinates

filename = "vrp_200000_dataset.csv"  # Saved in current dir

rand = random.Random()

# Open the CSV file for writing
with open(filename, mode='w', newline='') as file:
    writer = csv.writer(file)

    # Write the depot information (assumed at the center of the grid)
    writer.writerow(["Depot", 50, 50, 0])

    # Generate and write customer data
    for i in range(1, num_customers + 1):
        x = rand.randint(0, grid_size)
        y = rand.randint(0, grid_size)
        demand = rand.randint(1, max_demand)
        writer.writerow([f"Customer{i}", x, y, demand])

print(f"Dataset generated successfully and saved to {filename}")
