package s3.service;

import common.AbstractFileService;
import entity.FileEntity;
import io.vertx.rxjava.core.eventbus.EventBus;
import s3.config.S3Config;
import s3.superS3t.S3Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by admin on 1/26/16.
 */
@Singleton
public class S3Service extends AbstractFileService {

    private final EventBus eventBus;
    private final S3Client s3Client;
    private final ExecutorService executorService;

    @Inject
    public S3Service(EventBus eventBus) {
        this.eventBus = eventBus;
        this.s3Client = new S3Client(S3Config.getAwsAccessKey(), S3Config.getAwsSecretKey(), S3Config.getEndPoint());
        this.executorService = Executors.newFixedThreadPool(S3Config.getThreads());
    }

    @Override
    public Future<byte[]> get(final FileEntity fileEntity) {
        return executorService.submit(new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                // todo get file byte[] using s3client
                return new byte[0];
            }
        });
    }

    @Override
    public Future<FileEntity> put(final FileEntity fileEntity) {
        return executorService.submit(new Callable<FileEntity>() {
            @Override
            public FileEntity call() throws Exception {
                return null;
            }
        });
    }

    @Override
    public void delete(FileEntity fileEntity) {

    }
}
