package com.github.zomboiddecompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import com.github.zomboiddecompiler.rosetta.RosettaPackage;
import com.github.zomboiddecompiler.rosetta.RosettaParser;
import com.github.zomboiddecompiler.rosetta.vineflower.RosettaJavadocProvider;
import com.github.zomboiddecompiler.rosetta.vineflower.RosettaPlugin;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

public class ZomboidDecompiler {
    /// Main program log.
    public static ILogger log = new DummyLogger();
    /// Vineflower log.
    private static ILogger vineflowerLog = new DummyLogger();

    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 2;
    public static final int VERSION_PATCH = 3;

    /// Whether to make a copy of all detected dependencies to output/dependencies
    private boolean copyDependencies = false;
    /// Whether to create a jar file containing all detected source files.
    private boolean jarGame = false;
    /// Whether to add docstrings to objects that have appropriate Rosetta data.
    private boolean addDocstrings = true;
    /// Whether to change the line mappings in the original source files to align with the decompiled source.
    private boolean remapLineNumbers = false;

    public void setCopyDependencies(boolean copyDependencies) {
        this.copyDependencies = copyDependencies;
    }

    public void setJarGame(boolean jarGame) {
        this.jarGame = jarGame;
    }

    public void setAddDocstrings(boolean addDocstrings) {
        this.addDocstrings = addDocstrings;
    }

    public void setRemapLineNumbers(boolean remapLineNumbers) {
        this.remapLineNumbers = remapLineNumbers;
    }

    /**
     * Recursively scans a directory for any .class files.
     * @param directory The directory to scan.
     * @return Whether the directory contains any class files.
     */
    static boolean containsClassFiles(Path directory) {
        assert Files.isDirectory(directory);

        try(Stream<Path> files = Files.list(directory)) {
            return files.anyMatch(path ->
                Files.isDirectory(path) ? containsClassFiles(path) : path.getFileName().toString().endsWith(".class"));
        } catch (IOException e) {
            log.log(e);
            return false;
        }
    }

    private boolean copyDependencies(List<Path> dependencies, Path outDirectory) {
        if (Files.exists(outDirectory)) {
            FileUtils.clearDirectory(outDirectory);
        } else {
            try {
                Files.createDirectory(outDirectory);
            } catch (IOException e) {
                log.log(e);
                return false;
            }
        }

        try {
            for (Path dependency : dependencies) {
                String dependencyName = dependency.getFileName().toString();
                FileUtils.copyFileOrDirectory(dependency, outDirectory.resolve(dependencyName));
            }
        } catch (IOException e) {
            log.log(e);
        }
        return true;
    }

    Set<String> BAD_DEPENDENCY_NAMES = Set.of("zombie", "media", "steamapps", "mods", "Workshop");

    private List<Path> findDependencies(Path dir) {
        List<Path> dependencies = new ArrayList<>();
        try(Stream<Path> files = Files.list(dir)) {
            files.filter(
                        path -> !BAD_DEPENDENCY_NAMES.contains(
                                path.getFileName().toString()))
                .forEach(
                        path -> {
                            if (path.getFileName().toString().endsWith(".jar"))
                            {
                                log.log("Discovered dependency: " + path);
                                dependencies.add(path);
                            }
                        }
                );
        } catch (IOException e) {
            log.log(e);
        }
        return dependencies;
    }

    private final Set<String> BAD_CODE_DIRECTORY_NAMES = Set.of("media", "steamapps", "mods", "Workshop");

    /**
     * Decompiles the game.
     * @param gamePath Root directory of the game.
     * @param outputPath Path to write the output to.
     */
    public void decompile(Path gamePath, Path outputPath, List<VineflowerArgument> vineflowerArgs) {
        assert Files.exists(gamePath) && Files.isDirectory(gamePath);

        if (Files.exists(gamePath.resolve("projectzomboid.sh"))) {
            log.log("gamePath seems to be a Linux installation (projectzomboid.sh detected)");
            gamePath = gamePath.resolve("projectzomboid");
            if (!Files.exists(gamePath) || !Files.isDirectory(gamePath)) {
                log.log("Not a valid Linux installation, aborting.");
                return;
            }
        }

        Path zombieDirectory = gamePath.resolve("zombie");
        if (!Files.exists(zombieDirectory) || !Files.isDirectory(zombieDirectory)) {
            log.log("Zombie directory does not exist. Aborting decompilation.");
            return;
        }

        if (Files.exists(outputPath)) {
            FileUtils.clearDirectory(outputPath);
        } else {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                log.log(e);
                log.log("Could not access output directory. Aborting decompilation.");
                return;
            }
        }

        // FIXME: this takes wayyyyy too long to decompile
        //  we probably need to unzip (if necessary?) and then only pass certain directories as sources
        //  see #24
        Path gameJar = gamePath.resolve("projectzomboid.jar");

        assert Files.isRegularFile(gameJar);

        if (jarGame) {
            try {
                FileUtils.copyFileOrDirectory(gameJar, outputPath.resolve("projectzomboid.jar"));
            } catch (IOException e) {
                log.log(e);
            }
        }

        ZomboidResultSaver resultSaver = new ZomboidResultSaver(outputPath.resolve("source"), gamePath);

        ZomboidContextSource gameSource;
        ZomboidContextSource dependencySource;
        try {
            Set<String> WANTED_PACKAGES = Set.of("zombie", "generation");
            gameSource = new ZomboidContextSource(gameJar, WANTED_PACKAGES, false);
            dependencySource = new ZomboidContextSource(gameJar, WANTED_PACKAGES, true);
        } catch (IOException e) {
            log.log(e);
            log.log("Aborting decompilation due to exception while opening context source");
            return;
        }

        Decompiler.Builder builder = Decompiler.builder()
                .inputs(gameSource)
                .output(resultSaver)
                .option(IFernflowerPreferences.ASCII_STRING_CHARACTERS, true)
                .option(IFernflowerPreferences.BANNER,
                        String.format("// Decompiled on %tc with Zomboid Decompiler v%d.%d.%d using Vineflower.\n",
                                      System.currentTimeMillis(), VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH))
                .option(IFernflowerPreferences.ERROR_MESSAGE, "Please report this to the Zomboid Decompiler issue tracker at https://github.com/demiurgeQuantified/ZomboidDecompiler/issues with the file name and game version.")
                //.option("log-level", "warn")
                .libraries(dependencySource)
                .logger(vineflowerLog instanceof StreamLogger fileLogger
                        ? new PrintStreamLogger(fileLogger.getStream())
                        : null)
                .option(IFernflowerPreferences.INCLUDE_JAVA_RUNTIME, "current")
                .option(IFernflowerPreferences.INDENT_STRING, "    ")
                .option(RosettaPlugin.NAMESPACE_PROPERTY_NAME, getResourceNamespaces())
                .option(RosettaPlugin.TYPE_NAMER_PROPERTY_NAME, new ZomboidTypeNameProvider());

        if (addDocstrings) {
            builder.option(IFabricJavadocProvider.PROPERTY_NAME, new RosettaJavadocProvider());
        }

        // FIXME: this can't rewrite the jar used in 42.13+
//        if (remapLineNumbers) {
//            // tells the decompiler to map bytecode to decompiled source lines
//            builder.option(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, true);
//            // use that data to remap the line numbers in the class files
//            resultSaver.setRemapLineNumbers(true);
//        }

        for (VineflowerArgument argument: vineflowerArgs) {
            builder.option(argument.parameter, argument.value);
        }

        log.log("Beginning decompilation...");

        Decompiler decompiler = builder.build();
        decompiler.decompile();

        log.log("Decompilation complete.");

        try {
            gameSource.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<String, String> ENV = Map.of(
            "create", "true"
    );

    public static void initLoggers(File logDirectory) {
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            System.out.println("Failed to create logs directory (probably a permissions issue). Logging to console instead.");
            log = new StreamLogger(System.out);
            vineflowerLog = new StreamLogger(System.out);
        }

        try {
            log = new StreamLogger(new File(logDirectory, "main.log"));
        } catch (FileNotFoundException e) {
            log.log("Failed to create main log (probably a permissions issue). Logging to console instead.");
            log = new StreamLogger(System.out);
            log.log(e);
        }

        try {
            vineflowerLog = new StreamLogger(new File(logDirectory, "vineflower.log"));
        } catch (FileNotFoundException e) {
            log.log("Failed to create vineflower log (probably a permissions issue). Logging to console instead.");
            log.log(e);
            vineflowerLog = new StreamLogger(System.out);
        }
    }

    public record VineflowerArgument(String parameter, Object value) {}

    private static List<RosettaPackage> getResourceNamespaces() {
        URL rosettaURL = ZomboidDecompiler.class.getClassLoader().getResource("rosetta");
        if (rosettaURL != null) {
            try {
                URI uri = rosettaURL.toURI();

                // this seems stupid and wasn't necessary before, but as soon as i moved to gradle, it is?
                // and it doesn't work when you aren't running with gradle!!
                try (FileSystem ignored = FileSystems.newFileSystem(uri, ENV)) {
                    Path rosettaPath = Paths.get(uri);
                    if (!Files.exists(rosettaPath) || !Files.isDirectory(rosettaPath)) {
                        return new ArrayList<>();
                    }

                    RosettaParser parser = new RosettaParser();
                    parser.parseDirectory(rosettaPath);

                    return parser.packages;
                }
            } catch (URISyntaxException | IOException e) {
                log.log(e);
            }
        }
        return new ArrayList<>();
    }
}