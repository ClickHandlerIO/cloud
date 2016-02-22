/**
 * This class is generated by jOOQ
 */
package io.clickhandler.data.schema.tables;


import io.clickhandler.data.schema.DefaultSchema;
import io.clickhandler.data.schema.Keys;
import io.clickhandler.data.schema.tables.records.EmailJnlRecord;

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
public class EmailJnl extends TableImpl<EmailJnlRecord> {

	private static final long serialVersionUID = -1313850461;

	/**
	 * The reference instance of <code>email_jnl</code>
	 */
	public static final EmailJnl EMAIL_JNL = new EmailJnl();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<EmailJnlRecord> getRecordType() {
		return EmailJnlRecord.class;
	}

	/**
	 * The column <code>email_jnl.id</code>.
	 */
	public final TableField<EmailJnlRecord, String> ID = createField("id", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>email_jnl.v</code>.
	 */
	public final TableField<EmailJnlRecord, Long> V = createField("v", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>email_jnl.c</code>.
	 */
	public final TableField<EmailJnlRecord, Timestamp> C = createField("c", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

	/**
	 * The column <code>email_jnl.user_id</code>.
	 */
	public final TableField<EmailJnlRecord, String> USER_ID = createField("user_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * The column <code>email_jnl.to</code>.
	 */
	public final TableField<EmailJnlRecord, String> TO = createField("to", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.cc</code>.
	 */
	public final TableField<EmailJnlRecord, String> CC = createField("cc", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.from</code>.
	 */
	public final TableField<EmailJnlRecord, String> FROM = createField("from", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.reply_to</code>.
	 */
	public final TableField<EmailJnlRecord, String> REPLY_TO = createField("reply_to", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.subject</code>.
	 */
	public final TableField<EmailJnlRecord, String> SUBJECT = createField("subject", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.text_body</code>.
	 */
	public final TableField<EmailJnlRecord, String> TEXT_BODY = createField("text_body", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.stripped_text_body</code>.
	 */
	public final TableField<EmailJnlRecord, String> STRIPPED_TEXT_BODY = createField("stripped_text_body", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.html_body</code>.
	 */
	public final TableField<EmailJnlRecord, String> HTML_BODY = createField("html_body", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.stripped_html_body</code>.
	 */
	public final TableField<EmailJnlRecord, String> STRIPPED_HTML_BODY = createField("stripped_html_body", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email_jnl.attachments</code>.
	 */
	public final TableField<EmailJnlRecord, Boolean> ATTACHMENTS = createField("attachments", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false), this, "");

	/**
	 * The column <code>email_jnl.message_id</code>.
	 */
	public final TableField<EmailJnlRecord, String> MESSAGE_ID = createField("message_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * Create a <code>email_jnl</code> table reference
	 */
	public EmailJnl() {
		this("email_jnl", null);
	}

	/**
	 * Create an aliased <code>email_jnl</code> table reference
	 */
	public EmailJnl(String alias) {
		this(alias, EMAIL_JNL);
	}

	private EmailJnl(String alias, Table<EmailJnlRecord> aliased) {
		this(alias, aliased, null);
	}

	private EmailJnl(String alias, Table<EmailJnlRecord> aliased, Field<?>[] parameters) {
		super(alias, DefaultSchema.DEFAULT_SCHEMA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<EmailJnlRecord> getPrimaryKey() {
		return Keys.PK_EMAIL_JNL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<EmailJnlRecord>> getKeys() {
		return Arrays.<UniqueKey<EmailJnlRecord>>asList(Keys.PK_EMAIL_JNL);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmailJnl as(String alias) {
		return new EmailJnl(alias, this);
	}

	/**
	 * Rename this table
	 */
	public EmailJnl rename(String name) {
		return new EmailJnl(name, null);
	}
}
