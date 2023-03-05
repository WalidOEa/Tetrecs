package uk.ac.soton.comp1206.event;

/**
 * Used to send status updates to necessary classes about the condition of game. In this context, updates timer
 * with new duration.
 */
public interface GameLoopListener {

    /**
     * Updates timeline with new duration.
     * @param duration duration to update
     */
    void gameLooped(int duration);
}
