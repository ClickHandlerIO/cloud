package ses.event;

import common.EmailSendEvent;
import entity.EmailEntity;

/**
 * Created by admin on 1/26/16.
 */
public class SESSendEvent extends EmailSendEvent {
    public static final String ADDRESS = "ses-email-send";

    public SESSendEvent() {
        super(ADDRESS);
    }

    public SESSendEvent(EmailEntity emailEntity, boolean success) {
        super(ADDRESS, emailEntity, success);
    }
}
