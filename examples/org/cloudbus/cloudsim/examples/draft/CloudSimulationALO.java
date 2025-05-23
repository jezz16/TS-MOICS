package org.cloudbus.cloudsim.examples.draft;

import java.util.Locale;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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

public class CloudSimulationALO {
  private static PowerDatacenter datacenter1, datacenter2, datacenter3, datacenter4, datacenter5, datacenter6;
  /** The cloudlet list. */
  private static List<Cloudlet> cloudletList;

  /** The vmlist. */
  private static List<Vm> vmlist;
  // private static int bot=1+1+1+1+1+1+1+1+1+1+1;

  private static List<Vm> createVM(int userId, int vms) {

    // Creates a container to store VMs.
    // This list is passed to the broker later
    LinkedList<Vm> list = new LinkedList<Vm>();

    // VM Parameters
    long size = 10000; // Image size (MB)
    int[] ram = { 512, 1024, 2048 }; // VM memory (MB)
    int[] mips = { 400, 500, 600 }; // VM processing power (MIPS)
    long bw = 1000; // VM bandwith
    int pesNumber = 1; // Number of cpus
    String vmm = "Xen"; // VMM name

    // create VMs
    Vm[] vm = new Vm[vms];

    for (int i = 0; i < vms; i++) {
      vm[i] = new Vm(i, userId, mips[i % 3], pesNumber, ram[i % 3], bw, size, vmm, new CloudletSchedulerSpaceShared());
      list.add(vm[i]);
    }

    return list;
  }

  private static ArrayList<Double> getSeedValue(int cloudletcount) {
    // Creating an arraylist to store Cloudlet Datasets
    ArrayList<Double> seed = new ArrayList<Double>();
    // Log.printLine(System.getProperty("user.dir")+
    // "/dataset/RandSimple"+bot+"000.txt");
//    Log.printLine(System.getProperty("user.dir") + "/datasets/randSimple/RandSimple10000.txt");
    Log.printLine(System.getProperty("user.dir") + "/datasets/SDSC/SDSC7395.txt");
//    Log.printLine(System.getProperty("user.dir") + "/datasets/randomStratified/RandStratified1000.txt");

    try {
      // Opening and scanning the file
      // File fobj = new File(System.getProperty("user.dir")+
      // "/dataset/RandSimple"+bot+"000.txt");
      File fobj = new File(System.getProperty("user.dir") + "/cloudsim-3.0.3/datasets/randomSimple/RandSimple1000.txt");
//    	File fobj = new File(System.getProperty("user.dir") + "/datasets/randomStratified/RandStratified1000.txt");
//      File fobj = new File(System.getProperty("user.dir") + "/cloudsim-3.0.3/datasets/SDSC/SDSC7395.txt");
      java.util.Scanner readFile = new java.util.Scanner(fobj);

      while (readFile.hasNextLine() && cloudletcount > 0) {
        // Adding the file to the arraylist
        seed.add(readFile.nextDouble());
        cloudletcount--;
      }
      readFile.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    return seed;
  }

  private static List<Cloudlet> createCloudlet(int userId, int cloudlets) {
    ArrayList<Double> randomSeed = getSeedValue(cloudlets);

    // Creates a container to store Cloudlets
    LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

    // Cloudlet parameters
    long fileSize = 300; // Cloudlet file size (MB)
    long outputSize = 300; // Cloudlet output file size (MB)
    int pesNumber = 1; // Number of CPUs
    UtilizationModel utilizationModel = new UtilizationModelFull();

    for (int i = 0; i < cloudlets; i++) {
      long length = 0; // Initialize length

      // If there are random seeds available, use them to set the length
      if (randomSeed.size() > i) {
        length = Double.valueOf(randomSeed.get(i)).longValue();
        // System.out.println("seed length: " + length);
      }

      // Creating the cloudlet with the length
      Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel,
          utilizationModel);
      cloudlet.setUserId(userId);
      list.add(cloudlet);
    }
    Collections.shuffle(list);

    return list;
  }

  /**
   * Creates main() to run this example
   */
  public static void main(String[] args) {
	Locale.setDefault(new Locale("de", "DE"));
    Log.printLine("Starting Cloud Simulation Example...");

    try {
      // First step: Initialize the CloudSim package. It should be called
      // before creating any entities.
      int num_user = 1; // Number of grid users
      Calendar calendar = Calendar.getInstance();
      boolean trace_flag = false; // Mean trace events
      int hostId = 0; // Starting host ID
      BufferedWriter outputWriter = null;
      outputWriter = new BufferedWriter(new FileWriter("filename.txt")); // Save output to text file
      int vmNumber = 54; // The number of VMs created
      // int cloudletNumber = bot*1000; // The number of Tasks created
      // int cloudletNumber = 7395; // SDSC Dataset
      int cloudletNumber = 1000;
//      int cloudletNumber = 2000;
//      int cloudletNumber = 3000;
//      int cloudletNumber = 4000;
//      int cloudletNumber = 5000;
//      int cloudletNumber = 6000;
//      int cloudletNumber = 7000;
//      int cloudletNumber = 8000;
//      int cloudletNumber = 9000;
//      int cloudletNumber = 10000;
//      int cloudletNumber = 7395;
      // Initialize the CloudSim library
      CloudSim.init(num_user, calendar, trace_flag);

      // Second step: Create Datacenters
      // Datacenters are the resource providers in CloudSim. We need at least one of
      // them to run a CloudSim simulation
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

      // Third step: Create Broker
      DatacenterBroker broker = createBroker();
      int brokerId = broker.getId();

      // Fourth step: Create VMs and Cloudlets
      vmlist = createVM(brokerId, vmNumber); // Creating vms
      cloudletList = createCloudlet(brokerId, cloudletNumber); // Creating cloudlets

      // Fifth step: Send VMs and Cloudlets to broker
      broker.submitVmList(vmlist);
      broker.submitCloudletList(cloudletList);

      int cloudletLoopingNumber = cloudletNumber / vmNumber - 1; // number of iteration needed to process the dataset

      for (int cloudletIterator = 0; cloudletIterator <= cloudletLoopingNumber; cloudletIterator++) {
        System.out.println("Cloudlet Iteration Number " + cloudletIterator);

        for (int dataCenterIterator = 1; dataCenterIterator <= 6; dataCenterIterator++) {
          // Initialize Antlion Optimizer algorithm
          // SquirrelSearchAlgorithm ssa = new SquirrelSearchAlgorithm(75, 0,
          // cloudletList, vmlist);
          // ALOAlgorithm ALO = new ALOAlgorithm(30, 40, cloudletList, vmlist);
          ALOAlgorithm ALO = new ALOAlgorithm(40, 35, cloudletList, vmlist);

          // Initialize population
          System.out.println("Datacenter " + dataCenterIterator + " Population Initialization");
          // Population population = SSA.initPopulation(cloudletNumber,
          // dataCenterIterator); //ganti ini
          Population Antpopulation = ALO.initPopulationAnt(cloudletNumber, dataCenterIterator);
          Population Antlionpopulation = ALO.initPopulationAntlion(cloudletNumber, dataCenterIterator);

          // Evaluate population33
          ALO.evalPopulation(Antpopulation, dataCenterIterator, cloudletIterator);
          ALO.evalPopulation(Antlionpopulation, dataCenterIterator, cloudletIterator);

          // iteration
          int iteration = 1;
          while (iteration <= 15) {
            // Get the fittest individual from population in every iteration
            Individual bestAntlion = Antlionpopulation.getFittest(iteration); // elite antlion = best solution for now

            // random walk

            // int minGene = dataCenterIterator * 9;
            // int maxGene = (dataCenterIterator + 1) * 9 - 1;
            int minGene = (dataCenterIterator - 1) * 9;
            int maxGene = ((dataCenterIterator) * 9) - 1;
            // min 1*9 = 9 | 2*9 = 18
            // maax 2*9-1 = 17???| 3*9-1 = 26
            for (Individual ant : Antpopulation.getIndividuals()) {
              ALO.randomWalk(ant, minGene, maxGene);
              // ALO.randomWalk(ant, 0, 8);
              // ALO.randomWalk(ant, 0, 53);
            } 

            ALO.trapAnts(Antpopulation, Antlionpopulation, dataCenterIterator, cloudletIterator);

             ALO.updateAntlionPopulation(Antpopulation, Antlionpopulation);
//            ALO.updateAntlionPopulationEOBL(Antpopulation, Antlionpopulation, dataCenterIterator);

            System.out.println(" ");
            for (int j = 0; j < 9; j++) {
              System.out.print(bestAntlion.getGene(j) + " ");
            }
            System.out.println("  fitness => " + ALO.calcFitness(bestAntlion, dataCenterIterator, iteration));

            // Increment the current iteration
            iteration++;
          }

          // Get the fittest individual from the Squirrel Search Algorithm
          System.out.println("Best solution of ALO: " + Antlionpopulation.getFittest(dataCenterIterator)
              + " For Datacenter-" + dataCenterIterator);
          System.out
              .println("Highest Fitness Achieved: " + Antlionpopulation.getFittest(dataCenterIterator).getFitness());

          // Assign Cloudlet to their respective VMs according to the fittest individual's
          // chromosome
          for (int assigner = 0 + (dataCenterIterator - 1) * 9 + cloudletIterator * 54; assigner < 9
              + (dataCenterIterator - 1) * 9 + cloudletIterator * 54; assigner++) {
            // broker.bindCloudletToVm(assigner,
            // population.getFittest(dataCenterIterator).getGene(assigner % 9));
            broker.bindCloudletToVm(assigner, Antlionpopulation.getFittest(dataCenterIterator)
                .getGene(assigner - (dataCenterIterator - 1) * 9 - cloudletIterator * 54));
            outputWriter.write(Long.toString(Antlionpopulation.getFittest(dataCenterIterator).getGene(assigner % 9) % 9)); // Print
                                                                                                                       // Assigned
                                                                                                                       // VM
                                                                                                                       // ID
                                                                                                                       // %
            outputWriter.write(" ");
          }

          outputWriter.newLine();
        }
      }

      // Seventh step: Starts the simulation
      CloudSim.startSimulation();

      outputWriter.flush();
      outputWriter.close();
      // Final step: Print results when simulation is over
      List<Cloudlet> newList = broker.getCloudletReceivedList();

      CloudSim.stopSimulation();

      printCloudletList(newList);

      Log.printLine("Cloud Simulation Example finished!");
    } catch (Exception e) {
      e.printStackTrace();
      Log.printLine("The simulation has been terminated due to an unexpected error");
    }
  }

  private static PowerDatacenter createDatacenter(String name, int hostId) {

    // Here are the steps needed to create a PowerDatacenter:
    // 1. We need to create a list to store one or more machines
    List<PowerHost> hostList = new ArrayList<PowerHost>();

    // 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
    // create a list to store these PEs before creating a Machine.
    List<Pe> peList1 = new ArrayList<Pe>();
    List<Pe> peList2 = new ArrayList<Pe>();
    List<Pe> peList3 = new ArrayList<Pe>();

    int mipsunused = 300; // Unused core, only 3 cores will be able to process Cloudlets for this
                          // simulation
    int mips1 = 400; // The MIPS Must be bigger than the VMs
    int mips2 = 500;
    int mips3 = 600;

    // 3. Create PEs and add these into the list.
    // for a quad-core machine, a list of 4 PEs is required:
    peList1.add(new Pe(0, new PeProvisionerSimple(mips1))); // need to store Pe id and MIPS Rating, Must be bigger than
                                                            // the VMs
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

    // 4. Create Hosts with its id and list of PEs and add them to the list of
    // machines
    int ram = 128000; // Host memory (MB), Must be bigger than the VMs
    long storage = 1000000; // Host storage (MB)
    int bw = 10000; // Host bandwith
    int maxpower = 117; // Host Max Power
    int staticPowerPercentage = 50; // Host Static Power Percentage

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

    // 5. Create a DatacenterCharacteristics object that stores the
    // properties of a data center: architecture, OS, list of
    // Machines, allocation policy: time- or space-shared, time zone
    // and its price (G$/Pe time unit).
    String arch = "x86"; // System architecture
    String os = "Linux"; // Operating system
    String vmm = "Xen"; // Name
    double time_zone = 10.0; // Time zone this resource located
    double cost = 3.0; // The cost of using processing in this resource
    double costPerMem = 0.05; // The cost of using memory in this resource
    double costPerStorage = 0.1; // The cost of using storage in this resource
    double costPerBw = 0.1; // The cost of using bw in this resource
    LinkedList<Storage> storageList = new LinkedList<Storage>();

    DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
        arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

    // 6. Finally, we need to create a PowerDatacenter object.
    PowerDatacenter datacenter = null;
    try {
      datacenter = new PowerDatacenter(name, characteristics, new PowerVmAllocationPolicySimple(hostList), storageList,
          9); // 15 --> is the cloud scheduling interval
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

  /**
   * Prints the Cloudlet objects
   * 
   * @param list list of Cloudlets
   * @throws FileNotFoundException
   */
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
      Log.print(cloudlet.getCloudletId() + indent + indent);

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

    // Show the parameters and print them out
    Log.printLine();
    System.out.println(String.format("min = %,6f",stats.getMin()));
    System.out.println(String.format("Response_Time: %,6f",CPUTimeSum / totalValues));

    Log.printLine();
    Log.printLine(String.format("TotalCPUTime : %,6f",CPUTimeSum));
    Log.printLine("TotalWaitTime : "+waitTimeSum);
    Log.printLine("TotalCloudletsFinished : "+totalValues);
//    Log.printLine();
//    Log.printLine();

    // Average Cloudlets Finished
    Log.printLine(String.format("AverageCloudletsFinished : %,6f",(CPUTimeSum / totalValues)));

    // Average Start Time
    double totalStartTime = 0.0;
    for (int i = 0; i < size; i++) {
      totalStartTime += cloudletList.get(i).getExecStartTime();
    }
    double avgStartTime = totalStartTime / size;
    System.out.println(String.format("Average StartTime: %,6f",avgStartTime));

    // Average Execution Time
    double ExecTime = 0.0;
    for (int i = 0; i < size; i++) {
      ExecTime += cloudletList.get(i).getActualCPUTime();
    }
    double avgExecTime = ExecTime / size;
    System.out.println(String.format("Average Execution Time: %,6f",avgExecTime));

    // Average Finish Time
    double totalTime = 0.0;
    for (int i = 0; i < size; i++) {
      totalTime += cloudletList.get(i).getFinishTime();
    }
    double avgTAT = totalTime / size;
    System.out.println(String.format("Average FinishTime: %,6f",avgTAT));

    // Average Waiting Time
    double avgWT = cloudlet.getWaitingTime() / size;
    System.out.println(String.format("Average Waiting time: %,6f",avgWT));
//
//    Log.printLine();
//    Log.printLine();

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

    // Makespan
    double makespan = 0.0;
    double makespan_total = makespan + cloudlet.getFinishTime();
    System.out.println(String.format("Makespan: %,f",makespan_total));

    // Imbalance Degree
    double degree_of_imbalance = (stats.getMax() - stats.getMin()) / (CPUTimeSum / totalValues);
    System.out.println(String.format("Imbalance Degree: %,3f",degree_of_imbalance));

    // Scheduling Length
    double scheduling_length = waitTimeSum + makespan_total;
    Log.printLine(String.format("Total Scheduling Length: %,f", scheduling_length));

    // CPU Resource Utilization
    double resource_utilization = (CPUTimeSum / (makespan_total * 54)) * 100;
    Log.printLine(String.format("Resouce Utilization: %,f",resource_utilization));

    // Energy Consumption
    Log.printLine(String.format("Total Energy Consumption: %,2f  kWh",
        (datacenter1.getPower() + datacenter2.getPower() + datacenter3.getPower() + datacenter4.getPower()
            + datacenter5.getPower() + datacenter6.getPower()) / (3600 * 1000)));
  }

}