package org.shellord.kaeru.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.shellord.kaeru.model.Task;
import org.shellord.kaeru.model.TimerModel;

import java.io.IOException;
import java.util.List;

/**
 * Central screen switcher.
 * Also holds the session data (timer + tasks) so controllers
 * can pass it between screens without coupling to each other.
 */
public class SceneManager {

    private static Stage stage;

    // Session data passed between screens
    private static TimerModel timerModel;
    private static List<Task> tasks;
    private static int todayFocusSeconds = 0; // accumulates across sessions

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    // ── Screen navigation ──

    public static void showSetup() {
        loadScene("/org/shellord/kaeru/view/setup.fxml", "Kaeru Focus");
        stage.setWidth(480);
        stage.setHeight(560);
    }

    public static void showFocus(TimerModel model, List<Task> taskList) {
        timerModel = model;
        tasks = taskList;
        loadScene("/org/shellord/kaeru/view/focus.fxml", "Kaeru Focus — Session");
        stage.setWidth(360);
        stage.setHeight(660);
    }

    public static void showSummary() {
        if (timerModel != null) {
            todayFocusSeconds += timerModel.getFocusSecondsAccum();
        }
        loadScene("/org/shellord/kaeru/view/summary.fxml", "Kaeru Focus — Done");
        stage.setWidth(480);
        stage.setHeight(560);
    }

    private static void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(fxmlPath)
            );
            Parent root = loader.load();

            if (stage.getScene() == null) {
                // first load — create the scene once
                Scene scene = new Scene(root);
                scene.getStylesheets().add(
                        SceneManager.class.getResource(
                                "/org/shellord/kaeru/styles/main.css"
                        ).toExternalForm()
                );
                stage.setScene(scene);
            } else {
                // subsequent loads — just swap the root, no new Scene
                stage.getScene().setRoot(root);
            }

            stage.setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load: " + fxmlPath, e);
        }
    }

    // ── Shared data getters ──

    public static TimerModel getTimerModel()     { return timerModel; }
    public static List<Task> getTasks()          { return tasks; }
    public static int getTodayFocusSeconds()     { return todayFocusSeconds; }

    public static String formatSeconds(int totalSecs) {
        int mins = totalSecs / 60;
        if (mins < 60) return mins + "m";
        return (mins / 60) + "h " + (mins % 60) + "m";
    }
}