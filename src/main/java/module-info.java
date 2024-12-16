module uk.ac.soton.comp1206 {
    requires java.scripting;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires org.apache.logging.log4j;
    requires java.sql;
    requires org.java_websocket;
    opens tetrecs.ui to javafx.fxml;
    exports tetrecs.ui;
    exports tetrecs.network;
    exports tetrecs.scene;
    exports tetrecs.event;
    exports tetrecs.component;
    exports tetrecs.game;
  opens tetrecs.scene to javafx.fxml;
    exports tetrecs;
}