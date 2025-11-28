package com.github.zomboiddecompiler.rosetta.vineflower;

import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.LinkedHashMap;
import java.util.Map;

/// Name provider for methods with no Rosetta data.
public class RosettaGenericNameProvider extends AbstractRosettaNameProvider {
    @Override
    public Map<VarVersionPair, String> rename(Map<VarVersionPair, Pair<VarType, String>> variables) {
        Map<VarVersionPair, VarType> unknownVariables = new LinkedHashMap<>();
        // TODO: if instance method, name of first variable doesn't matter
        // it should not be added in this case or it will affect name indices
        for (var entry : variables.entrySet()) {
            // invisible this argument to instance methods is null
            if (entry.getValue().a != null) {
                unknownVariables.put(entry.getKey(), entry.getValue().a);
            }
        }

        return assignUnknownVariableNames(unknownVariables, null);
    }
}
