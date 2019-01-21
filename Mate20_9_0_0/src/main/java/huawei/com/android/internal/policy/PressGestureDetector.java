package huawei.com.android.internal.policy;

import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import com.android.internal.policy.IPressGestureDetector;

public class PressGestureDetector implements IPressGestureDetector {
    private static final int COUNTS_FINGER_ONE = 1;
    private static final int COUNTS_FINGER_TWO = 2;
    private static final int COUNTS_FINGER_ZERO = 0;
    private static final long DEFAULT_GESTURE_TIME_OUT_LIMIT = 450;
    private static final long GESTURE_CONFLICT_POINTERS_ANGLE_LIMIT = 60;
    private static final long GESTURE_CONFLICT_POINTERS_DISTANCE_LIMIT = 100;
    private static final int POINTERS_MIN_DISTANCE_DP = 16;
    private static final int SCREEN_POINTER_MARGIN_DP = 5;
    private static final int SETTINGS_SWITCH_ON = 1;
    private static final String TAG = "HiTouch_PressGestureDetector";
    private static final String TALK_BACK = "talkback";
    private static final float TOUCH_MOVE_BOUND_X = 30.0f;
    private static final float TOUCH_MOVE_BOUND_Y = 50.0f;
    private static final long TOUCH_TWO_FINGERS_TIME_OUT_LIMIT = 150;
    private final Context mContext;
    private final Context mContextActivity;
    private final FrameLayout mDecorView;
    private int mDisplayHeigh = 0;
    private float mDisplayScale = 0.0f;
    private int mDisplayWidth = 0;
    private boolean mDistanceALot = false;
    private int mFingerCount = 0;
    private boolean mGestureInterrupted;
    private boolean mHiTouchRestricted;
    private boolean mIsPhoneLongClickSwipe = false;
    private float mLongPressDownX = 0.0f;
    private float mLongPressDownY = 0.0f;
    private float mLongPressPointerDownX = 0.0f;
    private float mLongPressPointerDownY = 0.0f;
    private final HiTouchSensor mSensor;
    private boolean mSensorRegistered = false;
    private boolean mStatus;
    private boolean mStatusChecked;
    private boolean mTextBoomEntered;
    private long mTouchDownTime;
    private long mTouchPointerDownTime;
    private final int mTouchSlop;
    private long mTriggerTime = DEFAULT_GESTURE_TIME_OUT_LIMIT;

    public PressGestureDetector(Context context, FrameLayout docerView, Context contextActivity) {
        this.mContext = context;
        this.mContextActivity = contextActivity;
        this.mDecorView = docerView;
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mSensor = new HiTouchSensor(this.mContext, this.mContextActivity);
        updateDisplayParameters();
    }

    public boolean isLongPressSwipe() {
        return this.mIsPhoneLongClickSwipe;
    }

    public void onAttached(final int windowType) {
        new Thread() {
            public void run() {
                PressGestureDetector.this.mHiTouchRestricted = PressGestureDetector.this.mSensor.isUnsupportScence(windowType);
            }
        }.start();
    }

    public void onDetached() {
        if (!this.mHiTouchRestricted) {
            if (this.mSensorRegistered) {
                this.mSensor.unregisterObserver();
                this.mSensorRegistered = false;
            }
            resetSwipeFlag();
        }
    }

    public void handleBackKey() {
        if (!this.mHiTouchRestricted) {
            resetSwipeFlag();
        }
    }

    public void handleConfigurationChanged(Configuration newConfig) {
        updateDisplayParameters();
        this.mSensor.onConfigurationChanged(newConfig);
    }

    public boolean dispatchTouchEvent(MotionEvent ev, boolean isHandling) {
        MotionEvent motionEvent = ev;
        if (isHandling || this.mHiTouchRestricted) {
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        if (this.mDecorView.getParent() == this.mDecorView.getViewRootImpl()) {
            switch (ev.getAction() & 255) {
                case 0:
                    this.mFingerCount = 1;
                    this.mTextBoomEntered = false;
                    this.mDistanceALot = false;
                    this.mGestureInterrupted = false;
                    this.mStatusChecked = false;
                    this.mIsPhoneLongClickSwipe = false;
                    int actionIndexDown = ev.getActionIndex();
                    this.mLongPressDownX = motionEvent.getX(actionIndexDown);
                    this.mLongPressDownY = motionEvent.getY(actionIndexDown);
                    this.mTouchDownTime = SystemClock.uptimeMillis();
                    break;
                case 1:
                    this.mFingerCount = 0;
                    resetSwipeFlag();
                    break;
                case 2:
                    if (!this.mTextBoomEntered && this.mFingerCount >= 2 && ev.getPointerCount() >= 2 && !this.mGestureInterrupted && ((!this.mStatusChecked || this.mStatus) && this.mSensor.checkDeviceProvisioned())) {
                        float mainPointX = motionEvent.getX(0);
                        float mainPointY = motionEvent.getY(0);
                        float secondPointX = motionEvent.getX(1);
                        float secondPointY = motionEvent.getY(1);
                        if (this.mDistanceALot) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("HiTouch Miss: Moved a lot, X1: ");
                            stringBuilder.append(Math.abs(this.mLongPressDownX - mainPointX));
                            stringBuilder.append(" Y1: ");
                            stringBuilder.append(Math.abs(this.mLongPressDownY - mainPointY));
                            stringBuilder.append(" || X2: ");
                            stringBuilder.append(Math.abs(this.mLongPressPointerDownX - secondPointX));
                            stringBuilder.append(" Y2: ");
                            stringBuilder.append(Math.abs(this.mLongPressPointerDownY - secondPointY));
                            Log.v(str, stringBuilder.toString());
                            break;
                        }
                        boolean needRemove = false;
                        if (this.mDecorView.pointInView(mainPointX, mainPointY, (float) this.mTouchSlop)) {
                            if (Math.abs(this.mLongPressDownX - mainPointX) >= TOUCH_MOVE_BOUND_X || Math.abs(this.mLongPressDownY - mainPointY) >= TOUCH_MOVE_BOUND_Y) {
                                needRemove = true;
                                this.mDistanceALot = true;
                            }
                            if (Math.abs(this.mLongPressPointerDownX - secondPointX) >= TOUCH_MOVE_BOUND_X || Math.abs(this.mLongPressPointerDownY - secondPointY) >= TOUCH_MOVE_BOUND_Y) {
                                needRemove = true;
                                this.mDistanceALot = true;
                            }
                            if (!checkPointsLocation(mainPointX, mainPointY, secondPointX, secondPointY)) {
                                this.mGestureInterrupted = true;
                                needRemove = true;
                            }
                        } else {
                            needRemove = true;
                            Log.i(TAG, "HiTouch Miss: point OUT of DecorView");
                        }
                        if (needRemove) {
                            Log.v(TAG, "Gesture doesn't match, stop text boom.");
                            resetSwipeFlag();
                            break;
                        }
                        long intervalTwoFingers = Math.abs(this.mTouchPointerDownTime - this.mTouchDownTime);
                        if (intervalTwoFingers > TOUCH_TWO_FINGERS_TIME_OUT_LIMIT) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("HiTouch Miss: Too large time interval(TwoFingers), ");
                            stringBuilder.append(intervalTwoFingers);
                            Log.i(str, stringBuilder.toString());
                            break;
                        }
                        if (!this.mSensorRegistered) {
                            this.mSensor.registerObserver();
                            this.mSensorRegistered = true;
                        }
                        if (checkTouchForBoom(ev.getSize(), mainPointX, mainPointY, secondPointX, secondPointY)) {
                            return true;
                        }
                    }
                    break;
                case 3:
                    resetSwipeFlag();
                    break;
                case 5:
                    Log.i(TAG, "ACTION_POINTER_DOWN.");
                    this.mFingerCount++;
                    this.mTextBoomEntered = false;
                    this.mDistanceALot = false;
                    this.mStatusChecked = false;
                    this.mIsPhoneLongClickSwipe = false;
                    if (this.mFingerCount > 2) {
                        Log.i(TAG, "HiTouch Miss: more than two pointers.");
                        this.mGestureInterrupted = true;
                    }
                    int actionIndexPointerDown = ev.getActionIndex();
                    if (this.mFingerCount == 2) {
                        this.mLongPressPointerDownX = motionEvent.getX(actionIndexPointerDown);
                        this.mLongPressPointerDownY = motionEvent.getY(actionIndexPointerDown);
                    }
                    this.mTouchPointerDownTime = SystemClock.uptimeMillis();
                    break;
                case 6:
                    this.mFingerCount--;
                    resetSwipeFlag();
                    break;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDecorView.getParent(): ");
        stringBuilder.append(this.mDecorView.getParent());
        stringBuilder.append(" mDecorView.getViewRootImpl(): ");
        stringBuilder.append(this.mDecorView.getViewRootImpl());
        Log.i(str, stringBuilder.toString());
        resetSwipeFlag();
        return false;
    }

    private void resetSwipeFlag() {
        this.mIsPhoneLongClickSwipe = false;
    }

    private boolean checkTouchForBoom(float touchSize, float x, float y, float x1, float y1) {
        float f;
        float f2;
        float f3;
        float f4;
        long interval = SystemClock.uptimeMillis() - this.mTouchDownTime;
        if (this.mStatusChecked) {
            f = x;
            f2 = y;
            f3 = x1;
            f4 = y1;
        } else {
            this.mStatus = this.mSensor.getStatus();
            f = x;
            f2 = y;
            f3 = x1;
            f4 = y1;
            if (checkTwoFingersGestureConflictScene(f, f2, f3, f4)) {
                this.mTriggerTime = Global.getLong(this.mContextActivity.getContentResolver(), "hitouch_triggerTime", DEFAULT_GESTURE_TIME_OUT_LIMIT);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Conflict scene, adaptive trigger time: ");
                stringBuilder.append(this.mTriggerTime);
                Log.v(str, stringBuilder.toString());
            } else {
                this.mTriggerTime = DEFAULT_GESTURE_TIME_OUT_LIMIT;
            }
            this.mStatusChecked = true;
        }
        if (!this.mStatus) {
            return false;
        }
        this.mIsPhoneLongClickSwipe = true;
        if (interval < this.mTriggerTime) {
            return false;
        }
        this.mSensor.launchHiTouchService(f, f2, f3, f4, 0);
        this.mTextBoomEntered = true;
        return true;
    }

    public double getDistance(float x1, float y1, float x2, float y2) {
        float _x = Math.abs(x1 - x2);
        float _y = Math.abs(y1 - y2);
        return Math.sqrt((double) ((_x * _x) + (_y * _y)));
    }

    private int dp2px(float dpValue) {
        return (int) ((this.mDisplayScale * dpValue) + 0.5f);
    }

    private int px2dp(float pxValue) {
        if (this.mDisplayScale == 0.0f) {
            return 0;
        }
        return (int) ((pxValue / this.mDisplayScale) + 0.5f);
    }

    private boolean checkPointsLocation(float pointX1, float pointY1, float pointX2, float pointY2) {
        int margin = dp2px(1084227584);
        String str;
        StringBuilder stringBuilder;
        if (pointX1 < ((float) margin) || pointX1 > ((float) (this.mDisplayWidth - margin))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("HiTouch Miss: Right: ");
            stringBuilder.append(margin);
            stringBuilder.append(" X1: ");
            stringBuilder.append(pointX1);
            stringBuilder.append(" Left: ");
            stringBuilder.append(this.mDisplayWidth - margin);
            Log.d(str, stringBuilder.toString());
            return false;
        } else if (pointX2 < ((float) margin) || pointX2 > ((float) (this.mDisplayWidth - margin))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("HiTouch Miss: Right: ");
            stringBuilder.append(margin);
            stringBuilder.append(" X2: ");
            stringBuilder.append(pointX2);
            stringBuilder.append(" Left: ");
            stringBuilder.append(this.mDisplayWidth - margin);
            Log.d(str, stringBuilder.toString());
            return false;
        } else if (pointY1 < ((float) margin) || pointY1 > ((float) (this.mDisplayHeigh - margin))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("HiTouch Miss: Top: ");
            stringBuilder.append(margin);
            stringBuilder.append(" Y1: ");
            stringBuilder.append(pointY1);
            stringBuilder.append(" Bottom: ");
            stringBuilder.append(this.mDisplayHeigh - margin);
            Log.d(str, stringBuilder.toString());
            return false;
        } else if (pointY2 < ((float) margin) || pointY2 > ((float) (this.mDisplayHeigh - margin))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("HiTouch Miss: Top: ");
            stringBuilder.append(margin);
            stringBuilder.append(" Y2: ");
            stringBuilder.append(pointY2);
            stringBuilder.append(" Bottom: ");
            stringBuilder.append(this.mDisplayHeigh - margin);
            Log.d(str, stringBuilder.toString());
            return false;
        } else {
            if (getDistance(pointX1, pointY1, pointX2, pointY2) >= ((double) dp2px(1098907648))) {
                return true;
            }
            Log.d(TAG, "HiTouch Miss: pointers are too close.");
            return false;
        }
    }

    private void updateDisplayParameters() {
        DisplayMetrics dm = this.mContextActivity.getResources().getDisplayMetrics();
        this.mDisplayScale = dm.density;
        this.mDisplayWidth = dm.widthPixels;
        this.mDisplayHeigh = dm.heightPixels;
        if (!SystemProperties.get("ro.config.hw_notch_size", "").trim().isEmpty()) {
            int notchHeight = this.mContextActivity.getResources().getDimensionPixelSize(17105318);
            String str;
            StringBuilder stringBuilder;
            if (this.mContextActivity.getResources().getConfiguration().orientation == 1) {
                this.mDisplayHeigh += notchHeight;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HiTouch on notch display, height corret:");
                stringBuilder.append(notchHeight);
                Log.d(str, stringBuilder.toString());
                return;
            }
            this.mDisplayWidth += notchHeight;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("HiTouch on notch display, width corret:");
            stringBuilder.append(notchHeight);
            Log.d(str, stringBuilder.toString());
        }
    }

    private int getAngleByTwoPointers(float firstPntX, float firstPntY, float SecondPntX, float SecondPntY) {
        double param1 = (double) Math.abs(firstPntY - SecondPntY);
        double param2 = (double) Math.abs(firstPntX - SecondPntX);
        if (param2 == 0.0d) {
            return 0;
        }
        return 90 - ((int) ((Math.atan(param1 / param2) / 3.141592653589793d) * 180.0d));
    }

    private boolean checkTwoFingersGestureConflictScene(float firstPntX, float firstPntY, float SecondPntX, float SecondPntY) {
        if (((long) getAngleByTwoPointers(firstPntX, firstPntY, SecondPntX, SecondPntY)) >= GESTURE_CONFLICT_POINTERS_ANGLE_LIMIT || ((long) px2dp((float) getDistance(firstPntX, firstPntY, SecondPntX, SecondPntY))) <= GESTURE_CONFLICT_POINTERS_DISTANCE_LIMIT) {
            return false;
        }
        return true;
    }
}
