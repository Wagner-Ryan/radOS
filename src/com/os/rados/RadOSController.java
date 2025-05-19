package com.os.rados;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class RadOSController {

    @FXML
    private Label processDisplay;

    @FXML
    private TextField processNameBox;

    @FXML
    private Button createProcessButton;

    @FXML
    private Label memoryDisplay;

    @FXML
    public TextField memoryPIDBox;

    @FXML
    public TextField memorySizeBox;

    @FXML
    public TextField memoryRIDBox;

    @FXML
    public Button allocateMemoryButton;

    @FXML
    public TextField freeMemoryPIDBox;

    @FXML
    public Button freeMemoryButton;

    @FXML
    public Label activityLogDisplay;

    @FXML
    protected void createProcess() throws InterruptedException {
        Main.semaphore.waitSem();
        String name = processNameBox.getText();
        if(name.equals("")){
            displayMessage("Process was not created.\n\nPlease enter a name\nof the process to create");
        }
        else {
            Main.pm.createProcess(name);
            processDisplay.setText(Main.pm.listProcesses());

        }
        processNameBox.setText("");
        Main.semaphore.signal();
    }

    @FXML
    public void allocateMemory(ActionEvent actionEvent) throws InterruptedException {
        Main.semaphore.waitSem();
        boolean valid = true;
        int pid = -1, size = -1, rid = -1;
        if(memoryPIDBox.getText().equals("")){
            valid = false;
        }
        else pid = Integer.parseInt(memoryPIDBox.getText());
        if(memorySizeBox.getText().equals("")){
            valid = false;
        }
        else size = Integer.parseInt(memorySizeBox.getText());
        if(memoryRIDBox.getText().equals("")){
            valid = false;
        }
        else rid = Integer.parseInt(memoryRIDBox.getText());

        if(!valid){
            displayMessage("Memory was not allocated.\n\nPlease enter the following\nPID, Size, and ResourceID\nto allocated memory");
        }
        else{
            if(Main.pm.getProcessByPid(pid) == null){
                displayMessage("Process with PID=" + pid + "\ndoes not exist");
            }
            else{
                int pidHolding = Main.mm.getPidHoldingResource(rid);
                if(pidHolding != -1 && pidHolding != pid){
                    // Resource is held by another process; request it
                    Main.pm.requestResource(pid, rid, pidHolding);
                    if(Main.pm.detectDeadlock(pid, rid)){
                        displayMessage("Allocated denied for PID=" + pid + "\nPotential deadlock detected");
                        // Keep request and BLOCKED state
                    }
                    else{
                        displayMessage("Resource " + rid + " is held by PID=" + pidHolding + "\nPID=" + pid + " is waiting");
                        // Keep request in resourceRequests
                    }
                }
                else{
                    if(Main.mm.allocate(pid, size, rid)){
                        Main.pm.addResource(pid, rid);
                        memoryDisplay.setText(Main.mm.printMemory());

                    }
                    else{
                        Main.pm.removeResource(pid, rid, false);
                    }
                }
            }
            processDisplay.setText(Main.pm.listProcesses());
            memoryPIDBox.setText("");
            memorySizeBox.setText("");
            memoryRIDBox.setText("");
            Main.semaphore.signal();
        }
    }

    @FXML
    public void freeMemory() throws InterruptedException {
        Main.semaphore.waitSem();
        String pidBoxText = freeMemoryPIDBox.getText();
        if(pidBoxText.equals("")){
            displayMessage("No memory was freed.\n\nPlease enter a PID\nto free memory");
        }
        else {
            int pid = Integer.parseInt(pidBoxText);
            Main.mm.free(pid);
            if(pid <= Main.pm.resourceAllocation.size()){
                List<Integer> heldResources = new ArrayList<>(Main.pm.resourceAllocation.get(pid - 1));
                for (Integer resourceId : heldResources) {
                    Main.pm.removeResource(pid, resourceId, true);
                }
                // Clear requests
                List<Integer> requestedResources = new ArrayList<>(Main.pm.resourceRequests.get(pid - 1));
                for (Integer resourceId : requestedResources) {
                    Main.pm.removeResource(pid, resourceId, false);
                }
            }
        }
        processDisplay.setText(Main.pm.listProcesses());
        memoryDisplay.setText(Main.mm.printMemory());
        freeMemoryPIDBox.setText("");
        Main.semaphore.signal();
    }

    @FXML
    public void schedule(){
        // Run in a background thread to avoid freezing UI
        new Thread(() -> {
            Main.semaphore.waitSem();

            // Clear display on JavaFX thread
            Platform.runLater(() -> activityLogDisplay.setText(""));

            for (PCB process : Main.pm.processes) {
                if (process.isActive() && process.getState().equals("READY")) {
                    process.setState("RUNNING");
                    Platform.runLater(() -> processDisplay.setText(Main.pm.listProcesses()));

                    // Append to label on JavaFX thread
                    String logEntry = "Running: PID=" + process.getPid() + ", Name=" + process.getName() + "\n";
                    Platform.runLater(() -> {
                        String oldText = activityLogDisplay.getText();
                        activityLogDisplay.setText(oldText + logEntry);
                    });

                    // Simulate execution delay (on background thread)
                    try {
                        Thread.sleep(5000); // Now safe
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    process.setState("READY");
                    // Update process display
                    Platform.runLater(() -> processDisplay.setText(Main.pm.listProcesses()));
                }
            }

            Main.semaphore.signal();
        }).start();

    }

    @FXML
    public void displayMessage(String message) throws InterruptedException{
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/ErrorMessage.fxml"));
            Parent root = loader.load();

            ErrorMessageController controller = loader.getController();
            controller.setMessage(message);

            // Create a new stage (new window)
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Error Message");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void memoryText(){
        memoryDisplay.setText(Main.mm.printMemory());
    }
}
