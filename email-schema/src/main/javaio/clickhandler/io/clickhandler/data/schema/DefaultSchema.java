/**
 * This class is generated by jOOQ
 */
package io.clickhandler.data.schema;


import io.clickhandler.data.schema.tables.Email;
import io.clickhandler.data.schema.tables.EmailAttachment;
import io.clickhandler.data.schema.tables.EmailAttachmentJnl;
import io.clickhandler.data.schema.tables.EmailJnl;
import io.clickhandler.data.schema.tables.EmailRecipient;
import io.clickhandler.data.schema.tables.EmailRecipientJnl;
import io.clickhandler.data.schema.tables.Evolution;
import io.clickhandler.data.schema.tables.EvolutionChange;
import io.clickhandler.data.schema.tables.File;
import io.clickhandler.data.schema.tables.FileJnl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


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
public class DefaultSchema extends SchemaImpl {

	private static final long serialVersionUID = 185135608;

	/**
	 * The reference instance of <code></code>
	 */
	public static final DefaultSchema DEFAULT_SCHEMA = new DefaultSchema();

	/**
	 * No further instances allowed
	 */
	private DefaultSchema() {
		super("");
	}

	@Override
	public final List<Table<?>> getTables() {
		List result = new ArrayList();
		result.addAll(getTables0());
		return result;
	}

	private final List<Table<?>> getTables0() {
		return Arrays.<Table<?>>asList(
			Email.EMAIL,
			EmailJnl.EMAIL_JNL,
			EvolutionChange.EVOLUTION_CHANGE,
			EmailRecipient.EMAIL_RECIPIENT,
			EmailRecipientJnl.EMAIL_RECIPIENT_JNL,
			EmailAttachment.EMAIL_ATTACHMENT,
			EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL,
			File.FILE,
			FileJnl.FILE_JNL,
			Evolution.EVOLUTION);
	}
}
