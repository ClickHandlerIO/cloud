package move.remoting.codegen;

import java.time.LocalDateTime;

/**
 *
 */
public class DateTimeType extends AbstractType {
    public DateTimeType() {
        super(LocalDateTime.class);
    }

    @Override
    public DataType dataType() {
        return DataType.DATETIME;
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
