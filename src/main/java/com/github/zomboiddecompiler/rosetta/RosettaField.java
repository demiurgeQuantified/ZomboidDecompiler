package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

public class RosettaField {
    private final String name;
    private final String type;

    RosettaClass clazz = null;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public @Nullable RosettaClass getClazz() {
        return clazz;
    }

    public RosettaField(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
