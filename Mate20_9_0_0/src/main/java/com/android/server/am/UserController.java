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
import android.os.IRemoteCallback.Stub;
import android.os.IUserManager;
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
import java.util.concurrent.atomic.AtomicInteger;

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

    /* renamed from: com.android.server.am.UserController$8 */
    class AnonymousClass8 extends Stub {
        final /* synthetic */ ArraySet val$curWaitingUserSwitchCallbacks;
        final /* synthetic */ long val$dispatchStartedTime;
        final /* synthetic */ String val$name;
        final /* synthetic */ int val$newUserId;
        final /* synthetic */ int val$observerCount;
        final /* synthetic */ int val$oldUserId;
        final /* synthetic */ UserState val$uss;
        final /* synthetic */ AtomicInteger val$waitingCallbacksCount;

        AnonymousClass8(long j, String str, int i, ArraySet arraySet, AtomicInteger atomicInteger, int i2, UserState userState, int i3) {
            this.val$dispatchStartedTime = j;
            this.val$name = str;
            this.val$observerCount = i;
            this.val$curWaitingUserSwitchCallbacks = arraySet;
            this.val$waitingCallbacksCount = atomicInteger;
            this.val$newUserId = i2;
            this.val$uss = userState;
            this.val$oldUserId = i3;
        }

        public void sendResult(Bundle data) throws RemoteException {
            synchronized (UserController.this.mLock) {
                StringBuilder stringBuilder;
                long delay = SystemClock.elapsedRealtime() - this.val$dispatchStartedTime;
                if (delay > 3000) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("User switch timeout: observer ");
                    stringBuilder.append(this.val$name);
                    stringBuilder.append(" sent result after ");
                    stringBuilder.append(delay);
                    stringBuilder.append(" ms");
                    Slog.e("ActivityManager", stringBuilder.toString());
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("_StartUser User switch done: observer ");
                stringBuilder.append(this.val$name);
                stringBuilder.append(" sent result after ");
                stringBuilder.append(delay);
                stringBuilder.append(" ms, total:");
                stringBuilder.append(this.val$observerCount);
                Slog.d("ActivityManager", stringBuilder.toString());
                this.val$curWaitingUserSwitchCallbacks.remove(this.val$name);
                if (this.val$waitingCallbacksCount.decrementAndGet() == 0 && this.val$curWaitingUserSwitchCallbacks == UserController.this.mCurWaitingUserSwitchCallbacks) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("_StartUser dispatchUserSwitch userid:");
                    stringBuilder.append(this.val$newUserId);
                    stringBuilder.append(" cost ");
                    stringBuilder.append(SystemClock.elapsedRealtime() - this.val$dispatchStartedTime);
                    stringBuilder.append(" ms");
                    Slog.i("ActivityManager", stringBuilder.toString());
                    UserController.this.sendContinueUserSwitchLU(this.val$uss, this.val$oldUserId, this.val$newUserId);
                }
            }
        }
    }

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
                this.mUserManager = (UserManagerService) IUserManager.Stub.asInterface(ServiceManager.getService("user"));
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
            return this.mService.isHiddenSpaceSwitch(first, second) && getKeyguardManager().isKeyguardLocked();
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

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.fixSplitterBlock(BlockFinish.java:45)
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:29)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    void dispatchUserSwitch(com.android.server.am.UserState r22, int r23, int r24) {
        /*
        r21 = this;
        r12 = r21;
        r13 = r24;
        r0 = "ActivityManager";
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Dispatch onUserSwitching oldUser #";
        r1.append(r2);
        r14 = r23;
        r1.append(r14);
        r2 = " newUser #";
        r1.append(r2);
        r1.append(r13);
        r1 = r1.toString();
        android.util.Slog.d(r0, r1);
        r0 = r12.mUserSwitchObservers;
        r15 = r0.beginBroadcast();
        if (r15 <= 0) goto L_0x00bd;
    L_0x002c:
        r0 = new android.util.ArraySet;
        r0.<init>();
        r11 = r0;
        r1 = r12.mLock;
        monitor-enter(r1);
        r0 = 1;
        r10 = r22;
        r10.switching = r0;	 Catch:{ all -> 0x00b4 }
        r12.mCurWaitingUserSwitchCallbacks = r11;	 Catch:{ all -> 0x00b4 }
        monitor-exit(r1);	 Catch:{ all -> 0x00b4 }
        r8 = new java.util.concurrent.atomic.AtomicInteger;
        r8.<init>(r15);
        r16 = android.os.SystemClock.elapsedRealtime();
        r0 = 0;
    L_0x0047:
        r9 = r0;
        if (r9 >= r15) goto L_0x00b1;
    L_0x004a:
        r0 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x00a2 }
        r0.<init>();	 Catch:{ RemoteException -> 0x00a2 }
        r1 = "#";	 Catch:{ RemoteException -> 0x00a2 }
        r0.append(r1);	 Catch:{ RemoteException -> 0x00a2 }
        r0.append(r9);	 Catch:{ RemoteException -> 0x00a2 }
        r1 = " ";	 Catch:{ RemoteException -> 0x00a2 }
        r0.append(r1);	 Catch:{ RemoteException -> 0x00a2 }
        r1 = r12.mUserSwitchObservers;	 Catch:{ RemoteException -> 0x00a2 }
        r1 = r1.getBroadcastCookie(r9);	 Catch:{ RemoteException -> 0x00a2 }
        r0.append(r1);	 Catch:{ RemoteException -> 0x00a2 }
        r0 = r0.toString();	 Catch:{ RemoteException -> 0x00a2 }
        r7 = r0;	 Catch:{ RemoteException -> 0x00a2 }
        r1 = r12.mLock;	 Catch:{ RemoteException -> 0x00a2 }
        monitor-enter(r1);	 Catch:{ RemoteException -> 0x00a2 }
        r11.add(r7);	 Catch:{ all -> 0x0094, RemoteException -> 0x009e }
        monitor-exit(r1);	 Catch:{ all -> 0x0094, RemoteException -> 0x009e }
        r0 = new com.android.server.am.UserController$8;	 Catch:{ RemoteException -> 0x00a2 }
        r1 = r0;
        r2 = r12;
        r3 = r16;
        r5 = r7;
        r6 = r15;
        r18 = r7;
        r7 = r11;
        r19 = r15;
        r15 = r9;
        r9 = r13;
        r10 = r22;
        r20 = r11;
        r11 = r14;
        r1.<init>(r3, r5, r6, r7, r8, r9, r10, r11);	 Catch:{ all -> 0x0094, RemoteException -> 0x009e }
        r1 = r12.mUserSwitchObservers;	 Catch:{ all -> 0x0094, RemoteException -> 0x009e }
        r1 = r1.getBroadcastItem(r15);	 Catch:{ all -> 0x0094, RemoteException -> 0x009e }
        r1 = (android.app.IUserSwitchObserver) r1;	 Catch:{ all -> 0x0094, RemoteException -> 0x009e }
        r1.onUserSwitching(r13, r0);	 Catch:{ all -> 0x0094, RemoteException -> 0x009e }
        goto L_0x00a8;
    L_0x0094:
        r0 = move-exception;
        r18 = r7;
        r20 = r11;
        r19 = r15;
        r15 = r9;
    L_0x009c:
        monitor-exit(r1);	 Catch:{ all -> 0x00a0 }
        throw r0;	 Catch:{ all -> 0x0094, RemoteException -> 0x009e }
    L_0x009e:
        r0 = move-exception;
        goto L_0x00a8;
    L_0x00a0:
        r0 = move-exception;
        goto L_0x009c;
    L_0x00a2:
        r0 = move-exception;
        r20 = r11;
        r19 = r15;
        r15 = r9;
    L_0x00a8:
        r0 = r15 + 1;
        r10 = r22;
        r15 = r19;
        r11 = r20;
        goto L_0x0047;
    L_0x00b1:
        r19 = r15;
        goto L_0x00c6;
    L_0x00b4:
        r0 = move-exception;
        r20 = r11;
        r19 = r15;
    L_0x00b9:
        monitor-exit(r1);	 Catch:{ all -> 0x00bb }
        throw r0;
    L_0x00bb:
        r0 = move-exception;
        goto L_0x00b9;
    L_0x00bd:
        r19 = r15;
        r1 = r12.mLock;
        monitor-enter(r1);
        r21.sendContinueUserSwitchLU(r22, r23, r24);
        monitor-exit(r1);
    L_0x00c6:
        r0 = r12.mUserSwitchObservers;
        r0.finishBroadcast();
        return;
    L_0x00cc:
        r0 = move-exception;
        monitor-exit(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.UserController.dispatchUserSwitch(com.android.server.am.UserState, int, int):void");
    }

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.fixSplitterBlock(BlockFinish.java:45)
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:29)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    boolean startUser(int r50, boolean r51, android.os.IProgressListener r52) {
        /*
        r49 = this;
        r1 = r49;
        r15 = r50;
        r14 = r51;
        r13 = r52;
        r0 = r1.mInjector;
        r2 = "android.permission.INTERACT_ACROSS_USERS_FULL";
        r0 = r0.checkCallingPermission(r2);
        if (r0 != 0) goto L_0x042b;
    L_0x0012:
        r11 = android.os.SystemClock.elapsedRealtime();
        r1.SwitchUser_Time = r11;
        r0 = "ActivityManager";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Starting userid:";
        r2.append(r3);
        r2.append(r15);
        r3 = " fg:";
        r2.append(r3);
        r2.append(r14);
        r2 = r2.toString();
        android.util.Slog.i(r0, r2);
        r2 = android.os.Binder.clearCallingIdentity();
        r9 = r2;
        r0 = r49.getCurrentUserId();	 Catch:{ all -> 0x0420 }
        r8 = r0;
        r0 = 1;
        if (r8 != r15) goto L_0x0048;
        android.os.Binder.restoreCallingIdentity(r9);
        return r0;
    L_0x0048:
        if (r14 == 0) goto L_0x005c;
    L_0x004a:
        r2 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r3 = "startUser";	 Catch:{ all -> 0x0053 }
        r2.clearAllLockedTasks(r3);	 Catch:{ all -> 0x0053 }
        goto L_0x005c;
    L_0x0053:
        r0 = move-exception;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
        goto L_0x0427;
    L_0x005c:
        r2 = r49.getUserInfo(r50);	 Catch:{ all -> 0x0420 }
        r7 = r2;
        r2 = 0;
        if (r7 != 0) goto L_0x007f;
    L_0x0064:
        r0 = "ActivityManager";	 Catch:{ all -> 0x0053 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0053 }
        r3.<init>();	 Catch:{ all -> 0x0053 }
        r4 = "No user info for user #";	 Catch:{ all -> 0x0053 }
        r3.append(r4);	 Catch:{ all -> 0x0053 }
        r3.append(r15);	 Catch:{ all -> 0x0053 }
        r3 = r3.toString();	 Catch:{ all -> 0x0053 }
        android.util.Slog.w(r0, r3);	 Catch:{ all -> 0x0053 }
        android.os.Binder.restoreCallingIdentity(r9);
        return r2;
    L_0x007f:
        if (r14 == 0) goto L_0x00a7;
    L_0x0081:
        r3 = r7.isManagedProfile();	 Catch:{ all -> 0x0053 }
        if (r3 == 0) goto L_0x00a7;	 Catch:{ all -> 0x0053 }
    L_0x0087:
        r0 = "ActivityManager";	 Catch:{ all -> 0x0053 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0053 }
        r3.<init>();	 Catch:{ all -> 0x0053 }
        r4 = "Cannot switch to User #";	 Catch:{ all -> 0x0053 }
        r3.append(r4);	 Catch:{ all -> 0x0053 }
        r3.append(r15);	 Catch:{ all -> 0x0053 }
        r4 = ": not a full user";	 Catch:{ all -> 0x0053 }
        r3.append(r4);	 Catch:{ all -> 0x0053 }
        r3 = r3.toString();	 Catch:{ all -> 0x0053 }
        android.util.Slog.w(r0, r3);	 Catch:{ all -> 0x0053 }
        android.os.Binder.restoreCallingIdentity(r9);
        return r2;
    L_0x00a7:
        r3 = r1.mInjector;	 Catch:{ all -> 0x0420 }
        r3 = r3.getWindowManager();	 Catch:{ all -> 0x0420 }
        r1.setMultiDpi(r3, r15);	 Catch:{ all -> 0x0420 }
        r3 = r1.mCurrentUserId;	 Catch:{ all -> 0x0420 }
        r3 = r1.getUserInfo(r3);	 Catch:{ all -> 0x0420 }
        r6 = r3;
        if (r14 == 0) goto L_0x00d4;
    L_0x00b9:
        r3 = r1.mUserSwitchUiEnabled;	 Catch:{ all -> 0x0053 }
        if (r3 == 0) goto L_0x00d4;	 Catch:{ all -> 0x0053 }
    L_0x00bd:
        r3 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r3 = r3.shouldSkipKeyguard(r6, r7);	 Catch:{ all -> 0x0053 }
        if (r3 != 0) goto L_0x00d4;	 Catch:{ all -> 0x0053 }
    L_0x00c5:
        r3 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r3 = r3.getWindowManager();	 Catch:{ all -> 0x0053 }
        r4 = 17432717; // 0x10a008d float:2.5346992E-38 double:8.6129066E-317;	 Catch:{ all -> 0x0053 }
        r5 = 17432716; // 0x10a008c float:2.534699E-38 double:8.612906E-317;	 Catch:{ all -> 0x0053 }
        r3.startFreezingScreen(r4, r5);	 Catch:{ all -> 0x0053 }
    L_0x00d4:
        r3 = 0;
        r4 = 0;
        r5 = r1.mLock;	 Catch:{ all -> 0x0420 }
        monitor-enter(r5);	 Catch:{ all -> 0x0420 }
        r2 = r1.mStartedUsers;	 Catch:{ all -> 0x040a }
        r2 = r2.get(r15);	 Catch:{ all -> 0x040a }
        r2 = (com.android.server.am.UserState) r2;	 Catch:{ all -> 0x040a }
        if (r2 != 0) goto L_0x012f;
    L_0x00e3:
        r0 = new com.android.server.am.UserState;	 Catch:{ all -> 0x011d }
        r20 = r3;
        r3 = android.os.UserHandle.of(r50);	 Catch:{ all -> 0x010b }
        r0.<init>(r3);	 Catch:{ all -> 0x010b }
        r2 = r0;	 Catch:{ all -> 0x010b }
        r0 = r2.mUnlockProgress;	 Catch:{ all -> 0x010b }
        r3 = new com.android.server.am.UserController$UserProgressListener;	 Catch:{ all -> 0x010b }
        r21 = r4;
        r4 = 0;
        r3.<init>(r4);	 Catch:{ all -> 0x0169 }
        r0.addListener(r3);	 Catch:{ all -> 0x0169 }
        r0 = r1.mStartedUsers;	 Catch:{ all -> 0x0169 }
        r0.put(r15, r2);	 Catch:{ all -> 0x0169 }
        r49.updateStartedUserArrayLU();	 Catch:{ all -> 0x0169 }
        r0 = 1;
        r4 = 1;
        r3 = r0;
        r21 = r4;
        goto L_0x017d;
    L_0x010b:
        r0 = move-exception;
        r21 = r4;
        r22 = r6;
        r30 = r7;
        r7 = r8;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
        r3 = r20;
        goto L_0x041a;
    L_0x011d:
        r0 = move-exception;
        r20 = r3;
        r21 = r4;
        r22 = r6;
        r30 = r7;
        r7 = r8;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
        goto L_0x041a;
    L_0x012f:
        r20 = r3;
        r21 = r4;
        r0 = r2.state;	 Catch:{ all -> 0x03f9 }
        r3 = 5;
        if (r0 != r3) goto L_0x017b;
    L_0x0138:
        r0 = r49.isCallingOnHandlerThread();	 Catch:{ all -> 0x0169 }
        if (r0 != 0) goto L_0x017b;	 Catch:{ all -> 0x0169 }
    L_0x013e:
        r0 = "ActivityManager";	 Catch:{ all -> 0x0169 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0169 }
        r3.<init>();	 Catch:{ all -> 0x0169 }
        r4 = "User #";	 Catch:{ all -> 0x0169 }
        r3.append(r4);	 Catch:{ all -> 0x0169 }
        r3.append(r15);	 Catch:{ all -> 0x0169 }
        r4 = " is shutting down - will start after full stop";	 Catch:{ all -> 0x0169 }
        r3.append(r4);	 Catch:{ all -> 0x0169 }
        r3 = r3.toString();	 Catch:{ all -> 0x0169 }
        android.util.Slog.i(r0, r3);	 Catch:{ all -> 0x0169 }
        r0 = r1.mHandler;	 Catch:{ all -> 0x0169 }
        r3 = new com.android.server.am.-$$Lambda$UserController$itozNmdxq9RsTKqW4_f-sH8yPdY;	 Catch:{ all -> 0x0169 }
        r3.<init>(r1, r15, r14, r13);	 Catch:{ all -> 0x0169 }
        r0.post(r3);	 Catch:{ all -> 0x0169 }
        monitor-exit(r5);	 Catch:{ all -> 0x0169 }
        android.os.Binder.restoreCallingIdentity(r9);
        r0 = 1;
        return r0;
    L_0x0169:
        r0 = move-exception;
        r22 = r6;
        r30 = r7;
        r7 = r8;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
        r3 = r20;
    L_0x0177:
        r4 = r21;
        goto L_0x041a;
    L_0x017b:
        r3 = r20;
    L_0x017d:
        r0 = java.lang.Integer.valueOf(r50);	 Catch:{ all -> 0x03ec }
        r4 = r0.intValue();	 Catch:{ all -> 0x03ec }
        r4 = r1.getUserInfo(r4);	 Catch:{ all -> 0x03ec }
        if (r4 == 0) goto L_0x01a7;
    L_0x018b:
        r4 = r0.intValue();	 Catch:{ all -> 0x019a }
        r4 = r1.getUserInfo(r4);	 Catch:{ all -> 0x019a }
        r4 = r4.isClonedProfile();	 Catch:{ all -> 0x019a }
        if (r4 != 0) goto L_0x01b1;
    L_0x0199:
        goto L_0x01a7;
    L_0x019a:
        r0 = move-exception;
        r22 = r6;
        r30 = r7;
        r7 = r8;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
        goto L_0x0177;
    L_0x01a7:
        r4 = r1.mUserLru;	 Catch:{ all -> 0x03ec }
        r4.remove(r0);	 Catch:{ all -> 0x03ec }
        r4 = r1.mUserLru;	 Catch:{ all -> 0x03ec }
        r4.add(r0);	 Catch:{ all -> 0x03ec }
    L_0x01b1:
        monitor-exit(r5);	 Catch:{ all -> 0x03ec }
        r5 = r2;
        if (r13 == 0) goto L_0x01ba;
    L_0x01b5:
        r0 = r5.mUnlockProgress;	 Catch:{ all -> 0x0053 }
        r0.addListener(r13);	 Catch:{ all -> 0x0053 }
    L_0x01ba:
        if (r21 == 0) goto L_0x01c7;	 Catch:{ all -> 0x0053 }
    L_0x01bc:
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0 = r0.getUserManagerInternal();	 Catch:{ all -> 0x0053 }
        r2 = r5.state;	 Catch:{ all -> 0x0053 }
        r0.setUserState(r15, r2);	 Catch:{ all -> 0x0053 }
    L_0x01c7:
        if (r14 == 0) goto L_0x021b;	 Catch:{ all -> 0x0053 }
    L_0x01c9:
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r2 = 16;	 Catch:{ all -> 0x0053 }
        r0.reportGlobalUsageEventLocked(r2);	 Catch:{ all -> 0x0053 }
        r2 = r1.mLock;	 Catch:{ all -> 0x0053 }
        monitor-enter(r2);	 Catch:{ all -> 0x0053 }
        r1.mCurrentUserId = r15;	 Catch:{ all -> 0x0053 }
        r0 = -10000; // 0xffffffffffffd8f0 float:NaN double:NaN;	 Catch:{ all -> 0x0053 }
        r1.mTargetUserId = r0;	 Catch:{ all -> 0x0053 }
        monitor-exit(r2);	 Catch:{ all -> 0x0053 }
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0.updateUserConfiguration();	 Catch:{ all -> 0x0053 }
        r49.updateCurrentProfileIds();	 Catch:{ all -> 0x0053 }
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0 = r0.getWindowManager();	 Catch:{ all -> 0x0053 }
        r2 = r49.getCurrentProfileIds();	 Catch:{ all -> 0x0053 }
        r0.setCurrentUser(r15, r2);	 Catch:{ all -> 0x0053 }
        android.hwtheme.HwThemeManager.linkDataSkinDirAsUser(r50);	 Catch:{ all -> 0x0053 }
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0.reportCurWakefulnessUsageEvent();	 Catch:{ all -> 0x0053 }
        r0 = r1.mUserSwitchUiEnabled;	 Catch:{ all -> 0x0053 }
        if (r0 == 0) goto L_0x0240;	 Catch:{ all -> 0x0053 }
    L_0x01fb:
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0 = r0.shouldSkipKeyguard(r6, r7);	 Catch:{ all -> 0x0053 }
        if (r0 != 0) goto L_0x0240;	 Catch:{ all -> 0x0053 }
    L_0x0203:
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0 = r0.getWindowManager();	 Catch:{ all -> 0x0053 }
        r2 = 1;	 Catch:{ all -> 0x0053 }
        r0.setSwitchingUser(r2);	 Catch:{ all -> 0x0053 }
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0 = r0.getWindowManager();	 Catch:{ all -> 0x0053 }
        r2 = 0;	 Catch:{ all -> 0x0053 }
        r0.lockNow(r2);	 Catch:{ all -> 0x0053 }
        goto L_0x0240;
    L_0x0218:
        r0 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x0053 }
        throw r0;	 Catch:{ all -> 0x0053 }
    L_0x021b:
        r0 = r1.mCurrentUserId;	 Catch:{ all -> 0x0420 }
        r0 = java.lang.Integer.valueOf(r0);	 Catch:{ all -> 0x0420 }
        r2 = r0;	 Catch:{ all -> 0x0420 }
        r49.updateCurrentProfileIds();	 Catch:{ all -> 0x0420 }
        r0 = r1.mInjector;	 Catch:{ all -> 0x0420 }
        r0 = r0.getWindowManager();	 Catch:{ all -> 0x0420 }
        r4 = r49.getCurrentProfileIds();	 Catch:{ all -> 0x0420 }
        r0.setCurrentProfileIds(r4);	 Catch:{ all -> 0x0420 }
        r4 = r1.mLock;	 Catch:{ all -> 0x0420 }
        monitor-enter(r4);	 Catch:{ all -> 0x0420 }
        r0 = r1.mUserLru;	 Catch:{ all -> 0x03dc, all -> 0x041c }
        r0.remove(r2);	 Catch:{ all -> 0x03dc, all -> 0x041c }
        r0 = r1.mUserLru;	 Catch:{ all -> 0x03dc, all -> 0x041c }
        r0.add(r2);	 Catch:{ all -> 0x03dc, all -> 0x041c }
        monitor-exit(r4);	 Catch:{ all -> 0x03dc, all -> 0x041c }
    L_0x0240:
        r0 = r5.state;	 Catch:{ all -> 0x0420 }
        r2 = 4;
        if (r0 != r2) goto L_0x0261;
    L_0x0245:
        r0 = r5.lastState;	 Catch:{ all -> 0x0053 }
        r5.setState(r0);	 Catch:{ all -> 0x0053 }
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0 = r0.getUserManagerInternal();	 Catch:{ all -> 0x0053 }
        r2 = r5.state;	 Catch:{ all -> 0x0053 }
        r0.setUserState(r15, r2);	 Catch:{ all -> 0x0053 }
        r2 = r1.mLock;	 Catch:{ all -> 0x0053 }
        monitor-enter(r2);	 Catch:{ all -> 0x0053 }
        r49.updateStartedUserArrayLU();	 Catch:{ all -> 0x0053 }
        monitor-exit(r2);	 Catch:{ all -> 0x0053 }
        r3 = 1;	 Catch:{ all -> 0x0053 }
        goto L_0x0281;	 Catch:{ all -> 0x0053 }
    L_0x025e:
        r0 = move-exception;	 Catch:{ all -> 0x0053 }
        monitor-exit(r2);	 Catch:{ all -> 0x0053 }
        throw r0;	 Catch:{ all -> 0x0053 }
    L_0x0261:
        r0 = r5.state;	 Catch:{ all -> 0x0420 }
        r2 = 5;
        if (r0 != r2) goto L_0x0281;
    L_0x0266:
        r0 = 0;
        r5.setState(r0);	 Catch:{ all -> 0x0053 }
        r0 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r0 = r0.getUserManagerInternal();	 Catch:{ all -> 0x0053 }
        r2 = r5.state;	 Catch:{ all -> 0x0053 }
        r0.setUserState(r15, r2);	 Catch:{ all -> 0x0053 }
        r2 = r1.mLock;	 Catch:{ all -> 0x0053 }
        monitor-enter(r2);	 Catch:{ all -> 0x0053 }
        r49.updateStartedUserArrayLU();	 Catch:{ all -> 0x0053 }
        monitor-exit(r2);	 Catch:{ all -> 0x0053 }
        r3 = 1;	 Catch:{ all -> 0x0053 }
        goto L_0x0281;	 Catch:{ all -> 0x0053 }
    L_0x027e:
        r0 = move-exception;	 Catch:{ all -> 0x0053 }
        monitor-exit(r2);	 Catch:{ all -> 0x0053 }
        throw r0;	 Catch:{ all -> 0x0053 }
    L_0x0281:
        r0 = r3;
        r2 = r5.state;	 Catch:{ all -> 0x0420 }
        if (r2 != 0) goto L_0x02a0;
    L_0x0286:
        r2 = r1.mInjector;	 Catch:{ all -> 0x0053 }
        r2 = r2.getUserManager();	 Catch:{ all -> 0x0053 }
        r2.onBeforeStartUser(r15);	 Catch:{ all -> 0x0053 }
        r2 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r3 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r4 = 50;	 Catch:{ all -> 0x0053 }
        r22 = r6;	 Catch:{ all -> 0x0053 }
        r6 = 0;	 Catch:{ all -> 0x0053 }
        r3 = r3.obtainMessage(r4, r15, r6);	 Catch:{ all -> 0x0053 }
        r2.sendMessage(r3);	 Catch:{ all -> 0x0053 }
        goto L_0x02a2;	 Catch:{ all -> 0x0053 }
    L_0x02a0:
        r22 = r6;	 Catch:{ all -> 0x0053 }
    L_0x02a2:
        if (r14 == 0) goto L_0x02da;	 Catch:{ all -> 0x0053 }
    L_0x02a4:
        r2 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r3 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r4 = 60;	 Catch:{ all -> 0x0053 }
        r3 = r3.obtainMessage(r4, r15, r8);	 Catch:{ all -> 0x0053 }
        r2.sendMessage(r3);	 Catch:{ all -> 0x0053 }
        r2 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r3 = 10;	 Catch:{ all -> 0x0053 }
        r2.removeMessages(r3);	 Catch:{ all -> 0x0053 }
        r2 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r4 = 30;	 Catch:{ all -> 0x0053 }
        r2.removeMessages(r4);	 Catch:{ all -> 0x0053 }
        r2 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r6 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r3 = r6.obtainMessage(r3, r8, r15, r5);	 Catch:{ all -> 0x0053 }
        r2.sendMessage(r3);	 Catch:{ all -> 0x0053 }
        r2 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r3 = r1.mHandler;	 Catch:{ all -> 0x0053 }
        r3 = r3.obtainMessage(r4, r8, r15, r5);	 Catch:{ all -> 0x0053 }
        r23 = r5;	 Catch:{ all -> 0x0053 }
        r4 = 3000; // 0xbb8 float:4.204E-42 double:1.482E-320;	 Catch:{ all -> 0x0053 }
        r2.sendMessageDelayed(r3, r4);	 Catch:{ all -> 0x0053 }
        goto L_0x02dc;
    L_0x02da:
        r23 = r5;
    L_0x02dc:
        if (r0 == 0) goto L_0x033f;
    L_0x02de:
        r2 = new android.content.Intent;	 Catch:{ all -> 0x0335 }
        r3 = "android.intent.action.USER_STARTED";	 Catch:{ all -> 0x0335 }
        r2.<init>(r3);	 Catch:{ all -> 0x0335 }
        r6 = r2;	 Catch:{ all -> 0x0335 }
        r2 = 1342177280; // 0x50000000 float:8.5899346E9 double:6.631236847E-315;	 Catch:{ all -> 0x0335 }
        r6.addFlags(r2);	 Catch:{ all -> 0x0335 }
        r2 = "android.intent.extra.user_handle";	 Catch:{ all -> 0x0335 }
        r6.putExtra(r2, r15);	 Catch:{ all -> 0x0335 }
        r2 = r1.mInjector;	 Catch:{ all -> 0x0335 }
        r4 = 0;	 Catch:{ all -> 0x0335 }
        r5 = 0;	 Catch:{ all -> 0x0335 }
        r16 = 0;	 Catch:{ all -> 0x0335 }
        r17 = 0;	 Catch:{ all -> 0x0335 }
        r18 = 0;	 Catch:{ all -> 0x0335 }
        r19 = 0;	 Catch:{ all -> 0x0335 }
        r20 = -1;	 Catch:{ all -> 0x0335 }
        r24 = 0;	 Catch:{ all -> 0x0335 }
        r25 = 0;	 Catch:{ all -> 0x0335 }
        r26 = 0;	 Catch:{ all -> 0x0335 }
        r27 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x0335 }
        r28 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r3 = r6;
        r29 = r23;
        r23 = r6;
        r6 = r16;
        r30 = r7;
        r7 = r17;
        r31 = r8;
        r8 = r18;
        r32 = r9;
        r9 = r19;
        r10 = r20;
        r18 = r11;
        r11 = r24;
        r12 = r25;
        r13 = r26;
        r17 = r14;
        r14 = r27;
        r15 = r28;
        r16 = r50;
        r2.broadcastIntent(r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15, r16);	 Catch:{ all -> 0x0331 }
        goto L_0x034b;
    L_0x0331:
        r0 = move-exception;
        r6 = r50;
        goto L_0x0358;
    L_0x0335:
        r0 = move-exception;
        r18 = r11;
        r17 = r14;
        r6 = r50;
        r8 = r9;
        goto L_0x0427;
    L_0x033f:
        r30 = r7;
        r31 = r8;
        r32 = r9;
        r18 = r11;
        r17 = r14;
        r29 = r23;
    L_0x034b:
        if (r17 == 0) goto L_0x035c;
    L_0x034d:
        r6 = r50;
        r5 = r29;
        r7 = r31;
        r1.moveUserToForeground(r5, r7, r6);	 Catch:{ all -> 0x0357 }
        goto L_0x0365;
    L_0x0357:
        r0 = move-exception;
    L_0x0358:
        r8 = r32;
        goto L_0x0427;
    L_0x035c:
        r6 = r50;
        r5 = r29;
        r7 = r31;
        r1.finishUserBoot(r5);	 Catch:{ all -> 0x03d7 }
    L_0x0365:
        if (r0 == 0) goto L_0x03a4;
    L_0x0367:
        r2 = new android.content.Intent;	 Catch:{ all -> 0x0357 }
        r3 = "android.intent.action.USER_STARTING";	 Catch:{ all -> 0x0357 }
        r2.<init>(r3);	 Catch:{ all -> 0x0357 }
        r3 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;	 Catch:{ all -> 0x0357 }
        r2.addFlags(r3);	 Catch:{ all -> 0x0357 }
        r3 = "android.intent.extra.user_handle";	 Catch:{ all -> 0x0357 }
        r2.putExtra(r3, r6);	 Catch:{ all -> 0x0357 }
        r3 = r1.mInjector;	 Catch:{ all -> 0x0357 }
        r36 = 0;	 Catch:{ all -> 0x0357 }
        r4 = new com.android.server.am.UserController$6;	 Catch:{ all -> 0x0357 }
        r4.<init>();	 Catch:{ all -> 0x0357 }
        r38 = 0;	 Catch:{ all -> 0x0357 }
        r39 = 0;	 Catch:{ all -> 0x0357 }
        r40 = 0;	 Catch:{ all -> 0x0357 }
        r8 = "android.permission.INTERACT_ACROSS_USERS";	 Catch:{ all -> 0x0357 }
        r41 = new java.lang.String[]{r8};	 Catch:{ all -> 0x0357 }
        r42 = -1;	 Catch:{ all -> 0x0357 }
        r43 = 0;	 Catch:{ all -> 0x0357 }
        r44 = 1;	 Catch:{ all -> 0x0357 }
        r45 = 0;	 Catch:{ all -> 0x0357 }
        r46 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x0357 }
        r47 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;	 Catch:{ all -> 0x0357 }
        r48 = -1;	 Catch:{ all -> 0x0357 }
        r34 = r3;	 Catch:{ all -> 0x0357 }
        r35 = r2;	 Catch:{ all -> 0x0357 }
        r37 = r4;	 Catch:{ all -> 0x0357 }
        r34.broadcastIntent(r35, r36, r37, r38, r39, r40, r41, r42, r43, r44, r45, r46, r47, r48);	 Catch:{ all -> 0x0357 }
    L_0x03a4:
        r1.isColdStart = r0;	 Catch:{ all -> 0x03d7 }
        r8 = r32;
        android.os.Binder.restoreCallingIdentity(r8);
        r0 = "ActivityManager";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "_StartUser startUser userid:";
        r2.append(r3);
        r2.append(r6);
        r3 = " cost ";
        r2.append(r3);
        r3 = android.os.SystemClock.elapsedRealtime();
        r3 = r3 - r18;
        r2.append(r3);
        r3 = " ms";
        r2.append(r3);
        r2 = r2.toString();
        android.util.Slog.i(r0, r2);
        r0 = 1;
        return r0;
    L_0x03d7:
        r0 = move-exception;
        r8 = r32;
        goto L_0x0427;
    L_0x03dc:
        r0 = move-exception;
        r22 = r6;
        r30 = r7;
        r7 = r8;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
    L_0x03e8:
        monitor-exit(r4);	 Catch:{ all -> 0x03ea }
        throw r0;	 Catch:{ all -> 0x03dc, all -> 0x041c }
    L_0x03ea:
        r0 = move-exception;
        goto L_0x03e8;
    L_0x03ec:
        r0 = move-exception;
        r22 = r6;
        r30 = r7;
        r7 = r8;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
        goto L_0x0407;
    L_0x03f9:
        r0 = move-exception;
        r22 = r6;
        r30 = r7;
        r7 = r8;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
        r3 = r20;
    L_0x0407:
        r4 = r21;
        goto L_0x041a;
    L_0x040a:
        r0 = move-exception;
        r20 = r3;
        r21 = r4;
        r22 = r6;
        r30 = r7;
        r7 = r8;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
    L_0x041a:
        monitor-exit(r5);	 Catch:{ all -> 0x041e }
        throw r0;	 Catch:{ all -> 0x03dc, all -> 0x041c }
    L_0x041c:
        r0 = move-exception;
        goto L_0x0427;
    L_0x041e:
        r0 = move-exception;
        goto L_0x041a;
    L_0x0420:
        r0 = move-exception;
        r8 = r9;
        r18 = r11;
        r17 = r14;
        r6 = r15;
    L_0x0427:
        android.os.Binder.restoreCallingIdentity(r8);
        throw r0;
    L_0x042b:
        r17 = r14;
        r6 = r15;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r2 = "Permission Denial: switchUser() from pid=";
        r0.append(r2);
        r2 = android.os.Binder.getCallingPid();
        r0.append(r2);
        r2 = ", uid=";
        r0.append(r2);
        r2 = android.os.Binder.getCallingUid();
        r0.append(r2);
        r2 = " requires ";
        r0.append(r2);
        r2 = "android.permission.INTERACT_ACROSS_USERS_FULL";
        r0.append(r2);
        r0 = r0.toString();
        r2 = "ActivityManager";
        android.util.Slog.w(r2, r0);
        r2 = new java.lang.SecurityException;
        r2.<init>(r0);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.UserController.startUser(int, boolean, android.os.IProgressListener):boolean");
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

    /* JADX WARNING: Missing block: B:13:0x0038, code:
            if (r2.setState(0, 1) == false) goto L_0x00c8;
     */
    /* JADX WARNING: Missing block: B:14:0x003a, code:
            r1.mInjector.getUserManagerInternal().setUserState(r15, r2.state);
     */
    /* JADX WARNING: Missing block: B:15:0x0045, code:
            if (r15 != 0) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:17:0x004d, code:
            if (r1.mInjector.isRuntimeRestarted() != false) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:19:0x0055, code:
            if (r1.mInjector.isFirstBootOrUpgrade() != false) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:20:0x0057, code:
            r0 = (int) (android.os.SystemClock.elapsedRealtime() / 1000);
            com.android.internal.logging.MetricsLogger.histogram(r1.mInjector.getContext(), "framework_locked_boot_completed", r0);
     */
    /* JADX WARNING: Missing block: B:21:0x006e, code:
            if (r0 <= START_USER_SWITCH_FG_MSG) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:22:0x0070, code:
            r6 = new java.lang.StringBuilder();
            r6.append("finishUserBoot took too long. uptimeSeconds=");
            r6.append(r0);
            android.util.Slog.wtf("SystemServerTiming", r6.toString());
     */
    /* JADX WARNING: Missing block: B:23:0x0086, code:
            r1.mHandler.sendMessage(r1.mHandler.obtainMessage(110, r15, 0));
            r0 = new android.content.Intent("android.intent.action.LOCKED_BOOT_COMPLETED", null);
            r0.putExtra("android.intent.extra.user_handle", r15);
            r0.addFlags(150994944);
            r18 = r15;
            r1.mInjector.broadcastIntent(r0, null, r21, 0, null, null, new java.lang.String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r18);
     */
    /* JADX WARNING: Missing block: B:24:0x00c8, code:
            r18 = r15;
     */
    /* JADX WARNING: Missing block: B:25:0x00ca, code:
            r4 = r18;
     */
    /* JADX WARNING: Missing block: B:26:0x00d6, code:
            if (r1.mInjector.getUserManager().isManagedProfile(r4) != false) goto L_0x00e9;
     */
    /* JADX WARNING: Missing block: B:28:0x00e2, code:
            if (r1.mInjector.getUserManager().isClonedProfile(r4) == false) goto L_0x00e5;
     */
    /* JADX WARNING: Missing block: B:29:0x00e5, code:
            maybeUnlockUser(r4);
     */
    /* JADX WARNING: Missing block: B:30:0x00e9, code:
            r0 = r1.mInjector.getUserManager().getProfileParent(r4);
     */
    /* JADX WARNING: Missing block: B:31:0x00f3, code:
            if (r0 == null) goto L_0x0127;
     */
    /* JADX WARNING: Missing block: B:33:0x00fc, code:
            if (isUserRunning(r0.id, 4) == false) goto L_0x0127;
     */
    /* JADX WARNING: Missing block: B:34:0x00fe, code:
            r5 = new java.lang.StringBuilder();
            r5.append("User ");
            r5.append(r4);
            r5.append(" (parent ");
            r5.append(r0.id);
            r5.append("): attempting unlock because parent is unlocked");
            android.util.Slog.d("ActivityManager", r5.toString());
            maybeUnlockUser(r4);
     */
    /* JADX WARNING: Missing block: B:35:0x0127, code:
            if (r0 != null) goto L_0x012c;
     */
    /* JADX WARNING: Missing block: B:36:0x0129, code:
            r3 = "<null>";
     */
    /* JADX WARNING: Missing block: B:37:0x012c, code:
            r3 = java.lang.String.valueOf(r0.id);
     */
    /* JADX WARNING: Missing block: B:38:0x0132, code:
            r6 = new java.lang.StringBuilder();
            r6.append("User ");
            r6.append(r4);
            r6.append(" (parent ");
            r6.append(r3);
            r6.append("): delaying unlock because parent is locked");
            android.util.Slog.d("ActivityManager", r6.toString());
     */
    /* JADX WARNING: Missing block: B:39:0x0156, code:
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
                if (this.mStartedUsers.get(userId) == uss && uss.state == 1) {
                    uss.mUnlockProgress.start();
                    uss.mUnlockProgress.setProgress(5, this.mInjector.getContext().getString(17039579));
                    FgThread.getHandler().post(new -$$Lambda$UserController$o6oQFjGYYIfx-I94cSakTLPLt6s(this, userId, uss));
                    return;
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

    /* JADX WARNING: Missing block: B:22:0x0033, code:
            r1.mInjector.getUserManagerInternal().setUserState(r15, r2.state);
            r2.mUnlockProgress.finish();
     */
    /* JADX WARNING: Missing block: B:23:0x0043, code:
            if (r15 != 0) goto L_0x004c;
     */
    /* JADX WARNING: Missing block: B:24:0x0045, code:
            r1.mInjector.startPersistentApps(262144);
     */
    /* JADX WARNING: Missing block: B:25:0x004c, code:
            r1.mInjector.installEncryptionUnawareProviders(r15);
            r0 = new android.content.Intent("android.intent.action.USER_UNLOCKED");
            r0.putExtra("android.intent.extra.user_handle", r15);
            r0.addFlags(1342177280);
            r19 = r15;
            r1.mInjector.broadcastIntent(r0, null, null, 0, null, null, null, -1, null, false, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r19);
            r4 = r19;
     */
    /* JADX WARNING: Missing block: B:26:0x008b, code:
            if (getUserInfo(r4).isManagedProfile() == false) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:27:0x008d, code:
            r3 = r1.mInjector.getUserManager().getProfileParent(r4);
     */
    /* JADX WARNING: Missing block: B:28:0x0097, code:
            if (r3 == null) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:29:0x0099, code:
            r5 = new android.content.Intent("android.intent.action.MANAGED_PROFILE_UNLOCKED");
            r5.putExtra("android.intent.extra.USER", android.os.UserHandle.of(r4));
            r5.addFlags(1342177280);
            r1.mInjector.broadcastIntent(r5, null, null, 0, null, null, null, -1, null, false, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r3.id);
     */
    /* JADX WARNING: Missing block: B:30:0x00d3, code:
            r3 = getUserInfo(r4);
     */
    /* JADX WARNING: Missing block: B:31:0x00df, code:
            if (java.util.Objects.equals(r3.lastLoggedInFingerprint, android.os.Build.FINGERPRINT) != false) goto L_0x010b;
     */
    /* JADX WARNING: Missing block: B:32:0x00e1, code:
            r6 = false;
     */
    /* JADX WARNING: Missing block: B:33:0x00e6, code:
            if (r3.isManagedProfile() != false) goto L_0x00f0;
     */
    /* JADX WARNING: Missing block: B:35:0x00ec, code:
            if (r3.isClonedProfile() == false) goto L_0x00ff;
     */
    /* JADX WARNING: Missing block: B:37:0x00f2, code:
            if (r2.tokenProvided == false) goto L_0x00fe;
     */
    /* JADX WARNING: Missing block: B:39:0x00fa, code:
            if (r1.mLockPatternUtils.isSeparateProfileChallengeEnabled(r4) != false) goto L_0x00ff;
     */
    /* JADX WARNING: Missing block: B:40:0x00fe, code:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:41:0x00ff, code:
            r1.mInjector.sendPreBootBroadcast(r4, r6, new com.android.server.am.-$$Lambda$UserController$d0zeElfogOIugnQQLWhCzumk53k(r1, r2));
     */
    /* JADX WARNING: Missing block: B:42:0x010b, code:
            finishUserUnlockedCompleted(r35);
     */
    /* JADX WARNING: Missing block: B:43:0x010e, code:
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

    /* JADX WARNING: Missing block: B:12:0x0023, code:
            r0 = getUserInfo(r15);
     */
    /* JADX WARNING: Missing block: B:13:0x0027, code:
            if (r0 != null) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:14:0x0029, code:
            return;
     */
    /* JADX WARNING: Missing block: B:16:0x002e, code:
            if (android.os.storage.StorageManager.isUserKeyUnlocked(r15) != false) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:17:0x0030, code:
            return;
     */
    /* JADX WARNING: Missing block: B:18:0x0031, code:
            r1.mInjector.getUserManager().onUserLoggedIn(r15);
     */
    /* JADX WARNING: Missing block: B:19:0x003e, code:
            if (r0.isInitialized() != false) goto L_0x008b;
     */
    /* JADX WARNING: Missing block: B:20:0x0040, code:
            if (r15 == 0) goto L_0x008b;
     */
    /* JADX WARNING: Missing block: B:21:0x0042, code:
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
    /* JADX WARNING: Missing block: B:22:0x008b, code:
            r20 = r15;
     */
    /* JADX WARNING: Missing block: B:23:0x008d, code:
            r4 = new java.lang.StringBuilder();
            r4.append("Sending BOOT_COMPLETE user #");
            r15 = r20;
            r4.append(r15);
            android.util.Slog.i("ActivityManager", r4.toString());
     */
    /* JADX WARNING: Missing block: B:24:0x00a5, code:
            if (r15 != 0) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:26:0x00ad, code:
            if (r1.mInjector.isRuntimeRestarted() != false) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:28:0x00b5, code:
            if (r1.mInjector.isFirstBootOrUpgrade() != false) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:29:0x00b7, code:
            com.android.internal.logging.MetricsLogger.histogram(r1.mInjector.getContext(), "framework_boot_completed", (int) (android.os.SystemClock.elapsedRealtime() / 1000));
     */
    /* JADX WARNING: Missing block: B:30:0x00ca, code:
            r14 = new android.content.Intent("android.intent.action.BOOT_COMPLETED", null);
            r14.putExtra("android.intent.extra.user_handle", r15);
            r14.addFlags(150994944);
            r4 = r14;
            r19 = r14;
            r1.mInjector.broadcastIntent(r4, null, new com.android.server.am.UserController.AnonymousClass2(r1), 0, null, null, new java.lang.String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, com.android.server.am.ActivityManagerService.MY_PID, 1000, r15);
     */
    /* JADX WARNING: Missing block: B:31:0x0106, code:
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
            if (this.mStartedUsers.get(userId) == uss && uss.state == 5) {
                stopped = true;
                this.mStartedUsers.remove(userId);
                this.mUserLru.remove(Integer.valueOf(userId));
                updateStartedUserArrayLU();
            } else {
                stopped = false;
            }
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

    /* JADX WARNING: Missing block: B:9:0x0015, code:
            r0 = r4.getUserInfo(r5);
     */
    /* JADX WARNING: Missing block: B:10:0x0019, code:
            if (r0 == null) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:12:0x001f, code:
            if (r0.isHwHiddenSpace() == false) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:13:0x0021, code:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:14:0x0023, code:
            r1 = false;
     */
    /* JADX WARNING: Missing block: B:15:0x0024, code:
            if (r1 != false) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:18:0x0028, code:
            if (r4.mIsSupportISec != false) goto L_0x0032;
     */
    /* JADX WARNING: Missing block: B:19:0x002a, code:
            r4.getStorageManager().lockUserKey(r5);
     */
    /* JADX WARNING: Missing block: B:20:0x0032, code:
            r4.getStorageManager().lockUserKeyISec(r5);
     */
    /* JADX WARNING: Missing block: B:21:0x003a, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:23:0x003f, code:
            throw r2.rethrowAsRuntimeException();
     */
    /* JADX WARNING: Missing block: B:24:0x0040, code:
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

    /* JADX WARNING: Missing block: B:16:0x0039, code:
            r1 = getUserInfo(r5);
     */
    /* JADX WARNING: Missing block: B:17:0x0041, code:
            if (r1.isEphemeral() == false) goto L_0x004e;
     */
    /* JADX WARNING: Missing block: B:18:0x0043, code:
            ((android.os.UserManagerInternal) com.android.server.LocalServices.getService(android.os.UserManagerInternal.class)).onEphemeralUserStop(r5);
     */
    /* JADX WARNING: Missing block: B:20:0x0052, code:
            if (r1.isGuest() != false) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:22:0x0058, code:
            if (r1.isEphemeral() == false) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:23:0x005a, code:
            r2 = r4.mLock;
     */
    /* JADX WARNING: Missing block: B:24:0x005c, code:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:27:?, code:
            stopUsersLU(r5, true, null);
     */
    /* JADX WARNING: Missing block: B:28:0x0062, code:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:29:0x0063, code:
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
            if (oldUserId == 0 || oldUserId == this.mCurrentUserId || oldUss == null || oldUss.state == 4 || oldUss.state == 5) {
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
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0098 A:{Splitter: B:29:0x008f, ExcHandler: android.os.RemoteException (r0_13 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x003e A:{Splitter: B:2:0x001a, ExcHandler: android.os.RemoteException (r0_6 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:9:0x002f, code:
            if (r13.isClonedProfile() != false) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:10:0x0031, code:
            android.util.Slog.i("ActivityManager", "ClonedProfile user unlock, set mHaveTryCloneProUserUnlock true!");
            r8.mHaveTryCloneProUserUnlock = true;
     */
    /* JADX WARNING: Missing block: B:12:0x003e, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:14:?, code:
            r2 = new java.lang.StringBuilder();
            r2.append("Failed to unlock: ");
            r2.append(r0.getMessage());
            r2.append(" ,SupportISec: ");
            r2.append(r8.mIsSupportISec);
            android.util.Slog.w("ActivityManager", r2.toString());
     */
    /* JADX WARNING: Missing block: B:15:0x0063, code:
            if (r13 != null) goto L_0x0065;
     */
    /* JADX WARNING: Missing block: B:17:0x0069, code:
            if (r13.isClonedProfile() != false) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:32:0x0098, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:33:0x0099, code:
            r3 = new java.lang.StringBuilder();
            r3.append("is SupportISec,Failed to setScreenStateFlag: ");
            r3.append(r0.getMessage());
            android.util.Slog.w("ActivityManager", r3.toString());
            r0 = r1;
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
            } catch (Exception e) {
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
                } catch (Exception e2) {
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
                        /* JADX WARNING: Removed duplicated region for block: B:2:0x0011 A:{Splitter: B:0:0x0000, ExcHandler: android.os.RemoteException (r0_1 'e' java.lang.Exception)} */
                        /* JADX WARNING: Missing block: B:2:0x0011, code:
            r0 = move-exception;
     */
                        /* JADX WARNING: Missing block: B:3:0x0012, code:
            r2 = new java.lang.StringBuilder();
            r2.append("is SupportISec,Failed to unlockUserScreenISec: ");
            r2.append(r0.getMessage());
            android.util.Slog.w("ActivityManager", r2.toString());
     */
                        /* JADX WARNING: Missing block: B:4:?, code:
            return;
     */
                        /* Code decompiled incorrectly, please refer to instructions dump. */
                        public void run() {
                            try {
                                iStorageManager.unlockUserScreenISec(i2, userInfo2.serialNumber, bArr3, bArr4, 1);
                            } catch (Exception e) {
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
                            StringBuilder stringBuilder = new StringBuilder();
                            userIds2 = userIds;
                            stringBuilder.append("User ");
                            stringBuilder.append(i3);
                            stringBuilder.append(" (parent ");
                            stringBuilder.append(parent.id);
                            stringBuilder.append("): attempting unlock because parent was just unlocked");
                            Slog.d("ActivityManager", stringBuilder.toString());
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

    /* JADX WARNING: Missing block: B:9:0x0015, code:
            if (r3.mLockPatternUtils.isSeparateProfileChallengeEnabled(r4) != false) goto L_0x0018;
     */
    /* JADX WARNING: Missing block: B:10:0x0017, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:11:0x0018, code:
            r0 = r3.mInjector.getKeyguardManager();
     */
    /* JADX WARNING: Missing block: B:12:0x0022, code:
            if (r0.isDeviceLocked(r4) == false) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:14:0x0028, code:
            if (r0.isDeviceSecure(r4) == false) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:15:0x002a, code:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:16:0x002c, code:
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
