package com.android.server.devicepolicy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminInfo.PolicyInfo;
import android.app.admin.DevicePolicyCache;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.admin.DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener;
import android.app.admin.NetworkEvent;
import android.app.admin.PasswordMetrics;
import android.app.admin.SecurityLog;
import android.app.admin.SecurityLog.SecurityEvent;
import android.app.admin.SystemUpdateInfo;
import android.app.admin.SystemUpdatePolicy;
import android.app.backup.IBackupManager;
import android.app.backup.ISelectBackupTransportCallback;
import android.app.trust.TrustManager;
import android.app.usage.UsageStatsManagerInternal;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.StringParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.IAudioService;
import android.net.ConnectivityManager;
import android.net.IIpConnectivityMetrics;
import android.net.IIpConnectivityMetrics.Stub;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RecoverySystem;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.UserManagerInternal.UserRestrictionsListener;
import android.os.storage.StorageManager;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsInternal;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.provider.Telephony.Carriers;
import android.security.IKeyChainAliasCallback;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyGenParameterSpec.Builder;
import android.security.keystore.ParcelableKeyGenParameterSpec;
import android.service.persistentdata.PersistentDataBlockManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Flog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.IAccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.devicepolicy.HwDevicePolicyFactory.IHwDevicePolicyManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserRestrictionsUtils;
import com.android.server.power.IHwShutdownThread;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.google.android.collect.Sets;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DevicePolicyManagerService extends AbsDevicePolicyManagerService {
    private static final String ACTION_EXPIRED_PASSWORD_NOTIFICATION = "com.android.server.ACTION_EXPIRED_PASSWORD_NOTIFICATION";
    private static final String ATTR_ALIAS = "alias";
    private static final String ATTR_APPLICATION_RESTRICTIONS_MANAGER = "application-restrictions-manager";
    private static final String ATTR_DELEGATED_CERT_INSTALLER = "delegated-cert-installer";
    private static final String ATTR_DEVICE_PAIRED = "device-paired";
    private static final String ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED = "device-provisioning-config-applied";
    private static final String ATTR_DISABLED = "disabled";
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PERMISSION_POLICY = "permission-policy";
    private static final String ATTR_PERMISSION_PROVIDER = "permission-provider";
    private static final String ATTR_PROVISIONING_STATE = "provisioning-state";
    private static final String ATTR_SETUP_COMPLETE = "setup-complete";
    private static final String ATTR_VALUE = "value";
    private static final String[] DELEGATIONS = new String[]{"delegation-cert-install", "delegation-app-restrictions", "delegation-block-uninstall", "delegation-enable-system-app", "delegation-keep-uninstalled-packages", "delegation-package-access", "delegation-permission-grant", "delegation-install-existing-package", "delegation-keep-uninstalled-packages"};
    private static final int DEVICE_ADMIN_DEACTIVATE_TIMEOUT = 10000;
    private static final String DEVICE_POLICIES_XML = "device_policies.xml";
    private static final String DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML = "do-not-ask-credentials-on-boot";
    private static boolean ENABLE_LOCK_GUARD = false;
    private static final long EXPIRATION_GRACE_PERIOD_MS = (5 * MS_PER_DAY);
    private static final Set<String> GLOBAL_SETTINGS_DEPRECATED = new ArraySet();
    private static final Set<String> GLOBAL_SETTINGS_WHITELIST = new ArraySet();
    protected static final String LOG_TAG = "DevicePolicyManager";
    private static final String LOG_TAG_DEVICE_OWNER = "device-owner";
    private static final String LOG_TAG_PROFILE_OWNER = "profile-owner";
    private static final long MINIMUM_STRONG_AUTH_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);
    private static final long MS_PER_DAY = TimeUnit.DAYS.toMillis(1);
    private static final int PROFILE_KEYGUARD_FEATURES = 440;
    private static final int PROFILE_KEYGUARD_FEATURES_PROFILE_ONLY = 8;
    private static final String PROPERTY_DEVICE_OWNER_PRESENT = "ro.device_owner";
    private static final int REQUEST_EXPIRE_PASSWORD = 5571;
    private static final Set<String> SECURE_SETTINGS_DEVICEOWNER_WHITELIST = new ArraySet();
    private static final Set<String> SECURE_SETTINGS_WHITELIST = new ArraySet();
    private static final int STATUS_BAR_DISABLE2_MASK = 1;
    private static final int STATUS_BAR_DISABLE_MASK = 34013184;
    private static final Set<String> SYSTEM_SETTINGS_WHITELIST = new ArraySet();
    private static final String TAG_ACCEPTED_CA_CERTIFICATES = "accepted-ca-certificate";
    private static final String TAG_ADMIN_BROADCAST_PENDING = "admin-broadcast-pending";
    private static final String TAG_AFFILIATION_ID = "affiliation-id";
    private static final String TAG_CURRENT_INPUT_METHOD_SET = "current-ime-set";
    private static final String TAG_INITIALIZATION_BUNDLE = "initialization-bundle";
    private static final String TAG_LAST_BUG_REPORT_REQUEST = "last-bug-report-request";
    private static final String TAG_LAST_NETWORK_LOG_RETRIEVAL = "last-network-log-retrieval";
    private static final String TAG_LAST_SECURITY_LOG_RETRIEVAL = "last-security-log-retrieval";
    private static final String TAG_LOCK_TASK_COMPONENTS = "lock-task-component";
    private static final String TAG_LOCK_TASK_FEATURES = "lock-task-features";
    private static final String TAG_OWNER_INSTALLED_CA_CERT = "owner-installed-ca-cert";
    private static final String TAG_PASSWORD_TOKEN_HANDLE = "password-token";
    private static final String TAG_PASSWORD_VALIDITY = "password-validity";
    private static final String TAG_STATUS_BAR = "statusbar";
    private static final String TAG_TRANSFER_OWNERSHIP_BUNDLE = "transfer-ownership-bundle";
    private static final String TRANSFER_OWNERSHIP_PARAMETERS_XML = "transfer-ownership-parameters.xml";
    private static final boolean VERBOSE_LOG = false;
    private static HwCustDevicePolicyManagerService mHwCustDevicePolicyManagerService = ((HwCustDevicePolicyManagerService) HwCustUtils.createObj(HwCustDevicePolicyManagerService.class, new Object[0]));
    final Handler mBackgroundHandler;
    private final CertificateMonitor mCertificateMonitor;
    private final DevicePolicyConstants mConstants;
    final Context mContext;
    private final DeviceAdminServiceController mDeviceAdminServiceController;
    final Handler mHandler;
    final boolean mHasFeature;
    final IPackageManager mIPackageManager;
    final Injector mInjector;
    boolean mIsMDMDeviceOwnerAPI;
    final boolean mIsWatch;
    final LocalService mLocalService;
    private final Object mLockDoNoUseDirectly;
    private final LockPatternUtils mLockPatternUtils;
    @GuardedBy("getLockObject()")
    private NetworkLogger mNetworkLogger;
    private final OverlayPackagesProvider mOverlayPackagesProvider;
    @VisibleForTesting
    final Owners mOwners;
    private final Set<Pair<String, Integer>> mPackagesToRemove;
    private final DevicePolicyCacheImpl mPolicyCache;
    final BroadcastReceiver mReceiver;
    private final BroadcastReceiver mRemoteBugreportConsentReceiver;
    private final BroadcastReceiver mRemoteBugreportFinishedReceiver;
    private final AtomicBoolean mRemoteBugreportServiceIsActive;
    private final AtomicBoolean mRemoteBugreportSharingAccepted;
    private final Runnable mRemoteBugreportTimeoutRunnable;
    private final SecurityLogMonitor mSecurityLogMonitor;
    private final SetupContentObserver mSetupContentObserver;
    private final StatLogger mStatLogger;
    final TelephonyManager mTelephonyManager;
    private final Binder mToken;
    @VisibleForTesting
    final TransferOwnershipMetadataManager mTransferOwnershipMetadataManager;
    final UsageStatsManagerInternal mUsageStatsManagerInternal;
    @GuardedBy("getLockObject()")
    final SparseArray<DevicePolicyData> mUserData;
    final UserManager mUserManager;
    final UserManagerInternal mUserManagerInternal;
    @GuardedBy("getLockObject()")
    final SparseArray<PasswordMetrics> mUserPasswordMetrics;

    static class ActiveAdmin {
        private static final String ATTR_LAST_NETWORK_LOGGING_NOTIFICATION = "last-notification";
        private static final String ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS = "num-notifications";
        private static final String ATTR_VALUE = "value";
        static final int DEF_KEYGUARD_FEATURES_DISABLED = 0;
        static final int DEF_MAXIMUM_FAILED_PASSWORDS_FOR_WIPE = 0;
        static final int DEF_MAXIMUM_NETWORK_LOGGING_NOTIFICATIONS_SHOWN = 2;
        static final long DEF_MAXIMUM_TIME_TO_UNLOCK = 0;
        static final int DEF_MINIMUM_PASSWORD_LENGTH = 0;
        static final int DEF_MINIMUM_PASSWORD_LETTERS = 1;
        static final int DEF_MINIMUM_PASSWORD_LOWER_CASE = 0;
        static final int DEF_MINIMUM_PASSWORD_NON_LETTER = 0;
        static final int DEF_MINIMUM_PASSWORD_NUMERIC = 1;
        static final int DEF_MINIMUM_PASSWORD_SYMBOLS = 1;
        static final int DEF_MINIMUM_PASSWORD_UPPER_CASE = 0;
        static final int DEF_ORGANIZATION_COLOR = Color.parseColor("#00796B");
        static final long DEF_PASSWORD_EXPIRATION_DATE = 0;
        static final long DEF_PASSWORD_EXPIRATION_TIMEOUT = 0;
        static final int DEF_PASSWORD_HISTORY_LENGTH = 0;
        private static final String TAG_ACCOUNT_TYPE = "account-type";
        private static final String TAG_CROSS_PROFILE_WIDGET_PROVIDERS = "cross-profile-widget-providers";
        private static final String TAG_DEFAULT_ENABLED_USER_RESTRICTIONS = "default-enabled-user-restrictions";
        private static final String TAG_DISABLE_ACCOUNT_MANAGEMENT = "disable-account-management";
        private static final String TAG_DISABLE_BLUETOOTH_CONTACT_SHARING = "disable-bt-contacts-sharing";
        private static final String TAG_DISABLE_CALLER_ID = "disable-caller-id";
        private static final String TAG_DISABLE_CAMERA = "disable-camera";
        private static final String TAG_DISABLE_CONTACTS_SEARCH = "disable-contacts-search";
        private static final String TAG_DISABLE_KEYGUARD_FEATURES = "disable-keyguard-features";
        private static final String TAG_DISABLE_SCREEN_CAPTURE = "disable-screen-capture";
        private static final String TAG_ENCRYPTION_REQUESTED = "encryption-requested";
        private static final String TAG_END_USER_SESSION_MESSAGE = "end_user_session_message";
        private static final String TAG_FORCE_EPHEMERAL_USERS = "force_ephemeral_users";
        private static final String TAG_GLOBAL_PROXY_EXCLUSION_LIST = "global-proxy-exclusion-list";
        private static final String TAG_GLOBAL_PROXY_SPEC = "global-proxy-spec";
        private static final String TAG_IS_LOGOUT_ENABLED = "is_logout_enabled";
        private static final String TAG_IS_NETWORK_LOGGING_ENABLED = "is_network_logging_enabled";
        private static final String TAG_KEEP_UNINSTALLED_PACKAGES = "keep-uninstalled-packages";
        private static final String TAG_LONG_SUPPORT_MESSAGE = "long-support-message";
        private static final String TAG_MANAGE_TRUST_AGENT_FEATURES = "manage-trust-agent-features";
        private static final String TAG_MANDATORY_BACKUP_TRANSPORT = "mandatory_backup_transport";
        private static final String TAG_MAX_FAILED_PASSWORD_WIPE = "max-failed-password-wipe";
        private static final String TAG_MAX_TIME_TO_UNLOCK = "max-time-to-unlock";
        private static final String TAG_METERED_DATA_DISABLED_PACKAGES = "metered_data_disabled_packages";
        private static final String TAG_MIN_PASSWORD_LENGTH = "min-password-length";
        private static final String TAG_MIN_PASSWORD_LETTERS = "min-password-letters";
        private static final String TAG_MIN_PASSWORD_LOWERCASE = "min-password-lowercase";
        private static final String TAG_MIN_PASSWORD_NONLETTER = "min-password-nonletter";
        private static final String TAG_MIN_PASSWORD_NUMERIC = "min-password-numeric";
        private static final String TAG_MIN_PASSWORD_SYMBOLS = "min-password-symbols";
        private static final String TAG_MIN_PASSWORD_UPPERCASE = "min-password-uppercase";
        private static final String TAG_ORGANIZATION_COLOR = "organization-color";
        private static final String TAG_ORGANIZATION_NAME = "organization-name";
        private static final String TAG_PACKAGE_LIST_ITEM = "item";
        private static final String TAG_PARENT_ADMIN = "parent-admin";
        private static final String TAG_PASSWORD_EXPIRATION_DATE = "password-expiration-date";
        private static final String TAG_PASSWORD_EXPIRATION_TIMEOUT = "password-expiration-timeout";
        private static final String TAG_PASSWORD_HISTORY_LENGTH = "password-history-length";
        private static final String TAG_PASSWORD_QUALITY = "password-quality";
        private static final String TAG_PERMITTED_ACCESSIBILITY_SERVICES = "permitted-accessiblity-services";
        private static final String TAG_PERMITTED_IMES = "permitted-imes";
        private static final String TAG_PERMITTED_NOTIFICATION_LISTENERS = "permitted-notification-listeners";
        private static final String TAG_POLICIES = "policies";
        private static final String TAG_PROVIDER = "provider";
        private static final String TAG_REQUIRE_AUTO_TIME = "require_auto_time";
        private static final String TAG_RESTRICTION = "restriction";
        private static final String TAG_SHORT_SUPPORT_MESSAGE = "short-support-message";
        private static final String TAG_SPECIFIES_GLOBAL_PROXY = "specifies-global-proxy";
        private static final String TAG_START_USER_SESSION_MESSAGE = "start_user_session_message";
        private static final String TAG_STRONG_AUTH_UNLOCK_TIMEOUT = "strong-auth-unlock-timeout";
        private static final String TAG_TEST_ONLY_ADMIN = "test-only-admin";
        private static final String TAG_TRUST_AGENT_COMPONENT = "component";
        private static final String TAG_TRUST_AGENT_COMPONENT_OPTIONS = "trust-agent-component-options";
        private static final String TAG_USER_RESTRICTIONS = "user-restrictions";
        final Set<String> accountTypesWithManagementDisabled = new ArraySet();
        boolean allowSimplePassword = true;
        List<String> crossProfileWidgetProviders;
        final Set<String> defaultEnabledRestrictionsAlreadySet = new ArraySet();
        boolean disableBluetoothContactSharing = true;
        boolean disableCallerId = false;
        boolean disableCamera = false;
        boolean disableContactsSearch = false;
        boolean disableScreenCapture = false;
        int disabledKeyguardFeatures = 0;
        boolean encryptionRequested = false;
        String endUserSessionMessage = null;
        boolean forceEphemeralUsers = false;
        String globalProxyExclusionList = null;
        String globalProxySpec = null;
        DeviceAdminInfo info;
        boolean isLogoutEnabled = false;
        boolean isNetworkLoggingEnabled = false;
        final boolean isParent;
        List<String> keepUninstalledPackages;
        long lastNetworkLoggingNotificationTimeMs = 0;
        CharSequence longSupportMessage = null;
        public HwActiveAdmin mHwActiveAdmin;
        ComponentName mandatoryBackupTransport = null;
        int maximumFailedPasswordsForWipe = 0;
        long maximumTimeToUnlock = 0;
        List<String> meteredDisabledPackages;
        PasswordMetrics minimumPasswordMetrics = new PasswordMetrics(0, 0, 1, 0, 0, 1, 1, 0);
        int numNetworkLoggingNotifications = 0;
        int organizationColor = DEF_ORGANIZATION_COLOR;
        String organizationName = null;
        ActiveAdmin parentAdmin;
        long passwordExpirationDate = 0;
        long passwordExpirationTimeout = 0;
        int passwordHistoryLength = 0;
        List<String> permittedAccessiblityServices;
        List<String> permittedInputMethods;
        List<String> permittedNotificationListeners;
        boolean requireAutoTime = false;
        CharSequence shortSupportMessage = null;
        boolean specifiesGlobalProxy = false;
        String startUserSessionMessage = null;
        long strongAuthUnlockTimeout = 0;
        boolean testOnlyAdmin = false;
        ArrayMap<String, TrustAgentInfo> trustAgentInfos = new ArrayMap();
        Bundle userRestrictions;

        static class TrustAgentInfo {
            public PersistableBundle options;

            TrustAgentInfo(PersistableBundle bundle) {
                this.options = bundle;
            }
        }

        ActiveAdmin(DeviceAdminInfo _info, boolean parent) {
            this.info = _info;
            this.isParent = parent;
        }

        ActiveAdmin getParentActiveAdmin() {
            Preconditions.checkState(this.isParent ^ 1);
            if (this.parentAdmin == null) {
                this.parentAdmin = new ActiveAdmin(this.info, true);
            }
            return this.parentAdmin;
        }

        boolean hasParentActiveAdmin() {
            return this.parentAdmin != null;
        }

        int getUid() {
            return this.info.getActivityInfo().applicationInfo.uid;
        }

        public UserHandle getUserHandle() {
            return UserHandle.of(UserHandle.getUserId(this.info.getActivityInfo().applicationInfo.uid));
        }

        void writeToXml(XmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
            out.startTag(null, TAG_POLICIES);
            this.info.writePoliciesToXml(out);
            out.endTag(null, TAG_POLICIES);
            if (this.mHwActiveAdmin != null) {
                this.mHwActiveAdmin.writePoliciesToXml(out);
            }
            if (this.minimumPasswordMetrics.quality != 0) {
                out.startTag(null, TAG_PASSWORD_QUALITY);
                out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.quality));
                out.endTag(null, TAG_PASSWORD_QUALITY);
                if (this.minimumPasswordMetrics.length != 0) {
                    out.startTag(null, TAG_MIN_PASSWORD_LENGTH);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.length));
                    out.endTag(null, TAG_MIN_PASSWORD_LENGTH);
                }
                if (this.passwordHistoryLength != 0) {
                    out.startTag(null, TAG_PASSWORD_HISTORY_LENGTH);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.passwordHistoryLength));
                    out.endTag(null, TAG_PASSWORD_HISTORY_LENGTH);
                }
                if (this.minimumPasswordMetrics.upperCase != 0) {
                    out.startTag(null, TAG_MIN_PASSWORD_UPPERCASE);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.upperCase));
                    out.endTag(null, TAG_MIN_PASSWORD_UPPERCASE);
                }
                if (this.minimumPasswordMetrics.lowerCase != 0) {
                    out.startTag(null, TAG_MIN_PASSWORD_LOWERCASE);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.lowerCase));
                    out.endTag(null, TAG_MIN_PASSWORD_LOWERCASE);
                }
                if (this.minimumPasswordMetrics.letters != 1) {
                    out.startTag(null, TAG_MIN_PASSWORD_LETTERS);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.letters));
                    out.endTag(null, TAG_MIN_PASSWORD_LETTERS);
                }
                if (this.minimumPasswordMetrics.numeric != 1) {
                    out.startTag(null, TAG_MIN_PASSWORD_NUMERIC);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.numeric));
                    out.endTag(null, TAG_MIN_PASSWORD_NUMERIC);
                }
                if (this.minimumPasswordMetrics.symbols != 1) {
                    out.startTag(null, TAG_MIN_PASSWORD_SYMBOLS);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.symbols));
                    out.endTag(null, TAG_MIN_PASSWORD_SYMBOLS);
                }
                if (this.minimumPasswordMetrics.nonLetter > 0) {
                    out.startTag(null, TAG_MIN_PASSWORD_NONLETTER);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.nonLetter));
                    out.endTag(null, TAG_MIN_PASSWORD_NONLETTER);
                }
                if (DevicePolicyManagerService.mHwCustDevicePolicyManagerService != null && DevicePolicyManagerService.mHwCustDevicePolicyManagerService.isForbiddenSimplePwdFeatureEnable()) {
                    out.startTag(null, "allow-simple-password");
                    out.attribute(null, ATTR_VALUE, Boolean.toString(this.allowSimplePassword));
                    out.endTag(null, "allow-simple-password");
                }
            }
            if (this.maximumTimeToUnlock != 0) {
                out.startTag(null, TAG_MAX_TIME_TO_UNLOCK);
                out.attribute(null, ATTR_VALUE, Long.toString(this.maximumTimeToUnlock));
                out.endTag(null, TAG_MAX_TIME_TO_UNLOCK);
            }
            if (this.strongAuthUnlockTimeout != 259200000) {
                out.startTag(null, TAG_STRONG_AUTH_UNLOCK_TIMEOUT);
                out.attribute(null, ATTR_VALUE, Long.toString(this.strongAuthUnlockTimeout));
                out.endTag(null, TAG_STRONG_AUTH_UNLOCK_TIMEOUT);
            }
            if (this.maximumFailedPasswordsForWipe != 0) {
                out.startTag(null, TAG_MAX_FAILED_PASSWORD_WIPE);
                out.attribute(null, ATTR_VALUE, Integer.toString(this.maximumFailedPasswordsForWipe));
                out.endTag(null, TAG_MAX_FAILED_PASSWORD_WIPE);
            }
            if (this.specifiesGlobalProxy) {
                out.startTag(null, TAG_SPECIFIES_GLOBAL_PROXY);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.specifiesGlobalProxy));
                out.endTag(null, TAG_SPECIFIES_GLOBAL_PROXY);
                if (this.globalProxySpec != null) {
                    out.startTag(null, TAG_GLOBAL_PROXY_SPEC);
                    out.attribute(null, ATTR_VALUE, this.globalProxySpec);
                    out.endTag(null, TAG_GLOBAL_PROXY_SPEC);
                }
                if (this.globalProxyExclusionList != null) {
                    out.startTag(null, TAG_GLOBAL_PROXY_EXCLUSION_LIST);
                    out.attribute(null, ATTR_VALUE, this.globalProxyExclusionList);
                    out.endTag(null, TAG_GLOBAL_PROXY_EXCLUSION_LIST);
                }
            }
            if (this.passwordExpirationTimeout != 0) {
                out.startTag(null, TAG_PASSWORD_EXPIRATION_TIMEOUT);
                out.attribute(null, ATTR_VALUE, Long.toString(this.passwordExpirationTimeout));
                out.endTag(null, TAG_PASSWORD_EXPIRATION_TIMEOUT);
            }
            if (this.passwordExpirationDate != 0) {
                out.startTag(null, TAG_PASSWORD_EXPIRATION_DATE);
                out.attribute(null, ATTR_VALUE, Long.toString(this.passwordExpirationDate));
                out.endTag(null, TAG_PASSWORD_EXPIRATION_DATE);
            }
            if (this.encryptionRequested) {
                out.startTag(null, TAG_ENCRYPTION_REQUESTED);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.encryptionRequested));
                out.endTag(null, TAG_ENCRYPTION_REQUESTED);
            }
            if (this.testOnlyAdmin) {
                out.startTag(null, TAG_TEST_ONLY_ADMIN);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.testOnlyAdmin));
                out.endTag(null, TAG_TEST_ONLY_ADMIN);
            }
            if (this.disableCamera) {
                out.startTag(null, TAG_DISABLE_CAMERA);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableCamera));
                out.endTag(null, TAG_DISABLE_CAMERA);
            }
            if (this.disableCallerId) {
                out.startTag(null, TAG_DISABLE_CALLER_ID);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableCallerId));
                out.endTag(null, TAG_DISABLE_CALLER_ID);
            }
            if (this.disableContactsSearch) {
                out.startTag(null, TAG_DISABLE_CONTACTS_SEARCH);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableContactsSearch));
                out.endTag(null, TAG_DISABLE_CONTACTS_SEARCH);
            }
            if (!this.disableBluetoothContactSharing) {
                out.startTag(null, TAG_DISABLE_BLUETOOTH_CONTACT_SHARING);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableBluetoothContactSharing));
                out.endTag(null, TAG_DISABLE_BLUETOOTH_CONTACT_SHARING);
            }
            if (this.disableScreenCapture) {
                out.startTag(null, TAG_DISABLE_SCREEN_CAPTURE);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableScreenCapture));
                out.endTag(null, TAG_DISABLE_SCREEN_CAPTURE);
            }
            if (this.requireAutoTime) {
                out.startTag(null, TAG_REQUIRE_AUTO_TIME);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.requireAutoTime));
                out.endTag(null, TAG_REQUIRE_AUTO_TIME);
            }
            if (this.forceEphemeralUsers) {
                out.startTag(null, TAG_FORCE_EPHEMERAL_USERS);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.forceEphemeralUsers));
                out.endTag(null, TAG_FORCE_EPHEMERAL_USERS);
            }
            if (this.isNetworkLoggingEnabled) {
                out.startTag(null, TAG_IS_NETWORK_LOGGING_ENABLED);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.isNetworkLoggingEnabled));
                out.attribute(null, ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS, Integer.toString(this.numNetworkLoggingNotifications));
                out.attribute(null, ATTR_LAST_NETWORK_LOGGING_NOTIFICATION, Long.toString(this.lastNetworkLoggingNotificationTimeMs));
                out.endTag(null, TAG_IS_NETWORK_LOGGING_ENABLED);
            }
            if (this.disabledKeyguardFeatures != 0) {
                out.startTag(null, TAG_DISABLE_KEYGUARD_FEATURES);
                out.attribute(null, ATTR_VALUE, Integer.toString(this.disabledKeyguardFeatures));
                out.endTag(null, TAG_DISABLE_KEYGUARD_FEATURES);
            }
            if (!this.accountTypesWithManagementDisabled.isEmpty()) {
                out.startTag(null, TAG_DISABLE_ACCOUNT_MANAGEMENT);
                writeAttributeValuesToXml(out, TAG_ACCOUNT_TYPE, this.accountTypesWithManagementDisabled);
                out.endTag(null, TAG_DISABLE_ACCOUNT_MANAGEMENT);
            }
            if (!this.trustAgentInfos.isEmpty()) {
                Set<Entry<String, TrustAgentInfo>> set = this.trustAgentInfos.entrySet();
                out.startTag(null, TAG_MANAGE_TRUST_AGENT_FEATURES);
                for (Entry<String, TrustAgentInfo> entry : set) {
                    TrustAgentInfo trustAgentInfo = (TrustAgentInfo) entry.getValue();
                    out.startTag(null, TAG_TRUST_AGENT_COMPONENT);
                    out.attribute(null, ATTR_VALUE, (String) entry.getKey());
                    if (trustAgentInfo.options != null) {
                        out.startTag(null, TAG_TRUST_AGENT_COMPONENT_OPTIONS);
                        try {
                            trustAgentInfo.options.saveToXml(out);
                        } catch (XmlPullParserException e) {
                            Log.e(DevicePolicyManagerService.LOG_TAG, "Failed to save TrustAgent options", e);
                        }
                        out.endTag(null, TAG_TRUST_AGENT_COMPONENT_OPTIONS);
                    }
                    out.endTag(null, TAG_TRUST_AGENT_COMPONENT);
                }
                out.endTag(null, TAG_MANAGE_TRUST_AGENT_FEATURES);
            }
            if (!(this.crossProfileWidgetProviders == null || this.crossProfileWidgetProviders.isEmpty())) {
                out.startTag(null, TAG_CROSS_PROFILE_WIDGET_PROVIDERS);
                writeAttributeValuesToXml(out, TAG_PROVIDER, this.crossProfileWidgetProviders);
                out.endTag(null, TAG_CROSS_PROFILE_WIDGET_PROVIDERS);
            }
            writePackageListToXml(out, TAG_PERMITTED_ACCESSIBILITY_SERVICES, this.permittedAccessiblityServices);
            writePackageListToXml(out, TAG_PERMITTED_IMES, this.permittedInputMethods);
            writePackageListToXml(out, TAG_PERMITTED_NOTIFICATION_LISTENERS, this.permittedNotificationListeners);
            writePackageListToXml(out, TAG_KEEP_UNINSTALLED_PACKAGES, this.keepUninstalledPackages);
            writePackageListToXml(out, TAG_METERED_DATA_DISABLED_PACKAGES, this.meteredDisabledPackages);
            if (hasUserRestrictions()) {
                UserRestrictionsUtils.writeRestrictions(out, this.userRestrictions, TAG_USER_RESTRICTIONS);
            }
            if (!this.defaultEnabledRestrictionsAlreadySet.isEmpty()) {
                out.startTag(null, TAG_DEFAULT_ENABLED_USER_RESTRICTIONS);
                writeAttributeValuesToXml(out, TAG_RESTRICTION, this.defaultEnabledRestrictionsAlreadySet);
                out.endTag(null, TAG_DEFAULT_ENABLED_USER_RESTRICTIONS);
            }
            if (!TextUtils.isEmpty(this.shortSupportMessage)) {
                out.startTag(null, TAG_SHORT_SUPPORT_MESSAGE);
                out.text(this.shortSupportMessage.toString());
                out.endTag(null, TAG_SHORT_SUPPORT_MESSAGE);
            }
            if (!TextUtils.isEmpty(this.longSupportMessage)) {
                out.startTag(null, TAG_LONG_SUPPORT_MESSAGE);
                out.text(this.longSupportMessage.toString());
                out.endTag(null, TAG_LONG_SUPPORT_MESSAGE);
            }
            if (this.parentAdmin != null) {
                out.startTag(null, TAG_PARENT_ADMIN);
                this.parentAdmin.writeToXml(out);
                out.endTag(null, TAG_PARENT_ADMIN);
            }
            if (this.organizationColor != DEF_ORGANIZATION_COLOR) {
                out.startTag(null, TAG_ORGANIZATION_COLOR);
                out.attribute(null, ATTR_VALUE, Integer.toString(this.organizationColor));
                out.endTag(null, TAG_ORGANIZATION_COLOR);
            }
            if (this.organizationName != null) {
                out.startTag(null, TAG_ORGANIZATION_NAME);
                out.text(this.organizationName);
                out.endTag(null, TAG_ORGANIZATION_NAME);
            }
            if (this.isLogoutEnabled) {
                out.startTag(null, TAG_IS_LOGOUT_ENABLED);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.isLogoutEnabled));
                out.endTag(null, TAG_IS_LOGOUT_ENABLED);
            }
            if (this.mandatoryBackupTransport != null) {
                out.startTag(null, TAG_MANDATORY_BACKUP_TRANSPORT);
                out.attribute(null, ATTR_VALUE, this.mandatoryBackupTransport.flattenToString());
                out.endTag(null, TAG_MANDATORY_BACKUP_TRANSPORT);
            }
            if (this.startUserSessionMessage != null) {
                out.startTag(null, TAG_START_USER_SESSION_MESSAGE);
                out.text(this.startUserSessionMessage);
                out.endTag(null, TAG_START_USER_SESSION_MESSAGE);
            }
            if (this.endUserSessionMessage != null) {
                out.startTag(null, TAG_END_USER_SESSION_MESSAGE);
                out.text(this.endUserSessionMessage);
                out.endTag(null, TAG_END_USER_SESSION_MESSAGE);
            }
        }

        void writePackageListToXml(XmlSerializer out, String outerTag, List<String> packageList) throws IllegalArgumentException, IllegalStateException, IOException {
            if (packageList != null) {
                out.startTag(null, outerTag);
                writeAttributeValuesToXml(out, "item", packageList);
                out.endTag(null, outerTag);
            }
        }

        void writeAttributeValuesToXml(XmlSerializer out, String tag, Collection<String> values) throws IOException {
            for (String value : values) {
                out.startTag(null, tag);
                out.attribute(null, ATTR_VALUE, value);
                out.endTag(null, tag);
            }
        }

        void readFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
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
                        String tag = parser.getName();
                        if (HwActiveAdmin.TAG_POLICIES.equals(tag)) {
                            this.mHwActiveAdmin = new HwActiveAdmin();
                            this.mHwActiveAdmin.readPoliciesFromXml(parser);
                        } else if (TAG_POLICIES.equals(tag)) {
                            this.info.readPoliciesFromXml(parser);
                        } else if (TAG_PASSWORD_QUALITY.equals(tag)) {
                            this.minimumPasswordMetrics.quality = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MIN_PASSWORD_LENGTH.equals(tag)) {
                            this.minimumPasswordMetrics.length = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_PASSWORD_HISTORY_LENGTH.equals(tag)) {
                            this.passwordHistoryLength = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MIN_PASSWORD_UPPERCASE.equals(tag)) {
                            this.minimumPasswordMetrics.upperCase = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MIN_PASSWORD_LOWERCASE.equals(tag)) {
                            this.minimumPasswordMetrics.lowerCase = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MIN_PASSWORD_LETTERS.equals(tag)) {
                            this.minimumPasswordMetrics.letters = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MIN_PASSWORD_NUMERIC.equals(tag)) {
                            this.minimumPasswordMetrics.numeric = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MIN_PASSWORD_SYMBOLS.equals(tag)) {
                            this.minimumPasswordMetrics.symbols = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MIN_PASSWORD_NONLETTER.equals(tag)) {
                            this.minimumPasswordMetrics.nonLetter = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MAX_TIME_TO_UNLOCK.equals(tag)) {
                            this.maximumTimeToUnlock = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_STRONG_AUTH_UNLOCK_TIMEOUT.equals(tag)) {
                            this.strongAuthUnlockTimeout = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MAX_FAILED_PASSWORD_WIPE.equals(tag)) {
                            this.maximumFailedPasswordsForWipe = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_SPECIFIES_GLOBAL_PROXY.equals(tag)) {
                            this.specifiesGlobalProxy = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_GLOBAL_PROXY_SPEC.equals(tag)) {
                            this.globalProxySpec = parser.getAttributeValue(null, ATTR_VALUE);
                        } else if (TAG_GLOBAL_PROXY_EXCLUSION_LIST.equals(tag)) {
                            this.globalProxyExclusionList = parser.getAttributeValue(null, ATTR_VALUE);
                        } else if (TAG_PASSWORD_EXPIRATION_TIMEOUT.equals(tag)) {
                            this.passwordExpirationTimeout = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_PASSWORD_EXPIRATION_DATE.equals(tag)) {
                            this.passwordExpirationDate = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_ENCRYPTION_REQUESTED.equals(tag)) {
                            this.encryptionRequested = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_TEST_ONLY_ADMIN.equals(tag)) {
                            this.testOnlyAdmin = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_DISABLE_CAMERA.equals(tag)) {
                            this.disableCamera = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_DISABLE_CALLER_ID.equals(tag)) {
                            this.disableCallerId = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_DISABLE_CONTACTS_SEARCH.equals(tag)) {
                            this.disableContactsSearch = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_DISABLE_BLUETOOTH_CONTACT_SHARING.equals(tag)) {
                            this.disableBluetoothContactSharing = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_DISABLE_SCREEN_CAPTURE.equals(tag)) {
                            this.disableScreenCapture = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_REQUIRE_AUTO_TIME.equals(tag)) {
                            this.requireAutoTime = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_FORCE_EPHEMERAL_USERS.equals(tag)) {
                            this.forceEphemeralUsers = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_IS_NETWORK_LOGGING_ENABLED.equals(tag)) {
                            this.isNetworkLoggingEnabled = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                            this.lastNetworkLoggingNotificationTimeMs = Long.parseLong(parser.getAttributeValue(null, ATTR_LAST_NETWORK_LOGGING_NOTIFICATION));
                            this.numNetworkLoggingNotifications = Integer.parseInt(parser.getAttributeValue(null, ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS));
                        } else if (TAG_DISABLE_KEYGUARD_FEATURES.equals(tag)) {
                            this.disabledKeyguardFeatures = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_DISABLE_ACCOUNT_MANAGEMENT.equals(tag)) {
                            readAttributeValues(parser, TAG_ACCOUNT_TYPE, this.accountTypesWithManagementDisabled);
                        } else if (TAG_MANAGE_TRUST_AGENT_FEATURES.equals(tag)) {
                            this.trustAgentInfos = getAllTrustAgentInfos(parser, tag);
                        } else if (TAG_CROSS_PROFILE_WIDGET_PROVIDERS.equals(tag)) {
                            this.crossProfileWidgetProviders = new ArrayList();
                            readAttributeValues(parser, TAG_PROVIDER, this.crossProfileWidgetProviders);
                        } else if (TAG_PERMITTED_ACCESSIBILITY_SERVICES.equals(tag)) {
                            this.permittedAccessiblityServices = readPackageList(parser, tag);
                        } else if (TAG_PERMITTED_IMES.equals(tag)) {
                            this.permittedInputMethods = readPackageList(parser, tag);
                        } else if (TAG_PERMITTED_NOTIFICATION_LISTENERS.equals(tag)) {
                            this.permittedNotificationListeners = readPackageList(parser, tag);
                        } else if (TAG_KEEP_UNINSTALLED_PACKAGES.equals(tag)) {
                            this.keepUninstalledPackages = readPackageList(parser, tag);
                        } else if (TAG_METERED_DATA_DISABLED_PACKAGES.equals(tag)) {
                            this.meteredDisabledPackages = readPackageList(parser, tag);
                        } else if (TAG_USER_RESTRICTIONS.equals(tag)) {
                            this.userRestrictions = UserRestrictionsUtils.readRestrictions(parser);
                        } else if (TAG_DEFAULT_ENABLED_USER_RESTRICTIONS.equals(tag)) {
                            readAttributeValues(parser, TAG_RESTRICTION, this.defaultEnabledRestrictionsAlreadySet);
                        } else if (TAG_SHORT_SUPPORT_MESSAGE.equals(tag)) {
                            if (parser.next() == 4) {
                                this.shortSupportMessage = parser.getText();
                            } else {
                                Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading short support message");
                            }
                        } else if (TAG_LONG_SUPPORT_MESSAGE.equals(tag)) {
                            if (parser.next() == 4) {
                                this.longSupportMessage = parser.getText();
                            } else {
                                Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading long support message");
                            }
                        } else if (TAG_PARENT_ADMIN.equals(tag)) {
                            Preconditions.checkState(this.isParent ^ 1);
                            this.parentAdmin = new ActiveAdmin(this.info, true);
                            this.parentAdmin.readFromXml(parser);
                        } else if (TAG_ORGANIZATION_COLOR.equals(tag)) {
                            this.organizationColor = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_ORGANIZATION_NAME.equals(tag)) {
                            if (parser.next() == 4) {
                                this.organizationName = parser.getText();
                            } else {
                                Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading organization name");
                            }
                        } else if (TAG_IS_LOGOUT_ENABLED.equals(tag)) {
                            this.isLogoutEnabled = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_MANDATORY_BACKUP_TRANSPORT.equals(tag)) {
                            this.mandatoryBackupTransport = ComponentName.unflattenFromString(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_START_USER_SESSION_MESSAGE.equals(tag)) {
                            if (parser.next() == 4) {
                                this.startUserSessionMessage = parser.getText();
                            } else {
                                Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading start session message");
                            }
                        } else if (TAG_END_USER_SESSION_MESSAGE.equals(tag)) {
                            if (parser.next() == 4) {
                                this.endUserSessionMessage = parser.getText();
                            } else {
                                Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading end session message");
                            }
                        } else if (DevicePolicyManagerService.mHwCustDevicePolicyManagerService != null && DevicePolicyManagerService.mHwCustDevicePolicyManagerService.isForbiddenSimplePwdFeatureEnable() && "allow-simple-password".equals(tag)) {
                            this.allowSimplePassword = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else {
                            String str = DevicePolicyManagerService.LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown admin tag: ");
                            stringBuilder.append(tag);
                            Slog.w(str, stringBuilder.toString());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
            }
        }

        private List<String> readPackageList(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            List<String> result = new ArrayList();
            int outerDepth = parser.getDepth();
            while (true) {
                int next = parser.next();
                int outerType = next;
                if (next == 1 || (outerType == 3 && parser.getDepth() <= outerDepth)) {
                    return result;
                }
                if (outerType != 3) {
                    if (outerType != 4) {
                        String outerTag = parser.getName();
                        String packageName;
                        if ("item".equals(outerTag)) {
                            packageName = parser.getAttributeValue(null, ATTR_VALUE);
                            if (packageName != null) {
                                result.add(packageName);
                            } else {
                                String str = DevicePolicyManagerService.LOG_TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Package name missing under ");
                                stringBuilder.append(outerTag);
                                Slog.w(str, stringBuilder.toString());
                            }
                        } else {
                            packageName = DevicePolicyManagerService.LOG_TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Unknown tag under ");
                            stringBuilder2.append(tag);
                            stringBuilder2.append(": ");
                            stringBuilder2.append(outerTag);
                            Slog.w(packageName, stringBuilder2.toString());
                        }
                    }
                }
            }
            return result;
        }

        private void readAttributeValues(XmlPullParser parser, String tag, Collection<String> result) throws XmlPullParserException, IOException {
            result.clear();
            int outerDepthDAM = parser.getDepth();
            while (true) {
                int next = parser.next();
                int typeDAM = next;
                if (next == 1) {
                    return;
                }
                if (typeDAM == 3 && parser.getDepth() <= outerDepthDAM) {
                    return;
                }
                if (typeDAM != 3) {
                    if (typeDAM != 4) {
                        String tagDAM = parser.getName();
                        if (tag.equals(tagDAM)) {
                            result.add(parser.getAttributeValue(null, ATTR_VALUE));
                        } else {
                            String str = DevicePolicyManagerService.LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Expected tag ");
                            stringBuilder.append(tag);
                            stringBuilder.append(" but found ");
                            stringBuilder.append(tagDAM);
                            Slog.e(str, stringBuilder.toString());
                        }
                    }
                }
            }
        }

        private ArrayMap<String, TrustAgentInfo> getAllTrustAgentInfos(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            int outerDepthDAM = parser.getDepth();
            ArrayMap<String, TrustAgentInfo> result = new ArrayMap();
            while (true) {
                int next = parser.next();
                int typeDAM = next;
                if (next == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepthDAM)) {
                    return result;
                }
                if (typeDAM != 3) {
                    if (typeDAM != 4) {
                        String tagDAM = parser.getName();
                        if (TAG_TRUST_AGENT_COMPONENT.equals(tagDAM)) {
                            result.put(parser.getAttributeValue(null, ATTR_VALUE), getTrustAgentInfo(parser, tag));
                        } else {
                            String str = DevicePolicyManagerService.LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown tag under ");
                            stringBuilder.append(tag);
                            stringBuilder.append(": ");
                            stringBuilder.append(tagDAM);
                            Slog.w(str, stringBuilder.toString());
                        }
                    }
                }
            }
            return result;
        }

        private TrustAgentInfo getTrustAgentInfo(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            int outerDepthDAM = parser.getDepth();
            TrustAgentInfo result = new TrustAgentInfo(null);
            while (true) {
                int next = parser.next();
                int typeDAM = next;
                if (next == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepthDAM)) {
                    return result;
                }
                if (typeDAM != 3) {
                    if (typeDAM != 4) {
                        String tagDAM = parser.getName();
                        if (TAG_TRUST_AGENT_COMPONENT_OPTIONS.equals(tagDAM)) {
                            result.options = PersistableBundle.restoreFromXml(parser);
                        } else {
                            String str = DevicePolicyManagerService.LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown tag under ");
                            stringBuilder.append(tag);
                            stringBuilder.append(": ");
                            stringBuilder.append(tagDAM);
                            Slog.w(str, stringBuilder.toString());
                        }
                    }
                }
            }
            return result;
        }

        boolean hasUserRestrictions() {
            return this.userRestrictions != null && this.userRestrictions.size() > 0;
        }

        Bundle ensureUserRestrictions() {
            if (this.userRestrictions == null) {
                this.userRestrictions = new Bundle();
            }
            return this.userRestrictions;
        }

        public void transfer(DeviceAdminInfo deviceAdminInfo) {
            if (hasParentActiveAdmin()) {
                this.parentAdmin.info = deviceAdminInfo;
            }
            this.info = deviceAdminInfo;
        }

        void dump(String prefix, PrintWriter pw) {
            pw.print(prefix);
            pw.print("uid=");
            pw.println(getUid());
            pw.print(prefix);
            pw.print("testOnlyAdmin=");
            pw.println(this.testOnlyAdmin);
            pw.print(prefix);
            pw.println("policies:");
            ArrayList<PolicyInfo> pols = this.info.getUsedPolicies();
            if (pols != null) {
                for (int i = 0; i < pols.size(); i++) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.println(((PolicyInfo) pols.get(i)).tag);
                }
            }
            pw.print(prefix);
            pw.print("passwordQuality=0x");
            pw.println(Integer.toHexString(this.minimumPasswordMetrics.quality));
            pw.print(prefix);
            pw.print("minimumPasswordLength=");
            pw.println(this.minimumPasswordMetrics.length);
            pw.print(prefix);
            pw.print("passwordHistoryLength=");
            pw.println(this.passwordHistoryLength);
            pw.print(prefix);
            pw.print("minimumPasswordUpperCase=");
            pw.println(this.minimumPasswordMetrics.upperCase);
            pw.print(prefix);
            pw.print("minimumPasswordLowerCase=");
            pw.println(this.minimumPasswordMetrics.lowerCase);
            pw.print(prefix);
            pw.print("minimumPasswordLetters=");
            pw.println(this.minimumPasswordMetrics.letters);
            pw.print(prefix);
            pw.print("minimumPasswordNumeric=");
            pw.println(this.minimumPasswordMetrics.numeric);
            pw.print(prefix);
            pw.print("minimumPasswordSymbols=");
            pw.println(this.minimumPasswordMetrics.symbols);
            pw.print(prefix);
            pw.print("minimumPasswordNonLetter=");
            pw.println(this.minimumPasswordMetrics.nonLetter);
            pw.print(prefix);
            pw.print("maximumTimeToUnlock=");
            pw.println(this.maximumTimeToUnlock);
            pw.print(prefix);
            pw.print("strongAuthUnlockTimeout=");
            pw.println(this.strongAuthUnlockTimeout);
            pw.print(prefix);
            pw.print("maximumFailedPasswordsForWipe=");
            pw.println(this.maximumFailedPasswordsForWipe);
            pw.print(prefix);
            pw.print("specifiesGlobalProxy=");
            pw.println(this.specifiesGlobalProxy);
            pw.print(prefix);
            pw.print("passwordExpirationTimeout=");
            pw.println(this.passwordExpirationTimeout);
            pw.print(prefix);
            pw.print("passwordExpirationDate=");
            pw.println(this.passwordExpirationDate);
            if (this.globalProxySpec != null) {
                pw.print(prefix);
                pw.print("globalProxySpec=");
                pw.println(this.globalProxySpec);
            }
            if (this.globalProxyExclusionList != null) {
                pw.print(prefix);
                pw.print("globalProxyEclusionList=");
                pw.println(this.globalProxyExclusionList);
            }
            pw.print(prefix);
            pw.print("encryptionRequested=");
            pw.println(this.encryptionRequested);
            pw.print(prefix);
            pw.print("disableCamera=");
            pw.println(this.disableCamera);
            pw.print(prefix);
            pw.print("disableCallerId=");
            pw.println(this.disableCallerId);
            pw.print(prefix);
            pw.print("disableContactsSearch=");
            pw.println(this.disableContactsSearch);
            pw.print(prefix);
            pw.print("disableBluetoothContactSharing=");
            pw.println(this.disableBluetoothContactSharing);
            pw.print(prefix);
            pw.print("disableScreenCapture=");
            pw.println(this.disableScreenCapture);
            pw.print(prefix);
            pw.print("requireAutoTime=");
            pw.println(this.requireAutoTime);
            pw.print(prefix);
            pw.print("forceEphemeralUsers=");
            pw.println(this.forceEphemeralUsers);
            pw.print(prefix);
            pw.print("isNetworkLoggingEnabled=");
            pw.println(this.isNetworkLoggingEnabled);
            pw.print(prefix);
            pw.print("disabledKeyguardFeatures=");
            pw.println(this.disabledKeyguardFeatures);
            pw.print(prefix);
            pw.print("crossProfileWidgetProviders=");
            pw.println(this.crossProfileWidgetProviders);
            if (this.permittedAccessiblityServices != null) {
                pw.print(prefix);
                pw.print("permittedAccessibilityServices=");
                pw.println(this.permittedAccessiblityServices);
            }
            if (this.permittedInputMethods != null) {
                pw.print(prefix);
                pw.print("permittedInputMethods=");
                pw.println(this.permittedInputMethods);
            }
            if (this.permittedNotificationListeners != null) {
                pw.print(prefix);
                pw.print("permittedNotificationListeners=");
                pw.println(this.permittedNotificationListeners);
            }
            if (this.keepUninstalledPackages != null) {
                pw.print(prefix);
                pw.print("keepUninstalledPackages=");
                pw.println(this.keepUninstalledPackages);
            }
            pw.print(prefix);
            pw.print("organizationColor=");
            pw.println(this.organizationColor);
            if (this.organizationName != null) {
                pw.print(prefix);
                pw.print("organizationName=");
                pw.println(this.organizationName);
            }
            pw.print(prefix);
            pw.println("userRestrictions:");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            UserRestrictionsUtils.dumpRestrictions(pw, stringBuilder.toString(), this.userRestrictions);
            pw.print(prefix);
            pw.print("defaultEnabledRestrictionsAlreadySet=");
            pw.println(this.defaultEnabledRestrictionsAlreadySet);
            pw.print(prefix);
            pw.print("isParent=");
            pw.println(this.isParent);
            if (this.parentAdmin != null) {
                pw.print(prefix);
                pw.println("parentAdmin:");
                ActiveAdmin activeAdmin = this.parentAdmin;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("  ");
                activeAdmin.dump(stringBuilder2.toString(), pw);
            }
        }
    }

    public static class DevicePolicyData {
        boolean doNotAskCredentialsOnBoot = false;
        final ArraySet<String> mAcceptedCaCertificates = new ArraySet();
        boolean mAdminBroadcastPending = false;
        final ArrayList<ActiveAdmin> mAdminList = new ArrayList();
        final ArrayMap<ComponentName, ActiveAdmin> mAdminMap = new ArrayMap();
        Set<String> mAffiliationIds = new ArraySet();
        boolean mCurrentInputMethodSet = false;
        final ArrayMap<String, List<String>> mDelegationMap = new ArrayMap();
        boolean mDeviceProvisioningConfigApplied = false;
        int mFailedPasswordAttempts = 0;
        PersistableBundle mInitBundle = null;
        boolean mIsCurrentPwdSimple = true;
        long mLastBugReportRequestTime = -1;
        long mLastMaximumTimeToLock = -1;
        long mLastNetworkLogsRetrievalTime = -1;
        long mLastSecurityLogRetrievalTime = -1;
        int mLockTaskFeatures = 16;
        List<String> mLockTaskPackages = new ArrayList();
        Set<String> mOwnerInstalledCaCerts = new ArraySet();
        boolean mPaired = false;
        int mPasswordOwner = -1;
        long mPasswordTokenHandle = 0;
        boolean mPasswordValidAtLastCheckpoint = true;
        int mPermissionPolicy;
        final ArrayList<ComponentName> mRemovingAdmins = new ArrayList();
        ComponentName mRestrictionsProvider;
        boolean mStatusBarDisabled = false;
        int mUserHandle;
        int mUserProvisioningState;
        boolean mUserSetupComplete = false;

        public DevicePolicyData(int userHandle) {
            this.mUserHandle = userHandle;
        }
    }

    @VisibleForTesting
    static class Injector {
        public final Context mContext;

        Injector(Context context) {
            this.mContext = context;
        }

        public boolean hasFeature() {
            return getPackageManager().hasSystemFeature("android.software.device_admin");
        }

        Context createContextAsUser(UserHandle user) throws NameNotFoundException {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
        }

        Resources getResources() {
            return this.mContext.getResources();
        }

        Owners newOwners() {
            return new Owners(getUserManager(), getUserManagerInternal(), getPackageManagerInternal());
        }

        UserManager getUserManager() {
            return UserManager.get(this.mContext);
        }

        UserManagerInternal getUserManagerInternal() {
            return (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }

        PackageManagerInternal getPackageManagerInternal() {
            return (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }

        UsageStatsManagerInternal getUsageStatsManagerInternal() {
            return (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        }

        NetworkPolicyManagerInternal getNetworkPolicyManagerInternal() {
            return (NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class);
        }

        NotificationManager getNotificationManager() {
            return (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        }

        IIpConnectivityMetrics getIIpConnectivityMetrics() {
            return Stub.asInterface(ServiceManager.getService("connmetrics"));
        }

        PackageManager getPackageManager() {
            return this.mContext.getPackageManager();
        }

        PowerManagerInternal getPowerManagerInternal() {
            return (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        }

        TelephonyManager getTelephonyManager() {
            return TelephonyManager.from(this.mContext);
        }

        TrustManager getTrustManager() {
            return (TrustManager) this.mContext.getSystemService("trust");
        }

        AlarmManager getAlarmManager() {
            return (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        }

        IWindowManager getIWindowManager() {
            return IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        }

        IActivityManager getIActivityManager() {
            return ActivityManager.getService();
        }

        ActivityManagerInternal getActivityManagerInternal() {
            return (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }

        IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        IBackupManager getIBackupManager() {
            return IBackupManager.Stub.asInterface(ServiceManager.getService(HealthServiceWrapper.INSTANCE_HEALTHD));
        }

        IAudioService getIAudioService() {
            return IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        }

        boolean isBuildDebuggable() {
            return Build.IS_DEBUGGABLE;
        }

        LockPatternUtils newLockPatternUtils() {
            return new LockPatternUtils(this.mContext);
        }

        boolean storageManagerIsFileBasedEncryptionEnabled() {
            return StorageManager.isFileEncryptedNativeOnly();
        }

        boolean storageManagerIsNonDefaultBlockEncrypted() {
            long identity = Binder.clearCallingIdentity();
            try {
                boolean isNonDefaultBlockEncrypted = StorageManager.isNonDefaultBlockEncrypted();
                return isNonDefaultBlockEncrypted;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        boolean storageManagerIsEncrypted() {
            return StorageManager.isEncrypted();
        }

        boolean storageManagerIsEncryptable() {
            return StorageManager.isEncryptable();
        }

        Looper getMyLooper() {
            return Looper.myLooper();
        }

        WifiManager getWifiManager() {
            return (WifiManager) this.mContext.getSystemService(WifiManager.class);
        }

        long binderClearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        void binderRestoreCallingIdentity(long token) {
            Binder.restoreCallingIdentity(token);
        }

        int binderGetCallingUid() {
            return Binder.getCallingUid();
        }

        int binderGetCallingPid() {
            return Binder.getCallingPid();
        }

        UserHandle binderGetCallingUserHandle() {
            return Binder.getCallingUserHandle();
        }

        boolean binderIsCallingUidMyUid() {
            return Binder.getCallingUid() == Process.myUid();
        }

        void binderWithCleanCallingIdentity(ThrowingRunnable action) {
            Binder.withCleanCallingIdentity(action);
        }

        final int userHandleGetCallingUserId() {
            return UserHandle.getUserId(binderGetCallingUid());
        }

        File environmentGetUserSystemDirectory(int userId) {
            return Environment.getUserSystemDirectory(userId);
        }

        void powerManagerGoToSleep(long time, int reason, int flags) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).goToSleep(time, reason, flags);
        }

        void powerManagerReboot(String reason) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot(reason);
        }

        void recoverySystemRebootWipeUserData(boolean shutdown, String reason, boolean force, boolean wipeEuicc) throws IOException {
            RecoverySystem.rebootWipeUserData(this.mContext, shutdown, reason, force, wipeEuicc);
        }

        boolean systemPropertiesGetBoolean(String key, boolean def) {
            return SystemProperties.getBoolean(key, def);
        }

        long systemPropertiesGetLong(String key, long def) {
            return SystemProperties.getLong(key, def);
        }

        String systemPropertiesGet(String key, String def) {
            return SystemProperties.get(key, def);
        }

        String systemPropertiesGet(String key) {
            return SystemProperties.get(key);
        }

        void systemPropertiesSet(String key, String value) {
            SystemProperties.set(key, value);
        }

        boolean userManagerIsSplitSystemUser() {
            return UserManager.isSplitSystemUser();
        }

        String getDevicePolicyFilePathForSystemUser() {
            return "/data/system/";
        }

        PendingIntent pendingIntentGetActivityAsUser(Context context, int requestCode, Intent intent, int flags, Bundle options, UserHandle user) {
            return PendingIntent.getActivityAsUser(context, requestCode, intent, flags, options, user);
        }

        void registerContentObserver(Uri uri, boolean notifyForDescendents, ContentObserver observer, int userHandle) {
            this.mContext.getContentResolver().registerContentObserver(uri, notifyForDescendents, observer, userHandle);
        }

        int settingsSecureGetIntForUser(String name, int def, int userHandle) {
            return Secure.getIntForUser(this.mContext.getContentResolver(), name, def, userHandle);
        }

        String settingsSecureGetStringForUser(String name, int userHandle) {
            return Secure.getStringForUser(this.mContext.getContentResolver(), name, userHandle);
        }

        void settingsSecurePutIntForUser(String name, int value, int userHandle) {
            Secure.putIntForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsSecurePutStringForUser(String name, String value, int userHandle) {
            Secure.putStringForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsGlobalPutStringForUser(String name, String value, int userHandle) {
            Global.putStringForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsSecurePutInt(String name, int value) {
            Secure.putInt(this.mContext.getContentResolver(), name, value);
        }

        int settingsGlobalGetInt(String name, int def) {
            return Global.getInt(this.mContext.getContentResolver(), name, def);
        }

        String settingsGlobalGetString(String name) {
            return Global.getString(this.mContext.getContentResolver(), name);
        }

        void settingsGlobalPutInt(String name, int value) {
            Global.putInt(this.mContext.getContentResolver(), name, value);
        }

        void settingsSecurePutString(String name, String value) {
            Secure.putString(this.mContext.getContentResolver(), name, value);
        }

        void settingsGlobalPutString(String name, String value) {
            Global.putString(this.mContext.getContentResolver(), name, value);
        }

        void settingsSystemPutStringForUser(String name, String value, int userId) {
            System.putStringForUser(this.mContext.getContentResolver(), name, value, userId);
        }

        void securityLogSetLoggingEnabledProperty(boolean enabled) {
            SecurityLog.setLoggingEnabledProperty(enabled);
        }

        boolean securityLogGetLoggingEnabledProperty() {
            return SecurityLog.getLoggingEnabledProperty();
        }

        boolean securityLogIsLoggingEnabled() {
            return SecurityLog.isLoggingEnabled();
        }

        KeyChainConnection keyChainBindAsUser(UserHandle user) throws InterruptedException {
            return KeyChain.bindAsUser(this.mContext, user);
        }

        void postOnSystemServerInitThreadPool(Runnable runnable) {
            SystemServerInitThreadPool.get().submit(runnable, DevicePolicyManagerService.LOG_TAG);
        }

        public TransferOwnershipMetadataManager newTransferOwnershipMetadataManager() {
            return new TransferOwnershipMetadataManager();
        }

        public void runCryptoSelfTest() {
            CryptoTestHelper.runAndLogSelfTest();
        }
    }

    @VisibleForTesting
    final class LocalService extends DevicePolicyManagerInternal {
        private List<OnCrossProfileWidgetProvidersChangeListener> mWidgetProviderListeners;

        LocalService() {
        }

        public List<String> getCrossProfileWidgetProviders(int profileId) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                if (DevicePolicyManagerService.this.mOwners == null) {
                    List emptyList = Collections.emptyList();
                    return emptyList;
                }
                ComponentName ownerComponent = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(profileId);
                if (ownerComponent == null) {
                    List emptyList2 = Collections.emptyList();
                    return emptyList2;
                }
                List list;
                ActiveAdmin admin = (ActiveAdmin) DevicePolicyManagerService.this.getUserDataUnchecked(profileId).mAdminMap.get(ownerComponent);
                if (!(admin == null || admin.crossProfileWidgetProviders == null)) {
                    if (!admin.crossProfileWidgetProviders.isEmpty()) {
                        list = admin.crossProfileWidgetProviders;
                        return list;
                    }
                }
                list = Collections.emptyList();
                return list;
            }
        }

        public void addOnCrossProfileWidgetProvidersChangeListener(OnCrossProfileWidgetProvidersChangeListener listener) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                if (this.mWidgetProviderListeners == null) {
                    this.mWidgetProviderListeners = new ArrayList();
                }
                if (!this.mWidgetProviderListeners.contains(listener)) {
                    this.mWidgetProviderListeners.add(listener);
                }
            }
        }

        public boolean isActiveAdminWithPolicy(int uid, int reqPolicy) {
            boolean z;
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                z = DevicePolicyManagerService.this.getActiveAdminWithPolicyForUidLocked(null, reqPolicy, uid) != null;
            }
            return z;
        }

        private void notifyCrossProfileProvidersChanged(int userId, List<String> packages) {
            List<OnCrossProfileWidgetProvidersChangeListener> listeners;
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                listeners = new ArrayList(this.mWidgetProviderListeners);
            }
            int listenerCount = listeners.size();
            for (int i = 0; i < listenerCount; i++) {
                ((OnCrossProfileWidgetProvidersChangeListener) listeners.get(i)).onCrossProfileWidgetProvidersChanged(userId, packages);
            }
        }

        public Intent createShowAdminSupportIntent(int userId, boolean useDefaultIfNoAdmin) {
            ComponentName profileOwner = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(userId);
            if (profileOwner != null) {
                return DevicePolicyManagerService.this.createShowAdminSupportIntent(profileOwner, userId);
            }
            Pair<Integer, ComponentName> deviceOwner = DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserIdAndComponent();
            if (deviceOwner != null && ((Integer) deviceOwner.first).intValue() == userId) {
                return DevicePolicyManagerService.this.createShowAdminSupportIntent((ComponentName) deviceOwner.second, userId);
            }
            if (useDefaultIfNoAdmin) {
                return DevicePolicyManagerService.this.createShowAdminSupportIntent(null, userId);
            }
            return null;
        }

        public Intent createUserRestrictionSupportIntent(int userId, String userRestriction) {
            long ident = DevicePolicyManagerService.this.mInjector.binderClearCallingIdentity();
            try {
                int source = DevicePolicyManagerService.this.mUserManager.getUserRestrictionSource(userRestriction, UserHandle.of(userId));
                if ((source & 1) != 0) {
                    return null;
                }
                boolean enforcedByPo = false;
                boolean enforcedByDo = (source & 2) != 0;
                if ((source & 4) != 0) {
                    enforcedByPo = true;
                }
                if (enforcedByDo && enforcedByPo) {
                    return DevicePolicyManagerService.this.createShowAdminSupportIntent(null, userId);
                }
                if (enforcedByPo) {
                    ComponentName profileOwner = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(userId);
                    if (profileOwner != null) {
                        return DevicePolicyManagerService.this.createShowAdminSupportIntent(profileOwner, userId);
                    }
                    return null;
                } else if (!enforcedByDo) {
                    return null;
                } else {
                    Pair<Integer, ComponentName> deviceOwner = DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserIdAndComponent();
                    if (deviceOwner != null) {
                        return DevicePolicyManagerService.this.createShowAdminSupportIntent((ComponentName) deviceOwner.second, ((Integer) deviceOwner.first).intValue());
                    }
                    return null;
                }
            } finally {
                DevicePolicyManagerService.this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }

        public boolean isUserAffiliatedWithDevice(int userId) {
            return DevicePolicyManagerService.this.isUserAffiliatedWithDeviceLocked(userId);
        }

        public void reportSeparateProfileChallengeChanged(int userId) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                DevicePolicyManagerService.this.updateMaximumTimeToLockLocked(userId);
            }
        }

        public boolean canUserHaveUntrustedCredentialReset(int userId) {
            return DevicePolicyManagerService.this.canUserHaveUntrustedCredentialReset(userId);
        }

        public CharSequence getPrintingDisabledReasonForUser(int userId) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                DevicePolicyData policy = DevicePolicyManagerService.this.getUserData(userId);
                if (DevicePolicyManagerService.this.mUserManager.hasUserRestriction("no_printing", UserHandle.of(userId))) {
                    String ownerPackage = DevicePolicyManagerService.this.mOwners.getProfileOwnerPackage(userId);
                    if (ownerPackage == null) {
                        ownerPackage = DevicePolicyManagerService.this.mOwners.getDeviceOwnerPackageName();
                    }
                    PackageManager pm = DevicePolicyManagerService.this.mInjector.getPackageManager();
                    try {
                        PackageInfo packageInfo = pm.getPackageInfo(ownerPackage, 0);
                        if (packageInfo == null) {
                            Log.e(DevicePolicyManagerService.LOG_TAG, "packageInfo is inexplicably null");
                            return null;
                        }
                        ApplicationInfo appInfo = packageInfo.applicationInfo;
                        if (appInfo == null) {
                            Log.e(DevicePolicyManagerService.LOG_TAG, "appInfo is inexplicably null");
                            return null;
                        }
                        if (pm.getApplicationLabel(appInfo) == null) {
                            Log.e(DevicePolicyManagerService.LOG_TAG, "appLabel is inexplicably null");
                            return null;
                        }
                        String string = ActivityThread.currentActivityThread().getSystemUiContext().getResources().getString(17040954, new Object[]{appLabel});
                        return string;
                    } catch (NameNotFoundException e) {
                        Log.e(DevicePolicyManagerService.LOG_TAG, "getPackageInfo error", e);
                        return null;
                    }
                }
                Log.e(DevicePolicyManagerService.LOG_TAG, "printing is enabled");
                return null;
            }
        }

        protected DevicePolicyCache getDevicePolicyCache() {
            return DevicePolicyManagerService.this.mPolicyCache;
        }
    }

    protected static class RestrictionsListener implements UserRestrictionsListener {
        private Context mContext;

        public RestrictionsListener(Context context) {
            this.mContext = context;
        }

        public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            if (newRestrictions.getBoolean("no_sharing_into_profile") != prevRestrictions.getBoolean("no_sharing_into_profile")) {
                Intent intent = new Intent("android.app.action.DATA_SHARING_RESTRICTION_CHANGED");
                intent.setPackage(DevicePolicyManagerService.getManagedProvisioningPackage(this.mContext));
                intent.putExtra("android.intent.extra.USER_ID", userId);
                intent.addFlags(268435456);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
            }
        }
    }

    private class SetupContentObserver extends ContentObserver {
        private final Uri mDefaultImeChanged = Secure.getUriFor("default_input_method");
        private final Uri mDeviceProvisioned = Global.getUriFor("device_provisioned");
        private final Uri mPaired = Secure.getUriFor("device_paired");
        @GuardedBy("getLockObject()")
        private Set<Integer> mUserIdsWithPendingChangesByOwner = new ArraySet();
        private final Uri mUserSetupComplete = Secure.getUriFor("user_setup_complete");

        public SetupContentObserver(Handler handler) {
            super(handler);
        }

        void register() {
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mUserSetupComplete, false, this, -1);
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mDeviceProvisioned, false, this, -1);
            if (DevicePolicyManagerService.this.mIsWatch) {
                DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mPaired, false, this, -1);
            }
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mDefaultImeChanged, false, this, -1);
        }

        @GuardedBy("getLockObject()")
        private void addPendingChangeByOwnerLocked(int userId) {
            this.mUserIdsWithPendingChangesByOwner.add(Integer.valueOf(userId));
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mUserSetupComplete.equals(uri) || (DevicePolicyManagerService.this.mIsWatch && this.mPaired.equals(uri))) {
                DevicePolicyManagerService.this.updateUserSetupCompleteAndPaired();
            } else if (this.mDeviceProvisioned.equals(uri)) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    DevicePolicyManagerService.this.setDeviceOwnerSystemPropertyLocked();
                }
            } else if (this.mDefaultImeChanged.equals(uri)) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    if (this.mUserIdsWithPendingChangesByOwner.contains(Integer.valueOf(userId))) {
                        this.mUserIdsWithPendingChangesByOwner.remove(Integer.valueOf(userId));
                    } else {
                        DevicePolicyManagerService.this.getUserData(userId).mCurrentInputMethodSet = false;
                        DevicePolicyManagerService.this.saveSettingsLocked(userId);
                    }
                }
            }
        }
    }

    interface Stats {
        public static final int COUNT = 1;
        public static final int LOCK_GUARD_GUARD = 0;
    }

    public static final class Lifecycle extends SystemService {
        private BaseIDevicePolicyManager mService;

        public Lifecycle(Context context) {
            super(context);
            IHwDevicePolicyManagerService iwms = HwDevicePolicyFactory.getHuaweiDevicePolicyManagerService();
            if (iwms != null) {
                this.mService = iwms.getInstance(context);
                return;
            }
            String dpmsClassName = context.getResources().getString(17039796);
            if (TextUtils.isEmpty(dpmsClassName)) {
                dpmsClassName = DevicePolicyManagerService.class.getName();
            }
            try {
                this.mService = (BaseIDevicePolicyManager) Class.forName(dpmsClassName).getConstructor(new Class[]{Context.class}).newInstance(new Object[]{context});
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to instantiate DevicePolicyManagerService with class name: ");
                stringBuilder.append(dpmsClassName);
                throw new IllegalStateException(stringBuilder.toString(), e);
            }
        }

        public void onStart() {
            publishBinderService("device_policy", this.mService);
        }

        public void onBootPhase(int phase) {
            this.mService.systemReady(phase);
        }

        public void onStartUser(int userHandle) {
            this.mService.handleStartUser(userHandle);
        }

        public void onUnlockUser(int userHandle) {
            this.mService.handleUnlockUser(userHandle);
        }

        public void onStopUser(int userHandle) {
            this.mService.handleStopUser(userHandle);
        }
    }

    static {
        SECURE_SETTINGS_WHITELIST.add("default_input_method");
        SECURE_SETTINGS_WHITELIST.add("skip_first_use_hints");
        SECURE_SETTINGS_WHITELIST.add("install_non_market_apps");
        SECURE_SETTINGS_DEVICEOWNER_WHITELIST.addAll(SECURE_SETTINGS_WHITELIST);
        SECURE_SETTINGS_DEVICEOWNER_WHITELIST.add("location_mode");
        GLOBAL_SETTINGS_WHITELIST.add("adb_enabled");
        GLOBAL_SETTINGS_WHITELIST.add("auto_time");
        GLOBAL_SETTINGS_WHITELIST.add("auto_time_zone");
        GLOBAL_SETTINGS_WHITELIST.add("data_roaming");
        GLOBAL_SETTINGS_WHITELIST.add("usb_mass_storage_enabled");
        GLOBAL_SETTINGS_WHITELIST.add("wifi_sleep_policy");
        GLOBAL_SETTINGS_WHITELIST.add("stay_on_while_plugged_in");
        GLOBAL_SETTINGS_WHITELIST.add("wifi_device_owner_configs_lockdown");
        GLOBAL_SETTINGS_DEPRECATED.add("bluetooth_on");
        GLOBAL_SETTINGS_DEPRECATED.add("development_settings_enabled");
        GLOBAL_SETTINGS_DEPRECATED.add("mode_ringer");
        GLOBAL_SETTINGS_DEPRECATED.add("network_preference");
        GLOBAL_SETTINGS_DEPRECATED.add("wifi_on");
        SYSTEM_SETTINGS_WHITELIST.add("screen_brightness");
        SYSTEM_SETTINGS_WHITELIST.add("screen_brightness_mode");
        SYSTEM_SETTINGS_WHITELIST.add("screen_off_timeout");
        boolean z = true;
        if (!(Build.IS_ENG || SystemProperties.getInt("debug.dpm.lock_guard", 0) == 1)) {
            z = false;
        }
        ENABLE_LOCK_GUARD = z;
    }

    final Object getLockObject() {
        if (ENABLE_LOCK_GUARD) {
            long start = this.mStatLogger.getTime();
            LockGuard.guard(7);
            this.mStatLogger.logDurationStat(0, start);
        }
        return this.mLockDoNoUseDirectly;
    }

    final void ensureLocked() {
        if (!Thread.holdsLock(this.mLockDoNoUseDirectly)) {
            Slog.wtfStack(LOG_TAG, "Not holding DPMS lock.");
        }
    }

    private void handlePackagesChanged(String packageName, int userHandle) {
        boolean removedAdmin = false;
        DevicePolicyData policy = getUserData(userHandle);
        synchronized (getLockObject()) {
            for (int i = policy.mAdminList.size() - 1; i >= 0; i--) {
                ActiveAdmin aa = (ActiveAdmin) policy.mAdminList.get(i);
                try {
                    String adminPackage = aa.info.getPackageName();
                    if ((packageName == null || packageName.equals(adminPackage)) && (this.mIPackageManager.getPackageInfo(adminPackage, 0, userHandle) == null || this.mIPackageManager.getReceiverInfo(aa.info.getComponent(), 786432, userHandle) == null)) {
                        removedAdmin = true;
                        policy.mAdminList.remove(i);
                        policy.mAdminMap.remove(aa.info.getComponent());
                        pushActiveAdminPackagesLocked(userHandle);
                        pushMeteredDisabledPackagesLocked(userHandle);
                    }
                } catch (RemoteException e) {
                }
            }
            if (removedAdmin) {
                validatePasswordOwnerLocked(policy);
            }
            boolean removedDelegate = false;
            for (int i2 = policy.mDelegationMap.size() - 1; i2 >= 0; i2--) {
                if (isRemovedPackage(packageName, (String) policy.mDelegationMap.keyAt(i2), userHandle)) {
                    policy.mDelegationMap.removeAt(i2);
                    removedDelegate = true;
                }
            }
            ComponentName owner = getOwnerComponent(userHandle);
            if (!(packageName == null || owner == null || !owner.getPackageName().equals(packageName))) {
                startOwnerService(userHandle, "package-broadcast");
            }
            if (removedAdmin || removedDelegate) {
                saveSettingsLocked(policy.mUserHandle);
            }
        }
        if (removedAdmin) {
            pushUserRestrictions(userHandle);
        }
    }

    private boolean isRemovedPackage(String changedPackage, String targetPackage, int userHandle) {
        boolean z = false;
        if (targetPackage != null) {
            if (changedPackage != null) {
                try {
                    if (changedPackage.equals(targetPackage)) {
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            if (this.mIPackageManager.getPackageInfo(targetPackage, 0, userHandle) == null) {
                z = true;
            }
        }
        return z;
    }

    public DevicePolicyManagerService(Context context) {
        this(new Injector(context));
    }

    @VisibleForTesting
    DevicePolicyManagerService(Injector injector) {
        this.mPolicyCache = new DevicePolicyCacheImpl();
        this.mPackagesToRemove = new ArraySet();
        this.mToken = new Binder();
        this.mIsMDMDeviceOwnerAPI = false;
        this.mRemoteBugreportServiceIsActive = new AtomicBoolean();
        this.mRemoteBugreportSharingAccepted = new AtomicBoolean();
        this.mStatLogger = new StatLogger(new String[]{"LockGuard.guard()"});
        this.mLockDoNoUseDirectly = LockGuard.installNewLock(7, true);
        this.mRemoteBugreportTimeoutRunnable = new Runnable() {
            public void run() {
                if (DevicePolicyManagerService.this.mRemoteBugreportServiceIsActive.get()) {
                    DevicePolicyManagerService.this.onBugreportFailed();
                }
            }
        };
        this.mRemoteBugreportFinishedReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.REMOTE_BUGREPORT_DISPATCH".equals(intent.getAction()) && DevicePolicyManagerService.this.mRemoteBugreportServiceIsActive.get()) {
                    DevicePolicyManagerService.this.onBugreportFinished(intent);
                }
            }
        };
        this.mRemoteBugreportConsentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                DevicePolicyManagerService.this.mInjector.getNotificationManager().cancel(DevicePolicyManagerService.LOG_TAG, 678432343);
                if ("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED".equals(action)) {
                    DevicePolicyManagerService.this.onBugreportSharingAccepted();
                } else if ("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED".equals(action)) {
                    DevicePolicyManagerService.this.onBugreportSharingDeclined();
                }
                DevicePolicyManagerService.this.mContext.unregisterReceiver(DevicePolicyManagerService.this.mRemoteBugreportConsentReceiver);
            }
        };
        this.mUserData = new SparseArray();
        this.mUserPasswordMetrics = new SparseArray();
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                final int userHandle = intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId());
                if ("android.intent.action.USER_STARTED".equals(action) && userHandle == DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserId()) {
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        if (DevicePolicyManagerService.this.isNetworkLoggingEnabledInternalLocked()) {
                            DevicePolicyManagerService.this.setNetworkLoggingActiveInternal(true);
                        }
                    }
                }
                if ("android.intent.action.BOOT_COMPLETED".equals(action) && userHandle == DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserId() && DevicePolicyManagerService.this.getDeviceOwnerRemoteBugreportUri() != null) {
                    IntentFilter filterConsent = new IntentFilter();
                    filterConsent.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED");
                    filterConsent.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED");
                    DevicePolicyManagerService.this.mContext.registerReceiver(DevicePolicyManagerService.this.mRemoteBugreportConsentReceiver, filterConsent);
                    DevicePolicyManagerService.this.mInjector.getNotificationManager().notifyAsUser(DevicePolicyManagerService.LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(DevicePolicyManagerService.this.mContext, 3), UserHandle.ALL);
                }
                if ("android.intent.action.BOOT_COMPLETED".equals(action) || DevicePolicyManagerService.ACTION_EXPIRED_PASSWORD_NOTIFICATION.equals(action)) {
                    DevicePolicyManagerService.this.mHandler.post(new Runnable() {
                        public void run() {
                            DevicePolicyManagerService.this.handlePasswordExpirationNotification(userHandle);
                        }
                    });
                }
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_ADDED", userHandle);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybePauseDeviceWideLoggingLocked();
                    }
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_REMOVED", userHandle);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        boolean isRemovedUserAffiliated = DevicePolicyManagerService.this.isUserAffiliatedWithDeviceLocked(userHandle);
                        DevicePolicyManagerService.this.removeUserData(userHandle);
                        if (!isRemovedUserAffiliated) {
                            DevicePolicyManagerService.this.discardDeviceWideLogsLocked();
                            DevicePolicyManagerService.this.maybeResumeDeviceWideLoggingLocked();
                        }
                    }
                } else if ("android.intent.action.USER_STARTED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_STARTED", userHandle);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybeSendAdminEnabledBroadcastLocked(userHandle);
                        DevicePolicyManagerService.this.mUserData.remove(userHandle);
                    }
                    DevicePolicyManagerService.this.handlePackagesChanged(null, userHandle);
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_STOPPED", userHandle);
                } else if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_SWITCHED", userHandle);
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybeSendAdminEnabledBroadcastLocked(userHandle);
                    }
                } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(null, userHandle);
                } else if ("android.intent.action.PACKAGE_CHANGED".equals(action) || ("android.intent.action.PACKAGE_ADDED".equals(action) && intent.getBooleanExtra("android.intent.extra.REPLACING", false))) {
                    DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
                } else if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action)) {
                    DevicePolicyManagerService.this.clearWipeProfileNotification();
                } else if ("android.intent.action.DATE_CHANGED".equals(action) || "android.intent.action.TIME_SET".equals(action)) {
                    DevicePolicyManagerService.this.updateSystemUpdateFreezePeriodsRecord(true);
                }
            }

            private void sendDeviceOwnerUserCommand(String action, int userHandle) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    ActiveAdmin deviceOwner = DevicePolicyManagerService.this.getDeviceOwnerAdminLocked();
                    if (deviceOwner != null) {
                        Bundle extras = new Bundle();
                        extras.putParcelable("android.intent.extra.USER", UserHandle.of(userHandle));
                        DevicePolicyManagerService.this.sendAdminCommandLocked(deviceOwner, action, extras, null, true);
                    }
                }
            }
        };
        this.mInjector = injector;
        this.mContext = (Context) Preconditions.checkNotNull(injector.mContext);
        this.mHandler = new Handler((Looper) Preconditions.checkNotNull(injector.getMyLooper()));
        this.mConstants = DevicePolicyConstants.loadFromString(this.mInjector.settingsGlobalGetString("device_policy_constants"));
        this.mOwners = (Owners) Preconditions.checkNotNull(injector.newOwners());
        this.mUserManager = (UserManager) Preconditions.checkNotNull(injector.getUserManager());
        this.mUserManagerInternal = (UserManagerInternal) Preconditions.checkNotNull(injector.getUserManagerInternal());
        this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) Preconditions.checkNotNull(injector.getUsageStatsManagerInternal());
        this.mIPackageManager = (IPackageManager) Preconditions.checkNotNull(injector.getIPackageManager());
        this.mTelephonyManager = (TelephonyManager) Preconditions.checkNotNull(injector.getTelephonyManager());
        this.mLocalService = new LocalService();
        this.mLockPatternUtils = injector.newLockPatternUtils();
        this.mSecurityLogMonitor = new SecurityLogMonitor(this);
        this.mHasFeature = this.mInjector.hasFeature();
        this.mIsWatch = this.mInjector.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        this.mBackgroundHandler = BackgroundThread.getHandler();
        this.mCertificateMonitor = new CertificateMonitor(this, this.mInjector, this.mBackgroundHandler);
        this.mDeviceAdminServiceController = new DeviceAdminServiceController(this, this.mConstants);
        this.mOverlayPackagesProvider = new OverlayPackagesProvider(this.mContext);
        this.mTransferOwnershipMetadataManager = this.mInjector.newTransferOwnershipMetadataManager();
        if (this.mHasFeature) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.addAction(ACTION_EXPIRED_PASSWORD_NOTIFICATION);
            filter.addAction("android.intent.action.USER_ADDED");
            filter.addAction("android.intent.action.USER_REMOVED");
            filter.addAction("android.intent.action.USER_STARTED");
            filter.addAction("android.intent.action.USER_STOPPED");
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("android.intent.action.USER_UNLOCKED");
            filter.setPriority(1000);
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, this.mHandler);
            filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addDataScheme("package");
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, this.mHandler);
            filter = new IntentFilter();
            filter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.DATE_CHANGED");
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, this.mHandler);
            LocalServices.addService(DevicePolicyManagerInternal.class, this.mLocalService);
            this.mSetupContentObserver = new SetupContentObserver(this.mHandler);
            this.mUserManagerInternal.addUserRestrictionsListener(new RestrictionsListener(this.mContext));
            return;
        }
        this.mSetupContentObserver = null;
    }

    DevicePolicyData getUserData(int userHandle) {
        DevicePolicyData policy;
        synchronized (getLockObject()) {
            policy = (DevicePolicyData) this.mUserData.get(userHandle);
            if (policy == null) {
                policy = new DevicePolicyData(userHandle);
                this.mUserData.append(userHandle, policy);
                loadSettingsLocked(policy, userHandle);
            }
            init();
        }
        return policy;
    }

    PasswordMetrics getUserPasswordMetricsLocked(int userHandle) {
        return (PasswordMetrics) this.mUserPasswordMetrics.get(userHandle);
    }

    DevicePolicyData getUserDataUnchecked(int userHandle) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            DevicePolicyData userData = getUserData(userHandle);
            return userData;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    void removeUserData(int userHandle) {
        synchronized (getLockObject()) {
            if (userHandle == 0) {
                try {
                    Slog.w(LOG_TAG, "Tried to remove device policy file for user 0! Ignoring.");
                } catch (Throwable th) {
                }
            } else {
                this.mPolicyCache.onUserRemoved(userHandle);
                this.mOwners.removeProfileOwner(userHandle);
                this.mOwners.writeProfileOwner(userHandle);
                if (((DevicePolicyData) this.mUserData.get(userHandle)) != null) {
                    this.mUserData.remove(userHandle);
                }
                if (this.mUserPasswordMetrics.get(userHandle) != null) {
                    this.mUserPasswordMetrics.remove(userHandle);
                }
                File policyFile = new File(this.mInjector.environmentGetUserSystemDirectory(userHandle), DEVICE_POLICIES_XML);
                policyFile.delete();
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Removed device policy file ");
                stringBuilder.append(policyFile.getAbsolutePath());
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    void loadOwners() {
        synchronized (getLockObject()) {
            this.mOwners.load();
            setDeviceOwnerSystemPropertyLocked();
            findOwnerComponentIfNecessaryLocked();
            migrateUserRestrictionsIfNecessaryLocked();
            maybeSetDefaultDeviceOwnerUserRestrictionsLocked();
            updateDeviceOwnerLocked();
        }
    }

    private void maybeSetDefaultDeviceOwnerUserRestrictionsLocked() {
        ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
        if (deviceOwner != null) {
            maybeSetDefaultRestrictionsForAdminLocked(this.mOwners.getDeviceOwnerUserId(), deviceOwner, UserRestrictionsUtils.getDefaultEnabledForDeviceOwner());
        }
    }

    private void maybeSetDefaultProfileOwnerUserRestrictions() {
        synchronized (getLockObject()) {
            for (Integer userId : this.mOwners.getProfileOwnerKeys()) {
                int userId2 = userId.intValue();
                ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userId2);
                if (profileOwner != null) {
                    if (this.mUserManager.isManagedProfile(userId2)) {
                        maybeSetDefaultRestrictionsForAdminLocked(userId2, profileOwner, UserRestrictionsUtils.getDefaultEnabledForManagedProfiles());
                        ensureUnknownSourcesRestrictionForProfileOwnerLocked(userId2, profileOwner, false);
                    }
                }
            }
        }
    }

    private void ensureUnknownSourcesRestrictionForProfileOwnerLocked(int userId, ActiveAdmin profileOwner, boolean newOwner) {
        if (newOwner || this.mInjector.settingsSecureGetIntForUser("unknown_sources_default_reversed", 0, userId) != 0) {
            profileOwner.ensureUserRestrictions().putBoolean("no_install_unknown_sources", true);
            saveUserRestrictionsLocked(userId);
            this.mInjector.settingsSecurePutIntForUser("unknown_sources_default_reversed", 0, userId);
        }
    }

    private void maybeSetDefaultRestrictionsForAdminLocked(int userId, ActiveAdmin admin, Set<String> defaultRestrictions) {
        if (!defaultRestrictions.equals(admin.defaultEnabledRestrictionsAlreadySet)) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("New user restrictions need to be set by default for user ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
            Set<String> restrictionsToSet = new ArraySet(defaultRestrictions);
            restrictionsToSet.removeAll(admin.defaultEnabledRestrictionsAlreadySet);
            if (!restrictionsToSet.isEmpty()) {
                for (String restriction : restrictionsToSet) {
                    admin.ensureUserRestrictions().putBoolean(restriction, true);
                }
                admin.defaultEnabledRestrictionsAlreadySet.addAll(restrictionsToSet);
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Enabled the following restrictions by default: ");
                stringBuilder2.append(restrictionsToSet);
                Slog.i(str2, stringBuilder2.toString());
                saveUserRestrictionsLocked(userId);
            }
        }
    }

    private void setDeviceOwnerSystemPropertyLocked() {
        boolean z = false;
        if (this.mInjector.settingsGlobalGetInt("device_provisioned", 0) != 0) {
            z = true;
        }
        boolean deviceProvisioned = z;
        boolean hasDeviceOwner = this.mOwners.hasDeviceOwner();
        if ((hasDeviceOwner || deviceProvisioned) && !StorageManager.inCryptKeeperBounce()) {
            if (this.mInjector.systemPropertiesGet(PROPERTY_DEVICE_OWNER_PRESENT, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).isEmpty()) {
                String value = Boolean.toString(hasDeviceOwner);
                this.mInjector.systemPropertiesSet(PROPERTY_DEVICE_OWNER_PRESENT, value);
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Set ro.device_owner property to ");
                stringBuilder.append(value);
                Slog.i(str, stringBuilder.toString());
            } else {
                Slog.w(LOG_TAG, "Trying to set ro.device_owner, but it has already been set?");
            }
        }
    }

    private void maybeStartSecurityLogMonitorOnActivityManagerReady() {
        synchronized (getLockObject()) {
            if (this.mInjector.securityLogIsLoggingEnabled()) {
                this.mSecurityLogMonitor.start();
                this.mInjector.runCryptoSelfTest();
                maybePauseDeviceWideLoggingLocked();
            }
        }
    }

    private void findOwnerComponentIfNecessaryLocked() {
        if (this.mOwners.hasDeviceOwner()) {
            ComponentName doComponentName = this.mOwners.getDeviceOwnerComponent();
            if (TextUtils.isEmpty(doComponentName.getClassName())) {
                ComponentName doComponent = findAdminComponentWithPackageLocked(doComponentName.getPackageName(), this.mOwners.getDeviceOwnerUserId());
                if (doComponent == null) {
                    Slog.e(LOG_TAG, "Device-owner isn't registered as device-admin");
                } else {
                    this.mOwners.setDeviceOwnerWithRestrictionsMigrated(doComponent, this.mOwners.getDeviceOwnerName(), this.mOwners.getDeviceOwnerUserId(), this.mOwners.getDeviceOwnerUserRestrictionsNeedsMigration() ^ 1);
                    this.mOwners.writeDeviceOwner();
                }
            }
        }
    }

    private void migrateUserRestrictionsIfNecessaryLocked() {
        if (this.mOwners.getDeviceOwnerUserRestrictionsNeedsMigration()) {
            migrateUserRestrictionsForUser(UserHandle.SYSTEM, getDeviceOwnerAdminLocked(), null, true);
            pushUserRestrictions(0);
            this.mOwners.setDeviceOwnerUserRestrictionsMigrated();
        }
        Set<String> secondaryUserExceptionList = Sets.newArraySet(new String[]{"no_outgoing_calls", "no_sms"});
        for (UserInfo ui : this.mUserManager.getUsers()) {
            int userId = ui.id;
            if (this.mOwners.getProfileOwnerUserRestrictionsNeedsMigration(userId)) {
                migrateUserRestrictionsForUser(ui.getUserHandle(), getProfileOwnerAdminLocked(userId), userId == 0 ? null : secondaryUserExceptionList, false);
                pushUserRestrictions(userId);
                this.mOwners.setProfileOwnerUserRestrictionsMigrated(userId);
            }
        }
    }

    private void migrateUserRestrictionsForUser(UserHandle user, ActiveAdmin admin, Set<String> exceptionList, boolean isDeviceOwner) {
        Bundle origRestrictions = this.mUserManagerInternal.getBaseUserRestrictions(user.getIdentifier());
        Bundle newBaseRestrictions = new Bundle();
        Bundle newOwnerRestrictions = new Bundle();
        for (String key : origRestrictions.keySet()) {
            if (origRestrictions.getBoolean(key)) {
                boolean canOwnerChange;
                if (isDeviceOwner) {
                    canOwnerChange = UserRestrictionsUtils.canDeviceOwnerChange(key);
                } else {
                    canOwnerChange = UserRestrictionsUtils.canProfileOwnerChange(key, user.getIdentifier());
                }
                if (!canOwnerChange || (exceptionList != null && exceptionList.contains(key))) {
                    newBaseRestrictions.putBoolean(key, true);
                } else {
                    newOwnerRestrictions.putBoolean(key, true);
                }
            }
        }
        this.mUserManagerInternal.setBaseUserRestrictionsByDpmsForMigration(user.getIdentifier(), newBaseRestrictions);
        if (admin != null) {
            admin.ensureUserRestrictions().clear();
            admin.ensureUserRestrictions().putAll(newOwnerRestrictions);
        } else {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ActiveAdmin for DO/PO not found. user=");
            stringBuilder.append(user.getIdentifier());
            Slog.w(str, stringBuilder.toString());
        }
        saveSettingsLocked(user.getIdentifier());
    }

    private ComponentName findAdminComponentWithPackageLocked(String packageName, int userId) {
        DevicePolicyData policy = getUserData(userId);
        int n = policy.mAdminList.size();
        ComponentName found = null;
        int nFound = 0;
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
            if (packageName.equals(admin.info.getPackageName())) {
                if (nFound == 0) {
                    found = admin.info.getComponent();
                }
                nFound++;
            }
        }
        if (nFound > 1) {
            Slog.w(LOG_TAG, "Multiple DA found; assume the first one is DO.");
        }
        return found;
    }

    private void setExpirationAlarmCheckLocked(Context context, int userHandle, boolean parent) {
        long alarmTime;
        int affectedUserHandle;
        Throwable th;
        Context context2;
        long j;
        AlarmManager am;
        int i = userHandle;
        boolean z = parent;
        long expiration = getPasswordExpirationLocked(null, i, z);
        long now = System.currentTimeMillis();
        long timeToExpire = expiration - now;
        if (expiration == 0) {
            alarmTime = 0;
        } else if (timeToExpire <= 0) {
            alarmTime = MS_PER_DAY + now;
        } else {
            alarmTime = timeToExpire % MS_PER_DAY;
            if (alarmTime == 0) {
                alarmTime = MS_PER_DAY;
            }
            alarmTime += now;
        }
        long token = this.mInjector.binderClearCallingIdentity();
        if (z) {
            try {
                affectedUserHandle = getProfileParentId(i);
            } catch (Throwable th2) {
                th = th2;
                context2 = context;
                j = expiration;
            }
        } else {
            affectedUserHandle = i;
        }
        try {
            am = this.mInjector.getAlarmManager();
        } catch (Throwable th3) {
            th = th3;
            context2 = context;
            j = expiration;
            this.mInjector.binderRestoreCallingIdentity(token);
            throw th;
        }
        try {
            PendingIntent pi = PendingIntent.getBroadcastAsUser(context, REQUEST_EXPIRE_PASSWORD, new Intent(ACTION_EXPIRED_PASSWORD_NOTIFICATION), 1207959552, UserHandle.of(affectedUserHandle));
            am.cancel(pi);
            if (alarmTime != 0) {
                am.set(1, alarmTime, pi);
            }
            this.mInjector.binderRestoreCallingIdentity(token);
        } catch (Throwable th4) {
            th = th4;
            this.mInjector.binderRestoreCallingIdentity(token);
            throw th;
        }
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName who, int userHandle) {
        ensureLocked();
        ActiveAdmin admin = (ActiveAdmin) getUserData(userHandle).mAdminMap.get(who);
        if (admin != null && who.getPackageName().equals(admin.info.getActivityInfo().packageName) && who.getClassName().equals(admin.info.getActivityInfo().name)) {
            return admin;
        }
        return null;
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName who, int userHandle, boolean parent) {
        ensureLocked();
        if (parent) {
            enforceManagedProfile(userHandle, "call APIs on the parent profile");
        }
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        if (admin == null || !parent) {
            return admin;
        }
        return admin.getParentActiveAdmin();
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName who, int reqPolicy) throws SecurityException {
        ensureLocked();
        int callingUid = this.mInjector.binderGetCallingUid();
        ActiveAdmin result = getActiveAdminWithPolicyForUidLocked(who, reqPolicy, callingUid);
        if (result != null) {
            return result;
        }
        if (who != null) {
            ActiveAdmin admin = (ActiveAdmin) getUserData(UserHandle.getUserId(callingUid)).mAdminMap.get(who);
            StringBuilder stringBuilder;
            if (reqPolicy == -2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Admin ");
                stringBuilder.append(admin.info.getComponent());
                stringBuilder.append(" does not own the device");
                throw new SecurityException(stringBuilder.toString());
            } else if (reqPolicy == -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Admin ");
                stringBuilder.append(admin.info.getComponent());
                stringBuilder.append(" does not own the profile");
                throw new SecurityException(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Admin ");
                stringBuilder.append(admin.info.getComponent());
                stringBuilder.append(" did not specify uses-policy for: ");
                stringBuilder.append(admin.info.getTagForPolicy(reqPolicy));
                throw new SecurityException(stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("No active admin owned by uid ");
        stringBuilder2.append(this.mInjector.binderGetCallingUid());
        stringBuilder2.append(" for policy #");
        stringBuilder2.append(reqPolicy);
        throw new SecurityException(stringBuilder2.toString());
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName who, int reqPolicy, boolean parent) throws SecurityException {
        ensureLocked();
        if (parent) {
            enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "call APIs on the parent profile");
        }
        ActiveAdmin admin = getActiveAdminForCallerLocked(who, reqPolicy);
        return parent ? admin.getParentActiveAdmin() : admin;
    }

    private ActiveAdmin getActiveAdminForUidLocked(ComponentName who, int uid) {
        ensureLocked();
        ActiveAdmin admin = (ActiveAdmin) getUserData(UserHandle.getUserId(uid)).mAdminMap.get(who);
        StringBuilder stringBuilder;
        if (admin == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No active admin ");
            stringBuilder.append(who);
            throw new SecurityException(stringBuilder.toString());
        } else if (admin.getUid() == uid) {
            return admin;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Admin ");
            stringBuilder.append(who);
            stringBuilder.append(" is not owned by uid ");
            stringBuilder.append(uid);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private ActiveAdmin getActiveAdminWithPolicyForUidLocked(ComponentName who, int reqPolicy, int uid) {
        ensureLocked();
        int userId = UserHandle.getUserId(uid);
        DevicePolicyData policy = getUserData(userId);
        if (who != null) {
            ActiveAdmin admin = (ActiveAdmin) policy.mAdminMap.get(who);
            StringBuilder stringBuilder;
            if (admin == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("No active admin ");
                stringBuilder.append(who);
                throw new SecurityException(stringBuilder.toString());
            } else if (admin.getUid() != uid) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Admin ");
                stringBuilder.append(who);
                stringBuilder.append(" is not owned by uid ");
                stringBuilder.append(uid);
                throw new SecurityException(stringBuilder.toString());
            } else if (isActiveAdminWithPolicyForUserLocked(admin, reqPolicy, userId)) {
                return admin;
            }
        }
        Iterator it = policy.mAdminList.iterator();
        while (it.hasNext()) {
            ActiveAdmin admin2 = (ActiveAdmin) it.next();
            if (admin2.getUid() == uid && isActiveAdminWithPolicyForUserLocked(admin2, reqPolicy, userId)) {
                return admin2;
            }
        }
        return null;
    }

    @VisibleForTesting
    boolean isActiveAdminWithPolicyForUserLocked(ActiveAdmin admin, int reqPolicy, int userId) {
        ensureLocked();
        boolean ownsDevice = isDeviceOwner(admin.info.getComponent(), userId);
        boolean ownsProfile = isProfileOwner(admin.info.getComponent(), userId);
        if (reqPolicy == -2) {
            return ownsDevice;
        }
        if (reqPolicy != -1) {
            return admin.info.usesPolicy(reqPolicy);
        }
        boolean z = ownsDevice || ownsProfile;
        return z;
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action) {
        sendAdminCommandLocked(admin, action, null);
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action, BroadcastReceiver result) {
        sendAdminCommandLocked(admin, action, null, result);
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action, Bundle adminExtras, BroadcastReceiver result) {
        sendAdminCommandLocked(admin, action, adminExtras, result, false);
    }

    boolean sendAdminCommandLocked(ActiveAdmin admin, String action, Bundle adminExtras, BroadcastReceiver result, boolean inForeground) {
        Intent intent = new Intent(action);
        intent.setComponent(admin.info.getComponent());
        if (UserManager.isDeviceInDemoMode(this.mContext)) {
            intent.addFlags(268435456);
        }
        if (action.equals("android.app.action.ACTION_PASSWORD_EXPIRING")) {
            intent.putExtra("expiration", admin.passwordExpirationDate);
        }
        if (inForeground) {
            intent.addFlags(268435456);
        }
        if (adminExtras != null) {
            intent.putExtras(adminExtras);
        }
        if (this.mInjector.getPackageManager().queryBroadcastReceiversAsUser(intent, 268435456, admin.getUserHandle()).isEmpty()) {
            return false;
        }
        if (result != null) {
            this.mContext.sendOrderedBroadcastAsUser(intent, admin.getUserHandle(), null, result, this.mHandler, -1, null, null);
        } else {
            this.mContext.sendBroadcastAsUser(intent, admin.getUserHandle());
        }
        return true;
    }

    void sendAdminCommandLocked(String action, int reqPolicy, int userHandle, Bundle adminExtras) {
        DevicePolicyData policy = getUserData(userHandle);
        int count = policy.mAdminList.size();
        for (int i = 0; i < count; i++) {
            ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
            if (admin.info.usesPolicy(reqPolicy)) {
                sendAdminCommandLocked(admin, action, adminExtras, null);
            }
        }
    }

    private void sendAdminCommandToSelfAndProfilesLocked(String action, int reqPolicy, int userHandle, Bundle adminExtras) {
        for (int profileId : this.mUserManager.getProfileIdsWithDisabled(userHandle)) {
            sendAdminCommandLocked(action, reqPolicy, profileId, adminExtras);
        }
    }

    private void sendAdminCommandForLockscreenPoliciesLocked(String action, int reqPolicy, int userHandle) {
        Bundle extras = new Bundle();
        extras.putParcelable("android.intent.extra.USER", UserHandle.of(userHandle));
        if (isSeparateProfileChallengeEnabled(userHandle)) {
            sendAdminCommandLocked(action, reqPolicy, userHandle, extras);
        } else {
            sendAdminCommandToSelfAndProfilesLocked(action, reqPolicy, userHandle, extras);
        }
    }

    void removeActiveAdminLocked(final ComponentName adminReceiver, final int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
        DevicePolicyData policy = getUserData(userHandle);
        if (admin != null && !policy.mRemovingAdmins.contains(adminReceiver)) {
            policy.mRemovingAdmins.add(adminReceiver);
            sendAdminCommandLocked(admin, "android.app.action.DEVICE_ADMIN_DISABLED", new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    DevicePolicyManagerService.this.removeAdminArtifacts(adminReceiver, userHandle);
                    DevicePolicyManagerService.this.removePackageIfRequired(adminReceiver.getPackageName(), userHandle);
                }
            });
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeActiveAdminLocked(");
            stringBuilder.append(adminReceiver);
            stringBuilder.append("), by user ");
            stringBuilder.append(userHandle);
            Flog.i(305, stringBuilder.toString());
        }
    }

    public DeviceAdminInfo findAdmin(ComponentName adminName, int userHandle, boolean throwForMissingPermission) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(userHandle);
        ActivityInfo ai = null;
        try {
            ai = this.mIPackageManager.getReceiverInfo(adminName, 819328, userHandle);
        } catch (RemoteException e) {
        }
        if (ai != null) {
            if (!"android.permission.BIND_DEVICE_ADMIN".equals(ai.permission)) {
                String message = new StringBuilder();
                message.append("DeviceAdminReceiver ");
                message.append(adminName);
                message.append(" must be protected with ");
                message.append("android.permission.BIND_DEVICE_ADMIN");
                message = message.toString();
                Slog.w(LOG_TAG, message);
                if (throwForMissingPermission && ai.applicationInfo.targetSdkVersion > 23) {
                    throw new IllegalArgumentException(message);
                }
            }
            try {
                return new DeviceAdminInfo(this.mContext, ai);
            } catch (IOException | XmlPullParserException e2) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Bad device admin requested for user=");
                stringBuilder.append(userHandle);
                stringBuilder.append(": ");
                stringBuilder.append(adminName);
                Slog.w(str, stringBuilder.toString(), e2);
                return null;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unknown admin: ");
        stringBuilder2.append(adminName);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private File getPolicyFileDirectory(int userId) {
        if (userId == 0) {
            return new File(this.mInjector.getDevicePolicyFilePathForSystemUser());
        }
        return this.mInjector.environmentGetUserSystemDirectory(userId);
    }

    private JournaledFile makeJournaledFile(int userId) {
        String base = new File(getPolicyFileDirectory(userId), DEVICE_POLICIES_XML).getAbsolutePath();
        HwCustDevicePolicyManagerService mHwCustDPMS = (HwCustDevicePolicyManagerService) HwCustUtils.createObj(HwCustDevicePolicyManagerService.class, new Object[0]);
        if (mHwCustDPMS != null && mHwCustDPMS.shouldActiveDeviceAdmins(base)) {
            mHwCustDPMS.activeDeviceAdmins(base);
        }
        File file = new File(base);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(base);
        stringBuilder.append(".tmp");
        return new JournaledFile(file, new File(stringBuilder.toString()));
    }

    void saveSettingsLocked(int userHandle) {
        DevicePolicyData policy = getUserData(userHandle);
        JournaledFile journal = makeJournaledFile(userHandle);
        FileOutputStream stream = null;
        try {
            int i;
            String delegatePackage;
            int i2;
            int i3 = 0;
            stream = new FileOutputStream(journal.chooseForWrite(), false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, "policies");
            if (policy.mRestrictionsProvider != null) {
                out.attribute(null, ATTR_PERMISSION_PROVIDER, policy.mRestrictionsProvider.flattenToString());
            }
            if (policy.mUserSetupComplete) {
                out.attribute(null, ATTR_SETUP_COMPLETE, Boolean.toString(true));
            }
            if (policy.mPaired) {
                out.attribute(null, ATTR_DEVICE_PAIRED, Boolean.toString(true));
            }
            if (policy.mDeviceProvisioningConfigApplied) {
                out.attribute(null, ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED, Boolean.toString(true));
            }
            if (policy.mUserProvisioningState != 0) {
                out.attribute(null, ATTR_PROVISIONING_STATE, Integer.toString(policy.mUserProvisioningState));
            }
            if (policy.mPermissionPolicy != 0) {
                out.attribute(null, ATTR_PERMISSION_POLICY, Integer.toString(policy.mPermissionPolicy));
            }
            for (i = 0; i < policy.mDelegationMap.size(); i++) {
                delegatePackage = (String) policy.mDelegationMap.keyAt(i);
                for (String scope : (List) policy.mDelegationMap.valueAt(i)) {
                    out.startTag(null, "delegation");
                    out.attribute(null, "delegatePackage", delegatePackage);
                    out.attribute(null, "scope", scope);
                    out.endTag(null, "delegation");
                }
            }
            i = policy.mAdminList.size();
            for (i2 = 0; i2 < i; i2++) {
                ActiveAdmin ap = (ActiveAdmin) policy.mAdminList.get(i2);
                if (ap != null) {
                    out.startTag(null, "admin");
                    out.attribute(null, "name", ap.info.getComponent().flattenToString());
                    ap.writeToXml(out);
                    out.endTag(null, "admin");
                }
            }
            if (policy.mPasswordOwner >= 0) {
                out.startTag(null, "password-owner");
                out.attribute(null, ATTR_VALUE, Integer.toString(policy.mPasswordOwner));
                out.endTag(null, "password-owner");
            }
            if (policy.mFailedPasswordAttempts != 0) {
                out.startTag(null, "failed-password-attempts");
                out.attribute(null, ATTR_VALUE, Integer.toString(policy.mFailedPasswordAttempts));
                out.endTag(null, "failed-password-attempts");
            }
            if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
                out.startTag(null, TAG_PASSWORD_VALIDITY);
                out.attribute(null, ATTR_VALUE, Boolean.toString(policy.mPasswordValidAtLastCheckpoint));
                out.endTag(null, TAG_PASSWORD_VALIDITY);
            }
            if (mHwCustDevicePolicyManagerService != null && mHwCustDevicePolicyManagerService.isForbiddenSimplePwdFeatureEnable()) {
                out.startTag(null, "is-currentpwd-simple");
                out.attribute(null, ATTR_VALUE, Boolean.toString(policy.mIsCurrentPwdSimple));
                out.endTag(null, "is-currentpwd-simple");
            }
            for (i2 = 0; i2 < policy.mAcceptedCaCertificates.size(); i2++) {
                out.startTag(null, TAG_ACCEPTED_CA_CERTIFICATES);
                out.attribute(null, "name", (String) policy.mAcceptedCaCertificates.valueAt(i2));
                out.endTag(null, TAG_ACCEPTED_CA_CERTIFICATES);
            }
            while (i3 < policy.mLockTaskPackages.size()) {
                delegatePackage = (String) policy.mLockTaskPackages.get(i3);
                out.startTag(null, TAG_LOCK_TASK_COMPONENTS);
                out.attribute(null, "name", delegatePackage);
                out.endTag(null, TAG_LOCK_TASK_COMPONENTS);
                i3++;
            }
            if (policy.mLockTaskFeatures != 0) {
                out.startTag(null, TAG_LOCK_TASK_FEATURES);
                out.attribute(null, ATTR_VALUE, Integer.toString(policy.mLockTaskFeatures));
                out.endTag(null, TAG_LOCK_TASK_FEATURES);
            }
            if (policy.mStatusBarDisabled) {
                out.startTag(null, TAG_STATUS_BAR);
                out.attribute(null, ATTR_DISABLED, Boolean.toString(policy.mStatusBarDisabled));
                out.endTag(null, TAG_STATUS_BAR);
            }
            if (policy.doNotAskCredentialsOnBoot) {
                out.startTag(null, DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML);
                out.endTag(null, DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML);
            }
            for (String delegatePackage2 : policy.mAffiliationIds) {
                out.startTag(null, TAG_AFFILIATION_ID);
                out.attribute(null, ATTR_ID, delegatePackage2);
                out.endTag(null, TAG_AFFILIATION_ID);
            }
            if (policy.mLastSecurityLogRetrievalTime >= 0) {
                out.startTag(null, TAG_LAST_SECURITY_LOG_RETRIEVAL);
                out.attribute(null, ATTR_VALUE, Long.toString(policy.mLastSecurityLogRetrievalTime));
                out.endTag(null, TAG_LAST_SECURITY_LOG_RETRIEVAL);
            }
            if (policy.mLastBugReportRequestTime >= 0) {
                out.startTag(null, TAG_LAST_BUG_REPORT_REQUEST);
                out.attribute(null, ATTR_VALUE, Long.toString(policy.mLastBugReportRequestTime));
                out.endTag(null, TAG_LAST_BUG_REPORT_REQUEST);
            }
            if (policy.mLastNetworkLogsRetrievalTime >= 0) {
                out.startTag(null, TAG_LAST_NETWORK_LOG_RETRIEVAL);
                out.attribute(null, ATTR_VALUE, Long.toString(policy.mLastNetworkLogsRetrievalTime));
                out.endTag(null, TAG_LAST_NETWORK_LOG_RETRIEVAL);
            }
            if (policy.mAdminBroadcastPending) {
                out.startTag(null, TAG_ADMIN_BROADCAST_PENDING);
                out.attribute(null, ATTR_VALUE, Boolean.toString(policy.mAdminBroadcastPending));
                out.endTag(null, TAG_ADMIN_BROADCAST_PENDING);
            }
            if (policy.mInitBundle != null) {
                out.startTag(null, TAG_INITIALIZATION_BUNDLE);
                policy.mInitBundle.saveToXml(out);
                out.endTag(null, TAG_INITIALIZATION_BUNDLE);
            }
            if (policy.mPasswordTokenHandle != 0) {
                out.startTag(null, TAG_PASSWORD_TOKEN_HANDLE);
                out.attribute(null, ATTR_VALUE, Long.toString(policy.mPasswordTokenHandle));
                out.endTag(null, TAG_PASSWORD_TOKEN_HANDLE);
            }
            if (policy.mCurrentInputMethodSet) {
                out.startTag(null, TAG_CURRENT_INPUT_METHOD_SET);
                out.endTag(null, TAG_CURRENT_INPUT_METHOD_SET);
            }
            for (String delegatePackage22 : policy.mOwnerInstalledCaCerts) {
                out.startTag(null, TAG_OWNER_INSTALLED_CA_CERT);
                out.attribute(null, ATTR_ALIAS, delegatePackage22);
                out.endTag(null, TAG_OWNER_INSTALLED_CA_CERT);
            }
            out.endTag(null, "policies");
            out.endDocument();
            stream.flush();
            FileUtils.sync(stream);
            stream.close();
            journal.commit();
            sendChangedNotification(userHandle);
        } catch (IOException | XmlPullParserException e) {
            Slog.w(LOG_TAG, "failed writing file", e);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e2) {
                }
            }
            journal.rollback();
        }
    }

    private void sendChangedNotification(int userHandle) {
        Intent intent = new Intent("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        intent.setFlags(1073741824);
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(userHandle));
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:213:0x0457 A:{SYNTHETIC, Splitter:B:213:0x0457} */
    /* JADX WARNING: Removed duplicated region for block: B:218:0x046b  */
    /* JADX WARNING: Removed duplicated region for block: B:230:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:221:0x0487  */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Removed duplicated region for block: B:199:0x0423 A:{PHI: r8 r18 , ExcHandler: IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException (e java.lang.Throwable), Splitter:B:63:0x00e1} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:104:0x01b0, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:107:0x01b7, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:108:0x01b8, code skipped:
            r21 = r5;
     */
    /* JADX WARNING: Missing block: B:109:0x01bb, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:110:0x01bc, code skipped:
            r20 = r4;
            r21 = r5;
     */
    /* JADX WARNING: Missing block: B:199:0x0423, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:200:0x0424, code skipped:
            r5 = r18;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadSettingsLocked(DevicePolicyData policy, int userHandle) {
        JournaledFile journaledFile;
        Exception e;
        String str;
        StringBuilder stringBuilder;
        FileInputStream stream;
        DevicePolicyData devicePolicyData = policy;
        int i = userHandle;
        JournaledFile journal = makeJournaledFile(i);
        FileInputStream stream2 = null;
        File file = journal.chooseForRead();
        boolean needsRewrite = false;
        try {
            stream2 = new FileInputStream(file);
            try {
                int type;
                String tag;
                String deviceProvisioningConfigApplied;
                int outerDepth;
                int type2;
                int outerDepth2;
                String deviceProvisioningConfigApplied2;
                String name;
                StringBuilder stringBuilder2;
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream2, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 1 || type == 2) {
                        tag = parser.getName();
                    }
                }
                tag = parser.getName();
                if ("policies".equals(tag)) {
                    String permissionProvider = parser.getAttributeValue(null, ATTR_PERMISSION_PROVIDER);
                    if (permissionProvider != null) {
                        try {
                            devicePolicyData.mRestrictionsProvider = ComponentName.unflattenFromString(permissionProvider);
                        } catch (FileNotFoundException e2) {
                            journaledFile = journal;
                        } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e3) {
                            e = e3;
                            journaledFile = journal;
                            str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("failed parsing ");
                            stringBuilder.append(file);
                            Slog.w(str, stringBuilder.toString(), e);
                        }
                    }
                    String userSetupComplete = parser.getAttributeValue(null, ATTR_SETUP_COMPLETE);
                    if (userSetupComplete != null) {
                        if (Boolean.toString(true).equals(userSetupComplete)) {
                            devicePolicyData.mUserSetupComplete = true;
                        }
                    }
                    String paired = parser.getAttributeValue(null, ATTR_DEVICE_PAIRED);
                    if (paired != null) {
                        if (Boolean.toString(true).equals(paired)) {
                            devicePolicyData.mPaired = true;
                        }
                    }
                    deviceProvisioningConfigApplied = parser.getAttributeValue(null, ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED);
                    if (deviceProvisioningConfigApplied != null) {
                        if (Boolean.toString(true).equals(deviceProvisioningConfigApplied)) {
                            devicePolicyData.mDeviceProvisioningConfigApplied = true;
                        }
                    }
                    String provisioningState = parser.getAttributeValue(null, ATTR_PROVISIONING_STATE);
                    if (!TextUtils.isEmpty(provisioningState)) {
                        devicePolicyData.mUserProvisioningState = Integer.parseInt(provisioningState);
                    }
                    try {
                        List<String> scopes;
                        str = parser.getAttributeValue(null, ATTR_PERMISSION_POLICY);
                        if (!TextUtils.isEmpty(str)) {
                            try {
                                devicePolicyData.mPermissionPolicy = Integer.parseInt(str);
                            } catch (FileNotFoundException e4) {
                            } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e5) {
                                e = e5;
                                str = LOG_TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("failed parsing ");
                                stringBuilder.append(file);
                                Slog.w(str, stringBuilder.toString(), e);
                            }
                        }
                        String permissionPolicy = str;
                        str = parser.getAttributeValue(null, ATTR_DELEGATED_CERT_INSTALLER);
                        if (str != null) {
                            scopes = (List) devicePolicyData.mDelegationMap.get(str);
                            if (scopes == null) {
                                stream = stream2;
                                try {
                                    scopes = new ArrayList();
                                    devicePolicyData.mDelegationMap.put(str, scopes);
                                } catch (FileNotFoundException e6) {
                                    stream2 = stream;
                                } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e7) {
                                }
                            } else {
                                stream = stream2;
                            }
                            if (!scopes.contains("delegation-cert-install")) {
                                scopes.add("delegation-cert-install");
                                needsRewrite = true;
                            }
                        } else {
                            stream = stream2;
                        }
                        String appRestrictionsDelegate = parser.getAttributeValue(null, ATTR_APPLICATION_RESTRICTIONS_MANAGER);
                        if (appRestrictionsDelegate != null) {
                            scopes = (List) devicePolicyData.mDelegationMap.get(appRestrictionsDelegate);
                            String certDelegate;
                            if (scopes == null) {
                                certDelegate = str;
                                scopes = new ArrayList();
                                devicePolicyData.mDelegationMap.put(appRestrictionsDelegate, scopes);
                            } else {
                                certDelegate = str;
                            }
                            if (scopes.contains("delegation-app-restrictions") == null) {
                                scopes.add("delegation-app-restrictions");
                                needsRewrite = true;
                            }
                        }
                        journal = parser.next();
                        outerDepth = parser.getDepth();
                        devicePolicyData.mLockTaskPackages.clear();
                        devicePolicyData.mAdminList.clear();
                        devicePolicyData.mAdminMap.clear();
                        devicePolicyData.mAffiliationIds.clear();
                        devicePolicyData.mOwnerInstalledCaCerts.clear();
                        while (true) {
                            type = parser.next();
                            journal = type;
                            if (type == 1 || (journal == 3 && parser.getDepth() <= outerDepth)) {
                                stream2 = stream;
                            } else {
                                if (journal == 3) {
                                    type2 = journal;
                                    outerDepth2 = outerDepth;
                                    deviceProvisioningConfigApplied2 = deviceProvisioningConfigApplied;
                                } else if (journal == 4) {
                                    type2 = journal;
                                    outerDepth2 = outerDepth;
                                    deviceProvisioningConfigApplied2 = deviceProvisioningConfigApplied;
                                } else {
                                    type = parser.getName();
                                    if ("admin".equals(type)) {
                                        name = parser.getAttributeValue(null, "name");
                                        type2 = journal;
                                        DeviceAdminInfo dai = findAdmin(ComponentName.unflattenFromString(name), i, null);
                                        if (dai != null) {
                                            outerDepth2 = outerDepth;
                                            journal = new ActiveAdmin(dai, 0);
                                            journal.readFromXml(parser);
                                            devicePolicyData.mAdminMap.put(journal.info.getComponent(), journal);
                                        } else {
                                            outerDepth2 = outerDepth;
                                        }
                                        deviceProvisioningConfigApplied2 = deviceProvisioningConfigApplied;
                                    } else {
                                        type2 = journal;
                                        outerDepth2 = outerDepth;
                                        deviceProvisioningConfigApplied2 = deviceProvisioningConfigApplied;
                                        if ("delegation".equals(type)) {
                                            tag = parser.getAttributeValue(null, "delegatePackage");
                                            str = parser.getAttributeValue(null, "scope");
                                            List<String> scopes2 = (List) devicePolicyData.mDelegationMap.get(tag);
                                            if (scopes2 == null) {
                                                scopes2 = new ArrayList();
                                                devicePolicyData.mDelegationMap.put(tag, scopes2);
                                            }
                                            if (!scopes2.contains(str)) {
                                                scopes2.add(str);
                                            }
                                        } else if ("failed-password-attempts".equals(type)) {
                                            devicePolicyData.mFailedPasswordAttempts = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                                        } else if ("password-owner".equals(type)) {
                                            devicePolicyData.mPasswordOwner = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                                        } else if (TAG_ACCEPTED_CA_CERTIFICATES.equals(type)) {
                                            devicePolicyData.mAcceptedCaCertificates.add(parser.getAttributeValue(null, "name"));
                                        } else if (TAG_LOCK_TASK_COMPONENTS.equals(type)) {
                                            devicePolicyData.mLockTaskPackages.add(parser.getAttributeValue(null, "name"));
                                        } else if (TAG_LOCK_TASK_FEATURES.equals(type)) {
                                            devicePolicyData.mLockTaskFeatures = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                                        } else if (TAG_STATUS_BAR.equals(type)) {
                                            devicePolicyData.mStatusBarDisabled = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_DISABLED));
                                        } else if (DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML.equals(type)) {
                                            devicePolicyData.doNotAskCredentialsOnBoot = true;
                                        } else if (TAG_AFFILIATION_ID.equals(type)) {
                                            devicePolicyData.mAffiliationIds.add(parser.getAttributeValue(null, ATTR_ID));
                                        } else if (TAG_LAST_SECURITY_LOG_RETRIEVAL.equals(type)) {
                                            devicePolicyData.mLastSecurityLogRetrievalTime = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                                        } else if (TAG_LAST_BUG_REPORT_REQUEST.equals(type)) {
                                            devicePolicyData.mLastBugReportRequestTime = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                                        } else if (TAG_LAST_NETWORK_LOG_RETRIEVAL.equals(type)) {
                                            devicePolicyData.mLastNetworkLogsRetrievalTime = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                                        } else if (TAG_ADMIN_BROADCAST_PENDING.equals(type)) {
                                            devicePolicyData.mAdminBroadcastPending = Boolean.toString(true).equals(parser.getAttributeValue(null, ATTR_VALUE));
                                        } else if (TAG_INITIALIZATION_BUNDLE.equals(type)) {
                                            devicePolicyData.mInitBundle = PersistableBundle.restoreFromXml(parser);
                                        } else {
                                            if ("active-password".equals(type)) {
                                                needsRewrite = true;
                                            } else if (TAG_PASSWORD_VALIDITY.equals(type)) {
                                                if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
                                                    devicePolicyData.mPasswordValidAtLastCheckpoint = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                                                }
                                            } else if (TAG_PASSWORD_TOKEN_HANDLE.equals(type)) {
                                                devicePolicyData.mPasswordTokenHandle = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                                            } else if (TAG_CURRENT_INPUT_METHOD_SET.equals(type)) {
                                                devicePolicyData.mCurrentInputMethodSet = true;
                                            } else if (TAG_OWNER_INSTALLED_CA_CERT.equals(type)) {
                                                devicePolicyData.mOwnerInstalledCaCerts.add(parser.getAttributeValue(null, ATTR_ALIAS));
                                            } else if (mHwCustDevicePolicyManagerService != null && mHwCustDevicePolicyManagerService.isForbiddenSimplePwdFeatureEnable() && "is-currentpwd-simple".equals(type)) {
                                                devicePolicyData.mIsCurrentPwdSimple = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                                            } else {
                                                tag = LOG_TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Unknown tag: ");
                                                stringBuilder.append(type);
                                                Slog.w(tag, stringBuilder.toString());
                                                XmlUtils.skipCurrentTag(parser);
                                            }
                                            tag = type;
                                        }
                                    }
                                    tag = type;
                                }
                                journal = type2;
                                outerDepth = outerDepth2;
                                deviceProvisioningConfigApplied = deviceProvisioningConfigApplied2;
                            }
                        }
                        stream2 = stream;
                    } catch (FileNotFoundException e8) {
                        stream = stream2;
                    } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e9) {
                        e = e9;
                        stream = stream2;
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("failed parsing ");
                        stringBuilder.append(file);
                        Slog.w(str, stringBuilder.toString(), e);
                    }
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e10) {
                        }
                    }
                    devicePolicyData.mAdminList.addAll(devicePolicyData.mAdminMap.values());
                    if (needsRewrite) {
                        saveSettingsLocked(i);
                    }
                    validatePasswordOwnerLocked(policy);
                    updateMaximumTimeToLockLocked(i);
                    syncHwDeviceSettingsLocked(devicePolicyData.mUserHandle);
                    updateLockTaskPackagesLocked(devicePolicyData.mLockTaskPackages, i);
                    updateLockTaskFeaturesLocked(devicePolicyData.mLockTaskFeatures, i);
                    if (!devicePolicyData.mStatusBarDisabled) {
                        setStatusBarDisabledInternal(devicePolicyData.mStatusBarDisabled, i);
                        return;
                    }
                    return;
                }
                stream = stream2;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Settings do not start with policies tag: found ");
                stringBuilder2.append(tag);
                throw new XmlPullParserException(stringBuilder2.toString());
                journal = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                deviceProvisioningConfigApplied2 = deviceProvisioningConfigApplied;
                stringBuilder2.append("Failed loading admin ");
                stringBuilder2.append(name);
                Slog.w(journal, stringBuilder2.toString(), e);
                tag = type;
                journal = type2;
                outerDepth = outerDepth2;
                deviceProvisioningConfigApplied = deviceProvisioningConfigApplied2;
            } catch (FileNotFoundException e11) {
                journaledFile = journal;
                stream = stream2;
            } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e12) {
                e = e12;
                journaledFile = journal;
                stream = stream2;
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed parsing ");
                stringBuilder.append(file);
                Slog.w(str, stringBuilder.toString(), e);
            }
        } catch (FileNotFoundException e13) {
            journaledFile = journal;
        } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e14) {
            e = e14;
            journaledFile = journal;
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            Slog.w(str, stringBuilder.toString(), e);
        }
    }

    private void updateLockTaskPackagesLocked(List<String> packages, int userId) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityManager().updateLockTaskPackages(userId, (String[]) packages.toArray(new String[packages.size()]));
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
        this.mInjector.binderRestoreCallingIdentity(ident);
    }

    private void updateLockTaskFeaturesLocked(int flags, int userId) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityManager().updateLockTaskFeatures(userId, flags);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
        this.mInjector.binderRestoreCallingIdentity(ident);
    }

    private void updateDeviceOwnerLocked() {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
            if (deviceOwnerComponent != null) {
                this.mInjector.getIActivityManager().updateDeviceOwner(deviceOwnerComponent.getPackageName());
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
        this.mInjector.binderRestoreCallingIdentity(ident);
    }

    static void validateQualityConstant(int quality) {
        if (quality != 0 && quality != 32768 && quality != 65536 && quality != 131072 && quality != 196608 && quality != 262144 && quality != 327680 && quality != 393216 && quality != DumpState.DUMP_FROZEN) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid quality constant: 0x");
            stringBuilder.append(Integer.toHexString(quality));
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    void validatePasswordOwnerLocked(DevicePolicyData policy) {
        if (policy.mPasswordOwner >= 0) {
            boolean haveOwner = false;
            for (int i = policy.mAdminList.size() - 1; i >= 0; i--) {
                if (((ActiveAdmin) policy.mAdminList.get(i)).getUid() == policy.mPasswordOwner) {
                    haveOwner = true;
                    break;
                }
            }
            if (!haveOwner) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Previous password owner ");
                stringBuilder.append(policy.mPasswordOwner);
                stringBuilder.append(" no longer active; disabling");
                Slog.w(str, stringBuilder.toString());
                policy.mPasswordOwner = -1;
            }
        }
    }

    @VisibleForTesting
    void systemReady(int phase) {
        if (this.mHasFeature) {
            if (phase == 480) {
                onLockSettingsReady();
                loadAdminDataAsync();
                this.mOwners.systemReady();
            } else if (phase == 550) {
                maybeStartSecurityLogMonitorOnActivityManagerReady();
            } else if (phase == 1000) {
                ensureDeviceOwnerUserStarted();
            }
        }
    }

    private void onLockSettingsReady() {
        List<String> packageList;
        getUserData(0);
        loadOwners();
        cleanUpOldUsers();
        maybeSetDefaultProfileOwnerUserRestrictions();
        handleStartUser(0);
        maybeLogStart();
        this.mSetupContentObserver.register();
        updateUserSetupCompleteAndPaired();
        synchronized (getLockObject()) {
            packageList = getKeepUninstalledPackagesLocked();
        }
        if (packageList != null) {
            this.mInjector.getPackageManagerInternal().setKeepUninstalledPackages(packageList);
        }
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            if (deviceOwner != null) {
                this.mUserManagerInternal.setForceEphemeralUsers(deviceOwner.forceEphemeralUsers);
                ActivityManagerInternal activityManagerInternal = this.mInjector.getActivityManagerInternal();
                activityManagerInternal.setSwitchingFromSystemUserMessage(deviceOwner.startUserSessionMessage);
                activityManagerInternal.setSwitchingToSystemUserMessage(deviceOwner.endUserSessionMessage);
            }
            revertTransferOwnershipIfNecessaryLocked();
        }
    }

    private void revertTransferOwnershipIfNecessaryLocked() {
        if (this.mTransferOwnershipMetadataManager.metadataFileExists()) {
            Slog.e(LOG_TAG, "Owner transfer metadata file exists! Reverting transfer.");
            Metadata metadata = this.mTransferOwnershipMetadataManager.loadMetadataFile();
            if (metadata.adminType.equals(LOG_TAG_PROFILE_OWNER)) {
                transferProfileOwnershipLocked(metadata.targetComponent, metadata.sourceComponent, metadata.userId);
                deleteTransferOwnershipMetadataFileLocked();
                deleteTransferOwnershipBundleLocked(metadata.userId);
            } else if (metadata.adminType.equals(LOG_TAG_DEVICE_OWNER)) {
                transferDeviceOwnershipLocked(metadata.targetComponent, metadata.sourceComponent, metadata.userId);
                deleteTransferOwnershipMetadataFileLocked();
                deleteTransferOwnershipBundleLocked(metadata.userId);
            }
            updateSystemUpdateFreezePeriodsRecord(true);
        }
    }

    private void maybeLogStart() {
        if (SecurityLog.isLoggingEnabled()) {
            String verifiedBootState = this.mInjector.systemPropertiesGet("ro.boot.verifiedbootstate");
            String verityMode = this.mInjector.systemPropertiesGet("ro.boot.veritymode");
            SecurityLog.writeEvent(210009, new Object[]{verifiedBootState, verityMode});
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0016, code skipped:
            if (r1 == 0) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:11:?, code skipped:
            r4.mInjector.getIActivityManager().startUserInBackground(r1);
     */
    /* JADX WARNING: Missing block: B:12:0x0022, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:0x0023, code skipped:
            android.util.Slog.w(LOG_TAG, "Exception starting user", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void ensureDeviceOwnerUserStarted() {
        synchronized (getLockObject()) {
            if (this.mOwners.hasDeviceOwner()) {
                int userId = this.mOwners.getDeviceOwnerUserId();
            }
        }
    }

    void handleStartUser(int userId) {
        updateScreenCaptureDisabled(userId, getScreenCaptureDisabled(null, userId));
        pushUserRestrictions(userId);
        startOwnerService(userId, "start-user");
    }

    void handleUnlockUser(int userId) {
        startOwnerService(userId, "unlock-user");
    }

    void handleStopUser(int userId) {
        stopOwnerService(userId, "stop-user");
    }

    private void startOwnerService(int userId, String actionForLog) {
        ComponentName owner = getOwnerComponent(userId);
        if (owner != null) {
            this.mDeviceAdminServiceController.startServiceForOwner(owner.getPackageName(), userId, actionForLog);
        }
    }

    private void stopOwnerService(int userId, String actionForLog) {
        this.mDeviceAdminServiceController.stopServiceForOwner(userId, actionForLog);
    }

    private void cleanUpOldUsers() {
        Set<Integer> usersWithProfileOwners;
        Set<Integer> usersWithData;
        synchronized (getLockObject()) {
            usersWithProfileOwners = this.mOwners.getProfileOwnerKeys();
            usersWithData = new ArraySet();
            for (int i = 0; i < this.mUserData.size(); i++) {
                usersWithData.add(Integer.valueOf(this.mUserData.keyAt(i)));
            }
        }
        Set<Integer> usersWithProfileOwners2 = usersWithProfileOwners;
        usersWithProfileOwners = usersWithData;
        List<UserInfo> allUsers = this.mUserManager.getUsers();
        Set<Integer> deletedUsers = new ArraySet();
        deletedUsers.addAll(usersWithProfileOwners2);
        deletedUsers.addAll(usersWithProfileOwners);
        for (UserInfo userInfo : allUsers) {
            deletedUsers.remove(Integer.valueOf(userInfo.id));
        }
        for (Integer userId : deletedUsers) {
            removeUserData(userId.intValue());
        }
    }

    private void handlePasswordExpirationNotification(int userHandle) {
        int i = userHandle;
        Bundle adminExtras = new Bundle();
        adminExtras.putParcelable("android.intent.extra.USER", UserHandle.of(userHandle));
        synchronized (getLockObject()) {
            long now = System.currentTimeMillis();
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(i, false);
            int N = admins.size();
            for (int i2 = 0; i2 < N; i2++) {
                ActiveAdmin admin = (ActiveAdmin) admins.get(i2);
                if (admin.info.usesPolicy(6) && admin.passwordExpirationTimeout > 0 && now >= admin.passwordExpirationDate - getUsrSetExtendTime() && admin.passwordExpirationDate > 0) {
                    sendAdminCommandLocked(admin, "android.app.action.ACTION_PASSWORD_EXPIRING", adminExtras, null);
                }
            }
            setExpirationAlarmCheckLocked(this.mContext, i, false);
        }
    }

    protected long getUsrSetExtendTime() {
        return EXPIRATION_GRACE_PERIOD_MS;
    }

    protected void onInstalledCertificatesChanged(UserHandle userHandle, Collection<String> installedCertificates) {
        if (this.mHasFeature) {
            enforceManageUsers();
            synchronized (getLockObject()) {
                DevicePolicyData policy = getUserData(userHandle.getIdentifier());
                if ((false | policy.mAcceptedCaCertificates.retainAll(installedCertificates)) | policy.mOwnerInstalledCaCerts.retainAll(installedCertificates)) {
                    saveSettingsLocked(userHandle.getIdentifier());
                }
            }
        }
    }

    protected Set<String> getAcceptedCaCertificates(UserHandle userHandle) {
        if (!this.mHasFeature) {
            return Collections.emptySet();
        }
        ArraySet arraySet;
        synchronized (getLockObject()) {
            arraySet = getUserData(userHandle.getIdentifier()).mAcceptedCaCertificates;
        }
        return arraySet;
    }

    public void setActiveAdmin(ComponentName adminReceiver, boolean refreshing, int userHandle) {
        if (this.mHasFeature) {
            setActiveAdmin(adminReceiver, refreshing, userHandle, null);
        }
    }

    private void setActiveAdmin(ComponentName adminReceiver, boolean refreshing, int userHandle, Bundle onEnableData) {
        Throwable th;
        Bundle bundle;
        ComponentName componentName = adminReceiver;
        int i = userHandle;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
        enforceFullCrossUsersPermission(i);
        DevicePolicyData policy = getUserData(i);
        DeviceAdminInfo info = findAdmin(componentName, i, true);
        synchronized (getLockObject()) {
            try {
                checkActiveAdminPrecondition(componentName, info, policy);
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    boolean z;
                    ActiveAdmin existingAdmin = getActiveAdminUncheckedLocked(componentName, i);
                    if (!refreshing) {
                        if (existingAdmin != null) {
                            throw new IllegalArgumentException("Admin is already added");
                        }
                    }
                    int i2 = 0;
                    ActiveAdmin newAdmin = new ActiveAdmin(info, false);
                    if (existingAdmin != null) {
                        z = existingAdmin.testOnlyAdmin;
                    } else {
                        z = isPackageTestOnly(adminReceiver.getPackageName(), i);
                    }
                    newAdmin.testOnlyAdmin = z;
                    policy.mAdminMap.put(componentName, newAdmin);
                    int replaceIndex = -1;
                    int N = policy.mAdminList.size();
                    while (i2 < N) {
                        if (((ActiveAdmin) policy.mAdminList.get(i2)).info.getComponent().equals(componentName)) {
                            replaceIndex = i2;
                            break;
                        }
                        i2++;
                    }
                    if (replaceIndex == -1) {
                        policy.mAdminList.add(newAdmin);
                        enableIfNecessary(info.getPackageName(), i);
                        this.mUsageStatsManagerInternal.onActiveAdminAdded(adminReceiver.getPackageName(), i);
                    } else {
                        policy.mAdminList.set(replaceIndex, newAdmin);
                    }
                    saveSettingsLocked(i);
                    try {
                        sendAdminCommandLocked(newAdmin, "android.app.action.DEVICE_ADMIN_ENABLED", onEnableData, null);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setActiveAdmin(");
                        stringBuilder.append(componentName);
                        stringBuilder.append("), by user ");
                        stringBuilder.append(i);
                        Flog.i(305, stringBuilder.toString());
                        this.mInjector.binderRestoreCallingIdentity(ident);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    bundle = onEnableData;
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                bundle = onEnableData;
            }
        }
    }

    private void loadAdminDataAsync() {
        this.mInjector.postOnSystemServerInitThreadPool(new -$$Lambda$DevicePolicyManagerService$_Nw-YGl5ncBg-LJs8W81WNW6xoU(this));
    }

    public static /* synthetic */ void lambda$loadAdminDataAsync$0(DevicePolicyManagerService devicePolicyManagerService) {
        devicePolicyManagerService.pushActiveAdminPackages();
        devicePolicyManagerService.mUsageStatsManagerInternal.onAdminDataAvailable();
        devicePolicyManagerService.pushAllMeteredRestrictedPackages();
        devicePolicyManagerService.mInjector.getNetworkPolicyManagerInternal().onAdminDataAvailable();
    }

    private void pushActiveAdminPackages() {
        synchronized (getLockObject()) {
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int i = users.size() - 1; i >= 0; i--) {
                int userId = ((UserInfo) users.get(i)).id;
                this.mUsageStatsManagerInternal.setActiveAdminApps(getActiveAdminPackagesLocked(userId), userId);
            }
        }
    }

    private void pushAllMeteredRestrictedPackages() {
        synchronized (getLockObject()) {
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int i = users.size() - 1; i >= 0; i--) {
                int userId = ((UserInfo) users.get(i)).id;
                this.mInjector.getNetworkPolicyManagerInternal().setMeteredRestrictedPackagesAsync(getMeteredDisabledPackagesLocked(userId), userId);
            }
        }
    }

    private void pushActiveAdminPackagesLocked(int userId) {
        this.mUsageStatsManagerInternal.setActiveAdminApps(getActiveAdminPackagesLocked(userId), userId);
    }

    private Set<String> getActiveAdminPackagesLocked(int userId) {
        DevicePolicyData policy = getUserData(userId);
        Set<String> adminPkgs = null;
        for (int i = policy.mAdminList.size() - 1; i >= 0; i--) {
            String pkgName = ((ActiveAdmin) policy.mAdminList.get(i)).info.getPackageName();
            if (adminPkgs == null) {
                adminPkgs = new ArraySet();
            }
            adminPkgs.add(pkgName);
        }
        return adminPkgs;
    }

    private void transferActiveAdminUncheckedLocked(ComponentName incomingReceiver, ComponentName outgoingReceiver, int userHandle) {
        DevicePolicyData policy = getUserData(userHandle);
        if (policy.mAdminMap.containsKey(outgoingReceiver) || !policy.mAdminMap.containsKey(incomingReceiver)) {
            DeviceAdminInfo incomingDeviceInfo = findAdmin(incomingReceiver, userHandle, true);
            ActiveAdmin adminToTransfer = (ActiveAdmin) policy.mAdminMap.get(outgoingReceiver);
            int oldAdminUid = adminToTransfer.getUid();
            adminToTransfer.transfer(incomingDeviceInfo);
            policy.mAdminMap.remove(outgoingReceiver);
            policy.mAdminMap.put(incomingReceiver, adminToTransfer);
            if (policy.mPasswordOwner == oldAdminUid) {
                policy.mPasswordOwner = adminToTransfer.getUid();
            }
            saveSettingsLocked(userHandle);
            sendAdminCommandLocked(adminToTransfer, "android.app.action.DEVICE_ADMIN_ENABLED", null, null);
        }
    }

    private void checkActiveAdminPrecondition(ComponentName adminReceiver, DeviceAdminInfo info, DevicePolicyData policy) {
        StringBuilder stringBuilder;
        if (info == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad admin: ");
            stringBuilder.append(adminReceiver);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!info.getActivityInfo().applicationInfo.isInternal()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Only apps in internal storage can be active admin: ");
            stringBuilder.append(adminReceiver);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (info.getActivityInfo().applicationInfo.isInstantApp()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Instant apps cannot be device admins: ");
            stringBuilder.append(adminReceiver);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (policy.mRemovingAdmins.contains(adminReceiver)) {
            throw new IllegalArgumentException("Trying to set an admin which is being removed");
        }
    }

    public boolean isAdminActive(ComponentName adminReceiver, int userHandle) {
        boolean isAdminActive = false;
        if (this.mHasFeature) {
            enforceFullCrossUsersPermission(userHandle);
            synchronized (getLockObject()) {
                if (getActiveAdminUncheckedLocked(adminReceiver, userHandle) != null) {
                    isAdminActive = true;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isAdminActive(");
                stringBuilder.append(adminReceiver);
                stringBuilder.append("), by user ");
                stringBuilder.append(userHandle);
                stringBuilder.append(", return ");
                stringBuilder.append(isAdminActive);
                Flog.i(305, stringBuilder.toString());
            }
            return isAdminActive;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isAdminActive(");
        stringBuilder2.append(adminReceiver);
        stringBuilder2.append("), by user ");
        stringBuilder2.append(userHandle);
        stringBuilder2.append(", return false, cause no feature");
        Flog.i(305, stringBuilder2.toString());
        return false;
    }

    public boolean isRemovingAdmin(ComponentName adminReceiver, int userHandle) {
        if (!this.mHasFeature) {
            return false;
        }
        boolean contains;
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            contains = getUserData(userHandle).mRemovingAdmins.contains(adminReceiver);
        }
        return contains;
    }

    public boolean hasGrantedPolicy(ComponentName adminReceiver, int policyId, int userHandle) {
        if (!this.mHasFeature) {
            return false;
        }
        boolean usesPolicy;
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            ActiveAdmin administrator = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
            if (administrator != null) {
                usesPolicy = administrator.info.usesPolicy(policyId);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No active admin ");
                stringBuilder.append(adminReceiver);
                throw new SecurityException(stringBuilder.toString());
            }
        }
        return usesPolicy;
    }

    public List<ComponentName> getActiveAdmins(int userHandle) {
        if (!this.mHasFeature) {
            return Collections.EMPTY_LIST;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            if (N <= 0) {
                return null;
            }
            ArrayList<ComponentName> res = new ArrayList(N);
            for (int i = 0; i < N; i++) {
                res.add(((ActiveAdmin) policy.mAdminList.get(i)).info.getComponent());
            }
            return res;
        }
    }

    public boolean packageHasActiveAdmins(String packageName, int userHandle) {
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                if (((ActiveAdmin) policy.mAdminList.get(i)).info.getPackageName().equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void forceRemoveActiveAdmin(ComponentName adminReceiver, int userHandle) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(adminReceiver, "ComponentName is null");
            enforceShell("forceRemoveActiveAdmin");
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                synchronized (getLockObject()) {
                    if (isAdminTestOnlyLocked(adminReceiver, userHandle)) {
                        if (isDeviceOwner(adminReceiver, userHandle)) {
                            clearDeviceOwnerLocked(getDeviceOwnerAdminLocked(), userHandle);
                        }
                        if (isProfileOwner(adminReceiver, userHandle)) {
                            clearProfileOwnerLocked(getActiveAdminUncheckedLocked(adminReceiver, userHandle, null), userHandle);
                        }
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Attempt to remove non-test admin ");
                        stringBuilder.append(adminReceiver);
                        stringBuilder.append(" ");
                        stringBuilder.append(userHandle);
                        throw new SecurityException(stringBuilder.toString());
                    }
                }
                removeAdminArtifacts(adminReceiver, userHandle);
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Admin ");
                stringBuilder2.append(adminReceiver);
                stringBuilder2.append(" removed from user ");
                stringBuilder2.append(userHandle);
                Slog.i(str, stringBuilder2.toString());
                this.mInjector.binderRestoreCallingIdentity(ident);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    private void clearDeviceOwnerUserRestrictionLocked(UserHandle userHandle) {
        if (this.mUserManager.hasUserRestriction("no_add_user", userHandle)) {
            this.mUserManager.setUserRestriction("no_add_user", false, userHandle);
        }
    }

    private boolean isPackageTestOnly(String packageName, int userHandle) {
        try {
            ApplicationInfo ai = this.mInjector.getIPackageManager().getApplicationInfo(packageName, 786432, userHandle);
            if (ai != null) {
                return (ai.flags & 256) != 0;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't find package: ");
                stringBuilder.append(packageName);
                stringBuilder.append(" on user ");
                stringBuilder.append(userHandle);
                throw new IllegalStateException(stringBuilder.toString());
            }
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isAdminTestOnlyLocked(ComponentName who, int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        return admin != null && admin.testOnlyAdmin;
    }

    private void enforceShell(String method) {
        int callingUid = this.mInjector.binderGetCallingUid();
        if (callingUid != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && callingUid != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Non-shell user attempted to call ");
            stringBuilder.append(method);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public void removeActiveAdmin(ComponentName adminReceiver, int userHandle) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.DEVICE_POLICY_REMOVEACTIVEADMIN);
        if (this.mHasFeature) {
            enforceFullCrossUsersPermission(userHandle);
            enforceUserUnlocked(userHandle);
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
                if (admin == null) {
                    return;
                }
                if (!isDeviceOwner(adminReceiver, userHandle)) {
                    if (!isProfileOwner(adminReceiver, userHandle)) {
                        if (admin.getUid() != this.mInjector.binderGetCallingUid()) {
                            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
                        }
                        long ident = this.mInjector.binderClearCallingIdentity();
                        try {
                            notifyPlugins(adminReceiver, userHandle);
                            removeActiveAdminLocked(adminReceiver, userHandle);
                            return;
                        } finally {
                            this.mInjector.binderRestoreCallingIdentity(ident);
                        }
                    }
                }
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Device/profile owner cannot be removed: component=");
                stringBuilder.append(adminReceiver);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    public boolean isSeparateProfileChallengeAllowed(int userHandle) {
        ComponentName profileOwner = getProfileOwner(userHandle);
        return profileOwner != null && getTargetSdk(profileOwner.getPackageName(), userHandle) > 23;
    }

    public void setPasswordQuality(ComponentName who, int quality, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            validateQualityConstant(quality);
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                PasswordMetrics metrics = getActiveAdminForCallerLocked(who, null, parent).minimumPasswordMetrics;
                if (metrics.quality != quality) {
                    metrics.quality = quality;
                    updatePasswordValidityCheckpointLocked(userId, parent);
                    saveSettingsLocked(userId);
                }
                maybeLogPasswordComplexitySet(who, userId, parent, metrics);
            }
        }
    }

    private void updatePasswordValidityCheckpointLocked(int userHandle, boolean parent) {
        int credentialOwner = getCredentialOwner(userHandle, parent);
        DevicePolicyData policy = getUserData(credentialOwner);
        PasswordMetrics metrics = getUserPasswordMetricsLocked(credentialOwner);
        if (metrics == null) {
            metrics = new PasswordMetrics();
        }
        policy.mPasswordValidAtLastCheckpoint = isPasswordSufficientForUserWithoutCheckpointLocked(metrics, userHandle, parent);
        saveSettingsLocked(credentialOwner);
    }

    public int getPasswordQuality(ComponentName who, int userHandle, boolean parent) {
        int i;
        int i2 = 0;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            int mode = 0;
            if (who != null) {
                try {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    i = admin != null ? admin.minimumPasswordMetrics.quality : 0;
                } finally {
                }
            } else {
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
                int N = admins.size();
                while (i2 < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) admins.get(i2);
                    if (mode < admin2.minimumPasswordMetrics.quality) {
                        mode = admin2.minimumPasswordMetrics.quality;
                    }
                    i2++;
                }
                return mode;
            }
        }
        return i;
    }

    private List<ActiveAdmin> getActiveAdminsForLockscreenPoliciesLocked(int userHandle, boolean parent) {
        if (!parent && isSeparateProfileChallengeEnabled(userHandle)) {
            return getUserDataUnchecked(userHandle).mAdminList;
        }
        ArrayList<ActiveAdmin> admins = new ArrayList();
        for (UserInfo userInfo : this.mUserManager.getProfiles(userHandle)) {
            DevicePolicyData policy = getUserData(userInfo.id);
            if (userInfo.isManagedProfile()) {
                boolean hasSeparateChallenge = isSeparateProfileChallengeEnabled(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                    if (admin.hasParentActiveAdmin()) {
                        admins.add(admin.getParentActiveAdmin());
                    }
                    if (!hasSeparateChallenge) {
                        admins.add(admin);
                    }
                }
            } else {
                admins.addAll(policy.mAdminList);
            }
        }
        return admins;
    }

    private boolean isSeparateProfileChallengeEnabled(int userHandle) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            boolean isSeparateProfileChallengeEnabled = this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userHandle);
            return isSeparateProfileChallengeEnabled;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void setPasswordMinimumLength(ComponentName who, int length, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                PasswordMetrics metrics = getActiveAdminForCallerLocked(who, null, parent).minimumPasswordMetrics;
                if (metrics.length != length) {
                    metrics.length = length;
                    updatePasswordValidityCheckpointLocked(userId, parent);
                    saveSettingsLocked(userId);
                }
                maybeLogPasswordComplexitySet(who, userId, parent, metrics);
            }
        }
    }

    public int getPasswordMinimumLength(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, -$$Lambda$DevicePolicyManagerService$NzTaj70nEECGXhr52RbDyXK_fPU.INSTANCE, 0);
    }

    public void setPasswordHistoryLength(ComponentName who, int length, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
                if (ap.passwordHistoryLength != length) {
                    ap.passwordHistoryLength = length;
                    updatePasswordValidityCheckpointLocked(userId, parent);
                    saveSettingsLocked(userId);
                }
            }
            if (SecurityLog.isLoggingEnabled()) {
                int affectedUserId = parent ? getProfileParentId(userId) : userId;
                SecurityLog.writeEvent(210018, new Object[]{who.getPackageName(), Integer.valueOf(userId), Integer.valueOf(affectedUserId), Integer.valueOf(length)});
            }
        }
    }

    public int getPasswordHistoryLength(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, -$$Lambda$DevicePolicyManagerService$kf4uUzLBApkNlieB7zr8MNfAxbg.INSTANCE, 0);
    }

    public void setPasswordExpirationTimeout(ComponentName who, long timeout, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            Preconditions.checkArgumentNonnegative(timeout, "Timeout must be >= 0 ms");
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 6, parent);
                long expiration = timeout > 0 ? System.currentTimeMillis() + timeout : 0;
                ap.passwordExpirationDate = expiration;
                ap.passwordExpirationTimeout = timeout;
                if (timeout > 0) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setPasswordExpiration(): password will expire on ");
                    stringBuilder.append(DateFormat.getDateTimeInstance(2, 2).format(new Date(expiration)));
                    Slog.w(str, stringBuilder.toString());
                }
                saveSettingsLocked(userHandle);
                setExpirationAlarmCheckLocked(this.mContext, userHandle, parent);
            }
            if (SecurityLog.isLoggingEnabled()) {
                int affectedUserId = parent ? getProfileParentId(userHandle) : userHandle;
                SecurityLog.writeEvent(210016, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), Integer.valueOf(affectedUserId), Long.valueOf(timeout)});
            }
        }
    }

    public long getPasswordExpirationTimeout(ComponentName who, int userHandle, boolean parent) {
        long j;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            long timeout = 0;
            if (who != null) {
                try {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    j = admin != null ? admin.passwordExpirationTimeout : 0;
                } finally {
                }
            } else {
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
                int N = admins.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = (ActiveAdmin) admins.get(i);
                    if (timeout == 0 || (admin2.passwordExpirationTimeout != 0 && timeout > admin2.passwordExpirationTimeout)) {
                        timeout = admin2.passwordExpirationTimeout;
                    }
                }
                return timeout;
            }
        }
        return j;
    }

    public boolean addCrossProfileWidgetProvider(ComponentName admin, String packageName) {
        int userId = UserHandle.getCallingUserId();
        List<String> changedProviders = null;
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
            if (activeAdmin.crossProfileWidgetProviders == null) {
                activeAdmin.crossProfileWidgetProviders = new ArrayList();
            }
            List<String> providers = activeAdmin.crossProfileWidgetProviders;
            if (!providers.contains(packageName)) {
                providers.add(packageName);
                changedProviders = new ArrayList(providers);
                saveSettingsLocked(userId);
            }
        }
        if (changedProviders == null) {
            return false;
        }
        this.mLocalService.notifyCrossProfileProvidersChanged(userId, changedProviders);
        return true;
    }

    /* JADX WARNING: Missing block: B:13:0x002f, code skipped:
            if (r1 == null) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:14:0x0031, code skipped:
            com.android.server.devicepolicy.DevicePolicyManagerService.LocalService.access$2200(r7.mLocalService, r0, r1);
     */
    /* JADX WARNING: Missing block: B:15:0x0037, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:16:0x0038, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean removeCrossProfileWidgetProvider(ComponentName admin, String packageName) {
        int userId = UserHandle.getCallingUserId();
        List<String> changedProviders = null;
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
            if (activeAdmin.crossProfileWidgetProviders != null) {
                if (!activeAdmin.crossProfileWidgetProviders.isEmpty()) {
                    List<String> providers = activeAdmin.crossProfileWidgetProviders;
                    if (providers.remove(packageName)) {
                        changedProviders = new ArrayList(providers);
                        saveSettingsLocked(userId);
                    }
                }
            }
            return false;
        }
    }

    public List<String> getCrossProfileWidgetProviders(ComponentName admin) {
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
            if (activeAdmin.crossProfileWidgetProviders != null) {
                if (!activeAdmin.crossProfileWidgetProviders.isEmpty()) {
                    if (this.mInjector.binderIsCallingUidMyUid()) {
                        ArrayList arrayList = new ArrayList(activeAdmin.crossProfileWidgetProviders);
                        return arrayList;
                    }
                    List list = activeAdmin.crossProfileWidgetProviders;
                    return list;
                }
            }
            return null;
        }
    }

    private long getPasswordExpirationLocked(ComponentName who, int userHandle, boolean parent) {
        long timeout = 0;
        if (who != null) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
            return admin != null ? admin.passwordExpirationDate : 0;
        }
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
        int N = admins.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin admin2 = (ActiveAdmin) admins.get(i);
            if (timeout == 0 || (admin2.passwordExpirationDate != 0 && timeout > admin2.passwordExpirationDate)) {
                timeout = admin2.passwordExpirationDate;
            }
        }
        return timeout;
    }

    public long getPasswordExpiration(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        long passwordExpirationLocked;
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            passwordExpirationLocked = getPasswordExpirationLocked(who, userHandle, parent);
        }
        return passwordExpirationLocked;
    }

    public void setPasswordMinimumUpperCase(ComponentName who, int length, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                PasswordMetrics metrics = getActiveAdminForCallerLocked(who, null, parent).minimumPasswordMetrics;
                if (metrics.upperCase != length) {
                    metrics.upperCase = length;
                    updatePasswordValidityCheckpointLocked(userId, parent);
                    saveSettingsLocked(userId);
                }
                maybeLogPasswordComplexitySet(who, userId, parent, metrics);
            }
        }
    }

    public int getPasswordMinimumUpperCase(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, -$$Lambda$DevicePolicyManagerService$GdvC4eub6BtkkX5BnHuPR5Ob0ag.INSTANCE, 393216);
    }

    public void setPasswordMinimumLowerCase(ComponentName who, int length, boolean parent) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            PasswordMetrics metrics = getActiveAdminForCallerLocked(who, null, parent).minimumPasswordMetrics;
            if (metrics.lowerCase != length) {
                metrics.lowerCase = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
            maybeLogPasswordComplexitySet(who, userId, parent, metrics);
        }
    }

    public int getPasswordMinimumLowerCase(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, -$$Lambda$DevicePolicyManagerService$O6O5T5aoG6MmH8aAAGYNwYhbtw8.INSTANCE, 393216);
    }

    public void setPasswordMinimumLetters(ComponentName who, int length, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                PasswordMetrics metrics = getActiveAdminForCallerLocked(who, null, parent).minimumPasswordMetrics;
                if (metrics.letters != length) {
                    metrics.letters = length;
                    updatePasswordValidityCheckpointLocked(userId, parent);
                    saveSettingsLocked(userId);
                }
                maybeLogPasswordComplexitySet(who, userId, parent, metrics);
            }
        }
    }

    public int getPasswordMinimumLetters(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, -$$Lambda$DevicePolicyManagerService$tN28Me5AH2pjgYHvPnMAsCjK_NU.INSTANCE, 393216);
    }

    public void setPasswordMinimumNumeric(ComponentName who, int length, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                PasswordMetrics metrics = getActiveAdminForCallerLocked(who, null, parent).minimumPasswordMetrics;
                if (metrics.numeric != length) {
                    metrics.numeric = length;
                    updatePasswordValidityCheckpointLocked(userId, parent);
                    saveSettingsLocked(userId);
                }
                maybeLogPasswordComplexitySet(who, userId, parent, metrics);
            }
        }
    }

    public int getPasswordMinimumNumeric(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, -$$Lambda$DevicePolicyManagerService$BYd2ftVebU2Ktj6tr-DFfrGE5TE.INSTANCE, 393216);
    }

    public void setPasswordMinimumSymbols(ComponentName who, int length, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, null, parent);
                PasswordMetrics metrics = ap.minimumPasswordMetrics;
                if (metrics.symbols != length) {
                    ap.minimumPasswordMetrics.symbols = length;
                    updatePasswordValidityCheckpointLocked(userId, parent);
                    saveSettingsLocked(userId);
                }
                maybeLogPasswordComplexitySet(who, userId, parent, metrics);
            }
        }
    }

    public int getPasswordMinimumSymbols(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, -$$Lambda$DevicePolicyManagerService$CClEW-CtZQRadOocoqGh0wiKhG4.INSTANCE, 393216);
    }

    public void setPasswordMinimumNonLetter(ComponentName who, int length, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, null, parent);
                PasswordMetrics metrics = ap.minimumPasswordMetrics;
                if (metrics.nonLetter != length) {
                    ap.minimumPasswordMetrics.nonLetter = length;
                    updatePasswordValidityCheckpointLocked(userId, parent);
                    saveSettingsLocked(userId);
                }
                maybeLogPasswordComplexitySet(who, userId, parent, metrics);
            }
        }
    }

    public int getPasswordMinimumNonLetter(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, -$$Lambda$DevicePolicyManagerService$8nvbMteplUbtaSMuw4DWJ-MQa4g.INSTANCE, 393216);
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getStrictestPasswordRequirement(ComponentName who, int userHandle, boolean parent, Function<ActiveAdmin, Integer> getter, int minimumPasswordQuality) {
        int i = 0;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    if (admin != null) {
                        i = ((Integer) getter.apply(admin)).intValue();
                    }
                } catch (Throwable th) {
                }
            } else {
                int maxValue = 0;
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
                int N = admins.size();
                while (i < N) {
                    ActiveAdmin admin2 = (ActiveAdmin) admins.get(i);
                    if (isLimitPasswordAllowed(admin2, minimumPasswordQuality)) {
                        Integer adminValue = (Integer) getter.apply(admin2);
                        if (adminValue.intValue() > maxValue) {
                            maxValue = adminValue.intValue();
                        }
                    }
                    i++;
                }
                return maxValue;
            }
        }
    }

    public boolean isActivePasswordSufficient(int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return true;
        }
        boolean isActivePasswordSufficientForUserLocked;
        enforceFullCrossUsersPermission(userHandle);
        enforceUserUnlocked(userHandle, parent);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(null, 0, parent);
            int credentialOwner = getCredentialOwner(userHandle, parent);
            DevicePolicyData policy = getUserDataUnchecked(credentialOwner);
            isActivePasswordSufficientForUserLocked = isActivePasswordSufficientForUserLocked(policy.mPasswordValidAtLastCheckpoint, getUserPasswordMetricsLocked(credentialOwner), userHandle, parent);
        }
        return isActivePasswordSufficientForUserLocked;
    }

    public boolean isUsingUnifiedPassword(ComponentName admin) {
        if (!this.mHasFeature) {
            return true;
        }
        int userId = this.mInjector.userHandleGetCallingUserId();
        enforceProfileOrDeviceOwner(admin);
        enforceManagedProfile(userId, "query unified challenge status");
        return 1 ^ isSeparateProfileChallengeEnabled(userId);
    }

    public boolean isProfileActivePasswordSufficientForParent(int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        boolean isActivePasswordSufficientForUserLocked;
        enforceFullCrossUsersPermission(userHandle);
        enforceManagedProfile(userHandle, "call APIs refering to the parent profile");
        synchronized (getLockObject()) {
            int targetUser = getProfileParentId(userHandle);
            enforceUserUnlocked(targetUser, false);
            int credentialOwner = getCredentialOwner(userHandle, false);
            DevicePolicyData policy = getUserDataUnchecked(credentialOwner);
            isActivePasswordSufficientForUserLocked = isActivePasswordSufficientForUserLocked(policy.mPasswordValidAtLastCheckpoint, getUserPasswordMetricsLocked(credentialOwner), targetUser, false);
        }
        return isActivePasswordSufficientForUserLocked;
    }

    private boolean isActivePasswordSufficientForUserLocked(boolean passwordValidAtLastCheckpoint, PasswordMetrics metrics, int userHandle, boolean parent) {
        if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled() && metrics == null) {
            return passwordValidAtLastCheckpoint;
        }
        if (metrics == null) {
            metrics = new PasswordMetrics();
        }
        return isPasswordSufficientForUserWithoutCheckpointLocked(metrics, userHandle, parent);
    }

    private boolean isPasswordSufficientForUserWithoutCheckpointLocked(PasswordMetrics passwordMetrics, int userHandle, boolean parent) {
        int requiredPasswordQuality = getPasswordQuality(null, userHandle, parent);
        boolean z = false;
        if (passwordMetrics.quality < requiredPasswordQuality) {
            return false;
        }
        if (requiredPasswordQuality >= 131072 && passwordMetrics.length < getPasswordMinimumLength(null, userHandle, parent)) {
            return false;
        }
        if (mHwCustDevicePolicyManagerService != null && mHwCustDevicePolicyManagerService.isForbiddenSimplePwdFeatureEnable() && !getAllowSimplePassword(null, userHandle)) {
            Slog.d(LOG_TAG, "forbiddenSimplePassword and current pwd is simple return false");
            return false;
        } else if (requiredPasswordQuality != 393216) {
            return true;
        } else {
            if (passwordMetrics.upperCase >= getPasswordMinimumUpperCase(null, userHandle, parent) && passwordMetrics.lowerCase >= getPasswordMinimumLowerCase(null, userHandle, parent) && passwordMetrics.letters >= getPasswordMinimumLetters(null, userHandle, parent) && passwordMetrics.numeric >= getPasswordMinimumNumeric(null, userHandle, parent) && passwordMetrics.symbols >= getPasswordMinimumSymbols(null, userHandle, parent) && passwordMetrics.nonLetter >= getPasswordMinimumNonLetter(null, userHandle, parent)) {
                z = true;
            }
            return z;
        }
    }

    public int getCurrentFailedPasswordAttempts(int userHandle, boolean parent) {
        int i;
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            if (!isCallerWithSystemUid()) {
                getActiveAdminForCallerLocked(null, 1, parent);
            }
            i = getUserDataUnchecked(getCredentialOwner(userHandle, parent)).mFailedPasswordAttempts;
        }
        return i;
    }

    public void setMaximumFailedPasswordsForWipe(ComponentName who, int num, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(who, 4, parent);
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 1, parent);
                if (ap.maximumFailedPasswordsForWipe != num) {
                    ap.maximumFailedPasswordsForWipe = num;
                    saveSettingsLocked(userId);
                }
            }
            if (SecurityLog.isLoggingEnabled()) {
                int affectedUserId = parent ? getProfileParentId(userId) : userId;
                SecurityLog.writeEvent(210020, new Object[]{who.getPackageName(), Integer.valueOf(userId), Integer.valueOf(affectedUserId), Integer.valueOf(num)});
            }
        }
    }

    public int getMaximumFailedPasswordsForWipe(ComponentName who, int userHandle, boolean parent) {
        int i = 0;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            ActiveAdmin admin;
            if (who != null) {
                try {
                    admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                } catch (Throwable th) {
                }
            } else {
                admin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle, parent);
            }
            if (admin != null) {
                i = admin.maximumFailedPasswordsForWipe;
            }
        }
        return i;
    }

    public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle, boolean parent) {
        int i = -10000;
        if (!this.mHasFeature) {
            return -10000;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle, parent);
            if (admin != null) {
                i = admin.getUserHandle().getIdentifier();
            }
        }
        return i;
    }

    private ActiveAdmin getAdminWithMinimumFailedPasswordsForWipeLocked(int userHandle, boolean parent) {
        int count = 0;
        ActiveAdmin strictestAdmin = null;
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
        int N = admins.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin admin = (ActiveAdmin) admins.get(i);
            if (admin.maximumFailedPasswordsForWipe != 0) {
                int userId = admin.getUserHandle().getIdentifier();
                if (count == 0 || count > admin.maximumFailedPasswordsForWipe || (count == admin.maximumFailedPasswordsForWipe && getUserInfo(userId).isPrimary())) {
                    count = admin.maximumFailedPasswordsForWipe;
                    strictestAdmin = admin;
                }
            }
        }
        return strictestAdmin;
    }

    private UserInfo getUserInfo(int userId) {
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(userId);
            return userInfo;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    private boolean canPOorDOCallResetPassword(ActiveAdmin admin, int userId) {
        return getTargetSdk(admin.info.getPackageName(), userId) < 26;
    }

    private boolean canUserHaveUntrustedCredentialReset(int userId) {
        synchronized (getLockObject()) {
            Iterator it = getUserData(userId).mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = (ActiveAdmin) it.next();
                if (isActiveAdminWithPolicyForUserLocked(admin, -1, userId)) {
                    if (canPOorDOCallResetPassword(admin, userId)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public boolean resetPassword(String passwordOrNull, int flags) throws RemoteException {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.DEVICE_POLICY_RESETPASSWORD);
        int callingUid = this.mInjector.binderGetCallingUid();
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        String password = passwordOrNull != null ? passwordOrNull : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (TextUtils.isEmpty(password)) {
            enforceNotManagedProfile(userHandle, "clear the active password");
        }
        synchronized (getLockObject()) {
            boolean preN;
            ActiveAdmin admin = getActiveAdminWithPolicyForUidLocked(null, -1, callingUid);
            boolean z = true;
            if (admin == null) {
                if (getTargetSdk(getActiveAdminForCallerLocked(null, 2).info.getPackageName(), userHandle) > 23) {
                    z = false;
                }
                preN = z;
                if (TextUtils.isEmpty(password)) {
                    if (preN) {
                        Slog.e(LOG_TAG, "Cannot call with null password");
                        return false;
                    }
                    throw new SecurityException("Cannot call with null password");
                } else if (isLockScreenSecureUnchecked(userHandle)) {
                    if (preN) {
                        Slog.e(LOG_TAG, "Admin cannot change current password");
                        return false;
                    }
                    throw new SecurityException("Admin cannot change current password");
                }
            } else if (canPOorDOCallResetPassword(admin, userHandle)) {
                if (getTargetSdk(admin.info.getPackageName(), userHandle) > 23) {
                    z = false;
                }
                preN = z;
            } else {
                throw new SecurityException("resetPassword() is deprecated for DPC targeting O or later");
            }
            if (!isManagedProfile(userHandle)) {
                for (UserInfo userInfo : this.mUserManager.getProfiles(userHandle)) {
                    if (userInfo.isManagedProfile()) {
                        if (preN) {
                            Slog.e(LOG_TAG, "Cannot reset password on user has managed profile");
                            return false;
                        }
                        throw new IllegalStateException("Cannot reset password on user has managed profile");
                    }
                }
            }
            if (this.mUserManager.isUserUnlocked(userHandle)) {
                return resetPasswordInternal(password, 0, null, flags, callingUid, userHandle);
            } else if (preN) {
                Slog.e(LOG_TAG, "Cannot reset password when user is locked");
                return false;
            } else {
                throw new IllegalStateException("Cannot reset password when user is locked");
            }
        }
    }

    /* JADX WARNING: Missing block: B:65:0x01ba, code skipped:
            r13 = r0;
            r14 = getUserData(r12);
     */
    /* JADX WARNING: Missing block: B:66:0x01c1, code skipped:
            if (r14.mPasswordOwner < 0) goto L_0x01d1;
     */
    /* JADX WARNING: Missing block: B:68:0x01c5, code skipped:
            if (r14.mPasswordOwner == r11) goto L_0x01d1;
     */
    /* JADX WARNING: Missing block: B:69:0x01c7, code skipped:
            android.util.Slog.w(LOG_TAG, "resetPassword: already set by another uid and not entered by user");
     */
    /* JADX WARNING: Missing block: B:70:0x01d0, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:71:0x01d1, code skipped:
            r15 = isCallerDeviceOwner(r11);
            r16 = true;
     */
    /* JADX WARNING: Missing block: B:72:0x01da, code skipped:
            if ((r25 & 2) == 0) goto L_0x01df;
     */
    /* JADX WARNING: Missing block: B:73:0x01dc, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:74:0x01df, code skipped:
            r2 = false;
     */
    /* JADX WARNING: Missing block: B:75:0x01e0, code skipped:
            r17 = r2;
     */
    /* JADX WARNING: Missing block: B:76:0x01e2, code skipped:
            if (r15 == false) goto L_0x01e9;
     */
    /* JADX WARNING: Missing block: B:77:0x01e4, code skipped:
            if (r17 == false) goto L_0x01e9;
     */
    /* JADX WARNING: Missing block: B:78:0x01e6, code skipped:
            setDoNotAskCredentialsOnBoot();
     */
    /* JADX WARNING: Missing block: B:79:0x01e9, code skipped:
            r8 = r1.mInjector.binderClearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:80:0x01f2, code skipped:
            if (r24 != null) goto L_0x020e;
     */
    /* JADX WARNING: Missing block: B:83:0x01f8, code skipped:
            if (android.text.TextUtils.isEmpty(r21) != false) goto L_0x0200;
     */
    /* JADX WARNING: Missing block: B:84:0x01fa, code skipped:
            r1.mLockPatternUtils.saveLockPassword(r10, null, r13, r12);
     */
    /* JADX WARNING: Missing block: B:85:0x0200, code skipped:
            r1.mLockPatternUtils.clearLock(null, r12);
     */
    /* JADX WARNING: Missing block: B:86:0x0205, code skipped:
            r2 = true;
            r0 = -1;
            r18 = r8;
     */
    /* JADX WARNING: Missing block: B:87:0x020a, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:88:0x020b, code skipped:
            r5 = r8;
     */
    /* JADX WARNING: Missing block: B:90:?, code skipped:
            r2 = r1.mLockPatternUtils;
     */
    /* JADX WARNING: Missing block: B:91:0x0214, code skipped:
            if (android.text.TextUtils.isEmpty(r21) == false) goto L_0x0218;
     */
    /* JADX WARNING: Missing block: B:92:0x0216, code skipped:
            r4 = -1;
     */
    /* JADX WARNING: Missing block: B:93:0x0218, code skipped:
            r4 = 2;
     */
    /* JADX WARNING: Missing block: B:94:0x021a, code skipped:
            r0 = -1;
            r18 = r8;
     */
    /* JADX WARNING: Missing block: B:96:?, code skipped:
            r2 = r2.setLockCredentialWithToken(r10, r4, r13, r22, r24, r12);
     */
    /* JADX WARNING: Missing block: B:98:0x022a, code skipped:
            if ((r25 & 1) == 0) goto L_0x022d;
     */
    /* JADX WARNING: Missing block: B:99:0x022d, code skipped:
            r16 = false;
     */
    /* JADX WARNING: Missing block: B:100:0x022f, code skipped:
            r3 = r16;
     */
    /* JADX WARNING: Missing block: B:101:0x0231, code skipped:
            if (r3 == false) goto L_0x023e;
     */
    /* JADX WARNING: Missing block: B:103:?, code skipped:
            r1.mLockPatternUtils.requireStrongAuth(2, r0);
     */
    /* JADX WARNING: Missing block: B:104:0x023a, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:105:0x023b, code skipped:
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:107:?, code skipped:
            r4 = getLockObject();
     */
    /* JADX WARNING: Missing block: B:108:0x0242, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:109:0x0243, code skipped:
            if (r3 == false) goto L_0x0247;
     */
    /* JADX WARNING: Missing block: B:110:0x0245, code skipped:
            r0 = r11;
     */
    /* JADX WARNING: Missing block: B:113:0x0249, code skipped:
            if (r14.mPasswordOwner == r0) goto L_0x0255;
     */
    /* JADX WARNING: Missing block: B:115:?, code skipped:
            r14.mPasswordOwner = r0;
            saveSettingsLocked(r12);
     */
    /* JADX WARNING: Missing block: B:116:0x0251, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:117:0x0252, code skipped:
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:119:?, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:120:0x0256, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r18);
     */
    /* JADX WARNING: Missing block: B:121:0x025f, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:122:0x0260, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:123:0x0261, code skipped:
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:125:?, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:127:?, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:128:0x0265, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:129:0x0267, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:131:0x0269, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:132:0x026a, code skipped:
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:133:0x026d, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:134:0x026e, code skipped:
            r5 = r8;
     */
    /* JADX WARNING: Missing block: B:135:0x026f, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r5);
     */
    /* JADX WARNING: Missing block: B:136:0x0274, code skipped:
            throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean resetPasswordInternal(String password, long tokenHandle, byte[] token, int flags, int callingUid, int userHandle) {
        String str = password;
        int i = callingUid;
        int i2 = userHandle;
        synchronized (getLockObject()) {
            int realQuality;
            String str2;
            StringBuilder stringBuilder;
            if (mHwCustDevicePolicyManagerService != null && mHwCustDevicePolicyManagerService.isForbiddenSimplePwdFeatureEnable()) {
                boolean isNewPwdSimple = mHwCustDevicePolicyManagerService.isNewPwdSimpleCheck(str, this.mContext);
                if (!getAllowSimplePassword(null, i2) && isNewPwdSimple) {
                    Slog.e(LOG_TAG, "Cannot reset password when forbidden SimplePassword and current pwd is simple");
                    return false;
                }
            }
            int quality = getPasswordQuality(null, i2, false);
            if (quality == DumpState.DUMP_FROZEN) {
                quality = 0;
            }
            PasswordMetrics metrics = PasswordMetrics.computeForPassword(password);
            if (quality != 0) {
                realQuality = metrics.quality;
                if (realQuality >= quality || quality == 393216) {
                    quality = Math.max(realQuality, quality);
                } else {
                    str2 = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("resetPassword: password quality 0x");
                    stringBuilder.append(Integer.toHexString(realQuality));
                    stringBuilder.append(" does not meet required quality 0x");
                    stringBuilder.append(Integer.toHexString(quality));
                    Slog.w(str2, stringBuilder.toString());
                    return false;
                }
            }
            realQuality = getPasswordMinimumLength(null, i2, false);
            if (password.length() < realQuality) {
                str2 = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("resetPassword: password length ");
                stringBuilder.append(password.length());
                stringBuilder.append(" does not meet required length ");
                stringBuilder.append(realQuality);
                Slog.w(str2, stringBuilder.toString());
                return false;
            } else if (quality == 393216) {
                int neededLetters = getPasswordMinimumLetters(null, i2, false);
                if (metrics.letters < neededLetters) {
                    str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("resetPassword: number of letters ");
                    stringBuilder2.append(metrics.letters);
                    stringBuilder2.append(" does not meet required number of letters ");
                    stringBuilder2.append(neededLetters);
                    Slog.w(str2, stringBuilder2.toString());
                    return false;
                }
                int neededNumeric = getPasswordMinimumNumeric(null, i2, false);
                if (metrics.numeric < neededNumeric) {
                    str2 = LOG_TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("resetPassword: number of numerical digits ");
                    stringBuilder3.append(metrics.numeric);
                    stringBuilder3.append(" does not meet required number of numerical digits ");
                    stringBuilder3.append(neededNumeric);
                    Slog.w(str2, stringBuilder3.toString());
                    return false;
                }
                int neededLowerCase = getPasswordMinimumLowerCase(null, i2, false);
                if (metrics.lowerCase < neededLowerCase) {
                    str2 = LOG_TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("resetPassword: number of lowercase letters ");
                    stringBuilder4.append(metrics.lowerCase);
                    stringBuilder4.append(" does not meet required number of lowercase letters ");
                    stringBuilder4.append(neededLowerCase);
                    Slog.w(str2, stringBuilder4.toString());
                    return false;
                }
                int neededUpperCase = getPasswordMinimumUpperCase(null, i2, false);
                StringBuilder stringBuilder5;
                if (metrics.upperCase < neededUpperCase) {
                    str2 = LOG_TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("resetPassword: number of uppercase letters ");
                    stringBuilder5.append(metrics.upperCase);
                    stringBuilder5.append(" does not meet required number of uppercase letters ");
                    stringBuilder5.append(neededUpperCase);
                    Slog.w(str2, stringBuilder5.toString());
                    return false;
                }
                int neededSymbols = getPasswordMinimumSymbols(null, i2, false);
                if (metrics.symbols < neededSymbols) {
                    str2 = LOG_TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("resetPassword: number of special symbols ");
                    stringBuilder5.append(metrics.symbols);
                    stringBuilder5.append(" does not meet required number of special symbols ");
                    stringBuilder5.append(neededSymbols);
                    Slog.w(str2, stringBuilder5.toString());
                    return false;
                }
                neededLetters = getPasswordMinimumNonLetter(null, i2, false);
                if (metrics.nonLetter < neededLetters) {
                    str2 = LOG_TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("resetPassword: number of non-letter characters ");
                    stringBuilder5.append(metrics.nonLetter);
                    stringBuilder5.append(" does not meet required number of non-letter characters ");
                    stringBuilder5.append(neededLetters);
                    Slog.w(str2, stringBuilder5.toString());
                    return false;
                }
            }
        }
    }

    private boolean isLockScreenSecureUnchecked(int userId) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            boolean isSecure = this.mLockPatternUtils.isSecure(userId);
            return isSecure;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private void setDoNotAskCredentialsOnBoot() {
        synchronized (getLockObject()) {
            DevicePolicyData policyData = getUserData(0);
            if (!policyData.doNotAskCredentialsOnBoot) {
                policyData.doNotAskCredentialsOnBoot = true;
                saveSettingsLocked(0);
            }
        }
    }

    public boolean getDoNotAskCredentialsOnBoot() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.QUERY_DO_NOT_ASK_CREDENTIALS_ON_BOOT", null);
        synchronized (getLockObject()) {
            z = getUserData(null).doNotAskCredentialsOnBoot;
        }
        return z;
    }

    public void setMaximumTimeToLock(ComponentName who, long timeMs, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 3, parent);
                if (ap.maximumTimeToUnlock != timeMs) {
                    ap.maximumTimeToUnlock = timeMs;
                    saveSettingsLocked(userHandle);
                    updateMaximumTimeToLockLocked(userHandle);
                }
            }
            if (SecurityLog.isLoggingEnabled()) {
                int affectedUserId = parent ? getProfileParentId(userHandle) : userHandle;
                SecurityLog.writeEvent(210019, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), Integer.valueOf(affectedUserId), Long.valueOf(timeMs)});
            }
        }
    }

    private void updateMaximumTimeToLockLocked(int userId) {
        if (isManagedProfile(userId)) {
            updateProfileLockTimeoutLocked(userId);
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            int parentId = getProfileParentId(userId);
            long timeMs = getMaximumTimeToLockPolicyFromAdmins(getActiveAdminsForLockscreenPoliciesLocked(parentId, false));
            DevicePolicyData policy = getUserDataUnchecked(parentId);
            if (policy.mLastMaximumTimeToLock != timeMs) {
                policy.mLastMaximumTimeToLock = timeMs;
                if (policy.mLastMaximumTimeToLock != JobStatus.NO_LATEST_RUNTIME) {
                    this.mInjector.settingsGlobalPutInt("stay_on_while_plugged_in", 0);
                }
                this.mInjector.binderRestoreCallingIdentity(ident);
                this.mInjector.getPowerManagerInternal().setMaximumScreenOffTimeoutFromDeviceAdmin(0, timeMs);
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private void updateProfileLockTimeoutLocked(int userId) {
        long timeMs;
        if (isSeparateProfileChallengeEnabled(userId)) {
            timeMs = getMaximumTimeToLockPolicyFromAdmins(getActiveAdminsForLockscreenPoliciesLocked(userId, false));
        } else {
            timeMs = JobStatus.NO_LATEST_RUNTIME;
        }
        DevicePolicyData policy = getUserDataUnchecked(userId);
        if (policy.mLastMaximumTimeToLock != timeMs) {
            policy.mLastMaximumTimeToLock = timeMs;
            this.mInjector.getPowerManagerInternal().setMaximumScreenOffTimeoutFromDeviceAdmin(userId, policy.mLastMaximumTimeToLock);
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001b, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:19:0x0032, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getMaximumTimeToLock(ComponentName who, int userHandle, boolean parent) {
        long j = 0;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    if (admin != null) {
                        j = admin.maximumTimeToUnlock;
                    }
                } catch (Throwable th) {
                }
            } else {
                long timeMs = getMaximumTimeToLockPolicyFromAdmins(getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent));
                if (timeMs != JobStatus.NO_LATEST_RUNTIME) {
                    j = timeMs;
                }
            }
        }
    }

    private long getMaximumTimeToLockPolicyFromAdmins(List<ActiveAdmin> admins) {
        long time = JobStatus.NO_LATEST_RUNTIME;
        for (ActiveAdmin admin : admins) {
            if (admin.maximumTimeToUnlock > 0 && admin.maximumTimeToUnlock < time) {
                time = admin.maximumTimeToUnlock;
            }
        }
        return time;
    }

    public void setRequiredStrongAuthTimeout(ComponentName who, long timeoutMs, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            Preconditions.checkArgument(timeoutMs >= 0, "Timeout must not be a negative number.");
            long minimumStrongAuthTimeout = getMinimumStrongAuthTimeoutMs();
            if (timeoutMs != 0 && timeoutMs < minimumStrongAuthTimeout) {
                timeoutMs = minimumStrongAuthTimeout;
            }
            if (timeoutMs > 259200000) {
                timeoutMs = 259200000;
            }
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, -1, parent);
                if (ap.strongAuthUnlockTimeout != timeoutMs) {
                    ap.strongAuthUnlockTimeout = timeoutMs;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getRequiredStrongAuthTimeout(ComponentName who, int userId, boolean parent) {
        if (!this.mHasFeature) {
            return 259200000;
        }
        enforceFullCrossUsersPermission(userId);
        synchronized (getLockObject()) {
            long j = 0;
            if (who != null) {
                try {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userId, parent);
                    if (admin != null) {
                        j = admin.strongAuthUnlockTimeout;
                    }
                } catch (Throwable th) {
                }
            } else {
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userId, parent);
                long strongAuthUnlockTimeout = 259200000;
                for (int i = 0; i < admins.size(); i++) {
                    long timeout = ((ActiveAdmin) admins.get(i)).strongAuthUnlockTimeout;
                    if (timeout != 0) {
                        strongAuthUnlockTimeout = Math.min(timeout, strongAuthUnlockTimeout);
                    }
                }
                j = Math.max(strongAuthUnlockTimeout, getMinimumStrongAuthTimeoutMs());
                return j;
            }
        }
    }

    private long getMinimumStrongAuthTimeoutMs() {
        if (this.mInjector.isBuildDebuggable()) {
            return Math.min(this.mInjector.systemPropertiesGetLong("persist.sys.min_str_auth_timeo", MINIMUM_STRONG_AUTH_TIMEOUT_MS), MINIMUM_STRONG_AUTH_TIMEOUT_MS);
        }
        return MINIMUM_STRONG_AUTH_TIMEOUT_MS;
    }

    public void lockNow(int flags, boolean parent) {
        ActiveAdmin activeAdmin;
        Injector injector;
        Throwable th;
        boolean z = parent;
        if (this.mHasFeature) {
            int callingUserId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(null, 3, z);
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    ComponentName adminComponent = admin.info.getComponent();
                    if ((flags & 1) != 0) {
                        try {
                            enforceManagedProfile(callingUserId, "set FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY");
                            if (!isProfileOwner(adminComponent, callingUserId)) {
                                throw new SecurityException("Only profile owner admins can set FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY");
                            } else if (z) {
                                throw new IllegalArgumentException("Cannot set FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY for the parent");
                            } else if (this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
                                this.mUserManager.evictCredentialEncryptionKey(callingUserId);
                            } else {
                                throw new UnsupportedOperationException("FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY only applies to FBE devices");
                            }
                        } catch (RemoteException e) {
                            activeAdmin = admin;
                            injector = this.mInjector;
                            injector.binderRestoreCallingIdentity(ident);
                        } catch (Throwable th2) {
                            th = th2;
                            activeAdmin = admin;
                            this.mInjector.binderRestoreCallingIdentity(ident);
                            throw th;
                        }
                    }
                    int userToLock = (z || !isSeparateProfileChallengeEnabled(callingUserId)) ? -1 : callingUserId;
                    this.mLockPatternUtils.requireStrongAuth(2, userToLock);
                    if (userToLock == -1) {
                        try {
                            this.mInjector.powerManagerGoToSleep(SystemClock.uptimeMillis(), 1, 0);
                            this.mInjector.getIWindowManager().lockNow(null);
                        } catch (RemoteException e2) {
                            injector = this.mInjector;
                            injector.binderRestoreCallingIdentity(ident);
                        } catch (Throwable th3) {
                            th = th3;
                            this.mInjector.binderRestoreCallingIdentity(ident);
                            throw th;
                        }
                    }
                    this.mInjector.getTrustManager().setDeviceLockedForUser(userToLock, true);
                    if (SecurityLog.isLoggingEnabled()) {
                        int affectedUserId = z ? getProfileParentId(callingUserId) : callingUserId;
                        SecurityLog.writeEvent(210022, new Object[]{adminComponent.getPackageName(), Integer.valueOf(callingUserId), Integer.valueOf(affectedUserId)});
                    }
                    injector = this.mInjector;
                } catch (RemoteException e3) {
                    activeAdmin = admin;
                    injector = this.mInjector;
                    injector.binderRestoreCallingIdentity(ident);
                } catch (Throwable th4) {
                    th = th4;
                    activeAdmin = admin;
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    throw th;
                }
                injector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    public void enforceCanManageCaCerts(ComponentName who, String callerPackage) {
        if (who != null) {
            enforceProfileOrDeviceOwner(who);
        } else if (!isCallerDelegate(callerPackage, "delegation-cert-install")) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_CA_CERTIFICATES", null);
        }
    }

    private void enforceProfileOrDeviceOwner(ComponentName who) {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
        }
    }

    public boolean approveCaCert(String alias, int userId, boolean approval) {
        enforceManageUsers();
        synchronized (getLockObject()) {
            Set<String> certs = getUserData(userId).mAcceptedCaCertificates;
            if (approval ? certs.add(alias) : certs.remove(alias)) {
                saveSettingsLocked(userId);
                this.mCertificateMonitor.onCertificateApprovalsChanged(userId);
                return true;
            }
            return false;
        }
    }

    public boolean isCaCertApproved(String alias, int userId) {
        boolean contains;
        enforceManageUsers();
        synchronized (getLockObject()) {
            contains = getUserData(userId).mAcceptedCaCertificates.contains(alias);
        }
        return contains;
    }

    private void removeCaApprovalsIfNeeded(int userId) {
        for (UserInfo userInfo : this.mUserManager.getProfiles(userId)) {
            boolean isSecure = this.mLockPatternUtils.isSecure(userInfo.id);
            if (userInfo.isManagedProfile()) {
                isSecure |= this.mLockPatternUtils.isSecure(getProfileParentId(userInfo.id));
            }
            if (!isSecure) {
                synchronized (getLockObject()) {
                    getUserData(userInfo.id).mAcceptedCaCertificates.clear();
                    saveSettingsLocked(userInfo.id);
                }
                this.mCertificateMonitor.onCertificateApprovalsChanged(userId);
            }
        }
    }

    public boolean installCaCert(ComponentName admin, String callerPackage, byte[] certBuffer) throws RemoteException {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.DEVICE_POLICY_INSTALLCACERT);
        if (!this.mHasFeature) {
            return false;
        }
        enforceCanManageCaCerts(admin, callerPackage);
        UserHandle userHandle = this.mInjector.binderGetCallingUserHandle();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            String alias = this.mCertificateMonitor.installCaCert(userHandle, certBuffer);
            if (alias == null) {
                Log.w(LOG_TAG, "Problem installing cert");
                return false;
            }
            this.mInjector.binderRestoreCallingIdentity(id);
            synchronized (getLockObject()) {
                getUserData(userHandle.getIdentifier()).mOwnerInstalledCaCerts.add(alias);
                saveSettingsLocked(userHandle.getIdentifier());
            }
            return true;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public void uninstallCaCerts(ComponentName admin, String callerPackage, String[] aliases) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.DEVICE_POLICY_UNINSTALLCACERTS);
        if (this.mHasFeature) {
            enforceCanManageCaCerts(admin, callerPackage);
            int userId = this.mInjector.userHandleGetCallingUserId();
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mCertificateMonitor.uninstallCaCerts(UserHandle.of(userId), aliases);
                synchronized (getLockObject()) {
                    if (getUserData(userId).mOwnerInstalledCaCerts.removeAll(Arrays.asList(aliases))) {
                        saveSettingsLocked(userId);
                    }
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public boolean installKeyPair(ComponentName who, String callerPackage, byte[] privKey, byte[] cert, byte[] chain, String alias, boolean requestAccess, boolean isUserSelectable) {
        RemoteException e;
        byte[] bArr;
        byte[] bArr2;
        byte[] bArr3;
        boolean z;
        Throwable th;
        InterruptedException e2;
        String str = alias;
        enforceCanManageScope(who, callerPackage, -1, "delegation-cert-install");
        int callingUid = this.mInjector.binderGetCallingUid();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            IKeyChainService keyChain;
            KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, UserHandle.getUserHandleForUid(callingUid));
            try {
                keyChain = keyChainConnection.getService();
            } catch (RemoteException e3) {
                e = e3;
                bArr = privKey;
                bArr2 = cert;
                bArr3 = chain;
                z = isUserSelectable;
                try {
                    Log.e(LOG_TAG, "Installing certificate", e);
                    keyChainConnection.close();
                    this.mInjector.binderRestoreCallingIdentity(id);
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    keyChainConnection.close();
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                bArr = privKey;
                bArr2 = cert;
                bArr3 = chain;
                z = isUserSelectable;
                keyChainConnection.close();
                throw th;
            }
            try {
                if (keyChain.installKeyPair(privKey, cert, chain, str)) {
                    if (requestAccess) {
                        keyChain.setGrant(callingUid, str, true);
                    }
                    try {
                        keyChain.setUserSelectable(str, isUserSelectable);
                    } catch (RemoteException e4) {
                        e = e4;
                    }
                    try {
                        keyChainConnection.close();
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return true;
                    } catch (InterruptedException e5) {
                        e2 = e5;
                        Log.w(LOG_TAG, "Interrupted while installing certificate", e2);
                        Thread.currentThread().interrupt();
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return false;
                    }
                }
                try {
                    keyChainConnection.close();
                    this.mInjector.binderRestoreCallingIdentity(id);
                    return false;
                } catch (InterruptedException e6) {
                    e2 = e6;
                    z = isUserSelectable;
                    try {
                        Log.w(LOG_TAG, "Interrupted while installing certificate", e2);
                        Thread.currentThread().interrupt();
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return false;
                    } catch (Throwable th4) {
                        th = th4;
                        this.mInjector.binderRestoreCallingIdentity(id);
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    z = isUserSelectable;
                    this.mInjector.binderRestoreCallingIdentity(id);
                    throw th;
                }
            } catch (RemoteException e7) {
                e = e7;
                z = isUserSelectable;
                Log.e(LOG_TAG, "Installing certificate", e);
                keyChainConnection.close();
                this.mInjector.binderRestoreCallingIdentity(id);
                return false;
            } catch (Throwable th6) {
                th = th6;
                z = isUserSelectable;
                keyChainConnection.close();
                throw th;
            }
        } catch (InterruptedException e8) {
            e2 = e8;
            bArr = privKey;
            bArr2 = cert;
            bArr3 = chain;
            z = isUserSelectable;
            Log.w(LOG_TAG, "Interrupted while installing certificate", e2);
            Thread.currentThread().interrupt();
            this.mInjector.binderRestoreCallingIdentity(id);
            return false;
        } catch (Throwable th7) {
            th = th7;
            bArr = privKey;
            bArr2 = cert;
            bArr3 = chain;
            z = isUserSelectable;
            this.mInjector.binderRestoreCallingIdentity(id);
            throw th;
        }
    }

    public boolean removeKeyPair(ComponentName who, String callerPackage, String alias) {
        enforceCanManageScope(who, callerPackage, -1, "delegation-cert-install");
        UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
        long id = Binder.clearCallingIdentity();
        KeyChainConnection keyChainConnection;
        try {
            keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
            try {
                boolean removeKeyPair = keyChainConnection.getService().removeKeyPair(alias);
                keyChainConnection.close();
                Binder.restoreCallingIdentity(id);
                return removeKeyPair;
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Removing keypair", e);
                keyChainConnection.close();
                Binder.restoreCallingIdentity(id);
                return false;
            }
        } catch (InterruptedException e2) {
            try {
                Log.w(LOG_TAG, "Interrupted while removing keypair", e2);
                Thread.currentThread().interrupt();
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(id);
            }
        } catch (Throwable th2) {
            keyChainConnection.close();
        }
    }

    private void enforceIsDeviceOwnerOrCertInstallerOfDeviceOwner(ComponentName who, String callerPackage, int callerUid) throws SecurityException {
        if (who != null) {
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(who, -2);
            }
        } else if (!this.mOwners.hasDeviceOwner()) {
            throw new SecurityException("Not in Device Owner mode.");
        } else if (UserHandle.getUserId(callerUid) != this.mOwners.getDeviceOwnerUserId()) {
            throw new SecurityException("Caller not from device owner user");
        } else if (!isCallerDelegate(callerPackage, "delegation-cert-install")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Caller with uid ");
            stringBuilder.append(this.mInjector.binderGetCallingUid());
            stringBuilder.append("has no permission to generate keys.");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    public static int[] translateIdAttestationFlags(int idAttestationFlags) {
        Map<Integer, Integer> idTypeToAttestationFlag = new HashMap();
        idTypeToAttestationFlag.put(Integer.valueOf(2), Integer.valueOf(1));
        idTypeToAttestationFlag.put(Integer.valueOf(4), Integer.valueOf(2));
        idTypeToAttestationFlag.put(Integer.valueOf(8), Integer.valueOf(3));
        int numFlagsSet = Integer.bitCount(idAttestationFlags);
        if (numFlagsSet == 0) {
            return null;
        }
        if ((idAttestationFlags & 1) != 0) {
            numFlagsSet--;
            idAttestationFlags &= -2;
        }
        int[] attestationUtilsFlags = new int[numFlagsSet];
        int i = 0;
        for (Integer idType : idTypeToAttestationFlag.keySet()) {
            if ((idType.intValue() & idAttestationFlags) != 0) {
                int i2 = i + 1;
                attestationUtilsFlags[i] = ((Integer) idTypeToAttestationFlag.get(idType)).intValue();
                i = i2;
            }
        }
        return attestationUtilsFlags;
    }

    /* JADX WARNING: Removed duplicated region for block: B:91:0x0139 A:{SYNTHETIC, Splitter:B:91:0x0139} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0130 A:{ExcHandler: Throwable (r0_22 'th' java.lang.Throwable), Splitter:B:25:0x0066} */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x0139 A:{SYNTHETIC, Splitter:B:91:0x0139} */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x0139 A:{SYNTHETIC, Splitter:B:91:0x0139} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:82:0x012b, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:83:0x012c, code skipped:
            r19 = r4;
            r2 = null;
     */
    /* JADX WARNING: Missing block: B:84:0x0130, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:85:0x0131, code skipped:
            r19 = r4;
            r14 = r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean generateKeyPair(ComponentName who, String callerPackage, String algorithm, ParcelableKeyGenParameterSpec parcelableKeySpec, int idAttestationFlags, KeymasterCertificateChain attestationChain) {
        RemoteException e;
        int[] iArr;
        InterruptedException e2;
        Throwable th;
        Throwable th2;
        Throwable th3;
        ComponentName componentName = who;
        String str = callerPackage;
        int[] attestationUtilsFlags = translateIdAttestationFlags(idAttestationFlags);
        boolean deviceIdAttestationRequired = attestationUtilsFlags != null;
        int callingUid = this.mInjector.binderGetCallingUid();
        if (!deviceIdAttestationRequired || attestationUtilsFlags.length <= 0) {
            enforceCanManageScope(componentName, str, -1, "delegation-cert-install");
        } else {
            enforceIsDeviceOwnerOrCertInstallerOfDeviceOwner(componentName, str, callingUid);
        }
        KeyGenParameterSpec keySpec = parcelableKeySpec.getSpec();
        String alias = keySpec.getKeystoreAlias();
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("Empty alias provided.");
        } else if (keySpec.getUid() != -1) {
            Log.e(LOG_TAG, "Only the caller can be granted access to the generated keypair.");
            return false;
        } else if (deviceIdAttestationRequired && keySpec.getAttestationChallenge() == null) {
            throw new IllegalArgumentException("Requested Device ID attestation but challenge is empty.");
        } else {
            UserHandle userHandle = this.mInjector.binderGetCallingUserHandle();
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
                try {
                    IKeyChainService keyChain = keyChainConnection.getService();
                    if (keyChain.generateKeyPair(algorithm, new ParcelableKeyGenParameterSpec(new Builder(keySpec).setAttestationChallenge(null).build())) != 0) {
                        try {
                            Log.e(LOG_TAG, String.format("KeyChain failed to generate a keypair, error %d.", new Object[]{Integer.valueOf(keyChain.generateKeyPair(algorithm, new ParcelableKeyGenParameterSpec(new Builder(keySpec).setAttestationChallenge(null).build())))}));
                            if (keyChainConnection != null) {
                                try {
                                    $closeResource(null, keyChainConnection);
                                } catch (RemoteException e3) {
                                    e = e3;
                                    iArr = attestationUtilsFlags;
                                } catch (InterruptedException e4) {
                                    e2 = e4;
                                    iArr = attestationUtilsFlags;
                                    Log.w(LOG_TAG, "Interrupted while generating keypair", e2);
                                    Thread.currentThread().interrupt();
                                    this.mInjector.binderRestoreCallingIdentity(id);
                                    return false;
                                } catch (Throwable th4) {
                                    th = th4;
                                    iArr = attestationUtilsFlags;
                                    this.mInjector.binderRestoreCallingIdentity(id);
                                    throw th;
                                }
                            }
                            this.mInjector.binderRestoreCallingIdentity(id);
                            return false;
                        } catch (Throwable th5) {
                            th = th5;
                            th3 = null;
                            if (keyChainConnection != null) {
                            }
                            throw th;
                        }
                    }
                    keyChain.setGrant(callingUid, alias, true);
                    byte[] attestationChallenge = keySpec.getAttestationChallenge();
                    if (attestationChallenge != null) {
                        int attestationResult = keyChain.attestKey(alias, attestationChallenge, attestationUtilsFlags, attestationChain);
                        if (attestationResult != 0) {
                            iArr = attestationUtilsFlags;
                            try {
                                Log.e(LOG_TAG, String.format("Attestation for %s failed (rc=%d), deleting key.", new Object[]{alias, Integer.valueOf(attestationResult)}));
                                keyChain.removeKeyPair(alias);
                                if (attestationResult != 3) {
                                    if (keyChainConnection != null) {
                                        try {
                                            $closeResource(null, keyChainConnection);
                                        } catch (RemoteException e5) {
                                            e = e5;
                                            Log.e(LOG_TAG, "KeyChain error while generating a keypair", e);
                                            this.mInjector.binderRestoreCallingIdentity(id);
                                            return false;
                                        } catch (InterruptedException e6) {
                                            e2 = e6;
                                            Log.w(LOG_TAG, "Interrupted while generating keypair", e2);
                                            Thread.currentThread().interrupt();
                                            this.mInjector.binderRestoreCallingIdentity(id);
                                            return false;
                                        }
                                    }
                                    this.mInjector.binderRestoreCallingIdentity(id);
                                    return false;
                                }
                                throw new UnsupportedOperationException("Device does not support Device ID attestation.");
                            } catch (Throwable th6) {
                                th = th6;
                                th3 = null;
                                if (keyChainConnection != null) {
                                    $closeResource(th3, keyChainConnection);
                                }
                                throw th;
                            }
                        }
                    }
                    byte[] bArr = attestationChallenge;
                    iArr = attestationUtilsFlags;
                    if (keyChainConnection != null) {
                        $closeResource(null, keyChainConnection);
                    }
                    this.mInjector.binderRestoreCallingIdentity(id);
                    return true;
                } catch (Throwable th7) {
                    th = th7;
                    iArr = attestationUtilsFlags;
                    th3 = null;
                    if (keyChainConnection != null) {
                    }
                    throw th;
                }
            } catch (RemoteException e7) {
                e = e7;
                iArr = attestationUtilsFlags;
                Log.e(LOG_TAG, "KeyChain error while generating a keypair", e);
                this.mInjector.binderRestoreCallingIdentity(id);
                return false;
            } catch (InterruptedException e8) {
                e2 = e8;
                iArr = attestationUtilsFlags;
                Log.w(LOG_TAG, "Interrupted while generating keypair", e2);
                Thread.currentThread().interrupt();
                this.mInjector.binderRestoreCallingIdentity(id);
                return false;
            } catch (Throwable th8) {
                th = th8;
                this.mInjector.binderRestoreCallingIdentity(id);
                throw th;
            }
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    public boolean setKeyPairCertificate(ComponentName who, String callerPackage, String alias, byte[] cert, byte[] chain, boolean isUserSelectable) {
        enforceCanManageScope(who, callerPackage, -1, "delegation-cert-install");
        int callingUid = this.mInjector.binderGetCallingUid();
        long id = this.mInjector.binderClearCallingIdentity();
        KeyChainConnection keyChainConnection;
        try {
            keyChainConnection = KeyChain.bindAsUser(this.mContext, UserHandle.getUserHandleForUid(callingUid));
            IKeyChainService keyChain = keyChainConnection.getService();
            if (keyChain.setKeyPairCertificate(alias, cert, chain)) {
                keyChain.setUserSelectable(alias, isUserSelectable);
                if (keyChainConnection != null) {
                    $closeResource(null, keyChainConnection);
                }
                this.mInjector.binderRestoreCallingIdentity(id);
                return true;
            }
            if (keyChainConnection != null) {
                $closeResource(null, keyChainConnection);
            }
            this.mInjector.binderRestoreCallingIdentity(id);
            return false;
        } catch (InterruptedException e) {
            Log.w(LOG_TAG, "Interrupted while setting keypair certificate", e);
            Thread.currentThread().interrupt();
            this.mInjector.binderRestoreCallingIdentity(id);
            return false;
        } catch (RemoteException e2) {
            try {
                Log.e(LOG_TAG, "Failed setting keypair certificate", e2);
                this.mInjector.binderRestoreCallingIdentity(id);
                return false;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        } catch (Throwable th2) {
            if (keyChainConnection != null) {
                $closeResource(r5, keyChainConnection);
            }
        }
    }

    public void choosePrivateKeyAlias(int uid, Uri uri, String alias, IBinder response) {
        Throwable th;
        long id;
        final IBinder iBinder = response;
        if (isCallerWithSystemUid()) {
            UserHandle caller = this.mInjector.binderGetCallingUserHandle();
            ComponentName aliasChooser = getProfileOwner(caller.getIdentifier());
            if (aliasChooser == null && caller.isSystem()) {
                synchronized (getLockObject()) {
                    ActiveAdmin deviceOwnerAdmin = getDeviceOwnerAdminLocked();
                    if (deviceOwnerAdmin != null) {
                        aliasChooser = deviceOwnerAdmin.info.getComponent();
                    }
                }
            }
            ComponentName aliasChooser2 = aliasChooser;
            if (aliasChooser2 == null) {
                sendPrivateKeyAliasResponse(null, iBinder);
                return;
            }
            Intent intent = new Intent("android.app.action.CHOOSE_PRIVATE_KEY_ALIAS");
            intent.setComponent(aliasChooser2);
            intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_SENDER_UID", uid);
            intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_URI", uri);
            intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_ALIAS", alias);
            intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_RESPONSE", iBinder);
            intent.addFlags(268435456);
            long id2 = this.mInjector.binderClearCallingIdentity();
            try {
                long id3 = id2;
                try {
                    this.mContext.sendOrderedBroadcastAsUser(intent, caller, null, new BroadcastReceiver() {
                        public void onReceive(Context context, Intent intent) {
                            DevicePolicyManagerService.this.sendPrivateKeyAliasResponse(getResultData(), iBinder);
                        }
                    }, null, -1, null, null);
                    this.mInjector.binderRestoreCallingIdentity(id3);
                } catch (Throwable th2) {
                    th = th2;
                    id = id3;
                    this.mInjector.binderRestoreCallingIdentity(id);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                id = id2;
                this.mInjector.binderRestoreCallingIdentity(id);
                throw th;
            }
        }
    }

    private void sendPrivateKeyAliasResponse(String alias, IBinder responseBinder) {
        try {
            IKeyChainAliasCallback.Stub.asInterface(responseBinder).alias(alias);
        } catch (Exception e) {
            Log.e(LOG_TAG, "error while responding to callback", e);
        }
    }

    private static boolean shouldCheckIfDelegatePackageIsInstalled(String delegatePackage, int targetSdk, List<String> scopes) {
        if (targetSdk >= 24) {
            return true;
        }
        if ((scopes.size() == 1 && ((String) scopes.get(0)).equals("delegation-cert-install")) || scopes.isEmpty()) {
            return false;
        }
        return true;
    }

    public void setDelegatedScopes(ComponentName who, String delegatePackage, List<String> scopes) throws SecurityException {
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkStringNotEmpty(delegatePackage, "Delegate package is null or empty");
        Preconditions.checkCollectionElementsNotNull(scopes, "Scopes");
        ArrayList scopes2 = new ArrayList(new ArraySet(scopes));
        if (scopes2.retainAll(Arrays.asList(DELEGATIONS))) {
            throw new IllegalArgumentException("Unexpected delegation scopes");
        }
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            if (shouldCheckIfDelegatePackageIsInstalled(delegatePackage, getTargetSdk(who.getPackageName(), userId), scopes2)) {
                if (!isPackageInstalledForUser(delegatePackage, userId)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Package ");
                    stringBuilder.append(delegatePackage);
                    stringBuilder.append(" is not installed on the current user");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            DevicePolicyData policy = getUserData(userId);
            if (scopes2.isEmpty()) {
                policy.mDelegationMap.remove(delegatePackage);
            } else {
                policy.mDelegationMap.put(delegatePackage, new ArrayList(scopes2));
            }
            Intent intent = new Intent("android.app.action.APPLICATION_DELEGATION_SCOPES_CHANGED");
            intent.addFlags(1073741824);
            intent.setPackage(delegatePackage);
            intent.putStringArrayListExtra("android.app.extra.DELEGATION_SCOPES", scopes2);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
            saveSettingsLocked(userId);
        }
    }

    public List<String> getDelegatedScopes(ComponentName who, String delegatePackage) throws SecurityException {
        List<String> list;
        Preconditions.checkNotNull(delegatePackage, "Delegate package is null");
        int callingUid = this.mInjector.binderGetCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        synchronized (getLockObject()) {
            if (who != null) {
                getActiveAdminForCallerLocked(who, -1);
            } else {
                int uid = 0;
                try {
                    uid = this.mInjector.getPackageManager().getPackageUidAsUser(delegatePackage, userId);
                } catch (NameNotFoundException e) {
                }
                if (uid != callingUid) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Caller with uid ");
                    stringBuilder.append(callingUid);
                    stringBuilder.append(" is not ");
                    stringBuilder.append(delegatePackage);
                    throw new SecurityException(stringBuilder.toString());
                }
            }
            List<String> scopes = (List) getUserData(userId).mDelegationMap.get(delegatePackage);
            list = scopes == null ? Collections.EMPTY_LIST : scopes;
        }
        return list;
    }

    public List<String> getDelegatePackages(ComponentName who, String scope) throws SecurityException {
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkNotNull(scope, "Scope is null");
        if (Arrays.asList(DELEGATIONS).contains(scope)) {
            List<String> delegatePackagesWithScope;
            int userId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(who, -1);
                DevicePolicyData policy = getUserData(userId);
                delegatePackagesWithScope = new ArrayList();
                for (int i = 0; i < policy.mDelegationMap.size(); i++) {
                    if (((List) policy.mDelegationMap.valueAt(i)).contains(scope)) {
                        delegatePackagesWithScope.add((String) policy.mDelegationMap.keyAt(i));
                    }
                }
            }
            return delegatePackagesWithScope;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected delegation scope: ");
        stringBuilder.append(scope);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private boolean isCallerDelegate(String callerPackage, String scope) {
        Preconditions.checkNotNull(callerPackage, "callerPackage is null");
        if (Arrays.asList(DELEGATIONS).contains(scope)) {
            int callingUid = this.mInjector.binderGetCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            synchronized (getLockObject()) {
                List<String> scopes = (List) getUserData(userId).mDelegationMap.get(callerPackage);
                boolean z = false;
                if (scopes != null && scopes.contains(scope)) {
                    try {
                        if (this.mInjector.getPackageManager().getPackageUidAsUser(callerPackage, userId) == callingUid) {
                            z = true;
                        }
                        return z;
                    } catch (NameNotFoundException e) {
                        return false;
                    }
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected delegation scope: ");
        stringBuilder.append(scope);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void enforceCanManageScope(ComponentName who, String callerPackage, int reqPolicy, String scope) {
        if (who != null) {
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(who, reqPolicy);
            }
        } else if (!isCallerDelegate(callerPackage, scope)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Caller with uid ");
            stringBuilder.append(this.mInjector.binderGetCallingUid());
            stringBuilder.append(" is not a delegate of scope ");
            stringBuilder.append(scope);
            stringBuilder.append(".");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void setDelegatedScopePreO(ComponentName who, String delegatePackage, String scope) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            DevicePolicyData policy = getUserData(userId);
            if (delegatePackage != null) {
                List<String> scopes = (List) policy.mDelegationMap.get(delegatePackage);
                if (scopes == null) {
                    scopes = new ArrayList();
                }
                if (!scopes.contains(scope)) {
                    scopes.add(scope);
                    setDelegatedScopes(who, delegatePackage, scopes);
                }
            }
            for (int i = 0; i < policy.mDelegationMap.size(); i++) {
                String currentPackage = (String) policy.mDelegationMap.keyAt(i);
                List<String> currentScopes = (List) policy.mDelegationMap.valueAt(i);
                if (!currentPackage.equals(delegatePackage) && currentScopes.contains(scope)) {
                    List<String> newScopes = new ArrayList(currentScopes);
                    newScopes.remove(scope);
                    setDelegatedScopes(who, currentPackage, newScopes);
                }
            }
        }
    }

    public void setCertInstallerPackage(ComponentName who, String installerPackage) throws SecurityException {
        setDelegatedScopePreO(who, installerPackage, "delegation-cert-install");
    }

    public String getCertInstallerPackage(ComponentName who) throws SecurityException {
        List<String> delegatePackages = getDelegatePackages(who, "delegation-cert-install");
        return delegatePackages.size() > 0 ? (String) delegatePackages.get(0) : null;
    }

    public boolean setAlwaysOnVpnPackage(ComponentName admin, String vpnPackage, boolean lockdown) throws SecurityException {
        enforceProfileOrDeviceOwner(admin);
        int userId = this.mInjector.userHandleGetCallingUserId();
        long token = this.mInjector.binderClearCallingIdentity();
        if (vpnPackage != null) {
            try {
                if (!isPackageInstalledForUser(vpnPackage, userId)) {
                    return false;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(token);
            }
        }
        if (((ConnectivityManager) this.mContext.getSystemService("connectivity")).setAlwaysOnVpnPackageForUser(userId, vpnPackage, lockdown)) {
            this.mInjector.binderRestoreCallingIdentity(token);
            return true;
        }
        throw new UnsupportedOperationException();
    }

    public String getAlwaysOnVpnPackage(ComponentName admin) throws SecurityException {
        enforceProfileOrDeviceOwner(admin);
        int userId = this.mInjector.userHandleGetCallingUserId();
        long token = this.mInjector.binderClearCallingIdentity();
        String alwaysOnVpnPackageForUser;
        try {
            alwaysOnVpnPackageForUser = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getAlwaysOnVpnPackageForUser(userId);
            return alwaysOnVpnPackageForUser;
        } finally {
            alwaysOnVpnPackageForUser = this.mInjector;
            alwaysOnVpnPackageForUser.binderRestoreCallingIdentity(token);
        }
    }

    private void forceWipeDeviceNoLock(boolean wipeExtRequested, String reason, boolean wipeEuicc) {
        wtfIfInLock();
        if (wipeExtRequested) {
            ((StorageManager) this.mContext.getSystemService("storage")).wipeAdoptableDisks();
        }
        clearWipeDataFactoryLowlevel(reason, wipeEuicc);
    }

    private void forceWipeUser(int userId, String wipeReasonForUser) {
        boolean success = false;
        try {
            IActivityManager am = this.mInjector.getIActivityManager();
            if (am.getCurrentUser().id == userId) {
                am.switchUser(0);
            }
            success = this.mUserManagerInternal.removeUserEvenWhenDisallowed(userId);
            if (!success) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't remove user ");
                stringBuilder.append(userId);
                Slog.w(str, stringBuilder.toString());
            } else if (isManagedProfile(userId)) {
                sendWipeProfileNotification(wipeReasonForUser);
            }
            if (success) {
                return;
            }
        } catch (RemoteException e) {
            if (success) {
                return;
            }
        } catch (Throwable th) {
            if (!success) {
                SecurityLog.writeEvent(210023, new Object[0]);
            }
        }
        SecurityLog.writeEvent(210023, new Object[0]);
    }

    public void wipeDataWithReason(int flags, String wipeReasonForUser) {
        if (this.mHasFeature) {
            ActiveAdmin admin;
            Preconditions.checkStringNotEmpty(wipeReasonForUser, "wipeReasonForUser is null or empty");
            enforceFullCrossUsersPermission(this.mInjector.userHandleGetCallingUserId());
            synchronized (getLockObject()) {
                admin = getActiveAdminForCallerLocked(null, 4);
            }
            String internalReason = new StringBuilder();
            internalReason.append("DevicePolicyManager.wipeDataWithReason() from ");
            internalReason.append(admin.info.getComponent().flattenToShortString());
            wipeDataNoLock(admin.info.getComponent(), flags, internalReason.toString(), wipeReasonForUser, admin.getUserHandle().getIdentifier());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:34:0x006e A:{SYNTHETIC, Splitter:B:34:0x006e} */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0025 A:{Catch:{ all -> 0x000f }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void wipeDataNoLock(ComponentName admin, int flags, String internalReason, String wipeReasonForUser, int userId) {
        String restriction;
        wtfIfInLock();
        long ident = this.mInjector.binderClearCallingIdentity();
        if (userId == 0) {
            try {
                restriction = "no_factory_reset";
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        } else if (isManagedProfile(userId)) {
            restriction = "no_remove_managed_profile";
        } else {
            restriction = "no_remove_user";
            if (isAdminAffectedByRestriction(admin, restriction, userId)) {
                monitorFactoryReset(admin.flattenToShortString(), internalReason);
                if ((flags & 2) != 0) {
                    if (isDeviceOwner(admin, userId)) {
                        PersistentDataBlockManager manager = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
                        if (manager != null) {
                            manager.wipe();
                        }
                    } else {
                        throw new SecurityException("Only device owner admins can set WIPE_RESET_PROTECTION_DATA");
                    }
                }
                if (userId == 0) {
                    boolean z = false;
                    boolean z2 = (flags & 1) != 0;
                    if ((flags & 4) != 0) {
                        z = true;
                    }
                    forceWipeDeviceNoLock(z2, internalReason, z);
                } else {
                    forceWipeUser(userId, wipeReasonForUser);
                }
                this.mInjector.binderRestoreCallingIdentity(ident);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot wipe data. ");
            stringBuilder.append(restriction);
            stringBuilder.append(" restriction is set for user ");
            stringBuilder.append(userId);
            throw new SecurityException(stringBuilder.toString());
        }
        if (isAdminAffectedByRestriction(admin, restriction, userId)) {
        }
    }

    private void sendWipeProfileNotification(String wipeReasonForUser) {
        this.mInjector.getNotificationManager().notify(NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE, new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17301642).setContentTitle(this.mContext.getString(17041424)).setContentText(wipeReasonForUser).setColor(this.mContext.getColor(17170784)).setStyle(new BigTextStyle().bigText(wipeReasonForUser)).build());
    }

    private void clearWipeProfileNotification() {
        this.mInjector.getNotificationManager().cancel(NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE);
    }

    public void getRemoveWarning(ComponentName comp, RemoteCallback result, int userHandle) {
        final RemoteCallback remoteCallback = result;
        int i = userHandle;
        if (this.mHasFeature) {
            enforceFullCrossUsersPermission(i);
            this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(comp, i);
                if (admin == null) {
                    remoteCallback.sendResult(null);
                    return;
                }
                Intent intent = new Intent("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED");
                intent.setFlags(268435456);
                intent.setComponent(admin.info.getComponent());
                this.mContext.sendOrderedBroadcastAsUser(intent, new UserHandle(i), null, new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        remoteCallback.sendResult(getResultExtras(false));
                    }
                }, null, -1, null, null);
            }
        }
    }

    public void setActivePasswordState(PasswordMetrics metrics, int userHandle) {
        if (this.mHasFeature) {
            enforceFullCrossUsersPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
            if (isManagedProfile(userHandle) && !isSeparateProfileChallengeEnabled(userHandle)) {
                metrics = new PasswordMetrics();
            }
            validateQualityConstant(metrics.quality);
            synchronized (getLockObject()) {
                this.mUserPasswordMetrics.put(userHandle, metrics);
            }
        }
    }

    public void reportPasswordChanged(int userId) {
        if (this.mHasFeature) {
            enforceFullCrossUsersPermission(userId);
            if (!isSeparateProfileChallengeEnabled(userId)) {
                enforceNotManagedProfile(userId, "set the active password");
            }
            this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
            DevicePolicyData policy = getUserData(userId);
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                synchronized (getLockObject()) {
                    policy.mFailedPasswordAttempts = 0;
                    updatePasswordValidityCheckpointLocked(userId, false);
                    saveSettingsLocked(userId);
                    updatePasswordExpirationsLocked(userId);
                    setExpirationAlarmCheckLocked(this.mContext, userId, false);
                    sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_CHANGED", 0, userId);
                }
                removeCaApprovalsIfNeeded(userId);
                this.mInjector.binderRestoreCallingIdentity(ident);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    private void updatePasswordExpirationsLocked(int userHandle) {
        ArraySet<Integer> affectedUserIds = new ArraySet();
        int i = 0;
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, false);
        int N = admins.size();
        while (i < N) {
            ActiveAdmin admin = (ActiveAdmin) admins.get(i);
            if (admin.info.usesPolicy(6)) {
                affectedUserIds.add(Integer.valueOf(admin.getUserHandle().getIdentifier()));
                long timeout = admin.passwordExpirationTimeout;
                long expiration = 0;
                if (timeout > 0) {
                    expiration = System.currentTimeMillis() + timeout;
                }
                admin.passwordExpirationDate = expiration;
            }
            i++;
        }
        Iterator it = affectedUserIds.iterator();
        while (it.hasNext()) {
            saveSettingsLocked(((Integer) it.next()).intValue());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:72:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x014f  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x014f  */
    /* JADX WARNING: Removed duplicated region for block: B:72:? A:{SYNTHETIC, RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reportFailedPasswordAttempt(int userHandle) {
        int i;
        int failedAttempts;
        boolean wipeData;
        Throwable th;
        SecurityException e;
        String str;
        int userId;
        HwCustDevicePolicyManagerService hwCustDevicePolicyManagerService;
        String str2;
        StringBuilder stringBuilder;
        int i2 = userHandle;
        enforceFullCrossUsersPermission(userHandle);
        if (!isSeparateProfileChallengeEnabled(userHandle)) {
            enforceNotManagedProfile(i2, "report failed password attempt if separate profile challenge is not in place");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        boolean wipeData2 = false;
        ActiveAdmin strictestAdmin = null;
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            int i3;
            ActiveAdmin strictestAdmin2;
            synchronized (getLockObject()) {
                try {
                    DevicePolicyData policy = getUserData(userHandle);
                    i = 1;
                    policy.mFailedPasswordAttempts++;
                    saveSettingsLocked(userHandle);
                    failedAttempts = policy.mFailedPasswordAttempts;
                    try {
                        int max;
                        i3 = 0;
                        if (this.mHasFeature) {
                            strictestAdmin = getAdminWithMinimumFailedPasswordsForWipeLocked(i2, false);
                            max = strictestAdmin != null ? strictestAdmin.maximumFailedPasswordsForWipe : 0;
                            if (max > 0 && policy.mFailedPasswordAttempts >= max) {
                                wipeData2 = true;
                            }
                            sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_FAILED", 1, i2);
                        }
                        wipeData = wipeData2;
                        strictestAdmin2 = strictestAdmin;
                        try {
                        } catch (Throwable th2) {
                            th = th2;
                            max = failedAttempts;
                            wipeData2 = wipeData;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    throw th;
                }
            }
            HwCustDevicePolicyManagerService mHwCustRecoverySystem = (HwCustDevicePolicyManagerService) HwCustUtils.createObj(HwCustDevicePolicyManagerService.class, new Object[i3]);
            if (mHwCustRecoverySystem != null && mHwCustRecoverySystem.isAttEraseDataOn(this.mContext)) {
                mHwCustRecoverySystem.isStartEraseAllDataForAtt(this.mContext, failedAttempts);
            }
            if (!wipeData || strictestAdmin2 == null) {
            } else {
                int userId2 = strictestAdmin2.getUserHandle().getIdentifier();
                String str3 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Max failed password attempts policy reached for admin: ");
                stringBuilder2.append(strictestAdmin2.info.getComponent().flattenToShortString());
                stringBuilder2.append(". Calling wipeData for user ");
                stringBuilder2.append(userId2);
                Slog.i(str3, stringBuilder2.toString());
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("WipeData for max failed PWD reached:");
                stringBuilder3.append(strictestAdmin2.maximumFailedPasswordsForWipe);
                String reason = stringBuilder3.toString();
                monitorFactoryReset(strictestAdmin2.info.getComponent().flattenToShortString(), reason);
                try {
                    String wipeReasonForUser = this.mContext.getString(17041428);
                    boolean isCustWipeData = false;
                    if (i2 == 0 && mHwCustRecoverySystem != null) {
                        try {
                            if (mHwCustRecoverySystem.eraseStorageForEAS(this.mContext) || mHwCustRecoverySystem.wipeDataAndReset(this.mContext)) {
                                isCustWipeData = true;
                                Slog.d(LOG_TAG, "Successed wipe storage data.");
                            }
                        } catch (SecurityException e2) {
                            e = e2;
                            str = reason;
                            userId = userId2;
                            hwCustDevicePolicyManagerService = mHwCustRecoverySystem;
                            str2 = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to wipe user ");
                            stringBuilder.append(userId);
                            stringBuilder.append(" after max failed password attempts reached.");
                            Slog.w(str2, stringBuilder.toString(), e);
                            if (this.mInjector.securityLogIsLoggingEnabled()) {
                                SecurityLog.writeEvent(210007, new Object[]{Integer.valueOf(i3), Integer.valueOf(i)});
                                return;
                            }
                            return;
                        }
                    }
                    if (isCustWipeData) {
                        userId = userId2;
                        hwCustDevicePolicyManagerService = mHwCustRecoverySystem;
                    } else {
                        userId = userId2;
                        try {
                            wipeDataNoLock(strictestAdmin2.info.getComponent(), 0, reason, wipeReasonForUser, userId);
                        } catch (SecurityException e3) {
                            e = e3;
                        }
                    }
                } catch (SecurityException e4) {
                    e = e4;
                    str = reason;
                    userId = userId2;
                    hwCustDevicePolicyManagerService = mHwCustRecoverySystem;
                    str2 = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to wipe user ");
                    stringBuilder.append(userId);
                    stringBuilder.append(" after max failed password attempts reached.");
                    Slog.w(str2, stringBuilder.toString(), e);
                    if (this.mInjector.securityLogIsLoggingEnabled()) {
                    }
                }
            }
            if (this.mInjector.securityLogIsLoggingEnabled()) {
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void reportSuccessfulPasswordAttempt(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userHandle);
            int i = 0;
            if (policy.mFailedPasswordAttempts != 0 || policy.mPasswordOwner >= 0) {
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    policy.mFailedPasswordAttempts = 0;
                    policy.mPasswordOwner = -1;
                    saveSettingsLocked(userHandle);
                    if (this.mHasFeature) {
                        sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_SUCCEEDED", 1, userHandle);
                    }
                } finally {
                    i = this.mInjector;
                    i.binderRestoreCallingIdentity(ident);
                }
            }
        }
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{Integer.valueOf(1), Integer.valueOf(1)});
        }
    }

    public void reportFailedFingerprintAttempt(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{Integer.valueOf(0), Integer.valueOf(0)});
        }
    }

    public void reportSuccessfulFingerprintAttempt(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{Integer.valueOf(1), Integer.valueOf(0)});
        }
    }

    public void reportKeyguardDismissed(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210006, new Object[0]);
        }
    }

    public void reportKeyguardSecured(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210008, new Object[0]);
        }
    }

    public ComponentName setGlobalProxy(ComponentName who, String proxySpec, String exclusionList) {
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (getLockObject()) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            DevicePolicyData policy = getUserData(0);
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, 5);
            for (ComponentName component : policy.mAdminMap.keySet()) {
                if (((ActiveAdmin) policy.mAdminMap.get(component)).specifiesGlobalProxy && !component.equals(who)) {
                    return component;
                }
            }
            if (UserHandle.getCallingUserId() != 0) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Only the owner is allowed to set the global proxy. User ");
                stringBuilder.append(UserHandle.getCallingUserId());
                stringBuilder.append(" is not permitted.");
                Slog.w(str, stringBuilder.toString());
                return null;
            }
            if (proxySpec == null) {
                admin.specifiesGlobalProxy = false;
                admin.globalProxySpec = null;
                admin.globalProxyExclusionList = null;
            } else {
                admin.specifiesGlobalProxy = true;
                admin.globalProxySpec = proxySpec;
                admin.globalProxyExclusionList = exclusionList;
            }
            long origId = this.mInjector.binderClearCallingIdentity();
            try {
                resetGlobalProxyLocked(policy);
                return null;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(origId);
            }
        }
    }

    public ComponentName getGlobalProxyAdmin(int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            int i = 0;
            DevicePolicyData policy = getUserData(0);
            int N = policy.mAdminList.size();
            while (i < N) {
                ActiveAdmin ap = (ActiveAdmin) policy.mAdminList.get(i);
                if (ap.specifiesGlobalProxy) {
                    ComponentName component = ap.info.getComponent();
                    return component;
                }
                i++;
            }
            return null;
        }
    }

    public void setRecommendedGlobalProxy(ComponentName who, ProxyInfo proxyInfo) {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            ((ConnectivityManager) this.mContext.getSystemService("connectivity")).setGlobalProxy(proxyInfo);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    private void resetGlobalProxyLocked(DevicePolicyData policy) {
        int N = policy.mAdminList.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin ap = (ActiveAdmin) policy.mAdminList.get(i);
            if (ap.specifiesGlobalProxy) {
                saveGlobalProxyLocked(ap.globalProxySpec, ap.globalProxyExclusionList);
                return;
            }
        }
        saveGlobalProxyLocked(null, null);
    }

    private void saveGlobalProxyLocked(String proxySpec, String exclusionList) {
        if (exclusionList == null) {
            exclusionList = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        if (proxySpec == null) {
            proxySpec = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String[] data = proxySpec.trim().split(":");
        int proxyPort = 8080;
        if (data.length > 1) {
            try {
                proxyPort = Integer.parseInt(data[1]);
            } catch (NumberFormatException e) {
            }
        }
        exclusionList = exclusionList.trim();
        ProxyInfo proxyProperties = new ProxyInfo(data[0], proxyPort, exclusionList);
        if (proxyProperties.isValid()) {
            this.mInjector.settingsGlobalPutString("global_http_proxy_host", data[0]);
            this.mInjector.settingsGlobalPutInt("global_http_proxy_port", proxyPort);
            this.mInjector.settingsGlobalPutString("global_http_proxy_exclusion_list", exclusionList);
            return;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid proxy properties, ignoring: ");
        stringBuilder.append(proxyProperties.toString());
        Slog.e(str, stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:27:0x0074, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int setStorageEncryption(ComponentName who, boolean encrypt) {
        int i = 0;
        if (!this.mHasFeature) {
            return 0;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            if (userHandle != 0) {
                try {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Only owner/system user is allowed to set storage encryption. User ");
                    stringBuilder.append(UserHandle.getCallingUserId());
                    stringBuilder.append(" is not permitted.");
                    Slog.w(str, stringBuilder.toString());
                    return 0;
                } catch (Throwable th) {
                }
            } else {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 7);
                if (isEncryptionSupported()) {
                    if (ap.encryptionRequested != encrypt) {
                        ap.encryptionRequested = encrypt;
                        saveSettingsLocked(userHandle);
                    }
                    DevicePolicyData policy = getUserData(0);
                    boolean newRequested = false;
                    while (i < policy.mAdminList.size()) {
                        newRequested |= ((ActiveAdmin) policy.mAdminList.get(i)).encryptionRequested;
                        i++;
                    }
                    setEncryptionRequested(newRequested);
                    if (newRequested) {
                        i = 3;
                    } else {
                        i = 1;
                    }
                } else {
                    return 0;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001a, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getStorageEncryption(ComponentName who, int userHandle) {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    ActiveAdmin ap = getActiveAdminUncheckedLocked(who, userHandle);
                    if (ap != null) {
                        z = ap.encryptionRequested;
                    }
                } catch (Throwable th) {
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    if (((ActiveAdmin) policy.mAdminList.get(i)).encryptionRequested) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public int getStorageEncryptionStatus(String callerPackage, int userHandle) {
        boolean z = this.mHasFeature;
        enforceFullCrossUsersPermission(userHandle);
        ensureCallerPackage(callerPackage);
        try {
            boolean legacyApp = false;
            if (this.mIPackageManager.getApplicationInfo(callerPackage, 0, userHandle).targetSdkVersion <= 23) {
                legacyApp = true;
            }
            int rawStatus = getEncryptionStatus();
            if (rawStatus == 5 && legacyApp) {
                return 3;
            }
            return rawStatus;
        } catch (RemoteException e) {
            throw new SecurityException(e);
        }
    }

    private boolean isEncryptionSupported() {
        return getEncryptionStatus() != 0;
    }

    private int getEncryptionStatus() {
        if (this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
            return 5;
        }
        if (this.mInjector.storageManagerIsNonDefaultBlockEncrypted()) {
            return 3;
        }
        if (isSecureBlockEncrypted()) {
            Log.i(LOG_TAG, "return ENCRYPTION_STATUS_ACTIVE for old products upgrade to N with secure block encryption.");
            return 3;
        } else if (this.mInjector.storageManagerIsEncrypted()) {
            return 4;
        } else {
            if (this.mInjector.storageManagerIsEncryptable()) {
                return 1;
            }
            return 0;
        }
    }

    private void setEncryptionRequested(boolean encrypt) {
    }

    public void setScreenCaptureDisabled(ComponentName who, boolean disabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = UserHandle.getCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, -1);
                if (ap.disableScreenCapture != disabled) {
                    ap.disableScreenCapture = disabled;
                    saveSettingsLocked(userHandle);
                    updateScreenCaptureDisabled(userHandle, disabled);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0017, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getScreenCaptureDisabled(ComponentName who, int userHandle) {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                    if (admin != null) {
                        z = admin.disableScreenCapture;
                    }
                } catch (Throwable th) {
                }
            } else {
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    if (((ActiveAdmin) policy.mAdminList.get(i)).disableScreenCapture) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private void updateScreenCaptureDisabled(final int userHandle, boolean disabled) {
        this.mPolicyCache.setScreenCaptureDisabled(userHandle, disabled);
        this.mHandler.post(new Runnable() {
            public void run() {
                try {
                    DevicePolicyManagerService.this.mInjector.getIWindowManager().refreshScreenCaptureDisabled(userHandle);
                } catch (RemoteException e) {
                    Log.w(DevicePolicyManagerService.LOG_TAG, "Unable to notify WindowManager.", e);
                }
            }
        });
    }

    public void setAutoTimeRequired(ComponentName who, boolean required) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = UserHandle.getCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
                if (admin.requireAutoTime != required) {
                    admin.requireAutoTime = required;
                    saveSettingsLocked(userHandle);
                }
            }
            if (required) {
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    this.mInjector.settingsGlobalPutInt("auto_time", 1);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            }
        }
    }

    public boolean getAutoTimeRequired() {
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            if (deviceOwner == null || !deviceOwner.requireAutoTime) {
                for (Integer userId : this.mOwners.getProfileOwnerKeys()) {
                    ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userId.intValue());
                    if (profileOwner != null && profileOwner.requireAutoTime) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
    }

    public void setForceEphemeralUsers(ComponentName who, boolean forceEphemeralUsers) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            if (!forceEphemeralUsers || this.mInjector.userManagerIsSplitSystemUser()) {
                boolean removeAllUsers = false;
                synchronized (getLockObject()) {
                    ActiveAdmin deviceOwner = getActiveAdminForCallerLocked(who, -2);
                    if (deviceOwner.forceEphemeralUsers != forceEphemeralUsers) {
                        deviceOwner.forceEphemeralUsers = forceEphemeralUsers;
                        saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
                        this.mUserManagerInternal.setForceEphemeralUsers(forceEphemeralUsers);
                        removeAllUsers = forceEphemeralUsers;
                    }
                }
                if (removeAllUsers) {
                    long identitity = this.mInjector.binderClearCallingIdentity();
                    try {
                        this.mUserManagerInternal.removeAllUsers();
                    } finally {
                        this.mInjector.binderRestoreCallingIdentity(identitity);
                    }
                }
                return;
            }
            throw new UnsupportedOperationException("Cannot force ephemeral users on systems without split system user.");
        }
    }

    public boolean getForceEphemeralUsers(ComponentName who) {
        if (!this.mHasFeature) {
            return false;
        }
        boolean z;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            z = getActiveAdminForCallerLocked(who, -2).forceEphemeralUsers;
        }
        return z;
    }

    private void ensureDeviceOwnerAndAllUsersAffiliated(ComponentName who) throws SecurityException {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
            if (areAllUsersAffiliatedWithDeviceLocked()) {
            } else {
                throw new SecurityException("Not all users are affiliated.");
            }
        }
    }

    public boolean requestBugreport(ComponentName who) {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        ensureDeviceOwnerAndAllUsersAffiliated(who);
        if (this.mRemoteBugreportServiceIsActive.get() || getDeviceOwnerRemoteBugreportUri() != null) {
            Slog.d(LOG_TAG, "Remote bugreport wasn't started because there's already one running.");
            return false;
        }
        long currentTime = System.currentTimeMillis();
        synchronized (getLockObject()) {
            DevicePolicyData policyData = getUserData(0);
            if (currentTime > policyData.mLastBugReportRequestTime) {
                policyData.mLastBugReportRequestTime = currentTime;
                saveSettingsLocked(0);
            }
        }
        long callingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityManager().requestBugReport(2);
            this.mRemoteBugreportServiceIsActive.set(true);
            this.mRemoteBugreportSharingAccepted.set(false);
            registerRemoteBugreportReceivers();
            this.mInjector.getNotificationManager().notifyAsUser(LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(this.mContext, 1), UserHandle.ALL);
            this.mHandler.postDelayed(this.mRemoteBugreportTimeoutRunnable, 600000);
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Failed to make remote calls to start bugreportremote service", re);
            return z;
        } finally {
            z = this.mInjector;
            z.binderRestoreCallingIdentity(callingIdentity);
            return z;
        }
        return true;
    }

    void sendDeviceOwnerCommand(String action, Bundle extras) {
        int deviceOwnerUserId;
        ComponentName deviceOwnerComponent;
        synchronized (getLockObject()) {
            deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
            deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
        }
        sendActiveAdminCommand(action, extras, deviceOwnerUserId, deviceOwnerComponent);
    }

    private void sendProfileOwnerCommand(String action, Bundle extras, int userHandle) {
        sendActiveAdminCommand(action, extras, userHandle, this.mOwners.getProfileOwnerComponent(userHandle));
    }

    private void sendActiveAdminCommand(String action, Bundle extras, int userHandle, ComponentName receiverComponent) {
        Intent intent = new Intent(action);
        intent.setComponent(receiverComponent);
        if (extras != null) {
            intent.putExtras(extras);
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userHandle));
    }

    private void sendOwnerChangedBroadcast(String broadcast, int userId) {
        this.mContext.sendBroadcastAsUser(new Intent(broadcast).addFlags(DumpState.DUMP_SERVICE_PERMISSIONS), UserHandle.of(userId));
    }

    private String getDeviceOwnerRemoteBugreportUri() {
        String deviceOwnerRemoteBugreportUri;
        synchronized (getLockObject()) {
            deviceOwnerRemoteBugreportUri = this.mOwners.getDeviceOwnerRemoteBugreportUri();
        }
        return deviceOwnerRemoteBugreportUri;
    }

    private void setDeviceOwnerRemoteBugreportUriAndHash(String bugreportUri, String bugreportHash) {
        synchronized (getLockObject()) {
            this.mOwners.setDeviceOwnerRemoteBugreportUriAndHash(bugreportUri, bugreportHash);
        }
    }

    private void registerRemoteBugreportReceivers() {
        try {
            this.mContext.registerReceiver(this.mRemoteBugreportFinishedReceiver, new IntentFilter("android.intent.action.REMOTE_BUGREPORT_DISPATCH", "application/vnd.android.bugreport"));
        } catch (MalformedMimeTypeException e) {
            Slog.w(LOG_TAG, "Failed to set type application/vnd.android.bugreport", e);
        }
        IntentFilter filterConsent = new IntentFilter();
        filterConsent.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED");
        filterConsent.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED");
        this.mContext.registerReceiver(this.mRemoteBugreportConsentReceiver, filterConsent);
    }

    private void onBugreportFinished(Intent intent) {
        this.mHandler.removeCallbacks(this.mRemoteBugreportTimeoutRunnable);
        this.mRemoteBugreportServiceIsActive.set(false);
        Uri bugreportUri = intent.getData();
        String bugreportUriString = null;
        if (bugreportUri != null) {
            bugreportUriString = bugreportUri.toString();
        }
        String bugreportHash = intent.getStringExtra("android.intent.extra.REMOTE_BUGREPORT_HASH");
        if (this.mRemoteBugreportSharingAccepted.get()) {
            shareBugreportWithDeviceOwnerIfExists(bugreportUriString, bugreportHash);
            this.mInjector.getNotificationManager().cancel(LOG_TAG, 678432343);
        } else {
            setDeviceOwnerRemoteBugreportUriAndHash(bugreportUriString, bugreportHash);
            this.mInjector.getNotificationManager().notifyAsUser(LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(this.mContext, 3), UserHandle.ALL);
        }
        this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
    }

    private void onBugreportFailed() {
        this.mRemoteBugreportServiceIsActive.set(false);
        this.mInjector.systemPropertiesSet("ctl.stop", "bugreportremote");
        this.mRemoteBugreportSharingAccepted.set(false);
        setDeviceOwnerRemoteBugreportUriAndHash(null, null);
        this.mInjector.getNotificationManager().cancel(LOG_TAG, 678432343);
        Bundle extras = new Bundle();
        extras.putInt("android.app.extra.BUGREPORT_FAILURE_REASON", 0);
        sendDeviceOwnerCommand("android.app.action.BUGREPORT_FAILED", extras);
        this.mContext.unregisterReceiver(this.mRemoteBugreportConsentReceiver);
        this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
    }

    private void onBugreportSharingAccepted() {
        String bugreportUriString;
        String bugreportHash;
        this.mRemoteBugreportSharingAccepted.set(true);
        synchronized (getLockObject()) {
            bugreportUriString = getDeviceOwnerRemoteBugreportUri();
            bugreportHash = this.mOwners.getDeviceOwnerRemoteBugreportHash();
        }
        if (bugreportUriString != null) {
            shareBugreportWithDeviceOwnerIfExists(bugreportUriString, bugreportHash);
        } else if (this.mRemoteBugreportServiceIsActive.get()) {
            this.mInjector.getNotificationManager().notifyAsUser(LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(this.mContext, 2), UserHandle.ALL);
        }
    }

    private void onBugreportSharingDeclined() {
        if (this.mRemoteBugreportServiceIsActive.get()) {
            this.mInjector.systemPropertiesSet("ctl.stop", "bugreportremote");
            this.mRemoteBugreportServiceIsActive.set(false);
            this.mHandler.removeCallbacks(this.mRemoteBugreportTimeoutRunnable);
            this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
        }
        this.mRemoteBugreportSharingAccepted.set(false);
        setDeviceOwnerRemoteBugreportUriAndHash(null, null);
        sendDeviceOwnerCommand("android.app.action.BUGREPORT_SHARING_DECLINED", null);
    }

    private void shareBugreportWithDeviceOwnerIfExists(String bugreportUriString, String bugreportHash) {
        ParcelFileDescriptor pfd = null;
        if (bugreportUriString != null) {
            try {
                Uri bugreportUri = Uri.parse(bugreportUriString);
                pfd = this.mContext.getContentResolver().openFileDescriptor(bugreportUri, "r");
                synchronized (getLockObject()) {
                    Intent intent = new Intent("android.app.action.BUGREPORT_SHARE");
                    intent.setComponent(this.mOwners.getDeviceOwnerComponent());
                    intent.setDataAndType(bugreportUri, "application/vnd.android.bugreport");
                    intent.putExtra("android.app.extra.BUGREPORT_HASH", bugreportHash);
                    intent.setFlags(1);
                    ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).grantUriPermissionFromIntent(IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME, this.mOwners.getDeviceOwnerComponent().getPackageName(), intent, this.mOwners.getDeviceOwnerUserId());
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.of(this.mOwners.getDeviceOwnerUserId()));
                }
                if (pfd != null) {
                    try {
                        pfd.close();
                    } catch (IOException e) {
                    }
                }
            } catch (FileNotFoundException e2) {
                try {
                    Bundle extras = new Bundle();
                    extras.putInt("android.app.extra.BUGREPORT_FAILURE_REASON", 1);
                    sendDeviceOwnerCommand("android.app.action.BUGREPORT_FAILED", extras);
                    if (pfd != null) {
                        try {
                            pfd.close();
                        } catch (IOException e3) {
                        }
                    }
                } catch (Throwable th) {
                    if (pfd != null) {
                        try {
                            pfd.close();
                        } catch (IOException e4) {
                        }
                    }
                    this.mRemoteBugreportSharingAccepted.set(false);
                    setDeviceOwnerRemoteBugreportUriAndHash(null, null);
                }
            }
            this.mRemoteBugreportSharingAccepted.set(false);
            setDeviceOwnerRemoteBugreportUriAndHash(null, null);
            return;
        }
        throw new FileNotFoundException();
    }

    public void setCameraDisabled(ComponentName who, boolean disabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 8);
                if (ap.disableCamera != disabled) {
                    ap.disableCamera = disabled;
                    saveSettingsLocked(userHandle);
                }
            }
            pushUserRestrictions(userHandle);
        }
    }

    public boolean getCameraDisabled(ComponentName who, int userHandle) {
        return getCameraDisabled(who, userHandle, true);
    }

    /* JADX WARNING: Missing block: B:11:0x0017, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getCameraDisabled(ComponentName who, int userHandle, boolean mergeDeviceOwnerRestriction) {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                    if (admin != null) {
                        z = admin.disableCamera;
                    }
                } catch (Throwable th) {
                }
            } else {
                if (mergeDeviceOwnerRestriction) {
                    ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
                    if (deviceOwner != null && deviceOwner.disableCamera) {
                        return true;
                    }
                }
                DevicePolicyData policy = getUserData(userHandle);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    if (((ActiveAdmin) policy.mAdminList.get(i)).disableCamera) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public void setKeyguardDisabledFeatures(ComponentName who, int which, boolean parent) {
        if (this.mHasFeature) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setKeyguardDisabledFeatures to ");
            stringBuilder.append(which);
            stringBuilder.append("; by: ");
            stringBuilder.append(who);
            Slog.w(str, stringBuilder.toString());
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            if (isManagedProfile(userHandle)) {
                if (parent) {
                    which &= 432;
                } else {
                    which &= PROFILE_KEYGUARD_FEATURES;
                }
            }
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 9, parent);
                if (ap.disabledKeyguardFeatures != which) {
                    ap.disabledKeyguardFeatures = which;
                    saveSettingsLocked(userHandle);
                }
            }
            if (SecurityLog.isLoggingEnabled()) {
                int affectedUserId = parent ? getProfileParentId(userHandle) : userHandle;
                SecurityLog.writeEvent(210021, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), Integer.valueOf(affectedUserId), Integer.valueOf(which)});
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x0044 A:{Catch:{ all -> 0x0077 }} */
    /* JADX WARNING: Missing block: B:13:0x0020, code skipped:
            r12.mInjector.binderRestoreCallingIdentity(r2);
     */
    /* JADX WARNING: Missing block: B:14:0x0025, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getKeyguardDisabledFeatures(ComponentName who, int userHandle, boolean parent) {
        int i = 0;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    if (admin != null) {
                        i = admin.disabledKeyguardFeatures;
                    }
                } else {
                    List<ActiveAdmin> admins;
                    int N;
                    int which;
                    int i2;
                    if (!parent) {
                        if (isManagedProfile(userHandle)) {
                            admins = getUserDataUnchecked(userHandle).mAdminList;
                            N = admins.size();
                            which = 0;
                            for (i2 = 0; i2 < N; i2++) {
                                ActiveAdmin admin2 = (ActiveAdmin) admins.get(i2);
                                int userId = admin2.getUserHandle().getIdentifier();
                                boolean isRequestedUser = !parent && userId == userHandle;
                                if (!isRequestedUser) {
                                    if (isManagedProfile(userId)) {
                                        which |= admin2.disabledKeyguardFeatures & 432;
                                    }
                                }
                                which |= admin2.disabledKeyguardFeatures;
                            }
                            this.mInjector.binderRestoreCallingIdentity(ident);
                            return which;
                        }
                    }
                    admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
                    N = admins.size();
                    which = 0;
                    while (i2 < N) {
                    }
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    return which;
                }
            }
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void setKeepUninstalledPackages(ComponentName who, String callerPackage, List<String> packageList) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(packageList, "packageList is null");
            int userHandle = UserHandle.getCallingUserId();
            synchronized (getLockObject()) {
                enforceCanManageScope(who, callerPackage, -2, "delegation-keep-uninstalled-packages");
                getDeviceOwnerAdminLocked().keepUninstalledPackages = packageList;
                saveSettingsLocked(userHandle);
                this.mInjector.getPackageManagerInternal().setKeepUninstalledPackages(packageList);
            }
        }
    }

    public List<String> getKeepUninstalledPackages(ComponentName who, String callerPackage) {
        if (!this.mHasFeature) {
            return null;
        }
        List keepUninstalledPackagesLocked;
        synchronized (getLockObject()) {
            enforceCanManageScope(who, callerPackage, -2, "delegation-keep-uninstalled-packages");
            keepUninstalledPackagesLocked = getKeepUninstalledPackagesLocked();
        }
        return keepUninstalledPackagesLocked;
    }

    private List<String> getKeepUninstalledPackagesLocked() {
        ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
        return deviceOwner != null ? deviceOwner.keepUninstalledPackages : null;
    }

    public boolean setDeviceOwner(ComponentName admin, String ownerName, int userId) {
        if (!this.mHasFeature) {
            return false;
        }
        if (admin == null || !isPackageInstalledForUser(admin.getPackageName(), userId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid component ");
            stringBuilder.append(admin);
            stringBuilder.append(" for device owner");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        boolean hasIncompatibleAccountsOrNonAdb = hasIncompatibleAccountsOrNonAdbNoLock(userId, admin);
        synchronized (getLockObject()) {
            enforceCanSetDeviceOwnerLocked(admin, userId, hasIncompatibleAccountsOrNonAdb);
            ActiveAdmin activeAdmin = getActiveAdminUncheckedLocked(admin, userId);
            if (activeAdmin == null || getUserData(userId).mRemovingAdmins.contains(admin)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Not active admin: ");
                stringBuilder2.append(admin);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                String str;
                StringBuilder stringBuilder3;
                if (this.mInjector.getIBackupManager() != null) {
                    this.mInjector.getIBackupManager().setBackupServiceActive(0, false);
                }
                this.mInjector.binderRestoreCallingIdentity(ident);
                if (isAdb()) {
                    MetricsLogger.action(this.mContext, NetdResponseCode.StrictCleartext, LOG_TAG_DEVICE_OWNER);
                }
                this.mOwners.setDeviceOwner(admin, ownerName, userId);
                this.mOwners.writeDeviceOwner();
                updateDeviceOwnerLocked();
                setDeviceOwnerSystemPropertyLocked();
                Set<String> restrictions = UserRestrictionsUtils.getDefaultEnabledForDeviceOwner();
                if (!restrictions.isEmpty()) {
                    for (String restriction : restrictions) {
                        activeAdmin.ensureUserRestrictions().putBoolean(restriction, true);
                    }
                    activeAdmin.defaultEnabledRestrictionsAlreadySet.addAll(restrictions);
                    str = LOG_TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Enabled the following restrictions by default: ");
                    stringBuilder3.append(restrictions);
                    Slog.i(str, stringBuilder3.toString());
                    saveUserRestrictionsLocked(userId);
                }
                ident = this.mInjector.binderClearCallingIdentity();
                sendOwnerChangedBroadcast("android.app.action.DEVICE_OWNER_CHANGED", userId);
                this.mInjector.binderRestoreCallingIdentity(ident);
                this.mDeviceAdminServiceController.startServiceForOwner(admin.getPackageName(), userId, "set-device-owner");
                str = LOG_TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Device owner set: ");
                stringBuilder3.append(admin);
                stringBuilder3.append(" on user ");
                stringBuilder3.append(userId);
                Slog.i(str, stringBuilder3.toString());
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed deactivating backup service.", e);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        return true;
    }

    public boolean hasDeviceOwner() {
        enforceDeviceOwnerOrManageUsers();
        return this.mOwners.hasDeviceOwner();
    }

    boolean isDeviceOwner(ActiveAdmin admin) {
        return isDeviceOwner(admin.info.getComponent(), admin.getUserHandle().getIdentifier());
    }

    public boolean isDeviceOwner(ComponentName who, int userId) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == userId && this.mOwners.getDeviceOwnerComponent().equals(who);
        }
        return z;
    }

    private boolean isDeviceOwnerPackage(String packageName, int userId) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == userId && this.mOwners.getDeviceOwnerPackageName().equals(packageName);
        }
        return z;
    }

    private boolean isProfileOwnerPackage(String packageName, int userId) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasProfileOwner(userId) && this.mOwners.getProfileOwnerPackage(userId).equals(packageName);
        }
        return z;
    }

    public boolean isProfileOwner(ComponentName who, int userId) {
        return who != null && who.equals(getProfileOwner(userId));
    }

    public ComponentName getDeviceOwnerComponent(boolean callingUserOnly) {
        if (!this.mHasFeature) {
            return null;
        }
        if (!callingUserOnly) {
            enforceManageUsers();
        }
        synchronized (getLockObject()) {
            if (!this.mOwners.hasDeviceOwner()) {
                return null;
            } else if (!callingUserOnly || this.mInjector.userHandleGetCallingUserId() == this.mOwners.getDeviceOwnerUserId()) {
                ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
                return deviceOwnerComponent;
            } else {
                return null;
            }
        }
    }

    public int getDeviceOwnerUserId() {
        int i = -10000;
        if (!this.mHasFeature) {
            return -10000;
        }
        enforceManageUsers();
        synchronized (getLockObject()) {
            if (this.mOwners.hasDeviceOwner()) {
                i = this.mOwners.getDeviceOwnerUserId();
            }
        }
        return i;
    }

    public String getDeviceOwnerName() {
        if (!this.mHasFeature) {
            return null;
        }
        enforceManageUsers();
        synchronized (getLockObject()) {
            if (this.mOwners.hasDeviceOwner()) {
                String applicationLabel = getApplicationLabel(this.mOwners.getDeviceOwnerPackageName(), 0);
                return applicationLabel;
            }
            return null;
        }
    }

    @VisibleForTesting
    ActiveAdmin getDeviceOwnerAdminLocked() {
        ensureLocked();
        ComponentName component = this.mOwners.getDeviceOwnerComponent();
        if (component == null) {
            return null;
        }
        DevicePolicyData policy = getUserData(this.mOwners.getDeviceOwnerUserId());
        int n = policy.mAdminList.size();
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
            if (component.equals(admin.info.getComponent())) {
                return admin;
            }
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Active admin for device owner not found. component=");
        stringBuilder.append(component);
        Slog.wtf(str, stringBuilder.toString());
        return null;
    }

    public void clearDeviceOwner(String packageName) {
        Preconditions.checkNotNull(packageName, "packageName is null");
        int callingUid = this.mInjector.binderGetCallingUid();
        try {
            int uid = this.mInjector.getPackageManager().getPackageUidAsUser(packageName, UserHandle.getUserId(callingUid));
            synchronized (this) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("clearDeviceOwner packageName=");
                stringBuilder.append(packageName);
                stringBuilder.append(",callingUid=");
                stringBuilder.append(callingUid);
                stringBuilder.append(",mIsMDMDeviceOwnerAPI=");
                stringBuilder.append(this.mIsMDMDeviceOwnerAPI);
                Slog.w(str, stringBuilder.toString());
                if (uid != callingUid) {
                    if (!this.mIsMDMDeviceOwnerAPI) {
                        throw new SecurityException("Invalid packageName");
                    }
                }
            }
            synchronized (getLockObject()) {
                ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
                int deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
                if (this.mOwners.hasDeviceOwner() && deviceOwnerComponent.getPackageName().equals(packageName) && deviceOwnerUserId == UserHandle.getUserId(callingUid)) {
                    enforceUserUnlocked(deviceOwnerUserId);
                    ActiveAdmin admin = getDeviceOwnerAdminLocked();
                    long ident = this.mInjector.binderClearCallingIdentity();
                    try {
                        clearDeviceOwnerLocked(admin, deviceOwnerUserId);
                        removeActiveAdminLocked(deviceOwnerComponent, deviceOwnerUserId);
                        sendOwnerChangedBroadcast("android.app.action.DEVICE_OWNER_CHANGED", deviceOwnerUserId);
                        String str2 = LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Device owner removed: ");
                        stringBuilder2.append(deviceOwnerComponent);
                        Slog.i(str2, stringBuilder2.toString());
                    } finally {
                        this.mInjector.binderRestoreCallingIdentity(ident);
                    }
                } else {
                    throw new SecurityException("clearDeviceOwner can only be called by the device owner");
                }
            }
        } catch (NameNotFoundException e) {
            throw new SecurityException(e);
        }
    }

    private void clearOverrideApnUnchecked() {
        int i = 0;
        setOverrideApnsEnabledUnchecked(false);
        List<ApnSetting> apns = getOverrideApnsUnchecked();
        while (i < apns.size()) {
            removeOverrideApnUnchecked(((ApnSetting) apns.get(i)).getId());
            i++;
        }
    }

    private void clearDeviceOwnerLocked(ActiveAdmin admin, int userId) {
        this.mDeviceAdminServiceController.stopServiceForOwner(userId, "clear-device-owner");
        if (admin != null) {
            admin.disableCamera = false;
            admin.userRestrictions = null;
            admin.defaultEnabledRestrictionsAlreadySet.clear();
            admin.forceEphemeralUsers = false;
            admin.isNetworkLoggingEnabled = false;
            this.mUserManagerInternal.setForceEphemeralUsers(admin.forceEphemeralUsers);
        }
        getUserData(userId).mCurrentInputMethodSet = false;
        saveSettingsLocked(userId);
        DevicePolicyData systemPolicyData = getUserData(0);
        systemPolicyData.mLastSecurityLogRetrievalTime = -1;
        systemPolicyData.mLastBugReportRequestTime = -1;
        systemPolicyData.mLastNetworkLogsRetrievalTime = -1;
        saveSettingsLocked(0);
        clearUserPoliciesLocked(userId);
        clearOverrideApnUnchecked();
        this.mOwners.clearDeviceOwner();
        this.mOwners.writeDeviceOwner();
        updateDeviceOwnerLocked();
        clearDeviceOwnerUserRestrictionLocked(UserHandle.of(userId));
        this.mInjector.securityLogSetLoggingEnabledProperty(false);
        this.mSecurityLogMonitor.stop();
        setNetworkLoggingActiveInternal(false);
        deleteTransferOwnershipBundleLocked(userId);
        try {
            if (this.mInjector.getIBackupManager() != null) {
                this.mInjector.getIBackupManager().setBackupServiceActive(0, true);
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed reactivating backup service.", e);
        }
    }

    public boolean setProfileOwner(ComponentName who, String ownerName, int userHandle) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        if (who == null || !isPackageInstalledForUser(who.getPackageName(), userHandle)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Component ");
            stringBuilder.append(who);
            stringBuilder.append(" not installed for userId:");
            stringBuilder.append(userHandle);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        boolean hasIncompatibleAccountsOrNonAdb = hasIncompatibleAccountsOrNonAdbNoLock(userHandle, who);
        synchronized (getLockObject()) {
            enforceCanSetProfileOwnerLocked(who, userHandle, hasIncompatibleAccountsOrNonAdb);
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
            StringBuilder stringBuilder2;
            if (admin == null || getUserData(userHandle).mRemovingAdmins.contains(who)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Not active admin: ");
                stringBuilder2.append(who);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
            if (isAdb()) {
                MetricsLogger.action(this.mContext, NetdResponseCode.StrictCleartext, LOG_TAG_PROFILE_OWNER);
            }
            this.mOwners.setProfileOwner(who, ownerName, userHandle);
            this.mOwners.writeProfileOwner(userHandle);
            String str = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Profile owner set: ");
            stringBuilder2.append(who);
            stringBuilder2.append(" on user ");
            stringBuilder2.append(userHandle);
            Slog.i(str, stringBuilder2.toString());
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                z = true;
                if (this.mUserManager.isManagedProfile(userHandle)) {
                    maybeSetDefaultRestrictionsForAdminLocked(userHandle, admin, UserRestrictionsUtils.getDefaultEnabledForManagedProfiles());
                    ensureUnknownSourcesRestrictionForProfileOwnerLocked(userHandle, admin, true);
                }
                sendOwnerChangedBroadcast("android.app.action.PROFILE_OWNER_CHANGED", userHandle);
                this.mDeviceAdminServiceController.startServiceForOwner(who.getPackageName(), userHandle, "set-profile-owner");
            } finally {
                z = this.mInjector;
                z.binderRestoreCallingIdentity(id);
            }
        }
        return z;
    }

    public void clearProfileOwner(ComponentName who) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userId = this.mInjector.userHandleGetCallingUserId();
            enforceNotManagedProfile(userId, "clear profile owner");
            enforceUserUnlocked(userId);
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    clearProfileOwnerLocked(admin, userId);
                    removeActiveAdminLocked(who, userId);
                    sendOwnerChangedBroadcast("android.app.action.PROFILE_OWNER_CHANGED", userId);
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Profile owner ");
                    stringBuilder.append(who);
                    stringBuilder.append(" removed from user ");
                    stringBuilder.append(userId);
                    Slog.i(str, stringBuilder.toString());
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            }
        }
    }

    public void clearProfileOwnerLocked(ActiveAdmin admin, int userId) {
        this.mDeviceAdminServiceController.stopServiceForOwner(userId, "clear-profile-owner");
        if (admin != null) {
            admin.disableCamera = false;
            admin.userRestrictions = null;
            admin.defaultEnabledRestrictionsAlreadySet.clear();
        }
        DevicePolicyData policyData = getUserData(userId);
        policyData.mCurrentInputMethodSet = false;
        policyData.mOwnerInstalledCaCerts.clear();
        saveSettingsLocked(userId);
        clearUserPoliciesLocked(userId);
        this.mOwners.removeProfileOwner(userId);
        this.mOwners.writeProfileOwner(userId);
        deleteTransferOwnershipBundleLocked(userId);
    }

    public void setDeviceOwnerLockScreenInfo(ComponentName who, CharSequence info) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (this.mHasFeature) {
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(who, -2);
                long token = this.mInjector.binderClearCallingIdentity();
                try {
                    this.mLockPatternUtils.setDeviceOwnerInfo(info != null ? info.toString() : null);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(token);
                }
            }
        }
    }

    public CharSequence getDeviceOwnerLockScreenInfo() {
        return this.mLockPatternUtils.getDeviceOwnerInfo();
    }

    private void clearUserPoliciesLocked(int userId) {
        DevicePolicyData policy = getUserData(userId);
        policy.mPermissionPolicy = 0;
        policy.mDelegationMap.clear();
        policy.mStatusBarDisabled = false;
        policy.mUserProvisioningState = 0;
        policy.mAffiliationIds.clear();
        policy.mLockTaskPackages.clear();
        updateLockTaskPackagesLocked(policy.mLockTaskPackages, userId);
        policy.mLockTaskFeatures = 0;
        saveSettingsLocked(userId);
        try {
            this.mIPackageManager.updatePermissionFlagsForAllApps(4, 0, userId);
            pushUserRestrictions(userId);
        } catch (RemoteException e) {
        }
    }

    public boolean hasUserSetupCompleted() {
        return hasUserSetupCompleted(UserHandle.getCallingUserId());
    }

    private boolean hasUserSetupCompleted(int userHandle) {
        if (this.mHasFeature) {
            return getUserData(userHandle).mUserSetupComplete;
        }
        return true;
    }

    private boolean hasPaired(int userHandle) {
        if (this.mHasFeature) {
            return getUserData(userHandle).mPaired;
        }
        return true;
    }

    public int getUserProvisioningState() {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceManageUsers();
        return getUserProvisioningState(this.mInjector.userHandleGetCallingUserId());
    }

    private int getUserProvisioningState(int userHandle) {
        return getUserData(userHandle).mUserProvisioningState;
    }

    public void setUserProvisioningState(int newState, int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        if (userHandle == this.mOwners.getDeviceOwnerUserId() || this.mOwners.hasProfileOwner(userHandle) || getManagedUserId(userHandle) != -1) {
            synchronized (getLockObject()) {
                boolean transitionCheckNeeded = true;
                if (!isAdb()) {
                    enforceCanManageProfileAndDeviceOwners();
                } else if (getUserProvisioningState(userHandle) == 0 && newState == 3) {
                    transitionCheckNeeded = false;
                } else {
                    throw new IllegalStateException("Not allowed to change provisioning state unless current provisioning state is unmanaged, and new state is finalized.");
                }
                DevicePolicyData policyData = getUserData(userHandle);
                if (transitionCheckNeeded) {
                    checkUserProvisioningStateTransition(policyData.mUserProvisioningState, newState);
                }
                policyData.mUserProvisioningState = newState;
                saveSettingsLocked(userHandle);
            }
            return;
        }
        throw new IllegalStateException("Not allowed to change provisioning state unless a device or profile owner is set.");
    }

    private void checkUserProvisioningStateTransition(int currentState, int newState) {
        if (currentState != 4) {
            switch (currentState) {
                case 0:
                    if (newState != 0) {
                        return;
                    }
                    break;
                case 1:
                case 2:
                    if (newState == 3) {
                        return;
                    }
                    break;
            }
        } else if (newState == 0) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot move to user provisioning state [");
        stringBuilder.append(newState);
        stringBuilder.append("] from state [");
        stringBuilder.append(currentState);
        stringBuilder.append("]");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void setProfileEnabled(ComponentName who) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(who, -1);
                int userId = UserHandle.getCallingUserId();
                enforceManagedProfile(userId, "enable the profile");
                if (getUserInfo(userId).isEnabled()) {
                    Slog.e(LOG_TAG, "setProfileEnabled is called when the profile is already enabled");
                    return;
                }
                long id = this.mInjector.binderClearCallingIdentity();
                try {
                    this.mUserManager.setUserEnabled(userId);
                    UserInfo parent = this.mUserManager.getProfileParent(userId);
                    Intent intent = new Intent("android.intent.action.MANAGED_PROFILE_ADDED");
                    intent.putExtra("android.intent.extra.USER", new UserHandle(userId));
                    intent.addFlags(1342177280);
                    this.mContext.sendBroadcastAsUser(intent, new UserHandle(parent.id));
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
        }
    }

    public void setProfileName(ComponentName who, String profileName) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userId = UserHandle.getCallingUserId();
        getActiveAdminForCallerLocked(who, -1);
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            this.mUserManager.setUserName(userId, profileName);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public ComponentName getProfileOwner(int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        ComponentName profileOwnerComponent;
        synchronized (getLockObject()) {
            profileOwnerComponent = this.mOwners.getProfileOwnerComponent(userHandle);
        }
        return profileOwnerComponent;
    }

    @VisibleForTesting
    ActiveAdmin getProfileOwnerAdminLocked(int userHandle) {
        ComponentName profileOwner = this.mOwners.getProfileOwnerComponent(userHandle);
        if (profileOwner == null) {
            return null;
        }
        DevicePolicyData policy = getUserData(userHandle);
        int n = policy.mAdminList.size();
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
            if (profileOwner.equals(admin.info.getComponent())) {
                return admin;
            }
        }
        return null;
    }

    public String getProfileOwnerName(int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceManageUsers();
        ComponentName profileOwner = getProfileOwner(userHandle);
        if (profileOwner == null) {
            return null;
        }
        return getApplicationLabel(profileOwner.getPackageName(), userHandle);
    }

    private String getApplicationLabel(String packageName, int userHandle) {
        long token = this.mInjector.binderClearCallingIdentity();
        String str = null;
        try {
            Context userContext = this.mContext.createPackageContextAsUser(packageName, 0, new UserHandle(userHandle));
            ApplicationInfo appInfo = userContext.getApplicationInfo();
            CharSequence result = null;
            if (appInfo != null) {
                result = appInfo.loadUnsafeLabel(userContext.getPackageManager());
            }
            if (result != null) {
                str = result.toString();
            }
            this.mInjector.binderRestoreCallingIdentity(token);
            return str;
        } catch (NameNotFoundException nnfe) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(" is not installed for user ");
            stringBuilder.append(userHandle);
            Log.w(str2, stringBuilder.toString(), nnfe);
            this.mInjector.binderRestoreCallingIdentity(token);
            return null;
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(token);
            throw th;
        }
    }

    private void wtfIfInLock() {
        if (Thread.holdsLock(this)) {
            Slog.wtfStack(LOG_TAG, "Shouldn't be called with DPMS lock held");
        }
    }

    private void enforceCanSetProfileOwnerLocked(ComponentName owner, int userHandle, boolean hasIncompatibleAccountsOrNonAdb) {
        UserInfo info = getUserInfo(userHandle);
        if (info == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempted to set profile owner for invalid userId: ");
            stringBuilder.append(userHandle);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (info.isGuest()) {
            throw new IllegalStateException("Cannot set a profile owner on a guest");
        } else if (this.mOwners.hasProfileOwner(userHandle)) {
            throw new IllegalStateException("Trying to set the profile owner, but profile owner is already set.");
        } else if (this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == userHandle) {
            throw new IllegalStateException("Trying to set the profile owner, but the user already has a device owner.");
        } else if (!isAdb()) {
            enforceCanManageProfileAndDeviceOwners();
            if ((this.mIsWatch || hasUserSetupCompleted(userHandle)) && !isCallerWithSystemUid()) {
                throw new IllegalStateException("Cannot set the profile owner on a user which is already set-up");
            }
        } else if ((this.mIsWatch || hasUserSetupCompleted(userHandle)) && hasIncompatibleAccountsOrNonAdb) {
            throw new IllegalStateException("Not allowed to set the profile owner because there are already some accounts on the profile");
        }
    }

    private void enforceCanSetDeviceOwnerLocked(ComponentName owner, int userId, boolean hasIncompatibleAccountsOrNonAdb) {
        if (!isAdb()) {
            enforceCanManageProfileAndDeviceOwners();
        }
        int code = checkDeviceOwnerProvisioningPreConditionLocked(owner, userId, isAdb(), hasIncompatibleAccountsOrNonAdb);
        StringBuilder stringBuilder;
        switch (code) {
            case 0:
                return;
            case 1:
                throw new IllegalStateException("Trying to set the device owner, but device owner is already set.");
            case 2:
                throw new IllegalStateException("Trying to set the device owner, but the user already has a profile owner.");
            case 3:
                stringBuilder = new StringBuilder();
                stringBuilder.append("User not running: ");
                stringBuilder.append(userId);
                throw new IllegalStateException(stringBuilder.toString());
            case 4:
                throw new IllegalStateException("Cannot set the device owner if the device is already set-up");
            case 5:
                throw new IllegalStateException("Not allowed to set the device owner because there are already several users on the device");
            case 6:
                throw new IllegalStateException("Not allowed to set the device owner because there are already some accounts on the device");
            case 7:
                throw new IllegalStateException("User is not system user");
            case 8:
                throw new IllegalStateException("Not allowed to set the device owner because this device has already paired");
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected @ProvisioningPreCondition ");
                stringBuilder.append(code);
                throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private void enforceUserUnlocked(int userId) {
        Preconditions.checkState(this.mUserManager.isUserUnlocked(userId), "User must be running and unlocked");
    }

    private void enforceUserUnlocked(int userId, boolean parent) {
        if (parent) {
            enforceUserUnlocked(getProfileParentId(userId));
        } else {
            enforceUserUnlocked(userId);
        }
    }

    private void enforceManageUsers() {
        int callingUid = this.mInjector.binderGetCallingUid();
        if (!isCallerWithSystemUid() && callingUid != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        }
    }

    public void enforceFullCrossUsersPermission(int userHandle) {
        enforceSystemUserOrPermissionIfCrossUser(userHandle, "android.permission.INTERACT_ACROSS_USERS_FULL");
    }

    private void enforceCrossUsersPermission(int userHandle) {
        enforceSystemUserOrPermissionIfCrossUser(userHandle, "android.permission.INTERACT_ACROSS_USERS");
    }

    private void enforceSystemUserOrPermission(String permission) {
        if (!isCallerWithSystemUid() && this.mInjector.binderGetCallingUid() != 0) {
            Context context = this.mContext;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Must be system or have ");
            stringBuilder.append(permission);
            stringBuilder.append(" permission");
            context.enforceCallingOrSelfPermission(permission, stringBuilder.toString());
        }
    }

    private void enforceSystemUserOrPermissionIfCrossUser(int userHandle, String permission) {
        if (userHandle < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid userId ");
            stringBuilder.append(userHandle);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (userHandle != this.mInjector.userHandleGetCallingUserId()) {
            enforceSystemUserOrPermission(permission);
        }
    }

    private void enforceManagedProfile(int userHandle, String message) {
        if (!isManagedProfile(userHandle)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("You can not ");
            stringBuilder.append(message);
            stringBuilder.append(" outside a managed profile.");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void enforceNotManagedProfile(int userHandle, String message) {
        if (isManagedProfile(userHandle)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("You can not ");
            stringBuilder.append(message);
            stringBuilder.append(" for a managed profile.");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void enforceDeviceOwnerOrManageUsers() {
        synchronized (getLockObject()) {
            if (getActiveAdminWithPolicyForUidLocked(null, -2, this.mInjector.binderGetCallingUid()) != null) {
                return;
            }
            enforceManageUsers();
        }
    }

    private void enforceProfileOwnerOrSystemUser() {
        synchronized (getLockObject()) {
            if (getActiveAdminWithPolicyForUidLocked(null, -1, this.mInjector.binderGetCallingUid()) != null) {
                return;
            }
            Preconditions.checkState(isCallerWithSystemUid(), "Only profile owner, device owner and system may call this method.");
        }
    }

    private void enforceProfileOwnerOrFullCrossUsersPermission(int userId) {
        if (userId == this.mInjector.userHandleGetCallingUserId()) {
            synchronized (getLockObject()) {
                if (getActiveAdminWithPolicyForUidLocked(null, -1, this.mInjector.binderGetCallingUid()) != null) {
                    return;
                }
            }
        }
        enforceSystemUserOrPermission("android.permission.INTERACT_ACROSS_USERS_FULL");
    }

    private boolean canUserUseLockTaskLocked(int userId) {
        if (isUserAffiliatedWithDeviceLocked(userId)) {
            return true;
        }
        if (this.mOwners.hasDeviceOwner() || getProfileOwner(userId) == null || isManagedProfile(userId)) {
            return false;
        }
        return true;
    }

    private void enforceCanCallLockTaskLocked(ComponentName who) {
        getActiveAdminForCallerLocked(who, -1);
        int userId = this.mInjector.userHandleGetCallingUserId();
        if (!canUserUseLockTaskLocked(userId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User ");
            stringBuilder.append(userId);
            stringBuilder.append(" is not allowed to use lock task");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void ensureCallerPackage(String packageName) {
        if (packageName == null) {
            Preconditions.checkState(isCallerWithSystemUid(), "Only caller can omit package name");
            return;
        }
        try {
            boolean z = false;
            if (this.mIPackageManager.getApplicationInfo(packageName, 0, this.mInjector.userHandleGetCallingUserId()).uid == this.mInjector.binderGetCallingUid()) {
                z = true;
            }
            Preconditions.checkState(z, "Unmatching package name");
        } catch (RemoteException e) {
        }
    }

    private boolean isCallerWithSystemUid() {
        return UserHandle.isSameApp(this.mInjector.binderGetCallingUid(), 1000);
    }

    protected int getProfileParentId(int userHandle) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            int i;
            UserInfo parentUser = this.mUserManager.getProfileParent(userHandle);
            if (parentUser != null) {
                i = parentUser.id;
            } else {
                i = userHandle;
            }
            this.mInjector.binderRestoreCallingIdentity(ident);
            return i;
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private int getCredentialOwner(int userHandle, boolean parent) {
        long ident = this.mInjector.binderClearCallingIdentity();
        if (parent) {
            try {
                UserInfo parentProfile = this.mUserManager.getProfileParent(userHandle);
                if (parentProfile != null) {
                    userHandle = parentProfile.id;
                }
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        int credentialOwnerProfile = this.mUserManager.getCredentialOwnerProfile(userHandle);
        this.mInjector.binderRestoreCallingIdentity(ident);
        return credentialOwnerProfile;
    }

    private boolean isManagedProfile(int userHandle) {
        UserInfo user = getUserInfo(userHandle);
        return user != null && user.isManagedProfile();
    }

    private void enableIfNecessary(String packageName, int userId) {
        try {
            if (this.mIPackageManager.getApplicationInfo(packageName, 32768, userId).enabledSetting == 4) {
                this.mIPackageManager.setApplicationEnabledSetting(packageName, 0, 1, userId, LOG_TAG);
            }
        } catch (RemoteException e) {
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, pw)) {
            synchronized (getLockObject()) {
                pw.println("Current Device Policy Manager state:");
                this.mOwners.dump("  ", pw);
                this.mDeviceAdminServiceController.dump("  ", pw);
                int userCount = this.mUserData.size();
                for (int u = 0; u < userCount; u++) {
                    DevicePolicyData policy = getUserData(this.mUserData.keyAt(u));
                    pw.println();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Enabled Device Admins (User ");
                    stringBuilder.append(policy.mUserHandle);
                    stringBuilder.append(", provisioningState: ");
                    stringBuilder.append(policy.mUserProvisioningState);
                    stringBuilder.append("):");
                    pw.println(stringBuilder.toString());
                    pw.println(" ");
                    if (mHwCustDevicePolicyManagerService != null && mHwCustDevicePolicyManagerService.isForbiddenSimplePwdFeatureEnable()) {
                        pw.print("  mIsCurrentPwdSimple=");
                        pw.println(policy.mIsCurrentPwdSimple);
                    }
                    pw.print("    mPasswordOwner=");
                    pw.println(policy.mPasswordOwner);
                }
                pw.println();
                this.mConstants.dump("  ", pw);
                pw.println();
                this.mStatLogger.dump(pw, "  ");
                pw.println();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  Encryption Status: ");
                stringBuilder2.append(getEncryptionStatusName(getEncryptionStatus()));
                pw.println(stringBuilder2.toString());
            }
        }
    }

    private String getEncryptionStatusName(int encryptionStatus) {
        switch (encryptionStatus) {
            case 0:
                return "unsupported";
            case 1:
                return "inactive";
            case 2:
                return "activating";
            case 3:
                return "block";
            case 4:
                return "block default key";
            case 5:
                return "per-user";
            default:
                return Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    public void addPersistentPreferredActivity(ComponentName who, IntentFilter filter, ComponentName activity) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            Injector injector;
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.addPersistentPreferredActivity(filter, activity, userHandle);
                this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                injector = this.mInjector;
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
            injector.binderRestoreCallingIdentity(id);
        }
    }

    public void clearPackagePersistentPreferredActivities(ComponentName who, String packageName) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            Injector injector;
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.clearPackagePersistentPreferredActivities(packageName, userHandle);
                this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                injector = this.mInjector;
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
            injector.binderRestoreCallingIdentity(id);
        }
    }

    public void setDefaultSmsApplication(ComponentName admin, String packageName) {
        Preconditions.checkNotNull(admin, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(admin, -2);
        }
        this.mInjector.binderWithCleanCallingIdentity(new -$$Lambda$DevicePolicyManagerService$dDeS1FUetDCbtT673Qp0Hcsm5Vw(this, packageName));
    }

    public boolean setApplicationRestrictionsManagingPackage(ComponentName admin, String packageName) {
        try {
            setDelegatedScopePreO(admin, packageName, "delegation-app-restrictions");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getApplicationRestrictionsManagingPackage(ComponentName admin) {
        List<String> delegatePackages = getDelegatePackages(admin, "delegation-app-restrictions");
        return delegatePackages.size() > 0 ? (String) delegatePackages.get(0) : null;
    }

    public boolean isCallerApplicationRestrictionsManagingPackage(String callerPackage) {
        return isCallerDelegate(callerPackage, "delegation-app-restrictions");
    }

    public void setApplicationRestrictions(ComponentName who, String callerPackage, String packageName, Bundle settings) {
        enforceCanManageScope(who, callerPackage, -1, "delegation-app-restrictions");
        UserHandle userHandle = this.mInjector.binderGetCallingUserHandle();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            this.mUserManager.setApplicationRestrictions(packageName, settings, userHandle);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(admin, "admin is null");
            Preconditions.checkNotNull(agent, "agent is null");
            int userHandle = UserHandle.getCallingUserId();
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(admin, 9, parent).trustAgentInfos.put(agent.flattenToString(), new TrustAgentInfo(args));
                saveSettingsLocked(userHandle);
            }
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0047, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:48:0x00c6, code skipped:
            return r16;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userHandle, boolean parent) {
        ComponentName componentName = admin;
        int i = userHandle;
        boolean z = parent;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(agent, "agent null");
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            String componentName2 = agent.flattenToString();
            if (componentName != null) {
                ActiveAdmin ap = getActiveAdminUncheckedLocked(componentName, i, z);
                if (ap == null) {
                    return null;
                }
                TrustAgentInfo trustAgentInfo = (TrustAgentInfo) ap.trustAgentInfos.get(componentName2);
                if (trustAgentInfo != null) {
                    if (trustAgentInfo.options != null) {
                        List<PersistableBundle> result = new ArrayList();
                        result.add(trustAgentInfo.options);
                        return result;
                    }
                }
            } else {
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(i, z);
                boolean allAdminsHaveOptions = true;
                int N = admins.size();
                List<PersistableBundle> result2 = null;
                int i2 = 0;
                while (i2 < N) {
                    String componentName3;
                    ActiveAdmin active = (ActiveAdmin) admins.get(i2);
                    boolean disablesTrust = (active.disabledKeyguardFeatures & 16) != 0;
                    TrustAgentInfo info = (TrustAgentInfo) active.trustAgentInfos.get(componentName2);
                    if (info == null || info.options == null || info.options.isEmpty()) {
                        componentName3 = componentName2;
                        if (disablesTrust) {
                            allAdminsHaveOptions = false;
                            break;
                        }
                    } else if (disablesTrust) {
                        if (result2 == null) {
                            result2 = new ArrayList();
                        }
                        result2.add(info.options);
                        componentName3 = componentName2;
                    } else {
                        String str = LOG_TAG;
                        componentName3 = componentName2;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring admin ");
                        stringBuilder.append(active.info);
                        stringBuilder.append(" because it has trust options but doesn't declare KEYGUARD_DISABLE_TRUST_AGENTS");
                        Log.w(str, stringBuilder.toString());
                    }
                    i2++;
                    componentName2 = componentName3;
                }
                List<PersistableBundle> list = allAdminsHaveOptions ? result2 : null;
            }
        }
    }

    public void setRestrictionsProvider(ComponentName who, ComponentName permissionProvider) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            int userHandle = UserHandle.getCallingUserId();
            getUserData(userHandle).mRestrictionsProvider = permissionProvider;
            saveSettingsLocked(userHandle);
        }
    }

    public ComponentName getRestrictionsProvider(int userHandle) {
        ComponentName componentName;
        synchronized (getLockObject()) {
            if (isCallerWithSystemUid()) {
                DevicePolicyData userData = getUserData(userHandle);
                componentName = userData != null ? userData.mRestrictionsProvider : null;
            } else {
                throw new SecurityException("Only the system can query the permission provider");
            }
        }
        return componentName;
    }

    public void addCrossProfileIntentFilter(ComponentName who, IntentFilter filter, int flags) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            Injector injector;
            try {
                UserInfo parent = this.mUserManager.getProfileParent(callingUserId);
                if (parent == null) {
                    Slog.e(LOG_TAG, "Cannot call addCrossProfileIntentFilter if there is no parent");
                    this.mInjector.binderRestoreCallingIdentity(id);
                    return;
                }
                if ((flags & 1) != 0) {
                    this.mIPackageManager.addCrossProfileIntentFilter(filter, who.getPackageName(), callingUserId, parent.id, 0);
                }
                if ((flags & 2) != 0) {
                    this.mIPackageManager.addCrossProfileIntentFilter(filter, who.getPackageName(), parent.id, callingUserId, 0);
                }
                injector = this.mInjector;
                injector.binderRestoreCallingIdentity(id);
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public void clearCrossProfileIntentFilters(ComponentName who) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            Injector injector;
            try {
                UserInfo parent = this.mUserManager.getProfileParent(callingUserId);
                if (parent == null) {
                    Slog.e(LOG_TAG, "Cannot call clearCrossProfileIntentFilter if there is no parent");
                    this.mInjector.binderRestoreCallingIdentity(id);
                    return;
                }
                this.mIPackageManager.clearCrossProfileIntentFilters(callingUserId, who.getPackageName());
                this.mIPackageManager.clearCrossProfileIntentFilters(parent.id, who.getPackageName());
                injector = this.mInjector;
                injector.binderRestoreCallingIdentity(id);
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    private boolean checkPackagesInPermittedListOrSystem(List<String> enabledPackages, List<String> permittedList, int userIdToCheck) {
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo user = getUserInfo(userIdToCheck);
            if (user.isManagedProfile()) {
                userIdToCheck = user.profileGroupId;
            }
            Iterator it = enabledPackages.iterator();
            while (true) {
                boolean z = true;
                if (it.hasNext()) {
                    String enabledPackage = (String) it.next();
                    boolean systemService = false;
                    if ((this.mIPackageManager.getApplicationInfo(enabledPackage, 8192, userIdToCheck).flags & 1) == 0) {
                        z = false;
                    }
                    systemService = z;
                    if (!systemService && !permittedList.contains(enabledPackage)) {
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return false;
                    }
                } else {
                    this.mInjector.binderRestoreCallingIdentity(id);
                    return true;
                }
            }
        } catch (RemoteException e) {
            Log.i(LOG_TAG, "Can't talk to package managed", e);
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    private AccessibilityManager getAccessibilityManagerForUser(int userId) {
        IBinder iBinder = ServiceManager.getService("accessibility");
        return new AccessibilityManager(this.mContext, iBinder == null ? null : IAccessibilityManager.Stub.asInterface(iBinder), userId);
    }

    public boolean setPermittedAccessibilityServices(ComponentName who, List packageList) {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (packageList != null) {
            int userId = UserHandle.getCallingUserId();
            List<AccessibilityServiceInfo> enabledServices = null;
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                UserInfo user = getUserInfo(userId);
                if (user.isManagedProfile()) {
                    userId = user.profileGroupId;
                }
                enabledServices = getAccessibilityManagerForUser(userId).getEnabledAccessibilityServiceList(-1);
                if (enabledServices != null) {
                    List<String> enabledPackages = new ArrayList();
                    for (AccessibilityServiceInfo service : enabledServices) {
                        enabledPackages.add(service.getResolveInfo().serviceInfo.packageName);
                    }
                    if (!checkPackagesInPermittedListOrSystem(enabledPackages, packageList, userId)) {
                        Slog.e(LOG_TAG, "Cannot set permitted accessibility services, because it contains already enabled accesibility services.");
                        return z;
                    }
                }
            } finally {
                z = this.mInjector;
                z.binderRestoreCallingIdentity(id);
            }
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1).permittedAccessiblityServices = packageList;
            saveSettingsLocked(UserHandle.getCallingUserId());
        }
        return true;
    }

    public List getPermittedAccessibilityServices(ComponentName who) {
        if (!this.mHasFeature) {
            return null;
        }
        List list;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            list = getActiveAdminForCallerLocked(who, -1).permittedAccessiblityServices;
        }
        return list;
    }

    public List getPermittedAccessibilityServicesForUser(int userId) {
        if (!this.mHasFeature) {
            return null;
        }
        List<String> result;
        enforceManageUsers();
        synchronized (getLockObject()) {
            int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(userId);
            int length = profileIds.length;
            result = null;
            int result2 = 0;
            while (result2 < length) {
                DevicePolicyData policy = getUserDataUnchecked(profileIds[result2]);
                int N = policy.mAdminList.size();
                List<String> result3 = result;
                for (result = null; result < N; result++) {
                    List<String> fromAdmin = ((ActiveAdmin) policy.mAdminList.get(result)).permittedAccessiblityServices;
                    if (fromAdmin != null) {
                        if (result3 == null) {
                            result3 = new ArrayList(fromAdmin);
                        } else {
                            result3.retainAll(fromAdmin);
                        }
                    }
                }
                result2++;
                result = result3;
            }
            if (result != null) {
                long id = this.mInjector.binderClearCallingIdentity();
                try {
                    UserInfo user = getUserInfo(userId);
                    if (user.isManagedProfile()) {
                        userId = user.profileGroupId;
                    }
                    List<AccessibilityServiceInfo> installedServices = getAccessibilityManagerForUser(userId).getInstalledAccessibilityServiceList();
                    if (installedServices != null) {
                        for (AccessibilityServiceInfo service : installedServices) {
                            ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
                            if ((serviceInfo.applicationInfo.flags & 1) != 0) {
                                result.add(serviceInfo.packageName);
                            }
                        }
                    }
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
        }
        return result;
    }

    public boolean isAccessibilityServicePermittedByAdmin(ComponentName who, String packageName, int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkStringNotEmpty(packageName, "packageName is null");
        if (isCallerWithSystemUid()) {
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin == null) {
                    return false;
                } else if (admin.permittedAccessiblityServices == null) {
                    return true;
                } else {
                    boolean checkPackagesInPermittedListOrSystem = checkPackagesInPermittedListOrSystem(Collections.singletonList(packageName), admin.permittedAccessiblityServices, userHandle);
                    return checkPackagesInPermittedListOrSystem;
                }
            }
        }
        throw new SecurityException("Only the system can query if an accessibility service is disabled by admin");
    }

    private boolean checkCallerIsCurrentUserOrProfile() {
        int callingUserId = UserHandle.getCallingUserId();
        long token = this.mInjector.binderClearCallingIdentity();
        boolean z;
        try {
            UserInfo callingUser = getUserInfo(callingUserId);
            z = false;
            UserInfo currentUser = this.mInjector.getIActivityManager().getCurrentUser();
            if (callingUser.isManagedProfile() && callingUser.profileGroupId != currentUser.id) {
                Slog.e(LOG_TAG, "Cannot set permitted input methods for managed profile of a user that isn't the foreground user.");
                return z;
            } else if (callingUser.isManagedProfile() || callingUserId == currentUser.id) {
                this.mInjector.binderRestoreCallingIdentity(token);
                return true;
            } else {
                Slog.e(LOG_TAG, "Cannot set permitted input methods of a user that isn't the foreground user.");
                this.mInjector.binderRestoreCallingIdentity(token);
                return false;
            }
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to talk to activity managed.", e);
            return z;
        } finally {
            z = this.mInjector;
            z.binderRestoreCallingIdentity(token);
        }
    }

    public boolean setPermittedInputMethods(ComponentName who, List packageList) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (!checkCallerIsCurrentUserOrProfile()) {
            return false;
        }
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        if (packageList != null) {
            List<InputMethodInfo> enabledImes = ((InputMethodManager) this.mContext.getSystemService(InputMethodManager.class)).getEnabledInputMethodList();
            if (enabledImes != null) {
                List<String> enabledPackages = new ArrayList();
                for (InputMethodInfo ime : enabledImes) {
                    enabledPackages.add(ime.getPackageName());
                }
                if (!checkPackagesInPermittedListOrSystem(enabledPackages, packageList, callingUserId)) {
                    Slog.e(LOG_TAG, "Cannot set permitted input methods, because it contains already enabled input method.");
                    return false;
                }
            }
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1).permittedInputMethods = packageList;
            saveSettingsLocked(callingUserId);
        }
        return true;
    }

    public List getPermittedInputMethods(ComponentName who) {
        if (!this.mHasFeature) {
            return null;
        }
        List list;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            list = getActiveAdminForCallerLocked(who, -1).permittedInputMethods;
        }
        return list;
    }

    public List getPermittedInputMethodsForCurrentUser() {
        enforceManageUsers();
        try {
            List<String> result;
            int userId = this.mInjector.getIActivityManager().getCurrentUser().id;
            synchronized (getLockObject()) {
                int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(userId);
                int length = profileIds.length;
                result = null;
                int result2 = 0;
                while (result2 < length) {
                    DevicePolicyData policy = getUserDataUnchecked(profileIds[result2]);
                    int N = policy.mAdminList.size();
                    List<String> result3 = result;
                    for (result = null; result < N; result++) {
                        List<String> fromAdmin = ((ActiveAdmin) policy.mAdminList.get(result)).permittedInputMethods;
                        if (fromAdmin != null) {
                            if (result3 == null) {
                                result3 = new ArrayList(fromAdmin);
                            } else {
                                result3.retainAll(fromAdmin);
                            }
                        }
                    }
                    result2++;
                    result = result3;
                }
                if (result != null) {
                    List<InputMethodInfo> imes = ((InputMethodManager) this.mContext.getSystemService(InputMethodManager.class)).getInputMethodList();
                    long id = this.mInjector.binderClearCallingIdentity();
                    if (imes != null) {
                        try {
                            for (InputMethodInfo ime : imes) {
                                ServiceInfo serviceInfo = ime.getServiceInfo();
                                if ((serviceInfo.applicationInfo.flags & 1) != 0) {
                                    result.add(serviceInfo.packageName);
                                }
                            }
                        } catch (Throwable th) {
                            this.mInjector.binderRestoreCallingIdentity(id);
                        }
                    }
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
            return result;
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to make remote calls to get current user", e);
            return null;
        }
    }

    public boolean isInputMethodPermittedByAdmin(ComponentName who, String packageName, int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkStringNotEmpty(packageName, "packageName is null");
        if (isCallerWithSystemUid()) {
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin == null) {
                    return false;
                } else if (admin.permittedInputMethods == null) {
                    return true;
                } else {
                    boolean checkPackagesInPermittedListOrSystem = checkPackagesInPermittedListOrSystem(Collections.singletonList(packageName), admin.permittedInputMethods, userHandle);
                    return checkPackagesInPermittedListOrSystem;
                }
            }
        }
        throw new SecurityException("Only the system can query if an input method is disabled by admin");
    }

    public boolean setPermittedCrossProfileNotificationListeners(ComponentName who, List<String> packageList) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        if (!isManagedProfile(callingUserId)) {
            return false;
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1).permittedNotificationListeners = packageList;
            saveSettingsLocked(callingUserId);
        }
        return true;
    }

    public List<String> getPermittedCrossProfileNotificationListeners(ComponentName who) {
        if (!this.mHasFeature) {
            return null;
        }
        List list;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            list = getActiveAdminForCallerLocked(who, -1).permittedNotificationListeners;
        }
        return list;
    }

    /* JADX WARNING: Missing block: B:17:0x002f, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isNotificationListenerServicePermitted(String packageName, int userId) {
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkStringNotEmpty(packageName, "packageName is null or empty");
        if (isCallerWithSystemUid()) {
            synchronized (getLockObject()) {
                ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userId);
                if (profileOwner != null) {
                    if (profileOwner.permittedNotificationListeners != null) {
                        boolean checkPackagesInPermittedListOrSystem = checkPackagesInPermittedListOrSystem(Collections.singletonList(packageName), profileOwner.permittedNotificationListeners, userId);
                        return checkPackagesInPermittedListOrSystem;
                    }
                }
            }
        } else {
            throw new SecurityException("Only the system can query if a notification listener service is permitted");
        }
    }

    private void maybeSendAdminEnabledBroadcastLocked(int userHandle) {
        DevicePolicyData policyData = getUserData(userHandle);
        if (policyData.mAdminBroadcastPending) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userHandle);
            boolean clearInitBundle = true;
            if (admin != null) {
                PersistableBundle initBundle = policyData.mInitBundle;
                clearInitBundle = sendAdminCommandLocked(admin, "android.app.action.DEVICE_ADMIN_ENABLED", initBundle == null ? null : new Bundle(initBundle), null, true);
            }
            if (clearInitBundle) {
                policyData.mInitBundle = null;
                policyData.mAdminBroadcastPending = false;
                saveSettingsLocked(userHandle);
            }
        }
    }

    /* JADX WARNING: Missing block: B:79:0x0114, code skipped:
            if (r9 != null) goto L_0x0124;
     */
    /* JADX WARNING: Missing block: B:81:0x0118, code skipped:
            if (r4 >= 28) goto L_0x011b;
     */
    /* JADX WARNING: Missing block: B:82:0x011a, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:84:0x0123, code skipped:
            throw new android.os.ServiceSpecificException(1, "failed to create user");
     */
    /* JADX WARNING: Missing block: B:85:0x0124, code skipped:
            r5 = r9.getIdentifier();
            r1.mContext.sendBroadcastAsUser(new android.content.Intent("android.app.action.MANAGED_USER_CREATED").putExtra("android.intent.extra.user_handle", r5).putExtra("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", r8).setPackage(getManagedProvisioningPackage(r1.mContext)).addFlags(268435456), android.os.UserHandle.SYSTEM);
            r11 = r1.mInjector.binderClearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:88:0x015c, code skipped:
            r13 = r22.getPackageName();
     */
    /* JADX WARNING: Missing block: B:91:0x0163, code skipped:
            if (r1.mIPackageManager.isPackageAvailable(r13, r5) != false) goto L_0x016e;
     */
    /* JADX WARNING: Missing block: B:92:0x0165, code skipped:
            r1.mIPackageManager.installExistingPackageAsUser(r13, r5, 0, 1);
     */
    /* JADX WARNING: Missing block: B:105:0x019a, code skipped:
            if ((r26 & 1) == 0) goto L_0x01a9;
     */
    /* JADX WARNING: Missing block: B:107:?, code skipped:
            android.provider.Settings.Secure.putIntForUser(r1.mContext.getContentResolver(), "user_setup_complete", 1, r5);
     */
    /* JADX WARNING: Missing block: B:108:0x01a9, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r11);
     */
    /* JADX WARNING: Missing block: B:109:0x01af, code skipped:
            return r9;
     */
    /* JADX WARNING: Missing block: B:117:0x01b7, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:118:0x01b9, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:120:?, code skipped:
            r1.mUserManager.removeUser(r5);
     */
    /* JADX WARNING: Missing block: B:122:0x01c1, code skipped:
            if (r4 < 28) goto L_0x01c3;
     */
    /* JADX WARNING: Missing block: B:123:0x01c3, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r11);
     */
    /* JADX WARNING: Missing block: B:124:0x01c9, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:127:0x01d4, code skipped:
            throw new android.os.ServiceSpecificException(1, r0.getMessage());
     */
    /* JADX WARNING: Missing block: B:128:0x01d5, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r11);
     */
    /* JADX WARNING: Missing block: B:129:0x01da, code skipped:
            throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public UserHandle createAndManageUser(ComponentName admin, String name, ComponentName profileOwner, PersistableBundle adminExtras, int flags) {
        Throwable th;
        boolean z;
        ComponentName componentName = admin;
        ComponentName componentName2 = profileOwner;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.DEVICE_POLICY_CREATEANDMANAGEUSER);
        Preconditions.checkNotNull(componentName, "admin is null");
        Preconditions.checkNotNull(componentName2, "profileOwner is null");
        if (!admin.getPackageName().equals(profileOwner.getPackageName())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("profileOwner ");
            stringBuilder.append(componentName2);
            stringBuilder.append(" and admin ");
            stringBuilder.append(componentName);
            stringBuilder.append(" are not in the same package");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.mInjector.binderGetCallingUserHandle().isSystem()) {
            boolean ephemeral = (flags & 2) != 0;
            boolean z2 = (flags & 4) != 0 && UserManager.isDeviceInDemoMode(this.mContext);
            boolean demo = z2;
            boolean leaveAllSystemAppsEnabled = (flags & 16) != 0;
            synchronized (getLockObject()) {
                UserHandle user;
                try {
                    getActiveAdminForCallerLocked(componentName, -2);
                    int callingUid = this.mInjector.binderGetCallingUid();
                    long id = this.mInjector.binderClearCallingIdentity();
                    try {
                        int targetSdkVersion = this.mInjector.getPackageManagerInternal().getUidTargetSdkVersion(callingUid);
                        if (!((DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class)).isMemoryLow()) {
                            user = null;
                            try {
                                if (this.mUserManager.canAddMoreUsers()) {
                                    String[] disallowedPackages;
                                    int userInfoFlags = 0;
                                    if (ephemeral) {
                                        userInfoFlags = 0 | 256;
                                    }
                                    if (demo) {
                                        userInfoFlags |= 512;
                                    }
                                    if (leaveAllSystemAppsEnabled) {
                                        disallowedPackages = null;
                                    } else {
                                        try {
                                            disallowedPackages = (String[]) this.mOverlayPackagesProvider.getNonRequiredApps(componentName, UserHandle.myUserId(), "android.app.action.PROVISION_MANAGED_USER").toArray(new String[0]);
                                        } catch (Throwable th2) {
                                            th = th2;
                                            this.mInjector.binderRestoreCallingIdentity(id);
                                            throw th;
                                        }
                                    }
                                    UserInfo userInfo = this.mUserManagerInternal.createUserEvenWhenDisallowed(name, userInfoFlags, disallowedPackages);
                                    UserHandle user2;
                                    if (userInfo != null) {
                                        user2 = userInfo.getUserHandle();
                                    } else {
                                        user2 = user;
                                    }
                                    try {
                                        this.mInjector.binderRestoreCallingIdentity(id);
                                        int targetSdkVersion2 = targetSdkVersion;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        throw th;
                                    }
                                } else if (targetSdkVersion < 28) {
                                    try {
                                        this.mInjector.binderRestoreCallingIdentity(id);
                                        return null;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        z = ephemeral;
                                        throw th;
                                    }
                                } else {
                                    throw new ServiceSpecificException(6, "user limit reached");
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                z = ephemeral;
                                this.mInjector.binderRestoreCallingIdentity(id);
                                throw th;
                            }
                        } else if (targetSdkVersion < 28) {
                            try {
                                this.mInjector.binderRestoreCallingIdentity(id);
                                return null;
                            } catch (Throwable th6) {
                                th = th6;
                                z = ephemeral;
                                throw th;
                            }
                        } else {
                            try {
                                user = null;
                                try {
                                    throw new ServiceSpecificException(5, "low device storage");
                                } catch (Throwable th7) {
                                    th = th7;
                                    z = ephemeral;
                                    try {
                                        this.mInjector.binderRestoreCallingIdentity(id);
                                        throw th;
                                    } catch (Throwable th8) {
                                        th = th8;
                                        throw th;
                                    }
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                user = null;
                                z = ephemeral;
                                this.mInjector.binderRestoreCallingIdentity(id);
                                throw th;
                            }
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        z = ephemeral;
                        user = null;
                        this.mInjector.binderRestoreCallingIdentity(id);
                        throw th;
                    }
                } catch (Throwable th11) {
                    th = th11;
                    z = ephemeral;
                    user = null;
                    throw th;
                }
            }
        } else {
            throw new SecurityException("createAndManageUser was called from non-system user");
        }
        setActiveAdmin(componentName2, true, userHandle);
        String ownerName = getProfileOwnerName(Process.myUserHandle().getIdentifier());
        setProfileOwner(componentName2, ownerName, userHandle);
        synchronized (getLockObject()) {
            try {
                DevicePolicyData policyData = getUserData(userHandle);
                try {
                    policyData.mInitBundle = adminExtras;
                    policyData.mAdminBroadcastPending = true;
                    saveSettingsLocked(userHandle);
                } catch (Throwable th12) {
                    th = th12;
                    throw th;
                }
            } catch (Throwable th13) {
                th = th13;
                String str = ownerName;
                throw th;
            }
        }
    }

    public boolean removeUser(ComponentName who, UserHandle userHandle) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkNotNull(userHandle, "UserHandle is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        long id = this.mInjector.binderClearCallingIdentity();
        boolean z;
        try {
            String restriction;
            if (isManagedProfile(userHandle.getIdentifier())) {
                restriction = "no_remove_managed_profile";
            } else {
                restriction = "no_remove_user";
            }
            if (isAdminAffectedByRestriction(who, restriction, callingUserId)) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("The device owner cannot remove a user because ");
                stringBuilder.append(restriction);
                stringBuilder.append(" is enabled, and was not set by the device owner");
                Log.w(str, stringBuilder.toString());
                z = false;
                return z;
            }
            z = this.mUserManagerInternal.removeUserEvenWhenDisallowed(userHandle.getIdentifier());
            this.mInjector.binderRestoreCallingIdentity(id);
            return z;
        } finally {
            z = this.mInjector;
            z.binderRestoreCallingIdentity(id);
        }
    }

    private boolean isAdminAffectedByRestriction(ComponentName admin, String userRestriction, int userId) {
        int userRestrictionSource = this.mUserManager.getUserRestrictionSource(userRestriction, UserHandle.of(userId));
        if (userRestrictionSource == 0) {
            return false;
        }
        if (userRestrictionSource == 2) {
            return isDeviceOwner(admin, userId) ^ 1;
        }
        if (userRestrictionSource != 4) {
            return true;
        }
        return isProfileOwner(admin, userId) ^ 1;
    }

    public boolean switchUser(ComponentName who, UserHandle userHandle) {
        boolean switchUser;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
            long id = this.mInjector.binderClearCallingIdentity();
            RemoteException e = null;
            if (userHandle != null) {
                try {
                    e = userHandle.getIdentifier();
                } catch (RemoteException e2) {
                    try {
                        Log.e(LOG_TAG, "Couldn't switch user", e2);
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return false;
                    } catch (Throwable th) {
                        this.mInjector.binderRestoreCallingIdentity(id);
                    }
                }
            }
            switchUser = this.mInjector.getIActivityManager().switchUser(e2);
            this.mInjector.binderRestoreCallingIdentity(id);
        }
        return switchUser;
    }

    public int startUserInBackground(ComponentName who, UserHandle userHandle) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkNotNull(userHandle, "UserHandle is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        int userId = userHandle.getIdentifier();
        if (isManagedProfile(userId)) {
            Log.w(LOG_TAG, "Managed profile cannot be started in background");
            return 2;
        }
        long id = this.mInjector.binderClearCallingIdentity();
        int i = 1;
        try {
            if (!this.mInjector.getActivityManagerInternal().canStartMoreUsers()) {
                Log.w(LOG_TAG, "Cannot start more users in background");
                i = 3;
                return i;
            } else if (this.mInjector.getIActivityManager().startUserInBackground(userId)) {
                this.mInjector.binderRestoreCallingIdentity(id);
                return 0;
            } else {
                this.mInjector.binderRestoreCallingIdentity(id);
                return 1;
            }
        } catch (RemoteException e) {
            return i;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public int stopUser(ComponentName who, UserHandle userHandle) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkNotNull(userHandle, "UserHandle is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        int userId = userHandle.getIdentifier();
        if (!isManagedProfile(userId)) {
            return stopUserUnchecked(userId);
        }
        Log.w(LOG_TAG, "Managed profile cannot be stopped");
        return 2;
    }

    public int logoutUser(ComponentName who) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            if (isUserAffiliatedWithDeviceLocked(callingUserId)) {
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Admin ");
                stringBuilder.append(who);
                stringBuilder.append(" is neither the device owner or affiliated user's profile owner.");
                throw new SecurityException(stringBuilder.toString());
            }
        }
        if (isManagedProfile(callingUserId)) {
            Log.w(LOG_TAG, "Managed profile cannot be logout");
            return 2;
        }
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            if (this.mInjector.getIActivityManager().switchUser(0)) {
                this.mInjector.binderRestoreCallingIdentity(id);
                return stopUserUnchecked(callingUserId);
            }
            Log.w(LOG_TAG, "Failed to switch to primary user");
            return 1;
        } catch (RemoteException e) {
            return 1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    private int stopUserUnchecked(int userId) {
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            int stopUser = this.mInjector.getIActivityManager().stopUser(userId, true, null);
            if (stopUser == -2) {
                this.mInjector.binderRestoreCallingIdentity(id);
                return 4;
            } else if (stopUser != 0) {
                return 1;
            } else {
                this.mInjector.binderRestoreCallingIdentity(id);
                return 0;
            }
        } catch (RemoteException e) {
            return 1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public List<UserHandle> getSecondaryUsers(ComponentName who) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        long id = this.mInjector.binderClearCallingIdentity();
        List<UserHandle> userHandles;
        try {
            List<UserInfo> userInfos = this.mInjector.getUserManager().getUsers(true);
            userHandles = new ArrayList();
            for (UserInfo userInfo : userInfos) {
                UserHandle userHandle = userInfo.getUserHandle();
                if (!(userHandle.isSystem() || isManagedProfile(userHandle.getIdentifier()))) {
                    userHandles.add(userInfo.getUserHandle());
                }
            }
            return userHandles;
        } finally {
            userHandles = this.mInjector;
            userHandles.binderRestoreCallingIdentity(id);
        }
    }

    public boolean isEphemeralUser(ComponentName who) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
        }
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            boolean isUserEphemeral = this.mInjector.getUserManager().isUserEphemeral(callingUserId);
            return isUserEphemeral;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public Bundle getApplicationRestrictions(ComponentName who, String callerPackage, String packageName) {
        enforceCanManageScope(who, callerPackage, -1, "delegation-app-restrictions");
        UserHandle userHandle = this.mInjector.binderGetCallingUserHandle();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            Bundle bundle;
            Bundle bundle2 = this.mUserManager.getApplicationRestrictions(packageName, userHandle);
            if (bundle2 != null) {
                bundle = bundle2;
            } else {
                bundle = Bundle.EMPTY;
            }
            this.mInjector.binderRestoreCallingIdentity(id);
            return bundle;
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public String[] setPackagesSuspended(ComponentName who, String callerPackage, String[] packageNames, boolean suspended) {
        Throwable th;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            try {
                long id;
                try {
                    enforceCanManageScope(who, callerPackage, -1, "delegation-package-access");
                    id = this.mInjector.binderClearCallingIdentity();
                    try {
                        String[] packagesSuspendedAsUser = this.mIPackageManager.setPackagesSuspendedAsUser(packageNames, suspended, null, null, null, PackageManagerService.PLATFORM_PACKAGE_NAME, callingUserId);
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return packagesSuspendedAsUser;
                    } catch (RemoteException re) {
                        Slog.e(LOG_TAG, "Failed talking to the package manager", re);
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return packageNames;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                ComponentName componentName = who;
                String str = callerPackage;
                throw th;
            }
        }
    }

    public boolean isPackageSuspended(ComponentName who, String callerPackage, String packageName) {
        boolean isPackageSuspendedForUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(who, callerPackage, -1, "delegation-package-access");
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                isPackageSuspendedForUser = this.mIPackageManager.isPackageSuspendedForUser(packageName, callingUserId);
                this.mInjector.binderRestoreCallingIdentity(id);
            } catch (RemoteException re) {
                try {
                    Slog.e(LOG_TAG, "Failed talking to the package manager", re);
                    return false;
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
        }
        return isPackageSuspendedForUser;
    }

    public void setUserRestriction(ComponentName who, String key, boolean enabledFromThisOwner) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (UserRestrictionsUtils.isValidRestriction(key)) {
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(who, -1);
                StringBuilder stringBuilder;
                if (isDeviceOwner(who, userHandle)) {
                    if (!UserRestrictionsUtils.canDeviceOwnerChange(key)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Device owner cannot set user restriction ");
                        stringBuilder.append(key);
                        throw new SecurityException(stringBuilder.toString());
                    }
                } else if (!UserRestrictionsUtils.canProfileOwnerChange(key, userHandle)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Profile owner cannot set user restriction ");
                    stringBuilder.append(key);
                    throw new SecurityException(stringBuilder.toString());
                }
                Bundle restrictions = activeAdmin.ensureUserRestrictions();
                if (enabledFromThisOwner) {
                    restrictions.putBoolean(key, true);
                } else {
                    restrictions.remove(key);
                }
                saveUserRestrictionsLocked(userHandle);
            }
            if (SecurityLog.isLoggingEnabled()) {
                int eventTag;
                if (enabledFromThisOwner) {
                    eventTag = 210027;
                } else {
                    eventTag = 210028;
                }
                SecurityLog.writeEvent(eventTag, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), key});
            }
        }
    }

    private void saveUserRestrictionsLocked(int userId) {
        saveSettingsLocked(userId);
        pushUserRestrictions(userId);
        sendChangedNotification(userId);
    }

    private void pushUserRestrictions(int userId) {
        synchronized (getLockObject()) {
            Bundle userRestrictions;
            boolean isDeviceOwner = this.mOwners.isDeviceOwnerUserId(userId);
            boolean disallowCameraGlobally = false;
            ActiveAdmin deviceOwner;
            if (isDeviceOwner) {
                deviceOwner = getDeviceOwnerAdminLocked();
                if (deviceOwner == null) {
                    return;
                } else {
                    userRestrictions = deviceOwner.userRestrictions;
                    disallowCameraGlobally = deviceOwner.disableCamera;
                }
            } else {
                deviceOwner = getProfileOwnerAdminLocked(userId);
                userRestrictions = deviceOwner != null ? deviceOwner.userRestrictions : null;
            }
            this.mUserManagerInternal.setDevicePolicyUserRestrictions(userId, userRestrictions, isDeviceOwner, getCameraRestrictionScopeLocked(userId, disallowCameraGlobally));
        }
    }

    private int getCameraRestrictionScopeLocked(int userId, boolean disallowCameraGlobally) {
        if (disallowCameraGlobally) {
            return 2;
        }
        if (getCameraDisabled(null, userId, false)) {
            return 1;
        }
        return 0;
    }

    public Bundle getUserRestrictions(ComponentName who) {
        if (!this.mHasFeature) {
            return null;
        }
        Bundle bundle;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            bundle = getActiveAdminForCallerLocked(who, -1).userRestrictions;
        }
        return bundle;
    }

    public boolean setApplicationHidden(ComponentName who, String callerPackage, String packageName, boolean hidden) {
        boolean applicationHiddenSettingAsUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(who, callerPackage, -1, "delegation-package-access");
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                applicationHiddenSettingAsUser = this.mIPackageManager.setApplicationHiddenSettingAsUser(packageName, hidden, callingUserId);
                this.mInjector.binderRestoreCallingIdentity(id);
            } catch (RemoteException re) {
                try {
                    Slog.e(LOG_TAG, "Failed to setApplicationHiddenSetting", re);
                    return false;
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
        }
        return applicationHiddenSettingAsUser;
    }

    public boolean isApplicationHidden(ComponentName who, String callerPackage, String packageName) {
        boolean applicationHiddenSettingAsUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(who, callerPackage, -1, "delegation-package-access");
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                applicationHiddenSettingAsUser = this.mIPackageManager.getApplicationHiddenSettingAsUser(packageName, callingUserId);
                this.mInjector.binderRestoreCallingIdentity(id);
            } catch (RemoteException re) {
                try {
                    Slog.e(LOG_TAG, "Failed to getApplicationHiddenSettingAsUser", re);
                    return false;
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
        }
        return applicationHiddenSettingAsUser;
    }

    public void enableSystemApp(ComponentName who, String callerPackage, String packageName) {
        synchronized (getLockObject()) {
            Injector re;
            enforceCanManageScope(who, callerPackage, -1, "delegation-enable-system-app");
            boolean isDemo = isCurrentUserDemo();
            int userId = UserHandle.getCallingUserId();
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                int parentUserId = getProfileParentId(userId);
                if (!isDemo) {
                    if (!isSystemApp(this.mIPackageManager, packageName, parentUserId)) {
                        throw new IllegalArgumentException("Only system apps can be enabled this way.");
                    }
                }
                this.mIPackageManager.installExistingPackageAsUser(packageName, userId, 0, 1);
                if (isDemo) {
                    this.mIPackageManager.setApplicationEnabledSetting(packageName, 1, 1, userId, LOG_TAG);
                }
                re = this.mInjector;
            } catch (RemoteException re2) {
                try {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to install ");
                    stringBuilder.append(packageName);
                    Slog.wtf(str, stringBuilder.toString(), re2);
                    re = this.mInjector;
                } catch (Throwable th) {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
            re.binderRestoreCallingIdentity(id);
        }
    }

    public int enableSystemAppWithIntent(ComponentName who, String callerPackage, Intent intent) {
        Throwable th;
        Intent intent2 = intent;
        synchronized (getLockObject()) {
            try {
                long id;
                try {
                    enforceCanManageScope(who, callerPackage, -1, "delegation-enable-system-app");
                    int userId = UserHandle.getCallingUserId();
                    id = this.mInjector.binderClearCallingIdentity();
                    int i = 0;
                    try {
                        int parentUserId = getProfileParentId(userId);
                        List<ResolveInfo> activitiesToEnable = this.mIPackageManager.queryIntentActivities(intent2, intent2.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432, parentUserId).getList();
                        int numberOfAppsInstalled = 0;
                        if (activitiesToEnable != null) {
                            for (ResolveInfo info : activitiesToEnable) {
                                int parentUserId2;
                                if (info.activityInfo != null) {
                                    String packageName = info.activityInfo.packageName;
                                    if (isSystemApp(this.mIPackageManager, packageName, parentUserId)) {
                                        numberOfAppsInstalled++;
                                        parentUserId2 = parentUserId;
                                        this.mIPackageManager.installExistingPackageAsUser(packageName, userId, i, 1);
                                    } else {
                                        parentUserId2 = parentUserId;
                                        String str = LOG_TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Not enabling ");
                                        stringBuilder.append(packageName);
                                        stringBuilder.append(" since is not a system app");
                                        Slog.d(str, stringBuilder.toString());
                                    }
                                } else {
                                    parentUserId2 = parentUserId;
                                }
                                parentUserId = parentUserId2;
                                i = 0;
                            }
                        }
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return numberOfAppsInstalled;
                    } catch (RemoteException e) {
                        String str2 = LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failed to resolve intent for: ");
                        stringBuilder2.append(intent2);
                        Slog.wtf(str2, stringBuilder2.toString());
                        this.mInjector.binderRestoreCallingIdentity(id);
                        return 0;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                ComponentName componentName = who;
                String str3 = callerPackage;
                throw th;
            }
        }
    }

    private boolean isSystemApp(IPackageManager pm, String packageName, int userId) throws RemoteException {
        ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 8192, userId);
        if (appInfo != null) {
            return (appInfo.flags & 1) != 0;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The application ");
            stringBuilder.append(packageName);
            stringBuilder.append(" is not present on this device");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public boolean installExistingPackage(ComponentName who, String callerPackage, String packageName) {
        boolean z;
        synchronized (getLockObject()) {
            enforceCanManageScope(who, callerPackage, -1, "delegation-install-existing-package");
            int callingUserId = this.mInjector.userHandleGetCallingUserId();
            if (isUserAffiliatedWithDeviceLocked(callingUserId)) {
                long id = this.mInjector.binderClearCallingIdentity();
                z = false;
                try {
                    if (this.mIPackageManager.installExistingPackageAsUser(packageName, callingUserId, 0, 1) == 1) {
                        z = true;
                    }
                } catch (RemoteException e) {
                    return z;
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Admin ");
                stringBuilder.append(who);
                stringBuilder.append(" is neither the device owner or affiliated user's profile owner.");
                throw new SecurityException(stringBuilder.toString());
            }
        }
        return z;
    }

    public void setAccountManagementDisabled(ComponentName who, String accountType, boolean disabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            synchronized (getLockObject()) {
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, -1);
                if (disabled) {
                    ap.accountTypesWithManagementDisabled.add(accountType);
                } else {
                    ap.accountTypesWithManagementDisabled.remove(accountType);
                }
                saveSettingsLocked(UserHandle.getCallingUserId());
            }
        }
    }

    public String[] getAccountTypesWithManagementDisabled() {
        return getAccountTypesWithManagementDisabledAsUser(UserHandle.getCallingUserId());
    }

    public String[] getAccountTypesWithManagementDisabledAsUser(int userId) {
        enforceFullCrossUsersPermission(userId);
        if (!this.mHasFeature) {
            return null;
        }
        String[] strArr;
        synchronized (getLockObject()) {
            DevicePolicyData policy = getUserData(userId);
            int N = policy.mAdminList.size();
            ArraySet<String> resultSet = new ArraySet();
            for (int i = 0; i < N; i++) {
                resultSet.addAll(((ActiveAdmin) policy.mAdminList.get(i)).accountTypesWithManagementDisabled);
            }
            strArr = (String[]) resultSet.toArray(new String[resultSet.size()]);
        }
        return strArr;
    }

    public void setUninstallBlocked(ComponentName who, String callerPackage, String packageName, boolean uninstallBlocked) {
        int userId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            Injector re;
            enforceCanManageScope(who, callerPackage, -1, "delegation-block-uninstall");
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.setBlockUninstallForUser(packageName, uninstallBlocked, userId);
                re = this.mInjector;
            } catch (RemoteException re2) {
                try {
                    Slog.e(LOG_TAG, "Failed to setBlockUninstallForUser", re2);
                    re = this.mInjector;
                } catch (Throwable th) {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
            re.binderRestoreCallingIdentity(id);
        }
    }

    public boolean isUninstallBlocked(ComponentName who, String packageName) {
        boolean blockUninstallForUser;
        int userId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    getActiveAdminForCallerLocked(who, -1);
                } catch (Throwable th) {
                }
            }
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                blockUninstallForUser = this.mIPackageManager.getBlockUninstallForUser(packageName, userId);
                this.mInjector.binderRestoreCallingIdentity(id);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Failed to getBlockUninstallForUser", re);
                this.mInjector.binderRestoreCallingIdentity(id);
                return false;
            }
        }
        return blockUninstallForUser;
    }

    public void setCrossProfileCallerIdDisabled(ComponentName who, boolean disabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
                if (admin.disableCallerId != disabled) {
                    admin.disableCallerId = disabled;
                    saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
                }
            }
        }
    }

    public boolean getCrossProfileCallerIdDisabled(ComponentName who) {
        if (!this.mHasFeature) {
            return false;
        }
        boolean z;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            z = getActiveAdminForCallerLocked(who, -1).disableCallerId;
        }
        return z;
    }

    public boolean getCrossProfileCallerIdDisabledForUser(int userId) {
        boolean z;
        enforceCrossUsersPermission(userId);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableCallerId : false;
        }
        return z;
    }

    public void setCrossProfileContactsSearchDisabled(ComponentName who, boolean disabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
                if (admin.disableContactsSearch != disabled) {
                    admin.disableContactsSearch = disabled;
                    saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
                }
            }
        }
    }

    public boolean getCrossProfileContactsSearchDisabled(ComponentName who) {
        if (!this.mHasFeature) {
            return false;
        }
        boolean z;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            z = getActiveAdminForCallerLocked(who, -1).disableContactsSearch;
        }
        return z;
    }

    public boolean getCrossProfileContactsSearchDisabledForUser(int userId) {
        boolean z;
        enforceCrossUsersPermission(userId);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableContactsSearch : false;
        }
        return z;
    }

    public void startManagedQuickContact(String actualLookupKey, long actualContactId, boolean isContactIdIgnored, long actualDirectoryId, Intent originalIntent) {
        Intent intent = QuickContact.rebuildManagedQuickContactsIntent(actualLookupKey, actualContactId, isContactIdIgnored, actualDirectoryId, originalIntent);
        int callingUserId = UserHandle.getCallingUserId();
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                int managedUserId = getManagedUserId(callingUserId);
                if (managedUserId < 0) {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                } else if (isCrossProfileQuickContactDisabled(managedUserId)) {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                } else {
                    ContactsInternal.startQuickContactWithErrorToastForUser(this.mContext, intent, new UserHandle(managedUserId));
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            }
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private boolean isCrossProfileQuickContactDisabled(int userId) {
        return getCrossProfileCallerIdDisabledForUser(userId) && getCrossProfileContactsSearchDisabledForUser(userId);
    }

    public int getManagedUserId(int callingUserId) {
        for (UserInfo ui : this.mUserManager.getProfiles(callingUserId)) {
            if (ui.id != callingUserId) {
                if (ui.isManagedProfile()) {
                    return ui.id;
                }
            }
        }
        return -1;
    }

    public void setBluetoothContactSharingDisabled(ComponentName who, boolean disabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
                if (admin.disableBluetoothContactSharing != disabled) {
                    admin.disableBluetoothContactSharing = disabled;
                    saveSettingsLocked(UserHandle.getCallingUserId());
                }
            }
        }
    }

    public boolean getBluetoothContactSharingDisabled(ComponentName who) {
        if (!this.mHasFeature) {
            return false;
        }
        boolean z;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            z = getActiveAdminForCallerLocked(who, -1).disableBluetoothContactSharing;
        }
        return z;
    }

    public boolean getBluetoothContactSharingDisabledForUser(int userId) {
        boolean z;
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableBluetoothContactSharing : false;
        }
        return z;
    }

    public void setLockTaskPackages(ComponentName who, String[] packages) throws SecurityException {
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkNotNull(packages, "packages is null");
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(who);
            setLockTaskPackagesLocked(this.mInjector.userHandleGetCallingUserId(), new ArrayList(Arrays.asList(packages)));
        }
    }

    private void setLockTaskPackagesLocked(int userHandle, List<String> packages) {
        getUserData(userHandle).mLockTaskPackages = packages;
        saveSettingsLocked(userHandle);
        updateLockTaskPackagesLocked(packages, userHandle);
    }

    public String[] getLockTaskPackages(ComponentName who) {
        String[] strArr;
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.binderGetCallingUserHandle().getIdentifier();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(who);
            List<String> packages = getUserData(userHandle).mLockTaskPackages;
            strArr = (String[]) packages.toArray(new String[packages.size()]);
        }
        return strArr;
    }

    public boolean isLockTaskPermitted(String pkg) {
        boolean contains;
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            contains = getUserData(userHandle).mLockTaskPackages.contains(pkg);
        }
        return contains;
    }

    public void setLockTaskFeatures(ComponentName who, int flags) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        boolean z = false;
        boolean hasHome = (flags & 4) != 0;
        boolean z2 = hasHome || !((flags & 8) != 0);
        Preconditions.checkArgument(z2, "Cannot use LOCK_TASK_FEATURE_OVERVIEW without LOCK_TASK_FEATURE_HOME");
        z2 = (flags & 2) != 0;
        if (hasHome || !z2) {
            z = true;
        }
        Preconditions.checkArgument(z, "Cannot use LOCK_TASK_FEATURE_NOTIFICATIONS without LOCK_TASK_FEATURE_HOME");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(who);
            setLockTaskFeaturesLocked(userHandle, flags);
        }
    }

    private void setLockTaskFeaturesLocked(int userHandle, int flags) {
        getUserData(userHandle).mLockTaskFeatures = flags;
        saveSettingsLocked(userHandle);
        updateLockTaskFeaturesLocked(flags, userHandle);
    }

    public int getLockTaskFeatures(ComponentName who) {
        int i;
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(who);
            i = getUserData(userHandle).mLockTaskFeatures;
        }
        return i;
    }

    private void maybeClearLockTaskPolicyLocked() {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            List<UserInfo> userInfos = this.mUserManager.getUsers(true);
            int i = userInfos.size() - 1;
            while (true) {
                int i2 = i;
                if (i2 < 0) {
                    break;
                }
                i = ((UserInfo) userInfos.get(i2)).id;
                if (!canUserUseLockTaskLocked(i)) {
                    if (!getUserData(i).mLockTaskPackages.isEmpty()) {
                        String str = LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("User id ");
                        stringBuilder.append(i);
                        stringBuilder.append(" not affiliated. Clearing lock task packages");
                        Slog.d(str, stringBuilder.toString());
                        setLockTaskPackagesLocked(i, Collections.emptyList());
                    }
                    if (getUserData(i).mLockTaskFeatures != 0) {
                        String str2 = LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("User id ");
                        stringBuilder2.append(i);
                        stringBuilder2.append(" not affiliated. Clearing lock task features");
                        Slog.d(str2, stringBuilder2.toString());
                        setLockTaskFeaturesLocked(i, 0);
                    }
                }
                i = i2 - 1;
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userHandle) {
        if (isCallerWithSystemUid()) {
            synchronized (getLockObject()) {
                DevicePolicyData policy = getUserData(userHandle);
                if (policy.mStatusBarDisabled) {
                    setStatusBarDisabledInternal(isEnabled ^ 1, userHandle);
                }
                Bundle adminExtras = new Bundle();
                adminExtras.putString("android.app.extra.LOCK_TASK_PACKAGE", pkg);
                Iterator it = policy.mAdminList.iterator();
                while (it.hasNext()) {
                    ActiveAdmin admin = (ActiveAdmin) it.next();
                    boolean ownsDevice = isDeviceOwner(admin.info.getComponent(), userHandle);
                    boolean ownsProfile = isProfileOwner(admin.info.getComponent(), userHandle);
                    if (ownsDevice || ownsProfile) {
                        if (isEnabled) {
                            sendAdminCommandLocked(admin, "android.app.action.LOCK_TASK_ENTERING", adminExtras, null);
                        } else {
                            sendAdminCommandLocked(admin, "android.app.action.LOCK_TASK_EXITING");
                        }
                    }
                }
            }
            return;
        }
        throw new SecurityException("notifyLockTaskModeChanged can only be called by system");
    }

    public void setGlobalSetting(ComponentName who, String setting, String value) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
            if (GLOBAL_SETTINGS_DEPRECATED.contains(setting)) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Global setting no longer supported: ");
                stringBuilder.append(setting);
                Log.i(str, stringBuilder.toString());
                return;
            }
            long timeMs;
            if (!GLOBAL_SETTINGS_WHITELIST.contains(setting)) {
                if (!UserManager.isDeviceInDemoMode(this.mContext)) {
                    throw new SecurityException(String.format("Permission denial: device owners cannot update %1$s", new Object[]{setting}));
                }
            }
            if ("stay_on_while_plugged_in".equals(setting)) {
                timeMs = getMaximumTimeToLock(who, this.mInjector.userHandleGetCallingUserId(), false);
                if (timeMs > 0 && timeMs < JobStatus.NO_LATEST_RUNTIME) {
                    return;
                }
            }
            timeMs = this.mInjector.binderClearCallingIdentity();
            try {
                this.mInjector.settingsGlobalPutString(setting, value);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(timeMs);
            }
        }
    }

    public void setSystemSetting(ComponentName who, String setting, String value) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkStringNotEmpty(setting, "String setting is null or empty");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            if (SYSTEM_SETTINGS_WHITELIST.contains(setting)) {
                this.mInjector.binderWithCleanCallingIdentity(new -$$Lambda$DevicePolicyManagerService$mignzFcOqIvnBFOYi8O3tmqXI68(this, setting, value, this.mInjector.userHandleGetCallingUserId()));
            } else {
                throw new SecurityException(String.format("Permission denial: device owners cannot update %1$s", new Object[]{setting}));
            }
        }
    }

    public boolean setTime(ComponentName who, long millis) {
        Preconditions.checkNotNull(who, "ComponentName is null in setTime");
        getActiveAdminForCallerLocked(who, -2);
        if (this.mInjector.settingsGlobalGetInt("auto_time", 0) == 1) {
            return false;
        }
        this.mInjector.binderWithCleanCallingIdentity(new -$$Lambda$DevicePolicyManagerService$zqf4q6-7wkQreppEUOBfp0NE94M(this, millis));
        return true;
    }

    public boolean setTimeZone(ComponentName who, String timeZone) {
        Preconditions.checkNotNull(who, "ComponentName is null in setTimeZone");
        getActiveAdminForCallerLocked(who, -2);
        if (this.mInjector.settingsGlobalGetInt("auto_time_zone", 0) == 1) {
            return false;
        }
        this.mInjector.binderWithCleanCallingIdentity(new -$$Lambda$DevicePolicyManagerService$1qc4cD7h8K2CVmZeyPCWra8TVtQ(this, timeZone));
        return true;
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:27:0x009d, B:41:0x00dc] */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r3 = LOG_TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Invalid value: ");
            r4.append(r10);
            r4.append(" for setting ");
            r4.append(r9);
            android.util.Slog.e(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:54:0x010e, code skipped:
            r7.mInjector.binderRestoreCallingIdentity(r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setSecureSetting(ComponentName who, String setting, String value) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            boolean z = false;
            if (isDeviceOwner(who, callingUserId)) {
                if (!SECURE_SETTINGS_DEVICEOWNER_WHITELIST.contains(setting)) {
                    if (!isCurrentUserDemo()) {
                        throw new SecurityException(String.format("Permission denial: Device owners cannot update %1$s", new Object[]{setting}));
                    }
                }
            } else if (!SECURE_SETTINGS_WHITELIST.contains(setting)) {
                if (!isCurrentUserDemo()) {
                    throw new SecurityException(String.format("Permission denial: Profile owners cannot update %1$s", new Object[]{setting}));
                }
            }
            String str;
            if (!setting.equals("install_non_market_apps")) {
                long id = this.mInjector.binderClearCallingIdentity();
                if ("default_input_method".equals(setting)) {
                    if (!TextUtils.equals(this.mInjector.settingsSecureGetStringForUser("default_input_method", callingUserId), value)) {
                        this.mSetupContentObserver.addPendingChangeByOwnerLocked(callingUserId);
                    }
                    getUserData(callingUserId).mCurrentInputMethodSet = true;
                    saveSettingsLocked(callingUserId);
                }
                this.mInjector.settingsSecurePutStringForUser(setting, value, callingUserId);
                this.mInjector.binderRestoreCallingIdentity(id);
                return;
            } else if (getTargetSdk(who.getPackageName(), callingUserId) >= 26) {
                throw new UnsupportedOperationException("install_non_market_apps is deprecated. Please use the user restriction no_install_unknown_sources instead.");
            } else if (this.mUserManager.isManagedProfile(callingUserId)) {
                str = "no_install_unknown_sources";
                if (Integer.parseInt(value) == 0) {
                    z = true;
                }
                setUserRestriction(who, str, z);
            } else {
                str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring setSecureSetting request for ");
                stringBuilder.append(setting);
                stringBuilder.append(". User restriction ");
                stringBuilder.append("no_install_unknown_sources");
                stringBuilder.append(" should be used instead.");
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    public void setMasterVolumeMuted(ComponentName who, boolean on) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            setUserRestriction(who, "disallow_unmute_device", on);
        }
    }

    public boolean isMasterVolumeMuted(ComponentName who) {
        boolean isMasterMute;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            isMasterMute = ((AudioManager) this.mContext.getSystemService("audio")).isMasterMute();
        }
        return isMasterMute;
    }

    public void setUserIcon(ComponentName who, Bitmap icon) {
        synchronized (getLockObject()) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            getActiveAdminForCallerLocked(who, -1);
            int userId = UserHandle.getCallingUserId();
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mUserManagerInternal.setUserIcon(userId, icon);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public boolean setKeyguardDisabled(ComponentName who, boolean disabled) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            if (isUserAffiliatedWithDeviceLocked(userId)) {
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Admin ");
                stringBuilder.append(who);
                stringBuilder.append(" is neither the device owner or affiliated user's profile owner.");
                throw new SecurityException(stringBuilder.toString());
            }
        }
        if (isManagedProfile(userId)) {
            throw new SecurityException("Managed profile cannot disable keyguard");
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        if (disabled) {
            try {
                if (this.mLockPatternUtils.isSecure(userId)) {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    return false;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        this.mLockPatternUtils.setLockScreenDisabled(disabled, userId);
        this.mInjector.getIWindowManager().dismissKeyguard(null, null);
        this.mInjector.binderRestoreCallingIdentity(ident);
        return true;
    }

    /* JADX WARNING: Missing block: B:28:0x004d, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setStatusBarDisabled(ComponentName who, boolean disabled) {
        int userId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -1);
            if (!isUserAffiliatedWithDeviceLocked(userId)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Admin ");
                stringBuilder.append(who);
                stringBuilder.append(" is neither the device owner or affiliated user's profile owner.");
                throw new SecurityException(stringBuilder.toString());
            } else if (isManagedProfile(userId)) {
                throw new SecurityException("Managed profile cannot disable status bar");
            } else {
                DevicePolicyData policy = getUserData(userId);
                if (policy.mStatusBarDisabled != disabled) {
                    boolean isLockTaskMode = false;
                    try {
                        isLockTaskMode = this.mInjector.getIActivityManager().getLockTaskModeState() != 0;
                    } catch (RemoteException e) {
                        Slog.e(LOG_TAG, "Failed to get LockTask mode");
                    }
                    if (isLockTaskMode || setStatusBarDisabledInternal(disabled, userId)) {
                        policy.mStatusBarDisabled = disabled;
                        saveSettingsLocked(userId);
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    private boolean setStatusBarDisabledInternal(boolean disabled, int userId) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            IStatusBarService statusBarService = IStatusBarService.Stub.asInterface(ServiceManager.checkService(TAG_STATUS_BAR));
            if (statusBarService != null) {
                boolean flags2 = disabled;
                statusBarService.disableForUser(disabled ? STATUS_BAR_DISABLE_MASK : 0, this.mToken, this.mContext.getPackageName(), userId);
                statusBarService.disable2ForUser(flags2, this.mToken, this.mContext.getPackageName(), userId);
                this.mInjector.binderRestoreCallingIdentity(ident);
                return true;
            }
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to disable the status bar", e);
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
        this.mInjector.binderRestoreCallingIdentity(ident);
        return false;
    }

    void updateUserSetupCompleteAndPaired() {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        int N = users.size();
        for (int i = 0; i < N; i++) {
            DevicePolicyData policy;
            int userHandle = ((UserInfo) users.get(i)).id;
            if (this.mInjector.settingsSecureGetIntForUser("user_setup_complete", 0, userHandle) != 0) {
                policy = getUserData(userHandle);
                if (!policy.mUserSetupComplete) {
                    policy.mUserSetupComplete = true;
                    synchronized (getLockObject()) {
                        saveSettingsLocked(userHandle);
                    }
                }
            }
            if (this.mIsWatch && this.mInjector.settingsSecureGetIntForUser("device_paired", 0, userHandle) != 0) {
                policy = getUserData(userHandle);
                if (policy.mPaired) {
                    continue;
                } else {
                    policy.mPaired = true;
                    synchronized (getLockObject()) {
                        saveSettingsLocked(userHandle);
                    }
                }
            }
        }
    }

    private Intent createShowAdminSupportIntent(ComponentName admin, int userId) {
        Intent intent = new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS");
        intent.putExtra("android.intent.extra.USER_ID", userId);
        intent.putExtra("android.app.extra.DEVICE_ADMIN", admin);
        intent.setFlags(268435456);
        return intent;
    }

    public Intent createAdminSupportIntent(String restriction) {
        Preconditions.checkNotNull(restriction);
        int userId = UserHandle.getUserId(this.mInjector.binderGetCallingUid());
        Intent intent = null;
        if ("policy_disable_camera".equals(restriction) || "policy_disable_screen_capture".equals(restriction) || "policy_mandatory_backups".equals(restriction)) {
            synchronized (getLockObject()) {
                DevicePolicyData policy = getUserData(userId);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin = (ActiveAdmin) policy.mAdminList.get(i);
                    if ((admin.disableCamera && "policy_disable_camera".equals(restriction)) || ((admin.disableScreenCapture && "policy_disable_screen_capture".equals(restriction)) || (admin.mandatoryBackupTransport != null && "policy_mandatory_backups".equals(restriction)))) {
                        intent = createShowAdminSupportIntent(admin.info.getComponent(), userId);
                        break;
                    }
                }
                if (intent == null && "policy_disable_camera".equals(restriction)) {
                    ActiveAdmin admin2 = getDeviceOwnerAdminLocked();
                    if (admin2 != null && admin2.disableCamera) {
                        intent = createShowAdminSupportIntent(admin2.info.getComponent(), this.mOwners.getDeviceOwnerUserId());
                    }
                }
            }
        } else {
            intent = this.mLocalService.createUserRestrictionSupportIntent(userId, restriction);
        }
        if (intent != null) {
            intent.putExtra("android.app.extra.RESTRICTION", restriction);
        }
        return intent;
    }

    private static boolean isLimitPasswordAllowed(ActiveAdmin admin, int minPasswordQuality) {
        if (admin.minimumPasswordMetrics.quality < minPasswordQuality) {
            return false;
        }
        return admin.info.usesPolicy(0);
    }

    public void setSystemUpdatePolicy(ComponentName who, SystemUpdatePolicy policy) {
        if (policy != null) {
            policy.validateType();
            policy.validateFreezePeriods();
            Pair<LocalDate, LocalDate> record = this.mOwners.getSystemUpdateFreezePeriodRecord();
            policy.validateAgainstPreviousFreezePeriod((LocalDate) record.first, (LocalDate) record.second, LocalDate.now());
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
            if (policy == null) {
                this.mOwners.clearSystemUpdatePolicy();
            } else {
                this.mOwners.setSystemUpdatePolicy(policy);
                updateSystemUpdateFreezePeriodsRecord(false);
            }
            this.mOwners.writeDeviceOwner();
        }
        this.mContext.sendBroadcastAsUser(new Intent("android.app.action.SYSTEM_UPDATE_POLICY_CHANGED"), UserHandle.SYSTEM);
    }

    /* JADX WARNING: Missing block: B:11:0x001e, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SystemUpdatePolicy getSystemUpdatePolicy() {
        synchronized (getLockObject()) {
            SystemUpdatePolicy policy = this.mOwners.getSystemUpdatePolicy();
            if (policy == null || policy.isValid()) {
            } else {
                Slog.w(LOG_TAG, "Stored system update policy is invalid, return null instead.");
                return null;
            }
        }
    }

    private static boolean withinRange(Pair<LocalDate, LocalDate> range, LocalDate date) {
        return (date.isBefore((ChronoLocalDate) range.first) || date.isAfter((ChronoLocalDate) range.second)) ? false : true;
    }

    /* JADX WARNING: Missing block: B:36:0x008d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateSystemUpdateFreezePeriodsRecord(boolean saveIfChanged) {
        Slog.d(LOG_TAG, "updateSystemUpdateFreezePeriodsRecord");
        synchronized (getLockObject()) {
            SystemUpdatePolicy policy = this.mOwners.getSystemUpdatePolicy();
            if (policy == null) {
                return;
            }
            LocalDate now = LocalDate.now();
            Pair<LocalDate, LocalDate> currentPeriod = policy.getCurrentFreezePeriod(now);
            if (currentPeriod == null) {
                return;
            }
            boolean changed;
            Pair<LocalDate, LocalDate> record = this.mOwners.getSystemUpdateFreezePeriodRecord();
            LocalDate start = record.first;
            LocalDate end = record.second;
            if (end != null) {
                if (start != null) {
                    changed = now.equals(end.plusDays(1)) ? this.mOwners.setSystemUpdateFreezePeriodRecord(start, now) : now.isAfter(end.plusDays(1)) ? (withinRange(currentPeriod, start) && withinRange(currentPeriod, end)) ? this.mOwners.setSystemUpdateFreezePeriodRecord(start, now) : this.mOwners.setSystemUpdateFreezePeriodRecord(now, now) : now.isBefore(start) ? this.mOwners.setSystemUpdateFreezePeriodRecord(now, now) : false;
                    if (changed && saveIfChanged) {
                        this.mOwners.writeDeviceOwner();
                    }
                }
            }
            changed = this.mOwners.setSystemUpdateFreezePeriodRecord(now, now);
            this.mOwners.writeDeviceOwner();
        }
    }

    public void clearSystemUpdatePolicyFreezePeriodRecord() {
        enforceShell("clearSystemUpdatePolicyFreezePeriodRecord");
        synchronized (getLockObject()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Clear freeze period record: ");
            stringBuilder.append(this.mOwners.getSystemUpdateFreezePeriodRecordAsString());
            Slog.i(str, stringBuilder.toString());
            if (this.mOwners.setSystemUpdateFreezePeriodRecord(null, null)) {
                this.mOwners.writeDeviceOwner();
            }
        }
    }

    @VisibleForTesting
    boolean isCallerDeviceOwner(int callerUid) {
        synchronized (getLockObject()) {
            if (!this.mOwners.hasDeviceOwner()) {
                return false;
            } else if (UserHandle.getUserId(callerUid) != this.mOwners.getDeviceOwnerUserId()) {
                return false;
            } else {
                String deviceOwnerPackageName = this.mOwners.getDeviceOwnerComponent().getPackageName();
                try {
                    for (String pkg : this.mInjector.getIPackageManager().getPackagesForUid(callerUid)) {
                        if (deviceOwnerPackageName.equals(pkg)) {
                            return true;
                        }
                    }
                    return false;
                } catch (RemoteException e) {
                    return false;
                }
            }
        }
    }

    public void notifyPendingSystemUpdate(SystemUpdateInfo info) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NOTIFY_PENDING_SYSTEM_UPDATE", "Only the system update service can broadcast update information");
        if (UserHandle.getCallingUserId() != 0) {
            Slog.w(LOG_TAG, "Only the system update service in the system user can broadcast update information.");
        } else if (this.mOwners.saveSystemUpdateInfo(info)) {
            Intent intent = new Intent("android.app.action.NOTIFY_PENDING_SYSTEM_UPDATE").putExtra("android.app.extra.SYSTEM_UPDATE_RECEIVED_TIME", info == null ? -1 : info.getReceivedTime());
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                synchronized (getLockObject()) {
                    if (this.mOwners.hasDeviceOwner()) {
                        UserHandle deviceOwnerUser = UserHandle.of(this.mOwners.getDeviceOwnerUserId());
                        intent.setComponent(this.mOwners.getDeviceOwnerComponent());
                        this.mContext.sendBroadcastAsUser(intent, deviceOwnerUser);
                    }
                }
                try {
                    for (int userId : this.mInjector.getIActivityManager().getRunningUserIds()) {
                        synchronized (getLockObject()) {
                            ComponentName profileOwnerPackage = this.mOwners.getProfileOwnerComponent(userId);
                            if (profileOwnerPackage != null) {
                                intent.setComponent(profileOwnerPackage);
                                this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
                            }
                        }
                    }
                    this.mInjector.binderRestoreCallingIdentity(ident);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Could not retrieve the list of running users", e);
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    public SystemUpdateInfo getPendingSystemUpdate(ComponentName admin) {
        Preconditions.checkNotNull(admin, "ComponentName is null");
        enforceProfileOrDeviceOwner(admin);
        return this.mOwners.getSystemUpdateInfo();
    }

    public void setPermissionPolicy(ComponentName admin, String callerPackage, int policy) throws RemoteException {
        int userId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(admin, callerPackage, -1, "delegation-permission-grant");
            DevicePolicyData userPolicy = getUserData(userId);
            if (userPolicy.mPermissionPolicy != policy) {
                userPolicy.mPermissionPolicy = policy;
                saveSettingsLocked(userId);
            }
        }
    }

    public int getPermissionPolicy(ComponentName admin) throws RemoteException {
        int i;
        int userId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            i = getUserData(userId).mPermissionPolicy;
        }
        return i;
    }

    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r13);
     */
    /* JADX WARNING: Missing block: B:37:0x0098, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setPermissionGrantState(ComponentName admin, String callerPackage, String packageName, String permission, int grantState) throws RemoteException {
        long ident;
        Throwable th;
        String str = packageName;
        String str2 = permission;
        UserHandle user = this.mInjector.binderGetCallingUserHandle();
        synchronized (getLockObject()) {
            enforceCanManageScope(admin, callerPackage, -1, "delegation-permission-grant");
            long ident2 = this.mInjector.binderClearCallingIdentity();
            try {
                if (getTargetSdk(str, user.getIdentifier()) >= 23) {
                    if (isRuntimePermission(str2)) {
                        PackageManager packageManager = this.mInjector.getPackageManager();
                        switch (grantState) {
                            case 0:
                                ident = ident2;
                                packageManager.updatePermissionFlags(str2, str, 4, 0, user);
                                break;
                            case 1:
                                ident = ident2;
                                this.mInjector.getPackageManagerInternal().grantRuntimePermission(str, str2, user.getIdentifier(), true);
                                packageManager.updatePermissionFlags(str2, str, 4, 4, user);
                                break;
                            case 2:
                                this.mInjector.getPackageManagerInternal().revokeRuntimePermission(str, str2, user.getIdentifier(), true);
                                ident = ident2;
                                try {
                                    packageManager.updatePermissionFlags(str2, str, 4, 4, user);
                                    break;
                                } catch (SecurityException e) {
                                    this.mInjector.binderRestoreCallingIdentity(ident);
                                    return false;
                                } catch (NameNotFoundException e2) {
                                    this.mInjector.binderRestoreCallingIdentity(ident);
                                    return false;
                                } catch (Throwable th2) {
                                    th = th2;
                                    this.mInjector.binderRestoreCallingIdentity(ident);
                                    throw th;
                                }
                            default:
                                ident = ident2;
                                break;
                        }
                    }
                    this.mInjector.binderRestoreCallingIdentity(ident2);
                    return false;
                }
                this.mInjector.binderRestoreCallingIdentity(ident2);
                return false;
            } catch (SecurityException e3) {
                ident = ident2;
                this.mInjector.binderRestoreCallingIdentity(ident);
                return false;
            } catch (NameNotFoundException e4) {
                ident = ident2;
                this.mInjector.binderRestoreCallingIdentity(ident);
                return false;
            } catch (Throwable th3) {
                th = th3;
                ident = ident2;
                this.mInjector.binderRestoreCallingIdentity(ident);
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x003d, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getPermissionGrantState(ComponentName admin, String callerPackage, String packageName, String permission) throws RemoteException {
        PackageManager packageManager = this.mInjector.getPackageManager();
        UserHandle user = this.mInjector.binderGetCallingUserHandle();
        if (!isCallerWithSystemUid()) {
            enforceCanManageScope(admin, callerPackage, -1, "delegation-permission-grant");
        }
        synchronized (getLockObject()) {
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                int granted = this.mIPackageManager.checkPermission(permission, packageName, user.getIdentifier());
                if ((packageManager.getPermissionFlags(permission, packageName, user) & 4) == 4) {
                    int i;
                    if (granted == 0) {
                        i = 1;
                    } else {
                        i = 2;
                    }
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    return i;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    boolean isPackageInstalledForUser(String packageName, int userHandle) {
        try {
            PackageInfo pi = this.mInjector.getIPackageManager().getPackageInfo(packageName, 0, userHandle);
            if (pi == null || pi.applicationInfo.flags == 0) {
                return false;
            }
            return true;
        } catch (RemoteException re) {
            throw new RuntimeException("Package manager has died", re);
        }
    }

    public boolean isRuntimePermission(String permissionName) throws NameNotFoundException {
        if ((this.mInjector.getPackageManager().getPermissionInfo(permissionName, 0).protectionLevel & 15) == 1) {
            return true;
        }
        return false;
    }

    public boolean isProvisioningAllowed(String action, String packageName) {
        Preconditions.checkNotNull(packageName);
        int callingUid = this.mInjector.binderGetCallingUid();
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            Preconditions.checkArgument(callingUid == this.mInjector.getPackageManager().getPackageUidAsUser(packageName, UserHandle.getUserId(callingUid)), "Caller uid doesn't match the one for the provided package.");
            this.mInjector.binderRestoreCallingIdentity(ident);
            if (checkProvisioningPreConditionSkipPermission(action, packageName) == 0) {
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid package provided ");
            stringBuilder.append(packageName);
            throw new IllegalArgumentException(stringBuilder.toString(), e);
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public int checkProvisioningPreCondition(String action, String packageName) {
        Preconditions.checkNotNull(packageName);
        enforceCanManageProfileAndDeviceOwners();
        return checkProvisioningPreConditionSkipPermission(action, packageName);
    }

    private int checkProvisioningPreConditionSkipPermission(String action, String packageName) {
        if (!this.mHasFeature) {
            return 13;
        }
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        if (action != null) {
            Object obj = -1;
            int hashCode = action.hashCode();
            if (hashCode != -920528692) {
                if (hashCode != -514404415) {
                    if (hashCode != -340845101) {
                        if (hashCode == 631897778 && action.equals("android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE")) {
                            obj = 3;
                        }
                    } else if (action.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                        obj = null;
                    }
                } else if (action.equals("android.app.action.PROVISION_MANAGED_USER")) {
                    obj = 2;
                }
            } else if (action.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                obj = 1;
            }
            switch (obj) {
                case null:
                    return checkManagedProfileProvisioningPreCondition(packageName, callingUserId);
                case 1:
                    return checkDeviceOwnerProvisioningPreCondition(callingUserId);
                case 2:
                    return checkManagedUserProvisioningPreCondition(callingUserId);
                case 3:
                    return checkManagedShareableDeviceProvisioningPreCondition(callingUserId);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown provisioning action ");
        stringBuilder.append(action);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private int checkDeviceOwnerProvisioningPreConditionLocked(ComponentName owner, int deviceOwnerUserId, boolean isAdb, boolean hasIncompatibleAccountsOrNonAdb) {
        if (this.mOwners.hasDeviceOwner()) {
            return 1;
        }
        if (this.mOwners.hasProfileOwner(deviceOwnerUserId)) {
            return 2;
        }
        if (!this.mUserManager.isUserRunning(new UserHandle(deviceOwnerUserId))) {
            return 3;
        }
        if (this.mIsWatch && hasPaired(0)) {
            return 8;
        }
        if (isAdb || this.mIsMDMDeviceOwnerAPI) {
            if ((this.mIsWatch || hasUserSetupCompleted(0)) && !this.mInjector.userManagerIsSplitSystemUser()) {
                if (this.mUserManager.getUserCount() > 1) {
                    return 5;
                }
                if (hasIncompatibleAccountsOrNonAdb) {
                    return 6;
                }
            }
            return 0;
        }
        if (!this.mInjector.userManagerIsSplitSystemUser()) {
            if (deviceOwnerUserId != 0) {
                return 7;
            }
            if (hasUserSetupCompleted(0)) {
                return 4;
            }
        }
        return 0;
    }

    private int checkDeviceOwnerProvisioningPreCondition(int deviceOwnerUserId) {
        int checkDeviceOwnerProvisioningPreConditionLocked;
        synchronized (getLockObject()) {
            checkDeviceOwnerProvisioningPreConditionLocked = checkDeviceOwnerProvisioningPreConditionLocked(null, deviceOwnerUserId, false, true);
        }
        return checkDeviceOwnerProvisioningPreConditionLocked;
    }

    private int checkManagedProfileProvisioningPreCondition(String packageName, int callingUserId) {
        if (!hasFeatureManagedUsers()) {
            return 9;
        }
        if (callingUserId == 0 && this.mInjector.userManagerIsSplitSystemUser()) {
            return 14;
        }
        if (getProfileOwner(callingUserId) != null) {
            return 2;
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            UserHandle callingUserHandle = UserHandle.of(callingUserId);
            ComponentName ownerAdmin = getOwnerComponent(packageName, callingUserId);
            if (this.mUserManager.hasUserRestriction("no_add_managed_profile", callingUserHandle) && (ownerAdmin == null || isAdminAffectedByRestriction(ownerAdmin, "no_add_managed_profile", callingUserId))) {
                this.mInjector.binderRestoreCallingIdentity(ident);
                return 15;
            }
            boolean canRemoveProfile = true;
            if (this.mUserManager.hasUserRestriction("no_remove_managed_profile", callingUserHandle) && (ownerAdmin == null || isAdminAffectedByRestriction(ownerAdmin, "no_remove_managed_profile", callingUserId))) {
                canRemoveProfile = false;
            }
            if (this.mUserManager.canAddMoreManagedProfiles(callingUserId, canRemoveProfile)) {
                this.mInjector.binderRestoreCallingIdentity(ident);
                return 0;
            }
            this.mInjector.binderRestoreCallingIdentity(ident);
            return 11;
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private ComponentName getOwnerComponent(String packageName, int userId) {
        if (isDeviceOwnerPackage(packageName, userId)) {
            return this.mOwners.getDeviceOwnerComponent();
        }
        if (isProfileOwnerPackage(packageName, userId)) {
            return this.mOwners.getProfileOwnerComponent(userId);
        }
        return null;
    }

    private ComponentName getOwnerComponent(int userId) {
        synchronized (getLockObject()) {
            ComponentName deviceOwnerComponent;
            if (this.mOwners.getDeviceOwnerUserId() == userId) {
                deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
                return deviceOwnerComponent;
            } else if (this.mOwners.hasProfileOwner(userId)) {
                deviceOwnerComponent = this.mOwners.getProfileOwnerComponent(userId);
                return deviceOwnerComponent;
            } else {
                return null;
            }
        }
    }

    private int checkManagedUserProvisioningPreCondition(int callingUserId) {
        if (!hasFeatureManagedUsers()) {
            return 9;
        }
        if (!this.mInjector.userManagerIsSplitSystemUser()) {
            return 12;
        }
        if (callingUserId == 0) {
            return 10;
        }
        if (hasUserSetupCompleted(callingUserId)) {
            return 4;
        }
        if (this.mIsWatch && hasPaired(0)) {
            return 8;
        }
        return 0;
    }

    private int checkManagedShareableDeviceProvisioningPreCondition(int callingUserId) {
        if (this.mInjector.userManagerIsSplitSystemUser()) {
            return checkDeviceOwnerProvisioningPreCondition(callingUserId);
        }
        return 12;
    }

    private boolean hasFeatureManagedUsers() {
        try {
            return this.mIPackageManager.hasSystemFeature("android.software.managed_users", 0);
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getWifiMacAddress(ComponentName admin) {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(admin, -2);
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        String str;
        try {
            WifiInfo wifiInfo = this.mInjector.getWifiManager().getConnectionInfo();
            str = null;
            if (wifiInfo == null) {
                return str;
            }
            if (wifiInfo.hasRealMacAddress()) {
                str = wifiInfo.getMacAddress();
            }
            this.mInjector.binderRestoreCallingIdentity(ident);
            return str;
        } finally {
            str = this.mInjector;
            str.binderRestoreCallingIdentity(ident);
        }
    }

    private int getTargetSdk(String packageName, int userId) {
        int targetSdkVersion = 0;
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(packageName, 0, userId);
            if (ai != null) {
                targetSdkVersion = ai.targetSdkVersion;
            }
            return targetSdkVersion;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public boolean isManagedProfile(ComponentName admin) {
        enforceProfileOrDeviceOwner(admin);
        return isManagedProfile(this.mInjector.userHandleGetCallingUserId());
    }

    public boolean isSystemOnlyUser(ComponentName admin) {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(admin, -2);
        }
        return UserManager.isSplitSystemUser() && this.mInjector.userHandleGetCallingUserId() == 0;
    }

    public void reboot(ComponentName admin) {
        Preconditions.checkNotNull(admin);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(admin, -2);
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            if (this.mTelephonyManager.getCallState() == 0) {
                this.mInjector.powerManagerReboot("deviceowner");
                return;
            }
            throw new IllegalStateException("Cannot be called with ongoing call on the device");
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void setShortSupportMessage(ComponentName who, CharSequence message) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForUidLocked(who, this.mInjector.binderGetCallingUid());
                if (!TextUtils.equals(admin.shortSupportMessage, message)) {
                    admin.shortSupportMessage = message;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public CharSequence getShortSupportMessage(ComponentName who) {
        if (!this.mHasFeature) {
            return null;
        }
        CharSequence charSequence;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            charSequence = getActiveAdminForUidLocked(who, this.mInjector.binderGetCallingUid()).shortSupportMessage;
        }
        return charSequence;
    }

    public void setLongSupportMessage(ComponentName who, CharSequence message) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForUidLocked(who, this.mInjector.binderGetCallingUid());
                if (!TextUtils.equals(admin.longSupportMessage, message)) {
                    admin.longSupportMessage = message;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public CharSequence getLongSupportMessage(ComponentName who) {
        if (!this.mHasFeature) {
            return null;
        }
        CharSequence charSequence;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            charSequence = getActiveAdminForUidLocked(who, this.mInjector.binderGetCallingUid()).longSupportMessage;
        }
        return charSequence;
    }

    public CharSequence getShortSupportMessageForUser(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (isCallerWithSystemUid()) {
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin != null) {
                    CharSequence charSequence = admin.shortSupportMessage;
                    return charSequence;
                }
                return null;
            }
        }
        throw new SecurityException("Only the system can query support message for user");
    }

    public CharSequence getLongSupportMessageForUser(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (isCallerWithSystemUid()) {
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin != null) {
                    CharSequence charSequence = admin.longSupportMessage;
                    return charSequence;
                }
                return null;
            }
        }
        throw new SecurityException("Only the system can query support message for user");
    }

    public void setOrganizationColor(ComponentName who, int color) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            enforceManagedProfile(userHandle, "set organization color");
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(who, -1).organizationColor = color;
                saveSettingsLocked(userHandle);
            }
        }
    }

    public void setOrganizationColorForUser(int color, int userId) {
        if (this.mHasFeature) {
            enforceFullCrossUsersPermission(userId);
            enforceManageUsers();
            enforceManagedProfile(userId, "set organization color");
            synchronized (getLockObject()) {
                getProfileOwnerAdminLocked(userId).organizationColor = color;
                saveSettingsLocked(userId);
            }
        }
    }

    public int getOrganizationColor(ComponentName who) {
        if (!this.mHasFeature) {
            return ActiveAdmin.DEF_ORGANIZATION_COLOR;
        }
        int i;
        Preconditions.checkNotNull(who, "ComponentName is null");
        enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "get organization color");
        synchronized (getLockObject()) {
            i = getActiveAdminForCallerLocked(who, -1).organizationColor;
        }
        return i;
    }

    public int getOrganizationColorForUser(int userHandle) {
        if (!this.mHasFeature) {
            return ActiveAdmin.DEF_ORGANIZATION_COLOR;
        }
        int i;
        enforceFullCrossUsersPermission(userHandle);
        enforceManagedProfile(userHandle, "get organization color");
        synchronized (getLockObject()) {
            ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userHandle);
            if (profileOwner != null) {
                i = profileOwner.organizationColor;
            } else {
                i = ActiveAdmin.DEF_ORGANIZATION_COLOR;
            }
        }
        return i;
    }

    public void setOrganizationName(ComponentName who, CharSequence text) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
                if (!TextUtils.equals(admin.organizationName, text)) {
                    String charSequence;
                    if (text != null) {
                        if (text.length() != 0) {
                            charSequence = text.toString();
                            admin.organizationName = charSequence;
                            saveSettingsLocked(userHandle);
                        }
                    }
                    charSequence = null;
                    admin.organizationName = charSequence;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public CharSequence getOrganizationName(ComponentName who) {
        if (!this.mHasFeature) {
            return null;
        }
        String str;
        Preconditions.checkNotNull(who, "ComponentName is null");
        enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "get organization name");
        synchronized (getLockObject()) {
            str = getActiveAdminForCallerLocked(who, -1).organizationName;
        }
        return str;
    }

    public CharSequence getDeviceOwnerOrganizationName() {
        CharSequence charSequence = null;
        if (!this.mHasFeature) {
            return null;
        }
        enforceDeviceOwnerOrManageUsers();
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwnerAdmin = getDeviceOwnerAdminLocked();
            if (deviceOwnerAdmin != null) {
                charSequence = deviceOwnerAdmin.organizationName;
            }
        }
        return charSequence;
    }

    public CharSequence getOrganizationNameForUser(int userHandle) {
        CharSequence charSequence = null;
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(userHandle);
        enforceManagedProfile(userHandle, "get organization name");
        synchronized (getLockObject()) {
            ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userHandle);
            if (profileOwner != null) {
                charSequence = profileOwner.organizationName;
            }
        }
        return charSequence;
    }

    public List<String> setMeteredDataDisabledPackages(ComponentName who, List<String> packageNames) {
        Preconditions.checkNotNull(who);
        Preconditions.checkNotNull(packageNames);
        if (!this.mHasFeature) {
            return packageNames;
        }
        List<String> excludedPkgs;
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            int callingUserId = this.mInjector.userHandleGetCallingUserId();
            long identity = this.mInjector.binderClearCallingIdentity();
            try {
                excludedPkgs = removeInvalidPkgsForMeteredDataRestriction(callingUserId, packageNames);
                admin.meteredDisabledPackages = packageNames;
                pushMeteredDisabledPackagesLocked(callingUserId);
                saveSettingsLocked(callingUserId);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(identity);
            }
        }
        return excludedPkgs;
    }

    private List<String> removeInvalidPkgsForMeteredDataRestriction(int userId, List<String> pkgNames) {
        Set<String> activeAdmins = getActiveAdminPackagesLocked(userId);
        List<String> excludedPkgs = new ArrayList();
        for (int i = pkgNames.size() - 1; i >= 0; i--) {
            String pkgName = (String) pkgNames.get(i);
            if (activeAdmins.contains(pkgName)) {
                excludedPkgs.add(pkgName);
            } else {
                try {
                    if (!this.mInjector.getIPackageManager().isPackageAvailable(pkgName, userId)) {
                        excludedPkgs.add(pkgName);
                    }
                } catch (RemoteException e) {
                }
            }
        }
        pkgNames.removeAll(excludedPkgs);
        return excludedPkgs;
    }

    public List<String> getMeteredDataDisabledPackages(ComponentName who) {
        Preconditions.checkNotNull(who);
        if (!this.mHasFeature) {
            return new ArrayList();
        }
        List<String> arrayList;
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            arrayList = admin.meteredDisabledPackages == null ? new ArrayList() : admin.meteredDisabledPackages;
        }
        return arrayList;
    }

    /* JADX WARNING: Missing block: B:16:0x0027, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isMeteredDataDisabledPackageForUser(ComponentName who, String packageName, int userId) {
        Preconditions.checkNotNull(who);
        if (!this.mHasFeature) {
            return false;
        }
        if (isCallerWithSystemUid()) {
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userId);
                if (admin == null || admin.meteredDisabledPackages == null) {
                } else {
                    boolean contains = admin.meteredDisabledPackages.contains(packageName);
                    return contains;
                }
            }
        }
        throw new SecurityException("Only the system can query restricted pkgs for a specific user");
    }

    private void pushMeteredDisabledPackagesLocked(int userId) {
        this.mInjector.getNetworkPolicyManagerInternal().setMeteredRestrictedPackages(getMeteredDisabledPackagesLocked(userId), userId);
    }

    private Set<String> getMeteredDisabledPackagesLocked(int userId) {
        ComponentName who = getOwnerComponent(userId);
        Set<String> restrictedPkgs = new ArraySet();
        if (who != null) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userId);
            if (!(admin == null || admin.meteredDisabledPackages == null)) {
                restrictedPkgs.addAll(admin.meteredDisabledPackages);
            }
        }
        return restrictedPkgs;
    }

    public void setAffiliationIds(ComponentName admin, List<String> ids) {
        if (!this.mHasFeature) {
            return;
        }
        if (ids != null) {
            for (String id : ids) {
                if (TextUtils.isEmpty(id)) {
                    throw new IllegalArgumentException("ids must not contain empty string");
                }
            }
            Set<String> affiliationIds = new ArraySet(ids);
            int callingUserId = this.mInjector.userHandleGetCallingUserId();
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(admin, -1);
                getUserData(callingUserId).mAffiliationIds = affiliationIds;
                saveSettingsLocked(callingUserId);
                if (callingUserId != 0 && isDeviceOwner(admin, callingUserId)) {
                    getUserData(0).mAffiliationIds = affiliationIds;
                    saveSettingsLocked(0);
                }
                maybePauseDeviceWideLoggingLocked();
                maybeResumeDeviceWideLoggingLocked();
                maybeClearLockTaskPolicyLocked();
            }
            return;
        }
        throw new IllegalArgumentException("ids must not be null");
    }

    public List<String> getAffiliationIds(ComponentName admin) {
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        ArrayList arrayList;
        Preconditions.checkNotNull(admin);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(admin, -1);
            arrayList = new ArrayList(getUserData(this.mInjector.userHandleGetCallingUserId()).mAffiliationIds);
        }
        return arrayList;
    }

    public boolean isAffiliatedUser() {
        if (!this.mHasFeature) {
            return false;
        }
        boolean isUserAffiliatedWithDeviceLocked;
        synchronized (getLockObject()) {
            isUserAffiliatedWithDeviceLocked = isUserAffiliatedWithDeviceLocked(this.mInjector.userHandleGetCallingUserId());
        }
        return isUserAffiliatedWithDeviceLocked;
    }

    private boolean isUserAffiliatedWithDeviceLocked(int userId) {
        if (!this.mOwners.hasDeviceOwner()) {
            return false;
        }
        if (userId == this.mOwners.getDeviceOwnerUserId() || userId == 0) {
            return true;
        }
        if (getProfileOwner(userId) == null) {
            return false;
        }
        Set<String> userAffiliationIds = getUserData(userId).mAffiliationIds;
        Set<String> deviceAffiliationIds = getUserData(0).mAffiliationIds;
        for (String id : userAffiliationIds) {
            if (deviceAffiliationIds.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean areAllUsersAffiliatedWithDeviceLocked() {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            List<UserInfo> userInfos = this.mUserManager.getUsers(true);
            int i = 0;
            while (i < userInfos.size()) {
                int userId = ((UserInfo) userInfos.get(i)).id;
                if (isUserAffiliatedWithDeviceLocked(userId)) {
                    i++;
                } else {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("User id ");
                    stringBuilder.append(userId);
                    stringBuilder.append(" not affiliated.");
                    Slog.d(str, stringBuilder.toString());
                    return false;
                }
            }
            this.mInjector.binderRestoreCallingIdentity(ident);
            return true;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0031, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setSecurityLoggingEnabled(ComponentName admin, boolean enabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(admin);
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(admin, -2);
                if (enabled == this.mInjector.securityLogGetLoggingEnabledProperty()) {
                    return;
                }
                this.mInjector.securityLogSetLoggingEnabledProperty(enabled);
                if (enabled) {
                    this.mSecurityLogMonitor.start();
                    maybePauseDeviceWideLoggingLocked();
                } else {
                    this.mSecurityLogMonitor.stop();
                }
            }
        }
    }

    public boolean isSecurityLoggingEnabled(ComponentName admin) {
        if (!this.mHasFeature) {
            return false;
        }
        boolean securityLogGetLoggingEnabledProperty;
        synchronized (getLockObject()) {
            if (!isCallerWithSystemUid()) {
                Preconditions.checkNotNull(admin);
                getActiveAdminForCallerLocked(admin, -2);
            }
            securityLogGetLoggingEnabledProperty = this.mInjector.securityLogGetLoggingEnabledProperty();
        }
        return securityLogGetLoggingEnabledProperty;
    }

    private void recordSecurityLogRetrievalTime() {
        synchronized (getLockObject()) {
            long currentTime = System.currentTimeMillis();
            DevicePolicyData policyData = getUserData(0);
            if (currentTime > policyData.mLastSecurityLogRetrievalTime) {
                policyData.mLastSecurityLogRetrievalTime = currentTime;
                saveSettingsLocked(0);
            }
        }
    }

    public ParceledListSlice<SecurityEvent> retrievePreRebootSecurityLogs(ComponentName admin) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(admin);
        ensureDeviceOwnerAndAllUsersAffiliated(admin);
        if (!this.mContext.getResources().getBoolean(17957039) || !this.mInjector.securityLogGetLoggingEnabledProperty()) {
            return null;
        }
        recordSecurityLogRetrievalTime();
        ArrayList<SecurityEvent> output = new ArrayList();
        try {
            SecurityLog.readPreviousEvents(output);
            return new ParceledListSlice(output);
        } catch (IOException e) {
            Slog.w(LOG_TAG, "Fail to read previous events", e);
            return new ParceledListSlice(Collections.emptyList());
        }
    }

    public ParceledListSlice<SecurityEvent> retrieveSecurityLogs(ComponentName admin) {
        ParceledListSlice<SecurityEvent> parceledListSlice = null;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(admin);
        ensureDeviceOwnerAndAllUsersAffiliated(admin);
        if (!this.mInjector.securityLogGetLoggingEnabledProperty()) {
            return null;
        }
        recordSecurityLogRetrievalTime();
        List<SecurityEvent> logs = this.mSecurityLogMonitor.retrieveLogs();
        if (logs != null) {
            parceledListSlice = new ParceledListSlice(logs);
        }
        return parceledListSlice;
    }

    public long forceSecurityLogs() {
        enforceShell("forceSecurityLogs");
        if (this.mInjector.securityLogGetLoggingEnabledProperty()) {
            return this.mSecurityLogMonitor.forceLogs();
        }
        throw new IllegalStateException("logging is not available");
    }

    private void enforceCanManageDeviceAdmin() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
    }

    private void enforceCanManageProfileAndDeviceOwners() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS", null);
    }

    private void enforceCallerSystemUserHandle() {
        if (UserHandle.getUserId(this.mInjector.binderGetCallingUid()) != 0) {
            throw new SecurityException("Caller has to be in user 0");
        }
    }

    public boolean isUninstallInQueue(String packageName) {
        boolean contains;
        enforceCanManageDeviceAdmin();
        Pair<String, Integer> packageUserPair = new Pair(packageName, Integer.valueOf(this.mInjector.userHandleGetCallingUserId()));
        synchronized (getLockObject()) {
            contains = this.mPackagesToRemove.contains(packageUserPair);
        }
        return contains;
    }

    public void uninstallPackageWithActiveAdmins(final String packageName) {
        enforceCanManageDeviceAdmin();
        Preconditions.checkArgument(TextUtils.isEmpty(packageName) ^ 1);
        final int userId = this.mInjector.userHandleGetCallingUserId();
        enforceUserUnlocked(userId);
        ComponentName profileOwner = getProfileOwner(userId);
        if (profileOwner == null || !packageName.equals(profileOwner.getPackageName())) {
            ComponentName deviceOwner = getDeviceOwnerComponent(null);
            if (getDeviceOwnerUserId() == userId && deviceOwner != null && packageName.equals(deviceOwner.getPackageName())) {
                throw new IllegalArgumentException("Cannot uninstall a package with a device owner");
            }
            Pair<String, Integer> packageUserPair = new Pair(packageName, Integer.valueOf(userId));
            synchronized (getLockObject()) {
                this.mPackagesToRemove.add(packageUserPair);
            }
            List<ComponentName> allActiveAdmins = getActiveAdmins(userId);
            final List<ComponentName> packageActiveAdmins = new ArrayList();
            if (allActiveAdmins != null) {
                for (ComponentName activeAdmin : allActiveAdmins) {
                    if (packageName.equals(activeAdmin.getPackageName())) {
                        packageActiveAdmins.add(activeAdmin);
                        removeActiveAdmin(activeAdmin, userId);
                    }
                }
            }
            if (packageActiveAdmins.size() == 0) {
                startUninstallIntent(packageName, userId);
                return;
            } else {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        for (ComponentName activeAdmin : packageActiveAdmins) {
                            DevicePolicyManagerService.this.removeAdminArtifacts(activeAdmin, userId);
                        }
                        DevicePolicyManagerService.this.startUninstallIntent(packageName, userId);
                    }
                }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                return;
            }
        }
        throw new IllegalArgumentException("Cannot uninstall a package with a profile owner");
    }

    public boolean isDeviceProvisioned() {
        boolean z;
        enforceManageUsers();
        synchronized (getLockObject()) {
            z = getUserDataUnchecked(0).mUserSetupComplete;
        }
        return z;
    }

    private boolean isCurrentUserDemo() {
        if (!UserManager.isDeviceInDemoMode(this.mContext)) {
            return false;
        }
        int userId = this.mInjector.userHandleGetCallingUserId();
        long callingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            boolean isDemo = this.mUserManager.getUserInfo(userId).isDemo();
            return isDemo;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(callingIdentity);
        }
    }

    private void removePackageIfRequired(String packageName, int userId) {
        if (!packageHasActiveAdmins(packageName, userId)) {
            startUninstallIntent(packageName, userId);
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0029, code skipped:
            if (r5.mInjector.getIPackageManager().getPackageInfo(r6, 0, r7) != null) goto L_0x0035;
     */
    /* JADX WARNING: Missing block: B:12:0x002b, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:14:0x002e, code skipped:
            android.util.Log.e(LOG_TAG, "Failure talking to PackageManager while getting package info");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void startUninstallIntent(String packageName, int userId) {
        Pair<String, Integer> packageUserPair = new Pair(packageName, Integer.valueOf(userId));
        synchronized (getLockObject()) {
            if (this.mPackagesToRemove.contains(packageUserPair)) {
                this.mPackagesToRemove.remove(packageUserPair);
            } else {
                return;
            }
        }
        StringBuilder stringBuilder;
        Intent uninstallIntent;
        try {
            this.mInjector.getIActivityManager().forceStopPackage(packageName, userId);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failure talking to ActivityManager while force stopping package");
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("package:");
        stringBuilder.append(packageName);
        uninstallIntent = new Intent("android.intent.action.UNINSTALL_PACKAGE", Uri.parse(stringBuilder.toString()));
        uninstallIntent.setFlags(268435456);
        this.mContext.startActivityAsUser(uninstallIntent, UserHandle.of(userId));
        stringBuilder = new StringBuilder();
        stringBuilder.append("package:");
        stringBuilder.append(packageName);
        uninstallIntent = new Intent("android.intent.action.UNINSTALL_PACKAGE", Uri.parse(stringBuilder.toString()));
        uninstallIntent.setFlags(268435456);
        this.mContext.startActivityAsUser(uninstallIntent, UserHandle.of(userId));
    }

    private void removeAdminArtifacts(ComponentName adminReceiver, int userHandle) {
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
            if (admin == null) {
                return;
            }
            DevicePolicyData policy = getUserData(userHandle);
            boolean doProxyCleanup = admin.info.usesPolicy(5);
            policy.mAdminList.remove(admin);
            policy.mAdminMap.remove(adminReceiver);
            validatePasswordOwnerLocked(policy);
            if (doProxyCleanup) {
                resetGlobalProxyLocked(policy);
            }
            pushActiveAdminPackagesLocked(userHandle);
            pushMeteredDisabledPackagesLocked(userHandle);
            saveSettingsLocked(userHandle);
            updateMaximumTimeToLockLocked(userHandle);
            policy.mRemovingAdmins.remove(adminReceiver);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Device admin ");
            stringBuilder.append(adminReceiver);
            stringBuilder.append(" removed from user ");
            stringBuilder.append(userHandle);
            Slog.i(str, stringBuilder.toString());
            syncHwDeviceSettingsLocked(policy.mUserHandle);
            removeActiveAdminCompleted(adminReceiver);
            pushUserRestrictions(userHandle);
        }
    }

    public void setDeviceProvisioningConfigApplied() {
        enforceManageUsers();
        synchronized (getLockObject()) {
            getUserData(0).mDeviceProvisioningConfigApplied = true;
            saveSettingsLocked(0);
        }
    }

    public boolean isDeviceProvisioningConfigApplied() {
        boolean z;
        enforceManageUsers();
        synchronized (getLockObject()) {
            z = getUserData(null).mDeviceProvisioningConfigApplied;
        }
        return z;
    }

    public void forceUpdateUserSetupComplete() {
        enforceCanManageProfileAndDeviceOwners();
        enforceCallerSystemUserHandle();
        if (this.mInjector.isBuildDebuggable()) {
            getUserData(0).mUserSetupComplete = this.mInjector.settingsSecureGetIntForUser("user_setup_complete", 0, 0) != 0;
            synchronized (getLockObject()) {
                saveSettingsLocked(0);
            }
        }
    }

    public void setBackupServiceEnabled(ComponentName admin, boolean enabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(admin);
            synchronized (getLockObject()) {
                ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -2);
                if (!enabled) {
                    activeAdmin.mandatoryBackupTransport = null;
                    saveSettingsLocked(0);
                }
            }
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                IBackupManager ibm = this.mInjector.getIBackupManager();
                if (ibm != null) {
                    ibm.setBackupServiceActive(0, enabled);
                }
                this.mInjector.binderRestoreCallingIdentity(ident);
            } catch (RemoteException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed ");
                stringBuilder.append(enabled ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "de");
                stringBuilder.append("activating backup service.");
                throw new IllegalStateException(stringBuilder.toString(), e);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    public boolean isBackupServiceEnabled(ComponentName admin) {
        Preconditions.checkNotNull(admin);
        boolean z = true;
        if (!this.mHasFeature) {
            return true;
        }
        synchronized (getLockObject()) {
            try {
                getActiveAdminForCallerLocked(admin, -2);
                IBackupManager ibm = this.mInjector.getIBackupManager();
                if (ibm == null || !ibm.isBackupServiceActive(0)) {
                    z = false;
                }
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed requesting backup service state.", e);
            } catch (Throwable th) {
            }
        }
        return z;
    }

    public boolean setMandatoryBackupTransport(ComponentName admin, ComponentName backupTransportComponent) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(admin);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(admin, -2);
        }
        int callingUid = this.mInjector.binderGetCallingUid();
        AtomicBoolean success = new AtomicBoolean(false);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final ComponentName componentName = admin;
        final int i = callingUid;
        final ComponentName componentName2 = backupTransportComponent;
        final AtomicBoolean atomicBoolean = success;
        final CountDownLatch countDownLatch2 = countDownLatch;
        ISelectBackupTransportCallback selectBackupTransportCallbackInternal = new ISelectBackupTransportCallback.Stub() {
            public void onSuccess(String transportName) {
                DevicePolicyManagerService.this.saveMandatoryBackupTransport(componentName, i, componentName2);
                atomicBoolean.set(true);
                countDownLatch2.countDown();
            }

            public void onFailure(int reason) {
                countDownLatch2.countDown();
            }
        };
        long identity = this.mInjector.binderClearCallingIdentity();
        try {
            IBackupManager ibm = this.mInjector.getIBackupManager();
            if (ibm != null && backupTransportComponent != null) {
                if (!ibm.isBackupServiceActive(0)) {
                    ibm.setBackupServiceActive(0, true);
                }
                ibm.selectBackupTransportAsync(backupTransportComponent, selectBackupTransportCallbackInternal);
                countDownLatch.await();
                if (success.get()) {
                    ibm.setBackupEnabled(true);
                }
            } else if (backupTransportComponent == null) {
                saveMandatoryBackupTransport(admin, callingUid, backupTransportComponent);
                success.set(true);
            }
            this.mInjector.binderRestoreCallingIdentity(identity);
            return success.get();
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to set mandatory backup transport.", e);
        } catch (InterruptedException e2) {
            throw new IllegalStateException("Failed to set mandatory backup transport.", e2);
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(identity);
        }
    }

    private void saveMandatoryBackupTransport(ComponentName admin, int callingUid, ComponentName backupTransportComponent) {
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getActiveAdminWithPolicyForUidLocked(admin, -2, callingUid);
            if (!Objects.equals(backupTransportComponent, activeAdmin.mandatoryBackupTransport)) {
                activeAdmin.mandatoryBackupTransport = backupTransportComponent;
                saveSettingsLocked(0);
            }
        }
    }

    public ComponentName getMandatoryBackupTransport() {
        ComponentName componentName = null;
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getDeviceOwnerAdminLocked();
            if (activeAdmin != null) {
                componentName = activeAdmin.mandatoryBackupTransport;
            }
        }
        return componentName;
    }

    public boolean bindDeviceAdminServiceAsUser(ComponentName admin, IApplicationThread caller, IBinder activtiyToken, Intent serviceIntent, IServiceConnection connection, int flags, int targetUserId) {
        Throwable th;
        String str;
        Intent intent = serviceIntent;
        int i = targetUserId;
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(admin);
        Preconditions.checkNotNull(caller);
        Preconditions.checkNotNull(serviceIntent);
        boolean z2 = (serviceIntent.getComponent() == null && serviceIntent.getPackage() == null) ? false : true;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Service intent must be explicit (with a package name or component): ");
        stringBuilder.append(intent);
        Preconditions.checkArgument(z2, stringBuilder.toString());
        Preconditions.checkNotNull(connection);
        Preconditions.checkArgument(this.mInjector.userHandleGetCallingUserId() != i, "target user id must be different from the calling user id");
        if (getBindDeviceAdminTargetUsers(admin).contains(UserHandle.of(targetUserId))) {
            String targetPackage;
            synchronized (getLockObject()) {
                targetPackage = getOwnerPackageNameForUserLocked(i);
            }
            long callingIdentity = this.mInjector.binderClearCallingIdentity();
            long callingIdentity2;
            try {
                if (createCrossUserServiceIntent(intent, targetPackage, i) == null) {
                    this.mInjector.binderRestoreCallingIdentity(callingIdentity);
                    return false;
                }
                callingIdentity2 = callingIdentity;
                try {
                    if (this.mInjector.getIActivityManager().bindService(caller, activtiyToken, intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), connection, flags, this.mContext.getOpPackageName(), i) != 0) {
                        z = true;
                    }
                    this.mInjector.binderRestoreCallingIdentity(callingIdentity2);
                    return z;
                } catch (RemoteException e) {
                    this.mInjector.binderRestoreCallingIdentity(callingIdentity2);
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    this.mInjector.binderRestoreCallingIdentity(callingIdentity2);
                    throw th;
                }
            } catch (RemoteException e2) {
                str = targetPackage;
                callingIdentity2 = callingIdentity;
                this.mInjector.binderRestoreCallingIdentity(callingIdentity2);
                return false;
            } catch (Throwable th3) {
                th = th3;
                str = targetPackage;
                callingIdentity2 = callingIdentity;
                this.mInjector.binderRestoreCallingIdentity(callingIdentity2);
                throw th;
            }
        }
        throw new SecurityException("Not allowed to bind to target user id");
    }

    public List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName admin) {
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        ArrayList<UserHandle> targetUsers;
        Preconditions.checkNotNull(admin);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(admin, -1);
            int callingUserId = this.mInjector.userHandleGetCallingUserId();
            long callingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                targetUsers = new ArrayList();
                if (isDeviceOwner(admin, callingUserId)) {
                    List<UserInfo> userInfos = this.mUserManager.getUsers(true);
                    for (int i = 0; i < userInfos.size(); i++) {
                        int userId = ((UserInfo) userInfos.get(i)).id;
                        if (userId != callingUserId && canUserBindToDeviceOwnerLocked(userId)) {
                            targetUsers.add(UserHandle.of(userId));
                        }
                    }
                } else if (canUserBindToDeviceOwnerLocked(callingUserId)) {
                    targetUsers.add(UserHandle.of(this.mOwners.getDeviceOwnerUserId()));
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(callingIdentity);
            }
        }
        return targetUsers;
    }

    /* JADX WARNING: Missing block: B:11:0x0033, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean canUserBindToDeviceOwnerLocked(int userId) {
        if (this.mOwners.hasDeviceOwner() && userId != this.mOwners.getDeviceOwnerUserId() && this.mOwners.hasProfileOwner(userId) && TextUtils.equals(this.mOwners.getDeviceOwnerPackageName(), this.mOwners.getProfileOwnerPackage(userId))) {
            return isUserAffiliatedWithDeviceLocked(userId);
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:10:0x003b, code skipped:
            wtfIfInLock();
            r6 = r1.mInjector.binderClearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r8 = android.accounts.AccountManager.get(r1.mContext);
            r9 = r8.getAccountsAsUser(r2);
     */
    /* JADX WARNING: Missing block: B:14:0x0052, code skipped:
            if (r9.length != 0) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:15:0x0054, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r6);
     */
    /* JADX WARNING: Missing block: B:16:0x005a, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            r11 = getLockObject();
     */
    /* JADX WARNING: Missing block: B:19:0x005f, code skipped:
            monitor-enter(r11);
     */
    /* JADX WARNING: Missing block: B:20:0x0060, code skipped:
            if (r3 == null) goto L_0x00e9;
     */
    /* JADX WARNING: Missing block: B:23:0x0066, code skipped:
            if (isAdminTestOnlyLocked(r3, r2) != false) goto L_0x006a;
     */
    /* JADX WARNING: Missing block: B:25:0x006a, code skipped:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:27:?, code skipped:
            r0 = new java.lang.String[]{"android.account.DEVICE_OR_PROFILE_OWNER_ALLOWED"};
            r11 = new java.lang.String[]{"android.account.DEVICE_OR_PROFILE_OWNER_DISALLOWED"};
            r12 = true;
            r13 = r9.length;
            r14 = 0;
     */
    /* JADX WARNING: Missing block: B:28:0x007a, code skipped:
            if (r14 >= r13) goto L_0x00c8;
     */
    /* JADX WARNING: Missing block: B:29:0x007c, code skipped:
            r15 = r9[r14];
     */
    /* JADX WARNING: Missing block: B:30:0x0082, code skipped:
            if (hasAccountFeatures(r8, r15, r11) == false) goto L_0x00a1;
     */
    /* JADX WARNING: Missing block: B:31:0x0084, code skipped:
            r13 = LOG_TAG;
            r14 = new java.lang.StringBuilder();
            r14.append(r15);
            r14.append(" has ");
            r14.append(r11[0]);
            android.util.Log.e(r13, r14.toString());
            r12 = false;
     */
    /* JADX WARNING: Missing block: B:33:0x00a5, code skipped:
            if (hasAccountFeatures(r8, r15, r0) != false) goto L_0x00c4;
     */
    /* JADX WARNING: Missing block: B:34:0x00a7, code skipped:
            r5 = LOG_TAG;
            r13 = new java.lang.StringBuilder();
            r13.append(r15);
            r13.append(" doesn't have ");
            r13.append(r0[0]);
            android.util.Log.e(r5, r13.toString());
            r12 = false;
     */
    /* JADX WARNING: Missing block: B:35:0x00c4, code skipped:
            r14 = r14 + 1;
     */
    /* JADX WARNING: Missing block: B:36:0x00c8, code skipped:
            if (r12 == false) goto L_0x00d2;
     */
    /* JADX WARNING: Missing block: B:37:0x00ca, code skipped:
            android.util.Log.w(LOG_TAG, "All accounts are compatible");
     */
    /* JADX WARNING: Missing block: B:38:0x00d2, code skipped:
            android.util.Log.e(LOG_TAG, "Found incompatible accounts");
     */
    /* JADX WARNING: Missing block: B:39:0x00d9, code skipped:
            if (r12 != false) goto L_0x00de;
     */
    /* JADX WARNING: Missing block: B:40:0x00db, code skipped:
            r17 = true;
     */
    /* JADX WARNING: Missing block: B:41:0x00de, code skipped:
            r17 = false;
     */
    /* JADX WARNING: Missing block: B:42:0x00e1, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r6);
     */
    /* JADX WARNING: Missing block: B:43:0x00e6, code skipped:
            return r17;
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            android.util.Log.w(LOG_TAG, "Non test-only owner can't be installed with existing accounts.");
     */
    /* JADX WARNING: Missing block: B:47:0x00f0, code skipped:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:48:0x00f1, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r6);
     */
    /* JADX WARNING: Missing block: B:49:0x00f7, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:55:0x00fb, code skipped:
            r1.mInjector.binderRestoreCallingIdentity(r6);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean hasIncompatibleAccountsOrNonAdbNoLock(int userId, ComponentName owner) {
        int i = userId;
        ComponentName componentName = owner;
        int callingUid = this.mInjector.binderGetCallingUid();
        synchronized (this) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hasIncompatibleAccountsOrNonAdbNoLock mIsMDMDeviceOwnerAPI=");
            stringBuilder.append(this.mIsMDMDeviceOwnerAPI);
            stringBuilder.append(",callingUid =");
            stringBuilder.append(callingUid);
            Slog.w(str, stringBuilder.toString());
            if (isAdb() || this.mIsMDMDeviceOwnerAPI) {
            } else {
                return true;
            }
        }
    }

    private boolean hasAccountFeatures(AccountManager am, Account account, String[] features) {
        try {
            return ((Boolean) am.hasFeatures(account, features, null, null).getResult()).booleanValue();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to get account feature", e);
            return false;
        }
    }

    private boolean isAdb() {
        int callingUid = this.mInjector.binderGetCallingUid();
        return callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || callingUid == 0;
    }

    public void setNetworkLoggingEnabled(ComponentName admin, boolean enabled) {
        if (this.mHasFeature) {
            synchronized (getLockObject()) {
                Preconditions.checkNotNull(admin);
                getActiveAdminForCallerLocked(admin, -2);
                if (enabled == isNetworkLoggingEnabledInternalLocked()) {
                    return;
                }
                ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
                deviceOwner.isNetworkLoggingEnabled = enabled;
                if (!enabled) {
                    deviceOwner.numNetworkLoggingNotifications = 0;
                    deviceOwner.lastNetworkLoggingNotificationTimeMs = 0;
                }
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
                setNetworkLoggingActiveInternal(enabled);
            }
        }
    }

    private void setNetworkLoggingActiveInternal(boolean active) {
        synchronized (getLockObject()) {
            long callingIdentity = this.mInjector.binderClearCallingIdentity();
            if (active) {
                try {
                    this.mNetworkLogger = new NetworkLogger(this, this.mInjector.getPackageManagerInternal());
                    if (!this.mNetworkLogger.startNetworkLogging()) {
                        this.mNetworkLogger = null;
                        Slog.wtf(LOG_TAG, "Network logging could not be started due to the logging service not being available yet.");
                    }
                    maybePauseDeviceWideLoggingLocked();
                    sendNetworkLoggingNotificationLocked();
                } catch (Throwable th) {
                    this.mInjector.binderRestoreCallingIdentity(callingIdentity);
                }
            } else {
                if (!(this.mNetworkLogger == null || this.mNetworkLogger.stopNetworkLogging())) {
                    Slog.wtf(LOG_TAG, "Network logging could not be stopped due to the logging service not being available yet.");
                }
                this.mNetworkLogger = null;
                this.mInjector.getNotificationManager().cancel(1002);
            }
            this.mInjector.binderRestoreCallingIdentity(callingIdentity);
        }
    }

    private void maybePauseDeviceWideLoggingLocked() {
        if (!areAllUsersAffiliatedWithDeviceLocked()) {
            Slog.i(LOG_TAG, "There are unaffiliated users, security and network logging will be paused if enabled.");
            this.mSecurityLogMonitor.pause();
            if (this.mNetworkLogger != null) {
                this.mNetworkLogger.pause();
            }
        }
    }

    private void maybeResumeDeviceWideLoggingLocked() {
        if (areAllUsersAffiliatedWithDeviceLocked()) {
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                this.mSecurityLogMonitor.resume();
                if (this.mNetworkLogger != null) {
                    this.mNetworkLogger.resume();
                }
                this.mInjector.binderRestoreCallingIdentity(ident);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    private void discardDeviceWideLogsLocked() {
        this.mSecurityLogMonitor.discardLogs();
        if (this.mNetworkLogger != null) {
            this.mNetworkLogger.discardLogs();
        }
    }

    public boolean isNetworkLoggingEnabled(ComponentName admin) {
        if (!this.mHasFeature) {
            return false;
        }
        boolean isNetworkLoggingEnabledInternalLocked;
        synchronized (getLockObject()) {
            enforceDeviceOwnerOrManageUsers();
            isNetworkLoggingEnabledInternalLocked = isNetworkLoggingEnabledInternalLocked();
        }
        return isNetworkLoggingEnabledInternalLocked;
    }

    private boolean isNetworkLoggingEnabledInternalLocked() {
        ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
        return deviceOwner != null && deviceOwner.isNetworkLoggingEnabled;
    }

    /* JADX WARNING: Missing block: B:18:0x0039, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<NetworkEvent> retrieveNetworkLogs(ComponentName admin, long batchToken) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(admin);
        ensureDeviceOwnerAndAllUsersAffiliated(admin);
        synchronized (getLockObject()) {
            if (this.mNetworkLogger != null) {
                if (isNetworkLoggingEnabledInternalLocked()) {
                    long currentTime = System.currentTimeMillis();
                    DevicePolicyData policyData = getUserData(0);
                    if (currentTime > policyData.mLastNetworkLogsRetrievalTime) {
                        policyData.mLastNetworkLogsRetrievalTime = currentTime;
                        saveSettingsLocked(0);
                    }
                    List retrieveLogs = this.mNetworkLogger.retrieveLogs(batchToken);
                    return retrieveLogs;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x00af, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sendNetworkLoggingNotificationLocked() {
        ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
        if (deviceOwner != null && deviceOwner.isNetworkLoggingEnabled && deviceOwner.numNetworkLoggingNotifications < 2) {
            long now = System.currentTimeMillis();
            if (now - deviceOwner.lastNetworkLoggingNotificationTimeMs >= MS_PER_DAY) {
                deviceOwner.numNetworkLoggingNotifications++;
                if (deviceOwner.numNetworkLoggingNotifications >= 2) {
                    deviceOwner.lastNetworkLoggingNotificationTimeMs = 0;
                } else {
                    deviceOwner.lastNetworkLoggingNotificationTimeMs = now;
                }
                Intent intent = new Intent("android.app.action.SHOW_DEVICE_MONITORING_DIALOG");
                intent.setPackage("com.android.systemui");
                this.mInjector.getNotificationManager().notify(1002, new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17302398).setContentTitle(this.mContext.getString(17040549)).setContentText(this.mContext.getString(17040548)).setTicker(this.mContext.getString(17040549)).setShowWhen(true).setContentIntent(PendingIntent.getBroadcastAsUser(this.mContext, 0, intent, 0, UserHandle.CURRENT)).setStyle(new BigTextStyle().bigText(this.mContext.getString(17040548))).build());
                saveSettingsLocked(this.mOwners.getDeviceOwnerUserId());
            }
        }
    }

    private String getOwnerPackageNameForUserLocked(int userId) {
        if (this.mOwners.getDeviceOwnerUserId() == userId) {
            return this.mOwners.getDeviceOwnerPackageName();
        }
        return this.mOwners.getProfileOwnerPackage(userId);
    }

    private Intent createCrossUserServiceIntent(Intent rawIntent, String expectedPackageName, int targetUserId) throws RemoteException, SecurityException {
        ResolveInfo info = this.mIPackageManager.resolveService(rawIntent, rawIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 0, targetUserId);
        StringBuilder stringBuilder;
        if (info == null || info.serviceInfo == null) {
            String str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Fail to look up the service: ");
            stringBuilder.append(rawIntent);
            stringBuilder.append(" or user ");
            stringBuilder.append(targetUserId);
            stringBuilder.append(" is not running");
            Log.e(str, stringBuilder.toString());
            return null;
        } else if (!expectedPackageName.equals(info.serviceInfo.packageName)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Only allow to bind service in ");
            stringBuilder.append(expectedPackageName);
            throw new SecurityException(stringBuilder.toString());
        } else if (!info.serviceInfo.exported || "android.permission.BIND_DEVICE_ADMIN".equals(info.serviceInfo.permission)) {
            rawIntent.setComponent(info.serviceInfo.getComponentName());
            return rawIntent;
        } else {
            throw new SecurityException("Service must be protected by BIND_DEVICE_ADMIN permission");
        }
    }

    public long getLastSecurityLogRetrievalTime() {
        enforceDeviceOwnerOrManageUsers();
        return getUserData(0).mLastSecurityLogRetrievalTime;
    }

    public long getLastBugReportRequestTime() {
        enforceDeviceOwnerOrManageUsers();
        return getUserData(0).mLastBugReportRequestTime;
    }

    public long getLastNetworkLogRetrievalTime() {
        enforceDeviceOwnerOrManageUsers();
        return getUserData(0).mLastNetworkLogsRetrievalTime;
    }

    public boolean setResetPasswordToken(ComponentName admin, byte[] token) {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        if (token == null || token.length < 32) {
            throw new IllegalArgumentException("token must be at least 32-byte long");
        }
        synchronized (getLockObject()) {
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(admin, -1);
            DevicePolicyData policy = getUserData(userHandle);
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                if (policy.mPasswordTokenHandle != 0) {
                    this.mLockPatternUtils.removeEscrowToken(policy.mPasswordTokenHandle, userHandle);
                }
                policy.mPasswordTokenHandle = this.mLockPatternUtils.addEscrowToken(token, userHandle);
                saveSettingsLocked(userHandle);
                if (policy.mPasswordTokenHandle != 0) {
                    z = true;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:13:0x003b, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean clearResetPasswordToken(ComponentName admin) {
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(admin, -1);
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mPasswordTokenHandle != 0) {
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    boolean result = this.mLockPatternUtils.removeEscrowToken(policy.mPasswordTokenHandle, userHandle);
                    policy.mPasswordTokenHandle = 0;
                    saveSettingsLocked(userHandle);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            } else {
                return false;
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x002f, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isResetPasswordTokenActive(ComponentName admin) {
        synchronized (getLockObject()) {
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(admin, -1);
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mPasswordTokenHandle != 0) {
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    boolean isEscrowTokenActive = this.mLockPatternUtils.isEscrowTokenActive(policy.mPasswordTokenHandle, userHandle);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            } else {
                return false;
            }
        }
    }

    public boolean resetPasswordWithToken(ComponentName admin, String passwordOrNull, byte[] token, int flags) {
        Preconditions.checkNotNull(token);
        synchronized (getLockObject()) {
            int userHandle = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(admin, -1);
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mPasswordTokenHandle != 0) {
                boolean resetPasswordInternal = resetPasswordInternal(passwordOrNull != null ? passwordOrNull : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, policy.mPasswordTokenHandle, token, flags, this.mInjector.binderGetCallingUid(), userHandle);
                return resetPasswordInternal;
            }
            Slog.w(LOG_TAG, "No saved token handle");
            return false;
        }
    }

    public boolean isCurrentInputMethodSetByOwner() {
        enforceProfileOwnerOrSystemUser();
        return getUserData(this.mInjector.userHandleGetCallingUserId()).mCurrentInputMethodSet;
    }

    public StringParceledListSlice getOwnerInstalledCaCerts(UserHandle user) {
        StringParceledListSlice stringParceledListSlice;
        int userId = user.getIdentifier();
        enforceProfileOwnerOrFullCrossUsersPermission(userId);
        synchronized (getLockObject()) {
            stringParceledListSlice = new StringParceledListSlice(new ArrayList(getUserData(userId).mOwnerInstalledCaCerts));
        }
        return stringParceledListSlice;
    }

    public void clearApplicationUserData(ComponentName admin, String packageName, IPackageDataObserver callback) {
        Preconditions.checkNotNull(admin, "ComponentName is null");
        Preconditions.checkNotNull(packageName, "packageName is null");
        Preconditions.checkNotNull(callback, "callback is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(admin, -1);
        }
        int userId = UserHandle.getCallingUserId();
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            ActivityManager.getService().clearApplicationUserData(packageName, false, callback, userId);
        } catch (RemoteException e) {
        } catch (SecurityException se) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not allowed to clear application user data for package ");
            stringBuilder.append(packageName);
            Slog.w(str, stringBuilder.toString(), se);
            try {
                callback.onRemoveCompleted(packageName, false);
            } catch (RemoteException e2) {
            }
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
        this.mInjector.binderRestoreCallingIdentity(ident);
    }

    public void setLogoutEnabled(ComponentName admin, boolean enabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(admin);
            synchronized (getLockObject()) {
                ActiveAdmin deviceOwner = getActiveAdminForCallerLocked(admin, -2);
                if (deviceOwner.isLogoutEnabled == enabled) {
                    return;
                }
                deviceOwner.isLogoutEnabled = enabled;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public boolean isLogoutEnabled() {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            if (deviceOwner != null && deviceOwner.isLogoutEnabled) {
                z = true;
            }
        }
        return z;
    }

    public List<String> getDisallowedSystemApps(ComponentName admin, int userId, String provisioningAction) throws RemoteException {
        enforceCanManageProfileAndDeviceOwners();
        return new ArrayList(this.mOverlayPackagesProvider.getNonRequiredApps(admin, userId, provisioningAction));
    }

    public void transferOwnership(ComponentName admin, ComponentName target, PersistableBundle bundle) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(admin, "Admin cannot be null.");
            Preconditions.checkNotNull(target, "Target cannot be null.");
            enforceProfileOrDeviceOwner(admin);
            if (admin.equals(target)) {
                throw new IllegalArgumentException("Provided administrator and target are the same object.");
            } else if (admin.getPackageName().equals(target.getPackageName())) {
                throw new IllegalArgumentException("Provided administrator and target have the same package name.");
            } else {
                int callingUserId = this.mInjector.userHandleGetCallingUserId();
                DevicePolicyData policy = getUserData(callingUserId);
                DeviceAdminInfo incomingDeviceInfo = findAdmin(target, callingUserId, true);
                checkActiveAdminPrecondition(target, incomingDeviceInfo, policy);
                if (incomingDeviceInfo.supportsTransferOwnership()) {
                    long id = this.mInjector.binderClearCallingIdentity();
                    try {
                        synchronized (getLockObject()) {
                            if (bundle == null) {
                                bundle = new PersistableBundle();
                            }
                            if (isProfileOwner(admin, callingUserId)) {
                                prepareTransfer(admin, target, bundle, callingUserId, LOG_TAG_PROFILE_OWNER);
                                transferProfileOwnershipLocked(admin, target, callingUserId);
                                sendProfileOwnerCommand("android.app.action.TRANSFER_OWNERSHIP_COMPLETE", getTransferOwnershipAdminExtras(bundle), callingUserId);
                                postTransfer("android.app.action.PROFILE_OWNER_CHANGED", callingUserId);
                                if (isUserAffiliatedWithDeviceLocked(callingUserId)) {
                                    notifyAffiliatedProfileTransferOwnershipComplete(callingUserId);
                                }
                            } else if (isDeviceOwner(admin, callingUserId)) {
                                prepareTransfer(admin, target, bundle, callingUserId, LOG_TAG_DEVICE_OWNER);
                                transferDeviceOwnershipLocked(admin, target, callingUserId);
                                sendDeviceOwnerCommand("android.app.action.TRANSFER_OWNERSHIP_COMPLETE", getTransferOwnershipAdminExtras(bundle));
                                postTransfer("android.app.action.DEVICE_OWNER_CHANGED", callingUserId);
                            }
                        }
                        this.mInjector.binderRestoreCallingIdentity(id);
                    } catch (Throwable th) {
                        this.mInjector.binderRestoreCallingIdentity(id);
                    }
                } else {
                    throw new IllegalArgumentException("Provided target does not support ownership transfer.");
                }
            }
        }
    }

    private void prepareTransfer(ComponentName admin, ComponentName target, PersistableBundle bundle, int callingUserId, String adminType) {
        saveTransferOwnershipBundleLocked(bundle, callingUserId);
        this.mTransferOwnershipMetadataManager.saveMetadataFile(new Metadata(admin, target, callingUserId, adminType));
    }

    private void postTransfer(String broadcast, int callingUserId) {
        deleteTransferOwnershipMetadataFileLocked();
        sendOwnerChangedBroadcast(broadcast, callingUserId);
    }

    private void notifyAffiliatedProfileTransferOwnershipComplete(int callingUserId) {
        Bundle extras = new Bundle();
        extras.putParcelable("android.intent.extra.USER", UserHandle.of(callingUserId));
        sendDeviceOwnerCommand("android.app.action.AFFILIATED_PROFILE_TRANSFER_OWNERSHIP_COMPLETE", extras);
    }

    private void transferProfileOwnershipLocked(ComponentName admin, ComponentName target, int profileOwnerUserId) {
        transferActiveAdminUncheckedLocked(target, admin, profileOwnerUserId);
        this.mOwners.transferProfileOwner(target, profileOwnerUserId);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Profile owner set: ");
        stringBuilder.append(target);
        stringBuilder.append(" on user ");
        stringBuilder.append(profileOwnerUserId);
        Slog.i(str, stringBuilder.toString());
        this.mOwners.writeProfileOwner(profileOwnerUserId);
        this.mDeviceAdminServiceController.startServiceForOwner(target.getPackageName(), profileOwnerUserId, "transfer-profile-owner");
    }

    private void transferDeviceOwnershipLocked(ComponentName admin, ComponentName target, int userId) {
        transferActiveAdminUncheckedLocked(target, admin, userId);
        this.mOwners.transferDeviceOwnership(target);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Device owner set: ");
        stringBuilder.append(target);
        stringBuilder.append(" on user ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        this.mOwners.writeDeviceOwner();
        this.mDeviceAdminServiceController.startServiceForOwner(target.getPackageName(), userId, "transfer-device-owner");
    }

    private Bundle getTransferOwnershipAdminExtras(PersistableBundle bundle) {
        Bundle extras = new Bundle();
        if (bundle != null) {
            extras.putParcelable("android.app.extra.TRANSFER_OWNERSHIP_ADMIN_EXTRAS_BUNDLE", bundle);
        }
        return extras;
    }

    public void setStartUserSessionMessage(ComponentName admin, CharSequence startUserSessionMessage) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(admin);
            String startUserSessionMessageString = startUserSessionMessage != null ? startUserSessionMessage.toString() : null;
            synchronized (getLockObject()) {
                ActiveAdmin deviceOwner = getActiveAdminForCallerLocked(admin, -2);
                if (TextUtils.equals(deviceOwner.startUserSessionMessage, startUserSessionMessage)) {
                    return;
                }
                deviceOwner.startUserSessionMessage = startUserSessionMessageString;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
                this.mInjector.getActivityManagerInternal().setSwitchingFromSystemUserMessage(startUserSessionMessageString);
            }
        }
    }

    public void setEndUserSessionMessage(ComponentName admin, CharSequence endUserSessionMessage) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(admin);
            String endUserSessionMessageString = endUserSessionMessage != null ? endUserSessionMessage.toString() : null;
            synchronized (getLockObject()) {
                ActiveAdmin deviceOwner = getActiveAdminForCallerLocked(admin, -2);
                if (TextUtils.equals(deviceOwner.endUserSessionMessage, endUserSessionMessage)) {
                    return;
                }
                deviceOwner.endUserSessionMessage = endUserSessionMessageString;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
                this.mInjector.getActivityManagerInternal().setSwitchingToSystemUserMessage(endUserSessionMessageString);
            }
        }
    }

    public String getStartUserSessionMessage(ComponentName admin) {
        if (!this.mHasFeature) {
            return null;
        }
        String str;
        Preconditions.checkNotNull(admin);
        synchronized (getLockObject()) {
            str = getActiveAdminForCallerLocked(admin, -2).startUserSessionMessage;
        }
        return str;
    }

    public String getEndUserSessionMessage(ComponentName admin) {
        if (!this.mHasFeature) {
            return null;
        }
        String str;
        Preconditions.checkNotNull(admin);
        synchronized (getLockObject()) {
            str = getActiveAdminForCallerLocked(admin, -2).endUserSessionMessage;
        }
        return str;
    }

    private void deleteTransferOwnershipMetadataFileLocked() {
        this.mTransferOwnershipMetadataManager.deleteMetadataFile();
    }

    public PersistableBundle getTransferOwnershipBundle() {
        FileInputStream stream;
        Throwable th;
        Throwable th2;
        synchronized (getLockObject()) {
            int callingUserId = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(null, -1);
            File bundleFile = new File(this.mInjector.environmentGetUserSystemDirectory(callingUserId), TRANSFER_OWNERSHIP_PARAMETERS_XML);
            if (bundleFile.exists()) {
                try {
                    stream = new FileInputStream(bundleFile);
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(stream, null);
                        parser.next();
                        PersistableBundle restoreFromXml = PersistableBundle.restoreFromXml(parser);
                        $closeResource(null, stream);
                        return restoreFromXml;
                    } catch (Throwable th22) {
                        Throwable th3 = th22;
                        th22 = th;
                        th = th3;
                    }
                } catch (IOException | IllegalArgumentException | XmlPullParserException e) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Caught exception while trying to load the owner transfer parameters from file ");
                    stringBuilder.append(bundleFile);
                    Slog.e(str, stringBuilder.toString(), e);
                    return null;
                }
            }
            return null;
        }
        $closeResource(th22, stream);
        throw th;
    }

    public int addOverrideApn(ComponentName who, ApnSetting apnSetting) {
        if (!this.mHasFeature) {
            return -1;
        }
        Preconditions.checkNotNull(who, "ComponentName is null in addOverrideApn");
        Preconditions.checkNotNull(apnSetting, "ApnSetting is null in addOverrideApn");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        int operatedId = -1;
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            Uri resultUri = this.mContext.getContentResolver().insert(Carriers.DPC_URI, apnSetting.toContentValues());
            if (resultUri != null) {
                try {
                    operatedId = Integer.parseInt(resultUri.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Slog.e(LOG_TAG, "Failed to parse inserted override APN id.", e);
                }
            }
            return operatedId;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public boolean updateOverrideApn(ComponentName who, int apnId, ApnSetting apnSetting) {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null in updateOverrideApn");
        Preconditions.checkNotNull(apnSetting, "ApnSetting is null in updateOverrideApn");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        if (apnId < 0) {
            return false;
        }
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            if (this.mContext.getContentResolver().update(Uri.withAppendedPath(Carriers.DPC_URI, Integer.toString(apnId)), apnSetting.toContentValues(), null, null) > 0) {
                z = true;
            }
            this.mInjector.binderRestoreCallingIdentity(id);
            return z;
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public boolean removeOverrideApn(ComponentName who, int apnId) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null in removeOverrideApn");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        return removeOverrideApnUnchecked(apnId);
    }

    private boolean removeOverrideApnUnchecked(int apnId) {
        boolean z = false;
        if (apnId < 0) {
            return false;
        }
        int numDeleted = 0;
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            numDeleted = this.mContext.getContentResolver().delete(Uri.withAppendedPath(Carriers.DPC_URI, Integer.toString(apnId)), null, null);
            if (numDeleted > 0) {
                z = true;
            }
            return z;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public List<ApnSetting> getOverrideApns(ComponentName who) {
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Preconditions.checkNotNull(who, "ComponentName is null in getOverrideApns");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        return getOverrideApnsUnchecked();
    }

    private List<ApnSetting> getOverrideApnsUnchecked() {
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            Cursor cursor = this.mContext.getContentResolver().query(Carriers.DPC_URI, null, null, null, null);
            if (cursor == null) {
                return Collections.emptyList();
            }
            try {
                List<ApnSetting> apnList = new ArrayList();
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    apnList.add(ApnSetting.makeApnSetting(cursor));
                }
                return apnList;
            } finally {
                cursor.close();
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public void setOverrideApnsEnabled(ComponentName who, boolean enabled) {
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null in setOverrideApnEnabled");
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(who, -2);
            }
            setOverrideApnsEnabledUnchecked(enabled);
        }
    }

    private void setOverrideApnsEnabledUnchecked(boolean enabled) {
        ContentValues value = new ContentValues();
        value.put("enforced", Boolean.valueOf(enabled));
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            this.mContext.getContentResolver().update(Carriers.ENFORCE_MANAGED_URI, value, null, null);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public boolean isOverrideApnEnabled(ComponentName who) {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null in isOverrideApnEnabled");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, -2);
        }
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            Cursor enforceCursor = this.mContext.getContentResolver().query(Carriers.ENFORCE_MANAGED_URI, null, null, null, null);
            if (enforceCursor == null) {
                return z;
            }
            try {
                if (enforceCursor.moveToFirst()) {
                    if (enforceCursor.getInt(enforceCursor.getColumnIndex("enforced")) == 1) {
                        z = true;
                    }
                    enforceCursor.close();
                    return z;
                }
            } catch (IllegalArgumentException e) {
                Slog.e(LOG_TAG, "Cursor returned from ENFORCE_MANAGED_URI doesn't contain correct info.", e);
            } catch (Throwable th) {
                enforceCursor.close();
            }
            enforceCursor.close();
            return z;
        } finally {
            z = this.mInjector;
            z.binderRestoreCallingIdentity(id);
        }
    }

    @VisibleForTesting
    void saveTransferOwnershipBundleLocked(PersistableBundle bundle, int userId) {
        File parametersFile = new File(this.mInjector.environmentGetUserSystemDirectory(userId), TRANSFER_OWNERSHIP_PARAMETERS_XML);
        AtomicFile atomicFile = new AtomicFile(parametersFile);
        FileOutputStream stream = null;
        try {
            stream = atomicFile.startWrite();
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(stream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.startTag(null, TAG_TRANSFER_OWNERSHIP_BUNDLE);
            bundle.saveToXml(serializer);
            serializer.endTag(null, TAG_TRANSFER_OWNERSHIP_BUNDLE);
            serializer.endDocument();
            atomicFile.finishWrite(stream);
        } catch (IOException | XmlPullParserException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Caught exception while trying to save the owner transfer parameters to file ");
            stringBuilder.append(parametersFile);
            Slog.e(str, stringBuilder.toString(), e);
            parametersFile.delete();
            atomicFile.failWrite(stream);
        }
    }

    void deleteTransferOwnershipBundleLocked(int userId) {
        new File(this.mInjector.environmentGetUserSystemDirectory(userId), TRANSFER_OWNERSHIP_PARAMETERS_XML).delete();
    }

    protected void clearWipeDataFactoryLowlevel(String reason, boolean wipeEuicc) {
        boolean success = false;
        try {
            this.mInjector.recoverySystemRebootWipeUserData(false, reason, true, wipeEuicc);
            if (true) {
                return;
            }
        } catch (IOException | SecurityException e) {
            Slog.w(LOG_TAG, "Failed requesting data wipe", e);
            if (success) {
                return;
            }
        } catch (Throwable th) {
            if (!success) {
                SecurityLog.writeEvent(210023, new Object[0]);
            }
        }
        SecurityLog.writeEvent(210023, new Object[0]);
    }

    private void maybeLogPasswordComplexitySet(ComponentName who, int userId, boolean parent, PasswordMetrics metrics) {
        if (SecurityLog.isLoggingEnabled()) {
            int affectedUserId = parent ? getProfileParentId(userId) : userId;
            SecurityLog.writeEvent(210017, new Object[]{who.getPackageName(), Integer.valueOf(userId), Integer.valueOf(affectedUserId), Integer.valueOf(metrics.length), Integer.valueOf(metrics.quality), Integer.valueOf(metrics.letters), Integer.valueOf(metrics.nonLetter), Integer.valueOf(metrics.numeric), Integer.valueOf(metrics.upperCase), Integer.valueOf(metrics.lowerCase), Integer.valueOf(metrics.symbols)});
        }
    }

    private static String getManagedProvisioningPackage(Context context) {
        return context.getResources().getString(17039827);
    }
}
