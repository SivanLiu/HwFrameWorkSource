package com.android.server.slice;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.ContentProviderHolder;
import android.app.IActivityManager;
import android.app.slice.ISliceManager.Stub;
import android.app.slice.SliceSpec;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml.Encoding;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.AssistUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class SliceManagerService extends Stub {
    private static final String TAG = "SliceManagerService";
    private final AppOpsManager mAppOps;
    private final UsageStatsManagerInternal mAppUsageStats;
    private final AssistUtils mAssistUtils;
    @GuardedBy("mLock")
    private final SparseArray<PackageMatchingCache> mAssistantLookup;
    private final Context mContext;
    private final Handler mHandler;
    @GuardedBy("mLock")
    private final SparseArray<PackageMatchingCache> mHomeLookup;
    private final Object mLock;
    private final PackageManagerInternal mPackageManagerInternal;
    private final SlicePermissionManager mPermissions;
    @GuardedBy("mLock")
    private final ArrayMap<Uri, PinnedSliceState> mPinnedSlicesByUri;
    private final BroadcastReceiver mReceiver;

    static class PackageMatchingCache {
        private String mCurrentPkg;
        private final Supplier<String> mPkgSource;

        public PackageMatchingCache(Supplier<String> pkgSource) {
            this.mPkgSource = pkgSource;
        }

        public boolean matches(String pkgCandidate) {
            if (pkgCandidate == null) {
                return false;
            }
            if (Objects.equals(pkgCandidate, this.mCurrentPkg)) {
                return true;
            }
            this.mCurrentPkg = (String) this.mPkgSource.get();
            return Objects.equals(pkgCandidate, this.mCurrentPkg);
        }
    }

    private class SliceGrant {
        private final String mPkg;
        private final Uri mUri;
        private final int mUserId;

        public SliceGrant(Uri uri, String pkg, int userId) {
            this.mUri = uri;
            this.mPkg = pkg;
            this.mUserId = userId;
        }

        public int hashCode() {
            return this.mUri.hashCode() + this.mPkg.hashCode();
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof SliceGrant)) {
                return false;
            }
            SliceGrant other = (SliceGrant) obj;
            if (Objects.equals(other.mUri, this.mUri) && Objects.equals(other.mPkg, this.mPkg) && other.mUserId == this.mUserId) {
                z = true;
            }
            return z;
        }
    }

    public static class Lifecycle extends SystemService {
        private SliceManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            this.mService = new SliceManagerService(getContext());
            publishBinderService("slice", this.mService);
        }

        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mService.systemReady();
            }
        }

        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }

        public void onStopUser(int userHandle) {
            this.mService.onStopUser(userHandle);
        }
    }

    public SliceManagerService(Context context) {
        this(context, createHandler().getLooper());
    }

    @VisibleForTesting
    SliceManagerService(Context context, Looper looper) {
        this.mLock = new Object();
        this.mPinnedSlicesByUri = new ArrayMap();
        this.mAssistantLookup = new SparseArray();
        this.mHomeLookup = new SparseArray();
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (userId == -10000) {
                    String str = SliceManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Intent broadcast does not contain user handle: ");
                    stringBuilder.append(intent);
                    Slog.w(str, stringBuilder.toString());
                    return;
                }
                Uri data = intent.getData();
                String pkg = data != null ? data.getSchemeSpecificPart() : null;
                String str2;
                if (pkg == null) {
                    str2 = SliceManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Intent broadcast does not contain package name: ");
                    stringBuilder2.append(intent);
                    Slog.w(str2, stringBuilder2.toString());
                    return;
                }
                str2 = intent.getAction();
                boolean z = true;
                int hashCode = str2.hashCode();
                if (hashCode != 267468725) {
                    if (hashCode == 525384130 && str2.equals("android.intent.action.PACKAGE_REMOVED")) {
                        z = false;
                    }
                } else if (str2.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                    z = true;
                }
                switch (z) {
                    case false:
                        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                            SliceManagerService.this.mPermissions.removePkg(pkg, userId);
                            break;
                        }
                        break;
                    case true:
                        SliceManagerService.this.mPermissions.removePkg(pkg, userId);
                        break;
                }
            }
        };
        this.mContext = context;
        this.mPackageManagerInternal = (PackageManagerInternal) Preconditions.checkNotNull((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mAssistUtils = new AssistUtils(context);
        this.mHandler = new Handler(looper);
        this.mAppUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        this.mPermissions = new SlicePermissionManager(this.mContext, looper);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, this.mHandler);
    }

    private void systemReady() {
    }

    private void onUnlockUser(int userId) {
    }

    private void onStopUser(int userId) {
        synchronized (this.mLock) {
            this.mPinnedSlicesByUri.values().removeIf(new -$$Lambda$SliceManagerService$EsoJb3dNe0G_qzoQixj72OS5gnw(userId));
        }
    }

    static /* synthetic */ boolean lambda$onStopUser$0(int userId, PinnedSliceState s) {
        return ContentProvider.getUserIdFromUri(s.getUri()) == userId;
    }

    public Uri[] getPinnedSlices(String pkg) {
        verifyCaller(pkg);
        int callingUser = Binder.getCallingUserHandle().getIdentifier();
        ArrayList<Uri> ret = new ArrayList();
        synchronized (this.mLock) {
            for (PinnedSliceState state : this.mPinnedSlicesByUri.values()) {
                if (Objects.equals(pkg, state.getPkg())) {
                    Uri uri = state.getUri();
                    if (ContentProvider.getUserIdFromUri(uri, callingUser) == callingUser) {
                        ret.add(ContentProvider.getUriWithoutUserId(uri));
                    }
                }
            }
        }
        return (Uri[]) ret.toArray(new Uri[ret.size()]);
    }

    public void pinSlice(String pkg, Uri uri, SliceSpec[] specs, IBinder token) throws RemoteException {
        verifyCaller(pkg);
        enforceAccess(pkg, uri);
        int user = Binder.getCallingUserHandle().getIdentifier();
        uri = ContentProvider.maybeAddUserId(uri, user);
        String slicePkg = getProviderPkg(uri, user);
        getOrCreatePinnedSlice(uri, slicePkg).pin(pkg, specs, token);
        this.mHandler.post(new -$$Lambda$SliceManagerService$pJ39TkC3AEVezLFEPuJgSQSTDJM(this, slicePkg, pkg, user));
    }

    public static /* synthetic */ void lambda$pinSlice$1(SliceManagerService sliceManagerService, String slicePkg, String pkg, int user) {
        if (slicePkg != null && !Objects.equals(pkg, slicePkg)) {
            UsageStatsManagerInternal usageStatsManagerInternal = sliceManagerService.mAppUsageStats;
            int i = (sliceManagerService.isAssistant(pkg, user) || sliceManagerService.isDefaultHomeApp(pkg, user)) ? 13 : 14;
            usageStatsManagerInternal.reportEvent(slicePkg, user, i);
        }
    }

    public void unpinSlice(String pkg, Uri uri, IBinder token) throws RemoteException {
        verifyCaller(pkg);
        enforceAccess(pkg, uri);
        uri = ContentProvider.maybeAddUserId(uri, Binder.getCallingUserHandle().getIdentifier());
        if (getPinnedSlice(uri).unpin(pkg, token)) {
            removePinnedSlice(uri);
        }
    }

    public boolean hasSliceAccess(String pkg) throws RemoteException {
        verifyCaller(pkg);
        return hasFullSliceAccess(pkg, Binder.getCallingUserHandle().getIdentifier());
    }

    public SliceSpec[] getPinnedSpecs(Uri uri, String pkg) throws RemoteException {
        verifyCaller(pkg);
        enforceAccess(pkg, uri);
        return getPinnedSlice(uri).getSpecs();
    }

    public void grantSlicePermission(String pkg, String toPkg, Uri uri) throws RemoteException {
        verifyCaller(pkg);
        int user = Binder.getCallingUserHandle().getIdentifier();
        enforceOwner(pkg, uri, user);
        this.mPermissions.grantSliceAccess(toPkg, user, pkg, user, uri);
    }

    public void revokeSlicePermission(String pkg, String toPkg, Uri uri) throws RemoteException {
        verifyCaller(pkg);
        int user = Binder.getCallingUserHandle().getIdentifier();
        enforceOwner(pkg, uri, user);
        this.mPermissions.revokeSliceAccess(toPkg, user, pkg, user, uri);
    }

    public int checkSlicePermission(Uri uri, String pkg, int pid, int uid, String[] autoGrantPermissions) {
        Uri uri2 = uri;
        String str = pkg;
        int i = pid;
        int i2 = uid;
        String[] strArr = autoGrantPermissions;
        int userId = UserHandle.getUserId(uid);
        if (str == null) {
            String[] packagesForUid = this.mContext.getPackageManager().getPackagesForUid(i2);
            int length = packagesForUid.length;
            int i3 = 0;
            while (i3 < length) {
                int i4 = i3;
                int i5 = length;
                if (checkSlicePermission(uri2, packagesForUid[i3], i, i2, strArr) == 0) {
                    return 0;
                }
                i3 = i4 + 1;
                length = i5;
            }
            return -1;
        } else if (hasFullSliceAccess(str, userId) || this.mPermissions.hasPermission(str, userId, uri2)) {
            return 0;
        } else {
            if (strArr != null) {
                enforceOwner(str, uri2, userId);
                for (String perm : strArr) {
                    if (this.mContext.checkPermission(perm, i, i2) == 0) {
                        int providerUser = ContentProvider.getUserIdFromUri(uri2, userId);
                        this.mPermissions.grantSliceAccess(str, userId, getProviderPkg(uri2, providerUser), providerUser, uri2);
                        return 0;
                    }
                }
            }
            return this.mContext.checkUriPermission(uri2, i, i2, 2) == 0 ? 0 : -1;
        }
    }

    public void grantPermissionFromUser(Uri uri, String pkg, String callingPkg, boolean allSlices) {
        verifyCaller(callingPkg);
        getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_SLICE_PERMISSIONS", "Slice granting requires MANAGE_SLICE_PERMISSIONS");
        int userId = Binder.getCallingUserHandle().getIdentifier();
        if (allSlices) {
            this.mPermissions.grantFullAccess(pkg, userId);
        } else {
            Uri grantUri = uri.buildUpon().path(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).build();
            int providerUser = ContentProvider.getUserIdFromUri(grantUri, userId);
            this.mPermissions.grantSliceAccess(pkg, userId, getProviderPkg(grantUri, providerUser), providerUser, grantUri);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.getContentResolver().notifyChange(uri, null);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public byte[] getBackupPayload(int user) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Caller must be system");
        } else if (user != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getBackupPayload: cannot backup policy for user ");
            stringBuilder.append(user);
            Slog.w(str, stringBuilder.toString());
            return null;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                XmlSerializer out = XmlPullParserFactory.newInstance().newSerializer();
                out.setOutput(baos, Encoding.UTF_8.name());
                this.mPermissions.writeBackup(out);
                out.flush();
                return baos.toByteArray();
            } catch (IOException | XmlPullParserException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getBackupPayload: error writing payload for user ");
                stringBuilder2.append(user);
                Slog.w(str2, stringBuilder2.toString(), e);
                return null;
            }
        }
    }

    public void applyRestore(byte[] payload, int user) {
        String str;
        StringBuilder stringBuilder;
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Caller must be system");
        } else if (payload == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("applyRestore: no payload to restore for user ");
            stringBuilder.append(user);
            Slog.w(str, stringBuilder.toString());
        } else if (user != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("applyRestore: cannot restore policy for user ");
            stringBuilder.append(user);
            Slog.w(str, stringBuilder.toString());
        } else {
            ByteArrayInputStream bais = new ByteArrayInputStream(payload);
            try {
                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setInput(bais, Encoding.UTF_8.name());
                this.mPermissions.readRestore(parser);
            } catch (IOException | NumberFormatException | XmlPullParserException e) {
                Slog.w(TAG, "applyRestore: error reading payload", e);
            }
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new SliceShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    private void enforceOwner(String pkg, Uri uri, int user) {
        if (!Objects.equals(getProviderPkg(uri, user), pkg) || pkg == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Caller must own ");
            stringBuilder.append(uri);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    protected void removePinnedSlice(Uri uri) {
        synchronized (this.mLock) {
            ((PinnedSliceState) this.mPinnedSlicesByUri.remove(uri)).destroy();
        }
    }

    private PinnedSliceState getPinnedSlice(Uri uri) {
        PinnedSliceState manager;
        synchronized (this.mLock) {
            manager = (PinnedSliceState) this.mPinnedSlicesByUri.get(uri);
            if (manager != null) {
            } else {
                throw new IllegalStateException(String.format("Slice %s not pinned", new Object[]{uri.toString()}));
            }
        }
        return manager;
    }

    private PinnedSliceState getOrCreatePinnedSlice(Uri uri, String pkg) {
        PinnedSliceState manager;
        synchronized (this.mLock) {
            manager = (PinnedSliceState) this.mPinnedSlicesByUri.get(uri);
            if (manager == null) {
                manager = createPinnedSlice(uri, pkg);
                this.mPinnedSlicesByUri.put(uri, manager);
            }
        }
        return manager;
    }

    @VisibleForTesting
    protected PinnedSliceState createPinnedSlice(Uri uri, String pkg) {
        return new PinnedSliceState(this, uri, pkg);
    }

    public Object getLock() {
        return this.mLock;
    }

    public Context getContext() {
        return this.mContext;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    protected int checkAccess(String pkg, Uri uri, int uid, int pid) {
        return checkSlicePermission(uri, pkg, uid, pid, null);
    }

    private String getProviderPkg(Uri uri, int user) {
        long ident = Binder.clearCallingIdentity();
        try {
            IBinder token = new Binder();
            IActivityManager activityManager = ActivityManager.getService();
            String providerName = ContentProvider.getUriWithoutUserId(uri).getAuthority();
            try {
                ContentProviderHolder holder = activityManager.getContentProviderExternal(providerName, ContentProvider.getUserIdFromUri(uri, user), token);
                if (holder == null || holder.info == null) {
                    if (holder != null) {
                        if (holder.provider != null) {
                            activityManager.removeContentProviderExternal(providerName, token);
                        }
                    }
                    Binder.restoreCallingIdentity(ident);
                    return null;
                }
                String str = holder.info.packageName;
                if (holder != null) {
                    if (holder.provider != null) {
                        activityManager.removeContentProviderExternal(providerName, token);
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return str;
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            } catch (Throwable th) {
                if (null != null) {
                    if (null.provider != null) {
                        activityManager.removeContentProviderExternal(providerName, token);
                    }
                }
            }
        } catch (Throwable th2) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void enforceCrossUser(String pkg, Uri uri) {
        int user = Binder.getCallingUserHandle().getIdentifier();
        if (ContentProvider.getUserIdFromUri(uri, user) != user) {
            getContext().enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "Slice interaction across users requires INTERACT_ACROSS_USERS_FULL");
        }
    }

    private void enforceAccess(String pkg, Uri uri) throws RemoteException {
        if (checkAccess(pkg, uri, Binder.getCallingUid(), Binder.getCallingPid()) == 0 || Objects.equals(pkg, getProviderPkg(uri, ContentProvider.getUserIdFromUri(uri, Binder.getCallingUserHandle().getIdentifier())))) {
            enforceCrossUser(pkg, uri);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Access to slice ");
        stringBuilder.append(uri);
        stringBuilder.append(" is required");
        throw new SecurityException(stringBuilder.toString());
    }

    private void verifyCaller(String pkg) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), pkg);
    }

    private boolean hasFullSliceAccess(String pkg, int userId) {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean ret = isDefaultHomeApp(pkg, userId) || isAssistant(pkg, userId) || isGrantedFullAccess(pkg, userId);
            Binder.restoreCallingIdentity(ident);
            return ret;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isAssistant(String pkg, int userId) {
        return getAssistantMatcher(userId).matches(pkg);
    }

    private boolean isDefaultHomeApp(String pkg, int userId) {
        return getHomeMatcher(userId).matches(pkg);
    }

    private PackageMatchingCache getAssistantMatcher(int userId) {
        PackageMatchingCache matcher = (PackageMatchingCache) this.mAssistantLookup.get(userId);
        if (matcher != null) {
            return matcher;
        }
        matcher = new PackageMatchingCache(new -$$Lambda$SliceManagerService$ic_PW16x_KcVi-NszMwHhErqI0s(this, userId));
        this.mAssistantLookup.put(userId, matcher);
        return matcher;
    }

    private PackageMatchingCache getHomeMatcher(int userId) {
        PackageMatchingCache matcher = (PackageMatchingCache) this.mHomeLookup.get(userId);
        if (matcher != null) {
            return matcher;
        }
        matcher = new PackageMatchingCache(new -$$Lambda$SliceManagerService$LkusK1jmu9JKJTiMRWqWxNGEGbY(this, userId));
        this.mHomeLookup.put(userId, matcher);
        return matcher;
    }

    private String getAssistant(int userId) {
        ComponentName cn = this.mAssistUtils.getAssistComponentForUser(userId);
        if (cn == null) {
            return null;
        }
        return cn.getPackageName();
    }

    @VisibleForTesting
    protected String getDefaultHome(int userId) {
        long token = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> allHomeCandidates = new ArrayList();
            ComponentName defaultLauncher = this.mPackageManagerInternal.getHomeActivitiesAsUser(allHomeCandidates, userId);
            ComponentName detected = null;
            if (defaultLauncher != null) {
                detected = defaultLauncher;
            }
            if (detected == null) {
                int size = allHomeCandidates.size();
                int lastPriority = Integer.MIN_VALUE;
                for (int i = 0; i < size; i++) {
                    ResolveInfo ri = (ResolveInfo) allHomeCandidates.get(i);
                    if (ri.activityInfo.applicationInfo.isSystemApp()) {
                        if (ri.priority >= lastPriority) {
                            detected = ri.activityInfo.getComponentName();
                            lastPriority = ri.priority;
                        }
                    }
                }
            }
            String packageName = detected != null ? detected.getPackageName() : null;
            Binder.restoreCallingIdentity(token);
            return packageName;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isGrantedFullAccess(String pkg, int userId) {
        return this.mPermissions.hasFullAccess(pkg, userId);
    }

    private static ServiceThread createHandler() {
        ServiceThread handlerThread = new ServiceThread(TAG, 10, true);
        handlerThread.start();
        return handlerThread;
    }

    public String[] getAllPackagesGranted(String authority) {
        return this.mPermissions.getAllPackagesGranted(getProviderPkg(new Builder().scheme("content").authority(authority).build(), 0));
    }
}
