package com.huawei.zxing.qrcode.detector;

import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.ResultPointCallback;
import com.huawei.zxing.common.BitMatrix;
import com.huawei.zxing.common.DetectorResult;
import com.huawei.zxing.common.GridSampler;
import com.huawei.zxing.common.PerspectiveTransform;
import com.huawei.zxing.common.detector.MathUtils;
import com.huawei.zxing.qrcode.decoder.Version;
import java.util.Map;

public class Detector {
    private final BitMatrix image;
    private ResultPointCallback resultPointCallback;

    public Detector(BitMatrix image) {
        this.image = image;
    }

    protected final BitMatrix getImage() {
        return this.image;
    }

    protected final ResultPointCallback getResultPointCallback() {
        return this.resultPointCallback;
    }

    public DetectorResult detect() throws NotFoundException, FormatException {
        return detect(null);
    }

    public final DetectorResult detect(Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException {
        ResultPointCallback resultPointCallback;
        if (hints == null) {
            resultPointCallback = null;
        } else {
            resultPointCallback = (ResultPointCallback) hints.get(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
        }
        this.resultPointCallback = resultPointCallback;
        return processFinderPatternInfo(new FinderPatternFinder(this.image, this.resultPointCallback).find(hints));
    }

    protected final DetectorResult processFinderPatternInfo(FinderPatternInfo info) throws NotFoundException, FormatException {
        FinderPattern topLeft = info.getTopLeft();
        FinderPattern topRight = info.getTopRight();
        FinderPattern bottomLeft = info.getBottomLeft();
        float moduleSize = calculateModuleSize(topLeft, topRight, bottomLeft);
        if (moduleSize >= 1.0f) {
            ResultPoint[] points;
            int dimension = computeDimension(topLeft, topRight, bottomLeft, moduleSize);
            Version provisionalVersion = Version.getProvisionalVersionForDimension(dimension);
            int modulesBetweenFPCenters = provisionalVersion.getDimensionForVersion() - 7;
            AlignmentPattern alignmentPattern = null;
            if (provisionalVersion.getAlignmentPatternCenters().length > 0) {
                float correctionToTopLeft = 1.0f - (3.0f / ((float) modulesBetweenFPCenters));
                int estAlignmentX = (int) (topLeft.getX() + ((((topRight.getX() - topLeft.getX()) + bottomLeft.getX()) - topLeft.getX()) * correctionToTopLeft));
                int estAlignmentY = (int) (topLeft.getY() + ((((topRight.getY() - topLeft.getY()) + bottomLeft.getY()) - topLeft.getY()) * correctionToTopLeft));
                int i = 4;
                while (true) {
                    int i2 = i;
                    if (i2 > 16) {
                        break;
                    }
                    try {
                        alignmentPattern = findAlignmentInRegion(moduleSize, estAlignmentX, estAlignmentY, (float) i2);
                        break;
                    } catch (NotFoundException e) {
                        i = i2 << 1;
                    }
                }
            }
            BitMatrix bits = sampleGrid(this.image, createTransform(topLeft, topRight, bottomLeft, alignmentPattern, dimension), dimension);
            if (alignmentPattern == null) {
                points = new ResultPoint[]{bottomLeft, topLeft, topRight};
            } else {
                points = new ResultPoint[]{bottomLeft, topLeft, topRight, alignmentPattern};
            }
            return new DetectorResult(bits, points);
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static PerspectiveTransform createTransform(ResultPoint topLeft, ResultPoint topRight, ResultPoint bottomLeft, ResultPoint alignmentPattern, int dimension) {
        float bottomRightX;
        float bottomRightY;
        float sourceBottomRightX;
        float sourceBottomRightY;
        float dimMinusThree = ((float) dimension) - 3.5f;
        if (alignmentPattern != null) {
            float sourceBottomRightX2 = dimMinusThree - 3.0f;
            bottomRightX = alignmentPattern.getX();
            bottomRightY = alignmentPattern.getY();
            sourceBottomRightX = sourceBottomRightX2;
            sourceBottomRightY = sourceBottomRightX2;
        } else {
            sourceBottomRightY = dimMinusThree;
            bottomRightX = (topRight.getX() - topLeft.getX()) + bottomLeft.getX();
            bottomRightY = (topRight.getY() - topLeft.getY()) + bottomLeft.getY();
            sourceBottomRightX = dimMinusThree;
        }
        return PerspectiveTransform.quadrilateralToQuadrilateral(3.5f, 3.5f, dimMinusThree, 3.5f, sourceBottomRightX, sourceBottomRightY, 3.5f, dimMinusThree, topLeft.getX(), topLeft.getY(), topRight.getX(), topRight.getY(), bottomRightX, bottomRightY, bottomLeft.getX(), bottomLeft.getY());
    }

    private static BitMatrix sampleGrid(BitMatrix image, PerspectiveTransform transform, int dimension) throws NotFoundException {
        return GridSampler.getInstance().sampleGrid(image, dimension, dimension, transform);
    }

    private static int computeDimension(ResultPoint topLeft, ResultPoint topRight, ResultPoint bottomLeft, float moduleSize) throws NotFoundException {
        int dimension = ((MathUtils.round(ResultPoint.distance(topLeft, topRight) / moduleSize) + MathUtils.round(ResultPoint.distance(topLeft, bottomLeft) / moduleSize)) >> 1) + 7;
        int i = dimension & 3;
        if (i == 0) {
            return dimension + 1;
        }
        switch (i) {
            case 2:
                return dimension - 1;
            case 3:
                throw NotFoundException.getNotFoundInstance();
            default:
                return dimension;
        }
    }

    protected final float calculateModuleSize(ResultPoint topLeft, ResultPoint topRight, ResultPoint bottomLeft) {
        return (calculateModuleSizeOneWay(topLeft, topRight) + calculateModuleSizeOneWay(topLeft, bottomLeft)) / 2.0f;
    }

    private float calculateModuleSizeOneWay(ResultPoint pattern, ResultPoint otherPattern) {
        float moduleSizeEst1 = sizeOfBlackWhiteBlackRunBothWays((int) pattern.getX(), (int) pattern.getY(), (int) otherPattern.getX(), (int) otherPattern.getY());
        float moduleSizeEst2 = sizeOfBlackWhiteBlackRunBothWays((int) otherPattern.getX(), (int) otherPattern.getY(), (int) pattern.getX(), (int) pattern.getY());
        if (Float.isNaN(moduleSizeEst1)) {
            return moduleSizeEst2 / 7.0f;
        }
        if (Float.isNaN(moduleSizeEst2)) {
            return moduleSizeEst1 / 7.0f;
        }
        return (moduleSizeEst1 + moduleSizeEst2) / 14.0f;
    }

    private float sizeOfBlackWhiteBlackRunBothWays(int fromX, int fromY, int toX, int toY) {
        float result = sizeOfBlackWhiteBlackRun(fromX, fromY, toX, toY);
        float scale = 1.0f;
        int otherToX = fromX - (toX - fromX);
        if (otherToX < 0) {
            scale = ((float) fromX) / ((float) (fromX - otherToX));
            otherToX = 0;
        } else if (otherToX >= this.image.getWidth()) {
            scale = ((float) ((this.image.getWidth() - 1) - fromX)) / ((float) (otherToX - fromX));
            otherToX = this.image.getWidth() - 1;
        }
        int otherToY = (int) (((float) fromY) - (((float) (toY - fromY)) * scale));
        scale = 1.0f;
        if (otherToY < 0) {
            scale = ((float) fromY) / ((float) (fromY - otherToY));
            otherToY = 0;
        } else if (otherToY >= this.image.getHeight()) {
            scale = ((float) ((this.image.getHeight() - 1) - fromY)) / ((float) (otherToY - fromY));
            otherToY = this.image.getHeight() - 1;
        }
        return (result + sizeOfBlackWhiteBlackRun(fromX, fromY, (int) (((float) fromX) + (((float) (otherToX - fromX)) * scale)), otherToY)) - 1.0f;
    }

    private float sizeOfBlackWhiteBlackRun(int fromX, int fromY, int toX, int toY) {
        int fromX2;
        int fromY2;
        int toX2;
        int toY2;
        int xLimit;
        boolean z = true;
        boolean steep = Math.abs(toY - fromY) > Math.abs(toX - fromX);
        if (steep) {
            fromX2 = fromY;
            fromY2 = fromX;
            toX2 = toY;
            toY2 = toX;
        } else {
            fromX2 = fromX;
            fromY2 = fromY;
            toX2 = toX;
            toY2 = toY;
        }
        int dx = Math.abs(toX2 - fromX2);
        int dy = Math.abs(toY2 - fromY2);
        int error = (-dx) >> 1;
        int ystep = -1;
        int xstep = fromX2 < toX2 ? 1 : -1;
        if (fromY2 < toY2) {
            ystep = 1;
        }
        int state = 0;
        int xLimit2 = toX2 + xstep;
        int x = fromX2;
        int error2 = error;
        error = fromY2;
        while (x != xLimit2) {
            int realX = steep ? error : x;
            int realY = steep ? x : error;
            if (state != z) {
                z = false;
            }
            boolean steep2 = steep;
            xLimit = xLimit2;
            if (z == this.image.get(realX, realY)) {
                if (state == true) {
                    return MathUtils.distance(x, error, fromX2, fromY2);
                }
                state++;
            }
            error2 += dy;
            if (error2 > 0) {
                if (error == toY2) {
                    break;
                }
                error += ystep;
                error2 -= dx;
            }
            x += xstep;
            steep = steep2;
            xLimit2 = xLimit;
            z = true;
        }
        xLimit = xLimit2;
        if (state == 2) {
            return MathUtils.distance(toX2 + xstep, toY2, fromX2, fromY2);
        }
        return Float.NaN;
    }

    protected final AlignmentPattern findAlignmentInRegion(float overallEstModuleSize, int estAlignmentX, int estAlignmentY, float allowanceFactor) throws NotFoundException {
        int allowance = (int) (allowanceFactor * overallEstModuleSize);
        int alignmentAreaLeftX = Math.max(0, estAlignmentX - allowance);
        int alignmentAreaRightX = Math.min(this.image.getWidth() - 1, estAlignmentX + allowance);
        if (((float) (alignmentAreaRightX - alignmentAreaLeftX)) >= overallEstModuleSize * 3.0f) {
            int alignmentAreaTopY = Math.max(0, estAlignmentY - allowance);
            int alignmentAreaBottomY = Math.min(this.image.getHeight() - 1, estAlignmentY + allowance);
            if (((float) (alignmentAreaBottomY - alignmentAreaTopY)) >= overallEstModuleSize * 3.0f) {
                return new AlignmentPatternFinder(this.image, alignmentAreaLeftX, alignmentAreaTopY, alignmentAreaRightX - alignmentAreaLeftX, alignmentAreaBottomY - alignmentAreaTopY, overallEstModuleSize, this.resultPointCallback).find();
            }
            throw NotFoundException.getNotFoundInstance();
        }
        throw NotFoundException.getNotFoundInstance();
    }
}
