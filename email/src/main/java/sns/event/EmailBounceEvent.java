package sns.event;

import entity.SNSEmailMessageEntity;

/**
 * Created by admin on 1/25/16.
 */
public class EmailBounceEvent extends SNSEvent {

    private SNSEmailMessageEntity message;

    public EmailBounceEvent(SNSEmailMessageEntity message) {
        this.message = message;
    }

    public SNSEmailMessageEntity getMessage() {
        return message;
    }

    public void setMessage(SNSEmailMessageEntity message) {
        this.message = message;
    }
}
