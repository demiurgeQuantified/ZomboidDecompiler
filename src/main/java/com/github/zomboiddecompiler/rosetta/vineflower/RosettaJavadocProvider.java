package com.github.zomboiddecompiler.rosetta.vineflower;

import com.github.zomboiddecompiler.rosetta.*;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructMethodParametersAttribute;

import java.util.*;

import static org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute.ATTRIBUTE_METHOD_PARAMETERS;

public class RosettaJavadocProvider implements IFabricJavadocProvider {
    private Map<String, RosettaClass> classes = new HashMap<>();

    @Override
    public @Nullable String getClassDoc(StructClass clazz) {
        if (!classes.containsKey(clazz.qualifiedName)) {
            return null;
        }

        RosettaClass rosettaClass = classes.get(clazz.qualifiedName);
        JavadocBuilder javadoc = new JavadocBuilder();
        javadoc.append(rosettaClass.getNotes());

        if (rosettaClass.isDeprecated()) {
            javadoc.append("@deprecated");
        }

        if (javadoc.isEmpty()) {
            return null;
        }

        return javadoc.build();
    }

    @Override
    public @Nullable String getFieldDoc(StructClass clazz, StructField field) {
        if (!classes.containsKey(clazz.qualifiedName)) {
            return null;
        }

        RosettaClass rosettaClazz = classes.get(clazz.qualifiedName);
        if (!rosettaClazz.getFields().containsKey(field.getName())) {
            return null;
        }

        RosettaField rosettaField = rosettaClazz.getFields().get(field.getName());
        JavadocBuilder javadoc = new JavadocBuilder();

        javadoc.append(rosettaField.getNotes());

        if (rosettaField.isDeprecated()) {
            javadoc.append("@deprecated");
        }

        if (javadoc.isEmpty()) {
            return null;
        }

        return javadoc.build();
    }

    @Override
    public @Nullable String getMethodDoc(StructClass clazz, StructMethod method) {
        if (!classes.containsKey(clazz.qualifiedName)) {
            return null;
        }
        RosettaClass rosettaClazz = classes.get(clazz.qualifiedName);

        RosettaExecutable executable = VineflowerUtils.getMatchingExecutable(rosettaClazz, method);
        if (executable == null) {
            return null;
        }

        JavadocBuilder javadoc = new JavadocBuilder();
        javadoc.append(executable.getNotes());

        javadoc.append(getParameterDocs(method, executable));

        if (executable.getReturn() != RosettaReturn.VOID) {
            RosettaReturn returns = executable.getReturn();
            if (!returns.getNotes().isBlank()) {
                javadoc.append("@return " + returns.getNotes());
            }
        }

        if (executable.isDeprecated()) {
            javadoc.append("@deprecated");
        }

        if (javadoc.isEmpty()) {
            return null;
        }

        return javadoc.build();
    }

    public void addClassesFromNamespaces(List<RosettaPackage> classes) {
        this.classes = VineflowerUtils.buildClassMap(classes);
    }

    private String getParameterName(StructMethod method, int index) {
        if (!method.hasModifier(CodeConstants.ACC_STATIC)
                && !method.getName().equals(CodeConstants.INIT_NAME)
                && !method.getName().equals(CodeConstants.CLINIT_NAME)) {
            index += 1;
        }

        if (method.getLocalVariableAttr() != null) {
            return method.getLocalVariableAttr().getVariables().toList().get(index).getName();
        } else {
            StructMethodParametersAttribute parameters = method.getAttribute(ATTRIBUTE_METHOD_PARAMETERS);
            if (parameters != null) {
                return parameters.getEntries().get(index).myName;
            }
        }

        // FIXME: interfaces don't keep their MethodParameters and their methods don't have code
        //  so there's no way to get their parameter names :(
        //  the parameter rename doesn't run until AFTER javadoc
        return "ERROR";
    }

    private String getParameterDocs(StructMethod method, RosettaExecutable executable) {
        StringBuilder parameterBuilder = new StringBuilder();

        boolean anyNotes = false;
        for (int i = 0; i < executable.getParameters().size(); i++) {
            RosettaParameter parameter = executable.getParameters().get(i);
            parameterBuilder.append("\n");

            parameterBuilder.append("@param ")
                    .append(this.getParameterName(method, i));

            String notes = parameter.getNotes();
            if (!notes.isBlank()) {
                anyNotes = true;
                parameterBuilder.append(" ")
                        .append(parameter.getNotes());
            }
        }

        if (!anyNotes) {
            // if no parameter had a note, remove parameter annotations
            return "";
        }

        return parameterBuilder.toString();
    }

    private static class JavadocBuilder {
        public void append(String string) {
            if (string.isBlank()) {
                return;
            }

            if (!this.builder.isEmpty()) {
                this.builder.append("\n");
            }

            this.builder.append(string);
        }

        public String build() {
            return this.builder.toString();
        }

        public boolean isEmpty() {
            return this.builder.isEmpty();
        }

        private final StringBuilder builder = new StringBuilder();
    }
}
