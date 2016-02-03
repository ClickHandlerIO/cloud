package io.clickhandler.remoting.compiler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 */
public class FieldSpec {
    private Field field;
    private String name;
    private StandardType type;
    private String jsonName;
    private JsonProperty jsonProperty;

    public Field field() {
        return this.field;
    }

    public FieldSpec field(final Field field) {
        this.field = field;
        return this;
    }

    public String name() {
        return this.name;
    }

    public StandardType type() {
        return this.type;
    }

    public String jsonName() {
        return this.jsonName;
    }

    public JsonProperty jsonProperty() {
        return this.jsonProperty;
    }

    public FieldSpec name(final String name) {
        this.name = name;
        return this;
    }

    public FieldSpec type(final StandardType type) {
        this.type = type;
        return this;
    }

    public FieldSpec jsonName(final String jsonName) {
        this.jsonName = jsonName;
        return this;
    }

    public FieldSpec jsonProperty(final JsonProperty jsonProperty) {
        this.jsonProperty = jsonProperty;
        return this;
    }

    public boolean isStatic() {
        return Modifier.isStatic(field.getModifiers());
    }
}
