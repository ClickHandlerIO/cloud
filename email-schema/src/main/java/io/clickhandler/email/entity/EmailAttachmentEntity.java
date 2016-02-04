package io.clickhandler.email.entity;


import io.clickhandler.sql.AbstractEntity;
import io.clickhandler.sql.Column;
import io.clickhandler.sql.Table;

/**
 *
 */
@Table
public class EmailAttachmentEntity extends AbstractEntity {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String mimeType; // string for MIME type for file
    @Column
    private String fileId; // points to file entity for attachment
    @Column
    private String emailId; // points to email which attachment belongs to

    public EmailAttachmentEntity() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
