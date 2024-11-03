package tetrecs.game;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * The Grid is a model which holds the state of a game board. It is made up of a set of Integer values arranged in a 2D
 * arrow, with rows and columns.
 *
 * Each value inside the Grid is an IntegerProperty can be bound to enable modification and display of the contents of
 * the grid.
 *
 * The Grid contains functions related to modifying the model, for example, placing a piece inside the grid.
 *
 * The Grid should be linked to a GameBoard for its display.
 */
public class Grid {

    /**
     * The number of columns in this grid.
     */
    private final int cols;

    /**
     * The number of rows in this grid.
     */
    private final int rows;

    /**
     * The grid is a 2D arrow with rows and columns of SimpleIntegerProperties.
     */
    private final SimpleIntegerProperty[][] grid;

    /**
     * Create a new Grid with the specified number of columns and rows and initialise them.
     * @param cols number of columns
     * @param rows number of rows
     */
    public Grid(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;

        // Create the grid itself
        grid = new SimpleIntegerProperty[cols][rows];

        // Add a SimpleIntegerProperty to every block in the grid
        for (var y = 0; y < rows; y++) {
            for (var x = 0; x < cols; x++) {
                grid[x][y] = new SimpleIntegerProperty(0);
            }
        }
    }

    /**
     * Get the Integer property contained inside the grid at a given row and column index. Can be used for binding.
     *
     * @param x column
     * @param y row
     * @return the IntegerProperty at the given x and y in this grid
     */
    public IntegerProperty getGridProperty(int x, int y) {
        return grid[x][y];
    }

    /**
     * Update the value at the given x and y index within the grid.
     * @param x     column
     * @param y     row
     * @param value the new value
     */
    public void set(int x, int y, int value) {
        grid[x][y].set(value);
    }

    /**
     * Get the value represented at the given x and y index within the grid.
     * @param x column
     * @param y row
     * @return the value
     */
    public int get(int x, int y) {
        try {
            // Get the value held in the property at the x and y index provided
            return grid[x][y].get();
        } catch (ArrayIndexOutOfBoundsException e) {
            // No such index
            return -1;
        }
    }

    /**
     * Get the number of columns in this game.
     *
     * @return number of columns
     */
    public int getCols() {
        return cols;
    }

    /**
     * Get the number of rows in this game.
     *
     * @return number of rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Determines whether a piece can be played by extracting the surrounding 3x3 matrix makeup of the x and y positions,
     * adding it to the matrix makeup of the piece and comparing the new matrix to the piece value. For any block not representing
     * the value of gamePiece: if the value has changed compared to the original makeup, return false.
     * @param x         x position of block clicked
     * @param y         y position of block clicked
     * @param gamePiece piece to extract 3x3 matrix makeup
     * @return boolean whether a piece can be played or not
     */
    public boolean canPlayPiece(int x, int y, GamePiece gamePiece) {
        int[][] blocks = gamePiece.getBlocks();
        int[][] grid = new int[3][3];
        int[][] tempMatrix = new int[3][3];
        int temp_x = x;

        // Extract 3x3 matrix from grid
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                grid[i][j] = get(x - 1, y - 1);
                x++;
            }
            x = temp_x;
            y++;
        }

        // Add both matrices together
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tempMatrix[i][j] = grid[i][j] + blocks[i][j];
            }
        }

        // Determine illegal placement by comparing the two matrices
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tempMatrix[i][j] != grid[i][j]) {
                    if (tempMatrix[i][j] != gamePiece.getValue()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Places the piece into the grid and updates surrounding values with value of gamePiece, according to its makeup.
     * @param x x position of block clicked
     * @param y y position of block clicked
     * @param gamePiece GamePiece object to play in grid
     * @return boolean used in Game to verify if piece has been placed.
     */
    public boolean playPiece(int x, int y, GamePiece gamePiece) {
        int[][] blocks = gamePiece.getBlocks();
        int[][][] coordinates = new int[3][3][];
        int temp_x = x;

        if (canPlayPiece(x, y, gamePiece)) {

            // Store coordinates as array into 3x3 matrix
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    coordinates[i][j] = new int[]{x - 1, y - 1};
                    x++;
                }
                x = temp_x;
                y++;
            }

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (blocks[i][j] == gamePiece.getValue()) {
                        set(coordinates[i][j][0], coordinates[i][j][1], blocks[i][j]);
                    }
                }
            }
            return true;
        }
        return false;
    }
}
