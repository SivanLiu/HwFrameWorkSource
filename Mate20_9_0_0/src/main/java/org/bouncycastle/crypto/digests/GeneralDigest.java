package org.bouncycastle.crypto.digests;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Pack;

public abstract class GeneralDigest implements ExtendedDigest, Memoable {
    private static final int BYTE_LENGTH = 64;
    private long byteCount;
    private final byte[] xBuf;
    private int xBufOff;

    protected GeneralDigest() {
        this.xBuf = new byte[4];
        this.xBufOff = 0;
    }

    protected GeneralDigest(GeneralDigest generalDigest) {
        this.xBuf = new byte[4];
        copyIn(generalDigest);
    }

    protected GeneralDigest(byte[] bArr) {
        this.xBuf = new byte[4];
        System.arraycopy(bArr, 0, this.xBuf, 0, this.xBuf.length);
        this.xBufOff = Pack.bigEndianToInt(bArr, 4);
        this.byteCount = Pack.bigEndianToLong(bArr, 8);
    }

    protected void copyIn(GeneralDigest generalDigest) {
        System.arraycopy(generalDigest.xBuf, 0, this.xBuf, 0, generalDigest.xBuf.length);
        this.xBufOff = generalDigest.xBufOff;
        this.byteCount = generalDigest.byteCount;
    }

    public void finish() {
        long j = this.byteCount << 3;
        byte b = Byte.MIN_VALUE;
        while (true) {
            update(b);
            if (this.xBufOff != 0) {
                b = (byte) 0;
            } else {
                processLength(j);
                processBlock();
                return;
            }
        }
    }

    public int getByteLength() {
        return 64;
    }

    protected void populateState(byte[] bArr) {
        System.arraycopy(this.xBuf, 0, bArr, 0, this.xBufOff);
        Pack.intToBigEndian(this.xBufOff, bArr, 4);
        Pack.longToBigEndian(this.byteCount, bArr, 8);
    }

    protected abstract void processBlock();

    protected abstract void processLength(long j);

    protected abstract void processWord(byte[] bArr, int i);

    public void reset() {
        this.byteCount = 0;
        this.xBufOff = 0;
        for (int i = 0; i < this.xBuf.length; i++) {
            this.xBuf[i] = (byte) 0;
        }
    }

    public void update(byte b) {
        byte[] bArr = this.xBuf;
        int i = this.xBufOff;
        this.xBufOff = i + 1;
        bArr[i] = b;
        if (this.xBufOff == this.xBuf.length) {
            processWord(this.xBuf, 0);
            this.xBufOff = 0;
        }
        this.byteCount++;
    }

    public void update(byte[] bArr, int i, int i2) {
        int i3;
        int i4;
        int i5 = 0;
        i2 = Math.max(0, i2);
        if (this.xBufOff != 0) {
            i3 = 0;
            while (i3 < i2) {
                byte[] bArr2 = this.xBuf;
                i4 = this.xBufOff;
                this.xBufOff = i4 + 1;
                int i6 = i3 + 1;
                bArr2[i4] = bArr[i3 + i];
                if (this.xBufOff == 4) {
                    processWord(this.xBuf, 0);
                    this.xBufOff = 0;
                    i5 = i6;
                    break;
                }
                i3 = i6;
            }
            i5 = i3;
        }
        i3 = ((i2 - i5) & -4) + i5;
        while (i5 < i3) {
            processWord(bArr, i + i5);
            i5 += 4;
        }
        while (i5 < i2) {
            byte[] bArr3 = this.xBuf;
            int i7 = this.xBufOff;
            this.xBufOff = i7 + 1;
            i4 = i5 + 1;
            bArr3[i7] = bArr[i5 + i];
            i5 = i4;
        }
        this.byteCount += (long) i2;
    }
}
