package com.github.zomboiddecompiler.rosetta.vineflower;

import com.github.zomboiddecompiler.rosetta.RosettaPackage;
import com.github.zomboiddecompiler.rosetta.RosettaParser;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.util.Pair;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class RosettaPlugin implements Plugin {
    /**
      * Name of a compiler property that RosettaNamespaces to be used for documentation can be passed in through.
      * The property should contain List<RosettaNamespace> or null.
      */
    public static String NAMESPACE_PROPERTY_NAME = "rosetta:namespaces";
    /**
     * Name of a compiler property that a class that provides pretty names for types can be passed in through.
     */
    public static String TYPE_NAMER_PROPERTY_NAME = "rosetta:typenamer";

    @Override
    public String id() {
        return "rosetta";
    }

    @Override
    public String description() {
        return "Renames method parameters using data from Rosetta files.";
    }

    @Override
    public @Nullable IVariableNamingFactory getRenamingFactory() {
        return namingFactory;
    }

    @Override
    public @Nullable PluginOptions getPluginOptions() {
        return () -> Pair.of(RosettaPluginOptions.class, RosettaPluginOptions::addDefaults);
    }

    public RosettaPlugin() {
        List<RosettaPackage> namespaces = loadNamespaces();

        IFabricJavadocProvider javadocProvider = (IFabricJavadocProvider)DecompilerContext.getProperty(IFabricJavadocProvider.PROPERTY_NAME);
        if (javadocProvider instanceof RosettaJavadocProvider rosettaProvider) {
            rosettaProvider.addClassesFromNamespaces(namespaces);
        }

        namingFactory.addClassesFromNamespaces(namespaces);
    }

    private final RosettaNamingFactory namingFactory = new RosettaNamingFactory();

    private List<RosettaPackage> loadNamespaces() {
        List<RosettaPackage> namespaces = new ArrayList<>();

        String rosettaDir = (String)DecompilerContext.getProperty(RosettaPluginOptions.ROSETTA_DIRECTORY);
        if (rosettaDir != null) {
            Path rosettaPath = resolveRosettaPath(rosettaDir);
            if (rosettaPath == null) {
                DecompilerContext.getLogger().writeMessage("Rosetta path did not resolve to a valid directory. Ignoring.", IFernflowerLogger.Severity.WARN);
            } else {
                RosettaParser parser = new RosettaParser();
                parser.parseDirectory(rosettaPath);
                namespaces.addAll(parser.packages);
            }
        }

        namespaces.addAll(getPropertyNamespaces());

        return namespaces;
    }

    @SuppressWarnings("unchecked")
    private List<RosettaPackage> getPropertyNamespaces() {
        Object namespaces = DecompilerContext.getProperty(NAMESPACE_PROPERTY_NAME);
        if (namespaces == null) {
            return new ArrayList<>();
        }

        if (!(namespaces instanceof List)) {
            DecompilerContext.getLogger().writeMessage("Option " + NAMESPACE_PROPERTY_NAME + " must be an instance of List<RosettaNamespace>. Ignoring.", IFernflowerLogger.Severity.WARN);
            return new ArrayList<>();
        }

        return (List<RosettaPackage>)namespaces;
    }

    private @Nullable Path resolveRosettaPath(String directory) {
        Path rosettaPath = Paths.get(directory);
        if (!Files.exists(rosettaPath) || !Files.isDirectory(rosettaPath)) {
            return null;
        }
        return rosettaPath;
    }
}
