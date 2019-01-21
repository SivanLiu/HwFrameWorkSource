package sun.misc;

import java.math.BigInteger;
import java.util.Arrays;

public class FDBigInteger {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final long[] LONG_5_POW = new long[]{1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125, 9765625, 48828125, 244140625, 1220703125, 6103515625L, 30517578125L, 152587890625L, 762939453125L, 3814697265625L, 19073486328125L, 95367431640625L, 476837158203125L, 2384185791015625L, 11920928955078125L, 59604644775390625L, 298023223876953125L, 1490116119384765625L};
    private static final long LONG_MASK = 4294967295L;
    private static final int MAX_FIVE_POW = 340;
    private static final FDBigInteger[] POW_5_CACHE = new FDBigInteger[MAX_FIVE_POW];
    static final int[] SMALL_5_POW = new int[]{1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125, 9765625, 48828125, 244140625, 1220703125};
    public static final FDBigInteger ZERO = new FDBigInteger(new int[0], 0);
    private int[] data;
    private boolean isImmutable = $assertionsDisabled;
    private int nWords;
    private int offset;

    static {
        FDBigInteger pow5;
        int i = 0;
        while (i < SMALL_5_POW.length) {
            pow5 = new FDBigInteger(new int[]{SMALL_5_POW[i]}, 0);
            pow5.makeImmutable();
            POW_5_CACHE[i] = pow5;
            i++;
        }
        pow5 = POW_5_CACHE[i - 1];
        while (i < MAX_FIVE_POW) {
            FDBigInteger[] fDBigIntegerArr = POW_5_CACHE;
            FDBigInteger mult = pow5.mult(5);
            pow5 = mult;
            fDBigIntegerArr[i] = mult;
            pow5.makeImmutable();
            i++;
        }
        ZERO.makeImmutable();
    }

    private FDBigInteger(int[] data, int offset) {
        this.data = data;
        this.offset = offset;
        this.nWords = data.length;
        trimLeadingZeros();
    }

    public FDBigInteger(long lValue, char[] digits, int kDigits, int nDigits) {
        int ilim;
        int v = 0;
        this.data = new int[Math.max((nDigits + 8) / 9, 2)];
        this.data[0] = (int) lValue;
        this.data[1] = (int) (lValue >>> 32);
        this.offset = 0;
        this.nWords = 2;
        int i = kDigits;
        int limit = nDigits - 5;
        while (i < limit) {
            ilim = i + 5;
            int i2 = i + 1;
            i = digits[i] - 48;
            while (i2 < ilim) {
                i = ((10 * i) + digits[i2]) - 48;
                i2++;
            }
            multAddMe(100000, i);
            i = i2;
        }
        ilim = 1;
        while (i < nDigits) {
            v = ((10 * v) + digits[i]) - 48;
            ilim *= 10;
            i++;
        }
        if (ilim != 1) {
            multAddMe(ilim, v);
        }
        trimLeadingZeros();
    }

    public static FDBigInteger valueOfPow52(int p5, int p2) {
        if (p5 == 0) {
            return valueOfPow2(p2);
        }
        if (p2 == 0) {
            return big5pow(p5);
        }
        if (p5 >= SMALL_5_POW.length) {
            return big5pow(p5).leftShift(p2);
        }
        int pow5 = SMALL_5_POW[p5];
        int wordcount = p2 >> 5;
        if ((p2 & 31) == 0) {
            return new FDBigInteger(new int[]{pow5}, wordcount);
        }
        return new FDBigInteger(new int[]{pow5 << (p2 & 31), pow5 >>> (32 - (p2 & 31))}, wordcount);
    }

    public static FDBigInteger valueOfMulPow52(long value, int p5, int p2) {
        long j = value;
        int i = p5;
        int i2 = p2;
        int v0 = (int) j;
        int v1 = (int) (j >>> 32);
        int wordcount = i2 >> 5;
        int bitcount = i2 & 31;
        if (i != 0) {
            if (i < SMALL_5_POW.length) {
                long pow5 = ((long) SMALL_5_POW[i]) & LONG_MASK;
                long carry = (((long) v0) & LONG_MASK) * pow5;
                v0 = (int) carry;
                long carry2 = ((((long) v1) & LONG_MASK) * pow5) + (carry >>> 32);
                v1 = (int) carry2;
                int v2 = (int) (carry2 >>> 32);
                if (bitcount == 0) {
                    return new FDBigInteger(new int[]{v0, v1, v2}, wordcount);
                }
                return new FDBigInteger(new int[]{v0 << bitcount, (v1 << bitcount) | (v0 >>> (32 - bitcount)), (v2 << bitcount) | (v1 >>> (32 - bitcount)), v2 >>> (32 - bitcount)}, wordcount);
            }
            int[] r;
            FDBigInteger pow52 = big5pow(p5);
            if (v1 == 0) {
                r = new int[((pow52.nWords + 1) + (i2 != 0 ? 1 : 0))];
                mult(pow52.data, pow52.nWords, v0, r);
            } else {
                r = new int[((pow52.nWords + 2) + (i2 != 0 ? 1 : 0))];
                mult(pow52.data, pow52.nWords, v0, v1, r);
            }
            return new FDBigInteger(r, pow52.offset).leftShift(i2);
        } else if (i2 == 0) {
            return new FDBigInteger(new int[]{v0, v1}, 0);
        } else if (bitcount == 0) {
            return new FDBigInteger(new int[]{v0, v1}, wordcount);
        } else {
            return new FDBigInteger(new int[]{v0 << bitcount, (v1 << bitcount) | (v0 >>> (32 - bitcount)), v1 >>> (32 - bitcount)}, wordcount);
        }
    }

    private static FDBigInteger valueOfPow2(int p2) {
        return new FDBigInteger(new int[]{1 << (p2 & 31)}, p2 >> 5);
    }

    private void trimLeadingZeros() {
        int i = this.nWords;
        if (i > 0) {
            i--;
            if (this.data[i] == 0) {
                while (i > 0 && this.data[i - 1] == 0) {
                    i--;
                }
                this.nWords = i;
                if (i == 0) {
                    this.offset = 0;
                }
            }
        }
    }

    public int getNormalizationBias() {
        if (this.nWords != 0) {
            int zeros = Integer.numberOfLeadingZeros(this.data[this.nWords - 1]);
            return zeros < 4 ? 28 + zeros : zeros - 4;
        } else {
            throw new IllegalArgumentException("Zero value cannot be normalized");
        }
    }

    private static void leftShift(int[] src, int idx, int[] result, int bitcount, int anticount, int prev) {
        while (idx > 0) {
            int v = prev << bitcount;
            prev = src[idx - 1];
            result[idx] = v | (prev >>> anticount);
            idx--;
        }
        result[0] = prev << bitcount;
    }

    public FDBigInteger leftShift(int shift) {
        if (shift == 0 || this.nWords == 0) {
            return this;
        }
        int wordcount = shift >> 5;
        int bitcount = shift & 31;
        int anticount;
        int idx;
        int prev;
        int hi;
        int[] result;
        int[] src;
        if (!this.isImmutable) {
            if (bitcount != 0) {
                anticount = 32 - bitcount;
                int prev2;
                if ((this.data[0] << bitcount) == 0) {
                    int v;
                    int idx2 = 0;
                    prev2 = this.data[0];
                    while (idx2 < this.nWords - 1) {
                        v = prev2 >>> anticount;
                        prev2 = this.data[idx2 + 1];
                        this.data[idx2] = v | (prev2 << bitcount);
                        idx2++;
                    }
                    v = prev2 >>> anticount;
                    this.data[idx2] = v;
                    if (v == 0) {
                        this.nWords--;
                    }
                    this.offset++;
                } else {
                    idx = this.nWords - 1;
                    prev = this.data[idx];
                    hi = prev >>> anticount;
                    result = this.data;
                    src = this.data;
                    if (hi != 0) {
                        if (this.nWords == this.data.length) {
                            int[] iArr = new int[(this.nWords + 1)];
                            result = iArr;
                            this.data = iArr;
                        }
                        prev2 = this.nWords;
                        this.nWords = prev2 + 1;
                        result[prev2] = hi;
                    }
                    leftShift(src, idx, result, bitcount, anticount, prev);
                }
            }
            this.offset += wordcount;
            return this;
        } else if (bitcount == 0) {
            return new FDBigInteger(Arrays.copyOf(this.data, this.nWords), this.offset + wordcount);
        } else {
            anticount = 32 - bitcount;
            idx = this.nWords - 1;
            prev = this.data[idx];
            hi = prev >>> anticount;
            if (hi != 0) {
                result = new int[(this.nWords + 1)];
                result[this.nWords] = hi;
            } else {
                result = new int[this.nWords];
            }
            src = result;
            leftShift(this.data, idx, src, bitcount, anticount, prev);
            return new FDBigInteger(src, this.offset + wordcount);
        }
    }

    private int size() {
        return this.nWords + this.offset;
    }

    public int quoRemIteration(FDBigInteger S) throws IllegalArgumentException {
        FDBigInteger fDBigInteger = S;
        int thSize = size();
        int sSize = S.size();
        if (thSize < sSize) {
            int p = multAndCarryBy10(this.data, this.nWords, this.data);
            if (p != 0) {
                int[] iArr = this.data;
                int i = this.nWords;
                this.nWords = i + 1;
                iArr[i] = p;
            } else {
                trimLeadingZeros();
            }
            return 0;
        } else if (thSize <= sSize) {
            long q = (((long) this.data[this.nWords - 1]) & LONG_MASK) / (((long) fDBigInteger.data[fDBigInteger.nWords - 1]) & LONG_MASK);
            long j = 0;
            if (multDiffMe(q, fDBigInteger) != 0) {
                long sum = 0;
                int tStart = fDBigInteger.offset - this.offset;
                int[] sd = fDBigInteger.data;
                int[] td = this.data;
                while (sum == j) {
                    long sum2 = sum;
                    int sIndex = 0;
                    int tIndex = tStart;
                    while (tIndex < this.nWords) {
                        int thSize2 = thSize;
                        long sum3 = sum2 + ((((long) td[tIndex]) & LONG_MASK) + (((long) sd[sIndex]) & LONG_MASK));
                        td[tIndex] = (int) sum3;
                        sum2 = sum3 >>> 32;
                        sIndex++;
                        tIndex++;
                        thSize = thSize2;
                        fDBigInteger = S;
                    }
                    q--;
                    sum = sum2;
                    thSize = thSize;
                    fDBigInteger = S;
                    j = 0;
                }
            }
            int p2 = multAndCarryBy10(this.data, this.nWords, this.data);
            trimLeadingZeros();
            return (int) q;
        } else {
            throw new IllegalArgumentException("disparate values");
        }
    }

    public FDBigInteger multBy10() {
        if (this.nWords == 0) {
            return this;
        }
        if (this.isImmutable) {
            int[] res = new int[(this.nWords + 1)];
            res[this.nWords] = multAndCarryBy10(this.data, this.nWords, res);
            return new FDBigInteger(res, this.offset);
        }
        int p = multAndCarryBy10(this.data, this.nWords, this.data);
        if (p != 0) {
            if (this.nWords == this.data.length) {
                if (this.data[0] == 0) {
                    Object obj = this.data;
                    Object obj2 = this.data;
                    int i = this.nWords - 1;
                    this.nWords = i;
                    System.arraycopy(obj, 1, obj2, 0, i);
                    this.offset++;
                } else {
                    this.data = Arrays.copyOf(this.data, this.data.length + 1);
                }
            }
            int[] iArr = this.data;
            int i2 = this.nWords;
            this.nWords = i2 + 1;
            iArr[i2] = p;
        } else {
            trimLeadingZeros();
        }
        return this;
    }

    public FDBigInteger multByPow52(int p5, int p2) {
        if (this.nWords == 0) {
            return this;
        }
        FDBigInteger res = this;
        if (p5 != 0) {
            int extraSize = p2 != 0 ? 1 : 0;
            if (p5 < SMALL_5_POW.length) {
                int[] r = new int[((this.nWords + 1) + extraSize)];
                mult(this.data, this.nWords, SMALL_5_POW[p5], r);
                res = new FDBigInteger(r, this.offset);
            } else {
                FDBigInteger pow5 = big5pow(p5);
                int[] r2 = new int[((this.nWords + pow5.size()) + extraSize)];
                mult(this.data, this.nWords, pow5.data, pow5.nWords, r2);
                res = new FDBigInteger(r2, this.offset + pow5.offset);
            }
        }
        return res.leftShift(p2);
    }

    private static void mult(int[] s1, int s1Len, int[] s2, int s2Len, int[] dst) {
        int i = s2Len;
        for (int i2 = 0; i2 < s1Len; i2++) {
            long v = ((long) s1[i2]) & LONG_MASK;
            long p = 0;
            int j = 0;
            while (j < i) {
                p += (((long) dst[i2 + j]) & LONG_MASK) + ((((long) s2[j]) & LONG_MASK) * v);
                dst[i2 + j] = (int) p;
                p >>>= 32;
                j++;
                int i3 = s1Len;
            }
            dst[i2 + i] = (int) p;
        }
    }

    public FDBigInteger leftInplaceSub(FDBigInteger subtrahend) {
        FDBigInteger minuend;
        int[] sData;
        long diff;
        FDBigInteger fDBigInteger = subtrahend;
        if (this.isImmutable) {
            minuend = new FDBigInteger((int[]) this.data.clone(), this.offset);
        } else {
            minuend = this;
        }
        int offsetDiff = fDBigInteger.offset - minuend.offset;
        int[] sData2 = fDBigInteger.data;
        int[] mData = minuend.data;
        int subLen = fDBigInteger.nWords;
        int minLen = minuend.nWords;
        int sIndex = 0;
        if (offsetDiff < 0) {
            int rLen = minLen - offsetDiff;
            if (rLen < mData.length) {
                System.arraycopy((Object) mData, 0, (Object) mData, -offsetDiff, minLen);
                Arrays.fill(mData, 0, -offsetDiff, 0);
            } else {
                Object r = new int[rLen];
                System.arraycopy((Object) mData, 0, r, -offsetDiff, minLen);
                mData = r;
                minuend.data = r;
            }
            minuend.offset = fDBigInteger.offset;
            minLen = rLen;
            minuend.nWords = rLen;
            offsetDiff = 0;
        }
        long borrow = 0;
        int mIndex = offsetDiff;
        while (sIndex < subLen && mIndex < minLen) {
            int offsetDiff2 = offsetDiff;
            sData = sData2;
            diff = ((((long) mData[mIndex]) & LONG_MASK) - (((long) sData2[sIndex]) & LONG_MASK)) + borrow;
            mData[mIndex] = (int) diff;
            borrow = diff >> 32;
            sIndex++;
            mIndex++;
            sData2 = sData;
            offsetDiff = offsetDiff2;
            fDBigInteger = subtrahend;
        }
        sData = sData2;
        while (borrow != 0 && mIndex < minLen) {
            diff = (((long) mData[mIndex]) & LONG_MASK) + borrow;
            mData[mIndex] = (int) diff;
            borrow = diff >> 32;
            mIndex++;
        }
        minuend.trimLeadingZeros();
        return minuend;
    }

    public FDBigInteger rightInplaceSub(FDBigInteger subtrahend) {
        int rLen;
        int offsetDiff;
        FDBigInteger subtrahend2 = subtrahend;
        FDBigInteger minuend = this;
        if (subtrahend2.isImmutable) {
            subtrahend2 = new FDBigInteger((int[]) subtrahend2.data.clone(), subtrahend2.offset);
        }
        int offsetDiff2 = minuend.offset - subtrahend2.offset;
        int[] sData = subtrahend2.data;
        int[] mData = minuend.data;
        int subLen = subtrahend2.nWords;
        int minLen = minuend.nWords;
        if (offsetDiff2 < 0) {
            rLen = minLen;
            if (rLen < sData.length) {
                System.arraycopy((Object) sData, 0, (Object) sData, -offsetDiff2, subLen);
                Arrays.fill(sData, 0, -offsetDiff2, 0);
            } else {
                Object r = new int[rLen];
                System.arraycopy((Object) sData, 0, r, -offsetDiff2, subLen);
                sData = r;
                subtrahend2.data = r;
            }
            subtrahend2.offset = minuend.offset;
            subLen -= offsetDiff2;
            offsetDiff2 = 0;
        } else {
            rLen = minLen + offsetDiff2;
            if (rLen >= sData.length) {
                int[] copyOf = Arrays.copyOf(sData, rLen);
                sData = copyOf;
                subtrahend2.data = copyOf;
            }
        }
        rLen = 0;
        long borrow = 0;
        while (rLen < offsetDiff2) {
            FDBigInteger minuend2 = minuend;
            offsetDiff = offsetDiff2;
            minuend = (null - (((long) sData[rLen]) & 4294967295)) + borrow;
            sData[rLen] = (int) minuend;
            borrow = minuend >> 32;
            rLen++;
            minuend = minuend2;
            offsetDiff2 = offsetDiff;
        }
        offsetDiff = offsetDiff2;
        int mIndex = 0;
        while (true) {
            int mIndex2 = mIndex;
            int subLen2;
            if (mIndex2 < minLen) {
                int[] mData2 = mData;
                subLen2 = subLen;
                long diff = ((((long) mData[mIndex2]) & LONG_MASK) - (((long) sData[rLen]) & LONG_MASK)) + borrow;
                sData[rLen] = (int) diff;
                borrow = diff >> 32;
                rLen++;
                mIndex = mIndex2 + 1;
                mData = mData2;
                subLen = subLen2;
            } else {
                subLen2 = subLen;
                subtrahend2.nWords = rLen;
                subtrahend2.trimLeadingZeros();
                return subtrahend2;
            }
        }
    }

    private static int checkZeroTail(int[] a, int from) {
        while (from > 0) {
            from--;
            if (a[from] != 0) {
                return 1;
            }
        }
        return 0;
    }

    public int cmp(FDBigInteger other) {
        int aSize = this.nWords + this.offset;
        int bSize = other.nWords + other.offset;
        int i = 1;
        if (aSize > bSize) {
            return 1;
        }
        if (aSize < bSize) {
            return -1;
        }
        int aLen = this.nWords;
        int bLen = other.nWords;
        while (aLen > 0 && bLen > 0) {
            aLen--;
            int a = this.data[aLen];
            bLen--;
            int b = other.data[bLen];
            if (a != b) {
                if ((((long) a) & LONG_MASK) < (LONG_MASK & ((long) b))) {
                    i = -1;
                }
                return i;
            }
        }
        if (aLen > 0) {
            return checkZeroTail(this.data, aLen);
        }
        if (bLen > 0) {
            return -checkZeroTail(other.data, bLen);
        }
        return 0;
    }

    public int cmpPow52(int p5, int p2) {
        if (p5 != 0) {
            return cmp(big5pow(p5).leftShift(p2));
        }
        int wordcount = p2 >> 5;
        int bitcount = p2 & 31;
        int size = this.nWords + this.offset;
        int i = 1;
        if (size > wordcount + 1) {
            return 1;
        }
        if (size < wordcount + 1) {
            return -1;
        }
        int a = this.data[this.nWords - 1];
        int b = 1 << bitcount;
        if (a == b) {
            return checkZeroTail(this.data, this.nWords - 1);
        }
        if ((((long) a) & LONG_MASK) < (LONG_MASK & ((long) b))) {
            i = -1;
        }
        return i;
    }

    public int addAndCmp(FDBigInteger x, FDBigInteger y) {
        FDBigInteger big;
        FDBigInteger small;
        int bSize;
        int sSize;
        int xSize = x.size();
        int ySize = y.size();
        if (xSize >= ySize) {
            big = x;
            small = y;
            bSize = xSize;
            sSize = ySize;
        } else {
            big = y;
            small = x;
            bSize = ySize;
            sSize = xSize;
        }
        int thSize = size();
        int i = 1;
        if (bSize == 0) {
            if (thSize == 0) {
                i = 0;
            }
            return i;
        } else if (sSize == 0) {
            return cmp(big);
        } else {
            if (bSize > thSize) {
                return -1;
            }
            if (bSize + 1 < thSize) {
                return 1;
            }
            long top = ((long) big.data[big.nWords - 1]) & LONG_MASK;
            if (sSize == bSize) {
                top += ((long) small.data[small.nWords - 1]) & LONG_MASK;
            }
            long v;
            if ((top >>> 32) == 0) {
                if (((top + 1) >>> 32) == 0) {
                    if (bSize < thSize) {
                        return 1;
                    }
                    v = LONG_MASK & ((long) this.data[this.nWords - 1]);
                    if (v < top) {
                        return -1;
                    }
                    if (v > top + 1) {
                        return 1;
                    }
                }
            } else if (bSize + 1 > thSize) {
                return -1;
            } else {
                top >>>= 32;
                v = LONG_MASK & ((long) this.data[this.nWords - 1]);
                if (v < top) {
                    return -1;
                }
                if (v > top + 1) {
                    return 1;
                }
            }
            return cmp(big.add(small));
        }
    }

    public void makeImmutable() {
        this.isImmutable = true;
    }

    private FDBigInteger mult(int i) {
        if (this.nWords == 0) {
            return this;
        }
        int[] r = new int[(this.nWords + 1)];
        mult(this.data, this.nWords, i, r);
        return new FDBigInteger(r, this.offset);
    }

    private FDBigInteger mult(FDBigInteger other) {
        if (this.nWords == 0) {
            return this;
        }
        if (size() == 1) {
            return other.mult(this.data[0]);
        }
        if (other.nWords == 0) {
            return other;
        }
        if (other.size() == 1) {
            return mult(other.data[0]);
        }
        int[] r = new int[(this.nWords + other.nWords)];
        mult(this.data, this.nWords, other.data, other.nWords, r);
        return new FDBigInteger(r, this.offset + other.offset);
    }

    private FDBigInteger add(FDBigInteger other) {
        FDBigInteger big;
        int bigLen;
        FDBigInteger small;
        int smallLen;
        int oSize;
        int tSize = size();
        int oSize2 = other.size();
        if (tSize >= oSize2) {
            big = this;
            bigLen = tSize;
            small = other;
            smallLen = oSize2;
        } else {
            big = other;
            bigLen = oSize2;
            small = this;
            smallLen = tSize;
        }
        int[] r = new int[(bigLen + 1)];
        int i = 0;
        long carry = 0;
        while (i < smallLen) {
            long j;
            int tSize2;
            long j2;
            if (i < big.offset) {
                j = 0;
            } else {
                j = ((long) big.data[i - big.offset]) & LONG_MASK;
            }
            if (i < small.offset) {
                tSize2 = tSize;
                oSize = oSize2;
                j2 = 0;
            } else {
                tSize2 = tSize;
                oSize = oSize2;
                j2 = ((long) small.data[i - small.offset]) & LONG_MASK;
            }
            carry += j + j2;
            r[i] = (int) carry;
            carry >>= 32;
            i++;
            tSize = tSize2;
            oSize2 = oSize;
        }
        oSize = oSize2;
        while (i < bigLen) {
            carry += i < big.offset ? 0 : ((long) big.data[i - big.offset]) & LONG_MASK;
            r[i] = (int) carry;
            carry >>= 32;
            i++;
        }
        r[bigLen] = (int) carry;
        return new FDBigInteger(r, 0);
    }

    private void multAddMe(int iv, int addend) {
        long v = ((long) iv) & LONG_MASK;
        long p = ((((long) this.data[0]) & LONG_MASK) * v) + (((long) addend) & LONG_MASK);
        this.data[0] = (int) p;
        long p2 = p >>> 32;
        for (int i = 1; i < this.nWords; i++) {
            p2 += (((long) this.data[i]) & LONG_MASK) * v;
            this.data[i] = (int) p2;
            p2 >>>= 32;
        }
        if (p2 != 0) {
            int[] iArr = this.data;
            int i2 = this.nWords;
            this.nWords = i2 + 1;
            iArr[i2] = (int) p2;
        }
    }

    private long multDiffMe(long q, FDBigInteger S) {
        FDBigInteger fDBigInteger = S;
        long diff = 0;
        if (q == 0) {
            return 0;
        }
        int deltaSize = fDBigInteger.offset - this.offset;
        if (deltaSize >= 0) {
            int[] sd = fDBigInteger.data;
            int[] td = this.data;
            int sIndex = 0;
            long diff2 = 0;
            int tIndex = deltaSize;
            while (sIndex < fDBigInteger.nWords) {
                int deltaSize2 = deltaSize;
                int tIndex2 = tIndex;
                diff2 += (((long) td[tIndex]) & LONG_MASK) - ((((long) sd[sIndex]) & LONG_MASK) * q);
                td[tIndex2] = (int) diff2;
                diff2 >>= 32;
                sIndex++;
                tIndex = tIndex2 + 1;
                deltaSize = deltaSize2;
                fDBigInteger = S;
            }
            return diff2;
        }
        int[] td2;
        int deltaSize3 = -deltaSize;
        int[] rd = new int[(this.nWords + deltaSize3)];
        int sIndex2 = 0;
        int rIndex = 0;
        FDBigInteger fDBigInteger2 = S;
        int[] sd2 = fDBigInteger2.data;
        while (rIndex < deltaSize3 && sIndex2 < fDBigInteger2.nWords) {
            diff -= (((long) sd2[sIndex2]) & LONG_MASK) * q;
            rd[rIndex] = (int) diff;
            diff >>= 32;
            sIndex2++;
            rIndex++;
        }
        int tIndex3 = 0;
        int[] td3 = this.data;
        while (sIndex2 < fDBigInteger2.nWords) {
            td2 = td3;
            int[] sd3 = sd2;
            diff += (((long) td3[tIndex3]) & LONG_MASK) - ((((long) sd2[sIndex2]) & LONG_MASK) * q);
            rd[rIndex] = (int) diff;
            diff >>= 32;
            sIndex2++;
            tIndex3++;
            rIndex++;
            td3 = td2;
            sd2 = sd3;
            fDBigInteger2 = S;
        }
        td2 = td3;
        this.nWords += deltaSize3;
        this.offset -= deltaSize3;
        this.data = rd;
        return diff;
    }

    private static int multAndCarryBy10(int[] src, int srcLen, int[] dst) {
        long carry = 0;
        for (int i = 0; i < srcLen; i++) {
            long product = ((((long) src[i]) & LONG_MASK) * 10) + carry;
            dst[i] = (int) product;
            carry = product >>> 32;
        }
        return (int) carry;
    }

    private static void mult(int[] src, int srcLen, int value, int[] dst) {
        long val = ((long) value) & LONG_MASK;
        long carry = 0;
        for (int i = 0; i < srcLen; i++) {
            long product = ((((long) src[i]) & LONG_MASK) * val) + carry;
            dst[i] = (int) product;
            carry = product >>> 32;
        }
        dst[srcLen] = (int) carry;
    }

    private static void mult(int[] src, int srcLen, int v0, int v1, int[] dst) {
        long product;
        int i = srcLen;
        long v = ((long) v0) & LONG_MASK;
        int j = 0;
        long carry = 0;
        for (int j2 = 0; j2 < i; j2++) {
            product = ((((long) src[j2]) & LONG_MASK) * v) + carry;
            dst[j2] = (int) product;
            carry = product >>> 32;
        }
        dst[i] = (int) carry;
        v = ((long) v1) & LONG_MASK;
        carry = 0;
        while (j < i) {
            int j3 = j;
            product = ((((long) dst[j + 1]) & LONG_MASK) + ((((long) src[j]) & LONG_MASK) * v)) + carry;
            dst[j3 + 1] = (int) product;
            carry = product >>> 32;
            j = j3 + 1;
        }
        dst[i + 1] = (int) carry;
    }

    private static FDBigInteger big5pow(int p) {
        if (p < MAX_FIVE_POW) {
            return POW_5_CACHE[p];
        }
        return big5powRec(p);
    }

    private static FDBigInteger big5powRec(int p) {
        if (p < MAX_FIVE_POW) {
            return POW_5_CACHE[p];
        }
        int q = p >> 1;
        int r = p - q;
        FDBigInteger bigq = big5powRec(q);
        if (r < SMALL_5_POW.length) {
            return bigq.mult(SMALL_5_POW[r]);
        }
        return bigq.mult(big5powRec(r));
    }

    public String toHexString() {
        if (this.nWords == 0) {
            return "0";
        }
        int i;
        StringBuilder sb = new StringBuilder((this.nWords + this.offset) * 8);
        for (i = this.nWords - 1; i >= 0; i--) {
            String subStr = Integer.toHexString(this.data[i]);
            for (int j = subStr.length(); j < 8; j++) {
                sb.append('0');
            }
            sb.append(subStr);
        }
        for (i = this.offset; i > 0; i--) {
            sb.append("00000000");
        }
        return sb.toString();
    }

    public BigInteger toBigInteger() {
        byte[] magnitude = new byte[((this.nWords * 4) + 1)];
        for (int i = 0; i < this.nWords; i++) {
            int w = this.data[i];
            magnitude[(magnitude.length - (4 * i)) - 1] = (byte) w;
            magnitude[(magnitude.length - (4 * i)) - 2] = (byte) (w >> 8);
            magnitude[(magnitude.length - (4 * i)) - 3] = (byte) (w >> 16);
            magnitude[(magnitude.length - (4 * i)) - 4] = (byte) (w >> 24);
        }
        return new BigInteger(magnitude).shiftLeft(this.offset * 32);
    }

    public String toString() {
        return toBigInteger().toString();
    }
}
