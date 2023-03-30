package uk.ac.soton.comp1206.component;

import javafx.animation.PauseTransition;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.event.BlockClickedListener;
import uk.ac.soton.comp1206.event.RightClickedListener;
import uk.ac.soton.comp1206.game.Grid;

import java.util.HashSet;

/**
 * A GameBoard is a visual component to represent the visual GameBoard.
 * It extends a GridPane to hold a grid of GameBlocks.
 *
 * The GameBoard can hold an internal grid of its own, for example, for displaying an upcoming block. It is also
 * linked to an external grid, for the main game board.
 *
 * The GameBoard is only a visual representation and should not contain game logic or model logic in it, which should
 * take place in the Grid.
 */
public class GameBoard extends GridPane {

    private final Logger logger = LogManager.getLogger(GameBoard.class);

    /**
     * Number of columns in the board
     */
    protected final int cols;

    /**
     * Number of rows in the board
     */
    protected final int rows;

    /**
     * The visual width of the board - has to be specified due to being a Canvas
     */
    protected final double width;

    /**
     * The visual height of the board - has to be specified due to being a Canvas
     */
    protected final double height;

    /**
     * Aim of x coordinate
     */
    int aimX;

    /**
     * Aim of y coordinate
     */
    int aimY;

    /**
     * The grid this GameBoard represents
     */
    final Grid grid;

    /**
     * The blocks inside the grid
     */
    GameBlock[][] blocks;

    /**
     * The listener to call when a specific block is clicked
     */
    public BlockClickedListener blockClickedListener;

    /**
     * The listener to call when right clicked on GameBoard
     */
    public RightClickedListener rightClickedListener;

    /**
     * Create a new GameBoard, based off a given grid, with a visual width and height.
     * @param grid linked grid
     * @param width the visual width
     * @param height the visual height
     */
    public GameBoard(Grid grid, double width, double height) {
        this.cols = grid.getCols();
        this.rows = grid.getRows();
        this.width = width;
        this.height = height;
        this.grid = grid;

        this.setOnMouseClicked(this::rightClicked);

        //Build the GameBoard
        build();
    }

    /**
     * Create a new GameBoard with its own internal grid, specifying the number of columns and rows, along with the
     * visual width and height.
     *
     * @param cols number of columns for internal grid
     * @param rows number of rows for internal grid
     * @param width the visual width
     * @param height the visual height
     */
    public GameBoard(int cols, int rows, double width, double height) {
        this.cols = cols;
        this.rows = rows;
        this.width = width;
        this.height = height;
        this.grid = new Grid(cols,rows);

    }

    /**
     * Get a specific block from the GameBoard, specified by its row and column
     * @param x column
     * @param y row
     * @return game block at the given column and row
     */
    public GameBlock getBlock(int x, int y) {
        return blocks[x][y];
    }

    /**
     * Build the GameBoard by creating a block at every x and y column and row
     */
    protected void build() {
        logger.info("Building grid: {} x {}",cols,rows);

        aimX = cols/2;
        aimY = rows/2;

        setMaxWidth(width);
        setMaxHeight(height);

        setGridLinesVisible(true);

        blocks = new GameBlock[cols][rows];

        for(var y = 0; y < rows; y++) {
            for (var x = 0; x < cols; x++) {
                createBlock(x, y);
            }
        }
    }

    /**
     * Create a block at the given x and y position in the GameBoard
     * @param x column
     * @param y row
     */
    protected GameBlock createBlock(int x, int y) {
        var blockWidth = width / cols;
        var blockHeight = height / rows;

        // Create a new GameBlock UI component
        GameBlock block = new GameBlock(this, x, y, blockWidth, blockHeight);

        // Add to the GridPane
        add(block,x,y);

        // Add to the block directory
        blocks[x][y] = block;

        // Link the GameBlock component to the corresponding value in the Grid
        block.bind(grid.getGridProperty(x,y));

        // Add a mouse click handler to the block to trigger GameBoard blockClicked method
        block.setOnMouseClicked((e) -> blockClicked(e, block));

        // Paint highlighted block over current block mouse is hovering on
        block.setOnMouseEntered((MouseEvent event) -> {
            aimX = x;
            aimY = y;



            getBlock(getAimX(), getAimY()).mouseHoverPaint();
        });

        // Upon exiting the block with the mouse, reset the paint
        block.setOnMouseExited((MouseEvent event) -> {
            if (grid.get(getBlock(getAimX(), getAimY()).getX(), getBlock(getAimX(), getAimY()).getY()) == 0) {
                getBlock(getAimX(), getAimY()).resetPaint();
            } else {
                getBlock(getAimX(), getAimY()).resetEstablishedPaint(getBlock(getAimX(), getAimY()));
            }
        });


        return block;
    }

    /**
     * Play fadeout animation on specified block coordinates.
     * @param coords coordinates of blocks to be faded out
     */
    public void fadeOut(HashSet<int[]> coords) {
        GameBlock block;

        for (int[] coord : coords) {
            block = getBlock(coord[1], coord[0]);
            block.fadeOut(block);
        }

        PauseTransition pauseTransition = new PauseTransition(new Duration(500));
        pauseTransition.setOnFinished((event) -> {
            for (int[] k : coords) {
                grid.set(k[1], k[0], 0);
            }
        });
        pauseTransition.play();

    }

    /**
     * Get current x aim of board
     * @return current aim x coordinate
     */
    public int getAimX() {
        return aimX;
    }

    /**
     * Get current y aim of board
     * @return current aim y coordinate
     */
    public int getAimY() {
        return aimY;
    }

    public void setAimX(int x) {
        aimX = x;
    }

    public void setAimY(int y) {
        aimY = y;
    }

    /**
     * Set the listener to handle an event when a block is clicked
     * @param listener listener to add
     */
    public void setOnBlockClick(BlockClickedListener listener) {
        this.blockClickedListener = listener;
    }

    /**
     * Set the listener to handle an event when right clicking on board
     * @param listener listener to add
     */
    public void setOnRightClick(RightClickedListener listener) { this.rightClickedListener = listener; }

    /**
     * Triggered when a block is clicked. Call the attached listener.
     * @param event mouse event
     * @param block block clicked on
     */
    protected void blockClicked(MouseEvent event, GameBlock block) {
        logger.info("Block clicked, x: {}, y: {}", block.getX(), block.getY());

        if (event.getButton() == MouseButton.PRIMARY) {
            if (blockClickedListener != null) {
                blockClickedListener.blockClicked(block);
            }
        }
    }

    /**
     * Triggered when the board is clicked. Call the attached listener.
     * @param event mouse event
     */
    private void rightClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            logger.info("Pressed, {}", event.getButton());
            if (rightClickedListener != null) {
                rightClickedListener.rightClicked();
            }
        }
    }
}
