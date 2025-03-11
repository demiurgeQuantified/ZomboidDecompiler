package com.github.zomboiddecompiler.rosetta.vineflower;

import com.github.zomboiddecompiler.rosetta.*;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;

import java.util.*;

public class RosettaJavadocProvider implements IFabricJavadocProvider {
    private Map<String, RosettaClass> classes = new HashMap<>();

    @Override
    public @Nullable String getClassDoc(StructClass clazz) {
        if (!classes.containsKey(clazz.qualifiedName)) {
            return null;
        }

        String javadoc = classes.get(clazz.qualifiedName).getNotes();

        if (javadoc.isBlank()) {
            return null;
        }

        return javadoc;
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

        String javadoc= rosettaClazz.getFields().get(field.getName()).getNotes();


        if (javadoc.isBlank()) {
            return null;
        }

        return javadoc;
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

        boolean anyNotes = false;

        String notes = executable.getNotes();
        if (!notes.isBlank()) {
            anyNotes = true;
        }

        StringBuilder javadoc = new StringBuilder(notes);

        javadoc.append(getParameterDoc(clazz, executable));

        if (executable.getReturn() != RosettaReturn.VOID) {
            RosettaReturn returns = executable.getReturn();
            if (!javadoc.isEmpty()) {
                javadoc.append("\n");
            }

            notes = returns.getNotes();
            if (!notes.isBlank()) {
                anyNotes = true;
                javadoc.append("@return ")
                        .append(notes);
            }
        }

        if (!anyNotes) {
            return null;
        }

        return javadoc.toString();
    }

    public void addClassesFromNamespace(List<RosettaNamespace> classes) {
        this.classes = VineflowerUtils.buildClassMap(classes);
    }

    private String getParameterDoc(StructClass clazz, RosettaExecutable executable) {
        StringBuilder parameterBuilder = new StringBuilder();

        boolean anyNotes = false;
        for (RosettaParameter parameter : executable.getParameters()) {
            parameterBuilder.append("\n");

            parameterBuilder.append("@param ")
                    // rename for consistency with the code
                    .append(VineflowerUtils.renameParameterIfNeeded(clazz, parameter.getName()));

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
}
