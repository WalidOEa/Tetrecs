package uk.ac.soton.comp1206.component;

import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Map;

/**
 * Custom UI component that receives current channels within the server and creates text label reflecting the name and binds
 * an event handler to each.
 */
public class LobbyList extends VBox {

    /**
     * Create the component
     * @param lobby update the component using the contents of the observable map
     */
    public LobbyList(ObservableMap<String, EventHandler<MouseEvent>> lobby) {
        lobby.addListener((MapChangeListener<? super String, ? super EventHandler<MouseEvent>>) change -> build(lobby));
    }

    /**
     * Clear old nodes and reconstruct again with newer nodes.
     * @param lobby lobbies to create
     */
    public void build(ObservableMap<String, EventHandler<MouseEvent>> lobby) {
        this.getChildren().clear();
        var VBox = new VBox(15);

        for (Map.Entry<String, EventHandler<MouseEvent>> entry : lobby.entrySet()) {
            var channelText = new Text(entry.getKey());
            channelText.getStyleClass().add("channelItem");
            channelText.setOnMouseClicked(entry.getValue());
            VBox.getChildren().add(channelText);
        }

        this.getChildren().add(VBox);
    }
}
