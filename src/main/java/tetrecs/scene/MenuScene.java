package tetrecs.scene;

import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tetrecs.ui.GamePane;
import tetrecs.ui.GameWindow;

import java.io.File;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.Objects;

/**
 * The main menu of the game. Provides a gateway to the rest of the game.
 */
public class MenuScene extends BaseScene {

    private final Logger logger = LogManager.getLogger(MenuScene.class);

    /**
     * Holds string list of all available songs to play
     */
    public static LinkedList<String> musicToPlay = new LinkedList<>();

    /**
     * Refers to the index of currently playing song
     */
    public static int musicIndex = 0;

    /**
     * Create a new menu scene
     * @param gameWindow the Game Window this will be displayed in
     */
    public MenuScene(GameWindow gameWindow) {
        super(gameWindow);
        logger.info("Creating Menu Scene");
    }

    /**
     * Build the UI layout and add any event handlers.
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        var menuPane = new StackPane();
        menuPane.setMaxWidth(gameWindow.getWidth());
        menuPane.setMaxHeight(gameWindow.getHeight());
        menuPane.getStyleClass().add("menu-background");
        root.getChildren().add(menuPane);

        var mainPane = new BorderPane();
        menuPane.getChildren().add(mainPane);

        // Title
        var title = new ImageView(Objects.requireNonNull(this.getClass().getResource("/images/TetrECS.png")).toExternalForm());
        title.setFitHeight(100);
        title.setFitWidth(gameWindow.getWidth() - 325);
        // Add simple rotation animation to title image
        var rotateTransition = new RotateTransition(new Duration(3000), title);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setFromAngle(-7);
        rotateTransition.setToAngle(7);
        rotateTransition.setAutoReverse(true);
        rotateTransition.play();
        BorderPane.setAlignment(title, Pos.TOP_CENTER);
        BorderPane.setMargin(title, new Insets(25, 25, 25, 25));
        mainPane.setTop(title);

        // Contains scene buttons
        var sceneButtons = new VBox(25);

        // Singleplayer
        var singlePlayer = new Text("Singleplayer");
        singlePlayer.getStyleClass().add("menuItem");
        singlePlayer.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // Load new scene
                Multimedia.playSound("pling.wav");
                startGame();
            }
        });
        sceneButtons.getChildren().add(singlePlayer);

        // Multiplayer
        var multiPlayer = new Text("Multiplayer");
        multiPlayer.getStyleClass().add("menuItem");
        multiPlayer.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Multimedia.playSound("pling.wav");
                startLobby();
            }
        });
        sceneButtons.getChildren().add(multiPlayer);

        // Help
        var help = new Text("How to play");
        help.getStyleClass().add("menuItem");
        help.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Multimedia.playSound("pling.wav");
                startInstruction();
            }
        });
        sceneButtons.getChildren().add(help);

        // Quit
        var quit = new Text("Quit");
        quit.getStyleClass().add("menuItem");
        quit.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Multimedia.playSound("pling.wav");
                quit();
            }
        });
        sceneButtons.getChildren().add(quit);

        sceneButtons.setAlignment(Pos.TOP_CENTER);
        BorderPane.setMargin(sceneButtons, new Insets(125, 125, 125, 125));
        mainPane.setBottom(sceneButtons);
    }

    /**
     * Initialise the menu scene and add any necessary event handlers
     */
    @Override
    public void initialise() {

        // Pressing escape quits the game
        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if (key.getCode() == KeyCode.ESCAPE) {
                logger.info("Exiting program");
                System.exit(0);
            }
        });

        // Create folder to place music if it does not exist
        if (new File("usermusic").mkdirs()) {
            logger.error("Cannot create folder");
        }

        // Check /music and external music folder for any wav, mp3 or mp4 files and add to list
        musicToPlay.add(Objects.requireNonNull(this.getClass().getResource("/music/game.wav")).toExternalForm());

        File folder = new File("usermusic");
        File[] listOfFiles = folder.listFiles();

        // Check files in directory for valid types and convert into URL to play
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    if (file.toString().endsWith(".mp3") || file.toString().endsWith(".wav") || file.toString().endsWith(".mp4")) {
                        try {
                            musicToPlay.add(file.toURI().toURL().toString());
                        } catch (MalformedURLException malformedURLException) {
                            logger.error("Malformed URL");
                        }
                    }
                }
            }
        }

        // Play music
        Multimedia.playBackgroundMusic("menu.mp3");
    }

    // Event handling
    /**
     * Handler assigned to singleplayer, launches ChallengeScene.
     */
    private void startGame() {
        gameWindow.startChallenge();
    }

    /**
     * Handler assigned to multiplayer, launches LobbyScene.
     */
    private void startLobby() { gameWindow.startLobby(); }

    /**
     * Handler assigned to instruction, launches InstructionScene.
     */
    private void startInstruction() { gameWindow.startInstructionMenu(); }

    /**
     * Terminates the game.
     */
    private void quit() {
        logger.info("Exiting program");
        System.exit(0) ;
    }
}
