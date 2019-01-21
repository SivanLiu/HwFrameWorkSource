package android.support.v4.graphics;

import android.graphics.Path;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import com.huawei.nearbysdk.closeRange.CloseRangeConstant;
import java.util.ArrayList;

@RestrictTo({Scope.LIBRARY_GROUP})
public class PathParser {
    private static final String LOGTAG = "PathParser";

    private static class ExtractFloatResult {
        int mEndPosition;
        boolean mEndWithNegOrDot;

        ExtractFloatResult() {
        }
    }

    public static class PathDataNode {
        @RestrictTo({Scope.LIBRARY_GROUP})
        public float[] mParams;
        @RestrictTo({Scope.LIBRARY_GROUP})
        public char mType;

        PathDataNode(char type, float[] params) {
            this.mType = type;
            this.mParams = params;
        }

        PathDataNode(PathDataNode n) {
            this.mType = n.mType;
            this.mParams = PathParser.copyOfRange(n.mParams, 0, n.mParams.length);
        }

        public static void nodesToPath(PathDataNode[] node, Path path) {
            float[] current = new float[6];
            char previousCommand = 'm';
            for (int i = 0; i < node.length; i++) {
                addCommand(path, current, previousCommand, node[i].mType, node[i].mParams);
                previousCommand = node[i].mType;
            }
        }

        public void interpolatePathDataNode(PathDataNode nodeFrom, PathDataNode nodeTo, float fraction) {
            for (int i = 0; i < nodeFrom.mParams.length; i++) {
                this.mParams[i] = (nodeFrom.mParams[i] * (1.0f - fraction)) + (nodeTo.mParams[i] * fraction);
            }
        }

        /* JADX WARNING: Missing block: B:16:0x006d, code skipped:
            r27 = r6;
     */
        /* JADX WARNING: Missing block: B:42:0x019c, code skipped:
            r21 = r0;
            r22 = r1;
     */
        /* JADX WARNING: Missing block: B:43:0x01a0, code skipped:
            r14 = r7;
     */
        /* JADX WARNING: Missing block: B:79:0x033f, code skipped:
            r21 = r0;
            r22 = r1;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private static void addCommand(Path path, float[] current, char previousCmd, char cmd, float[] val) {
            Path path2 = path;
            float[] fArr = val;
            int incr = 2;
            boolean z = false;
            float currentX = current[0];
            float currentY = current[1];
            float ctrlPointX = current[2];
            float ctrlPointY = current[3];
            float currentSegmentStartX = current[4];
            float currentSegmentStartY = current[5];
            switch (cmd) {
                case 'A':
                case 'a':
                    incr = 7;
                    break;
                case 'C':
                case 'c':
                    incr = 6;
                    break;
                case 'H':
                case 'V':
                case CloseRangeConstant.CALLBACK_ONDEVICE /*104*/:
                case 'v':
                    incr = 1;
                    break;
                case 'L':
                case 'M':
                case 'T':
                case 'l':
                case 'm':
                case 't':
                    incr = 2;
                    break;
                case 'Q':
                case 'S':
                case 'q':
                case 's':
                    incr = 4;
                    break;
                case 'Z':
                case 'z':
                    path.close();
                    currentX = currentSegmentStartX;
                    currentY = currentSegmentStartY;
                    ctrlPointX = currentSegmentStartX;
                    ctrlPointY = currentSegmentStartY;
                    path2.moveTo(currentX, currentY);
                    break;
            }
            int incr2 = incr;
            char previousCmd2 = previousCmd;
            float currentX2 = currentX;
            float currentY2 = currentY;
            float ctrlPointX2 = ctrlPointX;
            float ctrlPointY2 = ctrlPointY;
            float currentSegmentStartX2 = currentSegmentStartX;
            float currentSegmentStartY2 = currentSegmentStartY;
            incr = 0;
            while (true) {
                int k;
                int k2 = incr;
                char c;
                if (k2 < fArr.length) {
                    float ctrlPointX3;
                    float f;
                    float f2;
                    float reflectiveCtrlPointY;
                    switch (cmd) {
                        case 'A':
                            k = k2;
                            c = previousCmd2;
                            drawArc(path2, currentX2, currentY2, fArr[k + 5], fArr[k + 6], fArr[k + 0], fArr[k + 1], fArr[k + 2], fArr[k + 3] != 0.0f, fArr[k + 4] != 0.0f);
                            currentX2 = fArr[k + 5];
                            currentY2 = fArr[k + 6];
                            ctrlPointX3 = currentX2;
                            currentX = currentY2;
                            break;
                        case 'C':
                            k = k2;
                            c = previousCmd2;
                            f = currentY2;
                            f2 = currentX2;
                            path2.cubicTo(fArr[k + 0], fArr[k + 1], fArr[k + 2], fArr[k + 3], fArr[k + 4], fArr[k + 5]);
                            currentX2 = fArr[k + 4];
                            currentY2 = fArr[k + 5];
                            ctrlPointX3 = fArr[k + 2];
                            currentX = fArr[k + 3];
                            break;
                        case 'H':
                            k = k2;
                            c = previousCmd2;
                            f2 = currentX2;
                            path2.lineTo(fArr[k + 0], currentY2);
                            currentX2 = fArr[k + 0];
                            break;
                        case 'L':
                            k = k2;
                            c = previousCmd2;
                            f = currentY2;
                            f2 = currentX2;
                            path2.lineTo(fArr[k + 0], fArr[k + 1]);
                            currentX2 = fArr[k + 0];
                            currentY2 = fArr[k + 1];
                            break;
                        case 'M':
                            k = k2;
                            c = previousCmd2;
                            f = currentY2;
                            f2 = currentX2;
                            currentX2 = fArr[k + 0];
                            currentY2 = fArr[k + 1];
                            if (k <= 0) {
                                path2.moveTo(fArr[k + 0], fArr[k + 1]);
                                currentSegmentStartX2 = currentX2;
                                currentSegmentStartY2 = currentY2;
                                break;
                            }
                            path2.lineTo(fArr[k + 0], fArr[k + 1]);
                            break;
                        case 'Q':
                            k = k2;
                            c = previousCmd2;
                            f = currentY2;
                            f2 = currentX2;
                            path2.quadTo(fArr[k + 0], fArr[k + 1], fArr[k + 2], fArr[k + 3]);
                            ctrlPointX3 = fArr[k + 0];
                            currentX = fArr[k + 1];
                            currentX2 = fArr[k + 2];
                            currentY2 = fArr[k + 3];
                            break;
                        case 'S':
                            k = k2;
                            c = previousCmd2;
                            f = currentY2;
                            f2 = currentX2;
                            currentY = f2;
                            ctrlPointX = f;
                            if (c == 'c' || c == 's' || c == 'C' || c == 'S') {
                                currentX2 = (2.0f * f2) - ctrlPointX2;
                                reflectiveCtrlPointY = (2.0f * f) - ctrlPointY2;
                            } else {
                                currentX2 = currentY;
                                reflectiveCtrlPointY = ctrlPointX;
                            }
                            path2.cubicTo(currentX2, reflectiveCtrlPointY, fArr[k + 0], fArr[k + 1], fArr[k + 2], fArr[k + 3]);
                            ctrlPointX3 = fArr[k + 0];
                            currentX = fArr[k + 1];
                            currentY = fArr[k + 2];
                            currentY2 = fArr[k + 3];
                            ctrlPointX2 = ctrlPointX3;
                            ctrlPointY2 = currentX;
                            currentX2 = currentY;
                            break;
                        case 'T':
                            k = k2;
                            c = previousCmd2;
                            f = currentY2;
                            f2 = currentX2;
                            ctrlPointX3 = f2;
                            currentX = f;
                            if (c == 'q' || c == 't' || c == 'Q' || c == 'T') {
                                ctrlPointX3 = (2.0f * f2) - ctrlPointX2;
                                currentX = (2.0f * f) - ctrlPointY2;
                            }
                            path2.quadTo(ctrlPointX3, currentX, fArr[k + 0], fArr[k + 1]);
                            currentY = ctrlPointX3;
                            ctrlPointX = currentX;
                            currentX2 = fArr[k + 0];
                            currentY2 = fArr[k + 1];
                            ctrlPointX2 = currentY;
                            ctrlPointY2 = ctrlPointX;
                            break;
                        case 'V':
                            k = k2;
                            c = previousCmd2;
                            f = currentY2;
                            path2.lineTo(currentX2, fArr[k + 0]);
                            currentY2 = fArr[k + 0];
                            break;
                        case 'a':
                            k = k2;
                            ctrlPointX = fArr[k + 5] + currentX2;
                            ctrlPointY = fArr[k + 6] + currentY2;
                            currentSegmentStartX = fArr[k + 0];
                            currentSegmentStartY = fArr[k + 1];
                            reflectiveCtrlPointY = fArr[k + 2];
                            boolean z2 = fArr[k + 3] != 0.0f ? true : z;
                            int i = fArr[k + 4] != 0.0f ? 1 : z;
                            c = previousCmd2;
                            f = currentY2;
                            f2 = currentX2;
                            drawArc(path2, currentX2, currentY2, ctrlPointX, ctrlPointY, currentSegmentStartX, currentSegmentStartY, reflectiveCtrlPointY, z2, i);
                            currentX2 = f2 + fArr[k + 5];
                            currentY2 = f + fArr[k + 6];
                            ctrlPointX3 = currentX2;
                            currentX = currentY2;
                            break;
                        case 'c':
                            k = k2;
                            path2.rCubicTo(fArr[k + 0], fArr[k + 1], fArr[k + 2], fArr[k + 3], fArr[k + 4], fArr[k + 5]);
                            ctrlPointX3 = fArr[k + 2] + currentX2;
                            currentX = fArr[k + 3] + currentY2;
                            currentX2 += fArr[k + 4];
                            currentY2 += fArr[k + 5];
                            break;
                        case CloseRangeConstant.CALLBACK_ONDEVICE /*104*/:
                            k = k2;
                            path2.rLineTo(fArr[k + 0], 0.0f);
                            currentX2 += fArr[k + 0];
                            break;
                        case 'l':
                            k = k2;
                            path2.rLineTo(fArr[k + 0], fArr[k + 1]);
                            currentX2 += fArr[k + 0];
                            currentY2 += fArr[k + 1];
                            break;
                        case 'm':
                            k = k2;
                            currentX2 += fArr[k + 0];
                            currentY2 += fArr[k + 1];
                            if (k <= 0) {
                                path2.rMoveTo(fArr[k + 0], fArr[k + 1]);
                                currentSegmentStartX2 = currentX2;
                                currentSegmentStartY2 = currentY2;
                                break;
                            }
                            path2.rLineTo(fArr[k + 0], fArr[k + 1]);
                            break;
                        case 'q':
                            k = k2;
                            path2.rQuadTo(fArr[k + 0], fArr[k + 1], fArr[k + 2], fArr[k + 3]);
                            ctrlPointX3 = fArr[k + 0] + currentX2;
                            currentX = fArr[k + 1] + currentY2;
                            currentX2 += fArr[k + 2];
                            currentY2 += fArr[k + 3];
                            break;
                        case 's':
                            float reflectiveCtrlPointX;
                            if (previousCmd2 == 'c' || previousCmd2 == 's' || previousCmd2 == 'C' || previousCmd2 == 'S') {
                                reflectiveCtrlPointX = currentX2 - ctrlPointX2;
                                reflectiveCtrlPointY = currentY2 - ctrlPointY2;
                            } else {
                                reflectiveCtrlPointX = 0.0f;
                                reflectiveCtrlPointY = 0.0f;
                            }
                            k = k2;
                            path2.rCubicTo(reflectiveCtrlPointX, reflectiveCtrlPointY, fArr[k2 + 0], fArr[k2 + 1], fArr[k2 + 2], fArr[k2 + 3]);
                            ctrlPointX3 = fArr[k + 0] + currentX2;
                            currentX = fArr[k + 1] + currentY2;
                            currentX2 += fArr[k + 2];
                            currentY2 += fArr[k + 3];
                            break;
                        case 't':
                            ctrlPointX3 = 0.0f;
                            currentX = 0.0f;
                            if (previousCmd2 == 'q' || previousCmd2 == 't' || previousCmd2 == 'Q' || previousCmd2 == 'T') {
                                ctrlPointX3 = currentX2 - ctrlPointX2;
                                currentX = currentY2 - ctrlPointY2;
                            }
                            path2.rQuadTo(ctrlPointX3, currentX, fArr[k2 + 0], fArr[k2 + 1]);
                            currentY = currentX2 + ctrlPointX3;
                            ctrlPointX = currentY2 + currentX;
                            currentX2 += fArr[k2 + 0];
                            currentY2 += fArr[k2 + 1];
                            ctrlPointX2 = currentY;
                            ctrlPointY2 = ctrlPointX;
                            break;
                        case 'v':
                            path2.rLineTo(0.0f, fArr[k2 + 0]);
                            currentY2 += fArr[k2 + 0];
                            break;
                        default:
                            k = k2;
                            break;
                    }
                }
                c = previousCmd2;
                currentY = currentY2;
                current[0] = currentX2;
                current[1] = currentY;
                current[2] = ctrlPointX2;
                current[3] = ctrlPointY2;
                current[4] = currentSegmentStartX2;
                current[5] = currentSegmentStartY2;
                return;
                previousCmd2 = cmd;
                incr = k + incr2;
                z = false;
            }
        }

        private static void drawArc(Path p, float x0, float y0, float x1, float y1, float a, float b, float theta, boolean isMoreThanHalf, boolean isPositiveArc) {
            float f = x0;
            float f2 = y0;
            float f3 = x1;
            float f4 = y1;
            float f5 = a;
            float f6 = b;
            boolean z = isPositiveArc;
            float f7 = theta;
            double thetaD = Math.toRadians((double) f7);
            double cosTheta = Math.cos(thetaD);
            double sinTheta = Math.sin(thetaD);
            double x0p = ((((double) f) * cosTheta) + (((double) f2) * sinTheta)) / ((double) f5);
            double y0p = ((((double) (-f)) * sinTheta) + (((double) f2) * cosTheta)) / ((double) f6);
            double x1p = ((((double) f3) * cosTheta) + (((double) f4) * sinTheta)) / ((double) f5);
            double y1p = ((((double) (-f3)) * sinTheta) + (((double) f4) * cosTheta)) / ((double) f6);
            double dx = x0p - x1p;
            double dy = y0p - y1p;
            double xm = (x0p + x1p) / 2.0d;
            double ym = (y0p + y1p) / 2.0d;
            double dsq = (dx * dx) + (dy * dy);
            if (dsq == 0.0d) {
                Log.w(PathParser.LOGTAG, " Points are coincident");
                return;
            }
            double disc = (1.0d / dsq) - 0.25d;
            double disc2;
            float adjust;
            if (disc < 0.0d) {
                String str = PathParser.LOGTAG;
                StringBuilder stringBuilder = new StringBuilder();
                disc2 = disc;
                stringBuilder.append("Points are too far apart ");
                stringBuilder.append(dsq);
                Log.w(str, stringBuilder.toString());
                adjust = (float) (Math.sqrt(dsq) / 1.99999d);
                drawArc(p, f, f2, f3, f4, f5 * adjust, f6 * adjust, f7, isMoreThanHalf, isPositiveArc);
                return;
            }
            double cx;
            double cy;
            disc2 = dsq;
            double thetaD2 = thetaD;
            disc = Math.sqrt(disc);
            dsq = disc * dx;
            thetaD = disc * dy;
            z = isPositiveArc;
            if (isMoreThanHalf == z) {
                cx = xm - thetaD;
                cy = ym + dsq;
            } else {
                cx = xm + thetaD;
                cy = ym - dsq;
            }
            double eta0 = Math.atan2(y0p - cy, x0p - cx);
            disc = Math.atan2(y1p - cy, x1p - cx);
            dsq = disc - eta0;
            if (z != (dsq >= 0.0d ? 1 : null)) {
                if (dsq > 0.0d) {
                    dsq -= 6.283185307179586d;
                } else {
                    dsq += 6.283185307179586d;
                }
            }
            float f8 = a;
            cx *= (double) f8;
            adjust = b;
            cy *= (double) adjust;
            thetaD = cx;
            arcToBezier(p, (cx * cosTheta) - (cy * sinTheta), (thetaD * sinTheta) + (cy * cosTheta), (double) f8, (double) adjust, (double) f, (double) f2, thetaD2, eta0, dsq);
        }

        private static void arcToBezier(Path p, double cx, double cy, double a, double b, double e1x, double e1y, double theta, double start, double sweep) {
            double eta1 = a;
            int numSegments = (int) Math.ceil(Math.abs((sweep * 4.0d) / 3.141592653589793d));
            double eta12 = start;
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            double cosEta1 = Math.cos(eta12);
            double sinEta1 = Math.sin(eta12);
            double anglePerSegment = sweep / ((double) numSegments);
            int i = 0;
            double ep1y = (((-eta1) * sinTheta) * sinEta1) + ((b * cosTheta) * cosEta1);
            double e1y2 = e1y;
            double ep1x = (((-eta1) * cosTheta) * sinEta1) - ((b * sinTheta) * cosEta1);
            double e1x2 = e1x;
            while (true) {
                int i2 = i;
                int numSegments2;
                double cosTheta2;
                double sinTheta2;
                if (i2 < numSegments) {
                    int i3 = i2;
                    double eta2 = eta12 + anglePerSegment;
                    double sinEta2 = Math.sin(eta2);
                    double cosEta2 = Math.cos(eta2);
                    double anglePerSegment2 = anglePerSegment;
                    double e2x = (cx + ((eta1 * cosTheta) * cosEta2)) - ((b * sinTheta) * sinEta2);
                    double e2y = (cy + ((eta1 * sinTheta) * cosEta2)) + ((b * cosTheta) * sinEta2);
                    double ep2x = (((-eta1) * cosTheta) * sinEta2) - ((b * sinTheta) * cosEta2);
                    anglePerSegment = (((-eta1) * sinTheta) * sinEta2) + ((b * cosTheta) * cosEta2);
                    eta1 = Math.tan((eta2 - eta12) / 2.0d);
                    double alpha = (Math.sin(eta2 - eta12) * (Math.sqrt(4.0d + ((3.0d * eta1) * eta1)) - 1.0d)) / 3.0d;
                    eta1 = e1x2 + (alpha * ep1x);
                    numSegments2 = numSegments;
                    double q1y = e1y2 + (alpha * ep1y);
                    cosTheta2 = cosTheta;
                    double q2x = e2x - (alpha * ep2x);
                    alpha = e2y - (alpha * anglePerSegment);
                    sinTheta2 = sinTheta;
                    Path path = p;
                    path.rLineTo(0.0f, 0.0f);
                    float f = (float) alpha;
                    alpha = e2x;
                    eta12 = e2y;
                    path.cubicTo((float) eta1, (float) q1y, (float) q2x, f, (float) alpha, (float) eta12);
                    e1x2 = alpha;
                    e1y2 = eta12;
                    ep1x = ep2x;
                    ep1y = anglePerSegment;
                    i = i3 + 1;
                    eta12 = eta2;
                    anglePerSegment = anglePerSegment2;
                    numSegments = numSegments2;
                    cosTheta = cosTheta2;
                    sinTheta = sinTheta2;
                    eta1 = a;
                } else {
                    numSegments2 = numSegments;
                    double d = eta12;
                    cosTheta2 = cosTheta;
                    sinTheta2 = sinTheta;
                    sinTheta = p;
                    return;
                }
            }
        }
    }

    static float[] copyOfRange(float[] original, int start, int end) {
        if (start <= end) {
            int originalLength = original.length;
            if (start < 0 || start > originalLength) {
                throw new ArrayIndexOutOfBoundsException();
            }
            int resultLength = end - start;
            float[] result = new float[resultLength];
            System.arraycopy(original, start, result, 0, Math.min(resultLength, originalLength - start));
            return result;
        }
        throw new IllegalArgumentException();
    }

    public static Path createPathFromPathData(String pathData) {
        Path path = new Path();
        PathDataNode[] nodes = createNodesFromPathData(pathData);
        if (nodes == null) {
            return null;
        }
        try {
            PathDataNode.nodesToPath(nodes, path);
            return path;
        } catch (RuntimeException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error in parsing ");
            stringBuilder.append(pathData);
            throw new RuntimeException(stringBuilder.toString(), e);
        }
    }

    public static PathDataNode[] createNodesFromPathData(String pathData) {
        if (pathData == null) {
            return null;
        }
        int start = 0;
        int end = 1;
        ArrayList<PathDataNode> list = new ArrayList();
        while (end < pathData.length()) {
            end = nextStart(pathData, end);
            String s = pathData.substring(start, end).trim();
            if (s.length() > 0) {
                addNode(list, s.charAt(0), getFloats(s));
            }
            start = end;
            end++;
        }
        if (end - start == 1 && start < pathData.length()) {
            addNode(list, pathData.charAt(start), new float[0]);
        }
        return (PathDataNode[]) list.toArray(new PathDataNode[list.size()]);
    }

    public static PathDataNode[] deepCopyNodes(PathDataNode[] source) {
        if (source == null) {
            return null;
        }
        PathDataNode[] copy = new PathDataNode[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = new PathDataNode(source[i]);
        }
        return copy;
    }

    /* JADX WARNING: Missing block: B:17:0x002c, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean canMorph(PathDataNode[] nodesFrom, PathDataNode[] nodesTo) {
        if (nodesFrom == null || nodesTo == null || nodesFrom.length != nodesTo.length) {
            return false;
        }
        int i = 0;
        while (i < nodesFrom.length) {
            if (nodesFrom[i].mType != nodesTo[i].mType || nodesFrom[i].mParams.length != nodesTo[i].mParams.length) {
                return false;
            }
            i++;
        }
        return true;
    }

    public static void updateNodes(PathDataNode[] target, PathDataNode[] source) {
        for (int i = 0; i < source.length; i++) {
            target[i].mType = source[i].mType;
            for (int j = 0; j < source[i].mParams.length; j++) {
                target[i].mParams[j] = source[i].mParams[j];
            }
        }
    }

    private static int nextStart(String s, int end) {
        while (end < s.length()) {
            char c = s.charAt(end);
            if (((c - 65) * (c - 90) <= 0 || (c - 97) * (c - 122) <= 0) && c != 'e' && c != 'E') {
                return end;
            }
            end++;
        }
        return end;
    }

    private static void addNode(ArrayList<PathDataNode> list, char cmd, float[] val) {
        list.add(new PathDataNode(cmd, val));
    }

    private static float[] getFloats(String s) {
        if (s.charAt(0) == 'z' || s.charAt(0) == 'Z') {
            return new float[0];
        }
        try {
            float[] results = new float[s.length()];
            int count = 0;
            int startPosition = 1;
            ExtractFloatResult result = new ExtractFloatResult();
            int totalLength = s.length();
            while (startPosition < totalLength) {
                extract(s, startPosition, result);
                int endPosition = result.mEndPosition;
                if (startPosition < endPosition) {
                    int count2 = count + 1;
                    results[count] = Float.parseFloat(s.substring(startPosition, endPosition));
                    count = count2;
                }
                if (result.mEndWithNegOrDot) {
                    startPosition = endPosition;
                } else {
                    startPosition = endPosition + 1;
                }
            }
            return copyOfRange(results, 0, count);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error in parsing \"");
            stringBuilder.append(s);
            stringBuilder.append("\"");
            throw new RuntimeException(stringBuilder.toString(), e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x003b A:{LOOP_END, LOOP:0: B:1:0x0007->B:20:0x003b} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x003e A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void extract(String s, int start, ExtractFloatResult result) {
        int currentIndex = start;
        boolean foundSeparator = false;
        boolean isExponential = false;
        result.mEndWithNegOrDot = false;
        boolean secondDot = false;
        while (currentIndex < s.length()) {
            boolean isPrevExponential = isExponential;
            isExponential = false;
            char currentChar = s.charAt(currentIndex);
            if (currentChar != ' ') {
                if (currentChar != 'E' && currentChar != 'e') {
                    switch (currentChar) {
                        case MotionEventCompat.AXIS_GENERIC_13 /*44*/:
                            break;
                        case MotionEventCompat.AXIS_GENERIC_14 /*45*/:
                            if (!(currentIndex == start || isPrevExponential)) {
                                foundSeparator = true;
                                result.mEndWithNegOrDot = true;
                                break;
                            }
                        case MotionEventCompat.AXIS_GENERIC_15 /*46*/:
                            if (!secondDot) {
                                secondDot = true;
                                break;
                            }
                            foundSeparator = true;
                            result.mEndWithNegOrDot = true;
                            break;
                    }
                }
                isExponential = true;
                if (foundSeparator) {
                    currentIndex++;
                } else {
                    result.mEndPosition = currentIndex;
                }
            }
            foundSeparator = true;
            if (foundSeparator) {
            }
        }
        result.mEndPosition = currentIndex;
    }

    private PathParser() {
    }
}
