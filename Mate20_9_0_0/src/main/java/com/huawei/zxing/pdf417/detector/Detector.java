package com.huawei.zxing.pdf417.detector;

import com.huawei.zxing.BinaryBitmap;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitArray;
import com.huawei.zxing.common.BitMatrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Detector {
    private static final int BARCODE_MIN_HEIGHT = 10;
    private static final int[] INDEXES_START_PATTERN = new int[]{0, 4, 1, 5};
    private static final int[] INDEXES_STOP_PATTERN = new int[]{6, 2, 7, 3};
    private static final int INTEGER_MATH_SHIFT = 8;
    private static final int MAX_AVG_VARIANCE = 107;
    private static final int MAX_INDIVIDUAL_VARIANCE = 204;
    private static final int MAX_PATTERN_DRIFT = 5;
    private static final int MAX_PIXEL_DRIFT = 3;
    private static final int PATTERN_MATCH_RESULT_SCALE_FACTOR = 256;
    private static final int ROW_STEP = 5;
    private static final int SKIPPED_ROW_COUNT_MAX = 25;
    private static final int[] START_PATTERN = new int[]{8, 1, 1, 1, 1, 1, 1, 3};
    private static final int[] STOP_PATTERN = new int[]{7, 1, 1, 3, 1, 1, 1, 2, 1};

    private Detector() {
    }

    public static PDF417DetectorResult detect(BinaryBitmap image, Map<DecodeHintType, ?> map, boolean multiple) throws NotFoundException {
        BitMatrix bitMatrix = image.getBlackMatrix();
        List<ResultPoint[]> barcodeCoordinates = detect(multiple, bitMatrix);
        if (barcodeCoordinates.isEmpty()) {
            rotate180(bitMatrix);
            barcodeCoordinates = detect(multiple, bitMatrix);
        }
        return new PDF417DetectorResult(bitMatrix, barcodeCoordinates);
    }

    private static List<ResultPoint[]> detect(boolean multiple, BitMatrix bitMatrix) {
        List<ResultPoint[]> barcodeCoordinates = new ArrayList();
        int column = 0;
        int row = 0;
        boolean foundBarcodeInRow = false;
        while (row < bitMatrix.getHeight()) {
            ResultPoint[] vertices = findVertices(bitMatrix, row, column);
            if (vertices[0] != null || vertices[3] != null) {
                foundBarcodeInRow = true;
                barcodeCoordinates.add(vertices);
                if (!multiple) {
                    break;
                }
                float y;
                if (vertices[2] != null) {
                    column = (int) vertices[2].getX();
                    y = vertices[2].getY();
                } else {
                    column = (int) vertices[4].getX();
                    y = vertices[4].getY();
                }
                row = (int) y;
            } else if (!foundBarcodeInRow) {
                break;
            } else {
                foundBarcodeInRow = false;
                column = 0;
                for (ResultPoint[] barcodeCoordinate : barcodeCoordinates) {
                    if (barcodeCoordinate[1] != null) {
                        row = (int) Math.max((float) row, barcodeCoordinate[1].getY());
                    }
                    if (barcodeCoordinate[3] != null) {
                        row = Math.max(row, (int) barcodeCoordinate[3].getY());
                    }
                }
                row += 5;
            }
        }
        return barcodeCoordinates;
    }

    static void rotate180(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BitArray firstRowBitArray = new BitArray(width);
        BitArray secondRowBitArray = new BitArray(width);
        BitArray tmpBitArray = new BitArray(width);
        for (int y = 0; y < ((height + 1) >> 1); y++) {
            firstRowBitArray = bitMatrix.getRow(y, firstRowBitArray);
            bitMatrix.setRow(y, mirror(bitMatrix.getRow((height - 1) - y, secondRowBitArray), tmpBitArray));
            bitMatrix.setRow((height - 1) - y, mirror(firstRowBitArray, tmpBitArray));
        }
    }

    static BitArray mirror(BitArray input, BitArray result) {
        result.clear();
        int size = input.getSize();
        for (int i = 0; i < size; i++) {
            if (input.get(i)) {
                result.set((size - 1) - i);
            }
        }
        return result;
    }

    private static ResultPoint[] findVertices(BitMatrix matrix, int startRow, int startColumn) {
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        ResultPoint[] result = new ResultPoint[8];
        copyToResult(result, findRowsWithPattern(matrix, height, width, startRow, startColumn, START_PATTERN), INDEXES_START_PATTERN);
        if (result[4] != null) {
            startColumn = (int) result[4].getX();
            startRow = (int) result[4].getY();
        }
        copyToResult(result, findRowsWithPattern(matrix, height, width, startRow, startColumn, STOP_PATTERN), INDEXES_STOP_PATTERN);
        return result;
    }

    private static void copyToResult(ResultPoint[] result, ResultPoint[] tmpResult, int[] destinationIndexes) {
        for (int i = 0; i < destinationIndexes.length; i++) {
            result[destinationIndexes[i]] = tmpResult[i];
        }
    }

    private static ResultPoint[] findRowsWithPattern(BitMatrix matrix, int height, int width, int startRow, int startColumn, int[] pattern) {
        int i;
        int startRow2;
        int skippedRowCount;
        int i2 = height;
        ResultPoint[] result = new ResultPoint[4];
        boolean startRow3 = false;
        int[] iArr = pattern;
        int startRow4 = startRow;
        int[] counters = new int[iArr.length];
        while (true) {
            i = 0;
            if (startRow4 >= i2) {
                break;
            }
            int[] loc = findGuardPattern(matrix, startColumn, startRow4, width, false, iArr, counters);
            if (loc != null) {
                while (startRow4 > 0) {
                    startRow2 = startRow4 - 1;
                    int[] previousRowLoc = findGuardPattern(matrix, startColumn, startRow2, width, false, pattern, counters);
                    if (previousRowLoc == null) {
                        startRow4 = startRow2 + 1;
                        break;
                    }
                    loc = previousRowLoc;
                    iArr = pattern;
                    startRow4 = startRow2;
                }
                result[0] = new ResultPoint((float) loc[0], (float) startRow4);
                result[1] = new ResultPoint((float) loc[1], (float) startRow4);
                startRow3 = true;
            } else {
                startRow4 += 5;
                iArr = pattern;
            }
        }
        boolean found = startRow3;
        int startRow5 = startRow4;
        startRow2 = startRow5 + 1;
        if (found) {
            skippedRowCount = 0;
            int[] previousRowLoc2 = new int[]{(int) result[0].getX(), (int) result[1].getX()};
            while (startRow2 < i2) {
                int[] loc2 = findGuardPattern(matrix, previousRowLoc2[0], startRow2, width, false, pattern, counters);
                if (loc2 != null && Math.abs(previousRowLoc2[0] - loc2[0]) < 5 && Math.abs(previousRowLoc2[1] - loc2[1]) < 5) {
                    previousRowLoc2 = loc2;
                    skippedRowCount = 0;
                } else if (skippedRowCount > 25) {
                    break;
                } else {
                    skippedRowCount++;
                }
                startRow2++;
            }
            startRow2 -= skippedRowCount + 1;
            result[2] = new ResultPoint((float) previousRowLoc2[0], (float) startRow2);
            result[3] = new ResultPoint((float) previousRowLoc2[1], (float) startRow2);
        }
        if (startRow2 - startRow5 < 10) {
            while (true) {
                skippedRowCount = i;
                if (skippedRowCount >= result.length) {
                    break;
                }
                result[skippedRowCount] = null;
                i = skippedRowCount + 1;
            }
        }
        return result;
    }

    private static int[] findGuardPattern(BitMatrix matrix, int column, int row, int width, boolean whiteFirst, int[] pattern, int[] counters) {
        int pixelDrift;
        BitMatrix bitMatrix = matrix;
        int i = row;
        int[] iArr = pattern;
        int[] iArr2 = counters;
        Arrays.fill(iArr2, 0, iArr2.length, 0);
        int patternLength = iArr.length;
        boolean isWhite = whiteFirst;
        int patternStart = column;
        int pixelDrift2 = 0;
        while (bitMatrix.get(patternStart, i) && patternStart > 0) {
            pixelDrift = pixelDrift2 + 1;
            if (pixelDrift2 >= 3) {
                pixelDrift2 = pixelDrift;
                break;
            }
            patternStart--;
            pixelDrift2 = pixelDrift;
        }
        int patternStart2 = patternStart;
        boolean isWhite2 = isWhite;
        int counterPosition = 0;
        for (pixelDrift = patternStart; pixelDrift < width; pixelDrift++) {
            if ((bitMatrix.get(pixelDrift, i) ^ isWhite2) != 0) {
                iArr2[counterPosition] = iArr2[counterPosition] + 1;
            } else {
                if (counterPosition != patternLength - 1) {
                    counterPosition++;
                } else if (patternMatchVariance(iArr2, iArr, MAX_INDIVIDUAL_VARIANCE) < 107) {
                    return new int[]{patternStart2, pixelDrift};
                } else {
                    patternStart2 += iArr2[0] + iArr2[1];
                    System.arraycopy(iArr2, 2, iArr2, 0, patternLength - 2);
                    iArr2[patternLength - 2] = 0;
                    iArr2[patternLength - 1] = 0;
                    counterPosition--;
                }
                iArr2[counterPosition] = 1;
                isWhite2 = !isWhite2;
            }
        }
        if (counterPosition != patternLength - 1 || patternMatchVariance(iArr2, iArr, MAX_INDIVIDUAL_VARIANCE) >= 107) {
            return null;
        }
        return new int[]{patternStart2, pixelDrift - 1};
    }

    private static int patternMatchVariance(int[] counters, int[] pattern, int maxIndividualVariance) {
        int numCounters = counters.length;
        int x = 0;
        int patternLength = 0;
        int total = 0;
        for (int i = 0; i < numCounters; i++) {
            total += counters[i];
            patternLength += pattern[i];
        }
        if (total < patternLength) {
            return Integer.MAX_VALUE;
        }
        int unitBarWidth = (total << 8) / patternLength;
        maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> 8;
        int totalVariance = 0;
        while (x < numCounters) {
            int counter = counters[x] << 8;
            int scaledPattern = pattern[x] * unitBarWidth;
            int variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
            if (variance > maxIndividualVariance) {
                return Integer.MAX_VALUE;
            }
            totalVariance += variance;
            x++;
        }
        return totalVariance / total;
    }
}
