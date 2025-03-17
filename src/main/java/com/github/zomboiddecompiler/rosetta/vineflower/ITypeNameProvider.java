package com.github.zomboiddecompiler.rosetta.vineflower;

import org.jetbrains.java.decompiler.struct.gen.VarType;

/**
 * Provides pretty names for types for use in variable renaming.
 */
public interface ITypeNameProvider {
    /**
     * Returns the pretty name for a type.
     * @param type The type.
     * @return The pretty name of the type.
     */
    String nameVar(VarType type);
}
