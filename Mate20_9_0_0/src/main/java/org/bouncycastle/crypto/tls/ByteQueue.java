package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteQueue {
    private static final int DEFAULT_CAPACITY = 1024;
    private int available;
    private byte[] databuf;
    private boolean readOnlyBuf;
    private int skipped;

    public ByteQueue() {
        this(1024);
    }

    public ByteQueue(int i) {
        this.skipped = 0;
        this.available = 0;
        this.readOnlyBuf = false;
        this.databuf = i == 0 ? TlsUtils.EMPTY_BYTES : new byte[i];
    }

    public ByteQueue(byte[] bArr, int i, int i2) {
        this.skipped = 0;
        this.available = 0;
        this.readOnlyBuf = false;
        this.databuf = bArr;
        this.skipped = i;
        this.available = i2;
        this.readOnlyBuf = true;
    }

    public static int nextTwoPow(int i) {
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        return (i | (i >> 16)) + 1;
    }

    public void addData(byte[] bArr, int i, int i2) {
        if (this.readOnlyBuf) {
            throw new IllegalStateException("Cannot add data to read-only buffer");
        }
        if ((this.skipped + this.available) + i2 > this.databuf.length) {
            int nextTwoPow = nextTwoPow(this.available + i2);
            if (nextTwoPow > this.databuf.length) {
                byte[] bArr2 = new byte[nextTwoPow];
                System.arraycopy(this.databuf, this.skipped, bArr2, 0, this.available);
                this.databuf = bArr2;
            } else {
                System.arraycopy(this.databuf, this.skipped, this.databuf, 0, this.available);
            }
            this.skipped = 0;
        }
        System.arraycopy(bArr, i, this.databuf, this.skipped + this.available, i2);
        this.available += i2;
    }

    public int available() {
        return this.available;
    }

    public void copyTo(OutputStream outputStream, int i) throws IOException {
        if (i <= this.available) {
            outputStream.write(this.databuf, this.skipped, i);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot copy ");
        stringBuilder.append(i);
        stringBuilder.append(" bytes, only got ");
        stringBuilder.append(this.available);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void read(byte[] bArr, int i, int i2, int i3) {
        if (bArr.length - i < i2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Buffer size of ");
            stringBuilder.append(bArr.length);
            stringBuilder.append(" is too small for a read of ");
            stringBuilder.append(i2);
            stringBuilder.append(" bytes");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.available - i3 >= i2) {
            System.arraycopy(this.databuf, this.skipped + i3, bArr, i, i2);
        } else {
            throw new IllegalStateException("Not enough data to read");
        }
    }

    public ByteArrayInputStream readFrom(int i) {
        if (i <= this.available) {
            int i2 = this.skipped;
            this.available -= i;
            this.skipped += i;
            return new ByteArrayInputStream(this.databuf, i2, i);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot read ");
        stringBuilder.append(i);
        stringBuilder.append(" bytes, only got ");
        stringBuilder.append(this.available);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void removeData(int i) {
        if (i <= this.available) {
            this.available -= i;
            this.skipped += i;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot remove ");
        stringBuilder.append(i);
        stringBuilder.append(" bytes, only got ");
        stringBuilder.append(this.available);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void removeData(byte[] bArr, int i, int i2, int i3) {
        read(bArr, i, i2, i3);
        removeData(i3 + i2);
    }

    public byte[] removeData(int i, int i2) {
        byte[] bArr = new byte[i];
        removeData(bArr, 0, i, i2);
        return bArr;
    }

    public void shrink() {
        byte[] bArr;
        if (this.available == 0) {
            bArr = TlsUtils.EMPTY_BYTES;
        } else {
            int nextTwoPow = nextTwoPow(this.available);
            if (nextTwoPow < this.databuf.length) {
                bArr = new byte[nextTwoPow];
                System.arraycopy(this.databuf, this.skipped, bArr, 0, this.available);
            } else {
                return;
            }
        }
        this.databuf = bArr;
        this.skipped = 0;
    }
}
