package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.Comparator;

public class Population {
    private Individual[] individuals;

    // Constructor untuk inisialisasi populasi dengan Data Center tertentu
    public Population(int populationSize, int chromosomeLength, int dataCenterIterator) {
        individuals = new Individual[populationSize];
        for (int i = 0; i < populationSize; i++) {
            individuals[i] = new Individual(chromosomeLength, dataCenterIterator);
        }
    }  

    // Mengembalikan individu terbaik berdasarkan fitness
    public Individual getFittest() {
        Arrays.sort(individuals, new Comparator<Individual>() {
            @Override
            public int compare(Individual o1, Individual o2) {
                return Double.compare(o2.getFitness(), o1.getFitness());
            }
        }); 
        return individuals[0];
    }

    // Getter & Setter
    public Individual[] getIndividuals() {
        return individuals;
    }

    public void addAllIndividuals(Individual[] newIndividuals) {
        for (Individual individual : newIndividuals) {
            addIndividual(individual); 
        }
    }
    
    public void addIndividual(Individual individual) {
        // Temukan indeks kosong dalam array individuals dan tambahkan individu
        for (int i = 0; i < individuals.length; i++) 
        	individuals[i] = individual;
        	return; // Keluar setelah menambahkan individu
    }

    public void setIndividuals(Individual[] newIndividuals) {
        // Pastikan ukuran array baru tidak melebihi ukuran array individuals
        if (newIndividuals.length > individuals.length) {
            throw new IllegalArgumentException("Too many individuals to set.");
        }
        
        // Mengatur individu dari array baru ke dalam array individuals
        for (int i = 0; i < newIndividuals.length; i++) {
            individuals[i] = newIndividuals[i];
        }
    }
    
    public Individual getBestIndividual() {
        Individual best = individuals[0];
        for (Individual individual : individuals) {
            if (individual != null && individual.getFitness() > best.getFitness()) {
                best = individual;
            }
        }
        return best;
    }
    
    private boolean isBetter(Individual ind1, Individual ind2) {
        double[] fit1 = ind1.getFitnessValues();
        double[] fit2 = ind2.getFitnessValues();
        return (fit1[0] < fit2[0] && fit1[1] < fit2[1]) ||
               (fit1[0] < fit2[0] && fit1[1] == fit2[1]) ||
               (fit1[0] == fit2[0] && fit1[1] < fit2[1]);
    }

    public int size() {
        return individuals.length;
    }
}


//package org.cloudbus.cloudsim.examples;
//
//public class Population {
//    private Individual[] individuals;
//
//    // Constructor untuk inisialisasi populasi dengan Data Center tertentu
//    public Population(int populationSize, int chromosomeLength, int dataCenterIterator) {
//        individuals = new Individual[populationSize];
//        for (int i = 0; i < populationSize; i++) {
//            individuals[i] = new Individual(chromosomeLength, dataCenterIterator);
//        }
//    }
//
//    // Getter & Setter
//    public Individual[] getIndividuals() {
//        return individuals;
//    }
//
//    public void setIndividuals(Individual[] newIndividuals) {
//        // Pastikan ukuran array baru tidak melebihi ukuran array individuals
//        if (newIndividuals.length > individuals.length) {
//            throw new IllegalArgumentException("Too many individuals to set.");
//        }
//        // Mengatur individu dari array baru ke dalam array individuals
//        for (int i = 0; i < newIndividuals.length; i++) {
//            individuals[i] = newIndividuals[i];
//        }
//    }
//
//    public void addAllIndividuals(Individual[] newIndividuals) {
//        for (Individual individual : newIndividuals) {
//            addIndividual(individual);
//        }
//    }
//
//    public void addIndividual(Individual individual) {
//        // Temukan indeks kosong dalam array individuals dan tambahkan individu
//        for (int i = 0; i < individuals.length; i++) {
//                individuals[i] = individual;
//                return; // Keluar setelah menambahkan individu
//        }
//        throw new IllegalStateException("Population is full.");
//    }
//
//    public Individual getFittest() {
//        Individual best = individuals[0];
//        for (Individual individual : individuals) {
//            if (individual != null && isBetter(individual, best)) {
//                best = individual;
//            }
//        }
//        return best;
//    }
//
//    private boolean isBetter(Individual ind1, Individual ind2) {
//        double[] fit1 = ind1.getFitnessValues();
//        double[] fit2 = ind2.getFitnessValues();
//        return (fit1[0] < fit2[0] && fit1[1] < fit2[1]) ||
//               (fit1[0] < fit2[0] && fit1[1] == fit2[1]) ||
//               (fit1[0] == fit2[0] && fit1[1] < fit2[1]);
//    }
//
//    public int size() {
//        return individuals.length;
//    }
//}