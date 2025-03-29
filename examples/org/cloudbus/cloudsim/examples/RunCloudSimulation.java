package org.cloudbus.cloudsim.examples;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.PrintStream;
//import java.io.FileNotFoundException;

public class RunCloudSimulation { 
	public static void main(String[] args) {
        int numberOfRuns = 10; // Jumlah iterasi simulasi

        for (int i = 1; i <= numberOfRuns; i++) {
            String outputFileName = "cloudlet_output_run_" + i + ".txt"; // Nama file unik untuk setiap iterasi
            System.out.println("Running simulation iteration: " + i + ", Output file: " + outputFileName);

            // Jalankan simulasi dengan nama file sebagai argumen  
            String[] simulationArgs = {outputFileName};
            CloudSimulationMOICS.main(simulationArgs);
        }

        System.out.println("All simulations completed.");
    }
}