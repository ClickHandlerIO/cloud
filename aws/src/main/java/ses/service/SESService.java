package ses.service;

import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.sql.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by admin on 1/26/16.
 */
@Singleton
public class SESService extends AbstractIdleService {
    private final static Logger LOG = LoggerFactory.getLogger(SESService.class);

    // ses services
    private final SESSendPrepService sesSendPrepService;
    private final AttachmentService attachmentService;
    private final SESSendService sesSendService;

    @Inject
    public SESService(Database db) {
        this.attachmentService = new AttachmentService(db);
        this.sesSendService = new SESSendService(db);
        this.sesSendPrepService = new SESSendPrepService(db, attachmentService, sesSendService);
    }

    @Override
    protected void startUp() throws Exception {
        this.sesSendService.startAsync();
        this.attachmentService.startAsync();
        this.sesSendPrepService.startAsync();
    }

    @Override
    protected void shutDown() throws Exception {
        this.sesSendPrepService.stopAsync();
        this.attachmentService.stopAsync();
        this.sesSendService.stopAsync();
    }
}
