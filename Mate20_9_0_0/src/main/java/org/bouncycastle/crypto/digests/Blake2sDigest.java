package org.bouncycastle.crypto.digests;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.Arrays;

public class Blake2sDigest implements ExtendedDigest {
    private static final int BLOCK_LENGTH_BYTES = 64;
    private static final int ROUNDS = 10;
    private static final int[] blake2s_IV = new int[]{1779033703, -1150833019, 1013904242, -1521486534, 1359893119, -1694144372, 528734635, 1541459225};
    private static final byte[][] blake2s_sigma = new byte[][]{new byte[]{(byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15}, new byte[]{(byte) 14, (byte) 10, (byte) 4, (byte) 8, (byte) 9, (byte) 15, (byte) 13, (byte) 6, (byte) 1, (byte) 12, (byte) 0, (byte) 2, (byte) 11, (byte) 7, (byte) 5, (byte) 3}, new byte[]{(byte) 11, (byte) 8, (byte) 12, (byte) 0, (byte) 5, (byte) 2, (byte) 15, (byte) 13, (byte) 10, (byte) 14, (byte) 3, (byte) 6, (byte) 7, (byte) 1, (byte) 9, (byte) 4}, new byte[]{(byte) 7, (byte) 9, (byte) 3, (byte) 1, (byte) 13, (byte) 12, (byte) 11, (byte) 14, (byte) 2, (byte) 6, (byte) 5, (byte) 10, (byte) 4, (byte) 0, (byte) 15, (byte) 8}, new byte[]{(byte) 9, (byte) 0, (byte) 5, (byte) 7, (byte) 2, (byte) 4, (byte) 10, (byte) 15, (byte) 14, (byte) 1, (byte) 11, (byte) 12, (byte) 6, (byte) 8, (byte) 3, (byte) 13}, new byte[]{(byte) 2, (byte) 12, (byte) 6, (byte) 10, (byte) 0, (byte) 11, (byte) 8, (byte) 3, (byte) 4, (byte) 13, (byte) 7, (byte) 5, (byte) 15, (byte) 14, (byte) 1, (byte) 9}, new byte[]{(byte) 12, (byte) 5, (byte) 1, (byte) 15, (byte) 14, (byte) 13, (byte) 4, (byte) 10, (byte) 0, (byte) 7, (byte) 6, (byte) 3, (byte) 9, (byte) 2, (byte) 8, (byte) 11}, new byte[]{(byte) 13, (byte) 11, (byte) 7, (byte) 14, (byte) 12, (byte) 1, (byte) 3, (byte) 9, (byte) 5, (byte) 0, (byte) 15, (byte) 4, (byte) 8, (byte) 6, (byte) 2, (byte) 10}, new byte[]{(byte) 6, (byte) 15, (byte) 14, (byte) 9, (byte) 11, (byte) 3, (byte) 0, (byte) 8, (byte) 12, (byte) 2, (byte) 13, (byte) 7, (byte) 1, (byte) 4, (byte) 10, (byte) 5}, new byte[]{(byte) 10, (byte) 2, (byte) 8, (byte) 4, (byte) 7, (byte) 6, (byte) 1, (byte) 5, (byte) 15, (byte) 11, (byte) 9, (byte) 14, (byte) 3, (byte) 12, (byte) 13, (byte) 0}};
    private byte[] buffer;
    private int bufferPos;
    private int[] chainValue;
    private int digestLength;
    private int f0;
    private int[] internalState;
    private byte[] key;
    private int keyLength;
    private byte[] personalization;
    private byte[] salt;
    private int t0;
    private int t1;

    public Blake2sDigest() {
        this(256);
    }

    public Blake2sDigest(int i) {
        this.digestLength = 32;
        this.keyLength = 0;
        this.salt = null;
        this.personalization = null;
        this.key = null;
        this.buffer = null;
        this.bufferPos = 0;
        this.internalState = new int[16];
        this.chainValue = null;
        this.t0 = 0;
        this.t1 = 0;
        this.f0 = 0;
        if (i == 128 || i == CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256 || i == 224 || i == 256) {
            this.buffer = new byte[64];
            this.keyLength = 0;
            this.digestLength = i / 8;
            init();
            return;
        }
        throw new IllegalArgumentException("BLAKE2s digest restricted to one of [128, 160, 224, 256]");
    }

    public Blake2sDigest(Blake2sDigest blake2sDigest) {
        this.digestLength = 32;
        this.keyLength = 0;
        this.salt = null;
        this.personalization = null;
        this.key = null;
        this.buffer = null;
        this.bufferPos = 0;
        this.internalState = new int[16];
        this.chainValue = null;
        this.t0 = 0;
        this.t1 = 0;
        this.f0 = 0;
        this.bufferPos = blake2sDigest.bufferPos;
        this.buffer = Arrays.clone(blake2sDigest.buffer);
        this.keyLength = blake2sDigest.keyLength;
        this.key = Arrays.clone(blake2sDigest.key);
        this.digestLength = blake2sDigest.digestLength;
        this.chainValue = Arrays.clone(blake2sDigest.chainValue);
        this.personalization = Arrays.clone(blake2sDigest.personalization);
    }

    public Blake2sDigest(byte[] bArr) {
        this.digestLength = 32;
        this.keyLength = 0;
        this.salt = null;
        this.personalization = null;
        this.key = null;
        this.buffer = null;
        this.bufferPos = 0;
        this.internalState = new int[16];
        this.chainValue = null;
        this.t0 = 0;
        this.t1 = 0;
        this.f0 = 0;
        this.buffer = new byte[64];
        if (bArr != null) {
            if (bArr.length <= 32) {
                this.key = new byte[bArr.length];
                System.arraycopy(bArr, 0, this.key, 0, bArr.length);
                this.keyLength = bArr.length;
                System.arraycopy(bArr, 0, this.buffer, 0, bArr.length);
                this.bufferPos = 64;
            } else {
                throw new IllegalArgumentException("Keys > 32 are not supported");
            }
        }
        this.digestLength = 32;
        init();
    }

    public Blake2sDigest(byte[] bArr, int i, byte[] bArr2, byte[] bArr3) {
        this.digestLength = 32;
        this.keyLength = 0;
        this.salt = null;
        this.personalization = null;
        this.key = null;
        this.buffer = null;
        this.bufferPos = 0;
        this.internalState = new int[16];
        this.chainValue = null;
        this.t0 = 0;
        this.t1 = 0;
        this.f0 = 0;
        this.buffer = new byte[64];
        if (i < 1 || i > 32) {
            throw new IllegalArgumentException("Invalid digest length (required: 1 - 32)");
        }
        this.digestLength = i;
        if (bArr2 != null) {
            if (bArr2.length == 8) {
                this.salt = new byte[8];
                System.arraycopy(bArr2, 0, this.salt, 0, bArr2.length);
            } else {
                throw new IllegalArgumentException("Salt length must be exactly 8 bytes");
            }
        }
        if (bArr3 != null) {
            if (bArr3.length == 8) {
                this.personalization = new byte[8];
                System.arraycopy(bArr3, 0, this.personalization, 0, bArr3.length);
            } else {
                throw new IllegalArgumentException("Personalization length must be exactly 8 bytes");
            }
        }
        if (bArr != null) {
            if (bArr.length <= 32) {
                this.key = new byte[bArr.length];
                System.arraycopy(bArr, 0, this.key, 0, bArr.length);
                this.keyLength = bArr.length;
                System.arraycopy(bArr, 0, this.buffer, 0, bArr.length);
                this.bufferPos = 64;
            } else {
                throw new IllegalArgumentException("Keys > 32 bytes are not supported");
            }
        }
        init();
    }

    private void G(int i, int i2, int i3, int i4, int i5, int i6) {
        this.internalState[i3] = (this.internalState[i3] + this.internalState[i4]) + i;
        this.internalState[i6] = rotr32(this.internalState[i6] ^ this.internalState[i3], 16);
        this.internalState[i5] = this.internalState[i5] + this.internalState[i6];
        this.internalState[i4] = rotr32(this.internalState[i4] ^ this.internalState[i5], 12);
        this.internalState[i3] = (this.internalState[i3] + this.internalState[i4]) + i2;
        this.internalState[i6] = rotr32(this.internalState[i6] ^ this.internalState[i3], 8);
        this.internalState[i5] = this.internalState[i5] + this.internalState[i6];
        this.internalState[i4] = rotr32(this.internalState[i4] ^ this.internalState[i5], 7);
    }

    private int bytes2int(byte[] bArr, int i) {
        return ((bArr[i + 3] & 255) << 24) | (((bArr[i] & 255) | ((bArr[i + 1] & 255) << 8)) | ((bArr[i + 2] & 255) << 16));
    }

    private void compress(byte[] bArr, int i) {
        initializeInternalState();
        int[] iArr = new int[16];
        int i2 = 0;
        for (int i3 = 0; i3 < 16; i3++) {
            iArr[i3] = bytes2int(bArr, (i3 * 4) + i);
        }
        for (int i4 = 0; i4 < 10; i4++) {
            G(iArr[blake2s_sigma[i4][0]], iArr[blake2s_sigma[i4][1]], 0, 4, 8, 12);
            G(iArr[blake2s_sigma[i4][2]], iArr[blake2s_sigma[i4][3]], 1, 5, 9, 13);
            G(iArr[blake2s_sigma[i4][4]], iArr[blake2s_sigma[i4][5]], 2, 6, 10, 14);
            G(iArr[blake2s_sigma[i4][6]], iArr[blake2s_sigma[i4][7]], 3, 7, 11, 15);
            G(iArr[blake2s_sigma[i4][8]], iArr[blake2s_sigma[i4][9]], 0, 5, 10, 15);
            G(iArr[blake2s_sigma[i4][10]], iArr[blake2s_sigma[i4][11]], 1, 6, 11, 12);
            G(iArr[blake2s_sigma[i4][12]], iArr[blake2s_sigma[i4][13]], 2, 7, 8, 13);
            G(iArr[blake2s_sigma[i4][14]], iArr[blake2s_sigma[i4][15]], 3, 4, 9, 14);
        }
        while (i2 < this.chainValue.length) {
            this.chainValue[i2] = (this.chainValue[i2] ^ this.internalState[i2]) ^ this.internalState[i2 + 8];
            i2++;
        }
    }

    private void init() {
        if (this.chainValue == null) {
            int[] iArr;
            this.chainValue = new int[8];
            this.chainValue[0] = (((this.keyLength << 8) | this.digestLength) | 16842752) ^ blake2s_IV[0];
            this.chainValue[1] = blake2s_IV[1];
            this.chainValue[2] = blake2s_IV[2];
            this.chainValue[3] = blake2s_IV[3];
            this.chainValue[4] = blake2s_IV[4];
            this.chainValue[5] = blake2s_IV[5];
            if (this.salt != null) {
                iArr = this.chainValue;
                iArr[4] = iArr[4] ^ bytes2int(this.salt, 0);
                iArr = this.chainValue;
                iArr[5] = iArr[5] ^ bytes2int(this.salt, 4);
            }
            this.chainValue[6] = blake2s_IV[6];
            this.chainValue[7] = blake2s_IV[7];
            if (this.personalization != null) {
                iArr = this.chainValue;
                iArr[6] = iArr[6] ^ bytes2int(this.personalization, 0);
                iArr = this.chainValue;
                iArr[7] = iArr[7] ^ bytes2int(this.personalization, 4);
            }
        }
    }

    private void initializeInternalState() {
        System.arraycopy(this.chainValue, 0, this.internalState, 0, this.chainValue.length);
        System.arraycopy(blake2s_IV, 0, this.internalState, this.chainValue.length, 4);
        this.internalState[12] = this.t0 ^ blake2s_IV[4];
        this.internalState[13] = this.t1 ^ blake2s_IV[5];
        this.internalState[14] = this.f0 ^ blake2s_IV[6];
        this.internalState[15] = blake2s_IV[7];
    }

    private byte[] int2bytes(int i) {
        return new byte[]{(byte) i, (byte) (i >> 8), (byte) (i >> 16), (byte) (i >> 24)};
    }

    private int rotr32(int i, int i2) {
        return (i << (32 - i2)) | (i >>> i2);
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
        this.t0 += this.bufferPos;
        if (this.t0 < 0 && this.bufferPos > (-this.t0)) {
            this.t1++;
        }
        compress(this.buffer, 0);
        Arrays.fill(this.buffer, (byte) 0);
        Arrays.fill(this.internalState, 0);
        for (int i2 = 0; i2 < this.chainValue.length; i2++) {
            int i3 = i2 * 4;
            if (i3 >= this.digestLength) {
                break;
            }
            Object int2bytes = int2bytes(this.chainValue[i2]);
            if (i3 < this.digestLength - 4) {
                System.arraycopy(int2bytes, 0, bArr, i3 + i, 4);
            } else {
                System.arraycopy(int2bytes, 0, bArr, i + i3, this.digestLength - i3);
            }
        }
        Arrays.fill(this.chainValue, 0);
        reset();
        return this.digestLength;
    }

    public String getAlgorithmName() {
        return "BLAKE2s";
    }

    public int getByteLength() {
        return 64;
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
            this.bufferPos = 64;
        }
        init();
    }

    public void update(byte b) {
        if (64 - this.bufferPos == 0) {
            this.t0 += 64;
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

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0048  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void update(byte[] bArr, int i, int i2) {
        if (bArr != null && i2 != 0) {
            Object obj;
            int i3 = 0;
            int i4;
            int i5;
            if (this.bufferPos != 0) {
                i4 = 64 - this.bufferPos;
                if (i4 < i2) {
                    System.arraycopy(bArr, i, this.buffer, this.bufferPos, i4);
                    this.t0 += 64;
                    if (this.t0 == 0) {
                        this.t1++;
                    }
                    compress(this.buffer, 0);
                    this.bufferPos = 0;
                    Arrays.fill(this.buffer, (byte) 0);
                    i2 += i;
                    i5 = i2 - 64;
                    i += i4;
                    while (i < i5) {
                        this.t0 += 64;
                        if (this.t0 == 0) {
                            this.t1++;
                        }
                        compress(bArr, i);
                        i += 64;
                    }
                    obj = this.buffer;
                    i2 -= i;
                } else {
                    obj = this.buffer;
                    i3 = this.bufferPos;
                }
            } else {
                i4 = 0;
                i2 += i;
                i5 = i2 - 64;
                i += i4;
                while (i < i5) {
                }
                obj = this.buffer;
                i2 -= i;
            }
            System.arraycopy(bArr, i, obj, i3, i2);
            this.bufferPos += i2;
        }
    }
}
