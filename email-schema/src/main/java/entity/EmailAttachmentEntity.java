package entity;

import io.clickhandler.sql.annotations.Column;

/**
 *
 */
public class EmailAttachmentEntity {
    @Column
    private String name;
    @Column
    private String type;
    @Column
    private String fileId;
    @Column
    private String cid;
    @Column
    private boolean image;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public boolean isImage() {
        return image;
    }

    public void setImage(boolean image) {
        this.image = image;
    }
}
