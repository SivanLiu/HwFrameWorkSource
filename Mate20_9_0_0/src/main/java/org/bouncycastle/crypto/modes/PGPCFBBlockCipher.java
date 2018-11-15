package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class PGPCFBBlockCipher implements BlockCipher {
    private byte[] FR = new byte[this.blockSize];
    private byte[] FRE = new byte[this.blockSize];
    private byte[] IV = new byte[this.blockSize];
    private int blockSize;
    private BlockCipher cipher;
    private int count;
    private boolean forEncryption;
    private boolean inlineIv;
    private byte[] tmp = new byte[this.blockSize];

    public PGPCFBBlockCipher(BlockCipher blockCipher, boolean z) {
        this.cipher = blockCipher;
        this.inlineIv = z;
        this.blockSize = blockCipher.getBlockSize();
    }

    private int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.blockSize + i > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (this.blockSize + i2 <= bArr2.length) {
            int i3 = 0;
            this.cipher.processBlock(this.FR, 0, this.FRE, 0);
            for (int i4 = 0; i4 < this.blockSize; i4++) {
                bArr2[i2 + i4] = encryptByte(bArr[i + i4], i4);
            }
            while (i3 < this.blockSize) {
                this.FR[i3] = bArr[i + i3];
                i3++;
            }
            return this.blockSize;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    private int decryptBlockWithIV(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        int i3;
        if (this.blockSize + i > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (this.blockSize + i2 > bArr2.length) {
            throw new OutputLengthException("output buffer too short");
        } else if (this.count == 0) {
            for (int i4 = 0; i4 < this.blockSize; i4++) {
                this.FR[i4] = bArr[i + i4];
            }
            this.cipher.processBlock(this.FR, 0, this.FRE, 0);
            this.count += this.blockSize;
            return 0;
        } else if (this.count == this.blockSize) {
            System.arraycopy(bArr, i, this.tmp, 0, this.blockSize);
            System.arraycopy(this.FR, 2, this.FR, 0, this.blockSize - 2);
            this.FR[this.blockSize - 2] = this.tmp[0];
            this.FR[this.blockSize - 1] = this.tmp[1];
            this.cipher.processBlock(this.FR, 0, this.FRE, 0);
            for (i3 = 0; i3 < this.blockSize - 2; i3++) {
                bArr2[i2 + i3] = encryptByte(this.tmp[i3 + 2], i3);
            }
            System.arraycopy(this.tmp, 2, this.FR, 0, this.blockSize - 2);
            this.count += 2;
            return this.blockSize - 2;
        } else {
            if (this.count >= this.blockSize + 2) {
                System.arraycopy(bArr, i, this.tmp, 0, this.blockSize);
                bArr2[i2 + 0] = encryptByte(this.tmp[0], this.blockSize - 2);
                bArr2[i2 + 1] = encryptByte(this.tmp[1], this.blockSize - 1);
                System.arraycopy(this.tmp, 0, this.FR, this.blockSize - 2, 2);
                this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                for (i3 = 0; i3 < this.blockSize - 2; i3++) {
                    bArr2[(i2 + i3) + 2] = encryptByte(this.tmp[i3 + 2], i3);
                }
                System.arraycopy(this.tmp, 2, this.FR, 0, this.blockSize - 2);
            }
            return this.blockSize;
        }
    }

    private int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.blockSize + i > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (this.blockSize + i2 <= bArr2.length) {
            int i3 = 0;
            this.cipher.processBlock(this.FR, 0, this.FRE, 0);
            for (int i4 = 0; i4 < this.blockSize; i4++) {
                bArr2[i2 + i4] = encryptByte(bArr[i + i4], i4);
            }
            while (i3 < this.blockSize) {
                this.FR[i3] = bArr2[i2 + i3];
                i3++;
            }
            return this.blockSize;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    private int encryptBlockWithIV(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        int i3;
        if (this.blockSize + i > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (this.count != 0) {
            if (this.count >= this.blockSize + 2) {
                if (this.blockSize + i2 <= bArr2.length) {
                    this.cipher.processBlock(this.FR, 0, this.FRE, 0);
                    for (i3 = 0; i3 < this.blockSize; i3++) {
                        bArr2[i2 + i3] = encryptByte(bArr[i + i3], i3);
                    }
                    System.arraycopy(bArr2, i2, this.FR, 0, this.blockSize);
                } else {
                    throw new OutputLengthException("output buffer too short");
                }
            }
            return this.blockSize;
        } else if (((this.blockSize * 2) + i2) + 2 <= bArr2.length) {
            this.cipher.processBlock(this.FR, 0, this.FRE, 0);
            for (i3 = 0; i3 < this.blockSize; i3++) {
                bArr2[i2 + i3] = encryptByte(this.IV[i3], i3);
            }
            System.arraycopy(bArr2, i2, this.FR, 0, this.blockSize);
            this.cipher.processBlock(this.FR, 0, this.FRE, 0);
            bArr2[this.blockSize + i2] = encryptByte(this.IV[this.blockSize - 2], 0);
            bArr2[(this.blockSize + i2) + 1] = encryptByte(this.IV[this.blockSize - 1], 1);
            System.arraycopy(bArr2, i2 + 2, this.FR, 0, this.blockSize);
            this.cipher.processBlock(this.FR, 0, this.FRE, 0);
            for (i3 = 0; i3 < this.blockSize; i3++) {
                bArr2[((this.blockSize + i2) + 2) + i3] = encryptByte(bArr[i + i3], i3);
            }
            System.arraycopy(bArr2, (i2 + this.blockSize) + 2, this.FR, 0, this.blockSize);
            this.count += (this.blockSize * 2) + 2;
            return (this.blockSize * 2) + 2;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    private byte encryptByte(byte b, int i) {
        return (byte) (b ^ this.FRE[i]);
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder;
        String str;
        if (this.inlineIv) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.cipher.getAlgorithmName());
            str = "/PGPCFBwithIV";
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.cipher.getAlgorithmName());
            str = "/PGPCFB";
        }
        stringBuilder.append(str);
        return stringBuilder.toString();
    }

    public int getBlockSize() {
        return this.cipher.getBlockSize();
    }

    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        BlockCipher blockCipher;
        this.forEncryption = z;
        if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            Object iv = parametersWithIV.getIV();
            if (iv.length < this.IV.length) {
                System.arraycopy(iv, 0, this.IV, this.IV.length - iv.length, iv.length);
                for (int i = 0; i < this.IV.length - iv.length; i++) {
                    this.IV[i] = (byte) 0;
                }
            } else {
                System.arraycopy(iv, 0, this.IV, 0, this.IV.length);
            }
            reset();
            blockCipher = this.cipher;
            cipherParameters = parametersWithIV.getParameters();
        } else {
            reset();
            blockCipher = this.cipher;
        }
        blockCipher.init(true, cipherParameters);
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        return this.inlineIv ? this.forEncryption ? encryptBlockWithIV(bArr, i, bArr2, i2) : decryptBlockWithIV(bArr, i, bArr2, i2) : this.forEncryption ? encryptBlock(bArr, i, bArr2, i2) : decryptBlock(bArr, i, bArr2, i2);
    }

    public void reset() {
        this.count = 0;
        for (int i = 0; i != this.FR.length; i++) {
            if (this.inlineIv) {
                this.FR[i] = (byte) 0;
            } else {
                this.FR[i] = this.IV[i];
            }
        }
        this.cipher.reset();
    }
}
