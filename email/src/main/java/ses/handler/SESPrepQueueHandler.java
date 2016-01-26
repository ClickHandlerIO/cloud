package ses.handler;

import com.google.common.base.Preconditions;
import data.schema.Tables;
import entity.EmailAttachmentEntity;
import entity.EmailEntity;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.Database;
import io.clickhandler.sql.db.DatabaseSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ses.data.DownloadRequest;
import ses.service.AttachmentService;
import ses.service.SESSendService;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prepares email messages by building messages from db email record. Also downloads attachments for email using
 * S3Service. Once built, passes email message to be sent to SESSendService.
 *
 * @author Brad Behnke
 */
public class SESPrepQueueHandler  implements QueueHandler<String>, Tables {

    private static final Logger LOG = LoggerFactory.getLogger(SESPrepQueueHandler.class);
    private final DatabaseSession db;
    private final AttachmentService attachmentService;
    private final SESSendService sesSendService;
    private final AtomicInteger activeDownloads = new AtomicInteger(); // tracks number of current downloads and notifies completion
    private final AtomicBoolean attachFailed = new AtomicBoolean(); // tracks if a download failed or not

    public SESPrepQueueHandler(Database db, AttachmentService attachmentService, SESSendService sesSendService) {
        this.db = db.getSession();
        this.attachmentService = attachmentService;
        this.sesSendService = sesSendService;
    }

    @Override
    public void receive(List<String> emailIds) {
        for (String emailId:emailIds) {
            activeDownloads.set(0);
            attachFailed.set(false);
            try {
                EmailEntity emailEntity = getEmailEntity(emailId);
                final MimeMessage message = buildMimeMessage(emailEntity);
                // load attachments if there are any
                if(emailEntity.isAttachments()) {
                    processAttachments(emailEntity, message);
                }
                // Wait for attachment downloads, will not enter if there were no attachments
                if(activeDownloads.get() > 0 && !attachFailed.get()) {
                    activeDownloads.wait();
                }
                // send email if no downloads failed
                if(!attachFailed.get()) {
                    sesSendService.enqueue(emailId, message);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }
    }

    private EmailEntity getEmailEntity(String emailId) throws Exception {
        final EmailEntity emailEntity = db.getEntity(EmailEntity.class, emailId);
        if(emailEntity == null) {
            throw new Exception("Email record not found.");
        }
        return emailEntity;
    }

    private MimeMessage buildMimeMessage(EmailEntity emailEntity) throws Exception {
        final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setSubject((emailEntity.getSubject() == null || emailEntity.getSubject().isEmpty()) ? "No Subject" : emailEntity.getSubject());
        message.setFrom(new InternetAddress(emailEntity.getFrom()));
        message.setReplyTo(new Address[]{new InternetAddress(emailEntity.getReplyTo())});
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailEntity.getTo()));
        message.setSentDate(new Date());
        // Text version
        final MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(emailEntity.getTextBody(), "text/plain");
        // HTML version
        final MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(emailEntity.getHtmlBody(), "text/html");
        // Wrap Parts
        final Multipart mp = new MimeMultipart("alternative");
        mp.addBodyPart(textPart);
        mp.addBodyPart(htmlPart);
        // Set wrapper as message's content
        message.setContent(mp);
        return message;
    }

    private void processAttachments(EmailEntity emailEntity, MimeMessage message) throws Exception {
        // get attachment info from db
        List<EmailAttachmentEntity> attachmentEntities = getEmailAttachmentEntities(emailEntity);
        final MimeMultipart content = (MimeMultipart) message.getContent();
        for (final EmailAttachmentEntity attachmentEntity : attachmentEntities) {
            // breaks if still looping and a previous download failed.
            if(attachFailed.get())
                break;
            activeDownloads.incrementAndGet();
            // get file byte[] from AttachmentService
            attachmentService.enqueue(new DownloadRequest(attachmentEntity.getFileId(), new DownloadCallBack() {
                @Override
                public void onSuccess(byte[] data) {
                    try {
                        Preconditions.checkNotNull(data);
                        final MimeBodyPart attachmentPart = new MimeBodyPart();
                        attachmentPart.setDescription(attachmentEntity.getDescription(), "UTF-8");
                        final DataSource ds = new ByteArrayDataSource(data, attachmentEntity.getMimeType());
                        attachmentPart.setDataHandler(new DataHandler(ds));
                        attachmentPart.setHeader("Content-ID", "<" + attachmentEntity.getId() + ">");
                        attachmentPart.setFileName(attachmentEntity.getName());
                        content.addBodyPart(attachmentPart);

                        if(activeDownloads.decrementAndGet() <=0){
                            activeDownloads.notify();
                        }
                    } catch (Exception e) {
                        LOG.error("Email Attachment Build Failed", e);
                        // break out of loop or if already out of loop stop waiting, and don't send.
                        attachFailed.set(true);
                        activeDownloads.notify();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    LOG.error("Email Attachment S3 Download Failed", e);
                    // break out of loop or if already out of loop stop waiting, and don't send.
                    attachFailed.set(true);
                    activeDownloads.notify();
                }
            }));
        }
    }

    private List<EmailAttachmentEntity> getEmailAttachmentEntities(EmailEntity emailEntity) throws Exception {
        List<EmailAttachmentEntity> attachmentEntities = db.select(EMAIL_ATTACHMENT.fields())
                .where(EMAIL_ATTACHMENT.EMAIL_ID.eq(emailEntity.getId()))
                .fetch().into(EMAIL_ATTACHMENT).into(EmailAttachmentEntity.class);
        if (attachmentEntities.isEmpty()) {
            throw new Exception("Email attachment record not found.");
        }
        return attachmentEntities;
    }
}
