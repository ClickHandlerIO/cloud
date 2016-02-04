package entity;

import com.google.common.base.Strings;
import io.clickhandler.sql.*;

@Table
@Indexes({
        @Index(columns = @IndexColumn("name"), unique = false),
        @Index(columns = @IndexColumn("store_bucket"), unique = false),
        @Index(columns = @IndexColumn("store_id"), unique = true)
})
public class FileEntity extends AbstractEntity {
    @Column(length = 128)
    private String name;
    @Column(length = 4000)
    private String description;
    @Column(length = 128)
    private String contentType;
    @Column(length = 64)
    private String md5;
    @Column(length = 64)
    private String etag;
    @Column(length = 12)
    private int compression;
    @Column
    private long size;
    @Column(length = 128)
    private String storeBucket;
    @Column(length = 128)
    private String storeId;
    @Column
    private EncodedKey key;
    @Column(length = 16)
    private String storageClass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public int getCompression() {
        return compression;
    }

    public void setCompression(int compression) {
        this.compression = compression;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getStoreBucket() {
        return storeBucket;
    }

    public void setStoreBucket(String storeBucket) {
        this.storeBucket = storeBucket;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public EncodedKey getKey() {
        return key;
    }

    public void setKey(EncodedKey key) {
        this.key = key;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    public boolean isEncrypted() {
        return key != null && !Strings.nullToEmpty(key.getKey()).trim().isEmpty();
    }
}
