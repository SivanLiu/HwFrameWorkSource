package com.huawei.zxing.common;

import com.huawei.zxing.Binarizer;
import com.huawei.zxing.LuminanceSource;
import com.huawei.zxing.NotFoundException;

public class GlobalHistogramBinarizer extends Binarizer {
    private static final byte[] EMPTY = new byte[0];
    private static final int LUMINANCE_BITS = 5;
    private static final int LUMINANCE_BUCKETS = 32;
    private static final int LUMINANCE_SHIFT = 3;
    private final int[] buckets = new int[32];
    private byte[] luminances = EMPTY;

    public GlobalHistogramBinarizer(LuminanceSource source) {
        super(source);
    }

    public BitArray getBlackRow(int y, BitArray row) throws NotFoundException {
        int x;
        int i;
        LuminanceSource source = getLuminanceSource();
        int width = source.getWidth();
        if (row == null || row.getSize() < width) {
            row = new BitArray(width);
        } else {
            row.clear();
        }
        initArrays(width);
        byte[] localLuminances = source.getRow(y, this.luminances);
        int[] localBuckets = this.buckets;
        for (x = 0; x < width; x++) {
            i = (localLuminances[x] & 255) >> 3;
            localBuckets[i] = localBuckets[i] + 1;
        }
        x = estimateBlackPoint(localBuckets);
        int center = localLuminances[1] & 255;
        i = localLuminances[0] & 255;
        for (int x2 = 1; x2 < width - 1; x2++) {
            int right = localLuminances[x2 + 1] & 255;
            if (((((center << 2) - i) - right) >> 1) < x) {
                row.set(x2);
            }
            i = center;
            center = right;
        }
        return row;
    }

    public BitMatrix getBlackMatrix() throws NotFoundException {
        int right;
        LuminanceSource source = getLuminanceSource();
        int width = source.getWidth();
        int height = source.getHeight();
        BitMatrix matrix = new BitMatrix(width, height);
        initArrays(width);
        int[] localBuckets = this.buckets;
        for (int y = 1; y < 5; y++) {
            byte[] localLuminances = source.getRow((height * y) / 5, this.luminances);
            right = (width << 2) / 5;
            for (int x = width / 5; x < right; x++) {
                int i = (localLuminances[x] & 255) >> 3;
                localBuckets[i] = localBuckets[i] + 1;
            }
        }
        int blackPoint = estimateBlackPoint(localBuckets);
        byte[] localLuminances2 = source.getMatrix();
        for (int y2 = 0; y2 < height; y2++) {
            int offset = y2 * width;
            for (right = 0; right < width; right++) {
                if ((localLuminances2[offset + right] & 255) < blackPoint) {
                    matrix.set(right, y2);
                }
            }
        }
        return matrix;
    }

    public Binarizer createBinarizer(LuminanceSource source) {
        return new GlobalHistogramBinarizer(source);
    }

    private void initArrays(int luminanceSize) {
        if (this.luminances.length < luminanceSize) {
            this.luminances = new byte[luminanceSize];
        }
        for (int x = 0; x < 32; x++) {
            this.buckets[x] = 0;
        }
    }

    private static int estimateBlackPoint(int[] buckets) throws NotFoundException {
        int x;
        int distanceToBiggest;
        int score;
        int numBuckets = buckets.length;
        int firstPeak = 0;
        int firstPeakSize = 0;
        int x2 = 0;
        int maxBucketCount = 0;
        for (x = 0; x < numBuckets; x++) {
            if (buckets[x] > firstPeakSize) {
                firstPeak = x;
                firstPeakSize = buckets[x];
            }
            if (buckets[x] > maxBucketCount) {
                maxBucketCount = buckets[x];
            }
        }
        x = 0;
        int secondPeakScore = 0;
        while (x2 < numBuckets) {
            distanceToBiggest = x2 - firstPeak;
            score = (buckets[x2] * distanceToBiggest) * distanceToBiggest;
            if (score > secondPeakScore) {
                x = x2;
                secondPeakScore = score;
            }
            x2++;
        }
        if (firstPeak > x) {
            x2 = firstPeak;
            firstPeak = x;
            x = x2;
        }
        if (x - firstPeak > (numBuckets >> 4)) {
            x2 = x - 1;
            distanceToBiggest = -1;
            for (score = x - 1; score > firstPeak; score--) {
                int fromFirst = score - firstPeak;
                int score2 = ((fromFirst * fromFirst) * (x - score)) * (maxBucketCount - buckets[score]);
                if (score2 > distanceToBiggest) {
                    x2 = score;
                    distanceToBiggest = score2;
                }
            }
            return x2 << 3;
        }
        throw NotFoundException.getNotFoundInstance();
    }
}
