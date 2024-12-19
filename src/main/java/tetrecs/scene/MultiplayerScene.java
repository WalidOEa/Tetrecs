package tetrecs.scene;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tetrecs.component.GameBlock;
import tetrecs.component.GameBoard;
import tetrecs.component.LeaderBoard;
import tetrecs.component.PieceBoard;
import tetrecs.game.MultiplayerGame;
import tetrecs.network.Communicator;
import tetrecs.ui.GamePane;
import tetrecs.ui.GameWindow;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * MultiplayerScene is responsible for constructing the UI and handling events pertaining to the multiplayer mode of TetrECS.
 */
public class MultiplayerScene extends ChallengeScene {

    private final Logger logger = LogManager.getLogger(MultiplayerScene.class);

    /**
     * Obtains system time in format of hours:minutes. Used to append to start of message.
     */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Used to send and receive message from server.
     */
    private final Communicator communicator;

    /**
     * Contains the chatroom.
     */
    private final VBox chatRoom = new VBox(5);

    /**
     * Contains messages received by the server.
     */
    private final Text messages = new Text("");

    /**
     * ArrayList containing pairs of name and score.
     */
    private final ArrayList<Pair<String, Integer>> remoteScores = new ArrayList<>();

    /**
     * Observers remoteScores for any changes.
     */
    private final ObservableList<Pair<String, Integer>> remoteScoresObservable = FXCollections.observableArrayList(remoteScores);

    /**
     * Instantiates an observable property with the observer and allows for binding and listeners.
     */
    private final SimpleListProperty<Pair<String, Integer>> remoteScoresObservableProperty = new SimpleListProperty<>(remoteScoresObservable);

    /**
     * Schedules delay used for server polling.
     */
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    /**
     * Reflects username of player from server.
     */
    private final SimpleStringProperty nameProperty = new SimpleStringProperty("");

    /**
     * Displays all competing players and their respective scores.
     */
    private final LeaderBoard leaderBoard = new LeaderBoard(remoteScoresObservable);

    /**
     * Responsible for logic pertaining to the multiplayer game.
     */
    private MultiplayerGame game;

    /**
     * Field to input message to send to server
     */
    private final TextField sendMessage = new TextField();

    /**
     * Create a new multiplayer challenge scene.
     * @param gameWindow the Game Window
     * @param communicator Communicator object to send messages to
     * @param scene scene to bind event handlers to
     */
    public MultiplayerScene(GameWindow gameWindow, Communicator communicator, Scene scene) {
        super(gameWindow);
        logger.info("Creating multiplayer scene");

        this.communicator = communicator;
        this.scene = scene;
    }

    /**
     * Specifies construction of multiplayer UI
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        setupGame();

        communicator.addListener((message) ->
                Platform.runLater(() -> receiveMessage(message))
        );

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        var multiplayerPane = new StackPane();
        multiplayerPane.setMaxWidth(gameWindow.getWidth());
        multiplayerPane.setMaxHeight(gameWindow.getHeight());
        multiplayerPane.getStyleClass().add("multiplayer-background");
        root.getChildren().add(multiplayerPane);

        var mainPane = new BorderPane();
        multiplayerPane.getChildren().add(mainPane);

        // Contains top nodes such as score, lives and text
        var topNodesStack = new StackPane();

        var scoreVBox = new VBox();

        // Score title
        var scoreTitle = new Text("");
        scoreTitle.textProperty().bind(nameProperty);
        scoreTitle.getStyleClass().add("lives");
        scoreVBox.getChildren().add(scoreTitle);

        // Current score
        var score = new Text("");
        score.getStyleClass().add("score");
        score.textProperty().bind(game.scoreProperty().asString());
        scoreVBox.getChildren().add(score);
        scoreVBox.setAlignment(Pos.TOP_LEFT);
        topNodesStack.getChildren().add(scoreVBox);

        var challengeTitle = new Text("Multiplayer Mode");
        challengeTitle.getStyleClass().add("title");
        topNodesStack.getChildren().add(challengeTitle);

        // Contains lives title and current lives
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

        // Leaderboard
        var pieceBoardVBox = new VBox(20);

        var versus = new Text("Versus");
        versus.getStyleClass().add("incoming");

        // Populate leaderboard
        remoteScoresObservableProperty.addListener((ListChangeListener<? super Pair<String, Integer>>) (change) ->
                leaderBoard.build(remoteScoresObservable)
        );

        pieceBoardVBox.getChildren().add(versus);
        pieceBoardVBox.getChildren().add(leaderBoard);

        // Board and pieceboards
        // Pieceboards

        nextPieceBoard = new PieceBoard(3, 3, gameWindow.getWidth()/9.0, gameWindow.getWidth()/9.0);
        currentPieceBoard = new PieceBoard(3, 3, gameWindow.getWidth()/7.0, gameWindow.getWidth()/7.0);

        pieceBoardVBox.getChildren().add(currentPieceBoard);
        pieceBoardVBox.getChildren().add(nextPieceBoard);
        pieceBoardVBox.setAlignment(Pos.CENTER_RIGHT);

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
                Multimedia.musicCleanUp();
                MenuScene.musicIndex--;
                try {
                    Multimedia.playMusic(MenuScene.musicToPlay.get(MenuScene.musicIndex));
                    songNameProperty.set(MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/")[MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/").length - 1]);
                } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
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
                    MenuScene.musicIndex = 0;
                    Multimedia.playMusic(MenuScene.musicToPlay.get(MenuScene.musicIndex));
                    songNameProperty.set(MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/")[MenuScene.musicToPlay.get(MenuScene.musicIndex).split("/").length - 1]);
                }
            }
        });


        // Gameboard
        var boardVBox = new VBox(15);

        board = new GameBoard(game.getGrid(), gameWindow.getWidth()/2.3, gameWindow.getWidth()/2.3);
        boardVBox.getChildren().add(board);
        boardVBox.setAlignment(Pos.CENTER_LEFT);

        // Add nodes to same horizontal box
        var boardsHBox = new HBox(23);

        boardsHBox.getChildren().add(boardVBox);
        boardsHBox.getChildren().add(pieceBoardVBox);
        boardsHBox.setAlignment(Pos.CENTER);

        mainPane.setCenter(boardsHBox);

        // Chat
        messages.getStyleClass().add("channelItem");
        messages.setText("Press T to open chat");
        messages.setWrappingWidth(250);

        sendMessage.setPrefWidth(gameWindow.getWidth()/2.0);
        sendMessage.getStyleClass().add("TextField");
        sendMessage.setPromptText("Press enter to send message");
        sendMessage.setVisible(false);

        chatRoom.getChildren().add(messages);
        chatRoom.getChildren().add(sendMessage);
        chatRoom.setAlignment(Pos.CENTER_LEFT);

        boardVBox.getChildren().add(chatRoom);

        // Timer
        createTimer(game.getDelay());

        mainPane.setBottom(timerBar);

        // Event handlers
        game.addLineClearedListener((coords) -> board.fadeOut(coords));

        game.setCurrentPieceListener((currentPiece) -> currentPieceBoard.displayPiece(currentPiece));

        game.setNextPieceListener((nextPiece) -> nextPieceBoard.displayPiece(nextPiece));

        board.setOnBlockClick(this::blockClicked);

        game.setOnGameLoop(this::updateTimer);

        board.setOnRightClick(this::rotateCounterClockwise);

        currentPieceBoard.setOnLeftClick(this::rotateCounterClockwise);

        nextPieceBoard.setOnLeftClick(this::swapTiles);

        /*
        // Chat room event handler
        sendMessage.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Do not send empty message
                if (sendMessage.getText().equals("")) {
                    sendMessage.setVisible(false);
                } else {
                    Multimedia.playSound("message.wav");
                    communicator.send("MSG " + sendMessage.getText());
                    sendMessage.clear();
                    sendMessage.setVisible(false);
                }
            }
        });

         */
    }

    /**
     * Initialise schedules that involve polling the server for information as well as binding any event handlers
     * to the scene.
     */
    @Override
    public void initialise() {
        logger.info("Initialising multiplayer");

        logger.info("Initial aim is, x: {}, y: {}", board.getAimX(), board.getAimY());

        this.scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> sendMessage.setVisible(key.getCode() == KeyCode.T));

        Runnable runnable = () -> {
            communicator.send("SCORES");
            communicator.send("BOARD");
            communicator.send("SCORE " + game.getScore());
        };

        game.gameOverProperty().addListener((observable -> {
            if (game.gameOverProperty().get()) {
                logger.info("Game over");
                scheduledExecutorService.shutdown();
                gameWindow.cleanup();
                Platform.runLater(() -> gameWindow.startScore(game));
            }
        }));

        scheduledExecutorService.scheduleAtFixedRate(runnable, 0, 1000, TimeUnit.MILLISECONDS);

        logger.info("Initial aim is, x: {}, y: {}", board.getAimX(), board.getAimY());

        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            logger.info("Pressed, {}", key.getCode());

            // Exit
            if (key.getCode() == KeyCode.ESCAPE) {
                game.resetState();
                scheduledExecutorService.shutdown();
                communicator.send("PART");
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

        game.start();
    }

    /**
     * Set up the game object and model
     */
    public void setupGame() {
        logger.info("Starting a new multiplayer");

        // Start new game
        game = new MultiplayerGame(5, 5, communicator);
    }

    /**
     * Handles a block clicked on the gameboard
     * @param gameBlock the Game Block that was clocked
     */
    @Override
    protected void blockClicked(GameBlock gameBlock) {
        game.blockClicked(gameBlock);
    }

    /**
     * Rotates piece clockwise.
     */
    @Override
    protected void rotateClockwise() {
        Multimedia.playSound("rotate.wav");
        for (int i = 0; i < 3; i++) {
            game.rotateCurrentPiece(game.getCurrentPiece());
        }
        currentPieceBoard.displayPiece(game.getCurrentPiece());
    }

    /**
     * Rotates piece counterclockwise.
     */
    @Override
    protected void rotateCounterClockwise() {
        Multimedia.playSound("rotate.wav");
        game.rotateCurrentPiece(game.getCurrentPiece());
        currentPieceBoard.displayPiece(game.getCurrentPiece());
    }

    /**
     * Swaps piece tiles
     */
    @Override
    protected void swapTiles() {
        Multimedia.playSound("transition.wav");
        game.swapCurrentPiece(game.getCurrentPiece(), game.getNextPiece());
        currentPieceBoard.displayPiece(game.getCurrentPiece());
        nextPieceBoard.displayPiece(game.getNextPiece());
    }

    public void receiveMessage(String message) {
        if (message.startsWith("MSG")) {
            String[] components = message.replace("MSG ", "").split(":");
            String username = components[0];
            String userMessage = components[1];

            LocalTime now = LocalTime.now();

            String receivedMessage = (formatter.format(now) + " " + username + ": " + userMessage + "\n");

            messages.setText(receivedMessage);

            Multimedia.playSound("message.wav");

        }
        if (message.startsWith("SCORES")) {
            logger.info("SCORES");
            String[] components = message.replace("SCORES ", "").split("\n");

            for (String parts : components) {
                String username = parts.split(":")[0];
                String score = parts.split(":")[1];
                String lives = parts.split(":")[2];

                // Update leaderboard when any player gets new score
                if (!remoteScoresObservableProperty.contains(new Pair<>(username, Integer.parseInt(score)))) {
                    for (Pair<String, Integer> pair : remoteScoresObservableProperty) {
                        if (pair.getKey().equals(username)) {
                            remoteScoresObservableProperty.remove(pair);
                            remoteScoresObservableProperty.add(new Pair<>(username, Integer.parseInt(score)));
                            return;
                        }
                    }
                    remoteScoresObservableProperty.add(new Pair<>(username, Integer.parseInt(score)));
                }

                if (lives.equals("DEAD")) {
                    // Rebuild leaderboard reflecting eliminated players

                    leaderBoard.build(remoteScoresObservable, username, score);
                }
            }
        }
        else if (message.startsWith("SCORE")) {
            String[] components = message.replace("SCORE ", "").split(":");
            String username = components[0];

            nameProperty.set(username);
        }

        if (message.startsWith("ERROR")) {
            logger.error("An error occurred, {}", message.replace("ERROR ", ""));

            Alert alertError = new Alert(Alert.AlertType.ERROR);
            alertError.setContentText("An error has occurred!\n\n" + message.replace("ERROR ", ""));

            alertError.show();
        }
    }
}
