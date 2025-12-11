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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ZomboidContextSource implements IContextSource, AutoCloseable {
    private final Path jar;
    private static final String CLASS_SUFFIX = ".class";

    private final boolean invertPatterns;
    private final FileSystem jarFilesystem;

    private final ClassPatterns patterns;

    @Override
    public String getName() {
        return "Project Zomboid (inverted: " + this.invertPatterns + "): " + this.jar.toString();
    }

    @Override
    public Entries getEntries() {
        List<Entry> classes = new ArrayList<>();
        List<String> directories = new ArrayList<>();

        for (Path root : jarFilesystem.getRootDirectories()) {
            scanDirectory(root, classes, directories);
        }

        return new Entries(classes, directories, new ArrayList<>(), new ArrayList<>());
    }

    void scanDirectory(final Path current, final List<Entry> classes, final List<String> directories) {
        String relativePath = current.toString().replace(File.separatorChar, '/');
        // need to remove leading "/" or it writes to root of disk lol
        relativePath = relativePath.substring(1);
        directories.add(relativePath);
        try (Stream<Path> files = Files.list(current)) {
            for (Path file : files.toList()) {
                if (Files.isDirectory(file)) {
                    if (this.invertPatterns != this.patterns.partialMatch(file)) {
                        scanDirectory(file, classes, directories);
                    }
                } else if (
                        file.getFileName().toString().endsWith(CLASS_SUFFIX)
                        && this.invertPatterns != this.patterns.fullMatch(file)
                ) {
                    String fileName = file.getFileName().toString();
                    classes.add(Entry.atBase(
                            relativePath + "/" + fileName.substring(0, fileName.length() - CLASS_SUFFIX.length())));
                }
            }
        } catch (IOException e) {
            ZomboidDecompiler.log.log(e);
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

    public ZomboidContextSource(Path jar, String patterns, boolean invertPatterns) throws IOException {
        assert Files.isRegularFile(jar);
        this.jar = jar;
        this.jarFilesystem = FileSystems.newFileSystem(jar);
        this.invertPatterns = invertPatterns;
        this.patterns = ClassPatterns.fromString(patterns);
    }

    public static class ClassPatterns {
        private final List<ClassPattern> positivePatterns;
        private final List<ClassPattern> negativePatterns;

        /**
         * Tests whether a class file matches the patterns.
         * A class matches the patterns if no negative (-) pattern matches and at least one positive pattern does.
         * @param path Class relative to the classpath root.
         * @return Whether the class matches the pattern.
         */
        public boolean fullMatch(Path path) {
            if (path.isAbsolute()) {
                path = path.getRoot().relativize(path);
            }

            for (ClassPattern pattern : this.negativePatterns) {
                if (pattern.fullMatch(path)) {
                    return false;
                }
            }

            for (ClassPattern pattern : this.positivePatterns) {
                if (pattern.fullMatch(path)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Allows matches for directories that (could) lead to full matches.
         * @param path Class or directory relative to the classpath root.
         * @return Whether the path matches the pattern.
         * @see ClassPatterns#fullMatch(Path)
         */
        public boolean partialMatch(Path path) {
            if (path.isAbsolute()) {
                path = path.getRoot().relativize(path);
            }

            for (ClassPattern pattern : this.negativePatterns) {
                if (pattern.partialMatch(path)) {
                    return false;
                }
            }

            for (ClassPattern pattern : this.positivePatterns) {
                if (pattern.partialMatch(path)) {
                    return true;
                }
            }

            return false;
        }

        public static ClassPatterns fromString(String patterns) {
            List<ClassPattern> positivePatterns = new ArrayList<>();
            List<ClassPattern> negativePatterns = new ArrayList<>();
            for (String pattern : patterns.split(",")) {
                if (pattern.startsWith("-")) {
                    negativePatterns.add(ClassPattern.fromString(pattern.substring(1)));
                } else {
                    positivePatterns.add(ClassPattern.fromString(pattern));
                }
            }

            return new ClassPatterns(positivePatterns, negativePatterns);
        }

        private ClassPatterns(final List<ClassPattern> positivePatterns, final List<ClassPattern> negativePatterns) {
            this.positivePatterns = positivePatterns;
            this.negativePatterns = negativePatterns;
        }
    }

    public static class ClassPattern {
        private final List<String> elements;
        private final boolean isWildcard;

        /**
         * Tests whether a class file matches the pattern.
         * @param path Class relative to the classpath root.
         * @return Whether the class matches the pattern.
         */
        public boolean fullMatch(Path path) {
            if (this.isWildcard && elements.isEmpty()) {
                return true;
            }

            if (this.isWildcard) {
                return path.startsWith(String.join(File.separator, elements));
            }

            return path.toString().equals(String.join(File.separator, elements));
        }

        /**
         * Allows matches for directories that (could) lead to full matches.
         * @param path Class or directory relative to the classpath root.
         * @return Whether the path matches the pattern.
         * @see ClassPattern#fullMatch(Path)
         */
        public boolean partialMatch(Path path) {
            if (this.isWildcard && elements.isEmpty()) {
                return true;
            }

            if (path.getNameCount() < elements.size()) {
                for (int i = 1; i < path.getNameCount(); i++) {
                    if (!path.getName(i).toString().equals(elements.get(i))) {
                        return false;
                    }
                }
                return true;
            }

            if (this.isWildcard) {
                return path.startsWith(String.join(File.separator, elements));
            }

            return fullMatch(path);
        }

        public static ClassPattern fromString(final String pattern) {
            final List<String> elements = new ArrayList<>(
                    Arrays.asList(pattern.split("\\."))
            );

            boolean isWildcard = false;
            if (elements.get(elements.size() - 1).equals("*")) {
                elements.remove(elements.size() - 1);
                isWildcard = true;
            }

            return new ClassPattern(
                    elements,
                    isWildcard
            );
        }

        private ClassPattern(List<String> elements, boolean isWildcard) {
            this.elements = elements;
            this.isWildcard = isWildcard;
        }
    }
}
