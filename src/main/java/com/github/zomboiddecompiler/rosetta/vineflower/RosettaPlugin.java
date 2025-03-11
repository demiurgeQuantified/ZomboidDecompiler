package com.github.zomboiddecompiler.rosetta.vineflower;

import com.github.zomboiddecompiler.rosetta.RosettaParser;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.util.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class RosettaPlugin implements Plugin {
    private FileSystem fileSystem = null;

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
        String rosettaDir = (String)DecompilerContext.getProperty(RosettaPluginOptions.ROSETTA_DIRECTORY);
        if (rosettaDir == null) {
            return null;
        }

        Path rosettaPath = resolveRosettaPath(rosettaDir);
        if (rosettaPath == null) {
            DecompilerContext.getLogger().writeMessage("Rosetta path did not resolve to a valid directory. Ignoring.", IFernflowerLogger.Severity.WARN);
            return null;
        }

        RosettaParser parser = new RosettaParser();
        parser.parseDirectory(rosettaPath);

        // HACK to send the rosetta data to the javadoc provider
        // this seems kind of dumb but i couldn't find a better way to do it
        IFabricJavadocProvider javadocProvider = (IFabricJavadocProvider)DecompilerContext.getProperty(IFabricJavadocProvider.PROPERTY_NAME);
        if (javadocProvider instanceof RosettaJavadocProvider rosettaProvider) {
            rosettaProvider.addClassesFromNamespace(parser.namespaces);
        }

        cleanup();

        return new RosettaNamingFactory(parser.namespaces);
    }

    // hack to prevent the file system from being closed when the rosetta parser tries to read it
    // TODO i hate this!!! resource cleanup should not be manual
    // how can this be restructured for try-with-resources to make sense?
    private void cleanup() {
        if (fileSystem != null) {
            try {
                fileSystem.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public @Nullable PluginOptions getPluginOptions() {
        return () -> Pair.of(RosettaPluginOptions.class, RosettaPluginOptions::addDefaults);
    }

    private @Nullable Path resolveRosettaPath(String directory) {
        if (directory.startsWith("$")) {
            URL rosettaURL = getClass().getClassLoader().getResource(directory.substring(1));
            if (rosettaURL == null) {
                return null;
            }

            try {
                URI uri = rosettaURL.toURI();

                // this seems stupid and wasn't necessary before, but as soon as i moved to gradle, it is?
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");
                fileSystem = FileSystems.newFileSystem(uri, env);

                Path rosettaPath = Paths.get(uri);
                if (!Files.exists(rosettaPath) || !Files.isDirectory(rosettaPath)) {
                    return null;
                }
                return rosettaPath;
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Path rosettaPath = Paths.get(directory);
            if (!Files.exists(rosettaPath) || !Files.isDirectory(rosettaPath)) {
                return null;
            }
            return rosettaPath;
        }
    }
}
