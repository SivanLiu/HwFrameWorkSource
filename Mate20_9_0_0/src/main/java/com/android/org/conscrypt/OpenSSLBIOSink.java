package com.android.org.conscrypt;

import java.io.ByteArrayOutputStream;

final class OpenSSLBIOSink {
    private final ByteArrayOutputStream buffer;
    private final long ctx;
    private int position;

    static OpenSSLBIOSink create() {
        return new OpenSSLBIOSink(new ByteArrayOutputStream());
    }

    private OpenSSLBIOSink(ByteArrayOutputStream buffer) {
        this.ctx = NativeCrypto.create_BIO_OutputStream(buffer);
        this.buffer = buffer;
    }

    int available() {
        return this.buffer.size() - this.position;
    }

    void reset() {
        this.buffer.reset();
        this.position = 0;
    }

    long skip(long byteCount) {
        int maxLength = Math.min(available(), (int) byteCount);
        this.position += maxLength;
        if (this.position == this.buffer.size()) {
            reset();
        }
        return (long) maxLength;
    }

    long getContext() {
        return this.ctx;
    }

    byte[] toByteArray() {
        return this.buffer.toByteArray();
    }

    int position() {
        return this.position;
    }

    protected void finalize() throws Throwable {
        try {
            NativeCrypto.BIO_free_all(this.ctx);
        } finally {
            super.finalize();
        }
    }
}
