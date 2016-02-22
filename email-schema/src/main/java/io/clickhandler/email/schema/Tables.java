/**
 * This class is generated by jOOQ
 */
package io.clickhandler.email.schema;


import io.clickhandler.email.schema.tables.Email;
import io.clickhandler.email.schema.tables.EmailAttachment;
import io.clickhandler.email.schema.tables.EmailAttachmentJnl;
import io.clickhandler.email.schema.tables.EmailJnl;
import io.clickhandler.email.schema.tables.EmailRecipient;
import io.clickhandler.email.schema.tables.EmailRecipientJnl;
import io.clickhandler.email.schema.tables.Evolution;
import io.clickhandler.email.schema.tables.EvolutionChange;
import io.clickhandler.email.schema.tables.File;
import io.clickhandler.email.schema.tables.FileJnl;

import javax.annotation.Generated;


/**
 * Convenience access to all tables in 
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ v:3.7.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public interface Tables {

	/**
	 * The table evolution
	 */
	public static final Evolution EVOLUTION = io.clickhandler.email.schema.tables.Evolution.EVOLUTION;

	/**
	 * The table email_attachment
	 */
	public static final EmailAttachment EMAIL_ATTACHMENT = io.clickhandler.email.schema.tables.EmailAttachment.EMAIL_ATTACHMENT;

	/**
	 * The table email_attachment_jnl
	 */
	public static final EmailAttachmentJnl EMAIL_ATTACHMENT_JNL = io.clickhandler.email.schema.tables.EmailAttachmentJnl.EMAIL_ATTACHMENT_JNL;

	/**
	 * The table email_recipient
	 */
	public static final EmailRecipient EMAIL_RECIPIENT = io.clickhandler.email.schema.tables.EmailRecipient.EMAIL_RECIPIENT;

	/**
	 * The table email_recipient_jnl
	 */
	public static final EmailRecipientJnl EMAIL_RECIPIENT_JNL = io.clickhandler.email.schema.tables.EmailRecipientJnl.EMAIL_RECIPIENT_JNL;

	/**
	 * The table email
	 */
	public static final Email EMAIL = io.clickhandler.email.schema.tables.Email.EMAIL;

	/**
	 * The table email_jnl
	 */
	public static final EmailJnl EMAIL_JNL = io.clickhandler.email.schema.tables.EmailJnl.EMAIL_JNL;

	/**
	 * The table evolution_change
	 */
	public static final EvolutionChange EVOLUTION_CHANGE = io.clickhandler.email.schema.tables.EvolutionChange.EVOLUTION_CHANGE;

	/**
	 * The table file
	 */
	public static final File FILE = io.clickhandler.email.schema.tables.File.FILE;

	/**
	 * The table file_jnl
	 */
	public static final FileJnl FILE_JNL = io.clickhandler.email.schema.tables.FileJnl.FILE_JNL;
}
