package com.github.zomboiddecompiler.rosetta;

import java.util.List;

public interface RosettaExecutable {
    void setNotes(String notes);
    String getNotes();

    List<RosettaParameter> getParameters();
    void addParameter(RosettaParameter parameter);

     void setDeprecated(boolean deprecated);
     boolean isDeprecated();

    RosettaReturn getReturn();
}
