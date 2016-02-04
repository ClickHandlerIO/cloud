package mailgun.config;

import common.config.EmailConfig;

/**
 * @author Brad Behnke
 */
public class MailgunConfig1 extends EmailConfig {

    private String domain;
    private String apiKey;
    private int sendParallelism = 2;
    private int sendBatchSize = 10;
    private int messageParallelism = 2;
    private int messageBatchSize = 10;
    private int sendRetryMax = 3;

    public MailgunConfig1 apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public MailgunConfig1 domain(String domain) {
        this.domain = domain;
        return this;
    }

    public MailgunConfig1 sendBatchSize(int sendBatchSize) {
        this.sendBatchSize = sendBatchSize;
        return this;
    }

    public MailgunConfig1 sendParallelism(int sendParallelism) {
        this.sendParallelism = sendParallelism;
        return this;
    }

    public MailgunConfig1 messageBatchSize(int messageBatchSize) {
        this.messageBatchSize = messageBatchSize;
        return this;
    }

    public MailgunConfig1 messageParallelism(int messageParallelism) {
        this.messageParallelism = messageParallelism;
        return this;
    }

    public MailgunConfig1 sendRetryMax(int sendRetryMax) {
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
