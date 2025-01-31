package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class RosettaNameProvider implements IVariableNameProvider {
    private final RosettaMethod method;

    /**
     * Gets the 'true' index of a variable from its 'raw' index.
     * Raw indices jump a number for double width types
     * Raw indices consider 'this' as a function parameter for instance functions.
     * @param index Raw index of the variable.
     * @return True index of the variable.
     */
    private int getTrueVariableIndex(int index) {
        if (!method.isStatic() && !method.getParameters().isEmpty()) {
            index -= 1;
        }

        int i = 0;
        // FIXME: this doesn't account for wide local variables
        while (i < index && i < method.getParameters().size()) {
            String parameterType = method.getParameters().get(i).getType();
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
        assert method.getClazz() != null;
        if (method.getClazz().getFields().containsKey(name)) {
            // it's technically possible that this causes a conflict but it would require really stupid
            // field names that the game probably doesn't have any of
            name = name + "x";
        }
        return name;
    }

    @Override
    public Map<VarVersionPair, String> rename(Map<VarVersionPair, Pair<VarType, String>> variables) {
        Map<VarVersionPair, String> result = new LinkedHashMap<>();
        for (VarVersionPair pair : variables.keySet()) {
            int index = getTrueVariableIndex(pair.var);

            if (index == -1) {
                assert !method.isStatic();
                result.put(pair, "this");
            } else if (index < method.getParameters().size()) {
                String name = method.getParameters().get(index).getName();
                name = renameParameterIfNeeded(name);
                result.put(pair, name);
            } else {
                // TODO: count number of each type and use that count to name the variables
                // e.g. what is currently isoZombie1, isoPlayer2 would become isoZombie1, isoPlayer1
                // also if there is only one there's no need for a number at all
                String name = RosettaNamingFactory.getTypeName(variables.get(pair).a);
                name = name.substring(name.lastIndexOf(".") + 1);
                name = name.substring(0, 1).toLowerCase() + name.substring(1);
                name += (index - method.getParameters().size() + 1);
                result.put(pair, name);

            }
        }

        return result;
    }

    @Override
    public String renameAbstractParameter(String name, int index) {
        index = getTrueVariableIndex(index);
        name = method.getParameters().get(index).getName();
        return renameParameterIfNeeded(name);
    }

    @Override
    public String renameParameter(int flags, VarType type, String name, int index) {
        return renameAbstractParameter(name, index);
    }

    @Override
    public void addParentContext(IVariableNameProvider iVariableNameProvider) {
        // TODO: i don't know what this is even supposed to do lol
    }

    public RosettaNameProvider(RosettaMethod method) {
        this.method = method;
    }
}

