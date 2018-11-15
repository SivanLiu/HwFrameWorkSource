package com.google.android.maps;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import com.google.android.maps.MapView.LayoutParams;

class GestureDetector {
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private boolean mAlwaysInBiggerTapRegion;
    private boolean mAlwaysInTapRegion;
    private MotionEvent mCurrentDownEvent;
    private OnDoubleTapListener mDoubleTapListener;
    private int mDoubleTapSlopSquare;
    private int mDoubleTapTouchSlopSquare;
    private float mDownFocusX;
    private float mDownFocusY;
    private final Handler mHandler;
    private boolean mInLongPress;
    private boolean mIsDoubleTapping;
    private boolean mIsLongpressEnabled;
    private float mLastFocusX;
    private float mLastFocusY;
    private final OnGestureListener mListener;
    private int mMaximumFlingVelocity;
    private int mMinimumFlingVelocity;
    private MotionEvent mPreviousUpEvent;
    private boolean mStillDown;
    private int mTouchSlopSquare;
    private VelocityTracker mVelocityTracker;

    private class GestureHandler extends Handler {
        GestureHandler() {
        }

        GestureHandler(Handler handler) {
            super(handler.getLooper());
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    GestureDetector.this.mListener.onShowPress(GestureDetector.this.mCurrentDownEvent);
                    return;
                case OverlayItem.ITEM_STATE_SELECTED_MASK /*2*/:
                    GestureDetector.this.dispatchLongPress();
                    return;
                case LayoutParams.LEFT /*3*/:
                    if (GestureDetector.this.mDoubleTapListener != null && !GestureDetector.this.mStillDown) {
                        GestureDetector.this.mDoubleTapListener.onSingleTapConfirmed(GestureDetector.this.mCurrentDownEvent);
                        return;
                    }
                    return;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown message ");
                    stringBuilder.append(msg);
                    throw new RuntimeException(stringBuilder.toString());
            }
        }
    }

    public interface OnDoubleTapListener {
        boolean onDoubleTap(MotionEvent motionEvent);

        boolean onDoubleTapEvent(MotionEvent motionEvent);

        boolean onSingleTapConfirmed(MotionEvent motionEvent);
    }

    public interface OnGestureListener {
        boolean onDown(MotionEvent motionEvent);

        boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);

        void onLongPress(MotionEvent motionEvent);

        boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);

        void onShowPress(MotionEvent motionEvent);

        boolean onSingleTapUp(MotionEvent motionEvent);
    }

    public static class SimpleOnGestureListener implements OnDoubleTapListener, OnGestureListener {
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        public void onLongPress(MotionEvent e) {
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        public void onShowPress(MotionEvent e) {
        }

        public boolean onDown(MotionEvent e) {
            return false;
        }

        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }
    }

    public GestureDetector(Context context, OnGestureListener listener) {
        this(context, listener, null);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler) {
        if (handler != null) {
            this.mHandler = new GestureHandler(handler);
        } else {
            this.mHandler = new GestureHandler();
        }
        this.mListener = listener;
        if (listener instanceof OnDoubleTapListener) {
            setOnDoubleTapListener((OnDoubleTapListener) listener);
        }
        init(context);
    }

    private void init(Context context) {
        if (this.mListener != null) {
            int touchSlop;
            int doubleTapTouchSlop;
            int doubleTapSlop;
            this.mIsLongpressEnabled = true;
            if (context == null) {
                touchSlop = ViewConfiguration.getTouchSlop();
                doubleTapTouchSlop = touchSlop;
                doubleTapSlop = ViewConfiguration.getDoubleTapSlop();
                this.mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
                this.mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
            } else {
                ViewConfiguration configuration = ViewConfiguration.get(context);
                doubleTapTouchSlop = configuration.getScaledTouchSlop();
                doubleTapSlop = configuration.getScaledDoubleTapTouchSlop();
                int doubleTapSlop2 = configuration.getScaledDoubleTapSlop();
                this.mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
                this.mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
                touchSlop = doubleTapTouchSlop;
                doubleTapTouchSlop = doubleTapSlop;
                doubleTapSlop = doubleTapSlop2;
            }
            this.mTouchSlopSquare = touchSlop * touchSlop;
            this.mDoubleTapTouchSlopSquare = doubleTapTouchSlop * doubleTapTouchSlop;
            this.mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
            return;
        }
        throw new NullPointerException("OnGestureListener must not be null");
    }

    public void setOnDoubleTapListener(OnDoubleTapListener onDoubleTapListener) {
        this.mDoubleTapListener = onDoubleTapListener;
    }

    public void setIsLongpressEnabled(boolean isLongpressEnabled) {
        this.mIsLongpressEnabled = isLongpressEnabled;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int i;
        MotionEvent motionEvent = ev;
        int action = ev.getAction();
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        boolean pointerUp = (action & 255) == 6;
        int skipIndex = pointerUp ? ev.getActionIndex() : -1;
        int count = ev.getPointerCount();
        float sumY = 0.0f;
        float sumX = 0.0f;
        for (i = 0; i < count; i++) {
            if (skipIndex != i) {
                sumX += motionEvent.getX(i);
                sumY += motionEvent.getY(i);
            }
        }
        i = pointerUp ? count - 1 : count;
        float focusX = sumX / ((float) i);
        float focusY = sumY / ((float) i);
        boolean handled = false;
        boolean z;
        int i2;
        boolean hadTapMessage;
        switch (action & 255) {
            case LayoutParams.MODE_MAP /*0*/:
                z = pointerUp;
                i2 = skipIndex;
                if (this.mDoubleTapListener != null) {
                    hadTapMessage = this.mHandler.hasMessages(3);
                    if (hadTapMessage) {
                        this.mHandler.removeMessages(3);
                    }
                    if (this.mCurrentDownEvent == null || this.mPreviousUpEvent == null || !hadTapMessage || !isConsideredDoubleTap(this.mCurrentDownEvent, this.mPreviousUpEvent, motionEvent)) {
                        this.mHandler.sendEmptyMessageDelayed(3, (long) DOUBLE_TAP_TIMEOUT);
                    } else {
                        this.mIsDoubleTapping = true;
                        handled = (this.mDoubleTapListener.onDoubleTap(this.mCurrentDownEvent) | false) | this.mDoubleTapListener.onDoubleTapEvent(motionEvent);
                    }
                }
                this.mLastFocusX = focusX;
                this.mDownFocusX = focusX;
                this.mLastFocusY = focusY;
                this.mDownFocusY = focusY;
                if (this.mCurrentDownEvent != null) {
                    this.mCurrentDownEvent.recycle();
                }
                this.mCurrentDownEvent = MotionEvent.obtain(ev);
                this.mAlwaysInTapRegion = true;
                this.mAlwaysInBiggerTapRegion = true;
                this.mStillDown = true;
                this.mInLongPress = false;
                if (this.mIsLongpressEnabled) {
                    this.mHandler.removeMessages(2);
                    this.mHandler.sendEmptyMessageAtTime(2, (this.mCurrentDownEvent.getDownTime() + ((long) TAP_TIMEOUT)) + ((long) LONGPRESS_TIMEOUT));
                }
                this.mHandler.sendEmptyMessageAtTime(1, this.mCurrentDownEvent.getDownTime() + ((long) TAP_TIMEOUT));
                return handled | this.mListener.onDown(motionEvent);
            case 1:
                z = pointerUp;
                i2 = skipIndex;
                this.mStillDown = false;
                action = MotionEvent.obtain(ev);
                if (this.mIsDoubleTapping) {
                    handled = false | this.mDoubleTapListener.onDoubleTapEvent(motionEvent);
                } else if (this.mInLongPress) {
                    this.mHandler.removeMessages(3);
                    this.mInLongPress = false;
                } else if (this.mAlwaysInTapRegion) {
                    handled = this.mListener.onSingleTapUp(motionEvent);
                } else {
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumFlingVelocity);
                    float velocityY = velocityTracker.getYVelocity();
                    float velocityX = velocityTracker.getXVelocity();
                    if (Math.abs(velocityY) > ((float) this.mMinimumFlingVelocity) || Math.abs(velocityX) > ((float) this.mMinimumFlingVelocity)) {
                        handled = this.mListener.onFling(this.mCurrentDownEvent, motionEvent, velocityX, velocityY);
                    }
                }
                if (this.mPreviousUpEvent != null) {
                    this.mPreviousUpEvent.recycle();
                }
                this.mPreviousUpEvent = action;
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                this.mIsDoubleTapping = false;
                this.mHandler.removeMessages(1);
                this.mHandler.removeMessages(2);
                return handled;
            case OverlayItem.ITEM_STATE_SELECTED_MASK /*2*/:
                if (!this.mInLongPress) {
                    float scrollX = this.mLastFocusX - focusX;
                    float scrollY = this.mLastFocusY - focusY;
                    if (this.mIsDoubleTapping) {
                        handled = false | this.mDoubleTapListener.onDoubleTapEvent(motionEvent);
                        break;
                    } else if (this.mAlwaysInTapRegion) {
                        int deltaX = (int) (focusX - this.mDownFocusX);
                        int deltaY = (int) (focusY - this.mDownFocusY);
                        int i3 = action;
                        hadTapMessage = (deltaX * deltaX) + (deltaY * deltaY);
                        z = pointerUp;
                        if (hadTapMessage > this.mTouchSlopSquare) {
                            i2 = skipIndex;
                            pointerUp = this.mListener.onScroll(this.mCurrentDownEvent, motionEvent, scrollX, scrollY);
                            this.mLastFocusX = focusX;
                            this.mLastFocusY = focusY;
                            this.mAlwaysInTapRegion = false;
                            this.mHandler.removeMessages(3);
                            this.mHandler.removeMessages(1);
                            this.mHandler.removeMessages(2);
                            handled = pointerUp;
                        }
                        if (hadTapMessage <= this.mDoubleTapTouchSlopSquare) {
                            return handled;
                        }
                        this.mAlwaysInBiggerTapRegion = false;
                        return handled;
                    } else {
                        z = pointerUp;
                        i2 = skipIndex;
                        if (Math.abs(scrollX) < 1065353216 && Math.abs(scrollY) < 1065353216) {
                            return false;
                        }
                        handled = this.mListener.onScroll(this.mCurrentDownEvent, motionEvent, scrollX, scrollY);
                        this.mLastFocusX = focusX;
                        this.mLastFocusY = focusY;
                        return handled;
                    }
                }
                break;
            case LayoutParams.LEFT /*3*/:
                cancel();
                break;
            case LayoutParams.RIGHT /*5*/:
                this.mLastFocusX = focusX;
                this.mDownFocusX = focusX;
                this.mLastFocusY = focusY;
                this.mDownFocusY = focusY;
                cancelTaps();
                break;
            case 6:
                this.mLastFocusX = focusX;
                this.mDownFocusX = focusX;
                this.mLastFocusY = focusY;
                this.mDownFocusY = focusY;
                this.mVelocityTracker.clear();
                break;
            default:
                return false;
        }
        return handled;
    }

    private void cancel() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
        this.mIsDoubleTapping = false;
        this.mStillDown = false;
        this.mAlwaysInTapRegion = false;
        this.mAlwaysInBiggerTapRegion = false;
        if (this.mInLongPress) {
            this.mInLongPress = false;
        }
    }

    private void cancelTaps() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        this.mIsDoubleTapping = false;
        this.mAlwaysInTapRegion = false;
        this.mAlwaysInBiggerTapRegion = false;
        if (this.mInLongPress) {
            this.mInLongPress = false;
        }
    }

    private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp, MotionEvent secondDown) {
        boolean z = false;
        if (!this.mAlwaysInBiggerTapRegion || secondDown.getEventTime() - firstUp.getEventTime() > ((long) DOUBLE_TAP_TIMEOUT)) {
            return false;
        }
        int deltaX = ((int) firstDown.getX()) - ((int) secondDown.getX());
        int deltaY = ((int) firstDown.getY()) - ((int) secondDown.getY());
        if ((deltaX * deltaX) + (deltaY * deltaY) < this.mDoubleTapSlopSquare) {
            z = true;
        }
        return z;
    }

    private void dispatchLongPress() {
        this.mHandler.removeMessages(3);
        this.mInLongPress = true;
        this.mListener.onLongPress(this.mCurrentDownEvent);
    }
}
