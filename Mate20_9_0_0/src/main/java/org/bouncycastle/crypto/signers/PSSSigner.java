package org.bouncycastle.crypto.signers;

import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSABlindingParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;

public class PSSSigner implements Signer {
    public static final byte TRAILER_IMPLICIT = (byte) -68;
    private byte[] block;
    private AsymmetricBlockCipher cipher;
    private Digest contentDigest;
    private int emBits;
    private int hLen;
    private byte[] mDash;
    private Digest mgfDigest;
    private int mgfhLen;
    private SecureRandom random;
    private int sLen;
    private boolean sSet;
    private byte[] salt;
    private byte trailer;

    public PSSSigner(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, int i) {
        this(asymmetricBlockCipher, digest, i, (byte) TRAILER_IMPLICIT);
    }

    public PSSSigner(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, int i, byte b) {
        this(asymmetricBlockCipher, digest, digest, i, b);
    }

    public PSSSigner(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, Digest digest2, int i) {
        this(asymmetricBlockCipher, digest, digest2, i, (byte) TRAILER_IMPLICIT);
    }

    public PSSSigner(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, Digest digest2, int i, byte b) {
        this.cipher = asymmetricBlockCipher;
        this.contentDigest = digest;
        this.mgfDigest = digest2;
        this.hLen = digest.getDigestSize();
        this.mgfhLen = digest2.getDigestSize();
        this.sSet = false;
        this.sLen = i;
        this.salt = new byte[i];
        this.mDash = new byte[((8 + i) + this.hLen)];
        this.trailer = b;
    }

    public PSSSigner(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, Digest digest2, byte[] bArr) {
        this(asymmetricBlockCipher, digest, digest2, bArr, (byte) TRAILER_IMPLICIT);
    }

    public PSSSigner(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, Digest digest2, byte[] bArr, byte b) {
        this.cipher = asymmetricBlockCipher;
        this.contentDigest = digest;
        this.mgfDigest = digest2;
        this.hLen = digest.getDigestSize();
        this.mgfhLen = digest2.getDigestSize();
        this.sSet = true;
        this.sLen = bArr.length;
        this.salt = bArr;
        this.mDash = new byte[((8 + this.sLen) + this.hLen)];
        this.trailer = b;
    }

    public PSSSigner(AsymmetricBlockCipher asymmetricBlockCipher, Digest digest, byte[] bArr) {
        this(asymmetricBlockCipher, digest, digest, bArr, (byte) TRAILER_IMPLICIT);
    }

    private void ItoOSP(int i, byte[] bArr) {
        bArr[0] = (byte) (i >>> 24);
        bArr[1] = (byte) (i >>> 16);
        bArr[2] = (byte) (i >>> 8);
        bArr[3] = (byte) (i >>> 0);
    }

    private void clearBlock(byte[] bArr) {
        for (int i = 0; i != bArr.length; i++) {
            bArr[i] = (byte) 0;
        }
    }

    private byte[] maskGeneratorFunction1(byte[] bArr, int i, int i2, int i3) {
        Object obj = new byte[i3];
        Object obj2 = new byte[this.mgfhLen];
        byte[] bArr2 = new byte[4];
        this.mgfDigest.reset();
        int i4 = 0;
        while (i4 < i3 / this.mgfhLen) {
            ItoOSP(i4, bArr2);
            this.mgfDigest.update(bArr, i, i2);
            this.mgfDigest.update(bArr2, 0, bArr2.length);
            this.mgfDigest.doFinal(obj2, 0);
            System.arraycopy(obj2, 0, obj, this.mgfhLen * i4, this.mgfhLen);
            i4++;
        }
        if (this.mgfhLen * i4 < i3) {
            ItoOSP(i4, bArr2);
            this.mgfDigest.update(bArr, i, i2);
            this.mgfDigest.update(bArr2, 0, bArr2.length);
            this.mgfDigest.doFinal(obj2, 0);
            System.arraycopy(obj2, 0, obj, this.mgfhLen * i4, obj.length - (i4 * this.mgfhLen));
        }
        return obj;
    }

    public byte[] generateSignature() throws CryptoException, DataLengthException {
        this.contentDigest.doFinal(this.mDash, (this.mDash.length - this.hLen) - this.sLen);
        if (this.sLen != 0) {
            if (!this.sSet) {
                this.random.nextBytes(this.salt);
            }
            System.arraycopy(this.salt, 0, this.mDash, this.mDash.length - this.sLen, this.sLen);
        }
        Object obj = new byte[this.hLen];
        this.contentDigest.update(this.mDash, 0, this.mDash.length);
        this.contentDigest.doFinal(obj, 0);
        this.block[(((this.block.length - this.sLen) - 1) - this.hLen) - 1] = (byte) 1;
        System.arraycopy(this.salt, 0, this.block, ((this.block.length - this.sLen) - this.hLen) - 1, this.sLen);
        byte[] maskGeneratorFunction1 = maskGeneratorFunction1(obj, 0, obj.length, (this.block.length - this.hLen) - 1);
        for (int i = 0; i != maskGeneratorFunction1.length; i++) {
            byte[] bArr = this.block;
            bArr[i] = (byte) (bArr[i] ^ maskGeneratorFunction1[i]);
        }
        maskGeneratorFunction1 = this.block;
        maskGeneratorFunction1[0] = (byte) (maskGeneratorFunction1[0] & (255 >> ((this.block.length * 8) - this.emBits)));
        System.arraycopy(obj, 0, this.block, (this.block.length - this.hLen) - 1, this.hLen);
        this.block[this.block.length - 1] = this.trailer;
        byte[] processBlock = this.cipher.processBlock(this.block, 0, this.block.length);
        clearBlock(this.block);
        return processBlock;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        CipherParameters parameters;
        RSAKeyParameters publicKey;
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            parameters = parametersWithRandom.getParameters();
            this.random = parametersWithRandom.getRandom();
        } else {
            if (z) {
                this.random = new SecureRandom();
            }
            parameters = cipherParameters;
        }
        if (parameters instanceof RSABlindingParameters) {
            publicKey = ((RSABlindingParameters) parameters).getPublicKey();
            this.cipher.init(z, cipherParameters);
        } else {
            publicKey = (RSAKeyParameters) parameters;
            this.cipher.init(z, parameters);
        }
        this.emBits = publicKey.getModulus().bitLength() - 1;
        if (this.emBits >= ((this.hLen * 8) + (this.sLen * 8)) + 9) {
            this.block = new byte[((this.emBits + 7) / 8)];
            reset();
            return;
        }
        throw new IllegalArgumentException("key too small for specified hash and salt lengths");
    }

    public void reset() {
        this.contentDigest.reset();
    }

    public void update(byte b) {
        this.contentDigest.update(b);
    }

    public void update(byte[] bArr, int i, int i2) {
        this.contentDigest.update(bArr, i, i2);
    }

    public boolean verifySignature(byte[] bArr) {
        this.contentDigest.doFinal(this.mDash, (this.mDash.length - this.hLen) - this.sLen);
        try {
            Object processBlock = this.cipher.processBlock(bArr, 0, bArr.length);
            System.arraycopy(processBlock, 0, this.block, this.block.length - processBlock.length, processBlock.length);
            if (this.block[this.block.length - 1] == this.trailer) {
                int i;
                int i2;
                bArr = maskGeneratorFunction1(this.block, (this.block.length - this.hLen) - 1, this.hLen, (this.block.length - this.hLen) - 1);
                for (i = 0; i != bArr.length; i++) {
                    byte[] bArr2 = this.block;
                    bArr2[i] = (byte) (bArr2[i] ^ bArr[i]);
                }
                bArr = this.block;
                bArr[0] = (byte) (bArr[0] & (255 >> ((this.block.length * 8) - this.emBits)));
                for (i2 = 0; i2 != ((this.block.length - this.hLen) - this.sLen) - 2; i2++) {
                    if (this.block[i2] != (byte) 0) {
                        break;
                    }
                }
                if (this.block[((this.block.length - this.hLen) - this.sLen) - 2] == (byte) 1) {
                    if (this.sSet) {
                        System.arraycopy(this.salt, 0, this.mDash, this.mDash.length - this.sLen, this.sLen);
                    } else {
                        System.arraycopy(this.block, ((this.block.length - this.sLen) - this.hLen) - 1, this.mDash, this.mDash.length - this.sLen, this.sLen);
                    }
                    this.contentDigest.update(this.mDash, 0, this.mDash.length);
                    this.contentDigest.doFinal(this.mDash, this.mDash.length - this.hLen);
                    i2 = (this.block.length - this.hLen) - 1;
                    i = this.mDash.length - this.hLen;
                    while (i != this.mDash.length) {
                        if ((this.block[i2] ^ this.mDash[i]) != 0) {
                            clearBlock(this.mDash);
                        } else {
                            i2++;
                            i++;
                        }
                    }
                    clearBlock(this.mDash);
                    clearBlock(this.block);
                    return true;
                }
            }
            clearBlock(this.block);
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
