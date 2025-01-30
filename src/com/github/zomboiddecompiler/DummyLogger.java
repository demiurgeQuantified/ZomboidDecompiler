package com.github.zomboiddecompiler;

import org.jetbrains.annotations.NotNull;

public class DummyLogger implements ILogger {
    @Override
    public void log(@NotNull String text) {}
}
