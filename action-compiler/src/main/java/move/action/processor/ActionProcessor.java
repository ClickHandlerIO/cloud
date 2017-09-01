package move.action.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import move.action.ActionLocator;
import move.action.ActionPackage;
import move.action.ActionProvider;
import move.action.FifoWorkerActionProvider;
import move.action.IAction;
import move.action.Internal;
import move.action.InternalActionProvider;
import move.action.Remote;
import move.action.RemoteActionProvider;
import move.action.Scheduled;
import move.action.ScheduledActionProvider;
import move.action.Worker;
import move.action.WorkerActionProvider;

/**
 * Action annotation processor.
 *
 * @author Clay Molocznik
 */
@AutoService(Processor.class)
public class ActionProcessor extends AbstractProcessor {

  static final String LOCATOR = "_Locator";
  static final String LOCATOR_ROOT = "_LocatorRoot";
  static final ParameterizedTypeName ACTION_PROVIDER_NAME = ParameterizedTypeName.get(
      ClassName.bestGuess("move.action.ActionProvider"),
      WildcardTypeName.subtypeOf(TypeName.OBJECT),
      WildcardTypeName.subtypeOf(TypeName.OBJECT),
      WildcardTypeName.subtypeOf(TypeName.OBJECT)
  );
  static ClassName VERTX_CLASSNAME = ClassName.bestGuess(
      "io.vertx.rxjava.core.Vertx"
  );

  final TreeMap<String, ActionHolder> actionMap = new TreeMap<>();
  final Pkg rootPackage = new Pkg("Action", "");
  final ArrayList<ActionPackage> actionPackages = new ArrayList<>();
  Types typeUtils;
  Elements elementUtils;
  Filer filer;
  Messager messager;

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
    final Set<String> annotataions = new LinkedHashSet<>();
//    annotataions.add(ActionPackage.class.getCanonicalName());
    annotataions.add(Remote.class.getCanonicalName());
    annotataions.add(Internal.class.getCanonicalName());
    annotataions.add(Worker.class.getCanonicalName());
    annotataions.add(Scheduled.class.getCanonicalName());
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
      final Set<? extends Element> remoteElements = roundEnv
          .getElementsAnnotatedWith(Remote.class);
      final Set<? extends Element> internalElements = roundEnv
          .getElementsAnnotatedWith(Internal.class);
      final Set<? extends Element> workerElements = roundEnv
          .getElementsAnnotatedWith(Worker.class);
      final Set<? extends Element> scheduledElements = roundEnv
          .getElementsAnnotatedWith(Scheduled.class);

      final HashSet<Element> elements = new HashSet<>();

      if (remoteElements != null) {
        elements.addAll(remoteElements);
      }
      if (internalElements != null) {
        elements.addAll(internalElements);
      }
      if (workerElements != null) {
        elements.addAll(workerElements);
      }
      if (scheduledElements != null) {
        elements.addAll(scheduledElements);
      }

//            final Set<? extends Element> packageElements = roundEnv.getElementsAnnotatedWith(ActionPackage.class);
//
//            if (packageElements != null) {
//                packageElements.forEach(e -> actionPackages.add(e.getAnnotation(ActionPackage.class)));
//            }
//
//            if (roundEnv.processingOver() && actionPackages.isEmpty() && !actionMap.isEmpty()) {
////                messager.printMessage(Diagnostic.Kind.ERROR, "@ActionPackage on package-info.java not found");
//            }
//
//            if (packageElements != null) {
//
//                messager.printMessage(Diagnostic.Kind.WARNING, packageElements.size() + " @ActionPackage found");
//                packageElements.forEach(p -> messager.printMessage(Diagnostic.Kind.WARNING, "@ActionPackage(" + p.getAnnotation(ActionPackage.class).value() + ")"));
//            }

      for (Element annotatedElement : elements) {
        final Remote remote = annotatedElement
            .getAnnotation(Remote.class);
        final Internal internal = annotatedElement
            .getAnnotation(Internal.class);
        final Worker worker = annotatedElement
            .getAnnotation(Worker.class);
        final Scheduled scheduled = annotatedElement
            .getAnnotation(Scheduled.class);

        final TypeElement element = elementUtils.getTypeElement(annotatedElement.toString());

        ActionHolder holder = actionMap.get(element.getQualifiedName().toString());

        if (holder == null) {
          final TypeParameterResolver typeParamResolver = TypeParameterResolver.resolve(element);

          DeclaredTypeVar requestType = null;
          try {
            requestType = typeParamResolver.resolve(IAction.class, 0);
          } catch (Throwable e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
          }

          DeclaredTypeVar responseType = null;
          try {
            responseType = typeParamResolver.resolve(IAction.class, 1);
          } catch (Throwable e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
          }

          holder = new ActionHolder(element, requestType, responseType, remote, internal, worker,
              scheduled);
        }

        // Ensure only 1 action annotation was used.
        int actionAnnotationCount = 0;
        if (holder.remote != null) {
          actionAnnotationCount++;
        }
        if (holder.internal != null) {
          actionAnnotationCount++;
        }
        if (holder.worker != null) {
          actionAnnotationCount++;
        }
        if (holder.scheduled != null) {
          actionAnnotationCount++;
        }
        if (actionAnnotationCount > 1) {
          messager.printMessage(
              Diagnostic.Kind.ERROR,
              element.getQualifiedName() +
                  "  has multiple Action annotations. Only one of the following may be used... " +
                  "@Remote or @QueueAction or @Internal or @ActorAction"
          );
          continue;
        }

        // Make sure it's concrete.
        if (element.getModifiers().contains(Modifier.ABSTRACT)
            || (element.getTypeParameters() != null && !element.getTypeParameters().isEmpty())) {
          messager.printMessage(
              Diagnostic.Kind.ERROR,
              "@Remote was placed on a non-concrete class " +
                  element.getQualifiedName() +
                  " It cannot be abstract or have TypeParameters."
          );
        }

//        if (holder.requestType == null || holder.replyType == null) {
//          final TypeParameterResolver typeParamResolver = new TypeParameterResolver(element);
//
//          try {
//            holder.requestType = typeParamResolver.resolve(IAction.class, 0);
//          } catch (Throwable e) {
//            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
//          }
//
//          try {
//            holder.replyType = typeParamResolver.resolve(IAction.class, 1);
//          } catch (Throwable e) {
//            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
//          }
//
//          messager.printMessage(Diagnostic.Kind.WARNING, element.getQualifiedName().toString());
//
//          if (holder.requestType != null) {
////                        messager.printMessage(Diagnostic.Kind.WARNING, "IN = " + holder.requestType.getResolvedElement().getQualifiedName().toString());
//          }
//          if (holder.replyType != null) {
////                        messager.printMessage(Diagnostic.Kind.WARNING, "OUT = " + holder.replyType.getResolvedElement().getQualifiedName().toString());
//          }
//        }

        actionMap.put(element.getQualifiedName().toString(), holder);
      }

      // Build Packages.
      for (ActionHolder actionHolder : actionMap.values()) {
        String jpkgName = actionHolder.pkgName;

        // Split package parts down.
        final String[] parts = jpkgName.split("[.]");
        final String firstName = parts[0];

//                messager.printMessage(Diagnostic.Kind.WARNING, "PKG: " + jpkgName);

        // Is it a Root Action?
        if (parts.length == 1 && firstName.isEmpty()) {
          rootPackage.actions.put(actionHolder.name, actionHolder);
        } else {
          // Let's find it's Package and construct the tree as needed during the process.
          Pkg parent = rootPackage;
          for (int i = 0; i < parts.length; i++) {
            final String nextName = parts[i];
            Pkg next = parent.children.get(nextName);
            if (next == null) {
              next = new Pkg(nextName, parent.path == null || parent.path.isEmpty()
                  ? nextName
                  : parent.path + "." + nextName, parent);
              parent.children.put(nextName, next);
            }
            parent = next;
          }

          // Add Descriptor.
          parent.actions.put(actionHolder.name, actionHolder);
        }
      }

      rootPackage.generateJava();
    } catch (Throwable e) {
      e.printStackTrace();
      error(null, e.getMessage());
    }

    return true;
  }

  /**
   * Prints an error message
   *
   * @param e The element which has caused the error. Can be null
   * @param msg The error message
   */
  public void error(Element e, String msg) {
    messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
  }

  /**
   *
   */
  public static class ActionHolder {

    // Configure Remote options
    final Remote remote;
    final Internal internal;
    final Worker worker;
    // Deprecated
    final Scheduled scheduled;
    final ClassName className;
    final TypeElement type;
    final TypeName typeName;
    final ClassName providerClassName;
    final ParameterizedTypeName providerTypeName;
    final String name;
    final String simpleName;
    final String fieldName;
    final String pkgName;
    final String moduleName;
    final String generatedProviderName;
    final ClassName generatedProviderClassName;
    final DeclaredTypeVar requestType;
    final DeclaredTypeVar replyType;
    final boolean hasParameterlessCtor;
    final boolean hasInjectCtor;
    final boolean hasFieldsInject;

    boolean generated;

    public ActionHolder(TypeElement type,
        DeclaredTypeVar requestType,
        DeclaredTypeVar replyType,
        Remote remote,
        Internal internal,
        Worker worker,
        Scheduled scheduled) {
      this.type = type;
      this.requestType = requestType;
      this.replyType = replyType;
      this.remote = remote;
      this.internal = internal;
      this.worker = worker;
      this.scheduled = scheduled;
      this.className = ClassName.get(type);
      this.name = type.getQualifiedName().toString();
      this.simpleName = type.getSimpleName().toString();
      final String f = type.getSimpleName().toString();
      this.fieldName = Character.toLowerCase(f.charAt(0)) + f.substring(1);
      this.moduleName = simpleName + "_Module";
      this.generatedProviderName = simpleName + "_Provider";

      typeName = ClassName.get(type);

      boolean parameterlessCtor = false;
      boolean hasInjectCtor = false;
      boolean hasFieldsInject = false;

      for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
        if (field.getAnnotation(Inject.class) != null) {
          hasFieldsInject = true;
          break;
        }
      }
      this.hasFieldsInject = hasFieldsInject;

      for (ExecutableElement cons :
          ElementFilter.constructorsIn(type.getEnclosedElements())) {
        if (cons.getParameters().isEmpty()) {
          parameterlessCtor = true;
        }

        if (cons.getAnnotation(Inject.class) != null) {
          hasInjectCtor = true;
        }
      }

      this.hasParameterlessCtor = parameterlessCtor;
      this.hasInjectCtor = hasInjectCtor;

      final String[] parts = name.split("[.]");
      if (parts.length == 1) {
        pkgName = "";
      } else {
        final String lastPart = parts[parts.length - 1];
        pkgName = name.substring(0, name.length() - lastPart.length() - 1);
      }

      generatedProviderClassName = ClassName.bestGuess(pkgName + "." + generatedProviderName);

      if (isRemote()) {
        providerClassName = ClassName.get(RemoteActionProvider.class);
      } else if (isInternal()) {
        providerClassName = ClassName.get(InternalActionProvider.class);
      } else if (isWorker()) {
        providerClassName = worker.fifo() ?
            ClassName.get(FifoWorkerActionProvider.class) :
            ClassName.get(WorkerActionProvider.class);
      } else if (isScheduled()) {
        providerClassName = ClassName.get(ScheduledActionProvider.class);
      } else {
        providerClassName = ClassName.get(ActionProvider.class);
      }

      if (isWorker()) {
        // Get Action IN resolved name.
        ClassName inName = ClassName.get(requestType.getResolvedElement());

        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className,
            inName
        );
      } else if (isScheduled()) {
        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className
        );
      } else {
        // Get Action IN resolved name.
        ClassName inName = ClassName.get(requestType.getResolvedElement());
        // Get Action OUT resolved name.
        ClassName outName = ClassName.get(replyType.getResolvedElement());

        providerTypeName = ParameterizedTypeName.get(
            providerClassName,
            className,
            inName,
            outName
        );
      }
    }

    boolean isRemote() {
      return remote != null;
    }

    boolean isInternal() {
      return internal != null;
    }

    boolean isWorker() {
      return worker != null;
    }

    boolean isScheduled() {
      return scheduled != null;
    }
  }

  /**
   * Resolves a single DECLARED Type Variable.
   * <p/>
   * Searches down the superclass, interface, and superinterface paths.
   *
   * @author Clay Molocznik
   */
  public static class DeclaredTypeVar {

    private final Map<String, ResolvedTypeVar> resolvedMap = new HashMap<>();
    private DeclaredType type;
    private TypeElement element;
    private int index;
    private String varName;
    private TypeMirror varType;
    private TypeElement varTypeElement;

    public DeclaredTypeVar(DeclaredType type, TypeElement element, int index, String varName,
        TypeMirror varType) {
      this.type = type;
      this.element = element;
      this.index = index;
      this.varName = varName;
      this.varType = varType;

      if (varType instanceof DeclaredType) {
        this.varTypeElement = (TypeElement) ((DeclaredType) varType).asElement();
      }
    }

    public DeclaredType getType() {
      return type;
    }

    public TypeElement getElement() {
      return element;
    }

    public int getIndex() {
      return index;
    }

    public String getVarName() {
      return varName;
    }

    public TypeMirror getResolvedType() {
      return varType;
    }

    public TypeElement getResolvedElement() {
      return varTypeElement;
    }

    public ResolvedTypeVar getResolved(String name) {
      return resolvedMap.get(name);
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
          resolvedMap.put(element.toString(), new ResolvedTypeVar(type, element, i, varName));

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

  /**
   * @author Clay Molocznik
   */
  public static class ResolvedTypeVar {

    private DeclaredType type;
    private TypeElement element;
    private int index;
    private String varName;

    public ResolvedTypeVar(DeclaredType type, TypeElement element, int index, String varName) {
      this.type = type;
      this.element = element;
      this.index = index;
      this.varName = varName;
    }

    public DeclaredType getType() {
      return type;
    }

    public TypeElement getElement() {
      return element;
    }

    public int getIndex() {
      return index;
    }

    public String getVarName() {
      return varName;
    }
  }

  /**
   * @author Clay Molocznik
   */
  public static class TypeParameterResolver {

    private final TypeElement element;
    private final List<DeclaredTypeVar> vars = new ArrayList<>();
    private boolean resolved;

    public TypeParameterResolver(TypeElement element) {
      this.element = element;
    }

    public static TypeParameterResolver resolve(TypeElement element) {
      final TypeParameterResolver resolver = new TypeParameterResolver(element);
      resolver.resolve();
      return resolver;
    }

    void resolve() {
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
        final ResolvedTypeVar actionTypeVar = var.getResolved(cls.getName());
        if (actionTypeVar != null) {
          if (actionTypeVar.getIndex() == typeVarIndex) {
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
            final List<? extends TypeParameterElement> typeParams =
                ((TypeElement) ((DeclaredType) type).asElement()).getTypeParameters();
            final String typeVarName = typeParams.get(i).getSimpleName().toString();

            final DeclaredTypeVar declaredTypeVar =
                new DeclaredTypeVar(declaredType, element, i, typeVarName, typeArg);
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

  /**
   *
   */
  public class Pkg {

    final String name;
    final String simpleName;
    final String moduleSimpleName;
    final Pkg parent;
    final TreeMap<String, Pkg> children = new TreeMap<>();
    final TreeMap<String, ActionHolder> actions = new TreeMap<>();
    final boolean root;
    final ClassName moduleName;
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

      this.simpleName = capitalize(name);

      if (path == null || path.isEmpty()) {
        this.moduleSimpleName = "Move_Root_Module";
        this.moduleName = ClassName.bestGuess("Move_Root_Module");
      } else {
        this.moduleSimpleName = simpleName + "_Module";
        this.moduleName = ClassName.bestGuess(path + "." + moduleSimpleName);
      }
    }

    public String getFullPath() {
      if (path == null || path.isEmpty()) {
        return getClassName();
      } else {
        return path + "." + getClassName();
      }
    }

    public String getClassName() {
//            return "Action_Locator";
      return name == null || name.isEmpty()
          ? "Root"
          : Character.toUpperCase(name.charAt(0)) + name.substring(1) + (root
              ? LOCATOR_ROOT
              : LOCATOR);
    }

    String capitalize(String value) {
      if (value == null || value.isEmpty()) {
        return "";
      }

      if (value.length() == 1) {
        return value.toUpperCase();
      }

      return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    String existingPackageModuleSource() {
      try {
        final FileObject fileObject = filer.getResource(
            StandardLocation.SOURCE_OUTPUT,
            path,
            moduleSimpleName + ".java"
        );
        final CharSequence content = fileObject.getCharContent(true);
        return content.toString();
      } catch (Throwable e) {
        return "";
      }
    }

    FileObject getModuleFile() {
      try {
        final FileObject fileObject = filer.getResource(
            StandardLocation.SOURCE_OUTPUT,
            path,
            moduleSimpleName + ".java"
        );
        return fileObject;
      } catch (Throwable e) {
        return null;
      }
    }

    void generateSource() {
      if (processed) {
        return;
      }

      processed = true;

      // Generate action modules.
      actions.values().stream().filter(a -> !a.generated).forEach(action -> {
        action.generated = true;
        generateProvider(action);
        generateModule(action);
      });

      // Package module.
      final FileObject packageModuleSource = getModuleFile();
      final List<ClassName> classNames = new ArrayList<>();

      if (packageModuleSource != null) {
        try {
          final String contents = packageModuleSource.getCharContent(true).toString();

          messager.printMessage(Kind.WARNING, contents);

          if (!packageModuleSource.delete()) {
            messager.printMessage(Kind.WARNING,
                "Failed to delete Package Module File: " +
                    moduleName.toString()
            );
          }
        } catch (IOException e) {
          if (e instanceof FileNotFoundException) {

          } else {
            messager.printMessage(
                Kind.ERROR,
                "Failed to get contents of Package Module File: " +
                    moduleName.toString() +
                    ": " +
                    e.getMessage()
            );
          }
        }
      } else {

      }

//      if (!packageModuleSource.isEmpty()) {
//
//        messager.printMessage(Kind.WARNING, "Package Module already exists");
//      } else {
      // Generate a new package module.
      final AnnotationSpec.Builder builder = AnnotationSpec.builder(Module.class);

      final CodeBlock.Builder codeBlock = CodeBlock.builder();
      codeBlock.beginControlFlow("");

      final List<ClassName> subModuleClasses = children.values().stream()
          .map(p -> p.moduleName)
          .collect(Collectors.toList());

      final List<ClassName> actionClasses = actions
          .values()
          .stream()
          .map(a -> ClassName.bestGuess(path + "." + a.moduleName)).collect(
              Collectors.toList());

      classNames.addAll(subModuleClasses);
      classNames.addAll(actionClasses);

      for (int i = 0; i < classNames.size(); i++) {
        codeBlock.add(i < classNames.size() - 1 ? "$T.class," : "$T.class", classNames.get(i));
        codeBlock.add("\n");
      }

      codeBlock.add("");
      codeBlock.endControlFlow();
      builder.addMember("includes", codeBlock.build());

      final TypeSpec.Builder typeBuilder = TypeSpec
          .classBuilder(moduleSimpleName)
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(builder.build());

      final JavaFile providerJavaFile = JavaFile.builder(path, typeBuilder.build()).build();

      try {
        // Write .java source code file.
        providerJavaFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
//      }
    }

    private void generateProvider(ActionHolder action) {
      final MethodSpec.Builder providerCtor = MethodSpec.constructorBuilder()
          .addAnnotation(Inject.class)
          .addParameter(ParameterSpec
              .builder(VERTX_CLASSNAME, "vertx").build())
          .addParameter(ParameterSpec
              .builder(
                  ParameterizedTypeName.get(ClassName.get(Provider.class), action.className),
                  "actionProvider").build())
          .addStatement("super(vertx, actionProvider)");

      final TypeSpec.Builder providerType = TypeSpec
          .classBuilder(action.generatedProviderName)
          .addModifiers(Modifier.PUBLIC)
          .superclass(action.providerTypeName)
          .addAnnotation(Singleton.class);

      providerType.addMethod(providerCtor.build());

      // Build java file.
      final JavaFile providerJavaFile = JavaFile.builder(path, providerType.build()).build();

      try {
        // Write .java source code file.
        providerJavaFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }

    private void generateModule(ActionHolder action) {
      final TypeSpec.Builder actionModuleType = TypeSpec
          .classBuilder(action.moduleName)
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Module.class);

      if (action.hasParameterlessCtor && !action.hasInjectCtor) {
        final MethodSpec.Builder provideActionMethod = MethodSpec.methodBuilder("_1")
            .addAnnotation(Provides.class)
            .returns(action.className)
            .addStatement("return new $T()", action.className);
        actionModuleType.addMethod(provideActionMethod.build());
      }

      final MethodSpec.Builder mapMethod = MethodSpec.methodBuilder("_2")
          .addAnnotation(Provides.class)
          .addAnnotation(IntoMap.class)
          .addAnnotation(AnnotationSpec
              .builder(ClassKey.class)
              .addMember("value", CodeBlock
                  .builder()
                  .add("$T.class", action.type)
                  .build())
              .build())
          .returns(ACTION_PROVIDER_NAME)
          .addParameter(ParameterSpec
              .builder(action.generatedProviderClassName, "provider")
              .build()
          )
          .addStatement("return provider");

      actionModuleType.addMethod(mapMethod.build());

      // Build java file.
      final JavaFile moduleFile = JavaFile.builder(path, actionModuleType.build())
          .build();

      try {
        // Write .java source code file.
        moduleFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }
    }

    public void generateJava() {
      if (processed) {
        return;
      }

      if (root) {
        if (children.isEmpty()) {
          return;
        }

        path = children.values().iterator().next().path;
      }

      generateSource();
      processed = true;

      // Build empty @Inject constructor.
      final MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
          .addAnnotation(Inject.class);
//                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

      // Init Class.
      final TypeSpec.Builder type = TypeSpec.classBuilder(getClassName())
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .superclass(TypeName.get(ActionLocator.class))
          .addAnnotation(Singleton.class);

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
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//                        .addAnnotation(Inject.class)
                .build()
        );

        ctor.addParameter(
            ParameterSpec.builder(typeName, childPackage.name, Modifier.FINAL).build()
        );

        ctor.addStatement("this.$L = $L", childPackage.name, childPackage.name);

        // Add code to initChildren() code.
        initChildrenCode.addStatement("getChildren().add($L)", childPackage.name);

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
        type.addField(
            FieldSpec.builder(action.generatedProviderClassName, action.fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
        );

        ctor.addParameter(
            ParameterSpec
                .builder(action.generatedProviderClassName, action.fieldName, Modifier.FINAL)
                .build()
        );

        ctor.addStatement("this.$L = $L", action.fieldName, action.fieldName);

        initActionsCode.addStatement("put($T.class, $L)", action.type, action.fieldName);

        type.addMethod(
            MethodSpec.methodBuilder(action.fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(action.generatedProviderClassName)
                .addStatement("return " + action.fieldName)
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

      if (!actions.isEmpty()) {
        try {
          final FileObject fileObject = filer.getResource(
              StandardLocation.SOURCE_OUTPUT,
              path,
              getClassName() + ".java"
          );
          final CharSequence content = fileObject.getCharContent(true);
          final String contents = content.toString();

          if (!contents.isEmpty()) {
            for (ActionHolder action : actions.values()) {
              if (action.isInternal()) {
                if (!contents.contains("InternalActionProvider<" + action.type.getSimpleName())) {
                  messager.printMessage(
                      Diagnostic.Kind.ERROR,
                      "Action: " +
                          action.name +
                          " was created. Full Regeneration needed. \"clean\" and \"compile\""
                  );
                  return;
                }
              } else if (action.isRemote()) {
                if (!contents.contains("RemoteActionProvider<" + action.type.getSimpleName())) {
                  messager.printMessage(
                      Diagnostic.Kind.ERROR,
                      "Action: " +
                          action.name +
                          " was created. Full Regeneration needed. \"clean\" and \"compile\""
                  );
                  return;
                }
              } else if (action.isWorker()) {
                if (!contents.contains("WorkerActionProvider<" + action.type.getSimpleName())) {
                  messager.printMessage(
                      Diagnostic.Kind.ERROR,
                      "Action: " +
                          action.name +
                          " was created. Full Regeneration needed. \"clean\" and \"compile\""
                  );
                  return;
                }
              } else if (action.isScheduled()) {
                if (!contents.contains("ScheduledActionProvider<" + action.type.getSimpleName())) {
                  messager.printMessage(
                      Diagnostic.Kind.ERROR,
                      "Action: " +
                          action.name +
                          " was created. Full Regeneration needed. \"clean\" and \"compile\""
                  );
                  return;
                }
              }
            }

//                        messager.printMessage(
//                            Diagnostic.Kind.WARNING,
//                            getFullPath() +
//                                " has a change, but it appears that it may save a full re-compile. " +
//                                "When in doubt \"clean\" and \"compile\""
//                        );

            // Generate child packages.
            for (Pkg childPackage : children.values()) {
              childPackage.generateJava();
            }

            return;
          }
        } catch (Throwable e) {
          // Ignore.
        }
      }

      if (!children.isEmpty()) {
        try {
          final FileObject fileObject = filer.getResource(
              StandardLocation.SOURCE_OUTPUT,
              path,
              getClassName() + ".java"
          );
          final CharSequence content = fileObject.getCharContent(true);
          final String contents = content.toString();

          if (!contents.isEmpty()) {
            for (Pkg child : children.values()) {
              if (!contents.contains(child.getClassName() + " " + child.name + ";")) {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "ActionLocator: " +
                        child.path +
                        " was created. Full Regeneration needed. \"clean\" and \"compile\""
                );
                return;
              }
            }

//                        messager.printMessage(
//                            Diagnostic.Kind.WARNING,
//                            getFullPath() +
//                                " has a change, but it appears that it may save a full re-compile. " +
//                                "When in doubt \"clean\" and \"compile\"");

            // Generate child packages.
            for (Pkg childPackage : children.values()) {
              childPackage.generateJava();
            }

            return;
          }
        } catch (Throwable e) {
          // Ignore.
        }
      }

      type.addMethod(ctor.build());

//            if (actions.isEmpty() && children.isEmpty()) {
      // Build java file.
      final JavaFile javaFile = JavaFile.builder(path, type.build()).build();

      try {
        // Write .java source code file.
        javaFile.writeTo(filer);
      } catch (Throwable e) {
        // Ignore.
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate Source File: " + e.getMessage());
      }

      // Generate child packages.
      for (Pkg childPackage : children.values()) {
        childPackage.generateJava();
      }
    }
  }
}
