package org.shellord.kaeru;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.shellord.kaeru.util.MenuBarApp;
import org.shellord.kaeru.util.SceneManager;

public class KaeruApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Stay alive when the popup is hidden — the JVM has no visible windows
        // most of the time; the menu-bar icon is the app's entry point.
        Platform.setImplicitExit(false);

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Kaeru Focus");
        stage.setWidth(480);
        stage.setHeight(560);
        stage.setResizable(false);

        SceneManager.init(stage);
        SceneManager.showSetup();

        MenuBarApp.install(stage);
        // No stage.show() — sit silently in the menu bar until clicked.
    }
}
