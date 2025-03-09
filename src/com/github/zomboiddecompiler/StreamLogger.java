package com.github.zomboiddecompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class StreamLogger implements ILogger {
    private final PrintStream stream;

    StreamLogger(File file) throws FileNotFoundException {
        stream = new PrintStream(file);
    }

    StreamLogger(PrintStream stream) {
        this.stream = stream;
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
