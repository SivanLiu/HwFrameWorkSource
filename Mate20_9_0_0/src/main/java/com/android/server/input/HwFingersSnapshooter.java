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
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Flog;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputEvent;
import android.view.MotionEvent;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavConst;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.com.android.server.fingerprint.FingerViewController;

public class HwFingersSnapshooter {
    private static final String APS_RESOLUTION_CHANGE_ACTION = "huawei.intent.action.APS_RESOLUTION_CHANGE_ACTION";
    private static final String APS_RESOLUTION_CHANGE_PERSISSIONS = "huawei.intent.permissions.APS_RESOLUTION_CHANGE_ACTION";
    private static final String KEY_TRIPLE_FINGER_MOTION = "motion_triple_finger_shot";
    private static final float MAX_FINGER_DOWN_INTERVAL = 1000.0f;
    private static final float MAX_FINGER_DOWN_X_DISTANCE = 400.0f;
    private static final float MAX_FINGER_DOWN_Y_DISTANCE = 145.0f;
    private static final float MIN_Y_TRIGGER_LANDSCAPE_DISTANCE = 90.0f;
    private static final float MIN_Y_TRIGGER_PORTRAIT_DISTANCE = 120.0f;
    public static final int MSG_HANDLE_FINGER_SNAP_SHOOTER_INIT = 0;
    public static final int MSG_HANDLE_USER_SWITCH = 1;
    private static final String TAG = "HwFingersSnapshooter";
    private static final String TALKBACK_COMPONENT_NAME = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private static final String TALKBACK_CONFIG = "ro.config.hw_talkback_btn";
    private static final int TRIGGER_MIN_FINGERS = 3;
    private static final int TRIPLE_FINGER_MOTION_OFF = 0;
    private static final int TRIPLE_FINGER_MOTION_ON = 1;
    private boolean mCanFilter = false;
    private Context mContext;
    private float mDensity = 2.0f;
    private int mEnabled;
    private boolean mFilterCurrentTouch = false;
    private BroadcastReceiver mFingerSnapReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String str = HwFingersSnapshooter.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive intent action = ");
                stringBuilder.append(intent.getAction());
                Log.i(str, stringBuilder.toString());
                if (intent.getAction() != null) {
                    if (HwFingersSnapshooter.APS_RESOLUTION_CHANGE_ACTION.equals(intent.getAction())) {
                        HwFingersSnapshooter.this.mDensity = HwFingersSnapshooter.this.mContext.getResources().getDisplayMetrics().density;
                        HwFingersSnapshooter.this.mMaxFingerDownYDistance = HwFingersSnapshooter.this.dipsToPixels(HwFingersSnapshooter.MAX_FINGER_DOWN_Y_DISTANCE);
                        HwFingersSnapshooter.this.mMaxFingerDownXDistance = HwFingersSnapshooter.this.dipsToPixels(HwFingersSnapshooter.MAX_FINGER_DOWN_X_DISTANCE);
                        HwFingersSnapshooter.this.mMinYTriggerLandscapeDistance = HwFingersSnapshooter.this.dipsToPixels(HwFingersSnapshooter.MIN_Y_TRIGGER_LANDSCAPE_DISTANCE);
                        HwFingersSnapshooter.this.mMinYTriggerPortraitDistance = HwFingersSnapshooter.this.dipsToPixels(HwFingersSnapshooter.MIN_Y_TRIGGER_PORTRAIT_DISTANCE);
                        str = HwFingersSnapshooter.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("mDisplayResolutionModeObserver mMaxFingerDownYDistance = ");
                        stringBuilder.append(HwFingersSnapshooter.this.mMaxFingerDownYDistance);
                        stringBuilder.append(",mMinYTriggerLandscapeDistance:");
                        stringBuilder.append(HwFingersSnapshooter.this.mMinYTriggerLandscapeDistance);
                        stringBuilder.append(",mMinYTriggerPortraitDistance:");
                        stringBuilder.append(HwFingersSnapshooter.this.mMinYTriggerPortraitDistance);
                        Log.i(str, stringBuilder.toString());
                    } else if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                        Message message = new Message();
                        message.what = 1;
                        message.arg1 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                        HwFingersSnapshooter.this.mHandler.sendMessage(message);
                    }
                }
            }
        }
    };
    private Handler mHandler;
    private float mHeight = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private HwInputManagerService mIm = null;
    private float mMaxFingerDownXDistance = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mMaxFingerDownYDistance = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mMaxY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mMinYTriggerDistance = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mMinYTriggerLandscapeDistance = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mMinYTriggerPortraitDistance = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private HwPhoneWindowManager mPolicy;
    ServiceConnection mScreenshotConnection = null;
    final Object mScreenshotLock = new Object();
    private final Runnable mScreenshotRunnable = new Runnable() {
        public void run() {
            HwFingersSnapshooter.this.takeScreenshot();
        }
    };
    final Runnable mScreenshotTimeout = new Runnable() {
        public void run() {
            synchronized (HwFingersSnapshooter.this.mScreenshotLock) {
                if (HwFingersSnapshooter.this.mScreenshotConnection != null) {
                    HwFingersSnapshooter.this.mContext.unbindService(HwFingersSnapshooter.this.mScreenshotConnection);
                    HwFingersSnapshooter.this.mScreenshotConnection = null;
                }
            }
        }
    };
    private SparseArray<Point> mTouchingFingers = new SparseArray();
    private float mTrigerStartThreshold = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private ContentObserver mTripleFingerMotionModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            HwFingersSnapshooter.this.mEnabled = System.getIntForUser(HwFingersSnapshooter.this.mContext.getContentResolver(), HwFingersSnapshooter.KEY_TRIPLE_FINGER_MOTION, 0, -2);
            String str = HwFingersSnapshooter.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mTripleFingerMotionModeObserver mEnabled = ");
            stringBuilder.append(HwFingersSnapshooter.this.mEnabled);
            Log.i(str, stringBuilder.toString());
        }
    };

    private final class FingerSnapHandler extends Handler {
        public FingerSnapHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    HwFingersSnapshooter.this.handleFingerSnapShooterInit();
                    return;
                case 1:
                    HwFingersSnapshooter.this.handleUserSwitch(msg.arg1);
                    return;
                default:
                    Log.e(HwFingersSnapshooter.TAG, "Invalid message");
                    return;
            }
        }
    }

    private static class Point {
        public float moveY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public long time;
        public float x;
        public float y;

        public Point(float x_, float y_, long time_) {
            this.x = x_;
            this.y = y_;
            this.time = time_;
        }

        public void updateMoveDistance(float x_, float y_) {
            this.moveY = y_ - this.y;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("(");
            stringBuilder.append(this.x);
            stringBuilder.append(",");
            stringBuilder.append(this.y);
            stringBuilder.append(")");
            return stringBuilder.toString();
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
            switch (action) {
                case 0:
                    resetState();
                    Point down = new Point(motionEvent.getRawX(), motionEvent.getRawY(), time);
                    this.mTouchingFingers.put(id, down);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleMotionEvent first finger(");
                    stringBuilder.append(id);
                    stringBuilder.append(") touch down at ");
                    stringBuilder.append(down);
                    Log.i(str, stringBuilder.toString());
                    break;
                case 1:
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("handleMotionEvent last finger(");
                    stringBuilder2.append(id);
                    stringBuilder2.append(") up ");
                    Log.i(str2, stringBuilder2.toString());
                    resetState();
                    break;
                case 2:
                    if (this.mCanFilter) {
                        handleFingerMove(motionEvent);
                        break;
                    }
                    break;
                case 5:
                    this.mCanFilter = handleFingerDown(motionEvent);
                    break;
                case 6:
                    handleFingerUp(motionEvent);
                    this.mTouchingFingers.delete(id);
                    this.mFilterCurrentTouch = false;
                    break;
                default:
                    Log.e(TAG, "Invalid motionevent");
                    break;
            }
            return 1 ^ this.mFilterCurrentTouch;
        }
        Log.i(TAG, "handleMotionEvent not a motionEvent");
        return true;
    }

    private void resetState() {
        this.mFilterCurrentTouch = false;
        this.mCanFilter = false;
        this.mMaxY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        this.mTouchingFingers.clear();
    }

    private boolean handleFingerDown(MotionEvent motionEvent) {
        MotionEvent motionEvent2 = motionEvent;
        float offsetX = motionEvent.getRawX() - motionEvent.getX();
        float offsetY = motionEvent.getRawY() - motionEvent.getY();
        int actionIndex = motionEvent.getActionIndex();
        int id = motionEvent2.getPointerId(actionIndex);
        Point pointerDown = new Point(motionEvent2.getX(actionIndex) + offsetX, motionEvent2.getY(actionIndex) + offsetY, motionEvent.getEventTime());
        this.mTouchingFingers.put(id, pointerDown);
        int fingerSize = this.mTouchingFingers.size();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleFingerDown new finger(");
        stringBuilder.append(id);
        stringBuilder.append(") touch down at ");
        stringBuilder.append(pointerDown);
        stringBuilder.append(",size:");
        stringBuilder.append(fingerSize);
        Log.i(str, stringBuilder.toString());
        if (fingerSize != 3) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleFingerDown ");
            stringBuilder2.append(fingerSize);
            stringBuilder2.append(" fingers touching down");
            Log.i(str2, stringBuilder2.toString());
            return false;
        }
        float distanceY = getDistanceY();
        if (distanceY > this.mMaxFingerDownYDistance) {
            boolean ret = this.mContext;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("{distance:");
            stringBuilder3.append(pixelsToDips(distanceY));
            stringBuilder3.append("}");
            ret = Flog.bdReport(ret, CPUFeature.MSG_SET_VIP_THREAD, stringBuilder3.toString());
            String str3 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("errorState:the fingers' position faraway on Y dimension! EventId:143 ret:");
            stringBuilder3.append(ret);
            Log.i(str3, stringBuilder3.toString());
            return false;
        } else if (getDistanceX() > this.mMaxFingerDownXDistance) {
            Log.i(TAG, "errorState:the fingers' position faraway on X dimension!");
            return false;
        } else {
            long interval = getInterval();
            StringBuilder stringBuilder4;
            if (((float) interval) > MAX_FINGER_DOWN_INTERVAL) {
                Context context = this.mContext;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("{interval:");
                stringBuilder4.append(interval);
                stringBuilder4.append("}");
                offsetX = Flog.bdReport(context, CPUFeature.MSG_RESET_VIP_THREAD, stringBuilder4.toString());
                str = TAG;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("errorState:fingers'interval longer than except time! EventId:144 ret:");
                stringBuilder5.append(offsetX);
                Log.i(str, stringBuilder5.toString());
                return false;
            }
            float f = offsetY;
            boolean z = false;
            if (canDisableGesture()) {
                return z;
            }
            if (SystemProperties.getBoolean("sys.super_power_save", z)) {
                Log.i(TAG, "can not take screen shot in super power mode!");
                return z;
            } else if (isUserUnlocked()) {
                startHandleTripleFingerSnap(motionEvent);
                if (this.mMaxY + this.mMinYTriggerDistance > this.mHeight) {
                    Log.i(TAG, "errorState:the remaining distance is not enough on Y dimension!");
                    this.mMaxY = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                    return false;
                }
                boolean ret2 = Flog.bdReport(this.mContext, 140);
                String str4 = TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("handleFingerDown, this finger may trigger the 3-finger snapshot. EventId:140 ret:");
                stringBuilder4.append(ret2);
                Log.i(str4, stringBuilder4.toString());
                return true;
            } else {
                Log.i(TAG, "now isRestrictAsEncrypt, do not allow to take screenshot");
                return z;
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
            Point p = (Point) this.mTouchingFingers.valueAt(i);
            if (p.y < minY) {
                minY = p.y;
            }
            if (p.y > maxY) {
                maxY = p.y;
            }
        }
        this.mMaxY = maxY;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDistance maxY = ");
        stringBuilder.append(maxY);
        stringBuilder.append(", minY = ");
        stringBuilder.append(minY);
        Log.i(str, stringBuilder.toString());
        return maxY - minY;
    }

    private float getDistanceX() {
        float maxX = Float.MIN_VALUE;
        float minX = Float.MAX_VALUE;
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            Point p = (Point) this.mTouchingFingers.valueAt(i);
            if (p.x < minX) {
                minX = p.x;
            }
            if (p.x > maxX) {
                maxX = p.x;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDistance maxX = ");
        stringBuilder.append(maxX);
        stringBuilder.append(", minX = ");
        stringBuilder.append(minX);
        Log.i(str, stringBuilder.toString());
        return maxX - minX;
    }

    private long getInterval() {
        long startTime = Long.MAX_VALUE;
        long latestTime = Long.MIN_VALUE;
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            Point p = (Point) this.mTouchingFingers.valueAt(i);
            if (p.time < startTime) {
                startTime = p.time;
            }
            if (p.time > latestTime) {
                latestTime = p.time;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getInterval interval = ");
        stringBuilder.append(latestTime - startTime);
        Log.i(str, stringBuilder.toString());
        return latestTime - startTime;
    }

    private void startHandleTripleFingerSnap(MotionEvent motionEvent) {
        if (this.mPolicy == null) {
            this.mPolicy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        }
        if (this.mPolicy.isLandscape()) {
            this.mMinYTriggerDistance = this.mMinYTriggerLandscapeDistance;
        } else {
            this.mMinYTriggerDistance = this.mMinYTriggerPortraitDistance;
        }
        this.mHeight = (float) this.mContext.getResources().getDisplayMetrics().heightPixels;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleFingerDown mMinYTriggerDistance:");
        stringBuilder.append(this.mMinYTriggerDistance);
        stringBuilder.append(", mHeight:");
        stringBuilder.append(this.mHeight);
        Log.i(str, stringBuilder.toString());
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
            Point p = (Point) this.mTouchingFingers.get(id);
            String str;
            StringBuilder stringBuilder;
            if (p != null) {
                p.updateMoveDistance(curX, curY);
                if (!(this.mFilterCurrentTouch || curY == p.y)) {
                    moveCount++;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("handleFingerMove finger(");
                    stringBuilder.append(id);
                    stringBuilder.append(") moveCount:");
                    stringBuilder.append(moveCount);
                    Log.i(str, stringBuilder.toString());
                    if (moveCount == 3) {
                        Log.i(TAG, "handleFingerMove cancel");
                        this.mHandler.post(new Runnable() {
                            public void run() {
                                MotionEvent cancelEvent = motionEvent.copy();
                                cancelEvent.setAction(3);
                                HwFingersSnapshooter.this.mIm.injectInputEvent(cancelEvent, 2);
                            }
                        });
                        this.mFilterCurrentTouch = true;
                    }
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleFingerMove point(");
                stringBuilder.append(id);
                stringBuilder.append(") not tracked!");
                Log.i(str, stringBuilder.toString());
            }
        }
    }

    private void handleFingerUp(MotionEvent motionEvent) {
        int id = motionEvent.getPointerId(motionEvent.getActionIndex());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleFingerUp finger(");
        stringBuilder.append(id);
        stringBuilder.append(") up");
        Log.i(str, stringBuilder.toString());
        if (this.mFilterCurrentTouch) {
            int fingerSize = this.mTouchingFingers.size();
            if (fingerSize != 3) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleFingerUp, current touch has ");
                stringBuilder2.append(fingerSize);
                stringBuilder2.append(" fingers");
                Log.i(str2, stringBuilder2.toString());
                return;
            }
            boolean triggerSnapshot = true;
            for (int i = 0; i < fingerSize; i++) {
                float moveDistance = ((Point) this.mTouchingFingers.get(i)).moveY;
                if (moveDistance < this.mMinYTriggerDistance) {
                    boolean ret = this.mContext;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("{moveDistance:");
                    stringBuilder3.append(pixelsToDips(moveDistance));
                    stringBuilder3.append("}");
                    ret = Flog.bdReport(ret, CPUFeature.MSG_SET_BG_UIDS, stringBuilder3.toString());
                    String str3 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("errorState:finger(");
                    stringBuilder4.append(i);
                    stringBuilder4.append(") move ");
                    stringBuilder4.append(pixelsToDips(moveDistance));
                    stringBuilder4.append("dp, less than except distance! EventId:");
                    stringBuilder4.append(CPUFeature.MSG_SET_BG_UIDS);
                    stringBuilder4.append(" ret:");
                    stringBuilder4.append(ret);
                    Log.i(str3, stringBuilder4.toString());
                    triggerSnapshot = false;
                    break;
                }
            }
            if (triggerSnapshot) {
                resetState();
                boolean ret2 = Flog.bdReport(this.mContext, 141);
                String str4 = TAG;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("trigger the snapshot! EventId:141 ret:");
                stringBuilder5.append(ret2);
                Log.i(str4, stringBuilder5.toString());
                this.mHandler.post(this.mScreenshotRunnable);
            }
            return;
        }
        Log.i(TAG, "handleFingerUp, current touch not marked as filter");
    }

    private void handleFingerSnapShooterInit() {
        this.mDensity = this.mContext.getResources().getDisplayMetrics().density;
        this.mTrigerStartThreshold = (float) ((Context) checkNull("context", this.mContext)).getResources().getDimensionPixelSize(17105318);
        this.mEnabled = System.getIntForUser(this.mContext.getContentResolver(), KEY_TRIPLE_FINGER_MOTION, 0, -2);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor(KEY_TRIPLE_FINGER_MOTION), true, this.mTripleFingerMotionModeObserver, -1);
        this.mContext.registerReceiverAsUser(this.mFingerSnapReceiver, UserHandle.ALL, new IntentFilter(APS_RESOLUTION_CHANGE_ACTION), APS_RESOLUTION_CHANGE_PERSISSIONS, null);
        this.mContext.registerReceiverAsUser(this.mFingerSnapReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_SWITCHED"), null, null);
        this.mPolicy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        this.mMaxFingerDownYDistance = dipsToPixels(MAX_FINGER_DOWN_Y_DISTANCE);
        this.mMaxFingerDownXDistance = dipsToPixels(MAX_FINGER_DOWN_X_DISTANCE);
        this.mMinYTriggerLandscapeDistance = dipsToPixels(MIN_Y_TRIGGER_LANDSCAPE_DISTANCE);
        this.mMinYTriggerPortraitDistance = dipsToPixels(MIN_Y_TRIGGER_PORTRAIT_DISTANCE);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mTrigerStartThreshold:");
        stringBuilder.append(this.mTrigerStartThreshold);
        stringBuilder.append(", mEnabled:");
        stringBuilder.append(this.mEnabled);
        Log.i(str, stringBuilder.toString());
    }

    private void handleUserSwitch(int userId) {
        this.mEnabled = System.getIntForUser(this.mContext.getContentResolver(), KEY_TRIPLE_FINGER_MOTION, 0, -2);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onReceive ACTION_USER_SWITCHED  currentUserId= ");
        stringBuilder.append(userId);
        stringBuilder.append(", mEnabled:");
        stringBuilder.append(this.mEnabled);
        Log.i(str, stringBuilder.toString());
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg != null) {
            return arg;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(" must not be null");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    final float dipsToPixels(float dips) {
        return (this.mDensity * dips) + 0.5f;
    }

    final float pixelsToDips(float pixels) {
        return (pixels / this.mDensity) + 0.5f;
    }

    private boolean canDisableGesture() {
        if (inInValidArea()) {
            Log.i(TAG, "inInvalidarea");
            return true;
        } else if (SystemProperties.getBoolean(TALKBACK_CONFIG, true) && isTalkBackServicesOn()) {
            Log.i(TAG, "in talkback mode");
            return true;
        } else if (!SystemProperties.getBoolean("runtime.mmitest.isrunning", false)) {
            return false;
        } else {
            Log.i(TAG, "in MMI test");
            return true;
        }
    }

    private boolean isTalkBackServicesOn() {
        boolean z = false;
        if (this.mContext == null) {
            return false;
        }
        boolean accessibilityEnabled = Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_enabled", 0, -2) == 1;
        String enabledSerices = Secure.getStringForUser(this.mContext.getContentResolver(), "enabled_accessibility_services", -2);
        boolean isContainsTalkBackService = enabledSerices != null && enabledSerices.contains(TALKBACK_COMPONENT_NAME);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("accessibilityEnabled:");
        stringBuilder.append(accessibilityEnabled);
        stringBuilder.append(",isContainsTalkBackService:");
        stringBuilder.append(isContainsTalkBackService);
        Log.i(str, stringBuilder.toString());
        if (accessibilityEnabled && isContainsTalkBackService) {
            z = true;
        }
        return z;
    }

    private boolean inInValidArea() {
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            if (((Point) this.mTouchingFingers.valueAt(i)).y <= this.mTrigerStartThreshold) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:11:0x0036, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void takeScreenshot() {
        synchronized (this.mScreenshotLock) {
            if (this.mScreenshotConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName(FingerViewController.PKGNAME_OF_KEYGUARD, "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (HwFingersSnapshooter.this.mScreenshotLock) {
                        if (HwFingersSnapshooter.this.mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        msg.replyTo = new Messenger(new Handler(HwFingersSnapshooter.this.mHandler.getLooper()) {
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
                        msg.arg2 = 0;
                        msg.arg1 = 0;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            Log.e(HwFingersSnapshooter.TAG, "takeScreenshot: sending msg occured an error");
                        }
                    }
                }

                public void onServiceDisconnected(ComponentName name) {
                }
            };
            if (this.mContext.bindServiceAsUser(intent, conn, 1, UserHandle.CURRENT)) {
                this.mScreenshotConnection = conn;
                this.mHandler.postDelayed(this.mScreenshotTimeout, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
            }
        }
    }
}
