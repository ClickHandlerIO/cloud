package move.remoting.codegen;

import java.util.List;

/**
 * A Materialized Type is a type that requires code generation.
 */
public interface MaterializedType extends StandardType {
    /**
     * @return
     */
    Namespace namespace();

    /**
     * @param namespace
     * @return
     */
    MaterializedType namespace(Namespace namespace);

    /**
     * @return
     */
    String name();

    /**
     * @return
     */
    String canonicalName();

    /**
     * @param canonicalName
     * @return
     */
    MaterializedType canonicalName(String canonicalName);

    /**
     * @return
     */
    List<MaterializedType> children();

    /**
     * @return
     */
    String path();
}
