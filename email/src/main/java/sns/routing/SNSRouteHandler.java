package sns.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sns.json.SNSMessage;
import sns.service.SNSService;

/**
 * Created by admin on 1/22/16.
 */
public abstract class SNSRouteHandler<T extends SNSMessage> implements Handler<RoutingContext> {

    private final static Logger LOG = LoggerFactory.getLogger(SNSRouteHandler.class);
    protected final SNSService snsService;
    private final Class<T> messageClass;

    // SNS Header Keys
    protected static final String TYPE_HEADER = "x-amz-sns-message-type";
    protected static final String MESSAGE_ID_HEADER = "x-amz-sns-message-id";
    protected static final String TOPIC_ARN_HEADER = "x-amz-sns-topic-arn";
    protected static final String SUBSCRIPTION_ARN_HEADER = "x-amz-sns-subscription-arn";

    public SNSRouteHandler(SNSService snsService, Class<T> messageClass){
        this.snsService = snsService;
        this.messageClass = messageClass;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Abstracts
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public abstract void handle(RoutingContext routingContext);

    protected T getMessage(byte[] content) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(content, messageClass);
        } catch (Exception e) {
            LOG.error("Failed to process SNS Message", e);
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected HeaderInfo processHeaders(MultiMap headers) {
        HeaderInfo info = new HeaderInfo();
        if(headers == null) {
            return info;
        }
        info.setMessageType(headers.get(TYPE_HEADER));
        info.setMessageId(headers.get(MESSAGE_ID_HEADER));
        info.setTopicArn(headers.get(TOPIC_ARN_HEADER));
        info.setSubscriptionArn(headers.get(SUBSCRIPTION_ARN_HEADER));
        return info;
    }

    protected void passToService(SNSMessage message) {
        snsService.enqueueMessage(message);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected class HeaderInfo {
        private String messageType;
        private String messageId;
        private String topicArn;
        private String subscriptionArn;

        public HeaderInfo() {
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getSubscriptionArn() {
            return subscriptionArn;
        }

        public void setSubscriptionArn(String subscriptionArn) {
            this.subscriptionArn = subscriptionArn;
        }

        public String getTopicArn() {
            return topicArn;
        }

        public void setTopicArn(String topicArn) {
            this.topicArn = topicArn;
        }

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        public boolean isComplete() {
            return (messageType != null && !messageType.isEmpty()
                    && messageId != null && !messageId.isEmpty()
                    && topicArn != null && !topicArn.isEmpty()
                    && subscriptionArn != null && !subscriptionArn.isEmpty());
        }
    }
}
