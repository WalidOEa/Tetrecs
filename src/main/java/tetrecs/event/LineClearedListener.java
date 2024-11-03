package tetrecs.event;

import java.util.HashSet;

/**
 * Handles which lines to be cleared
 */
public interface LineClearedListener {

    /**
     * Lines to be cleared
     * @param coords coordinates of blocks to be cleared
     */
    void lineCleared(HashSet<int[]> coords);

}
