package org.bouncycastle.pqc.crypto.sphincs;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.util.Strings;

class HashFunctions {
    private static final byte[] hashc = Strings.toByteArray("expand 32-byte to 64-byte state!");
    private final Digest dig256;
    private final Digest dig512;
    private final Permute perm;

    HashFunctions(Digest digest) {
        this(digest, null);
    }

    HashFunctions(Digest digest, Digest digest2) {
        this.perm = new Permute();
        this.dig256 = digest;
        this.dig512 = digest2;
    }

    Digest getMessageHash() {
        return this.dig512;
    }

    int hash_2n_n(byte[] bArr, int i, byte[] bArr2, int i2) {
        int i3;
        byte[] bArr3 = new byte[64];
        for (i3 = 0; i3 < 32; i3++) {
            bArr3[i3] = bArr2[i2 + i3];
            bArr3[i3 + 32] = hashc[i3];
        }
        this.perm.chacha_permute(bArr3, bArr3);
        for (i3 = 0; i3 < 32; i3++) {
            bArr3[i3] = (byte) (bArr3[i3] ^ bArr2[(i2 + i3) + 32]);
        }
        this.perm.chacha_permute(bArr3, bArr3);
        for (int i4 = 0; i4 < 32; i4++) {
            bArr[i + i4] = bArr3[i4];
        }
        return 0;
    }

    int hash_2n_n_mask(byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, int i3) {
        byte[] bArr4 = new byte[64];
        for (int i4 = 0; i4 < 64; i4++) {
            bArr4[i4] = (byte) (bArr2[i2 + i4] ^ bArr3[i3 + i4]);
        }
        return hash_2n_n(bArr, i, bArr4, 0);
    }

    int hash_n_n(byte[] bArr, int i, byte[] bArr2, int i2) {
        byte[] bArr3 = new byte[64];
        for (int i3 = 0; i3 < 32; i3++) {
            bArr3[i3] = bArr2[i2 + i3];
            bArr3[i3 + 32] = hashc[i3];
        }
        this.perm.chacha_permute(bArr3, bArr3);
        for (int i4 = 0; i4 < 32; i4++) {
            bArr[i + i4] = bArr3[i4];
        }
        return 0;
    }

    int hash_n_n_mask(byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, int i3) {
        byte[] bArr4 = new byte[32];
        for (int i4 = 0; i4 < 32; i4++) {
            bArr4[i4] = (byte) (bArr2[i2 + i4] ^ bArr3[i3 + i4]);
        }
        return hash_n_n(bArr, i, bArr4, 0);
    }

    int varlen_hash(byte[] bArr, int i, byte[] bArr2, int i2) {
        this.dig256.update(bArr2, 0, i2);
        this.dig256.doFinal(bArr, i);
        return 0;
    }
}
