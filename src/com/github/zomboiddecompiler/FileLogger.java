package com.github.zomboiddecompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class FileLogger implements ILogger {
    private final PrintStream stream;

    FileLogger(File file) throws FileNotFoundException {
        stream = new PrintStream(file);
    }

    @Override
    public void log(String text) {
        stream.println(text);
    }

    @Override
    public void log(Exception exception) {
        exception.printStackTrace(stream);
    }

    public PrintStream getStream() {
        return stream;
    }
}
