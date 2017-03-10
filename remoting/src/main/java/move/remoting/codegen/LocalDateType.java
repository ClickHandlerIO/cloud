package move.remoting.codegen;

import java.time.LocalDate;

/**
 *
 */
public class LocalDateType extends AbstractType {
    public LocalDateType() {
        super(LocalDate.class);
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
