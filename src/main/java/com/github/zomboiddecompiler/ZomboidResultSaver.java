package com.github.zomboiddecompiler;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Mostly a copy of DirectoryResultSaver, which is unfortunately final
public final class ZomboidResultSaver implements IResultSaver {
    private final Path root;

    /// If true, when saving a decompiled source file, the corresponding .class file will be found and its line numbers will be remapped to the source file.
    private boolean remapLineNumbers = false;
    /// File system for accessing the game's jar archive.
    private final FileSystem jarFilesystem;

    public ZomboidResultSaver(Path root, FileSystem jarFilesystem) {
        this.root = root;
        this.jarFilesystem = jarFilesystem;
    }

    public void setRemapLineNumbers(boolean remapLineNumbers) {
        this.remapLineNumbers = remapLineNumbers;
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
        Path entryPath = this.root.resolve(entryName);

        try (BufferedWriter writer = Files.newBufferedWriter(entryPath)) {
            if (content != null) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save class", e);
        }
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
        Path entryPath = this.root.resolve(entryName);
        try {
            Files.createDirectories(entryPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save directory", e);
        }
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {

    }

    @Override
    public void saveFolder(String path) {
        Path entryPath = this.root.resolve(path);
        try {
            Files.createDirectories(entryPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save directory", e);
        }
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
        try {
            InterpreterUtil.copyFile(new File(source), this.root.resolve(entryName).toFile());
        } catch (IOException ex) {
            DecompilerContext.getLogger().writeMessage("Cannot copy " + source + " to " + entryName, ex);
        }
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        Path entryPath = this.root.resolve(path).resolve(entryName);

        try (BufferedWriter writer = Files.newBufferedWriter(entryPath)) {
            if (content != null) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save class", e);
        }

        // i don't really like doing this here, but i can't find another place to extract the line mappings
        // mapping length is checked so that files with no code don't get remapped, as this fails
        if (remapLineNumbers && mapping.length > 0) {
            Map<Integer, Integer> mappingMap = new LinkedHashMap<>();
            for (int i = 0; i < mapping.length; i += 2) {
                mappingMap.put(mapping[i], mapping[i + 1]);
            }

            // this actually points to a .java file, which doesn't exist!!
            Path classFile = this.jarFilesystem.getPath(entryName);
            Path classDirectory = classFile.getParent();
            String classPath = entryName.replace(this.jarFilesystem.getSeparator(), ".");
            classPath = classPath.substring(0, classPath.length() - ".java".length());
            String className = classFile.getFileName().toString();
            className = className.substring(0, className.length() - ".java".length());

            ZomboidDecompiler.log.log("Remapping line numbers in " + classPath);

            List<Path> files;
            try (Stream<Path> fileStream = Files.list(classDirectory)) {
                String finalClassName = className;
                files = fileStream.filter(
                        (file) -> {
                            String fileName = file.getFileName().toString();
                            return fileName.equals(finalClassName + ".class")
                                    || (fileName.startsWith(finalClassName + "$") && fileName.endsWith(".class"));
                        }).toList();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (Path file: files) {
                LineRemapper.remapClass(file, file, mappingMap);
            }
        }
    }

    @Override
    public void copyEntry(String source, String path, String archiveName, String entryName) {
        try (ZipFile srcArchive = new ZipFile(new File(source))) {
            ZipEntry entry = srcArchive.getEntry(entryName);
            if (entry != null) {
                try (InputStream in = srcArchive.getInputStream(entry)) {
                    InterpreterUtil.copyStream(in, new FileOutputStream(this.root.resolve(entryName).toFile()));
                }
            }
        } catch (IOException ex) {
            String message = "Cannot copy entry " + entryName + " from " + source;
            DecompilerContext.getLogger().writeMessage(message, ex);
        }
    }

    @Override
    public void closeArchive(String path, String archiveName) {

    }
}