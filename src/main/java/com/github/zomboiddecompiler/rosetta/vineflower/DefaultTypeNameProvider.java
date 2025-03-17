package com.github.zomboiddecompiler.rosetta.vineflower;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;

import java.util.Map;
import java.util.Set;

public class DefaultTypeNameProvider implements ITypeNameProvider {
    @Override
    public String nameVar(VarType type) {
        String typeName = VineflowerUtils.getRawTypeName(type);

        // in practice, it doesn't seem like isGeneric() ever returns true
        // this may start working when the generic inference plugin is written
//        if (COLLECTION_TYPES.contains(typeName) && type.isGeneric()) {
//            GenericType genericType = (GenericType)type;
//            if (!genericType.isTypeUnfinished()) {
//                return nameVar(genericType.getArguments().get(0)) + "s";
//            }
//        }

        if (TYPE_NAME_OVERRIDES.containsKey(typeName)) {
            typeName = TYPE_NAME_OVERRIDES.get(typeName);
        } else {
            typeName = trimPackages(typeName);
            typeName = trimOuterClasses(typeName);
            typeName = trimInterfacePrefix(typeName);
        }
        if (type.arrayDim > 0) {
            typeName = typeName + "s";
        }
        return convertToCamelCase(typeName);
    }

    /**
     * Removes package names from a fully qualified type name.
     * @param typeName A fully qualified type name.
     * @return The same type name, with the package names removed.
     */
    public static String trimPackages(String typeName) {
        return typeName.substring(typeName.lastIndexOf("/") + 1);
    }

    /**
     * Removes the I prefix from the name of the type if it is an interface.
     * In the interest of not mangling type names, the implementation is strict on what it considers a valid prefix.
     * @param typeName The name of a type.
     * @return The name with the I prefix removed.
     * The unmodified name will be returned if it was not an interface or the prefix was not detected.
     */
    public static String trimInterfacePrefix(String typeName) {
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
    public static String trimOuterClasses(String typeName) {
        return typeName.substring(typeName.lastIndexOf('$') + 1);
    }

    /// Types in this set are named like array types (plural of type name).
    private static final Set<String> COLLECTION_TYPES = Set.of(
            "java/util/ArrayList",
            "java/util/Set",
            "java/util/Vector",
            "java/util/HashMap",
            "java/util/LinkedHashMap",
            "java/util/HashSet",
            "java/util/LinkedHashSet"
    );

    private static final Map<String, String> TYPE_NAME_OVERRIDES = Map.of(
            "java/lang/Class", "clazz"
    );

    private static String convertToCamelCase(String str) {
        if (isAllUpperCase(str)) {
            return str.toLowerCase();
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private static boolean isAllUpperCase(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (Character.isLowerCase(str.codePointAt(i))) {
                return false;
            }
        }
        return true;
    }
}
