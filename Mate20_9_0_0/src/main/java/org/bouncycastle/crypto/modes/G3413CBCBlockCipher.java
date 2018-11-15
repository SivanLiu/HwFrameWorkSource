package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;

public class G3413CBCBlockCipher implements BlockCipher {
    private byte[] R;
    private byte[] R_init;
    private int blockSize;
    private BlockCipher cipher;
    private boolean forEncryption;
    private boolean initialized = false;
    private int m;

    public G3413CBCBlockCipher(BlockCipher blockCipher) {
        this.blockSize = blockCipher.getBlockSize();
        this.cipher = blockCipher;
    }

    private int decrypt(byte[] bArr, int i, byte[] bArr2, int i2) {
        byte[] MSB = GOST3413CipherUtil.MSB(this.R, this.blockSize);
        bArr = GOST3413CipherUtil.copyFromInput(bArr, this.blockSize, i);
        byte[] bArr3 = new byte[bArr.length];
        this.cipher.processBlock(bArr, 0, bArr3, 0);
        Object sum = GOST3413CipherUtil.sum(bArr3, MSB);
        System.arraycopy(sum, 0, bArr2, i2, sum.length);
        if (bArr2.length > i2 + sum.length) {
            generateR(bArr);
        }
        return sum.length;
    }

    private int encrypt(byte[] bArr, int i, byte[] bArr2, int i2) {
        bArr = GOST3413CipherUtil.sum(GOST3413CipherUtil.copyFromInput(bArr, this.blockSize, i), GOST3413CipherUtil.MSB(this.R, this.blockSize));
        Object obj = new byte[bArr.length];
        this.cipher.processBlock(bArr, 0, obj, 0);
        System.arraycopy(obj, 0, bArr2, i2, obj.length);
        if (bArr2.length > i2 + bArr.length) {
            generateR(obj);
        }
        return obj.length;
    }

    private void generateR(byte[] bArr) {
        Object LSB = GOST3413CipherUtil.LSB(this.R, this.m - this.blockSize);
        System.arraycopy(LSB, 0, this.R, 0, LSB.length);
        System.arraycopy(bArr, 0, this.R, LSB.length, this.m - LSB.length);
    }

    private void initArrays() {
        this.R = new byte[this.m];
        this.R_init = new byte[this.m];
    }

    private void setupDefaultParams() {
        this.m = this.blockSize;
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.cipher.getAlgorithmName());
        stringBuilder.append("/CBC");
        return stringBuilder.toString();
    }

    public int getBlockSize() {
        return this.blockSize;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        BlockCipher blockCipher;
        this.forEncryption = z;
        if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            byte[] iv = parametersWithIV.getIV();
            if (iv.length >= this.blockSize) {
                this.m = iv.length;
                initArrays();
                this.R_init = Arrays.clone(iv);
                System.arraycopy(this.R_init, 0, this.R, 0, this.R_init.length);
                if (parametersWithIV.getParameters() != null) {
                    blockCipher = this.cipher;
                    cipherParameters = parametersWithIV.getParameters();
                }
                this.initialized = true;
            }
            throw new IllegalArgumentException("Parameter m must blockSize <= m");
        }
        setupDefaultParams();
        initArrays();
        System.arraycopy(this.R_init, 0, this.R, 0, this.R_init.length);
        if (cipherParameters != null) {
            blockCipher = this.cipher;
        }
        this.initialized = true;
        blockCipher.init(z, cipherParameters);
        this.initialized = true;
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        return this.forEncryption ? encrypt(bArr, i, bArr2, i2) : decrypt(bArr, i, bArr2, i2);
    }

    public void reset() {
        if (this.initialized) {
            System.arraycopy(this.R_init, 0, this.R, 0, this.R_init.length);
            this.cipher.reset();
        }
    }
}
