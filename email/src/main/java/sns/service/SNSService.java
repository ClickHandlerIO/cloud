package sns.service;


import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sns.config.SNSConfig;
import entity.SNSMessageEntity;
import sns.handler.SNSQueueHandler;
import sns.routing.SNSEmailRouteHandler;
import sns.routing.SNSGeneralRouteHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Brad on 1/20/16.
 */
@Singleton
public class SNSService extends AbstractIdleService {
    private final static Logger LOG = LoggerFactory.getLogger(SNSService.class);
    private SNSGeneralRouteHandler generalRouteHandler;
    private SNSEmailRouteHandler emailRouteHandler;
    private QueueService<SNSMessageEntity> queueService;

    @Inject
    public SNSService(EventBus eventBus, Database db) {

        // initialize sns queues
        QueueFactory factory = new LocalQueueServiceFactory();
        // main queue and handler
        final QueueServiceConfig<SNSMessageEntity> mainConfig = new QueueServiceConfig<>(SNSConfig.getName(), SNSMessageEntity.class, true, SNSConfig.getParallelism(), SNSConfig.getBatchSize());
        mainConfig.setHandler(new SNSQueueHandler(eventBus, db));
        this.queueService = factory.build(mainConfig);

        this.generalRouteHandler = new SNSGeneralRouteHandler(this);
        this.emailRouteHandler = new SNSEmailRouteHandler(this);
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

    public void enqueueMessage(SNSMessageEntity message) {
        if(message == null) {
            return;
        }
        this.queueService.add(message);
    }

    public SNSEmailRouteHandler getEmailRouteHandler() {
        return emailRouteHandler;
    }

    public SNSGeneralRouteHandler getGeneralRouteHandler() {
        return generalRouteHandler;
    }
}
