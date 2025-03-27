package org.cloudbus.cloudsim.examples;

import java.util.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

public class MOICS2 {
    // Parameter Algoritma
    private int Imax; // Jumlah iterasi maksimum
    private int populationSize; // Ukuran populasi
    private double pa; // Probabilitas abandon nest (biasanya 0.25)
    private double beta = 3; // Parameter distribusi Lévy
    
    // Data Cloud
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private int numberOfDataCenters = 6;
    
    
    // Penyimpanan solusi terbaik
    private double[] globalBestFitnesses;
    private int[][] globalBestPositions;
    
    private List<List<Individual>> paretoFronts;

    public MOICS2(int Imax, int populationSize, double pa, 
                 List<Cloudlet> cloudletList, List<Vm> vmList) {
        this.Imax = Imax;
        this.populationSize = populationSize;
        this.pa = pa;
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        
        // Inisialisasi global best
        globalBestFitnesses = new double[numberOfDataCenters];
        globalBestPositions = new int[numberOfDataCenters][];
        for (int i = 0; i < numberOfDataCenters; i++) {
            globalBestFitnesses[i] = Double.NEGATIVE_INFINITY;
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
            double mips = calculateMips(vmId % 9);

            totalExecutionTime += cloudletList.get(i).getCloudletLength() / mips;
            totalCost += calculateCost(vmId, cloudletList.get(i));
            iterator++;
        }    
//        totalExecutionTime *= 10; // Contoh skala
//        totalCost *= 10;
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
            double newFitness = 1.0 / (newFitnessValues[0] + newFitnessValues[1]);
            double currentFitness = 1.0 / (individual.getFitnessValues()[0] + individual.getFitnessValues()[1]);

            if (newFitness > currentFitness) {
                individual.setChromosome(newChromosome);
                individual.setFitnessValues(newFitnessValues[0], newFitnessValues[1]);
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
    
    
    // Abandon sarang terburuk
    public void abandonWorstNests(Population population, int dataCenterIterator) {
    	Random rand = new Random();
//    	int dcIndex = dataCenterIterator - 1;
    	
    	// Hitung jumlah sarang yang harus diabaikan
    	int worstNestCount = (int) (pa * populationSize);
    	
    	// Urutkan individu berdasarkan fitness multi-objective
        List<Individual> individuals = Arrays.asList(population.getIndividuals());
        individuals.sort((a, b) -> {
            double[] aFitness = a.getFitnessValues();
            double[] bFitness = b.getFitnessValues();
            return Double.compare(aFitness[0] + aFitness[1], bFitness[0] + bFitness[1]);
        });
    	
        // Abandon worst nests secara acak
        for (int i = 0; i < worstNestCount / 2; i++) {
            // Pilih individu terburuk secara acak dari yang terburuk
            int randomIndex = rand.nextInt(worstNestCount);
            Individual worstIndividual = individuals.get(randomIndex);
            worstIndividual.randomizeChromosome(dataCenterIterator); // Randomisasi kromosom
        }
        
        // Abandon worst nests melalui mutasi
        for (int i = worstNestCount / 2; i < worstNestCount; i++) {
            Individual worstIndividual = individuals.get(i);
            mutate(worstIndividual, dataCenterIterator); // Terapkan mutasi
        }
    }
    
    // Mutasi untuk diversifikasi pencarian
    public void mutate(Individual individual, int dataCenterIterator) {
        Random rand = new Random();
        int[] chromosome = individual.getChromosome();

        for (int i = 0; i < chromosome.length; i++) {
            if (rand.nextDouble() < 0.15) { // Probabilitas mutasi
                chromosome[i] = rand.nextInt((dataCenterIterator * 9) - ((dataCenterIterator - 1) * 9)) + ((dataCenterIterator - 1) * 9);
            }
        }
        individual.setChromosome(chromosome);
    }
    

    // Simpan solusi terbaik
//    public void keepBestSolutions(Population population, int dataCenterIterator) {
//        int dcIndex = dataCenterIterator - 1;
//        List<Individual> paretoFront = paretoFronts.get(dcIndex);
//
//        for (Individual individual : population.getIndividuals()) {
//            ParetoUtils.addToParetoFront(paretoFront, individual);
//        }
//
//        // Pilih solusi terbaik dari Pareto Front
//        if (!paretoFront.isEmpty()) {
//            Individual bestIndividual = Collections.max(paretoFront, Comparator.comparingDouble(ind -> 1.0 / (ind.getFitnessValues()[0] + ind.getFitnessValues()[1])));
//            globalBestFitnesses[dcIndex] = 1.0 / (bestIndividual.getFitnessValues()[0] + bestIndividual.getFitnessValues()[1]);
//            globalBestPositions[dcIndex] = bestIndividual.getChromosome().clone();
//        }
//    }
    
    public void keepBestSolutions(Population population, int dataCenterIterator) {
        int dcIndex = dataCenterIterator - 1;
        List<Individual> paretoFront = paretoFronts.get(dcIndex);

        for (Individual individual : population.getIndividuals()) {
            ParetoUtils.addToParetoFront(paretoFront, individual);
        }

        // Pilih solusi terbaik dari Pareto Front
        if (!paretoFront.isEmpty()) {
            // Memperbarui logika untuk memilih solusi terbaik
            Individual bestIndividual = Collections.max(paretoFront, Comparator.comparingDouble(ind -> {
                double[] fitnessValues = ind.getFitnessValues();
                return fitnessValues[0] + fitnessValues[1]; // Menggunakan penjumlahan untuk mendapatkan nilai yang lebih tinggi
            }));
            
            // Memperbarui globalBestFitnesses dengan nilai yang lebih tinggi
            globalBestFitnesses[dcIndex] = bestIndividual.getFitnessValues()[0] + bestIndividual.getFitnessValues()[1];
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
    public void applyOBL(Population population, int dataCenterIterator,  int cloudletIteration) {
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
            
            // Misalkan Anda ingin menggunakan totalExecutionTime sebagai fitness
//            double oppositeFitness = oppositeFitnessValues[1]; // atau gunakan [1] untuk totalCost
            
            double oppositeFitness = 1.0 / (oppositeFitnessValues[0] + oppositeFitnessValues[1]);
            double currentFitness = 1.0 / (individual.getFitnessValues()[0] + individual.getFitnessValues()[1]);
            
            // Bandingkan dengan individu saat ini dan ambil yang lebih baik
//            if (oppositeFitness < individual.getFitness()) { // Pastikan perbandingan sesuai dengan tujuan Anda
//                individual.setChromosome(oppositeSolution);
//                individual.setFitness(oppositeFitness);
//            }
//            
            // Bandingkan dengan individu saat ini dan ambil yang lebih baik
            if (oppositeFitness > currentFitness) {
                individual.setChromosome(oppositeSolution);
                individual.setFitnessValues(oppositeFitnessValues[0], oppositeFitnessValues[1]);
            }
        }
    }

    // Helper function: Batasi VM dalam rentang data center
    private int clampVM(int vm, int dataCenterIterator) {
        int min = (dataCenterIterator - 1) * 9;
        int max = dataCenterIterator * 9 - 1;
        return Math.min(Math.max(vm, min), max);
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

    // Helper function: Hitung biaya
    private double calculateCost(int vmId, Cloudlet cloudlet) {
        Vm vm = vmList.get(vmId);
        double cloudletLength = cloudlet.getCloudletLength();
        double costPerMips = vm.getCostPerMips();
        double mips = vm.getMips();
        double executionTime = cloudletLength / mips;
        return costPerMips * executionTime;      
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
}