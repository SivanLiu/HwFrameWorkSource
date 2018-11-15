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
        this.mTouchDownRegion = new Rect();
        boolean z = false;
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
        if (this.mNavId == 3) {
            z = true;
        }
        this.mCheckDistanceY = z;
        this.mStrategyHandler = new StrategyHandler(looper);
        this.mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        this.mFastVelocityThreshold = 10.0f * ((float) ViewConfiguration.get(context).getScaledMinimumFlingVelocity());
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
            String str = GestureNavConst.TAG_GESTURE_STRATEGY;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("navId:");
            stringBuilder.append(this.mNavId);
            stringBuilder.append(", width:");
            stringBuilder.append(this.mDisplayWidth);
            stringBuilder.append(", height:");
            stringBuilder.append(this.mDisplayHeight);
            stringBuilder.append(", region:");
            stringBuilder.append(r);
            stringBuilder.append(", slipOutMajor:");
            stringBuilder.append(this.mSlideOutThresholdMajorAxis);
            stringBuilder.append(", slipOutMinor:");
            stringBuilder.append(this.mSlideOutThresholdMinorAxis);
            stringBuilder.append(", slipUp:");
            stringBuilder.append(this.mSlideUpThreshold);
            Log.d(str, stringBuilder.toString());
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
        return this.mCheckDistanceY ? 4500.0f : 6000.0f;
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
        if (action != 5) {
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
            }
        }
        handleMultiTouchDown(event);
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
            String str = GestureNavConst.TAG_GESTURE_STRATEGY;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Down rawX=");
            stringBuilder.append(this.mTouchDownRawX);
            stringBuilder.append(", rawY=");
            stringBuilder.append(this.mTouchDownRawY);
            stringBuilder.append(", rect=");
            stringBuilder.append(getRegion());
            stringBuilder.append(", navId=");
            stringBuilder.append(this.mNavId);
            stringBuilder.append(", xOffset=");
            stringBuilder.append(this.mXOffset);
            stringBuilder.append(", yOffset=");
            stringBuilder.append(this.mYOffset);
            Log.d(str, stringBuilder.toString());
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
        if (!this.mGestureFailed && !this.mGuestureReallyStarted && shouldDropMultiTouch()) {
            int pointerCount = event.getPointerCount();
            int count = pointerCount;
            if (pointerCount > 1) {
                String str = GestureNavConst.TAG_GESTURE_STRATEGY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Multi touch pointer down, count=");
                stringBuilder.append(count);
                Log.i(str, stringBuilder.toString());
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" ,pointerCount=");
                stringBuilder2.append(count);
                gestureFailed(6, false, stringBuilder2.toString(), event.getEventTime());
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x0194  */
    /* JADX WARNING: Removed duplicated region for block: B:92:? A:{SYNTHETIC, RETURN, SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x02a5  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleActionMove(MotionEvent event) {
        float offsetX;
        float offsetY;
        int slipOutMode;
        int slipOutMode2;
        float offsetX2;
        MotionEvent motionEvent = event;
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(motionEvent);
        }
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        float offsetX3 = rawX - this.mTouchDownRawX;
        float offsetY2 = rawY - this.mTouchDownRawY;
        float distanceX = distanceX(offsetX3);
        float distanceY = distanceY(offsetY2);
        float distance = diff(distanceX, distanceY);
        if (GestureNavConst.DEBUG_ALL) {
            String str = GestureNavConst.TAG_GESTURE_STRATEGY;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Move rawX=");
            stringBuilder.append(rawX);
            stringBuilder.append(", rawY=");
            stringBuilder.append(rawY);
            stringBuilder.append(", distance=");
            stringBuilder.append(distance);
            Log.d(str, stringBuilder.toString());
        }
        if (!(this.mGestureFailed || this.mGestureSlowProcessStarted)) {
            recordPendingMoveDatas(motionEvent, distance, rawX, rawY);
        }
        if (this.mPreCheckingFastSlideTimeout && this.mFastSlideMajorAxisChecking) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_STRATEGY, "cancel fast check when move arrived after timeout");
            }
            this.mFastSlideMajorAxisChecking = false;
        }
        if (this.mUseProxyAngleStrategy || this.mGestureFailed || this.mHasCheckAngle) {
            offsetX = offsetX3;
            offsetY = offsetY2;
            slipOutMode = 0;
        } else {
            int distanceExceedThreshold = distanceExceedThreshold(distanceX, distanceY);
            slipOutMode = distanceExceedThreshold;
            if (distanceExceedThreshold == 1) {
                long delayTime;
                int slipOutMode3;
                String str2;
                StringBuilder stringBuilder2;
                this.mHasCheckAngle = true;
                double angle = angle(Math.abs(offsetX3), Math.abs(offsetY2));
                int velocity = 0;
                StringBuilder stringBuilder3;
                if (angle > ((double) moveOutAngleThreshold())) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(", angle=");
                    stringBuilder3.append(angle);
                    offsetX = offsetX3;
                    offsetY = offsetY2;
                    offsetX3 = angle;
                    String stringBuilder4 = stringBuilder3.toString();
                    slipOutMode2 = slipOutMode;
                    gestureFailed(2, false, stringBuilder4, event.getEventTime());
                } else {
                    slipOutMode2 = slipOutMode;
                    offsetX = offsetX3;
                    offsetY = offsetY2;
                    offsetX3 = angle;
                    if (!this.mHasCheckTimeout) {
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
                            slipOutMode3 = velocity;
                            str2 = GestureNavConst.TAG_GESTURE_STRATEGY;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("angle:");
                            stringBuilder2.append(offsetX3);
                            stringBuilder2.append(", velocity:");
                            stringBuilder2.append(slipOutMode3);
                            stringBuilder2.append(", checkTimeout:");
                            stringBuilder2.append(this.mHasCheckTimeout);
                            stringBuilder2.append(", fastChecking:");
                            stringBuilder2.append(this.mFastSlideMajorAxisChecking);
                            stringBuilder2.append(", delayTime:");
                            stringBuilder2.append(delayTime);
                            stringBuilder2.append(", slipOutMode:");
                            stringBuilder2.append(slipOutMode2);
                            stringBuilder2.append(", distanceX:");
                            stringBuilder2.append(distanceX);
                            stringBuilder2.append(", distanceY:");
                            stringBuilder2.append(distanceY);
                            Log.i(str2, stringBuilder2.toString());
                            if (slipOutMode2 == 2 && !this.mHasChangeTimeoutWhenSlideOut) {
                                this.mHasChangeTimeoutWhenSlideOut = true;
                                this.mMoveOutTimeThreshold = moveOutTimeThreshold(true);
                            }
                            if (!this.mGestureFailed || this.mHasCheckTimeout) {
                                offsetX2 = offsetX;
                                distanceX = offsetY;
                            } else {
                                long eventTime = event.getEventTime() - this.mTouchDownTime;
                                long time = eventTime;
                                if (eventTime > ((long) this.mMoveOutTimeThreshold)) {
                                    double angle2;
                                    String str3;
                                    StringBuilder stringBuilder5;
                                    this.mHasCheckTimeout = true;
                                    float f;
                                    if (frameContainsPoint(rawX, rawY)) {
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append(", time=");
                                        stringBuilder3.append(time);
                                        stringBuilder3.append("ms, point(");
                                        stringBuilder3.append(rawX);
                                        stringBuilder3.append(", ");
                                        stringBuilder3.append(rawY);
                                        stringBuilder3.append(")");
                                        gestureFailed(1, false, stringBuilder3.toString(), event.getEventTime());
                                        f = distanceX;
                                        offsetX2 = offsetX;
                                        distanceX = offsetY;
                                    } else if (this.mHasCheckAngle) {
                                        offsetX2 = offsetX;
                                        distanceX = offsetY;
                                    } else {
                                        this.mHasCheckAngle = true;
                                        float offsetX4 = offsetX;
                                        float offsetY3 = offsetY;
                                        angle = angle(Math.abs(offsetX4), Math.abs(offsetY3));
                                        if (angle > ((double) moveOutAngleThreshold())) {
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append(", timeout angle=");
                                            stringBuilder3.append(angle);
                                            offsetX = angle;
                                            offsetX2 = offsetX4;
                                            distanceX = offsetY3;
                                            gestureFailed(2, false, stringBuilder3.toString(), event.getEventTime());
                                        } else {
                                            offsetX = angle;
                                            offsetX2 = offsetX4;
                                            f = distanceX;
                                            distanceX = offsetY3;
                                        }
                                        angle2 = offsetX;
                                        str3 = GestureNavConst.TAG_GESTURE_STRATEGY;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("move out time:");
                                        stringBuilder5.append(time);
                                        stringBuilder5.append("ms, threshold:");
                                        stringBuilder5.append(this.mMoveOutTimeThreshold);
                                        stringBuilder5.append("ms, point(");
                                        stringBuilder5.append(rawX);
                                        stringBuilder5.append(", ");
                                        stringBuilder5.append(rawY);
                                        stringBuilder5.append("), angle:");
                                        stringBuilder5.append(angle2);
                                        Log.i(str3, stringBuilder5.toString());
                                    }
                                    angle2 = 0.0d;
                                    str3 = GestureNavConst.TAG_GESTURE_STRATEGY;
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("move out time:");
                                    stringBuilder5.append(time);
                                    stringBuilder5.append("ms, threshold:");
                                    stringBuilder5.append(this.mMoveOutTimeThreshold);
                                    stringBuilder5.append("ms, point(");
                                    stringBuilder5.append(rawX);
                                    stringBuilder5.append(", ");
                                    stringBuilder5.append(rawY);
                                    stringBuilder5.append("), angle:");
                                    stringBuilder5.append(angle2);
                                    Log.i(str3, stringBuilder5.toString());
                                } else {
                                    offsetX2 = offsetX;
                                    distanceX = offsetY;
                                }
                            }
                            if (!this.mGuestureReallyStarted && gestureReady()) {
                                Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture really started");
                                this.mGuestureReallyStarted = true;
                                onGestureReallyStarted();
                            }
                            if (!this.mGuestureReallyStarted && !this.mFastSlideMajorAxisChecking) {
                                if (!this.mGestureSlowProcessStarted) {
                                    Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture slow process started");
                                    this.mGestureSlowProcessStarted = true;
                                    onGestureSlowProcessStarted(this.mPendingMoveDistance);
                                }
                                onGestureSlowProcess(distance, offsetX2, distanceX);
                                return;
                            }
                            return;
                        }
                    }
                }
                slipOutMode3 = velocity;
                delayTime = 0;
                str2 = GestureNavConst.TAG_GESTURE_STRATEGY;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("angle:");
                stringBuilder2.append(offsetX3);
                stringBuilder2.append(", velocity:");
                stringBuilder2.append(slipOutMode3);
                stringBuilder2.append(", checkTimeout:");
                stringBuilder2.append(this.mHasCheckTimeout);
                stringBuilder2.append(", fastChecking:");
                stringBuilder2.append(this.mFastSlideMajorAxisChecking);
                stringBuilder2.append(", delayTime:");
                stringBuilder2.append(delayTime);
                stringBuilder2.append(", slipOutMode:");
                stringBuilder2.append(slipOutMode2);
                stringBuilder2.append(", distanceX:");
                stringBuilder2.append(distanceX);
                stringBuilder2.append(", distanceY:");
                stringBuilder2.append(distanceY);
                Log.i(str2, stringBuilder2.toString());
                this.mHasChangeTimeoutWhenSlideOut = true;
                this.mMoveOutTimeThreshold = moveOutTimeThreshold(true);
                if (this.mGestureFailed) {
                }
                offsetX2 = offsetX;
                distanceX = offsetY;
                Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture really started");
                this.mGuestureReallyStarted = true;
                onGestureReallyStarted();
                if (!this.mGuestureReallyStarted) {
                    return;
                }
                return;
            }
            offsetX = offsetX3;
            offsetY = offsetY2;
        }
        if (this.mUseProxyAngleStrategy && !this.mHasCheckAngle) {
            this.mHasCheckAngle = true;
        }
        slipOutMode2 = slipOutMode;
        this.mHasChangeTimeoutWhenSlideOut = true;
        this.mMoveOutTimeThreshold = moveOutTimeThreshold(true);
        if (this.mGestureFailed) {
        }
        offsetX2 = offsetX;
        distanceX = offsetY;
        Log.i(GestureNavConst.TAG_GESTURE_STRATEGY, "gesture really started");
        this.mGuestureReallyStarted = true;
        onGestureReallyStarted();
        if (!this.mGuestureReallyStarted) {
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
        if (!(this.mGestureFailed || this.mGuestureReallyStarted || !frameContainsPoint(this.mTouchUpRawX, this.mTouchUpRawY))) {
            gestureFailed(3, true, "", this.mTouchUpTime);
        }
        float distance = diff(this.mTouchDownRawX, this.mTouchDownRawY, this.mTouchUpRawX, this.mTouchUpRawY);
        if (!(this.mGestureFailed || this.mGuestureReallyStarted || distance >= ((float) this.mSlideUpThreshold))) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(", distance:");
            stringBuilder.append(distance);
            gestureFailed(4, true, stringBuilder.toString(), this.mTouchUpTime);
        }
        long durationTime = this.mTouchUpTime - this.mTouchDownTime;
        if (!this.mGestureFailed && this.mGuestureReallyStarted && ((distance < ((float) this.mSlideUpThreshold) && durationTime > 150) || action == 3)) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(", duration:");
            stringBuilder2.append(durationTime);
            stringBuilder2.append("ms, distance:");
            stringBuilder2.append(distance);
            stringBuilder2.append(", action:");
            stringBuilder2.append(action);
            gestureFailed(5, true, stringBuilder2.toString(), this.mTouchUpTime);
        }
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_STRATEGY;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Up rawX=");
            stringBuilder3.append(this.mTouchUpRawX);
            stringBuilder3.append(", rawY=");
            stringBuilder3.append(this.mTouchUpRawY);
            stringBuilder3.append(", failed=");
            stringBuilder3.append(this.mGestureFailed);
            stringBuilder3.append(", rellayStarted=");
            stringBuilder3.append(this.mGuestureReallyStarted);
            stringBuilder3.append(", velocity=");
            stringBuilder3.append(velocity);
            stringBuilder3.append(", distance=");
            stringBuilder3.append(distance);
            stringBuilder3.append(", duration=");
            stringBuilder3.append(durationTime);
            Log.d(str, stringBuilder3.toString());
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
        String str = GestureNavConst.TAG_GESTURE_STRATEGY;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(translateFailedReason(reason));
        stringBuilder.append(appendStr);
        Log.i(str, stringBuilder.toString());
        this.mGestureFailedReason = reason;
        this.mTouchFailedTime = failedTime;
        this.mGestureFailed = true;
        onGestureFailed(this.mGestureFailedReason, failedInEventEnd);
    }

    protected boolean gestureReady() {
        return !this.mGestureFailed && this.mHasCheckTimeout && this.mHasCheckAngle;
    }

    private void gestureFinished(float distance, long durationTime, float velocity) {
        if (!this.mGestureFailed) {
            if (!this.mGestureSlowProcessStarted && (this.mFastSlideMajorAxisChecking || velocity > 6000.0f)) {
                this.mIsFastSlideGesture = true;
            }
            String str = GestureNavConst.TAG_GESTURE_STRATEGY;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("gesture success, reallyStarted=");
            stringBuilder.append(this.mGuestureReallyStarted);
            stringBuilder.append(", slowStarted=");
            stringBuilder.append(this.mGestureSlowProcessStarted);
            stringBuilder.append(", velocity=");
            stringBuilder.append(velocity);
            stringBuilder.append(", fastChecking=");
            stringBuilder.append(this.mFastSlideMajorAxisChecking);
            stringBuilder.append(", fastGesture=");
            stringBuilder.append(this.mIsFastSlideGesture);
            Log.i(str, stringBuilder.toString());
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
                }
                String str = GestureNavConst.TAG_GESTURE_STRATEGY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("no need compensate as duration time is too long, duration:");
                stringBuilder.append(durationTime);
                Log.i(str, stringBuilder.toString());
                return;
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
        long j;
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_STRATEGY;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("compensateSingleEvent x=");
            stringBuilder.append(p.startX);
            stringBuilder.append(", y=");
            stringBuilder.append(p.startY);
            stringBuilder.append(", action=");
            stringBuilder.append(p.action);
            Log.i(str, stringBuilder.toString());
        }
        if (p.action == 0) {
            j = p.downTime;
        } else {
            j = SystemClock.uptimeMillis();
        }
        GestureUtils.injectMotionEvent(p.action, p.downTime, j, p.startX, p.startY, p.deviceId, p.source);
    }

    private void compensateBatchDownEvent(float startX, float startY, ArrayList<PointF> pendingMovePoints, long downTime, long durationTime, int deviceId, int source) {
        float f;
        float f2;
        long j;
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_STRATEGY;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("compensateBatchDownEvent x=");
            f = startX;
            stringBuilder.append(f);
            stringBuilder.append(", y=");
            f2 = startY;
            stringBuilder.append(f2);
            stringBuilder.append(", durationTime=");
            j = durationTime;
            stringBuilder.append(j);
            Log.i(str, stringBuilder.toString());
        } else {
            f = startX;
            f2 = startY;
            j = durationTime;
        }
        GestureUtils.injectDownWithBatchMoveEvent(downTime, f, f2, pendingMovePoints, j, deviceId, source);
    }

    private void checkCompensateEvent(PointEvent p, ArrayList<PointF> pendingMovePoints, boolean hasMultiTouched) {
        int distance = distance(p.startX, p.startY, p.endX, p.endY);
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_STRATEGY;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkCompensateEvent distance=");
            stringBuilder.append(distance);
            Log.i(str, stringBuilder.toString());
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
        ArrayList arrayList;
        boolean canUsePendingPoints = true;
        if (pendingMovePoints == null) {
            canUsePendingPoints = false;
        } else if (pendingMovePoints.size() >= 30 || pendingMovePoints.size() < 4) {
            canUsePendingPoints = false;
            if (GestureNavConst.DEBUG) {
                String str = GestureNavConst.TAG_GESTURE_STRATEGY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pending points size:");
                stringBuilder.append(pendingMovePoints.size());
                Log.i(str, stringBuilder.toString());
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
        } else {
            arrayList = null;
        }
        GestureUtils.sendSwipe(f, f2, f3, f4, i, i2, i3, arrayList, hasMultiTouched);
    }

    private void recordPendingMoveDatas(MotionEvent event, float distance, float rawX, float rawY) {
        int dist;
        int h = 0;
        if (this.mPendingMoveDistance.size() < 30) {
            dist = (int) Math.abs(distance - this.mLastPendingDistance);
            int dt = 150;
            if (dist > 150) {
                int times = dist / 150;
                if (distance <= this.mLastPendingDistance) {
                    dt = -150;
                }
                for (int i = 0; i < times; i++) {
                    this.mPendingMoveDistance.add(Float.valueOf(distance - ((float) ((times - i) * dt))));
                }
            }
            this.mPendingMoveDistance.add(Float.valueOf(distance));
            this.mLastPendingDistance = distance;
        }
        if (this.mPendingMovePoints.size() < 30) {
            dist = event.getHistorySize();
            while (h < dist) {
                float rx = event.getHistoricalX(h) - this.mXOffset;
                float ry = event.getHistoricalY(h) - this.mYOffset;
                this.mPendingMovePoints.add(new PointF(rx, ry));
                if (GestureNavConst.DEBUG_ALL) {
                    String str = GestureNavConst.TAG_GESTURE_STRATEGY;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("record rx=");
                    stringBuilder.append(rx);
                    stringBuilder.append(", ry=");
                    stringBuilder.append(ry);
                    Log.d(str, stringBuilder.toString());
                }
                h++;
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
