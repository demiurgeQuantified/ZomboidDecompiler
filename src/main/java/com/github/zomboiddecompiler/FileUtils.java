package com.github.zomboiddecompiler;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class FileUtils {
    /**
     * Zips the contents of a directory.
     * @param in Path of the directory to zip.
     * @param out Path to write the zip to. Extension should be included.
     */
    public static boolean zipDirectory(Path in, Path out) {
        try {
            try (FileSystem zipFileSystem = FileSystems.newFileSystem(out, ENV)) {
                copyFileOrDirectory(in, zipFileSystem.getPath(
                        in.getFileName().toString()
                ));
            }
        } catch (IOException e) {
            ZomboidDecompiler.log.log(e);
            return false;
        }
        return true;
    }

    public static void copyFileOrDirectory(Path source, Path destination) throws IOException {
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
                                ZomboidDecompiler.log.log(e);
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
    public static void clearDirectory(Path directory) {
        assert Files.isDirectory(directory);

        try (Stream<Path> files = Files.list(directory)) {
            files.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        clearDirectory(path);
                    }
                    Files.delete(path);
                } catch (IOException e) {
                    ZomboidDecompiler.log.log(e);
                }
            });
        } catch (IOException e) {
            ZomboidDecompiler.log.log(e);
        }
    }

    private static final Map<String, String> ENV = Map.of(
            "create", "true"
    );
}
