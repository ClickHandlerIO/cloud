package mailgun.handler;

import common.handler.EmailSendPrepQueueHandler;
import common.service.FileAttachmentDownloadService;
import entity.EmailEntity;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.MultiMap;
import io.vertx.rxjava.core.eventbus.EventBus;
import mailgun.data.MailgunSendRequest;
import mailgun.service.MailgunSendService;

import java.util.List;

/**
 * @author Brad Behnke
 */
public class MailgunSendPrepQueueHandler extends EmailSendPrepQueueHandler<MailgunSendRequest> {

    private final EventBus eventBus;
    private final SqlExecutor db;
    private final FileAttachmentDownloadService fileAttachmentDownloadService;
    private final MailgunSendService sendService;

    public MailgunSendPrepQueueHandler(EventBus eventBus, SqlExecutor db, FileAttachmentDownloadService fileAttachmentDownloadService, MailgunSendService sendService) {
        super(db);
        this.eventBus = eventBus;
        this.db = db;
        this.fileAttachmentDownloadService = fileAttachmentDownloadService;
        this.sendService = sendService;
    }

    @Override
    public void receive(List<MailgunSendRequest> sendRequests) {
        sendRequests.forEach(this::processRequest);
    }

    private void processRequest(MailgunSendRequest sendRequest) {
        final EmailEntity emailEntity = sendRequest.getEmailEntity();
        MultiMap multiMap = MultiMap.caseInsensitiveMultiMap();

    }
}
