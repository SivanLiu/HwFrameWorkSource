package android.gesture;

import android.graphics.RectF;
import android.util.Log;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public final class GestureUtils {
    private static final float NONUNIFORM_SCALE = ((float) Math.sqrt(2.0d));
    private static final float SCALING_THRESHOLD = 0.26f;

    private GestureUtils() {
    }

    static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(GestureConstants.LOG_TAG, "Could not close stream", e);
            }
        }
    }

    public static float[] spatialSampling(Gesture gesture, int bitmapSize) {
        return spatialSampling(gesture, bitmapSize, false);
    }

    public static float[] spatialSampling(Gesture gesture, int bitmapSize, boolean keepAspectRatio) {
        float scale;
        float scale2;
        int i = bitmapSize;
        float targetPatchSize = (float) (i - 1);
        float[] sample = new float[(i * i)];
        Arrays.fill(sample, 0.0f);
        RectF rect = gesture.getBoundingBox();
        float gestureWidth = rect.width();
        float gestureHeight = rect.height();
        float sx = targetPatchSize / gestureWidth;
        float sy = targetPatchSize / gestureHeight;
        if (keepAspectRatio) {
            scale = sx < sy ? sx : sy;
            sx = scale;
            sy = scale;
        } else {
            scale = gestureWidth / gestureHeight;
            if (scale > 1.0f) {
                scale = 1.0f / scale;
            }
            if (scale < SCALING_THRESHOLD) {
                scale2 = sx < sy ? sx : sy;
                sx = scale2;
                sy = scale2;
            } else if (sx > sy) {
                scale2 = NONUNIFORM_SCALE * sy;
                if (scale2 < sx) {
                    sx = scale2;
                }
            } else {
                scale2 = NONUNIFORM_SCALE * sx;
                if (scale2 < sy) {
                    sy = scale2;
                }
            }
        }
        scale = -rect.centerX();
        scale2 = -rect.centerY();
        float postDx = targetPatchSize / 2.0f;
        float postDy = targetPatchSize / 2.0f;
        ArrayList<GestureStroke> strokes = gesture.getStrokes();
        int count = strokes.size();
        int index = 0;
        while (true) {
            int index2 = index;
            RectF rect2;
            float gestureWidth2;
            float gestureHeight2;
            int count2;
            float sx2;
            float preDx;
            if (index2 < count) {
                int size;
                rect2 = rect;
                GestureStroke stroke = (GestureStroke) strokes.get(index2);
                gestureWidth2 = gestureWidth;
                float[] strokepoints = stroke.points;
                int size2 = strokepoints.length;
                gestureHeight2 = gestureHeight;
                float[] pts = new float[size2];
                index = 0;
                while (true) {
                    count2 = count;
                    count = index;
                    if (count >= size2) {
                        break;
                    }
                    pts[count] = ((strokepoints[count] + scale) * sx) + postDx;
                    pts[count + 1] = ((strokepoints[count + 1] + scale2) * sy) + postDy;
                    index = count + 2;
                    count = count2;
                }
                sx2 = sx;
                gestureWidth = -1.0f;
                count = 0;
                sx = -1.0f;
                while (count < size2) {
                    float targetPatchSize2;
                    float[] pts2;
                    float segmentStartX = pts[count] < 0.0f ? 0.0f : pts[count];
                    float segmentStartY = pts[count + 1] < 0.0f ? 0.0f : pts[count + 1];
                    if (segmentStartX > targetPatchSize) {
                        segmentStartX = targetPatchSize;
                    }
                    size = size2;
                    float segmentStartX2 = segmentStartX;
                    if (segmentStartY > targetPatchSize) {
                        segmentStartY = targetPatchSize;
                        targetPatchSize2 = targetPatchSize;
                    } else {
                        targetPatchSize2 = targetPatchSize;
                        targetPatchSize = segmentStartY;
                    }
                    plot(segmentStartX2, targetPatchSize, sample, i);
                    if (gestureWidth != -1.0f) {
                        float xpos;
                        if (gestureWidth > segmentStartX2) {
                            preDx = scale;
                            xpos = (float) Math.ceil((double) segmentStartX2);
                            scale = (sx - targetPatchSize) / (gestureWidth - segmentStartX2);
                            while (xpos < gestureWidth) {
                                pts2 = pts;
                                plot(xpos, ((xpos - segmentStartX2) * scale) + targetPatchSize, sample, i);
                                xpos += 1.0f;
                                pts = pts2;
                            }
                            pts2 = pts;
                        } else {
                            pts2 = pts;
                            preDx = scale;
                            if (gestureWidth < segmentStartX2) {
                                xpos = (sx - targetPatchSize) / (gestureWidth - segmentStartX2);
                                for (pts = (float) Math.ceil((double) gestureWidth); pts < segmentStartX2; pts += 1.0f) {
                                    plot(pts, ((pts - segmentStartX2) * xpos) + targetPatchSize, sample, i);
                                }
                            }
                        }
                        if (sx > targetPatchSize) {
                            xpos = (gestureWidth - segmentStartX2) / (sx - targetPatchSize);
                            for (pts = (float) Math.ceil((double) targetPatchSize); pts < sx; pts += 1.0f) {
                                plot(((pts - targetPatchSize) * xpos) + segmentStartX2, pts, sample, i);
                            }
                        } else if (sx < targetPatchSize) {
                            xpos = (gestureWidth - segmentStartX2) / (sx - targetPatchSize);
                            for (pts = (float) Math.ceil((double) sx); pts < targetPatchSize; pts += 1.0f) {
                                plot(((pts - targetPatchSize) * xpos) + segmentStartX2, pts, sample, i);
                            }
                        }
                    } else {
                        pts2 = pts;
                        preDx = scale;
                    }
                    gestureWidth = segmentStartX2;
                    sx = targetPatchSize;
                    count += 2;
                    size2 = size;
                    targetPatchSize = targetPatchSize2;
                    scale = preDx;
                    pts = pts2;
                }
                size = size2;
                preDx = scale;
                index = index2 + 1;
                index2 = 0;
                rect = rect2;
                gestureWidth = gestureWidth2;
                gestureHeight = gestureHeight2;
                count = count2;
                sx = sx2;
            } else {
                rect2 = rect;
                gestureWidth2 = gestureWidth;
                gestureHeight2 = gestureHeight;
                sx2 = sx;
                count2 = count;
                preDx = scale;
                return sample;
            }
        }
    }

    private static void plot(float x, float y, float[] sample, int sampleSize) {
        float y2 = 0.0f;
        float x2 = x < 0.0f ? 0.0f : x;
        if (y >= 0.0f) {
            y2 = y;
        }
        int xFloor = (int) Math.floor((double) x2);
        int xCeiling = (int) Math.ceil((double) x2);
        int yFloor = (int) Math.floor((double) y2);
        int yCeiling = (int) Math.ceil((double) y2);
        int i;
        int i2;
        if (x2 == ((float) xFloor) && y2 == ((float) yFloor)) {
            int index = (yCeiling * sampleSize) + xCeiling;
            if (sample[index] < 1.0f) {
                sample[index] = 1.0f;
            }
            float f = y2;
            float f2 = x2;
            i = xFloor;
            i2 = xCeiling;
            return;
        }
        double xFloorSq = Math.pow((double) (((float) xFloor) - x2), 2.0d);
        double yFloorSq = Math.pow((double) (((float) yFloor) - y2), 2.0d);
        double xCeilingSq = Math.pow((double) (((float) xCeiling) - x2), 2.0d);
        double yCeilingSq = Math.pow((double) (((float) yCeiling) - y2), 2.0d);
        float topLeft = (float) Math.sqrt(xFloorSq + yFloorSq);
        i = xFloor;
        i2 = xCeiling;
        float topRight = (float) Math.sqrt(xCeilingSq + yFloorSq);
        float btmLeft = (float) Math.sqrt(xFloorSq + yCeilingSq);
        float btmRight = (float) Math.sqrt(xCeilingSq + yCeilingSq);
        float sum = ((topLeft + topRight) + btmLeft) + btmRight;
        float value = topLeft / sum;
        int index2 = (yFloor * sampleSize) + i;
        if (value > sample[index2]) {
            sample[index2] = value;
        }
        value = topRight / sum;
        int index3 = (yFloor * sampleSize) + i2;
        if (value > sample[index3]) {
            sample[index3] = value;
        }
        value = btmLeft / sum;
        index2 = (yCeiling * sampleSize) + i;
        if (value > sample[index2]) {
            sample[index2] = value;
        }
        value = btmRight / sum;
        index3 = (yCeiling * sampleSize) + i2;
        if (value > sample[index3]) {
            sample[index3] = value;
        }
    }

    public static float[] temporalSampling(GestureStroke stroke, int numPoints) {
        GestureStroke gestureStroke = stroke;
        float increment = gestureStroke.length / ((float) (numPoints - 1));
        int vectorLength = numPoints * 2;
        float[] vector = new float[vectorLength];
        float distanceSoFar = 0.0f;
        float[] pts = gestureStroke.points;
        float lstPointX = pts[0];
        int i = 1;
        float lstPointY = pts[1];
        float currentPointX = Float.MIN_VALUE;
        float currentPointY = Float.MIN_VALUE;
        vector[0] = lstPointX;
        int index = 0 + 1;
        vector[index] = lstPointY;
        index++;
        int i2 = 0;
        int count = pts.length / 2;
        while (i2 < count) {
            int i3;
            int i4;
            if (currentPointX == Float.MIN_VALUE) {
                i2++;
                if (i2 >= count) {
                    i3 = count;
                    break;
                }
                currentPointX = pts[i2 * 2];
                currentPointY = pts[(i2 * 2) + i];
            }
            float deltaX = currentPointX - lstPointX;
            float deltaY = currentPointY - lstPointY;
            int i5 = i2;
            i3 = count;
            float distance = (float) Math.hypot((double) deltaX, (double) deltaY);
            float ratio;
            if (distanceSoFar + distance >= increment) {
                ratio = (increment - distanceSoFar) / distance;
                float nx = (ratio * deltaX) + lstPointX;
                count = (ratio * deltaY) + lstPointY;
                vector[index] = nx;
                index++;
                vector[index] = count;
                i4 = 1;
                index++;
                lstPointX = nx;
                lstPointY = count;
                distanceSoFar = 0.0f;
            } else {
                i4 = 1;
                ratio = currentPointX;
                lstPointX = currentPointY;
                distanceSoFar += distance;
                currentPointY = Float.MIN_VALUE;
                currentPointX = Float.MIN_VALUE;
                lstPointY = lstPointX;
                lstPointX = ratio;
            }
            i = i4;
            count = i3;
            i2 = i5;
            gestureStroke = stroke;
        }
        for (int i6 = index; i6 < vectorLength; i6 += 2) {
            vector[i6] = lstPointX;
            vector[i6 + 1] = lstPointY;
        }
        return vector;
    }

    static float[] computeCentroid(float[] points) {
        float centerY = 0.0f;
        float centerX = 0.0f;
        int i = 0;
        while (i < points.length) {
            centerX += points[i];
            i++;
            centerY += points[i];
            i++;
        }
        return new float[]{(2.0f * centerX) / ((float) points.length), (2.0f * centerY) / ((float) points.length)};
    }

    private static float[][] computeCoVariance(float[] points) {
        float[][] array = (float[][]) Array.newInstance(float.class, new int[]{2, 2});
        array[0][0] = 0.0f;
        array[0][1] = 0.0f;
        array[1][0] = 0.0f;
        array[1][1] = 0.0f;
        int count = points.length;
        int i = 0;
        while (i < count) {
            float x = points[i];
            i++;
            float y = points[i];
            float[] fArr = array[0];
            fArr[0] = fArr[0] + (x * x);
            fArr = array[0];
            fArr[1] = fArr[1] + (x * y);
            array[1][0] = array[0][1];
            fArr = array[1];
            fArr[1] = fArr[1] + (y * y);
            i++;
        }
        float[] fArr2 = array[0];
        fArr2[0] = fArr2[0] / ((float) (count / 2));
        fArr2 = array[0];
        fArr2[1] = fArr2[1] / ((float) (count / 2));
        fArr2 = array[1];
        fArr2[0] = fArr2[0] / ((float) (count / 2));
        float[] fArr3 = array[1];
        fArr3[1] = fArr3[1] / ((float) (count / 2));
        return array;
    }

    static float computeTotalLength(float[] points) {
        float sum = 0.0f;
        for (int i = 0; i < points.length - 4; i += 2) {
            sum = (float) (((double) sum) + Math.hypot((double) (points[i + 2] - points[i]), (double) (points[i + 3] - points[i + 1])));
        }
        return sum;
    }

    static float computeStraightness(float[] points) {
        return ((float) Math.hypot((double) (points[2] - points[0]), (double) (points[3] - points[1]))) / computeTotalLength(points);
    }

    static float computeStraightness(float[] points, float totalLen) {
        return ((float) Math.hypot((double) (points[2] - points[0]), (double) (points[3] - points[1]))) / totalLen;
    }

    static float squaredEuclideanDistance(float[] vector1, float[] vector2) {
        float squaredDistance = 0.0f;
        int size = vector1.length;
        for (int i = 0; i < size; i++) {
            float difference = vector1[i] - vector2[i];
            squaredDistance += difference * difference;
        }
        return squaredDistance / ((float) size);
    }

    static float cosineDistance(float[] vector1, float[] vector2) {
        float sum = 0.0f;
        for (int i = 0; i < vector1.length; i++) {
            sum += vector1[i] * vector2[i];
        }
        return (float) Math.acos((double) sum);
    }

    static float minimumCosineDistance(float[] vector1, float[] vector2, int numOrientations) {
        float[] fArr = vector1;
        int i = numOrientations;
        float a = 0.0f;
        float b = 0.0f;
        for (int i2 = 0; i2 < fArr.length; i2 += 2) {
            a += (fArr[i2] * vector2[i2]) + (fArr[i2 + 1] * vector2[i2 + 1]);
            b += (fArr[i2] * vector2[i2 + 1]) - (fArr[i2 + 1] * vector2[i2]);
        }
        if (a == 0.0f) {
            return 1.5707964f;
        }
        float tan = b / a;
        double angle = Math.atan((double) tan);
        if (i > 2 && Math.abs(angle) >= 3.141592653589793d / ((double) i)) {
            return (float) Math.acos((double) a);
        }
        double cosine = Math.cos(angle);
        return (float) Math.acos((((double) a) * cosine) + (((double) b) * (((double) tan) * cosine)));
    }

    public static OrientedBoundingBox computeOrientedBoundingBox(ArrayList<GesturePoint> originalPoints) {
        int count = originalPoints.size();
        float[] points = new float[(count * 2)];
        for (int i = 0; i < count; i++) {
            GesturePoint point = (GesturePoint) originalPoints.get(i);
            int index = i * 2;
            points[index] = point.x;
            points[index + 1] = point.y;
        }
        return computeOrientedBoundingBox(points, computeCentroid(points));
    }

    public static OrientedBoundingBox computeOrientedBoundingBox(float[] originalPoints) {
        int size = originalPoints.length;
        float[] points = new float[size];
        for (int i = 0; i < size; i++) {
            points[i] = originalPoints[i];
        }
        return computeOrientedBoundingBox(points, computeCentroid(points));
    }

    private static OrientedBoundingBox computeOrientedBoundingBox(float[] points, float[] centroid) {
        float angle;
        float[] fArr = points;
        translate(fArr, -centroid[0], -centroid[1]);
        float[] targetVector = computeOrientation(computeCoVariance(points));
        if (targetVector[0] == 0.0f && targetVector[1] == 0.0f) {
            angle = -1.5707964f;
        } else {
            angle = (float) Math.atan2((double) targetVector[1], (double) targetVector[0]);
            rotate(fArr, -angle);
        }
        float maxx = Float.MIN_VALUE;
        float maxy = Float.MIN_VALUE;
        int count = fArr.length;
        float miny = Float.MAX_VALUE;
        float minx = Float.MAX_VALUE;
        int i = 0;
        while (i < count) {
            if (fArr[i] < minx) {
                minx = fArr[i];
            }
            if (fArr[i] > maxx) {
                maxx = fArr[i];
            }
            i++;
            if (fArr[i] < miny) {
                miny = fArr[i];
            }
            if (fArr[i] > maxy) {
                maxy = fArr[i];
            }
            i++;
        }
        return new OrientedBoundingBox((float) (((double) (180.0f * angle)) / 3.141592653589793d), centroid[0], centroid[1], maxx - minx, maxy - miny);
    }

    private static float[] computeOrientation(float[][] covarianceMatrix) {
        float[] targetVector = new float[2];
        if (covarianceMatrix[0][1] == 0.0f || covarianceMatrix[1][0] == 0.0f) {
            targetVector[0] = 1.0f;
            targetVector[1] = 0.0f;
        }
        float value = ((-covarianceMatrix[0][0]) - covarianceMatrix[1][1]) / 2.0f;
        float rightside = (float) Math.sqrt(Math.pow((double) value, 2.0d) - ((double) ((covarianceMatrix[0][0] * covarianceMatrix[1][1]) - (covarianceMatrix[0][1] * covarianceMatrix[1][0]))));
        float lambda1 = (-value) + rightside;
        float lambda2 = (-value) - rightside;
        if (lambda1 == lambda2) {
            targetVector[0] = 0.0f;
            targetVector[1] = 0.0f;
        } else {
            float lambda = lambda1 > lambda2 ? lambda1 : lambda2;
            targetVector[0] = 1.0f;
            targetVector[1] = (lambda - covarianceMatrix[0][0]) / covarianceMatrix[0][1];
        }
        return targetVector;
    }

    static float[] rotate(float[] points, float angle) {
        float cos = (float) Math.cos((double) angle);
        float sin = (float) Math.sin((double) angle);
        int size = points.length;
        for (int i = 0; i < size; i += 2) {
            float y = (points[i] * sin) + (points[i + 1] * cos);
            points[i] = (points[i] * cos) - (points[i + 1] * sin);
            points[i + 1] = y;
        }
        return points;
    }

    static float[] translate(float[] points, float dx, float dy) {
        int size = points.length;
        for (int i = 0; i < size; i += 2) {
            points[i] = points[i] + dx;
            int i2 = i + 1;
            points[i2] = points[i2] + dy;
        }
        return points;
    }

    static float[] scale(float[] points, float sx, float sy) {
        int size = points.length;
        for (int i = 0; i < size; i += 2) {
            points[i] = points[i] * sx;
            int i2 = i + 1;
            points[i2] = points[i2] * sy;
        }
        return points;
    }
}
