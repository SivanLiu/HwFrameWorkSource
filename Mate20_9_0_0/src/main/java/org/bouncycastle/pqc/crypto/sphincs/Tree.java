package org.bouncycastle.pqc.crypto.sphincs;

class Tree {

    static class leafaddr {
        int level;
        long subleaf;
        long subtree;

        public leafaddr(leafaddr leafaddr) {
            this.level = leafaddr.level;
            this.subtree = leafaddr.subtree;
            this.subleaf = leafaddr.subleaf;
        }
    }

    Tree() {
    }

    static void gen_leaf_wots(HashFunctions hashFunctions, byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, leafaddr leafaddr) {
        byte[] bArr4 = new byte[32];
        byte[] bArr5 = new byte[2144];
        Wots wots = new Wots();
        HashFunctions hashFunctions2 = hashFunctions;
        Seed.get_seed(hashFunctions2, bArr4, 0, bArr3, leafaddr);
        wots.wots_pkgen(hashFunctions2, bArr5, 0, bArr4, 0, bArr2, i2);
        l_tree(hashFunctions2, bArr, i, bArr5, 0, bArr2, i2);
    }

    static void l_tree(HashFunctions hashFunctions, byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, int i3) {
        Object obj = bArr2;
        int i4 = i2;
        int i5 = 67;
        for (int i6 = 0; i6 < 7; i6++) {
            int i7;
            int i8 = 0;
            while (true) {
                i7 = i5 >>> 1;
                if (i8 >= i7) {
                    break;
                }
                hashFunctions.hash_2n_n_mask(obj, i4 + (i8 * 32), obj, i4 + ((i8 * 2) * 32), bArr3, i3 + ((i6 * 2) * 32));
                i8++;
            }
            if ((i5 & 1) != 0) {
                System.arraycopy(obj, i4 + ((i5 - 1) * 32), obj, (i7 * 32) + i4, 32);
                i7++;
            }
            i5 = i7;
        }
        System.arraycopy(obj, i4, bArr, i, 32);
    }

    static void treehash(HashFunctions hashFunctions, byte[] bArr, int i, int i2, byte[] bArr2, leafaddr leafaddr, byte[] bArr3, int i3) {
        int i4;
        int i5;
        leafaddr leafaddr2 = new leafaddr(leafaddr);
        int i6 = i2 + 1;
        byte[] bArr4 = new byte[(i6 * 32)];
        int[] iArr = new int[i6];
        int i7 = 1;
        int i8 = (int) (leafaddr2.subleaf + ((long) (1 << i2)));
        int i9 = 0;
        while (true) {
            int i10 = 32;
            if (leafaddr2.subleaf >= ((long) i8)) {
                break;
            }
            int i11;
            int[] iArr2;
            gen_leaf_wots(hashFunctions, bArr4, i9 * 32, bArr3, i3, bArr2, leafaddr2);
            iArr[i9] = 0;
            i4 = i9 + i7;
            while (i4 > i7) {
                i6 = i4 - 1;
                int i12 = i4 - 2;
                if (iArr[i6] != iArr[i12]) {
                    break;
                }
                i6 = i12 * 32;
                i5 = i10;
                i11 = i8;
                i8 = i6;
                i6 = i7;
                iArr2 = iArr;
                hashFunctions.hash_2n_n_mask(bArr4, i6, bArr4, i8, bArr3, i3 + ((2 * (iArr[i6] + 7)) * i10));
                iArr2[i12] = iArr2[i12] + i6;
                i4--;
                i7 = i6;
                i10 = i5;
                i8 = i11;
                iArr = iArr2;
            }
            i11 = i8;
            i6 = i7;
            iArr2 = iArr;
            leafaddr2.subleaf++;
            i9 = i4;
            i7 = i6;
            i8 = i11;
            iArr = iArr2;
        }
        i5 = 32;
        for (i4 = 0; i4 < i5; i4++) {
            bArr[i + i4] = bArr4[i4];
        }
    }
}
