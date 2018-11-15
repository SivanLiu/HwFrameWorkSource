package org.bouncycastle.crypto.encodings;

import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.util.Arrays;

public class OAEPEncoding implements AsymmetricBlockCipher {
    private byte[] defHash;
    private AsymmetricBlockCipher engine;
    private boolean forEncryption;
    private Digest mgf1Hash;
    private SecureRandom random;

    public OAEPEncoding(AsymmetricBlockCipher asymmetricBlockCipher) {
        this(asymmetricBlockCipher, DigestFactory.createSHA1(), null);
    }

    public OAEPEncoding(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest) {
        this(asymmetricBlockCipher, digest, null);
    }

    public OAEPEncoding(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, Digest digest2, byte[] bArr) {
        this.engine = asymmetricBlockCipher;
        this.mgf1Hash = digest2;
        this.defHash = new byte[digest.getDigestSize()];
        digest.reset();
        if (bArr != null) {
            digest.update(bArr, 0, bArr.length);
        }
        digest.doFinal(this.defHash, 0);
    }

    public OAEPEncoding(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, byte[] bArr) {
        this(asymmetricBlockCipher, digest, digest, bArr);
    }

    private void ItoOSP(int i, byte[] bArr) {
        bArr[0] = (byte) (i >>> 24);
        bArr[1] = (byte) (i >>> 16);
        bArr[2] = (byte) (i >>> 8);
        bArr[3] = (byte) (i >>> 0);
    }

    private byte[] maskGeneratorFunction1(byte[] bArr, int i, int i2, int i3) {
        Object obj = new byte[i3];
        Object obj2 = new byte[this.mgf1Hash.getDigestSize()];
        byte[] bArr2 = new byte[4];
        this.mgf1Hash.reset();
        int i4 = 0;
        while (i4 < i3 / obj2.length) {
            ItoOSP(i4, bArr2);
            this.mgf1Hash.update(bArr, i, i2);
            this.mgf1Hash.update(bArr2, 0, bArr2.length);
            this.mgf1Hash.doFinal(obj2, 0);
            System.arraycopy(obj2, 0, obj, obj2.length * i4, obj2.length);
            i4++;
        }
        if (obj2.length * i4 < i3) {
            ItoOSP(i4, bArr2);
            this.mgf1Hash.update(bArr, i, i2);
            this.mgf1Hash.update(bArr2, 0, bArr2.length);
            this.mgf1Hash.doFinal(obj2, 0);
            System.arraycopy(obj2, 0, obj, obj2.length * i4, obj.length - (i4 * obj2.length));
        }
        return obj;
    }

    public byte[] decodeBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        int i3;
        int length;
        Object processBlock = this.engine.processBlock(bArr, i, i2);
        byte[] bArr2 = new byte[this.engine.getOutputBlockSize()];
        System.arraycopy(processBlock, 0, bArr2, bArr2.length - processBlock.length, processBlock.length);
        int i4 = bArr2.length < (this.defHash.length * 2) + 1 ? 1 : 0;
        byte[] maskGeneratorFunction1 = maskGeneratorFunction1(bArr2, this.defHash.length, bArr2.length - this.defHash.length, this.defHash.length);
        for (i3 = 0; i3 != this.defHash.length; i3++) {
            bArr2[i3] = (byte) (bArr2[i3] ^ maskGeneratorFunction1[i3]);
        }
        maskGeneratorFunction1 = maskGeneratorFunction1(bArr2, 0, this.defHash.length, bArr2.length - this.defHash.length);
        for (i3 = this.defHash.length; i3 != bArr2.length; i3++) {
            bArr2[i3] = (byte) (bArr2[i3] ^ maskGeneratorFunction1[i3 - this.defHash.length]);
        }
        i2 = 0;
        i3 = i2;
        while (i2 != this.defHash.length) {
            if (this.defHash[i2] != bArr2[this.defHash.length + i2]) {
                i3 = 1;
            }
            i2++;
        }
        i2 = bArr2.length;
        for (length = 2 * this.defHash.length; length != bArr2.length; length++) {
            if (((bArr2[length] != (byte) 0 ? 1 : 0) & (i2 == bArr2.length ? 1 : 0)) != 0) {
                i2 = length;
            }
        }
        length = i2 > bArr2.length - 1 ? 1 : 0;
        int i5 = bArr2[i2] != (byte) 1 ? 1 : 0;
        i2++;
        if (((i4 | i3) | (length | i5)) == 0) {
            processBlock = new byte[(bArr2.length - i2)];
            System.arraycopy(bArr2, i2, processBlock, 0, processBlock.length);
            return processBlock;
        }
        Arrays.fill(bArr2, (byte) 0);
        throw new InvalidCipherTextException("data wrong");
    }

    public byte[] encodeBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (i2 <= getInputBlockSize()) {
            Object obj = new byte[((getInputBlockSize() + 1) + (2 * this.defHash.length))];
            System.arraycopy(bArr, i, obj, obj.length - i2, i2);
            obj[(obj.length - i2) - 1] = 1;
            System.arraycopy(this.defHash, 0, obj, this.defHash.length, this.defHash.length);
            Object obj2 = new byte[this.defHash.length];
            this.random.nextBytes(obj2);
            byte[] maskGeneratorFunction1 = maskGeneratorFunction1(obj2, 0, obj2.length, obj.length - this.defHash.length);
            for (i2 = this.defHash.length; i2 != obj.length; i2++) {
                obj[i2] = (byte) (obj[i2] ^ maskGeneratorFunction1[i2 - this.defHash.length]);
            }
            System.arraycopy(obj2, 0, obj, 0, this.defHash.length);
            bArr = maskGeneratorFunction1(obj, this.defHash.length, obj.length - this.defHash.length, this.defHash.length);
            for (i = 0; i != this.defHash.length; i++) {
                obj[i] = (byte) (obj[i] ^ bArr[i]);
            }
            return this.engine.processBlock(obj, 0, obj.length);
        }
        throw new DataLengthException("input data too long");
    }

    public int getInputBlockSize() {
        int inputBlockSize = this.engine.getInputBlockSize();
        return this.forEncryption ? (inputBlockSize - 1) - (2 * this.defHash.length) : inputBlockSize;
    }

    public int getOutputBlockSize() {
        int outputBlockSize = this.engine.getOutputBlockSize();
        return this.forEncryption ? outputBlockSize : (outputBlockSize - 1) - (2 * this.defHash.length);
    }

    public AsymmetricBlockCipher getUnderlyingCipher() {
        return this.engine;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.random = cipherParameters instanceof ParametersWithRandom ? ((ParametersWithRandom) cipherParameters).getRandom() : new SecureRandom();
        this.engine.init(z, cipherParameters);
        this.forEncryption = z;
    }

    public byte[] processBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        return this.forEncryption ? encodeBlock(bArr, i, i2) : decodeBlock(bArr, i, i2);
    }
}
