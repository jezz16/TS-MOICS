package org.cloudbus.cloudsim.examples;

import java.util.LinkedList;
import java.util.Queue;

class Process {
    int id; // Process ID
    int burstTime; // Total burst time
    int remainingTime; // Remaining time
    int waitingTime; // Waiting time
    int turnaroundTime; // Turnaround time

    public Process(int id, int burstTime) {
        this.id = id;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
    }
}

public class RoundRobin {
    private int timeQuantum; // Time quantum for each process

    public RoundRobin(int timeQuantum) {
        this.timeQuantum = timeQuantum;
    }

    public void schedule(Queue<Process> processQueue) {
        int time = 0; // Current time

        while (!processQueue.isEmpty()) {
            Process currentProcess = processQueue.poll(); // Get the next process

            if (currentProcess.remainingTime > timeQuantum) {
                // Process runs for the time quantum
                time += timeQuantum;
                currentProcess.remainingTime -= timeQuantum;
                processQueue.add(currentProcess); // Re-add to the queue
            } else {
                // Process finishes execution
                time += currentProcess.remainingTime;
                currentProcess.waitingTime = time - currentProcess.burstTime; // Calculate waiting time
                currentProcess.turnaroundTime = time; // Calculate turnaround time
                currentProcess.remainingTime = 0; // Mark as finished
                System.out.println("Process " + currentProcess.id + " finished. Waiting Time: " + currentProcess.waitingTime + ", Turnaround Time: " + currentProcess.turnaroundTime);
            }
        }
    }
}
