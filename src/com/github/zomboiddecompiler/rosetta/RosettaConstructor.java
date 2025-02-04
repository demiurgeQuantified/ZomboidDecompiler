package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RosettaConstructor implements RosettaExecutable {
    private final List<RosettaParameter> parameters = new ArrayList<>();

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

    public List<RosettaParameter> getParameters() {
        return parameters;
    }

    public @Nullable RosettaClass getClazz() {
        return clazz;
    }

    @Override
    public String getReturnType() {
        return "void";
    }

    @Override
    public String getReturnName() {
        return "";
    }
}
