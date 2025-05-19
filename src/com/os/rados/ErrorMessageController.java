package com.os.rados;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ErrorMessageController {

    @FXML
    public Label messageLabel;

    public void setMessage(String message){
        messageLabel.setText(message);
    }

}
