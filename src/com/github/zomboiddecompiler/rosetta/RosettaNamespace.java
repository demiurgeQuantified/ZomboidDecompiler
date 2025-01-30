package com.github.zomboiddecompiler.rosetta;

import java.util.ArrayList;
import java.util.List;

public class RosettaNamespace {
    private final String name;
    private final List<RosettaClass> classes = new ArrayList<>();

    String getName() {
        return name;
    }

    public void addClass(RosettaClass clazz) {
        classes.add(clazz);
        clazz.namespace = this;
    }

    public List<RosettaClass> getClasses() {
        return classes;
    }

    public RosettaNamespace(String name) {
        this.name = name;
    };
}
