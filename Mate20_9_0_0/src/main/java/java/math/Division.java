package java.math;

class Division {
    Division() {
    }

    static int divideArrayByInt(int[] quotient, int[] dividend, int dividendLength, int divisor) {
        int i = divisor;
        long rem = 0;
        long j = 4294967295L;
        long bLong = ((long) i) & 4294967295L;
        for (int i2 = dividendLength - 1; i2 >= 0; i2--) {
            long quot;
            long temp = (rem << 32) | (((long) dividend[i2]) & j);
            if (temp >= 0) {
                quot = temp / bLong;
                rem = temp % bLong;
            } else {
                long aPos = temp >>> 1;
                j = (long) (i >>> 1);
                long j2 = aPos / j;
                long rem2 = ((aPos % j) << 1) + (temp & 1);
                if ((i & 1) != 0) {
                    if (j2 <= rem2) {
                        rem2 -= j2;
                    } else {
                        if (j2 - rem2 <= bLong) {
                            rem2 += bLong - j2;
                            quot = j2 - 1;
                        } else {
                            rem2 += (bLong << 1) - j2;
                            quot = j2 - 2;
                        }
                        rem = rem2;
                    }
                }
                quot = j2;
                rem = rem2;
            }
            j = 4294967295L;
            quotient[i2] = (int) (quot & 4294967295L);
        }
        return (int) rem;
    }
}
