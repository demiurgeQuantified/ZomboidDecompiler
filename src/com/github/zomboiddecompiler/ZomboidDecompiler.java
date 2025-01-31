package com.github.zomboiddecompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;

public class ZomboidDecompiler {
    /// Main program log.
    private ILogger log;
    /// Vineflower log.
    private ILogger vineflowerLog;

    public static final int VERSION_MAJOR = 0;
    public static final int VERSION_MINOR = 1;
    public static final int VERSION_PATCH = 1;

    private boolean copyDependencies = false;

    public void setCopyDependencies(boolean copyDependencies) {
        this.copyDependencies = copyDependencies;
    }

    /**
     * Recursively scans a directory for any .class files.
     * @param directory The directory to scan.
     * @return Whether the directory contains any class files.
     */
    static boolean containsClassFiles(@NotNull File directory) {
        assert directory.isDirectory();

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                if (containsClassFiles(file)) {
                    return true;
                }
            } else if (file.getName().endsWith(".class")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Decompiles the game.
     * @param gamePath Root directory of the game.
     * @param outputPath Path to write the output to.
     */
    public void decompile(@NotNull File gamePath, @NotNull File outputPath, @Nullable String rosettaPath) {
        assert gamePath.exists();

        File zombieDirectory = new File(gamePath, "zombie");
        assert zombieDirectory.exists();

        ArrayList<File> dependencies = new ArrayList<>();
        for (File file : Objects.requireNonNull(gamePath.listFiles(DependencyFilter.filter))) {
            if (file.isFile() || (file.isDirectory() && containsClassFiles(file))) {
                log.log("Discovered dependency: " + file.getName());
                dependencies.add(file);
            }
        }

        if (copyDependencies) {
            File dependenciesPath = new File(outputPath, "dependencies");
            if (dependenciesPath.exists()) {
                try {
                    clearDirectory(dependenciesPath);
                } catch (IOException e) {
                    log.log(Arrays.toString(e.getStackTrace()));
                }
            } else {
                dependenciesPath.mkdirs();
            }

            for (File dependency : dependencies) {
                try {
                    // FIXME: for directories, this copies the entire directory
                    // it should only copy .class files
                    copyFileOrDirectory(dependency, new File(dependenciesPath, dependency.getName()));
                } catch (IOException e) {
                    log.log(Arrays.toString(e.getStackTrace()));
                }
            }
        }

        outputPath = new File(outputPath, "zombie");
        if (outputPath.exists()) {
            try {
                clearDirectory(outputPath);
            } catch (IOException e) {
                log.log(Arrays.toString(e.getStackTrace()));
            }
        } else {
            outputPath.mkdirs();
        }

        Decompiler decompiler = Decompiler.builder()
                .inputs(zombieDirectory)
                .output(new DirectoryResultSaver(outputPath))
                .option("ascii-strings", true)
                .option("banner",
                        String.format("// Decompiled on %tc with Zomboid Decompiler v%d.%d.%d using Vineflower.\n",
                                      System.currentTimeMillis(), VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH))
                .option("error-message", "Please report this to the Zomboid Decompiler issue tracker at https://github.com/demiurgeQuantified/ZomboidDecompiler/issues with the file name and game version.")
                //.option("log-level", "warn")
                .libraries(dependencies.toArray(new File[0]))
                .logger(vineflowerLog instanceof FileLogger fileLogger ? new PrintStreamLogger(fileLogger.getStream()) : null)
                .option("rosetta-directory", rosettaPath)
                .build();

        decompiler.decompile();
    }

    static void copyFileOrDirectory(File source, File destination) throws IOException {
        assert source.exists();

        if (source.isDirectory()) {
            destination.mkdir();
            for (File file : Objects.requireNonNull(source.listFiles())) {
                copyFileOrDirectory(file, new File(destination, file.getName()));
            }
        } else {
            Files.copy(source.toPath(), destination.toPath());
        }
    }

    /**
     * Recursively deletes every file in a directory.
     * @param directory The directory to clear.
     * @throws IOException If access is denied to delete a file.
     */
    static void clearDirectory(File directory) throws IOException {
        assert directory.isDirectory();

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                clearDirectory(file);
            }
            file.delete();
        }
    }

    public ZomboidDecompiler() {
        log = new DummyLogger();
        vineflowerLog = new DummyLogger();
    }

    public ZomboidDecompiler(@NotNull File logDirectory) {
        logDirectory.mkdirs();

        try {
            log = new FileLogger(new File(logDirectory, "main.log"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log = new DummyLogger();
        }

        try {
            vineflowerLog = new FileLogger(new File(logDirectory, "vineflower.log"));
        } catch (FileNotFoundException e) {
            log.log(Arrays.toString(e.getStackTrace()));
            vineflowerLog = new DummyLogger();
        }
    }

    static class DependencyFilter implements FilenameFilter {
        /// List of filenames that should be skipped.
        private static final List<String> badNames = Arrays.asList("zombie", "media", "steamapps", "mods", "Workshop");
        /// Instance of the filter to use (as it has no state)
        public static final DependencyFilter filter = new DependencyFilter();

        @Override
        public boolean accept(File dir, String name) {
            if (name.contains(".") && !name.endsWith(".jar")) {
                return false;
            }
            return !badNames.contains(name);
        }

        private DependencyFilter() {}
    }
}