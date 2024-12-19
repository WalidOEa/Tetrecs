package tetrecs.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tetrecs.event.CommunicationsListener;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import tetrecs.ui.GameWindow;

/**
 * Uses web sockets to talk to a web socket server and relays communication to attached listeners
 */
public class Communicator extends WebSocketClient {

    private static final Logger logger = LogManager.getLogger(Communicator.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Attached communication listeners listening to messages on this Communicator. Each will be sent any messages.
     */
    private final List<CommunicationsListener> handlers = new ArrayList<>();

    /**
     * Create a new communicator to the given web socket server
     * @param serverURI server to connect to
     * @param draft
     */
    public Communicator(URI serverURI, Draft draft) {
        super(serverURI, draft);
    }

    /**
     * Create a new communicator to the given web socket server
     * @param serverURI server to connect to
     */
    public Communicator(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("New connection opened to " + GameWindow.getServerURI());

        scheduler.scheduleAtFixedRate(() -> {
            if (this.isOpen()) {
                sendMessage("Marco");
            } else {
                logger.warn("Connection closed, terminating keep-alive");

                scheduler.shutdown();
            }
        }, 0, 2, TimeUnit.MINUTES);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("Closed with Exit Code: " + code + ", Reason: " + reason);
    }

    @Override
    public void onMessage(String message) {
        logger.info("Message received -> " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        logger.info("Message received -> " + message);
    }

    @Override
    public void onError(Exception e) {
        logger.error("An error has occurred: " + e.getMessage());
    }

    public void sendMessage(String message) {
        logger.info("Sending message -> " + message);

        this.send(message);
    }

    /**
     * Web socket server to connect to and receive and send messages to.
     */
    /*
    private WebSocket ws = null;

    /*

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

    /** Receive a message from the server. Relay to any attached listeners
     * @param message the message that was received
     */
    private void receive(String message) {
        logger.info("Received, {}", message);

        for(CommunicationsListener handler : handlers) {
            handler.receiveCommunication(message);
        }
    }
}
