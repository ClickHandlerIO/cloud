package io.clickhandler.email.sns.service;


import com.google.common.util.concurrent.AbstractIdleService;
import com.sun.istack.internal.NotNull;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.clickhandler.email.sns.config.SNSConfig;
import io.clickhandler.email.sns.handler.SNSQueueHandler;
import io.clickhandler.email.common.data.Message;
import io.clickhandler.email.sns.routing.email.SNSEmailNotifyRouteHandler;
import io.clickhandler.email.sns.routing.email.SNSEmailReceiveRouteHandler;
import io.clickhandler.email.sns.routing.general.SNSGeneralRouteHandler;

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
    public SNSService(@NotNull SNSConfig snsConfig, @NotNull EventBus eventBus, @NotNull SqlExecutor db) {

        // initialize io.clickhandler.email.sns queues
        QueueFactory factory = new LocalQueueServiceFactory();
        // main queue and handler
        final QueueServiceConfig<Message> mainConfig = new QueueServiceConfig<>(snsConfig.getName(), Message.class, true, snsConfig.getParallelism(), snsConfig.getBatchSize());
        mainConfig.setHandler(new SNSQueueHandler(snsConfig, eventBus, db));
        this.queueService = factory.build(mainConfig);

        this.generalRouteHandler = new SNSGeneralRouteHandler(this);
        this.emailNotifyRouteHandler = new SNSEmailNotifyRouteHandler(this);
        this.emailReceiveRouteHandler = new SNSEmailReceiveRouteHandler(this);
    }

    @Override
    protected void startUp() throws Exception {
        // start io.clickhandler.email.sns queueing and handling
        this.queueService.startAsync();
        LOG.info("SNSService Started");
    }

    @Override
    protected void shutDown() throws Exception {
        // stop io.clickhandler.email.sns queueing and handling
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
