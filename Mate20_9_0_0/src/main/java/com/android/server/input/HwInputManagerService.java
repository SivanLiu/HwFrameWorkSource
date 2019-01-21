package com.android.server.input;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.IActivityController;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.service.dreams.DreamManagerInternal;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.view.IInputFilter;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.PointerIcon;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.am.ActivityRecord;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.sysload.SysLoadManager;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.WindowState;
import com.huawei.android.gameassist.HwGameAssistGamePad;
import com.huawei.android.os.BuildEx.VERSION;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.pgmng.IPGPlugCallbacks;
import com.huawei.pgmng.PGAction;
import com.huawei.pgmng.PGPlug;
import huawei.android.os.HwGeneralManager;
import huawei.cust.HwCustUtils;
import java.util.ArrayList;
import java.util.List;

public class HwInputManagerService extends InputManagerService {
    private static final String App_Left = "pressure_launch_app_left";
    private static final String App_Right = "pressure_launch_app_right";
    static final boolean ENABLE_BACK_TO_HOME = true;
    static final boolean ENABLE_LOCK_DEVICE = false;
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
    private static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    private static final int GOHOME_TIMEOUT_DELAY = 15000;
    private static final int HOME_TRACE_TIME_WINDOW = 3000;
    private static final int HT_STATE_DISABLE = 1;
    private static final int HT_STATE_GOING_HOME = 5;
    private static final int HT_STATE_IDLE = 3;
    private static final int HT_STATE_NO_INIT = 2;
    private static final int HT_STATE_PRE_GOING_HOME = 4;
    private static final int HT_STATE_UNKOWN = 0;
    private static final boolean HW_DEBUG = false;
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final int MIN_HOME_KEY_CLICK_COUNT = 2;
    private static final int MODAL_WINDOW_MASK = 40;
    private static final int MOUSE_EVENT = 8194;
    private boolean IS_SUPPORT_PRESSURE = false;
    private String TAGPRESSURE = "pressure:hwInputMS";
    private boolean isSupportPg = true;
    boolean mAnswerCall = false;
    private ContentObserver mAppLeftStartObserver;
    private ContentObserver mAppRightStartObserver;
    boolean mBackToHome = false;
    private InputWindowHandle mCurFocusedWindowHandle = null;
    int mCurrentUserId = 0;
    HwCustInputManagerService mCust;
    private IActivityController mCustomController = null;
    private DreamManagerInternal mDreamManagerInternal = null;
    private boolean mEnableFingerSnapshot = false;
    private Context mExternalContext = null;
    FingerPressNavigation mFingerPressNavi = null;
    boolean mFingerprintMarketDemoSwitch = false;
    private FingerprintNavigation mFingerprintNavigationFilter;
    private HwFingersSnapshooter mFingersSnapshooter = null;
    private Object mFreezeDetectLock = new Object();
    boolean mGallerySlide;
    boolean mGoBack = false;
    HwGuiShou mGuiShou;
    private Handler mHandler;
    private int mHeight;
    private ComponentName mHomeComponent = null;
    private Handler mHomeTraceHandler = null;
    private int mHomeTraceState = 0;
    boolean mInputDeviceChanged = false;
    boolean mIsStartInputEventControl = false;
    private boolean mKeepCustomPointerIcon = false;
    private InputWindowHandle mLastFocusedWindowHandle = null;
    private boolean mLastImmersiveMode = false;
    private ContentObserver mMinNaviObserver;
    private boolean mModalWindowOnTop = false;
    private ContentObserver mNaviBarPosObserver;
    private PGPlug mPGPlug;
    private PgEventProcesser mPgEventProcesser = new PgEventProcesser(this, null);
    private WindowManagerPolicy mPolicy = null;
    private ContentObserver mPressLimitObserver;
    private boolean mPressNaviEnable = false;
    private ContentObserver mPressNaviObserver;
    boolean mRecentApp = false;
    private ArrayList<Long> mRecentHomeKeyTimes = new ArrayList();
    private final ContentResolver mResolver;
    final SettingsObserver mSettingsObserver;
    boolean mShowNotification = false;
    boolean mStopAlarm = false;
    private int mWidth;
    private Runnable mWindowSwitchChecker = new Runnable() {
        public void run() {
            HwInputManagerService.this.handleGoHomeTimeout();
        }
    };
    private String needTip = "pressure_needTip";

    private class BroadcastThread extends Thread {
        boolean mMinNaviBar;

        private BroadcastThread() {
        }

        /* synthetic */ BroadcastThread(HwInputManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void setMiniNaviBar(boolean minNaviBar) {
            this.mMinNaviBar = minNaviBar;
        }

        public void run() {
            String access$500 = HwInputManagerService.this.TAGPRESSURE;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendBroadcast minNaviBar = ");
            stringBuilder.append(this.mMinNaviBar);
            Log.d(access$500, stringBuilder.toString());
            Intent intent = new Intent("com.huawei.navigationbar.statuschange");
            intent.putExtra("minNavigationBar", this.mMinNaviBar);
            HwInputManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public final class HwInputManagerLocalService {
        public boolean injectInputEvent(InputEvent event, int mode) {
            return HwInputManagerService.this.injectInputEventOtherScreens(event, mode);
        }

        public void setExternalDisplayContext(Context context) {
            HwInputManagerService.this.setExternalContext(context);
        }

        public void setPointerIconTypeAndKeep(int iconId, boolean keep) {
            HwInputManagerService.this.setPointerIconTypeAndKeepImpl(iconId, keep);
        }

        public void setCustomPointerIconAndKeep(PointerIcon icon, boolean keep) {
            HwInputManagerService.this.setCustomPointerIconAndKeepImpl(icon, keep);
        }
    }

    public final class HwInputManagerServiceInternal {
        public void notifyHomeLaunching() {
            HwInputManagerService.this.startTraceHomeKey();
        }

        public void setCustomActivityController(IActivityController controller) {
            HwInputManagerService.this.setCustomActivityControllerInternal(controller);
        }
    }

    private class PgEventProcesser implements IPGPlugCallbacks {
        private PgEventProcesser() {
        }

        /* synthetic */ PgEventProcesser(HwInputManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onDaemonConnected() {
        }

        public void onConnectedTimeout() {
        }

        public boolean onEvent(int actionID, String msg) {
            if (PGAction.checkActionType(actionID) == 1 && PGAction.checkActionFlag(actionID) == 3) {
                if (actionID == IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT || actionID == IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT) {
                    if (HwInputManagerService.this.mFingerPressNavi != null) {
                        HwInputManagerService.this.mFingerPressNavi.setGameScene(true);
                    }
                } else if (HwInputManagerService.this.mFingerPressNavi != null) {
                    HwInputManagerService.this.mFingerPressNavi.setGameScene(false);
                }
            }
            return true;
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
        }

        public void registerContentObserver(int userId) {
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_CAMERA_SWITCH), false, this, userId);
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_ANSWER_CALL), false, this, userId);
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_SHOW_NOTIFICATION), false, this, userId);
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_BACK_TO_HOME), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_STOP_ALARM), false, this, userId);
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_LOCK_DEVICE), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_GO_BACK), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_RECENT_APP), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(System.getUriFor(HwInputManagerService.FINGERPRINT_MARKET_DEMO_SWITCH), false, this);
            HwInputManagerService.this.mResolver.registerContentObserver(Secure.getUriFor(HwInputManagerService.FINGERPRINT_GALLERY_SLIDE), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            Slog.d("InputManager", "SettingDB has Changed");
            HwInputManagerService.this.updateFingerprintSlideSwitchValue();
        }
    }

    public HwInputManagerService(Context context, Handler handler) {
        super(context);
        HwGameAssistGamePad.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(handler);
        this.mHandler = handler;
        this.mFingerprintNavigationFilter = new FingerprintNavigation(this.mContext);
        this.mGuiShou = new HwGuiShou(this, this.mContext, this.mHandler, this.mFingerprintNavigationFilter);
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        updateFingerprintSlideSwitchValue(this.mCurrentUserId);
    }

    private void initGuiShou() {
        this.mGuiShou.registerPhoneStateListener();
        this.mGuiShou.registerFpSlideSwitchSettingsObserver();
    }

    private boolean isPressureModeOpenForChina() {
        return 1 == System.getIntForUser(this.mResolver, "virtual_notification_key_type", 0, this.mCurrentUserId);
    }

    private boolean isPressureModeOpenForOther() {
        return 1 == System.getIntForUser(this.mResolver, "virtual_notification_key_type", 0, this.mCurrentUserId);
    }

    private boolean isNavigationBarMin() {
        return 1 == Global.getInt(this.mResolver, "navigationbar_is_min", 0);
    }

    private void initObserver(Handler handler) {
        this.mPressNaviObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                HwInputManagerService.this.handleNaviChangeForTip();
                if (HwInputManagerService.IS_CHINA_AREA) {
                    HwInputManagerService.this.handleNaviChangeForChina();
                } else {
                    HwInputManagerService.this.handleNaviChangeForOther();
                }
            }
        };
        this.mMinNaviObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                if (HwInputManagerService.IS_CHINA_AREA) {
                    HwInputManagerService.this.handleNaviChangeForChina();
                }
            }
        };
        this.mPressLimitObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                float limit = System.getFloatForUser(HwInputManagerService.this.mContext.getContentResolver(), "pressure_habit_threshold", 0.2f, ActivityManager.getCurrentUser());
                String access$500 = HwInputManagerService.this.TAGPRESSURE;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mPressLimitObserver onChange open limit = ");
                stringBuilder.append(limit);
                Slog.d(access$500, stringBuilder.toString());
                if (HwInputManagerService.this.mFingerPressNavi != null) {
                    HwInputManagerService.this.mFingerPressNavi.setPressureLimit(limit);
                }
            }
        };
        this.mNaviBarPosObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                int naviBarPos = System.getIntForUser(HwInputManagerService.this.mContext.getContentResolver(), "virtual_key_type", 0, ActivityManager.getCurrentUser());
                String access$500 = HwInputManagerService.this.TAGPRESSURE;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mPressNaviObserver onChange open naviBarPos = ");
                stringBuilder.append(naviBarPos);
                Slog.d(access$500, stringBuilder.toString());
                if (HwInputManagerService.this.mFingerPressNavi != null) {
                    HwInputManagerService.this.mFingerPressNavi.setNaviBarPosition(naviBarPos);
                }
            }
        };
        this.mAppLeftStartObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                String appLeftPkg = System.getStringForUser(HwInputManagerService.this.mContext.getContentResolver(), HwInputManagerService.App_Left, ActivityManager.getCurrentUser());
                String access$500 = HwInputManagerService.this.TAGPRESSURE;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AppStartObserver onChange open appLeftPkg = ");
                stringBuilder.append(appLeftPkg);
                Slog.d(access$500, stringBuilder.toString());
                if (HwInputManagerService.this.mFingerPressNavi != null && appLeftPkg != null) {
                    HwInputManagerService.this.mFingerPressNavi.setAppLeftPkg(appLeftPkg);
                }
            }
        };
        this.mAppRightStartObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                String appRightPkg = System.getStringForUser(HwInputManagerService.this.mContext.getContentResolver(), HwInputManagerService.App_Right, ActivityManager.getCurrentUser());
                String access$500 = HwInputManagerService.this.TAGPRESSURE;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AppStartObserver onChange open appRightPkg = ");
                stringBuilder.append(appRightPkg);
                Slog.d(access$500, stringBuilder.toString());
                if (HwInputManagerService.this.mFingerPressNavi != null && appRightPkg != null) {
                    HwInputManagerService.this.mFingerPressNavi.setAppRightPkg(appRightPkg);
                }
            }
        };
        this.mResolver.registerContentObserver(System.getUriFor("virtual_notification_key_type"), false, this.mPressNaviObserver, -1);
        this.mResolver.registerContentObserver(Global.getUriFor("navigationbar_is_min"), false, this.mMinNaviObserver, -1);
        this.mResolver.registerContentObserver(System.getUriFor("pressure_habit_threshold"), false, this.mPressLimitObserver, -1);
        this.mResolver.registerContentObserver(System.getUriFor("virtual_key_type"), false, this.mNaviBarPosObserver, -1);
        this.mResolver.registerContentObserver(System.getUriFor(App_Left), false, this.mAppLeftStartObserver, -1);
        this.mResolver.registerContentObserver(System.getUriFor(App_Right), false, this.mAppRightStartObserver, -1);
    }

    private void handleNaviChangeForChina() {
        if (this.IS_SUPPORT_PRESSURE) {
            if (isPressureModeOpenForChina() && isNavigationBarMin()) {
                Slog.d(this.TAGPRESSURE, "mPressNaviObserver onChange open");
                this.mPressNaviEnable = true;
                this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
                if (this.mFingerPressNavi != null) {
                    this.mFingerPressNavi.createPointerCircleAnimation();
                    this.mFingerPressNavi.setDisplayWidthAndHeight(this.mWidth, this.mHeight);
                    this.mFingerPressNavi.setMode(1);
                }
                SystemProperties.set("persist.sys.fingerpressnavi", "1");
            } else {
                Slog.d(this.TAGPRESSURE, "mPressNaviObserver onChange close");
                this.mPressNaviEnable = false;
                this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
                if (this.mFingerPressNavi != null) {
                    this.mFingerPressNavi.destoryPointerCircleAnimation();
                    this.mFingerPressNavi.setMode(0);
                }
                SystemProperties.set("persist.sys.fingerpressnavi", "1");
            }
            int naviBarPos = System.getIntForUser(this.mContext.getContentResolver(), "virtual_key_type", 0, this.mCurrentUserId);
            if (this.mFingerPressNavi != null) {
                this.mFingerPressNavi.setNaviBarPosition(naviBarPos);
            }
        }
    }

    private void handleNaviChangeForOther() {
        if (this.IS_SUPPORT_PRESSURE) {
            if (isPressureModeOpenForOther()) {
                Slog.d(this.TAGPRESSURE, "mPressNaviObserver onChange open");
                this.mPressNaviEnable = true;
                this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
                if (this.mFingerPressNavi != null) {
                    this.mFingerPressNavi.createPointerCircleAnimation();
                    this.mFingerPressNavi.setDisplayWidthAndHeight(this.mWidth, this.mHeight);
                    this.mFingerPressNavi.setMode(1);
                }
                sendBroadcast(true);
                SystemProperties.set("persist.sys.fingerpressnavi", "1");
            } else {
                Slog.d(this.TAGPRESSURE, "mPressNaviObserver onChange close");
                this.mPressNaviEnable = false;
                this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
                if (this.mFingerPressNavi != null) {
                    this.mFingerPressNavi.destoryPointerCircleAnimation();
                    this.mFingerPressNavi.setMode(0);
                }
                sendBroadcast(false);
                SystemProperties.set("persist.sys.fingerpressnavi", "1");
            }
            int naviBarPos = System.getIntForUser(this.mContext.getContentResolver(), "virtual_key_type", 0, this.mCurrentUserId);
            if (this.mFingerPressNavi != null) {
                this.mFingerPressNavi.setNaviBarPosition(naviBarPos);
            }
        }
    }

    private void sendBroadcast(boolean minNaviBar) {
        BroadcastThread sendthread = new BroadcastThread(this, null);
        sendthread.setMiniNaviBar(minNaviBar);
        sendthread.start();
    }

    private void pressureinit() {
        if (this.mPressNaviEnable || this.IS_SUPPORT_PRESSURE) {
            this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
            if (this.mFingerPressNavi != null) {
                if (this.mPressNaviEnable) {
                    Log.v(this.TAGPRESSURE, "systemRunning  mPressNaviEnable ");
                    this.mFingerPressNavi.createPointerCircleAnimation();
                    this.mFingerPressNavi.setMode(1);
                } else if (this.IS_SUPPORT_PRESSURE) {
                    this.mFingerPressNavi.setMode(0);
                    Log.v(this.TAGPRESSURE, "systemRunning  IS_SUPPORT_PRESSURE ");
                }
                float limit = System.getFloatForUser(this.mContext.getContentResolver(), "pressure_habit_threshold", 0.2f, this.mCurrentUserId);
                int naviBarPos = System.getIntForUser(this.mContext.getContentResolver(), "virtual_key_type", 0, this.mCurrentUserId);
                String appLeftPkg = System.getStringForUser(this.mContext.getContentResolver(), App_Left, this.mCurrentUserId);
                if (appLeftPkg != null) {
                    this.mFingerPressNavi.setAppLeftPkg(appLeftPkg);
                } else {
                    this.mFingerPressNavi.setAppLeftPkg("none_app");
                }
                String appRightPkg = System.getStringForUser(this.mContext.getContentResolver(), App_Right, this.mCurrentUserId);
                if (appRightPkg != null) {
                    this.mFingerPressNavi.setAppRightPkg(appRightPkg);
                } else {
                    this.mFingerPressNavi.setAppRightPkg("none_app");
                }
                this.mFingerPressNavi.setNaviBarPosition(naviBarPos);
                this.mFingerPressNavi.setPressureLimit(limit);
                SystemProperties.set("persist.sys.fingerpressnavi", "1");
                return;
            }
            return;
        }
        SystemProperties.set("persist.sys.fingerpressnavi", "0");
    }

    private int isNeedTip() {
        return System.getIntForUser(this.mContext.getContentResolver(), this.needTip, 0, this.mCurrentUserId);
    }

    private void handleNaviChangeForTip() {
        if (isPressureModeOpenForChina()) {
            System.putIntForUser(this.mContext.getContentResolver(), this.needTip, 1, this.mCurrentUserId);
            this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
            if (this.mFingerPressNavi != null) {
                this.mFingerPressNavi.setNeedTip(true);
                return;
            }
            return;
        }
        System.putIntForUser(this.mContext.getContentResolver(), this.needTip, 2, this.mCurrentUserId);
        this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
        if (this.mFingerPressNavi != null) {
            this.mFingerPressNavi.setNeedTip(false);
        }
    }

    private void pressureinitData() {
        if (this.IS_SUPPORT_PRESSURE) {
            this.mFingerPressNavi = FingerPressNavigation.getInstance(this.mContext);
            if (this.mFingerPressNavi != null) {
                int istip = isNeedTip();
                if (istip == 0) {
                    System.putIntForUser(this.mContext.getContentResolver(), this.needTip, 1, this.mCurrentUserId);
                    this.mFingerPressNavi.setNeedTip(true);
                } else if (istip == 3) {
                    System.putIntForUser(this.mContext.getContentResolver(), this.needTip, 2, this.mCurrentUserId);
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
        if (this.mFingerPressNavi != null) {
            this.mFingerPressNavi.setDisplayWidthAndHeight(width, height);
        }
    }

    public void setCurFocusWindow(WindowState focus) {
        if (this.mFingerPressNavi != null) {
            this.mFingerPressNavi.setCurFocusWindow(focus);
        }
    }

    public void setIsTopFullScreen(boolean isTopFullScreen) {
        if (this.mFingerPressNavi != null) {
            this.mFingerPressNavi.setIsTopFullScreen(isTopFullScreen);
        }
    }

    public void setImmersiveMode(boolean mode) {
        if (this.IS_SUPPORT_PRESSURE) {
            if (this.mFingerPressNavi != null) {
                this.mFingerPressNavi.setImmersiveMode(mode);
            }
            if (!(IS_CHINA_AREA || mode || !this.mLastImmersiveMode)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setImmersiveMode  has Changedi mode = ");
                stringBuilder.append(mode);
                Slog.d("InputManager", stringBuilder.toString());
                handleNaviChangeForOther();
            }
            this.mLastImmersiveMode = mode;
        }
    }

    public void systemRunning() {
        super.systemRunning();
        this.mEnableFingerSnapshot = SystemProperties.getBoolean("ro.config.hw_triple_finger", false);
        if (this.mEnableFingerSnapshot) {
            this.mFingersSnapshooter = new HwFingersSnapshooter(this.mContext, this);
            synchronized (this.mInputFilterLock) {
                nativeSetInputFilterEnabled(this.mPtr, true);
            }
        }
        this.IS_SUPPORT_PRESSURE = HwGeneralManager.getInstance().isSupportForce();
        if (IS_CHINA_AREA) {
            if (this.IS_SUPPORT_PRESSURE && isPressureModeOpenForChina() && isNavigationBarMin()) {
                this.mPressNaviEnable = true;
            }
        } else if (this.IS_SUPPORT_PRESSURE && isPressureModeOpenForOther()) {
            this.mPressNaviEnable = true;
        }
        if (this.IS_SUPPORT_PRESSURE) {
            initObserver(this.mHandler);
        }
        this.mFingerprintNavigationFilter.systemRunning();
        pressureinit();
        pressureinitData();
        if (this.isSupportPg) {
            initPgPlugThread();
        }
        if (isHomeTraceSupported()) {
            this.mHomeTraceState = 2;
        } else {
            this.mHomeTraceState = 1;
        }
        if (HwPCUtils.enabled()) {
            LocalServices.addService(HwInputManagerLocalService.class, new HwInputManagerLocalService());
        }
        AwareFakeActivityRecg.self().setInputManagerService(this);
        SysLoadManager.getInstance().setInputManagerService(this);
    }

    public void start() {
        super.start();
        this.mCust = (HwCustInputManagerService) HwCustUtils.createObj(HwCustInputManagerService.class, new Object[]{this});
        if (this.mCust != null) {
            this.mCust.registerContentObserverForSetGloveMode(this.mContext);
        }
        initGuiShou();
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                HwInputManagerService.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                String access$500 = HwInputManagerService.this.TAGPRESSURE;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("user switch mCurrentUserId = ");
                stringBuilder.append(HwInputManagerService.this.mCurrentUserId);
                Slog.e(access$500, stringBuilder.toString());
                if (HwInputManagerService.this.IS_SUPPORT_PRESSURE) {
                    HwInputManagerService.this.pressureinit();
                    HwInputManagerService.this.pressureinitData();
                    if (HwInputManagerService.IS_CHINA_AREA) {
                        HwInputManagerService.this.handleNaviChangeForChina();
                    } else {
                        HwInputManagerService.this.handleNaviChangeForOther();
                    }
                }
            }
        }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mHandler);
    }

    private void initPgPlugThread() {
        this.mPGPlug = new PGPlug(this.mPgEventProcesser, "InputManager");
        new Thread(this.mPGPlug, "InputManager").start();
    }

    public void inputdevicechanged() {
        Slog.d("InputManager", "inputdevicechanged.....");
        this.mInputDeviceChanged = true;
    }

    boolean filterInputEvent(InputEvent event, int policyFlags) {
        if (this.mIsStartInputEventControl) {
            if (this.mInputDeviceChanged && event.getSource() == MOUSE_EVENT) {
                this.mInputDeviceChanged = false;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mouse event :");
                stringBuilder.append(this.mInputDeviceChanged);
                Slog.d("InputManager", stringBuilder.toString());
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
        if (this.mFingerprintNavigationFilter.filterInputEvent(event, policyFlags)) {
            return false;
        }
        if (this.mFingerPressNavi != null && !this.mFingerPressNavi.filterPressueInputEvent(event)) {
            return false;
        }
        if (HwPCUtils.isPcCastModeInServer() || this.mFingersSnapshooter == null || this.mFingersSnapshooter.handleMotionEvent(event)) {
            return super.filterInputEvent(event, policyFlags);
        }
        return false;
    }

    boolean isFingprintEventUnHandled(KeyEvent event) {
        return (event.getHwFlags() & 2048) != 0;
    }

    KeyEvent dispatchUnhandledKey(InputWindowHandle focus, KeyEvent event, int policyFlags) {
        if (!isFingprintEventUnHandled(event) || !this.mFingerprintNavigationFilter.dispatchUnhandledKey(event, policyFlags)) {
            return super.dispatchUnhandledKey(focus, event, policyFlags);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dispatchUnhandledKey flags=");
        stringBuilder.append(Integer.toHexString(event.getHwFlags()));
        Slog.d("InputManager", stringBuilder.toString());
        return null;
    }

    public final void updateFingerprintSlideSwitchValue() {
        updateFingerprintSlideSwitchValue(ActivityManager.getCurrentUser());
    }

    public final void updateFingerprintSlideSwitchValue(int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ActivityManager.getCurrentUser:");
        stringBuilder.append(userId);
        Slog.d("InputManager", stringBuilder.toString());
        if (!HwGuiShou.isGuiShouEnabled()) {
            this.mAnswerCall = Secure.getIntForUser(this.mResolver, FINGERPRINT_ANSWER_CALL, 0, userId) != 0;
            this.mStopAlarm = Secure.getIntForUser(this.mResolver, FINGERPRINT_STOP_ALARM, 0, userId) != 0;
        }
        this.mShowNotification = Secure.getIntForUser(this.mResolver, FINGERPRINT_SHOW_NOTIFICATION, 0, userId) != 0;
        this.mBackToHome = Secure.getInt(this.mResolver, FINGERPRINT_BACK_TO_HOME, 0) != 0;
        this.mGoBack = Secure.getInt(this.mResolver, FINGERPRINT_GO_BACK, 0) != 0;
        this.mRecentApp = Secure.getInt(this.mResolver, FINGERPRINT_RECENT_APP, 0) != 0;
        this.mFingerprintMarketDemoSwitch = System.getInt(this.mResolver, FINGERPRINT_MARKET_DEMO_SWITCH, 0) == 1;
        this.mGallerySlide = Secure.getIntForUser(this.mResolver, FINGERPRINT_GALLERY_SLIDE, 1, userId) != 0;
        StringBuilder stringBuilder2;
        if (this.mBackToHome || this.mGoBack || this.mRecentApp || this.mFingerprintMarketDemoSwitch || FRONT_FINGERPRINT_NAVIGATION) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("open fingerprint nav->FINGERPRINT_SLIDE_SWITCH to 1 userId:");
            stringBuilder2.append(userId);
            Slog.d("InputManager", stringBuilder2.toString());
            System.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, 1, userId);
            this.mGuiShou.setFingerprintSlideSwitchValue(true);
        } else if (HwPCUtils.isPcCastModeInServer()) {
            HwPCUtils.log("InputManager", "set fingerprint_slide_switch=1 in pc mode");
            System.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, 1, userId);
        } else if (HwGuiShou.isGuiShouEnabled() || !(this.mShowNotification || this.mAnswerCall || this.mStopAlarm)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("close fingerprint nav ->FINGERPRINT_SLIDE_SWITCH to 0 userId:");
            stringBuilder.append(userId);
            Slog.d("InputManager", stringBuilder.toString());
            System.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, 0, userId);
            this.mGuiShou.setFingerprintSlideSwitchValue(false);
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("open fingerprint nav->FINGERPRINT_SLIDE_SWITCH to 1 userId:");
            stringBuilder2.append(userId);
            Slog.d("InputManager", stringBuilder2.toString());
            System.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, 1, userId);
            this.mGuiShou.setFingerprintSlideSwitchValue(true);
        }
    }

    public boolean isDefaultSlideSwitchOn() {
        return this.mShowNotification || this.mBackToHome || this.mGoBack || this.mRecentApp || this.mFingerprintMarketDemoSwitch || this.mGallerySlide;
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUserSwitching, newUserId=");
        stringBuilder.append(newUserId);
        Slog.i("InputManager", stringBuilder.toString());
        this.mGuiShou.registerFpSlideSwitchSettingsObserver(newUserId);
        this.mSettingsObserver.registerContentObserver(newUserId);
        this.mSettingsObserver.onChange(true);
        this.mFingerprintNavigationFilter.setCurrentUser(newUserId, currentProfileIds);
    }

    public void setInputWindows(InputWindowHandle[] windowHandles, InputWindowHandle focusedWindowHandle) {
        super.setInputWindows(windowHandles, focusedWindowHandle);
        checkHomeWindowResumed(windowHandles);
    }

    private boolean inSuperPowerSavingMode() {
        return SystemProperties.getBoolean("sys.super_power_save", false);
    }

    private boolean isHomeTraceSupported() {
        boolean z = false;
        if (!SystemProperties.get("ro.runmode", "normal").equals("normal") || SystemProperties.getInt("ro.logsystem.usertype", 1) != 3) {
            return false;
        }
        if (VERSION.EMUI_SDK_INT >= 11) {
            z = true;
        }
        return z;
    }

    private void notifyWindowSwitched() {
        synchronized (this.mFreezeDetectLock) {
            this.mHomeTraceState = 3;
            this.mFreezeDetectLock.notifyAll();
        }
    }

    private String getInputWindowInfo(InputWindowHandle windowHandle) {
        if (windowHandle == null) {
            return null;
        }
        String appName = windowHandle.inputApplicationHandle != null ? windowHandle.inputApplicationHandle.name : "null";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("windowHandle=");
        stringBuilder.append(windowHandle.name);
        stringBuilder.append(",windowHandle.InputApplicationHandle=");
        stringBuilder.append(appName);
        return stringBuilder.toString();
    }

    private void handleGoHomeTimeout() {
        synchronized (this.mFreezeDetectLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Go home timeout, current focused window: ");
            stringBuilder.append(getInputWindowInfo(this.mCurFocusedWindowHandle));
            stringBuilder.append(", Preferred homeActivity = ");
            stringBuilder.append(this.mHomeComponent);
            Slog.e("InputManager", stringBuilder.toString());
            this.mHomeTraceState = 3;
        }
    }

    private boolean isHomeTraceEnabled() {
        if (this.mHomeTraceState == 2) {
            this.mHomeTraceState = initHomeTrace();
        }
        return this.mHomeTraceState >= 3;
    }

    private ComponentName getComponentName(InputWindowHandle windowHandle) {
        if (windowHandle == null) {
            Slog.i("InputManager", "non-home window: windowHandle is null");
            return null;
        }
        WindowState windowState = windowHandle.windowState;
        if (windowState == null) {
            Slog.i("InputManager", "non-home window: windowHandle's state is null");
            return null;
        }
        ActivityRecord r = ActivityRecord.forToken((IBinder) windowState.getAppToken());
        if (r != null) {
            return r.realActivity;
        }
        Slog.i("InputManager", "non-home window:Can not get ActivityRecord from token of windowHandle's windowState");
        return null;
    }

    private boolean isHomeWindow(ComponentName componentName) {
        boolean z = false;
        if (componentName == null) {
            return false;
        }
        String packageName = componentName.getPackageName();
        if (!(this.mHomeComponent == null || packageName == null || !packageName.equals(this.mHomeComponent.getPackageName()))) {
            z = true;
        }
        return z;
    }

    private boolean isHomeWindow(InputWindowHandle windowHandle) {
        if (getHomeActivity() == null) {
            return false;
        }
        return isHomeWindow(getComponentName(windowHandle));
    }

    private InputWindowHandle updateFocusedWindow(InputWindowHandle[] windowHandles) {
        synchronized (this.mFreezeDetectLock) {
            this.mCurFocusedWindowHandle = null;
            for (InputWindowHandle windowHandle : windowHandles) {
                if (!(windowHandle == null || !windowHandle.hasFocus || windowHandle.inputChannel == null)) {
                    this.mCurFocusedWindowHandle = windowHandle;
                }
            }
        }
        return this.mCurFocusedWindowHandle;
    }

    /* JADX WARNING: Missing block: B:9:0x0017, code skipped:
            r5.mModalWindowOnTop = false;
     */
    /* JADX WARNING: Missing block: B:10:0x001c, code skipped:
            if (r0 == com.android.server.hidata.arbitration.HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL) goto L_0x0024;
     */
    /* JADX WARNING: Missing block: B:12:0x0020, code skipped:
            if (r0 != 2003) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:13:0x0024, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:15:0x0026, code skipped:
            if (r4 == false) goto L_0x002e;
     */
    /* JADX WARNING: Missing block: B:17:0x002a, code skipped:
            if ((r1 & 40) != 0) goto L_0x002e;
     */
    /* JADX WARNING: Missing block: B:18:0x002c, code skipped:
            r5.mModalWindowOnTop = true;
     */
    /* JADX WARNING: Missing block: B:20:0x0030, code skipped:
            return r5.mModalWindowOnTop;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean modalWindowOnTop() {
        synchronized (this.mFreezeDetectLock) {
            boolean z = false;
            if (this.mCurFocusedWindowHandle == null) {
                return false;
            }
            int curWindowType = this.mCurFocusedWindowHandle.layoutParamsType;
            int curLayoutParamsFlags = this.mCurFocusedWindowHandle.layoutParamsFlags;
        }
    }

    private boolean focusWindowChanged() {
        boolean z;
        synchronized (this.mFreezeDetectLock) {
            z = (this.mLastFocusedWindowHandle == null || this.mCurFocusedWindowHandle == null || this.mLastFocusedWindowHandle == this.mCurFocusedWindowHandle) ? false : true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:26:0x0056, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void checkHomeWindowResumed(InputWindowHandle[] windowHandles) {
        if (windowHandles != null && updateFocusedWindow(windowHandles) != null && isHomeTraceEnabled()) {
            if (modalWindowOnTop()) {
                finishTraceHomeKey();
            } else if (this.mHomeTraceHandler.hasCallbacks(this.mWindowSwitchChecker) && focusWindowChanged()) {
                synchronized (this.mFreezeDetectLock) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Change focus window to ");
                    stringBuilder.append(this.mCurFocusedWindowHandle.name);
                    stringBuilder.append(" after clicked HOME key, freeze not detected");
                    Slog.i("InputManager", stringBuilder.toString());
                }
                finishTraceHomeKey();
            }
        }
    }

    private ComponentName getHomeActivity() {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            Slog.e("InputManager", "getHomeActivity, can't get packagemanager");
            return null;
        }
        List<ResolveInfo> homeActivities = new ArrayList();
        ComponentName preferHomeActivity = pm.getHomeActivities(homeActivities);
        if (preferHomeActivity != null) {
            this.mHomeComponent = preferHomeActivity;
        } else if (homeActivities.size() == 0) {
            Slog.e("InputManager", "No home activity found!");
            return null;
        } else {
            ActivityInfo homeActivityInfo = ((ResolveInfo) homeActivities.get(0)).activityInfo;
            this.mHomeComponent = new ComponentName(homeActivityInfo.packageName, homeActivityInfo.name);
        }
        return this.mHomeComponent;
    }

    private int initHomeTrace() {
        int uiModeType = 0;
        UiModeManager uiModeManager = (UiModeManager) this.mContext.getSystemService("uimode");
        if (uiModeManager != null) {
            uiModeType = uiModeManager.getCurrentModeType();
        }
        if (uiModeType != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("current Ui mode is ");
            stringBuilder.append(uiModeType);
            stringBuilder.append(" not support to trace HOME!");
            Slog.e("InputManager", stringBuilder.toString());
            return 1;
        } else if (getHomeActivity() == null) {
            Slog.e("InputManager", "do not trace HOME because no prefered homeActivity found!");
            return 1;
        } else {
            LocalServices.addService(HwInputManagerServiceInternal.class, new HwInputManagerServiceInternal());
            this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
            this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
            this.mHomeTraceHandler = DisplayThread.getHandler();
            return 3;
        }
    }

    private boolean isUserSetupComplete() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    private boolean ignoreHomeTrace() {
        if ((this.mDreamManagerInternal != null && this.mDreamManagerInternal.isDreaming()) || (this.mPolicy != null && this.mPolicy.isKeyguardLocked())) {
            return true;
        }
        if (!isUserSetupComplete()) {
            Slog.i("InputManager", "user setup not complete, skip the trace of HOME");
            return true;
        } else if (inSuperPowerSavingMode()) {
            Slog.i("InputManager", "in super power saving mode, skip the trace of HOME");
            return true;
        } else if (!this.mModalWindowOnTop) {
            return false;
        } else {
            Slog.i("InputManager", "Modal window on the top, skip the trace of HOME");
            return true;
        }
    }

    protected void finishTraceHomeKey() {
        this.mHomeTraceHandler.removeCallbacks(this.mWindowSwitchChecker);
        notifyWindowSwitched();
    }

    private boolean isHomeKeyUp(KeyEvent event) {
        if (event.getKeyCode() == 3 && event.getAction() == 1 && !event.isCanceled()) {
            return true;
        }
        return false;
    }

    protected void preStartTraceHome(InputWindowHandle focus, KeyEvent event) {
        if (focus != null && isHomeKeyUp(event)) {
            if (ActivityManager.isUserAMonkey() || hasCustomActivityController()) {
                Slog.d("InputManager", "trigger HOME in monkey mode or custom ActivityController set");
            } else if (!isHomeTraceEnabled() || ignoreHomeTrace() || isHomeWindow(focus)) {
                Slog.i("InputManager", "preStart Home return.");
            } else {
                long eventTime = event.getEventTime();
                this.mRecentHomeKeyTimes.add(Long.valueOf(eventTime));
                for (int i = this.mRecentHomeKeyTimes.size() - 1; i >= 0; i--) {
                    long interval = eventTime - ((Long) this.mRecentHomeKeyTimes.get(i)).longValue();
                    if (interval >= 0 && interval > HwArbitrationDEFS.NotificationMonitorPeriodMillis) {
                        this.mRecentHomeKeyTimes.remove(i);
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("preStart Home is ");
                stringBuilder.append(this.mRecentHomeKeyTimes.size());
                Slog.i("InputManager", stringBuilder.toString());
                StringBuilder stringBuilder2;
                if (this.mRecentHomeKeyTimes.size() < 2) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("click HOME ");
                    stringBuilder2.append(this.mRecentHomeKeyTimes.size());
                    stringBuilder2.append(" times, trace if continuous click ");
                    stringBuilder2.append(2);
                    stringBuilder2.append(" times in ");
                    stringBuilder2.append(3000);
                    stringBuilder2.append("ms");
                    Slog.i("InputManager", stringBuilder2.toString());
                    return;
                }
                synchronized (this.mFreezeDetectLock) {
                    if (this.mHomeTraceState == 3) {
                        this.mHomeTraceState = 4;
                        this.mLastFocusedWindowHandle = focus;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("start HOME in window:");
                        stringBuilder2.append(focus.name);
                        Slog.i("InputManager", stringBuilder2.toString());
                    }
                }
            }
        }
    }

    protected void postStartTraceHome(InputWindowHandle focus) {
        synchronized (this.mFreezeDetectLock) {
            if (this.mHomeTraceState == 4) {
                String str = "InputManager";
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Home app not launched at ");
                stringBuilder.append(focus != null ? focus.name : "null window");
                Slog.i(str, stringBuilder.toString());
                this.mHomeTraceState = 3;
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x002a, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean startTraceHomeKey() {
        synchronized (this.mFreezeDetectLock) {
            if (this.mHomeTraceState != 4) {
                return false;
            } else if (!this.mHomeTraceHandler.hasCallbacks(this.mWindowSwitchChecker)) {
                Slog.i("InputManager", "Launching HOME, keep checking if the window switch");
                this.mHomeTraceHandler.postDelayed(this.mWindowSwitchChecker, 15000);
                this.mHomeTraceState = 5;
            }
        }
    }

    private void setCustomActivityControllerInternal(IActivityController controller) {
        synchronized (this.mFreezeDetectLock) {
            this.mCustomController = controller;
        }
    }

    public Context getExternalContext() {
        return this.mExternalContext;
    }

    private void setExternalContext(Context context) {
        this.mExternalContext = context;
        nativeReloadPointerIcons(this.mPtr, this.mExternalContext);
    }

    private boolean hasCustomActivityController() {
        boolean z;
        synchronized (this.mFreezeDetectLock) {
            z = this.mCustomController != null;
        }
        return z;
    }

    protected long interceptKeyBeforeDispatching(InputWindowHandle focus, KeyEvent event, int policyFlags) {
        preStartTraceHome(focus, event);
        long ret = super.interceptKeyBeforeDispatching(focus, event, policyFlags);
        postStartTraceHome(focus);
        return ret;
    }

    public void notifyNativeEvent(int eventType, int eventValue, int keyAction, int pid, int uid) {
        AwareFakeActivityRecg.self().processNativeEventNotify(eventType, eventValue, keyAction, pid, uid);
    }

    public boolean injectInputEventOtherScreens(InputEvent event, int mode) {
        int displayId = HwPCUtils.getPCDisplayID();
        if (displayId != 0 && displayId != -1) {
            return injectInputEventInternal(event, displayId, mode);
        }
        Slog.i("InputManager", "not other screen found!");
        return false;
    }

    public static String getAppName(Context context, int pid) {
        if (pid <= 0 || context == null) {
            return null;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        if (activityManager == null) {
            return null;
        }
        List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null || appProcesses.size() == 0) {
            return null;
        }
        String packageName = null;
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.pid == pid) {
                packageName = appProcess.processName;
                break;
            }
        }
        return packageName;
    }

    private boolean checkIsSystemApp() {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        if (pm.checkSignatures(Binder.getCallingUid(), Process.myUid()) != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("not SIGNATURE_MATCH ....");
            stringBuilder.append(Binder.getCallingUid());
            Slog.d("InputManager", stringBuilder.toString());
            return false;
        }
        String pckName = "";
        try {
            pckName = getAppName(this.mContext, Binder.getCallingPid());
            StringBuilder stringBuilder2;
            if (pckName == null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pckName is null ");
                stringBuilder2.append(Binder.getCallingPid());
                stringBuilder2.append(" ");
                stringBuilder2.append(Process.myUid());
                Slog.e("InputManager", stringBuilder2.toString());
                return false;
            }
            ApplicationInfo info = pm.getApplicationInfo(pckName, 0);
            if (info == null || (info.flags & 1) == 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("return false ");
                stringBuilder2.append(pckName);
                Slog.d("InputManager", stringBuilder2.toString());
                return false;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("return true ");
            stringBuilder3.append(pckName);
            Slog.d("InputManager", stringBuilder3.toString());
            return true;
        } catch (Exception ex) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("isSystemApp not found app");
            stringBuilder4.append(pckName);
            stringBuilder4.append("exception=");
            stringBuilder4.append(ex.toString());
            Slog.e("InputManager", stringBuilder4.toString());
            return false;
        }
    }

    private void setInputFilterEnabled() {
        synchronized (this.mInputFilterLock) {
            if (!(this.mEnableFingerSnapshot || this.mIsStartInputEventControl)) {
                if (this.mInputFilter == null) {
                    nativeSetInputFilterEnabled(this.mPtr, false);
                }
            }
            nativeSetInputFilterEnabled(this.mPtr, true);
        }
    }

    public void setInputEventStrategy(boolean isStartInputEventControl) {
        if (checkIsSystemApp() && this.mIsStartInputEventControl != isStartInputEventControl) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mIsStartInputEventControl change to:");
            stringBuilder.append(isStartInputEventControl);
            Slog.d("InputManager", stringBuilder.toString());
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

    public void setInputFilter(IInputFilter filter) {
        super.setInputFilter(filter);
        synchronized (this.mInputFilterLock) {
            if (filter == null) {
                try {
                    if (this.mEnableFingerSnapshot || this.mIsStartInputEventControl) {
                        nativeSetInputFilterEnabled(this.mPtr, true);
                    }
                } catch (Throwable th) {
                }
            }
        }
    }

    public void setPointerIconType(int iconId) {
        if ((this.mIsStartInputEventControl || this.mWindowManagerCallbacks.hasLighterViewInPCCastMode()) && this.mKeepCustomPointerIcon) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPointerIconType cannot change pointer icon when lighter view above.gamepad:");
            stringBuilder.append(this.mIsStartInputEventControl);
            Slog.i("InputManager", stringBuilder.toString());
            return;
        }
        super.setPointerIconType(iconId);
    }

    public void setCustomPointerIcon(PointerIcon icon) {
        if ((this.mIsStartInputEventControl || this.mWindowManagerCallbacks.hasLighterViewInPCCastMode()) && this.mKeepCustomPointerIcon) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setCustomPointerIcon cannot change pointer icon when lighter view above.gamepad:");
            stringBuilder.append(this.mIsStartInputEventControl);
            Slog.i("InputManager", stringBuilder.toString());
            return;
        }
        super.setCustomPointerIcon(icon);
    }

    public void setPointerIconTypeAndKeepImpl(int iconId, boolean keep) {
        super.setPointerIconType(iconId);
        this.mKeepCustomPointerIcon = keep;
    }

    public void setCustomPointerIconAndKeepImpl(PointerIcon icon, boolean keep) {
        super.setCustomPointerIcon(icon);
        this.mKeepCustomPointerIcon = keep;
    }
}
