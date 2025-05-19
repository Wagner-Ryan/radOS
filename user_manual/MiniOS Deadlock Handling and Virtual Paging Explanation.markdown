# Detailed Explanation of Deadlock Handling and Virtual Paging in MiniOS

The MiniOS simulator, implemented in Java, is designed to emulate core operating system concepts, including process management, memory allocation, and resource handling. Two critical features are **deadlock handling** and **virtual paging with memory visualization**, which are central to its functionality. This document provides an in-depth analysis of how these features are implemented, their underlying algorithms, data structures, and interactions, with references to the code and examples from the simulator’s output.

## 1. Deadlock Handling

Deadlock handling in MiniOS is implemented to detect and prevent deadlocks, a situation where processes are unable to proceed because each holds resources needed by others, forming a circular wait. The simulator uses a **Resource Allocation Graph (RAG)** to model resource dependencies and detect cycles, which indicate potential deadlocks. When a deadlock is detected, the allocation is denied, and processes remain in a BLOCKED state until resources are freed.

### 1.1 Key Components
- **ProcessManager**: Manages processes and resources, handling the RAG and deadlock detection logic (`ProcessManager.java`).
- **MemoryManager**: Tracks resource allocations and checks for contention (`MemoryManager.java`).
- **PCB**: Represents process control blocks with states (READY, RUNNING, BLOCKED) (`PCB.java`).
- **Main**: Processes user commands and coordinates deadlock handling (`Main.java`).

### 1.2 Data Structures
- **resourceAllocation**: A `List<List<Integer>>` where `resourceAllocation.get(i)` lists resource IDs held by process `i+1` (PID=1 is index 0).
  ```java
  private List<List<Integer>> resourceAllocation; // Process -> Resources held
  ```
- **resourceRequests**: A `List<List<Integer>>` where `resourceRequests.get(i)` lists resource IDs requested by process `i+1`.
  ```java
  private List<List<Integer>> resourceRequests;   // Process -> Resources requested
  ```
- **resourceToPages**: A `Map<Integer, List<Integer>>` in `MemoryManager` mapping resource IDs to allocated memory pages, used to identify which process holds a resource.
  ```java
  private Map<Integer, List<Integer>> resourceToPages; // Maps resourceId to list of page indices
  ```

### 1.3 Deadlock Detection Algorithm
MiniOS uses a **cycle detection algorithm** based on Depth-First Search (DFS) to identify circular waits in the RAG. The RAG is a directed graph where:
- **Nodes**: Processes and resources.
- **Edges**:
  - **Request edges**: Process → Resource (process requests a resource).
  - **Allocation edges**: Resource → Process (process holds a resource).

The algorithm is implemented in `ProcessManager.detectDeadlock` and `hasCycle`.

#### Steps in Deadlock Detection
1. **Trigger**: Called during an `alloc [pid] [size] [resourceId]` command when the resource is held by another process.
   - In `Main.java`:
     ```java
     int pidHolding = mm.getPidHoldingResource(resourceId);
     if (pidHolding != -1 && pidHolding != pid) {
         pm.requestResource(pid, resourceId, pidHolding);
         if (pm.detectDeadlock(pid, resourceId)) {
             System.out.println("Allocation denied for PID=" + pid + ": Potential deadlock detected");
         }
     }
     ```

2. **Build Temporary Request**:
   - `detectDeadlock(pid, resourceId)` creates a temporary request list for the process, simulating the new request:
     ```java
     List<Integer> tempRequests = new ArrayList<>(resourceRequests.get(pid - 1));
     if (!tempRequests.contains(resourceId)) {
         tempRequests.add(resourceId);
     }
     ```

3. **Cycle Detection (hasCycle)**:
   - Uses DFS to traverse the RAG, starting from the requesting process (`pid - 1`).
   - Maintains two sets:
     - `visited`: Tracks visited processes.
     - `recStack`: Tracks processes in the current DFS path (recursion stack).
     ```java
     HashSet<Integer> visited = new HashSet<>();
     HashSet<Integer> recStack = new HashSet<>();
     return hasCycle(pid - 1, visited, recStack, tempRequests);
     ```
   - In `hasCycle`:
     - Mark the process as visited and add to `recStack`.
     - For each requested resource in `tempRequests` (or `resourceRequests` for other processes):
       - Find processes holding the resource via `resourceAllocation`.
       - Recursively visit those processes.
       - If a process is revisited and in `recStack`, a cycle is detected.
     ```java
     if (!visited.contains(pid)) {
         visited.add(pid);
         recStack.add(pid);
         List<Integer> requests = (pid == processes.size() - 1 && tempRequests != null) ? tempRequests : resourceRequests.get(pid);
         for (Integer resource : requests) {
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
     ```

4. **Implicit Requests**:
   - To ensure cycle formation, `requestResource` adds implicit requests. When Process1 (PID=1) requests Resource1 (held by PID=2), Process2 is assumed to request Resource2 (held by PID=1), simulating mutual waiting.
     ```java
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
     ```

### 1.4 Deadlock Prevention
- When a cycle is detected, the allocation is denied, and the process remains BLOCKED:
  ```java
  if (pm.detectDeadlock(pid, resourceId)) {
      System.out.println("Allocation denied for PID=" + pid + ": Potential deadlock detected");
  }
  ```
- The request is kept in `resourceRequests`, and the process stays BLOCKED until the resource is freed (via `free`).

### 1.5 Resource Release
- The `free [pid]` command releases resources and notifies waiting processes:
  - In `Main.java`:
    ```java
    mm.free(pid);
    if (pid <= pm.resourceAllocation.size()) {
        List<Integer> heldResources = new ArrayList<>(pm.resourceAllocation.get(pid - 1));
        for (Integer resourceId : heldResources) {
            pm.removeResource(pid, resourceId, true);
        }
        List<Integer> requestedResources = new ArrayList<>(pm.resourceRequests.get(pid - 1));
        for (Integer resourceId : requestedResources) {
            pm.removeResource(pid, resourceId, false);
        }
    }
    ```
  - In `ProcessManager.removeResource`:
    ```java
    if (isHeld) {
        resourceAllocation.get(pid - 1).remove((Integer) resourceId);
        notifyWaitingProcesses(resourceId);
    }
    ```
  - `notifyWaitingProcesses` transitions processes waiting for the freed resource to READY:
    ```java
    private void notifyWaitingProcesses(int resourceId) {
        for (int i = 0; i < resourceRequests.size(); i++) {
            if (resourceRequests.get(i).contains(resourceId)) {
                PCB process = getProcessByPid(i + 1);
                if (process != null && process.getState().equals("BLOCKED")) {
                    process.setState("READY");
                }
            }
        }
    }
    ```

### 1.6 Example from Output
Consider the test scenario:
```
> create process1
> create process2
> alloc 1 20 2
> alloc 2 20 1
> alloc 1 10 1
```

- **Initial State**:
  - After `alloc 1 20 2`:
    - `resourceAllocation[0] = [2]` (PID=1 holds Resource2).
    - `resourceRequests[0] = []`.
  - After `alloc 2 20 1`:
    - `resourceAllocation[1] = [1]` (PID=2 holds Resource1).
    - `resourceRequests[1] = []`.

- **Deadlock Attempt**:
  - `alloc 1 10 1`:
    - `mm.getPidHoldingResource(1)` returns PID=2.
    - `pm.requestResource(1, 1, 2)`:
      - Adds Resource1 to `resourceRequests[0] = [1]`.
      - Sets PID=1 to BLOCKED.
      - Adds Resource2 to `resourceRequests[1] = [2]` (implicit request).
      - Sets PID=2 to BLOCKED.
    - `pm.detectDeadlock(1, 1)`:
      - Creates `tempRequests = [1]` for PID=1.
      - `hasCycle(0, ..., [1])`:
        - PID=1 requests Resource1.
        - Resource1 held by PID=2 (`resourceAllocation[1].contains(1)`).
        - PID=2 requests Resource2 (`resourceRequests[1] = [2]`).
        - Resource2 held by PID=1 (`resourceAllocation[0].contains(2)`).
        - Cycle detected: PID=1 → Resource1 → PID=2 → Resource2 → PID=1.
      - Returns `true`, triggering:
        ```
        Allocation denied for PID=1: Potential deadlock detected
        ```
    - `ps` shows:
      ```
      PID     Name    State
      1       process1        BLOCKED
      2       process2        BLOCKED
      ```

- **Resolution**:
  - `free 2`:
    - `mm.free(2)` clears pages 2–3.
    - `pm.removeResource(2, 1, true)`:
      - Removes Resource1 from `resourceAllocation[1]`.
      - Calls `notifyWaitingProcesses(1)`:
        - Finds PID=1 requesting Resource1 (`resourceRequests[0] = [1]`).
        - Sets PID=1 to READY.
      - Sets PID=2 to READY.
    - `pm.removeResource(2, 2, false)` clears `resourceRequests[1] = []`.
    - `ps` shows:
      ```
      PID     Name    State
      1       process1        READY
      2       process2        READY
      ```
  - `alloc 1 10 1`:
    - Resource1 is free (`mm.getPidHoldingResource(1) = -1`).
    - `mm.allocate(1, 10, 1)` allocates page 2.
    - `pm.addResource(1, 1)` moves Resource1 to `resourceAllocation[0]`.

### 1.7 Design Choices
- **Implicit Requests**: Adding implicit requests ensures cycle detection in simple scenarios, simulating mutual waiting without requiring explicit user commands.
- **Prevention vs. Recovery**: The simulator prevents deadlocks by denying allocations, avoiding complex recovery mechanisms (e.g., process termination).
- **Manual Retry**: After freeing resources, processes transition to READY, requiring manual `alloc` retry. Automatic retry was not implemented for simplicity but could be added with a pending allocation queue.

## 2. Virtual Paging and Memory Visualization

MiniOS implements a **paged virtual memory system** with a fixed memory size of 100 units, divided into 10 pages of 10 units each. Memory visualization provides a detailed view of page allocations, process IDs, resource IDs, and a character-based map, helping users understand memory usage.

### 2.1 Key Components
- **MemoryManager**: Manages the memory array, page allocations, and visualization (`MemoryManager.java`).
- **Main**: Invokes memory visualization via the `mem` command (`Main.java`).

### 2.2 Data Structures
- **memory**: An `int[]` of 100 elements, where `memory[i]` is the PID occupying unit `i`, or -1 if free.
  ```java
  private int[] memory;
  private static final int MEMORY_SIZE = 100;
  ```
- **resourceToPages**: A `Map<Integer, List<Integer>>` mapping resource IDs to lists of page indices allocated for that resource.
  ```java
  private Map<Integer, List<Integer>> resourceToPages;
  ```
- **Constants**:
  ```java
  private static final int PAGE_SIZE = 10;
  private static final int NUM_PAGES = MEMORY_SIZE / PAGE_SIZE; // 10
  ```

### 2.3 Virtual Paging Implementation
MiniOS uses a **fixed-size paging system** where memory is divided into 10 pages (0–9), each containing 10 units. Pages are allocated contiguously for a process and resource, ensuring no fragmentation within a page.

#### Allocation Process (`allocate`)
- Triggered by `alloc [pid] [size] [resourceId]` in `Main.java`:
  ```java
  if (mm.allocate(pid, size, resourceId)) {
      pm.addResource(pid, resourceId);
  }
  ```

- Steps in `MemoryManager.allocate`:
  1. **Calculate Pages Needed**:
     - Rounds up `size` to the nearest page count:
       ```java
       int pagesNeeded = (int) Math.ceil((double) size / PAGE_SIZE);
       ```
     - Example: `alloc 1 20 2` needs 2 pages (20 ÷ 10 = 2).

  2. **Check Resource Contention**:
     - Verifies if `resourceId` is held by another process:
       ```java
       if (resourceToPages.containsKey(resourceId)) {
           List<Integer> pages = resourceToPages.get(resourceId);
           int currentPid = memory[pages.get(0) * PAGE_SIZE];
           if (currentPid != -1 && currentPid != pid) {
               System.out.println("Resource " + resourceId + " is held by PID=" + currentPid + "; PID=" + pid + " must wait");
               return false;
           }
       }
       ```

  3. **Find Free Pages**:
     - Scans pages to find `pagesNeeded` free pages:
       ```java
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
       ```

  4. **Allocate Pages**:
     - Assigns the PID to all units in the selected pages:
       ```java
       List<Integer> allocatedPages = new ArrayList<>();
       for (int i = 0; i < pagesNeeded; i++) {
           int page = freePages.get(i);
           allocatedPages.add(page);
           for (int j = page * PAGE_SIZE; j < (page + 1) * PAGE_SIZE; j++) {
               memory[j] = pid;
           }
       }
       ```
     - Maps the resource to the allocated pages:
       ```java
       resourceToPages.put(resourceId, allocatedPages);
       ```
     - Outputs success:
       ```java
       System.out.println("Allocated " + pagesNeeded + " pages (" + (pagesNeeded * PAGE_SIZE) + " units) for PID=" + pid + " with resourceId=" + resourceId);
       ```

- **Failure Cases**:
  - If `pagesNeeded` exceeds available free pages:
    ```java
    if (freePages.size() >= pagesNeeded) {
        // Allocate
    } else {
        System.out.println("Allocation failed for PID=" + pid + ": insufficient free pages");
        return false;
    }
    ```
  - If the resource is held, triggers deadlock detection (handled by `Main` and `ProcessManager`).

#### Deallocation Process (`free`)
- Triggered by `free [pid]` in `Main.java`:
  ```java
  mm.free(pid);
  ```

- Steps in `MemoryManager.free`:
  1. **Identify Resources**:
     - Finds all resources allocated to `pid`:
       ```java
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
           }
       }
       ```

  2. **Clear Mappings**:
     - Removes resource-to-page mappings:
       ```java
       for (int resourceId : resourcesToRemove) {
           resourceToPages.remove(resourceId);
       }
       ```

  3. **Output Result**:
     ```java
     if (found) {
         System.out.println("Freed memory for PID=" + pid);
     } else {
         System.out.println("No memory allocated for PID=" + pid);
     }
     ```

### 2.4 Memory Visualization (`printMemory`)
The `mem` command displays the memory state in two formats: a table and a detailed map.

- Triggered in `Main.java`:
  ```java
  case "mem":
      mm.printMemory();
      break;
  ```

- Steps in `MemoryManager.printMemory`:
  1. **Table Display**:
     - Iterates over all 10 pages:
       ```java
       System.out.println("Memory Layout (Paged, " + PAGE_SIZE + " units per page):");
       System.out.println("Page\tStatus\tPID\tResourceID");
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
           System.out.println("Page " + i + "\t" + status + "\t" + pidDisplay + "\t" + resourceDisplay);
       }
       ```
     - **Fields**:
       - **Page**: Page number (0–9).
       - **Status**: “Allocated” or “Free”.
       - **PID**: Process ID or “-” if free.
       - **ResourceID**: Resource ID or “-” if free.
     - Ensures consistency within each page (all units have the same PID).

  2. **Detailed Memory Map**:
     - Displays each page as a 10-character string, with each character representing a memory unit:
       - PID (e.g., “1” for PID=1) if allocated.
       - “.” if free.
       ```java
       System.out.println("\nDetailed Memory Map:");
       for (int i = 0; i < MEMORY_SIZE; i += PAGE_SIZE) {
           System.out.print("Page " + (i / PAGE_SIZE) + " [" + i + "-" + (i + PAGE_SIZE - 1) + "] ");
           for (int j = i; j < i + PAGE_SIZE; j++) {
               System.out.print(memory[j] == -1 ? "." : memory[j]);
           }
           System.out.println();
       }
       ```

### 2.5 Example from Output
From the test scenario:
```
> alloc 1 20 2
> alloc 2 20 1
> mem
Memory Layout (Paged, 10 units per page):
Page    Status  PID     ResourceID
Page 0  Allocated       1       2
Page 1  Allocated       1       2
Page 2  Allocated       2       1
Page 3  Allocated       2       1
Page 4  Free    -       -
...
Detailed Memory Map:
Page 0 [0-9] 1111111111
Page 1 [10-19] 1111111111
Page 2 [20-29] 2222222222
Page 3 [30-39] 2222222222
Page 4 [40-49] ..........
...
```

- **After `alloc 1 20 2`**:
  - Pages 0–1 allocated to PID=1, Resource2.
  - `memory[0..19] = 1`, `resourceToPages.get(2) = [0, 1]`.

- **After `alloc 2 20 1`**:
  - Pages 2–3 allocated to PID=2, Resource1.
  - `memory[20..39] = 2`, `resourceToPages.get(1) = [2, 3]`.

- **After `free 2` and `alloc 1 10 1`**:
  ```
  > mem
  Memory Layout (Paged, 10 units per page):
  Page    Status  PID     ResourceID
  Page 0  Allocated       1       2
  Page 1  Allocated       1       2
  Page 2  Allocated       1       1
  Page 3  Free    -       -
  ...
  Detailed Memory Map:
  Page 0 [0-9] 1111111111
  Page 1 [10-19] 1111111111
  Page 2 [20-29] 1111111111
  Page 3 [30-39] ..........
  ...
  ```
  - Pages 2–3 freed, then page 2 reallocated to PID=1, Resource1.
  - `memory[20..29] = 1`, `resourceToPages.get(1) = [2]`.

### 2.6 Design Choices
- **Fixed Paging**: Simplifies allocation by using fixed 10-unit pages, avoiding fragmentation issues.
- **Contiguous Pages**: Ensures pages for a single allocation are contiguous in the visualization, improving clarity.
- **Resource Tracking**: `resourceToPages` links memory allocations to resources, enabling deadlock detection integration.
- **Visualization**: The dual-format output (table and map) balances detailed information with visual intuition, suitable for educational purposes.

## 3. Integration of Deadlock Handling and Paging
- **Resource Contention**:
  - `MemoryManager.getPidHoldingResource` uses `resourceToPages` to check if a resource is held, triggering deadlock detection in `Main`:
    ```java
    int pidHolding = mm.getPidHoldingResource(resourceId);
    if (pidHolding != -1 && pidHolding != pid) {
        pm.requestResource(pid, resourceId, pidHolding);
    }
    ```

- **State Management**:
  - `ProcessManager.requestResource` sets processes to BLOCKED, and `addResource` or `notifyWaitingProcesses` transitions them to READY, reflected in `mem` and `ps` outputs.

- **Memory Consistency**:
  - `MemoryManager.allocate` ensures pages are fully allocated to one PID, and `free` clears entire pages, maintaining consistency in the RAG and visualization.

## 4. Limitations and Potential Improvements
- **Deadlock Handling**:
  - **Manual Retry**: Requires manual `alloc` retry after `free`. A queue of pending allocations could automate this.
  - **Implicit Requests**: Assumes mutual waiting, which may not always reflect real scenarios. Explicit user requests could be supported.
  - **Recovery**: Only prevents deadlocks; recovery (e.g., preempting resources) could be added.

- **Virtual Paging**:
  - **Fixed Size**: Limited to 100 units and 10 pages. Dynamic memory sizing could be implemented.
  - **No Swapping**: Lacks disk-based paging. Adding a swap space could simulate virtual memory fully.
  - **Simple Allocation**: Uses first-fit page allocation. Best-fit or buddy systems could optimize usage.

## 5. Conclusion
MiniOS’s deadlock handling uses a RAG-based cycle detection algorithm to prevent circular waits, integrating seamlessly with resource management in `MemoryManager`. The virtual paging system employs a fixed-size, page-based memory model, with clear visualization through tables and maps. Together, these features provide a robust educational tool for exploring OS concepts, as demonstrated in the test output. For further enhancements, consider adding automatic allocation retries or dynamic memory sizing, depending on project goals.