package com.android.server.pm.permission;

import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.NewPermissionInfo;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.Permission;
import android.content.pm.PackageParser.PermissionGroup;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.metrics.LogMaker;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.util.ArrayUtils;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.SystemConfig.PermissionEntry;
import com.android.server.Watchdog;
import com.android.server.pm.HwCustPackageManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageManagerServiceUtils;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.SharedUserSetting;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.permission.DefaultPermissionGrantPolicy.DefaultPermissionGrantedCallback;
import com.android.server.pm.permission.PermissionManagerInternal.PermissionCallback;
import com.android.server.pm.permission.PermissionsState.PermissionState;
import com.android.server.power.IHwShutdownThread;
import huawei.cust.HwCustUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import libcore.util.EmptyArray;

public class PermissionManagerService {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final int GRANT_DENIED = 1;
    private static final int GRANT_INSTALL = 2;
    private static final int GRANT_RUNTIME = 3;
    private static final int GRANT_UPGRADE = 4;
    private static final int MAX_PERMISSION_TREE_FOOTPRINT = 32768;
    private static final String TAG = "PackageManager";
    private static final int UPDATE_PERMISSIONS_ALL = 1;
    private static final int UPDATE_PERMISSIONS_REPLACE_ALL = 4;
    private static final int UPDATE_PERMISSIONS_REPLACE_PKG = 2;
    private final Context mContext;
    private HwCustPackageManagerService mCustPms = ((HwCustPackageManagerService) HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]));
    private final DefaultPermissionGrantPolicy mDefaultPermissionGrantPolicy;
    private final int[] mGlobalGids;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final Object mLock;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final PackageManagerInternal mPackageManagerInt;
    @GuardedBy("mLock")
    private ArraySet<String> mPrivappPermissionsViolations;
    @GuardedBy("mLock")
    private final PermissionSettings mSettings;
    private final SparseArray<ArraySet<String>> mSystemPermissions;
    @GuardedBy("mLock")
    private boolean mSystemReady;
    private final UserManagerInternal mUserManagerInt;

    private class PermissionManagerInternalImpl extends PermissionManagerInternal {
        private PermissionManagerInternalImpl() {
        }

        public void systemReady() {
            PermissionManagerService.this.systemReady();
        }

        public boolean isPermissionsReviewRequired(Package pkg, int userId) {
            return PermissionManagerService.this.isPermissionsReviewRequired(pkg, userId);
        }

        public void revokeRuntimePermissionsIfGroupChanged(Package newPackage, Package oldPackage, ArrayList<String> allPackageNames, PermissionCallback permissionCallback) {
            PermissionManagerService.this.revokeRuntimePermissionsIfGroupChanged(newPackage, oldPackage, allPackageNames, permissionCallback);
        }

        public void addAllPermissions(Package pkg, boolean chatty) {
            PermissionManagerService.this.addAllPermissions(pkg, chatty);
        }

        public void addAllPermissionGroups(Package pkg, boolean chatty) {
            PermissionManagerService.this.addAllPermissionGroups(pkg, chatty);
        }

        public void removeAllPermissions(Package pkg, boolean chatty) {
            PermissionManagerService.this.removeAllPermissions(pkg, chatty);
        }

        public boolean addDynamicPermission(PermissionInfo info, boolean async, int callingUid, PermissionCallback callback) {
            return PermissionManagerService.this.addDynamicPermission(info, callingUid, callback);
        }

        public void removeDynamicPermission(String permName, int callingUid, PermissionCallback callback) {
            PermissionManagerService.this.removeDynamicPermission(permName, callingUid, callback);
        }

        public void grantRuntimePermission(String permName, String packageName, boolean overridePolicy, int callingUid, int userId, PermissionCallback callback) {
            PermissionManagerService.this.grantRuntimePermission(permName, packageName, overridePolicy, callingUid, userId, callback);
        }

        public void grantRequestedRuntimePermissions(Package pkg, int[] userIds, String[] grantedPermissions, int callingUid, PermissionCallback callback) {
            PermissionManagerService.this.grantRequestedRuntimePermissions(pkg, userIds, grantedPermissions, callingUid, callback);
        }

        public void grantRuntimePermissionsGrantedToDisabledPackage(Package pkg, int callingUid, PermissionCallback callback) {
            PermissionManagerService.this.grantRuntimePermissionsGrantedToDisabledPackageLocked(pkg, callingUid, callback);
        }

        public void revokeRuntimePermission(String permName, String packageName, boolean overridePolicy, int callingUid, int userId, PermissionCallback callback) {
            PermissionManagerService.this.revokeRuntimePermission(permName, packageName, overridePolicy, callingUid, userId, callback);
        }

        public void updatePermissions(String packageName, Package pkg, boolean replaceGrant, Collection<Package> allPackages, PermissionCallback callback) {
            PermissionManagerService.this.updatePermissions(packageName, pkg, replaceGrant, allPackages, callback);
        }

        public void updateAllPermissions(String volumeUuid, boolean sdkUpdated, Collection<Package> allPackages, PermissionCallback callback) {
            PermissionManagerService.this.updateAllPermissions(volumeUuid, sdkUpdated, allPackages, callback);
        }

        public String[] getAppOpPermissionPackages(String permName) {
            return PermissionManagerService.this.getAppOpPermissionPackages(permName);
        }

        public int getPermissionFlags(String permName, String packageName, int callingUid, int userId) {
            return PermissionManagerService.this.getPermissionFlags(permName, packageName, callingUid, userId);
        }

        public void updatePermissionFlags(String permName, String packageName, int flagMask, int flagValues, int callingUid, int userId, PermissionCallback callback) {
            PermissionManagerService.this.updatePermissionFlags(permName, packageName, flagMask, flagValues, callingUid, userId, callback);
        }

        public boolean updatePermissionFlagsForAllApps(int flagMask, int flagValues, int callingUid, int userId, Collection<Package> packages, PermissionCallback callback) {
            return PermissionManagerService.this.updatePermissionFlagsForAllApps(flagMask, flagValues, callingUid, userId, packages, callback);
        }

        public void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, String message) {
            PermissionManagerService.this.enforceCrossUserPermission(callingUid, userId, requireFullPermission, checkShell, false, message);
        }

        public void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, boolean requirePermissionWhenSameUser, String message) {
            PermissionManagerService.this.enforceCrossUserPermission(callingUid, userId, requireFullPermission, checkShell, requirePermissionWhenSameUser, message);
        }

        public void enforceGrantRevokeRuntimePermissionPermissions(String message) {
            PermissionManagerService.this.enforceGrantRevokeRuntimePermissionPermissions(message);
        }

        public int checkPermission(String permName, String packageName, int callingUid, int userId) {
            return PermissionManagerService.this.checkPermission(permName, packageName, callingUid, userId);
        }

        public int checkUidPermission(String permName, Package pkg, int uid, int callingUid) {
            return PermissionManagerService.this.checkUidPermission(permName, pkg, uid, callingUid);
        }

        public PermissionGroupInfo getPermissionGroupInfo(String groupName, int flags, int callingUid) {
            return PermissionManagerService.this.getPermissionGroupInfo(groupName, flags, callingUid);
        }

        public List<PermissionGroupInfo> getAllPermissionGroups(int flags, int callingUid) {
            return PermissionManagerService.this.getAllPermissionGroups(flags, callingUid);
        }

        public PermissionInfo getPermissionInfo(String permName, String packageName, int flags, int callingUid) {
            return PermissionManagerService.this.getPermissionInfo(permName, packageName, flags, callingUid);
        }

        public List<PermissionInfo> getPermissionInfoByGroup(String group, int flags, int callingUid) {
            return PermissionManagerService.this.getPermissionInfoByGroup(group, flags, callingUid);
        }

        public PermissionSettings getPermissionSettings() {
            return PermissionManagerService.this.mSettings;
        }

        public DefaultPermissionGrantPolicy getDefaultPermissionGrantPolicy() {
            return PermissionManagerService.this.mDefaultPermissionGrantPolicy;
        }

        public BasePermission getPermissionTEMP(String permName) {
            BasePermission permissionLocked;
            synchronized (PermissionManagerService.this.mLock) {
                permissionLocked = PermissionManagerService.this.mSettings.getPermissionLocked(permName);
            }
            return permissionLocked;
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
    private void grantRequestedRuntimePermissionsForUser(android.content.pm.PackageParser.Package r22, int r23, java.lang.String[] r24, int r25, com.android.server.pm.permission.PermissionManagerInternal.PermissionCallback r26) {
        /*
        r21 = this;
        r9 = r21;
        r10 = r22;
        r11 = r23;
        r12 = r24;
        r0 = r10.mExtras;
        r13 = r0;
        r13 = (com.android.server.pm.PackageSetting) r13;
        if (r13 != 0) goto L_0x0010;
    L_0x000f:
        return;
    L_0x0010:
        r14 = r13.getPermissionsState();
        r15 = 20;
        r0 = r10.applicationInfo;
        r0 = r0.targetSdkVersion;
        r1 = 23;
        if (r0 < r1) goto L_0x0020;
    L_0x001e:
        r0 = 1;
        goto L_0x0021;
    L_0x0020:
        r0 = 0;
    L_0x0021:
        r16 = r0;
        r0 = r9.mPackageManagerInt;
        r1 = r10.packageName;
        r17 = r0.isInstantApp(r1, r11);
        r0 = r10.requestedPermissions;
        r0 = r0.iterator();
    L_0x0031:
        r1 = r0.hasNext();
        if (r1 == 0) goto L_0x00ae;
    L_0x0037:
        r1 = r0.next();
        r8 = r1;
        r8 = (java.lang.String) r8;
        r1 = r9.mLock;
        monitor-enter(r1);
        r2 = r9.mSettings;	 Catch:{ all -> 0x00a7 }
        r2 = r2.getPermissionLocked(r8);	 Catch:{ all -> 0x00a7 }
        r7 = r2;	 Catch:{ all -> 0x00a7 }
        monitor-exit(r1);	 Catch:{ all -> 0x00a7 }
        if (r7 == 0) goto L_0x00a6;
    L_0x004b:
        r1 = r7.isRuntime();
        if (r1 != 0) goto L_0x0057;
    L_0x0051:
        r1 = r7.isDevelopment();
        if (r1 == 0) goto L_0x00a6;
    L_0x0057:
        if (r17 == 0) goto L_0x005f;
    L_0x0059:
        r1 = r7.isInstant();
        if (r1 == 0) goto L_0x00a6;
    L_0x005f:
        if (r16 != 0) goto L_0x0067;
    L_0x0061:
        r1 = r7.isRuntimeOnly();
        if (r1 != 0) goto L_0x00a6;
    L_0x0067:
        if (r12 == 0) goto L_0x006f;
    L_0x0069:
        r1 = com.android.internal.util.ArrayUtils.contains(r12, r8);
        if (r1 == 0) goto L_0x00a6;
    L_0x006f:
        r18 = r14.getPermissionFlags(r8, r11);
        if (r16 == 0) goto L_0x0089;
    L_0x0075:
        r1 = r18 & 20;
        if (r1 != 0) goto L_0x00a6;
    L_0x0079:
        r3 = r10.packageName;
        r4 = 0;
        r1 = r9;
        r2 = r8;
        r5 = r25;
        r6 = r11;
        r19 = r7;
        r7 = r26;
        r1.grantRuntimePermission(r2, r3, r4, r5, r6, r7);
        goto L_0x00a6;
    L_0x0089:
        r19 = r7;
        r1 = r9.mSettings;
        r1 = r1.mPermissionReviewRequired;
        if (r1 == 0) goto L_0x00a6;
    L_0x0091:
        r1 = r18 & 64;
        if (r1 == 0) goto L_0x00a6;
    L_0x0095:
        r3 = r10.packageName;
        r4 = 64;
        r5 = 0;
        r1 = r9;
        r2 = r8;
        r6 = r25;
        r7 = r11;
        r20 = r8;
        r8 = r26;
        r1.updatePermissionFlags(r2, r3, r4, r5, r6, r7, r8);
    L_0x00a6:
        goto L_0x0031;
    L_0x00a7:
        r0 = move-exception;
        r20 = r8;
    L_0x00aa:
        monitor-exit(r1);	 Catch:{ all -> 0x00ac }
        throw r0;
    L_0x00ac:
        r0 = move-exception;
        goto L_0x00aa;
    L_0x00ae:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.permission.PermissionManagerService.grantRequestedRuntimePermissionsForUser(android.content.pm.PackageParser$Package, int, java.lang.String[], int, com.android.server.pm.permission.PermissionManagerInternal$PermissionCallback):void");
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
    private void grantRuntimePermission(java.lang.String r21, java.lang.String r22, boolean r23, int r24, int r25, com.android.server.pm.permission.PermissionManagerInternal.PermissionCallback r26) {
        /*
        r20 = this;
        r8 = r20;
        r9 = r21;
        r10 = r22;
        r11 = r25;
        r12 = r26;
        r0 = r8.mUserManagerInt;
        r0 = r0.exists(r11);
        if (r0 != 0) goto L_0x0029;
    L_0x0012:
        r0 = "PackageManager";
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "No such user:";
        r1.append(r2);
        r1.append(r11);
        r1 = r1.toString();
        android.util.Log.e(r0, r1);
        return;
    L_0x0029:
        r0 = r8.mContext;
        r1 = "android.permission.GRANT_RUNTIME_PERMISSIONS";
        r2 = "grantRuntimePermission";
        r0.enforceCallingOrSelfPermission(r1, r2);
        r4 = 1;
        r5 = 1;
        r6 = 0;
        r7 = "grantRuntimePermission";
        r1 = r8;
        r2 = r24;
        r3 = r11;
        r1.enforceCrossUserPermission(r2, r3, r4, r5, r6, r7);
        r0 = r8.mPackageManagerInt;
        r1 = r0.getPackage(r10);
        if (r1 == 0) goto L_0x01dd;
    L_0x0046:
        r0 = r1.mExtras;
        if (r0 == 0) goto L_0x01dd;
    L_0x004a:
        r2 = r8.mLock;
        monitor-enter(r2);
        r0 = r8.mSettings;	 Catch:{ all -> 0x01d4 }
        r0 = r0.getPermissionLocked(r9);	 Catch:{ all -> 0x01d4 }
        r3 = r0;	 Catch:{ all -> 0x01d4 }
        monitor-exit(r2);	 Catch:{ all -> 0x01d4 }
        if (r3 == 0) goto L_0x01b9;
    L_0x0057:
        r0 = r8.mPackageManagerInt;
        r4 = r24;
        r0 = r0.filterAppAccess(r1, r4, r11);
        if (r0 != 0) goto L_0x01a0;
    L_0x0061:
        r3.enforceDeclaredUsedAndRuntimeOrDevelopment(r1);
        r0 = r8.mSettings;
        r0 = r0.mPermissionReviewRequired;
        r2 = 23;
        if (r0 == 0) goto L_0x0079;
    L_0x006c:
        r0 = r1.applicationInfo;
        r0 = r0.targetSdkVersion;
        if (r0 >= r2) goto L_0x0079;
    L_0x0072:
        r0 = r3.isRuntime();
        if (r0 == 0) goto L_0x0079;
    L_0x0078:
        return;
    L_0x0079:
        r0 = r1.applicationInfo;
        r0 = r0.uid;
        r5 = android.os.UserHandle.getUid(r11, r0);
        r0 = r1.mExtras;
        r6 = r0;
        r6 = (com.android.server.pm.PackageSetting) r6;
        r7 = r6.getPermissionsState();
        r13 = r7.getPermissionFlags(r9, r11);
        r0 = r13 & 16;
        if (r0 != 0) goto L_0x017f;
    L_0x0092:
        if (r23 != 0) goto L_0x00b8;
    L_0x0094:
        r0 = r13 & 4;
        if (r0 != 0) goto L_0x0099;
    L_0x0098:
        goto L_0x00b8;
    L_0x0099:
        r0 = new java.lang.SecurityException;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r14 = "Cannot grant policy fixed permission ";
        r2.append(r14);
        r2.append(r9);
        r14 = " for package ";
        r2.append(r14);
        r2.append(r10);
        r2 = r2.toString();
        r0.<init>(r2);
        throw r0;
    L_0x00b8:
        r0 = r3.isDevelopment();
        r14 = -1;
        if (r0 == 0) goto L_0x00cb;
    L_0x00bf:
        r0 = r7.grantInstallPermission(r3);
        if (r0 == r14) goto L_0x00ca;
    L_0x00c5:
        if (r12 == 0) goto L_0x00ca;
    L_0x00c7:
        r26.onInstallPermissionGranted();
    L_0x00ca:
        return;
    L_0x00cb:
        r0 = r6.getInstantApp(r11);
        if (r0 == 0) goto L_0x00f7;
    L_0x00d1:
        r0 = r3.isInstant();
        if (r0 == 0) goto L_0x00d8;
    L_0x00d7:
        goto L_0x00f7;
    L_0x00d8:
        r0 = new java.lang.SecurityException;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r14 = "Cannot grant non-ephemeral permission";
        r2.append(r14);
        r2.append(r9);
        r14 = " for package ";
        r2.append(r14);
        r2.append(r10);
        r2 = r2.toString();
        r0.<init>(r2);
        throw r0;
    L_0x00f7:
        r0 = r1.applicationInfo;
        r0 = r0.targetSdkVersion;
        if (r0 >= r2) goto L_0x0105;
    L_0x00fd:
        r0 = "PackageManager";
        r2 = "Cannot grant runtime permission to a legacy app";
        android.util.Slog.w(r0, r2);
        return;
    L_0x0105:
        r2 = r7.grantRuntimePermission(r3, r11);
        if (r2 == r14) goto L_0x017a;
    L_0x010b:
        r0 = 1;
        if (r2 == r0) goto L_0x010f;
    L_0x010e:
        goto L_0x011c;
    L_0x010f:
        if (r12 == 0) goto L_0x011c;
    L_0x0111:
        r0 = r1.applicationInfo;
        r0 = r0.uid;
        r0 = android.os.UserHandle.getAppId(r0);
        r12.onGidsChanged(r0, r11);
    L_0x011c:
        r0 = r3.isRuntime();
        if (r0 == 0) goto L_0x0127;
    L_0x0122:
        r0 = 1243; // 0x4db float:1.742E-42 double:6.14E-321;
        r8.logPermission(r0, r9, r10);
    L_0x0127:
        if (r12 == 0) goto L_0x012c;
    L_0x0129:
        r12.onPermissionGranted(r5, r11);
    L_0x012c:
        r0 = "android.permission.READ_EXTERNAL_STORAGE";
        r0 = r0.equals(r9);
        if (r0 != 0) goto L_0x0142;
    L_0x0134:
        r0 = "android.permission.WRITE_EXTERNAL_STORAGE";
        r0 = r0.equals(r9);
        if (r0 == 0) goto L_0x013d;
    L_0x013c:
        goto L_0x0142;
    L_0x013d:
        r18 = r1;
        r19 = r2;
        goto L_0x016e;
    L_0x0142:
        r14 = android.os.Binder.clearCallingIdentity();
        r16 = r14;
        r0 = r8.mUserManagerInt;	 Catch:{ all -> 0x016f }
        r0 = r0.isUserInitialized(r11);	 Catch:{ all -> 0x016f }
        if (r0 == 0) goto L_0x0164;
    L_0x0150:
        r0 = android.os.storage.StorageManagerInternal.class;	 Catch:{ all -> 0x015c }
        r0 = com.android.server.LocalServices.getService(r0);	 Catch:{ all -> 0x015c }
        r0 = (android.os.storage.StorageManagerInternal) r0;	 Catch:{ all -> 0x015c }
        r0.onExternalStoragePolicyChanged(r5, r10);	 Catch:{ all -> 0x015c }
        goto L_0x0164;
    L_0x015c:
        r0 = move-exception;
        r18 = r1;
        r19 = r2;
        r1 = r16;
        goto L_0x0176;
    L_0x0164:
        r18 = r1;
        r19 = r2;
        r1 = r16;
        android.os.Binder.restoreCallingIdentity(r1);
    L_0x016e:
        return;
    L_0x016f:
        r0 = move-exception;
        r18 = r1;
        r19 = r2;
        r1 = r16;
    L_0x0176:
        android.os.Binder.restoreCallingIdentity(r1);
        throw r0;
    L_0x017a:
        r18 = r1;
        r19 = r2;
        return;
    L_0x017f:
        r18 = r1;
        r0 = new java.lang.SecurityException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Cannot grant system fixed permission ";
        r1.append(r2);
        r1.append(r9);
        r2 = " for package ";
        r1.append(r2);
        r1.append(r10);
        r1 = r1.toString();
        r0.<init>(r1);
        throw r0;
    L_0x01a0:
        r18 = r1;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Unknown package: ";
        r1.append(r2);
        r1.append(r10);
        r1 = r1.toString();
        r0.<init>(r1);
        throw r0;
    L_0x01b9:
        r4 = r24;
        r18 = r1;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Unknown permission: ";
        r1.append(r2);
        r1.append(r9);
        r1 = r1.toString();
        r0.<init>(r1);
        throw r0;
    L_0x01d4:
        r0 = move-exception;
        r4 = r24;
        r18 = r1;
    L_0x01d9:
        monitor-exit(r2);	 Catch:{ all -> 0x01db }
        throw r0;
    L_0x01db:
        r0 = move-exception;
        goto L_0x01d9;
    L_0x01dd:
        r4 = r24;
        r18 = r1;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Unknown package: ";
        r1.append(r2);
        r1.append(r10);
        r1 = r1.toString();
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.permission.PermissionManagerService.grantRuntimePermission(java.lang.String, java.lang.String, boolean, int, int, com.android.server.pm.permission.PermissionManagerInternal$PermissionCallback):void");
    }

    PermissionManagerService(Context context, DefaultPermissionGrantedCallback defaultGrantCallback, Object externalLock) {
        int i = 0;
        this.mContext = context;
        this.mLock = externalLock;
        this.mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mUserManagerInt = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        this.mSettings = new PermissionSettings(context, this.mLock);
        this.mHandlerThread = new ServiceThread(TAG, 10, true);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        Watchdog.getInstance().addThread(this.mHandler);
        this.mDefaultPermissionGrantPolicy = HwServiceFactory.getHwDefaultPermissionGrantPolicy(context, this.mHandlerThread.getLooper(), defaultGrantCallback, this);
        SystemConfig systemConfig = SystemConfig.getInstance();
        this.mSystemPermissions = systemConfig.getSystemPermissions();
        this.mGlobalGids = systemConfig.getGlobalGids();
        ArrayMap<String, PermissionEntry> permConfig = SystemConfig.getInstance().getPermissions();
        synchronized (this.mLock) {
            while (i < permConfig.size()) {
                PermissionEntry perm = (PermissionEntry) permConfig.valueAt(i);
                BasePermission bp = this.mSettings.getPermissionLocked(perm.name);
                if (bp == null) {
                    bp = new BasePermission(perm.name, PackageManagerService.PLATFORM_PACKAGE_NAME, 1);
                    this.mSettings.putPermissionLocked(perm.name, bp);
                }
                if (perm.gids != null) {
                    bp.setGids(perm.gids, perm.perUser);
                }
                i++;
            }
        }
        LocalServices.addService(PermissionManagerInternal.class, new PermissionManagerInternalImpl());
    }

    public static PermissionManagerInternal create(Context context, DefaultPermissionGrantedCallback defaultGrantCallback, Object externalLock) {
        PermissionManagerInternal permMgrInt = (PermissionManagerInternal) LocalServices.getService(PermissionManagerInternal.class);
        if (permMgrInt != null) {
            return permMgrInt;
        }
        PermissionManagerService permissionManagerService = new PermissionManagerService(context, defaultGrantCallback, externalLock);
        return (PermissionManagerInternal) LocalServices.getService(PermissionManagerInternal.class);
    }

    BasePermission getPermission(String permName) {
        BasePermission permissionLocked;
        synchronized (this.mLock) {
            permissionLocked = this.mSettings.getPermissionLocked(permName);
        }
        return permissionLocked;
    }

    private int checkPermission(String permName, String pkgName, int callingUid, int userId) {
        if (!this.mUserManagerInt.exists(userId)) {
            return -1;
        }
        Package pkg = this.mPackageManagerInt.getPackage(pkgName);
        if (pkg == null || pkg.mExtras == null || this.mPackageManagerInt.filterAppAccess(pkg, callingUid, userId)) {
            return -1;
        }
        PackageSetting ps = pkg.mExtras;
        boolean instantApp = ps.getInstantApp(userId);
        PermissionsState permissionsState = ps.getPermissionsState();
        if (permissionsState.hasPermission(permName, userId)) {
            if (!instantApp) {
                return 0;
            }
            synchronized (this.mLock) {
                BasePermission bp = this.mSettings.getPermissionLocked(permName);
                if (bp == null || !bp.isInstant()) {
                } else {
                    return 0;
                }
            }
        }
        if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName) && permissionsState.hasPermission("android.permission.ACCESS_FINE_LOCATION", userId)) {
            return 0;
        }
        return -1;
    }

    /* JADX WARNING: Removed duplicated region for block: B:40:0x0089 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int checkUidPermission(String permName, Package pkg, int uid, int callingUid) {
        int callingUserId = UserHandle.getUserId(callingUid);
        boolean isUidInstantApp = true;
        boolean isCallerInstantApp = this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null;
        if (this.mPackageManagerInt.getInstantAppPackageName(uid) == null) {
            isUidInstantApp = false;
        }
        int userId = UserHandle.getUserId(uid);
        if (!this.mUserManagerInt.exists(userId)) {
            return -1;
        }
        if (pkg != null) {
            if (pkg.mSharedUserId != null) {
                if (isCallerInstantApp) {
                    return -1;
                }
            } else if (this.mPackageManagerInt.filterAppAccess(pkg, callingUid, callingUserId)) {
                return -1;
            }
            PermissionsState permissionsState = ((PackageSetting) pkg.mExtras).getPermissionsState();
            if (permissionsState.hasPermission(permName, userId) && (!isUidInstantApp || this.mSettings.isPermissionInstant(permName))) {
                return 0;
            }
            if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName) && permissionsState.hasPermission("android.permission.ACCESS_FINE_LOCATION", userId)) {
                return 0;
            }
            return -1;
        }
        ArraySet<String> perms = (ArraySet) this.mSystemPermissions.get(uid);
        if (perms != null) {
            if (perms.contains(permName)) {
                return 0;
            }
            if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName) && perms.contains("android.permission.ACCESS_FINE_LOCATION")) {
                return 0;
            }
        }
        return -1;
    }

    private PermissionGroupInfo getPermissionGroupInfo(String groupName, int flags, int callingUid) {
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        PermissionGroupInfo generatePermissionGroupInfo;
        synchronized (this.mLock) {
            generatePermissionGroupInfo = PackageParser.generatePermissionGroupInfo((PermissionGroup) this.mSettings.mPermissionGroups.get(groupName), flags);
        }
        return generatePermissionGroupInfo;
    }

    private List<PermissionGroupInfo> getAllPermissionGroups(int flags, int callingUid) {
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        ArrayList<PermissionGroupInfo> out;
        synchronized (this.mLock) {
            out = new ArrayList(this.mSettings.mPermissionGroups.size());
            for (PermissionGroup pg : this.mSettings.mPermissionGroups.values()) {
                out.add(PackageParser.generatePermissionGroupInfo(pg, flags));
            }
        }
        return out;
    }

    private PermissionInfo getPermissionInfo(String permName, String packageName, int flags, int callingUid) {
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        synchronized (this.mLock) {
            BasePermission bp = this.mSettings.getPermissionLocked(permName);
            if (bp == null) {
                return null;
            }
            PermissionInfo generatePermissionInfo = bp.generatePermissionInfo(adjustPermissionProtectionFlagsLocked(bp.getProtectionLevel(), packageName, callingUid), flags);
            return generatePermissionInfo;
        }
    }

    private List<PermissionInfo> getPermissionInfoByGroup(String groupName, int flags, int callingUid) {
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        synchronized (this.mLock) {
            if (groupName != null) {
                if (!this.mSettings.mPermissionGroups.containsKey(groupName)) {
                    return null;
                }
            }
            ArrayList<PermissionInfo> out = new ArrayList(10);
            for (BasePermission bp : this.mSettings.mPermissions.values()) {
                PermissionInfo pi = bp.generatePermissionInfo(groupName, flags);
                if (pi != null) {
                    out.add(pi);
                }
            }
            return out;
        }
    }

    private int adjustPermissionProtectionFlagsLocked(int protectionLevel, String packageName, int uid) {
        int protectionLevelMasked = protectionLevel & 3;
        if (protectionLevelMasked == 2) {
            return protectionLevel;
        }
        int appId = UserHandle.getAppId(uid);
        if (appId == 1000 || appId == 0 || appId == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            return protectionLevel;
        }
        Package pkg = this.mPackageManagerInt.getPackage(packageName);
        if (pkg == null) {
            return protectionLevel;
        }
        if (pkg.applicationInfo.targetSdkVersion < 26) {
            return protectionLevelMasked;
        }
        PackageSetting ps = pkg.mExtras;
        if (ps == null || ps.getAppId() == appId) {
            return protectionLevel;
        }
        return protectionLevel;
    }

    private void revokeRuntimePermissionsIfGroupChanged(Package newPackage, Package oldPackage, ArrayList<String> allPackageNames, PermissionCallback permissionCallback) {
        int i;
        String permissionName;
        Package packageR = newPackage;
        Package packageR2 = oldPackage;
        int numOldPackagePermissions = packageR2.permissions.size();
        ArrayMap<String, String> oldPermissionNameToGroupName = new ArrayMap(numOldPackagePermissions);
        for (i = 0; i < numOldPackagePermissions; i++) {
            Permission permission = (Permission) packageR2.permissions.get(i);
            if (permission.group != null) {
                oldPermissionNameToGroupName.put(permission.info.name, permission.group.info.name);
            }
        }
        int numNewPackagePermissions = packageR.permissions.size();
        i = 0;
        while (true) {
            int newPermissionNum = i;
            ArrayMap<String, String> oldPermissionNameToGroupName2;
            if (newPermissionNum < numNewPackagePermissions) {
                Permission newPermission = (Permission) packageR.permissions.get(newPermissionNum);
                if ((newPermission.info.getProtection() & 1) != 0) {
                    String permissionName2 = newPermission.info.name;
                    String newPermissionGroupName = newPermission.group == null ? null : newPermission.group.info.name;
                    String oldPermissionGroupName = (String) oldPermissionNameToGroupName.get(permissionName2);
                    if (newPermissionGroupName != null && !newPermissionGroupName.equals(oldPermissionGroupName)) {
                        int[] userIds = this.mUserManagerInt.getUserIds();
                        int numUserIds = userIds.length;
                        i = 0;
                        while (true) {
                            int userIdNum = i;
                            if (userIdNum >= numUserIds) {
                                break;
                            }
                            int numOldPackagePermissions2;
                            int numUserIds2;
                            int[] userIds2;
                            String oldPermissionGroupName2;
                            String newPermissionGroupName2;
                            Permission newPermission2;
                            int userId = userIds[userIdNum];
                            int numPackages = allPackageNames.size();
                            i = 0;
                            while (true) {
                                numOldPackagePermissions2 = numOldPackagePermissions;
                                numOldPackagePermissions = i;
                                if (numOldPackagePermissions >= numPackages) {
                                    break;
                                }
                                int userIdNum2;
                                int numPackages2 = numPackages;
                                String packageName = (String) allPackageNames.get(numOldPackagePermissions);
                                oldPermissionNameToGroupName2 = oldPermissionNameToGroupName;
                                if (checkPermission(permissionName2, packageName, 0, userId) == 0) {
                                    Object[] objArr = new Object[3];
                                    objArr[0] = "72710897";
                                    objArr[1] = Integer.valueOf(packageR.applicationInfo.uid);
                                    StringBuilder stringBuilder = new StringBuilder();
                                    int userIdNum3 = userIdNum;
                                    stringBuilder.append("Revoking permission ");
                                    stringBuilder.append(permissionName2);
                                    stringBuilder.append(" from package ");
                                    stringBuilder.append(packageName);
                                    stringBuilder.append(" as the group changed from ");
                                    stringBuilder.append(oldPermissionGroupName);
                                    stringBuilder.append(" to ");
                                    stringBuilder.append(newPermissionGroupName);
                                    objArr[2] = stringBuilder.toString();
                                    EventLog.writeEvent(1397638484, objArr);
                                    userIdNum2 = userIdNum3;
                                    numUserIds2 = numUserIds;
                                    userIds2 = userIds;
                                    oldPermissionGroupName2 = oldPermissionGroupName;
                                    newPermissionGroupName2 = newPermissionGroupName;
                                    permissionName = permissionName2;
                                    newPermission2 = newPermission;
                                    try {
                                        revokeRuntimePermission(permissionName2, packageName, false, 1000, userId, permissionCallback);
                                    } catch (IllegalArgumentException e) {
                                        IllegalArgumentException illegalArgumentException = e;
                                        String str = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Could not revoke ");
                                        stringBuilder2.append(permissionName);
                                        stringBuilder2.append(" from ");
                                        stringBuilder2.append(packageName);
                                        Slog.e(str, stringBuilder2.toString(), e);
                                    }
                                } else {
                                    userIdNum2 = userIdNum;
                                    numUserIds2 = numUserIds;
                                    userIds2 = userIds;
                                    oldPermissionGroupName2 = oldPermissionGroupName;
                                    newPermissionGroupName2 = newPermissionGroupName;
                                    permissionName = permissionName2;
                                    newPermission2 = newPermission;
                                }
                                i = numOldPackagePermissions + 1;
                                permissionName2 = permissionName;
                                userIdNum = userIdNum2;
                                numUserIds = numUserIds2;
                                numOldPackagePermissions = numOldPackagePermissions2;
                                numPackages = numPackages2;
                                oldPermissionNameToGroupName = oldPermissionNameToGroupName2;
                                userIds = userIds2;
                                oldPermissionGroupName = oldPermissionGroupName2;
                                newPermissionGroupName = newPermissionGroupName2;
                                newPermission = newPermission2;
                            }
                            numUserIds2 = numUserIds;
                            userIds2 = userIds;
                            oldPermissionGroupName2 = oldPermissionGroupName;
                            newPermissionGroupName2 = newPermissionGroupName;
                            newPermission2 = newPermission;
                            oldPermissionNameToGroupName2 = oldPermissionNameToGroupName;
                            i = userIdNum + 1;
                            numOldPackagePermissions = numOldPackagePermissions2;
                            oldPermissionNameToGroupName = oldPermissionNameToGroupName2;
                            packageR2 = oldPackage;
                        }
                    }
                }
                i = newPermissionNum + 1;
                numOldPackagePermissions = numOldPackagePermissions;
                oldPermissionNameToGroupName = oldPermissionNameToGroupName;
                packageR2 = oldPackage;
            } else {
                oldPermissionNameToGroupName2 = oldPermissionNameToGroupName;
                return;
            }
        }
    }

    private void addAllPermissions(Package pkg, boolean chatty) {
        int N = pkg.permissions.size();
        for (int i = 0; i < N; i++) {
            Permission p = (Permission) pkg.permissions.get(i);
            PermissionInfo permissionInfo = p.info;
            permissionInfo.flags &= -1073741825;
            synchronized (this.mLock) {
                if (pkg.applicationInfo.targetSdkVersion > 22) {
                    p.group = (PermissionGroup) this.mSettings.mPermissionGroups.get(p.info.group);
                    if (PackageManagerService.DEBUG_PERMISSIONS && p.info.group != null && p.group == null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Permission ");
                        stringBuilder.append(p.info.name);
                        stringBuilder.append(" from package ");
                        stringBuilder.append(p.info.packageName);
                        stringBuilder.append(" in an unknown group ");
                        stringBuilder.append(p.info.group);
                        Slog.i(str, stringBuilder.toString());
                    }
                }
                if (p.tree) {
                    this.mSettings.putPermissionTreeLocked(p.info.name, BasePermission.createOrUpdate(this.mSettings.getPermissionTreeLocked(p.info.name), p, pkg, this.mSettings.getAllPermissionTreesLocked(), chatty));
                } else {
                    this.mSettings.putPermissionLocked(p.info.name, BasePermission.createOrUpdate(this.mSettings.getPermissionLocked(p.info.name), p, pkg, this.mSettings.getAllPermissionTreesLocked(), chatty));
                }
            }
        }
    }

    private void addAllPermissionGroups(Package pkg, boolean chatty) {
        int N = pkg.permissionGroups.size();
        StringBuilder r = null;
        for (int i = 0; i < N; i++) {
            PermissionGroup pg = (PermissionGroup) pkg.permissionGroups.get(i);
            PermissionGroup cur = (PermissionGroup) this.mSettings.mPermissionGroups.get(pg.info.name);
            boolean isPackageUpdate = pg.info.packageName.equals(cur == null ? null : cur.info.packageName);
            if (cur == null || isPackageUpdate) {
                this.mSettings.mPermissionGroups.put(pg.info.name, pg);
                if (chatty && PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                    if (r == null) {
                        r = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    if (isPackageUpdate) {
                        r.append("UPD:");
                    }
                    r.append(pg.info.name);
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Permission group ");
                stringBuilder.append(pg.info.name);
                stringBuilder.append(" from package ");
                stringBuilder.append(pg.info.packageName);
                stringBuilder.append(" ignored: original from ");
                stringBuilder.append(cur.info.packageName);
                Slog.w(str, stringBuilder.toString());
                if (chatty && PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                    if (r == null) {
                        r = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    r.append("DUP:");
                    r.append(pg.info.name);
                }
            }
        }
        if (r != null && PackageManagerService.DEBUG_PACKAGE_SCANNING) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  Permission Groups: ");
            stringBuilder2.append(r);
            Log.d(str2, stringBuilder2.toString());
        }
    }

    private void removeAllPermissions(Package pkg, boolean chatty) {
        synchronized (this.mLock) {
            int N = pkg.permissions.size();
            int i = 0;
            StringBuilder r = null;
            for (int i2 = 0; i2 < N; i2++) {
                Permission p = (Permission) pkg.permissions.get(i2);
                BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(p.info.name);
                if (bp == null) {
                    bp = (BasePermission) this.mSettings.mPermissionTrees.get(p.info.name);
                }
                if (bp != null && bp.isPermission(p)) {
                    bp.setPermission(null);
                    if (PackageManagerService.DEBUG_REMOVE && chatty) {
                        if (r == null) {
                            r = new StringBuilder(256);
                        } else {
                            r.append(' ');
                        }
                        r.append(p.info.name);
                    }
                }
                if (p.isAppOp()) {
                    ArraySet<String> appOpPkgs = (ArraySet) this.mSettings.mAppOpPermissionPackages.get(p.info.name);
                    if (appOpPkgs != null) {
                        appOpPkgs.remove(pkg.packageName);
                    }
                }
            }
            if (r != null && PackageManagerService.DEBUG_REMOVE) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  Permissions: ");
                stringBuilder.append(r);
                Log.d(str, stringBuilder.toString());
            }
            N = pkg.requestedPermissions.size();
            while (i < N) {
                String perm = (String) pkg.requestedPermissions.get(i);
                if (this.mSettings.isPermissionAppOp(perm)) {
                    ArraySet<String> appOpPkgs2 = (ArraySet) this.mSettings.mAppOpPermissionPackages.get(perm);
                    if (appOpPkgs2 != null) {
                        appOpPkgs2.remove(pkg.packageName);
                        if (appOpPkgs2.isEmpty()) {
                            this.mSettings.mAppOpPermissionPackages.remove(perm);
                        }
                    }
                }
                i++;
            }
            if (null != null && PackageManagerService.DEBUG_REMOVE) {
                String str2 = TAG;
                r = new StringBuilder();
                r.append("  Permissions: ");
                r.append(null);
                Log.d(str2, r.toString());
            }
        }
    }

    private boolean addDynamicPermission(PermissionInfo info, int callingUid, PermissionCallback callback) {
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            throw new SecurityException("Instant apps can't add permissions");
        } else if (info != null && info.labelRes == 0 && info.nonLocalizedLabel == null) {
            throw new SecurityException("Label must be specified in permission");
        } else {
            boolean added;
            boolean changed;
            BasePermission tree = this.mSettings.enforcePermissionTree(info.name, callingUid);
            synchronized (this.mLock) {
                BasePermission bp = this.mSettings.getPermissionLocked(info.name);
                added = bp == null;
                int fixedLevel = PermissionInfo.fixProtectionLevel(info.protectionLevel);
                if (added) {
                    enforcePermissionCapLocked(info, tree);
                    bp = new BasePermission(info.name, tree.getSourcePackageName(), 2);
                } else if (!bp.isDynamic()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Not allowed to modify non-dynamic permission ");
                    stringBuilder.append(info.name);
                    throw new SecurityException(stringBuilder.toString());
                }
                changed = bp.addToTree(fixedLevel, info, tree);
                if (added) {
                    this.mSettings.putPermissionLocked(info.name, bp);
                }
            }
            if (changed && callback != null) {
                callback.onPermissionChanged();
            }
            return added;
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0042, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void removeDynamicPermission(String permName, int callingUid, PermissionCallback callback) {
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) == null) {
            BasePermission tree = this.mSettings.enforcePermissionTree(permName, callingUid);
            synchronized (this.mLock) {
                BasePermission bp = this.mSettings.getPermissionLocked(permName);
                if (bp == null) {
                    return;
                }
                if (bp.isDynamic()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Not allowed to modify non-dynamic permission ");
                    stringBuilder.append(permName);
                    Slog.wtf(str, stringBuilder.toString());
                }
                this.mSettings.removePermissionLocked(permName);
                if (callback != null) {
                    callback.onPermissionRemoved();
                }
            }
        } else {
            throw new SecurityException("Instant applications don't have access to this method");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:122:0x0210  */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x01e9 A:{SYNTHETIC, Splitter: B:118:0x01e9} */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x03fd A:{Catch:{ all -> 0x04b3, all -> 0x04e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x0216 A:{SYNTHETIC, Splitter: B:125:0x0216} */
    /* JADX WARNING: Missing block: B:229:0x0387, code:
            r15 = r12;
            r13 = r20;
     */
    /* JADX WARNING: Missing block: B:249:0x03d0, code:
            if (r4.equals(r2.packageName) != false) goto L_0x03d2;
     */
    /* JADX WARNING: Missing block: B:306:0x0577, code:
            if (r3.isSystem() != false) goto L_0x0584;
     */
    /* JADX WARNING: Missing block: B:312:0x0588, code:
            if (r3.isUpdatedSystem() != false) goto L_0x058a;
     */
    /* JADX WARNING: Missing block: B:315:?, code:
            r3.setInstallPermissionsFixed(true);
     */
    /* JADX WARNING: Missing block: B:318:0x058f, code:
            r5 = r41;
     */
    /* JADX WARNING: Missing block: B:319:0x0591, code:
            if (r5 == null) goto L_0x059b;
     */
    /* JADX WARNING: Missing block: B:320:0x0593, code:
            r5.onPermissionUpdated(r19, r16);
     */
    /* JADX WARNING: Missing block: B:321:0x059b, code:
            r11 = r16;
            r12 = r19;
     */
    /* JADX WARNING: Missing block: B:322:0x059f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void grantPermissions(Package pkg, boolean replace, String packageOfInterest, PermissionCallback callback) {
        int[] updatedUserIds;
        Throwable N;
        boolean changedInstallPermission;
        PermissionManagerService permissionManagerService = this;
        Package packageR = pkg;
        String str = packageOfInterest;
        PermissionCallback permissionCallback = callback;
        PackageSetting ps = packageR.mExtras;
        if (ps != null) {
            boolean isLegacySystemApp = permissionManagerService.mPackageManagerInt.isLegacySystemApp(packageR);
            PermissionsState permissionsState = ps.getPermissionsState();
            PermissionsState origPermissions = permissionsState;
            int[] currentUserIds = UserManagerService.getInstance().getUserIds();
            boolean runtimePermissionsRevoked = false;
            int[] updatedUserIds2 = EMPTY_INT_ARRAY;
            boolean changedInstallPermission2 = false;
            if (replace) {
                ps.setInstallPermissionsFixed(false);
                if (ps.isSharedUser()) {
                    synchronized (permissionManagerService.mLock) {
                        updatedUserIds2 = permissionManagerService.revokeUnusedSharedUserPermissionsLocked(ps.getSharedUser(), UserManagerService.getInstance().getUserIds());
                        if (!ArrayUtils.isEmpty(updatedUserIds2)) {
                            runtimePermissionsRevoked = true;
                        }
                    }
                } else {
                    origPermissions = new PermissionsState(permissionsState);
                    permissionsState.reset();
                }
            }
            permissionsState.setGlobalGids(permissionManagerService.mGlobalGids);
            synchronized (permissionManagerService.mLock) {
                PackageSetting packageSetting;
                boolean z;
                int[] iArr;
                try {
                    int N2 = packageR.requestedPermissions.size();
                    int[] updatedUserIds3 = updatedUserIds2;
                    int i = 0;
                    while (true) {
                        boolean runtimePermissionsRevoked2 = runtimePermissionsRevoked;
                        int N3;
                        boolean changedInstallPermission3;
                        PackageSetting ps2;
                        if (i < N2) {
                            try {
                                String permName = (String) packageR.requestedPermissions.get(i);
                                N3 = N2;
                                N2 = permissionManagerService.mSettings.getPermissionLocked(permName);
                                updatedUserIds = updatedUserIds3;
                                try {
                                    boolean appSupportsRuntimePermissions = packageR.applicationInfo.targetSdkVersion >= 23;
                                    try {
                                        int i2;
                                        String str2;
                                        StringBuilder stringBuilder;
                                        if (PackageManagerService.DEBUG_INSTALL) {
                                            try {
                                                String str3 = TAG;
                                                StringBuilder stringBuilder2 = new StringBuilder();
                                                changedInstallPermission3 = changedInstallPermission2;
                                                try {
                                                    stringBuilder2.append("Package ");
                                                    stringBuilder2.append(packageR.packageName);
                                                    stringBuilder2.append(" checking ");
                                                    stringBuilder2.append(permName);
                                                    stringBuilder2.append(": ");
                                                    stringBuilder2.append(N2);
                                                    Log.i(str3, stringBuilder2.toString());
                                                } catch (Throwable th) {
                                                    N = th;
                                                }
                                            } catch (Throwable th2) {
                                                N = th2;
                                                changedInstallPermission3 = changedInstallPermission2;
                                                packageSetting = ps;
                                                z = isLegacySystemApp;
                                                iArr = currentUserIds;
                                                permName = runtimePermissionsRevoked2;
                                                i = updatedUserIds;
                                                permissionCallback = callback;
                                            }
                                        } else {
                                            changedInstallPermission3 = changedInstallPermission2;
                                        }
                                        if (N2 == 0) {
                                            ps2 = ps;
                                            z = isLegacySystemApp;
                                            iArr = currentUserIds;
                                            i2 = i;
                                        } else if (N2.getSourcePackageSetting() == null) {
                                            ps2 = ps;
                                            z = isLegacySystemApp;
                                            iArr = currentUserIds;
                                            i2 = i;
                                        } else {
                                            StringBuilder stringBuilder3;
                                            if (packageR.applicationInfo.isInstantApp()) {
                                                if (!N2.isInstant()) {
                                                    if (PackageManagerService.DEBUG_PERMISSIONS) {
                                                        str2 = TAG;
                                                        stringBuilder3 = new StringBuilder();
                                                        stringBuilder3.append("Denying non-ephemeral permission ");
                                                        stringBuilder3.append(N2.getName());
                                                        stringBuilder3.append(" for package ");
                                                        stringBuilder3.append(packageR.packageName);
                                                        Log.i(str2, stringBuilder3.toString());
                                                    }
                                                    ps2 = ps;
                                                    z = isLegacySystemApp;
                                                    iArr = currentUserIds;
                                                    i2 = i;
                                                    updatedUserIds3 = updatedUserIds;
                                                    changedInstallPermission2 = changedInstallPermission3;
                                                    i = i2 + 1;
                                                    runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                    N2 = N3;
                                                    isLegacySystemApp = z;
                                                    currentUserIds = iArr;
                                                    ps = ps2;
                                                    permissionManagerService = this;
                                                    permissionCallback = callback;
                                                }
                                            }
                                            if (!N2.isRuntimeOnly() || appSupportsRuntimePermissions) {
                                                boolean allowedSig;
                                                int grant;
                                                int grant2;
                                                str2 = N2.getName();
                                                if (N2.isAppOp()) {
                                                    allowedSig = false;
                                                    grant = 1;
                                                    permissionManagerService.mSettings.addAppOpPackage(str2, packageR.packageName);
                                                } else {
                                                    allowedSig = false;
                                                    grant = 1;
                                                }
                                                try {
                                                    if (N2.isNormal()) {
                                                        grant2 = 2;
                                                    } else if (N2.isRuntime()) {
                                                        if (!appSupportsRuntimePermissions) {
                                                            if (!permissionManagerService.mSettings.mPermissionReviewRequired) {
                                                                grant2 = 2;
                                                            }
                                                        }
                                                        if (origPermissions.hasInstallPermission(N2.getName())) {
                                                            grant2 = 4;
                                                        } else if (isLegacySystemApp) {
                                                            grant2 = 4;
                                                        } else {
                                                            grant2 = 3;
                                                        }
                                                    } else {
                                                        if (N2.isSignature()) {
                                                            try {
                                                                changedInstallPermission2 = permissionManagerService.grantSignaturePermission(str2, packageR, N2, origPermissions);
                                                                if (permissionManagerService.mCustPms != null) {
                                                                    z = isLegacySystemApp;
                                                                    try {
                                                                        if (permissionManagerService.mCustPms.isHwFiltReqInstallPerm(packageR.packageName, str2)) {
                                                                            changedInstallPermission2 = false;
                                                                        }
                                                                    } catch (Throwable th3) {
                                                                        N = th3;
                                                                        packageSetting = ps;
                                                                    }
                                                                } else {
                                                                    z = isLegacySystemApp;
                                                                }
                                                                if (changedInstallPermission2) {
                                                                    grant2 = 2;
                                                                    allowedSig = changedInstallPermission2;
                                                                } else {
                                                                    allowedSig = changedInstallPermission2;
                                                                    grant2 = grant;
                                                                }
                                                            } catch (Throwable th4) {
                                                                N = th4;
                                                                z = isLegacySystemApp;
                                                                packageSetting = ps;
                                                                iArr = currentUserIds;
                                                                runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                                updatedUserIds2 = updatedUserIds;
                                                                changedInstallPermission2 = changedInstallPermission3;
                                                                permissionCallback = callback;
                                                                while (true) {
                                                                    try {
                                                                        break;
                                                                    } catch (Throwable th5) {
                                                                        N = th5;
                                                                    }
                                                                }
                                                                throw N;
                                                            }
                                                        }
                                                        z = isLegacySystemApp;
                                                        grant2 = grant;
                                                        if (PackageManagerService.DEBUG_PERMISSIONS) {
                                                            i2 = i;
                                                        } else {
                                                            String str4 = TAG;
                                                            stringBuilder3 = new StringBuilder();
                                                            i2 = i;
                                                            stringBuilder3.append("Granting permission ");
                                                            stringBuilder3.append(str2);
                                                            stringBuilder3.append(" to package ");
                                                            stringBuilder3.append(packageR.packageName);
                                                            Slog.i(str4, stringBuilder3.toString());
                                                        }
                                                        if (grant2 == 1) {
                                                            if (!ps.isSystem()) {
                                                                if (!(!ps.areInstallPermissionsFixed() || allowedSig || origPermissions.hasInstallPermission(str2) || permissionManagerService.isNewPlatformPermissionForPackage(str2, packageR))) {
                                                                    grant2 = 1;
                                                                }
                                                            }
                                                            int i3;
                                                            int i4;
                                                            int length;
                                                            int userId;
                                                            switch (grant2) {
                                                                case 2:
                                                                    ps2 = ps;
                                                                    iArr = currentUserIds;
                                                                    i3 = grant2;
                                                                    updatedUserIds2 = updatedUserIds;
                                                                    for (int userId2 : UserManagerService.getInstance().getUserIds()) {
                                                                        if (origPermissions.getRuntimePermissionState(str2, userId2) != null) {
                                                                            origPermissions.revokeRuntimePermission(N2, userId2);
                                                                            origPermissions.updatePermissionFlags(N2, userId2, 255, 0);
                                                                            updatedUserIds2 = ArrayUtils.appendInt(updatedUserIds2, userId2);
                                                                        }
                                                                    }
                                                                    if (permissionsState.grantInstallPermission(N2) != -1) {
                                                                        changedInstallPermission2 = true;
                                                                        updatedUserIds3 = updatedUserIds2;
                                                                        break;
                                                                    }
                                                                    break;
                                                                case 3:
                                                                    ps2 = ps;
                                                                    iArr = currentUserIds;
                                                                    i3 = grant2;
                                                                    ps = UserManagerService.getInstance().getUserIds();
                                                                    length = ps.length;
                                                                    updatedUserIds2 = updatedUserIds;
                                                                    i4 = 0;
                                                                    while (i4 < length) {
                                                                        try {
                                                                            PackageSetting packageSetting2;
                                                                            int i5;
                                                                            changedInstallPermission2 = ps[i4];
                                                                            PermissionState permissionState = origPermissions.getRuntimePermissionState(str2, changedInstallPermission2);
                                                                            updatedUserIds = permissionState != null ? permissionState.getFlags() : 0;
                                                                            if (origPermissions.hasRuntimePermission(str2, changedInstallPermission2)) {
                                                                                boolean revokeOnUpgrade = (updatedUserIds & 8) != 0;
                                                                                if (revokeOnUpgrade) {
                                                                                    updatedUserIds &= -9;
                                                                                    updatedUserIds2 = ArrayUtils.appendInt(updatedUserIds2, changedInstallPermission2);
                                                                                }
                                                                                packageSetting2 = ps;
                                                                                if (permissionManagerService.mSettings.mPermissionReviewRequired == null || !revokeOnUpgrade) {
                                                                                    i5 = length;
                                                                                    if (permissionsState.grantRuntimePermission(N2, changedInstallPermission2) == -1) {
                                                                                        updatedUserIds2 = ArrayUtils.appendInt(updatedUserIds2, changedInstallPermission2);
                                                                                    }
                                                                                } else {
                                                                                    i5 = length;
                                                                                }
                                                                                if (!(permissionManagerService.mSettings.mPermissionReviewRequired == null || !appSupportsRuntimePermissions || (updatedUserIds & 64) == null)) {
                                                                                    ps = updatedUserIds & -65;
                                                                                    updatedUserIds2 = ArrayUtils.appendInt(updatedUserIds2, changedInstallPermission2);
                                                                                    updatedUserIds = ps;
                                                                                }
                                                                            } else {
                                                                                packageSetting2 = ps;
                                                                                i5 = length;
                                                                                if (!(permissionManagerService.mSettings.mPermissionReviewRequired == null || appSupportsRuntimePermissions)) {
                                                                                    if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(N2.getSourcePackageName()) != null && (updatedUserIds & 64) == null) {
                                                                                        updatedUserIds |= 64;
                                                                                        updatedUserIds2 = ArrayUtils.appendInt(updatedUserIds2, changedInstallPermission2);
                                                                                    }
                                                                                    if (permissionsState.grantRuntimePermission(N2, changedInstallPermission2) != -1) {
                                                                                        updatedUserIds2 = ArrayUtils.appendInt(updatedUserIds2, changedInstallPermission2);
                                                                                    }
                                                                                }
                                                                            }
                                                                            ps = updatedUserIds;
                                                                            permissionsState.updatePermissionFlags(N2, changedInstallPermission2, ps, ps);
                                                                            i4++;
                                                                            ps = packageSetting2;
                                                                            length = i5;
                                                                        } catch (Throwable th6) {
                                                                            N = th6;
                                                                            runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                                            permissionCallback = callback;
                                                                            while (true) {
                                                                                break;
                                                                            }
                                                                            throw N;
                                                                        }
                                                                    }
                                                                    break;
                                                                case 4:
                                                                    PermissionState permissionState2 = origPermissions.getInstallPermissionState(str2);
                                                                    int flags = permissionState2 != null ? permissionState2.getFlags() : 0;
                                                                    if (origPermissions.revokeInstallPermission(N2) != -1) {
                                                                        origPermissions.updatePermissionFlags(N2, -1, 255, 0);
                                                                        changedInstallPermission2 = true;
                                                                    } else {
                                                                        changedInstallPermission2 = changedInstallPermission3;
                                                                    }
                                                                    length = flags;
                                                                    if ((length & 8) == 0) {
                                                                        try {
                                                                            i = currentUserIds.length;
                                                                            changedInstallPermission = changedInstallPermission2;
                                                                            i3 = grant2;
                                                                            updatedUserIds3 = updatedUserIds;
                                                                            userId2 = 0;
                                                                            while (userId2 < i) {
                                                                                try {
                                                                                    iArr = currentUserIds;
                                                                                    int i6 = i;
                                                                                    i4 = currentUserIds[userId2];
                                                                                    try {
                                                                                        ps2 = ps;
                                                                                        if (permissionsState.grantRuntimePermission(N2, i4) != -1) {
                                                                                            try {
                                                                                                permissionsState.updatePermissionFlags(N2, i4, length, length);
                                                                                                updatedUserIds3 = ArrayUtils.appendInt(updatedUserIds3, i4);
                                                                                            } catch (Throwable th7) {
                                                                                                N = th7;
                                                                                                updatedUserIds2 = updatedUserIds3;
                                                                                                runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                                                                changedInstallPermission2 = changedInstallPermission;
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                        userId2++;
                                                                                        currentUserIds = iArr;
                                                                                        i = i6;
                                                                                        ps = ps2;
                                                                                    } catch (Throwable th8) {
                                                                                        N = th8;
                                                                                        packageSetting = ps;
                                                                                        updatedUserIds2 = updatedUserIds3;
                                                                                        runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                                                        changedInstallPermission2 = changedInstallPermission;
                                                                                        permissionCallback = callback;
                                                                                        break;
                                                                                    }
                                                                                } catch (Throwable th9) {
                                                                                    N = th9;
                                                                                    iArr = currentUserIds;
                                                                                    packageSetting = ps;
                                                                                    updatedUserIds2 = updatedUserIds3;
                                                                                    runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                                                    changedInstallPermission2 = changedInstallPermission;
                                                                                    break;
                                                                                }
                                                                            }
                                                                            ps2 = ps;
                                                                            iArr = currentUserIds;
                                                                        } catch (Throwable th10) {
                                                                            N = th10;
                                                                            iArr = currentUserIds;
                                                                            changedInstallPermission = changedInstallPermission2;
                                                                            packageSetting = ps;
                                                                            runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                                            updatedUserIds2 = updatedUserIds;
                                                                            permissionCallback = callback;
                                                                            break;
                                                                        }
                                                                    }
                                                                    ps2 = ps;
                                                                    iArr = currentUserIds;
                                                                    changedInstallPermission = changedInstallPermission2;
                                                                    i3 = grant2;
                                                                    updatedUserIds3 = updatedUserIds;
                                                                    changedInstallPermission2 = changedInstallPermission;
                                                                    break;
                                                                default:
                                                                    ps2 = ps;
                                                                    iArr = currentUserIds;
                                                                    if (str != null) {
                                                                        break;
                                                                    }
                                                                    if (PackageManagerService.DEBUG_PERMISSIONS) {
                                                                        String str5 = TAG;
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("Not granting permission ");
                                                                        stringBuilder.append(str2);
                                                                        stringBuilder.append(" to package ");
                                                                        stringBuilder.append(packageR.packageName);
                                                                        stringBuilder.append(" because it was previously installed without");
                                                                        Slog.i(str5, stringBuilder.toString());
                                                                        break;
                                                                    }
                                                                    break;
                                                            }
                                                        }
                                                        ps2 = ps;
                                                        iArr = currentUserIds;
                                                        String str6;
                                                        if (permissionsState.revokeInstallPermission(N2) != -1) {
                                                            permissionsState.updatePermissionFlags(N2, -1, 255, 0);
                                                            changedInstallPermission2 = true;
                                                            try {
                                                                str6 = TAG;
                                                                StringBuilder stringBuilder4 = new StringBuilder();
                                                                stringBuilder4.append("Un-granting permission ");
                                                                stringBuilder4.append(str2);
                                                                stringBuilder4.append(" from package ");
                                                                stringBuilder4.append(packageR.packageName);
                                                                stringBuilder4.append(" (protectionLevel=");
                                                                stringBuilder4.append(N2.getProtectionLevel());
                                                                stringBuilder4.append(" flags=0x");
                                                                stringBuilder4.append(Integer.toHexString(packageR.applicationInfo.flags));
                                                                stringBuilder4.append(")");
                                                                Slog.i(str6, stringBuilder4.toString());
                                                                updatedUserIds3 = updatedUserIds;
                                                                i = i2 + 1;
                                                                runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                                N2 = N3;
                                                                isLegacySystemApp = z;
                                                                currentUserIds = iArr;
                                                                ps = ps2;
                                                                permissionManagerService = this;
                                                                permissionCallback = callback;
                                                            } catch (Throwable th11) {
                                                                N = th11;
                                                                runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                                updatedUserIds2 = updatedUserIds;
                                                            }
                                                        } else {
                                                            if (N2.isAppOp() && PackageManagerService.DEBUG_PERMISSIONS && (str == null || str.equals(packageR.packageName))) {
                                                                str6 = TAG;
                                                                isLegacySystemApp = new StringBuilder();
                                                                isLegacySystemApp.append("Not granting permission ");
                                                                isLegacySystemApp.append(str2);
                                                                isLegacySystemApp.append(" to package ");
                                                                isLegacySystemApp.append(packageR.packageName);
                                                                isLegacySystemApp.append(" (protectionLevel=");
                                                                isLegacySystemApp.append(N2.getProtectionLevel());
                                                                isLegacySystemApp.append(" flags=0x");
                                                                isLegacySystemApp.append(Integer.toHexString(packageR.applicationInfo.flags));
                                                                isLegacySystemApp.append(")");
                                                                Slog.i(str6, isLegacySystemApp.toString());
                                                            }
                                                            updatedUserIds3 = updatedUserIds;
                                                            changedInstallPermission2 = changedInstallPermission3;
                                                            i = i2 + 1;
                                                            runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                            N2 = N3;
                                                            isLegacySystemApp = z;
                                                            currentUserIds = iArr;
                                                            ps = ps2;
                                                            permissionManagerService = this;
                                                            permissionCallback = callback;
                                                        }
                                                    }
                                                    z = isLegacySystemApp;
                                                } catch (Throwable th12) {
                                                    N = th12;
                                                    z = isLegacySystemApp;
                                                    iArr = currentUserIds;
                                                    packageSetting = ps;
                                                    permName = runtimePermissionsRevoked2;
                                                    i = updatedUserIds;
                                                    changedInstallPermission2 = changedInstallPermission3;
                                                    permissionCallback = callback;
                                                }
                                                try {
                                                    if (PackageManagerService.DEBUG_PERMISSIONS) {
                                                    }
                                                    if (grant2 == 1) {
                                                    }
                                                } catch (Throwable th13) {
                                                    N = th13;
                                                    permName = runtimePermissionsRevoked2;
                                                }
                                            } else {
                                                if (PackageManagerService.DEBUG_PERMISSIONS) {
                                                    str2 = TAG;
                                                    stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("Denying runtime-only permission ");
                                                    stringBuilder3.append(N2.getName());
                                                    stringBuilder3.append(" for package ");
                                                    stringBuilder3.append(packageR.packageName);
                                                    Log.i(str2, stringBuilder3.toString());
                                                }
                                                ps2 = ps;
                                                z = isLegacySystemApp;
                                                iArr = currentUserIds;
                                                i2 = i;
                                                updatedUserIds3 = updatedUserIds;
                                                changedInstallPermission2 = changedInstallPermission3;
                                                i = i2 + 1;
                                                runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                                N2 = N3;
                                                isLegacySystemApp = z;
                                                currentUserIds = iArr;
                                                ps = ps2;
                                                permissionManagerService = this;
                                                permissionCallback = callback;
                                            }
                                        }
                                        if ((str == null || str.equals(packageR.packageName)) && PackageManagerService.DEBUG_PERMISSIONS) {
                                            str2 = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Unknown permission ");
                                            stringBuilder.append(permName);
                                            stringBuilder.append(" in package ");
                                            stringBuilder.append(packageR.packageName);
                                            Slog.i(str2, stringBuilder.toString());
                                        }
                                        updatedUserIds3 = updatedUserIds;
                                        changedInstallPermission2 = changedInstallPermission3;
                                        i = i2 + 1;
                                        runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                        N2 = N3;
                                        isLegacySystemApp = z;
                                        currentUserIds = iArr;
                                        ps = ps2;
                                        permissionManagerService = this;
                                        permissionCallback = callback;
                                    } catch (Throwable th14) {
                                        N = th14;
                                        z = isLegacySystemApp;
                                        iArr = currentUserIds;
                                        changedInstallPermission3 = changedInstallPermission2;
                                        packageSetting = ps;
                                        runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                        updatedUserIds2 = updatedUserIds;
                                        permissionCallback = callback;
                                    }
                                } catch (Throwable th15) {
                                    N = th15;
                                    z = isLegacySystemApp;
                                    iArr = currentUserIds;
                                    changedInstallPermission3 = changedInstallPermission2;
                                    packageSetting = ps;
                                }
                            } catch (Throwable th16) {
                                N = th16;
                                z = isLegacySystemApp;
                                iArr = currentUserIds;
                                changedInstallPermission3 = changedInstallPermission2;
                                packageSetting = ps;
                                runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                updatedUserIds2 = updatedUserIds3;
                            }
                        } else {
                            N3 = N2;
                            ps2 = ps;
                            z = isLegacySystemApp;
                            iArr = currentUserIds;
                            changedInstallPermission3 = changedInstallPermission2;
                            updatedUserIds = updatedUserIds3;
                            if (changedInstallPermission3 || replace) {
                                packageSetting = ps2;
                                if (!packageSetting.areInstallPermissionsFixed()) {
                                    try {
                                    } catch (Throwable th17) {
                                        N = th17;
                                        permissionCallback = callback;
                                        while (true) {
                                            break;
                                        }
                                        throw N;
                                    }
                                }
                            }
                            packageSetting = ps2;
                            try {
                            } catch (Throwable th18) {
                                N = th18;
                                runtimePermissionsRevoked = runtimePermissionsRevoked2;
                                updatedUserIds2 = updatedUserIds;
                                permissionCallback = callback;
                                changedInstallPermission2 = changedInstallPermission3;
                                while (true) {
                                    break;
                                }
                                throw N;
                            }
                        }
                    }
                } catch (Throwable th19) {
                    N = th19;
                    packageSetting = ps;
                    z = isLegacySystemApp;
                    iArr = currentUserIds;
                    while (true) {
                        break;
                    }
                    throw N;
                }
            }
        }
        return;
        permissionCallback = callback;
        while (true) {
            break;
        }
        throw N;
        permissionCallback = callback;
        while (true) {
            break;
        }
        throw N;
    }

    private boolean isNewPlatformPermissionForPackage(String perm, Package pkg) {
        int NP = PackageParser.NEW_PERMISSIONS.length;
        int ip = 0;
        while (ip < NP) {
            NewPermissionInfo npi = PackageParser.NEW_PERMISSIONS[ip];
            if (!npi.name.equals(perm) || pkg.applicationInfo.targetSdkVersion >= npi.sdkVersion) {
                ip++;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Auto-granting ");
                stringBuilder.append(perm);
                stringBuilder.append(" to old pkg ");
                stringBuilder.append(pkg.packageName);
                Log.i(str, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    private boolean hasPrivappWhitelistEntry(String perm, Package pkg) {
        ArraySet<String> wlPermissions;
        if (pkg.isVendor()) {
            wlPermissions = SystemConfig.getInstance().getVendorPrivAppPermissions(pkg.packageName);
        } else if (pkg.isProduct()) {
            wlPermissions = SystemConfig.getInstance().getProductPrivAppPermissions(pkg.packageName);
        } else {
            wlPermissions = SystemConfig.getInstance().getPrivAppPermissions(pkg.packageName);
        }
        boolean whitelisted = wlPermissions != null && wlPermissions.contains(perm);
        if (whitelisted || (pkg.parentPackage != null && hasPrivappWhitelistEntry(perm, pkg.parentPackage))) {
            return true;
        }
        return false;
    }

    private boolean grantSignaturePermission(String perm, Package pkg, BasePermission bp, PermissionsState origPermissions) {
        String str = perm;
        Package packageR = pkg;
        boolean oemPermission = bp.isOEM();
        boolean vendorPrivilegedPermission = bp.isVendorPrivileged();
        boolean privilegedPermission = bp.isPrivileged() || bp.isVendorPrivileged();
        boolean privappPermissionsDisable = RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_DISABLE;
        boolean platformPermission = PackageManagerService.PLATFORM_PACKAGE_NAME.equals(bp.getSourcePackageName());
        boolean platformPackage = PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageR.packageName);
        if (!privappPermissionsDisable && privilegedPermission && pkg.isPrivileged() && !platformPackage && platformPermission && !hasPrivappWhitelistEntry(perm, pkg)) {
            if (!(this.mSystemReady || pkg.isUpdatedSystemApp())) {
                ArraySet<String> deniedPermissions;
                if (pkg.isVendor()) {
                    deniedPermissions = SystemConfig.getInstance().getVendorPrivAppDenyPermissions(packageR.packageName);
                } else if (pkg.isProduct()) {
                    deniedPermissions = SystemConfig.getInstance().getProductPrivAppDenyPermissions(packageR.packageName);
                } else {
                    deniedPermissions = SystemConfig.getInstance().getPrivAppDenyPermissions(packageR.packageName);
                }
                boolean permissionViolation = deniedPermissions == null || !deniedPermissions.contains(str);
                if (!permissionViolation) {
                    return false;
                }
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Privileged permission ");
                stringBuilder.append(str);
                stringBuilder.append(" for package ");
                stringBuilder.append(packageR.packageName);
                stringBuilder.append(" - not in privapp-permissions whitelist");
                Slog.w(str2, stringBuilder.toString());
                if (RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_ENFORCE) {
                    if (this.mPrivappPermissionsViolations == null) {
                        this.mPrivappPermissionsViolations = new ArraySet();
                    }
                    ArraySet arraySet = this.mPrivappPermissionsViolations;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(packageR.packageName);
                    stringBuilder.append(": ");
                    stringBuilder.append(str);
                    arraySet.add(stringBuilder.toString());
                }
            }
            if (RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_ENFORCE) {
                return false;
            }
        }
        Package systemPackage = this.mPackageManagerInt.getPackage(this.mPackageManagerInt.getKnownPackageName(0, 0));
        boolean allowed = packageR.mSigningDetails.hasAncestorOrSelf(bp.getSourcePackageSetting().getSigningDetails()) || bp.getSourcePackageSetting().getSigningDetails().checkCapability(packageR.mSigningDetails, 4) || packageR.mSigningDetails.hasAncestorOrSelf(systemPackage.mSigningDetails) || systemPackage.mSigningDetails.checkCapability(packageR.mSigningDetails, 4);
        if (!allowed && ((privilegedPermission || oemPermission) && pkg.isSystem())) {
            if (pkg.isUpdatedSystemApp()) {
                Package disabledPkg = this.mPackageManagerInt.getDisabledPackage(packageR.packageName);
                PackageSetting disabledPs = disabledPkg != null ? (PackageSetting) disabledPkg.mExtras : null;
                if (disabledPs == null || !disabledPs.getPermissionsState().hasInstallPermission(str)) {
                    if (disabledPs != null && disabledPkg != null && isPackageRequestingPermission(disabledPkg, str) && ((privilegedPermission && disabledPs.isPrivileged()) || (oemPermission && disabledPs.isOem() && canGrantOemPermission(disabledPs, str)))) {
                        allowed = true;
                    }
                    if (packageR.parentPackage != null) {
                        Package disabledParentPkg = this.mPackageManagerInt.getDisabledPackage(packageR.parentPackage.packageName);
                        PackageSetting disabledParentPs = disabledParentPkg != null ? (PackageSetting) disabledParentPkg.mExtras : null;
                        if (disabledParentPkg != null && ((privilegedPermission && disabledParentPs.isPrivileged()) || (oemPermission && disabledParentPs.isOem()))) {
                            if (isPackageRequestingPermission(disabledParentPkg, str) && canGrantOemPermission(disabledParentPs, str)) {
                                allowed = true;
                            } else {
                                if (disabledParentPkg.childPackages != null) {
                                    Iterator it;
                                    for (Iterator it2 = disabledParentPkg.childPackages.iterator(); it2.hasNext(); it2 = it) {
                                        Package disabledParentPkg2 = disabledParentPkg;
                                        disabledParentPkg = (Package) it2.next();
                                        if (disabledParentPkg != null) {
                                            it = it2;
                                            disabledParentPs = (PackageSetting) disabledParentPkg.mExtras;
                                        } else {
                                            it = it2;
                                            disabledParentPs = null;
                                        }
                                        if (isPackageRequestingPermission(disabledParentPkg, str) && canGrantOemPermission(disabledChildPs, str)) {
                                            allowed = true;
                                            break;
                                        }
                                        disabledParentPkg = disabledParentPkg2;
                                    }
                                }
                            }
                        }
                    }
                } else if ((privilegedPermission && disabledPs.isPrivileged()) || (oemPermission && disabledPs.isOem() && canGrantOemPermission(disabledPs, str))) {
                    allowed = true;
                }
            } else {
                boolean z = (privilegedPermission && pkg.isPrivileged()) || (oemPermission && pkg.isOem() && canGrantOemPermission(packageR.mExtras, str));
                allowed = z;
            }
            if (allowed && privilegedPermission && !vendorPrivilegedPermission && pkg.isVendor()) {
                String str3 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Permission ");
                stringBuilder2.append(str);
                stringBuilder2.append(" cannot be granted to privileged vendor apk ");
                stringBuilder2.append(packageR.packageName);
                stringBuilder2.append(" because it isn't a 'vendorPrivileged' permission.");
                Slog.w(str3, stringBuilder2.toString());
                allowed = false;
            }
        }
        PermissionsState permissionsState;
        if (allowed) {
            permissionsState = origPermissions;
            boolean z2 = oemPermission;
        } else {
            if (!allowed && bp.isPre23() && packageR.applicationInfo.targetSdkVersion < 23) {
                allowed = true;
            }
            if (!allowed && bp.isInstaller() && packageR.packageName.equals(this.mPackageManagerInt.getKnownPackageName(2, 0))) {
                allowed = true;
            }
            if (!allowed && bp.isVerifier() && packageR.packageName.equals(this.mPackageManagerInt.getKnownPackageName(3, 0))) {
                allowed = true;
            }
            if (!allowed && bp.isPreInstalled() && pkg.isSystem()) {
                allowed = true;
            }
            if (allowed || !bp.isDevelopment()) {
                permissionsState = origPermissions;
            } else {
                allowed = origPermissions.hasInstallPermission(str);
            }
            if (allowed || !bp.isSetup()) {
            } else {
                if (packageR.packageName.equals(this.mPackageManagerInt.getKnownPackageName(true, 0))) {
                    allowed = true;
                }
            }
            if (!allowed && bp.isSystemTextClassifier() && packageR.packageName.equals(this.mPackageManagerInt.getKnownPackageName(5, 0))) {
                allowed = true;
            }
        }
        if (!allowed) {
            allowed = this.mPackageManagerInt.getHwCertPermission(allowed, packageR, str);
        }
        return allowed;
    }

    private static boolean canGrantOemPermission(PackageSetting ps, String permission) {
        boolean z = false;
        if (!ps.isOem()) {
            return false;
        }
        Boolean granted = (Boolean) SystemConfig.getInstance().getOemPermissions(ps.name).get(permission);
        if (granted != null) {
            if (Boolean.TRUE == granted) {
                z = true;
            }
            return z;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("OEM permission");
        stringBuilder.append(permission);
        stringBuilder.append(" requested by package ");
        stringBuilder.append(ps.name);
        stringBuilder.append(" must be explicitly declared granted or not");
        throw new IllegalStateException(stringBuilder.toString());
    }

    private boolean isPermissionsReviewRequired(Package pkg, int userId) {
        if (this.mSettings.mPermissionReviewRequired && pkg.applicationInfo.targetSdkVersion < 23 && pkg != null && pkg.mExtras != null) {
            return pkg.mExtras.getPermissionsState().isPermissionReviewRequired(userId);
        }
        return false;
    }

    private boolean isPackageRequestingPermission(Package pkg, String permission) {
        int permCount = pkg.requestedPermissions.size();
        for (int j = 0; j < permCount; j++) {
            if (permission.equals((String) pkg.requestedPermissions.get(j))) {
                return true;
            }
        }
        return false;
    }

    @GuardedBy("mLock")
    private void grantRuntimePermissionsGrantedToDisabledPackageLocked(Package pkg, int callingUid, PermissionCallback callback) {
        Package packageR = pkg;
        if (packageR.parentPackage != null && packageR.requestedPermissions != null) {
            Package disabledPkg = this.mPackageManagerInt.getDisabledPackage(packageR.parentPackage.packageName);
            if (disabledPkg != null && disabledPkg.mExtras != null) {
                PackageSetting disabledPs = disabledPkg.mExtras;
                if (disabledPs.isPrivileged() && !disabledPs.hasChildPackages()) {
                    int permCount = packageR.requestedPermissions.size();
                    int i = 0;
                    while (true) {
                        int i2 = i;
                        if (i2 < permCount) {
                            String permission = (String) packageR.requestedPermissions.get(i2);
                            BasePermission bp = this.mSettings.getPermissionLocked(permission);
                            if (bp != null && (bp.isRuntime() || bp.isDevelopment())) {
                                int[] userIds = this.mUserManagerInt.getUserIds();
                                int length = userIds.length;
                                int i3 = 0;
                                while (i3 < length) {
                                    int i4;
                                    int i5;
                                    int[] iArr;
                                    int userId = userIds[i3];
                                    if (disabledPs.getPermissionsState().hasRuntimePermission(permission, userId)) {
                                        i4 = i3;
                                        i5 = length;
                                        iArr = userIds;
                                        grantRuntimePermission(permission, packageR.packageName, false, callingUid, userId, callback);
                                    } else {
                                        i4 = i3;
                                        i5 = length;
                                        iArr = userIds;
                                    }
                                    i3 = i4 + 1;
                                    length = i5;
                                    userIds = iArr;
                                }
                            }
                            i = i2 + 1;
                        } else {
                            return;
                        }
                    }
                }
            }
        }
    }

    private void grantRequestedRuntimePermissions(Package pkg, int[] userIds, String[] grantedPermissions, int callingUid, PermissionCallback callback) {
        for (int userId : userIds) {
            grantRequestedRuntimePermissionsForUser(pkg, userId, grantedPermissions, callingUid, callback);
        }
    }

    private void revokeRuntimePermission(String permName, String packageName, boolean overridePolicy, int callingUid, int userId, PermissionCallback callback) {
        String str = permName;
        String str2 = packageName;
        int i = userId;
        PermissionCallback permissionCallback = callback;
        if (this.mUserManagerInt.exists(i)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
            enforceCrossUserPermission(Binder.getCallingUid(), i, true, true, false, "revokeRuntimePermission");
            Package pkg = this.mPackageManagerInt.getPackage(str2);
            StringBuilder stringBuilder;
            if (pkg == null || pkg.mExtras == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown package: ");
                stringBuilder.append(str2);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (this.mPackageManagerInt.filterAppAccess(pkg, Binder.getCallingUid(), i)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown package: ");
                stringBuilder.append(str2);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else {
                BasePermission bp = this.mSettings.getPermissionLocked(str);
                if (bp != null) {
                    bp.enforceDeclaredUsedAndRuntimeOrDevelopment(pkg);
                    if (!this.mSettings.mPermissionReviewRequired || pkg.applicationInfo.targetSdkVersion >= 23 || !bp.isRuntime()) {
                        PermissionsState permissionsState = pkg.mExtras.getPermissionsState();
                        int flags = permissionsState.getPermissionFlags(str, i);
                        if ((flags & 16) != 0 && UserHandle.getCallingAppId() != 1000) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Non-System UID cannot revoke system fixed permission ");
                            stringBuilder2.append(str);
                            stringBuilder2.append(" for package ");
                            stringBuilder2.append(str2);
                            throw new SecurityException(stringBuilder2.toString());
                        } else if (!overridePolicy && (flags & 4) != 0) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Cannot revoke policy fixed permission ");
                            stringBuilder3.append(str);
                            stringBuilder3.append(" for package ");
                            stringBuilder3.append(str2);
                            throw new SecurityException(stringBuilder3.toString());
                        } else if (bp.isDevelopment()) {
                            if (!(permissionsState.revokeInstallPermission(bp) == -1 || permissionCallback == null)) {
                                callback.onInstallPermissionRevoked();
                            }
                            return;
                        } else if (permissionsState.revokeRuntimePermission(bp, i) != -1) {
                            if (bp.isRuntime()) {
                                logPermission(1245, str, str2);
                            }
                            if (permissionCallback != null) {
                                int uid = UserHandle.getUid(i, pkg.applicationInfo.uid);
                                permissionCallback.onPermissionRevoked(pkg.applicationInfo.uid, i);
                            }
                            return;
                        } else {
                            return;
                        }
                    }
                    return;
                }
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Unknown permission: ");
                stringBuilder4.append(str);
                throw new IllegalArgumentException(stringBuilder4.toString());
            }
        }
        String str3 = TAG;
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append("No such user:");
        stringBuilder5.append(i);
        Log.e(str3, stringBuilder5.toString());
    }

    @GuardedBy("mLock")
    private int[] revokeUnusedSharedUserPermissionsLocked(SharedUserSetting suSetting, int[] allUserIds) {
        PermissionManagerService permissionManagerService = this;
        int[] iArr = allUserIds;
        ArraySet<String> usedPermissions = new ArraySet();
        List<Package> pkgList = suSetting.getPackages();
        if (pkgList == null || pkgList.size() == 0) {
            return EmptyArray.INT;
        }
        int j;
        int i;
        Iterator it = pkgList.iterator();
        while (true) {
            j = 0;
            if (!it.hasNext()) {
                break;
            }
            Package pkg = (Package) it.next();
            if (pkg.requestedPermissions != null) {
                int requestedPermCount = pkg.requestedPermissions.size();
                while (j < requestedPermCount) {
                    String permission = (String) pkg.requestedPermissions.get(j);
                    if (permissionManagerService.mSettings.getPermissionLocked(permission) != null) {
                        usedPermissions.add(permission);
                    }
                    j++;
                }
            }
        }
        PermissionsState permissionsState = suSetting.getPermissionsState();
        List<PermissionState> installPermStates = permissionsState.getInstallPermissionStates();
        int i2 = installPermStates.size() - 1;
        while (true) {
            i = 255;
            if (i2 < 0) {
                break;
            }
            PermissionState permissionState = (PermissionState) installPermStates.get(i2);
            if (!usedPermissions.contains(permissionState.getName())) {
                BasePermission bp = permissionManagerService.mSettings.getPermissionLocked(permissionState.getName());
                if (bp != null) {
                    permissionsState.revokeInstallPermission(bp);
                    permissionsState.updatePermissionFlags(bp, -1, 255, 0);
                }
            }
            i2--;
        }
        int[] runtimePermissionChangedUserIds = EmptyArray.INT;
        int length = iArr.length;
        int[] runtimePermissionChangedUserIds2 = runtimePermissionChangedUserIds;
        i2 = 0;
        while (i2 < length) {
            int i3;
            int userId = iArr[i2];
            List<PermissionState> runtimePermStates = permissionsState.getRuntimePermissionStates(userId);
            int i4 = runtimePermStates.size() - 1;
            while (i4 >= 0) {
                PermissionState permissionState2 = (PermissionState) runtimePermStates.get(i4);
                if (!usedPermissions.contains(permissionState2.getName())) {
                    BasePermission bp2 = permissionManagerService.mSettings.getPermissionLocked(permissionState2.getName());
                    if (bp2 != null) {
                        permissionsState.revokeRuntimePermission(bp2, userId);
                        i3 = 255;
                        i = 0;
                        permissionsState.updatePermissionFlags(bp2, userId, 255, 0);
                        runtimePermissionChangedUserIds2 = ArrayUtils.appendInt(runtimePermissionChangedUserIds2, userId);
                        i4--;
                        j = i;
                        permissionManagerService = this;
                        i = i3;
                        iArr = allUserIds;
                    }
                }
                i3 = 255;
                i = 0;
                i4--;
                j = i;
                permissionManagerService = this;
                i = i3;
                iArr = allUserIds;
            }
            i3 = i;
            i = j;
            i2++;
            permissionManagerService = this;
            i = i3;
            iArr = allUserIds;
        }
        return runtimePermissionChangedUserIds2;
    }

    private String[] getAppOpPermissionPackages(String permName) {
        if (this.mPackageManagerInt.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mLock) {
            ArraySet<String> pkgs = (ArraySet) this.mSettings.mAppOpPermissionPackages.get(permName);
            if (pkgs == null) {
                return null;
            }
            String[] strArr = (String[]) pkgs.toArray(new String[pkgs.size()]);
            return strArr;
        }
    }

    /* JADX WARNING: Missing block: B:16:0x003b, code:
            if (r9.mPackageManagerInt.filterAppAccess(r0, r12, r13) == false) goto L_0x003e;
     */
    /* JADX WARNING: Missing block: B:17:0x003d, code:
            return 0;
     */
    /* JADX WARNING: Missing block: B:19:0x004a, code:
            return r0.mExtras.getPermissionsState().getPermissionFlags(r10, r13);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getPermissionFlags(String permName, String packageName, int callingUid, int userId) {
        if (!this.mUserManagerInt.exists(userId)) {
            return 0;
        }
        enforceGrantRevokeRuntimePermissionPermissions("getPermissionFlags");
        enforceCrossUserPermission(callingUid, userId, true, false, false, "getPermissionFlags");
        Package pkg = this.mPackageManagerInt.getPackage(packageName);
        if (pkg == null || pkg.mExtras == null) {
            return 0;
        }
        synchronized (this.mLock) {
            if (this.mSettings.getPermissionLocked(permName) == null) {
                return 0;
            }
        }
    }

    private void updatePermissions(String packageName, Package pkg, boolean replaceGrant, Collection<Package> allPackages, PermissionCallback callback) {
        int i;
        int flags = 0;
        if (pkg != null) {
            i = 1;
        } else {
            i = 0;
        }
        if (replaceGrant) {
            flags = 2;
        }
        flags |= i;
        updatePermissions(packageName, pkg, getVolumeUuidForPackage(pkg), flags, allPackages, callback);
        if (pkg != null && pkg.childPackages != null) {
            Iterator it = pkg.childPackages.iterator();
            while (it.hasNext()) {
                Package childPkg = (Package) it.next();
                updatePermissions(childPkg.packageName, childPkg, getVolumeUuidForPackage(childPkg), flags, allPackages, callback);
            }
        }
    }

    private void updateAllPermissions(String volumeUuid, boolean sdkUpdated, Collection<Package> allPackages, PermissionCallback callback) {
        int flags;
        if (sdkUpdated) {
            flags = 6;
        } else {
            flags = 0;
        }
        updatePermissions(null, null, volumeUuid, flags | 1, allPackages, callback);
    }

    private void updatePermissions(String changingPkgName, Package changingPkg, String replaceVolumeUuid, int flags, Collection<Package> allPackages, PermissionCallback callback) {
        flags = updatePermissions(changingPkgName, changingPkg, updatePermissionTrees(changingPkgName, changingPkg, flags));
        Trace.traceBegin(262144, "grantPermissions");
        boolean replace = false;
        if ((flags & 1) != 0) {
            for (Package pkg : allPackages) {
                if (pkg != changingPkg) {
                    boolean replace2 = (flags & 4) != 0 && Objects.equals(replaceVolumeUuid, getVolumeUuidForPackage(pkg));
                    grantPermissions(pkg, replace2, changingPkgName, callback);
                }
            }
        }
        if (changingPkg != null) {
            String volumeUuid = getVolumeUuidForPackage(changingPkg);
            if ((flags & 2) != 0 && Objects.equals(replaceVolumeUuid, volumeUuid)) {
                replace = true;
            }
            grantPermissions(changingPkg, replace, changingPkgName, callback);
        }
        Trace.traceEnd(262144);
    }

    private int updatePermissions(String packageName, Package pkg, int flags) {
        Set<BasePermission> needsUpdate = null;
        synchronized (this.mLock) {
            Iterator<BasePermission> it = this.mSettings.mPermissions.values().iterator();
            while (it.hasNext()) {
                BasePermission bp = (BasePermission) it.next();
                if (bp.isDynamic()) {
                    bp.updateDynamicPermission(this.mSettings.mPermissionTrees.values());
                }
                if (bp.getSourcePackageSetting() == null) {
                    if (needsUpdate == null) {
                        needsUpdate = new ArraySet(this.mSettings.mPermissions.size());
                    }
                    needsUpdate.add(bp);
                } else if (packageName != null && packageName.equals(bp.getSourcePackageName())) {
                    if (pkg == null || !hasPermission(pkg, bp.getName())) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Removing old permission tree: ");
                        stringBuilder.append(bp.getName());
                        stringBuilder.append(" from package ");
                        stringBuilder.append(bp.getSourcePackageName());
                        Slog.i(str, stringBuilder.toString());
                        flags |= 1;
                        it.remove();
                    }
                }
            }
        }
        if (needsUpdate != null) {
            for (BasePermission bp2 : needsUpdate) {
                Package sourcePkg = this.mPackageManagerInt.getPackage(bp2.getSourcePackageName());
                synchronized (this.mLock) {
                    if (sourcePkg != null) {
                        if (sourcePkg.mExtras != null) {
                            PackageSetting sourcePs = sourcePkg.mExtras;
                            if (bp2.getSourcePackageSetting() == null) {
                                bp2.setSourcePackageSetting(sourcePs);
                            }
                        }
                    }
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Removing dangling permission: ");
                    stringBuilder2.append(bp2.getName());
                    stringBuilder2.append(" from package ");
                    stringBuilder2.append(bp2.getSourcePackageName());
                    Slog.w(str2, stringBuilder2.toString());
                    this.mSettings.removePermissionLocked(bp2.getName());
                }
            }
        }
        return flags;
    }

    private int updatePermissionTrees(String packageName, Package pkg, int flags) {
        Set<BasePermission> needsUpdate = null;
        synchronized (this.mLock) {
            Iterator<BasePermission> it = this.mSettings.mPermissionTrees.values().iterator();
            while (it.hasNext()) {
                BasePermission bp = (BasePermission) it.next();
                if (bp.getSourcePackageSetting() == null) {
                    if (needsUpdate == null) {
                        needsUpdate = new ArraySet(this.mSettings.mPermissionTrees.size());
                    }
                    needsUpdate.add(bp);
                } else if (packageName != null && packageName.equals(bp.getSourcePackageName())) {
                    if (pkg == null || !hasPermission(pkg, bp.getName())) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Removing old permission tree: ");
                        stringBuilder.append(bp.getName());
                        stringBuilder.append(" from package ");
                        stringBuilder.append(bp.getSourcePackageName());
                        Slog.i(str, stringBuilder.toString());
                        flags |= 1;
                        it.remove();
                    }
                }
            }
        }
        if (needsUpdate != null) {
            for (BasePermission bp2 : needsUpdate) {
                Package sourcePkg = this.mPackageManagerInt.getPackage(bp2.getSourcePackageName());
                synchronized (this.mLock) {
                    if (sourcePkg != null) {
                        if (sourcePkg.mExtras != null) {
                            PackageSetting sourcePs = sourcePkg.mExtras;
                            if (bp2.getSourcePackageSetting() == null) {
                                bp2.setSourcePackageSetting(sourcePs);
                            }
                        }
                    }
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Removing dangling permission tree: ");
                    stringBuilder2.append(bp2.getName());
                    stringBuilder2.append(" from package ");
                    stringBuilder2.append(bp2.getSourcePackageName());
                    Slog.w(str2, stringBuilder2.toString());
                    this.mSettings.removePermissionLocked(bp2.getName());
                }
            }
        }
        return flags;
    }

    private void updatePermissionFlags(String permName, String packageName, int flagMask, int flagValues, int callingUid, int userId, PermissionCallback callback) {
        String str = permName;
        String str2 = packageName;
        int i = callingUid;
        int i2 = userId;
        PermissionCallback permissionCallback = callback;
        if (this.mUserManagerInt.exists(i2)) {
            int flagValues2;
            int flagMask2;
            enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlags");
            enforceCrossUserPermission(i, i2, true, true, false, "updatePermissionFlags");
            if (i != 1000) {
                flagValues2 = ((flagValues & -17) & -33) & -65;
                flagMask2 = (flagMask & -17) & -33;
            } else {
                flagMask2 = flagMask;
                flagValues2 = flagValues;
            }
            Package pkg = this.mPackageManagerInt.getPackage(str2);
            StringBuilder stringBuilder;
            if (pkg == null || pkg.mExtras == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown package: ");
                stringBuilder.append(str2);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (this.mPackageManagerInt.filterAppAccess(pkg, i, i2)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown package: ");
                stringBuilder.append(str2);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else {
                BasePermission bp;
                synchronized (this.mLock) {
                    bp = this.mSettings.getPermissionLocked(str);
                }
                if (bp != null) {
                    PermissionsState permissionsState = pkg.mExtras.getPermissionsState();
                    boolean hadState = permissionsState.getRuntimePermissionState(str, i2) != null;
                    if (permissionsState.updatePermissionFlags(bp, i2, flagMask2, flagValues2) && permissionCallback != null) {
                        if (permissionsState.getInstallPermissionState(str) != null) {
                            callback.onInstallPermissionUpdated();
                        } else if (permissionsState.getRuntimePermissionState(str, i2) != null || hadState) {
                            permissionCallback.onPermissionUpdated(new int[]{i2}, false);
                        }
                    }
                    return;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknown permission: ");
                stringBuilder2.append(str);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }
    }

    private boolean updatePermissionFlagsForAllApps(int flagMask, int flagValues, int callingUid, int userId, Collection<Package> packages, PermissionCallback callback) {
        if (!this.mUserManagerInt.exists(userId)) {
            return false;
        }
        enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlagsForAllApps");
        enforceCrossUserPermission(callingUid, userId, true, true, false, "updatePermissionFlagsForAllApps");
        if (callingUid != 1000) {
            flagMask &= -17;
            flagValues &= -17;
        }
        boolean changed = false;
        for (Package pkg : packages) {
            PackageSetting ps = pkg.mExtras;
            if (ps != null) {
                changed |= ps.getPermissionsState().updatePermissionFlagsForAllPermissions(userId, flagMask, flagValues);
            }
        }
        return changed;
    }

    private void enforceGrantRevokeRuntimePermissionPermissions(String message) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS") != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(message);
            stringBuilder.append(" requires ");
            stringBuilder.append("android.permission.GRANT_RUNTIME_PERMISSIONS");
            stringBuilder.append(" or ");
            stringBuilder.append("android.permission.REVOKE_RUNTIME_PERMISSIONS");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, boolean requirePermissionWhenSameUser, String message) {
        if (userId >= 0) {
            if (checkShell) {
                PackageManagerServiceUtils.enforceShellRestriction("no_debugging_features", callingUid, userId);
            }
            if (!((!requirePermissionWhenSameUser && userId == UserHandle.getUserId(callingUid)) || callingUid == 1000 || callingUid == 0)) {
                if (requireFullPermission) {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
                } else {
                    try {
                        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
                    } catch (SecurityException e) {
                        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", message);
                    }
                }
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid userId ");
        stringBuilder.append(userId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private int calculateCurrentPermissionFootprintLocked(BasePermission tree) {
        int size = 0;
        for (BasePermission perm : this.mSettings.mPermissions.values()) {
            size += tree.calculateFootprint(perm);
        }
        return size;
    }

    private void enforcePermissionCapLocked(PermissionInfo info, BasePermission tree) {
        if (tree.getUid() != 1000) {
            if (info.calculateFootprint() + calculateCurrentPermissionFootprintLocked(tree) > 32768) {
                throw new SecurityException("Permission tree size cap exceeded");
            }
        }
    }

    private void systemReady() {
        this.mSystemReady = true;
        if (this.mPrivappPermissionsViolations != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Signature|privileged permissions not in privapp-permissions whitelist: ");
            stringBuilder.append(this.mPrivappPermissionsViolations);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private static String getVolumeUuidForPackage(Package pkg) {
        if (pkg == null) {
            return StorageManager.UUID_PRIVATE_INTERNAL;
        }
        if (!pkg.isExternal()) {
            return StorageManager.UUID_PRIVATE_INTERNAL;
        }
        if (TextUtils.isEmpty(pkg.volumeUuid)) {
            return "primary_physical";
        }
        return pkg.volumeUuid;
    }

    private static boolean hasPermission(Package pkgInfo, String permName) {
        for (int i = pkgInfo.permissions.size() - 1; i >= 0; i--) {
            if (((Permission) pkgInfo.permissions.get(i)).info.name.equals(permName)) {
                return true;
            }
        }
        return false;
    }

    private void logPermission(int action, String name, String packageName) {
        LogMaker log = new LogMaker(action);
        log.setPackageName(packageName);
        log.addTaggedData(1241, name);
        this.mMetricsLogger.write(log);
    }
}
