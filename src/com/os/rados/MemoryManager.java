package com.os.rados;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MemoryManager {
    private int[] memory;
    private static final int MEMORY_SIZE = 100;
    private static final int PAGE_SIZE = 10;
    private static final int NUM_PAGES = MEMORY_SIZE / PAGE_SIZE;
    private Map<Integer, List<Integer>> resourceToPages; // Maps resourceId to list of page indices

    public MemoryManager() {
        memory = new int[MEMORY_SIZE];
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = -1; // -1 indicates free memory
        }
        resourceToPages = new HashMap<>();
    }

    public boolean allocate(int pid, int size, int resourceId) {
        // Calculate number of pages needed
        int pagesNeeded = (int) Math.ceil((double) size / PAGE_SIZE);

        // Check if resourceId is already allocated to another process
        if (resourceToPages.containsKey(resourceId)) {
            List<Integer> pages = resourceToPages.get(resourceId);
            int currentPid = memory[pages.get(0) * PAGE_SIZE];
            if (currentPid != -1 && currentPid != pid) {
                System.out.println("Resource " + resourceId + " is held by PID=" + currentPid + "; PID=" + pid + " must wait");
                return false; // Resource is held, process must wait
            }
        }

        // Find free pages
        List<Integer> freePages = new ArrayList<>();
        for (int i = 0; i < NUM_PAGES; i++) {
            boolean pageFree = true;
            for (int j = i * PAGE_SIZE; j < (i + 1) * PAGE_SIZE; j++) {
                if (memory[j] != -1) {
                    pageFree = false;
                    break;
                }
            }
            if (pageFree) {
                freePages.add(i);
            }
            if (freePages.size() >= pagesNeeded) {
                break;
            }
        }

        if (freePages.size() >= pagesNeeded) {
            // Allocate pages
            List<Integer> allocatedPages = new ArrayList<>();
            for (int i = 0; i < pagesNeeded; i++) {
                int page = freePages.get(i);
                allocatedPages.add(page);
                for (int j = page * PAGE_SIZE; j < (page + 1) * PAGE_SIZE; j++) {
                    memory[j] = pid;
                }
            }
            // Associate resourceId with allocated pages
            resourceToPages.put(resourceId, allocatedPages);
            System.out.println("Allocated " + pagesNeeded + " pages (" + (pagesNeeded * PAGE_SIZE) + " units) for PID=" + pid + " with resourceId=" + resourceId);
            return true;
        } else {
            System.out.println("Allocation failed for PID=" + pid + ": insufficient free pages");
            return false;
        }
    }

    public void free(int pid) {
        boolean found = false;
        List<Integer> resourcesToRemove = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : resourceToPages.entrySet()) {
            int resourceId = entry.getKey();
            List<Integer> pages = entry.getValue();
            if (!pages.isEmpty() && memory[pages.get(0) * PAGE_SIZE] == pid) {
                resourcesToRemove.add(resourceId);
                for (int page : pages) {
                    for (int j = page * PAGE_SIZE; j < (page + 1) * PAGE_SIZE; j++) {
                        memory[j] = -1;
                    }
                }
                found = true;
            }
        }
        // Remove resource mappings
        for (int resourceId : resourcesToRemove) {
            resourceToPages.remove(resourceId);
        }
        if (found) {
            System.out.println("Freed memory for PID=" + pid);
        } else {
            System.out.println("No memory allocated for PID=" + pid);
        }
    }

    public String printMemory() {
        String output = "";
        output += ("Memory Layout (Paged, " + PAGE_SIZE + " units per page):\n");
        output += ("Page\t\tStatus\t\tPID\t\tResourceID\n");
        for (int i = 0; i < NUM_PAGES; i++) {
            int pid = memory[i * PAGE_SIZE];
            boolean consistent = true;
            for (int j = i * PAGE_SIZE; j < (i + 1) * PAGE_SIZE; j++) {
                if (memory[j] != pid) {
                    consistent = false;
                    break;
                }
            }
            String status = pid == -1 ? "Free" : "Allocated";
            String pidDisplay = pid == -1 ? "-" : String.valueOf(pid);
            String resourceDisplay = "-";
            for (Map.Entry<Integer, List<Integer>> entry : resourceToPages.entrySet()) {
                if (entry.getValue().contains(i)) {
                    resourceDisplay = String.valueOf(entry.getKey());
                    break;
                }
            }
            output += ("Page " + i + "\t\t" + status + "\t\t" + pidDisplay + "\t\t" + resourceDisplay + "\n");
        }
        // Detailed visualization
        output += ("\nDetailed Memory Map:\n");
        for (int i = 0; i < MEMORY_SIZE; i += PAGE_SIZE) {
            output += ("Page " + (i / PAGE_SIZE) + " [" + i + "-" + (i + PAGE_SIZE - 1) + "] ");
            for (int j = i; j < i + PAGE_SIZE; j++) {
                output += (memory[j] == -1 ? "." : memory[j]);
            }
            output += "\n";
        }
        return output;
    }

    // Helper method to check if a resource is held by another process
    public int getPidHoldingResource(int resourceId) {
        if (resourceToPages.containsKey(resourceId)) {
            List<Integer> pages = resourceToPages.get(resourceId);
            return memory[pages.get(0) * PAGE_SIZE];
        }
        return -1;
    }
}