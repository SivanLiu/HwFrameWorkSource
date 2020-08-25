package com.android.server.notification;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hdm.HwDeviceManager;
import android.media.IRingtonePlayer;
import android.net.Uri;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.CollectData;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SplitNotificationUtils;
import android.view.IWindowManager;
import android.widget.RemoteViews;
import com.android.server.HwBluetoothBigDataService;
import com.android.server.HwBluetoothManagerServiceEx;
import com.android.server.LocalServices;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.lights.LightsService;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.PreferencesHelper;
import com.android.server.pm.UserManagerService;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.security.hsm.HwAddViewHelper;
import com.android.server.security.trustspace.TrustSpaceManagerInternal;
import com.android.server.statusbar.HwStatusBarManagerService;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.HwSnsVideoManager;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.app.IGameObserver;
import com.huawei.recsys.aidl.HwRecSysAidlInterface;
import huawei.com.android.server.fingerprint.FingerViewController;
import huawei.cust.HwCustUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HwNotificationManagerService extends NotificationManagerService {
    private static final String ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE = "huawei.intent.action.HWSYSTEMMANAGER_CHANGE_POWERMODE";
    private static final String ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE = "huawei.intent.action.HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE";
    private static final String BIND_ACTION = "com.huawei.recsys.action.THIRD_REQUEST_ENGINE";
    private static final long BIND_TIMEOUT = 10000;
    private static final int CLOSE_SAVE_POWER = 0;
    private static final String CONTACTS_PKGNAME = "com.android.contacts";
    private static final String CONTACTS_PKGNAME_HW = "com.huawei.contacts";
    private static final String CONTENTVIEW_REVERTING_FLAG = "HW_CONTENTVIEW_REVERTING_FLAG";
    private static final boolean CUST_DIALER_ENABLE = SystemProperties.get("ro.product.custom", HwBluetoothManagerServiceEx.DEFAULT_PACKAGE_NAME).contains("docomo");
    private static final boolean DEBUG = true;
    private static final String DIALER_PKGNAME = "com.android.dialer";
    private static final boolean DISABLE_MULTIWIN = SystemProperties.getBoolean("ro.huawei.disable_multiwindow", false);
    private static final int EVENT_MARK_AS_GAME = 4;
    private static final int EVENT_MOVE_BACKGROUND = 2;
    private static final int EVENT_MOVE_FRONT = 1;
    private static final int EVENT_REPLACE_FRONT = 3;
    public static final List<String> EXPANDEDNTF_PKGS = new ArrayList();
    private static final boolean HWMULTIWIN_ENABLED = SystemProperties.getBoolean("ro.config.hw_multiwindow_optimization", false);
    private static final boolean HWRIDEMODE_FEATURE_SUPPORTED = SystemProperties.getBoolean("ro.config.ride_mode", false);
    static final int INIT_CFG_DELAY = 10000;
    private static final int IS_TOP_FULL_SCREEN_TOKEN = 206;
    private static final String KEY_SMART_NOTIFICATION_SWITCH = "smart_notification_switch";
    private static final String KEY_TRUST_SPACE_BADGE_SWITCH = "trust_secure_hint_enable";
    private static final String MMS_PKGNAME = "com.android.mms";
    private static final int MORE_PRIORITY = 5;
    private static final String NOTIFICATION_ACTION_ALLOW = "com.huawei.notificationmanager.notification.allow";
    private static final String NOTIFICATION_ACTION_REFUSE = "com.huawei.notificationmanager.notification.refuse";
    private static final String NOTIFICATION_CENTER_ORIGIN_PKG = "hw_origin_sender_package_name";
    private static final String NOTIFICATION_CENTER_PKG = "com.huawei.android.pushagent";
    private static final String[] NOTIFICATION_NARROW_DISPLAY_APPS = {"com.huawei.contacts", "com.android.contacts"};
    private static final String[] NOTIFICATION_WHITE_APPS_PACKAGE = {"com.huawei.intelligent"};
    private static final int OPEN_SAVE_POWER = 3;
    private static final String PACKAGE_NAME_HSM = "com.huawei.systemmanager";
    private static final String PERMISSION = "com.huawei.android.launcher.permission.CHANGE_POWERMODE";
    private static final String PHONE_PKGNAME = "com.android.phone";
    private static final String POWER_MODE = "power_mode";
    private static final String POWER_SAVER_NOTIFICATION_WHITELIST = "super_power_save_notification_whitelist";
    private static final String RIDEMODE_NOTIFICATION_WHITE_LIST = "com.huawei.ridemode,com.huawei.sos,com.android.phone,com.android.server.telecom,com.android.incallui,com.android.deskclock,com.android.cellbroadcastreceiver,com.huawei.meetime,com.huawei.deskclock";
    private static final String SERVER_PAKAGE_NAME = "com.huawei.recsys";
    private static final String SHUTDOWN_LIMIT_POWERMODE = "shutdomn_limit_powermode";
    static final String TAG = "HwNotificationService";
    private static final String TELECOM_PKGNAME = "com.android.server.telecom";
    private static final long TIME_NOT_BIND_LIMIT = 300000;
    private static final int TRUST_SPACE_BADGE_STATUS_ENABLE = 1;
    private static final String TYPE_COUNSELING_MESSAGE = "102";
    private static final String TYPE_IGNORE = "0";
    private static final String TYPE_INFORMATION = "3";
    private static final int TYPE_ISCHINA = 1;
    private static final String TYPE_MUSIC = "103";
    private static final int TYPE_NOTCHINA = 0;
    private static final String TYPE_PROMOTION = "2";
    private static final String TYPE_TOOLS = "107";
    private static final String WECHAT_HONGBAO = "[微信红包]";
    /* access modifiers changed from: private */
    public static final boolean mIsChina = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    final Uri URI_NOTIFICATION_CFG;
    private BatteryManagerInternal mBatteryManagerInternal;
    private Runnable mBindRunable;
    private List<String> mBtwList;
    DBContentObserver mCfgDBObserver;
    Map<String, Integer> mCfgMap;
    private ContentObserver mContentObserver;
    Context mContext;
    private HwCustZenModeHelper mCust;
    private BroadcastReceiver mHangButtonReceiver;
    private HwGameObserver mHwGameObserver;
    Handler mHwHandler;
    private final HwStatusBarManagerService.HwNotificationDelegate mHwNotificationDelegate;
    /* access modifiers changed from: private */
    public HwRecSysAidlInterface mHwRecSysAidlInterface;
    private long mLastBindTime;
    /* access modifiers changed from: private */
    public final Object mLock;
    /* access modifiers changed from: private */
    public final ArrayList<NotificationContentViewRecord> mOriginContentViews;
    /* access modifiers changed from: private */
    public PowerSaverObserver mPowerSaverObserver;
    /* access modifiers changed from: private */
    public Handler mRecHandler;
    private HandlerThread mRecHandlerThread;
    private final ArrayMap<String, String> mRecognizeMap;
    private ServiceConnection mServiceConnection;
    /* access modifiers changed from: private */
    public final ArrayMap<Integer, Boolean> mSmartNtfSwitchMap;
    private final ArrayMap<String, NotificationRecord> mUpdateEnqueuedNotifications;
    private String[] noitfication_white_list;
    private String[] plus_notification_white_list;
    private final BroadcastReceiver powerReceiver;
    private HashSet<String> power_save_whiteSet;

    static {
        EXPANDEDNTF_PKGS.add("com.android.incallui");
        EXPANDEDNTF_PKGS.add("com.android.deskclock");
        EXPANDEDNTF_PKGS.add("com.huawei.meetime");
        EXPANDEDNTF_PKGS.add("com.huawei.deskclock");
    }

    private StatusBarManagerService getStatusBarManagerService() {
        return ServiceManager.getService("statusbar");
    }

    public void onStart() {
        HwNotificationManagerService.super.onStart();
        this.mBatteryManagerInternal = (BatteryManagerInternal) getLocalService(BatteryManagerInternal.class);
        IntentFilter powerFilter = new IntentFilter();
        powerFilter.addAction(ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE);
        powerFilter.addAction(ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE);
        getContext().registerReceiver(this.powerReceiver, powerFilter, PERMISSION, null);
    }

    public HwNotificationManagerService(Context context, StatusBarManagerService statusBar, LightsService lights) {
        super(context);
        this.mHwHandler = null;
        this.mCfgDBObserver = null;
        this.mCfgMap = new HashMap();
        this.URI_NOTIFICATION_CFG = Uri.parse("content://com.huawei.systemmanager.NotificationDBProvider/notificationCfg");
        this.mLock = new Object();
        this.mLastBindTime = 0;
        this.mRecognizeMap = new ArrayMap<>();
        this.mSmartNtfSwitchMap = new ArrayMap<>();
        this.mHwGameObserver = null;
        this.mCust = (HwCustZenModeHelper) HwCustUtils.createObj(HwCustZenModeHelper.class, new Object[0]);
        this.mUpdateEnqueuedNotifications = new ArrayMap<>();
        this.noitfication_white_list = new String[]{"com.huawei.message", MMS_PKGNAME, "com.huawei.contacts", "com.android.contacts", PHONE_PKGNAME, "com.android.deskclock", "com.android.calendar", "com.huawei.calendar", FingerViewController.PKGNAME_OF_KEYGUARD, AppStartupDataMgr.HWPUSH_PKGNAME, "com.android.incallui", "com.android.phone.recorder", "com.android.cellbroadcastreceiver", TELECOM_PKGNAME, "com.huawei.meetime", "com.huawei.deskclock"};
        this.plus_notification_white_list = new String[]{HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME};
        this.power_save_whiteSet = new HashSet<>();
        this.mBtwList = new ArrayList();
        this.mBtwList.add("2");
        this.mBtwList.add("3");
        this.mBtwList.add("102");
        this.mBtwList.add(TYPE_TOOLS);
        this.mHwNotificationDelegate = new HwStatusBarManagerService.HwNotificationDelegate() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass1 */

            @Override // com.android.server.statusbar.HwStatusBarManagerService.HwNotificationDelegate
            public void onNotificationResidentClear(String pkg, String tag, int id, int userId) {
                HwNotificationManagerService.this.cancelNotification(Binder.getCallingUid(), Binder.getCallingPid(), pkg, tag, id, 0, 0, true, userId, 2, null);
            }
        };
        this.mHangButtonReceiver = new BroadcastReceiver() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null) {
                    String action = intent.getAction();
                    if (HwNotificationManagerService.NOTIFICATION_ACTION_ALLOW.equals(action)) {
                        String pkg = intent.getStringExtra("pkgName");
                        int uid = intent.getIntExtra("uid", -1);
                        if (pkg != null && uid != -1) {
                            HwNotificationManagerService.this.recoverNotificationContentView(pkg, uid);
                        }
                    } else if (action != null && action.equals(HwNotificationManagerService.NOTIFICATION_ACTION_REFUSE)) {
                        String pkg2 = intent.getStringExtra("pkgName");
                        int uid2 = intent.getIntExtra("uid", -1);
                        if (pkg2 != null && uid2 != -1) {
                            synchronized (HwNotificationManagerService.this.mOriginContentViews) {
                                Iterator<NotificationContentViewRecord> itor = HwNotificationManagerService.this.mOriginContentViews.iterator();
                                while (itor.hasNext()) {
                                    NotificationContentViewRecord cvr = itor.next();
                                    if (cvr.pkg.equals(pkg2) && cvr.uid == uid2) {
                                        itor.remove();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
        this.mOriginContentViews = new ArrayList<>();
        this.mBindRunable = new Runnable() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass3 */

            public void run() {
                HwNotificationManagerService.this.bind();
            }
        };
        this.mServiceConnection = new ServiceConnection() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass4 */

            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(HwNotificationManagerService.TAG, "onServiceConnected");
                HwRecSysAidlInterface unused = HwNotificationManagerService.this.mHwRecSysAidlInterface = HwRecSysAidlInterface.Stub.asInterface(service);
                synchronized (HwNotificationManagerService.this.mLock) {
                    HwNotificationManagerService.this.mLock.notifyAll();
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.i(HwNotificationManagerService.TAG, "onServiceDisConnected");
                HwRecSysAidlInterface unused = HwNotificationManagerService.this.mHwRecSysAidlInterface = null;
                synchronized (HwNotificationManagerService.this.mLock) {
                    HwNotificationManagerService.this.mLock.notifyAll();
                }
            }
        };
        this.mContentObserver = new ContentObserver(null) {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass5 */

            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (HwNotificationManagerService.this.mRecHandler != null) {
                    HwNotificationManagerService.this.mRecHandler.postAtFrontOfQueue(new Runnable() {
                        /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass5.AnonymousClass1 */

                        public void run() {
                            int userId = ActivityManager.getCurrentUser();
                            boolean enable = false;
                            if (Settings.System.getIntForUser(HwNotificationManagerService.this.mContext.getContentResolver(), HwNotificationManagerService.KEY_SMART_NOTIFICATION_SWITCH, HwNotificationManagerService.mIsChina ? 1 : 0, userId) == 1) {
                                enable = true;
                            }
                            boolean allUserClose = true;
                            synchronized (HwNotificationManagerService.this.mSmartNtfSwitchMap) {
                                HwNotificationManagerService.this.mSmartNtfSwitchMap.put(Integer.valueOf(userId), Boolean.valueOf(enable));
                                if (HwNotificationManagerService.this.mSmartNtfSwitchMap.containsValue(true)) {
                                    allUserClose = false;
                                }
                            }
                            if (allUserClose) {
                                HwNotificationManagerService.this.unBind();
                            } else {
                                HwNotificationManagerService.this.bindRecSys();
                            }
                            Log.i(HwNotificationManagerService.TAG, "switch change to: " + enable + ",userId: " + userId + ",allUserClose :" + allUserClose);
                        }
                    });
                }
            }
        };
        this.powerReceiver = new BroadcastReceiver() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass6 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String action = intent.getAction();
                    if (HwNotificationManagerService.ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE.equals(action)) {
                        if (intent.getIntExtra(HwNotificationManagerService.POWER_MODE, 0) == 3) {
                            if (HwNotificationManagerService.this.mPowerSaverObserver == null) {
                                HwNotificationManagerService hwNotificationManagerService = HwNotificationManagerService.this;
                                PowerSaverObserver unused = hwNotificationManagerService.mPowerSaverObserver = new PowerSaverObserver(hwNotificationManagerService.mHwHandler);
                            }
                            HwNotificationManagerService.this.mPowerSaverObserver.observe();
                            Log.i(HwNotificationManagerService.TAG, "super power save 2.0 recevier brodcast register sqlite listener");
                        }
                    } else if (HwNotificationManagerService.ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE.equals(action) && intent.getIntExtra(HwNotificationManagerService.SHUTDOWN_LIMIT_POWERMODE, 0) == 0 && HwNotificationManagerService.this.mPowerSaverObserver != null) {
                        HwNotificationManagerService.this.mPowerSaverObserver.unObserve();
                        PowerSaverObserver unused2 = HwNotificationManagerService.this.mPowerSaverObserver = null;
                        Log.i(HwNotificationManagerService.TAG, "super power save 2.0 recevier brodcast unregister sqlite listener");
                    }
                }
            }
        };
        this.mContext = context;
    }

    public HwNotificationManagerService(Context context) {
        super(context);
        this.mHwHandler = null;
        this.mCfgDBObserver = null;
        this.mCfgMap = new HashMap();
        this.URI_NOTIFICATION_CFG = Uri.parse("content://com.huawei.systemmanager.NotificationDBProvider/notificationCfg");
        this.mLock = new Object();
        this.mLastBindTime = 0;
        this.mRecognizeMap = new ArrayMap<>();
        this.mSmartNtfSwitchMap = new ArrayMap<>();
        this.mHwGameObserver = null;
        this.mCust = (HwCustZenModeHelper) HwCustUtils.createObj(HwCustZenModeHelper.class, new Object[0]);
        this.mUpdateEnqueuedNotifications = new ArrayMap<>();
        this.noitfication_white_list = new String[]{"com.huawei.message", MMS_PKGNAME, "com.huawei.contacts", "com.android.contacts", PHONE_PKGNAME, "com.android.deskclock", "com.android.calendar", "com.huawei.calendar", FingerViewController.PKGNAME_OF_KEYGUARD, AppStartupDataMgr.HWPUSH_PKGNAME, "com.android.incallui", "com.android.phone.recorder", "com.android.cellbroadcastreceiver", TELECOM_PKGNAME, "com.huawei.meetime", "com.huawei.deskclock"};
        this.plus_notification_white_list = new String[]{HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME};
        this.power_save_whiteSet = new HashSet<>();
        this.mBtwList = new ArrayList();
        this.mBtwList.add("2");
        this.mBtwList.add("3");
        this.mBtwList.add("102");
        this.mBtwList.add(TYPE_TOOLS);
        this.mHwNotificationDelegate = new HwStatusBarManagerService.HwNotificationDelegate() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass1 */

            @Override // com.android.server.statusbar.HwStatusBarManagerService.HwNotificationDelegate
            public void onNotificationResidentClear(String pkg, String tag, int id, int userId) {
                HwNotificationManagerService.this.cancelNotification(Binder.getCallingUid(), Binder.getCallingPid(), pkg, tag, id, 0, 0, true, userId, 2, null);
            }
        };
        this.mHangButtonReceiver = new BroadcastReceiver() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null) {
                    String action = intent.getAction();
                    if (HwNotificationManagerService.NOTIFICATION_ACTION_ALLOW.equals(action)) {
                        String pkg = intent.getStringExtra("pkgName");
                        int uid = intent.getIntExtra("uid", -1);
                        if (pkg != null && uid != -1) {
                            HwNotificationManagerService.this.recoverNotificationContentView(pkg, uid);
                        }
                    } else if (action != null && action.equals(HwNotificationManagerService.NOTIFICATION_ACTION_REFUSE)) {
                        String pkg2 = intent.getStringExtra("pkgName");
                        int uid2 = intent.getIntExtra("uid", -1);
                        if (pkg2 != null && uid2 != -1) {
                            synchronized (HwNotificationManagerService.this.mOriginContentViews) {
                                Iterator<NotificationContentViewRecord> itor = HwNotificationManagerService.this.mOriginContentViews.iterator();
                                while (itor.hasNext()) {
                                    NotificationContentViewRecord cvr = itor.next();
                                    if (cvr.pkg.equals(pkg2) && cvr.uid == uid2) {
                                        itor.remove();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
        this.mOriginContentViews = new ArrayList<>();
        this.mBindRunable = new Runnable() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass3 */

            public void run() {
                HwNotificationManagerService.this.bind();
            }
        };
        this.mServiceConnection = new ServiceConnection() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass4 */

            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(HwNotificationManagerService.TAG, "onServiceConnected");
                HwRecSysAidlInterface unused = HwNotificationManagerService.this.mHwRecSysAidlInterface = HwRecSysAidlInterface.Stub.asInterface(service);
                synchronized (HwNotificationManagerService.this.mLock) {
                    HwNotificationManagerService.this.mLock.notifyAll();
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.i(HwNotificationManagerService.TAG, "onServiceDisConnected");
                HwRecSysAidlInterface unused = HwNotificationManagerService.this.mHwRecSysAidlInterface = null;
                synchronized (HwNotificationManagerService.this.mLock) {
                    HwNotificationManagerService.this.mLock.notifyAll();
                }
            }
        };
        this.mContentObserver = new ContentObserver(null) {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass5 */

            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (HwNotificationManagerService.this.mRecHandler != null) {
                    HwNotificationManagerService.this.mRecHandler.postAtFrontOfQueue(new Runnable() {
                        /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass5.AnonymousClass1 */

                        public void run() {
                            int userId = ActivityManager.getCurrentUser();
                            boolean enable = false;
                            if (Settings.System.getIntForUser(HwNotificationManagerService.this.mContext.getContentResolver(), HwNotificationManagerService.KEY_SMART_NOTIFICATION_SWITCH, HwNotificationManagerService.mIsChina ? 1 : 0, userId) == 1) {
                                enable = true;
                            }
                            boolean allUserClose = true;
                            synchronized (HwNotificationManagerService.this.mSmartNtfSwitchMap) {
                                HwNotificationManagerService.this.mSmartNtfSwitchMap.put(Integer.valueOf(userId), Boolean.valueOf(enable));
                                if (HwNotificationManagerService.this.mSmartNtfSwitchMap.containsValue(true)) {
                                    allUserClose = false;
                                }
                            }
                            if (allUserClose) {
                                HwNotificationManagerService.this.unBind();
                            } else {
                                HwNotificationManagerService.this.bindRecSys();
                            }
                            Log.i(HwNotificationManagerService.TAG, "switch change to: " + enable + ",userId: " + userId + ",allUserClose :" + allUserClose);
                        }
                    });
                }
            }
        };
        this.powerReceiver = new BroadcastReceiver() {
            /* class com.android.server.notification.HwNotificationManagerService.AnonymousClass6 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String action = intent.getAction();
                    if (HwNotificationManagerService.ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE.equals(action)) {
                        if (intent.getIntExtra(HwNotificationManagerService.POWER_MODE, 0) == 3) {
                            if (HwNotificationManagerService.this.mPowerSaverObserver == null) {
                                HwNotificationManagerService hwNotificationManagerService = HwNotificationManagerService.this;
                                PowerSaverObserver unused = hwNotificationManagerService.mPowerSaverObserver = new PowerSaverObserver(hwNotificationManagerService.mHwHandler);
                            }
                            HwNotificationManagerService.this.mPowerSaverObserver.observe();
                            Log.i(HwNotificationManagerService.TAG, "super power save 2.0 recevier brodcast register sqlite listener");
                        }
                    } else if (HwNotificationManagerService.ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE.equals(action) && intent.getIntExtra(HwNotificationManagerService.SHUTDOWN_LIMIT_POWERMODE, 0) == 0 && HwNotificationManagerService.this.mPowerSaverObserver != null) {
                        HwNotificationManagerService.this.mPowerSaverObserver.unObserve();
                        PowerSaverObserver unused2 = HwNotificationManagerService.this.mPowerSaverObserver = null;
                        Log.i(HwNotificationManagerService.TAG, "super power save 2.0 recevier brodcast unregister sqlite listener");
                    }
                }
            }
        };
        this.mContext = context;
        SystemServerInitThreadPool.get().submit(new Runnable() {
            /* class com.android.server.notification.$$Lambda$HwNotificationManagerService$ZCrKpb82sziMOc51dwZrcRLjWI */

            public final void run() {
                HwNotificationManagerService.this.lambda$new$0$HwNotificationManagerService();
            }
        }, "HwNotificationManagerService init");
    }

    /* access modifiers changed from: private */
    /* renamed from: init */
    public void lambda$new$0$HwNotificationManagerService() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_ACTION_ALLOW);
        filter.addAction(NOTIFICATION_ACTION_REFUSE);
        this.mContext.registerReceiverAsUser(this.mHangButtonReceiver, UserHandle.ALL, filter, "com.android.permission.system_manager_interface", null);
        StatusBarManagerService sb = getStatusBarManagerService();
        if (sb instanceof HwStatusBarManagerService) {
            ((HwStatusBarManagerService) sb).setHwNotificationDelegate(this.mHwNotificationDelegate);
        }
        this.mHwHandler = new Handler(Looper.getMainLooper());
        this.mHwHandler.postDelayed(new HwCfgLoadingRunnable(), 10000);
        this.mCfgDBObserver = new DBContentObserver();
        this.mContext.getContentResolver().registerContentObserver(this.URI_NOTIFICATION_CFG, true, this.mCfgDBObserver, ActivityManager.getCurrentUser());
        if (!SystemProperties.getBoolean("ro.config.hwsmartnotification.disable", false)) {
            this.mRecHandlerThread = new HandlerThread("notification manager");
            this.mRecHandlerThread.start();
            this.mRecHandler = new Handler(this.mRecHandlerThread.getLooper());
        }
        try {
            ContentResolver cr = this.mContext.getContentResolver();
            if (cr != null) {
                cr.registerContentObserver(Settings.System.getUriFor(KEY_SMART_NOTIFICATION_SWITCH), false, this.mContentObserver, -1);
            }
            this.mContentObserver.onChange(true);
        } catch (Exception e) {
            Log.w(TAG, "init failed", e);
        }
        registerHwGameObserver();
    }

    private class HwCfgLoadingRunnable implements Runnable {
        private HwCfgLoadingRunnable() {
        }

        public void run() {
            new Thread("HwCfgLoading") {
                /* class com.android.server.notification.HwNotificationManagerService.HwCfgLoadingRunnable.AnonymousClass1 */

                public void run() {
                    HwCfgLoadingRunnable.this.load();
                }
            }.start();
        }

        /* access modifiers changed from: private */
        /* JADX WARNING: Code restructure failed: missing block: B:67:0x019f, code lost:
            if (r2 == null) goto L_0x01a4;
         */
        /* JADX WARNING: Removed duplicated region for block: B:63:0x0193 A[Catch:{ RuntimeException -> 0x0194, Exception -> 0x0186, all -> 0x0182, all -> 0x01ad }] */
        /* JADX WARNING: Removed duplicated region for block: B:73:0x01b0  */
        public void load() {
            Context context;
            int nCidColIndex;
            int nCfgColIndex;
            Context context2;
            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Starts ");
            Cursor cursor = null;
            try {
                context = HwNotificationManagerService.this.mContext.createPackageContextAsUser("com.huawei.systemmanager", 0, new UserHandle(ActivityManager.getCurrentUser()));
            } catch (Exception e) {
                Slog.w(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Fail to convert context");
                context = null;
            }
            if (context != null) {
                try {
                    cursor = context.getContentResolver().query(HwNotificationManagerService.this.URI_NOTIFICATION_CFG, null, null, null, null);
                    if (cursor == null) {
                        try {
                            Slog.w(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Fail to get cfg from DB");
                            if (cursor != null) {
                                cursor.close();
                            }
                            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                        } catch (RuntimeException e2) {
                            e = e2;
                            Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : RuntimeException ", e);
                        } catch (Exception e3) {
                            e = e3;
                            Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Exception ", e);
                            if (cursor != null) {
                            }
                            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                        } catch (Throwable th) {
                            th = th;
                            if (cursor != null) {
                            }
                            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                            throw th;
                        }
                    } else {
                        HashMap<String, Integer> tempMap = new HashMap<>();
                        ArrayList<PreferencesHelper.NotificationSysMgrCfg> tempSysMgrCfgList = new ArrayList<>();
                        if (cursor.getCount() > 0) {
                            int nPkgColIndex = cursor.getColumnIndex("packageName");
                            int nCfgColIndex2 = cursor.getColumnIndex("sound_vibrate");
                            int nCidColIndex2 = cursor.getColumnIndex("channelid");
                            int nLockscreenColIndex = cursor.getColumnIndex("lockscreencfg");
                            int nImportaceColIndex = cursor.getColumnIndex("channelimportance");
                            int nBypassdndColIndex = cursor.getColumnIndex("channelbypassdnd");
                            int nIconDadgeColIndex = cursor.getColumnIndex("channeliconbadge");
                            while (cursor.moveToNext()) {
                                String pkgName = cursor.getString(nPkgColIndex);
                                if (!TextUtils.isEmpty(pkgName)) {
                                    int cfg = cursor.getInt(nCfgColIndex2);
                                    String channelId = cursor.getString(nCidColIndex2);
                                    String key = HwNotificationManagerService.this.pkgAndCidKey(pkgName, channelId);
                                    if ("miscellaneous".equals(channelId)) {
                                        PreferencesHelper.NotificationSysMgrCfg mgrCfg = new PreferencesHelper.NotificationSysMgrCfg();
                                        context2 = context;
                                        try {
                                            mgrCfg.smc_userId = ActivityManager.getCurrentUser();
                                            mgrCfg.smc_packageName = pkgName;
                                            mgrCfg.smc_visilibity = cursor.getInt(nLockscreenColIndex);
                                            mgrCfg.smc_importance = cursor.getInt(nImportaceColIndex);
                                            mgrCfg.smc_bypassDND = cursor.getInt(nBypassdndColIndex);
                                            mgrCfg.smc_iconBadge = cursor.getInt(nIconDadgeColIndex);
                                            tempSysMgrCfgList.add(mgrCfg);
                                            if (HwNotificationManagerService.MMS_PKGNAME.equals(pkgName)) {
                                                nCfgColIndex = nCfgColIndex2;
                                                StringBuilder sb = new StringBuilder();
                                                nCidColIndex = nCidColIndex2;
                                                sb.append("mgrCfg.importance : ");
                                                sb.append(mgrCfg.smc_importance);
                                                Slog.i(HwNotificationManagerService.TAG, sb.toString());
                                            } else {
                                                nCfgColIndex = nCfgColIndex2;
                                                nCidColIndex = nCidColIndex2;
                                            }
                                        } catch (RuntimeException e4) {
                                            e = e4;
                                            Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : RuntimeException ", e);
                                        } catch (Exception e5) {
                                            e = e5;
                                            Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Exception ", e);
                                            if (cursor != null) {
                                                cursor.close();
                                            }
                                            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                                        }
                                    } else {
                                        context2 = context;
                                        nCfgColIndex = nCfgColIndex2;
                                        nCidColIndex = nCidColIndex2;
                                    }
                                    tempMap.put(key, Integer.valueOf(cfg));
                                    nPkgColIndex = nPkgColIndex;
                                    context = context2;
                                    nCfgColIndex2 = nCfgColIndex;
                                    nCidColIndex2 = nCidColIndex;
                                }
                            }
                        }
                        HwNotificationManagerService.this.setSysMgrCfgMap(tempSysMgrCfgList);
                        synchronized (HwNotificationManagerService.this.mCfgMap) {
                            HwNotificationManagerService.this.mCfgMap.clear();
                            HwNotificationManagerService.this.mCfgMap.putAll(tempMap);
                            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: get cfg size:" + HwNotificationManagerService.this.mCfgMap.size());
                        }
                        cursor.close();
                        Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                    }
                } catch (RuntimeException e6) {
                    e = e6;
                    Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : RuntimeException ", e);
                } catch (Exception e7) {
                    e = e7;
                    Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Exception ", e);
                    if (cursor != null) {
                    }
                    Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                } catch (Throwable th2) {
                    th = th2;
                    if (cursor != null) {
                    }
                    Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                    throw th;
                }
            }
        }
    }

    private class DBContentObserver extends ContentObserver {
        public DBContentObserver() {
            super(null);
        }

        public void onChange(boolean selfChange) {
            Slog.v(HwNotificationManagerService.TAG, "Notification db cfg changed");
            HwNotificationManagerService.this.mHwHandler.post(new HwCfgLoadingRunnable());
        }
    }

    /* access modifiers changed from: protected */
    public void handleGetNotifications(Parcel data, Parcel reply) {
        HashSet<String> notificationPkgs = new HashSet<>();
        if (this.mContext.checkCallingPermission("huawei.permission.IBINDER_NOTIFICATION_SERVICE") != 0) {
            Slog.e(TAG, "NotificationManagerService.handleGetNotifications: permissin deny");
            return;
        }
        getNotificationPkgs_hwHsm(notificationPkgs);
        Slog.v(TAG, "NotificationManagerService.handleGetNotifications: got " + notificationPkgs.size() + " pkgs");
        reply.writeInt(notificationPkgs.size());
        Iterator<String> it = notificationPkgs.iterator();
        while (it.hasNext()) {
            String pkg = it.next();
            Slog.v(TAG, "NotificationManagerService.handleGetNotifications: reply " + pkg);
            reply.writeString(pkg);
        }
    }

    /* access modifiers changed from: package-private */
    public void getNotificationPkgs_hwHsm(HashSet<String> notificationPkgs) {
        if (notificationPkgs != null) {
            if (notificationPkgs.size() > 0) {
                notificationPkgs.clear();
            }
            synchronized (this.mNotificationList) {
                int N = this.mNotificationList.size();
                if (N != 0) {
                    for (int i = 0; i < N; i++) {
                        NotificationRecord r = (NotificationRecord) this.mNotificationList.get(i);
                        if (r != null) {
                            String sPkg = r.sbn.getPackageName();
                            if (sPkg != null && sPkg.length() > 0) {
                                notificationPkgs.add(sPkg);
                            }
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public static final class NotificationContentViewRecord {
        final int id;
        final String pkg;
        RemoteViews rOldBigContentView;
        final String tag;
        final int uid;
        final int userId;

        NotificationContentViewRecord(String pkg2, int uid2, String tag2, int id2, int userId2, RemoteViews rOldBigContentView2) {
            this.pkg = pkg2;
            this.tag = tag2;
            this.uid = uid2;
            this.id = id2;
            this.userId = userId2;
            this.rOldBigContentView = rOldBigContentView2;
        }

        public final String toString() {
            return "NotificationContentViewRecord{" + Integer.toHexString(System.identityHashCode(this)) + " pkg=" + this.pkg + " uid=" + this.uid + " id=" + Integer.toHexString(this.id) + " tag=" + this.tag + " userId=" + Integer.toHexString(this.userId) + "}";
        }
    }

    private int indexOfContentViewLocked(String pkg, String tag, int id, int userId) {
        synchronized (this.mOriginContentViews) {
            ArrayList<NotificationContentViewRecord> list = this.mOriginContentViews;
            int len = list.size();
            for (int i = 0; i < len; i++) {
                NotificationContentViewRecord r = list.get(i);
                if (userId == -1 || r.userId == -1 || r.userId == userId) {
                    if (r.id == id) {
                        if (tag == null) {
                            if (r.tag != null) {
                            }
                        } else if (!tag.equals(r.tag)) {
                        }
                        if (r.pkg.equals(pkg)) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }
    }

    /* access modifiers changed from: private */
    public void recoverNotificationContentView(String pkg, int uid) {
        synchronized (this.mOriginContentViews) {
            Iterator<NotificationContentViewRecord> itor = this.mOriginContentViews.iterator();
            while (itor.hasNext()) {
                NotificationContentViewRecord cvr = itor.next();
                if (cvr.pkg.equals(pkg)) {
                    NotificationRecord r = findNotificationByListLocked(this.mNotificationList, cvr.pkg, cvr.tag, cvr.id, cvr.userId);
                    if (r != null) {
                        r.sbn.getNotification().bigContentView = cvr.rOldBigContentView;
                        Slog.d(TAG, "revertNotificationView enqueueNotificationInternal pkg=" + pkg + " id=" + r.sbn.getId() + " userId=" + r.sbn.getUserId());
                        r.sbn.getNotification().extras.putBoolean(CONTENTVIEW_REVERTING_FLAG, true);
                        enqueueNotificationInternal(pkg, r.sbn.getOpPkg(), r.sbn.getUid(), r.sbn.getInitialPid(), r.sbn.getTag(), r.sbn.getId(), r.sbn.getNotification(), r.sbn.getUserId());
                        itor.remove();
                    } else {
                        Slog.w(TAG, "Notification can't find in NotificationRecords");
                    }
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean isHwSoundAllow(String pkg, String channelId, int userId) {
        Integer cfg;
        String key = pkgAndCidKey(pkg, channelId);
        synchronized (this.mCfgMap) {
            cfg = this.mCfgMap.get(key);
        }
        Slog.v(TAG, "isHwSoundAllow pkg=" + pkg + ", channelId=" + channelId + ", userId=" + userId + ", cfg=" + cfg);
        return cfg == null || (cfg.intValue() & 1) != 0;
    }

    /* access modifiers changed from: protected */
    public boolean isHwVibrateAllow(String pkg, String channelId, int userId) {
        Integer cfg;
        String key = pkgAndCidKey(pkg, channelId);
        synchronized (this.mCfgMap) {
            cfg = this.mCfgMap.get(key);
        }
        Slog.v(TAG, "isHwVibrateAllow pkg=" + pkg + ", channelId=" + channelId + ", userId=" + userId + ", cfg=" + cfg);
        return cfg == null || (cfg.intValue() & 2) != 0;
    }

    /* access modifiers changed from: private */
    public String pkgAndCidKey(String pkg, String channelId) {
        return pkg + "_" + channelId;
    }

    /* access modifiers changed from: protected */
    @Deprecated
    public int modifyScoreBySM(String pkg, int callingUid, int origScore) {
        return origScore;
    }

    /* access modifiers changed from: protected */
    public void detectNotifyBySM(int callingUid, String pkg, Notification notification) {
        Intent intent = new Intent("com.huawei.notificationmanager.detectnotify");
        intent.putExtra("callerUid", callingUid);
        intent.putExtra("packageName", pkg);
        Bundle bundle = new Bundle();
        bundle.putParcelable("sendNotify", notification);
        intent.putExtra("notifyBundle", bundle);
    }

    /* access modifiers changed from: protected */
    public void hwEnqueueNotificationWithTag(String pkg, int uid, NotificationRecord nr) {
        if (nr.sbn.getNotification().extras.getBoolean(CONTENTVIEW_REVERTING_FLAG, false)) {
            nr.sbn.getNotification().extras.putBoolean(CONTENTVIEW_REVERTING_FLAG, false);
        }
    }

    /* access modifiers changed from: protected */
    public boolean inNonDisturbMode(String pkg) {
        if (pkg == null) {
            return false;
        }
        return isWhiteApp(pkg);
    }

    private boolean isWhiteApp(String pkg) {
        HwCustZenModeHelper hwCustZenModeHelper = this.mCust;
        if (hwCustZenModeHelper != null && hwCustZenModeHelper.getWhiteApps(this.mContext) != null) {
            return Arrays.asList(this.mCust.getWhiteApps(this.mContext)).contains(pkg);
        }
        String[] DEFAULT_WHITEAPP = {MMS_PKGNAME};
        if (this.mContext == null) {
            return false;
        }
        for (String pkgname : DEFAULT_WHITEAPP) {
            if (pkgname.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isImportantNotification(String pkg, Notification notification) {
        if (notification == null || notification.priority < 3) {
            return false;
        }
        if ((pkg.equals(PHONE_PKGNAME) || pkg.equals(MMS_PKGNAME) || pkg.equals("com.android.contacts") || pkg.equals("com.huawei.contacts") || pkg.equals(TELECOM_PKGNAME)) && notification.priority < 7) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isMmsNotificationEnable(String pkg) {
        if (pkg == null) {
            return false;
        }
        if (pkg.equalsIgnoreCase(MMS_PKGNAME) || pkg.equalsIgnoreCase("com.android.contacts") || pkg.equals("com.huawei.contacts")) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void hwCancelNotification(String pkg, String tag, int id, int userId) {
        synchronized (this.mOriginContentViews) {
            int indexView = indexOfContentViewLocked(pkg, tag, id, userId);
            if (indexView >= 0) {
                this.mOriginContentViews.remove(indexView);
                Slog.d(TAG, "hwCancelNotification: pkg = " + pkg + ", id = " + id);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updateLight(boolean enable, int ledOnMS, int ledOffMS) {
        this.mBatteryManagerInternal.updateBatteryLight(enable, ledOnMS, ledOffMS);
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitchEvents(int userId) {
        if (this.mCfgDBObserver != null) {
            this.mHwHandler.post(new HwCfgLoadingRunnable());
            this.mContext.getContentResolver().unregisterContentObserver(this.mCfgDBObserver);
            this.mContext.getContentResolver().registerContentObserver(this.URI_NOTIFICATION_CFG, true, this.mCfgDBObserver, ActivityManager.getCurrentUser());
        }
    }

    /* access modifiers changed from: protected */
    public void stopPlaySound() {
        this.mSoundNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
            if (player != null) {
                player.stopAsync();
            }
        } catch (RemoteException e) {
        } catch (Throwable player2) {
            Binder.restoreCallingIdentity(identity);
            throw player2;
        }
        Binder.restoreCallingIdentity(identity);
    }

    /* access modifiers changed from: protected */
    public boolean isAFWUserId(int userId) {
        boolean temp = false;
        long token = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = UserManagerService.getInstance().getUserInfo(userId);
            if (userInfo != null) {
                temp = userInfo.isManagedProfile() || userInfo.isClonedProfile();
            }
        } catch (Exception e) {
        } catch (Throwable userInfo2) {
            Binder.restoreCallingIdentity(token);
            throw userInfo2;
        }
        Binder.restoreCallingIdentity(token);
        return temp;
    }

    /* access modifiers changed from: protected */
    public void addHwExtraForNotification(Notification notification, String pkg, int pid) {
        if (isIntentProtectedApp(pkg) && isTrustSpaceBadgeEnabled()) {
            notification.extras.putBoolean("com.huawei.isIntentProtectedApp", true);
        }
    }

    private boolean isIntentProtectedApp(String pkg) {
        TrustSpaceManagerInternal mTrustSpaceManagerInternal = (TrustSpaceManagerInternal) LocalServices.getService(TrustSpaceManagerInternal.class);
        if (mTrustSpaceManagerInternal != null) {
            return mTrustSpaceManagerInternal.isIntentProtectedApp(pkg);
        }
        Slog.e(TAG, "TrustSpaceManagerInternal not find !");
        return false;
    }

    private boolean isTrustSpaceBadgeEnabled() {
        return 1 == Settings.Secure.getInt(this.mContext.getContentResolver(), KEY_TRUST_SPACE_BADGE_SWITCH, 1);
    }

    /* access modifiers changed from: protected */
    public String getNCTargetAppPkg(String opPkg, String defaultPkg, Notification notification) {
        Bundle bundle;
        String targetPkg;
        if (!NOTIFICATION_CENTER_PKG.equals(opPkg) || (bundle = notification.extras) == null || (targetPkg = bundle.getString(NOTIFICATION_CENTER_ORIGIN_PKG)) == null || !isVaildPkg(targetPkg)) {
            return defaultPkg;
        }
        Slog.v(TAG, "Notification Center targetPkg:" + targetPkg);
        return targetPkg;
    }

    private boolean isVaildPkg(String pkg) {
        try {
            if (AppGlobals.getPackageManager().getApplicationInfo(pkg, 0, UserHandle.getCallingUserId()) == null) {
                return false;
            }
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isBlockRideModeNotification(String pkg) {
        return HWRIDEMODE_FEATURE_SUPPORTED && SystemProperties.getBoolean("sys.ride_mode", false) && !RIDEMODE_NOTIFICATION_WHITE_LIST.contains(pkg);
    }

    public void reportToIAware(String pkg, int uid, int nid, boolean added) {
        HwSysResManager resManager;
        if (pkg != null && !pkg.isEmpty() && (resManager = HwSysResManager.getInstance()) != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC))) {
            Bundle bundleArgs = new Bundle();
            bundleArgs.putString(MemoryConstant.MEM_PREREAD_ITEM_NAME, pkg);
            bundleArgs.putInt("tgtUid", uid);
            bundleArgs.putInt("notification_id", nid);
            bundleArgs.putInt("relationType", added ? 20 : 21);
            CollectData data = new CollectData(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
            long id = Binder.clearCallingIdentity();
            resManager.reportData(data);
            Binder.restoreCallingIdentity(id);
        }
    }

    /* access modifiers changed from: protected */
    public boolean doForUpdateNotification(String key, Handler handler) {
        if (!MemoryConstant.isNotificatinSwitchEnable() || handler == null || key == null || !isUpdateNotificationNeedToMng(key)) {
            return false;
        }
        long interval = MemoryConstant.getNotificationInterval();
        NotificationRecord newNotification = findNotificationByListLocked(key);
        if (newNotification == null) {
            return false;
        }
        if (((newNotification.getFlags() & 2) != 0 && (newNotification.getFlags() & 64) != 0) || !isImUpdateNotification(newNotification)) {
            return false;
        }
        if (this.mUpdateEnqueuedNotifications.get(key) != null) {
            if (isWechatHongbao(newNotification)) {
                handler.post(new PostNotificationRunnable(this, key));
            } else {
                this.mUpdateEnqueuedNotifications.put(key, newNotification);
            }
            int N = this.mEnqueuedNotifications.size();
            for (int i = 0; i < N; i++) {
                if (Objects.equals(key, ((NotificationRecord) this.mEnqueuedNotifications.get(i)).getKey())) {
                    this.mEnqueuedNotifications.remove(i);
                    return true;
                }
            }
            return true;
        } else if (isWechatHongbao(newNotification)) {
            return false;
        } else {
            this.mUpdateEnqueuedNotifications.put(key, newNotification);
            handler.postDelayed(new PostNotificationRunnable(this, key), interval);
            return true;
        }
    }

    private boolean isImUpdateNotification(NotificationRecord record) {
        StatusBarNotification n = record.sbn;
        if (n == null) {
            return false;
        }
        int appType = AwareIntelligentRecg.getInstance().getAppMngSpecType(n.getPackageName());
        if (appType == 0 || 6 == appType || 311 == appType || 318 == appType) {
            return true;
        }
        return false;
    }

    private boolean isWechatHongbao(NotificationRecord record) {
        Notification notification;
        Bundle extras;
        CharSequence charSequence;
        StatusBarNotification n = record.sbn;
        String topImCN = AwareIntelligentRecg.getInstance().getActTopIMCN();
        if (topImCN == null || !topImCN.equals(n.getPackageName()) || (notification = n.getNotification()) == null || (extras = notification.extras) == null || (charSequence = extras.getCharSequence("android.text")) == null || !charSequence.toString().contains(WECHAT_HONGBAO)) {
            return false;
        }
        return true;
    }

    private NotificationRecord findNotificationByListLocked(String key) {
        for (int i = this.mEnqueuedNotifications.size() - 1; i >= 0; i--) {
            if (key.equals(((NotificationRecord) this.mEnqueuedNotifications.get(i)).getKey())) {
                return (NotificationRecord) this.mEnqueuedNotifications.get(i);
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void removeNotificationInUpdateQueue(String key) {
        if (key != null && this.mUpdateEnqueuedNotifications.containsKey(key)) {
            this.mUpdateEnqueuedNotifications.remove(key);
        }
    }

    private boolean isUpdateNotificationNeedToMng(String key) {
        return indexOfNotificationLocked(key) >= 0;
    }

    /* access modifiers changed from: protected */
    public boolean isFromPinNotification(Notification notification, String pkg) {
        return isPkgInWhiteApp(pkg) && notification.extras.getBoolean("pin_notification");
    }

    private boolean isPkgInWhiteApp(String pkg) {
        for (String s : NOTIFICATION_WHITE_APPS_PACKAGE) {
            if (s.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isGameRunningForeground() {
        return HwActivityTaskManager.isGameDndOn();
    }

    /* access modifiers changed from: protected */
    public boolean isGameDndSwitchOn() {
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "game_dnd_mode", 2) == 1) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isPackageRequestNarrowNotification() {
        String topPkg = getTopPkgName();
        for (String pkg : NOTIFICATION_NARROW_DISPLAY_APPS) {
            if (pkg.equalsIgnoreCase(topPkg)) {
                return true;
            }
        }
        return false;
    }

    private String getTopPkgName() {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        List<ActivityManager.RunningTaskInfo> tasks = null;
        if (am != null) {
            tasks = am.getRunningTasks(1);
        }
        ActivityManager.RunningTaskInfo runningTaskInfo = null;
        if (tasks != null && !tasks.isEmpty()) {
            runningTaskInfo = tasks.get(0);
        }
        if (runningTaskInfo != null) {
            return runningTaskInfo.topActivity.getPackageName();
        }
        return "";
    }

    private void registerHwGameObserver() {
        if (this.mHwGameObserver == null) {
            this.mHwGameObserver = new HwGameObserver();
        }
        ActivityManagerEx.registerGameObserver(this.mHwGameObserver);
    }

    private class HwGameObserver extends IGameObserver.Stub {
        private HwGameObserver() {
        }

        public void onGameListChanged() {
        }

        public void onGameStatusChanged(String packageName, int event) {
            if (1 == event || 3 == event || 4 == event) {
                HwNotificationManagerService.this.mGameDndStatus = true;
            } else if (2 == event) {
                HwNotificationManagerService.this.mGameDndStatus = false;
            }
            HwNotificationManagerService.this.updateNotificationInGameMode();
            Log.d(HwNotificationManagerService.TAG, "onGameStatusChanged event=" + event + ",mGameDndStatus=" + HwNotificationManagerService.this.mGameDndStatus);
        }
    }

    /* access modifiers changed from: private */
    public void updateNotificationInGameMode() {
        synchronized (this.mNotificationLock) {
            updateLightsLocked();
        }
    }

    public void bindRecSys() {
        Handler handler = this.mRecHandler;
        if (handler != null) {
            handler.removeCallbacks(this.mBindRunable);
            this.mRecHandler.post(this.mBindRunable);
        }
    }

    /* access modifiers changed from: private */
    public void bind() {
        if (this.mHwRecSysAidlInterface != null) {
            Log.d(TAG, "bind: already binded");
            return;
        }
        try {
            Log.i(TAG, "bind service: action=com.huawei.recsys.action.THIRD_REQUEST_ENGINE, pkg=com.huawei.recsys");
            Intent intent = new Intent();
            intent.setAction(BIND_ACTION);
            intent.setPackage(SERVER_PAKAGE_NAME);
            boolean ret = this.mContext.bindService(intent, this.mServiceConnection, 1);
            if (ret) {
                synchronized (this.mLock) {
                    this.mLock.wait(10000);
                }
            }
            Log.i(TAG, "bind service finish, ret=" + ret);
        } catch (Exception e) {
            Log.e(TAG, "bind service failed!", e);
        }
    }

    /* access modifiers changed from: private */
    public void unBind() {
        Handler handler = this.mRecHandler;
        if (handler != null) {
            handler.removeCallbacks(this.mBindRunable);
        }
        if (this.mHwRecSysAidlInterface == null) {
            Log.d(TAG, "unbind: already unbinded");
            return;
        }
        try {
            Log.i(TAG, "unbind service");
            this.mContext.unbindService(this.mServiceConnection);
            Log.i(TAG, "unbind service finish");
        } catch (Exception e) {
            Log.e(TAG, "unbind service failed!", e);
        }
        this.mHwRecSysAidlInterface = null;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:100:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x00ad, code lost:
        if (isSystemApp(r25, r26) == false) goto L_0x00c6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x00af, code lost:
        android.util.Log.i(com.android.server.notification.HwNotificationManagerService.TAG, "recognize: system app");
        r2 = r20.mRecognizeMap;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x00b9, code lost:
        monitor-enter(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:?, code lost:
        r20.mRecognizeMap.put(r8, "0");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x00c1, code lost:
        monitor-exit(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00c2, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x00c8, code lost:
        if (r20.mHwRecSysAidlInterface != null) goto L_0x00fd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00cc, code lost:
        if (r20.mRecHandler == null) goto L_0x00fd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x00ce, code lost:
        r2 = java.lang.System.currentTimeMillis();
        r4 = java.lang.Math.abs(r2 - r20.mLastBindTime);
        android.util.Log.i(com.android.server.notification.HwNotificationManagerService.TAG, "RecSys service is disconnect, we should retry to connect service, bindInterval=" + r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00f5, code lost:
        if (r4 <= 300000) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00f7, code lost:
        r20.mLastBindTime = r2;
        bindRecSys();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00ff, code lost:
        if (r20.mHwRecSysAidlInterface == null) goto L_0x01de;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:?, code lost:
        r0 = r20.mHwRecSysAidlInterface.doNotificationCollect(new android.service.notification.StatusBarNotification(r25, r25, r22, r21, r26, r27, r23, r24, null, java.lang.System.currentTimeMillis()));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x012b, code lost:
        if (r0 != null) goto L_0x012e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x012d, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x0134, code lost:
        if (r0.equals("0") == false) goto L_0x0166;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:?, code lost:
        r4 = r20.mRecognizeMap;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x0138, code lost:
        monitor-enter(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:?, code lost:
        r20.mRecognizeMap.put(r8, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x0140, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:?, code lost:
        android.util.Log.d(com.android.server.notification.HwNotificationManagerService.TAG, "recognize: just ignore type : " + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x0158, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x0159, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x015b, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x015e, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x015f, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x0160, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x016e, code lost:
        if (r0.equals("103") == false) goto L_0x017b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x0170, code lost:
        r23.extras.putString("hw_type", "type_music");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x017b, code lost:
        r23.extras.putString("hw_type", r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x0182, code lost:
        r23.extras.putBoolean("hw_btw", r20.mBtwList.contains(r0));
        r4 = new java.lang.StringBuilder();
        r4.append("doNotificationCollect: pkg=");
        r4.append(r25);
        r4.append(", uid=");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:?, code lost:
        r4.append(r26);
        r4.append(", hw_type=");
        r4.append(r0);
        r4.append(", hw_btw=");
        r4.append(r20.mBtwList.contains(r0));
        android.util.Log.i(com.android.server.notification.HwNotificationManagerService.TAG, r4.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x01c7, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x01c9, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x01cd, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x01d3, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x01d6, code lost:
        android.util.Log.e(com.android.server.notification.HwNotificationManagerService.TAG, "doNotificationCollect failed", r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:96:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:98:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:99:?, code lost:
        return;
     */
    public void recognize(String tag, int id, Notification notification, UserHandle user, String pkg, int uid, int pid) {
        if (!mIsChina) {
            Log.i(TAG, "recognize: not in china");
        } else if (isFeartureDisable()) {
            Log.i(TAG, "recognize: feature is disabled");
        } else {
            Log.i(TAG, "recognize: tag=" + tag + ", id=" + id + ", user=" + user + ", pkg=" + pkg + ", uid=" + uid + ", callingPid=" + pid);
            StringBuilder sb = new StringBuilder();
            sb.append(pkg);
            sb.append(pid);
            String key = sb.toString();
            synchronized (this.mRecognizeMap) {
                try {
                    if ("0".equals(this.mRecognizeMap.get(key))) {
                        try {
                            Log.i(TAG, "Return ! recognize the app not in list : " + pkg);
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }
    }

    private boolean isSystemApp(String pkg, int uid) {
        if (AppStartupDataMgr.HWPUSH_PKGNAME.equals(pkg)) {
            return true;
        }
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(pkg, 0, UserHandle.getUserId(uid));
            if (ai != null && ai.isSystemApp() && !isRemoveAblePreInstall(ai, pkg)) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isRemoveAblePreInstall(ApplicationInfo ai, String pkg) {
        return ((ai.hwFlags & 33554432) == 0 && (ai.hwFlags & 67108864) == 0) ? false : true;
    }

    private boolean isFeartureDisable() {
        boolean isDisable;
        long callingId = Binder.clearCallingIdentity();
        try {
            int userId = ActivityManager.getCurrentUser();
            synchronized (this.mSmartNtfSwitchMap) {
                isDisable = this.mSmartNtfSwitchMap.containsKey(Integer.valueOf(userId)) && !this.mSmartNtfSwitchMap.get(Integer.valueOf(userId)).booleanValue();
            }
            return isDisable;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isNotificationDisable() {
        return HwDeviceManager.disallowOp(102);
    }

    /* JADX INFO: finally extract failed */
    public boolean isAllowToShow(String pkg, ActivityInfo topActivity) {
        if (!((topActivity == null || pkg == null || pkg.equals(topActivity.packageName)) ? false : true)) {
            return true;
        }
        int uid = Binder.getCallingUid();
        long restoreCurId = Binder.clearCallingIdentity();
        try {
            boolean hsmCheck = HwAddViewHelper.getInstance(getContext()).addViewPermissionCheck(pkg, 2, uid);
            Binder.restoreCallingIdentity(restoreCurId);
            Slog.i("ToastInterrupt", "isAllowToShowToast:" + hsmCheck + ", pkg:" + pkg + ", topActivity:" + topActivity);
            return hsmCheck;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(restoreCurId);
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isCustDialer(String packageName) {
        return CUST_DIALER_ENABLE && DIALER_PKGNAME.equals(packageName);
    }

    /* access modifiers changed from: protected */
    public String getPackageNameByPid(int pid) {
        ActivityManager activityManager;
        List<ActivityManager.RunningAppProcessInfo> appProcesses;
        if (pid <= 0 || (activityManager = (ActivityManager) getContext().getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)) == null || (appProcesses = activityManager.getRunningAppProcesses()) == null) {
            return null;
        }
        String packageName = null;
        Iterator<ActivityManager.RunningAppProcessInfo> it = appProcesses.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ActivityManager.RunningAppProcessInfo appProcess = it.next();
            if (appProcess.pid == pid) {
                packageName = appProcess.processName;
                break;
            }
        }
        int indexProcessFlag = -1;
        if (packageName != null) {
            indexProcessFlag = packageName.indexOf(58);
        }
        return indexProcessFlag > 0 ? packageName.substring(0, indexProcessFlag) : packageName;
    }

    public void addNotificationFlag(StatusBarNotification n) {
        if (n.getNotification().extras != null) {
            if (!DISABLE_MULTIWIN) {
                try {
                    n.getNotification().extras.putString("specialType", getNotificationType(n, HWMULTIWIN_ENABLED));
                    n.getNotification().extras.putBoolean("topFullscreen", !isExpandedNtfPkg(n.getPackageName()) && (isTopFullscreen() || isPackageRequestNarrowNotification()));
                    Log.i(TAG, "specialType is:" + n.getNotification().extras.getString("specialType") + " ,topFullscreen is:" + n.getNotification().extras.getBoolean("topFullscreen") + ",HWMULTIWIN_ENABLED is:" + HWMULTIWIN_ENABLED);
                } catch (ConcurrentModificationException e) {
                    Log.e(TAG, "ConcurrentModificationException is happen, notification.extras.putBoolean:" + e.toString());
                }
            }
            boolean isGameDndSwitchOn = false;
            boolean isDeferLaunchActivity = false;
            try {
                if (this.mGameDndStatus) {
                    isGameDndSwitchOn = isGameDndSwitchOn();
                    n.getNotification().extras.putBoolean("gameDndSwitchOn", isGameDndSwitchOn);
                    PendingIntent pendingIntent = n.getNotification().fullScreenIntent;
                    if (!(pendingIntent == null || pendingIntent.getIntent() == null || pendingIntent.getIntent().getComponent() == null)) {
                        isDeferLaunchActivity = HwSnsVideoManager.getInstance(this.mContext).getDeferLaunchingActivitys().contains(pendingIntent.getIntent().getComponent().flattenToShortString());
                    }
                    n.getNotification().extras.putBoolean("isDeferLaunchActivity", isDeferLaunchActivity);
                }
                n.getNotification().extras.putBoolean("gameDndOn", this.mGameDndStatus);
                Log.d(TAG, "mGameDndStatus is:" + this.mGameDndStatus + " ,isGameDndSwitchOn is:" + isGameDndSwitchOn + ",isDeferLaunchActivity is:" + isDeferLaunchActivity);
            } catch (ConcurrentModificationException e2) {
                Log.e(TAG, "notification.extras:" + e2.toString());
            }
        }
    }

    private boolean isExpandedNtfPkg(String pkgName) {
        return EXPANDEDNTF_PKGS.contains(pkgName);
    }

    private boolean isTopFullscreen() {
        int ret = 0;
        try {
            IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            if (wm == null) {
                return false;
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.view.IWindowManager");
            wm.asBinder().transact(206, data, reply, 0);
            ret = reply.readInt();
            if (ret > 0) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "isTopIsFullscreen", e);
        }
    }

    public void setNotificationWhiteList() {
        String apps_plus = Settings.Secure.getString(getContext().getContentResolver(), POWER_SAVER_NOTIFICATION_WHITELIST);
        Log.i(TAG, "getNotificationWhiteList from db: " + apps_plus);
        this.power_save_whiteSet.clear();
        for (String s : this.plus_notification_white_list) {
            this.power_save_whiteSet.add(s);
        }
        for (String s2 : this.noitfication_white_list) {
            this.power_save_whiteSet.add(s2);
        }
        if (!TextUtils.isEmpty(apps_plus)) {
            for (String s3 : apps_plus.split(";")) {
                this.power_save_whiteSet.add(s3);
            }
        }
    }

    public boolean isNoitficationWhiteApp(String pkg) {
        return this.power_save_whiteSet.contains(pkg);
    }

    /* access modifiers changed from: private */
    public final class PowerSaverObserver extends ContentObserver {
        private final Uri SUPER_POWER_SAVE_NOTIFICATION_URI = Settings.Secure.getUriFor(HwNotificationManagerService.POWER_SAVER_NOTIFICATION_WHITELIST);
        private boolean initObserver = false;

        PowerSaverObserver(Handler handler) {
            super(handler);
        }

        /* access modifiers changed from: package-private */
        public void observe() {
            if (!this.initObserver) {
                this.initObserver = true;
                HwNotificationManagerService.this.getContext().getContentResolver().registerContentObserver(this.SUPER_POWER_SAVE_NOTIFICATION_URI, false, this, -1);
                update(null);
            }
        }

        /* access modifiers changed from: package-private */
        public void unObserve() {
            this.initObserver = false;
            HwNotificationManagerService.this.getContext().getContentResolver().unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (uri == null || this.SUPER_POWER_SAVE_NOTIFICATION_URI.equals(uri)) {
                HwNotificationManagerService.this.setNotificationWhiteList();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void checkCallerIsSystemOrSystemApp() {
        if (!isCallerSystemOrSystemApp()) {
            throw new SecurityException("Disallowed call for uid " + Binder.getCallingUid());
        }
    }

    private boolean isCallerSystemOrSystemApp() {
        boolean z = true;
        if (isCallerSystemOrPhone()) {
            return true;
        }
        boolean isSystemApp = false;
        int callingUid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            if (this.mPackageManager.checkUidSignatures(callingUid, 1000) != 0) {
                z = false;
            }
            isSystemApp = z;
        } catch (RemoteException e) {
            Log.e(TAG, "checkUidSignatures failed");
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
        Binder.restoreCallingIdentity(identity);
        return isSystemApp;
    }

    private String getNotificationType(StatusBarNotification n, boolean isHwMultiWin) {
        if (isHwMultiWin) {
            return isResizeableForHwMultiWin(n.getNotification(), n.getUserId()) ? "floating_window_notification" : "";
        }
        return SplitNotificationUtils.getInstance(getContext()).getNotificationType(n.getPackageName(), 1);
    }

    private boolean isResizeableForHwMultiWin(Notification notification, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            PendingIntent pendingIntent = notification.contentIntent != null ? notification.contentIntent : notification.fullScreenIntent;
            if (pendingIntent != null) {
                if (pendingIntent.isActivity()) {
                    Intent jumpIntent = pendingIntent.getIntent();
                    if (jumpIntent == null) {
                        Binder.restoreCallingIdentity(identity);
                        return false;
                    }
                    ResolveInfo resolveInfo = this.mPackageManager.resolveIntent(jumpIntent, jumpIntent.resolveType(this.mContext), 0, userId);
                    if (resolveInfo == null || resolveInfo.activityInfo == null || resolveInfo.activityInfo.packageName == null) {
                        Binder.restoreCallingIdentity(identity);
                        return false;
                    }
                    Intent mainIntent = new Intent("android.intent.action.MAIN");
                    mainIntent.setPackage(resolveInfo.activityInfo.packageName);
                    mainIntent.addCategory("android.intent.category.LAUNCHER");
                    ResolveInfo mainResolveInfo = this.mPackageManager.resolveIntent(mainIntent, mainIntent.resolveType(this.mContext), 0, userId);
                    if (mainResolveInfo == null || mainResolveInfo.activityInfo == null) {
                        Log.e(TAG, "isResizeableForHwMultiWin: mainResolveInfo is null");
                        Binder.restoreCallingIdentity(identity);
                        return false;
                    }
                    boolean isResizableApp = HwActivityTaskManager.isResizableApp(mainResolveInfo.activityInfo);
                    Binder.restoreCallingIdentity(identity);
                    return isResizableApp;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "method isBanNotification has Exception");
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
