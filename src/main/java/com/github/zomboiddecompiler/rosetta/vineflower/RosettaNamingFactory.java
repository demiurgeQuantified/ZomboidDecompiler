package com.github.zomboiddecompiler.rosetta.vineflower;

import com.github.zomboiddecompiler.rosetta.RosettaClass;
import com.github.zomboiddecompiler.rosetta.RosettaExecutable;
import com.github.zomboiddecompiler.rosetta.RosettaNamespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.struct.StructMethod;

import java.util.List;
import java.util.Map;

public class RosettaNamingFactory implements IVariableNamingFactory {
    private final Map<String, RosettaClass> classes;
    /// Generic variable name provider for when there is no rosetta data
    private static final IVariableNameProvider DEFAULT = new RosettaGenericNameProvider();

    @Override
    public @NotNull IVariableNameProvider createFactory(StructMethod method) {
        RosettaClass rosettaClass = classes.get(method.getClassQualifiedName());
        if (rosettaClass == null) {
            return DEFAULT;
        }

        RosettaExecutable executable = VineflowerUtils.getMatchingExecutable(rosettaClass, method);

        if (executable == null) {
//            ZomboidDecompiler.log.log("No rosetta data found for "
//                    + method.getClassQualifiedName() + "#" + method.getName());
            return DEFAULT;
        }

        return new RosettaNameProvider(executable,
                                       DecompilerContext.getContextProperty(DecompilerContext.CURRENT_CLASS));
    }

    RosettaNamingFactory(List<RosettaNamespace> namespaces) {
        classes = VineflowerUtils.buildClassMap(namespaces);
    }
}
