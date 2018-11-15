package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Xof;

final class KeyedHashFunctions {
    private final Digest digest;
    private final int digestSize;

    protected KeyedHashFunctions(Digest digest, int i) {
        if (digest != null) {
            this.digest = digest;
            this.digestSize = i;
            return;
        }
        throw new NullPointerException("digest == null");
    }

    private byte[] coreDigest(int i, byte[] bArr, byte[] bArr2) {
        byte[] toBytesBigEndian = XMSSUtil.toBytesBigEndian((long) i, this.digestSize);
        this.digest.update(toBytesBigEndian, 0, toBytesBigEndian.length);
        this.digest.update(bArr, 0, bArr.length);
        this.digest.update(bArr2, 0, bArr2.length);
        toBytesBigEndian = new byte[this.digestSize];
        if (this.digest instanceof Xof) {
            ((Xof) this.digest).doFinal(toBytesBigEndian, 0, this.digestSize);
            return toBytesBigEndian;
        }
        this.digest.doFinal(toBytesBigEndian, 0);
        return toBytesBigEndian;
    }

    protected byte[] F(byte[] bArr, byte[] bArr2) {
        if (bArr.length != this.digestSize) {
            throw new IllegalArgumentException("wrong key length");
        } else if (bArr2.length == this.digestSize) {
            return coreDigest(0, bArr, bArr2);
        } else {
            throw new IllegalArgumentException("wrong in length");
        }
    }

    protected byte[] H(byte[] bArr, byte[] bArr2) {
        if (bArr.length != this.digestSize) {
            throw new IllegalArgumentException("wrong key length");
        } else if (bArr2.length == 2 * this.digestSize) {
            return coreDigest(1, bArr, bArr2);
        } else {
            throw new IllegalArgumentException("wrong in length");
        }
    }

    protected byte[] HMsg(byte[] bArr, byte[] bArr2) {
        if (bArr.length == 3 * this.digestSize) {
            return coreDigest(2, bArr, bArr2);
        }
        throw new IllegalArgumentException("wrong key length");
    }

    protected byte[] PRF(byte[] bArr, byte[] bArr2) {
        if (bArr.length != this.digestSize) {
            throw new IllegalArgumentException("wrong key length");
        } else if (bArr2.length == 32) {
            return coreDigest(3, bArr, bArr2);
        } else {
            throw new IllegalArgumentException("wrong address length");
        }
    }
}
