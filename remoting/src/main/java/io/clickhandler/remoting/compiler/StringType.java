package io.clickhandler.remoting.compiler;

/**
 *
 */
public class StringType extends AbstractType {
    public StringType() {
        super(String.class);
    }

    @Override
    public DataType dataType() {
        return DataType.STRING;
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
