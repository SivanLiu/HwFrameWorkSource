package org.bouncycastle.crypto.digests;

import org.bouncycastle.util.Memoable;

public final class GOST3411_2012_256Digest extends GOST3411_2012Digest {
    private static final byte[] IV = new byte[]{(byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1};

    public GOST3411_2012_256Digest() {
        super(IV);
    }

    public GOST3411_2012_256Digest(GOST3411_2012_256Digest gOST3411_2012_256Digest) {
        super(IV);
        reset(gOST3411_2012_256Digest);
    }

    public Memoable copy() {
        return new GOST3411_2012_256Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        Object obj = new byte[64];
        super.doFinal(obj, 0);
        System.arraycopy(obj, 32, bArr, i, 32);
        return 32;
    }

    public String getAlgorithmName() {
        return "GOST3411-2012-256";
    }

    public int getDigestSize() {
        return 32;
    }
}
