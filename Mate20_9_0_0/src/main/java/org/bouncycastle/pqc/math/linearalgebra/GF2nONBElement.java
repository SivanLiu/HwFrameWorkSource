package org.bouncycastle.pqc.math.linearalgebra;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.asn1.cmc.BodyPartID;

public class GF2nONBElement extends GF2nElement {
    private static final int MAXLONG = 64;
    private static final long[] mBitmask = new long[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648L, 4294967296L, 8589934592L, 17179869184L, 34359738368L, 68719476736L, 137438953472L, 274877906944L, 549755813888L, 1099511627776L, 2199023255552L, 4398046511104L, 8796093022208L, 17592186044416L, 35184372088832L, 70368744177664L, 140737488355328L, 281474976710656L, 562949953421312L, 1125899906842624L, 2251799813685248L, 4503599627370496L, 9007199254740992L, 18014398509481984L, 36028797018963968L, 72057594037927936L, 144115188075855872L, 288230376151711744L, 576460752303423488L, 1152921504606846976L, 2305843009213693952L, 4611686018427387904L, Long.MIN_VALUE};
    private static final int[] mIBY64 = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5};
    private static final long[] mMaxmask = new long[]{1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, 2147483647L, BodyPartID.bodyIdMax, 8589934591L, 17179869183L, 34359738367L, 68719476735L, 137438953471L, 274877906943L, 549755813887L, 1099511627775L, 2199023255551L, 4398046511103L, 8796093022207L, 17592186044415L, 35184372088831L, 70368744177663L, 140737488355327L, 281474976710655L, 562949953421311L, 1125899906842623L, 2251799813685247L, 4503599627370495L, 9007199254740991L, 18014398509481983L, 36028797018963967L, 72057594037927935L, 144115188075855871L, 288230376151711743L, 576460752303423487L, 1152921504606846975L, 2305843009213693951L, 4611686018427387903L, Long.MAX_VALUE, -1};
    private int mBit;
    private int mLength;
    private long[] mPol;

    public GF2nONBElement(GF2nONBElement gF2nONBElement) {
        this.mField = gF2nONBElement.mField;
        this.mDegree = this.mField.getDegree();
        this.mLength = ((GF2nONBField) this.mField).getONBLength();
        this.mBit = ((GF2nONBField) this.mField).getONBBit();
        this.mPol = new long[this.mLength];
        assign(gF2nONBElement.getElement());
    }

    public GF2nONBElement(GF2nONBField gF2nONBField, BigInteger bigInteger) {
        this.mField = gF2nONBField;
        this.mDegree = this.mField.getDegree();
        this.mLength = gF2nONBField.getONBLength();
        this.mBit = gF2nONBField.getONBBit();
        this.mPol = new long[this.mLength];
        assign(bigInteger);
    }

    public GF2nONBElement(GF2nONBField gF2nONBField, SecureRandom secureRandom) {
        this.mField = gF2nONBField;
        this.mDegree = this.mField.getDegree();
        this.mLength = gF2nONBField.getONBLength();
        this.mBit = gF2nONBField.getONBBit();
        this.mPol = new long[this.mLength];
        int i = 0;
        if (this.mLength > 1) {
            while (i < this.mLength - 1) {
                this.mPol[i] = secureRandom.nextLong();
                i++;
            }
            this.mPol[this.mLength - 1] = secureRandom.nextLong() >>> (64 - this.mBit);
            return;
        }
        this.mPol[0] = secureRandom.nextLong();
        this.mPol[0] = this.mPol[0] >>> (64 - this.mBit);
    }

    public GF2nONBElement(GF2nONBField gF2nONBField, byte[] bArr) {
        this.mField = gF2nONBField;
        this.mDegree = this.mField.getDegree();
        this.mLength = gF2nONBField.getONBLength();
        this.mBit = gF2nONBField.getONBBit();
        this.mPol = new long[this.mLength];
        assign(bArr);
    }

    private GF2nONBElement(GF2nONBField gF2nONBField, long[] jArr) {
        this.mField = gF2nONBField;
        this.mDegree = this.mField.getDegree();
        this.mLength = gF2nONBField.getONBLength();
        this.mBit = gF2nONBField.getONBBit();
        this.mPol = jArr;
    }

    public static GF2nONBElement ONE(GF2nONBField gF2nONBField) {
        int oNBLength = gF2nONBField.getONBLength();
        long[] jArr = new long[oNBLength];
        int i = 0;
        while (true) {
            int i2 = oNBLength - 1;
            if (i < i2) {
                jArr[i] = -1;
                i++;
            } else {
                jArr[i2] = mMaxmask[gF2nONBField.getONBBit() - 1];
                return new GF2nONBElement(gF2nONBField, jArr);
            }
        }
    }

    public static GF2nONBElement ZERO(GF2nONBField gF2nONBField) {
        return new GF2nONBElement(gF2nONBField, new long[gF2nONBField.getONBLength()]);
    }

    private void assign(BigInteger bigInteger) {
        assign(bigInteger.toByteArray());
    }

    private void assign(byte[] bArr) {
        this.mPol = new long[this.mLength];
        for (int i = 0; i < bArr.length; i++) {
            long[] jArr = this.mPol;
            int i2 = i >>> 3;
            jArr[i2] = jArr[i2] | ((((long) bArr[(bArr.length - 1) - i]) & 255) << ((i & 7) << 3));
        }
    }

    private void assign(long[] jArr) {
        System.arraycopy(jArr, 0, this.mPol, 0, this.mLength);
    }

    private long[] getElement() {
        Object obj = new long[this.mPol.length];
        System.arraycopy(this.mPol, 0, obj, 0, this.mPol.length);
        return obj;
    }

    private long[] getElementReverseOrder() {
        long[] jArr = new long[this.mPol.length];
        for (int i = 0; i < this.mDegree; i++) {
            if (testBit((this.mDegree - i) - 1)) {
                int i2 = i >>> 6;
                jArr[i2] = jArr[i2] | mBitmask[i & 63];
            }
        }
        return jArr;
    }

    public GFElement add(GFElement gFElement) throws RuntimeException {
        GFElement gF2nONBElement = new GF2nONBElement(this);
        gF2nONBElement.addToThis(gFElement);
        return gF2nONBElement;
    }

    public void addToThis(GFElement gFElement) throws RuntimeException {
        if (gFElement instanceof GF2nONBElement) {
            GF2nONBElement gF2nONBElement = (GF2nONBElement) gFElement;
            if (this.mField.equals(gF2nONBElement.mField)) {
                for (int i = 0; i < this.mLength; i++) {
                    long[] jArr = this.mPol;
                    jArr[i] = jArr[i] ^ gF2nONBElement.mPol[i];
                }
                return;
            }
            throw new RuntimeException();
        }
        throw new RuntimeException();
    }

    void assignOne() {
        for (int i = 0; i < this.mLength - 1; i++) {
            this.mPol[i] = -1;
        }
        this.mPol[this.mLength - 1] = mMaxmask[this.mBit - 1];
    }

    void assignZero() {
        this.mPol = new long[this.mLength];
    }

    public Object clone() {
        return new GF2nONBElement(this);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GF2nONBElement)) {
            return false;
        }
        GF2nONBElement gF2nONBElement = (GF2nONBElement) obj;
        for (int i = 0; i < this.mLength; i++) {
            if (this.mPol[i] != gF2nONBElement.mPol[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return this.mPol.hashCode();
    }

    public GF2nElement increase() {
        GF2nElement gF2nONBElement = new GF2nONBElement(this);
        gF2nONBElement.increaseThis();
        return gF2nONBElement;
    }

    public void increaseThis() {
        addToThis(ONE((GF2nONBField) this.mField));
    }

    public GFElement invert() throws ArithmeticException {
        GFElement gF2nONBElement = new GF2nONBElement(this);
        gF2nONBElement.invertThis();
        return gF2nONBElement;
    }

    public void invertThis() throws ArithmeticException {
        if (isZero()) {
            throw new ArithmeticException();
        }
        int i = 31;
        int i2 = 0;
        while (i2 == 0 && i >= 0) {
            if ((((long) (this.mDegree - 1)) & mBitmask[i]) != 0) {
                i2 = 1;
            }
            i--;
        }
        i++;
        ZERO((GF2nONBField) this.mField);
        GF2nElement gF2nONBElement = new GF2nONBElement(this);
        int i3 = 1;
        for (i--; i >= 0; i--) {
            GF2nElement gF2nElement = (GF2nElement) gF2nONBElement.clone();
            for (int i4 = 1; i4 <= i3; i4++) {
                gF2nElement.squareThis();
            }
            gF2nONBElement.multiplyThisBy(gF2nElement);
            i3 <<= 1;
            if ((((long) (this.mDegree - 1)) & mBitmask[i]) != 0) {
                gF2nONBElement.squareThis();
                gF2nONBElement.multiplyThisBy(this);
                i3++;
            }
        }
        gF2nONBElement.squareThis();
    }

    public boolean isOne() {
        boolean z = true;
        for (int i = 0; i < this.mLength - 1 && z; i++) {
            z = z && (this.mPol[i] & -1) == -1;
        }
        if (z) {
            if (z && (this.mPol[this.mLength - 1] & mMaxmask[this.mBit - 1]) == mMaxmask[this.mBit - 1]) {
                return true;
            }
            z = false;
        }
        return z;
    }

    public boolean isZero() {
        boolean z = true;
        for (int i = 0; i < this.mLength && z; i++) {
            z = z && (this.mPol[i] & -1) == 0;
        }
        return z;
    }

    public GFElement multiply(GFElement gFElement) throws RuntimeException {
        GFElement gF2nONBElement = new GF2nONBElement(this);
        gF2nONBElement.multiplyThisBy(gFElement);
        return gF2nONBElement;
    }

    public void multiplyThisBy(GFElement gFElement) throws RuntimeException {
        GFElement gFElement2 = gFElement;
        if (gFElement2 instanceof GF2nONBElement) {
            GF2nONBElement gF2nONBElement = (GF2nONBElement) gFElement2;
            if (!this.mField.equals(gF2nONBElement.mField)) {
                throw new RuntimeException();
            } else if (equals(gFElement)) {
                squareThis();
                return;
            } else {
                long[] jArr = this.mPol;
                long[] jArr2 = gF2nONBElement.mPol;
                long[] jArr3 = new long[this.mLength];
                int[][] iArr = ((GF2nONBField) this.mField).mMult;
                int i = this.mLength - 1;
                int i2 = this.mBit - 1;
                long j = mBitmask[63];
                long j2 = mBitmask[i2];
                int i3 = 0;
                int i4 = 0;
                while (i4 < this.mDegree) {
                    int i5;
                    Object obj;
                    Object obj2;
                    int i6 = i3;
                    int i7 = i6;
                    while (i6 < this.mDegree) {
                        i5 = mIBY64[i6];
                        int i8 = i6 & 63;
                        int i9 = mIBY64[iArr[i6][i3]];
                        int i10 = iArr[i6][i3] & 63;
                        if ((jArr[i5] & mBitmask[i8]) != 0) {
                            if ((jArr2[i9] & mBitmask[i10]) != 0) {
                                i7 ^= 1;
                            }
                            if (iArr[i6][1] != -1) {
                                if ((jArr2[mIBY64[iArr[i6][1]]] & mBitmask[iArr[i6][1] & 63]) != 0) {
                                    i7 ^= 1;
                                }
                            }
                        }
                        i6++;
                        i3 = 0;
                    }
                    i5 = mIBY64[i4];
                    i3 = i4 & 63;
                    if (i7 != 0) {
                        jArr3[i5] = jArr3[i5] ^ mBitmask[i3];
                    }
                    if (this.mLength > 1) {
                        i3 = i - 1;
                        Object obj3 = (jArr[i] & 1) == 1 ? 1 : null;
                        i5 = i3;
                        while (i5 >= 0) {
                            Object obj4 = (jArr[i5] & 1) != 0 ? 1 : null;
                            jArr[i5] = jArr[i5] >>> 1;
                            if (obj3 != null) {
                                jArr[i5] = jArr[i5] ^ j;
                            }
                            i5--;
                            obj3 = obj4;
                        }
                        jArr[i] = jArr[i] >>> 1;
                        if (obj3 != null) {
                            jArr[i] = jArr[i] ^ j2;
                        }
                        obj = (jArr2[i] & 1) == 1 ? 1 : null;
                        while (i3 >= 0) {
                            obj3 = (jArr2[i3] & 1) != 0 ? 1 : null;
                            jArr2[i3] = jArr2[i3] >>> 1;
                            if (obj != null) {
                                jArr2[i3] = jArr2[i3] ^ j;
                            }
                            i3--;
                            obj = obj3;
                        }
                        jArr2[i] = jArr2[i] >>> 1;
                        if (obj != null) {
                            jArr2[i] = jArr2[i] ^ j2;
                        }
                        i3 = 0;
                        obj2 = 1;
                    } else {
                        i3 = 0;
                        i5 = (jArr[0] & 1) == 1 ? 1 : 0;
                        jArr[0] = jArr[0] >>> 1;
                        if (i5 != 0) {
                            jArr[0] = jArr[0] ^ j2;
                        }
                        i5 = (jArr2[0] & 1) == 1 ? 1 : 0;
                        obj2 = 1;
                        jArr2[0] = jArr2[0] >>> 1;
                        if (i5 != 0) {
                            jArr2[0] = jArr2[0] ^ j2;
                        }
                    }
                    i4++;
                    obj = obj2;
                }
                assign(jArr3);
                return;
            }
        }
        throw new RuntimeException("The elements have different representation: not yet implemented");
    }

    void reverseOrder() {
        this.mPol = getElementReverseOrder();
    }

    public GF2nElement solveQuadraticEquation() throws RuntimeException {
        int i = 1;
        if (trace() != 1) {
            long j = mBitmask[63];
            long[] jArr = new long[this.mLength];
            int i2 = 0;
            long j2 = 0;
            while (i2 < this.mLength - i) {
                long j3 = j2;
                int i3 = i;
                while (i3 < 64) {
                    if (((mBitmask[i3] & this.mPol[i2]) == 0 || (mBitmask[i3 - 1] & j3) == 0) && !((this.mPol[i2] & mBitmask[i3]) == 0 && (mBitmask[i3 - 1] & j3) == 0)) {
                        j3 ^= mBitmask[i3];
                    }
                    i3++;
                }
                jArr[i2] = j3;
                i = ((j & j3) > 0 ? 1 : ((j & j3) == 0 ? 0 : -1));
                j2 = ((i == 0 || (this.mPol[i2 + 1] & 1) != 1) && !(i == 0 && (this.mPol[i2 + 1] & 1) == 0)) ? 1 : 0;
                i2++;
                i = 1;
            }
            i = this.mDegree & 63;
            long j4 = this.mPol[this.mLength - 1];
            int i4 = 1;
            while (i4 < i) {
                if (((mBitmask[i4] & j4) == 0 || (mBitmask[i4 - 1] & j2) == 0) && !((mBitmask[i4] & j4) == 0 && (mBitmask[i4 - 1] & j2) == 0)) {
                    j2 = mBitmask[i4] ^ j2;
                }
                i4++;
            }
            jArr[this.mLength - 1] = j2;
            return new GF2nONBElement((GF2nONBField) this.mField, jArr);
        }
        throw new RuntimeException();
    }

    public GF2nElement square() {
        GF2nElement gF2nONBElement = new GF2nONBElement(this);
        gF2nONBElement.squareThis();
        return gF2nONBElement;
    }

    public GF2nElement squareRoot() {
        GF2nElement gF2nONBElement = new GF2nONBElement(this);
        gF2nONBElement.squareRootThis();
        return gF2nONBElement;
    }

    public void squareRootThis() {
        long[] element = getElement();
        int i = this.mLength - 1;
        int i2 = this.mBit - 1;
        long j = mBitmask[63];
        int i3 = (element[0] & 1) != 0 ? 1 : 0;
        int i4 = i;
        while (i4 >= 0) {
            int i5 = (element[i4] & 1) != 0 ? 1 : 0;
            element[i4] = element[i4] >>> 1;
            if (i3 != 0) {
                if (i4 == i) {
                    element[i4] = element[i4] ^ mBitmask[i2];
                } else {
                    element[i4] = element[i4] ^ j;
                }
            }
            i4--;
            i3 = i5;
        }
        assign(element);
    }

    public void squareThis() {
        long[] element = getElement();
        int i = this.mLength - 1;
        int i2 = this.mBit - 1;
        long j = mBitmask[63];
        int i3 = 0;
        int i4 = (element[i] & mBitmask[i2]) != 0 ? 1 : 0;
        int i5 = 0;
        while (i5 < i) {
            int i6 = (element[i5] & j) != 0 ? 1 : 0;
            element[i5] = element[i5] << 1;
            if (i4 != 0) {
                element[i5] = element[i5] ^ 1;
            }
            i5++;
            i4 = i6;
        }
        if ((element[i] & mBitmask[i2]) != 0) {
            i3 = 1;
        }
        element[i] = element[i] << 1;
        if (i4 != 0) {
            element[i] = element[i] ^ 1;
        }
        if (i3 != 0) {
            element[i] = mBitmask[i2 + 1] ^ element[i];
        }
        assign(element);
    }

    boolean testBit(int i) {
        boolean z = false;
        if (i >= 0) {
            if (i > this.mDegree) {
                return false;
            }
            if ((this.mPol[i >>> 6] & mBitmask[i & 63]) != 0) {
                z = true;
            }
        }
        return z;
    }

    public boolean testRightmostBit() {
        return (this.mPol[this.mLength - 1] & mBitmask[this.mBit - 1]) != 0;
    }

    public byte[] toByteArray() {
        int i = ((this.mDegree - 1) >> 3) + 1;
        byte[] bArr = new byte[i];
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = (i2 & 7) << 3;
            bArr[(i - i2) - 1] = (byte) ((int) ((this.mPol[i2 >>> 3] & (255 << i3)) >>> i3));
        }
        return bArr;
    }

    public BigInteger toFlexiBigInt() {
        return new BigInteger(1, toByteArray());
    }

    public String toString() {
        return toString(16);
    }

    public String toString(int i) {
        String str = "";
        long[] element = getElement();
        int i2 = this.mBit;
        if (i == 2) {
            while (true) {
                i2--;
                if (i2 < 0) {
                    break;
                }
                StringBuilder stringBuilder;
                if ((element[element.length - 1] & (1 << i2)) == 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(str);
                    str = "0";
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(str);
                    str = "1";
                }
                stringBuilder.append(str);
                str = stringBuilder.toString();
            }
            for (i = element.length - 2; i >= 0; i--) {
                for (i2 = 63; i2 >= 0; i2--) {
                    StringBuilder stringBuilder2;
                    if ((element[i] & mBitmask[i2]) == 0) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(str);
                        str = "0";
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(str);
                        str = "1";
                    }
                    stringBuilder2.append(str);
                    str = stringBuilder2.toString();
                }
            }
        } else if (i == 16) {
            char[] cArr = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            for (int length = element.length - 1; length >= 0; length--) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 60)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 56)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 52)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 48)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 44)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 40)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 36)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 32)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 28)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 24)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 20)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 16)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 12)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 8)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) (element[length] >>> 4)) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(cArr[((int) element[length]) & 15]);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(" ");
                str = stringBuilder3.toString();
            }
        }
        return str;
    }

    public int trace() {
        int i = this.mLength - 1;
        int i2 = 0;
        int i3 = 0;
        int i4 = i3;
        while (i3 < i) {
            int i5 = i4;
            for (i4 = 0; i4 < 64; i4++) {
                if ((this.mPol[i3] & mBitmask[i4]) != 0) {
                    i5 ^= 1;
                }
            }
            i3++;
            i4 = i5;
        }
        i3 = this.mBit;
        while (i2 < i3) {
            if ((this.mPol[i] & mBitmask[i2]) != 0) {
                i4 ^= 1;
            }
            i2++;
        }
        return i4;
    }
}
