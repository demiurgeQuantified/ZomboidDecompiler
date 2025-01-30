package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RosettaMethod {
    private final String name;
    private String returnType;

    private final List<Parameter> parameters = new ArrayList<>();
    private boolean isStatic = false;

    RosettaClass clazz = null;

    /**
     * Returns a more specific name, useful for debugging.
     * @return Qualified name of the method.
     */
    public String getQualifiedName() {
        String qualifiedName = name;
        if (clazz != null) {
            qualifiedName = clazz.getName() + "#" + qualifiedName;
            if (clazz.namespace != null) {
                qualifiedName = clazz.namespace.getName() + "." + qualifiedName;
            }
        }
        return qualifiedName;
    }

    /**
     * Returns a string describing the method's signature for debugging purposes.
     * @return String signature of the method.
     */
    public String getSignatureString() {
        StringBuilder signature = new StringBuilder(name).append("(");
        for (Parameter parameter: parameters) {
            signature.append(parameter.type).append(" ").append(parameter.name).append(", ");
        }
        signature.append(") -> ").append(returnType);
        return signature.toString();
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getReturnType() {
        return returnType;
    }

    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
        parameter.method = this;
    }

    public List<Parameter> getParameters() {
        return parameters;
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

    public RosettaMethod(String name) {
        this.name = name;
    }

    public static class Parameter {
        private final String name;
        private final String type;

        RosettaMethod method = null;

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public @Nullable RosettaMethod getMethod() {
            return method;
        }

        public Parameter(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
