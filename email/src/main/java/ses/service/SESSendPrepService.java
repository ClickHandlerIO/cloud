package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlDatabase;
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

    public SESSendPrepService(EventBus eventBus, SqlDatabase db, SESAttachmentService SESAttachmentService, SESSendService sesSendService) {
        final QueueServiceConfig<SESSendRequest> config = new QueueServiceConfig<>("SESPrepQueue", SESSendRequest.class, true, SESConfig.getPrepParallelism(), SESConfig.getPrepBatchSize());
        config.setHandler(new SESPrepQueueHandler(eventBus, db, SESAttachmentService, sesSendService));

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
