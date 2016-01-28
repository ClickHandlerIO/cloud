package common.config;

/**
 * Created by admin on 1/28/16.
 */
public class EmailConfig {
    private int attachmentParallelism = 2;
    private int attachmentBatchSize = 10;

    public int getAttachmentBatchSize() {
        return attachmentBatchSize;
    }

    public EmailConfig attachmentBatchSize(int attachmentBatchSize) {
        this.attachmentBatchSize = attachmentBatchSize;
        return this;
    }

    public int getAttachmentParallelism() {
        return attachmentParallelism;
    }

    public EmailConfig attachmentParallelism(int attachmentParallelism) {
        this.attachmentParallelism = attachmentParallelism;
        return this;
    }
}
