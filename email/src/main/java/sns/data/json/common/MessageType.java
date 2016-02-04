package sns.data.json.common;

/**
 * SNS JSON object types
 *
 * @author Brad Behnke
 */
public class MessageType {
    public final static String SUB_CONFIRM = "SubscriptionConfirmation";
    public final static String UNSUB_CONFIRM = "UnsubscribeConfirmation";
    public final static String NOTIFICATION = "Notification";
    public final static String DELIVERY = "Delivery";
    public final static String BOUNCE = "Bounce";
    public final static String COMPLAINT = "Complaint";
    public final static String UNKNOWN = "Unknown";
    public final static String RECEIVED = "Received";
    
    public static Type getTypeEnum(String in) {
        switch (in) {
            case SUB_CONFIRM:
                return Type.SUB_CONFIRM;
            case UNSUB_CONFIRM:
                return Type.UNSUB_CONFIRM;
            case NOTIFICATION:
                return Type.NOTIFICATION;
            case DELIVERY:
                return Type.DELIVERY;
            case BOUNCE:
                return Type.BOUNCE;
            case COMPLAINT:
                return Type.COMPLAINT;
            case RECEIVED:
                return Type.RECEIVED;
            default:
                return Type.UNKNOWN;
        }
    }
    
    private static String getTypeString(Type in) {
        switch (in) {
            case SUB_CONFIRM:
                return SUB_CONFIRM;
            case UNSUB_CONFIRM:
                return UNSUB_CONFIRM;
            case NOTIFICATION:
                return NOTIFICATION;
            case DELIVERY:
                return DELIVERY;
            case BOUNCE:
                return BOUNCE;
            case COMPLAINT:
                return COMPLAINT;
            case RECEIVED:
                return RECEIVED;
            default:
                return UNKNOWN;
        }
    }
    
    public enum Type {
        SUB_CONFIRM,
        UNSUB_CONFIRM,
        NOTIFICATION,
        DELIVERY,
        BOUNCE,
        COMPLAINT,
        RECEIVED,
        UNKNOWN
    }
}