package com.github.zomboiddecompiler.rosetta.vineflower;

import com.github.zomboiddecompiler.rosetta.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.collections.VBStyleCollection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VineflowerUtils {
    /**
     * Returns the 'raw' name of the type represented by a VarType.
     * If the variable is an array type, this will not be represented in the returned string.
     * @param varType The VarType.
     * @return Raw name of the represented type.
     */
    public static String getRawTypeName(VarType varType) {
        assert varType.value != null;
        return switch (varType.type) {
            case VOID -> "void";
            case BOOLEAN -> "boolean";
            case FLOAT -> "float";
            case CHAR -> "char";
            case INT -> "int";
            case BYTE -> "byte";
            case DOUBLE -> "double";
            case LONG -> "long";
            case SHORT -> "short";
            default -> varType.value;
        };
    }

    public static String getTypeName(VarType varType) {
        String typeName = getRawTypeName(varType);
        return typeName.substring(typeName.lastIndexOf("/") + 1).replace("$", ".");
    }

    public static boolean signaturesMatch(RosettaExecutable executable, MethodDescriptor descriptor) {
        if (!Objects.equals(
                executable.getReturn().getType(),
                getTypeName(descriptor.ret))) {
            return false;
        }

        VarType[] parameterTypes = descriptor.params;
        if (parameterTypes.length != executable.getParameters().size()) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            VarType parameterType = parameterTypes[i];
            String rosettaType = executable.getParameters().get(i).getType();

            if (!Objects.equals(getTypeName(parameterType), rosettaType)) {
                return false;
            }
        }

        return true;
    }

    public static @Nullable RosettaExecutable getMatchingExecutable(RosettaClass clazz, StructMethod method) {
        MethodDescriptor descriptor = method.methodDescriptor();
        if (descriptor == null) {
            return null;
        }

        if (Objects.equals(method.getName(), "<init>")) { // constructor
            for (RosettaConstructor constructor : clazz.getConstructors()) {
                if (VineflowerUtils.signaturesMatch(constructor, descriptor)) {
                    return constructor;
                }
            }
        } else { // regular method
            boolean isStaticMethod = method.hasModifier(CodeConstants.ACC_STATIC);

            for (RosettaMethod classMethod : clazz.getMethods()) {
                if (!Objects.equals(classMethod.getName(), method.getName())) continue;
                if (isStaticMethod != classMethod.isStatic()) continue;

                if (VineflowerUtils.signaturesMatch(classMethod, descriptor)) {
                    return classMethod;
                }
            }
        }

        return null;
    }

    public static Map<String, RosettaClass> buildClassMap(List<RosettaNamespace> namespaces) {
        Map<String, RosettaClass> classes = new HashMap<>();
        for (RosettaNamespace namespace : namespaces) {
            for (RosettaClass clazz: namespace.getClasses()) {
                // convert the class name into the format vineflower gives them
                classes.put(namespace.getName().replace(".", "/") + "/" +
                        clazz.getName().replace(".", "$"), clazz);
            }
        }
        return classes;
    }

    /**
     * Renames a parameter name if necessary to avoid shadowing a field.
     * @param clazz The class the method belongs to.
     * @param name Name of the parameter.
     * @return If the name shadowed a field, a name that doesn't shadow a field. Otherwise the original name is returned.
     */
    public static String renameParameterIfNeeded(StructClass clazz, String name) {
        VBStyleCollection<StructField, String> fields = clazz.getFields();
        for (StructField field : fields) {
            if (Objects.equals(field.getName(), name)) {
                name = "_" + name;
                break;
            }
        }

        return name;
    }
}
