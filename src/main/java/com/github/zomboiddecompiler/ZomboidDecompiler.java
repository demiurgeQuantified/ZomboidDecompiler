package com.github.zomboiddecompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import com.github.zomboiddecompiler.rosetta.RosettaJavadocProvider;
import com.github.zomboiddecompiler.rosetta.RosettaNamespace;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;

public class ZomboidDecompiler {
    /// Main program log.
    public static ILogger log = new DummyLogger();
    /// Vineflower log.
    private static ILogger vineflowerLog = new DummyLogger();

    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 1;
    public static final int VERSION_PATCH = 6;

    private boolean copyDependencies = false;
    private boolean jarGame = false;
    private boolean addDocstrings = true;

    public void setCopyDependencies(boolean copyDependencies) {
        this.copyDependencies = copyDependencies;
    }

    public void setJarGame(boolean jarGame) {
        this.jarGame = jarGame;
    }

    public void setAddDocstrings(boolean addDocstrings) {
        this.addDocstrings = addDocstrings;
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
            clearDirectory(outDirectory);
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
                    copyFileOrDirectory(dependency, zipFileSystem.getPath(dependencyName));
                } else {
                    copyFileOrDirectory(dependency, outDirectory.resolve(dependencyName));
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
    public void decompile(Path gamePath, Path outputPath, @Nullable String rosettaPath,
                          @Nullable List<VineflowerArgument> vineflowerArgs) {
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
            clearDirectory(outputPath);
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
            if (zipDirectory(zombieDirectory, outputPath.resolve("zombie.jar"))) {
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

        Decompiler.Builder builder = Decompiler.builder()
                .inputs(new ZomboidContextSource(gamePath.toFile()))
                .output(new DirectoryResultSaver(outputPath.toFile()))
                .option("ascii-strings", true)
                .option("banner",
                        String.format("// Decompiled on %tc with Zomboid Decompiler v%d.%d.%d using Vineflower.\n",
                                      System.currentTimeMillis(), VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH))
                .option("error-message", "Please report this to the Zomboid Decompiler issue tracker at https://github.com/demiurgeQuantified/ZomboidDecompiler/issues with the file name and game version.")
                //.option("log-level", "warn")
                .libraries(dependencyFiles)
                .logger(vineflowerLog instanceof StreamLogger fileLogger
                        ? new PrintStreamLogger(fileLogger.getStream())
                        : null)
                .option("rosetta-directory", rosettaPath)
                .option("indent-string", "    ");

        if (addDocstrings) {
            builder.option(IFabricJavadocProvider.PROPERTY_NAME, new RosettaJavadocProvider());
        }

        if (vineflowerArgs != null) {
            for (VineflowerArgument argument: vineflowerArgs) {
                builder.option(argument.parameter, argument.value);
            }
        }

        log.log("Beginning decompilation...");

        Decompiler decompiler = builder.build();
        decompiler.decompile();

        log.log("Decompilation complete.");
    }

    private static final Map<String, String> ENV = Map.of(
            "create", "true"
    );

    /**
     * Zips the contents of a directory.
     * @param in Path of the directory to zip.
     * @param out Path to write the zip to. Extension should be included.
     */
    static boolean zipDirectory(Path in, Path out) {
        try {
            try (FileSystem zipFileSystem = FileSystems.newFileSystem(out, ENV)) {
                copyFileOrDirectory(in, zipFileSystem.getPath(
                        in.getFileName().toString()
                ));
            }
        } catch (IOException e) {
            log.log(e);
            return false;
        }
        return true;
    }

    static void copyFileOrDirectory(Path source, Path destination) throws IOException {
        assert Files.exists(source);

        if (Files.isDirectory(source)) {
            Files.createDirectory(destination);
            try (Stream<Path> files = Files.list(source)) {
                files.forEach(
                        path -> {
                            try {
                                copyFileOrDirectory(path, destination.resolve(
                                        path.getFileName().toString()));
                            } catch (IOException e) {
                                log.log(e);
                            }
                        });
            }
        } else {
            Files.copy(source, destination);
        }
    }

    /**
     * Recursively deletes every file in a directory.
     * @param directory The directory to clear.
     */
    private static void clearDirectory(Path directory) {
        assert Files.isDirectory(directory);

        try (Stream<Path> files = Files.list(directory)) {
            files.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        clearDirectory(path);
                    }
                    Files.delete(path);
                } catch (IOException e) {
                    log.log(e);
                }
            });
        } catch (IOException e) {
            log.log(e);
        }
    }

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
}