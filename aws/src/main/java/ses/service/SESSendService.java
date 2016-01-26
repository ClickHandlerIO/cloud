package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.queue.LocalQueueServiceFactory;
import io.clickhandler.queue.QueueFactory;
import io.clickhandler.queue.QueueService;
import io.clickhandler.queue.QueueServiceConfig;
import io.clickhandler.sql.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ses.handler.SESSendQueueHandler;

import javax.mail.internet.MimeMessage;

/**
 * Initializes/Kills queue and handler for emails to be sent using SES.
 *
 * @author Brad Behnke
 */
public class SESSendService extends AbstractIdleService {
    private final static Logger LOG = LoggerFactory.getLogger(SESSendService.class);

    private final QueueService<Message> queueService;
    private final SESSendQueueHandler queueHandler;

    public SESSendService(Database db) {
        final QueueServiceConfig<Message> config = new QueueServiceConfig<>("SESSendQueue", Message.class, true, 2, 10);
        this.queueHandler = new SESSendQueueHandler(db);
        config.setHandler(this.queueHandler);

        QueueFactory factory = new LocalQueueServiceFactory();
        this.queueService = factory.build(config);
    }

    @Override
    protected void startUp() throws Exception {
        this.queueService.startAsync();
        LOG.info("SES Send Service Started");
    }

    @Override
    protected void shutDown() throws Exception {
        this.queueService.stopAsync();
        this.queueHandler.shutdown();
        LOG.info("SES Send Service Shutdown");
    }

    public void enqueue(String emailId, MimeMessage message) {
        enqueue(new Message(emailId,message));
    }

    public void enqueue(Message message) {
        queueService.add(message);
    }

    public class Message {
        private String emailId;
        private MimeMessage mimeMessage;
        private int attempts;

        public Message(String emailId, MimeMessage mimeMessage) {
            this.emailId = emailId;
            this.mimeMessage = mimeMessage;
        }

        public String getEmailId() {
            return emailId;
        }

        public void setEmailId(String emailId) {
            this.emailId = emailId;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public MimeMessage getMimeMessage() {
            return mimeMessage;
        }

        public void setMimeMessage(MimeMessage mimeMessage) {
            this.mimeMessage = mimeMessage;
        }

        public void incrementAttempts() {
            this.attempts++;
        }
    }
}
