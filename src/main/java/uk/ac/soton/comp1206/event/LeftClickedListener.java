package uk.ac.soton.comp1206.event;

/**
 * Interface is used to register a left click on a board and rotating it within Game class
 */
public interface LeftClickedListener {

    /**
     * Handle a left click on board
     */
    void leftClicked();
}
