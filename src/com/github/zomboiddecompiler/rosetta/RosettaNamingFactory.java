package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.IdentityRenamerFactory;
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
    // generic variable name provider for when there is no valid rosetta data
    // TODO: replace this with a RosettaParser that does the fancy naming without the var names
    private static final IVariableNameProvider DEFAULT = new IdentityRenamerFactory();

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
            default -> varType.value.substring(varType.value.lastIndexOf("/") + 1).replace("$", ".");
        };
    }

    @Override
    public @NotNull IVariableNameProvider createFactory(StructMethod method) {
        RosettaClass rosettaClass = classes.get(method.getClassQualifiedName());
        if (rosettaClass == null) {
            return DEFAULT;
        }

        boolean isStaticMethod = (method.getAccessFlags() & CodeConstants.ACC_STATIC) != 0;

        RosettaMethod rosettaMethod = null;
        for (RosettaMethod classMethod : rosettaClass.getMethods()) {
            if (!Objects.equals(classMethod.getName(), method.getName())) continue;
            if (isStaticMethod != classMethod.isStatic()) continue;
            MethodDescriptor descriptor = method.methodDescriptor();
            if (descriptor != null) {
                if (!Objects.equals(
                        classMethod.getReturnType(),
                        getTypeName(descriptor.ret))) {
                    continue;
                }

                VarType[] parameterTypes = descriptor.params;
                if (parameterTypes.length != classMethod.getParameters().size()) continue;

                boolean parametersMatch = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    VarType parameterType = parameterTypes[i];
                    String rosettaType = classMethod.getParameters().get(i).getType();

                    if (!Objects.equals(getTypeName(parameterType), rosettaType)) {
                        parametersMatch = false;
                        break;
                    }
                }

                if (!parametersMatch) {
                    continue;
                }
            }

            rosettaMethod = classMethod;

            break;
        }

        if (rosettaMethod == null) {
            return DEFAULT;
        }

        // TODO: there might be a decent performance benefit from pooling these
        return new RosettaNameProvider(rosettaMethod);
    }

    RosettaNamingFactory(@NotNull List<RosettaNamespace> namespaces) {
        for (RosettaNamespace namespace : namespaces) {
            for (RosettaClass clazz: namespace.getClasses()) {
                // convert the class name into the format vineflower gives them
                classes.put(namespace.getName().replace(".", "/") + "/" +
                        clazz.getName().replace(".", "$"), clazz);
            }
        }
    }
}
