# MiniOS User Manual

## Overview
MiniOS is a command-line operating system simulator developed in Java to demonstrate core OS concepts, including process management, memory allocation, resource handling, and deadlock detection. It supports commands to create and manage processes, allocate and free memory, schedule processes, and detect potential deadlocks using a Resource Allocation Graph (RAG). The simulator is designed for educational purposes, providing a hands-on way to explore OS functionality in a controlled environment.

### Key Features
- **Process Management**: Create, list, and schedule processes with states (READY, RUNNING, BLOCKED).
- **Memory Management**: Allocate and free memory using a paged memory model (100 units, 10 units per page).
- **Resource Management**: Manage resources associated with memory allocations, with deadlock detection to prevent circular wait conditions.
- **Mutual Exclusion**: Uses a semaphore to ensure thread-safe command execution.
- **Command-Line Interface**: Interactive prompt for entering commands and viewing system state.

## System Requirements
- **Java Development Kit (JDK)**: Version 8 or higher.
- **Operating System**: Windows, macOS, or Linux.
- **Disk Space**: Minimal (source files and compiled classes require <1 MB).
- **Command-Line Tool**: Terminal (Linux/macOS) or PowerShell/Command Prompt (Windows).

## Installation and Setup
1. **Obtain the Source Code**:
   - Save the following files in a directory (e.g., `MiniOS/`):
     - `Main.java`
     - `ProcessManager.java`
     - `MemoryManager.java`
     - `PCB.java`
     - `Semaphore.java`
   - Ensure the files match the latest versions provided in the project documentation.

2. **Navigate to the Directory**:
   - Open a terminal or command prompt.
   - Change to the project directory:
     ```bash
     cd path/to/MiniOS
     ```
     Example (Windows):
     ```powershell
     cd C:\Users\YourName\MiniOS
     ```

3. **Compile the Code**:
   - Run the Java compiler to compile all `.java` files:
     ```bash
     javac *.java
     ```
   - This generates `.class` files in the same directory.

4. **Run the Simulator**:
   - Start the simulator by running the `Main` class:
     ```bash
     java -cp . Main
     ```
   - The simulator will display:
     ```
     MiniOS Simulator
     Commands: create [name], ps, schedule, alloc [pid] [size] [resourceId], free [pid], mem, exit
     > 
     ```

## Commands
The simulator accepts the following commands at the `>` prompt. Commands are case-insensitive, and parameters are space-separated.

| Command | Syntax | Description | Example |
|---------|--------|-------------|---------|
| **create** | `create [name]` | Creates a new process with a unique PID and the specified name. The process starts in the READY state. | `create process1` |
| **ps** | `ps` | Lists all processes, showing their PID, name, and state (READY, RUNNING, or BLOCKED). | `ps` |
| **schedule** | `schedule` | Simulates process execution by running each READY process for 5 seconds, transitioning it to RUNNING and back to READY. | `schedule` |
| **alloc** | `alloc [pid] [size] [resourceId]` | Allocates memory (in units, rounded up to pages of 10 units) to the specified PID with the given resource ID. Detects and prevents deadlocks. | `alloc 1 20 2` |
| **free** | `free [pid]` | Frees all memory and resources allocated to the specified PID, transitioning waiting processes to READY. | `free 2` |
| **mem** | `mem` | Displays the memory layout, showing page status, PID, resource ID, and a detailed memory map. | `mem` |
| **exit** | `exit` | Terminates the simulator. | `exit` |

### Command Details
- **create [name]**:
  - Assigns a unique PID (starting from 1).
  - Sets the process state to READY.
  - Example output:
    ```
    > create process1
    Process created: PID=1, Name=process1
    ```

- **ps**:
  - Displays a table of all processes.
  - Example output:
    ```
    > ps
    PID     Name    State
    1       process1        READY
    2       process2        BLOCKED
    ```

- **schedule**:
  - Executes READY processes sequentially, simulating a 5-second run.
  - Example output:
    ```
    > schedule
    Running: PID=1, Name=process1
    ```

- **alloc [pid] [size] [resourceId]**:
  - Allocates `size` units of memory (in pages) to `pid` with `resourceId`.
  - Checks for resource contention and deadlock:
    - If the resource is held by another process, the requesting process is set to BLOCKED.
    - If a deadlock is detected, the allocation is denied.
  - Example outputs:
    ```
    > alloc 1 20 2
    Allocated 2 pages (20 units) for PID=1 with resourceId=2
    ```
    ```
    > alloc 1 10 1
    Allocation denied for PID=1: Potential deadlock detected
    ```

- **free [pid]**:
  - Releases all memory and resources for `pid`.
  - Transitions processes waiting for freed resources to READY.
  - Example output:
    ```
    > free 2
    Freed memory for PID=2
    ```

- **mem**:
  - Shows memory allocation (10 pages, 100 units total).
  - Includes a table (page, status, PID, resource ID) and a visual map.
  - Example output:
    ```
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

- **exit**:
  - Closes the scanner and exits.
  - Example output:
    ```
    > exit
    Exiting MiniOS
    ```

## Sample Session
This session demonstrates creating processes, allocating memory, encountering a deadlock, resolving it, and verifying the system state.

1. **Start the Simulator**:
   ```bash
   java -cp . Main
   MiniOS Simulator
   Commands: create [name], ps, schedule, alloc [pid] [size] [resourceId], free [pid], mem, exit
   > 
   ```

2. **Create Processes**:
   ```
   > create process1
   Process created: PID=1, Name=process1
   > create process2
   Process created: PID=2, Name=process2
   ```

3. **Allocate Memory**:
   ```
   > alloc 1 20 2
   Allocated 2 pages (20 units) for PID=1 with resourceId=2
   > alloc 2 20 1
   Allocated 2 pages (20 units) for PID=2 with resourceId=1
   ```

4. **Check Process States**:
   ```
   > ps
   PID     Name    State
   1       process1        READY
   2       process2        READY
   ```

5. **Attempt Allocation (Deadlock)**:
   ```
   > alloc 1 10 1
   Allocation denied for PID=1: Potential deadlock detected
   > ps
   PID     Name    State
   1       process1        BLOCKED
   2       process2        BLOCKED
   ```

6. **Check Memory**:
   ```
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

7. **Resolve Deadlock**:
   ```
   > free 2
   Freed memory for PID=2
   > ps
   PID     Name    State
   1       process1        READY
   2       process2        READY
   ```

8. **Retry Allocation**:
   ```
   > alloc 1 10 1
   Allocated 1 pages (10 units) for PID=1 with resourceId=1
   > mem
   Memory Layout (Paged, 10 units per page):
   Page    Status  PID     ResourceID
   Page 0  Allocated       1       2
   Page 1  Allocated       1       2
   Page 2  Allocated       1       1
   Page 3  Free    -       -
   Page 4  Free    -       -
   ...
   Detailed Memory Map:
   Page 0 [0-9] 1111111111
   Page 1 [10-19] 1111111111
   Page 2 [20-29] 1111111111
   Page 3 [30-39] ..........
   Page 4 [40-49] ..........
   ...
   ```

9. **Exit**:
   ```
   > exit
   Exiting MiniOS
   ```

## Troubleshooting
- **Compilation Error**:
  - **Issue**: `javac *.java` fails with syntax errors.
  - **Solution**: Ensure all source files (`Main.java`, `ProcessManager.java`, etc.) are in the directory and match the provided versions. Check for typos or missing files.

- **Runtime Error: Class Not Found**:
  - **Issue**: `java -cp . Main` fails with `Error: Could not find or load main class Main`.
  - **Solution**: Verify that compilation succeeded (`.class` files exist). Ensure you’re in the correct directory and use `-cp .` to set the classpath.

- **Invalid Command**:
  - **Issue**: Entering a command like `alloc 999 20 999` results in `Process with PID=999 does not exist`.
  - **Solution**: Create a process first (e.g., `create process3`) and use a valid PID. Check `ps` for active PIDs.

- **Deadlock Detected but Process Stays BLOCKED**:
  - **Issue**: After `alloc` is denied, a process remains BLOCKED after `free`.
  - **Solution**: Ensure the latest `ProcessManager.java` includes `notifyWaitingProcesses`. Verify the `free` command clears resources correctly. Rerun the allocation manually (e.g., `alloc 1 10 1`).

- **Insufficient Memory**:
  - **Issue**: `Allocation failed for PID=1: insufficient free pages`.
  - **Solution**: Free memory for other processes (e.g., `free 2`) or request a smaller size (total memory is 100 units).

- **Unexpected Behavior**:
  - **Solution**: Enable debugging by adding print statements in `ProcessManager.java` (e.g., in `requestResource` or `notifyWaitingProcesses`) to trace resource allocations and requests. Example:
    ```java
    System.out.println("Requests: " + resourceRequests);
    ```

## Notes
- **Deadlock Handling**: The simulator detects deadlocks by constructing a RAG and checking for cycles. When a deadlock is detected, the allocation is denied, and processes remain BLOCKED until resources are freed.
- **Manual Retry**: After freeing a resource (e.g., `free 2`), you must manually retry allocations (e.g., `alloc 1 10 1`) for processes that were BLOCKED. Automatic retry is not implemented but can be added.
- **Thread Safety**: A semaphore ensures commands execute sequentially, preventing race conditions in the command-line interface.
- **Extensibility**: To add features (e.g., priority scheduling, automatic allocation retry), modify `ProcessManager.java` or `Main.java` as needed.

## Support
For issues or enhancements, consult the project documentation or contact the developer. Provide the command sequence, output, and source file versions when reporting bugs. For academic use, refer to your instructor for project-specific guidelines.

---
**MiniOS Simulator**  
Developed for Operating Systems Final Project  
Last Updated: May 16, 2025