package sns.service;


import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sns.json.SNSMessage;
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
    private QueueService<SNSMessage> queueService;

    @Inject
    public SNSService(SNSQueueHandler mainQueueHandler) {

        // initialize sns queues
        QueueFactory factory = new LocalQueueServiceFactory();
        // main queue and handler
        final QueueServiceConfig<SNSMessage> mainConfig = new QueueServiceConfig<>("SNSQueue", SNSMessage.class, true, 2, 10);
        // TODO how to set handler?
//        mainConfig.setHandler(mainQueueHandler);
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

    public void enqueueMessage(SNSMessage message) {
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
