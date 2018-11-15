package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.StreamBlockCipher;

public class CTSBlockCipher extends BufferedBlockCipher {
    private int blockSize;

    public CTSBlockCipher(BlockCipher blockCipher) {
        if (blockCipher instanceof StreamBlockCipher) {
            throw new IllegalArgumentException("CTSBlockCipher can only accept ECB, or CBC ciphers");
        }
        this.cipher = blockCipher;
        this.blockSize = blockCipher.getBlockSize();
        this.buf = new byte[(this.blockSize * 2)];
        this.bufOff = 0;
    }

    public int doFinal(byte[] bArr, int i) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
        if (this.bufOff + i <= bArr.length) {
            int i2;
            int blockSize = this.cipher.getBlockSize();
            int i3 = this.bufOff - blockSize;
            Object obj = new byte[blockSize];
            if (this.forEncryption) {
                if (this.bufOff >= blockSize) {
                    this.cipher.processBlock(this.buf, 0, obj, 0);
                    if (this.bufOff > blockSize) {
                        int i4;
                        for (i4 = this.bufOff; i4 != this.buf.length; i4++) {
                            this.buf[i4] = obj[i4 - blockSize];
                        }
                        for (i4 = blockSize; i4 != this.bufOff; i4++) {
                            byte[] bArr2 = this.buf;
                            bArr2[i4] = (byte) (bArr2[i4] ^ obj[i4 - blockSize]);
                        }
                        (this.cipher instanceof CBCBlockCipher ? ((CBCBlockCipher) this.cipher).getUnderlyingCipher() : this.cipher).processBlock(this.buf, blockSize, bArr, i);
                        System.arraycopy(obj, 0, bArr, i + blockSize, i3);
                        i2 = this.bufOff;
                        reset();
                        return i2;
                    }
                }
                throw new DataLengthException("need at least one block of input for CTS");
            } else if (this.bufOff >= blockSize) {
                Object obj2 = new byte[blockSize];
                if (this.bufOff > blockSize) {
                    (this.cipher instanceof CBCBlockCipher ? ((CBCBlockCipher) this.cipher).getUnderlyingCipher() : this.cipher).processBlock(this.buf, 0, obj, 0);
                    for (int i5 = blockSize; i5 != this.bufOff; i5++) {
                        int i6 = i5 - blockSize;
                        obj2[i6] = (byte) (obj[i6] ^ this.buf[i5]);
                    }
                    System.arraycopy(this.buf, blockSize, obj, 0, i3);
                    this.cipher.processBlock(obj, 0, bArr, i);
                    System.arraycopy(obj2, 0, bArr, i + blockSize, i3);
                    i2 = this.bufOff;
                    reset();
                    return i2;
                }
                this.cipher.processBlock(this.buf, 0, obj, 0);
            } else {
                throw new DataLengthException("need at least one block of input for CTS");
            }
            System.arraycopy(obj, 0, bArr, i, blockSize);
            i2 = this.bufOff;
            reset();
            return i2;
        }
        throw new OutputLengthException("output buffer to small in doFinal");
    }

    public int getOutputSize(int i) {
        return i + this.bufOff;
    }

    public int getUpdateOutputSize(int i) {
        i += this.bufOff;
        int length = i % this.buf.length;
        return length == 0 ? i - this.buf.length : i - length;
    }

    public int processByte(byte b, byte[] bArr, int i) throws DataLengthException, IllegalStateException {
        int processBlock;
        if (this.bufOff == this.buf.length) {
            processBlock = this.cipher.processBlock(this.buf, 0, bArr, i);
            System.arraycopy(this.buf, this.blockSize, this.buf, 0, this.blockSize);
            this.bufOff = this.blockSize;
        } else {
            processBlock = 0;
        }
        byte[] bArr2 = this.buf;
        int i2 = this.bufOff;
        this.bufOff = i2 + 1;
        bArr2[i2] = b;
        return processBlock;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException, IllegalStateException {
        if (i2 >= 0) {
            int blockSize = getBlockSize();
            int updateOutputSize = getUpdateOutputSize(i2);
            if (updateOutputSize <= 0 || updateOutputSize + i3 <= bArr2.length) {
                updateOutputSize = this.buf.length - this.bufOff;
                int i4 = 0;
                if (i2 > updateOutputSize) {
                    System.arraycopy(bArr, i, this.buf, this.bufOff, updateOutputSize);
                    int processBlock = this.cipher.processBlock(this.buf, 0, bArr2, i3) + 0;
                    System.arraycopy(this.buf, blockSize, this.buf, 0, blockSize);
                    this.bufOff = blockSize;
                    i2 -= updateOutputSize;
                    i += updateOutputSize;
                    while (i2 > blockSize) {
                        System.arraycopy(bArr, i, this.buf, this.bufOff, blockSize);
                        processBlock += this.cipher.processBlock(this.buf, 0, bArr2, i3 + processBlock);
                        System.arraycopy(this.buf, blockSize, this.buf, 0, blockSize);
                        i2 -= blockSize;
                        i += blockSize;
                    }
                    i4 = processBlock;
                }
                System.arraycopy(bArr, i, this.buf, this.bufOff, i2);
                this.bufOff += i2;
                return i4;
            }
            throw new OutputLengthException("output buffer too short");
        }
        throw new IllegalArgumentException("Can't have a negative input length!");
    }
}
