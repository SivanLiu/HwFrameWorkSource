package org.bouncycastle.crypto.engines;

import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.MaxBytesExceededException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Pack;
import org.bouncycastle.util.Strings;

public class Salsa20Engine implements SkippingStreamCipher {
    public static final int DEFAULT_ROUNDS = 20;
    private static final int STATE_SIZE = 16;
    private static final int[] TAU_SIGMA = Pack.littleEndianToInt(Strings.toByteArray("expand 16-byte kexpand 32-byte k"), 0, 8);
    protected static final byte[] sigma = Strings.toByteArray("expand 32-byte k");
    protected static final byte[] tau = Strings.toByteArray("expand 16-byte k");
    private int cW0;
    private int cW1;
    private int cW2;
    protected int[] engineState;
    private int index;
    private boolean initialised;
    private byte[] keyStream;
    protected int rounds;
    protected int[] x;

    public Salsa20Engine() {
        this(20);
    }

    public Salsa20Engine(int i) {
        this.index = 0;
        this.engineState = new int[16];
        this.x = new int[16];
        this.keyStream = new byte[64];
        this.initialised = false;
        if (i <= 0 || (i & 1) != 0) {
            throw new IllegalArgumentException("'rounds' must be a positive, even number");
        }
        this.rounds = i;
    }

    private boolean limitExceeded() {
        int i = this.cW0 + 1;
        this.cW0 = i;
        if (i == 0) {
            i = this.cW1 + 1;
            this.cW1 = i;
            if (i == 0) {
                i = this.cW2 + 1;
                this.cW2 = i;
                return (i & 32) != 0;
            }
        }
        return false;
    }

    private boolean limitExceeded(int i) {
        this.cW0 += i;
        if (this.cW0 < i && this.cW0 >= 0) {
            i = this.cW1 + 1;
            this.cW1 = i;
            if (i == 0) {
                i = this.cW2 + 1;
                this.cW2 = i;
                return (i & 32) != 0;
            }
        }
        return false;
    }

    private void resetLimitCounter() {
        this.cW0 = 0;
        this.cW1 = 0;
        this.cW2 = 0;
    }

    protected static int rotl(int i, int i2) {
        return (i >>> (-i2)) | (i << i2);
    }

    public static void salsaCore(int i, int[] iArr, int[] iArr2) {
        int[] iArr3 = iArr;
        int[] iArr4 = iArr2;
        if (iArr3.length != 16) {
            throw new IllegalArgumentException();
        } else if (iArr4.length != 16) {
            throw new IllegalArgumentException();
        } else if (i % 2 == 0) {
            int rotl;
            int i2 = 0;
            int i3 = iArr3[0];
            int i4 = iArr3[1];
            int i5 = iArr3[2];
            int i6 = iArr3[3];
            int i7 = iArr3[4];
            int i8 = iArr3[5];
            int i9 = iArr3[6];
            int i10 = 7;
            int i11 = iArr3[7];
            int i12 = iArr3[8];
            int i13 = 9;
            int i14 = iArr3[9];
            int i15 = iArr3[10];
            int i16 = iArr3[11];
            int i17 = iArr3[12];
            int i18 = 13;
            int i19 = iArr3[13];
            int i20 = iArr3[14];
            int i21 = iArr3[15];
            int i22 = i;
            while (i22 > 0) {
                int rotl2 = rotl(i3 + i17, i10) ^ i7;
                i7 = i12 ^ rotl(rotl2 + i3, i13);
                int rotl3 = i17 ^ rotl(i7 + rotl2, i18);
                i3 ^= rotl(rotl3 + i7, 18);
                int rotl4 = i14 ^ rotl(i8 + i4, i10);
                i10 = i19 ^ rotl(rotl4 + i8, i13);
                i4 ^= rotl(i10 + rotl4, i18);
                i13 = rotl(i4 + i10, 18) ^ i8;
                i2 = i20 ^ rotl(i15 + i9, 7);
                i5 ^= rotl(i2 + i15, 9);
                i18 = i9 ^ rotl(i5 + i2, 13);
                rotl = i15 ^ rotl(i18 + i5, 18);
                int rotl5 = i6 ^ rotl(i21 + i16, 7);
                i6 = i11 ^ rotl(rotl5 + i21, 9);
                int i23 = i22;
                i22 = i16 ^ rotl(i6 + rotl5, 13);
                i8 = i21 ^ rotl(i22 + i6, 18);
                int i24 = rotl3;
                i4 ^= rotl(i3 + rotl5, 7);
                i5 ^= rotl(i4 + i3, 9);
                rotl5 ^= rotl(i5 + i4, 13);
                i3 ^= rotl(rotl5 + i5, 18);
                i9 = i18 ^ rotl(i13 + rotl2, 7);
                i11 = i6 ^ rotl(i9 + i13, 9);
                rotl3 = rotl(i11 + i9, 13) ^ rotl2;
                i16 = i22 ^ rotl(rotl + rotl4, 7);
                i12 = i7 ^ rotl(i16 + rotl, 9);
                i14 = rotl4 ^ rotl(i12 + i16, 13);
                i15 = rotl ^ rotl(i14 + i12, 18);
                i17 = i24 ^ rotl(i8 + i2, 7);
                i19 = i10 ^ rotl(i17 + i8, 9);
                i20 = i2 ^ rotl(i19 + i17, 13);
                i21 = i8 ^ rotl(i20 + i19, 18);
                i22 = i23 - 2;
                i6 = rotl5;
                i7 = rotl3;
                i8 = rotl(rotl3 + i11, 18) ^ i13;
                iArr3 = iArr;
                iArr4 = iArr2;
                i2 = 0;
                i18 = 13;
                i13 = 9;
                i10 = 7;
            }
            rotl = i2;
            int[] iArr5 = iArr2;
            iArr5[rotl] = i3 + iArr3[rotl];
            iArr5[1] = i4 + iArr3[1];
            iArr5[2] = i5 + iArr3[2];
            iArr5[3] = i6 + iArr3[3];
            iArr5[4] = i7 + iArr3[4];
            iArr5[5] = i8 + iArr3[5];
            iArr5[6] = i9 + iArr3[6];
            iArr5[7] = i11 + iArr3[7];
            iArr5[8] = i12 + iArr3[8];
            iArr5[9] = i14 + iArr3[9];
            iArr5[10] = i15 + iArr3[10];
            iArr5[11] = i16 + iArr3[11];
            iArr5[12] = i17 + iArr3[12];
            iArr5[13] = i19 + iArr3[13];
            iArr5[14] = i20 + iArr3[14];
            iArr5[15] = i21 + iArr3[15];
        } else {
            throw new IllegalArgumentException("Number of rounds must be even");
        }
    }

    protected void advanceCounter() {
        int[] iArr = this.engineState;
        int i = iArr[8] + 1;
        iArr[8] = i;
        if (i == 0) {
            iArr = this.engineState;
            iArr[9] = iArr[9] + 1;
        }
    }

    protected void advanceCounter(long j) {
        int i = (int) (j >>> 32);
        int i2 = (int) j;
        if (i > 0) {
            int[] iArr = this.engineState;
            iArr[9] = iArr[9] + i;
        }
        i = this.engineState[8];
        int[] iArr2 = this.engineState;
        iArr2[8] = iArr2[8] + i2;
        if (i != 0 && this.engineState[8] < i) {
            int[] iArr3 = this.engineState;
            iArr3[9] = iArr3[9] + 1;
        }
    }

    protected void generateKeyStream(byte[] bArr) {
        salsaCore(this.rounds, this.engineState, this.x);
        Pack.intToLittleEndian(this.x, bArr, 0);
    }

    public String getAlgorithmName() {
        String str = "Salsa20";
        if (this.rounds == 20) {
            return str;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("/");
        stringBuilder.append(this.rounds);
        return stringBuilder.toString();
    }

    protected long getCounter() {
        return (((long) this.engineState[9]) << 32) | (((long) this.engineState[8]) & BodyPartID.bodyIdMax);
    }

    protected int getNonceSize() {
        return 8;
    }

    public long getPosition() {
        return (getCounter() * 64) + ((long) this.index);
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        StringBuilder stringBuilder;
        if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            byte[] iv = parametersWithIV.getIV();
            if (iv == null || iv.length != getNonceSize()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(getAlgorithmName());
                stringBuilder.append(" requires exactly ");
                stringBuilder.append(getNonceSize());
                stringBuilder.append(" bytes of IV");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            byte[] bArr;
            cipherParameters = parametersWithIV.getParameters();
            if (cipherParameters == null) {
                if (this.initialised) {
                    bArr = null;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(getAlgorithmName());
                    stringBuilder.append(" KeyParameter can not be null for first initialisation");
                    throw new IllegalStateException(stringBuilder.toString());
                }
            } else if (cipherParameters instanceof KeyParameter) {
                bArr = ((KeyParameter) cipherParameters).getKey();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(getAlgorithmName());
                stringBuilder.append(" Init parameters must contain a KeyParameter (or null for re-init)");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            setKey(bArr, iv);
            reset();
            this.initialised = true;
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(getAlgorithmName());
        stringBuilder.append(" Init parameters must include an IV");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    protected void packTauOrSigma(int i, int[] iArr, int i2) {
        i = (i - 16) / 4;
        iArr[i2] = TAU_SIGMA[i];
        iArr[i2 + 1] = TAU_SIGMA[i + 1];
        iArr[i2 + 2] = TAU_SIGMA[i + 2];
        iArr[i2 + 3] = TAU_SIGMA[i + 3];
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        if (!this.initialised) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getAlgorithmName());
            stringBuilder.append(" not initialised");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (i + i2 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i3 + i2 > bArr2.length) {
            throw new OutputLengthException("output buffer too short");
        } else if (limitExceeded(i2)) {
            throw new MaxBytesExceededException("2^70 byte limit per IV would be exceeded; Change IV");
        } else {
            for (int i4 = 0; i4 < i2; i4++) {
                bArr2[i4 + i3] = (byte) (this.keyStream[this.index] ^ bArr[i4 + i]);
                this.index = (this.index + 1) & 63;
                if (this.index == 0) {
                    advanceCounter();
                    generateKeyStream(this.keyStream);
                }
            }
            return i2;
        }
    }

    public void reset() {
        this.index = 0;
        resetLimitCounter();
        resetCounter();
        generateKeyStream(this.keyStream);
    }

    protected void resetCounter() {
        int[] iArr = this.engineState;
        this.engineState[9] = 0;
        iArr[8] = 0;
    }

    protected void retreatCounter() {
        if (this.engineState[8] == 0 && this.engineState[9] == 0) {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }
        int[] iArr = this.engineState;
        int i = iArr[8] - 1;
        iArr[8] = i;
        if (i == -1) {
            iArr = this.engineState;
            iArr[9] = iArr[9] - 1;
        }
    }

    protected void retreatCounter(long j) {
        int i = (int) (j >>> 32);
        int i2 = (int) j;
        if (i != 0) {
            if ((((long) this.engineState[9]) & BodyPartID.bodyIdMax) >= (((long) i) & BodyPartID.bodyIdMax)) {
                int[] iArr = this.engineState;
                iArr[9] = iArr[9] - i;
            } else {
                throw new IllegalStateException("attempt to reduce counter past zero.");
            }
        }
        int[] iArr2;
        if ((((long) this.engineState[8]) & BodyPartID.bodyIdMax) >= (((long) i2) & BodyPartID.bodyIdMax)) {
            iArr2 = this.engineState;
            iArr2[8] = iArr2[8] - i2;
        } else if (this.engineState[9] != 0) {
            int[] iArr3 = this.engineState;
            iArr3[9] = iArr3[9] - 1;
            iArr2 = this.engineState;
            iArr2[8] = iArr2[8] - i2;
        } else {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }
    }

    public byte returnByte(byte b) {
        if (limitExceeded()) {
            throw new MaxBytesExceededException("2^70 byte limit per IV; Change IV");
        }
        b = (byte) (b ^ this.keyStream[this.index]);
        this.index = (this.index + 1) & 63;
        if (this.index == 0) {
            advanceCounter();
            generateKeyStream(this.keyStream);
        }
        return b;
    }

    public long seekTo(long j) {
        reset();
        return skip(j);
    }

    protected void setKey(byte[] bArr, byte[] bArr2) {
        if (bArr != null) {
            if (bArr.length == 16 || bArr.length == 32) {
                int length = (bArr.length - 16) / 4;
                this.engineState[0] = TAU_SIGMA[length];
                this.engineState[5] = TAU_SIGMA[length + 1];
                this.engineState[10] = TAU_SIGMA[length + 2];
                this.engineState[15] = TAU_SIGMA[length + 3];
                Pack.littleEndianToInt(bArr, 0, this.engineState, 1, 4);
                Pack.littleEndianToInt(bArr, bArr.length - 16, this.engineState, 11, 4);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getAlgorithmName());
                stringBuilder.append(" requires 128 bit or 256 bit key");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        Pack.littleEndianToInt(bArr2, 0, this.engineState, 6, 2);
    }

    public long skip(long j) {
        long j2 = 0;
        if (j >= 0) {
            if (j >= 64) {
                j2 = j / 64;
                advanceCounter(j2);
                j2 = j - (j2 * 64);
            } else {
                j2 = j;
            }
            int i = this.index;
            this.index = (this.index + ((int) j2)) & 63;
            if (this.index < i) {
                advanceCounter();
            }
        } else {
            long j3 = -j;
            if (j3 >= 64) {
                long j4 = j3 / 64;
                retreatCounter(j4);
                j3 -= j4 * 64;
            }
            while (j2 < j3) {
                if (this.index == 0) {
                    retreatCounter();
                }
                this.index = (this.index - 1) & 63;
                j2++;
            }
        }
        generateKeyStream(this.keyStream);
        return j;
    }
}
