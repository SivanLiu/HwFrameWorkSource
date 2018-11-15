package com.android.server.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.Flog;
import android.util.HwLog;
import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import com.android.server.gesture.GestureNavConst;
import com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.huawei.hwextdevice.HWExtDeviceEvent;
import com.huawei.hwextdevice.HWExtDeviceEventListener;
import com.huawei.hwextdevice.HWExtDeviceManager;
import com.huawei.hwextdevice.devices.HWExtMotion;
import com.huawei.motiondetection.MRUtils;
import java.util.ArrayList;
import java.util.List;

public class PickUpWakeScreenManager {
    private static final String ACTION_PICKUP_FACEDETECT = "huawei.intent.action.PICKUP_FACEDETECT";
    private static final int CAN_DISABLE_MOTION = 0;
    private static final int CAN_ENBALE_MOTION = 1;
    private static final boolean CHECK_PROXIMITY_SENSOR_ABSENT = true;
    private static final boolean DEBUG;
    private static final Uri HWMOTIONS_CONTENT_URI = Uri.parse("content://com.huawei.providers.motions/hwmotions");
    public static final String MOTION_PICKUP_WAKEUP_DEVICE = "motion_pickup_wakeup_device";
    private static final int MSG_RECORD_WAKEUP = 2;
    private static final int MSG_WAIT_PROXIMITY_SENSOR_TIMEOUT = 1;
    private static final String PERMISSION_PICKUP_FACEDETECT = "com.huawei.services.permission.PICKUP_FACEDETECT";
    private static int SCREEN_OFF_TIME_WITHOUT_TOUCH = 5000;
    private static final String TAG = "PickUpWakeScreenManager";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final long WAIT_PROXIMITY_SENSOR_TIMEOUT = 500;
    private static final String WAKEUP_REASON = "WakeUpReason";
    private static Object mLock = new Object();
    private static PickUpWakeScreenManager mPickUpWakeScreenManager;
    private static final int[] mSensorsPickup = new int[]{1};
    private static final boolean sSupportFaceRecognition = SystemProperties.getBoolean("ro.config.face_recognition", false);
    private Context mContext;
    private boolean mFirstBoot = true;
    private HWExtDeviceEventListener mHWEDListener = new HWExtDeviceEventListener() {
        public void onDeviceDataChanged(HWExtDeviceEvent hwextDeviceEvent) {
            Slog.d(PickUpWakeScreenManager.TAG, "onDeviceDataChanged");
            if (!PickUpWakeScreenManager.this.mIsPickupSupport) {
                Slog.d(PickUpWakeScreenManager.TAG, "mIsPickupSwitchOn is disable");
            } else if (hwextDeviceEvent.getDeviceValues() == null) {
                Slog.d(PickUpWakeScreenManager.TAG, "onDeviceDataChanged  deviceValues is null ");
            } else if (PickUpWakeScreenManager.this.mIsPickupSwitchOn) {
                PickUpWakeScreenManager.this.wakeupScreenOnIfNeeded(PickUpWakeScreenManager.this.mContext);
            } else {
                Slog.d(PickUpWakeScreenManager.TAG, "mIsPickupSwitchOn is off");
            }
        }
    };
    private HWExtDeviceManager mHWEDManager = null;
    private HWExtMotion mHWExtMotion = null;
    private Handler mHandler;
    private boolean mInFastScreenOn = false;
    private boolean mIsPickupSupport = false;
    private boolean mIsPickupSwitchOn = false;
    private KeyguardServiceDelegate mKgDelegate;
    private int mMotionRegistState = 1;
    private boolean mNeedControlTurnOff = false;
    private ContentObserver mPickupEnabledObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Slog.d(PickUpWakeScreenManager.TAG, "PickupEnabledObserver");
            PickUpWakeScreenManager.this.observePickupSwitchState();
        }
    };
    public final Runnable mPickupMotionTimeoutRunnable = new Runnable() {
        public void run() {
            PowerManager pm = (PowerManager) PickUpWakeScreenManager.this.mContext.getSystemService("power");
            if (pm != null && pm.isScreenOn()) {
                pm.goToSleep(SystemClock.uptimeMillis());
                Slog.d(PickUpWakeScreenManager.TAG, "ScreenOff because pickup motion");
            }
        }
    };
    private boolean mPointerListerInitState = false;
    private Handler mProximityHandler;
    private Sensor mProximitySensor;
    private boolean mProximitySensorEnabled;
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (PickUpWakeScreenManager.this.mProximitySensorEnabled) {
                boolean positive = false;
                float distance = event.values[0];
                if (distance >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && distance < PickUpWakeScreenManager.this.mProximityThreshold) {
                    positive = true;
                }
                if (PickUpWakeScreenManager.DEBUG) {
                    String str = PickUpWakeScreenManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onSensorChanged distance=");
                    stringBuilder.append(distance);
                    stringBuilder.append(",positive=");
                    stringBuilder.append(positive);
                    stringBuilder.append(",sensorEnabled=");
                    stringBuilder.append(PickUpWakeScreenManager.this.mProximitySensorEnabled);
                    Slog.d(str, stringBuilder.toString());
                }
                PickUpWakeScreenManager.this.handleProximitySensorEvent(positive);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private float mProximityThreshold;
    private ContentResolver mResolver;
    private ScreenTouchEventListener mScreenTouchEventListener = null;
    private final SensorManager mSensorManager;
    private BroadcastReceiver mUserSwitchReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Slog.d(PickUpWakeScreenManager.TAG, "on receive userSwitchReceiver");
            PickUpWakeScreenManager.this.observePickupSwitchState();
        }
    };
    private WindowManagerFuncs mWindowManagerFuncs;
    private boolean trunOffTimeOutEnable = false;

    private class ProximityHandler extends Handler {
        public ProximityHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Slog.i(PickUpWakeScreenManager.TAG, "wait proximity sensor timeout(more than 500ms).");
                    PickUpWakeScreenManager.this.setProximitySensorEnabled(false);
                    PickUpWakeScreenManager.this.stopWakeUpReady(PickUpWakeScreenManager.this.mContext, true);
                    return;
                case 2:
                    String str = PickUpWakeScreenManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Face Dectect wakeUpInternal type:");
                    stringBuilder.append(msg.obj);
                    Slog.d(str, stringBuilder.toString());
                    Global.putString(PickUpWakeScreenManager.this.mContext.getContentResolver(), PickUpWakeScreenManager.WAKEUP_REASON, msg.obj == null ? "unknow" : msg.obj.toString());
                    return;
                default:
                    return;
            }
        }
    }

    private class ScreenTouchEventListener implements PointerEventListener {
        private ScreenTouchEventListener() {
        }

        /* synthetic */ ScreenTouchEventListener(PickUpWakeScreenManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (PickUpWakeScreenManager.this.mPickupMotionTimeoutRunnable != null && PickUpWakeScreenManager.this.trunOffTimeOutEnable) {
                PickUpWakeScreenManager.this.mHandler.removeCallbacks(PickUpWakeScreenManager.this.mPickupMotionTimeoutRunnable);
                PickUpWakeScreenManager.this.trunOffTimeOutEnable = false;
                Slog.d(PickUpWakeScreenManager.TAG, "ScreenTouchEventListener remove");
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public static PickUpWakeScreenManager getInstance(Context context, Handler handler, WindowManagerFuncs windowManagerFuncs, KeyguardServiceDelegate KeyguardDelegate) {
        PickUpWakeScreenManager pickUpWakeScreenManager;
        synchronized (mLock) {
            if (mPickUpWakeScreenManager == null) {
                mPickUpWakeScreenManager = new PickUpWakeScreenManager(context, handler, windowManagerFuncs, KeyguardDelegate);
            }
            pickUpWakeScreenManager = mPickUpWakeScreenManager;
        }
        return pickUpWakeScreenManager;
    }

    public static PickUpWakeScreenManager getInstance() {
        PickUpWakeScreenManager pickUpWakeScreenManager;
        synchronized (mLock) {
            pickUpWakeScreenManager = mPickUpWakeScreenManager;
        }
        return pickUpWakeScreenManager;
    }

    public void stopTrunOffScrren() {
        if (this.mPickupMotionTimeoutRunnable != null && this.trunOffTimeOutEnable) {
            this.mHandler.removeCallbacks(this.mPickupMotionTimeoutRunnable);
            this.trunOffTimeOutEnable = false;
            Slog.d(TAG, "stopTrunOffScrren remove mPickupMotionTimeout");
        }
    }

    public PickUpWakeScreenManager(Context context, Handler handler, WindowManagerFuncs windowManagerFuncs, KeyguardServiceDelegate KeyguardDelegate) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mResolver = this.mContext.getContentResolver();
        this.mProximityHandler = new ProximityHandler(handler.getLooper());
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mProximitySensor = this.mSensorManager.getDefaultSensor(8);
        if (this.mProximitySensor != null) {
            float maximumRange = this.mProximitySensor.getMaximumRange();
            float f = TYPICAL_PROXIMITY_THRESHOLD;
            if (maximumRange < TYPICAL_PROXIMITY_THRESHOLD) {
                f = this.mProximitySensor.getMaximumRange();
            }
            this.mProximityThreshold = f;
        }
        this.mContext.registerReceiverAsUser(this.mUserSwitchReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_SWITCHED"), null, null);
        this.mKgDelegate = KeyguardDelegate;
    }

    public void pickUpWakeScreenInit() {
        this.mIsPickupSupport = getPickupWakeEnabled();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mIsPickupSupport = ");
        stringBuilder.append(this.mIsPickupSupport);
        Slog.d(str, stringBuilder.toString());
        this.mNeedControlTurnOff = getSupportFaceDectect() ^ 1;
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNeedControlTurnOff = ");
        stringBuilder.append(this.mNeedControlTurnOff);
        Slog.d(str, stringBuilder.toString());
        MRUtils.observerMotionEnableStateChange(this.mContext, this.mPickupEnabledObserver, -1);
        if (this.mIsPickupSupport) {
            this.mHWEDManager = HWExtDeviceManager.getInstance(this.mContext);
            this.mHWExtMotion = new HWExtMotion(100);
            this.mScreenTouchEventListener = new ScreenTouchEventListener(this, null);
        }
    }

    public static boolean isPickupSensorSupport(Context context) {
        boolean isSensorSupport = true;
        SensorManager mSensorManager = (SensorManager) context.getSystemService("sensor");
        ArrayList<Integer> mSensorTypeList = new ArrayList();
        List<Sensor> list = mSensorManager.getSensorList(-1);
        int i = 0;
        for (int i2 = 0; i2 < list.size(); i2++) {
            mSensorTypeList.add(Integer.valueOf(((Sensor) list.get(i2)).getType()));
        }
        while (i < mSensorsPickup.length) {
            if (!mSensorTypeList.contains(Integer.valueOf(mSensorsPickup[i]))) {
                isSensorSupport = false;
                break;
            }
            i++;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isPickupSensorSupport isSensorSupport = ");
        stringBuilder.append(isSensorSupport);
        Slog.d(str, stringBuilder.toString());
        return isSensorSupport;
    }

    public void enablePickupMotionOrNot(boolean isLocked) {
        if (this.mIsPickupSupport) {
            if (this.mFirstBoot) {
                int motionPickUp = MRUtils.getMotionEnableStateAsUser(this.mContext, MOTION_PICKUP_WAKEUP_DEVICE, ActivityManager.getCurrentUser());
                if (motionPickUp != -1) {
                    boolean z = true;
                    if (motionPickUp != 1) {
                        z = false;
                    }
                    this.mIsPickupSwitchOn = z;
                    this.mFirstBoot = false;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mIsPickupSwitchOn = ");
            stringBuilder.append(this.mIsPickupSwitchOn);
            Slog.d(str, stringBuilder.toString());
            if (Secure.getIntForUser(this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) == 0) {
                Slog.d(TAG, "Device is in Provision");
                return;
            } else if (this.mIsPickupSwitchOn) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isLocked = ");
                stringBuilder2.append(isLocked);
                Slog.d(str, stringBuilder2.toString());
                if (isLocked) {
                    enablePickupMotion(this.mHandler);
                } else {
                    disablePickupMotion();
                }
                return;
            } else {
                return;
            }
        }
        Slog.d(TAG, "mIsPickupSwitchOn is disable");
    }

    private void enablePickupMotion(Handler handler) {
        if (this.mMotionRegistState == 0) {
            Slog.d(TAG, "can not regist twice");
        } else if (this.mHWEDManager != null) {
            this.mHWEDManager.registerDeviceListener(this.mHWEDListener, this.mHWExtMotion, handler);
            Slog.d(TAG, "regist listener");
            if (this.mScreenTouchEventListener != null) {
                if (this.mPointerListerInitState) {
                    this.mWindowManagerFuncs.unregisterPointerEventListener(this.mScreenTouchEventListener);
                } else {
                    this.mPointerListerInitState = true;
                }
                this.mScreenTouchEventListener = null;
                Slog.d(TAG, "unregisterPointerEventListener");
                this.mMotionRegistState = 0;
            }
        } else {
            Slog.d(TAG, "mHWEDManager is null, return");
        }
    }

    private void disablePickupMotion() {
        if (this.mMotionRegistState == 1) {
            Slog.d(TAG, "can not unregist without regist");
        } else if (this.mHWEDManager != null) {
            if (this.mScreenTouchEventListener == null) {
                this.mScreenTouchEventListener = new ScreenTouchEventListener(this, null);
            }
            this.mWindowManagerFuncs.registerPointerEventListener(this.mScreenTouchEventListener);
            Slog.d(TAG, "registerPointerEventListener");
            this.mMotionRegistState = 1;
        } else {
            Slog.d(TAG, " mHWEDManager is null,return ");
        }
    }

    private void triggerFaceDetect(String reason) {
        if (this.mKgDelegate != null) {
            this.mKgDelegate.doFaceRecognize(true, reason);
        }
    }

    private void wakeupScreenOnIfNeeded(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService("power");
        if (pm == null) {
            Slog.d(TAG, "pm is null");
        } else if (pm.isScreenOn()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("screen on =");
            stringBuilder.append(pm.isScreenOn());
            Slog.d(str, stringBuilder.toString());
            triggerFaceDetect("PICKUP:SC_ON");
        } else {
            triggerFaceDetect("PICKUP:SC_OFF");
            wakeUpWaitForProximity();
            if (this.mNeedControlTurnOff) {
                this.mHandler.removeCallbacks(this.mPickupMotionTimeoutRunnable);
                this.mHandler.postDelayed(this.mPickupMotionTimeoutRunnable, (long) SCREEN_OFF_TIME_WITHOUT_TOUCH);
                this.trunOffTimeOutEnable = true;
            }
        }
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mProximitySensorEnabled) {
                this.mProximitySensorEnabled = true;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mProximityHandler);
                this.mProximityHandler.sendEmptyMessageDelayed(1, 500);
                if (DEBUG) {
                    Slog.d(TAG, "proximity sensor registered.");
                }
            }
        } else if (this.mProximitySensorEnabled) {
            this.mProximitySensorEnabled = false;
            this.mProximityHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            if (DEBUG) {
                Slog.d(TAG, "proximity sensor unregistered.");
            }
        }
    }

    private void handleProximitySensorEvent(boolean positive) {
        if (this.mProximitySensorEnabled) {
            if (positive) {
                stopWakeUpReady(this.mContext, false);
            } else {
                stopWakeUpReady(this.mContext, true);
            }
            setProximitySensorEnabled(false);
        }
    }

    private void wakeUpWaitForProximity() {
        if (this.mInFastScreenOn) {
            Slog.d(TAG, "wakeUpWaitForProximity already in processing");
            return;
        }
        startWakeUpReady(this.mContext);
        setProximitySensorEnabled(true);
    }

    private void startWakeUpReady(Context context) {
        String str;
        StringBuilder stringBuilder;
        PowerManager power = null;
        if (context != null) {
            power = (PowerManager) context.getSystemService("power");
        }
        if (power == null) {
            Slog.w(TAG, "startWakeUpReady powermanager is null");
            return;
        }
        Slog.w(TAG, "wakeUpWaitForProximity start");
        try {
            power.getClass().getMethod("startWakeUpReady", new Class[]{Long.TYPE}).invoke(power, new Object[]{Long.valueOf(SystemClock.uptimeMillis())});
            this.mInFastScreenOn = true;
        } catch (NoSuchMethodException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PowerManager Value: System hasn't startWakeUpReady method ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        } catch (IllegalArgumentException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PowerManager Value: startWakeUpReady method has wrong parameter ");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
        } catch (Exception e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PowerManager Value: other reflect exception ");
            stringBuilder.append(e3);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void stopWakeUpReady(Context context, boolean turnOn) {
        String str;
        StringBuilder stringBuilder;
        if (this.mInFastScreenOn) {
            PowerManager power = null;
            if (context != null) {
                power = (PowerManager) context.getSystemService("power");
            }
            if (power == null) {
                Slog.w(TAG, "stopWakeUpReady powermanager is null");
                this.mInFastScreenOn = false;
                return;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("stopWakeUpReady turnon=");
            stringBuilder2.append(turnOn);
            Slog.w(str2, stringBuilder2.toString());
            try {
                power.getClass().getMethod("stopWakeUpReady", new Class[]{Long.TYPE, Boolean.TYPE}).invoke(power, new Object[]{Long.valueOf(SystemClock.uptimeMillis()), Boolean.valueOf(turnOn)});
            } catch (NoSuchMethodException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PowerManager Value: System hasn't stopWakeUpReady method ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PowerManager Value: stopWakeUpReady method has wrong parameter ");
                stringBuilder.append(e2);
                Slog.e(str, stringBuilder.toString());
            } catch (Exception e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PowerManager Value: other reflect exception ");
                stringBuilder.append(e3);
                Slog.e(str, stringBuilder.toString());
            }
            this.mInFastScreenOn = false;
            if (turnOn) {
                if (sSupportFaceRecognition) {
                    Handler handler = this.mProximityHandler;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("android.policy:HWPICKUP#");
                    stringBuilder3.append(Binder.getCallingUid());
                    handler.obtainMessage(2, 0, 0, stringBuilder3.toString()).sendToTarget();
                }
                boolean ret = Flog.bdReport(this.mContext, 500);
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("report no proximity ScreenOn waked by pickup motion. EventId:500 ret:");
                stringBuilder2.append(ret);
                Slog.d(str2, stringBuilder2.toString());
                HwLog.dubaie("DUBAI_TAG_PICKUP_WAKE", "type=1");
            } else {
                HwLog.dubaie("DUBAI_TAG_PICKUP_WAKE", "type=0");
            }
            return;
        }
        Slog.w(TAG, "stopWakeUpReady, not in fast screen on");
    }

    private boolean getPickupWakeEnabled() {
        return SystemProperties.getBoolean("ro.config.hw_wakeup_device", false);
    }

    private boolean getSupportFaceDectect() {
        return 1 == SystemProperties.getInt("ro.config.face_detect", 0) || SystemProperties.getBoolean("ro.config.face_recognition", false) || SystemProperties.getBoolean("ro.config.face_smart_keepon", false);
    }

    private boolean getPickupSwitchOn() {
        return MRUtils.getMotionEnableStateAsUser(this.mContext, MOTION_PICKUP_WAKEUP_DEVICE, ActivityManager.getCurrentUser()) == 1;
    }

    private void observePickupSwitchState() {
        this.mIsPickupSwitchOn = getPickupSwitchOn();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("observePickupSwitchState mIsPickupSwitchOn:");
        stringBuilder.append(this.mIsPickupSwitchOn);
        Slog.d(str, stringBuilder.toString());
        if (this.mIsPickupSupport && !this.mIsPickupSwitchOn && this.mHWEDManager != null) {
            this.mHWEDManager.unregisterDeviceListener(this.mHWEDListener, this.mHWExtMotion);
            Slog.d(TAG, "unregist listener");
        }
    }
}
