package com.android.server.trust;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.ITrustListener;
import android.app.trust.ITrustManager.Stub;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.DumpUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrustManagerService extends SystemService {
    static final boolean DEBUG;
    private static final int MSG_CLEANUP_USER = 8;
    private static final int MSG_DISPATCH_UNLOCK_ATTEMPT = 3;
    private static final int MSG_DISPATCH_UNLOCK_LOCKOUT = 13;
    private static final int MSG_ENABLED_AGENTS_CHANGED = 4;
    private static final int MSG_FLUSH_TRUST_USUALLY_MANAGED = 10;
    private static final int MSG_KEYGUARD_SHOWING_CHANGED = 6;
    private static final int MSG_REFRESH_DEVICE_LOCKED_FOR_USER = 14;
    private static final int MSG_REGISTER_LISTENER = 1;
    private static final int MSG_START_USER = 7;
    private static final int MSG_STOP_USER = 12;
    private static final int MSG_SWITCH_USER = 9;
    private static final int MSG_UNLOCK_USER = 11;
    private static final int MSG_UNREGISTER_LISTENER = 2;
    private static final String PERMISSION_PROVIDE_AGENT = "android.permission.PROVIDE_TRUST_AGENT";
    private static final String TAG = "TrustManagerService";
    private static final Intent TRUST_AGENT_INTENT = new Intent("android.service.trust.TrustAgentService");
    private static final int TRUST_USUALLY_MANAGED_FLUSH_DELAY = 120000;
    private final ArraySet<AgentInfo> mActiveAgents = new ArraySet();
    private final ActivityManager mActivityManager;
    final TrustArchive mArchive = new TrustArchive();
    private final Context mContext;
    private int mCurrentUser = 0;
    private HwCustTrustManagerService mCust;
    @GuardedBy("mDeviceLockedForUser")
    private final SparseBooleanArray mDeviceLockedForUser = new SparseBooleanArray();
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            boolean i = false;
            switch (msg.what) {
                case 1:
                    TrustManagerService.this.addListener((ITrustListener) msg.obj);
                    return;
                case 2:
                    TrustManagerService.this.removeListener((ITrustListener) msg.obj);
                    return;
                case 3:
                    TrustManagerService trustManagerService = TrustManagerService.this;
                    if (msg.arg1 != 0) {
                        i = true;
                    }
                    trustManagerService.dispatchUnlockAttempt(i, msg.arg2);
                    return;
                case 4:
                    TrustManagerService.this.refreshAgentList(-1);
                    TrustManagerService.this.refreshDeviceLockedForUser(-1);
                    return;
                case 6:
                    TrustManagerService.this.refreshDeviceLockedForUser(TrustManagerService.this.mCurrentUser);
                    return;
                case 7:
                case 8:
                case 11:
                    TrustManagerService.this.refreshAgentList(msg.arg1);
                    return;
                case 9:
                    TrustManagerService.this.mCurrentUser = msg.arg1;
                    TrustManagerService.this.refreshDeviceLockedForUser(-1);
                    return;
                case 10:
                    SparseBooleanArray usuallyManaged;
                    synchronized (TrustManagerService.this.mTrustUsuallyManagedForUser) {
                        usuallyManaged = TrustManagerService.this.mTrustUsuallyManagedForUser.clone();
                    }
                    while (true) {
                        int i2;
                        int i3 = i2;
                        if (i3 < usuallyManaged.size()) {
                            i2 = usuallyManaged.keyAt(i3);
                            boolean value = usuallyManaged.valueAt(i3);
                            if (value != TrustManagerService.this.mLockPatternUtils.isTrustUsuallyManaged(i2)) {
                                TrustManagerService.this.mLockPatternUtils.setTrustUsuallyManaged(value, i2);
                            }
                            i2 = i3 + 1;
                        } else {
                            return;
                        }
                    }
                case 12:
                    TrustManagerService.this.setDeviceLockedForUser(msg.arg1, true);
                    return;
                case 13:
                    TrustManagerService.this.dispatchUnlockLockout(msg.arg1, msg.arg2);
                    return;
                case 14:
                    TrustManagerService.this.refreshDeviceLockedForUser(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    };
    private final LockPatternUtils mLockPatternUtils;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onSomePackagesChanged() {
            TrustManagerService.this.refreshAgentList(-1);
        }

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            return true;
        }

        public void onPackageDisappeared(String packageName, int reason) {
            TrustManagerService.this.removeAgentsOfPackage(packageName);
        }
    };
    private final Receiver mReceiver = new Receiver(this, null);
    private final IBinder mService = new Stub() {
        public void reportUnlockAttempt(boolean authenticated, int userId) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.obtainMessage(3, authenticated, userId).sendToTarget();
        }

        public void reportUnlockLockout(int timeoutMs, int userId) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.obtainMessage(13, timeoutMs, userId).sendToTarget();
        }

        public void reportEnabledTrustAgentsChanged(int userId) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.removeMessages(4);
            TrustManagerService.this.mHandler.sendEmptyMessage(4);
        }

        public void reportKeyguardShowingChanged() throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.removeMessages(6);
            TrustManagerService.this.mHandler.sendEmptyMessage(6);
            TrustManagerService.this.mHandler.runWithScissors(-$$Lambda$TrustManagerService$1$98HKBkg-C1PLlz_Q1vJz1OJtw4c.INSTANCE, 0);
        }

        static /* synthetic */ void lambda$reportKeyguardShowingChanged$0() {
        }

        public void registerTrustListener(ITrustListener trustListener) throws RemoteException {
            enforceListenerPermission();
            TrustManagerService.this.mHandler.obtainMessage(1, trustListener).sendToTarget();
        }

        public void unregisterTrustListener(ITrustListener trustListener) throws RemoteException {
            enforceListenerPermission();
            TrustManagerService.this.mHandler.obtainMessage(2, trustListener).sendToTarget();
        }

        public boolean isDeviceLocked(int userId) throws RemoteException {
            userId = ActivityManager.handleIncomingUser(AnonymousClass1.getCallingPid(), AnonymousClass1.getCallingUid(), userId, false, true, "isDeviceLocked", null);
            long token = Binder.clearCallingIdentity();
            try {
                if (!TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId)) {
                    userId = TrustManagerService.this.resolveProfileParent(userId);
                }
                boolean isDeviceLockedInner = TrustManagerService.this.isDeviceLockedInner(userId);
                return isDeviceLockedInner;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isDeviceSecure(int userId) throws RemoteException {
            userId = ActivityManager.handleIncomingUser(AnonymousClass1.getCallingPid(), AnonymousClass1.getCallingUid(), userId, false, true, "isDeviceSecure", null);
            long token = Binder.clearCallingIdentity();
            try {
                if (!TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId)) {
                    userId = TrustManagerService.this.resolveProfileParent(userId);
                }
                boolean isSecure = TrustManagerService.this.mLockPatternUtils.isSecure(userId);
                return isSecure;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        private void enforceReportPermission() {
            TrustManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_KEYGUARD_SECURE_STORAGE", "reporting trust events");
        }

        private void enforceListenerPermission() {
            TrustManagerService.this.mContext.enforceCallingPermission("android.permission.TRUST_LISTENER", "register trust listener");
        }

        protected void dump(FileDescriptor fd, final PrintWriter fout, String[] args) {
            if (!DumpUtils.checkDumpPermission(TrustManagerService.this.mContext, TrustManagerService.TAG, fout)) {
                return;
            }
            if (TrustManagerService.this.isSafeMode()) {
                fout.println("disabled because the system is in safe mode.");
            } else if (TrustManagerService.this.mTrustAgentsCanRun) {
                final List<UserInfo> userInfos = TrustManagerService.this.mUserManager.getUsers(true);
                TrustManagerService.this.mHandler.runWithScissors(new Runnable() {
                    public void run() {
                        fout.println("Trust manager state:");
                        for (UserInfo user : userInfos) {
                            AnonymousClass1.this.dumpUser(fout, user, user.id == TrustManagerService.this.mCurrentUser);
                        }
                    }
                }, 1500);
            } else {
                fout.println("disabled because the third-party apps can't run yet.");
            }
        }

        private void dumpUser(PrintWriter fout, UserInfo user, boolean isCurrent) {
            fout.printf(" User \"%s\" (id=%d, flags=%#x)", new Object[]{user.name, Integer.valueOf(user.id), Integer.valueOf(user.flags)});
            if (user.supportsSwitchToByUser()) {
                if (isCurrent) {
                    fout.print(" (current)");
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(": trusted=");
                stringBuilder.append(dumpBool(TrustManagerService.this.aggregateIsTrusted(user.id)));
                fout.print(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", trustManaged=");
                stringBuilder.append(dumpBool(TrustManagerService.this.aggregateIsTrustManaged(user.id)));
                fout.print(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", deviceLocked=");
                stringBuilder.append(dumpBool(TrustManagerService.this.isDeviceLockedInner(user.id)));
                fout.print(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(", strongAuthRequired=");
                stringBuilder.append(dumpHex(TrustManagerService.this.mStrongAuthTracker.getStrongAuthForUser(user.id)));
                fout.print(stringBuilder.toString());
                fout.println();
                fout.println("   Enabled agents:");
                boolean duplicateSimpleNames = false;
                ArraySet<String> simpleNames = new ArraySet();
                Iterator it = TrustManagerService.this.mActiveAgents.iterator();
                while (it.hasNext()) {
                    AgentInfo info = (AgentInfo) it.next();
                    if (info.userId == user.id) {
                        boolean trusted = info.agent.isTrusted();
                        fout.print("    ");
                        fout.println(info.component.flattenToShortString());
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("     bound=");
                        stringBuilder2.append(dumpBool(info.agent.isBound()));
                        fout.print(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(", connected=");
                        stringBuilder2.append(dumpBool(info.agent.isConnected()));
                        fout.print(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(", managingTrust=");
                        stringBuilder2.append(dumpBool(info.agent.isManagingTrust()));
                        fout.print(stringBuilder2.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(", trusted=");
                        stringBuilder2.append(dumpBool(trusted));
                        fout.print(stringBuilder2.toString());
                        fout.println();
                        if (trusted) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("      message=\"");
                            stringBuilder2.append(info.agent.getMessage());
                            stringBuilder2.append("\"");
                            fout.println(stringBuilder2.toString());
                        }
                        if (!info.agent.isConnected()) {
                            String restartTime = TrustArchive.formatDuration(info.agent.getScheduledRestartUptimeMillis() - SystemClock.uptimeMillis());
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("      restartScheduledAt=");
                            stringBuilder3.append(restartTime);
                            fout.println(stringBuilder3.toString());
                        }
                        if (!simpleNames.add(TrustArchive.getSimpleName(info.component))) {
                            duplicateSimpleNames = true;
                        }
                    }
                }
                fout.println("   Events:");
                TrustManagerService.this.mArchive.dump(fout, 50, user.id, "    ", duplicateSimpleNames);
                fout.println();
                return;
            }
            fout.println("(managed profile)");
            fout.println("   disabled because switching to this user is not possible.");
        }

        private String dumpBool(boolean b) {
            return b ? "1" : "0";
        }

        private String dumpHex(int i) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("0x");
            stringBuilder.append(Integer.toHexString(i));
            return stringBuilder.toString();
        }

        public void setDeviceLockedForUser(int userId, boolean locked) {
            enforceReportPermission();
            long identity = Binder.clearCallingIdentity();
            try {
                if (TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId)) {
                    synchronized (TrustManagerService.this.mDeviceLockedForUser) {
                        TrustManagerService.this.mDeviceLockedForUser.put(userId, locked);
                    }
                    if (locked) {
                        try {
                            ActivityManager.getService().notifyLockedProfile(userId);
                        } catch (RemoteException e) {
                        }
                    }
                    Intent lockIntent = new Intent("android.intent.action.DEVICE_LOCKED_CHANGED");
                    lockIntent.addFlags(1073741824);
                    lockIntent.putExtra("android.intent.extra.user_handle", userId);
                    TrustManagerService.this.mContext.sendBroadcastAsUser(lockIntent, UserHandle.SYSTEM, "android.permission.TRUST_LISTENER", null);
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isTrustUsuallyManaged(int userId) {
            TrustManagerService.this.mContext.enforceCallingPermission("android.permission.TRUST_LISTENER", "query trust state");
            return TrustManagerService.this.isTrustUsuallyManagedInternal(userId);
        }

        public void unlockedByFingerprintForUser(int userId) {
            enforceReportPermission();
            synchronized (TrustManagerService.this.mUsersUnlockedByFingerprint) {
                TrustManagerService.this.mUsersUnlockedByFingerprint.put(userId, true);
            }
            TrustManagerService.this.mHandler.obtainMessage(14, userId, 0).sendToTarget();
        }

        public void clearAllFingerprints() {
            enforceReportPermission();
            synchronized (TrustManagerService.this.mUsersUnlockedByFingerprint) {
                TrustManagerService.this.mUsersUnlockedByFingerprint.clear();
            }
            TrustManagerService.this.mHandler.obtainMessage(14, -1, 0).sendToTarget();
        }
    };
    private final StrongAuthTracker mStrongAuthTracker;
    private boolean mTrustAgentsCanRun = false;
    private final ArrayList<ITrustListener> mTrustListeners = new ArrayList();
    @GuardedBy("mTrustUsuallyManagedForUser")
    private final SparseBooleanArray mTrustUsuallyManagedForUser = new SparseBooleanArray();
    @GuardedBy("mUserIsTrusted")
    private final SparseBooleanArray mUserIsTrusted = new SparseBooleanArray();
    private final UserManager mUserManager;
    @GuardedBy("mUsersUnlockedByFingerprint")
    private final SparseBooleanArray mUsersUnlockedByFingerprint = new SparseBooleanArray();

    private static final class AgentInfo {
        TrustAgentWrapper agent;
        ComponentName component;
        Drawable icon;
        CharSequence label;
        SettingsAttrs settings;
        int userId;

        private AgentInfo() {
        }

        /* synthetic */ AgentInfo(AnonymousClass1 x0) {
            this();
        }

        public boolean equals(Object other) {
            boolean z = false;
            if (!(other instanceof AgentInfo)) {
                return false;
            }
            AgentInfo o = (AgentInfo) other;
            if (this.component.equals(o.component) && this.userId == o.userId) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (this.component.hashCode() * 31) + this.userId;
        }
    }

    private class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        /* synthetic */ Receiver(TrustManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId;
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                TrustManagerService.this.refreshAgentList(getSendingUserId());
                TrustManagerService.this.updateDevicePolicyFeatures();
            } else if ("android.intent.action.USER_ADDED".equals(action)) {
                userId = getUserId(intent);
                if (userId > 0) {
                    TrustManagerService.this.maybeEnableFactoryTrustAgents(TrustManagerService.this.mLockPatternUtils, userId);
                }
            } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                userId = getUserId(intent);
                if (userId > 0) {
                    synchronized (TrustManagerService.this.mUserIsTrusted) {
                        TrustManagerService.this.mUserIsTrusted.delete(userId);
                    }
                    synchronized (TrustManagerService.this.mDeviceLockedForUser) {
                        TrustManagerService.this.mDeviceLockedForUser.delete(userId);
                    }
                    synchronized (TrustManagerService.this.mTrustUsuallyManagedForUser) {
                        TrustManagerService.this.mTrustUsuallyManagedForUser.delete(userId);
                    }
                    synchronized (TrustManagerService.this.mUsersUnlockedByFingerprint) {
                        TrustManagerService.this.mUsersUnlockedByFingerprint.delete(userId);
                    }
                    TrustManagerService.this.refreshAgentList(userId);
                    TrustManagerService.this.refreshDeviceLockedForUser(userId);
                }
            }
        }

        private int getUserId(Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -100);
            if (userId > 0) {
                return userId;
            }
            String str = TrustManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EXTRA_USER_HANDLE missing or invalid, value=");
            stringBuilder.append(userId);
            Slog.wtf(str, stringBuilder.toString());
            return -100;
        }

        public void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
            filter.addAction("android.intent.action.USER_ADDED");
            filter.addAction("android.intent.action.USER_REMOVED");
            context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, null);
        }
    }

    private static class SettingsAttrs {
        public boolean canUnlockProfile;
        public ComponentName componentName;

        public SettingsAttrs(ComponentName componentName, boolean canUnlockProfile) {
            this.componentName = componentName;
            this.canUnlockProfile = canUnlockProfile;
        }
    }

    private class StrongAuthTracker extends com.android.internal.widget.LockPatternUtils.StrongAuthTracker {
        SparseBooleanArray mStartFromSuccessfulUnlock = new SparseBooleanArray();

        public StrongAuthTracker(Context context) {
            super(context);
        }

        public void onStrongAuthRequiredChanged(int userId) {
            this.mStartFromSuccessfulUnlock.delete(userId);
            if (TrustManagerService.DEBUG) {
                String str = TrustManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onStrongAuthRequiredChanged(");
                stringBuilder.append(userId);
                stringBuilder.append(") -> trustAllowed=");
                stringBuilder.append(isTrustAllowedForUser(userId));
                stringBuilder.append(" agentsCanRun=");
                stringBuilder.append(canAgentsRunForUser(userId));
                Log.i(str, stringBuilder.toString());
            }
            TrustManagerService.this.refreshAgentList(userId);
            TrustManagerService.this.updateTrust(userId, 0);
        }

        boolean canAgentsRunForUser(int userId) {
            return this.mStartFromSuccessfulUnlock.get(userId) || super.isTrustAllowedForUser(userId);
        }

        void allowTrustFromUnlock(int userId) {
            if (userId >= 0) {
                boolean previous = canAgentsRunForUser(userId);
                this.mStartFromSuccessfulUnlock.put(userId, true);
                if (TrustManagerService.DEBUG) {
                    String str = TrustManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("allowTrustFromUnlock(");
                    stringBuilder.append(userId);
                    stringBuilder.append(") -> trustAllowed=");
                    stringBuilder.append(isTrustAllowedForUser(userId));
                    stringBuilder.append(" agentsCanRun=");
                    stringBuilder.append(canAgentsRunForUser(userId));
                    Log.i(str, stringBuilder.toString());
                }
                if (canAgentsRunForUser(userId) != previous) {
                    TrustManagerService.this.refreshAgentList(userId);
                    return;
                }
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("userId must be a valid user: ");
            stringBuilder2.append(userId);
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    static {
        boolean z = Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 2);
        DEBUG = z;
    }

    public TrustManagerService(Context context) {
        super(context);
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mStrongAuthTracker = new StrongAuthTracker(context);
        this.mCust = (HwCustTrustManagerService) HwCustUtils.createObj(HwCustTrustManagerService.class, new Object[0]);
    }

    public void onStart() {
        publishBinderService("trust", this.mService);
    }

    public void onBootPhase(int phase) {
        if (!isSafeMode()) {
            if (phase == 500) {
                this.mPackageMonitor.register(this.mContext, this.mHandler.getLooper(), UserHandle.ALL, true);
                this.mReceiver.register(this.mContext);
                this.mLockPatternUtils.registerStrongAuthTracker(this.mStrongAuthTracker);
            } else if (phase == 600) {
                this.mTrustAgentsCanRun = true;
                refreshAgentList(-1);
                refreshDeviceLockedForUser(-1);
            } else if (phase == 1000) {
                maybeEnableFactoryTrustAgents(this.mLockPatternUtils, 0);
            }
        }
    }

    private void updateTrustAll() {
        for (UserInfo userInfo : this.mUserManager.getUsers(true)) {
            updateTrust(userInfo.id, 0);
        }
    }

    public void updateTrust(int userId, int flags) {
        boolean changed;
        boolean managed = aggregateIsTrustManaged(userId);
        dispatchOnTrustManagedChanged(managed, userId);
        if (this.mStrongAuthTracker.isTrustAllowedForUser(userId) && isTrustUsuallyManagedInternal(userId) != managed) {
            updateTrustUsuallyManaged(userId, managed);
        }
        boolean trusted = aggregateIsTrusted(userId);
        synchronized (this.mUserIsTrusted) {
            changed = this.mUserIsTrusted.get(userId) != trusted;
            this.mUserIsTrusted.put(userId, trusted);
        }
        dispatchOnTrustChanged(trusted, userId, flags);
        if (changed) {
            refreshDeviceLockedForUser(userId);
        }
    }

    private void updateTrustUsuallyManaged(int userId, boolean managed) {
        synchronized (this.mTrustUsuallyManagedForUser) {
            this.mTrustUsuallyManagedForUser.put(userId, managed);
        }
        this.mHandler.removeMessages(10);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(10), JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
    }

    public long addEscrowToken(byte[] token, int userId) {
        return this.mLockPatternUtils.addEscrowToken(token, userId);
    }

    public boolean removeEscrowToken(long handle, int userId) {
        return this.mLockPatternUtils.removeEscrowToken(handle, userId);
    }

    public boolean isEscrowTokenActive(long handle, int userId) {
        return this.mLockPatternUtils.isEscrowTokenActive(handle, userId);
    }

    public void unlockUserWithToken(long handle, byte[] token, int userId) {
        this.mLockPatternUtils.unlockUserWithToken(handle, token, userId);
    }

    void showKeyguardErrorMessage(CharSequence message) {
        dispatchOnTrustError(message);
    }

    /* JADX WARNING: Removed duplicated region for block: B:108:0x0328  */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x034c  */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x0346  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void refreshAgentList(int userIdOrAll) {
        String str;
        int userIdOrAll2 = userIdOrAll;
        if (DEBUG) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("refreshAgentList(");
            stringBuilder.append(userIdOrAll2);
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mTrustAgentsCanRun) {
            StringBuilder stringBuilder2;
            List<UserInfo> userInfos;
            List<UserInfo> userInfos2;
            LockPatternUtils lockPatternUtils;
            if (userIdOrAll2 != -1 && userIdOrAll2 < 0) {
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("refreshAgentList(userId=");
                stringBuilder2.append(userIdOrAll2);
                stringBuilder2.append("): Invalid user handle, must be USER_ALL or a specific user.");
                Log.e(str2, stringBuilder2.toString(), new Throwable("here"));
                userIdOrAll2 = -1;
            }
            PackageManager pm = this.mContext.getPackageManager();
            boolean z = true;
            if (userIdOrAll2 == -1) {
                userInfos = this.mUserManager.getUsers(true);
            } else {
                userInfos = new ArrayList();
                userInfos.add(this.mUserManager.getUserInfo(userIdOrAll2));
            }
            LockPatternUtils lockPatternUtils2 = this.mLockPatternUtils;
            ArraySet<AgentInfo> obsoleteAgents = new ArraySet();
            obsoleteAgents.addAll(this.mActiveAgents);
            for (UserInfo userInfo : userInfos) {
                if (!(userInfo == null || userInfo.partial || !userInfo.isEnabled())) {
                    if (!userInfo.guestToRemove) {
                        String str3;
                        StringBuilder stringBuilder3;
                        if (userInfo.supportsSwitchToByUser()) {
                            if (this.mActivityManager.isUserRunning(userInfo.id)) {
                                if (lockPatternUtils2.isSecure(userInfo.id)) {
                                    DevicePolicyManager dpm = lockPatternUtils2.getDevicePolicyManager();
                                    boolean disableTrustAgents = (dpm.getKeyguardDisabledFeatures(null, userInfo.id) & 16) != 0 ? z : false;
                                    List<ComponentName> enabledAgents = lockPatternUtils2.getEnabledTrustAgents(userInfo.id);
                                    if (enabledAgents != null) {
                                        List<ResolveInfo> resolveInfos = resolveAllowedTrustAgents(pm, userInfo.id);
                                        for (ResolveInfo resolveInfo : resolveInfos) {
                                            List<ResolveInfo> resolveInfos2;
                                            ComponentName name = getComponentName(resolveInfo);
                                            if (enabledAgents.contains(name)) {
                                                String str4;
                                                PackageManager pm2;
                                                resolveInfos2 = resolveInfos;
                                                userInfos2 = userInfos;
                                                lockPatternUtils = lockPatternUtils2;
                                                if (disableTrustAgents) {
                                                    List<PersistableBundle> config = dpm.getTrustAgentConfiguration(null, name, userInfo.id);
                                                    if (config == null || config.isEmpty()) {
                                                        if (DEBUG) {
                                                            String str5 = TAG;
                                                            StringBuilder stringBuilder4 = new StringBuilder();
                                                            stringBuilder4.append("refreshAgentList: skipping ");
                                                            stringBuilder4.append(name.flattenToShortString());
                                                            stringBuilder4.append(" u");
                                                            stringBuilder4.append(userInfo.id);
                                                            stringBuilder4.append(": not allowed by DPM");
                                                            Slog.d(str5, stringBuilder4.toString());
                                                        }
                                                    }
                                                }
                                                AgentInfo agentInfo = new AgentInfo();
                                                agentInfo.component = name;
                                                agentInfo.userId = userInfo.id;
                                                if (this.mActiveAgents.contains(agentInfo)) {
                                                    agentInfo = (AgentInfo) this.mActiveAgents.valueAt(this.mActiveAgents.indexOf(agentInfo));
                                                } else {
                                                    agentInfo.label = resolveInfo.loadLabel(pm);
                                                    agentInfo.icon = resolveInfo.loadIcon(pm);
                                                    agentInfo.settings = getSettingsAttrs(pm, resolveInfo);
                                                }
                                                boolean directUnlock = resolveInfo.serviceInfo.directBootAware && agentInfo.settings.canUnlockProfile;
                                                if (directUnlock && DEBUG) {
                                                    str4 = TAG;
                                                    pm2 = pm;
                                                    pm = new StringBuilder();
                                                    pm.append("refreshAgentList: trustagent ");
                                                    pm.append(name);
                                                    pm.append("of user ");
                                                    pm.append(userInfo.id);
                                                    pm.append("can unlock user profile.");
                                                    Slog.d(str4, pm.toString());
                                                } else {
                                                    pm2 = pm;
                                                    ResolveInfo resolveInfo2 = resolveInfo;
                                                }
                                                if (this.mUserManager.isUserUnlockingOrUnlocked(userInfo.id) != null || directUnlock) {
                                                    boolean z2;
                                                    if (this.mStrongAuthTracker.canAgentsRunForUser(userInfo.id) == null) {
                                                        pm = this.mStrongAuthTracker.getStrongAuthForUser(userInfo.id);
                                                        if (pm != 8) {
                                                            if (pm == 1 && directUnlock) {
                                                                z2 = directUnlock;
                                                                if (agentInfo.agent == null) {
                                                                    agentInfo.agent = new TrustAgentWrapper(this.mContext, this, new Intent().setComponent(name), userInfo.getUserHandle());
                                                                }
                                                                if (this.mActiveAgents.contains(agentInfo) != null) {
                                                                    this.mActiveAgents.add(agentInfo);
                                                                } else {
                                                                    obsoleteAgents.remove(agentInfo);
                                                                }
                                                            } else if (DEBUG) {
                                                                str4 = TAG;
                                                                stringBuilder2 = new StringBuilder();
                                                                int flag = pm;
                                                                stringBuilder2.append("refreshAgentList: skipping user ");
                                                                stringBuilder2.append(userInfo.id);
                                                                stringBuilder2.append(": prevented by StrongAuthTracker = 0x");
                                                                stringBuilder2.append(Integer.toHexString(this.mStrongAuthTracker.getStrongAuthForUser(userInfo.id)));
                                                                Slog.d(str4, stringBuilder2.toString());
                                                            } else {
                                                                resolveInfos = resolveInfos2;
                                                                userInfos = userInfos2;
                                                                lockPatternUtils2 = lockPatternUtils;
                                                                pm = pm2;
                                                            }
                                                        }
                                                    }
                                                    z2 = directUnlock;
                                                    if (agentInfo.agent == null) {
                                                    }
                                                    if (this.mActiveAgents.contains(agentInfo) != null) {
                                                    }
                                                } else if (DEBUG != null) {
                                                    pm = TAG;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("refreshAgentList: skipping user ");
                                                    stringBuilder2.append(userInfo.id);
                                                    stringBuilder2.append("'s trust agent ");
                                                    stringBuilder2.append(name);
                                                    stringBuilder2.append(": FBE still locked and  the agent cannot unlock user profile.");
                                                    Slog.d(pm, stringBuilder2.toString());
                                                }
                                                resolveInfos = resolveInfos2;
                                                userInfos = userInfos2;
                                                lockPatternUtils2 = lockPatternUtils;
                                                pm = pm2;
                                            } else if (DEBUG) {
                                                resolveInfos2 = resolveInfos;
                                                str = TAG;
                                                userInfos2 = userInfos;
                                                StringBuilder stringBuilder5 = new StringBuilder();
                                                lockPatternUtils = lockPatternUtils2;
                                                stringBuilder5.append("refreshAgentList: skipping ");
                                                stringBuilder5.append(name.flattenToShortString());
                                                stringBuilder5.append(" u");
                                                stringBuilder5.append(userInfo.id);
                                                stringBuilder5.append(": not enabled by user");
                                                Slog.d(str, stringBuilder5.toString());
                                            }
                                            resolveInfos = resolveInfos2;
                                            userInfos = userInfos2;
                                            lockPatternUtils2 = lockPatternUtils;
                                        }
                                        userInfos2 = userInfos;
                                        lockPatternUtils = lockPatternUtils2;
                                        z = true;
                                    } else if (DEBUG) {
                                        str3 = TAG;
                                        StringBuilder stringBuilder6 = new StringBuilder();
                                        stringBuilder6.append("refreshAgentList: skipping user ");
                                        stringBuilder6.append(userInfo.id);
                                        stringBuilder6.append(": no agents enabled by user");
                                        Slog.d(str3, stringBuilder6.toString());
                                    }
                                } else if (DEBUG) {
                                    str3 = TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("refreshAgentList: skipping user ");
                                    stringBuilder3.append(userInfo.id);
                                    stringBuilder3.append(": no secure credential");
                                    Slog.d(str3, stringBuilder3.toString());
                                }
                            } else if (DEBUG) {
                                str3 = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("refreshAgentList: skipping user ");
                                stringBuilder3.append(userInfo.id);
                                stringBuilder3.append(": user not started");
                                Slog.d(str3, stringBuilder3.toString());
                            }
                        } else if (DEBUG) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("refreshAgentList: skipping user ");
                            stringBuilder3.append(userInfo.id);
                            stringBuilder3.append(": switchToByUser=false");
                            Slog.d(str3, stringBuilder3.toString());
                        }
                    }
                }
            }
            userInfos2 = userInfos;
            lockPatternUtils = lockPatternUtils2;
            boolean trustMayHaveChanged = false;
            for (int i = 0; i < obsoleteAgents.size(); i++) {
                AgentInfo info = (AgentInfo) obsoleteAgents.valueAt(i);
                if (userIdOrAll2 == -1 || userIdOrAll2 == info.userId) {
                    if (info.agent.isManagingTrust()) {
                        trustMayHaveChanged = true;
                    }
                    info.agent.destroy();
                    this.mActiveAgents.remove(info);
                }
            }
            if (trustMayHaveChanged) {
                if (userIdOrAll2 == -1) {
                    updateTrustAll();
                } else {
                    updateTrust(userIdOrAll2, 0);
                }
            }
        }
    }

    boolean isDeviceLockedInner(int userId) {
        boolean z;
        synchronized (this.mDeviceLockedForUser) {
            z = this.mDeviceLockedForUser.get(userId, true);
        }
        return z;
    }

    private void refreshDeviceLockedForUser(int userId) {
        List<UserInfo> userInfos;
        if (userId != -1 && userId < 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("refreshDeviceLockedForUser(userId=");
            stringBuilder.append(userId);
            stringBuilder.append("): Invalid user handle, must be USER_ALL or a specific user.");
            Log.e(str, stringBuilder.toString(), new Throwable("here"));
            userId = -1;
        }
        if (userId == -1) {
            userInfos = this.mUserManager.getUsers(true);
        } else {
            userInfos = new ArrayList();
            userInfos.add(this.mUserManager.getUserInfo(userId));
        }
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        for (int i = 0; i < userInfos.size(); i++) {
            UserInfo info = (UserInfo) userInfos.get(i);
            if (!(info == null || info.partial || !info.isEnabled() || info.guestToRemove || !info.supportsSwitchToByUser())) {
                int id = info.id;
                boolean secure = this.mLockPatternUtils.isSecure(id);
                boolean trusted = aggregateIsTrusted(id);
                boolean showingKeyguard = true;
                boolean fingerprintAuthenticated = false;
                if (this.mCurrentUser == id) {
                    synchronized (this.mUsersUnlockedByFingerprint) {
                        fingerprintAuthenticated = this.mUsersUnlockedByFingerprint.get(id, false);
                    }
                    try {
                        showingKeyguard = wm.isKeyguardLocked();
                    } catch (RemoteException e) {
                    }
                }
                boolean deviceLocked = secure && showingKeyguard && !trusted && !fingerprintAuthenticated;
                setDeviceLockedForUser(id, deviceLocked);
            }
        }
    }

    private void setDeviceLockedForUser(int userId, boolean locked) {
        boolean changed;
        synchronized (this.mDeviceLockedForUser) {
            changed = isDeviceLockedInner(userId) != locked;
            this.mDeviceLockedForUser.put(userId, locked);
        }
        if (changed) {
            dispatchDeviceLocked(userId, locked);
        }
    }

    private void dispatchDeviceLocked(int userId, boolean isLocked) {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo agent = (AgentInfo) this.mActiveAgents.valueAt(i);
            if (agent.userId == userId) {
                if (isLocked) {
                    agent.agent.onDeviceLocked();
                } else {
                    agent.agent.onDeviceUnlocked();
                }
            }
        }
    }

    void updateDevicePolicyFeatures() {
        boolean changed = false;
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = (AgentInfo) this.mActiveAgents.valueAt(i);
            if (info.agent.isConnected()) {
                info.agent.updateDevicePolicyFeatures();
                changed = true;
            }
        }
        if (changed) {
            this.mArchive.logDevicePolicyChanged();
        }
    }

    private void removeAgentsOfPackage(String packageName) {
        boolean trustMayHaveChanged = false;
        for (int i = this.mActiveAgents.size() - 1; i >= 0; i--) {
            AgentInfo info = (AgentInfo) this.mActiveAgents.valueAt(i);
            if (packageName.equals(info.component.getPackageName())) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Resetting agent ");
                stringBuilder.append(info.component.flattenToShortString());
                Log.i(str, stringBuilder.toString());
                if (info.agent.isManagingTrust()) {
                    trustMayHaveChanged = true;
                }
                info.agent.destroy();
                this.mActiveAgents.removeAt(i);
            }
        }
        if (trustMayHaveChanged) {
            updateTrustAll();
        }
    }

    public void resetAgent(ComponentName name, int userId) {
        boolean trustMayHaveChanged = false;
        for (int i = this.mActiveAgents.size() - 1; i >= 0; i--) {
            AgentInfo info = (AgentInfo) this.mActiveAgents.valueAt(i);
            if (name.equals(info.component) && userId == info.userId) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Resetting agent ");
                stringBuilder.append(info.component.flattenToShortString());
                Log.i(str, stringBuilder.toString());
                if (info.agent.isManagingTrust()) {
                    trustMayHaveChanged = true;
                }
                info.agent.destroy();
                this.mActiveAgents.removeAt(i);
            }
        }
        if (trustMayHaveChanged) {
            updateTrust(userId, 0);
        }
        refreshAgentList(userId);
    }

    /* JADX WARNING: Missing block: B:28:0x0074, code:
            if (r3 != null) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:29:0x0076, code:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:36:0x0083, code:
            if (r3 == null) goto L_0x0090;
     */
    /* JADX WARNING: Missing block: B:39:0x0088, code:
            if (r3 == null) goto L_0x0090;
     */
    /* JADX WARNING: Missing block: B:42:0x008d, code:
            if (r3 == null) goto L_0x0090;
     */
    /* JADX WARNING: Missing block: B:43:0x0090, code:
            if (r4 == null) goto L_0x00ad;
     */
    /* JADX WARNING: Missing block: B:44:0x0092, code:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("Error parsing : ");
            r6.append(r14.serviceInfo.packageName);
            android.util.Slog.w(r5, r6.toString(), r4);
     */
    /* JADX WARNING: Missing block: B:45:0x00ac, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:46:0x00ad, code:
            if (r1 != null) goto L_0x00b0;
     */
    /* JADX WARNING: Missing block: B:47:0x00af, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:49:0x00b6, code:
            if (r1.indexOf(47) >= 0) goto L_0x00d0;
     */
    /* JADX WARNING: Missing block: B:50:0x00b8, code:
            r0 = new java.lang.StringBuilder();
            r0.append(r14.serviceInfo.packageName);
            r0.append(com.android.server.slice.SliceClientPermissions.SliceAuthority.DELIMITER);
            r0.append(r1);
            r1 = r0.toString();
     */
    /* JADX WARNING: Missing block: B:52:0x00d9, code:
            return new com.android.server.trust.TrustManagerService.SettingsAttrs(android.content.ComponentName.unflattenFromString(r1), r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private SettingsAttrs getSettingsAttrs(PackageManager pm, ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        boolean canUnlockProfile = false;
        XmlResourceParser parser = null;
        Exception caughtException = null;
        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm, "android.service.trust.trustagent");
            if (parser == null) {
                Slog.w(TAG, "Can't find android.service.trust.trustagent meta-data");
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            Resources res = pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            while (true) {
                int next = parser.next();
                int type = next;
                if (next == 1 || type == 2) {
                }
            }
            if ("trust-agent".equals(parser.getName())) {
                TypedArray sa = res.obtainAttributes(attrs, R.styleable.TrustAgent);
                cn = sa.getString(2);
                canUnlockProfile = sa.getBoolean(3, false);
                sa.recycle();
            } else {
                Slog.w(TAG, "Meta-data does not start with trust-agent tag");
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
        } catch (Exception e) {
            caughtException = e;
        } catch (Exception e2) {
            caughtException = e2;
        } catch (Exception e22) {
            caughtException = e22;
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    private void maybeEnableFactoryTrustAgents(LockPatternUtils utils, int userId) {
        int i = userId;
        boolean shouldUseDefaultAgent = false;
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "trust_agents_initialized", 0, i) == 0) {
            ComponentName mGoogleSmartLock = new ComponentName("com.google.android.gms", "com.google.android.gms.auth.trustagent.GoogleTrustAgent");
            List<ResolveInfo> resolveInfos = resolveAllowedTrustAgents(this.mContext.getPackageManager(), i);
            ComponentName defaultAgent = getDefaultFactoryTrustAgent(this.mContext);
            if (defaultAgent != null) {
                shouldUseDefaultAgent = true;
            }
            ArraySet<ComponentName> discoveredAgents = new ArraySet();
            if (shouldUseDefaultAgent) {
                discoveredAgents.add(defaultAgent);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Enabling ");
                stringBuilder.append(defaultAgent);
                stringBuilder.append(" because it is a default agent.");
                Log.i(str, stringBuilder.toString());
            } else {
                for (ResolveInfo resolveInfo : resolveInfos) {
                    ComponentName componentName = getComponentName(resolveInfo);
                    if ((resolveInfo.serviceInfo.applicationInfo.flags & 1) == 0) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Leaving agent ");
                        stringBuilder2.append(componentName);
                        stringBuilder2.append(" disabled because package is not a system package.");
                        Log.i(str2, stringBuilder2.toString());
                    } else {
                        discoveredAgents.add(componentName);
                    }
                }
            }
            if (discoveredAgents.contains(mGoogleSmartLock) && (this.mCust == null || !this.mCust.isShowSmartLcok())) {
                discoveredAgents.remove(mGoogleSmartLock);
            }
            List<ComponentName> previouslyEnabledAgents = utils.getEnabledTrustAgents(userId);
            if (previouslyEnabledAgents != null) {
                discoveredAgents.addAll(previouslyEnabledAgents);
            }
            utils.setEnabledTrustAgents(discoveredAgents, i);
            Secure.putIntForUser(this.mContext.getContentResolver(), "trust_agents_initialized", 1, i);
        }
    }

    private static ComponentName getDefaultFactoryTrustAgent(Context context) {
        String defaultTrustAgent = context.getResources().getString(17039791);
        if (TextUtils.isEmpty(defaultTrustAgent)) {
            return null;
        }
        return ComponentName.unflattenFromString(defaultTrustAgent);
    }

    private List<ResolveInfo> resolveAllowedTrustAgents(PackageManager pm, int userId) {
        List<ResolveInfo> resolveInfos = pm.queryIntentServicesAsUser(TRUST_AGENT_INTENT, 786560, userId);
        ArrayList<ResolveInfo> allowedAgents = new ArrayList(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo != null) {
                if (resolveInfo.serviceInfo.applicationInfo != null) {
                    if (pm.checkPermission(PERMISSION_PROVIDE_AGENT, resolveInfo.serviceInfo.packageName) != 0) {
                        ComponentName name = getComponentName(resolveInfo);
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Skipping agent ");
                        stringBuilder.append(name);
                        stringBuilder.append(" because package does not have permission ");
                        stringBuilder.append(PERMISSION_PROVIDE_AGENT);
                        stringBuilder.append(".");
                        Log.w(str, stringBuilder.toString());
                    } else {
                        allowedAgents.add(resolveInfo);
                    }
                }
            }
        }
        return allowedAgents;
    }

    private boolean aggregateIsTrusted(int userId) {
        if (!this.mStrongAuthTracker.isTrustAllowedForUser(userId)) {
            return false;
        }
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = (AgentInfo) this.mActiveAgents.valueAt(i);
            if (info.userId == userId && info.agent.isTrusted()) {
                return true;
            }
        }
        return false;
    }

    private boolean aggregateIsTrustManaged(int userId) {
        if (!this.mStrongAuthTracker.isTrustAllowedForUser(userId)) {
            return false;
        }
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = (AgentInfo) this.mActiveAgents.valueAt(i);
            if (info.userId == userId && info.agent.isManagingTrust()) {
                return true;
            }
        }
        return false;
    }

    private void dispatchUnlockAttempt(boolean successful, int userId) {
        if (successful) {
            this.mStrongAuthTracker.allowTrustFromUnlock(userId);
        }
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = (AgentInfo) this.mActiveAgents.valueAt(i);
            if (info.userId == userId) {
                info.agent.onUnlockAttempt(successful);
            }
        }
    }

    private void dispatchUnlockLockout(int timeoutMs, int userId) {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = (AgentInfo) this.mActiveAgents.valueAt(i);
            if (info.userId == userId) {
                info.agent.onUnlockLockout(timeoutMs);
            }
        }
    }

    private void addListener(ITrustListener listener) {
        if (listener == null) {
            Log.i(TAG, "addListener, listener is null, just return");
            return;
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            if (((ITrustListener) this.mTrustListeners.get(i)).asBinder() != listener.asBinder()) {
                i++;
            } else {
                return;
            }
        }
        this.mTrustListeners.add(listener);
        updateTrustAll();
    }

    private void removeListener(ITrustListener listener) {
        if (listener == null) {
            Log.i(TAG, "removeListener, listener is null, just return");
            return;
        }
        for (int i = 0; i < this.mTrustListeners.size(); i++) {
            if (((ITrustListener) this.mTrustListeners.get(i)).asBinder() == listener.asBinder()) {
                this.mTrustListeners.remove(i);
                return;
            }
        }
    }

    private void dispatchOnTrustChanged(boolean enabled, int userId, int flags) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onTrustChanged(");
            stringBuilder.append(enabled);
            stringBuilder.append(", ");
            stringBuilder.append(userId);
            stringBuilder.append(", 0x");
            stringBuilder.append(Integer.toHexString(flags));
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
        }
        if (!enabled) {
            flags = 0;
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                ((ITrustListener) this.mTrustListeners.get(i)).onTrustChanged(enabled, userId, flags);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i++;
        }
    }

    private void dispatchOnTrustManagedChanged(boolean managed, int userId) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onTrustManagedChanged(");
            stringBuilder.append(managed);
            stringBuilder.append(", ");
            stringBuilder.append(userId);
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                ((ITrustListener) this.mTrustListeners.get(i)).onTrustManagedChanged(managed, userId);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i++;
        }
    }

    private void dispatchOnTrustError(CharSequence message) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onTrustError(");
            stringBuilder.append(message);
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                ((ITrustListener) this.mTrustListeners.get(i)).onTrustError(message);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i++;
        }
    }

    public void onStartUser(int userId) {
        this.mHandler.obtainMessage(7, userId, 0, null).sendToTarget();
    }

    public void onCleanupUser(int userId) {
        this.mHandler.obtainMessage(8, userId, 0, null).sendToTarget();
    }

    public void onSwitchUser(int userId) {
        this.mHandler.obtainMessage(9, userId, 0, null).sendToTarget();
    }

    public void onUnlockUser(int userId) {
        this.mHandler.obtainMessage(11, userId, 0, null).sendToTarget();
    }

    public void onStopUser(int userId) {
        this.mHandler.obtainMessage(12, userId, 0, null).sendToTarget();
    }

    /* JADX WARNING: Missing block: B:9:0x0014, code:
            r1 = r4.mLockPatternUtils.isTrustUsuallyManaged(r5);
            r2 = r4.mTrustUsuallyManagedForUser;
     */
    /* JADX WARNING: Missing block: B:10:0x001c, code:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            r0 = r4.mTrustUsuallyManagedForUser.indexOfKey(r5);
     */
    /* JADX WARNING: Missing block: B:13:0x0023, code:
            if (r0 < 0) goto L_0x002d;
     */
    /* JADX WARNING: Missing block: B:14:0x0025, code:
            r3 = r4.mTrustUsuallyManagedForUser.valueAt(r0);
     */
    /* JADX WARNING: Missing block: B:15:0x002b, code:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:16:0x002c, code:
            return r3;
     */
    /* JADX WARNING: Missing block: B:17:0x002d, code:
            r4.mTrustUsuallyManagedForUser.put(r5, r1);
     */
    /* JADX WARNING: Missing block: B:18:0x0032, code:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:19:0x0033, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isTrustUsuallyManagedInternal(int userId) {
        synchronized (this.mTrustUsuallyManagedForUser) {
            int i = this.mTrustUsuallyManagedForUser.indexOfKey(userId);
            if (i >= 0) {
                boolean valueAt = this.mTrustUsuallyManagedForUser.valueAt(i);
                return valueAt;
            }
        }
    }

    private int resolveProfileParent(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            UserInfo parent = this.mUserManager.getProfileParent(userId);
            if (parent != null) {
                int identifier = parent.getUserHandle().getIdentifier();
                return identifier;
            }
            Binder.restoreCallingIdentity(identity);
            return userId;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
