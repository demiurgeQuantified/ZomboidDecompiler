package com.github.zomboiddecompiler;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class FileLogger implements ILogger {
    private final PrintStream stream;

    FileLogger(File file) throws FileNotFoundException {
        stream = new PrintStream(file);
    }

    @Override
    public void log(@NotNull String text) {
        stream.println(text);
    }

    @NotNull
    public PrintStream getStream() {
        return stream;
    }
}
