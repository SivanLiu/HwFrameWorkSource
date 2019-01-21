package com.android.server.content;

import android.accounts.Account;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.job.JobInfo;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentService.Stub;
import android.content.ISyncStatusObserver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncRequest;
import android.content.SyncStatusInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageManagerInternal.SyncAdapterPackagesProvider;
import android.content.pm.ProviderInfo;
import android.database.IContentObserver;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FactoryTest;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.rms.HwSysResource;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.content.SyncStorageEngine.EndPoint;
import com.android.server.power.IHwShutdownThread;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ContentService extends Stub {
    static final boolean DEBUG = false;
    static final String TAG = "ContentService";
    private static HwSysResource mContentObserverResource = null;
    private static PackageManager mPackageManager = null;
    @GuardedBy("mCache")
    private final SparseArray<ArrayMap<String, ArrayMap<Pair<String, Uri>, Bundle>>> mCache = new SparseArray();
    private BroadcastReceiver mCacheReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            synchronized (ContentService.this.mCache) {
                if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                    ContentService.this.mCache.clear();
                } else {
                    Uri data = intent.getData();
                    if (data != null) {
                        int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                        String packageName = data.getSchemeSpecificPart();
                        ContentService.this.invalidateCacheLocked(userId, packageName, null);
                        if ("android.intent.action.PACKAGE_CHANGED".equals(intent.getAction()) || "android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                            if (!(ContentService.mContentObserverResource == null || uid == -1)) {
                                ContentService.mContentObserverResource.clear(uid, packageName, -1);
                            }
                        }
                    }
                }
            }
        }
    };
    private Context mContext;
    private boolean mFactoryTest;
    private final ObserverNode mRootNode = new ObserverNode(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    private SyncManager mSyncManager = null;
    private final Object mSyncManagerLock = new Object();

    public static final class ObserverCall {
        final ObserverNode mNode;
        final IContentObserver mObserver;
        final int mObserverUserId;
        final boolean mSelfChange;

        ObserverCall(ObserverNode node, IContentObserver observer, boolean selfChange, int observerUserId) {
            this.mNode = node;
            this.mObserver = observer;
            this.mSelfChange = selfChange;
            this.mObserverUserId = observerUserId;
        }
    }

    public static final class ObserverNode {
        public static final int DELETE_TYPE = 2;
        public static final int INSERT_TYPE = 0;
        public static final int UPDATE_TYPE = 1;
        private ArrayList<ObserverNode> mChildren = new ArrayList();
        private String mName;
        private ArrayList<ObserverEntry> mObservers = new ArrayList();

        private class ObserverEntry implements DeathRecipient {
            public final boolean notifyForDescendants;
            public final IContentObserver observer;
            private final Object observersLock;
            public final String packageName;
            public final int pid;
            public final int uid;
            private final int userHandle;

            public ObserverEntry(IContentObserver o, boolean n, Object observersLock, int _uid, int _pid, String _packageName, int _userHandle) {
                this.observersLock = observersLock;
                this.observer = o;
                this.uid = _uid;
                this.pid = _pid;
                this.packageName = _packageName;
                this.userHandle = _userHandle;
                this.notifyForDescendants = n;
                try {
                    this.observer.asBinder().linkToDeath(this, 0);
                } catch (RemoteException e) {
                    binderDied();
                }
            }

            public ObserverEntry(IContentObserver o, boolean n, Object observersLock, int _uid, int _pid, int _userHandle) {
                this.observersLock = observersLock;
                this.observer = o;
                this.uid = _uid;
                this.pid = _pid;
                this.packageName = null;
                this.userHandle = _userHandle;
                this.notifyForDescendants = n;
                try {
                    this.observer.asBinder().linkToDeath(this, 0);
                } catch (RemoteException e) {
                    binderDied();
                }
            }

            public void binderDied() {
                synchronized (this.observersLock) {
                    ObserverNode.this.removeObserverLocked(this.observer);
                }
            }

            public void dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, String name, String prefix, SparseIntArray pidCounts) {
                pidCounts.put(this.pid, pidCounts.get(this.pid) + 1);
                pw.print(prefix);
                pw.print(name);
                pw.print(": pid=");
                pw.print(this.pid);
                pw.print(" uid=");
                pw.print(this.uid);
                pw.print(" user=");
                pw.print(this.userHandle);
                pw.print(" target=");
                pw.println(Integer.toHexString(System.identityHashCode(this.observer != null ? this.observer.asBinder() : null)));
            }
        }

        public ObserverNode(String name) {
            this.mName = name;
        }

        public void dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, String name, String prefix, int[] counts, SparseIntArray pidCounts) {
            StringBuilder stringBuilder;
            String str = name;
            String innerName = null;
            if (this.mObservers.size() > 0) {
                if (BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str)) {
                    innerName = this.mName;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(str);
                    stringBuilder.append(SliceAuthority.DELIMITER);
                    stringBuilder.append(this.mName);
                    innerName = stringBuilder.toString();
                }
                int i = 0;
                while (true) {
                    int i2 = i;
                    if (i2 >= this.mObservers.size()) {
                        break;
                    }
                    counts[1] = counts[1] + 1;
                    ((ObserverEntry) this.mObservers.get(i2)).dumpLocked(fd, pw, args, innerName, prefix, pidCounts);
                    i = i2 + 1;
                }
            }
            if (this.mChildren.size() > 0) {
                if (innerName == null) {
                    if (BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str)) {
                        innerName = this.mName;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(str);
                        stringBuilder.append(SliceAuthority.DELIMITER);
                        stringBuilder.append(this.mName);
                        innerName = stringBuilder.toString();
                    }
                }
                String innerName2 = innerName;
                innerName = null;
                while (true) {
                    int i3 = innerName;
                    if (i3 < this.mChildren.size()) {
                        counts[0] = counts[0] + 1;
                        ((ObserverNode) this.mChildren.get(i3)).dumpLocked(fd, pw, args, innerName2, prefix, counts, pidCounts);
                        innerName = i3 + 1;
                    } else {
                        return;
                    }
                }
            }
        }

        private String getUriSegment(Uri uri, int index) {
            if (uri == null) {
                return null;
            }
            if (index == 0) {
                return uri.getAuthority();
            }
            return (String) uri.getPathSegments().get(index - 1);
        }

        private int countUriSegments(Uri uri) {
            if (uri == null) {
                return 0;
            }
            return uri.getPathSegments().size() + 1;
        }

        public void addObserverLocked(Uri uri, IContentObserver observer, boolean notifyForDescendants, Object observersLock, int uid, int pid, int userHandle) {
            addObserverLocked(uri, 0, observer, notifyForDescendants, observersLock, uid, pid, userHandle);
        }

        private void addObserverLocked(Uri uri, int index, IContentObserver observer, boolean notifyForDescendants, Object observersLock, int uid, int pid, int userHandle) {
            int i = index;
            int i2 = uid;
            if (i == countUriSegments(uri)) {
                String packageName = null;
                if (ContentService.mContentObserverResource == null) {
                    ContentService.mContentObserverResource = HwFrameworkFactory.getHwResource(29);
                }
                if (!(ContentService.mContentObserverResource == null || ContentService.mPackageManager == null)) {
                    packageName = ContentService.mPackageManager.getNameForUid(i2);
                    if (packageName != null) {
                        ContentService.mContentObserverResource.acquire(i2, packageName, -1);
                    }
                }
                this.mObservers.add(new ObserverEntry(observer, notifyForDescendants, observersLock, i2, pid, packageName, userHandle));
                return;
            }
            String segment = getUriSegment(uri, index);
            if (segment != null) {
                int N = this.mChildren.size();
                int i3 = 0;
                while (true) {
                    int i4 = i3;
                    if (i4 < N) {
                        ObserverNode node = (ObserverNode) this.mChildren.get(i4);
                        if (node.mName.equals(segment)) {
                            node.addObserverLocked(uri, i + 1, observer, notifyForDescendants, observersLock, i2, pid, userHandle);
                            return;
                        }
                        i3 = i4 + 1;
                    } else {
                        ObserverNode node2 = new ObserverNode(segment);
                        this.mChildren.add(node2);
                        node2.addObserverLocked(uri, i + 1, observer, notifyForDescendants, observersLock, i2, pid, userHandle);
                        return;
                    }
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid Uri (");
            stringBuilder.append(uri);
            stringBuilder.append(") used for observer");
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public boolean removeObserverLocked(IContentObserver observer) {
            int size = this.mChildren.size();
            int i = 0;
            while (i < size) {
                if (((ObserverNode) this.mChildren.get(i)).removeObserverLocked(observer)) {
                    this.mChildren.remove(i);
                    i--;
                    size--;
                }
                i++;
            }
            IBinder observerBinder = observer.asBinder();
            size = this.mObservers.size();
            int i2 = 0;
            while (i2 < size) {
                ObserverEntry entry = (ObserverEntry) this.mObservers.get(i2);
                if (entry.observer.asBinder() == observerBinder) {
                    this.mObservers.remove(i2);
                    if (!(ContentService.mContentObserverResource == null || entry.packageName == null)) {
                        ContentService.mContentObserverResource.release(entry.uid, entry.packageName, -1);
                    }
                    observerBinder.unlinkToDeath(entry, 0);
                    return this.mChildren.size() != 0 && this.mObservers.size() == 0;
                } else {
                    i2++;
                }
            }
            if (this.mChildren.size() != 0) {
            }
        }

        private void collectMyObserversLocked(boolean leaf, IContentObserver observer, boolean observerWantsSelfNotifications, int flags, int targetUserHandle, ArrayList<ObserverCall> calls) {
            int N = this.mObservers.size();
            IBinder observerBinder = observer == null ? null : observer.asBinder();
            for (int i = 0; i < N; i++) {
                ObserverEntry entry = (ObserverEntry) this.mObservers.get(i);
                boolean selfChange = entry.observer.asBinder() == observerBinder;
                if ((!selfChange || observerWantsSelfNotifications) && !HwServiceFactory.getHwNLPManager().shouldSkipGoogleNlp(entry.pid) && (targetUserHandle == -1 || entry.userHandle == -1 || targetUserHandle == entry.userHandle)) {
                    if (leaf) {
                        if ((flags & 2) != 0 && entry.notifyForDescendants) {
                        }
                    } else if (!entry.notifyForDescendants) {
                    }
                    calls.add(new ObserverCall(this, entry.observer, selfChange, UserHandle.getUserId(entry.uid)));
                }
            }
        }

        public void collectObserversLocked(Uri uri, int index, IContentObserver observer, boolean observerWantsSelfNotifications, int flags, int targetUserHandle, ArrayList<ObserverCall> calls) {
            int i = index;
            String segment = null;
            int segmentCount = countUriSegments(uri);
            if (i >= segmentCount) {
                collectMyObserversLocked(true, observer, observerWantsSelfNotifications, flags, targetUserHandle, calls);
            } else if (i < segmentCount) {
                segment = getUriSegment(uri, index);
                collectMyObserversLocked(false, observer, observerWantsSelfNotifications, flags, targetUserHandle, calls);
            }
            int N = this.mChildren.size();
            for (int i2 = 0; i2 < N; i2++) {
                ObserverNode node = (ObserverNode) this.mChildren.get(i2);
                if (segment == null || node.mName.equals(segment)) {
                    node.collectObserversLocked(uri, i + 1, observer, observerWantsSelfNotifications, flags, targetUserHandle, calls);
                    if (segment != null) {
                        return;
                    }
                }
            }
        }
    }

    public static class Lifecycle extends SystemService {
        private ContentService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            boolean z = true;
            if (FactoryTest.getMode() != 1) {
                z = false;
            }
            this.mService = new ContentService(getContext(), z);
            publishBinderService("content", this.mService);
        }

        public void onBootPhase(int phase) {
            this.mService.onBootPhase(phase);
        }

        public void onStartUser(int userHandle) {
            this.mService.onStartUser(userHandle);
        }

        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }

        public void onStopUser(int userHandle) {
            this.mService.onStopUser(userHandle);
        }

        public void onCleanupUser(int userHandle) {
            synchronized (this.mService.mCache) {
                this.mService.mCache.remove(userHandle);
            }
        }
    }

    private SyncManager getSyncManager() {
        SyncManager createHwSyncManager;
        synchronized (this.mSyncManagerLock) {
            try {
                if (this.mSyncManager == null) {
                    createHwSyncManager = HwServiceFactory.createHwSyncManager(this.mContext, this.mFactoryTest);
                    this.mSyncManager = createHwSyncManager;
                    this.mSyncManager = createHwSyncManager;
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "Can't create SyncManager", e);
            }
            createHwSyncManager = this.mSyncManager;
        }
        return createHwSyncManager;
    }

    void onStartUser(int userHandle) {
        if (this.mSyncManager != null) {
            this.mSyncManager.onStartUser(userHandle);
        }
    }

    void onUnlockUser(int userHandle) {
        if (this.mSyncManager != null) {
            this.mSyncManager.onUnlockUser(userHandle);
        }
    }

    void onStopUser(int userHandle) {
        if (this.mSyncManager != null) {
            this.mSyncManager.onStopUser(userHandle);
        }
    }

    protected synchronized void dump(FileDescriptor fd, PrintWriter pw_, String[] args) {
        Throwable th;
        PrintWriter printWriter = pw_;
        synchronized (this) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
                PrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
                String[] strArr = args;
                boolean dumpAll = ArrayUtils.contains(strArr, "-a");
                long identityToken = clearCallingIdentity();
                try {
                    FileDescriptor fileDescriptor;
                    if (this.mSyncManager == null) {
                        pw.println("SyncManager not available yet");
                        fileDescriptor = fd;
                    } else {
                        fileDescriptor = fd;
                        this.mSyncManager.dump(fileDescriptor, pw, dumpAll);
                    }
                    pw.println();
                    pw.println("Observer tree:");
                    ObserverNode observerNode = this.mRootNode;
                    synchronized (observerNode) {
                        ObserverNode observerNode2;
                        try {
                            int[] counts = new int[2];
                            SparseIntArray pidCounts = new SparseIntArray();
                            observerNode2 = observerNode;
                            try {
                                final SparseIntArray pidCounts2;
                                this.mRootNode.dumpLocked(fileDescriptor, pw, strArr, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, "  ", counts, pidCounts);
                                pw.println();
                                ArrayList<Integer> sorted = new ArrayList();
                                int i = 0;
                                int i2 = 0;
                                while (true) {
                                    pidCounts2 = pidCounts;
                                    if (i2 >= pidCounts2.size()) {
                                        break;
                                    }
                                    sorted.add(Integer.valueOf(pidCounts2.keyAt(i2)));
                                    i2++;
                                    pidCounts = pidCounts2;
                                }
                                Collections.sort(sorted, new Comparator<Integer>() {
                                    public int compare(Integer lhs, Integer rhs) {
                                        int lc = pidCounts2.get(lhs.intValue());
                                        int rc = pidCounts2.get(rhs.intValue());
                                        if (lc < rc) {
                                            return 1;
                                        }
                                        if (lc > rc) {
                                            return -1;
                                        }
                                        return 0;
                                    }
                                });
                                for (i2 = 0; i2 < sorted.size(); i2++) {
                                    int pid = ((Integer) sorted.get(i2)).intValue();
                                    pw.print("  pid ");
                                    pw.print(pid);
                                    pw.print(": ");
                                    pw.print(pidCounts2.get(pid));
                                    pw.println(" observers");
                                }
                                pw.println();
                                pw.print(" Total number of nodes: ");
                                pw.println(counts[0]);
                                pw.print(" Total number of observers: ");
                                pw.println(counts[1]);
                                synchronized (this.mCache) {
                                    pw.println();
                                    pw.println("Cached content:");
                                    pw.increaseIndent();
                                    while (true) {
                                        int i3 = i;
                                        if (i3 < this.mCache.size()) {
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("User ");
                                            stringBuilder.append(this.mCache.keyAt(i3));
                                            stringBuilder.append(":");
                                            pw.println(stringBuilder.toString());
                                            pw.increaseIndent();
                                            pw.println(this.mCache.valueAt(i3));
                                            pw.decreaseIndent();
                                            i = i3 + 1;
                                        } else {
                                            pw.decreaseIndent();
                                        }
                                    }
                                }
                                restoreCallingIdentity(identityToken);
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            observerNode2 = observerNode;
                            throw th;
                        }
                    }
                } catch (Throwable th4) {
                    restoreCallingIdentity(identityToken);
                }
            }
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Content Service Crash", e);
            }
            throw e;
        }
    }

    ContentService(Context context, boolean factoryTest) {
        this.mContext = context;
        this.mFactoryTest = factoryTest;
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setSyncAdapterPackagesprovider(new SyncAdapterPackagesProvider() {
            public String[] getPackages(String authority, int userId) {
                return ContentService.this.getSyncAdapterPackagesForAuthorityAsUser(authority, userId);
            }
        });
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mCacheReceiver, UserHandle.ALL, packageFilter, null, null);
        IntentFilter localeFilter = new IntentFilter();
        localeFilter.addAction("android.intent.action.LOCALE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mCacheReceiver, UserHandle.ALL, localeFilter, null, null);
        setPackageManager(this.mContext.getPackageManager());
    }

    private static void setPackageManager(PackageManager pm) {
        if (mPackageManager == null && pm != null) {
            mPackageManager = pm;
        }
    }

    void onBootPhase(int phase) {
        if (phase == 550) {
            getSyncManager();
        }
        if (this.mSyncManager != null) {
            this.mSyncManager.onBootPhase(phase);
        }
    }

    public void registerContentObserver(Uri uri, boolean notifyForDescendants, IContentObserver observer, int userHandle, int targetSdkVersion) {
        Throwable th;
        Uri uri2 = uri;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.CONTENT_REGISTERCONTENTOBSERVER, new Object[]{uri2});
        int i;
        if (observer == null || uri2 == null) {
            i = targetSdkVersion;
            throw new IllegalArgumentException("You must pass a valid uri and observer");
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        int userHandle2 = handleIncomingUser(uri2, pid, uid, 1, true, userHandle);
        String msg = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).checkContentProviderAccess(uri.getAuthority(), userHandle2);
        if (msg == null) {
            i = targetSdkVersion;
        } else if (targetSdkVersion >= 26) {
            throw new SecurityException(msg);
        } else if (!msg.startsWith("Failed to find provider")) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring content changes for ");
            stringBuilder.append(uri2);
            stringBuilder.append(" from ");
            stringBuilder.append(uid);
            stringBuilder.append(": ");
            stringBuilder.append(msg);
            Log.w(str, stringBuilder.toString());
            return;
        }
        synchronized (this.mRootNode) {
            try {
                this.mRootNode.addObserverLocked(uri2, observer, notifyForDescendants, this.mRootNode, uid, pid, ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).handleUserForClone(uri.getAuthority(), userHandle2));
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void registerContentObserver(Uri uri, boolean notifyForDescendants, IContentObserver observer) {
        registerContentObserver(uri, notifyForDescendants, observer, UserHandle.getCallingUserId(), 10000);
    }

    public void unregisterContentObserver(IContentObserver observer) {
        if (observer != null) {
            synchronized (this.mRootNode) {
                this.mRootNode.removeObserverLocked(observer);
            }
            return;
        }
        throw new IllegalArgumentException("You must pass a valid observer");
    }

    /* JADX WARNING: Removed duplicated region for block: B:48:0x00c4 A:{SYNTHETIC, Splitter:B:48:0x00c4} */
    /* JADX WARNING: Missing block: B:25:0x0094, code skipped:
            r9 = r2.size();
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:26:0x0096, code skipped:
            r10 = r0;
     */
    /* JADX WARNING: Missing block: B:27:0x0097, code skipped:
            if (r10 >= r9) goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:30:0x009f, code skipped:
            r11 = (com.android.server.content.ContentService.ObserverCall) r2.get(r10);
     */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r11.mObserver.onChange(r11.mSelfChange, r28, r7);
     */
    /* JADX WARNING: Missing block: B:36:0x00a9, code skipped:
            r21 = r2;
            r23 = r3;
     */
    /* JADX WARNING: Missing block: B:37:0x00b0, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:38:0x00b2, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:39:0x00b4, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:40:0x00b5, code skipped:
            r13 = r28;
     */
    /* JADX WARNING: Missing block: B:41:0x00b7, code skipped:
            r9 = r3;
            r11 = r6;
            r12 = r19;
     */
    /* JADX WARNING: Missing block: B:42:0x00bd, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:43:0x00be, code skipped:
            r13 = r28;
     */
    /* JADX WARNING: Missing block: B:44:0x00c0, code skipped:
            r12 = r0;
     */
    /* JADX WARNING: Missing block: B:47:0x00c3, code skipped:
            monitor-enter(r8.mRootNode);
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            android.util.Log.w(TAG, "Found dead observer, removing");
            r0 = r11.mObserver.asBinder();
            r15 = com.android.server.content.ContentService.ObserverNode.access$300(r11.mNode);
     */
    /* JADX WARNING: Missing block: B:50:0x00db, code skipped:
            r1 = r15.size();
            r16 = 0;
     */
    /* JADX WARNING: Missing block: B:51:0x00df, code skipped:
            r21 = r2;
            r2 = r16;
     */
    /* JADX WARNING: Missing block: B:52:0x00e5, code skipped:
            if (r2 < r1) goto L_0x00e7;
     */
    /* JADX WARNING: Missing block: B:55:0x00ed, code skipped:
            r23 = r3;
     */
    /* JADX WARNING: Missing block: B:58:0x00f9, code skipped:
            if (((com.android.server.content.ContentService.ObserverNode.ObserverEntry) r15.get(r2)).observer.asBinder() == r0) goto L_0x00fb;
     */
    /* JADX WARNING: Missing block: B:59:0x00fb, code skipped:
            r15.remove(r2);
            r1 = r1 - 1;
            r2 = r2 - 1;
     */
    /* JADX WARNING: Missing block: B:60:0x0104, code skipped:
            r16 = r2 + 1;
            r2 = r21;
            r3 = r23;
     */
    /* JADX WARNING: Missing block: B:61:0x010b, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:62:0x010c, code skipped:
            r23 = r3;
     */
    /* JADX WARNING: Missing block: B:63:0x010f, code skipped:
            r23 = r3;
     */
    /* JADX WARNING: Missing block: B:66:0x011a, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:67:0x011b, code skipped:
            r21 = r2;
            r23 = r3;
     */
    /* JADX WARNING: Missing block: B:70:?, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:71:0x0121, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:72:0x0122, code skipped:
            r11 = r6;
            r12 = r19;
            r9 = r23;
     */
    /* JADX WARNING: Missing block: B:73:0x0129, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:74:0x012b, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:75:0x012d, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:76:0x012e, code skipped:
            r13 = r28;
     */
    /* JADX WARNING: Missing block: B:77:0x0130, code skipped:
            r9 = r3;
            r11 = r6;
            r12 = r19;
     */
    /* JADX WARNING: Missing block: B:78:0x0136, code skipped:
            r21 = r2;
            r23 = r3;
            r13 = r28;
     */
    /* JADX WARNING: Missing block: B:79:0x013e, code skipped:
            if ((r31 & 1) == 0) goto L_0x016e;
     */
    /* JADX WARNING: Missing block: B:81:?, code skipped:
            r0 = getSyncManager();
     */
    /* JADX WARNING: Missing block: B:82:0x0144, code skipped:
            if (r0 == null) goto L_0x016e;
     */
    /* JADX WARNING: Missing block: B:84:0x014b, code skipped:
            r12 = r19;
     */
    /* JADX WARNING: Missing block: B:87:0x0151, code skipped:
            r15 = r21;
            r25 = r9;
            r9 = r23;
            r11 = r6;
     */
    /* JADX WARNING: Missing block: B:89:?, code skipped:
            r0.scheduleLocalSync(null, r18, r12, r28.getAuthority(), getSyncExemptionForCaller(r12));
     */
    /* JADX WARNING: Missing block: B:90:0x0162, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:91:0x0163, code skipped:
            r11 = r6;
            r9 = r23;
     */
    /* JADX WARNING: Missing block: B:92:0x0167, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:93:0x0168, code skipped:
            r11 = r6;
            r12 = r19;
            r9 = r23;
     */
    /* JADX WARNING: Missing block: B:94:0x016e, code skipped:
            r11 = r6;
            r25 = r9;
            r12 = r19;
            r15 = r21;
            r9 = r23;
     */
    /* JADX WARNING: Missing block: B:95:0x0177, code skipped:
            r1 = r8.mCache;
     */
    /* JADX WARNING: Missing block: B:96:0x0179, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:98:?, code skipped:
            invalidateCacheLocked(r7, getProviderPackageName(r28), r13);
     */
    /* JADX WARNING: Missing block: B:99:0x0181, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:100:0x0182, code skipped:
            restoreCallingIdentity(r9);
     */
    /* JADX WARNING: Missing block: B:101:0x0186, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:107:0x018a, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:108:0x018b, code skipped:
            r9 = r3;
            r11 = r6;
            r12 = r19;
            r13 = r28;
     */
    /* JADX WARNING: Missing block: B:117:0x01a3, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyChange(Uri uri, IContentObserver observer, boolean observerWantsSelfNotifications, int flags, int userHandle, int targetSdkVersion) {
        long identityToken;
        ArrayList<ObserverCall> calls;
        Throwable th;
        ArrayList<ObserverCall> arrayList;
        long identityToken2;
        String str;
        int i;
        Uri uri2;
        Uri uri3 = uri;
        if (uri3 != null) {
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            int callingUserHandle = UserHandle.getCallingUserId();
            int userHandle2 = handleIncomingUser(uri3, pid, uid, 2, true, userHandle);
            String msg = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).checkContentProviderAccess(uri.getAuthority(), userHandle2);
            if (msg == null) {
                int i2 = targetSdkVersion;
            } else if (targetSdkVersion >= 26) {
                throw new SecurityException(msg);
            } else if (!msg.startsWith("Failed to find provider")) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring notify for ");
                stringBuilder.append(uri3);
                stringBuilder.append(" from ");
                stringBuilder.append(uid);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                Log.w(str2, stringBuilder.toString());
                return;
            }
            identityToken = clearCallingIdentity();
            try {
                calls = new ArrayList();
                synchronized (this.mRootNode) {
                    try {
                        int uid2 = uid;
                        try {
                            this.mRootNode.collectObserversLocked(uri3, 0, observer, observerWantsSelfNotifications, flags, userHandle2, calls);
                        } catch (Throwable th2) {
                            th = th2;
                            arrayList = calls;
                            identityToken2 = identityToken;
                            str = msg;
                            i = uid2;
                            uri2 = uri;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        identityToken2 = identityToken;
                        str = msg;
                        i = uid;
                        uri2 = uri3;
                        arrayList = calls;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
                identityToken2 = identityToken;
                str = msg;
                i = uid;
                uri2 = uri3;
                restoreCallingIdentity(identityToken2);
                throw th;
            }
        }
        uri2 = uri3;
        throw new NullPointerException("Uri must not be null");
        int i3 = i + 1;
        calls = calls;
        identityToken = identityToken;
    }

    private int checkUriPermission(Uri uri, int pid, int uid, int modeFlags, int userHandle) {
        try {
            return ActivityManager.getService().checkUriPermission(uri, pid, uid, modeFlags, userHandle, null);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void notifyChange(Uri uri, IContentObserver observer, boolean observerWantsSelfNotifications, boolean syncToNetwork) {
        notifyChange(uri, observer, observerWantsSelfNotifications, syncToNetwork, UserHandle.getCallingUserId(), 10000);
    }

    public void requestSync(Account account, String authority, Bundle extras) {
        Bundle bundle = extras;
        Bundle.setDefusable(bundle, true);
        ContentResolver.validateSyncExtrasBundle(extras);
        int userId = UserHandle.getCallingUserId();
        int uId = Binder.getCallingUid();
        validateExtras(uId, bundle);
        int syncExemption = getSyncExemptionAndCleanUpExtrasForCaller(uId, bundle);
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.scheduleSync(account, userId, uId, authority, bundle, -2, syncExemption);
            }
            restoreCallingIdentity(identityToken);
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    public void sync(SyncRequest request) {
        syncAsUser(request, UserHandle.getCallingUserId());
    }

    private long clampPeriod(long period) {
        long minPeriod = JobInfo.getMinPeriodMillis() / 1000;
        if (period >= minPeriod) {
            return period;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Requested poll frequency of ");
        stringBuilder.append(period);
        stringBuilder.append(" seconds being rounded up to ");
        stringBuilder.append(minPeriod);
        stringBuilder.append("s.");
        Slog.w(str, stringBuilder.toString());
        return minPeriod;
    }

    public void syncAsUser(SyncRequest request, int userId) {
        long identityToken;
        Throwable th;
        int i = userId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to request sync as user: ");
        stringBuilder.append(i);
        enforceCrossUserPermission(i, stringBuilder.toString());
        int callerUid = Binder.getCallingUid();
        Bundle extras = request.getBundle();
        validateExtras(callerUid, extras);
        int syncExemption = getSyncExemptionAndCleanUpExtrasForCaller(callerUid, extras);
        long identityToken2 = clearCallingIdentity();
        Bundle bundle;
        int i2;
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager == null) {
                restoreCallingIdentity(identityToken2);
                return;
            }
            long flextime = request.getSyncFlexTime();
            long runAtTime = request.getSyncRunTime();
            if (request.isPeriodic()) {
                try {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
                    getSyncManager().updateOrAddPeriodicSync(new EndPoint(request.getAccount(), request.getProvider(), i), clampPeriod(runAtTime), flextime, extras);
                    bundle = extras;
                    i2 = callerUid;
                    identityToken = identityToken2;
                } catch (Throwable th2) {
                    th = th2;
                    bundle = extras;
                    i2 = callerUid;
                    identityToken = identityToken2;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            }
            int i3 = i;
            identityToken = identityToken2;
            try {
                syncManager.scheduleSync(request.getAccount(), i3, callerUid, request.getProvider(), extras, -2, syncExemption);
            } catch (Throwable th3) {
                th = th3;
                restoreCallingIdentity(identityToken);
                throw th;
            }
            restoreCallingIdentity(identityToken);
        } catch (Throwable th4) {
            th = th4;
            bundle = extras;
            i2 = callerUid;
            identityToken = identityToken2;
            restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    public void cancelSync(Account account, String authority, ComponentName cname) {
        cancelSyncAsUser(account, authority, cname, UserHandle.getCallingUserId());
    }

    public void cancelSyncAsUser(Account account, String authority, ComponentName cname, int userId) {
        if (authority == null || authority.length() != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("no permission to modify the sync settings for user ");
            stringBuilder.append(userId);
            enforceCrossUserPermission(userId, stringBuilder.toString());
            long identityToken = clearCallingIdentity();
            if (cname != null) {
                Slog.e(TAG, "cname not null.");
                return;
            }
            try {
                SyncManager syncManager = getSyncManager();
                if (syncManager != null) {
                    EndPoint info = new EndPoint(account, authority, userId);
                    syncManager.clearScheduledSyncOperations(info);
                    syncManager.cancelActiveSync(info, null, "API");
                }
                restoreCallingIdentity(identityToken);
            } catch (Throwable th) {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
    }

    public void cancelRequest(SyncRequest request) {
        SyncManager syncManager = getSyncManager();
        if (syncManager != null) {
            int userId = UserHandle.getCallingUserId();
            int callingUid = Binder.getCallingUid();
            if (request.isPeriodic()) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
            }
            Bundle extras = new Bundle(request.getBundle());
            validateExtras(callingUid, extras);
            long identityToken = clearCallingIdentity();
            try {
                EndPoint info = new EndPoint(request.getAccount(), request.getProvider(), userId);
                if (request.isPeriodic()) {
                    SyncManager syncManager2 = getSyncManager();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cancelRequest() by uid=");
                    stringBuilder.append(callingUid);
                    syncManager2.removePeriodicSync(info, extras, stringBuilder.toString());
                }
                syncManager.cancelScheduledSyncOperation(info, extras);
                syncManager.cancelActiveSync(info, extras, "API");
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
    }

    public SyncAdapterType[] getSyncAdapterTypes() {
        return getSyncAdapterTypesAsUser(UserHandle.getCallingUserId());
    }

    public SyncAdapterType[] getSyncAdapterTypesAsUser(int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to read sync settings for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        long identityToken = clearCallingIdentity();
        try {
            SyncAdapterType[] syncAdapterTypes = getSyncManager().getSyncAdapterTypes(userId);
            return syncAdapterTypes;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public String[] getSyncAdapterPackagesForAuthorityAsUser(String authority, int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to read sync settings for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        long identityToken = clearCallingIdentity();
        try {
            String[] syncAdapterPackagesForAuthorityAsUser = getSyncManager().getSyncAdapterPackagesForAuthorityAsUser(authority, userId);
            return syncAdapterPackagesForAuthorityAsUser;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean getSyncAutomatically(Account account, String providerName) {
        return getSyncAutomaticallyAsUser(account, providerName, UserHandle.getCallingUserId());
    }

    public boolean getSyncAutomaticallyAsUser(Account account, String providerName, int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to read the sync settings for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                boolean syncAutomatically = syncManager.getSyncStorageEngine().getSyncAutomatically(account, userId, providerName);
                return syncAutomatically;
            }
            restoreCallingIdentity(identityToken);
            return false;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setSyncAutomatically(Account account, String providerName, boolean sync) {
        setSyncAutomaticallyAsUser(account, providerName, sync, UserHandle.getCallingUserId());
    }

    public void setSyncAutomaticallyAsUser(Account account, String providerName, boolean sync, int userId) {
        int i = userId;
        if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to modify the sync settings for user ");
        stringBuilder.append(i);
        enforceCrossUserPermission(i, stringBuilder.toString());
        int callingUid = Binder.getCallingUid();
        int syncExemptionFlag = getSyncExemptionForCaller(callingUid);
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setSyncAutomatically(account, i, providerName, sync, syncExemptionFlag, callingUid);
            }
            restoreCallingIdentity(identityToken);
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    public void addPeriodicSync(Account account, String authority, Bundle extras, long pollFrequency) {
        Throwable th;
        Account account2 = account;
        Bundle bundle = extras;
        Bundle.setDefusable(bundle, true);
        long j;
        if (account2 == null) {
            j = pollFrequency;
            throw new IllegalArgumentException("Account must not be null");
        } else if (TextUtils.isEmpty(authority)) {
            j = pollFrequency;
            throw new IllegalArgumentException("Authority must not be empty.");
        } else {
            this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
            validateExtras(Binder.getCallingUid(), bundle);
            int userId = UserHandle.getCallingUserId();
            long pollFrequency2 = clampPeriod(pollFrequency);
            long defaultFlex = SyncStorageEngine.calculateDefaultFlexTime(pollFrequency2);
            long identityToken = clearCallingIdentity();
            long identityToken2;
            try {
                identityToken2 = identityToken;
                try {
                    getSyncManager().updateOrAddPeriodicSync(new EndPoint(account2, authority, userId), pollFrequency2, defaultFlex, bundle);
                    restoreCallingIdentity(identityToken2);
                } catch (Throwable th2) {
                    th = th2;
                    restoreCallingIdentity(identityToken2);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken2 = identityToken;
                restoreCallingIdentity(identityToken2);
                throw th;
            }
        }
    }

    public void removePeriodicSync(Account account, String authority, Bundle extras) {
        Bundle.setDefusable(extras, true);
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        } else if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must not be empty");
        } else {
            this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
            validateExtras(Binder.getCallingUid(), extras);
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getCallingUserId();
            long identityToken = clearCallingIdentity();
            try {
                SyncManager syncManager = getSyncManager();
                EndPoint endPoint = new EndPoint(account, authority, userId);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removePeriodicSync() by uid=");
                stringBuilder.append(callingUid);
                syncManager.removePeriodicSync(endPoint, extras, stringBuilder.toString());
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
    }

    public List<PeriodicSync> getPeriodicSyncs(Account account, String providerName, ComponentName cname) {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        } else if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must not be empty");
        } else {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
            int userId = UserHandle.getCallingUserId();
            long identityToken = clearCallingIdentity();
            try {
                List<PeriodicSync> periodicSyncs = getSyncManager().getPeriodicSyncs(new EndPoint(account, providerName, userId));
                return periodicSyncs;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
    }

    public int getIsSyncable(Account account, String providerName) {
        return getIsSyncableAsUser(account, providerName, UserHandle.getCallingUserId());
    }

    public int getIsSyncableAsUser(Account account, String providerName, int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to read the sync settings for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                int computeSyncable = syncManager.computeSyncable(account, userId, providerName, false);
                return computeSyncable;
            }
            restoreCallingIdentity(identityToken);
            return -1;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setIsSyncable(Account account, String providerName, int syncable) {
        if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        syncable = normalizeSyncable(syncable);
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setIsSyncable(account, userId, providerName, syncable, callingUid);
            }
            restoreCallingIdentity(identityToken);
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean getMasterSyncAutomatically() {
        return getMasterSyncAutomaticallyAsUser(UserHandle.getCallingUserId());
    }

    public boolean getMasterSyncAutomaticallyAsUser(int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to read the sync settings for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                boolean masterSyncAutomatically = syncManager.getSyncStorageEngine().getMasterSyncAutomatically(userId);
                return masterSyncAutomatically;
            }
            restoreCallingIdentity(identityToken);
            return false;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setMasterSyncAutomatically(boolean flag) {
        setMasterSyncAutomaticallyAsUser(flag, UserHandle.getCallingUserId());
    }

    public void setMasterSyncAutomaticallyAsUser(boolean flag, int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to set the sync status for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        int callingUid = Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setMasterSyncAutomatically(flag, userId, getSyncExemptionForCaller(callingUid), callingUid);
            }
            restoreCallingIdentity(identityToken);
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean isSyncActive(Account account, String authority, ComponentName cname) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            boolean z;
            if (syncManager == null) {
                z = false;
                return z;
            }
            z = syncManager.getSyncStorageEngine().isSyncActive(new EndPoint(account, authority, userId));
            restoreCallingIdentity(identityToken);
            return z;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public List<SyncInfo> getCurrentSyncs() {
        return getCurrentSyncsAsUser(UserHandle.getCallingUserId());
    }

    public List<SyncInfo> getCurrentSyncsAsUser(int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to read the sync settings for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        boolean canAccessAccounts = this.mContext.checkCallingOrSelfPermission("android.permission.GET_ACCOUNTS") == 0;
        long identityToken = clearCallingIdentity();
        try {
            List<SyncInfo> currentSyncsCopy = getSyncManager().getSyncStorageEngine().getCurrentSyncsCopy(userId, canAccessAccounts);
            return currentSyncsCopy;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public SyncStatusInfo getSyncStatus(Account account, String authority, ComponentName cname) {
        return getSyncStatusAsUser(account, authority, cname, UserHandle.getCallingUserId());
    }

    public SyncStatusInfo getSyncStatusAsUser(Account account, String authority, ComponentName cname, int userId) {
        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to read the sync stats for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            SyncStatusInfo syncStatusInfo;
            if (syncManager == null) {
                syncStatusInfo = null;
                return syncStatusInfo;
            } else if (account == null || authority == null) {
                throw new IllegalArgumentException("Must call sync status with valid authority");
            } else {
                syncStatusInfo = new EndPoint(account, authority, userId);
                SyncStatusInfo statusByAuthority = syncManager.getSyncStorageEngine().getStatusByAuthority(syncStatusInfo);
                restoreCallingIdentity(identityToken);
                return statusByAuthority;
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean isSyncPending(Account account, String authority, ComponentName cname) {
        return isSyncPendingAsUser(account, authority, cname, UserHandle.getCallingUserId());
    }

    public boolean isSyncPendingAsUser(Account account, String authority, ComponentName cname, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no permission to retrieve the sync settings for user ");
        stringBuilder.append(userId);
        enforceCrossUserPermission(userId, stringBuilder.toString());
        long identityToken = clearCallingIdentity();
        SyncManager syncManager = getSyncManager();
        if (syncManager == null) {
            return false;
        }
        if (account == null || authority == null) {
            throw new IllegalArgumentException("Invalid authority specified");
        }
        try {
            boolean isSyncPending = syncManager.getSyncStorageEngine().isSyncPending(new EndPoint(account, authority, userId));
            return isSyncPending;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void addStatusChangeListener(int mask, ISyncStatusObserver callback) {
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (!(syncManager == null || callback == null)) {
                syncManager.getSyncStorageEngine().addStatusChangeListener(mask, callback);
            }
            restoreCallingIdentity(identityToken);
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver callback) {
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (!(syncManager == null || callback == null)) {
                syncManager.getSyncStorageEngine().removeStatusChangeListener(callback);
            }
            restoreCallingIdentity(identityToken);
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    private String getProviderPackageName(Uri uri) {
        ProviderInfo pi = this.mContext.getPackageManager().resolveContentProvider(uri.getAuthority(), 0);
        return pi != null ? pi.packageName : null;
    }

    @GuardedBy("mCache")
    private ArrayMap<Pair<String, Uri>, Bundle> findOrCreateCacheLocked(int userId, String providerPackageName) {
        ArrayMap<String, ArrayMap<Pair<String, Uri>, Bundle>> userCache = (ArrayMap) this.mCache.get(userId);
        if (userCache == null) {
            userCache = new ArrayMap();
            this.mCache.put(userId, userCache);
        }
        ArrayMap<Pair<String, Uri>, Bundle> packageCache = (ArrayMap) userCache.get(providerPackageName);
        if (packageCache != null) {
            return packageCache;
        }
        packageCache = new ArrayMap();
        userCache.put(providerPackageName, packageCache);
        return packageCache;
    }

    @GuardedBy("mCache")
    private void invalidateCacheLocked(int userId, String providerPackageName, Uri uri) {
        ArrayMap<String, ArrayMap<Pair<String, Uri>, Bundle>> userCache = (ArrayMap) this.mCache.get(userId);
        if (userCache != null) {
            ArrayMap<Pair<String, Uri>, Bundle> packageCache = (ArrayMap) userCache.get(providerPackageName);
            if (packageCache != null) {
                if (uri != null) {
                    int i = 0;
                    while (i < packageCache.size()) {
                        Pair<String, Uri> key = (Pair) packageCache.keyAt(i);
                        if (key.second == null || !((Uri) key.second).toString().startsWith(uri.toString())) {
                            i++;
                        } else {
                            packageCache.removeAt(i);
                        }
                    }
                } else {
                    packageCache.clear();
                }
            }
        }
    }

    public void putCache(String packageName, Uri key, Bundle value, int userId) {
        Bundle.setDefusable(value, true);
        enforceCrossUserPermission(userId, TAG);
        this.mContext.enforceCallingOrSelfPermission("android.permission.CACHE_CONTENT", TAG);
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), packageName);
        String providerPackageName = getProviderPackageName(key);
        Pair<String, Uri> fullKey = Pair.create(packageName, key);
        synchronized (this.mCache) {
            ArrayMap<Pair<String, Uri>, Bundle> cache = findOrCreateCacheLocked(userId, providerPackageName);
            if (value != null) {
                cache.put(fullKey, value);
            } else {
                cache.remove(fullKey);
            }
        }
    }

    public Bundle getCache(String packageName, Uri key, int userId) {
        Bundle bundle;
        enforceCrossUserPermission(userId, TAG);
        this.mContext.enforceCallingOrSelfPermission("android.permission.CACHE_CONTENT", TAG);
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), packageName);
        String providerPackageName = getProviderPackageName(key);
        Pair<String, Uri> fullKey = Pair.create(packageName, key);
        synchronized (this.mCache) {
            bundle = (Bundle) findOrCreateCacheLocked(userId, providerPackageName).get(fullKey);
        }
        return bundle;
    }

    private int handleIncomingUser(Uri uri, int pid, int uid, int modeFlags, boolean allowNonFull, int userId) {
        if (userId == -2) {
            userId = ActivityManager.getCurrentUser();
        }
        if (userId == -1) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", TAG);
        } else if (userId < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid user: ");
            stringBuilder.append(userId);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!(userId == UserHandle.getCallingUserId() || checkUriPermission(uri, pid, uid, modeFlags, userId) == 0)) {
            boolean allow = false;
            if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
                allow = true;
            } else if (allowNonFull && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") == 0) {
                allow = true;
            }
            if (!allow) {
                String permissions = allowNonFull ? "android.permission.INTERACT_ACROSS_USERS_FULL or android.permission.INTERACT_ACROSS_USERS" : "android.permission.INTERACT_ACROSS_USERS_FULL";
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ContentServiceNeither user ");
                stringBuilder2.append(uid);
                stringBuilder2.append(" nor current process has ");
                stringBuilder2.append(permissions);
                throw new SecurityException(stringBuilder2.toString());
            }
        }
        return userId;
    }

    private void enforceCrossUserPermission(int userHandle, String message) {
        if (UserHandle.getCallingUserId() != userHandle) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
        }
    }

    private static int normalizeSyncable(int syncable) {
        if (syncable > 0) {
            return 1;
        }
        if (syncable == 0) {
            return 0;
        }
        return -2;
    }

    private void validateExtras(int callingUid, Bundle extras) {
        if (extras.containsKey("v_exemption") && callingUid != 0 && callingUid != 1000 && callingUid != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            String msg = "Invalid extras specified.";
            Log.w(TAG, "Invalid extras specified. requestsync -f/-F needs to run on 'adb shell'");
            throw new SecurityException("Invalid extras specified.");
        }
    }

    private int getSyncExemptionForCaller(int callingUid) {
        return getSyncExemptionAndCleanUpExtrasForCaller(callingUid, null);
    }

    private int getSyncExemptionAndCleanUpExtrasForCaller(int callingUid, Bundle extras) {
        int procState;
        if (extras != null) {
            int exemption = extras.getInt("v_exemption", -1);
            extras.remove("v_exemption");
            if (exemption != -1) {
                return exemption;
            }
        }
        ActivityManagerInternal ami = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        if (ami != null) {
            procState = ami.getUidProcessState(callingUid);
        } else {
            procState = 19;
        }
        if (procState <= 2) {
            return 2;
        }
        if (procState <= 5) {
            return 1;
        }
        return 0;
    }

    private void enforceShell(String method) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && callingUid != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Non-shell user attempted to call ");
            stringBuilder.append(method);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public void resetTodayStats() {
        enforceShell("resetTodayStats");
        if (this.mSyncManager != null) {
            long token = Binder.clearCallingIdentity();
            try {
                this.mSyncManager.resetTodayStats();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new ContentShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }
}
