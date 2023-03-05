package uk.ac.soton.comp1206.scene;

import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.component.ScoresList;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Contains local scores received from the final Game state and animates the reveal.
 */
public class ScoresScene extends BaseScene{

    private static final Logger logger = LogManager.getLogger(ScoresScene.class);

    /**
     * Obtains information from final state of Game.
     */
    private final Game game;

    /**
     * Holds pairs consisting of name and score.
     */
    private final ArrayList<Pair<String, Integer>> scorePairs = new ArrayList<>();

    /**
     * Observes arraylist scorePairs.
     */
    private final ObservableList<Pair<String, Integer>> scorePairsObservable = FXCollections.observableArrayList(scorePairs);

    /**
     * Expose observable list scorePairsObservable as property, allows for binding and addition of listeners.
     */
    private final ListProperty<Pair<String, Integer>> scoresPairsObservableProperty = new SimpleListProperty<>(scorePairsObservable);

    /**
     * Holds online names and scores.
     */
    private final ArrayList<Pair<String, Integer>> remoteScores = new ArrayList<>();

    /**
     * Observes arrayList remoteScores.
     */
    private final ObservableList<Pair<String, Integer>> remoteScoresObservable = FXCollections.observableArrayList(remoteScores);

    /**
     * Exposes observable list remoteScoresObservable as property, allowing for binding and event listeners.
     */
    private final SimpleListProperty<Pair<String, Integer>> remoteScoresObservableProperty = new SimpleListProperty<>(remoteScoresObservable);

    /**
     * Textfield prompting user to input their name if they beat any local scores.
     */
    private TextField nameTextField;

    /**
     * File writer object to write new scores into local scores
     */
    private BufferedWriter writer;

    /**
     * Allows for dynamic binding to change display of name on score scene
     */
    private final SimpleStringProperty nameProperty = new SimpleStringProperty("Guest");

    /**
     * Contains button and textfield prompting user for name
     */
    private final HBox nameHBox = new HBox(15);

    /**
     * Sends and receives messages to server
     */
    private final Communicator communicator;

    /**
     * Contains local scores
     */
    private Text localScores = new Text();

    /**
     * Declared as a field variable to allow for use outside its usual method of build
     */
    private BorderPane mainPane = new BorderPane();

    /**
     * Constructs the score scene
     * @param gameWindow game window to display in
     */
    public ScoresScene(GameWindow gameWindow, Game game, Communicator communicator) {
        super(gameWindow);
        this.game = game;
        this.communicator = communicator;

        logger.info("Creating new score scene");
    }

    /**
     * Builds the score scene
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        //communicator.addListener((message) -> Platform.runLater(() -> this.receiveMessage(message)));

        var scorePane = new StackPane();
        scorePane.setMaxWidth(gameWindow.getWidth());
        scorePane.setMaxHeight(gameWindow.getHeight());
        scorePane.getStyleClass().add("menu-background");
        root.getChildren().add(scorePane);

        mainPane = new BorderPane();
        scorePane.getChildren().add(mainPane);

        // Game over text
        var gameOverText = new Text("Game Over!");
        gameOverText.getStyleClass().add("gameover");
        var rotateTransition = new RotateTransition(new Duration(1500), gameOverText);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setFromAngle(-4);
        rotateTransition.setToAngle(4);
        rotateTransition.setAutoReverse(true);
        rotateTransition.play();
        var gameOverWrapper = new StackPane();
        gameOverWrapper.getChildren().add(gameOverText);
        gameOverWrapper.setAlignment(Pos.BOTTOM_CENTER);

        var textVBox = new VBox(5);

        textVBox.getChildren().add(gameOverWrapper);

        // Name prompt textfield
        nameTextField = new TextField();
        nameTextField.setPromptText("Enter your name");
        nameTextField.setMaxWidth(gameWindow.getWidth()/2.0);
        nameTextField.getStyleClass().add("TextField");

        // Text field event handler
        nameTextField.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                int index;
                nameProperty.set(nameTextField.getText());
                nameTextField.clear();
                index = scoresPairsObservableProperty.indexOf(new Pair<>("Guest", game.getScore()));
                scoresPairsObservableProperty.remove(index);
                scoresPairsObservableProperty.add(index, new Pair<>(nameProperty.get(), game.getScore()));
                nameHBox.setVisible(false);
                writeScores("scores.txt");
            }
        });

        // Submit button
        var submitButton = new Button("Submit");
        submitButton.getStyleClass().add("TextField");
        submitButton.setOnAction(event -> {
            int index;
            nameProperty.set(nameTextField.getText());
            nameTextField.clear();
            index = scoresPairsObservableProperty.indexOf(new Pair<>("Guest", game.getScore()));
            scoresPairsObservableProperty.remove(index);
            scoresPairsObservableProperty.add(index, new Pair<>(nameProperty.get(), game.getScore()));
            nameHBox.setVisible(false);
            writeScores("scores.txt");
        });

        nameHBox.getChildren().add(nameTextField);
        nameHBox.getChildren().add(submitButton);
        nameHBox.setVisible(false);
        nameHBox.setAlignment(Pos.TOP_CENTER);

        var nodeStack = new StackPane();

        textVBox.setAlignment(Pos.BOTTOM_CENTER);
        nodeStack.getChildren().add(textVBox);

        mainPane.setTop(nodeStack);
        mainPane.setBottom(nameHBox);

        // Local scores
        var scoresList = new ScoresList(scorePairsObservable);

        scoresPairsObservableProperty.addListener((ListChangeListener<? super Pair<String, Integer>>) (change) ->
                        scoresList.build(scorePairsObservable)
        );

        var localScoresVBox = new VBox(8);
        localScores = new Text("Local scores: ");
        localScores.getStyleClass().add("heading");

        localScoresVBox.getChildren().add(localScores);
        localScoresVBox.getChildren().add(scoresList);
        localScoresVBox.setAlignment(Pos.BOTTOM_LEFT);
        BorderPane.setMargin(localScoresVBox, new Insets(30, 30, 30, 30));
        mainPane.setLeft(localScoresVBox);

        /*
        // Online scores
        var onlineScoresList = new ScoresList(remoteScoresObservable);

        remoteScoresObservableProperty.addListener((ListChangeListener<? super Pair<String, Integer>>) (change) ->
                        onlineScoresList.build(remoteScoresObservable)
        );

        var onlineScoresVBox = new VBox(8);
        var onlineScores = new Text("Online scores: ");
        onlineScores.getStyleClass().add("heading");

        onlineScoresVBox.getChildren().add(onlineScores);
        onlineScoresVBox.getChildren().add(onlineScoresList);
        onlineScoresVBox.setAlignment(Pos.BOTTOM_RIGHT);
        BorderPane.setMargin(onlineScoresVBox, new Insets(30, 30, 30, 30));
        mainPane.setRight(onlineScoresVBox);

         */
    }

    /**
     * Loads scores from a given text file. If the file does not exist then create a new one and write generated scores
     * into it.
     * @param filename Name of file to be read from
     */
    public void loadScores(String filename) {
        String line;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));

            try {
                // Reads from file and populates UI component
                while ((line = reader.readLine()) != null) {
                    String[] components = line.split(":");
                    String name = components[0];
                    String score = components[1];

                    if (!scoresPairsObservableProperty.contains(new Pair<>(name, Integer.parseInt(score)))) {
                        scoresPairsObservableProperty.add(new Pair<>(name, Integer.parseInt(score)));
                    }
                }

                // Write scores in text file
                writeScores("scores.txt");
            } catch(IOException ioException) {
                ioException.printStackTrace();
            }

        } catch(FileNotFoundException fileNotFoundException) {
            logger.error("File not found");

            // Create new file
            createScores("scores.txt");

            // Read newly created file
            loadScores("scores.txt");
        }
    }

    /**
     * Write local score into file.
     * @param filename Name of file to be written into
     */
    public void writeScores(String filename) {
        try {
            writer = new BufferedWriter(new FileWriter(filename));

            for (Pair<String, Integer> pair : scoresPairsObservableProperty) {
                String name = pair.getKey();
                String score = pair.getValue().toString();
                String nameScore = name + ":" + score + "\n";
                writer.write(nameScore);
            }

            writer.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * If file does not exist, create a new file and populate with.
     * @param filename name of file to create
     */
    public void createScores(String filename) {
        logger.info("Creating new file");

        try {
            writer = new BufferedWriter(new FileWriter(filename));

            for (int i = 10; i > 0; i--) {
                writer.write("Player" + (11 - i) + ":" + i * 1000 + "\n");
            }
            writer.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /*
    /**
     * Handle receive messages from server.
     * @param message message to process
     */

    /*
    private void receiveMessage(String message) {
        if (message.startsWith("HISCORES")) {
            String[] components = message.replace("HISCORES ", "").split("\n");

            for (String parts : components) {
                remoteScoresObservableProperty.add(new Pair<>(parts.split(":")[0], Integer.parseInt(parts.split(":")[1])));
            }

        } if (message.startsWith("NEWSCORE")) {
            // Player achieved new online high score
            logger.info("Online scores");
            remoteScoresObservableProperty.clear();

            loadOnlineScores();

        } if (message.startsWith("SCORES")) {
            String[] components = message.replace("SCORES ", "").split("\n");
            Random rand = new Random();

            // Delete local scores and show scores from multiplayer match
            scoresPairsObservableProperty.clear();
            try {
                localScores.setText("");
                localScores = null;
            } catch (NullPointerException ignored) {

            }

            var multiplayerMatch = new VBox();

            var multiplayerText = new Text("Multiplayer score: ");
            multiplayerText.getStyleClass().add("heading");

            multiplayerMatch.getChildren().add(multiplayerText);

            // Add scores to multiplayer leaderboard
            for (String pair : components) {
                String[] parts = pair.split(":");
                String name = parts[0];
                String score = parts[1];

                var nameScore = new Text(name + ": " + score);
                nameScore.getStyleClass().add("scorelist");
                nameScore.setFill(GameBlock.COLOURS[rand.nextInt(GameBlock.COLOURS.length)]);
                multiplayerMatch.getChildren().add(nameScore);
            }

            multiplayerMatch.setAlignment(Pos.CENTER_LEFT);
            BorderPane.setMargin(multiplayerMatch, new Insets(30, 30, 30, 30));
            mainPane.setLeft(multiplayerMatch);

        }
    }

     */


    /**
     * Sends message to server and receives online scores.
     */
    public void loadOnlineScores() {
        //communicator.send("HISCORES");
    }

    /**
     * Sends message to server and updates remote scores with new score.
     */
    public void writeOnlineScores() {
        //communicator.send("HISCORE " + nameProperty.get() + ":" + game.getScore());
    }

    /**
     * Initialises an event handler to handle an escape keystroke. After the keystroke, the scene is cleaned of any
     * listeners and the Menu scene is initialised instead.
     */
    @Override
    public void initialise() {
        Pair<String, Integer> removePair = null;
        int index = 0;
        boolean flag = false;

        logger.info("Initialising Scores");

        loadScores("scores.txt");
        loadOnlineScores();

        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if (key.getCode() == KeyCode.ESCAPE) {
                game.resetState();
                gameWindow.cleanup();
                gameWindow.startMenu();
            }
        });

        // Check user score beat any local scores
        for (Pair<String, Integer> pair : scoresPairsObservableProperty) {
            if (game.getScore() >= pair.getValue()) {
                nameHBox.setVisible(true);
                removePair = pair;
                index = scoresPairsObservableProperty.indexOf(pair);
                flag = true;
                break;
            }
        }

        /*
        // Write new score into online scores once
        for (Pair<String, Integer> pair : remoteScoresObservableProperty) {
            if (game.getScore() >= pair.getValue()) {
                writeOnlineScores();
                break;
            }
        }

         */

        // Remove outdated pair and replace with new pair in local scores
        if (flag) {
            scoresPairsObservableProperty.remove(removePair);
            scoresPairsObservableProperty.add(index, new Pair<>(nameProperty.get(), game.getScore()));
        }
        writeScores("scores.txt");

        Multimedia.playBackgroundMusic("end.wav");

        // Leave the channel
        //communicator.send("DIE");
    }
}
