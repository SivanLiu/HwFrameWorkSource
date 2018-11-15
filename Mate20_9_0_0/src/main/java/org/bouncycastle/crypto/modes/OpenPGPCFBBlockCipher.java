package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;

public class OpenPGPCFBBlockCipher implements BlockCipher {
    private byte[] FR = new byte[this.blockSize];
    private byte[] FRE = new byte[this.blockSize];
    private byte[] IV = new byte[this.blockSize];
    private int blockSize;
    private BlockCipher cipher;
    private int count;
    private boolean forEncryption;

    public OpenPGPCFBBlockCipher(BlockCipher blockCipher) {
        this.cipher = blockCipher;
        this.blockSize = blockCipher.getBlockSize();
    }

    private int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.blockSize + i > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (this.blockSize + i2 <= bArr2.length) {
            int i3 = 2;
            int i4 = 0;
            byte b;
            if (this.count > this.blockSize) {
                b = bArr[i];
                this.FR[this.blockSize - 2] = b;
                bArr2[i2] = encryptByte(b, this.blockSize - 2);
                b = bArr[i + 1];
                this.FR[this.blockSize - 1] = b;
                bArr2[i2 + 1] = encryptByte(b, this.blockSize - 1);
                this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                while (i3 < this.blockSize) {
                    b = bArr[i + i3];
                    i4 = i3 - 2;
                    this.FR[i4] = b;
                    bArr2[i2 + i3] = encryptByte(b, i4);
                    i3++;
                }
            } else {
                if (this.count == 0) {
                    this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                    while (i4 < this.blockSize) {
                        int i5 = i + i4;
                        this.FR[i4] = bArr[i5];
                        bArr2[i4] = encryptByte(bArr[i5], i4);
                        i4++;
                    }
                } else if (this.count == this.blockSize) {
                    this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                    b = bArr[i];
                    byte b2 = bArr[i + 1];
                    bArr2[i2] = encryptByte(b, 0);
                    bArr2[i2 + 1] = encryptByte(b2, 1);
                    System.arraycopy(this.FR, 2, this.FR, 0, this.blockSize - 2);
                    this.FR[this.blockSize - 2] = b;
                    this.FR[this.blockSize - 1] = b2;
                    this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                    while (i3 < this.blockSize) {
                        b = bArr[i + i3];
                        i4 = i3 - 2;
                        this.FR[i4] = b;
                        bArr2[i2 + i3] = encryptByte(b, i4);
                        i3++;
                    }
                }
                this.count += this.blockSize;
            }
            return this.blockSize;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    private int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.blockSize + i > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (this.blockSize + i2 <= bArr2.length) {
            int i3 = 2;
            int i4 = 0;
            byte[] bArr3;
            int i5;
            byte encryptByte;
            int i6;
            byte encryptByte2;
            if (this.count > this.blockSize) {
                bArr3 = this.FR;
                i5 = this.blockSize - 2;
                byte encryptByte3 = encryptByte(bArr[i], this.blockSize - 2);
                bArr2[i2] = encryptByte3;
                bArr3[i5] = encryptByte3;
                bArr3 = this.FR;
                i5 = this.blockSize - 1;
                int i7 = i2 + 1;
                encryptByte = encryptByte(bArr[i + 1], this.blockSize - 1);
                bArr2[i7] = encryptByte;
                bArr3[i5] = encryptByte;
                this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                while (i3 < this.blockSize) {
                    bArr3 = this.FR;
                    i5 = i3 - 2;
                    i6 = i2 + i3;
                    encryptByte2 = encryptByte(bArr[i + i3], i5);
                    bArr2[i6] = encryptByte2;
                    bArr3[i5] = encryptByte2;
                    i3++;
                }
            } else {
                if (this.count == 0) {
                    this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                    while (i4 < this.blockSize) {
                        bArr3 = this.FR;
                        i5 = i2 + i4;
                        encryptByte = encryptByte(bArr[i + i4], i4);
                        bArr2[i5] = encryptByte;
                        bArr3[i4] = encryptByte;
                        i4++;
                    }
                } else if (this.count == this.blockSize) {
                    this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                    bArr2[i2] = encryptByte(bArr[i], 0);
                    bArr2[i2 + 1] = encryptByte(bArr[i + 1], 1);
                    System.arraycopy(this.FR, 2, this.FR, 0, this.blockSize - 2);
                    System.arraycopy(bArr2, i2, this.FR, this.blockSize - 2, 2);
                    this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                    while (i3 < this.blockSize) {
                        bArr3 = this.FR;
                        i5 = i3 - 2;
                        i6 = i2 + i3;
                        encryptByte2 = encryptByte(bArr[i + i3], i5);
                        bArr2[i6] = encryptByte2;
                        bArr3[i5] = encryptByte2;
                        i3++;
                    }
                }
                this.count += this.blockSize;
            }
            return this.blockSize;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    private byte encryptByte(byte b, int i) {
        return (byte) (b ^ this.FRE[i]);
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.cipher.getAlgorithmName());
        stringBuilder.append("/OpenPGPCFB");
        return stringBuilder.toString();
    }

    public int getBlockSize() {
        return this.cipher.getBlockSize();
    }

    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        this.forEncryption = z;
        reset();
        this.cipher.init(true, cipherParameters);
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        return this.forEncryption ? encryptBlock(bArr, i, bArr2, i2) : decryptBlock(bArr, i, bArr2, i2);
    }

    public void reset() {
        this.count = 0;
        System.arraycopy(this.IV, 0, this.FR, 0, this.FR.length);
        this.cipher.reset();
    }
}
