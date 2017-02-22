package move.remoting.codegen;

/**
 *
 */
public class ArrayType extends AbstractType {
    private StandardType componentType;

    public ArrayType(StandardType type) {
        super(type.javaType());
        this.componentType = type;
    }

    public StandardType componentType() {
        return componentType;
    }

    @Override
    public DataType dataType() {
        return DataType.ARRAY;
    }

    @Override
    public boolean nullable() {
        return true;
    }
}
