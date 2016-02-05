package io.clickhandler.email.mailgun.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.files.service.FileService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.SqlExecutor;
import io.clickhandler.email.mailgun.config.MailgunConfig;
import io.clickhandler.email.mailgun.data.MailgunSendRequest;
import io.clickhandler.email.mailgun.handler.MailgunSendQueueHandler;
import io.vertx.rxjava.core.eventbus.EventBus;

/**
 * @author Brad Behnke
 */
public class MailgunSendService extends AbstractIdleService {

    private final QueueService<MailgunSendRequest> queueService;
    private final MailgunSendQueueHandler queueHandler;

    public MailgunSendService(MailgunConfig mailgunConfig, EventBus eventBus, SqlExecutor db, FileService fileService) {
        final QueueServiceConfig<MailgunSendRequest> config = new QueueServiceConfig<>("MailgunSendQueue", MailgunSendRequest.class, true, mailgunConfig.getSendParallelism(), mailgunConfig.getSendBatchSize());
        this.queueHandler = new MailgunSendQueueHandler(mailgunConfig, eventBus, db, fileService);
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

    public void enqueue(MailgunSendRequest sendRequest) {
        this.queueService.add(sendRequest);
    }
}
