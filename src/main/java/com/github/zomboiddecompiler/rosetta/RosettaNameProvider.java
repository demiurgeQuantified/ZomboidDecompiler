package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.Pair;
import org.jetbrains.java.decompiler.util.collections.VBStyleCollection;

import java.util.*;

/// Name provider for methods with Rosetta parameter names.
public class RosettaNameProvider extends AbstractRosettaNameProvider {
    private final RosettaExecutable executable;
    private final StructClass vineflowerClass;

    /**
     * Gets the 'true' index of a variable from its 'raw' index.
     * Raw indices jump a number for double width types
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

    /**
     * Renames a parameter name if necessary to avoid shadowing a field.
     * @param name Name of the parameter.
     * @return If the name shadowed a field, a name that doesn't shadow a field. Otherwise the original name is returned.
     */
    private String renameParameterIfNeeded(String name) {
        VBStyleCollection<StructField, String> fields = vineflowerClass.getFields();
        for (StructField field : fields) {
            if (Objects.equals(field.getName(), name)) {
                name = "_" + name;
                break;
            }
        }

        return name;
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
                name = renameParameterIfNeeded(name);
                result.put(pair, name);
                takenNames.add(name);
            } else {
                unknownVariables.put(pair, entry.getValue().a);
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
        return renameParameterIfNeeded(name);
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

