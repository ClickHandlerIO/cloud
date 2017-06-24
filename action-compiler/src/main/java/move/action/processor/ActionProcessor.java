package move.action.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.validation.constraints.NotNull;
import move.action.ActionLocator;
import move.action.ActionPackage;
import move.action.IAction;
import move.action.InternalAction;
import move.action.RemoteAction;
import move.action.ScheduledAction;
import move.action.WorkerAction;

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
  private final HashMap<String, ActionHolder> remoteActionMap = new HashMap<>();
  private final ArrayList<ActionPackage> actionPackages = new ArrayList<>();

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
    final Set<String> annotataions = new LinkedHashSet<>();
    annotataions.add(ActionPackage.class.getCanonicalName());
    annotataions.add(RemoteAction.class.getCanonicalName());
    annotataions.add(InternalAction.class.getCanonicalName());
    annotataions.add(WorkerAction.class.getCanonicalName());
    annotataions.add(ScheduledAction.class.getCanonicalName());
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
          .getElementsAnnotatedWith(RemoteAction.class);
      final Set<? extends Element> internalElements = roundEnv
          .getElementsAnnotatedWith(InternalAction.class);
      final Set<? extends Element> workerElements = roundEnv
          .getElementsAnnotatedWith(WorkerAction.class);
      final Set<? extends Element> scheduledElements = roundEnv
          .getElementsAnnotatedWith(ScheduledAction.class);

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

      boolean allGood = true;
      for (Element annotatedElement : elements) {
        final RemoteAction remoteAction = annotatedElement.getAnnotation(RemoteAction.class);
        final InternalAction internalAction = annotatedElement.getAnnotation(InternalAction.class);
        final WorkerAction workerAction = annotatedElement.getAnnotation(WorkerAction.class);
        final ScheduledAction scheduledAction = annotatedElement
            .getAnnotation(ScheduledAction.class);

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
//                    if (remoteActionMap.containsKey(remoteAction.path())) {
//                        final ActionHolder actionProvider = remoteActionMap.get(remoteAction.path());
//                        messager.printMessage(Diagnostic.Kind.ERROR, "Duplicate RemoteAction Entry for key [" + remoteAction.path() + "]. " + actionProvider.type.getQualifiedName() + " and " + holder.type.getQualifiedName());
//                    }
//                    remoteActionMap.put(remoteAction.path(), holder);
        }
        if (holder.internalAction == null) {
          holder.internalAction = internalAction;
        }
        if (holder.workerAction == null) {
          holder.workerAction = workerAction;
        }
        if (holder.scheduledAction == null) {
          holder.scheduledAction = scheduledAction;
        }

        // Ensure only 1 action annotation was used.
        int actionAnnotationCount = 0;
        if (holder.remoteAction != null) {
          actionAnnotationCount++;
        }
        if (holder.internalAction != null) {
          actionAnnotationCount++;
        }
        if (holder.workerAction != null) {
          actionAnnotationCount++;
        }
        if (holder.scheduledAction != null) {
          actionAnnotationCount++;
        }
        if (actionAnnotationCount > 1) {
          messager.printMessage(
              Diagnostic.Kind.ERROR,
              element.getQualifiedName() +
                  "  has multiple Action annotations. Only one of the following may be used... " +
                  "@RemoteAction or @QueueAction or @InternalAction or @ActorAction"
          );
          continue;
        }

        // Make sure it's concrete.
        if (element.getModifiers().contains(Modifier.ABSTRACT)
            || (element.getTypeParameters() != null && !element.getTypeParameters().isEmpty())) {
          messager.printMessage(
              Diagnostic.Kind.ERROR,
              "@RemoteAction was placed on a non-concrete class " +
                  element.getQualifiedName() +
                  " It cannot be abstract or have TypeParameters."
          );
        }

        if (holder.inType == null || holder.outType == null) {
          final TypeParameterResolver typeParamResolver = new TypeParameterResolver(element);

          try {
            holder.inType = typeParamResolver.resolve(IAction.class, 0);
          } catch (Throwable e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
          }

          try {
            holder.outType = typeParamResolver.resolve(IAction.class, 1);
          } catch (Throwable e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
          }

          messager.printMessage(Diagnostic.Kind.WARNING, element.getQualifiedName().toString());

          if (holder.inType != null) {
//                        messager.printMessage(Diagnostic.Kind.WARNING, "IN = " + holder.inType.getResolvedElement().getQualifiedName().toString());
          }
          if (holder.outType != null) {
//                        messager.printMessage(Diagnostic.Kind.WARNING, "OUT = " + holder.outType.getResolvedElement().getQualifiedName().toString());
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

//                messager.printMessage(Diagnostic.Kind.WARNING, "PKG: " + jpkgName);

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
              next = new Pkg(nextName, parent.path == null || parent.path.isEmpty()
                  ? nextName
                  : parent.path + "." + nextName, parent);
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
//            return "Action_Locator";
      return name == null || name.isEmpty()
          ? "Root"
          : Character.toUpperCase(name.charAt(0)) + name.substring(1) + (root
              ? LOCATOR_ROOT
              : LOCATOR);
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
        // Get Action classname.
        ClassName actionName = ClassName.get(action.type);

        TypeName actionProviderBuilder;

        if (action.isWorker()) {
          // Get Action IN resolved name.
          ClassName inName = ClassName.get(action.inType.getResolvedElement());

          actionProviderBuilder = ParameterizedTypeName.get(
              action.getProviderTypeName(),
              actionName,
              inName
          );
        } else if (action.isScheduled()) {
          actionProviderBuilder = ParameterizedTypeName.get(
              action.getProviderTypeName(),
              actionName
          );
        } else {
          // Get Action IN resolved name.
          ClassName inName = ClassName.get(action.inType.getResolvedElement());
          // Get Action OUT resolved name.
          ClassName outName = ClassName.get(action.outType.getResolvedElement());

          actionProviderBuilder = ParameterizedTypeName.get(
              action.getProviderTypeName(),
              actionName,
              inName,
              outName
          );
        }

        type.addField(
            FieldSpec.builder(actionProviderBuilder, action.getFieldName())
//                        .addAnnotation(Inject.class)
                .addAnnotation(NotNull.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build()
        );

        // Init Class.
        final TypeSpec.Builder providerType = TypeSpec
            .classBuilder(action.type.getSimpleName() + "Action")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(actionProviderBuilder)
            .addAnnotation(Singleton.class);

        providerType.addMethod(MethodSpec.constructorBuilder()
            .addAnnotation(Inject.class).build());

//                final JavaFile javaFile = JavaFile.builder(path, providerType.build()).build();
//
//                try {
//                    // Write .java source code file.
//                    javaFile.writeTo(filer);
//                }
//                catch (Throwable e) {
//                    // Ignore.
//                    messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate Source File: " + e.getMessage());
//                }

        ctor.addParameter(
            ParameterSpec.builder(actionProviderBuilder, action.getFieldName(), Modifier.FINAL)
                .build()
        );

        ctor.addStatement("this.$L = $L", action.getFieldName(), action.getFieldName());

        initActionsCode.addStatement("put($T.class, $L)", action.type, action.getFieldName());

        type.addMethod(
            MethodSpec.methodBuilder(action.getFieldName())
                .addAnnotation(NotNull.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
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
                          action.getName() +
                          " was created. Full Regeneration needed. \"clean\" and \"compile\""
                  );
                  return;
                }
              } else if (action.isRemote()) {
                if (!contents.contains("RemoteActionProvider<" + action.type.getSimpleName())) {
                  messager.printMessage(
                      Diagnostic.Kind.ERROR,
                      "Action: " +
                          action.getName() +
                          " was created. Full Regeneration needed. \"clean\" and \"compile\""
                  );
                  return;
                }
              } else if (action.isWorker()) {
                if (!contents.contains("WorkerActionProvider<" + action.type.getSimpleName())) {
                  messager.printMessage(
                      Diagnostic.Kind.ERROR,
                      "Action: " +
                          action.getName() +
                          " was created. Full Regeneration needed. \"clean\" and \"compile\""
                  );
                  return;
                }
              } else if (action.isScheduled()) {
                if (!contents.contains("ScheduledActionProvider<" + action.type.getSimpleName())) {
                  messager.printMessage(
                      Diagnostic.Kind.ERROR,
                      "Action: " +
                          action.getName() +
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
