package sns.service;


import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.SqlDatabase;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sns.config.SNSConfig;
import sns.handler.SNSQueueHandler;
import sns.data.json.common.Message;
import sns.routing.email.SNSEmailNotifyRouteHandler;
import sns.routing.email.SNSEmailReceiveRouteHandler;
import sns.routing.general.SNSGeneralRouteHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Queue manager for all incoming SNS message processing.
 *
 * @author Brad Behnke
 */
@Singleton
public class SNSService extends AbstractIdleService {
    private final static Logger LOG = LoggerFactory.getLogger(SNSService.class);
    private SNSGeneralRouteHandler generalRouteHandler;
    private SNSEmailNotifyRouteHandler emailNotifyRouteHandler;
    private SNSEmailReceiveRouteHandler emailReceiveRouteHandler;
    private QueueService<Message> queueService;

    @Inject
    public SNSService(EventBus eventBus, SqlDatabase db) {

        // initialize sns queues
        QueueFactory factory = new LocalQueueServiceFactory();
        // main queue and handler
        final QueueServiceConfig<Message> mainConfig = new QueueServiceConfig<>(SNSConfig.getName(), Message.class, true, SNSConfig.getParallelism(), SNSConfig.getBatchSize());
        mainConfig.setHandler(new SNSQueueHandler(eventBus, db));
        this.queueService = factory.build(mainConfig);

        this.generalRouteHandler = new SNSGeneralRouteHandler(this);
        this.emailNotifyRouteHandler = new SNSEmailNotifyRouteHandler(this);
        this.emailReceiveRouteHandler = new SNSEmailReceiveRouteHandler(this);
    }

    @Override
    protected void startUp() throws Exception {
        // start sns queueing and handling
        this.queueService.startAsync();
        LOG.info("SNSService Started");
    }

    @Override
    protected void shutDown() throws Exception {
        // stop sns queueing and handling
        this.queueService.stopAsync();
        LOG.info("SNSService Shutdown");
    }

    public void enqueueMessage(Message message) {
        if(message == null) {
            return;
        }
        this.queueService.add(message);
    }

    public SNSEmailNotifyRouteHandler getEmailNotifyRouteHandler() {
        return emailNotifyRouteHandler;
    }

    public SNSEmailReceiveRouteHandler getEmailReceiveRouteHandler() {
        return emailReceiveRouteHandler;
    }

    public SNSGeneralRouteHandler getGeneralRouteHandler() {
        return generalRouteHandler;
    }
}
