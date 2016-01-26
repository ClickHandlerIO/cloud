package sns.routing;

import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sns.json.SNSGeneralMessage;
import sns.service.SNSService;

import java.io.InputStream;
import java.net.URL;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by admin on 1/22/16.
 */

public class SNSGeneralRouteHandler extends SNSRouteHandler<SNSGeneralMessage> {
    private final static Logger LOG = LoggerFactory.getLogger(SNSGeneralRouteHandler.class);

    public SNSGeneralRouteHandler(SNSService snsService) {
        super(snsService, SNSGeneralMessage.class);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        // get header info
        HttpServerRequest request = routingContext.request();
        if(!request.method().equals(HttpMethod.POST)) {
            LOG.error("Invalid HttpMethod Caught: " + request.method().toString());
            routingContext.fail(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        HeaderInfo headerInfo = processHeaders(request.headers());
        if (!headerInfo.isComplete()) {
            routingContext.fail(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        // get message from body json
        SNSGeneralMessage message = getMessage(routingContext.getBody().toString());
        if (message == null || !isMessageSignatureValid(message)) {
            routingContext.fail(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        passToService(message);
        request.response().setStatusCode(HttpStatus.SC_OK).end();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Message Validation
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean isMessageSignatureValid(SNSGeneralMessage message) {
        try {
            URL url = new URL(message.getSigningCertURL());
            InputStream inStream = url.openStream();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
            inStream.close();

            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(cert.getPublicKey());
            sig.update(getMessageBytesToSign(message));
            return sig.verify(Base64.decodeBase64(message.getSignature()));
        } catch (Exception e) {
            throw new SecurityException("Verify method failed.", e);
        }
    }

    private static byte [] getMessageBytesToSign (SNSGeneralMessage message) {
        byte [] bytesToSign = null;
        if (message.getType().equals("Notification"))
            bytesToSign = buildNotificationStringToSign(message).getBytes();
        else if (message.getType().equals("SubscriptionConfirmation") || message.getType().equals("UnsubscribeConfirmation"))
            bytesToSign = buildSubscriptionStringToSign(message).getBytes();
        return bytesToSign;
    }

    //Build the string to sign for Notification messages.
    public static String buildNotificationStringToSign(SNSGeneralMessage message) {
        String stringToSign = null;

        //Build the string to sign from the values in the message.
        //Name and values separated by newline characters
        //The name value pairs are sorted by name
        //in byte sort order.
        stringToSign = "Message\n";
        stringToSign += message.getMessage() + "\n";
        stringToSign += "MessageId\n";
        stringToSign += message.getMessageId() + "\n";
        if (message.getSubject() != null) {
            stringToSign += "Subject\n";
            stringToSign += message.getSubject() + "\n";
        }
        stringToSign += "Timestamp\n";
        stringToSign += message.getTimestamp() + "\n";
        stringToSign += "TopicArn\n";
        stringToSign += message.getTopicArn() + "\n";
        stringToSign += "Type\n";
        stringToSign += message.getType() + "\n";
        return stringToSign;
    }

    //Build the string to sign for SubscriptionConfirmation
    //and UnsubscribeConfirmation messages.
    public static String buildSubscriptionStringToSign(SNSGeneralMessage message) {
        String stringToSign = null;
        //Build the string to sign from the values in the message.
        //Name and values separated by newline characters
        //The name value pairs are sorted by name
        //in byte sort order.
        stringToSign = "Message\n";
        stringToSign += message.getMessage() + "\n";
        stringToSign += "MessageId\n";
        stringToSign += message.getMessageId() + "\n";
        stringToSign += "SubscribeURL\n";
        stringToSign += message.getSubscribeURL() + "\n";
        stringToSign += "Timestamp\n";
        stringToSign += message.getTimestamp() + "\n";
        stringToSign += "Token\n";
        stringToSign += message.getToken() + "\n";
        stringToSign += "TopicArn\n";
        stringToSign += message.getTopicArn() + "\n";
        stringToSign += "Type\n";
        stringToSign += message.getType() + "\n";
        return stringToSign;
    }
}
