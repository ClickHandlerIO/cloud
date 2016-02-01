package mailgun.config;

import common.config.EmailConfig;

/**
 * @author Brad Behnke
 */
public class MailgunConfig extends EmailConfig {

    private String domain;
    private String apiKey;
    private int sendParallelism = 2;
    private int sendBatchSize = 10;
    private int messageParallelism = 2;
    private int messageBatchSize = 10;
    private int sendRetryMax = 3;

    public MailgunConfig apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public MailgunConfig domain(String domain) {
        this.domain = domain;
        return this;
    }

    public MailgunConfig sendBatchSize(int sendBatchSize) {
        this.sendBatchSize = sendBatchSize;
        return this;
    }

    public MailgunConfig sendParallelism(int sendParallelism) {
        this.sendParallelism = sendParallelism;
        return this;
    }

    public MailgunConfig messageBatchSize(int messageBatchSize) {
        this.messageBatchSize = messageBatchSize;
        return this;
    }

    public MailgunConfig messageParallelism(int messageParallelism) {
        this.messageParallelism = messageParallelism;
        return this;
    }

    public MailgunConfig sendRetryMax(int sendRetryMax) {
        this.sendRetryMax = sendRetryMax;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getDomain() {
        return domain;
    }

    public int getSendBatchSize() {
        return sendBatchSize;
    }

    public int getSendParallelism() {
        return sendParallelism;
    }

    public int getMessageBatchSize() {
        return messageBatchSize;
    }

    public int getMessageParallelism() {
        return messageParallelism;
    }

    public int getSendRetryMax() {
        return sendRetryMax;
    }
}
