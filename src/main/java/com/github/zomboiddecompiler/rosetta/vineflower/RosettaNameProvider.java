package com.github.zomboiddecompiler.rosetta.vineflower;

import com.github.zomboiddecompiler.rosetta.RosettaExecutable;
import com.github.zomboiddecompiler.rosetta.RosettaMethod;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.*;

/// Name provider for methods with Rosetta parameter names.
public class RosettaNameProvider extends AbstractRosettaNameProvider {
    private final RosettaExecutable executable;
    private final StructClass vineflowerClass;

    /**
     * Gets the 'true' index of a variable from its 'raw' index.
     * Raw indices jump a number for double width types.
     * Raw indices consider 'this' as a function parameter for instance functions.
     * @param index Raw index of the variable.
     * @return True index of the variable.
     */
    private int getTrueVariableIndex(int index) {
        if (!(executable instanceof RosettaMethod method && method.isStatic())
                && !executable.getParameters().isEmpty()) {
            index -= 1;
        }

        int i = 0;
        // FIXME: this doesn't account for wide local variables
        // VarType has getStackSize that could be used for this but would need to rewrite this whole method
        while (i < index && i < executable.getParameters().size()) {
            String parameterType = executable.getParameters().get(i).getType();
            if (Objects.equals(parameterType, "long")
                    || Objects.equals(parameterType, "double")) {
                index--;
            }
            i++;
        }
        return index;
    }

    @Override
    public Map<VarVersionPair, String> rename(Map<VarVersionPair, Pair<VarType, String>> variables) {
        Map<VarVersionPair, String> result = new LinkedHashMap<>();
        Map<VarVersionPair, VarType> unknownVariables = new LinkedHashMap<>();
        Set<String> takenNames = new HashSet<>();

        for (var entry : variables.entrySet()) {
            VarVersionPair pair = entry.getKey();
            int index = getTrueVariableIndex(pair.var);

            if (index == -1) {
                assert !(executable instanceof RosettaMethod method && method.isStatic());
                result.put(pair, "this");
            } else if (index < executable.getParameters().size()) {
                String name = executable.getParameters().get(index).getName();
                name = VineflowerUtils.renameParameterIfNeeded(vineflowerClass, name);
                result.put(pair, name);
                takenNames.add(name);
            } else {
                // invisible this argument to instance methods is null
                if (entry.getValue().a != null) {
                    unknownVariables.put(pair, entry.getValue().a);
                }
            }
        }

        result.putAll(
                assignUnknownVariableNames(unknownVariables, takenNames));

        return result;
    }

    @Override
    public String renameAbstractParameter(String name, int index) {
        index = getTrueVariableIndex(index);
        name = executable.getParameters().get(index).getName();
        return VineflowerUtils.renameParameterIfNeeded(vineflowerClass, name);
    }

    @Override
    public String renameParameter(int flags, VarType type, String name, int index) {
        return renameAbstractParameter(name, index);
    }

    public RosettaNameProvider(RosettaExecutable executable, StructClass clazz) {
        this.executable = executable;
        this.vineflowerClass = clazz;
    }
}

