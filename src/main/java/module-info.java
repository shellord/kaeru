module org.shellord.kaeru {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    // open to javafx.fxml so it can access controllers via reflection
    opens org.shellord.kaeru             to javafx.fxml;
    opens org.shellord.kaeru.controller  to javafx.fxml;
    opens org.shellord.kaeru.model       to javafx.fxml;
    opens org.shellord.kaeru.util        to javafx.fxml;

    exports org.shellord.kaeru;
    exports org.shellord.kaeru.controller;
    exports org.shellord.kaeru.model;
    exports org.shellord.kaeru.util;
}