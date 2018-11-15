package com.android.server.am;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.IHwActivityNotifier;
import android.app.KeyguardManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.cover.CoverManager;
import android.cover.HallState;
import android.cover.IHallCallback.Stub;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IMWThirdpartyCallback;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimingsTraceLog;
import android.view.MotionEvent;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import android.widget.Toast;
import android.zrhung.ZrHungData;
import com.android.server.HwServiceFactory;
import com.android.server.PPPOEStateMachine;
import com.android.server.ServiceThread;
import com.android.server.UiThread;
import com.android.server.am.ActivityStack.ActivityState;
import com.android.server.am.AppNotRespondingDialog.Data;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.os.HwBootCheck;
import com.android.server.pm.HwPackageManagerService;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.security.hsm.HwAddViewHelper;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.hsm.permission.ANRFilter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class HwActivityManagerServiceEx implements IHwActivityManagerServiceEx {
    private static final String ACTION_CONFIRM_APPLOCK_CREDENTIAL = "huawei.intent.action.APPLOCK_FRAMEWORK_MANAGER";
    private static final String ACTION_CONFIRM_APPLOCK_PACKAGENAME = "com.huawei.systemmanager";
    private static final String ACTION_HWOUC_SHOW_UPGRADE_REMIND = "com.huawei.android.hwouc.action.SHOW_UPGRADE_REMIND";
    private static final Set<String> ACTIVITY_NOTIFIER_TYPES = new HashSet<String>() {
        {
            add("returnToHome");
            add("activityLifeState");
            add("appSwitch");
        }
    };
    private static final String APKPATCH_META_DATA = "android.huawei.MARKETED_SYSTEM_APP";
    private static final int APP_ASSOC_HOME_UPDATE = 11;
    private static final String ASSOC_CALL_PID = "callPid";
    private static final String ASSOC_CALL_PROCNAME = "callProcName";
    private static final String ASSOC_CALL_UID = "callUid";
    private static final String ASSOC_PID = "pid";
    private static final String ASSOC_PKGNAME = "pkgname";
    private static final String ASSOC_RELATION_TYPE = "relationType";
    private static final int ASSOC_REPORT_MIN_TIME = 60000;
    private static final String ASSOC_TGT_COMPNAME = "compName";
    private static final String ASSOC_TGT_PROCNAME = "tgtProcName";
    private static final String ASSOC_TGT_UID = "tgtUid";
    private static final int CACHED_PROCESS_LIMIT = 8;
    private static final Set<String> CLONEPROFILE_PERMISSION = new HashSet<String>() {
        {
            add("com.huawei.hidisk");
            add("com.android.gallery3d");
            add("com.hicloud.android.clone");
            add("com.huawei.KoBackup");
        }
    };
    static final boolean DEBUG_HWTRIM = smcsLOGV;
    static final boolean DEBUG_HWTRIM_PERFORM = smcsLOGV;
    private static final Set<String> EXEMPTED_AUTHORITIES = new HashSet<String>() {
        {
            add("com.huawei.systemmanager.fileProvider");
            add("com.huawei.pcassistant.provider");
        }
    };
    private static final int FG_TO_TOP_APP_MSG = 70;
    private static final int HWOUC_UPDATE_REMIND_MSG = 80;
    private static final String HW_LAUNCHER_PKGNAME = "com.huawei.android.launcher";
    private static final boolean HW_SUPPORT_LAUNCHER_EXIT_ANIM = (SystemProperties.getBoolean("ro.config.disable_launcher_exit_anim", false) ^ 1);
    private static final String HW_SYSTEM_SERVER_START = "com.huawei.systemserver.START";
    private static final boolean IS_CHINA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    public static final boolean IS_SUPPORT_CLONE_APP = SystemProperties.getBoolean("ro.config.hw_support_clone_app", false);
    static final int KILL_APPLICATION_MSG = 22;
    static final int MULTI_WINDOW_MODE_CHANGED_MSG = 23;
    private static final int NOTIFY_ACTIVITY_STATE = 71;
    static final int NOTIFY_CALL = 24;
    private static final String PACKAGE_HWOUC = "com.huawei.android.hwouc";
    private static final String PERMISSION_HWOUC_UPGRADE_REMIND = "com.huawei.android.hwouc.permission.UPGRADE_REMIND";
    private static final int PERSISTENT_MASK = 9;
    private static final int PRIMARY_SYSTEM_GID = 1000;
    private static final Set<String> PROCESS_NAME_IN_REPAIR_MODE = new HashSet<String>() {
        {
            add("com.huawei.ddtTest");
            add("com.huawei.morpheus");
            add("com.huawei.hwdetectrepair");
        }
    };
    private static final String REASON_SYS_REPLACE = "replace sys pkg";
    private static final int REPAIR_MODE_SYSTEM_UID = 12701000;
    private static final String RESOURCE_APPASSOC = "RESOURCE_APPASSOC";
    private static final String SETTING_GUEST_HAS_LOGGED_IN = "guest_has_logged_in";
    private static final int SHOW_APPFREEZE_DIALOG_MSG = 51;
    private static final int SHOW_GUEST_SWITCH_DIALOG_MSG = 50;
    private static final int SHOW_SWITCH_DIALOG_MSG = 49;
    static final int SHOW_UNINSTALL_LAUNCHER_MSG = 48;
    private static final String SPLIT_SCREEN_APP_NAME = "splitscreen.SplitScreenAppActivity";
    private static final String SYSTEMUI_NAME = "com.android.systemui";
    static final String TAG = "HwActivityManagerServiceEx";
    static final int TASK_SNAPSHOT = 25;
    private static final boolean enableRms = SystemProperties.getBoolean("ro.config.enable_rms", false);
    private static Set<String> mPIPWhitelists = new HashSet();
    private static Set<String> mTranslucentWhitelists = new HashSet();
    private static Set<String> mWhitelistActivities = new HashSet();
    static final boolean smcsLOGV = SystemProperties.getBoolean("ro.enable.st_debug", false);
    private boolean isLastMultiMode = false;
    final RemoteCallbackList<IHwActivityNotifier> mActivityNotifiers = new RemoteCallbackList();
    private final ArrayMap<Integer, ArrayMap<Integer, Long>> mAssocMap = new ArrayMap();
    private Handler mBootCheckHandler;
    final Context mContext;
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 80) {
                switch (i) {
                    case 48:
                        HwActivityManagerServiceEx.this.showUninstallLauncher();
                        return;
                    case 49:
                        HwActivityManagerServiceEx.this.mIAmsInner.getUserController().showUserSwitchDialog((Pair) msg.obj);
                        return;
                    case 50:
                        HwActivityManagerServiceEx.this.showGuestSwitchDialog(msg.arg1, (String) msg.obj);
                        return;
                    case 51:
                        HwActivityManagerServiceEx.this.showAppEyeAnrUi(msg);
                        return;
                    default:
                        switch (i) {
                            case 70:
                                HwActivityManagerServiceEx.this.reportFgToTopMsg(msg);
                                return;
                            case HwActivityManagerServiceEx.NOTIFY_ACTIVITY_STATE /*71*/:
                                HwActivityManagerServiceEx.this.handleNotifyActivityState(msg);
                                return;
                            default:
                                return;
                        }
                }
            }
            Slog.i(HwActivityManagerServiceEx.TAG, "send UPDATE REMIND broacast to HWOUC");
            Intent intent = new Intent(HwActivityManagerServiceEx.ACTION_HWOUC_SHOW_UPGRADE_REMIND);
            intent.setPackage(HwActivityManagerServiceEx.PACKAGE_HWOUC);
            HwActivityManagerServiceEx.this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM, HwActivityManagerServiceEx.PERMISSION_HWOUC_UPGRADE_REMIND);
        }
    };
    Handler mHwHandler = null;
    ServiceThread mHwHandlerThread = null;
    final TaskChangeNotificationController mHwTaskChangeNotificationController;
    IHwActivityManagerInner mIAmsInner = null;
    private String mLastLauncherName;
    private boolean mNeedRemindHwOUC = false;
    private ResetSessionDialog mNewSessionDialog;
    public HashMap<String, Long> mPCUsageStats = new HashMap();
    private Intent mQuickSlideIntent = null;
    private long mQuickSlideStartTime;
    private SettingsObserver mSettingsObserver;
    private RemoteCallbackList<IMWThirdpartyCallback> mThirdPartyCallbackList;

    private class ResetSessionDialog extends AlertDialog implements OnClickListener {
        private final int mUserId;

        public ResetSessionDialog(Context context, int userId) {
            super(context, context.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog", null, null));
            getWindow().setType(2014);
            getWindow().addFlags(655360);
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService("keyguard");
            if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
                getWindow().addPrivateFlags(Integer.MIN_VALUE);
            }
            setMessage(context.getString(33685841));
            setButton(-1, context.getString(33685843), this);
            setButton(-2, context.getString(33685842), this);
            setCanceledOnTouchOutside(false);
            this.mUserId = userId;
        }

        public void onClick(DialogInterface dialog, int which) {
            String str = HwActivityManagerServiceEx.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onClick which:");
            stringBuilder.append(which);
            Slog.i(str, stringBuilder.toString());
            if (which == -2) {
                HwActivityManagerServiceEx.this.wipeGuestSession(this.mUserId);
                dismiss();
            } else if (which == -1) {
                cancel();
                HwActivityManagerServiceEx.this.sendMessageToSwitchUser(this.mUserId, HwActivityManagerServiceEx.this.getGuestName());
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private static final String KEY_HW_UPGRADE_REMIND = "hw_upgrade_remind";
        private final Uri URI_HW_UPGRADE_REMIND = Secure.getUriFor(KEY_HW_UPGRADE_REMIND);

        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void init() {
            ContentResolver resolver = HwActivityManagerServiceEx.this.mContext.getContentResolver();
            boolean z = false;
            resolver.registerContentObserver(this.URI_HW_UPGRADE_REMIND, false, this, 0);
            HwActivityManagerServiceEx hwActivityManagerServiceEx = HwActivityManagerServiceEx.this;
            if (Secure.getIntForUser(resolver, KEY_HW_UPGRADE_REMIND, 0, 0) != 0) {
                z = true;
            }
            hwActivityManagerServiceEx.mNeedRemindHwOUC = z;
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (this.URI_HW_UPGRADE_REMIND.equals(uri)) {
                HwActivityManagerServiceEx hwActivityManagerServiceEx = HwActivityManagerServiceEx.this;
                boolean z = false;
                if (Secure.getIntForUser(HwActivityManagerServiceEx.this.mContext.getContentResolver(), KEY_HW_UPGRADE_REMIND, 0, 0) != 0) {
                    z = true;
                }
                hwActivityManagerServiceEx.mNeedRemindHwOUC = z;
                String str = HwActivityManagerServiceEx.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mNeedRemindHwOUC has changed to : ");
                stringBuilder.append(HwActivityManagerServiceEx.this.mNeedRemindHwOUC);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    static {
        mWhitelistActivities.add("com.vlocker.settings.DismissActivity");
        mTranslucentWhitelists.add("com.android.packageinstaller.permission.ui.GrantPermissionsActivity");
        mPIPWhitelists.add("com.android.systemui.pip.phone.PipMenuActivity");
    }

    public HwActivityManagerServiceEx(IHwActivityManagerInner iams, Context context) {
        this.mIAmsInner = iams;
        this.mContext = context;
        this.mHwHandlerThread = new ServiceThread(TAG, -2, false);
        this.mHwHandlerThread.start();
        this.mHwHandler = new Handler(this.mHwHandlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                int appId;
                String reason;
                switch (msg.what) {
                    case 22:
                        synchronized (HwActivityManagerServiceEx.this.mIAmsInner.getAMSForLock()) {
                            appId = msg.arg1;
                            int userId = msg.arg2;
                            Bundle bundle = (Bundle) msg.obj;
                            String pkg = bundle.getString("pkg");
                            reason = bundle.getString("reason");
                            String str = HwActivityManagerServiceEx.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("killApplication start for pkg: ");
                            stringBuilder.append(pkg);
                            stringBuilder.append(", userId: ");
                            stringBuilder.append(userId);
                            Slog.w(str, stringBuilder.toString());
                            HwActivityManagerServiceEx.this.mIAmsInner.forceStopPackageLockedInner(pkg, appId, false, false, true, false, false, userId, reason);
                            str = HwActivityManagerServiceEx.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("killApplication end for pkg: ");
                            stringBuilder.append(pkg);
                            stringBuilder.append(", userId: ");
                            stringBuilder.append(userId);
                            Slog.w(str, stringBuilder.toString());
                        }
                        return;
                    case 23:
                        boolean isInMultiWindowMode = ((Boolean) msg.obj).booleanValue();
                        synchronized (HwActivityManagerServiceEx.this.mThirdPartyCallbackList) {
                            try {
                                appId = HwActivityManagerServiceEx.this.mThirdPartyCallbackList.beginBroadcast();
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("onMultiWindowModeChanged begin : mThirdPartyCallbackList size : ");
                                stringBuilder2.append(appId);
                                Flog.i(100, stringBuilder2.toString());
                                while (appId > 0) {
                                    appId--;
                                    try {
                                        ((IMWThirdpartyCallback) HwActivityManagerServiceEx.this.mThirdPartyCallbackList.getBroadcastItem(appId)).onModeChanged(isInMultiWindowMode);
                                    } catch (Exception e) {
                                        Flog.e(100, "Error in sending the Callback");
                                    }
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("onMultiWindowModeChanged end : mThirdPartyCallbackList size : ");
                                stringBuilder2.append(appId);
                                Flog.i(100, stringBuilder2.toString());
                                HwActivityManagerServiceEx.this.mThirdPartyCallbackList.finishBroadcast();
                            } catch (IllegalStateException e2) {
                                Flog.e(100, "beginBroadcast() called while already in a broadcast");
                            }
                        }
                        return;
                    case 24:
                        synchronized (HwActivityManagerServiceEx.this.mActivityNotifiers) {
                            try {
                                Bundle bundle2 = msg.obj;
                                String userId2 = String.valueOf(bundle2.getInt("android.intent.extra.user_handle"));
                                String reason2 = bundle2.getString("android.intent.extra.REASON");
                                int i = HwActivityManagerServiceEx.this.mActivityNotifiers.beginBroadcast();
                                while (i > 0) {
                                    i--;
                                    IHwActivityNotifier notifier = (IHwActivityNotifier) HwActivityManagerServiceEx.this.mActivityNotifiers.getBroadcastItem(i);
                                    HashMap<String, String> cookie = (HashMap) HwActivityManagerServiceEx.this.mActivityNotifiers.getBroadcastCookie(i);
                                    if ((userId2.equals(cookie.get("android.intent.extra.user_handle")) || cookie.get("android.intent.extra.USER") != null) && reason2.equals(cookie.get("android.intent.extra.REASON"))) {
                                        try {
                                            long start = System.currentTimeMillis();
                                            ((IHwActivityNotifier) HwActivityManagerServiceEx.this.mActivityNotifiers.getBroadcastItem(i)).call(bundle2);
                                            reason = HwActivityManagerServiceEx.TAG;
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("HwActivityNotifier end call ");
                                            stringBuilder3.append(notifier);
                                            stringBuilder3.append(" for ");
                                            stringBuilder3.append(reason2);
                                            stringBuilder3.append(" under user ");
                                            stringBuilder3.append(userId2);
                                            stringBuilder3.append(" cost ");
                                            stringBuilder3.append(System.currentTimeMillis() - start);
                                            Slog.w(reason, stringBuilder3.toString());
                                        } catch (RemoteException e3) {
                                            StringBuilder stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("observer.call get RemoteException, remove notifier ");
                                            stringBuilder4.append(notifier);
                                            Flog.e(100, stringBuilder4.toString());
                                            HwActivityManagerServiceEx.this.mActivityNotifiers.unregister(notifier);
                                        }
                                    }
                                }
                                HwActivityManagerServiceEx.this.mActivityNotifiers.finishBroadcast();
                            } catch (Exception e4) {
                                Flog.e(100, "HwActivityNotifier call error");
                            }
                        }
                        return;
                    case 25:
                        ActivityRecord from = msg.obj;
                        if (from.getConfiguration().orientation == 2 && from.appToken != null) {
                            String str2 = HwActivityManagerServiceEx.TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("takeTaskSnapShot package ");
                            stringBuilder5.append(from.packageName);
                            Slog.v(str2, stringBuilder5.toString());
                            HwActivityManagerServiceEx.this.mIAmsInner.getAMSForLock().mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(from.appToken);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        HwBootCheck.bootSceneStart(100, AppHibernateCst.DELAY_ONE_MINS);
        this.mHwTaskChangeNotificationController = new TaskChangeNotificationController(this.mIAmsInner.getAMSForLock(), this.mIAmsInner.getStackSupervisor(), this.mHandler);
        this.mThirdPartyCallbackList = new RemoteCallbackList();
    }

    final void reportFgToTopMsg(Message msg) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(String.valueOf(msg.arg1));
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(String.valueOf(msg.arg2));
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(msg.obj);
        this.mIAmsInner.getDAMonitor().DAMonitorReport(this.mIAmsInner.getDAMonitor().getFirstDevSchedEventId(), stringBuffer.toString());
    }

    final void handleNotifyActivityState(Message msg) {
        if (msg != null) {
            String activityInfo = null;
            if (msg.obj instanceof String) {
                activityInfo = msg.obj;
            }
            if (activityInfo == null) {
                Slog.e(TAG, "msg.obj type error.");
                return;
            }
            if (!(this.mIAmsInner == null || this.mIAmsInner.getDAMonitor() == null)) {
                this.mIAmsInner.getDAMonitor().notifyActivityState(activityInfo);
            }
        }
    }

    private void showUninstallLauncher() {
        Context mUiContext = this.mIAmsInner.getUiContext();
        try {
            if (this.mContext.getPackageManager().getPackageInfo(this.mLastLauncherName, 0) != null) {
                AlertDialog d = new BaseErrorDialog(mUiContext);
                d.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL);
                d.setCancelable(false);
                d.setTitle(mUiContext.getString(33685930));
                d.setMessage(mUiContext.getString(33685932, new Object[]{this.mContext.getPackageManager().getApplicationLabel(pInfo.applicationInfo).toString()}));
                d.setButton(-1, mUiContext.getString(33685931), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        HwActivityManagerServiceEx.this.mContext.getPackageManager().deletePackage(HwActivityManagerServiceEx.this.mLastLauncherName, null, 0);
                    }
                });
                d.setButton(-2, mUiContext.getString(17039360), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                d.show();
            }
        } catch (NameNotFoundException e) {
        }
    }

    public int changeGidIfRepairMode(int uid, String processName) {
        if (uid == REPAIR_MODE_SYSTEM_UID && PROCESS_NAME_IN_REPAIR_MODE.contains(processName)) {
            return 1000;
        }
        return uid;
    }

    public void showUninstallLauncherDialog(String pkgName) {
        this.mLastLauncherName = pkgName;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(48));
    }

    private void showGuestSwitchDialog(int userId, String userName) {
        cancelDialog();
        ContentResolver cr = this.mContext.getContentResolver();
        int notFirstLogin = System.getIntForUser(cr, SETTING_GUEST_HAS_LOGGED_IN, 0, userId);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notFirstLogin:");
        stringBuilder.append(notFirstLogin);
        stringBuilder.append(", userid=");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        if (notFirstLogin != 0) {
            showGuestResetSessionDialog(userId);
            return;
        }
        System.putIntForUser(cr, SETTING_GUEST_HAS_LOGGED_IN, 1, userId);
        sendMessageToSwitchUser(userId, userName);
    }

    public void killApplication(String pkg, int appId, int userId, String reason) {
        if (appId < 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid appid specified for pkg : ");
            stringBuilder.append(pkg);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        int callerUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callerUid) == 1000) {
            Message msg = this.mHwHandler.obtainMessage(22);
            msg.arg1 = appId;
            msg.arg2 = userId;
            Bundle bundle = new Bundle();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("killApplication send message for pkg: ");
            stringBuilder2.append(pkg);
            stringBuilder2.append(", userId: ");
            stringBuilder2.append(userId);
            Slog.w(str2, stringBuilder2.toString());
            bundle.putString("pkg", pkg);
            bundle.putString("reason", reason);
            msg.obj = bundle;
            this.mHwHandler.sendMessage(msg);
            return;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(callerUid);
        stringBuilder3.append(" cannot kill pkg: ");
        stringBuilder3.append(pkg);
        throw new SecurityException(stringBuilder3.toString());
    }

    private final boolean cleanProviderLocked(ProcessRecord proc, ContentProviderRecord cpr, boolean always) {
        boolean inLaunching = this.mIAmsInner.getLaunchingProviders().contains(cpr);
        if (!inLaunching || always) {
            synchronized (cpr) {
                cpr.launchingApp = null;
                cpr.notifyAll();
            }
            this.mIAmsInner.getProviderMap().removeProviderByClass(cpr.name, UserHandle.getUserId(cpr.uid));
            String[] names = cpr.info.authority.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
            for (String removeProviderByName : names) {
                this.mIAmsInner.getProviderMap().removeProviderByName(removeProviderByName, UserHandle.getUserId(cpr.uid));
            }
        }
        for (int i = cpr.connections.size() - 1; i >= 0; i--) {
            ContentProviderConnection conn = (ContentProviderConnection) cpr.connections.get(i);
            if (!conn.waiting || !inLaunching || always) {
                ProcessRecord capp = conn.client;
                conn.dead = true;
                if (conn.stableCount > 0) {
                    if (!(capp.persistent || capp.thread == null || capp.pid == 0 || capp.pid == this.mIAmsInner.getAmsPid())) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("depends on provider ");
                        stringBuilder.append(cpr.name.flattenToShortString());
                        stringBuilder.append(" in dying proc ");
                        stringBuilder.append(proc != null ? proc.processName : "??");
                        capp.kill(stringBuilder.toString(), true);
                    }
                } else if (!(capp.thread == null || conn.provider.provider == null)) {
                    try {
                        capp.thread.unstableProviderDied(conn.provider.provider.asBinder());
                    } catch (RemoteException e) {
                        Slog.e(TAG, "cleanProviderLocked error because RemoteException!");
                    }
                    cpr.connections.remove(i);
                    if (conn.client.conProviders.remove(conn)) {
                        this.mIAmsInner.stopAssociationLockedInner(capp.uid, capp.processName, cpr.uid, cpr.name);
                    }
                }
            }
        }
        if (inLaunching && always) {
            this.mIAmsInner.getLaunchingProviders().remove(cpr);
        }
        return inLaunching;
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0059 A:{Catch:{ all -> 0x00f0, all -> 0x00f4 }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x005e A:{Catch:{ all -> 0x00f0, all -> 0x00f4 }} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x0085 A:{Catch:{ all -> 0x00f0, all -> 0x00f4 }} */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0091 A:{LOOP_END, LOOP:1: B:36:0x008e->B:38:0x0091, Catch:{ all -> 0x00f0, all -> 0x00f4 }} */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00a7 A:{LOOP_END, LOOP:2: B:40:0x00a5->B:41:0x00a7, Catch:{ all -> 0x00f0, all -> 0x00f4 }} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00c3 A:{SKIP, Catch:{ all -> 0x00f0, all -> 0x00f4 }} */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x00b9 A:{Catch:{ all -> 0x00f0, all -> 0x00f4 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean cleanPackageRes(List<String> packageList, Map<String, List<String>> alarmTags, int targetUid, boolean cleanAlarm, boolean isNative, boolean hasPerceptAlarm) {
        Throwable th;
        Map<String, List<String>> map = alarmTags;
        int i = targetUid;
        if (packageList == null) {
            return false;
        }
        boolean didSomething = false;
        int userId = UserHandle.getUserId(targetUid);
        synchronized (this.mIAmsInner.getAMSForLock()) {
            int userId2;
            try {
                Iterator it = packageList.iterator();
                while (it.hasNext()) {
                    String packageName;
                    ArrayList<ContentProviderRecord> providers;
                    Iterator it2;
                    String packageName2;
                    ArrayList<ContentProviderRecord> providers2;
                    int i2;
                    String packageName3 = (String) it.next();
                    if (!isNative) {
                        try {
                            if (!canCleanTaskRecord(packageName3)) {
                                packageName = packageName3;
                                userId2 = userId;
                                if (this.mIAmsInner.bringDownDisabledPackageServicesLocked(packageName, null, userId, false, true, 1)) {
                                    didSomething = true;
                                }
                                userId = packageName;
                                if (userId == 0) {
                                    this.mIAmsInner.getStickyBroadcasts().remove(userId2);
                                }
                                providers = new ArrayList();
                                it2 = it;
                                packageName2 = userId;
                                if (this.mIAmsInner.getProviderMap().collectPackageProvidersLocked(userId, null, true, false, userId2, providers)) {
                                    didSomething = true;
                                }
                                providers2 = providers;
                                for (i2 = providers2.size() - 1; i2 >= 0; i2--) {
                                    cleanProviderLocked(null, (ContentProviderRecord) providers2.get(i2), true);
                                }
                                for (i2 = this.mIAmsInner.getBroadcastQueues().length - 1; i2 >= 0; i2--) {
                                    didSomething |= this.mIAmsInner.getBroadcastQueues()[i2].cleanupDisabledPackageReceiversLocked(packageName2, null, userId2, true);
                                }
                                if (map != null) {
                                    this.mIAmsInner.getAlarmService().removePackageAlarm(packageName2, null, i);
                                } else if (cleanAlarm && this.mIAmsInner.getAlarmService() != null) {
                                    List<String> tags = (List) map.get(packageName2);
                                    if (tags != null) {
                                        this.mIAmsInner.getAlarmService().removePackageAlarm(packageName2, tags, i);
                                    }
                                }
                                if (!isNative || !hasPerceptAlarm) {
                                    this.mIAmsInner.finishForceStopPackageLockedInner(packageName2, i);
                                }
                                userId = userId2;
                                it = it2;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            userId2 = userId;
                        }
                    }
                    packageName = packageName3;
                    if (this.mIAmsInner.finishDisabledPackageActivitiesLocked(packageName3, null, true, false, userId)) {
                        didSomething = true;
                    }
                    userId2 = userId;
                    if (this.mIAmsInner.bringDownDisabledPackageServicesLocked(packageName, null, userId, false, true, 1)) {
                    }
                    userId = packageName;
                    if (userId == 0) {
                    }
                    providers = new ArrayList();
                    it2 = it;
                    packageName2 = userId;
                    if (this.mIAmsInner.getProviderMap().collectPackageProvidersLocked(userId, null, true, false, userId2, providers)) {
                    }
                    providers2 = providers;
                    while (i2 >= 0) {
                    }
                    while (i2 >= 0) {
                    }
                    if (map != null) {
                    }
                    if (!isNative) {
                    }
                    this.mIAmsInner.finishForceStopPackageLockedInner(packageName2, i);
                    userId = userId2;
                    it = it2;
                }
                return didSomething;
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:40:0x00af, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean canCleanTaskRecord(String packageName) {
        if (packageName == null) {
            return true;
        }
        synchronized (this.mIAmsInner.getAMSForLock()) {
            ArrayList<TaskRecord> recentTasks = this.mIAmsInner.getRecentRawTasks();
            if (recentTasks == null) {
                return true;
            }
            int size = recentTasks.size();
            int maxFoundNum = this.mIAmsInner.getDAMonitor().getActivityImportCount();
            int foundNum = 0;
            for (int i = 0; i < size && foundNum < maxFoundNum; i++) {
                TaskRecord tr = (TaskRecord) recentTasks.get(i);
                if (!(tr == null || tr.mActivities == null)) {
                    if (!(tr.mActivities.size() <= 0 || tr.getBaseIntent() == null || tr.getBaseIntent().getComponent() == null)) {
                        if (packageName.equals(tr.getBaseIntent().getComponent().getPackageName())) {
                            return false;
                        } else if (!this.mIAmsInner.getDAMonitor().getRecentTask().equals(tr.getBaseIntent().getComponent().flattenToShortString())) {
                            if ((tr.getBaseIntent().getFlags() & 8388608) != 0) {
                            }
                        }
                    }
                    foundNum++;
                }
            }
            if ((this.mIAmsInner.getStackSupervisor() instanceof HwActivityStackSupervisor) && ((HwActivityStackSupervisor) this.mIAmsInner.getStackSupervisor()).isInVisibleStack(packageName)) {
                return false;
            }
        }
    }

    public Boolean switchUser(int userId) {
        boolean isStorageLow = false;
        try {
            isStorageLow = AppGlobals.getPackageManager().isStorageLow();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("check low storage error because e: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
        if (isStorageLow) {
            UiThread.getHandler().post(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(HwActivityManagerServiceEx.this.mContext, HwActivityManagerServiceEx.this.mContext.getResources().getString(17040400), 1);
                    toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_BIND_CHECK_FAIL_NOTIFY;
                    LayoutParams windowParams = toast.getWindowParams();
                    windowParams.privateFlags |= 16;
                    toast.show();
                }
            });
            return Boolean.FALSE;
        }
        UserInfo targetUser = this.mIAmsInner.getUserController().getUserInfo(userId);
        if (targetUser == null || !targetUser.isGuest()) {
            return null;
        }
        this.mHandler.removeMessages(50);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(50, userId, 0, targetUser.name));
        return Boolean.TRUE;
    }

    private void sendMessageToSwitchUser(int userId, String userName) {
        UserController userctl = this.mIAmsInner.getUserController();
        Pair<UserInfo, UserInfo> userNames = new Pair(userctl.getUserInfo(userctl.getCurrentUserId()), userctl.getUserInfo(userId));
        this.mHandler.removeMessages(49);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(49, userNames));
    }

    private void showGuestResetSessionDialog(int guestId) {
        this.mNewSessionDialog = new ResetSessionDialog(this.mContext, guestId);
        this.mNewSessionDialog.show();
        LayoutParams lp = this.mNewSessionDialog.getWindow().getAttributes();
        lp.width = -1;
        this.mNewSessionDialog.getWindow().setAttributes(lp);
    }

    private void cancelDialog() {
        if (this.mNewSessionDialog != null && this.mNewSessionDialog.isShowing()) {
            this.mNewSessionDialog.cancel();
            this.mNewSessionDialog = null;
        }
    }

    private String getUserName(int userId) {
        if (this.mIAmsInner.getUserController() == null) {
            return null;
        }
        UserInfo info = this.mIAmsInner.getUserController().getUserInfo(userId);
        if (info == null) {
            return null;
        }
        return info.name;
    }

    private String getGuestName() {
        return this.mContext.getString(33685844);
    }

    private void wipeGuestSession(int userId) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager.markGuestForDeletion(userId)) {
            UserInfo newGuest = userManager.createGuest(this.mContext, getGuestName());
            if (newGuest == null) {
                Slog.e(TAG, "Could not create new guest, switching back to owner");
                sendMessageToSwitchUser(0, getUserName(0));
                userManager.removeUser(userId);
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Create new guest, switching to = ");
            stringBuilder.append(newGuest.id);
            Slog.d(str, stringBuilder.toString());
            sendMessageToSwitchUser(newGuest.id, newGuest.name);
            System.putIntForUser(this.mContext.getContentResolver(), SETTING_GUEST_HAS_LOGGED_IN, 1, newGuest.id);
            userManager.removeUser(userId);
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Couldn't mark the guest for deletion for user ");
        stringBuilder2.append(userId);
        Slog.w(str2, stringBuilder2.toString());
    }

    public TaskChangeNotificationController getHwTaskChangeController() {
        return this.mHwTaskChangeNotificationController;
    }

    public void onAppGroupChanged(int pid, int uid, String pkgName, int oldSchedGroup, int newSchedGroup) {
        if (newSchedGroup == 3) {
            Message msg = this.mHandler.obtainMessage(70);
            msg.arg1 = pid;
            msg.arg2 = uid;
            msg.obj = pkgName;
            this.mHandler.sendMessage(msg);
        }
    }

    public boolean registerThirdPartyCallBack(IMWThirdpartyCallback aCallBackHandler) {
        boolean lRegistered = false;
        if (aCallBackHandler != null) {
            synchronized (this.mThirdPartyCallbackList) {
                lRegistered = this.mThirdPartyCallbackList.register(aCallBackHandler);
            }
        }
        return lRegistered;
    }

    public boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback aCallBackHandler) {
        boolean lUnregistered = false;
        if (aCallBackHandler != null) {
            synchronized (this.mThirdPartyCallbackList) {
                lUnregistered = this.mThirdPartyCallbackList.unregister(aCallBackHandler);
            }
        }
        return lUnregistered;
    }

    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (this.isLastMultiMode != isInMultiWindowMode) {
            this.isLastMultiMode = isInMultiWindowMode;
            Message msg = this.mHwHandler.obtainMessage(23);
            msg.obj = Boolean.valueOf(isInMultiWindowMode);
            this.mHwHandler.sendMessage(msg);
        }
    }

    public void notifyActivityState(ActivityRecord r, String state) {
        Message msg = this.mHandler.obtainMessage(NOTIFY_ACTIVITY_STATE);
        String activityInfo = parseActivityStateInfo(r, state);
        if (activityInfo == null) {
            Slog.e(TAG, "parse activity info error.");
            return;
        }
        msg.obj = activityInfo;
        this.mHandler.sendMessage(msg);
        if (this.mNeedRemindHwOUC && r.userId == 0 && r.isActivityTypeHome() && state.equals(ActivityState.RESUMED.toString())) {
            this.mHandler.removeMessages(80);
            this.mHandler.sendEmptyMessage(80);
        }
    }

    private String parseActivityStateInfo(ActivityRecord r, String state) {
        if (r == null || state == null) {
            Slog.e(TAG, "invalid input param, error.");
            return null;
        } else if (r.packageName == null || r.shortComponentName == null || r.app == null || r.appInfo == null || r.appInfo.uid <= 1000) {
            Slog.e(TAG, "invalid ActivityRecord, error.");
            return null;
        } else {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(r.packageName);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.shortComponentName);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.appInfo.uid);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.app.pid);
            stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(state);
            return stringBuffer.toString();
        }
    }

    public boolean isApplyPersistAppPatch(String ssp, int uid, int userId, boolean bWillRestart, boolean evenPersistent, String reason, String action) {
        String str = reason;
        String str2 = action;
        boolean bResult = false;
        boolean bHandle = "android.intent.action.PACKAGE_REMOVED".equals(str2);
        boolean bDisableService = bWillRestart && evenPersistent && str != null && str.endsWith(REASON_SYS_REPLACE);
        if (!bDisableService && !bHandle) {
            return false;
        }
        String str3 = ssp;
        ApplicationInfo info = this.mIAmsInner.getPackageManagerInternal().getApplicationInfo(str3, 1152, Process.myUid(), userId);
        if (info == null) {
            return false;
        }
        ProcessRecord apprecord = this.mIAmsInner.getProcessRecord(info.processName, uid, true);
        if ((bHandle && apprecord != null && !apprecord.persistent) || apprecord == null) {
            return false;
        }
        if (!(apprecord.info == null || apprecord.info.sourceDir == null || ((apprecord.info.hwFlags & 536870912) == 0 && (info.metaData == null || !info.metaData.getBoolean(APKPATCH_META_DATA, false))))) {
            if (!apprecord.info.sourceDir.equals(info.sourceDir) && bHandle) {
                this.mIAmsInner.forceStopPackageLockedInner(str3, uid, true, false, true, true, false, userId, REASON_SYS_REPLACE);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str2);
                stringBuilder.append(TAG);
                stringBuilder.append("-----kill & restart---");
                Slog.i("PatchService", stringBuilder.toString());
                this.mIAmsInner.startPersistApp(info, null, false, null);
            }
            bResult = true;
        }
        return bResult;
    }

    public boolean isSpecialVideoForPCMode(ActivityRecord r) {
        if (HwPCUtils.isPcCastModeInServer()) {
            HwPCMultiWindowManager multiWindowMgr = HwPCMultiWindowManager.getInstance(this.mIAmsInner.getAMSForLock());
            if (multiWindowMgr != null) {
                int stackId = r.getStack().getStackId();
                if (r.packageName != null && HwPCUtils.isPcDynamicStack(stackId) && multiWindowMgr.isPortraitApp(r.getTask())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canSleepForPCMode() {
        if (!(this.mIAmsInner.getStackSupervisor() instanceof HwActivityStackSupervisor)) {
            return false;
        }
        HwActivityStackSupervisor HwSupervisor = (HwActivityStackSupervisor) this.mIAmsInner.getStackSupervisor();
        int i = HwSupervisor.getChildCount() - 1;
        while (i >= 0) {
            ActivityDisplay display = HwSupervisor.getActivityDisplay(i);
            if (display == null || (!display.mStacks.isEmpty() && display.mAllSleepTokens.isEmpty())) {
                i--;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("the display ");
                stringBuilder.append(display.mDisplayId);
                stringBuilder.append(" has (");
                stringBuilder.append(display.mAllSleepTokens.size());
                stringBuilder.append(") SleepTokens when goToSleep,mStacks ");
                stringBuilder.append(display.mStacks);
                HwPCUtils.log(str, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    public boolean canUpdateSleepForPCMode() {
        if (!(this.mIAmsInner.getStackSupervisor() instanceof HwActivityStackSupervisor)) {
            return false;
        }
        HwActivityStackSupervisor HwSupervisor = (HwActivityStackSupervisor) this.mIAmsInner.getStackSupervisor();
        int i = HwSupervisor.getChildCount() - 1;
        while (i >= 0) {
            ActivityDisplay display = HwSupervisor.getActivityDisplay(i);
            if (display == null || display.mAllSleepTokens.isEmpty()) {
                i--;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("the display ");
                stringBuilder.append(display.mDisplayId);
                stringBuilder.append(" has (");
                stringBuilder.append(display.mAllSleepTokens.size());
                stringBuilder.append(") SleepTokens when updateSleep");
                HwPCUtils.log(str, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    public String[] updateEntryPointArgsForPCMode(ProcessRecord app, String[] entryPointArgs) {
        if ((HwPCUtils.isPcCastModeInServer() || HwVRUtils.isVRMode()) && app.entryPointArgs != null) {
            return concat(entryPointArgs, app.entryPointArgs);
        }
        return entryPointArgs;
    }

    private static String[] concat(String[] first, String[] second) {
        String[] result = new String[(first.length + second.length)];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public boolean isFreeFormVisible() {
        return false;
    }

    public void call(Bundle extras) {
        Message msg = this.mHwHandler.obtainMessage(24);
        msg.obj = extras;
        this.mHwHandler.sendMessage(msg);
    }

    public void registerHwActivityNotifier(IHwActivityNotifier notifier, String reason) {
        if (notifier != null && ACTIVITY_NOTIFIER_TYPES.contains(reason) && this.mContext.checkCallingOrSelfPermission("com.huawei.permission.ACTIVITY_NOTIFIER_PERMISSION") == 0) {
            Map<String, String> cookie = new HashMap();
            cookie.put("android.intent.extra.REASON", reason);
            cookie.put("android.intent.extra.user_handle", String.valueOf(Binder.getCallingUserHandle().getIdentifier()));
            String[] callingPkgNames = this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
            if (callingPkgNames != null) {
                for (String pkgName : callingPkgNames) {
                    if ("com.android.systemui".equals(pkgName)) {
                        cookie.put("android.intent.extra.USER", String.valueOf(Binder.getCallingUserHandle().getIdentifier()));
                        break;
                    }
                }
            }
            synchronized (this.mActivityNotifiers) {
                this.mActivityNotifiers.register(notifier, cookie);
            }
        }
    }

    public void unregisterHwActivityNotifier(IHwActivityNotifier notifier) {
        if (notifier != null && this.mContext.checkCallingOrSelfPermission("com.huawei.permission.ACTIVITY_NOTIFIER_PERMISSION") == 0) {
            synchronized (this.mActivityNotifiers) {
                this.mActivityNotifiers.unregister(notifier);
            }
        }
    }

    public void notifyAppSwitch(ActivityRecord from, ActivityRecord to) {
        String packageName;
        if (HW_SUPPORT_LAUNCHER_EXIT_ANIM && from != null && to != null && from != to && to.isActivityTypeHome() && "com.huawei.android.launcher".equals(to.packageName)) {
            packageName = from.packageName;
            TaskRecord tr = from.getTask();
            if ("com.huawei.systemmanager".equals(packageName) && ACTION_CONFIRM_APPLOCK_CREDENTIAL.equals(from.intent.getAction())) {
                packageName = from.intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
            } else if (!(tr == null || tr.getRootActivity() == null)) {
                packageName = tr.getRootActivity().packageName;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setResumedActivityUncheckLocked start call, from: ");
            stringBuilder.append(from);
            stringBuilder.append(", to: ");
            stringBuilder.append(to);
            Slog.w(str, stringBuilder.toString());
            Bundle bundle = new Bundle();
            bundle.putString("package", packageName);
            bundle.putString("topPackage", from.packageName);
            bundle.putBoolean("isTransluent", from.isTransluent());
            bundle.putInt("userId", from.userId);
            bundle.putString("android.intent.extra.REASON", "returnToHome");
            bundle.putInt("android.intent.extra.user_handle", to.userId);
            Message msg = this.mHwHandler.obtainMessage(24);
            msg.obj = bundle;
            this.mHwHandler.sendMessageAtFrontOfQueue(msg);
            Message msg2 = this.mHwHandler.obtainMessage(25);
            msg2.obj = from;
            this.mHwHandler.sendMessage(msg2);
        }
        if (from != null && to != null && !from.packageName.equals(to.packageName)) {
            packageName = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("appSwitch from: ");
            stringBuilder2.append(from.packageName);
            stringBuilder2.append(" to: ");
            stringBuilder2.append(to.packageName);
            Slog.w(packageName, stringBuilder2.toString());
            Bundle bundle2 = new Bundle();
            bundle2.putString("fromPackage", from.packageName);
            bundle2.putInt("fromUid", from.getUid());
            bundle2.putString("toPackage", to.packageName);
            bundle2.putInt("toUid", to.getUid());
            bundle2.putString("android.intent.extra.REASON", "appSwitch");
            bundle2.putInt("android.intent.extra.user_handle", this.mIAmsInner.getUserController().getCurrentUserIdLU());
            Message msg3 = this.mHwHandler.obtainMessage(24);
            msg3.obj = bundle2;
            this.mHwHandler.sendMessage(msg3);
        }
    }

    public void dispatchActivityLifeState(ActivityRecord r, String state) {
        if (r != null) {
            TaskRecord task = r.task;
            boolean z = true;
            if (mTranslucentWhitelists.contains(r.info.name) && task != null) {
                for (int i = task.mActivities.size() - 1; i >= 0; i--) {
                    ActivityRecord activityRecord = (ActivityRecord) task.mActivities.get(i);
                    if (activityRecord != null && !activityRecord.finishing && activityRecord != r) {
                        r = activityRecord;
                        break;
                    }
                }
            }
            Bundle bundle = new Bundle();
            if (r.app != null) {
                bundle.putInt("uid", r.app.uid);
                bundle.putInt("pid", r.app.pid);
            }
            ComponentName comp = r.info.getComponentName();
            Rect bounds = r.getBounds();
            if (r.maxAspectRatio <= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || r.maxAspectRatio >= ActivityRecord.mDeviceMaxRatio || mWhitelistActivities.contains(comp.getClassName())) {
                bounds = new Rect();
            }
            bundle.putBoolean("isTop", r == this.mIAmsInner.getLastResumedActivityRecord());
            bundle.putString("state", state);
            bundle.putParcelable("comp", comp);
            bundle.putParcelable("bounds", bounds);
            String str = "isFloating";
            if (!(mPIPWhitelists.contains(comp.getClassName()) || r.isFloating())) {
                z = false;
            }
            bundle.putBoolean(str, z);
            bundle.putFloat("maxRatio", r.maxAspectRatio);
            bundle.putBoolean("isHomeActivity", r.isActivityTypeHome());
            bundle.putString("android.intent.extra.REASON", "activityLifeState");
            bundle.putInt("android.intent.extra.user_handle", this.mIAmsInner.getUserController().getCurrentUserIdLU());
            Message msg = this.mHwHandler.obtainMessage(24);
            msg.obj = bundle;
            this.mHwHandler.sendMessage(msg);
        }
    }

    private Intent slideGetDefaultIntent() {
        Intent intent = new Intent();
        if (SystemProperties.get("ro.config.hw_optb", "0").equals("156")) {
            intent.setPackage("com.huawei.vassistant");
            intent.setAction("com.huawei.action.VOICE_ASSISTANT");
            intent.putExtra("from_slide_open", true);
        } else {
            intent.setPackage("no_set");
        }
        return intent;
    }

    private Intent slideGetIntentFromSetting() {
        String keyStr;
        if (this.mIAmsInner.isSleeping()) {
            keyStr = "quick_slide_app_db_secure";
        } else {
            keyStr = "quick_slide_app_db";
        }
        String intentStr = Secure.getStringForUser(this.mContext.getContentResolver(), keyStr, this.mIAmsInner.getUserController().getCurrentUserId());
        if (intentStr == null) {
            return null;
        }
        try {
            return Intent.parseUri(intentStr, 0);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startActivity get intent err : ");
            stringBuilder.append(intentStr);
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public void slideOpenStartActivity() {
        Intent intent = slideGetIntentFromSetting();
        if (intent == null) {
            intent = slideGetDefaultIntent();
        }
        this.mQuickSlideIntent = intent;
        this.mQuickSlideStartTime = SystemClock.uptimeMillis();
        ActivityRecord lastResumedActivity = this.mIAmsInner.getLastResumedActivityRecord();
        String lastResumedPkg = lastResumedActivity != null ? lastResumedActivity.packageName : null;
        Context context = this.mContext;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{curPkgName:");
        stringBuilder.append(lastResumedPkg);
        stringBuilder.append(",startPkgName:");
        stringBuilder.append(intent.getPackage());
        stringBuilder.append("}");
        Flog.bdReport(context, PPPOEStateMachine.PPPOE_EVENT_CODE, stringBuilder.toString());
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("slideOpenStartActivity lastResumedPkg:");
        stringBuilder2.append(lastResumedPkg);
        stringBuilder2.append(", startPkgName:");
        stringBuilder2.append(intent.getPackage());
        Slog.i(str, stringBuilder2.toString());
        if (intent == null || intent.getPackage() == null || intent.getPackage().equals("no_set") || (lastResumedActivity != null && lastResumedActivity.visible && lastResumedActivity.packageName.equals(intent.getPackage()))) {
            Slog.i(TAG, "no_set or has been started, need not start activity!");
        } else {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    public void slideCloseMoveActivityToBack() {
        String str;
        ActivityRecord lastResumedActivity = this.mIAmsInner.getLastResumedActivityRecord();
        if (!(lastResumedActivity == null || lastResumedActivity.task == null || lastResumedActivity.task.getStack() == null || this.mQuickSlideIntent == null || !lastResumedActivity.packageName.equals(this.mQuickSlideIntent.getPackage()))) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("moveActivityToBack task:");
            stringBuilder.append(lastResumedActivity.task);
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mIAmsInner.getAMSForLock()) {
                lastResumedActivity.task.getStack().moveTaskToBackLocked(lastResumedActivity.task.taskId);
            }
        }
        str = this.mQuickSlideIntent != null ? this.mQuickSlideIntent.getPackage() : null;
        long curTime = SystemClock.uptimeMillis();
        long durTime = (this.mQuickSlideStartTime == 0 || curTime < this.mQuickSlideStartTime) ? 0 : curTime - this.mQuickSlideStartTime;
        Context context = this.mContext;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("{durTime:");
        stringBuilder2.append(durTime);
        stringBuilder2.append(",pkgName:");
        stringBuilder2.append(str);
        stringBuilder2.append("}");
        Flog.bdReport(context, 653, stringBuilder2.toString());
        String str2 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("slideCloseMoveActivityToBack durTime:");
        stringBuilder3.append(durTime);
        stringBuilder3.append(", pkgName:");
        stringBuilder3.append(str);
        Slog.i(str2, stringBuilder3.toString());
        this.mQuickSlideIntent = null;
        this.mQuickSlideStartTime = 0;
    }

    private void registerHallCallback() {
        if (!new CoverManager().registerHallCallback("android", 1, new Stub() {
            public void onStateChange(HallState hallState) {
                long ident = Binder.clearCallingIdentity();
                try {
                    if (hallState.state == 2) {
                        HwActivityManagerServiceEx.this.slideOpenStartActivity();
                    } else if (hallState.state == 0) {
                        HwActivityManagerServiceEx.this.slideCloseMoveActivityToBack();
                    }
                    Binder.restoreCallingIdentity(ident);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        })) {
            Slog.i(TAG, "registerHallCallback err!");
        }
    }

    public void registerBroadcastReceiver() {
        if ((SystemProperties.getInt("ro.config.hw_hall_prop", 0) & 1) != 0) {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (intent != null && HwActivityManagerServiceEx.HW_SYSTEM_SERVER_START.equals(intent.getAction())) {
                        Slog.i(HwActivityManagerServiceEx.TAG, "registerBroadcastReceiver");
                        HwActivityManagerServiceEx.this.registerHallCallback();
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(HW_SYSTEM_SERVER_START);
            this.mContext.registerReceiverAsUser(receiver, UserHandle.ALL, filter, null, null);
        }
    }

    public void systemReady(Runnable goingCallback, TimingsTraceLog traceLog) {
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.init();
    }

    public boolean handleANRFilterFIFO(int uid, int cmd) {
        switch (cmd) {
            case 0:
                return ANRFilter.getInstance().addUid(uid);
            case 1:
                return ANRFilter.getInstance().removeUid(uid);
            case 2:
                return ANRFilter.getInstance().checkUid(uid);
            default:
                return false;
        }
    }

    private void handleShowAppEyeAnrUi(int pid, int uid, String processName, String packageName) {
        Message msg = this.mHandler.obtainMessage(51);
        msg.arg1 = pid;
        msg.arg2 = uid;
        Bundle bundle = new Bundle();
        if (packageName != null) {
            bundle.putString("pkg", packageName);
        }
        if (processName != null) {
            bundle.putString("proc", processName);
        }
        msg.obj = bundle;
        msg.sendToTarget();
    }

    private void showAppEyeAnrUi(Message msg) {
        int pid = msg.arg1;
        int uid = msg.arg2;
        String processName = msg.obj.getString("proc");
        if (processName != null) {
            ProcessRecord app;
            synchronized (this.mIAmsInner.getAMSForLock()) {
                app = this.mIAmsInner.getProcessRecord(processName, uid, true);
            }
            if (app != null) {
                appEyeAppNotResponding(app);
            } else {
                Slog.e(TAG, "showAppEyeAnrUi null!");
            }
        }
    }

    /* JADX WARNING: Missing block: B:28:0x008c, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void appEyeAppNotResponding(ProcessRecord app) {
        boolean showBackground = Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        synchronized (this.mIAmsInner.getAMSForLock()) {
            if (!showBackground) {
                if (!(app.isInterestingToUserLocked() || app.pid == this.mIAmsInner.getAmsPid())) {
                    app.kill("BG ANR", true);
                    zrHungSendEvent("recoverresult", 0, 0, app.info.packageName, null, "BG Kill");
                    return;
                }
            }
            AppErrors mAppErrors = this.mIAmsInner.getAppErrors();
            if (mAppErrors == null) {
                return;
            }
            mAppErrors.makeAppeyeAppNotRespondingLocked(app, null, "AppFreeze!", null);
            Message msg = Message.obtain();
            msg.what = 2;
            msg.obj = new Data(app, null, false);
            app.anrType = 2;
            if (zrHungSendEvent("showanrdialog", app.pid, app.uid, app.info.packageName, null, "appeye")) {
                Handler mUiHandler = this.mIAmsInner.getUiHandler();
                if (mUiHandler == null) {
                    return;
                }
                mUiHandler.sendMessage(msg);
            }
        }
    }

    public boolean zrHungSendEvent(String eventType, int pid, int uid, String packageName, String processName, String event) {
        ZrHungData data = new ZrHungData();
        if (eventType == null) {
            Slog.e(TAG, "eventType is null");
            return true;
        } else if (eventType.equals("handleshowdialog")) {
            handleShowAppEyeAnrUi(pid, uid, processName, packageName);
            return true;
        } else {
            if (pid > 0) {
                data.putInt("pid", pid);
            }
            if (uid > 0) {
                data.putInt("uid", uid);
            }
            if (packageName != null) {
                data.putString("packageName", packageName);
            }
            if (processName != null) {
                data.putString("processName", processName);
            }
            if (event != null) {
                data.putString("result", event);
            }
            data.putString("eventtype", eventType);
            if (HwServiceFactory.getZRHungService().sendEvent(data)) {
                return true;
            }
            Slog.e(TAG, "zrHungSendEvent failed!");
            return false;
        }
    }

    public boolean isTaskVisible(int id) {
        int callerUid = Binder.getCallingUid();
        if (callerUid == 1000) {
            boolean z = false;
            if (this.mIAmsInner == null) {
                return false;
            }
            ActivityStackSupervisor mStackSupervisor = this.mIAmsInner.getStackSupervisor();
            if (mStackSupervisor == null) {
                return false;
            }
            TaskRecord tr = mStackSupervisor.anyTaskForIdLocked(id);
            if (!(tr == null || tr.getTopActivity() == null || !tr.getTopActivity().visible)) {
                z = true;
            }
            return z;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Process with uid=");
        stringBuilder.append(callerUid);
        stringBuilder.append(" cannot call function isTaskVisible.");
        throw new SecurityException(stringBuilder.toString());
    }

    public boolean shouldPreventStartProcess(String processName, int userId) {
        if (userId != 0) {
            String str;
            for (String procName : this.mContext.getResources().getStringArray(33816583)) {
                if (procName.equals(processName)) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(processName);
                    stringBuilder.append(" is not allowed for sub user ");
                    stringBuilder.append(userId);
                    Slog.i(str, stringBuilder.toString());
                    return true;
                }
            }
            UserInfo ui = null;
            long ident = Binder.clearCallingIdentity();
            try {
                StringBuilder stringBuilder2;
                ui = this.mIAmsInner.getUserController().getUserInfo(userId);
                if (ui != null && ui.isManagedProfile()) {
                    for (String procName2 : this.mContext.getResources().getStringArray(33816584)) {
                        if (procName2.equals(processName)) {
                            str = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(processName);
                            stringBuilder2.append(" is not allowed for afw user ");
                            stringBuilder2.append(userId);
                            Slog.i(str, stringBuilder2.toString());
                            return true;
                        }
                    }
                }
                if (ui != null && ui.isClonedProfile()) {
                    for (String procName22 : this.mContext.getResources().getStringArray(33816585)) {
                        if (procName22.equals(processName)) {
                            str = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(processName);
                            stringBuilder2.append(" is not allowed for clone user ");
                            stringBuilder2.append(userId);
                            Slog.i(str, stringBuilder2.toString());
                            return true;
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return false;
    }

    public int getEffectiveUid(int hwHbsUid, int defaultUid) {
        int effectiveUid = defaultUid;
        if (19959 > hwHbsUid || hwHbsUid > 19999) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid HBS app uid ");
            stringBuilder.append(hwHbsUid);
            Slog.e(str, stringBuilder.toString());
            return effectiveUid;
        }
        int hbsCoreUid;
        try {
            hbsCoreUid = Os.lstat("/data/data/com.huawei.hbs.framework").st_uid;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("HBS uid found: ");
            stringBuilder2.append(hbsCoreUid);
            Slog.i(str2, stringBuilder2.toString());
        } catch (ErrnoException e) {
            Slog.i(TAG, "HBS uid not found");
            hbsCoreUid = -1;
        }
        String str3;
        StringBuilder stringBuilder3;
        if (defaultUid == hbsCoreUid) {
            str3 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Change HBS app uid ");
            stringBuilder3.append(defaultUid);
            stringBuilder3.append(" -> ");
            stringBuilder3.append(hwHbsUid);
            Slog.i(str3, stringBuilder3.toString());
            return hwHbsUid;
        }
        str3 = TAG;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Invalid HBS core uid ");
        stringBuilder3.append(defaultUid);
        Slog.i(str3, stringBuilder3.toString());
        return effectiveUid;
    }

    public boolean isExemptedAuthority(Uri grantUri) {
        return EXEMPTED_AUTHORITIES.contains(grantUri.getAuthority());
    }

    public void setThreadSchedPolicy(int oldSchedGroup, ProcessRecord app) {
        if (app != null) {
            if (app.curSchedGroup == 3) {
                if (oldSchedGroup != 3) {
                    this.mIAmsInner.getDAMonitor().setVipThread(app.pid, app.renderThreadTid, true);
                }
            } else if (oldSchedGroup == 3) {
                this.mIAmsInner.getDAMonitor().setVipThread(app.pid, app.renderThreadTid, false);
            }
        }
    }

    public PointerEventListener getPointerEventListener() {
        return new PointerEventListener() {
            public void onPointerEvent(MotionEvent motionEvent) {
                HwActivityManagerServiceEx.this.mIAmsInner.getDAMonitor().onPointerEvent(motionEvent.getAction());
            }
        };
    }

    public void noteActivityStart(String packageName, String processName, String activityName, int pid, int uid, boolean started) {
        if (this.mIAmsInner.getSystemReady()) {
            this.mIAmsInner.getDAMonitor().noteActivityStart(packageName, processName, activityName, pid, uid, started);
        }
    }

    public List<String> getPidWithUiFromUid(int uid) {
        List<String> pids = new ArrayList();
        synchronized (this.mIAmsInner.getPidsSelfLocked()) {
            int pidsSize = this.mIAmsInner.getPidsSelfLocked().size();
            for (int i = 0; i < pidsSize; i++) {
                ProcessRecord p = (ProcessRecord) this.mIAmsInner.getPidsSelfLocked().valueAt(i);
                if (p.uid == uid && p.pid != 0 && p.hasShownUi) {
                    pids.add(String.valueOf(p.pid));
                }
            }
        }
        return pids;
    }

    public void removePackageStopFlag(String packageName, int uid, String resolvedType, int resultCode, String requiredPermission, Bundle options, int userId) {
        String str;
        StringBuilder stringBuilder;
        if (packageName != null && options != null && options.getBoolean("fromSystemUI")) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("packageName: ");
            stringBuilder2.append(packageName);
            stringBuilder2.append(", uid: ");
            stringBuilder2.append(uid);
            stringBuilder2.append(", resolvedType: ");
            stringBuilder2.append(resolvedType);
            stringBuilder2.append(", resultCode: ");
            stringBuilder2.append(resultCode);
            stringBuilder2.append(", requiredPermission: ");
            stringBuilder2.append(requiredPermission);
            stringBuilder2.append(", userId: ");
            stringBuilder2.append(userId);
            Slog.d(str2, stringBuilder2.toString());
            try {
                AppGlobals.getPackageManager().setPackageStoppedState(packageName, false, UserHandle.getUserId(uid));
            } catch (RemoteException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed trying to unstop ");
                stringBuilder.append(packageName);
                stringBuilder.append(" due to  RemoteException");
                Slog.w(str, stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed trying to unstop package ");
                stringBuilder.append(packageName);
                stringBuilder.append(" due to IllegalArgumentException");
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:51:0x0104, code:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int preloadApplication(String packageName, int userId) {
        String str;
        StringBuilder stringBuilder;
        if (Binder.getCallingUid() != 1000) {
            return -1;
        }
        synchronized (this.mIAmsInner.getAMSForLock()) {
            if (this.mIAmsInner.getConstants().CUR_MAX_CACHED_PROCESSES <= 8) {
                return -1;
            }
            IPackageManager pm = AppGlobals.getPackageManager();
            if (pm == null) {
                return -1;
            }
            ApplicationInfo appInfo = null;
            try {
                appInfo = pm.getApplicationInfo(packageName, 1152, userId);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed trying to get application info: ");
                stringBuilder2.append(packageName);
                Slog.w(str2, stringBuilder2.toString());
            }
            String str3;
            if (appInfo == null) {
                str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("preloadApplication, get application info failed, packageName = ");
                stringBuilder3.append(packageName);
                Slog.d(str3, stringBuilder3.toString());
                return -1;
            }
            ProcessRecord app = this.mIAmsInner.getProcessRecordLockedEx(appInfo.processName, appInfo.uid, true);
            if (app != null && app.thread != null) {
                str3 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("process has started, packageName:");
                stringBuilder4.append(packageName);
                stringBuilder4.append(", processName:");
                stringBuilder4.append(appInfo.processName);
                Slog.d(str3, stringBuilder4.toString());
                return -1;
            } else if ((appInfo.flags & 9) == 9) {
                Slog.d(TAG, "preloadApplication, application is persistent, return");
                return -1;
            } else {
                if (app == null) {
                    app = this.mIAmsInner.newProcessRecordLockedEx(appInfo, null, false, 0);
                    this.mIAmsInner.updateLruProcessLockedEx(app, false, null);
                    this.mIAmsInner.updateOomAdjLockedEx();
                }
                try {
                    pm.setPackageStoppedState(packageName, false, UserHandle.getUserId(app.uid));
                } catch (RemoteException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RemoteException, Failed trying to unstop package: ");
                    stringBuilder.append(packageName);
                    Slog.w(str, stringBuilder.toString());
                } catch (IllegalArgumentException e3) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IllegalArgumentException, Failed trying to unstop package ");
                    stringBuilder.append(packageName);
                    Slog.w(str, stringBuilder.toString());
                }
                if (app.thread == null) {
                    this.mIAmsInner.startProcessLockedEx(app, "start application", app.processName, null);
                }
            }
        }
    }

    private void reportAppForceStopMsg(int userId, String packageName, int callingPid) {
        String SETTINGS_PKGNAME = "com.android.settings";
        if (checkIfPackageNameMatchesPid(callingPid, "com.android.settings")) {
            this.mIAmsInner.getDAMonitor().reportAppDiedMsg(userId, packageName, "settings");
        }
    }

    private boolean checkIfPackageNameMatchesPid(int mPid, String targetPackageName) {
        if (targetPackageName.equals(getPackageNameForPid(mPid))) {
            return true;
        }
        return false;
    }

    public void reportAppDiedMsg(int userId, String processName, int callerPid, String reason) {
        if (reason == null || !reason.contains("forceStop")) {
            this.mIAmsInner.getDAMonitor().reportAppDiedMsg(userId, processName, reason);
            return;
        }
        if (reason.contains("SystemManager")) {
            this.mIAmsInner.getDAMonitor().reportAppDiedMsg(userId, processName, "SystemManager");
        } else if (reason.contains("PowerGenie")) {
            this.mIAmsInner.getDAMonitor().reportAppDiedMsg(userId, processName, "PowerGenie");
        } else {
            reportAppForceStopMsg(userId, processName, callerPid);
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0021, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String getPackageNameForPid(int pid) {
        synchronized (this.mIAmsInner.getPidsSelfLocked()) {
            ProcessRecord proc = (ProcessRecord) this.mIAmsInner.getPidsSelfLocked().get(pid);
            if (proc != null) {
                String str = proc.info != null ? proc.info.packageName : "android";
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ProcessRecord for pid ");
                stringBuilder.append(pid);
                stringBuilder.append(" does not exist");
                Flog.i(100, stringBuilder.toString());
                return null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:22:0x009f, code:
            r4 = r10.mIAmsInner.getAMSForLock();
     */
    /* JADX WARNING: Missing block: B:23:0x00a5, code:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:25:?, code:
            r10.mIAmsInner.cleanupAppInLaunchingProvidersLockedEx(r0, true);
     */
    /* JADX WARNING: Missing block: B:26:0x00ac, code:
            if (r13 == false) goto L_0x00b2;
     */
    /* JADX WARNING: Missing block: B:27:0x00ae, code:
            r0.killedByAm = true;
            r0.killed = true;
     */
    /* JADX WARNING: Missing block: B:28:0x00b2, code:
            r0.unlinkDeathRecipient();
            r3 = new java.lang.StringBuilder();
            r3.append(r14);
            r3.append("(");
            r3.append(r0.adjType);
            r3.append(")");
            r3 = r3.toString();
            r10.mIAmsInner.removeProcessLockedEx(r0, false, r12, r3);
     */
    /* JADX WARNING: Missing block: B:29:0x00d5, code:
            if (r13 == false) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:30:0x00d7, code:
            r10.mIAmsInner.getDAMonitor().killProcessGroupForQuickKill(r0.info.uid, r11.mPid);
            r7 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("Killing ");
            r8.append(r1);
            r8.append(" (adj ");
            r8.append(r0.curAdj);
            r8.append("): ");
            r8.append(r3);
            android.util.Slog.i(r7, r8.toString());
            android.util.EventLog.writeEvent(30023, new java.lang.Object[]{java.lang.Integer.valueOf(r2), java.lang.Integer.valueOf(r11.mPid), r1, java.lang.Integer.valueOf(r0.curAdj), r3});
     */
    /* JADX WARNING: Missing block: B:31:0x0133, code:
            r10.mIAmsInner.cleanupBroadcastLockedEx(r0);
            r10.mIAmsInner.cleanupAlarmLockedEx(r0);
     */
    /* JADX WARNING: Missing block: B:32:0x013d, code:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:33:0x013e, code:
            r10.mIAmsInner.getDAMonitor().reportAppDiedMsg(r2, r1, r14);
     */
    /* JADX WARNING: Missing block: B:34:0x0147, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean killProcessRecordFromIAwareInternal(ProcessInfo procInfo, boolean restartservice, boolean isAsynchronous, String reason, boolean isNative) {
        synchronized (this.mIAmsInner.getPidsSelfLocked()) {
            String str;
            StringBuilder stringBuilder;
            if (procInfo.mPid == this.mIAmsInner.getMyPid() || procInfo.mPid < 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("killProcessRecordFromIAware it is failed to get process record ,mUid :");
                stringBuilder.append(procInfo.mUid);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
            ProcessRecord proc = (ProcessRecord) this.mIAmsInner.getPidsSelfLocked().get(procInfo.mPid);
            if (proc == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("killProcessRecordFromIAware this process has been killed or died before  :");
                stringBuilder.append(procInfo.mProcessName);
                Slog.e(str, stringBuilder.toString());
                return false;
            } else if (isNative || proc.curAdj >= 200 || AwareAppMngSort.EXEC_SERVICES.equals(proc.adjType)) {
                String killedProcessName = proc.processName;
                int killedAppUserId = proc.userId;
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("killProcessRecordFromIAware process cleaner kill process: adj changed, new adj:");
                stringBuilder.append(proc.curAdj);
                stringBuilder.append(", old adj:");
                stringBuilder.append(procInfo.mCurAdj);
                stringBuilder.append(", pid:");
                stringBuilder.append(procInfo.mPid);
                stringBuilder.append(", uid:");
                stringBuilder.append(procInfo.mUid);
                stringBuilder.append(", ");
                stringBuilder.append(procInfo.mProcessName);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
        }
    }

    public boolean killProcessRecordFromMTM(ProcessInfo procInfo, boolean restartservice, String reason) {
        String str;
        if (procInfo.mPid == this.mIAmsInner.getMyPid() || procInfo.mPid < 0) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("killProcessRecordFromMTM it is failed to get process record ,mUid :");
            stringBuilder.append(procInfo.mUid);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
        synchronized (this.mIAmsInner.getPidsSelfLocked()) {
            int adj = procInfo.mCurAdj;
            ProcessRecord proc = (ProcessRecord) this.mIAmsInner.getPidsSelfLocked().get(procInfo.mPid);
            if (proc == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("killProcessRecordFromMTM this process has been killed or died before  :");
                stringBuilder2.append(procInfo.mProcessName);
                Slog.e(str2, stringBuilder2.toString());
                return false;
            }
            synchronized (this.mIAmsInner.getAMSForLock()) {
                str = new StringBuilder();
                str.append("iAwareK[");
                str.append(reason);
                str.append("](adj:");
                str.append(adj);
                str.append(",type:");
                str.append(proc.adjType);
                str.append(")");
                this.mIAmsInner.removeProcessLockedEx(proc, false, restartservice, str.toString());
            }
            return true;
        }
    }

    public void removePackageAlarm(String pkg, List<String> tags, int targetUid) {
        synchronized (this.mIAmsInner.getAMSForLock()) {
            if (this.mIAmsInner.getAlarmService() != null) {
                this.mIAmsInner.getAlarmService().removePackageAlarm(pkg, tags, targetUid);
            } else {
                Slog.e(TAG, "removeByTag alarm instance is null");
            }
        }
    }

    /* JADX WARNING: Missing block: B:39:?, code:
            r9.mLru = r8.mIAmsInner.getLruProcesses().lastIndexOf(r0);
     */
    /* JADX WARNING: Missing block: B:41:0x00c6, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getProcessRecordFromMTM(ProcessInfo procInfo) {
        int i = 0;
        if (procInfo == null) {
            Slog.e(TAG, "getProcessRecordFromMTM procInfo is null");
            return false;
        }
        synchronized (this.mIAmsInner.getAMSForLock()) {
            synchronized (this.mIAmsInner.getPidsSelfLocked()) {
                String str;
                StringBuilder stringBuilder;
                if (procInfo.mPid == this.mIAmsInner.getMyPid() || procInfo.mPid < 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getProcessRecordFromMTM it is failed to get process record ,mPid :");
                    stringBuilder.append(procInfo.mPid);
                    Slog.e(str, stringBuilder.toString());
                    return false;
                }
                ProcessRecord proc = (ProcessRecord) this.mIAmsInner.getPidsSelfLocked().get(procInfo.mPid);
                if (proc == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getProcessRecordFromMTM process info is null ,mUid :");
                    stringBuilder.append(procInfo.mPid);
                    Slog.e(str, stringBuilder.toString());
                    return false;
                }
                if (procInfo.mType == 0) {
                    procInfo.mType = getAppType(procInfo.mPid, proc.info);
                }
                procInfo.mProcessName = proc.processName;
                procInfo.mCurSchedGroup = proc.curSchedGroup;
                procInfo.mCurAdj = proc.curAdj;
                procInfo.mAdjType = proc.adjType;
                procInfo.mAppUid = proc.info.uid;
                procInfo.mSetProcState = proc.setProcState;
                procInfo.mForegroundActivities = proc.foregroundActivities;
                procInfo.mForegroundServices = proc.foregroundServices;
                procInfo.mForceToForeground = proc.forcingToImportant != null;
                if (procInfo.mPackageName.size() == 0) {
                    int list_size = proc.pkgList.size();
                    while (i < list_size) {
                        String packagename = (String) proc.pkgList.keyAt(i);
                        if (!procInfo.mPackageName.contains(packagename)) {
                            procInfo.mPackageName.add(packagename);
                        }
                        i++;
                    }
                }
            }
        }
    }

    public int canAppBoost(ActivityInfo aInfo, boolean isScreenOn) {
        if (isScreenOn || aInfo == null) {
            return 1;
        }
        int type = getAppType(-1, aInfo.applicationInfo);
        if (type == 4) {
            return 0;
        }
        if (1 != type) {
            String packageName = aInfo.packageName;
            if (!(packageName == null || packageName.startsWith("com.huawei"))) {
                return 0;
            }
        }
        return 1;
    }

    private int getAppType(int pid, ApplicationInfo info) {
        if (info == null) {
            Slog.e(TAG, "getAppType app info is null");
            return 0;
        }
        int flags = info.flags;
        try {
            int hwFlags = ((Integer) Class.forName("android.content.pm.ApplicationInfo").getField("hwFlags").get(info)).intValue();
            if (!((flags & 1) == 0 || (hwFlags & 100663296) == 0)) {
                return 3;
            }
        } catch (ClassNotFoundException e) {
            Slog.e(TAG, "getAppType exception: ClassNotFoundException");
        } catch (NoSuchFieldException e2) {
            Slog.e(TAG, "getAppType exception: NoSuchFieldException");
        } catch (IllegalArgumentException e3) {
            Slog.e(TAG, "getAppType exception: IllegalArgumentException");
        } catch (IllegalAccessException e4) {
            Slog.e(TAG, "getAppType exception: IllegalAccessException");
        }
        if (pid == Process.myPid()) {
            return 1;
        }
        if ((flags & 1) != 0) {
            return 2;
        }
        return 4;
    }

    public void setAndRestoreMaxAdjIfNeed(List<String> adjCustPkg) {
        if (adjCustPkg != null) {
            ArraySet<String> adjCustPkgSet = new ArraySet();
            adjCustPkgSet.addAll(adjCustPkg);
            synchronized (this.mIAmsInner.getAMSForLock()) {
                synchronized (this.mIAmsInner.getPidsSelfLocked()) {
                    int list_size = this.mIAmsInner.getPidsSelfLocked().size();
                    for (int i = 0; i < list_size; i++) {
                        ProcessRecord p = (ProcessRecord) this.mIAmsInner.getPidsSelfLocked().valueAt(i);
                        if (p != null) {
                            boolean pkgContains = false;
                            for (String pkg : p.pkgList.keySet()) {
                                if (adjCustPkgSet.contains(pkg)) {
                                    pkgContains = true;
                                    break;
                                }
                            }
                            if (pkgContains) {
                                if (p.maxAdj > AwareDefaultConfigList.HW_PERCEPTIBLE_APP_ADJ) {
                                    p.maxAdj = AwareDefaultConfigList.HW_PERCEPTIBLE_APP_ADJ;
                                }
                            } else if (p.maxAdj == AwareDefaultConfigList.HW_PERCEPTIBLE_APP_ADJ) {
                                p.maxAdj = 1001;
                            }
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0074, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reportServiceRelationIAware(int relationType, ServiceRecord r, ProcessRecord caller) {
        if (r != null && caller != null && r.name != null && r.appInfo != null && caller.uid != r.appInfo.uid && this.mIAmsInner.getDAMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            Bundle bundleArgs = new Bundle();
            int callerUid = caller.uid;
            int callerPid = caller.pid;
            String callerProcessName = caller.processName;
            int targetUid = r.appInfo.uid;
            String targetProcessName = r.processName;
            String compName = r.name.flattenToShortString();
            bundleArgs.putInt(ASSOC_CALL_PID, callerPid);
            bundleArgs.putInt(ASSOC_CALL_UID, callerUid);
            bundleArgs.putString(ASSOC_CALL_PROCNAME, callerProcessName);
            bundleArgs.putInt(ASSOC_TGT_UID, targetUid);
            bundleArgs.putString(ASSOC_TGT_PROCNAME, targetProcessName);
            bundleArgs.putString(ASSOC_TGT_COMPNAME, compName);
            bundleArgs.putInt(ASSOC_RELATION_TYPE, relationType);
            this.mIAmsInner.getDAMonitor().reportData(RESOURCE_APPASSOC, System.currentTimeMillis(), bundleArgs);
        }
    }

    /* JADX WARNING: Missing block: B:33:0x00eb, code:
            return;
     */
    /* JADX WARNING: Missing block: B:34:0x00ec, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reportServiceRelationIAware(int relationType, ContentProviderRecord r, ProcessRecord caller) {
        if (caller != null && r != null && r.info != null && r.name != null && caller.uid != r.uid && this.mIAmsInner.getDAMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            if (r.proc != null) {
                synchronized (this.mAssocMap) {
                    ArrayMap<Integer, Long> pids = (ArrayMap) this.mAssocMap.get(Integer.valueOf(caller.pid));
                    if (pids != null) {
                        Long elaseTime = (Long) pids.get(Integer.valueOf(r.proc.pid));
                        if (elaseTime == null) {
                            pids.put(Integer.valueOf(r.proc.pid), Long.valueOf(SystemClock.elapsedRealtime()));
                        } else if (SystemClock.elapsedRealtime() - elaseTime.longValue() < AppHibernateCst.DELAY_ONE_MINS) {
                            return;
                        }
                    }
                    pids = new ArrayMap();
                    pids.put(Integer.valueOf(r.proc.pid), Long.valueOf(SystemClock.elapsedRealtime()));
                    this.mAssocMap.put(Integer.valueOf(caller.pid), pids);
                }
            }
            Bundle bundleArgs = new Bundle();
            int callerUid = caller.uid;
            int callerPid = caller.pid;
            String callerProcessName = caller.processName;
            int targetUid = r.uid;
            String targetProcessName = r.info.processName;
            String compName = r.name.flattenToShortString();
            bundleArgs.putInt(ASSOC_CALL_PID, callerPid);
            bundleArgs.putInt(ASSOC_CALL_UID, callerUid);
            bundleArgs.putString(ASSOC_CALL_PROCNAME, callerProcessName);
            bundleArgs.putInt(ASSOC_TGT_UID, targetUid);
            bundleArgs.putString(ASSOC_TGT_PROCNAME, targetProcessName);
            bundleArgs.putString(ASSOC_TGT_COMPNAME, compName);
            bundleArgs.putInt(ASSOC_RELATION_TYPE, relationType);
            this.mIAmsInner.getDAMonitor().reportData(RESOURCE_APPASSOC, System.currentTimeMillis(), bundleArgs);
        }
    }

    public void reportProcessDied(int pid) {
        synchronized (this.mAssocMap) {
            this.mAssocMap.remove(Integer.valueOf(pid));
            Iterator<Entry<Integer, ArrayMap<Integer, Long>>> it = this.mAssocMap.entrySet().iterator();
            while (it.hasNext()) {
                ArrayMap<Integer, Long> pids = (ArrayMap) ((Entry) it.next()).getValue();
                pids.remove(Integer.valueOf(pid));
                if (pids.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    public void reportAssocDisable() {
        synchronized (this.mAssocMap) {
            this.mAssocMap.clear();
        }
    }

    public void reportPreviousInfo(int relationType, ProcessRecord prevProc) {
        if (prevProc != null && this.mIAmsInner.getDAMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            int prevPid = prevProc.pid;
            int prevUid = prevProc.uid;
            Bundle bundleArgs = new Bundle();
            bundleArgs.putInt("pid", prevPid);
            bundleArgs.putInt(ASSOC_TGT_UID, prevUid);
            bundleArgs.putInt(ASSOC_RELATION_TYPE, relationType);
            this.mIAmsInner.getDAMonitor().reportData(RESOURCE_APPASSOC, System.currentTimeMillis(), bundleArgs);
        }
    }

    public void reportHomeProcess(ProcessRecord homeProc) {
        if (this.mIAmsInner.getDAMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            int pid = 0;
            int uid = 0;
            ArrayList<String> pkgs = new ArrayList();
            if (homeProc != null) {
                try {
                    pid = homeProc.pid;
                    uid = homeProc.uid;
                    for (String pkg : homeProc.pkgList.keySet()) {
                        if (!pkgs.contains(pkg)) {
                            pkgs.add(pkg);
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    Slog.i(TAG, "reportHomeProcess error happened.");
                }
            }
            Bundle bundleArgs = new Bundle();
            bundleArgs.putInt("pid", pid);
            bundleArgs.putInt(ASSOC_TGT_UID, uid);
            bundleArgs.putStringArrayList("pkgname", pkgs);
            bundleArgs.putInt(ASSOC_RELATION_TYPE, 11);
            this.mIAmsInner.getDAMonitor().reportData(RESOURCE_APPASSOC, System.currentTimeMillis(), bundleArgs);
        }
    }

    /* JADX WARNING: Missing block: B:11:0x003a, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setHbsMiniAppUid(ApplicationInfo info, Intent intent) {
        if (!(info == null || intent == null || intent.getComponent() == null || !"com.huawei.hbs.framework".equals(intent.getComponent().getPackageName()))) {
            try {
                Slog.i(TAG, "This is HBS mini application, setHbsMiniAppUid");
                info.hwHbsUid = new Intent(intent).getIntExtra("AppUID", -1);
            } catch (Throwable th) {
                Slog.w(TAG, "HBS set app uid for mini app throw errors");
            }
        }
    }

    public boolean checkActivityStartForPCMode(ActivityStarter as, ActivityOptions options, ActivityRecord startActivity, ActivityStack targetStack) {
        if (as == null || startActivity == null || targetStack == null) {
            HwPCUtils.log(TAG, "null params, return true for checkActivityStartForPCMode");
            return true;
        } else if (!HwPCUtils.isPcCastModeInServer() || as.hasStartedOnOtherDisplay(startActivity, targetStack.mDisplayId) == -1) {
            return true;
        } else {
            ActivityOptions.abort(options);
            ActivityStack sourceStack = startActivity.resultTo != null ? startActivity.resultTo.getStack() : null;
            if (sourceStack != null) {
                sourceStack.sendActivityResultLocked(-1, startActivity.resultTo, startActivity.resultWho, startActivity.requestCode, 0, null);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cancel activity start, act:");
            stringBuilder.append(startActivity);
            stringBuilder.append(" targetStack:");
            stringBuilder.append(targetStack);
            HwPCUtils.log(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean isProcessExistPidsSelfLocked(String processName, int uid) {
        boolean isExisted = false;
        SparseArray<ProcessRecord> pidsSelfLocked = this.mIAmsInner.getPidsSelfLocked();
        synchronized (pidsSelfLocked) {
            int pidsSize = pidsSelfLocked.size();
            for (int i = 0; i < pidsSize; i++) {
                ProcessRecord p = (ProcessRecord) pidsSelfLocked.valueAt(i);
                if (p != null && p.uid == uid && p.processName != null && p.processName.equals(processName)) {
                    isExisted = true;
                    break;
                }
            }
        }
        return isExisted;
    }

    public boolean isPcMutiResumeStack(ActivityStack stack) {
        if (HwPCUtils.isPcCastModeInServer() && stack != null && HwPCUtils.isPcDynamicStack(stack.getStackId())) {
            ActivityRecord resumedActivity = stack.getResumedActivity();
            List<String> mutiResumeAppList = HwPCUtils.getMutiResumeAppList();
            if (!(resumedActivity == null || !resumedActivity.isResizeable() || mutiResumeAppList == null || resumedActivity.packageName == null)) {
                return mutiResumeAppList.contains(resumedActivity.packageName);
            }
        }
        return false;
    }

    public boolean isTaskSupportResize(int taskId, boolean isFullscreen, boolean isMaximized) {
        if (!HwPCUtils.isPcCastModeInServer()) {
            return false;
        }
        HwPCMultiWindowManager multiWindowMgr = HwPCMultiWindowManager.getInstance(this.mIAmsInner.getAMSForLock());
        if (multiWindowMgr == null || !(this.mIAmsInner.getStackSupervisor() instanceof HwActivityStackSupervisor)) {
            return false;
        }
        HwActivityStackSupervisor hwSupervisor = (HwActivityStackSupervisor) this.mIAmsInner.getStackSupervisor();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIAmsInner.getAMSForLock()) {
                TaskRecord task = hwSupervisor.anyTaskForIdLocked(taskId);
                if (task == null) {
                    Binder.restoreCallingIdentity(origId);
                    return false;
                }
                boolean isSupportResize = multiWindowMgr.isSupportResize(task, isFullscreen, isMaximized);
                Binder.restoreCallingIdentity(origId);
                return isSupportResize;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int getTopTaskIdInDisplay(int displayId, String pkgName, boolean invisibleAlso) {
        if (displayId != 0 && HwPCUtils.isPcCastModeInServer() && !HwPCUtils.isValidExtDisplayId(displayId)) {
            Slog.e(TAG, "is not a valid pc display id");
            return -1;
        } else if (!(this.mIAmsInner.getStackSupervisor() instanceof HwActivityStackSupervisor)) {
            return -1;
        } else {
            int topTaskIdInDisplay;
            HwActivityStackSupervisor hwSupervisor = (HwActivityStackSupervisor) this.mIAmsInner.getStackSupervisor();
            synchronized (this.mIAmsInner.getAMSForLock()) {
                topTaskIdInDisplay = hwSupervisor.getTopTaskIdInDisplay(displayId, pkgName, invisibleAlso);
            }
            return topTaskIdInDisplay;
        }
    }

    public Rect getPCTopTaskBounds(int displayId) {
        if (HwPCUtils.isPcCastModeInServer() && !HwPCUtils.isValidExtDisplayId(displayId)) {
            Slog.e(TAG, "is not a valid pc display id");
            return null;
        } else if (!(this.mIAmsInner.getStackSupervisor() instanceof HwActivityStackSupervisor)) {
            return null;
        } else {
            Rect pCTopTaskBounds;
            HwActivityStackSupervisor hwSupervisor = (HwActivityStackSupervisor) this.mIAmsInner.getStackSupervisor();
            synchronized (this.mIAmsInner.getAMSForLock()) {
                pCTopTaskBounds = hwSupervisor.getPCTopTaskBounds(displayId);
            }
            return pCTopTaskBounds;
        }
    }

    public void updateUsageStatsForPCMode(ActivityRecord component, boolean visible, UsageStatsManagerInternal usageStatsService) {
        if (component != null && HwPCUtils.isPcDynamicStack(component.getStackId()) && usageStatsService != null) {
            if (!visible) {
                usageStatsService.reportEvent(component.realActivity, component.userId, 2, HwPCUtils.getPCDisplayID());
                this.mPCUsageStats.remove(component.realActivity.toShortString());
            } else if (!this.mPCUsageStats.containsKey(component.realActivity.toShortString())) {
                usageStatsService.reportEvent(component.realActivity, component.userId, 1, HwPCUtils.getPCDisplayID());
                this.mPCUsageStats.put(component.realActivity.toShortString(), Long.valueOf(System.currentTimeMillis()));
            }
        }
    }

    public void dismissSplitScreenModeWithFinish(ActivityRecord r) {
        if (r.getWindowingMode() == 4 && r.getActivityType() == 1) {
            ActivityStackSupervisor mStackSupervisor = this.mIAmsInner.getStackSupervisor();
            if (mStackSupervisor == null) {
                Slog.w(TAG, "dismissSplitScreenModeWithFinish:mStackSupervisor not found.");
            } else if (r.info.name.contains(SPLIT_SCREEN_APP_NAME)) {
                dismissSplitScreenToPrimaryStack(mStackSupervisor);
            } else {
                ActivityStack nextTargetAs = mStackSupervisor.getNextStackInSplitSecondary(r.getStack());
                if (nextTargetAs != null) {
                    ActivityRecord nextTargetAR = nextTargetAs.topRunningActivityLocked();
                    if (nextTargetAR != null && nextTargetAR.info.name.contains(SPLIT_SCREEN_APP_NAME)) {
                        dismissSplitScreenToPrimaryStack(mStackSupervisor);
                    }
                }
            }
        }
    }

    private void dismissSplitScreenToPrimaryStack(ActivityStackSupervisor mStackSupervisor) {
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityStack stack = mStackSupervisor.getDefaultDisplay().getSplitScreenPrimaryStack();
            if (stack == null) {
                Slog.w(TAG, "dismissSplitScreenToPrimaryStack: primary split-screen stack not found.");
                return;
            }
            this.mIAmsInner.getAMSForLock().mWindowManager.mShouldResetTime = true;
            this.mIAmsInner.getAMSForLock().mWindowManager.startFreezingScreen(0, 0);
            stack.moveToFront("dismissSplitScreenToPrimaryStack");
            stack.setWindowingMode(1);
            this.mIAmsInner.getAMSForLock().mWindowManager.stopFreezingScreen();
            Binder.restoreCallingIdentity(ident);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int[] changeGidsIfNeeded(ProcessRecord app, int[] gids) {
        ProcessRecord processRecord = app;
        int[] gids2 = gids;
        int userId = UserHandle.getUserId(processRecord.uid);
        boolean z = true;
        int i = 0;
        if (!"com.huawei.securitymgr".equals(processRecord.info.packageName) || userId == 0) {
            if (IS_SUPPORT_CLONE_APP) {
                long ident = Binder.clearCallingIdentity();
                try {
                    List<UserInfo> profiles = ((UserManager) this.mContext.getSystemService("user")).getProfiles(userId);
                    if (profiles.size() > 1) {
                        Iterator<UserInfo> iterator = profiles.iterator();
                        while (iterator.hasNext()) {
                            if (((UserInfo) iterator.next()).isManagedProfile()) {
                                iterator.remove();
                            }
                        }
                        if (profiles.size() > 1) {
                            for (UserInfo ui : profiles) {
                                if (ui.id != userId) {
                                    int[] newGids = new int[(gids2.length + 2)];
                                    System.arraycopy(gids2, i, newGids, i, gids2.length);
                                    newGids[gids2.length] = UserHandle.getUserGid(ui.id);
                                    newGids[gids2.length + 1] = UserHandle.getUid(ui.id, 1023);
                                    gids2 = newGids;
                                    i = 0;
                                }
                            }
                        }
                    }
                    Binder.restoreCallingIdentity(ident);
                    if (!(CLONEPROFILE_PERMISSION.contains(processRecord.info.packageName) || HwPackageManagerService.isSupportCloneAppInCust(processRecord.info.packageName) || processRecord.info.processName.contains("android.process.media"))) {
                        z = false;
                    }
                    if (z && userId == 0) {
                        int[] newGids2 = new int[(gids2.length + (2 * 20))];
                        int i2 = 0;
                        System.arraycopy(gids2, 0, newGids2, 0, gids2.length);
                        while (true) {
                            int i3 = i2;
                            if (i3 >= 20) {
                                break;
                            }
                            newGids2[gids2.length + i3] = UserHandle.getUid(128 + i3, 1023);
                            newGids2[(gids2.length + 20) + i3] = UserHandle.getUserGid(128 + i3);
                            i2 = i3 + 1;
                        }
                        gids2 = newGids2;
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            return gids2;
        }
        int[] newGids3 = new int[(gids2.length + 1)];
        System.arraycopy(gids2, 0, newGids3, 0, gids2.length);
        newGids3[gids2.length] = UserHandle.getUserGid(0);
        return newGids3;
    }

    public boolean isAllowToStartActivity(Context context, String callerPkg, ActivityInfo aInfo, boolean isKeyguard, ActivityInfo topActivity) {
        if (!IS_CHINA || callerPkg == null || aInfo == null) {
            return true;
        }
        int activityMode;
        int i = 0;
        if (isKeyguard) {
            activityMode = 8;
        } else {
            boolean isNotTop = (topActivity == null || aInfo.packageName == null || aInfo.packageName.equals(topActivity.packageName)) ? false : true;
            if (!callerPkg.equals(aInfo.packageName) || !isNotTop) {
                return true;
            }
            activityMode = 4;
        }
        if (aInfo.applicationInfo != null) {
            i = aInfo.applicationInfo.uid;
        }
        boolean hsmCheck = HwAddViewHelper.getInstance(context).addViewPermissionCheck(aInfo.packageName, activityMode, i);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isAllowToStartActivity:");
        stringBuilder.append(hsmCheck);
        stringBuilder.append(", activityMode:");
        stringBuilder.append(activityMode);
        stringBuilder.append(", callerPkg:");
        stringBuilder.append(callerPkg);
        stringBuilder.append(", destInfo:");
        stringBuilder.append(aInfo);
        stringBuilder.append(", topActivity:");
        stringBuilder.append(topActivity);
        Slog.i(str, stringBuilder.toString());
        return hsmCheck;
    }
}
