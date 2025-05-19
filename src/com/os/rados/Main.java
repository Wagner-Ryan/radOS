package com.os.rados;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static ProcessManager pm;
    public static MemoryManager mm;
    public static Semaphore semaphore;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("resources/radOSDisplay.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        RadOSController controller = fxmlLoader.getController();
        controller.memoryText();

        primaryStage.setTitle("radOS");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        pm = new ProcessManager();
        mm = new MemoryManager();
        semaphore = new Semaphore(1); // For mutual exclusion

        launch(args);
    }
}
