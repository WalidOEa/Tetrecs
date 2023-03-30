package uk.ac.soton.comp1206.component;

import javafx.animation.*;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.*;

/**
 * The Visual User Interface component representing a single block in the grid.
 *
 * Extends Canvas and is responsible for drawing itself.
 *
 * Displays an empty square (when the value is 0) or a coloured square depending on value.
 *
 * The GameBlock value should be bound to a corresponding block in the Grid model.
 */
public class GameBlock extends Canvas {

    /**
     * The set of colours for different pieces
     */
    public static final Color[] COLOURS = {
            Color.TRANSPARENT,
            Color.DEEPPINK,
            Color.RED,
            Color.ORANGE,
            Color.YELLOW,
            Color.YELLOWGREEN,
            Color.LIME,
            Color.GREEN,
            Color.DARKGREEN,
            Color.DARKTURQUOISE,
            Color.DEEPSKYBLUE,
            Color.AQUA,
            Color.AQUAMARINE,
            Color.BLUE,
            Color.MEDIUMPURPLE,
            Color.PURPLE
    };

    private final double width;
    private final double height;


    /**
     * The column this block exists as in the grid
     */
    private final int x;

    /**
     * The row this block exists as in the grid
     */
    private final int y;

    private double opacity;

    /**
     * The value of this block (0 = empty, otherwise specifies the colour to render as)
     */
    private final IntegerProperty value = new SimpleIntegerProperty(0);

    /**
     * Create a new single Game Block
     * @param gameBoard the board this block belongs to
     * @param x the column the block exists in
     * @param y the row the block exists in
     * @param width the width of the canvas to render
     * @param height the height of the canvas to render
     */
    public GameBlock(GameBoard gameBoard, int x, int y, double width, double height) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;

        // A canvas needs a fixed width and height
        setWidth(width);
        setHeight(height);

        // Do an initial paint
        paint();

        // When the value property is updated, call the internal updateValue method
        value.addListener(this::updateValue);
    }

    /**
     * When the value of this block is updated,
     * @param observable what was updated
     * @param oldValue the old value
     * @param newValue the new value
     */
    private void updateValue(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        paint();
    }

    /**
     * Handle painting of the block canvas
     */
    public void paint() {
        // If the block is empty, paint as empty
        if(value.get() == 0) {
            paintEmpty();
        } else {
            // If the block is not empty, paint with the colour represented by the value
            paintColor(COLOURS[value.get()]);
        }
    }

    /**
     * Paint this canvas empty
     */
    public void paintEmpty() {
        var gc = getGraphicsContext2D();

        //Clear
        gc.clearRect(0,0,width,height);

        //Fill
        gc.setGlobalAlpha(0.5);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        //Triangle
        gc.setGlobalAlpha(0.17);
        gc.setFill(Color.DARKGRAY);
        gc.fillPolygon(new double[]{0, 0, width}, new double[]{0, height, width}, 3);

        //Border
        gc.setGlobalAlpha(1.0);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);
        gc.strokeRect(0,0,width,height);
    }

    /**
     * Paint this canvas with the given colour
     * @param colour the colour to paint
     */
    private void paintColor(Paint colour) {
        var gc = getGraphicsContext2D();

        // Re-establish opacity
        gc.setGlobalAlpha(1.0);

        // Clear
        gc.clearRect(0,0,width,height);

        // Colour fill
        gc.setFill(colour);
        gc.fillRect(0,0, width, height);

        // Triangle
        gc.setGlobalAlpha(0.15);
        gc.setFill(Color.BLACK);
        gc.fillPolygon(new double[]{0, 0, width}, new double[]{0, height, width}, 3);

        // Border
        gc.setGlobalAlpha(0.5);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(0,0,width,height);

        // Paint circle onto currentPieceBoard
        if(PieceBoard.centreBlockProperty().equals(this)) {
            gc.setGlobalAlpha(0.95);
            gc.setFill(Color.BLACK);
            gc.fillOval(width/4.0, width/4.0, height/2.0, height/2.0);
        }
    }

    /**
     * Paint a whiteblock at half opacity on any block where mouse is hovering.
     */
    public void mouseHoverPaint() {
        var gc = getGraphicsContext2D();

        gc.setGlobalAlpha(0.28);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
    }

    /**
     * Reset paint after leaving block
     */
    public void resetPaint() {
        paintEmpty();
    }

    /**
     * If the value of block > 0, reestablish paint.
     * @param block block to paint over again
     */
    public void resetEstablishedPaint(GameBlock block) {
        paintColor(COLOURS[block.getValue()]);
    }

    /**
     * Implements AnimationTimer to fade away cleared blocks
     */
    public void fadeOut(GameBlock block) {
        opacity = 1.0;

        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                opacity -= 0.15;

                if (opacity <= 0) {
                    block.paintEmpty();
                    block.setOpacity(1.0);
                    stop();
                } else {
                    block.paintColor(COLOURS[block.getValue()]);
                    block.setOpacity(opacity);
                }
            }
        };

        animationTimer.start();
    }

    /**
     * Get the column of this block
     * @return column number
     */
    public int getX() {
        return x;
    }

    /**
     * Get the row of this block
     * @return row number
     */
    public int getY() {
        return y;
    }

    /**
     * Get the current value held by this block, representing its colour
     * @return value
     */
    public int getValue() {
        return this.value.get();
    }

    /**
     * Bind the value of this block to another property. Used to link the visual block to a corresponding block in the Grid.
     * @param input property to bind the value to
     */
    public void bind(ObservableValue<? extends Number> input) {
        value.bind(input);
    }

}
