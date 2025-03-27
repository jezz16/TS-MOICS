package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CalculateAverageMetrics {
    public static void main(String[] args) {
        int numberOfRuns = 10; // Jumlah file output
        String baseFileName = "cloudlet_output_run_"; // Awalan nama file

        // List untuk menyimpan nilai dari setiap file
        List<Double> makespans = new ArrayList<>();
        List<Double> totalCosts = new ArrayList<>();
        List<Double> avgWaitingTimes = new ArrayList<>();
        List<Double> throughputs = new ArrayList<>();
        List<Double> resourceUtilizations = new ArrayList<>();
        List<Double> totalEnergyConsumptions = new ArrayList<>();

        // Loop untuk membaca setiap file
        for (int i = 1; i <= numberOfRuns; i++) {
            String fileName = baseFileName + i + ".txt";
            try {
                extractMetricsFromFile(fileName, makespans, totalCosts, avgWaitingTimes, throughputs, resourceUtilizations, totalEnergyConsumptions);
            } catch (IOException e) {
                System.err.println("Error reading file: " + fileName);
                e.printStackTrace();
            }
        }

        // Hitung rata-rata
        double avgMakespan = calculateAverage(makespans);
        double avgTotalCost = calculateAverage(totalCosts);
        double avgWaitingTime = calculateAverage(avgWaitingTimes);
        double avgThroughput = calculateAverage(throughputs);
        double avgResourceUtilization = calculateAverage(resourceUtilizations);
        double avgEnergyConsumption = calculateAverage(totalEnergyConsumptions);

        // Cetak hasil rata-rata
        System.out.println("========== AVERAGE METRICS ==========");
        System.out.printf("Average Makespan: %.6f%n", avgMakespan);
        System.out.printf("Average Total Cost: %.2f%n", avgTotalCost);
        System.out.printf("Average Waiting Time: %.6f%n", avgWaitingTime);
        System.out.printf("Average Throughput: %.9f%n", avgThroughput);
        System.out.printf("Average Resource Utilization: %.6f%n", avgResourceUtilization);
        System.out.printf("Average Energy Consumption: %.2f kWh%n", avgEnergyConsumption);
        System.out.printf("Average Metric for MOICS SDSC");
    }

    private static void extractMetricsFromFile(String fileName, List<Double> makespans, List<Double> totalCosts,
                                                List<Double> avgWaitingTimes, List<Double> throughputs,
                                                List<Double> resourceUtilizations, List<Double> energyConsumptions)
            throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Makespan:")) {
                    makespans.add(extractDoubleValue(line));
                } else if (line.contains("Total Cost:")) {
                    totalCosts.add(extractDoubleValue(line));
                } else if (line.contains("Average Waiting time:")) {
                    avgWaitingTimes.add(extractDoubleValue(line));
                } else if (line.contains("Throughput:")) {
                    throughputs.add(extractDoubleValue(line));
                } else if (line.contains("Resource Utilization:")) {
                    resourceUtilizations.add(extractDoubleValue(line));
                } else if (line.contains("Total Energy Consumption:")) {
                    energyConsumptions.add(extractDoubleValue(line));
                }
            }
        }
    }

    private static double extractDoubleValue(String line) {
        // Ekstrak nilai numerik dari baris teks
        String[] parts = line.split(":");
        if (parts.length > 1) {
            String valuePart = parts[1].trim().replaceAll("[^\\d.]", "");
            return Double.parseDouble(valuePart);
        }
        return 0.0;
    }

    private static double calculateAverage(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}