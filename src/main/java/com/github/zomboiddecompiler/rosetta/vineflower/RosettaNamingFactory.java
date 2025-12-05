package com.github.zomboiddecompiler.rosetta.vineflower;

import com.github.zomboiddecompiler.rosetta.RosettaClass;
import com.github.zomboiddecompiler.rosetta.RosettaExecutable;
import com.github.zomboiddecompiler.rosetta.RosettaPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.struct.StructMethod;

import java.util.List;
import java.util.Map;

public class RosettaNamingFactory implements IVariableNamingFactory {
    @Override
    public @NotNull IVariableNameProvider createFactory(StructMethod method) {
        RosettaClass rosettaClass = classes.get(method.getClassQualifiedName());
        if (rosettaClass == null) {
            return new RosettaGenericNameProvider(method);
        }

        RosettaExecutable executable = VineflowerUtils.getMatchingExecutable(rosettaClass, method);

        if (executable == null) {
            return new RosettaGenericNameProvider(method);
        }

        return new RosettaNameProvider(
                executable,
                DecompilerContext.getContextProperty(DecompilerContext.CURRENT_CLASS),
                method
        );
    }

    private Map<String, RosettaClass> classes;

    void addClassesFromNamespaces(List<RosettaPackage> namespaces) {
        classes = VineflowerUtils.buildClassMap(namespaces);
    }
}
