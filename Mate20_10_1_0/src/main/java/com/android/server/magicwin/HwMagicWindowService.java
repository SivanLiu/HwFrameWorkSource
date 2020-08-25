package com.android.server.magicwin;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.magicwin.IHwMagicWindow;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.HwMwUtils;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.magicwin.HwMagicWindowConfig;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityStack;
import com.android.server.wm.HwActivityTaskManagerServiceEx;
import com.android.server.wm.HwMagicModeA1An;
import com.android.server.wm.HwMagicModeAnAn;
import com.android.server.wm.HwMagicModeBase;
import com.android.server.wm.HwMagicModeOpen;
import com.android.server.wm.HwMagicWinAmsPolicy;
import com.android.server.wm.HwMagicWinWmsPolicy;
import com.android.server.wm.HwMultiWindowManager;
import com.android.server.wm.WindowManagerService;
import com.huawei.android.fsm.HwFoldScreenManagerEx;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public final class HwMagicWindowService extends IHwMagicWindow.Stub {
    public static final String ACTION_FROM_SYSTEMMANAGER = "huawei.intent.action.UPDATE_MAGIC_WINDOW_FEATURE_ACTION";
    public static final int ALARM_TIME_FOR_UPDATE_CONFIG = 4;
    public static final String ALARM_UPDATE_CONFIG_ACTION = "huawei.intent.action.ALARM_UPDATE_CONFIG_ACTION";
    private static final String APP_CONFIG_UPDATE_ACTION = "com.huawei.easygo.package_data_change";
    private static final String BUNDLE_GET_POS_INT = "int";
    private static final String BUNDLE_IS_IN_MAGIC_WIN_MODE = "boolean";
    private static final long CHECK_REPORT_MAGICWIN_USAGE_PERIOD = 1800000;
    public static final String CONFIG_URI = "configUri";
    private static final String DELAYED_UPDATE_CONFIG_PERMISSION = "com.huawei.permission.mgcw.UPDATE_CLOUD_CONFIG";
    private static final String EASYGO_LOGIN_STATUS_KEY = "@int:loginStatus";
    private static final String EASYGO_PERMISSION_SEND_SYS_BCST = "com.huawei.easygo.permission.SEND_SYS_BCST";
    private static final String EASYGO_REGISTER_FUNCTION_KEY = "function";
    private static final String EASYGO_REGISTER_FUNCTION_VALUE = "magicwindow";
    private static final String EASYGO_REGISTER_SERVICE_KEY = "server_service_name";
    private static final String EASYGO_REGISTER_SERVICE_VALUE = "com.android.server.magicwin.HwMagicWindowService";
    private static final String EASYGO_TARGET_POS_KEY = "@int:targetPosition";
    private static final String EASYGO_TASK_ID_KEY = "@int:taskId";
    private static final String FUNC_GET_TASK_POS = "getTaskPosition";
    private static final String FUNC_IS_IN_MAGIC_WIN_MODE = "isInMagicWindowMode";
    private static final String FUNC_SET_LOGIN_STATUS = "setLoginStatus";
    private static final String FUNC_SET_TASK_POS = "setTaskPosition";
    private static final String KEY_MULTI_RESUME = "android.allow_multiple_resumed_activities";
    private static final String MUlTIWIN_FOR_CAMERA_PROP_KEY = "sys.multiwin_for_camera";
    private static final int PARAM_NUM_UPDATE_WALLPAPER = 1;
    public static final String SYSTEM_MANAGER_PERMISSION = "com.huawei.systemmanager.permission.ACCESS_INTERFACE";
    private static final String TAG = "HwMagicWindowService";
    private HwMagicModeBase mA1AnMode;
    /* access modifiers changed from: private */
    public PendingIntent mAlarmIntent;
    /* access modifiers changed from: private */
    public AlarmManager mAlarmManager;
    private final BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass6 */

        public void onReceive(Context context, Intent intent) {
            if (!HwMagicWindowService.this.isScreenOn()) {
                HwMagicWindowService.this.mHandler.sendMessage(HwMagicWindowService.this.mHandler.obtainMessage(4));
                HwMagicWindowService.this.mAlarmManager.cancel(HwMagicWindowService.this.mAlarmIntent);
                context.unregisterReceiver(this);
                return;
            }
            Slog.i(HwMagicWindowService.TAG, "Ignore updating config because screen on. Will be rescheduled at 4 oclock next day.");
        }
    };
    private ActivityManagerService mAms;
    /* access modifiers changed from: private */
    public HwMagicWinAmsPolicy mAmsPolicy = null;
    private HwMagicModeBase mAnAnMode;
    private BroadcastReceiver mAppConfigUpdateReceiver = new BroadcastReceiver() {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            Slog.i(HwMagicWindowService.TAG, "App Receive Config Update and loadAppAdapterInfo");
            if (intent != null) {
                Message msg = HwMagicWindowService.this.mHandler.obtainMessage(7);
                Bundle bundle = intent.getBundleExtra("params");
                if (bundle == null) {
                    Slog.w(HwMagicWindowService.TAG, "bundle of params is null");
                    return;
                }
                msg.obj = bundle.getString("client_name");
                HwMagicWindowService.this.mHandler.sendMessage(msg);
            }
        }
    };
    private HwMagicModeBase mBaseMode;
    private BroadcastReceiver mBdControlReceiver = new BroadcastReceiver() {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            HwMagicWindowService.this.mHandler.sendMessage(HwMagicWindowService.this.mHandler.obtainMessage(8, intent));
        }
    };
    /* access modifiers changed from: private */
    public String mCloudConfigUri = null;
    /* access modifiers changed from: private */
    public HwMagicWindowConfig mConfig = null;
    /* access modifiers changed from: private */
    public Context mContext = null;
    private HwFoldScreenManagerEx.FoldDisplayModeListener mDisplayListener = new HwFoldScreenManagerEx.FoldDisplayModeListener() {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass7 */

        public void onScreenDisplayModeChange(int displayMode) {
            Slog.d(HwMagicWindowService.TAG, "receive display mode change to: " + displayMode);
            if (HwMagicWindowService.this.mLastFoldMode != displayMode) {
                int unused = HwMagicWindowService.this.mLastFoldMode = displayMode;
                if (displayMode == 2 || displayMode == 3) {
                    HwMagicWindowService.this.mHandler.sendEmptyMessage(10);
                } else if (displayMode == 1) {
                    HwMagicWindowService.this.mHandler.sendEmptyMessage(11);
                }
            }
        }
    };
    public final Handler mHandler = new Handler(BackgroundThread.getHandler().getLooper()) {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass9 */

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    HwMagicWindowService.this.mUIController.getWallpaperBitmap();
                    return;
                case 2:
                    Slog.i(HwMagicWindowService.TAG, "case HwMwUtils.MSG_UPDATE_MAGIC_WINDOW_CONFIG:");
                    HwMagicWindowService.this.updateMagicWindowConfig();
                    return;
                case 3:
                    SystemProperties.set(HwMagicWindowService.MUlTIWIN_FOR_CAMERA_PROP_KEY, String.format(Locale.ROOT, "%d", msg.obj));
                    return;
                case 4:
                    Slog.i(HwMagicWindowService.TAG, "Update config at idle time.");
                    HwMagicWindowService.this.applyNewCloudConfig();
                    return;
                case 5:
                    Slog.i(HwMagicWindowService.TAG, "case HwMwUtils.MSG_USER_SWITCH:");
                    HwMagicWindowService.this.mConfig.onUserSwitch();
                    HwMagicWindowService.this.mAmsPolicy.calcHwSplitStackBounds();
                    HwMagicWindowService.this.mWmsPolicy.updateSettings();
                    HwMagicWindowService.this.registerEasyGoService();
                    return;
                case 6:
                    Slog.i(HwMagicWindowService.TAG, "case HwMwUtils.MSG_FORCE_STOP_PACKAGE:");
                    HwMagicWindowService.this.mAmsPolicy.removeRecentMagicWindowApp((String) msg.obj);
                    return;
                case 7:
                    Slog.i(HwMagicWindowService.TAG, "case HwMwUtils.MSG_UPDATE_APP_CONFIG:");
                    HwMagicWindowService.this.mConfig.loadAppAdapterInfo((String) msg.obj);
                    return;
                case 8:
                    if (msg.obj != null && (msg.obj instanceof Intent)) {
                        String action = ((Intent) msg.obj).getAction();
                        if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF.equals(action)) {
                            Slog.i(HwMagicWindowService.TAG, "Handle screen off");
                            HwMagicWinStatistics.getInstance().stopTick();
                            HwMagicWindowService.this.mAmsPolicy.sendMessageForSetMultiWinCameraProp(false);
                            HwMagicWindowService.this.mAmsPolicy.pauseTopWhenScreenOff();
                        }
                        if (SmartDualCardConsts.SYSTEM_STATE_NAME_ACTION_SHUTDOWN.equals(action)) {
                            Slog.i(HwMagicWindowService.TAG, "Handle shutdown");
                            HwMagicWinStatistics.getInstance().stopTick();
                            HwMagicWinStatistics.getInstance().saveCache();
                            return;
                        }
                        return;
                    }
                    return;
                case 9:
                    Slog.i(HwMagicWindowService.TAG, "case HwMwUtils.MSG_REPORT_USAGE_STATISTICS:");
                    HwMagicWinStatistics.getInstance().handleReport(HwMagicWindowService.this.mContext);
                    HwMagicWindowService.this.mHandler.sendEmptyMessageDelayed(9, HwMagicWindowService.CHECK_REPORT_MAGICWIN_USAGE_PERIOD);
                    return;
                case 10:
                    Slog.i(HwMagicWindowService.TAG, "case HwMwUtils.MSG_MOVE_LATEST_TO_TOP:");
                    HwMagicWindowService.this.mAmsPolicy.mModeSwitcher.moveLatestActivityToTop(true, false);
                    return;
                case 11:
                    Slog.d(HwMagicWindowService.TAG, "case HwMwUtils.MSG_BACK_TO_FOLD_FULL_DISPLAY:");
                    HwMagicWindowService.this.mAmsPolicy.mModeSwitcher.backToFoldFullDisplay();
                    return;
                case 12:
                    if (msg.obj instanceof ActivityRecord) {
                        HwMagicWindowService.this.mAmsPolicy.updateStackVisibility((ActivityRecord) msg.obj, false);
                        return;
                    }
                    return;
                case 13:
                    Slog.i(HwMagicWindowService.TAG, "case HwMwUtils.MSG_START_RELATE_ACT");
                    HwMagicWindowService.this.startRelateIntent(msg.obj, msg.arg1);
                    return;
                case 14:
                    if (msg.obj instanceof ActivityRecord) {
                        HwMagicWindowService.this.mAmsPolicy.mModeSwitcher.updateActivityToFullScreenConfiguration((ActivityRecord) msg.obj);
                        return;
                    }
                    return;
                case 15:
                    HwMagicWindowService.this.mConfig.writeSetting();
                    return;
                case 16:
                    HwMagicWindowService.this.updateSystemBoundSize();
                    return;
                case 17:
                    Slog.i(HwMagicWindowService.TAG, "case HwMwUtils.MSG_SERVICE_INIT");
                    HwMagicWindowService.this.init();
                    return;
                case 18:
                    HwMagicWindowService.this.processLocaleChange();
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mIsCloudConfigChanged = false;
    private boolean mIsInited = false;
    /* access modifiers changed from: private */
    public int mLastFoldMode = 0;
    private BroadcastReceiver mLocaleChangeReceiver = new BroadcastReceiver() {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass8 */

        public void onReceive(Context context, Intent intent) {
            boolean isRtl = true;
            if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) != 1) {
                isRtl = false;
            }
            if (HwMagicWindowService.this.mConfig.isRtl() != isRtl) {
                Slog.i(HwMagicWindowService.TAG, "LocaleChanged, isRtl changed to " + isRtl);
                HwMagicWindowService.this.mConfig.setIsRtl(isRtl);
                HwMagicWindowService.this.mHandler.sendEmptyMessage(18);
            }
        }
    };
    private HwMagicModeBase mOpenMode;
    private PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public HwMagicWindowUIController mUIController = null;
    private BroadcastReceiver mUnistallReciver = new BroadcastReceiver() {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass5 */

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null || intent.getData() == null) {
                Slog.e(HwMagicWindowService.TAG, "UnistallReciver context or intent or data is null");
                return;
            }
            String pkgName = intent.getData().getSchemeSpecificPart();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", 0);
            if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && !intent.getBooleanExtra("android.intent.extra.REPLACING", false) && !UserHandle.isClonedProfile(userId)) {
                HwMagicWindowService.this.mConfig.removeSettingConfig(pkgName);
                Slog.d(HwMagicWindowService.TAG, "removeSettingConfig pkgName " + pkgName + " id " + userId);
            }
            HwMagicWindowService.this.mConfig.removeReportLoginStatus(HwMagicWindowService.this.mAmsPolicy.getJoinStr(pkgName, userId));
        }
    };
    private final BroadcastReceiver mUpdateMagicWindowConfigBdReceiver = new BroadcastReceiver() {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            String unused = HwMagicWindowService.this.mCloudConfigUri = intent.getStringExtra(HwMagicWindowService.CONFIG_URI);
            Slog.i(HwMagicWindowService.TAG, "mCloudConfigUri: " + HwMagicWindowService.this.mCloudConfigUri);
            if (HwMagicWindowService.this.mCloudConfigUri != null) {
                HwMagicWindowService.this.mHandler.sendMessage(HwMagicWindowService.this.mHandler.obtainMessage(2));
            }
        }
    };
    private BroadcastReceiver mUserSwitchReceiver = new BroadcastReceiver() {
        /* class com.android.server.magicwin.HwMagicWindowService.AnonymousClass4 */

        public void onReceive(Context context, Intent intent) {
            HwMagicWindowService.this.mHandler.sendMessage(HwMagicWindowService.this.mHandler.obtainMessage(5));
        }
    };
    private WindowManagerService mWms;
    /* access modifiers changed from: private */
    public HwMagicWinWmsPolicy mWmsPolicy = null;

    private void registerLocaleChangeReceiver() {
        this.mContext.registerReceiverAsUser(this.mLocaleChangeReceiver, UserHandle.CURRENT, new IntentFilter("android.intent.action.LOCALE_CHANGED"), null, null);
    }

    private void registerFoldDisplayReceiver() {
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
            HwFoldScreenManagerEx.registerFoldDisplayMode(this.mDisplayListener);
        }
    }

    private void registerBdControlReceiver() {
        IntentFilter bdControlFilter = new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
        bdControlFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_ACTION_SHUTDOWN);
        this.mContext.registerReceiver(this.mBdControlReceiver, bdControlFilter);
    }

    private void registerAppConfigUpdateReceiver() {
        try {
            if (this.mIsInited) {
                this.mContext.unregisterReceiver(this.mAppConfigUpdateReceiver);
            }
            IntentFilter appConfigUpdateFilter = new IntentFilter();
            appConfigUpdateFilter.addAction(APP_CONFIG_UPDATE_ACTION);
            appConfigUpdateFilter.addDataScheme("package");
            appConfigUpdateFilter.addDataSchemeSpecificPart("magicwin", 0);
            this.mContext.registerReceiverAsUser(this.mAppConfigUpdateReceiver, UserHandle.CURRENT, appConfigUpdateFilter, EASYGO_PERMISSION_SEND_SYS_BCST, null);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "register AppConfig Update fail");
        }
    }

    private void registerUninstallReceiver() {
        IntentFilter uninstallFilter = new IntentFilter();
        uninstallFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        uninstallFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mUnistallReciver, UserHandle.ALL, uninstallFilter, null, null);
    }

    private void registerUserSwitchReceiver() {
        IntentFilter userSwitchFilter = new IntentFilter();
        userSwitchFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_ACTION_USER_SWITCHED);
        this.mContext.registerReceiver(this.mUserSwitchReceiver, userSwitchFilter);
    }

    public HwMagicWindowService(Context context, ActivityManagerService ams, WindowManagerService wms) {
        this.mAms = ams;
        this.mWms = wms;
        this.mContext = context;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mHandler.sendEmptyMessage(17);
        registerUserSwitchReceiver();
    }

    /* access modifiers changed from: private */
    public void init() {
        this.mConfig = new HwMagicWindowConfig(this.mContext);
        this.mUIController = new HwMagicWindowUIController(this, this.mContext, this.mAms.mActivityTaskManager);
        this.mAmsPolicy = new HwMagicWinAmsPolicy(this, this.mContext, this.mAms);
        this.mWmsPolicy = new HwMagicWinWmsPolicy(this, this.mContext, this.mWms);
        this.mBaseMode = new HwMagicModeBase(this, this.mContext);
        this.mA1AnMode = new HwMagicModeA1An(this, this.mContext);
        this.mAnAnMode = new HwMagicModeAnAn(this, this.mContext);
        this.mOpenMode = new HwMagicModeOpen(this, this.mContext);
        HwMagicWinStatistics.getInstance().loadCache();
        this.mHandler.sendEmptyMessage(1);
        this.mHandler.sendEmptyMessageDelayed(9, CHECK_REPORT_MAGICWIN_USAGE_PERIOD);
        registerReceivers();
        registerForHwMultiWindow(this.mAms);
        registerUninstallReceiver();
        registerBdControlReceiver();
        registerFoldDisplayReceiver();
        registerEasyGoService();
        registerLocaleChangeReceiver();
        this.mIsInited = true;
        Slog.e(TAG, "service init completed!");
    }

    private void registerForHwMultiWindow(ActivityManagerService ams) {
        HwMultiWindowManager hwMultiWindowManager = HwMultiWindowManager.getInstance(ams.mActivityTaskManager);
        if (hwMultiWindowManager == null) {
            Slog.w(TAG, "registerForHwMultiWindow failed, cause hwMultiWindowManager is null!");
        } else {
            hwMultiWindowManager.setHwMagicWindowService(this);
        }
    }

    private void enforceSystemUid() {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only available to system processes");
        }
    }

    public Bitmap getWallpaperScreenShot() {
        return this.mUIController.getWallpaperScreenShot();
    }

    /* access modifiers changed from: private */
    public void registerEasyGoService() {
        if (isSupportOpenCapability()) {
            registerEasyGo();
            registerAppConfigUpdateReceiver();
            this.mConfig.loadAppAdapterInfo("*");
        }
    }

    private void registerEasyGo() {
        Bundle reply;
        Slog.i(TAG, "registerEasyGo");
        Bundle bundle = new Bundle();
        bundle.putString(EASYGO_REGISTER_FUNCTION_KEY, EASYGO_REGISTER_FUNCTION_VALUE);
        bundle.putString("server_data_schema", "package:magicwin");
        bundle.putString("service_name", TAG);
        bundle.putString("service_version", "1.0");
        try {
            Context context = this.mContext.createPackageContextAsUser("com.huawei.systemserver", 0, UserHandle.of(this.mAms.getCurrentUser().id));
            if (context != null && (reply = context.getContentResolver().call(Uri.parse("content://com.huawei.easygo.easygoprovider/v_function"), "register_server", (String) null, bundle)) != null) {
                Slog.i(TAG, "registerEasyGo reply " + reply.toString());
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "register EasyGo no package context");
        } catch (IllegalArgumentException e2) {
            Slog.e(TAG, "registerEasyGo fail " + e2);
        }
    }

    private void registerReceivers() {
        IntentFilter updateCfgFilter = new IntentFilter();
        updateCfgFilter.addAction(ACTION_FROM_SYSTEMMANAGER);
        this.mContext.registerReceiverAsUser(this.mUpdateMagicWindowConfigBdReceiver, UserHandle.ALL, updateCfgFilter, SYSTEM_MANAGER_PERMISSION, null);
    }

    private void registerAlarmUpdateReceiver() {
        IntentFilter alarmUpdateFilter = new IntentFilter();
        alarmUpdateFilter.addAction(ALARM_UPDATE_CONFIG_ACTION);
        this.mContext.registerReceiver(this.mAlarmReceiver, alarmUpdateFilter, DELAYED_UPDATE_CONFIG_PERMISSION, null);
    }

    public HwMagicWindowConfig getConfig() {
        return this.mConfig;
    }

    /* access modifiers changed from: private */
    public void startRelateIntent(Object obj, int userrId) {
        Context context = this.mContext;
        if (context != null && (obj instanceof Intent)) {
            try {
                context.startActivityAsUser((Intent) obj, UserHandle.of(userrId));
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "can't find the relate actvity");
            } catch (Exception e2) {
                Slog.e(TAG, "start relate actvity exception");
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:50:0x008d A[FALL_THROUGH] */
    public Bundle performHwMagicWindowPolicy(int policy, List params) {
        if (!this.mIsInited) {
            return Bundle.EMPTY;
        }
        Bundle result = new Bundle();
        if (!(policy == 0 || policy == 1 || policy == 2 || policy == 3 || policy == 4 || policy == 5 || policy == 6 || policy == 7)) {
            if (policy != 20) {
                if (policy != 21) {
                    if (!(policy == 31 || policy == 32)) {
                        if (policy != 51 && policy != 52) {
                            if (!(policy == 61 || policy == 62 || policy == 132 || policy == 133)) {
                                switch (policy) {
                                    case 7:
                                    case 9:
                                    case 10:
                                    case 28:
                                    case 41:
                                    case 80:
                                        break;
                                    case 8:
                                        updateMagicWindowWallpaperVisibility(params);
                                        break;
                                    case 25:
                                    case 128:
                                    case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED:
                                        break;
                                    default:
                                        switch (policy) {
                                            default:
                                                switch (policy) {
                                                    case 70:
                                                    case 71:
                                                        break;
                                                    case 72:
                                                        break;
                                                    default:
                                                        switch (policy) {
                                                            case 101:
                                                            case 102:
                                                            case 103:
                                                            case 104:
                                                            case 105:
                                                            case 106:
                                                            case 107:
                                                                break;
                                                            default:
                                                                switch (policy) {
                                                                    case CPUFeature.MSG_SET_BOOST_CPUS /*{ENCODED_INT: 135}*/:
                                                                    case CPUFeature.MSG_RESET_BOOST_CPUS /*{ENCODED_INT: 136}*/:
                                                                    case CPUFeature.MSG_SET_LIMIT_CGROUP /*{ENCODED_INT: 137}*/:
                                                                        break;
                                                                    default:
                                                                        Slog.e(TAG, "policy type error : " + policy);
                                                                        break;
                                                                }
                                                            case HwAPPQoEUtils.MSG_APP_STATE_UNKNOW:
                                                                this.mAmsPolicy.performHwMagicWindowPolicy(policy, params, result);
                                                                break;
                                                        }
                                                }
                                            case 13:
                                            case 14:
                                            case 15:
                                            case 16:
                                            case 17:
                                            case 18:
                                                break;
                                        }
                                }
                            }
                        } else {
                            this.mAmsPolicy.performHwMagicWindowPolicy(policy, params, result);
                            this.mWmsPolicy.performHwMagicWindowPolicy(policy, params, result);
                        }
                    }
                }
                this.mWmsPolicy.performHwMagicWindowPolicy(policy, params, result);
            } else {
                this.mWmsPolicy.getRatio(params, result);
            }
            return result;
        }
        this.mAmsPolicy.performHwMagicWindowPolicy(policy, params, result);
        return result;
    }

    public HwMagicWinWmsPolicy getWmsPolicy() {
        return this.mWmsPolicy;
    }

    public HwMagicWinAmsPolicy getAmsPolicy() {
        return this.mAmsPolicy;
    }

    public HwMagicWindowUIController getUIController() {
        return this.mUIController;
    }

    public HwMagicModeBase getMode(String pkg) {
        int windowMode = this.mConfig.getWindowMode(pkg);
        if (windowMode == 1) {
            return this.mA1AnMode;
        }
        if (windowMode == 2) {
            return this.mAnAnMode;
        }
        if (windowMode != 3) {
            return this.mBaseMode;
        }
        return this.mOpenMode;
    }

    public HwMagicModeBase getBaseMode() {
        return this.mBaseMode;
    }

    public boolean isSupportMultiResume(String packageName) {
        return isSupportMultiResumeMetaData(packageName) && getHwMagicWinEnabled(packageName);
    }

    private boolean isSupportMultiResumeMetaData(String packageName) {
        try {
            ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 128);
            if (appInfo.metaData == null || appInfo.metaData.getBoolean(KEY_MULTI_RESUME, true)) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "isSupportMultiResume:package name no found !");
            return false;
        }
    }

    public boolean isScaled(String pkgName) {
        return this.mConfig.isScaled(pkgName);
    }

    public float getRatio(String pkgName) {
        return this.mConfig.getRatio(pkgName);
    }

    public Rect getBounds(int position, String pkgName) {
        return this.mConfig.getBounds(position, pkgName);
    }

    public Rect getBounds(int position, boolean isScaled) {
        return this.mConfig.getBounds(position, isScaled);
    }

    public boolean isMaster(ActivityRecord ar) {
        return checkPosition(ar, 1);
    }

    public boolean isSlave(ActivityRecord ar) {
        return checkPosition(ar, 2);
    }

    public boolean isMiddle(ActivityRecord ar) {
        return checkPosition(ar, 3);
    }

    public boolean isFull(ActivityRecord ar) {
        return checkPosition(ar, 5);
    }

    private boolean checkPosition(ActivityRecord ar, int pos) {
        if (ar != null && getBoundsPosition(ar.getRequestedOverrideBounds()) == pos) {
            return true;
        }
        return false;
    }

    public boolean checkPosition(Rect bound, int pos) {
        return getBoundsPosition(bound) == pos;
    }

    public int getBoundsPosition(Rect bounds) {
        if (bounds == null || getAmsPolicy().mFullScreenBounds.equals(bounds) || getAmsPolicy().mDefaultFullScreenBounds.equals(bounds)) {
            HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mAmsPolicy;
            String pkgName = hwMagicWinAmsPolicy.getPackageName(hwMagicWinAmsPolicy.getTopActivity());
            if (bounds == null || !this.mConfig.isSupportAppTaskSplitScreen(pkgName)) {
                return 5;
            }
            return 3;
        }
        int posSplite = this.mConfig.getBoundPosition(bounds, 0);
        if (posSplite != 0) {
            return posSplite;
        }
        return 0;
    }

    public int getAppSupportMode(String packageName) {
        return this.mConfig.getWindowMode(packageName);
    }

    public boolean isAppSupportMagicWin(String packageName) {
        return getAppSupportMode(packageName) >= 0;
    }

    public boolean isSupportAnimation() {
        return this.mConfig.isSystemSupport(1);
    }

    public boolean isSupportOpenCapability() {
        return this.mConfig.isSystemSupport(3);
    }

    public boolean isHomePage(String pkg, String component) {
        String[] homes;
        if (component == null || component.isEmpty() || (homes = this.mConfig.getHomes(pkg)) == null) {
            return false;
        }
        for (String home : homes) {
            if (home != null && !home.isEmpty() && home.equals(component)) {
                return true;
            }
        }
        return false;
    }

    public boolean isJudgeHost(String pkg, String component) {
        String home;
        if (component == null || component.isEmpty() || (home = this.mConfig.getJudgeHost(pkg)) == null || !home.equals(component)) {
            return false;
        }
        return true;
    }

    public void setHost(String pkg, String homeActivity) {
        this.mConfig.createHost(pkg, homeActivity);
    }

    public boolean isNeedDect(String pkg) {
        if (!this.mConfig.getHwMagicWinEnabled(pkg) || isSupportOpenMode(pkg)) {
            return false;
        }
        return this.mConfig.isNeedDect(pkg);
    }

    public boolean isSupportAnAnMode(String pkg) {
        return 2 == this.mConfig.getWindowMode(pkg);
    }

    public boolean isSupportOpenMode(String pkg) {
        return this.mConfig.getWindowMode(pkg) == 3;
    }

    public boolean isReLaunchWhenResize(String pkg) {
        return this.mConfig.isReLaunchWhenResize(pkg);
    }

    public boolean isSupportFullScreenVideo(String packageName) {
        return this.mConfig.isVideoFullscreen(packageName);
    }

    public Map<String, Boolean> getHwMagicWinEnabledApps() {
        if (this.mIsInited) {
            return this.mConfig.getHwMagicWinEnabledApps();
        }
        Slog.w(TAG, "getHwMagicWinEnabledApps, service is not ready!");
        return new HashMap();
    }

    public boolean setHwMagicWinEnabled(String pkg, boolean isEnabled) {
        Slog.d(TAG, "setHwMagicWinEnabled, pkg = " + pkg + ", enabled = " + isEnabled);
        if (pkg == null || !this.mIsInited) {
            return false;
        }
        enforceSystemUid();
        this.mConfig.onAppSwitchChanged(pkg, isEnabled);
        Message msg = this.mHandler.obtainMessage(6);
        msg.obj = pkg;
        this.mHandler.sendMessage(msg);
        return true;
    }

    public boolean getHwMagicWinEnabled(String pkg) {
        return this.mIsInited && this.mConfig.getHwMagicWinEnabled(pkg);
    }

    public boolean getDialogShownForApp(String pkg) {
        return this.mConfig.getDialogShownForApp(pkg);
    }

    public boolean setDialogShownForApp(String pkg, boolean isDialogShown) {
        Slog.d(TAG, "setDialogShownForApp, pkg = " + pkg + ", isDialogShown = " + isDialogShown);
        if (pkg == null) {
            return false;
        }
        this.mConfig.onAppDialogShown(pkg, isDialogShown);
        return true;
    }

    /* access modifiers changed from: private */
    public void updateMagicWindowConfig() {
        String[] fromFileArray = {this.mCloudConfigUri + "/" + HwMagicWindowConfigLoader.CLOUD_PACKAGE_CONFIG_FILE_NAME};
        String[] toFileArray = {"/data/system/magicWindowFeature_magic_window_application_list.xml"};
        int copyCnt = fromFileArray.length < toFileArray.length ? fromFileArray.length : toFileArray.length;
        for (int i = 0; i < copyCnt; i++) {
            File file = new File(fromFileArray[i]);
            if (file.exists()) {
                try {
                    FileUtils.copy(file, new File(toFileArray[i]));
                    Slog.i(TAG, "copy " + fromFileArray[i] + " to " + toFileArray[i]);
                    this.mIsCloudConfigChanged = true;
                } catch (IOException e) {
                    Slog.e(TAG, "FileUtils.copy from: " + fromFileArray[i] + " to: " + toFileArray[i], e);
                }
            } else {
                Slog.w(TAG, "Cloud update cache file not exist.");
            }
        }
        if (HwMwUtils.ENABLED && this.mIsCloudConfigChanged) {
            if (!isScreenOn()) {
                Slog.i(TAG, "update config immediately.");
                applyNewCloudConfig();
                return;
            }
            registerAlarmUpdateReceiver();
            sendAlarmUpdateConfiguraion();
        }
    }

    /* access modifiers changed from: private */
    public void applyNewCloudConfig() {
        this.mAmsPolicy.removeCachedMagicWindowApps(this.mConfig.onCloudUpdate());
        this.mIsCloudConfigChanged = false;
    }

    private void sendAlarmUpdateConfiguraion() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(11);
        int dayOfYear = calendar.get(6);
        if (hourOfDay > 4) {
            dayOfYear++;
        }
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        calendar.set(11, 4);
        calendar.set(6, dayOfYear);
        this.mAlarmIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ALARM_UPDATE_CONFIG_ACTION), 0);
        Slog.i(TAG, "Will update config at day " + dayOfYear + " " + 4 + " oclock.");
        this.mAlarmManager.setRepeating(0, calendar.getTimeInMillis(), 86400000, this.mAlarmIntent);
    }

    /* access modifiers changed from: private */
    public boolean isScreenOn() {
        return this.mPowerManager.isScreenOn();
    }

    /* access modifiers changed from: private */
    public void processLocaleChange() {
        this.mConfig.updateSystemBoundSize();
        this.mConfig.updateDragModeForLocaleChange();
        this.mAmsPolicy.sendMsgToWriteSettingsXml();
        this.mAmsPolicy.calcHwSplitStackBounds();
        this.mAmsPolicy.mModeSwitcher.changeLayoutDirection(this.mConfig.isRtl());
    }

    public boolean needRelaunch(String packageName) {
        return this.mConfig.needRelaunch(packageName);
    }

    private void updateMagicWindowWallpaperVisibility(List params) {
        if (((ArrayList) params).size() != 1) {
            Slog.e(TAG, "Err : performOverrideIntentPolicy params count : " + params.size());
        } else if (params.get(0) instanceof Boolean) {
            this.mUIController.updateMagicWindowWallpaperVisibility((Boolean) params.get(0));
        } else {
            Slog.e(TAG, "Err : updateMagicWindowWallpaperVisibility params error params.get(0)=" + params.get(0));
        }
    }

    public void changeWallpaper(boolean isMiddle) {
        this.mUIController.changeWallpaper(isMiddle);
    }

    public void updateSystemBoundSize() {
        this.mConfig.updateSystemBoundSize();
        this.mUIController.setNeedUpdateWallpaperSize(true);
        Map<String, Boolean> enableMagicApps = new HashMap<>();
        this.mConfig.getHwMagicWinSettingConfigs().forEach(new BiConsumer(enableMagicApps) {
            /* class com.android.server.magicwin.$$Lambda$HwMagicWindowService$oaWjFig3ZgOy1puCAznV4CU0d0 */
            private final /* synthetic */ Map f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                HwMagicWindowService.lambda$updateSystemBoundSize$0(this.f$0, (String) obj, (HwMagicWindowConfig.SettingConfig) obj2);
            }
        });
        Set<String> tempApps = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : enableMagicApps.entrySet()) {
            if (entry != null && entry.getValue().booleanValue()) {
                tempApps.add(entry.getKey());
            }
        }
        this.mAmsPolicy.removeCachedMagicWindowApps(tempApps);
    }

    static /* synthetic */ void lambda$updateSystemBoundSize$0(Map enableMagicApps, String k, HwMagicWindowConfig.SettingConfig v) {
        Boolean bool = (Boolean) enableMagicApps.put(k, Boolean.valueOf(v.getHwMagicWinEnabled()));
    }

    public Bundle invokeSync(String packageName, String method, String params, Bundle objects) {
        Slog.i(TAG, "EasyGo Binder invokeSync packageName: " + packageName + " method " + method + " params " + params);
        Bundle result = new Bundle();
        if (method == null || packageName == null || params == null) {
            return result;
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            if (method.equals(FUNC_SET_LOGIN_STATUS)) {
                this.mAmsPolicy.setLoginStatus(packageName, this.mConfig.parseIntParama(params, EASYGO_LOGIN_STATUS_KEY), uid);
                return result;
            }
            int taskId = this.mConfig.parseIntParama(params, EASYGO_TASK_ID_KEY);
            if (taskId < 0) {
                Binder.restoreCallingIdentity(ident);
                return result;
            }
            if (method.equals(FUNC_SET_TASK_POS)) {
                this.mAmsPolicy.setTaskPosition(packageName, taskId, this.mConfig.parseIntParama(params, EASYGO_TARGET_POS_KEY));
            } else if (method.equals(FUNC_GET_TASK_POS)) {
                result.putInt(BUNDLE_GET_POS_INT, this.mAmsPolicy.getTaskPosition(packageName, taskId));
            } else if (method.equals(FUNC_IS_IN_MAGIC_WIN_MODE)) {
                result.putBoolean(BUNDLE_IS_IN_MAGIC_WIN_MODE, this.mAmsPolicy.isInMagicWindowMode(taskId));
            } else {
                Slog.e(TAG, "no such funtion");
            }
            Binder.restoreCallingIdentity(ident);
            return result;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void invokeAsync(String packageName, String method, String params, Bundle objects, IBinder callback) {
        Slog.i(TAG, "EasyGo Binder invokeAsync packageName: " + packageName + " method " + method);
    }

    public boolean isInAppSplitWinMode(ActivityRecord ar) {
        return this.mAmsPolicy.mMagicWinSplitMng.isPkgSpliteScreenMode(ar, false);
    }

    public ActivityStack getNewTopStack(ActivityStack oldStack, int otherSideModeToChange) {
        return this.mAmsPolicy.mMagicWinSplitMng.getNewTopStack(oldStack, otherSideModeToChange);
    }

    public void addOtherSnapShot(ActivityStack stack, HwActivityTaskManagerServiceEx HwAtmsEx, List<ActivityManager.TaskSnapshot> snapShots) {
        this.mAmsPolicy.mMagicWinSplitMng.addOtherSnapShot(stack, HwAtmsEx, snapShots);
    }
}
