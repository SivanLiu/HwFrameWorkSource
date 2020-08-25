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
import android.hardware.SystemSensorManager;
import android.hardware.display.HwFoldScreenState;
import android.os.Binder;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.HwScreenOnProximityLayout;
import com.android.server.policy.WindowManagerPolicy;
import com.huawei.android.fsm.HwFoldScreenManagerEx;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.android.util.HwNotchSizeUtil;
import com.huawei.android.view.WindowManagerEx;
import com.huawei.hwextdevice.HWExtDeviceEvent;
import com.huawei.hwextdevice.HWExtDeviceEventListener;
import com.huawei.hwextdevice.HWExtDeviceManager;
import com.huawei.hwextdevice.devices.HWExtMotion;
import com.huawei.msdp.devicestatus.DeviceStatusConstant;
import huawei.android.view.HwWindowManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HwScreenOnProximityLock {
    private static final String APS_RESOLUTION_CHANGE_ACTION = "huawei.intent.action.APS_RESOLUTION_CHANGE_ACTION";
    private static final String APS_RESOLUTION_CHANGE_PERSISSIONS = "huawei.intent.permissions.APS_RESOLUTION_CHANGE_ACTION";
    private static final int AXIS_VALUE_LENGTH = 3;
    private static final int AXIS_X_INDEX = 0;
    private static final int AXIS_Y_INDEX = 1;
    private static final int AXIS_Z_INDEX = 2;
    private static final float AXIS_Z_THRESHOLD = 5.0f;
    private static final int DYNAMIC_SENSOR_BIT = 8;
    private static final int DYNAMIC_SENSOR_INDEX = 2;
    private static final int DYNAMIC_SENSOR_LENGTH = 3;
    public static final int FARAWAY_SENSOR = 2;
    public static final int FORCE_QUIT = 0;
    public static final int HEADDOWN_LEAVE = 4;
    private static final int HEIGHT_SCALE = 4;
    private static final int HEIGHT_SCALE_FOLD = 8;
    private static final String HW_CURVED_SIDE = SystemProperties.get("ro.config.hw_curved_side_disp", "");
    private static final String HW_NOTCH_SIZE = SystemProperties.get("ro.config.hw_notch_size", "");
    private static final boolean IS_DEBUG = false;
    private static final boolean IS_FOLDABLE = HwFoldScreenState.isFoldScreenDevice();
    private static final boolean IS_HWFLOW = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(SCREENON_TAG, 4)));
    public static final int LOCK_GOAWAY = 3;
    private static final int MATH_POW_NUM = 2;
    private static final int MOTION_HEADDOWN_ENTER = 1;
    private static final int MOTION_HEADDOWN_LEAVE = 2;
    private static final int MOTION_VALID_LENGTH = 2;
    private static final float PITCH_HIGHER = 12.0f;
    private static final float PITCH_LOWER = 3.6f;
    private static final String POCKET_IN = "pattern [POCKET_IN]";
    private static final String POCKET_OUT = "pattern [POCKET_OUT]";
    private static final boolean PROXIMITY_ENABLE = SystemProperties.getBoolean("ro.product.proximityenable", true);
    private static final String PROXIMITY_WND_NAME = "Emui:ProximityWnd";
    private static final float ROLL_THRESHOLD = 6.0f;
    private static final String ROOT_CAUSE_RESOLVE_PATTERN_LOG = "/data/log/reliability/rootcauseresolve/pattern.log";
    private static final int ROOT_CAUSE_RESOLVE_PATTERN_PATTERN_FILE_SIZE = 10240;
    private static final String SCREENON_TAG = "ScreenOn";
    public static final int SCREEN_OFF = 1;
    private static final int SENSOR_FEATURE = 2049;
    private static final String TAG = "HwScreenOnProximityLock";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final int WIDTH_SCALE = 5;
    private AccSensorListener mAccListener = null;
    private BroadcastReceiver mApsResolutionChangeReceiver = new BroadcastReceiver() {
        /* class huawei.com.android.server.policy.HwScreenOnProximityLock.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            Log.i(HwScreenOnProximityLock.TAG, "on receive apsResolutionChangeReceiver");
            boolean unused = HwScreenOnProximityLock.this.mIsNeedRefresh = true;
        }
    };
    private Context mContext;
    /* access modifiers changed from: private */
    public CoverManager mCoverManager;
    private ContentObserver mFontScaleObserver = new ContentObserver(new Handler()) {
        /* class huawei.com.android.server.policy.HwScreenOnProximityLock.AnonymousClass1 */

        public void onChange(boolean isSelfChange) {
            Log.i(HwScreenOnProximityLock.TAG, "font Scale changed");
            HwScreenOnProximityLock.this.prepareProximityView();
        }
    };
    /* access modifiers changed from: private */
    public BallFrameView mFrameView;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private boolean mHasNotchInScreen = false;
    /* access modifiers changed from: private */
    public int mHeadDownStatus = 2;
    private View mHintView;
    private HWExtDeviceEventListener mHwExtDeviceListener = new HWExtDeviceEventListener() {
        /* class huawei.com.android.server.policy.HwScreenOnProximityLock.AnonymousClass2 */

        public void onDeviceDataChanged(HWExtDeviceEvent hwextDeviceEvent) {
            Log.d(HwScreenOnProximityLock.TAG, "onDeviceDataChanged");
            float[] deviceValues = hwextDeviceEvent.getDeviceValues();
            if (deviceValues == null) {
                Log.e(HwScreenOnProximityLock.TAG, "onDeviceDataChanged deviceValues is null ");
            } else if (deviceValues.length < 2) {
                Log.e(HwScreenOnProximityLock.TAG, "hwextDeviceEvent data error");
            } else {
                boolean isUseSensorFeature = true;
                int unused = HwScreenOnProximityLock.this.mHeadDownStatus = (int) deviceValues[1];
                Log.i(HwScreenOnProximityLock.TAG, "mHeadDownStatus:" + HwScreenOnProximityLock.this.mHeadDownStatus);
                if (HwScreenOnProximityLock.this.mHeadDownStatus == 1) {
                    if (!HwScreenOnProximityLock.this.mIsSupportSensorFeature || HwScreenOnProximityLock.this.mIsProximityGen || !HwScreenOnProximityLock.this.mPhoneWindowManager.isKeyguardLocked()) {
                        isUseSensorFeature = false;
                    }
                    HwScreenOnProximityLock hwScreenOnProximityLock = HwScreenOnProximityLock.this;
                    boolean unused2 = hwScreenOnProximityLock.mIsProximity = isUseSensorFeature ? hwScreenOnProximityLock.mIsProximityDyn : hwScreenOnProximityLock.mIsProximityGen;
                    if (HwScreenOnProximityLock.this.mIsProximity) {
                        HwScreenOnProximityLock.this.addProximityView();
                    }
                } else if (HwScreenOnProximityLock.this.mHeadDownStatus == 2) {
                    HwScreenOnProximityLock hwScreenOnProximityLock2 = HwScreenOnProximityLock.this;
                    boolean unused3 = hwScreenOnProximityLock2.mIsProximity = hwScreenOnProximityLock2.mIsProximityGen;
                    if (!HwScreenOnProximityLock.this.mIsProximity) {
                        HwScreenOnProximityLock.this.lambda$releaseLock$3$HwScreenOnProximityLock(4);
                    }
                } else {
                    Log.d(HwScreenOnProximityLock.TAG, "not handle other headdown status");
                }
            }
        }
    };
    private HWExtDeviceManager mHwExtDeviceManager = null;
    private HWExtMotion mHwExtMotion = null;
    private boolean mIsDeviceHeld = false;
    /* access modifiers changed from: private */
    public boolean mIsNeedRefresh = true;
    /* access modifiers changed from: private */
    public boolean mIsProximity;
    /* access modifiers changed from: private */
    public boolean mIsProximityDyn;
    /* access modifiers changed from: private */
    public boolean mIsProximityGen;
    private boolean mIsProximityHeld;
    private final boolean mIsProximityTop = SystemProperties.getBoolean("ro.config.proximity_top", false);
    /* access modifiers changed from: private */
    public boolean mIsSupportSensorFeature = false;
    private final boolean mIsTouchHeadDown = SystemPropertiesEx.getBoolean("ro.config.touch.head_down", true);
    /* access modifiers changed from: private */
    public boolean mIsUseSensorFeature = false;
    private boolean mIsViewAttached = false;
    private ProximitySensorListener mListener;
    private BroadcastReceiver mLocaleChangeReceiver = new BroadcastReceiver() {
        /* class huawei.com.android.server.policy.HwScreenOnProximityLock.AnonymousClass4 */

        public void onReceive(Context context, Intent intent) {
            Log.i(HwScreenOnProximityLock.TAG, "on receive localeChangeReceiver");
            boolean unused = HwScreenOnProximityLock.this.mIsNeedRefresh = true;
        }
    };
    private WindowManager.LayoutParams mParams = null;
    /* access modifiers changed from: private */
    public HwPhoneWindowManager mPhoneWindowManager;
    /* access modifiers changed from: private */
    public float mProximityThreshold;
    /* access modifiers changed from: private */
    public HwScreenOnProximityLayout mProximityView = null;
    private int[] mReleaseReasons = {0, 1, 3};
    private int mRotation = 0;
    /* access modifiers changed from: private */
    public SensorManager mSensorManager;
    private SystemSensorManager mSystemSensorManager;
    private BroadcastReceiver mUserSwitchReceiver = new BroadcastReceiver() {
        /* class huawei.com.android.server.policy.HwScreenOnProximityLock.AnonymousClass5 */

        public void onReceive(Context context, Intent intent) {
            Log.i(HwScreenOnProximityLock.TAG, "on receive userSwitchReceiver");
            boolean unused = HwScreenOnProximityLock.this.mIsNeedRefresh = true;
        }
    };
    private WindowManager mWindowManager;

    public HwScreenOnProximityLock(Context context, HwPhoneWindowManager phoneWindowManager, WindowManagerPolicy.WindowManagerFuncs windowFuncs, Handler handler) {
        if (context == null) {
            Log.w(TAG, "HwScreenOnProximityLock context is null");
            return;
        }
        this.mContext = context;
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mCoverManager = new CoverManager();
        this.mPhoneWindowManager = phoneWindowManager;
        this.mHwExtDeviceManager = HWExtDeviceManager.getInstance(this.mContext);
        this.mHwExtMotion = new HWExtMotion((int) DeviceStatusConstant.TYPE_HEAD_DOWN);
        this.mListener = new ProximitySensorListener();
        if (this.mIsProximityTop) {
            this.mAccListener = new AccSensorListener();
        }
        this.mHandler = handler;
        this.mHasNotchInScreen = !TextUtils.isEmpty(HW_NOTCH_SIZE);
        init();
        this.mSystemSensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
        this.mIsSupportSensorFeature = this.mSystemSensorManager.supportSensorFeature((int) SENSOR_FEATURE);
        Settings.System.putInt(this.mContext.getContentResolver(), "support_sensor_feature", this.mIsSupportSensorFeature ? 1 : 0);
    }

    private void init() {
        registerBroadcastReceiver();
        registerContentObserver();
    }

    private void registerBroadcastReceiver() {
        this.mContext.registerReceiverAsUser(this.mApsResolutionChangeReceiver, UserHandle.ALL, new IntentFilter(APS_RESOLUTION_CHANGE_ACTION), APS_RESOLUTION_CHANGE_PERSISSIONS, null);
        this.mContext.registerReceiverAsUser(this.mLocaleChangeReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.LOCALE_CHANGED"), null, null);
        this.mContext.registerReceiverAsUser(this.mUserSwitchReceiver, UserHandle.ALL, new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED), null, null);
    }

    private void registerContentObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("font_scale"), true, this.mFontScaleObserver);
    }

    public void registerDeviceListener() {
        if (!this.mIsTouchHeadDown) {
            Log.w(TAG, "mIsTouchHeadDown is false,no need to registerDeviceListener");
        } else {
            this.mHandler.postAtFrontOfQueue(new Runnable() {
                /* class huawei.com.android.server.policy.$$Lambda$HwScreenOnProximityLock$b0HgKB67Sd5ouvSdxuM9VxgEtVw */

                public final void run() {
                    HwScreenOnProximityLock.this.lambda$registerDeviceListener$0$HwScreenOnProximityLock();
                }
            });
        }
    }

    public /* synthetic */ void lambda$registerDeviceListener$0$HwScreenOnProximityLock() {
        if (this.mIsDeviceHeld) {
            Log.w(TAG, "mIsDeviceHeld is true,register return");
            return;
        }
        HWExtDeviceManager hWExtDeviceManager = this.mHwExtDeviceManager;
        if (hWExtDeviceManager == null) {
            Log.e(TAG, "mHwExtDeviceManager is null,register return");
            return;
        }
        this.mIsDeviceHeld = hWExtDeviceManager.registerDeviceListener(this.mHwExtDeviceListener, this.mHwExtMotion, this.mHandler);
        if (this.mIsDeviceHeld) {
            Log.i(TAG, "registerDeviceListener succeed");
        } else {
            Log.w(TAG, "registerDeviceListener fail");
        }
    }

    public void unregisterDeviceListener() {
        this.mHandler.post(new Runnable() {
            /* class huawei.com.android.server.policy.$$Lambda$HwScreenOnProximityLock$gJEIfTMEZVrMEmzaGjn_QhSVvVw */

            public final void run() {
                HwScreenOnProximityLock.this.lambda$unregisterDeviceListener$1$HwScreenOnProximityLock();
            }
        });
    }

    public /* synthetic */ void lambda$unregisterDeviceListener$1$HwScreenOnProximityLock() {
        if (!this.mIsDeviceHeld) {
            Log.w(TAG, "mIsDeviceHeld is false,unregister return");
            return;
        }
        HWExtDeviceManager hWExtDeviceManager = this.mHwExtDeviceManager;
        if (hWExtDeviceManager == null) {
            Log.e(TAG, "mHwExtDeviceManager is null,unregister return");
            return;
        }
        hWExtDeviceManager.unregisterDeviceListener(this.mHwExtDeviceListener, this.mHwExtMotion);
        Log.i(TAG, "unregisterDeviceListener succeed");
        this.mIsDeviceHeld = false;
        this.mHeadDownStatus = 2;
    }

    private void registerProximityListener() {
        if (this.mIsProximityHeld) {
            Log.w(TAG, "mIsProximityHeld is true,registerProximityListener return");
            return;
        }
        Sensor sensor = this.mSensorManager.getDefaultSensor(8);
        if (sensor == null) {
            Log.w(TAG, "registerProximityListener return because of proximity sensor is not existed");
            return;
        }
        float maxRange = sensor.getMaximumRange();
        if (maxRange >= 5.0f) {
            this.mProximityThreshold = 5.0f;
        } else if (maxRange < 0.0f) {
            this.mProximityThreshold = 5.0f;
        } else {
            this.mProximityThreshold = maxRange;
        }
        this.mIsProximityHeld = this.mSensorManager.registerListener(this.mListener, sensor, 3, this.mHandler);
        if (this.mIsProximityHeld) {
            Log.i(TAG, "registerProximityListener success");
            AccSensorListener accSensorListener = this.mAccListener;
            if (accSensorListener != null) {
                accSensorListener.register();
                return;
            }
            return;
        }
        Log.w(TAG, "registerProximityListener fail");
    }

    private boolean shouldReleaseProximity(int reason) {
        int len = this.mReleaseReasons.length;
        for (int i = 0; i < len; i++) {
            if (reason == this.mReleaseReasons[i]) {
                return true;
            }
        }
        boolean isKeyguardLocked = this.mPhoneWindowManager.isKeyguardLocked();
        Log.i(TAG, "isKeyguardLocked:" + isKeyguardLocked);
        return !isKeyguardLocked;
    }

    private boolean shouldRegisterProximity(int mode) {
        if (!IS_FOLDABLE) {
            return true;
        }
        if (mode == 0 || mode == 3) {
            return false;
        }
        return true;
    }

    public void acquireLock(WindowManagerPolicy policy, int mode) {
        this.mHandler.postAtFrontOfQueue(new Runnable(policy, mode) {
            /* class huawei.com.android.server.policy.$$Lambda$HwScreenOnProximityLock$tjD4nHi5OGBDXdFe7rOhC54xArE */
            private final /* synthetic */ WindowManagerPolicy f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                HwScreenOnProximityLock.this.lambda$acquireLock$2$HwScreenOnProximityLock(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$acquireLock$2$HwScreenOnProximityLock(WindowManagerPolicy policy, int mode) {
        if (policy == null) {
            Log.w(TAG, "acquire Lock: return because get Window Manager policy is null");
            return;
        }
        if (shouldRegisterProximity(mode)) {
            registerProximityListener();
        }
        if (!this.mPhoneWindowManager.isKeyguardLocked()) {
            Log.i(TAG, "keyguard not locked");
        }
    }

    public void releaseLock(int reason) {
        this.mHandler.postAtFrontOfQueue(new Runnable(reason) {
            /* class huawei.com.android.server.policy.$$Lambda$HwScreenOnProximityLock$AJkzApm46Pr9c2qkiS87jeIFHyI */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwScreenOnProximityLock.this.lambda$releaseLock$3$HwScreenOnProximityLock(this.f$1);
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: releaseLockInternal */
    public void lambda$releaseLock$3$HwScreenOnProximityLock(int reason) {
        Log.i(TAG, "releaseLock,reason:" + reason);
        if (!this.mIsProximityHeld) {
            Log.w(TAG, "releaseLock: return because sensor listener is held = " + this.mIsProximityHeld);
            return;
        }
        BallFrameView ballFrameView = this.mFrameView;
        if (ballFrameView != null) {
            ballFrameView.updateAnimCount(0);
        }
        removeProximityView();
        if (shouldReleaseProximity(reason)) {
            this.mIsProximityHeld = false;
            this.mSensorManager.unregisterListener(this.mListener);
            Log.i(TAG, "unregister proximity sensor listener");
            AccSensorListener accSensorListener = this.mAccListener;
            if (accSensorListener != null) {
                accSensorListener.unregister();
            }
        }
    }

    public boolean isShowing() {
        return this.mProximityView != null && this.mIsViewAttached;
    }

    public void forceShowHint() {
        this.mHandler.post(new Runnable() {
            /* class huawei.com.android.server.policy.$$Lambda$HwScreenOnProximityLock$g2wRLmFtvIIGw_NvqZL9hW8jxec */

            public final void run() {
                HwScreenOnProximityLock.this.lambda$forceShowHint$4$HwScreenOnProximityLock();
            }
        });
    }

    /* access modifiers changed from: private */
    public void prepareProximityView() {
        View notchView;
        removeProximityView();
        Log.i(TAG, "inflate View, config : " + this.mContext.getResources().getConfiguration());
        boolean isFoldFullScreen = IS_FOLDABLE && HwFoldScreenManagerEx.getDisplayMode() == 1;
        initProximityView(isFoldFullScreen);
        HwScreenOnProximityLayout hwScreenOnProximityLayout = this.mProximityView;
        if (hwScreenOnProximityLayout == null) {
            Log.e(TAG, "initProximityView null");
            return;
        }
        if (this.mHasNotchInScreen && (notchView = hwScreenOnProximityLayout.findViewById(34603058)) != null) {
            ViewGroup.LayoutParams notchParam = notchView.getLayoutParams();
            LinearLayout.LayoutParams params = null;
            if (notchParam instanceof LinearLayout.LayoutParams) {
                params = (LinearLayout.LayoutParams) notchParam;
            }
            if (params != null) {
                params.height = HwNotchSizeUtil.getNotchSize()[1];
                notchView.setLayoutParams(params);
            } else {
                Log.e(TAG, "prepareProximityView params is null");
            }
        }
        setHintLayout(isFoldFullScreen);
        setBottomView();
        setProximityViewParam();
        Log.i(TAG, "prepareProximityView addView ");
    }

    private void initProximityView(boolean isFoldFullScreen) {
        View view;
        int height;
        if (!isLandScape() || isFoldFullScreen) {
            view = View.inflate(this.mContext, 34013254, null);
            View hintView = view.findViewById(34603057);
            LinearLayout ll = null;
            if (hintView instanceof LinearLayout) {
                ll = (LinearLayout) hintView;
            }
            if (ll == null) {
                Log.e(TAG, "initProximityView ll is null");
                return;
            }
            DisplayMetrics dm = new DisplayMetrics();
            this.mWindowManager.getDefaultDisplay().getMetrics(dm);
            int width = dm.widthPixels;
            if (isFoldFullScreen) {
                height = (width * 5) / 8;
            } else {
                height = (width * 5) / 4;
            }
            ll.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        } else {
            view = View.inflate(this.mContext, 34013424, null);
        }
        if (view == null) {
            Log.e(TAG, "view is null");
        } else if (!(view instanceof HwScreenOnProximityLayout)) {
            Log.e(TAG, "view not instanceof HwScreenOnProximityLayout");
        } else {
            this.mProximityView = (HwScreenOnProximityLayout) view;
            this.mHintView = this.mProximityView.findViewById(34603159);
            this.mProximityView.setEventListener(new HwScreenOnProximityLayout.EventListener() {
                /* class huawei.com.android.server.policy.HwScreenOnProximityLock.AnonymousClass6 */

                @Override // com.android.server.policy.HwScreenOnProximityLayout.EventListener
                public void onDownEvent(MotionEvent event) {
                    HwScreenOnProximityLock.this.lambda$forceShowHint$4$HwScreenOnProximityLock();
                    if (HwScreenOnProximityLock.this.mFrameView != null) {
                        HwScreenOnProximityLock.this.mFrameView.startTextViewAnimal();
                    }
                }
            });
        }
    }

    private void setProximityViewParam() {
        this.mParams = new WindowManager.LayoutParams(-1, -1, 2100, HwWindowManager.LayoutParams.FLAG_CAPTURE_KNUCKLES | 1280 | 134217728, -2);
        this.mParams.inputFeatures |= 4;
        this.mParams.privateFlags |= -2147483632;
        this.mParams.setTitle(PROXIMITY_WND_NAME);
        if (this.mHasNotchInScreen) {
            this.mParams.layoutInDisplayCutoutMode = 1;
        }
        if (!"".equals(HW_CURVED_SIDE)) {
            new WindowManagerEx.LayoutParamsEx(this.mParams).setDisplaySideMode(1);
        }
    }

    private void setHintLayout(boolean isFoldFullScreen) {
        View hintView = this.mProximityView.findViewById(34603394);
        LinearLayout hintLayout = null;
        if (hintView instanceof LinearLayout) {
            hintLayout = (LinearLayout) hintView;
        }
        if (hintLayout != null) {
            hintLayout.setOnTouchListener(new View.OnTouchListener() {
                /* class huawei.com.android.server.policy.HwScreenOnProximityLock.AnonymousClass7 */

                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            if (isFoldFullScreen) {
                hintLayout.setPadding(hintLayout.getPaddingLeft(), (int) this.mContext.getResources().getDimension(34472498), hintLayout.getPaddingRight(), hintLayout.getPaddingBottom());
            }
        }
    }

    private void setBottomView() {
        View bottomView = this.mProximityView.findViewById(34603393);
        if (bottomView != null) {
            bottomView.setOnTouchListener(new View.OnTouchListener() {
                /* class huawei.com.android.server.policy.HwScreenOnProximityLock.AnonymousClass8 */

                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
    }

    private boolean isLandScape() {
        int i = this.mRotation;
        return i == 1 || i == 3;
    }

    /* access modifiers changed from: private */
    /* renamed from: showHintView */
    public void lambda$forceShowHint$4$HwScreenOnProximityLock() {
        View view;
        if (this.mProximityView != null && (view = this.mHintView) != null && view.getVisibility() != 0 && this.mIsViewAttached) {
            this.mHintView.setVisibility(0);
            long token = Binder.clearCallingIdentity();
            try {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "proximity_wnd_status", 1, ActivityManager.getCurrentUser());
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    /* access modifiers changed from: private */
    public void addProximityView() {
        CoverManager coverManager;
        int i;
        int i2 = 0;
        if (this.mIsNeedRefresh) {
            prepareProximityView();
            this.mIsNeedRefresh = false;
        }
        if (this.mProximityView == null || this.mPhoneWindowManager.isKeyguardShortcutApps() || (this.mPhoneWindowManager.isLandscape() && this.mPhoneWindowManager.isLsKeyguardShortcutApps())) {
            Log.i(TAG, "no need to addProximityView");
        } else if (this.mIsTouchHeadDown && this.mHeadDownStatus != 1 && ((i = this.mRotation) == 1 || i == 3)) {
            Log.i(TAG, "no need to addProximityView when screen is horizontal not headdown");
        } else if (this.mProximityView != null && this.mParams != null && !this.mIsViewAttached && this.mIsProximityHeld && (coverManager = this.mCoverManager) != null && coverManager.isCoverOpen()) {
            AccSensorListener accSensorListener = this.mAccListener;
            if (accSensorListener == null || !accSensorListener.isFlat()) {
                writePocketMode(ROOT_CAUSE_RESOLVE_PATTERN_LOG, POCKET_IN);
                Log.i(TAG, "addProximityView ");
                this.mWindowManager.addView(this.mProximityView, this.mParams);
                this.mFrameView = (BallFrameView) this.mProximityView.findViewById(34603044);
                BallFrameView ballFrameView = this.mFrameView;
                if (ballFrameView != null) {
                    ballFrameView.setBallViewVisibal(0);
                    this.mFrameView.updateStart(0);
                }
                restoreHintTextView();
                View view = this.mHintView;
                if (view != null) {
                    if (PROXIMITY_ENABLE) {
                        i2 = 8;
                    }
                    view.setVisibility(i2);
                    this.mIsViewAttached = true;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void removeProximityView() {
        HwScreenOnProximityLayout hwScreenOnProximityLayout = this.mProximityView;
        if (hwScreenOnProximityLayout != null && this.mIsViewAttached) {
            ViewParent vp = hwScreenOnProximityLayout.getParent();
            if (vp == null) {
                writePocketMode(ROOT_CAUSE_RESOLVE_PATTERN_LOG, POCKET_OUT);
                this.mWindowManager.removeViewImmediate(this.mProximityView);
                this.mIsViewAttached = false;
                Log.i(TAG, "removeview directly ");
            } else if (this.mWindowManager == null || !(vp instanceof ViewRootImpl)) {
                Log.w(TAG, "removeView fail: mWindowManager = " + this.mWindowManager + ", viewparent = " + vp);
            } else {
                writePocketMode(ROOT_CAUSE_RESOLVE_PATTERN_LOG, POCKET_OUT);
                Log.i(TAG, "removeProximityView success vp " + vp);
                this.mWindowManager.removeViewImmediate(this.mProximityView);
                this.mIsViewAttached = false;
            }
            long token = Binder.clearCallingIdentity();
            try {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "proximity_wnd_status", 0, ActivityManager.getCurrentUser());
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public void refreshForRotationChange(int rotation) {
        this.mHandler.post(new Runnable(rotation) {
            /* class huawei.com.android.server.policy.$$Lambda$HwScreenOnProximityLock$rUS7ibjgSumdbsq3xHqMlfAdsc */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwScreenOnProximityLock.this.lambda$refreshForRotationChange$5$HwScreenOnProximityLock(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$refreshForRotationChange$5$HwScreenOnProximityLock(int rotation) {
        this.mRotation = rotation;
        lambda$forceRefreshHintView$6$HwScreenOnProximityLock();
    }

    public void forceRefreshHintView() {
        this.mHandler.post(new Runnable() {
            /* class huawei.com.android.server.policy.$$Lambda$HwScreenOnProximityLock$jDga2RdRKGH2p0aaS62ljpUagCI */

            public final void run() {
                HwScreenOnProximityLock.this.lambda$forceRefreshHintView$6$HwScreenOnProximityLock();
            }
        });
    }

    public void refreshHintTextView() {
        TextView animText = null;
        View animView = this.mProximityView.findViewById(34603022);
        if (animView instanceof TextView) {
            animText = (TextView) animView;
        }
        if (animText == null) {
            Log.e(TAG, "refreshHintTextView animText is null");
            return;
        }
        String swipeText = this.mContext.getResources().getString(33686013);
        animText.setText(swipeText);
        Log.i(TAG, "refreshHintTextView: " + swipeText);
    }

    public void restoreHintTextView() {
        TextView animText = null;
        View animView = this.mProximityView.findViewById(34603022);
        if (animView instanceof TextView) {
            animText = (TextView) animView;
        }
        if (animText == null) {
            Log.e(TAG, "restoreHintTextView animText is null");
            return;
        }
        String swipeText = this.mContext.getResources().getString(33686012);
        animText.setText(swipeText);
        Log.i(TAG, "restoreHintTextView: " + swipeText);
    }

    public void swipeExitHintView() {
        releaseLock(0);
    }

    /* access modifiers changed from: private */
    /* renamed from: refreshHintViewInternal */
    public void lambda$forceRefreshHintView$6$HwScreenOnProximityLock() {
        View view = this.mHintView;
        if (view != null) {
            boolean shouldShow = true;
            this.mIsNeedRefresh = true;
            if (view.getVisibility() != 0) {
                shouldShow = false;
            }
            if (this.mIsProximityHeld && this.mIsProximity) {
                addProximityView();
                if (shouldShow) {
                    lambda$forceShowHint$4$HwScreenOnProximityLock();
                }
            }
        }
    }

    private void writePocketMode(String destPath, String content) {
        FileWriter fw;
        FileWriter fw2 = null;
        try {
            if (getFileSize(destPath) < ROOT_CAUSE_RESOLVE_PATTERN_PATTERN_FILE_SIZE) {
                fw = new FileWriter(destPath, true);
            } else {
                fw = new FileWriter(destPath);
            }
            String fileContent = "time [" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "]" + content + System.lineSeparator();
            fw.write(fileContent, 0, fileContent.length());
            fw.flush();
            Log.i(TAG, "Write" + fileContent + "success.");
            try {
                fw.close();
            } catch (IOException e) {
                Log.e(TAG, "close fw error");
            }
        } catch (IOException e2) {
            Log.e(TAG, "Failed to write" + content);
            if (0 != 0) {
                fw2.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fw2.close();
                } catch (IOException e3) {
                    Log.e(TAG, "close fw error");
                }
            }
            throw th;
        }
    }

    private int getFileSize(String path) {
        File file = new File(path);
        if (file.exists()) {
            return (int) file.length();
        }
        return 0;
    }

    private class AccSensorListener implements SensorEventListener {
        private boolean mIsFlat;
        private boolean mIsListening;

        private AccSensorListener() {
            this.mIsListening = false;
            this.mIsFlat = false;
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            if (event != null && event.values != null && event.values.length >= 3) {
                boolean z = false;
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];
                float sqrt = (float) Math.sqrt((double) ((float) (Math.pow((double) axisX, 2.0d) + Math.pow((double) axisY, 2.0d) + Math.pow((double) axisZ, 2.0d))));
                float pitch = (float) (-Math.asin((double) (axisY / sqrt)));
                float roll = (float) Math.asin((double) (axisX / sqrt));
                if (((double) pitch) >= -0.8726646491148832d && ((double) pitch) <= 0.2617993877991494d && ((double) Math.abs(roll)) <= 0.5235987755982988d && axisZ >= 5.0f) {
                    z = true;
                }
                this.mIsFlat = z;
                if (this.mIsFlat) {
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
            Log.d(HwScreenOnProximityLock.TAG, "AccSensorListener sensortype " + sensor.getType());
            if (this.mIsListening) {
                Log.w(HwScreenOnProximityLock.TAG, "AccSensorListener already register");
                return;
            }
            Log.i(HwScreenOnProximityLock.TAG, "AccSensorListener register");
            this.mIsListening = HwScreenOnProximityLock.this.mSensorManager.registerListener(this, sensor, 3, HwScreenOnProximityLock.this.mHandler);
        }

        public void unregister() {
            if (!this.mIsListening) {
                Log.w(HwScreenOnProximityLock.TAG, "AccSensorListener not register yet");
                return;
            }
            Log.i(HwScreenOnProximityLock.TAG, "AccSensorListener unregister");
            HwScreenOnProximityLock.this.mSensorManager.unregisterListener(this);
            this.mIsListening = false;
        }

        public boolean isFlat() {
            Log.i(HwScreenOnProximityLock.TAG, "AccSensorListener mIsFlat " + this.mIsFlat);
            return this.mIsFlat;
        }
    }

    private class ProximitySensorListener implements SensorEventListener {
        private ProximitySensorListener() {
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            boolean z = false;
            float d1 = event.values[0];
            int d2 = 0;
            if (event.values.length >= 3) {
                d2 = (((int) event.values[2]) >> 8) & 1;
            }
            HwScreenOnProximityLock hwScreenOnProximityLock = HwScreenOnProximityLock.this;
            boolean unused = hwScreenOnProximityLock.mIsProximityGen = d1 >= 0.0f && d1 < hwScreenOnProximityLock.mProximityThreshold;
            boolean unused2 = HwScreenOnProximityLock.this.mIsProximityDyn = d2 == 1;
            HwScreenOnProximityLock hwScreenOnProximityLock2 = HwScreenOnProximityLock.this;
            if (hwScreenOnProximityLock2.mIsSupportSensorFeature && !HwScreenOnProximityLock.this.mIsProximityGen && HwScreenOnProximityLock.this.mHeadDownStatus == 1 && HwScreenOnProximityLock.this.mPhoneWindowManager.isKeyguardLocked()) {
                z = true;
            }
            boolean unused3 = hwScreenOnProximityLock2.mIsUseSensorFeature = z;
            Log.d(HwScreenOnProximityLock.TAG, "mIsSupportSensorFeature:" + HwScreenOnProximityLock.this.mIsSupportSensorFeature + ",mHeadDownStatus:" + HwScreenOnProximityLock.this.mHeadDownStatus + ",mIsUseSensorFeature:" + HwScreenOnProximityLock.this.mIsUseSensorFeature);
            HwScreenOnProximityLock hwScreenOnProximityLock3 = HwScreenOnProximityLock.this;
            boolean unused4 = hwScreenOnProximityLock3.mIsProximity = hwScreenOnProximityLock3.mIsUseSensorFeature ? HwScreenOnProximityLock.this.mIsProximityDyn : HwScreenOnProximityLock.this.mIsProximityGen;
            handleSensorChanges();
        }

        private void handleSensorChanges() {
            if (HwScreenOnProximityLock.this.mCoverManager == null) {
                Log.e(HwScreenOnProximityLock.TAG, "mCoverManager is null");
                return;
            }
            boolean isCoverOpen = HwScreenOnProximityLock.this.mCoverManager.isCoverOpen();
            Log.i(HwScreenOnProximityLock.TAG, "handleSensorChanged: close to sensor: " + HwScreenOnProximityLock.this.mIsProximity + ", isCoverOpen: " + isCoverOpen);
            if (HwScreenOnProximityLock.this.mIsProximity && isCoverOpen) {
                HwScreenOnProximityLock.this.addProximityView();
            } else if (isCoverOpen || HwScreenOnProximityLock.this.mProximityView != null) {
                HwScreenOnProximityLock.this.lambda$releaseLock$3$HwScreenOnProximityLock(2);
            } else {
                Log.i(HwScreenOnProximityLock.TAG, "no need to releaseLock");
            }
        }
    }
}
