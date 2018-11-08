package com.android.internal.policy;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Size;
import android.view.Gravity;
import com.android.internal.R;
import java.io.PrintWriter;
import java.util.ArrayList;

public class PipSnapAlgorithm {
    private static final float CORNER_MAGNET_THRESHOLD = 0.3f;
    private static final int SNAP_MODE_CORNERS_AND_SIDES = 1;
    private static final int SNAP_MODE_CORNERS_ONLY = 0;
    private static final int SNAP_MODE_EDGE = 2;
    private static final int SNAP_MODE_EDGE_MAGNET_CORNERS = 3;
    private static final int SNAP_MODE_LONG_EDGE_MAGNET_CORNERS = 4;
    private final Context mContext;
    private final float mDefaultSizePercent;
    private final int mDefaultSnapMode = 3;
    private final int mFlingDeceleration;
    private boolean mIsMinimized;
    private final float mMaxAspectRatioForMinSize;
    private final float mMinAspectRatioForMinSize;
    private final int mMinimizedVisibleSize;
    private int mOrientation = 0;
    private final ArrayList<Integer> mSnapGravities = new ArrayList();
    private int mSnapMode = 3;

    public PipSnapAlgorithm(Context context) {
        Resources res = context.getResources();
        this.mContext = context;
        this.mMinimizedVisibleSize = res.getDimensionPixelSize(R.dimen.pip_minimized_visible_size);
        this.mDefaultSizePercent = res.getFloat(R.dimen.config_pictureInPictureDefaultSizePercent);
        this.mMaxAspectRatioForMinSize = res.getFloat(R.dimen.config_pictureInPictureAspectRatioLimitForMinSize);
        this.mMinAspectRatioForMinSize = 1.0f / this.mMaxAspectRatioForMinSize;
        this.mFlingDeceleration = this.mContext.getResources().getDimensionPixelSize(R.dimen.pip_fling_deceleration);
        onConfigurationChanged();
    }

    public void onConfigurationChanged() {
        Resources res = this.mContext.getResources();
        this.mOrientation = res.getConfiguration().orientation;
        this.mSnapMode = res.getInteger(R.integer.config_pictureInPictureSnapMode);
        calculateSnapTargets();
    }

    public void setMinimized(boolean isMinimized) {
        this.mIsMinimized = isMinimized;
    }

    public Rect findClosestSnapBounds(Rect movementBounds, Rect stackBounds, float velocityX, float velocityY, Point dragStartPosition) {
        Rect intersectStackBounds = new Rect(stackBounds);
        Point intersect = getEdgeIntersect(stackBounds, movementBounds, velocityX, velocityY, dragStartPosition);
        intersectStackBounds.offsetTo(intersect.x, intersect.y);
        return findClosestSnapBounds(movementBounds, intersectStackBounds);
    }

    public Point getEdgeIntersect(Rect stackBounds, Rect movementBounds, float velX, float velY, Point dragStartPosition) {
        int maxDistance;
        boolean isLandscape = this.mOrientation == 2;
        int x = stackBounds.left;
        int y = stackBounds.top;
        float slope = velY / velX;
        float yIntercept = ((float) y) - (((float) x) * slope);
        Point vertPoint = new Point();
        Point horizPoint = new Point();
        vertPoint.x = velX > 0.0f ? movementBounds.right : movementBounds.left;
        vertPoint.y = findY(slope, yIntercept, (float) vertPoint.x);
        horizPoint.y = velY > 0.0f ? movementBounds.bottom : movementBounds.top;
        horizPoint.x = findX(slope, yIntercept, (float) horizPoint.y);
        if (isLandscape) {
            if (velX > 0.0f) {
                maxDistance = movementBounds.right - stackBounds.left;
            } else {
                maxDistance = stackBounds.left - movementBounds.left;
            }
        } else if (velY > 0.0f) {
            maxDistance = movementBounds.bottom - stackBounds.top;
        } else {
            maxDistance = stackBounds.top - movementBounds.top;
        }
        if (maxDistance > 0) {
            int startPoint = isLandscape ? dragStartPosition.y : dragStartPosition.x;
            int endPoint = isLandscape ? horizPoint.y : horizPoint.x;
            int center = movementBounds.centerX();
            if ((startPoint < center && endPoint < center) || (startPoint > center && endPoint > center)) {
                float f;
                if (isLandscape) {
                    f = velX;
                } else {
                    f = velY;
                }
                int distance = Math.min(((int) (0.0d - Math.pow((double) f, 2.0d))) / (this.mFlingDeceleration * 2), maxDistance);
                int i;
                if (isLandscape) {
                    i = stackBounds.left;
                    if (velX <= 0.0f) {
                        distance = -distance;
                    }
                    horizPoint.x = i + distance;
                } else {
                    i = stackBounds.top;
                    if (velY <= 0.0f) {
                        distance = -distance;
                    }
                    horizPoint.y = i + distance;
                }
                return horizPoint;
            }
        }
        double distanceVert = Math.hypot((double) (vertPoint.x - x), (double) (vertPoint.y - y));
        double distanceHoriz = Math.hypot((double) (horizPoint.x - x), (double) (horizPoint.y - y));
        if (distanceVert == 0.0d) {
            return horizPoint;
        }
        if (distanceHoriz == 0.0d) {
            return vertPoint;
        }
        if (Math.abs(distanceVert) <= Math.abs(distanceHoriz)) {
            horizPoint = vertPoint;
        }
        return horizPoint;
    }

    private int findY(float slope, float yIntercept, float x) {
        return (int) ((slope * x) + yIntercept);
    }

    private int findX(float slope, float yIntercept, float y) {
        return (int) ((y - yIntercept) / slope);
    }

    public Rect findClosestSnapBounds(Rect movementBounds, Rect stackBounds) {
        Rect pipBounds = new Rect(movementBounds.left, movementBounds.top, movementBounds.right + stackBounds.width(), movementBounds.bottom + stackBounds.height());
        Rect newBounds = new Rect(stackBounds);
        Rect tmpBounds;
        Point[] snapTargets;
        int i;
        Point snapTarget;
        if (this.mSnapMode == 4 || this.mSnapMode == 3) {
            tmpBounds = new Rect();
            snapTargets = new Point[this.mSnapGravities.size()];
            for (i = 0; i < this.mSnapGravities.size(); i++) {
                Gravity.apply(((Integer) this.mSnapGravities.get(i)).intValue(), stackBounds.width(), stackBounds.height(), pipBounds, 0, 0, tmpBounds);
                snapTargets[i] = new Point(tmpBounds.left, tmpBounds.top);
            }
            snapTarget = findClosestPoint(stackBounds.left, stackBounds.top, snapTargets);
            if (distanceToPoint(snapTarget, stackBounds.left, stackBounds.top) < ((float) Math.max(stackBounds.width(), stackBounds.height())) * CORNER_MAGNET_THRESHOLD) {
                newBounds.offsetTo(snapTarget.x, snapTarget.y);
            } else {
                snapRectToClosestEdge(stackBounds, movementBounds, newBounds);
            }
        } else if (this.mSnapMode == 2) {
            snapRectToClosestEdge(stackBounds, movementBounds, newBounds);
        } else {
            tmpBounds = new Rect();
            snapTargets = new Point[this.mSnapGravities.size()];
            for (i = 0; i < this.mSnapGravities.size(); i++) {
                Gravity.apply(((Integer) this.mSnapGravities.get(i)).intValue(), stackBounds.width(), stackBounds.height(), pipBounds, 0, 0, tmpBounds);
                snapTargets[i] = new Point(tmpBounds.left, tmpBounds.top);
            }
            snapTarget = findClosestPoint(stackBounds.left, stackBounds.top, snapTargets);
            newBounds.offsetTo(snapTarget.x, snapTarget.y);
        }
        return newBounds;
    }

    public void applyMinimizedOffset(Rect stackBounds, Rect movementBounds, Point displaySize, Rect stableInsets) {
        if (stackBounds.left <= movementBounds.centerX()) {
            stackBounds.offsetTo((stableInsets.left + this.mMinimizedVisibleSize) - stackBounds.width(), stackBounds.top);
        } else {
            stackBounds.offsetTo((displaySize.x - stableInsets.right) - this.mMinimizedVisibleSize, stackBounds.top);
        }
    }

    public float getSnapFraction(Rect stackBounds, Rect movementBounds) {
        Rect tmpBounds = new Rect();
        snapRectToClosestEdge(stackBounds, movementBounds, tmpBounds);
        float widthFraction = ((float) (tmpBounds.left - movementBounds.left)) / ((float) movementBounds.width());
        float heightFraction = ((float) (tmpBounds.top - movementBounds.top)) / ((float) movementBounds.height());
        if (tmpBounds.top == movementBounds.top) {
            return widthFraction;
        }
        if (tmpBounds.left == movementBounds.right) {
            return 1.0f + heightFraction;
        }
        if (tmpBounds.top == movementBounds.bottom) {
            return (1.0f - widthFraction) + 2.0f;
        }
        return (1.0f - heightFraction) + 3.0f;
    }

    public void applySnapFraction(Rect stackBounds, Rect movementBounds, float snapFraction) {
        if (snapFraction < 1.0f) {
            stackBounds.offsetTo(movementBounds.left + ((int) (((float) movementBounds.width()) * snapFraction)), movementBounds.top);
        } else if (snapFraction < 2.0f) {
            stackBounds.offsetTo(movementBounds.right, movementBounds.top + ((int) (((float) movementBounds.height()) * (snapFraction - 1.0f))));
        } else if (snapFraction < 3.0f) {
            stackBounds.offsetTo(movementBounds.left + ((int) ((1.0f - (snapFraction - 2.0f)) * ((float) movementBounds.width()))), movementBounds.bottom);
        } else {
            stackBounds.offsetTo(movementBounds.left, movementBounds.top + ((int) ((1.0f - (snapFraction - 3.0f)) * ((float) movementBounds.height()))));
        }
    }

    public void getMovementBounds(Rect stackBounds, Rect insetBounds, Rect movementBoundsOut, int imeHeight) {
        movementBoundsOut.set(insetBounds);
        movementBoundsOut.right = Math.max(insetBounds.left, insetBounds.right - stackBounds.width());
        movementBoundsOut.bottom = Math.max(insetBounds.top, insetBounds.bottom - stackBounds.height());
        movementBoundsOut.bottom -= imeHeight;
    }

    public Size getSizeForAspectRatio(float aspectRatio, float minEdgeSize, int displayWidth, int displayHeight) {
        int height;
        int width;
        int minSize = (int) Math.max(minEdgeSize, ((float) Math.min(displayWidth, displayHeight)) * this.mDefaultSizePercent);
        if (aspectRatio > this.mMinAspectRatioForMinSize && aspectRatio <= this.mMaxAspectRatioForMinSize) {
            float radius = PointF.length(this.mMaxAspectRatioForMinSize * ((float) minSize), (float) minSize);
            height = (int) Math.round(Math.sqrt((double) ((radius * radius) / ((aspectRatio * aspectRatio) + 1.0f))));
            width = Math.round(((float) height) * aspectRatio);
        } else if (aspectRatio <= 1.0f) {
            width = minSize;
            height = Math.round(((float) minSize) / aspectRatio);
        } else {
            height = minSize;
            width = Math.round(((float) minSize) * aspectRatio);
        }
        return new Size(width, height);
    }

    private Point findClosestPoint(int x, int y, Point[] points) {
        Point closestPoint = null;
        float minDistance = Float.MAX_VALUE;
        for (Point p : points) {
            float distance = distanceToPoint(p, x, y);
            if (distance < minDistance) {
                closestPoint = p;
                minDistance = distance;
            }
        }
        return closestPoint;
    }

    private void snapRectToClosestEdge(Rect stackBounds, Rect movementBounds, Rect boundsOut) {
        int boundedLeft = Math.max(movementBounds.left, Math.min(movementBounds.right, stackBounds.left));
        int boundedTop = Math.max(movementBounds.top, Math.min(movementBounds.bottom, stackBounds.top));
        boundsOut.set(stackBounds);
        if (this.mIsMinimized) {
            boundsOut.offsetTo(boundedLeft, boundedTop);
            return;
        }
        int shortest;
        int fromLeft = Math.abs(stackBounds.left - movementBounds.left);
        int fromTop = Math.abs(stackBounds.top - movementBounds.top);
        int fromRight = Math.abs(movementBounds.right - stackBounds.left);
        int fromBottom = Math.abs(movementBounds.bottom - stackBounds.top);
        if (this.mSnapMode != 4) {
            shortest = Math.min(Math.min(fromLeft, fromRight), Math.min(fromTop, fromBottom));
        } else if (this.mOrientation == 2) {
            shortest = Math.min(fromTop, fromBottom);
        } else {
            shortest = Math.min(fromLeft, fromRight);
        }
        if (shortest == fromLeft) {
            boundsOut.offsetTo(movementBounds.left, boundedTop);
        } else if (shortest == fromTop) {
            boundsOut.offsetTo(boundedLeft, movementBounds.top);
        } else if (shortest == fromRight) {
            boundsOut.offsetTo(movementBounds.right, boundedTop);
        } else {
            boundsOut.offsetTo(boundedLeft, movementBounds.bottom);
        }
    }

    private float distanceToPoint(Point p, int x, int y) {
        return PointF.length((float) (p.x - x), (float) (p.y - y));
    }

    private void calculateSnapTargets() {
        this.mSnapGravities.clear();
        switch (this.mSnapMode) {
            case 0:
            case 3:
            case 4:
                break;
            case 1:
                if (this.mOrientation != 2) {
                    this.mSnapGravities.add(Integer.valueOf(19));
                    this.mSnapGravities.add(Integer.valueOf(21));
                    break;
                }
                this.mSnapGravities.add(Integer.valueOf(49));
                this.mSnapGravities.add(Integer.valueOf(81));
                break;
            default:
                return;
        }
        this.mSnapGravities.add(Integer.valueOf(51));
        this.mSnapGravities.add(Integer.valueOf(53));
        this.mSnapGravities.add(Integer.valueOf(83));
        this.mSnapGravities.add(Integer.valueOf(85));
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println(prefix + PipSnapAlgorithm.class.getSimpleName());
        pw.println(innerPrefix + "mSnapMode=" + this.mSnapMode);
        pw.println(innerPrefix + "mOrientation=" + this.mOrientation);
        pw.println(innerPrefix + "mMinimizedVisibleSize=" + this.mMinimizedVisibleSize);
        pw.println(innerPrefix + "mIsMinimized=" + this.mIsMinimized);
    }
}
