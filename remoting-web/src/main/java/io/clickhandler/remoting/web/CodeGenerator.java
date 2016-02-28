package io.clickhandler.remoting.web;

import com.google.web.bindery.event.shared.HandlerRegistration;
import com.squareup.javapoet.*;
import com.squareup.javapoet.FieldSpec;
import io.clickhandler.action.RemoteAction;
import io.clickhandler.remoting.Push;
import io.clickhandler.remoting.compiler.*;
import io.clickhandler.web.Bus;
import io.clickhandler.web.EventCallback;
import io.clickhandler.web.remoting.PushSubscription;
import io.clickhandler.web.remoting.ResponseEvent;
import io.clickhandler.web.remoting.WsAction;
import io.clickhandler.web.remoting.WsDispatcher;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import javax.annotation.Generated;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * ClickHandler Web Remoting code generator.
 *
 * @author Clay Molocznik
 */
public class CodeGenerator {
    private final static String ACTION_LOCATOR_SUFFIX = ".Action_Locator";
    private final static String ACTION_LOCATOR_NAME = "Action_Locator";
    private final static String RESPONSE_EVENT_NAME = "Event";
    private final String rootPackage;
    private final File outputDir;
    private RemotingAST ast;
    private TypeSpec.Builder initializerType = null;
    private MethodSpec.Builder initMethod = null;

    public CodeGenerator(RemotingAST ast, String rootPackage, File outputDir) {
        this.ast = ast;
        this.rootPackage = rootPackage;
        this.outputDir = outputDir;
    }

    private static String lowercaseFirst(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (value.length() == 1) {
            return String.valueOf(Character.toLowerCase(value.charAt(0)));
        }

        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String upperFirst(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (value.length() == 1) {
            return String.valueOf(Character.toUpperCase(value.charAt(0)));
        }

        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public void generate() {
        initializerType =
            TypeSpec.classBuilder("Starter")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Singleton.class)
                .addField(FieldSpec.builder(WsDispatcher.class, "dispatcher")
                    .addModifiers(Modifier.FINAL).build()
                );

        // Add @Generated annotation
        addGeneratorAnnotation(initializerType);

        initializerType.addMethod(MethodSpec.constructorBuilder()
            .addAnnotation(Inject.class)
            .addParameter(WsDispatcher.class, "dispatcher")
            .addStatement("this.dispatcher = dispatcher")
            .addStatement("start()")
            .build());

        initMethod = MethodSpec.methodBuilder("start")
            .addModifiers(Modifier.PUBLIC);

        // Generate Packages and Materialized Views.
        final Namespace root = ast.getRoot();
        generate(null, root);
        generateLocators(root);

        initMethod.addStatement("dispatcher.start()");
        initializerType.addMethod(initMethod.build());
        try {
            JavaFile.builder(rootPackage, initializerType.build()).build().writeTo(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generate(TypeSpec.Builder parentType, Namespace namespace) {
        if (namespace.isClass()) {
            final TypeSpec.Builder type = TypeSpec.classBuilder(namespace.name());

            // Add @Generated annotation
            addGeneratorAnnotation(type);

            if (namespace instanceof ActionSpec) {
                final ActionSpec actionSpec = (ActionSpec) namespace;

                type.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC).addAnnotation(Inject.class).build());

                type.superclass(ParameterizedTypeName.get(
                    ClassName.get(WsAction.class),
                    ClassName.bestGuess(actionSpec.inSpec().canonicalName()),
                    ClassName.bestGuess(actionSpec.outSpec().canonicalName())
                ));

                final TypeName inType = ClassName.bestGuess(actionSpec.inSpec().canonicalName());
                final TypeName outType = ClassName.bestGuess(actionSpec.outSpec().canonicalName());

                type.addMethod(MethodSpec.methodBuilder("inTypeName")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Bus.TypeName.class),
                        inType
                    ))
                    .addCode(CodeBlock.builder().addStatement("return $T.Event.NAME", inType).build())
                    .build()
                );

                type.addMethod(MethodSpec.methodBuilder("outTypeName")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Bus.TypeName.class),
                        outType
                    ))
                    .addCode(CodeBlock.builder().addStatement("return $T.Event.NAME", outType).build())
                    .build()
                );

                final RemoteAction remoteAction =
                    (RemoteAction) actionSpec.provider().getActionClass().getAnnotation(RemoteAction.class);

                final TypeName responseEventName = ClassName.bestGuess(actionSpec.canonicalName() + "." + RESPONSE_EVENT_NAME);

                type.addMethod(MethodSpec.methodBuilder("path")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(String.class)
                    .addStatement("return $S", remoteAction.path()).build());

                type.addMethod(MethodSpec.methodBuilder("timeoutMillis")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(int.class)
                    .addStatement("return $L", 15000).build());

                type.addMethod(MethodSpec.methodBuilder("responseEvent")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(responseEventName)
                    .addParameter(inType, "in")
                    .addParameter(outType, "out")
                    .addStatement("return new $L(in, out)", RESPONSE_EVENT_NAME).build());

                // Build ResponseEvent.
                TypeSpec.Builder responseEventBuilder = TypeSpec.classBuilder(RESPONSE_EVENT_NAME);
                responseEventBuilder.superclass(ParameterizedTypeName.get(ClassName.get(ResponseEvent.class), inType, outType));
                responseEventBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                responseEventBuilder.addMethod(MethodSpec
                    .constructorBuilder()
                    .addParameter(inType, "in")
                    .addParameter(outType, "out")
                    .addStatement("super(in, out)").build()
                );

                type.addType(responseEventBuilder.build());
            }

            type.addModifiers(Modifier.PUBLIC);
            if (parentType != null) {
                type.addModifiers(Modifier.STATIC);
            }

            namespace.children().forEach((key, value) -> generate(type, value));
            namespace.types().forEach(materializedType -> generate(type, materializedType));

            if (parentType != null) {
                parentType.addType(type.build());
            } else {
                try {
                    JavaFile.builder(namespace.path(), type.build()).build().writeTo(outputDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            namespace.children().forEach((key, value) -> generate(null, value));
            namespace.types().forEach(materializedType -> generate(null, materializedType));
        }
    }

    private void generate(TypeSpec.Builder parent, MaterializedType materializedType) {
        TypeSpec.Builder type = null;

        if (materializedType instanceof ComplexType) {
            ComplexType complexType = (ComplexType) materializedType;
            final ClassName name = ClassName.bestGuess(materializedType.canonicalName());

            type = TypeSpec.interfaceBuilder(materializedType.name());
            type.addModifiers(Modifier.PUBLIC);
            if (parent != null) {
                type.addModifiers(Modifier.STATIC);

            }

            type.addAnnotation(AnnotationSpec
                .builder(JsType.class)
                .addMember("isNative", "true")
                .build());

            final StandardType[] ifaces = complexType.interfaces();
            if (ifaces != null && ifaces.length > 0) {
                for (StandardType iface : ifaces) {
                    type.addSuperinterface(ClassName.bestGuess(iface.canonicalName()));
                }
            }
            if (complexType.superType() != null && !Object.class.equals(complexType.superType().javaType())) {
                type.addSuperinterface(ClassName.bestGuess(complexType.superType().canonicalName()));
            }

            // Build Factory.
            if (!materializedType.isAbstract() && !materializedType.isInterface()) {
                TypeSpec.Builder eventBuilder = TypeSpec.classBuilder("Event").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

                eventBuilder.addField(FieldSpec.builder(
                    ParameterizedTypeName.get(
                        ClassName.get(Bus.TypeName.class), name
                    ),
                    "NAME",
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer(
                    CodeBlock.builder().add("new $T<>()", Bus.TypeName.class).build()
                ).build());

                type.addType(eventBuilder.build());
            }

            if (ast.isPush(complexType)) {
                final Push push = (Push) complexType.javaType().getAnnotation(Push.class);
                final String address = push != null && !push.value().isEmpty() ? push.value() : complexType.javaType().getCanonicalName();
                final ClassName subscribeName = ClassName.bestGuess(materializedType.canonicalName() + ".Subscribe");

                TypeSpec.Builder subscription = TypeSpec.classBuilder("Subscribe");
                subscription.addModifiers(Modifier.PUBLIC, Modifier.STATIC);

                initMethod.addStatement("$T.DISPATCHER = dispatcher", subscribeName);

                subscription.addField(FieldSpec.builder(
                    String.class,
                    "VALUE",
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer(
                    CodeBlock.builder().add("$S", address).build()
                ).build());

                subscription.addField(FieldSpec.builder(
                    WsDispatcher.class,
                    "DISPATCHER",
                    Modifier.PUBLIC, Modifier.STATIC).build());

                subscription.addMethod(MethodSpec.methodBuilder("on")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(HandlerRegistration.class)
                    .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(EventCallback.class), name), "handler").build())
                    .addStatement("return on(DISPATCHER, null, handler)")
                    .build());

                subscription.addMethod(MethodSpec.methodBuilder("on")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(HandlerRegistration.class)
                    .addParameter(WsDispatcher.class, "dispatcher")
                    .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(EventCallback.class), name), "handler").build())
                    .addStatement("return on(dispatcher, null, handler)")
                    .build());

                subscription.addMethod(MethodSpec.methodBuilder("on")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(HandlerRegistration.class)
                    .addParameter(String.class, "id")
                    .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(EventCallback.class), name), "handler").build())
                    .addStatement("return on(DISPATCHER, id, handler)")
                    .build());

                subscription.addMethod(MethodSpec.methodBuilder("on")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(HandlerRegistration.class)
                    .addParameter(WsDispatcher.class, "dispatcher")
                    .addParameter(String.class, "id")
                    .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(EventCallback.class), name), "handler").build())
                    .addStatement("return dispatcher.subscribe(new $T<$T>(VALUE, Event.NAME, handler, id))", PushSubscription.class, name)
                    .build());

                type.addType(subscription.build());
            }

            generateComplexType(type, (ComplexType) materializedType);

            final TypeSpec.Builder t = type;
            materializedType.children().forEach(child -> generate(t, child));
        } else if (materializedType instanceof EnumType) {
            final EnumType enumType = (EnumType) materializedType;

            type = TypeSpec.enumBuilder(materializedType.name());
            type.addModifiers(Modifier.PUBLIC);
            if (parent != null) {
                type.addModifiers(Modifier.STATIC);
            }
            for (String value : enumType.values()) {
                type.addEnumConstant(value);
            }
        }

        if (type == null)
            return;

        if (parent == null)
            // Add @Generated annotation
            addGeneratorAnnotation(type);

        if (parent == null) {
            try {
                JavaFile.builder(materializedType.path(), type.build()).build().writeTo(outputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            parent.addType(type.build());
        }
    }

    private void generateComplexType(TypeSpec.Builder type, ComplexType complexType) {
        final StandardType superType = complexType.superType();
        if (superType != null && superType instanceof ComplexType) {
            ComplexType superT = (ComplexType) superType;

            if (!superT.javaType().equals(Object.class)) {
                type.superclass(ClassName.bestGuess(superT.canonicalName()));
            }
        }

        complexType.fields().forEach(field -> {
            if (java.lang.reflect.Modifier.isStatic(field.field().getModifiers())) {
                return;
            }

            TypeName typeName = null;
            TypeName valueTypeName = null;

            switch (field.type().dataType()) {
                case LIST:
                case ARRAY:
                    ArrayType arrayType = (ArrayType) field.type();
                    valueTypeName = getTypeName(arrayType.componentType());
                    typeName = ArrayTypeName.of(valueTypeName);
                    break;
                case LONG:
                case BYTE:
                case SHORT:
                case CHAR:
                case INT:
                case FLOAT:
                case DOUBLE:
                case DATE:
                    // JavaScript only has 1 number type.
                    typeName = getTypeName(field.type());
                    break;

                case BOOLEAN:
                case STRING:
                case WILDCARD:
                    typeName = TypeName.get(field.type().javaType());
                    break;

                case SET:
                    SetType setType = (SetType) field.type();
                    typeName = ParameterizedTypeName.get(
                        ClassName.get(Set.class),
                        ClassName.bestGuess(setType.componentType().canonicalName())
                    );
                    break;

                case MAP:
                    MapType mapType = (MapType) field.type();
                    typeName = ParameterizedTypeName.get(
                        ClassName.get(Set.class),
                        ClassName.bestGuess(mapType.keyType().canonicalName()),
                        ClassName.bestGuess(mapType.valueType().canonicalName())
                    );
                    break;

                case ENUM:
                    // Enums are serialized to Strings.
                    typeName = TypeName.get(String.class);
                    break;

                case COMPLEX:
                    typeName = ClassName.bestGuess(field.type().canonicalName());
                    break;
            }

            final String getterName = "get" + upperFirst(field.name());
            final String setterName = "set" + upperFirst(field.name());

            type.addMethod(MethodSpec.methodBuilder(getterName).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(typeName)
                .addAnnotation(AnnotationSpec.builder(JsProperty.class)
                    .addMember("name", "$S", field.jsonName()).build()).build());

            type.addMethod(MethodSpec.methodBuilder(setterName).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ParameterSpec.builder(typeName, "value").build())
                .addAnnotation(AnnotationSpec.builder(JsProperty.class)
                    .addMember("name", "$S", field.jsonName()).build()).build());

            switch (field.type().dataType()) {
                case ARRAY:
                case LIST:
                    ArrayType arrayType = (ArrayType) field.type();
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("$L(value)", setterName)
                        .addStatement("return this")
                        .build());

                    if (!arrayType.componentType().javaType().isPrimitive()) {
                        try {
                            // List accessors
                            type.addMethod(MethodSpec.methodBuilder(field.name() + "AsList").addAnnotation(JsOverlay.class)
                                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                                .returns(ParameterizedTypeName.get(ClassName.get(List.class), valueTypeName))
                                .addStatement("return $L() != null ? $T.asList($L()) : null", getterName, Arrays.class, getterName)
                                .build());
                            type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                                .returns(ClassName.bestGuess(complexType.canonicalName()))
                                .addParameter(ParameterSpec.builder(
                                    ParameterizedTypeName.get(
                                        ClassName.get(List.class),
                                        valueTypeName
                                    ),
                                    "value",
                                    Modifier.FINAL
                                ).build())
                                .addStatement("$L(value != null ? value.toArray(new $T[value.size()]) : null)", setterName, valueTypeName)
                                .addStatement("return this")
                                .build());
                        } catch (Throwable e) {
                            String name = valueTypeName.toString();
                            e.printStackTrace();
                        }
                    }
                    break;
                case DATE:
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("$L(value)", setterName)
                        .addStatement("return this")
                        .build());
                    break;

                default:
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("$L(value)", setterName)
                        .addStatement("return this")
                        .build());
                    break;
                case SET:
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("$L(value)", setterName)
                        .addStatement("return this")
                        .build());
                    break;
                case MAP:
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("$L(value)", setterName)
                        .addStatement("return this")
                        .build());
                    break;
                case ENUM:
                    TypeName enumType = ClassName.bestGuess(field.type().canonicalName());

                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(enumType)
                        .addCode(CodeBlock.builder()
                            .beginControlFlow("try")
                            .addStatement("return $T.valueOf($L())", enumType, getterName)
                            .endControlFlow()
                            .beginControlFlow("catch ($T e)", Throwable.class)
                            .addStatement("$L(null)", setterName)
                            .addStatement("return null")
                            .endControlFlow().build()
                        )
                        .build());
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(enumType, "value", Modifier.FINAL).build())
                        .addStatement("$L(value != null ? value.toString() : null)", setterName)
                        .addStatement("return this")
                        .build());
                    break;
            }
        });
    }

    private TypeName getTypeName(StandardType type) {
        switch (type.dataType()) {
            case LONG:
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case FLOAT:
            case DOUBLE:
            case DATE:
                // JavaScript only has 1 number type.
                return TypeName.get(double.class);
            case BOOLEAN:
            case STRING:
            case WILDCARD:
                return TypeName.get(type.javaType());
            case LIST:
                ListType listType = (ListType) type;
                return ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    ClassName.bestGuess(listType.componentType().canonicalName())
                );
            case SET:
                SetType setType = (SetType) type;
                return ParameterizedTypeName.get(
                    ClassName.get(Set.class),
                    ClassName.bestGuess(setType.componentType().canonicalName())
                );
            case MAP:
                MapType mapType = (MapType) type;
                return ParameterizedTypeName.get(
                    ClassName.get(Set.class),
                    ClassName.bestGuess(mapType.keyType().canonicalName()),
                    ClassName.bestGuess(mapType.valueType().canonicalName())
                );
            case ENUM:
                // Enums are serialized to Strings.
                return TypeName.get(String.class);

            case COMPLEX:
                return ClassName.bestGuess(type.canonicalName());
        }

        return TypeName.get(type.javaType());
    }

    private void generateLocators(Namespace namespace) {
        if (!namespace.hasActions()) {
            return;
        }

        if (namespace.path().isEmpty()) {
            namespace.children().forEach((key, value) -> {
                generateLocators(value);
            });
            return;
        }

        TypeSpec.Builder type = TypeSpec.classBuilder(ACTION_LOCATOR_NAME);
        type.addModifiers(Modifier.PUBLIC);
        type.addAnnotation(Singleton.class);

        // Add @Generated annotation
        addGeneratorAnnotation(type);

        // Add @Inject constructor.
        type.addMethod(MethodSpec.constructorBuilder().addAnnotation(Inject.class).build());

        namespace.children().forEach((key, value) -> {
            if (value instanceof ActionSpec) {
                final ActionSpec actionSpec = (ActionSpec) value;
                final ClassName className = ClassName.bestGuess(actionSpec.canonicalName());
                final String fieldName = lowercaseFirst(value.name());
                final TypeName typeName = ParameterizedTypeName.get(
                    ClassName.get(Provider.class),
                    className
                );

                type.addField(FieldSpec.builder(
                    typeName,
                    fieldName
                ).addAnnotation(Inject.class).build());

                type.addMethod(MethodSpec.methodBuilder(value.name())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(typeName)
                    .addStatement("return $L", fieldName)
                    .build());
            } else if (value.hasActions()) {
                final ClassName className = ClassName.bestGuess(value.canonicalName() + ACTION_LOCATOR_SUFFIX);
                final String fieldName = lowercaseFirst(value.name());

                type.addField(FieldSpec.builder(
                    className,
                    fieldName
                ).addAnnotation(Inject.class).build());

                type.addMethod(MethodSpec.methodBuilder(value.name())
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(className)
                    .addStatement("return $L", fieldName)
                    .build());

                generateLocators(value);
            }
        });

        try {
            JavaFile.builder(namespace.path(), type.build()).build().writeTo(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void addGeneratorAnnotation(TypeSpec.Builder type) {
        type.addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", CodeGenerator.class.getCanonicalName())
            .build());
    }
}
