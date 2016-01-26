package sns.event;

import sns.data.SNSEmailMessage;

/**
 * Created by admin on 1/25/16.
 */
public class EmailDeliveryEvent extends SNSEvent {

    private SNSEmailMessage message;

    public EmailDeliveryEvent(SNSEmailMessage message) {
        this.message = message;
    }

    public SNSEmailMessage getMessage() {
        return message;
    }

    public void setMessage(SNSEmailMessage message) {
        this.message = message;
    }
}
