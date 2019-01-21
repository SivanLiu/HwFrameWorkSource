package android.icu.impl.coll;

import java.util.Arrays;

public final class CollationWeights {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private int[] maxBytes = new int[5];
    private int middleLength;
    private int[] minBytes = new int[5];
    private int rangeCount;
    private int rangeIndex;
    private WeightRange[] ranges = new WeightRange[7];

    private static final class WeightRange implements Comparable<WeightRange> {
        int count;
        long end;
        int length;
        long start;

        private WeightRange() {
        }

        public int compareTo(WeightRange other) {
            long l = this.start;
            long r = other.start;
            if (l < r) {
                return -1;
            }
            if (l > r) {
                return 1;
            }
            return 0;
        }
    }

    public void initForPrimary(boolean compressible) {
        this.middleLength = 1;
        this.minBytes[1] = 3;
        this.maxBytes[1] = 255;
        if (compressible) {
            this.minBytes[2] = 4;
            this.maxBytes[2] = 254;
        } else {
            this.minBytes[2] = 2;
            this.maxBytes[2] = 255;
        }
        this.minBytes[3] = 2;
        this.maxBytes[3] = 255;
        this.minBytes[4] = 2;
        this.maxBytes[4] = 255;
    }

    public void initForSecondary() {
        this.middleLength = 3;
        this.minBytes[1] = 0;
        this.maxBytes[1] = 0;
        this.minBytes[2] = 0;
        this.maxBytes[2] = 0;
        this.minBytes[3] = 2;
        this.maxBytes[3] = 255;
        this.minBytes[4] = 2;
        this.maxBytes[4] = 255;
    }

    public void initForTertiary() {
        this.middleLength = 3;
        this.minBytes[1] = 0;
        this.maxBytes[1] = 0;
        this.minBytes[2] = 0;
        this.maxBytes[2] = 0;
        this.minBytes[3] = 2;
        this.maxBytes[3] = 63;
        this.minBytes[4] = 2;
        this.maxBytes[4] = 63;
    }

    public boolean allocWeights(long lowerLimit, long upperLimit, int n) {
        if (!getWeightRanges(lowerLimit, upperLimit)) {
            return false;
        }
        while (true) {
            int minLength = this.ranges[0].length;
            if (allocWeightsInShortRanges(n, minLength)) {
                break;
            } else if (minLength == 4) {
                return false;
            } else {
                if (allocWeightsInMinLengthRanges(n, minLength)) {
                    break;
                }
                int i = 0;
                while (i < this.rangeCount && this.ranges[i].length == minLength) {
                    lengthenRange(this.ranges[i]);
                    i++;
                }
            }
        }
        this.rangeIndex = 0;
        if (this.rangeCount < this.ranges.length) {
            this.ranges[this.rangeCount] = null;
        }
        return true;
    }

    public long nextWeight() {
        if (this.rangeIndex >= this.rangeCount) {
            return 4294967295L;
        }
        WeightRange range = this.ranges[this.rangeIndex];
        long weight = range.start;
        int i = range.count - 1;
        range.count = i;
        if (i == 0) {
            this.rangeIndex++;
        } else {
            range.start = incWeight(weight, range.length);
        }
        return weight;
    }

    public static int lengthOfWeight(long weight) {
        if ((16777215 & weight) == 0) {
            return 1;
        }
        if ((65535 & weight) == 0) {
            return 2;
        }
        if ((255 & weight) == 0) {
            return 3;
        }
        return 4;
    }

    private static int getWeightTrail(long weight, int length) {
        return ((int) (weight >> (8 * (4 - length)))) & 255;
    }

    private static long setWeightTrail(long weight, int length, int trail) {
        int length2 = 8 * (4 - length);
        return ((CollationRootElements.PRIMARY_SENTINEL << length2) & weight) | (((long) trail) << length2);
    }

    private static int getWeightByte(long weight, int idx) {
        return getWeightTrail(weight, idx);
    }

    private static long setWeightByte(long weight, int idx, int b) {
        long mask;
        idx *= 8;
        if (idx < 32) {
            mask = 4294967295L >> idx;
        } else {
            mask = 0;
        }
        int idx2 = 32 - idx;
        return (weight & (mask | (CollationRootElements.PRIMARY_SENTINEL << idx2))) | (((long) b) << idx2);
    }

    private static long truncateWeight(long weight, int length) {
        return (4294967295L << (8 * (4 - length))) & weight;
    }

    private static long incWeightTrail(long weight, int length) {
        return (1 << (8 * (4 - length))) + weight;
    }

    private static long decWeightTrail(long weight, int length) {
        return weight - (1 << (8 * (4 - length)));
    }

    private int countBytes(int idx) {
        return (this.maxBytes[idx] - this.minBytes[idx]) + 1;
    }

    private long incWeight(long weight, int length) {
        while (true) {
            int b = getWeightByte(weight, length);
            if (b < this.maxBytes[length]) {
                return setWeightByte(weight, length, b + 1);
            }
            weight = setWeightByte(weight, length, this.minBytes[length]);
            length--;
        }
    }

    private long incWeightByOffset(long weight, int length, int offset) {
        while (true) {
            offset += getWeightByte(weight, length);
            if (offset <= this.maxBytes[length]) {
                return setWeightByte(weight, length, offset);
            }
            offset -= this.minBytes[length];
            weight = setWeightByte(weight, length, this.minBytes[length] + (offset % countBytes(length)));
            offset /= countBytes(length);
            length--;
        }
    }

    private void lengthenRange(WeightRange range) {
        int length = range.length + 1;
        range.start = setWeightTrail(range.start, length, this.minBytes[length]);
        range.end = setWeightTrail(range.end, length, this.maxBytes[length]);
        range.count *= countBytes(length);
        range.length = length;
    }

    private boolean getWeightRanges(long lowerLimit, long upperLimit) {
        long j = upperLimit;
        int lowerLength = lengthOfWeight(lowerLimit);
        int upperLength = lengthOfWeight(upperLimit);
        if (lowerLimit >= j) {
            return false;
        }
        if (lowerLength < upperLength && lowerLimit == truncateWeight(j, lowerLength)) {
            return false;
        }
        int upperLength2;
        int length;
        WeightRange[] lower;
        boolean z;
        WeightRange[] lower2 = new WeightRange[5];
        AnonymousClass1 anonymousClass1 = null;
        WeightRange middle = new WeightRange();
        WeightRange[] upper = new WeightRange[5];
        long weight = lowerLimit;
        int length2 = lowerLength;
        while (length2 > this.middleLength) {
            int lowerLength2;
            int trail = getWeightTrail(weight, length2);
            if (trail < this.maxBytes[length2]) {
                lower2[length2] = new WeightRange();
                lower2[length2].start = incWeightTrail(weight, length2);
                lowerLength2 = lowerLength;
                upperLength2 = upperLength;
                lower2[length2].end = setWeightTrail(weight, length2, this.maxBytes[length2]);
                lower2[length2].length = length2;
                lower2[length2].count = this.maxBytes[length2] - trail;
            } else {
                lowerLength2 = lowerLength;
                upperLength2 = upperLength;
            }
            weight = truncateWeight(weight, length2 - 1);
            length2--;
            lowerLength = lowerLength2;
            upperLength = upperLength2;
        }
        upperLength2 = upperLength;
        if (weight < 4278190080L) {
            middle.start = incWeightTrail(weight, this.middleLength);
        } else {
            middle.start = 4294967295L;
        }
        long weight2 = j;
        for (length = upperLength2; length > this.middleLength; length--) {
            int trail2 = getWeightTrail(weight2, length);
            if (trail2 > this.minBytes[length]) {
                upper[length] = new WeightRange();
                upper[length].start = setWeightTrail(weight2, length, this.minBytes[length]);
                upper[length].end = decWeightTrail(weight2, length);
                upper[length].length = length;
                upper[length].count = trail2 - this.minBytes[length];
            }
            weight2 = truncateWeight(weight2, length - 1);
        }
        middle.end = decWeightTrail(weight2, this.middleLength);
        middle.length = this.middleLength;
        if (middle.end >= middle.start) {
            middle.count = ((int) ((middle.end - middle.start) >> (8 * (4 - this.middleLength)))) + 1;
            lower = lower2;
        } else {
            length = 4;
            while (length > this.middleLength) {
                AnonymousClass1 anonymousClass12;
                if (lower2[length] == null || upper[length] == null || lower2[length].count <= 0 || upper[length].count <= 0) {
                    lower = lower2;
                    anonymousClass12 = anonymousClass1;
                } else {
                    long lowerEnd = lower2[length].end;
                    long upperStart = upper[length].start;
                    boolean merged = false;
                    if (lowerEnd > upperStart) {
                        lower = lower2;
                        lower2[length].end = upper[length].end;
                        lower[length].count = (getWeightTrail(lower[length].end, length) - getWeightTrail(lower[length].start, length)) + 1;
                        merged = true;
                    } else {
                        lower = lower2;
                        if (lowerEnd != upperStart && incWeight(lowerEnd, length) == upperStart) {
                            lower[length].end = upper[length].end;
                            WeightRange weightRange = lower[length];
                            weightRange.count += upper[length].count;
                            merged = true;
                        }
                    }
                    if (merged) {
                        upper[length].count = 0;
                        while (true) {
                            length--;
                            if (length <= this.middleLength) {
                                break;
                            }
                            upper[length] = null;
                            lower[length] = null;
                        }
                    } else {
                        anonymousClass12 = null;
                    }
                }
                length--;
                anonymousClass1 = anonymousClass12;
                lower2 = lower;
                j = upperLimit;
            }
            lower = lower2;
        }
        this.rangeCount = 0;
        if (middle.count > 0) {
            this.ranges[0] = middle;
            z = true;
            this.rangeCount = 1;
        } else {
            z = true;
        }
        length = this.middleLength + z;
        while (length <= 4) {
            WeightRange[] weightRangeArr;
            int i;
            if (upper[length] != null && upper[length].count > 0) {
                weightRangeArr = this.ranges;
                i = this.rangeCount;
                this.rangeCount = i + 1;
                weightRangeArr[i] = upper[length];
            }
            if (lower[length] != null && lower[length].count > 0) {
                weightRangeArr = this.ranges;
                i = this.rangeCount;
                this.rangeCount = i + 1;
                weightRangeArr[i] = lower[length];
            }
            length++;
        }
        if (this.rangeCount <= 0) {
            z = false;
        }
        return z;
    }

    private boolean allocWeightsInShortRanges(int n, int minLength) {
        int n2 = n;
        n = 0;
        while (n < this.rangeCount && this.ranges[n].length <= minLength + 1) {
            if (n2 <= this.ranges[n].count) {
                if (this.ranges[n].length > minLength) {
                    this.ranges[n].count = n2;
                }
                this.rangeCount = n + 1;
                if (this.rangeCount > 1) {
                    Arrays.sort(this.ranges, 0, this.rangeCount);
                }
                return true;
            }
            n2 -= this.ranges[n].count;
            n++;
        }
        return false;
    }

    private boolean allocWeightsInMinLengthRanges(int n, int minLength) {
        int i = n;
        int i2 = minLength;
        int count = 0;
        int minLengthRangeCount = 0;
        while (minLengthRangeCount < this.rangeCount && this.ranges[minLengthRangeCount].length == i2) {
            count += this.ranges[minLengthRangeCount].count;
            minLengthRangeCount++;
        }
        int nextCountBytes = countBytes(i2 + 1);
        if (i > count * nextCountBytes) {
            return false;
        }
        int i3;
        boolean z;
        long start = this.ranges[0].start;
        long end = this.ranges[0].end;
        long start2 = start;
        for (i3 = 1; i3 < minLengthRangeCount; i3++) {
            if (this.ranges[i3].start < start2) {
                start2 = this.ranges[i3].start;
            }
            if (this.ranges[i3].end > end) {
                end = this.ranges[i3].end;
            }
        }
        i3 = (i - count) / (nextCountBytes - 1);
        int count1 = count - i3;
        if (i3 == 0 || (i3 * nextCountBytes) + count1 < i) {
            i3++;
            count1--;
        }
        this.ranges[0].start = start2;
        if (count1 == 0) {
            this.ranges[0].end = end;
            this.ranges[0].count = count;
            lengthenRange(this.ranges[0]);
            this.rangeCount = 1;
            long j = end;
            z = true;
        } else {
            long end2 = end;
            this.ranges[0].end = incWeightByOffset(start2, i2, count1 - 1);
            this.ranges[0].count = count1;
            z = true;
            if (this.ranges[1] == null) {
                this.ranges[1] = new WeightRange();
            }
            this.ranges[1].start = incWeight(this.ranges[0].end, i2);
            this.ranges[1].end = end2;
            this.ranges[1].length = i2;
            this.ranges[1].count = i3;
            lengthenRange(this.ranges[1]);
            this.rangeCount = 2;
        }
        return z;
    }
}
