package org.shellord.kaeru;

import javafx.application.Application;
import javafx.stage.Stage;
import org.shellord.kaeru.util.SceneManager;

public class KaeruApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Kaeru Focus");
        stage.setWidth(480);
        stage.setHeight(560);
        stage.setResizable(false);

        SceneManager.init(stage);
        SceneManager.showSetup();

        stage.show();
    }
}