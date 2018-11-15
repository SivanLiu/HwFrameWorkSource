package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.ParametersWithSBox;

public class GCFBBlockCipher extends StreamBlockCipher {
    private static final byte[] C = new byte[]{(byte) 105, (byte) 0, (byte) 114, (byte) 34, (byte) 100, (byte) -55, (byte) 4, (byte) 35, (byte) -115, (byte) 58, (byte) -37, (byte) -106, (byte) 70, (byte) -23, (byte) 42, (byte) -60, (byte) 24, (byte) -2, (byte) -84, (byte) -108, (byte) 0, (byte) -19, (byte) 7, (byte) 18, (byte) -64, (byte) -122, (byte) -36, (byte) -62, (byte) -17, (byte) 76, (byte) -87, (byte) 43};
    private final CFBBlockCipher cfbEngine;
    private long counter = 0;
    private boolean forEncryption;
    private KeyParameter key;

    public GCFBBlockCipher(BlockCipher blockCipher) {
        super(blockCipher);
        this.cfbEngine = new CFBBlockCipher(blockCipher, blockCipher.getBlockSize() * 8);
    }

    protected byte calculateByte(byte b) {
        if (this.counter > 0 && this.counter % 1024 == 0) {
            BlockCipher underlyingCipher = this.cfbEngine.getUnderlyingCipher();
            underlyingCipher.init(false, this.key);
            byte[] bArr = new byte[32];
            underlyingCipher.processBlock(C, 0, bArr, 0);
            underlyingCipher.processBlock(C, 8, bArr, 8);
            underlyingCipher.processBlock(C, 16, bArr, 16);
            underlyingCipher.processBlock(C, 24, bArr, 24);
            this.key = new KeyParameter(bArr);
            underlyingCipher.init(true, this.key);
            bArr = this.cfbEngine.getCurrentIV();
            underlyingCipher.processBlock(bArr, 0, bArr, 0);
            this.cfbEngine.init(this.forEncryption, new ParametersWithIV(this.key, bArr));
        }
        this.counter++;
        return this.cfbEngine.calculateByte(b);
    }

    public String getAlgorithmName() {
        String algorithmName = this.cfbEngine.getAlgorithmName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(algorithmName.substring(0, algorithmName.indexOf(47)));
        stringBuilder.append("/G");
        stringBuilder.append(algorithmName.substring(algorithmName.indexOf(47) + 1));
        return stringBuilder.toString();
    }

    public int getBlockSize() {
        return this.cfbEngine.getBlockSize();
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        this.counter = 0;
        this.cfbEngine.init(z, cipherParameters);
        this.forEncryption = z;
        if (cipherParameters instanceof ParametersWithIV) {
            cipherParameters = ((ParametersWithIV) cipherParameters).getParameters();
        }
        if (cipherParameters instanceof ParametersWithRandom) {
            cipherParameters = ((ParametersWithRandom) cipherParameters).getParameters();
        }
        if (cipherParameters instanceof ParametersWithSBox) {
            cipherParameters = ((ParametersWithSBox) cipherParameters).getParameters();
        }
        this.key = (KeyParameter) cipherParameters;
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        processBytes(bArr, i, this.cfbEngine.getBlockSize(), bArr2, i2);
        return this.cfbEngine.getBlockSize();
    }

    public void reset() {
        this.counter = 0;
        this.cfbEngine.reset();
    }
}
