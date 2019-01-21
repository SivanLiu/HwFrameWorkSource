package com.android.org.bouncycastle.crypto.modes.gcm;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Pack;
import java.lang.reflect.Array;

public class Tables8kGCMMultiplier implements GCMMultiplier {
    private byte[] H;
    private int[][][] M;

    public void init(byte[] H) {
        int j;
        int j2 = 4;
        if (this.M == null) {
            this.M = (int[][][]) Array.newInstance(int.class, new int[]{32, 16, 4});
        } else if (Arrays.areEqual(this.H, H)) {
            return;
        }
        this.H = Arrays.clone(H);
        GCMUtil.asInts(H, this.M[1][8]);
        for (j = 4; j >= 1; j >>= 1) {
            GCMUtil.multiplyP(this.M[1][j + j], this.M[1][j]);
        }
        int i = 0;
        GCMUtil.multiplyP(this.M[1][1], this.M[0][8]);
        while (true) {
            j = j2;
            if (j < 1) {
                break;
            }
            GCMUtil.multiplyP(this.M[0][j + j], this.M[0][j]);
            j2 = j >> 1;
        }
        while (true) {
            j = i;
            for (j2 = 2; j2 < 16; j2 += j2) {
                for (int k = 1; k < j2; k++) {
                    GCMUtil.xor(this.M[j][j2], this.M[j][k], this.M[j][j2 + k]);
                }
            }
            i = j + 1;
            if (i != 32) {
                if (i > 1) {
                    for (j = 8; j > 0; j >>= 1) {
                        GCMUtil.multiplyP8(this.M[i - 2][j], this.M[i][j]);
                    }
                }
            } else {
                return;
            }
        }
    }

    public void multiplyH(byte[] x) {
        int[] z = new int[4];
        for (int i = 15; i >= 0; i--) {
            int[] m = this.M[i + i][x[i] & 15];
            z[0] = z[0] ^ m[0];
            z[1] = z[1] ^ m[1];
            z[2] = z[2] ^ m[2];
            z[3] = z[3] ^ m[3];
            m = this.M[(i + i) + 1][(x[i] & 240) >>> 4];
            z[0] = z[0] ^ m[0];
            z[1] = z[1] ^ m[1];
            z[2] = z[2] ^ m[2];
            z[3] = z[3] ^ m[3];
        }
        Pack.intToBigEndian(z, x, 0);
    }
}
