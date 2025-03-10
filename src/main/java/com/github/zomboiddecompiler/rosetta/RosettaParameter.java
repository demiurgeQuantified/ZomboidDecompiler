package com.github.zomboiddecompiler.rosetta;

public class RosettaParameter {
    private final String name;
    private final String type;
    private String notes;

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

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
