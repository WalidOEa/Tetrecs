package tetrecs.event;

import tetrecs.game.GamePiece;

/**
 * CurrentPieceListener is used to listen to pieces generated by Game for UI manipulation.
 */
public interface CurrentPieceListener {

    /**
     * Handle a current piece received by Game.
     * @param currentGamePiece current piece in game
     */
    void currentPiece(GamePiece currentGamePiece);
}
