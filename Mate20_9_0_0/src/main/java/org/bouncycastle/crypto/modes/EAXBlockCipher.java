package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;

public class EAXBlockCipher implements AEADBlockCipher {
    private static final byte cTAG = (byte) 2;
    private static final byte hTAG = (byte) 1;
    private static final byte nTAG = (byte) 0;
    private byte[] associatedTextMac = new byte[this.mac.getMacSize()];
    private int blockSize;
    private byte[] bufBlock;
    private int bufOff;
    private SICBlockCipher cipher;
    private boolean cipherInitialized;
    private boolean forEncryption;
    private byte[] initialAssociatedText;
    private Mac mac;
    private byte[] macBlock = new byte[this.blockSize];
    private int macSize;
    private byte[] nonceMac = new byte[this.mac.getMacSize()];

    public EAXBlockCipher(BlockCipher blockCipher) {
        this.blockSize = blockCipher.getBlockSize();
        this.mac = new CMac(blockCipher);
        this.cipher = new SICBlockCipher(blockCipher);
    }

    private void calculateMac() {
        byte[] bArr = new byte[this.blockSize];
        int i = 0;
        this.mac.doFinal(bArr, 0);
        while (i < this.macBlock.length) {
            this.macBlock[i] = (byte) ((this.nonceMac[i] ^ this.associatedTextMac[i]) ^ bArr[i]);
            i++;
        }
    }

    private void initCipher() {
        if (!this.cipherInitialized) {
            this.cipherInitialized = true;
            this.mac.doFinal(this.associatedTextMac, 0);
            byte[] bArr = new byte[this.blockSize];
            bArr[this.blockSize - 1] = cTAG;
            this.mac.update(bArr, 0, this.blockSize);
        }
    }

    private int process(byte b, byte[] bArr, int i) {
        byte[] bArr2 = this.bufBlock;
        int i2 = this.bufOff;
        this.bufOff = i2 + 1;
        bArr2[i2] = b;
        if (this.bufOff != this.bufBlock.length) {
            return 0;
        }
        if (bArr.length >= this.blockSize + i) {
            int processBlock;
            if (this.forEncryption) {
                processBlock = this.cipher.processBlock(this.bufBlock, 0, bArr, i);
                this.mac.update(bArr, i, this.blockSize);
            } else {
                this.mac.update(this.bufBlock, 0, this.blockSize);
                processBlock = this.cipher.processBlock(this.bufBlock, 0, bArr, i);
            }
            this.bufOff = 0;
            if (!this.forEncryption) {
                System.arraycopy(this.bufBlock, this.blockSize, this.bufBlock, 0, this.macSize);
                this.bufOff = this.macSize;
            }
            return processBlock;
        }
        throw new OutputLengthException("Output buffer is too short");
    }

    private void reset(boolean z) {
        this.cipher.reset();
        this.mac.reset();
        this.bufOff = 0;
        Arrays.fill(this.bufBlock, (byte) 0);
        if (z) {
            Arrays.fill(this.macBlock, (byte) 0);
        }
        byte[] bArr = new byte[this.blockSize];
        bArr[this.blockSize - 1] = hTAG;
        this.mac.update(bArr, 0, this.blockSize);
        this.cipherInitialized = false;
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }

    private boolean verifyMac(byte[] bArr, int i) {
        int i2 = 0;
        int i3 = i2;
        while (i2 < this.macSize) {
            i3 |= this.macBlock[i2] ^ bArr[i + i2];
            i2++;
        }
        return i3 == 0;
    }

    public int doFinal(byte[] bArr, int i) throws IllegalStateException, InvalidCipherTextException {
        initCipher();
        int i2 = this.bufOff;
        byte[] bArr2 = new byte[this.bufBlock.length];
        this.bufOff = 0;
        if (this.forEncryption) {
            int i3 = i + i2;
            if (bArr.length >= this.macSize + i3) {
                this.cipher.processBlock(this.bufBlock, 0, bArr2, 0);
                System.arraycopy(bArr2, 0, bArr, i, i2);
                this.mac.update(bArr2, 0, i2);
                calculateMac();
                System.arraycopy(this.macBlock, 0, bArr, i3, this.macSize);
                reset(false);
                return i2 + this.macSize;
            }
            throw new OutputLengthException("Output buffer too short");
        } else if (i2 < this.macSize) {
            throw new InvalidCipherTextException("data too short");
        } else if (bArr.length >= (i + i2) - this.macSize) {
            if (i2 > this.macSize) {
                this.mac.update(this.bufBlock, 0, i2 - this.macSize);
                this.cipher.processBlock(this.bufBlock, 0, bArr2, 0);
                System.arraycopy(bArr2, 0, bArr, i, i2 - this.macSize);
            }
            calculateMac();
            if (verifyMac(this.bufBlock, i2 - this.macSize)) {
                reset(false);
                return i2 - this.macSize;
            }
            throw new InvalidCipherTextException("mac check in EAX failed");
        } else {
            throw new OutputLengthException("Output buffer too short");
        }
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.cipher.getUnderlyingCipher().getAlgorithmName());
        stringBuilder.append("/EAX");
        return stringBuilder.toString();
    }

    public int getBlockSize() {
        return this.cipher.getBlockSize();
    }

    public byte[] getMac() {
        byte[] bArr = new byte[this.macSize];
        System.arraycopy(this.macBlock, 0, bArr, 0, this.macSize);
        return bArr;
    }

    public int getOutputSize(int i) {
        i += this.bufOff;
        return this.forEncryption ? i + this.macSize : i < this.macSize ? 0 : i - this.macSize;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.cipher.getUnderlyingCipher();
    }

    public int getUpdateOutputSize(int i) {
        i += this.bufOff;
        if (!this.forEncryption) {
            if (i < this.macSize) {
                return 0;
            }
            i -= this.macSize;
        }
        return i - (i % this.blockSize);
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        byte[] nonce;
        this.forEncryption = z;
        if (cipherParameters instanceof AEADParameters) {
            AEADParameters aEADParameters = (AEADParameters) cipherParameters;
            nonce = aEADParameters.getNonce();
            this.initialAssociatedText = aEADParameters.getAssociatedText();
            this.macSize = aEADParameters.getMacSize() / 8;
            cipherParameters = aEADParameters.getKey();
        } else if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            nonce = parametersWithIV.getIV();
            this.initialAssociatedText = null;
            this.macSize = this.mac.getMacSize() / 2;
            cipherParameters = parametersWithIV.getParameters();
        } else {
            throw new IllegalArgumentException("invalid parameters passed to EAX");
        }
        this.bufBlock = new byte[(z ? this.blockSize : this.blockSize + this.macSize)];
        byte[] bArr = new byte[this.blockSize];
        this.mac.init(cipherParameters);
        bArr[this.blockSize - 1] = (byte) 0;
        this.mac.update(bArr, 0, this.blockSize);
        this.mac.update(nonce, 0, nonce.length);
        this.mac.doFinal(this.nonceMac, 0);
        this.cipher.init(true, new ParametersWithIV(null, this.nonceMac));
        reset();
    }

    public void processAADByte(byte b) {
        if (this.cipherInitialized) {
            throw new IllegalStateException("AAD data cannot be added after encryption/decryption processing has begun.");
        }
        this.mac.update(b);
    }

    public void processAADBytes(byte[] bArr, int i, int i2) {
        if (this.cipherInitialized) {
            throw new IllegalStateException("AAD data cannot be added after encryption/decryption processing has begun.");
        }
        this.mac.update(bArr, i, i2);
    }

    public int processByte(byte b, byte[] bArr, int i) throws DataLengthException {
        initCipher();
        return process(b, bArr, i);
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
        initCipher();
        if (bArr.length >= i + i2) {
            int i4 = 0;
            int i5 = 0;
            while (i4 != i2) {
                i5 += process(bArr[i + i4], bArr2, i3 + i5);
                i4++;
            }
            return i5;
        }
        throw new DataLengthException("Input buffer too short");
    }

    public void reset() {
        reset(true);
    }
}
