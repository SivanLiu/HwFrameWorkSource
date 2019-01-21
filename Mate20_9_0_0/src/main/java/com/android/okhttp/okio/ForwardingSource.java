package com.android.okhttp.okio;

import java.io.IOException;

public abstract class ForwardingSource implements Source {
    private final Source delegate;

    public ForwardingSource(Source delegate) {
        if (delegate != null) {
            this.delegate = delegate;
            return;
        }
        throw new IllegalArgumentException("delegate == null");
    }

    public final Source delegate() {
        return this.delegate;
    }

    public long read(Buffer sink, long byteCount) throws IOException {
        return this.delegate.read(sink, byteCount);
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
