package org.cloudbus.cloudsim.examples;

import java.util.Random;

public class Individual {
    private int[] chromosome;
    private double fitness = -1;
    // for discrete velocity
    private int[] velocity;
    // private double[] velocity;
    private int[] personalBestPosition;
    private double personalBestFitness = Double.NEGATIVE_INFINITY;

    public Individual(int[] chromosome) {
        this.chromosome = chromosome
        // for discrete velocity
        this.velocity = new int[chromosome.length];
        // this.velocity = new double[chromosome.length];
        this.personalBestPosition = chromosome.clone();
        this.personalBestFitness = Double.NEGATIVE_INFINITY;
    }

    public Individual(int chromosomeLength, int dataCenterIterator) {
        this.chromosome = new int[chromosomeLength];
        // for discrete velocity
        this.velocity = new int[chromosomeLength];
        // this.velocity = new double[chromosomeLength];
        this.personalBestPosition = new int[chromosomeLength];

        dataCenterIterator = dataCenterIterator - 1;
        int max = 8 + 9 * dataCenterIterator;
        int min = 0 + 9 * dataCenterIterator;
        int range = max - min + 1;

        // for discrete velocity
        int vmSize = (int) ((54 / 6) - 1);
        int Vmax = (int) (vmSize * 0.5); // Maksimum velocity
        int minVelocity = -Vmax;
        int maxVelocity = Vmax;

        // double vmSize = (54.0 / 6.0) - 1.0;
        // double Vmax = vmSize * 0.5;
        // double minVelocity = -Vmax;
        // double maxVelocity = Vmax;


        Random random = new Random();

        for (int gene = 0; gene < chromosomeLength; gene++) {
            int rand = random.nextInt(range) + min;
            setGene(gene, rand);
            // for discrete velocity
            this.velocity[gene] = random.nextInt((maxVelocity - minVelocity) + 1) + minVelocity;
            // this.velocity[gene] = minVelocity + (maxVelocity - minVelocity) * random.nextDouble();
            this.personalBestPosition[gene] = rand;
        }
    }

    public int[] getVelocity() {
        return this.velocity;
    }

    // public double[] getVelocity() {
    //     return this.velocity;
    // }

    public void setVelocity(int index, int value) {
        this.velocity[index] = value;
    }

    // public void setVelocity(int index, double value) {
    //     this.velocity[index] = value;
    // }

    public int[] getPersonalBestPosition() {
        return this.personalBestPosition;
    }

    public void setPersonalBestPosition(int[] position) {
        this.personalBestPosition = position.clone();
    }

    public double getPersonalBestFitness() {
        return this.personalBestFitness;
    }

    public void setPersonalBestFitness(double fitness) {
        this.personalBestFitness = fitness;
    }

    public int[] getChromosome() {
        return this.chromosome;
    }

    public int getChromosomeLength() {
        return this.chromosome.length;
    }

    public void setGene(int offset, int gene) {
        this.chromosome[offset] = gene;
    }

    public int getGene(int offset) {
        return this.chromosome[offset];
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getFitness() {
        return this.fitness;
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        for (int gene = 0; gene < this.chromosome.length; gene++) {
            output.append(this.chromosome[gene]);
            output.append(", ");
        }
        return output.toString();
    }
}