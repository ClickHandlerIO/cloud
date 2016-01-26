package sns.event;

import entity.SNSGeneralMessageEntity;

/**
 * Created by admin on 1/25/16.
 */
public class UnsubscribeEvent extends SNSEvent {
    private SNSGeneralMessageEntity message;

    public UnsubscribeEvent(SNSGeneralMessageEntity message) {
        this.message = message;
    }

    public SNSGeneralMessageEntity getMessage() {
        return message;
    }

    public void setMessage(SNSGeneralMessageEntity message) {
        this.message = message;
    }
}
