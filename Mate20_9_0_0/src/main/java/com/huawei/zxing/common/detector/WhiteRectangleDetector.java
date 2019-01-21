package com.huawei.zxing.common.detector;

import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitMatrix;

public final class WhiteRectangleDetector {
    private static final int CORR = 1;
    private static final int INIT_SIZE = 30;
    private final int downInit;
    private final int height;
    private final BitMatrix image;
    private final int leftInit;
    private final int rightInit;
    private final int upInit;
    private final int width;

    public WhiteRectangleDetector(BitMatrix image) throws NotFoundException {
        this.image = image;
        this.height = image.getHeight();
        this.width = image.getWidth();
        this.leftInit = (this.width - 30) >> 1;
        this.rightInit = (this.width + 30) >> 1;
        this.upInit = (this.height - 30) >> 1;
        this.downInit = (this.height + 30) >> 1;
        if (this.upInit < 0 || this.leftInit < 0 || this.downInit >= this.height || this.rightInit >= this.width) {
            throw NotFoundException.getNotFoundInstance();
        }
    }

    public WhiteRectangleDetector(BitMatrix image, int initSize, int x, int y) throws NotFoundException {
        this.image = image;
        this.height = image.getHeight();
        this.width = image.getWidth();
        int halfsize = initSize >> 1;
        this.leftInit = x - halfsize;
        this.rightInit = x + halfsize;
        this.upInit = y - halfsize;
        this.downInit = y + halfsize;
        if (this.upInit < 0 || this.leftInit < 0 || this.downInit >= this.height || this.rightInit >= this.width) {
            throw NotFoundException.getNotFoundInstance();
        }
    }

    public ResultPoint[] detect() throws NotFoundException {
        int right;
        boolean z;
        int down;
        int left = this.leftInit;
        int right2 = this.rightInit;
        int up = this.upInit;
        int down2 = this.downInit;
        boolean sizeExceeded = false;
        boolean aBlackPointFoundOnBorder = true;
        int left2 = left;
        boolean atLeastOneBlackPointFoundOnBorder = false;
        while (aBlackPointFoundOnBorder) {
            boolean aBlackPointFoundOnBorder2 = false;
            right = right2;
            boolean rightBorderNotWhite = true;
            while (rightBorderNotWhite && right < this.width) {
                rightBorderNotWhite = containsBlackPoint(up, down2, right, false);
                if (rightBorderNotWhite) {
                    right++;
                    aBlackPointFoundOnBorder2 = true;
                }
            }
            if (right >= this.width) {
                sizeExceeded = true;
                z = aBlackPointFoundOnBorder2;
                down = down2;
                break;
            }
            boolean aBlackPointFoundOnBorder3 = aBlackPointFoundOnBorder2;
            down = down2;
            down2 = 1;
            while (down2 != 0 && down < this.height) {
                down2 = containsBlackPoint(left2, right, down, true);
                if (down2 != 0) {
                    down++;
                    aBlackPointFoundOnBorder3 = true;
                }
            }
            if (down >= this.height) {
                sizeExceeded = true;
                z = aBlackPointFoundOnBorder3;
                break;
            }
            boolean aBlackPointFoundOnBorder4 = aBlackPointFoundOnBorder3;
            int left3 = left2;
            boolean leftBorderNotWhite = true;
            while (leftBorderNotWhite && left3 >= 0) {
                leftBorderNotWhite = containsBlackPoint(up, down, left3, false);
                if (leftBorderNotWhite) {
                    left3--;
                    aBlackPointFoundOnBorder4 = true;
                }
            }
            if (left3 < 0) {
                sizeExceeded = true;
                left2 = left3;
                z = aBlackPointFoundOnBorder4;
                break;
            }
            z = aBlackPointFoundOnBorder4;
            int up2 = up;
            boolean topBorderNotWhite = true;
            while (topBorderNotWhite && up2 >= 0) {
                topBorderNotWhite = containsBlackPoint(left3, right, up2, true);
                if (topBorderNotWhite) {
                    up2--;
                    z = true;
                }
            }
            if (up2 < 0) {
                sizeExceeded = true;
                left2 = left3;
                up = up2;
                break;
            }
            if (z) {
                atLeastOneBlackPointFoundOnBorder = true;
            }
            right2 = right;
            down2 = down;
            left2 = left3;
            up = up2;
            aBlackPointFoundOnBorder = z;
        }
        down = down2;
        z = aBlackPointFoundOnBorder;
        right = right2;
        if (sizeExceeded || !atLeastOneBlackPointFoundOnBorder) {
            throw NotFoundException.getNotFoundInstance();
        }
        right2 = right - left2;
        ResultPoint z2 = null;
        for (down2 = 1; down2 < right2; down2++) {
            z2 = getBlackPointOnSegment((float) left2, (float) (down - down2), (float) (left2 + down2), (float) down);
            if (z2 != null) {
                break;
            }
        }
        if (z2 != null) {
            ResultPoint t = null;
            for (down2 = 1; down2 < right2; down2++) {
                t = getBlackPointOnSegment((float) left2, (float) (up + down2), (float) (left2 + down2), (float) up);
                if (t != null) {
                    break;
                }
            }
            if (t != null) {
                ResultPoint x = null;
                down2 = 1;
                while (down2 < right2) {
                    boolean atLeastOneBlackPointFoundOnBorder2 = atLeastOneBlackPointFoundOnBorder;
                    x = getBlackPointOnSegment((float) right, (float) (up + down2), (float) (right - down2), (float) up);
                    if (x != null) {
                        break;
                    }
                    down2++;
                    atLeastOneBlackPointFoundOnBorder = atLeastOneBlackPointFoundOnBorder2;
                }
                if (x != null) {
                    atLeastOneBlackPointFoundOnBorder = false;
                    int i = 1;
                    while (true) {
                        down2 = i;
                        if (down2 >= right2) {
                            Object obj = atLeastOneBlackPointFoundOnBorder;
                            break;
                        }
                        ResultPoint y = atLeastOneBlackPointFoundOnBorder;
                        atLeastOneBlackPointFoundOnBorder = getBlackPointOnSegment((float) right, (float) (down - down2), (float) (right - down2), (float) down);
                        if (atLeastOneBlackPointFoundOnBorder) {
                            break;
                        }
                        i = down2 + 1;
                    }
                    if (atLeastOneBlackPointFoundOnBorder) {
                        return centerEdges(atLeastOneBlackPointFoundOnBorder, z2, x, t);
                    }
                    throw NotFoundException.getNotFoundInstance();
                }
                throw NotFoundException.getNotFoundInstance();
            }
            throw NotFoundException.getNotFoundInstance();
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private ResultPoint getBlackPointOnSegment(float aX, float aY, float bX, float bY) {
        int dist = MathUtils.round(MathUtils.distance(aX, aY, bX, bY));
        float xStep = (bX - aX) / ((float) dist);
        float yStep = (bY - aY) / ((float) dist);
        for (int i = 0; i < dist; i++) {
            int x = MathUtils.round((((float) i) * xStep) + aX);
            int y = MathUtils.round((((float) i) * yStep) + aY);
            if (this.image.get(x, y)) {
                return new ResultPoint((float) x, (float) y);
            }
        }
        return null;
    }

    private ResultPoint[] centerEdges(ResultPoint y, ResultPoint z, ResultPoint x, ResultPoint t) {
        float yi = y.getX();
        float yj = y.getY();
        float zi = z.getX();
        float zj = z.getY();
        float xi = x.getX();
        float xj = x.getY();
        float ti = t.getX();
        float tj = t.getY();
        if (yi < ((float) this.width) / 2.0f) {
            return new ResultPoint[]{new ResultPoint(ti - 1.0f, tj + 1.0f), new ResultPoint(zi + 1.0f, zj + 1.0f), new ResultPoint(xi - 1.0f, xj - 1.0f), new ResultPoint(yi + 1.0f, yj - 1.0f)};
        }
        return new ResultPoint[]{new ResultPoint(ti + 1.0f, tj + 1.0f), new ResultPoint(zi + 1.0f, zj - 1.0f), new ResultPoint(xi - 1.0f, xj + 1.0f), new ResultPoint(yi - 1.0f, yj - 1.0f)};
    }

    private boolean containsBlackPoint(int a, int b, int fixed, boolean horizontal) {
        int x;
        if (horizontal) {
            for (x = a; x <= b; x++) {
                if (this.image.get(x, fixed)) {
                    return true;
                }
            }
        } else {
            for (x = a; x <= b; x++) {
                if (this.image.get(fixed, x)) {
                    return true;
                }
            }
        }
        return false;
    }
}
