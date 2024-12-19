package tetrecs.scene;

import javafx.application.Platform;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tetrecs.component.LobbyList;
import tetrecs.network.Communicator;
import tetrecs.ui.GamePane;
import tetrecs.ui.GameWindow;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for managing and creating lobbies in the server as well as displaying all possible games.
 */
public class LobbyScene extends BaseScene {

    private final Logger logger = LogManager.getLogger(LobbyScene.class);

    /**
     * Allows for receiving and sending of messages to web socket server.
     */
    private final Communicator communicator;

    /**
     * Formats system time as: Hour:minute.
     */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Map containing keys of string representing lobby name and values representing event handlers.
     */
    private final Map<String, EventHandler<MouseEvent>> lobbies = new HashMap<>();

    /**
     * Observers changes to lobbies.
     */
    private final ObservableMap<String, EventHandler<MouseEvent>> lobbiesObservable = FXCollections.observableMap(lobbies);

    /**
     * Represents lobbiesObservable as a property allowing for binding and listeners to be assigned.
     */
    private final SimpleMapProperty<String, EventHandler<MouseEvent>> lobbiesObservableProperty = new SimpleMapProperty<>(lobbiesObservable);

    /**
     * Updates UI with new lobbies.
     */
    private final SimpleStringProperty channelTitleProperty = new SimpleStringProperty("");

    /**
     * Updates lobby with current users.
     */
    private final SimpleStringProperty channelUsersProperty = new SimpleStringProperty("");

    /**
     * Name of lobby.
     */
    private final Text channelTitle = new Text("");

    /**
     * Current users of lobby.
     */
    private final Text channelUsers = new Text("");

    /**
     * Holds all nodes pertaining to displaying lobby.
     */
    private final StackPane chatRoomStack = new StackPane();

    /**
     * Holds all user messages received from server.
     */
    private final TextFlow messages = new TextFlow();

    /**
     * Starts game in channel.
     */
    private final Button startGame = new Button("Start game");

    /**
     * Allows for scrolling of messages.
     */
    private final ScrollPane scroller = new ScrollPane();

    /**
     * Determines whether the bottom of messages has been reached.
     */
    private Boolean scrollToBottom;

    /**
     * Schedules new tasks such as polling the server for current lobbies.
     */
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    /**
     * Create a new lobby scene.
     * @param gameWindow the GameWindow this will be displayed in
     * @param communicator the communicator to send and receive messages from
     * @param scene the current Scene
     */
    public LobbyScene(GameWindow gameWindow, Communicator communicator, Scene scene) {
        super(gameWindow);
        this.communicator = communicator;
        this.scene = scene;

        logger.info("Creating Lobby Scene");
    }

    /**
     * Builds the UI layout and binds any event handlers.
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        root = new GamePane(gameWindow.getWidth(), gameWindow.getHeight());

        var lobbyPane = new BorderPane();
        lobbyPane.setMaxWidth(gameWindow.getWidth());
        lobbyPane.setMaxHeight(gameWindow.getHeight());
        lobbyPane.getStyleClass().add("menu-background");
        root.getChildren().add(lobbyPane);

        communicator.addListener((message) -> Platform.runLater(() -> receiveMessage(message)));

        var wrapper = new StackPane();

        var multiplayerTitle = new Text("Multiplayer");
        multiplayerTitle.getStyleClass().add("title");
        wrapper.getChildren().add(multiplayerTitle);
        wrapper.setAlignment(Pos.CENTER);
        lobbyPane.setTop(wrapper);

        // Lobby list
        var lobbies = new VBox(5);

        var lobbyList = new LobbyList(lobbiesObservable);

        var createChannel = new Text("Host new game");
        createChannel.getStyleClass().add("channelItem");

        var channelField = new TextField();
        channelField.setPromptText("Enter lobby name");
        channelField.getStyleClass().add("TextField");
        channelField.setMaxWidth(gameWindow.getWidth()/2.0);
        channelField.setVisible(false);

        var title = new Text("Current games: ");
        title.getStyleClass().add("channelItem");

        lobbiesObservableProperty.addListener((MapChangeListener<? super String, ? super EventHandler<MouseEvent>>) (change) -> lobbyList.build(lobbiesObservable));

        // Event handlers
        createChannel.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                channelField.setVisible(true);
                Multimedia.playSound("pling.wav");
            }
        });

        channelField.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                channelField.setVisible(false);
                communicator.send("CREATE " + channelField.getText());
                startGame.setVisible(true);
                channelField.clear();
                Multimedia.playSound("pling.wav");
            }
        });

        lobbies.getChildren().add(createChannel);
        lobbies.getChildren().add(channelField);
        lobbies.getChildren().add(title);
        lobbies.getChildren().add(lobbyList);
        BorderPane.setMargin(lobbies, new Insets(25, 25, 25, 25));
        lobbyPane.setLeft(lobbies);

        // Chat room
        var chatRoom = new Rectangle(gameWindow.getWidth()/1.9, gameWindow.getWidth()/1.6, Color.BLACK);
        chatRoom.setOpacity(0.7);

        var chatRoomVBox = new VBox();
        channelTitle.textProperty().bind(channelTitleProperty);
        channelTitle.getStyleClass().add("heading");

        channelUsers.textProperty().bind(channelUsersProperty);
        channelUsers.getStyleClass().add("channelItem");

        Text infoText = new Text("Type /nick <name> to change your name\n\n\n");
        infoText.getStyleClass().add("messages");

        messages.setPrefHeight(gameWindow.getWidth()/2.0);
        messages.setPrefWidth(chatRoom.getWidth());
        messages.getStyleClass().add("messages");
        messages.getChildren().add(infoText);
        messages.getStyleClass().add("channelItem");

        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("scroller");
        scroller.setContent(messages);

        var messageBox = new HBox();

        var sendMessage = new TextField();
        sendMessage.getStyleClass().add("TextField");
        sendMessage.setPromptText("Message " + channelTitleProperty.get());

        startGame.setVisible(false);

        var sendMessageButton = new Button("Send");
        sendMessageButton.getStyleClass().add("TextField");

        HBox.setHgrow(sendMessage, Priority.ALWAYS);
        messageBox.getChildren().add(sendMessage);
        messageBox.getChildren().add(sendMessageButton);

        var buttonsStack = new StackPane();

        startGame.getStyleClass().add("TextField");
        StackPane.setAlignment(startGame, Pos.BOTTOM_LEFT);
        buttonsStack.getChildren().add(startGame);

        var leaveLobby = new Button("Leave lobby");
        leaveLobby.getStyleClass().add("TextField");
        StackPane.setAlignment(leaveLobby, Pos.BOTTOM_RIGHT);
        buttonsStack.getChildren().add(leaveLobby);

        scene.addPostLayoutPulseListener(this::jumpToBottom);

        chatRoomVBox.getChildren().add(chatRoom);
        chatRoomVBox.getChildren().add(channelTitle);
        chatRoomVBox.getChildren().add(channelUsers);
        chatRoomVBox.getChildren().add(messages);
        chatRoomVBox.getChildren().add(scroller);
        chatRoomVBox.getChildren().add(messageBox);
        chatRoomVBox.getChildren().add(buttonsStack);

        chatRoomStack.getChildren().add(chatRoom);
        chatRoomStack.getChildren().add(chatRoomVBox);
        chatRoomStack.setVisible(false);

        BorderPane.setMargin(chatRoomStack, new Insets(25, 25, 25, 25));
        lobbyPane.setRight(chatRoomStack);

        // Chat room event handlers
        sendMessage.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (sendMessage.getText().equals("")) {
                    return;
                }
                if (sendMessage.getText().contains("/nick")) {
                    communicator.send("NICK " + sendMessage.getText().replace("/nick ", ""));
                    sendMessage.clear();
                    Multimedia.playSound("message.wav");
                } else {
                    communicator.send("MSG " + sendMessage.getText());
                    sendMessage.clear();
                    Multimedia.playSound("message.wav");
                }
            }
        });

        startGame.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                communicator.send("START");
                scheduledExecutorService.shutdown();
                gameWindow.cleanup();
                gameWindow.startMultiplayer();
                Multimedia.playSound("pling.wav");
            }
        });

        leaveLobby.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                communicator.send("PART");
                chatRoomStack.setVisible(false);
                startGame.setVisible(false);
                Multimedia.playSound("pling.wav");
            }
        });

        sendMessageButton.setOnAction(event -> {
            if (sendMessage.getText().equals("")) {
                return;
            }
            if (sendMessage.getText().equals("/nick")) {
                communicator.send("NICK " + sendMessage.getText().replace("/nick ", ""));
                sendMessage.clear();
                Multimedia.playSound("message.wav");
            } else {
                communicator.send("MSG " + sendMessage.getText());
                sendMessage.clear();
                Multimedia.playSound("message.wav");
            }
        });
    }

    /**
     * Initialise the scene with event handlers that respond to certain key presses and start the game. A scheduler is
     * tasked with polling the server every 2 seconds for new lobbies to display.
     */
    @Override
    public void initialise() {
        Runnable runnable = () -> communicator.send("LIST");

        scheduledExecutorService.scheduleAtFixedRate(runnable, 0, 2000, TimeUnit.MILLISECONDS);

        //TODO: Send PART message to server when escape is pressed
        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if (key.getCode() == KeyCode.ESCAPE) {
                // Prevent sending of messages when leaving scene
                scheduledExecutorService.shutdown();
                gameWindow.cleanup();
                gameWindow.startMenu();
            }
        });

        // Play music
        Multimedia.playBackgroundMusic("menu.mp3");
    }

    /**
     * Handles received messages from communicator.
     * @param message message received from server
     */
    private void receiveMessage(String message) {
        // Receive list of channels currently available
        if (message.startsWith("CHANNELS")) {
            String[] components = message.split("\n");
            String channel = "";

            for (String parts : components) {
                if (parts.contains("CHANNELS")) {
                    parts = parts.replace("CHANNELS ", "");
                }

                // Add to component for display and do not duplicate channel names
                if (!lobbiesObservableProperty.containsKey(parts)) {
                    String finalParts = parts;
                    lobbiesObservableProperty.put(parts, mouseEvent -> joinChannel(mouseEvent, finalParts));
                }

                for (String channels : lobbiesObservableProperty.keySet()) {
                    if (!message.contains(channels)) {
                        channel = channels;
                    }
                }

                lobbiesObservableProperty.remove(channel);
            }
        }

        if (message.startsWith("JOIN")) {
            // Send join message to server to join a server
            chatRoomStack.setVisible(true);
            channelTitleProperty.set(message.replace("JOIN ", ""));

            communicator.send("USERS");
        }

        if (message.startsWith("NICK")) {
            logger.info(message);
        }

        if (message.startsWith("MSG")) {
            // Format and display user message received from server in chatroom
            String[] components = message.replace("MSG ", "").split(":");
            String username = components[0];
            String userMessage = components[1];

            LocalTime now = LocalTime.now();

            Text receivedMessage = new Text(formatter.format(now) + " " + username + ": " + userMessage + "\n");

            messages.getChildren().add(receivedMessage);

            // Scroll to bottom
            if (scroller.getVvalue() == 0.0f || scroller.getVvalue() > 0.9f) {
                scrollToBottom = true;
            }
        }

        if (message.startsWith("USERS")) {
            logger.info(message);
            String[] components = message.replace("USERS ", "").split("\n");
            channelUsersProperty.set("");

            // Display all current users in channel
            for (String username : components) {
                channelUsersProperty.set(channelUsersProperty.get() + username + " ");
            }
        }

        if (message.startsWith("HOST")) {
            // Allow user to start game by displaying button
            startGame.setVisible(true);
        }

        if (message.startsWith("START")) {
            // Start multiplayer game and shutdown scheduler to prevent further polling
            communicator.send("START");
            scheduledExecutorService.shutdown();
            gameWindow.cleanup();
            gameWindow.startMultiplayer();
        }

        if (message.startsWith("ERROR")) {
            // Display error message in alert box
            logger.error("An error occurred, {}", message.replace("ERROR ", ""));

            Alert alertError = new Alert(Alert.AlertType.ERROR);
            alertError.setContentText("An error has occurred!\n\n" + message.replace("ERROR ", ""));

            alertError.show();
        }
    }

    /**
     * Binds event to text in UI. Clicking channel name in UI sends a JOIN message to server.
     * @param mouseEvent Ensures button is left mouse button
     * @param channel Name of channel to join
     */
    private void joinChannel(MouseEvent mouseEvent, String channel) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            communicator.send("JOIN " + channel);
        }
    }

    /**
     * Move the scroller to the bottom.
     */
    private void jumpToBottom() {
        if(!scrollToBottom) return;
        scroller.setVvalue(1.0f);
        scrollToBottom = false;
    }
}
