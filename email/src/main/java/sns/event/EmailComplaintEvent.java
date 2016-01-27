package sns.event;

import sns.json.SNSEmailMessage;

/**
 * Created by admin on 1/25/16.
 */
public class EmailComplaintEvent extends SNSEvent {
    public static final String ADDRESS = "sns-email-complaint";
    private SNSEmailMessage message;

    public EmailComplaintEvent() {
        super(ADDRESS);
    }

    public EmailComplaintEvent(SNSEmailMessage message) {
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
