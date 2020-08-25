package com.android.server.input;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.freeform.HwFreeFormUtils;
import android.os.Binder;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.view.IInputFilter;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.PointerIcon;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavConst;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.rms.iaware.sysload.SysLoadManager;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.WindowState;
import com.huawei.android.gameassist.HwGameAssistGamePad;
import com.huawei.android.pgmng.plug.PowerKit;
import com.huawei.android.view.HwWindowManager;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.server.security.behaviorcollect.BehaviorCollector;
import huawei.android.os.HwGeneralManager;
import huawei.cust.HwCustUtils;
import java.util.List;

public class HwInputManagerService extends InputManagerService {
    private static final String APP_LEFT = "pressure_launch_app_left";
    private static final String APP_RIGHT = "pressure_launch_app_right";
    static final String FINGERPRINT_ANSWER_CALL = "fp_answer_call";
    static final String FINGERPRINT_BACK_TO_HOME = "fp_return_desk";
    static final String FINGERPRINT_CAMERA_SWITCH = "fp_take_photo";
    static final String FINGERPRINT_GALLERY_SLIDE = "fingerprint_gallery_slide";
    static final String FINGERPRINT_GO_BACK = "fp_go_back";
    static final String FINGERPRINT_LOCK_DEVICE = "fp_lock_device";
    static final String FINGERPRINT_MARKET_DEMO_SWITCH = "fingerprint_market_demo_switch";
    static final String FINGERPRINT_RECENT_APP = "fp_recent_application";
    static final String FINGERPRINT_SHOW_NOTIFICATION = "fp_show_notification";
    static final String FINGERPRINT_SLIDE_SWITCH = "fingerprint_slide_switch";
    static final String FINGERPRINT_STOP_ALARM = "fp_stop_alarm";
    private static final String FINGER_PRESS_NVI = "persist.sys.fingerpressnavi";
    private static final String FINGER_PRESS_NVI_DISABLE = "0";
    private static final String FINGER_PRESS_NVI_ENABLE = "1";
    /* access modifiers changed from: private */
    public static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    static final boolean IS_ENABLE_BACK_TO_HOME = true;
    static final boolean IS_ENABLE_LOCK_DEVICE = false;
    private static final boolean IS_FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    private static final int MOUSE_EVENT = 8194;
    private static final int POWERKIT_TRY_INTERVAL = 5000;
    private static final int POWERKIT_TRY_MAX_TIMES = 10;
    private static final String TAGPRESSURE = "pressure:hwInputMS";
    private boolean isSupportPg = true;
    /* access modifiers changed from: private */
    public boolean isSupportPressure = false;
    private ContentObserver mAppLeftStartObserver;
    private ContentObserver mAppRightStartObserver;
    int mCurrentUserId = 0;
    HwCustInputManagerService mCust;
    private Context mExternalContext = null;
    FingerPressNavigation mFingerPressNavi = null;
    private FingerprintNavigation mFingerprintNavigationFilter;
    private HwFingersSnapshooter mFingersSnapshooter = null;
    private Handler mHandler;
    private int mHeight;
    boolean mIsAnswerCall = false;
    boolean mIsBackToHome = false;
    private boolean mIsEnableFingerSnapshot = false;
    boolean mIsFpMarketDemoSwitchOn = false;
    boolean mIsGallerySlide;
    boolean mIsGoBack = false;
    boolean mIsInputDeviceChanged = false;
    private boolean mIsKeepCustomPointerIcon = false;
    private boolean mIsLastImmersiveMode = false;
    private boolean mIsPressNaviEnable = false;
    boolean mIsRecentApp = false;
    boolean mIsShowNotification = false;
    boolean mIsStartInputEventControl = false;
    boolean mIsStopAlarm = false;
    private ContentObserver mMinNaviObserver;
    private ContentObserver mNaviBarPosObserver;
    private WindowManagerPolicy mPolicy = null;
    /* access modifiers changed from: private */
    public PowerKit mPowerKit;
    private ContentObserver mPressLimitObserver;
    private ContentObserver mPressNaviObserver;
    /* access modifiers changed from: private */
    public final ContentResolver mResolver;
    final SettingsObserver mSettingsObserver;
    /* access modifiers changed from: private */
    public PowerKit.Sink mStateRecognitionListener;
    private HwTripleFingersFreeForm mTripleFingersFreeForm = null;
    private int mWidth;
    private String needTip = "pressure_needTip";

    public HwInputManagerService(Context context, Handler handler) {
        super(context);
        HwGameAssistGamePad.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(handler);
        this.mHandler = handler;
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        updateFingerprintSlideSwitchValue(this.mCurrentUserId);
        LocalServices.addService(HwInputManagerLocalService.class, new HwInputManagerLocalService());
    }

    private boolean isPressureModeOpenForChina() {
        return Settings.System.getIntForUser(this.mResolver, "virtual_notification_key_type", 0, this.mCurrentUserId) == 1;
    }

    private boolean isPressureModeOpenForOther() {
        return Settings.System.getIntForUser(this.mResolver, "virtual_notification_key_type", 0, this.mCurrentUserId) == 1;
    }

    private boolean isNavigationBarMin() {
        return Settings.Global.getInt(this.mResolver, "navigationbar_is_min", 0) == 1;
    }

    private void initNaviObserver(Handler handler) {
        this.mPressNaviObserver = new ContentObserver(handler) {
            /* class com.android.server.input.HwInputManagerService.AnonymousClass1 */

            public void onChange(boolean isSelfChange) {
                HwInputManagerService.this.handleNaviChangeForTip();
                if (HwInputManagerService.IS_CHINA_AREA) {
                    HwInputManagerService.this.handleNaviChangeForChina();
                } else {
                    HwInputManagerService.this.handleNaviChangeForOther();
                }
            }
        };
        this.mMinNaviObserver = new ContentObserver(handler) {
            /* class com.android.server.input.HwInputManagerService.AnonymousClass2 */

            public void onChange(boolean isSelfChange) {
                if (HwInputManagerService.IS_CHINA_AREA) {
                    HwInputManagerService.this.handleNaviChangeForChina();
                }
            }
        };
        this.mNaviBarPosObserver = new ContentObserver(handler) {
            /* class com.android.server.input.HwInputManagerService.AnonymousClass3 */

            public void onChange(boolean isSelfChange) {
                int naviBarPos = Settings.System.getIntForUser(HwInputManagerService.this.mContext.getContentResolver(), "virtual_key_type", 0, ActivityManager.getCurrentUser());
                Slog.d(HwInputManagerService.TAGPRESSURE, "mPressNaviObserver onChange open naviBarPos = " + naviBarPos);
                if (HwInputManagerService.this.mFingerPressNavi != null) {
                    HwInputManagerService.this.mFingerPressNavi.setNaviBarPosition(naviBarPos);
                }
            }
        };
    }

    private void initAppObserver(Handler handler) {
        this.mAppLeftStartObserver = new ContentObserver(handler) {
            /* class com.android.server.input.HwInputManagerService.AnonymousClass4 */

            public void onChange(boolean isSelfChange) {
                String appLeftPkg = Settings.System.getStringForUser(HwInputManagerService.this.mContext.getContentResolver(), HwInputManagerService.APP_LEFT, ActivityManager.getCurrentUser());
                Slog.d(HwInputManagerService.TAGPRESSURE, "AppStartObserver onChange open appLeftPkg = " + appLeftPkg);
                if (HwInputManagerService.this.mFingerPressNavi != null && appLeftPkg != null) {
                    HwInputManagerService.this.mFingerPressNavi.setAppLeftPkg(appLeftPkg);
                }
            }
        };
        this.mAppRightStartObserver = new ContentObserver(handler) {
            /* class com.android.server.input.HwInputManagerService.AnonymousClass5 */

            public void onChange(boolean isSelfChange) {
                String appRightPkg = Settings.System.getStringForUser(HwInputManagerService.this.mContext.getContentResolver(), HwInputManagerService.APP_RIGHT, ActivityManager.getCurrentUser());
                Slog.d(HwInputManagerService.TAGPRESSURE, "AppStartObserver onChange open appRightPkg = " + appRightPkg);
                if (HwInputManagerService.this.mFingerPressNavi != null && appRightPkg != null) {
                    HwInputManagerService.this.mFingerPressNavi.setAppRightPkg(appRightPkg);
                }
            }
        };
    }

    private void initObserver(Handler handler) {
        this.mPressLimitObserver = new ContentObserver(handler) {
            /* class com.android.server.input.HwInputManagerService.AnonymousClass6 */

            public void onChange(boolean isSelfChange) {
                float limit = Settings.System.getFloatForUser(HwInputManagerService.this.mContext.getContentResolver(), "pressure_habit_threshold", 0.2f, ActivityManager.getCurrentUser());
                Slog.d(HwInputManagerService.TAGPRESSURE, "mPressLimitObserver onChange open limit = " + limit);
                if (HwInputManagerService.this.mFingerPressNavi != null) {
                    HwInputManagerService.this.mFingerPressNavi.setPressureLimit(limit);
                }
            }
        };
        initNaviObserver(handler);
        initAppObserver(handler);
        registerInputManagerObserver();
    }

    private void registerInputManagerObserver() {
        this.mResolver.registerContentObserver(Settings.System.getUriFor("virtual_notification_key_type"), false, this.mPressNaviObserver, -1);
        this.mResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_is_min"), false, this.mMinNaviObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("pressure_habit_threshold"), false, this.mPressLimitObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("virtual_key_type"), false, this.mNaviBarPosObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(APP_LEFT), false, this.mAppLeftStartObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(APP_RIGHT), false, this.mAppRightStartObserver, -1);
    }

    /* access modifiers changed from: private */
    public void handleNaviChangeForChina() {
        if (this.isSupportPressure) {
            if (!isPressureModeOpenForChina() || !isNavigationBarMin()) {
                Slog.d(TAGPRESSURE, "mPressNaviObserver onChange close");
                this.mIsPressNaviEnable = false;
                this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
                FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
                if (fingerPressNavigation != null) {
                    fingerPressNavigation.destoryPointerCircleAnimation();
                    this.mFingerPressNavi.setMode(0);
                }
                SystemProperties.set(FINGER_PRESS_NVI, "1");
            } else {
                Slog.d(TAGPRESSURE, "mPressNaviObserver onChange open");
                this.mIsPressNaviEnable = true;
                this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
                FingerPressNavigation fingerPressNavigation2 = this.mFingerPressNavi;
                if (fingerPressNavigation2 != null) {
                    fingerPressNavigation2.createPointerCircleAnimation();
                    this.mFingerPressNavi.setDisplayWidthAndHeight(this.mWidth, this.mHeight);
                    this.mFingerPressNavi.setMode(1);
                }
                SystemProperties.set(FINGER_PRESS_NVI, "1");
            }
            int naviBarPos = Settings.System.getIntForUser(this.mContext.getContentResolver(), "virtual_key_type", 0, this.mCurrentUserId);
            FingerPressNavigation fingerPressNavigation3 = this.mFingerPressNavi;
            if (fingerPressNavigation3 != null) {
                fingerPressNavigation3.setNaviBarPosition(naviBarPos);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleNaviChangeForOther() {
        if (this.isSupportPressure) {
            if (isPressureModeOpenForOther()) {
                Slog.d(TAGPRESSURE, "mPressNaviObserver onChange open");
                this.mIsPressNaviEnable = true;
                this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
                FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
                if (fingerPressNavigation != null) {
                    fingerPressNavigation.createPointerCircleAnimation();
                    this.mFingerPressNavi.setDisplayWidthAndHeight(this.mWidth, this.mHeight);
                    this.mFingerPressNavi.setMode(1);
                }
                sendBroadcast(true);
                SystemProperties.set(FINGER_PRESS_NVI, "1");
            } else {
                Slog.d(TAGPRESSURE, "mPressNaviObserver onChange close");
                this.mIsPressNaviEnable = false;
                this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
                FingerPressNavigation fingerPressNavigation2 = this.mFingerPressNavi;
                if (fingerPressNavigation2 != null) {
                    fingerPressNavigation2.destoryPointerCircleAnimation();
                    this.mFingerPressNavi.setMode(0);
                }
                sendBroadcast(false);
                SystemProperties.set(FINGER_PRESS_NVI, "1");
            }
            int naviBarPos = Settings.System.getIntForUser(this.mContext.getContentResolver(), "virtual_key_type", 0, this.mCurrentUserId);
            FingerPressNavigation fingerPressNavigation3 = this.mFingerPressNavi;
            if (fingerPressNavigation3 != null) {
                fingerPressNavigation3.setNaviBarPosition(naviBarPos);
            }
        }
    }

    private class BroadcastThread extends Thread {
        boolean mIsMinNaviBar;

        private BroadcastThread() {
        }

        public void setMiniNaviBar(boolean isMinNaviBar) {
            this.mIsMinNaviBar = isMinNaviBar;
        }

        public void run() {
            Log.d(HwInputManagerService.TAGPRESSURE, "sendBroadcast minNaviBar = " + this.mIsMinNaviBar);
            Intent intent = new Intent("com.huawei.navigationbar.statuschange");
            intent.putExtra("minNavigationBar", this.mIsMinNaviBar);
            HwInputManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void sendBroadcast(boolean isMinNaviBar) {
        BroadcastThread sendthread = new BroadcastThread();
        sendthread.setMiniNaviBar(isMinNaviBar);
        sendthread.start();
    }

    /* access modifiers changed from: private */
    public void pressureinit() {
        if (this.mIsPressNaviEnable || this.isSupportPressure) {
            this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
            FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
            if (fingerPressNavigation != null) {
                if (this.mIsPressNaviEnable) {
                    Log.d(TAGPRESSURE, "systemRunning mIsPressNaviEnable ");
                    this.mFingerPressNavi.createPointerCircleAnimation();
                    this.mFingerPressNavi.setMode(1);
                } else if (this.isSupportPressure) {
                    fingerPressNavigation.setMode(0);
                    Log.d(TAGPRESSURE, "systemRunning isSupportPressure ");
                }
                String appLeftPkg = Settings.System.getStringForUser(this.mContext.getContentResolver(), APP_LEFT, this.mCurrentUserId);
                if (appLeftPkg != null) {
                    this.mFingerPressNavi.setAppLeftPkg(appLeftPkg);
                } else {
                    this.mFingerPressNavi.setAppLeftPkg("none_app");
                }
                String appRightPkg = Settings.System.getStringForUser(this.mContext.getContentResolver(), APP_RIGHT, this.mCurrentUserId);
                if (appRightPkg != null) {
                    this.mFingerPressNavi.setAppRightPkg(appRightPkg);
                } else {
                    this.mFingerPressNavi.setAppRightPkg("none_app");
                }
                float limit = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "pressure_habit_threshold", 0.2f, this.mCurrentUserId);
                this.mFingerPressNavi.setNaviBarPosition(Settings.System.getIntForUser(this.mContext.getContentResolver(), "virtual_key_type", 0, this.mCurrentUserId));
                this.mFingerPressNavi.setPressureLimit(limit);
                SystemProperties.set(FINGER_PRESS_NVI, "1");
                return;
            }
            return;
        }
        SystemProperties.set(FINGER_PRESS_NVI, "0");
    }

    private int isNeedTip() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), this.needTip, 0, this.mCurrentUserId);
    }

    /* access modifiers changed from: private */
    public void handleNaviChangeForTip() {
        if (isPressureModeOpenForChina()) {
            Settings.System.putIntForUser(this.mContext.getContentResolver(), this.needTip, 1, this.mCurrentUserId);
            this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
            FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
            if (fingerPressNavigation != null) {
                fingerPressNavigation.setNeedTip(true);
                return;
            }
            return;
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), this.needTip, 2, this.mCurrentUserId);
        this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
        FingerPressNavigation fingerPressNavigation2 = this.mFingerPressNavi;
        if (fingerPressNavigation2 != null) {
            fingerPressNavigation2.setNeedTip(false);
        }
    }

    /* access modifiers changed from: private */
    public void pressureinitData() {
        if (this.isSupportPressure) {
            this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
            if (this.mFingerPressNavi != null) {
                int istip = isNeedTip();
                if (istip == 0) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), this.needTip, 1, this.mCurrentUserId);
                    this.mFingerPressNavi.setNeedTip(true);
                } else if (istip == 3) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), this.needTip, 2, this.mCurrentUserId);
                    this.mFingerPressNavi.setNeedTip(false);
                } else if (istip == 1) {
                    this.mFingerPressNavi.setNeedTip(true);
                } else if (istip == 2) {
                    this.mFingerPressNavi.setNeedTip(false);
                }
            }
        }
    }

    public void setDisplayWidthAndHeight(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
        if (fingerPressNavigation != null) {
            fingerPressNavigation.setDisplayWidthAndHeight(width, height);
        }
    }

    public void setCurFocusWindow(WindowState focus) {
        FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
        if (fingerPressNavigation != null) {
            fingerPressNavigation.setCurFocusWindow(focus);
        }
    }

    public void setIsTopFullScreen(boolean isTopFullScreen) {
        FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
        if (fingerPressNavigation != null) {
            fingerPressNavigation.setIsTopFullScreen(isTopFullScreen);
        }
    }

    public void setImmersiveMode(boolean isImmersiveMode) {
        if (this.isSupportPressure) {
            FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
            if (fingerPressNavigation != null) {
                fingerPressNavigation.setImmersiveMode(isImmersiveMode);
            }
            if (!IS_CHINA_AREA && !isImmersiveMode && this.mIsLastImmersiveMode) {
                Slog.d("InputManager", "setImmersiveMode has Changedi mode = " + isImmersiveMode);
                handleNaviChangeForOther();
            }
            this.mIsLastImmersiveMode = isImmersiveMode;
        }
    }

    public void systemRunning() {
        HwInputManagerService.super.systemRunning();
        this.mIsEnableFingerSnapshot = SystemProperties.getBoolean("ro.config.hw_triple_finger", false);
        if (this.mIsEnableFingerSnapshot) {
            this.mFingersSnapshooter = new HwFingersSnapshooter(this.mContext, this);
            synchronized (this.mInputFilterLock) {
                nativeSetInputFilterEnabled(this.mPtr, true);
            }
        }
        this.mTripleFingersFreeForm = new HwTripleFingersFreeForm(this.mContext, this);
        synchronized (this.mInputFilterLock) {
            nativeSetInputFilterEnabled(this.mPtr, true);
        }
        this.mFingerprintNavigationFilter = new FingerprintNavigation(this.mContext);
        this.mFingerprintNavigationFilter.systemRunning();
        this.isSupportPressure = HwGeneralManager.getInstance().isSupportForce();
        if (IS_CHINA_AREA) {
            if (this.isSupportPressure && isPressureModeOpenForChina() && isNavigationBarMin()) {
                this.mIsPressNaviEnable = true;
            }
        } else if (this.isSupportPressure && isPressureModeOpenForOther()) {
            this.mIsPressNaviEnable = true;
        }
        if (this.isSupportPressure) {
            initObserver(this.mHandler);
        }
        pressureinit();
        pressureinitData();
        if (this.isSupportPg && (this.isSupportPressure || this.mFingerPressNavi != null)) {
            initPowerKit();
        }
        AwareFakeActivityRecg.self().setInputManagerService(this);
        SysLoadManager.getInstance().setInputManagerService(this);
        this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    }

    public void start() {
        HwInputManagerService.super.start();
        this.mCust = (HwCustInputManagerService) HwCustUtils.createObj(HwCustInputManagerService.class, new Object[]{this});
        HwCustInputManagerService hwCustInputManagerService = this.mCust;
        if (hwCustInputManagerService != null) {
            hwCustInputManagerService.registerContentObserverForSetGloveMode(this.mContext);
        }
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.android.server.input.HwInputManagerService.AnonymousClass7 */

            public void onReceive(Context context, Intent intent) {
                HwInputManagerService.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                Slog.e(HwInputManagerService.TAGPRESSURE, "user switch mCurrentUserId = " + HwInputManagerService.this.mCurrentUserId);
                if (HwInputManagerService.this.isSupportPressure) {
                    HwInputManagerService.this.pressureinit();
                    HwInputManagerService.this.pressureinitData();
                    if (HwInputManagerService.IS_CHINA_AREA) {
                        HwInputManagerService.this.handleNaviChangeForChina();
                    } else {
                        HwInputManagerService.this.handleNaviChangeForOther();
                    }
                }
            }
        }, new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED), null, this.mHandler);
    }

    private void initPowerKit() {
        new Thread(new Runnable() {
            /* class com.android.server.input.HwInputManagerService.AnonymousClass8 */

            public void run() {
                int i = 0;
                while (i < 10) {
                    PowerKit unused = HwInputManagerService.this.mPowerKit = PowerKit.getInstance();
                    if (HwInputManagerService.this.mPowerKit != null) {
                        try {
                            Slog.i("InputManager", "get PowerKit instance success!");
                            PowerKit.Sink unused2 = HwInputManagerService.this.mStateRecognitionListener = new StateRecognitionListener();
                            HwInputManagerService.this.mPowerKit.enableStateEvent(HwInputManagerService.this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT);
                            HwInputManagerService.this.mPowerKit.enableStateEvent(HwInputManagerService.this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT);
                            return;
                        } catch (RemoteException e) {
                            Slog.e("InputManager", "VBR PG Exception e: initialize powerkit error!");
                            return;
                        }
                    } else {
                        Slog.i("InputManager", "get PowerKit instance failed! tryTimes:" + i);
                        SystemClock.sleep(5000);
                        i++;
                    }
                }
            }
        }).start();
    }

    private class StateRecognitionListener implements PowerKit.Sink {
        private StateRecognitionListener() {
        }

        public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
            if ((stateType != 10002 && stateType != 10011) || HwInputManagerService.this.mFingerPressNavi == null) {
                return;
            }
            if (eventType == 1) {
                HwInputManagerService.this.mFingerPressNavi.setGameScene(true);
            } else {
                HwInputManagerService.this.mFingerPressNavi.setGameScene(false);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void deliverInputDevicesChanged(InputDevice[] oldInputDevices) {
        HwInputManagerService.super.deliverInputDevicesChanged(oldInputDevices);
        if (DEBUG_HWFLOW) {
            Slog.d("InputManager", "inputdevicechanged.....");
        }
        this.mIsInputDeviceChanged = true;
    }

    /* access modifiers changed from: package-private */
    public boolean filterInputEvent(InputEvent event, int policyFlags) {
        HwFingersSnapshooter hwFingersSnapshooter;
        boolean isNeedInputEvent = true;
        if (this.mIsStartInputEventControl) {
            if (this.mIsInputDeviceChanged && event.getSource() == MOUSE_EVENT) {
                this.mIsInputDeviceChanged = false;
                if (DEBUG_HWFLOW) {
                    Slog.d("InputManager", "mouse event :" + this.mIsInputDeviceChanged);
                }
                setPointerIconTypeAndKeepImpl(1000, true);
                setPointerIconTypeAndKeepImpl(0, true);
            }
            int result = HwGameAssistGamePad.notifyInputEvent(event);
            if (result == 0) {
                return false;
            }
            if (result == 1) {
                setInputEventStrategy(false);
            }
        }
        BehaviorCollector.enableActiveTouch();
        FingerprintNavigation fingerprintNavigation = this.mFingerprintNavigationFilter;
        if (fingerprintNavigation != null && fingerprintNavigation.filterInputEvent(event, policyFlags)) {
            return false;
        }
        FingerPressNavigation fingerPressNavigation = this.mFingerPressNavi;
        if (fingerPressNavigation != null && !fingerPressNavigation.filterPressueInputEvent(event)) {
            return false;
        }
        boolean isPcMode = HwPCUtils.isPcCastModeInServer();
        if (!isPcMode && (hwFingersSnapshooter = this.mFingersSnapshooter) != null && !hwFingersSnapshooter.handleMotionEvent(event)) {
            return false;
        }
        if (!((isPcMode || this.mTripleFingersFreeForm == null || this.mPolicy == null) ? false : true) || this.mPolicy.isKeyguardLocked() || inSuperPowerSavingMode() || this.mTripleFingersFreeForm.handleMotionEvent(event)) {
            isNeedInputEvent = false;
        }
        if (isNeedInputEvent) {
            return false;
        }
        return HwInputManagerService.super.filterInputEvent(event, policyFlags);
    }

    private boolean inSuperPowerSavingMode() {
        return SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
        }

        public void registerContentObserver(int userId) {
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_CAMERA_SWITCH), false, this, userId);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_ANSWER_CALL), false, this, userId);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_SHOW_NOTIFICATION), false, this, userId);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_BACK_TO_HOME), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_STOP_ALARM), false, this, userId);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_LOCK_DEVICE), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_GO_BACK), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_RECENT_APP), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.System.getUriFor(HwInputManagerService.FINGERPRINT_MARKET_DEMO_SWITCH), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwInputManagerService.FINGERPRINT_GALLERY_SLIDE), false, this, userId);
        }

        public void onChange(boolean isSelfChange) {
            Slog.d("InputManager", "SettingDB has Changed");
            HwInputManagerService.this.updateFingerprintSlideSwitchValue();
        }
    }

    public final void updateFingerprintSlideSwitchValue() {
        updateFingerprintSlideSwitchValue(ActivityManager.getCurrentUser());
    }

    public final void updateFingerprintSlideSwitchValue(int userId) {
        Slog.d("InputManager", "ActivityManager.getCurrentUser:" + userId);
        this.mIsAnswerCall = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_ANSWER_CALL, 0, userId) != 0;
        this.mIsStopAlarm = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_STOP_ALARM, 0, userId) != 0;
        this.mIsShowNotification = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_SHOW_NOTIFICATION, 0, userId) != 0;
        this.mIsBackToHome = Settings.Secure.getInt(this.mResolver, FINGERPRINT_BACK_TO_HOME, 0) != 0;
        this.mIsGoBack = Settings.Secure.getInt(this.mResolver, FINGERPRINT_GO_BACK, 0) != 0;
        this.mIsRecentApp = Settings.Secure.getInt(this.mResolver, FINGERPRINT_RECENT_APP, 0) != 0;
        this.mIsFpMarketDemoSwitchOn = Settings.System.getInt(this.mResolver, FINGERPRINT_MARKET_DEMO_SWITCH, 0) == 1;
        this.mIsGallerySlide = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_GALLERY_SLIDE, 1, userId) != 0;
        if (this.mIsBackToHome || this.mIsGoBack || this.mIsRecentApp || this.mIsFpMarketDemoSwitchOn || IS_FRONT_FINGERPRINT_NAVIGATION) {
            Slog.d("InputManager", "open fingerprint nav->FINGERPRINT_SLIDE_SWITCH to 1 userId:" + userId);
            Settings.System.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, 1, userId);
        } else if (HwPCUtils.isPcCastModeInServer()) {
            HwPCUtils.log("InputManager", "set fingerprint_slide_switch=1 in pc mode");
            Settings.System.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, 1, userId);
        } else if (this.mIsShowNotification || this.mIsAnswerCall || this.mIsStopAlarm) {
            Slog.d("InputManager", "open fingerprint nav->FINGERPRINT_SLIDE_SWITCH to 1 userId:" + userId);
            Settings.System.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, 1, userId);
        } else {
            Slog.d("InputManager", "close fingerprint nav ->FINGERPRINT_SLIDE_SWITCH to 0 userId:" + userId);
            Settings.System.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, 0, userId);
        }
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        Slog.i("InputManager", "onUserSwitching, newUserId=" + newUserId);
        this.mSettingsObserver.registerContentObserver(newUserId);
        this.mSettingsObserver.onChange(true);
        FingerprintNavigation fingerprintNavigation = this.mFingerprintNavigationFilter;
        if (fingerprintNavigation != null) {
            fingerprintNavigation.setCurrentUser(newUserId, currentProfileIds);
        }
    }

    public Context getExternalContext() {
        return this.mExternalContext;
    }

    /* access modifiers changed from: private */
    public void setExternalContext(Context context) {
        this.mExternalContext = context;
        nativeReloadPointerIcons(this.mPtr, this.mExternalContext);
    }

    public final class HwInputManagerLocalService {
        public HwInputManagerLocalService() {
        }

        public boolean injectInputEvent(InputEvent event, int mode) {
            return HwInputManagerService.this.injectInputEventOtherScreens(event, mode);
        }

        public boolean injectInputEvent(InputEvent event, int mode, int appendPolicyFlag) {
            return HwInputManagerService.this.injectInputEventInternal(event, mode, appendPolicyFlag);
        }

        public void setExternalDisplayContext(Context context) {
            HwInputManagerService.this.setExternalContext(context);
        }

        public void setPointerIconTypeAndKeep(int iconId, boolean isKeep) {
            HwInputManagerService.this.setPointerIconTypeAndKeepImpl(iconId, isKeep);
        }

        public void setCustomPointerIconAndKeep(PointerIcon icon, boolean isKeep) {
            HwInputManagerService.this.setCustomPointerIconAndKeepImpl(icon, isKeep);
        }

        public void setMirrorLinkInputStatus(boolean isMirrorLinkStatus) {
            InputManagerService.nativeSetMirrorLinkInputStatus(HwInputManagerService.this.mPtr, isMirrorLinkStatus);
        }

        public void setKeyguardState(boolean isShowing) {
            InputManagerService.nativeSetKeyguardState(HwInputManagerService.this.mPtr, isShowing);
        }

        public void setLazyMode(int mode) {
            InputManagerService.nativeSetLazyMode(HwInputManagerService.this.mPtr, mode);
        }
    }

    public void responseTouchEvent(boolean isNeedResponseStatus) {
        nativeResponseTouchEvent(this.mPtr, isNeedResponseStatus);
    }

    public boolean injectInputEventOtherScreens(InputEvent event, int mode) {
        int displayId = HwPCUtils.getPCDisplayID();
        if (displayId != 0 && displayId != -1) {
            return injectInputEventInternal(event, mode);
        }
        Slog.i("InputManager", "not other screen found!");
        return false;
    }

    public static String getAppName(Context context, int pid) {
        ActivityManager activityManager;
        List<ActivityManager.RunningAppProcessInfo> appProcesses;
        if (pid <= 0 || context == null || (activityManager = (ActivityManager) context.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)) == null || (appProcesses = activityManager.getRunningAppProcesses()) == null || appProcesses.size() == 0) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    private boolean checkIsSystemApp() {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        if (pm.checkSignatures(Binder.getCallingUid(), Process.myUid()) != 0) {
            Slog.d("InputManager", "not SIGNATURE_MATCH ...." + Binder.getCallingUid());
            return false;
        }
        try {
            String pckName = getAppName(this.mContext, Binder.getCallingPid());
            if (pckName == null) {
                Slog.e("InputManager", "pckName is null " + Binder.getCallingPid() + " " + Process.myUid());
                return false;
            }
            ApplicationInfo info = pm.getApplicationInfo(pckName, 0);
            if (info == null || (info.flags & 1) == 0) {
                Slog.d("InputManager", "return false " + pckName);
                return false;
            }
            Slog.d("InputManager", "return true " + pckName);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e("InputManager", "isSystemApp not found app" + "" + "exception");
            return false;
        }
    }

    private void setInputFilterEnabled() {
        synchronized (this.mInputFilterLock) {
            if (!this.mIsEnableFingerSnapshot && !this.mIsStartInputEventControl && this.mInputFilter == null) {
                if (this.mTripleFingersFreeForm == null) {
                    nativeSetInputFilterEnabled(this.mPtr, false);
                }
            }
            nativeSetInputFilterEnabled(this.mPtr, true);
        }
    }

    public void setInputEventStrategy(boolean isStartInputEventControl) {
        if (checkIsSystemApp() && this.mIsStartInputEventControl != isStartInputEventControl) {
            Slog.d("InputManager", "mIsStartInputEventControl change to:" + isStartInputEventControl);
            this.mIsStartInputEventControl = isStartInputEventControl;
            if (isStartInputEventControl) {
                setPointerIconTypeAndKeepImpl(0, true);
                HwGameAssistGamePad.bindService();
            } else {
                setPointerIconTypeAndKeepImpl(1000, false);
                HwGameAssistGamePad.unbindService();
            }
            setInputFilterEnabled();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x001d  */
    public void setInputFilter(IInputFilter filter) {
        boolean isToNativeSetInput;
        HwInputManagerService.super.setInputFilter(filter);
        synchronized (this.mInputFilterLock) {
            if (filter == null) {
                if (!this.mIsEnableFingerSnapshot && !this.mIsStartInputEventControl) {
                    if (!HwFreeFormUtils.isFreeFormEnable()) {
                        isToNativeSetInput = false;
                        if (isToNativeSetInput) {
                            nativeSetInputFilterEnabled(this.mPtr, true);
                        }
                    }
                }
                isToNativeSetInput = true;
                if (isToNativeSetInput) {
                }
            }
        }
    }

    public void setPointerIconType(int iconId) {
        if ((this.mIsStartInputEventControl || HwWindowManager.hasLighterViewInPCCastMode()) && this.mIsKeepCustomPointerIcon) {
            Slog.i("InputManager", "setPointerIconType cannot change pointer icon when lighter view above.gamepad:" + this.mIsStartInputEventControl);
            return;
        }
        HwInputManagerService.super.setPointerIconType(iconId);
    }

    public void setCustomPointerIcon(PointerIcon icon) {
        if ((this.mIsStartInputEventControl || HwWindowManager.hasLighterViewInPCCastMode()) && this.mIsKeepCustomPointerIcon) {
            Slog.i("InputManager", "setCustomPointerIcon cannot change pointer icon when lighter view above.gamepad:" + this.mIsStartInputEventControl);
            return;
        }
        HwInputManagerService.super.setCustomPointerIcon(icon);
    }

    public void setPointerIconTypeAndKeepImpl(int iconId, boolean isKeep) {
        HwInputManagerService.super.setPointerIconType(iconId);
        this.mIsKeepCustomPointerIcon = isKeep;
    }

    public void setCustomPointerIconAndKeepImpl(PointerIcon icon, boolean isKeep) {
        HwInputManagerService.super.setCustomPointerIcon(icon);
        this.mIsKeepCustomPointerIcon = isKeep;
    }

    public void setIawareGameMode(int gameMode) {
        nativeSetIawareGameMode(this.mPtr, gameMode);
    }
}
