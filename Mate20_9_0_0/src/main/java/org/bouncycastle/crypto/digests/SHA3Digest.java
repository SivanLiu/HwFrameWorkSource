package org.bouncycastle.crypto.digests;

public class SHA3Digest extends KeccakDigest {
    public SHA3Digest() {
        this(256);
    }

    public SHA3Digest(int i) {
        super(checkBitLength(i));
    }

    public SHA3Digest(SHA3Digest sHA3Digest) {
        super((KeccakDigest) sHA3Digest);
    }

    /* JADX WARNING: Missing block: B:11:0x002d, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int checkBitLength(int i) {
        if (i == 224 || i == 256 || i == 384 || i == 512) {
            return i;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'bitLength' ");
        stringBuilder.append(i);
        stringBuilder.append(" not supported for SHA-3");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int doFinal(byte[] bArr, int i) {
        absorbBits(2, 2);
        return super.doFinal(bArr, i);
    }

    protected int doFinal(byte[] bArr, int i, byte b, int i2) {
        if (i2 < 0 || i2 > 7) {
            throw new IllegalArgumentException("'partialBits' must be in the range [0,7]");
        }
        int i3 = (b & ((1 << i2) - 1)) | (2 << i2);
        i2 += 2;
        if (i2 >= 8) {
            absorb(new byte[]{(byte) i3}, 0, 1);
            i2 -= 8;
            i3 >>>= 8;
        }
        return super.doFinal(bArr, i, (byte) i3, i2);
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SHA3-");
        stringBuilder.append(this.fixedOutputLength);
        return stringBuilder.toString();
    }
}
