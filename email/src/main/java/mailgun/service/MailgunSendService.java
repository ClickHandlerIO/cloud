package mailgun.service;

import com.google.common.util.concurrent.AbstractIdleService;
import common.service.FileService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.core.eventbus.EventBus;
import mailgun.config.MailgunConfig1;
import mailgun.data.MailgunSendRequest1;
import mailgun.handler.MailgunSendQueueHandler;

/**
 * @author Brad Behnke
 */
public class MailgunSendService extends AbstractIdleService {

    private final QueueService<MailgunSendRequest1> queueService;
    private final MailgunSendQueueHandler queueHandler;

    public MailgunSendService(MailgunConfig1 mailgunConfig, EventBus eventBus, SqlExecutor db, FileService fileService) {
        final QueueServiceConfig<MailgunSendRequest1> config = new QueueServiceConfig<>("MailgunSendQueue", MailgunSendRequest1.class, true, mailgunConfig.getSendParallelism(), mailgunConfig.getSendBatchSize());
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

    public void enqueue(MailgunSendRequest1 sendRequest) {
        this.queueService.add(sendRequest);
    }
}
