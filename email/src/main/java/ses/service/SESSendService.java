package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.rxjava.core.eventbus.EventBus;
import ses.config.SESConfig;
import ses.data.SESSendRequest;
import ses.handler.SESSendQueueHandler;

/**
 * SES email sending queue manager.
 *
 * @author Brad Behnke
 */
public class SESSendService extends AbstractIdleService {

    private final QueueService<SESSendRequest> queueService;
    private final SESSendQueueHandler queueHandler;

    public SESSendService(SESConfig sesConfig, EventBus eventBus, SqlExecutor db) {
        final QueueServiceConfig<SESSendRequest> config = new QueueServiceConfig<>("SESSendQueue", SESSendRequest.class, true, sesConfig.getSendParallelism(), sesConfig.getSendBatchSize());
        this.queueHandler = new SESSendQueueHandler(sesConfig, eventBus, db);
        config.setHandler(this.queueHandler);

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
        this.queueHandler.shutdown();
    }

    public void enqueue(SESSendRequest sendRequest) {
        queueService.add(sendRequest);
    }
}
