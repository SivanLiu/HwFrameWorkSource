package com.huawei.zxing.common.detector;

import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitMatrix;

public final class MonochromeRectangleDetector {
    private static final int MAX_MODULES = 32;
    private final BitMatrix image;

    public MonochromeRectangleDetector(BitMatrix image) {
        this.image = image;
    }

    public ResultPoint[] detect() throws NotFoundException {
        int height = this.image.getHeight();
        int width = this.image.getWidth();
        int halfHeight = height >> 1;
        int halfWidth = width >> 1;
        int deltaY = Math.max(1, height / 256);
        int deltaX = Math.max(1, width / 256);
        int bottom = height;
        ResultPoint pointA = findCornerFromCenter(halfWidth, 0, 0, width, halfHeight, -deltaY, 0, bottom, halfWidth >> 1);
        int top = ((int) pointA.getY()) - 1;
        int i = -deltaX;
        int deltaX2 = deltaX;
        ResultPoint pointD = halfWidth;
        int deltaY2 = deltaY;
        int i2 = 1;
        int width2 = width;
        height = halfHeight;
        int i3 = top;
        int i4 = bottom;
        ResultPoint pointB = findCornerFromCenter(pointD, i, 0, width, height, 0, i3, i4, halfHeight >> 1);
        int left = ((int) pointB.getX()) - 1;
        int i5 = left;
        ResultPoint pointC = findCornerFromCenter(pointD, deltaX2, i5, width, height, 0, i3, i4, halfHeight >> 1);
        int right = ((int) pointC.getX()) + 1;
        ResultPoint pointA2 = findCornerFromCenter(halfWidth, 0, left, right, halfHeight, -deltaY2, top, ((int) findCornerFromCenter(pointD, 0, i5, right, height, deltaY2, i3, i4, halfWidth >> 1).getY()) + 1, halfWidth >> 2);
        return new ResultPoint[]{pointA2, pointB, pointC, pointD};
    }

    private ResultPoint findCornerFromCenter(int centerX, int deltaX, int left, int right, int centerY, int deltaY, int top, int bottom, int maxWhiteRun) throws NotFoundException {
        int i = centerX;
        int i2 = centerY;
        int[] lastRange = null;
        int y = i2;
        int x = i;
        while (true) {
            int i3 = bottom;
            int i4;
            int i5;
            if (y >= i3) {
                i4 = right;
                i5 = top;
                break;
            }
            i5 = top;
            if (y < i5) {
                i4 = right;
                break;
            }
            i4 = right;
            if (x >= i4) {
                break;
            }
            int i6 = left;
            if (x < i6) {
                break;
            }
            int[] range;
            if (deltaX == 0) {
                range = blackWhiteRange(y, maxWhiteRun, i6, i4, true);
            } else {
                range = blackWhiteRange(x, maxWhiteRun, i5, i3, false);
            }
            int lastY;
            if (range != null) {
                lastRange = range;
                y += deltaY;
                x += deltaX;
            } else if (lastRange == null) {
                throw NotFoundException.getNotFoundInstance();
            } else if (deltaX == 0) {
                lastY = y - deltaY;
                if (lastRange[0] >= i) {
                    return new ResultPoint((float) lastRange[1], (float) lastY);
                }
                if (lastRange[1] <= i) {
                    return new ResultPoint((float) lastRange[0], (float) lastY);
                }
                return new ResultPoint((float) (deltaY > 0 ? lastRange[0] : lastRange[1]), (float) lastY);
            } else {
                lastY = x - deltaX;
                if (lastRange[0] >= i2) {
                    return new ResultPoint((float) lastY, (float) lastRange[1]);
                }
                if (lastRange[1] <= i2) {
                    return new ResultPoint((float) lastY, (float) lastRange[0]);
                }
                return new ResultPoint((float) lastY, (float) (deltaX < 0 ? lastRange[0] : lastRange[1]));
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x0035 A:{SYNTHETIC, EDGE_INSN: B:53:0x0035->B:16:0x0035 ?: BREAK  , EDGE_INSN: B:53:0x0035->B:16:0x0035 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0022  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x0072 A:{SYNTHETIC, EDGE_INSN: B:67:0x0072->B:37:0x0072 ?: BREAK  , EDGE_INSN: B:67:0x0072->B:37:0x0072 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x005f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int[] blackWhiteRange(int fixedDimension, int maxWhiteRun, int minDim, int maxDim, boolean horizontal) {
        int start;
        int whiteRunSize;
        int center = (minDim + maxDim) >> 1;
        int start2 = center;
        while (start2 >= minDim) {
            if (!horizontal) {
                start = start2;
                while (true) {
                    start--;
                    if (start < minDim) {
                    }
                }
                whiteRunSize = start2 - start;
                if (start >= minDim) {
                }
                break;
            }
            start = start2;
            while (true) {
                start--;
                if (start < minDim) {
                    break;
                } else if (horizontal) {
                    if (this.image.get(start, fixedDimension)) {
                        break;
                    }
                } else if (this.image.get(fixedDimension, start)) {
                    break;
                }
            }
            whiteRunSize = start2 - start;
            if (start >= minDim || whiteRunSize > maxWhiteRun) {
                break;
            }
            start2 = start;
            start2--;
        }
        start2++;
        start = center;
        while (start < maxDim) {
            int whiteRunSize2;
            if (!horizontal) {
                whiteRunSize = start;
                while (true) {
                    whiteRunSize++;
                    if (whiteRunSize >= maxDim) {
                    }
                }
                whiteRunSize2 = whiteRunSize - start;
                if (whiteRunSize < maxDim) {
                }
                break;
            }
            whiteRunSize = start;
            while (true) {
                whiteRunSize++;
                if (whiteRunSize >= maxDim) {
                    break;
                } else if (horizontal) {
                    if (this.image.get(whiteRunSize, fixedDimension)) {
                        break;
                    }
                } else if (this.image.get(fixedDimension, whiteRunSize)) {
                    break;
                }
            }
            whiteRunSize2 = whiteRunSize - start;
            if (whiteRunSize < maxDim || whiteRunSize2 > maxWhiteRun) {
                break;
            }
            start = whiteRunSize;
            start++;
        }
        if (start - 1 <= start2) {
            return null;
        }
        return new int[]{start2, start - 1};
    }
}
