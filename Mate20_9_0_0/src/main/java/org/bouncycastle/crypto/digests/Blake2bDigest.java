package org.bouncycastle.crypto.digests;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.Arrays;

public class Blake2bDigest implements ExtendedDigest {
    private static final int BLOCK_LENGTH_BYTES = 128;
    private static final long[] blake2b_IV = new long[]{7640891576956012808L, -4942790177534073029L, 4354685564936845355L, -6534734903238641935L, 5840696475078001361L, -7276294671716946913L, 2270897969802886507L, 6620516959819538809L};
    private static final byte[][] blake2b_sigma = new byte[][]{new byte[]{(byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15}, new byte[]{(byte) 14, (byte) 10, (byte) 4, (byte) 8, (byte) 9, (byte) 15, (byte) 13, (byte) 6, (byte) 1, (byte) 12, (byte) 0, (byte) 2, (byte) 11, (byte) 7, (byte) 5, (byte) 3}, new byte[]{(byte) 11, (byte) 8, (byte) 12, (byte) 0, (byte) 5, (byte) 2, (byte) 15, (byte) 13, (byte) 10, (byte) 14, (byte) 3, (byte) 6, (byte) 7, (byte) 1, (byte) 9, (byte) 4}, new byte[]{(byte) 7, (byte) 9, (byte) 3, (byte) 1, (byte) 13, (byte) 12, (byte) 11, (byte) 14, (byte) 2, (byte) 6, (byte) 5, (byte) 10, (byte) 4, (byte) 0, (byte) 15, (byte) 8}, new byte[]{(byte) 9, (byte) 0, (byte) 5, (byte) 7, (byte) 2, (byte) 4, (byte) 10, (byte) 15, (byte) 14, (byte) 1, (byte) 11, (byte) 12, (byte) 6, (byte) 8, (byte) 3, (byte) 13}, new byte[]{(byte) 2, (byte) 12, (byte) 6, (byte) 10, (byte) 0, (byte) 11, (byte) 8, (byte) 3, (byte) 4, (byte) 13, (byte) 7, (byte) 5, (byte) 15, (byte) 14, (byte) 1, (byte) 9}, new byte[]{(byte) 12, (byte) 5, (byte) 1, (byte) 15, (byte) 14, (byte) 13, (byte) 4, (byte) 10, (byte) 0, (byte) 7, (byte) 6, (byte) 3, (byte) 9, (byte) 2, (byte) 8, (byte) 11}, new byte[]{(byte) 13, (byte) 11, (byte) 7, (byte) 14, (byte) 12, (byte) 1, (byte) 3, (byte) 9, (byte) 5, (byte) 0, (byte) 15, (byte) 4, (byte) 8, (byte) 6, (byte) 2, (byte) 10}, new byte[]{(byte) 6, (byte) 15, (byte) 14, (byte) 9, (byte) 11, (byte) 3, (byte) 0, (byte) 8, (byte) 12, (byte) 2, (byte) 13, (byte) 7, (byte) 1, (byte) 4, (byte) 10, (byte) 5}, new byte[]{(byte) 10, (byte) 2, (byte) 8, (byte) 4, (byte) 7, (byte) 6, (byte) 1, (byte) 5, (byte) 15, (byte) 11, (byte) 9, (byte) 14, (byte) 3, (byte) 12, (byte) 13, (byte) 0}, new byte[]{(byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15}, new byte[]{(byte) 14, (byte) 10, (byte) 4, (byte) 8, (byte) 9, (byte) 15, (byte) 13, (byte) 6, (byte) 1, (byte) 12, (byte) 0, (byte) 2, (byte) 11, (byte) 7, (byte) 5, (byte) 3}};
    private static int rOUNDS = 12;
    private byte[] buffer;
    private int bufferPos;
    private long[] chainValue;
    private int digestLength;
    private long f0;
    private long[] internalState;
    private byte[] key;
    private int keyLength;
    private byte[] personalization;
    private byte[] salt;
    private long t0;
    private long t1;

    public Blake2bDigest() {
        this(512);
    }

    public Blake2bDigest(int i) {
        this.digestLength = 64;
        this.keyLength = 0;
        this.salt = null;
        this.personalization = null;
        this.key = null;
        this.buffer = null;
        this.bufferPos = 0;
        this.internalState = new long[16];
        this.chainValue = null;
        this.t0 = 0;
        this.t1 = 0;
        this.f0 = 0;
        if (i == CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256 || i == 256 || i == 384 || i == 512) {
            this.buffer = new byte[128];
            this.keyLength = 0;
            this.digestLength = i / 8;
            init();
            return;
        }
        throw new IllegalArgumentException("Blake2b digest restricted to one of [160, 256, 384, 512]");
    }

    public Blake2bDigest(Blake2bDigest blake2bDigest) {
        this.digestLength = 64;
        this.keyLength = 0;
        this.salt = null;
        this.personalization = null;
        this.key = null;
        this.buffer = null;
        this.bufferPos = 0;
        this.internalState = new long[16];
        this.chainValue = null;
        this.t0 = 0;
        this.t1 = 0;
        this.f0 = 0;
        this.bufferPos = blake2bDigest.bufferPos;
        this.buffer = Arrays.clone(blake2bDigest.buffer);
        this.keyLength = blake2bDigest.keyLength;
        this.key = Arrays.clone(blake2bDigest.key);
        this.digestLength = blake2bDigest.digestLength;
        this.chainValue = Arrays.clone(blake2bDigest.chainValue);
        this.personalization = Arrays.clone(blake2bDigest.personalization);
        this.salt = Arrays.clone(blake2bDigest.salt);
        this.t0 = blake2bDigest.t0;
        this.t1 = blake2bDigest.t1;
        this.f0 = blake2bDigest.f0;
    }

    public Blake2bDigest(byte[] bArr) {
        this.digestLength = 64;
        this.keyLength = 0;
        this.salt = null;
        this.personalization = null;
        this.key = null;
        this.buffer = null;
        this.bufferPos = 0;
        this.internalState = new long[16];
        this.chainValue = null;
        this.t0 = 0;
        this.t1 = 0;
        this.f0 = 0;
        this.buffer = new byte[128];
        if (bArr != null) {
            this.key = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.key, 0, bArr.length);
            if (bArr.length <= 64) {
                this.keyLength = bArr.length;
                System.arraycopy(bArr, 0, this.buffer, 0, bArr.length);
                this.bufferPos = 128;
            } else {
                throw new IllegalArgumentException("Keys > 64 are not supported");
            }
        }
        this.digestLength = 64;
        init();
    }

    public Blake2bDigest(byte[] bArr, int i, byte[] bArr2, byte[] bArr3) {
        this.digestLength = 64;
        this.keyLength = 0;
        this.salt = null;
        this.personalization = null;
        this.key = null;
        this.buffer = null;
        this.bufferPos = 0;
        this.internalState = new long[16];
        this.chainValue = null;
        this.t0 = 0;
        this.t1 = 0;
        this.f0 = 0;
        this.buffer = new byte[128];
        if (i < 1 || i > 64) {
            throw new IllegalArgumentException("Invalid digest length (required: 1 - 64)");
        }
        this.digestLength = i;
        if (bArr2 != null) {
            if (bArr2.length == 16) {
                this.salt = new byte[16];
                System.arraycopy(bArr2, 0, this.salt, 0, bArr2.length);
            } else {
                throw new IllegalArgumentException("salt length must be exactly 16 bytes");
            }
        }
        if (bArr3 != null) {
            if (bArr3.length == 16) {
                this.personalization = new byte[16];
                System.arraycopy(bArr3, 0, this.personalization, 0, bArr3.length);
            } else {
                throw new IllegalArgumentException("personalization length must be exactly 16 bytes");
            }
        }
        if (bArr != null) {
            this.key = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.key, 0, bArr.length);
            if (bArr.length <= 64) {
                this.keyLength = bArr.length;
                System.arraycopy(bArr, 0, this.buffer, 0, bArr.length);
                this.bufferPos = 128;
            } else {
                throw new IllegalArgumentException("Keys > 64 are not supported");
            }
        }
        init();
    }

    private void G(long j, long j2, int i, int i2, int i3, int i4) {
        this.internalState[i] = (this.internalState[i] + this.internalState[i2]) + j;
        this.internalState[i4] = rotr64(this.internalState[i4] ^ this.internalState[i], 32);
        this.internalState[i3] = this.internalState[i3] + this.internalState[i4];
        this.internalState[i2] = rotr64(this.internalState[i2] ^ this.internalState[i3], 24);
        this.internalState[i] = (this.internalState[i] + this.internalState[i2]) + j2;
        this.internalState[i4] = rotr64(this.internalState[i4] ^ this.internalState[i], 16);
        this.internalState[i3] = this.internalState[i3] + this.internalState[i4];
        this.internalState[i2] = rotr64(this.internalState[i2] ^ this.internalState[i3], 63);
    }

    private final long bytes2long(byte[] bArr, int i) {
        return ((((long) bArr[i + 7]) & 255) << 56) | (((((((((long) bArr[i]) & 255) | ((((long) bArr[i + 1]) & 255) << 8)) | ((((long) bArr[i + 2]) & 255) << 16)) | ((((long) bArr[i + 3]) & 255) << 24)) | ((((long) bArr[i + 4]) & 255) << 32)) | ((((long) bArr[i + 5]) & 255) << 40)) | ((((long) bArr[i + 6]) & 255) << 48));
    }

    private void compress(byte[] bArr, int i) {
        initializeInternalState();
        long[] jArr = new long[16];
        int i2 = 0;
        for (int i3 = 0; i3 < 16; i3++) {
            jArr[i3] = bytes2long(bArr, (i3 * 8) + i);
        }
        for (int i4 = 0; i4 < rOUNDS; i4++) {
            G(jArr[blake2b_sigma[i4][0]], jArr[blake2b_sigma[i4][1]], 0, 4, 8, 12);
            G(jArr[blake2b_sigma[i4][2]], jArr[blake2b_sigma[i4][3]], 1, 5, 9, 13);
            G(jArr[blake2b_sigma[i4][4]], jArr[blake2b_sigma[i4][5]], 2, 6, 10, 14);
            G(jArr[blake2b_sigma[i4][6]], jArr[blake2b_sigma[i4][7]], 3, 7, 11, 15);
            G(jArr[blake2b_sigma[i4][8]], jArr[blake2b_sigma[i4][9]], 0, 5, 10, 15);
            G(jArr[blake2b_sigma[i4][10]], jArr[blake2b_sigma[i4][11]], 1, 6, 11, 12);
            G(jArr[blake2b_sigma[i4][12]], jArr[blake2b_sigma[i4][13]], 2, 7, 8, 13);
            G(jArr[blake2b_sigma[i4][14]], jArr[blake2b_sigma[i4][15]], 3, 4, 9, 14);
        }
        while (i2 < this.chainValue.length) {
            this.chainValue[i2] = (this.chainValue[i2] ^ this.internalState[i2]) ^ this.internalState[i2 + 8];
            i2++;
        }
    }

    private void init() {
        if (this.chainValue == null) {
            long[] jArr;
            this.chainValue = new long[8];
            this.chainValue[0] = blake2b_IV[0] ^ ((long) ((this.digestLength | (this.keyLength << 8)) | 16842752));
            this.chainValue[1] = blake2b_IV[1];
            this.chainValue[2] = blake2b_IV[2];
            this.chainValue[3] = blake2b_IV[3];
            this.chainValue[4] = blake2b_IV[4];
            this.chainValue[5] = blake2b_IV[5];
            if (this.salt != null) {
                jArr = this.chainValue;
                jArr[4] = jArr[4] ^ bytes2long(this.salt, 0);
                jArr = this.chainValue;
                jArr[5] = jArr[5] ^ bytes2long(this.salt, 8);
            }
            this.chainValue[6] = blake2b_IV[6];
            this.chainValue[7] = blake2b_IV[7];
            if (this.personalization != null) {
                jArr = this.chainValue;
                jArr[6] = bytes2long(this.personalization, 0) ^ jArr[6];
                jArr = this.chainValue;
                jArr[7] = jArr[7] ^ bytes2long(this.personalization, 8);
            }
        }
    }

    private void initializeInternalState() {
        System.arraycopy(this.chainValue, 0, this.internalState, 0, this.chainValue.length);
        System.arraycopy(blake2b_IV, 0, this.internalState, this.chainValue.length, 4);
        this.internalState[12] = this.t0 ^ blake2b_IV[4];
        this.internalState[13] = this.t1 ^ blake2b_IV[5];
        this.internalState[14] = this.f0 ^ blake2b_IV[6];
        this.internalState[15] = blake2b_IV[7];
    }

    private final byte[] long2bytes(long j) {
        return new byte[]{(byte) ((int) j), (byte) ((int) (j >> 8)), (byte) ((int) (j >> 16)), (byte) ((int) (j >> 24)), (byte) ((int) (j >> 32)), (byte) ((int) (j >> 40)), (byte) ((int) (j >> 48)), (byte) ((int) (j >> 56))};
    }

    private long rotr64(long j, int i) {
        return (j << (64 - i)) | (j >>> i);
    }

    public void clearKey() {
        if (this.key != null) {
            Arrays.fill(this.key, (byte) 0);
            Arrays.fill(this.buffer, (byte) 0);
        }
    }

    public void clearSalt() {
        if (this.salt != null) {
            Arrays.fill(this.salt, (byte) 0);
        }
    }

    public int doFinal(byte[] bArr, int i) {
        this.f0 = -1;
        this.t0 += (long) this.bufferPos;
        if (this.bufferPos > 0 && this.t0 == 0) {
            this.t1++;
        }
        compress(this.buffer, 0);
        Arrays.fill(this.buffer, (byte) 0);
        Arrays.fill(this.internalState, 0);
        for (int i2 = 0; i2 < this.chainValue.length; i2++) {
            int i3 = i2 * 8;
            if (i3 >= this.digestLength) {
                break;
            }
            Object long2bytes = long2bytes(this.chainValue[i2]);
            if (i3 < this.digestLength - 8) {
                System.arraycopy(long2bytes, 0, bArr, i3 + i, 8);
            } else {
                System.arraycopy(long2bytes, 0, bArr, i + i3, this.digestLength - i3);
            }
        }
        Arrays.fill(this.chainValue, 0);
        reset();
        return this.digestLength;
    }

    public String getAlgorithmName() {
        return "Blake2b";
    }

    public int getByteLength() {
        return 128;
    }

    public int getDigestSize() {
        return this.digestLength;
    }

    public void reset() {
        this.bufferPos = 0;
        this.f0 = 0;
        this.t0 = 0;
        this.t1 = 0;
        this.chainValue = null;
        Arrays.fill(this.buffer, (byte) 0);
        if (this.key != null) {
            System.arraycopy(this.key, 0, this.buffer, 0, this.key.length);
            this.bufferPos = 128;
        }
        init();
    }

    public void update(byte b) {
        if (128 - this.bufferPos == 0) {
            this.t0 += 128;
            if (this.t0 == 0) {
                this.t1++;
            }
            compress(this.buffer, 0);
            Arrays.fill(this.buffer, (byte) 0);
            this.buffer[0] = b;
            this.bufferPos = 1;
            return;
        }
        this.buffer[this.bufferPos] = b;
        this.bufferPos++;
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x004e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void update(byte[] bArr, int i, int i2) {
        if (bArr != null && i2 != 0) {
            int i3;
            int i4;
            if (this.bufferPos != 0) {
                i3 = 128 - this.bufferPos;
                if (i3 < i2) {
                    System.arraycopy(bArr, i, this.buffer, this.bufferPos, i3);
                    this.t0 += 128;
                    if (this.t0 == 0) {
                        this.t1++;
                    }
                    compress(this.buffer, 0);
                    this.bufferPos = 0;
                    Arrays.fill(this.buffer, (byte) 0);
                    i2 += i;
                    i4 = i2 - 128;
                    i += i3;
                    while (i < i4) {
                        this.t0 += 128;
                        if (this.t0 == 0) {
                            this.t1++;
                        }
                        compress(bArr, i);
                        i += 128;
                    }
                    i2 -= i;
                    System.arraycopy(bArr, i, this.buffer, 0, i2);
                } else {
                    System.arraycopy(bArr, i, this.buffer, this.bufferPos, i2);
                }
            } else {
                i3 = 0;
                i2 += i;
                i4 = i2 - 128;
                i += i3;
                while (i < i4) {
                }
                i2 -= i;
                System.arraycopy(bArr, i, this.buffer, 0, i2);
            }
            this.bufferPos += i2;
        }
    }
}
