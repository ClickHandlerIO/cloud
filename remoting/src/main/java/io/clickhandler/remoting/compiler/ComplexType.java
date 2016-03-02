package io.clickhandler.remoting.compiler;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ComplexType extends AbstractType implements MaterializedType {
    private final List<FieldSpec> fields = new ArrayList<>();
    private final List<MaterializedType> children = new ArrayList<>(0);
    private Namespace namespace;
    private StandardType superType;
    private StandardType[] interfaces;
    private String canonicalName;
    private boolean processed;

    public ComplexType(Class typeClass) {
        super(typeClass);
    }

    public ComplexType(Class type, StandardType superType) {
        super(type);
        this.superType = superType;
    }

    void setSuperType(StandardType superType) {
        this.superType = superType;
    }

    @Override
    public DataType dataType() {
        return DataType.COMPLEX;
    }

    @Override
    public boolean nullable() {
        return true;
    }

    public StandardType superType() {
        return this.superType;
    }

    public StandardType[] interfaces() {
        return this.interfaces;
    }

    public ComplexType superType(final StandardType superType) {
        this.superType = superType;
        return this;
    }

    public ComplexType interfaces(final StandardType[] interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    public Namespace namespace() {
        return this.namespace;
    }

    public String canonicalName() {
        return this.canonicalName;
    }

    public boolean processed() {
        return this.processed;
    }

    public ComplexType namespace(final Namespace namespace) {
        this.namespace = namespace;
        return this;
    }

    @Override
    public String name() {
        return javaType().getSimpleName();
    }

    public ComplexType canonicalName(final String canonicalName) {
        this.canonicalName = canonicalName;
        return this;
    }

    public ComplexType processed(final boolean processed) {
        this.processed = processed;
        return this;
    }

    @Override
    public List<MaterializedType> children() {
        return children;
    }

    @Override
    public String path() {
        return namespace().canonicalName();
    }

    public List<FieldSpec> fields() {
        return fields;
    }
}
