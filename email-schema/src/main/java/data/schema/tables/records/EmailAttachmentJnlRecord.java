/**
 * This class is generated by jOOQ
 */
package data.schema.tables.records;


import data.schema.tables.EmailAttachmentJnl;

import java.sql.Timestamp;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record8;
import org.jooq.Row8;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.7.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class EmailAttachmentJnlRecord extends UpdatableRecordImpl<EmailAttachmentJnlRecord> implements Record8<String, Long, Timestamp, String, String, String, String, String> {

	private static final long serialVersionUID = -1216261518;

	/**
	 * Setter for <code>email_attachment_jnl.id</code>.
	 */
	public EmailAttachmentJnlRecord setId(String value) {
		setValue(0, value);
		return this;
	}

	/**
	 * Getter for <code>email_attachment_jnl.id</code>.
	 */
	public String getId() {
		return (String) getValue(0);
	}

	/**
	 * Setter for <code>email_attachment_jnl.v</code>.
	 */
	public EmailAttachmentJnlRecord setV(Long value) {
		setValue(1, value);
		return this;
	}

	/**
	 * Getter for <code>email_attachment_jnl.v</code>.
	 */
	public Long getV() {
		return (Long) getValue(1);
	}

	/**
	 * Setter for <code>email_attachment_jnl.c</code>.
	 */
	public EmailAttachmentJnlRecord setC(Timestamp value) {
		setValue(2, value);
		return this;
	}

	/**
	 * Getter for <code>email_attachment_jnl.c</code>.
	 */
	public Timestamp getC() {
		return (Timestamp) getValue(2);
	}

	/**
	 * Setter for <code>email_attachment_jnl.name</code>.
	 */
	public EmailAttachmentJnlRecord setName(String value) {
		setValue(3, value);
		return this;
	}

	/**
	 * Getter for <code>email_attachment_jnl.name</code>.
	 */
	public String getName() {
		return (String) getValue(3);
	}

	/**
	 * Setter for <code>email_attachment_jnl.description</code>.
	 */
	public EmailAttachmentJnlRecord setDescription(String value) {
		setValue(4, value);
		return this;
	}

	/**
	 * Getter for <code>email_attachment_jnl.description</code>.
	 */
	public String getDescription() {
		return (String) getValue(4);
	}

	/**
	 * Setter for <code>email_attachment_jnl.mime_type</code>.
	 */
	public EmailAttachmentJnlRecord setMimeType(String value) {
		setValue(5, value);
		return this;
	}

	/**
	 * Getter for <code>email_attachment_jnl.mime_type</code>.
	 */
	public String getMimeType() {
		return (String) getValue(5);
	}

	/**
	 * Setter for <code>email_attachment_jnl.file_id</code>.
	 */
	public EmailAttachmentJnlRecord setFileId(String value) {
		setValue(6, value);
		return this;
	}

	/**
	 * Getter for <code>email_attachment_jnl.file_id</code>.
	 */
	public String getFileId() {
		return (String) getValue(6);
	}

	/**
	 * Setter for <code>email_attachment_jnl.email_id</code>.
	 */
	public EmailAttachmentJnlRecord setEmailId(String value) {
		setValue(7, value);
		return this;
	}

	/**
	 * Getter for <code>email_attachment_jnl.email_id</code>.
	 */
	public String getEmailId() {
		return (String) getValue(7);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Record2<String, Long> key() {
		return (Record2) super.key();
	}

	// -------------------------------------------------------------------------
	// Record8 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row8<String, Long, Timestamp, String, String, String, String, String> fieldsRow() {
		return (Row8) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row8<String, Long, Timestamp, String, String, String, String, String> valuesRow() {
		return (Row8) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field1() {
		return EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Long> field2() {
		return EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.V;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Timestamp> field3() {
		return EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.C;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field4() {
		return EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field5() {
		return EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.DESCRIPTION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field6() {
		return EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.MIME_TYPE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field7() {
		return EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.FILE_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field8() {
		return EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL.EMAIL_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value1() {
		return getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long value2() {
		return getV();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Timestamp value3() {
		return getC();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value4() {
		return getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value5() {
		return getDescription();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value6() {
		return getMimeType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value7() {
		return getFileId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value8() {
		return getEmailId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord value1(String value) {
		setId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord value2(Long value) {
		setV(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord value3(Timestamp value) {
		setC(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord value4(String value) {
		setName(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord value5(String value) {
		setDescription(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord value6(String value) {
		setMimeType(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord value7(String value) {
		setFileId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord value8(String value) {
		setEmailId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnlRecord values(String value1, Long value2, Timestamp value3, String value4, String value5, String value6, String value7, String value8) {
		value1(value1);
		value2(value2);
		value3(value3);
		value4(value4);
		value5(value5);
		value6(value6);
		value7(value7);
		value8(value8);
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached EmailAttachmentJnlRecord
	 */
	public EmailAttachmentJnlRecord() {
		super(EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL);
	}

	/**
	 * Create a detached, initialised EmailAttachmentJnlRecord
	 */
	public EmailAttachmentJnlRecord(String id, Long v, Timestamp c, String name, String description, String mimeType, String fileId, String emailId) {
		super(EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL);

		setValue(0, id);
		setValue(1, v);
		setValue(2, c);
		setValue(3, name);
		setValue(4, description);
		setValue(5, mimeType);
		setValue(6, fileId);
		setValue(7, emailId);
	}
}