package com.android.server.am;

import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.IHwActivityNotifier;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
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
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.TimingsTraceLog;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.server.ServiceThread;
import com.android.server.UiThread;
import com.android.server.am.ActivityStack.ActivityState;
import com.android.server.gesture.GestureNavConst;
import com.android.server.location.HwGpsPowerTracker;
import com.android.server.os.HwBootCheck;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HwActivityManagerServiceEx implements IHwActivityManagerServiceEx {
    private static final String ACTION_CONFIRM_APPLOCK_CREDENTIAL = "huawei.intent.action.APPLOCK_FRAMEWORK_MANAGER";
    private static final String ACTION_CONFIRM_APPLOCK_PACKAGENAME = "com.huawei.systemmanager";
    private static final String ACTION_HWOUC_SHOW_UPGRADE_REMIND = "com.huawei.android.hwouc.action.SHOW_UPGRADE_REMIND";
    private static final Set<String> ACTIVITY_NOTIFIER_TYPES = new HashSet<String>() {
        {
            add("returnToHome");
            add("activityLifeState");
        }
    };
    private static final String APKPATCH_META_DATA = "android.huawei.MARKETED_SYSTEM_APP";
    static final boolean DEBUG_HWTRIM = smcsLOGV;
    static final boolean DEBUG_HWTRIM_PERFORM = smcsLOGV;
    private static final int FG_TO_TOP_APP_MSG = 70;
    private static final int HWOUC_UPDATE_REMIND_MSG = 80;
    private static final String HW_LAUNCHER_PKGNAME = "com.huawei.android.launcher";
    private static final boolean HW_SUPPORT_LAUNCHER_EXIT_ANIM = (SystemProperties.getBoolean("ro.config.disable_launcher_exit_anim", false) ^ 1);
    static final int KILL_APPLICATION_MSG = 22;
    static final int MULTI_WINDOW_MODE_CHANGED_MSG = 23;
    private static final int NOTIFY_ACTIVITY_STATE = 71;
    static final int NOTIFY_CALL = 24;
    private static final String PACKAGE_HWOUC = "com.huawei.android.hwouc";
    private static final String PERMISSION_HWOUC_UPGRADE_REMIND = "com.huawei.android.hwouc.permission.UPGRADE_REMIND";
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
    private static final String SETTING_GUEST_HAS_LOGGED_IN = "guest_has_logged_in";
    private static final int SHOW_GUEST_SWITCH_DIALOG_MSG = 50;
    private static final int SHOW_SWITCH_DIALOG_MSG = 49;
    static final int SHOW_UNINSTALL_LAUNCHER_MSG = 48;
    private static final String SYSTEMUI_NAME = "com.android.systemui";
    static final String TAG = "HwActivityManagerServiceEx";
    private static Set<String> mPIPWhitelists = new HashSet();
    private static Set<String> mTranslucentWhitelists = new HashSet();
    private static Set<String> mWhitelistActivities = new HashSet();
    static final boolean smcsLOGV = SystemProperties.getBoolean("ro.enable.st_debug", false);
    private boolean isLastMultiMode = false;
    final RemoteCallbackList<IHwActivityNotifier> mActivityNotifiers = new RemoteCallbackList();
    private Handler mBootCheckHandler;
    final Context mContext;
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 48:
                    HwActivityManagerServiceEx.this.showUninstallLauncher();
                    return;
                case 49:
                    HwActivityManagerServiceEx.this.mIAmsInner.getUserController().showUserSwitchDialog((Pair) msg.obj);
                    return;
                case 50:
                    HwActivityManagerServiceEx.this.showGuestSwitchDialog(msg.arg1, (String) msg.obj);
                    return;
                case 70:
                    HwActivityManagerServiceEx.this.reportFgToTopMsg(msg);
                    return;
                case HwActivityManagerServiceEx.NOTIFY_ACTIVITY_STATE /*71*/:
                    HwActivityManagerServiceEx.this.handleNotifyActivityState(msg);
                    return;
                case 80:
                    Slog.i(HwActivityManagerServiceEx.TAG, "send UPDATE REMIND broacast to HWOUC");
                    Intent intent = new Intent(HwActivityManagerServiceEx.ACTION_HWOUC_SHOW_UPGRADE_REMIND);
                    intent.setPackage(HwActivityManagerServiceEx.PACKAGE_HWOUC);
                    HwActivityManagerServiceEx.this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM, HwActivityManagerServiceEx.PERMISSION_HWOUC_UPGRADE_REMIND);
                    return;
                default:
                    return;
            }
        }
    };
    Handler mHwHandler = null;
    ServiceThread mHwHandlerThread = null;
    final TaskChangeNotificationController mHwTaskChangeNotificationController;
    IHwActivityManagerInner mIAmsInner = null;
    private String mLastLauncherName;
    private boolean mNeedRemindHwOUC = false;
    private ResetSessionDialog mNewSessionDialog;
    private SettingsObserver mSettingsObserver;
    private RemoteCallbackList<IMWThirdpartyCallback> mThirdPartyCallbackList;

    private class ResetSessionDialog extends AlertDialog implements OnClickListener {
        private final int mUserId;

        public ResetSessionDialog(Context context, int userId) {
            super(context, context.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog", null, null));
            getWindow().setType(2014);
            getWindow().addFlags(655360);
            if (((KeyguardManager) context.getSystemService("keyguard")).isKeyguardLocked()) {
                getWindow().addPrivateFlags(Integer.MIN_VALUE);
            }
            setMessage(context.getString(33685841));
            setButton(-1, context.getString(33685843), this);
            setButton(-2, context.getString(33685842), this);
            setCanceledOnTouchOutside(false);
            this.mUserId = userId;
        }

        public void onClick(DialogInterface dialog, int which) {
            Slog.i(HwActivityManagerServiceEx.TAG, "onClick which:" + which);
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
        private final Context mContext;

        SettingsObserver(Context context, Handler handler) {
            super(handler);
            this.mContext = context;
        }

        public void init() {
            boolean z = false;
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.registerContentObserver(this.URI_HW_UPGRADE_REMIND, false, this, 0);
            HwActivityManagerServiceEx hwActivityManagerServiceEx = HwActivityManagerServiceEx.this;
            if (Secure.getIntForUser(resolver, KEY_HW_UPGRADE_REMIND, 0, 0) != 0) {
                z = true;
            }
            hwActivityManagerServiceEx.mNeedRemindHwOUC = z;
        }

        public void onChange(boolean selfChange, Uri uri) {
            boolean z = false;
            if (this.URI_HW_UPGRADE_REMIND.equals(uri)) {
                HwActivityManagerServiceEx hwActivityManagerServiceEx = HwActivityManagerServiceEx.this;
                if (Secure.getIntForUser(this.mContext.getContentResolver(), KEY_HW_UPGRADE_REMIND, 0, 0) != 0) {
                    z = true;
                }
                hwActivityManagerServiceEx.mNeedRemindHwOUC = z;
                Slog.i(HwActivityManagerServiceEx.TAG, "mNeedRemindHwOUC has changed to : " + HwActivityManagerServiceEx.this.mNeedRemindHwOUC);
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
                Bundle bundle;
                String reason;
                RemoteCallbackList -get2;
                int i;
                switch (msg.what) {
                    case 22:
                        synchronized (HwActivityManagerServiceEx.this.mIAmsInner.getAMSForLock()) {
                            int appId = msg.arg1;
                            int userId = msg.arg2;
                            bundle = msg.obj;
                            String pkg = bundle.getString(HwGpsPowerTracker.DEL_PKG);
                            reason = bundle.getString("reason");
                            Slog.w(HwActivityManagerServiceEx.TAG, "killApplication start for pkg: " + pkg + ", userId: " + userId);
                            HwActivityManagerServiceEx.this.mIAmsInner.forceStopPackageLockedInner(pkg, appId, false, false, true, false, false, userId, reason);
                            Slog.w(HwActivityManagerServiceEx.TAG, "killApplication end for pkg: " + pkg + ", userId: " + userId);
                        }
                        return;
                    case 23:
                        boolean isInMultiWindowMode = ((Boolean) msg.obj).booleanValue();
                        -get2 = HwActivityManagerServiceEx.this.mThirdPartyCallbackList;
                        synchronized (-get2) {
                            try {
                                i = HwActivityManagerServiceEx.this.mThirdPartyCallbackList.beginBroadcast();
                                Flog.i(100, "onMultiWindowModeChanged begin : mThirdPartyCallbackList size : " + i);
                                while (i > 0) {
                                    i--;
                                    try {
                                        ((IMWThirdpartyCallback) HwActivityManagerServiceEx.this.mThirdPartyCallbackList.getBroadcastItem(i)).onModeChanged(isInMultiWindowMode);
                                    } catch (Exception e) {
                                        Flog.e(100, "Error in sending the Callback");
                                    }
                                }
                                Flog.i(100, "onMultiWindowModeChanged end : mThirdPartyCallbackList size : " + i);
                                HwActivityManagerServiceEx.this.mThirdPartyCallbackList.finishBroadcast();
                                break;
                            } catch (IllegalStateException e2) {
                                Flog.e(100, "beginBroadcast() called while already in a broadcast");
                                break;
                            }
                        }
                    case 24:
                        -get2 = HwActivityManagerServiceEx.this.mActivityNotifiers;
                        synchronized (-get2) {
                            try {
                                bundle = (Bundle) msg.obj;
                                String userId2 = String.valueOf(bundle.getInt("android.intent.extra.user_handle"));
                                reason = bundle.getString("android.intent.extra.REASON");
                                i = HwActivityManagerServiceEx.this.mActivityNotifiers.beginBroadcast();
                                while (i > 0) {
                                    i--;
                                    IHwActivityNotifier notifier = (IHwActivityNotifier) HwActivityManagerServiceEx.this.mActivityNotifiers.getBroadcastItem(i);
                                    HashMap<String, String> cookie = (HashMap) HwActivityManagerServiceEx.this.mActivityNotifiers.getBroadcastCookie(i);
                                    if ((userId2.equals(cookie.get("android.intent.extra.user_handle")) || cookie.get("android.intent.extra.USER") != null) && reason.equals(cookie.get("android.intent.extra.REASON"))) {
                                        try {
                                            long start = System.currentTimeMillis();
                                            ((IHwActivityNotifier) HwActivityManagerServiceEx.this.mActivityNotifiers.getBroadcastItem(i)).call(bundle);
                                            Slog.w(HwActivityManagerServiceEx.TAG, "HwActivityNotifier end call " + notifier + " for " + reason + " under user " + userId2 + " cost " + (System.currentTimeMillis() - start));
                                        } catch (RemoteException e3) {
                                            Flog.e(100, "observer.call get RemoteException, remove notifier " + notifier);
                                            HwActivityManagerServiceEx.this.mActivityNotifiers.unregister(notifier);
                                        }
                                    }
                                }
                                HwActivityManagerServiceEx.this.mActivityNotifiers.finishBroadcast();
                                break;
                            } catch (Exception e4) {
                                Flog.e(100, "HwActivityNotifier call error");
                                break;
                            }
                        }
                    default:
                        return;
                }
                return;
            }
        };
        HwBootCheck.bootSceneStart(100, AppHibernateCst.DELAY_ONE_MINS);
        this.mHwTaskChangeNotificationController = new TaskChangeNotificationController(this.mIAmsInner.getAMSForLock(), this.mIAmsInner.getStackSupervisor(), this.mHandler);
        this.mThirdPartyCallbackList = new RemoteCallbackList();
    }

    public int changeGidIfRepairMode(int uid, String processName) {
        if (uid == REPAIR_MODE_SYSTEM_UID && PROCESS_NAME_IN_REPAIR_MODE.contains(processName)) {
            return 1000;
        }
        return uid;
    }

    final void reportFgToTopMsg(Message msg) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(String.valueOf(msg.arg1)).append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(String.valueOf(msg.arg2)).append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(msg.obj);
        this.mIAmsInner.getDAMonitor().DAMonitorReport(this.mIAmsInner.getDAMonitor().getFirstDevSchedEventId(), stringBuffer.toString());
    }

    final void handleNotifyActivityState(Message msg) {
        if (msg != null) {
            String str = null;
            if (msg.obj instanceof String) {
                str = msg.obj;
            }
            if (str == null) {
                Slog.e(TAG, "msg.obj type error.");
                return;
            }
            if (!(this.mIAmsInner == null || this.mIAmsInner.getDAMonitor() == null)) {
                this.mIAmsInner.getDAMonitor().notifyActivityState(str);
            }
        }
    }

    private void showUninstallLauncher() {
        Context mUiContext = this.mIAmsInner.getUiContext();
        try {
            PackageInfo pInfo = this.mContext.getPackageManager().getPackageInfo(this.mLastLauncherName, 0);
            if (pInfo != null) {
                AlertDialog d = new BaseErrorDialog(mUiContext);
                d.getWindow().setType(2010);
                d.setCancelable(false);
                d.setTitle(mUiContext.getString(33685930));
                String appName = this.mContext.getPackageManager().getApplicationLabel(pInfo.applicationInfo).toString();
                d.setMessage(mUiContext.getString(33685932, new Object[]{appName}));
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

    public void showUninstallLauncherDialog(String pkgName) {
        this.mLastLauncherName = pkgName;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(48));
    }

    private void showGuestSwitchDialog(int userId, String userName) {
        cancelDialog();
        ContentResolver cr = this.mContext.getContentResolver();
        int notFirstLogin = System.getIntForUser(cr, SETTING_GUEST_HAS_LOGGED_IN, 0, userId);
        Slog.i(TAG, "notFirstLogin:" + notFirstLogin + ", userid=" + userId);
        if (notFirstLogin != 0) {
            showGuestResetSessionDialog(userId);
            return;
        }
        System.putIntForUser(cr, SETTING_GUEST_HAS_LOGGED_IN, 1, userId);
        sendMessageToSwitchUser(userId, userName);
    }

    public void killApplication(String pkg, int appId, int userId, String reason) {
        if (appId < 0) {
            Slog.w(TAG, "Invalid appid specified for pkg : " + pkg);
            return;
        }
        int callerUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callerUid) == 1000) {
            Message msg = this.mHwHandler.obtainMessage(22);
            msg.arg1 = appId;
            msg.arg2 = userId;
            Bundle bundle = new Bundle();
            Slog.w(TAG, "killApplication send message for pkg: " + pkg + ", userId: " + userId);
            bundle.putString(HwGpsPowerTracker.DEL_PKG, pkg);
            bundle.putString("reason", reason);
            msg.obj = bundle;
            this.mHwHandler.sendMessage(msg);
            return;
        }
        throw new SecurityException(callerUid + " cannot kill pkg: " + pkg);
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
            if (!conn.waiting || !inLaunching || (always ^ 1) == 0) {
                ProcessRecord capp = conn.client;
                conn.dead = true;
                if (conn.stableCount > 0) {
                    if (!(capp.persistent || capp.thread == null || capp.pid == 0 || capp.pid == this.mIAmsInner.getAmsPid())) {
                        capp.kill("depends on provider " + cpr.name.flattenToShortString() + " in dying proc " + (proc != null ? proc.processName : "??"), true);
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

    public boolean cleanPackageRes(List<String> packageList, Map<String, List<String>> alarmTags, int targetUid, boolean cleanAlarm, boolean isNative, boolean hasPerceptAlarm) {
        if (packageList == null) {
            return false;
        }
        boolean didSomething = false;
        int userId = UserHandle.getUserId(targetUid);
        synchronized (this.mIAmsInner.getAMSForLock()) {
            for (String packageName : packageList) {
                int i;
                if ((isNative || canCleanTaskRecord(packageName)) && this.mIAmsInner.finishDisabledPackageActivitiesLocked(packageName, null, true, false, userId)) {
                    didSomething = true;
                }
                if (this.mIAmsInner.bringDownDisabledPackageServicesLocked(packageName, null, userId, false, true, true)) {
                    didSomething = true;
                }
                if (packageName == null) {
                    this.mIAmsInner.getStickyBroadcasts().remove(userId);
                }
                ArrayList<ContentProviderRecord> providers = new ArrayList();
                if (this.mIAmsInner.getProviderMap().collectPackageProvidersLocked(packageName, null, true, false, userId, providers)) {
                    didSomething = true;
                }
                for (i = providers.size() - 1; i >= 0; i--) {
                    cleanProviderLocked(null, (ContentProviderRecord) providers.get(i), true);
                }
                for (i = this.mIAmsInner.getBroadcastQueues().length - 1; i >= 0; i--) {
                    didSomething |= this.mIAmsInner.getBroadcastQueues()[i].cleanupDisabledPackageReceiversLocked(packageName, null, userId, true);
                }
                if (alarmTags == null) {
                    this.mIAmsInner.getAlarmService().removePackageAlarm(packageName, null, targetUid);
                } else if (cleanAlarm) {
                    if (this.mIAmsInner.getAlarmService() != null) {
                        List<String> tags = (List) alarmTags.get(packageName);
                        if (tags != null) {
                            this.mIAmsInner.getAlarmService().removePackageAlarm(packageName, tags, targetUid);
                        }
                    }
                }
                if (isNative || (hasPerceptAlarm ^ 1) != 0) {
                    this.mIAmsInner.finishForceStopPackageLockedInner(packageName, targetUid);
                }
            }
        }
        return didSomething;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean canCleanTaskRecord(String packageName) {
        if (packageName == null) {
            return true;
        }
        synchronized (this.mIAmsInner.getAMSForLock()) {
            ArrayList<TaskRecord> recentTasks = this.mIAmsInner.getRecentTasks();
            if (recentTasks == null) {
                return true;
            }
            int size = recentTasks.size();
            int foundNum = 0;
            int maxFoundNum = this.mIAmsInner.getDAMonitor().getActivityImportCount();
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
            Slog.e(TAG, "check low storage error because e: " + e);
        }
        if (isStorageLow) {
            UiThread.getHandler().post(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(HwActivityManagerServiceEx.this.mContext, HwActivityManagerServiceEx.this.mContext.getResources().getString(17040336), 1);
                    toast.getWindowParams().type = 2006;
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
        UserInfo mCurrentUserInfo = userctl.getUserInfo(userctl.getCurrentUserIdLocked());
        int targetUserId = userId;
        UserInfo mTargetUserInfo = userctl.getUserInfo(userId);
        userctl.setTargetUserIdLocked(userId);
        Pair<UserInfo, UserInfo> userNames = new Pair(mCurrentUserInfo, mTargetUserInfo);
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
            Slog.d(TAG, "Create new guest, switching to = " + newGuest.id);
            sendMessageToSwitchUser(newGuest.id, newGuest.name);
            System.putIntForUser(this.mContext.getContentResolver(), SETTING_GUEST_HAS_LOGGED_IN, 1, newGuest.id);
            userManager.removeUser(userId);
            return;
        }
        Slog.w(TAG, "Couldn't mark the guest for deletion for user " + userId);
    }

    public TaskChangeNotificationController getHwTaskChangeController() {
        return this.mHwTaskChangeNotificationController;
    }

    public void onAppGroupChanged(int pid, int uid, String pkgName, int oldSchedGroup, int newSchedGroup) {
        if (newSchedGroup == 2) {
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
        if (this.mNeedRemindHwOUC && r.userId == 0 && r.isHomeActivity() && state.equals(ActivityState.RESUMED.toString())) {
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
            stringBuffer.append(r.packageName).append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.shortComponentName).append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.appInfo.uid).append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(r.app.pid).append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            stringBuffer.append(state);
            return stringBuffer.toString();
        }
    }

    public boolean isApplyPersistAppPatch(String ssp, int uid, int userId, boolean bWillRestart, boolean evenPersistent, String reason, String action) {
        boolean endsWith;
        boolean bResult = false;
        boolean bHandle = "android.intent.action.PACKAGE_REMOVED".equals(action);
        if (bWillRestart && evenPersistent && reason != null) {
            endsWith = reason.endsWith(REASON_SYS_REPLACE);
        } else {
            endsWith = false;
        }
        if (!endsWith && (bHandle ^ 1) != 0) {
            return false;
        }
        ApplicationInfo info = this.mIAmsInner.getPackageManagerInternal().getApplicationInfo(ssp, 1152, Process.myUid(), userId);
        if (info == null) {
            return false;
        }
        ProcessRecord apprecord = this.mIAmsInner.getProcessRecord(info.processName, uid, true);
        if ((bHandle && apprecord != null && !apprecord.persistent) || apprecord == null) {
            return false;
        }
        if (!(apprecord.info == null || apprecord.info.sourceDir == null || ((apprecord.info.hwFlags & 536870912) == 0 && (info.metaData == null || !info.metaData.getBoolean(APKPATCH_META_DATA, false))))) {
            if (!apprecord.info.sourceDir.equals(info.sourceDir) && bHandle) {
                this.mIAmsInner.forceStopPackageLockedInner(ssp, uid, true, false, true, true, false, userId, REASON_SYS_REPLACE);
                Slog.i("PatchService", action + TAG + "-----kill & restart---");
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
                String packageName = r.packageName;
                if (packageName != null && HwPCUtils.isPcDynamicStack(stackId) && (multiWindowMgr.isSpecialVideo(packageName) || multiWindowMgr.isPortraitApp(r.getTask()))) {
                    return true;
                }
            }
        }
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
        if (HW_SUPPORT_LAUNCHER_EXIT_ANIM && from != null && to != null && to.isHomeActivity() && "com.huawei.android.launcher".equals(to.packageName)) {
            String packageName = from.packageName;
            TaskRecord tr = from.getTask();
            if (!(tr == null || tr.getRootActivity() == null)) {
                packageName = tr.getRootActivity().packageName;
            }
            if (ACTION_CONFIRM_APPLOCK_PACKAGENAME.equals(packageName) && ACTION_CONFIRM_APPLOCK_CREDENTIAL.equals(from.intent.getAction())) {
                packageName = from.intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
            }
            Slog.w(TAG, "setResumedActivityUncheckLocked start call, from: " + from + ", to: " + to);
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
        }
    }

    public void dispatchActivityLifeState(ActivityRecord r, String state) {
        if (r != null) {
            TaskRecord task = r.task;
            if (mTranslucentWhitelists.contains(r.info.name) && task != null) {
                for (int i = task.mActivities.size() - 1; i >= 0; i--) {
                    ActivityRecord activityRecord = (ActivityRecord) task.mActivities.get(i);
                    if (activityRecord != null && (activityRecord.finishing ^ 1) != 0 && activityRecord != r) {
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
            if (mWhitelistActivities.contains(comp.getClassName())) {
                bounds = new Rect();
            }
            bundle.putString("state", state);
            bundle.putParcelable("comp", comp);
            bundle.putParcelable("bounds", bounds);
            bundle.putBoolean("isFloating", !mPIPWhitelists.contains(comp.getClassName()) ? r.isFloating() : true);
            bundle.putBoolean("isHomeActivity", r.isHomeActivity());
            bundle.putString("android.intent.extra.REASON", "activityLifeState");
            bundle.putInt("android.intent.extra.user_handle", this.mIAmsInner.getUserController().getCurrentUserIdLocked());
            Message msg = this.mHwHandler.obtainMessage(24);
            msg.obj = bundle;
            this.mHwHandler.sendMessage(msg);
        }
    }

    public void systemReady(Runnable goingCallback, TimingsTraceLog traceLog) {
        this.mSettingsObserver = new SettingsObserver(this.mContext, this.mHandler);
        this.mSettingsObserver.init();
    }

    public int canAppBoost(ApplicationInfo appInfo) {
        if (appInfo == null) {
            return 1;
        }
        BatteryStatsService mBatteryStatsService = this.mIAmsInner.getBatteryStatsService();
        if (mBatteryStatsService == null || mBatteryStatsService.getActiveStatistics().isScreenOn()) {
            return 1;
        }
        int type = getAppType(-1, appInfo);
        if (type == 4) {
            return 0;
        }
        if (1 != type) {
            String packageName = appInfo.packageName;
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
            if (!((flags & 1) == 0 || (100663296 & hwFlags) == 0)) {
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

    public void gestureToHome() {
        Bundle bundle = new Bundle();
        ActivityRecord from = this.mIAmsInner.getLastResumedActivityRecord();
        if (from != null) {
            String packageName = from.packageName;
            TaskRecord tr = from.getTask();
            if (ACTION_CONFIRM_APPLOCK_PACKAGENAME.equals(packageName) && ACTION_CONFIRM_APPLOCK_CREDENTIAL.equals(from.intent.getAction())) {
                packageName = from.intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
            } else if (!(tr == null || tr.getRootActivity() == null)) {
                packageName = tr.getRootActivity().packageName;
            }
            Slog.w(TAG, "gestureToHome, from: " + from);
            bundle.putString("package", packageName);
            bundle.putString("topPackage", from.packageName);
            bundle.putBoolean("isTransluent", from.isTransluent());
            bundle.putInt("userId", from.userId);
            bundle.putString("android.intent.extra.REASON", "returnToHome");
            bundle.putString("returnToHome", "gesture");
            bundle.putInt("android.intent.extra.user_handle", this.mIAmsInner.getUserController().getCurrentOrTargetUserIdLocked());
            Message msg = this.mHwHandler.obtainMessage(24);
            msg.obj = bundle;
            this.mHwHandler.sendMessageAtFrontOfQueue(msg);
        }
    }

    public void setActivityVisibleState(boolean state) {
        ActivityRecord lastResumedActivity = this.mIAmsInner.getLastResumedActivityRecord();
        Slog.i(TAG, " setActivityVisibleState: " + state + " ResumedActivity : " + lastResumedActivity);
        if (state) {
            this.mIAmsInner.getAMSForLock().mWindowManager.setViewAlpha(1.0f);
            return;
        }
        lastResumedActivity.mAlreadyScreenshot = true;
        lastResumedActivity.updateThumbnailLocked(lastResumedActivity.screenshotActivityLocked(), null);
        this.mIAmsInner.getAMSForLock().mWindowManager.setViewAlpha(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
    }
}
