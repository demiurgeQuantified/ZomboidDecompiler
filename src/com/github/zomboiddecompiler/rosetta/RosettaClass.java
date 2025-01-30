package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RosettaClass {
    private final String name;
    private final List<RosettaMethod> methods = new ArrayList<>();

    RosettaNamespace namespace = null;

    public void addMethod(RosettaMethod method) {
        methods.add(method);
        method.clazz = this;
    }

    public List<RosettaMethod> getMethods() {
        return methods;
    }

    public String getName() {
        return name;
    }

    public @Nullable RosettaNamespace getNamespace() {
        return namespace;
    }

    RosettaClass(String name) {
        this.name = name;
    }
}
