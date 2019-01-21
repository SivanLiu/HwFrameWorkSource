package com.huawei.zxing.pdf417.decoder;

import com.huawei.zxing.ChecksumException;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitMatrix;
import com.huawei.zxing.common.DecoderResult;
import com.huawei.zxing.pdf417.PDF417Common;
import com.huawei.zxing.pdf417.decoder.ec.ErrorCorrection;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;

public final class PDF417ScanningDecoder {
    private static final int CODEWORD_SKEW_SIZE = 2;
    private static final int MAX_EC_CODEWORDS = 512;
    private static final int MAX_ERRORS = 3;
    private static final ErrorCorrection errorCorrection = new ErrorCorrection();

    private PDF417ScanningDecoder() {
    }

    public static DecoderResult decode(BitMatrix image, ResultPoint imageTopLeft, ResultPoint imageBottomLeft, ResultPoint imageTopRight, ResultPoint imageBottomRight, int minCodewordWidth, int maxCodewordWidth) throws NotFoundException, FormatException, ChecksumException {
        boolean z = false;
        DetectionResult detectionResult = null;
        DetectionResultRowIndicatorColumn rightRowIndicatorColumn = null;
        DetectionResultRowIndicatorColumn leftRowIndicatorColumn = null;
        BoundingBox boundingBox = new BoundingBox(image, imageTopLeft, imageBottomLeft, imageTopRight, imageBottomRight);
        int i = 0;
        while (i < 2) {
            if (imageTopLeft != null) {
                leftRowIndicatorColumn = getRowIndicatorColumn(image, boundingBox, imageTopLeft, true, minCodewordWidth, maxCodewordWidth);
            }
            if (imageTopRight != null) {
                rightRowIndicatorColumn = getRowIndicatorColumn(image, boundingBox, imageTopRight, false, minCodewordWidth, maxCodewordWidth);
            }
            detectionResult = merge(leftRowIndicatorColumn, rightRowIndicatorColumn);
            if (detectionResult == null) {
                throw NotFoundException.getNotFoundInstance();
            } else if (i != 0 || detectionResult.getBoundingBox() == null || (detectionResult.getBoundingBox().getMinY() >= boundingBox.getMinY() && detectionResult.getBoundingBox().getMaxY() <= boundingBox.getMaxY())) {
                detectionResult.setBoundingBox(boundingBox);
                break;
            } else {
                boundingBox = detectionResult.getBoundingBox();
                i++;
            }
        }
        boolean z2 = true;
        i = detectionResult.getBarcodeColumnCount() + 1;
        detectionResult.setDetectionResultColumn(0, leftRowIndicatorColumn);
        detectionResult.setDetectionResultColumn(i, rightRowIndicatorColumn);
        boolean leftToRight = leftRowIndicatorColumn != null;
        int minCodewordWidth2 = minCodewordWidth;
        int maxCodewordWidth2 = maxCodewordWidth;
        int barcodeColumnCount = 1;
        while (barcodeColumnCount <= i) {
            int maxBarcodeColumn;
            int barcodeColumn = leftToRight ? barcodeColumnCount : i - barcodeColumnCount;
            if (detectionResult.getDetectionResultColumn(barcodeColumn) != null) {
                maxBarcodeColumn = i;
            } else {
                DetectionResultColumn detectionResultColumn;
                if (barcodeColumn == 0 || barcodeColumn == i) {
                    detectionResultColumn = new DetectionResultRowIndicatorColumn(boundingBox, barcodeColumn == 0 ? z2 : z);
                } else {
                    detectionResultColumn = new DetectionResultColumn(boundingBox);
                }
                DetectionResultColumn detectionResultColumn2 = detectionResultColumn;
                detectionResult.setDetectionResultColumn(barcodeColumn, detectionResultColumn2);
                int previousStartColumn = -1;
                int imageRow = boundingBox.getMinY();
                int maxCodewordWidth3 = maxCodewordWidth2;
                maxCodewordWidth2 = minCodewordWidth2;
                minCodewordWidth2 = previousStartColumn;
                while (true) {
                    int imageRow2 = imageRow;
                    if (imageRow2 > boundingBox.getMaxY()) {
                        break;
                    }
                    DetectionResultColumn detectionResultColumn3;
                    int startColumn = getStartColumn(detectionResult, barcodeColumn, imageRow2, leftToRight);
                    if (startColumn < 0 || startColumn > boundingBox.getMaxX()) {
                        if (minCodewordWidth2 == -1) {
                            maxBarcodeColumn = i;
                            int i2 = startColumn;
                            detectionResultColumn3 = detectionResultColumn2;
                            imageRow = imageRow2 + 1;
                            i = maxBarcodeColumn;
                            detectionResultColumn2 = detectionResultColumn3;
                        } else {
                            startColumn = minCodewordWidth2;
                        }
                    }
                    maxBarcodeColumn = i;
                    i = detectionResultColumn2;
                    Codeword codeword = detectCodeword(image, boundingBox.getMinX(), boundingBox.getMaxX(), leftToRight, startColumn, imageRow2, maxCodewordWidth2, maxCodewordWidth3);
                    if (codeword != null) {
                        i.setCodeword(imageRow2, codeword);
                        minCodewordWidth2 = startColumn;
                        maxCodewordWidth2 = Math.min(maxCodewordWidth2, codeword.getWidth());
                        detectionResultColumn3 = i;
                        maxCodewordWidth3 = Math.max(maxCodewordWidth3, codeword.getWidth());
                        codeword = startColumn;
                    } else {
                        detectionResultColumn3 = i;
                        i = maxCodewordWidth3;
                    }
                    imageRow = imageRow2 + 1;
                    i = maxBarcodeColumn;
                    detectionResultColumn2 = detectionResultColumn3;
                }
                maxBarcodeColumn = i;
                minCodewordWidth2 = maxCodewordWidth2;
                maxCodewordWidth2 = maxCodewordWidth3;
            }
            barcodeColumnCount++;
            i = maxBarcodeColumn;
            z = false;
            z2 = true;
        }
        return createDecoderResult(detectionResult);
    }

    private static DetectionResult merge(DetectionResultRowIndicatorColumn leftRowIndicatorColumn, DetectionResultRowIndicatorColumn rightRowIndicatorColumn) throws NotFoundException {
        if (leftRowIndicatorColumn == null && rightRowIndicatorColumn == null) {
            return null;
        }
        BarcodeMetadata barcodeMetadata = getBarcodeMetadata(leftRowIndicatorColumn, rightRowIndicatorColumn);
        if (barcodeMetadata == null) {
            return null;
        }
        return new DetectionResult(barcodeMetadata, BoundingBox.merge(adjustBoundingBox(leftRowIndicatorColumn), adjustBoundingBox(rightRowIndicatorColumn)));
    }

    private static BoundingBox adjustBoundingBox(DetectionResultRowIndicatorColumn rowIndicatorColumn) throws NotFoundException {
        if (rowIndicatorColumn == null) {
            return null;
        }
        int[] rowHeights = rowIndicatorColumn.getRowHeights();
        if (rowHeights == null) {
            return null;
        }
        int i;
        int maxRowHeight = getMax(rowHeights);
        int row = 0;
        int missingStartRows = 0;
        for (int rowHeight : rowHeights) {
            missingStartRows += maxRowHeight - rowHeight;
            if (rowHeight > 0) {
                break;
            }
        }
        Codeword[] codewords = rowIndicatorColumn.getCodewords();
        while (true) {
            i = row;
            if (missingStartRows <= 0 || codewords[i] != null) {
                i = 0;
            } else {
                missingStartRows--;
                row = i + 1;
            }
        }
        i = 0;
        for (row = rowHeights.length - 1; row >= 0; row--) {
            i += maxRowHeight - rowHeights[row];
            if (rowHeights[row] > 0) {
                break;
            }
        }
        row = codewords.length - 1;
        while (i > 0 && codewords[row] == null) {
            i--;
            row--;
        }
        return rowIndicatorColumn.getBoundingBox().addMissingRows(missingStartRows, i, rowIndicatorColumn.isLeft());
    }

    private static int getMax(int[] values) {
        int maxValue = -1;
        for (int value : values) {
            maxValue = Math.max(maxValue, value);
        }
        return maxValue;
    }

    private static BarcodeMetadata getBarcodeMetadata(DetectionResultRowIndicatorColumn leftRowIndicatorColumn, DetectionResultRowIndicatorColumn rightRowIndicatorColumn) {
        BarcodeMetadata barcodeMetadata = null;
        if (leftRowIndicatorColumn == null || leftRowIndicatorColumn.getBarcodeMetadata() == null) {
            if (rightRowIndicatorColumn != null) {
                barcodeMetadata = rightRowIndicatorColumn.getBarcodeMetadata();
            }
            return barcodeMetadata;
        } else if (rightRowIndicatorColumn == null || rightRowIndicatorColumn.getBarcodeMetadata() == null) {
            if (leftRowIndicatorColumn != null) {
                barcodeMetadata = leftRowIndicatorColumn.getBarcodeMetadata();
            }
            return barcodeMetadata;
        } else {
            BarcodeMetadata leftBarcodeMetadata = leftRowIndicatorColumn.getBarcodeMetadata();
            BarcodeMetadata rightBarcodeMetadata = rightRowIndicatorColumn.getBarcodeMetadata();
            if (leftBarcodeMetadata.getColumnCount() == rightBarcodeMetadata.getColumnCount() || leftBarcodeMetadata.getErrorCorrectionLevel() == rightBarcodeMetadata.getErrorCorrectionLevel() || leftBarcodeMetadata.getRowCount() == rightBarcodeMetadata.getRowCount()) {
                return leftBarcodeMetadata;
            }
            return null;
        }
    }

    private static DetectionResultRowIndicatorColumn getRowIndicatorColumn(BitMatrix image, BoundingBox boundingBox, ResultPoint startPoint, boolean leftToRight, int minCodewordWidth, int maxCodewordWidth) {
        boolean z = leftToRight;
        DetectionResultRowIndicatorColumn rowIndicatorColumn = new DetectionResultRowIndicatorColumn(boundingBox, z);
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= 2) {
                return rowIndicatorColumn;
            }
            int increment = i2 == 0 ? 1 : -1;
            int imageRow = (int) startPoint.getY();
            int startColumn = (int) startPoint.getX();
            while (true) {
                int imageRow2 = imageRow;
                if (imageRow2 > boundingBox.getMaxY() || imageRow2 < boundingBox.getMinY()) {
                    i = i2 + 1;
                } else {
                    Codeword codeword = detectCodeword(image, 0, image.getWidth(), z, startColumn, imageRow2, minCodewordWidth, maxCodewordWidth);
                    if (codeword != null) {
                        rowIndicatorColumn.setCodeword(imageRow2, codeword);
                        if (z) {
                            startColumn = codeword.getStartX();
                        } else {
                            startColumn = codeword.getEndX();
                        }
                    }
                    imageRow = imageRow2 + increment;
                }
            }
            i = i2 + 1;
        }
    }

    private static void adjustCodewordCount(DetectionResult detectionResult, BarcodeValue[][] barcodeMatrix) throws NotFoundException {
        int[] numberOfCodewords = barcodeMatrix[0][1].getValue();
        int calculatedNumberOfCodewords = (detectionResult.getBarcodeColumnCount() * detectionResult.getBarcodeRowCount()) - getNumberOfECCodeWords(detectionResult.getBarcodeECLevel());
        if (numberOfCodewords.length == 0) {
            if (calculatedNumberOfCodewords < 1 || calculatedNumberOfCodewords > PDF417Common.MAX_CODEWORDS_IN_BARCODE) {
                throw NotFoundException.getNotFoundInstance();
            }
            barcodeMatrix[0][1].setValue(calculatedNumberOfCodewords);
        } else if (numberOfCodewords[0] != calculatedNumberOfCodewords) {
            barcodeMatrix[0][1].setValue(calculatedNumberOfCodewords);
        }
    }

    private static DecoderResult createDecoderResult(DetectionResult detectionResult) throws FormatException, ChecksumException, NotFoundException {
        BarcodeValue[][] barcodeMatrix = createBarcodeMatrix(detectionResult);
        adjustCodewordCount(detectionResult, barcodeMatrix);
        Collection<Integer> erasures = new ArrayList();
        int[] codewords = new int[(detectionResult.getBarcodeRowCount() * detectionResult.getBarcodeColumnCount())];
        List<int[]> ambiguousIndexValuesList = new ArrayList();
        List<Integer> ambiguousIndexesList = new ArrayList();
        int i = 0;
        for (int row = 0; row < detectionResult.getBarcodeRowCount(); row++) {
            for (int column = 0; column < detectionResult.getBarcodeColumnCount(); column++) {
                int[] values = barcodeMatrix[row][column + 1].getValue();
                int codewordIndex = (detectionResult.getBarcodeColumnCount() * row) + column;
                if (values.length == 0) {
                    erasures.add(Integer.valueOf(codewordIndex));
                } else if (values.length == 1) {
                    codewords[codewordIndex] = values[0];
                } else {
                    ambiguousIndexesList.add(Integer.valueOf(codewordIndex));
                    ambiguousIndexValuesList.add(values);
                }
            }
        }
        int[][] ambiguousIndexValues = new int[ambiguousIndexValuesList.size()][];
        while (i < ambiguousIndexValues.length) {
            ambiguousIndexValues[i] = (int[]) ambiguousIndexValuesList.get(i);
            i++;
        }
        return createDecoderResultFromAmbiguousValues(detectionResult.getBarcodeECLevel(), codewords, PDF417Common.toIntArray(erasures), PDF417Common.toIntArray(ambiguousIndexesList), ambiguousIndexValues);
    }

    private static DecoderResult createDecoderResultFromAmbiguousValues(int ecLevel, int[] codewords, int[] erasureArray, int[] ambiguousIndexes, int[][] ambiguousIndexValues) throws FormatException, ChecksumException {
        int[] ambiguousIndexCount = new int[ambiguousIndexes.length];
        int tries = 100;
        while (true) {
            int tries2 = tries - 1;
            if (tries > 0) {
                int i;
                for (i = 0; i < ambiguousIndexCount.length; i++) {
                    codewords[ambiguousIndexes[i]] = ambiguousIndexValues[i][ambiguousIndexCount[i]];
                }
                try {
                    return decodeCodewords(codewords, ecLevel, erasureArray);
                } catch (ChecksumException e) {
                    if (ambiguousIndexCount.length != 0) {
                        i = 0;
                        while (i < ambiguousIndexCount.length) {
                            if (ambiguousIndexCount[i] < ambiguousIndexValues[i].length - 1) {
                                ambiguousIndexCount[i] = ambiguousIndexCount[i] + 1;
                                break;
                            }
                            ambiguousIndexCount[i] = 0;
                            if (i != ambiguousIndexCount.length - 1) {
                                i++;
                            } else {
                                throw ChecksumException.getChecksumInstance();
                            }
                        }
                        tries = tries2;
                    } else {
                        throw ChecksumException.getChecksumInstance();
                    }
                }
            }
            throw ChecksumException.getChecksumInstance();
        }
    }

    private static BarcodeValue[][] createBarcodeMatrix(DetectionResult detectionResult) {
        int row;
        BarcodeValue[][] barcodeMatrix = (BarcodeValue[][]) Array.newInstance(BarcodeValue.class, new int[]{detectionResult.getBarcodeRowCount(), detectionResult.getBarcodeColumnCount() + 2});
        for (row = 0; row < barcodeMatrix.length; row++) {
            for (int column = 0; column < barcodeMatrix[row].length; column++) {
                barcodeMatrix[row][column] = new BarcodeValue();
            }
        }
        int column2 = -1;
        for (DetectionResultColumn detectionResultColumn : detectionResult.getDetectionResultColumns()) {
            column2++;
            if (detectionResultColumn != null) {
                for (Codeword codeword : detectionResultColumn.getCodewords()) {
                    if (!(codeword == null || codeword.getRowNumber() == -1)) {
                        barcodeMatrix[codeword.getRowNumber()][column2].setValue(codeword.getValue());
                    }
                }
            }
        }
        return barcodeMatrix;
    }

    private static boolean isValidBarcodeColumn(DetectionResult detectionResult, int barcodeColumn) {
        return barcodeColumn >= 0 && barcodeColumn <= detectionResult.getBarcodeColumnCount() + 1;
    }

    private static int getStartColumn(DetectionResult detectionResult, int barcodeColumn, int imageRow, boolean leftToRight) {
        int offset = leftToRight ? 1 : -1;
        Codeword codeword = null;
        if (isValidBarcodeColumn(detectionResult, barcodeColumn - offset)) {
            codeword = detectionResult.getDetectionResultColumn(barcodeColumn - offset).getCodeword(imageRow);
        }
        if (codeword != null) {
            return leftToRight ? codeword.getEndX() : codeword.getStartX();
        }
        codeword = detectionResult.getDetectionResultColumn(barcodeColumn).getCodewordNearby(imageRow);
        if (codeword != null) {
            return leftToRight ? codeword.getStartX() : codeword.getEndX();
        }
        if (isValidBarcodeColumn(detectionResult, barcodeColumn - offset)) {
            codeword = detectionResult.getDetectionResultColumn(barcodeColumn - offset).getCodewordNearby(imageRow);
        }
        if (codeword != null) {
            return leftToRight ? codeword.getEndX() : codeword.getStartX();
        }
        int barcodeColumn2 = barcodeColumn;
        barcodeColumn = 0;
        while (isValidBarcodeColumn(detectionResult, barcodeColumn2 - offset)) {
            barcodeColumn2 -= offset;
            for (Codeword previousRowCodeword : detectionResult.getDetectionResultColumn(barcodeColumn2).getCodewords()) {
                if (previousRowCodeword != null) {
                    return (leftToRight ? previousRowCodeword.getEndX() : previousRowCodeword.getStartX()) + ((offset * barcodeColumn) * (previousRowCodeword.getEndX() - previousRowCodeword.getStartX()));
                }
            }
            barcodeColumn++;
        }
        return leftToRight ? detectionResult.getBoundingBox().getMinX() : detectionResult.getBoundingBox().getMaxX();
    }

    private static Codeword detectCodeword(BitMatrix image, int minColumn, int maxColumn, boolean leftToRight, int startColumn, int imageRow, int minCodewordWidth, int maxCodewordWidth) {
        startColumn = adjustCodewordStartColumn(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
        int[] moduleBitCount = getModuleBitCount(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
        if (moduleBitCount == null) {
            return null;
        }
        int endColumn;
        int tmpCount;
        int codewordBitCount = PDF417Common.getBitCountSum(moduleBitCount);
        if (leftToRight) {
            endColumn = startColumn + codewordBitCount;
        } else {
            for (endColumn = 0; endColumn < (moduleBitCount.length >> 1); endColumn++) {
                tmpCount = moduleBitCount[endColumn];
                moduleBitCount[endColumn] = moduleBitCount[(moduleBitCount.length - 1) - endColumn];
                moduleBitCount[(moduleBitCount.length - 1) - endColumn] = tmpCount;
            }
            endColumn = startColumn;
            startColumn = endColumn - codewordBitCount;
        }
        if (!checkCodewordSkew(codewordBitCount, minCodewordWidth, maxCodewordWidth)) {
            return null;
        }
        tmpCount = PDF417CodewordDecoder.getDecodedValue(moduleBitCount);
        int codeword = PDF417Common.getCodeword((long) tmpCount);
        if (codeword == -1) {
            return null;
        }
        return new Codeword(startColumn, endColumn, getCodewordBucketNumber(tmpCount), codeword);
    }

    private static int[] getModuleBitCount(BitMatrix image, int minColumn, int maxColumn, boolean leftToRight, int startColumn, int imageRow) {
        int imageColumn = startColumn;
        int[] moduleBitCount = new int[8];
        int increment = leftToRight ? 1 : -1;
        int moduleNumber = 0;
        int imageColumn2 = imageColumn;
        boolean previousPixelValue = leftToRight;
        while (true) {
            if (((leftToRight && imageColumn2 < maxColumn) || (!leftToRight && imageColumn2 >= minColumn)) && moduleNumber < moduleBitCount.length) {
                if (image.get(imageColumn2, imageRow) == previousPixelValue) {
                    moduleBitCount[moduleNumber] = moduleBitCount[moduleNumber] + 1;
                    imageColumn2 += increment;
                } else {
                    moduleNumber++;
                    previousPixelValue = !previousPixelValue;
                }
            }
        }
        if (moduleNumber == moduleBitCount.length || (((leftToRight && imageColumn2 == maxColumn) || (!leftToRight && imageColumn2 == minColumn)) && moduleNumber == moduleBitCount.length - 1)) {
            return moduleBitCount;
        }
        return null;
    }

    private static int getNumberOfECCodeWords(int barcodeECLevel) {
        return 2 << barcodeECLevel;
    }

    private static int adjustCodewordStartColumn(BitMatrix image, int minColumn, int maxColumn, boolean leftToRight, int codewordStartColumn, int imageRow) {
        int correctedStartColumn = codewordStartColumn;
        int increment = leftToRight ? -1 : 1;
        boolean leftToRight2 = leftToRight;
        for (int i = 0; i < 2; i++) {
            while (true) {
                if (((!leftToRight2 || correctedStartColumn < minColumn) && (leftToRight2 || correctedStartColumn >= maxColumn)) || leftToRight2 != image.get(correctedStartColumn, imageRow)) {
                    increment = -increment;
                } else if (Math.abs(codewordStartColumn - correctedStartColumn) > 2) {
                    return codewordStartColumn;
                } else {
                    correctedStartColumn += increment;
                }
            }
            increment = -increment;
            leftToRight2 = !leftToRight2;
        }
        return correctedStartColumn;
    }

    private static boolean checkCodewordSkew(int codewordSize, int minCodewordWidth, int maxCodewordWidth) {
        return minCodewordWidth + -2 <= codewordSize && codewordSize <= maxCodewordWidth + 2;
    }

    private static DecoderResult decodeCodewords(int[] codewords, int ecLevel, int[] erasures) throws FormatException, ChecksumException {
        if (codewords.length != 0) {
            int numECCodewords = 1 << (ecLevel + 1);
            int correctedErrorsCount = correctErrors(codewords, erasures, numECCodewords);
            verifyCodewordCount(codewords, numECCodewords);
            DecoderResult decoderResult = DecodedBitStreamParser.decode(codewords, String.valueOf(ecLevel));
            decoderResult.setErrorsCorrected(Integer.valueOf(correctedErrorsCount));
            decoderResult.setErasures(Integer.valueOf(erasures.length));
            return decoderResult;
        }
        throw FormatException.getFormatInstance();
    }

    private static int correctErrors(int[] codewords, int[] erasures, int numECCodewords) throws ChecksumException {
        if ((erasures == null || erasures.length <= (numECCodewords / 2) + 3) && numECCodewords >= 0 && numECCodewords <= MAX_EC_CODEWORDS) {
            return errorCorrection.decode(codewords, numECCodewords, erasures);
        }
        throw ChecksumException.getChecksumInstance();
    }

    private static void verifyCodewordCount(int[] codewords, int numECCodewords) throws FormatException {
        if (codewords.length >= 4) {
            int numberOfCodewords = codewords[0];
            if (numberOfCodewords > codewords.length) {
                throw FormatException.getFormatInstance();
            } else if (numberOfCodewords != 0) {
                return;
            } else {
                if (numECCodewords < codewords.length) {
                    codewords[0] = codewords.length - numECCodewords;
                    return;
                }
                throw FormatException.getFormatInstance();
            }
        }
        throw FormatException.getFormatInstance();
    }

    private static int[] getBitCountForCodeword(int codeword) {
        int[] result = new int[8];
        int previousValue = 0;
        int i = result.length - 1;
        while (true) {
            if ((codeword & 1) != previousValue) {
                previousValue = codeword & 1;
                i--;
                if (i < 0) {
                    return result;
                }
            }
            result[i] = result[i] + 1;
            codeword >>= 1;
        }
    }

    private static int getCodewordBucketNumber(int codeword) {
        return getCodewordBucketNumber(getBitCountForCodeword(codeword));
    }

    private static int getCodewordBucketNumber(int[] moduleBitCount) {
        return ((((moduleBitCount[0] - moduleBitCount[2]) + moduleBitCount[4]) - moduleBitCount[6]) + 9) % 9;
    }

    public static String toString(BarcodeValue[][] barcodeMatrix) {
        Formatter formatter = new Formatter();
        for (int row = 0; row < barcodeMatrix.length; row++) {
            formatter.format("Row %2d: ", new Object[]{Integer.valueOf(row)});
            for (BarcodeValue barcodeValue : barcodeMatrix[row]) {
                if (barcodeValue.getValue().length == 0) {
                    formatter.format("        ", (Object[]) null);
                } else {
                    formatter.format("%4d(%2d)", new Object[]{Integer.valueOf(barcodeMatrix[row][column].getValue()[0]), barcodeMatrix[row][column].getConfidence(barcodeMatrix[row][column].getValue()[0])});
                }
            }
            formatter.format("\n", new Object[0]);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
