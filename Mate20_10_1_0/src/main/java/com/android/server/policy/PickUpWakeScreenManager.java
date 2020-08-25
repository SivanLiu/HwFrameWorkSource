package com.android.server.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.cover.CoverManager;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Flog;
import android.util.HwLog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.android.os.HwPowerManager;
import com.huawei.hwextdevice.HWExtDeviceEvent;
import com.huawei.hwextdevice.HWExtDeviceEventListener;
import com.huawei.hwextdevice.HWExtDeviceManager;
import com.huawei.hwextdevice.devices.HWExtMotion;
import com.huawei.motiondetection.MRUtils;
import com.huawei.server.HwPCFactory;
import com.huawei.server.hwmultidisplay.DefaultHwMultiDisplayUtils;
import huawei.android.security.facerecognition.FaceReportEventToIaware;
import java.util.ArrayList;
import java.util.List;

public class PickUpWakeScreenManager {
    private static final int DEFAULT_PICK_UP_MOTION = -1;
    /* access modifiers changed from: private */
    public static final boolean IS_SIMPLE_COVER = SystemProperties.getBoolean("ro.config.hw_simplecover", false);
    private static final boolean IS_SUPPORT_FACE_RECOGNITION = SystemProperties.getBoolean("ro.config.face_recognition", false);
    private static final boolean IS_SUPPORT_PICKUP_PROP = SystemProperties.getBoolean("ro.config.hw_wakeup_device", false);
    /* access modifiers changed from: private */
    public static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", MemoryConstant.MEM_SCENE_DEFAULT));
    private static final String MOTION_PICKUP_WAKEUP_DEVICE = "motion_pickup_wakeup_device";
    private static final int MSG_RECORD_WAKEUP = 2;
    private static final int MSG_SET_TURNOFF_CONTROLLER = 4;
    private static final int MSG_UPDATE_DEVICE_LISTENER = 3;
    private static final int MSG_WAIT_PROXIMITY_SENSOR_TIMEOUT = 1;
    private static final int[] PICKUP_SENSORS = {1};
    private static final int SCREEN_OFF_TIME_WITHOUT_TOUCH = 5000;
    private static final String TAG = "PickUpWakeScreenManager";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final long WAIT_PROXIMITY_SENSOR_TIMEOUT = 500;
    private static final String WAKEUP_REASON = "WakeUpReason";
    private static DefaultHwMultiDisplayUtils sHwMultiDisplayUtils = HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwMultiDisplayUtils();
    private static boolean sIsNeedControlTurnOff = false;
    private static boolean sIsSupportPickUpSensor = true;
    private static PickUpWakeScreenManager sPickUpWakeScreenManager;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public CoverManager mCoverManager;
    private HWExtMotion mHwExtMotion = null;
    private HWExtDeviceEventListener mHwedListener = null;
    private HWExtDeviceManager mHwedManager = null;
    private boolean mIsFirstBoot;
    private boolean mIsInFastScreenOn;
    private boolean mIsInitCompleted;
    /* access modifiers changed from: private */
    public boolean mIsPickupSwitchOn;
    /* access modifiers changed from: private */
    public boolean mIsProximitySensorEnabled;
    /* access modifiers changed from: private */
    public boolean mIsTurnOffTimeOutEnabled;
    private KeyguardServiceDelegate mKgDelegate;
    private Handler mPickUpHandler;
    private ContentObserver mPickupEnabledObserver;
    private final Runnable mPickupMotionTimeoutRunnable = new Runnable() {
        /* class com.android.server.policy.PickUpWakeScreenManager.AnonymousClass1 */

        public void run() {
            PowerManager pm = (PowerManager) PickUpWakeScreenManager.this.mContext.getSystemService("power");
            if (pm != null && pm.isInteractive()) {
                Slog.i(PickUpWakeScreenManager.TAG, "ScreenOff because no user activty after pickup motion");
                pm.goToSleep(SystemClock.uptimeMillis());
            }
            PickUpWakeScreenManager.this.setTurnOffController(false);
        }
    };
    /* access modifiers changed from: private */
    public Sensor mProximitySensor;
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
        /* class com.android.server.policy.PickUpWakeScreenManager.AnonymousClass2 */

        public void onSensorChanged(SensorEvent event) {
            if (PickUpWakeScreenManager.this.mIsProximitySensorEnabled) {
                boolean isPositive = false;
                float distance = event.values[0];
                if (distance >= 0.0f && distance < PickUpWakeScreenManager.this.mProximityThreshold) {
                    isPositive = true;
                }
                Slog.i(PickUpWakeScreenManager.TAG, "onSensorChanged distance=" + distance + ", isPositive = " + isPositive + ",sensorEnabled=" + PickUpWakeScreenManager.this.mIsProximitySensorEnabled);
                PickUpWakeScreenManager.this.handleProximitySensorEvent(isPositive);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    /* access modifiers changed from: private */
    public float mProximityThreshold;
    private ContentResolver mResolver;
    private ScreenTouchEventListener mScreenTouchEventListener = null;
    private SensorManager mSensorManager;
    private BroadcastReceiver mUserSwitchReceiver;
    private WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;

    private PickUpWakeScreenManager() {
        boolean z = false;
        this.mIsTurnOffTimeOutEnabled = false;
        this.mIsPickupSwitchOn = false;
        this.mIsInFastScreenOn = false;
        this.mIsFirstBoot = true;
        this.mIsInitCompleted = false;
        this.mPickupEnabledObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.policy.PickUpWakeScreenManager.AnonymousClass3 */

            public void onChange(boolean selfChange) {
                Slog.i(PickUpWakeScreenManager.TAG, "PickupEnabledObserver");
                PickUpWakeScreenManager.this.updatePickupSwitchState();
            }
        };
        this.mUserSwitchReceiver = new BroadcastReceiver() {
            /* class com.android.server.policy.PickUpWakeScreenManager.AnonymousClass4 */

            public void onReceive(Context context, Intent intent) {
                Slog.i(PickUpWakeScreenManager.TAG, "on receive userSwitchReceiver");
                PickUpWakeScreenManager.this.updatePickupSwitchState();
            }
        };
        if (IS_SUPPORT_PICKUP_PROP && !isSupportFaceDectect()) {
            z = true;
        }
        sIsNeedControlTurnOff = z;
        Slog.i(TAG, "IS_SUPPORT_PICKUP:" + IS_SUPPORT_PICKUP_PROP + ", mIsNeedControlTurnOff:" + sIsNeedControlTurnOff);
    }

    public static synchronized PickUpWakeScreenManager getInstance() {
        PickUpWakeScreenManager pickUpWakeScreenManager;
        synchronized (PickUpWakeScreenManager.class) {
            if (sPickUpWakeScreenManager == null) {
                sPickUpWakeScreenManager = new PickUpWakeScreenManager();
            }
            pickUpWakeScreenManager = sPickUpWakeScreenManager;
        }
        return pickUpWakeScreenManager;
    }

    public void initIfNeed(Context context, Handler handler, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs, KeyguardServiceDelegate keyguardDelegate) {
        this.mContext = context;
        sIsSupportPickUpSensor = isSupportPickUpSensor(this.mContext);
        Slog.i(TAG, "isSupportPickUpSensor:" + sIsSupportPickUpSensor);
        if (isSupportPickUp()) {
            this.mWindowManagerFuncs = windowManagerFuncs;
            this.mKgDelegate = keyguardDelegate;
            this.mResolver = this.mContext.getContentResolver();
            this.mPickUpHandler = new PickUpHandler(handler.getLooper());
            this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
            this.mProximitySensor = this.mSensorManager.getDefaultSensor(8);
            Sensor sensor = this.mProximitySensor;
            if (sensor != null) {
                float maximumRange = sensor.getMaximumRange();
                float f = TYPICAL_PROXIMITY_THRESHOLD;
                if (maximumRange < TYPICAL_PROXIMITY_THRESHOLD) {
                    f = this.mProximitySensor.getMaximumRange();
                }
                this.mProximityThreshold = f;
            }
            this.mContext.registerReceiverAsUser(this.mUserSwitchReceiver, UserHandle.ALL, new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED), null, null);
            MRUtils.observerMotionEnableStateChange(this.mContext, this.mPickupEnabledObserver, -1);
            this.mHwedManager = HWExtDeviceManager.getInstance(this.mContext);
            this.mHwExtMotion = new HWExtMotion(100);
            this.mCoverManager = new CoverManager();
            this.mIsInitCompleted = true;
        }
    }

    public void enablePickupMotionOrNot(boolean isScreenOff) {
        int motionPickUp;
        if (isSupportPickUp() && this.mIsInitCompleted) {
            if (this.mIsFirstBoot && (motionPickUp = MRUtils.getMotionEnableStateAsUser(this.mContext, MOTION_PICKUP_WAKEUP_DEVICE, ActivityManager.getCurrentUser())) != -1) {
                boolean z = true;
                if (motionPickUp != 1) {
                    z = false;
                }
                this.mIsPickupSwitchOn = z;
                this.mIsFirstBoot = false;
            }
            if (!this.mIsPickupSwitchOn && !HwPCUtils.isInWindowsCastMode()) {
                return;
            }
            if (Settings.Secure.getInt(this.mResolver, "device_provisioned", 0) == 0) {
                Slog.i(TAG, "Device is in Provision");
                return;
            }
            Slog.i(TAG, "isScreenOff:" + isScreenOff + ", mIsPickupSwitchOn:" + this.mIsPickupSwitchOn);
            if (isScreenOff) {
                scheduleUpdateDeviceListener();
            }
        }
    }

    public void stopTurnOffController() {
        setTurnOffController(false);
    }

    /* access modifiers changed from: private */
    public static boolean isSupportPickUp() {
        return IS_SUPPORT_PICKUP_PROP && sIsSupportPickUpSensor;
    }

    private static boolean isSupportFaceDectect() {
        return SystemProperties.getInt("ro.config.face_detect", 0) == 1 || SystemProperties.getBoolean("ro.config.face_recognition", false) || SystemProperties.getBoolean("ro.config.face_smart_keepon", false);
    }

    /* access modifiers changed from: private */
    public void setTurnOffController(boolean enable) {
        Handler handler;
        if (isSupportPickUp() && sIsNeedControlTurnOff && (handler = this.mPickUpHandler) != null) {
            handler.removeMessages(4);
            this.mPickUpHandler.sendMessage(this.mPickUpHandler.obtainMessage(4, enable ? 1 : 0, 0));
        }
    }

    /* access modifiers changed from: private */
    public void handleTurnOffController(boolean enable) {
        Slog.i(TAG, "handleTurnOffController enable:" + enable + ", turnOffTimeOutEnabled:" + this.mIsTurnOffTimeOutEnabled);
        if (this.mPickUpHandler.hasCallbacks(this.mPickupMotionTimeoutRunnable)) {
            this.mPickUpHandler.removeCallbacks(this.mPickupMotionTimeoutRunnable);
        }
        if (enable) {
            this.mPickUpHandler.postDelayed(this.mPickupMotionTimeoutRunnable, 5000);
        }
        this.mIsTurnOffTimeOutEnabled = enable;
        setPointerEventListenerEnable(enable);
    }

    private boolean isSupportPickUpSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        if (sensorManager == null) {
            return true;
        }
        List<Sensor> listSensors = sensorManager.getSensorList(-1);
        List<Integer> sensorTypeList = new ArrayList<>(listSensors.size());
        for (Sensor sensor : listSensors) {
            sensorTypeList.add(Integer.valueOf(sensor.getType()));
        }
        for (int sensorType : PICKUP_SENSORS) {
            if (!sensorTypeList.contains(Integer.valueOf(sensorType))) {
                return false;
            }
        }
        return true;
    }

    private void setPointerEventListenerEnable(boolean enable) {
        if (sIsNeedControlTurnOff) {
            if (!enable) {
                ScreenTouchEventListener screenTouchEventListener = this.mScreenTouchEventListener;
                if (screenTouchEventListener != null) {
                    this.mWindowManagerFuncs.unregisterPointerEventListener(screenTouchEventListener, 0);
                    this.mScreenTouchEventListener = null;
                    Slog.i(TAG, "unregisterPointerEventListener");
                }
            } else if (this.mScreenTouchEventListener == null) {
                this.mScreenTouchEventListener = new ScreenTouchEventListener();
                this.mWindowManagerFuncs.registerPointerEventListener(this.mScreenTouchEventListener, 0);
                Slog.i(TAG, "registerPointerEventListener");
            }
        }
    }

    private void triggerFaceDetect(String reason) {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKgDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.doFaceRecognize(true, reason);
        }
    }

    /* access modifiers changed from: private */
    public void wakeupScreenOnIfNeeded(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService("power");
        if (pm == null) {
            Slog.e(TAG, "pm is null");
        } else if (!pm.isInteractive() || !sHwMultiDisplayUtils.isScreenOnForHwMultiDisplay()) {
            sHwMultiDisplayUtils.lightScreenOnForHwMultiDisplay();
            FaceReportEventToIaware.reportEventToIaware(context, 20025);
            triggerFaceDetect("PICKUP:SC_OFF");
            wakeUpWaitForProximity();
        } else {
            Slog.i(TAG, "screen is already on");
            triggerFaceDetect("PICKUP:SC_ON");
        }
    }

    /* access modifiers changed from: private */
    public void setProximitySensorEnabled(boolean isEnabled) {
        if (isEnabled) {
            if (!this.mIsProximitySensorEnabled) {
                this.mIsProximitySensorEnabled = true;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mPickUpHandler);
                this.mPickUpHandler.sendEmptyMessageDelayed(1, 500);
                Slog.i(TAG, "proximity sensor registered.");
            }
        } else if (this.mIsProximitySensorEnabled) {
            this.mIsProximitySensorEnabled = false;
            this.mPickUpHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            Slog.i(TAG, "proximity sensor unregistered.");
        }
    }

    /* access modifiers changed from: private */
    public void handleProximitySensorEvent(boolean isPositive) {
        if (this.mIsProximitySensorEnabled) {
            if (isPositive) {
                stopWakeUpReady(this.mContext, false);
            } else {
                stopWakeUpReady(this.mContext, true);
            }
            setProximitySensorEnabled(false);
        }
    }

    private void wakeUpWaitForProximity() {
        if (this.mIsInFastScreenOn) {
            Slog.w(TAG, "wakeUpWaitForProximity already in processing");
            return;
        }
        startWakeUpReady(this.mContext);
        setProximitySensorEnabled(true);
    }

    private void startWakeUpReady(Context context) {
        Slog.i(TAG, "wakeUpWaitForProximity start");
        HwPowerManager.startWakeUpReady(context, SystemClock.uptimeMillis());
        this.mIsInFastScreenOn = true;
    }

    private void lockOnPadAssistMode(boolean isTurnOn) {
        if (isTurnOn && HwPCUtils.isPadAssistantMode()) {
            this.mWindowManagerFuncs.lockDeviceNow();
        }
    }

    /* access modifiers changed from: private */
    public void stopWakeUpReady(Context context, boolean isTurnOn) {
        if (!this.mIsInFastScreenOn) {
            Slog.w(TAG, "stopWakeUpReady, not in fast screen on");
            return;
        }
        Slog.i(TAG, "stopWakeUpReady isTurnOn = " + isTurnOn);
        lockOnPadAssistMode(isTurnOn);
        HwPowerManager.stopWakeUpReady(context, SystemClock.uptimeMillis(), isTurnOn);
        this.mIsInFastScreenOn = false;
        if (isTurnOn) {
            setTurnOffController(true);
            if (IS_SUPPORT_FACE_RECOGNITION) {
                Handler handler = this.mPickUpHandler;
                handler.obtainMessage(2, 0, 0, "android.policy:HWPICKUP#" + Binder.getCallingUid()).sendToTarget();
            }
            boolean isBdReportSuccess = Flog.bdReport(this.mContext, 500);
            Slog.d(TAG, "report no proximity ScreenOn waked by pickup motion. EventId:500 isBdReportSuccess:" + isBdReportSuccess);
            HwLog.dubaie("DUBAI_TAG_PICKUP_WAKE", "type=1");
            return;
        }
        HwLog.dubaie("DUBAI_TAG_PICKUP_WAKE, ENTER AOD", "type=0");
    }

    private boolean getPickupSwitchOn() {
        return MRUtils.getMotionEnableStateAsUser(this.mContext, MOTION_PICKUP_WAKEUP_DEVICE, ActivityManager.getCurrentUser()) == 1;
    }

    /* access modifiers changed from: private */
    public void updatePickupSwitchState() {
        this.mIsPickupSwitchOn = getPickupSwitchOn();
        scheduleUpdateDeviceListener();
    }

    private void scheduleUpdateDeviceListener() {
        Handler handler = this.mPickUpHandler;
        if (handler != null && !handler.hasMessages(3)) {
            this.mPickUpHandler.sendEmptyMessage(3);
        }
    }

    /* access modifiers changed from: private */
    public void updateDeviceListener() {
        if (isSupportPickUp() && this.mHwedManager != null) {
            Slog.i(TAG, "updateDeviceListener mIsPickupSwitchOn:" + this.mIsPickupSwitchOn);
            if (this.mIsPickupSwitchOn) {
                if (this.mHwedListener == null) {
                    this.mHwedListener = new DeviceMotionListener();
                    this.mHwedManager.registerDeviceListener(this.mHwedListener, this.mHwExtMotion, this.mPickUpHandler);
                    Slog.i(TAG, "register device listener");
                }
            } else if (this.mHwedListener != null && !HwPCUtils.isInWindowsCastMode()) {
                this.mHwedManager.unregisterDeviceListener(this.mHwedListener, this.mHwExtMotion);
                this.mHwedListener = null;
                Slog.i(TAG, "unregister device listener");
            }
        }
    }

    private class PickUpHandler extends Handler {
        PickUpHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            boolean z = false;
            if (i == 1) {
                Slog.i(PickUpWakeScreenManager.TAG, "wait proximity sensor timeout(more than 500ms).");
                PickUpWakeScreenManager.this.setProximitySensorEnabled(false);
                PickUpWakeScreenManager pickUpWakeScreenManager = PickUpWakeScreenManager.this;
                pickUpWakeScreenManager.stopWakeUpReady(pickUpWakeScreenManager.mContext, true);
            } else if (i == 2) {
                Slog.i(PickUpWakeScreenManager.TAG, "Face Dectect wakeUpInternal type:" + msg.obj);
                Settings.Global.putString(PickUpWakeScreenManager.this.mContext.getContentResolver(), PickUpWakeScreenManager.WAKEUP_REASON, msg.obj == null ? "unknow" : msg.obj.toString());
            } else if (i == 3) {
                PickUpWakeScreenManager.this.updateDeviceListener();
            } else if (i == 4) {
                PickUpWakeScreenManager pickUpWakeScreenManager2 = PickUpWakeScreenManager.this;
                if (msg.arg1 == 1) {
                    z = true;
                }
                pickUpWakeScreenManager2.handleTurnOffController(z);
            }
        }
    }

    private class DeviceMotionListener implements HWExtDeviceEventListener {
        private DeviceMotionListener() {
        }

        public void onDeviceDataChanged(HWExtDeviceEvent hwextDeviceEvent) {
            Slog.i(PickUpWakeScreenManager.TAG, "onDeviceDataChanged");
            if (PickUpWakeScreenManager.isSupportPickUp()) {
                if (hwextDeviceEvent.getDeviceValues() == null) {
                    Slog.e(PickUpWakeScreenManager.TAG, "onDeviceDataChanged deviceValues is null");
                } else if (!PickUpWakeScreenManager.this.mIsPickupSwitchOn && !HwPCUtils.isInWindowsCastMode()) {
                    Slog.w(PickUpWakeScreenManager.TAG, "mIsPickupSwitchOn is off");
                } else if (!PickUpWakeScreenManager.IS_TABLET || !PickUpWakeScreenManager.IS_SIMPLE_COVER || PickUpWakeScreenManager.this.mProximitySensor != null || PickUpWakeScreenManager.this.mCoverManager == null || PickUpWakeScreenManager.this.mCoverManager.isCoverOpen()) {
                    PickUpWakeScreenManager pickUpWakeScreenManager = PickUpWakeScreenManager.this;
                    pickUpWakeScreenManager.wakeupScreenOnIfNeeded(pickUpWakeScreenManager.mContext);
                } else {
                    Slog.i(PickUpWakeScreenManager.TAG, "has no proxmimity and cover is closed");
                }
            }
        }
    }

    private class ScreenTouchEventListener implements WindowManagerPolicyConstants.PointerEventListener {
        private ScreenTouchEventListener() {
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (PickUpWakeScreenManager.this.mIsTurnOffTimeOutEnabled && motionEvent.getAction() == 0) {
                PickUpWakeScreenManager.this.setTurnOffController(false);
            }
        }
    }
}
