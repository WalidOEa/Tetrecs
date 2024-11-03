package tetrecs.event;

/**
 * The Communications Listener is used for listening to messages received by the communicator.
 */
@Deprecated
public interface CommunicationsListener {

    /**
     * Handle an incoming message received by the Communicator
     * @param communication the message that was received
     */
    public void receiveCommunication(String communication);
}
