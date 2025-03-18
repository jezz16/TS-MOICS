package org.cloudbus.cloudsim.examples;

import java.util.*;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

    public Individual getIndividual(int index) {
        return individuals[index];
    }

    public void setIndividual(int index, Individual individual) {
        individuals[index] = individual;
    }

    public int size() {
        return individuals.length;
    }
}