package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import common.service.FileAttachmentDownloadService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.rxjava.core.eventbus.EventBus;
import ses.config.SESConfig;
import ses.data.SESSendRequest;
import ses.handler.SESPrepQueueHandler;

/**
 * SES email sending prep queue manager.
 *
 * @author Brad Behnke
 */
public class SESSendPrepService extends AbstractIdleService {

    private final QueueService<SESSendRequest> queueService;

    public SESSendPrepService(SESConfig sesConfig, EventBus eventBus, SqlExecutor db, FileAttachmentDownloadService fileAttachmentDownloadService, SESSendService sesSendService) {
        final QueueServiceConfig<SESSendRequest> config = new QueueServiceConfig<>("SESPrepQueue", SESSendRequest.class, true, sesConfig.getPrepParallelism(), sesConfig.getPrepBatchSize());
        config.setHandler(new SESPrepQueueHandler(eventBus, db, fileAttachmentDownloadService, sesSendService));

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

    public void enqueue(SESSendRequest sendRequest) {
        this.queueService.add(sendRequest);
    }
}
