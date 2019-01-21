package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.io.Streams;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

class DefiniteLengthInputStream extends LimitedInputStream {
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final int _originalLength;
    private int _remaining;

    DefiniteLengthInputStream(InputStream in, int length) {
        super(in, length);
        if (length >= 0) {
            this._originalLength = length;
            this._remaining = length;
            if (length == 0) {
                setParentEofDetect(true);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("negative lengths not allowed");
    }

    int getRemaining() {
        return this._remaining;
    }

    public int read() throws IOException {
        if (this._remaining == 0) {
            return -1;
        }
        int b = this._in.read();
        if (b >= 0) {
            int i = this._remaining - 1;
            this._remaining = i;
            if (i == 0) {
                setParentEofDetect(true);
            }
            return b;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DEF length ");
        stringBuilder.append(this._originalLength);
        stringBuilder.append(" object truncated by ");
        stringBuilder.append(this._remaining);
        throw new EOFException(stringBuilder.toString());
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        if (this._remaining == 0) {
            return -1;
        }
        int numRead = this._in.read(buf, off, Math.min(len, this._remaining));
        if (numRead >= 0) {
            int i = this._remaining - numRead;
            this._remaining = i;
            if (i == 0) {
                setParentEofDetect(true);
            }
            return numRead;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DEF length ");
        stringBuilder.append(this._originalLength);
        stringBuilder.append(" object truncated by ");
        stringBuilder.append(this._remaining);
        throw new EOFException(stringBuilder.toString());
    }

    byte[] toByteArray() throws IOException {
        if (this._remaining == 0) {
            return EMPTY_BYTES;
        }
        byte[] bytes = new byte[this._remaining];
        int readFully = this._remaining - Streams.readFully(this._in, bytes);
        this._remaining = readFully;
        if (readFully == 0) {
            setParentEofDetect(true);
            return bytes;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DEF length ");
        stringBuilder.append(this._originalLength);
        stringBuilder.append(" object truncated by ");
        stringBuilder.append(this._remaining);
        throw new EOFException(stringBuilder.toString());
    }
}
