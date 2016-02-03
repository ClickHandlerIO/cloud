package io.clickhandler.remoting.compiler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import io.clickhandler.action.RemoteActionProvider;
import javaslang.control.Match;
import javaslang.control.Try;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author Clay Molocznik
 */
public class RemotingAST {
    public final Map<Object, RemoteActionProvider<?, ?, ?>> providerMap;
    private final Map<Class, StandardType> types = new HashMap<>();
    private final Map<Class, ActionSpec> actionSpecs = new HashMap<>();
    private final TreeMap<String, ActionSpec> actionMap = new TreeMap<>();
    private final TreeMap<String, String> prefixMap;

    private final Map<String, Boolean> isClassMap = new HashMap<>();
    private final Namespace root = new Namespace().name("").canonicalName("");
    private final TreeMap<String, Namespace> namespaceMap = new TreeMap<>();

    /**
     * @param providerMap
     * @param prefixMap
     */
    public RemotingAST(Map<Object, RemoteActionProvider<?, ?, ?>> providerMap,
                       TreeMap<String, String> prefixMap) {
        this.providerMap = providerMap;
        this.prefixMap = prefixMap == null ? new TreeMap<>() : prefixMap;
        this.prefixMap.put("", "");
        construct();
    }

    /**
     * @param value
     * @return
     */
    private static String capitalize(String value) {
        if (value == null || value.isEmpty())
            return "";

        final String out = Character.toString(Character.toUpperCase(value.charAt(0)));
        return value.length() == 1 ? out : out + value.substring(1);
    }

    public Map<Class, StandardType> getTypes() {
        return types;
    }

    public Map<Class, ActionSpec> getActionSpecs() {
        return actionSpecs;
    }

    public TreeMap<String, ActionSpec> getActionMap() {
        return actionMap;
    }

    public TreeMap<String, String> getPrefixMap() {
        return prefixMap;
    }

    public Map<String, Boolean> getIsClassMap() {
        return isClassMap;
    }

    public Namespace getRoot() {
        return root;
    }

    public TreeMap<String, Namespace> getNamespaceMap() {
        return namespaceMap;
    }

    /**
     *
     */
    public void construct() {
        // Build ActionSpecs
        providerMap.forEach((key, value) -> {
            if (value != null) {
                actionSpecs.put(value.getActionClass(), new ActionSpec()
                    .provider(value)
                    .inSpec(buildType(value.getInClass()))
                    .outSpec(buildType(value.getOutClass())));
            }
        });


        actionSpecs.forEach((key, value) -> {
            value.name(key.getSimpleName());
            value.canonicalName(findCanonicalName(key.getCanonicalName()));
            final Namespace parent = findNamespace(value.canonicalName(), 1);
            value.parent(parent);
            parent.addChild(value);
            actionMap.put(value.canonicalName(), value);
            namespaceMap.put(value.canonicalName(), value);
        });

        // Build namespaces.

        final TreeMap<String, MaterializedType> sorted = new TreeMap<>();
        types.forEach((type, spec) -> {
            if (spec instanceof MaterializedType) {
                final MaterializedType materializedType = (MaterializedType) spec;
                final String canonicalName = findCanonicalName(type.getCanonicalName());
                final Namespace namespace = findNamespace(canonicalName, 1);

                materializedType
                    .namespace(namespace)
                    .canonicalName(canonicalName);

                namespace.types().add(materializedType);
                sorted.put(canonicalName, materializedType);
            }
        });

        sorted.keySet().forEach(System.out::println);
    }

    /**
     * @param canonicalName
     * @return
     */
    protected String findCanonicalName(String canonicalName) {
        final Map.Entry<String, String> prefix = prefixMap.floorEntry(canonicalName);

        if (prefix != null && !prefix.getKey().isEmpty() && canonicalName.startsWith(prefix.getKey())) {
            canonicalName = canonicalName.substring(prefix.getKey().length());
            canonicalName = prefix.getValue() + canonicalName;
        }

        return canonicalName;
    }

    /**
     * @param canonicalName
     * @param offset
     * @return
     */
    protected Namespace findNamespace(String canonicalName, int offset) {
        final String[] pathParts = canonicalName.split("[.]");
        Namespace parent = root;
        for (int i = 0; i < pathParts.length - offset; i++) {
            final String pathPart = pathParts[i];
            Namespace next = parent.children().get(pathPart);
            if (next == null) {
                next = new Namespace()
                    .name(pathPart)
                    .canonicalName(parent.name().isEmpty() ? pathPart : parent.path() + "." + pathPart)
                    .parent(parent);
                next.isClass(isClass(next.canonicalName()));
                parent.addChild(next);
                namespaceMap.put(next.canonicalName(), next);
            }
            parent = next;
        }
        return parent;
    }

    /**
     * @param canonicalName
     * @return
     */
    protected boolean isClass(String canonicalName) {
        if (isClassMap.containsKey(canonicalName))
            return isClassMap.get(canonicalName);

        final boolean isClass = Try.of(() -> Class.forName(canonicalName) != null).getOrElse(false);
        isClassMap.put(canonicalName, isClass);
        return isClass;
    }

    /**
     * @param type
     * @return
     */
    private StandardType buildType(Class type) {
        return buildType(type, null);
    }

    /**
     * @param type
     * @param genericType
     * @return
     */
    private StandardType buildType(Class type, Type genericType) {
        StandardType dataType = types.get(type);
        if (dataType != null) {
            return dataType;
        }

        if (type.isEnum()) {
            final Object[] enumConstants = type.getEnumConstants();
            final String[] vals;
            if (enumConstants != null) {
                vals = new String[enumConstants.length];
                for (int i = 0; i < enumConstants.length; i++)
                    vals[i] = enumConstants[i].toString();
            } else {
                vals = new String[0];
            }

            dataType = new EnumType(type, vals);
        } else if (type.isArray()) {
            dataType = new ArrayType(buildType(type.getComponentType()));
        } else if (List.class.isAssignableFrom(type)) {
            final Class componentType = TypeToken.of(genericType).resolveType(List.class.getTypeParameters()[0]).getRawType();
            dataType = new ListType(buildType(componentType));
        } else if (Set.class.isAssignableFrom(type)) {
            final Class componentType = TypeToken.of(genericType).resolveType(Set.class.getTypeParameters()[0]).getRawType();
            dataType = new SetType(buildType(componentType));
        } else if (Map.class.isAssignableFrom(type)) {
            final Class keyType = TypeToken.of(genericType).resolveType(Map.class.getTypeParameters()[0]).getRawType();
            final Class valueType = TypeToken.of(genericType).resolveType(Map.class.getTypeParameters()[1]).getRawType();
            dataType = new MapType(type, buildType(keyType), buildType(valueType));
        } else {
            dataType = Match.of(type)
                .whenIs(byte.class).then((StandardType) new PrimitiveType(byte.class, DataType.BYTE, false))
                .whenIs(Byte.class).then(new PrimitiveType(Byte.class, DataType.BYTE, true))
                .whenIs(boolean.class).then(new PrimitiveType(boolean.class, DataType.BOOLEAN, false))
                .whenIs(Boolean.class).then(new PrimitiveType(Boolean.class, DataType.BOOLEAN, true))
                .whenIs(short.class).then(new PrimitiveType(short.class, DataType.SHORT, false))
                .whenIs(Short.class).then(new PrimitiveType(Short.class, DataType.SHORT, true))
                .whenIs(char.class).then(new PrimitiveType(char.class, DataType.CHAR, false))
                .whenIs(Character.class).then(new PrimitiveType(Character.class, DataType.CHAR, true))
                .whenIs(int.class).then(new PrimitiveType(int.class, DataType.INT, false))
                .whenIs(Integer.class).then(new PrimitiveType(Integer.class, DataType.INT, true))
                .whenIs(long.class).then(new PrimitiveType(long.class, DataType.LONG, false))
                .whenIs(Long.class).then(new PrimitiveType(Long.class, DataType.LONG, true))
                .whenIs(float.class).then(new PrimitiveType(float.class, DataType.FLOAT, false))
                .whenIs(Float.class).then(new PrimitiveType(Float.class, DataType.FLOAT, true))
                .whenIs(double.class).then(new PrimitiveType(double.class, DataType.DOUBLE, false))
                .whenIs(Double.class).then(new PrimitiveType(Double.class, DataType.DOUBLE, true))
                .whenIs(Date.class).then(new DateType())
                .whenIs(String.class).then(new StringType())
                .whenIs(Object.class).then(new WildcardType())
                .whenIs(Enum.class).then(new WildcardType())
                .otherwise((StandardType) null)
                .get();

            if (dataType == null) {
                final ComplexType complexType = new ComplexType(type, buildType(type.getSuperclass()));
                dataType = complexType;
                final Field[] declaredFields = type.getDeclaredFields();
                for (Field field : declaredFields) {
                    final String name = field.getName();
                    final Class fieldClass = field.getType();
                    final Type fieldType = field.getGenericType();

                    JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);

                    final Method none = null;
                    final Method fluentGetter = Try.of(() -> type.getDeclaredMethod(name)).getOrElse(none);

                    Method getter = Try.of(() -> type.getDeclaredMethod("get" + capitalize(name))).getOrElse(none);
                    if (getter == null)
                        getter = Try.of(() -> type.getDeclaredMethod("is" + capitalize(name))).getOrElse(none);

                    if (jsonProperty == null && fluentGetter != null) {
                        jsonProperty = fluentGetter.getAnnotation(JsonProperty.class);
                    }
                    if (jsonProperty == null && getter != null) {
                        jsonProperty = getter.getAnnotation(JsonProperty.class);
                    }

                    String jsonName = jsonProperty != null ? Strings.nullToEmpty(jsonProperty.value()).trim() : "";

                    if (jsonName.isEmpty()) {
                        jsonName = name;
                    }

                    complexType.fields().add(new FieldSpec()
                        .field(field)
                        .name(name)
                        .type(buildType(fieldClass, fieldType))
                        .jsonName(jsonName)
                        .jsonProperty(jsonProperty));
                }
            }
        }

        types.put(type, dataType);
        return dataType;
    }
}
