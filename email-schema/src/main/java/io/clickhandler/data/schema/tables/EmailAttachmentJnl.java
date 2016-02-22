/**
 * This class is generated by jOOQ
 */
package io.clickhandler.data.schema.tables;


import io.clickhandler.data.schema.DefaultSchema;
import io.clickhandler.data.schema.Keys;
import io.clickhandler.data.schema.tables.records.EmailAttachmentJnlRecord;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ v:3.7.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class EmailAttachmentJnl extends TableImpl<EmailAttachmentJnlRecord> {

	private static final long serialVersionUID = -1414587532;

	/**
	 * The reference instance of <code>email_attachment_jnl</code>
	 */
	public static final EmailAttachmentJnl EMAIL_ATTACHMENT_JNL = new EmailAttachmentJnl();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<EmailAttachmentJnlRecord> getRecordType() {
		return EmailAttachmentJnlRecord.class;
	}

	/**
	 * The column <code>email_attachment_jnl.id</code>.
	 */
	public final TableField<EmailAttachmentJnlRecord, String> ID = createField("id", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>email_attachment_jnl.v</code>.
	 */
	public final TableField<EmailAttachmentJnlRecord, Long> V = createField("v", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>email_attachment_jnl.c</code>.
	 */
	public final TableField<EmailAttachmentJnlRecord, Timestamp> C = createField("c", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

	/**
	 * The column <code>email_attachment_jnl.name</code>.
	 */
	public final TableField<EmailAttachmentJnlRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_attachment_jnl.description</code>.
	 */
	public final TableField<EmailAttachmentJnlRecord, String> DESCRIPTION = createField("description", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_attachment_jnl.mime_type</code>.
	 */
	public final TableField<EmailAttachmentJnlRecord, String> MIME_TYPE = createField("mime_type", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_attachment_jnl.file_id</code>.
	 */
	public final TableField<EmailAttachmentJnlRecord, String> FILE_ID = createField("file_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * The column <code>email_attachment_jnl.email_id</code>.
	 */
	public final TableField<EmailAttachmentJnlRecord, String> EMAIL_ID = createField("email_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * Create a <code>email_attachment_jnl</code> table reference
	 */
	public EmailAttachmentJnl() {
		this("email_attachment_jnl", null);
	}

	/**
	 * Create an aliased <code>email_attachment_jnl</code> table reference
	 */
	public EmailAttachmentJnl(String alias) {
		this(alias, EMAIL_ATTACHMENT_JNL);
	}

	private EmailAttachmentJnl(String alias, Table<EmailAttachmentJnlRecord> aliased) {
		this(alias, aliased, null);
	}

	private EmailAttachmentJnl(String alias, Table<EmailAttachmentJnlRecord> aliased, Field<?>[] parameters) {
		super(alias, DefaultSchema.DEFAULT_SCHEMA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<EmailAttachmentJnlRecord> getPrimaryKey() {
		return Keys.PK_EMAIL_ATTACHMENT_JNL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<EmailAttachmentJnlRecord>> getKeys() {
		return Arrays.<UniqueKey<EmailAttachmentJnlRecord>>asList(Keys.PK_EMAIL_ATTACHMENT_JNL);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailAttachmentJnl as(String alias) {
		return new EmailAttachmentJnl(alias, this);
	}

	/**
	 * Rename this table
	 */
	public EmailAttachmentJnl rename(String name) {
		return new EmailAttachmentJnl(name, null);
	}
}
