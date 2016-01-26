package sns.event;

import entity.SNSEmailMessageEntity;

/**
 * Created by admin on 1/25/16.
 */
public class EmailComplaintEvent extends SNSEvent {

    private SNSEmailMessageEntity message;

    public EmailComplaintEvent(SNSEmailMessageEntity message) {
        this.message = message;
    }

    public SNSEmailMessageEntity getMessage() {
        return message;
    }

    public void setMessage(SNSEmailMessageEntity message) {
        this.message = message;
    }
}
