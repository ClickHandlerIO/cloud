/**
 * This class is generated by jOOQ
 */
package data.schema.tables;


import data.schema.DefaultSchema;
import data.schema.Keys;
import data.schema.tables.records.EmailRecipientRecord;

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
		"jOOQ version:3.7.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class EmailRecipient extends TableImpl<EmailRecipientRecord> {

	private static final long serialVersionUID = -289853449;

	/**
	 * The reference instance of <code>email_recipient</code>
	 */
	public static final EmailRecipient EMAIL_RECIPIENT = new EmailRecipient();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<EmailRecipientRecord> getRecordType() {
		return EmailRecipientRecord.class;
	}

	/**
	 * The column <code>email_recipient.id</code>.
	 */
	public final TableField<EmailRecipientRecord, String> ID = createField("id", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>email_recipient.v</code>.
	 */
	public final TableField<EmailRecipientRecord, Long> V = createField("v", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>email_recipient.c</code>.
	 */
	public final TableField<EmailRecipientRecord, Timestamp> C = createField("c", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

	/**
	 * The column <code>email_recipient.email_id</code>.
	 */
	public final TableField<EmailRecipientRecord, String> EMAIL_ID = createField("email_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * The column <code>email_recipient.type</code>.
	 */
	public final TableField<EmailRecipientRecord, String> TYPE = createField("type", org.jooq.impl.SQLDataType.VARCHAR.length(13), this, "");

	/**
	 * The column <code>email_recipient.name</code>.
	 */
	public final TableField<EmailRecipientRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_recipient.address</code>.
	 */
	public final TableField<EmailRecipientRecord, String> ADDRESS = createField("address", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_recipient.status</code>.
	 */
	public final TableField<EmailRecipientRecord, String> STATUS = createField("status", org.jooq.impl.SQLDataType.VARCHAR.length(19), this, "");

	/**
	 * The column <code>email_recipient.sent</code>.
	 */
	public final TableField<EmailRecipientRecord, Timestamp> SENT = createField("sent", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>email_recipient.delivered</code>.
	 */
	public final TableField<EmailRecipientRecord, Timestamp> DELIVERED = createField("delivered", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>email_recipient.bounced</code>.
	 */
	public final TableField<EmailRecipientRecord, Timestamp> BOUNCED = createField("bounced", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>email_recipient.complaint</code>.
	 */
	public final TableField<EmailRecipientRecord, Timestamp> COMPLAINT = createField("complaint", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>email_recipient.failed</code>.
	 */
	public final TableField<EmailRecipientRecord, Timestamp> FAILED = createField("failed", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>email_recipient.opened</code>.
	 */
	public final TableField<EmailRecipientRecord, Timestamp> OPENED = createField("opened", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>email_recipient.contact_id</code>.
	 */
	public final TableField<EmailRecipientRecord, String> CONTACT_ID = createField("contact_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * The column <code>email_recipient.user_id</code>.
	 */
	public final TableField<EmailRecipientRecord, String> USER_ID = createField("user_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * Create a <code>email_recipient</code> table reference
	 */
	public EmailRecipient() {
		this("email_recipient", null);
	}

	/**
	 * Create an aliased <code>email_recipient</code> table reference
	 */
	public EmailRecipient(String alias) {
		this(alias, EMAIL_RECIPIENT);
	}

	private EmailRecipient(String alias, Table<EmailRecipientRecord> aliased) {
		this(alias, aliased, null);
	}

	private EmailRecipient(String alias, Table<EmailRecipientRecord> aliased, Field<?>[] parameters) {
		super(alias, DefaultSchema.DEFAULT_SCHEMA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<EmailRecipientRecord> getPrimaryKey() {
		return Keys.PK_EMAIL_RECIPIENT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<EmailRecipientRecord>> getKeys() {
		return Arrays.<UniqueKey<EmailRecipientRecord>>asList(Keys.PK_EMAIL_RECIPIENT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailRecipient as(String alias) {
		return new EmailRecipient(alias, this);
	}

	/**
	 * Rename this table
	 */
	public EmailRecipient rename(String name) {
		return new EmailRecipient(name, null);
	}
}
