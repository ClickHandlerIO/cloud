package io.clickhandler.remoting.codegen;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import io.clickhandler.action.Action;
import io.clickhandler.action.RemoteActionProvider;
import io.clickhandler.remoting.RemotingType;
import io.clickhandler.remoting.Push;
import javaslang.control.Try;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Clay Molocznik
 */
public class RemotingRegistry {
    public final Map<Object, RemoteActionProvider<?, ?, ?>> providerMap;
    final StandardType OBJECT_TYPE = new PrimitiveType(Object.class, DataType.WILDCARD, true);
    private final Map<Type, StandardType> types = new HashMap<>();
    private final Map<Class, ActionSpec> actionSpecs = new HashMap<>();
    private final TreeMap<String, ActionSpec> actionMap = new TreeMap<>();
    private final TreeMap<String, String> prefixMap;
    private final Map<String, Boolean> isClassMap = new HashMap<>();
    private final Namespace root = new Namespace().name("").canonicalName("");
    private final TreeMap<String, Namespace> namespaceMap = new TreeMap<>();
    private final Set<ComplexType> pushTypes = new HashSet<>();
    private String[] searchPackages = new String[]{"model", "io.clickhandler"};

    /**
     * @param providerMap
     * @param prefixMap
     */
    public RemotingRegistry(Map<Object, RemoteActionProvider<?, ?, ?>> providerMap,
                            TreeMap<String, String> prefixMap) {
        this.providerMap = providerMap;
        this.prefixMap = prefixMap == null ? new TreeMap<>() : prefixMap;
        this.prefixMap.put("", "");
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

    public Map<Type, StandardType> getTypes() {
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

    public String[] getSearchPackages() {
        return searchPackages;
    }

    public void setSearchPackages(String[] searchPackages) {
        this.searchPackages = searchPackages;
    }

    public Set<ComplexType> getPushTypes() {
        return pushTypes;
    }

    public boolean isPush(ComplexType type) {
        return pushTypes.contains(type);
    }

    /**
     *
     */
    public void construct() {
        // Sniff out Pushes.
        if (searchPackages != null) {
            for (String pkg : searchPackages) {
                loadPush(pkg);
            }

            for (String pkg : searchPackages) {
                loadExtraTypes(pkg);
            }
        }

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

        final List<MaterializedType> nonStaticTypes = new ArrayList<>();

        final TreeMap<String, MaterializedType> sorted = new TreeMap<>();
        types.forEach((type, spec) -> {
            if (spec instanceof MaterializedType) {
                final MaterializedType materializedType = (MaterializedType) spec;
                final String canonicalName = findCanonicalName(TypeToken.of(type).getRawType().getCanonicalName());
                final Namespace namespace = findNamespace(canonicalName, 1);

                materializedType
                    .namespace(namespace)
                    .canonicalName(canonicalName);

                // Ensure it's not an embedded type on an action.
                if (!namespaceMap.containsKey(materializedType.canonicalName())) {
                    namespace.types().add(materializedType);
                }

                if (materializedType.namespace().isClass() && !Modifier.isStatic(materializedType.javaType().getModifiers())) {
                    nonStaticTypes.add(materializedType);
                }

                sorted.put(canonicalName, materializedType);
            }
        });

        if (!nonStaticTypes.isEmpty()) {
            nonStaticTypes.forEach(t -> System.err.println("Type [" + t.javaType().getCanonicalName() + "] needs the 'static' modifier."));
            throw new RuntimeException("Invalid Types found");
        }

        sorted.keySet().forEach(System.out::println);
    }

    private void loadPush(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Push.class);
        if (classes == null || classes.isEmpty())
            return;

        classes.forEach(c -> {
            final StandardType type = buildType(c);
            if (type == null) return;

            if (type instanceof ComplexType) {
                pushTypes.add((ComplexType) type);
                types.put(type.javaType(), type);
            }
        });
    }

    private void loadExtraTypes(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RemotingType.class);
        if (classes == null || classes.isEmpty())
            return;

        classes.forEach(c -> {
            final StandardType type = buildType(c);
            if (type == null) return;

            if (type instanceof ComplexType) {
                types.put(type.javaType(), type);
            }
        });
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
        try {
            StandardType dataType = genericType != null ? types.get(genericType) : types.get(type);
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
            } else if (javaslang.collection.Set.class.isAssignableFrom(type)) {
                final Class componentType = TypeToken.of(genericType).resolveType(javaslang.collection.Set.class.getTypeParameters()[0]).getRawType();
                dataType = new ListType(buildType(componentType));
            } else if (javaslang.collection.Seq.class.isAssignableFrom(type)) {
                final Class componentType = TypeToken.of(genericType).resolveType(javaslang.collection.Seq.class.getTypeParameters()[0]).getRawType();
                dataType = new ListType(buildType(componentType));
            } else if (List.class.isAssignableFrom(type)) {
                final Class componentType = TypeToken.of(genericType).resolveType(List.class.getTypeParameters()[0]).getRawType();
                dataType = new ListType(buildType(componentType));
            } else if (Set.class.isAssignableFrom(type)) {
                final Class componentType = TypeToken.of(genericType).resolveType(Set.class.getTypeParameters()[0]).getRawType();
                dataType = new SetType(buildType(componentType));
            } else if (Collection.class.isAssignableFrom(type)) {
                final Class componentType = TypeToken.of(genericType).resolveType(List.class.getTypeParameters()[0]).getRawType();
                dataType = new ListType(buildType(componentType));
            } else if (Map.class.isAssignableFrom(type)) {
                final Class keyType = TypeToken.of(genericType).resolveType(Map.class.getTypeParameters()[0]).getRawType();
                final Class valueType = TypeToken.of(genericType).resolveType(Map.class.getTypeParameters()[1]).getRawType();
                dataType = new MapType(type, buildType(keyType), buildType(valueType));
            } else {
                if (type == byte.class)
                    dataType = new PrimitiveType(byte.class, DataType.BYTE, false);
                else if (type == Byte.class)
                    dataType = new PrimitiveType(Byte.class, DataType.BYTE, true);
                else if (type == boolean.class)
                    dataType = new PrimitiveType(boolean.class, DataType.BOOLEAN, false);
                else if (type == Boolean.class)
                    dataType = new PrimitiveType(Boolean.class, DataType.BOOLEAN, true);
                else if (type == short.class)
                    dataType = new PrimitiveType(short.class, DataType.SHORT, false);
                else if (type == Short.class)
                    dataType = new PrimitiveType(Short.class, DataType.SHORT, true);
                else if (type == char.class)
                    dataType = new PrimitiveType(char.class, DataType.CHAR, false);
                else if (type == Character.class)
                    dataType = new PrimitiveType(Character.class, DataType.CHAR, true);
                else if (type == int.class)
                    dataType = new PrimitiveType(int.class, DataType.INT, false);
                else if (type == Integer.class)
                    dataType = new PrimitiveType(Integer.class, DataType.INT, true);
                else if (type == long.class)
                    dataType = new PrimitiveType(long.class, DataType.LONG, false);
                else if (type == Long.class)
                    dataType = new PrimitiveType(Long.class, DataType.LONG, true);
                else if (type == float.class)
                    dataType = new PrimitiveType(float.class, DataType.FLOAT, false);
                else if (type == Float.class)
                    dataType = new PrimitiveType(Float.class, DataType.FLOAT, true);
                else if (type == double.class)
                    dataType = new PrimitiveType(double.class, DataType.DOUBLE, false);
                else if (type == Double.class)
                    dataType = new PrimitiveType(Double.class, DataType.DOUBLE, true);
                else if (type == Date.class)
                    dataType = new DateType();
                else if (type == LocalDateTime.class)
                    dataType = new DateTimeType();
                else if (type == String.class)
                    dataType = new StringType();
                else if (type == Object.class)
                    dataType = new WildcardType();
                else if (type == Enum.class)
                    dataType = new WildcardType();

                if (dataType == null) {
                    if (Action.class.isAssignableFrom(type)) {
                        dataType = new ComplexType(type);
                    } else {
                        final ComplexType complexType = new ComplexType(type);
                        dataType = complexType;
                        if (genericType != null) {
                            types.put(genericType, complexType);
                        } else {
                            types.put(type, complexType);
                        }

                        complexType.setSuperType(buildType(type.getSuperclass()));

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
            }

            if (genericType != null) {
                types.put(genericType, dataType);
            } else {
                types.put(type, dataType);
            }

            return dataType;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
