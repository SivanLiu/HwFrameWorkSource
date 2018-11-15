package org.bouncycastle.pqc.crypto.mceliece;

import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.pqc.crypto.MessageEncryptor;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.bouncycastle.pqc.math.linearalgebra.GF2Vector;
import org.bouncycastle.pqc.math.linearalgebra.IntegerFunctions;

public class McElieceKobaraImaiCipher implements MessageEncryptor {
    private static final String DEFAULT_PRNG_NAME = "SHA1PRNG";
    public static final String OID = "1.3.6.1.4.1.8301.3.1.3.4.2.3";
    public static final byte[] PUBLIC_CONSTANT = "a predetermined public constant".getBytes();
    private boolean forEncryption;
    private int k;
    McElieceCCA2KeyParameters key;
    private Digest messDigest;
    private int n;
    private SecureRandom sr;
    private int t;

    private void initCipherDecrypt(McElieceCCA2PrivateKeyParameters mcElieceCCA2PrivateKeyParameters) {
        this.messDigest = Utils.getDigest(mcElieceCCA2PrivateKeyParameters.getDigest());
        this.n = mcElieceCCA2PrivateKeyParameters.getN();
        this.k = mcElieceCCA2PrivateKeyParameters.getK();
        this.t = mcElieceCCA2PrivateKeyParameters.getT();
    }

    private void initCipherEncrypt(McElieceCCA2PublicKeyParameters mcElieceCCA2PublicKeyParameters) {
        this.messDigest = Utils.getDigest(mcElieceCCA2PublicKeyParameters.getDigest());
        this.n = mcElieceCCA2PublicKeyParameters.getN();
        this.k = mcElieceCCA2PublicKeyParameters.getK();
        this.t = mcElieceCCA2PublicKeyParameters.getT();
    }

    public int getKeySize(McElieceCCA2KeyParameters mcElieceCCA2KeyParameters) {
        if (mcElieceCCA2KeyParameters instanceof McElieceCCA2PublicKeyParameters) {
            return ((McElieceCCA2PublicKeyParameters) mcElieceCCA2KeyParameters).getN();
        }
        if (mcElieceCCA2KeyParameters instanceof McElieceCCA2PrivateKeyParameters) {
            return ((McElieceCCA2PrivateKeyParameters) mcElieceCCA2KeyParameters).getN();
        }
        throw new IllegalArgumentException("unsupported type");
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.forEncryption = z;
        if (z) {
            if (cipherParameters instanceof ParametersWithRandom) {
                ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
                this.sr = parametersWithRandom.getRandom();
                this.key = (McElieceCCA2PublicKeyParameters) parametersWithRandom.getParameters();
            } else {
                this.sr = new SecureRandom();
                this.key = (McElieceCCA2PublicKeyParameters) cipherParameters;
            }
            initCipherEncrypt((McElieceCCA2PublicKeyParameters) this.key);
            return;
        }
        this.key = (McElieceCCA2PrivateKeyParameters) cipherParameters;
        initCipherDecrypt((McElieceCCA2PrivateKeyParameters) this.key);
    }

    public byte[] messageDecrypt(byte[] bArr) throws InvalidCipherTextException {
        if (this.forEncryption) {
            throw new IllegalStateException("cipher initialised for decryption");
        }
        int i = this.n >> 3;
        if (bArr.length >= i) {
            byte[][] split;
            byte[] bArr2;
            int digestSize = this.messDigest.getDigestSize();
            int i2 = this.k >> 3;
            int length = bArr.length - i;
            if (length > 0) {
                split = ByteUtils.split(bArr, length);
                bArr2 = split[0];
                bArr = split[1];
            } else {
                bArr2 = new byte[0];
            }
            GF2Vector[] decryptionPrimitive = McElieceCCA2Primitives.decryptionPrimitive((McElieceCCA2PrivateKeyParameters) this.key, GF2Vector.OS2VP(this.n, bArr));
            byte[] encoded = decryptionPrimitive[0].getEncoded();
            GF2Vector gF2Vector = decryptionPrimitive[1];
            if (encoded.length > i2) {
                encoded = ByteUtils.subArray(encoded, 0, i2);
            }
            bArr = ByteUtils.concatenate(ByteUtils.concatenate(bArr2, Conversions.decode(this.n, this.t, gF2Vector)), encoded);
            i2 = bArr.length - digestSize;
            split = ByteUtils.split(bArr, digestSize);
            bArr2 = split[0];
            bArr = split[1];
            encoded = new byte[this.messDigest.getDigestSize()];
            this.messDigest.update(bArr, 0, bArr.length);
            this.messDigest.doFinal(encoded, 0);
            for (digestSize--; digestSize >= 0; digestSize--) {
                encoded[digestSize] = (byte) (encoded[digestSize] ^ bArr2[digestSize]);
            }
            DigestRandomGenerator digestRandomGenerator = new DigestRandomGenerator(new SHA1Digest());
            digestRandomGenerator.addSeedMaterial(encoded);
            bArr2 = new byte[i2];
            digestRandomGenerator.nextBytes(bArr2);
            for (digestSize = i2 - 1; digestSize >= 0; digestSize--) {
                bArr2[digestSize] = (byte) (bArr2[digestSize] ^ bArr[digestSize]);
            }
            if (bArr2.length >= i2) {
                split = ByteUtils.split(bArr2, i2 - PUBLIC_CONSTANT.length);
                byte[] bArr3 = split[0];
                if (ByteUtils.equals(split[1], PUBLIC_CONSTANT)) {
                    return bArr3;
                }
                throw new InvalidCipherTextException("Bad Padding: invalid ciphertext");
            }
            throw new InvalidCipherTextException("Bad Padding: invalid ciphertext");
        }
        throw new InvalidCipherTextException("Bad Padding: Ciphertext too short.");
    }

    public byte[] messageEncrypt(byte[] bArr) {
        if (this.forEncryption) {
            int digestSize = this.messDigest.getDigestSize();
            int i = this.k >> 3;
            int bitLength = (IntegerFunctions.binomial(this.n, this.t).bitLength() - 1) >> 3;
            int length = ((i + bitLength) - digestSize) - PUBLIC_CONSTANT.length;
            if (bArr.length > length) {
                length = bArr.length;
            }
            int length2 = PUBLIC_CONSTANT.length + length;
            int i2 = ((length2 + digestSize) - i) - bitLength;
            Object obj = new byte[length2];
            System.arraycopy(bArr, 0, obj, 0, bArr.length);
            System.arraycopy(PUBLIC_CONSTANT, 0, obj, length, PUBLIC_CONSTANT.length);
            bArr = new byte[digestSize];
            this.sr.nextBytes(bArr);
            DigestRandomGenerator digestRandomGenerator = new DigestRandomGenerator(new SHA1Digest());
            digestRandomGenerator.addSeedMaterial(bArr);
            byte[] bArr2 = new byte[length2];
            digestRandomGenerator.nextBytes(bArr2);
            for (length2--; length2 >= 0; length2--) {
                bArr2[length2] = (byte) (bArr2[length2] ^ obj[length2]);
            }
            byte[] bArr3 = new byte[this.messDigest.getDigestSize()];
            this.messDigest.update(bArr2, 0, bArr2.length);
            this.messDigest.doFinal(bArr3, 0);
            for (digestSize--; digestSize >= 0; digestSize--) {
                bArr3[digestSize] = (byte) (bArr3[digestSize] ^ bArr[digestSize]);
            }
            Object concatenate = ByteUtils.concatenate(bArr3, bArr2);
            byte[] bArr4 = new byte[0];
            if (i2 > 0) {
                bArr4 = new byte[i2];
                System.arraycopy(concatenate, 0, bArr4, 0, i2);
            }
            Object obj2 = new byte[bitLength];
            System.arraycopy(concatenate, i2, obj2, 0, bitLength);
            Object obj3 = new byte[i];
            System.arraycopy(concatenate, bitLength + i2, obj3, 0, i);
            bArr = McElieceCCA2Primitives.encryptionPrimitive((McElieceCCA2PublicKeyParameters) this.key, GF2Vector.OS2VP(this.k, obj3), Conversions.encode(this.n, this.t, obj2)).getEncoded();
            return i2 > 0 ? ByteUtils.concatenate(bArr4, bArr) : bArr;
        } else {
            throw new IllegalStateException("cipher initialised for decryption");
        }
    }
}
