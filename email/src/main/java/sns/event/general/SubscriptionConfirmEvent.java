package sns.event.general;

import sns.event.common.SNSEvent;
import sns.json.general.GeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class SubscriptionConfirmEvent extends SNSEvent {
    public static final String ADDRESS = "sns-subscribe";
    private GeneralMessage message;

    public SubscriptionConfirmEvent() {
        super(ADDRESS);
    }

    public SubscriptionConfirmEvent(GeneralMessage message) {
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
