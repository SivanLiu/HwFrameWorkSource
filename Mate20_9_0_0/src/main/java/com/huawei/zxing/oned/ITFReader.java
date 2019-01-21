package com.huawei.zxing.oned;

import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitArray;
import java.util.Map;

public final class ITFReader extends OneDReader {
    private static final int[] DEFAULT_ALLOWED_LENGTHS = new int[]{48, 44, 24, 20, 18, 16, 14, 12, 10, 8, 6};
    private static final int[] END_PATTERN_REVERSED = new int[]{1, 1, 3};
    private static final int MAX_AVG_VARIANCE = 107;
    private static final int MAX_INDIVIDUAL_VARIANCE = 199;
    private static final int N = 1;
    static final int[][] PATTERNS = new int[][]{new int[]{1, 1, 3, 3, 1}, new int[]{3, 1, 1, 1, 3}, new int[]{1, 3, 1, 1, 3}, new int[]{3, 3, 1, 1, 1}, new int[]{1, 1, 3, 1, 3}, new int[]{3, 1, 3, 1, 1}, new int[]{1, 3, 3, 1, 1}, new int[]{1, 1, 1, 3, 3}, new int[]{3, 1, 1, 3, 1}, new int[]{1, 3, 1, 3, 1}};
    private static final int[] START_PATTERN = new int[]{1, 1, 1, 1};
    private static final int W = 3;
    private int narrowLineWidth = -1;

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws FormatException, NotFoundException {
        int i = rowNumber;
        BitArray bitArray = row;
        Map<DecodeHintType, ?> map = hints;
        int[] startRange = decodeStart(bitArray);
        int[] endRange = decodeEnd(bitArray);
        StringBuilder result = new StringBuilder(20);
        decodeMiddle(bitArray, startRange[1], endRange[0], result);
        String resultString = result.toString();
        int[] allowedLengths = null;
        if (map != null) {
            allowedLengths = (int[]) map.get(DecodeHintType.ALLOWED_LENGTHS);
        }
        if (allowedLengths == null) {
            allowedLengths = DEFAULT_ALLOWED_LENGTHS;
        }
        int length = resultString.length();
        boolean lengthOK = false;
        for (int allowedLength : allowedLengths) {
            if (length == allowedLength) {
                lengthOK = true;
                break;
            }
        }
        if (lengthOK) {
            return new Result(resultString, null, new ResultPoint[]{new ResultPoint((float) startRange[1], (float) i), new ResultPoint((float) endRange[0], (float) i)}, BarcodeFormat.ITF);
        }
        throw FormatException.getFormatInstance();
    }

    private static void decodeMiddle(BitArray row, int payloadStart, int payloadEnd, StringBuilder resultString) throws NotFoundException {
        int[] counterDigitPair = new int[10];
        int[] counterBlack = new int[5];
        int[] counterWhite = new int[5];
        while (payloadStart < payloadEnd) {
            OneDReader.recordPattern(row, payloadStart, counterDigitPair);
            int i = 0;
            for (int k = 0; k < 5; k++) {
                int twoK = k << 1;
                counterBlack[k] = counterDigitPair[twoK];
                counterWhite[k] = counterDigitPair[twoK + 1];
            }
            resultString.append((char) (48 + decodeDigit(counterBlack)));
            resultString.append((char) (48 + decodeDigit(counterWhite)));
            while (i < counterDigitPair.length) {
                payloadStart += counterDigitPair[i];
                i++;
            }
        }
    }

    int[] decodeStart(BitArray row) throws NotFoundException {
        int[] startPattern = findGuardPattern(row, skipWhiteSpace(row), START_PATTERN);
        this.narrowLineWidth = (startPattern[1] - startPattern[0]) >> 2;
        validateQuietZone(row, startPattern[0]);
        return startPattern;
    }

    private void validateQuietZone(BitArray row, int startPattern) throws NotFoundException {
        int quietCount = this.narrowLineWidth * 10;
        quietCount = quietCount < startPattern ? quietCount : startPattern;
        int i = startPattern - 1;
        while (quietCount > 0 && i >= 0 && !row.get(i)) {
            quietCount--;
            i--;
        }
        if (quietCount != 0) {
            throw NotFoundException.getNotFoundInstance();
        }
    }

    private static int skipWhiteSpace(BitArray row) throws NotFoundException {
        int width = row.getSize();
        int endStart = row.getNextSet(0);
        if (endStart != width) {
            return endStart;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    int[] decodeEnd(BitArray row) throws NotFoundException {
        row.reverse();
        try {
            int[] endPattern = findGuardPattern(row, skipWhiteSpace(row), END_PATTERN_REVERSED);
            validateQuietZone(row, endPattern[0]);
            int temp = endPattern[0];
            endPattern[0] = row.getSize() - endPattern[1];
            endPattern[1] = row.getSize() - temp;
            return endPattern;
        } finally {
            row.reverse();
        }
    }

    private static int[] findGuardPattern(BitArray row, int rowOffset, int[] pattern) throws NotFoundException {
        int patternLength = pattern.length;
        int[] counters = new int[patternLength];
        int width = row.getSize();
        int patternStart = rowOffset;
        int counterPosition = 0;
        boolean isWhite = false;
        for (int x = rowOffset; x < width; x++) {
            boolean z = true;
            if ((row.get(x) ^ isWhite) != 0) {
                counters[counterPosition] = counters[counterPosition] + 1;
            } else {
                if (counterPosition != patternLength - 1) {
                    counterPosition++;
                } else if (OneDReader.patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < 107) {
                    return new int[]{patternStart, x};
                } else {
                    patternStart += counters[0] + counters[1];
                    System.arraycopy(counters, 2, counters, 0, patternLength - 2);
                    counters[patternLength - 2] = 0;
                    counters[patternLength - 1] = 0;
                    counterPosition--;
                }
                counters[counterPosition] = 1;
                if (isWhite) {
                    z = false;
                }
                isWhite = z;
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static int decodeDigit(int[] counters) throws NotFoundException {
        int bestVariance = 107;
        int bestMatch = -1;
        int max = PATTERNS.length;
        for (int i = 0; i < max; i++) {
            int variance = OneDReader.patternMatchVariance(counters, PATTERNS[i], MAX_INDIVIDUAL_VARIANCE);
            if (variance < bestVariance) {
                bestVariance = variance;
                bestMatch = i;
            }
        }
        if (bestMatch >= 0) {
            return bestMatch;
        }
        throw NotFoundException.getNotFoundInstance();
    }
}
