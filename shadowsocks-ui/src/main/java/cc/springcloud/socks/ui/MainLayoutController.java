package cc.springcloud.socks.ui;

import cc.springcloud.socks.Constant;
import cc.springcloud.socks.MainGui;
import cc.springcloud.socks.misc.JsonConfig;
import cc.springcloud.socks.misc.UTF8Control;
import cc.springcloud.socks.network.IServer;
import cc.springcloud.socks.network.NioLocalServer;
import cc.springcloud.socks.network.proxy.IProxy;
import cc.springcloud.socks.network.proxy.ProxyFactory;
import cc.springcloud.socks.ss.CryptBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.util.Locale;
import java.util.ResourceBundle;


public class MainLayoutController {
    @FXML
    private TextField txtServerIP;
    @FXML
    private TextField txtServerPort;
    @FXML
    private ComboBox<String> cboCipher;
    @FXML
    private TextField txtPassword;
    @FXML
    private TextField txtLocalPort;
    @FXML
    private ComboBox<IProxy.TYPE> cboProxyType;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnLog;
    @FXML
    private Button btnClose;

    private Logger logger = LoggerFactory.getLogger(getClass());
    private MainGui gui;
    private IServer server;
    private Stage logStage;
    private JsonConfig jsonConfig;

    @FXML
    private void initialize() {
        // set cipher options
        ObservableList<String> ciphers = FXCollections.observableArrayList();
        ciphers.addAll(CryptBuilder.getSupportedCiphers());
        cboCipher.setItems(ciphers);

        // set proxy options
        ObservableList<IProxy.TYPE> proxyTypes = FXCollections.observableArrayList();
        proxyTypes.addAll(ProxyFactory.getSupportedProxyTypes());
        cboProxyType.setItems(proxyTypes);

        // prepare configuration
        jsonConfig = new JsonConfig();
        jsonConfig.loadFromJson();
        txtServerIP.setText(jsonConfig.getRemoteIpAddress());
        txtServerPort.setText(String.valueOf(jsonConfig.getRemotePort()));
        txtLocalPort.setText(String.valueOf(jsonConfig.getLocalPort()));
        txtPassword.setText(jsonConfig.getPassword());
        cboCipher.setValue(jsonConfig.getMethod());
        cboProxyType.setValue(IProxy.TYPE.valueOf(jsonConfig.getProxyType()));

        // prepare log window
        Stage stage = new Stage();
        try {
            FXMLLoader logLayoutLoader = new FXMLLoader(MainGui.class.getResource("/resources/ui/LogLayout.fxml"));
            logLayoutLoader.setResources(ResourceBundle.getBundle("resources.bundle.ui", Locale.getDefault(), new UTF8Control()));
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
            logger.warn("Unable to load ICON: {}", e);
        }

        btnStop.setDisable(true);
    }

    @FXML
    private void handleStart() {
        if (!txtServerIP.getText().matches("[0-9]{1,4}.[0-9]{1,4}.[0-9]{1,4}.[0-9]{1,4}")) {
            showAlert(Constant.PROGRAM_NAME, "Invalid IP address", Alert.AlertType.ERROR);
            return;
        }
        String ip = txtServerIP.getText();
        if (!txtServerPort.getText().matches("[0-9]+")) {
            showAlert(Constant.PROGRAM_NAME, "Invalid Port", Alert.AlertType.ERROR);
            return;
        }
        int port = Integer.parseInt(txtServerPort.getText());
        String method = cboCipher.getValue();
        if (txtPassword.getText().length() == 0) {
            showAlert(Constant.PROGRAM_NAME, "Please specified password", Alert.AlertType.ERROR);
            return;
        }
        String password = txtPassword.getText();
        IProxy.TYPE type = cboProxyType.getValue();
        if (!txtLocalPort.getText().matches("[0-9]+")) {
            showAlert(Constant.PROGRAM_NAME, "Invalid Port", Alert.AlertType.ERROR);
            return;
        }
        int localPort = Integer.parseInt(txtLocalPort.getText());
        // create jsonConfig
        jsonConfig.setRemoteIpAddress(ip);
        jsonConfig.setRemotePort(port);
        jsonConfig.setLocalIpAddress("127.0.0.1");
        jsonConfig.setLocalPort(localPort);
        jsonConfig.setMethod(method);
        jsonConfig.setPassword(password);
        jsonConfig.setProxyType(type.name());
        jsonConfig.saveToJson();
        // start start
        try {
            server = new NioLocalServer(jsonConfig);
            Thread t = new Thread(server);
            t.setDaemon(true);
            t.start();
            String message = String.format("(Connected) Server %s:%d", jsonConfig.getRemoteIpAddress(), jsonConfig.getRemotePort());
            gui.setTooltip(message);
            gui.showNotification(message);
        } catch (IOException | InvalidAlgorithmParameterException e) {
            logger.warn("Unable to start server: {}", e);
        }
        btnStop.setDisable(false);
        btnStart.setDisable(true);
    }

    @FXML
    private void handleStop() {
        if (server != null) {
            server.close();
            String message = String.format("(Disconnected) Server %s:%d", jsonConfig.getRemoteIpAddress(), jsonConfig.getRemotePort());
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
