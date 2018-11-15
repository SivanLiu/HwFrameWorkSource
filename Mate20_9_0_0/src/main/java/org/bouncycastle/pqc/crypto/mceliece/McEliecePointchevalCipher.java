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

public class McEliecePointchevalCipher implements MessageEncryptor {
    public static final String OID = "1.3.6.1.4.1.8301.3.1.3.4.2.2";
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
        this.sr = this.sr != null ? this.sr : new SecureRandom();
        this.messDigest = Utils.getDigest(mcElieceCCA2PublicKeyParameters.getDigest());
        this.n = mcElieceCCA2PublicKeyParameters.getN();
        this.k = mcElieceCCA2PublicKeyParameters.getK();
        this.t = mcElieceCCA2PublicKeyParameters.getT();
    }

    protected int decryptOutputSize(int i) {
        return 0;
    }

    protected int encryptOutputSize(int i) {
        return 0;
    }

    public int getKeySize(McElieceCCA2KeyParameters mcElieceCCA2KeyParameters) throws IllegalArgumentException {
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
        int i = (this.n + 7) >> 3;
        int length = bArr.length - i;
        byte[][] split = ByteUtils.split(bArr, i);
        byte[] bArr2 = split[0];
        bArr = split[1];
        GF2Vector[] decryptionPrimitive = McElieceCCA2Primitives.decryptionPrimitive((McElieceCCA2PrivateKeyParameters) this.key, GF2Vector.OS2VP(this.n, bArr2));
        byte[] encoded = decryptionPrimitive[0].getEncoded();
        Object obj = decryptionPrimitive[1];
        DigestRandomGenerator digestRandomGenerator = new DigestRandomGenerator(new SHA1Digest());
        digestRandomGenerator.addSeedMaterial(encoded);
        encoded = new byte[length];
        digestRandomGenerator.nextBytes(encoded);
        for (int i2 = 0; i2 < length; i2++) {
            encoded[i2] = (byte) (encoded[i2] ^ bArr[i2]);
        }
        this.messDigest.update(encoded, 0, encoded.length);
        bArr = new byte[this.messDigest.getDigestSize()];
        this.messDigest.doFinal(bArr, 0);
        if (Conversions.encode(this.n, this.t, bArr).equals(obj)) {
            return ByteUtils.split(encoded, length - (this.k >> 3))[0];
        }
        throw new InvalidCipherTextException("Bad Padding: Invalid ciphertext.");
    }

    public byte[] messageEncrypt(byte[] bArr) {
        if (this.forEncryption) {
            int i;
            int i2 = this.k >> 3;
            byte[] bArr2 = new byte[i2];
            this.sr.nextBytes(bArr2);
            GF2Vector gF2Vector = new GF2Vector(this.k, this.sr);
            byte[] encoded = gF2Vector.getEncoded();
            byte[] concatenate = ByteUtils.concatenate(bArr, bArr2);
            int i3 = 0;
            this.messDigest.update(concatenate, 0, concatenate.length);
            concatenate = new byte[this.messDigest.getDigestSize()];
            this.messDigest.doFinal(concatenate, 0);
            byte[] encoded2 = McElieceCCA2Primitives.encryptionPrimitive((McElieceCCA2PublicKeyParameters) this.key, gF2Vector, Conversions.encode(this.n, this.t, concatenate)).getEncoded();
            DigestRandomGenerator digestRandomGenerator = new DigestRandomGenerator(new SHA1Digest());
            digestRandomGenerator.addSeedMaterial(encoded);
            encoded = new byte[(bArr.length + i2)];
            digestRandomGenerator.nextBytes(encoded);
            for (i = 0; i < bArr.length; i++) {
                encoded[i] = (byte) (encoded[i] ^ bArr[i]);
            }
            while (i3 < i2) {
                i = bArr.length + i3;
                encoded[i] = (byte) (encoded[i] ^ bArr2[i3]);
                i3++;
            }
            return ByteUtils.concatenate(encoded2, encoded);
        }
        throw new IllegalStateException("cipher initialised for decryption");
    }
}
