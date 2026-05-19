package org.shellord.kaeru;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Must be set before AWT initializes — makes this a menu-bar-only
        // app on macOS (no Dock icon, no cmd-tab entry).
        System.setProperty("apple.awt.UIElement", "true");
        System.setProperty("apple.awt.application.appearance", "system");
        Application.launch(KaeruApplication.class, args);
    }
}
