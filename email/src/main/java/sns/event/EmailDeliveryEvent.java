package sns.event;

import sns.json.SNSEmailMessage;

/**
 * Created by admin on 1/25/16.
 */
public class EmailDeliveryEvent extends SNSEvent {
    public static final String ADDRESS = "sns-email-delivery";
    private SNSEmailMessage message;

    public EmailDeliveryEvent() {
        super(ADDRESS);
    }

    public EmailDeliveryEvent(SNSEmailMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public SNSEmailMessage getMessage() {
        return message;
    }

    public void setMessage(SNSEmailMessage message) {
        this.message = message;
    }
}
