package com.huawei.zxing.oned;

import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.common.BitArray;

public final class EAN13Reader extends UPCEANReader {
    static final int[] FIRST_DIGIT_ENCODINGS = new int[]{0, 11, 13, 14, 19, 25, 28, 21, 22, 26};
    private final int[] decodeMiddleCounters = new int[4];

    protected int decodeMiddle(BitArray row, int[] startRange, StringBuilder resultString) throws NotFoundException {
        BitArray bitArray = row;
        StringBuilder stringBuilder = resultString;
        int[] counters = this.decodeMiddleCounters;
        counters[0] = 0;
        counters[1] = 0;
        counters[2] = 0;
        counters[3] = 0;
        int end = row.getSize();
        int lgPatternFound = 0;
        int rowOffset = startRange[1];
        int x = 0;
        while (x < 6 && rowOffset < end) {
            int bestMatch = UPCEANReader.decodeDigit(bitArray, counters, rowOffset, L_AND_G_PATTERNS);
            stringBuilder.append((char) (48 + (bestMatch % 10)));
            int rowOffset2 = rowOffset;
            for (int counter : counters) {
                rowOffset2 += counter;
            }
            if (bestMatch >= 10) {
                lgPatternFound = (1 << (5 - x)) | lgPatternFound;
            }
            x++;
            rowOffset = rowOffset2;
        }
        determineFirstDigit(stringBuilder, lgPatternFound);
        rowOffset = UPCEANReader.findGuardPattern(bitArray, rowOffset, true, MIDDLE_PATTERN)[1];
        int x2 = 0;
        while (x2 < 6 && rowOffset < end) {
            stringBuilder.append((char) (48 + UPCEANReader.decodeDigit(bitArray, counters, rowOffset, L_PATTERNS)));
            int rowOffset3 = rowOffset;
            for (int counter2 : counters) {
                rowOffset3 += counter2;
            }
            x2++;
            rowOffset = rowOffset3;
        }
        return rowOffset;
    }

    BarcodeFormat getBarcodeFormat() {
        return BarcodeFormat.EAN_13;
    }

    private static void determineFirstDigit(StringBuilder resultString, int lgPatternFound) throws NotFoundException {
        for (int d = 0; d < 10; d++) {
            if (lgPatternFound == FIRST_DIGIT_ENCODINGS[d]) {
                resultString.insert(0, (char) (48 + d));
                return;
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }
}
