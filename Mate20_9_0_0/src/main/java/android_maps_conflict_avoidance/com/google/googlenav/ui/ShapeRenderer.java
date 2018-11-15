package android_maps_conflict_avoidance.com.google.googlenav.ui;

import android_maps_conflict_avoidance.com.google.map.MapPoint;
import android_maps_conflict_avoidance.com.google.map.Zoom;

public class ShapeRenderer {
    private Zoom pixelZoom;
    private long[][][] polyBoundaryPixelXY;
    private final RenderableShape[] shapes;

    public interface Painter {
        void addLineSegment(int[] iArr, int[] iArr2, boolean z);

        void endLine();

        void paintEllipse(int i, int i2, int i3, int i4, int i5, int i6, int i7);

        void paintPolygon(long[][] jArr, int i, int i2, int i3);

        void startLine(int i, int i2, int i3);
    }

    public int getImageVersion() {
        int id = 0;
        for (int p = 0; p < this.shapes.length; p++) {
            if (!this.shapes[p].isAvailable()) {
                return 0;
            }
            id = (id * 29) + this.shapes[p].getId();
        }
        return id;
    }

    private void precalculatePixels(Zoom zoom) {
        Zoom zoom2 = zoom;
        if (zoom2 != this.pixelZoom) {
            this.polyBoundaryPixelXY = new long[this.shapes.length][][];
            int p = 0;
            int p2 = 0;
            while (p2 < this.shapes.length) {
                int p3;
                if (this.shapes[p2] instanceof RenderablePoly) {
                    RenderablePoly poly = this.shapes[p2];
                    int boundaryCount = getBoundaryCount(poly);
                    this.polyBoundaryPixelXY[p2] = new long[boundaryCount][];
                    MapPoint[][] boundaries = getBoundaries(poly);
                    int b = p;
                    while (b < boundaryCount) {
                        int point;
                        MapPoint[] boundary = boundaries[b];
                        long[] pixelXY = new long[boundary.length];
                        this.pixelZoom = zoom2;
                        pixelXY[p] = getXY(boundary[p].getXPixel(this.pixelZoom), boundary[p].getYPixel(this.pixelZoom));
                        int point2 = 1;
                        int i = 1;
                        while (i < boundary.length) {
                            int x = boundary[i].getXPixel(this.pixelZoom);
                            int y = boundary[i].getYPixel(this.pixelZoom);
                            p3 = p2;
                            if (Math.abs(x - getX(pixelXY[point2 - 1])) <= 2) {
                                point = point2;
                                if (Math.abs(y - getY(pixelXY[point2 - 1])) <= 2) {
                                    if (i != boundary.length - 1) {
                                        point2 = point;
                                        i++;
                                        p2 = p3;
                                    }
                                }
                            } else {
                                point = point2;
                            }
                            pixelXY[point] = getXY(x, y);
                            point2 = point + 1;
                            i++;
                            p2 = p3;
                        }
                        p3 = p2;
                        point = point2;
                        this.polyBoundaryPixelXY[p3][b] = new long[point2];
                        System.arraycopy(pixelXY, 0, this.polyBoundaryPixelXY[p3][b], 0, point2);
                        b++;
                        p = 0;
                        p2 = p3;
                    }
                }
                p3 = p2;
                int i2 = p;
                p2 = p3 + 1;
                p = i2;
            }
        }
    }

    private static int outcode(int width, int height, int x, int y) {
        int i = 0;
        int i2 = x < 0 ? 8 : x > width ? 4 : 0;
        if (y < 0) {
            i = 2;
        } else if (y > height) {
            i = 1;
        }
        return i | i2;
    }

    public void render(Painter painter, int x, int y, int width, int height, Zoom zoom) {
        if (getImageVersion() != 0) {
            Zoom zoom2 = zoom;
            precalculatePixels(zoom2);
            int p = 0;
            while (true) {
                int p2 = p;
                if (p2 < this.shapes.length) {
                    if (!(this.shapes[p2] instanceof RenderableEllipse)) {
                        RenderablePoly renderablePoly = (RenderablePoly) this.shapes[p2];
                        if (!this.shapes[p2].isFilled()) {
                            p = 0;
                            while (true) {
                                int b = p;
                                if (b >= this.polyBoundaryPixelXY[p2].length) {
                                    break;
                                }
                                renderLine(painter, x, y, width, height, this.polyBoundaryPixelXY[p2][b], renderablePoly, zoom2);
                                p = b + 1;
                            }
                        } else {
                            renderPolygonFill(painter, x, y, width, height, this.polyBoundaryPixelXY[p2], renderablePoly, zoom2);
                        }
                    } else {
                        renderEllipse(painter, x, y, width, height, (RenderableEllipse) this.shapes[p2], zoom2);
                    }
                    p = p2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private void renderEllipse(Painter painter, int x, int y, int screenWidth, int screenHeight, RenderableEllipse ellipse, Zoom zoom) {
        int i = screenWidth;
        int i2 = screenHeight;
        Zoom zoom2 = zoom;
        if (ellipse.getLineColor() != -1 || ellipse.getFillColor() != -1) {
            MapPoint center = ellipse.getCenter();
            int ellipseWidth = zoom2.getPixelsForDistance(ellipse.getEllipseWidth());
            int ellipseHeight = zoom2.getPixelsForDistance(ellipse.getEllipseHeight());
            int centerX = center.getXPixel(zoom2) - x;
            int centerY = center.getYPixel(zoom2) - y;
            if ((outcode(i, i2, centerX - (ellipseWidth / 2), centerY - (ellipseHeight / 2)) & outcode(i, i2, (ellipseWidth / 2) + centerX, (ellipseHeight / 2) + centerY)) == 0) {
                painter.paintEllipse(centerX, centerY, ellipseWidth, ellipseHeight, ellipse.getLineWidthForZoom(zoom), ellipse.getLineColor(), ellipse.getFillColor());
            }
        }
    }

    static void makeInRange(int endX, int endY, int startX, int startY, int[] outPoint) {
        int width = endX - startX;
        int height = endY - startY;
        if (endX > 4000 || endX < -4000) {
            if (endX > 0) {
                endX = 4000;
            } else {
                endX = -4000;
            }
            endY = startY + ((int) ((((long) (endX - startX)) * ((long) height)) / ((long) width)));
        }
        if (endY > 4000 || endY < -4000) {
            if (height + startY > 0) {
                endY = 4000;
            } else {
                endY = -4000;
            }
            endX = startX + ((int) ((((long) (endY - startY)) * ((long) width)) / ((long) height)));
        }
        outPoint[0] = endX;
        outPoint[1] = endY;
    }

    protected static boolean isInRange(int[] pointXy) {
        return pointXy[0] <= 4000 && pointXy[0] >= -4000 && pointXy[1] <= 4000 && pointXy[1] >= -4000;
    }

    private void renderLine(Painter painter, int x, int y, int width, int height, long[] pixelXY, RenderablePoly poly, Zoom zoom) {
        int lastOutcode;
        boolean skipTo;
        Painter painter2 = painter;
        int i = width;
        int i2 = height;
        long[] jArr = pixelXY;
        int[] xyDiff = new int[2];
        int[] xyDiffLast = new int[2];
        rangeAdjustedXy = new int[2];
        int i3 = 0;
        xyDiffLast[0] = getX(jArr[0]) - x;
        xyDiffLast[1] = getY(jArr[0]) - y;
        boolean lineStarted = false;
        boolean skipTo2 = true;
        int lastOutcode2 = outcode(i, i2, xyDiffLast[0], xyDiffLast[1]);
        int i4 = 1;
        while (i4 < jArr.length) {
            lastOutcode = lastOutcode2;
            int outcode = getX(jArr[i4]) - x;
            xyDiff[i3] = outcode;
            boolean skipTo3 = skipTo2;
            lastOutcode2 = getY(jArr[i4]) - y;
            xyDiff[1] = lastOutcode2;
            outcode = outcode(i, i2, outcode, lastOutcode2);
            if ((lastOutcode & outcode) == 0) {
                int i5;
                if (!lineStarted) {
                    painter2.startLine(poly.getLineColor(), poly.getLineWidthForZoom(zoom), poly.getLineStyle());
                    lineStarted = true;
                }
                boolean inRange = isInRange(xyDiff);
                if (inRange) {
                    i5 = 1;
                } else {
                    i5 = 1;
                    makeInRange(xyDiff[i3], xyDiff[1], xyDiffLast[i3], xyDiffLast[1], rangeAdjustedXy);
                }
                if (!isInRange(xyDiffLast)) {
                    makeInRange(xyDiffLast[0], xyDiffLast[i5], xyDiff[0], xyDiff[i5], xyDiffLast);
                }
                if (inRange) {
                    painter2.addLineSegment(xyDiff, xyDiffLast, skipTo3);
                } else {
                    painter2.addLineSegment(rangeAdjustedXy, xyDiffLast, skipTo3);
                }
                skipTo = inRange ^ 1;
            } else {
                skipTo = true;
            }
            skipTo2 = skipTo;
            xyDiffLast[0] = xyDiff[0];
            xyDiffLast[1] = xyDiff[1];
            lastOutcode2 = outcode;
            i4++;
            outcode = 1;
            i3 = 0;
        }
        lastOutcode = lastOutcode2;
        skipTo = skipTo2;
        if (lineStarted) {
            painter.endLine();
        }
    }

    private void renderPolygonFill(Painter painter, int x, int y, int width, int height, long[][] boundaryPixelXY, RenderablePoly poly, Zoom zoom) {
        int i = x;
        int i2 = y;
        int i3 = width;
        int i4 = height;
        boolean overlap = false;
        int boundaries = getBoundaryCount(poly);
        long[][] boundaryPixelXYOnScreen = new long[boundaries][];
        boundaryPixelXYOnScreen[0] = getPixelXYOnScreen(i, i2, boundaryPixelXY[0]);
        int lastOutcode = outcode(i3, i4, getX(boundaryPixelXYOnScreen[0][0]), getY(boundaryPixelXYOnScreen[0][0]));
        int i5 = 1;
        int cumulativeOutcode = lastOutcode;
        for (int i6 = 1; i6 < boundaryPixelXYOnScreen[0].length; i6++) {
            int outcode = outcode(i3, i4, getX(boundaryPixelXYOnScreen[0][i6]), getY(boundaryPixelXYOnScreen[0][i6]));
            if ((lastOutcode & outcode) == 0) {
                overlap = true;
                break;
            }
            cumulativeOutcode |= outcode;
            lastOutcode = outcode;
        }
        if (cumulativeOutcode == 15) {
            overlap = true;
        }
        if (overlap) {
            while (true) {
                int i7 = i5;
                if (i7 < boundaries) {
                    boundaryPixelXYOnScreen[i7] = getPixelXYOnScreen(i, i2, boundaryPixelXY[i7]);
                    i5 = i7 + 1;
                } else {
                    painter.paintPolygon(boundaryPixelXYOnScreen, poly.getLineColor(), poly.getLineWidthForZoom(zoom), poly.getFillColor());
                    return;
                }
            }
        }
        Painter painter2 = painter;
    }

    private static long[] getPixelXYOnScreen(int x, int y, long[] pixelXY) {
        long[] xy = new long[pixelXY.length];
        for (int i = 0; i < pixelXY.length; i++) {
            xy[i] = getXY(getX(pixelXY[i]) - x, getY(pixelXY[i]) - y);
        }
        return xy;
    }

    private static int getBoundaryCount(RenderablePoly poly) {
        if (poly.getInnerBoundaries() == null) {
            return 1;
        }
        return 1 + poly.getInnerBoundaries().length;
    }

    private static MapPoint[][] getBoundaries(RenderablePoly poly) {
        int i = 1;
        if (poly.getInnerBoundaries() == null) {
            return new MapPoint[][]{poly.getLine()};
        }
        MapPoint[][] boundaries = new MapPoint[getBoundaryCount(poly)][];
        boundaries[0] = poly.getLine();
        while (true) {
            int i2 = i;
            if (i2 >= boundaries.length) {
                return boundaries;
            }
            boundaries[i2] = poly.getInnerBoundaries()[i2 - 1];
            i = i2 + 1;
        }
    }

    public static long getXY(int x, int y) {
        return (((long) x) << 32) | ((((long) y) << 32) >>> 32);
    }

    public static int getX(long xy) {
        return (int) (xy >> 32);
    }

    public static int getY(long xy) {
        return (int) (4294967295L & xy);
    }
}
