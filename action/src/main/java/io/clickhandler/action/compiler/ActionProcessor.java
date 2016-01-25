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
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;

/**
 *
 */
@AutoService(Processor.class)
public class ActionProcessor extends AbstractProcessor {
    public static final String LOCATOR = "_Locator";
    public static final String LOCATOR_ROOT = "_LocatorRoot";
    private final TreeMap<String, ActionHolder> actionMap = new TreeMap<>();
    private final Pkg rootPackage = new Pkg("Action", "", null);

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

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new LinkedHashSet<>();
        annotataions.add(RemoteAction.class.getCanonicalName());
        annotataions.add(QueueAction.class.getCanonicalName());
        annotataions.add(ActionConfig.class.getCanonicalName());
        return annotataions;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {

            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(RemoteAction.class)) {
                final RemoteAction remoteAction = annotatedElement.getAnnotation(RemoteAction.class);

                final TypeElement element = elementUtils.getTypeElement(annotatedElement.toString());

                ActionHolder holder = actionMap.get(element.getQualifiedName().toString());

                if (holder == null) {
                    holder = new ActionHolder();
                    holder.remoteAction = remoteAction;
                    holder.type = element;
                    actionMap.put(element.getQualifiedName().toString(), holder);
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
                        messager.printMessage(Diagnostic.Kind.WARNING, "IN = " + holder.inType.varTypeElement.getQualifiedName().toString());
                    }
                    if (holder.outType != null) {
                        messager.printMessage(Diagnostic.Kind.WARNING, "OUT = " + holder.outType.varTypeElement.getQualifiedName().toString());
                    }
                }
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

    private static class DeclaredTypeVar {
        private final Map<String, TypeVarImpl> resolvedMap = new HashMap<>();
        private DeclaredType type;
        private TypeElement element;
        private int index;
        private String varName;
        private TypeMirror varType;
        private TypeElement varTypeElement;

        public DeclaredTypeVar(DeclaredType type, TypeElement element, int index, String varName, TypeMirror varType) {
            this.type = type;
            this.element = element;
            this.index = index;
            this.varName = varName;
            this.varType = varType;

            if (varType instanceof DeclaredType) {
                this.varTypeElement = (TypeElement) ((DeclaredType) varType).asElement();
            }
        }

        public void resolve() {
            // Find 'varName' on the SuperClass or any Interfaces.
            final TypeMirror superClass = element.getSuperclass();

            if (superClass != null) {
                search(varName, superClass);
            }
        }

        private void search(String varName, TypeMirror mirror) {
            if (mirror == null || !(mirror instanceof DeclaredType)) {
                return;
            }

            final DeclaredType type = (DeclaredType) mirror;
            final TypeElement element = (TypeElement) type.asElement();
            final List<? extends TypeMirror> typeArgs = type.getTypeArguments();
            if (typeArgs == null || typeArgs.isEmpty()) {
                return;
            }

            for (int i = 0; i < typeArgs.size(); i++) {
                final TypeMirror typeArg = typeArgs.get(i);

                if (typeArg.getKind() == TypeKind.TYPEVAR
                    && typeArg.toString().equals(varName)) {
                    // Found it.
                    // Get TypeVariable name.
                    varName = element.getTypeParameters().get(i).getSimpleName().toString();

                    // Add to resolved map.
                    resolvedMap.put(element.toString(), new TypeVarImpl(type, element, i, varName));

                    // Go up to the SuperClass.
                    search(varName, element.getSuperclass());

                    final List<? extends TypeMirror> interfaces = element.getInterfaces();
                    if (interfaces != null && !interfaces.isEmpty()) {
                        for (TypeMirror iface : interfaces) {
                            search(varName, iface);
                        }
                    }
                }
            }
        }
    }

    private static class TypeVarImpl {
        private DeclaredType type;
        private TypeElement element;
        private int index;
        private String varName;

        public TypeVarImpl(DeclaredType type, TypeElement element, int index, String varName) {
            this.type = type;
            this.element = element;
            this.index = index;
            this.varName = varName;
        }
    }

    private static class TypeParameterResolver {
        private final TypeElement element;
        private final List<DeclaredTypeVar> vars = new ArrayList<>();
        private boolean resolved;

        public TypeParameterResolver(TypeElement element) {
            this.element = element;
        }

        public void resolve() {
            if (resolved) {
                return;
            }

            resolveDeclaredTypeVars(element.getSuperclass());

            final List<? extends TypeMirror> interfaces = element.getInterfaces();
            if (interfaces != null && !interfaces.isEmpty()) {
                for (TypeMirror iface : interfaces) {
                    resolveDeclaredTypeVars(iface);
                }
            }

            resolved = true;
        }

        public DeclaredTypeVar resolve(Class cls, int typeVarIndex) {
            if (!resolved) {
                resolve();
            }
            for (DeclaredTypeVar var : vars) {
                final TypeVarImpl actionTypeVar = var.resolvedMap.get(cls.getName());
                if (actionTypeVar != null) {
                    if (actionTypeVar.index == typeVarIndex) {
                        return var;
                    }
                }
            }
            return null;
        }

        private void resolveDeclaredTypeVars(TypeMirror type) {
            if (type == null) {
                return;
            }

            if (type.getKind() != TypeKind.DECLARED) {
                return;
            }

            if (type.toString().equals(Object.class.getName())) {
                return;
            }

            if (!(type instanceof DeclaredType)) {
                return;
            }

            final DeclaredType declaredType = (DeclaredType) type;
            final TypeElement element = ((TypeElement) ((DeclaredType) type).asElement());

            if (element.getQualifiedName().toString().equals(Object.class.getName())) {
                return;
            }

            final List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
            if (typeArgs != null && !typeArgs.isEmpty()) {
                for (int i = 0; i < typeArgs.size(); i++) {
                    final TypeMirror typeArg = typeArgs.get(i);

                    if (typeArg.getKind() == TypeKind.DECLARED) {
                        final List<? extends TypeParameterElement> typeParams = ((TypeElement) ((DeclaredType) type).asElement()).getTypeParameters();
                        final String typeVarName = typeParams.get(i).getSimpleName().toString();

                        final DeclaredTypeVar declaredTypeVar = new DeclaredTypeVar(declaredType, element, i, typeVarName, typeArg);
                        declaredTypeVar.resolve();
                        vars.add(declaredTypeVar);
                    }
                }
            }

            resolveDeclaredTypeVars(element.getSuperclass());

            final List<? extends TypeMirror> interfaces = element.getInterfaces();
            if (interfaces != null && !interfaces.isEmpty()) {
                for (TypeMirror iface : interfaces) {
                    resolveDeclaredTypeVars(iface);
                }
            }
        }
    }

    public static class ActionHolder {
        RemoteAction remoteAction;
        QueueAction queueAction;
        ActionConfig config;
        TypeElement type;
        DeclaredTypeVar inType;
        DeclaredTypeVar outType;

        String pkgName = null;

        public String getName() {
            return type.getQualifiedName().toString();
        }

        public String getFieldName() {
            final String name = type.getSimpleName().toString();
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        public String getPackage() {
            if (pkgName != null) {
                return pkgName;
            }

            final String qname = type.getQualifiedName().toString();

            final String[] parts = qname.split("[.]");
            if (parts.length == 1) {
                return pkgName = "";
            } else {
                final String lastPart = parts[parts.length - 1];
                return pkgName = qname.substring(0, qname.length() - lastPart.length() - 1);
            }
        }
    }

    public class Pkg {
        public final String name;
        public final Pkg parent;
        public final TreeMap<String, Pkg> children = new TreeMap<>();
        public final TreeMap<String, ActionHolder> actions = new TreeMap<>();
        public String path;
        private boolean processed = false;

        public Pkg(String name, String path, Pkg parent) {
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
            return name == null || name.isEmpty() ? "Root" : Character.toUpperCase(name.charAt(0)) + name.substring(1) + (this == rootPackage ? LOCATOR_ROOT : LOCATOR);
        }

        public void generateJava() {
            if (processed) {
                return;
            }
            processed = true;

            if (this == rootPackage) {
                if (children.isEmpty()) {
                    return;
                }

                path = children.values().iterator().next().path;

                messager.printMessage(Diagnostic.Kind.WARNING, "Found ActionRoot Path: " + path);
            }

            MethodSpec ctor = MethodSpec.constructorBuilder()
                .addAnnotation(AnnotationSpec.builder(Inject.class).build())
                .addModifiers(Modifier.PUBLIC)
                .build();

            TypeSpec.Builder type = TypeSpec.classBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(TypeName.get(ActionLocator.class))
                .addAnnotation(Singleton.class)
                .addMethod(ctor);

            final CodeBlock.Builder initActionsCode = CodeBlock.builder();
            final CodeBlock.Builder initChildrenCode = CodeBlock.builder();

            // Generate SubPackage locators.
            for (Pkg childPackage : children.values()) {
                final TypeName typeName = ClassName.get(childPackage.path, childPackage.getClassName());

                type.addField(
                    FieldSpec.builder(
                        typeName,
                        childPackage.name
                    ).addAnnotation(AnnotationSpec.builder(Inject.class).build()).build()
                );

                initChildrenCode.addStatement("children.add($L)", childPackage.name);

                type.addMethod(
                    MethodSpec.methodBuilder(childPackage.name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(typeName)
                        .addStatement("return " + childPackage.name)
                        .build()
                );
            }

            actions.forEach((classPath, action) -> {
                ClassName actionName = ClassName.get(action.type);
                ClassName inName = ClassName.get(action.inType.varTypeElement);
                ClassName outName = ClassName.get(action.outType.varTypeElement);

                TypeName actionProvider = ParameterizedTypeName.get(
                    ClassName.get(RemoteActionProvider.class),
                    actionName,
                    inName,
                    outName
                );

                type.addField(
                    FieldSpec.builder(
                        actionProvider, action.getFieldName()
                    ).addAnnotation(Inject.class).build()
                );

                initActionsCode.addStatement("actionMap.put($T.class, $L)", action.type, action.getFieldName());
                initActionsCode.addStatement("actionMap.put($T.class, $L)", action.inType.varTypeElement, action.getFieldName());
                initActionsCode.addStatement("actionMap.put($T.class, $L)", action.outType.varTypeElement, action.getFieldName());

                type.addMethod(
                    MethodSpec.methodBuilder(action.getFieldName())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(actionProvider)
                        .addStatement("return " + action.getFieldName())
                        .build()
                );
            });

            type.addMethod(
                MethodSpec.methodBuilder("initActions")
                    .addModifiers(Modifier.PROTECTED)
                    .returns(void.class)
                    .addCode(initActionsCode.build()).build()
            );

            type.addMethod(
                MethodSpec.methodBuilder("initChildren")
                    .addModifiers(Modifier.PROTECTED)
                    .returns(void.class)
                    .addCode(initChildrenCode.build()).build()
            );

            JavaFile javaFile = JavaFile.builder(path, type.build()).build();

            try {
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
