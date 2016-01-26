package sns.event;

import sns.data.SNSGeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class SubscriptionEvent extends SNSEvent {
    private SNSGeneralMessage message;

    public SubscriptionEvent(SNSGeneralMessage message) {
        this.message = message;
    }

    public SNSGeneralMessage getMessage() {
        return message;
    }

    public void setMessage(SNSGeneralMessage message) {
        this.message = message;
    }
}
