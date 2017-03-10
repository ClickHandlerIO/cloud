package move.remoting.web;

import com.google.common.base.Charsets;
import com.squareup.javapoet.*;
import com.squareup.javapoet.FieldSpec;
import common.client.Bus;
import common.client.JSON;
import common.client.MessageProvider;
import common.client.Try;
import move.action.RemoteAction;
import move.remoting.Push;
import move.remoting.codegen.*;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import moment.client.Moment;
import remoting.client.ResponseEvent;
import remoting.client.WsAction;
import remoting.client.WsDispatcher;

import javax.annotation.Generated;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
    private RemotingRegistry ast;
    private TypeSpec.Builder initializerType = null;
    private MethodSpec.Builder initMethod = null;
    private String gwtModuleFileName = "Api.gwt.xml";

    public CodeGenerator(RemotingRegistry ast, String rootPackage, File outputDir) {
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

        final String gwtModuleFileContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<module>\n" +
            "    <inherits name=\"com.google.gwt.user.User\"/>\n" +
            "    <inherits name=\"com.google.gwt.resources.Resources\"/>\n" +
            "    <inherits name=\"ClickHandlerCore\" />\n" +
            "\n" +
            "    <source path=\"\"/>\n" +
            "</module>\n";
        try {
            com.google.common.io.Files.write(
                gwtModuleFileContents.getBytes(Charsets.UTF_8),
                new File(outputDir + File.separator + rootPackage.replace(".", File.separator) + File.separator + gwtModuleFileName)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
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

            if (ast.isPush(complexType)) {
                final Push push = (Push) complexType.javaType().getAnnotation(Push.class);
                final String address = push != null && !push.value().isEmpty() ? push.value() : complexType.javaType().getSimpleName();

                type = TypeSpec.classBuilder(complexType.name());
                type.addModifiers(Modifier.PUBLIC);

                final String canonicalName = complexType.canonicalName();
                final ComplexType subType = complexType.convertToHolder("Message");

                type.addSuperinterface(ParameterizedTypeName.get(
                    ClassName.get(MessageProvider.class),
                    ClassName.bestGuess(subType.canonicalName())
                ));

                type.addField(
                    FieldSpec.builder(
                        ClassName.bestGuess(subType.canonicalName()),
                        "message",
                        Modifier.PUBLIC, Modifier.FINAL
                    ).build()
                );

                type.addMethod(
                    MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(
                            ParameterSpec.builder(
                                ClassName.bestGuess(subType.canonicalName()),
                                "message",
                                Modifier.FINAL
                            ).build()
                        ).addStatement("this.message = message")
                        .build()
                );

                type.addMethod(
                    MethodSpec.methodBuilder("get")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ClassName.bestGuess(subType.canonicalName()))
                        .addStatement("return message")
                        .build()
                );

                initMethod.addStatement(
                    "dispatcher.registerPush($S, $T.class, json -> new $T($T.parse(json)))",
                    address,
                    ClassName.bestGuess(canonicalName),
                    ClassName.bestGuess(canonicalName),
                    JSON.class
                );

                generate(type, subType);
            } else {
                type = TypeSpec.classBuilder(materializedType.name());
                type.addModifiers(Modifier.PUBLIC);
                if (parent != null) {
                    type.addModifiers(Modifier.STATIC);
                }

                type.addAnnotation(AnnotationSpec
                    .builder(JsType.class)
                    .addMember("isNative", "true")
                    .addMember("namespace", "\"" + JsPackage.GLOBAL + "\"")
                    .addMember("name", "\"Object\"")
                    .build());

                final StandardType[] ifaces = complexType.interfaces();
                if (ifaces != null && ifaces.length > 0) {
                    for (StandardType iface : ifaces) {
                        type.addSuperinterface(ClassName.bestGuess(iface.canonicalName()));
//                    type.superclass(ClassName.bestGuess(iface.canonicalName()));
                    }
                }
                if (complexType.superType() != null && !Object.class.equals(complexType.superType().javaType())) {
//                type.addSuperinterface(ClassName.bestGuess(complexType.superType().canonicalName()));
                    type.superclass(ClassName.bestGuess(complexType.superType().canonicalName()));
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

                generateComplexType(type, (ComplexType) materializedType);

                final TypeSpec.Builder t = type;
                materializedType.children().forEach(child -> generate(t, child));
            }
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
                JavaFile.builder(
                    materializedType.path(),
                    type.build()
                ).build().writeTo(outputDir);
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
                Try.run(() -> type.superclass(ClassName.bestGuess(superT.canonicalName())));
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
                case BOOLEAN:
                case DATETIME:
                    // JavaScript only has 1 number type.
                    typeName = getTypeName(field.type());
                    break;

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

            type.addField(FieldSpec.builder(typeName, field.name(), Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(JsProperty.class)
                    .addMember("name", "$S", field.jsonName()).build()).build());

            type.addMethod(MethodSpec.methodBuilder(getterName).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(typeName)
                .addStatement("return $L", field.name())
                .addAnnotation(AnnotationSpec.builder(JsOverlay.class).build()).build());

            type.addMethod(MethodSpec.methodBuilder(setterName).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(ParameterSpec.builder(typeName, "value").build())
                .addStatement("this.$L = value", field.name())
                .addAnnotation(AnnotationSpec.builder(JsOverlay.class).build()).build());

            switch (field.type().dataType()) {
                case ARRAY:
                case LIST:
                    ArrayType arrayType = (ArrayType) field.type();
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("this.$L = value", field.name())
                        .addStatement("return this")
                        .build());

                    if (!arrayType.componentType().javaType().isPrimitive()) {
                        try {
                            // List accessors
                            type.addMethod(MethodSpec.methodBuilder(field.name() + "AsList").addAnnotation(JsOverlay.class)
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                .returns(ParameterizedTypeName.get(ClassName.get(List.class), valueTypeName))
                                .addStatement("return this.$L != null ? $T.asList(this.$L) : new java.util.ArrayList<>()", field.name(), Arrays.class, field.name())
                                .build());
                            type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                .returns(ClassName.bestGuess(complexType.canonicalName()))
                                .addParameter(ParameterSpec.builder(
                                    ParameterizedTypeName.get(
                                        ClassName.get(List.class),
                                        valueTypeName
                                    ),
                                    "value",
                                    Modifier.FINAL
                                ).build())
                                .addStatement("this.$L = value != null ? value.toArray(new $T[value.size()]) : null", field.name(), valueTypeName)
                                .addStatement("return this")
                                .build());
                        } catch (Throwable e) {
                            String name = valueTypeName.toString();
                            e.printStackTrace();
                        }
                    }
                    break;

//                case DURATION:
//                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
//                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//                        .returns(ClassName.bestGuess(complexType.canonicalName()))
//                        .addParameter(ParameterSpec.builder(TypeName.get(Moment.class), "value", Modifier.FINAL).build())
//                        .addStatement("this.$L = value == null ? null : value.toISOString()", field.name())
//                        .addStatement("return this")
//                        .build());
//
//                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
//                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//                        .returns(ClassName.bestGuess(complexType.canonicalName()))
//                        .addParameter(ParameterSpec.builder(TypeName.get(Double.class), "value", Modifier.FINAL).build())
//                        .addStatement("this.$L = value", field.name())
//                        .addStatement("return this")
//                        .build());
//
//                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
//                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//                        .returns(ClassName.bestGuess(complexType.canonicalName()))
//                        .addParameter(ParameterSpec.builder(TypeName.get(double.class), "value", Modifier.FINAL).build())
//                        .addStatement("this.$L = $T.moment(value).toISOString()", field.name(), Moment.class)
//                        .addStatement("return this")
//                        .build());
//
//                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
//                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//                        .returns(ClassName.bestGuess(complexType.canonicalName()))
//                        .addParameter(ParameterSpec.builder(TypeName.get(Date.class), "value", Modifier.FINAL).build())
//                        .addStatement("this.$L = value == null ? null : $T.moment(value.getTime()).toISOString()", field.name(), Moment.class)
//                        .addStatement("return this")
//                        .build());
//
//                    type.addMethod(MethodSpec.methodBuilder(field.name() + "AsMoment").addAnnotation(JsOverlay.class)
//                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//                        .returns(TypeName.get(Moment.class))
//                        .addStatement("return this.$L == null ? null : $T.moment(this.$L)", field.name(), Moment.class, field.name())
//                        .build());
//
//                    type.addMethod(MethodSpec.methodBuilder(field.name() + "AsDate").addAnnotation(JsOverlay.class)
//                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//                        .returns(TypeName.get(Date.class))
//                        .addStatement("return this.$L == null ? null : new $T((long)$T.moment(this.$L).unix() * 1000)", field.name(), Date.class, Moment.class, field.name())
//                        .build());
//                    break;

                case INSTANT:
                case DATE:
                case DATETIME:
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(TypeName.get(Moment.class), "value", Modifier.FINAL).build())
                        .addStatement("this.$L = value == null ? null : value.toISOString()", field.name())
                        .addStatement("return this")
                        .build());

                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(TypeName.get(String.class), "value", Modifier.FINAL).build())
                        .addStatement("this.$L = value", field.name())
                        .addStatement("return this")
                        .build());

                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(TypeName.get(double.class), "value", Modifier.FINAL).build())
                        .addStatement("this.$L = $T.moment(value).toISOString()", field.name(), Moment.class)
                        .addStatement("return this")
                        .build());

                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(TypeName.get(Date.class), "value", Modifier.FINAL).build())
                        .addStatement("this.$L = value == null ? null : $T.moment(value.getTime()).toISOString()", field.name(), Moment.class)
                        .addStatement("return this")
                        .build());

                    type.addMethod(MethodSpec.methodBuilder(field.name() + "AsMoment").addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(TypeName.get(Moment.class))
                        .addStatement("return this.$L == null ? null : $T.moment(this.$L)", field.name(), Moment.class, field.name())
                        .build());

                    type.addMethod(MethodSpec.methodBuilder(field.name() + "AsDate").addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(TypeName.get(Date.class))
                        .addStatement("return this.$L == null ? null : new $T((long)$T.moment(this.$L).unix() * 1000)", field.name(), Date.class, Moment.class, field.name())
                        .build());

                    type.addMethod(MethodSpec.methodBuilder(field.name() + "AsUnix").addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(TypeName.get(Double.class))
                        .addStatement("return this.$L == null ? null : $T.moment(this.$L).unix() * 1000", field.name(), Moment.class, field.name())
                        .build());
                    break;

                case BOOLEAN:
                    break;

                default:
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("this.$L = value", field.name())
                        .addStatement("return this")
                        .build());
                    break;
                case SET:
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("this.$L = value", field.name())
                        .addStatement("return this")
                        .build());
                    break;
                case MAP:
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(typeName, "value", Modifier.FINAL).build())
                        .addStatement("this.$L = value", field.name())
                        .addStatement("return this")
                        .build());
                    break;
                case ENUM:
                    TypeName enumType = ClassName.bestGuess(field.type().canonicalName());

                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(enumType)
                        .addCode(CodeBlock.builder()
                            .beginControlFlow("try")
                            .addStatement("return $T.valueOf(this.$L)", enumType, field.name())
                            .endControlFlow()
                            .beginControlFlow("catch ($T e)", Throwable.class)
                            .addStatement("this.$L = null", field.name())
                            .addStatement("return null")
                            .endControlFlow().build()
                        )
                        .build());
                    type.addMethod(MethodSpec.methodBuilder(field.name()).addAnnotation(JsOverlay.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(ClassName.bestGuess(complexType.canonicalName()))
                        .addParameter(ParameterSpec.builder(enumType, "value", Modifier.FINAL).build())
                        .addStatement("this.$L = value != null ? value.toString() : null", field.name())
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
                // JavaScript only has 1 number type.
                return TypeName.get(Double.class);
            case BOOLEAN:
                return TypeName.get(Boolean.class);
            case DATE:
            case DATETIME:
                return TypeName.get(String.class);
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
                    ClassName.get(Map.class),
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

        if (namespace.canonicalName().startsWith(rootPackage)) {
            try {
                JavaFile.builder(namespace.path(), type.build()).build().writeTo(outputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void addGeneratorAnnotation(TypeSpec.Builder type) {
        type.addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", CodeGenerator.class.getCanonicalName())
            .build());
    }
}