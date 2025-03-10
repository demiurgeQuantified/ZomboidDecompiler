package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RosettaMethod implements RosettaExecutable {
    private final String name;
    private String notes = "";

    private final RosettaReturn returns;
    private final List<RosettaParameter> parameters = new ArrayList<>();
    private boolean isStatic = false;

    RosettaClass clazz = null;

    /**
     * Returns a more specific name, useful for debugging.
     * @return Qualified name of the method.
     */
    public String getQualifiedName() {
        String qualifiedName = name;
        if (clazz != null) {
            qualifiedName = clazz.getQualifiedName() + "#" + qualifiedName;
        }
        return qualifiedName;
    }

    /**
     * Returns a string describing the method's signature for debugging purposes.
     * @return String signature of the method.
     */
    public String getSignatureString() {
        StringBuilder signature = new StringBuilder(name).append("(");
        for (RosettaParameter parameter: parameters) {
            signature.append(parameter.getType()).append(" ").append(parameter.getName()).append(", ");
        }
        signature.append(") -> ").append(returns.getType());
        return signature.toString();
    }

    public void addParameter(RosettaParameter parameter) {
        parameters.add(parameter);
    }

    @Override
    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public List<RosettaParameter> getParameters() {
        return parameters;
    }

    @Override
    public RosettaReturn getReturn() {
        return returns;
    }

    public String getName() {
        return name;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean bStatic) {
        isStatic = bStatic;
    }

    public @Nullable RosettaClass getClazz() {
        return clazz;
    }

    public RosettaMethod(String name, RosettaReturn returns) {
        this.name = name;
        this.returns = returns;
    }

    public RosettaMethod(String name) {
        this(name, RosettaReturn.VOID);
    }
}
