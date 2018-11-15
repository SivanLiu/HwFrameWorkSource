package com.android.server.gesture;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import java.util.ArrayList;

public class GestureNavBaseStrategy {
    public static final int GESTURE_FAIL_REASON_ANGLE_TOO_LARGE = 2;
    public static final int GESTURE_FAIL_REASON_MULTI_TOUCH = 6;
    public static final int GESTURE_FAIL_REASON_SLIDE_CANCEL = 5;
    public static final int GESTURE_FAIL_REASON_SLIDE_TOO_SHORT = 4;
    public static final int GESTURE_FAIL_REASON_TIMEOUT = 1;
    public static final int GESTURE_FAIL_REASON_UNKNOWN = 0;
    public static final int GESTURE_FAIL_REASON_UP_IN_REGION = 3;
    private static final int MAX_PENDING_DATA_SIZE = 30;
    private static final int MSG_COMPENSATE_MOVE_EVENT = 2;
    private static final int MSG_COMPENSATE_SINGLE_EVENT = 1;
    private static final int MSG_PRECHECK_FAST_SLIDING_TIMEOUT = 4;
    private static final int MSG_SEND_KEY_EVENT = 3;
    private static final int PENDING_DATA_SIZE_TOO_SMALL = 4;
    private final int SLIDE_EXCEED_MAJOR_AXIS_THRESHOLD;
    private final int SLIDE_EXCEED_MINOR_AXIS_THRESHOLD;
    private final int SLIDE_NOT_EXCEED_THRESHOLD;
    private boolean mCheckDistanceY;
    protected Context mContext;
    private int mDeviceId;
    protected int mDisplayHeight;
    protected int mDisplayWidth;
    protected boolean mFastSlideMajorAxisChecking;
    protected final float mFastVelocityThreshold;
    protected boolean mGestureEnd;
    protected boolean mGestureFailed;
    protected int mGestureFailedReason;
    protected boolean mGestureSlowProcessStarted;
    protected boolean mGestureSuccessFinished;
    protected boolean mGuestureReallyStarted;
    private boolean mHasChangeTimeoutWhenSlideOut;
    private boolean mHasCheckAngle;
    private boolean mHasCheckTimeout;
    protected boolean mHasMultiTouched;
    protected boolean mIsFastSlideGesture;
    protected boolean mKeyguardShowing;
    private float mLastPendingDistance;
    protected Looper mLooper;
    protected final int mMaximumVelocity;
    private int mMoveOutTimeThreshold;
    protected int mNavId;
    protected ArrayList<Float> mPendingMoveDistance;
    protected ArrayList<PointF> mPendingMovePoints;
    protected boolean mPreCheckingFastSlideTimeout;
    private int mSlideOutThresholdMajorAxis;
    private int mSlideOutThresholdMinorAxis;
    private int mSlideUpThreshold;
    private int mSource;
    private Handler mStrategyHandler;
    protected float mTouchDownRawX;
    protected float mTouchDownRawY;
    private Rect mTouchDownRegion;
    protected long mTouchDownTime;
    protected long mTouchFailedTime;
    protected float mTouchUpRawX;
    protected float mTouchUpRawY;
    protected long mTouchUpTime;
    private boolean mUseProxyAngleStrategy;
    protected VelocityTracker mVelocityTracker;
    private float mXOffset;
    private float mYOffset;

    protected static final class PointEvent {
        public int action;
        public int deviceId;
        public long downTime;
        public long durationTime;
        public float endX;
        public float endY;
        public int source;
        public float startX;
        public float startY;

        public PointEvent(float _startX, float _startY, long _downTime, int _action, int _deviceId, int _source) {
            this.startX = _startX;
            this.startY = _startY;
            this.endX = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.endY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.downTime = _downTime;
            this.durationTime = 0;
            this.action = _action;
            this.deviceId = _deviceId;
            this.source = _source;
        }

        public PointEvent(float _startX, float _startY, float _endX, float _endY, long _time, int _deviceId, int _source) {
            this.startX = _startX;
            this.startY = _startY;
            this.endX = _endX;
            this.endY = _endY;
            this.downTime = 0;
            this.durationTime = _time;
            this.action = 0;
            this.deviceId = _deviceId;
            this.source = _source;
        }
    }

    private final class StrategyHandler extends Handler {
        public StrategyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    GestureNavBaseStrategy.this.compensateSingleEvent((PointEvent) msg.obj);
                    return;
                case 2:
                    GestureNavBaseStrategy.this.checkCompensateEvent((PointEvent) msg.obj, null, false);
                    return;
                case 3:
                    GestureUtils.sendKeyEvent(msg.arg1);
                    return;
                case 4:
                    if (GestureNavConst.DEBUG) {
                        Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "precheck fast sliding timeout");
                    }
                    GestureNavBaseStrategy.this.mPreCheckingFastSlideTimeout = true;
                    return;
                default:
                    return;
            }
        }
    }

    public GestureNavBaseStrategy(int navId, Context context) {
        this(navId, context, Looper.getMainLooper());
    }

    public GestureNavBaseStrategy(int navId, Context context, Looper looper) {
        boolean z = true;
        this.mTouchDownRegion = new Rect();
        this.mDeviceId = 0;
        this.mSource = 4098;
        this.mPendingMoveDistance = new ArrayList();
        this.mPendingMovePoints = new ArrayList();
        this.SLIDE_NOT_EXCEED_THRESHOLD = 0;
        this.SLIDE_EXCEED_MAJOR_AXIS_THRESHOLD = 1;
        this.SLIDE_EXCEED_MINOR_AXIS_THRESHOLD = 2;
        this.mNavId = navId;
        this.mContext = context;
        this.mLooper = looper;
        if (this.mNavId != 3) {
            z = false;
        }
        this.mCheckDistanceY = z;
        this.mStrategyHandler = new StrategyHandler(looper);
        this.mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        this.mFastVelocityThreshold = ((float) ViewConfiguration.get(context).getScaledMinimumFlingVelocity()) * 10.0f;
    }

    protected void sendKeyEvent(int keycode) {
        sendKeyEvent(keycode, false);
    }

    protected void sendKeyEvent(int keycode, boolean async) {
        if (async) {
            this.mStrategyHandler.sendMessage(this.mStrategyHandler.obtainMessage(3, keycode, 0, null));
            return;
        }
        GestureUtils.sendKeyEvent(keycode);
    }

    public void updateDeviceState(boolean keyguardShowing) {
        this.mKeyguardShowing = keyguardShowing;
    }

    public void onNavCreate(GestureNavView view) {
    }

    public void onNavUpdate() {
    }

    public void onNavDestroy() {
    }

    public void updateConfig(int displayWidth, int displayHeight, Rect r) {
        this.mDisplayWidth = displayWidth;
        this.mDisplayHeight = displayHeight;
        this.mTouchDownRegion.set(r);
        int windowThreshold = this.mCheckDistanceY ? this.mTouchDownRegion.height() : this.mTouchDownRegion.width();
        this.mSlideOutThresholdMajorAxis = ((int) (((float) windowThreshold) * 1.2f)) + slipOutThresholdOffset();
        this.mSlideOutThresholdMinorAxis = ((int) (((float) windowThreshold) * 1.6f)) + slipOutThresholdOffset();
        this.mSlideUpThreshold = ((int) (((float) windowThreshold) * 2.0f)) + slipOverThresholdOffset();
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "navId:" + this.mNavId + ", width:" + this.mDisplayWidth + ", height:" + this.mDisplayHeight + ", region:" + r + ", slipOutMajor:" + this.mSlideOutThresholdMajorAxis + ", slipOutMinor:" + this.mSlideOutThresholdMinorAxis + ", slipUp:" + this.mSlideUpThreshold);
        }
    }

    protected Rect getRegion() {
        return this.mTouchDownRegion;
    }

    protected boolean frameContainsPoint(float x, float y) {
        return this.mTouchDownRegion.contains((int) x, (int) y);
    }

    protected int slipOutThresholdOffset() {
        return 0;
    }

    protected int slipOverThresholdOffset() {
        return 0;
    }

    protected int moveOutAngleThreshold() {
        return 70;
    }

    protected int moveOutTimeThreshold(boolean useMinTime) {
        int i = 120;
        if (useMinTime) {
            return 120;
        }
        if (this.mCheckDistanceY) {
            i = GestureNavConst.GESTURE_MOVE_TIME_THRESHOLD_4;
        }
        return i;
    }

    protected int distanceExceedThreshold(float distanceX, float distanceY) {
        float distanceMajor = this.mCheckDistanceY ? distanceY : distanceX;
        float distanceMinor = this.mCheckDistanceY ? distanceX : distanceY;
        if (distanceMajor > ((float) this.mSlideOutThresholdMajorAxis)) {
            return 1;
        }
        if (distanceMinor > ((float) this.mSlideOutThresholdMinorAxis)) {
            return 2;
        }
        return 0;
    }

    protected float fastVelocityTheshold() {
        return this.mCheckDistanceY ? 1500.0f : 6000.0f;
    }

    protected int fastTimeoutTheshold() {
        return this.mCheckDistanceY ? GestureNavConst.GESTURE_MOVE_TIME_THRESHOLD_4 : 200;
    }

    protected void setUseProxyAngleStrategy(boolean useProxyAngleStrategy) {
        this.mUseProxyAngleStrategy = useProxyAngleStrategy;
    }

    protected boolean shouldDropMultiTouch() {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case 0:
                handleActionDown(event);
                break;
            case 1:
            case 3:
                handleActionUp(event, action);
                break;
            case 2:
                handleActionMove(event);
                break;
            case 5:
                handleMultiTouchDown(event);
                break;
        }
        return this.mGestureFailed;
    }

    private void handleActionDown(MotionEvent event) {
        gestureReset();
        this.mTouchDownTime = event.getEventTime();
        this.mTouchDownRawX = event.getRawX();
        this.mTouchDownRawY = event.getRawY();
        this.mXOffset = event.getX() - this.mTouchDownRawX;
        this.mYOffset = event.getY() - this.mTouchDownRawY;
        this.mDeviceId = event.getDeviceId();
        this.mSource = event.getSource();
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "Down rawX=" + this.mTouchDownRawX + ", rawY=" + this.mTouchDownRawY + ", rect=" + getRegion() + ", navId=" + this.mNavId + ", xOffset=" + this.mXOffset + ", yOffset=" + this.mYOffset);
        }
        this.mVelocityTracker = VelocityTracker.obtain();
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
        onGestureStarted(this.mTouchDownRawX, this.mTouchDownRawY);
    }

    private void handleMultiTouchDown(MotionEvent event) {
        if (!this.mHasMultiTouched) {
            this.mHasMultiTouched = true;
        }
        if (!this.mGestureFailed && (this.mGuestureReallyStarted ^ 1) != 0 && shouldDropMultiTouch()) {
            int count = event.getPointerCount();
            if (count > 1) {
                Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "Multi touch pointer down, count=" + count);
                gestureFailed(6, false, " ,pointerCount=" + count, event.getEventTime());
            }
        }
    }

    private void handleActionMove(MotionEvent event) {
        double angle;
        long time;
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        float offsetX = rawX - this.mTouchDownRawX;
        float offsetY = rawY - this.mTouchDownRawY;
        float distanceX = distanceX(offsetX);
        float distanceY = distanceY(offsetY);
        float distance = diff(distanceX, distanceY);
        if (GestureNavConst.DEBUG_ALL) {
            Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "Move rawX=" + rawX + ", rawY=" + rawY + ", distance=" + distance);
        }
        if (!(this.mGestureFailed || (this.mGestureSlowProcessStarted ^ 1) == 0)) {
            recordPendingMoveDatas(event, distance, rawX, rawY);
        }
        if (this.mPreCheckingFastSlideTimeout && this.mFastSlideMajorAxisChecking) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "cancel fast check when move arrived after timeout");
            }
            this.mFastSlideMajorAxisChecking = false;
        }
        int slipOutMode = 0;
        if (!(this.mUseProxyAngleStrategy || (this.mGestureFailed ^ 1) == 0 || (this.mHasCheckAngle ^ 1) == 0)) {
            slipOutMode = distanceExceedThreshold(distanceX, distanceY);
            if (slipOutMode == 1) {
                this.mHasCheckAngle = true;
                angle = angle(Math.abs(offsetX), Math.abs(offsetY));
                float velocity = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                long delayTime = 0;
                if (angle > ((double) moveOutAngleThreshold())) {
                    gestureFailed(2, false, ", angle=" + angle, event.getEventTime());
                } else if (!this.mHasCheckTimeout) {
                    this.mHasCheckTimeout = true;
                    VelocityTracker vt = this.mVelocityTracker;
                    if (vt != null) {
                        vt.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                        velocity = getVelocity(vt);
                    }
                    if (velocity > fastVelocityTheshold()) {
                        delayTime = ((long) fastTimeoutTheshold()) - (event.getEventTime() - this.mTouchDownTime);
                        if (delayTime > 0) {
                            this.mFastSlideMajorAxisChecking = true;
                            this.mStrategyHandler.sendEmptyMessageDelayed(4, delayTime);
                        }
                    }
                }
                Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "angle:" + angle + ", velocity:" + velocity + ", checkTimeout:" + this.mHasCheckTimeout + ", fastChecking:" + this.mFastSlideMajorAxisChecking + ", delayTime:" + delayTime + ", slipOutMode:" + slipOutMode + ", distanceX:" + distanceX + ", distanceY:" + distanceY);
                if (slipOutMode == 2 && (this.mHasChangeTimeoutWhenSlideOut ^ 1) != 0) {
                    this.mHasChangeTimeoutWhenSlideOut = true;
                    this.mMoveOutTimeThreshold = moveOutTimeThreshold(true);
                }
                if (!(this.mGestureFailed || (this.mHasCheckTimeout ^ 1) == 0)) {
                    time = event.getEventTime() - this.mTouchDownTime;
                    if (time > ((long) this.mMoveOutTimeThreshold)) {
                        this.mHasCheckTimeout = true;
                        angle = 0.0d;
                        if (frameContainsPoint(rawX, rawY)) {
                            gestureFailed(1, false, ", time=" + time + "ms, point(" + rawX + ", " + rawY + ")", event.getEventTime());
                        } else if (!this.mHasCheckAngle) {
                            this.mHasCheckAngle = true;
                            angle = angle(Math.abs(offsetX), Math.abs(offsetY));
                            if (angle > ((double) moveOutAngleThreshold())) {
                                gestureFailed(2, false, ", timeout angle=" + angle, event.getEventTime());
                            }
                        }
                        Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "move out time:" + time + "ms, threshold:" + this.mMoveOutTimeThreshold + "ms, point(" + rawX + ", " + rawY + "), angle:" + angle);
                    }
                }
                if (!this.mGuestureReallyStarted && gestureReady()) {
                    Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture really started");
                    this.mGuestureReallyStarted = true;
                    onGestureReallyStarted();
                }
                if (this.mGuestureReallyStarted && (this.mFastSlideMajorAxisChecking ^ 1) != 0) {
                    if (!this.mGestureSlowProcessStarted) {
                        Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture slow process started");
                        this.mGestureSlowProcessStarted = true;
                        onGestureSlowProcessStarted(this.mPendingMoveDistance);
                    }
                    onGestureSlowProcess(distance, offsetX, offsetY);
                    return;
                }
            }
        }
        if (this.mUseProxyAngleStrategy && (this.mHasCheckAngle ^ 1) != 0) {
            this.mHasCheckAngle = true;
        }
        this.mHasChangeTimeoutWhenSlideOut = true;
        this.mMoveOutTimeThreshold = moveOutTimeThreshold(true);
        time = event.getEventTime() - this.mTouchDownTime;
        if (time > ((long) this.mMoveOutTimeThreshold)) {
            this.mHasCheckTimeout = true;
            angle = 0.0d;
            if (frameContainsPoint(rawX, rawY)) {
                gestureFailed(1, false, ", time=" + time + "ms, point(" + rawX + ", " + rawY + ")", event.getEventTime());
            } else if (this.mHasCheckAngle) {
                this.mHasCheckAngle = true;
                angle = angle(Math.abs(offsetX), Math.abs(offsetY));
                if (angle > ((double) moveOutAngleThreshold())) {
                    gestureFailed(2, false, ", timeout angle=" + angle, event.getEventTime());
                }
            }
            Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "move out time:" + time + "ms, threshold:" + this.mMoveOutTimeThreshold + "ms, point(" + rawX + ", " + rawY + "), angle:" + angle);
        }
        Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture really started");
        this.mGuestureReallyStarted = true;
        onGestureReallyStarted();
        if (this.mGuestureReallyStarted) {
        }
    }

    private void handleActionUp(MotionEvent event, int action) {
        this.mTouchUpTime = event.getEventTime();
        this.mTouchUpRawX = event.getRawX();
        this.mTouchUpRawY = event.getRawY();
        this.mStrategyHandler.removeMessages(4);
        float velocity = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
            this.mVelocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
            velocity = getVelocity(this.mVelocityTracker);
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
        if (this.mGestureFailed && action == 1) {
            Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "Receive up after gesture failed");
            onGestureUpArrivedAfterFailed(this.mTouchUpRawX, this.mTouchUpRawY, this.mTouchDownTime);
        }
        if (!(this.mGestureFailed || (this.mGuestureReallyStarted ^ 1) == 0 || !frameContainsPoint(this.mTouchUpRawX, this.mTouchUpRawY))) {
            gestureFailed(3, true, "", this.mTouchUpTime);
        }
        float distance = diff(this.mTouchDownRawX, this.mTouchDownRawY, this.mTouchUpRawX, this.mTouchUpRawY);
        if (!(this.mGestureFailed || (this.mGuestureReallyStarted ^ 1) == 0 || distance >= ((float) this.mSlideUpThreshold))) {
            gestureFailed(4, true, ", distance:" + distance, this.mTouchUpTime);
        }
        long durationTime = this.mTouchUpTime - this.mTouchDownTime;
        if (!this.mGestureFailed && this.mGuestureReallyStarted) {
            if (distance >= ((float) this.mSlideUpThreshold) || durationTime <= 150) {
                if (action == 3) {
                }
            }
            gestureFailed(5, true, ", duration:" + durationTime + "ms, distance:" + distance + ", action:" + action, this.mTouchUpTime);
        }
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "Up rawX=" + this.mTouchUpRawX + ", rawY=" + this.mTouchUpRawY + ", failed=" + this.mGestureFailed + ", rellayStarted=" + this.mGuestureReallyStarted + ", velocity=" + velocity + ", distance=" + distance + ", duration=" + durationTime);
        }
        gestureFinished(distance, durationTime, velocity);
        gestureEnd(action);
    }

    private void gestureReset() {
        this.mGestureFailed = false;
        this.mHasCheckTimeout = false;
        this.mHasCheckAngle = false;
        this.mGuestureReallyStarted = false;
        this.mGestureSlowProcessStarted = false;
        this.mGestureEnd = false;
        this.mGestureSuccessFinished = false;
        this.mGestureFailedReason = 0;
        this.mIsFastSlideGesture = false;
        this.mPreCheckingFastSlideTimeout = false;
        this.mFastSlideMajorAxisChecking = false;
        this.mUseProxyAngleStrategy = false;
        this.mHasChangeTimeoutWhenSlideOut = false;
        this.mHasMultiTouched = false;
        this.mMoveOutTimeThreshold = moveOutTimeThreshold(false);
        this.mStrategyHandler.removeMessages(4);
        this.mLastPendingDistance = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        this.mPendingMoveDistance.clear();
        this.mPendingMovePoints.clear();
    }

    private void gestureFailed(int reason, boolean failedInEventEnd, String appendStr, long failedTime) {
        Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, translateFailedReason(reason) + appendStr);
        this.mGestureFailedReason = reason;
        this.mTouchFailedTime = failedTime;
        this.mGestureFailed = true;
        onGestureFailed(this.mGestureFailedReason, failedInEventEnd);
    }

    protected boolean gestureReady() {
        return (this.mGestureFailed || !this.mHasCheckTimeout) ? false : this.mHasCheckAngle;
    }

    private void gestureFinished(float distance, long durationTime, float velocity) {
        if (!this.mGestureFailed) {
            if (!this.mGestureSlowProcessStarted && (this.mFastSlideMajorAxisChecking || velocity > 6000.0f)) {
                this.mIsFastSlideGesture = true;
            }
            Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture success, reallyStarted=" + this.mGuestureReallyStarted + ", slowStarted=" + this.mGestureSlowProcessStarted + ", velocity=" + velocity + ", fastChecking=" + this.mFastSlideMajorAxisChecking + ", fastGesture=" + this.mIsFastSlideGesture);
            this.mGestureSuccessFinished = true;
            onGestureSuccessFinished(distance, durationTime, velocity, this.mIsFastSlideGesture);
        }
    }

    private void gestureEnd(int action) {
        this.mGestureEnd = true;
        this.mPendingMoveDistance.clear();
        this.mPendingMovePoints.clear();
        onGestureEnd(action);
    }

    protected void onGestureStarted(float rawX, float rawY) {
    }

    protected void onGestureReallyStarted() {
    }

    protected void onGestureSlowProcessStarted(ArrayList<Float> arrayList) {
    }

    protected void onGestureSlowProcess(float distance, float offsetX, float offsetY) {
    }

    protected void onGestureFailed(int reason, boolean failedInEventEnd) {
        switch (reason) {
            case 1:
            case 2:
                compensateBatchDownEvent(this.mTouchDownRawX, this.mTouchDownRawY, this.mPendingMovePoints, this.mTouchDownTime, this.mTouchFailedTime - this.mTouchDownTime, this.mDeviceId, this.mSource);
                return;
            case 3:
            case 4:
                long durationTime = this.mTouchFailedTime - this.mTouchDownTime;
                if (durationTime < 500) {
                    checkCompensateEvent(new PointEvent(this.mTouchDownRawX, this.mTouchDownRawY, this.mTouchUpRawX, this.mTouchUpRawY, durationTime, this.mDeviceId, this.mSource), this.mPendingMovePoints, this.mHasMultiTouched);
                    return;
                } else {
                    Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "no need compensate as duration time is too long, duration:" + durationTime);
                    return;
                }
            case 5:
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture cancel, no need process");
                    return;
                }
                return;
            case 6:
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "multi touch, no need process");
                    return;
                }
                return;
            default:
                return;
        }
    }

    protected void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture) {
    }

    protected void onGestureEnd(int action) {
    }

    protected void onGestureUpArrivedAfterFailed(float rawX, float rawY, long pointDownTime) {
        this.mStrategyHandler.sendMessage(this.mStrategyHandler.obtainMessage(1, 0, 0, new PointEvent(rawX, rawY, pointDownTime, 1, this.mDeviceId, this.mSource)));
    }

    private void compensateSingleEvent(PointEvent p) {
        long eventTime;
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "compensateSingleEvent x=" + p.startX + ", y=" + p.startY + ", action=" + p.action);
        }
        if (p.action == 0) {
            eventTime = p.downTime;
        } else {
            eventTime = SystemClock.uptimeMillis();
        }
        GestureUtils.injectMotionEvent(p.action, p.downTime, eventTime, p.startX, p.startY, p.deviceId, p.source);
    }

    private void compensateBatchDownEvent(float startX, float startY, ArrayList<PointF> pendingMovePoints, long downTime, long durationTime, int deviceId, int source) {
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "compensateBatchDownEvent x=" + startX + ", y=" + startY + ", durationTime=" + durationTime);
        }
        GestureUtils.injectDownWithBatchMoveEvent(downTime, startX, startY, pendingMovePoints, durationTime, deviceId, source);
    }

    private void checkCompensateEvent(PointEvent p, ArrayList<PointF> pendingMovePoints, boolean hasMultiTouched) {
        int distance = distance(p.startX, p.startY, p.endX, p.endY);
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "checkCompensateEvent distance=" + distance);
        }
        if (distance < 15) {
            compensateTapEvent(p);
        } else {
            compensateMoveEvent(p, pendingMovePoints, hasMultiTouched);
        }
    }

    private void compensateTapEvent(PointEvent p) {
        GestureUtils.sendTap(p.startX, p.startY, p.deviceId, p.source);
    }

    private void compensateMoveEvent(PointEvent p, ArrayList<PointF> pendingMovePoints, boolean hasMultiTouched) {
        ArrayList arrayList = null;
        boolean canUsePendingPoints = true;
        if (pendingMovePoints == null) {
            canUsePendingPoints = false;
        } else if (pendingMovePoints.size() >= 30 || pendingMovePoints.size() < 4) {
            canUsePendingPoints = false;
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "pending points size:" + pendingMovePoints.size());
            }
        }
        float f = p.startX;
        float f2 = p.startY;
        float f3 = p.endX;
        float f4 = p.endY;
        int i = (int) p.durationTime;
        int i2 = p.deviceId;
        int i3 = p.source;
        if (canUsePendingPoints) {
            arrayList = pendingMovePoints;
        }
        GestureUtils.sendSwipe(f, f2, f3, f4, i, i2, i3, arrayList, hasMultiTouched);
    }

    private void recordPendingMoveDatas(MotionEvent event, float distance, float rawX, float rawY) {
        if (this.mPendingMoveDistance.size() < 30) {
            int dist = (int) Math.abs(distance - this.mLastPendingDistance);
            if (dist > 150) {
                int times = dist / 150;
                int dt = distance > this.mLastPendingDistance ? 150 : -150;
                for (int i = 0; i < times; i++) {
                    this.mPendingMoveDistance.add(Float.valueOf(distance - ((float) ((times - i) * dt))));
                }
            }
            this.mPendingMoveDistance.add(Float.valueOf(distance));
            this.mLastPendingDistance = distance;
        }
        if (this.mPendingMovePoints.size() < 30) {
            int historySize = event.getHistorySize();
            for (int h = 0; h < historySize; h++) {
                float rx = event.getHistoricalX(h) - this.mXOffset;
                float ry = event.getHistoricalY(h) - this.mYOffset;
                this.mPendingMovePoints.add(new PointF(rx, ry));
                if (GestureNavConst.DEBUG_ALL) {
                    Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "record rx=" + rx + ", ry=" + ry);
                }
            }
            this.mPendingMovePoints.add(new PointF(rawX, rawY));
        }
    }

    private static final int distance(float startX, float startY, float endX, float endY) {
        float dx = startX - endX;
        float dy = startY - endY;
        return (int) Math.sqrt((double) ((dx * dx) + (dy * dy)));
    }

    private float distanceX(float offsetX) {
        return Math.abs(offsetX);
    }

    private float distanceY(float offsetY) {
        return Math.abs(offsetY);
    }

    private float diff(float diffX, float diffY) {
        return this.mCheckDistanceY ? diffY : diffX;
    }

    private float diff(float startX, float startY, float endX, float endY) {
        return this.mCheckDistanceY ? distanceY(endY - startY) : distanceX(endX - startX);
    }

    private double angle(float distanceX, float distanceY) {
        return GestureUtils.angle(distanceX, distanceY, this.mCheckDistanceY);
    }

    private float getVelocity(VelocityTracker velocityTracker) {
        return Math.abs(this.mCheckDistanceY ? velocityTracker.getYVelocity() : velocityTracker.getXVelocity());
    }

    protected String translateFailedReason(int reason) {
        switch (reason) {
            case 1:
                return "Gesture failed as move out region timeout";
            case 2:
                return "Gesture failed as move out gesture angle too large";
            case 3:
                return "Gesture failed as up in region(maybe sliping in region)";
            case 4:
                return "Gesture failed as sliding distance too short";
            case 5:
                return "Gesture failed as sliding rollback or canceled";
            case 6:
                return "Gesture failed as multi finger sliding";
            default:
                return "Gesture failed as other";
        }
    }
}
