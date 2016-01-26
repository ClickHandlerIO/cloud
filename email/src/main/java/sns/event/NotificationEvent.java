package sns.event;

import sns.data.SNSGeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class NotificationEvent extends SNSEvent {
    private SNSGeneralMessage message;

    public NotificationEvent(SNSGeneralMessage message) {
        this.message = message;
    }

    public SNSGeneralMessage getMessage() {
        return message;
    }

    public void setMessage(SNSGeneralMessage message) {
        this.message = message;
    }
}
