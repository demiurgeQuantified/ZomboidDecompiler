package com.github.zomboiddecompiler.rosetta;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class RosettaPlugin implements Plugin {
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

        Path rosettaPath;

        if (rosettaDir.startsWith("$")) {
            URL rosettaURL = getClass().getResource(rosettaDir.substring(1));
            if (rosettaURL == null) {
                return null;
            }

            try {
                URI uri = rosettaURL.toURI();

                // this seems stupid and wasn't necessary before, but as soon as i moved to gradle, it is?
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");
                try(FileSystem ignored = FileSystems.newFileSystem(uri, env)) {
                    rosettaPath = Paths.get(uri);
                }
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            rosettaPath = Paths.get(rosettaDir);
        }

        Path jsonFolder = rosettaPath.resolve("json");

        assert Files.exists(jsonFolder);

        RosettaParser parser = new RosettaParser();

        try (Stream<Path> files = Files.walk(jsonFolder)) {
            for (Path file : files.toList()) {
                if (!file.getFileName().toString().toLowerCase().endsWith(".json")) continue;
                try {
                    parser.parseJson(Files.newInputStream(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new RosettaNamingFactory(parser.namespaces);
    }

    @Override
    public @Nullable PluginOptions getPluginOptions() {
        return () -> Pair.of(RosettaPluginOptions.class, RosettaPluginOptions::addDefaults);
    }
}
