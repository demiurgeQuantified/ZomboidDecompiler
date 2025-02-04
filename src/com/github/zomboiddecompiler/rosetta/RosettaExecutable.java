package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface RosettaExecutable {
    List<RosettaParameter> getParameters();
    String getReturnType();
    String getReturnName();
}
