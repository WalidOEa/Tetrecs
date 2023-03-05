package uk.ac.soton.comp1206.component;

import javafx.beans.property.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.event.LeftClickedListener;
import uk.ac.soton.comp1206.game.GamePiece;


/**
 * Contains and displays both upcoming pieces in a 3x3 grid
 */
public class PieceBoard extends GameBoard {

    private final Logger logger = LogManager.getLogger(PieceBoard.class);

    /**
     * Listener to assign
     */
    public LeftClickedListener leftClickedListener;

    /**
     * Calculates centreblock of pieceboard
     */
    public static SimpleObjectProperty<GameBlock> centreBlock = new SimpleObjectProperty<>();

    /**
     * Creates a new gameboard with specified number of columns and rows along with visual height
     * and width
     *
     * @param cols   columns of pieceboard
     * @param rows   rows of pieceboard
     * @param width  width of pieceboard
     * @param height height of pieceboard
     */
    public PieceBoard(int cols, int rows, double width, double height) {
        super(cols, rows, width, height);

        this.setOnMouseClicked(this::leftClicked);

        build();
    }

    /**
     * Build the GameBoard by creating a block at every x and y column and row
     */
    @Override
    protected void build() {
        logger.info("Building grid: {} x {}", cols, rows);

        setMaxWidth(width);
        setMaxHeight(height);

        setGridLinesVisible(true);

        blocks = new GameBlock[cols][rows];

        for (var y = 0; y < rows; y++) {
            for (var x = 0; x < cols; x++) {
                if (x == cols / 2 && y == rows / 2) {
                    logger.info("Centre block found");
                    setCentreBlock(createBlock(x, y));
                } else {
                    createBlock(x, y);
                }
            }
        }
    }

    /**
     * Create a block at the given x and y position in the GameBoard
     *
     * @param x column
     * @param y row
     */
    @Override
    protected GameBlock createBlock(int x, int y) {
        var blockWidth = width / cols;
        var blockHeight = height / rows;

        // Create a new GameBlock UI component
        GameBlock block = new GameBlock(this, x, y, blockWidth, blockHeight);

        // Add to the GridPane
        add(block, x, y);

        // Add to the block directory
        blocks[x][y] = block;

        // Link the GameBlock component to the corresponding value in the Grid
        block.bind(grid.getGridProperty(x, y));

        // Add a mouse click handler to the block to trigger GameBoard blockClicked method
        block.setOnMouseClicked((e) -> blockClicked(e, block));

        return block;
    }

    /**
     * Displays current and next piece
     * @param piece piece to display in pieceboard
     */
    public void displayPiece(GamePiece piece) {
        int[][] blocks = piece.getBlocks();
        int[][] pieceGrid = new int[3][3];

        // Get piece board make up
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                pieceGrid[i][j] = grid.get(i, j);
            }
        }

        // Clean up grid
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (pieceGrid[i][j] > 0) {
                    grid.set(i, j, 0);
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (blocks[i][j] > 0) {
                    grid.set(j, i, piece.getValue());
                }
            }
        }
    }

    /**
     * Binds listeners
     * @param listener listener to assign
     */
    public void setOnLeftClick(LeftClickedListener listener) {
        this.leftClickedListener = listener;
    }

    /**
     * If leftclicked on pieceboard, fire listener
     * @param event mouse event
     */
    private void leftClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            if (leftClickedListener != null) {
                leftClickedListener.leftClicked();
            }
        }
    }

    /**
     * Set centre block of pieceboard
     * @param block block to assign as centre
     */
    public void setCentreBlock(GameBlock block) {
        centreBlock.set(block);
    }

    /**
     * Return centreblock of pieceboard
     * @return return centre block of pieceboard
     */
    public static GameBlock centreBlockProperty() {
        return centreBlock.get();
    }

}
