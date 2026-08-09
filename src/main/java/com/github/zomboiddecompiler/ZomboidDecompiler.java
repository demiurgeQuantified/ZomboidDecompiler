package com.github.zomboiddecompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

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
    public static final int VERSION_MINOR = 3;
    public static final int VERSION_PATCH = 2;

    /// Whether to create a jar file containing all detected source files.
    private boolean jarGame = false;
    /// Whether to add docstrings to objects that have appropriate Rosetta data.
    private boolean addDocstrings = true;
    /// Whether to change the line mappings in the original source files to align with the decompiled source.
    private boolean remapLineNumbers = false;

    private String classPatterns = "";

    public void setClassPatterns(String classPatterns) {
        this.classPatterns = classPatterns;
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

        Path gameJar = gamePath.resolve("projectzomboid.jar");
        if (!Files.isRegularFile(gameJar)) {
            log.log("projectzomboid.jar not found, aborting");
            return;
        }

        if (jarGame) {
            try {
                FileUtils.copyFileOrDirectory(gameJar, outputPath.resolve("projectzomboid.jar"));
            } catch (IOException e) {
                log.log(e);
            }
        }

        FileSystem gameJarFilesystem;
        try {
            gameJarFilesystem = FileSystems.newFileSystem(gameJar);
        } catch (IOException e) {
            log.log(e);
            log.log("Could not open jar. Aborting decompilation.");
            return;
        }

        ZomboidResultSaver resultSaver = new ZomboidResultSaver(outputPath.resolve("source"), gameJarFilesystem);
        ZomboidContextSource gameSource = new ZomboidContextSource(gameJar, gameJarFilesystem, this.classPatterns, false);
        ZomboidContextSource dependencySource = new ZomboidContextSource(gameJar, gameJarFilesystem, this.classPatterns, true);

        Decompiler.Builder builder = Decompiler.builder()
                .inputs(gameSource)
                .output(resultSaver)
                .option(IFernflowerPreferences.ASCII_STRING_CHARACTERS, true)
                .option(IFernflowerPreferences.BANNER,
                        String.format("// Decompiled with Zomboid Decompiler v%d.%d.%d using Vineflower.\n",
                                      VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH))
                .option(IFernflowerPreferences.ERROR_MESSAGE, "Please report this to the Zomboid Decompiler issue tracker at https://github.com/demiurgeQuantified/ZomboidDecompiler/issues with the file name and game version.")
                //.option("log-level", "warn")
                .libraries(dependencySource)
                .logger(vineflowerLog instanceof StreamLogger fileLogger
                        ? new PrintStreamLogger(fileLogger.getStream())
                        : null)
                .option(
                        IFernflowerPreferences.INCLUDE_JAVA_RUNTIME,
                        gamePath.resolve("jre64").toAbsolutePath().toString()
                )
                .option(IFernflowerPreferences.INDENT_STRING, "    ")
                .option(RosettaPlugin.NAMESPACE_PROPERTY_NAME, getResourceNamespaces())
                .option(RosettaPlugin.TYPE_NAMER_PROPERTY_NAME, new ZomboidTypeNameProvider());

        if (addDocstrings) {
            builder.option(IFabricJavadocProvider.PROPERTY_NAME, new RosettaJavadocProvider());
        }

        if (remapLineNumbers) {
            try {
                // back up the original jar before remapping line numbers inside it
                Path backupJar = gameJar.resolveSibling(gameJar.getFileName() + ".backup");
                Files.copy(gameJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
                // tells the decompiler to map bytecode to decompiled source lines
                builder.option(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, true);
                // use that data to remap the line numbers in the class files
                resultSaver.setRemapLineNumbers(true);
            }
            catch (IOException e) {
                log.log(e);
                log.log("Could not create jar backup. Line remapping is cancelled.");
            }
        }

        for (VineflowerArgument argument: vineflowerArgs) {
            builder.option(argument.parameter, argument.value);
        }

        log.log("Beginning decompilation...");

        Decompiler decompiler = builder.build();
        decompiler.decompile();

        log.log("Decompilation complete.");

        try {
            gameJarFilesystem.close();
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