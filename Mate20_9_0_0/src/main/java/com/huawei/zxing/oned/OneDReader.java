package com.huawei.zxing.oned;

import com.huawei.zxing.BinaryBitmap;
import com.huawei.zxing.ChecksumException;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Reader;
import com.huawei.zxing.ReaderException;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultMetadataType;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitArray;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public abstract class OneDReader implements Reader {
    protected static final int INTEGER_MATH_SHIFT = 8;
    protected static final int PATTERN_MATCH_RESULT_SCALE_FACTOR = 256;

    public abstract Result decodeRow(int i, BitArray bitArray, Map<DecodeHintType, ?> map) throws NotFoundException, ChecksumException, FormatException;

    public Result decode(BinaryBitmap image) throws NotFoundException, FormatException {
        return decode(image, null);
    }

    public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException {
        try {
            return doDecode(image, hints);
        } catch (NotFoundException nfe) {
            if (image.isRotateSupported()) {
                BinaryBitmap rotatedImage = image.rotateCounterClockwise();
                Result result = doDecode(rotatedImage, hints);
                Map<ResultMetadataType, ?> metadata = result.getResultMetadata();
                int orientation = 270;
                if (metadata != null && metadata.containsKey(ResultMetadataType.ORIENTATION)) {
                    orientation = (((Integer) metadata.get(ResultMetadataType.ORIENTATION)).intValue() + 270) % 360;
                }
                result.putMetadata(ResultMetadataType.ORIENTATION, Integer.valueOf(orientation));
                ResultPoint[] points = result.getResultPoints();
                if (points != null) {
                    int height = rotatedImage.getHeight();
                    for (int i = 0; i < points.length; i++) {
                        points[i] = new ResultPoint((((float) height) - points[i].getY()) - 1.0f, points[i].getX());
                    }
                }
                return result;
            }
            throw nfe;
        }
    }

    public void reset() {
    }

    /* JADX WARNING: Missing block: B:66:0x00fb, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:75:0x013b, code skipped:
            r15 = r22;
            r20 = r1;
            r17 = r2;
            r19 = r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Result doDecode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException {
        int maxLines;
        Map<DecodeHintType, ?> map = hints;
        int width = image.getWidth();
        int height = image.getHeight();
        BitArray row = new BitArray(width);
        int middle = height >> 1;
        int i = 1;
        boolean tryHarder = map != null && map.containsKey(DecodeHintType.TRY_HARDER);
        int rowStep = Math.max(1, height >> (tryHarder ? 8 : 5));
        if (tryHarder) {
            maxLines = height;
        } else {
            maxLines = 15;
        }
        BitArray row2 = row;
        Map<DecodeHintType, ?> hints2 = map;
        int x = 0;
        loop0:
        while (true) {
            int x2 = x;
            if (x2 >= maxLines) {
                break;
            }
            int rowStepsAboveOrBelow = (x2 + 1) >> 1;
            int rowNumber = middle + (((x2 & 1) == 0 ? i : false ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow) * rowStep);
            if (rowNumber >= 0) {
                int i2;
                int i3;
                int i4;
                if (rowNumber >= height) {
                    BinaryBitmap binaryBitmap = image;
                    i2 = width;
                    i3 = height;
                    i4 = middle;
                    break;
                }
                try {
                    row2 = image.getBlackRow(rowNumber, row2);
                    x = 0;
                    while (true) {
                        Map<DecodeHintType, ?> map2;
                        int attempt = x;
                        if (attempt >= 2) {
                            i2 = width;
                            i3 = height;
                            i4 = middle;
                            break;
                        }
                        if (attempt == i) {
                            row2.reverse();
                            if (hints2 != null && hints2.containsKey(DecodeHintType.NEED_RESULT_POINT_CALLBACK)) {
                                map = new EnumMap(DecodeHintType.class);
                                map.putAll(hints2);
                                map.remove(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
                                hints2 = map;
                            }
                        }
                        try {
                            Result result = decodeRow(rowNumber, row2, hints2);
                            i3 = height;
                            if (attempt != 1) {
                                map2 = hints2;
                                i4 = middle;
                                break loop0;
                            }
                            try {
                                map2 = hints2;
                                try {
                                    result.putMetadata(ResultMetadataType.ORIENTATION, Integer.valueOf(180));
                                    height = result.getResultPoints();
                                    if (height == 0) {
                                        i4 = middle;
                                        break loop0;
                                    }
                                    i4 = middle;
                                    try {
                                        height[0] = new ResultPoint((((float) width) - height[0].getX()) - 1.0f, height[0].getY());
                                        i2 = width;
                                        i = 1;
                                        try {
                                            height[1] = new ResultPoint((((float) width) - height[1].getX()) - 1.0f, height[1].getY());
                                            break loop0;
                                        } catch (ReaderException e) {
                                        }
                                    } catch (ReaderException e2) {
                                        i2 = width;
                                        i = 1;
                                    }
                                } catch (ReaderException e3) {
                                    i2 = width;
                                    i4 = middle;
                                    i = 1;
                                }
                            } catch (ReaderException e4) {
                                i2 = width;
                                map2 = hints2;
                                i4 = middle;
                                i = 1;
                            }
                        } catch (ReaderException e5) {
                            i2 = width;
                            i3 = height;
                            map2 = hints2;
                            i4 = middle;
                            i = 1;
                        }
                        x = attempt + 1;
                        height = i3;
                        hints2 = map2;
                        middle = i4;
                        width = i2;
                    }
                } catch (NotFoundException ignored) {
                    i2 = width;
                    i3 = height;
                    i4 = middle;
                    width = ignored;
                }
                x = x2 + 1;
                height = i3;
                middle = i4;
                width = i2;
            } else {
                break;
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    protected static void recordPattern(BitArray row, int start, int[] counters) throws NotFoundException {
        int numCounters = counters.length;
        Arrays.fill(counters, 0, numCounters, 0);
        int end = row.getSize();
        if (start < end) {
            int counterPosition = 0;
            boolean isWhite = row.get(start) ^ true;
            int i = start;
            while (i < end) {
                if ((row.get(i) ^ isWhite) != 0) {
                    counters[counterPosition] = counters[counterPosition] + 1;
                } else {
                    counterPosition++;
                    if (counterPosition == numCounters) {
                        break;
                    }
                    counters[counterPosition] = 1;
                    isWhite = !isWhite;
                }
                i++;
            }
            if (counterPosition == numCounters) {
                return;
            }
            if (counterPosition != numCounters - 1 || i != end) {
                throw NotFoundException.getNotFoundInstance();
            }
            return;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    protected static void recordPatternInReverse(BitArray row, int start, int[] counters) throws NotFoundException {
        int numTransitionsLeft = counters.length;
        boolean last = row.get(start);
        while (start > 0 && numTransitionsLeft >= 0) {
            start--;
            if (row.get(start) != last) {
                numTransitionsLeft--;
                last = !last;
            }
        }
        if (numTransitionsLeft < 0) {
            recordPattern(row, start + 1, counters);
            return;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    protected static int patternMatchVariance(int[] counters, int[] pattern, int maxIndividualVariance) {
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
