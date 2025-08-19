package com.github.zomboiddecompiler.rosetta.vineflower;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Base class for Rosetta name providers.
public abstract class AbstractRosettaNameProvider implements IVariableNameProvider {
    @Override
    public void addParentContext(IVariableNameProvider renamer) {

    }

    /**
     * Assigns good names to variables based on their type.
     * @param variables The variables to assign names for.
     * @param invalidNames Names that should not be used. This is best used to avoid giving names already taken in that scope.
     * @return Map of variables to their new names.
     */
    protected Map<VarVersionPair, String> assignUnknownVariableNames(Map<VarVersionPair, VarType> variables,
                                                                     @Nullable Set<String> invalidNames) {
        Map<VarVersionPair, String> result = new LinkedHashMap<>();

        // map of variables by their string type name
        // the string is used so that type names that end up the same will share an id space
        Map<String, List<VarVersionPair>> variableTypeMap = new LinkedHashMap<>();
        for (var entry : variables.entrySet()) {
            String typeName = getDefaultVariableName(entry.getValue());
            variableTypeMap.putIfAbsent(typeName, new ArrayList<>());
            variableTypeMap.get(typeName).add(entry.getKey());
        }

        for (var entry : variableTypeMap.entrySet()) {
            List<VarVersionPair> vars = entry.getValue();
            String typeName = entry.getKey();

            if (vars.size() == 1) {
                if (AbstractRosettaNameProvider.reservedNames.contains(typeName)) {
                    typeName = typeName + 0;
                }
                result.put(vars.get(0), getValidName(typeName, invalidNames));
                continue;
            }

            for (int i = 0; i < vars.size(); i++) {
                String name = getValidName(typeName + i, invalidNames);
                result.put(vars.get(i), name);
            }
        }

        return result;
    }

    /// Names that should never be used as variable names without a number suffix.
    private final static Set<String> reservedNames = Set.of(
            "byte",
            "short",
            "int",
            "long",
            "float",
            "double",
            "boolean",
            "char",
            "zombie"
    );

    private static boolean invalidTypeNameProviderWarned = false;

    private final static ITypeNameProvider DEFAULT_NAME_PROVIDER = new DefaultTypeNameProvider();

    /**
     * Ensures the passed name is a valid name for Java code, returning it if it is or a new valid name if it isn't.
     * @param name The name to check for validity.
     * @param invalidNames A set of names that should not be used.
     * @return A valid name for a Java variable.
     */
    private String getValidName(String name, @Nullable Set<String> invalidNames) {
        if (!SourceVersion.isName(name) || (invalidNames != null && invalidNames.contains(name))) {
            name = "_" + name;
            assert SourceVersion.isName(name) && (invalidNames == null || !invalidNames.contains(name));
        }
        return name;
    }

    /**
     * Returns a 'pretty' name for a type for use in naming variables.
     * @param type The type of the variable.
     * @return Pretty name for the variable.
     */
    private String getDefaultVariableName(VarType type) {
        Object nameProvider = DecompilerContext.getProperty(RosettaPlugin.TYPE_NAMER_PROPERTY_NAME);
        if (nameProvider != null) {
            if (nameProvider instanceof ITypeNameProvider) {
                return ((ITypeNameProvider)nameProvider).nameVar(type);
            } else if (!invalidTypeNameProviderWarned) {
                DecompilerContext.getLogger().writeMessage(
                        "Type name provider must be an instance of ITypeNameProvider. Ignoring.",
                        IFernflowerLogger.Severity.WARN);
                invalidTypeNameProviderWarned = true;
            }
        }
        return DEFAULT_NAME_PROVIDER.nameVar(type);
    }
}
