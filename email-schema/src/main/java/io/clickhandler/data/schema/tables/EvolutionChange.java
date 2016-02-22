/**
 * This class is generated by jOOQ
 */
package io.clickhandler.data.schema.tables;


import io.clickhandler.data.schema.DefaultSchema;
import io.clickhandler.data.schema.Keys;
import io.clickhandler.data.schema.tables.records.EvolutionChangeRecord;

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
public class EvolutionChange extends TableImpl<EvolutionChangeRecord> {

	private static final long serialVersionUID = 1430544969;

	/**
	 * The reference instance of <code>evolution_change</code>
	 */
	public static final EvolutionChange EVOLUTION_CHANGE = new EvolutionChange();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<EvolutionChangeRecord> getRecordType() {
		return EvolutionChangeRecord.class;
	}

	/**
	 * The column <code>evolution_change.id</code>.
	 */
	public final TableField<EvolutionChangeRecord, String> ID = createField("id", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>evolution_change.v</code>.
	 */
	public final TableField<EvolutionChangeRecord, Long> V = createField("v", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>evolution_change.c</code>.
	 */
	public final TableField<EvolutionChangeRecord, Timestamp> C = createField("c", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

	/**
	 * The column <code>evolution_change.type</code>.
	 */
	public final TableField<EvolutionChangeRecord, String> TYPE = createField("type", org.jooq.impl.SQLDataType.VARCHAR.length(29), this, "");

	/**
	 * The column <code>evolution_change.sql</code>.
	 */
	public final TableField<EvolutionChangeRecord, String> SQL = createField("sql", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>evolution_change.end</code>.
	 */
	public final TableField<EvolutionChangeRecord, Timestamp> END = createField("end", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>evolution_change.success</code>.
	 */
	public final TableField<EvolutionChangeRecord, Boolean> SUCCESS = createField("success", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false), this, "");

	/**
	 * The column <code>evolution_change.affected</code>.
	 */
	public final TableField<EvolutionChangeRecord, Long> AFFECTED = createField("affected", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>evolution_change.message</code>.
	 */
	public final TableField<EvolutionChangeRecord, String> MESSAGE = createField("message", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * Create a <code>evolution_change</code> table reference
	 */
	public EvolutionChange() {
		this("evolution_change", null);
	}

	/**
	 * Create an aliased <code>evolution_change</code> table reference
	 */
	public EvolutionChange(String alias) {
		this(alias, EVOLUTION_CHANGE);
	}

	private EvolutionChange(String alias, Table<EvolutionChangeRecord> aliased) {
		this(alias, aliased, null);
	}

	private EvolutionChange(String alias, Table<EvolutionChangeRecord> aliased, Field<?>[] parameters) {
		super(alias, DefaultSchema.DEFAULT_SCHEMA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<EvolutionChangeRecord> getPrimaryKey() {
		return Keys.PK_EVOLUTION_CHANGE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<EvolutionChangeRecord>> getKeys() {
		return Arrays.<UniqueKey<EvolutionChangeRecord>>asList(Keys.PK_EVOLUTION_CHANGE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EvolutionChange as(String alias) {
		return new EvolutionChange(alias, this);
	}

	/**
	 * Rename this table
	 */
	public EvolutionChange rename(String name) {
		return new EvolutionChange(name, null);
	}
}
