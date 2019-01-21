package com.android.okhttp.internal.http;

import com.android.okhttp.internal.Util;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.Sink;
import com.android.okhttp.okio.Timeout;
import java.io.IOException;
import java.net.ProtocolException;

public final class RetryableSink implements Sink {
    private boolean closed;
    private final Buffer content;
    private final int limit;

    public RetryableSink(int limit) {
        this.content = new Buffer();
        this.limit = limit;
    }

    public RetryableSink() {
        this(-1);
    }

    public void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            if (this.content.size() < ((long) this.limit)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("content-length promised ");
                stringBuilder.append(this.limit);
                stringBuilder.append(" bytes, but received ");
                stringBuilder.append(this.content.size());
                throw new ProtocolException(stringBuilder.toString());
            }
        }
    }

    public void write(Buffer source, long byteCount) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        Util.checkOffsetAndCount(source.size(), 0, byteCount);
        if (this.limit == -1 || this.content.size() <= ((long) this.limit) - byteCount) {
            this.content.write(source, byteCount);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("exceeded content-length limit of ");
        stringBuilder.append(this.limit);
        stringBuilder.append(" bytes");
        throw new ProtocolException(stringBuilder.toString());
    }

    public void flush() throws IOException {
    }

    public Timeout timeout() {
        return Timeout.NONE;
    }

    public long contentLength() throws IOException {
        return this.content.size();
    }

    public void writeToSocket(Sink socketOut) throws IOException {
        Buffer buffer = new Buffer();
        this.content.copyTo(buffer, 0, this.content.size());
        socketOut.write(buffer, buffer.size());
    }
}
