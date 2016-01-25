package io.clickhandler.action;

import com.google.common.base.Joiner;
import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.*;
import javaslang.control.Try;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 */
public class ActionLocatorBuilder {
    private final static Logger LOG = LoggerFactory.getLogger(ActionLocatorBuilder.class);

    private String modelPackage;
    private String implPackage;

    public ActionLocatorBuilder(String modelPackage, String implPackage) {
        this.modelPackage = modelPackage;
        this.implPackage = implPackage;
    }

    public static void main(String[] args) throws Throwable {
//        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//        root.setLevel(Level.INFO);

        new ActionLocatorBuilder("model.action", "action").generate();
    }

    public void generate() {
        final Reflections modelReflections = new Reflections(modelPackage);
        final Reflections implReflections = new Reflections(implPackage);
        final Pkg rootPackage = new Pkg("", implPackage, null);

        final Set<Class<? extends Action>> classes = modelReflections.getSubTypesOf(Action.class);
        for (Class<? extends Action> cls : classes) {
            final TypeToken<? extends Action> actionToken = TypeToken.of(cls);
            final Class requestClass = actionToken.resolveType(Action.class.getTypeParameters()[0]).getRawType();
            final Class responseClass = actionToken.resolveType(Action.class.getTypeParameters()[1]).getRawType();

            System.err.println("Request = " + requestClass.getCanonicalName());
            System.err.println("Response = " + responseClass.getCanonicalName());

            // Find implementations.
            final Set impls = implReflections.getSubTypesOf(cls);

            if (impls == null || impls.isEmpty()) {
                LOG.warn("Action [" + cls.getCanonicalName() + "] has no implementation");
                continue;
            }

            // Did we find multiple Action implementations?
            if (impls.size() > 1) {
                // Let's warn.
                LOG.warn("Action [" + cls.getCanonicalName() + "] has multiple implementations \n" + Joiner.on('\n').join(impls.toArray()));
            }

            // Grab Impl.
            final Class impl = (Class) impls.iterator().next();
            // Should we warn about picking an implementation when multiple were found?
            if (impls.size() > 1) {
                LOG.warn("Action [" + cls.getCanonicalName() + "] is mapped to [" + impl.getCanonicalName() + "]");
            }

            final Package jpkg = impl.getPackage();
            String jpkgName = jpkg != null ? impl.getPackage().getName() : "";
            if (jpkgName.startsWith(implPackage)) {
                jpkgName = jpkgName.substring(implPackage.length());
            }
            if (jpkgName.startsWith(".")) {
                jpkgName = jpkgName.substring(1);
            }

            // Split package parts down.
            final String[] parts = jpkgName.split("[.]");
            final String firstName = parts[0];

            // Create Descriptor.
            final Descriptor descriptor = new Descriptor(cls, impl, requestClass, responseClass);

            // Is it a Root Action?
            if (parts.length == 1 && firstName.isEmpty()) {
                rootPackage.descriptors.put(descriptor.modelClass.getSimpleName(), descriptor);
            } else {
                // Let's find it's Package and construct the tree as needed during the process.
                Pkg parent = rootPackage;
                for (int i = 0; i < parts.length; i++) {
                    final String nextName = parts[i];
                    Pkg next = parent.children.get(nextName);
                    if (next == null) {
                        next = new Pkg(nextName, parent.path + "." + nextName, parent);
                        parent.children.put(nextName, next);
                    }
                    parent = next;
                }

                // Add Descriptor.
                parent.descriptors.put(descriptor.modelClass.getSimpleName(), descriptor);
            }
        }

        rootPackage.generateJava();

        System.out.println("");
    }

    public static class Pkg {
        public final String name;
        public final String path;
        public final Pkg parent;
        public final TreeMap<String, Pkg> children = new TreeMap<>();
        public final TreeMap<String, Descriptor> descriptors = new TreeMap<>();

        public Pkg(String name, String path, Pkg parent) {
            this.name = name;
            this.path = path;
            this.parent = parent;
        }

        public String getClassName() {
            return name == null || name.isEmpty() ? "Actions" : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        public void generateJava() {
            MethodSpec main = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String[].class, "args")
                .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
                .build();

            MethodSpec ctor = MethodSpec.constructorBuilder()
                .addAnnotation(AnnotationSpec.builder(Inject.class).build())
                .build();

            TypeSpec.Builder type = TypeSpec.classBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(ctor);

            // Generate SubPackage locators.
            for (Pkg childPackage : children.values()) {
                final TypeName typeName = ClassName.get(childPackage.path, childPackage.getClassName());
                type.addField(
                    FieldSpec.builder(
                        typeName,
                        childPackage.name
                    ).addAnnotation(AnnotationSpec.builder(Inject.class).build()).build()
                );

                type.addMethod(
                    MethodSpec.methodBuilder(childPackage.name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(typeName)
                        .addStatement("return " + childPackage.name)
                        .build()
                );
            }

            JavaFile javaFile = JavaFile.builder(path, type.build())
                .build();

            Try.run(() -> javaFile.writeTo(System.out));
        }
    }

    public static class Descriptor {
        public final Class modelClass;
        public final Class implClass;
        public final Class requestClass;
        public final Class responseClass;

        public Descriptor(Class modelClass, Class implClass, Class requestClass, Class responseClass) {
            this.modelClass = modelClass;
            this.implClass = implClass;
            this.requestClass = requestClass;
            this.responseClass = responseClass;
        }
    }
}
