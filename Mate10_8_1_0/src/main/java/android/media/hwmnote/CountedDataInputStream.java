package android.media.hwmnote;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class CountedDataInputStream extends FilterInputStream {
    static final /* synthetic */ boolean -assertionsDisabled = (CountedDataInputStream.class.desiredAssertionStatus() ^ 1);
    private final byte[] mByteArray = new byte[8];
    private final ByteBuffer mByteBuffer = ByteBuffer.wrap(this.mByteArray);
    private int mCount = 0;

    protected CountedDataInputStream(InputStream in) {
        super(in);
    }

    public int getReadByteCount() {
        return this.mCount;
    }

    public int read(byte[] b) throws IOException {
        int i = 0;
        int r = this.in.read(b);
        int i2 = this.mCount;
        if (r >= 0) {
            i = r;
        }
        this.mCount = i + i2;
        return r;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int i = 0;
        int r = this.in.read(b, off, len);
        int i2 = this.mCount;
        if (r >= 0) {
            i = r;
        }
        this.mCount = i + i2;
        return r;
    }

    public int read() throws IOException {
        int i = 0;
        int r = this.in.read();
        int i2 = this.mCount;
        if (r >= 0) {
            i = 1;
        }
        this.mCount = i + i2;
        return r;
    }

    public long skip(long length) throws IOException {
        long skip = this.in.skip(length);
        this.mCount = (int) (((long) this.mCount) + skip);
        return skip;
    }

    public void skipOrThrow(long length) throws IOException {
        if (skip(length) != length) {
            throw new EOFException();
        }
    }

    public void skipTo(long target) throws IOException {
        long diff = target - ((long) this.mCount);
        if (-assertionsDisabled || diff >= 0) {
            skipOrThrow(diff);
            return;
        }
        throw new AssertionError();
    }

    public void readOrThrow(byte[] b, int off, int len) throws IOException {
        if (read(b, off, len) != len) {
            throw new EOFException();
        }
    }

    public void readOrThrow(byte[] b) throws IOException {
        readOrThrow(b, 0, b.length);
    }

    public void setByteOrder(ByteOrder order) {
        this.mByteBuffer.order(order);
    }

    public ByteOrder getByteOrder() {
        return this.mByteBuffer.order();
    }

    public short readShort() throws IOException {
        readOrThrow(this.mByteArray, 0, 2);
        this.mByteBuffer.rewind();
        return this.mByteBuffer.getShort();
    }

    public int readUnsignedShort() throws IOException {
        return readShort() & 65535;
    }

    public int readInt() throws IOException {
        readOrThrow(this.mByteArray, 0, 4);
        this.mByteBuffer.rewind();
        return this.mByteBuffer.getInt();
    }

    public long readUnsignedInt() throws IOException {
        return ((long) readInt()) & 4294967295L;
    }

    public long readLong() throws IOException {
        readOrThrow(this.mByteArray, 0, 8);
        this.mByteBuffer.rewind();
        return this.mByteBuffer.getLong();
    }
}
