package com.huawei.zxing.common;

import com.huawei.zxing.Binarizer;
import com.huawei.zxing.LuminanceSource;
import com.huawei.zxing.NotFoundException;
import java.lang.reflect.Array;

public final class HybridBinarizer extends GlobalHistogramBinarizer {
    private static final int BLOCK_SIZE = 8;
    private static final int BLOCK_SIZE_MASK = 7;
    private static final int BLOCK_SIZE_POWER = 3;
    private static final int MINIMUM_DIMENSION = 40;
    private static final int MIN_DYNAMIC_RANGE = 24;
    private BitMatrix matrix;

    public HybridBinarizer(LuminanceSource source) {
        super(source);
    }

    public BitMatrix getBlackMatrix() throws NotFoundException {
        if (this.matrix != null) {
            return this.matrix;
        }
        LuminanceSource source = getLuminanceSource();
        int width = source.getWidth();
        int height = source.getHeight();
        if (width < 40 || height < 40) {
            this.matrix = super.getBlackMatrix();
        } else {
            byte[] luminances = source.getMatrix();
            int subWidth = width >> 3;
            if ((width & 7) != 0) {
                subWidth++;
            }
            int subWidth2 = subWidth;
            subWidth = height >> 3;
            if ((height & 7) != 0) {
                subWidth++;
            }
            int subHeight = subWidth;
            int[][] blackPoints = calculateBlackPoints(luminances, subWidth2, subHeight, width, height);
            BitMatrix newMatrix = new BitMatrix(width, height);
            calculateThresholdForBlock(luminances, subWidth2, subHeight, width, height, blackPoints, newMatrix);
            this.matrix = newMatrix;
        }
        return this.matrix;
    }

    public Binarizer createBinarizer(LuminanceSource source) {
        return new HybridBinarizer(source);
    }

    private static void calculateThresholdForBlock(byte[] luminances, int subWidth, int subHeight, int width, int height, int[][] blackPoints, BitMatrix matrix) {
        int i = subWidth;
        int i2 = subHeight;
        for (int y = 0; y < i2; y++) {
            int yoffset = y << 3;
            int maxYOffset = height - 8;
            if (yoffset > maxYOffset) {
                yoffset = maxYOffset;
            }
            int x = 0;
            while (true) {
                int x2 = x;
                if (x2 >= i) {
                    break;
                }
                x = x2 << 3;
                int maxXOffset = width - 8;
                if (x > maxXOffset) {
                    x = maxXOffset;
                }
                int xoffset = x;
                int left = cap(x2, 2, i - 3);
                int top = cap(y, 2, i2 - 3);
                int z = -2;
                int sum = 0;
                while (true) {
                    x = z;
                    if (x > 2) {
                        break;
                    }
                    int[] blackRow = blackPoints[top + x];
                    sum += (((blackRow[left - 2] + blackRow[left - 1]) + blackRow[left]) + blackRow[left + 1]) + blackRow[left + 2];
                    z = x + 1;
                }
                thresholdBlock(luminances, xoffset, yoffset, sum / 25, width, matrix);
                x = x2 + 1;
            }
        }
    }

    private static int cap(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }

    private static void thresholdBlock(byte[] luminances, int xoffset, int yoffset, int threshold, int stride, BitMatrix matrix) {
        int y = 0;
        int offset = (yoffset * stride) + xoffset;
        while (y < 8) {
            for (int x = 0; x < 8; x++) {
                if ((luminances[offset + x] & 255) <= threshold) {
                    matrix.set(xoffset + x, yoffset + y);
                }
            }
            y++;
            offset += stride;
        }
    }

    private static int[][] calculateBlackPoints(byte[] luminances, int subWidth, int subHeight, int width, int height) {
        int i = subWidth;
        int i2 = subHeight;
        int[][] blackPoints = (int[][]) Array.newInstance(int.class, new int[]{i2, i});
        int y = 0;
        while (y < i2) {
            int yoffset = y << 3;
            int maxYOffset = height - 8;
            if (yoffset > maxYOffset) {
                yoffset = maxYOffset;
            }
            int x = 0;
            while (x < i) {
                int xoffset = x << 3;
                int maxXOffset = width - 8;
                if (xoffset > maxXOffset) {
                    xoffset = maxXOffset;
                }
                int max = 0;
                int offset = (yoffset * width) + xoffset;
                int min = 255;
                int sum = 0;
                int yy = 0;
                while (true) {
                    int offset2 = offset;
                    i = 8;
                    if (yy >= 8) {
                        break;
                    }
                    int max2 = max;
                    max = min;
                    min = 0;
                    while (min < i) {
                        i = luminances[offset2 + min] & 255;
                        sum += i;
                        if (i < max) {
                            max = i;
                        }
                        i2 = max2;
                        if (i > i2) {
                            max2 = i;
                        } else {
                            max2 = i2;
                        }
                        min++;
                        i = 8;
                        i2 = subHeight;
                    }
                    i2 = max2;
                    if (i2 - max <= 24) {
                        yy++;
                        offset = offset2 + width;
                        min = max;
                        i = subWidth;
                        max = i2;
                        i2 = subHeight;
                    }
                    while (true) {
                        yy++;
                        offset2 += width;
                        i = 8;
                        if (yy >= 8) {
                            break;
                        }
                        min = 0;
                        while (min < i) {
                            sum += luminances[offset2 + min] & 255;
                            min++;
                            i = 8;
                        }
                    }
                    yy++;
                    offset = offset2 + width;
                    min = max;
                    i = subWidth;
                    max = i2;
                    i2 = subHeight;
                }
                i = sum >> 6;
                if (max - min <= 24) {
                    i = min >> 1;
                    if (y > 0 && x > 0) {
                        i2 = ((blackPoints[y - 1][x] + (blackPoints[y][x - 1] * 2)) + blackPoints[y - 1][x - 1]) >> 2;
                        if (min < i2) {
                            i = i2;
                        }
                    }
                }
                blackPoints[y][x] = i;
                x++;
                i = subWidth;
                i2 = subHeight;
            }
            y++;
            i = subWidth;
            i2 = subHeight;
        }
        return blackPoints;
    }
}
