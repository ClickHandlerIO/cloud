package sns.event;

import sns.json.SNSEmailMessage;

/**
 * Created by admin on 1/25/16.
 */
public class EmailBounceEvent extends SNSEvent {
    public static final String ADDRESS = "sns-email-bounce";
    private SNSEmailMessage message;

    public EmailBounceEvent() {
        super(ADDRESS);
    }

    public EmailBounceEvent(SNSEmailMessage message) {
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
