package io.clickhandler.email.mailgun.routing;

import io.vertx.core.Handler;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.clickhandler.email.mailgun.config.MailgunConfig;
import io.clickhandler.email.mailgun.service.MailgunService;
import io.clickhandler.email.common.data.Message;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Abstract Vertx route for capturing Amazon SNS messages.
 *
 * @see io.vertx.rxjava.ext.web.Route
 * @author Brad Behnke
 */
public abstract class MailgunRouteHandler<T extends Message> implements Handler<RoutingContext> {

    private final MailgunService mailgunService;
    private final String apiKey;

    public MailgunRouteHandler(MailgunConfig config, MailgunService mailgunService){
        this.mailgunService = mailgunService;
        this.apiKey = config.getApiKey();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Abstracts
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public abstract void handle(RoutingContext routingContext);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void verifyRequest(HttpServerRequest request, VerifyCallback callback) {
        if(callback == null) {
            return;
        }
        if(request == null) {
            callback.isInvalid();
            return;
        }
        String timestamp = request.params().get("timestamp");
        String token = request.params().get("token");
        String signature = request.params().get("signature");
        if(timestamp == null || timestamp.isEmpty()
                || token == null || token.isEmpty()
                || signature == null || signature.isEmpty()) {
            callback.isInvalid();
            return;
        }
        try {
            String timeTokConcat = timestamp + token;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(apiKey.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(timeTokConcat.getBytes()));
            if(hash.equals(signature)) {
                callback.isValid();
                return;
            }
            callback.isInvalid();
        } catch (Exception e) {
            callback.isInvalid();
        }
    }

    protected void passToService(T message) {
        mailgunService.enqueueMessage(message);
    }

    protected abstract void buildMessage(HttpServerRequest request, BuildCallback<T> callback);

    protected interface VerifyCallback {
        void isValid();
        void isInvalid();
    }

    protected interface BuildCallback<T extends Message> {
        void success(T message);
        void failure(Throwable throwable);
    }
}
