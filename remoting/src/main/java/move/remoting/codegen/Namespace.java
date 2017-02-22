package move.remoting.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class Namespace {
    private final Map<String, Namespace> children = new TreeMap<>();
    private final List<MaterializedType> types = new ArrayList<>();
    private boolean isClass;
    private ComplexType type;
    private String name;
    private String canonicalName;
    private Namespace parent;

    public void addChild(Namespace namespace) {
        namespace.parent = this;
        children.put(namespace.name(), namespace);
    }

    public boolean isClass() {
        return this.isClass;
    }

    public String name() {
        return this.name;
    }

    public String canonicalName() {
        return this.canonicalName;
    }

    public Namespace parent() {
        return this.parent;
    }

    public Namespace isClass(final boolean isClass) {
        this.isClass = isClass;
        return this;
    }

    public Namespace name(final String name) {
        this.name = name;
        return this;
    }

    public Namespace canonicalName(final String canonicalName) {
        this.canonicalName = canonicalName;
        return this;
    }

    public Namespace parent(final Namespace parent) {
        this.parent = parent;
        return this;
    }

    public ComplexType type() {
        return this.type;
    }

    public Namespace type(final ComplexType type) {
        this.type = type;
        return this;
    }

    public String path() {
        return canonicalName();
    }

    public Map<String, Namespace> children() {
        return children;
    }

    public List<MaterializedType> types() {
        return types;
    }

    public boolean hasActions() {
        for (Namespace namespace : children.values()) {
            if (namespace instanceof ActionSpec || namespace.hasActions()) {
                return true;
            }
        }

        return false;
    }
}
