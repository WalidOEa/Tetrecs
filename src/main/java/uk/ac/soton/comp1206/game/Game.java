package uk.ac.soton.comp1206.game;

import javafx.beans.property.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.event.*;
import uk.ac.soton.comp1206.scene.Multimedia;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.*;

import static uk.ac.soton.comp1206.game.GamePiece.PIECES;

/**
 * The Game class handles the main logic, state and properties of the TetrECS game. Methods to manipulate the game state
 * and to handle actions made by the player take place in this class.
 */
public class Game {

    private final Logger logger = LogManager.getLogger(Game.class);

    /**
     * Keeps track of current GamePiece object.
     */
    protected GamePiece currentPiece;

    /**
     * Keeps track of the next GamePiece object.
     */
    protected GamePiece nextPiece;

    /**
     * Number of rows of grid.
     */
    protected final int rows;

    /**
     * Number of columns.
     */
    protected final int cols;

    /**
     * The grid model linked to the game.
     */
    protected final Grid grid;

    /**
     * Random object to generate a random number for spawnPiece method.
     */
    private final Random rand = new Random();

    // Properties
    /**
     * Keeps track of player score earned by clearing lines in Game.
     */
    protected final SimpleIntegerProperty score = new SimpleIntegerProperty(0);

    /**
     * Keeps track of level that increments every 1000 points in Game.
     */
    protected final SimpleIntegerProperty level = new SimpleIntegerProperty(0);

    /**
     * Keeps track of number of lives lost after each scheduled task finishes execution in Game.
     */
    protected final SimpleIntegerProperty lives = new SimpleIntegerProperty(3);

    /**
     * Keeps track of multiplier gained from clearing lines in Game.
     */
    protected final SimpleIntegerProperty multiplier = new SimpleIntegerProperty(1);

    /**
     * Keeps track of high score in Game.
     */
    protected final SimpleIntegerProperty hiScore = new SimpleIntegerProperty(0);

    /**
     * Keeps track of game over state.
     */
    public final SimpleBooleanProperty gameOver = new SimpleBooleanProperty(false);

    /**
     * Keeps track of time delay
     */
    public int delay = 12000;

    // Listeners
    /**
     * Assigns a listener to GamePiece object for UI manipulation.
     */
    protected NextPieceListener nextPieceListener;

    /**
     * Assigns a listener to GamePiece object for UI manipulation.
     */
    protected CurrentPieceListener currentPieceListener;

    /**
     * Passes set of block coordinates to UI components to manipulate.
     */
    protected LineClearedListener lineClearedListener;

    /**
     * Passes the condition of the duration onto scheduler in UI.
     */
    protected GameLoopListener gameLoopListener;

    /**
     * Schedules method to be called after initial time delay
     */
    protected ScheduledExecutorService scheduler;

    /**
     * Schedules future methods to be called after modified time delay
     */
    protected ScheduledFuture<?> schedulerFuture;

    /**
     * Anonymous class to be placed into thread
     */
    protected Runnable runnable;

    /**
     * Determines if duration of delay has decreased
     */
    protected boolean scheduleFlag = false;

    /**
     * Create a new game with the specified rows and columns. Creates a corresponding grid model.
     * @param cols number of columns
     * @param rows number of rows
     */
    public Game(int cols, int rows) {
        logger.info("Constructing new game");

        this.cols = cols;
        this.rows = rows;

        // Create a new grid model to represent the game state
        this.grid = new Grid(cols,rows);
    }

    /**
     * Start the game
     */
    public void start() {
        logger.info("Starting game");

        // Initialise the game
        initialiseGame();

    }

    /**
     * Initialise a new game and instantiate a new Runnable anonymous class. runnable calls to gameLoop and is to be scheduled after
     * a specified time delay in milliseconds. nextPiece is assigned a piece initially.
     */
    protected void initialiseGame() {
        logger.info("Initialising singleplayer game");

        scheduler = Executors.newScheduledThreadPool(1);

        runnable = this::gameLoop;

        schedulerFuture = scheduler.scheduleAtFixedRate(runnable, getDelay(), getDelay(), TimeUnit.MILLISECONDS);

        // Assigned once, subsequent calls handled by gameLoop
        nextPiece = spawnPiece();

        nextPiece();
    }

    /**
     * Cancels any currently running tasks and reschedules a new task with a different delay.
     * @param delay new time in milliseconds to schedule
     */
    protected void changeDelayTime(int delay) {
        if (schedulerFuture != null) {
            logger.info("Cancelling task");
            schedulerFuture.cancel(true);
        }
        schedulerFuture = scheduler.scheduleAtFixedRate(runnable, delay, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Reschedule after specified delay duration. Responsible for all logic pertaining to timer.
     */
    protected void gameLoop() {
        logger.info("Scheduling new task");

        Multimedia.playSound("lifelose.wav");

        // Calculate time delay
        getTimerDelay();

        // Reschedule delay
        loopGame(getDelay());

        // Decrement lives
        setLives(getLives() - 1);

        // Reset pieces
        nextPiece();

        // Reset multiplier
        setMultiplier(1);

        // Check for game over
        gameOver();
    }

    /**
     * Checks to see whether game has ended.
     */
    protected void gameOver() {
        if (getLives() < 0) {
            logger.info("Game over");

            Multimedia.musicCleanUp();
            schedulerFuture.cancel(true);
            scheduler.shutdown();
            gameOver.set(true);
        }
    }

    /**
     * Calculates new delay given level.
     */
    protected void getTimerDelay() {
        logger.info("Current delay is, {}", getDelay());

        if (scheduleFlag) {
            setDelay(Math.max((12000 - (500 * getLevel())), 2500));
            logger.info("Delay changed to, {}", getDelay());

            // Set new task with new delay
            changeDelayTime(getDelay());

            scheduleFlag = false;
        }
    }

    /**
     * Handle what should happen when a particular block is clicked.
     * @param gameBlock the block that was clicked
     */
    public void blockClicked(GameBlock gameBlock) {
        // Get the position of this block
        int x = gameBlock.getX();
        int y = gameBlock.getY();

        // Update pieces only if a piece can be played
        if (grid.playPiece(x, y, currentPiece)) {
            Multimedia.playSound("place.wav");

            // Calculate delay
            getTimerDelay();

            changeDelayTime(getDelay());

            // Reset timer
            loopGame(getDelay());

            // Fetch next piece
            nextPiece();

            // Check if anything needs to be done after playing a piece
            afterPiece();
        } else {
            Multimedia.playSound("fail.wav");
        }
    }

    /**
     * Creates a new random game piece.
     * @return gamePiece a newly created gamePiece
     */
    private GamePiece spawnPiece() {
        return GamePiece.createPiece(rand.nextInt(PIECES));
    }

    /**
     * Replaces currentPiece with nextPiece and assigns new piece to nextPiece.
     */
    protected void nextPiece() {
        currentPiece = nextPiece;

        currentPieceListener.currentPiece(currentPiece);

        nextPiece = spawnPiece();

        nextPieceListener.nextPiece(nextPiece);

        logger.info("Piece created, {}", nextPiece);
    }

    /**
     * Rotates the current piece by 90 degrees clockwise.
     * @param piece piece to be rotated
     */
    public void rotateCurrentPiece(GamePiece piece) {
        logger.info("Rotating piece");

        piece.rotate();
    }

    /**
     * Swaps the current piece and the next piece with each other.
     * @param currentPiece current piece in Game
     * @param nextPiece next piece in Game
     */
    public void swapCurrentPiece(GamePiece currentPiece, GamePiece nextPiece) {
        logger.info("Swapping piece");

        setCurrentPiece(nextPiece);

        setNextPiece(currentPiece);
    }

    /**
     * Handles the clearance of lines if possible. This is done by obtaining the grid makeup then checking rows and columns for values > 0. For any
     * lines to be cleared, means that blocks in either a row of column or intersecting row/column have values > 0. The blocks are added to a hashset and
     * are then reset to their original value of 0. Changes are then made to score and multiplier. If necessary, increment the level.
     */
    protected void afterPiece() {
        HashSet<int[]> blocks = new HashSet<>();
        int[][] rows = new int[getCols()][2];
        int[][] cols = new int[getRows()][2];
        int[][][] coordinates = new int[getRows()][getCols()][];
        boolean flag;
        int lines = 0;

        // Get grid makeup
        for (int i = 0; i < getCols(); i++) {
            for (int j = 0; j < getRows(); j++) {
                coordinates[i][j] = new int[] {i, j};
            }
        }

        // Check rows first
        for (int i = 0; i < getCols(); i++) {
            flag = true;

            if (getRows() >= 0) System.arraycopy(coordinates[i], 0, rows, 0, getRows());

            // Check each value in row
            for(int[] k : rows) {
                if (grid.get(k[1], k[0]) == 0) {
                    flag = false;
                }
            }

            if (flag) {
                lines++;

                Collections.addAll(blocks, rows);
            }
        }

        // Check columns next
        for (int j = 0; j < getRows(); j++) {
            flag = true;

            for (int i = 0; i < getCols(); i++) {
                cols[i] = coordinates[i][j];
            }

            for(int[] k : cols) {
                if (grid.get(k[1], k[0]) == 0) {
                    flag = false;
                }
            }

            if (flag) {
                Multimedia.playSound("clear.wav");

                lines++;

                Collections.addAll(blocks, cols);
            }
        }

        setScore(getScore() + (lines * blocks.size() * 10 * getMultiplier()));

        // Changes high score to user score
        if (getScore() > getHiScore()) {
            setHiScore(getScore());
        }

        // Increment to multiplier if necessary
        if (lines > 0) {
            setMultiplier(getMultiplier() + 1);
        } else {
            setMultiplier(1);
        }

        // Fire listener to fade out animation
        clearLine(blocks);

        // Level has changed
        if (getScore()/1000 != getLevel()) {
            logger.info("Level changed");

            setLevel(getScore()/1000);
            scheduleFlag = true;
            getTimerDelay();
            loopGame(getDelay());
        }
    }

    /**
     * Reset state of game. Used when clearing up scenes and transition between menu and challenge scenes.
     */
    public void resetState() {
        setDelay(12000);
        setMultiplier(1);
        setLives(3);
        setScore(0);
        Multimedia.musicCleanUp();
        schedulerFuture.cancel(true);
        scheduler.shutdown();
        gameOver.set(false);
    }

    /**
     * Removes all pieces from board and adds a new arbitrary score.
     */
    public boolean bomb() {
        HashSet<int[]> blocks = new HashSet<>();
        int[][][] coordinates = new int[getRows()][getCols()][];

        logger.info("Bomb used");

        int k = 0;

        // Get grid makeup
        for (int i = 0; i < getCols(); i++) {
            for (int j = 0; j < getRows(); j++) {
                coordinates[i][j] = new int[] {i, j};
            }
        }

        // Add blocks to set
        for (int[][] i : coordinates) {
            for (int[] j : i) {
                if (grid.get(j[1], j[0]) > 0) {
                    blocks.add(j);
                }
            }
        }

        clearLine(blocks);

        // Count each block with value > 0
        for (int i = 0; i < getCols(); i++) {
            for (int j = 0; j < getRows(); j++) {
                if (grid.get(i, j) > 0) {
                    k++;
                }
            }
        }

        setScore(getScore() + (k * 70) - 500);

        if (getScore()/1000 != getLevel()) {
            setLevel(getScore()/1000);
            loopGame(getDelay());
            scheduleFlag = true;
        }

        return true;
    }

    /**
     * Adds a new life, used when selecting lives button.
     */
    public void newLife() {
        setScore(getScore() - 2000);
        setLives(getLives() + 1);
    }

    /**
     * Assign listener object for observers to be alerted when lines are cleared.
     * @param listener listener object to assign
     */
    public void addLineClearedListener(LineClearedListener listener) {
        this.lineClearedListener = listener;
    }

    /**
     * Assign listener object for observers to reset UI timer.
     * @param listener listener object to assign
     */
    public void setOnGameLoop(GameLoopListener listener) {
        this.gameLoopListener = listener;
    }

    /**
     * Assign listener object for observers to reset UI timer.
     * @param duration listener object to assign
     */
    protected void loopGame(int duration) {
        gameLoopListener.gameLooped(duration);
    }

    /**
     * Assign listener object for observers to clear out lines specified by the given coordinates.
     * @param coords listener object to assign
     */
    protected void clearLine(HashSet<int[]> coords) {
        lineClearedListener.lineCleared(coords);
    }

    /**
     * Assign listener object for observers to know what the current piece is.
     * @param listener listener object to assign
     */
    public void setCurrentPieceListener(CurrentPieceListener listener) {
        this.currentPieceListener = listener;
    }

    /**
     * Assign listener object for observers to know what the next piece is.
     * @param listener listener object to assign
     */
    public void setNextPieceListener(NextPieceListener listener) {
        this.nextPieceListener = listener;
    }


    // Mutators
    /**
     * Set the new delay given by getTimerDelay
     */
    public void setDelay(int newDelay) { delay = newDelay; }

    /**
     * Set the score upon clearing lines
     * @param score new score to be set
     */
    public void setScore(int score) {
        this.score.set(score);
    }

    /**
     * Set the new high score if the old high score has been beaten
     * @param hiScore new high score to be set
     */
    public void setHiScore(int hiScore) {
        this.hiScore.set(Math.max(score.get(), hiScore));
    }

    /**
     * Set the new multiplier when clearing lines
     * @param multiplier multiplier to be set
     */
    public void setMultiplier(int multiplier) { this.multiplier.set(multiplier); }

    /**
     * Set the new level upon obtaining 1000 points
     * @param level level to be set
     */
    public void setLevel(int level) { this.level.set(level); }

    /**
     * Set the life if failed to place piece
     * @param life live to be set
     */
    public void setLives(int life) { this.lives.set(life); }

    /**
     * Set current piece, used for swapping pieces
     * @param piece piece to swap
     */
    public void setCurrentPiece(GamePiece piece) {
        currentPiece = piece;
    }

    /**
     * Set the next piece, used for swapping piece
     * @param piece piece to swap
     */
    public void setNextPiece(GamePiece piece) {
        nextPiece = piece;
    }

    // Accessors
    /**
     * Get new delay. Used to reassign tasks
     */
    public int getDelay() { return delay; }

    /**
     * Return the current piece.
     * @return currentPiece
     */
    public GamePiece getCurrentPiece() { return currentPiece; }

    /**
     * Return the next piece.
     * @return nextPiece
     */
    public GamePiece getNextPiece() {
        return nextPiece;
    }

    /**
     * Get the grid model inside this game representing the game state of the board.
     * @return game grid model
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Get the number of columns in this game.
     * @return number of columns
     */
    public int getCols() {
        return cols;
    }

    /**
     * Get the number of rows in this game.
     * @return number of rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Get the current score.
     * @return score
     */
    public int getScore() {
        return this.score.get();
    }

    /**
     * Get the current level.
     * @return level
     */
    public int getLevel() {
        return this.level.get();
    }

    /**
     * Get the current lives.
     * @return lives
     */
    public int getLives() {
        return this.lives.get();
    }

    /**
     * Get current high score.
     * @return high score
     */
    public int getHiScore() { return this.hiScore.get(); }

    /**
     * Get the current multiplier.
     * @return multiplier
     */
    public int getMultiplier() {
        return this.multiplier.get();
    }

    // Property exposing
    /**
     * Expose the score property.
     * @return score property
     */
    public IntegerProperty scoreProperty() {
        return this.score;
    }

    /**
     * Expose the level property.
     * @return level property
     */
    public IntegerProperty levelProperty() {
        return level;
    }

    /**
     * Expose the multiplier property.
     * @return multiplier property
     */
    public IntegerProperty multiplierProperty() {
        return this.multiplier;
    }

    /**
     * Expose the gameOver flag property.
     * @return  gameOver property
     */
    public BooleanProperty gameOverProperty() {
        return this.gameOver;
    }

    /**
     * Expose the hiScore property.
     * @return hiScore property
     */
    public IntegerProperty hiScoreProperty() {
        return this.hiScore;
    }

    /**
     * Expose the lives property.
     * @return lives property
     */
    public IntegerProperty livesProperty() {
        return this.lives;
    }
}
