package ses.service;

import common.AbstractEmailService;
import common.AbstractSendRequest;
import io.clickhandler.sql.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s3.service.S3Service;
import ses.data.SESSendRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by admin on 1/26/16.
 */
@Singleton
public class SESService extends AbstractEmailService {
    private final static Logger LOG = LoggerFactory.getLogger(SESService.class);

    // ses services
    private final SESSendPrepService sesSendPrepService;
    private final SESAttachmentService SESAttachmentService;
    private final SESSendService sesSendService;

    @Inject
    public SESService(Database db, S3Service s3Service) {
        this.SESAttachmentService = new SESAttachmentService(db, s3Service);
        this.sesSendService = new SESSendService(db);
        this.sesSendPrepService = new SESSendPrepService(db, SESAttachmentService, sesSendService);
    }

    @Override
    protected void startUp() throws Exception {
        this.sesSendService.startAsync();
        this.SESAttachmentService.startAsync();
        this.sesSendPrepService.startAsync();
        LOG.info("SES Service Started");
    }

    @Override
    protected void shutDown() throws Exception {
        this.sesSendPrepService.stopAsync();
        this.SESAttachmentService.stopAsync();
        this.sesSendService.stopAsync();
        LOG.info("SES Service Shutdown");
    }

    @Override
    public void send(AbstractSendRequest sendRequest) {
        if(sendRequest instanceof SESSendRequest) {
            SESSendRequest request = (SESSendRequest) sendRequest;
            if(sendRequest.getEmailEntity() == null) {
                sendRequest.getSendHandler().onFailure(new Exception("Null email entity."));
                return;
            }
            if(sendRequest.getEmailEntity().getId() == null || sendRequest.getEmailEntity().getId().isEmpty()) {
                sendRequest.getSendHandler().onFailure(new Exception("Null or empty email  Id."));
                return;
            }
            this.sesSendPrepService.enqueue(request);
        }
    }
}
