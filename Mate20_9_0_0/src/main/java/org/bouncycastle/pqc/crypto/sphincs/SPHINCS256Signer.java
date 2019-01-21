package org.bouncycastle.pqc.crypto.sphincs;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.pqc.crypto.MessageSigner;
import org.bouncycastle.util.Pack;

public class SPHINCS256Signer implements MessageSigner {
    private final HashFunctions hashFunctions;
    private byte[] keyData;

    public SPHINCS256Signer(Digest digest, Digest digest2) {
        if (digest.getDigestSize() != 32) {
            throw new IllegalArgumentException("n-digest needs to produce 32 bytes of output");
        } else if (digest2.getDigestSize() == 64) {
            this.hashFunctions = new HashFunctions(digest, digest2);
        } else {
            throw new IllegalArgumentException("2n-digest needs to produce 64 bytes of output");
        }
    }

    static void compute_authpath_wots(HashFunctions hashFunctions, byte[] bArr, byte[] bArr2, int i, leafaddr leafaddr, byte[] bArr3, byte[] bArr4, int i2) {
        leafaddr leafaddr2 = leafaddr;
        leafaddr leafaddr3 = new leafaddr(leafaddr2);
        byte[] bArr5 = new byte[2048];
        byte[] bArr6 = new byte[1024];
        byte[] bArr7 = new byte[68608];
        leafaddr3.subleaf = 0;
        while (leafaddr3.subleaf < 32) {
            Seed.get_seed(hashFunctions, bArr6, (int) (leafaddr3.subleaf * 32), bArr3, leafaddr3);
            leafaddr3.subleaf++;
        }
        HashFunctions hashFunctions2 = hashFunctions;
        Wots wots = new Wots();
        leafaddr3.subleaf = 0;
        while (leafaddr3.subleaf < 32) {
            Wots wots2 = wots;
            wots.wots_pkgen(hashFunctions2, bArr7, (int) ((leafaddr3.subleaf * 67) * 32), bArr6, (int) (leafaddr3.subleaf * 32), bArr4, 0);
            leafaddr3.subleaf++;
            hashFunctions2 = hashFunctions;
            wots = wots2;
        }
        leafaddr3.subleaf = 0;
        while (leafaddr3.subleaf < 32) {
            Tree.l_tree(hashFunctions, bArr5, (int) (1024 + (leafaddr3.subleaf * 32)), bArr7, (int) ((leafaddr3.subleaf * 67) * 32), bArr4, 0);
            leafaddr3.subleaf++;
        }
        int i3 = 0;
        for (int i4 = 32; i4 > 0; i4 >>>= 1) {
            for (int i5 = 0; i5 < i4; i5 += 2) {
                hashFunctions.hash_2n_n_mask(bArr5, ((i4 >>> 1) * 32) + ((i5 >>> 1) * 32), bArr5, (i4 * 32) + (i5 * 32), bArr4, (2 * (7 + i3)) * 32);
            }
            i3++;
        }
        int i6 = (int) leafaddr2.subleaf;
        int i7 = i2;
        for (int i8 = 0; i8 < i7; i8++) {
            System.arraycopy(bArr5, ((32 >>> i8) * 32) + (((i6 >>> i8) ^ 1) * 32), bArr2, i + (i8 * 32), 32);
        }
        System.arraycopy(bArr5, 32, bArr, 0, 32);
    }

    static void validate_authpath(HashFunctions hashFunctions, byte[] bArr, byte[] bArr2, int i, byte[] bArr3, int i2, byte[] bArr4, int i3) {
        int i4;
        byte[] bArr5 = new byte[64];
        int i5;
        if ((i & 1) != 0) {
            for (i5 = 0; i5 < 32; i5++) {
                bArr5[32 + i5] = bArr2[i5];
            }
            for (i4 = 0; i4 < 32; i4++) {
                bArr5[i4] = bArr3[i2 + i4];
            }
        } else {
            for (i5 = 0; i5 < 32; i5++) {
                bArr5[i5] = bArr2[i5];
            }
            for (i4 = 0; i4 < 32; i4++) {
                bArr5[32 + i4] = bArr3[i2 + i4];
            }
        }
        int i6 = i;
        int i7 = i2 + 32;
        i4 = 0;
        while (i4 < i3 - 1) {
            int i8 = i6 >>> 1;
            if ((i8 & 1) != 0) {
                hashFunctions.hash_2n_n_mask(bArr5, 32, bArr5, 0, bArr4, (2 * (7 + i4)) * 32);
                for (i6 = 0; i6 < 32; i6++) {
                    bArr5[i6] = bArr3[i7 + i6];
                }
            } else {
                hashFunctions.hash_2n_n_mask(bArr5, 0, bArr5, 0, bArr4, (2 * (7 + i4)) * 32);
                for (i6 = 0; i6 < 32; i6++) {
                    bArr5[i6 + 32] = bArr3[i7 + i6];
                }
            }
            i7 += 32;
            i4++;
            i6 = i8;
        }
        hashFunctions.hash_2n_n_mask(bArr, 0, bArr5, 0, bArr4, (2 * ((7 + i3) - 1)) * 32);
    }

    private void zerobytes(byte[] bArr, int i, int i2) {
        for (int i3 = 0; i3 != i2; i3++) {
            bArr[i + i3] = (byte) 0;
        }
    }

    byte[] crypto_sign(HashFunctions hashFunctions, byte[] bArr, byte[] bArr2) {
        int i;
        HashFunctions hashFunctions2 = hashFunctions;
        byte[] bArr3 = bArr;
        byte[] bArr4 = new byte[41000];
        byte[] bArr5 = new byte[32];
        byte[] bArr6 = new byte[64];
        long[] jArr = new long[8];
        byte[] bArr7 = new byte[32];
        byte[] bArr8 = new byte[32];
        Object obj = new byte[1024];
        byte[] bArr9 = new byte[1088];
        for (int i2 = 0; i2 < 1088; i2++) {
            bArr9[i2] = bArr2[i2];
        }
        System.arraycopy(bArr9, 1056, bArr4, 40968, 32);
        Digest messageHash = hashFunctions.getMessageHash();
        byte[] bArr10 = new byte[messageHash.getDigestSize()];
        messageHash.update(bArr4, 40968, 32);
        messageHash.update(bArr3, 0, bArr3.length);
        messageHash.doFinal(bArr10, 0);
        zerobytes(bArr4, 40968, 32);
        for (int i3 = 0; i3 != jArr.length; i3++) {
            jArr[i3] = Pack.littleEndianToLong(bArr10, i3 * 8);
        }
        long j = jArr[0] & 1152921504606846975L;
        System.arraycopy(bArr10, 16, bArr5, 0, 32);
        System.arraycopy(bArr5, 0, bArr4, 39912, 32);
        leafaddr leafaddr = new leafaddr();
        leafaddr.level = 11;
        byte[] bArr11 = bArr5;
        leafaddr.subtree = 0;
        leafaddr.subleaf = 0;
        System.arraycopy(bArr9, 32, bArr4, 39944, 1024);
        int i4 = 0;
        byte[] bArr12 = bArr9;
        Object obj2 = obj;
        byte[] bArr13 = bArr8;
        byte[] bArr14 = bArr7;
        Tree.treehash(hashFunctions2, bArr4, 40968, 5, bArr12, leafaddr, bArr4, 39944);
        Digest messageHash2 = hashFunctions.getMessageHash();
        messageHash2.update(bArr4, 39912, 1088);
        messageHash2.update(bArr3, 0, bArr3.length);
        messageHash2.doFinal(bArr6, 0);
        leafaddr leafaddr2 = new leafaddr();
        int i5 = 12;
        leafaddr2.level = 12;
        leafaddr2.subleaf = (long) ((int) (j & 31));
        leafaddr2.subtree = j >>> 5;
        for (i = 0; i < 32; i++) {
            bArr4[i] = bArr11[i];
        }
        bArr7 = bArr12;
        System.arraycopy(bArr7, 32, obj2, 0, 1024);
        for (i = 0; i < 8; i++) {
            bArr4[32 + i] = (byte) ((int) ((j >>> (8 * i)) & 255));
        }
        bArr8 = bArr13;
        Seed.get_seed(hashFunctions2, bArr8, 0, bArr7, leafaddr2);
        Horst horst = new Horst();
        bArr5 = bArr8;
        int horst_sign = 40 + Horst.horst_sign(hashFunctions2, bArr4, 40, bArr14, bArr8, obj2, bArr6);
        Wots wots = new Wots();
        int i6 = horst_sign;
        int i7 = 0;
        while (i7 < i5) {
            leafaddr2.level = i7;
            Seed.get_seed(hashFunctions2, bArr5, 0, bArr7, leafaddr2);
            bArr9 = bArr4;
            i5 = i6;
            horst_sign = i7;
            Object obj3 = obj2;
            wots.wots_sign(hashFunctions2, bArr9, i6, bArr14, bArr5, obj3);
            i5 += 2144;
            byte[] bArr15 = bArr7;
            compute_authpath_wots(hashFunctions2, bArr14, bArr9, i5, leafaddr2, bArr7, obj3, 5);
            i6 = i5 + CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256;
            leafaddr2.subleaf = (long) ((int) (leafaddr2.subtree & 31));
            leafaddr2.subtree >>>= 5;
            i7 = horst_sign + 1;
            bArr7 = bArr15;
            hashFunctions2 = hashFunctions;
            i5 = 12;
        }
        zerobytes(bArr7, 0, 1088);
        return bArr4;
    }

    public byte[] generateSignature(byte[] bArr) {
        return crypto_sign(this.hashFunctions, bArr, this.keyData);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:5:0x0012 in {1, 3, 4} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void init(boolean r1, org.bouncycastle.crypto.CipherParameters r2) {
        /*
        r0 = this;
        if (r1 == 0) goto L_0x000b;
        r2 = (org.bouncycastle.pqc.crypto.sphincs.SPHINCSPrivateKeyParameters) r2;
        r1 = r2.getKeyData();
        r0.keyData = r1;
        return;
        r2 = (org.bouncycastle.pqc.crypto.sphincs.SPHINCSPublicKeyParameters) r2;
        r1 = r2.getKeyData();
        goto L_0x0008;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.pqc.crypto.sphincs.SPHINCS256Signer.init(boolean, org.bouncycastle.crypto.CipherParameters):void");
    }

    boolean verify(HashFunctions hashFunctions, byte[] bArr, byte[] bArr2, byte[] bArr3) {
        byte[] bArr4 = bArr;
        Object obj = bArr2;
        byte[] bArr5 = new byte[2144];
        byte[] bArr6 = new byte[32];
        byte[] bArr7 = new byte[32];
        byte[] bArr8 = new byte[41000];
        byte[] bArr9 = new byte[1056];
        if (obj.length == 41000) {
            int i;
            byte[] bArr10;
            byte[] bArr11 = new byte[64];
            for (int i2 = 0; i2 < 1056; i2++) {
                bArr9[i2] = bArr3[i2];
            }
            byte[] bArr12 = new byte[32];
            for (int i3 = 0; i3 < 32; i3++) {
                bArr12[i3] = obj[i3];
            }
            System.arraycopy(obj, 0, bArr8, 0, 41000);
            Digest messageHash = hashFunctions.getMessageHash();
            messageHash.update(bArr12, 0, 32);
            messageHash.update(bArr9, 0, 1056);
            messageHash.update(bArr4, 0, bArr4.length);
            messageHash.doFinal(bArr11, 0);
            long j = 0;
            for (i = 0; i < 8; i++) {
                j ^= ((long) (bArr8[32 + i] & 255)) << (8 * i);
            }
            Horst horst = new Horst();
            Horst.horst_verify(hashFunctions, bArr7, bArr8, 40, bArr9, bArr11);
            Wots wots = new Wots();
            int i4 = 13352;
            i = 0;
            long j2 = j;
            while (i < 12) {
                int i5 = i4;
                bArr10 = bArr9;
                wots.wots_verify(hashFunctions, bArr5, bArr8, i4, bArr7, bArr9);
                i5 += 2144;
                Tree.l_tree(hashFunctions, bArr6, 0, bArr5, 0, bArr10, 0);
                byte[] bArr13 = bArr8;
                bArr12 = bArr7;
                byte[] bArr14 = bArr6;
                validate_authpath(hashFunctions, bArr7, bArr6, (int) (j2 & 31), bArr13, i5, bArr10, 5);
                j2 >>= 5;
                i4 = i5 + CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256;
                i++;
                bArr8 = bArr13;
                bArr9 = bArr10;
            }
            bArr10 = bArr9;
            bArr12 = bArr7;
            boolean z = true;
            for (i = 0; i < 32; i++) {
                if (bArr12[i] != bArr10[i + 1024]) {
                    z = false;
                }
            }
            return z;
        }
        throw new IllegalArgumentException("signature wrong size");
    }

    public boolean verifySignature(byte[] bArr, byte[] bArr2) {
        return verify(this.hashFunctions, bArr, bArr2, this.keyData);
    }
}
