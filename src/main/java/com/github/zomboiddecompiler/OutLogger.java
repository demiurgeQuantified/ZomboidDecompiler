package com.github.zomboiddecompiler;

import org.jetbrains.annotations.Nullable;

public class OutLogger implements ILogger {
    public @Nullable ILogger delegate;

    @Override
    public void log(String text) {
        System.out.println(text);
        if (delegate != null) {
            delegate.log(text);
        }
    }

    @Override
    public void log(Exception exception) {
        exception.printStackTrace(System.err);
        if (delegate != null) {
            delegate.log(exception);
        }
    }

    public OutLogger(@Nullable ILogger delegate) {
        this.delegate = delegate;
    }
}
