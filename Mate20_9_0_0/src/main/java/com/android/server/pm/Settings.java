package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.AuthorityEntry;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser.ActivityIntentInfo;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.Permission;
import android.content.pm.PackageUserState;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.pm.VerifierDeviceIdentity;
import android.net.Uri.Builder;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.EventLogTags;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.XmlUtils;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.os.HwBootFail;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.permission.BasePermission;
import com.android.server.pm.permission.PermissionSettings;
import com.android.server.pm.permission.PermissionsState;
import com.android.server.pm.permission.PermissionsState.PermissionState;
import com.android.server.power.IHwShutdownThread;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class Settings {
    private static final String ATTR_APP_LINK_GENERATION = "app-link-generation";
    private static final String ATTR_BLOCKED = "blocked";
    @Deprecated
    private static final String ATTR_BLOCK_UNINSTALL = "blockUninstall";
    private static final String ATTR_CE_DATA_INODE = "ceDataInode";
    private static final String ATTR_CODE = "code";
    private static final String ATTR_DATABASE_VERSION = "databaseVersion";
    private static final String ATTR_DOMAIN_VERIFICATON_STATE = "domainVerificationStatus";
    private static final String ATTR_ENABLED = "enabled";
    private static final String ATTR_ENABLED_CALLER = "enabledCaller";
    private static final String ATTR_ENFORCEMENT = "enforcement";
    private static final String ATTR_FINGERPRINT = "fingerprint";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_GRANTED = "granted";
    private static final String ATTR_HARMFUL_APP_WARNING = "harmful-app-warning";
    private static final String ATTR_HIDDEN = "hidden";
    private static final String ATTR_HWFINGERPRINT = "hwFingerprint";
    private static final String ATTR_INSTALLED = "inst";
    private static final String ATTR_INSTALL_REASON = "install-reason";
    private static final String ATTR_INSTANT_APP = "instant-app";
    public static final String ATTR_NAME = "name";
    private static final String ATTR_NOT_LAUNCHED = "nl";
    public static final String ATTR_PACKAGE = "package";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_REVOKE_ON_UPGRADE = "rou";
    private static final String ATTR_SDK_VERSION = "sdkVersion";
    private static final String ATTR_STOPPED = "stopped";
    private static final String ATTR_SUSPENDED = "suspended";
    private static final String ATTR_SUSPENDING_PACKAGE = "suspending-package";
    private static final String ATTR_SUSPEND_DIALOG_MESSAGE = "suspend_dialog_message";
    private static final String ATTR_USER = "user";
    private static final String ATTR_USER_FIXED = "fixed";
    private static final String ATTR_USER_SET = "set";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_VIRTUAL_PRELOAD = "virtual-preload";
    private static final String ATTR_VOLUME_UUID = "volumeUuid";
    public static final int CURRENT_DATABASE_VERSION = 3;
    private static final boolean DEBUG_KERNEL = false;
    private static final boolean DEBUG_MU = false;
    private static final boolean DEBUG_PARSER = false;
    private static final boolean DEBUG_STOPPED = false;
    private static final String DIR_CUST_XML = "/data/cust/xml/";
    private static final String DIR_ETC_XML = "/system/etc/xml/";
    private static final String FILE_SUB_USER_DELAPPS_LIST = "hw_subuser_delapps_config.xml";
    static final Object[] FLAG_DUMP_SPEC = new Object[]{Integer.valueOf(1), "SYSTEM", Integer.valueOf(2), "DEBUGGABLE", Integer.valueOf(4), "HAS_CODE", Integer.valueOf(8), "PERSISTENT", Integer.valueOf(16), "FACTORY_TEST", Integer.valueOf(32), "ALLOW_TASK_REPARENTING", Integer.valueOf(64), "ALLOW_CLEAR_USER_DATA", Integer.valueOf(128), "UPDATED_SYSTEM_APP", Integer.valueOf(256), "TEST_ONLY", Integer.valueOf(16384), "VM_SAFE_MODE", Integer.valueOf(32768), "ALLOW_BACKUP", Integer.valueOf(65536), "KILL_AFTER_RESTORE", Integer.valueOf(131072), "RESTORE_ANY_VERSION", Integer.valueOf(262144), "EXTERNAL_STORAGE", Integer.valueOf(DumpState.DUMP_DEXOPT), "LARGE_HEAP"};
    static final Object[] HW_FLAG_DUMP_SPEC = new Object[]{Integer.valueOf(DumpState.DUMP_HANDLE), "PARSE_IS_REMOVABLE_PREINSTALLED_APK", Integer.valueOf(67108864), "FLAG_UPDATED_REMOVEABLE_APP"};
    private static final String KEY_PACKAGE_SETTINS_ERROR = "persist.sys.package_settings_error";
    private static int PRE_M_APP_INFO_FLAG_CANT_SAVE_STATE = 268435456;
    private static int PRE_M_APP_INFO_FLAG_FORWARD_LOCK = 536870912;
    private static int PRE_M_APP_INFO_FLAG_HIDDEN = 134217728;
    private static int PRE_M_APP_INFO_FLAG_PRIVILEGED = 1073741824;
    private static final Object[] PRIVATE_FLAG_DUMP_SPEC = new Object[]{Integer.valueOf(1024), "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE", Integer.valueOf(4096), "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION", Integer.valueOf(2048), "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_UNRESIZEABLE", Integer.valueOf(8192), "BACKUP_IN_FOREGROUND", Integer.valueOf(2), "CANT_SAVE_STATE", Integer.valueOf(32), "DEFAULT_TO_DEVICE_PROTECTED_STORAGE", Integer.valueOf(64), "DIRECT_BOOT_AWARE", Integer.valueOf(4), "FORWARD_LOCK", Integer.valueOf(16), "HAS_DOMAIN_URLS", Integer.valueOf(1), "HIDDEN", Integer.valueOf(128), "EPHEMERAL", Integer.valueOf(32768), "ISOLATED_SPLIT_LOADING", Integer.valueOf(131072), "OEM", Integer.valueOf(256), "PARTIALLY_DIRECT_BOOT_AWARE", Integer.valueOf(8), "PRIVILEGED", Integer.valueOf(512), "REQUIRED_FOR_SYSTEM_USER", Integer.valueOf(16384), "STATIC_SHARED_LIBRARY", Integer.valueOf(262144), "VENDOR", Integer.valueOf(DumpState.DUMP_FROZEN), "PRODUCT", Integer.valueOf(65536), "VIRTUAL_PRELOAD"};
    private static final String RUNTIME_PERMISSIONS_FILE_NAME = "runtime-permissions.xml";
    private static final String TAG = "PackageSettings";
    private static final String TAG_ALL_INTENT_FILTER_VERIFICATION = "all-intent-filter-verifications";
    private static final String TAG_BLOCK_UNINSTALL = "block-uninstall";
    private static final String TAG_BLOCK_UNINSTALL_PACKAGES = "block-uninstall-packages";
    private static final String TAG_CHILD_PACKAGE = "child-package";
    static final String TAG_CROSS_PROFILE_INTENT_FILTERS = "crossProfile-intent-filters";
    private static final String TAG_DEFAULT_APPS = "default-apps";
    private static final String TAG_DEFAULT_BROWSER = "default-browser";
    private static final String TAG_DEFAULT_DIALER = "default-dialer";
    private static final String TAG_DISABLED_COMPONENTS = "disabled-components";
    private static final String TAG_DOMAIN_VERIFICATION = "domain-verification";
    private static final String TAG_ENABLED_COMPONENTS = "enabled-components";
    public static final String TAG_ITEM = "item";
    private static final String TAG_PACKAGE = "pkg";
    private static final String TAG_PACKAGE_RESTRICTIONS = "package-restrictions";
    private static final String TAG_PERMISSIONS = "perms";
    private static final String TAG_PERMISSION_ENTRY = "perm";
    private static final String TAG_PERSISTENT_PREFERRED_ACTIVITIES = "persistent-preferred-activities";
    private static final String TAG_READ_EXTERNAL_STORAGE = "read-external-storage";
    private static final String TAG_RESTORED_RUNTIME_PERMISSIONS = "restored-perms";
    private static final String TAG_RUNTIME_PERMISSIONS = "runtime-permissions";
    private static final String TAG_SHARED_USER = "shared-user";
    private static final String TAG_SUSPENDED_APP_EXTRAS = "suspended-app-extras";
    private static final String TAG_SUSPENDED_LAUNCHER_EXTRAS = "suspended-launcher-extras";
    private static final String TAG_USES_STATIC_LIB = "uses-static-lib";
    private static final String TAG_VERSION = "version";
    private static final int USER_RUNTIME_GRANT_MASK = 11;
    private static int mFirstAvailableUid = 0;
    private static AtomicBoolean mIsCheckDelAppsFinished = new AtomicBoolean(false);
    private boolean isNeedRetryNewUserId;
    private final File mBackupSettingsFilename;
    private final File mBackupStoppedPackagesFilename;
    private final SparseArray<ArraySet<String>> mBlockUninstallPackages;
    final SparseArray<CrossProfileIntentResolver> mCrossProfileIntentResolvers;
    private HwCustSettings mCustSettings;
    final SparseArray<String> mDefaultBrowserApp;
    final SparseArray<String> mDefaultDialerApp;
    private ArrayList<String> mDelAppLists;
    protected final ArrayMap<String, PackageSetting> mDisabledSysPackages;
    final ArraySet<String> mInstallerPackages;
    private boolean mIsPackageSettingsError;
    private final ArrayMap<String, KernelPackageState> mKernelMapping;
    private final File mKernelMappingFilename;
    public final KeySetManagerService mKeySetManagerService;
    private final ArrayMap<Long, Integer> mKeySetRefs;
    private final Object mLock;
    final SparseIntArray mNextAppLinkGeneration;
    private final SparseArray<Object> mOtherUserIds;
    private final File mPackageListFilename;
    final ArrayMap<String, PackageSetting> mPackages;
    final ArrayList<PackageCleanItem> mPackagesToBeCleaned;
    private final ArrayList<Signature> mPastSignatures;
    private final ArrayList<PackageSetting> mPendingPackages;
    final PermissionSettings mPermissions;
    final SparseArray<PersistentPreferredIntentResolver> mPersistentPreferredActivities;
    final SparseArray<PreferredIntentResolver> mPreferredActivities;
    Boolean mReadExternalStorageEnforced;
    final StringBuilder mReadMessages;
    private final ArrayMap<String, String> mRenamedPackages;
    private final ArrayMap<String, IntentFilterVerificationInfo> mRestoredIntentFilterVerifications;
    private final SparseArray<ArrayMap<String, ArraySet<RestoredPermissionGrant>>> mRestoredUserGrants;
    private final RuntimePermissionPersistence mRuntimePermissionsPersistence;
    private final File mSettingsFilename;
    final ArrayMap<String, SharedUserSetting> mSharedUsers;
    private final File mStoppedPackagesFilename;
    private final File mSystemDir;
    private final ArrayList<Object> mUserIds;
    private VerifierDeviceIdentity mVerifierDeviceIdentity;
    private ArrayMap<String, VersionInfo> mVersion;

    public static class DatabaseVersion {
        public static final int FIRST_VERSION = 1;
        public static final int SIGNATURE_END_ENTITY = 2;
        public static final int SIGNATURE_MALFORMED_RECOVER = 3;
    }

    private static final class KernelPackageState {
        int appId;
        int[] excludedUserIds;

        private KernelPackageState() {
        }
    }

    final class RestoredPermissionGrant {
        int grantBits;
        boolean granted;
        String permissionName;

        RestoredPermissionGrant(String name, boolean isGranted, int theGrantBits) {
            this.permissionName = name;
            this.granted = isGranted;
            this.grantBits = theGrantBits;
        }
    }

    private final class RuntimePermissionPersistence {
        private static final long MAX_WRITE_PERMISSIONS_DELAY_MILLIS = 2000;
        private static final long WRITE_PERMISSIONS_DELAY_MILLIS = 200;
        @GuardedBy("mLock")
        private final SparseBooleanArray mDefaultPermissionsGranted = new SparseBooleanArray();
        @GuardedBy("mLock")
        private final SparseArray<String> mFingerprints = new SparseArray();
        private final Handler mHandler = new MyHandler();
        @GuardedBy("mLock")
        private final SparseLongArray mLastNotWrittenMutationTimesMillis = new SparseLongArray();
        private final Object mPersistenceLock;
        @GuardedBy("mLock")
        private final SparseBooleanArray mWriteScheduled = new SparseBooleanArray();

        private final class MyHandler extends Handler {
            public MyHandler() {
                super(BackgroundThread.getHandler().getLooper());
            }

            public void handleMessage(Message message) {
                Runnable callback = message.obj;
                RuntimePermissionPersistence.this.writePermissionsSync(message.what);
                if (callback != null) {
                    callback.run();
                }
            }
        }

        public RuntimePermissionPersistence(Object persistenceLock) {
            this.mPersistenceLock = persistenceLock;
        }

        public boolean areDefaultRuntimPermissionsGrantedLPr(int userId) {
            return this.mDefaultPermissionsGranted.get(userId);
        }

        public void onDefaultRuntimePermissionsGrantedLPr(int userId) {
            this.mFingerprints.put(userId, Build.FINGERPRINT);
            writePermissionsForUserAsyncLPr(userId);
        }

        public void writePermissionsForUserSyncLPr(int userId) {
            this.mHandler.removeMessages(userId);
            writePermissionsSync(userId);
        }

        public void writePermissionsForUserAsyncLPr(int userId) {
            long currentTimeMillis = SystemClock.uptimeMillis();
            if (this.mWriteScheduled.get(userId)) {
                this.mHandler.removeMessages(userId);
                long lastNotWrittenMutationTimeMillis = this.mLastNotWrittenMutationTimesMillis.get(userId);
                if (currentTimeMillis - lastNotWrittenMutationTimeMillis >= MAX_WRITE_PERMISSIONS_DELAY_MILLIS) {
                    this.mHandler.obtainMessage(userId).sendToTarget();
                    return;
                }
                long writeDelayMillis = Math.min(WRITE_PERMISSIONS_DELAY_MILLIS, Math.max((MAX_WRITE_PERMISSIONS_DELAY_MILLIS + lastNotWrittenMutationTimeMillis) - currentTimeMillis, 0));
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(userId), writeDelayMillis);
            } else {
                this.mLastNotWrittenMutationTimesMillis.put(userId, currentTimeMillis);
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(userId), WRITE_PERMISSIONS_DELAY_MILLIS);
                this.mWriteScheduled.put(userId, true);
            }
        }

        /* JADX WARNING: Missing block: B:26:0x00aa, code skipped:
            r0 = null;
            r6 = null;
     */
        /* JADX WARNING: Missing block: B:28:?, code skipped:
            r6 = r3.startWrite();
            r8 = android.util.Xml.newSerializer();
            r8.setOutput(r6, java.nio.charset.StandardCharsets.UTF_8.name());
            r8.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            r8.startDocument(null, java.lang.Boolean.valueOf(true));
            r8.startTag(null, com.android.server.pm.Settings.TAG_RUNTIME_PERMISSIONS);
            r9 = (java.lang.String) r1.mFingerprints.get(r2);
     */
        /* JADX WARNING: Missing block: B:29:0x00d9, code skipped:
            if (r9 == null) goto L_0x00ef;
     */
        /* JADX WARNING: Missing block: B:31:?, code skipped:
            r8.attribute(null, com.android.server.pm.Settings.ATTR_FINGERPRINT, r9);
     */
        /* JADX WARNING: Missing block: B:32:0x00e1, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:33:0x00e2, code skipped:
            r18 = r4;
            r22 = r5;
     */
        /* JADX WARNING: Missing block: B:34:0x00e8, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:35:0x00e9, code skipped:
            r18 = r4;
            r22 = r5;
     */
        /* JADX WARNING: Missing block: B:37:?, code skipped:
            r11 = r4.size();
     */
        /* JADX WARNING: Missing block: B:38:0x00f3, code skipped:
            r12 = 0;
     */
        /* JADX WARNING: Missing block: B:39:0x00f4, code skipped:
            if (r12 >= r11) goto L_0x011a;
     */
        /* JADX WARNING: Missing block: B:41:?, code skipped:
            r13 = (java.lang.String) r4.keyAt(r12);
            r14 = (java.util.List) r4.valueAt(r12);
            r8.startTag(null, "pkg");
            r8.attribute(null, com.android.server.pm.Settings.ATTR_NAME, r13);
            writePermissions(r8, r14);
            r8.endTag(null, "pkg");
     */
        /* JADX WARNING: Missing block: B:42:0x0117, code skipped:
            r12 = r12 + 1;
     */
        /* JADX WARNING: Missing block: B:44:?, code skipped:
            r12 = r5.size();
     */
        /* JADX WARNING: Missing block: B:45:0x011e, code skipped:
            r13 = 0;
     */
        /* JADX WARNING: Missing block: B:46:0x011f, code skipped:
            if (r13 >= r12) goto L_0x0145;
     */
        /* JADX WARNING: Missing block: B:48:?, code skipped:
            r14 = (java.lang.String) r5.keyAt(r13);
            r15 = (java.util.List) r5.valueAt(r13);
            r8.startTag(null, com.android.server.pm.Settings.TAG_SHARED_USER);
            r8.attribute(null, com.android.server.pm.Settings.ATTR_NAME, r14);
            writePermissions(r8, r15);
            r8.endTag(null, com.android.server.pm.Settings.TAG_SHARED_USER);
     */
        /* JADX WARNING: Missing block: B:49:0x0142, code skipped:
            r13 = r13 + 1;
     */
        /* JADX WARNING: Missing block: B:51:?, code skipped:
            r8.endTag(null, com.android.server.pm.Settings.TAG_RUNTIME_PERMISSIONS);
     */
        /* JADX WARNING: Missing block: B:52:0x0155, code skipped:
            if (com.android.server.pm.Settings.access$300(r1.this$0).get(r2) == null) goto L_0x023c;
     */
        /* JADX WARNING: Missing block: B:53:0x0157, code skipped:
            r7 = (android.util.ArrayMap) com.android.server.pm.Settings.access$300(r1.this$0).get(r2);
     */
        /* JADX WARNING: Missing block: B:54:0x0163, code skipped:
            if (r7 == null) goto L_0x023c;
     */
        /* JADX WARNING: Missing block: B:55:0x0165, code skipped:
            r13 = r7.size();
            r14 = 0;
     */
        /* JADX WARNING: Missing block: B:56:0x016a, code skipped:
            if (r14 >= r13) goto L_0x023c;
     */
        /* JADX WARNING: Missing block: B:57:0x016c, code skipped:
            r15 = (android.util.ArraySet) r7.valueAt(r14);
     */
        /* JADX WARNING: Missing block: B:58:0x0173, code skipped:
            if (r15 == null) goto L_0x0229;
     */
        /* JADX WARNING: Missing block: B:60:0x0179, code skipped:
            if (r15.size() <= 0) goto L_0x0229;
     */
        /* JADX WARNING: Missing block: B:61:0x017b, code skipped:
            r17 = (java.lang.String) r7.keyAt(r14);
            r8.startTag(r0, com.android.server.pm.Settings.TAG_RESTORED_RUNTIME_PERMISSIONS);
     */
        /* JADX WARNING: Missing block: B:62:0x018c, code skipped:
            r18 = r4;
            r4 = r17;
     */
        /* JADX WARNING: Missing block: B:64:?, code skipped:
            r8.attribute(r0, "packageName", r4);
            r10 = r15.size();
            r16 = 0;
     */
        /* JADX WARNING: Missing block: B:65:0x0199, code skipped:
            r0 = r16;
     */
        /* JADX WARNING: Missing block: B:66:0x019d, code skipped:
            if (r0 >= r10) goto L_0x0210;
     */
        /* JADX WARNING: Missing block: B:67:0x019f, code skipped:
            r20 = (com.android.server.pm.Settings.RestoredPermissionGrant) r15.valueAt(r0);
            r21 = r4;
     */
        /* JADX WARNING: Missing block: B:68:0x01ac, code skipped:
            r22 = r5;
     */
        /* JADX WARNING: Missing block: B:70:?, code skipped:
            r8.startTag(null, com.android.server.pm.Settings.TAG_PERMISSION_ENTRY);
            r23 = r7;
            r5 = r20;
            r24 = r10;
            r8.attribute(0, com.android.server.pm.Settings.ATTR_NAME, r5.permissionName);
     */
        /* JADX WARNING: Missing block: B:71:0x01c3, code skipped:
            if (r5.granted == null) goto L_0x01ce;
     */
        /* JADX WARNING: Missing block: B:72:0x01c5, code skipped:
            r8.attribute(null, com.android.server.pm.Settings.ATTR_GRANTED, "true");
     */
        /* JADX WARNING: Missing block: B:74:0x01d2, code skipped:
            if ((r5.grantBits & 1) == null) goto L_0x01de;
     */
        /* JADX WARNING: Missing block: B:75:0x01d4, code skipped:
            r8.attribute(null, com.android.server.pm.Settings.ATTR_USER_SET, "true");
     */
        /* JADX WARNING: Missing block: B:77:0x01e2, code skipped:
            if ((r5.grantBits & 2) == null) goto L_0x01ed;
     */
        /* JADX WARNING: Missing block: B:78:0x01e4, code skipped:
            r8.attribute(null, com.android.server.pm.Settings.ATTR_USER_FIXED, "true");
     */
        /* JADX WARNING: Missing block: B:80:0x01f1, code skipped:
            if ((r5.grantBits & 8) == null) goto L_0x01fd;
     */
        /* JADX WARNING: Missing block: B:81:0x01f3, code skipped:
            r8.attribute(null, com.android.server.pm.Settings.ATTR_REVOKE_ON_UPGRADE, "true");
     */
        /* JADX WARNING: Missing block: B:82:0x01fd, code skipped:
            r8.endTag(null, com.android.server.pm.Settings.TAG_PERMISSION_ENTRY);
            r16 = r0 + 1;
            r4 = r21;
            r5 = r22;
            r7 = r23;
            r10 = r24;
     */
        /* JADX WARNING: Missing block: B:83:0x0210, code skipped:
            r21 = r4;
            r22 = r5;
            r23 = r7;
            r24 = r10;
            r4 = null;
            r8.endTag(null, com.android.server.pm.Settings.TAG_RESTORED_RUNTIME_PERMISSIONS);
     */
        /* JADX WARNING: Missing block: B:84:0x0220, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:85:0x0221, code skipped:
            r22 = r5;
     */
        /* JADX WARNING: Missing block: B:86:0x0225, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:87:0x0226, code skipped:
            r22 = r5;
     */
        /* JADX WARNING: Missing block: B:88:0x0229, code skipped:
            r18 = r4;
            r22 = r5;
            r23 = r7;
            r4 = r0;
     */
        /* JADX WARNING: Missing block: B:89:0x0230, code skipped:
            r14 = r14 + 1;
            r0 = r4;
            r4 = r18;
            r5 = r22;
            r7 = r23;
     */
        /* JADX WARNING: Missing block: B:90:0x023c, code skipped:
            r18 = r4;
            r22 = r5;
            r8.endDocument();
            r3.finishWrite(r6);
     */
        /* JADX WARNING: Missing block: B:91:0x024c, code skipped:
            if (android.os.Build.FINGERPRINT.equals(r9) == false) goto L_0x0295;
     */
        /* JADX WARNING: Missing block: B:92:0x024e, code skipped:
            r4 = new java.lang.StringBuilder();
            r4.append("writePermissionsSync -> user:");
            r4.append(r2);
            r4.append(", put true:, Build.FINGERPRINT:");
            r4.append(android.os.Build.FINGERPRINT);
            r4.append(", fingerprint:");
            r4.append(r9);
            android.util.Slog.i("PackageManager", r4.toString());
            r1.mDefaultPermissionsGranted.put(r2, true);
     */
        /* JADX WARNING: Missing block: B:93:0x027e, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:94:0x0280, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:95:0x0281, code skipped:
            r18 = r4;
            r22 = r5;
     */
        /* JADX WARNING: Missing block: B:96:0x0286, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:97:0x0287, code skipped:
            r18 = r4;
            r22 = r5;
     */
        /* JADX WARNING: Missing block: B:99:?, code skipped:
            android.util.Slog.wtf("PackageManager", "Failed to write settings, restoring backup", r0);
            r3.failWrite(r6);
     */
        /* JADX WARNING: Missing block: B:100:0x0295, code skipped:
            libcore.io.IoUtils.closeQuietly(r6);
     */
        /* JADX WARNING: Missing block: B:101:0x0299, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:102:0x029a, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:103:0x029b, code skipped:
            libcore.io.IoUtils.closeQuietly(r6);
     */
        /* JADX WARNING: Missing block: B:104:0x029e, code skipped:
            throw r0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void writePermissionsSync(int userId) {
            Throwable th;
            ArrayMap<String, List<PermissionState>> arrayMap;
            ArrayMap<String, List<PermissionState>> arrayMap2;
            int i = userId;
            File access$200 = Settings.this.getUserRuntimePermissionsFile(i);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("package-perms-");
            stringBuilder.append(i);
            AtomicFile destination = new AtomicFile(access$200, stringBuilder.toString());
            ArrayMap<String, List<PermissionState>> permissionsForPackage = new ArrayMap();
            ArrayMap<String, List<PermissionState>> permissionsForSharedUser = new ArrayMap();
            synchronized (this.mPersistenceLock) {
                try {
                    this.mWriteScheduled.delete(i);
                    int packageCount = Settings.this.mPackages.size();
                    int i2 = 0;
                    while (i2 < packageCount) {
                        try {
                            String packageName = (String) Settings.this.mPackages.keyAt(i2);
                            PackageSetting packageSetting = (PackageSetting) Settings.this.mPackages.valueAt(i2);
                            if (packageSetting.sharedUser == null) {
                                List<PermissionState> permissionsStates = packageSetting.getPermissionsState().getRuntimePermissionStates(i);
                                if (!permissionsStates.isEmpty()) {
                                    permissionsForPackage.put(packageName, permissionsStates);
                                }
                            }
                            i2++;
                        } catch (Throwable th2) {
                            th = th2;
                            arrayMap = permissionsForPackage;
                            arrayMap2 = permissionsForSharedUser;
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
                    i2 = Settings.this.mSharedUsers.size();
                    for (int i3 = 0; i3 < i2; i3++) {
                        String sharedUserName = (String) Settings.this.mSharedUsers.keyAt(i3);
                        List<PermissionState> permissionsStates2 = ((SharedUserSetting) Settings.this.mSharedUsers.valueAt(i3)).getPermissionsState().getRuntimePermissionStates(i);
                        if (!permissionsStates2.isEmpty()) {
                            permissionsForSharedUser.put(sharedUserName, permissionsStates2);
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                    arrayMap = permissionsForPackage;
                    arrayMap2 = permissionsForSharedUser;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }

        private void onUserRemovedLPw(int userId) {
            this.mHandler.removeMessages(userId);
            for (SettingBase sb : Settings.this.mPackages.values()) {
                revokeRuntimePermissionsAndClearFlags(sb, userId);
            }
            for (SettingBase sb2 : Settings.this.mSharedUsers.values()) {
                revokeRuntimePermissionsAndClearFlags(sb2, userId);
            }
            this.mDefaultPermissionsGranted.delete(userId);
            this.mFingerprints.remove(userId);
        }

        private void revokeRuntimePermissionsAndClearFlags(SettingBase sb, int userId) {
            PermissionsState permissionsState = sb.getPermissionsState();
            for (PermissionState permissionState : permissionsState.getRuntimePermissionStates(userId)) {
                BasePermission bp = Settings.this.mPermissions.getPermission(permissionState.getName());
                if (bp != null) {
                    permissionsState.revokeRuntimePermission(bp, userId);
                    permissionsState.updatePermissionFlags(bp, userId, 255, 0);
                }
            }
        }

        public void deleteUserRuntimePermissionsFile(int userId) {
            Settings.this.getUserRuntimePermissionsFile(userId).delete();
        }

        public void readStateForUserSyncLPr(int userId) {
            File permissionsFile = Settings.this.getUserRuntimePermissionsFile(userId);
            if (permissionsFile.exists()) {
                try {
                    FileInputStream in = new AtomicFile(permissionsFile).openRead();
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(in, null);
                        parseRuntimePermissionsLPr(parser, userId);
                        IoUtils.closeQuietly(in);
                    } catch (IOException | XmlPullParserException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed parsing permissions file: ");
                        stringBuilder.append(permissionsFile);
                        throw new IllegalStateException(stringBuilder.toString(), e);
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(in);
                    }
                } catch (FileNotFoundException e2) {
                    Slog.i("PackageManager", "No permissions state");
                }
            }
        }

        public void rememberRestoredUserGrantLPr(String pkgName, String permission, boolean isGranted, int restoredFlagSet, int userId) {
            ArrayMap<String, ArraySet<RestoredPermissionGrant>> grantsByPackage = (ArrayMap) Settings.this.mRestoredUserGrants.get(userId);
            if (grantsByPackage == null) {
                grantsByPackage = new ArrayMap();
                Settings.this.mRestoredUserGrants.put(userId, grantsByPackage);
            }
            ArraySet<RestoredPermissionGrant> grants = (ArraySet) grantsByPackage.get(pkgName);
            if (grants == null) {
                grants = new ArraySet();
                grantsByPackage.put(pkgName, grants);
            }
            grants.add(new RestoredPermissionGrant(permission, isGranted, restoredFlagSet));
        }

        /* JADX WARNING: Removed duplicated region for block: B:42:0x00e4  */
        /* JADX WARNING: Removed duplicated region for block: B:38:0x00ae  */
        /* JADX WARNING: Removed duplicated region for block: B:34:0x0077  */
        /* JADX WARNING: Removed duplicated region for block: B:33:0x006b  */
        /* JADX WARNING: Removed duplicated region for block: B:42:0x00e4  */
        /* JADX WARNING: Removed duplicated region for block: B:38:0x00ae  */
        /* JADX WARNING: Removed duplicated region for block: B:34:0x0077  */
        /* JADX WARNING: Removed duplicated region for block: B:33:0x006b  */
        /* JADX WARNING: Removed duplicated region for block: B:42:0x00e4  */
        /* JADX WARNING: Removed duplicated region for block: B:38:0x00ae  */
        /* JADX WARNING: Removed duplicated region for block: B:34:0x0077  */
        /* JADX WARNING: Removed duplicated region for block: B:33:0x006b  */
        /* JADX WARNING: Missing block: B:26:0x0056, code skipped:
            if (r4.equals("pkg") != false) goto L_0x0065;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void parseRuntimePermissionsLPr(XmlPullParser parser, int userId) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            while (true) {
                int next = parser.next();
                int type = next;
                Object obj = 1;
                if (next == 1) {
                    return;
                }
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3) {
                        if (type != 4) {
                            String name = parser.getName();
                            int hashCode = name.hashCode();
                            if (hashCode == -2044791156) {
                                if (name.equals(Settings.TAG_RESTORED_RUNTIME_PERMISSIONS)) {
                                    obj = 3;
                                    switch (obj) {
                                        case null:
                                            break;
                                        case 1:
                                            break;
                                        case 2:
                                            break;
                                        case 3:
                                            break;
                                    }
                                }
                            } else if (hashCode != 111052) {
                                if (hashCode == 160289295) {
                                    if (name.equals(Settings.TAG_RUNTIME_PERMISSIONS)) {
                                        obj = null;
                                        switch (obj) {
                                            case null:
                                                break;
                                            case 1:
                                                break;
                                            case 2:
                                                break;
                                            case 3:
                                                break;
                                        }
                                    }
                                } else if (hashCode == 485578803 && name.equals(Settings.TAG_SHARED_USER)) {
                                    obj = 2;
                                    String fingerprint;
                                    StringBuilder stringBuilder;
                                    switch (obj) {
                                        case null:
                                            fingerprint = parser.getAttributeValue(null, Settings.ATTR_FINGERPRINT);
                                            this.mFingerprints.put(userId, fingerprint);
                                            boolean defaultsGranted = Build.FINGERPRINT.equals(fingerprint);
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("parseRuntimePermissionsLPr-> user:");
                                            stringBuilder.append(userId);
                                            stringBuilder.append(", put ");
                                            stringBuilder.append(defaultsGranted);
                                            stringBuilder.append(" ,Build.FINGERPRINT:");
                                            stringBuilder.append(Build.FINGERPRINT);
                                            stringBuilder.append(",fingerprint:");
                                            stringBuilder.append(fingerprint);
                                            Slog.i("PackageManager", stringBuilder.toString());
                                            this.mDefaultPermissionsGranted.put(userId, defaultsGranted);
                                        case 1:
                                            fingerprint = parser.getAttributeValue(null, Settings.ATTR_NAME);
                                            PackageSetting ps = (PackageSetting) Settings.this.mPackages.get(fingerprint);
                                            if (ps == null) {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Unknown package:");
                                                stringBuilder.append(fingerprint);
                                                Slog.w("PackageManager", stringBuilder.toString());
                                                XmlUtils.skipCurrentTag(parser);
                                                continue;
                                                continue;
                                                continue;
                                                continue;
                                            } else {
                                                parsePermissionsLPr(parser, ps.getPermissionsState(), userId);
                                            }
                                        case 2:
                                            fingerprint = parser.getAttributeValue(null, Settings.ATTR_NAME);
                                            SharedUserSetting sus = (SharedUserSetting) Settings.this.mSharedUsers.get(fingerprint);
                                            if (sus == null) {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Unknown shared user:");
                                                stringBuilder.append(fingerprint);
                                                Slog.w("PackageManager", stringBuilder.toString());
                                                XmlUtils.skipCurrentTag(parser);
                                                continue;
                                                continue;
                                                continue;
                                                continue;
                                            } else {
                                                parsePermissionsLPr(parser, sus.getPermissionsState(), userId);
                                            }
                                        case 3:
                                            parseRestoredRuntimePermissionsLPr(parser, parser.getAttributeValue(null, "packageName"), userId);
                                            break;
                                    }
                                }
                            }
                            obj = -1;
                            switch (obj) {
                                case null:
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }

        private void parseRestoredRuntimePermissionsLPr(XmlPullParser parser, String pkgName, int userId) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            while (true) {
                int next = parser.next();
                int type = next;
                if (next == 1) {
                    return;
                }
                if (type == 3 && parser.getDepth() <= outerDepth) {
                    return;
                }
                if (type != 3) {
                    if (type != 4) {
                        String name = parser.getName();
                        Object obj = -1;
                        if (name.hashCode() == 3437296 && name.equals(Settings.TAG_PERMISSION_ENTRY)) {
                            obj = null;
                        }
                        if (obj == null) {
                            int permBits;
                            name = parser.getAttributeValue(null, Settings.ATTR_NAME);
                            boolean isGranted = "true".equals(parser.getAttributeValue(null, Settings.ATTR_GRANTED));
                            int permBits2 = 0;
                            if ("true".equals(parser.getAttributeValue(null, Settings.ATTR_USER_SET))) {
                                permBits2 = 0 | 1;
                            }
                            if ("true".equals(parser.getAttributeValue(null, Settings.ATTR_USER_FIXED))) {
                                permBits2 |= 2;
                            }
                            if ("true".equals(parser.getAttributeValue(null, Settings.ATTR_REVOKE_ON_UPGRADE))) {
                                permBits = permBits2 | 8;
                            } else {
                                permBits = permBits2;
                            }
                            if (isGranted || permBits != 0) {
                                rememberRestoredUserGrantLPr(pkgName, name, isGranted, permBits, userId);
                            }
                        }
                    }
                }
            }
        }

        private void parsePermissionsLPr(XmlPullParser parser, PermissionsState permissionsState, int userId) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            while (true) {
                int next = parser.next();
                int type = next;
                boolean granted = true;
                if (next == 1) {
                    return;
                }
                if (type == 3 && parser.getDepth() <= outerDepth) {
                    return;
                }
                if (type != 3) {
                    if (type != 4) {
                        String name = parser.getName();
                        int i = -1;
                        int flags = 0;
                        if (name.hashCode() == 3242771 && name.equals(Settings.TAG_ITEM)) {
                            i = 0;
                        }
                        if (i == 0) {
                            name = parser.getAttributeValue(null, Settings.ATTR_NAME);
                            BasePermission bp = Settings.this.mPermissions.getPermission(name);
                            if (bp == null) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown permission:");
                                stringBuilder.append(name);
                                Slog.w("PackageManager", stringBuilder.toString());
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                String grantedStr = parser.getAttributeValue(null, Settings.ATTR_GRANTED);
                                if (!(grantedStr == null || Boolean.parseBoolean(grantedStr))) {
                                    granted = false;
                                }
                                String flagsStr = parser.getAttributeValue(null, Settings.ATTR_FLAGS);
                                if (flagsStr != null) {
                                    flags = Integer.parseInt(flagsStr, 16);
                                }
                                if (granted) {
                                    permissionsState.grantRuntimePermission(bp, userId);
                                    permissionsState.updatePermissionFlags(bp, userId, 255, flags);
                                } else {
                                    permissionsState.updatePermissionFlags(bp, userId, 255, flags);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void writePermissions(XmlSerializer serializer, List<PermissionState> permissionStates) throws IOException {
            for (PermissionState permissionState : permissionStates) {
                serializer.startTag(null, Settings.TAG_ITEM);
                serializer.attribute(null, Settings.ATTR_NAME, permissionState.getName());
                serializer.attribute(null, Settings.ATTR_GRANTED, String.valueOf(permissionState.isGranted()));
                serializer.attribute(null, Settings.ATTR_FLAGS, Integer.toHexString(permissionState.getFlags()));
                serializer.endTag(null, Settings.TAG_ITEM);
            }
        }
    }

    public static class VersionInfo {
        int databaseVersion;
        String fingerprint;
        String hwFingerprint;
        int sdkVersion;

        public void forceCurrent() {
            this.sdkVersion = VERSION.SDK_INT;
            this.databaseVersion = 3;
            this.fingerprint = Build.FINGERPRINT;
            this.hwFingerprint = Build.HWFINGERPRINT;
        }
    }

    Settings(PermissionSettings permissions, Object lock) {
        this(Environment.getDataDirectory(), permissions, lock);
    }

    Settings(File dataDir, PermissionSettings permission, Object lock) {
        this.mPackages = new ArrayMap();
        this.mInstallerPackages = new ArraySet();
        this.mKernelMapping = new ArrayMap();
        this.mDisabledSysPackages = new ArrayMap();
        this.mBlockUninstallPackages = new SparseArray();
        this.mRestoredIntentFilterVerifications = new ArrayMap();
        this.mCustSettings = (HwCustSettings) HwCustUtils.createObj(HwCustSettings.class, new Object[0]);
        this.mRestoredUserGrants = new SparseArray();
        this.mVersion = new ArrayMap();
        this.mPreferredActivities = new SparseArray();
        this.mPersistentPreferredActivities = new SparseArray();
        this.mCrossProfileIntentResolvers = new SparseArray();
        this.mSharedUsers = new ArrayMap();
        this.mUserIds = new ArrayList();
        this.mOtherUserIds = new SparseArray();
        this.mPastSignatures = new ArrayList();
        this.mKeySetRefs = new ArrayMap();
        this.mPackagesToBeCleaned = new ArrayList();
        this.mRenamedPackages = new ArrayMap();
        this.mDefaultBrowserApp = new SparseArray();
        this.mDefaultDialerApp = new SparseArray();
        this.mNextAppLinkGeneration = new SparseIntArray();
        this.mReadMessages = new StringBuilder();
        this.mPendingPackages = new ArrayList();
        this.mKeySetManagerService = new KeySetManagerService(this.mPackages);
        this.mIsPackageSettingsError = false;
        this.isNeedRetryNewUserId = true;
        this.mDelAppLists = new ArrayList();
        this.mLock = lock;
        this.mPermissions = permission;
        this.mRuntimePermissionsPersistence = new RuntimePermissionPersistence(this.mLock);
        this.mSystemDir = new File(dataDir, "system");
        this.mSystemDir.mkdirs();
        FileUtils.setPermissions(this.mSystemDir.toString(), 509, -1, -1);
        this.mSettingsFilename = new File(this.mSystemDir, "packages.xml");
        this.mBackupSettingsFilename = new File(this.mSystemDir, "packages-backup.xml");
        this.mPackageListFilename = new File(this.mSystemDir, "packages.list");
        FileUtils.setPermissions(this.mPackageListFilename, 416, 1000, 1032);
        File kernelDir = new File("/config/sdcardfs");
        this.mKernelMappingFilename = kernelDir.exists() ? kernelDir : null;
        this.mStoppedPackagesFilename = new File(this.mSystemDir, "packages-stopped.xml");
        this.mBackupStoppedPackagesFilename = new File(this.mSystemDir, "packages-stopped-backup.xml");
    }

    PackageSetting getPackageLPr(String pkgName) {
        return (PackageSetting) this.mPackages.get(pkgName);
    }

    String getRenamedPackageLPr(String pkgName) {
        return (String) this.mRenamedPackages.get(pkgName);
    }

    String addRenamedPackageLPw(String pkgName, String origPkgName) {
        return (String) this.mRenamedPackages.put(pkgName, origPkgName);
    }

    void applyPendingPermissionGrantsLPw(String packageName, int userId) {
        ArrayMap<String, ArraySet<RestoredPermissionGrant>> grantsByPackage = (ArrayMap) this.mRestoredUserGrants.get(userId);
        if (grantsByPackage != null && grantsByPackage.size() != 0) {
            ArraySet<RestoredPermissionGrant> grants = (ArraySet) grantsByPackage.get(packageName);
            if (grants != null && grants.size() != 0) {
                PackageSetting ps = (PackageSetting) this.mPackages.get(packageName);
                if (ps == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't find supposedly installed package ");
                    stringBuilder.append(packageName);
                    Slog.e(str, stringBuilder.toString());
                    return;
                }
                PermissionsState perms = ps.getPermissionsState();
                Iterator it = grants.iterator();
                while (it.hasNext()) {
                    RestoredPermissionGrant grant = (RestoredPermissionGrant) it.next();
                    BasePermission bp = this.mPermissions.getPermission(grant.permissionName);
                    if (bp != null) {
                        if (grant.granted) {
                            perms.grantRuntimePermission(bp, userId);
                        }
                        perms.updatePermissionFlags(bp, userId, 11, grant.grantBits);
                    }
                }
                grantsByPackage.remove(packageName);
                if (grantsByPackage.size() < 1) {
                    this.mRestoredUserGrants.remove(userId);
                }
                writeRuntimePermissionsForUserLPr(userId, false);
            }
        }
    }

    public boolean canPropagatePermissionToInstantApp(String permName) {
        return this.mPermissions.canPropagatePermissionToInstantApp(permName);
    }

    void setInstallerPackageName(String pkgName, String installerPkgName) {
        PackageSetting p = (PackageSetting) this.mPackages.get(pkgName);
        if (p != null) {
            p.setInstallerPackageName(installerPkgName);
            if (installerPkgName != null) {
                this.mInstallerPackages.add(installerPkgName);
            }
        }
    }

    SharedUserSetting getSharedUserLPw(String name, int pkgFlags, int pkgPrivateFlags, boolean create) throws PackageManagerException {
        SharedUserSetting s = (SharedUserSetting) this.mSharedUsers.get(name);
        if (s == null && create) {
            s = new SharedUserSetting(name, pkgFlags, pkgPrivateFlags);
            s.userId = newUserIdLPw(s);
            if (s.userId >= 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("New shared user ");
                stringBuilder.append(name);
                stringBuilder.append(": id=");
                stringBuilder.append(s.userId);
                Log.i("PackageManager", stringBuilder.toString());
                this.mSharedUsers.put(name, s);
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Creating shared user ");
                stringBuilder2.append(name);
                stringBuilder2.append(" failed");
                throw new PackageManagerException(-4, stringBuilder2.toString());
            }
        }
        return s;
    }

    Collection<SharedUserSetting> getAllSharedUsersLPw() {
        return this.mSharedUsers.values();
    }

    String getDisabledSysPackagesPath(String name) {
        PackageSetting dp = (PackageSetting) this.mDisabledSysPackages.get(name);
        if (dp == null) {
            return null;
        }
        return dp.codePathString;
    }

    boolean disableSystemPackageLPw(String name) {
        return disableSystemPackageLPw(name, true);
    }

    boolean disableSystemPackageLPw(String name, boolean replaced) {
        PackageSetting p = (PackageSetting) this.mPackages.get(name);
        if (p == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(name);
            stringBuilder.append(" is not an installed package");
            Log.w("PackageManager", stringBuilder.toString());
            return false;
        } else if (((PackageSetting) this.mDisabledSysPackages.get(name)) != null || p.pkg == null || !p.pkg.isSystem() || p.pkg.isUpdatedSystemApp()) {
            return false;
        } else {
            if (!(p.pkg == null || p.pkg.applicationInfo == null)) {
                ApplicationInfo applicationInfo = p.pkg.applicationInfo;
                applicationInfo.flags |= 128;
            }
            this.mDisabledSysPackages.put(name, p);
            if (replaced) {
                replacePackageLPw(name, new PackageSetting(p));
            }
            return true;
        }
    }

    PackageSetting enableSystemPackageLPw(String name) {
        String str = name;
        PackageSetting p = (PackageSetting) this.mDisabledSysPackages.get(str);
        if (p == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(str);
            stringBuilder.append(" is not disabled");
            Log.w("PackageManager", stringBuilder.toString());
            return null;
        }
        if (!(p.pkg == null || p.pkg.applicationInfo == null)) {
            ApplicationInfo applicationInfo = p.pkg.applicationInfo;
            applicationInfo.flags &= -129;
        }
        String str2 = p.realName;
        File file = p.codePath;
        File file2 = p.resourcePath;
        String str3 = p.legacyNativeLibraryPathString;
        String str4 = p.primaryCpuAbiString;
        String str5 = p.secondaryCpuAbiString;
        String str6 = p.cpuAbiOverrideString;
        int i = p.appId;
        long j = p.versionCode;
        int i2 = p.pkgFlags;
        int i3 = p.pkgPrivateFlags;
        String str7 = p.parentPackageName;
        int i4 = i2;
        String str8 = str7;
        int i5 = i3;
        i2 = i4;
        PackageSetting ret = addPackageLPw(str, str2, file, file2, str3, str4, str5, str6, i, j, i2, i5, str8, p.childPackageNames, p.usesStaticLibraries, p.usesStaticLibrariesVersions);
        this.mDisabledSysPackages.remove(name);
        return ret;
    }

    String getDisabledSystemPackageName(String filePath) {
        if (filePath == null) {
            Log.e(TAG, "getDisabledSystemPackageName, error");
            return null;
        }
        for (PackageSetting pkg : this.mDisabledSysPackages.values()) {
            if (filePath.equals(pkg.codePathString)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getDisabledSystemPackageName ");
                stringBuilder.append(filePath);
                stringBuilder.append(", pkg.name ");
                stringBuilder.append(pkg.name);
                Log.i(str, stringBuilder.toString());
                return pkg.name;
            }
        }
        return null;
    }

    boolean isDisabledSystemPackageLPr(String name) {
        return this.mDisabledSysPackages.containsKey(name);
    }

    void removeDisabledSystemPackageLPw(String name) {
        this.mDisabledSysPackages.remove(name);
    }

    PackageSetting addPackageLPw(String name, String realName, File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, String cpuAbiOverrideString, int uid, long vc, int pkgFlags, int pkgPrivateFlags, String parentPackageName, List<String> childPackageNames, String[] usesStaticLibraries, long[] usesStaticLibraryNames) {
        String str = name;
        int i = uid;
        PackageSetting p = (PackageSetting) this.mPackages.get(str);
        if (p == null) {
            int i2 = i;
            PackageSetting p2 = new PackageSetting(str, realName, codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, vc, pkgFlags, pkgPrivateFlags, parentPackageName, childPackageNames, 0, usesStaticLibraries, usesStaticLibraryNames);
            p2.appId = i2;
            int i3 = i2;
            String str2 = name;
            if (!addUserIdLPw(i3, p2, str2)) {
                return null;
            }
            this.mPackages.put(str2, p2);
            return p2;
        } else if (p.appId == i) {
            return p;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Adding duplicate package, keeping first: ");
            stringBuilder.append(str);
            PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
            return null;
        }
    }

    void addAppOpPackage(String permName, String packageName) {
        this.mPermissions.addAppOpPackage(permName, packageName);
    }

    SharedUserSetting addSharedUserLPw(String name, int uid, int pkgFlags, int pkgPrivateFlags) {
        SharedUserSetting s = (SharedUserSetting) this.mSharedUsers.get(name);
        if (s == null) {
            s = new SharedUserSetting(name, pkgFlags, pkgPrivateFlags);
            s.userId = uid;
            if (!addUserIdLPw(uid, s, name)) {
                return null;
            }
            this.mSharedUsers.put(name, s);
            return s;
        } else if (s.userId == uid) {
            return s;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Adding duplicate shared user, keeping first: ");
            stringBuilder.append(name);
            PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
            return null;
        }
    }

    void pruneSharedUsersLPw() {
        ArrayList<String> removeStage = new ArrayList();
        for (Entry<String, SharedUserSetting> entry : this.mSharedUsers.entrySet()) {
            SharedUserSetting sus = (SharedUserSetting) entry.getValue();
            if (sus == null) {
                removeStage.add((String) entry.getKey());
            } else {
                Iterator<PackageSetting> iter = sus.packages.iterator();
                while (iter.hasNext()) {
                    if (this.mPackages.get(((PackageSetting) iter.next()).name) == null) {
                        iter.remove();
                    }
                }
                if (sus.packages.size() == 0) {
                    removeStage.add((String) entry.getKey());
                }
            }
        }
        for (int i = 0; i < removeStage.size(); i++) {
            this.mSharedUsers.remove(removeStage.get(i));
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:41:0x0131  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x012e  */
    /* JADX WARNING: Missing block: B:13:0x00db, code skipped:
            if (com.android.server.HwServiceFactory.isCustedCouldStopped(r43, false, false) != false) goto L_0x00e5;
     */
    /* JADX WARNING: Missing block: B:31:0x0113, code skipped:
            if (isAdbInstallDisallowed(r63, r8.id) != false) goto L_0x0118;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static PackageSetting createNewSetting(String pkgName, PackageSetting originalPkg, PackageSetting disabledPkg, String realPkgName, SharedUserSetting sharedUser, File codePath, File resourcePath, String legacyNativeLibraryPath, String primaryCpuAbi, String secondaryCpuAbi, long versionCode, int pkgFlags, int pkgPrivateFlags, UserHandle installUser, boolean allowInstall, boolean instantApp, boolean virtualPreload, String parentPkgName, List<String> childPkgNames, UserManagerService userManager, String[] usesStaticLibraries, long[] usesStaticLibrariesVersions) {
        PackageSetting pkgSetting;
        boolean z;
        int i;
        List<UserInfo> users;
        String str = pkgName;
        PackageSetting packageSetting = originalPkg;
        PackageSetting packageSetting2 = disabledPkg;
        SharedUserSetting sharedUserSetting = sharedUser;
        int i2 = pkgFlags;
        List<String> list = childPkgNames;
        UserManagerService userManagerService;
        String str2;
        SharedUserSetting sharedUserSetting2;
        PackageSetting packageSetting3;
        if (packageSetting != null) {
            if (PackageManagerService.DEBUG_UPGRADE) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(str);
                stringBuilder.append(" is adopting original package ");
                stringBuilder.append(packageSetting.name);
                Log.v("PackageManager", stringBuilder.toString());
            }
            pkgSetting = new PackageSetting(packageSetting, str);
            pkgSetting.childPackageNames = list != null ? new ArrayList(list) : null;
            pkgSetting.codePath = codePath;
            pkgSetting.legacyNativeLibraryPathString = legacyNativeLibraryPath;
            pkgSetting.parentPackageName = parentPkgName;
            pkgSetting.pkgFlags = i2;
            pkgSetting.pkgPrivateFlags = pkgPrivateFlags;
            pkgSetting.primaryCpuAbiString = primaryCpuAbi;
            pkgSetting.resourcePath = resourcePath;
            pkgSetting.secondaryCpuAbiString = secondaryCpuAbi;
            pkgSetting.signatures = new PackageSignatures();
            pkgSetting.versionCode = versionCode;
            pkgSetting.usesStaticLibraries = usesStaticLibraries;
            pkgSetting.usesStaticLibrariesVersions = usesStaticLibrariesVersions;
            pkgSetting.setTimeStamp(codePath.lastModified());
            userManagerService = userManager;
            str2 = str;
            sharedUserSetting2 = sharedUser;
            z = false;
            packageSetting3 = disabledPkg;
            i = pkgFlags;
        } else {
            File file = resourcePath;
            String str3 = primaryCpuAbi;
            str2 = secondaryCpuAbi;
            int i3 = pkgPrivateFlags;
            String str4 = parentPkgName;
            String[] strArr = usesStaticLibraries;
            long[] jArr = usesStaticLibrariesVersions;
            i = pkgFlags;
            packageSetting = disabledPkg;
            pkgSetting = new PackageSetting(str, realPkgName, codePath, resourcePath, legacyNativeLibraryPath, primaryCpuAbi, secondaryCpuAbi, null, versionCode, i, pkgPrivateFlags, parentPkgName, childPkgNames, 0, usesStaticLibraries, usesStaticLibrariesVersions);
            pkgSetting.setTimeStamp(codePath.lastModified());
            sharedUserSetting2 = sharedUser;
            pkgSetting.sharedUser = sharedUserSetting2;
            if ((i & 1) != 0) {
                z = false;
            } else {
                str2 = pkgName;
                z = false;
                List<UserInfo> users2 = getAllUsers(userManager);
                i3 = installUser != null ? installUser.getIdentifier() : z;
                if (users2 != null && allowInstall) {
                    for (UserInfo user : users2) {
                        boolean installed;
                        if (installUser != null) {
                            if (i3 != -1) {
                                userManagerService = userManager;
                            }
                            if (i3 != user.id) {
                                installed = z;
                                pkgSetting.setUserState(user.id, 0, 0, user.isClonedProfile() ? z : installed, true, true, false, false, null, null, null, null, instantApp, virtualPreload, null, null, null, 0, 0, 0, null);
                            }
                        } else {
                            userManagerService = userManager;
                        }
                        installed = true;
                        if (user.isClonedProfile()) {
                        }
                        pkgSetting.setUserState(user.id, 0, 0, user.isClonedProfile() ? z : installed, true, true, false, false, null, null, null, null, instantApp, virtualPreload, null, null, null, 0, 0, 0, null);
                    }
                }
            }
            userManagerService = userManager;
            if (sharedUserSetting2 != null) {
                pkgSetting.appId = sharedUserSetting2.userId;
                packageSetting3 = disabledPkg;
            } else {
                packageSetting3 = disabledPkg;
                if (packageSetting3 != null) {
                    pkgSetting.signatures = new PackageSignatures(packageSetting3.signatures);
                    pkgSetting.appId = packageSetting3.appId;
                    pkgSetting.getPermissionsState().copyFrom(disabledPkg.getPermissionsState());
                    users = getAllUsers(userManager);
                    if (users != null) {
                        for (UserInfo user2 : users) {
                            int userId = user2.id;
                            pkgSetting.setDisabledComponentsCopy(packageSetting3.getDisabledComponents(userId), userId);
                            pkgSetting.setEnabledComponentsCopy(packageSetting3.getEnabledComponents(userId), userId);
                        }
                    }
                }
            }
        }
        if ((i & 1) != 0 && packageSetting3 == null) {
            users = getAllUsers(userManager);
            if (users != null) {
                for (UserInfo user22 : users) {
                    if (user22.isClonedProfile()) {
                        pkgSetting.setInstalled(z, user22.id);
                        break;
                    }
                }
            }
        }
        return pkgSetting;
    }

    static void updatePackageSetting(PackageSetting pkgSetting, PackageSetting disabledPkg, SharedUserSetting sharedUser, File codePath, File resourcePath, String legacyNativeLibraryPath, String primaryCpuAbi, String secondaryCpuAbi, int pkgFlags, int pkgPrivateFlags, List<String> childPkgNames, UserManagerService userManager, String[] usesStaticLibraries, long[] usesStaticLibrariesVersions) throws PackageManagerException {
        PackageSetting packageSetting = pkgSetting;
        SharedUserSetting sharedUserSetting = sharedUser;
        File file = codePath;
        File file2 = resourcePath;
        List<String> list = childPkgNames;
        String[] strArr = usesStaticLibraries;
        long[] jArr = usesStaticLibrariesVersions;
        String pkgName = packageSetting.name;
        StringBuilder stringBuilder;
        if (packageSetting.sharedUser != sharedUserSetting) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Package ");
            stringBuilder2.append(pkgName);
            stringBuilder2.append(" shared user changed from ");
            stringBuilder2.append(packageSetting.sharedUser != null ? packageSetting.sharedUser.name : "<nothing>");
            stringBuilder2.append(" to ");
            stringBuilder2.append(sharedUserSetting != null ? sharedUserSetting.name : "<nothing>");
            PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Updating application package ");
            stringBuilder.append(pkgName);
            stringBuilder.append(" failed");
            throw new PackageManagerException(-8, stringBuilder.toString());
        }
        boolean isSystem;
        String str;
        if (packageSetting.codePath.equals(file)) {
            str = legacyNativeLibraryPath;
        } else {
            isSystem = packageSetting.isSystem();
            String str2 = "PackageManager";
            stringBuilder = new StringBuilder();
            stringBuilder.append("Update");
            stringBuilder.append(isSystem ? " system" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder.append(" package ");
            stringBuilder.append(pkgName);
            stringBuilder.append(" code path from ");
            stringBuilder.append(packageSetting.codePathString);
            stringBuilder.append(" to ");
            stringBuilder.append(codePath.toString());
            stringBuilder.append("; Retain data and using new");
            Slog.i(str2, stringBuilder.toString());
            if (isSystem) {
                str = legacyNativeLibraryPath;
            } else {
                if ((pkgFlags & 1) != 0 && disabledPkg == null) {
                    List<UserInfo> allUserInfos = getAllUsers(userManager);
                    if (allUserInfos != null) {
                        for (UserInfo userInfo : allUserInfos) {
                            if (!userInfo.isClonedProfile() || packageSetting.getInstalled(userInfo.id)) {
                                packageSetting.setInstalled(true, userInfo.id);
                                sharedUserSetting = sharedUser;
                            }
                        }
                    }
                }
                packageSetting.legacyNativeLibraryPathString = legacyNativeLibraryPath;
            }
            packageSetting.codePath = file;
            packageSetting.codePathString = codePath.toString();
        }
        if (!packageSetting.resourcePath.equals(file2)) {
            isSystem = packageSetting.isSystem();
            String str3 = "PackageManager";
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Update");
            stringBuilder3.append(isSystem ? " system" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder3.append(" package ");
            stringBuilder3.append(pkgName);
            stringBuilder3.append(" resource path from ");
            stringBuilder3.append(packageSetting.resourcePathString);
            stringBuilder3.append(" to ");
            stringBuilder3.append(resourcePath.toString());
            stringBuilder3.append("; Retain data and using new");
            Slog.i(str3, stringBuilder3.toString());
            packageSetting.resourcePath = file2;
            packageSetting.resourcePathString = resourcePath.toString();
        }
        packageSetting.pkgFlags &= -2;
        packageSetting.pkgPrivateFlags &= -917513;
        packageSetting.pkgFlags |= pkgFlags & 1;
        packageSetting.pkgPrivateFlags |= pkgPrivateFlags & 8;
        packageSetting.pkgPrivateFlags |= pkgPrivateFlags & 131072;
        packageSetting.pkgPrivateFlags |= pkgPrivateFlags & 262144;
        packageSetting.pkgPrivateFlags |= pkgPrivateFlags & DumpState.DUMP_FROZEN;
        packageSetting.primaryCpuAbiString = primaryCpuAbi;
        packageSetting.secondaryCpuAbiString = secondaryCpuAbi;
        if (list != null) {
            packageSetting.childPackageNames = new ArrayList(list);
        }
        if (strArr == null || jArr == null || strArr.length != jArr.length) {
            packageSetting.usesStaticLibraries = null;
            packageSetting.usesStaticLibrariesVersions = null;
            return;
        }
        packageSetting.usesStaticLibraries = strArr;
        packageSetting.usesStaticLibrariesVersions = jArr;
    }

    void addUserToSettingLPw(PackageSetting p) throws PackageManagerException {
        if (p.appId == 0) {
            p.appId = newUserIdLPw(p);
        } else {
            addUserIdLPw(p.appId, p, p.name);
        }
        if (p.appId < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(p.name);
            stringBuilder.append(" could not be assigned a valid UID");
            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Package ");
            stringBuilder2.append(p.name);
            stringBuilder2.append(" could not be assigned a valid UID");
            throw new PackageManagerException(-4, stringBuilder2.toString());
        }
    }

    void writeUserRestrictionsLPw(PackageSetting newPackage, PackageSetting oldPackage) {
        if (getPackageLPr(newPackage.name) != null) {
            List<UserInfo> allUsers = getAllUsers(UserManagerService.getInstance());
            if (allUsers != null) {
                for (UserInfo user : allUsers) {
                    PackageUserState oldUserState;
                    if (oldPackage == null) {
                        oldUserState = PackageSettingBase.DEFAULT_USER_STATE;
                    } else {
                        oldUserState = oldPackage.readUserState(user.id);
                    }
                    if (!oldUserState.equals(newPackage.readUserState(user.id))) {
                        writePackageRestrictionsLPr(user.id);
                    }
                }
            }
        }
    }

    static boolean isAdbInstallDisallowed(UserManagerService userManager, int userId) {
        return userManager.hasUserRestriction("no_debugging_features", userId);
    }

    void insertPackageSettingLPw(PackageSetting p, Package pkg) {
        if (p.signatures.mSigningDetails.signatures == null) {
            p.signatures.mSigningDetails = pkg.mSigningDetails;
        }
        if (p.sharedUser != null && p.sharedUser.signatures.mSigningDetails.signatures == null) {
            p.sharedUser.signatures.mSigningDetails = pkg.mSigningDetails;
        }
        addPackageSettingLPw(p, p.sharedUser);
    }

    private void addPackageSettingLPw(PackageSetting p, SharedUserSetting sharedUser) {
        this.mPackages.put(p.name, p);
        if (sharedUser != null) {
            StringBuilder stringBuilder;
            if (p.sharedUser != null && p.sharedUser != sharedUser) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(p.name);
                stringBuilder.append(" was user ");
                stringBuilder.append(p.sharedUser);
                stringBuilder.append(" but is now ");
                stringBuilder.append(sharedUser);
                stringBuilder.append("; I am not changing its files so it will probably fail!");
                PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                p.sharedUser.removePackage(p);
            } else if (p.appId != sharedUser.userId) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(p.name);
                stringBuilder.append(" was user id ");
                stringBuilder.append(p.appId);
                stringBuilder.append(" but is now user ");
                stringBuilder.append(sharedUser);
                stringBuilder.append(" with id ");
                stringBuilder.append(sharedUser.userId);
                stringBuilder.append("; I am not changing its files so it will probably fail!");
                PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
            }
            sharedUser.addPackage(p);
            p.sharedUser = sharedUser;
            p.appId = sharedUser.userId;
        }
        SettingBase userIdPs = getUserIdLPr(p.appId);
        if (sharedUser == null) {
            if (!(userIdPs == null || userIdPs == p)) {
                replaceUserIdLPw(p.appId, p);
            }
        } else if (!(userIdPs == null || userIdPs == sharedUser)) {
            replaceUserIdLPw(p.appId, sharedUser);
        }
        IntentFilterVerificationInfo ivi = (IntentFilterVerificationInfo) this.mRestoredIntentFilterVerifications.get(p.name);
        if (ivi != null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Applying restored IVI for ");
                stringBuilder2.append(p.name);
                stringBuilder2.append(" : ");
                stringBuilder2.append(ivi.getStatusString());
                Slog.i(str, stringBuilder2.toString());
            }
            this.mRestoredIntentFilterVerifications.remove(p.name);
            p.setIntentFilterVerificationInfo(ivi);
        }
    }

    int updateSharedUserPermsLPw(PackageSetting deletedPs, int userId) {
        if (deletedPs == null || deletedPs.pkg == null) {
            Slog.i("PackageManager", "Trying to update info for null package. Just ignoring");
            return -10000;
        } else if (deletedPs.sharedUser == null) {
            return -10000;
        } else {
            SharedUserSetting sus = deletedPs.sharedUser;
            Iterator it = deletedPs.pkg.requestedPermissions.iterator();
            while (it.hasNext()) {
                String eachPerm = (String) it.next();
                BasePermission bp = this.mPermissions.getPermission(eachPerm);
                if (bp != null) {
                    PackageSetting pkg;
                    boolean used = false;
                    Iterator it2 = sus.packages.iterator();
                    while (it2.hasNext()) {
                        pkg = (PackageSetting) it2.next();
                        if (pkg.pkg != null && !pkg.pkg.packageName.equals(deletedPs.pkg.packageName) && pkg.pkg.requestedPermissions.contains(eachPerm)) {
                            used = true;
                            break;
                        }
                    }
                    if (!used) {
                        PermissionsState permissionsState = sus.getPermissionsState();
                        pkg = getDisabledSystemPkgLPr(deletedPs.pkg.packageName);
                        if (!(pkg == null || pkg.pkg == null)) {
                            boolean reqByDisabledSysPkg = false;
                            Iterator it3 = pkg.pkg.requestedPermissions.iterator();
                            while (it3.hasNext()) {
                                if (((String) it3.next()).equals(eachPerm)) {
                                    reqByDisabledSysPkg = true;
                                    break;
                                }
                            }
                            if (reqByDisabledSysPkg) {
                            }
                        }
                        permissionsState.updatePermissionFlags(bp, userId, 255, 0);
                        if (permissionsState.revokeInstallPermission(bp) == 1) {
                            return -1;
                        }
                        if (permissionsState.revokeRuntimePermission(bp, userId) == 1) {
                            return userId;
                        }
                    }
                }
            }
            return -10000;
        }
    }

    int removePackageLPw(String name) {
        PackageSetting p = (PackageSetting) this.mPackages.get(name);
        if (p != null) {
            this.mPackages.remove(name);
            removeInstallerPackageStatus(name);
            if (p.sharedUser != null) {
                p.sharedUser.removePackage(p);
                if (p.sharedUser.packages.size() == 0) {
                    this.mSharedUsers.remove(p.sharedUser.name);
                    removeUserIdLPw(p.sharedUser.userId);
                    return p.sharedUser.userId;
                }
            }
            removeUserIdLPw(p.appId);
            return p.appId;
        }
        return -1;
    }

    private void removeInstallerPackageStatus(String packageName) {
        if (this.mInstallerPackages.contains(packageName)) {
            for (int i = 0; i < this.mPackages.size(); i++) {
                PackageSetting ps = (PackageSetting) this.mPackages.valueAt(i);
                String installerPackageName = ps.getInstallerPackageName();
                if (installerPackageName != null && installerPackageName.equals(packageName)) {
                    ps.setInstallerPackageName(null);
                    ps.isOrphaned = true;
                }
            }
            this.mInstallerPackages.remove(packageName);
        }
    }

    private void replacePackageLPw(String name, PackageSetting newp) {
        PackageSetting p = (PackageSetting) this.mPackages.get(name);
        if (p != null) {
            if (p.sharedUser != null) {
                p.sharedUser.removePackage(p);
                p.sharedUser.addPackage(newp);
            } else {
                replaceUserIdLPw(p.appId, newp);
            }
        }
        this.mPackages.put(name, newp);
    }

    private boolean addUserIdLPw(int uid, Object obj, Object name) {
        if (uid > 19999) {
            return false;
        }
        if (uid >= 10000) {
            int index = uid - 10000;
            for (int N = this.mUserIds.size(); index >= N; N++) {
                this.mUserIds.add(null);
            }
            if (this.mUserIds.get(index) != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Adding duplicate user id: ");
                stringBuilder.append(uid);
                stringBuilder.append(" name=");
                stringBuilder.append(name);
                PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                return false;
            }
            this.mUserIds.set(index, obj);
        } else if (this.mOtherUserIds.get(uid) != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Adding duplicate shared id: ");
            stringBuilder2.append(uid);
            stringBuilder2.append(" name=");
            stringBuilder2.append(name);
            PackageManagerService.reportSettingsProblem(6, stringBuilder2.toString());
            return false;
        } else {
            this.mOtherUserIds.put(uid, obj);
        }
        return true;
    }

    public Object getUserIdLPr(int uid) {
        if (uid >= 19959 && uid <= 19999) {
            int hbsUid = getHbsUid();
            uid = hbsUid != -1 ? hbsUid : uid;
        }
        if (uid < 10000) {
            return this.mOtherUserIds.get(uid);
        }
        int index = uid - 10000;
        return index < this.mUserIds.size() ? this.mUserIds.get(index) : null;
    }

    private int getHbsUid() {
        try {
            return Os.lstat("/data/data/com.huawei.hbs.framework").st_uid;
        } catch (ErrnoException e) {
            return -1;
        }
    }

    private void removeUserIdLPw(int uid) {
        if (uid >= 10000) {
            int index = uid - 10000;
            if (index < this.mUserIds.size()) {
                this.mUserIds.set(index, null);
            }
        } else {
            this.mOtherUserIds.remove(uid);
        }
        setFirstAvailableUid(uid + 1);
    }

    private void replaceUserIdLPw(int uid, Object obj) {
        if (uid >= 10000) {
            int index = uid - 10000;
            if (index < this.mUserIds.size()) {
                this.mUserIds.set(index, obj);
                return;
            }
            return;
        }
        this.mOtherUserIds.put(uid, obj);
    }

    PreferredIntentResolver editPreferredActivitiesLPw(int userId) {
        PreferredIntentResolver pir = (PreferredIntentResolver) this.mPreferredActivities.get(userId);
        if (pir != null) {
            return pir;
        }
        pir = new PreferredIntentResolver();
        this.mPreferredActivities.put(userId, pir);
        return pir;
    }

    PersistentPreferredIntentResolver editPersistentPreferredActivitiesLPw(int userId) {
        PersistentPreferredIntentResolver ppir = (PersistentPreferredIntentResolver) this.mPersistentPreferredActivities.get(userId);
        if (ppir != null) {
            return ppir;
        }
        ppir = new PersistentPreferredIntentResolver();
        this.mPersistentPreferredActivities.put(userId, ppir);
        return ppir;
    }

    CrossProfileIntentResolver editCrossProfileIntentResolverLPw(int userId) {
        CrossProfileIntentResolver cpir = (CrossProfileIntentResolver) this.mCrossProfileIntentResolvers.get(userId);
        if (cpir != null) {
            return cpir;
        }
        cpir = new CrossProfileIntentResolver();
        this.mCrossProfileIntentResolvers.put(userId, cpir);
        return cpir;
    }

    IntentFilterVerificationInfo getIntentFilterVerificationLPr(String packageName) {
        PackageSetting ps = (PackageSetting) this.mPackages.get(packageName);
        if (ps != null) {
            return ps.getIntentFilterVerificationInfo();
        }
        if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No package known: ");
            stringBuilder.append(packageName);
            Slog.w("PackageManager", stringBuilder.toString());
        }
        return null;
    }

    IntentFilterVerificationInfo createIntentFilterVerificationIfNeededLPw(String packageName, ArraySet<String> domains) {
        PackageSetting ps = (PackageSetting) this.mPackages.get(packageName);
        if (ps == null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No package known: ");
                stringBuilder.append(packageName);
                Slog.w("PackageManager", stringBuilder.toString());
            }
            return null;
        }
        IntentFilterVerificationInfo ivi = ps.getIntentFilterVerificationInfo();
        StringBuilder stringBuilder2;
        if (ivi == null) {
            ivi = new IntentFilterVerificationInfo(packageName, domains);
            ps.setIntentFilterVerificationInfo(ivi);
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Creating new IntentFilterVerificationInfo for pkg: ");
                stringBuilder2.append(packageName);
                Slog.d("PackageManager", stringBuilder2.toString());
            }
        } else {
            ivi.setDomains(domains);
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Setting domains to existing IntentFilterVerificationInfo for pkg: ");
                stringBuilder2.append(packageName);
                stringBuilder2.append(" and with domains: ");
                stringBuilder2.append(ivi.getDomainsString());
                Slog.d("PackageManager", stringBuilder2.toString());
            }
        }
        return ivi;
    }

    int getIntentFilterVerificationStatusLPr(String packageName, int userId) {
        PackageSetting ps = (PackageSetting) this.mPackages.get(packageName);
        if (ps != null) {
            return (int) (ps.getDomainVerificationStatusForUser(userId) >> 32);
        }
        if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No package known: ");
            stringBuilder.append(packageName);
            Slog.w("PackageManager", stringBuilder.toString());
        }
        return 0;
    }

    boolean updateIntentFilterVerificationStatusLPw(String packageName, int status, int userId) {
        PackageSetting current = (PackageSetting) this.mPackages.get(packageName);
        int alwaysGeneration = 0;
        if (current == null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No package known: ");
                stringBuilder.append(packageName);
                Slog.w("PackageManager", stringBuilder.toString());
            }
            return false;
        }
        if (status == 2) {
            alwaysGeneration = this.mNextAppLinkGeneration.get(userId) + 1;
            this.mNextAppLinkGeneration.put(userId, alwaysGeneration);
        }
        current.setDomainVerificationStatusForUser(status, alwaysGeneration, userId);
        return true;
    }

    List<IntentFilterVerificationInfo> getIntentFilterVerificationsLPr(String packageName) {
        if (packageName == null) {
            return Collections.emptyList();
        }
        ArrayList<IntentFilterVerificationInfo> result = new ArrayList();
        for (PackageSetting ps : this.mPackages.values()) {
            IntentFilterVerificationInfo ivi = ps.getIntentFilterVerificationInfo();
            if (!(ivi == null || TextUtils.isEmpty(ivi.getPackageName()))) {
                if (ivi.getPackageName().equalsIgnoreCase(packageName)) {
                    result.add(ivi);
                }
            }
        }
        return result;
    }

    boolean removeIntentFilterVerificationLPw(String packageName, int userId) {
        PackageSetting ps = (PackageSetting) this.mPackages.get(packageName);
        if (ps == null) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No package known: ");
                stringBuilder.append(packageName);
                Slog.w("PackageManager", stringBuilder.toString());
            }
            return false;
        }
        ps.clearDomainVerificationStatusForUser(userId);
        return true;
    }

    boolean removeIntentFilterVerificationLPw(String packageName, int[] userIds) {
        boolean result = false;
        for (int userId : userIds) {
            result |= removeIntentFilterVerificationLPw(packageName, userId);
        }
        return result;
    }

    boolean setDefaultBrowserPackageNameLPw(String packageName, int userId) {
        if (userId == -1) {
            return false;
        }
        if (packageName != null) {
            this.mDefaultBrowserApp.put(userId, packageName);
        } else {
            this.mDefaultBrowserApp.remove(userId);
        }
        writePackageRestrictionsLPr(userId);
        return true;
    }

    String getDefaultBrowserPackageNameLPw(int userId) {
        return userId == -1 ? null : (String) this.mDefaultBrowserApp.get(userId);
    }

    boolean setDefaultDialerPackageNameLPw(String packageName, int userId) {
        if (userId == -1) {
            return false;
        }
        this.mDefaultDialerApp.put(userId, packageName);
        writePackageRestrictionsLPr(userId);
        return true;
    }

    String getDefaultDialerPackageNameLPw(int userId) {
        return userId == -1 ? null : (String) this.mDefaultDialerApp.get(userId);
    }

    private File getUserPackagesStateFile(int userId) {
        return new File(new File(new File(this.mSystemDir, SoundModelContract.KEY_USERS), Integer.toString(userId)), "package-restrictions.xml");
    }

    private File getUserRuntimePermissionsFile(int userId) {
        return new File(new File(new File(this.mSystemDir, SoundModelContract.KEY_USERS), Integer.toString(userId)), RUNTIME_PERMISSIONS_FILE_NAME);
    }

    private File getUserPackagesStateBackupFile(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), "package-restrictions-backup.xml");
    }

    void writeAllUsersPackageRestrictionsLPr() {
        List<UserInfo> users = getAllUsers(UserManagerService.getInstance());
        if (users != null) {
            for (UserInfo user : users) {
                writePackageRestrictionsLPr(user.id);
            }
        }
    }

    void writeAllRuntimePermissionsLPr() {
        for (int userId : UserManagerService.getInstance().getUserIds()) {
            this.mRuntimePermissionsPersistence.writePermissionsForUserAsyncLPr(userId);
        }
    }

    boolean areDefaultRuntimePermissionsGrantedLPr(int userId) {
        return this.mRuntimePermissionsPersistence.areDefaultRuntimPermissionsGrantedLPr(userId);
    }

    void onDefaultRuntimePermissionsGrantedLPr(int userId) {
        this.mRuntimePermissionsPersistence.onDefaultRuntimePermissionsGrantedLPr(userId);
    }

    public VersionInfo findOrCreateVersion(String volumeUuid) {
        VersionInfo ver = (VersionInfo) this.mVersion.get(volumeUuid);
        if (ver != null) {
            return ver;
        }
        ver = new VersionInfo();
        this.mVersion.put(volumeUuid, ver);
        return ver;
    }

    public VersionInfo getInternalVersion() {
        return (VersionInfo) this.mVersion.get(StorageManager.UUID_PRIVATE_INTERNAL);
    }

    public VersionInfo getExternalVersion() {
        return (VersionInfo) this.mVersion.get("primary_physical");
    }

    public void onVolumeForgotten(String fsUuid) {
        this.mVersion.remove(fsUuid);
    }

    void readPreferredActivitiesLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        PreferredActivity pa = new PreferredActivity(parser);
                        if (pa.mPref.getParseError() == null) {
                            editPreferredActivitiesLPw(userId).addFilter(pa);
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error in package manager settings: <preferred-activity> ");
                            stringBuilder.append(pa.mPref.getParseError());
                            stringBuilder.append(" at ");
                            stringBuilder.append(parser.getPositionDescription());
                            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                        }
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown element under <preferred-activities>: ");
                        stringBuilder2.append(parser.getName());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    private void readPersistentPreferredActivitiesLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        editPersistentPreferredActivitiesLPw(userId).addFilter(new PersistentPreferredActivity(parser));
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element under <persistent-preferred-activities>: ");
                        stringBuilder.append(parser.getName());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    private void readCrossProfileIntentFiltersLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_ITEM)) {
                        editCrossProfileIntentResolverLPw(userId).addFilter(new CrossProfileIntentFilter(parser));
                    } else {
                        String msg = new StringBuilder();
                        msg.append("Unknown element under crossProfile-intent-filters: ");
                        msg.append(tagName);
                        PackageManagerService.reportSettingsProblem(5, msg.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    private void readDomainVerificationLPw(XmlPullParser parser, PackageSettingBase packageSetting) throws XmlPullParserException, IOException {
        packageSetting.setIntentFilterVerificationInfo(new IntentFilterVerificationInfo(parser));
    }

    private void readRestoredIntentFilterVerifications(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_DOMAIN_VERIFICATION)) {
                        IntentFilterVerificationInfo ivi = new IntentFilterVerificationInfo(parser);
                        if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Restored IVI for ");
                            stringBuilder.append(ivi.getPackageName());
                            stringBuilder.append(" status=");
                            stringBuilder.append(ivi.getStatusString());
                            Slog.i(str, stringBuilder.toString());
                        }
                        this.mRestoredIntentFilterVerifications.put(ivi.getPackageName(), ivi);
                    } else {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown element: ");
                        stringBuilder2.append(tagName);
                        Slog.w(str2, stringBuilder2.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    void readDefaultAppsLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(TAG_DEFAULT_BROWSER)) {
                        this.mDefaultBrowserApp.put(userId, parser.getAttributeValue(null, "packageName"));
                    } else if (tagName.equals(TAG_DEFAULT_DIALER)) {
                        this.mDefaultDialerApp.put(userId, parser.getAttributeValue(null, "packageName"));
                    } else {
                        String msg = new StringBuilder();
                        msg.append("Unknown element under default-apps: ");
                        msg.append(parser.getName());
                        PackageManagerService.reportSettingsProblem(5, msg.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    void readBlockUninstallPackagesLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        ArraySet<String> packages = new ArraySet();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type != 3) {
                    if (type != 4) {
                        if (parser.getName().equals(TAG_BLOCK_UNINSTALL)) {
                            packages.add(parser.getAttributeValue(null, "packageName"));
                        } else {
                            String msg = new StringBuilder();
                            msg.append("Unknown element under block-uninstall-packages: ");
                            msg.append(parser.getName());
                            PackageManagerService.reportSettingsProblem(5, msg.toString());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
            }
        }
        if (packages.isEmpty()) {
            this.mBlockUninstallPackages.remove(userId);
        } else {
            this.mBlockUninstallPackages.put(userId, packages);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:69:0x016b  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0155 A:{SYNTHETIC, Splitter:B:62:0x0155} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void readPackageRestrictionsLPr(int userId) {
        File userPackagesStateFile;
        FileInputStream str;
        XmlPullParserException e;
        File file;
        int i;
        int i2;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        IOException e2;
        PackageSetting pkg;
        Settings settings = this;
        int i3 = userId;
        FileInputStream str2 = null;
        File userPackagesStateFile2 = getUserPackagesStateFile(userId);
        File backupFile = getUserPackagesStateBackupFile(userId);
        int i4 = 4;
        if (backupFile.exists()) {
            try {
                str2 = new FileInputStream(backupFile);
                settings.mReadMessages.append("Reading from backup stopped packages file\n");
                PackageManagerService.reportSettingsProblem(4, "Need to read from backup stopped packages file");
                if (userPackagesStateFile2.exists()) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Cleaning up stopped packages file ");
                    stringBuilder3.append(userPackagesStateFile2);
                    Slog.w("PackageManager", stringBuilder3.toString());
                    userPackagesStateFile2.delete();
                }
            } catch (IOException e3) {
            }
        }
        FileInputStream str3 = str2;
        if (str3 == null) {
            File backupFile2;
            try {
                File userPackagesStateFile3;
                if (userPackagesStateFile2.exists()) {
                    userPackagesStateFile3 = userPackagesStateFile2;
                    try {
                        userPackagesStateFile = userPackagesStateFile3;
                        try {
                            str = new FileInputStream(userPackagesStateFile);
                        } catch (XmlPullParserException e4) {
                            e = e4;
                            file = userPackagesStateFile;
                            str = str3;
                            i = userId;
                            i2 = 6;
                            stringBuilder = settings.mReadMessages;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error reading: ");
                            stringBuilder2.append(e.toString());
                            stringBuilder.append(stringBuilder2.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error reading stopped packages: ");
                            stringBuilder.append(e);
                            PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                        } catch (IOException e5) {
                            e2 = e5;
                            i = userId;
                            stringBuilder = settings.mReadMessages;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error reading: ");
                            stringBuilder2.append(e2.toString());
                            stringBuilder.append(stringBuilder2.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error reading settings: ");
                            stringBuilder.append(e2);
                            PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                        }
                    } catch (XmlPullParserException e6) {
                        e = e6;
                        str = str3;
                        file = userPackagesStateFile3;
                        i = userId;
                        i2 = 6;
                        stringBuilder = settings.mReadMessages;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error reading: ");
                        stringBuilder2.append(e.toString());
                        stringBuilder.append(stringBuilder2.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error reading stopped packages: ");
                        stringBuilder.append(e);
                        PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                        Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                    } catch (IOException e7) {
                        e2 = e7;
                        str = str3;
                        file = userPackagesStateFile3;
                        i = userId;
                        stringBuilder = settings.mReadMessages;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error reading: ");
                        stringBuilder2.append(e2.toString());
                        stringBuilder.append(stringBuilder2.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error reading settings: ");
                        stringBuilder.append(e2);
                        PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                        Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                    }
                }
                try {
                    settings.mReadMessages.append("No stopped packages file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No stopped packages file; assuming all started");
                    for (PackageSetting pkg2 : settings.mPackages.values()) {
                        backupFile2 = backupFile;
                        userPackagesStateFile3 = userPackagesStateFile2;
                        try {
                            pkg2.setUserState(i3, 0, 0, true, false, false, false, false, null, null, null, null, false, false, null, null, null, 0, 0, 0, null);
                            backupFile = backupFile2;
                            userPackagesStateFile2 = userPackagesStateFile3;
                            i3 = userId;
                        } catch (XmlPullParserException e8) {
                            e = e8;
                            str = str3;
                            file = userPackagesStateFile3;
                            i = userId;
                            i2 = 6;
                            stringBuilder = settings.mReadMessages;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error reading: ");
                            stringBuilder2.append(e.toString());
                            stringBuilder.append(stringBuilder2.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error reading stopped packages: ");
                            stringBuilder.append(e);
                            PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                        } catch (IOException e9) {
                            e2 = e9;
                            str = str3;
                            file = userPackagesStateFile3;
                            i = userId;
                            stringBuilder = settings.mReadMessages;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error reading: ");
                            stringBuilder2.append(e2.toString());
                            stringBuilder.append(stringBuilder2.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error reading settings: ");
                            stringBuilder.append(e2);
                            PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                        }
                    }
                    userPackagesStateFile3 = userPackagesStateFile2;
                    return;
                } catch (XmlPullParserException e10) {
                    e = e10;
                    backupFile2 = backupFile;
                    i2 = 6;
                    file = userPackagesStateFile2;
                    i = i3;
                    str = str3;
                    stringBuilder = settings.mReadMessages;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error reading: ");
                    stringBuilder2.append(e.toString());
                    stringBuilder.append(stringBuilder2.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error reading stopped packages: ");
                    stringBuilder.append(e);
                    PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                } catch (IOException e11) {
                    e2 = e11;
                    backupFile2 = backupFile;
                    file = userPackagesStateFile2;
                    i = i3;
                    str = str3;
                    stringBuilder = settings.mReadMessages;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error reading: ");
                    stringBuilder2.append(e2.toString());
                    stringBuilder.append(stringBuilder2.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error reading settings: ");
                    stringBuilder.append(e2);
                    PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                }
            } catch (XmlPullParserException e12) {
                e = e12;
                backupFile2 = backupFile;
                i2 = 6;
                file = userPackagesStateFile2;
                str = str3;
                i = userId;
                stringBuilder = settings.mReadMessages;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error reading: ");
                stringBuilder2.append(e.toString());
                stringBuilder.append(stringBuilder2.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error reading stopped packages: ");
                stringBuilder.append(e);
                PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
            } catch (IOException e13) {
                e2 = e13;
                backupFile2 = backupFile;
                file = userPackagesStateFile2;
                str = str3;
                i = userId;
                stringBuilder = settings.mReadMessages;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error reading: ");
                stringBuilder2.append(e2.toString());
                stringBuilder.append(stringBuilder2.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error reading settings: ");
                stringBuilder.append(e2);
                PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
            }
        }
        userPackagesStateFile = userPackagesStateFile2;
        str = str3;
        FileInputStream str4;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(str, StandardCharsets.UTF_8.name());
            while (true) {
                int next = parser.next();
                i = next;
                Object obj = 2;
                boolean z = true;
                if (next == 2 || i == 1) {
                    if (i == 2) {
                        try {
                            settings.mReadMessages.append("No start tag found in package restrictions file\n");
                            PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager stopped packages");
                            return;
                        } catch (XmlPullParserException e14) {
                            e = e14;
                            file = userPackagesStateFile;
                            i = userId;
                            i2 = 6;
                            stringBuilder = settings.mReadMessages;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error reading: ");
                            stringBuilder2.append(e.toString());
                            stringBuilder.append(stringBuilder2.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error reading stopped packages: ");
                            stringBuilder.append(e);
                            PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                        } catch (IOException e15) {
                            e2 = e15;
                            file = userPackagesStateFile;
                            i = userId;
                            stringBuilder = settings.mReadMessages;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error reading: ");
                            stringBuilder2.append(e2.toString());
                            stringBuilder.append(stringBuilder2.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error reading settings: ");
                            stringBuilder.append(e2);
                            PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                        }
                    }
                    FileInputStream str5;
                    int outerDepth = parser.getDepth();
                    String str6 = null;
                    int maxAppLinkGeneration = 0;
                    pkg2 = null;
                    while (true) {
                        boolean next2 = parser.next();
                        boolean type = next2;
                        boolean z2;
                        int i5;
                        if (next2 != z) {
                            boolean z3;
                            String str7;
                            Object obj2;
                            int i6;
                            if (type) {
                                if (parser.getDepth() <= outerDepth) {
                                    z2 = type;
                                    i4 = maxAppLinkGeneration;
                                    i5 = outerDepth;
                                    str5 = str;
                                    file = userPackagesStateFile;
                                    i = userId;
                                }
                            }
                            int type2;
                            if (type) {
                                type2 = type;
                                z3 = z;
                                str7 = str6;
                                obj2 = obj;
                                i6 = i4;
                                i5 = outerDepth;
                                str5 = str;
                                file = userPackagesStateFile;
                                i = userId;
                                i4 = maxAppLinkGeneration;
                            } else if (type == i4) {
                                type2 = type;
                                z3 = z;
                                str7 = str6;
                                obj2 = obj;
                                i6 = i4;
                                i5 = outerDepth;
                                str5 = str;
                                file = userPackagesStateFile;
                                i = userId;
                                i4 = maxAppLinkGeneration;
                            } else {
                                try {
                                    String tagName = parser.getName();
                                    String name;
                                    int i7;
                                    if (tagName.equals("pkg")) {
                                        try {
                                            name = parser.getAttributeValue(str6, ATTR_NAME);
                                            PackageSetting ps = (PackageSetting) settings.mPackages.get(name);
                                            if (ps == null) {
                                                StringBuilder stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("No package known for stopped package ");
                                                stringBuilder4.append(name);
                                                Slog.w("PackageManager", stringBuilder4.toString());
                                                XmlUtils.skipCurrentTag(parser);
                                                pkg2 = ps;
                                            } else {
                                                String suspendingPackage;
                                                int packageDepth;
                                                boolean z4;
                                                int maxAppLinkGeneration2 = maxAppLinkGeneration;
                                                String name2 = name;
                                                long ceDataInode = XmlUtils.readLongAttribute(parser, ATTR_CE_DATA_INODE, 0);
                                                String tagName2 = tagName;
                                                tagName = XmlUtils.readBooleanAttribute(parser, ATTR_INSTALLED, z);
                                                boolean stopped = XmlUtils.readBooleanAttribute(parser, ATTR_STOPPED, false);
                                                i4 = maxAppLinkGeneration2;
                                                boolean notLaunched = XmlUtils.readBooleanAttribute(parser, ATTR_NOT_LAUNCHED, false);
                                                String blockedStr = parser.getAttributeValue(str6, ATTR_BLOCKED);
                                                boolean hidden = blockedStr == null ? false : Boolean.parseBoolean(blockedStr);
                                                String hiddenStr = parser.getAttributeValue(str6, ATTR_HIDDEN);
                                                long ceDataInode2 = ceDataInode;
                                                ceDataInode = str6;
                                                boolean hidden2 = hiddenStr == null ? hidden : Boolean.parseBoolean(hiddenStr);
                                                boolean suspended = XmlUtils.readBooleanAttribute(parser, ATTR_SUSPENDED, false);
                                                name = parser.getAttributeValue(null, ATTR_SUSPENDING_PACKAGE);
                                                i5 = outerDepth;
                                                outerDepth = parser.getAttributeValue(0, ATTR_SUSPEND_DIALOG_MESSAGE);
                                                if (suspended && name == null) {
                                                    suspendingPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
                                                } else {
                                                    suspendingPackage = name;
                                                }
                                                boolean blockUninstall = XmlUtils.readBooleanAttribute(parser, ATTR_BLOCK_UNINSTALL, false);
                                                hidden = XmlUtils.readBooleanAttribute(parser, ATTR_INSTANT_APP, false);
                                                boolean virtualPreload = XmlUtils.readBooleanAttribute(parser, ATTR_VIRTUAL_PRELOAD, false);
                                                int i8 = 1;
                                                int enabled = XmlUtils.readIntAttribute(parser, ATTR_ENABLED, 0);
                                                String enabledCaller = parser.getAttributeValue(null, ATTR_ENABLED_CALLER);
                                                String harmfulAppWarning = parser.getAttributeValue(null, ATTR_HARMFUL_APP_WARNING);
                                                int verifState = XmlUtils.readIntAttribute(parser, ATTR_DOMAIN_VERIFICATON_STATE, 0);
                                                i2 = XmlUtils.readIntAttribute(parser, ATTR_APP_LINK_GENERATION, 0);
                                                maxAppLinkGeneration2 = i2 > i4 ? i2 : i4;
                                                int installReason = XmlUtils.readIntAttribute(parser, ATTR_INSTALL_REASON, 0);
                                                int packageDepth2 = parser.getDepth();
                                                ArraySet<String> enabledComponents = 0;
                                                ArraySet<String> disabledComponents = null;
                                                PersistableBundle suspendedAppExtras = null;
                                                PersistableBundle suspendedLauncherExtras = null;
                                                while (true) {
                                                    i = packageDepth2;
                                                    i4 = parser.next();
                                                    int type3 = i4;
                                                    String hiddenStr2;
                                                    if (i4 != i8) {
                                                        i4 = type3;
                                                        if (i4 == 3) {
                                                            if (parser.getDepth() <= i) {
                                                                packageDepth = i;
                                                                hiddenStr2 = hiddenStr;
                                                            }
                                                        }
                                                        if (i4 != 3) {
                                                            if (i4 != 4) {
                                                                name = parser.getName();
                                                                Object obj3 = -1;
                                                                packageDepth = i;
                                                                i = name.hashCode();
                                                                hiddenStr2 = hiddenStr;
                                                                if (i != -2027581689) {
                                                                    if (i != -1963032286) {
                                                                        if (i != -1592287551) {
                                                                            if (i == -1422791362) {
                                                                                if (name.equals(TAG_SUSPENDED_LAUNCHER_EXTRAS) != 0) {
                                                                                    obj3 = 3;
                                                                                }
                                                                            }
                                                                        } else if (name.equals(TAG_SUSPENDED_APP_EXTRAS) != 0) {
                                                                            obj3 = 2;
                                                                        }
                                                                    } else if (name.equals(TAG_ENABLED_COMPONENTS) != 0) {
                                                                        obj3 = null;
                                                                    }
                                                                } else if (name.equals(TAG_DISABLED_COMPONENTS) != 0) {
                                                                    obj3 = 1;
                                                                }
                                                                switch (obj3) {
                                                                    case null:
                                                                        enabledComponents = settings.readComponentsLPr(parser);
                                                                        break;
                                                                    case 1:
                                                                        disabledComponents = settings.readComponentsLPr(parser);
                                                                        break;
                                                                    case 2:
                                                                        suspendedAppExtras = PersistableBundle.restoreFromXml(parser);
                                                                        break;
                                                                    case 3:
                                                                        suspendedLauncherExtras = PersistableBundle.restoreFromXml(parser);
                                                                        break;
                                                                    default:
                                                                        i = TAG;
                                                                        stringBuilder2 = new StringBuilder();
                                                                        stringBuilder2.append("Unknown tag ");
                                                                        stringBuilder2.append(parser.getName());
                                                                        stringBuilder2.append(" under tag ");
                                                                        stringBuilder2.append("pkg");
                                                                        Slog.wtf(i, stringBuilder2.toString());
                                                                        break;
                                                                }
                                                            }
                                                            packageDepth = i;
                                                            hiddenStr2 = hiddenStr;
                                                        } else {
                                                            packageDepth = i;
                                                            hiddenStr2 = hiddenStr;
                                                        }
                                                        type2 = i4;
                                                        packageDepth2 = packageDepth;
                                                        hiddenStr = hiddenStr2;
                                                        i8 = 1;
                                                    } else {
                                                        packageDepth = i;
                                                        hiddenStr2 = hiddenStr;
                                                        i4 = type3;
                                                    }
                                                }
                                                if (blockUninstall) {
                                                    i8 = userId;
                                                    z4 = true;
                                                    try {
                                                        settings.setBlockUninstallLPw(i8, name2, true);
                                                    } catch (XmlPullParserException e16) {
                                                        e = e16;
                                                        i = i8;
                                                        file = userPackagesStateFile;
                                                    } catch (IOException e17) {
                                                        e2 = e17;
                                                        i = i8;
                                                        file = userPackagesStateFile;
                                                        stringBuilder = settings.mReadMessages;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Error reading: ");
                                                        stringBuilder2.append(e2.toString());
                                                        stringBuilder.append(stringBuilder2.toString());
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Error reading settings: ");
                                                        stringBuilder.append(e2);
                                                        PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                                                        Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                                                    }
                                                }
                                                i8 = userId;
                                                z4 = true;
                                                packageDepth2 = packageDepth;
                                                i = i8;
                                                obj2 = 2;
                                                type2 = i4;
                                                i6 = 4;
                                                str5 = str;
                                                file = userPackagesStateFile;
                                                i7 = i8;
                                                int i9 = i2;
                                                ceDataInode = ceDataInode2;
                                                str7 = null;
                                                z3 = z4;
                                                try {
                                                    ps.setUserState(i, ceDataInode, enabled, tagName, stopped, notLaunched, hidden2, suspended, suspendingPackage, outerDepth, suspendedAppExtras, suspendedLauncherExtras, hidden, virtualPreload, enabledCaller, enabledComponents, disabledComponents, verifState, i2, installReason, harmfulAppWarning);
                                                    i = i7;
                                                    maxAppLinkGeneration = maxAppLinkGeneration2;
                                                    settings = this;
                                                    outerDepth = i5;
                                                    obj = obj2;
                                                    str6 = str7;
                                                    z = z3;
                                                    i4 = i6;
                                                    userPackagesStateFile = file;
                                                    str = str5;
                                                } catch (XmlPullParserException e18) {
                                                    e = e18;
                                                    i = i7;
                                                    str = str5;
                                                    settings = this;
                                                    i2 = 6;
                                                    stringBuilder = settings.mReadMessages;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("Error reading: ");
                                                    stringBuilder2.append(e.toString());
                                                    stringBuilder.append(stringBuilder2.toString());
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Error reading stopped packages: ");
                                                    stringBuilder.append(e);
                                                    PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                                                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                                } catch (IOException e19) {
                                                    e2 = e19;
                                                    i = i7;
                                                    str = str5;
                                                    settings = this;
                                                    stringBuilder = settings.mReadMessages;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("Error reading: ");
                                                    stringBuilder2.append(e2.toString());
                                                    stringBuilder.append(stringBuilder2.toString());
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Error reading settings: ");
                                                    stringBuilder.append(e2);
                                                    PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                                                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                                                }
                                            }
                                        } catch (XmlPullParserException e20) {
                                            e = e20;
                                            str5 = str;
                                            file = userPackagesStateFile;
                                            settings = this;
                                            i = userId;
                                            i2 = 6;
                                            stringBuilder = settings.mReadMessages;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Error reading: ");
                                            stringBuilder2.append(e.toString());
                                            stringBuilder.append(stringBuilder2.toString());
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Error reading stopped packages: ");
                                            stringBuilder.append(e);
                                            PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                        } catch (IOException e21) {
                                            e2 = e21;
                                            str5 = str;
                                            file = userPackagesStateFile;
                                            settings = this;
                                            i = userId;
                                            stringBuilder = settings.mReadMessages;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Error reading: ");
                                            stringBuilder2.append(e2.toString());
                                            stringBuilder.append(stringBuilder2.toString());
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Error reading settings: ");
                                            stringBuilder.append(e2);
                                            PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                                            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                                        }
                                    }
                                    type2 = type;
                                    z3 = z;
                                    str7 = str6;
                                    obj2 = obj;
                                    i6 = i4;
                                    i5 = outerDepth;
                                    str5 = str;
                                    file = userPackagesStateFile;
                                    i7 = userId;
                                    i4 = maxAppLinkGeneration;
                                    try {
                                        name = tagName;
                                        if (name.equals("preferred-activities")) {
                                            i = i7;
                                            settings = this;
                                            try {
                                                settings.readPreferredActivitiesLPw(parser, i);
                                            } catch (XmlPullParserException e22) {
                                                e = e22;
                                                i2 = 6;
                                                stringBuilder = settings.mReadMessages;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Error reading: ");
                                                stringBuilder2.append(e.toString());
                                                stringBuilder.append(stringBuilder2.toString());
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Error reading stopped packages: ");
                                                stringBuilder.append(e);
                                                PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                                                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                            } catch (IOException e23) {
                                                e2 = e23;
                                                stringBuilder = settings.mReadMessages;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Error reading: ");
                                                stringBuilder2.append(e2.toString());
                                                stringBuilder.append(stringBuilder2.toString());
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Error reading settings: ");
                                                stringBuilder.append(e2);
                                                PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                                                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                                            }
                                        }
                                        i = i7;
                                        settings = this;
                                        if (name.equals(TAG_PERSISTENT_PREFERRED_ACTIVITIES)) {
                                            settings.readPersistentPreferredActivitiesLPw(parser, i);
                                        } else if (name.equals(TAG_CROSS_PROFILE_INTENT_FILTERS)) {
                                            settings.readCrossProfileIntentFiltersLPw(parser, i);
                                        } else if (name.equals(TAG_DEFAULT_APPS)) {
                                            settings.readDefaultAppsLPw(parser, i);
                                        } else if (name.equals(TAG_BLOCK_UNINSTALL_PACKAGES)) {
                                            settings.readBlockUninstallPackagesLPw(parser, i);
                                        } else {
                                            StringBuilder stringBuilder5 = new StringBuilder();
                                            stringBuilder5.append("Unknown element under <stopped-packages>: ");
                                            stringBuilder5.append(parser.getName());
                                            Slog.w("PackageManager", stringBuilder5.toString());
                                            XmlUtils.skipCurrentTag(parser);
                                        }
                                        maxAppLinkGeneration = i4;
                                        outerDepth = i5;
                                        obj = obj2;
                                        str6 = str7;
                                        z = z3;
                                        i4 = i6;
                                        userPackagesStateFile = file;
                                        str = str5;
                                    } catch (XmlPullParserException e24) {
                                        e = e24;
                                        i = i7;
                                        settings = this;
                                        i2 = 6;
                                        stringBuilder = settings.mReadMessages;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Error reading: ");
                                        stringBuilder2.append(e.toString());
                                        stringBuilder.append(stringBuilder2.toString());
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error reading stopped packages: ");
                                        stringBuilder.append(e);
                                        PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                                        Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                    } catch (IOException e25) {
                                        e2 = e25;
                                        i = i7;
                                        settings = this;
                                        stringBuilder = settings.mReadMessages;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Error reading: ");
                                        stringBuilder2.append(e2.toString());
                                        stringBuilder.append(stringBuilder2.toString());
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error reading settings: ");
                                        stringBuilder.append(e2);
                                        PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                                        Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                                    }
                                } catch (XmlPullParserException e26) {
                                    e = e26;
                                    str5 = str;
                                    file = userPackagesStateFile;
                                    i = userId;
                                    i2 = 6;
                                    stringBuilder = settings.mReadMessages;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Error reading: ");
                                    stringBuilder2.append(e.toString());
                                    stringBuilder.append(stringBuilder2.toString());
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error reading stopped packages: ");
                                    stringBuilder.append(e);
                                    PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                                } catch (IOException e27) {
                                    e2 = e27;
                                    str5 = str;
                                    file = userPackagesStateFile;
                                    i = userId;
                                    stringBuilder = settings.mReadMessages;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Error reading: ");
                                    stringBuilder2.append(e2.toString());
                                    stringBuilder.append(stringBuilder2.toString());
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error reading settings: ");
                                    stringBuilder.append(e2);
                                    PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                                    Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                                }
                            }
                            maxAppLinkGeneration = i4;
                            outerDepth = i5;
                            obj = obj2;
                            str6 = str7;
                            z = z3;
                            i4 = i6;
                            userPackagesStateFile = file;
                            str = str5;
                        } else {
                            z2 = type;
                            i4 = maxAppLinkGeneration;
                            i5 = outerDepth;
                            str5 = str;
                            file = userPackagesStateFile;
                            i = userId;
                        }
                    }
                    str4 = str5;
                    try {
                        str4.close();
                        settings.mNextAppLinkGeneration.put(i, i4 + 1);
                        str = str4;
                    } catch (XmlPullParserException e28) {
                        e = e28;
                        str = str4;
                        i2 = 6;
                        stringBuilder = settings.mReadMessages;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error reading: ");
                        stringBuilder2.append(e.toString());
                        stringBuilder.append(stringBuilder2.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error reading stopped packages: ");
                        stringBuilder.append(e);
                        PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
                        Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
                    } catch (IOException e29) {
                        e2 = e29;
                        str = str4;
                        stringBuilder = settings.mReadMessages;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error reading: ");
                        stringBuilder2.append(e2.toString());
                        stringBuilder.append(stringBuilder2.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error reading settings: ");
                        stringBuilder.append(e2);
                        PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                        Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                    }
                }
            }
            if (i == 2) {
            }
        } catch (XmlPullParserException e30) {
            e = e30;
            str4 = str;
            file = userPackagesStateFile;
            i = userId;
            i2 = 6;
            stringBuilder = settings.mReadMessages;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error reading: ");
            stringBuilder2.append(e.toString());
            stringBuilder.append(stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error reading stopped packages: ");
            stringBuilder.append(e);
            PackageManagerService.reportSettingsProblem(i2, stringBuilder.toString());
            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e);
        } catch (IOException e31) {
            e2 = e31;
            str4 = str;
            file = userPackagesStateFile;
            i = userId;
            stringBuilder = settings.mReadMessages;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error reading: ");
            stringBuilder2.append(e2.toString());
            stringBuilder.append(stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error reading settings: ");
            stringBuilder.append(e2);
            PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
            Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
        }
    }

    void setBlockUninstallLPw(int userId, String packageName, boolean blockUninstall) {
        ArraySet<String> packages = (ArraySet) this.mBlockUninstallPackages.get(userId);
        if (blockUninstall) {
            if (packages == null) {
                packages = new ArraySet();
                this.mBlockUninstallPackages.put(userId, packages);
            }
            packages.add(packageName);
        } else if (packages != null) {
            packages.remove(packageName);
            if (packages.isEmpty()) {
                this.mBlockUninstallPackages.remove(userId);
            }
        }
    }

    boolean getBlockUninstallLPr(int userId, String packageName) {
        ArraySet<String> packages = (ArraySet) this.mBlockUninstallPackages.get(userId);
        if (packages == null) {
            return false;
        }
        return packages.contains(packageName);
    }

    private ArraySet<String> readComponentsLPr(XmlPullParser parser) throws IOException, XmlPullParserException {
        ArraySet<String> components = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return components;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        String componentName = parser.getAttributeValue(null, ATTR_NAME);
                        if (componentName != null) {
                            if (components == null) {
                                components = new ArraySet();
                            }
                            components.add(componentName);
                        }
                    }
                }
            }
        }
        return components;
    }

    void writePreferredActivitiesLPr(XmlSerializer serializer, int userId, boolean full) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, "preferred-activities");
        PreferredIntentResolver pir = (PreferredIntentResolver) this.mPreferredActivities.get(userId);
        if (pir != null) {
            for (PreferredActivity pa : pir.filterSet()) {
                serializer.startTag(null, TAG_ITEM);
                pa.writeToXml(serializer, full);
                serializer.endTag(null, TAG_ITEM);
            }
        }
        serializer.endTag(null, "preferred-activities");
    }

    void writePersistentPreferredActivitiesLPr(XmlSerializer serializer, int userId) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, TAG_PERSISTENT_PREFERRED_ACTIVITIES);
        PersistentPreferredIntentResolver ppir = (PersistentPreferredIntentResolver) this.mPersistentPreferredActivities.get(userId);
        if (ppir != null) {
            for (PersistentPreferredActivity ppa : ppir.filterSet()) {
                serializer.startTag(null, TAG_ITEM);
                ppa.writeToXml(serializer);
                serializer.endTag(null, TAG_ITEM);
            }
        }
        serializer.endTag(null, TAG_PERSISTENT_PREFERRED_ACTIVITIES);
    }

    void writeCrossProfileIntentFiltersLPr(XmlSerializer serializer, int userId) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, TAG_CROSS_PROFILE_INTENT_FILTERS);
        CrossProfileIntentResolver cpir = (CrossProfileIntentResolver) this.mCrossProfileIntentResolvers.get(userId);
        if (cpir != null) {
            for (CrossProfileIntentFilter cpif : cpir.filterSet()) {
                serializer.startTag(null, TAG_ITEM);
                cpif.writeToXml(serializer);
                serializer.endTag(null, TAG_ITEM);
            }
        }
        serializer.endTag(null, TAG_CROSS_PROFILE_INTENT_FILTERS);
    }

    void writeDomainVerificationsLPr(XmlSerializer serializer, IntentFilterVerificationInfo verificationInfo) throws IllegalArgumentException, IllegalStateException, IOException {
        if (verificationInfo != null && verificationInfo.getPackageName() != null) {
            serializer.startTag(null, TAG_DOMAIN_VERIFICATION);
            verificationInfo.writeToXml(serializer);
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Wrote domain verification for package: ");
                stringBuilder.append(verificationInfo.getPackageName());
                Slog.d(str, stringBuilder.toString());
            }
            serializer.endTag(null, TAG_DOMAIN_VERIFICATION);
        }
    }

    void writeAllDomainVerificationsLPr(XmlSerializer serializer, int userId) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, TAG_ALL_INTENT_FILTER_VERIFICATION);
        int N = this.mPackages.size();
        for (int i = 0; i < N; i++) {
            IntentFilterVerificationInfo ivi = ((PackageSetting) this.mPackages.valueAt(i)).getIntentFilterVerificationInfo();
            if (ivi != null) {
                writeDomainVerificationsLPr(serializer, ivi);
            }
        }
        serializer.endTag(null, TAG_ALL_INTENT_FILTER_VERIFICATION);
    }

    void readAllDomainVerificationsLPr(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        this.mRestoredIntentFilterVerifications.clear();
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_DOMAIN_VERIFICATION)) {
                        IntentFilterVerificationInfo ivi = new IntentFilterVerificationInfo(parser);
                        String pkgName = ivi.getPackageName();
                        PackageSetting ps = (PackageSetting) this.mPackages.get(pkgName);
                        String str;
                        StringBuilder stringBuilder;
                        if (ps != null) {
                            ps.setIntentFilterVerificationInfo(ivi);
                            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Restored IVI for existing app ");
                                stringBuilder.append(pkgName);
                                stringBuilder.append(" status=");
                                stringBuilder.append(ivi.getStatusString());
                                Slog.d(str, stringBuilder.toString());
                            }
                        } else {
                            this.mRestoredIntentFilterVerifications.put(pkgName, ivi);
                            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Restored IVI for pending app ");
                                stringBuilder.append(pkgName);
                                stringBuilder.append(" status=");
                                stringBuilder.append(ivi.getStatusString());
                                Slog.d(str, stringBuilder.toString());
                            }
                        }
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown element under <all-intent-filter-verification>: ");
                        stringBuilder2.append(parser.getName());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    public void processRestoredPermissionGrantLPr(String pkgName, String permission, boolean isGranted, int restoredFlagSet, int userId) {
        this.mRuntimePermissionsPersistence.rememberRestoredUserGrantLPr(pkgName, permission, isGranted, restoredFlagSet, userId);
    }

    void writeDefaultAppsLPr(XmlSerializer serializer, int userId) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, TAG_DEFAULT_APPS);
        String defaultBrowser = (String) this.mDefaultBrowserApp.get(userId);
        if (!TextUtils.isEmpty(defaultBrowser)) {
            serializer.startTag(null, TAG_DEFAULT_BROWSER);
            serializer.attribute(null, "packageName", defaultBrowser);
            serializer.endTag(null, TAG_DEFAULT_BROWSER);
        }
        String defaultDialer = (String) this.mDefaultDialerApp.get(userId);
        if (!TextUtils.isEmpty(defaultDialer)) {
            serializer.startTag(null, TAG_DEFAULT_DIALER);
            serializer.attribute(null, "packageName", defaultDialer);
            serializer.endTag(null, TAG_DEFAULT_DIALER);
        }
        serializer.endTag(null, TAG_DEFAULT_APPS);
    }

    void writeBlockUninstallPackagesLPr(XmlSerializer serializer, int userId) throws IOException {
        ArraySet<String> packages = (ArraySet) this.mBlockUninstallPackages.get(userId);
        if (packages != null) {
            serializer.startTag(null, TAG_BLOCK_UNINSTALL_PACKAGES);
            for (int i = 0; i < packages.size(); i++) {
                serializer.startTag(null, TAG_BLOCK_UNINSTALL);
                serializer.attribute(null, "packageName", (String) packages.valueAt(i));
                serializer.endTag(null, TAG_BLOCK_UNINSTALL);
            }
            serializer.endTag(null, TAG_BLOCK_UNINSTALL_PACKAGES);
        }
    }

    void writePackageRestrictionsLPr(int userId) {
        String str;
        StringBuilder stringBuilder;
        int i = userId;
        long startTime = SystemClock.uptimeMillis();
        File userPackagesStateFile = getUserPackagesStateFile(userId);
        File backupFile = getUserPackagesStateBackupFile(userId);
        new File(userPackagesStateFile.getParent()).mkdirs();
        if (userPackagesStateFile.exists()) {
            if (backupFile.exists()) {
                userPackagesStateFile.delete();
                Slog.w("PackageManager", "Preserving older stopped packages backup");
            } else if (!userPackagesStateFile.renameTo(backupFile)) {
                Slog.wtf("PackageManager", "Unable to backup user packages state file, current changes will be lost at reboot");
                return;
            }
        }
        try {
            FileOutputStream fstr = new FileOutputStream(userPackagesStateFile);
            BufferedOutputStream str2 = new BufferedOutputStream(fstr);
            FastXmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(str2, StandardCharsets.UTF_8.name());
            String str3 = null;
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, TAG_PACKAGE_RESTRICTIONS);
            for (PackageSetting pkg : this.mPackages.values()) {
                Iterator it;
                PackageUserState ustate = pkg.readUserState(i);
                serializer.startTag(str3, "pkg");
                serializer.attribute(str3, ATTR_NAME, pkg.name);
                if (ustate.ceDataInode != 0) {
                    XmlUtils.writeLongAttribute(serializer, ATTR_CE_DATA_INODE, ustate.ceDataInode);
                }
                if (!ustate.installed) {
                    serializer.attribute(null, ATTR_INSTALLED, "false");
                }
                if (ustate.stopped) {
                    serializer.attribute(null, ATTR_STOPPED, "true");
                }
                if (ustate.notLaunched) {
                    serializer.attribute(null, ATTR_NOT_LAUNCHED, "true");
                }
                if (ustate.hidden) {
                    serializer.attribute(null, ATTR_HIDDEN, "true");
                }
                if (ustate.suspended) {
                    serializer.attribute(null, ATTR_SUSPENDED, "true");
                    if (ustate.suspendingPackage != null) {
                        serializer.attribute(null, ATTR_SUSPENDING_PACKAGE, ustate.suspendingPackage);
                    }
                    if (ustate.dialogMessage != null) {
                        serializer.attribute(null, ATTR_SUSPEND_DIALOG_MESSAGE, ustate.dialogMessage);
                    }
                    if (ustate.suspendedAppExtras != null) {
                        serializer.startTag(null, TAG_SUSPENDED_APP_EXTRAS);
                        try {
                            ustate.suspendedAppExtras.saveToXml(serializer);
                        } catch (XmlPullParserException xmle) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Exception while trying to write suspendedAppExtras for ");
                            stringBuilder.append(pkg);
                            stringBuilder.append(". Will be lost on reboot");
                            Slog.wtf(str, stringBuilder.toString(), xmle);
                        }
                        serializer.endTag(null, TAG_SUSPENDED_APP_EXTRAS);
                    }
                    if (ustate.suspendedLauncherExtras != null) {
                        serializer.startTag(null, TAG_SUSPENDED_LAUNCHER_EXTRAS);
                        try {
                            ustate.suspendedLauncherExtras.saveToXml(serializer);
                        } catch (XmlPullParserException xmle2) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Exception while trying to write suspendedLauncherExtras for ");
                            stringBuilder.append(pkg);
                            stringBuilder.append(". Will be lost on reboot");
                            Slog.wtf(str, stringBuilder.toString(), xmle2);
                        }
                        serializer.endTag(null, TAG_SUSPENDED_LAUNCHER_EXTRAS);
                    }
                }
                if (ustate.instantApp) {
                    serializer.attribute(null, ATTR_INSTANT_APP, "true");
                }
                if (ustate.virtualPreload) {
                    serializer.attribute(null, ATTR_VIRTUAL_PRELOAD, "true");
                }
                if (ustate.enabled != 0) {
                    serializer.attribute(null, ATTR_ENABLED, Integer.toString(ustate.enabled));
                    if (ustate.lastDisableAppCaller != null) {
                        serializer.attribute(null, ATTR_ENABLED_CALLER, ustate.lastDisableAppCaller);
                    }
                }
                if (ustate.domainVerificationStatus != 0) {
                    XmlUtils.writeIntAttribute(serializer, ATTR_DOMAIN_VERIFICATON_STATE, ustate.domainVerificationStatus);
                }
                if (ustate.appLinkGeneration != 0) {
                    XmlUtils.writeIntAttribute(serializer, ATTR_APP_LINK_GENERATION, ustate.appLinkGeneration);
                }
                if (ustate.installReason != 0) {
                    serializer.attribute(null, ATTR_INSTALL_REASON, Integer.toString(ustate.installReason));
                }
                if (ustate.harmfulAppWarning != null) {
                    serializer.attribute(null, ATTR_HARMFUL_APP_WARNING, ustate.harmfulAppWarning);
                }
                if (!ArrayUtils.isEmpty(ustate.enabledComponents)) {
                    serializer.startTag(null, TAG_ENABLED_COMPONENTS);
                    it = ustate.enabledComponents.iterator();
                    while (it.hasNext()) {
                        str = (String) it.next();
                        serializer.startTag(null, TAG_ITEM);
                        serializer.attribute(null, ATTR_NAME, str);
                        serializer.endTag(null, TAG_ITEM);
                    }
                    serializer.endTag(null, TAG_ENABLED_COMPONENTS);
                }
                if (!ArrayUtils.isEmpty(ustate.disabledComponents)) {
                    serializer.startTag(null, TAG_DISABLED_COMPONENTS);
                    it = ustate.disabledComponents.iterator();
                    while (it.hasNext()) {
                        str = (String) it.next();
                        serializer.startTag(null, TAG_ITEM);
                        serializer.attribute(null, ATTR_NAME, str);
                        serializer.endTag(null, TAG_ITEM);
                    }
                    serializer.endTag(null, TAG_DISABLED_COMPONENTS);
                }
                serializer.endTag(null, "pkg");
                str3 = null;
            }
            writePreferredActivitiesLPr(serializer, i, true);
            writePersistentPreferredActivitiesLPr(serializer, i);
            writeCrossProfileIntentFiltersLPr(serializer, i);
            writeDefaultAppsLPr(serializer, i);
            writeBlockUninstallPackagesLPr(serializer, i);
            serializer.endTag(null, TAG_PACKAGE_RESTRICTIONS);
            serializer.endDocument();
            str2.flush();
            FileUtils.sync(fstr);
            str2.close();
            backupFile.delete();
            FileUtils.setPermissions(userPackagesStateFile.toString(), 432, -1, -1);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("package-user-");
            stringBuilder2.append(i);
            EventLogTags.writeCommitSysConfigFile(stringBuilder2.toString(), SystemClock.uptimeMillis() - startTime);
        } catch (IOException e) {
            Slog.wtf("PackageManager", "Unable to write package manager user packages state,  current changes will be lost at reboot", e);
            if (userPackagesStateFile.exists() && !userPackagesStateFile.delete()) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failed to clean up mangled file: ");
                stringBuilder3.append(this.mStoppedPackagesFilename);
                Log.i("PackageManager", stringBuilder3.toString());
            }
        }
    }

    void readInstallPermissionsLPr(XmlPullParser parser, PermissionsState permissionsState) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            boolean granted = true;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        BasePermission bp = this.mPermissions.getPermission(name);
                        if (bp == null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown permission: ");
                            stringBuilder.append(name);
                            Slog.w("PackageManager", stringBuilder.toString());
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            String grantedStr = parser.getAttributeValue(null, ATTR_GRANTED);
                            int flags = 0;
                            if (!(grantedStr == null || Boolean.parseBoolean(grantedStr))) {
                                granted = false;
                            }
                            String flagsStr = parser.getAttributeValue(null, ATTR_FLAGS);
                            if (flagsStr != null) {
                                flags = Integer.parseInt(flagsStr, 16);
                            }
                            StringBuilder stringBuilder2;
                            if (granted) {
                                if (permissionsState.grantInstallPermission(bp) == -1) {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Permission already added: ");
                                    stringBuilder2.append(name);
                                    Slog.w("PackageManager", stringBuilder2.toString());
                                    XmlUtils.skipCurrentTag(parser);
                                } else {
                                    permissionsState.updatePermissionFlags(bp, -1, 255, flags);
                                }
                            } else if (permissionsState.revokeInstallPermission(bp) == -1) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Permission already added: ");
                                stringBuilder2.append(name);
                                Slog.w("PackageManager", stringBuilder2.toString());
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                permissionsState.updatePermissionFlags(bp, -1, 255, flags);
                            }
                        }
                    } else {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Unknown element under <permissions>: ");
                        stringBuilder3.append(parser.getName());
                        Slog.w("PackageManager", stringBuilder3.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    void writePermissionsLPr(XmlSerializer serializer, List<PermissionState> permissionStates) throws IOException {
        if (!permissionStates.isEmpty()) {
            serializer.startTag(null, TAG_PERMISSIONS);
            for (PermissionState permissionState : permissionStates) {
                serializer.startTag(null, TAG_ITEM);
                serializer.attribute(null, ATTR_NAME, permissionState.getName());
                serializer.attribute(null, ATTR_GRANTED, String.valueOf(permissionState.isGranted()));
                serializer.attribute(null, ATTR_FLAGS, Integer.toHexString(permissionState.getFlags()));
                serializer.endTag(null, TAG_ITEM);
            }
            serializer.endTag(null, TAG_PERMISSIONS);
        }
    }

    void writeChildPackagesLPw(XmlSerializer serializer, List<String> childPackageNames) throws IOException {
        if (childPackageNames != null) {
            int childCount = childPackageNames.size();
            for (int i = 0; i < childCount; i++) {
                String childPackageName = (String) childPackageNames.get(i);
                serializer.startTag(null, TAG_CHILD_PACKAGE);
                serializer.attribute(null, ATTR_NAME, childPackageName);
                serializer.endTag(null, TAG_CHILD_PACKAGE);
            }
        }
    }

    void readUsesStaticLibLPw(XmlPullParser parser, PackageSetting outPs) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    String libName = parser.getAttributeValue(null, ATTR_NAME);
                    long libVersion = -1;
                    try {
                        libVersion = Long.parseLong(parser.getAttributeValue(null, "version"));
                    } catch (NumberFormatException e) {
                    }
                    if (libName != null && libVersion >= 0) {
                        outPs.usesStaticLibraries = (String[]) ArrayUtils.appendElement(String.class, outPs.usesStaticLibraries, libName);
                        outPs.usesStaticLibrariesVersions = ArrayUtils.appendLong(outPs.usesStaticLibrariesVersions, libVersion);
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    void writeUsesStaticLibLPw(XmlSerializer serializer, String[] usesStaticLibraries, long[] usesStaticLibraryVersions) throws IOException {
        if (!ArrayUtils.isEmpty(usesStaticLibraries) && !ArrayUtils.isEmpty(usesStaticLibraryVersions) && usesStaticLibraries.length == usesStaticLibraryVersions.length) {
            int libCount = usesStaticLibraries.length;
            for (int i = 0; i < libCount; i++) {
                String libName = usesStaticLibraries[i];
                long libVersion = usesStaticLibraryVersions[i];
                serializer.startTag(null, TAG_USES_STATIC_LIB);
                serializer.attribute(null, ATTR_NAME, libName);
                serializer.attribute(null, "version", Long.toString(libVersion));
                serializer.endTag(null, TAG_USES_STATIC_LIB);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x00ae A:{Catch:{ XmlPullParserException -> 0x0087, IOException -> 0x0084 }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x00a0 A:{Catch:{ XmlPullParserException -> 0x0087, IOException -> 0x0084 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void readStoppedLPw() {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        int type;
        FileInputStream str = null;
        if (this.mBackupStoppedPackagesFilename.exists()) {
            try {
                str = new FileInputStream(this.mBackupStoppedPackagesFilename);
                this.mReadMessages.append("Reading from backup stopped packages file\n");
                PackageManagerService.reportSettingsProblem(4, "Need to read from backup stopped packages file");
                if (this.mSettingsFilename.exists()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cleaning up stopped packages file ");
                    stringBuilder.append(this.mStoppedPackagesFilename);
                    Slog.w("PackageManager", stringBuilder.toString());
                    this.mStoppedPackagesFilename.delete();
                }
            } catch (IOException e) {
            }
        }
        if (str == null) {
            try {
                if (this.mStoppedPackagesFilename.exists()) {
                    str = new FileInputStream(this.mStoppedPackagesFilename);
                } else {
                    this.mReadMessages.append("No stopped packages file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No stopped packages file file; assuming all started");
                    for (PackageSetting pkg : this.mPackages.values()) {
                        pkg.setStopped(false, 0);
                        pkg.setNotLaunched(false, 0);
                    }
                    return;
                }
            } catch (XmlPullParserException e2) {
                stringBuilder = this.mReadMessages;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error reading: ");
                stringBuilder2.append(e2.toString());
                stringBuilder.append(stringBuilder2.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error reading stopped packages: ");
                stringBuilder.append(e2);
                PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
            } catch (IOException e22) {
                stringBuilder = this.mReadMessages;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error reading: ");
                stringBuilder2.append(e22.toString());
                stringBuilder.append(stringBuilder2.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error reading settings: ");
                stringBuilder.append(e22);
                PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e22);
            }
        }
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(str, null);
        while (true) {
            int next = parser.next();
            type = next;
            if (next == 2 || type == 1) {
                if (type == 2) {
                    this.mReadMessages.append("No start tag found in stopped packages file\n");
                    PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager stopped packages");
                    return;
                }
                next = parser.getDepth();
                while (true) {
                    int next2 = parser.next();
                    type = next2;
                    if (next2 == 1 || (type == 3 && parser.getDepth() <= next)) {
                        str.close();
                    } else if (type != 3) {
                        if (type != 4) {
                            if (parser.getName().equals("pkg")) {
                                String name = parser.getAttributeValue(null, ATTR_NAME);
                                PackageSetting ps = (PackageSetting) this.mPackages.get(name);
                                if (ps != null) {
                                    ps.setStopped(true, 0);
                                    if ("1".equals(parser.getAttributeValue(null, ATTR_NOT_LAUNCHED))) {
                                        ps.setNotLaunched(true, 0);
                                    }
                                } else {
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("No package known for stopped package ");
                                    stringBuilder3.append(name);
                                    Slog.w("PackageManager", stringBuilder3.toString());
                                }
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Unknown element under <stopped-packages>: ");
                                stringBuilder4.append(parser.getName());
                                Slog.w("PackageManager", stringBuilder4.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                }
                str.close();
                return;
            }
        }
        if (type == 2) {
        }
    }

    void writeLPr() {
        long startTime = SystemClock.uptimeMillis();
        if (this.mSettingsFilename.exists()) {
            if (this.mBackupSettingsFilename.exists()) {
                this.mSettingsFilename.delete();
                Slog.w("PackageManager", "Preserving older settings backup");
            } else if (!this.mSettingsFilename.renameTo(this.mBackupSettingsFilename)) {
                Slog.wtf("PackageManager", "Unable to backup package manager settings,  current changes will be lost at reboot");
                return;
            }
        }
        this.mPastSignatures.clear();
        try {
            int i;
            Iterator it;
            FileOutputStream fstr = new FileOutputStream(this.mSettingsFilename);
            BufferedOutputStream str = new BufferedOutputStream(fstr);
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(str, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "packages");
            int i2 = 0;
            for (i = 0; i < this.mVersion.size(); i++) {
                String volumeUuid = (String) this.mVersion.keyAt(i);
                VersionInfo ver = (VersionInfo) this.mVersion.valueAt(i);
                serializer.startTag(null, "version");
                XmlUtils.writeStringAttribute(serializer, ATTR_VOLUME_UUID, volumeUuid);
                XmlUtils.writeIntAttribute(serializer, ATTR_SDK_VERSION, ver.sdkVersion);
                XmlUtils.writeIntAttribute(serializer, ATTR_DATABASE_VERSION, ver.databaseVersion);
                XmlUtils.writeStringAttribute(serializer, ATTR_FINGERPRINT, ver.fingerprint);
                XmlUtils.writeStringAttribute(serializer, ATTR_HWFINGERPRINT, ver.hwFingerprint);
                serializer.endTag(null, "version");
            }
            if (this.mVerifierDeviceIdentity != null) {
                serializer.startTag(null, "verifier");
                serializer.attribute(null, "device", this.mVerifierDeviceIdentity.toString());
                serializer.endTag(null, "verifier");
            }
            if (this.mReadExternalStorageEnforced != null) {
                serializer.startTag(null, TAG_READ_EXTERNAL_STORAGE);
                serializer.attribute(null, ATTR_ENFORCEMENT, this.mReadExternalStorageEnforced.booleanValue() ? "1" : "0");
                serializer.endTag(null, TAG_READ_EXTERNAL_STORAGE);
            }
            serializer.startTag(null, "permission-trees");
            this.mPermissions.writePermissionTrees(serializer);
            serializer.endTag(null, "permission-trees");
            serializer.startTag(null, "permissions");
            this.mPermissions.writePermissions(serializer);
            serializer.endTag(null, "permissions");
            for (PackageSetting pkg : this.mPackages.values()) {
                writePackageLPr(serializer, pkg);
            }
            for (PackageSetting pkg2 : this.mDisabledSysPackages.values()) {
                writeDisabledSysPackageLPr(serializer, pkg2);
            }
            for (SharedUserSetting usr : this.mSharedUsers.values()) {
                serializer.startTag(null, TAG_SHARED_USER);
                serializer.attribute(null, ATTR_NAME, usr.name);
                serializer.attribute(null, "userId", Integer.toString(usr.userId));
                usr.signatures.writeXml(serializer, "sigs", this.mPastSignatures);
                writePermissionsLPr(serializer, usr.getPermissionsState().getInstallPermissionStates());
                serializer.endTag(null, TAG_SHARED_USER);
            }
            if (this.mPackagesToBeCleaned.size() > 0) {
                it = this.mPackagesToBeCleaned.iterator();
                while (it.hasNext()) {
                    PackageCleanItem item = (PackageCleanItem) it.next();
                    String userStr = Integer.toString(item.userId);
                    serializer.startTag(null, "cleaning-package");
                    serializer.attribute(null, ATTR_NAME, item.packageName);
                    serializer.attribute(null, ATTR_CODE, item.andCode ? "true" : "false");
                    serializer.attribute(null, ATTR_USER, userStr);
                    serializer.endTag(null, "cleaning-package");
                }
            }
            if (this.mRenamedPackages.size() > 0) {
                for (Entry<String, String> e : this.mRenamedPackages.entrySet()) {
                    serializer.startTag(null, "renamed-package");
                    serializer.attribute(null, "new", (String) e.getKey());
                    serializer.attribute(null, "old", (String) e.getValue());
                    serializer.endTag(null, "renamed-package");
                }
            }
            i = this.mRestoredIntentFilterVerifications.size();
            if (i > 0) {
                if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                    Slog.i(TAG, "Writing restored-ivi entries to packages.xml");
                }
                serializer.startTag(null, "restored-ivi");
                while (i2 < i) {
                    writeDomainVerificationsLPr(serializer, (IntentFilterVerificationInfo) this.mRestoredIntentFilterVerifications.valueAt(i2));
                    i2++;
                }
                serializer.endTag(null, "restored-ivi");
            } else if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.i(TAG, "  no restored IVI entries to write");
            }
            this.mKeySetManagerService.writeKeySetManagerServiceLPr(serializer);
            serializer.endTag(null, "packages");
            serializer.endDocument();
            str.flush();
            FileUtils.sync(fstr);
            str.close();
            this.mBackupSettingsFilename.delete();
            FileUtils.setPermissions(this.mSettingsFilename.toString(), 432, -1, -1);
            writeKernelMappingLPr();
            writePackageListLPr();
            writeAllUsersPackageRestrictionsLPr();
            writeAllRuntimePermissionsLPr();
            EventLogTags.writeCommitSysConfigFile("package", SystemClock.uptimeMillis() - startTime);
        } catch (IOException e2) {
            Slog.wtf("PackageManager", "Unable to write package manager settings, current changes will be lost at reboot", e2);
            if (this.mSettingsFilename.exists() && !this.mSettingsFilename.delete()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to clean up mangled file: ");
                stringBuilder.append(this.mSettingsFilename);
                Slog.wtf("PackageManager", stringBuilder.toString());
            }
        }
    }

    private void writeKernelRemoveUserLPr(int userId) {
        if (this.mKernelMappingFilename != null) {
            writeIntToFile(new File(this.mKernelMappingFilename, "remove_userid"), userId);
        }
    }

    void writeKernelMappingLPr() {
        if (this.mKernelMappingFilename != null) {
            int i;
            String[] known = this.mKernelMappingFilename.list();
            ArraySet<String> knownSet = new ArraySet(known.length);
            int i2 = 0;
            for (String name : known) {
                knownSet.add(name);
            }
            for (PackageSetting ps : this.mPackages.values()) {
                knownSet.remove(ps.name);
                writeKernelMappingLPr(ps);
            }
            while (true) {
                i = i2;
                if (i < knownSet.size()) {
                    String name2 = (String) knownSet.valueAt(i);
                    this.mKernelMapping.remove(name2);
                    new File(this.mKernelMappingFilename, name2).delete();
                    i2 = i + 1;
                } else {
                    return;
                }
            }
        }
    }

    void writeKernelMappingLPr(PackageSetting ps) {
        if (this.mKernelMappingFilename != null && ps != null && ps.name != null) {
            KernelPackageState cur = (KernelPackageState) this.mKernelMapping.get(ps.name);
            int i = 0;
            boolean userIdsChanged = true;
            boolean firstTime = cur == null;
            int[] excludedUserIds = ps.getNotInstalledUserIds();
            if (!firstTime && Arrays.equals(excludedUserIds, cur.excludedUserIds)) {
                userIdsChanged = false;
            }
            File dir = new File(this.mKernelMappingFilename, ps.name);
            if (firstTime) {
                dir.mkdir();
                cur = new KernelPackageState();
                this.mKernelMapping.put(ps.name, cur);
            }
            if (cur.appId != ps.appId) {
                writeIntToFile(new File(dir, "appid"), ps.appId);
            }
            if (userIdsChanged) {
                int i2 = 0;
                while (i2 < excludedUserIds.length) {
                    if (cur.excludedUserIds == null || !ArrayUtils.contains(cur.excludedUserIds, excludedUserIds[i2])) {
                        writeIntToFile(new File(dir, "excluded_userids"), excludedUserIds[i2]);
                    }
                    i2++;
                }
                if (cur.excludedUserIds != null) {
                    while (i < cur.excludedUserIds.length) {
                        if (!ArrayUtils.contains(excludedUserIds, cur.excludedUserIds[i])) {
                            writeIntToFile(new File(dir, "clear_userid"), cur.excludedUserIds[i]);
                        }
                        i++;
                    }
                }
                cur.excludedUserIds = excludedUserIds;
            }
        }
    }

    private void writeIntToFile(File file, int value) {
        try {
            FileUtils.bytesToFile(file.getAbsolutePath(), Integer.toString(value).getBytes(StandardCharsets.US_ASCII));
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't write ");
            stringBuilder.append(value);
            stringBuilder.append(" to ");
            stringBuilder.append(file.getAbsolutePath());
            Slog.w(str, stringBuilder.toString());
        }
    }

    void writePackageListLPr() {
        writePackageListLPr(-1);
    }

    void writePackageListLPr(int creatingUserId) {
        int i = creatingUserId;
        List<UserInfo> users = UserManagerService.getInstance().getUsers(true);
        int[] userIds = new int[users.size()];
        for (int i2 = 0; i2 < userIds.length; i2++) {
            userIds[i2] = ((UserInfo) users.get(i2)).id;
        }
        if (i != -1) {
            userIds = ArrayUtils.appendInt(userIds, i);
        }
        int[] userIds2 = userIds;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mPackageListFilename.getAbsolutePath());
        stringBuilder.append(".tmp");
        JournaledFile journal = new JournaledFile(this.mPackageListFilename, new File(stringBuilder.toString()));
        BufferedWriter writer = null;
        try {
            FileOutputStream fstr = new FileOutputStream(journal.chooseForWrite());
            writer = new BufferedWriter(new OutputStreamWriter(fstr, Charset.defaultCharset()));
            FileUtils.setPermissions(fstr.getFD(), 416, 1000, 1032);
            StringBuilder sb = new StringBuilder();
            for (PackageSetting pkg : this.mPackages.values()) {
                if (!(pkg.pkg == null || pkg.pkg.applicationInfo == null)) {
                    if (pkg.pkg.applicationInfo.dataDir != null) {
                        ApplicationInfo ai = pkg.pkg.applicationInfo;
                        String dataPath = ai.dataDir;
                        boolean isDebug = (ai.flags & 2) != 0;
                        int[] gids = pkg.getPermissionsState().computeGids(userIds2);
                        if (dataPath.indexOf(32) >= 0) {
                        } else {
                            sb.setLength(0);
                            sb.append(ai.packageName);
                            sb.append(" ");
                            sb.append(ai.uid);
                            sb.append(isDebug ? " 1 " : " 0 ");
                            sb.append(dataPath);
                            sb.append(" ");
                            sb.append(ai.seInfo);
                            sb.append(" ");
                            if (gids == null || gids.length <= 0) {
                                sb.append("none");
                            } else {
                                sb.append(gids[0]);
                                for (i = 1; i < gids.length; i++) {
                                    sb.append(",");
                                    sb.append(gids[i]);
                                }
                            }
                            sb.append("\n");
                            writer.append(sb);
                            i = creatingUserId;
                        }
                    }
                }
                if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg.name)) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Skipping ");
                    stringBuilder2.append(pkg);
                    stringBuilder2.append(" due to missing metadata");
                    Slog.w(str, stringBuilder2.toString());
                }
                i = creatingUserId;
            }
            writer.flush();
            FileUtils.sync(fstr);
            writer.close();
            journal.commit();
        } catch (Exception e) {
            Slog.wtf(TAG, "Failed to write packages.list", e);
            IoUtils.closeQuietly(writer);
            journal.rollback();
        }
    }

    void writeDisabledSysPackageLPr(XmlSerializer serializer, PackageSetting pkg) throws IOException {
        serializer.startTag(null, "updated-package");
        serializer.attribute(null, ATTR_NAME, pkg.name);
        if (pkg.realName != null) {
            serializer.attribute(null, "realName", pkg.realName);
        }
        serializer.attribute(null, "codePath", pkg.codePathString);
        serializer.attribute(null, "ft", Long.toHexString(pkg.timeStamp));
        serializer.attribute(null, "it", Long.toHexString(pkg.firstInstallTime));
        serializer.attribute(null, "ut", Long.toHexString(pkg.lastUpdateTime));
        serializer.attribute(null, "version", String.valueOf(pkg.versionCode));
        if (!pkg.resourcePathString.equals(pkg.codePathString)) {
            serializer.attribute(null, "resourcePath", pkg.resourcePathString);
        }
        if (pkg.legacyNativeLibraryPathString != null) {
            serializer.attribute(null, "nativeLibraryPath", pkg.legacyNativeLibraryPathString);
        }
        if (pkg.primaryCpuAbiString != null) {
            serializer.attribute(null, "primaryCpuAbi", pkg.primaryCpuAbiString);
        }
        if (pkg.secondaryCpuAbiString != null) {
            serializer.attribute(null, "secondaryCpuAbi", pkg.secondaryCpuAbiString);
        }
        if (pkg.cpuAbiOverrideString != null) {
            serializer.attribute(null, "cpuAbiOverride", pkg.cpuAbiOverrideString);
        }
        if (pkg.sharedUser == null) {
            serializer.attribute(null, "userId", Integer.toString(pkg.appId));
        } else {
            serializer.attribute(null, "sharedUserId", Integer.toString(pkg.appId));
        }
        if (pkg.parentPackageName != null) {
            serializer.attribute(null, "parentPackageName", pkg.parentPackageName);
        }
        writeChildPackagesLPw(serializer, pkg.childPackageNames);
        writeUsesStaticLibLPw(serializer, pkg.usesStaticLibraries, pkg.usesStaticLibrariesVersions);
        if (pkg.sharedUser == null) {
            writePermissionsLPr(serializer, pkg.getPermissionsState().getInstallPermissionStates());
        }
        serializer.endTag(null, "updated-package");
    }

    void writePackageLPr(XmlSerializer serializer, PackageSetting pkg) throws IOException {
        serializer.startTag(null, "package");
        serializer.attribute(null, ATTR_NAME, pkg.name);
        if (pkg.realName != null) {
            serializer.attribute(null, "realName", pkg.realName);
        }
        serializer.attribute(null, "codePath", pkg.codePathString);
        if (!pkg.resourcePathString.equals(pkg.codePathString)) {
            serializer.attribute(null, "resourcePath", pkg.resourcePathString);
        }
        if (pkg.legacyNativeLibraryPathString != null) {
            serializer.attribute(null, "nativeLibraryPath", pkg.legacyNativeLibraryPathString);
        }
        if (pkg.primaryCpuAbiString != null) {
            serializer.attribute(null, "primaryCpuAbi", pkg.primaryCpuAbiString);
        }
        if (pkg.secondaryCpuAbiString != null) {
            serializer.attribute(null, "secondaryCpuAbi", pkg.secondaryCpuAbiString);
        }
        if (pkg.cpuAbiOverrideString != null) {
            serializer.attribute(null, "cpuAbiOverride", pkg.cpuAbiOverrideString);
        }
        serializer.attribute(null, "publicFlags", Integer.toString(pkg.pkgFlags));
        serializer.attribute(null, "privateFlags", Integer.toString(pkg.pkgPrivateFlags));
        serializer.attribute(null, "ft", Long.toHexString(pkg.timeStamp));
        serializer.attribute(null, "it", Long.toHexString(pkg.firstInstallTime));
        serializer.attribute(null, "ut", Long.toHexString(pkg.lastUpdateTime));
        serializer.attribute(null, "version", String.valueOf(pkg.versionCode));
        if (pkg.sharedUser == null) {
            serializer.attribute(null, "userId", Integer.toString(pkg.appId));
        } else {
            serializer.attribute(null, "sharedUserId", Integer.toString(pkg.appId));
        }
        if (pkg.uidError) {
            serializer.attribute(null, "uidError", "true");
        }
        if (pkg.installerPackageName != null) {
            serializer.attribute(null, "installer", pkg.installerPackageName);
        }
        if (pkg.maxAspectRatio > 0.0f) {
            serializer.attribute(null, "maxAspectRatio", Float.toString(pkg.maxAspectRatio));
        }
        serializer.attribute(null, "appUseNotchMode", Integer.toString(pkg.appUseNotchMode));
        if (pkg.isOrphaned) {
            serializer.attribute(null, "isOrphaned", "true");
        }
        if (pkg.volumeUuid != null) {
            serializer.attribute(null, ATTR_VOLUME_UUID, pkg.volumeUuid);
        }
        if (pkg.categoryHint != -1) {
            serializer.attribute(null, "categoryHint", Integer.toString(pkg.categoryHint));
        }
        if (pkg.parentPackageName != null) {
            serializer.attribute(null, "parentPackageName", pkg.parentPackageName);
        }
        if (pkg.updateAvailable) {
            serializer.attribute(null, "updateAvailable", "true");
        }
        writeChildPackagesLPw(serializer, pkg.childPackageNames);
        writeUsesStaticLibLPw(serializer, pkg.usesStaticLibraries, pkg.usesStaticLibrariesVersions);
        if (pkg.pkg == null || !pkg.pkg.mRealSigningDetails.hasSignatures()) {
            pkg.signatures.writeXml(serializer, "sigs", this.mPastSignatures);
        } else {
            new PackageSignatures(pkg.pkg.mRealSigningDetails).writeXml(serializer, "sigs", this.mPastSignatures);
        }
        writePermissionsLPr(serializer, pkg.getPermissionsState().getInstallPermissionStates());
        writeSigningKeySetLPr(serializer, pkg.keySetData);
        writeUpgradeKeySetsLPr(serializer, pkg.keySetData);
        writeKeySetAliasesLPr(serializer, pkg.keySetData);
        writeDomainVerificationsLPr(serializer, pkg.verificationInfo);
        serializer.endTag(null, "package");
    }

    void writeSigningKeySetLPr(XmlSerializer serializer, PackageKeySetData data) throws IOException {
        serializer.startTag(null, "proper-signing-keyset");
        serializer.attribute(null, "identifier", Long.toString(data.getProperSigningKeySet()));
        serializer.endTag(null, "proper-signing-keyset");
    }

    void writeUpgradeKeySetsLPr(XmlSerializer serializer, PackageKeySetData data) throws IOException {
        if (data.isUsingUpgradeKeySets()) {
            for (long id : data.getUpgradeKeySets()) {
                serializer.startTag(null, "upgrade-keyset");
                serializer.attribute(null, "identifier", Long.toString(id));
                serializer.endTag(null, "upgrade-keyset");
            }
        }
    }

    void writeKeySetAliasesLPr(XmlSerializer serializer, PackageKeySetData data) throws IOException {
        for (Entry<String, Long> e : data.getAliases().entrySet()) {
            serializer.startTag(null, "defined-keyset");
            serializer.attribute(null, "alias", (String) e.getKey());
            serializer.attribute(null, "identifier", Long.toString(((Long) e.getValue()).longValue()));
            serializer.endTag(null, "defined-keyset");
        }
    }

    void writePermissionLPr(XmlSerializer serializer, BasePermission bp) throws IOException {
        bp.writeLPr(serializer);
    }

    void addPackageToCleanLPw(PackageCleanItem pkg) {
        if (!this.mPackagesToBeCleaned.contains(pkg)) {
            this.mPackagesToBeCleaned.add(pkg);
        }
    }

    boolean isPackageSettingsError() {
        return this.mIsPackageSettingsError || "1".equals(SystemProperties.get(KEY_PACKAGE_SETTINS_ERROR, "0"));
    }

    static void setPackageSettingsError() {
        SystemProperties.set(KEY_PACKAGE_SETTINS_ERROR, "1");
    }

    static void resetPackageSettingsError() {
        SystemProperties.set(KEY_PACKAGE_SETTINS_ERROR, "0");
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:0x00d6 A:{Catch:{ XmlPullParserException -> 0x00a3, IOException -> 0x00a0, Exception -> 0x009d }} */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00c1 A:{Catch:{ XmlPullParserException -> 0x00a3, IOException -> 0x00a0, Exception -> 0x009d }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean readLPw(List<UserInfo> users) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3;
        IOException e;
        int type;
        BufferedInputStream str = null;
        int i = 4;
        if (this.mBackupSettingsFilename.exists()) {
            try {
                str = new BufferedInputStream(new FileInputStream(this.mBackupSettingsFilename), DumpState.DUMP_COMPILER_STATS);
                this.mReadMessages.append("Reading from backup settings file\n");
                PackageManagerService.reportSettingsProblem(4, "Need to read from backup settings file");
                if (this.mSettingsFilename.exists()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cleaning up settings file ");
                    stringBuilder.append(this.mSettingsFilename);
                    Slog.w("PackageManager", stringBuilder.toString());
                    this.mSettingsFilename.delete();
                }
            } catch (IOException e2) {
            }
        }
        this.mPendingPackages.clear();
        this.mPastSignatures.clear();
        this.mKeySetRefs.clear();
        this.mInstallerPackages.clear();
        if (str == null) {
            try {
                if (this.mSettingsFilename.exists()) {
                    str = new BufferedInputStream(new FileInputStream(this.mSettingsFilename), DumpState.DUMP_COMPILER_STATS);
                } else {
                    this.mReadMessages.append("No settings file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No settings file; creating initial state");
                    findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL).forceCurrent();
                    findOrCreateVersion("primary_physical").forceCurrent();
                    return false;
                }
            } catch (XmlPullParserException e3) {
                this.mIsPackageSettingsError = true;
                stringBuilder2 = this.mReadMessages;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error reading: ");
                stringBuilder3.append(e3.toString());
                stringBuilder2.append(stringBuilder3.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error reading settings: ");
                stringBuilder2.append(e3);
                PackageManagerService.reportSettingsProblem(6, stringBuilder2.toString());
                Slog.e("PackageManager", "Error reading package manager settings", e3);
                HwBootFail.brokenFileBootFail(83886084, "/data/system/packages.xml", new Throwable());
                this.mPendingPackages.clear();
                this.mPastSignatures.clear();
                this.mKeySetRefs.clear();
                this.mUserIds.clear();
                this.mSharedUsers.clear();
                addSharedUserLPw("android.uid.system", 1000, 1, 8);
                addSharedUserLPw("android.uid.phone", NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE, 1, 8);
                addSharedUserLPw("android.uid.log", 1007, 1, 8);
                addSharedUserLPw("android.uid.nfc", UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE, 1, 8);
                addSharedUserLPw("com.nxp.uid.nfceeapi", 1054, 1, 8);
                addSharedUserLPw("android.uid.bluetooth", 1002, 1, 8);
                addSharedUserLPw("android.uid.shell", IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME, 1, 8);
                addSharedUserLPw("android.uid.hbs", 5508, 1, 8);
                findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL);
                findOrCreateVersion("primary_physical");
                return false;
            } catch (IOException e4) {
                this.mIsPackageSettingsError = true;
                stringBuilder2 = this.mReadMessages;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error reading: ");
                stringBuilder3.append(e4.toString());
                stringBuilder2.append(stringBuilder3.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error reading settings: ");
                stringBuilder2.append(e4);
                PackageManagerService.reportSettingsProblem(6, stringBuilder2.toString());
                Slog.wtf("PackageManager", "Error reading package manager settings", e4);
            } catch (Exception e42) {
                this.mIsPackageSettingsError = true;
                stringBuilder2 = this.mReadMessages;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error reading: ");
                stringBuilder3.append(e42.toString());
                stringBuilder2.append(stringBuilder3.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error reading settings: ");
                stringBuilder2.append(e42);
                PackageManagerService.reportSettingsProblem(6, stringBuilder2.toString());
                Log.wtf("PackageManager", "Error reading package manager settings", e42);
            }
        }
        IOException parser = Xml.newPullParser();
        parser.setInput(str, StandardCharsets.UTF_8.name());
        while (true) {
            e42 = parser.next();
            type = e42;
            if (e42 == 2 || type == 1) {
                if (type == 2) {
                    this.mReadMessages.append("No start tag found in settings file\n");
                    PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager settings");
                    Slog.wtf("PackageManager", "No start tag found in package manager settings");
                    return false;
                }
                e42 = parser.getDepth();
                while (true) {
                    int outerDepth = e42;
                    e42 = parser.next();
                    type = e42;
                    if (e42 == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        str.close();
                    } else {
                        if (type != 3) {
                            if (type != i) {
                                IOException tagName = parser.getName();
                                if (tagName.equals("package") != null) {
                                    readPackageLPw(parser);
                                } else if (tagName.equals("permissions") != null) {
                                    this.mPermissions.readPermissions(parser);
                                } else if (tagName.equals("permission-trees") != null) {
                                    this.mPermissions.readPermissionTrees(parser);
                                } else if (tagName.equals(TAG_SHARED_USER) != null) {
                                    readSharedUserLPw(parser);
                                } else if (tagName.equals("preferred-packages") == null) {
                                    VersionInfo external;
                                    if (tagName.equals("preferred-activities") != null) {
                                        readPreferredActivitiesLPw(parser, 0);
                                    } else if (tagName.equals(TAG_PERSISTENT_PREFERRED_ACTIVITIES) != null) {
                                        readPersistentPreferredActivitiesLPw(parser, 0);
                                    } else if (tagName.equals(TAG_CROSS_PROFILE_INTENT_FILTERS) != null) {
                                        readCrossProfileIntentFiltersLPw(parser, 0);
                                    } else if (tagName.equals(TAG_DEFAULT_BROWSER) != null) {
                                        readDefaultAppsLPw(parser, 0);
                                    } else if (tagName.equals("updated-package") != null) {
                                        readDisabledSysPackageLPw(parser);
                                    } else if (tagName.equals("cleaning-package") != null) {
                                        IOException name = parser.getAttributeValue(null, ATTR_NAME);
                                        IOException userStr = parser.getAttributeValue(null, ATTR_USER);
                                        IOException codeStr = parser.getAttributeValue(null, ATTR_CODE);
                                        if (name != null) {
                                            int userId = 0;
                                            boolean andCode = true;
                                            if (userStr != null) {
                                                try {
                                                    userId = Integer.parseInt(userStr);
                                                } catch (NumberFormatException e5) {
                                                }
                                            }
                                            if (codeStr != null) {
                                                e42 = Boolean.parseBoolean(codeStr);
                                                andCode = e42;
                                            } else {
                                                e42 = andCode;
                                            }
                                            addPackageToCleanLPw(new PackageCleanItem(userId, name, e42));
                                        }
                                    } else if (tagName.equals("renamed-package") != null) {
                                        e42 = parser.getAttributeValue(null, "new");
                                        String oname = parser.getAttributeValue(null, "old");
                                        if (!(e42 == null || oname == null)) {
                                            this.mRenamedPackages.put(e42, oname);
                                        }
                                    } else if (tagName.equals("restored-ivi") != null) {
                                        readRestoredIntentFilterVerifications(parser);
                                    } else if (tagName.equals("last-platform-version") != null) {
                                        e42 = findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL);
                                        external = findOrCreateVersion("primary_physical");
                                        e42.sdkVersion = XmlUtils.readIntAttribute(parser, "internal", 0);
                                        external.sdkVersion = XmlUtils.readIntAttribute(parser, "external", 0);
                                        String readStringAttribute = XmlUtils.readStringAttribute(parser, ATTR_FINGERPRINT);
                                        external.fingerprint = readStringAttribute;
                                        e42.fingerprint = readStringAttribute;
                                        readStringAttribute = XmlUtils.readStringAttribute(parser, ATTR_HWFINGERPRINT);
                                        external.hwFingerprint = readStringAttribute;
                                        e42.hwFingerprint = readStringAttribute;
                                    } else if (tagName.equals("database-version") != null) {
                                        e42 = findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL);
                                        external = findOrCreateVersion("primary_physical");
                                        e42.databaseVersion = XmlUtils.readIntAttribute(parser, "internal", 0);
                                        external.databaseVersion = XmlUtils.readIntAttribute(parser, "external", 0);
                                    } else if (tagName.equals("verifier") != null) {
                                        try {
                                            this.mVerifierDeviceIdentity = VerifierDeviceIdentity.parse(parser.getAttributeValue(null, "device"));
                                        } catch (IllegalArgumentException e422) {
                                            StringBuilder stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("Discard invalid verifier device id: ");
                                            stringBuilder4.append(e422.getMessage());
                                            Slog.w("PackageManager", stringBuilder4.toString());
                                        }
                                    } else if (TAG_READ_EXTERNAL_STORAGE.equals(tagName) != null) {
                                        this.mReadExternalStorageEnforced = "1".equals(parser.getAttributeValue(null, ATTR_ENFORCEMENT)) ? Boolean.TRUE : Boolean.FALSE;
                                    } else if (tagName.equals("keyset-settings") != null) {
                                        this.mKeySetManagerService.readKeySetsLPw(parser, this.mKeySetRefs);
                                    } else if ("version".equals(tagName) != null) {
                                        external = findOrCreateVersion(XmlUtils.readStringAttribute(parser, ATTR_VOLUME_UUID));
                                        external.sdkVersion = XmlUtils.readIntAttribute(parser, ATTR_SDK_VERSION);
                                        external.databaseVersion = XmlUtils.readIntAttribute(parser, ATTR_DATABASE_VERSION);
                                        external.fingerprint = XmlUtils.readStringAttribute(parser, ATTR_FINGERPRINT);
                                        external.hwFingerprint = XmlUtils.readStringAttribute(parser, ATTR_HWFINGERPRINT);
                                    } else {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Unknown element under <packages>: ");
                                        stringBuilder2.append(parser.getName());
                                        Slog.w("PackageManager", stringBuilder2.toString());
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                }
                            }
                        }
                        e422 = outerDepth;
                        i = 4;
                    }
                }
                str.close();
                i = this.mPendingPackages.size();
                for (int i2 = 0; i2 < i; i2++) {
                    PackageSetting p = (PackageSetting) this.mPendingPackages.get(i2);
                    type = p.getSharedUserId();
                    SharedUserSetting idObj = getUserIdLPr(type);
                    String msg;
                    if (idObj instanceof SharedUserSetting) {
                        SharedUserSetting sharedUser = idObj;
                        p.sharedUser = sharedUser;
                        p.appId = sharedUser.userId;
                        addPackageSettingLPw(p, sharedUser);
                    } else if (idObj != null) {
                        msg = new StringBuilder();
                        msg.append("Bad package setting: package ");
                        msg.append(p.name);
                        msg.append(" has shared uid ");
                        msg.append(type);
                        msg.append(" that is not a shared uid\n");
                        msg = msg.toString();
                        this.mReadMessages.append(msg);
                        PackageManagerService.reportSettingsProblem(6, msg);
                    } else {
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Bad package setting: package ");
                        stringBuilder5.append(p.name);
                        stringBuilder5.append(" has shared uid ");
                        stringBuilder5.append(type);
                        stringBuilder5.append(" that is not defined\n");
                        msg = stringBuilder5.toString();
                        this.mReadMessages.append(msg);
                        PackageManagerService.reportSettingsProblem(6, msg);
                    }
                }
                this.mPendingPackages.clear();
                if (this.mBackupStoppedPackagesFilename.exists() || this.mStoppedPackagesFilename.exists()) {
                    readStoppedLPw();
                    this.mBackupStoppedPackagesFilename.delete();
                    this.mStoppedPackagesFilename.delete();
                    writePackageRestrictionsLPr(0);
                } else {
                    for (UserInfo user : users) {
                        readPackageRestrictionsLPr(user.id);
                    }
                }
                try {
                    for (UserInfo user2 : users) {
                        this.mRuntimePermissionsPersistence.readStateForUserSyncLPr(user2.id);
                    }
                } catch (IllegalStateException e6) {
                    HwBootFail.brokenFileBootFail(83886085, "/data/system/users/0/runtime-permissions.xml", new Throwable());
                    Log.wtf("PackageManager", "Error reading state for user", e6);
                }
                for (PackageSetting disabledPs : this.mDisabledSysPackages.values()) {
                    Object id = getUserIdLPr(disabledPs.appId);
                    if (id != null && (id instanceof SharedUserSetting)) {
                        disabledPs.sharedUser = (SharedUserSetting) id;
                    }
                }
                stringBuilder = this.mReadMessages;
                StringBuilder stringBuilder6 = new StringBuilder();
                stringBuilder6.append("Read completed successfully: ");
                stringBuilder6.append(this.mPackages.size());
                stringBuilder6.append(" packages, ");
                stringBuilder6.append(this.mSharedUsers.size());
                stringBuilder6.append(" shared uids\n");
                stringBuilder.append(stringBuilder6.toString());
                writeKernelMappingLPr();
                return true;
            }
        }
        if (type == 2) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:44:0x013d A:{SYNTHETIC, Splitter:B:44:0x013d} */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x011d A:{Catch:{ XmlPullParserException -> 0x0191, IOException -> 0x0174, all -> 0x0172 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void applyDefaultPreferredAppsLPw(PackageManagerService service, int userId) {
        int i;
        String str;
        StringBuilder stringBuilder;
        Iterator it = this.mPackages.values().iterator();
        while (true) {
            i = 0;
            if (!it.hasNext()) {
                break;
            }
            PackageSetting ps = (PackageSetting) it.next();
            if (!((1 & ps.pkgFlags) == 0 || ps.pkg == null || ps.pkg.preferredActivityFilters == null)) {
                ArrayList<ActivityIntentInfo> intents = ps.pkg.preferredActivityFilters;
                while (i < intents.size()) {
                    ActivityIntentInfo aii = (ActivityIntentInfo) intents.get(i);
                    applyDefaultPreferredActivityLPw(service, aii, new ComponentName(ps.name, aii.activity.className), userId);
                    i++;
                }
            }
        }
        File preferredDir = new File(Environment.getRootDirectory(), "etc/preferred-apps");
        if (!preferredDir.exists() || !preferredDir.isDirectory()) {
            return;
        }
        if (preferredDir.canRead()) {
            File[] listFiles = preferredDir.listFiles();
            int length = listFiles.length;
            while (i < length) {
                File f = listFiles[i];
                String str2;
                StringBuilder stringBuilder2;
                if (!f.getPath().endsWith(".xml")) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Non-xml file ");
                    stringBuilder2.append(f);
                    stringBuilder2.append(" in ");
                    stringBuilder2.append(preferredDir);
                    stringBuilder2.append(" directory, ignoring");
                    Slog.i(str2, stringBuilder2.toString());
                } else if (f.canRead()) {
                    if (PackageManagerService.DEBUG_PREFERRED) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Reading default preferred ");
                        stringBuilder2.append(f);
                        Log.d(str2, stringBuilder2.toString());
                    }
                    InputStream str3 = null;
                    try {
                        int type;
                        str3 = new BufferedInputStream(new FileInputStream(f));
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(str3, null);
                        while (true) {
                            int next = parser.next();
                            type = next;
                            if (next == 2 || type == 1) {
                                StringBuilder stringBuilder3;
                                if (type == 2) {
                                    str2 = TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Preferred apps file ");
                                    stringBuilder3.append(f);
                                    stringBuilder3.append(" does not have start tag");
                                    Slog.w(str2, stringBuilder3.toString());
                                    try {
                                        str3.close();
                                    } catch (IOException e) {
                                    }
                                } else if ("preferred-activities".equals(parser.getName())) {
                                    readDefaultPreferredActivitiesLPw(service, parser, userId);
                                    str3.close();
                                } else {
                                    str2 = TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Preferred apps file ");
                                    stringBuilder3.append(f);
                                    stringBuilder3.append(" does not start with 'preferred-activities'");
                                    Slog.w(str2, stringBuilder3.toString());
                                    str3.close();
                                }
                            }
                        }
                        if (type == 2) {
                        }
                    } catch (XmlPullParserException e2) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error reading apps file ");
                        stringBuilder.append(f);
                        Slog.w(str, stringBuilder.toString(), e2);
                        if (str3 != null) {
                            str3.close();
                        }
                    } catch (IOException e3) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error reading apps file ");
                        stringBuilder.append(f);
                        Slog.w(str, stringBuilder.toString(), e3);
                        if (str3 != null) {
                            str3.close();
                        }
                    } catch (Throwable th) {
                        if (str3 != null) {
                            try {
                                str3.close();
                            } catch (IOException e4) {
                            }
                        }
                    }
                } else {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Preferred apps file ");
                    stringBuilder2.append(f);
                    stringBuilder2.append(" cannot be read");
                    Slog.w(str2, stringBuilder2.toString());
                }
                i++;
            }
            return;
        }
        String str4 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Directory ");
        stringBuilder4.append(preferredDir);
        stringBuilder4.append(" cannot be read");
        Slog.w(str4, stringBuilder4.toString());
    }

    private void applyDefaultPreferredActivityLPw(PackageManagerService service, IntentFilter tmpPa, ComponentName cn, int userId) {
        int i;
        int issp;
        int issp2;
        int issp3;
        String scheme;
        Intent intent;
        Builder builder;
        IntentFilter intentFilter = tmpPa;
        if (PackageManagerService.DEBUG_PREFERRED) {
            Log.d(TAG, "Processing preferred:");
            intentFilter.dump(new LogPrinter(3, TAG), "  ");
        }
        Intent intent2 = new Intent();
        int i2 = 0;
        intent2.setAction(intentFilter.getAction(0));
        int flags = 786432;
        for (i = 0; i < tmpPa.countCategories(); i++) {
            String cat = intentFilter.getCategory(i);
            if (cat.equals("android.intent.category.DEFAULT")) {
                flags |= 65536;
            } else {
                intent2.addCategory(cat);
            }
        }
        boolean doNonData = true;
        i = 0;
        boolean hasSchemes = false;
        while (i < tmpPa.countDataSchemes()) {
            String scheme2 = intentFilter.getDataScheme(i);
            if (!(scheme2 == null || scheme2.isEmpty())) {
                hasSchemes = true;
            }
            boolean doScheme = true;
            issp = i2;
            while (true) {
                issp2 = issp;
                if (issp2 >= tmpPa.countDataSchemeSpecificParts()) {
                    break;
                }
                Builder builder2 = new Builder();
                builder2.scheme(scheme2);
                PatternMatcher dataSchemeSpecificPart = intentFilter.getDataSchemeSpecificPart(issp2);
                builder2.opaquePart(dataSchemeSpecificPart.getPath());
                Intent finalIntent = new Intent(intent2);
                finalIntent.setData(builder2.build());
                PatternMatcher ssp = dataSchemeSpecificPart;
                issp3 = issp2;
                scheme = scheme2;
                applyDefaultPreferredActivityLPw(service, finalIntent, flags, cn, scheme2, dataSchemeSpecificPart, null, null, userId);
                doScheme = false;
                issp = issp3 + 1;
                scheme2 = scheme;
            }
            scheme = scheme2;
            issp = 0;
            while (true) {
                int iauth = issp;
                if (iauth >= tmpPa.countDataAuthorities()) {
                    break;
                }
                AuthorityEntry auth;
                int iauth2;
                AuthorityEntry auth2 = intentFilter.getDataAuthority(iauth);
                boolean doScheme2 = doScheme;
                doScheme = true;
                issp = 0;
                while (true) {
                    int ipath = issp;
                    if (ipath >= tmpPa.countDataPaths()) {
                        break;
                    }
                    Builder builder3 = new Builder();
                    builder3.scheme(scheme);
                    if (auth2.getHost() != null) {
                        builder3.authority(auth2.getHost());
                    }
                    PatternMatcher path = intentFilter.getDataPath(ipath);
                    builder3.path(path.getPath());
                    Intent finalIntent2 = new Intent(intent2);
                    finalIntent2.setData(builder3.build());
                    intent = finalIntent2;
                    Builder builder4 = builder3;
                    int ipath2 = ipath;
                    auth = auth2;
                    iauth2 = iauth;
                    applyDefaultPreferredActivityLPw(service, finalIntent2, flags, cn, scheme, null, auth2, path, userId);
                    doScheme2 = false;
                    doScheme = false;
                    issp = ipath2 + 1;
                    auth2 = auth;
                    iauth = iauth2;
                }
                auth = auth2;
                iauth2 = iauth;
                if (doScheme) {
                    builder = new Builder();
                    builder.scheme(scheme);
                    auth2 = auth;
                    if (auth2.getHost() != null) {
                        builder.authority(auth2.getHost());
                    }
                    Intent finalIntent3 = new Intent(intent2);
                    finalIntent3.setData(builder.build());
                    intent = finalIntent3;
                    applyDefaultPreferredActivityLPw(service, finalIntent3, flags, cn, scheme, null, auth2, null, userId);
                    doScheme = false;
                } else {
                    doScheme = doScheme2;
                }
                issp = iauth2 + 1;
            }
            if (doScheme) {
                builder = new Builder();
                builder.scheme(scheme);
                Intent finalIntent4 = new Intent(intent2);
                finalIntent4.setData(builder.build());
                applyDefaultPreferredActivityLPw(service, finalIntent4, flags, cn, scheme, null, null, null, userId);
            }
            doNonData = false;
            i++;
            i2 = 0;
        }
        int i3 = i2;
        for (i = i3; i < tmpPa.countDataTypes(); i++) {
            scheme = intentFilter.getDataType(i);
            if (hasSchemes) {
                builder = new Builder();
                issp = i3;
                while (true) {
                    issp2 = issp;
                    if (issp2 >= tmpPa.countDataSchemes()) {
                        break;
                    }
                    Builder builder5;
                    String scheme3 = intentFilter.getDataScheme(issp2);
                    if (scheme3 == null || scheme3.isEmpty()) {
                        issp3 = issp2;
                        builder5 = builder;
                    } else {
                        Intent finalIntent5 = new Intent(intent2);
                        builder.scheme(scheme3);
                        finalIntent5.setDataAndType(builder.build(), scheme);
                        intent = finalIntent5;
                        issp3 = issp2;
                        builder5 = builder;
                        applyDefaultPreferredActivityLPw(service, finalIntent5, flags, cn, scheme3, null, null, null, userId);
                    }
                    issp = issp3 + 1;
                    builder = builder5;
                }
            } else {
                Intent finalIntent6 = new Intent(intent2);
                finalIntent6.setType(scheme);
                applyDefaultPreferredActivityLPw(service, finalIntent6, flags, cn, null, null, null, null, userId);
            }
            doNonData = false;
        }
        if (doNonData) {
            applyDefaultPreferredActivityLPw(service, intent2, flags, cn, null, null, null, null, userId);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:73:0x024c  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0206  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void applyDefaultPreferredActivityLPw(PackageManagerService service, Intent intent, int flags, ComponentName cn, String scheme, PatternMatcher ssp, AuthorityEntry auth, PatternMatcher path, int userId) {
        ComponentName flags2;
        Intent intent2 = intent;
        String str = scheme;
        AuthorityEntry authorityEntry = auth;
        PatternMatcher patternMatcher = path;
        int flags3 = service.updateFlagsForResolve(flags, userId, intent2, Binder.getCallingUid(), false);
        List<ResolveInfo> ri = service.mActivities.queryIntent(intent2, intent.getType(), flags3, 0);
        if (PackageManagerService.DEBUG_PREFERRED) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Queried ");
            stringBuilder.append(intent2);
            stringBuilder.append(" results: ");
            stringBuilder.append(ri);
            Log.d(str2, stringBuilder.toString());
        }
        int i;
        int i2;
        List<ResolveInfo> list;
        String str3;
        int i3;
        if (ri == null || ri.size() <= 1) {
            i = userId;
            i2 = flags3;
            list = ri;
            flags3 = cn;
            str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No potential matches found for ");
            stringBuilder2.append(intent2);
            stringBuilder2.append(" while setting preferred ");
            stringBuilder2.append(cn.flattenToShortString());
            Slog.w(str3, stringBuilder2.toString());
            i3 = 0;
            return;
        }
        boolean haveAct = false;
        ComponentName haveNonSys = null;
        ComponentName[] set = new ComponentName[ri.size()];
        i = 0;
        int i4 = 0;
        while (i4 < ri.size()) {
            ActivityInfo ai = ((ResolveInfo) ri.get(i4)).activityInfo;
            set[i4] = new ComponentName(ai.packageName, ai.name);
            StringBuilder stringBuilder3;
            if ((ai.applicationInfo.flags & 1) == 0) {
                if (((ResolveInfo) ri.get(i4)).match >= 0) {
                    if (PackageManagerService.DEBUG_PREFERRED) {
                        str3 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Result ");
                        stringBuilder3.append(ai.packageName);
                        stringBuilder3.append(SliceAuthority.DELIMITER);
                        stringBuilder3.append(ai.name);
                        stringBuilder3.append(": non-system!");
                        Log.d(str3, stringBuilder3.toString());
                    }
                    haveNonSys = set[i4];
                    if (haveNonSys != null && 0 < i) {
                        haveNonSys = null;
                    }
                    if (haveAct || haveNonSys != null) {
                        i2 = flags3;
                        i3 = i;
                        flags3 = cn;
                        i = userId;
                        if (haveNonSys != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("No component ");
                            sb.append(cn.flattenToShortString());
                            sb.append(" found setting preferred ");
                            sb.append(intent2);
                            sb.append("; possible matches are ");
                            int i5 = 0;
                            while (true) {
                                int i6 = i5;
                                list = ri;
                                if (i6 < set.length) {
                                    if (i6 > 0) {
                                        sb.append(", ");
                                    }
                                    sb.append(set[i6].flattenToShortString());
                                    i5 = i6 + 1;
                                    ri = list;
                                } else {
                                    Slog.w(TAG, sb.toString());
                                    return;
                                }
                            }
                        }
                        i4 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Not setting preferred ");
                        stringBuilder4.append(intent2);
                        stringBuilder4.append("; found third party match ");
                        stringBuilder4.append(haveNonSys.flattenToShortString());
                        Slog.i(i4, stringBuilder4.toString());
                        return;
                    }
                    IntentFilter filter = new IntentFilter();
                    if (intent.getAction() != null) {
                        filter.addAction(intent.getAction());
                    }
                    if (intent.getCategories() != null) {
                        for (String cat : intent.getCategories()) {
                            filter.addCategory(cat);
                        }
                    }
                    if ((65536 & flags3) != 0) {
                        filter.addCategory("android.intent.category.DEFAULT");
                    }
                    if (str != null) {
                        filter.addDataScheme(str);
                    }
                    if (ssp != null) {
                        filter.addDataSchemeSpecificPart(ssp.getPath(), ssp.getType());
                    }
                    if (authorityEntry != null) {
                        filter.addDataAuthority(authorityEntry);
                    }
                    if (patternMatcher != null) {
                        filter.addDataPath(patternMatcher);
                    }
                    if (intent.getType() != null) {
                        try {
                            filter.addDataType(intent.getType());
                            i2 = flags3;
                            flags2 = cn;
                        } catch (MalformedMimeTypeException ex) {
                            String str4 = TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Malformed mimetype ");
                            stringBuilder5.append(intent.getType());
                            stringBuilder5.append(" for ");
                            flags2 = cn;
                            stringBuilder5.append(flags2);
                            Slog.w(str4, stringBuilder5.toString());
                        }
                    } else {
                        flags2 = cn;
                    }
                    editPreferredActivitiesLPw(userId).addFilter(new PreferredActivity(filter, i, set, flags2, true));
                    list = ri;
                    return;
                }
            } else if (cn.getPackageName().equals(ai.packageName) && cn.getClassName().equals(ai.name)) {
                if (PackageManagerService.DEBUG_PREFERRED) {
                    str3 = TAG;
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("Result ");
                    stringBuilder6.append(ai.packageName);
                    stringBuilder6.append(SliceAuthority.DELIMITER);
                    stringBuilder6.append(ai.name);
                    stringBuilder6.append(": default!");
                    Log.d(str3, stringBuilder6.toString());
                }
                haveAct = true;
                i = ((ResolveInfo) ri.get(i4)).match;
            } else if (PackageManagerService.DEBUG_PREFERRED) {
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Result ");
                stringBuilder3.append(ai.packageName);
                stringBuilder3.append(SliceAuthority.DELIMITER);
                stringBuilder3.append(ai.name);
                stringBuilder3.append(": skipped");
                Log.d(str3, stringBuilder3.toString());
            }
            i4++;
            PackageManagerService packageManagerService = service;
        }
        haveNonSys = null;
        if (haveAct) {
        }
        i2 = flags3;
        i3 = i;
        flags3 = cn;
        i = userId;
        if (haveNonSys != null) {
        }
    }

    private void readDefaultPreferredActivitiesLPw(PackageManagerService service, XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        PreferredActivity tmpPa = new PreferredActivity(parser);
                        if (tmpPa.mPref.getParseError() == null) {
                            applyDefaultPreferredActivityLPw(service, tmpPa, tmpPa.mPref.mComponent, userId);
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error in package manager settings: <preferred-activity> ");
                            stringBuilder.append(tmpPa.mPref.getParseError());
                            stringBuilder.append(" at ");
                            stringBuilder.append(parser.getPositionDescription());
                            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                        }
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown element under <preferred-activities>: ");
                        stringBuilder2.append(parser.getName());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
    }

    private void readDisabledSysPackageLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        int pkgPrivateFlags;
        String sharedIdStr;
        XmlPullParser xmlPullParser = parser;
        String name = xmlPullParser.getAttributeValue(null, ATTR_NAME);
        String realName = xmlPullParser.getAttributeValue(null, "realName");
        String codePathStr = xmlPullParser.getAttributeValue(null, "codePath");
        String resourcePathStr = xmlPullParser.getAttributeValue(null, "resourcePath");
        String legacyCpuAbiStr = xmlPullParser.getAttributeValue(null, "requiredCpuAbi");
        String legacyNativeLibraryPathStr = xmlPullParser.getAttributeValue(null, "nativeLibraryPath");
        String parentPackageName = xmlPullParser.getAttributeValue(null, "parentPackageName");
        String primaryCpuAbiStr = xmlPullParser.getAttributeValue(null, "primaryCpuAbi");
        String secondaryCpuAbiStr = xmlPullParser.getAttributeValue(null, "secondaryCpuAbi");
        String cpuAbiOverrideStr = xmlPullParser.getAttributeValue(null, "cpuAbiOverride");
        if (primaryCpuAbiStr == null && legacyCpuAbiStr != null) {
            primaryCpuAbiStr = legacyCpuAbiStr;
        }
        String primaryCpuAbiStr2 = primaryCpuAbiStr;
        if (resourcePathStr == null) {
            resourcePathStr = codePathStr;
        }
        String resourcePathStr2 = resourcePathStr;
        String version = xmlPullParser.getAttributeValue(null, "version");
        long versionCode = 0;
        if (version != null) {
            try {
                versionCode = Long.parseLong(version);
            } catch (NumberFormatException e) {
            }
        }
        long versionCode2 = versionCode;
        int pkgFlags = 0 | 1;
        if (PackageManagerService.locationIsPrivileged(codePathStr)) {
            pkgPrivateFlags = 0 | 8;
        } else {
            pkgPrivateFlags = 0;
        }
        String name2 = name;
        PackageSetting ps = new PackageSetting(name, realName, new File(codePathStr), new File(resourcePathStr2), legacyNativeLibraryPathStr, primaryCpuAbiStr2, secondaryCpuAbiStr, cpuAbiOverrideStr, versionCode2, pkgFlags, pkgPrivateFlags, parentPackageName, null, 0, null, null);
        String timeStampStr = xmlPullParser.getAttributeValue(null, "ft");
        if (timeStampStr != null) {
            try {
                ps.setTimeStamp(Long.parseLong(timeStampStr, 16));
            } catch (NumberFormatException e2) {
            }
        } else {
            timeStampStr = xmlPullParser.getAttributeValue(null, "ts");
            if (timeStampStr != null) {
                try {
                    ps.setTimeStamp(Long.parseLong(timeStampStr));
                } catch (NumberFormatException e3) {
                }
            }
        }
        timeStampStr = xmlPullParser.getAttributeValue(null, "it");
        if (timeStampStr != null) {
            try {
                ps.firstInstallTime = Long.parseLong(timeStampStr, 16);
            } catch (NumberFormatException e4) {
            }
        }
        timeStampStr = xmlPullParser.getAttributeValue(null, "ut");
        if (timeStampStr != null) {
            try {
                ps.lastUpdateTime = Long.parseLong(timeStampStr, 16);
            } catch (NumberFormatException e5) {
            }
        }
        resourcePathStr = xmlPullParser.getAttributeValue(null, "userId");
        int i = 0;
        ps.appId = resourcePathStr != null ? Integer.parseInt(resourcePathStr) : 0;
        if (ps.appId <= 0) {
            sharedIdStr = xmlPullParser.getAttributeValue(null, "sharedUserId");
            if (sharedIdStr != null) {
                i = Integer.parseInt(sharedIdStr);
            }
            ps.appId = i;
        }
        i = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= i)) {
                this.mDisabledSysPackages.put(name2, ps);
            } else if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_PERMISSIONS)) {
                        readInstallPermissionsLPr(xmlPullParser, ps.getPermissionsState());
                    } else if (parser.getName().equals(TAG_CHILD_PACKAGE)) {
                        sharedIdStr = xmlPullParser.getAttributeValue(null, ATTR_NAME);
                        if (ps.childPackageNames == null) {
                            ps.childPackageNames = new ArrayList();
                        }
                        ps.childPackageNames.add(sharedIdStr);
                    } else if (parser.getName().equals(TAG_USES_STATIC_LIB)) {
                        readUsesStaticLibLPw(xmlPullParser, ps);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element under <updated-package>: ");
                        stringBuilder.append(parser.getName());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
        this.mDisabledSysPackages.put(name2, ps);
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:91:0x0137=Splitter:B:91:0x0137, B:72:0x00fb=Splitter:B:72:0x00fb} */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00e0 A:{SYNTHETIC, Splitter:B:60:0x00e0} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x0126 A:{SYNTHETIC, Splitter:B:83:0x0126} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x00f3 A:{SYNTHETIC, Splitter:B:68:0x00f3} */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x01a4 A:{SYNTHETIC, Splitter:B:127:0x01a4} */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x0198 A:{SYNTHETIC, Splitter:B:122:0x0198} */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x01c3  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x01da  */
    /* JADX WARNING: Removed duplicated region for block: B:165:0x0220 A:{Catch:{ NumberFormatException -> 0x0229 }} */
    /* JADX WARNING: Removed duplicated region for block: B:158:0x01ea A:{SYNTHETIC, Splitter:B:158:0x01ea} */
    /* JADX WARNING: Removed duplicated region for block: B:172:0x0234 A:{Catch:{ NumberFormatException -> 0x0229 }} */
    /* JADX WARNING: Removed duplicated region for block: B:167:0x0224 A:{Catch:{ NumberFormatException -> 0x0229 }} */
    /* JADX WARNING: Removed duplicated region for block: B:175:0x023c  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x0237 A:{Catch:{ NumberFormatException -> 0x0229 }} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0242  */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x0240  */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x0246 A:{SYNTHETIC, Splitter:B:181:0x0246} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x0298  */
    /* JADX WARNING: Removed duplicated region for block: B:188:0x0256 A:{SYNTHETIC, Splitter:B:188:0x0256} */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:414:0x09ad  */
    /* JADX WARNING: Removed duplicated region for block: B:417:0x09bb  */
    /* JADX WARNING: Removed duplicated region for block: B:422:0x09cf  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0bc5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x09e4  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:404:0x0959  */
    /* JADX WARNING: Removed duplicated region for block: B:403:0x0951  */
    /* JADX WARNING: Removed duplicated region for block: B:414:0x09ad  */
    /* JADX WARNING: Removed duplicated region for block: B:417:0x09bb  */
    /* JADX WARNING: Removed duplicated region for block: B:422:0x09cf  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x09e4  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0bc5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:390:0x08fe  */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0bcc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readPackageLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        String uidError;
        String attributeValue;
        String resourcePathStr;
        String str;
        String str2;
        String secondaryCpuAbiString;
        String isOrphaned;
        String volumeUuid;
        String categoryHintString;
        String version;
        String version2;
        int i;
        String categoryHintString2;
        String installerPackageName;
        String str3;
        StringBuilder stringBuilder;
        String str4;
        PackageSetting packageSetting;
        String categoryHintString3;
        int i2;
        String str5;
        String str6;
        String str7;
        String str8;
        long j;
        int volumeUuid2;
        String appUseNotchModeStr;
        String maxAspectRatioStr;
        String installerPackageName2;
        String enabledStr;
        int outerDepth;
        String legacyNativeLibraryPathStr;
        String primaryCpuAbiString;
        String str9;
        XmlPullParser xmlPullParser = parser;
        String name = null;
        String realName = null;
        String idStr = null;
        String sharedIdStr = null;
        String codePathStr = null;
        String systemStr = null;
        String updateAvailable = null;
        int categoryHint = -1;
        int pkgFlags = 0;
        int pkgPrivateFlags = 0;
        long timeStamp = 0;
        long firstInstallTime = 0;
        long lastUpdateTime = 0;
        PackageSetting packageSetting2 = null;
        String cpuAbiOverrideString = null;
        String legacyCpuAbiString;
        Settings settings;
        long timeStamp2;
        long firstInstallTime2;
        long lastUpdateTime2;
        String resourcePathStr2;
        String cpuAbiOverrideString2;
        int categoryHint2;
        long versionCode;
        String str10;
        try {
            String parentPackageName;
            name = xmlPullParser.getAttributeValue(null, ATTR_NAME);
            try {
                realName = xmlPullParser.getAttributeValue(null, "realName");
                idStr = xmlPullParser.getAttributeValue(null, "userId");
                try {
                    uidError = xmlPullParser.getAttributeValue(null, "uidError");
                    try {
                        sharedIdStr = xmlPullParser.getAttributeValue(null, "sharedUserId");
                        try {
                            codePathStr = xmlPullParser.getAttributeValue(null, "codePath");
                            try {
                                attributeValue = xmlPullParser.getAttributeValue(null, "resourcePath");
                                try {
                                    legacyCpuAbiString = xmlPullParser.getAttributeValue(null, "requiredCpuAbi");
                                    try {
                                        parentPackageName = xmlPullParser.getAttributeValue(null, "parentPackageName");
                                        resourcePathStr = xmlPullParser.getAttributeValue(null, "nativeLibraryPath");
                                    } catch (NumberFormatException e) {
                                        str = name;
                                        name = sharedIdStr;
                                        str2 = codePathStr;
                                        settings = this;
                                        sharedIdStr = idStr;
                                        resourcePathStr = null;
                                        secondaryCpuAbiString = null;
                                        isOrphaned = null;
                                        volumeUuid = null;
                                        categoryHintString = null;
                                        timeStamp2 = 0;
                                        firstInstallTime2 = 0;
                                        lastUpdateTime2 = 0;
                                        version = null;
                                        version2 = null;
                                        resourcePathStr2 = attributeValue;
                                        i = 5;
                                        sharedIdStr = name;
                                        timeStamp = realName;
                                        name = str;
                                        categoryHintString2 = null;
                                        installerPackageName = null;
                                        str3 = cpuAbiOverrideString;
                                        cpuAbiOverrideString = null;
                                        cpuAbiOverrideString2 = str3;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error in package manager settings: package ");
                                        stringBuilder.append(name);
                                        stringBuilder.append(" has bad userId ");
                                        stringBuilder.append(idStr);
                                        stringBuilder.append(" at ");
                                        stringBuilder.append(parser.getPositionDescription());
                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                        str = name;
                                        systemStr = idStr;
                                        str2 = codePathStr;
                                        str4 = version;
                                        version = updateAvailable;
                                        packageSetting = packageSetting2;
                                        idStr = installerPackageName;
                                        realName = uidError;
                                        sharedIdStr = isOrphaned;
                                        codePathStr = volumeUuid;
                                        resourcePathStr2 = resourcePathStr;
                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                        categoryHint2 = categoryHint;
                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                        if (packageSetting != null) {
                                        }
                                    }
                                } catch (NumberFormatException e2) {
                                    str = name;
                                    name = sharedIdStr;
                                    str2 = codePathStr;
                                    settings = this;
                                    sharedIdStr = idStr;
                                    legacyCpuAbiString = null;
                                    resourcePathStr = null;
                                    secondaryCpuAbiString = null;
                                    isOrphaned = null;
                                    volumeUuid = null;
                                    categoryHintString = null;
                                    timeStamp2 = 0;
                                    firstInstallTime2 = 0;
                                    lastUpdateTime2 = 0;
                                    version = null;
                                    version2 = null;
                                    resourcePathStr2 = attributeValue;
                                    i = 5;
                                    sharedIdStr = name;
                                    timeStamp = realName;
                                    name = str;
                                    categoryHintString2 = null;
                                    installerPackageName = null;
                                    str3 = cpuAbiOverrideString;
                                    cpuAbiOverrideString = null;
                                    cpuAbiOverrideString2 = str3;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error in package manager settings: package ");
                                    stringBuilder.append(name);
                                    stringBuilder.append(" has bad userId ");
                                    stringBuilder.append(idStr);
                                    stringBuilder.append(" at ");
                                    stringBuilder.append(parser.getPositionDescription());
                                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                    str = name;
                                    systemStr = idStr;
                                    str2 = codePathStr;
                                    str4 = version;
                                    version = updateAvailable;
                                    packageSetting = packageSetting2;
                                    idStr = installerPackageName;
                                    realName = uidError;
                                    sharedIdStr = isOrphaned;
                                    codePathStr = volumeUuid;
                                    resourcePathStr2 = resourcePathStr;
                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                    categoryHint2 = categoryHint;
                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                    if (packageSetting != null) {
                                    }
                                }
                            } catch (NumberFormatException e3) {
                                str = name;
                                name = sharedIdStr;
                                str2 = codePathStr;
                                settings = this;
                                sharedIdStr = idStr;
                                legacyCpuAbiString = null;
                                isOrphaned = null;
                                volumeUuid = null;
                                categoryHintString = null;
                                timeStamp2 = 0;
                                firstInstallTime2 = 0;
                                lastUpdateTime2 = 0;
                                i = 5;
                                sharedIdStr = name;
                                timeStamp = realName;
                                resourcePathStr2 = null;
                                resourcePathStr = null;
                                secondaryCpuAbiString = null;
                                name = str;
                                categoryHintString2 = null;
                                version = null;
                                version2 = null;
                                installerPackageName = null;
                                str3 = cpuAbiOverrideString;
                                cpuAbiOverrideString = null;
                                cpuAbiOverrideString2 = str3;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Error in package manager settings: package ");
                                stringBuilder.append(name);
                                stringBuilder.append(" has bad userId ");
                                stringBuilder.append(idStr);
                                stringBuilder.append(" at ");
                                stringBuilder.append(parser.getPositionDescription());
                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                str = name;
                                systemStr = idStr;
                                str2 = codePathStr;
                                str4 = version;
                                version = updateAvailable;
                                packageSetting = packageSetting2;
                                idStr = installerPackageName;
                                realName = uidError;
                                sharedIdStr = isOrphaned;
                                codePathStr = volumeUuid;
                                resourcePathStr2 = resourcePathStr;
                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                categoryHint2 = categoryHint;
                                secondaryCpuAbiString = cpuAbiOverrideString;
                                if (packageSetting != null) {
                                }
                            }
                        } catch (NumberFormatException e4) {
                            str = name;
                            name = sharedIdStr;
                            settings = this;
                            sharedIdStr = idStr;
                            legacyCpuAbiString = null;
                            isOrphaned = null;
                            volumeUuid = null;
                            categoryHintString = null;
                            timeStamp2 = 0;
                            firstInstallTime2 = 0;
                            lastUpdateTime2 = 0;
                            i = 5;
                            sharedIdStr = name;
                            resourcePathStr2 = null;
                            resourcePathStr = null;
                            secondaryCpuAbiString = null;
                            name = str;
                            categoryHintString2 = null;
                            version = null;
                            versionCode = 0;
                            installerPackageName = null;
                            str3 = cpuAbiOverrideString;
                            cpuAbiOverrideString = null;
                            cpuAbiOverrideString2 = str3;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error in package manager settings: package ");
                            stringBuilder.append(name);
                            stringBuilder.append(" has bad userId ");
                            stringBuilder.append(idStr);
                            stringBuilder.append(" at ");
                            stringBuilder.append(parser.getPositionDescription());
                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                            str = name;
                            systemStr = idStr;
                            str2 = codePathStr;
                            str4 = version;
                            version = updateAvailable;
                            packageSetting = packageSetting2;
                            idStr = installerPackageName;
                            realName = uidError;
                            sharedIdStr = isOrphaned;
                            codePathStr = volumeUuid;
                            resourcePathStr2 = resourcePathStr;
                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                            categoryHint2 = categoryHint;
                            secondaryCpuAbiString = cpuAbiOverrideString;
                            if (packageSetting != null) {
                            }
                        }
                    } catch (NumberFormatException e5) {
                        str = name;
                        name = idStr;
                        settings = this;
                        legacyCpuAbiString = null;
                        name = str;
                        isOrphaned = null;
                        volumeUuid = null;
                        categoryHintString = null;
                        timeStamp2 = 0;
                        firstInstallTime2 = 0;
                        lastUpdateTime2 = 0;
                        i = 5;
                        timeStamp = realName;
                        resourcePathStr2 = null;
                        resourcePathStr = null;
                        secondaryCpuAbiString = null;
                        categoryHintString2 = null;
                        version = null;
                        version2 = null;
                        installerPackageName = null;
                        str3 = cpuAbiOverrideString;
                        cpuAbiOverrideString = null;
                        cpuAbiOverrideString2 = str3;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error in package manager settings: package ");
                        stringBuilder.append(name);
                        stringBuilder.append(" has bad userId ");
                        stringBuilder.append(idStr);
                        stringBuilder.append(" at ");
                        stringBuilder.append(parser.getPositionDescription());
                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                        str = name;
                        systemStr = idStr;
                        str2 = codePathStr;
                        str4 = version;
                        version = updateAvailable;
                        packageSetting = packageSetting2;
                        idStr = installerPackageName;
                        realName = uidError;
                        sharedIdStr = isOrphaned;
                        codePathStr = volumeUuid;
                        resourcePathStr2 = resourcePathStr;
                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                        categoryHint2 = categoryHint;
                        secondaryCpuAbiString = cpuAbiOverrideString;
                        if (packageSetting != null) {
                        }
                    }
                } catch (NumberFormatException e6) {
                    str = name;
                    name = idStr;
                    settings = this;
                    legacyCpuAbiString = null;
                    name = str;
                    uidError = null;
                    isOrphaned = null;
                    volumeUuid = null;
                    categoryHintString = null;
                    timeStamp2 = 0;
                    firstInstallTime2 = 0;
                    lastUpdateTime2 = 0;
                    i = 5;
                    timeStamp = realName;
                    resourcePathStr2 = null;
                    resourcePathStr = null;
                    secondaryCpuAbiString = null;
                    categoryHintString2 = null;
                    version = null;
                    version2 = null;
                    installerPackageName = null;
                    str3 = cpuAbiOverrideString;
                    cpuAbiOverrideString = null;
                    cpuAbiOverrideString2 = str3;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error in package manager settings: package ");
                    stringBuilder.append(name);
                    stringBuilder.append(" has bad userId ");
                    stringBuilder.append(idStr);
                    stringBuilder.append(" at ");
                    stringBuilder.append(parser.getPositionDescription());
                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                    str = name;
                    systemStr = idStr;
                    str2 = codePathStr;
                    str4 = version;
                    version = updateAvailable;
                    packageSetting = packageSetting2;
                    idStr = installerPackageName;
                    realName = uidError;
                    sharedIdStr = isOrphaned;
                    codePathStr = volumeUuid;
                    resourcePathStr2 = resourcePathStr;
                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                    categoryHint2 = categoryHint;
                    secondaryCpuAbiString = cpuAbiOverrideString;
                    if (packageSetting != null) {
                    }
                }
            } catch (NumberFormatException e7) {
                str = name;
                settings = this;
                i = 5;
                legacyCpuAbiString = null;
                uidError = null;
                isOrphaned = null;
                volumeUuid = null;
                categoryHintString = null;
                timeStamp2 = 0;
                firstInstallTime2 = 0;
                lastUpdateTime2 = 0;
                timeStamp = realName;
                resourcePathStr2 = null;
                resourcePathStr = null;
                secondaryCpuAbiString = null;
                categoryHintString2 = null;
                version = null;
                version2 = null;
                installerPackageName = null;
                str3 = cpuAbiOverrideString;
                cpuAbiOverrideString = null;
                cpuAbiOverrideString2 = str3;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error in package manager settings: package ");
                stringBuilder.append(name);
                stringBuilder.append(" has bad userId ");
                stringBuilder.append(idStr);
                stringBuilder.append(" at ");
                stringBuilder.append(parser.getPositionDescription());
                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                str = name;
                systemStr = idStr;
                str2 = codePathStr;
                str4 = version;
                version = updateAvailable;
                packageSetting = packageSetting2;
                idStr = installerPackageName;
                realName = uidError;
                sharedIdStr = isOrphaned;
                codePathStr = volumeUuid;
                resourcePathStr2 = resourcePathStr;
                cpuAbiOverrideString2 = secondaryCpuAbiString;
                categoryHint2 = categoryHint;
                secondaryCpuAbiString = cpuAbiOverrideString;
                if (packageSetting != null) {
                }
            }
            try {
                resourcePathStr2 = xmlPullParser.getAttributeValue(null, "primaryCpuAbi");
                try {
                    int sharedUserId;
                    long timeStamp3;
                    long firstInstallTime3;
                    long lastUpdateTime3;
                    String resourcePathStr3;
                    String str11;
                    String str12;
                    secondaryCpuAbiString = xmlPullParser.getAttributeValue(null, "secondaryCpuAbi");
                    try {
                        cpuAbiOverrideString2 = xmlPullParser.getAttributeValue(null, "cpuAbiOverride");
                        try {
                            updateAvailable = xmlPullParser.getAttributeValue(null, "updateAvailable");
                            cpuAbiOverrideString = (resourcePathStr2 != null || legacyCpuAbiString == null) ? resourcePathStr2 : legacyCpuAbiString;
                            try {
                                version = xmlPullParser.getAttributeValue(null, "version");
                                if (version != null) {
                                    int pkgFlags2;
                                    try {
                                        versionCode = Long.parseLong(version);
                                    } catch (NumberFormatException e8) {
                                    }
                                    installerPackageName = xmlPullParser.getAttributeValue(null, "installer");
                                    try {
                                        isOrphaned = xmlPullParser.getAttributeValue(null, "isOrphaned");
                                        try {
                                            volumeUuid = xmlPullParser.getAttributeValue(null, ATTR_VOLUME_UUID);
                                            try {
                                                resourcePathStr2 = xmlPullParser.getAttributeValue(null, "categoryHint");
                                                if (resourcePathStr2 != null) {
                                                    try {
                                                        categoryHint = Integer.parseInt(resourcePathStr2);
                                                    } catch (NumberFormatException e9) {
                                                    }
                                                }
                                                try {
                                                    systemStr = xmlPullParser.getAttributeValue(null, "publicFlags");
                                                    if (systemStr == null) {
                                                        try {
                                                            pkgFlags = Integer.parseInt(systemStr);
                                                        } catch (NumberFormatException e10) {
                                                        }
                                                        try {
                                                            systemStr = xmlPullParser.getAttributeValue(null, "privateFlags");
                                                            if (systemStr != null) {
                                                                try {
                                                                    pkgPrivateFlags = Integer.parseInt(systemStr);
                                                                } catch (NumberFormatException e11) {
                                                                }
                                                            }
                                                        } catch (NumberFormatException e12) {
                                                            categoryHintString = resourcePathStr2;
                                                            categoryHintString2 = systemStr;
                                                            settings = this;
                                                            timeStamp2 = 0;
                                                            firstInstallTime2 = 0;
                                                            lastUpdateTime2 = 0;
                                                            resourcePathStr2 = attributeValue;
                                                            i = 5;
                                                            timeStamp = realName;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("Error in package manager settings: package ");
                                                            stringBuilder.append(name);
                                                            stringBuilder.append(" has bad userId ");
                                                            stringBuilder.append(idStr);
                                                            stringBuilder.append(" at ");
                                                            stringBuilder.append(parser.getPositionDescription());
                                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                            str = name;
                                                            systemStr = idStr;
                                                            str2 = codePathStr;
                                                            str4 = version;
                                                            version = updateAvailable;
                                                            packageSetting = packageSetting2;
                                                            idStr = installerPackageName;
                                                            realName = uidError;
                                                            sharedIdStr = isOrphaned;
                                                            codePathStr = volumeUuid;
                                                            resourcePathStr2 = resourcePathStr;
                                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                            categoryHint2 = categoryHint;
                                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                                            if (packageSetting != null) {
                                                            }
                                                        }
                                                    } else {
                                                        systemStr = xmlPullParser.getAttributeValue(null, ATTR_FLAGS);
                                                        if (systemStr != null) {
                                                            try {
                                                                pkgFlags = Integer.parseInt(systemStr);
                                                            } catch (NumberFormatException e13) {
                                                            }
                                                            if ((pkgFlags & PRE_M_APP_INFO_FLAG_HIDDEN) != 0) {
                                                                pkgPrivateFlags = 0 | 1;
                                                            }
                                                            if ((pkgFlags & PRE_M_APP_INFO_FLAG_CANT_SAVE_STATE) != 0) {
                                                                pkgPrivateFlags |= 2;
                                                            }
                                                            if ((pkgFlags & PRE_M_APP_INFO_FLAG_FORWARD_LOCK) != 0) {
                                                                pkgPrivateFlags |= 4;
                                                            }
                                                            if ((pkgFlags & PRE_M_APP_INFO_FLAG_PRIVILEGED) != 0) {
                                                                pkgPrivateFlags |= 8;
                                                            }
                                                            pkgFlags2 = pkgFlags & (~(((PRE_M_APP_INFO_FLAG_HIDDEN | PRE_M_APP_INFO_FLAG_CANT_SAVE_STATE) | PRE_M_APP_INFO_FLAG_FORWARD_LOCK) | PRE_M_APP_INFO_FLAG_PRIVILEGED));
                                                        } else {
                                                            systemStr = xmlPullParser.getAttributeValue(null, "system");
                                                            if (systemStr != null) {
                                                                pkgFlags2 = 0 | ("true".equalsIgnoreCase(systemStr) ? 1 : 0);
                                                            } else {
                                                                pkgFlags2 = 0 | 1;
                                                            }
                                                        }
                                                        pkgFlags = pkgFlags2;
                                                    }
                                                    systemStr = xmlPullParser.getAttributeValue(null, "ft");
                                                    if (systemStr == null) {
                                                        try {
                                                            timeStamp = Long.parseLong(systemStr, 16);
                                                        } catch (NumberFormatException e14) {
                                                        }
                                                    } else {
                                                        systemStr = xmlPullParser.getAttributeValue(null, "ts");
                                                        if (systemStr != null) {
                                                            try {
                                                                timeStamp = Long.parseLong(systemStr);
                                                            } catch (NumberFormatException e15) {
                                                            }
                                                        }
                                                    }
                                                    timeStamp2 = timeStamp;
                                                } catch (NumberFormatException e16) {
                                                    str = name;
                                                    name = sharedIdStr;
                                                    str2 = codePathStr;
                                                    categoryHintString = resourcePathStr2;
                                                    str4 = version;
                                                    sharedIdStr = idStr;
                                                    idStr = 5;
                                                    settings = this;
                                                    timeStamp2 = 0;
                                                    firstInstallTime2 = 0;
                                                    lastUpdateTime2 = 0;
                                                    resourcePathStr2 = attributeValue;
                                                    i = idStr;
                                                    idStr = sharedIdStr;
                                                    sharedIdStr = name;
                                                    name = str;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Error in package manager settings: package ");
                                                    stringBuilder.append(name);
                                                    stringBuilder.append(" has bad userId ");
                                                    stringBuilder.append(idStr);
                                                    stringBuilder.append(" at ");
                                                    stringBuilder.append(parser.getPositionDescription());
                                                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                    str = name;
                                                    systemStr = idStr;
                                                    str2 = codePathStr;
                                                    str4 = version;
                                                    version = updateAvailable;
                                                    packageSetting = packageSetting2;
                                                    idStr = installerPackageName;
                                                    realName = uidError;
                                                    sharedIdStr = isOrphaned;
                                                    codePathStr = volumeUuid;
                                                    resourcePathStr2 = resourcePathStr;
                                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                    categoryHint2 = categoryHint;
                                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                                    if (packageSetting != null) {
                                                    }
                                                }
                                            } catch (NumberFormatException e17) {
                                                str = name;
                                                name = sharedIdStr;
                                                str2 = codePathStr;
                                                str4 = version;
                                                sharedIdStr = idStr;
                                                settings = this;
                                                categoryHintString = null;
                                                timeStamp2 = 0;
                                                firstInstallTime2 = 0;
                                                lastUpdateTime2 = 0;
                                                resourcePathStr2 = attributeValue;
                                                i = 5;
                                                sharedIdStr = name;
                                                name = str;
                                                categoryHintString2 = null;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Error in package manager settings: package ");
                                                stringBuilder.append(name);
                                                stringBuilder.append(" has bad userId ");
                                                stringBuilder.append(idStr);
                                                stringBuilder.append(" at ");
                                                stringBuilder.append(parser.getPositionDescription());
                                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                str = name;
                                                systemStr = idStr;
                                                str2 = codePathStr;
                                                str4 = version;
                                                version = updateAvailable;
                                                packageSetting = packageSetting2;
                                                idStr = installerPackageName;
                                                realName = uidError;
                                                sharedIdStr = isOrphaned;
                                                codePathStr = volumeUuid;
                                                resourcePathStr2 = resourcePathStr;
                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                categoryHint2 = categoryHint;
                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                if (packageSetting != null) {
                                                }
                                            }
                                        } catch (NumberFormatException e18) {
                                            str = name;
                                            name = sharedIdStr;
                                            str2 = codePathStr;
                                            str4 = version;
                                            sharedIdStr = idStr;
                                            settings = this;
                                            volumeUuid = null;
                                            categoryHintString = null;
                                            timeStamp2 = 0;
                                            firstInstallTime2 = 0;
                                            lastUpdateTime2 = 0;
                                            resourcePathStr2 = attributeValue;
                                            i = 5;
                                            sharedIdStr = name;
                                            name = str;
                                            categoryHintString2 = null;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Error in package manager settings: package ");
                                            stringBuilder.append(name);
                                            stringBuilder.append(" has bad userId ");
                                            stringBuilder.append(idStr);
                                            stringBuilder.append(" at ");
                                            stringBuilder.append(parser.getPositionDescription());
                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                            str = name;
                                            systemStr = idStr;
                                            str2 = codePathStr;
                                            str4 = version;
                                            version = updateAvailable;
                                            packageSetting = packageSetting2;
                                            idStr = installerPackageName;
                                            realName = uidError;
                                            sharedIdStr = isOrphaned;
                                            codePathStr = volumeUuid;
                                            resourcePathStr2 = resourcePathStr;
                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                            categoryHint2 = categoryHint;
                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                            if (packageSetting != null) {
                                            }
                                        }
                                    } catch (NumberFormatException e19) {
                                        str = name;
                                        name = sharedIdStr;
                                        str2 = codePathStr;
                                        str4 = version;
                                        sharedIdStr = idStr;
                                        settings = this;
                                        isOrphaned = null;
                                        volumeUuid = null;
                                        categoryHintString = null;
                                        timeStamp2 = 0;
                                        firstInstallTime2 = 0;
                                        lastUpdateTime2 = 0;
                                        resourcePathStr2 = attributeValue;
                                        i = 5;
                                        sharedIdStr = name;
                                        name = str;
                                        categoryHintString2 = null;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error in package manager settings: package ");
                                        stringBuilder.append(name);
                                        stringBuilder.append(" has bad userId ");
                                        stringBuilder.append(idStr);
                                        stringBuilder.append(" at ");
                                        stringBuilder.append(parser.getPositionDescription());
                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                        str = name;
                                        systemStr = idStr;
                                        str2 = codePathStr;
                                        str4 = version;
                                        version = updateAvailable;
                                        packageSetting = packageSetting2;
                                        idStr = installerPackageName;
                                        realName = uidError;
                                        sharedIdStr = isOrphaned;
                                        codePathStr = volumeUuid;
                                        resourcePathStr2 = resourcePathStr;
                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                        categoryHint2 = categoryHint;
                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                        if (packageSetting != null) {
                                        }
                                    }
                                    try {
                                        str = xmlPullParser.getAttributeValue(null, "it");
                                        if (str != null) {
                                            try {
                                                firstInstallTime = Long.parseLong(str, 16);
                                            } catch (NumberFormatException e20) {
                                            }
                                        }
                                        firstInstallTime2 = firstInstallTime;
                                        try {
                                            str = xmlPullParser.getAttributeValue(null, "ut");
                                            if (str != null) {
                                                try {
                                                    lastUpdateTime = Long.parseLong(str, 16);
                                                } catch (NumberFormatException e21) {
                                                }
                                            }
                                            lastUpdateTime2 = lastUpdateTime;
                                            try {
                                                String str13;
                                                if (PackageManagerService.DEBUG_SETTINGS) {
                                                    categoryHintString3 = resourcePathStr2;
                                                } else {
                                                    try {
                                                        str13 = "PackageManager";
                                                        StringBuilder stringBuilder2 = new StringBuilder();
                                                        categoryHintString3 = resourcePathStr2;
                                                        try {
                                                            stringBuilder2.append("Reading package: ");
                                                            stringBuilder2.append(name);
                                                            stringBuilder2.append(" userId=");
                                                            stringBuilder2.append(idStr);
                                                            stringBuilder2.append(" sharedUserId=");
                                                            stringBuilder2.append(sharedIdStr);
                                                            Log.v(str13, stringBuilder2.toString());
                                                        } catch (NumberFormatException e22) {
                                                            settings = this;
                                                            resourcePathStr2 = attributeValue;
                                                            i = 5;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("Error in package manager settings: package ");
                                                            stringBuilder.append(name);
                                                            stringBuilder.append(" has bad userId ");
                                                            stringBuilder.append(idStr);
                                                            stringBuilder.append(" at ");
                                                            stringBuilder.append(parser.getPositionDescription());
                                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                            str = name;
                                                            systemStr = idStr;
                                                            str2 = codePathStr;
                                                            str4 = version;
                                                            version = updateAvailable;
                                                            packageSetting = packageSetting2;
                                                            idStr = installerPackageName;
                                                            realName = uidError;
                                                            sharedIdStr = isOrphaned;
                                                            codePathStr = volumeUuid;
                                                            resourcePathStr2 = resourcePathStr;
                                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                            categoryHint2 = categoryHint;
                                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                                            if (packageSetting != null) {
                                                            }
                                                        }
                                                    } catch (NumberFormatException e23) {
                                                        categoryHintString3 = resourcePathStr2;
                                                        str10 = realName;
                                                        settings = this;
                                                        resourcePathStr2 = attributeValue;
                                                        categoryHintString = categoryHintString3;
                                                        i = 5;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Error in package manager settings: package ");
                                                        stringBuilder.append(name);
                                                        stringBuilder.append(" has bad userId ");
                                                        stringBuilder.append(idStr);
                                                        stringBuilder.append(" at ");
                                                        stringBuilder.append(parser.getPositionDescription());
                                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                        str = name;
                                                        systemStr = idStr;
                                                        str2 = codePathStr;
                                                        str4 = version;
                                                        version = updateAvailable;
                                                        packageSetting = packageSetting2;
                                                        idStr = installerPackageName;
                                                        realName = uidError;
                                                        sharedIdStr = isOrphaned;
                                                        codePathStr = volumeUuid;
                                                        resourcePathStr2 = resourcePathStr;
                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                        categoryHint2 = categoryHint;
                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                        if (packageSetting != null) {
                                                        }
                                                    }
                                                }
                                                if (idStr == null) {
                                                    pkgFlags2 = Integer.parseInt(idStr);
                                                } else {
                                                    pkgFlags2 = 0;
                                                }
                                                sharedUserId = sharedIdStr == null ? Integer.parseInt(sharedIdStr) : null;
                                                if (attributeValue != null) {
                                                    resourcePathStr2 = codePathStr;
                                                } else {
                                                    resourcePathStr2 = attributeValue;
                                                }
                                                if (realName != null) {
                                                    try {
                                                        realName = realName.intern();
                                                    } catch (NumberFormatException e24) {
                                                        str10 = realName;
                                                        settings = this;
                                                        i = 5;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Error in package manager settings: package ");
                                                        stringBuilder.append(name);
                                                        stringBuilder.append(" has bad userId ");
                                                        stringBuilder.append(idStr);
                                                        stringBuilder.append(" at ");
                                                        stringBuilder.append(parser.getPositionDescription());
                                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                        str = name;
                                                        systemStr = idStr;
                                                        str2 = codePathStr;
                                                        str4 = version;
                                                        version = updateAvailable;
                                                        packageSetting = packageSetting2;
                                                        idStr = installerPackageName;
                                                        realName = uidError;
                                                        sharedIdStr = isOrphaned;
                                                        codePathStr = volumeUuid;
                                                        resourcePathStr2 = resourcePathStr;
                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                        categoryHint2 = categoryHint;
                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                        if (packageSetting != null) {
                                                        }
                                                    }
                                                }
                                                str10 = realName;
                                                String idStr2;
                                                if (name != null) {
                                                    try {
                                                        stringBuilder = new StringBuilder();
                                                        idStr2 = idStr;
                                                        try {
                                                            stringBuilder.append("Error in package manager settings: <package> has no name at ");
                                                            stringBuilder.append(parser.getPositionDescription());
                                                            i2 = 5;
                                                            try {
                                                                PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                                                                str = name;
                                                                name = sharedIdStr;
                                                                version = this;
                                                                timeStamp3 = timeStamp2;
                                                                firstInstallTime3 = firstInstallTime2;
                                                                lastUpdateTime3 = lastUpdateTime2;
                                                                categoryHintString = categoryHintString3;
                                                                sharedIdStr = idStr2;
                                                                i2 = 5;
                                                            } catch (NumberFormatException e25) {
                                                                settings = this;
                                                                categoryHintString = categoryHintString3;
                                                                i = i2;
                                                                idStr = idStr2;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("Error in package manager settings: package ");
                                                                stringBuilder.append(name);
                                                                stringBuilder.append(" has bad userId ");
                                                                stringBuilder.append(idStr);
                                                                stringBuilder.append(" at ");
                                                                stringBuilder.append(parser.getPositionDescription());
                                                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                str = name;
                                                                systemStr = idStr;
                                                                str2 = codePathStr;
                                                                str4 = version;
                                                                version = updateAvailable;
                                                                packageSetting = packageSetting2;
                                                                idStr = installerPackageName;
                                                                realName = uidError;
                                                                sharedIdStr = isOrphaned;
                                                                codePathStr = volumeUuid;
                                                                resourcePathStr2 = resourcePathStr;
                                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                categoryHint2 = categoryHint;
                                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                                if (packageSetting != null) {
                                                                }
                                                            }
                                                        } catch (NumberFormatException e26) {
                                                            settings = this;
                                                            categoryHintString = categoryHintString3;
                                                            idStr = idStr2;
                                                            i = 5;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("Error in package manager settings: package ");
                                                            stringBuilder.append(name);
                                                            stringBuilder.append(" has bad userId ");
                                                            stringBuilder.append(idStr);
                                                            stringBuilder.append(" at ");
                                                            stringBuilder.append(parser.getPositionDescription());
                                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                            str = name;
                                                            systemStr = idStr;
                                                            str2 = codePathStr;
                                                            str4 = version;
                                                            version = updateAvailable;
                                                            packageSetting = packageSetting2;
                                                            idStr = installerPackageName;
                                                            realName = uidError;
                                                            sharedIdStr = isOrphaned;
                                                            codePathStr = volumeUuid;
                                                            resourcePathStr2 = resourcePathStr;
                                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                            categoryHint2 = categoryHint;
                                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                                            if (packageSetting != null) {
                                                            }
                                                        }
                                                    } catch (NumberFormatException e27) {
                                                        idStr2 = idStr;
                                                        settings = this;
                                                        categoryHintString = categoryHintString3;
                                                        i = 5;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Error in package manager settings: package ");
                                                        stringBuilder.append(name);
                                                        stringBuilder.append(" has bad userId ");
                                                        stringBuilder.append(idStr);
                                                        stringBuilder.append(" at ");
                                                        stringBuilder.append(parser.getPositionDescription());
                                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                        str = name;
                                                        systemStr = idStr;
                                                        str2 = codePathStr;
                                                        str4 = version;
                                                        version = updateAvailable;
                                                        packageSetting = packageSetting2;
                                                        idStr = installerPackageName;
                                                        realName = uidError;
                                                        sharedIdStr = isOrphaned;
                                                        codePathStr = volumeUuid;
                                                        resourcePathStr2 = resourcePathStr;
                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                        categoryHint2 = categoryHint;
                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                        if (packageSetting != null) {
                                                        }
                                                    }
                                                } else {
                                                    idStr2 = idStr;
                                                    long j2;
                                                    if (codePathStr == null) {
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Error in package manager settings: <package> has no codePath at ");
                                                        stringBuilder.append(parser.getPositionDescription());
                                                        i2 = 5;
                                                        PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                                                        str = name;
                                                        name = sharedIdStr;
                                                        str2 = codePathStr;
                                                        str4 = version;
                                                        Object version3 = this;
                                                        timeStamp3 = timeStamp2;
                                                        firstInstallTime3 = firstInstallTime2;
                                                        lastUpdateTime3 = lastUpdateTime2;
                                                        categoryHintString = categoryHintString3;
                                                        sharedIdStr = idStr2;
                                                    } else if (pkgFlags2 > 0) {
                                                        String name2;
                                                        try {
                                                            String sharedIdStr2 = sharedIdStr;
                                                            try {
                                                                String codePathStr2 = codePathStr;
                                                                try {
                                                                    name2 = name;
                                                                    int i3 = 5;
                                                                    str5 = idStr2;
                                                                    str6 = sharedIdStr2;
                                                                    str7 = codePathStr2;
                                                                    str8 = resourcePathStr2;
                                                                    categoryHintString = categoryHintString3;
                                                                    str4 = version;
                                                                    str = versionCode;
                                                                } catch (NumberFormatException e28) {
                                                                    str = name;
                                                                    str8 = resourcePathStr2;
                                                                    str4 = version;
                                                                    j2 = timeStamp2;
                                                                    j = firstInstallTime2;
                                                                    lastUpdateTime3 = lastUpdateTime2;
                                                                    categoryHintString = categoryHintString3;
                                                                    settings = this;
                                                                    idStr = idStr2;
                                                                    sharedIdStr = sharedIdStr2;
                                                                    codePathStr = codePathStr2;
                                                                    i = 5;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("Error in package manager settings: package ");
                                                                    stringBuilder.append(name);
                                                                    stringBuilder.append(" has bad userId ");
                                                                    stringBuilder.append(idStr);
                                                                    stringBuilder.append(" at ");
                                                                    stringBuilder.append(parser.getPositionDescription());
                                                                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                    str = name;
                                                                    systemStr = idStr;
                                                                    str2 = codePathStr;
                                                                    str4 = version;
                                                                    version = updateAvailable;
                                                                    packageSetting = packageSetting2;
                                                                    idStr = installerPackageName;
                                                                    realName = uidError;
                                                                    sharedIdStr = isOrphaned;
                                                                    codePathStr = volumeUuid;
                                                                    resourcePathStr2 = resourcePathStr;
                                                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                    categoryHint2 = categoryHint;
                                                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                                                    if (packageSetting != null) {
                                                                    }
                                                                }
                                                            } catch (NumberFormatException e29) {
                                                                str = name;
                                                                str7 = codePathStr;
                                                                str8 = resourcePathStr2;
                                                                str4 = version;
                                                                j2 = timeStamp2;
                                                                j = firstInstallTime2;
                                                                lastUpdateTime3 = lastUpdateTime2;
                                                                categoryHintString = categoryHintString3;
                                                                settings = this;
                                                                idStr = idStr2;
                                                                sharedIdStr = sharedIdStr2;
                                                                i = 5;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("Error in package manager settings: package ");
                                                                stringBuilder.append(name);
                                                                stringBuilder.append(" has bad userId ");
                                                                stringBuilder.append(idStr);
                                                                stringBuilder.append(" at ");
                                                                stringBuilder.append(parser.getPositionDescription());
                                                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                str = name;
                                                                systemStr = idStr;
                                                                str2 = codePathStr;
                                                                str4 = version;
                                                                version = updateAvailable;
                                                                packageSetting = packageSetting2;
                                                                idStr = installerPackageName;
                                                                realName = uidError;
                                                                sharedIdStr = isOrphaned;
                                                                codePathStr = volumeUuid;
                                                                resourcePathStr2 = resourcePathStr;
                                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                categoryHint2 = categoryHint;
                                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                                if (packageSetting != null) {
                                                                }
                                                            }
                                                        } catch (NumberFormatException e30) {
                                                            str = name;
                                                            str7 = codePathStr;
                                                            str8 = resourcePathStr2;
                                                            str4 = version;
                                                            j2 = timeStamp2;
                                                            j = firstInstallTime2;
                                                            sharedUserId = lastUpdateTime2;
                                                            categoryHintString = categoryHintString3;
                                                            settings = this;
                                                            idStr = idStr2;
                                                            sharedIdStr = sharedIdStr;
                                                            i = 5;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("Error in package manager settings: package ");
                                                            stringBuilder.append(name);
                                                            stringBuilder.append(" has bad userId ");
                                                            stringBuilder.append(idStr);
                                                            stringBuilder.append(" at ");
                                                            stringBuilder.append(parser.getPositionDescription());
                                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                            str = name;
                                                            systemStr = idStr;
                                                            str2 = codePathStr;
                                                            str4 = version;
                                                            version = updateAvailable;
                                                            packageSetting = packageSetting2;
                                                            idStr = installerPackageName;
                                                            realName = uidError;
                                                            sharedIdStr = isOrphaned;
                                                            codePathStr = volumeUuid;
                                                            resourcePathStr2 = resourcePathStr;
                                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                            categoryHint2 = categoryHint;
                                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                                            if (packageSetting != null) {
                                                            }
                                                        }
                                                        try {
                                                            packageSetting = addPackageLPw(name.intern(), str10, new File(codePathStr), new File(resourcePathStr2), resourcePathStr, cpuAbiOverrideString, secondaryCpuAbiString, cpuAbiOverrideString2, pkgFlags2, str, pkgFlags, pkgPrivateFlags, parentPackageName, null, null, null);
                                                            try {
                                                                StringBuilder stringBuilder3;
                                                                if (PackageManagerService.DEBUG_SETTINGS) {
                                                                    try {
                                                                        realName = "PackageManager";
                                                                        stringBuilder3 = new StringBuilder();
                                                                        stringBuilder3.append("Reading package ");
                                                                        str = name2;
                                                                        try {
                                                                            stringBuilder3.append(str);
                                                                            stringBuilder3.append(": userId=");
                                                                            stringBuilder3.append(pkgFlags2);
                                                                            stringBuilder3.append(" pkg=");
                                                                            stringBuilder3.append(packageSetting);
                                                                            Log.i(realName, stringBuilder3.toString());
                                                                        } catch (NumberFormatException e31) {
                                                                            packageSetting2 = packageSetting;
                                                                            name = str;
                                                                            version = str4;
                                                                            idStr = str5;
                                                                            sharedIdStr = str6;
                                                                            codePathStr = str7;
                                                                            resourcePathStr2 = str8;
                                                                            settings = this;
                                                                            i = 5;
                                                                            stringBuilder = new StringBuilder();
                                                                            stringBuilder.append("Error in package manager settings: package ");
                                                                            stringBuilder.append(name);
                                                                            stringBuilder.append(" has bad userId ");
                                                                            stringBuilder.append(idStr);
                                                                            stringBuilder.append(" at ");
                                                                            stringBuilder.append(parser.getPositionDescription());
                                                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                            str = name;
                                                                            systemStr = idStr;
                                                                            str2 = codePathStr;
                                                                            str4 = version;
                                                                            version = updateAvailable;
                                                                            packageSetting = packageSetting2;
                                                                            idStr = installerPackageName;
                                                                            realName = uidError;
                                                                            sharedIdStr = isOrphaned;
                                                                            codePathStr = volumeUuid;
                                                                            resourcePathStr2 = resourcePathStr;
                                                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                            categoryHint2 = categoryHint;
                                                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                                                            if (packageSetting != null) {
                                                                            }
                                                                        }
                                                                    } catch (NumberFormatException e32) {
                                                                        packageSetting2 = packageSetting;
                                                                        name = name2;
                                                                        version = str4;
                                                                        idStr = str5;
                                                                        sharedIdStr = str6;
                                                                        codePathStr = str7;
                                                                        resourcePathStr2 = str8;
                                                                        settings = this;
                                                                        i = 5;
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("Error in package manager settings: package ");
                                                                        stringBuilder.append(name);
                                                                        stringBuilder.append(" has bad userId ");
                                                                        stringBuilder.append(idStr);
                                                                        stringBuilder.append(" at ");
                                                                        stringBuilder.append(parser.getPositionDescription());
                                                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                        str = name;
                                                                        systemStr = idStr;
                                                                        str2 = codePathStr;
                                                                        str4 = version;
                                                                        version = updateAvailable;
                                                                        packageSetting = packageSetting2;
                                                                        idStr = installerPackageName;
                                                                        realName = uidError;
                                                                        sharedIdStr = isOrphaned;
                                                                        codePathStr = volumeUuid;
                                                                        resourcePathStr2 = resourcePathStr;
                                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                        categoryHint2 = categoryHint;
                                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                                        if (packageSetting != null) {
                                                                        }
                                                                    }
                                                                } else {
                                                                    str = name2;
                                                                }
                                                                if (packageSetting == null) {
                                                                    stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append("Failure adding uid ");
                                                                    stringBuilder3.append(pkgFlags2);
                                                                    stringBuilder3.append(" while parsing settings at ");
                                                                    stringBuilder3.append(parser.getPositionDescription());
                                                                    PackageManagerService.reportSettingsProblem(6, stringBuilder3.toString());
                                                                    j2 = timeStamp2;
                                                                    j = firstInstallTime2;
                                                                    lastUpdateTime3 = lastUpdateTime2;
                                                                } else {
                                                                    j2 = timeStamp2;
                                                                    try {
                                                                        packageSetting.setTimeStamp(j2);
                                                                        lastUpdateTime3 = firstInstallTime2;
                                                                        try {
                                                                            packageSetting.firstInstallTime = lastUpdateTime3;
                                                                            j = lastUpdateTime3;
                                                                            lastUpdateTime3 = lastUpdateTime2;
                                                                            try {
                                                                                packageSetting.lastUpdateTime = lastUpdateTime3;
                                                                            } catch (NumberFormatException e33) {
                                                                                packageSetting2 = packageSetting;
                                                                                timeStamp2 = j2;
                                                                                name = str;
                                                                                lastUpdateTime2 = lastUpdateTime3;
                                                                                version = str4;
                                                                                idStr = str5;
                                                                                sharedIdStr = str6;
                                                                                codePathStr = str7;
                                                                                resourcePathStr2 = str8;
                                                                                firstInstallTime2 = j;
                                                                                settings = this;
                                                                                i = 5;
                                                                                stringBuilder = new StringBuilder();
                                                                                stringBuilder.append("Error in package manager settings: package ");
                                                                                stringBuilder.append(name);
                                                                                stringBuilder.append(" has bad userId ");
                                                                                stringBuilder.append(idStr);
                                                                                stringBuilder.append(" at ");
                                                                                stringBuilder.append(parser.getPositionDescription());
                                                                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                                str = name;
                                                                                systemStr = idStr;
                                                                                str2 = codePathStr;
                                                                                str4 = version;
                                                                                version = updateAvailable;
                                                                                packageSetting = packageSetting2;
                                                                                idStr = installerPackageName;
                                                                                realName = uidError;
                                                                                sharedIdStr = isOrphaned;
                                                                                codePathStr = volumeUuid;
                                                                                resourcePathStr2 = resourcePathStr;
                                                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                                categoryHint2 = categoryHint;
                                                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                                                if (packageSetting != null) {
                                                                                }
                                                                            }
                                                                        } catch (NumberFormatException e34) {
                                                                            j = lastUpdateTime3;
                                                                            packageSetting2 = packageSetting;
                                                                            timeStamp2 = j2;
                                                                            name = str;
                                                                            version = str4;
                                                                            idStr = str5;
                                                                            sharedIdStr = str6;
                                                                            codePathStr = str7;
                                                                            resourcePathStr2 = str8;
                                                                            firstInstallTime2 = j;
                                                                            settings = this;
                                                                            i = 5;
                                                                            stringBuilder = new StringBuilder();
                                                                            stringBuilder.append("Error in package manager settings: package ");
                                                                            stringBuilder.append(name);
                                                                            stringBuilder.append(" has bad userId ");
                                                                            stringBuilder.append(idStr);
                                                                            stringBuilder.append(" at ");
                                                                            stringBuilder.append(parser.getPositionDescription());
                                                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                            str = name;
                                                                            systemStr = idStr;
                                                                            str2 = codePathStr;
                                                                            str4 = version;
                                                                            version = updateAvailable;
                                                                            packageSetting = packageSetting2;
                                                                            idStr = installerPackageName;
                                                                            realName = uidError;
                                                                            sharedIdStr = isOrphaned;
                                                                            codePathStr = volumeUuid;
                                                                            resourcePathStr2 = resourcePathStr;
                                                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                            categoryHint2 = categoryHint;
                                                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                                                            if (packageSetting != null) {
                                                                            }
                                                                        }
                                                                    } catch (NumberFormatException e35) {
                                                                        j = firstInstallTime2;
                                                                        lastUpdateTime3 = lastUpdateTime2;
                                                                        packageSetting2 = packageSetting;
                                                                        timeStamp2 = j2;
                                                                        name = str;
                                                                        version = str4;
                                                                        idStr = str5;
                                                                        sharedIdStr = str6;
                                                                        codePathStr = str7;
                                                                        resourcePathStr2 = str8;
                                                                        settings = this;
                                                                        i = 5;
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("Error in package manager settings: package ");
                                                                        stringBuilder.append(name);
                                                                        stringBuilder.append(" has bad userId ");
                                                                        stringBuilder.append(idStr);
                                                                        stringBuilder.append(" at ");
                                                                        stringBuilder.append(parser.getPositionDescription());
                                                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                        str = name;
                                                                        systemStr = idStr;
                                                                        str2 = codePathStr;
                                                                        str4 = version;
                                                                        version = updateAvailable;
                                                                        packageSetting = packageSetting2;
                                                                        idStr = installerPackageName;
                                                                        realName = uidError;
                                                                        sharedIdStr = isOrphaned;
                                                                        codePathStr = volumeUuid;
                                                                        resourcePathStr2 = resourcePathStr;
                                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                        categoryHint2 = categoryHint;
                                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                                        if (packageSetting != null) {
                                                                        }
                                                                    }
                                                                }
                                                                packageSetting2 = packageSetting;
                                                                timeStamp3 = j2;
                                                                sharedIdStr = str5;
                                                                name = str6;
                                                                str2 = str7;
                                                                resourcePathStr3 = str8;
                                                                firstInstallTime3 = j;
                                                                i2 = 5;
                                                                version = this;
                                                                parentPackageName = name;
                                                                resourcePathStr2 = resourcePathStr;
                                                                str11 = cpuAbiOverrideString2;
                                                                lastUpdateTime2 = lastUpdateTime3;
                                                                str12 = resourcePathStr3;
                                                                categoryHint2 = categoryHint;
                                                                packageSetting = packageSetting2;
                                                                realName = uidError;
                                                                codePathStr = volumeUuid;
                                                                timeStamp2 = timeStamp3;
                                                                firstInstallTime2 = firstInstallTime3;
                                                                resourcePathStr3 = i2;
                                                                systemStr = sharedIdStr;
                                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                settings = version;
                                                                version = updateAvailable;
                                                                idStr = installerPackageName;
                                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                                sharedIdStr = isOrphaned;
                                                            } catch (NumberFormatException e36) {
                                                                j2 = timeStamp2;
                                                                j = firstInstallTime2;
                                                                lastUpdateTime3 = lastUpdateTime2;
                                                                str = name2;
                                                                packageSetting2 = packageSetting;
                                                                name = str;
                                                                version = str4;
                                                                idStr = str5;
                                                                sharedIdStr = str6;
                                                                codePathStr = str7;
                                                                resourcePathStr2 = str8;
                                                                settings = this;
                                                                i = 5;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("Error in package manager settings: package ");
                                                                stringBuilder.append(name);
                                                                stringBuilder.append(" has bad userId ");
                                                                stringBuilder.append(idStr);
                                                                stringBuilder.append(" at ");
                                                                stringBuilder.append(parser.getPositionDescription());
                                                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                str = name;
                                                                systemStr = idStr;
                                                                str2 = codePathStr;
                                                                str4 = version;
                                                                version = updateAvailable;
                                                                packageSetting = packageSetting2;
                                                                idStr = installerPackageName;
                                                                realName = uidError;
                                                                sharedIdStr = isOrphaned;
                                                                codePathStr = volumeUuid;
                                                                resourcePathStr2 = resourcePathStr;
                                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                categoryHint2 = categoryHint;
                                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                                if (packageSetting != null) {
                                                                }
                                                            }
                                                        } catch (NumberFormatException e37) {
                                                            j2 = timeStamp2;
                                                            j = firstInstallTime2;
                                                            lastUpdateTime3 = lastUpdateTime2;
                                                            str = name2;
                                                            name = str;
                                                            version = str4;
                                                            idStr = str5;
                                                            sharedIdStr = str6;
                                                            codePathStr = str7;
                                                            resourcePathStr2 = str8;
                                                            settings = this;
                                                            i = 5;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("Error in package manager settings: package ");
                                                            stringBuilder.append(name);
                                                            stringBuilder.append(" has bad userId ");
                                                            stringBuilder.append(idStr);
                                                            stringBuilder.append(" at ");
                                                            stringBuilder.append(parser.getPositionDescription());
                                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                            str = name;
                                                            systemStr = idStr;
                                                            str2 = codePathStr;
                                                            str4 = version;
                                                            version = updateAvailable;
                                                            packageSetting = packageSetting2;
                                                            idStr = installerPackageName;
                                                            realName = uidError;
                                                            sharedIdStr = isOrphaned;
                                                            codePathStr = volumeUuid;
                                                            resourcePathStr2 = resourcePathStr;
                                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                            categoryHint2 = categoryHint;
                                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                                            if (packageSetting != null) {
                                                            }
                                                        }
                                                        XmlPullParser xmlPullParser2;
                                                        int i4;
                                                        if (packageSetting != null) {
                                                            int type;
                                                            packageSetting.uidError = "true".equals(realName);
                                                            packageSetting.installerPackageName = idStr;
                                                            packageSetting.isOrphaned = "true".equals(sharedIdStr);
                                                            packageSetting.volumeUuid = codePathStr;
                                                            packageSetting.categoryHint = categoryHint2;
                                                            packageSetting.legacyNativeLibraryPathString = resourcePathStr2;
                                                            packageSetting.primaryCpuAbiString = secondaryCpuAbiString;
                                                            packageSetting.secondaryCpuAbiString = cpuAbiOverrideString2;
                                                            packageSetting.updateAvailable = "true".equals(version);
                                                            xmlPullParser2 = parser;
                                                            realName = null;
                                                            sharedIdStr = xmlPullParser2.getAttributeValue(null, ATTR_ENABLED);
                                                            if (sharedIdStr != null) {
                                                                try {
                                                                    volumeUuid2 = 0;
                                                                    try {
                                                                        packageSetting.setEnabled(Integer.parseInt(sharedIdStr), 0, null);
                                                                        i4 = categoryHint2;
                                                                    } catch (NumberFormatException e38) {
                                                                        if (sharedIdStr.equalsIgnoreCase("true")) {
                                                                        }
                                                                        realName = null;
                                                                        if (idStr != null) {
                                                                        }
                                                                        str13 = xmlPullParser2.getAttributeValue(realName, "maxAspectRatio");
                                                                        if (str13 != null) {
                                                                        }
                                                                        realName = xmlPullParser2.getAttributeValue(null, "appUseNotchMode");
                                                                        if (realName != null) {
                                                                        }
                                                                        categoryHint2 = parser.getDepth();
                                                                        while (true) {
                                                                            volumeUuid2 = parser.next();
                                                                            type = volumeUuid2;
                                                                            appUseNotchModeStr = realName;
                                                                            if (volumeUuid2 != 1) {
                                                                            }
                                                                            realName = appUseNotchModeStr;
                                                                            str13 = maxAspectRatioStr;
                                                                            idStr = installerPackageName2;
                                                                            sharedIdStr = enabledStr;
                                                                            categoryHint2 = outerDepth;
                                                                            resourcePathStr2 = legacyNativeLibraryPathStr;
                                                                            secondaryCpuAbiString = primaryCpuAbiString;
                                                                        }
                                                                    }
                                                                } catch (NumberFormatException e39) {
                                                                    str9 = codePathStr;
                                                                    volumeUuid2 = 0;
                                                                    if (sharedIdStr.equalsIgnoreCase("true")) {
                                                                        packageSetting.setEnabled(1, volumeUuid2, null);
                                                                    } else {
                                                                        if (sharedIdStr.equalsIgnoreCase("false")) {
                                                                            packageSetting.setEnabled(2, volumeUuid2, null);
                                                                        } else if (sharedIdStr.equalsIgnoreCase(HealthServiceWrapper.INSTANCE_VENDOR)) {
                                                                            packageSetting.setEnabled(volumeUuid2, volumeUuid2, null);
                                                                        } else {
                                                                            stringBuilder = new StringBuilder();
                                                                            stringBuilder.append("Error in package manager settings: package ");
                                                                            stringBuilder.append(str);
                                                                            stringBuilder.append(" has bad enabled value: ");
                                                                            stringBuilder.append(systemStr);
                                                                            stringBuilder.append(" at ");
                                                                            stringBuilder.append(parser.getPositionDescription());
                                                                            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                                                                        }
                                                                    }
                                                                    realName = null;
                                                                    if (idStr != null) {
                                                                    }
                                                                    str13 = xmlPullParser2.getAttributeValue(realName, "maxAspectRatio");
                                                                    if (str13 != null) {
                                                                    }
                                                                    realName = xmlPullParser2.getAttributeValue(null, "appUseNotchMode");
                                                                    if (realName != null) {
                                                                    }
                                                                    categoryHint2 = parser.getDepth();
                                                                    while (true) {
                                                                        volumeUuid2 = parser.next();
                                                                        type = volumeUuid2;
                                                                        appUseNotchModeStr = realName;
                                                                        if (volumeUuid2 != 1) {
                                                                        }
                                                                        realName = appUseNotchModeStr;
                                                                        str13 = maxAspectRatioStr;
                                                                        idStr = installerPackageName2;
                                                                        sharedIdStr = enabledStr;
                                                                        categoryHint2 = outerDepth;
                                                                        resourcePathStr2 = legacyNativeLibraryPathStr;
                                                                        secondaryCpuAbiString = primaryCpuAbiString;
                                                                    }
                                                                }
                                                            } else {
                                                                i4 = categoryHint2;
                                                                realName = null;
                                                                packageSetting.setEnabled(0, 0, null);
                                                            }
                                                            if (idStr != null) {
                                                                settings.mInstallerPackages.add(idStr);
                                                            }
                                                            str13 = xmlPullParser2.getAttributeValue(realName, "maxAspectRatio");
                                                            if (str13 != null) {
                                                                float maxAspectRatio = Float.parseFloat(str13);
                                                                if (maxAspectRatio > 0.0f) {
                                                                    packageSetting.maxAspectRatio = maxAspectRatio;
                                                                }
                                                            }
                                                            realName = xmlPullParser2.getAttributeValue(null, "appUseNotchMode");
                                                            if (realName != null) {
                                                                packageSetting.appUseNotchMode = Integer.parseInt(realName);
                                                            }
                                                            categoryHint2 = parser.getDepth();
                                                            while (true) {
                                                                volumeUuid2 = parser.next();
                                                                type = volumeUuid2;
                                                                appUseNotchModeStr = realName;
                                                                if (volumeUuid2 != 1) {
                                                                    volumeUuid2 = type;
                                                                    if (volumeUuid2 != 3 || parser.getDepth() > categoryHint2) {
                                                                        if (volumeUuid2 == 3) {
                                                                            maxAspectRatioStr = str13;
                                                                            installerPackageName2 = idStr;
                                                                            enabledStr = sharedIdStr;
                                                                            outerDepth = categoryHint2;
                                                                            legacyNativeLibraryPathStr = resourcePathStr2;
                                                                            primaryCpuAbiString = secondaryCpuAbiString;
                                                                        } else if (volumeUuid2 == 4) {
                                                                            maxAspectRatioStr = str13;
                                                                            installerPackageName2 = idStr;
                                                                            enabledStr = sharedIdStr;
                                                                            outerDepth = categoryHint2;
                                                                            legacyNativeLibraryPathStr = resourcePathStr2;
                                                                            primaryCpuAbiString = secondaryCpuAbiString;
                                                                        } else {
                                                                            realName = parser.getName();
                                                                            maxAspectRatioStr = str13;
                                                                            if (realName.equals(TAG_DISABLED_COMPONENTS) != null) {
                                                                                installerPackageName2 = idStr;
                                                                                settings.readDisabledComponentsLPw(packageSetting, xmlPullParser2, null);
                                                                            } else {
                                                                                installerPackageName2 = idStr;
                                                                                int i5;
                                                                                if (realName.equals(TAG_ENABLED_COMPONENTS) != null) {
                                                                                    settings.readEnabledComponentsLPw(packageSetting, xmlPullParser2, 0);
                                                                                } else if (realName.equals("sigs") != null) {
                                                                                    packageSetting.signatures.readXml(xmlPullParser2, settings.mPastSignatures);
                                                                                } else if (realName.equals(TAG_PERMISSIONS) != null) {
                                                                                    settings.readInstallPermissionsLPr(xmlPullParser2, packageSetting.getPermissionsState());
                                                                                    packageSetting.installPermissionsFixed = true;
                                                                                    enabledStr = sharedIdStr;
                                                                                    i5 = volumeUuid2;
                                                                                    outerDepth = categoryHint2;
                                                                                    legacyNativeLibraryPathStr = resourcePathStr2;
                                                                                    primaryCpuAbiString = secondaryCpuAbiString;
                                                                                } else {
                                                                                    if (realName.equals("proper-signing-keyset") != null) {
                                                                                        enabledStr = sharedIdStr;
                                                                                        j2 = Long.parseLong(xmlPullParser2.getAttributeValue(null, "identifier"));
                                                                                        i5 = volumeUuid2;
                                                                                        Integer maxAspectRatioStr2 = (Integer) settings.mKeySetRefs.get(Long.valueOf(j2));
                                                                                        Integer refCt;
                                                                                        if (maxAspectRatioStr2 != null) {
                                                                                            outerDepth = categoryHint2;
                                                                                            refCt = maxAspectRatioStr2;
                                                                                            legacyNativeLibraryPathStr = resourcePathStr2;
                                                                                            settings.mKeySetRefs.put(Long.valueOf(j2), Integer.valueOf(maxAspectRatioStr2.intValue() + 1));
                                                                                        } else {
                                                                                            refCt = maxAspectRatioStr2;
                                                                                            outerDepth = categoryHint2;
                                                                                            legacyNativeLibraryPathStr = resourcePathStr2;
                                                                                            settings.mKeySetRefs.put(Long.valueOf(j2), Integer.valueOf(1));
                                                                                        }
                                                                                        packageSetting.keySetData.setProperSigningKeySet(j2);
                                                                                    } else {
                                                                                        enabledStr = sharedIdStr;
                                                                                        i5 = volumeUuid2;
                                                                                        outerDepth = categoryHint2;
                                                                                        legacyNativeLibraryPathStr = resourcePathStr2;
                                                                                        if (realName.equals("signing-keyset") == null) {
                                                                                            if (realName.equals("upgrade-keyset") != null) {
                                                                                                packageSetting.keySetData.addUpgradeKeySetById(Long.parseLong(xmlPullParser2.getAttributeValue(null, "identifier")));
                                                                                            } else if (realName.equals("defined-keyset") != null) {
                                                                                                long id = Long.parseLong(xmlPullParser2.getAttributeValue(null, "identifier"));
                                                                                                str13 = xmlPullParser2.getAttributeValue(null, "alias");
                                                                                                Integer refCt2 = (Integer) settings.mKeySetRefs.get(Long.valueOf(id));
                                                                                                if (refCt2 != null) {
                                                                                                    primaryCpuAbiString = secondaryCpuAbiString;
                                                                                                    settings.mKeySetRefs.put(Long.valueOf(id), Integer.valueOf(refCt2.intValue() + 1));
                                                                                                } else {
                                                                                                    primaryCpuAbiString = secondaryCpuAbiString;
                                                                                                    settings.mKeySetRefs.put(Long.valueOf(id), Integer.valueOf(1));
                                                                                                }
                                                                                                packageSetting.keySetData.addDefinedKeySet(id, str13);
                                                                                            } else {
                                                                                                primaryCpuAbiString = secondaryCpuAbiString;
                                                                                                if (realName.equals(TAG_DOMAIN_VERIFICATION) != null) {
                                                                                                    settings.readDomainVerificationLPw(xmlPullParser2, packageSetting);
                                                                                                } else if (realName.equals(TAG_CHILD_PACKAGE) != null) {
                                                                                                    str13 = xmlPullParser2.getAttributeValue(null, ATTR_NAME);
                                                                                                    if (packageSetting.childPackageNames == null) {
                                                                                                        packageSetting.childPackageNames = new ArrayList();
                                                                                                    }
                                                                                                    packageSetting.childPackageNames.add(str13);
                                                                                                } else {
                                                                                                    str13 = new StringBuilder();
                                                                                                    str13.append("Unknown element under <package>: ");
                                                                                                    str13.append(parser.getName());
                                                                                                    PackageManagerService.reportSettingsProblem(5, str13.toString());
                                                                                                    XmlUtils.skipCurrentTag(parser);
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    primaryCpuAbiString = secondaryCpuAbiString;
                                                                                }
                                                                            }
                                                                            enabledStr = sharedIdStr;
                                                                            outerDepth = categoryHint2;
                                                                            legacyNativeLibraryPathStr = resourcePathStr2;
                                                                            primaryCpuAbiString = secondaryCpuAbiString;
                                                                        }
                                                                        realName = appUseNotchModeStr;
                                                                        str13 = maxAspectRatioStr;
                                                                        idStr = installerPackageName2;
                                                                        sharedIdStr = enabledStr;
                                                                        categoryHint2 = outerDepth;
                                                                        resourcePathStr2 = legacyNativeLibraryPathStr;
                                                                        secondaryCpuAbiString = primaryCpuAbiString;
                                                                    } else {
                                                                        installerPackageName2 = idStr;
                                                                        legacyNativeLibraryPathStr = resourcePathStr2;
                                                                        primaryCpuAbiString = secondaryCpuAbiString;
                                                                        return;
                                                                    }
                                                                }
                                                                legacyNativeLibraryPathStr = resourcePathStr2;
                                                                primaryCpuAbiString = secondaryCpuAbiString;
                                                                return;
                                                            }
                                                        }
                                                        xmlPullParser2 = parser;
                                                        String str14 = realName;
                                                        installerPackageName2 = idStr;
                                                        String str15 = sharedIdStr;
                                                        str9 = codePathStr;
                                                        i4 = categoryHint2;
                                                        legacyNativeLibraryPathStr = resourcePathStr2;
                                                        primaryCpuAbiString = secondaryCpuAbiString;
                                                        XmlUtils.skipCurrentTag(parser);
                                                        return;
                                                    } else {
                                                        str7 = codePathStr;
                                                        str8 = resourcePathStr2;
                                                        str4 = version;
                                                        String str16 = str;
                                                        int sharedUserId2 = sharedUserId;
                                                        j2 = timeStamp2;
                                                        j = firstInstallTime2;
                                                        lastUpdateTime3 = lastUpdateTime2;
                                                        categoryHintString = categoryHintString3;
                                                        str5 = idStr2;
                                                        str = name;
                                                        name = sharedIdStr;
                                                        int sharedUserId3;
                                                        StringBuilder stringBuilder4;
                                                        int i6;
                                                        if (name != null) {
                                                            sharedUserId3 = sharedUserId2;
                                                            if (sharedUserId3 > 0) {
                                                                try {
                                                                    String intern = str.intern();
                                                                    resourcePathStr3 = str7;
                                                                    try {
                                                                        resourcePathStr2 = new File(resourcePathStr3);
                                                                        str2 = resourcePathStr3;
                                                                        resourcePathStr3 = str8;
                                                                        try {
                                                                            String packageSetting3 = new PackageSetting(intern, str10, resourcePathStr2, new File(resourcePathStr3), resourcePathStr, cpuAbiOverrideString, secondaryCpuAbiString, cpuAbiOverrideString2, versionCode, pkgFlags, pkgPrivateFlags, parentPackageName, null, sharedUserId3, null, null);
                                                                            try {
                                                                                packageSetting3.setTimeStamp(j2);
                                                                                timeStamp3 = j2;
                                                                                j2 = j;
                                                                                try {
                                                                                    packageSetting3.firstInstallTime = j2;
                                                                                    packageSetting3.lastUpdateTime = lastUpdateTime3;
                                                                                    version = this;
                                                                                    try {
                                                                                        version.mPendingPackages.add(packageSetting3);
                                                                                        if (PackageManagerService.DEBUG_SETTINGS != null) {
                                                                                            resourcePathStr2 = "PackageManager";
                                                                                            stringBuilder4 = new StringBuilder();
                                                                                            firstInstallTime3 = j2;
                                                                                            try {
                                                                                                stringBuilder4.append("Reading package ");
                                                                                                stringBuilder4.append(str);
                                                                                                stringBuilder4.append(": sharedUserId=");
                                                                                                stringBuilder4.append(sharedUserId3);
                                                                                                stringBuilder4.append(" pkg=");
                                                                                                stringBuilder4.append(packageSetting3);
                                                                                                Log.i(resourcePathStr2, stringBuilder4.toString());
                                                                                            } catch (NumberFormatException e40) {
                                                                                                sharedIdStr = name;
                                                                                                packageSetting2 = packageSetting3;
                                                                                            }
                                                                                        } else {
                                                                                            firstInstallTime3 = j2;
                                                                                        }
                                                                                        packageSetting2 = packageSetting3;
                                                                                        sharedIdStr = str5;
                                                                                        i2 = 5;
                                                                                        parentPackageName = name;
                                                                                        resourcePathStr2 = resourcePathStr;
                                                                                        str11 = cpuAbiOverrideString2;
                                                                                        lastUpdateTime2 = lastUpdateTime3;
                                                                                        str12 = resourcePathStr3;
                                                                                        categoryHint2 = categoryHint;
                                                                                        packageSetting = packageSetting2;
                                                                                        realName = uidError;
                                                                                        codePathStr = volumeUuid;
                                                                                        timeStamp2 = timeStamp3;
                                                                                        firstInstallTime2 = firstInstallTime3;
                                                                                        resourcePathStr3 = i2;
                                                                                        systemStr = sharedIdStr;
                                                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                                        settings = version;
                                                                                        version = updateAvailable;
                                                                                        idStr = installerPackageName;
                                                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                                                        sharedIdStr = isOrphaned;
                                                                                    } catch (NumberFormatException e41) {
                                                                                        firstInstallTime3 = j2;
                                                                                        sharedIdStr = name;
                                                                                        packageSetting2 = packageSetting3;
                                                                                        name = str;
                                                                                        lastUpdateTime2 = lastUpdateTime3;
                                                                                        resourcePathStr2 = resourcePathStr3;
                                                                                        idStr = str5;
                                                                                        codePathStr = str2;
                                                                                        timeStamp2 = timeStamp3;
                                                                                        firstInstallTime2 = firstInstallTime3;
                                                                                        i = 5;
                                                                                        settings = version;
                                                                                        version = str4;
                                                                                        stringBuilder = new StringBuilder();
                                                                                        stringBuilder.append("Error in package manager settings: package ");
                                                                                        stringBuilder.append(name);
                                                                                        stringBuilder.append(" has bad userId ");
                                                                                        stringBuilder.append(idStr);
                                                                                        stringBuilder.append(" at ");
                                                                                        stringBuilder.append(parser.getPositionDescription());
                                                                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                                        str = name;
                                                                                        systemStr = idStr;
                                                                                        str2 = codePathStr;
                                                                                        str4 = version;
                                                                                        version = updateAvailable;
                                                                                        packageSetting = packageSetting2;
                                                                                        idStr = installerPackageName;
                                                                                        realName = uidError;
                                                                                        sharedIdStr = isOrphaned;
                                                                                        codePathStr = volumeUuid;
                                                                                        resourcePathStr2 = resourcePathStr;
                                                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                                        categoryHint2 = categoryHint;
                                                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                                                        if (packageSetting != null) {
                                                                                        }
                                                                                    }
                                                                                } catch (NumberFormatException e42) {
                                                                                    firstInstallTime3 = j2;
                                                                                    sharedIdStr = name;
                                                                                    packageSetting2 = packageSetting3;
                                                                                    name = str;
                                                                                    lastUpdateTime2 = lastUpdateTime3;
                                                                                    resourcePathStr2 = resourcePathStr3;
                                                                                    version = str4;
                                                                                    idStr = str5;
                                                                                    codePathStr = str2;
                                                                                    timeStamp2 = timeStamp3;
                                                                                    firstInstallTime2 = firstInstallTime3;
                                                                                    settings = this;
                                                                                    i = 5;
                                                                                    stringBuilder = new StringBuilder();
                                                                                    stringBuilder.append("Error in package manager settings: package ");
                                                                                    stringBuilder.append(name);
                                                                                    stringBuilder.append(" has bad userId ");
                                                                                    stringBuilder.append(idStr);
                                                                                    stringBuilder.append(" at ");
                                                                                    stringBuilder.append(parser.getPositionDescription());
                                                                                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                                    str = name;
                                                                                    systemStr = idStr;
                                                                                    str2 = codePathStr;
                                                                                    str4 = version;
                                                                                    version = updateAvailable;
                                                                                    packageSetting = packageSetting2;
                                                                                    idStr = installerPackageName;
                                                                                    realName = uidError;
                                                                                    sharedIdStr = isOrphaned;
                                                                                    codePathStr = volumeUuid;
                                                                                    resourcePathStr2 = resourcePathStr;
                                                                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                                    categoryHint2 = categoryHint;
                                                                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                                                                    if (packageSetting != null) {
                                                                                    }
                                                                                }
                                                                            } catch (NumberFormatException e43) {
                                                                                timeStamp3 = j2;
                                                                                firstInstallTime3 = j;
                                                                                sharedIdStr = name;
                                                                                packageSetting2 = packageSetting3;
                                                                                name = str;
                                                                                resourcePathStr2 = resourcePathStr3;
                                                                                version = str4;
                                                                                idStr = str5;
                                                                                codePathStr = str2;
                                                                                timeStamp2 = timeStamp3;
                                                                                firstInstallTime2 = firstInstallTime3;
                                                                                settings = this;
                                                                                i = 5;
                                                                                stringBuilder = new StringBuilder();
                                                                                stringBuilder.append("Error in package manager settings: package ");
                                                                                stringBuilder.append(name);
                                                                                stringBuilder.append(" has bad userId ");
                                                                                stringBuilder.append(idStr);
                                                                                stringBuilder.append(" at ");
                                                                                stringBuilder.append(parser.getPositionDescription());
                                                                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                                str = name;
                                                                                systemStr = idStr;
                                                                                str2 = codePathStr;
                                                                                str4 = version;
                                                                                version = updateAvailable;
                                                                                packageSetting = packageSetting2;
                                                                                idStr = installerPackageName;
                                                                                realName = uidError;
                                                                                sharedIdStr = isOrphaned;
                                                                                codePathStr = volumeUuid;
                                                                                resourcePathStr2 = resourcePathStr;
                                                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                                categoryHint2 = categoryHint;
                                                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                                                if (packageSetting != null) {
                                                                                }
                                                                            }
                                                                        } catch (NumberFormatException e44) {
                                                                            timeStamp3 = j2;
                                                                            firstInstallTime3 = j;
                                                                            sharedIdStr = name;
                                                                            name = str;
                                                                            resourcePathStr2 = resourcePathStr3;
                                                                            version = str4;
                                                                            idStr = str5;
                                                                            codePathStr = str2;
                                                                            timeStamp2 = timeStamp3;
                                                                            firstInstallTime2 = firstInstallTime3;
                                                                            settings = this;
                                                                            i = 5;
                                                                            stringBuilder = new StringBuilder();
                                                                            stringBuilder.append("Error in package manager settings: package ");
                                                                            stringBuilder.append(name);
                                                                            stringBuilder.append(" has bad userId ");
                                                                            stringBuilder.append(idStr);
                                                                            stringBuilder.append(" at ");
                                                                            stringBuilder.append(parser.getPositionDescription());
                                                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                            str = name;
                                                                            systemStr = idStr;
                                                                            str2 = codePathStr;
                                                                            str4 = version;
                                                                            version = updateAvailable;
                                                                            packageSetting = packageSetting2;
                                                                            idStr = installerPackageName;
                                                                            realName = uidError;
                                                                            sharedIdStr = isOrphaned;
                                                                            codePathStr = volumeUuid;
                                                                            resourcePathStr2 = resourcePathStr;
                                                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                            categoryHint2 = categoryHint;
                                                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                                                            if (packageSetting != null) {
                                                                            }
                                                                        }
                                                                    } catch (NumberFormatException e45) {
                                                                        timeStamp3 = j2;
                                                                        sharedIdStr = name;
                                                                        name = str;
                                                                        lastUpdateTime2 = lastUpdateTime3;
                                                                        resourcePathStr2 = str8;
                                                                        version = str4;
                                                                        idStr = str5;
                                                                        codePathStr = resourcePathStr3;
                                                                        timeStamp2 = timeStamp3;
                                                                        firstInstallTime2 = j;
                                                                        settings = this;
                                                                        i = 5;
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("Error in package manager settings: package ");
                                                                        stringBuilder.append(name);
                                                                        stringBuilder.append(" has bad userId ");
                                                                        stringBuilder.append(idStr);
                                                                        stringBuilder.append(" at ");
                                                                        stringBuilder.append(parser.getPositionDescription());
                                                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                        str = name;
                                                                        systemStr = idStr;
                                                                        str2 = codePathStr;
                                                                        str4 = version;
                                                                        version = updateAvailable;
                                                                        packageSetting = packageSetting2;
                                                                        idStr = installerPackageName;
                                                                        realName = uidError;
                                                                        sharedIdStr = isOrphaned;
                                                                        codePathStr = volumeUuid;
                                                                        resourcePathStr2 = resourcePathStr;
                                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                        categoryHint2 = categoryHint;
                                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                                        if (packageSetting != null) {
                                                                        }
                                                                    }
                                                                } catch (NumberFormatException e46) {
                                                                    timeStamp3 = j2;
                                                                    sharedIdStr = name;
                                                                    name = str;
                                                                    lastUpdateTime2 = lastUpdateTime3;
                                                                    resourcePathStr2 = str8;
                                                                    version = str4;
                                                                    idStr = str5;
                                                                    codePathStr = str7;
                                                                    timeStamp2 = timeStamp3;
                                                                    firstInstallTime2 = j;
                                                                    settings = this;
                                                                    i = 5;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("Error in package manager settings: package ");
                                                                    stringBuilder.append(name);
                                                                    stringBuilder.append(" has bad userId ");
                                                                    stringBuilder.append(idStr);
                                                                    stringBuilder.append(" at ");
                                                                    stringBuilder.append(parser.getPositionDescription());
                                                                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                    str = name;
                                                                    systemStr = idStr;
                                                                    str2 = codePathStr;
                                                                    str4 = version;
                                                                    version = updateAvailable;
                                                                    packageSetting = packageSetting2;
                                                                    idStr = installerPackageName;
                                                                    realName = uidError;
                                                                    sharedIdStr = isOrphaned;
                                                                    codePathStr = volumeUuid;
                                                                    resourcePathStr2 = resourcePathStr;
                                                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                    categoryHint2 = categoryHint;
                                                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                                                    if (packageSetting != null) {
                                                                    }
                                                                }
                                                                if (packageSetting != null) {
                                                                }
                                                            } else {
                                                                i6 = pkgFlags2;
                                                                timeStamp3 = j2;
                                                                str2 = str7;
                                                                resourcePathStr3 = str8;
                                                                firstInstallTime3 = j;
                                                                version = this;
                                                                try {
                                                                    stringBuilder4 = new StringBuilder();
                                                                    stringBuilder4.append("Error in package manager settings: package ");
                                                                    stringBuilder4.append(str);
                                                                    stringBuilder4.append(" has bad sharedId ");
                                                                    stringBuilder4.append(name);
                                                                    stringBuilder4.append(" at ");
                                                                    stringBuilder4.append(parser.getPositionDescription());
                                                                    i2 = 5;
                                                                    try {
                                                                        PackageManagerService.reportSettingsProblem(5, stringBuilder4.toString());
                                                                        sharedIdStr = str5;
                                                                        parentPackageName = name;
                                                                        resourcePathStr2 = resourcePathStr;
                                                                        str11 = cpuAbiOverrideString2;
                                                                        lastUpdateTime2 = lastUpdateTime3;
                                                                        str12 = resourcePathStr3;
                                                                        categoryHint2 = categoryHint;
                                                                        packageSetting = packageSetting2;
                                                                        realName = uidError;
                                                                        codePathStr = volumeUuid;
                                                                        timeStamp2 = timeStamp3;
                                                                        firstInstallTime2 = firstInstallTime3;
                                                                        resourcePathStr3 = i2;
                                                                        systemStr = sharedIdStr;
                                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                        settings = version;
                                                                        version = updateAvailable;
                                                                        idStr = installerPackageName;
                                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                                        sharedIdStr = isOrphaned;
                                                                    } catch (NumberFormatException e47) {
                                                                        sharedIdStr = name;
                                                                        name = str;
                                                                        lastUpdateTime2 = lastUpdateTime3;
                                                                        resourcePathStr2 = resourcePathStr3;
                                                                        codePathStr = str2;
                                                                        timeStamp2 = timeStamp3;
                                                                        firstInstallTime2 = firstInstallTime3;
                                                                        i = 5;
                                                                        settings = version;
                                                                        version = str4;
                                                                        idStr = str5;
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("Error in package manager settings: package ");
                                                                        stringBuilder.append(name);
                                                                        stringBuilder.append(" has bad userId ");
                                                                        stringBuilder.append(idStr);
                                                                        stringBuilder.append(" at ");
                                                                        stringBuilder.append(parser.getPositionDescription());
                                                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                        str = name;
                                                                        systemStr = idStr;
                                                                        str2 = codePathStr;
                                                                        str4 = version;
                                                                        version = updateAvailable;
                                                                        packageSetting = packageSetting2;
                                                                        idStr = installerPackageName;
                                                                        realName = uidError;
                                                                        sharedIdStr = isOrphaned;
                                                                        codePathStr = volumeUuid;
                                                                        resourcePathStr2 = resourcePathStr;
                                                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                        categoryHint2 = categoryHint;
                                                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                                                        if (packageSetting != null) {
                                                                        }
                                                                    }
                                                                } catch (NumberFormatException e48) {
                                                                    sharedIdStr = name;
                                                                    name = str;
                                                                    resourcePathStr2 = resourcePathStr3;
                                                                    idStr = str5;
                                                                    codePathStr = str2;
                                                                    i = 5;
                                                                    settings = version;
                                                                    version = str4;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("Error in package manager settings: package ");
                                                                    stringBuilder.append(name);
                                                                    stringBuilder.append(" has bad userId ");
                                                                    stringBuilder.append(idStr);
                                                                    stringBuilder.append(" at ");
                                                                    stringBuilder.append(parser.getPositionDescription());
                                                                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                    str = name;
                                                                    systemStr = idStr;
                                                                    str2 = codePathStr;
                                                                    str4 = version;
                                                                    version = updateAvailable;
                                                                    packageSetting = packageSetting2;
                                                                    idStr = installerPackageName;
                                                                    realName = uidError;
                                                                    sharedIdStr = isOrphaned;
                                                                    codePathStr = volumeUuid;
                                                                    resourcePathStr2 = resourcePathStr;
                                                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                    categoryHint2 = categoryHint;
                                                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                                                    if (packageSetting != null) {
                                                                    }
                                                                }
                                                                if (packageSetting != null) {
                                                                }
                                                            }
                                                        } else {
                                                            i6 = pkgFlags2;
                                                            timeStamp3 = j2;
                                                            str2 = str7;
                                                            resourcePathStr3 = str8;
                                                            sharedUserId3 = sharedUserId2;
                                                            firstInstallTime3 = j;
                                                            i2 = 5;
                                                            version = this;
                                                            try {
                                                                stringBuilder4 = new StringBuilder();
                                                                stringBuilder4.append("Error in package manager settings: package ");
                                                                stringBuilder4.append(str);
                                                                stringBuilder4.append(" has bad userId ");
                                                                sharedIdStr = str5;
                                                                try {
                                                                    stringBuilder4.append(sharedIdStr);
                                                                    stringBuilder4.append(" at ");
                                                                    stringBuilder4.append(parser.getPositionDescription());
                                                                    PackageManagerService.reportSettingsProblem(5, stringBuilder4.toString());
                                                                    parentPackageName = name;
                                                                    resourcePathStr2 = resourcePathStr;
                                                                    str11 = cpuAbiOverrideString2;
                                                                    lastUpdateTime2 = lastUpdateTime3;
                                                                    str12 = resourcePathStr3;
                                                                    categoryHint2 = categoryHint;
                                                                    packageSetting = packageSetting2;
                                                                    realName = uidError;
                                                                    codePathStr = volumeUuid;
                                                                    timeStamp2 = timeStamp3;
                                                                    firstInstallTime2 = firstInstallTime3;
                                                                    resourcePathStr3 = i2;
                                                                    systemStr = sharedIdStr;
                                                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                    settings = version;
                                                                    version = updateAvailable;
                                                                    idStr = installerPackageName;
                                                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                                                    sharedIdStr = isOrphaned;
                                                                } catch (NumberFormatException e49) {
                                                                    lastUpdateTime2 = lastUpdateTime3;
                                                                    resourcePathStr2 = resourcePathStr3;
                                                                    codePathStr = str2;
                                                                    timeStamp2 = timeStamp3;
                                                                    firstInstallTime2 = firstInstallTime3;
                                                                    i = 5;
                                                                    idStr = sharedIdStr;
                                                                    settings = version;
                                                                    version = str4;
                                                                    sharedIdStr = name;
                                                                    name = str;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("Error in package manager settings: package ");
                                                                    stringBuilder.append(name);
                                                                    stringBuilder.append(" has bad userId ");
                                                                    stringBuilder.append(idStr);
                                                                    stringBuilder.append(" at ");
                                                                    stringBuilder.append(parser.getPositionDescription());
                                                                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                    str = name;
                                                                    systemStr = idStr;
                                                                    str2 = codePathStr;
                                                                    str4 = version;
                                                                    version = updateAvailable;
                                                                    packageSetting = packageSetting2;
                                                                    idStr = installerPackageName;
                                                                    realName = uidError;
                                                                    sharedIdStr = isOrphaned;
                                                                    codePathStr = volumeUuid;
                                                                    resourcePathStr2 = resourcePathStr;
                                                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                    categoryHint2 = categoryHint;
                                                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                                                    if (packageSetting != null) {
                                                                    }
                                                                }
                                                            } catch (NumberFormatException e50) {
                                                                lastUpdateTime2 = lastUpdateTime3;
                                                                resourcePathStr2 = resourcePathStr3;
                                                                codePathStr = str2;
                                                                timeStamp2 = timeStamp3;
                                                                firstInstallTime2 = firstInstallTime3;
                                                                i = 5;
                                                                idStr = str5;
                                                                settings = version;
                                                                version = str4;
                                                                sharedIdStr = name;
                                                                name = str;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("Error in package manager settings: package ");
                                                                stringBuilder.append(name);
                                                                stringBuilder.append(" has bad userId ");
                                                                stringBuilder.append(idStr);
                                                                stringBuilder.append(" at ");
                                                                stringBuilder.append(parser.getPositionDescription());
                                                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                                str = name;
                                                                systemStr = idStr;
                                                                str2 = codePathStr;
                                                                str4 = version;
                                                                version = updateAvailable;
                                                                packageSetting = packageSetting2;
                                                                idStr = installerPackageName;
                                                                realName = uidError;
                                                                sharedIdStr = isOrphaned;
                                                                codePathStr = volumeUuid;
                                                                resourcePathStr2 = resourcePathStr;
                                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                                categoryHint2 = categoryHint;
                                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                                if (packageSetting != null) {
                                                                }
                                                            }
                                                            if (packageSetting != null) {
                                                            }
                                                        }
                                                    }
                                                }
                                                resourcePathStr3 = resourcePathStr2;
                                                parentPackageName = name;
                                                resourcePathStr2 = resourcePathStr;
                                                str11 = cpuAbiOverrideString2;
                                                lastUpdateTime2 = lastUpdateTime3;
                                                str12 = resourcePathStr3;
                                                categoryHint2 = categoryHint;
                                                packageSetting = packageSetting2;
                                                realName = uidError;
                                                codePathStr = volumeUuid;
                                                timeStamp2 = timeStamp3;
                                                firstInstallTime2 = firstInstallTime3;
                                                resourcePathStr3 = i2;
                                                systemStr = sharedIdStr;
                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                settings = version;
                                                version = updateAvailable;
                                                idStr = installerPackageName;
                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                sharedIdStr = isOrphaned;
                                            } catch (NumberFormatException e51) {
                                                str2 = codePathStr;
                                                categoryHintString = resourcePathStr2;
                                                str4 = version;
                                                timeStamp3 = timeStamp2;
                                                firstInstallTime3 = firstInstallTime2;
                                                systemStr = lastUpdateTime2;
                                                str10 = realName;
                                                settings = this;
                                                resourcePathStr2 = attributeValue;
                                                i = 5;
                                                idStr = idStr;
                                                sharedIdStr = sharedIdStr;
                                                name = name;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Error in package manager settings: package ");
                                                stringBuilder.append(name);
                                                stringBuilder.append(" has bad userId ");
                                                stringBuilder.append(idStr);
                                                stringBuilder.append(" at ");
                                                stringBuilder.append(parser.getPositionDescription());
                                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                                str = name;
                                                systemStr = idStr;
                                                str2 = codePathStr;
                                                str4 = version;
                                                version = updateAvailable;
                                                packageSetting = packageSetting2;
                                                idStr = installerPackageName;
                                                realName = uidError;
                                                sharedIdStr = isOrphaned;
                                                codePathStr = volumeUuid;
                                                resourcePathStr2 = resourcePathStr;
                                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                                categoryHint2 = categoryHint;
                                                secondaryCpuAbiString = cpuAbiOverrideString;
                                                if (packageSetting != null) {
                                                }
                                            }
                                        } catch (NumberFormatException e52) {
                                            str2 = codePathStr;
                                            categoryHintString = resourcePathStr2;
                                            str4 = version;
                                            timeStamp3 = timeStamp2;
                                            firstInstallTime3 = firstInstallTime2;
                                            str10 = realName;
                                            settings = this;
                                            lastUpdateTime2 = 0;
                                            resourcePathStr2 = attributeValue;
                                            i = 5;
                                            idStr = idStr;
                                            sharedIdStr = sharedIdStr;
                                            name = name;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Error in package manager settings: package ");
                                            stringBuilder.append(name);
                                            stringBuilder.append(" has bad userId ");
                                            stringBuilder.append(idStr);
                                            stringBuilder.append(" at ");
                                            stringBuilder.append(parser.getPositionDescription());
                                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                            str = name;
                                            systemStr = idStr;
                                            str2 = codePathStr;
                                            str4 = version;
                                            version = updateAvailable;
                                            packageSetting = packageSetting2;
                                            idStr = installerPackageName;
                                            realName = uidError;
                                            sharedIdStr = isOrphaned;
                                            codePathStr = volumeUuid;
                                            resourcePathStr2 = resourcePathStr;
                                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                                            categoryHint2 = categoryHint;
                                            secondaryCpuAbiString = cpuAbiOverrideString;
                                            if (packageSetting != null) {
                                            }
                                        }
                                    } catch (NumberFormatException e53) {
                                        str2 = codePathStr;
                                        categoryHintString = resourcePathStr2;
                                        str4 = version;
                                        timeStamp3 = timeStamp2;
                                        settings = this;
                                        firstInstallTime2 = 0;
                                        lastUpdateTime2 = 0;
                                        resourcePathStr2 = attributeValue;
                                        i = 5;
                                        idStr = idStr;
                                        sharedIdStr = sharedIdStr;
                                        name = name;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error in package manager settings: package ");
                                        stringBuilder.append(name);
                                        stringBuilder.append(" has bad userId ");
                                        stringBuilder.append(idStr);
                                        stringBuilder.append(" at ");
                                        stringBuilder.append(parser.getPositionDescription());
                                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                        str = name;
                                        systemStr = idStr;
                                        str2 = codePathStr;
                                        str4 = version;
                                        version = updateAvailable;
                                        packageSetting = packageSetting2;
                                        idStr = installerPackageName;
                                        realName = uidError;
                                        sharedIdStr = isOrphaned;
                                        codePathStr = volumeUuid;
                                        resourcePathStr2 = resourcePathStr;
                                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                                        categoryHint2 = categoryHint;
                                        secondaryCpuAbiString = cpuAbiOverrideString;
                                        if (packageSetting != null) {
                                        }
                                    }
                                    if (packageSetting != null) {
                                    }
                                }
                                versionCode = 0;
                                try {
                                    installerPackageName = xmlPullParser.getAttributeValue(null, "installer");
                                    isOrphaned = xmlPullParser.getAttributeValue(null, "isOrphaned");
                                    volumeUuid = xmlPullParser.getAttributeValue(null, ATTR_VOLUME_UUID);
                                    resourcePathStr2 = xmlPullParser.getAttributeValue(null, "categoryHint");
                                    if (resourcePathStr2 != null) {
                                    }
                                    systemStr = xmlPullParser.getAttributeValue(null, "publicFlags");
                                    if (systemStr == null) {
                                    }
                                } catch (NumberFormatException e54) {
                                    str = name;
                                    name = sharedIdStr;
                                    str2 = codePathStr;
                                    str4 = version;
                                    sharedIdStr = idStr;
                                    settings = this;
                                    installerPackageName = null;
                                    isOrphaned = null;
                                    volumeUuid = null;
                                    categoryHintString = null;
                                    timeStamp2 = 0;
                                    firstInstallTime2 = 0;
                                    lastUpdateTime2 = 0;
                                    resourcePathStr2 = attributeValue;
                                    i = 5;
                                    sharedIdStr = name;
                                    name = str;
                                    categoryHintString2 = null;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error in package manager settings: package ");
                                    stringBuilder.append(name);
                                    stringBuilder.append(" has bad userId ");
                                    stringBuilder.append(idStr);
                                    stringBuilder.append(" at ");
                                    stringBuilder.append(parser.getPositionDescription());
                                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                    str = name;
                                    systemStr = idStr;
                                    str2 = codePathStr;
                                    str4 = version;
                                    version = updateAvailable;
                                    packageSetting = packageSetting2;
                                    idStr = installerPackageName;
                                    realName = uidError;
                                    sharedIdStr = isOrphaned;
                                    codePathStr = volumeUuid;
                                    resourcePathStr2 = resourcePathStr;
                                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                                    categoryHint2 = categoryHint;
                                    secondaryCpuAbiString = cpuAbiOverrideString;
                                    if (packageSetting != null) {
                                    }
                                }
                            } catch (NumberFormatException e55) {
                                str = name;
                                name = sharedIdStr;
                                str2 = codePathStr;
                                sharedIdStr = idStr;
                                settings = this;
                                isOrphaned = null;
                                volumeUuid = null;
                                categoryHintString = null;
                                timeStamp2 = 0;
                                firstInstallTime2 = 0;
                                lastUpdateTime2 = 0;
                                version = null;
                                versionCode = 0;
                                resourcePathStr2 = attributeValue;
                                i = 5;
                                sharedIdStr = name;
                                name = str;
                                categoryHintString2 = null;
                                installerPackageName = null;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Error in package manager settings: package ");
                                stringBuilder.append(name);
                                stringBuilder.append(" has bad userId ");
                                stringBuilder.append(idStr);
                                stringBuilder.append(" at ");
                                stringBuilder.append(parser.getPositionDescription());
                                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                                str = name;
                                systemStr = idStr;
                                str2 = codePathStr;
                                str4 = version;
                                version = updateAvailable;
                                packageSetting = packageSetting2;
                                idStr = installerPackageName;
                                realName = uidError;
                                sharedIdStr = isOrphaned;
                                codePathStr = volumeUuid;
                                resourcePathStr2 = resourcePathStr;
                                cpuAbiOverrideString2 = secondaryCpuAbiString;
                                categoryHint2 = categoryHint;
                                secondaryCpuAbiString = cpuAbiOverrideString;
                                if (packageSetting != null) {
                                }
                            }
                        } catch (NumberFormatException e56) {
                            str = name;
                            name = sharedIdStr;
                            str2 = codePathStr;
                            sharedIdStr = idStr;
                            cpuAbiOverrideString = resourcePathStr2;
                            settings = this;
                            isOrphaned = null;
                            volumeUuid = null;
                            categoryHintString = null;
                            timeStamp2 = 0;
                            firstInstallTime2 = 0;
                            lastUpdateTime2 = 0;
                            version = null;
                            versionCode = 0;
                            resourcePathStr2 = attributeValue;
                            i = 5;
                            sharedIdStr = name;
                            name = str;
                            categoryHintString2 = null;
                            installerPackageName = null;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error in package manager settings: package ");
                            stringBuilder.append(name);
                            stringBuilder.append(" has bad userId ");
                            stringBuilder.append(idStr);
                            stringBuilder.append(" at ");
                            stringBuilder.append(parser.getPositionDescription());
                            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                            str = name;
                            systemStr = idStr;
                            str2 = codePathStr;
                            str4 = version;
                            version = updateAvailable;
                            packageSetting = packageSetting2;
                            idStr = installerPackageName;
                            realName = uidError;
                            sharedIdStr = isOrphaned;
                            codePathStr = volumeUuid;
                            resourcePathStr2 = resourcePathStr;
                            cpuAbiOverrideString2 = secondaryCpuAbiString;
                            categoryHint2 = categoryHint;
                            secondaryCpuAbiString = cpuAbiOverrideString;
                            if (packageSetting != null) {
                            }
                        }
                    } catch (NumberFormatException e57) {
                        str = name;
                        name = sharedIdStr;
                        str2 = codePathStr;
                        sharedIdStr = idStr;
                        settings = this;
                        isOrphaned = null;
                        volumeUuid = null;
                        categoryHintString = null;
                        timeStamp2 = 0;
                        firstInstallTime2 = 0;
                        lastUpdateTime2 = 0;
                        version = null;
                        versionCode = 0;
                        cpuAbiOverrideString2 = cpuAbiOverrideString;
                        i = 5;
                        sharedIdStr = name;
                        cpuAbiOverrideString = resourcePathStr2;
                        name = str;
                        categoryHintString2 = null;
                        installerPackageName = null;
                        resourcePathStr2 = attributeValue;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error in package manager settings: package ");
                        stringBuilder.append(name);
                        stringBuilder.append(" has bad userId ");
                        stringBuilder.append(idStr);
                        stringBuilder.append(" at ");
                        stringBuilder.append(parser.getPositionDescription());
                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                        str = name;
                        systemStr = idStr;
                        str2 = codePathStr;
                        str4 = version;
                        version = updateAvailable;
                        packageSetting = packageSetting2;
                        idStr = installerPackageName;
                        realName = uidError;
                        sharedIdStr = isOrphaned;
                        codePathStr = volumeUuid;
                        resourcePathStr2 = resourcePathStr;
                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                        categoryHint2 = categoryHint;
                        secondaryCpuAbiString = cpuAbiOverrideString;
                        if (packageSetting != null) {
                        }
                    }
                    try {
                        systemStr = xmlPullParser.getAttributeValue(null, "ft");
                        if (systemStr == null) {
                        }
                        timeStamp2 = timeStamp;
                        str = xmlPullParser.getAttributeValue(null, "it");
                        if (str != null) {
                        }
                        firstInstallTime2 = firstInstallTime;
                        str = xmlPullParser.getAttributeValue(null, "ut");
                        if (str != null) {
                        }
                        lastUpdateTime2 = lastUpdateTime;
                        if (PackageManagerService.DEBUG_SETTINGS) {
                        }
                        if (idStr == null) {
                        }
                        if (sharedIdStr == null) {
                        }
                        sharedUserId = sharedIdStr == null ? Integer.parseInt(sharedIdStr) : null;
                        if (attributeValue != null) {
                        }
                        if (realName != null) {
                        }
                        str10 = realName;
                        if (name != null) {
                        }
                        resourcePathStr3 = resourcePathStr2;
                        parentPackageName = name;
                        resourcePathStr2 = resourcePathStr;
                        str11 = cpuAbiOverrideString2;
                        lastUpdateTime2 = lastUpdateTime3;
                        str12 = resourcePathStr3;
                        categoryHint2 = categoryHint;
                        packageSetting = packageSetting2;
                        realName = uidError;
                        codePathStr = volumeUuid;
                        timeStamp2 = timeStamp3;
                        firstInstallTime2 = firstInstallTime3;
                        resourcePathStr3 = i2;
                        systemStr = sharedIdStr;
                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                        settings = version;
                        version = updateAvailable;
                        idStr = installerPackageName;
                        secondaryCpuAbiString = cpuAbiOverrideString;
                        sharedIdStr = isOrphaned;
                    } catch (NumberFormatException e58) {
                        str = name;
                        name = sharedIdStr;
                        str2 = codePathStr;
                        categoryHintString = resourcePathStr2;
                        str4 = version;
                        sharedIdStr = idStr;
                        idStr = 5;
                        settings = this;
                        timeStamp2 = 0;
                        firstInstallTime2 = 0;
                        lastUpdateTime2 = 0;
                        resourcePathStr2 = attributeValue;
                        i = idStr;
                        idStr = sharedIdStr;
                        sharedIdStr = name;
                        name = str;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error in package manager settings: package ");
                        stringBuilder.append(name);
                        stringBuilder.append(" has bad userId ");
                        stringBuilder.append(idStr);
                        stringBuilder.append(" at ");
                        stringBuilder.append(parser.getPositionDescription());
                        PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                        str = name;
                        systemStr = idStr;
                        str2 = codePathStr;
                        str4 = version;
                        version = updateAvailable;
                        packageSetting = packageSetting2;
                        idStr = installerPackageName;
                        realName = uidError;
                        sharedIdStr = isOrphaned;
                        codePathStr = volumeUuid;
                        resourcePathStr2 = resourcePathStr;
                        cpuAbiOverrideString2 = secondaryCpuAbiString;
                        categoryHint2 = categoryHint;
                        secondaryCpuAbiString = cpuAbiOverrideString;
                        if (packageSetting != null) {
                        }
                    }
                } catch (NumberFormatException e59) {
                    str = name;
                    name = sharedIdStr;
                    str2 = codePathStr;
                    settings = this;
                    sharedIdStr = idStr;
                    secondaryCpuAbiString = null;
                    isOrphaned = null;
                    volumeUuid = null;
                    categoryHintString = null;
                    timeStamp2 = 0;
                    firstInstallTime2 = 0;
                    lastUpdateTime2 = 0;
                    version = null;
                    versionCode = 0;
                    cpuAbiOverrideString2 = cpuAbiOverrideString;
                    i = 5;
                    sharedIdStr = name;
                    cpuAbiOverrideString = resourcePathStr2;
                    name = str;
                    categoryHintString2 = null;
                    installerPackageName = null;
                    resourcePathStr2 = attributeValue;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error in package manager settings: package ");
                    stringBuilder.append(name);
                    stringBuilder.append(" has bad userId ");
                    stringBuilder.append(idStr);
                    stringBuilder.append(" at ");
                    stringBuilder.append(parser.getPositionDescription());
                    PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                    str = name;
                    systemStr = idStr;
                    str2 = codePathStr;
                    str4 = version;
                    version = updateAvailable;
                    packageSetting = packageSetting2;
                    idStr = installerPackageName;
                    realName = uidError;
                    sharedIdStr = isOrphaned;
                    codePathStr = volumeUuid;
                    resourcePathStr2 = resourcePathStr;
                    cpuAbiOverrideString2 = secondaryCpuAbiString;
                    categoryHint2 = categoryHint;
                    secondaryCpuAbiString = cpuAbiOverrideString;
                    if (packageSetting != null) {
                    }
                }
            } catch (NumberFormatException e60) {
                str = name;
                name = sharedIdStr;
                str2 = codePathStr;
                settings = this;
                sharedIdStr = idStr;
                secondaryCpuAbiString = null;
                isOrphaned = null;
                volumeUuid = null;
                categoryHintString = null;
                timeStamp2 = 0;
                firstInstallTime2 = 0;
                lastUpdateTime2 = 0;
                version = null;
                version2 = null;
                resourcePathStr2 = attributeValue;
                i = 5;
                sharedIdStr = name;
                timeStamp = realName;
                name = str;
                categoryHintString2 = null;
                installerPackageName = null;
                str3 = cpuAbiOverrideString;
                cpuAbiOverrideString = null;
                cpuAbiOverrideString2 = str3;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error in package manager settings: package ");
                stringBuilder.append(name);
                stringBuilder.append(" has bad userId ");
                stringBuilder.append(idStr);
                stringBuilder.append(" at ");
                stringBuilder.append(parser.getPositionDescription());
                PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
                str = name;
                systemStr = idStr;
                str2 = codePathStr;
                str4 = version;
                version = updateAvailable;
                packageSetting = packageSetting2;
                idStr = installerPackageName;
                realName = uidError;
                sharedIdStr = isOrphaned;
                codePathStr = volumeUuid;
                resourcePathStr2 = resourcePathStr;
                cpuAbiOverrideString2 = secondaryCpuAbiString;
                categoryHint2 = categoryHint;
                secondaryCpuAbiString = cpuAbiOverrideString;
                if (packageSetting != null) {
                }
            }
        } catch (NumberFormatException e61) {
            settings = this;
            i = 5;
            legacyCpuAbiString = null;
            uidError = null;
            isOrphaned = null;
            volumeUuid = null;
            categoryHintString = null;
            timeStamp2 = 0;
            firstInstallTime2 = 0;
            lastUpdateTime2 = 0;
            str10 = null;
            resourcePathStr2 = null;
            resourcePathStr = null;
            secondaryCpuAbiString = null;
            categoryHintString2 = null;
            version = null;
            versionCode = 0;
            installerPackageName = null;
            str3 = cpuAbiOverrideString;
            cpuAbiOverrideString = null;
            cpuAbiOverrideString2 = str3;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error in package manager settings: package ");
            stringBuilder.append(name);
            stringBuilder.append(" has bad userId ");
            stringBuilder.append(idStr);
            stringBuilder.append(" at ");
            stringBuilder.append(parser.getPositionDescription());
            PackageManagerService.reportSettingsProblem(i, stringBuilder.toString());
            str = name;
            systemStr = idStr;
            str2 = codePathStr;
            str4 = version;
            version = updateAvailable;
            packageSetting = packageSetting2;
            idStr = installerPackageName;
            realName = uidError;
            sharedIdStr = isOrphaned;
            codePathStr = volumeUuid;
            resourcePathStr2 = resourcePathStr;
            cpuAbiOverrideString2 = secondaryCpuAbiString;
            categoryHint2 = categoryHint;
            secondaryCpuAbiString = cpuAbiOverrideString;
            if (packageSetting != null) {
            }
        }
        if (packageSetting != null) {
        }
    }

    private void readDisabledComponentsLPw(PackageSettingBase packageSetting, XmlPullParser parser, int userId) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        if (name != null) {
                            packageSetting.addDisabledComponent(name.intern(), userId);
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error in package manager settings: <disabled-components> has no name at ");
                            stringBuilder.append(parser.getPositionDescription());
                            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                        }
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown element under <disabled-components>: ");
                        stringBuilder2.append(parser.getName());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private void readEnabledComponentsLPw(PackageSettingBase packageSetting, XmlPullParser parser, int userId) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        String name = parser.getAttributeValue(null, ATTR_NAME);
                        if (name != null) {
                            packageSetting.addEnabledComponent(name.intern(), userId);
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error in package manager settings: <enabled-components> has no name at ");
                            stringBuilder.append(parser.getPositionDescription());
                            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                        }
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown element under <enabled-components>: ");
                        stringBuilder2.append(parser.getName());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private void readSharedUserLPw(XmlPullParser parser) throws XmlPullParserException, IOException {
        int userId;
        int pkgFlags = 0;
        SharedUserSetting su = null;
        try {
            String name = parser.getAttributeValue(null, ATTR_NAME);
            String idStr = parser.getAttributeValue(null, "userId");
            userId = idStr != null ? Integer.parseInt(idStr) : 0;
            if ("true".equals(parser.getAttributeValue(null, "system"))) {
                pkgFlags = 0 | 1;
            }
            StringBuilder stringBuilder;
            if (name == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error in package manager settings: <shared-user> has no name at ");
                stringBuilder.append(parser.getPositionDescription());
                PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
            } else if (userId == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error in package manager settings: shared-user ");
                stringBuilder.append(name);
                stringBuilder.append(" has bad userId ");
                stringBuilder.append(idStr);
                stringBuilder.append(" at ");
                stringBuilder.append(parser.getPositionDescription());
                PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
            } else {
                SharedUserSetting addSharedUserLPw = addSharedUserLPw(name.intern(), userId, pkgFlags, 0);
                su = addSharedUserLPw;
                if (addSharedUserLPw == null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Occurred while parsing settings at ");
                    stringBuilder2.append(parser.getPositionDescription());
                    PackageManagerService.reportSettingsProblem(6, stringBuilder2.toString());
                }
            }
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Error in package manager settings: package ");
            stringBuilder3.append(null);
            stringBuilder3.append(" has bad userId ");
            stringBuilder3.append(null);
            stringBuilder3.append(" at ");
            stringBuilder3.append(parser.getPositionDescription());
            PackageManagerService.reportSettingsProblem(5, stringBuilder3.toString());
        }
        if (su != null) {
            int outerDepth = parser.getDepth();
            while (true) {
                userId = parser.next();
                int type = userId;
                if (userId == 1) {
                    return;
                }
                if (type == 3 && parser.getDepth() <= outerDepth) {
                    return;
                }
                if (type != 3) {
                    if (type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals("sigs")) {
                            su.signatures.readXml(parser, this.mPastSignatures);
                        } else if (tagName.equals(TAG_PERMISSIONS)) {
                            readInstallPermissionsLPr(parser, su.getPermissionsState());
                        } else {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Unknown element under <shared-user>: ");
                            stringBuilder4.append(parser.getName());
                            PackageManagerService.reportSettingsProblem(5, stringBuilder4.toString());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
            }
        } else {
            XmlUtils.skipCurrentTag(parser);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:0x00c8 A:{Catch:{ all -> 0x006a, all -> 0x0144 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void createNewUserLI(PackageManagerService service, Installer installer, int userHandle, String[] disallowedPackages) {
        int packagesCount;
        String[] volumeUuids;
        String[] names;
        int[] appIds;
        String[] seinfos;
        int[] targetSdkVersions;
        int i;
        Throwable th;
        PackageManagerService packageManagerService;
        int i2;
        int packagesCount2;
        InstallerException e;
        int i3 = userHandle;
        synchronized (this.mPackages) {
            String[] strArr;
            try {
                Collection<PackageSetting> packages = this.mPackages.values();
                packagesCount = packages.size();
                volumeUuids = new String[packagesCount];
                names = new String[packagesCount];
                appIds = new int[packagesCount];
                seinfos = new String[packagesCount];
                targetSdkVersions = new int[packagesCount];
                Iterator<PackageSetting> packagesIterator = packages.iterator();
                i = 0;
                int i4 = 0;
                while (true) {
                    int i5 = i4;
                    if (i5 >= packagesCount) {
                        break;
                    }
                    Collection<PackageSetting> packages2;
                    int i6;
                    Iterator<PackageSetting> packagesIterator2;
                    PackageSetting ps = (PackageSetting) packagesIterator.next();
                    if (ps.pkg == null) {
                        packages2 = packages;
                        i6 = i5;
                        packagesIterator2 = packagesIterator;
                    } else if (ps.pkg.applicationInfo == null) {
                        packages2 = packages;
                        i6 = i5;
                        packagesIterator2 = packagesIterator;
                    } else {
                        boolean shouldInstall;
                        boolean shouldInstall2;
                        boolean isSystemApp = ps.isSystem();
                        if (this.mCustSettings != null && this.mCustSettings.isInNosysAppList(ps.name)) {
                            isSystemApp = true;
                        }
                        if (!isSystemApp) {
                            strArr = disallowedPackages;
                        } else if (!ArrayUtils.contains(disallowedPackages, ps.name)) {
                            isSystemApp = true;
                            shouldInstall = isSystemApp;
                            ps.setInstalled(shouldInstall, i3);
                            if (shouldInstall || !isInDelAppList(ps.name)) {
                                packages2 = packages;
                                shouldInstall2 = shouldInstall;
                                packages = ps;
                                i6 = i5;
                                packagesIterator2 = packagesIterator;
                            } else {
                                String str = TAG;
                                packages2 = packages;
                                packages = new StringBuilder();
                                shouldInstall2 = shouldInstall;
                                packages.append("disable application: ");
                                packages.append(ps.name);
                                packages.append(" for user ");
                                packages.append(i3);
                                Slog.w(str, packages.toString());
                                packages = ps;
                                i6 = i5;
                                packagesIterator2 = packagesIterator;
                                service.setApplicationEnabledSetting(ps.name, 2, 0, i3, null);
                            }
                            if (!shouldInstall2) {
                                writeKernelMappingLPr(packages);
                            }
                            volumeUuids[i6] = packages.volumeUuid;
                            names[i6] = packages.name;
                            appIds[i6] = packages.appId;
                            seinfos[i6] = packages.pkg.applicationInfo.seInfo;
                            targetSdkVersions[i6] = packages.pkg.applicationInfo.targetSdkVersion;
                        }
                        isSystemApp = false;
                        shouldInstall = isSystemApp;
                        ps.setInstalled(shouldInstall, i3);
                        if (shouldInstall) {
                        }
                        packages2 = packages;
                        shouldInstall2 = shouldInstall;
                        packages = ps;
                        i6 = i5;
                        packagesIterator2 = packagesIterator;
                        if (shouldInstall2) {
                        }
                        volumeUuids[i6] = packages.volumeUuid;
                        names[i6] = packages.name;
                        appIds[i6] = packages.appId;
                        seinfos[i6] = packages.pkg.applicationInfo.seInfo;
                        targetSdkVersions[i6] = packages.pkg.applicationInfo.targetSdkVersion;
                    }
                    i4 = i6 + 1;
                    packages = packages2;
                    packagesIterator = packagesIterator2;
                }
            } catch (Throwable th2) {
                th = th2;
                packageManagerService = service;
                while (true) {
                    break;
                }
                throw th;
            }
        }
        while (true) {
            int i7 = i;
            if (i7 < packagesCount) {
                if (names[i7] == null) {
                    i2 = i7;
                    packagesCount2 = packagesCount;
                } else {
                    i = 3;
                    try {
                        i2 = i7;
                        packagesCount2 = packagesCount;
                        try {
                            installer.createAppData(volumeUuids[i7], names[i7], i3, 3, appIds[i7], seinfos[i7], targetSdkVersions[i7]);
                        } catch (InstallerException e2) {
                            e = e2;
                        }
                    } catch (InstallerException e3) {
                        e = e3;
                        i2 = i7;
                        packagesCount2 = packagesCount;
                        Slog.w(TAG, "Failed to prepare app data", e);
                        i = i2 + 1;
                        packagesCount = packagesCount2;
                    }
                }
                i = i2 + 1;
                packagesCount = packagesCount2;
            } else {
                synchronized (this.mPackages) {
                    applyDefaultPreferredAppsLPw(service, i3);
                }
                return;
            }
        }
    }

    void removeUserLPw(int userId) {
        for (Entry<String, PackageSetting> entry : this.mPackages.entrySet()) {
            ((PackageSetting) entry.getValue()).removeUser(userId);
        }
        this.mPreferredActivities.remove(userId);
        getUserPackagesStateFile(userId).delete();
        getUserPackagesStateBackupFile(userId).delete();
        removeCrossProfileIntentFiltersLPw(userId);
        this.mRuntimePermissionsPersistence.onUserRemovedLPw(userId);
        writePackageListLPr();
        writeKernelRemoveUserLPr(userId);
    }

    void removeCrossProfileIntentFiltersLPw(int userId) {
        synchronized (this.mCrossProfileIntentResolvers) {
            if (this.mCrossProfileIntentResolvers.get(userId) != null) {
                this.mCrossProfileIntentResolvers.remove(userId);
                writePackageRestrictionsLPr(userId);
            }
            int count = this.mCrossProfileIntentResolvers.size();
            for (int i = 0; i < count; i++) {
                int sourceUserId = this.mCrossProfileIntentResolvers.keyAt(i);
                CrossProfileIntentResolver cpir = (CrossProfileIntentResolver) this.mCrossProfileIntentResolvers.get(sourceUserId);
                boolean needsWriting = false;
                Iterator it = new ArraySet(cpir.filterSet()).iterator();
                while (it.hasNext()) {
                    CrossProfileIntentFilter cpif = (CrossProfileIntentFilter) it.next();
                    if (cpif.getTargetUserId() == userId) {
                        needsWriting = true;
                        cpir.removeFilter(cpif);
                    }
                }
                if (needsWriting) {
                    writePackageRestrictionsLPr(sourceUserId);
                }
            }
        }
    }

    private void setFirstAvailableUid(int uid) {
        if (uid > mFirstAvailableUid) {
            mFirstAvailableUid = uid;
        }
    }

    private int newUserIdLPw(Object obj) {
        int N = this.mUserIds.size();
        for (int i = mFirstAvailableUid; i < N; i++) {
            if (this.mUserIds.get(i) == null) {
                this.mUserIds.set(i, obj);
                return 10000 + i;
            }
        }
        if (N > 9958) {
            return retryNewUserIdLPw(obj);
        }
        this.mUserIds.add(obj);
        return 10000 + N;
    }

    private int retryNewUserIdLPw(Object obj) {
        int N = this.mUserIds.size();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("retryNewUserIdLPw N:");
        stringBuilder.append(N);
        stringBuilder.append(",first available uid:");
        stringBuilder.append(mFirstAvailableUid);
        Slog.i(str, stringBuilder.toString());
        if (this.isNeedRetryNewUserId) {
            int appId = -1;
            for (int i = 0; i < N; i++) {
                if (this.mUserIds.get(i) == null) {
                    this.mUserIds.set(i, obj);
                    appId = 10000 + i;
                    mFirstAvailableUid = i + 1;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("we find an available UserId ");
                    stringBuilder2.append(appId);
                    stringBuilder2.append(" to assign after retry, change first available uid to ");
                    stringBuilder2.append(mFirstAvailableUid);
                    Slog.i(str2, stringBuilder2.toString());
                    break;
                }
            }
            if (appId < 0) {
                this.isNeedRetryNewUserId = false;
                Slog.e(TAG, "we could not find an available UserId to assign after retry!");
            }
            return appId;
        }
        Slog.i(TAG, "No need to retry to find an available UserId to assign!");
        return -1;
    }

    public VerifierDeviceIdentity getVerifierDeviceIdentityLPw() {
        if (this.mVerifierDeviceIdentity == null) {
            this.mVerifierDeviceIdentity = VerifierDeviceIdentity.generate();
            writeLPr();
        }
        return this.mVerifierDeviceIdentity;
    }

    boolean hasOtherDisabledSystemPkgWithChildLPr(String parentPackageName, String childPackageName) {
        int packageCount = this.mDisabledSysPackages.size();
        for (int i = 0; i < packageCount; i++) {
            PackageSetting disabledPs = (PackageSetting) this.mDisabledSysPackages.valueAt(i);
            if (!(disabledPs.childPackageNames == null || disabledPs.childPackageNames.isEmpty() || disabledPs.name.equals(parentPackageName))) {
                int childCount = disabledPs.childPackageNames.size();
                for (int j = 0; j < childCount; j++) {
                    if (((String) disabledPs.childPackageNames.get(j)).equals(childPackageName)) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    public PackageSetting getDisabledSystemPkgLPr(String name) {
        return (PackageSetting) this.mDisabledSysPackages.get(name);
    }

    boolean isEnabledAndMatchLPr(ComponentInfo componentInfo, int flags, int userId) {
        PackageSetting ps = (PackageSetting) this.mPackages.get(componentInfo.packageName);
        if (ps == null) {
            return false;
        }
        return ps.readUserState(userId).isMatch(componentInfo, flags);
    }

    String getInstallerPackageNameLPr(String packageName) {
        PackageSetting pkg = (PackageSetting) this.mPackages.get(packageName);
        if (pkg != null) {
            return pkg.installerPackageName;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown package: ");
        stringBuilder.append(packageName);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    boolean isOrphaned(String packageName) {
        PackageSetting pkg = (PackageSetting) this.mPackages.get(packageName);
        if (pkg != null) {
            return pkg.isOrphaned;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown package: ");
        stringBuilder.append(packageName);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    int getApplicationEnabledSettingLPr(String packageName, int userId) {
        PackageSetting pkg = (PackageSetting) this.mPackages.get(packageName);
        if (pkg != null) {
            return pkg.getEnabled(userId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown package: ");
        stringBuilder.append(packageName);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    int getComponentEnabledSettingLPr(ComponentName componentName, int userId) {
        PackageSetting pkg = (PackageSetting) this.mPackages.get(componentName.getPackageName());
        if (pkg != null) {
            return pkg.getCurrentEnabledStateLPr(componentName.getClassName(), userId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown component: ");
        stringBuilder.append(componentName);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    boolean wasPackageEverLaunchedLPr(String packageName, int userId) {
        PackageSetting pkgSetting = (PackageSetting) this.mPackages.get(packageName);
        if (pkgSetting != null) {
            return pkgSetting.getNotLaunched(userId) ^ 1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown package: ");
        stringBuilder.append(packageName);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    boolean setPackageStoppedStateLPw(PackageManagerService pm, String packageName, boolean stopped, boolean allowedByPermission, int uid, int userId) {
        int appId = UserHandle.getAppId(uid);
        PackageSetting pkgSetting = (PackageSetting) this.mPackages.get(packageName);
        StringBuilder stringBuilder;
        if (pkgSetting == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown package: ");
            stringBuilder.append(packageName);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!allowedByPermission && appId != pkgSetting.appId) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Permission Denial: attempt to change stopped state from pid=");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", package uid=");
            stringBuilder.append(pkgSetting.appId);
            throw new SecurityException(stringBuilder.toString());
        } else if (pkgSetting.getStopped(userId) == stopped) {
            return false;
        } else {
            pkgSetting.setStopped(stopped, userId);
            if (pkgSetting.getNotLaunched(userId)) {
                if (pkgSetting.installerPackageName != null) {
                    pm.notifyFirstLaunch(pkgSetting.name, pkgSetting.installerPackageName, userId);
                }
                pkgSetting.setNotLaunched(false, userId);
            }
            return true;
        }
    }

    void setHarmfulAppWarningLPw(String packageName, CharSequence warning, int userId) {
        PackageSetting pkgSetting = (PackageSetting) this.mPackages.get(packageName);
        if (pkgSetting != null) {
            pkgSetting.setHarmfulAppWarning(userId, warning == null ? null : warning.toString());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown package: ");
        stringBuilder.append(packageName);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    String getHarmfulAppWarningLPr(String packageName, int userId) {
        PackageSetting pkgSetting = (PackageSetting) this.mPackages.get(packageName);
        if (pkgSetting != null) {
            return pkgSetting.getHarmfulAppWarning(userId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown package: ");
        stringBuilder.append(packageName);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static List<UserInfo> getAllUsers(UserManagerService userManager) {
        long id = Binder.clearCallingIdentity();
        List<UserInfo> e;
        try {
            e = userManager.getUsers(false);
            return e;
        } catch (NullPointerException e2) {
            e = e2;
            return null;
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    List<PackageSetting> getVolumePackagesLPr(String volumeUuid) {
        ArrayList<PackageSetting> res = new ArrayList();
        for (int i = 0; i < this.mPackages.size(); i++) {
            PackageSetting setting = (PackageSetting) this.mPackages.valueAt(i);
            if (Objects.equals(volumeUuid, setting.volumeUuid)) {
                res.add(setting);
            }
        }
        return res;
    }

    static void printFlags(PrintWriter pw, int val, Object[] spec) {
        pw.print("[ ");
        for (int i = 0; i < spec.length; i += 2) {
            if ((val & ((Integer) spec[i]).intValue()) != 0) {
                pw.print(spec[i + 1]);
                pw.print(" ");
            }
        }
        pw.print("]");
    }

    void dumpVersionLPr(IndentingPrintWriter pw) {
        pw.increaseIndent();
        for (int i = 0; i < this.mVersion.size(); i++) {
            String volumeUuid = (String) this.mVersion.keyAt(i);
            VersionInfo ver = (VersionInfo) this.mVersion.valueAt(i);
            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
                pw.println("Internal:");
            } else if (Objects.equals("primary_physical", volumeUuid)) {
                pw.println("External:");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UUID ");
                stringBuilder.append(volumeUuid);
                stringBuilder.append(":");
                pw.println(stringBuilder.toString());
            }
            pw.increaseIndent();
            pw.printPair(ATTR_SDK_VERSION, Integer.valueOf(ver.sdkVersion));
            pw.printPair(ATTR_DATABASE_VERSION, Integer.valueOf(ver.databaseVersion));
            pw.println();
            pw.printPair(ATTR_FINGERPRINT, ver.fingerprint);
            pw.println();
            pw.printPair(ATTR_HWFINGERPRINT, ver.hwFingerprint);
            pw.println();
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    void dumpPackageLPr(PrintWriter pw, String prefix, String checkinTag, ArraySet<String> permissionNames, PackageSetting ps, SimpleDateFormat sdf, Date date, List<UserInfo> users, boolean dumpAll) {
        PrintWriter printWriter = pw;
        String str = prefix;
        String str2 = checkinTag;
        ArraySet<String> arraySet = permissionNames;
        PackageSetting packageSetting = ps;
        Date date2 = date;
        int i;
        String lastDisabledAppCaller;
        if (str2 != null) {
            printWriter.print(str2);
            printWriter.print(",");
            printWriter.print(packageSetting.realName != null ? packageSetting.realName : packageSetting.name);
            printWriter.print(",");
            printWriter.print(packageSetting.appId);
            printWriter.print(",");
            printWriter.print(packageSetting.versionCode);
            printWriter.print(",");
            printWriter.print(packageSetting.firstInstallTime);
            printWriter.print(",");
            printWriter.print(packageSetting.lastUpdateTime);
            printWriter.print(",");
            printWriter.print(packageSetting.installerPackageName != null ? packageSetting.installerPackageName : "?");
            pw.println();
            if (packageSetting.pkg != null) {
                printWriter.print(str2);
                printWriter.print("-");
                printWriter.print("splt,");
                printWriter.print("base,");
                printWriter.println(packageSetting.pkg.baseRevisionCode);
                if (packageSetting.pkg.splitNames != null) {
                    int i2 = 0;
                    while (true) {
                        i = i2;
                        if (i >= packageSetting.pkg.splitNames.length) {
                            break;
                        }
                        printWriter.print(str2);
                        printWriter.print("-");
                        printWriter.print("splt,");
                        printWriter.print(packageSetting.pkg.splitNames[i]);
                        printWriter.print(",");
                        printWriter.println(packageSetting.pkg.splitRevisionCodes[i]);
                        i2 = i + 1;
                    }
                }
            }
            for (UserInfo user : users) {
                printWriter.print(str2);
                printWriter.print("-");
                printWriter.print("usr");
                printWriter.print(",");
                printWriter.print(user.id);
                printWriter.print(",");
                printWriter.print(packageSetting.getInstalled(user.id) ? "I" : "i");
                printWriter.print(packageSetting.getHidden(user.id) ? "B" : "b");
                printWriter.print(packageSetting.getSuspended(user.id) ? "SU" : "su");
                printWriter.print(packageSetting.getStopped(user.id) ? "S" : "s");
                printWriter.print(packageSetting.getNotLaunched(user.id) ? "l" : "L");
                printWriter.print(packageSetting.getInstantApp(user.id) ? "IA" : "ia");
                printWriter.print(packageSetting.getVirtulalPreload(user.id) ? "VPI" : "vpi");
                printWriter.print(packageSetting.getHarmfulAppWarning(user.id) != null ? "HA" : "ha");
                printWriter.print(",");
                printWriter.print(packageSetting.getEnabled(user.id));
                lastDisabledAppCaller = packageSetting.getLastDisabledAppCaller(user.id);
                printWriter.print(",");
                printWriter.print(lastDisabledAppCaller != null ? lastDisabledAppCaller : "?");
                printWriter.print(",");
                pw.println();
            }
            return;
        }
        int i3;
        String perm;
        pw.print(prefix);
        printWriter.print("Package [");
        printWriter.print(packageSetting.realName != null ? packageSetting.realName : packageSetting.name);
        printWriter.print("] (");
        printWriter.print(Integer.toHexString(System.identityHashCode(ps)));
        printWriter.println("):");
        if (packageSetting.realName != null) {
            pw.print(prefix);
            printWriter.print("  compat name=");
            printWriter.println(packageSetting.name);
        }
        pw.print(prefix);
        printWriter.print("  userId=");
        printWriter.println(packageSetting.appId);
        if (packageSetting.sharedUser != null) {
            pw.print(prefix);
            printWriter.print("  sharedUser=");
            printWriter.println(packageSetting.sharedUser);
        }
        pw.print(prefix);
        printWriter.print("  pkg=");
        printWriter.println(packageSetting.pkg);
        pw.print(prefix);
        printWriter.print("  codePath=");
        printWriter.println(packageSetting.codePathString);
        if (arraySet == null) {
            pw.print(prefix);
            printWriter.print("  resourcePath=");
            printWriter.println(packageSetting.resourcePathString);
            pw.print(prefix);
            printWriter.print("  legacyNativeLibraryDir=");
            printWriter.println(packageSetting.legacyNativeLibraryPathString);
            pw.print(prefix);
            printWriter.print("  primaryCpuAbi=");
            printWriter.println(packageSetting.primaryCpuAbiString);
            pw.print(prefix);
            printWriter.print("  secondaryCpuAbi=");
            printWriter.println(packageSetting.secondaryCpuAbiString);
        }
        pw.print(prefix);
        printWriter.print("  versionCode=");
        printWriter.print(packageSetting.versionCode);
        if (packageSetting.pkg != null) {
            printWriter.print(" minSdk=");
            printWriter.print(packageSetting.pkg.applicationInfo.minSdkVersion);
            printWriter.print(" targetSdk=");
            printWriter.print(packageSetting.pkg.applicationInfo.targetSdkVersion);
        }
        pw.println();
        if (packageSetting.pkg != null) {
            int i4;
            if (packageSetting.pkg.parentPackage != null) {
                Package parentPkg = packageSetting.pkg.parentPackage;
                PackageSetting pps = (PackageSetting) this.mPackages.get(parentPkg.packageName);
                if (pps == null || !pps.codePathString.equals(parentPkg.codePath)) {
                    pps = (PackageSetting) this.mDisabledSysPackages.get(parentPkg.packageName);
                }
                if (pps != null) {
                    pw.print(prefix);
                    printWriter.print("  parentPackage=");
                    printWriter.println(pps.realName != null ? pps.realName : pps.name);
                }
            } else if (packageSetting.pkg.childPackages != null) {
                pw.print(prefix);
                printWriter.print("  childPackages=[");
                i = packageSetting.pkg.childPackages.size();
                for (i3 = 0; i3 < i; i3++) {
                    Package childPkg = (Package) packageSetting.pkg.childPackages.get(i3);
                    PackageSetting cps = (PackageSetting) this.mPackages.get(childPkg.packageName);
                    if (cps == null || !cps.codePathString.equals(childPkg.codePath)) {
                        cps = (PackageSetting) this.mDisabledSysPackages.get(childPkg.packageName);
                    }
                    if (cps != null) {
                        if (i3 > 0) {
                            printWriter.print(", ");
                        }
                        printWriter.print(cps.realName != null ? cps.realName : cps.name);
                    }
                }
                printWriter.println("]");
            }
            pw.print(prefix);
            printWriter.print("  versionName=");
            printWriter.println(packageSetting.pkg.mVersionName);
            pw.print(prefix);
            printWriter.print("  splits=");
            dumpSplitNames(printWriter, packageSetting.pkg);
            pw.println();
            i = packageSetting.pkg.mSigningDetails.signatureSchemeVersion;
            pw.print(prefix);
            printWriter.print("  apkSigningVersion=");
            printWriter.println(i);
            pw.print(prefix);
            printWriter.print("  applicationInfo=");
            printWriter.println(packageSetting.pkg.applicationInfo.toString());
            pw.print(prefix);
            printWriter.print("  flags=");
            printFlags(printWriter, packageSetting.pkg.applicationInfo.flags, FLAG_DUMP_SPEC);
            pw.println();
            pw.print(prefix);
            printWriter.print("  hwflags=");
            printFlags(printWriter, packageSetting.pkg.applicationInfo.hwFlags, HW_FLAG_DUMP_SPEC);
            pw.println();
            if (packageSetting.pkg.applicationInfo.privateFlags != 0) {
                pw.print(prefix);
                printWriter.print("  privateFlags=");
                printFlags(printWriter, packageSetting.pkg.applicationInfo.privateFlags, PRIVATE_FLAG_DUMP_SPEC);
                pw.println();
            }
            pw.print(prefix);
            printWriter.print("  dataDir=");
            printWriter.println(packageSetting.pkg.applicationInfo.dataDir);
            pw.print(prefix);
            printWriter.print("  supportsScreens=[");
            boolean first = true;
            if ((packageSetting.pkg.applicationInfo.flags & 512) != 0) {
                if (1 == null) {
                    printWriter.print(", ");
                }
                first = false;
                printWriter.print("small");
            }
            if ((packageSetting.pkg.applicationInfo.flags & 1024) != 0) {
                if (!first) {
                    printWriter.print(", ");
                }
                first = false;
                printWriter.print("medium");
            }
            if ((packageSetting.pkg.applicationInfo.flags & 2048) != 0) {
                if (!first) {
                    printWriter.print(", ");
                }
                first = false;
                printWriter.print("large");
            }
            if ((packageSetting.pkg.applicationInfo.flags & DumpState.DUMP_FROZEN) != 0) {
                if (!first) {
                    printWriter.print(", ");
                }
                first = false;
                printWriter.print("xlarge");
            }
            if ((packageSetting.pkg.applicationInfo.flags & 4096) != 0) {
                if (!first) {
                    printWriter.print(", ");
                }
                first = false;
                printWriter.print("resizeable");
            }
            if ((packageSetting.pkg.applicationInfo.flags & 8192) != 0) {
                if (!first) {
                    printWriter.print(", ");
                }
                printWriter.print("anyDensity");
            }
            printWriter.println("]");
            if (packageSetting.pkg.libraryNames != null && packageSetting.pkg.libraryNames.size() > 0) {
                pw.print(prefix);
                printWriter.println("  dynamic libraries:");
                for (i4 = 0; i4 < packageSetting.pkg.libraryNames.size(); i4++) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.println((String) packageSetting.pkg.libraryNames.get(i4));
                }
            }
            if (packageSetting.pkg.staticSharedLibName != null) {
                pw.print(prefix);
                printWriter.println("  static library:");
                pw.print(prefix);
                printWriter.print("    ");
                printWriter.print("name:");
                printWriter.print(packageSetting.pkg.staticSharedLibName);
                printWriter.print(" version:");
                printWriter.println(packageSetting.pkg.staticSharedLibVersion);
            }
            if (packageSetting.pkg.usesLibraries != null && packageSetting.pkg.usesLibraries.size() > 0) {
                pw.print(prefix);
                printWriter.println("  usesLibraries:");
                for (i4 = 0; i4 < packageSetting.pkg.usesLibraries.size(); i4++) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.println((String) packageSetting.pkg.usesLibraries.get(i4));
                }
            }
            if (packageSetting.pkg.usesStaticLibraries != null && packageSetting.pkg.usesStaticLibraries.size() > 0) {
                pw.print(prefix);
                printWriter.println("  usesStaticLibraries:");
                for (i4 = 0; i4 < packageSetting.pkg.usesStaticLibraries.size(); i4++) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.print((String) packageSetting.pkg.usesStaticLibraries.get(i4));
                    printWriter.print(" version:");
                    printWriter.println(packageSetting.pkg.usesStaticLibrariesVersions[i4]);
                }
            }
            if (packageSetting.pkg.usesOptionalLibraries != null && packageSetting.pkg.usesOptionalLibraries.size() > 0) {
                pw.print(prefix);
                printWriter.println("  usesOptionalLibraries:");
                for (i4 = 0; i4 < packageSetting.pkg.usesOptionalLibraries.size(); i4++) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.println((String) packageSetting.pkg.usesOptionalLibraries.get(i4));
                }
            }
            if (packageSetting.pkg.usesLibraryFiles != null && packageSetting.pkg.usesLibraryFiles.length > 0) {
                pw.print(prefix);
                printWriter.println("  usesLibraryFiles:");
                for (String lastDisabledAppCaller2 : packageSetting.pkg.usesLibraryFiles) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.println(lastDisabledAppCaller2);
                }
            }
        }
        pw.print(prefix);
        printWriter.print("  timeStamp=");
        date2.setTime(packageSetting.timeStamp);
        printWriter.println(sdf.format(date));
        pw.print(prefix);
        printWriter.print("  firstInstallTime=");
        date2.setTime(packageSetting.firstInstallTime);
        printWriter.println(sdf.format(date));
        pw.print(prefix);
        printWriter.print("  lastUpdateTime=");
        date2.setTime(packageSetting.lastUpdateTime);
        printWriter.println(sdf.format(date));
        if (packageSetting.installerPackageName != null) {
            pw.print(prefix);
            printWriter.print("  installerPackageName=");
            printWriter.println(packageSetting.installerPackageName);
        }
        if (packageSetting.volumeUuid != null) {
            pw.print(prefix);
            printWriter.print("  volumeUuid=");
            printWriter.println(packageSetting.volumeUuid);
        }
        pw.print(prefix);
        printWriter.print("  signatures=");
        printWriter.println(packageSetting.signatures);
        pw.print(prefix);
        printWriter.print("  installPermissionsFixed=");
        printWriter.print(packageSetting.installPermissionsFixed);
        pw.println();
        pw.print(prefix);
        printWriter.print("  pkgFlags=");
        printFlags(printWriter, packageSetting.pkgFlags, FLAG_DUMP_SPEC);
        pw.println();
        if (!(packageSetting.pkg == null || packageSetting.pkg.mOverlayTarget == null)) {
            pw.print(prefix);
            printWriter.print("  overlayTarget=");
            printWriter.println(packageSetting.pkg.mOverlayTarget);
            pw.print(prefix);
            printWriter.print("  overlayCategory=");
            printWriter.println(packageSetting.pkg.mOverlayCategory);
        }
        if (!(packageSetting.pkg == null || packageSetting.pkg.permissions == null || packageSetting.pkg.permissions.size() <= 0)) {
            ArrayList<Permission> perms = packageSetting.pkg.permissions;
            pw.print(prefix);
            printWriter.println("  declared permissions:");
            for (i3 = 0; i3 < perms.size(); i3++) {
                Permission perm2 = (Permission) perms.get(i3);
                if (arraySet == null || arraySet.contains(perm2.info.name)) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.print(perm2.info.name);
                    printWriter.print(": prot=");
                    printWriter.print(PermissionInfo.protectionToString(perm2.info.protectionLevel));
                    if ((perm2.info.flags & 1) != 0) {
                        printWriter.print(", COSTS_MONEY");
                    }
                    if ((perm2.info.flags & 2) != 0) {
                        printWriter.print(", HIDDEN");
                    }
                    if ((perm2.info.flags & 1073741824) != 0) {
                        printWriter.print(", INSTALLED");
                    }
                    pw.println();
                }
            }
        }
        if ((arraySet != null || dumpAll) && packageSetting.pkg != null && packageSetting.pkg.requestedPermissions != null && packageSetting.pkg.requestedPermissions.size() > 0) {
            ArrayList<String> perms2 = packageSetting.pkg.requestedPermissions;
            pw.print(prefix);
            printWriter.println("  requested permissions:");
            for (i3 = 0; i3 < perms2.size(); i3++) {
                perm = (String) perms2.get(i3);
                if (arraySet == null || arraySet.contains(perm)) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.println(perm);
                }
            }
        }
        if (packageSetting.sharedUser == null || arraySet != null || dumpAll) {
            PermissionsState permissionsState = ps.getPermissionsState();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append("  ");
            dumpInstallPermissionsLPr(printWriter, stringBuilder.toString(), arraySet, permissionsState);
        }
        for (UserInfo user2 : users) {
            UserInfo user3;
            pw.print(prefix);
            printWriter.print("  User ");
            printWriter.print(user2.id);
            printWriter.print(": ");
            printWriter.print("ceDataInode=");
            printWriter.print(packageSetting.getCeDataInode(user2.id));
            printWriter.print(" installed=");
            printWriter.print(packageSetting.getInstalled(user2.id));
            printWriter.print(" hidden=");
            printWriter.print(packageSetting.getHidden(user2.id));
            printWriter.print(" suspended=");
            printWriter.print(packageSetting.getSuspended(user2.id));
            if (packageSetting.getSuspended(user2.id)) {
                PackageUserState pus = packageSetting.readUserState(user2.id);
                printWriter.print(" suspendingPackage=");
                printWriter.print(pus.suspendingPackage);
                printWriter.print(" dialogMessage=");
                printWriter.print(pus.dialogMessage);
            }
            printWriter.print(" stopped=");
            printWriter.print(packageSetting.getStopped(user2.id));
            printWriter.print(" notLaunched=");
            printWriter.print(packageSetting.getNotLaunched(user2.id));
            printWriter.print(" enabled=");
            printWriter.print(packageSetting.getEnabled(user2.id));
            printWriter.print(" instant=");
            printWriter.print(packageSetting.getInstantApp(user2.id));
            printWriter.print(" virtual=");
            printWriter.println(packageSetting.getVirtulalPreload(user2.id));
            String[] overlayPaths = packageSetting.getOverlayPaths(user2.id);
            if (overlayPaths != null && overlayPaths.length > 0) {
                pw.print(prefix);
                printWriter.println("  overlay paths:");
                for (String perm3 : overlayPaths) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.println(perm3);
                }
            }
            lastDisabledAppCaller2 = packageSetting.getLastDisabledAppCaller(user2.id);
            if (lastDisabledAppCaller2 != null) {
                pw.print(prefix);
                printWriter.print("    lastDisabledCaller: ");
                printWriter.println(lastDisabledAppCaller2);
            }
            if (packageSetting.sharedUser == null) {
                PermissionsState permissionsState2 = ps.getPermissionsState();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(str);
                stringBuilder2.append("    ");
                dumpGidsLPr(printWriter, stringBuilder2.toString(), permissionsState2.computeGids(user2.id));
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(str);
                stringBuilder2.append("    ");
                user3 = user2;
                dumpRuntimePermissionsLPr(printWriter, stringBuilder2.toString(), arraySet, permissionsState2.getRuntimePermissionStates(user2.id), dumpAll);
            } else {
                String[] strArr = overlayPaths;
                user3 = user2;
            }
            String harmfulAppWarning = packageSetting.getHarmfulAppWarning(user3.id);
            if (harmfulAppWarning != null) {
                pw.print(prefix);
                printWriter.print("      harmfulAppWarning: ");
                printWriter.println(harmfulAppWarning);
            }
            if (arraySet == null) {
                Iterator it;
                ArraySet<String> cmp = packageSetting.getDisabledComponents(user3.id);
                if (cmp != null && cmp.size() > 0) {
                    pw.print(prefix);
                    printWriter.println("    disabledComponents:");
                    it = cmp.iterator();
                    while (it.hasNext()) {
                        lastDisabledAppCaller2 = (String) it.next();
                        pw.print(prefix);
                        printWriter.print("      ");
                        printWriter.println(lastDisabledAppCaller2);
                    }
                }
                cmp = packageSetting.getEnabledComponents(user3.id);
                if (cmp != null && cmp.size() > 0) {
                    pw.print(prefix);
                    printWriter.println("    enabledComponents:");
                    it = cmp.iterator();
                    while (it.hasNext()) {
                        lastDisabledAppCaller2 = (String) it.next();
                        pw.print(prefix);
                        printWriter.print("      ");
                        printWriter.println(lastDisabledAppCaller2);
                    }
                }
            }
        }
    }

    void dumpPackagesLPr(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState, boolean checkin) {
        Settings settings = this;
        PrintWriter printWriter = pw;
        String str = packageName;
        ArraySet<String> arraySet = permissionNames;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        boolean printedSomething = false;
        List<UserInfo> users = getAllUsers(UserManagerService.getInstance());
        Iterator it = settings.mPackages.values().iterator();
        while (true) {
            String str2 = null;
            if (!it.hasNext()) {
                break;
            }
            PackageSetting ps = (PackageSetting) it.next();
            if (str == null || str.equals(ps.realName) || str.equals(ps.name)) {
                if (arraySet == null || ps.getPermissionsState().hasRequestedPermission(arraySet)) {
                    if (checkin || str == null) {
                        DumpState dumpState2 = dumpState;
                    } else {
                        dumpState.setSharedUser(ps.sharedUser);
                    }
                    if (!(checkin || printedSomething)) {
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        printWriter.println("Packages:");
                        printedSomething = true;
                    }
                    boolean printedSomething2 = printedSomething;
                    String str3 = "  ";
                    if (checkin) {
                        str2 = "pkg";
                    }
                    settings.dumpPackageLPr(printWriter, str3, str2, arraySet, ps, sdf, date, users, str != null);
                    printedSomething = printedSomething2;
                }
            }
        }
        printedSomething = false;
        if (settings.mRenamedPackages.size() > 0 && arraySet == null) {
            for (Entry<String, String> e : settings.mRenamedPackages.entrySet()) {
                if (str == null || str.equals(e.getKey()) || str.equals(e.getValue())) {
                    if (checkin) {
                        printWriter.print("ren,");
                    } else {
                        if (!printedSomething) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            printWriter.println("Renamed packages:");
                            printedSomething = true;
                        }
                        printWriter.print("  ");
                    }
                    printWriter.print((String) e.getKey());
                    printWriter.print(checkin ? " -> " : ",");
                    printWriter.println((String) e.getValue());
                }
            }
        }
        printedSomething = false;
        if (settings.mDisabledSysPackages.size() > 0 && arraySet == null) {
            for (PackageSetting ps2 : settings.mDisabledSysPackages.values()) {
                if (str == null || str.equals(ps2.realName) || str.equals(ps2.name)) {
                    if (!(checkin || printedSomething)) {
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        printWriter.println("Hidden system packages:");
                        printedSomething = true;
                    }
                    settings.dumpPackageLPr(printWriter, "  ", checkin ? "dis" : null, permissionNames, ps2, sdf, date, users, str != null);
                    settings = this;
                    printWriter = pw;
                    str = packageName;
                }
            }
        }
    }

    void dumpPackagesProto(ProtoOutputStream proto) {
        List<UserInfo> users = getAllUsers(UserManagerService.getInstance());
        int count = this.mPackages.size();
        for (int i = 0; i < count; i++) {
            ((PackageSetting) this.mPackages.valueAt(i)).writeToProto(proto, 2246267895813L, users);
        }
    }

    void dumpPermissionsLPr(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState) {
        this.mPermissions.dumpPermissions(pw, packageName, permissionNames, this.mReadExternalStorageEnforced == Boolean.TRUE, dumpState);
    }

    void dumpSharedUsersLPr(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState, boolean checkin) {
        PrintWriter printWriter = pw;
        ArraySet<String> arraySet = permissionNames;
        boolean printedSomething = false;
        for (SharedUserSetting su : this.mSharedUsers.values()) {
            if (packageName == null || su == dumpState.getSharedUser()) {
                if (arraySet == null || su.getPermissionsState().hasRequestedPermission(arraySet)) {
                    if (checkin) {
                        printWriter.print("suid,");
                        printWriter.print(su.userId);
                        printWriter.print(",");
                        printWriter.println(su.name);
                    } else {
                        if (!printedSomething) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            printWriter.println("Shared users:");
                            printedSomething = true;
                        }
                        boolean printedSomething2 = printedSomething;
                        printWriter.print("  SharedUser [");
                        printWriter.print(su.name);
                        printWriter.print("] (");
                        printWriter.print(Integer.toHexString(System.identityHashCode(su)));
                        printWriter.println("):");
                        String prefix = "    ";
                        printWriter.print(prefix);
                        printWriter.print("userId=");
                        printWriter.println(su.userId);
                        PermissionsState permissionsState = su.getPermissionsState();
                        dumpInstallPermissionsLPr(printWriter, prefix, arraySet, permissionsState);
                        int[] userIds = UserManagerService.getInstance().getUserIds();
                        int length = userIds.length;
                        int i = 0;
                        while (i < length) {
                            int i2;
                            int i3;
                            int[] iArr;
                            int userId = userIds[i];
                            int[] gids = permissionsState.computeGids(userId);
                            List<PermissionState> permissions = permissionsState.getRuntimePermissionStates(userId);
                            if (ArrayUtils.isEmpty(gids) && permissions.isEmpty()) {
                                i2 = i;
                                i3 = length;
                                iArr = userIds;
                            } else {
                                printWriter.print(prefix);
                                List<PermissionState> permissions2 = permissions;
                                printWriter.print("User ");
                                printWriter.print(userId);
                                printWriter.println(": ");
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(prefix);
                                stringBuilder.append("  ");
                                dumpGidsLPr(printWriter, stringBuilder.toString(), gids);
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(prefix);
                                stringBuilder.append("  ");
                                i2 = i;
                                i3 = length;
                                iArr = userIds;
                                dumpRuntimePermissionsLPr(printWriter, stringBuilder.toString(), arraySet, permissions2, packageName != null);
                            }
                            i = i2 + 1;
                            length = i3;
                            userIds = iArr;
                        }
                        printedSomething = printedSomething2;
                    }
                }
            }
        }
    }

    void dumpSharedUsersProto(ProtoOutputStream proto) {
        int count = this.mSharedUsers.size();
        for (int i = 0; i < count; i++) {
            ((SharedUserSetting) this.mSharedUsers.valueAt(i)).writeToProto(proto, 2246267895814L);
        }
    }

    void dumpReadMessagesLPr(PrintWriter pw, DumpState dumpState) {
        pw.println("Settings parse messages:");
        pw.print(this.mReadMessages.toString());
    }

    void dumpRestoredPermissionGrantsLPr(PrintWriter pw, DumpState dumpState) {
        if (this.mRestoredUserGrants.size() > 0) {
            pw.println();
            pw.println("Restored (pending) permission grants:");
            for (int userIndex = 0; userIndex < this.mRestoredUserGrants.size(); userIndex++) {
                ArrayMap<String, ArraySet<RestoredPermissionGrant>> grantsByPackage = (ArrayMap) this.mRestoredUserGrants.valueAt(userIndex);
                if (grantsByPackage != null && grantsByPackage.size() > 0) {
                    int userId = this.mRestoredUserGrants.keyAt(userIndex);
                    pw.print("  User ");
                    pw.println(userId);
                    for (int pkgIndex = 0; pkgIndex < grantsByPackage.size(); pkgIndex++) {
                        ArraySet<RestoredPermissionGrant> grants = (ArraySet) grantsByPackage.valueAt(pkgIndex);
                        if (grants != null && grants.size() > 0) {
                            String pkgName = (String) grantsByPackage.keyAt(pkgIndex);
                            pw.print("    ");
                            pw.print(pkgName);
                            pw.println(" :");
                            Iterator it = grants.iterator();
                            while (it.hasNext()) {
                                RestoredPermissionGrant g = (RestoredPermissionGrant) it.next();
                                pw.print("      ");
                                pw.print(g.permissionName);
                                if (g.granted) {
                                    pw.print(" GRANTED");
                                }
                                if ((g.grantBits & 1) != 0) {
                                    pw.print(" user_set");
                                }
                                if ((g.grantBits & 2) != 0) {
                                    pw.print(" user_fixed");
                                }
                                if ((g.grantBits & 8) != 0) {
                                    pw.print(" revoke_on_upgrade");
                                }
                                pw.println();
                            }
                        }
                    }
                }
            }
            pw.println();
        }
    }

    private static void dumpSplitNames(PrintWriter pw, Package pkg) {
        if (pkg == null) {
            pw.print(Shell.NIGHT_MODE_STR_UNKNOWN);
            return;
        }
        pw.print("[");
        pw.print("base");
        if (pkg.baseRevisionCode != 0) {
            pw.print(":");
            pw.print(pkg.baseRevisionCode);
        }
        if (pkg.splitNames != null) {
            for (int i = 0; i < pkg.splitNames.length; i++) {
                pw.print(", ");
                pw.print(pkg.splitNames[i]);
                if (pkg.splitRevisionCodes[i] != 0) {
                    pw.print(":");
                    pw.print(pkg.splitRevisionCodes[i]);
                }
            }
        }
        pw.print("]");
    }

    void dumpGidsLPr(PrintWriter pw, String prefix, int[] gids) {
        if (!ArrayUtils.isEmpty(gids)) {
            pw.print(prefix);
            pw.print("gids=");
            pw.println(PackageManagerService.arrayToString(gids));
        }
    }

    void dumpRuntimePermissionsLPr(PrintWriter pw, String prefix, ArraySet<String> permissionNames, List<PermissionState> permissionStates, boolean dumpAll) {
        if (!permissionStates.isEmpty() || dumpAll) {
            pw.print(prefix);
            pw.println("runtime permissions:");
            for (PermissionState permissionState : permissionStates) {
                if (permissionNames == null || permissionNames.contains(permissionState.getName())) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print(permissionState.getName());
                    pw.print(": granted=");
                    pw.print(permissionState.isGranted());
                    pw.println(permissionFlagsToString(", flags=", permissionState.getFlags()));
                }
            }
        }
    }

    private static String permissionFlagsToString(String prefix, int flags) {
        StringBuilder flagsString = null;
        while (flags != 0) {
            if (flagsString == null) {
                flagsString = new StringBuilder();
                flagsString.append(prefix);
                flagsString.append("[ ");
            }
            int flag = 1 << Integer.numberOfTrailingZeros(flags);
            flags &= ~flag;
            flagsString.append(PackageManager.permissionFlagToString(flag));
            flagsString.append(' ');
        }
        if (flagsString == null) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        flagsString.append(']');
        return flagsString.toString();
    }

    void dumpInstallPermissionsLPr(PrintWriter pw, String prefix, ArraySet<String> permissionNames, PermissionsState permissionsState) {
        List<PermissionState> permissionStates = permissionsState.getInstallPermissionStates();
        if (!permissionStates.isEmpty()) {
            pw.print(prefix);
            pw.println("install permissions:");
            for (PermissionState permissionState : permissionStates) {
                if (permissionNames == null || permissionNames.contains(permissionState.getName())) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print(permissionState.getName());
                    pw.print(": granted=");
                    pw.print(permissionState.isGranted());
                    pw.println(permissionFlagsToString(", flags=", permissionState.getFlags()));
                }
            }
        }
    }

    public void writeRuntimePermissionsForUserLPr(int userId, boolean sync) {
        if (sync) {
            this.mRuntimePermissionsPersistence.writePermissionsForUserSyncLPr(userId);
        } else {
            this.mRuntimePermissionsPersistence.writePermissionsForUserAsyncLPr(userId);
        }
    }

    private boolean isInDelAppList(String packageName) {
        if (mIsCheckDelAppsFinished.compareAndSet(false, true)) {
            readDelAppsFiles();
        }
        return this.mDelAppLists.contains(packageName);
    }

    private void readDelAppsFiles() {
        Object file;
        ArrayList<File> delAppsFileList = new ArrayList();
        try {
            delAppsFileList = HwCfgFilePolicy.getCfgFileList("xml/hw_subuser_delapps_config.xml", 0);
            file = new File(getCustomizedFileName(FILE_SUB_USER_DELAPPS_LIST));
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            file = new File(getCustomizedFileName(FILE_SUB_USER_DELAPPS_LIST));
        } catch (Throwable th) {
            delAppsFileList.add(new File(getCustomizedFileName(FILE_SUB_USER_DELAPPS_LIST)));
        }
        delAppsFileList.add(file);
        Iterator it = delAppsFileList.iterator();
        while (it.hasNext()) {
            loadDelAppsFromXml((File) it.next());
        }
    }

    /* JADX WARNING: Missing block: B:20:?, code skipped:
            r1.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadDelAppsFromXml(File configFile) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        if (configFile.exists()) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(configFile);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, null);
                int depth = parser.getDepth();
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if ((next == 3 && parser.getDepth() <= depth) || type == 1) {
                        try {
                            break;
                        } catch (IOException e2) {
                            e = e2;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    } else if (type == 2) {
                        if (parser.getName().equals("del_app")) {
                            this.mDelAppLists.add(parser.getAttributeValue(0));
                        }
                    }
                }
            } catch (FileNotFoundException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("file is not exist ");
                stringBuilder.append(e3);
                Slog.e(str, stringBuilder.toString());
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                        e = e4;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (XmlPullParserException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed parsing ");
                stringBuilder.append(configFile);
                stringBuilder.append(" ");
                stringBuilder.append(e5);
                Slog.e(str, stringBuilder.toString());
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e6) {
                        e = e6;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (IOException e7) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed parsing ");
                stringBuilder.append(configFile);
                stringBuilder.append(" ");
                stringBuilder.append(e7);
                Slog.e(str, stringBuilder.toString());
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e8) {
                        e7 = e8;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Throwable th) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e9) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("failed close stream ");
                        stringBuilder.append(e9);
                        Slog.e(TAG, stringBuilder.toString());
                    }
                }
            }
        }
        return;
        stringBuilder.append("failed close stream ");
        stringBuilder.append(e7);
        Slog.e(str, stringBuilder.toString());
    }

    private String getCustomizedFileName(String xmlName) {
        String path = new StringBuilder();
        path.append("/data/cust/xml/");
        path.append(xmlName);
        path = path.toString();
        if (new File(path).exists()) {
            return path;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DIR_ETC_XML);
        stringBuilder.append(xmlName);
        return stringBuilder.toString();
    }
}
