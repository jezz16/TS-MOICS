package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;

public class Individual {
    private int[] chromosome;
    private double fitness = -1;
    private int[] personalBestPosition;
    private double personalBestFitness = Double.NEGATIVE_INFINITY;
    private List<Cloudlet> cloudletList;
    private double[] fitnessValues; // Array untuk menyimpan nilai fitness multi-objective

    // Constructor untuk inisialisasi kromosom berdasarkan Data Center
    public Individual(int chromosomeLength, int dataCenterIterator) {
        this.chromosome = new int[chromosomeLength];
        this.personalBestPosition = new int[chromosomeLength];
        initializeChromosome(dataCenterIterator);
    }
    
    public Individual(int[] chromosome) {
        this.chromosome = chromosome.clone();
        this.personalBestPosition = chromosome.clone();
    }
    
 // Konstruktor pertama: menerima cloudletList
    public Individual(List<Cloudlet> cloudletList) {
        if (cloudletList == null || cloudletList.isEmpty()) {
            throw new IllegalArgumentException("cloudletList cannot be null or empty");
        }
        this.cloudletList = cloudletList;
        this.fitnessValues = new double[2]; // Dua fungsi tujuan: makespan dan cost
    }

    // Konstruktor kedua: menerima chromosome dan cloudletList
    public Individual(int[] chromosome, List<Cloudlet> cloudletList) {
        if (cloudletList == null || cloudletList.isEmpty()) {
            throw new IllegalArgumentException("cloudletList cannot be null or empty");
        }
        this.cloudletList = cloudletList;
        this.chromosome = chromosome;
        this.fitnessValues = new double[2]; // Dua fungsi tujuan: makespan dan cost
    }
    
    // Inisialisasi kromosom dengan VM yang valid dalam rentang Data Center
    private void initializeChromosome(int dataCenterIterator) {
        dataCenterIterator--; // Adjust to 0-based index
        int minVM = dataCenterIterator * 9;
        int maxVM = (dataCenterIterator + 1) * 9 - 1;
        Random rand = new Random();
        
        for (int i = 0; i < chromosome.length; i++) {
            int vmIndex = minVM + rand.nextInt(maxVM - minVM + 1);
            chromosome[i] = vmIndex;
            personalBestPosition[i] = vmIndex; // Inisialisasi personal best
        }
    }

    // Getter & Setter
    public int[] getChromosome() {
        return chromosome;
    }
    
    public void setChromosome(int[] chromosome) {
        this.chromosome = chromosome;
    }

    public int getGene(int index) {
        return chromosome[index];
    }

    public void setGene(int index, int value) {
        chromosome[index] = value;
    }

    public double getFitness() {
        return fitness;
    }
    
    public double[] getFitnessValues() {
        return fitnessValues;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
        if (fitness > personalBestFitness) {
            personalBestFitness = fitness;
            personalBestPosition = chromosome.clone();
        }
    }
    
    public void setFitnessValues(double makespan, double cost) {
        if (fitnessValues == null) {
            fitnessValues = new double[2]; // Inisialisasi jika belum diinisialisasi
        }
        this.fitnessValues[0] = makespan;
        this.fitnessValues[1] = cost;
    }

    public int[] getPersonalBestPosition() {
        return personalBestPosition;
    }

    public double getPersonalBestFitness() {
        return personalBestFitness;
    }
    
//    public void randomizeChromosome(int dataCenterIterator) {
//        if (dataCenterIterator <= 0) {
//            throw new IllegalArgumentException("dataCenterIterator must be greater than 0");
//        }
//        dataCenterIterator--;
//        int minVM = dataCenterIterator * 9;
//        int maxVM = (dataCenterIterator + 1) * 9 - 1;
//        Random rand = new Random();
//
//        // Pertahankan variasi genetik dengan crossover antara individu terbaik
//        int[] newChromosome = new int[chromosome.length];
//        for (int i = 0; i < chromosome.length; i++) {
//            if (rand.nextDouble() < 0.5) {
//                newChromosome[i] = chromosome[i];
//            } else {
//                int vmIndex = minVM + rand.nextInt(maxVM - minVM + 1);
//                newChromosome[i] = vmIndex;
//            }
//        }
//        chromosome = newChromosome;
//        personalBestPosition = chromosome.clone();
//        fitness = -1;
//    }    
    
    public void randomizeChromosome(int dataCenterIterator) {
        if (dataCenterIterator <= 0) {
            throw new IllegalArgumentException("dataCenterIterator must be greater than 0");
        }
        dataCenterIterator--;
        int minVM = dataCenterIterator * 9;
        int maxVM = (dataCenterIterator + 1) * 9 - 1;
        Random rand = new Random();
        
        for (int i = 0; i < chromosome.length; i++) {
            chromosome[i] = minVM + rand.nextInt(maxVM - minVM + 1);
        }        
        personalBestPosition = chromosome.clone(); // Reset personal best setelah randomisasi
//        fitnessValues = new double[2]; // Dua fungsi tujuan: makespan dan cost
        fitness = -1; // Fitness harus dihitung ulang setelah randomisasi
    }
}


//package org.cloudbus.cloudsim.examples;
//
//import java.util.List;
//import java.util.Random;
//
//import org.cloudbus.cloudsim.Cloudlet;
//
//public class Individual {
//    private int[] chromosome;
//    private double[] fitnessValues; // Array untuk menyimpan nilai fitness multi-objective
//
//    // Constructor untuk inisialisasi kromosom berdasarkan Data Center
//    public Individual(int chromosomeLength, int dataCenterIterator) {
//        this.chromosome = new int[chromosomeLength];
//        this.fitnessValues = new double[2]; // Dua fungsi tujuan: makespan dan cost
//        initializeChromosome(dataCenterIterator);
//    }
//
//    public Individual(int[] chromosome) {
//        this.chromosome = chromosome.clone();
//        this.fitnessValues = new double[2]; // Dua fungsi tujuan: makespan dan cost
//    }
//
//    // Konstruktor pertama: menerima cloudletList
//    public Individual(List<Cloudlet> cloudletList) {
//        if (cloudletList == null || cloudletList.isEmpty()) {
//            throw new IllegalArgumentException("cloudletList cannot be null or empty");
//        }
//        this.fitnessValues = new double[2]; // Dua fungsi tujuan: makespan dan cost
//    }
//
//    // Konstruktor kedua: menerima chromosome dan cloudletList
//    public Individual(int[] chromosome, List<Cloudlet> cloudletList) {
//        if (cloudletList == null || cloudletList.isEmpty()) {
//            throw new IllegalArgumentException("cloudletList cannot be null or empty");
//        }
//        this.chromosome = chromosome;
//        this.fitnessValues = new double[2]; // Dua fungsi tujuan: makespan dan cost
//    }
//
//    // Inisialisasi kromosom dengan VM yang valid dalam rentang Data Center
//    private void initializeChromosome(int dataCenterIterator) {
//        dataCenterIterator--; // Adjust to 0-based index
//        int minVM = dataCenterIterator * 9;
//        int maxVM = (dataCenterIterator + 1) * 9 - 1;
//        Random rand = new Random();
//        for (int i = 0; i < chromosome.length; i++) {
//            int vmIndex = minVM + rand.nextInt(maxVM - minVM + 1);
//            chromosome[i] = vmIndex;
//        }
//    }
//
//    // Getter & Setter
//    public int[] getChromosome() {
//        return chromosome;
//    }
//
//    public void setChromosome(int[] chromosome) {
//        this.chromosome = chromosome;
//    }
//
//    public int getGene(int index) {
//        return chromosome[index];
//    }
//
//    public void setGene(int index, int value) {
//        chromosome[index] = value;
//    }
//
//    public double[] getFitnessValues() {
//        return fitnessValues;
//    }
//
//    public void setFitnessValues(double makespan, double cost) {
//        this.fitnessValues[0] = makespan;
//        this.fitnessValues[1] = cost;
//    }
//
//    public void randomizeChromosome(int dataCenterIterator) {
//        if (dataCenterIterator <= 0) {
//            throw new IllegalArgumentException("dataCenterIterator must be greater than 0");
//        }
//        dataCenterIterator--;
//        int minVM = dataCenterIterator * 9;
//        int maxVM = (dataCenterIterator + 1) * 9 - 1;
//        Random rand = new Random();
//        for (int i = 0; i < chromosome.length; i++) {
//            chromosome[i] = minVM + rand.nextInt(maxVM - minVM + 1);
//        }
//        fitnessValues = new double[2]; // Reset fitness values setelah randomisasi
//    }
//}