package cc.springcloud.socks;

import cc.springcloud.socks.misc.UTF8Control;
import cc.springcloud.socks.ui.MainLayoutController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;


public class MainGui extends Application {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Stage primaryStage;
    private Scene rootScene;
    private MainLayoutController controller;
    private TrayIcon trayIcon;

    @Override
    public void start(Stage primaryStage) {

        Platform.setImplicitExit(false);
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Server Configuration");

        try {
            // Load the root layout from the fxml file
            FXMLLoader mainLayoutLoader = new FXMLLoader(MainGui.class.getResource("/resources/ui/MainLayout.fxml"));
            mainLayoutLoader.setResources(ResourceBundle.getBundle("resources.bundle.ui", Locale.getDefault(), new UTF8Control()));
            Pane rootLayout = mainLayoutLoader.load();

            rootScene = new Scene(rootLayout);
            primaryStage.setScene(rootScene);
            primaryStage.setResizable(false);

            controller = mainLayoutLoader.getController();
            controller.setMainGui(this);

            addToTray();

            primaryStage.getIcons().add(new Image(MainGui.class.getResource("/resources/image/icon.png").toString()));
            primaryStage.show();
        } catch (IOException e) {
            // Exception gets thrown if the fxml file could not be loaded
            logger.info("start error",e);
        }
    }


    private void addToTray() {
        // ensure awt is initialized
        java.awt.Toolkit.getDefaultToolkit();

        // make sure system tray is supported
        if (!java.awt.SystemTray.isSupported()) {
            logger.warn("No system tray support!");
        }

        final java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
        try {

            java.awt.Image image = ImageIO.read(MainGui.class.getResource("/resources/image/icon.png"));
            trayIcon = new TrayIcon(image);
            trayIcon.addActionListener(e -> Platform.runLater(() -> primaryStage.show()));

            java.awt.MenuItem openItem = new java.awt.MenuItem("Configuration");
            openItem.addActionListener(e -> Platform.runLater(() -> show()));

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener(e -> {
                controller.closeServer();
                Platform.exit();
                tray.remove(trayIcon);
            });

            PopupMenu popup = new PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);
            trayIcon.setToolTip("Not Connected");
            tray.add(trayIcon);
        } catch (IOException | AWTException e) {
            logger.info("addToTray error",e);
        }
    }

    public void show() {
        primaryStage.show();
    }

    public void hide() {
        primaryStage.hide();
    }

    public void setTooltip(String message) {
        if (trayIcon != null) {
            trayIcon.setToolTip(message);
        }
    }

    public void showNotification(String message) {
        trayIcon.displayMessage(
                "shadowsocks-java",
                message,
                java.awt.TrayIcon.MessageType.INFO
        );
    }
}
