package com.huawei.okio;

import java.io.IOException;

public abstract class ForwardingSink implements Sink {
    private final Sink delegate;

    public ForwardingSink(Sink delegate) {
        if (delegate != null) {
            this.delegate = delegate;
            return;
        }
        throw new IllegalArgumentException("delegate == null");
    }

    public final Sink delegate() {
        return this.delegate;
    }

    public void write(Buffer source, long byteCount) throws IOException {
        this.delegate.write(source, byteCount);
    }

    public void flush() throws IOException {
        this.delegate.flush();
    }

    public Timeout timeout() {
        return this.delegate.timeout();
    }

    public void close() throws IOException {
        this.delegate.close();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append(this.delegate.toString());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
