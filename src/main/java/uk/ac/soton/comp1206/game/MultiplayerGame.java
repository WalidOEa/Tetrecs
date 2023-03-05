package uk.ac.soton.comp1206.game;

import javafx.scene.control.Alert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.event.NextPieceListener;
import uk.ac.soton.comp1206.event.GameLoopListener;
import uk.ac.soton.comp1206.event.LineClearedListener;
import uk.ac.soton.comp1206.event.CurrentPieceListener;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.scene.Multimedia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static uk.ac.soton.comp1206.game.GamePiece.createPiece;

/**
 * The MultiplayerGame class is responsible for handling logic pertaining to the multiplayer. Unlike Game, MultiplayerGame
 * communicates to web socket server to generate new pieces, update scores, lives, messages and board statuses.
 */
public class MultiplayerGame extends Game {

    private final Logger logger = LogManager.getLogger(MultiplayerGame.class);

    /**
     * Communicator object to send messages to web server socket.
     */
    private final Communicator communicator;

    /**
     * Communicator sends message requesting pieces. Returns value and the values are used to generate pieces. The pieces
     * are then stored in a queue.
     */
    private final Queue<GamePiece> pieceQueue = new LinkedList<>();

    // Listeners
    /**
     * Assigns a listener to GamePiece object for UI manipulation.
     */
    private CurrentPieceListener currentPieceListener;

    /**
     * Assigns a listener to GamePiece object for UI manipulation.
     */
    private NextPieceListener nextPieceListener;

    /**
     * Create a new game with the specified rows and columns. Creates a corresponding grid model.
     *
     * @param cols number of columns
     * @param rows number of rows
     */
    public MultiplayerGame(int cols, int rows, Communicator communicator) {
        super(cols, rows);

        logger.info("Constructing new multiplayer game");

        this.communicator = communicator;

        this.communicator.addListener(this::receiveMessage);
    }

    /**
     * Schedules initialRunnable to generate pieces to pieceBoard, executes only once. After runnable is scheduled after
     * the given delay.
     */
    @Override
    public void initialiseGame() {
        logger.info("Initialising multiplayer game");

        scheduler = Executors.newScheduledThreadPool(1);

        Runnable initialRunnable = () -> {
            receivePiece();
            nextPiece = getPiece();
            nextPiece();
        };

        scheduler.execute(initialRunnable);

        runnable = this::gameLoop;

        schedulerFuture = scheduler.scheduleAtFixedRate(runnable, getDelay(), getDelay(), TimeUnit.MILLISECONDS);
    }

    /**
     * Reschedule after specified delay duration. Message to sent to communicator to update lives.
     */
    @Override
    protected void gameLoop() {
        super.gameLoop();

        sendLives();
    }

    /**
     * Once lives reaches -1, the game is ended
     */
    @Override
    protected void gameOver() {
        super.gameOver();
        if (getLives() < 0) {
            logger.info("Game over");
            //sendGameOver();

            setLives(0);
        }
    }

    /**
     * Replaces currentPiece with nextPiece and assigns new piece from pieceQueue to nextPiece.
     */
    @Override
    protected void nextPiece() {
        // Send message 4 times and receive 4 pieces.
        for (int i = 0; i < 4; i++) {
            receivePiece();
        }

        currentPiece = nextPiece;
        currentPieceListener.currentPiece(currentPiece);

        nextPiece = getPiece();
        nextPieceListener.nextPiece(nextPiece);
    }

    /**
     * Handle what should happen when a particular block is clicked.
     * @param gameBlock the block that was clicked
     */
    @Override
    public void blockClicked(GameBlock gameBlock) {
        // Get the position of this block
        int x = gameBlock.getX();
        int y = gameBlock.getY();
        StringBuilder blockValues = new StringBuilder();
        ArrayList<Integer> blocks= new ArrayList<>();

        // Update pieces only if a piece can be played
        if (grid.playPiece(x, y, currentPiece)) {
            Multimedia.playSound("place.wav");

            // Calculate delay
            getTimerDelay();

            // Reset timer
            changeDelayTime(getDelay());

            // Reset timer
            loopGame(getDelay());

            // Fetch next piece
            nextPiece();

            // Check if anything needs to be done after playing a piece
            afterPiece();

            for (int i = 0; i < getRows(); i++) {
                for (int j = 0; j < getCols(); j++) {
                    blocks.add(grid.get(i, j));
                }
            }

            for (Integer i : blocks) {
                blockValues.append(i).append(" ");
            }

            //communicator.send("BOARD " + blockValues);
        } else {
            Multimedia.playSound("fail.wav");
        }
    }


    /**
     * Receives messages from web socket server.
     * @param message message received
     */
    private void receiveMessage(String message) {
        if (message.startsWith("PIECE")) {
            int value = Integer.parseInt(message.replace("PIECE ", ""));

            // Adds piece to queue then notifies any threads waiting
            synchronized (pieceQueue) {
                pieceQueue.add(createPiece(value));
                pieceQueue.notifyAll();
            }
        }
        if (message.startsWith("ERROR")) {
            logger.error("An error occurred, {}", message.replace("ERROR ", ""));

            Alert alertError = new Alert(Alert.AlertType.ERROR);
            alertError.setContentText("An error has occurred!\n\n" + message.replace("ERROR ", ""));

            alertError.show();
        }
    }

    /**
     * Retrieves piece from queue. If queue is empty, wait on thread.
     * @return GamePiece from pieceQueue
     */
    private GamePiece getPiece() {
        synchronized (pieceQueue) {
            while (pieceQueue.isEmpty()) {
                try {
                    pieceQueue.wait();
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
            return pieceQueue.remove();
        }
    }

    /**
     * Sends PIECE message to web socket server.
     */
    private void receivePiece() {
        logger.info("Sending PIECE message");
        //communicator.send("PIECE");
    }

    /**
     * Sends player lives to web socket server.
     */
    private void sendLives() {
        logger.info("Sending LIVES message");
        //communicator.send("LIVES " + lives.get());
    }

    // Listeners
    /**
     * Set listener object.
     * @param listener listener object
     */
    public void setCurrentPieceListener(CurrentPieceListener listener) {
        this.currentPieceListener = listener;
    }

    /**
     * Set listener object.
     * @param listener listener object
     */
    public void setNextPieceListener(NextPieceListener listener) {
        this.nextPieceListener = listener;
    }

    /**
     * Set listener object.
     * @param listener listener object
     */
    public void addLineClearedListener(LineClearedListener listener) {
        this.lineClearedListener = listener;
    }

    /**
     * Set listener object.
     * @param listener listener object
     */
    public void setOnGameLoop(GameLoopListener listener) {
        this.gameLoopListener = listener;
    }

    /**
     * Set listener object.
     * @param duration time duration
     */
    protected void loopGame(int duration) {
        gameLoopListener.gameLooped(duration);
    }

    /**
     * Set listener object.
     * @param coords coordinates of blocks
     */
    protected void clearLine(HashSet<int[]> coords) {
        lineClearedListener.lineCleared(coords);
    }
}
