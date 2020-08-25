package com.android.server.input;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.freeform.HwFreeFormUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.IDockedStackListener;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.WindowManagerGlobal;
import android.widget.Toast;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavConst;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.HwActivityTaskManager;
import java.util.List;

public class HwTripleFingersFreeForm {
    private static final String ACCESSIBILITY_SCREENREADER_ENABLED = "accessibility_screenreader_enabled";
    private static final String APS_RESOLUTION_CHANGE_ACTION = "huawei.intent.action.APS_RESOLUTION_CHANGE_ACTION";
    private static final String APS_RESOLUTION_CHANGE_PERSISSIONS = "huawei.intent.permissions.APS_RESOLUTION_CHANGE_ACTION";
    private static final float DENSITY_INIT = 2.0f;
    private static final float DIP_TO_PIXEL_OFFSET = 0.5f;
    private static final boolean IS_DISABLE_MULTIWIN = SystemProperties.getBoolean("ro.huawei.disable_multiwindow", false);
    private static final boolean IS_MULTIWINDOW_OPTIMIZATION = SystemProperties.getBoolean("ro.config.hw_multiwindow_optimization", false);
    private static final float MAX_FINGER_DOWN_INTERVAL = 200.0f;
    private static final float MAX_FINGER_DOWN_X_DISTANCE = 400.0f;
    private static final float MAX_FINGER_DOWN_Y_DISTANCE = 145.0f;
    private static final float MIN_Y_TRIGGER_LANDSCAPE_DISTANCE = 90.0f;
    private static final float MIN_Y_TRIGGER_PORTRAIT_DISTANCE = 120.0f;
    public static final int MSG_HANDLE_FREEFORM_INIT = 0;
    public static final int MSG_SHOW_SPLIT_TOAST = 1;
    private static final String TAG = "HwTripleFingersFreeForm";
    private static final String TALKBACK_CONFIG = "ro.config.hw_talkback_btn";
    private static final int TRIGGER_MIN_FINGERS = 3;
    ActivityManager mAm = null;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public float mDensity = 2.0f;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public HwInputManagerService mIm = null;
    /* access modifiers changed from: private */
    public boolean mIsDockedStackExists = false;
    private boolean mIsFilter = false;
    private boolean mIsFilterCurrentTouch = false;
    /* access modifiers changed from: private */
    public float mMaxFingerDownDistanceX = 0.0f;
    /* access modifiers changed from: private */
    public float mMaxFingerDownDistanceY = 0.0f;
    private float mMinTriggerDistanceY = 0.0f;
    /* access modifiers changed from: private */
    public float mMinTriggerLandscapeDistanceY = 0.0f;
    /* access modifiers changed from: private */
    public float mMinTriggerPortraitDistanceY = 0.0f;
    private HwPhoneWindowManager mPolicy;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.server.input.HwTripleFingersFreeForm.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && HwTripleFingersFreeForm.APS_RESOLUTION_CHANGE_ACTION.equals(intent.getAction())) {
                HwTripleFingersFreeForm hwTripleFingersFreeForm = HwTripleFingersFreeForm.this;
                float unused = hwTripleFingersFreeForm.mDensity = hwTripleFingersFreeForm.mContext.getResources().getDisplayMetrics().density;
                HwTripleFingersFreeForm hwTripleFingersFreeForm2 = HwTripleFingersFreeForm.this;
                float unused2 = hwTripleFingersFreeForm2.mMaxFingerDownDistanceY = hwTripleFingersFreeForm2.dipsToPixels(HwTripleFingersFreeForm.MAX_FINGER_DOWN_Y_DISTANCE);
                HwTripleFingersFreeForm hwTripleFingersFreeForm3 = HwTripleFingersFreeForm.this;
                float unused3 = hwTripleFingersFreeForm3.mMaxFingerDownDistanceX = hwTripleFingersFreeForm3.dipsToPixels(HwTripleFingersFreeForm.MAX_FINGER_DOWN_X_DISTANCE);
                HwTripleFingersFreeForm hwTripleFingersFreeForm4 = HwTripleFingersFreeForm.this;
                float unused4 = hwTripleFingersFreeForm4.mMinTriggerLandscapeDistanceY = hwTripleFingersFreeForm4.dipsToPixels(HwTripleFingersFreeForm.MIN_Y_TRIGGER_LANDSCAPE_DISTANCE);
                HwTripleFingersFreeForm hwTripleFingersFreeForm5 = HwTripleFingersFreeForm.this;
                float unused5 = hwTripleFingersFreeForm5.mMinTriggerPortraitDistanceY = hwTripleFingersFreeForm5.dipsToPixels(HwTripleFingersFreeForm.MIN_Y_TRIGGER_PORTRAIT_DISTANCE);
                Log.i(HwTripleFingersFreeForm.TAG, "MaxYDistance:" + HwTripleFingersFreeForm.this.mMaxFingerDownDistanceY + "MaxXDistance:" + HwTripleFingersFreeForm.this.mMaxFingerDownDistanceX + ",MinLandDistance:" + HwTripleFingersFreeForm.this.mMinTriggerLandscapeDistanceY + ",MinPortraitDistance:" + HwTripleFingersFreeForm.this.mMinTriggerPortraitDistanceY);
            }
        }
    };
    private SparseArray<Point> mTouchingFingers = new SparseArray<>();
    private float mTrigerStartThreshold = 0.0f;
    private float mTrigerStartThresholdAbove = 0.0f;

    private final class FreeFormHandler extends Handler {
        private FreeFormHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                HwTripleFingersFreeForm.this.handleFreeFormInit();
            } else if (i == 1) {
                if (HwTripleFingersFreeForm.this.isTopTaskHome()) {
                    HwTripleFingersFreeForm hwTripleFingersFreeForm = HwTripleFingersFreeForm.this;
                    hwTripleFingersFreeForm.showToastForAllUser(hwTripleFingersFreeForm.mContext, 33686211);
                    return;
                }
                HwTripleFingersFreeForm hwTripleFingersFreeForm2 = HwTripleFingersFreeForm.this;
                hwTripleFingersFreeForm2.showToastForAllUser(hwTripleFingersFreeForm2.mContext, 33685924);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isTopTaskHome() {
        List<ActivityManager.RunningTaskInfo> tasks;
        ActivityManager.RunningTaskInfo topTask;
        ActivityManager activityManager = this.mAm;
        if (activityManager == null || (tasks = activityManager.getRunningTasks(1)) == null || tasks.isEmpty() || (topTask = tasks.get(0)) == null || !isInHomeStack(topTask)) {
            return false;
        }
        return true;
    }

    private static class Point {
        /* access modifiers changed from: private */
        public float moveY;
        /* access modifiers changed from: private */
        public float pointX;
        /* access modifiers changed from: private */
        public float pointY;
        /* access modifiers changed from: private */
        public long time;

        private Point(float ax, float ay, long inTime) {
            this.pointX = ax;
            this.pointY = ay;
            this.time = inTime;
            this.moveY = 0.0f;
        }

        /* access modifiers changed from: private */
        public void updateMoveDistance(float ay) {
            this.moveY = ay - this.pointY;
        }
    }

    public HwTripleFingersFreeForm(Context context, HwInputManagerService inputManager) {
        this.mContext = context;
        this.mIm = inputManager;
        this.mAm = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        if (!IS_DISABLE_MULTIWIN) {
            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();
            this.mHandler = new FreeFormHandler(handlerThread.getLooper());
            this.mHandler.sendEmptyMessage(0);
        }
    }

    /* access modifiers changed from: private */
    public void handleFreeFormInit() {
        this.mDensity = this.mContext.getResources().getDisplayMetrics().density;
        this.mTrigerStartThreshold = (float) this.mContext.getResources().getDimensionPixelSize(17105443);
        this.mTrigerStartThresholdAbove = (float) this.mContext.getResources().getDimensionPixelSize(34472510);
        this.mPolicy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        this.mMaxFingerDownDistanceY = dipsToPixels(MAX_FINGER_DOWN_Y_DISTANCE);
        this.mMaxFingerDownDistanceX = dipsToPixels(MAX_FINGER_DOWN_X_DISTANCE);
        this.mMinTriggerLandscapeDistanceY = dipsToPixels(MIN_Y_TRIGGER_LANDSCAPE_DISTANCE);
        this.mMinTriggerPortraitDistanceY = dipsToPixels(MIN_Y_TRIGGER_PORTRAIT_DISTANCE);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, new IntentFilter(APS_RESOLUTION_CHANGE_ACTION), APS_RESOLUTION_CHANGE_PERSISSIONS, null);
    }

    public boolean handleMotionEvent(InputEvent event) {
        if (HwFreeFormUtils.getFreeFormStackVisible()) {
            return true;
        }
        if (event instanceof MotionEvent) {
            MotionEvent motionEvent = (MotionEvent) event;
            int action = motionEvent.getActionMasked();
            int id = motionEvent.getPointerId(motionEvent.getActionIndex());
            long time = motionEvent.getEventTime();
            if (action == 0) {
                resetState();
                this.mTouchingFingers.put(id, new Point(motionEvent.getRawX(), motionEvent.getRawY(), time));
            } else if (action == 1) {
                resetState();
            } else if (action != 2) {
                if (action == 5) {
                    this.mIsFilter = handleFingerDown(motionEvent);
                } else if (action == 6) {
                    handleFingerUp(motionEvent);
                    this.mTouchingFingers.delete(id);
                    this.mIsFilterCurrentTouch = false;
                }
            } else if (this.mIsFilter) {
                handleFingerMove(motionEvent);
            }
            return true ^ this.mIsFilterCurrentTouch;
        }
        Log.i(TAG, "handleMotionEvent not a motionEvent");
        return true;
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
        if (fingerSize != 3) {
            Log.i(TAG, "handleFingerDown " + fingerSize + " fingers touching down");
            return false;
        } else if (getDistanceY() > this.mMaxFingerDownDistanceY) {
            Log.i(TAG, "fingers touch down on the screen exceeds to much on Y");
            return false;
        } else if (getDistanceX() > this.mMaxFingerDownDistanceX) {
            Log.i(TAG, "fingers touch down on the screen exceeds to much on X");
            return false;
        } else if (((float) getInterval()) > MAX_FINGER_DOWN_INTERVAL) {
            Log.i(TAG, "fingers'interval longer than except time!");
            return false;
        } else if (shouldDisableGesture()) {
            return false;
        } else {
            if (this.mIsDockedStackExists) {
                Log.i(TAG, "DockedStackExist!");
                return false;
            }
            startHandleTripleFinger(motionEvent);
            return true;
        }
    }

    private void handleSplitScreenGesture() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.post(new Runnable() {
                /* class com.android.server.input.HwTripleFingersFreeForm.AnonymousClass2 */

                public void run() {
                    Log.i(HwTripleFingersFreeForm.TAG, "handleFingerUp toggle split to systemUI");
                    ((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).toggleSplitScreen();
                }
            });
        }
    }

    private void startDockTipDialogActivity() {
        Log.i(TAG, "startDockTipDialogActivity");
        Intent intent = new Intent("com.huawei.hwdockbar.action");
        intent.putExtra("AROUSAL_MODE", "triple");
        intent.addFlags(67108864);
        try {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "start hwdockbar ActivityNotFoundException");
        }
    }

    private void startHandleTripleFinger(MotionEvent motionEvent) {
        if (this.mPolicy == null) {
            this.mPolicy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        }
        if (this.mPolicy.isLandscape()) {
            this.mMinTriggerDistanceY = -this.mMinTriggerLandscapeDistanceY;
        } else {
            this.mMinTriggerDistanceY = -this.mMinTriggerPortraitDistanceY;
        }
    }

    private void handleFingerMove(MotionEvent motionEvent) {
        float offsetY = motionEvent.getY() - motionEvent.getRawY();
        int pointerCount = motionEvent.getPointerCount();
        int moveCount = 0;
        for (int i = 0; i < pointerCount; i++) {
            float curY = motionEvent.getY(i) + offsetY;
            int id = motionEvent.getPointerId(i);
            Point point = this.mTouchingFingers.get(id);
            if (point == null) {
                Log.i(TAG, "handleFingerMove point(" + id + ") not tracked!");
            } else {
                point.updateMoveDistance(curY);
                if (!this.mIsFilterCurrentTouch && curY < point.pointY) {
                    moveCount++;
                    Log.i(TAG, "handleFingerMove finger(" + id + ") moveCount:" + moveCount);
                    if (moveCount == 3) {
                        postCancelEvent(motionEvent);
                        this.mIsFilterCurrentTouch = true;
                    }
                }
            }
        }
    }

    private void postCancelEvent(final MotionEvent motionEvent) {
        Log.i(TAG, "handleFingerMove cancel");
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.post(new Runnable() {
                /* class com.android.server.input.HwTripleFingersFreeForm.AnonymousClass3 */

                public void run() {
                    MotionEvent cancelEvent = motionEvent.copy();
                    cancelEvent.setAction(3);
                    HwTripleFingersFreeForm.this.mIm.injectInputEvent(cancelEvent, 2);
                }
            });
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
            Log.i(TAG, "handleFingerUp fingerSize = " + fingerSize);
        } else if (SystemProperties.getInt("sys.ride_mode", 0) == 1) {
            Log.i(TAG, "can not split in Ride mode");
        } else {
            boolean isDistanceLongEnough = true;
            int i = 0;
            while (true) {
                if (i >= fingerSize) {
                    break;
                }
                float moveDistance = this.mTouchingFingers.get(i).moveY;
                if (moveDistance > this.mMinTriggerDistanceY) {
                    Log.i(TAG, "move " + pixelsToDips(moveDistance) + "dp, less than except distance!");
                    isDistanceLongEnough = false;
                    break;
                }
                i++;
            }
            if (isDistanceLongEnough) {
                resetState();
                if (IS_MULTIWINDOW_OPTIMIZATION) {
                    startDockTipDialogActivity();
                } else if (!isMultiWindowDisabled()) {
                    if (shouldSplit() || isSimpleUi()) {
                        handleSplitScreenGesture();
                        updateDockedStackFlag();
                    } else if (this.mHandler != null) {
                        Log.i(TAG, "app do not surpot split show toast");
                        this.mHandler.sendEmptyMessage(1);
                    }
                }
            }
        }
    }

    private boolean shouldSplit() {
        ActivityManager.RunningTaskInfo topTask = getTopMostTask();
        if (topTask == null || isInHomeStack(topTask) || !topTask.supportsSplitScreenMultiWindow) {
            return false;
        }
        return true;
    }

    private void resetState() {
        this.mIsFilterCurrentTouch = false;
        this.mIsFilter = false;
        this.mTouchingFingers.clear();
    }

    private boolean isUserUnlocked() {
        return ((UserManager) this.mContext.getSystemService("user")).isUserUnlocked(ActivityManager.getCurrentUser());
    }

    private boolean shouldDisableGesture() {
        if (IS_DISABLE_MULTIWIN) {
            Log.i(TAG, "product is not support split");
            return true;
        } else if (inInValidArea()) {
            Log.i(TAG, "finger is too close to navigation");
            return true;
        } else if (isGameMode()) {
            Log.i(TAG, "can not split in gaming");
            return true;
        } else if (SystemProperties.getBoolean(TALKBACK_CONFIG, true) && isTalkBackServicesOn()) {
            Log.i(TAG, "can not split in talkback mode");
            return true;
        } else if (SystemProperties.getBoolean("runtime.mmitest.isrunning", false)) {
            Log.i(TAG, "can not split in MMI test");
            return true;
        } else if (SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false)) {
            Log.i(TAG, "can not split in superpower");
            return true;
        } else if (isUserUnlocked()) {
            return false;
        } else {
            Log.i(TAG, "Do not allow split if user is unlocked");
            return true;
        }
    }

    private boolean isTalkBackServicesOn() {
        Context context = this.mContext;
        if (context == null) {
            return false;
        }
        boolean isAccessibilityEnabled = Settings.Secure.getIntForUser(context.getContentResolver(), "accessibility_enabled", 0, -2) == 1;
        boolean isContainsTalkBackService = Settings.Secure.getInt(this.mContext.getContentResolver(), "accessibility_screenreader_enabled", 0) == 1;
        if (!isAccessibilityEnabled || !isContainsTalkBackService) {
            return false;
        }
        return true;
    }

    private boolean inInValidArea() {
        int fingerSize = this.mTouchingFingers.size();
        float height = (float) this.mContext.getResources().getDisplayMetrics().heightPixels;
        for (int i = 0; i < fingerSize; i++) {
            float fromY = this.mTouchingFingers.valueAt(i).pointY;
            if (fromY <= this.mTrigerStartThreshold || fromY >= height - this.mTrigerStartThresholdAbove) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Multiple debug info for r5v2 long: [D('interval' long), D('i' int)] */
    private long getInterval() {
        long startTime = Long.MAX_VALUE;
        long latestTime = Long.MIN_VALUE;
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            Point point = this.mTouchingFingers.valueAt(i);
            if (point.time < startTime) {
                startTime = point.time;
            }
            if (point.time > latestTime) {
                latestTime = point.time;
            }
        }
        return latestTime - startTime;
    }

    /* access modifiers changed from: package-private */
    public final float dipsToPixels(float dips) {
        return (this.mDensity * dips) + 0.5f;
    }

    /* access modifiers changed from: package-private */
    public final float pixelsToDips(float pixels) {
        return (pixels / this.mDensity) + 0.5f;
    }

    private ActivityManager.RunningTaskInfo getTopMostTask() {
        List<ActivityManager.RunningTaskInfo> tasks = getRunningTasks(1);
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        return tasks.get(0);
    }

    private List<ActivityManager.RunningTaskInfo> getRunningTasks(int numTasks) {
        ActivityManager activityManager = this.mAm;
        if (activityManager == null) {
            return null;
        }
        return activityManager.getRunningTasks(numTasks);
    }

    private boolean isInHomeStack(ActivityManager.RunningTaskInfo runningTask) {
        if (runningTask != null && runningTask.configuration.windowConfiguration.getActivityType() == 2) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void showToastForAllUser(final Context context, final int message) {
        if (context != null) {
            runOnUiThread(new Runnable() {
                /* class com.android.server.input.HwTripleFingersFreeForm.AnonymousClass4 */

                public void run() {
                    Toast toast = Toast.makeText(context, message, 0);
                    toast.getWindowParams().privateFlags |= 16;
                    toast.show();
                }
            });
        }
    }

    private void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler();
        if (handler.getLooper() != Looper.myLooper()) {
            handler.post(runnable);
        } else {
            runnable.run();
        }
    }

    private boolean isSimpleUi() {
        int simpleuiVal = Settings.System.getIntForUser(this.mContext.getContentResolver(), "simpleui_mode", 0, ActivityManager.getCurrentUser());
        if (simpleuiVal == 2 || simpleuiVal == 5) {
            return true;
        }
        return false;
    }

    private boolean isGameMode() {
        return ActivityManagerEx.isGameDndOn();
    }

    private float getDistanceY() {
        float maxY = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            Point point = this.mTouchingFingers.valueAt(i);
            if (point.pointY < minY) {
                minY = point.pointY;
            }
            if (point.pointY > maxY) {
                maxY = point.pointY;
            }
        }
        float distanceY = maxY - minY;
        Log.i(TAG, "getDistance maxY = " + maxY + ", minY = " + minY);
        return distanceY;
    }

    private float getDistanceX() {
        float maxX = Float.MIN_VALUE;
        float minX = Float.MAX_VALUE;
        int fingerSize = this.mTouchingFingers.size();
        for (int i = 0; i < fingerSize; i++) {
            Point point = this.mTouchingFingers.valueAt(i);
            if (point.pointX < minX) {
                minX = point.pointX;
            }
            if (point.pointX > maxX) {
                maxX = point.pointX;
            }
        }
        float distanceX = maxX - minX;
        Log.i(TAG, "getDistance maxX = " + maxX + ", minX = " + minX);
        return distanceX;
    }

    private void updateDockedStackFlag() {
        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(new IDockedStackListener.Stub() {
                /* class com.android.server.input.HwTripleFingersFreeForm.AnonymousClass5 */

                public void onDividerVisibilityChanged(boolean isVisible) throws RemoteException {
                }

                public void onDockedStackExistsChanged(boolean isExist) throws RemoteException {
                    boolean unused = HwTripleFingersFreeForm.this.mIsDockedStackExists = isExist;
                }

                public void onDockedStackMinimizedChanged(boolean isMinimized, long animDuration, boolean isHomeStackResizable) throws RemoteException {
                }

                public void onAdjustedForImeChanged(boolean isAdjustedForIme, long animDuration) throws RemoteException {
                }

                public void onDockSideChanged(int newDockSide) throws RemoteException {
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Failed registering docked stack exists listener");
        }
    }

    private boolean isMultiWindowDisabled() {
        return HwActivityTaskManager.getMultiWindowDisabled();
    }
}
