package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.OutputLengthException;

public class NISTCTSBlockCipher extends BufferedBlockCipher {
    public static final int CS1 = 1;
    public static final int CS2 = 2;
    public static final int CS3 = 3;
    private final int blockSize;
    private final int type;

    public NISTCTSBlockCipher(int i, BlockCipher blockCipher) {
        this.type = i;
        this.cipher = new CBCBlockCipher(blockCipher);
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
            Object obj2;
            if (this.forEncryption) {
                if (this.bufOff < blockSize) {
                    throw new DataLengthException("need at least one block of input for NISTCTS");
                } else if (this.bufOff > blockSize) {
                    obj2 = new byte[blockSize];
                    if (this.type == 2 || this.type == 3) {
                        this.cipher.processBlock(this.buf, 0, obj, 0);
                        System.arraycopy(this.buf, blockSize, obj2, 0, i3);
                        this.cipher.processBlock(obj2, 0, obj2, 0);
                        if (this.type == 2 && i3 == blockSize) {
                            System.arraycopy(obj, 0, bArr, i, blockSize);
                            System.arraycopy(obj2, 0, bArr, i + blockSize, i3);
                            i2 = this.bufOff;
                            reset();
                            return i2;
                        }
                        System.arraycopy(obj2, 0, bArr, i, blockSize);
                        System.arraycopy(obj, 0, bArr, i + blockSize, i3);
                        i2 = this.bufOff;
                        reset();
                        return i2;
                    }
                    System.arraycopy(this.buf, 0, obj, 0, blockSize);
                    this.cipher.processBlock(obj, 0, obj, 0);
                    System.arraycopy(obj, 0, bArr, i, i3);
                    System.arraycopy(this.buf, this.bufOff - i3, obj2, 0, i3);
                    this.cipher.processBlock(obj2, 0, obj2, 0);
                    System.arraycopy(obj2, 0, bArr, i + i3, blockSize);
                    i2 = this.bufOff;
                    reset();
                    return i2;
                }
            } else if (this.bufOff >= blockSize) {
                obj2 = new byte[blockSize];
                if (this.bufOff > blockSize) {
                    if (this.type == 3 || (this.type == 2 && (this.buf.length - this.bufOff) % blockSize != 0)) {
                        (this.cipher instanceof CBCBlockCipher ? ((CBCBlockCipher) this.cipher).getUnderlyingCipher() : this.cipher).processBlock(this.buf, 0, obj, 0);
                        for (int i4 = blockSize; i4 != this.bufOff; i4++) {
                            int i5 = i4 - blockSize;
                            obj2[i5] = (byte) (obj[i5] ^ this.buf[i4]);
                        }
                        System.arraycopy(this.buf, blockSize, obj, 0, i3);
                        this.cipher.processBlock(obj, 0, bArr, i);
                        System.arraycopy(obj2, 0, bArr, i + blockSize, i3);
                        i2 = this.bufOff;
                        reset();
                        return i2;
                    }
                    ((CBCBlockCipher) this.cipher).getUnderlyingCipher().processBlock(this.buf, this.bufOff - blockSize, obj2, 0);
                    System.arraycopy(this.buf, 0, obj, 0, blockSize);
                    if (i3 != blockSize) {
                        System.arraycopy(obj2, i3, obj, i3, blockSize - i3);
                    }
                    this.cipher.processBlock(obj, 0, obj, 0);
                    System.arraycopy(obj, 0, bArr, i, blockSize);
                    for (int i6 = 0; i6 != i3; i6++) {
                        obj2[i6] = (byte) (obj2[i6] ^ this.buf[i6]);
                    }
                    System.arraycopy(obj2, 0, bArr, i + blockSize, i3);
                    i2 = this.bufOff;
                    reset();
                    return i2;
                }
            } else {
                throw new DataLengthException("need at least one block of input for CTS");
            }
            this.cipher.processBlock(this.buf, 0, obj, 0);
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
