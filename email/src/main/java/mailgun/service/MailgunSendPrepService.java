package mailgun.service;

import com.google.common.util.concurrent.AbstractIdleService;
import common.service.FileAttachmentDownloadService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.rxjava.core.eventbus.EventBus;
import mailgun.config.MailgunConfig;
import mailgun.data.MailgunSendRequest;
import mailgun.handler.MailgunSendPrepQueueHandler;

/**
 * @author Brad Behnke
 */
public class MailgunSendPrepService extends AbstractIdleService {

    private final QueueService<MailgunSendRequest> queueService;

    public MailgunSendPrepService(MailgunConfig mgConfig, EventBus eventBus, SqlExecutor db, FileAttachmentDownloadService fileAttachmentDownloadService, MailgunSendService sendService) {
        final QueueServiceConfig<MailgunSendRequest> config = new QueueServiceConfig<>("MailgunPrepQueue", MailgunSendRequest.class, true, mgConfig.getPrepParallelism(), mgConfig.getPrepBatchSize());
        config.setHandler(new MailgunSendPrepQueueHandler(eventBus, db, fileAttachmentDownloadService, sendService));

        QueueFactory factory = new LocalQueueServiceFactory();
        this.queueService = factory.build(config);
    }

    @Override
    protected void startUp() throws Exception {
        this.queueService.startAsync();
    }

    @Override
    protected void shutDown() throws Exception {
        this.queueService.stopAsync();
    }

    public void enqueue(MailgunSendRequest sendRequest) {
        this.queueService.add(sendRequest);
    }
}
