package org.bouncycastle.crypto.modes;

import java.io.ByteArrayOutputStream;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.macs.CBCBlockCipherMac;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.Arrays;

public class CCMBlockCipher implements AEADBlockCipher {
    private ExposedByteArrayOutputStream associatedText = new ExposedByteArrayOutputStream();
    private int blockSize;
    private BlockCipher cipher;
    private ExposedByteArrayOutputStream data = new ExposedByteArrayOutputStream();
    private boolean forEncryption;
    private byte[] initialAssociatedText;
    private CipherParameters keyParam;
    private byte[] macBlock;
    private int macSize;
    private byte[] nonce;

    private class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
        public byte[] getBuffer() {
            return this.buf;
        }
    }

    public CCMBlockCipher(BlockCipher blockCipher) {
        this.cipher = blockCipher;
        this.blockSize = blockCipher.getBlockSize();
        this.macBlock = new byte[this.blockSize];
        if (this.blockSize != 16) {
            throw new IllegalArgumentException("cipher required with a block size of 16.");
        }
    }

    private int calculateMac(byte[] bArr, int i, int i2, byte[] bArr2) {
        Mac cBCBlockCipherMac = new CBCBlockCipherMac(this.cipher, this.macSize * 8);
        cBCBlockCipherMac.init(this.keyParam);
        Object obj = new byte[16];
        if (hasAssociatedText()) {
            obj[0] = (byte) (obj[0] | 64);
        }
        int i3 = 2;
        obj[0] = (byte) (obj[0] | ((((cBCBlockCipherMac.getMacSize() - 2) / 2) & 7) << 3));
        obj[0] = (byte) (obj[0] | (((15 - this.nonce.length) - 1) & 7));
        System.arraycopy(this.nonce, 0, obj, 1, this.nonce.length);
        int i4 = i2;
        int i5 = 1;
        while (i4 > 0) {
            obj[obj.length - i5] = (byte) (i4 & 255);
            i4 >>>= 8;
            i5++;
        }
        cBCBlockCipherMac.update(obj, 0, obj.length);
        if (hasAssociatedText()) {
            int associatedTextLength = getAssociatedTextLength();
            if (associatedTextLength < CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB) {
                cBCBlockCipherMac.update((byte) (associatedTextLength >> 8));
                cBCBlockCipherMac.update((byte) associatedTextLength);
            } else {
                cBCBlockCipherMac.update((byte) -1);
                cBCBlockCipherMac.update((byte) -2);
                cBCBlockCipherMac.update((byte) (associatedTextLength >> 24));
                cBCBlockCipherMac.update((byte) (associatedTextLength >> 16));
                cBCBlockCipherMac.update((byte) (associatedTextLength >> 8));
                cBCBlockCipherMac.update((byte) associatedTextLength);
                i3 = 6;
            }
            if (this.initialAssociatedText != null) {
                cBCBlockCipherMac.update(this.initialAssociatedText, 0, this.initialAssociatedText.length);
            }
            if (this.associatedText.size() > 0) {
                cBCBlockCipherMac.update(this.associatedText.getBuffer(), 0, this.associatedText.size());
            }
            i3 = (i3 + associatedTextLength) % 16;
            if (i3 != 0) {
                while (i3 != 16) {
                    cBCBlockCipherMac.update((byte) 0);
                    i3++;
                }
            }
        }
        cBCBlockCipherMac.update(bArr, i, i2);
        return cBCBlockCipherMac.doFinal(bArr2, 0);
    }

    private int getAssociatedTextLength() {
        return this.associatedText.size() + (this.initialAssociatedText == null ? 0 : this.initialAssociatedText.length);
    }

    private boolean hasAssociatedText() {
        return getAssociatedTextLength() > 0;
    }

    public int doFinal(byte[] bArr, int i) throws IllegalStateException, InvalidCipherTextException {
        int processPacket = processPacket(this.data.getBuffer(), 0, this.data.size(), bArr, i);
        reset();
        return processPacket;
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.cipher.getAlgorithmName());
        stringBuilder.append("/CCM");
        return stringBuilder.toString();
    }

    public byte[] getMac() {
        Object obj = new byte[this.macSize];
        System.arraycopy(this.macBlock, 0, obj, 0, obj.length);
        return obj;
    }

    public int getOutputSize(int i) {
        i += this.data.size();
        return this.forEncryption ? i + this.macSize : i < this.macSize ? 0 : i - this.macSize;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    public int getUpdateOutputSize(int i) {
        return 0;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        CipherParameters key;
        this.forEncryption = z;
        if (cipherParameters instanceof AEADParameters) {
            AEADParameters aEADParameters = (AEADParameters) cipherParameters;
            this.nonce = aEADParameters.getNonce();
            this.initialAssociatedText = aEADParameters.getAssociatedText();
            this.macSize = aEADParameters.getMacSize() / 8;
            key = aEADParameters.getKey();
        } else if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            this.nonce = parametersWithIV.getIV();
            this.initialAssociatedText = null;
            this.macSize = this.macBlock.length / 2;
            key = parametersWithIV.getParameters();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid parameters passed to CCM: ");
            stringBuilder.append(cipherParameters.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        if (key != null) {
            this.keyParam = key;
        }
        if (this.nonce == null || this.nonce.length < 7 || this.nonce.length > 13) {
            throw new IllegalArgumentException("nonce must have length from 7 to 13 octets");
        }
        reset();
    }

    public void processAADByte(byte b) {
        this.associatedText.write(b);
    }

    public void processAADBytes(byte[] bArr, int i, int i2) {
        this.associatedText.write(bArr, i, i2);
    }

    public int processByte(byte b, byte[] bArr, int i) throws DataLengthException, IllegalStateException {
        this.data.write(b);
        return 0;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException, IllegalStateException {
        if (bArr.length >= i + i2) {
            this.data.write(bArr, i, i2);
            return 0;
        }
        throw new DataLengthException("Input buffer too short");
    }

    public int processPacket(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalStateException, InvalidCipherTextException, DataLengthException {
        if (this.keyParam != null) {
            int length = 15 - this.nonce.length;
            if (length >= 4 || i2 < (1 << (8 * length))) {
                Object obj = new byte[this.blockSize];
                obj[0] = (byte) ((length - 1) & 7);
                System.arraycopy(this.nonce, 0, obj, 1, this.nonce.length);
                BlockCipher sICBlockCipher = new SICBlockCipher(this.cipher);
                sICBlockCipher.init(this.forEncryption, new ParametersWithIV(this.keyParam, obj));
                int i4;
                int i5;
                if (this.forEncryption) {
                    i4 = this.macSize + i2;
                    if (bArr2.length >= i4 + i3) {
                        calculateMac(bArr, i, i2, this.macBlock);
                        Object obj2 = new byte[this.blockSize];
                        sICBlockCipher.processBlock(this.macBlock, 0, obj2, 0);
                        i5 = i;
                        int i6 = i3;
                        while (true) {
                            int i7 = i + i2;
                            if (i5 < i7 - this.blockSize) {
                                sICBlockCipher.processBlock(bArr, i5, bArr2, i6);
                                i6 += this.blockSize;
                                i5 += this.blockSize;
                            } else {
                                Object obj3 = new byte[this.blockSize];
                                i7 -= i5;
                                System.arraycopy(bArr, i5, obj3, 0, i7);
                                sICBlockCipher.processBlock(obj3, 0, obj3, 0);
                                System.arraycopy(obj3, 0, bArr2, i6, i7);
                                System.arraycopy(obj2, 0, bArr2, i3 + i2, this.macSize);
                                return i4;
                            }
                        }
                    }
                    throw new OutputLengthException("Output buffer too short.");
                } else if (i2 >= this.macSize) {
                    i4 = i2 - this.macSize;
                    if (bArr2.length >= i4 + i3) {
                        int i8;
                        i2 = i + i4;
                        System.arraycopy(bArr, i2, this.macBlock, 0, this.macSize);
                        sICBlockCipher.processBlock(this.macBlock, 0, this.macBlock, 0);
                        for (i8 = this.macSize; i8 != this.macBlock.length; i8++) {
                            this.macBlock[i8] = (byte) 0;
                        }
                        i8 = i;
                        i5 = i3;
                        while (i8 < i2 - this.blockSize) {
                            sICBlockCipher.processBlock(bArr, i8, bArr2, i5);
                            i5 += this.blockSize;
                            i8 += this.blockSize;
                        }
                        Object obj4 = new byte[this.blockSize];
                        i = i4 - (i8 - i);
                        System.arraycopy(bArr, i8, obj4, 0, i);
                        sICBlockCipher.processBlock(obj4, 0, obj4, 0);
                        System.arraycopy(obj4, 0, bArr2, i5, i);
                        bArr = new byte[this.blockSize];
                        calculateMac(bArr2, i3, i4, bArr);
                        if (Arrays.constantTimeAreEqual(this.macBlock, bArr)) {
                            return i4;
                        }
                        throw new InvalidCipherTextException("mac check in CCM failed");
                    }
                    throw new OutputLengthException("Output buffer too short.");
                } else {
                    throw new InvalidCipherTextException("data too short");
                }
            }
            throw new IllegalStateException("CCM packet too large for choice of q.");
        }
        throw new IllegalStateException("CCM cipher unitialized.");
    }

    public byte[] processPacket(byte[] bArr, int i, int i2) throws IllegalStateException, InvalidCipherTextException {
        int i3;
        if (this.forEncryption) {
            i3 = this.macSize + i2;
        } else if (i2 >= this.macSize) {
            i3 = i2 - this.macSize;
        } else {
            throw new InvalidCipherTextException("data too short");
        }
        byte[] bArr2 = new byte[i3];
        processPacket(bArr, i, i2, bArr2, 0);
        return bArr2;
    }

    public void reset() {
        this.cipher.reset();
        this.associatedText.reset();
        this.data.reset();
    }
}
