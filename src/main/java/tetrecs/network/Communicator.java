package tetrecs.network;

import javafx.scene.control.Alert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tetrecs.event.CommunicationsListener;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Uses web sockets to talk to a web socket server and relays communication to attached listeners
 */
public class Communicator extends WebSocketClient {

    private static final Logger logger = LogManager.getLogger(Communicator.class);

    /**
     * Attached communication listeners listening to messages on this Communicator. Each will be sent any messages.
     */
    private final List<CommunicationsListener> handlers = new ArrayList<>();

    public Communicator(URI serverURI, Draft draft) {
        super(serverURI, draft);
    }

    public Communicator(URI serverURI) {
        super(serverURI);
    }


    @Override
    public void onOpen(ServerHandshake handshakedata) {
        send("Hello, it is me. Mario :)");
        System.out.println("new connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("received message: " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }







    /*

    /**
     * Web socket server to connect to and receive and send messages to.
     */
    /*
    private WebSocket ws = null;

    /**
     * Create a new communicator to the given web socket server
     * @param server server to connect to
     */
    /*
    public Communicator(String server) {

        try {
            var socketFactory = new WebSocketFactory();

            // Connect to the server
            ws = socketFactory.createSocket(server);
            ws.connect();
            logger.info("Connected to " + server);

            /*
            // When a message is received, call the receive method
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    Communicator.this.receive(websocket, message);
                }
                @Override
                public void onPingFrame(WebSocket webSocket, WebSocketFrame webSocketFrame) throws Exception {
                    logger.info("Ping? Pong!");
                }
            });

            // Error handling
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    if(message.startsWith("ERROR")) {
                        logger.error(message);
                    }
                }
                @Override
                public void handleCallbackError(WebSocket webSocket, Throwable throwable) throws Exception {
                    logger.error("Callback Error:" + throwable.getMessage());
                    throwable.printStackTrace();
                }
                @Override
                public void onError(WebSocket webSocket, WebSocketException e) throws Exception {
                    logger.error("Error:" + e.getMessage());
                    e.printStackTrace();
                }
            });

        } catch (Exception e){
            logger.error("Socket error: " + e.getMessage());
            e.printStackTrace();

            Alert error = new Alert(Alert.AlertType.ERROR,"Unable to communicate with the TetrECS server\n\n" + e.getMessage() + "\n\nPlease ensure you are connected to the VPN");
            error.showAndWait();
            System.exit(1);
        }
    }


    /**
     * Send a message to the server
     * @param message Message to send
     */
    /*
    public void send(String message) {
        logger.info("Sending message: " + message);

        ws.sendText(message);
    }


    /**
     * Add a new listener to receive messages from the server
     * @param listener the listener to add
     */
    public void addListener(CommunicationsListener listener) {
        this.handlers.add(listener);
    }

    /**
     * Clear all current listeners
     */
    public void clearListeners() {
        this.handlers.clear();
    }

    /*
    /** Receive a message from the server. Relay to any attached listeners
     *
     * @param websocket the socket
     * @param message the message that was received
     */
    /*
    private void receive(WebSocket websocket, String message) {
        logger.info("Received, {}", message);

        for(CommunicationsListener handler : handlers) {
            handler.receiveCommunication(message);
        }
    }

    */

}