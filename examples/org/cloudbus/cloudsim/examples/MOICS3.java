package org.cloudbus.cloudsim.examples;

import java.util.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

public class MOICS3 {
    // Parameter Algoritma
    private int Imax; // Jumlah iterasi maksimum
    private int populationSize; // Ukuran populasi
    private double pa; // Probabilitas abandon nest (biasanya 0.25)
    private double beta = 1.5; // Parameter distribusi Lévy
    // Data Cloud
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private int numberOfDataCenters = 6;
    // Penyimpanan solusi terbaik
    private double[] globalBestFitnesses;
    private int[][] globalBestPositions;
    private List<List<Individual>> paretoFronts;

    public MOICS3 (int Imax, int populationSize, double pa, List<Cloudlet> cloudletList, List<Vm> vmList) {
        this.Imax = Imax;
        this.populationSize = populationSize;
        this.pa = pa;
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        // Inisialisasi global best
        globalBestFitnesses = new double[numberOfDataCenters];
        globalBestPositions = new int[numberOfDataCenters][];
        for (int i = 0; i < numberOfDataCenters; i++) {
            globalBestFitnesses[i] = Double.MAX_VALUE;
            globalBestPositions[i] = null;
        }
        // Inisialisasi Pareto Fronts
        paretoFronts = new ArrayList<>();
        for (int i = 0; i < numberOfDataCenters; i++) {
            paretoFronts.add(new ArrayList<>());
        }
    }

    // Inisialisasi populasi untuk data center tertentu
    public Population initPopulation(int chromosomeLength, int dataCenterIterator) {
        return new Population(populationSize, chromosomeLength, dataCenterIterator);
    }

    // Evaluasi fitness
    public void evaluateFitness(Population population, int dataCenterIterator, int cloudletIteration) {
        for (Individual individual : population.getIndividuals()) {
            double[] fitnessValues = calculateMultiObjectiveFitness(individual, dataCenterIterator, cloudletIteration);
            individual.setFitnessValues(fitnessValues[0], fitnessValues[1]);
        }
    }

    // Perhitungan fitness multi-objective
    private double[] calculateMultiObjectiveFitness(Individual individual, int dataCenterIterator, int cloudletIteration) {
        double totalExecutionTime = 0;
        double totalCost = 0;
        int iterator = 0;
        dataCenterIterator--; // Convert to 0-based index
        int startIndex = dataCenterIterator * 9 + cloudletIteration * 54;
        int endIndex = (dataCenterIterator + 1) * 9 + cloudletIteration * 54;
        // Validate indices against cloudletList size
        if (startIndex < 0 || endIndex > cloudletList.size()) {
            System.err.println("Invalid indices: start=" + startIndex + ", end=" + endIndex + ", listSize=" + cloudletList.size());
            return new double[]{Double.MAX_VALUE, Double.MAX_VALUE}; // Indicate invalid fitness
        }
        for (int i = startIndex; i < endIndex && i < cloudletList.size(); i++) {
            if (iterator >= individual.getChromosome().length) {
                System.err.println("Chromosome index out of bounds: " + iterator);
                break;
            }
            int vmId = individual.getGene(iterator);
            double mips = getMips(vmId % 9);
            totalExecutionTime += cloudletList.get(i).getCloudletLength() / mips;
            totalCost += calculateCost(vmId, cloudletList.get(i));
            iterator++;
        }
        return new double[]{totalExecutionTime, totalCost};
    }

    // Lévy Flight untuk generasi solusi baru
    public void levyFlightUpdate(Population population, int dataCenterIterator) {
        for (Individual individual : population.getIndividuals()) {
            int[] newChromosome = individual.getChromosome().clone();
            for (int i = 0; i < newChromosome.length; i++) {
                double step = levyFlightStep();
                int newVM = (int) (newChromosome[i] + step);
                // Pastikan VM valid
                newVM = clampVM(newVM, dataCenterIterator);
                newChromosome[i] = newVM;
            }
            // Ganti solusi jika lebih baik
            double[] newFitnessValues = calculateMultiObjectiveFitness(new Individual(newChromosome, cloudletList), dataCenterIterator, 0);
            double newMakespan = newFitnessValues[0];
            double newCost = newFitnessValues[1];
            double[] currentFitnessValues = individual.getFitnessValues();
            double currentMakespan = currentFitnessValues[0];
            double currentCost = currentFitnessValues[1];
            if (isBetter(newMakespan, newCost, currentMakespan, currentCost)) {
                individual.setChromosome(newChromosome);
                individual.setFitnessValues(newMakespan, newCost);
            }
        }
    }

    // Langkah Lévy Flight
    private double levyFlightStep() {
        Random rand = new Random();
        double sigma = Math.pow(
                (gamma(1 + beta) * Math.sin(Math.PI * beta / 2)) /
                (gamma((1 + beta) / 2) * beta * Math.pow(2, (beta - 1) / 2)),
                1.0 / beta
            );
        double u = rand.nextGaussian() * sigma;
        double v = Math.abs(rand.nextGaussian());
        return u / Math.pow(v, 1 / beta);
    }

    public void abandonWorstNests(Population population, int dataCenterIterator) {
        Random rand = new Random();
        int worstNestCount = (int) (pa * populationSize);
        // Urutkan individu berdasarkan fitness multi-objective
        List<Individual> individuals = Arrays.asList(population.getIndividuals());
        individuals.sort(new Comparator<Individual>() {
            @Override
            public int compare(Individual a, Individual b) {
                double[] aFitness = a.getFitnessValues();
                double[] bFitness = b.getFitnessValues();
                if (aFitness[0] != bFitness[0]) {
                    return Double.compare(bFitness[0], aFitness[0]); // Minimalkan makespan
                } else {
                    return Double.compare(bFitness[1], aFitness[1]); // Minimalkan cost
                }
            }
        });
        // Pertahankan beberapa individu terbaik
        int preserveCount = (int) (0.2 * populationSize); // Pertahankan 20% terbaik
        List<Individual> preservedIndividuals = individuals.subList(0, preserveCount);
        // Abandon (Pa)/2 worst nests secara random dari individu yang tidak dipertahankan
        List<Individual> remainingIndividuals = individuals.subList(preserveCount, individuals.size());
        for (int i = 0; i < worstNestCount / 2; i++) {
            int randomIndex = rand.nextInt(remainingIndividuals.size());
            remainingIndividuals.get(randomIndex).randomizeChromosome(dataCenterIterator);
        }
        // Abandon (Pa)/2 worst nests melalui mutasi dari individu yang tidak dipertahankan
        for (int i = 0; i < worstNestCount / 2; i++) {
            int randomIndex = rand.nextInt(remainingIndividuals.size());
            mutate(remainingIndividuals.get(randomIndex), dataCenterIterator);
        }
        // Gabungkan individu terbaik kembali ke populasi
        population.setIndividuals(preservedIndividuals.toArray(new Individual[0]));
        population.addAllIndividuals(remainingIndividuals.toArray(new Individual[0]));
    }

    public void mutate(Individual individual, int dataCenterIterator) {
        Random rand = new Random();
        int[] chromosome = individual.getChromosome();
        for (int i = 0; i < chromosome.length; i++) {
            double mutationProbability = 0.05 + (0.05 * (double) (Imax - 0) / Imax); // Probabilitas mutasi berubah seiring iterasi
            if (rand.nextDouble() < mutationProbability) {
                // Swap mutation
                int j = rand.nextInt(chromosome.length);
                int temp = chromosome[i];
                chromosome[i] = chromosome[j];
                chromosome[j] = temp;
            }
        }
        individual.setChromosome(chromosome);
    }

    // Simpan solusi terbaik
    public void keepBestSolutions(Population population, int dataCenterIterator) {
        int dcIndex = dataCenterIterator - 1;
        List<Individual> paretoFront = paretoFronts.get(dcIndex);
        for (Individual individual : population.getIndividuals()) {
            ParetoUtils.addToParetoFront(paretoFront, individual);
        }
        // Pilih solusi terbaik dari Pareto Front
        if (!paretoFront.isEmpty()) {
            Individual bestIndividual = Collections.max(paretoFront, new Comparator<Individual>() {
                @Override
                public int compare(Individual ind1, Individual ind2) {
                    double[] fit1 = ind1.getFitnessValues();
                    double[] fit2 = ind2.getFitnessValues();
                    if (fit1[0] < fit2[0] && fit1[1] < fit2[1]) return 1;
                    if (fit1[0] > fit2[0] || fit1[1] > fit2[1]) return -1;
                    return 0;
                }
            });
            globalBestFitnesses[dcIndex] = bestIndividual.getFitnessValues()[0];
            globalBestPositions[dcIndex] = bestIndividual.getChromosome().clone();
        }
    }

    public class ParetoUtils {
        // Metode untuk mengecek apakah solusi a mendominasi solusi b
        public static boolean dominates(double[] a, double[] b) {
            boolean betterInAny = false;
            for (int i = 0; i < a.length; i++) {
                if (a[i] < b[i]) {
                    betterInAny = true;
                } else if (a[i] > b[i]) {
                    return false;
                }
            }
            return betterInAny;
        }

        // Metode untuk menambahkan solusi ke front Pareto
        public static void addToParetoFront(List<Individual> paretoFront, Individual individual) {
            Iterator<Individual> it = paretoFront.iterator();
            boolean dominated = false;
            while (it.hasNext()) {
                Individual existing = it.next();
                if (dominates(existing.getFitnessValues(), individual.getFitnessValues())) {
                    dominated = true;
                    break;
                } else if (dominates(individual.getFitnessValues(), existing.getFitnessValues())) {
                    it.remove();
                }
            }
            if (!dominated) {
                paretoFront.add(individual);
            }
        }
    }

    // OBL (Opposition-Based Learning)
    public void applyOBL(Population population, int dataCenterIterator, int cloudletIteration) {
        int dcIndex = dataCenterIterator - 1;
        int[] eliteSolution = globalBestPositions[dcIndex];
        int minVM = (dataCenterIterator - 1) * 9;
        int maxVM = dataCenterIterator * 9 - 1;
        for (Individual individual : population.getIndividuals()) {
            int[] oppositeSolution = new int[eliteSolution.length];
            // Menghitung solusi lawan berdasarkan rumus OBL
            for (int i = 0; i < eliteSolution.length; i++) {
                oppositeSolution[i] = minVM + maxVM - individual.getGene(i);
                oppositeSolution[i] = clampVM(oppositeSolution[i], dataCenterIterator);
            }
            // Evaluasi solusi oposisi
            Individual oppositeInd = new Individual(oppositeSolution, cloudletList);
            double[] oppositeFitnessValues = calculateMultiObjectiveFitness(oppositeInd, dataCenterIterator, cloudletIteration);
            double oppositeMakespan = oppositeFitnessValues[0];
            double oppositeCost = oppositeFitnessValues[1];
            double[] currentFitnessValues = individual.getFitnessValues();
            double currentMakespan = currentFitnessValues[0];
            double currentCost = currentFitnessValues[1];
            // Bandingkan dengan individu saat ini dan ambil yang lebih baik
            if (isBetter(oppositeMakespan, oppositeCost, currentMakespan, currentCost)) {
                individual.setChromosome(oppositeSolution);
                individual.setFitnessValues(oppositeMakespan, oppositeCost);
            }
        }
    }

    // Helper function: Batasi VM dalam rentang data center
    private int clampVM(int vm, int dataCenterIterator) {
        int min = (dataCenterIterator - 1) * 9;
        int max = dataCenterIterator * 9 - 1;
        return Math.min(Math.max(vm, min), max);
    }

    // Helper function: Hitung biaya
    private double calculateCost(int vmId, Cloudlet cloudlet) {
        Vm vm = vmList.get(vmId);
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

    // Helper function: Dapatkan MIPS VM
    private double getMips(int vmType) {
        switch (vmType % 3) {
            case 0: return 400;
            case 1: return 500;
            case 2: return 600;
            default: return 500;
        }
    }

    // Fungsi Gamma (approximasi)
    private double gamma(double x) {
        return Math.sqrt(2 * Math.PI / x) * Math.pow((x / Math.E), x);
    }

    // Getter solusi terbaik
    public int[] getBestVmAllocationForDatacenter(int dataCenterIterator) {
        return globalBestPositions[dataCenterIterator - 1];
    }

    public double getBestFitnessForDatacenter(int dataCenterIterator) {
        return globalBestFitnesses[dataCenterIterator - 1];
    }

    // Helper function: Bandingkan dua solusi multi-objective
    private boolean isBetter(double makespan1, double cost1, double makespan2, double cost2) {
        return (makespan1 < makespan2 && cost1 < cost2) ||
               (makespan1 < makespan2 && cost1 == cost2) ||
               (makespan1 == makespan2 && cost1 < cost2);
    }
}