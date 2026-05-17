package org.shellord.kaeru.util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton-style audio manager.
 * Handles lofi track playback, volume, mute and skip.
 */
public class AudioManager {

    public record Track(String id, String title, String file) {}

    private static final List<Track> tracks = new ArrayList<>();
    private static MediaPlayer player;
    private static int currentIndex = 0;
    private static double volume    = 0.5;
    private static boolean muted    = false;

    // ── Load tracks from hardcoded list (mirrors music.json) ──
    static {
        tracks.add(new Track("lofi_rain",     "lofi rain",       "lofi_rain.mp3"));
        tracks.add(new Track("lofi_cafe",     "jazz café",       "lofi_cafe.mp3"));
        tracks.add(new Track("lofi_lanterns", "hanging lanterns","lofi_lanterns.mp3"));
        tracks.add(new Track("lofi_snow",     "first snow",      "lofi_snow.mp3"));
        tracks.add(new Track("lofi_waves",    "waves",           "lofi_waves.mp3"));
    }

    // ── Playback ──

    public static void play() {
        if (tracks.isEmpty()) return;

        stopAndDispose();

        Track track = tracks.get(currentIndex);
        URL url = AudioManager.class.getResource(
                "/org/shellord/kaeru/audio/" + track.file()
        );

        if (url == null) {
            System.err.println("[AudioManager] File not found: " + track.file());
            return;
        }

        player = new MediaPlayer(new Media(url.toExternalForm()));
        player.setVolume(muted ? 0 : volume);
        player.setCycleCount(MediaPlayer.INDEFINITE); // loop each track
        player.setOnEndOfMedia(() -> {
            // auto-advance when track ends (if not looping)
        });
        player.play();
    }

    public static void stop() {
        stopAndDispose();
    }

    public static void skip() {
        currentIndex = (currentIndex + 1) % tracks.size();
        play();
    }

    public static void previous() {
        currentIndex = (currentIndex - 1 + tracks.size()) % tracks.size();
        play();
    }

    // ── Volume ──

    public static void setVolume(double v) {
        volume = Math.max(0, Math.min(1, v));
        if (player != null && !muted) player.setVolume(volume);
    }

    public static double getVolume() { return volume; }

    // ── Mute ──

    public static boolean isMuted() { return muted; }

    public static void toggleMute() {
        muted = !muted;
        if (player != null) player.setVolume(muted ? 0 : volume);
    }

    // ── Info ──

    public static String getCurrentTitle() {
        if (tracks.isEmpty()) return "—";
        return tracks.get(currentIndex).title();
    }

    public static int getCurrentIndex()  { return currentIndex; }
    public static int getTrackCount()    { return tracks.size(); }
    public static List<Track> getTracks() { return tracks; }

    public static boolean isPlaying() {
        return player != null &&
                player.getStatus() == MediaPlayer.Status.PLAYING;
    }

    // ── Internal ──

    private static void stopAndDispose() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    private AudioManager() {}
}