package sns.event;

import sns.json.GeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class NotificationEvent extends SNSEvent {
    public static final String ADDRESS = "sns-notification";
    private GeneralMessage message;

    public NotificationEvent() {
        super(ADDRESS);
    }

    public NotificationEvent(GeneralMessage message) {
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
