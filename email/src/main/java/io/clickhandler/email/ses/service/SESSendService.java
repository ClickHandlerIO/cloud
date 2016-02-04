package io.clickhandler.email.ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.files.service.FileService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.core.eventbus.EventBus;
import io.clickhandler.email.ses.config.SESConfig;
import io.clickhandler.email.ses.data.MimeSendRequest;
import io.clickhandler.email.ses.handler.SESSendQueueHandler;

/**
 * SES email sending queue manager.
 *
 * @author Brad Behnke
 */
public class SESSendService extends AbstractIdleService {

    private final QueueService<MimeSendRequest> queueService;
    private final SESSendQueueHandler queueHandler;

    public SESSendService(SESConfig sesConfig, EventBus eventBus, SqlExecutor db, FileService fileService) {
        final QueueServiceConfig<MimeSendRequest> config = new QueueServiceConfig<>("SESSendQueue", MimeSendRequest.class, true, sesConfig.getSendParallelism(), sesConfig.getSendBatchSize());
        this.queueHandler = new SESSendQueueHandler(sesConfig, eventBus, db, fileService);
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

    public void enqueue(MimeSendRequest sendRequest) {
        queueService.add(sendRequest);
    }
}
