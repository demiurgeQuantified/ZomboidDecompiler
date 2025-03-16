package com.github.zomboiddecompiler.rosetta.vineflower;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.struct.StructClass;

public class DefaultTypeNameProvider implements ITypeNameProvider {
    /**
     * Removes package names from a fully qualified type name.
     * @param typeName A fully qualified type name.
     * @return The same type name, with the package names removed.
     */
    public String trimPackages(String typeName) {
        return typeName.substring(typeName.lastIndexOf("/") + 1);
    }

    /**
     * Removes the I prefix from the name of the type if it is an interface.
     * In the interest of not mangling type names, the implementation is strict on what it considers a valid prefix.
     * @param typeName The name of a type.
     * @return The name with the I prefix removed.
     * The unmodified name will be returned if it was not an interface or the prefix was not detected.
     */
    public String trimInterfacePrefix(String typeName) {
        // if the class is an interface,
        // and its name starts with I followed by a capital and then non-capital letter, remove the I
        // e.g. IVariableNameProvider -> VariableNameProvider
        // we don't count repeat capitals as I might be part of an acronym
        @Nullable StructClass clazz = DecompilerContext.getStructContext().getClass(typeName);
        if (clazz != null
                && clazz.hasModifier(CodeConstants.ACC_INTERFACE)
                && typeName.length() > 3
                && typeName.startsWith("I")
                && Character.isUpperCase(typeName.codePointAt(1))
                && Character.isLowerCase(typeName.codePointAt(2))) {
            typeName = typeName.substring(1);
        }

        return typeName;
    }

    /**
     * Removes outer class names from a type name, if there are any.
     * @param typeName The name of a type.
     * @return The type name trimmed down to the innermost type name.
     */
    public String trimOuterClasses(String typeName) {
        return typeName.substring(typeName.lastIndexOf('$') + 1);
    }

    @Override
    public String renameType(String typeName) {
        typeName = trimPackages(typeName);
        typeName = trimOuterClasses(typeName);
        typeName = trimInterfacePrefix(typeName);
        return convertToCamelCase(typeName);
    }

    private String convertToCamelCase(String str) {
        if (isAllUpperCase(str)) {
            return str.toLowerCase();
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private boolean isAllUpperCase(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (Character.isLowerCase(str.codePointAt(i))) {
                return false;
            }
        }
        return true;
    }
}
