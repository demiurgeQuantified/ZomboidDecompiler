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
    public static final int VERSION_PATCH = 2;

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

        try (FileSystem zipFileSystem = FileSystems.newFileSystem(
                outDirectory.resolve("loose-dependencies.jar"), ENV))
        {
            for (Path dependency : dependencies) {
                String dependencyName = dependency.getFileName().toString();
                if (Files.isDirectory(dependency)) {
                    FileUtils.copyFileOrDirectory(dependency, zipFileSystem.getPath(dependencyName));
                } else {
                    FileUtils.copyFileOrDirectory(dependency, outDirectory.resolve(dependencyName));
                }
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
                            if (Files.isDirectory(path)
                                    ? containsClassFiles(path)
                                    : path.getFileName().toString().endsWith(".jar"))
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

        List<Path> dependencies = findDependencies(gamePath);

        if (copyDependencies) {
            log.log("Copying dependencies...");
            if (copyDependencies(dependencies, outputPath.resolve("dependencies"))) {
                log.log("Dependencies copied.");
            } else {
                log.log("Dependency copying failed. Previous log messages may give details.");
            }
        }

        if (jarGame) {
            log.log("Jarring game...");
            if (FileUtils.zipDirectory(zombieDirectory, outputPath.resolve("zombie.jar"))) {
                log.log("Game jarred.");
            } else {
                log.log("Game jarring failed. Aborting because this usually means something is wrong with the game installation.");
                return;
            }
        }

        File[] dependencyFiles = new File[dependencies.size()];
        for (int i = 0; i < dependencies.size(); i++) {
            dependencyFiles[i] = dependencies.get(i).toFile();
        }

        ZomboidResultSaver resultSaver = new ZomboidResultSaver(outputPath, gamePath);

        Decompiler.Builder builder = Decompiler.builder()
                .inputs(new ZomboidContextSource(gamePath.toFile()))
                .output(resultSaver)
                .option(IFernflowerPreferences.ASCII_STRING_CHARACTERS, true)
                .option(IFernflowerPreferences.BANNER,
                        String.format("// Decompiled on %tc with Zomboid Decompiler v%d.%d.%d using Vineflower.\n",
                                      System.currentTimeMillis(), VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH))
                .option(IFernflowerPreferences.ERROR_MESSAGE, "Please report this to the Zomboid Decompiler issue tracker at https://github.com/demiurgeQuantified/ZomboidDecompiler/issues with the file name and game version.")
                //.option("log-level", "warn")
                .libraries(dependencyFiles)
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

        if (remapLineNumbers) {
            // tells the decompiler to map bytecode to decompiled source lines
            builder.option(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, true);
            // use that data to remap the line numbers in the class files
            resultSaver.setRemapLineNumbers(true);
        }

        for (VineflowerArgument argument: vineflowerArgs) {
            builder.option(argument.parameter, argument.value);
        }

        log.log("Beginning decompilation...");

        Decompiler decompiler = builder.build();
        decompiler.decompile();

        log.log("Decompilation complete.");
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