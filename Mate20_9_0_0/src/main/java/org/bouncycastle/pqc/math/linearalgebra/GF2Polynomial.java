package org.bouncycastle.pqc.math.linearalgebra;

import java.math.BigInteger;
import java.util.Random;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.eac.CertificateBody;
import org.bouncycastle.crypto.tls.CipherSuite;

public class GF2Polynomial {
    private static final int[] bitMask = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, PKIFailureInfo.certConfirmed, PKIFailureInfo.certRevoked, 16384, 32768, PKIFailureInfo.notAuthorized, PKIFailureInfo.unsupportedVersion, PKIFailureInfo.transactionIdInUse, PKIFailureInfo.signerNotTrusted, PKIFailureInfo.badCertTemplate, PKIFailureInfo.badSenderNonce, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, PKIFailureInfo.duplicateCertReq, 1073741824, PKIFailureInfo.systemUnavail, 0};
    private static final boolean[] parity = new boolean[]{false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true, false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false};
    private static Random rand = new Random();
    private static final int[] reverseRightMask = new int[]{0, 1, 3, 7, 15, 31, 63, CertificateBody.profileType, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, Integer.MAX_VALUE, -1};
    private static final short[] squaringTable = new short[]{(short) 0, (short) 1, (short) 4, (short) 5, (short) 16, (short) 17, (short) 20, (short) 21, (short) 64, (short) 65, (short) 68, (short) 69, (short) 80, (short) 81, (short) 84, (short) 85, (short) 256, (short) 257, (short) 260, (short) 261, (short) 272, (short) 273, (short) 276, (short) 277, (short) 320, (short) 321, (short) 324, (short) 325, (short) 336, (short) 337, (short) 340, (short) 341, (short) 1024, (short) 1025, (short) 1028, (short) 1029, (short) 1040, (short) 1041, (short) 1044, (short) 1045, (short) 1088, (short) 1089, (short) 1092, (short) 1093, (short) 1104, (short) 1105, (short) 1108, (short) 1109, (short) 1280, (short) 1281, (short) 1284, (short) 1285, (short) 1296, (short) 1297, (short) 1300, (short) 1301, (short) 1344, (short) 1345, (short) 1348, (short) 1349, (short) 1360, (short) 1361, (short) 1364, (short) 1365, (short) 4096, (short) 4097, (short) 4100, (short) 4101, (short) 4112, (short) 4113, (short) 4116, (short) 4117, (short) 4160, (short) 4161, (short) 4164, (short) 4165, (short) 4176, (short) 4177, (short) 4180, (short) 4181, (short) 4352, (short) 4353, (short) 4356, (short) 4357, (short) 4368, (short) 4369, (short) 4372, (short) 4373, (short) 4416, (short) 4417, (short) 4420, (short) 4421, (short) 4432, (short) 4433, (short) 4436, (short) 4437, (short) 5120, (short) 5121, (short) 5124, (short) 5125, (short) 5136, (short) 5137, (short) 5140, (short) 5141, (short) 5184, (short) 5185, (short) 5188, (short) 5189, (short) 5200, (short) 5201, (short) 5204, (short) 5205, (short) 5376, (short) 5377, (short) 5380, (short) 5381, (short) 5392, (short) 5393, (short) 5396, (short) 5397, (short) 5440, (short) 5441, (short) 5444, (short) 5445, (short) 5456, (short) 5457, (short) 5460, (short) 5461, (short) 16384, (short) 16385, (short) 16388, (short) 16389, (short) 16400, (short) 16401, (short) 16404, (short) 16405, (short) 16448, (short) 16449, (short) 16452, (short) 16453, (short) 16464, (short) 16465, (short) 16468, (short) 16469, (short) 16640, (short) 16641, (short) 16644, (short) 16645, (short) 16656, (short) 16657, (short) 16660, (short) 16661, (short) 16704, (short) 16705, (short) 16708, (short) 16709, (short) 16720, (short) 16721, (short) 16724, (short) 16725, (short) 17408, (short) 17409, (short) 17412, (short) 17413, (short) 17424, (short) 17425, (short) 17428, (short) 17429, (short) 17472, (short) 17473, (short) 17476, (short) 17477, (short) 17488, (short) 17489, (short) 17492, (short) 17493, (short) 17664, (short) 17665, (short) 17668, (short) 17669, (short) 17680, (short) 17681, (short) 17684, (short) 17685, (short) 17728, (short) 17729, (short) 17732, (short) 17733, (short) 17744, (short) 17745, (short) 17748, (short) 17749, (short) 20480, (short) 20481, (short) 20484, (short) 20485, (short) 20496, (short) 20497, (short) 20500, (short) 20501, (short) 20544, (short) 20545, (short) 20548, (short) 20549, (short) 20560, (short) 20561, (short) 20564, (short) 20565, (short) 20736, (short) 20737, (short) 20740, (short) 20741, (short) 20752, (short) 20753, (short) 20756, (short) 20757, (short) 20800, (short) 20801, (short) 20804, (short) 20805, (short) 20816, (short) 20817, (short) 20820, (short) 20821, (short) 21504, (short) 21505, (short) 21508, (short) 21509, (short) 21520, (short) 21521, (short) 21524, (short) 21525, (short) 21568, (short) 21569, (short) 21572, (short) 21573, (short) 21584, (short) 21585, (short) 21588, (short) 21589, (short) 21760, (short) 21761, (short) 21764, (short) 21765, (short) 21776, (short) 21777, (short) 21780, (short) 21781, (short) 21824, (short) 21825, (short) 21828, (short) 21829, (short) 21840, (short) 21841, (short) 21844, (short) 21845};
    private int blocks;
    private int len;
    private int[] value;

    public GF2Polynomial(int i) {
        if (i < 1) {
            i = 1;
        }
        this.blocks = ((i - 1) >> 5) + 1;
        this.value = new int[this.blocks];
        this.len = i;
    }

    public GF2Polynomial(int i, String str) {
        if (i < 1) {
            i = 1;
        }
        this.blocks = ((i - 1) >> 5) + 1;
        this.value = new int[this.blocks];
        this.len = i;
        if (str.equalsIgnoreCase("ZERO")) {
            assignZero();
        } else if (str.equalsIgnoreCase("ONE")) {
            assignOne();
        } else if (str.equalsIgnoreCase("RANDOM")) {
            randomize();
        } else if (str.equalsIgnoreCase("X")) {
            assignX();
        } else if (str.equalsIgnoreCase("ALL")) {
            assignAll();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: GF2Polynomial was called using ");
            stringBuilder.append(str);
            stringBuilder.append(" as value!");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public GF2Polynomial(int i, BigInteger bigInteger) {
        if (i < 1) {
            i = 1;
        }
        this.blocks = ((i - 1) >> 5) + 1;
        this.value = new int[this.blocks];
        this.len = i;
        byte[] toByteArray = bigInteger.toByteArray();
        int i2 = 0;
        if (toByteArray[0] == (byte) 0) {
            byte[] bArr = new byte[(toByteArray.length - 1)];
            System.arraycopy(toByteArray, 1, bArr, 0, bArr.length);
            toByteArray = bArr;
        }
        int length = toByteArray.length & 3;
        int length2 = ((toByteArray.length - 1) >> 2) + 1;
        for (int i3 = 0; i3 < length; i3++) {
            int[] iArr = this.value;
            int i4 = length2 - 1;
            iArr[i4] = iArr[i4] | ((toByteArray[i3] & 255) << (((length - 1) - i3) << 3));
        }
        while (i2 <= ((toByteArray.length - 4) >> 2)) {
            length = (toByteArray.length - 1) - (i2 << 2);
            this.value[i2] = toByteArray[length] & 255;
            int[] iArr2 = this.value;
            iArr2[i2] = iArr2[i2] | ((toByteArray[length - 1] << 8) & CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB);
            iArr2 = this.value;
            iArr2[i2] = iArr2[i2] | ((toByteArray[length - 2] << 16) & 16711680);
            iArr2 = this.value;
            iArr2[i2] = ((toByteArray[length - 3] << 24) & -16777216) | iArr2[i2];
            i2++;
        }
        if ((this.len & 31) != 0) {
            int[] iArr3 = this.value;
            i2 = this.blocks - 1;
            iArr3[i2] = iArr3[i2] & reverseRightMask[this.len & 31];
        }
        reduceN();
    }

    public GF2Polynomial(int i, Random random) {
        if (i < 1) {
            i = 1;
        }
        this.blocks = ((i - 1) >> 5) + 1;
        this.value = new int[this.blocks];
        this.len = i;
        randomize(random);
    }

    public GF2Polynomial(int i, byte[] bArr) {
        int i2;
        int[] iArr;
        if (i < 1) {
            i = 1;
        }
        this.blocks = ((i - 1) >> 5) + 1;
        this.value = new int[this.blocks];
        this.len = i;
        i = Math.min(((bArr.length - 1) >> 2) + 1, this.blocks);
        int i3 = 0;
        while (true) {
            i2 = i - 1;
            if (i3 >= i2) {
                break;
            }
            i2 = (bArr.length - (i3 << 2)) - 1;
            this.value[i3] = bArr[i2] & 255;
            int[] iArr2 = this.value;
            iArr2[i3] = (CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB & (bArr[i2 - 1] << 8)) | iArr2[i3];
            int[] iArr3 = this.value;
            iArr3[i3] = (16711680 & (bArr[i2 - 2] << 16)) | iArr3[i3];
            int[] iArr4 = this.value;
            iArr4[i3] = ((bArr[i2 - 3] << 24) & -16777216) | iArr4[i3];
            i3++;
        }
        i = (bArr.length - (i2 << 2)) - 1;
        this.value[i2] = bArr[i] & 255;
        if (i > 0) {
            int[] iArr5 = this.value;
            iArr5[i2] = (CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB & (bArr[i - 1] << 8)) | iArr5[i2];
        }
        if (i > 1) {
            iArr = this.value;
            iArr[i2] = iArr[i2] | (16711680 & (bArr[i - 2] << 16));
        }
        if (i > 2) {
            iArr = this.value;
            iArr[i2] = ((bArr[i - 3] << 24) & -16777216) | iArr[i2];
        }
        zeroUnusedBits();
        reduceN();
    }

    public GF2Polynomial(int i, int[] iArr) {
        if (i < 1) {
            i = 1;
        }
        this.blocks = ((i - 1) >> 5) + 1;
        this.value = new int[this.blocks];
        this.len = i;
        System.arraycopy(iArr, 0, this.value, 0, Math.min(this.blocks, iArr.length));
        zeroUnusedBits();
    }

    public GF2Polynomial(GF2Polynomial gF2Polynomial) {
        this.len = gF2Polynomial.len;
        this.blocks = gF2Polynomial.blocks;
        this.value = IntUtils.clone(gF2Polynomial.value);
    }

    private void doShiftBlocksLeft(int i) {
        if (this.blocks <= this.value.length) {
            int i2;
            for (i2 = this.blocks - 1; i2 >= i; i2--) {
                this.value[i2] = this.value[i2 - i];
            }
            for (i2 = 0; i2 < i; i2++) {
                this.value[i2] = 0;
            }
            return;
        }
        int[] iArr = new int[this.blocks];
        System.arraycopy(this.value, 0, iArr, i, this.blocks - i);
        this.value = null;
        this.value = iArr;
    }

    private GF2Polynomial karaMult(GF2Polynomial gF2Polynomial) {
        GF2Polynomial gF2Polynomial2 = new GF2Polynomial(this.len << 1);
        if (this.len <= 32) {
            gF2Polynomial2.value = mult32(this.value[0], gF2Polynomial.value[0]);
            return gF2Polynomial2;
        } else if (this.len <= 64) {
            gF2Polynomial2.value = mult64(this.value, gF2Polynomial.value);
            return gF2Polynomial2;
        } else if (this.len <= 128) {
            gF2Polynomial2.value = mult128(this.value, gF2Polynomial.value);
            return gF2Polynomial2;
        } else if (this.len <= 256) {
            gF2Polynomial2.value = mult256(this.value, gF2Polynomial.value);
            return gF2Polynomial2;
        } else if (this.len <= 512) {
            gF2Polynomial2.value = mult512(this.value, gF2Polynomial.value);
            return gF2Polynomial2;
        } else {
            int i = bitMask[IntegerFunctions.floorLog(this.len - 1)];
            int i2 = ((i - 1) >> 5) + 1;
            GF2Polynomial lower = lower(i2);
            GF2Polynomial upper = upper(i2);
            GF2Polynomial lower2 = gF2Polynomial.lower(i2);
            gF2Polynomial = gF2Polynomial.upper(i2);
            GF2Polynomial karaMult = upper.karaMult(gF2Polynomial);
            GF2Polynomial karaMult2 = lower.karaMult(lower2);
            lower.addToThis(upper);
            lower2.addToThis(gF2Polynomial);
            gF2Polynomial = lower.karaMult(lower2);
            gF2Polynomial2.shiftLeftAddThis(karaMult, i << 1);
            gF2Polynomial2.shiftLeftAddThis(karaMult, i);
            gF2Polynomial2.shiftLeftAddThis(gF2Polynomial, i);
            gF2Polynomial2.shiftLeftAddThis(karaMult2, i);
            gF2Polynomial2.addToThis(karaMult2);
            return gF2Polynomial2;
        }
    }

    private GF2Polynomial lower(int i) {
        GF2Polynomial gF2Polynomial = new GF2Polynomial(i << 5);
        System.arraycopy(this.value, 0, gF2Polynomial.value, 0, Math.min(i, this.blocks));
        return gF2Polynomial;
    }

    private static int[] mult128(int[] iArr, int[] iArr2) {
        int[] iArr3 = new int[8];
        int[] iArr4 = new int[2];
        System.arraycopy(iArr, 0, iArr4, 0, Math.min(2, iArr.length));
        int[] iArr5 = new int[2];
        if (iArr.length > 2) {
            System.arraycopy(iArr, 2, iArr5, 0, Math.min(2, iArr.length - 2));
        }
        iArr = new int[2];
        System.arraycopy(iArr2, 0, iArr, 0, Math.min(2, iArr2.length));
        int[] iArr6 = new int[2];
        if (iArr2.length > 2) {
            System.arraycopy(iArr2, 2, iArr6, 0, Math.min(2, iArr2.length - 2));
        }
        int[] mult64;
        if (iArr5[1] != 0 || iArr6[1] != 0) {
            mult64 = mult64(iArr5, iArr6);
            iArr3[7] = iArr3[7] ^ mult64[3];
            iArr3[6] = iArr3[6] ^ mult64[2];
            iArr3[5] = iArr3[5] ^ (mult64[1] ^ mult64[3]);
            iArr3[4] = iArr3[4] ^ (mult64[0] ^ mult64[2]);
            iArr3[3] = iArr3[3] ^ mult64[1];
            iArr3[2] = mult64[0] ^ iArr3[2];
        } else if (!(iArr5[0] == 0 && iArr6[0] == 0)) {
            mult64 = mult32(iArr5[0], iArr6[0]);
            iArr3[5] = iArr3[5] ^ mult64[1];
            iArr3[4] = iArr3[4] ^ mult64[0];
            iArr3[3] = iArr3[3] ^ mult64[1];
            iArr3[2] = mult64[0] ^ iArr3[2];
        }
        iArr5[0] = iArr5[0] ^ iArr4[0];
        iArr5[1] = iArr5[1] ^ iArr4[1];
        iArr6[0] = iArr6[0] ^ iArr[0];
        iArr6[1] = iArr6[1] ^ iArr[1];
        if (iArr5[1] == 0 && iArr6[1] == 0) {
            iArr5 = mult32(iArr5[0], iArr6[0]);
            iArr3[3] = iArr3[3] ^ iArr5[1];
            iArr3[2] = iArr5[0] ^ iArr3[2];
        } else {
            iArr5 = mult64(iArr5, iArr6);
            iArr3[5] = iArr3[5] ^ iArr5[3];
            iArr3[4] = iArr3[4] ^ iArr5[2];
            iArr3[3] = iArr3[3] ^ iArr5[1];
            iArr3[2] = iArr5[0] ^ iArr3[2];
        }
        if (iArr4[1] == 0 && iArr[1] == 0) {
            iArr = mult32(iArr4[0], iArr[0]);
            iArr3[3] = iArr3[3] ^ iArr[1];
            iArr3[2] = iArr3[2] ^ iArr[0];
            iArr3[1] = iArr3[1] ^ iArr[1];
            iArr3[0] = iArr[0] ^ iArr3[0];
            return iArr3;
        }
        iArr = mult64(iArr4, iArr);
        iArr3[5] = iArr3[5] ^ iArr[3];
        iArr3[4] = iArr3[4] ^ iArr[2];
        iArr3[3] = iArr3[3] ^ (iArr[1] ^ iArr[3]);
        iArr3[2] = iArr3[2] ^ (iArr[0] ^ iArr[2]);
        iArr3[1] = iArr3[1] ^ iArr[1];
        iArr3[0] = iArr[0] ^ iArr3[0];
        return iArr3;
    }

    private static int[] mult256(int[] iArr, int[] iArr2) {
        Object obj = iArr;
        Object obj2 = iArr2;
        int[] iArr3 = new int[16];
        int[] iArr4 = new int[4];
        System.arraycopy(obj, 0, iArr4, 0, Math.min(4, obj.length));
        int[] iArr5 = new int[4];
        if (obj.length > 4) {
            System.arraycopy(obj, 4, iArr5, 0, Math.min(4, obj.length - 4));
        }
        int[] iArr6 = new int[4];
        System.arraycopy(obj2, 0, iArr6, 0, Math.min(4, obj2.length));
        int[] iArr7 = new int[4];
        if (obj2.length > 4) {
            System.arraycopy(obj2, 4, iArr7, 0, Math.min(4, obj2.length - 4));
        }
        int[] mult128;
        if (iArr5[3] != 0 || iArr5[2] != 0 || iArr7[3] != 0 || iArr7[2] != 0) {
            mult128 = mult128(iArr5, iArr7);
            iArr3[15] = iArr3[15] ^ mult128[7];
            iArr3[14] = iArr3[14] ^ mult128[6];
            iArr3[13] = iArr3[13] ^ mult128[5];
            iArr3[12] = iArr3[12] ^ mult128[4];
            iArr3[11] = iArr3[11] ^ (mult128[3] ^ mult128[7]);
            iArr3[10] = iArr3[10] ^ (mult128[2] ^ mult128[6]);
            iArr3[9] = iArr3[9] ^ (mult128[1] ^ mult128[5]);
            iArr3[8] = iArr3[8] ^ (mult128[0] ^ mult128[4]);
            iArr3[7] = iArr3[7] ^ mult128[3];
            iArr3[6] = iArr3[6] ^ mult128[2];
            iArr3[5] = iArr3[5] ^ mult128[1];
            iArr3[4] = mult128[0] ^ iArr3[4];
        } else if (iArr5[1] != 0 || iArr7[1] != 0) {
            mult128 = mult64(iArr5, iArr7);
            iArr3[11] = iArr3[11] ^ mult128[3];
            iArr3[10] = iArr3[10] ^ mult128[2];
            iArr3[9] = iArr3[9] ^ mult128[1];
            iArr3[8] = iArr3[8] ^ mult128[0];
            iArr3[7] = iArr3[7] ^ mult128[3];
            iArr3[6] = iArr3[6] ^ mult128[2];
            iArr3[5] = iArr3[5] ^ mult128[1];
            iArr3[4] = mult128[0] ^ iArr3[4];
        } else if (!(iArr5[0] == 0 && iArr7[0] == 0)) {
            mult128 = mult32(iArr5[0], iArr7[0]);
            iArr3[9] = iArr3[9] ^ mult128[1];
            iArr3[8] = iArr3[8] ^ mult128[0];
            iArr3[5] = iArr3[5] ^ mult128[1];
            iArr3[4] = mult128[0] ^ iArr3[4];
        }
        iArr5[0] = iArr5[0] ^ iArr4[0];
        iArr5[1] = iArr5[1] ^ iArr4[1];
        iArr5[2] = iArr5[2] ^ iArr4[2];
        iArr5[3] = iArr5[3] ^ iArr4[3];
        iArr7[0] = iArr7[0] ^ iArr6[0];
        iArr7[1] = iArr7[1] ^ iArr6[1];
        iArr7[2] = iArr7[2] ^ iArr6[2];
        iArr7[3] = iArr7[3] ^ iArr6[3];
        iArr5 = mult128(iArr5, iArr7);
        iArr3[11] = iArr3[11] ^ iArr5[7];
        iArr3[10] = iArr3[10] ^ iArr5[6];
        iArr3[9] = iArr3[9] ^ iArr5[5];
        iArr3[8] = iArr3[8] ^ iArr5[4];
        iArr3[7] = iArr3[7] ^ iArr5[3];
        iArr3[6] = iArr3[6] ^ iArr5[2];
        iArr3[5] = iArr3[5] ^ iArr5[1];
        iArr3[4] = iArr5[0] ^ iArr3[4];
        iArr6 = mult128(iArr4, iArr6);
        iArr3[11] = iArr3[11] ^ iArr6[7];
        iArr3[10] = iArr3[10] ^ iArr6[6];
        iArr3[9] = iArr3[9] ^ iArr6[5];
        iArr3[8] = iArr3[8] ^ iArr6[4];
        iArr3[7] = iArr3[7] ^ (iArr6[3] ^ iArr6[7]);
        iArr3[6] = iArr3[6] ^ (iArr6[2] ^ iArr6[6]);
        iArr3[5] = iArr3[5] ^ (iArr6[1] ^ iArr6[5]);
        iArr3[4] = iArr3[4] ^ (iArr6[0] ^ iArr6[4]);
        iArr3[3] = iArr3[3] ^ iArr6[3];
        iArr3[2] = iArr3[2] ^ iArr6[2];
        iArr3[1] = iArr3[1] ^ iArr6[1];
        iArr3[0] = iArr6[0] ^ iArr3[0];
        return iArr3;
    }

    private static int[] mult32(int i, int i2) {
        int[] iArr = new int[2];
        if (i == 0 || i2 == 0) {
            return iArr;
        }
        long j = 0;
        long j2 = ((long) i2) & BodyPartID.bodyIdMax;
        for (int i3 = 1; i3 <= 32; i3++) {
            if ((bitMask[i3 - 1] & i) != 0) {
                j ^= j2;
            }
            j2 <<= 1;
        }
        iArr[1] = (int) (j >>> 32);
        iArr[0] = (int) (j & BodyPartID.bodyIdMax);
        return iArr;
    }

    private static int[] mult512(int[] iArr, int[] iArr2) {
        Object obj = iArr;
        Object obj2 = iArr2;
        int[] iArr3 = new int[32];
        int[] iArr4 = new int[8];
        System.arraycopy(obj, 0, iArr4, 0, Math.min(8, obj.length));
        int[] iArr5 = new int[8];
        if (obj.length > 8) {
            System.arraycopy(obj, 8, iArr5, 0, Math.min(8, obj.length - 8));
        }
        int[] iArr6 = new int[8];
        System.arraycopy(obj2, 0, iArr6, 0, Math.min(8, obj2.length));
        int[] iArr7 = new int[8];
        if (obj2.length > 8) {
            System.arraycopy(obj2, 8, iArr7, 0, Math.min(8, obj2.length - 8));
        }
        int[] mult256 = mult256(iArr5, iArr7);
        iArr3[31] = iArr3[31] ^ mult256[15];
        iArr3[30] = iArr3[30] ^ mult256[14];
        iArr3[29] = iArr3[29] ^ mult256[13];
        iArr3[28] = iArr3[28] ^ mult256[12];
        iArr3[27] = iArr3[27] ^ mult256[11];
        iArr3[26] = iArr3[26] ^ mult256[10];
        iArr3[25] = iArr3[25] ^ mult256[9];
        iArr3[24] = iArr3[24] ^ mult256[8];
        iArr3[23] = iArr3[23] ^ (mult256[7] ^ mult256[15]);
        iArr3[22] = iArr3[22] ^ (mult256[6] ^ mult256[14]);
        iArr3[21] = iArr3[21] ^ (mult256[5] ^ mult256[13]);
        iArr3[20] = iArr3[20] ^ (mult256[4] ^ mult256[12]);
        iArr3[19] = iArr3[19] ^ (mult256[3] ^ mult256[11]);
        iArr3[18] = iArr3[18] ^ (mult256[2] ^ mult256[10]);
        iArr3[17] = iArr3[17] ^ (mult256[1] ^ mult256[9]);
        iArr3[16] = iArr3[16] ^ (mult256[0] ^ mult256[8]);
        iArr3[15] = iArr3[15] ^ mult256[7];
        iArr3[14] = iArr3[14] ^ mult256[6];
        iArr3[13] = iArr3[13] ^ mult256[5];
        iArr3[12] = iArr3[12] ^ mult256[4];
        iArr3[11] = iArr3[11] ^ mult256[3];
        iArr3[10] = iArr3[10] ^ mult256[2];
        iArr3[9] = iArr3[9] ^ mult256[1];
        iArr3[8] = mult256[0] ^ iArr3[8];
        iArr5[0] = iArr5[0] ^ iArr4[0];
        iArr5[1] = iArr5[1] ^ iArr4[1];
        iArr5[2] = iArr5[2] ^ iArr4[2];
        iArr5[3] = iArr5[3] ^ iArr4[3];
        iArr5[4] = iArr5[4] ^ iArr4[4];
        iArr5[5] = iArr5[5] ^ iArr4[5];
        iArr5[6] = iArr5[6] ^ iArr4[6];
        iArr5[7] = iArr5[7] ^ iArr4[7];
        iArr7[0] = iArr7[0] ^ iArr6[0];
        iArr7[1] = iArr7[1] ^ iArr6[1];
        iArr7[2] = iArr7[2] ^ iArr6[2];
        iArr7[3] = iArr7[3] ^ iArr6[3];
        iArr7[4] = iArr7[4] ^ iArr6[4];
        iArr7[5] = iArr7[5] ^ iArr6[5];
        iArr7[6] = iArr7[6] ^ iArr6[6];
        iArr7[7] = iArr7[7] ^ iArr6[7];
        mult256 = mult256(iArr5, iArr7);
        iArr3[23] = iArr3[23] ^ mult256[15];
        iArr3[22] = iArr3[22] ^ mult256[14];
        iArr3[21] = iArr3[21] ^ mult256[13];
        iArr3[20] = iArr3[20] ^ mult256[12];
        iArr3[19] = iArr3[19] ^ mult256[11];
        iArr3[18] = iArr3[18] ^ mult256[10];
        iArr3[17] = iArr3[17] ^ mult256[9];
        iArr3[16] = iArr3[16] ^ mult256[8];
        iArr3[15] = iArr3[15] ^ mult256[7];
        iArr3[14] = iArr3[14] ^ mult256[6];
        iArr3[13] = iArr3[13] ^ mult256[5];
        iArr3[12] = iArr3[12] ^ mult256[4];
        iArr3[11] = iArr3[11] ^ mult256[3];
        iArr3[10] = iArr3[10] ^ mult256[2];
        iArr3[9] = iArr3[9] ^ mult256[1];
        iArr3[8] = mult256[0] ^ iArr3[8];
        iArr6 = mult256(iArr4, iArr6);
        iArr3[23] = iArr3[23] ^ iArr6[15];
        iArr3[22] = iArr3[22] ^ iArr6[14];
        iArr3[21] = iArr3[21] ^ iArr6[13];
        iArr3[20] = iArr3[20] ^ iArr6[12];
        iArr3[19] = iArr3[19] ^ iArr6[11];
        iArr3[18] = iArr3[18] ^ iArr6[10];
        iArr3[17] = iArr3[17] ^ iArr6[9];
        iArr3[16] = iArr3[16] ^ iArr6[8];
        iArr3[15] = iArr3[15] ^ (iArr6[7] ^ iArr6[15]);
        iArr3[14] = iArr3[14] ^ (iArr6[6] ^ iArr6[14]);
        iArr3[13] = iArr3[13] ^ (iArr6[5] ^ iArr6[13]);
        iArr3[12] = iArr3[12] ^ (iArr6[4] ^ iArr6[12]);
        iArr3[11] = iArr3[11] ^ (iArr6[3] ^ iArr6[11]);
        iArr3[10] = iArr3[10] ^ (iArr6[2] ^ iArr6[10]);
        iArr3[9] = iArr3[9] ^ (iArr6[1] ^ iArr6[9]);
        iArr3[8] = iArr3[8] ^ (iArr6[0] ^ iArr6[8]);
        iArr3[7] = iArr3[7] ^ iArr6[7];
        iArr3[6] = iArr3[6] ^ iArr6[6];
        iArr3[5] = iArr3[5] ^ iArr6[5];
        iArr3[4] = iArr3[4] ^ iArr6[4];
        iArr3[3] = iArr3[3] ^ iArr6[3];
        iArr3[2] = iArr3[2] ^ iArr6[2];
        iArr3[1] = iArr3[1] ^ iArr6[1];
        iArr3[0] = iArr6[0] ^ iArr3[0];
        return iArr3;
    }

    private static int[] mult64(int[] iArr, int[] iArr2) {
        int[] iArr3 = new int[4];
        int i = iArr[0];
        int i2 = iArr.length > 1 ? iArr[1] : 0;
        int i3 = iArr2[0];
        int i4 = iArr2.length > 1 ? iArr2[1] : 0;
        if (!(i2 == 0 && i4 == 0)) {
            int[] mult32 = mult32(i2, i4);
            iArr3[3] = iArr3[3] ^ mult32[1];
            iArr3[2] = iArr3[2] ^ (mult32[0] ^ mult32[1]);
            iArr3[1] = mult32[0] ^ iArr3[1];
        }
        iArr = mult32(i2 ^ i, i4 ^ i3);
        iArr3[2] = iArr3[2] ^ iArr[1];
        iArr3[1] = iArr[0] ^ iArr3[1];
        iArr = mult32(i, i3);
        iArr3[2] = iArr3[2] ^ iArr[1];
        iArr3[1] = iArr3[1] ^ (iArr[0] ^ iArr[1]);
        iArr3[0] = iArr[0] ^ iArr3[0];
        return iArr3;
    }

    private GF2Polynomial upper(int i) {
        int min = Math.min(i, this.blocks - i);
        GF2Polynomial gF2Polynomial = new GF2Polynomial(min << 5);
        if (this.blocks >= i) {
            System.arraycopy(this.value, i, gF2Polynomial.value, 0, min);
        }
        return gF2Polynomial;
    }

    private void zeroUnusedBits() {
        if ((this.len & 31) != 0) {
            int[] iArr = this.value;
            int i = this.blocks - 1;
            iArr[i] = iArr[i] & reverseRightMask[this.len & 31];
        }
    }

    public GF2Polynomial add(GF2Polynomial gF2Polynomial) {
        return xor(gF2Polynomial);
    }

    public void addToThis(GF2Polynomial gF2Polynomial) {
        expandN(gF2Polynomial.len);
        xorThisBy(gF2Polynomial);
    }

    public void assignAll() {
        for (int i = 0; i < this.blocks; i++) {
            this.value[i] = -1;
        }
        zeroUnusedBits();
    }

    public void assignOne() {
        for (int i = 1; i < this.blocks; i++) {
            this.value[i] = 0;
        }
        this.value[0] = 1;
    }

    public void assignX() {
        for (int i = 1; i < this.blocks; i++) {
            this.value[i] = 0;
        }
        this.value[0] = 2;
    }

    public void assignZero() {
        for (int i = 0; i < this.blocks; i++) {
            this.value[i] = 0;
        }
    }

    public Object clone() {
        return new GF2Polynomial(this);
    }

    public GF2Polynomial[] divide(GF2Polynomial gF2Polynomial) throws RuntimeException {
        GF2Polynomial[] gF2PolynomialArr = new GF2Polynomial[2];
        GF2Polynomial gF2Polynomial2 = new GF2Polynomial(this.len);
        GF2Polynomial gF2Polynomial3 = new GF2Polynomial(this);
        GF2Polynomial gF2Polynomial4 = new GF2Polynomial(gF2Polynomial);
        if (gF2Polynomial4.isZero()) {
            throw new RuntimeException();
        }
        gF2Polynomial3.reduceN();
        gF2Polynomial4.reduceN();
        if (gF2Polynomial3.len < gF2Polynomial4.len) {
            gF2PolynomialArr[0] = new GF2Polynomial(0);
            gF2PolynomialArr[1] = gF2Polynomial3;
            return gF2PolynomialArr;
        }
        int i = gF2Polynomial3.len - gF2Polynomial4.len;
        gF2Polynomial2.expandN(i + 1);
        while (i >= 0) {
            gF2Polynomial3.subtractFromThis(gF2Polynomial4.shiftLeft(i));
            gF2Polynomial3.reduceN();
            gF2Polynomial2.xorBit(i);
            i = gF2Polynomial3.len - gF2Polynomial4.len;
        }
        gF2PolynomialArr[0] = gF2Polynomial2;
        gF2PolynomialArr[1] = gF2Polynomial3;
        return gF2PolynomialArr;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GF2Polynomial)) {
            return false;
        }
        GF2Polynomial gF2Polynomial = (GF2Polynomial) obj;
        if (this.len != gF2Polynomial.len) {
            return false;
        }
        for (int i = 0; i < this.blocks; i++) {
            if (this.value[i] != gF2Polynomial.value[i]) {
                return false;
            }
        }
        return true;
    }

    public void expandN(int i) {
        if (this.len < i) {
            this.len = i;
            i = ((i - 1) >>> 5) + 1;
            if (this.blocks < i) {
                if (this.value.length >= i) {
                    for (int i2 = this.blocks; i2 < i; i2++) {
                        this.value[i2] = 0;
                    }
                    this.blocks = i;
                    return;
                }
                int[] iArr = new int[i];
                System.arraycopy(this.value, 0, iArr, 0, this.blocks);
                this.blocks = i;
                this.value = null;
                this.value = iArr;
            }
        }
    }

    public GF2Polynomial gcd(GF2Polynomial gF2Polynomial) throws RuntimeException {
        if (isZero() && gF2Polynomial.isZero()) {
            throw new ArithmeticException("Both operands of gcd equal zero.");
        } else if (isZero()) {
            return new GF2Polynomial(gF2Polynomial);
        } else {
            if (gF2Polynomial.isZero()) {
                return new GF2Polynomial(this);
            }
            GF2Polynomial gF2Polynomial2 = new GF2Polynomial(this);
            GF2Polynomial gF2Polynomial3 = new GF2Polynomial(gF2Polynomial);
            gF2Polynomial = gF2Polynomial2;
            gF2Polynomial2 = gF2Polynomial3;
            while (!gF2Polynomial2.isZero()) {
                GF2Polynomial gF2Polynomial4 = gF2Polynomial2;
                gF2Polynomial2 = gF2Polynomial.remainder(gF2Polynomial2);
                gF2Polynomial = gF2Polynomial4;
            }
            return gF2Polynomial;
        }
    }

    public int getBit(int i) {
        if (i < 0) {
            throw new RuntimeException();
        } else if (i > this.len - 1) {
            return 0;
        } else {
            return (bitMask[i & 31] & this.value[i >>> 5]) != 0 ? 1 : 0;
        }
    }

    public int getLength() {
        return this.len;
    }

    public int hashCode() {
        return this.len + this.value.hashCode();
    }

    public GF2Polynomial increase() {
        GF2Polynomial gF2Polynomial = new GF2Polynomial(this);
        gF2Polynomial.increaseThis();
        return gF2Polynomial;
    }

    public void increaseThis() {
        xorBit(0);
    }

    public boolean isIrreducible() {
        if (isZero()) {
            return false;
        }
        GF2Polynomial gF2Polynomial = new GF2Polynomial(this);
        gF2Polynomial.reduceN();
        int i = gF2Polynomial.len - 1;
        GF2Polynomial gF2Polynomial2 = new GF2Polynomial(gF2Polynomial.len, "X");
        for (int i2 = 1; i2 <= (i >> 1); i2++) {
            gF2Polynomial2.squareThisPreCalc();
            gF2Polynomial2 = gF2Polynomial2.remainder(gF2Polynomial);
            GF2Polynomial add = gF2Polynomial2.add(new GF2Polynomial(32, "X"));
            if (add.isZero() || !gF2Polynomial.gcd(add).isOne()) {
                return false;
            }
        }
        return true;
    }

    public boolean isOne() {
        for (int i = 1; i < this.blocks; i++) {
            if (this.value[i] != 0) {
                return false;
            }
        }
        return this.value[0] == 1;
    }

    public boolean isZero() {
        if (this.len == 0) {
            return true;
        }
        for (int i = 0; i < this.blocks; i++) {
            if (this.value[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public GF2Polynomial multiply(GF2Polynomial gF2Polynomial) {
        int max = Math.max(this.len, gF2Polynomial.len);
        expandN(max);
        gF2Polynomial.expandN(max);
        return karaMult(gF2Polynomial);
    }

    public GF2Polynomial multiplyClassic(GF2Polynomial gF2Polynomial) {
        int i = 1;
        GF2Polynomial gF2Polynomial2 = new GF2Polynomial(Math.max(this.len, gF2Polynomial.len) << 1);
        GF2Polynomial[] gF2PolynomialArr = new GF2Polynomial[32];
        gF2PolynomialArr[0] = new GF2Polynomial(this);
        while (i <= 31) {
            gF2PolynomialArr[i] = gF2PolynomialArr[i - 1].shiftLeft();
            i++;
        }
        for (i = 0; i < gF2Polynomial.blocks; i++) {
            int i2;
            for (i2 = 0; i2 <= 31; i2++) {
                if ((gF2Polynomial.value[i] & bitMask[i2]) != 0) {
                    gF2Polynomial2.xorThisBy(gF2PolynomialArr[i2]);
                }
            }
            for (i2 = 0; i2 <= 31; i2++) {
                gF2PolynomialArr[i2].shiftBlocksLeft();
            }
        }
        return gF2Polynomial2;
    }

    public GF2Polynomial quotient(GF2Polynomial gF2Polynomial) throws RuntimeException {
        GF2Polynomial gF2Polynomial2 = new GF2Polynomial(this.len);
        GF2Polynomial gF2Polynomial3 = new GF2Polynomial(this);
        GF2Polynomial gF2Polynomial4 = new GF2Polynomial(gF2Polynomial);
        if (gF2Polynomial4.isZero()) {
            throw new RuntimeException();
        }
        gF2Polynomial3.reduceN();
        gF2Polynomial4.reduceN();
        if (gF2Polynomial3.len < gF2Polynomial4.len) {
            return new GF2Polynomial(0);
        }
        int i = gF2Polynomial3.len - gF2Polynomial4.len;
        gF2Polynomial2.expandN(i + 1);
        while (i >= 0) {
            gF2Polynomial3.subtractFromThis(gF2Polynomial4.shiftLeft(i));
            gF2Polynomial3.reduceN();
            gF2Polynomial2.xorBit(i);
            i = gF2Polynomial3.len - gF2Polynomial4.len;
        }
        return gF2Polynomial2;
    }

    public void randomize() {
        for (int i = 0; i < this.blocks; i++) {
            this.value[i] = rand.nextInt();
        }
        zeroUnusedBits();
    }

    public void randomize(Random random) {
        for (int i = 0; i < this.blocks; i++) {
            this.value[i] = random.nextInt();
        }
        zeroUnusedBits();
    }

    public void reduceN() {
        int i;
        int i2;
        int i3 = this.blocks;
        while (true) {
            i3--;
            if (this.value[i3] != 0 || i3 <= 0) {
                i = this.value[i3];
                i2 = 0;
            }
        }
        i = this.value[i3];
        i2 = 0;
        while (i != 0) {
            i >>>= 1;
            i2++;
        }
        this.len = (i3 << 5) + i2;
        this.blocks = i3 + 1;
    }

    void reducePentanomial(int i, int[] iArr) {
        int i2;
        int i3;
        int i4;
        int i5;
        int[] iArr2;
        int[] iArr3;
        int i6 = i;
        int i7 = i6 >>> 5;
        int i8 = i6 & 31;
        int i9 = 32 - i8;
        int i10 = 0;
        int i11 = (i6 - iArr[0]) >>> 5;
        int i12 = 32 - ((i6 - iArr[0]) & 31);
        int i13 = (i6 - iArr[1]) >>> 5;
        int i14 = 32 - ((i6 - iArr[1]) & 31);
        int i15 = (i6 - iArr[2]) >>> 5;
        int i16 = 32 - ((i6 - iArr[2]) & 31);
        int i17 = ((i6 << 1) - 2) >>> 5;
        while (i17 > i7) {
            i2 = i11;
            long j = ((long) this.value[i17]) & BodyPartID.bodyIdMax;
            int[] iArr4 = this.value;
            int i18 = i17 - i7;
            int i19 = i18 - 1;
            i3 = i7;
            i4 = i8;
            iArr4[i19] = iArr4[i19] ^ ((int) (j << i9));
            int[] iArr5 = this.value;
            i5 = i16;
            iArr5[i18] = (int) (((long) iArr5[i18]) ^ (j >>> (32 - i9)));
            iArr2 = this.value;
            i16 = i17 - i2;
            i7 = i16 - 1;
            iArr2[i7] = iArr2[i7] ^ ((int) (j << i12));
            iArr2 = this.value;
            iArr2[i16] = (int) (((long) iArr2[i16]) ^ (j >>> (32 - i12)));
            iArr2 = this.value;
            i16 = i17 - i13;
            i7 = i16 - 1;
            iArr2[i7] = iArr2[i7] ^ ((int) (j << i14));
            iArr2 = this.value;
            iArr2[i16] = (int) (((long) iArr2[i16]) ^ (j >>> (32 - i14)));
            iArr2 = this.value;
            i16 = i17 - i15;
            i7 = i16 - 1;
            iArr2[i7] = iArr2[i7] ^ ((int) (j << i5));
            iArr2 = this.value;
            iArr2[i16] = (int) (((long) iArr2[i16]) ^ (j >>> (32 - i5)));
            this.value[i17] = 0;
            i17--;
            i10 = 0;
            i11 = i2;
            i7 = i3;
            i8 = i4;
            i16 = i5;
            i6 = i;
        }
        i5 = i16;
        i3 = i7;
        i4 = i8;
        i16 = i10;
        i2 = i11;
        long j2 = (((long) this.value[i3]) & BodyPartID.bodyIdMax) & (BodyPartID.bodyIdMax << i4);
        iArr2 = this.value;
        iArr2[i16] = (int) (((long) iArr2[i16]) ^ (j2 >>> (32 - i9)));
        i6 = i3 - i2;
        i16 = i6 - 1;
        if (i16 >= 0) {
            iArr3 = this.value;
            iArr3[i16] = iArr3[i16] ^ ((int) (j2 << i12));
        }
        int[] iArr6 = this.value;
        iArr6[i6] = (int) (((long) iArr6[i6]) ^ (j2 >>> (32 - i12)));
        i6 = i3 - i13;
        i16 = i6 - 1;
        if (i16 >= 0) {
            iArr3 = this.value;
            iArr3[i16] = iArr3[i16] ^ ((int) (j2 << i14));
        }
        iArr6 = this.value;
        iArr6[i6] = (int) (((long) iArr6[i6]) ^ (j2 >>> (32 - i14)));
        i6 = i3 - i15;
        i16 = i6 - 1;
        if (i16 >= 0) {
            iArr3 = this.value;
            iArr3[i16] = iArr3[i16] ^ ((int) (j2 << i5));
        }
        iArr6 = this.value;
        iArr6[i6] = (int) ((j2 >>> (32 - i5)) ^ ((long) iArr6[i6]));
        iArr2 = this.value;
        iArr2[i3] = iArr2[i3] & reverseRightMask[i4];
        i6 = i;
        this.blocks = ((i6 - 1) >>> 5) + 1;
        this.len = i6;
    }

    void reduceTrinomial(int i, int i2) {
        int i3;
        int[] iArr;
        int i4 = i;
        int i5 = i4 >>> 5;
        int i6 = i4 & 31;
        int i7 = 32 - i6;
        int i8 = i4 - i2;
        int i9 = i8 >>> 5;
        i8 = 32 - (i8 & 31);
        int i10 = ((i4 << 1) - 2) >>> 5;
        while (i10 > i5) {
            long j = BodyPartID.bodyIdMax & ((long) this.value[i10]);
            int[] iArr2 = this.value;
            int i11 = i10 - i5;
            int i12 = i11 - 1;
            i3 = i5;
            iArr2[i12] = ((int) (j << i7)) ^ iArr2[i12];
            iArr = this.value;
            iArr[i11] = (int) (((long) iArr[i11]) ^ (j >>> (32 - i7)));
            iArr = this.value;
            i5 = i10 - i9;
            int i13 = i5 - 1;
            iArr[i13] = iArr[i13] ^ ((int) (j << i8));
            iArr = this.value;
            iArr[i5] = (int) ((j >>> (32 - i8)) ^ ((long) iArr[i5]));
            this.value[i10] = 0;
            i10--;
            i5 = i3;
            i4 = i;
        }
        i3 = i5;
        long j2 = (((long) this.value[i3]) & BodyPartID.bodyIdMax) & (BodyPartID.bodyIdMax << i6);
        int[] iArr3 = this.value;
        iArr3[0] = (int) (((long) iArr3[0]) ^ (j2 >>> (32 - i7)));
        i7 = i3 - i9;
        i9 = i7 - 1;
        if (i9 >= 0) {
            iArr3 = this.value;
            iArr3[i9] = iArr3[i9] ^ ((int) (j2 << i8));
        }
        int[] iArr4 = this.value;
        iArr4[i7] = (int) ((j2 >>> (32 - i8)) ^ ((long) iArr4[i7]));
        iArr = this.value;
        iArr[i3] = iArr[i3] & reverseRightMask[i6];
        i4 = i;
        this.blocks = ((i4 - 1) >>> 5) + 1;
        this.len = i4;
    }

    public GF2Polynomial remainder(GF2Polynomial gF2Polynomial) throws RuntimeException {
        GF2Polynomial gF2Polynomial2 = new GF2Polynomial(this);
        GF2Polynomial gF2Polynomial3 = new GF2Polynomial(gF2Polynomial);
        if (gF2Polynomial3.isZero()) {
            throw new RuntimeException();
        }
        gF2Polynomial2.reduceN();
        gF2Polynomial3.reduceN();
        if (gF2Polynomial2.len < gF2Polynomial3.len) {
            return gF2Polynomial2;
        }
        while (true) {
            int i = gF2Polynomial2.len - gF2Polynomial3.len;
            if (i < 0) {
                return gF2Polynomial2;
            }
            gF2Polynomial2.subtractFromThis(gF2Polynomial3.shiftLeft(i));
            gF2Polynomial2.reduceN();
        }
    }

    public void resetBit(int i) throws RuntimeException {
        if (i < 0) {
            throw new RuntimeException();
        } else if (i <= this.len - 1) {
            int[] iArr = this.value;
            int i2 = i >>> 5;
            iArr[i2] = (~bitMask[i & 31]) & iArr[i2];
        }
    }

    public void setBit(int i) throws RuntimeException {
        if (i < 0 || i > this.len - 1) {
            throw new RuntimeException();
        }
        int[] iArr = this.value;
        int i2 = i >>> 5;
        iArr[i2] = bitMask[i & 31] | iArr[i2];
    }

    void shiftBlocksLeft() {
        this.blocks++;
        this.len += 32;
        if (this.blocks <= this.value.length) {
            for (int i = this.blocks - 1; i >= 1; i--) {
                this.value[i] = this.value[i - 1];
            }
            this.value[0] = 0;
            return;
        }
        int[] iArr = new int[this.blocks];
        System.arraycopy(this.value, 0, iArr, 1, this.blocks - 1);
        this.value = null;
        this.value = iArr;
    }

    public GF2Polynomial shiftLeft() {
        GF2Polynomial gF2Polynomial = new GF2Polynomial(this.len + 1, this.value);
        for (int i = gF2Polynomial.blocks - 1; i >= 1; i--) {
            int[] iArr = gF2Polynomial.value;
            iArr[i] = iArr[i] << 1;
            iArr = gF2Polynomial.value;
            iArr[i] = iArr[i] | (gF2Polynomial.value[i - 1] >>> 31);
        }
        int[] iArr2 = gF2Polynomial.value;
        iArr2[0] = iArr2[0] << 1;
        return gF2Polynomial;
    }

    public GF2Polynomial shiftLeft(int i) {
        GF2Polynomial gF2Polynomial = new GF2Polynomial(this.len + i, this.value);
        if (i >= 32) {
            gF2Polynomial.doShiftBlocksLeft(i >>> 5);
        }
        i &= 31;
        if (i != 0) {
            for (int i2 = gF2Polynomial.blocks - 1; i2 >= 1; i2--) {
                int[] iArr = gF2Polynomial.value;
                iArr[i2] = iArr[i2] << i;
                iArr = gF2Polynomial.value;
                iArr[i2] = iArr[i2] | (gF2Polynomial.value[i2 - 1] >>> (32 - i));
            }
            int[] iArr2 = gF2Polynomial.value;
            iArr2[0] = iArr2[0] << i;
        }
        return gF2Polynomial;
    }

    public void shiftLeftAddThis(GF2Polynomial gF2Polynomial, int i) {
        if (i == 0) {
            addToThis(gF2Polynomial);
            return;
        }
        expandN(gF2Polynomial.len + i);
        int i2 = i >>> 5;
        for (int i3 = gF2Polynomial.blocks - 1; i3 >= 0; i3--) {
            int i4 = i3 + i2;
            int i5 = i4 + 1;
            if (i5 < this.blocks) {
                int i6 = i & 31;
                if (i6 != 0) {
                    int[] iArr = this.value;
                    iArr[i5] = (gF2Polynomial.value[i3] >>> (32 - i6)) ^ iArr[i5];
                }
            }
            int[] iArr2 = this.value;
            iArr2[i4] = iArr2[i4] ^ (gF2Polynomial.value[i3] << (i & 31));
        }
    }

    public void shiftLeftThis() {
        int[] iArr;
        int i;
        if ((this.len & 31) == 0) {
            this.len++;
            this.blocks++;
            if (this.blocks > this.value.length) {
                iArr = new int[this.blocks];
                System.arraycopy(this.value, 0, iArr, 0, this.value.length);
                this.value = null;
                this.value = iArr;
            }
            for (i = this.blocks - 1; i >= 1; i--) {
                int[] iArr2 = this.value;
                int i2 = i - 1;
                iArr2[i] = iArr2[i] | (this.value[i2] >>> 31);
                iArr2 = this.value;
                iArr2[i2] = iArr2[i2] << 1;
            }
            return;
        }
        this.len++;
        for (i = this.blocks - 1; i >= 1; i--) {
            int[] iArr3 = this.value;
            iArr3[i] = iArr3[i] << 1;
            iArr3 = this.value;
            iArr3[i] = iArr3[i] | (this.value[i - 1] >>> 31);
        }
        iArr = this.value;
        iArr[0] = iArr[0] << 1;
    }

    public GF2Polynomial shiftRight() {
        int[] iArr;
        GF2Polynomial gF2Polynomial = new GF2Polynomial(this.len - 1);
        int i = 0;
        System.arraycopy(this.value, 0, gF2Polynomial.value, 0, gF2Polynomial.blocks);
        while (i <= gF2Polynomial.blocks - 2) {
            iArr = gF2Polynomial.value;
            iArr[i] = iArr[i] >>> 1;
            iArr = gF2Polynomial.value;
            int i2 = i + 1;
            iArr[i] = iArr[i] | (gF2Polynomial.value[i2] << 31);
            i = i2;
        }
        iArr = gF2Polynomial.value;
        int i3 = gF2Polynomial.blocks - 1;
        iArr[i3] = iArr[i3] >>> 1;
        if (gF2Polynomial.blocks < this.blocks) {
            iArr = gF2Polynomial.value;
            i3 = gF2Polynomial.blocks - 1;
            iArr[i3] = iArr[i3] | (this.value[gF2Polynomial.blocks] << 31);
        }
        return gF2Polynomial;
    }

    public void shiftRightThis() {
        this.len--;
        this.blocks = ((this.len - 1) >>> 5) + 1;
        int i = 0;
        while (i <= this.blocks - 2) {
            int[] iArr = this.value;
            iArr[i] = iArr[i] >>> 1;
            iArr = this.value;
            int i2 = i + 1;
            iArr[i] = iArr[i] | (this.value[i2] << 31);
            i = i2;
        }
        int[] iArr2 = this.value;
        int i3 = this.blocks - 1;
        iArr2[i3] = iArr2[i3] >>> 1;
        if ((this.len & 31) == 0) {
            iArr2 = this.value;
            i3 = this.blocks - 1;
            iArr2[i3] = iArr2[i3] | (this.value[this.blocks] << 31);
        }
    }

    public void squareThisBitwise() {
        if (!isZero()) {
            int[] iArr = new int[(this.blocks << 1)];
            for (int i = this.blocks - 1; i >= 0; i--) {
                int i2 = this.value[i];
                int i3 = 1;
                for (int i4 = 0; i4 < 16; i4++) {
                    int i5;
                    if ((i2 & 1) != 0) {
                        i5 = i << 1;
                        iArr[i5] = iArr[i5] | i3;
                    }
                    if ((PKIFailureInfo.notAuthorized & i2) != 0) {
                        i5 = (i << 1) + 1;
                        iArr[i5] = iArr[i5] | i3;
                    }
                    i3 <<= 2;
                    i2 >>>= 1;
                }
            }
            this.value = null;
            this.value = iArr;
            this.blocks = iArr.length;
            this.len = (this.len << 1) - 1;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:15:0x00b9 in {2, 7, 9, 13, 14} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void squareThisPreCalc() {
        /*
        r10 = this;
        r0 = r10.isZero();
        if (r0 == 0) goto L_0x0007;
        return;
        r0 = r10.value;
        r0 = r0.length;
        r1 = r10.blocks;
        r1 = r1 << 1;
        r2 = 65280; // 0xff00 float:9.1477E-41 double:3.22526E-319;
        r3 = -16777216; // 0xffffffffff000000 float:-1.7014118E38 double:NaN;
        r4 = 16711680; // 0xff0000 float:2.3418052E-38 double:8.256667E-317;
        if (r0 < r1) goto L_0x006c;
        r0 = r10.blocks;
        r0 = r0 + -1;
        if (r0 < 0) goto L_0x005d;
        r1 = r10.value;
        r5 = r0 << 1;
        r6 = r5 + 1;
        r7 = squaringTable;
        r8 = r10.value;
        r8 = r8[r0];
        r8 = r8 & r4;
        r8 = r8 >>> 16;
        r7 = r7[r8];
        r8 = squaringTable;
        r9 = r10.value;
        r9 = r9[r0];
        r9 = r9 & r3;
        r9 = r9 >>> 24;
        r8 = r8[r9];
        r8 = r8 << 16;
        r7 = r7 | r8;
        r1[r6] = r7;
        r1 = r10.value;
        r6 = squaringTable;
        r7 = r10.value;
        r7 = r7[r0];
        r7 = r7 & 255;
        r6 = r6[r7];
        r7 = squaringTable;
        r8 = r10.value;
        r8 = r8[r0];
        r8 = r8 & r2;
        r8 = r8 >>> 8;
        r7 = r7[r8];
        r7 = r7 << 16;
        r6 = r6 | r7;
        r1[r5] = r6;
        r0 = r0 + -1;
        goto L_0x001b;
        r0 = r10.blocks;
        r0 = r0 << 1;
        r10.blocks = r0;
        r0 = r10.len;
        r0 = r0 << 1;
        r0 = r0 + -1;
        r10.len = r0;
        return;
        r0 = r10.blocks;
        r0 = r0 << 1;
        r0 = new int[r0];
        r1 = 0;
        r5 = r10.blocks;
        if (r1 >= r5) goto L_0x00b3;
        r5 = r1 << 1;
        r6 = squaringTable;
        r7 = r10.value;
        r7 = r7[r1];
        r7 = r7 & 255;
        r6 = r6[r7];
        r7 = squaringTable;
        r8 = r10.value;
        r8 = r8[r1];
        r8 = r8 & r2;
        r8 = r8 >>> 8;
        r7 = r7[r8];
        r7 = r7 << 16;
        r6 = r6 | r7;
        r0[r5] = r6;
        r5 = r5 + 1;
        r6 = squaringTable;
        r7 = r10.value;
        r7 = r7[r1];
        r7 = r7 & r4;
        r7 = r7 >>> 16;
        r6 = r6[r7];
        r7 = squaringTable;
        r8 = r10.value;
        r8 = r8[r1];
        r8 = r8 & r3;
        r8 = r8 >>> 24;
        r7 = r7[r8];
        r7 = r7 << 16;
        r6 = r6 | r7;
        r0[r5] = r6;
        r1 = r1 + 1;
        goto L_0x0073;
        r1 = 0;
        r10.value = r1;
        r10.value = r0;
        goto L_0x005d;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.pqc.math.linearalgebra.GF2Polynomial.squareThisPreCalc():void");
    }

    public GF2Polynomial subtract(GF2Polynomial gF2Polynomial) {
        return xor(gF2Polynomial);
    }

    public void subtractFromThis(GF2Polynomial gF2Polynomial) {
        expandN(gF2Polynomial.len);
        xorThisBy(gF2Polynomial);
    }

    public boolean testBit(int i) {
        if (i < 0) {
            throw new RuntimeException();
        } else if (i > this.len - 1) {
            return false;
        } else {
            return (bitMask[i & 31] & this.value[i >>> 5]) != 0;
        }
    }

    public byte[] toByteArray() {
        int i = ((this.len - 1) >> 3) + 1;
        int i2 = i & 3;
        byte[] bArr = new byte[i];
        int i3 = 0;
        for (int i4 = 0; i4 < (i >> 2); i4++) {
            int i5 = (i - (i4 << 2)) - 1;
            bArr[i5] = (byte) (255 & this.value[i4]);
            bArr[i5 - 1] = (byte) ((this.value[i4] & CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB) >>> 8);
            bArr[i5 - 2] = (byte) ((this.value[i4] & 16711680) >>> 16);
            bArr[i5 - 3] = (byte) ((this.value[i4] & -16777216) >>> 24);
        }
        while (i3 < i2) {
            i = ((i2 - i3) - 1) << 3;
            bArr[i3] = (byte) ((this.value[this.blocks - 1] & (255 << i)) >>> i);
            i3++;
        }
        return bArr;
    }

    public BigInteger toFlexiBigInt() {
        return (this.len == 0 || isZero()) ? new BigInteger(0, new byte[0]) : new BigInteger(1, toByteArray());
    }

    public int[] toIntegerArray() {
        int[] iArr = new int[this.blocks];
        System.arraycopy(this.value, 0, iArr, 0, this.blocks);
        return iArr;
    }

    public String toString(int i) {
        char[] cArr = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        String[] strArr = new String[]{"0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011", "1100", "1101", "1110", "1111"};
        String str = new String();
        StringBuilder stringBuilder;
        if (i == 16) {
            for (int i2 = this.blocks - 1; i2 >= 0; i2--) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(cArr[(this.value[i2] >>> 28) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(cArr[(this.value[i2] >>> 24) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(cArr[(this.value[i2] >>> 20) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(cArr[(this.value[i2] >>> 16) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(cArr[(this.value[i2] >>> 12) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(cArr[(this.value[i2] >>> 8) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(cArr[(this.value[i2] >>> 4) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(cArr[this.value[i2] & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(" ");
                str = stringBuilder.toString();
            }
        } else {
            for (int i3 = this.blocks - 1; i3 >= 0; i3--) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(strArr[(this.value[i3] >>> 28) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(strArr[(this.value[i3] >>> 24) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(strArr[(this.value[i3] >>> 20) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(strArr[(this.value[i3] >>> 16) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(strArr[(this.value[i3] >>> 12) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(strArr[(this.value[i3] >>> 8) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(strArr[(this.value[i3] >>> 4) & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(strArr[this.value[i3] & 15]);
                str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(" ");
                str = stringBuilder.toString();
            }
        }
        return str;
    }

    public boolean vectorMult(GF2Polynomial gF2Polynomial) throws RuntimeException {
        if (this.len == gF2Polynomial.len) {
            int i = 0;
            int i2 = 0;
            while (i < this.blocks) {
                int i3 = this.value[i] & gF2Polynomial.value[i];
                i2 = (((i2 ^ parity[i3 & 255]) ^ parity[(i3 >>> 8) & 255]) ^ parity[(i3 >>> 16) & 255]) ^ parity[(i3 >>> 24) & 255];
                i++;
            }
            return i2;
        }
        throw new RuntimeException();
    }

    public GF2Polynomial xor(GF2Polynomial gF2Polynomial) {
        GF2Polynomial gF2Polynomial2;
        int min = Math.min(this.blocks, gF2Polynomial.blocks);
        int i = 0;
        if (this.len >= gF2Polynomial.len) {
            gF2Polynomial2 = new GF2Polynomial(this);
            while (i < min) {
                int[] iArr = gF2Polynomial2.value;
                iArr[i] = iArr[i] ^ gF2Polynomial.value[i];
                i++;
            }
        } else {
            gF2Polynomial2 = new GF2Polynomial(gF2Polynomial);
            while (i < min) {
                int[] iArr2 = gF2Polynomial2.value;
                iArr2[i] = iArr2[i] ^ this.value[i];
                i++;
            }
        }
        gF2Polynomial2.zeroUnusedBits();
        return gF2Polynomial2;
    }

    public void xorBit(int i) throws RuntimeException {
        if (i < 0 || i > this.len - 1) {
            throw new RuntimeException();
        }
        int[] iArr = this.value;
        int i2 = i >>> 5;
        iArr[i2] = bitMask[i & 31] ^ iArr[i2];
    }

    public void xorThisBy(GF2Polynomial gF2Polynomial) {
        for (int i = 0; i < Math.min(this.blocks, gF2Polynomial.blocks); i++) {
            int[] iArr = this.value;
            iArr[i] = iArr[i] ^ gF2Polynomial.value[i];
        }
        zeroUnusedBits();
    }
}
