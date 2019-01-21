package com.huawei.zxing.oned;

import com.huawei.android.app.AppOpsManagerEx;
import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitArray;
import java.util.Arrays;
import java.util.Map;

public final class CodaBarReader extends OneDReader {
    static final char[] ALPHABET = ALPHABET_STRING.toCharArray();
    private static final String ALPHABET_STRING = "0123456789-$:/.+ABCD";
    static final int[] CHARACTER_ENCODINGS = new int[]{3, 6, 9, 96, 18, 66, 33, 36, 48, 72, 12, 24, 69, 81, 84, 21, 26, 41, 11, 14};
    private static final int MAX_ACCEPTABLE = 512;
    private static final int MIN_CHARACTER_LENGTH = 3;
    private static final int PADDING = 384;
    private static final char[] STARTEND_ENCODING = new char[]{'A', 'B', 'C', 'D'};
    private int counterLength = 0;
    private int[] counters = new int[80];
    private final StringBuilder decodeRowResult = new StringBuilder(20);

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException {
        int charOffset;
        int i = rowNumber;
        Map map = hints;
        Arrays.fill(this.counters, 0);
        setCounters(row);
        int startOffset = findStartPattern();
        int nextStart = startOffset;
        this.decodeRowResult.setLength(0);
        while (true) {
            charOffset = toNarrowWidePattern(nextStart);
            if (charOffset != -1) {
                this.decodeRowResult.append((char) charOffset);
                nextStart += 8;
                if ((this.decodeRowResult.length() <= 1 || !arrayContains(STARTEND_ENCODING, ALPHABET[charOffset])) && nextStart < this.counterLength) {
                    Map<DecodeHintType, ?> map2 = hints;
                }
            } else {
                throw NotFoundException.getNotFoundInstance();
            }
        }
        charOffset = this.counters[nextStart - 1];
        int lastPatternSize = 0;
        for (int i2 = -8; i2 < -1; i2++) {
            lastPatternSize += this.counters[nextStart + i2];
        }
        if (nextStart >= this.counterLength || charOffset >= lastPatternSize / 2) {
            validatePattern(startOffset);
            for (int i3 = 0; i3 < this.decodeRowResult.length(); i3++) {
                this.decodeRowResult.setCharAt(i3, ALPHABET[this.decodeRowResult.charAt(i3)]);
            }
            if (arrayContains(STARTEND_ENCODING, this.decodeRowResult.charAt(0))) {
                if (!arrayContains(STARTEND_ENCODING, this.decodeRowResult.charAt(this.decodeRowResult.length() - 1))) {
                    throw NotFoundException.getNotFoundInstance();
                } else if (this.decodeRowResult.length() > 3) {
                    if (map2 == null || !map2.containsKey(DecodeHintType.RETURN_CODABAR_START_END)) {
                        this.decodeRowResult.deleteCharAt(this.decodeRowResult.length() - 1);
                        this.decodeRowResult.deleteCharAt(0);
                    }
                    int runningCount = 0;
                    for (int i4 = 0; i4 < startOffset; i4++) {
                        runningCount += this.counters[i4];
                    }
                    float left = (float) runningCount;
                    int runningCount2 = runningCount;
                    for (runningCount = startOffset; runningCount < nextStart - 1; runningCount++) {
                        runningCount2 += this.counters[runningCount];
                    }
                    float right = (float) runningCount2;
                    return new Result(this.decodeRowResult.toString(), null, new ResultPoint[]{new ResultPoint(left, (float) i), new ResultPoint(right, (float) i)}, BarcodeFormat.CODABAR);
                } else {
                    throw NotFoundException.getNotFoundInstance();
                }
            }
            throw NotFoundException.getNotFoundInstance();
        }
        throw NotFoundException.getNotFoundInstance();
    }

    void validatePattern(int start) throws NotFoundException {
        int pattern;
        int i;
        int[] sizes = new int[]{0, 0, 0, 0};
        int[] counts = new int[]{0, 0, 0, 0};
        int end = this.decodeRowResult.length() - 1;
        int i2 = 0;
        int pos = start;
        int i3 = 0;
        while (true) {
            pattern = CHARACTER_ENCODINGS[this.decodeRowResult.charAt(i3)];
            for (int j = 6; j >= 0; j--) {
                int category = (j & 1) + ((pattern & 1) * 2);
                sizes[category] = sizes[category] + this.counters[pos + j];
                counts[category] = counts[category] + 1;
                pattern >>= 1;
            }
            if (i3 >= end) {
                break;
            }
            pos += 8;
            i3++;
        }
        int[] maxes = new int[4];
        int[] mins = new int[4];
        for (i = 0; i < 2; i++) {
            mins[i] = 0;
            mins[i + 2] = (((sizes[i] << 8) / counts[i]) + ((sizes[i + 2] << 8) / counts[i + 2])) >> 1;
            maxes[i] = mins[i + 2];
            maxes[i + 2] = ((sizes[i + 2] * MAX_ACCEPTABLE) + PADDING) / counts[i + 2];
        }
        i = start;
        loop3:
        while (true) {
            i3 = i2;
            pos = CHARACTER_ENCODINGS[this.decodeRowResult.charAt(i3)];
            i2 = 6;
            while (i2 >= 0) {
                pattern = (i2 & 1) + ((pos & 1) * 2);
                int size = this.counters[i + i2] << 8;
                if (size >= mins[pattern] && size <= maxes[pattern]) {
                    pos >>= 1;
                    i2--;
                }
            }
            if (i3 < end) {
                i += 8;
                i2 = i3 + 1;
            } else {
                return;
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private void setCounters(BitArray row) throws NotFoundException {
        this.counterLength = 0;
        int i = row.getNextUnset(0);
        int end = row.getSize();
        if (i < end) {
            boolean isWhite = true;
            i = 0;
            for (int i2 = i; i2 < end; i2++) {
                if ((row.get(i2) ^ isWhite) != 0) {
                    i++;
                } else {
                    counterAppend(i);
                    i = 1;
                    isWhite = !isWhite;
                }
            }
            counterAppend(i);
            return;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private void counterAppend(int e) {
        this.counters[this.counterLength] = e;
        this.counterLength++;
        if (this.counterLength >= this.counters.length) {
            int[] temp = new int[(this.counterLength * 2)];
            System.arraycopy(this.counters, 0, temp, 0, this.counterLength);
            this.counters = temp;
        }
    }

    private int findStartPattern() throws NotFoundException {
        int i = 1;
        while (i < this.counterLength) {
            int charOffset = toNarrowWidePattern(i);
            if (charOffset != -1 && arrayContains(STARTEND_ENCODING, ALPHABET[charOffset])) {
                int patternSize = 0;
                for (int j = i; j < i + 7; j++) {
                    patternSize += this.counters[j];
                }
                if (i == 1 || this.counters[i - 1] >= patternSize / 2) {
                    return i;
                }
            }
            i += 2;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    static boolean arrayContains(char[] array, char key) {
        if (array != null) {
            for (char c : array) {
                if (c == key) {
                    return true;
                }
            }
        }
        return false;
    }

    private int toNarrowWidePattern(int position) {
        int end = position + 7;
        if (end >= this.counterLength) {
            return -1;
        }
        int j;
        int currentCounter;
        int j2;
        int currentCounter2;
        int[] theCounters = this.counters;
        int minBar = Integer.MAX_VALUE;
        int maxBar = 0;
        for (j = position; j < end; j += 2) {
            currentCounter = theCounters[j];
            if (currentCounter < minBar) {
                minBar = currentCounter;
            }
            if (currentCounter > maxBar) {
                maxBar = currentCounter;
            }
        }
        j = (minBar + maxBar) / 2;
        currentCounter = 0;
        int minSpace = Integer.MAX_VALUE;
        for (j2 = position + 1; j2 < end; j2 += 2) {
            currentCounter2 = theCounters[j2];
            if (currentCounter2 < minSpace) {
                minSpace = currentCounter2;
            }
            if (currentCounter2 > currentCounter) {
                currentCounter = currentCounter2;
            }
        }
        j2 = (minSpace + currentCounter) / 2;
        int pattern = 0;
        int bitmask = AppOpsManagerEx.TYPE_MICROPHONE;
        for (currentCounter2 = 0; currentCounter2 < 7; currentCounter2++) {
            bitmask >>= 1;
            if (theCounters[position + currentCounter2] > ((currentCounter2 & 1) == 0 ? j : j2)) {
                pattern |= bitmask;
            }
        }
        int i = 0;
        while (true) {
            currentCounter2 = i;
            if (currentCounter2 >= CHARACTER_ENCODINGS.length) {
                return -1;
            }
            if (CHARACTER_ENCODINGS[currentCounter2] == pattern) {
                return currentCounter2;
            }
            i = currentCounter2 + 1;
        }
    }
}
