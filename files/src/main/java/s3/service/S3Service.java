package s3.service;

import entity.FileEntity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;

/**
 * Created by admin on 1/26/16.
 */
@Singleton
public class S3Service {

    // TODO, upload and download using S3

    @Inject
    public S3Service() {

    }

    // DOWN
    public InputStream get(FileEntity fileEntity) {
        return null;
    }
}
