package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

public class RosettaField {
    private final String name;
    private final String type;
    private String notes = "";
    private boolean deprecated;

    RosettaClass clazz = null;

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
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

    public boolean isDeprecated() {
        return this.deprecated;
    }

    public @Nullable RosettaClass getClazz() {
        return clazz;
    }

    public RosettaField(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
