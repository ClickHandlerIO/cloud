package io.clickhandler.remoting.codegen;

/**
 *
 */
public class ListType extends ArrayType {
    public ListType(StandardType componentType) {
        super(componentType);
    }

    @Override
    public DataType dataType() {
        return DataType.LIST;
    }

    @Override
    public boolean isCollection() {
        return true;
    }
}
