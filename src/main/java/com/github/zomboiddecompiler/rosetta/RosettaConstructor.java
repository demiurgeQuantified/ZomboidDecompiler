package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RosettaConstructor implements RosettaExecutable {
    private final List<RosettaParameter> parameters = new ArrayList<>();
    private String notes = "";
    private boolean deprecated = false;

    RosettaClass clazz = null;

    public void addParameter(RosettaParameter parameter) {
        parameters.add(parameter);
    }

    /**
     * Returns a more specific name, useful for debugging.
     * @return Qualified name of the constructor.
     */
    public String getQualifiedName() {
        if (clazz != null) {
            return clazz.getQualifiedName() + "#<ctor" + clazz.getConstructors().indexOf(this) + ">";
        }
        return "unknown constructor";
    }

    @Override
    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    public List<RosettaParameter> getParameters() {
        return parameters;
    }

    @Override
    public boolean isDeprecated() {
        return this.deprecated;
    }

    @Override
    public RosettaReturn getReturn() {
        return RosettaReturn.VOID;
    }

    public @Nullable RosettaClass getClazz() {
        return clazz;
    }
}
