package org.shellord.kaeru.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.shellord.kaeru.model.Task;
import org.shellord.kaeru.model.TimerModel;
import org.shellord.kaeru.util.SceneManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SetupController implements Initializable {

    @FXML private Canvas setupCanvas;

    // ── Timer config ──
    @FXML private ToggleButton focus15Btn;
    @FXML private ToggleButton focus25Btn;
    @FXML private ToggleButton focus50Btn;
    @FXML private ToggleButton focusCustomBtn;
    @FXML private TextField    customFocusField;

    @FXML private ToggleButton break5Btn;
    @FXML private ToggleButton break10Btn;
    @FXML private ToggleButton break15Btn;
    @FXML private ToggleButton breakCustomBtn;
    @FXML private TextField    customBreakField;

    @FXML private ToggleButton rounds2Btn;
    @FXML private ToggleButton rounds4Btn;
    @FXML private ToggleButton rounds6Btn;
    @FXML private ToggleButton roundsCustomBtn;
    @FXML private TextField    customRoundsField;

    // ── Tasks ──
    @FXML private VBox     taskListContainer;
    @FXML private TextField newTaskField;
    @FXML private Button   addTaskConfirmBtn;

    // ── Start ──
    @FXML private Button startBtn;

    // ── State ──
    private int focusMinutes = 25;
    private int breakMinutes = 5;
    private int totalRounds  = 4;
    private final List<Task> tasks = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // focus toggle group
        ToggleGroup focusGroup = new ToggleGroup();
        focus15Btn.setToggleGroup(focusGroup);
        focus25Btn.setToggleGroup(focusGroup);
        focus50Btn.setToggleGroup(focusGroup);
        focusCustomBtn.setToggleGroup(focusGroup);
        focus25Btn.setSelected(true);

        focusGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == focus15Btn)       { focusMinutes = 15; hideCustomField(); }
            else if (nw == focus25Btn)  { focusMinutes = 25; hideCustomField(); }
            else if (nw == focus50Btn)  { focusMinutes = 50; hideCustomField(); }
            else if (nw == focusCustomBtn) { showCustomField(); }
        });

        hideCustomField();
        customFocusField.setPromptText("enter minutes...");
        customFocusField.textProperty().addListener((obs, old, nw) -> {
            try {
                int v = Integer.parseInt(nw.trim());
                if (v > 0) focusMinutes = v;
            } catch (NumberFormatException ignored) {}
        });

        // break toggle group
        ToggleGroup breakGroup = new ToggleGroup();
        break5Btn.setToggleGroup(breakGroup);
        break10Btn.setToggleGroup(breakGroup);
        break15Btn.setToggleGroup(breakGroup);
        breakCustomBtn.setToggleGroup(breakGroup);
        break5Btn.setSelected(true);

        breakGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == break5Btn)           { breakMinutes = 5;  hideBreakField(); }
            else if (nw == break10Btn)     { breakMinutes = 10; hideBreakField(); }
            else if (nw == break15Btn)     { breakMinutes = 15; hideBreakField(); }
            else if (nw == breakCustomBtn) { showBreakField(); }
        });

        hideBreakField();
        customBreakField.textProperty().addListener((obs, old, nw) -> {
            try {
                int v = Integer.parseInt(nw.trim());
                if (v > 0) breakMinutes = v;
            } catch (NumberFormatException ignored) {}
        });

        // rounds toggle group
        ToggleGroup roundsGroup = new ToggleGroup();
        rounds2Btn.setToggleGroup(roundsGroup);
        rounds4Btn.setToggleGroup(roundsGroup);
        rounds6Btn.setToggleGroup(roundsGroup);
        roundsCustomBtn.setToggleGroup(roundsGroup);
        rounds4Btn.setSelected(true);

        roundsGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == rounds2Btn)           { totalRounds = 2; hideRoundsField(); }
            else if (nw == rounds4Btn)      { totalRounds = 4; hideRoundsField(); }
            else if (nw == rounds6Btn)      { totalRounds = 6; hideRoundsField(); }
            else if (nw == roundsCustomBtn) { showRoundsField(); }
        });

        hideRoundsField();
        customRoundsField.textProperty().addListener((obs, old, nw) -> {
            try {
                int v = Integer.parseInt(nw.trim());
                if (v > 0) totalRounds = v;
            } catch (NumberFormatException ignored) {}
        });

        // add some demo tasks
        addTask("Review chapter 3");
        addTask("Practice problems");
        addTask("Make summary notes");

        // draw the pixel art scene header
        javafx.application.Platform.runLater(this::drawSetupScene);
    }

    private void drawSetupScene() {
        if (setupCanvas == null) return;
        GraphicsContext gc = setupCanvas.getGraphicsContext2D();
        double W = setupCanvas.getWidth();
        double H = setupCanvas.getHeight();
        double t = System.currentTimeMillis() / 1000.0;

        org.shellord.kaeru.util.PixelArt.drawScene(
                gc, W, H, t,
                org.shellord.kaeru.util.PixelArt.Mode.DAY,
                -1,    // rider parked at left
                null,
                false, // not moving
                "● setup",
                null
        );
    }

    @FXML
    private void onAddTaskClick() {
        String name = newTaskField.getText().trim();
        if (!name.isEmpty()) {
            addTask(name);
            newTaskField.clear();
        }
    }

    private void addTask(String name) {
        Task task = new Task(name);
        tasks.add(task);
        renderTaskList();
    }

    private void renderTaskList() {
        taskListContainer.getChildren().clear();

        VBox innerList = new VBox(3);
        for (Task task : tasks) {
            Label nameLabel = new Label(task.getName());
            nameLabel.getStyleClass().add("task-name-label");
            nameLabel.setWrapText(true);
            HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

            Button del = new Button("×");
            del.getStyleClass().add("task-delete-btn");
            del.setOnAction(e -> {
                tasks.remove(task);
                renderTaskList();
            });

            HBox row = new HBox(8, nameLabel, del);
            row.getStyleClass().add("task-row");
            innerList.getChildren().add(row);
        }

        if (tasks.size() <= 3) {
            taskListContainer.getChildren().add(innerList);
        } else {
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(innerList);
            scroll.setFitToWidth(true);
            scroll.setPrefViewportHeight(115);
            scroll.setMaxHeight(115);
            scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.getStyleClass().add("task-scroll");
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            taskListContainer.getChildren().add(scroll);
        }
    }

    @FXML
    private void onStartClick() {
        // reset all tasks
        tasks.forEach(t -> t.setDone(false));

        TimerModel model = new TimerModel(focusMinutes, breakMinutes, totalRounds);
        SceneManager.showFocus(model, new ArrayList<>(tasks));
    }

    private void showCustomField() {
        customFocusField.setVisible(true);
        customFocusField.setManaged(true);
        customFocusField.requestFocus();
    }

    private void hideCustomField() {
        customFocusField.setVisible(false);
        customFocusField.setManaged(false);
    }

    private void showBreakField() {
        customBreakField.setVisible(true);
        customBreakField.setManaged(true);
        customBreakField.requestFocus();
    }

    private void hideBreakField() {
        customBreakField.setVisible(false);
        customBreakField.setManaged(false);
    }

    private void showRoundsField() {
        customRoundsField.setVisible(true);
        customRoundsField.setManaged(true);
        customRoundsField.requestFocus();
    }

    private void hideRoundsField() {
        customRoundsField.setVisible(false);
        customRoundsField.setManaged(false);
    }
}