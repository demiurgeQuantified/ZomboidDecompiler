package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences.*;

public interface RosettaPluginOptions {
    @Name("Rosetta Directory")
    @Description("Use rosetta data stored at the given path to give names to method parameters.")
    @Type(Type.STRING)
    String ROSETTA_DIRECTORY = "rosetta-directory";

    static void addDefaults(PluginOptions.AddDefaults cons) {
        cons.addDefault(ROSETTA_DIRECTORY, null);
    }
}
