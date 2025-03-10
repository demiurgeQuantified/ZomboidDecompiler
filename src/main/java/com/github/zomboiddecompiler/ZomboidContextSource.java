package com.github.zomboiddecompiler;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ZomboidContextSource implements IContextSource {
    private final File directory;
    private static final String CLASS_SUFFIX = ".class";

    @Override
    public String getName() {
        return "Project Zomboid: " + directory.getAbsolutePath();
    }

    @Override
    public Entries getEntries() {
        List<Entry> classes = new ArrayList<>();
        List<String> directories = new ArrayList<>();

        File zombieDirectory = new File(directory, "zombie");
        scanDirectory(zombieDirectory, classes, directories);

        return new Entries(classes, directories, new ArrayList<>(), new ArrayList<>());
    }

    void scanDirectory(final File current, final List<Entry> classes, final List<String> directories) {
        String relativePath = directory.toPath().relativize(current.toPath())
                .toString().replace(File.separatorChar, '/');
        if (current.isDirectory()) {
            directories.add(relativePath);
            for (File file : Objects.requireNonNull(current.listFiles())) {
                if (file.isDirectory()) {
                    scanDirectory(file, classes, directories);
                } else if (file.getName().endsWith(CLASS_SUFFIX)) {
                    String fileName = file.getName();
                    classes.add(Entry.atBase(
                            relativePath + "/" + fileName.substring(0, fileName.length() - CLASS_SUFFIX.length())));
                }
            }
        }
    }

    @Override
    public InputStream getInputStream(String className) throws IOException {
        File classFile = new File(directory, className);
        return new FileInputStream(classFile);
    }

    @Override
    public IOutputSink createOutputSink(IResultSaver saver) {
        return new IOutputSink() {
            @Override
            public void begin() {
                if (!(saver instanceof ConsoleDecompiler)) {
                    saver.createArchive(directory.getAbsolutePath(), "", null);
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
                saver.copyFile(new File(directory, path).getAbsolutePath(), "", path);
            }

            @Override
            public void close() throws IOException {
                saver.closeArchive("", directory.getName() + ".jar");
            }
        };
    }

    public ZomboidContextSource(File gameDirectory) {
        assert gameDirectory.exists() && gameDirectory.isDirectory();
        directory = gameDirectory;
    }
}
