package com.github.zomboiddecompiler.rosetta;

public class RosettaParameter {
    private final String name;
    private final String type;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public RosettaParameter(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
