package io.clickhandler.remoting.compiler;

/**
 *
 */
public abstract class AbstractType implements StandardType {
    private final Class typeClass;

    public AbstractType(Class typeClass) {
        this.typeClass = typeClass;
    }

    @Override
    public Class javaType() {
        return typeClass;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public String canonicalName() {
        return typeClass.getCanonicalName();
    }
}
