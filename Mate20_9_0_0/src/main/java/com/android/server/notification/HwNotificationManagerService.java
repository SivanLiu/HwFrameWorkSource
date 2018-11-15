package com.android.server.notification;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AppGlobals;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
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
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.widget.RemoteViews;
import com.android.server.LocalServices;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.am.HwActivityManagerService;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.lights.LightsService;
import com.android.server.notification.RankingHelper.NotificationSysMgrCfg;
import com.android.server.pm.UserManagerService;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.security.hsm.HwAddViewHelper;
import com.android.server.security.trustspace.TrustSpaceManagerInternal;
import com.android.server.statusbar.HwStatusBarManagerService;
import com.android.server.statusbar.HwStatusBarManagerService.HwNotificationDelegate;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.app.IGameObserver.Stub;
import com.huawei.recsys.aidl.HwRecSysAidlInterface;
import huawei.cust.HwCustUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HwNotificationManagerService extends NotificationManagerService {
    private static final String BIND_ACTION = "com.huawei.recsys.action.THIRD_REQUEST_ENGINE";
    private static final long BIND_TIMEOUT = 10000;
    private static final String BUSINESS_NAME = "notification";
    private static final String CONTENTVIEW_REVERTING_FLAG = "HW_CONTENTVIEW_REVERTING_FLAG";
    private static final boolean CUST_DIALER_ENABLE = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    private static final boolean DEBUG = true;
    private static final int EVENT_MARK_AS_GAME = 4;
    private static final int EVENT_MOVE_BACKGROUND = 2;
    private static final int EVENT_MOVE_FRONT = 1;
    private static final int EVENT_REPLACE_FRONT = 3;
    private static final boolean HWRIDEMODE_FEATURE_SUPPORTED = SystemProperties.getBoolean("ro.config.ride_mode", false);
    static final int INIT_CFG_DELAY = 10000;
    private static final String KEY_SMART_NOTIFICATION_SWITCH = "smart_notification_switch";
    private static final String NOTIFICATION_ACTION_ALLOW = "com.huawei.notificationmanager.notification.allow";
    private static final String NOTIFICATION_ACTION_ALLOW_FORAPK = "com.huawei.notificationmanager.notification.allow.forAPK";
    private static final String NOTIFICATION_ACTION_REFUSE = "com.huawei.notificationmanager.notification.refuse";
    private static final String NOTIFICATION_ACTION_REFUSE_FORAPK = "com.huawei.notificationmanager.notification.refuse.forAPK";
    private static final String NOTIFICATION_CENTER_ORIGIN_PKG = "hw_origin_sender_package_name";
    private static final String NOTIFICATION_CENTER_PKG = "com.huawei.android.pushagent";
    static final int NOTIFICATION_CFG_REMIND = 0;
    private static final String[] NOTIFICATION_NARROW_DISPLAY_APPS = new String[]{"com.android.contacts"};
    private static final int NOTIFICATION_PRIORITY_MULTIPLIER = 10;
    private static final String[] NOTIFICATION_WHITE_APPS_PACKAGE = new String[]{"com.huawei.intelligent"};
    private static final int OPERATOR_QUERY = 1;
    private static final String PACKAGE_NAME_HSM = "com.huawei.systemmanager";
    private static final String RIDEMODE_NOTIFICATION_WHITE_LIST = "com.huawei.ridemode,com.huawei.sos,com.android.phone,com.android.server.telecom,com.android.incallui,com.android.deskclock,com.android.cellbroadcastreceiver";
    private static final String RULE_NAME = "ongoingAndNormalRules";
    private static final String SERVER_PAKAGE_NAME = "com.huawei.recsys";
    private static final int SMCS_NOTIFICATION_GET_NOTI = 1;
    static final String TAG = "HwNotificationManagerService";
    private static final String TYPE_COUNSELING_MESSAGE = "102";
    private static final String TYPE_IGNORE = "0";
    private static final String TYPE_INFORMATION = "3";
    private static final String TYPE_MUSIC = "103";
    private static final String TYPE_PROMOTION = "2";
    private static final String TYPE_TOOLS = "107";
    private static final String WHITELIST_LABLE = "200";
    private static final boolean mIsChina = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static int sRemoveableFlag;
    private static int sUpdateRemovableFlag;
    final Uri URI_NOTIFICATION_CFG;
    private BatteryManagerInternal mBatteryManagerInternal;
    private List<String> mBtwList;
    DBContentObserver mCfgDBObserver;
    Map<String, Integer> mCfgMap;
    private ContentObserver mContentObserver;
    Context mContext;
    private HwCustZenModeHelper mCust;
    private BroadcastReceiver mHangButtonReceiver;
    private HwGameObserver mHwGameObserver;
    Handler mHwHandler;
    private final HwNotificationDelegate mHwNotificationDelegate;
    private HwRecSysAidlInterface mHwRecSysAidlInterface;
    private Object mLock;
    private ArrayList<NotificationContentViewRecord> mOriginContentViews;
    private Handler mRecHandler;
    private HandlerThread mRecHandlerThread;
    private ArrayMap<String, String> mRecognizeMap;
    private ServiceConnection mServiceConnection;
    private ArrayMap<Integer, Boolean> mSmartNtfSwitchMap;

    private class DBContentObserver extends ContentObserver {
        public DBContentObserver() {
            super(null);
        }

        public void onChange(boolean selfChange) {
            Slog.v(HwNotificationManagerService.TAG, "Notification db cfg changed");
            HwNotificationManagerService.this.mHwHandler.post(new HwCfgLoadingRunnable(HwNotificationManagerService.this, null));
        }
    }

    private class HwCfgLoadingRunnable implements Runnable {
        private HwCfgLoadingRunnable() {
        }

        /* synthetic */ HwCfgLoadingRunnable(HwNotificationManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void run() {
            new Thread("HwCfgLoading") {
                public void run() {
                    HwCfgLoadingRunnable.this.load();
                }
            }.start();
        }

        /* JADX WARNING: Removed duplicated region for block: B:70:0x0178  */
        /* JADX WARNING: Removed duplicated region for block: B:70:0x0178  */
        /* JADX WARNING: Missing block: B:47:0x0142, code:
            if (r2 != null) goto L_0x0169;
     */
        /* JADX WARNING: Missing block: B:60:0x015a, code:
            if (r2 == null) goto L_0x016c;
     */
        /* JADX WARNING: Missing block: B:64:0x0167, code:
            if (r2 != null) goto L_0x0169;
     */
        /* JADX WARNING: Missing block: B:65:0x0169, code:
            r2.close();
     */
        /* JADX WARNING: Missing block: B:66:0x016c, code:
            android.util.Slog.v(com.android.server.notification.HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
     */
        /* JADX WARNING: Missing block: B:67:0x0174, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void load() {
            Exception e;
            RuntimeException e2;
            Object obj;
            Throwable th;
            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Starts ");
            Cursor cursor = null;
            Context context = null;
            try {
                e = HwNotificationManagerService.this.mContext.createPackageContextAsUser("com.huawei.systemmanager", 0, new UserHandle(ActivityManager.getCurrentUser()));
            } catch (Exception e3) {
                Slog.w(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Fail to convert context");
                e = null;
            }
            context = e;
            if (context != null) {
                Context context2;
                try {
                    cursor = context.getContentResolver().query(HwNotificationManagerService.this.URI_NOTIFICATION_CFG, null, null, null, null);
                    if (cursor == null) {
                        try {
                            Slog.w(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Fail to get cfg from DB");
                            if (cursor != null) {
                                cursor.close();
                            }
                            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                            return;
                        } catch (RuntimeException e4) {
                            e2 = e4;
                            obj = context;
                            Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : RuntimeException ", e2);
                        } catch (Exception e5) {
                            e = e5;
                            obj = context;
                            Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Exception ", e);
                        } catch (Throwable th2) {
                            th = th2;
                            obj = context;
                            if (cursor != null) {
                                cursor.close();
                            }
                            Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                            throw th;
                        }
                    }
                    HashMap<String, Integer> tempMap = new HashMap();
                    ArrayList<NotificationSysMgrCfg> tempSysMgrCfgList = new ArrayList();
                    if (cursor.getCount() > 0) {
                        int nPkgColIndex = cursor.getColumnIndex("packageName");
                        int nCfgColIndex = cursor.getColumnIndex("sound_vibrate");
                        int nCidColIndex = cursor.getColumnIndex("channelid");
                        int nLockscreenColIndex = cursor.getColumnIndex("lockscreencfg");
                        int nImportaceColIndex = cursor.getColumnIndex("channelimportance");
                        int nBypassdndColIndex = cursor.getColumnIndex("channelbypassdnd");
                        int nIconDadgeColIndex = cursor.getColumnIndex("channeliconbadge");
                        while (cursor.moveToNext()) {
                            int nPkgColIndex2;
                            String pkgName = cursor.getString(nPkgColIndex);
                            if (TextUtils.isEmpty(pkgName)) {
                                nPkgColIndex2 = nPkgColIndex;
                                context2 = context;
                            } else {
                                int cfg = cursor.getInt(nCfgColIndex);
                                String channelId = cursor.getString(nCidColIndex);
                                String key = HwNotificationManagerService.this.pkgAndCidKey(pkgName, channelId);
                                nPkgColIndex2 = nPkgColIndex;
                                if ("miscellaneous".equals(channelId)) {
                                    NotificationSysMgrCfg mgrCfg = new NotificationSysMgrCfg();
                                    context2 = context;
                                    try {
                                        mgrCfg.smc_userId = ActivityManager.getCurrentUser();
                                        mgrCfg.smc_packageName = pkgName;
                                        mgrCfg.smc_visilibity = cursor.getInt(nLockscreenColIndex);
                                        mgrCfg.smc_importance = cursor.getInt(nImportaceColIndex);
                                        mgrCfg.smc_bypassDND = cursor.getInt(nBypassdndColIndex);
                                        mgrCfg.smc_iconBadge = cursor.getInt(nIconDadgeColIndex);
                                        tempSysMgrCfgList.add(mgrCfg);
                                    } catch (RuntimeException e6) {
                                        e2 = e6;
                                    } catch (Exception e7) {
                                        e = e7;
                                        Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Exception ", e);
                                    }
                                } else {
                                    context2 = context;
                                }
                                tempMap.put(key, Integer.valueOf(cfg));
                            }
                            nPkgColIndex = nPkgColIndex2;
                            context = context2;
                        }
                    }
                    HwNotificationManagerService.this.setSysMgrCfgMap(tempSysMgrCfgList);
                    synchronized (HwNotificationManagerService.this.mCfgMap) {
                        HwNotificationManagerService.this.mCfgMap.clear();
                        HwNotificationManagerService.this.mCfgMap.putAll(tempMap);
                        String str = HwNotificationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("HwCfgLoadingRunnable: get cfg size:");
                        stringBuilder.append(HwNotificationManagerService.this.mCfgMap.size());
                        Slog.v(str, stringBuilder.toString());
                    }
                } catch (RuntimeException e8) {
                    e2 = e8;
                    context2 = context;
                    Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : RuntimeException ", e2);
                } catch (Exception e9) {
                    e = e9;
                    context2 = context;
                    Slog.e(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable : Exception ", e);
                } catch (Throwable th3) {
                    th = th3;
                    if (cursor != null) {
                    }
                    Slog.v(HwNotificationManagerService.TAG, "HwCfgLoadingRunnable: Ends. ");
                    throw th;
                }
            }
        }
    }

    private class HwGameObserver extends Stub {
        private HwGameObserver() {
        }

        /* synthetic */ HwGameObserver(HwNotificationManagerService x0, AnonymousClass1 x1) {
            this();
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
            String str = HwNotificationManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onGameStatusChanged event=");
            stringBuilder.append(event);
            stringBuilder.append(",mGameDndStatus=");
            stringBuilder.append(HwNotificationManagerService.this.mGameDndStatus);
            Log.d(str, stringBuilder.toString());
        }
    }

    private static final class NotificationContentViewRecord {
        final int id;
        final String pkg;
        RemoteViews rOldBigContentView;
        final String tag;
        final int uid;
        final int userId;

        NotificationContentViewRecord(String pkg, int uid, String tag, int id, int userId, RemoteViews rOldBigContentView) {
            this.pkg = pkg;
            this.tag = tag;
            this.uid = uid;
            this.id = id;
            this.userId = userId;
            this.rOldBigContentView = rOldBigContentView;
        }

        public final String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NotificationContentViewRecord{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" pkg=");
            stringBuilder.append(this.pkg);
            stringBuilder.append(" uid=");
            stringBuilder.append(this.uid);
            stringBuilder.append(" id=");
            stringBuilder.append(Integer.toHexString(this.id));
            stringBuilder.append(" tag=");
            stringBuilder.append(this.tag);
            stringBuilder.append(" userId=");
            stringBuilder.append(Integer.toHexString(this.userId));
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    static {
        sRemoveableFlag = 0;
        sUpdateRemovableFlag = 0;
        sRemoveableFlag = getStaticIntFiled("com.huawei.android.content.pm.PackageParserEx", "PARSE_IS_REMOVABLE_PREINSTALLED_APK");
        sUpdateRemovableFlag = getStaticIntFiled("com.huawei.android.content.pm.PackageParserEx", "FLAG_UPDATED_REMOVEABLE_APP");
    }

    private StatusBarManagerService getStatusBarManagerService() {
        return (StatusBarManagerService) ServiceManager.getService("statusbar");
    }

    public void onStart() {
        super.onStart();
        this.mBatteryManagerInternal = (BatteryManagerInternal) getLocalService(BatteryManagerInternal.class);
    }

    public HwNotificationManagerService(Context context, StatusBarManagerService statusBar, LightsService lights) {
        super(context);
        this.mHwHandler = null;
        this.mCfgDBObserver = null;
        this.mCfgMap = new HashMap();
        this.URI_NOTIFICATION_CFG = Uri.parse("content://com.huawei.systemmanager.NotificationDBProvider/notificationCfg");
        this.mLock = new Object();
        this.mRecognizeMap = new ArrayMap();
        this.mSmartNtfSwitchMap = new ArrayMap();
        this.mHwGameObserver = null;
        this.mCust = (HwCustZenModeHelper) HwCustUtils.createObj(HwCustZenModeHelper.class, new Object[0]);
        this.mBtwList = new ArrayList();
        this.mBtwList.add("2");
        this.mBtwList.add("3");
        this.mBtwList.add("102");
        this.mBtwList.add(TYPE_TOOLS);
        this.mHwNotificationDelegate = new HwNotificationDelegate() {
            public void onNotificationResidentClear(String pkg, String tag, int id, int userId) {
                HwNotificationManagerService.this.cancelNotification(Binder.getCallingUid(), Binder.getCallingPid(), pkg, tag, id, 0, 0, true, userId, 2, null);
            }
        };
        this.mHangButtonReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null) {
                    String action = intent.getAction();
                    String pkg;
                    int uid;
                    if (HwNotificationManagerService.NOTIFICATION_ACTION_ALLOW.equals(action)) {
                        pkg = intent.getStringExtra(AwareIntelligentRecg.CMP_PKGNAME);
                        uid = intent.getIntExtra("uid", -1);
                        if (!(pkg == null || uid == -1)) {
                            HwNotificationManagerService.this.recoverNotificationContentView(pkg, uid);
                        }
                    } else if (action != null && action.equals(HwNotificationManagerService.NOTIFICATION_ACTION_REFUSE)) {
                        pkg = intent.getStringExtra(AwareIntelligentRecg.CMP_PKGNAME);
                        uid = intent.getIntExtra("uid", -1);
                        if (!(pkg == null || uid == -1)) {
                            synchronized (HwNotificationManagerService.this.mOriginContentViews) {
                                Iterator<NotificationContentViewRecord> itor = HwNotificationManagerService.this.mOriginContentViews.iterator();
                                while (itor.hasNext()) {
                                    NotificationContentViewRecord cvr = (NotificationContentViewRecord) itor.next();
                                    if (cvr.pkg.equals(pkg) && cvr.uid == uid) {
                                        itor.remove();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
        this.mOriginContentViews = new ArrayList();
        this.mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(HwNotificationManagerService.TAG, "onServiceConnected");
                HwNotificationManagerService.this.mHwRecSysAidlInterface = HwRecSysAidlInterface.Stub.asInterface(service);
                synchronized (HwNotificationManagerService.this.mLock) {
                    HwNotificationManagerService.this.mLock.notifyAll();
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.i(HwNotificationManagerService.TAG, "onServiceDisConnected");
                HwNotificationManagerService.this.mHwRecSysAidlInterface = null;
                synchronized (HwNotificationManagerService.this.mLock) {
                    HwNotificationManagerService.this.mLock.notifyAll();
                }
            }
        };
        this.mContentObserver = new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                HwNotificationManagerService.this.mRecHandler.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        int userId = ActivityManager.getCurrentUser();
                        boolean enable = System.getIntForUser(HwNotificationManagerService.this.mContext.getContentResolver(), HwNotificationManagerService.KEY_SMART_NOTIFICATION_SWITCH, HwNotificationManagerService.mIsChina, userId) == 1;
                        boolean allUserClose = true;
                        synchronized (HwNotificationManagerService.this.mSmartNtfSwitchMap) {
                            HwNotificationManagerService.this.mSmartNtfSwitchMap.put(Integer.valueOf(userId), Boolean.valueOf(enable));
                            if (HwNotificationManagerService.this.mSmartNtfSwitchMap.containsValue(Boolean.valueOf(true))) {
                                allUserClose = false;
                            }
                        }
                        if (allUserClose) {
                            HwNotificationManagerService.this.unBind();
                        } else {
                            HwNotificationManagerService.this.bindRecSys();
                        }
                        String str = HwNotificationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("switch change to: ");
                        stringBuilder.append(enable);
                        stringBuilder.append(",userId: ");
                        stringBuilder.append(userId);
                        stringBuilder.append(", allUserClose :");
                        stringBuilder.append(allUserClose);
                        Log.i(str, stringBuilder.toString());
                    }
                });
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
        this.mRecognizeMap = new ArrayMap();
        this.mSmartNtfSwitchMap = new ArrayMap();
        this.mHwGameObserver = null;
        this.mCust = (HwCustZenModeHelper) HwCustUtils.createObj(HwCustZenModeHelper.class, new Object[0]);
        this.mBtwList = new ArrayList();
        this.mBtwList.add("2");
        this.mBtwList.add("3");
        this.mBtwList.add("102");
        this.mBtwList.add(TYPE_TOOLS);
        this.mHwNotificationDelegate = /* anonymous class already generated */;
        this.mHangButtonReceiver = /* anonymous class already generated */;
        this.mOriginContentViews = new ArrayList();
        this.mServiceConnection = /* anonymous class already generated */;
        this.mContentObserver = /* anonymous class already generated */;
        this.mContext = context;
        SystemServerInitThreadPool.get().submit(new -$$Lambda$HwNotificationManagerService$ZCrKpb82-sziMOc51dwZrcRLjWI(this), "HwNotificationManagerService init");
    }

    private void init() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_ACTION_ALLOW);
        filter.addAction(NOTIFICATION_ACTION_REFUSE);
        this.mContext.registerReceiverAsUser(this.mHangButtonReceiver, UserHandle.ALL, filter, "com.android.permission.system_manager_interface", null);
        StatusBarManagerService sb = getStatusBarManagerService();
        if (sb instanceof HwStatusBarManagerService) {
            ((HwStatusBarManagerService) sb).setHwNotificationDelegate(this.mHwNotificationDelegate);
        }
        this.mHwHandler = new Handler(Looper.getMainLooper());
        this.mHwHandler.postDelayed(new HwCfgLoadingRunnable(this, null), 10000);
        this.mCfgDBObserver = new DBContentObserver();
        this.mContext.getContentResolver().registerContentObserver(this.URI_NOTIFICATION_CFG, true, this.mCfgDBObserver, ActivityManager.getCurrentUser());
        this.mRecHandlerThread = new HandlerThread("notification manager");
        this.mRecHandlerThread.start();
        this.mRecHandler = new Handler(this.mRecHandlerThread.getLooper());
        try {
            ContentResolver cr = this.mContext.getContentResolver();
            if (cr != null) {
                cr.registerContentObserver(System.getUriFor(KEY_SMART_NOTIFICATION_SWITCH), false, this.mContentObserver, -1);
            }
            this.mContentObserver.onChange(true);
        } catch (Exception e) {
            Log.w(TAG, "init failed", e);
        }
        registerHwGameObserver();
    }

    protected void handleGetNotifications(Parcel data, Parcel reply) {
        HashSet<String> notificationPkgs = new HashSet();
        if (this.mContext.checkCallingPermission("huawei.permission.IBINDER_NOTIFICATION_SERVICE") != 0) {
            Slog.e(TAG, "NotificationManagerService.handleGetNotifications: permissin deny");
            return;
        }
        getNotificationPkgs_hwHsm(notificationPkgs);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NotificationManagerService.handleGetNotifications: got ");
        stringBuilder.append(notificationPkgs.size());
        stringBuilder.append(" pkgs");
        Slog.v(str, stringBuilder.toString());
        reply.writeInt(notificationPkgs.size());
        Iterator<String> it = notificationPkgs.iterator();
        while (it.hasNext()) {
            String pkg = (String) it.next();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NotificationManagerService.handleGetNotifications: reply ");
            stringBuilder.append(pkg);
            Slog.v(str, stringBuilder.toString());
            reply.writeString(pkg);
        }
    }

    void getNotificationPkgs_hwHsm(HashSet<String> notificationPkgs) {
        if (notificationPkgs != null) {
            if (notificationPkgs.size() > 0) {
                notificationPkgs.clear();
            }
            synchronized (this.mNotificationList) {
                int N = this.mNotificationList.size();
                if (N == 0) {
                    return;
                }
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

    private int indexOfContentViewLocked(String pkg, String tag, int id, int userId) {
        synchronized (this.mOriginContentViews) {
            ArrayList<NotificationContentViewRecord> list = this.mOriginContentViews;
            int len = list.size();
            for (int i = 0; i < len; i++) {
                NotificationContentViewRecord r = (NotificationContentViewRecord) list.get(i);
                if ((userId == -1 || r.userId == -1 || r.userId == userId) && r.id == id) {
                    if (tag == null) {
                        if (r.tag != null) {
                            continue;
                        }
                    } else if (!tag.equals(r.tag)) {
                        continue;
                    }
                    if (r.pkg.equals(pkg)) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }

    private void recoverNotificationContentView(String pkg, int uid) {
        String str = pkg;
        synchronized (this.mOriginContentViews) {
            Iterator<NotificationContentViewRecord> itor = this.mOriginContentViews.iterator();
            while (itor.hasNext()) {
                NotificationContentViewRecord cvr = (NotificationContentViewRecord) itor.next();
                if (cvr.pkg.equals(str)) {
                    NotificationRecord r = findNotificationByListLocked(this.mNotificationList, cvr.pkg, cvr.tag, cvr.id, cvr.userId);
                    if (r != null) {
                        r.sbn.getNotification().bigContentView = cvr.rOldBigContentView;
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("revertNotificationView enqueueNotificationInternal pkg=");
                        stringBuilder.append(str);
                        stringBuilder.append(" id=");
                        stringBuilder.append(r.sbn.getId());
                        stringBuilder.append(" userId=");
                        stringBuilder.append(r.sbn.getUserId());
                        Slog.d(str2, stringBuilder.toString());
                        r.sbn.getNotification().extras.putBoolean(CONTENTVIEW_REVERTING_FLAG, true);
                        enqueueNotificationInternal(str, r.sbn.getOpPkg(), r.sbn.getUid(), r.sbn.getInitialPid(), r.sbn.getTag(), r.sbn.getId(), r.sbn.getNotification(), r.sbn.getUserId());
                        itor.remove();
                    } else {
                        Slog.w(TAG, "Notification can't find in NotificationRecords");
                    }
                }
            }
        }
    }

    protected boolean isHwSoundAllow(String pkg, String channelId, int userId) {
        Integer cfg;
        String key = pkgAndCidKey(pkg, channelId);
        synchronized (this.mCfgMap) {
            cfg = (Integer) this.mCfgMap.get(key);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isHwSoundAllow pkg=");
        stringBuilder.append(pkg);
        stringBuilder.append(", channelId=");
        stringBuilder.append(channelId);
        stringBuilder.append(", userId=");
        stringBuilder.append(userId);
        stringBuilder.append(", cfg=");
        stringBuilder.append(cfg);
        Slog.v(str, stringBuilder.toString());
        return cfg == null || (cfg.intValue() & 1) != 0;
    }

    protected boolean isHwVibrateAllow(String pkg, String channelId, int userId) {
        Integer cfg;
        String key = pkgAndCidKey(pkg, channelId);
        synchronized (this.mCfgMap) {
            cfg = (Integer) this.mCfgMap.get(key);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isHwVibrateAllow pkg=");
        stringBuilder.append(pkg);
        stringBuilder.append(", channelId=");
        stringBuilder.append(channelId);
        stringBuilder.append(", userId=");
        stringBuilder.append(userId);
        stringBuilder.append(", cfg=");
        stringBuilder.append(cfg);
        Slog.v(str, stringBuilder.toString());
        return cfg == null || (cfg.intValue() & 2) != 0;
    }

    private String pkgAndCidKey(String pkg, String channelId) {
        StringBuilder key = new StringBuilder();
        key.append(pkg);
        key.append(Constant.RESULT_SEPERATE);
        key.append(channelId);
        return key.toString();
    }

    @Deprecated
    protected int modifyScoreBySM(String pkg, int callingUid, int origScore) {
        return origScore;
    }

    protected void detectNotifyBySM(int callingUid, String pkg, Notification notification) {
        Intent intent = new Intent("com.huawei.notificationmanager.detectnotify");
        intent.putExtra("callerUid", callingUid);
        intent.putExtra("packageName", pkg);
        Bundle bundle = new Bundle();
        bundle.putParcelable("sendNotify", notification);
        intent.putExtra("notifyBundle", bundle);
    }

    protected void hwEnqueueNotificationWithTag(String pkg, int uid, NotificationRecord nr) {
        if (nr.sbn.getNotification().extras.getBoolean(CONTENTVIEW_REVERTING_FLAG, false)) {
            nr.sbn.getNotification().extras.putBoolean(CONTENTVIEW_REVERTING_FLAG, false);
        }
    }

    protected boolean inNonDisturbMode(String pkg) {
        if (pkg == null) {
            return false;
        }
        return isWhiteApp(pkg);
    }

    private boolean isWhiteApp(String pkg) {
        if (this.mCust != null && this.mCust.getWhiteApps(this.mContext) != null) {
            return Arrays.asList(this.mCust.getWhiteApps(this.mContext)).contains(pkg);
        }
        String[] DEFAULT_WHITEAPP = new String[]{"com.android.mms", "com.android.contacts"};
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

    protected boolean isImportantNotification(String pkg, Notification notification) {
        if (notification == null || notification.priority < 3) {
            return false;
        }
        if ((pkg.equals("com.android.phone") || pkg.equals("com.android.mms") || pkg.equals("com.android.contacts") || pkg.equals("com.android.server.telecom")) && notification.priority < 7) {
            return true;
        }
        return false;
    }

    protected boolean isMmsNotificationEnable(String pkg) {
        if (pkg == null || (!pkg.equalsIgnoreCase("com.android.mms") && !pkg.equalsIgnoreCase("com.android.contacts"))) {
            return false;
        }
        return true;
    }

    protected void hwCancelNotification(String pkg, String tag, int id, int userId) {
        synchronized (this.mOriginContentViews) {
            int indexView = indexOfContentViewLocked(pkg, tag, id, userId);
            if (indexView >= 0) {
                this.mOriginContentViews.remove(indexView);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hwCancelNotification: pkg = ");
                stringBuilder.append(pkg);
                stringBuilder.append(", id = ");
                stringBuilder.append(id);
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    protected void updateLight(boolean enable, int ledOnMS, int ledOffMS) {
        this.mBatteryManagerInternal.updateBatteryLight(enable, ledOnMS, ledOffMS);
    }

    protected void handleUserSwitchEvents(int userId) {
        if (this.mCfgDBObserver != null) {
            this.mHwHandler.post(new HwCfgLoadingRunnable(this, null));
            this.mContext.getContentResolver().unregisterContentObserver(this.mCfgDBObserver);
            this.mContext.getContentResolver().registerContentObserver(this.URI_NOTIFICATION_CFG, true, this.mCfgDBObserver, ActivityManager.getCurrentUser());
        }
    }

    protected void stopPlaySound() {
        this.mSoundNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
            if (player != null) {
                player.stopAsync();
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
        Binder.restoreCallingIdentity(identity);
    }

    protected boolean isAFWUserId(int userId) {
        boolean temp = false;
        long token = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = UserManagerService.getInstance().getUserInfo(userId);
            if (userInfo != null) {
                boolean z = userInfo.isManagedProfile() || userInfo.isClonedProfile();
                temp = z;
            }
        } catch (Exception e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
        Binder.restoreCallingIdentity(token);
        return temp;
    }

    protected void addHwExtraForNotification(Notification notification, String pkg, int pid) {
        if (isIntentProtectedApp(pkg)) {
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

    protected int getNCTargetAppUid(String opPkg, String pkg, int defaultUid, Notification notification) {
        int uid = defaultUid;
        if (!NOTIFICATION_CENTER_PKG.equals(opPkg)) {
            return uid;
        }
        Bundle bundle = notification.extras;
        if (bundle == null) {
            return uid;
        }
        String targetPkg = bundle.getString(NOTIFICATION_CENTER_ORIGIN_PKG);
        if (targetPkg == null || !targetPkg.equals(pkg)) {
            return uid;
        }
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(targetPkg, 0, UserHandle.getCallingUserId());
            if (ai != null) {
                return ai.uid;
            }
            return uid;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown package pkg:");
            stringBuilder.append(targetPkg);
            Slog.w(str, stringBuilder.toString());
            return uid;
        }
    }

    protected String getNCTargetAppPkg(String opPkg, String defaultPkg, Notification notification) {
        String pkg = defaultPkg;
        if (!NOTIFICATION_CENTER_PKG.equals(opPkg)) {
            return pkg;
        }
        Bundle bundle = notification.extras;
        if (bundle == null) {
            return pkg;
        }
        String targetPkg = bundle.getString(NOTIFICATION_CENTER_ORIGIN_PKG);
        if (targetPkg == null || !isVaildPkg(targetPkg)) {
            return pkg;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Notification Center targetPkg:");
        stringBuilder.append(targetPkg);
        Slog.v(str, stringBuilder.toString());
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

    protected boolean isBlockRideModeNotification(String pkg) {
        return HWRIDEMODE_FEATURE_SUPPORTED && SystemProperties.getBoolean("sys.ride_mode", false) && !RIDEMODE_NOTIFICATION_WHITE_LIST.contains(pkg);
    }

    public void reportToIAware(String pkg, int uid, int nid, boolean added) {
        if (pkg != null && !pkg.isEmpty()) {
            HwSysResManager resManager = HwSysResManager.getInstance();
            if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC))) {
                Bundle bundleArgs = new Bundle();
                bundleArgs.putString(MemoryConstant.MEM_PREREAD_ITEM_NAME, pkg);
                bundleArgs.putInt("tgtUid", uid);
                bundleArgs.putInt("notification_id", nid);
                bundleArgs.putInt("relationType", added ? 20 : 21);
                CollectData data = new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
                long id = Binder.clearCallingIdentity();
                resManager.reportData(data);
                Binder.restoreCallingIdentity(id);
            }
        }
    }

    protected boolean isFromPinNotification(Notification notification, String pkg) {
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

    protected boolean isGameRunningForeground() {
        HwActivityManagerService mAms = HwActivityManagerService.self();
        if (mAms != null) {
            return mAms.isGameDndOn();
        }
        Slog.e(TAG, "Canot obtain HwActivityManagerService , mAms is null");
        return false;
    }

    protected boolean isGameDndSwitchOn() {
        String GAME_DND_MODE = "game_dnd_mode";
        if (Secure.getInt(this.mContext.getContentResolver(), "game_dnd_mode", 2) == 1) {
            return true;
        }
        return false;
    }

    protected boolean isPackageRequestNarrowNotification() {
        String topPkg = getTopPkgName();
        for (String pkg : NOTIFICATION_NARROW_DISPLAY_APPS) {
            if (pkg.equalsIgnoreCase(topPkg)) {
                return true;
            }
        }
        return false;
    }

    private String getTopPkgName() {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        List<RunningTaskInfo> tasks = null;
        if (am != null) {
            tasks = am.getRunningTasks(1);
        }
        RunningTaskInfo runningTaskInfo = null;
        if (!(tasks == null || tasks.isEmpty())) {
            runningTaskInfo = (RunningTaskInfo) tasks.get(0);
        }
        if (runningTaskInfo != null) {
            return runningTaskInfo.topActivity.getPackageName();
        }
        return "";
    }

    private void registerHwGameObserver() {
        if (this.mHwGameObserver == null) {
            this.mHwGameObserver = new HwGameObserver(this, null);
        }
        HwActivityManagerService.self().registerGameObserver(this.mHwGameObserver);
    }

    private void updateNotificationInGameMode() {
        synchronized (this.mNotificationLock) {
            updateLightsLocked();
        }
    }

    public void bindRecSys() {
        this.mRecHandler.post(new Runnable() {
            public void run() {
                HwNotificationManagerService.this.bind();
            }
        });
    }

    private void bind() {
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bind service finish, ret=");
            stringBuilder.append(ret);
            Log.i(str, stringBuilder.toString());
        } catch (Exception e) {
            Log.e(TAG, "bind service failed!", e);
        }
    }

    private void unBind() {
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

    /* JADX WARNING: Missing block: B:22:0x00aa, code:
            if (isSystemApp(r15, r12) == false) goto L_0x00c2;
     */
    /* JADX WARNING: Missing block: B:23:0x00ac, code:
            android.util.Log.i(TAG, "recognize: system app");
            r2 = r1.mRecognizeMap;
     */
    /* JADX WARNING: Missing block: B:24:0x00b5, code:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:26:?, code:
            r1.mRecognizeMap.put(r8, "0");
     */
    /* JADX WARNING: Missing block: B:27:0x00bd, code:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:28:0x00be, code:
            return;
     */
    /* JADX WARNING: Missing block: B:33:0x00c4, code:
            if (r1.mHwRecSysAidlInterface == null) goto L_0x0194;
     */
    /* JADX WARNING: Missing block: B:36:0x00ce, code:
            r2 = r2;
            r19 = r8;
     */
    /* JADX WARNING: Missing block: B:38:?, code:
            r3 = r1.mHwRecSysAidlInterface.doNotificationCollect(new android.service.notification.StatusBarNotification(r15, r15, r10, r11, r12, r13, r14, r25, null, java.lang.System.currentTimeMillis()));
     */
    /* JADX WARNING: Missing block: B:39:0x00e9, code:
            if (r3 != null) goto L_0x00ec;
     */
    /* JADX WARNING: Missing block: B:40:0x00eb, code:
            return;
     */
    /* JADX WARNING: Missing block: B:42:0x00f2, code:
            if (r3.equals("0") == false) goto L_0x011d;
     */
    /* JADX WARNING: Missing block: B:43:0x00f4, code:
            r4 = r1.mRecognizeMap;
     */
    /* JADX WARNING: Missing block: B:44:0x00f6, code:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:49:?, code:
            r1.mRecognizeMap.put(r19, r3);
     */
    /* JADX WARNING: Missing block: B:50:0x00fe, code:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:52:?, code:
            r0 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("recognize: just ignore type : ");
            r4.append(r3);
            android.util.Log.d(r0, r4.toString());
     */
    /* JADX WARNING: Missing block: B:53:0x0115, code:
            return;
     */
    /* JADX WARNING: Missing block: B:54:0x0116, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:55:0x0118, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:56:0x0119, code:
            r5 = r19;
     */
    /* JADX WARNING: Missing block: B:58:?, code:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:60:?, code:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:61:0x011d, code:
            r5 = r19;
     */
    /* JADX WARNING: Missing block: B:62:0x0125, code:
            if (r3.equals("103") == false) goto L_0x0132;
     */
    /* JADX WARNING: Missing block: B:63:0x0127, code:
            r14.extras.putString("hw_type", "type_music");
     */
    /* JADX WARNING: Missing block: B:64:0x0132, code:
            r14.extras.putString("hw_type", r3);
     */
    /* JADX WARNING: Missing block: B:65:0x0139, code:
            r14.extras.putBoolean("hw_btw", r1.mBtwList.contains(r3));
            r0 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("doNotificationCollect: pkg=");
            r4.append(r15);
            r4.append(", uid=");
     */
    /* JADX WARNING: Missing block: B:68:?, code:
            r4.append(r27);
            r4.append(", hw_type=");
            r4.append(r3);
            r4.append(", hw_btw=");
            r4.append(r1.mBtwList.contains(r3));
            android.util.Log.i(r0, r4.toString());
     */
    /* JADX WARNING: Missing block: B:69:0x017d, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:70:0x017f, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:71:0x0180, code:
            r6 = r27;
     */
    /* JADX WARNING: Missing block: B:72:0x0183, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:73:0x0184, code:
            r5 = r19;
            r6 = r27;
     */
    /* JADX WARNING: Missing block: B:74:0x0189, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:75:0x018a, code:
            r5 = r8;
            r6 = r12;
     */
    /* JADX WARNING: Missing block: B:76:0x018c, code:
            android.util.Log.e(TAG, "doNotificationCollect failed", r0);
     */
    /* JADX WARNING: Missing block: B:77:0x0194, code:
            r5 = r8;
            r6 = r12;
     */
    /* JADX WARNING: Missing block: B:78:0x0196, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void recognize(String tag, int id, Notification notification, UserHandle user, String pkg, int uid, int pid) {
        Throwable th;
        String str;
        int i;
        Notification notification2 = notification;
        String str2 = pkg;
        int i2 = uid;
        int i3 = pid;
        if (!mIsChina) {
            Log.i(TAG, "recognize: not in china");
        } else if (isFeartureDisable()) {
            Log.i(TAG, "recognize: feature is disabled");
        } else {
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("recognize: tag=");
            String str4 = tag;
            stringBuilder.append(str4);
            stringBuilder.append(", id=");
            int i4 = id;
            stringBuilder.append(i4);
            stringBuilder.append(", user=");
            stringBuilder.append(user);
            stringBuilder.append(", pkg=");
            stringBuilder.append(str2);
            stringBuilder.append(", uid=");
            stringBuilder.append(i2);
            stringBuilder.append(", callingPid=");
            stringBuilder.append(i3);
            Log.i(str3, stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str2);
            stringBuilder2.append(i3);
            String key = stringBuilder2.toString();
            synchronized (this.mRecognizeMap) {
                try {
                    if ("0".equals(this.mRecognizeMap.get(key))) {
                        try {
                            str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Return ! recognize the app not in list : ");
                            stringBuilder3.append(str2);
                            Log.i(str3, stringBuilder3.toString());
                        } catch (Throwable th2) {
                            th = th2;
                            str = key;
                            i = i2;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                    str = key;
                    i = i2;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }
    }

    private boolean isSystemApp(String pkg, int uid) {
        if ("android".equals(pkg)) {
            return true;
        }
        boolean z = false;
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(pkg, 0, UserHandle.getUserId(uid));
            if (ai == null) {
                return false;
            }
            if (ai.isSystemApp() && !isRemoveAblePreInstall(ai, pkg)) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isRemoveAblePreInstall(ApplicationInfo ai, String pkg) {
        boolean z = false;
        boolean removeable = false;
        try {
            int hwFlags = ((Integer) Class.forName("android.content.pm.ApplicationInfo").getField("hwFlags").get(ai)).intValue();
            if (!((sRemoveableFlag & hwFlags) == 0 && (sUpdateRemovableFlag & hwFlags) == 0)) {
                z = true;
            }
            return z;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return removeable;
        } catch (NoSuchFieldException e2) {
            e2.printStackTrace();
            return removeable;
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
            return removeable;
        } catch (Exception e4) {
            e4.printStackTrace();
            return removeable;
        }
    }

    private static int getStaticIntFiled(String clazzName, String fieldName) {
        try {
            return Class.forName(clazzName).getField(fieldName).getInt(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return 0;
        } catch (NoSuchFieldException e2) {
            e2.printStackTrace();
            return 0;
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
            return 0;
        } catch (Exception e4) {
            e4.printStackTrace();
            return 0;
        }
    }

    private boolean isFeartureDisable() {
        Throwable th;
        long callingId = Binder.clearCallingIdentity();
        boolean isDisable = false;
        boolean isDisable2 = false;
        try {
            int userId = ActivityManager.getCurrentUser();
            synchronized (this.mSmartNtfSwitchMap) {
                try {
                    if (this.mSmartNtfSwitchMap.containsKey(Integer.valueOf(userId)) && !((Boolean) this.mSmartNtfSwitchMap.get(Integer.valueOf(userId))).booleanValue()) {
                        isDisable = true;
                    }
                    try {
                    } catch (Throwable th2) {
                        Throwable th3 = th2;
                        isDisable2 = isDisable;
                        th = th3;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    throw th;
                }
            }
            return isDisable;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    protected boolean isNotificationDisable() {
        return HwDeviceManager.disallowOp(102);
    }

    public boolean isAllowToShow(String pkg, ActivityInfo topActivity) {
        boolean hsmCheck = true;
        boolean isNotTopApp = (topActivity == null || pkg == null || pkg.equals(topActivity.packageName)) ? false : true;
        if (!isNotTopApp) {
            return true;
        }
        int uid = Binder.getCallingUid();
        long restoreCurId = Binder.clearCallingIdentity();
        try {
            hsmCheck = HwAddViewHelper.getInstance(getContext()).addViewPermissionCheck(pkg, 2, uid);
            return hsmCheck;
        } finally {
            Binder.restoreCallingIdentity(restoreCurId);
        }
    }

    protected boolean isCustDialer(String packageName) {
        return CUST_DIALER_ENABLE && "com.android.dialer".equals(packageName);
    }
}
