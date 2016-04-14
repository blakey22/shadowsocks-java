package com.stfl.ui;

import com.stfl.Constant;
import com.stfl.MainGui;
import com.stfl.misc.Config;
import com.stfl.misc.UTF8Control;
import com.stfl.misc.Util;
import com.stfl.network.IServer;
import com.stfl.network.NioLocalServer;
import com.stfl.network.proxy.IProxy;
import com.stfl.network.proxy.ProxyFactory;
import com.stfl.ss.CryptFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.util.ResourceBundle;
import java.util.logging.Logger;


public class MainLayoutController {
    @FXML
    private TextField txtServerIP;
    @FXML
    private TextField txtServerPort;
    @FXML
    private ComboBox cboCipher;
    @FXML
    private TextField txtPassword;
    @FXML
    private TextField txtLocalPort;
    @FXML
    private ComboBox cboProxyType;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnLog;
    @FXML
    private Button btnClose;

    private Logger logger = Logger.getLogger(MainLayoutController.class.getName());
    private MainGui gui;
    private IServer server;
    private Stage logStage;
    private Config config;

    @FXML
    private void initialize() {
        // set cipher options
        ObservableList<String> ciphers = FXCollections.observableArrayList();
        ciphers.addAll(CryptFactory.getSupportedCiphers());
        cboCipher.setItems(ciphers);

        // set proxy options
        ObservableList<IProxy.TYPE> proxyTypes = FXCollections.observableArrayList();
        proxyTypes.addAll(ProxyFactory.getSupportedProxyTypes());
        cboProxyType.setItems(proxyTypes);

        // prepare configuration
        config = new Config();
        config.loadFromJson(Util.getFileContent(Constant.CONF_FILE));
        txtServerIP.setText(config.getRemoteIpAddress());
        txtServerPort.setText(String.valueOf(config.getRemotePort()));
        txtLocalPort.setText(String.valueOf(config.getLocalPort()));
        txtPassword.setText(config.getPassword());
        cboCipher.setValue(config.getMethod());
        cboProxyType.setValue(config.getProxyType());

        // prepare log window
        Stage stage = new Stage();
        try {
            FXMLLoader logLayoutLoader = new FXMLLoader(MainGui.class.getResource("/resources/ui/LogLayout.fxml"));
            logLayoutLoader.setResources(ResourceBundle.getBundle("resources.bundle.ui", Constant.LOCALE, new UTF8Control()));
            Pane logLayout = logLayoutLoader.load();
            Scene logScene = new Scene(logLayout);
            stage.setTitle("Log");
            stage.setScene(logScene);
            stage.setResizable(false);
            stage.getIcons().add(new Image(MainGui.class.getResource("/resources/image/icon.png").toString()));

            LogLayoutController controller = logLayoutLoader.getController();
            controller.setStage(stage);
            logStage = stage;
        } catch (IOException e) {
            logger.warning("Unable to load ICON: " + e.toString());
        }

        btnStop.setDisable(true);
    }

    @FXML
    private void handleStart() {
        boolean isValidated = false;
        do {
            if (!txtServerIP.getText().matches("[0-9]{1,4}.[0-9]{1,4}.[0-9]{1,4}.[0-9]{1,4}")) {
                showAlert(Constant.PROG_NAME, "Invalid IP address", Alert.AlertType.ERROR);
                break;
            }
            String ip = txtServerIP.getText();
            if (!txtServerPort.getText().matches("[0-9]+")) {
                showAlert(Constant.PROG_NAME, "Invalid Port", Alert.AlertType.ERROR);
                break;
            }
            int port = Integer.parseInt(txtServerPort.getText());

            String method = (String) cboCipher.getValue();
            if (txtPassword.getText().length() == 0) {
                showAlert(Constant.PROG_NAME, "Please specified password", Alert.AlertType.ERROR);
                break;
            }
            String password = txtPassword.getText();
            IProxy.TYPE type = (IProxy.TYPE) cboProxyType.getValue();
            if (!txtLocalPort.getText().matches("[0-9]+")) {
                showAlert(Constant.PROG_NAME, "Invalid Port", Alert.AlertType.ERROR);
                break;
            }
            int localPort = Integer.parseInt(txtLocalPort.getText());

            // create config
            config.setRemoteIpAddress(ip);
            config.setRemotePort(port);
            config.setLocalIpAddress("0.0.0.0");
            config.setLocalPort(localPort);
            config.setMethod(method);
            config.setPassword(password);
            config.setProxyType(type);
            Util.saveFile(Constant.CONF_FILE, config.saveToJson());

            isValidated = true;
        } while (false);

        if (!isValidated)
            return;

        // start start
        try {
            server = new NioLocalServer(config);
            Thread t = new Thread(server);
            t.setDaemon(true);
            t.start();
            String message = String.format("(Connected) Server %s:%d", config.getRemoteIpAddress(), config.getRemotePort());
            gui.setTooltip(message);
            gui.showNotification(message);
        } catch (IOException | InvalidAlgorithmParameterException e) {
            logger.warning("Unable to start server: " + e.toString());
        }
        btnStop.setDisable(false);
        btnStart.setDisable(true);
    }

    @FXML
    private void handleStop() {
        if (server != null) {
            server.close();
            String message = String.format("(Disconnected) Server %s:%d", config.getRemoteIpAddress(), config.getRemotePort());
            gui.showNotification(message);
            gui.setTooltip("Not Connected");
        }

        btnStop.setDisable(true);
        btnStart.setDisable(false);
    }

    @FXML
    private void handleLog() {
        logStage.show();
    }

    @FXML
    private void handleClose() {
        gui.hide();
    }

    public void setMainGui(MainGui gui) {
        this.gui = gui;
    }

    public void closeServer() {
        handleStop();
    }

    private boolean validationInput(String pattern, String text) {
        return false;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(type.name());
        a.setResizable(false);
        a.setContentText(message);
        a.showAndWait();
    }
}
