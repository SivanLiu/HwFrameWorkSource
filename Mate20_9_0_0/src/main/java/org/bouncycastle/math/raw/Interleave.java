package org.bouncycastle.math.raw;

import org.bouncycastle.crypto.tls.CipherSuite;

public class Interleave {
    private static final long M32 = 1431655765;
    private static final long M64 = 6148914691236517205L;
    private static final long M64R = -6148914691236517206L;

    public static int expand16to32(int i) {
        i &= 65535;
        i = (i | (i << 8)) & 16711935;
        i = (i | (i << 4)) & 252645135;
        i = (i | (i << 2)) & 858993459;
        return (i | (i << 1)) & 1431655765;
    }

    public static long expand32to64(int i) {
        int i2 = ((i >>> 8) ^ i) & CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB;
        i ^= i2 ^ (i2 << 8);
        i2 = ((i >>> 4) ^ i) & 15728880;
        i ^= i2 ^ (i2 << 4);
        i2 = ((i >>> 2) ^ i) & 202116108;
        i ^= i2 ^ (i2 << 2);
        i2 = ((i >>> 1) ^ i) & 572662306;
        i ^= i2 ^ (i2 << 1);
        return ((((long) (i >>> 1)) & M32) << 32) | (M32 & ((long) i));
    }

    public static void expand64To128(long j, long[] jArr, int i) {
        long j2 = ((j >>> 16) ^ j) & 4294901760L;
        j ^= j2 ^ (j2 << 16);
        j2 = ((j >>> 8) ^ j) & 280375465148160L;
        j ^= j2 ^ (j2 << 8);
        j2 = ((j >>> 4) ^ j) & 67555025218437360L;
        j ^= j2 ^ (j2 << 4);
        j2 = ((j >>> 2) ^ j) & 868082074056920076L;
        j ^= j2 ^ (j2 << 2);
        j2 = ((j >>> 1) ^ j) & 2459565876494606882L;
        j ^= j2 ^ (j2 << 1);
        jArr[i] = j & M64;
        jArr[i + 1] = (j >>> 1) & M64;
    }

    public static void expand64To128Rev(long j, long[] jArr, int i) {
        long j2 = ((j >>> 16) ^ j) & 4294901760L;
        j ^= j2 ^ (j2 << 16);
        j2 = ((j >>> 8) ^ j) & 280375465148160L;
        j ^= j2 ^ (j2 << 8);
        j2 = ((j >>> 4) ^ j) & 67555025218437360L;
        j ^= j2 ^ (j2 << 4);
        j2 = ((j >>> 2) ^ j) & 868082074056920076L;
        j ^= j2 ^ (j2 << 2);
        j2 = ((j >>> 1) ^ j) & 2459565876494606882L;
        j ^= j2 ^ (j2 << 1);
        jArr[i] = j & M64R;
        jArr[i + 1] = (j << 1) & M64R;
    }

    public static int expand8to16(int i) {
        i &= 255;
        i = (i | (i << 4)) & 3855;
        i = (i | (i << 2)) & 13107;
        return (i | (i << 1)) & 21845;
    }

    public static long unshuffle(long j) {
        long j2 = ((j >>> 1) ^ j) & 2459565876494606882L;
        j ^= j2 ^ (j2 << 1);
        j2 = ((j >>> 2) ^ j) & 868082074056920076L;
        j ^= j2 ^ (j2 << 2);
        j2 = ((j >>> 4) ^ j) & 67555025218437360L;
        j ^= j2 ^ (j2 << 4);
        j2 = ((j >>> 8) ^ j) & 280375465148160L;
        j ^= j2 ^ (j2 << 8);
        j2 = ((j >>> 16) ^ j) & 4294901760L;
        return j ^ (j2 ^ (j2 << 16));
    }
}
