package org.shellord.kaeru.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.shellord.kaeru.model.Task;
import org.shellord.kaeru.model.TimerModel;
import org.shellord.kaeru.util.AudioManager;
import org.shellord.kaeru.util.SceneManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FocusController implements Initializable {

    // ── Scene canvas ──
    @FXML private Canvas sceneCanvas;

    // ── Focus view ──
    @FXML private VBox  focusView;
    @FXML private Label currentTaskLabel;
    @FXML private Label ctRound;
    @FXML private Label timerLabel;
    @FXML private javafx.scene.layout.StackPane timerContainer;
    @FXML private javafx.scene.shape.Circle timerTrack;
    @FXML private javafx.scene.shape.Arc   timerArc;
    @FXML private VBox  taskListContainer;
    @FXML private Button pauseBtn;

    // ── Break view ──
    @FXML private VBox  breakView;
    @FXML private Label breakTimerLabel;
    @FXML private javafx.scene.layout.StackPane breakContainer;
    @FXML private javafx.scene.shape.Circle breakTrack;
    @FXML private javafx.scene.shape.Arc   breakArc;
    @FXML private Label breakNextLabel;
    @FXML private Label breakRoundsLabel;

    // ── Stats bar ──
    @FXML private Label todayTimeLabel;
    @FXML private Label roundsDoneLabel;

    // ── Audio controls ──
    @FXML private Label  trackLabel;
    @FXML private Button muteBtn;
    @FXML private javafx.scene.control.Slider volSlider;
    @FXML private Button skipBtn;

    // ── Internal ──
    private TimerModel model;
    private List<Task> tasks;
    private javafx.animation.AnimationTimer animTimer;
    private long animStartTime = System.currentTimeMillis();
    private org.shellord.kaeru.util.PixelArt.Star[] stars;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        model = SceneManager.getTimerModel();
        tasks = SceneManager.getTasks();

        model.setOnBreakStart(() -> Platform.runLater(this::switchToBreakMode));
        model.setOnFocusStart(() -> Platform.runLater(this::switchToFocusMode));
        model.setOnSessionDone(() -> Platform.runLater(this::onSessionDone));

        model.secondsLeftProperty().addListener((obs, old, nw) ->
                Platform.runLater(this::updateTimerDisplay)
        );

        renderTaskList();
        updateCurrentTaskLabel();
        updateTimerDisplay();
        updateStats();
        showFocusView();

        // ── Setup timer rings (proven pattern from fx-progress-circle) ──
        setupRing(timerContainer, timerArc);
        setupRing(breakContainer, breakArc);

        // pre-generate stars (positions stay fixed, only opacity twinkles)
        stars = org.shellord.kaeru.util.PixelArt.generateStars(40, 360, 80, 7);

        // continuous redraw at ~30fps for smooth cloud drift and star twinkle
        animTimer = new javafx.animation.AnimationTimer() {
            long lastUpdate = 0;
            @Override
            public void handle(long now) {
                // throttle to ~30fps to save cpu
                if (now - lastUpdate >= 33_000_000) {
                    drawScene(getRiderProgress());
                    lastUpdate = now;
                }
            }
        };
        animTimer.start();

        drawScene(0.0);
        model.start();
        pauseBtn.setText("⏸  pause");

        // ── start music ──
        AudioManager.play();
        updateAudioBar();
        volSlider.setValue(AudioManager.getVolume());
        volSlider.valueProperty().addListener((obs, old, nw) -> {
            AudioManager.setVolume(nw.doubleValue());
        });
    }

    // ── Button handlers ──

    @FXML
    private void onMuteClick() {
        AudioManager.toggleMute();
        updateAudioBar();
    }

    @FXML
    private void onSkipClick() {
        AudioManager.skip();
        updateAudioBar();
    }

    private void updateAudioBar() {
        trackLabel.setText("♪  " + AudioManager.getCurrentTitle());
        muteBtn.setText(AudioManager.isMuted() ? "🔇" : "♪");
    }

    @FXML
    private void onPauseClick() {
        model.togglePause();
        pauseBtn.setText(model.isRunning() ? "⏸  pause" : "▶  resume");
    }

    @FXML
    private void onSkipBreakClick() {
        model.stop();
        model.skipBreak();
        switchToFocusMode();
        model.start();
    }

    @FXML
    private void onEndEarlyClick() {
        if (animTimer != null) animTimer.stop();
        AudioManager.stop();
        model.stop();
        SceneManager.showSummary();
    }

    // ── Focus / Break mode switching ──

    private void switchToBreakMode() {
        updateBreakView();
        showBreakView();
        updateStats();
        drawScene(0.0); // rider resets to left at start of break
    }

    private void switchToFocusMode() {
        updateCurrentTaskLabel();
        updateTimerDisplay();
        showFocusView();
        updateStats();
        pauseBtn.setText("⏸  pause");
        drawScene(0.0); // rider resets to left at start of focus round
    }

    private void showFocusView() {
        focusView.setVisible(true);
        focusView.setManaged(true);
        breakView.setVisible(false);
        breakView.setManaged(false);
        ctRound.setText(model.getCurrentRound() + "/" + model.getTotalRounds());
    }

    private void showBreakView() {
        focusView.setVisible(false);
        focusView.setManaged(false);
        breakView.setVisible(true);
        breakView.setManaged(true);
    }

    /**
     * Configure a progress ring using the proven fx-progress-circle pattern:
     * the arc is unmanaged (so StackPane doesn't reposition it when its bounds
     * change), and its centerX/centerY are tied to the container's size.
     */
    private void setupRing(javafx.scene.layout.StackPane container,
                           javafx.scene.shape.Arc arc) {
        arc.setManaged(false);
        // initial center
        arc.setCenterX(container.getPrefWidth() / 2);
        arc.setCenterY(container.getPrefHeight() / 2);
        // re-center on resize
        container.widthProperty().addListener((o, ov, nv) ->
                arc.setCenterX(nv.doubleValue() / 2));
        container.heightProperty().addListener((o, ov, nv) ->
                arc.setCenterY(nv.doubleValue() / 2));
    }

    // ── Canvas drawing ──

    private void drawScene(double riderProgress) {
        if (sceneCanvas == null) return;
        GraphicsContext gc = sceneCanvas.getGraphicsContext2D();
        double W = sceneCanvas.getWidth();
        double H = sceneCanvas.getHeight();
        double t = (System.currentTimeMillis() - animStartTime) / 1000.0;

        boolean night = model.isBreak();
        org.shellord.kaeru.util.PixelArt.Mode mode =
                night ? org.shellord.kaeru.util.PixelArt.Mode.NIGHT
                        : org.shellord.kaeru.util.PixelArt.Mode.DAY;

        String topLeft  = night ? "● break" : "● focus";
        String topRight = "round " + model.getCurrentRound() + " / " + model.getTotalRounds();
        boolean moving  = !night && model.isRunning();

        org.shellord.kaeru.util.PixelArt.drawScene(
                gc, W, H, t, mode, riderProgress, stars, moving, topLeft, topRight
        );
    }

    private double getRiderProgress() {
        int total = model.isBreak()
                ? model.getBreakMinutes() * 60
                : model.getFocusMinutes() * 60;
        int elapsed = total - model.getSecondsLeft();
        return Math.min(1.0, Math.max(0.0, (double) elapsed / total));
    }

    private void updateTimerDisplay() {
        if (model.isBreak()) {
            breakTimerLabel.setText(model.getFormattedTime());
            updateBreakArc();
        } else {
            timerLabel.setText(model.getFormattedTime());
            updateFocusArc();
        }
        drawScene(getRiderProgress());
    }

    private void updateFocusArc() {
        if (timerArc == null) return;
        int totalSecs = model.getFocusMinutes() * 60;
        double progress = (double) model.getSecondsLeft() / totalSecs;
        // start full circle (-360), drain to 0 as time runs out
        timerArc.setStartAngle(90);
        timerArc.setLength(-360 * progress);
    }

    private void updateBreakArc() {
        if (breakArc == null) return;
        int totalSecs = model.getBreakMinutes() * 60;
        double progress = (double) model.getSecondsLeft() / totalSecs;
        breakArc.setStartAngle(90);
        breakArc.setLength(-360 * progress);
    }

    private void updateBreakView() {
        // next unchecked task
        String nextTask = tasks.stream()
                .filter(t -> !t.isDone())
                .map(Task::getName)
                .findFirst()
                .orElse(tasks.isEmpty() ? "next focus round" : "all tasks done!");

        breakNextLabel.setText("up next: " + nextTask);
        breakRoundsLabel.setText("round " + model.getRoundsDone() + " done");
        breakTimerLabel.setText(model.getFormattedTime());
    }

    private void updateCurrentTaskLabel() {
        if (tasks.isEmpty()) {
            currentTaskLabel.setText("no tasks — just focus");
            return;
        }

        long remaining = tasks.stream().filter(t -> !t.isDone()).count();

        if (remaining == 0) {
            currentTaskLabel.setText("all tasks done! ✓");
            // auto-end session after a short delay so user sees the message
            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                model.stop();
                SceneManager.showSummary();
            });
            pause.play();
            return;
        }

        String current = tasks.stream()
                .filter(t -> !t.isDone())
                .map(Task::getName)
                .findFirst()
                .orElse("—");
        currentTaskLabel.setText(current);
    }

    private void updateStats() {
        int todaySecs = SceneManager.getTodayFocusSeconds() + model.getFocusSecondsAccum();
        todayTimeLabel.setText(SceneManager.formatSeconds(todaySecs));
        roundsDoneLabel.setText(String.valueOf(model.getRoundsDone()));
    }

    // ── Task list ──

    private void renderTaskList() {
        taskListContainer.getChildren().clear();

        if (tasks.isEmpty()) {
            Label empty = new Label("no tasks — just focus");
            empty.getStyleClass().add("empty-label");
            taskListContainer.getChildren().add(empty);
            return;
        }

        // build a ScrollPane that shows ~3 tasks then scrolls
        VBox innerList = new VBox(3);

        for (Task task : tasks) {
            Label check = new Label(task.isDone() ? "✓" : "□");
            check.getStyleClass().add(task.isDone() ? "custom-cb-done" : "custom-cb");
            check.setMinWidth(18);

            Label name = new Label(task.getName());
            name.getStyleClass().add(task.isDone() ? "task-name-done" : "task-name-label");
            name.setWrapText(true);
            HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);

            HBox row = new HBox(10, check, name);
            row.getStyleClass().add("task-row");
            if (task.isDone()) row.getStyleClass().add("task-done");
            row.setOnMouseClicked(e -> {
                task.setDone(!task.isDone());
                renderTaskList();
                updateCurrentTaskLabel();
            });

            innerList.getChildren().add(row);
        }

        if (tasks.size() <= 3) {
            // no scroll needed — just add directly
            taskListContainer.getChildren().add(innerList);
        } else {
            // scrollable, fixed height showing ~3 tasks
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(innerList);
            scroll.setFitToWidth(true);
            scroll.setPrefViewportHeight(120); // ~3 tasks visible
            scroll.setMaxHeight(120);
            scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.getStyleClass().add("task-scroll");
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            taskListContainer.getChildren().add(scroll);
        }
    }

    private void onSessionDone() {
        if (animTimer != null) animTimer.stop();
        AudioManager.stop();
        SceneManager.showSummary();
    }
}