package move.remoting.codegen;

import java.time.LocalTime;

/**
 *
 */
public class LocalTimeType extends AbstractType {
    public LocalTimeType() {
        super(LocalTime.class);
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
