package com.huawei.zxing.pdf417.decoder;

import com.huawei.zxing.pdf417.PDF417Common;
import java.lang.reflect.Array;

final class PDF417CodewordDecoder {
    private static final float[][] RATIOS_TABLE = ((float[][]) Array.newInstance(float.class, new int[]{PDF417Common.SYMBOL_TABLE.length, 8}));

    static {
        for (int i = 0; i < PDF417Common.SYMBOL_TABLE.length; i++) {
            int currentSymbol = PDF417Common.SYMBOL_TABLE[i];
            int currentBit = currentSymbol & 1;
            int currentSymbol2 = currentSymbol;
            for (currentSymbol = 0; currentSymbol < 8; currentSymbol++) {
                float size = 0.0f;
                while ((currentSymbol2 & 1) == currentBit) {
                    size += 1.0f;
                    currentSymbol2 >>= 1;
                }
                currentBit = currentSymbol2 & 1;
                RATIOS_TABLE[i][(8 - currentSymbol) - 1] = size / 17.0f;
            }
        }
    }

    private PDF417CodewordDecoder() {
    }

    static int getDecodedValue(int[] moduleBitCount) {
        int decodedValue = getDecodedCodewordValue(sampleBitCounts(moduleBitCount));
        if (decodedValue != -1) {
            return decodedValue;
        }
        return getClosestDecodedValue(moduleBitCount);
    }

    private static int[] sampleBitCounts(int[] moduleBitCount) {
        float bitCountSum = (float) PDF417Common.getBitCountSum(moduleBitCount);
        int[] result = new int[8];
        int bitCountIndex = 0;
        int sumPreviousBits = 0;
        for (int i = 0; i < 17; i++) {
            if (((float) (moduleBitCount[bitCountIndex] + sumPreviousBits)) <= (bitCountSum / 34.0f) + ((((float) i) * bitCountSum) / 17.0f)) {
                sumPreviousBits += moduleBitCount[bitCountIndex];
                bitCountIndex++;
            }
            result[bitCountIndex] = result[bitCountIndex] + 1;
        }
        return result;
    }

    private static int getDecodedCodewordValue(int[] moduleBitCount) {
        int decodedValue = getBitValue(moduleBitCount);
        return PDF417Common.getCodeword((long) decodedValue) == -1 ? -1 : decodedValue;
    }

    private static int getBitValue(int[] moduleBitCount) {
        long result = 0;
        for (int i = 0; i < moduleBitCount.length; i++) {
            for (int bit = 0; bit < moduleBitCount[i]; bit++) {
                int i2 = 1;
                long j = result << 1;
                if (i % 2 != 0) {
                    i2 = 0;
                }
                result = j | ((long) i2);
            }
        }
        return (int) result;
    }

    private static int getClosestDecodedValue(int[] moduleBitCount) {
        int i;
        int bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);
        float[] bitCountRatios = new float[8];
        for (i = 0; i < bitCountRatios.length; i++) {
            bitCountRatios[i] = ((float) moduleBitCount[i]) / ((float) bitCountSum);
        }
        int bestMatch = -1;
        float bestMatchError = Float.MAX_VALUE;
        for (i = 0; i < RATIOS_TABLE.length; i++) {
            float[] ratioTableRow = RATIOS_TABLE[i];
            float error = 0.0f;
            for (int k = 0; k < 8; k++) {
                float diff = ratioTableRow[k] - bitCountRatios[k];
                error += diff * diff;
                if (error >= bestMatchError) {
                    break;
                }
            }
            if (error < bestMatchError) {
                bestMatchError = error;
                bestMatch = PDF417Common.SYMBOL_TABLE[i];
            }
        }
        return bestMatch;
    }
}
