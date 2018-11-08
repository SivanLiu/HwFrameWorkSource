package com.android.server.gesture;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.graphics.PointF;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.android.server.LocalServices;
import com.android.server.emcom.daemon.CommandsInterface;
import java.util.ArrayList;
import java.util.List;

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

    public static void systemReady() {
        if (!mHasInit) {
            mHasNotch = parseHole();
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_UTILS, "systemReady hasNotch=" + mHasNotch);
            }
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

    public static int getInputDeviceId(int inputSource) {
        for (int devId : InputDevice.getDeviceIds()) {
            if (InputDevice.getDevice(devId).supportsSource(inputSource)) {
                return devId;
            }
        }
        return 0;
    }

    public static void sendKeyEvent(int keycode) {
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_UTILS, "sendKeyEvent keycode=" + keycode);
        }
        long now = SystemClock.uptimeMillis();
        int[] actions = new int[]{0, 1};
        for (int keyEvent : actions) {
            InputManager.getInstance().injectInputEvent(new KeyEvent(now, now, keyEvent, keycode, 0, 0, -1, 0, 72, CommandsInterface.EMCOM_SD_XENGINE_START_ACC), 0);
        }
    }

    public static void sendTap(float x, float y, int deviceId, int source) {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_UTILS, "sendTap (" + x + ", " + y + ")");
        }
        long downTime = SystemClock.uptimeMillis();
        injectMotionEvent(0, downTime, downTime, x, y, 1.0f, deviceId, source);
        injectMotionEvent(1, downTime, SystemClock.uptimeMillis(), x, y, 0.0f, deviceId, source);
    }

    public static void sendSwipe(float x1, float y1, float x2, float y2, int duration, int deviceId, int source, ArrayList<PointF> pendingMovePoints, boolean hasMultiTouched) {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_UTILS, "sendSwipe (" + x1 + ", " + y1 + ") to (" + x2 + ", " + y2 + "), duration:" + duration);
        }
        if (duration < 80) {
            duration = 80;
        } else if (duration > 500) {
            duration = 500;
        }
        long now = SystemClock.uptimeMillis();
        long downTime = now;
        injectMotionEvent(0, now, now, x1, y1, 1.0f, deviceId, source);
        if (!(hasMultiTouched || pendingMovePoints == null)) {
            int size = pendingMovePoints.size();
            long size2 = (long) size;
            if (size > 0) {
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_UTILS, "inject " + size2 + " pending move points");
                }
                for (int i = 0; ((long) i) < size2; i++) {
                    injectMotionEvent(2, downTime, now, ((PointF) pendingMovePoints.get(i)).x, ((PointF) pendingMovePoints.get(i)).y, 1.0f, deviceId, source);
                    SystemClock.sleep(5);
                    now = SystemClock.uptimeMillis();
                }
                injectMotionEvent(1, downTime, now, x2, y2, 0.0f, deviceId, source);
            }
        }
        long endTime = now + ((long) duration);
        while (now < endTime) {
            float alpha = ((float) (now - downTime)) / ((float) duration);
            injectMotionEvent(2, downTime, now, lerp(x1, x2, alpha), lerp(y1, y2, alpha), 1.0f, deviceId, source);
            SystemClock.sleep(5);
            now = SystemClock.uptimeMillis();
        }
        injectMotionEvent(1, downTime, now, x2, y2, 0.0f, deviceId, source);
    }

    public static void injectMotionEvent(int action, long downTime, long eventTime, float x, float y, int deviceId, int source) {
        injectMotionEvent(action, downTime, eventTime, x, y, action == 1 ? 0.0f : 1.0f, deviceId, source);
    }

    public static void injectMotionEvent(int action, long downTime, long eventTime, float x, float y, float pressure, int deviceId, int source) {
        InputEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, pressure, 1.0f, 0, 1.0f, 1.0f, deviceId, 0);
        event.setSource(source);
        ((InputManagerInternal) LocalServices.getService(InputManagerInternal.class)).injectInputEvent(event, 0, 0, 524288);
    }

    public static void injectDownWithBatchMoveEvent(long downTime, float downX, float downY, ArrayList<PointF> batchMovePoints, long durationTime, int deviceId, int source) {
        MotionEvent event = MotionEvent.obtain(downTime, downTime, 0, downX, downY, 1.0f, 1.0f, null, 1.0f, 1.0f, deviceId, 0);
        event.setSource(source);
        int appendPolicyFlag = 524288;
        if (batchMovePoints != null) {
            int size = batchMovePoints.size();
            if (size > 0) {
                appendPolicyFlag = 786432;
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_UTILS, "inject down with " + size + " batch move points");
                }
                for (int i = 0; i < size; i++) {
                    event.addBatch(downTime + ((long) (((((float) (i + 1)) * 1.0f) / ((float) size)) * ((float) durationTime))), ((PointF) batchMovePoints.get(i)).x, ((PointF) batchMovePoints.get(i)).y, 1.0f, 1.0f, 0);
                }
            }
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
                Log.e(GestureNavConst.TAG_GESTURE_UTILS, "addWindowView fail." + e);
            }
        }
    }

    public static void updateViewLayout(WindowManager mWindowManager, View view, LayoutParams params) {
        if (view != null) {
            try {
                mWindowManager.updateViewLayout(view, params);
            } catch (Exception e) {
                Log.e(GestureNavConst.TAG_GESTURE_UTILS, "updateViewLayout fail." + e);
            }
        }
    }

    public static void removeWindowView(WindowManager mWindowManager, View view, boolean immediate) {
        if (view != null) {
            if (immediate) {
                try {
                    mWindowManager.removeViewImmediate(view);
                } catch (IllegalArgumentException e) {
                    Log.e(GestureNavConst.TAG_GESTURE_UTILS, "removeWindowView fail." + e);
                } catch (Exception e2) {
                    Log.e(GestureNavConst.TAG_GESTURE_UTILS, "removeWindowView fail." + e2);
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
        if (tasks == null || (tasks.isEmpty() ^ 1) == 0) {
            return null;
        }
        return (RunningTaskInfo) tasks.get(0);
    }

    public static boolean isSuperPowerSaveMode() {
        return SystemProperties.getBoolean("sys.super_power_save", false);
    }
}
