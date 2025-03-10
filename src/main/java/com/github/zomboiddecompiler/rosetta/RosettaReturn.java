package com.github.zomboiddecompiler.rosetta;

public class RosettaReturn {
    private final String name;
    private final String type;
    private String notes = "";

    public static final RosettaReturn VOID = new RosettaReturn("", "void");

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    RosettaReturn(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
