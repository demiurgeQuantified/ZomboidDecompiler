package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RosettaNamingFactory implements IVariableNamingFactory {
    private final HashMap<String, RosettaClass> classes = new HashMap<>();
    /// Generic variable name provider for when there is no rosetta data
    private static final IVariableNameProvider DEFAULT = new RosettaGenericNameProvider();

    public static String getTypeName(VarType varType) {
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
            default -> varType.value.substring(
                    varType.value.lastIndexOf("/") + 1).replace("$", ".");
        };
    }

    static boolean signaturesMatch(RosettaExecutable executable, MethodDescriptor descriptor) {
        if (!Objects.equals(
                executable.getReturnType(),
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

    @Override
    public @NotNull IVariableNameProvider createFactory(StructMethod method) {
        RosettaClass rosettaClass = classes.get(method.getClassQualifiedName());
        if (rosettaClass == null) {
            return DEFAULT;
        }

        MethodDescriptor descriptor = method.methodDescriptor();
        if (descriptor == null) {
            return DEFAULT;
        }

        RosettaExecutable executable = null;
        if (Objects.equals(method.getName(), "<init>")) { // constructor
            for (RosettaConstructor constructor : rosettaClass.getConstructors()) {
                if (signaturesMatch(constructor, descriptor)) {
                    executable = constructor;
                    break;
                }
            }
        } else {
            boolean isStaticMethod = method.hasModifier(CodeConstants.ACC_STATIC);

            for (RosettaMethod classMethod : rosettaClass.getMethods()) {
                if (!Objects.equals(classMethod.getName(), method.getName())) continue;
                if (isStaticMethod != classMethod.isStatic()) continue;

                if (signaturesMatch(classMethod, descriptor)) {
                    executable = classMethod;
                    break;
                }
            }
        }

        if (executable == null) {
//            ZomboidDecompiler.log.log("No rosetta data found for "
//                    + method.getClassQualifiedName() + "#" + method.getName());
            return DEFAULT;
        }

        return new RosettaNameProvider(executable,
                                       DecompilerContext.getContextProperty(DecompilerContext.CURRENT_CLASS));
    }

    RosettaNamingFactory(List<RosettaNamespace> namespaces) {
        for (RosettaNamespace namespace : namespaces) {
            for (RosettaClass clazz: namespace.getClasses()) {
                // convert the class name into the format vineflower gives them
                classes.put(namespace.getName().replace(".", "/") + "/" +
                        clazz.getName().replace(".", "$"), clazz);
            }
        }
    }
}
