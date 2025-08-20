package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RosettaClass {
    private final String name;
    private final List<RosettaMethod> methods = new ArrayList<>();
    private final Map<String, RosettaField> fields = new HashMap<>();
    private final List<RosettaConstructor> constructors = new ArrayList<>();
    private String notes = "";
    private boolean deprecated = false;

    RosettaPackage pkg = null;

    public void addMethod(RosettaMethod method) {
        methods.add(method);
        method.clazz = this;
    }

    public void addField(RosettaField field) {
        fields.put(field.getName(), field);
        field.clazz = this;
    }

    public void addConstructor(RosettaConstructor constructor) {
        constructors.add(constructor);
        constructor.clazz = this;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    /**
     * Returns a more specific name, useful for debugging.
     * @return Qualified name of the class.
     */
    public String getQualifiedName() {
        if (pkg != null) {
            return pkg.getName() + "." + name;
        }
        return name;
    }

    public List<RosettaMethod> getMethods() {
        return methods;
    }

    public Map<String, RosettaField> getFields() {
        return fields;
    }

    public String getName() {
        return name;
    }

    public @Nullable RosettaPackage getPackage() {
        return pkg;
    }

    public List<RosettaConstructor> getConstructors() {
        return constructors;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isDeprecated() {
        return this.deprecated;
    }

    RosettaClass(String name) {
        this.name = name;
    }
}
