package org.shellord.kaeru.model;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.util.Duration;

/**
 * Manages the full Pomodoro cycle:
 *   focus → break → focus → break → ... × totalRounds → done
 *
 * Consumers listen to the properties and callbacks
 * rather than polling state directly.
 */
public class TimerModel {

    // ── Settings (set before calling start()) ──
    private final int focusMinutes;
    private final int breakMinutes;
    private final int totalRounds;

    // ── Observable state ──
    private final IntegerProperty secondsLeft = new SimpleIntegerProperty();
    private final BooleanProperty isBreak     = new SimpleBooleanProperty(false);
    private final BooleanProperty isRunning   = new SimpleBooleanProperty(false);
    private final IntegerProperty currentRound = new SimpleIntegerProperty(1);
    private final IntegerProperty roundsDone   = new SimpleIntegerProperty(0);

    // Total seconds spent in focus across all rounds this session
    private int focusSecondsAccum = 0;

    // ── Callbacks ──
    private Runnable onBreakStart;   // called when focus ends → break begins
    private Runnable onFocusStart;   // called when break ends → focus begins
    private Runnable onSessionDone;  // called when all rounds complete

    // ── Internal ──
    private Timeline timeline;

    public TimerModel(int focusMinutes, int breakMinutes, int totalRounds) {
        this.focusMinutes = focusMinutes;
        this.breakMinutes = breakMinutes;
        this.totalRounds  = totalRounds;
        secondsLeft.set(focusMinutes * 60);
    }

    // ── Control ──

    public void start() {
        if (timeline != null) timeline.stop();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        isRunning.set(true);
    }

    public void pause() {
        if (timeline != null) timeline.pause();
        isRunning.set(false);
    }

    public void resume() {
        if (timeline != null) timeline.play();
        isRunning.set(true);
    }

    public void stop() {
        if (timeline != null) timeline.stop();
        isRunning.set(false);
    }

    public void togglePause() {
        if (isRunning.get()) pause();
        else resume();
    }

    /**
     * Skips the current break and moves to the next focus round.
     * Call stop() before this, then start() after.
     */
    public void skipBreak() {
        isBreak.set(false);
        currentRound.set(currentRound.get() + 1);
        secondsLeft.set(focusMinutes * 60);
    }

    // ── Internal tick ──

    private void tick() {
        int secs = secondsLeft.get() - 1;
        secondsLeft.set(secs);

        if (!isBreak.get()) focusSecondsAccum++;

        if (secs <= 0) onPeriodEnd();
    }

    private void onPeriodEnd() {
        if (!isBreak.get()) {
            // focus round finished
            roundsDone.set(roundsDone.get() + 1);

            if (roundsDone.get() >= totalRounds) {
                // all rounds done
                stop();
                if (onSessionDone != null) onSessionDone.run();
                return;
            }

            // start break
            isBreak.set(true);
            secondsLeft.set(breakMinutes * 60);
            if (onBreakStart != null) onBreakStart.run();

        } else {
            // break finished → next focus round
            isBreak.set(false);
            currentRound.set(currentRound.get() + 1);
            secondsLeft.set(focusMinutes * 60);
            if (onFocusStart != null) onFocusStart.run();
        }
    }

    // ── Formatted time helper ──

    public String getFormattedTime() {
        int secs = secondsLeft.get();
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    // ── Getters for settings ──

    public int getFocusMinutes()  { return focusMinutes; }
    public int getBreakMinutes()  { return breakMinutes; }
    public int getTotalRounds()   { return totalRounds; }
    public int getFocusSecondsAccum() { return focusSecondsAccum; }

    // ── Observable properties ──

    public IntegerProperty secondsLeftProperty()  { return secondsLeft; }
    public BooleanProperty isBreakProperty()      { return isBreak; }
    public BooleanProperty isRunningProperty()    { return isRunning; }
    public IntegerProperty currentRoundProperty() { return currentRound; }
    public IntegerProperty roundsDoneProperty()   { return roundsDone; }

    public int getSecondsLeft()   { return secondsLeft.get(); }
    public boolean isBreak()      { return isBreak.get(); }
    public boolean isRunning()    { return isRunning.get(); }
    public int getCurrentRound()  { return currentRound.get(); }
    public int getRoundsDone()    { return roundsDone.get(); }

    // ── Callback setters ──

    public void setOnBreakStart(Runnable cb)   { this.onBreakStart = cb; }
    public void setOnFocusStart(Runnable cb)   { this.onFocusStart = cb; }
    public void setOnSessionDone(Runnable cb)  { this.onSessionDone = cb; }
}