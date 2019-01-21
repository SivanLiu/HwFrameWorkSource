package com.android.org.bouncycastle.crypto.modes;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.SkippingStreamCipher;
import com.android.org.bouncycastle.crypto.StreamBlockCipher;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Pack;

public class SICBlockCipher extends StreamBlockCipher implements SkippingStreamCipher {
    private byte[] IV = new byte[this.blockSize];
    private final int blockSize = this.cipher.getBlockSize();
    private int byteCount = 0;
    private final BlockCipher cipher;
    private byte[] counter = new byte[this.blockSize];
    private byte[] counterOut = new byte[this.blockSize];

    public SICBlockCipher(BlockCipher c) {
        super(c);
        this.cipher = c;
    }

    public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
        if (params instanceof ParametersWithIV) {
            ParametersWithIV ivParam = (ParametersWithIV) params;
            this.IV = Arrays.clone(ivParam.getIV());
            if (this.blockSize >= this.IV.length) {
                int i = 8;
                if (8 > this.blockSize / 2) {
                    i = this.blockSize / 2;
                }
                int maxCounterSize = i;
                if (this.blockSize - this.IV.length <= maxCounterSize) {
                    if (ivParam.getParameters() != null) {
                        this.cipher.init(true, ivParam.getParameters());
                    }
                    reset();
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CTR/SIC mode requires IV of at least: ");
                stringBuilder.append(this.blockSize - maxCounterSize);
                stringBuilder.append(" bytes.");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CTR/SIC mode requires IV no greater than: ");
            stringBuilder2.append(this.blockSize);
            stringBuilder2.append(" bytes.");
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
        throw new IllegalArgumentException("CTR/SIC mode requires ParametersWithIV");
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.cipher.getAlgorithmName());
        stringBuilder.append("/SIC");
        return stringBuilder.toString();
    }

    public int getBlockSize() {
        return this.cipher.getBlockSize();
    }

    public int processBlock(byte[] in, int inOff, byte[] out, int outOff) throws DataLengthException, IllegalStateException {
        processBytes(in, inOff, this.blockSize, out, outOff);
        return this.blockSize;
    }

    protected byte calculateByte(byte in) throws DataLengthException, IllegalStateException {
        byte[] bArr;
        if (this.byteCount == 0) {
            this.cipher.processBlock(this.counter, 0, this.counterOut, 0);
            bArr = this.counterOut;
            int i = this.byteCount;
            this.byteCount = i + 1;
            return (byte) (bArr[i] ^ in);
        }
        bArr = this.counterOut;
        int i2 = this.byteCount;
        this.byteCount = i2 + 1;
        byte rv = (byte) (bArr[i2] ^ in);
        if (this.byteCount == this.counter.length) {
            this.byteCount = 0;
            incrementCounterAt(0);
            checkCounter();
        }
        return rv;
    }

    private void checkCounter() {
        if (this.IV.length < this.blockSize) {
            int i = 0;
            while (i != this.IV.length) {
                if (this.counter[i] == this.IV[i]) {
                    i++;
                } else {
                    throw new IllegalStateException("Counter in CTR/SIC mode out of range.");
                }
            }
        }
    }

    private void incrementCounterAt(int pos) {
        int i = this.counter.length - pos;
        byte b;
        do {
            i--;
            if (i >= 0) {
                byte[] bArr = this.counter;
                b = (byte) (bArr[i] + 1);
                bArr[i] = b;
            } else {
                return;
            }
        } while (b == (byte) 0);
    }

    private void incrementCounter(int offSet) {
        byte old = this.counter[this.counter.length - 1];
        byte[] bArr = this.counter;
        int length = this.counter.length - 1;
        bArr[length] = (byte) (bArr[length] + offSet);
        if (old != (byte) 0 && this.counter[this.counter.length - 1] < old) {
            incrementCounterAt(1);
        }
    }

    private void decrementCounterAt(int pos) {
        int i = this.counter.length - pos;
        byte b;
        do {
            i--;
            if (i >= 0) {
                byte[] bArr = this.counter;
                b = (byte) (bArr[i] - 1);
                bArr[i] = b;
            } else {
                return;
            }
        } while (b == (byte) -1);
    }

    private void adjustCounter(long n) {
        long j = n;
        int i = 5;
        long numBlocks;
        long rem;
        int i2;
        long diff;
        if (j >= 0) {
            numBlocks = (((long) this.byteCount) + j) / ((long) this.blockSize);
            rem = numBlocks;
            if (rem > 255) {
                while (true) {
                    i2 = i;
                    if (i2 < 1) {
                        break;
                    }
                    diff = 1 << (8 * i2);
                    while (rem >= diff) {
                        incrementCounterAt(i2);
                        rem -= diff;
                    }
                    i = i2 - 1;
                }
            }
            incrementCounter((int) rem);
            this.byteCount = (int) ((((long) this.byteCount) + j) - (((long) this.blockSize) * numBlocks));
            return;
        }
        rem = ((-j) - ((long) this.byteCount)) / ((long) this.blockSize);
        numBlocks = rem;
        if (numBlocks > 255) {
            while (true) {
                i2 = i;
                if (i2 < 1) {
                    break;
                }
                diff = 1 << (8 * i2);
                while (numBlocks > diff) {
                    decrementCounterAt(i2);
                    numBlocks -= diff;
                }
                i = i2 - 1;
            }
        }
        long i3 = 0;
        while (true) {
            long i4 = i3;
            if (i4 == numBlocks) {
                break;
            }
            decrementCounterAt(0);
            i3 = i4 + 1;
        }
        i2 = (int) ((((long) this.byteCount) + j) + (((long) this.blockSize) * rem));
        if (i2 >= 0) {
            this.byteCount = 0;
            return;
        }
        decrementCounterAt(0);
        this.byteCount = this.blockSize + i2;
    }

    public void reset() {
        Arrays.fill(this.counter, (byte) 0);
        System.arraycopy(this.IV, 0, this.counter, 0, this.IV.length);
        this.cipher.reset();
        this.byteCount = 0;
    }

    public long skip(long numberOfBytes) {
        adjustCounter(numberOfBytes);
        checkCounter();
        this.cipher.processBlock(this.counter, 0, this.counterOut, 0);
        return numberOfBytes;
    }

    public long seekTo(long position) {
        reset();
        return skip(position);
    }

    public long getPosition() {
        byte[] res = new byte[this.counter.length];
        System.arraycopy(this.counter, 0, res, 0, res.length);
        for (int i = res.length - 1; i >= 1; i--) {
            int v;
            if (i < this.IV.length) {
                v = (res[i] & 255) - (this.IV[i] & 255);
            } else {
                v = res[i] & 255;
            }
            if (v < 0) {
                int i2 = i - 1;
                res[i2] = (byte) (res[i2] - 1);
                v += 256;
            }
            res[i] = (byte) v;
        }
        return (Pack.bigEndianToLong(res, res.length - 8) * ((long) this.blockSize)) + ((long) this.byteCount);
    }
}
