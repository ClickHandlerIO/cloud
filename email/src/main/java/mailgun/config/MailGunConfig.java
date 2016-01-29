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
    private int prepParallelism = 2;
    private int prepBatchSize = 10;
    private int sendRetryMax = 3;

    public MailgunConfig apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public MailgunConfig domain(String domain) {
        this.domain = domain;
        return this;
    }

    public MailgunConfig prepBatchSize(int prepBatchSize) {
        this.prepBatchSize = prepBatchSize;
        return this;
    }

    public MailgunConfig prepParallelism(int prepParallelism) {
        this.prepParallelism = prepParallelism;
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

    public int getPrepBatchSize() {
        return prepBatchSize;
    }

    public int getPrepParallelism() {
        return prepParallelism;
    }

    public int getSendBatchSize() {
        return sendBatchSize;
    }

    public int getSendParallelism() {
        return sendParallelism;
    }

    public int getSendRetryMax() {
        return sendRetryMax;
    }
}
