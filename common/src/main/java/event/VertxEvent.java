package event;

/**
 * Abstract object for standardizing creating and receiving events over the Vert.x EventBus
 *
 * @author Brad Behnke
 */
public abstract class VertxEvent {
    public final String eventAddress;

    public VertxEvent(String eventAddress) {
        this.eventAddress = eventAddress;
    }
}
