package uk.ac.soton.comp1206.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.App;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.scene.*;

import static uk.ac.soton.comp1206.scene.Multimedia.musicCleanUp;

/**
 * The GameWindow is the single window for the game where everything takes place. To move between screens in the game,
 * we simply change the scene.
 *
 * The GameWindow has methods to launch each of the different parts of the game by switching scenes.
 */
public class GameWindow {

    private static final Logger logger = LogManager.getLogger(GameWindow.class);

    /**
     * Width of window
     */
    private final int width;

    /**
     * Height of window
     */
    private final int height;

    /**
     * Contains root node
     */
    private final Stage stage;

    /**
     * Handles common functionality between scenes
     */
    private BaseScene currentScene;

    /**
     * Displays scene
     */
    private Scene scene;

    final Communicator communicator;

    /**
     * Create a new GameWindow attached to the given stage with the specified width and height
     * @param stage stage
     * @param width width
     * @param height height
     */
    public GameWindow(Stage stage, int width, int height) {
        this.width = width;
        this.height = height;

        this.stage = stage;

        //Setup window
        setupStage();

        //Setup resources
        setupResources();

        //Setup default scene
        setupDefaultScene();

        //Setup communicator
        communicator = new Communicator("ws://ofb-labs.soton.ac.uk:9700");

        startIntro();
    }

    /**
     * Set up the font and any other resources we need.
     */
    private void setupResources() {
        logger.info("Loading resources");

        //We need to load fonts here due to the Font loader bug with spaces in URLs in the CSS files
        Font.loadFont(getClass().getResourceAsStream("/style/Orbitron-Regular.ttf"),32);
        Font.loadFont(getClass().getResourceAsStream("/style/Orbitron-Bold.ttf"),32);
        Font.loadFont(getClass().getResourceAsStream("/style/Orbitron-ExtraBold.ttf"),32);
    }

    /**
     * Loads and displays the splash screen logo at the start
     */
    public void startIntro() {
        loadScene(new SplashLogoScene(this));
    }

    /**
     * Display the main menu.
     */
    public void startMenu() {
        loadScene(new MenuScene(this)); }

    /**
     * Display the single player challenge.
     */
    public void startChallenge() { loadScene(new ChallengeScene(this)); }

    /**
     * Display the lobby scene which then leads into the multiplayer scene
     */
    public void startLobby() { loadScene(new LobbyScene(this, communicator, scene));}

    /**
     * Display the multiplayer scene
     */
    public void startMultiplayer() { loadScene(new MultiplayerScene(this, communicator, scene));}

    /**
     * Display the instruction menu.
     */
    public void startInstructionMenu() {
        loadScene(new InstructionScene(this));
    }

    /**
     * Display the score scene after challenge scene.
     */
    public void startScore(Game game) {
        loadScene(new ScoresScene(this, game, communicator));}

    /**
     * Set up the default settings for the stage itself (the window), such as the title and minimum width and height.
     */
    public void setupStage() {
        stage.setTitle("TetrECS");
        stage.setMinWidth(width);
        stage.setMinHeight(height + 20);
        stage.setOnCloseRequest(ev -> App.getInstance().shutdown());
    }

    /**
     * Load a given scene which extends BaseScene and switch over.
     * @param newScene new scene to load
     */
    public void loadScene(BaseScene newScene) {
        // Cleanup remains of the previous scene
        cleanup();

        // Create the new scene and set it up
        newScene.build();
        currentScene = newScene;
        scene = newScene.setScene();
        stage.setScene(scene);

        // Initialise the scene when ready
        Platform.runLater(() -> currentScene.initialise());
    }

    /**
     * Set up the default scene (an empty black scene) when no scene is loaded.
     */
    public void setupDefaultScene() {
        this.scene = new Scene(new Pane(),width,height, Color.BLACK);
        stage.setScene(this.scene);
    }

    /**
     * When switching scenes, perform any cleanup needed, such as removing previous listeners, stopping music
     * from playing.
     */
    public void cleanup() {
        logger.info("Clearing up previous scene");

        logger.info("Removing listeners");
        communicator.clearListeners();

        try {
            logger.info("Stopping music");
            musicCleanUp();
        } catch (NullPointerException exception) {
            logger.error("No music to stop");
        }
    }

    /**
     * Get the current scene being displayed
     * @return scene
     */
    public Scene getScene() {
        return scene;
    }

    /**
     * Get the width of the Game Window
     * @return width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of the Game Window
     * @return height
     */
    public int getHeight() {
        return this.height;
    }
}
