package mailgun.service;

import com.google.common.util.concurrent.AbstractIdleService;
import com.sun.istack.internal.NotNull;
import common.data.Message;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.core.eventbus.EventBus;
import mailgun.config.MailgunConfig;
import mailgun.handler.MailgunMessageQueueHandler;
/**
 * @author Brad Behnke
 */
public class MailgunMessageService extends AbstractIdleService {
    private QueueService<Message> queueService;

    public MailgunMessageService(@NotNull MailgunConfig config, @NotNull EventBus eventBus, @NotNull SqlExecutor db) {
        // initialize sns queues
        QueueFactory factory = new LocalQueueServiceFactory();
        // main queue and handler
        final QueueServiceConfig<Message> mainConfig = new QueueServiceConfig<>("MailgunMessageQueue", Message.class, true, config.getMessageParallelism(), config.getMessageBatchSize());
        mainConfig.setHandler(new MailgunMessageQueueHandler(eventBus, db));
        this.queueService = factory.build(mainConfig);
    }

    @Override
    protected void startUp() throws Exception {
        // start queueing and handling
        this.queueService.startAsync();
    }

    @Override
    protected void shutDown() throws Exception {
        // stop queueing and handling
        this.queueService.stopAsync();
    }

    public void enqueueMessage(Message message) {
        if(message == null) {
            return;
        }
        this.queueService.add(message);
    }
}
