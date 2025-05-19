package com.os.rados;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

class ProcessManager {
    public List<PCB> processes;
    private int nextPid;
    List<List<Integer>> resourceAllocation; // Process -> Resources held
    List<List<Integer>> resourceRequests;   // Process -> Resources requested
    private List<Integer> resources; // List of resource IDs

    public ProcessManager() {
        processes = new ArrayList<>();
        nextPid = 1;
        resourceAllocation = new ArrayList<>();
        resourceRequests = new ArrayList<>();
        resources = new ArrayList<>();
    }

    public void createProcess(String name) {
        PCB process = new PCB(nextPid++, name);
        processes.add(process);
        resourceAllocation.add(new ArrayList<>());
        resourceRequests.add(new ArrayList<>());
        System.out.println("Process created: PID=" + process.getPid() + ", Name=" + name);
    }

    public void schedule() {
        for (PCB process : processes) {
            if (process.isActive() && process.getState().equals("READY")) {
                process.setState("RUNNING");
                System.out.println("Running: PID=" + process.getPid() + ", Name=" + process.getName());
                try {
                    Thread.sleep(5000); // Simulate 5 seconds of running
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                process.setState("READY");
            }
        }
    }

    public String listProcesses() {
        String output = "";
        output += "PID\t\tName\t\t\tState\n";
        output += "--------------------------------\n";
        for (PCB process : processes) {
            output += String.format("%-15d %-20s %-10s%n", process.getPid(), process.getName(), process.getState());
        }
        return output;
    }

    public PCB getProcessByPid(int pid) {
        for (PCB process : processes) {
            if (process.getPid() == pid) {
                return process;
            }
        }
        return null;
    }

    public void addResource(int pid, int resourceId) {
        if (pid <= resourceAllocation.size()) {
            resourceAllocation.get(pid - 1).add(resourceId);
            resourceRequests.get(pid - 1).remove((Integer) resourceId); // Clear request
            PCB process = getProcessByPid(pid);
            if (process != null && process.getState().equals("BLOCKED")) {
                process.setState("READY"); // Move back to READY after acquiring resource
            }
            if (!resources.contains(resourceId)) {
                resources.add(resourceId);
            }
        }
    }

    public void removeResource(int pid, int resourceId, boolean isHeld) {
        if (pid <= resourceAllocation.size()) {
            if (isHeld) {
                resourceAllocation.get(pid - 1).remove((Integer) resourceId);
                // Notify processes waiting for this resource
                notifyWaitingProcesses(resourceId);
            }
            resourceRequests.get(pid - 1).remove((Integer) resourceId);
            if (isHeld) {
                PCB process = getProcessByPid(pid);
                if (process != null && process.getState().equals("BLOCKED")) {
                    process.setState("READY"); // Clear BLOCKED state if resource is freed
                }
            }
        }
    }

    public void requestResource(int pid, int resourceId, int pidHolding) {
        if (pid <= resourceRequests.size()) {
            if (!resourceRequests.get(pid - 1).contains(resourceId)) {
                resourceRequests.get(pid - 1).add(resourceId);
                PCB process = getProcessByPid(pid);
                if (process != null) {
                    process.setState("BLOCKED"); // Set to BLOCKED when waiting
                }
            }
            // Add implicit request: pidHolding requests a resource held by pid
            if (pidHolding > 0 && pidHolding <= resourceAllocation.size()) {
                for (Integer heldResource : resourceAllocation.get(pid - 1)) {
                    if (!resourceRequests.get(pidHolding - 1).contains(heldResource)) {
                        resourceRequests.get(pidHolding - 1).add(heldResource);
                        PCB holdingProcess = getProcessByPid(pidHolding);
                        if (holdingProcess != null) {
                            holdingProcess.setState("BLOCKED");
                        }
                    }
                }
            }
        }
    }

    public boolean detectDeadlock(int pid, int resourceId) {
        // Temporarily add the request
        List<Integer> tempRequests = new ArrayList<>(resourceRequests.get(pid - 1));
        if (!tempRequests.contains(resourceId)) {
            tempRequests.add(resourceId);
        }

        HashSet<Integer> visited = new HashSet<>();
        HashSet<Integer> recStack = new HashSet<>();
        return hasCycle(pid - 1, visited, recStack, tempRequests);
    }

    private boolean hasCycle(int pid, HashSet<Integer> visited, HashSet<Integer> recStack, List<Integer> tempRequests) {
        if (!visited.contains(pid)) {
            visited.add(pid);
            recStack.add(pid);

            // Use tempRequests for the requesting process, otherwise use resourceRequests
            List<Integer> requests = (pid == processes.size() - 1 && tempRequests != null) ? tempRequests : resourceRequests.get(pid);

            for (Integer resource : requests) {
                // Find processes holding this resource
                for (int i = 0; i < resourceAllocation.size(); i++) {
                    if (resourceAllocation.get(i).contains(resource)) {
                        if (!visited.contains(i)) {
                            if (hasCycle(i, visited, recStack, resourceRequests.get(i))) {
                                return true;
                            }
                        } else if (recStack.contains(i)) {
                            return true; // Cycle detected
                        }
                    }
                }
            }
        }
        recStack.remove(pid);
        return false;
    }

    private void notifyWaitingProcesses(int resourceId) {
        // Find processes requesting the freed resource
        for (int i = 0; i < resourceRequests.size(); i++) {
            if (resourceRequests.get(i).contains(resourceId)) {
                PCB process = getProcessByPid(i + 1);
                if (process != null && process.getState().equals("BLOCKED")) {
                    process.setState("READY"); // Transition to READY
                }
            }
        }
    }
}