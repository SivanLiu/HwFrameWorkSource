package org.bouncycastle.pqc.crypto.gmss.util;

import org.bouncycastle.crypto.Digest;

public class GMSSRandom {
    private Digest messDigestTree;

    public GMSSRandom(Digest digest) {
        this.messDigestTree = digest;
    }

    private void addByteArrays(byte[] bArr, byte[] bArr2) {
        int i = 0;
        int i2 = 0;
        while (i < bArr.length) {
            int i3 = ((bArr[i] & 255) + (255 & bArr2[i])) + i2;
            bArr[i] = (byte) i3;
            i2 = (byte) (i3 >> 8);
            i++;
        }
    }

    private void addOne(byte[] bArr) {
        int i = 1;
        for (int i2 = 0; i2 < bArr.length; i2++) {
            int i3 = (255 & bArr[i2]) + i;
            bArr[i2] = (byte) i3;
            i = (byte) (i3 >> 8);
        }
    }

    public byte[] nextSeed(byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length];
        this.messDigestTree.update(bArr, 0, bArr.length);
        bArr2 = new byte[this.messDigestTree.getDigestSize()];
        this.messDigestTree.doFinal(bArr2, 0);
        addByteArrays(bArr, bArr2);
        addOne(bArr);
        return bArr2;
    }
}
