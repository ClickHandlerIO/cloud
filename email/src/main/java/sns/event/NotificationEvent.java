package sns.event;

import sns.json.SNSGeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class NotificationEvent extends SNSEvent {
    public static final String ADDRESS = "sns-notification";
    private SNSGeneralMessage message;

    public NotificationEvent() {
        super(ADDRESS);
    }

    public NotificationEvent(SNSGeneralMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public SNSGeneralMessage getMessage() {
        return message;
    }

    public void setMessage(SNSGeneralMessage message) {
        this.message = message;
    }
}
