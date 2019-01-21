package com.huawei.zxing.datamatrix.detector;

import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitMatrix;
import com.huawei.zxing.common.DetectorResult;
import com.huawei.zxing.common.GridSampler;
import com.huawei.zxing.common.detector.MathUtils;
import com.huawei.zxing.common.detector.WhiteRectangleDetector;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class Detector {
    private final BitMatrix image;
    private final WhiteRectangleDetector rectangleDetector;

    private static final class ResultPointsAndTransitions {
        private final ResultPoint from;
        private final ResultPoint to;
        private final int transitions;

        private ResultPointsAndTransitions(ResultPoint from, ResultPoint to, int transitions) {
            this.from = from;
            this.to = to;
            this.transitions = transitions;
        }

        ResultPoint getFrom() {
            return this.from;
        }

        ResultPoint getTo() {
            return this.to;
        }

        public int getTransitions() {
            return this.transitions;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.from);
            stringBuilder.append("/");
            stringBuilder.append(this.to);
            stringBuilder.append('/');
            stringBuilder.append(this.transitions);
            return stringBuilder.toString();
        }
    }

    private static final class ResultPointsAndTransitionsComparator implements Comparator<ResultPointsAndTransitions>, Serializable {
        private ResultPointsAndTransitionsComparator() {
        }

        public int compare(ResultPointsAndTransitions o1, ResultPointsAndTransitions o2) {
            return o1.getTransitions() - o2.getTransitions();
        }
    }

    public Detector(BitMatrix image) throws NotFoundException {
        this.image = image;
        this.rectangleDetector = new WhiteRectangleDetector(image);
    }

    public DetectorResult detect() throws NotFoundException {
        ResultPoint point;
        ResultPoint[] cornerPoints = this.rectangleDetector.detect();
        ResultPoint pointA = cornerPoints[0];
        ResultPoint pointB = cornerPoints[1];
        ResultPoint pointC = cornerPoints[2];
        ResultPoint pointD = cornerPoints[3];
        List<ResultPointsAndTransitions> transitions = new ArrayList(4);
        transitions.add(transitionsBetween(pointA, pointB));
        transitions.add(transitionsBetween(pointA, pointC));
        transitions.add(transitionsBetween(pointB, pointD));
        transitions.add(transitionsBetween(pointC, pointD));
        Collections.sort(transitions, new ResultPointsAndTransitionsComparator());
        ResultPointsAndTransitions lSideOne = (ResultPointsAndTransitions) transitions.get(0);
        ResultPointsAndTransitions lSideTwo = (ResultPointsAndTransitions) transitions.get(1);
        Map<ResultPoint, Integer> pointCount = new HashMap();
        increment(pointCount, lSideOne.getFrom());
        increment(pointCount, lSideOne.getTo());
        increment(pointCount, lSideTwo.getFrom());
        increment(pointCount, lSideTwo.getTo());
        ResultPoint maybeBottomRight = null;
        ResultPoint maybeTopLeft = null;
        ResultPoint bottomLeft = null;
        for (Entry<ResultPoint, Integer> entry : pointCount.entrySet()) {
            point = (ResultPoint) entry.getKey();
            if (((Integer) entry.getValue()).intValue() == 2) {
                bottomLeft = point;
            } else if (maybeTopLeft == null) {
                maybeTopLeft = point;
            } else {
                maybeBottomRight = point;
            }
        }
        List<ResultPointsAndTransitions> list;
        ResultPoint[] resultPointArr;
        if (maybeTopLeft == null || bottomLeft == null || maybeBottomRight == null) {
            ResultPointsAndTransitions resultPointsAndTransitions = lSideTwo;
            ResultPointsAndTransitions resultPointsAndTransitions2 = lSideOne;
            list = transitions;
            ResultPoint resultPoint = pointD;
            resultPointArr = cornerPoints;
            throw NotFoundException.getNotFoundInstance();
        }
        int dimensionTop;
        int cornerPoints2;
        ResultPoint topRight;
        ResultPoint[] corners = new ResultPoint[]{maybeTopLeft, bottomLeft, maybeBottomRight};
        ResultPoint.orderBestPatterns(corners);
        ResultPoint bottomRight = corners[0];
        point = corners[1];
        ResultPoint topLeft = corners[2];
        if (!pointCount.containsKey(pointA)) {
            bottomLeft = pointA;
        } else if (!pointCount.containsKey(pointB)) {
            bottomLeft = pointB;
        } else if (pointCount.containsKey(pointC)) {
            bottomLeft = pointD;
        } else {
            bottomLeft = pointC;
        }
        ResultPoint topRight2 = bottomLeft;
        int dimensionTop2 = transitionsBetween(topLeft, topRight2).getTransitions();
        int dimensionRight = transitionsBetween(bottomRight, topRight2).getTransitions();
        if ((dimensionTop2 & 1) == 1) {
            dimensionTop2++;
        }
        dimensionTop2 += 2;
        if ((dimensionRight & 1) == 1) {
            dimensionRight++;
        }
        dimensionRight += 2;
        if (4 * dimensionTop2 >= 7 * dimensionRight) {
            dimensionTop = dimensionTop2;
            list = transitions;
            resultPointArr = cornerPoints;
            cornerPoints2 = 4;
            topRight = topRight2;
        } else if (4 * dimensionRight >= 7 * dimensionTop2) {
            dimensionTop = dimensionTop2;
            Object obj = transitions;
            resultPointArr = cornerPoints;
            cornerPoints2 = 4;
            topRight = topRight2;
        } else {
            cornerPoints2 = 4;
            topRight = topRight2;
            bottomLeft = correctTopRight(point, bottomRight, topLeft, topRight2, Math.min(dimensionRight, dimensionTop2));
            if (bottomLeft == null) {
                bottomLeft = topRight;
            }
            pointCount = Math.max(transitionsBetween(topLeft, bottomLeft).getTransitions(), transitionsBetween(bottomRight, bottomLeft).getTransitions()) + 1;
            if ((pointCount & 1) == 1) {
                pointCount++;
            }
            pointCount = sampleGrid(this.image, topLeft, point, bottomRight, bottomLeft, pointCount, pointCount);
            int dimension = pointD;
            lSideOne = new ResultPoint[cornerPoints2];
            lSideOne[0] = topLeft;
            lSideOne[1] = point;
            lSideOne[2] = bottomRight;
            lSideOne[3] = bottomLeft;
            return new DetectorResult(pointCount, lSideOne);
        }
        bottomLeft = correctTopRightRectangular(point, bottomRight, topLeft, topRight, dimensionTop, dimensionRight);
        if (bottomLeft == null) {
            bottomLeft = topRight;
        }
        pointCount = transitionsBetween(topLeft, bottomLeft).getTransitions();
        int dimensionRight2 = transitionsBetween(bottomRight, bottomLeft).getTransitions();
        if ((pointCount & 1) == 1) {
            pointCount++;
        }
        if ((dimensionRight2 & 1) == 1) {
            dimensionRight2++;
        }
        Map<ResultPoint, Integer> map = pointCount;
        dimensionRight = dimensionRight2;
        pointCount = sampleGrid(this.image, topLeft, point, bottomRight, bottomLeft, pointCount, dimensionRight2);
        lSideOne = new ResultPoint[cornerPoints2];
        lSideOne[0] = topLeft;
        lSideOne[1] = point;
        lSideOne[2] = bottomRight;
        lSideOne[3] = bottomLeft;
        return new DetectorResult(pointCount, lSideOne);
    }

    private ResultPoint correctTopRightRectangular(ResultPoint bottomLeft, ResultPoint bottomRight, ResultPoint topLeft, ResultPoint topRight, int dimensionTop, int dimensionRight) {
        float corr = ((float) distance(bottomLeft, bottomRight)) / ((float) dimensionTop);
        int norm = distance(topLeft, topRight);
        ResultPoint c1 = new ResultPoint(topRight.getX() + (corr * ((topRight.getX() - topLeft.getX()) / ((float) norm))), topRight.getY() + (corr * ((topRight.getY() - topLeft.getY()) / ((float) norm))));
        float corr2 = ((float) distance(bottomLeft, topLeft)) / ((float) dimensionRight);
        int norm2 = distance(bottomRight, topRight);
        ResultPoint c2 = new ResultPoint(topRight.getX() + (corr2 * ((topRight.getX() - bottomRight.getX()) / ((float) norm2))), topRight.getY() + (corr2 * ((topRight.getY() - bottomRight.getY()) / ((float) norm2))));
        if (isValid(c1)) {
            if (isValid(c2) && Math.abs(dimensionTop - transitionsBetween(topLeft, c1).getTransitions()) + Math.abs(dimensionRight - transitionsBetween(bottomRight, c1).getTransitions()) > Math.abs(dimensionTop - transitionsBetween(topLeft, c2).getTransitions()) + Math.abs(dimensionRight - transitionsBetween(bottomRight, c2).getTransitions())) {
                return c2;
            }
            return c1;
        } else if (isValid(c2)) {
            return c2;
        } else {
            return null;
        }
    }

    private ResultPoint correctTopRight(ResultPoint bottomLeft, ResultPoint bottomRight, ResultPoint topLeft, ResultPoint topRight, int dimension) {
        float corr = ((float) distance(bottomLeft, bottomRight)) / ((float) dimension);
        int norm = distance(topLeft, topRight);
        ResultPoint c1 = new ResultPoint(topRight.getX() + (corr * ((topRight.getX() - topLeft.getX()) / ((float) norm))), topRight.getY() + (corr * ((topRight.getY() - topLeft.getY()) / ((float) norm))));
        float corr2 = ((float) distance(bottomLeft, topLeft)) / ((float) dimension);
        int norm2 = distance(bottomRight, topRight);
        ResultPoint c2 = new ResultPoint(topRight.getX() + (corr2 * ((topRight.getX() - bottomRight.getX()) / ((float) norm2))), topRight.getY() + (corr2 * ((topRight.getY() - bottomRight.getY()) / ((float) norm2))));
        if (isValid(c1)) {
            if (!isValid(c2)) {
                return c1;
            }
            return Math.abs(transitionsBetween(topLeft, c1).getTransitions() - transitionsBetween(bottomRight, c1).getTransitions()) <= Math.abs(transitionsBetween(topLeft, c2).getTransitions() - transitionsBetween(bottomRight, c2).getTransitions()) ? c1 : c2;
        } else if (isValid(c2)) {
            return c2;
        } else {
            return null;
        }
    }

    private boolean isValid(ResultPoint p) {
        return p.getX() >= 0.0f && p.getX() < ((float) this.image.getWidth()) && p.getY() > 0.0f && p.getY() < ((float) this.image.getHeight());
    }

    private static int distance(ResultPoint a, ResultPoint b) {
        return MathUtils.round(ResultPoint.distance(a, b));
    }

    private static void increment(Map<ResultPoint, Integer> table, ResultPoint key) {
        Integer value = (Integer) table.get(key);
        int i = 1;
        if (value != null) {
            i = 1 + value.intValue();
        }
        table.put(key, Integer.valueOf(i));
    }

    private static BitMatrix sampleGrid(BitMatrix image, ResultPoint topLeft, ResultPoint bottomLeft, ResultPoint bottomRight, ResultPoint topRight, int dimensionX, int dimensionY) throws NotFoundException {
        int i = dimensionX;
        int i2 = dimensionY;
        return GridSampler.getInstance().sampleGrid(image, i, i2, 0.5f, 0.5f, ((float) i) - 0.5f, 0.5f, ((float) i) - 0.5f, ((float) i2) - 0.5f, 0.5f, ((float) i2) - 0.5f, topLeft.getX(), topLeft.getY(), topRight.getX(), topRight.getY(), bottomRight.getX(), bottomRight.getY(), bottomLeft.getX(), bottomLeft.getY());
    }

    private ResultPointsAndTransitions transitionsBetween(ResultPoint from, ResultPoint to) {
        Detector detector = this;
        int fromX = (int) from.getX();
        boolean fromY = (int) from.getY();
        boolean toX = (int) to.getX();
        boolean toY = (int) to.getY();
        int xstep = 1;
        boolean steep = Math.abs(toY - fromY) > Math.abs(toX - fromX);
        if (steep) {
            boolean temp = fromX;
            fromX = fromY;
            fromY = temp;
            temp = toX;
            toX = toY;
            toY = temp;
        }
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        int error = (-dx) >> 1;
        int ystep = fromY < toY ? 1 : -1;
        if (fromX >= toX) {
            xstep = -1;
        }
        int transitions = 0;
        boolean inBlack = detector.image.get(steep ? fromY : fromX, steep ? fromX : fromY);
        boolean x = fromX;
        int error2 = error;
        error = fromY;
        while (x != toX) {
            int fromX2;
            BitMatrix bitMatrix = detector.image;
            boolean isBlack = steep ? error : x;
            if (steep) {
                fromX2 = fromX;
                fromX = x;
            } else {
                fromX2 = fromX;
                fromX = error;
            }
            isBlack = bitMatrix.get(isBlack, fromX);
            if (isBlack != inBlack) {
                transitions++;
                inBlack = isBlack;
            }
            error2 += dy;
            if (error2 > 0) {
                if (error == toY) {
                    break;
                }
                error += ystep;
                error2 -= dx;
            }
            x += xstep;
            fromX = fromX2;
            detector = this;
        }
        return new ResultPointsAndTransitions(from, to, transitions);
    }
}
