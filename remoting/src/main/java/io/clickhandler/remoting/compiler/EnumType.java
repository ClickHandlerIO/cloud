package io.clickhandler.remoting.compiler;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class EnumType extends AbstractType implements MaterializedType {
    private String[] values;
    private Namespace namespace;
    private String canonicalName;
    private final List<MaterializedType> children = new ArrayList<>(0);

    public EnumType(Class type, String[] values) {
        super(type);
        this.values = values;
    }

    @Override
    public String name() {
        return javaType().getSimpleName();
    }

    @Override
    public DataType dataType() {
        return DataType.ENUM;
    }

    @Override
    public boolean nullable() {
        return true;
    }

    public String[] values() {
        return this.values;
    }

    public String canonicalName() {
        return this.canonicalName;
    }

    public EnumType values(final String[] values) {
        this.values = values;
        return this;
    }

    public EnumType canonicalName(final String canonicalName) {
        this.canonicalName = canonicalName;
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

    public Namespace namespace() {
        return this.namespace;
    }

    public EnumType namespace(final Namespace namespace) {
        this.namespace = namespace;
        return this;
    }
}
