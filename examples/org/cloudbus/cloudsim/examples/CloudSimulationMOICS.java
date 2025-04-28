package org.cloudbus.cloudsim.examples;

import java.util.Locale;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.DoubleStream;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


public class CloudSimulationMOICS {
    	private static PowerDatacenter datacenter1, datacenter2, datacenter3, datacenter4, datacenter5, datacenter6;
        private static List<Cloudlet> cloudletList;
        private static List<Vm> vmList;
        private static int bot = 7;
        
        public static void main(String[] args) {
        	Locale.setDefault(new Locale("en", "US"));
            Log.printLine("Starting Cloud Simulation with MOICS...");
            
            try {
                int num_user = 1;
                Calendar calendar = Calendar.getInstance();
                boolean trace_flag = false;
                
                CloudSim.init(num_user, calendar, trace_flag);

                int hostId = 0;

                datacenter1 = createDatacenter("DataCenter_1", hostId);
                hostId = 3;
                datacenter2 = createDatacenter("DataCenter_2", hostId);
                hostId = 6;
                datacenter3 = createDatacenter("DataCenter_3", hostId);
                hostId = 9;
                datacenter4 = createDatacenter("DataCenter_4", hostId);
                hostId = 12;
                datacenter5 = createDatacenter("DataCenter_5", hostId);
                hostId = 15;
                datacenter6 = createDatacenter("DataCenter_6", hostId);
                
                DatacenterBroker broker = createBroker();
                int brokerId = broker.getId();
                
                // Create VMs and Cloudlets
                int vmNumber = 54;
                int cloudletNumber = 7395; // dataset size
//                int cloudletNumber = bot*1000;
                
                vmList = createVM(brokerId, vmNumber);
                cloudletList = createCloudlet(brokerId, cloudletNumber);
                
                broker.submitVmList(vmList);
                broker.submitCloudletList(cloudletList);
                
                // MOICS Parameters
                int Imax = 5;
                int populationSize = 100;
                double pa = 0.25;

                MOICS moics = new MOICS (Imax, populationSize, pa, cloudletList, vmList);
                
                int cloudletLoopingNumber = cloudletNumber / vmNumber - 1;
                
                for(int cloudletIterator = 0; cloudletIterator <= cloudletLoopingNumber; cloudletIterator++) {
                    System.out.println("Cloudlet Iteration: " + cloudletIterator);
                    
                    for(int dataCenterIterator = 1; dataCenterIterator <= 6; dataCenterIterator++) {
                        System.out.println("Processing DataCenter: " + dataCenterIterator);
                        
                        // Initialize population
                        Population population = moics.initPopulation(cloudletNumber, dataCenterIterator);
                        
                        // MOICS Optimization Loop
                        for(int iteration = 1; iteration <= Imax; iteration++) {
                        	moics.evaluateFitness(population, dataCenterIterator, cloudletIterator);
                            moics.levyFlightUpdate(population, dataCenterIterator);
                            moics.abandonWorstNests(population, dataCenterIterator);
                            moics.keepBestSolutions(population, dataCenterIterator);
//                            moics.applyOBL(population, dataCenterIterator, cloudletIterator);
                            
                            System.out.println("Iteration " + iteration + " Best Fitness for DC" + 
                                dataCenterIterator + ": " + moics.getBestFitnessForDatacenter(dataCenterIterator));
                        }
                        
                        // Bind cloudlets to best VMs
                        int[] bestSolution = moics.getBestVmAllocationForDatacenter(dataCenterIterator);

                        int startAssigner = 0 + (dataCenterIterator - 1) * 9 + cloudletIterator * 54;
                        int endAssigner = 9 + (dataCenterIterator - 1) * 9 + cloudletIterator * 54;

                        // Pastikan endAssigner tidak melebihi ukuran cloudletList
                        if (endAssigner > cloudletList.size()) {
                            endAssigner = cloudletList.size();
                        }

                        for (int assigner = startAssigner; assigner < endAssigner; assigner++) {
                            // Cari cloudlet dengan ID = assigner secara manual
                            Cloudlet targetCloudlet = null;
                            for (Cloudlet cloudlet : cloudletList) {
                                if (cloudlet.getCloudletId() == assigner) {
                                    targetCloudlet = cloudlet;
                                    break;
                                }
                            }

                            if (targetCloudlet == null) {
                                System.err.println("Cloudlet ID " + assigner + " tidak ditemukan. Skip...");
                                continue;
                            }

                            // Pastikan index bestSolution valid
                            int solutionIndex = assigner % 9;
                            if (solutionIndex >= bestSolution.length) {
                                System.err.println("Index bestSolution invalid: " + solutionIndex);
                                continue;
                            }

                            int vmId = bestSolution[solutionIndex];
                            broker.bindCloudletToVm(targetCloudlet.getCloudletId(), vmId); // Gunakan ID asli cloudlet
                        }
                    }
                }
                
                // Start simulation
                CloudSim.startSimulation();
                CloudSim.stopSimulation();
                
                // Print results
                List<Cloudlet> resultList = broker.getCloudletReceivedList();
                printCloudletList(resultList);
                
                String outputFileName = "cloudlet_output_default.txt";
                if (args.length > 0) {
                    outputFileName = args[0]; // Ambil nama file dari argumen
                }
                saveCloudletListToFile(resultList, outputFileName);
                
                Log.printLine("MOICS Cloud Simulation finished!");
            } catch (Exception e) {
                e.printStackTrace();
                Log.printLine("Simulation terminated due to error");
            }
        }


  private static List<Cloudlet> createCloudlet(int userId, int cloudlets) {
    ArrayList<Double> randomSeed = getSeedValue(cloudlets);

    LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

    long fileSize = 300; 
    long outputSize = 300;
    int pesNumber = 1; 
    UtilizationModel utilizationModel = new UtilizationModelFull();

    for (int i = 0; i < cloudlets; i++) {
      long length = 0;

      if (randomSeed.size() > i) {
        length = Double.valueOf(randomSeed.get(i)).longValue();
      }

      Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
      cloudlet.setUserId(userId);
      list.add(cloudlet);
    }
    Collections.shuffle(list);

    return list;
  }

  private static List<Vm> createVM(int userId, int vms) {
    LinkedList<Vm> list = new LinkedList<Vm>();

    long size = 10000;
    int[] ram = { 512, 1024, 2048 }; 
    int[] mips = { 400, 500, 600 }; 
    long bw = 1000; 
    int pesNumber = 1; 
    String vmm = "Xen"; 

    Vm[] vm = new Vm[vms];

    for (int i = 0; i < vms; i++) {
      vm[i] = new Vm(i, userId, mips[i % 3], pesNumber, ram[i % 3], bw, size, vmm, new CloudletSchedulerSpaceShared());
      list.add(vm[i]);
    }

    return list;
  }

  private static ArrayList<Double> getSeedValue(int cloudletcount) {
    ArrayList<Double> seed = new ArrayList<Double>();
    try {
//      File fobj = new File(System.getProperty("user.dir") + "/datasets/randomSimple/RandSimple"+bot+"000.txt");
//      File fobj = new File(System.getProperty("user.dir") + "/datasets/randomStratified/RandStratified"+bot+"000.txt");
      File fobj = new File(System.getProperty("user.dir") + "/datasets/SDSC/SDSC7395.txt");
//      File fobj = new File(System.getProperty("user.dir") + "/datasets/LowTaskLength/low_task_length_dataset"+bot+"000.txt");
      java.util.Scanner readFile = new java.util.Scanner(fobj);

      while (readFile.hasNextLine() && cloudletcount > 0) {
        seed.add(readFile.nextDouble());
        cloudletcount--;
      }
      readFile.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    return seed;
  }

  private static PowerDatacenter createDatacenter(String name, int hostId) {

    List<PowerHost> hostList = new ArrayList<PowerHost>();

    List<Pe> peList1 = new ArrayList<Pe>();
    List<Pe> peList2 = new ArrayList<Pe>();
    List<Pe> peList3 = new ArrayList<Pe>();

    int mipsunused = 300; 
    int mips1 = 400; 
    int mips2 = 500;
    int mips3 = 600;

    peList1.add(new Pe(0, new PeProvisionerSimple(mips1))); 
    peList1.add(new Pe(1, new PeProvisionerSimple(mips1)));
    peList1.add(new Pe(2, new PeProvisionerSimple(mips1)));
    peList1.add(new Pe(3, new PeProvisionerSimple(mipsunused)));
    peList2.add(new Pe(4, new PeProvisionerSimple(mips2)));
    peList2.add(new Pe(5, new PeProvisionerSimple(mips2)));
    peList2.add(new Pe(6, new PeProvisionerSimple(mips2)));
    peList2.add(new Pe(7, new PeProvisionerSimple(mipsunused)));
    peList3.add(new Pe(8, new PeProvisionerSimple(mips3)));
    peList3.add(new Pe(9, new PeProvisionerSimple(mips3)));
    peList3.add(new Pe(10, new PeProvisionerSimple(mips3)));
    peList3.add(new Pe(11, new PeProvisionerSimple(mipsunused)));

    int ram = 128000;
    long storage = 1000000;
    int bw = 10000;
    int maxpower = 117; 
    int staticPowerPercentage = 50; 

    hostList.add(
        new PowerHostUtilizationHistory(
            hostId, new RamProvisionerSimple(ram),
            new BwProvisionerSimple(bw),
            storage,
            peList1,
            new VmSchedulerTimeShared(peList1),
            new PowerModelLinear(maxpower, staticPowerPercentage)));
    hostId++;

    hostList.add(
        new PowerHostUtilizationHistory(
            hostId, new RamProvisionerSimple(ram),
            new BwProvisionerSimple(bw),
            storage,
            peList2,
            new VmSchedulerTimeShared(peList2),
            new PowerModelLinear(maxpower, staticPowerPercentage)));
    hostId++;

    hostList.add(
        new PowerHostUtilizationHistory(
            hostId, new RamProvisionerSimple(ram),
            new BwProvisionerSimple(bw),
            storage,
            peList3,
            new VmSchedulerTimeShared(peList3),
            new PowerModelLinear(maxpower, staticPowerPercentage)));

    String arch = "x86"; 
    String os = "Linux"; 
    String vmm = "Xen"; 
    double time_zone = 10.0; 
    double cost = 3.0; 
    double costPerMem = 0.05; 
    double costPerStorage = 0.1; 
    double costPerBw = 0.1; 
    LinkedList<Storage> storageList = new LinkedList<Storage>();

    DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
        arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

    PowerDatacenter datacenter = null;
    try {
      datacenter = new PowerDatacenter(name, characteristics, new PowerVmAllocationPolicySimple(hostList), storageList, 9); 
    } catch (Exception e) {
      e.printStackTrace();
    }

    return datacenter;
  }

  private static DatacenterBroker createBroker() {

    DatacenterBroker broker = null;
    try {
      broker = new DatacenterBroker("Broker");
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return broker;
  }

  private static void printCloudletList(List<Cloudlet> list) throws FileNotFoundException {

    // Initializing the printed output to zero
    int size = list.size();
    Cloudlet cloudlet = null;

    String indent = "    ";
    Log.printLine();
    Log.printLine("========== OUTPUT ==========");
    Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
        "Data center ID" + indent + "VM ID" + indent + "Time"
        + indent + "Start Time" + indent + "Finish Time" + indent + "Waiting Time");

    double waitTimeSum = 0.0;
    double CPUTimeSum = 0.0;
    int totalValues = 0;
    DecimalFormat dft = new DecimalFormat("###,##");

    double response_time[] = new double[size];

    // Printing all the status of the Cloudlets
    for (int i = 0; i < size; i++) {
      cloudlet = list.get(i);
      if (cloudlet == null || cloudlet.getCloudletStatus() != Cloudlet.SUCCESS) {
          Log.printLine("Cloudlet " + i + " is null or not completed. Skipping...");
          continue;
      }

      if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
        Log.print("SUCCESS");
        CPUTimeSum = CPUTimeSum + cloudlet.getActualCPUTime();
        waitTimeSum = waitTimeSum + cloudlet.getWaitingTime();
        Log.printLine(
            indent + indent + indent + (cloudlet.getResourceId() - 1) + indent + indent + indent + cloudlet.getVmId() +
                indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent
                + dft.format(cloudlet.getExecStartTime()) +
                indent + indent + dft.format(cloudlet.getFinishTime()) + indent + indent + indent
                + dft.format(cloudlet.getWaitingTime()));
        totalValues++;

        response_time[i] = cloudlet.getActualCPUTime();
      }
    }
    DoubleSummaryStatistics stats = DoubleStream.of(response_time).summaryStatistics();

    Log.printLine();
    System.out.println(String.format("min = %,6f",stats.getMin()));
    System.out.println(String.format("Response_Time: %,6f",CPUTimeSum / totalValues));

    Log.printLine();
    Log.printLine(String.format("TotalCPUTime : %,6f",CPUTimeSum));
    Log.printLine("TotalWaitTime : "+waitTimeSum);
    Log.printLine("TotalCloudletsFinished : "+totalValues);

    // Average Cloudlets Finished
    Log.printLine(String.format("AverageCloudletsFinished : %,6f",(CPUTimeSum / totalValues)));

    
    // Makespan
    double makespan = 0.0;
    double makespan_total = makespan + cloudlet.getFinishTime();
    System.out.println(String.format("Makespan: %,f",makespan_total));
    
    // Total Cost
    double totalCost = 0.0;
    for (Cloudlet cl : list) {
        if (cl.getCloudletStatus() == Cloudlet.SUCCESS) {
            Vm vm = vmList.get(cl.getVmId());
            double executionTime = cl.getActualCPUTime();
            double memoryUsage = vm.getRam(); // Menggunakan RAM dari VM
            
            long bw = 1000;
            double costPerMips = vm.getCostPerMips();
            double costPerMem = 0.05; 
            double costPerBw = 0.1; 

            // Hitung biaya untuk cloudlet
            double cloudletCost = (executionTime * costPerMips) + 
                    (memoryUsage * costPerMem) + 
                    (bw * costPerBw);

            totalCost += cloudletCost;
        }
    }
    // Print Total Cost
    Log.printLine(String.format("Total Cost: $%,2f", totalCost));

    // Average Waiting Time
    double avgWT = cloudlet.getWaitingTime() / size;
    System.out.println(String.format("Average Waiting time: %,6f",avgWT));

    // Throughput
    double maxFT = 0.0;
    for (int i = 0; i < size; i++) {
      double currentFT = cloudletList.get(i).getFinishTime();
      if (currentFT > maxFT) {
        maxFT = currentFT;
      }
    }
    double throughput = size / maxFT;
    System.out.println(String.format("Throughput: %,9f",throughput));

    // CPU Resource Utilization
    double resource_utilization = (CPUTimeSum / (makespan_total * 54)) * 100;
    Log.printLine(String.format("Resouce Utilization: %,f",resource_utilization));

    // Energy Consumption
    Log.printLine(String.format("Total Energy Consumption: %,2f  kWh",
        (datacenter1.getPower() + datacenter2.getPower() + datacenter3.getPower() + datacenter4.getPower()
            + datacenter5.getPower() + datacenter6.getPower()) / (3600 * 1000))); 	
    
  }
  
  private static void saveCloudletListToFile(List<Cloudlet> list, String fileName) {
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
	        int size = list.size();
	        Cloudlet cloudlet = null;
	        String indent = "    ";
	        
	        // Menulis header
	        writer.write("========== OUTPUT ==========");
	        writer.newLine();
	        writer.write("Cloudlet ID" + indent + "STATUS" + indent +
	            "Data center ID" + indent + "VM ID" + indent + "Time"
	            + indent + "Start Time" + indent + "Finish Time" + indent + "Waiting Time");
	        writer.newLine();

	        double waitTimeSum = 0.0;
	        double CPUTimeSum = 0.0;
	        int totalValues = 0;
	        DecimalFormat dft = new DecimalFormat("###,##");
	        double response_time[] = new double[size];

	        // Menulis status setiap cloudlet
	        for (int i = 0; i < size; i++) {
	            cloudlet = list.get(i);
	            if (cloudlet == null || cloudlet.getCloudletStatus() != Cloudlet.SUCCESS) {
	                writer.write("Cloudlet " + i + " is null or not completed. Skipping...");
	                writer.newLine();
	                continue;
	            }
	            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
	                writer.write(cloudlet.getCloudletId() + indent + "SUCCESS" + indent +
	                    (cloudlet.getResourceId() - 1) + indent + indent + cloudlet.getVmId() +
	                    indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent
	                    + dft.format(cloudlet.getExecStartTime()) +
	                    indent + indent + dft.format(cloudlet.getFinishTime()) + indent + indent + indent
	                    + dft.format(cloudlet.getWaitingTime()));
	                writer.newLine();

	                CPUTimeSum += cloudlet.getActualCPUTime();
	                waitTimeSum += cloudlet.getWaitingTime();
	                totalValues++;
	                response_time[i] = cloudlet.getActualCPUTime();
	            }
	        }

	        // Statistik tambahan
	        DoubleSummaryStatistics stats = DoubleStream.of(response_time).summaryStatistics();
	        writer.newLine();
	        writer.write(String.format("min = %,6f", stats.getMin()));
	        writer.newLine();
	        writer.write(String.format("Response_Time: %,6f", CPUTimeSum / totalValues));
	        writer.newLine();
	        writer.write(String.format("TotalCPUTime : %,6f", CPUTimeSum));
	        writer.newLine();
	        writer.write("TotalWaitTime : " + waitTimeSum);
	        writer.newLine();
	        writer.write("TotalCloudletsFinished : " + totalValues);
	        writer.newLine();
	        writer.write(String.format("AverageCloudletsFinished : %,6f", (CPUTimeSum / totalValues)));
	        writer.newLine();

	        // Makespan
	        double makespan = 0.0;
	        double makespan_total = makespan + cloudlet.getFinishTime();
	        writer.write(String.format("Makespan: %,f", makespan_total));
	        writer.newLine();

	        // Total Cost
	        double totalCost = 0.0;
	        for (Cloudlet cl : list) {
	            if (cl.getCloudletStatus() == Cloudlet.SUCCESS) {
	                Vm vm = vmList.get(cl.getVmId());
	                double executionTime = cl.getActualCPUTime();
	                double memoryUsage = vm.getRam(); // Menggunakan RAM dari VM
	                long bw = 1000;
	                double costPerMips = vm.getCostPerMips();
	                double costPerMem = 0.05;
	                double costPerBw = 0.1;

	                // Hitung biaya untuk cloudlet
	                double cloudletCost = (executionTime * costPerMips) +
	                        (memoryUsage * costPerMem) +
	                        (bw * costPerBw);
	                totalCost += cloudletCost;
	            }
	        }
	        writer.write(String.format("Total Cost: $%,2f", totalCost));
	        writer.newLine();

	        // Average Waiting Time
	        double avgWT = cloudlet.getWaitingTime() / size;
	        writer.write(String.format("Average Waiting time: %,6f", avgWT));
	        writer.newLine();

	        // Throughput
	        double maxFT = 0.0;
	        for (int i = 0; i < size; i++) {
	            double currentFT = cloudletList.get(i).getFinishTime();
	            if (currentFT > maxFT) {
	                maxFT = currentFT;
	            }
	        }
	        double throughput = size / maxFT;
	        writer.write(String.format("Throughput: %,9f", throughput));
	        writer.newLine();

	        // CPU Resource Utilization
	        double resource_utilization = (CPUTimeSum / (makespan_total * 54)) * 100;
	        writer.write(String.format("Resource Utilization: %,f", resource_utilization));
	        writer.newLine();

	        // Energy Consumption
	        writer.write(String.format("Total Energy Consumption: %,2f kWh",
	            (datacenter1.getPower() + datacenter2.getPower() + datacenter3.getPower() + datacenter4.getPower()
	                + datacenter5.getPower() + datacenter6.getPower()) / (3600 * 1000)));
	        writer.newLine();

	        writer.write("Output saved successfully to " + fileName);
	        writer.newLine();

	    } catch (IOException e) {
	        e.printStackTrace();
	        Log.printLine("Error saving cloudlet list to file: " + fileName);
	    }
	}

}
