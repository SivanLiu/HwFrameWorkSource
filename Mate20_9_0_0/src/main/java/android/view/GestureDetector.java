package android.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class GestureDetector {
    private static final int DOUBLE_TAP_MIN_TIME = ViewConfiguration.getDoubleTapMinTime();
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int LONG_PRESS = 2;
    private static final int SHOW_PRESS = 1;
    private static final int TAP = 3;
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private boolean mAlwaysInBiggerTapRegion;
    private boolean mAlwaysInTapRegion;
    private OnContextClickListener mContextClickListener;
    private MotionEvent mCurrentDownEvent;
    private boolean mDeferConfirmSingleTap;
    private OnDoubleTapListener mDoubleTapListener;
    private int mDoubleTapSlopSquare;
    private int mDoubleTapTouchSlopSquare;
    private float mDownFocusX;
    private float mDownFocusY;
    private final Handler mHandler;
    private boolean mIgnoreNextUpEvent;
    private boolean mInContextClick;
    private boolean mInLongPress;
    private final InputEventConsistencyVerifier mInputEventConsistencyVerifier;
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

    public interface OnContextClickListener {
        boolean onContextClick(MotionEvent motionEvent);
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
                case 2:
                    GestureDetector.this.dispatchLongPress();
                    return;
                case 3:
                    if (GestureDetector.this.mDoubleTapListener == null) {
                        return;
                    }
                    if (GestureDetector.this.mStillDown) {
                        GestureDetector.this.mDeferConfirmSingleTap = true;
                        return;
                    } else {
                        GestureDetector.this.mDoubleTapListener.onSingleTapConfirmed(GestureDetector.this.mCurrentDownEvent);
                        return;
                    }
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown message ");
                    stringBuilder.append(msg);
                    throw new RuntimeException(stringBuilder.toString());
            }
        }
    }

    public static class SimpleOnGestureListener implements OnGestureListener, OnDoubleTapListener, OnContextClickListener {
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

        public boolean onContextClick(MotionEvent e) {
            return false;
        }
    }

    @Deprecated
    public GestureDetector(OnGestureListener listener, Handler handler) {
        this(null, listener, handler);
    }

    @Deprecated
    public GestureDetector(OnGestureListener listener) {
        this(null, listener, null);
    }

    public GestureDetector(Context context, OnGestureListener listener) {
        this(context, listener, null);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler) {
        this.mInputEventConsistencyVerifier = InputEventConsistencyVerifier.isInstrumentationEnabled() ? new InputEventConsistencyVerifier(this, 0) : null;
        if (handler != null) {
            this.mHandler = new GestureHandler(handler);
        } else {
            this.mHandler = new GestureHandler();
        }
        this.mListener = listener;
        if (listener instanceof OnDoubleTapListener) {
            setOnDoubleTapListener((OnDoubleTapListener) listener);
        }
        if (listener instanceof OnContextClickListener) {
            setContextClickListener((OnContextClickListener) listener);
        }
        init(context);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler, boolean unused) {
        this(context, listener, handler);
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

    public void setContextClickListener(OnContextClickListener onContextClickListener) {
        this.mContextClickListener = onContextClickListener;
    }

    public void setIsLongpressEnabled(boolean isLongpressEnabled) {
        this.mIsLongpressEnabled = isLongpressEnabled;
    }

    public boolean isLongpressEnabled() {
        return this.mIsLongpressEnabled;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int i;
        MotionEvent motionEvent = ev;
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTouchEvent(motionEvent, 0);
        }
        int action = ev.getAction();
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        boolean pointerUp = (action & 255) == 6;
        int skipIndex = pointerUp ? ev.getActionIndex() : -1;
        boolean isGeneratedGesture = (ev.getFlags() & 8) != 0;
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
        int deltaY;
        int slopSquare;
        switch (action & 255) {
            case 0:
                z = pointerUp;
                i2 = skipIndex;
                if (this.mDoubleTapListener != null) {
                    boolean hadTapMessage = this.mHandler.hasMessages(3);
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
                this.mDeferConfirmSingleTap = false;
                if (this.mIsLongpressEnabled) {
                    this.mHandler.removeMessages(2);
                    boolean z2 = isGeneratedGesture;
                    this.mHandler.sendEmptyMessageAtTime(2, this.mCurrentDownEvent.getDownTime() + ((long) LONGPRESS_TIMEOUT));
                }
                this.mHandler.sendEmptyMessageAtTime(1, this.mCurrentDownEvent.getDownTime() + ((long) TAP_TIMEOUT));
                handled |= this.mListener.onDown(motionEvent);
                break;
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
                } else if (this.mAlwaysInTapRegion && !this.mIgnoreNextUpEvent) {
                    handled = this.mListener.onSingleTapUp(motionEvent);
                    if (this.mDeferConfirmSingleTap && this.mDoubleTapListener != null) {
                        this.mDoubleTapListener.onSingleTapConfirmed(motionEvent);
                    }
                } else if (!this.mIgnoreNextUpEvent) {
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    skipIndex = motionEvent.getPointerId(0);
                    velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumFlingVelocity);
                    float velocityY = velocityTracker.getYVelocity(skipIndex);
                    float velocityX = velocityTracker.getXVelocity(skipIndex);
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
                this.mDeferConfirmSingleTap = false;
                this.mIgnoreNextUpEvent = false;
                this.mHandler.removeMessages(1);
                this.mHandler.removeMessages(true);
                break;
            case 2:
                z = pointerUp;
                i2 = skipIndex;
                if (this.mInLongPress == 0 && this.mInContextClick == 0) {
                    action = this.mLastFocusX - focusX;
                    pointerUp = this.mLastFocusY - focusY;
                    if (!this.mIsDoubleTapping) {
                        if (!this.mAlwaysInTapRegion) {
                            if (Math.abs(action) >= 1.0f || Math.abs(pointerUp) >= 1.0f) {
                                handled = this.mListener.onScroll(this.mCurrentDownEvent, motionEvent, action, pointerUp);
                                this.mLastFocusX = focusX;
                                this.mLastFocusY = focusY;
                                break;
                            }
                        }
                        skipIndex = (int) (focusX - this.mDownFocusX);
                        deltaY = (int) (focusY - this.mDownFocusY);
                        int distance = (skipIndex * skipIndex) + (deltaY * deltaY);
                        slopSquare = isGeneratedGesture ? 0 : this.mTouchSlopSquare;
                        if (distance > slopSquare) {
                            boolean slopSquare2 = this.mListener.onScroll(this.mCurrentDownEvent, motionEvent, action, pointerUp);
                            this.mLastFocusX = focusX;
                            this.mLastFocusY = focusY;
                            this.mAlwaysInTapRegion = false;
                            this.mHandler.removeMessages(3);
                            this.mHandler.removeMessages(1);
                            this.mHandler.removeMessages(2);
                            handled = slopSquare2;
                        } else {
                            int i3 = skipIndex;
                        }
                        if (distance > (isGeneratedGesture ? 0 : this.mDoubleTapTouchSlopSquare)) {
                            this.mAlwaysInBiggerTapRegion = false;
                            break;
                        }
                    }
                    handled = false | this.mDoubleTapListener.onDoubleTapEvent(motionEvent);
                    break;
                }
                break;
            case 3:
                z = pointerUp;
                i2 = skipIndex;
                cancel();
                break;
            case 5:
                z = pointerUp;
                i2 = skipIndex;
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
                this.mVelocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumFlingVelocity);
                slopSquare = ev.getActionIndex();
                deltaY = motionEvent.getPointerId(slopSquare);
                float x1 = this.mVelocityTracker.getXVelocity(deltaY);
                int i4 = action;
                action = this.mVelocityTracker.getYVelocity(deltaY);
                int i5 = 0;
                while (true) {
                    z = pointerUp;
                    int i6 = i5;
                    int y1;
                    int i7;
                    int i8;
                    if (i6 >= count) {
                        y1 = action;
                        i7 = slopSquare;
                        i2 = skipIndex;
                        i8 = deltaY;
                        break;
                    }
                    if (i6 == slopSquare) {
                        y1 = action;
                        i7 = slopSquare;
                        i2 = skipIndex;
                        i8 = deltaY;
                    } else {
                        i7 = slopSquare;
                        slopSquare = motionEvent.getPointerId(i6);
                        i2 = skipIndex;
                        i8 = deltaY;
                        if ((this.mVelocityTracker.getXVelocity(slopSquare) * x1) + (this.mVelocityTracker.getYVelocity(slopSquare) * action) < 0.0f) {
                            y1 = action;
                            this.mVelocityTracker.clear();
                            break;
                        }
                        y1 = action;
                    }
                    i5 = i6 + 1;
                    pointerUp = z;
                    slopSquare = i7;
                    skipIndex = i2;
                    deltaY = i8;
                    action = y1;
                }
            default:
                break;
        }
        if (!(handled || this.mInputEventConsistencyVerifier == null)) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(motionEvent, 0);
        }
        return handled;
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onGenericMotionEvent(ev, 0);
        }
        int actionButton = ev.getActionButton();
        switch (ev.getActionMasked()) {
            case 11:
                if (!(this.mContextClickListener == null || this.mInContextClick || this.mInLongPress || ((actionButton != 32 && actionButton != 2) || !this.mContextClickListener.onContextClick(ev)))) {
                    this.mInContextClick = true;
                    this.mHandler.removeMessages(2);
                    this.mHandler.removeMessages(3);
                    return true;
                }
            case 12:
                if (this.mInContextClick && (actionButton == 32 || actionButton == 2)) {
                    this.mInContextClick = false;
                    this.mIgnoreNextUpEvent = true;
                    break;
                }
        }
        return false;
    }

    private void cancel() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = null;
        this.mIsDoubleTapping = false;
        this.mStillDown = false;
        this.mAlwaysInTapRegion = false;
        this.mAlwaysInBiggerTapRegion = false;
        this.mDeferConfirmSingleTap = false;
        this.mInLongPress = false;
        this.mInContextClick = false;
        this.mIgnoreNextUpEvent = false;
    }

    private void cancelTaps() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        this.mIsDoubleTapping = false;
        this.mAlwaysInTapRegion = false;
        this.mAlwaysInBiggerTapRegion = false;
        this.mDeferConfirmSingleTap = false;
        this.mInLongPress = false;
        this.mInContextClick = false;
        this.mIgnoreNextUpEvent = false;
    }

    private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp, MotionEvent secondDown) {
        boolean z = false;
        if (!this.mAlwaysInBiggerTapRegion) {
            return false;
        }
        long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
        if (deltaTime > ((long) DOUBLE_TAP_TIMEOUT) || deltaTime < ((long) DOUBLE_TAP_MIN_TIME)) {
            return false;
        }
        int deltaX = ((int) firstDown.getX()) - ((int) secondDown.getX());
        int deltaY = ((int) firstDown.getY()) - ((int) secondDown.getY());
        if ((deltaX * deltaX) + (deltaY * deltaY) < ((firstDown.getFlags() & 8) != 0 ? 0 : this.mDoubleTapSlopSquare)) {
            z = true;
        }
        return z;
    }

    private void dispatchLongPress() {
        this.mHandler.removeMessages(3);
        this.mDeferConfirmSingleTap = false;
        this.mInLongPress = true;
        this.mListener.onLongPress(this.mCurrentDownEvent);
    }
}
