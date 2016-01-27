package common;

import entity.FileEntity;

import java.util.concurrent.Future;

/**
 * Created by admin on 1/27/16.
 */
public abstract class AbstractFileService {
    public abstract Future<byte[]> get(final FileEntity fileEntity);
    public abstract Future<FileEntity> put(final FileEntity fileEntity);
    public abstract void delete(final FileEntity fileEntity);
}
