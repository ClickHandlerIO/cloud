package sns.event.general;

import sns.event.common.SNSEvent;
import sns.json.general.GeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class UnsubscribeConfirmEvent extends SNSEvent {
    public static final String ADDRESS = "sns-unsubscribe";
    private GeneralMessage message;

    public UnsubscribeConfirmEvent() {
        super(ADDRESS);
    }

    public UnsubscribeConfirmEvent(GeneralMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public GeneralMessage getMessage() {
        return message;
    }

    public void setMessage(GeneralMessage message) {
        this.message = message;
    }
}
