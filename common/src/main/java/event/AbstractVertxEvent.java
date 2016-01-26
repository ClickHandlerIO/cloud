package event;

/**
 * Created by admin on 1/26/16.
 */
public abstract class AbstractVertxEvent {
    public final String eventAddress;

    public AbstractVertxEvent(String eventAddress) {
        this.eventAddress = eventAddress;
    }
}
