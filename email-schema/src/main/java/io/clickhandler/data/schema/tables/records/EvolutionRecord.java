/**
 * This class is generated by jOOQ
 */
package io.clickhandler.data.schema.tables.records;


import io.clickhandler.data.schema.tables.Evolution;

import java.sql.Timestamp;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


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
public class EvolutionRecord extends UpdatableRecordImpl<EvolutionRecord> implements Record5<String, Long, Timestamp, Boolean, Timestamp> {

	private static final long serialVersionUID = -1325102587;

	/**
	 * Setter for <code>evolution.id</code>.
	 */
	public EvolutionRecord setId(String value) {
		setValue(0, value);
		return this;
	}

	/**
	 * Getter for <code>evolution.id</code>.
	 */
	public String getId() {
		return (String) getValue(0);
	}

	/**
	 * Setter for <code>evolution.v</code>.
	 */
	public EvolutionRecord setV(Long value) {
		setValue(1, value);
		return this;
	}

	/**
	 * Getter for <code>evolution.v</code>.
	 */
	public Long getV() {
		return (Long) getValue(1);
	}

	/**
	 * Setter for <code>evolution.c</code>.
	 */
	public EvolutionRecord setC(Timestamp value) {
		setValue(2, value);
		return this;
	}

	/**
	 * Getter for <code>evolution.c</code>.
	 */
	public Timestamp getC() {
		return (Timestamp) getValue(2);
	}

	/**
	 * Setter for <code>evolution.success</code>.
	 */
	public EvolutionRecord setSuccess(Boolean value) {
		setValue(3, value);
		return this;
	}

	/**
	 * Getter for <code>evolution.success</code>.
	 */
	public Boolean getSuccess() {
		return (Boolean) getValue(3);
	}

	/**
	 * Setter for <code>evolution.end</code>.
	 */
	public EvolutionRecord setEnd(Timestamp value) {
		setValue(4, value);
		return this;
	}

	/**
	 * Getter for <code>evolution.end</code>.
	 */
	public Timestamp getEnd() {
		return (Timestamp) getValue(4);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Record1<String> key() {
		return (Record1) super.key();
	}

	// -------------------------------------------------------------------------
	// Record5 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row5<String, Long, Timestamp, Boolean, Timestamp> fieldsRow() {
		return (Row5) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row5<String, Long, Timestamp, Boolean, Timestamp> valuesRow() {
		return (Row5) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field1() {
		return Evolution.EVOLUTION.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Long> field2() {
		return Evolution.EVOLUTION.V;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Timestamp> field3() {
		return Evolution.EVOLUTION.C;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Boolean> field4() {
		return Evolution.EVOLUTION.SUCCESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Timestamp> field5() {
		return Evolution.EVOLUTION.END;
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
	public Boolean value4() {
		return getSuccess();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Timestamp value5() {
		return getEnd();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EvolutionRecord value1(String value) {
		setId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EvolutionRecord value2(Long value) {
		setV(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EvolutionRecord value3(Timestamp value) {
		setC(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EvolutionRecord value4(Boolean value) {
		setSuccess(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EvolutionRecord value5(Timestamp value) {
		setEnd(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EvolutionRecord values(String value1, Long value2, Timestamp value3, Boolean value4, Timestamp value5) {
		value1(value1);
		value2(value2);
		value3(value3);
		value4(value4);
		value5(value5);
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached EvolutionRecord
	 */
	public EvolutionRecord() {
		super(Evolution.EVOLUTION);
	}

	/**
	 * Create a detached, initialised EvolutionRecord
	 */
	public EvolutionRecord(String id, Long v, Timestamp c, Boolean success, Timestamp end) {
		super(Evolution.EVOLUTION);

		setValue(0, id);
		setValue(1, v);
		setValue(2, c);
		setValue(3, success);
		setValue(4, end);
	}
}
