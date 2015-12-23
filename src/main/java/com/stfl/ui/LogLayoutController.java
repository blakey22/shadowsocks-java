package com.stfl.ui;

import com.stfl.misc.Log;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class LogLayoutController {
    @FXML
    public TextArea txtLog;
    @FXML
    private Button btnClose;
    @FXML
    private Button btnClear;

    private Stage stage;

    @FXML
    private void initialize() {
        TextAreaLogHandler handler = new TextAreaLogHandler();
        handler.setTextArea(txtLog);
        Log.addHandler(handler);
    }

    @FXML
    private void handleClear() {
        txtLog.clear();
    }

    @FXML
    private void handleClose() {
        stage.hide();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
