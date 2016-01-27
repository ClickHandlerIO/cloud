package ses.handler;

import entity.FileEntity;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.Database;
import io.clickhandler.sql.db.DatabaseSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s3.service.S3Service;
import ses.data.DownloadRequest;

import java.util.List;

/**
 * Retrieves byte[] data for files requested from S3Service and returns data or Exception back to caller.
 *
 * @author Brad Behnke
 */
public class AttachmentQueueHandler implements QueueHandler<DownloadRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(AttachmentQueueHandler.class);
    private final DatabaseSession db;
    private final S3Service s3Service;

    public AttachmentQueueHandler(Database db, S3Service s3Service) {
        this.db = db.getSession();
        this.s3Service = s3Service;
    }

    @Override
    public void receive(List<DownloadRequest> downloadRequests) {
        downloadRequests.forEach(this::download);
    }

    public void download(DownloadRequest request) {
        try {
            // get file record from db
            final FileEntity file = db.getEntity(FileEntity.class, request.getFileId());
            if (file == null) {
                throw new Exception("FileEntity Not Found");
            }
            // get file data from S3
            final byte[] data = s3Service.get(file).get().getBytes();
            if(data == null) {
                throw new Exception("Failed to Get S3 File Data");
            }
            // return data to caller
            request.getCallBack().onSuccess(data);
        } catch (Exception e) {
            // send failure notification
            request.getCallBack().onFailure(e);
        }
    }
}
