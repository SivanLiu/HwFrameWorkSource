package org.bouncycastle.crypto.modes;

import java.io.ByteArrayOutputStream;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.modes.kgcm.KGCMMultiplier;
import org.bouncycastle.crypto.modes.kgcm.Tables16kKGCMMultiplier_512;
import org.bouncycastle.crypto.modes.kgcm.Tables4kKGCMMultiplier_128;
import org.bouncycastle.crypto.modes.kgcm.Tables8kKGCMMultiplier_256;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public class KGCMBlockCipher implements AEADBlockCipher {
    private static final int MIN_MAC_BITS = 64;
    private ExposedByteArrayOutputStream associatedText = new ExposedByteArrayOutputStream();
    private long[] b;
    private final int blockSize;
    private BufferedBlockCipher ctrEngine;
    private ExposedByteArrayOutputStream data = new ExposedByteArrayOutputStream();
    private BlockCipher engine;
    private boolean forEncryption;
    private byte[] initialAssociatedText;
    private byte[] iv;
    private byte[] macBlock;
    private int macSize;
    private KGCMMultiplier multiplier;

    private class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
        public byte[] getBuffer() {
            return this.buf;
        }
    }

    public KGCMBlockCipher(BlockCipher blockCipher) {
        this.engine = blockCipher;
        this.ctrEngine = new BufferedBlockCipher(new KCTRBlockCipher(this.engine));
        this.macSize = -1;
        this.blockSize = this.engine.getBlockSize();
        this.initialAssociatedText = new byte[this.blockSize];
        this.iv = new byte[this.blockSize];
        this.multiplier = createDefaultMultiplier(this.blockSize);
        this.b = new long[(this.blockSize >>> 3)];
        this.macBlock = null;
    }

    private void calculateMac(byte[] bArr, int i, int i2, int i3) {
        int i4 = i + i2;
        while (i < i4) {
            xorWithInput(this.b, bArr, i);
            this.multiplier.multiplyH(this.b);
            i += this.blockSize;
        }
        long j = (((long) i3) & BodyPartID.bodyIdMax) << 3;
        long j2 = (BodyPartID.bodyIdMax & ((long) i2)) << 3;
        long[] jArr = this.b;
        jArr[0] = j ^ jArr[0];
        long[] jArr2 = this.b;
        i = this.blockSize >>> 4;
        jArr2[i] = j2 ^ jArr2[i];
        this.macBlock = Pack.longToLittleEndian(this.b);
        this.engine.processBlock(this.macBlock, 0, this.macBlock, 0);
    }

    private static KGCMMultiplier createDefaultMultiplier(int i) {
        if (i == 16) {
            return new Tables4kKGCMMultiplier_128();
        }
        if (i == 32) {
            return new Tables8kKGCMMultiplier_256();
        }
        if (i == 64) {
            return new Tables16kKGCMMultiplier_512();
        }
        throw new IllegalArgumentException("Only 128, 256, and 512 -bit block sizes supported");
    }

    private void processAAD(byte[] bArr, int i, int i2) {
        i2 += i;
        while (i < i2) {
            xorWithInput(this.b, bArr, i);
            this.multiplier.multiplyH(this.b);
            i += this.blockSize;
        }
    }

    private static void xorWithInput(long[] jArr, byte[] bArr, int i) {
        for (int i2 = 0; i2 < jArr.length; i2++) {
            jArr[i2] = jArr[i2] ^ Pack.littleEndianToLong(bArr, i);
            i += 8;
        }
    }

    public int doFinal(byte[] bArr, int i) throws IllegalStateException, InvalidCipherTextException {
        int size = this.data.size();
        if (this.forEncryption || size >= this.macSize) {
            int processBytes;
            byte[] bArr2 = new byte[this.blockSize];
            this.engine.processBlock(bArr2, 0, bArr2, 0);
            long[] jArr = new long[(this.blockSize >>> 3)];
            Pack.littleEndianToLong(bArr2, 0, jArr);
            this.multiplier.init(jArr);
            Arrays.fill(bArr2, (byte) 0);
            Arrays.fill(jArr, 0);
            int size2 = this.associatedText.size();
            if (size2 > 0) {
                processAAD(this.associatedText.getBuffer(), 0, size2);
            }
            if (!this.forEncryption) {
                int i2 = size - this.macSize;
                if (bArr.length - i >= i2) {
                    calculateMac(this.data.getBuffer(), 0, i2, size2);
                    processBytes = this.ctrEngine.processBytes(this.data.getBuffer(), 0, i2, bArr, i);
                    processBytes += this.ctrEngine.doFinal(bArr, i + processBytes);
                } else {
                    throw new OutputLengthException("Output buffer too short");
                }
            } else if ((bArr.length - i) - this.macSize >= size) {
                processBytes = this.ctrEngine.processBytes(this.data.getBuffer(), 0, size, bArr, i);
                processBytes += this.ctrEngine.doFinal(bArr, i + processBytes);
                calculateMac(bArr, i, size, size2);
            } else {
                throw new OutputLengthException("Output buffer too short");
            }
            if (this.macBlock == null) {
                throw new IllegalStateException("mac is not calculated");
            } else if (this.forEncryption) {
                System.arraycopy(this.macBlock, 0, bArr, i + processBytes, this.macSize);
                reset();
                return processBytes + this.macSize;
            } else {
                Object obj = new byte[this.macSize];
                System.arraycopy(this.data.getBuffer(), size - this.macSize, obj, 0, this.macSize);
                Object obj2 = new byte[this.macSize];
                System.arraycopy(this.macBlock, 0, obj2, 0, this.macSize);
                if (Arrays.constantTimeAreEqual(obj, obj2)) {
                    reset();
                    return processBytes;
                }
                throw new InvalidCipherTextException("mac verification failed");
            }
        }
        throw new InvalidCipherTextException("data too short");
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.engine.getAlgorithmName());
        stringBuilder.append("/KGCM");
        return stringBuilder.toString();
    }

    public byte[] getMac() {
        Object obj = new byte[this.macSize];
        System.arraycopy(this.macBlock, 0, obj, 0, this.macSize);
        return obj;
    }

    public int getOutputSize(int i) {
        i += this.data.size();
        return this.forEncryption ? i + this.macSize : i < this.macSize ? 0 : i - this.macSize;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    public int getUpdateOutputSize(int i) {
        return 0;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        CipherParameters key;
        this.forEncryption = z;
        Object nonce;
        int length;
        if (cipherParameters instanceof AEADParameters) {
            AEADParameters aEADParameters = (AEADParameters) cipherParameters;
            nonce = aEADParameters.getNonce();
            length = this.iv.length - nonce.length;
            Arrays.fill(this.iv, (byte) 0);
            System.arraycopy(nonce, 0, this.iv, length, nonce.length);
            this.initialAssociatedText = aEADParameters.getAssociatedText();
            int macSize = aEADParameters.getMacSize();
            if (macSize < 64 || macSize > (this.blockSize << 3) || (macSize & 7) != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid value for MAC size: ");
                stringBuilder.append(macSize);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.macSize = macSize >>> 3;
            key = aEADParameters.getKey();
            if (this.initialAssociatedText != null) {
                processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
            }
        } else if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            nonce = parametersWithIV.getIV();
            length = this.iv.length - nonce.length;
            Arrays.fill(this.iv, (byte) 0);
            System.arraycopy(nonce, 0, this.iv, length, nonce.length);
            this.initialAssociatedText = null;
            this.macSize = this.blockSize;
            key = (KeyParameter) parametersWithIV.getParameters();
        } else {
            throw new IllegalArgumentException("Invalid parameter passed");
        }
        this.macBlock = new byte[this.blockSize];
        this.ctrEngine.init(true, new ParametersWithIV(key, this.iv));
        this.engine.init(true, key);
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
        throw new DataLengthException("input buffer too short");
    }

    public void reset() {
        Arrays.fill(this.b, 0);
        this.engine.reset();
        this.data.reset();
        this.associatedText.reset();
        if (this.initialAssociatedText != null) {
            processAADBytes(this.initialAssociatedText, 0, this.initialAssociatedText.length);
        }
    }
}
