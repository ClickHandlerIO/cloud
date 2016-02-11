/**
 * This class is generated by jOOQ
 */
package io.clickhandler.data.schema.tables;


import io.clickhandler.data.schema.DefaultSchema;
import io.clickhandler.data.schema.Keys;
import io.clickhandler.data.schema.tables.records.EmailRecord;

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
public class Email extends TableImpl<EmailRecord> {

	private static final long serialVersionUID = 201536782;

	/**
	 * The reference instance of <code>email</code>
	 */
	public static final Email EMAIL = new Email();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<EmailRecord> getRecordType() {
		return EmailRecord.class;
	}

	/**
	 * The column <code>email.id</code>.
	 */
	public final TableField<EmailRecord, String> ID = createField("id", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>email.v</code>.
	 */
	public final TableField<EmailRecord, Long> V = createField("v", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>email.c</code>.
	 */
	public final TableField<EmailRecord, Timestamp> C = createField("c", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

	/**
	 * The column <code>email.user_id</code>.
	 */
	public final TableField<EmailRecord, String> USER_ID = createField("user_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * The column <code>email.to</code>.
	 */
	public final TableField<EmailRecord, String> TO = createField("to", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.cc</code>.
	 */
	public final TableField<EmailRecord, String> CC = createField("cc", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.from</code>.
	 */
	public final TableField<EmailRecord, String> FROM = createField("from", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.reply_to</code>.
	 */
	public final TableField<EmailRecord, String> REPLY_TO = createField("reply_to", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.subject</code>.
	 */
	public final TableField<EmailRecord, String> SUBJECT = createField("subject", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.text_body</code>.
	 */
	public final TableField<EmailRecord, String> TEXT_BODY = createField("text_body", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.stripped_text_body</code>.
	 */
	public final TableField<EmailRecord, String> STRIPPED_TEXT_BODY = createField("stripped_text_body", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.html_body</code>.
	 */
	public final TableField<EmailRecord, String> HTML_BODY = createField("html_body", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.stripped_html_body</code>.
	 */
	public final TableField<EmailRecord, String> STRIPPED_HTML_BODY = createField("stripped_html_body", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * The column <code>email.attachments</code>.
	 */
	public final TableField<EmailRecord, Boolean> ATTACHMENTS = createField("attachments", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false), this, "");

	/**
	 * The column <code>email.message_id</code>.
	 */
	public final TableField<EmailRecord, String> MESSAGE_ID = createField("message_id", org.jooq.impl.SQLDataType.VARCHAR.length(32), this, "");

	/**
	 * Create a <code>email</code> table reference
	 */
	public Email() {
		this("email", null);
	}

	/**
	 * Create an aliased <code>email</code> table reference
	 */
	public Email(String alias) {
		this(alias, EMAIL);
	}

	private Email(String alias, Table<EmailRecord> aliased) {
		this(alias, aliased, null);
	}

	private Email(String alias, Table<EmailRecord> aliased, Field<?>[] parameters) {
		super(alias, DefaultSchema.DEFAULT_SCHEMA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<EmailRecord> getPrimaryKey() {
		return Keys.PK_EMAIL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<EmailRecord>> getKeys() {
		return Arrays.<UniqueKey<EmailRecord>>asList(Keys.PK_EMAIL);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Email as(String alias) {
		return new Email(alias, this);
	}

	/**
	 * Rename this table
	 */
	public Email rename(String name) {
		return new Email(name, null);
	}
}
