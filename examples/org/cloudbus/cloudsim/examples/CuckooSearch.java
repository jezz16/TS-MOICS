package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

@FunctionalInterface
interface ObjectiveFunction {
    double compute(int[] solution, int dataCenterIterator, int cloudletIteration);
}

public class CuckooSearch {
    private int maxIterations; // Maximum number of iterations
    private int populationSize; // Number of nests
    private double discoveryRate; // Probability of discovering alien eggs
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private int numberOfDataCenters = 6;
    private double[] globalBestFitnesses;
    private int[][] globalBestPositions;

    public CuckooSearch(int maxIterations, int populationSize, double discoveryRate,
                        List<Cloudlet> cloudletList, List<Vm> vmList) {
        this.maxIterations = maxIterations;
        this.populationSize = populationSize;
        this.discoveryRate = discoveryRate;
        this.cloudletList = cloudletList;
        this.vmList = vmList;

        globalBestFitnesses = new double[numberOfDataCenters];
        globalBestPositions = new int[numberOfDataCenters][];

        for (int i = 0; i < numberOfDataCenters; i++) {
            globalBestFitnesses[i] = Double.NEGATIVE_INFINITY;
            globalBestPositions[i] = null;
        }
    }

    public void runCS(int chromosomeLength, int dataCenterIterator, int cloudletIteration, ObjectiveFunction objectiveFunction) {
        Random random = new Random();
        int dcIndex = dataCenterIterator - 1;

        // Initialize population (nests)
        int[][] nests = new int[populationSize][chromosomeLength];
        double[] fitnessValues = new double[populationSize];

        // Initialize nests and calculate fitness
        initializeNests(nests, fitnessValues, chromosomeLength, dataCenterIterator, cloudletIteration, objectiveFunction);

        // Find initial best solution
        updateGlobalBest(nests, fitnessValues, dcIndex);

        // Main loop
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Generate new solutions via Levy flights
            for (int i = 0; i < populationSize; i++) {
                replaceBird(nests, fitnessValues, i, chromosomeLength, dataCenterIterator, cloudletIteration, objectiveFunction);
            }

            // Abandon some nests based on discovery rate
            abandonNests(nests, fitnessValues, chromosomeLength, dataCenterIterator, cloudletIteration, objectiveFunction);
        }
    }

    private void initializeNests(int[][] nests, double[] fitnessValues, int chromosomeLength, int dataCenterIterator, int cloudletIteration, ObjectiveFunction objectiveFunction) {
        Random random = new Random();
        for (int i = 0; i < populationSize; i++) {
            for (int j = 0; j < chromosomeLength; j++) {
                nests[i][j] = random.nextInt(9) + (dataCenterIterator - 1) * 9;
            }
            fitnessValues[i] = objectiveFunction.compute(nests[i], dataCenterIterator, cloudletIteration);
        }
    }

    private void updateGlobalBest(int[][] nests, double[] fitnessValues, int dcIndex) {
        int bestNestIndex = 0;
        for (int i = 1; i < populationSize; i++) {
            if (fitnessValues[i] > fitnessValues[bestNestIndex]) {
                bestNestIndex = i;
            }
        }
        globalBestFitnesses[dcIndex] = fitnessValues[bestNestIndex];
        globalBestPositions[dcIndex] = nests[bestNestIndex].clone();
    }

    private void abandonNests(int[][] nests, double[] fitnessValues, int chromosomeLength, int dataCenterIterator, int cloudletIteration, ObjectiveFunction objectiveFunction) {
        Random random = new Random();
        for (int i = 0; i < populationSize; i++) {
            if (random.nextDouble() < discoveryRate) {
                nests[i] = generateRandomNest(chromosomeLength, dataCenterIterator);
                fitnessValues[i] = objectiveFunction.compute(nests[i], dataCenterIterator, cloudletIteration);
            }
        }
    }

    private void replaceBird(int[][] nests, double[] fitnessValues, int index, int chromosomeLength, int dataCenterIterator, int cloudletIteration, ObjectiveFunction objectiveFunction) {
        // Generate a new nest using Levy flight
        int[] newNest = levyFlight(nests[index], chromosomeLength, dataCenterIterator);
        double newFitness = objectiveFunction.compute(newNest, dataCenterIterator, cloudletIteration);

        // Replace if the new nest is better
        if (newFitness > fitnessValues[index]) {
            nests[index] = newNest;
            fitnessValues[index] = newFitness;

            // Update global best if necessary
            int dcIndex = dataCenterIterator - 1;
            if (newFitness > globalBestFitnesses[dcIndex]) {
                globalBestFitnesses[dcIndex] = newFitness;
                globalBestPositions[dcIndex] = newNest.clone();
            }
        }
    }

    private int[] levyFlight(int[] nest, int chromosomeLength, int dataCenterIterator) {
        Random random = new Random();
        int[] newNest = nest.clone();

        for (int i = 0; i < chromosomeLength; i++) {
            double step = Math.pow(random.nextGaussian(), 2); // Levy flight step
            int newPosition = newNest[i] + (int) Math.round(step);

            // Boundary constraints
            int minPosition = (dataCenterIterator - 1) * 9;
            int maxPosition = (dataCenterIterator * 9) - 1;

            newPosition = Math.max(minPosition, Math.min(maxPosition, newPosition));
            newNest[i] = newPosition;
        }

        return newNest;
    }

    private int[] generateRandomNest(int chromosomeLength, int dataCenterIterator) {
        Random random = new Random();
        int[] nest = new int[chromosomeLength];

        for (int i = 0; i < chromosomeLength; i++) {
            nest[i] = random.nextInt(9) + (dataCenterIterator - 1) * 9;
        }

        return nest;
    }

    // Example objective function: Rosenbrock function
    public static double rosenbrock(int[] solution, int dataCenterIterator, int cloudletIteration) {
        double sum = 0.0;
        for (int i = 0; i < solution.length - 1; i++) {
            double x1 = solution[i];
            double x2 = solution[i + 1];
            sum += 100 * Math.pow(x2 - Math.pow(x1, 2), 2) + Math.pow(1 - x1, 2);
        }
        return -sum; // Minimize the negative of the function
    }

    // Example objective function: Six Hump Camel Back function
    public static double sixHumpCamelBack(int[] solution, int dataCenterIterator, int cloudletIteration) {
        double x1 = solution[0] / 10.0; // Scale down for better optimization
        double x2 = solution[1] / 10.0; // Scale down for better optimization
        return 4 * Math.pow(x1, 2) - 2.1 * Math.pow(x1, 4) + (1.0 / 3.0) * Math.pow(x1, 6) 
               + x1 * x2 - 4 * Math.pow(x2, 2) + 4 * Math.pow(x2, 4);
    }

    private double calcFitness(int[] solution, int dataCenterIterator, int cloudletIteration) {
        double totalExecutionTime = 0;
        double totalCost = 0;

        for (int i = 0; i < solution.length; i++) {
            int gene = solution[i];
            double mips = calculateMips(gene % 9);

            // Calculate cloudletIndex with modulo to ensure it's within bounds
            int cloudletIndex = (i + (dataCenterIterator - 1) * 9 + cloudletIteration * 54) % cloudletList.size();

            // Validate cloudletIndex
            if (cloudletIndex < 0 || cloudletIndex >= cloudletList.size()) {
                System.err.println("Error: Invalid cloudletIndex = " + cloudletIndex);
                System.err.println("i = " + i + ", dataCenterIterator = " + dataCenterIterator + ", cloudletIteration = " + cloudletIteration);
                return Double.NEGATIVE_INFINITY; // Return a default fitness value
            }

            // Calculate execution time and cost
            totalExecutionTime += cloudletList.get(cloudletIndex).getCloudletLength() / mips;
            totalCost += calculateCost(vmList.get(gene % 9), cloudletList.get(cloudletIndex));
        }

        double makespanFitness = calculateMakespanFitness(totalExecutionTime);
        double costFitness = calculateCostFitness(totalCost);

        return makespanFitness + costFitness;
    }

    private double calculateMips(int vmIndex) {
        double mips = 0;
        if (vmIndex % 9 == 0 || vmIndex % 9 == 3 || vmIndex % 9 == 6) {
            mips = 400;
        } else if (vmIndex % 9 == 1 || vmIndex % 9 == 4 || vmIndex % 9 == 7) {
            mips = 500;
        } else if (vmIndex % 9 == 2 || vmIndex % 9 == 5 || vmIndex % 9 == 8) {
            mips = 600;
        }
        return mips;
    }
    
    private double calculateCost(Vm vm, Cloudlet cloudlet) {
        double costPerMips = vm.getCostPerMips();
        double costPerMem = 0.05;
        double costPerBw = 0.1;
        double executionTime = cloudlet.getCloudletLength() / vm.getMips();
        double memoryUsage = vm.getRam();
        double bandwidthUsage = 1000; // Asumsi bandwidth usage
        double memoryCost = memoryUsage * costPerMem;
        double bandwidthCost = bandwidthUsage * costPerBw;
        return costPerMips * executionTime + memoryCost + bandwidthCost;
    }

    private double calculateMakespanFitness(double totalExecutionTime) {
        return 1.0 / totalExecutionTime;
    }

    private double calculateCostFitness(double totalCost) {
        return 1.0 / totalCost;
    }

    public int[] getBestVmAllocationForDatacenter(int dataCenterIterator) {
        return globalBestPositions[dataCenterIterator - 1];
    }

    public double getBestFitnessForDatacenter(int dataCenterIterator) {
        return globalBestFitnesses[dataCenterIterator - 1];
    }
}