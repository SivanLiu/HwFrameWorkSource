package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ILauncherApps.Stub;
import android.content.pm.IOnAppsChangedListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.ShortcutServiceInternal.ShortcutChangeListener;
import android.content.pm.UserInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.util.Collections;
import java.util.List;

public class LauncherAppsService extends SystemService {
    private final LauncherAppsImpl mLauncherAppsImpl;

    static class BroadcastCookie {
        public final int callingPid;
        public final int callingUid;
        public final String packageName;
        public final UserHandle user;

        BroadcastCookie(UserHandle userHandle, String packageName, int callingPid, int callingUid) {
            this.user = userHandle;
            this.packageName = packageName;
            this.callingUid = callingUid;
            this.callingPid = callingPid;
        }
    }

    @VisibleForTesting
    static class LauncherAppsImpl extends Stub {
        private static final boolean DEBUG = false;
        private static final String TAG = "LauncherAppsService";
        private final ActivityManagerInternal mActivityManagerInternal;
        private final Handler mCallbackHandler;
        private final Context mContext;
        private final PackageCallbackList<IOnAppsChangedListener> mListeners = new PackageCallbackList();
        private final MyPackageMonitor mPackageMonitor = new MyPackageMonitor();
        private final ShortcutServiceInternal mShortcutServiceInternal;
        private final UserManager mUm;
        private final UserManagerInternal mUserManagerInternal;

        private class MyPackageMonitor extends PackageMonitor implements ShortcutChangeListener {
            private MyPackageMonitor() {
            }

            public void onPackageAdded(String packageName, int uid) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, user, "onPackageAdded")) {
                            listener.onPackageAdded(user, packageName);
                        }
                    } catch (RemoteException re) {
                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackageAdded(packageName, uid);
            }

            public void onPackageRemoved(String packageName, int uid) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, user, "onPackageRemoved")) {
                            listener.onPackageRemoved(user, packageName);
                        }
                    } catch (RemoteException re) {
                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackageRemoved(packageName, uid);
            }

            public void onPackageModified(String packageName) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, user, "onPackageModified")) {
                            listener.onPackageChanged(user, packageName);
                        }
                    } catch (RemoteException re) {
                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackageModified(packageName);
            }

            public void onPackagesAvailable(String[] packages) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, user, "onPackagesAvailable")) {
                            listener.onPackagesAvailable(user, packages, isReplacing());
                        }
                    } catch (RemoteException re) {
                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesAvailable(packages);
            }

            public void onPackagesUnavailable(String[] packages) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, user, "onPackagesUnavailable")) {
                            listener.onPackagesUnavailable(user, packages, isReplacing());
                        }
                    } catch (RemoteException re) {
                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesUnavailable(packages);
            }

            public void onPackagesSuspended(String[] packages, Bundle launcherExtras) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, user, "onPackagesSuspended")) {
                            listener.onPackagesSuspended(user, packages, launcherExtras);
                        }
                    } catch (RemoteException re) {
                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesSuspended(packages, launcherExtras);
            }

            public void onPackagesUnsuspended(String[] packages) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, user, "onPackagesUnsuspended")) {
                            listener.onPackagesUnsuspended(user, packages);
                        }
                    } catch (RemoteException re) {
                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesUnsuspended(packages);
            }

            public void onShortcutChanged(String packageName, int userId) {
                LauncherAppsImpl.this.postToPackageMonitorHandler(new -$$Lambda$LauncherAppsService$LauncherAppsImpl$MyPackageMonitor$eTair5Mvr14v4M0nq9aQEW2cp-Y(this, packageName, userId));
            }

            private void onShortcutChangedInner(String packageName, int userId) {
                RemoteException re;
                String str;
                RuntimeException e;
                Throwable th;
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                try {
                    UserHandle user = UserHandle.of(userId);
                    int i = 0;
                    while (true) {
                        int i2 = i;
                        if (i2 >= n) {
                            break;
                        }
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i2);
                        BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i2);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user, "onShortcutChanged")) {
                            int launcherUserId = cookie.user.getIdentifier();
                            if (LauncherAppsImpl.this.mShortcutServiceInternal.hasShortcutHostPermission(launcherUserId, cookie.packageName, cookie.callingPid, cookie.callingUid)) {
                                ShortcutServiceInternal access$300 = LauncherAppsImpl.this.mShortcutServiceInternal;
                                String str2 = cookie.packageName;
                                int i3 = cookie.callingPid;
                                try {
                                    try {
                                        listener.onShortcutChanged(user, packageName, new ParceledListSlice(access$300.getShortcuts(launcherUserId, str2, 0, packageName, null, null, 1039, userId, i3, cookie.callingUid)));
                                    } catch (RemoteException e2) {
                                        re = e2;
                                    }
                                } catch (RemoteException e3) {
                                    re = e3;
                                    str = packageName;
                                    try {
                                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                                        i = i2 + 1;
                                    } catch (RuntimeException e4) {
                                        e = e4;
                                    }
                                }
                                i = i2 + 1;
                            }
                        }
                        str = packageName;
                        i = i2 + 1;
                    }
                    str = packageName;
                } catch (RuntimeException e5) {
                    e = e5;
                    str = packageName;
                    try {
                        Log.w(LauncherAppsImpl.TAG, e.getMessage(), e);
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    str = packageName;
                    LauncherAppsImpl.this.mListeners.finishBroadcast();
                    throw th;
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
            }
        }

        class PackageCallbackList<T extends IInterface> extends RemoteCallbackList<T> {
            PackageCallbackList() {
            }

            public void onCallbackDied(T t, Object cookie) {
                LauncherAppsImpl.this.checkCallbackCount();
            }
        }

        public LauncherAppsImpl(Context context) {
            this.mContext = context;
            this.mUm = (UserManager) this.mContext.getSystemService("user");
            this.mUserManagerInternal = (UserManagerInternal) Preconditions.checkNotNull((UserManagerInternal) LocalServices.getService(UserManagerInternal.class));
            this.mActivityManagerInternal = (ActivityManagerInternal) Preconditions.checkNotNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
            this.mShortcutServiceInternal = (ShortcutServiceInternal) Preconditions.checkNotNull((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class));
            this.mShortcutServiceInternal.addListener(this.mPackageMonitor);
            this.mCallbackHandler = BackgroundThread.getHandler();
        }

        @VisibleForTesting
        int injectBinderCallingUid() {
            return getCallingUid();
        }

        @VisibleForTesting
        int injectBinderCallingPid() {
            return getCallingPid();
        }

        final int injectCallingUserId() {
            return UserHandle.getUserId(injectBinderCallingUid());
        }

        @VisibleForTesting
        long injectClearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        @VisibleForTesting
        void injectRestoreCallingIdentity(long token) {
            Binder.restoreCallingIdentity(token);
        }

        private int getCallingUserId() {
            return UserHandle.getUserId(injectBinderCallingUid());
        }

        public void addOnAppsChangedListener(String callingPackage, IOnAppsChangedListener listener) throws RemoteException {
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.LAUNCHERAPPS_ADDONAPPSCHANGEDLISTENER);
            verifyCallingPackage(callingPackage);
            synchronized (this.mListeners) {
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    startWatchingPackageBroadcasts();
                }
                this.mListeners.unregister(listener);
                this.mListeners.register(listener, new BroadcastCookie(UserHandle.of(getCallingUserId()), callingPackage, injectBinderCallingPid(), injectBinderCallingUid()));
            }
        }

        public void removeOnAppsChangedListener(IOnAppsChangedListener listener) throws RemoteException {
            synchronized (this.mListeners) {
                this.mListeners.unregister(listener);
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    stopWatchingPackageBroadcasts();
                }
            }
        }

        private void startWatchingPackageBroadcasts() {
            this.mPackageMonitor.register(this.mContext, UserHandle.ALL, true, this.mCallbackHandler);
        }

        private void stopWatchingPackageBroadcasts() {
            this.mPackageMonitor.unregister();
        }

        void checkCallbackCount() {
            synchronized (this.mListeners) {
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    stopWatchingPackageBroadcasts();
                }
            }
        }

        private boolean canAccessProfile(int targetUserId, String message) {
            int callingUserId = injectCallingUserId();
            if (targetUserId == callingUserId) {
                return true;
            }
            long ident = injectClearCallingIdentity();
            try {
                UserInfo callingUserInfo = this.mUm.getUserInfo(callingUserId);
                if (callingUserInfo == null || !callingUserInfo.isManagedProfile()) {
                    injectRestoreCallingIdentity(ident);
                    return this.mUserManagerInternal.isProfileAccessible(injectCallingUserId(), targetUserId, message, true);
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(message);
                stringBuilder.append(" for another profile ");
                stringBuilder.append(targetUserId);
                stringBuilder.append(" from ");
                stringBuilder.append(callingUserId);
                stringBuilder.append(" not allowed");
                Slog.w(str, stringBuilder.toString());
                return false;
            } finally {
                injectRestoreCallingIdentity(ident);
            }
        }

        @VisibleForTesting
        void verifyCallingPackage(String callingPackage) {
            int packageUid = -1;
            try {
                packageUid = AppGlobals.getPackageManager().getPackageUid(callingPackage, 794624, UserHandle.getUserId(getCallingUid()));
            } catch (RemoteException e) {
            }
            if (packageUid < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Package not found: ");
                stringBuilder.append(callingPackage);
                Log.e(str, stringBuilder.toString());
            }
            if (packageUid != injectBinderCallingUid()) {
                throw new SecurityException("Calling package name mismatch");
            }
        }

        public ParceledListSlice<ResolveInfo> getLauncherActivities(String callingPackage, String packageName, UserHandle user) throws RemoteException {
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.LAUNCHERAPPS_GETLAUNCHERACTIVITIES);
            return queryActivitiesForUser(callingPackage, new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setPackage(packageName), user);
        }

        public ActivityInfo resolveActivity(String callingPackage, ComponentName component, UserHandle user) throws RemoteException {
            if (!canAccessProfile(user.getIdentifier(), "Cannot resolve activity")) {
                return null;
            }
            int callingUid = injectBinderCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                ActivityInfo activityInfo = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getActivityInfo(component, 786432, callingUid, user.getIdentifier());
                return activityInfo;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public ParceledListSlice getShortcutConfigActivities(String callingPackage, String packageName, UserHandle user) throws RemoteException {
            return queryActivitiesForUser(callingPackage, new Intent("android.intent.action.CREATE_SHORTCUT").setPackage(packageName), user);
        }

        private ParceledListSlice<ResolveInfo> queryActivitiesForUser(String callingPackage, Intent intent, UserHandle user) {
            if (!canAccessProfile(user.getIdentifier(), "Cannot retrieve activities")) {
                return null;
            }
            int callingUid = injectBinderCallingUid();
            long ident = injectClearCallingIdentity();
            try {
                ParceledListSlice<ResolveInfo> parceledListSlice = new ParceledListSlice(((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).queryIntentActivities(intent, 786432, callingUid, user.getIdentifier()));
                return parceledListSlice;
            } finally {
                injectRestoreCallingIdentity(ident);
            }
        }

        public IntentSender getShortcutConfigActivityIntent(String callingPackage, ComponentName component, UserHandle user) throws RemoteException {
            ensureShortcutPermission(callingPackage);
            IntentSender intentSender = null;
            if (!canAccessProfile(user.getIdentifier(), "Cannot check package")) {
                return null;
            }
            Preconditions.checkNotNull(component);
            Intent intent = new Intent("android.intent.action.CREATE_SHORTCUT").setComponent(component);
            long identity = Binder.clearCallingIdentity();
            try {
                PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, null, user);
                if (pi != null) {
                    intentSender = pi.getIntentSender();
                }
                Binder.restoreCallingIdentity(identity);
                return intentSender;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public ResolveInfo resolveActivityByIntent(Intent intent, UserHandle user) throws RemoteException {
            if (!canAccessProfile(user.getIdentifier(), "Cannot resolve activity")) {
                return null;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                ResolveInfo app = this.mContext.getPackageManager().resolveActivityAsUser(intent, 786432, user.getIdentifier());
                return app;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isPackageEnabled(String callingPackage, String packageName, UserHandle user) throws RemoteException {
            boolean z = false;
            if (!canAccessProfile(user.getIdentifier(), "Cannot check package")) {
                return false;
            }
            int callingUid = injectBinderCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PackageInfo info = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageInfo(packageName, 786432, callingUid, user.getIdentifier());
                if (info != null && info.applicationInfo.enabled) {
                    z = true;
                }
                Binder.restoreCallingIdentity(ident);
                return z;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public Bundle getSuspendedPackageLauncherExtras(String packageName, UserHandle user) {
            if (canAccessProfile(user.getIdentifier(), "Cannot get launcher extras")) {
                return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getSuspendedPackageLauncherExtras(packageName, user.getIdentifier());
            }
            return null;
        }

        public ApplicationInfo getApplicationInfo(String callingPackage, String packageName, int flags, UserHandle user) throws RemoteException {
            if (!canAccessProfile(user.getIdentifier(), "Cannot check package")) {
                return null;
            }
            int callingUid = injectBinderCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                ApplicationInfo info = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getApplicationInfo(packageName, flags, callingUid, user.getIdentifier());
                return info;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        private void ensureShortcutPermission(String callingPackage) {
            verifyCallingPackage(callingPackage);
            if (!this.mShortcutServiceInternal.hasShortcutHostPermission(getCallingUserId(), callingPackage, injectBinderCallingPid(), injectBinderCallingUid())) {
                throw new SecurityException("Caller can't access shortcut information");
            }
        }

        public ParceledListSlice getShortcuts(String callingPackage, long changedSince, String packageName, List shortcutIds, ComponentName componentName, int flags, UserHandle targetUser) {
            ensureShortcutPermission(callingPackage);
            if (!canAccessProfile(targetUser.getIdentifier(), "Cannot get shortcuts")) {
                return new ParceledListSlice(Collections.EMPTY_LIST);
            }
            if (shortcutIds == null || packageName != null) {
                return new ParceledListSlice(this.mShortcutServiceInternal.getShortcuts(getCallingUserId(), callingPackage, changedSince, packageName, shortcutIds, componentName, flags, targetUser.getIdentifier(), injectBinderCallingPid(), injectBinderCallingUid()));
            }
            throw new IllegalArgumentException("To query by shortcut ID, package name must also be set");
        }

        public void pinShortcuts(String callingPackage, String packageName, List<String> ids, UserHandle targetUser) {
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.LAUNCHERAPPS_PINSHORTCUTS);
            ensureShortcutPermission(callingPackage);
            if (canAccessProfile(targetUser.getIdentifier(), "Cannot pin shortcuts")) {
                this.mShortcutServiceInternal.pinShortcuts(getCallingUserId(), callingPackage, packageName, ids, targetUser.getIdentifier());
            }
        }

        public int getShortcutIconResId(String callingPackage, String packageName, String id, int targetUserId) {
            ensureShortcutPermission(callingPackage);
            if (canAccessProfile(targetUserId, "Cannot access shortcuts")) {
                return this.mShortcutServiceInternal.getShortcutIconResId(getCallingUserId(), callingPackage, packageName, id, targetUserId);
            }
            return 0;
        }

        public ParcelFileDescriptor getShortcutIconFd(String callingPackage, String packageName, String id, int targetUserId) {
            ensureShortcutPermission(callingPackage);
            if (canAccessProfile(targetUserId, "Cannot access shortcuts")) {
                return this.mShortcutServiceInternal.getShortcutIconFd(getCallingUserId(), callingPackage, packageName, id, targetUserId);
            }
            return null;
        }

        public boolean hasShortcutHostPermission(String callingPackage) {
            verifyCallingPackage(callingPackage);
            return this.mShortcutServiceInternal.hasShortcutHostPermission(getCallingUserId(), callingPackage, injectBinderCallingPid(), injectBinderCallingUid());
        }

        public boolean startShortcut(String callingPackage, String packageName, String shortcutId, Rect sourceBounds, Bundle startActivityOptions, int targetUserId) {
            int i = targetUserId;
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.LAUNCHERAPPS_STARTSHORTCUT);
            verifyCallingPackage(callingPackage);
            if (!canAccessProfile(i, "Cannot start activity")) {
                return false;
            }
            if (!this.mShortcutServiceInternal.isPinnedByCaller(getCallingUserId(), callingPackage, packageName, shortcutId, i)) {
                ensureShortcutPermission(callingPackage);
            }
            Intent[] intents = this.mShortcutServiceInternal.createShortcutIntents(getCallingUserId(), callingPackage, packageName, shortcutId, i, injectBinderCallingPid(), injectBinderCallingUid());
            if (intents == null || intents.length == 0) {
                String str = packageName;
                Rect rect = sourceBounds;
                Bundle bundle = startActivityOptions;
                return false;
            }
            intents[0].addFlags(268435456);
            intents[0].setSourceBounds(sourceBounds);
            return startShortcutIntentsAsPublisher(intents, packageName, startActivityOptions, i);
        }

        private boolean startShortcutIntentsAsPublisher(Intent[] intents, String publisherPackage, Bundle startActivityOptions, int userId) {
            try {
                int code = this.mActivityManagerInternal.startActivitiesAsPackage(publisherPackage, userId, intents, startActivityOptions);
                if (ActivityManager.isStartResultSuccessful(code)) {
                    return true;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't start activity, code=");
                stringBuilder.append(code);
                Log.e(str, stringBuilder.toString());
                return false;
            } catch (SecurityException e) {
                return false;
            }
        }

        public boolean isActivityEnabled(String callingPackage, ComponentName component, UserHandle user) throws RemoteException {
            boolean z = false;
            if (!canAccessProfile(user.getIdentifier(), "Cannot check component")) {
                return false;
            }
            int callingUid = injectBinderCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                if (((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getActivityInfo(component, 786432, callingUid, user.getIdentifier()) != null) {
                    z = true;
                }
                Binder.restoreCallingIdentity(ident);
                return z;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void startActivityAsUser(IApplicationThread caller, String callingPackage, ComponentName component, Rect sourceBounds, Bundle opts, UserHandle user) throws RemoteException {
            Throwable th;
            ComponentName componentName = component;
            if (canAccessProfile(user.getIdentifier(), "Cannot start activity")) {
                Intent launchIntent = new Intent("android.intent.action.MAIN");
                launchIntent.addCategory("android.intent.category.LAUNCHER");
                launchIntent.setSourceBounds(sourceBounds);
                launchIntent.addFlags(270532608);
                launchIntent.setPackage(component.getPackageName());
                boolean canLaunch = false;
                int callingUid = injectBinderCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PackageManagerInternal pmInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    if (pmInt.getActivityInfo(componentName, 786432, callingUid, user.getIdentifier()).exported) {
                        List<ResolveInfo> apps = pmInt.queryIntentActivities(launchIntent, 786432, callingUid, user.getIdentifier());
                        int size = apps.size();
                        for (int i = 0; i < size; i++) {
                            ActivityInfo activityInfo = ((ResolveInfo) apps.get(i)).activityInfo;
                            if (activityInfo.packageName.equals(component.getPackageName()) && activityInfo.name.equals(component.getClassName())) {
                                launchIntent.setPackage(null);
                                launchIntent.setComponent(componentName);
                                canLaunch = true;
                                break;
                            }
                        }
                        boolean canLaunch2 = canLaunch;
                        if (canLaunch2) {
                            Binder.restoreCallingIdentity(ident);
                            this.mActivityManagerInternal.startActivityAsUser(caller, callingPackage, launchIntent, opts, user.getIdentifier());
                            return;
                        }
                        try {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Attempt to launch activity without  category Intent.CATEGORY_LAUNCHER ");
                            stringBuilder.append(componentName);
                            throw new SecurityException(stringBuilder.toString());
                        } catch (Throwable th2) {
                            th = th2;
                            canLaunch = canLaunch2;
                            Binder.restoreCallingIdentity(ident);
                            throw th;
                        }
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Cannot launch non-exported components ");
                    stringBuilder2.append(componentName);
                    throw new SecurityException(stringBuilder2.toString());
                } catch (Throwable th3) {
                    th = th3;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
        }

        public void showAppDetailsAsUser(IApplicationThread caller, String callingPackage, ComponentName component, Rect sourceBounds, Bundle opts, UserHandle user) throws RemoteException {
            Throwable th;
            if (canAccessProfile(user.getIdentifier(), "Cannot show app details")) {
                long ident = Binder.clearCallingIdentity();
                try {
                    Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", component.getPackageName(), null));
                    intent.setFlags(268468224);
                    try {
                        intent.setSourceBounds(sourceBounds);
                        Binder.restoreCallingIdentity(ident);
                        this.mActivityManagerInternal.startActivityAsUser(caller, callingPackage, intent, opts, user.getIdentifier());
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(ident);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Rect rect = sourceBounds;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
        }

        private boolean isEnabledProfileOf(UserHandle listeningUser, UserHandle user, String debugMsg) {
            return this.mUserManagerInternal.isProfileAccessible(listeningUser.getIdentifier(), user.getIdentifier(), debugMsg, false);
        }

        @VisibleForTesting
        void postToPackageMonitorHandler(Runnable r) {
            this.mCallbackHandler.post(r);
        }
    }

    public LauncherAppsService(Context context) {
        super(context);
        this.mLauncherAppsImpl = new LauncherAppsImpl(context);
    }

    public void onStart() {
        publishBinderService("launcherapps", this.mLauncherAppsImpl);
    }
}
