package ses.handler;

import entity.FileEntity;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.SqlDatabase;
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
    private final SqlDatabase db;
    private final S3Service s3Service;

    public AttachmentQueueHandler(SqlDatabase db, S3Service s3Service) {
        this.db = db;
        this.s3Service = s3Service;
    }

    @Override
    public void receive(List<DownloadRequest> downloadRequests) {
        downloadRequests.forEach(this::download);
    }

    public void download(final DownloadRequest request) {
        try {
            // get File Entity
            SqlHelper sqlHelper = new SqlHelper();
            db.readObservable(session -> session.getEntity(FileEntity.class, request.getFileId())).subscribe(fileEntity -> {
                sqlHelper.setFileEntity(fileEntity);
                sqlHelper.notify();
            });
            sqlHelper.wait();
            if(sqlHelper.getFileEntity() == null) {
                throw new Exception("FileEntity Not Found");
            }
            // get file data from S3
            final byte[] data = s3Service.get(sqlHelper.getFileEntity()).get().getBytes();
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

    class SqlHelper {
        private FileEntity fileEntity;

        public SqlHelper() {
        }

        public FileEntity getFileEntity() {
            return fileEntity;
        }

        public void setFileEntity(FileEntity fileEntity) {
            this.fileEntity = fileEntity;
        }
    }
}
