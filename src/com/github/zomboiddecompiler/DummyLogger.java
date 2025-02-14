package com.github.zomboiddecompiler;

public class DummyLogger implements ILogger {
    @Override
    public void log(String text) {}

    @Override
    public void log(Exception exception) {}
}
