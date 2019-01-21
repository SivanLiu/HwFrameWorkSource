package com.android.server.am;

import android.app.AppGlobals;
import android.app.Dialog;
import android.app.IStopUserCallback;
import android.app.IUserSwitchObserver;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.encrypt.ISDCardCryptedHelper;
import android.hwtheme.HwThemeManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IProgressListener;
import android.os.IUserManager.Stub;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.provider.Settings.System;
import android.util.ArraySet;
import android.util.Flog;
import android.util.IntArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimingsTraceLog;
import android.util.proto.ProtoOutputStream;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.FgThread;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.pm.UserManagerService;
import com.android.server.power.IHwShutdownThread;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class UserController implements Callback {
    static final int CONTINUE_USER_SWITCH_MSG = 20;
    static final int FOREGROUND_PROFILE_CHANGED_MSG = 70;
    static final int REPORT_LOCKED_BOOT_COMPLETE_MSG = 110;
    static final int REPORT_USER_SWITCH_COMPLETE_MSG = 80;
    static final int REPORT_USER_SWITCH_MSG = 10;
    private static final int SCREEN_STATE_FLAG_PASSWORD = 2;
    static final int START_PROFILES_MSG = 40;
    static final int START_USER_SWITCH_FG_MSG = 120;
    static final int START_USER_SWITCH_UI_MSG = 1000;
    static final int SYSTEM_USER_CURRENT_MSG = 60;
    static final int SYSTEM_USER_START_MSG = 50;
    static final int SYSTEM_USER_UNLOCK_MSG = 100;
    private static final String TAG = "ActivityManager";
    private static final int UNLOCK_TYPE_VALUE = 1;
    private static final int USER_SWITCH_CALLBACKS_TIMEOUT_MS = 5000;
    static final int USER_SWITCH_CALLBACKS_TIMEOUT_MSG = 90;
    static final int USER_SWITCH_TIMEOUT_MS = 3000;
    static final int USER_SWITCH_TIMEOUT_MSG = 30;
    long SwitchUser_Time;
    boolean isColdStart;
    @GuardedBy("mLock")
    private volatile ArraySet<String> mCurWaitingUserSwitchCallbacks;
    @GuardedBy("mLock")
    private int[] mCurrentProfileIds;
    @GuardedBy("mLock")
    private volatile int mCurrentUserId;
    private final Handler mHandler;
    boolean mHaveTryCloneProUserUnlock;
    final Injector mInjector;
    private boolean mIsSupportISec;
    private final Object mLock;
    private final LockPatternUtils mLockPatternUtils;
    int mMaxRunningUsers;
    @GuardedBy("mLock")
    private int[] mStartedUserArray;
    @GuardedBy("mLock")
    private final SparseArray<UserState> mStartedUsers;
    @GuardedBy("mLock")
    private String mSwitchingFromSystemUserMessage;
    @GuardedBy("mLock")
    private String mSwitchingToSystemUserMessage;
    @GuardedBy("mLock")
    private volatile int mTargetUserId;
    @GuardedBy("mLock")
    private ArraySet<String> mTimeoutUserSwitchCallbacks;
    private final Handler mUiHandler;
    @GuardedBy("mLock")
    private final ArrayList<Integer> mUserLru;
    @GuardedBy("mLock")
    private final SparseIntArray mUserProfileGroupIds;
    private final RemoteCallbackList<IUserSwitchObserver> mUserSwitchObservers;
    boolean mUserSwitchUiEnabled;
    boolean misHiddenSpaceSwitch;

    @VisibleForTesting
    static class Injector {
        final ActivityManagerService mService;
        private UserManagerService mUserManager;
        private UserManagerInternal mUserManagerInternal;

        Injector(ActivityManagerService service) {
            this.mService = service;
        }

        protected Handler getHandler(Callback callback) {
            return new Handler(this.mService.mHandlerThread.getLooper(), callback);
        }

        protected Handler getUiHandler(Callback callback) {
            return new Handler(this.mService.mUiHandler.getLooper(), callback);
        }

        protected Context getContext() {
            return this.mService.mContext;
        }

        protected LockPatternUtils getLockPatternUtils() {
            return new LockPatternUtils(getContext());
        }

        protected int broadcastIntent(Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle bOptions, boolean ordered, boolean sticky, int callingPid, int callingUid, int userId) {
            int broadcastIntentLocked;
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    broadcastIntentLocked = this.mService.broadcastIntentLocked(null, null, intent, resolvedType, resultTo, resultCode, resultData, resultExtras, requiredPermissions, appOp, bOptions, ordered, sticky, callingPid, callingUid, userId);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            return broadcastIntentLocked;
        }

        int checkCallingPermission(String permission) {
            return this.mService.checkCallingPermission(permission);
        }

        WindowManagerService getWindowManager() {
            return this.mService.mWindowManager;
        }

        void activityManagerOnUserStopped(int userId) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.onUserStoppedLocked(userId);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        void systemServiceManagerCleanupUser(int userId) {
            this.mService.mSystemServiceManager.cleanupUser(userId);
        }

        protected UserManagerService getUserManager() {
            if (this.mUserManager == null) {
                this.mUserManager = (UserManagerService) Stub.asInterface(ServiceManager.getService("user"));
            }
            return this.mUserManager;
        }

        UserManagerInternal getUserManagerInternal() {
            if (this.mUserManagerInternal == null) {
                this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            }
            return this.mUserManagerInternal;
        }

        KeyguardManager getKeyguardManager() {
            return (KeyguardManager) this.mService.mContext.getSystemService(KeyguardManager.class);
        }

        void batteryStatsServiceNoteEvent(int code, String name, int uid) {
            this.mService.mBatteryStatsService.noteEvent(code, name, uid);
        }

        boolean isRuntimeRestarted() {
            return this.mService.mSystemServiceManager.isRuntimeRestarted();
        }

        SystemServiceManager getSystemServiceManager() {
            return this.mService.mSystemServiceManager;
        }

        boolean isFirstBootOrUpgrade() {
            IPackageManager pm = AppGlobals.getPackageManager();
            try {
                return pm.isFirstBoot() || pm.isUpgrade();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        void sendPreBootBroadcast(int userId, boolean quiet, Runnable onFinish) {
            final Runnable runnable = onFinish;
            new PreBootBroadcaster(this.mService, userId, null, quiet) {
                public void onFinished() {
                    runnable.run();
                }
            }.sendNext();
        }

        void activityManagerForceStopPackage(int userId, String reason) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.forceStopPackageLocked(null, -1, false, false, true, false, false, userId, reason);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        int checkComponentPermission(String permission, int pid, int uid, int owningUid, boolean exported) {
            return this.mService.checkComponentPermission(permission, pid, uid, owningUid, exported);
        }

        protected void startHomeActivity(int userId, String reason) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.startHomeActivityLocked(userId, reason);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        void updateUserConfiguration() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.updateUserConfigurationLocked();
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        void clearBroadcastQueueForUser(int userId) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.clearBroadcastQueueForUserLocked(userId);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        void loadUserRecents(int userId) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.getRecentTasks().loadUserRecentsLocked(userId);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        void startPersistentApps(int matchFlags) {
            this.mService.startPersistentApps(matchFlags);
        }

        void installEncryptionUnawareProviders(int userId) {
            this.mService.installEncryptionUnawareProviders(userId);
        }

        void showUserSwitchingDialog(UserInfo fromUser, UserInfo toUser, String switchingFromSystemUserMessage, String switchingToSystemUserMessage) {
            Dialog carUserSwitchingDialog;
            if (this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                carUserSwitchingDialog = new CarUserSwitchingDialog(this.mService, this.mService.mContext, fromUser, toUser, true, switchingFromSystemUserMessage, switchingToSystemUserMessage);
            } else {
                carUserSwitchingDialog = new UserSwitchingDialog(this.mService, this.mService.mContext, fromUser, toUser, true, switchingFromSystemUserMessage, switchingToSystemUserMessage);
            }
            d.show();
            Window window = d.getWindow();
            LayoutParams lp = window.getAttributes();
            lp.width = -1;
            window.setAttributes(lp);
        }

        void updatePersistentConfiguration(Configuration config) {
            this.mService.updatePersistentConfiguration(config);
        }

        void reportGlobalUsageEventLocked(int event) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.reportGlobalUsageEventLocked(event);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        void reportCurWakefulnessUsageEvent() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.reportCurWakefulnessUsageEventLocked();
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        void stackSupervisorRemoveUser(int userId) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.mStackSupervisor.removeUserLocked(userId);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        protected boolean stackSupervisorSwitchUser(int userId, UserState uss) {
            boolean switchUserLocked;
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    switchUserLocked = this.mService.mStackSupervisor.switchUserLocked(userId, uss);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            return switchUserLocked;
        }

        protected void stackSupervisorResumeFocusedStackTopActivity() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        protected void clearAllLockedTasks(String reason) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.getLockTaskController().clearLockedTasks(reason);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        protected boolean isCallerRecents(int callingUid) {
            return this.mService.getRecentTasks().isCallerRecents(callingUid);
        }

        private boolean shouldSkipKeyguard(UserInfo first, UserInfo second) {
            return this.mService.isHiddenSpaceSwitch(first, second) && getWindowManager().isKeyguardLocked();
        }

        void cleanAppForHiddenSpace() {
            this.mService.cleanAppForHiddenSpace();
        }
    }

    private static class UserProgressListener extends IProgressListener.Stub {
        private volatile long mUnlockStarted;

        private UserProgressListener() {
        }

        /* synthetic */ UserProgressListener(AnonymousClass1 x0) {
            this();
        }

        public void onStarted(int id, Bundle extras) throws RemoteException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Started unlocking user ");
            stringBuilder.append(id);
            Slog.d("ActivityManager", stringBuilder.toString());
            this.mUnlockStarted = SystemClock.uptimeMillis();
        }

        public void onProgress(int id, int progress, Bundle extras) throws RemoteException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unlocking user ");
            stringBuilder.append(id);
            stringBuilder.append(" progress ");
            stringBuilder.append(progress);
            Slog.d("ActivityManager", stringBuilder.toString());
        }

        public void onFinished(int id, Bundle extras) throws RemoteException {
            long unlockTime = SystemClock.uptimeMillis() - this.mUnlockStarted;
            if (id == 0) {
                new TimingsTraceLog("SystemServerTiming", 524288).logDuration("SystemUserUnlock", unlockTime);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unlocking user ");
            stringBuilder.append(id);
            stringBuilder.append(" took ");
            stringBuilder.append(unlockTime);
            stringBuilder.append(" ms");
            Slog.d("ActivityManager", stringBuilder.toString());
        }
    }

    UserController(ActivityManagerService service) {
        this(new Injector(service));
    }

    @VisibleForTesting
    UserController(Injector injector) {
        this.isColdStart = false;
        this.misHiddenSpaceSwitch = false;
        this.mHaveTryCloneProUserUnlock = false;
        this.mLock = new Object();
        this.mIsSupportISec = SystemProperties.getBoolean("ro.config.support_iudf", false);
        this.mCurrentUserId = 0;
        this.mTargetUserId = -10000;
        this.mStartedUsers = new SparseArray();
        this.mUserLru = new ArrayList();
        this.mStartedUserArray = new int[]{0};
        this.mCurrentProfileIds = new int[0];
        this.mUserProfileGroupIds = new SparseIntArray();
        this.mUserSwitchObservers = new RemoteCallbackList();
        this.mUserSwitchUiEnabled = true;
        this.mInjector = injector;
        this.mHandler = this.mInjector.getHandler(this);
        this.mUiHandler = this.mInjector.getUiHandler(this);
        UserState uss = new UserState(UserHandle.SYSTEM);
        uss.mUnlockProgress.addListener(new UserProgressListener());
        this.mStartedUsers.put(0, uss);
        this.mUserLru.add(Integer.valueOf(0));
        this.mLockPatternUtils = this.mInjector.getLockPatternUtils();
        updateStartedUserArrayLU();
    }

    void finishUserSwitch(UserState uss) {
        long startedTime = SystemClock.elapsedRealtime();
        finishUserBoot(uss);
        startProfiles();
        synchronized (this.mLock) {
            stopRunningUsersLU(this.mMaxRunningUsers);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("_StartUser finishUserSwitch userid:");
        stringBuilder.append(uss.mHandle.getIdentifier());
        stringBuilder.append(" cost ");
        stringBuilder.append(SystemClock.elapsedRealtime() - startedTime);
        stringBuilder.append(" ms");
        Slog.i("ActivityManager", stringBuilder.toString());
    }

    List<Integer> getRunningUsersLU() {
        ArrayList<Integer> runningUsers = new ArrayList();
        Iterator it = this.mUserLru.iterator();
        while (it.hasNext()) {
            Integer userId = (Integer) it.next();
            UserState uss = (UserState) this.mStartedUsers.get(userId.intValue());
            if (uss != null) {
                if (uss.state != 4) {
                    if (uss.state != 5) {
                        if (userId.intValue() != 0 || !UserInfo.isSystemOnly(userId.intValue())) {
                            runningUsers.add(userId);
                        }
                    }
                }
            }
        }
        return runningUsers;
    }

    void stopRunningUsersLU(int maxRunningUsers) {
        List<Integer> currentlyRunning = getRunningUsersLU();
        Iterator<Integer> iterator = currentlyRunning.iterator();
        while (currentlyRunning.size() > maxRunningUsers && iterator.hasNext()) {
            Integer userId = (Integer) iterator.next();
            if (userId.intValue() != 0) {
                if (userId.intValue() != this.mCurrentUserId) {
                    if (stopUsersLU(userId.intValue(), false, null) == 0) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    boolean canStartMoreUsers() {
        boolean z;
        synchronized (this.mLock) {
            z = getRunningUsersLU().size() < this.mMaxRunningUsers;
        }
        return z;
    }

    private void finishUserBoot(UserState uss) {
        finishUserBoot(uss, null);
    }

    /* JADX WARNING: Missing block: B:13:0x0038, code skipped:
            if (r2.setState(0, 1) == false) goto L_0x00c8;
     */
    /* JADX WARNING: Missing block: B:14:0x003a, code skipped:
            r1.mInjector.getUserManagerInternal().setUserState(r15, r2.state);
     */
    /* JADX WARNING: Missing block: B:15:0x0045, code skipped:
            if (r15 != 0) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:17:0x004d, code skipped:
            if (r1.mInjector.isRuntimeRestarted() != false) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:19:0x0055, code skipped:
            if (r1.mInjector.isFirstBootOrUpgrade() != false) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:20:0x0057, code skipped:
            r0 = (int) (android.os.SystemClock.elapsedRealtime() / 1000);
            com.android.internal.logging.MetricsLogger.histogram(r1.mInjector.getContext(), "framework_locked_boot_completed", r0);
     */
    /* JADX WARNING: Missing block: B:21:0x006e, code skipped:
            if (r0 <= START_USER_SWITCH_FG_MSG) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:22:0x0070, code skipped:
            r6 = new java.lang.StringBuilder();
            r6.append("finishUserBoot took too long. uptimeSeconds=");
            r6.append(r0);
            android.util.Slog.wtf("SystemServerTiming", r6.toString());
     */
    /* JADX WARNING: Missing block: B:23:0x0086, code skipped:
            r1.mHandler.sendMessage(r1.mHandler.obtainMessage(110, r15, 0));
            r0 = new android.content.Intent("android.intent.action.LOCKED_BOOT_COMPLETED", null);
            r0.putExtra("android.intent.extra.user_handle", r15);
            r0.addFlags(150994944);
            r18 = r15;
            r1.mInjector.broadcastIntent(r0, null, r21, 0, null, null, new java.lang.String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r18);
     */
    /* JADX WARNING: Missing block: B:24:0x00c8, code skipped:
            r18 = r15;
     */
    /* JADX WARNING: Missing block: B:25:0x00ca, code skipped:
            r4 = r18;
     */
    /* JADX WARNING: Missing block: B:26:0x00d6, code skipped:
            if (r1.mInjector.getUserManager().isManagedProfile(r4) != false) goto L_0x00e9;
     */
    /* JADX WARNING: Missing block: B:28:0x00e2, code skipped:
            if (r1.mInjector.getUserManager().isClonedProfile(r4) == false) goto L_0x00e5;
     */
    /* JADX WARNING: Missing block: B:29:0x00e5, code skipped:
            maybeUnlockUser(r4);
     */
    /* JADX WARNING: Missing block: B:30:0x00e9, code skipped:
            r0 = r1.mInjector.getUserManager().getProfileParent(r4);
     */
    /* JADX WARNING: Missing block: B:31:0x00f3, code skipped:
            if (r0 == null) goto L_0x0127;
     */
    /* JADX WARNING: Missing block: B:33:0x00fc, code skipped:
            if (isUserRunning(r0.id, 4) == false) goto L_0x0127;
     */
    /* JADX WARNING: Missing block: B:34:0x00fe, code skipped:
            r5 = new java.lang.StringBuilder();
            r5.append("User ");
            r5.append(r4);
            r5.append(" (parent ");
            r5.append(r0.id);
            r5.append("): attempting unlock because parent is unlocked");
            android.util.Slog.d("ActivityManager", r5.toString());
            maybeUnlockUser(r4);
     */
    /* JADX WARNING: Missing block: B:35:0x0127, code skipped:
            if (r0 != null) goto L_0x012c;
     */
    /* JADX WARNING: Missing block: B:36:0x0129, code skipped:
            r3 = "<null>";
     */
    /* JADX WARNING: Missing block: B:37:0x012c, code skipped:
            r3 = java.lang.String.valueOf(r0.id);
     */
    /* JADX WARNING: Missing block: B:38:0x0132, code skipped:
            r6 = new java.lang.StringBuilder();
            r6.append("User ");
            r6.append(r4);
            r6.append(" (parent ");
            r6.append(r3);
            r6.append("): delaying unlock because parent is locked");
            android.util.Slog.d("ActivityManager", r6.toString());
     */
    /* JADX WARNING: Missing block: B:39:0x0156, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void finishUserBoot(UserState uss, IIntentReceiver resultTo) {
        Throwable th;
        int i;
        UserState userState = uss;
        int userId = userState.mHandle.getIdentifier();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Finishing user boot ");
        stringBuilder.append(userId);
        Slog.d("ActivityManager", stringBuilder.toString());
        synchronized (this.mLock) {
            try {
                if (this.mStartedUsers.get(userId) != userState) {
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        i = userId;
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
                i = userId;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    private void finishUserUnlocking(UserState uss) {
        int userId = uss.mHandle.getIdentifier();
        if (StorageManager.isUserKeyUnlocked(userId)) {
            synchronized (this.mLock) {
                if (this.mStartedUsers.get(userId) == uss) {
                    if (uss.state == 1) {
                        uss.mUnlockProgress.start();
                        uss.mUnlockProgress.setProgress(5, this.mInjector.getContext().getString(17039579));
                        FgThread.getHandler().post(new -$$Lambda$UserController$o6oQFjGYYIfx-I94cSakTLPLt6s(this, userId, uss));
                        return;
                    }
                }
            }
        }
    }

    public static /* synthetic */ void lambda$finishUserUnlocking$0(UserController userController, int userId, UserState uss) {
        if (StorageManager.isUserKeyUnlocked(userId)) {
            userController.mInjector.getUserManager().onBeforeUnlockUser(userId);
            synchronized (userController.mLock) {
                if (uss.setState(1, 2)) {
                    userController.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                    uss.mUnlockProgress.setProgress(20);
                    userController.mHandler.obtainMessage(100, userId, 0, uss).sendToTarget();
                    return;
                }
                return;
            }
        }
        Slog.w("ActivityManager", "User key got locked unexpectedly, leaving user locked.");
    }

    /* JADX WARNING: Missing block: B:22:0x0033, code skipped:
            r1.mInjector.getUserManagerInternal().setUserState(r15, r2.state);
            r2.mUnlockProgress.finish();
     */
    /* JADX WARNING: Missing block: B:23:0x0043, code skipped:
            if (r15 != 0) goto L_0x004c;
     */
    /* JADX WARNING: Missing block: B:24:0x0045, code skipped:
            r1.mInjector.startPersistentApps(262144);
     */
    /* JADX WARNING: Missing block: B:25:0x004c, code skipped:
            r1.mInjector.installEncryptionUnawareProviders(r15);
            r0 = new android.content.Intent("android.intent.action.USER_UNLOCKED");
            r0.putExtra("android.intent.extra.user_handle", r15);
            r0.addFlags(1342177280);
            r19 = r15;
            r1.mInjector.broadcastIntent(r0, null, null, 0, null, null, null, -1, null, false, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r19);
            r4 = r19;
     */
    /* JADX WARNING: Missing block: B:26:0x008b, code skipped:
            if (getUserInfo(r4).isManagedProfile() == false) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:27:0x008d, code skipped:
            r3 = r1.mInjector.getUserManager().getProfileParent(r4);
     */
    /* JADX WARNING: Missing block: B:28:0x0097, code skipped:
            if (r3 == null) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:29:0x0099, code skipped:
            r5 = new android.content.Intent("android.intent.action.MANAGED_PROFILE_UNLOCKED");
            r5.putExtra("android.intent.extra.USER", android.os.UserHandle.of(r4));
            r5.addFlags(1342177280);
            r1.mInjector.broadcastIntent(r5, null, null, 0, null, null, null, -1, null, false, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r3.id);
     */
    /* JADX WARNING: Missing block: B:30:0x00d3, code skipped:
            r3 = getUserInfo(r4);
     */
    /* JADX WARNING: Missing block: B:31:0x00df, code skipped:
            if (java.util.Objects.equals(r3.lastLoggedInFingerprint, android.os.Build.FINGERPRINT) != false) goto L_0x010b;
     */
    /* JADX WARNING: Missing block: B:32:0x00e1, code skipped:
            r6 = false;
     */
    /* JADX WARNING: Missing block: B:33:0x00e6, code skipped:
            if (r3.isManagedProfile() != false) goto L_0x00f0;
     */
    /* JADX WARNING: Missing block: B:35:0x00ec, code skipped:
            if (r3.isClonedProfile() == false) goto L_0x00ff;
     */
    /* JADX WARNING: Missing block: B:37:0x00f2, code skipped:
            if (r2.tokenProvided == false) goto L_0x00fe;
     */
    /* JADX WARNING: Missing block: B:39:0x00fa, code skipped:
            if (r1.mLockPatternUtils.isSeparateProfileChallengeEnabled(r4) != false) goto L_0x00ff;
     */
    /* JADX WARNING: Missing block: B:40:0x00fe, code skipped:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:41:0x00ff, code skipped:
            r1.mInjector.sendPreBootBroadcast(r4, r6, new com.android.server.am.-$$Lambda$UserController$d0zeElfogOIugnQQLWhCzumk53k(r1, r2));
     */
    /* JADX WARNING: Missing block: B:42:0x010b, code skipped:
            finishUserUnlockedCompleted(r35);
     */
    /* JADX WARNING: Missing block: B:43:0x010e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void finishUserUnlocked(UserState uss) {
        Throwable th;
        int i;
        UserState userState = uss;
        int userId = userState.mHandle.getIdentifier();
        if (StorageManager.isUserKeyUnlocked(userId)) {
            synchronized (this.mLock) {
                try {
                    if (this.mStartedUsers.get(userState.mHandle.getIdentifier()) != userState) {
                        try {
                        } catch (Throwable th2) {
                            th = th2;
                            i = userId;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    } else if (!userState.setState(2, 3)) {
                    }
                } catch (Throwable th4) {
                    th = th4;
                    i = userId;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0023, code skipped:
            r0 = getUserInfo(r15);
     */
    /* JADX WARNING: Missing block: B:13:0x0027, code skipped:
            if (r0 != null) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:14:0x0029, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:16:0x002e, code skipped:
            if (android.os.storage.StorageManager.isUserKeyUnlocked(r15) != false) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:17:0x0030, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:0x0031, code skipped:
            r1.mInjector.getUserManager().onUserLoggedIn(r15);
     */
    /* JADX WARNING: Missing block: B:19:0x003e, code skipped:
            if (r0.isInitialized() != false) goto L_0x008b;
     */
    /* JADX WARNING: Missing block: B:20:0x0040, code skipped:
            if (r15 == 0) goto L_0x008b;
     */
    /* JADX WARNING: Missing block: B:21:0x0042, code skipped:
            r4 = new java.lang.StringBuilder();
            r4.append("Initializing user #");
            r4.append(r15);
            android.util.Slog.d("ActivityManager", r4.toString());
            r14 = new android.content.Intent("android.intent.action.USER_INITIALIZE");
            r14.addFlags(285212672);
            r19 = r14;
            r20 = r15;
            r1.mInjector.broadcastIntent(r14, null, new com.android.server.am.UserController.AnonymousClass1(r1), 0, null, null, null, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r20);
     */
    /* JADX WARNING: Missing block: B:22:0x008b, code skipped:
            r20 = r15;
     */
    /* JADX WARNING: Missing block: B:23:0x008d, code skipped:
            r4 = new java.lang.StringBuilder();
            r4.append("Sending BOOT_COMPLETE user #");
            r15 = r20;
            r4.append(r15);
            android.util.Slog.i("ActivityManager", r4.toString());
     */
    /* JADX WARNING: Missing block: B:24:0x00a5, code skipped:
            if (r15 != 0) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:26:0x00ad, code skipped:
            if (r1.mInjector.isRuntimeRestarted() != false) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:28:0x00b5, code skipped:
            if (r1.mInjector.isFirstBootOrUpgrade() != false) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:29:0x00b7, code skipped:
            com.android.internal.logging.MetricsLogger.histogram(r1.mInjector.getContext(), "framework_boot_completed", (int) (android.os.SystemClock.elapsedRealtime() / 1000));
     */
    /* JADX WARNING: Missing block: B:30:0x00ca, code skipped:
            r14 = new android.content.Intent("android.intent.action.BOOT_COMPLETED", null);
            r14.putExtra("android.intent.extra.user_handle", r15);
            r14.addFlags(150994944);
            r4 = r14;
            r19 = r14;
            r1.mInjector.broadcastIntent(r4, null, new com.android.server.am.UserController.AnonymousClass2(r1), 0, null, null, new java.lang.String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r15);
     */
    /* JADX WARNING: Missing block: B:31:0x0106, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void finishUserUnlockedCompleted(UserState uss) {
        Throwable th;
        int i;
        UserState userState = uss;
        int userId = userState.mHandle.getIdentifier();
        synchronized (this.mLock) {
            try {
                if (this.mStartedUsers.get(userState.mHandle.getIdentifier()) != userState) {
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        i = userId;
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
                i = userId;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    int restartUser(int userId, final boolean foreground) {
        return stopUser(userId, true, new IStopUserCallback.Stub() {
            public void userStopped(int userId) {
                UserController.this.mHandler.post(new -$$Lambda$UserController$3$DwbhQjwQF2qoVH0y07dd4wykxRA(this, userId, foreground));
            }

            public void userStopAborted(int userId) {
            }
        });
    }

    int stopUser(int userId, boolean force, IStopUserCallback callback) {
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = new StringBuilder();
            msg.append("Permission Denial: switchUser() from pid=");
            msg.append(Binder.getCallingPid());
            msg.append(", uid=");
            msg.append(Binder.getCallingUid());
            msg.append(" requires ");
            msg.append("android.permission.INTERACT_ACROSS_USERS_FULL");
            msg = msg.toString();
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        } else if (userId < 0 || userId == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't stop system user ");
            stringBuilder.append(userId);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            int stopUsersLU;
            enforceShellRestriction("no_debugging_features", userId);
            synchronized (this.mLock) {
                stopUsersLU = stopUsersLU(userId, force, callback);
            }
            return stopUsersLU;
        }
    }

    private int stopUsersLU(int userId, boolean force, IStopUserCallback callback) {
        if (userId == 0) {
            return -3;
        }
        if (isCurrentUserLU(userId)) {
            return -2;
        }
        int[] usersToStop = getUsersToStopLU(userId);
        for (int relatedUserId : usersToStop) {
            if (relatedUserId == 0 || isCurrentUserLU(relatedUserId)) {
                StringBuilder stringBuilder;
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("stopUsersLocked cannot stop related user ");
                    stringBuilder.append(relatedUserId);
                    Slog.i("ActivityManager", stringBuilder.toString());
                }
                if (!force) {
                    return -4;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Force stop user ");
                stringBuilder.append(userId);
                stringBuilder.append(". Related users will not be stopped");
                Slog.i("ActivityManager", stringBuilder.toString());
                stopSingleUserLU(userId, callback);
                return 0;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("stopUsersLocked usersToStop=");
            stringBuilder2.append(Arrays.toString(usersToStop));
            Slog.i("ActivityManager", stringBuilder2.toString());
        }
        for (int userIdToStop : usersToStop) {
            stopSingleUserLU(userIdToStop, userIdToStop == userId ? callback : null);
        }
        return 0;
    }

    private void stopSingleUserLU(int userId, IStopUserCallback callback) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopSingleUserLocked userId=");
            stringBuilder.append(userId);
            Slog.i("ActivityManager", stringBuilder.toString());
        }
        UserState uss = (UserState) this.mStartedUsers.get(userId);
        if (uss == null) {
            if (callback != null) {
                this.mHandler.post(new -$$Lambda$UserController$AHHTCREuropaUGilzG-tndQCCSM(callback, userId));
            }
            return;
        }
        if (callback != null) {
            uss.mStopCallbacks.add(callback);
        }
        if (!(uss.state == 4 || uss.state == 5)) {
            uss.setState(4);
            this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
            updateStartedUserArrayLU();
            this.mHandler.post(new -$$Lambda$UserController$GGvEPHwny2cP0yTZnJTgitTq9_U(this, userId, uss));
        }
    }

    static /* synthetic */ void lambda$stopSingleUserLU$2(IStopUserCallback callback, int userId) {
        try {
            callback.userStopped(userId);
        } catch (RemoteException e) {
        }
    }

    public static /* synthetic */ void lambda$stopSingleUserLU$3(UserController userController, int userId, UserState uss) {
        UserController userController2 = userController;
        final int i = userId;
        Intent stoppingIntent = new Intent("android.intent.action.USER_STOPPING");
        stoppingIntent.addFlags(1073741824);
        stoppingIntent.putExtra("android.intent.extra.user_handle", i);
        stoppingIntent.putExtra("android.intent.extra.SHUTDOWN_USERSPACE_ONLY", true);
        final UserState userState = uss;
        IIntentReceiver stoppingReceiver = new IIntentReceiver.Stub() {
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                UserController.this.mHandler.post(new -$$Lambda$UserController$4$P3Sj7pxBXLC7k_puCIIki2uVgGE(this, i, userState));
            }
        };
        userController2.mInjector.clearBroadcastQueueForUser(i);
        userController2.mInjector.broadcastIntent(stoppingIntent, null, stoppingReceiver, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, -1);
    }

    void finishUserStopping(int userId, UserState uss) {
        Throwable th;
        Intent intent;
        int i = userId;
        final UserState userState = uss;
        Intent shutdownIntent = new Intent("android.intent.action.ACTION_SHUTDOWN");
        shutdownIntent.putExtra("android.intent.extra.SHUTDOWN_USERSPACE_ONLY", true);
        IIntentReceiver shutdownReceiver = new IIntentReceiver.Stub() {
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                UserController.this.mHandler.post(new Runnable() {
                    public void run() {
                        UserController.this.finishUserStopped(userState);
                    }
                });
            }
        };
        synchronized (this.mLock) {
            try {
                if (userState.state != 4) {
                    try {
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        intent = shutdownIntent;
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
                userState.setState(5);
                this.mInjector.getUserManagerInternal().setUserState(i, userState.state);
                this.mInjector.batteryStatsServiceNoteEvent(16391, Integer.toString(userId), i);
                this.mInjector.getSystemServiceManager().stopUser(i);
                this.mInjector.broadcastIntent(shutdownIntent, null, shutdownReceiver, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, userId);
            } catch (Throwable th4) {
                th = th4;
                intent = shutdownIntent;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    void finishUserStopped(UserState uss) {
        ArrayList<IStopUserCallback> callbacks;
        boolean stopped;
        int userId = uss.mHandle.getIdentifier();
        synchronized (this.mLock) {
            callbacks = new ArrayList(uss.mStopCallbacks);
            if (this.mStartedUsers.get(userId) == uss) {
                if (uss.state == 5) {
                    stopped = true;
                    this.mStartedUsers.remove(userId);
                    this.mUserLru.remove(Integer.valueOf(userId));
                    updateStartedUserArrayLU();
                }
            }
            stopped = false;
        }
        boolean stopped2 = stopped;
        if (stopped2) {
            this.mInjector.getUserManagerInternal().removeUserState(userId);
            this.mInjector.activityManagerOnUserStopped(userId);
            forceStopUser(userId, "finish user");
        }
        for (int i = 0; i < callbacks.size(); i++) {
            if (stopped2) {
                try {
                    ((IStopUserCallback) callbacks.get(i)).userStopped(userId);
                } catch (RemoteException e) {
                }
            } else {
                ((IStopUserCallback) callbacks.get(i)).userStopAborted(userId);
            }
        }
        if (stopped2) {
            this.mInjector.systemServiceManagerCleanupUser(userId);
            this.mInjector.stackSupervisorRemoveUser(userId);
            if (getUserInfo(userId).isEphemeral()) {
                this.mInjector.getUserManager().removeUserEvenWhenDisallowed(userId);
            }
            FgThread.getHandler().post(new -$$Lambda$UserController$OCWSENtTocgCKtAUTrbiQWfjiB4(this, userId));
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0015, code skipped:
            r0 = r4.getUserInfo(r5);
     */
    /* JADX WARNING: Missing block: B:10:0x0019, code skipped:
            if (r0 == null) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:12:0x001f, code skipped:
            if (r0.isHwHiddenSpace() == false) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:13:0x0021, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:14:0x0023, code skipped:
            r1 = false;
     */
    /* JADX WARNING: Missing block: B:15:0x0024, code skipped:
            if (r1 != false) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:18:0x0028, code skipped:
            if (r4.mIsSupportISec != false) goto L_0x0032;
     */
    /* JADX WARNING: Missing block: B:19:0x002a, code skipped:
            r4.getStorageManager().lockUserKey(r5);
     */
    /* JADX WARNING: Missing block: B:20:0x0032, code skipped:
            r4.getStorageManager().lockUserKeyISec(r5);
     */
    /* JADX WARNING: Missing block: B:21:0x003a, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:23:0x003f, code skipped:
            throw r2.rethrowAsRuntimeException();
     */
    /* JADX WARNING: Missing block: B:24:0x0040, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$finishUserStopped$4(UserController userController, int userId) {
        synchronized (userController.mLock) {
            if (userController.mStartedUsers.get(userId) != null) {
                Slog.w("ActivityManager", "User was restarted, skipping key eviction");
            }
        }
    }

    private int[] getUsersToStopLU(int userId) {
        int startedUsersSize = this.mStartedUsers.size();
        IntArray userIds = new IntArray();
        userIds.add(userId);
        int userGroupId = this.mUserProfileGroupIds.get(userId, -10000);
        for (int i = 0; i < startedUsersSize; i++) {
            int startedUserId = ((UserState) this.mStartedUsers.valueAt(i)).mHandle.getIdentifier();
            boolean sameUserId = true;
            boolean sameGroup = userGroupId != -10000 && userGroupId == this.mUserProfileGroupIds.get(startedUserId, -10000);
            if (startedUserId != userId) {
                sameUserId = false;
            }
            if (sameGroup && !sameUserId) {
                userIds.add(startedUserId);
            }
        }
        return userIds.toArray();
    }

    private void forceStopUser(int userId, String reason) {
        int i = userId;
        this.mInjector.activityManagerForceStopPackage(i, reason);
        Intent intent = new Intent("android.intent.action.USER_STOPPED");
        intent.addFlags(1342177280);
        intent.putExtra("android.intent.extra.user_handle", i);
        this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, -1);
    }

    /* JADX WARNING: Missing block: B:17:0x0039, code skipped:
            r1 = getUserInfo(r5);
     */
    /* JADX WARNING: Missing block: B:18:0x0041, code skipped:
            if (r1.isEphemeral() == false) goto L_0x004e;
     */
    /* JADX WARNING: Missing block: B:19:0x0043, code skipped:
            ((android.os.UserManagerInternal) com.android.server.LocalServices.getService(android.os.UserManagerInternal.class)).onEphemeralUserStop(r5);
     */
    /* JADX WARNING: Missing block: B:21:0x0052, code skipped:
            if (r1.isGuest() != false) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:23:0x0058, code skipped:
            if (r1.isEphemeral() == false) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:24:0x005a, code skipped:
            r2 = r4.mLock;
     */
    /* JADX WARNING: Missing block: B:25:0x005c, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:28:?, code skipped:
            stopUsersLU(r5, true, null);
     */
    /* JADX WARNING: Missing block: B:29:0x0062, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:30:0x0063, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void stopGuestOrEphemeralUserIfBackground(int oldUserId) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stop guest or ephemeral user if background: ");
            stringBuilder.append(oldUserId);
            Slog.i("ActivityManager", stringBuilder.toString());
        }
        synchronized (this.mLock) {
            UserState oldUss = (UserState) this.mStartedUsers.get(oldUserId);
            if (!(oldUserId == 0 || oldUserId == this.mCurrentUserId || oldUss == null || oldUss.state == 4)) {
                if (oldUss.state == 5) {
                }
            }
        }
    }

    void scheduleStartProfiles() {
        FgThread.getHandler().post(new -$$Lambda$UserController$qvHU3An7LT0SKBclx4I2epe4KYI(this));
    }

    public static /* synthetic */ void lambda$scheduleStartProfiles$5(UserController userController) {
        if (!userController.mHandler.hasMessages(40)) {
            userController.mHandler.sendMessageDelayed(userController.mHandler.obtainMessage(40), 1000);
        }
    }

    void startProfiles() {
        int currentUserId = getCurrentUserId();
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i("ActivityManager", "startProfilesLocked");
        }
        List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(currentUserId, false);
        List<UserInfo> profilesToStart = new ArrayList(profiles.size());
        for (UserInfo user : profiles) {
            if (!((user.flags & 16) != 16 || user.id == currentUserId || user.isQuietModeEnabled())) {
                profilesToStart.add(user);
            }
        }
        int profilesToStartSize = profilesToStart.size();
        int i = 0;
        while (i < profilesToStartSize) {
            startUser(((UserInfo) profilesToStart.get(i)).id, false);
            i++;
        }
        if (i < profilesToStartSize) {
            Slog.w("ActivityManager", "More profiles than MAX_RUNNING_USERS");
        }
    }

    private IStorageManager getStorageManager() {
        return IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
    }

    boolean startUser(int userId, boolean foreground) {
        return startUser(userId, foreground, null);
    }

    private void setMultiDpi(WindowManagerService wms, int userId) {
        int dpi = SystemProperties.getInt("persist.sys.dpi", 0);
        int width = SystemProperties.getInt("persist.sys.rog.width", 0);
        int realdpi = SystemProperties.getInt("persist.sys.realdpi", 0);
        if (width > 0) {
            dpi = SystemProperties.getInt("persist.sys.realdpi", SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0))));
        }
        if (wms != null && dpi > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set multi dpi for user :");
            stringBuilder.append(userId);
            stringBuilder.append(", sys.dpi:");
            stringBuilder.append(dpi);
            stringBuilder.append(", readdpi:");
            stringBuilder.append(realdpi);
            stringBuilder.append(", width:");
            stringBuilder.append(width);
            Slog.i("ActivityManager", stringBuilder.toString());
            wms.setForcedDisplayDensityForUser(0, dpi, userId);
        }
    }

    /* JADX WARNING: Missing block: B:80:0x0197, code skipped:
            if (getUserInfo(r0.intValue()).isClonedProfile() == false) goto L_0x01a7;
     */
    /* JADX WARNING: Missing block: B:86:0x01b2, code skipped:
            r5 = r2;
     */
    /* JADX WARNING: Missing block: B:87:0x01b3, code skipped:
            if (r13 == null) goto L_0x01ba;
     */
    /* JADX WARNING: Missing block: B:89:?, code skipped:
            r5.mUnlockProgress.addListener(r13);
     */
    /* JADX WARNING: Missing block: B:90:0x01ba, code skipped:
            if (r21 == false) goto L_0x01c7;
     */
    /* JADX WARNING: Missing block: B:91:0x01bc, code skipped:
            r1.mInjector.getUserManagerInternal().setUserState(r15, r5.state);
     */
    /* JADX WARNING: Missing block: B:92:0x01c7, code skipped:
            if (r14 == false) goto L_0x021b;
     */
    /* JADX WARNING: Missing block: B:93:0x01c9, code skipped:
            r1.mInjector.reportGlobalUsageEventLocked(16);
            r2 = r1.mLock;
     */
    /* JADX WARNING: Missing block: B:94:0x01d2, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:96:?, code skipped:
            r1.mCurrentUserId = r15;
            r1.mTargetUserId = -10000;
     */
    /* JADX WARNING: Missing block: B:97:0x01d9, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:99:?, code skipped:
            r1.mInjector.updateUserConfiguration();
            updateCurrentProfileIds();
            r1.mInjector.getWindowManager().setCurrentUser(r15, getCurrentProfileIds());
            android.hwtheme.HwThemeManager.linkDataSkinDirAsUser(r50);
            r1.mInjector.reportCurWakefulnessUsageEvent();
     */
    /* JADX WARNING: Missing block: B:100:0x01f9, code skipped:
            if (r1.mUserSwitchUiEnabled == false) goto L_0x0240;
     */
    /* JADX WARNING: Missing block: B:102:0x0201, code skipped:
            if (com.android.server.am.UserController.Injector.access$200(r1.mInjector, r6, r7) != false) goto L_0x0240;
     */
    /* JADX WARNING: Missing block: B:103:0x0203, code skipped:
            r1.mInjector.getWindowManager().setSwitchingUser(true);
            r1.mInjector.getWindowManager().lockNow(null);
     */
    /* JADX WARNING: Missing block: B:110:?, code skipped:
            r2 = java.lang.Integer.valueOf(r1.mCurrentUserId);
            updateCurrentProfileIds();
            r1.mInjector.getWindowManager().setCurrentProfileIds(getCurrentProfileIds());
            r4 = r1.mLock;
     */
    /* JADX WARNING: Missing block: B:111:0x0234, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:113:?, code skipped:
            r1.mUserLru.remove(r2);
            r1.mUserLru.add(r2);
     */
    /* JADX WARNING: Missing block: B:114:0x023f, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:118:0x0243, code skipped:
            if (r5.state != 4) goto L_0x0261;
     */
    /* JADX WARNING: Missing block: B:120:?, code skipped:
            r5.setState(r5.lastState);
            r1.mInjector.getUserManagerInternal().setUserState(r15, r5.state);
            r2 = r1.mLock;
     */
    /* JADX WARNING: Missing block: B:121:0x0257, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:123:?, code skipped:
            updateStartedUserArrayLU();
     */
    /* JADX WARNING: Missing block: B:124:0x025b, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:125:0x025c, code skipped:
            r3 = true;
     */
    /* JADX WARNING: Missing block: B:133:0x0264, code skipped:
            if (r5.state != 5) goto L_0x0281;
     */
    /* JADX WARNING: Missing block: B:136:?, code skipped:
            r5.setState(0);
            r1.mInjector.getUserManagerInternal().setUserState(r15, r5.state);
            r2 = r1.mLock;
     */
    /* JADX WARNING: Missing block: B:137:0x0277, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:139:?, code skipped:
            updateStartedUserArrayLU();
     */
    /* JADX WARNING: Missing block: B:140:0x027b, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:141:0x027c, code skipped:
            r3 = true;
     */
    /* JADX WARNING: Missing block: B:146:0x0281, code skipped:
            r0 = r3;
     */
    /* JADX WARNING: Missing block: B:149:0x0284, code skipped:
            if (r5.state != 0) goto L_0x02a0;
     */
    /* JADX WARNING: Missing block: B:151:?, code skipped:
            r1.mInjector.getUserManager().onBeforeStartUser(r15);
            r22 = r6;
            r1.mHandler.sendMessage(r1.mHandler.obtainMessage(50, r15, null));
     */
    /* JADX WARNING: Missing block: B:152:0x02a0, code skipped:
            r22 = r6;
     */
    /* JADX WARNING: Missing block: B:153:0x02a2, code skipped:
            if (r14 == false) goto L_0x02da;
     */
    /* JADX WARNING: Missing block: B:154:0x02a4, code skipped:
            r1.mHandler.sendMessage(r1.mHandler.obtainMessage(60, r15, r8));
            r1.mHandler.removeMessages(10);
            r1.mHandler.removeMessages(30);
            r1.mHandler.sendMessage(r1.mHandler.obtainMessage(10, r8, r15, r5));
            r23 = r5;
            r1.mHandler.sendMessageDelayed(r1.mHandler.obtainMessage(30, r8, r15, r5), 3000);
     */
    /* JADX WARNING: Missing block: B:155:0x02da, code skipped:
            r23 = r5;
     */
    /* JADX WARNING: Missing block: B:156:0x02dc, code skipped:
            if (r0 == false) goto L_0x033f;
     */
    /* JADX WARNING: Missing block: B:158:?, code skipped:
            r6 = new android.content.Intent("android.intent.action.USER_STARTED");
            r6.addFlags(1342177280);
            r6.putExtra("android.intent.extra.user_handle", r15);
     */
    /* JADX WARNING: Missing block: B:159:0x0306, code skipped:
            r29 = r23;
            r23 = r6;
            r30 = r7;
            r31 = r8;
            r32 = r9;
            r18 = r11;
            r17 = r14;
     */
    /* JADX WARNING: Missing block: B:161:?, code skipped:
            r1.mInjector.broadcastIntent(r6, null, null, 0, null, null, null, -1, null, false, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r50);
     */
    /* JADX WARNING: Missing block: B:162:0x0331, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:163:0x0332, code skipped:
            r6 = r50;
     */
    /* JADX WARNING: Missing block: B:164:0x0335, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:165:0x0336, code skipped:
            r18 = r11;
            r17 = r14;
            r6 = r50;
            r8 = r9;
     */
    /* JADX WARNING: Missing block: B:166:0x033f, code skipped:
            r30 = r7;
            r31 = r8;
            r32 = r9;
            r18 = r11;
            r17 = r14;
            r29 = r23;
     */
    /* JADX WARNING: Missing block: B:167:0x034b, code skipped:
            if (r17 == false) goto L_0x035c;
     */
    /* JADX WARNING: Missing block: B:168:0x034d, code skipped:
            r6 = r50;
     */
    /* JADX WARNING: Missing block: B:170:?, code skipped:
            moveUserToForeground(r29, r31, r6);
     */
    /* JADX WARNING: Missing block: B:171:0x0357, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:173:0x035c, code skipped:
            r6 = r50;
            r7 = r31;
     */
    /* JADX WARNING: Missing block: B:175:?, code skipped:
            finishUserBoot(r29);
     */
    /* JADX WARNING: Missing block: B:176:0x0365, code skipped:
            if (r0 == false) goto L_0x03a4;
     */
    /* JADX WARNING: Missing block: B:178:?, code skipped:
            r2 = new android.content.Intent("android.intent.action.USER_STARTING");
            r2.addFlags(1073741824);
            r2.putExtra("android.intent.extra.user_handle", r6);
            r3 = r1.mInjector;
            r3.broadcastIntent(r2, null, new com.android.server.am.UserController.AnonymousClass6(r1), 0, null, null, new java.lang.String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, -1);
     */
    /* JADX WARNING: Missing block: B:180:?, code skipped:
            r1.isColdStart = r0;
     */
    /* JADX WARNING: Missing block: B:181:0x03a6, code skipped:
            android.os.Binder.restoreCallingIdentity(r32);
            r2 = new java.lang.StringBuilder();
            r2.append("_StartUser startUser userid:");
            r2.append(r6);
            r2.append(" cost ");
            r2.append(android.os.SystemClock.elapsedRealtime() - r18);
            r2.append(" ms");
            android.util.Slog.i("ActivityManager", r2.toString());
     */
    /* JADX WARNING: Missing block: B:182:0x03d6, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:183:0x03d7, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:184:0x03d8, code skipped:
            r8 = r32;
     */
    /* JADX WARNING: Missing block: B:186:0x03dd, code skipped:
            r22 = r6;
            r30 = r7;
            r7 = r8;
            r8 = r9;
            r18 = r11;
            r17 = r14;
            r6 = r15;
     */
    /* JADX WARNING: Missing block: B:204:0x041c, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean startUser(int userId, boolean foreground, IProgressListener unlockListener) {
        Throwable th;
        long ident;
        long j;
        boolean z;
        int i;
        boolean needStart;
        UserInfo userInfo;
        UserInfo userInfo2;
        int i2 = userId;
        boolean z2 = foreground;
        IProgressListener iProgressListener = unlockListener;
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            long startedTime = SystemClock.elapsedRealtime();
            this.SwitchUser_Time = startedTime;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting userid:");
            stringBuilder.append(i2);
            stringBuilder.append(" fg:");
            stringBuilder.append(z2);
            Slog.i("ActivityManager", stringBuilder.toString());
            long ident2 = Binder.clearCallingIdentity();
            int oldUserId = getCurrentUserId();
            if (oldUserId == i2) {
                Binder.restoreCallingIdentity(ident2);
                return true;
            }
            if (z2) {
                try {
                    this.mInjector.clearAllLockedTasks("startUser");
                } catch (Throwable th2) {
                    th = th2;
                    ident = ident2;
                    j = startedTime;
                    z = z2;
                    i = i2;
                }
            }
            try {
                UserInfo userInfo3 = getUserInfo(userId);
                StringBuilder stringBuilder2;
                if (userInfo3 == null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("No user info for user #");
                    stringBuilder2.append(i2);
                    Slog.w("ActivityManager", stringBuilder2.toString());
                    Binder.restoreCallingIdentity(ident2);
                    return false;
                }
                if (z2) {
                    if (userInfo3.isManagedProfile()) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cannot switch to User #");
                        stringBuilder2.append(i2);
                        stringBuilder2.append(": not a full user");
                        Slog.w("ActivityManager", stringBuilder2.toString());
                        Binder.restoreCallingIdentity(ident2);
                        return false;
                    }
                }
                setMultiDpi(this.mInjector.getWindowManager(), i2);
                UserInfo lastUserInfo = getUserInfo(this.mCurrentUserId);
                if (z2) {
                    if (this.mUserSwitchUiEnabled && !this.mInjector.shouldSkipKeyguard(lastUserInfo, userInfo3)) {
                        this.mInjector.getWindowManager().startFreezingScreen(17432717, 17432716);
                    }
                }
                synchronized (this.mLock) {
                    boolean updateUmState;
                    try {
                        UserState uss = (UserState) this.mStartedUsers.get(i2);
                        boolean needStart2;
                        if (uss == null) {
                            try {
                                needStart = false;
                                try {
                                    uss = new UserState(UserHandle.of(userId));
                                    updateUmState = false;
                                    try {
                                        uss.mUnlockProgress.addListener(new UserProgressListener());
                                        this.mStartedUsers.put(i2, uss);
                                        updateStartedUserArrayLU();
                                        needStart2 = true;
                                        updateUmState = true;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        userInfo3 = oldUserId;
                                        ident = ident2;
                                        z = z2;
                                        i = i2;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th4) {
                                                th = th4;
                                            }
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                    updateUmState = false;
                                    userInfo = lastUserInfo;
                                    userInfo2 = userInfo3;
                                    userInfo3 = oldUserId;
                                    ident = ident2;
                                    j = startedTime;
                                    z = z2;
                                    lastUserInfo = i2;
                                    needStart2 = needStart;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                needStart = false;
                                updateUmState = false;
                                userInfo = lastUserInfo;
                                ident = ident2;
                                z = z2;
                                lastUserInfo = i2;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        } else {
                            needStart = false;
                            updateUmState = false;
                            try {
                                if (uss.state == 5) {
                                    if (!isCallingOnHandlerThread()) {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("User #");
                                        stringBuilder2.append(i2);
                                        stringBuilder2.append(" is shutting down - will start after full stop");
                                        Slog.i("ActivityManager", stringBuilder2.toString());
                                        this.mHandler.post(new -$$Lambda$UserController$itozNmdxq9RsTKqW4_f-sH8yPdY(this, i2, z2, iProgressListener));
                                        Binder.restoreCallingIdentity(ident2);
                                        return true;
                                    }
                                }
                                needStart2 = needStart;
                            } catch (Throwable th7) {
                                th = th7;
                                userInfo = lastUserInfo;
                                userInfo2 = userInfo3;
                                userInfo3 = oldUserId;
                                ident = ident2;
                                j = startedTime;
                                z = z2;
                                lastUserInfo = i2;
                                needStart2 = needStart;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                        try {
                            Integer userIdInt = Integer.valueOf(userId);
                            if (getUserInfo(userIdInt.intValue()) != null) {
                                try {
                                } catch (Throwable th8) {
                                    th = th8;
                                    userInfo = lastUserInfo;
                                    userInfo2 = userInfo3;
                                    userInfo3 = oldUserId;
                                    ident = ident2;
                                    j = startedTime;
                                    z = z2;
                                    lastUserInfo = i2;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            this.mUserLru.remove(userIdInt);
                            this.mUserLru.add(userIdInt);
                        } catch (Throwable th9) {
                            th = th9;
                            userInfo = lastUserInfo;
                            userInfo2 = userInfo3;
                            userInfo3 = oldUserId;
                            ident = ident2;
                            j = startedTime;
                            z = z2;
                            lastUserInfo = i2;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        needStart = false;
                        updateUmState = false;
                        userInfo = lastUserInfo;
                        userInfo2 = userInfo3;
                        ident = ident2;
                        j = startedTime;
                        z = z2;
                        lastUserInfo = i2;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            } catch (Throwable th11) {
                th = th11;
                ident = ident2;
                j = startedTime;
                z = z2;
                i = i2;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
        z = z2;
        i = i2;
        String msg = new StringBuilder();
        msg.append("Permission Denial: switchUser() from pid=");
        msg.append(Binder.getCallingPid());
        msg.append(", uid=");
        msg.append(Binder.getCallingUid());
        msg.append(" requires ");
        msg.append("android.permission.INTERACT_ACROSS_USERS_FULL");
        msg = msg.toString();
        Slog.w("ActivityManager", msg);
        throw new SecurityException(msg);
        ident = ident;
        Binder.restoreCallingIdentity(ident);
        throw th;
    }

    private boolean isCallingOnHandlerThread() {
        return Looper.myLooper() == this.mHandler.getLooper();
    }

    void startUserInForeground(int targetUserId) {
        if (!startUser(targetUserId, true)) {
            this.mInjector.getWindowManager().setSwitchingUser(false);
        }
    }

    boolean unlockUser(int userId, byte[] token, byte[] secret, IProgressListener listener) {
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            long binderToken = Binder.clearCallingIdentity();
            try {
                boolean unlockUserCleared = unlockUserCleared(userId, token, secret, listener);
                return unlockUserCleared;
            } finally {
                Binder.restoreCallingIdentity(binderToken);
            }
        } else {
            String msg = new StringBuilder();
            msg.append("Permission Denial: unlockUser() from pid=");
            msg.append(Binder.getCallingPid());
            msg.append(", uid=");
            msg.append(Binder.getCallingUid());
            msg.append(" requires ");
            msg.append("android.permission.INTERACT_ACROSS_USERS_FULL");
            msg = msg.toString();
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        }
    }

    private boolean maybeUnlockUser(int userId) {
        return unlockUserCleared(userId, null, null, null);
    }

    private static void notifyFinished(int userId, IProgressListener listener) {
        if (listener != null) {
            try {
                listener.onFinished(userId, null);
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:39:0x00d6  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00e4 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x0104  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00ff  */
    /* JADX WARNING: Missing block: B:9:0x002f, code skipped:
            if (r13.isClonedProfile() != false) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:10:0x0031, code skipped:
            android.util.Slog.i("ActivityManager", "ClonedProfile user unlock, set mHaveTryCloneProUserUnlock true!");
            r8.mHaveTryCloneProUserUnlock = true;
     */
    /* JADX WARNING: Missing block: B:17:0x0069, code skipped:
            if (r13.isClonedProfile() != false) goto L_0x0031;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean unlockUserCleared(int userId, byte[] token, byte[] secret, IProgressListener listener) {
        boolean z;
        ISDCardCryptedHelper helper;
        UserState uss;
        UserState uss2;
        int i = userId;
        byte[] bArr = token;
        byte[] bArr2 = secret;
        IProgressListener iProgressListener = listener;
        UserInfo userInfo = getUserInfo(userId);
        IStorageManager storageManager = getStorageManager();
        if (!StorageManager.isUserKeyUnlocked(userId)) {
            try {
                if (this.mIsSupportISec) {
                    storageManager.unlockUserKeyISec(i, userInfo.serialNumber, bArr, bArr2);
                } else {
                    storageManager.unlockUserKey(i, userInfo.serialNumber, bArr, bArr2);
                }
                if (userInfo != null) {
                }
            } catch (RemoteException | RuntimeException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to unlock: ");
                stringBuilder.append(e.getMessage());
                stringBuilder.append(" ,SupportISec: ");
                stringBuilder.append(this.mIsSupportISec);
                Slog.w("ActivityManager", stringBuilder.toString());
                if (userInfo != null) {
                }
            } catch (Throwable th) {
                if (userInfo != null && userInfo.isClonedProfile()) {
                    Slog.i("ActivityManager", "ClonedProfile user unlock, set mHaveTryCloneProUserUnlock true!");
                    this.mHaveTryCloneProUserUnlock = true;
                }
            }
        } else if (this.mIsSupportISec) {
            if (bArr == null && bArr2 == null) {
                Slog.w("ActivityManager", "is SupportISec,Failed to unlockUserScreenISec: token is null  And secret is null");
            } else {
                boolean isSuccess;
                boolean isSuccess2 = false;
                try {
                    isSuccess = storageManager.setScreenStateFlag(i, userInfo.serialNumber, 2);
                } catch (RemoteException | RuntimeException e2) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("is SupportISec,Failed to setScreenStateFlag: ");
                    stringBuilder2.append(e2.getMessage());
                    Slog.w("ActivityManager", stringBuilder2.toString());
                    isSuccess = isSuccess2;
                }
                if (isSuccess) {
                    final IStorageManager iStorageManager = storageManager;
                    final int i2 = i;
                    AnonymousClass7 anonymousClass7 = r1;
                    final UserInfo userInfo2 = userInfo;
                    Handler handler = this.mHandler;
                    final byte[] bArr3 = bArr;
                    z = true;
                    final byte[] bArr4 = bArr2;
                    AnonymousClass7 anonymousClass72 = new Runnable() {
                        public void run() {
                            try {
                                iStorageManager.unlockUserScreenISec(i2, userInfo2.serialNumber, bArr3, bArr4, 1);
                            } catch (RemoteException | RuntimeException e) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("is SupportISec,Failed to unlockUserScreenISec: ");
                                stringBuilder.append(e.getMessage());
                                Slog.w("ActivityManager", stringBuilder.toString());
                            }
                        }
                    };
                    handler.post(anonymousClass7);
                    helper = HwServiceFactory.getSDCardCryptedHelper();
                    if (helper != null) {
                        UserInfo info = getUserInfo(userId);
                        if (info != null) {
                            helper.unlockKey(i, info.serialNumber, bArr, bArr2);
                        }
                    }
                    synchronized (this.mLock) {
                        uss = (UserState) this.mStartedUsers.get(i);
                        if (uss != null) {
                            uss.mUnlockProgress.addListener(iProgressListener);
                            uss.tokenProvided = bArr != null ? z : false;
                        }
                    }
                    uss2 = uss;
                    if (uss2 != null) {
                        notifyFinished(i, iProgressListener);
                        return false;
                    }
                    int[] userIds;
                    int i3;
                    int i4 = 0;
                    finishUserUnlocking(uss2);
                    synchronized (this.mLock) {
                        userIds = new int[this.mStartedUsers.size()];
                        for (i3 = 0; i3 < userIds.length; i3++) {
                            userIds[i3] = this.mStartedUsers.keyAt(i3);
                        }
                    }
                    i2 = userIds.length;
                    while (i4 < i2) {
                        int[] userIds2;
                        i3 = userIds[i4];
                        UserInfo parent = this.mInjector.getUserManager().getProfileParent(i3);
                        if (parent == null || parent.id != i || i3 == i) {
                            userIds2 = userIds;
                        } else {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            userIds2 = userIds;
                            stringBuilder3.append("User ");
                            stringBuilder3.append(i3);
                            stringBuilder3.append(" (parent ");
                            stringBuilder3.append(parent.id);
                            stringBuilder3.append("): attempting unlock because parent was just unlocked");
                            Slog.d("ActivityManager", stringBuilder3.toString());
                            maybeUnlockUser(i3);
                        }
                        i4++;
                        userIds = userIds2;
                    }
                    return z;
                }
            }
        }
        z = true;
        helper = HwServiceFactory.getSDCardCryptedHelper();
        if (helper != null) {
        }
        synchronized (this.mLock) {
        }
        uss2 = uss;
        if (uss2 != null) {
        }
    }

    boolean switchUser(int targetUserId) {
        enforceShellRestriction("no_debugging_features", targetUserId);
        int currentUserId = getCurrentUserId();
        UserInfo targetUserInfo = getUserInfo(targetUserId);
        StringBuilder stringBuilder;
        if (targetUserId == currentUserId) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("user #");
            stringBuilder.append(targetUserId);
            stringBuilder.append(" is already the current user");
            Slog.i("ActivityManager", stringBuilder.toString());
            return true;
        } else if (targetUserInfo == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No user info for user #");
            stringBuilder.append(targetUserId);
            Slog.w("ActivityManager", stringBuilder.toString());
            return false;
        } else if (!targetUserInfo.supportsSwitchTo()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot switch to User #");
            stringBuilder.append(targetUserId);
            stringBuilder.append(": not supported");
            Slog.w("ActivityManager", stringBuilder.toString());
            return false;
        } else if (targetUserInfo.isManagedProfile()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot switch to User #");
            stringBuilder.append(targetUserId);
            stringBuilder.append(": not a full user");
            Slog.w("ActivityManager", stringBuilder.toString());
            return false;
        } else {
            synchronized (this.mLock) {
                this.mTargetUserId = targetUserId;
            }
            UserInfo currentUserInfo = getUserInfo(this.mCurrentUserId);
            boolean isHiddenSpaceSwitch = this.mInjector.mService.isHiddenSpaceSwitch(currentUserInfo, targetUserInfo);
            this.misHiddenSpaceSwitch = isHiddenSpaceSwitch;
            if (!this.mUserSwitchUiEnabled || isHiddenSpaceSwitch) {
                if (isHiddenSpaceSwitch) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        this.mInjector.cleanAppForHiddenSpace();
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                this.mHandler.removeMessages(START_USER_SWITCH_FG_MSG);
                this.mHandler.sendMessage(this.mHandler.obtainMessage(START_USER_SWITCH_FG_MSG, targetUserId, 0));
            } else {
                Pair<UserInfo, UserInfo> userNames = new Pair(currentUserInfo, targetUserInfo);
                this.mUiHandler.removeMessages(1000);
                this.mUiHandler.sendMessage(this.mHandler.obtainMessage(1000, userNames));
            }
            return true;
        }
    }

    void showUserSwitchDialog(Pair<UserInfo, UserInfo> fromToUserPair) {
        this.mInjector.showUserSwitchingDialog((UserInfo) fromToUserPair.first, (UserInfo) fromToUserPair.second, getSwitchingFromSystemUserMessage(), getSwitchingToSystemUserMessage());
    }

    private void dispatchForegroundProfileChanged(int userId) {
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                ((IUserSwitchObserver) this.mUserSwitchObservers.getBroadcastItem(i)).onForegroundProfileSwitch(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    void dispatchUserSwitchComplete(int userId) {
        long startedTime = SystemClock.elapsedRealtime();
        int i = 0;
        this.mInjector.getWindowManager().setSwitchingUser(false);
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        while (i < observerCount) {
            try {
                ((IUserSwitchObserver) this.mUserSwitchObservers.getBroadcastItem(i)).onUserSwitchComplete(userId);
            } catch (RemoteException e) {
            }
            i++;
        }
        this.mUserSwitchObservers.finishBroadcast();
        if (this.misHiddenSpaceSwitch) {
            this.SwitchUser_Time = SystemClock.elapsedRealtime() - this.SwitchUser_Time;
            Context context = this.mInjector.getContext();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{isColdStart:");
            stringBuilder.append(this.isColdStart);
            stringBuilder.append(",SwitchUser_Time:");
            stringBuilder.append(this.SwitchUser_Time);
            stringBuilder.append("ms}");
            Flog.bdReport(context, 530, stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("_StartUser dispatchUserSwitchComplete userid:");
        stringBuilder2.append(userId);
        stringBuilder2.append(" cost ");
        stringBuilder2.append(SystemClock.elapsedRealtime() - startedTime);
        stringBuilder2.append(" ms");
        Slog.i("ActivityManager", stringBuilder2.toString());
    }

    private void dispatchLockedBootComplete(int userId) {
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                ((IUserSwitchObserver) this.mUserSwitchObservers.getBroadcastItem(i)).onLockedBootComplete(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    private void stopBackgroundUsersIfEnforced(int oldUserId) {
        if (oldUserId != 0 && hasUserRestriction("no_run_in_background", oldUserId)) {
            synchronized (this.mLock) {
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("stopBackgroundUsersIfEnforced stopping ");
                    stringBuilder.append(oldUserId);
                    stringBuilder.append(" and related users");
                    Slog.i("ActivityManager", stringBuilder.toString());
                }
                stopUsersLU(oldUserId, false, null);
            }
        }
    }

    private void timeoutUserSwitch(UserState uss, int oldUserId, int newUserId) {
        synchronized (this.mLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User switch timeout: from ");
            stringBuilder.append(oldUserId);
            stringBuilder.append(" to ");
            stringBuilder.append(newUserId);
            Slog.e("ActivityManager", stringBuilder.toString());
            this.mTimeoutUserSwitchCallbacks = this.mCurWaitingUserSwitchCallbacks;
            this.mHandler.removeMessages(USER_SWITCH_CALLBACKS_TIMEOUT_MSG);
            sendContinueUserSwitchLU(uss, oldUserId, newUserId);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(USER_SWITCH_CALLBACKS_TIMEOUT_MSG, oldUserId, newUserId), 5000);
        }
    }

    private void timeoutUserSwitchCallbacks(int oldUserId, int newUserId) {
        synchronized (this.mLock) {
            if (!(this.mTimeoutUserSwitchCallbacks == null || this.mTimeoutUserSwitchCallbacks.isEmpty())) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("User switch timeout: from ");
                stringBuilder.append(oldUserId);
                stringBuilder.append(" to ");
                stringBuilder.append(newUserId);
                stringBuilder.append(". Observers that didn't respond: ");
                stringBuilder.append(this.mTimeoutUserSwitchCallbacks);
                Slog.wtf("ActivityManager", stringBuilder.toString());
                this.mTimeoutUserSwitchCallbacks = null;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x004a A:{SYNTHETIC, Splitter:B:11:0x004a} */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x004a A:{SYNTHETIC, Splitter:B:11:0x004a} */
    /* JADX WARNING: Can't wrap try/catch for region: R(3:14|15|16) */
    /* JADX WARNING: Missing block: B:23:0x0095, code skipped:
            r18 = r7;
            r20 = r11;
            r19 = r15;
            r15 = r9;
     */
    /* JADX WARNING: Missing block: B:39:0x00bb, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void dispatchUserSwitch(UserState uss, int oldUserId, int newUserId) {
        ArraySet<String> curWaitingUserSwitchCallbacks;
        int observerCount;
        int i;
        int i2;
        int i3 = newUserId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Dispatch onUserSwitching oldUser #");
        int i4 = oldUserId;
        stringBuilder.append(i4);
        UserController userController = " newUser #";
        stringBuilder.append(userController);
        stringBuilder.append(i3);
        Slog.d("ActivityManager", stringBuilder.toString());
        int observerCount2 = this.mUserSwitchObservers.beginBroadcast();
        if (observerCount2 > 0) {
            UserState userState;
            ArraySet<String> curWaitingUserSwitchCallbacks2 = new ArraySet();
            AnonymousClass8 anonymousClass8 = this.mLock;
            synchronized (anonymousClass8) {
                userState = uss;
                try {
                    userState.switching = true;
                    this.mCurWaitingUserSwitchCallbacks = curWaitingUserSwitchCallbacks2;
                } finally {
                    observerCount2 = 
/*
Method generation error in method: com.android.server.am.UserController.dispatchUserSwitch(com.android.server.am.UserState, int, int):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r15_10 'observerCount2' int) = (r15_9 'observerCount2' int), (r14_0 'i4' int) in method: com.android.server.am.UserController.dispatchUserSwitch(com.android.server.am.UserState, int, int):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:205)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:102)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:52)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:300)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeSynchronizedRegion(RegionGen.java:230)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:67)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 33 more

*/

    void sendContinueUserSwitchLU(UserState uss, int oldUserId, int newUserId) {
        this.mCurWaitingUserSwitchCallbacks = null;
        this.mHandler.removeMessages(30);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(20, oldUserId, newUserId, uss));
    }

    void continueUserSwitch(UserState uss, int oldUserId, int newUserId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Continue user switch oldUser #");
        stringBuilder.append(oldUserId);
        stringBuilder.append(", newUser #");
        stringBuilder.append(newUserId);
        Slog.d("ActivityManager", stringBuilder.toString());
        if (this.mUserSwitchUiEnabled) {
            this.mInjector.getWindowManager().stopFreezingScreen();
        }
        uss.switching = false;
        this.mHandler.removeMessages(80);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(80, newUserId, 0));
        stopGuestOrEphemeralUserIfBackground(oldUserId);
        stopBackgroundUsersIfEnforced(oldUserId);
    }

    private void moveUserToForeground(UserState uss, int oldUserId, int newUserId) {
        boolean homeInFront = this.mInjector.stackSupervisorSwitchUser(newUserId, uss);
        HwThemeManager.updateConfiguration(true);
        ContentResolver cr = this.mInjector.getContext().getContentResolver();
        Configuration config = new Configuration();
        HwThemeManager.retrieveSimpleUIConfig(cr, config, newUserId);
        config.fontScale = System.getFloatForUser(cr, "font_scale", config.fontScale, newUserId);
        this.mInjector.updatePersistentConfiguration(config);
        if (homeInFront) {
            this.mInjector.startHomeActivity(newUserId, "moveUserToForeground");
        } else {
            this.mInjector.stackSupervisorResumeFocusedStackTopActivity();
        }
        EventLogTags.writeAmSwitchUser(newUserId);
        sendUserSwitchBroadcasts(oldUserId, newUserId);
    }

    void sendUserSwitchBroadcasts(int oldUserId, int newUserId) {
        List<UserInfo> profiles;
        int count;
        int i;
        Intent intent;
        int i2 = oldUserId;
        int i3 = newUserId;
        long ident = Binder.clearCallingIdentity();
        int i4 = 0;
        if (i2 >= 0) {
            try {
                profiles = this.mInjector.getUserManager().getProfiles(i2, false);
                count = profiles.size();
                for (i = 0; i < count; i++) {
                    int profileUserId = ((UserInfo) profiles.get(i)).id;
                    intent = new Intent("android.intent.action.USER_BACKGROUND");
                    intent.addFlags(1342177280);
                    intent.putExtra("android.intent.extra.user_handle", profileUserId);
                    this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, profileUserId);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
        if (i3 >= 0) {
            profiles = this.mInjector.getUserManager().getProfiles(i3, false);
            count = profiles.size();
            while (i4 < count) {
                i = ((UserInfo) profiles.get(i4)).id;
                intent = new Intent("android.intent.action.USER_FOREGROUND");
                intent.addFlags(1342177280);
                intent.putExtra("android.intent.extra.user_handle", i);
                this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, i);
                i4++;
            }
            Intent intent2 = new Intent("android.intent.action.USER_SWITCHED");
            intent2.addFlags(1342177280);
            intent2.putExtra("android.intent.extra.user_handle", i3);
            this.mInjector.broadcastIntent(intent2, null, null, 0, null, null, new String[]{"android.permission.MANAGE_USERS"}, -1, null, false, false, ActivityManagerService.MY_PID, 1000, -1);
        }
        Binder.restoreCallingIdentity(ident);
    }

    int handleIncomingUser(int callingPid, int callingUid, int userId, boolean allowAll, int allowMode, String name, String callerPackage) {
        int i = callingUid;
        int i2 = userId;
        int i3 = allowMode;
        String str = callerPackage;
        int callingUserId = UserHandle.getUserId(callingUid);
        if (callingUserId == i2 || this.mInjector.getUserManagerInternal().isSameGroupForClone(callingUserId, i2)) {
            return i2;
        }
        String str2;
        int targetUserId = unsafeConvertIncomingUser(i2);
        if (!(i == 0 || i == 1000)) {
            boolean allow;
            StringBuilder stringBuilder;
            if (this.mInjector.isCallerRecents(i) && callingUserId == getCurrentUserId() && isSameProfileGroup(callingUserId, targetUserId)) {
                allow = true;
            } else if (this.mInjector.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS_FULL", callingPid, i, -1, true) == 0) {
                allow = true;
            } else if (i3 == 2) {
                allow = false;
            } else if (this.mInjector.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", callingPid, i, -1, true) != 0) {
                allow = false;
            } else if (i3 == 0) {
                allow = true;
            } else if (i3 == 1) {
                allow = isSameProfileGroup(callingUserId, targetUserId);
            } else {
                str2 = name;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown mode: ");
                stringBuilder.append(i3);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            if (!allow) {
                if (i2 == -3) {
                    targetUserId = callingUserId;
                } else {
                    stringBuilder = new StringBuilder(128);
                    stringBuilder.append("Permission Denial: ");
                    stringBuilder.append(name);
                    if (str != null) {
                        stringBuilder.append(" from ");
                        stringBuilder.append(str);
                    }
                    stringBuilder.append(" asks to run as user ");
                    stringBuilder.append(i2);
                    stringBuilder.append(" but is calling from user ");
                    stringBuilder.append(UserHandle.getUserId(callingUid));
                    stringBuilder.append("; this requires ");
                    stringBuilder.append("android.permission.INTERACT_ACROSS_USERS_FULL");
                    if (i3 != 2) {
                        stringBuilder.append(" or ");
                        stringBuilder.append("android.permission.INTERACT_ACROSS_USERS");
                    }
                    String msg = stringBuilder.toString();
                    Slog.w("ActivityManager", msg);
                    throw new SecurityException(msg);
                }
            }
        }
        str2 = name;
        if (!allowAll) {
            ensureNotSpecialUser(targetUserId);
        }
        if (i != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || targetUserId < 0 || !hasUserRestriction("no_debugging_features", targetUserId)) {
            return targetUserId;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Shell does not have permission to access user ");
        stringBuilder2.append(targetUserId);
        stringBuilder2.append("\n ");
        stringBuilder2.append(Debug.getCallers(3));
        throw new SecurityException(stringBuilder2.toString());
    }

    int unsafeConvertIncomingUser(int userId) {
        if (userId == -2 || userId == -3) {
            return getCurrentUserId();
        }
        return userId;
    }

    void ensureNotSpecialUser(int userId) {
        if (userId < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Call does not support special user #");
            stringBuilder.append(userId);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    void registerUserSwitchObserver(IUserSwitchObserver observer, String name) {
        Preconditions.checkNotNull(name, "Observer name cannot be null");
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            this.mUserSwitchObservers.register(observer, name);
            return;
        }
        String msg = new StringBuilder();
        msg.append("Permission Denial: registerUserSwitchObserver() from pid=");
        msg.append(Binder.getCallingPid());
        msg.append(", uid=");
        msg.append(Binder.getCallingUid());
        msg.append(" requires ");
        msg.append("android.permission.INTERACT_ACROSS_USERS_FULL");
        msg = msg.toString();
        Slog.w("ActivityManager", msg);
        throw new SecurityException(msg);
    }

    void sendForegroundProfileChanged(int userId) {
        this.mHandler.removeMessages(70);
        this.mHandler.obtainMessage(70, userId, 0).sendToTarget();
    }

    void unregisterUserSwitchObserver(IUserSwitchObserver observer) {
        this.mUserSwitchObservers.unregister(observer);
    }

    UserState getStartedUserState(int userId) {
        UserState userState;
        synchronized (this.mLock) {
            userState = (UserState) this.mStartedUsers.get(userId);
        }
        return userState;
    }

    boolean hasStartedUserState(int userId) {
        return this.mStartedUsers.get(userId) != null;
    }

    private void updateStartedUserArrayLU() {
        int i;
        int i2 = 0;
        int num = 0;
        for (i = 0; i < this.mStartedUsers.size(); i++) {
            UserState uss = (UserState) this.mStartedUsers.valueAt(i);
            if (!(uss.state == 4 || uss.state == 5)) {
                num++;
            }
        }
        this.mStartedUserArray = new int[num];
        i = 0;
        while (i2 < this.mStartedUsers.size()) {
            UserState uss2 = (UserState) this.mStartedUsers.valueAt(i2);
            if (!(uss2.state == 4 || uss2.state == 5)) {
                int num2 = i + 1;
                this.mStartedUserArray[i] = this.mStartedUsers.keyAt(i2);
                i = num2;
            }
            i2++;
        }
    }

    void sendBootCompleted(IIntentReceiver resultTo) {
        SparseArray<UserState> startedUsers;
        synchronized (this.mLock) {
            startedUsers = this.mStartedUsers.clone();
        }
        for (int i = 0; i < startedUsers.size(); i++) {
            finishUserBoot((UserState) startedUsers.valueAt(i), resultTo);
        }
    }

    void onSystemReady() {
        updateCurrentProfileIds();
        this.mInjector.reportCurWakefulnessUsageEvent();
    }

    private void updateCurrentProfileIds() {
        int i = 0;
        List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(getCurrentUserId(), false);
        int[] currentProfileIds = new int[profiles.size()];
        for (int i2 = 0; i2 < currentProfileIds.length; i2++) {
            currentProfileIds[i2] = ((UserInfo) profiles.get(i2)).id;
        }
        List<UserInfo> users = this.mInjector.getUserManager().getUsers(false);
        synchronized (this.mLock) {
            this.mCurrentProfileIds = currentProfileIds;
            this.mUserProfileGroupIds.clear();
            while (i < users.size()) {
                UserInfo user = (UserInfo) users.get(i);
                if (user.profileGroupId != -10000) {
                    this.mUserProfileGroupIds.put(user.id, user.profileGroupId);
                }
                i++;
            }
        }
    }

    int[] getStartedUserArray() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mStartedUserArray;
        }
        return iArr;
    }

    boolean isUserRunning(int userId, int flags) {
        UserState state = getStartedUserState(userId);
        boolean z = false;
        if (state == null) {
            return false;
        }
        if ((flags & 1) != 0) {
            return true;
        }
        if ((flags & 2) != 0) {
            switch (state.state) {
                case 0:
                case 1:
                    return true;
                default:
                    return false;
            }
        } else if ((flags & 8) != 0) {
            switch (state.state) {
                case 2:
                case 3:
                    return true;
                case 4:
                case 5:
                    return StorageManager.isUserKeyUnlocked(userId);
                default:
                    return false;
            }
        } else if ((flags & 4) != 0) {
            switch (state.state) {
                case 3:
                    return true;
                case 4:
                case 5:
                    return StorageManager.isUserKeyUnlocked(userId);
                default:
                    return false;
            }
        } else {
            if (!(state.state == 4 || state.state == 5)) {
                z = true;
            }
            return z;
        }
    }

    UserInfo getCurrentUser() {
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") != 0 && this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = new StringBuilder();
            msg.append("Permission Denial: getCurrentUser() from pid=");
            msg.append(Binder.getCallingPid());
            msg.append(", uid=");
            msg.append(Binder.getCallingUid());
            msg.append(" requires ");
            msg.append("android.permission.INTERACT_ACROSS_USERS");
            msg = msg.toString();
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        } else if (this.mTargetUserId == -10000) {
            return getUserInfo(this.mCurrentUserId);
        } else {
            UserInfo currentUserLU;
            synchronized (this.mLock) {
                currentUserLU = getCurrentUserLU();
            }
            return currentUserLU;
        }
    }

    UserInfo getCurrentUserLU() {
        return getUserInfo(this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId);
    }

    int getCurrentOrTargetUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
        }
        return i;
    }

    int getCurrentOrTargetUserIdLU() {
        return this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
    }

    int getCurrentUserIdLU() {
        return this.mCurrentUserId;
    }

    int getCurrentUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mCurrentUserId;
        }
        return i;
    }

    private boolean isCurrentUserLU(int userId) {
        return userId == getCurrentOrTargetUserIdLU();
    }

    int[] getUsers() {
        UserManagerService ums = this.mInjector.getUserManager();
        if (ums != null) {
            return ums.getUserIds();
        }
        return new int[]{0};
    }

    UserInfo getUserInfo(int userId) {
        return this.mInjector.getUserManager().getUserInfo(userId);
    }

    int[] getUserIds() {
        return this.mInjector.getUserManager().getUserIds();
    }

    int[] expandUserId(int userId) {
        if (userId == -1) {
            return getUsers();
        }
        return new int[]{userId};
    }

    boolean exists(int userId) {
        return this.mInjector.getUserManager().exists(userId);
    }

    void enforceShellRestriction(String restriction, int userHandle) {
        if (Binder.getCallingUid() != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            return;
        }
        if (userHandle < 0 || hasUserRestriction(restriction, userHandle)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Shell does not have permission to access user ");
            stringBuilder.append(userHandle);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    boolean hasUserRestriction(String restriction, int userId) {
        return this.mInjector.getUserManager().hasUserRestriction(restriction, userId);
    }

    Set<Integer> getProfileIds(int userId) {
        Set<Integer> userIds = new HashSet();
        for (UserInfo user : this.mInjector.getUserManager().getProfiles(userId, false)) {
            userIds.add(Integer.valueOf(user.id));
        }
        return userIds;
    }

    boolean isSameProfileGroup(int callingUserId, int targetUserId) {
        boolean z = true;
        if (callingUserId == targetUserId) {
            return true;
        }
        synchronized (this.mLock) {
            int callingProfile = this.mUserProfileGroupIds.get(callingUserId, -10000);
            int targetProfile = this.mUserProfileGroupIds.get(targetUserId, -10000);
            if (callingProfile == -10000 || callingProfile != targetProfile) {
                z = false;
            }
        }
        return z;
    }

    boolean isUserOrItsParentRunning(int userId) {
        synchronized (this.mLock) {
            if (isUserRunning(userId, 0)) {
                return true;
            }
            int parentUserId = this.mUserProfileGroupIds.get(userId, -10000);
            if (parentUserId == -10000) {
                return false;
            }
            boolean isUserRunning = isUserRunning(parentUserId, 0);
            return isUserRunning;
        }
    }

    boolean isCurrentProfile(int userId) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mCurrentProfileIds, userId);
        }
        return contains;
    }

    int[] getCurrentProfileIds() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mCurrentProfileIds;
        }
        return iArr;
    }

    void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            int i = this.mUserProfileGroupIds.size() - 1;
            while (i >= 0) {
                if (this.mUserProfileGroupIds.keyAt(i) == userId || this.mUserProfileGroupIds.valueAt(i) == userId) {
                    this.mUserProfileGroupIds.removeAt(i);
                }
                i--;
            }
            this.mCurrentProfileIds = ArrayUtils.removeInt(this.mCurrentProfileIds, userId);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0015, code skipped:
            if (r3.mLockPatternUtils.isSeparateProfileChallengeEnabled(r4) != false) goto L_0x0018;
     */
    /* JADX WARNING: Missing block: B:10:0x0017, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:11:0x0018, code skipped:
            r0 = r3.mInjector.getKeyguardManager();
     */
    /* JADX WARNING: Missing block: B:12:0x0022, code skipped:
            if (r0.isDeviceLocked(r4) == false) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:14:0x0028, code skipped:
            if (r0.isDeviceSecure(r4) == false) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:15:0x002a, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:16:0x002c, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean shouldConfirmCredentials(int userId) {
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mStartedUsers.get(userId) == null) {
                return false;
            }
        }
    }

    boolean isLockScreenDisabled(int userId) {
        return this.mLockPatternUtils.isLockScreenDisabled(userId);
    }

    void setSwitchingFromSystemUserMessage(String switchingFromSystemUserMessage) {
        synchronized (this.mLock) {
            this.mSwitchingFromSystemUserMessage = switchingFromSystemUserMessage;
        }
    }

    void setSwitchingToSystemUserMessage(String switchingToSystemUserMessage) {
        synchronized (this.mLock) {
            this.mSwitchingToSystemUserMessage = switchingToSystemUserMessage;
        }
    }

    private String getSwitchingFromSystemUserMessage() {
        String str;
        synchronized (this.mLock) {
            str = this.mSwitchingFromSystemUserMessage;
        }
        return str;
    }

    private String getSwitchingToSystemUserMessage() {
        String str;
        synchronized (this.mLock) {
            str = this.mSwitchingToSystemUserMessage;
        }
        return str;
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        synchronized (this.mLock) {
            int i;
            long token = proto.start(fieldId);
            int i2 = 0;
            for (i = 0; i < this.mStartedUsers.size(); i++) {
                UserState uss = (UserState) this.mStartedUsers.valueAt(i);
                long uToken = proto.start(2246267895809L);
                proto.write(1120986464257L, uss.mHandle.getIdentifier());
                uss.writeToProto(proto, 1146756268034L);
                proto.end(uToken);
            }
            for (int write : this.mStartedUserArray) {
                proto.write(2220498092034L, write);
            }
            for (i = 0; i < this.mUserLru.size(); i++) {
                proto.write(2220498092035L, ((Integer) this.mUserLru.get(i)).intValue());
            }
            if (this.mUserProfileGroupIds.size() > 0) {
                while (i2 < this.mUserProfileGroupIds.size()) {
                    long uToken2 = proto.start(2246267895812L);
                    proto.write(1120986464257L, this.mUserProfileGroupIds.keyAt(i2));
                    proto.write(1120986464258L, this.mUserProfileGroupIds.valueAt(i2));
                    proto.end(uToken2);
                    i2++;
                }
            }
            proto.end(token);
        }
    }

    void dump(PrintWriter pw, boolean dumpAll) {
        synchronized (this.mLock) {
            int i;
            pw.println("  mStartedUsers:");
            int i2 = 0;
            for (i = 0; i < this.mStartedUsers.size(); i++) {
                UserState uss = (UserState) this.mStartedUsers.valueAt(i);
                pw.print("    User #");
                pw.print(uss.mHandle.getIdentifier());
                pw.print(": ");
                uss.dump(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, pw);
            }
            pw.print("  mStartedUserArray: [");
            for (i = 0; i < this.mStartedUserArray.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print(this.mStartedUserArray[i]);
            }
            pw.println("]");
            pw.print("  mUserLru: [");
            for (i = 0; i < this.mUserLru.size(); i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print(this.mUserLru.get(i));
            }
            pw.println("]");
            if (this.mUserProfileGroupIds.size() > 0) {
                pw.println("  mUserProfileGroupIds:");
                while (i2 < this.mUserProfileGroupIds.size()) {
                    pw.print("    User #");
                    pw.print(this.mUserProfileGroupIds.keyAt(i2));
                    pw.print(" -> profile #");
                    pw.println(this.mUserProfileGroupIds.valueAt(i2));
                    i2++;
                }
            }
        }
    }

    public boolean handleMessage(Message msg) {
        long startedTime = SystemClock.elapsedRealtime();
        StringBuilder stringBuilder;
        switch (msg.what) {
            case 10:
                dispatchUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                break;
            case 20:
                continueUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                break;
            case 30:
                timeoutUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                break;
            case 40:
                startProfiles();
                break;
            case 50:
                this.mInjector.batteryStatsServiceNoteEvent(32775, Integer.toString(msg.arg1), msg.arg1);
                this.mInjector.getSystemServiceManager().startUser(msg.arg1);
                stringBuilder = new StringBuilder();
                stringBuilder.append("_StartUser Handle SYSTEM_USER_START_MSG userid:");
                stringBuilder.append(msg.arg1);
                stringBuilder.append(" cost ");
                stringBuilder.append(SystemClock.elapsedRealtime() - startedTime);
                stringBuilder.append(" ms");
                Slog.i("ActivityManager", stringBuilder.toString());
                break;
            case 60:
                this.mInjector.batteryStatsServiceNoteEvent(16392, Integer.toString(msg.arg2), msg.arg2);
                this.mInjector.batteryStatsServiceNoteEvent(32776, Integer.toString(msg.arg1), msg.arg1);
                this.mInjector.getSystemServiceManager().switchUser(msg.arg1);
                stringBuilder = new StringBuilder();
                stringBuilder.append("_StartUser Handle SYSTEM_USER_CURRENT_MSG userid:");
                stringBuilder.append(msg.arg1);
                stringBuilder.append(" cost ");
                stringBuilder.append(SystemClock.elapsedRealtime() - startedTime);
                stringBuilder.append(" ms");
                Slog.i("ActivityManager", stringBuilder.toString());
                break;
            case 70:
                dispatchForegroundProfileChanged(msg.arg1);
                break;
            case 80:
                dispatchUserSwitchComplete(msg.arg1);
                break;
            case USER_SWITCH_CALLBACKS_TIMEOUT_MSG /*90*/:
                timeoutUserSwitchCallbacks(msg.arg1, msg.arg2);
                break;
            case 100:
                int userId = msg.arg1;
                this.mInjector.getSystemServiceManager().unlockUser(userId);
                FgThread.getHandler().post(new -$$Lambda$UserController$dpKWakbnwonBpCp5_FOiINcMU6s(this, userId));
                finishUserUnlocked((UserState) msg.obj);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("_StartUser Handle SYSTEM_USER_UNLOCK_MSG userid:");
                stringBuilder2.append(msg.arg1);
                stringBuilder2.append(" cost ");
                stringBuilder2.append(SystemClock.elapsedRealtime() - startedTime);
                stringBuilder2.append(" ms");
                Slog.i("ActivityManager", stringBuilder2.toString());
                break;
            case 110:
                dispatchLockedBootComplete(msg.arg1);
                break;
            case START_USER_SWITCH_FG_MSG /*120*/:
                startUserInForeground(msg.arg1);
                break;
            case 1000:
                showUserSwitchDialog((Pair) msg.obj);
                break;
        }
        return false;
    }
}
