package com.android.server.input;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Flog;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputEvent;
import android.view.MotionEvent;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.wavemapping.cons.WMStateCons;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.DecisionUtil;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.huawei.android.app.ActivityManagerEx;
import huawei.com.android.server.fingerprint.FingerViewController;

public class HwFingersSnapshooter {
    private static final String ACCESSIBILITY_SCREENREADER_ENABLED = "accessibility_screenreader_enabled";
    private static final String APS_RESOLUTION_CHANGE_ACTION = "huawei.intent.action.APS_RESOLUTION_CHANGE_ACTION";
    private static final String APS_RESOLUTION_CHANGE_PERSISSIONS = "huawei.intent.permissions.APS_RESOLUTION_CHANGE_ACTION";
    private static final String GAME_TRIPLE_FINGER = "game_triple_finger";
    private static final int GAME_TRIPLE_MODE_CLOSE = 2;
    private static final int GAME_TRIPLE_MODE_DEFAULT = 2;
    private static final int GAME_TRIPLE_MODE_OPEN = 1;
    private static final long INIT_DELAY_TIME = 10000;
    private static final float INIT_DENSITY = 2.0f;
    private static final String KEY_TRIPLE_FINGER_MOTION = "motion_triple_finger_shot";
    private static final float MAX_FINGER_DOWN_INTERVAL = 1000.0f;
    private static final float MAX_FINGER_DOWN_X_DISTANCE = 400.0f;
    private static final float MAX_FINGER_DOWN_Y_DISTANCE = 145.0f;
    private static final float MIN_Y_TRIGGER_LANDSCAPE_DISTANCE = 90.0f;
    private static final float MIN_Y_TRIGGER_PORTRAIT_DISTANCE = 120.0f;
    public static final int MSG_HANDLE_FINGER_SNAP_SHOOTER_INIT = 0;
    public static final int MSG_HANDLE_USER_SWITCH = 1;
    private static final int MSG_KEY_SCREEN_REMIND = 2;
    private static final int MSG_MENU_SCREEN_DELAY_TME = 15000;
    private static final String RIGHT_BRACKETS = "}";
    private static final String SCREEN_SHOT_EVENT_NAME = "com.huawei.screenshot.intent.action.TripleFingersScreenshot";
    private static final String TAG = "HwFingersSnapshooter";
    private static final String TALKBACK_CONFIG = "ro.config.hw_talkback_btn";
    private static final int TRIGGER_MIN_FINGERS = 3;
    private static final int TRIPLE_FINGER_MOTION_OFF = 0;
    private static final int TRIPLE_FINGER_MOTION_ON = 1;
    private static final float UPWARD_DIFF = 0.5f;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public float mDensity = 2.0f;
    /* access modifiers changed from: private */
    public int mEnabled;
    private BroadcastReceiver mFingerSnapReceiver = new BroadcastReceiver() {
        /* class com.android.server.input.HwFingersSnapshooter.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                Log.i(HwFingersSnapshooter.TAG, "onReceive intent action = " + intent.getAction());
                if (intent.getAction() != null) {
                    if (HwFingersSnapshooter.APS_RESOLUTION_CHANGE_ACTION.equals(intent.getAction())) {
                        HwFingersSnapshooter hwFingersSnapshooter = HwFingersSnapshooter.this;
                        float unused = hwFingersSnapshooter.mDensity = hwFingersSnapshooter.mContext.getResources().getDisplayMetrics().density;
                        HwFingersSnapshooter hwFingersSnapshooter2 = HwFingersSnapshooter.this;
                        float unused2 = hwFingersSnapshooter2.mMaxFingerDownyDistance = hwFingersSnapshooter2.dipsToPixels(HwFingersSnapshooter.MAX_FINGER_DOWN_Y_DISTANCE);
                        HwFingersSnapshooter hwFingersSnapshooter3 = HwFingersSnapshooter.this;
                        float unused3 = hwFingersSnapshooter3.mMaxFingerDownxDistance = hwFingersSnapshooter3.dipsToPixels(HwFingersSnapshooter.MAX_FINGER_DOWN_X_DISTANCE);
                        HwFingersSnapshooter hwFingersSnapshooter4 = HwFingersSnapshooter.this;
                        float unused4 = hwFingersSnapshooter4.mMinyTriggerLandscapeDistance = hwFingersSnapshooter4.dipsToPixels(HwFingersSnapshooter.MIN_Y_TRIGGER_LANDSCAPE_DISTANCE);
                        HwFingersSnapshooter hwFingersSnapshooter5 = HwFingersSnapshooter.this;
                        float unused5 = hwFingersSnapshooter5.mMinyTriggerPortraitDistance = hwFingersSnapshooter5.dipsToPixels(HwFingersSnapshooter.MIN_Y_TRIGGER_PORTRAIT_DISTANCE);
                        Log.i(HwFingersSnapshooter.TAG, "mDisplayResolutionModeObserver mMaxFingerDownyDistance = " + HwFingersSnapshooter.this.mMaxFingerDownyDistance + ",mMinyTriggerLandscapeDistance:" + HwFingersSnapshooter.this.mMinyTriggerLandscapeDistance + ",mMinyTriggerPortraitDistance:" + HwFingersSnapshooter.this.mMinyTriggerPortraitDistance);
                    } else if (SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED.equals(intent.getAction())) {
                        Message message = Message.obtain();
                        message.what = 1;
                        message.arg1 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                        HwFingersSnapshooter.this.mHandler.sendMessage(message);
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public Handler mHandler;
    private float mHeight = 0.0f;
    /* access modifiers changed from: private */
    public HwInputManagerService mIm = null;
    private boolean mIsCanFilter = false;
    private boolean mIsFilterCurrentTouch = false;
    /* access modifiers changed from: private */
    public float mMaxFingerDownxDistance = 0.0f;
    /* access modifiers changed from: private */
    public float mMaxFingerDownyDistance = 0.0f;
    private float mMaxY = 0.0f;
    private float mMinyTriggerDistance = 0.0f;
    /* access modifiers changed from: private */
    public float mMinyTriggerLandscapeDistance = 0.0f;
    /* access modifiers changed from: private */
    public float mMinyTriggerPortraitDistance = 0.0f;
    private HwPhoneWindowManager mPolicy;
    ServiceConnection mScreenshotConnection = null;
    /* access modifiers changed from: private */
    public final Object mScreenshotLock = new Object();
    private final Runnable mScreenshotRunnable = new Runnable() {
        /* class com.android.server.input.HwFingersSnapshooter.AnonymousClass2 */

        public void run() {
            HwFingersSnapshooter.this.takeScreenshot();
            HwFingersSnapshooter.this.mHandler.sendEmptyMessageDelayed(2, HwArbitrationDEFS.WIFI_RX_BYTES_THRESHOLD);
        }
    };
    final Runnable mScreenshotTimeout = new Runnable() {
        /* class com.android.server.input.HwFingersSnapshooter.AnonymousClass1 */

        public void run() {
            synchronized (HwFingersSnapshooter.this.mScreenshotLock) {
                if (HwFingersSnapshooter.this.mScreenshotConnection != null) {
                    HwFingersSnapshooter.this.mContext.unbindService(HwFingersSnapshooter.this.mScreenshotConnection);
                    HwFingersSnapshooter.this.mScreenshotConnection = null;
                }
            }
        }
    };
    private SparseArray<Point> mTouchingFingers = new SparseArray<>();
    private float mTrigerStartThreshold = 0.0f;
    private ContentObserver mTripleFingerMotionModeObserver = new ContentObserver(new Handler()) {
        /* class com.android.server.input.HwFingersSnapshooter.AnonymousClass4 */

        public void onChange(boolean selfChange) {
            HwFingersSnapshooter hwFingersSnapshooter = HwFingersSnapshooter.this;
            int unused = hwFingersSnapshooter.mEnabled = Settings.System.getIntForUser(hwFingersSnapshooter.mContext.getContentResolver(), HwFingersSnapshooter.KEY_TRIPLE_FINGER_MOTION, 0, -2);
            Log.i(HwFingersSnapshooter.TAG, "mTripleFingerMotionModeObserver mEnabled = " + HwFingersSnapshooter.this.mEnabled);
        }
    };

    private static class Point {
        /* access modifiers changed from: private */
        public float mMoveY = 0.0f;
        /* access modifiers changed from: private */
        public long mTime;
        /* access modifiers changed from: private */
        public float mX;
        /* access modifiers changed from: private */
        public float mY;

        Point(float x, float y, long time) {
            this.mX = x;
            this.mY = y;
            this.mTime = time;
        }

        public void updateMoveDistance(float x, float y) {
            this.mMoveY = y - this.mY;
        }

        public String toString() {
            return "(" + this.mX + "," + this.mY + ")";
        }
    }

    public HwFingersSnapshooter(Context context, HwInputManagerService inputManager) {
        Log.i(TAG, "HwFingersSnapshooter constructor");
        this.mContext = context;
        this.mIm = inputManager;
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new FingerSnapHandler(handlerThread.getLooper());
        this.mHandler.sendEmptyMessage(0);
        this.mHeight = (float) this.mContext.getResources().getDisplayMetrics().heightPixels;
    }

    public boolean handleMotionEvent(InputEvent event) {
        if (this.mEnabled == 0) {
            return true;
        }
        if (event instanceof MotionEvent) {
            MotionEvent motionEvent = (MotionEvent) event;
            int action = motionEvent.getActionMasked();
            int id = motionEvent.getPointerId(motionEvent.getActionIndex());
            long time = motionEvent.getEventTime();
            if (action == 0) {
                resetState();
                Point down = new Point(motionEvent.getRawX(), motionEvent.getRawY(), time);
                this.mTouchingFingers.put(id, down);
                Log.i(TAG, "handleMotionEvent first finger(" + id + ") touch down at " + down);
            } else if (action == 1) {
                Log.i(TAG, "handleMotionEvent last finger(" + id + ") up.");
                resetState();
            } else if (action != 2) {
                if (action == 5) {
                    this.mIsCanFilter = handleFingerDown(motionEvent);
                } else if (action != 6) {
                    Log.e(TAG, "Invalid motionevent");
                } else {
                    handleFingerUp(motionEvent);
                    this.mTouchingFingers.delete(id);
                    this.mIsFilterCurrentTouch = false;
                }
            } else if (this.mIsCanFilter) {
                handleFingerMove(motionEvent);
            }
            return true ^ this.mIsFilterCurrentTouch;
        }
        Log.i(TAG, "handleMotionEvent not a motionEvent");
        return true;
    }

    private void resetState() {
        this.mIsFilterCurrentTouch = false;
        this.mIsCanFilter = false;
        this.mMaxY = 0.0f;
        this.mTouchingFingers.clear();
    }

    private boolean handleFingerDown(MotionEvent motionEvent) {
        float offsetX = motionEvent.getRawX() - motionEvent.getX();
        float offsetY = motionEvent.getRawY() - motionEvent.getY();
        int actionIndex = motionEvent.getActionIndex();
        int id = motionEvent.getPointerId(actionIndex);
        Point pointerDown = new Point(motionEvent.getX(actionIndex) + offsetX, motionEvent.getY(actionIndex) + offsetY, motionEvent.getEventTime());
        this.mTouchingFingers.put(id, pointerDown);
        int fingerSize = this.mTouchingFingers.size();
        Log.i(TAG, "handleFingerDown new finger(" + id + ") touch down at " + pointerDown + ",size:" + fingerSize);
        if (fingerSize == 3) {
            return handleTriggerMinFingers(motionEvent);
        }
        Log.i(TAG, "handleFingerDown " + fingerSize + " fingers touching down");
        return false;
    }

    private boolean handleTriggerMinFingers(MotionEvent motionEvent) {
        float distanceY = getDistanceY();
        if (this.mMaxFingerDownyDistance < distanceY) {
            Context context = this.mContext;
            boolean isRet = Flog.bdReport(context, (int) CPUFeature.MSG_SET_VIP_THREAD, "{distance:" + pixelsToDips(distanceY) + RIGHT_BRACKETS);
            StringBuilder sb = new StringBuilder();
            sb.append("errorState:the fingers' position faraway on Y dimension! EventId:143 ret :");
            sb.append(isRet);
            Log.i(TAG, sb.toString());
            return false;
        }
        if (this.mMaxFingerDownxDistance < getDistanceX()) {
            Log.i(TAG, "errorState:the fingers' position faraway on X dimension!");
            return false;
        }
        long interval = getInterval();
        if (((float) interval) > MAX_FINGER_DOWN_INTERVAL) {
            Context context2 = this.mContext;
            boolean isRet2 = Flog.bdReport(context2, (int) CPUFeature.MSG_RESET_VIP_THREAD, "{interval:" + interval + RIGHT_BRACKETS);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("errorState:fingers'interval longer than except time! EventId:144 ret :");
            sb2.append(isRet2);
            Log.i(TAG, sb2.toString());
            return false;
        } else if (canDisableGesture()) {
            return false;
        } else {
            if (SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false)) {
                Log.i(TAG, "can not take screen shot in super power mode!");
                return false;
            } else if (!isUserUnlocked()) {
                Log.i(TAG, "now isRestrictAsEncrypt, do not allow to take screenshot");
                return false;
            } else {
                startHandleTripleFingerSnap(motionEvent);
                if (this.mMaxY + this.mMinyTriggerDistance > this.mHeight) {
                    Log.i(TAG, "errorState:the remaining distance is not enough on Y dimension!");
                    this.mMaxY = 0.0f;
                    return false;
                }
                boolean isRet3 = Flog.bdReport(this.mContext, (int) WMStateCons.MSG_CHECK_4G_COVERAGE);
                Log.i(TAG, "handleFingerDown, this finger may trigger the 3-finger snapshot. EventId:140 isRet:" + isRet3);
                return true;
            }
        }
    }

    private boolean isUserUnlocked() {
        return ((UserManager) this.mContext.getSystemService("user")).isUserUnlocked(ActivityManager.getCurrentUser());
    }

    private float getDistanceY() {
        float maxY = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            Point point = this.mTouchingFingers.valueAt(i);
            if (point.mY < minY) {
                minY = point.mY;
            }
            if (point.mY > maxY) {
                maxY = point.mY;
            }
        }
        this.mMaxY = maxY;
        Log.i(TAG, "getDistance maxY = " + maxY + ", minY = " + minY);
        return maxY - minY;
    }

    private float getDistanceX() {
        float maxX = Float.MIN_VALUE;
        float minX = Float.MAX_VALUE;
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            Point point = this.mTouchingFingers.valueAt(i);
            if (point.mX < minX) {
                minX = point.mX;
            }
            if (point.mX > maxX) {
                maxX = point.mX;
            }
        }
        Log.i(TAG, "getDistance maxX = " + maxX + ", minX = " + minX);
        return maxX - minX;
    }

    private long getInterval() {
        long startTime = Long.MAX_VALUE;
        long latestTime = Long.MIN_VALUE;
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            Point point = this.mTouchingFingers.valueAt(i);
            if (point.mTime < startTime) {
                startTime = point.mTime;
            }
            if (point.mTime > latestTime) {
                latestTime = point.mTime;
            }
        }
        Log.i(TAG, "getInterval interval = " + (latestTime - startTime));
        return latestTime - startTime;
    }

    private void startHandleTripleFingerSnap(MotionEvent motionEvent) {
        if (this.mPolicy == null) {
            this.mPolicy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        }
        if (this.mPolicy.isLandscape()) {
            this.mMinyTriggerDistance = this.mMinyTriggerLandscapeDistance;
        } else {
            this.mMinyTriggerDistance = this.mMinyTriggerPortraitDistance;
        }
        this.mHeight = (float) this.mContext.getResources().getDisplayMetrics().heightPixels;
        Log.i(TAG, "handleFingerDown mMinyTriggerDistance:" + this.mMinyTriggerDistance + ", mHeight:" + this.mHeight);
    }

    private void handleFingerMove(final MotionEvent motionEvent) {
        float offsetY = motionEvent.getY() - motionEvent.getRawY();
        float offsetX = motionEvent.getX() - motionEvent.getRawX();
        int pointerCount = motionEvent.getPointerCount();
        int moveCount = 0;
        for (int i = 0; i < pointerCount; i++) {
            float curY = motionEvent.getY(i) + offsetY;
            float curX = motionEvent.getX(i) + offsetX;
            int id = motionEvent.getPointerId(i);
            Point point = this.mTouchingFingers.get(id);
            if (point != null) {
                point.updateMoveDistance(curX, curY);
                if (!this.mIsFilterCurrentTouch && point.mY < curY) {
                    moveCount++;
                    Log.i(TAG, "handleFingerMove finger(" + id + ") moveCount:" + moveCount);
                    if (moveCount == 3) {
                        Log.i(TAG, "handleFingerMove cancel");
                        this.mHandler.post(new Runnable() {
                            /* class com.android.server.input.HwFingersSnapshooter.AnonymousClass5 */

                            public void run() {
                                MotionEvent cancelEvent = motionEvent.copy();
                                cancelEvent.setAction(3);
                                HwFingersSnapshooter.this.mIm.injectInputEvent(cancelEvent, 2);
                            }
                        });
                        this.mIsFilterCurrentTouch = true;
                    }
                }
            } else {
                Log.i(TAG, "handleFingerMove point(" + id + ") not tracked!");
            }
        }
    }

    private void handleFingerUp(MotionEvent motionEvent) {
        int id = motionEvent.getPointerId(motionEvent.getActionIndex());
        Log.i(TAG, "handleFingerUp finger(" + id + ") up");
        if (!this.mIsFilterCurrentTouch) {
            Log.i(TAG, "handleFingerUp, current touch not marked as filter");
            return;
        }
        int fingerSize = this.mTouchingFingers.size();
        if (fingerSize != 3) {
            Log.i(TAG, "handleFingerUp, current touch has " + fingerSize + " fingers");
            return;
        }
        boolean isTriggerSnapshot = true;
        int i = 0;
        while (true) {
            if (i >= fingerSize) {
                break;
            }
            float moveDistance = this.mTouchingFingers.get(i).mMoveY;
            if (moveDistance < this.mMinyTriggerDistance) {
                Context context = this.mContext;
                boolean isRet = Flog.bdReport(context, (int) CPUFeature.MSG_SET_BG_UIDS, "{moveDistance:" + pixelsToDips(moveDistance) + RIGHT_BRACKETS);
                Log.i(TAG, "errorState:finger(" + i + ") move " + pixelsToDips(moveDistance) + "dp, less than except distance! EventId:" + CPUFeature.MSG_SET_BG_UIDS + " ret:" + isRet);
                isTriggerSnapshot = false;
                break;
            }
            i++;
        }
        if (isTriggerSnapshot) {
            resetState();
            boolean isRet2 = Flog.bdReport(this.mContext, 141);
            Log.i(TAG, "trigger the snapshot! EventId:141 ret:" + isRet2);
            this.mHandler.post(this.mScreenshotRunnable);
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerSnapShooterInit() {
        this.mDensity = this.mContext.getResources().getDisplayMetrics().density;
        this.mTrigerStartThreshold = (float) ((Context) checkNull("context", this.mContext)).getResources().getDimensionPixelSize(17105443);
        this.mEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_TRIPLE_FINGER_MOTION, 0, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_TRIPLE_FINGER_MOTION), true, this.mTripleFingerMotionModeObserver, -1);
        this.mContext.registerReceiverAsUser(this.mFingerSnapReceiver, UserHandle.ALL, new IntentFilter(APS_RESOLUTION_CHANGE_ACTION), APS_RESOLUTION_CHANGE_PERSISSIONS, null);
        this.mContext.registerReceiverAsUser(this.mFingerSnapReceiver, UserHandle.ALL, new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED), null, null);
        this.mPolicy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        this.mMaxFingerDownyDistance = dipsToPixels(MAX_FINGER_DOWN_Y_DISTANCE);
        this.mMaxFingerDownxDistance = dipsToPixels(MAX_FINGER_DOWN_X_DISTANCE);
        this.mMinyTriggerLandscapeDistance = dipsToPixels(MIN_Y_TRIGGER_LANDSCAPE_DISTANCE);
        this.mMinyTriggerPortraitDistance = dipsToPixels(MIN_Y_TRIGGER_PORTRAIT_DISTANCE);
        Log.i(TAG, "mTrigerStartThreshold:" + this.mTrigerStartThreshold + ", mEnabled:" + this.mEnabled);
    }

    /* access modifiers changed from: private */
    public void handleUserSwitch(int userId) {
        this.mEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_TRIPLE_FINGER_MOTION, 0, -2);
        Log.i(TAG, "onReceive ACTION_USER_SWITCHED currentUserId= " + userId + ", mEnabled:" + this.mEnabled);
    }

    private final class FingerSnapHandler extends Handler {
        FingerSnapHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                HwFingersSnapshooter.this.handleFingerSnapShooterInit();
            } else if (i == 1) {
                HwFingersSnapshooter.this.handleUserSwitch(msg.arg1);
            } else if (i != 2) {
                Log.e(HwFingersSnapshooter.TAG, "Invalid message");
            } else {
                removeMessages(2);
                if (!DecisionUtil.bindServiceToAidsEngine(HwFingersSnapshooter.this.mContext, HwFingersSnapshooter.SCREEN_SHOT_EVENT_NAME)) {
                    Log.i(HwFingersSnapshooter.TAG, "bindServiceToAidsEngine error");
                }
            }
        }
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg != null) {
            return arg;
        }
        throw new IllegalArgumentException(name + " must not be null");
    }

    /* access modifiers changed from: package-private */
    public final float dipsToPixels(float dips) {
        return (this.mDensity * dips) + 0.5f;
    }

    /* access modifiers changed from: package-private */
    public final float pixelsToDips(float pixels) {
        return (pixels / this.mDensity) + 0.5f;
    }

    private boolean canDisableGesture() {
        if (inInValidArea()) {
            Log.i(TAG, "inInvalidarea");
            return true;
        } else if (SystemProperties.getBoolean(TALKBACK_CONFIG, true) && isTalkBackServicesOn()) {
            Log.i(TAG, "in talkback mode");
            return true;
        } else if (SystemProperties.getBoolean("runtime.mmitest.isrunning", false)) {
            Log.i(TAG, "in MMI test");
            return true;
        } else if (!isTripleGameDisabled()) {
            return false;
        } else {
            Log.i(TAG, "game space disable triple finger");
            return true;
        }
    }

    private boolean isTripleGameDisabled() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), GAME_TRIPLE_FINGER, 2, ActivityManagerEx.getCurrentUser()) == 1 && ActivityManagerEx.isGameDndOn();
    }

    private boolean isTalkBackServicesOn() {
        Context context = this.mContext;
        boolean isScreenReaderEnabled = false;
        if (context == null) {
            return false;
        }
        if (Settings.Secure.getIntForUser(context.getContentResolver(), "accessibility_screenreader_enabled", 0, -2) == 1) {
            isScreenReaderEnabled = true;
        }
        Log.i(TAG, "isScreenReaderEnabled : " + isScreenReaderEnabled);
        return isScreenReaderEnabled;
    }

    private boolean inInValidArea() {
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            if (this.mTrigerStartThreshold >= this.mTouchingFingers.valueAt(i).mY) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void takeScreenshot() {
        synchronized (this.mScreenshotLock) {
            if (this.mScreenshotConnection == null) {
                ComponentName cn = new ComponentName(FingerViewController.PKGNAME_OF_KEYGUARD, "com.android.systemui.screenshot.TakeScreenshotService");
                Intent intent = new Intent();
                intent.setComponent(cn);
                ServiceConnection conn = new ServiceConnection() {
                    /* class com.android.server.input.HwFingersSnapshooter.AnonymousClass6 */

                    public void onServiceConnected(ComponentName name, IBinder service) {
                        synchronized (HwFingersSnapshooter.this.mScreenshotLock) {
                            if (HwFingersSnapshooter.this.mScreenshotConnection == this) {
                                Messenger messenger = new Messenger(service);
                                Message msg = Message.obtain((Handler) null, 1);
                                msg.replyTo = new Messenger(new Handler(HwFingersSnapshooter.this.mHandler.getLooper()) {
                                    /* class com.android.server.input.HwFingersSnapshooter.AnonymousClass6.AnonymousClass1 */

                                    public void handleMessage(Message msg) {
                                        synchronized (HwFingersSnapshooter.this.mScreenshotLock) {
                                            if (HwFingersSnapshooter.this.mScreenshotConnection == this) {
                                                HwFingersSnapshooter.this.mContext.unbindService(HwFingersSnapshooter.this.mScreenshotConnection);
                                                HwFingersSnapshooter.this.mScreenshotConnection = null;
                                                HwFingersSnapshooter.this.mHandler.removeCallbacks(HwFingersSnapshooter.this.mScreenshotTimeout);
                                            }
                                        }
                                    }
                                });
                                msg.arg1 = 0;
                                msg.arg2 = 0;
                                try {
                                    messenger.send(msg);
                                } catch (RemoteException e) {
                                    Log.e(HwFingersSnapshooter.TAG, "takeScreenshot: sending msg occured an error");
                                }
                            }
                        }
                    }

                    public void onServiceDisconnected(ComponentName name) {
                    }
                };
                if (this.mContext.bindServiceAsUser(intent, conn, 1, UserHandle.CURRENT)) {
                    this.mScreenshotConnection = conn;
                    this.mHandler.postDelayed(this.mScreenshotTimeout, 10000);
                }
            }
        }
    }
}
