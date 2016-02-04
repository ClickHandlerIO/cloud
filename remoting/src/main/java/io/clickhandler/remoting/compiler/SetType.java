package io.clickhandler.remoting.compiler;

/**
 *
 */
public class SetType extends ArrayType {
    public SetType(StandardType componentType) {
        super(componentType);
    }

    @Override
    public DataType dataType() {
        return DataType.SET;
    }

    @Override
    public boolean isCollection() {
        return true;
    }
}
