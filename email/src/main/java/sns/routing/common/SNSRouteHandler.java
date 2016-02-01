package sns.routing.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;

import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.RoutingContext;
import common.data.Message;
import org.apache.http.HttpStatus;
import sns.service.SNSService;

/**
 * Abstract Vertx route for capturing Amazon SNS messages.
 *
 * @see io.vertx.rxjava.ext.web.Route
 * @author Brad Behnke
 */
public abstract class SNSRouteHandler<T extends Message> implements Handler<RoutingContext> {

    protected final SNSService snsService;
    private final Class<T> messageClass;
    private final static ObjectMapper jsonMapper = new ObjectMapper();

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

    protected T getMessage(String content) {
        try {
            return jsonMapper.readValue(content, messageClass);
        } catch (Exception e) {
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

    protected void passToService(Message message) {
        snsService.enqueueMessage(message);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Validation
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // todo

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

    protected class MessageBuilder implements BuildMessageHandler {
        private Buffer buffer;
        private RoutingContext routingContext;

        public MessageBuilder(RoutingContext routingContext) {
            this.buffer = Buffer.buffer();
            this.routingContext = routingContext;
        }

        @Override
        public void chunkReceived(Buffer chunk) {
            this.buffer.appendBuffer(chunk);
        }

        @Override
        public void onComplete() {
            T message = getMessage(buffer.toString());
            if (message == null /*|| !isMessageSignatureValid(message)*/) {
                routingContext.request().response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
                return;
            }
            passToService(message);
            routingContext.request().response().setStatusCode(HttpStatus.SC_OK).end();
        }

        @Override
        public void onFailure() {
            routingContext.request().response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
        }
    }

    protected interface BuildMessageHandler {
        void chunkReceived(Buffer chunk);
        void onComplete();
        void onFailure();
    }
}
