package event;

/**
 * Abstract object for standardizing creating and receiving events over the Vert.x EventBus
 *
 * @author Brad Behnke
 */
public abstract class AbstractVertxEvent {
    public final String eventAddress;

    public AbstractVertxEvent(String eventAddress) {
        this.eventAddress = eventAddress;
    }
}
