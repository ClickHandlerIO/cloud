package move.remoting.codegen;

/**
 *
 */
public class PrimitiveType extends AbstractType {
    private final DataType code;
    private final boolean nullable;

    public PrimitiveType(Class type, DataType code, boolean nullable) {
        super(type);
        this.code = code;
        this.nullable = nullable;
    }

    @Override
    public DataType dataType() {
        return code;
    }

    @Override
    public boolean nullable() {
        return nullable;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }
}
