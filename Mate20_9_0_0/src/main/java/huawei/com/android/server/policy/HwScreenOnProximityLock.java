package huawei.com.android.server.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
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
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.server.gesture.GestureNavConst;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs;
import com.android.server.security.trustcircle.tlv.command.register.RET_REG_CANCEL;
import com.huawei.android.util.HwNotchSizeUtil;
import com.huawei.hwextdevice.HWExtDeviceEvent;
import com.huawei.hwextdevice.HWExtDeviceEventListener;
import com.huawei.hwextdevice.HWExtDeviceManager;
import com.huawei.hwextdevice.devices.HWExtMotion;
import com.huawei.msdp.devicestatus.DeviceStatusConstant;

public class HwScreenOnProximityLock {
    private static final String APS_RESOLUTION_CHANGE_ACTION = "huawei.intent.action.APS_RESOLUTION_CHANGE_ACTION";
    private static final String APS_RESOLUTION_CHANGE_PERSISSIONS = "huawei.intent.permissions.APS_RESOLUTION_CHANGE_ACTION";
    private static final boolean DEBUG = false;
    public static final int FARAWAY_SENSOR = 2;
    public static final int FORCE_QUIT = 0;
    public static final int HEADDOWN_LEAVE = 4;
    private static final boolean HWFLOW;
    public static final int LOCK_GOAWAY = 3;
    private static final int MOTION_HEADDOWN_ENTER = 1;
    private static final int MOTION_HEADDOWN_LEAVE = 2;
    private static final int MSG_REFRESH_HINT_VIEW = 2;
    private static final int MSG_SHOW_HINT_VIEW = 1;
    private static final String SCREENON_TAG = "ScreenOn";
    public static final int SCREEN_OFF = 1;
    private static final int SENSOR_FEATURE = 2049;
    private static final String TAG = "HwScreenOnProximityLock";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final String mNotchProp = SystemProperties.get("ro.config.hw_notch_size", "");
    private static final String sProximityWndName = "Emui:ProximityWnd";
    private AccSensorListener mAccListener = null;
    private BroadcastReceiver mApsResolutionChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(HwScreenOnProximityLock.TAG, "on receive apsResolutionChangeReceiver");
            HwScreenOnProximityLock.this.preparePoriximityView();
        }
    };
    private Context mContext;
    private CoverManager mCoverManager;
    private boolean mDeviceHeld = false;
    private final Object mDeviceLock = new Object();
    private boolean mFirstBoot = true;
    public ContentObserver mFontScaleObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Log.i(HwScreenOnProximityLock.TAG, "font Scale changed");
            HwScreenOnProximityLock.this.preparePoriximityView();
        }
    };
    private HWExtDeviceEventListener mHWEDListener = new HWExtDeviceEventListener() {
        public void onDeviceDataChanged(HWExtDeviceEvent hwextDeviceEvent) {
            Log.d(HwScreenOnProximityLock.TAG, "onDeviceDataChanged");
            float[] deviceValues = hwextDeviceEvent.getDeviceValues();
            if (deviceValues == null) {
                Log.e(HwScreenOnProximityLock.TAG, "onDeviceDataChanged  deviceValues is null ");
            } else if (deviceValues.length < 2) {
                Log.e(HwScreenOnProximityLock.TAG, "hwextDeviceEvent data error");
            } else {
                HwScreenOnProximityLock.this.mHeadDownStatus = (int) deviceValues[1];
                String str = HwScreenOnProximityLock.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mHeadDownStatus:");
                stringBuilder.append(HwScreenOnProximityLock.this.mHeadDownStatus);
                Log.d(str, stringBuilder.toString());
            }
        }
    };
    private HWExtDeviceManager mHWEDManager = null;
    private HWExtMotion mHWExtMotion = null;
    private Handler mHandler;
    private boolean mHasNotchInScreen = false;
    private int mHeadDownStatus = 2;
    private boolean mHeld;
    private View mHintView;
    private boolean mIsProximity;
    private int mLastRotation = 0;
    private ProximitySensorListener mListener;
    private BroadcastReceiver mLocaleChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(HwScreenOnProximityLock.TAG, "on receive localeChangeReceiver");
            HwScreenOnProximityLock.this.preparePoriximityView();
        }
    };
    private final Object mLock = new Object();
    private LayoutParams mParams = null;
    private HwPhoneWindowManager mPhoneWindowManager;
    private WindowManagerPolicy mPolicy;
    private float mProximityThreshold;
    private final boolean mProximityTop = "true".equals(SystemProperties.get("ro.config.proximity_top", "false"));
    private FrameLayout mProximityView = null;
    private int[] mReleaseReasons = new int[]{0, 1, 3, 4};
    private int mRotation = 0;
    private SensorManager mSensorManager;
    private boolean mSupportSensorFeature = false;
    private final boolean mTouchHeadDown = "true".equals(SystemProperties.get("ro.config.touch.head_down", "true"));
    private boolean mUseSensorFeature = false;
    private BroadcastReceiver mUserSwitchReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(HwScreenOnProximityLock.TAG, "on receive userSwitchReceiver");
            HwScreenOnProximityLock.this.preparePoriximityView();
        }
    };
    private boolean mViewAttached = false;
    private WindowManager mWindowManager;

    private class AccSensorListener implements SensorEventListener {
        private boolean mFlat = false;
        private boolean mListening = false;

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            if (event != null && event.values != null && event.values.length >= 3) {
                boolean z = false;
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];
                float sqrt = (float) Math.sqrt((double) ((float) ((Math.pow((double) axisX, 2.0d) + Math.pow((double) axisY, 2.0d)) + Math.pow((double) axisZ, 2.0d))));
                float pitch = (float) (-Math.asin((double) (axisY / sqrt)));
                float roll = (float) Math.asin((double) (axisX / sqrt));
                if (((double) pitch) >= -0.7853981633974483d && ((double) pitch) <= 0.17453292519943295d && ((double) Math.abs(roll)) <= 0.2617993877991494d && axisZ >= HwScreenOnProximityLock.TYPICAL_PROXIMITY_THRESHOLD) {
                    z = true;
                }
                this.mFlat = z;
                if (this.mFlat) {
                    HwScreenOnProximityLock.this.removeProximityView();
                } else if (HwScreenOnProximityLock.this.mIsProximity) {
                    HwScreenOnProximityLock.this.addProximityView();
                }
            }
        }

        public void register() {
            Sensor sensor = HwScreenOnProximityLock.this.mSensorManager.getDefaultSensor(1);
            if (sensor == null) {
                Log.w(HwScreenOnProximityLock.TAG, "AccSensorListener register failed, because of orientation sensor is not existed");
                return;
            }
            String str = HwScreenOnProximityLock.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AccSensorListener sensortype ");
            stringBuilder.append(sensor.getType());
            Log.d(str, stringBuilder.toString());
            if (this.mListening) {
                Log.w(HwScreenOnProximityLock.TAG, "AccSensorListener already register");
                return;
            }
            Log.d(HwScreenOnProximityLock.TAG, "AccSensorListener register");
            this.mListening = HwScreenOnProximityLock.this.mSensorManager.registerListener(this, sensor, 3);
        }

        public void unregister() {
            if (this.mListening) {
                Log.d(HwScreenOnProximityLock.TAG, "AccSensorListener unregister");
                HwScreenOnProximityLock.this.mSensorManager.unregisterListener(this);
                this.mListening = false;
                return;
            }
            Log.w(HwScreenOnProximityLock.TAG, "AccSensorListener not register yet");
        }

        public boolean isFlat() {
            String str = HwScreenOnProximityLock.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AccSensorListener mFlat ");
            stringBuilder.append(this.mFlat);
            Log.d(str, stringBuilder.toString());
            return this.mFlat;
        }
    }

    private class ProximitySensorListener implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            boolean z = false;
            float d = event.values[0];
            HwScreenOnProximityLock hwScreenOnProximityLock = HwScreenOnProximityLock.this;
            boolean z2 = d >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && d < HwScreenOnProximityLock.this.mProximityThreshold;
            hwScreenOnProximityLock.mIsProximity = z2;
            hwScreenOnProximityLock = HwScreenOnProximityLock.this;
            z2 = HwScreenOnProximityLock.this.mSupportSensorFeature && !HwScreenOnProximityLock.this.mIsProximity && HwScreenOnProximityLock.this.mHeadDownStatus == 1 && HwScreenOnProximityLock.this.mPolicy.isKeyguardLocked();
            hwScreenOnProximityLock.mUseSensorFeature = z2;
            String str = HwScreenOnProximityLock.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mSupportSensorFeature:");
            stringBuilder.append(HwScreenOnProximityLock.this.mSupportSensorFeature);
            stringBuilder.append(",mHeadDownStatus:");
            stringBuilder.append(HwScreenOnProximityLock.this.mHeadDownStatus);
            stringBuilder.append(",mUseSensorFeature:");
            stringBuilder.append(HwScreenOnProximityLock.this.mUseSensorFeature);
            Log.d(str, stringBuilder.toString());
            if (HwScreenOnProximityLock.this.mUseSensorFeature) {
                int d2 = (((int) event.values[2]) >> 8) & 1;
                String str2 = HwScreenOnProximityLock.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onSensorChanged: useSensorFeature distance = ");
                stringBuilder2.append(d2);
                Log.d(str2, stringBuilder2.toString());
                HwScreenOnProximityLock hwScreenOnProximityLock2 = HwScreenOnProximityLock.this;
                if (d2 == 1) {
                    z = true;
                }
                hwScreenOnProximityLock2.mIsProximity = z;
            }
            handleSensorChanges();
        }

        /* JADX WARNING: Missing block: B:20:0x006b, code skipped:
            r4.this$0.releaseLock(2);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void handleSensorChanges() {
            if (HwScreenOnProximityLock.this.mCoverManager == null) {
                Log.e(HwScreenOnProximityLock.TAG, "mCoverManager is null");
                return;
            }
            boolean isCoverOpen = HwScreenOnProximityLock.this.mCoverManager.isCoverOpen();
            String str = HwScreenOnProximityLock.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleSensorChanged: close to sensor: ");
            stringBuilder.append(HwScreenOnProximityLock.this.mIsProximity);
            stringBuilder.append(", isCoverOpen: ");
            stringBuilder.append(isCoverOpen);
            Log.i(str, stringBuilder.toString());
            if (HwScreenOnProximityLock.this.mIsProximity && isCoverOpen) {
                HwScreenOnProximityLock.this.addProximityView();
            } else {
                synchronized (HwScreenOnProximityLock.this.mLock) {
                    if (isCoverOpen) {
                        try {
                            if (HwScreenOnProximityLock.this.mProximityView == null) {
                                Log.i(HwScreenOnProximityLock.TAG, "no need to releaseLock");
                            }
                        } catch (Throwable th) {
                            while (true) {
                            }
                        }
                    }
                }
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(SCREENON_TAG, 4));
        HWFLOW = z;
    }

    private void registerBroadcastReceiver() {
        this.mContext.registerReceiverAsUser(this.mApsResolutionChangeReceiver, UserHandle.ALL, new IntentFilter(APS_RESOLUTION_CHANGE_ACTION), APS_RESOLUTION_CHANGE_PERSISSIONS, null);
        this.mContext.registerReceiverAsUser(this.mLocaleChangeReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.LOCALE_CHANGED"), null, null);
        this.mContext.registerReceiverAsUser(this.mUserSwitchReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_SWITCHED"), null, null);
    }

    private void registerContentObserver() {
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("font_scale"), true, this.mFontScaleObserver);
    }

    public HwScreenOnProximityLock(Context context, WindowManagerPolicy policy, WindowManagerFuncs windowFuncs) {
        if (context == null) {
            Log.w(TAG, "HwScreenOnProximityLock context is null");
            return;
        }
        this.mContext = context;
        this.mPolicy = policy;
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mCoverManager = new CoverManager();
        this.mPhoneWindowManager = (HwPhoneWindowManager) policy;
        this.mHWEDManager = HWExtDeviceManager.getInstance(this.mContext);
        this.mHWExtMotion = new HWExtMotion(DeviceStatusConstant.TYPE_HEAD_DOWN);
        this.mListener = new ProximitySensorListener();
        if (this.mProximityTop) {
            this.mAccListener = new AccSensorListener();
        }
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        HwScreenOnProximityLock.this.showHintView();
                        return;
                    case 2:
                        HwScreenOnProximityLock.this.refreshHintView();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mHasNotchInScreen = 1 ^ TextUtils.isEmpty(mNotchProp);
        init();
        this.mSupportSensorFeature = this.mSensorManager.supportSensorFeature(SENSOR_FEATURE);
        System.putInt(this.mContext.getContentResolver(), "support_sensor_feature", this.mSupportSensorFeature);
    }

    private void init() {
        registerBroadcastReceiver();
        registerContentObserver();
    }

    /* JADX WARNING: Missing block: B:22:0x004b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void registerDeviceListener() {
        if (this.mTouchHeadDown) {
            synchronized (this.mDeviceLock) {
                if (this.mDeviceHeld) {
                    Log.d(TAG, "mDeviceHeld is true,register return");
                } else if (this.mHWEDManager == null) {
                    Log.e(TAG, "mHWEDManager is null,register return");
                } else {
                    this.mDeviceHeld = this.mHWEDManager.registerDeviceListener(this.mHWEDListener, this.mHWExtMotion, this.mHandler);
                    if (this.mDeviceHeld) {
                        Log.d(TAG, "registerDeviceListener succeed");
                    } else {
                        Log.d(TAG, "registerDeviceListener fail");
                    }
                }
            }
        } else {
            Log.w(TAG, "mTouchHeadDown is false,no need to registerDeviceListener");
        }
    }

    public void unregisterDeviceListener() {
        synchronized (this.mDeviceLock) {
            if (!this.mDeviceHeld) {
                Log.d(TAG, "mDeviceHeld is false,unregister return");
            } else if (this.mHWEDManager == null) {
                Log.e(TAG, "mHWEDManager is null,unregister return");
            } else {
                this.mHWEDManager.unregisterDeviceListener(this.mHWEDListener, this.mHWExtMotion);
                Log.d(TAG, "unregisterDeviceListener succeed");
                this.mDeviceHeld = false;
                this.mHeadDownStatus = 2;
            }
        }
    }

    /* JADX WARNING: Missing block: B:27:0x0062, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void registerProximityListener() {
        synchronized (this.mLock) {
            if (this.mHeld) {
                Log.w(TAG, "mHeld is true,registerProximityListener return");
                return;
            }
            Sensor sensor = this.mSensorManager.getDefaultSensor(8);
            if (sensor == null) {
                Log.w(TAG, "registerProximityListener return because of proximity sensor is not existed");
                return;
            }
            float maxRange = sensor.getMaximumRange();
            if (maxRange >= TYPICAL_PROXIMITY_THRESHOLD) {
                this.mProximityThreshold = TYPICAL_PROXIMITY_THRESHOLD;
            } else if (maxRange < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                this.mProximityThreshold = TYPICAL_PROXIMITY_THRESHOLD;
            } else {
                this.mProximityThreshold = maxRange;
            }
            this.mHeld = this.mSensorManager.registerListener(this.mListener, sensor, 3);
            if (this.mHeld) {
                Log.d(TAG, "registerProximityListener success");
                if (this.mAccListener != null) {
                    this.mAccListener.register();
                }
            } else {
                Log.d(TAG, "registerProximityListener fail");
            }
        }
    }

    private boolean shouldReleaseProximity(int reason) {
        for (int i : this.mReleaseReasons) {
            if (reason == i) {
                return true;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isKeyguardLocked:");
        stringBuilder.append(this.mPolicy.isKeyguardLocked());
        Log.i(str, stringBuilder.toString());
        return this.mPolicy.isKeyguardLocked() ^ 1;
    }

    public void acquireLock(WindowManagerPolicy policy) {
        if (policy == null) {
            Log.w(TAG, "acquire Lock: return because get Window Manager policy is null");
            return;
        }
        registerProximityListener();
        if (!this.mPolicy.isKeyguardLocked()) {
            Log.d(TAG, "keyguard not locked");
        }
    }

    /* JADX WARNING: Missing block: B:14:0x005c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void releaseLock(int reason) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (this.mHeld) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("releaseLock,reason:");
                stringBuilder.append(reason);
                Log.i(str, stringBuilder.toString());
                removeProximityView();
                if (shouldReleaseProximity(reason)) {
                    this.mHeld = false;
                    this.mSensorManager.unregisterListener(this.mListener);
                    Log.i(TAG, "unregister proximity sensor listener");
                    if (this.mAccListener != null) {
                        this.mAccListener.unregister();
                    }
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("releaseLock: return because sensor listener is held = ");
                stringBuilder.append(this.mHeld);
                Log.w(str, stringBuilder.toString());
            }
        }
    }

    public static String reasonString(int reason) {
        switch (reason) {
            case 0:
                return "force quit";
            case 1:
                return "screen off";
            case 2:
                return "faraway sensor";
            default:
                return "unkown reason";
        }
    }

    public boolean isShowing() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mProximityView != null && this.mViewAttached;
        }
        return z;
    }

    public void forceShowHint() {
        this.mHandler.sendEmptyMessage(1);
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x00eb A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0071  */
    /* JADX WARNING: Missing block: B:25:0x00ea, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void preparePoriximityView() {
        removeProximityView();
        synchronized (this.mLock) {
            View view;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("inflate View, config : ");
            stringBuilder.append(this.mContext.getResources().getConfiguration());
            Log.i(str, stringBuilder.toString());
            if (this.mRotation != 0) {
                if (this.mRotation != 2) {
                    view = View.inflate(this.mContext, 34013314, null);
                    if (view instanceof FrameLayout) {
                        return;
                    }
                    this.mProximityView = (FrameLayout) view;
                    this.mHintView = this.mProximityView.findViewById(34603159);
                    if (this.mHasNotchInScreen) {
                        View notchView = this.mProximityView.findViewById(34603044);
                        if (notchView != null) {
                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) notchView.getLayoutParams();
                            params.height = HwNotchSizeUtil.getNotchSize()[1];
                            notchView.setLayoutParams(params);
                        }
                    }
                    this.mProximityView.setOnTouchListener(new OnTouchListener() {
                        public boolean onTouch(View v, MotionEvent event) {
                            HwScreenOnProximityLock.this.showHintView();
                            return false;
                        }
                    });
                    this.mParams = new LayoutParams(-1, -1, 2100, 134223104, -2);
                    LayoutParams layoutParams = this.mParams;
                    layoutParams.inputFeatures |= 4;
                    layoutParams = this.mParams;
                    layoutParams.privateFlags |= RET_REG_CANCEL.ID;
                    this.mParams.setTitle(sProximityWndName);
                    if (this.mHasNotchInScreen) {
                        this.mParams.layoutInDisplayCutoutMode = 1;
                    }
                    if (HWFLOW) {
                        Log.i(TAG, "preparePoriximityView addView ");
                    }
                }
            }
            view = View.inflate(this.mContext, 34013254, null);
            LinearLayout ll = (LinearLayout) view.findViewById(34603043);
            DisplayMetrics dm = new DisplayMetrics();
            this.mWindowManager.getDefaultDisplay().getMetrics(dm);
            int width = dm.widthPixels;
            ll.setLayoutParams(new LinearLayout.LayoutParams(width, (width * 5) / 4));
            if (view instanceof FrameLayout) {
            }
        }
    }

    /* JADX WARNING: Missing block: B:23:0x003f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void showHintView() {
        synchronized (this.mLock) {
            if (this.mProximityView == null) {
            } else if (this.mHintView == null) {
            } else if (this.mHintView.getVisibility() != 0 && this.mViewAttached) {
                this.mHintView.setVisibility(0);
                long token = Binder.clearCallingIdentity();
                try {
                    System.putIntForUser(this.mContext.getContentResolver(), "proximity_wnd_status", 1, ActivityManager.getCurrentUser());
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:44:0x00c3, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addProximityView() {
        synchronized (this.mLock) {
            if (this.mRotation != this.mLastRotation) {
                preparePoriximityView();
                this.mLastRotation = this.mRotation;
            }
            if (!(this.mProximityView == null || this.mPhoneWindowManager.isKeyguardShortcutApps())) {
                if (!this.mPhoneWindowManager.isLandscape() || !this.mPhoneWindowManager.isLsKeyguardShortcutApps()) {
                    if (this.mProximityView == null || this.mParams == null || this.mViewAttached || !this.mHeld || this.mCoverManager == null || !this.mCoverManager.isCoverOpen() || (this.mAccListener != null && this.mAccListener.isFlat())) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("no need to addView:mProximityView = ");
                        stringBuilder.append(this.mProximityView);
                        stringBuilder.append("mParams=");
                        stringBuilder.append(this.mParams);
                        stringBuilder.append(",mViewAttached:");
                        stringBuilder.append(this.mViewAttached);
                        stringBuilder.append(",mHeld:");
                        stringBuilder.append(this.mHeld);
                        Log.w(str, stringBuilder.toString());
                    } else {
                        if (HWFLOW) {
                            Log.i(TAG, "addProximityView ");
                        }
                        if (this.mFirstBoot) {
                            Log.i(TAG, "first boot,prepare again");
                            preparePoriximityView();
                            this.mFirstBoot = false;
                        }
                        this.mWindowManager.addView(this.mProximityView, this.mParams);
                        if (this.mHintView == null) {
                            return;
                        } else {
                            this.mHintView.setVisibility(8);
                            this.mViewAttached = true;
                        }
                    }
                }
            }
            Log.i(TAG, "no need to addProximityView");
        }
    }

    private void removeProximityView() {
        synchronized (this.mLock) {
            if (this.mProximityView == null || !this.mViewAttached) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("no need to removeView:mProximityView = ");
                stringBuilder.append(this.mProximityView);
                stringBuilder.append(",mViewAttached");
                stringBuilder.append(this.mViewAttached);
                Log.w(str, stringBuilder.toString());
            } else {
                ViewParent vp = this.mProximityView.getParent();
                String str2;
                StringBuilder stringBuilder2;
                if (vp == null) {
                    try {
                        this.mWindowManager.removeView(this.mProximityView);
                        this.mViewAttached = false;
                        Log.i(TAG, "removeview directly ");
                    } catch (RuntimeException e) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("removeView fail ");
                        stringBuilder3.append(e.getMessage());
                        Log.i(str3, stringBuilder3.toString());
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(token);
                    }
                } else if (this.mWindowManager == null || !(vp instanceof ViewRootImpl)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("removeView fail: mWindowManager = ");
                    stringBuilder2.append(this.mWindowManager);
                    stringBuilder2.append(", viewparent = ");
                    stringBuilder2.append(vp);
                    Log.w(str2, stringBuilder2.toString());
                } else {
                    if (HWFLOW) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("removeProximityView success vp ");
                        stringBuilder2.append(vp);
                        Log.i(str2, stringBuilder2.toString());
                    }
                    this.mWindowManager.removeView(this.mProximityView);
                    this.mViewAttached = false;
                }
                long token = Binder.clearCallingIdentity();
                System.putIntForUser(this.mContext.getContentResolver(), "proximity_wnd_status", 0, ActivityManager.getCurrentUser());
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public void refreshForRotationChange(int rotation) {
        this.mLastRotation = this.mRotation;
        this.mRotation = rotation;
        this.mHandler.sendEmptyMessage(2);
    }

    private void refreshHintView() {
        boolean shouldShow = this.mHintView.getVisibility() == 0;
        if (this.mHeld && this.mIsProximity) {
            addProximityView();
            if (shouldShow) {
                showHintView();
            }
        }
    }
}
