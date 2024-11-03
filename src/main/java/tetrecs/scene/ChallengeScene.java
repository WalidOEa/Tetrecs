package tetrecs.scene;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tetrecs.component.GameBlock;
import tetrecs.component.GameBoard;
import tetrecs.component.PieceBoard;
import tetrecs.game.Game;
import tetrecs.ui.GamePane;
import tetrecs.ui.GameWindow;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ChallengeScene is responsible for constructing the UI and handling events pertaining to the 'Challenge mode' of TetrECS such
 * as key presses. This also serves as a bridge connecting the model and view together, in this case being Game and GameBoard.
 * Upon game over, this scene transitions into the scores scene.
 */
public class ChallengeScene extends BaseScene {

    private final Logger logger = LogManager.getLogger(ChallengeScene.class);

    /**
     * Used to retrieve information containing game state to manipulate UI.
     */
    private Game game;

    /**
     * Displays the currentPiece. Event handlers are bound to the board to allow for Game manipulation such as rotations.
     */
    protected PieceBoard currentPieceBoard;

    /**
     * Displays the nextPiece. Similar to currentPiece however only allows for swapping tiles.
     */
    protected PieceBoard nextPieceBoard;

    /**
     * UI reflection of logic pertaining to board. Event handlers are bound to the board to allow for Game manipulation such as
     * placing blocks and rotations.
     */
    protected GameBoard board;

    /**
     * Reflects the duration of each level. Resets when a piece is played or a life lost.
     */
    protected Rectangle timerBar = createTimerBar();

    /**
     * Possess keyframes that manipulates timerBar states, such as the duration of each timer.
     */
    protected Timeline timeline = new Timeline();

    /**
     * Contains buttons allowing for pausing and stopping of music and text displaying the current song.
     */
    protected VBox musicPlayer = new VBox();

    /**
     * Current song being played in MultiMedia class.
     */
    protected SimpleStringProperty songNameProperty = new SimpleStringProperty(MenuScene.musicToPlay.get(0).split("/")[MenuScene.musicToPlay.get(0).split("/").length - 1]);

    /**
     * Create a new Single Player challenge scene.
     * @param gameWindow the Game Window
     */
    public ChallengeScene(GameWindow gameWindow) {
        super(gameWindow);

        logger.info("Creating new Challenge Scene");
    }

    /**
     * Build the Challenge window and bind any event handlers to necessary components.
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        setupGame();

        // Initiate root object
        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        var challengePane = new StackPane();
        challengePane.setMaxWidth(gameWindow.getWidth());
        challengePane.setMaxHeight(gameWindow.getHeight());
        challengePane.getStyleClass().add("challenge-background");

        root.getChildren().add(challengePane);

        var mainPane = new BorderPane();
        challengePane.getChildren().add(mainPane);

        // Contains top nodes such as score, lives and title of mode
        var topNodesStack = new StackPane();

        var scoreVBox = new VBox();

        // Current score
        // Score title
        var scoreTitle = new Text("Score");
        scoreTitle.getStyleClass().add("lives");
        scoreVBox.getChildren().add(scoreTitle);

        // Score text
        var score = new Text("");
        score.getStyleClass().add("score");
        // Bound to properties
        score.textProperty().bind(game.scoreProperty().asString());
        scoreVBox.getChildren().add(score);
        scoreVBox.setAlignment(Pos.TOP_LEFT);
        topNodesStack.getChildren().add(scoreVBox);

        // Title of game mode
        var challengeTitle = new Text("Challenge Mode");
        challengeTitle.getStyleClass().add("title");
        topNodesStack.getChildren().add(challengeTitle);

        // Contains lives and current lives
        var livesVBox = new VBox();

        // Lives title
        var livesTitle = new Text("Lives");
        livesTitle.getStyleClass().add("lives");
        livesVBox.getChildren().add(livesTitle);

        // Current lives
        var lives = new Text("");
        lives.getStyleClass().add("score");
        lives.textProperty().bind(game.livesProperty().asString());
        livesVBox.getChildren().add(lives);
        livesVBox.setAlignment(Pos.TOP_RIGHT);
        topNodesStack.getChildren().add(livesVBox);
        mainPane.setTop(topNodesStack);

        // Board
        var boardHBox = new HBox(5);
        var boardStack = new StackPane();

        board = new GameBoard(game.getGrid(), gameWindow.getWidth()/2.0, gameWindow.getWidth()/2.0);
        board.setAlignment(Pos.CENTER);
        StackPane.setMargin(board, new Insets(30, 30, 30, 30));
        boardStack.getChildren().add(board);

        // Multiplier
        var multiplier = new Text("");
        multiplier.getStyleClass().add("multiplier");
        multiplier.textProperty().bind(game.multiplierProperty().asString());
        // Apply a simple rotation animation onto text node
        var rotateTransition = new RotateTransition(new Duration(1500), multiplier);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setFromAngle(-35);
        rotateTransition.setToAngle(35);
        rotateTransition.setAutoReverse(true);
        rotateTransition.play();
        StackPane.setAlignment(multiplier, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(multiplier, new Insets(40, 40, 40, 40));
        boardStack.getChildren().add(multiplier);
        boardHBox.getChildren().add(boardStack);

        // Pieceboards
        var pieceBoardVBox = new VBox(25);
        var currentPieceBoardHBox = new HBox(15);
        var nextPieceBoardHBox = new HBox(15);

        // Contains high score
        var hiScoreVBox = new VBox(15);

        // High score title
        var hiScoreTitle = new Text("High Score");
        hiScoreTitle.getStyleClass().add("hiscore");
        hiScoreVBox.getChildren().add(hiScoreTitle);

        // Get high score from file
        getHighScore("scores.txt");

        // High score
        var hiScore = new Text();
        hiScore.getStyleClass().add("hiscore");
        hiScore.textProperty().bind(game.hiScoreProperty().asString());
        hiScoreVBox.getChildren().add(hiScore);

        // Contains level
        var levelVBox = new VBox();

        // Level title
        var levelTitle = new Text("Level");
        levelTitle.getStyleClass().add("level");
        levelVBox.getChildren().add(levelTitle);

        // Level number
        var level = new Text("");
        level.getStyleClass().add("level");
        level.textProperty().bind(game.levelProperty().asString());
        levelVBox.getChildren().add(level);

        // Pieceboards
        nextPieceBoard = new PieceBoard(3, 3, gameWindow.getWidth()/8.7, gameWindow.getWidth()/8.7);
        currentPieceBoard = new PieceBoard(3, 3, gameWindow.getWidth()/7.0, gameWindow.getWidth()/7.0);

        // Power ups
        var powerUpsVBox = new VBox();

        // Nuke powerup
        var nukePowerUp = new Text("Nuke (1000 points)");
        nukePowerUp.getStyleClass().add("powerUpItem");

        powerUpsVBox.getChildren().add(nukePowerUp);

        // Multiplier powerup
        var multiplierPowerUp = new Text("15x multiplier (250 points)");
        multiplierPowerUp.getStyleClass().add("powerUpItem");

        powerUpsVBox.getChildren().add(multiplierPowerUp);

        // New life power up
        var newLifePowerUp = new Text("One UP (2000 points)");
        newLifePowerUp .getStyleClass().add("powerUpItem");

        powerUpsVBox.getChildren().add(newLifePowerUp );

        // Event handlers for powerups
        nukePowerUp.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (game.getScore() >= 1000) {
                    game.bomb();
                    nukePowerUp.setDisable(true);
                    Multimedia.playSound("bomb.mp3");
                    // Schedule task that enables power up again
                    Timer timer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            nukePowerUp.setDisable(false);
                        }
                    };
                    // Cooldown period of 1 minute
                    timer.schedule(timerTask, 60000);
                }
            }
        });

        multiplierPowerUp.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (game.getScore() >= 250) {
                    game.setScore(game.getScore() - 250);
                    game.setMultiplier(15);
                    multiplierPowerUp.setDisable(true);
                    Timer timer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            multiplierPowerUp.setDisable(false);
                        }
                    };
                    // Cooldown period of 20 seconds
                    timer.schedule(timerTask, 20000);
                }
            }
        });

        newLifePowerUp.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (game.getScore() >= 2000) {
                    Multimedia.playSound("lifegain.wav");
                    game.newLife();
                    newLifePowerUp.setDisable(true);
                    Timer timer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            newLifePowerUp .setDisable(false);
                        }
                    };
                    // Cooldown period of 2 minutes
                    timer.schedule(timerTask, 120000);
                }
            }
        });

        pieceBoardVBox.getChildren().add(powerUpsVBox);

        currentPieceBoardHBox.getChildren().add(currentPieceBoard);
        currentPieceBoardHBox.getChildren().add(hiScoreVBox);
        pieceBoardVBox.getChildren().add(currentPieceBoardHBox);
        nextPieceBoardHBox.getChildren().add(nextPieceBoard);
        nextPieceBoardHBox.getChildren().add(levelVBox);
        pieceBoardVBox.getChildren().add(nextPieceBoardHBox);
        pieceBoardVBox.setAlignment(Pos.CENTER);
        BorderPane.setMargin(pieceBoardVBox, new Insets(5, 5, 5, 5));
        boardHBox.getChildren().add(pieceBoardVBox);

        mainPane.setLeft(boardHBox);

        // Timer bar
        timerBar = createTimerBar();

        createTimer(game.getDelay());

        mainPane.setBottom(timerBar);

        // Music player
        var songTitle = new Text("");
        songTitle.getStyleClass().add("channelItem");
        songTitle.textProperty().bind(songNameProperty);
        var wrapper = new VBox();
        wrapper.getChildren().add(songTitle);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        musicPlayer.getChildren().add(wrapper);

        var musicButtons = new HBox(3);

        var backwardButton = new Button("Previous");
        backwardButton.getStyleClass().add("TextField");

        var startPauseStack = new StackPane();

        var startButton = new Button("Start");
        startButton.setVisible(false);
        startButton.getStyleClass().add("TextField");
        startPauseStack.getChildren().add(startButton);

        var pauseButton = new Button("Pause");
        pauseButton.getStyleClass().add("TextField");
        startPauseStack.getChildren().add(pauseButton);

        var forwardButton = new Button("Forward");
        forwardButton.getStyleClass().add("TextField");

        musicButtons.getChildren().add(backwardButton);
        musicButtons.getChildren().add(startPauseStack);
        musicButtons.getChildren().add(forwardButton);

        musicPlayer.getChildren().add(musicButtons);

        pieceBoardVBox.getChildren().add(musicPlayer);

        // musicPlayer event handlers
        Multimedia.playMusic(MenuScene.musicToPlay.get(0));

        startButton.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // Hide start when pause shows, vice versa
                startButton.setVisible(false);
                pauseButton.setVisible(true);
                Multimedia.resumeMusic();
            }
        });

        pauseButton.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                startButton.setVisible(true);
                pauseButton.setVisible(false);
                Multimedia.pauseMusic();
            }
        });

        backwardButton.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // Stop any playing music
                Multimedia.musicCleanUp();
                // Go backwards through music track
                MenuScene.musicIndex--;
                try {
                    Multimedia.playMusic(MenuScene.musicToPlay.get(MenuScene.musicIndex));
                    songNameProperty.set(MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/")[MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/").length - 1]);
                } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
                    // Reset pointer to end of linked list
                    MenuScene.musicIndex = MenuScene.musicToPlay.size() - 1;
                    Multimedia.playMusic(MenuScene.musicToPlay.get(MenuScene.musicIndex));
                    songNameProperty.set(MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/")[MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/").length - 1]);
                }
            }
        });

        forwardButton.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Multimedia.musicCleanUp();
                MenuScene.musicIndex++;
                try {
                    Multimedia.playMusic(MenuScene.musicToPlay.get(MenuScene.musicIndex));
                    songNameProperty.set(MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/")[MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/").length - 1]);
                } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
                    // Reset pointer to start of linked list
                    MenuScene.musicIndex = 0;
                    Multimedia.playMusic(MenuScene.musicToPlay.get(MenuScene.musicIndex));
                    songNameProperty.set(MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/")[MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/").length - 1]);
                }
            }
        });

        // Animate line clearing
        game.addLineClearedListener((coords) -> board.fadeOut(coords));

        // Update animation of timer
        game.setOnGameLoop(this::updateTimer);

        // Display nextPiece on pieceboard
        game.setNextPieceListener((nextPiece) -> nextPieceBoard.displayPiece(nextPiece));

        // Display currentPiece on pieceboard
        game.setCurrentPieceListener((currentPiece) -> currentPieceBoard.displayPiece(currentPiece));

        // Handle block on gameboard grid being clicked
        board.setOnBlockClick(this::blockClicked);

        // Rotate on right click on board
        board.setOnRightClick(this::rotateCounterClockwise);

        //Rotate on left or right click on pieceboard
        currentPieceBoard.setOnLeftClick(this::rotateCounterClockwise);

        // Swap tiles when nextPieceBoard is clicked
        nextPieceBoard.setOnLeftClick(this::swapTiles);
    }

    /**
     * Reads file to obtain the highest score and assigns it property located in Game.
     * @param filename name of file to be read from
     */
    public void getHighScore(String filename) {
        try {
            logger.info("Loading {}", filename);

            BufferedReader reader = new BufferedReader(new FileReader(filename));

            try {
                // Read first line
                String line;
                line = reader.readLine();
                String[] nameScore = line.split(":");

                game.setHiScore(Integer.parseInt(nameScore[1]));
            } catch (IOException exception) {
                logger.error("An error occurred when trying to load scores");
            }  catch(ArrayIndexOutOfBoundsException exception) {
                // File does not exist and cannot be read from, set to generic high score instead
                logger.error("ArrayIndexOutOfBoundsException caught, setting high score to 10000");
                game.setHiScore(10000);
            }
        } catch (FileNotFoundException exception) {
            logger.error("Could not find {}", filename);
            game.setHiScore(10000);
        }
    }

    /**
     * Destroys timeline and creates a new timeline with new duration to animate.
     * @param duration updated Game delay
     */
    protected void updateTimer(int duration) {
        createTimer(duration);
    }

    /**
     * Sets timeline to null and creates new timeline with updated duration. Applies a FillTransition onto timerBar,
     * changing its colours gradually from green to red.
     * @param duration duration to update timeline
     */
    protected void createTimer(int duration) {
        // Destroy old timeline
        if (timeline != null) {
            logger.info("Stopping timer");
            timeline.jumpTo(Duration.ZERO);
            timeline.stop();
            timeline.getKeyFrames().clear();
            // Mark for garbage collection
            timeline = null;
        }

        logger.info("Duration, {}", duration);

        // Create new timeline with updated duration
        timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(new Duration(duration),
                    new KeyValue(timerBar.translateXProperty(), gameWindow.getWidth())));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        // Apply colour transition from green to red
        FillTransition fillTransition = new FillTransition(new Duration(duration), timerBar, Color.GREEN, Color.RED);
        fillTransition.setCycleCount(4);
        fillTransition.setAutoReverse(true);

        // Play animation
        fillTransition.play();
    }

    /**
     * Creates new timerBar object consisting of a rectangle filled green.
     * @return timerBar component
     */
    protected Rectangle createTimerBar() {
        logger.info("Creating new timerBar");

        timerBar = new Rectangle(20, 20, gameWindow.getWidth(), 12);
        timerBar.setFill(Color.GREEN);

        return timerBar;
    }

    // Event handlers
    /**
     * Handle when a block is clicked.
     * @param gameBlock the Game Block that was clocked
     */
    protected void blockClicked(GameBlock gameBlock) {
        game.blockClicked(gameBlock);
    }

    /**
     * Rotates piece clockwise and redisplay result.
     */
    protected void rotateClockwise() {
        logger.info("Rotating piece clockwise");

        Multimedia.playSound("rotate.wav");
        game.rotateCurrentPiece(game.getCurrentPiece());
        currentPieceBoard.displayPiece(game.getCurrentPiece());
    }

    /**
     * Rotates piece counterclockwise and redisplay result.
     */
    protected void rotateCounterClockwise() {
        logger.info("Rotating piece counterclockwise");

        Multimedia.playSound("rotate.wav");
        for (int i = 0; i < 3; i++) {
            game.rotateCurrentPiece(game.getCurrentPiece());
        }
        currentPieceBoard.displayPiece(game.getCurrentPiece());
    }

    /**
     * Swaps pieces on pieceboards with each other.
     */
    protected void swapTiles() {
        logger.info("Swapping tiles");

        Multimedia.playSound("rotate.wav");
        game.swapCurrentPiece(game.getCurrentPiece(), game.getNextPiece());
        currentPieceBoard.displayPiece(game.getCurrentPiece());
        nextPieceBoard.displayPiece(game.getNextPiece());
    }

    /**
     * Binds event handlers onto the scene itself. Event handlers consisting of keyboard support.
     * @param scene scene to bind event handlers to
     */
    protected void eventHandlers(Scene scene) {
        logger.info("Initial aim is, x: {}, y: {}", board.getAimX(), board.getAimY());

        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            logger.info("Pressed, {}", key.getCode());

            // Exit
            if (key.getCode() == KeyCode.ESCAPE) {
                game.resetState();
                gameWindow.cleanup();
                gameWindow.startMenu();
            }

            // Rotations
            if (key.getCode() == KeyCode.Q) {
                rotateClockwise();
            }

            if (key.getCode() == KeyCode.E) {
                rotateCounterClockwise();
            }

            if (key.getCode() == KeyCode.Z) {
                rotateClockwise();
            }

            if (key.getCode() == KeyCode.C) {
                rotateCounterClockwise();
            }

            if (key.getCode() == KeyCode.OPEN_BRACKET) {
                rotateClockwise();
            }

            if (key.getCode() == KeyCode.CLOSE_BRACKET) {
                rotateCounterClockwise();
            }

            // Tile swapping
            if (key.getCode() == KeyCode.SPACE) {
                swapTiles();
            }

            if (key.getCode() == KeyCode.R) {
                swapTiles();
            }

            // Keyboard support to manipulate current aim
            // Translate x
            if (key.getCode() == KeyCode.LEFT || key.getCode() == KeyCode.A) {
                // Prevents ArrayIndexOutOfBounds exception from being thrown
                if (board.getAimX() > 0) {
                    if (board.getBlock(board.getAimX(), board.getAimY()).getValue() == 0) {
                        board.getBlock(board.getAimX(), board.getAimY()).resetPaint();
                    } else {
                        // Reset reestablished colour
                        board.getBlock(board.getAimX(), board.getAimY()).resetEstablishedPaint(board.getBlock(board.getAimX(), board.getAimY()));
                    }
                    board.setAimX(board.getAimX() - 1);
                    board.getBlock(board.getAimX(), board.getAimY()).mouseHoverPaint();
                }
            } else if (key.getCode() == KeyCode.RIGHT || key.getCode() == KeyCode.D) {
                if (board.getAimX() < game.getCols() - 1) {
                    if (board.getBlock(board.getAimX(), board.getAimY()).getValue() == 0) {
                        board.getBlock(board.getAimX(), board.getAimY()).resetPaint();
                    } else {
                        board.getBlock(board.getAimX(), board.getAimY()).resetEstablishedPaint(board.getBlock(board.getAimX(), board.getAimY()));
                    }
                    board.setAimX(board.getAimX() + 1);
                    board.getBlock(board.getAimX(), board.getAimY()).mouseHoverPaint();
                }
                // Translate y
            } else if (key.getCode() == KeyCode.UP || key.getCode() == KeyCode.W) {
                if (board.getAimY() > 0) {
                    if (board.getBlock(board.getAimX(), board.getAimY()).getValue() == 0) {
                        board.getBlock(board.getAimX(), board.getAimY()).resetPaint();
                    } else {
                        board.getBlock(board.getAimX(), board.getAimY()).resetEstablishedPaint(board.getBlock(board.getAimX(), board.getAimY()));
                    }
                    board.setAimY(board.getAimY() - 1);
                    board.getBlock(board.getAimX(), board.getAimY()).mouseHoverPaint();
                }
            } else if (key.getCode() == KeyCode.DOWN || key.getCode() == KeyCode.S) {
                if (board.getAimY() < game.getRows() - 1) {
                    if (board.getBlock(board.getAimX(), board.getAimY()).getValue() == 0) {
                        board.getBlock(board.getAimX(), board.getAimY()).resetPaint();
                    } else {
                        board.getBlock(board.getAimX(), board.getAimY()).resetEstablishedPaint(board.getBlock(board.getAimX(), board.getAimY()));
                    }
                    board.setAimY(board.getAimY() + 1);
                    board.getBlock(board.getAimX(), board.getAimY()).mouseHoverPaint();
                }
            }

            // Place block into grid
            if (key.getCode() == KeyCode.ENTER || key.getCode() == KeyCode.X) {
                game.blockClicked(board.getBlock(board.getAimX(), board.getAimY()));
            }
        });
    }

    /**
     * Set up the game object and model
     */
    public void setupGame() {
        logger.info("Starting a new challenge");

        // Start new game
        game = new Game(5, 5);
    }

    /**
     * Initialise the scene with event handlers that respond to certain key presses and start the game
     */
    @Override
    public void initialise() {
        logger.info("Initialising Challenge");

        logger.info("Initial aim is, x: {}, y: {}", board.getAimX(), board.getAimY());

        eventHandlers(this.scene);

        game.gameOverProperty().addListener((observable -> {
            if (game.gameOverProperty().get()) {
                Platform.runLater(() -> gameWindow.startScore(game));
            }
        }));

        game.start();
    }
}
