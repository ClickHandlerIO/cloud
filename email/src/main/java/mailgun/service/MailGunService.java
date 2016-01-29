package mailgun.service;

import com.sun.istack.internal.NotNull;
import common.data.SendRequest;
import common.service.EmailService;
import common.service.FileAttachmentDownloadService;
import common.service.FileService;
import entity.EmailEntity;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.rxjava.core.eventbus.EventBus;
import mailgun.config.MailgunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *  Email service using Mailgun to send/receive emails.
 *
 * @author Brad Behnke
 */
@Singleton
public class MailgunService extends EmailService<SendRequest>{
    private final static Logger LOG = LoggerFactory.getLogger(MailgunService.class);

    private final MailgunSendPrepService sendPrepService;
    private final MailgunSendService sendService;
    private final FileAttachmentDownloadService fileAttachmentDownloadService;

    @Inject
    public MailgunService(@NotNull MailgunConfig config, @NotNull EventBus eventBus, @NotNull SqlExecutor db, @NotNull FileService fileService) {
        this.fileAttachmentDownloadService = new FileAttachmentDownloadService(config, db, fileService);
        this.sendService = new MailgunSendService(config, eventBus, db);
        this.sendPrepService = new MailgunSendPrepService(config, eventBus, db, fileAttachmentDownloadService, sendService);
    }

    @Override
    protected void startUp() throws Exception {
        this.sendService.startAsync();
        this.fileAttachmentDownloadService.startAsync();
        this.sendPrepService.startAsync();
        LOG.info("Mailgun Service Started.");
    }

    @Override
    protected void shutDown() throws Exception {
        this.sendPrepService.stopAsync();
        this.fileAttachmentDownloadService.stopAsync();
        this.sendService.stopAsync();
        LOG.info("Mailgun Service Shutdown.");
    }

    @Override
    public Observable<EmailEntity> sendObservable(SendRequest sendRequest) {
        return null;
    }
}
