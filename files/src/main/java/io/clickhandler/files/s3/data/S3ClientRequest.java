package io.clickhandler.files.s3.data;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 *  Code from https://github.com/spartango/SuperS3t
 *
 *  Request object for S3 http communication.
 */

public class S3ClientRequest {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private static final Logger logger = LoggerFactory.getLogger(S3ClientRequest.class);

    private final HttpClientRequest request;

    // These are actually set when the request is created, but we need to know
    private final String method;
    private final String bucket;
    private final String key;

    // These are totally optional
    private String contentMd5;
    private String contentType;

    // Used for authentication(which may be optional depending on the bucket)
    private String awsAccessKey;
    private String awsSecretKey;

    public S3ClientRequest(String method,
                           String bucket,
                           String key,
                           HttpClientRequest request) {
        this(method, bucket, key, request, null, null);
    }

    public S3ClientRequest(String method,
                           String bucket,
                           String key,
                           HttpClientRequest request,
                           String awsAccessKey,
                           String awsSecretKey) {
        this(method, bucket, key, request, awsAccessKey, awsSecretKey, "", "");
    }

    public S3ClientRequest(String method,
                           String bucket,
                           String key,
                           HttpClientRequest request,
                           String awsAccessKey,
                           String awsSecretKey,
                           String contentMd5,
                           String contentType) {
        this.method = method;
        this.bucket = bucket;
        this.key = key;
        this.request = request;
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.contentMd5 = contentMd5;
        this.contentType = contentType;
    }

    public HttpClientRequest setWriteQueueMaxSize(int maxSize) {
        return request.setWriteQueueMaxSize(maxSize);
    }

    public HttpClientRequest handler(Handler<HttpClientResponse> handler) {
        return request.handler(handler);
    }

    public boolean writeQueueFull() {
        return request.writeQueueFull();
    }

    public HttpClientRequest drainHandler(Handler<Void> handler) {
        return request.drainHandler(handler);
    }

    public HttpClientRequest exceptionHandler(Handler<Throwable> handler) {
        return request.exceptionHandler(handler);
    }

    public HttpClientRequest setChunked(boolean chunked) {
        return request.setChunked(chunked);
    }

    public MultiMap headers() { return request.headers(); }

    public HttpClientRequest pause() { return request.pause(); }

    public HttpClientRequest resume() { return request.resume();}

    public HttpClientRequest endHandler(Handler<Void> endHandler) { return request.endHandler(endHandler);}

    public boolean isChunked() { return request.isChunked(); }

    public HttpMethod method() {
        return request.method();
    }

    public String uri() {
        return request.uri();
    }

    public HttpClientRequest putHeader(String name, String value) {
        return request.putHeader(name, value);
    }

    public HttpClientRequest putHeader(CharSequence name, CharSequence value) {
        return request.putHeader(name.toString(), value.toString());
    }

//    public HttpClientRequest putHeader(String name, Iterable<String> values) {
//        return request.putHeader(name, values);
//    }
//
//    public HttpClientRequest putHeader(CharSequence name, Iterable<CharSequence> values) {
//        return request.putHeader(name, values);
//    }

    public HttpClientRequest setTimeout(long timeoutMs) {
        return request.setTimeout(timeoutMs);
    }

    public HttpClientRequest write(Buffer chunk) {
        return request.write(chunk);
    }

    public HttpClientRequest write(String chunk) {
        return request.write(chunk);
    }

    public HttpClientRequest write(String chunk, String enc) {
        return request.write(chunk, enc);
    }

    public HttpClientRequest continueHandler(Handler<Void> handler) {
        return request.continueHandler(handler);
    }

    public HttpClientRequest sendHead() {
        // Generate authentication header
        initAuthenticationHeader();
        // Send the header
        return request.sendHead();
    }

    public void end(String chunk) {
        // Generate authentication header
        initAuthenticationHeader();
        request.end(chunk);
    }

    public void end(String chunk, String enc) {
        // Generate authentication header
        initAuthenticationHeader();
        request.end(chunk, enc);
    }

    public void end(Buffer chunk) {
        // Generate authentication header
        initAuthenticationHeader();
        request.end(chunk);
    }

    public void end() {
        // Generate authentication header
        initAuthenticationHeader();
        request.end();
    }

    protected void initAuthenticationHeader() {
        if (isAuthenticated()) {
            // Calculate the signature
            // http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html#ConstructingTheAuthenticationHeader

            // Date should look like Thu, 17 Nov 2005 18:49:58 GMT, and must be
            // within 15 min of S3 server time.
            // contentMd5 and type are optional

            // We can't risk letting our date get clobbered and being
            // inconsistent
            String xamzdate = currentDateString();
            headers().add("X-Amz-Date", xamzdate);

            String canonicalizedAmzHeaders = "x-amz-date:" + xamzdate + "\n";
            String canonicalizedResource = "/" + bucket + "/" + key;

            String toSign = method
                    + "\n"
                    + contentMd5
                    + "\n"
                    + contentType
                    + "\n\n" // Skipping the date, we'll use the x-amz
                    // date instead
                    + canonicalizedAmzHeaders
                    + canonicalizedResource;

            String signature;
            try {
                signature = b64SignHmacSha1(awsSecretKey, toSign);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                signature = "ERRORSIGNATURE";
                // This will totally fail,
                // but downstream users can handle it
                logger.error("Failed to sign S3 request due to " + e);
            }
            String authorization = "AWS" + " " + awsAccessKey + ":" + signature;

            // Put that nasty auth string in the headers and let vert.x deal
            headers().add("Authorization", authorization);
        }
        // Otherwise not needed
    }

    public HttpClientRequest getRequest() {
        return request;
    }

    public boolean isAuthenticated() {
        return awsAccessKey != null && awsSecretKey != null;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getMethod() {
        return method;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    private static String
    b64SignHmacSha1(String awsSecretKey, String canonicalString) throws NoSuchAlgorithmException,
            InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(awsSecretKey.getBytes(),
                "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        return new String(Base64.getEncoder().encode(mac.doFinal(canonicalString.getBytes())));
    }

    private static String currentDateString() {
        return dateFormat.format(new Date());
    }
}
