package com.github.zomboiddecompiler.commands;

import com.github.zomboiddecompiler.ZomboidDecompiler;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "decompile", mixinStandardHelpOptions = true,
        version = ZomboidDecompiler.VERSION_MAJOR + "." + ZomboidDecompiler.VERSION_MINOR + "." + ZomboidDecompiler.VERSION_PATCH,
        description = "Decompiles Project Zomboid with automatic dependency detection and specific variable renaming.")
public class Decompile implements Callable<Integer> {
    // a list of some default directories to look for the game in
    private static final List<String> possibleGameDirectories = List.of(
            "C:\\Program Files (x86)\\Steam\\steamapps\\common\\ProjectZomboid",
            "D:\\Program Files (x86)\\Steam\\steamapps\\common\\ProjectZomboid");

    @Option(names = {"--rosetta-path"}, description = "Root path of a rosetta installation to use for variable names.")
    private String rosettaPath = "rosetta";

    @Option(names = {"--log-path"}, description = "Path to a folder to write log files in.")
    private File logPath = new File("logs");

    @Option(names = {"--copy-dependencies"}, description = "Whether to copy all game dependencies to the output folder.")
    private boolean copyDependencies = true;

    @Parameters(index = "0", arity = "0..1")
    private File inputPath = null;
    @Parameters(index = "1", arity = "0..1")
    private File outputPath = new File("output");

    @Override
    public Integer call() {
        if (inputPath == null) {
            for (String dir : possibleGameDirectories) {
                inputPath = new File(dir);
                if (inputPath.exists()) {
                    System.out.println("Detected game install at " + dir);
                    break;
                }
                // TODO: check if the game path has a ProjectZomboid64.exe
            }
            if (inputPath == null || !inputPath.exists()) {
                System.out.println("Cannot detect game directory, aborting.");
                return 1;
            }
        }

        ZomboidDecompiler decompiler = new ZomboidDecompiler(logPath);
        decompiler.setCopyDependencies(copyDependencies);
        decompiler.decompile(inputPath, outputPath, rosettaPath);

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Decompile()).execute(args);
        System.exit(exitCode);
    }
}
