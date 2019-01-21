package org.bouncycastle.crypto.prng.drbg;

import java.util.Hashtable;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.prng.EntropySource;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Integers;

public class HashSP800DRBG implements SP80090DRBG {
    private static final int MAX_BITS_REQUEST = 262144;
    private static final byte[] ONE = new byte[]{(byte) 1};
    private static final long RESEED_MAX = 140737488355328L;
    private static final Hashtable seedlens = new Hashtable();
    private byte[] _C;
    private byte[] _V;
    private Digest _digest;
    private EntropySource _entropySource;
    private long _reseedCounter;
    private int _securityStrength;
    private int _seedLength;

    static {
        seedlens.put(McElieceCCA2KeyGenParameterSpec.SHA1, Integers.valueOf(440));
        seedlens.put(McElieceCCA2KeyGenParameterSpec.SHA224, Integers.valueOf(440));
        seedlens.put(McElieceCCA2KeyGenParameterSpec.SHA256, Integers.valueOf(440));
        seedlens.put("SHA-512/256", Integers.valueOf(440));
        seedlens.put("SHA-512/224", Integers.valueOf(440));
        seedlens.put(McElieceCCA2KeyGenParameterSpec.SHA384, Integers.valueOf(888));
        seedlens.put(McElieceCCA2KeyGenParameterSpec.SHA512, Integers.valueOf(888));
    }

    public HashSP800DRBG(Digest digest, int i, EntropySource entropySource, byte[] bArr, byte[] bArr2) {
        if (i > Utils.getMaxSecurityStrength(digest)) {
            throw new IllegalArgumentException("Requested security strength is not supported by the derivation function");
        } else if (entropySource.entropySize() >= i) {
            this._digest = digest;
            this._entropySource = entropySource;
            this._securityStrength = i;
            this._seedLength = ((Integer) seedlens.get(digest.getAlgorithmName())).intValue();
            this._V = Utils.hash_df(this._digest, Arrays.concatenate(getEntropy(), bArr2, bArr), this._seedLength);
            byte[] bArr3 = new byte[(this._V.length + 1)];
            System.arraycopy(this._V, 0, bArr3, 1, this._V.length);
            this._C = Utils.hash_df(this._digest, bArr3, this._seedLength);
            this._reseedCounter = 1;
        } else {
            throw new IllegalArgumentException("Not enough entropy for security strength required");
        }
    }

    private void addTo(byte[] bArr, byte[] bArr2) {
        int i;
        int i2 = 0;
        for (i = 1; i <= bArr2.length; i++) {
            int i3 = ((bArr[bArr.length - i] & 255) + (bArr2[bArr2.length - i] & 255)) + i2;
            i2 = i3 > 255 ? 1 : 0;
            bArr[bArr.length - i] = (byte) i3;
        }
        for (int length = bArr2.length + 1; length <= bArr.length; length++) {
            i = (bArr[bArr.length - length] & 255) + i2;
            i2 = i > 255 ? 1 : 0;
            bArr[bArr.length - length] = (byte) i;
        }
    }

    private void doHash(byte[] bArr, byte[] bArr2) {
        this._digest.update(bArr, 0, bArr.length);
        this._digest.doFinal(bArr2, 0);
    }

    private byte[] getEntropy() {
        byte[] entropy = this._entropySource.getEntropy();
        if (entropy.length >= (this._securityStrength + 7) / 8) {
            return entropy;
        }
        throw new IllegalStateException("Insufficient entropy provided by entropy source");
    }

    private byte[] hash(byte[] bArr) {
        byte[] bArr2 = new byte[this._digest.getDigestSize()];
        doHash(bArr, bArr2);
        return bArr2;
    }

    private byte[] hashgen(byte[] bArr, int i) {
        i /= 8;
        int digestSize = i / this._digest.getDigestSize();
        byte[] bArr2 = new byte[bArr.length];
        System.arraycopy(bArr, 0, bArr2, 0, bArr.length);
        bArr = new byte[i];
        byte[] bArr3 = new byte[this._digest.getDigestSize()];
        for (int i2 = 0; i2 <= digestSize; i2++) {
            doHash(bArr2, bArr3);
            System.arraycopy(bArr3, 0, bArr, bArr3.length * i2, bArr.length - (bArr3.length * i2) > bArr3.length ? bArr3.length : bArr.length - (bArr3.length * i2));
            addTo(bArr2, ONE);
        }
        return bArr;
    }

    public int generate(byte[] bArr, byte[] bArr2, boolean z) {
        int length = bArr.length * 8;
        if (length > 262144) {
            throw new IllegalArgumentException("Number of bits per request limited to 262144");
        } else if (this._reseedCounter > RESEED_MAX) {
            return -1;
        } else {
            Object bArr22;
            byte[] bArr3;
            if (z) {
                reseed(bArr22);
                bArr22 = null;
            }
            if (bArr22 != null) {
                bArr3 = new byte[((this._V.length + 1) + bArr22.length)];
                bArr3[0] = (byte) 2;
                System.arraycopy(this._V, 0, bArr3, 1, this._V.length);
                System.arraycopy(bArr22, 0, bArr3, this._V.length + 1, bArr22.length);
                addTo(this._V, hash(bArr3));
            }
            bArr22 = hashgen(this._V, length);
            bArr3 = new byte[(this._V.length + 1)];
            System.arraycopy(this._V, 0, bArr3, 1, this._V.length);
            bArr3[0] = (byte) 3;
            addTo(this._V, hash(bArr3));
            addTo(this._V, this._C);
            addTo(this._V, new byte[]{(byte) ((int) (this._reseedCounter >> 24)), (byte) ((int) (this._reseedCounter >> 16)), (byte) ((int) (this._reseedCounter >> 8)), (byte) ((int) this._reseedCounter)});
            this._reseedCounter++;
            System.arraycopy(bArr22, 0, bArr, 0, bArr.length);
            return length;
        }
    }

    public int getBlockSize() {
        return this._digest.getDigestSize() * 8;
    }

    public void reseed(byte[] bArr) {
        this._V = Utils.hash_df(this._digest, Arrays.concatenate(ONE, this._V, getEntropy(), bArr), this._seedLength);
        bArr = new byte[(this._V.length + 1)];
        bArr[0] = (byte) 0;
        System.arraycopy(this._V, 0, bArr, 1, this._V.length);
        this._C = Utils.hash_df(this._digest, bArr, this._seedLength);
        this._reseedCounter = 1;
    }
}
