package com.github.zomboiddecompiler.rosetta;

import java.util.ArrayList;
import java.util.List;

public class RosettaPackage {
    private final String name;
    private final List<RosettaClass> classes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void addClass(RosettaClass clazz) {
        classes.add(clazz);
        clazz.pkg = this;
    }

    public List<RosettaClass> getClasses() {
        return classes;
    }

    public RosettaPackage(String name) {
        this.name = name;
    }
}
