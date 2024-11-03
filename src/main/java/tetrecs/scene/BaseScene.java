package tetrecs.scene;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import tetrecs.ui.GamePane;
import tetrecs.ui.GameWindow;

import java.util.Objects;

/**
 * A Base Scene used in the game. Handles common functionality between all scenes.
 */
public abstract class BaseScene {

    /**
     * Single window where scenes are displayed.
     */
    protected final GameWindow gameWindow;

    /**
     * Root nodes contains all other existing nodes and establishes a hierarchy of nodes.
     */
    protected GamePane root;

    /**
     * Establishes layout for the root node and other nodes
     */
    protected Scene scene;

    /**
     * Create a new scene, passing in the GameWindow the scene will be displayed in
     * @param gameWindow the game window
     */
    public BaseScene(GameWindow gameWindow) {
        this.gameWindow = gameWindow;
    }

    /**
     * Initialise this scene. Called after creation
     */
    public abstract void initialise();

    /**
     * Build the layout of the scene
     */
    public abstract void build();

    /**
     * Create a new JavaFX scene using the root contained within this scene
     */
    public Scene setScene() {
        var previous = gameWindow.getScene();
        Scene scene = new Scene(root, previous.getWidth(), previous.getHeight(), Color.BLACK);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style/game.css")).toExternalForm());
        this.scene = scene;
        return scene;
    }

    /**
     * Get the JavaFX scene contained inside
     * @return JavaFX scene
     */
    public Scene getScene() {
        return this.scene;
    }
}
