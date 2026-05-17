package org.shellord.kaeru.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.shellord.kaeru.model.Task;
import org.shellord.kaeru.model.TimerModel;
import org.shellord.kaeru.util.PixelArt;
import org.shellord.kaeru.util.SceneManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SummaryController implements Initializable {

    @FXML private Canvas summaryCanvas;
    @FXML private Label focusedLabel;
    @FXML private Label tasksLabel;
    @FXML private Label todayLabel;
    @FXML private Label roundsLabel;
    @FXML private VBox  taskSummaryList;

    private final long animStart = System.currentTimeMillis();
    private PixelArt.Star[] stars;
    private AnimationTimer animTimer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TimerModel model = SceneManager.getTimerModel();
        List<Task> tasks = SceneManager.getTasks();

        focusedLabel.setText(SceneManager.formatSeconds(model.getFocusSecondsAccum()));
        roundsLabel.setText(String.valueOf(model.getRoundsDone()));
        todayLabel.setText(SceneManager.formatSeconds(SceneManager.getTodayFocusSeconds()));

        long done = tasks.stream().filter(Task::isDone).count();
        tasksLabel.setText(done + " / " + tasks.size());

        if (tasks.isEmpty()) {
            Label empty = new Label("no tasks this session");
            empty.getStyleClass().add("empty-label");
            taskSummaryList.getChildren().add(empty);
        } else {
            for (Task task : tasks) {
                Label check = new Label(task.isDone() ? "✓" : "○");
                check.getStyleClass().add(task.isDone() ? "check-done" : "check-miss");

                Label name = new Label(task.getName());
                name.getStyleClass().add(task.isDone() ? "task-done-name" : "task-miss-name");

                HBox row = new HBox(8, check, name);
                row.getStyleClass().add("summary-task-row");
                taskSummaryList.getChildren().add(row);
            }
        }

        // animated scene — dusk mode, rider parked at the right (finish line)
        stars = PixelArt.generateStars(20, 480, 80, 99);
        animTimer = new AnimationTimer() {
            long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 50_000_000) { // 20fps is enough for static scene
                    drawScene();
                    lastUpdate = now;
                }
            }
        };
        animTimer.start();
        drawScene();
    }

    private void drawScene() {
        if (summaryCanvas == null) return;
        GraphicsContext gc = summaryCanvas.getGraphicsContext2D();
        double W = summaryCanvas.getWidth();
        double H = summaryCanvas.getHeight();
        double t = (System.currentTimeMillis() - animStart) / 1000.0;

        PixelArt.drawScene(
                gc, W, H, t,
                PixelArt.Mode.DUSK,
                2,           // rider parked at finish line (right)
                stars,
                false,       // not moving — rest after journey
                "● done",
                null
        );
    }

    @FXML
    private void onNewSessionClick() {
        if (animTimer != null) animTimer.stop();
        SceneManager.showSetup();
    }

    @FXML
    private void onRepeatClick() {
        if (animTimer != null) animTimer.stop();
        TimerModel old = SceneManager.getTimerModel();
        List<Task> tasks = SceneManager.getTasks();
        tasks.forEach(t -> t.setDone(false));

        TimerModel fresh = new TimerModel(
                old.getFocusMinutes(),
                old.getBreakMinutes(),
                old.getTotalRounds()
        );
        SceneManager.showFocus(fresh, tasks);
    }
}