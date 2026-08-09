package com.github.zomboiddecompiler.commands;

import com.github.zomboiddecompiler.OutLogger;
import com.github.zomboiddecompiler.steam.VDFBlock;
import com.github.zomboiddecompiler.ZomboidDecompiler;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "decompile", mixinStandardHelpOptions = true,
        version = ZomboidDecompiler.VERSION_MAJOR + "." + ZomboidDecompiler.VERSION_MINOR + "." + ZomboidDecompiler.VERSION_PATCH,
        description = "Decompiles Project Zomboid with automatic dependency detection and specific variable renaming.")
public class Decompile implements Callable<Integer> {
    @Option(names = {"--add-docstrings"}, description = "If true, adds docstrings to the output based on the Rosetta data.")
    private boolean addDocstrings = true;

    @Option(names = {"--log-path"}, description = "Path to a folder to write log files in.")
    private File logPath = new File("logs");

    @Option(names = {"--jar-game"}, description = "If true, copies the game's classes into a jar file." +
            "Useful for recompiling single files.")
    private boolean jarGame = false;

    @Option(names = {"--remap-line-numbers"}, description = "If true, the line numbers in the original source files will " +
            "be remapped to the decompiled source line numbers. This will allow you to attach the game to a debugger. " +
            "Please keep in mind that this will modify your base game files. This should not affect any actual code " +
            "execution, and in current versions of the game should not trigger anti-cheat.")
    private boolean remapLineNumbers = false;

    @Parameters(index = "0", arity = "0..1")
    private Path inputPath = null;
    @Parameters(index = "1", arity = "0..1")
    private Path outputPath = Paths.get("output");

    @Option(
            names = {"--class-pattern"},
            description = "Patterns to select classes to decompile. "
            + "Patterns take the format of 'package.subpackage.classname'. "
            + "Multiple patterns are separated with commas. "
            + "A pattern ending in * will decompile all classes in the specific package and its subpackages. "
            + "A pattern prefixed with - is a negative pattern: "
            + "any class that matches a negative pattern is not decompiled even if it matches a positive pattern."
    )
    private String classPatterns = "zombie.*";

    @Option(names = "-vf", arity = "2", description = "Argument name and value to pass through to Vineflower. " +
            "Can be specified multiple times to pass multiple arguments. " +
            "Leading dashes should not be included in the argument name.")
    private String[] vineflowerArgs = new String[0];

    private @Nullable Path findSteamPathLinux() {
        Path steamPath = Paths.get(System.getProperty("user.home")).resolve(".steam/steam");

        if (!Files.exists(steamPath) || !Files.isDirectory(steamPath)) {
            return null;
        }

        return steamPath;
    }

    private @Nullable Path findSteamPathWindows() {
        String steamDirectory;

        String registryKey = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Valve\\Steam";
        if (!System.getProperty("os.arch").contains("64")) {
            // 32 bit windows uses a different key, hopefully this is a reliable way of checking
            registryKey = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Valve\\Steam";
        }

        try {
            Process process = Runtime.getRuntime().exec("reg query \"" + registryKey + "\" /v InstallPath");

            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            process.waitFor();
            StringBuilder result = new StringBuilder();
            while(reader.ready()) {
                result.append((char)reader.read());
            }

            // FIXME: this would fail if the user (for some reason) had 4 spaces in their install path
            steamDirectory = result.substring(result.lastIndexOf("    ") + 1);
            steamDirectory = steamDirectory.trim();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }

        Path steamPath = Paths.get(steamDirectory);
        if (!Files.exists(steamPath) || !Files.isDirectory(steamPath)) {
            return null;
        }

        return steamPath;
    }

    /**
     * Finds the path that Steam is installed to on the system.
     * @return The path that Steam is installed to.
     * <br> Null may be returned if Steam cannot be found.
     * <br> It is guaranteed that the path exists and is a directory.
     * It is not guaranteed that it actually contains a valid Steam installation.
     */
    private @Nullable Path findSteamPath() {
        String osName = System.getProperty("os.name");

        if (osName.equals("Linux")) {
            return findSteamPathLinux();
        } else if (osName.startsWith("Windows")) {
            return findSteamPathWindows();
        }

        return null;
    }

    /**
     * Finds and returns a list of steam library paths detected on the system.
     * @return List of steam library paths.
     * <br> The paths are guaranteed to exist and be directories, but may not be properly structured as a steam library.
     * <br> It is not guaranteed to contain every steam library on the system, or even any libraries at all.
     */
    private List<Path> findSteamLibraries() {
        Path steamPath = findSteamPath();

        if (steamPath == null) {
            return new ArrayList<>();
        }

        Path libraryfolders = steamPath.resolve("steamapps/libraryfolders.vdf");
        if (!Files.exists(libraryfolders) || !Files.isRegularFile(libraryfolders)) {
            return new ArrayList<>();
        }

        StringBuilder librariesVDF = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(libraryfolders))) {
            while(reader.ready()) {
                librariesVDF.append((char)reader.read());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        VDFBlock librariesBlock = VDFBlock.parse(librariesVDF.toString()).getBlock("libraryfolders");

        List<Path> libraries = new ArrayList<>();
        int index = 0;
        while (librariesBlock.hasBlock(String.valueOf(index))) {
            VDFBlock library = librariesBlock.getBlock(String.valueOf(index));
            Path libraryPath = Paths.get(library.getValue("path"));
            if (Files.exists(libraryPath) && Files.isDirectory(libraryPath)) {
                libraries.add(libraryPath);
            }
            index++;
        }

        return libraries;
    }

    /**
     * Attempts to find and return the path that Project Zomboid is installed to on the system.
     * @return Path of the root directory of a Project Zomboid installation.
     * <br> Null may be returned if Project Zomboid cannot be found.
     * <br> It is guaranteed that the directory exists and is a directory.
     * It is not guaranteed that a valid Project Zomboid installation is actually stored there.
     */
    private @Nullable Path findZomboidPath() {
        for (Path library : findSteamLibraries()) {
            Path zomboidPath = library.resolve("steamapps/common/ProjectZomboid");
            if (Files.exists(zomboidPath) && Files.isDirectory(zomboidPath)) {
                return zomboidPath;
            }
        }

        return null;
    }

    @Override
    public Integer call() {
        if (inputPath == null) {
            inputPath = findZomboidPath();
            if (inputPath == null) {
                System.out.println("Cannot detect game directory, aborting. When auto detection fails, you can pass the game directory as an argument on the command line.");
                return 1;
            } else {
                System.out.println("Found game installation at " + inputPath);
            }
        }

        if (!Files.exists(inputPath)) {
            System.out.println("Game directory does not exist.");
            return 1;
        }

        List<ZomboidDecompiler.VineflowerArgument> argsList = new ArrayList<>();
        for (int i = 0; i < vineflowerArgs.length; i += 2) {
            argsList.add(new ZomboidDecompiler.VineflowerArgument(vineflowerArgs[i], vineflowerArgs[i + 1]));
        }

        ZomboidDecompiler.initLoggers(logPath);
        ZomboidDecompiler.log = new OutLogger(ZomboidDecompiler.log);

        ZomboidDecompiler decompiler = new ZomboidDecompiler();
        decompiler.setJarGame(jarGame);
        decompiler.setAddDocstrings(addDocstrings);
        decompiler.setRemapLineNumbers(remapLineNumbers);
        decompiler.setClassPatterns(classPatterns);

        decompiler.decompile(inputPath, outputPath, argsList);

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Decompile()).execute(args);
        System.exit(exitCode);
    }
}
