package io.clickhandler.remoting.compiler;

/**
 *
 */
public class MapType extends AbstractType {
    private StandardType keyType;
    private StandardType valueType;

    public MapType(Class type, StandardType keyType, StandardType valueType) {
        super(type);
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public StandardType keyType() {
        return this.keyType;
    }

    public StandardType valueType() {
        return this.valueType;
    }

    public MapType keyType(final StandardType keyType) {
        this.keyType = keyType;
        return this;
    }

    public MapType valueType(final StandardType valueType) {
        this.valueType = valueType;
        return this;
    }


    @Override
    public DataType dataType() {
        return DataType.MAP;
    }

    @Override
    public boolean nullable() {
        return true;
    }

    @Override
    public boolean isCollection() {
        return true;
    }
}
