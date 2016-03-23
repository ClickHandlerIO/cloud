package io.clickhandler.action.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import io.clickhandler.action.*;

import javax.annotation.processing.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * Action annotation processor.
 *
 * @author Clay Molocznik
 */
@AutoService(Processor.class)
public class ActionProcessor extends AbstractProcessor {
    public static final String LOCATOR = "_Locator";
    public static final String LOCATOR_ROOT = "_LocatorRoot";
    private final TreeMap<String, ActionHolder> actionMap = new TreeMap<>();
    private final Pkg rootPackage = new Pkg("Action", "");

    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    /**
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    /**
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new LinkedHashSet<>();
        annotataions.add(RemoteAction.class.getCanonicalName());
        annotataions.add(QueueAction.class.getCanonicalName());
        annotataions.add(InternalAction.class.getCanonicalName());
        annotataions.add(StoreAction.class.getCanonicalName());
//        annotataions.add(ActionConfig.class.getCanonicalName());
        return annotataions;
    }

    /**
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            final Set<? extends Element> remoteElements = roundEnv.getElementsAnnotatedWith(RemoteAction.class);
            final Set<? extends Element> queueElements = roundEnv.getElementsAnnotatedWith(QueueAction.class);
            final Set<? extends Element> internalElements = roundEnv.getElementsAnnotatedWith(InternalAction.class);
            final Set<? extends Element> storeElements = roundEnv.getElementsAnnotatedWith(StoreAction.class);

            final HashSet<Element> elements = new HashSet<>();

            if (remoteElements != null) {
                elements.addAll(remoteElements);
            }
            if (queueElements != null) {
                elements.addAll(queueElements);
            }
            if (internalElements != null) {
                elements.addAll(internalElements);
            }

            boolean allGood = true;
            for (Element annotatedElement : elements) {
                final RemoteAction remoteAction = annotatedElement.getAnnotation(RemoteAction.class);
                final QueueAction queueAction = annotatedElement.getAnnotation(QueueAction.class);
                final InternalAction internalAction = annotatedElement.getAnnotation(InternalAction.class);
                final StoreAction storeAction = annotatedElement.getAnnotation(StoreAction.class);

                final TypeElement element = elementUtils.getTypeElement(annotatedElement.toString());

//                ActionConfig actionConfig;
//                try {
//                    actionConfig = annotatedElement.getAnnotation(ActionConfig.class);
//                } catch (Throwable e) {
//                    actionConfig = null;
//                }

                ActionHolder holder = actionMap.get(element.getQualifiedName().toString());

                if (holder == null) {
                    holder = new ActionHolder();
                    holder.type = element;
                }

//                holder.config = actionConfig;

                if (holder.remoteAction == null) {
                    holder.remoteAction = remoteAction;
                }
                if (holder.queueAction == null) {
                    holder.queueAction = queueAction;
                }
                if (holder.internalAction == null) {
                    holder.internalAction = internalAction;
                }
                if (holder.storeAction == null) {
                    holder.storeAction = storeAction;
                }

                // Ensure only 1 action annotation was used.
                int actionAnnotationCount = 0;
                if (holder.remoteAction != null) actionAnnotationCount++;
                if (holder.queueAction != null) actionAnnotationCount++;
                if (holder.internalAction != null) actionAnnotationCount++;
                if (holder.storeAction != null) actionAnnotationCount++;
                if (actionAnnotationCount > 1) {
                    messager.printMessage(Diagnostic.Kind.ERROR, element.getQualifiedName() + "  has multiple Action annotations. Only one of the following may be used... @RemoteAction or @QueueAction or @InternalAction");
                    continue;
                }

                // Make sure it's concrete.
                if (element.getModifiers().contains(Modifier.ABSTRACT)
                    || (element.getTypeParameters() != null && !element.getTypeParameters().isEmpty())) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "@RemoteAction was placed on a non-concrete class   " + element.getQualifiedName() + "   It cannot be abstract or have TypeParameters.");
                }

                if (holder.inType == null || holder.outType == null) {
                    final TypeParameterResolver typeParamResolver = new TypeParameterResolver(element);

                    holder.inType = typeParamResolver.resolve(Action.class, 0);
                    holder.outType = typeParamResolver.resolve(Action.class, 1);

                    messager.printMessage(Diagnostic.Kind.WARNING, element.getQualifiedName().toString());

                    if (holder.inType != null) {
                        messager.printMessage(Diagnostic.Kind.WARNING, "IN = " + holder.inType.getResolvedElement().getQualifiedName().toString());
                    }
                    if (holder.outType != null) {
                        messager.printMessage(Diagnostic.Kind.WARNING, "OUT = " + holder.outType.getResolvedElement().getQualifiedName().toString());
                    }
                }

                actionMap.put(element.getQualifiedName().toString(), holder);
            }

            // Build Packages.
            for (ActionHolder actionHolder : actionMap.values()) {
                String jpkgName = actionHolder.getPackage();

                // Split package parts down.
                final String[] parts = jpkgName.split("[.]");
                final String firstName = parts[0];

                messager.printMessage(Diagnostic.Kind.WARNING, "PKG: " + jpkgName);

                // Is it a Root Action?
                if (parts.length == 1 && firstName.isEmpty()) {
                    rootPackage.actions.put(actionHolder.getName(), actionHolder);
                } else {
                    // Let's find it's Package and construct the tree as needed during the process.
                    Pkg parent = rootPackage;
                    for (int i = 0; i < parts.length; i++) {
                        final String nextName = parts[i];
                        Pkg next = parent.children.get(nextName);
                        if (next == null) {
                            next = new Pkg(nextName, parent.path == null || parent.path.isEmpty() ? nextName : parent.path + "." + nextName, parent);
                            parent.children.put(nextName, next);
                        }
                        parent = next;
                    }

                    // Add Descriptor.
                    parent.actions.put(actionHolder.getName(), actionHolder);
                }
            }

            rootPackage.generateJava();
        } catch (Throwable e) {
            error(null, e.getMessage());
        }

        return true;
    }

    /**
     * Prints an error message
     *
     * @param e   The element which has caused the error. Can be null
     * @param msg The error message
     */
    public void error(Element e, String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    /**
     *
     */
    public class Pkg {
        public final String name;
        public final Pkg parent;
        public final TreeMap<String, Pkg> children = new TreeMap<>();
        public final TreeMap<String, ActionHolder> actions = new TreeMap<>();
        private final boolean root;
        public String path;
        private boolean processed = false;

        public Pkg(String name, String path) {
            this(name, path, null);
        }

        public Pkg(String name, String path, Pkg parent) {
            this.root = parent == null;
            this.name = name;
            this.path = path;
            this.parent = parent;
        }

        public String getFullPath() {
            if (path == null || path.isEmpty()) {
                return getClassName();
            } else {
                return path + "." + getClassName();
            }
        }

        public String getClassName() {
            return name == null || name.isEmpty() ? "Root" : Character.toUpperCase(name.charAt(0)) + name.substring(1) + (root ? LOCATOR_ROOT : LOCATOR);
        }

        public void generateJava() {
            if (processed) return;

            if (root) {
                if (children.isEmpty()) return;

                path = children.values().iterator().next().path;
                messager.printMessage(Diagnostic.Kind.WARNING, "Found ActionRoot Path: " + path);
            }

            processed = true;

            // Build empty @Inject constructor.
            final MethodSpec ctor = MethodSpec.constructorBuilder()
                .addAnnotation(Inject.class)
                .addModifiers(Modifier.PUBLIC)
                .build();

            // Init Class.
            final TypeSpec.Builder type = TypeSpec.classBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(TypeName.get(ActionLocator.class))
                .addAnnotation(Singleton.class)
                .addMethod(ctor);

            // We generate the code for "initActions()" and "initChildren()" as we process.
            final CodeBlock.Builder initActionsCode = CodeBlock.builder();
            final CodeBlock.Builder initChildrenCode = CodeBlock.builder();

            // Generate SubPackage locators.
            for (Pkg childPackage : children.values()) {
                // Build type name.
                final TypeName typeName = ClassName.get(childPackage.path, childPackage.getClassName());

                // Add field.
                type.addField(
                    FieldSpec.builder(typeName, childPackage.name)
                        .addAnnotation(Inject.class)
                        .build()
                );

                // Add code to initChildren() code.
                initChildrenCode.addStatement("children.add($L)", childPackage.name);

                // Add getter method.
                type.addMethod(
                    MethodSpec.methodBuilder(childPackage.name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(typeName)
                        .addStatement("return " + childPackage.name)
                        .build()
                );
            }

            // Go through all actions.
            actions.forEach((classPath, action) -> {
                // Get Action classname.
                ClassName actionName = ClassName.get(action.type);
                // Get Action IN resolved name.
                ClassName inName = ClassName.get(action.inType.getResolvedElement());
                // Get Action OUT resolved name.
                ClassName outName = ClassName.get(action.outType.getResolvedElement());

                TypeName actionProviderBuilder = ParameterizedTypeName.get(
                    action.getProviderTypeName(),
                    actionName,
                    inName,
                    outName
                );

                type.addField(
                    FieldSpec.builder(actionProviderBuilder, action.getFieldName())
                        .addAnnotation(Inject.class)
                        .build()
                );

                initActionsCode.addStatement("actionMap.put($T.class, $L)", action.type, action.getFieldName());

                type.addMethod(
                    MethodSpec.methodBuilder(action.getFieldName())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(actionProviderBuilder)
                        .addStatement("return " + action.getFieldName())
                        .build()
                );
            });

            // Add implemented "initActions()".
            type.addMethod(
                MethodSpec.methodBuilder("initActions")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(void.class)
                    .addCode(initActionsCode.build()).build()
            );

            // Add implemented "initChildren()".
            type.addMethod(
                MethodSpec.methodBuilder("initChildren")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(void.class)
                    .addCode(initChildrenCode.build()).build()
            );

            // Build java file.
            final JavaFile javaFile = JavaFile.builder(path, type.build()).build();

            try {
                // Write .java source code file.
                javaFile.writeTo(filer);
            } catch (Throwable e) {
                // Ignore.
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate Source File: " + e.getMessage());
            }

            // Generate child packages.
            for (Pkg childPackage : children.values()) {
                childPackage.generateJava();
            }
        }
    }
}
