package io.clickhandler.remoting.codegen;

import java.util.Date;

/**
 *
 */
public class DateType extends AbstractType {
    public DateType() {
        super(Date.class);
    }

    @Override
    public DataType dataType() {
        return DataType.DATE;
    }

    @Override
    public boolean nullable() {
        return true;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }
}
