package com.github.zomboiddecompiler;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ZomboidContextSource implements IContextSource, AutoCloseable {
    private final Path jar;
    private static final String CLASS_SUFFIX = ".class";

    private final Set<String> packages;
    private final boolean invertPackages;
    private final FileSystem jarFilesystem;

    @Override
    public String getName() {
        return "Project Zomboid (inverted: " + this.invertPackages + "): " + this.jar.toString();
    }

    @Override
    public Entries getEntries() {
        List<Entry> classes = new ArrayList<>();
        List<String> directories = new ArrayList<>();

        for (Path root : jarFilesystem.getRootDirectories()) {
            try(Stream<Path> files = Files.list(root)) {
                for (Path directory : files.toList()) {
                    if (!Files.isDirectory(directory)
                            || invertPackages == packages.contains(directory.getFileName().toString())) {
                        continue;
                    }

                    if (ZomboidDecompiler.containsClassFiles(directory)) {
                        scanDirectory(directory, classes, directories);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new Entries(classes, directories, new ArrayList<>(), new ArrayList<>());
    }

    void scanDirectory(final Path current, final List<Entry> classes, final List<String> directories) {
        String relativePath = current.toString().replace(File.separatorChar, '/');
        // need to remove leading "/" or it writes to root of disk lol
        relativePath = relativePath.substring(1);
        if (Files.isDirectory(current)) {
            directories.add(relativePath);
            try (Stream<Path> files = Files.list(current)) {
                for (Path file : files.toList()) {
                    if (Files.isDirectory(file)) {
                        scanDirectory(file, classes, directories);
                    } else if (file.getFileName().toString().endsWith(CLASS_SUFFIX)) {
                        String fileName = file.getFileName().toString();
                        classes.add(Entry.atBase(
                                relativePath + "/" + fileName.substring(0, fileName.length() - CLASS_SUFFIX.length())));
                    }
                }
            } catch (IOException e) {
                ZomboidDecompiler.log.log(e);
            }
        }
    }

    @Override
    public InputStream getInputStream(String className) throws IOException {
        return Files.newInputStream(this.jarFilesystem.getPath(className));
    }

    @Override
    public IOutputSink createOutputSink(IResultSaver saver) {
        return new IOutputSink() {
            @Override
            public void begin() {
                if (!(saver instanceof ConsoleDecompiler)) {
                    saver.createArchive(jar.toAbsolutePath().toString(), "", null);
                }
                saver.saveFolder("");
            }

            @Override
            public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
                saver.saveClassFile("", qualifiedName, fileName, content, mapping);
            }

            @Override
            public void acceptDirectory(String directory) {
                saver.saveFolder(directory);
            }

            @Override
            public void acceptOther(String path) {
                // can't do this from a zip... probably not a big deal
                // we don't populate the others list anyway
                // saver.copyFile(new File(jar, path).getAbsolutePath(), "", path);
            }

            @Override
            public void close() throws IOException {
                saver.closeArchive("", jar.getFileName().toString());
            }
        };
    }

    @Override
    public void close() throws IOException {
        this.jarFilesystem.close();
    }

    public ZomboidContextSource(Path jar, Set<String> packages, boolean invertPackages) throws IOException {
        assert Files.isDirectory(jar);
        this.jar = jar;
        this.jarFilesystem = FileSystems.newFileSystem(jar);
        this.packages = packages;
        this.invertPackages = invertPackages;
    }
}
