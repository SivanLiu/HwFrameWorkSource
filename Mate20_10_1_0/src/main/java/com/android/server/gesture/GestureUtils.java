package com.android.server.gesture;

import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.hardware.input.InputManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import com.android.server.LocalServices;
import com.android.server.input.HwInputManagerService;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.policy.WindowManagerPolicy;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.os.HwVibrator;
import com.huawei.hiai.awareness.AwarenessConstants;
import java.util.ArrayList;
import java.util.BitSet;

public class GestureUtils {
    private static final int DEFAULT_BUTTON_STATE = 0;
    public static final int DEFAULT_DEVICE_ID = 0;
    private static final int DEFAULT_EDGE_FLAGS = 0;
    private static final int DEFAULT_EVENT_FLAGS = 0;
    private static final int DEFAULT_META_STATE = 0;
    private static final float DEFAULT_PRECISION_X = 1.0f;
    private static final float DEFAULT_PRECISION_Y = 1.0f;
    private static final float DEFAULT_PRESSURE_DOWN = 1.0f;
    private static final float DEFAULT_PRESSURE_UP = 0.0f;
    private static final float DEFAULT_SIZE = 1.0f;
    private static final boolean IS_BACK_GESTURE_HAPTIC_FEEDBACK_EN = SystemProperties.getBoolean("ro.config.backgesture_haptic_feedback", false);
    private static final String KEY_CURVED_SIDE_DISP = "ro.config.hw_curved_side_disp";
    private static final int MOTION_EVENT_INJECTION_DELAY_MILLIS = 5;
    private static final Object SHARED_TEMP_LOCK = new Object();
    private static boolean isCurvedSide = false;
    private static boolean mHasInit = false;
    private static boolean mHasNotch = false;
    private static int mLeftCurvedSideDisp = 0;
    private static int mRightCurvedSideDisp = 0;
    private static boolean mSupportEffectVb = false;
    private static MotionEvent.PointerCoords[] sPointerCoords;
    private static MotionEvent.PointerProperties[] sPointerProps;

    public static void getCurvedSideDisp() {
        String[] curvedSideDisps = SystemProperties.get(KEY_CURVED_SIDE_DISP).split(",");
        if (curvedSideDisps.length != 4) {
            isCurvedSide = false;
            mLeftCurvedSideDisp = 0;
            mRightCurvedSideDisp = 0;
            return;
        }
        try {
            mLeftCurvedSideDisp = Integer.parseInt(curvedSideDisps[0]);
            mRightCurvedSideDisp = Integer.parseInt(curvedSideDisps[2]);
        } catch (NumberFormatException e) {
            Log.e(GestureNavConst.TAG_GESTURE_UTILS, "parseInt fail with NumberFormatException");
        }
        isCurvedSide = true;
    }

    public static int getCurvedSideLeftDisp() {
        return mLeftCurvedSideDisp;
    }

    public static int getCurvedSideRightDisp() {
        return mRightCurvedSideDisp;
    }

    public static boolean isCurvedSideDisp() {
        return isCurvedSide;
    }

    public static void systemReady() {
        if (!mHasInit) {
            mHasNotch = parseHole();
            mSupportEffectVb = HwVibrator.isSupportHwVibrator("haptic.virtual_navigation.click_back");
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_UTILS, "systemReady hasNotch=" + mHasNotch + ", effectVb=" + mSupportEffectVb);
            }
            getCurvedSideDisp();
            mHasInit = true;
        }
    }

    public static boolean parseHole() {
        String[] props = SystemProperties.get("ro.config.hw_notch_size", "").split(",");
        if (props == null || props.length != 4) {
            return false;
        }
        Log.d(GestureNavConst.TAG_GESTURE_UTILS, "prop hole height:" + Integer.parseInt(props[1]));
        return true;
    }

    public static boolean hasNotch() {
        return mHasNotch;
    }

    public static boolean isSupportEffectVibrator() {
        return mSupportEffectVb;
    }

    public static int getInputDeviceId(int inputSource) {
        int[] devIds = InputDevice.getDeviceIds();
        for (int devId : devIds) {
            InputDevice inputDev = InputDevice.getDevice(devId);
            if (inputDev != null && inputDev.supportsSource(inputSource)) {
                return devId;
            }
        }
        return 0;
    }

    public static final int getActiveActionIndex(int action) {
        return (65280 & action) >> 8;
    }

    public static final int getActivePointerId(MotionEvent event, int action) {
        return event.getPointerId(getActiveActionIndex(action));
    }

    public static final class PointerState {
        public int action;
        public int activePointerId;
        public float x;
        public float y;

        public PointerState(int activePointerId2, int action2, float positionX, float positionY) {
            this.activePointerId = activePointerId2;
            this.action = action2;
            this.x = positionX;
            this.y = positionY;
        }
    }

    public static void sendKeyEvent(int keycode) {
        int[] actions;
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_UTILS, "sendKeyEvent keycode=" + keycode);
        }
        long now = SystemClock.uptimeMillis();
        for (int i : new int[]{0, 1}) {
            InputManager.getInstance().injectInputEvent(new KeyEvent(now, now, i, keycode, 0, 0, -1, 0, 8, 257), 0);
        }
    }

    public static void sendTap(float posX, float posY, int deviceId, int source, int toolType) {
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_UTILS, "sendTap (" + posX + ", " + posY + ")");
        }
        long downTime = SystemClock.uptimeMillis();
        injectMotionEvent(0, downTime, downTime, posX, posY, 1.0f, deviceId, source, toolType);
        injectMotionEvent(1, downTime, SystemClock.uptimeMillis(), posX, posY, 0.0f, deviceId, source, toolType);
    }

    public static void sendSwipe(float x1, float y1, float x2, float y2, int duration, int deviceId, int source, int toolType, ArrayList<PointF> pendingMovePoints, boolean hasMultiTouched) {
        int adjustedDuration;
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_UTILS, "sendSwipe (" + x1 + ", " + y1 + ") to (" + x2 + ", " + y2 + "), duration:" + duration);
        }
        if (duration < 80) {
            adjustedDuration = 80;
        } else if (duration > 500) {
            adjustedDuration = 500;
        } else {
            adjustedDuration = duration;
        }
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(0, now, now, x1, y1, 1.0f, deviceId, source, toolType);
        long size = 0;
        if (!hasMultiTouched && pendingMovePoints != null) {
            long size2 = (long) pendingMovePoints.size();
            size = size2;
            if (size2 > 0) {
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_UTILS, "inject " + size + " pending move points");
                }
                for (int i = 0; ((long) i) < size; i++) {
                    injectMotionEvent(2, now, now, pendingMovePoints.get(i).x, pendingMovePoints.get(i).y, 1.0f, deviceId, source, toolType);
                    SystemClock.sleep(5);
                    now = SystemClock.uptimeMillis();
                }
                injectMotionEvent(1, now, now, x2, y2, 0.0f, deviceId, source, toolType);
            }
        }
        long endTime = now + ((long) adjustedDuration);
        while (now < endTime) {
            float alpha = ((float) (now - now)) / ((float) adjustedDuration);
            injectMotionEvent(2, now, now, lerp(x1, x2, alpha), lerp(y1, y2, alpha), 1.0f, deviceId, source, toolType);
            SystemClock.sleep(5);
            now = SystemClock.uptimeMillis();
        }
        injectMotionEvent(1, now, now, x2, y2, 0.0f, deviceId, source, toolType);
    }

    public static void injectMotionEvent(int action, long downTime, long eventTime, float posX, float posY, int deviceId, int source, int toolType) {
        injectMotionEvent(action, downTime, eventTime, posX, posY, action == 1 ? 0.0f : 1.0f, deviceId, source, toolType);
    }

    public static void injectMotionEvent(int action, long downTime, long eventTime, float posX, float posY, float pressure, int deviceId, int source, int toolType) {
        injectTransferMotionEvent(obtainMotionEvent(downTime, eventTime, action, posX, posY, pressure, deviceId, source, toolType));
    }

    public static void injectMotionEvent(MotionEvent event, int appendPolicyFlag) {
        HwInputManagerService.HwInputManagerLocalService hwInputManagerInternal = (HwInputManagerService.HwInputManagerLocalService) LocalServices.getService(HwInputManagerService.HwInputManagerLocalService.class);
        if (hwInputManagerInternal != null) {
            hwInputManagerInternal.injectInputEvent(event, 0, appendPolicyFlag);
        }
    }

    public static void injectTransferMotionEvent(MotionEvent event) {
        injectMotionEvent(event, AwarenessConstants.MSDP_ENVIRONMENT_TYPE_WAY_OFFICE);
    }

    public static void injectDownWithBatchMoveEvent(long downTime, float downX, float downY, ArrayList<PointF> batchMovePoints, long durationTime, int deviceId, int source, int toolType) {
        int appendPolicyFlag;
        int size;
        MotionEvent event = obtainMotionEvent(downTime, downTime, 0, downX, downY, 1.0f, deviceId, source, toolType);
        if (batchMovePoints == null || (size = batchMovePoints.size()) <= 0) {
            appendPolicyFlag = 524288;
        } else {
            appendPolicyFlag = 524288 | 262144;
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_UTILS, "inject down with " + size + " batch move points");
            }
            for (int i = 0; i < size; i++) {
                event.addBatch(downTime + ((long) (((((float) (i + 1)) * 1.0f) / ((float) size)) * ((float) durationTime))), batchMovePoints.get(i).x, batchMovePoints.get(i).y, 1.0f, 1.0f, 0);
            }
        }
        injectMotionEvent(event, appendPolicyFlag);
    }

    public static MotionEvent obtainMotionEvent(long downTime, long eventTime, int action, float posX, float posY, float pressure, int deviceId, int source, int toolType) {
        MotionEvent obtain;
        synchronized (SHARED_TEMP_LOCK) {
            if (sPointerProps == null) {
                sPointerProps = new MotionEvent.PointerProperties[1];
                sPointerProps[0] = new MotionEvent.PointerProperties();
            }
            MotionEvent.PointerProperties[] pp = sPointerProps;
            pp[0].clear();
            pp[0].id = 0;
            pp[0].toolType = toolType;
            if (sPointerCoords == null) {
                sPointerCoords = new MotionEvent.PointerCoords[1];
                sPointerCoords[0] = new MotionEvent.PointerCoords();
            }
            MotionEvent.PointerCoords[] pc = sPointerCoords;
            pc[0].clear();
            pc[0].x = posX;
            pc[0].y = posY;
            pc[0].pressure = pressure;
            pc[0].size = 1.0f;
            obtain = MotionEvent.obtain(downTime, eventTime, action, 1, pp, pc, 0, 0, 1.0f, 1.0f, deviceId, 0, source, 0);
        }
        return obtain;
    }

    public static void sendMultiPointerDown(ArrayList<PointerState> pendingPointerStates, int maxPointerCount, int deviceId, int source, int toolType, long firstDownTime, long durationTime) {
        sendMultiPointerGesture(pendingPointerStates, maxPointerCount, deviceId, source, toolType, true, true, firstDownTime, durationTime);
    }

    public static void sendMultiPointerTap(ArrayList<PointerState> pendingPointerStates, int maxPointerCount, int deviceId, int source, int toolType) {
        sendMultiPointerGesture(pendingPointerStates, maxPointerCount, deviceId, source, toolType, true, false, 0, 0);
    }

    public static void sendPointerUp(MotionEvent event) {
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_UTILS, "sendPointerUp");
        }
        int pointerCount = event.getPointerCount();
        MotionEvent.PointerProperties[] props = MotionEvent.PointerProperties.createArray(pointerCount);
        MotionEvent.PointerCoords[] coords = MotionEvent.PointerCoords.createArray(pointerCount);
        for (int i = 0; i < pointerCount; i++) {
            event.getPointerProperties(i, props[i]);
            coords[i].clear();
            coords[i].x = event.getRawX(i);
            coords[i].y = event.getRawY(i);
            coords[i].pressure = event.getPressure(i);
            coords[i].size = event.getSize(i);
        }
        MotionEvent motionEvent = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), pointerCount, props, coords, event.getMetaState(), event.getButtonState(), 1.0f, 1.0f, event.getDeviceId(), event.getEdgeFlags(), event.getSource(), event.getFlags());
        if (motionEvent != null) {
            injectTransferMotionEvent(motionEvent);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00a2, code lost:
        if (r15 != 6) goto L_0x00dd;
     */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x018b  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x0194  */
    public static void sendMultiPointerGesture(ArrayList<PointerState> pendingPointerStates, int maxPointerCount, int deviceId, int source, int toolType, boolean skipMove, boolean skipUp, long firstDownTime, long durationTime) {
        MotionEvent.PointerProperties[] pp;
        MotionEvent.PointerCoords[] pc;
        BitSet idBits;
        SparseArray<PointF> idToPointer;
        int i;
        long eventTime;
        PointerState ps;
        ArrayList<PointerState> arrayList = pendingPointerStates;
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_UTILS, "sendMultiPointerGesture count:" + maxPointerCount + ", skipMove:" + skipMove + ", skipUp:" + skipUp);
        }
        if (arrayList != null) {
            int size = pendingPointerStates.size();
            int i2 = 2;
            if (size >= 2 && maxPointerCount >= 2 && arrayList.get(0).action == 0) {
                int i3 = 1;
                if (skipUp || arrayList.get(size - 1).action == 1) {
                    long downTime = firstDownTime > 0 ? firstDownTime : SystemClock.uptimeMillis();
                    MotionEvent.PointerProperties[] pp2 = new MotionEvent.PointerProperties[maxPointerCount];
                    MotionEvent.PointerCoords[] pc2 = new MotionEvent.PointerCoords[maxPointerCount];
                    BitSet idBits2 = new BitSet(maxPointerCount);
                    SparseArray<PointF> idToPointer2 = new SparseArray<>();
                    long eventTime2 = downTime;
                    int currentPointerCount = 0;
                    int i4 = 0;
                    while (i4 < size) {
                        MotionEvent event = null;
                        PointerState ps2 = arrayList.get(i4);
                        int maskAction = ps2.action & 255;
                        if (maskAction != 0) {
                            if (maskAction != i3) {
                                if (maskAction != i2) {
                                    if (maskAction == 5) {
                                        ps = ps2;
                                        i = i4;
                                        idToPointer = idToPointer2;
                                        idBits = idBits2;
                                        pc = pc2;
                                        pp = pp2;
                                    }
                                } else if (!skipMove) {
                                    event = MotionEvent.obtain(downTime, eventTime2, ps2.action, currentPointerCount, pp2, pc2, 0, 0, 1.0f, 1.0f, deviceId, 0, source, 0);
                                    i = i4;
                                    idToPointer = idToPointer2;
                                    idBits = idBits2;
                                    pc = pc2;
                                    pp = pp2;
                                    if (event != null) {
                                        injectTransferMotionEvent(event);
                                    }
                                    if (firstDownTime > 0 || durationTime <= 0) {
                                        eventTime = SystemClock.uptimeMillis();
                                    } else {
                                        eventTime = firstDownTime + ((long) (((((float) (i + 1)) * 1.0f) / ((float) size)) * ((float) durationTime)));
                                    }
                                    eventTime2 = eventTime;
                                    arrayList = pendingPointerStates;
                                    i4 = i + 1;
                                    idToPointer2 = idToPointer;
                                    idBits2 = idBits;
                                    pc2 = pc;
                                    pp2 = pp;
                                    i2 = 2;
                                    i3 = 1;
                                }
                                i = i4;
                                idToPointer = idToPointer2;
                                idBits = idBits2;
                                pc = pc2;
                                pp = pp2;
                                if (event != null) {
                                }
                                if (firstDownTime > 0) {
                                }
                                eventTime = SystemClock.uptimeMillis();
                                eventTime2 = eventTime;
                                arrayList = pendingPointerStates;
                                i4 = i + 1;
                                idToPointer2 = idToPointer;
                                idBits2 = idBits;
                                pc2 = pc;
                                pp2 = pp;
                                i2 = 2;
                                i3 = 1;
                            }
                            if (!skipUp) {
                                event = MotionEvent.obtain(downTime, eventTime2, ps2.action, currentPointerCount, pp2, pc2, 0, 0, 1.0f, 1.0f, deviceId, 0, source, 0);
                            }
                            idBits2.clear(ps2.activePointerId);
                            idToPointer2.put(ps2.activePointerId, new PointF(ps2.x, ps2.y));
                            i = i4;
                            idToPointer = idToPointer2;
                            idBits = idBits2;
                            pc = pc2;
                            pp = pp2;
                            currentPointerCount = fillPointerEvent(maxPointerCount, pp2, pc2, toolType, idBits, idToPointer);
                            if (event != null) {
                            }
                            if (firstDownTime > 0) {
                            }
                            eventTime = SystemClock.uptimeMillis();
                            eventTime2 = eventTime;
                            arrayList = pendingPointerStates;
                            i4 = i + 1;
                            idToPointer2 = idToPointer;
                            idBits2 = idBits;
                            pc2 = pc;
                            pp2 = pp;
                            i2 = 2;
                            i3 = 1;
                        } else {
                            ps = ps2;
                            i = i4;
                            idToPointer = idToPointer2;
                            idBits = idBits2;
                            pc = pc2;
                            pp = pp2;
                        }
                        idBits.set(ps.activePointerId);
                        idToPointer.put(ps.activePointerId, new PointF(ps.x, ps.y));
                        int currentPointerCount2 = fillPointerEvent(maxPointerCount, pp, pc, toolType, idBits, idToPointer);
                        event = MotionEvent.obtain(downTime, eventTime2, ps.action, currentPointerCount2, pp, pc, 0, 0, 1.0f, 1.0f, deviceId, 0, source, 0);
                        currentPointerCount = currentPointerCount2;
                        if (event != null) {
                        }
                        if (firstDownTime > 0) {
                        }
                        eventTime = SystemClock.uptimeMillis();
                        eventTime2 = eventTime;
                        arrayList = pendingPointerStates;
                        i4 = i + 1;
                        idToPointer2 = idToPointer;
                        idBits2 = idBits;
                        pc2 = pc;
                        pp2 = pp;
                        i2 = 2;
                        i3 = 1;
                    }
                }
            }
        }
    }

    public static int fillPointerEvent(int maxPointerCount, MotionEvent.PointerProperties[] pp, MotionEvent.PointerCoords[] pc, int toolType, BitSet idBits, SparseArray<PointF> idToPointer) {
        int currentPointerCount = 0;
        int idSize = idBits.size();
        for (int j = 0; j < idSize; j++) {
            if (idBits.get(j)) {
                if (currentPointerCount >= maxPointerCount) {
                    return maxPointerCount;
                }
                pp[currentPointerCount] = new MotionEvent.PointerProperties();
                pp[currentPointerCount].id = j;
                pp[currentPointerCount].toolType = toolType;
                pc[currentPointerCount] = new MotionEvent.PointerCoords();
                pc[currentPointerCount].clear();
                pc[currentPointerCount].x = idToPointer.get(j).x;
                pc[currentPointerCount].y = idToPointer.get(j).y;
                pc[currentPointerCount].pressure = 1.0f;
                pc[currentPointerCount].size = 1.0f;
                currentPointerCount++;
            }
        }
        return currentPointerCount;
    }

    private static float lerp(float a, float b, float alpha) {
        return ((b - a) * alpha) + a;
    }

    public static double angle(float distanceX, float distanceY, boolean isDivY) {
        if ((isDivY ? distanceY : distanceX) == 0.0f) {
            return 90.0d;
        }
        return (Math.atan((double) (isDivY ? distanceX / distanceY : distanceY / distanceX)) / 3.141592653589793d) * 180.0d;
    }

    public static void addWindowView(WindowManager windowManager, View view, WindowManager.LayoutParams params) {
        if (view != null) {
            try {
                windowManager.addView(view, params);
            } catch (IllegalArgumentException e) {
                Log.e(GestureNavConst.TAG_GESTURE_UTILS, "addWindowView fail, catch IllegalArgumentException");
            } catch (Exception e2) {
                Log.e(GestureNavConst.TAG_GESTURE_UTILS, "addWindowView fail, catch Exception");
            }
        }
    }

    public static void updateViewLayout(WindowManager windowManager, View view, WindowManager.LayoutParams params) {
        if (view != null) {
            try {
                windowManager.updateViewLayout(view, params);
            } catch (IllegalArgumentException e) {
                Log.e(GestureNavConst.TAG_GESTURE_UTILS, "updateViewLayout fail, catch IllegalArgumentException");
            } catch (Exception e2) {
                Log.e(GestureNavConst.TAG_GESTURE_UTILS, "updateViewLayout fail, catch Exception");
            }
        }
    }

    public static void removeWindowView(WindowManager windowManager, View view, boolean immediate) {
        if (view != null) {
            if (immediate) {
                try {
                    windowManager.removeViewImmediate(view);
                } catch (IllegalArgumentException e) {
                    Log.e(GestureNavConst.TAG_GESTURE_UTILS, "removeWindowView fail." + e);
                } catch (Exception e2) {
                    Log.e(GestureNavConst.TAG_GESTURE_UTILS, "removeWindowView fail, catch Exception");
                }
            } else {
                windowManager.removeView(view);
            }
        }
    }

    public static boolean isInLockTaskMode() {
        try {
            return isInLockTaskMode(ActivityTaskManager.getService().getLockTaskModeState());
        } catch (RemoteException e) {
            Log.e(GestureNavConst.TAG_GESTURE_UTILS, "Check lock task mode fail.", e);
            return false;
        }
    }

    public static boolean isInLockTaskMode(int lockTaskState) {
        return lockTaskState != 0;
    }

    public static void exitLockTaskMode() {
        try {
            ActivityTaskManager.getService().stopSystemLockTaskMode();
        } catch (RemoteException e) {
            Log.e(GestureNavConst.TAG_GESTURE_UTILS, "Exit lock task mode fail.", e);
        }
    }

    public static boolean isSystemOrSignature(Context context, String packageName) {
        if (context == null || packageName == null) {
            return false;
        }
        boolean isTrust = false;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            if (!(appInfo == null || (appInfo.flags & 1) == 0)) {
                isTrust = true;
            }
            if (isTrust || pm.checkSignatures(packageName, AppStartupDataMgr.HWPUSH_PKGNAME) != 0) {
                return isTrust;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(GestureNavConst.TAG_GESTURE_UTILS, packageName + " not found.");
            return false;
        }
    }

    public static boolean isSuperPowerSaveMode() {
        return SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false);
    }

    public static boolean performHapticFeedbackIfNeed(Context context) {
        if (!IS_BACK_GESTURE_HAPTIC_FEEDBACK_EN) {
            return false;
        }
        if (mSupportEffectVb) {
            HwVibrator.setHwVibrator(Process.myUid(), context.getOpPackageName(), "haptic.virtual_navigation.click_back");
            return true;
        }
        WindowManagerPolicy policy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        if (policy == null) {
            return true;
        }
        policy.performHapticFeedback(Process.myUid(), context.getOpPackageName(), 1, false, "Gesture Nav Back");
        return true;
    }

    public static boolean isHapticFedbackEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(), "haptic_feedback_enabled", 0, -2) != 0;
    }

    public static boolean isGameAppForeground() {
        return ActivityManagerEx.isGameDndOn();
    }
}
