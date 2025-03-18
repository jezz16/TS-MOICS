//package org.cloudbus.cloudsim.examples;
//
//import java.util.*;
//import org.cloudbus.cloudsim.Cloudlet;
//import org.cloudbus.cloudsim.Vm;
//
//public class MOICS {
//    // Parameter Algoritma
//    private int Imax; // Jumlah iterasi maksimum
//    private int populationSize; // Ukuran populasi
//    private double pa; // Probabilitas abandon nest (biasanya 0.25)
//    private double beta = 1.5; // Parameter distribusi Lévy
//
//    // Data Cloud
//    private List<Cloudlet> cloudletList;
//    private List<Vm> vmList;
//    private int numberOfDataCenters = 6;
//
//    // Penyimpanan solusi terbaik
//    private double[] globalBestFitnesses;
//    private int[][] globalBestPositions;
//
//    public MOICS(int Imax, int populationSize, double pa,
//                 List<Cloudlet> cloudletList, List<Vm> vmList) {
//        this.Imax = Imax;
//        this.populationSize = populationSize;
//        this.pa = pa;
//        this.cloudletList = cloudletList;
//        this.vmList = vmList;
//
//        // Inisialisasi global best
//        globalBestFitnesses = new double[numberOfDataCenters];
//        globalBestPositions = new int[numberOfDataCenters][];
//        for (int i = 0; i < numberOfDataCenters; i++) {
//            globalBestFitnesses[i] = Double.NEGATIVE_INFINITY;
//            globalBestPositions[i] = null;
//        }
//    }
//
//    // Loop Utama Algoritma MOICS
//    public void run() {
//        Random rand = new Random();
//        for (int dataCenterIterator = 1; dataCenterIterator <= numberOfDataCenters; dataCenterIterator++) {
//            Population population = initPopulation(cloudletList.size(), dataCenterIterator);
//
//            for (int t = 0; t < Imax; t++) {
//                evaluateFitness(population, dataCenterIterator, 0);
//
//                // Levy Flight untuk generasi solusi baru
//                levyFlightUpdate(population, dataCenterIterator);
//
//                // Abandon sarang terburuk
//                abandonWorstNests(population, dataCenterIterator);
//
//                // Simpan solusi terbaik
//                keepBestSolutions(population, dataCenterIterator);
//            }
//        }
//    }
//
//    // Inisialisasi populasi untuk data center tertentu
//    public Population initPopulation(int chromosomeLength, int dataCenterIterator) {
//        return new Population(populationSize, chromosomeLength, dataCenterIterator);
//    }
//
//    // Evaluasi fitness
//    public void evaluateFitness(Population population, int dataCenterIterator, int cloudletIteration) {
//        for (Individual individual : population.getIndividuals()) {
//            double fitness = calculateFitness(individual, dataCenterIterator, cloudletIteration);
//            individual.setFitness(fitness);
//
//            // Update global best
//            int dcIndex = dataCenterIterator - 1;
//            if (fitness > globalBestFitnesses[dcIndex]) {
//                globalBestFitnesses[dcIndex] = fitness;
//                globalBestPositions[dcIndex] = individual.getChromosome().clone();
//            }
//        }
//    }
//
//    // Perhitungan fitness
//    private double calculateFitness(Individual individual, int dataCenterIterator, int cloudletIteration) {
//        double totalExecutionTime = 0;
//        double totalCost = 0;
//        int iterator = 0;
//        dataCenterIterator--; // Convert to 0-based index
//
//        int startIndex = dataCenterIterator * 9 + cloudletIteration * 54;
//        int endIndex = (dataCenterIterator + 1) * 9 + cloudletIteration * 54;
//
//        // Validate indices against cloudletList size
//        if (startIndex < 0 || endIndex > cloudletList.size()) {
//            System.err.println("Invalid indices: start=" + startIndex + ", end=" + endIndex + ", listSize=" + cloudletList.size());
//            return Double.NEGATIVE_INFINITY; // Indicate invalid fitness
//        }
//
//        for (int i = startIndex; i < endIndex && i < cloudletList.size(); i++) {
//            if (iterator >= individual.getChromosome().length) {
//                System.err.println("Chromosome index out of bounds: " + iterator);
//                break;
//            }
//
//            int vmId = individual.getGene(iterator);
//            double mips = getMips(vmId % 9);
//
//            totalExecutionTime += cloudletList.get(i).getCloudletLength() / mips;
//            totalCost += calculateCost(vmId, cloudletList.get(i));
//            iterator++;
//        }
//
//        return (totalExecutionTime + totalCost) == 0 ? Double.MAX_VALUE : 1.0 / (totalExecutionTime + totalCost);
//    }
//
//    // Lévy Flight untuk generasi solusi baru
//    public void levyFlightUpdate(Population population, int dataCenterIterator) {
//        Random rand = new Random();
//        int dcIndex = dataCenterIterator - 1;
//
//        for (Individual individual : population.getIndividuals()) {
//            int[] newChromosome = individual.getChromosome().clone();
//
//            for (int i = 0; i < newChromosome.length; i++) {
//                double step = levyFlightStep();
//                int newVM = (int) (newChromosome[i] + step);
//
//                // Pastikan VM valid
//                newVM = clampVM(newVM, dataCenterIterator);
//                newChromosome[i] = newVM;
//            }
//
//            // Ganti solusi jika lebih baik
//            double newFitness = calculateFitness(new Individual(newChromosome), dataCenterIterator, 0);
//            if (newFitness > individual.getFitness()) {
//                individual.setChromosome(newChromosome);
//                individual.setFitness(newFitness);
//            }
//        }
//    }
//
//    // Abandon sarang terburuk
//    private void abandonWorstNests(Population population, int dataCenterIterator) {
//        Random rand = new Random();
//        int dcIndex = dataCenterIterator - 1;
//
//        // Hitung jumlah sarang yang harus diabaikan
//        int worstNestCount = (int) (pa * populationSize);
//
//        // Urutkan individu berdasarkan fitness
//        List<Individual> individuals = Arrays.asList(population.getIndividuals());
//        individuals.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
//
//        // Abandon (Pa)/2 worst nests secara random
//        for (int i = 0; i < worstNestCount / 2; i++) {
//            int randomIndex = rand.nextInt(worstNestCount);
//            individuals.get(randomIndex).randomizeChromosome(dataCenterIterator);
//        }
//
//        // Abandon (Pa)/2 worst nests melalui mutasi
//        for (int i = worstNestCount / 2; i < worstNestCount; i++) {
//            mutate(individuals.get(i), dataCenterIterator);
//        }
//    }
//
//    // Mutasi untuk diversifikasi pencarian
//    private void mutate(Individual individual, int dataCenterIterator) {
//        Random rand = new Random();
//        int[] chromosome = individual.getChromosome();
//
//        for (int i = 0; i < chromosome.length; i++) {
//            if (rand.nextDouble() < 0.1) { // Probabilitas mutasi
//                chromosome[i] = rand.nextInt((dataCenterIterator * 9) - ((dataCenterIterator - 1) * 9)) + ((dataCenterIterator - 1) * 9);
//            }
//        }
//
//        individual.setChromosome(chromosome);
//    }
//
//    // Simpan solusi terbaik
//    private void keepBestSolutions(Population population, int dataCenterIterator) {
//        int dcIndex = dataCenterIterator - 1;
//
//        for (Individual individual : population.getIndividuals()) {
//            if (individual.getFitness() > globalBestFitnesses[dcIndex]) {
//                globalBestFitnesses[dcIndex] = individual.getFitness();
//                globalBestPositions[dcIndex] = individual.getChromosome().clone();
//            }
//        }
//    }
//
//    // Langkah Lévy Flight
//    private double levyFlightStep() {
//        Random rand = new Random();
//        double sigma = Math.pow(
//                (gamma(1 + beta) * Math.sin(Math.PI * beta / 2)) /
//                        (gamma((1 + beta) / 2) * beta * Math.pow(2, (beta - 1) / 2)),
//                1.0 / beta
//        );
//        double u = rand.nextGaussian() * sigma;
//        double v = Math.abs(rand.nextGaussian());
//        return u / Math.pow(v, 1 / beta);
//    }
//
//    // Helper function: Batasi VM dalam rentang data center
//    private int clampVM(int vm, int dataCenterIterator) {
//        int min = (dataCenterIterator - 1) * 9;
//        int max = dataCenterIterator * 9 - 1;
//        return Math.min(Math.max(vm, min), max);
//    }
//
//    // Helper function: Hitung biaya
//    private double calculateCost(int vmId, Cloudlet cloudlet) {
//        Vm vm = vmList.get(vmId);
//        double costPerMips = vm.getCostPerMips();
//        double executionTime = cloudlet.getCloudletLength() / vm.getMips();
//        return costPerMips * executionTime;
//    }
//
//    // Helper function: Dapatkan MIPS VM
//    private double getMips(int vmType) {
//        switch (vmType % 3) {
//            case 0: return 400;
//            case 1: return 500;
//            case 2: return 600;
//            default: return 500;
//        }
//    }
//
//    // Fungsi Gamma (approximasi)
//    private double gamma(double x) {
//        return Math.sqrt(2 * Math.PI / x) * Math.pow((x / Math.E), x);
//    }
//
//    // Getter solusi terbaik
//    public int[] getBestVmAllocationForDatacenter(int dataCenterIterator) {
//        return globalBestPositions[dataCenterIterator - 1];
//    }
//}