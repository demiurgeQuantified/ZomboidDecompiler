package com.github.zomboiddecompiler.rosetta.vineflower;

/**
 * Provides pretty names for types for use in variable renaming.
 */
public interface ITypeNameProvider {
    /**
     * Returns the pretty name for a type based on the original type name.
     * @param typeName The raw name of the type.
     * @return The pretty name of the type.
     */
    String renameType(String typeName);
}
