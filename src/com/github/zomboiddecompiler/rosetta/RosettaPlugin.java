package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class RosettaPlugin implements Plugin {
    private static List<RosettaNamespace> NAMESPACES;
    @Override
    public String id() {
        return "rosetta";
    }

    @Override
    public String description() {
        return "Renames method parameters using data from a Rosetta file.";
    }

    @Override
    public @Nullable IVariableNamingFactory getRenamingFactory() {
        String rosettaDir = (String)DecompilerContext.getProperty(RosettaPluginOptions.ROSETTA_DIRECTORY);
        if (rosettaDir == null) {
            return null;
        }

        RosettaParser parser = new RosettaParser();

        File rosettaDirFile = new File(rosettaDir, "json");
        for (File file : rosettaDirFile.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;
            try {
                parser.parseJson(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return new RosettaNamingFactory(parser.namespaces);
    }

    @Override
    public @Nullable PluginOptions getPluginOptions() {
        return () -> Pair.of(RosettaPluginOptions.class, RosettaPluginOptions::addDefaults);
    }
}
