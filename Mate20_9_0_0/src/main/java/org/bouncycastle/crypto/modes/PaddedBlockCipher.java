package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.OutputLengthException;

public class PaddedBlockCipher extends BufferedBlockCipher {
    public PaddedBlockCipher(BlockCipher blockCipher) {
        this.cipher = blockCipher;
        this.buf = new byte[blockCipher.getBlockSize()];
        this.bufOff = 0;
    }

    public int doFinal(byte[] bArr, int i) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
        int i2;
        int blockSize = this.cipher.getBlockSize();
        if (this.forEncryption) {
            if (this.bufOff != blockSize) {
                i2 = 0;
            } else if ((2 * blockSize) + i <= bArr.length) {
                i2 = this.cipher.processBlock(this.buf, 0, bArr, i);
                this.bufOff = 0;
            } else {
                throw new OutputLengthException("output buffer too short");
            }
            byte b = (byte) (blockSize - this.bufOff);
            while (this.bufOff < blockSize) {
                this.buf[this.bufOff] = b;
                this.bufOff++;
            }
            i2 += this.cipher.processBlock(this.buf, 0, bArr, i + i2);
        } else if (this.bufOff == blockSize) {
            i2 = this.cipher.processBlock(this.buf, 0, this.buf, 0);
            this.bufOff = 0;
            int i3 = this.buf[blockSize - 1] & 255;
            if (i3 < 0 || i3 > blockSize) {
                throw new InvalidCipherTextException("pad block corrupted");
            }
            i2 -= i3;
            System.arraycopy(this.buf, 0, bArr, i, i2);
        } else {
            throw new DataLengthException("last block incomplete in decryption");
        }
        reset();
        return i2;
    }

    public int getOutputSize(int i) {
        i += this.bufOff;
        int length = i % this.buf.length;
        if (length != 0) {
            i -= length;
            length = this.buf.length;
        } else if (!this.forEncryption) {
            return i;
        } else {
            length = this.buf.length;
        }
        return i + length;
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
            this.bufOff = 0;
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
                    this.bufOff = 0;
                    i2 -= updateOutputSize;
                    i += updateOutputSize;
                    i4 = processBlock;
                    while (i2 > this.buf.length) {
                        i4 += this.cipher.processBlock(bArr, i, bArr2, i3 + i4);
                        i2 -= blockSize;
                        i += blockSize;
                    }
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
