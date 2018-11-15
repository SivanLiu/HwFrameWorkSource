package com.android.server.accessibility;

import android.content.Context;
import android.gesture.GesturePoint;
import android.graphics.PointF;
import android.util.Slog;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import java.util.ArrayList;

class AccessibilityGestureDetector extends SimpleOnGestureListener {
    private static final float ANGLE_THRESHOLD = 0.0f;
    private static final long CANCEL_ON_PAUSE_THRESHOLD_NOT_STARTED_MS = 150;
    private static final long CANCEL_ON_PAUSE_THRESHOLD_STARTED_MS = 300;
    private static final boolean DEBUG = false;
    private static final int[][] DIRECTIONS_TO_GESTURE_ID = new int[][]{new int[]{3, 5, 9, 10}, new int[]{6, 4, 11, 12}, new int[]{13, 14, 1, 7}, new int[]{15, 16, 8, 2}};
    private static final int DOWN = 3;
    private static final int GESTURE_CONFIRM_MM = 10;
    private static final int LEFT = 0;
    private static final String LOG_TAG = "AccessibilityGestureDetector";
    private static final float MIN_INCHES_BETWEEN_SAMPLES = 0.1f;
    private static final float MIN_PREDICTION_SCORE = 2.0f;
    private static final int RIGHT = 1;
    private static final int TOUCH_TOLERANCE = 3;
    private static final int UP = 2;
    private long mBaseTime;
    private float mBaseX;
    private float mBaseY;
    private final Context mContext;
    private boolean mDoubleTapDetected;
    private boolean mFirstTapDetected;
    private final float mGestureDetectionThreshold;
    protected GestureDetector mGestureDetector;
    private boolean mGestureStarted;
    private final Listener mListener;
    private final float mMinPixelsBetweenSamplesX;
    private final float mMinPixelsBetweenSamplesY;
    private int mPolicyFlags;
    private float mPreviousGestureX;
    private float mPreviousGestureY;
    private boolean mRecognizingGesture;
    private boolean mSecondFingerDoubleTap;
    private long mSecondPointerDownTime;
    private final ArrayList<GesturePoint> mStrokeBuffer = new ArrayList(100);

    public interface Listener {
        boolean onDoubleTap(MotionEvent motionEvent, int i);

        void onDoubleTapAndHold(MotionEvent motionEvent, int i);

        boolean onGestureCancelled(MotionEvent motionEvent, int i);

        boolean onGestureCompleted(int i);

        boolean onGestureStarted();
    }

    AccessibilityGestureDetector(Context context, Listener listener) {
        this.mListener = listener;
        this.mContext = context;
        this.mGestureDetectionThreshold = TypedValue.applyDimension(5, 1.0f, context.getResources().getDisplayMetrics()) * 10.0f;
        float pixelsPerInchX = context.getResources().getDisplayMetrics().xdpi;
        float pixelsPerInchY = context.getResources().getDisplayMetrics().ydpi;
        this.mMinPixelsBetweenSamplesX = MIN_INCHES_BETWEEN_SAMPLES * pixelsPerInchX;
        this.mMinPixelsBetweenSamplesY = MIN_INCHES_BETWEEN_SAMPLES * pixelsPerInchY;
    }

    public boolean onMotionEvent(MotionEvent event, int policyFlags) {
        MotionEvent motionEvent = event;
        int i = policyFlags;
        if (this.mGestureDetector == null) {
            this.mGestureDetector = new GestureDetector(this.mContext, this);
            this.mGestureDetector.setOnDoubleTapListener(this);
        }
        float x = event.getX();
        float y = event.getY();
        long time = event.getEventTime();
        this.mPolicyFlags = i;
        float dY;
        switch (event.getActionMasked()) {
            case 0:
                this.mDoubleTapDetected = false;
                this.mSecondFingerDoubleTap = false;
                this.mRecognizingGesture = true;
                this.mGestureStarted = false;
                this.mPreviousGestureX = x;
                this.mPreviousGestureY = y;
                this.mStrokeBuffer.clear();
                this.mStrokeBuffer.add(new GesturePoint(x, y, time));
                this.mBaseX = x;
                this.mBaseY = y;
                this.mBaseTime = time;
                break;
            case 1:
                if (this.mDoubleTapDetected) {
                    return finishDoubleTap(event, policyFlags);
                }
                if (this.mGestureStarted) {
                    float dX = Math.abs(x - this.mPreviousGestureX);
                    dY = Math.abs(y - this.mPreviousGestureY);
                    if (dX >= this.mMinPixelsBetweenSamplesX || dY >= this.mMinPixelsBetweenSamplesY) {
                        this.mStrokeBuffer.add(new GesturePoint(x, y, time));
                    }
                    return recognizeGesture(event, policyFlags);
                }
                break;
            case 2:
                if (this.mRecognizingGesture) {
                    if (Math.hypot((double) (this.mBaseX - x), (double) (this.mBaseY - y)) > ((double) this.mGestureDetectionThreshold)) {
                        this.mBaseX = x;
                        this.mBaseY = y;
                        this.mBaseTime = time;
                        this.mFirstTapDetected = false;
                        this.mDoubleTapDetected = false;
                        if (!this.mGestureStarted) {
                            this.mGestureStarted = true;
                            return this.mListener.onGestureStarted();
                        }
                    } else if (!this.mFirstTapDetected) {
                        long threshold;
                        long timeDelta = time - this.mBaseTime;
                        if (this.mGestureStarted) {
                            threshold = CANCEL_ON_PAUSE_THRESHOLD_STARTED_MS;
                        } else {
                            threshold = CANCEL_ON_PAUSE_THRESHOLD_NOT_STARTED_MS;
                        }
                        if (timeDelta > threshold) {
                            cancelGesture();
                            return this.mListener.onGestureCancelled(motionEvent, i);
                        }
                    }
                    dY = Math.abs(x - this.mPreviousGestureX);
                    float dY2 = Math.abs(y - this.mPreviousGestureY);
                    if (dY >= this.mMinPixelsBetweenSamplesX || dY2 >= this.mMinPixelsBetweenSamplesY) {
                        this.mPreviousGestureX = x;
                        this.mPreviousGestureY = y;
                        this.mStrokeBuffer.add(new GesturePoint(x, y, time));
                        break;
                    }
                }
                break;
            case 3:
                clear();
                break;
            case 5:
                cancelGesture();
                if (event.getPointerCount() != 2) {
                    this.mSecondFingerDoubleTap = false;
                    break;
                }
                this.mSecondFingerDoubleTap = true;
                this.mSecondPointerDownTime = time;
                break;
            case 6:
                if (this.mSecondFingerDoubleTap && this.mDoubleTapDetected) {
                    return finishDoubleTap(event, policyFlags);
                }
        }
        if (this.mSecondFingerDoubleTap) {
            MotionEvent newEvent = mapSecondPointerToFirstPointer(event);
            if (newEvent == null) {
                return false;
            }
            boolean handled = this.mGestureDetector.onTouchEvent(newEvent);
            newEvent.recycle();
            return handled;
        } else if (this.mRecognizingGesture) {
            return this.mGestureDetector.onTouchEvent(motionEvent);
        } else {
            return false;
        }
    }

    public void clear() {
        this.mFirstTapDetected = false;
        this.mDoubleTapDetected = false;
        this.mSecondFingerDoubleTap = false;
        this.mGestureStarted = false;
        this.mGestureDetector.onTouchEvent(MotionEvent.obtain(0, 0, 3, ANGLE_THRESHOLD, ANGLE_THRESHOLD, 0));
        cancelGesture();
    }

    public boolean firstTapDetected() {
        return this.mFirstTapDetected;
    }

    public void onLongPress(MotionEvent e) {
        maybeSendLongPress(e, this.mPolicyFlags);
    }

    public boolean onSingleTapUp(MotionEvent event) {
        this.mFirstTapDetected = true;
        return false;
    }

    public boolean onSingleTapConfirmed(MotionEvent event) {
        clear();
        return false;
    }

    public boolean onDoubleTap(MotionEvent event) {
        this.mDoubleTapDetected = true;
        return false;
    }

    private void maybeSendLongPress(MotionEvent event, int policyFlags) {
        if (this.mDoubleTapDetected) {
            clear();
            this.mListener.onDoubleTapAndHold(event, policyFlags);
        }
    }

    private boolean finishDoubleTap(MotionEvent event, int policyFlags) {
        clear();
        return this.mListener.onDoubleTap(event, policyFlags);
    }

    private void cancelGesture() {
        this.mRecognizingGesture = false;
        this.mGestureStarted = false;
        this.mStrokeBuffer.clear();
    }

    private boolean recognizeGesture(MotionEvent event, int policyFlags) {
        MotionEvent motionEvent = event;
        int i = policyFlags;
        if (this.mStrokeBuffer.size() < 2) {
            return this.mListener.onGestureCancelled(motionEvent, i);
        }
        int count;
        float dY;
        ArrayList<PointF> path = new ArrayList();
        PointF lastDelimiter = new PointF(((GesturePoint) this.mStrokeBuffer.get(0)).x, ((GesturePoint) this.mStrokeBuffer.get(0)).y);
        path.add(lastDelimiter);
        int count2 = 0;
        PointF next = new PointF();
        float length = ANGLE_THRESHOLD;
        float dY2 = ANGLE_THRESHOLD;
        float dX = ANGLE_THRESHOLD;
        PointF lastDelimiter2 = lastDelimiter;
        int i2 = 1;
        while (i2 < this.mStrokeBuffer.size()) {
            float dX2;
            float nextLength;
            float dX3;
            next = new PointF(((GesturePoint) this.mStrokeBuffer.get(i2)).x, ((GesturePoint) this.mStrokeBuffer.get(i2)).y);
            if (count2 > 0) {
                float currentDX = dX / ((float) count2);
                float currentDY = dY2 / ((float) count2);
                dX2 = dX;
                dX = new PointF((length * currentDX) + lastDelimiter2.x, (length * currentDY) + lastDelimiter2.y);
                float nextDX = next.x - dX.x;
                float nextDY = next.y - dX.y;
                count = count2;
                dY = dY2;
                nextLength = (float) Math.sqrt((double) ((nextDX * nextDX) + (nextDY * nextDY)));
                if ((currentDX * (nextDX / nextLength)) + (currentDY * (nextDY / nextLength)) < ANGLE_THRESHOLD) {
                    path.add(dX);
                    lastDelimiter2 = dX;
                    dX3 = ANGLE_THRESHOLD;
                    dX2 = ANGLE_THRESHOLD;
                    count = 0;
                    dX = next.x - lastDelimiter2.x;
                    nextLength = next.y - lastDelimiter2.y;
                    length = (float) Math.sqrt((double) ((dX * dX) + (nextLength * nextLength)));
                    i2++;
                    dY2 = dX2 + (nextLength / length);
                    count2 = count + 1;
                    dX = (dX / length) + dX3;
                }
            } else {
                dX2 = dX;
                count = count2;
                dY = dY2;
            }
            dX3 = dX2;
            dX2 = dY;
            dX = next.x - lastDelimiter2.x;
            nextLength = next.y - lastDelimiter2.y;
            length = (float) Math.sqrt((double) ((dX * dX) + (nextLength * nextLength)));
            i2++;
            dY2 = dX2 + (nextLength / length);
            count2 = count + 1;
            dX = (dX / length) + dX3;
        }
        count = count2;
        dY = dY2;
        path.add(next);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("path=");
        stringBuilder.append(path.toString());
        Slog.i(str, stringBuilder.toString());
        return recognizeGesturePath(motionEvent, i, path);
    }

    private boolean recognizeGesturePath(MotionEvent event, int policyFlags, ArrayList<PointF> path) {
        PointF start;
        PointF end;
        if (path.size() == 2) {
            start = (PointF) path.get(0);
            end = (PointF) path.get(1);
            switch (toDirection(end.x - start.x, end.y - start.y)) {
                case 0:
                    return this.mListener.onGestureCompleted(3);
                case 1:
                    return this.mListener.onGestureCompleted(4);
                case 2:
                    return this.mListener.onGestureCompleted(1);
                case 3:
                    return this.mListener.onGestureCompleted(2);
            }
        } else if (path.size() == 3) {
            start = (PointF) path.get(0);
            PointF mid = (PointF) path.get(1);
            end = (PointF) path.get(2);
            float dX1 = end.x - mid.x;
            float dY1 = end.y - mid.y;
            int segmentDirection0 = toDirection(mid.x - start.x, mid.y - start.y);
            return this.mListener.onGestureCompleted(DIRECTIONS_TO_GESTURE_ID[segmentDirection0][toDirection(dX1, dY1)]);
        }
        return this.mListener.onGestureCancelled(event, policyFlags);
    }

    private static int toDirection(float dX, float dY) {
        if (Math.abs(dX) > Math.abs(dY)) {
            return dX < ANGLE_THRESHOLD ? 0 : 1;
        }
        return dY < ANGLE_THRESHOLD ? 2 : 3;
    }

    private MotionEvent mapSecondPointerToFirstPointer(MotionEvent event) {
        MotionEvent motionEvent = event;
        if (event.getPointerCount() != 2 || (event.getActionMasked() != 5 && event.getActionMasked() != 6 && event.getActionMasked() != 2)) {
            return null;
        }
        int action = event.getActionMasked();
        if (action == 5) {
            action = 0;
        } else if (action == 6) {
            action = 1;
        }
        return MotionEvent.obtain(this.mSecondPointerDownTime, event.getEventTime(), action, motionEvent.getX(1), motionEvent.getY(1), motionEvent.getPressure(1), motionEvent.getSize(1), event.getMetaState(), event.getXPrecision(), event.getYPrecision(), event.getDeviceId(), event.getEdgeFlags());
    }
}
