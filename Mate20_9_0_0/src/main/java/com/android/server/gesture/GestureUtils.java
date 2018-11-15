package com.android.server.gesture;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.graphics.PointF;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.android.server.LocalServices;
import com.huawei.android.os.HwVibrator;
import java.util.ArrayList;
import java.util.List;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsDetailModeID;

public class GestureUtils {
    public static final int DEFAULT_DEVICE_ID = 0;
    private static final int DEFAULT_EDGE_FLAGS = 0;
    private static final int DEFAULT_META_STATE = 0;
    private static final float DEFAULT_PRECISION_X = 1.0f;
    private static final float DEFAULT_PRECISION_Y = 1.0f;
    private static final float DEFAULT_PRESSURE_DOWN = 1.0f;
    private static final float DEFAULT_PRESSURE_UP = 0.0f;
    private static final float DEFAULT_SIZE = 1.0f;
    private static final int MOTION_EVENT_INJECTION_DELAY_MILLIS = 5;
    private static boolean mHasInit = false;
    private static boolean mHasNotch = false;
    private static boolean mSupportEffectVb = false;

    public static void systemReady() {
        if (!mHasInit) {
            mHasNotch = parseHole();
            mSupportEffectVb = HwVibrator.isSupportHwVibrator("haptic.virtual_navigation.click_back");
            if (GestureNavConst.DEBUG) {
                String str = GestureNavConst.TAG_GESTURE_UTILS;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("systemReady hasNotch=");
                stringBuilder.append(mHasNotch);
                stringBuilder.append(", effectVb=");
                stringBuilder.append(mSupportEffectVb);
                Log.d(str, stringBuilder.toString());
            }
            mHasInit = true;
        }
    }

    public static boolean parseHole() {
        String[] props = SystemProperties.get("ro.config.hw_notch_size", "").split(",");
        if (props == null || props.length != 4) {
            return false;
        }
        String str = GestureNavConst.TAG_GESTURE_UTILS;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("prop hole height:");
        stringBuilder.append(Integer.parseInt(props[1]));
        Log.d(str, stringBuilder.toString());
        return true;
    }

    public static boolean hasNotch() {
        return mHasNotch;
    }

    public static boolean isSupportEffectVibrator() {
        return mSupportEffectVb;
    }

    public static int getInputDeviceId(int inputSource) {
        for (int devId : InputDevice.getDeviceIds()) {
            if (InputDevice.getDevice(devId).supportsSource(inputSource)) {
                return devId;
            }
        }
        return 0;
    }

    public static void sendKeyEvent(int keycode) {
        int i;
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_UTILS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendKeyEvent keycode=");
            i = keycode;
            stringBuilder.append(i);
            Log.i(str, stringBuilder.toString());
        } else {
            i = keycode;
        }
        long now = SystemClock.uptimeMillis();
        int[] actions = new int[]{0, 1};
        int effectFlag = mSupportEffectVb ? 0 : 64;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < actions.length) {
                int i4 = i3;
                int[] actions2 = actions;
                InputManager.getInstance().injectInputEvent(new KeyEvent(now, now, actions[i3], i, 0, 0, -1, 0, 8 | effectFlag, 257), 0);
                i2 = i4 + 1;
                int i5 = 0;
                actions = actions2;
            } else {
                return;
            }
        }
    }

    public static void sendTap(float x, float y, int deviceId, int source) {
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_UTILS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendTap (");
            stringBuilder.append(x);
            stringBuilder.append(", ");
            stringBuilder.append(y);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        long downTime = SystemClock.uptimeMillis();
        long j = downTime;
        float f = x;
        float f2 = y;
        int i = deviceId;
        int i2 = source;
        injectMotionEvent(0, j, downTime, f, f2, 1.0f, i, i2);
        injectMotionEvent(1, j, SystemClock.uptimeMillis(), f, f2, 0.0f, i, i2);
    }

    public static void sendSwipe(float x1, float y1, float x2, float y2, int duration, int deviceId, int source, ArrayList<PointF> pendingMovePoints, boolean hasMultiTouched) {
        long size;
        long size2;
        float f = x1;
        float f2 = y1;
        float f3 = x2;
        float f4 = y2;
        int i = duration;
        ArrayList<PointF> arrayList = pendingMovePoints;
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_UTILS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendSwipe (");
            stringBuilder.append(f);
            stringBuilder.append(", ");
            stringBuilder.append(f2);
            stringBuilder.append(") to (");
            stringBuilder.append(f3);
            stringBuilder.append(", ");
            stringBuilder.append(f4);
            stringBuilder.append("), duration:");
            stringBuilder.append(i);
            Log.d(str, stringBuilder.toString());
        }
        if (i < 80) {
            i = 80;
        } else if (i > 500) {
            i = 500;
        }
        int duration2 = i;
        long now = SystemClock.uptimeMillis();
        long downTime = now;
        injectMotionEvent(0, downTime, now, f, f2, 1.0f, deviceId, source);
        long size3;
        if (hasMultiTouched || arrayList == null) {
            size3 = 0;
        } else {
            long size4 = (long) pendingMovePoints.size();
            size = size4;
            if (size4 > 0) {
                if (GestureNavConst.DEBUG != null) {
                    size2 = GestureNavConst.TAG_GESTURE_UTILS;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("inject ");
                    stringBuilder2.append(size);
                    stringBuilder2.append(" pending move points");
                    Log.d(size2, stringBuilder2.toString());
                }
                size2 = null;
                while (true) {
                    int i2 = size2;
                    if (((long) i2) >= size) {
                        break;
                    }
                    float f5 = ((PointF) arrayList.get(i2)).x;
                    float f6 = f5;
                    int i3 = i2;
                    size3 = size;
                    injectMotionEvent(2, downTime, now, f6, ((PointF) arrayList.get(i2)).y, 1.0f, deviceId, source);
                    SystemClock.sleep(5);
                    now = SystemClock.uptimeMillis();
                    size2 = i3 + 1;
                    size = size3;
                }
                injectMotionEvent(1, downTime, now, f3, f4, 0.0f, deviceId, source);
            }
        }
        size2 = downTime + ((long) duration2);
        while (true) {
            long endTime = size2;
            if (now >= endTime) {
                break;
            }
            size = now - downTime;
            float alpha = ((float) size) / ((float) duration2);
            injectMotionEvent(2, downTime, now, lerp(f, f3, alpha), lerp(f2, f4, alpha), 1.0f, deviceId, source);
            SystemClock.sleep(5);
            now = SystemClock.uptimeMillis();
            long j = 5;
            size2 = endTime;
        }
        injectMotionEvent(1, downTime, now, f3, f4, 0.0f, deviceId, source);
    }

    public static void injectMotionEvent(int action, long downTime, long eventTime, float x, float y, int deviceId, int source) {
        int i = action;
        injectMotionEvent(i, downTime, eventTime, x, y, i == 1 ? 0.0f : 1.0f, deviceId, source);
    }

    public static void injectMotionEvent(int action, long downTime, long eventTime, float x, float y, float pressure, int deviceId, int source) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, pressure, 1.0f, 0, 1.0f, 1.0f, deviceId, 0);
        event.setSource(source);
        ((InputManagerInternal) LocalServices.getService(InputManagerInternal.class)).injectInputEvent(event, 0, 0, 524288);
    }

    public static void injectDownWithBatchMoveEvent(long downTime, float downX, float downY, ArrayList<PointF> batchMovePoints, long durationTime, int deviceId, int source) {
        ArrayList<PointF> arrayList = batchMovePoints;
        MotionEvent event = MotionEvent.obtain(downTime, downTime, 0, downX, downY, 1.0f, 1.0f, 0, 1.0f, 1.0f, deviceId, 0);
        event.setSource(source);
        int appendPolicyFlag = 524288;
        int size;
        if (arrayList != null) {
            int size2 = batchMovePoints.size();
            size = size2;
            if (size2 > 0) {
                int appendPolicyFlag2 = 524288 | HighBitsDetailModeID.MODE_FOLIAGE;
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavConst.TAG_GESTURE_UTILS;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("inject down with ");
                    stringBuilder.append(size);
                    stringBuilder.append(" batch move points");
                    Log.d(str, stringBuilder.toString());
                }
                appendPolicyFlag = 0;
                while (true) {
                    int i = appendPolicyFlag;
                    if (i >= size) {
                        break;
                    }
                    event.addBatch(downTime + ((long) (((1.0f * ((float) (i + 1))) / ((float) size)) * ((float) durationTime))), ((PointF) arrayList.get(i)).x, ((PointF) arrayList.get(i)).y, 1.0f, 1.0f, 0);
                    appendPolicyFlag = i + 1;
                }
                appendPolicyFlag = appendPolicyFlag2;
            }
        } else {
            size = 0;
        }
        ((InputManagerInternal) LocalServices.getService(InputManagerInternal.class)).injectInputEvent(event, 0, 0, appendPolicyFlag);
    }

    private static final float lerp(float a, float b, float alpha) {
        return ((b - a) * alpha) + a;
    }

    public static double angle(float distanceX, float distanceY, boolean divY) {
        if ((divY ? distanceY : distanceX) == 0.0f) {
            return 90.0d;
        }
        return (Math.atan((double) (divY ? distanceX / distanceY : distanceY / distanceX)) / 3.141592653589793d) * 180.0d;
    }

    public static void addWindowView(WindowManager mWindowManager, View view, LayoutParams params) {
        if (view != null) {
            try {
                mWindowManager.addView(view, params);
            } catch (Exception e) {
                String str = GestureNavConst.TAG_GESTURE_UTILS;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addWindowView fail.");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    public static void updateViewLayout(WindowManager mWindowManager, View view, LayoutParams params) {
        if (view != null) {
            try {
                mWindowManager.updateViewLayout(view, params);
            } catch (Exception e) {
                String str = GestureNavConst.TAG_GESTURE_UTILS;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateViewLayout fail.");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    public static void removeWindowView(WindowManager mWindowManager, View view, boolean immediate) {
        String str;
        StringBuilder stringBuilder;
        if (view != null) {
            if (immediate) {
                try {
                    mWindowManager.removeViewImmediate(view);
                } catch (IllegalArgumentException e) {
                    str = GestureNavConst.TAG_GESTURE_UTILS;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("removeWindowView fail.");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                } catch (IllegalArgumentException e2) {
                    str = GestureNavConst.TAG_GESTURE_UTILS;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("removeWindowView fail.");
                    stringBuilder.append(e2);
                    Log.e(str, stringBuilder.toString());
                }
            } else {
                mWindowManager.removeView(view);
            }
        }
    }

    public static boolean isInLockTaskMode() {
        boolean z = false;
        try {
            if (ActivityManager.getService().getLockTaskModeState() != 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            Log.d(GestureNavConst.TAG_GESTURE_UTILS, "Check lock task mode fail.", e);
            return false;
        }
    }

    public static void exitLockTaskMode() {
        try {
            ActivityManager.getService().stopSystemLockTaskMode();
        } catch (RemoteException e) {
            Log.d(GestureNavConst.TAG_GESTURE_UTILS, "Exit lock task mode fail.", e);
        }
    }

    public static RunningTaskInfo getRunningTask(Context context) {
        List<RunningTaskInfo> tasks = ((ActivityManager) context.getSystemService("activity")).getRunningTasks(1);
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        return (RunningTaskInfo) tasks.get(0);
    }

    public static boolean isSuperPowerSaveMode() {
        return SystemProperties.getBoolean("sys.super_power_save", false);
    }

    public static boolean playHwEffectForBackIfNeed(Context context) {
        if (!mSupportEffectVb || !isHapticFedbackEnabled(context)) {
            return false;
        }
        HwVibrator.setHwVibrator(Process.myUid(), "android", "haptic.virtual_navigation.click_back");
        return true;
    }

    public static boolean isHapticFedbackEnabled(Context context) {
        return System.getIntForUser(context.getContentResolver(), "haptic_feedback_enabled", 0, -2) != 0;
    }
}
