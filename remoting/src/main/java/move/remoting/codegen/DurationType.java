package move.remoting.codegen;

import java.time.Duration;

/**
 *
 */
public class DurationType extends AbstractType {
    public DurationType() {
        super(Duration.class);
    }

    @Override
    public DataType dataType() {
        return DataType.DURATION;
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
