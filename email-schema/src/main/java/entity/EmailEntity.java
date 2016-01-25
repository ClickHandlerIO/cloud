package entity;

import io.clickhandler.sql.annotations.Column;
import io.clickhandler.sql.annotations.Table;
import io.clickhandler.sql.entity.AbstractEntity;

/**
 *
 */
@Table
public class EmailEntity extends AbstractEntity {
    @Column
    private String messageId;
    /**
     *
     */
    @Column
    private String storeId; // assuming this is the user Id of the sender, which would be the same for reply
    /**
     *
     */
    @Column
    private String html;
    /**
     *
     */
    @Column
    private String htmlStripped;
    /**
     *
     */
    @Column
    private String text;
    /**
     *
     */
    @Column
    private String textStripped;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getHtmlStripped() {
        return htmlStripped;
    }

    public void setHtmlStripped(String htmlStripped) {
        this.htmlStripped = htmlStripped;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTextStripped() {
        return textStripped;
    }

    public void setTextStripped(String textStripped) {
        this.textStripped = textStripped;
    }
}
