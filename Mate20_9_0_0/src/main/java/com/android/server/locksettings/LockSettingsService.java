package com.android.server.locksettings;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.admin.PasswordMetrics;
import android.app.backup.BackupManager;
import android.app.trust.IStrongAuthTracker;
import android.app.trust.TrustManager;
import android.common.HwFrameworkFactory;
import android.common.HwFrameworkMonitor;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteDatabase;
import android.encrypt.ISDCardCryptedHelper;
import android.hardware.authsecret.V1_0.IAuthSecret;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IProgressListener;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IStorageManager;
import android.os.storage.IStorageManager.Stub;
import android.os.storage.StorageManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.security.KeyStore;
import android.security.KeyStore.State;
import android.security.keystore.AndroidKeyStoreProvider;
import android.security.keystore.KeyProtection;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.RecoveryCertPath;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.service.gatekeeper.GateKeeperResponse;
import android.service.gatekeeper.IGateKeeperService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.StrongAuthTracker;
import com.android.internal.widget.LockSettingsInternal;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IHwLockSettingsService;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerService;
import com.android.server.locksettings.LockSettingsStorage.Callback;
import com.android.server.locksettings.LockSettingsStorage.CredentialHash;
import com.android.server.locksettings.LockSettingsStorage.PersistentData;
import com.android.server.locksettings.recoverablekeystore.RecoverableKeyStoreManager;
import com.android.server.power.IHwShutdownThread;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import libcore.util.HexEncoding;

public class LockSettingsService extends AbsLockSettingsService {
    private static final Intent ACTION_NULL = new Intent("android.intent.action.MAIN");
    protected static final boolean DEBUG = false;
    private static final String PERMISSION = "android.permission.ACCESS_KEYGUARD_SECURE_STORAGE";
    protected static final int PROFILE_KEY_IV_SIZE = 12;
    private static final String[] READ_CONTACTS_PROTECTED_SETTINGS = new String[]{"lock_screen_owner_info_enabled", "lock_screen_owner_info"};
    private static final String[] READ_PASSWORD_PROTECTED_SETTINGS = new String[]{"lockscreen.password_salt", "lockscreen.passwordhistory", "lockscreen.password_type", SEPARATE_PROFILE_CHALLENGE_KEY};
    private static final String SEPARATE_PROFILE_CHALLENGE_KEY = "lockscreen.profilechallenge";
    private static final String[] SETTINGS_TO_BACKUP = new String[]{"lock_screen_owner_info_enabled", "lock_screen_owner_info", "lock_pattern_visible_pattern", "lockscreen.power_button_instantly_locks"};
    private static final int SYNTHETIC_PASSWORD_ENABLED_BY_DEFAULT = 1;
    private static final int[] SYSTEM_CREDENTIAL_UIDS = new int[]{1010, 1016, 0, 1000};
    private static final String TAG = "LockSettingsService";
    private static final String[] VALID_SETTINGS = new String[]{"lockscreen.lockedoutpermanently", "lockscreen.patterneverchosen", "lockscreen.password_type", "lockscreen.password_type_alternate", "lockscreen.password_salt", "lockscreen.disabled", "lockscreen.options", "lockscreen.biometric_weak_fallback", "lockscreen.biometricweakeverchosen", "lockscreen.power_button_instantly_locks", "lockscreen.passwordhistory", "lock_pattern_autolock", "lock_biometric_weak_flags", "lock_pattern_visible_pattern", "lock_pattern_tactile_feedback_enabled"};
    private static HwFrameworkMonitor mMonitor = HwFrameworkFactory.getHwFrameworkMonitor();
    private final String ACTION_PRIVACY_USER_ADDED_FINISHED;
    private final IActivityManager mActivityManager;
    protected IAuthSecret mAuthSecretService;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final DeviceProvisionedObserver mDeviceProvisionedObserver;
    private boolean mFirstCallToVold;
    protected IGateKeeperService mGateKeeperService;
    @VisibleForTesting
    protected final Handler mHandler;
    private final Injector mInjector;
    private final KeyStore mKeyStore;
    protected LockPatternUtils mLockPatternUtils;
    private final NotificationManager mNotificationManager;
    private final RecoverableKeyStoreManager mRecoverableKeyStoreManager;
    private final Object mSeparateChallengeLock;
    @GuardedBy("mSpManager")
    private SparseArray<AuthenticationToken> mSpCache;
    private final SyntheticPasswordManager mSpManager;
    @VisibleForTesting
    protected final LockSettingsStorage mStorage;
    protected final LockSettingsStrongAuth mStrongAuth;
    private final SynchronizedStrongAuthTracker mStrongAuthTracker;
    protected final UserManager mUserManager;

    private class DeviceProvisionedObserver extends ContentObserver {
        private final Uri mDeviceProvisionedUri = Global.getUriFor("device_provisioned");
        private boolean mRegistered;
        private final Uri mUserSetupCompleteUri = Secure.getUriFor("user_setup_complete");

        public DeviceProvisionedObserver() {
            super(null);
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mDeviceProvisionedUri.equals(uri)) {
                updateRegistration();
                if (isProvisioned()) {
                    Slog.i(LockSettingsService.TAG, "Reporting device setup complete to IGateKeeperService");
                    reportDeviceSetupComplete();
                    clearFrpCredentialIfOwnerNotSecure();
                }
            } else if (this.mUserSetupCompleteUri.equals(uri)) {
                LockSettingsService.this.tryRemoveUserFromSpCacheLater(userId);
            }
        }

        public void onSystemReady() {
            if (LockPatternUtils.frpCredentialEnabled(LockSettingsService.this.mContext)) {
                updateRegistration();
            } else if (!isProvisioned()) {
                Slog.i(LockSettingsService.TAG, "FRP credential disabled, reporting device setup complete to Gatekeeper immediately");
                reportDeviceSetupComplete();
            }
        }

        private void reportDeviceSetupComplete() {
            try {
                LockSettingsService.this.getGateKeeperService().reportDeviceSetupComplete();
            } catch (RemoteException e) {
                Slog.e(LockSettingsService.TAG, "Failure reporting to IGateKeeperService", e);
            }
        }

        private void clearFrpCredentialIfOwnerNotSecure() {
            for (UserInfo user : LockSettingsService.this.mUserManager.getUsers()) {
                if (LockPatternUtils.userOwnsFrpCredential(LockSettingsService.this.mContext, user)) {
                    if (!LockSettingsService.this.isUserSecure(user.id)) {
                        LockSettingsService.this.mStorage.writePersistentDataBlock(0, user.id, 0, null);
                    }
                    return;
                }
            }
        }

        private void updateRegistration() {
            boolean register = isProvisioned() ^ 1;
            if (register != this.mRegistered) {
                if (register) {
                    LockSettingsService.this.mContext.getContentResolver().registerContentObserver(this.mDeviceProvisionedUri, false, this);
                    LockSettingsService.this.mContext.getContentResolver().registerContentObserver(this.mUserSetupCompleteUri, false, this, -1);
                } else {
                    LockSettingsService.this.mContext.getContentResolver().unregisterContentObserver(this);
                }
                this.mRegistered = register;
            }
        }

        private boolean isProvisioned() {
            return Global.getInt(LockSettingsService.this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        }
    }

    private class GateKeeperDiedRecipient implements DeathRecipient {
        private GateKeeperDiedRecipient() {
        }

        /* synthetic */ GateKeeperDiedRecipient(LockSettingsService x0, AnonymousClass1 x1) {
            this();
        }

        public void binderDied() {
            LockSettingsService.this.mGateKeeperService.asBinder().unlinkToDeath(this, 0);
            LockSettingsService.this.mGateKeeperService = null;
        }
    }

    static class Injector {
        protected Context mContext;

        public Injector(Context context) {
            this.mContext = context;
        }

        public Context getContext() {
            return this.mContext;
        }

        public Handler getHandler() {
            return new Handler();
        }

        public LockSettingsStorage getStorage() {
            final LockSettingsStorage storage = new LockSettingsStorage(this.mContext);
            storage.setDatabaseOnCreateCallback(new Callback() {
                public void initialize(SQLiteDatabase db) {
                    if (SystemProperties.getBoolean("ro.lockscreen.disable.default", false)) {
                        storage.writeKeyValue(db, "lockscreen.disabled", "1", 0);
                    }
                }
            });
            return storage;
        }

        public LockSettingsStrongAuth getStrongAuth() {
            return new LockSettingsStrongAuth(this.mContext);
        }

        public SynchronizedStrongAuthTracker getStrongAuthTracker() {
            return new SynchronizedStrongAuthTracker(this.mContext);
        }

        public IActivityManager getActivityManager() {
            return ActivityManager.getService();
        }

        public LockPatternUtils getLockPatternUtils() {
            return new LockPatternUtils(this.mContext);
        }

        public NotificationManager getNotificationManager() {
            return (NotificationManager) this.mContext.getSystemService("notification");
        }

        public UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService("user");
        }

        public DevicePolicyManager getDevicePolicyManager() {
            return (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        }

        public KeyStore getKeyStore() {
            return KeyStore.getInstance();
        }

        public RecoverableKeyStoreManager getRecoverableKeyStoreManager(KeyStore keyStore) {
            return RecoverableKeyStoreManager.getInstance(this.mContext, keyStore);
        }

        public IStorageManager getStorageManager() {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                return Stub.asInterface(service);
            }
            return null;
        }

        public SyntheticPasswordManager getSyntheticPasswordManager(LockSettingsStorage storage) {
            return new SyntheticPasswordManager(getContext(), storage, getUserManager());
        }

        public int binderGetCallingUid() {
            return Binder.getCallingUid();
        }
    }

    private final class LocalService extends LockSettingsInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(LockSettingsService x0, AnonymousClass1 x1) {
            this();
        }

        public long addEscrowToken(byte[] token, int userId) {
            try {
                return LockSettingsService.this.addEscrowToken(token, userId);
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public boolean removeEscrowToken(long handle, int userId) {
            return LockSettingsService.this.removeEscrowToken(handle, userId);
        }

        public boolean isEscrowTokenActive(long handle, int userId) {
            return LockSettingsService.this.isEscrowTokenActive(handle, userId);
        }

        public boolean setLockCredentialWithToken(String credential, int type, long tokenHandle, byte[] token, int requestedQuality, int userId) {
            try {
                return LockSettingsService.this.setLockCredentialWithToken(credential, type, tokenHandle, token, requestedQuality, userId);
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public boolean unlockUserWithToken(long tokenHandle, byte[] token, int userId) {
            try {
                return LockSettingsService.this.unlockUserWithToken(tokenHandle, token, userId);
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }
    }

    @VisibleForTesting
    protected static class SynchronizedStrongAuthTracker extends StrongAuthTracker {
        public SynchronizedStrongAuthTracker(Context context) {
            super(context);
        }

        protected void handleStrongAuthRequiredChanged(int strongAuthFlags, int userId) {
            synchronized (this) {
                super.handleStrongAuthRequiredChanged(strongAuthFlags, userId);
            }
        }

        public int getStrongAuthForUser(int userId) {
            int strongAuthForUser;
            synchronized (this) {
                strongAuthForUser = super.getStrongAuthForUser(userId);
            }
            return strongAuthForUser;
        }

        void register(LockSettingsStrongAuth strongAuth) {
            strongAuth.registerStrongAuthTracker(this.mStub);
        }
    }

    public static final class Lifecycle extends SystemService {
        private LockSettingsService mLockSettingsService;

        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            AndroidKeyStoreProvider.install();
            IHwLockSettingsService iLockSettingsService = HwServiceFactory.getHuaweiLockSettingsService();
            if (iLockSettingsService != null) {
                this.mLockSettingsService = iLockSettingsService.getInstance(getContext());
            } else {
                this.mLockSettingsService = new LockSettingsService(getContext());
            }
            publishBinderService("lock_settings", this.mLockSettingsService);
        }

        public void onBootPhase(int phase) {
            super.onBootPhase(phase);
            if (phase == 550) {
                this.mLockSettingsService.migrateOldDataAfterSystemReady();
            }
            if (phase == 550) {
                this.mLockSettingsService.showEncryptionNotificationForUsers();
            }
        }

        public void onStartUser(int userHandle) {
            this.mLockSettingsService.onStartUser(userHandle);
        }

        public void onUnlockUser(int userHandle) {
            this.mLockSettingsService.onUnlockUser(userHandle);
        }

        public void onCleanupUser(int userHandle) {
            this.mLockSettingsService.onCleanupUser(userHandle);
        }
    }

    static {
        ACTION_NULL.addCategory("android.intent.category.HOME");
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x006f A:{Splitter: B:18:0x0041, ExcHandler: java.security.NoSuchAlgorithmException (r2_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:20:0x006f, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:21:0x0070, code:
            android.util.Slog.e(TAG, "Fail to tie managed profile", r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void tieManagedProfileLockIfNecessary(int managedUserId, String managedUserPassword) {
        if (this.mUserManager.getUserInfo(managedUserId).isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(managedUserId) && !this.mStorage.hasChildProfileLock(managedUserId)) {
            int parentId = this.mUserManager.getProfileParent(managedUserId).id;
            if (isUserSecure(parentId)) {
                try {
                    if (getGateKeeperService().getSecureUserId(parentId) != 0) {
                        byte[] randomLockSeed = new byte[null];
                        try {
                            String newPassword = String.valueOf(HexEncoding.encode(SecureRandom.getInstance("SHA1PRNG").generateSeed(40)));
                            setLockCredentialInternal(newPassword, 2, managedUserPassword, 327680, managedUserId);
                            setLong("lockscreen.password_type", 327680, managedUserId);
                            tieProfileLockToParent(managedUserId, newPassword);
                        } catch (Exception e) {
                        }
                    }
                } catch (RemoteException e2) {
                    Slog.e(TAG, "Failed to talk to GateKeeper service", e2);
                }
            }
        }
    }

    public LockSettingsService(Context context) {
        this(new Injector(context));
    }

    @VisibleForTesting
    protected LockSettingsService(Injector injector) {
        this.mSeparateChallengeLock = new Object();
        this.mDeviceProvisionedObserver = new DeviceProvisionedObserver();
        this.ACTION_PRIVACY_USER_ADDED_FINISHED = "com.huawei.android.lockSettingService.action.USER_ADDED_FINISHED ";
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int userHandle;
                if ("android.intent.action.USER_ADDED".equals(intent.getAction())) {
                    userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    if (userHandle > 0) {
                        LockSettingsService.this.removeUser(userHandle, true);
                    }
                    KeyStore ks = KeyStore.getInstance();
                    UserInfo parentInfo = LockSettingsService.this.mUserManager.getProfileParent(userHandle);
                    ks.onUserAdded(userHandle, parentInfo != null ? parentInfo.id : -1);
                    UserInfo userInfo = LockSettingsService.this.mUserManager.getUserInfo(userHandle);
                    if (userInfo != null && userInfo.isHwHiddenSpace()) {
                        Intent finishIntent = new Intent("com.huawei.android.lockSettingService.action.USER_ADDED_FINISHED ");
                        finishIntent.putExtra("android.intent.extra.user_handle", userHandle);
                        Slog.d(LockSettingsService.TAG, "notify that hiden user has been added.");
                        LockSettingsService.this.mContext.sendBroadcastAsUser(finishIntent, UserHandle.ALL, "android.permission.MANAGE_USERS");
                    }
                } else if ("android.intent.action.USER_STARTING".equals(intent.getAction())) {
                    LockSettingsService.this.mStorage.prefetchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                    userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    if (userHandle > 0) {
                        LockSettingsService.this.removeUser(userHandle, false);
                    }
                }
            }
        };
        this.mSpCache = new SparseArray();
        this.mInjector = injector;
        this.mContext = injector.getContext();
        this.mKeyStore = injector.getKeyStore();
        this.mRecoverableKeyStoreManager = injector.getRecoverableKeyStoreManager(this.mKeyStore);
        this.mHandler = injector.getHandler();
        this.mStrongAuth = injector.getStrongAuth();
        this.mActivityManager = injector.getActivityManager();
        this.mLockPatternUtils = injector.getLockPatternUtils();
        this.mFirstCallToVold = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_STARTING");
        filter.addAction("android.intent.action.USER_REMOVED");
        injector.getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        this.mStorage = injector.getStorage();
        this.mNotificationManager = injector.getNotificationManager();
        this.mUserManager = injector.getUserManager();
        this.mStrongAuthTracker = injector.getStrongAuthTracker();
        this.mStrongAuthTracker.register(this.mStrongAuth);
        this.mSpManager = injector.getSyntheticPasswordManager(this.mStorage);
        LocalServices.addService(LockSettingsInternal.class, new LocalService(this, null));
    }

    private void maybeShowEncryptionNotificationForUser(int userId) {
        UserInfo user = this.mUserManager.getUserInfo(userId);
        if (user.isManagedProfile()) {
            UserHandle userHandle = user.getUserHandle();
            if (isUserSecure(userId) && !this.mUserManager.isUserUnlockingOrUnlocked(userHandle)) {
                UserInfo parent = this.mUserManager.getProfileParent(userId);
                if (!(parent == null || !this.mUserManager.isUserUnlockingOrUnlocked(parent.getUserHandle()) || this.mUserManager.isQuietModeEnabled(userHandle))) {
                    showEncryptionNotificationForProfile(userHandle);
                }
            }
        }
    }

    private void showEncryptionNotificationForUsers() {
        List<UserInfo> users = this.mUserManager.getUsers();
        for (int i = 0; i < users.size(); i++) {
            UserInfo user = (UserInfo) users.get(i);
            if (!user.isClonedProfile()) {
                UserHandle userHandle = user.getUserHandle();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("user.id = ");
                stringBuilder.append(user.id);
                Slog.i(str, stringBuilder.toString());
                if (!(!isUserSecure(user.id) || this.mUserManager.isUserUnlockingOrUnlocked(userHandle) || user.isManagedProfile())) {
                    Slog.i(TAG, "has password,show notification");
                    showEncryptionNotificationForUser(userHandle);
                }
            }
        }
    }

    private void showEncryptionNotificationForUser(UserHandle user) {
        Resources r = this.mContext.getResources();
        showEncryptionNotification(user, r.getText(33685896), r.getText(33685897), r.getText(17041306), PendingIntent.getBroadcast(this.mContext, 0, ACTION_NULL, 134217728));
    }

    private void showEncryptionNotificationForProfile(UserHandle user) {
        Resources r = this.mContext.getResources();
        CharSequence title = r.getText(17041308);
        CharSequence message = r.getText(17040955);
        CharSequence detail = r.getText(17040954);
        Intent unlockIntent = ((KeyguardManager) this.mContext.getSystemService("keyguard")).createConfirmDeviceCredentialIntent(null, null, user.getIdentifier());
        if (unlockIntent != null) {
            unlockIntent.setFlags(276824064);
            showEncryptionNotification(user, title, message, detail, PendingIntent.getActivity(this.mContext, 0, unlockIntent, 134217728));
        }
    }

    private void showEncryptionNotification(UserHandle user, CharSequence title, CharSequence message, CharSequence detail, PendingIntent intent) {
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            this.mNotificationManager.notifyAsUser(null, 9, new Builder(this.mContext, SystemNotificationChannels.SECURITY).setSmallIcon(17302778).setWhen(0).setOngoing(true).setTicker(title).setColor(this.mContext.getColor(17170784)).setContentTitle(title).setContentText(message).setSubText(detail).setVisibility(1).setContentIntent(intent).build(), user);
        }
    }

    private void hideEncryptionNotification(UserHandle userHandle) {
        this.mNotificationManager.cancelAsUser(null, 9, userHandle);
    }

    public void onCleanupUser(int userId) {
        hideEncryptionNotification(new UserHandle(userId));
        requireStrongAuth(1, userId);
    }

    public void onStartUser(int userId) {
        maybeShowEncryptionNotificationForUser(userId);
    }

    private void ensureProfileKeystoreUnlocked(int userId) {
        if (KeyStore.getInstance().state(userId) == State.LOCKED && tiedManagedProfileReadyToUnlock(this.mUserManager.getUserInfo(userId))) {
            Slog.i(TAG, "Managed profile got unlocked, will unlock its keystore");
            try {
                unlockChildProfile(userId, true);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to unlock child profile");
            }
        }
    }

    public void onUnlockUser(final int userId) {
        this.mHandler.post(new Runnable() {
            public void run() {
                LockSettingsService.this.ensureProfileKeystoreUnlocked(userId);
                LockSettingsService.this.hideEncryptionNotification(new UserHandle(userId));
                List<UserInfo> profiles = LockSettingsService.this.mUserManager.getProfiles(userId);
                for (int i = 0; i < profiles.size(); i++) {
                    UserInfo profile = (UserInfo) profiles.get(i);
                    if (LockSettingsService.this.isUserSecure(profile.id) && profile.isManagedProfile()) {
                        UserHandle userHandle = profile.getUserHandle();
                        if (!(LockSettingsService.this.mUserManager.isUserUnlockingOrUnlocked(userHandle) || LockSettingsService.this.mUserManager.isQuietModeEnabled(userHandle))) {
                            LockSettingsService.this.showEncryptionNotificationForProfile(userHandle);
                        }
                    }
                }
                if (LockSettingsService.this.mUserManager.getUserInfo(userId).isManagedProfile()) {
                    LockSettingsService.this.tieManagedProfileLockIfNecessary(userId, null);
                }
                if (LockSettingsService.this.mUserManager.getUserInfo(userId).isPrimary() && !LockSettingsService.this.isUserSecure(userId)) {
                    LockSettingsService.this.tryDeriveAuthTokenForUnsecuredPrimaryUser(userId);
                }
            }
        });
    }

    private void tryDeriveAuthTokenForUnsecuredPrimaryUser(int userId) {
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                try {
                    AuthenticationResult result = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), getSyntheticPasswordHandleLocked(userId), null, userId, null);
                    String str;
                    StringBuilder stringBuilder;
                    if (result.authToken != null) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Retrieved auth token for user ");
                        stringBuilder.append(userId);
                        Slog.i(str, stringBuilder.toString());
                        onAuthTokenKnownForUser(userId, result.authToken);
                    } else {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Auth token not available for user ");
                        stringBuilder.append(userId);
                        Slog.e(str, stringBuilder.toString());
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failure retrieving auth token", e);
                }
            } else {
                return;
            }
        }
    }

    public void systemReady() {
        if (this.mContext.checkCallingOrSelfPermission(PERMISSION) != 0) {
            EventLog.writeEvent(1397638484, new Object[]{"28251513", Integer.valueOf(getCallingUid()), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS});
        }
        checkWritePermission(0);
        migrateOldData();
        try {
            getGateKeeperService();
            this.mSpManager.initWeaverService();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failure retrieving IGateKeeperService", e);
        }
        try {
            this.mAuthSecretService = IAuthSecret.getService();
        } catch (NoSuchElementException e2) {
            Slog.i(TAG, "Device doesn't implement AuthSecret HAL");
        } catch (RemoteException e3) {
            Slog.w(TAG, "Failed to get AuthSecret HAL", e3);
        }
        this.mDeviceProvisionedObserver.onSystemReady();
        this.mStorage.prefetchUser(0);
        this.mStrongAuth.systemReady();
    }

    /* JADX WARNING: Removed duplicated region for block: B:79:0x01fb A:{Splitter: B:75:0x01d4, ExcHandler: java.security.KeyStoreException (r0_49 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x01fb A:{Splitter: B:75:0x01d4, ExcHandler: java.security.KeyStoreException (r0_49 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x01fb A:{Splitter: B:75:0x01d4, ExcHandler: java.security.KeyStoreException (r0_49 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:79:0x01fb, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:80:0x01fc, code:
            android.util.Slog.e(TAG, "Unable to remove tied profile key", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void migrateOldData() {
        int i;
        int i2;
        int user;
        List<UserInfo> users;
        int i3;
        if (getString("migrated", null, 0) == null) {
            ContentResolver cr = this.mContext.getContentResolver();
            for (String validSetting : VALID_SETTINGS) {
                String value = Secure.getString(cr, validSetting);
                if (value != null) {
                    setString(validSetting, value, 0);
                }
            }
            setString("migrated", "true", 0);
            Slog.i(TAG, "Migrated lock settings to new location");
        }
        if (getString("migrated_user_specific", null, 0) == null) {
            ContentResolver cr2 = this.mContext.getContentResolver();
            List<UserInfo> users2 = this.mUserManager.getUsers();
            user = 0;
            while (true) {
                int user2 = user;
                if (user2 >= users2.size()) {
                    break;
                }
                int userId = ((UserInfo) users2.get(user2)).id;
                String OWNER_INFO = "lock_screen_owner_info";
                String ownerInfo = Secure.getStringForUser(cr2, "lock_screen_owner_info", userId);
                if (!TextUtils.isEmpty(ownerInfo)) {
                    setString("lock_screen_owner_info", ownerInfo, userId);
                    Secure.putStringForUser(cr2, "lock_screen_owner_info", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, userId);
                }
                String OWNER_INFO_ENABLED = "lock_screen_owner_info_enabled";
                try {
                    setLong("lock_screen_owner_info_enabled", Secure.getIntForUser(cr2, "lock_screen_owner_info_enabled", userId) != 0 ? 1 : 0, userId);
                } catch (SettingNotFoundException e) {
                    if (!TextUtils.isEmpty(ownerInfo)) {
                        setLong("lock_screen_owner_info_enabled", 1, userId);
                    }
                }
                Secure.putIntForUser(cr2, "lock_screen_owner_info_enabled", 0, userId);
                user = user2 + 1;
            }
            setString("migrated_user_specific", "true", 0);
            Slog.i(TAG, "Migrated per-user lock settings to new location");
        }
        if (getString("migrated_biometric_weak", null, 0) == null) {
            users = this.mUserManager.getUsers();
            for (i3 = 0; i3 < users.size(); i3++) {
                i2 = ((UserInfo) users.get(i3)).id;
                long type = getLong("lockscreen.password_type", 0, i2);
                long alternateType = getLong("lockscreen.password_type_alternate", 0, i2);
                if (type == 32768) {
                    setLong("lockscreen.password_type", alternateType, i2);
                }
                setLong("lockscreen.password_type_alternate", 0, i2);
            }
            setString("migrated_biometric_weak", "true", 0);
            Slog.i(TAG, "Migrated biometric weak to use the fallback instead");
        }
        if (getString("migrated_lockscreen_disabled", null, 0) == null) {
            users = this.mUserManager.getUsers();
            i3 = users.size();
            i = 0;
            for (i2 = 0; i2 < i3; i2++) {
                if (((UserInfo) users.get(i2)).supportsSwitchTo()) {
                    i++;
                }
            }
            if (i > 1) {
                for (i2 = 0; i2 < i3; i2++) {
                    int id = ((UserInfo) users.get(i2)).id;
                    if (getBoolean("lockscreen.disabled", false, id)) {
                        setBoolean("lockscreen.disabled", false, id);
                    }
                }
            }
            setString("migrated_lockscreen_disabled", "true", 0);
            Slog.i(TAG, "Migrated lockscreen disabled flag");
        }
        List<UserInfo> users3 = this.mUserManager.getUsers();
        user = 0;
        while (true) {
            i2 = user;
            if (i2 >= users3.size()) {
                break;
            }
            String str;
            UserInfo userInfo = (UserInfo) users3.get(i2);
            if (userInfo.isManagedProfile() && this.mStorage.hasChildProfileLock(userInfo.id)) {
                long quality = getLong("lockscreen.password_type", 0, userInfo.id);
                if (quality == 0) {
                    Slog.i(TAG, "Migrated tied profile lock type");
                    setLong("lockscreen.password_type", 327680, userInfo.id);
                } else if (quality != 327680) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid tied profile lock type: ");
                    stringBuilder.append(quality);
                    Slog.e(str, stringBuilder.toString());
                }
            }
            try {
                str = new StringBuilder();
                str.append("profile_key_name_encrypt_");
                str.append(userInfo.id);
                str = str.toString();
                java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                if (keyStore.containsAlias(str)) {
                    keyStore.deleteEntry(str);
                }
            } catch (Exception e2) {
            }
            user = i2 + 1;
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch") && getString("migrated_wear_lockscreen_disabled", null, 0) == null) {
            int userCount = users3.size();
            for (i2 = 0; i2 < userCount; i2++) {
                setBoolean("lockscreen.disabled", false, ((UserInfo) users3.get(i2)).id);
            }
            setString("migrated_wear_lockscreen_disabled", "true", 0);
            Slog.i(TAG, "Migrated lockscreen_disabled for Wear devices");
        }
    }

    private void migrateOldDataAfterSystemReady() {
        try {
            if (LockPatternUtils.frpCredentialEnabled(this.mContext) && !getBoolean("migrated_frp", false, 0)) {
                migrateFrpCredential();
                setBoolean("migrated_frp", true, 0);
                Slog.i(TAG, "Migrated migrated_frp.");
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to migrateOldDataAfterSystemReady", e);
        }
    }

    private void migrateFrpCredential() throws RemoteException {
        if (this.mStorage.readPersistentDataBlock() == PersistentData.NONE) {
            for (UserInfo userInfo : this.mUserManager.getUsers()) {
                if (LockPatternUtils.userOwnsFrpCredential(this.mContext, userInfo) && isUserSecure(userInfo.id)) {
                    synchronized (this.mSpManager) {
                        if (isSyntheticPasswordBasedCredentialLocked(userInfo.id)) {
                            this.mSpManager.migrateFrpPasswordLocked(getSyntheticPasswordHandleLocked(userInfo.id), userInfo, redactActualQualityToMostLenientEquivalentQuality((int) getLong("lockscreen.password_type", 0, userInfo.id)));
                        }
                    }
                    return;
                }
            }
        }
    }

    private int redactActualQualityToMostLenientEquivalentQuality(int quality) {
        if (quality == 131072 || quality == 196608) {
            return 131072;
        }
        return (quality == 262144 || quality == 327680 || quality == 393216) ? 262144 : quality;
    }

    protected final void checkWritePermission(int userId) {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsWrite");
    }

    private final void checkPasswordReadPermission(int userId) {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsRead");
    }

    private final void checkPasswordHavePermission(int userId) {
        if (this.mContext.checkCallingOrSelfPermission(PERMISSION) != 0) {
            EventLog.writeEvent(1397638484, new Object[]{"28251513", Integer.valueOf(getCallingUid()), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS});
        }
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsHave");
    }

    private final void checkReadPermission(String requestedKey, int userId) {
        StringBuilder stringBuilder;
        int callingUid = Binder.getCallingUid();
        int i = 0;
        int i2 = 0;
        while (i2 < READ_CONTACTS_PROTECTED_SETTINGS.length) {
            if (!READ_CONTACTS_PROTECTED_SETTINGS[i2].equals(requestedKey) || this.mContext.checkCallingOrSelfPermission("android.permission.READ_CONTACTS") == 0) {
                i2++;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("uid=");
                stringBuilder.append(callingUid);
                stringBuilder.append(" needs permission ");
                stringBuilder.append("android.permission.READ_CONTACTS");
                stringBuilder.append(" to read ");
                stringBuilder.append(requestedKey);
                stringBuilder.append(" for user ");
                stringBuilder.append(userId);
                throw new SecurityException(stringBuilder.toString());
            }
        }
        while (i < READ_PASSWORD_PROTECTED_SETTINGS.length) {
            if (!READ_PASSWORD_PROTECTED_SETTINGS[i].equals(requestedKey) || this.mContext.checkCallingOrSelfPermission(PERMISSION) == 0) {
                i++;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("uid=");
                stringBuilder.append(callingUid);
                stringBuilder.append(" needs permission ");
                stringBuilder.append(PERMISSION);
                stringBuilder.append(" to read ");
                stringBuilder.append(requestedKey);
                stringBuilder.append(" for user ");
                stringBuilder.append(userId);
                throw new SecurityException(stringBuilder.toString());
            }
        }
    }

    public boolean getSeparateProfileChallengeEnabled(int userId) {
        boolean z;
        checkReadPermission(SEPARATE_PROFILE_CHALLENGE_KEY, userId);
        synchronized (this.mSeparateChallengeLock) {
            z = getBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, false, userId);
        }
        return z;
    }

    public void setSeparateProfileChallengeEnabled(int userId, boolean enabled, String managedUserPassword) {
        checkWritePermission(userId);
        synchronized (this.mSeparateChallengeLock) {
            setSeparateProfileChallengeEnabledLocked(userId, enabled, managedUserPassword);
        }
        notifySeparateProfileChallengeChanged(userId);
    }

    @GuardedBy("mSeparateChallengeLock")
    private void setSeparateProfileChallengeEnabledLocked(int userId, boolean enabled, String managedUserPassword) {
        setBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, enabled, userId);
        if (enabled) {
            this.mStorage.removeChildProfileLock(userId);
            removeKeystoreProfileKey(userId);
            return;
        }
        tieManagedProfileLockIfNecessary(userId, managedUserPassword);
    }

    private void notifySeparateProfileChallengeChanged(int userId) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (dpmi != null) {
            dpmi.reportSeparateProfileChallengeChanged(userId);
        }
    }

    public void setBoolean(String key, boolean value, int userId) {
        checkWritePermission(userId);
        setStringUnchecked(key, userId, value ? "1" : "0");
    }

    public void setLong(String key, long value, int userId) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.LOCKSETTINGS_SETLONG);
        }
        checkWritePermission(userId);
        setStringUnchecked(key, userId, Long.toString(value));
    }

    public void setString(String key, String value, int userId) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.LOCKSETTINGS_SETSTRING);
        }
        checkWritePermission(userId);
        setStringUnchecked(key, userId, value);
    }

    protected void setStringUnchecked(String key, int userId, String value) {
        Preconditions.checkArgument(userId != -9999, "cannot store lock settings for FRP user");
        this.mStorage.writeKeyValue(key, value, userId);
        if (ArrayUtils.contains(SETTINGS_TO_BACKUP, key)) {
            BackupManager.dataChanged(BackupManagerService.SETTINGS_PACKAGE);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue, int userId) {
        checkReadPermission(key, userId);
        String value = getStringUnchecked(key, null, userId);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        return value.equals("1") || value.equals("true");
    }

    public long getLong(String key, long defaultValue, int userId) {
        checkReadPermission(key, userId);
        String value = getStringUnchecked(key, null, userId);
        return TextUtils.isEmpty(value) ? defaultValue : Long.parseLong(value);
    }

    public String getString(String key, String defaultValue, int userId) {
        checkReadPermission(key, userId);
        return getStringUnchecked(key, defaultValue, userId);
    }

    public String getStringUnchecked(String key, String defaultValue, int userId) {
        if ("lock_pattern_autolock".equals(key)) {
            long ident = Binder.clearCallingIdentity();
            try {
                String str = this.mLockPatternUtils.isLockPatternEnabled(userId) ? "1" : "0";
                Binder.restoreCallingIdentity(ident);
                return str;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else if (userId == -9999) {
            return getFrpStringUnchecked(key);
        } else {
            if ("legacy_lock_pattern_enabled".equals(key)) {
                key = "lock_pattern_autolock";
            }
            return this.mStorage.readKeyValue(key, defaultValue, userId);
        }
    }

    private String getFrpStringUnchecked(String key) {
        if ("lockscreen.password_type".equals(key)) {
            return String.valueOf(readFrpPasswordQuality());
        }
        return null;
    }

    private int readFrpPasswordQuality() {
        return this.mStorage.readPersistentDataBlock().qualityForUi;
    }

    /* JADX WARNING: Missing block: B:10:0x001d, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean havePassword(int userId) throws RemoteException {
        checkPasswordHavePermission(userId);
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                boolean z = this.mSpManager.getCredentialType(getSyntheticPasswordHandleLocked(userId), userId) == 2;
            } else {
                return this.mStorage.hasPassword(userId);
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x001c, code:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean havePattern(int userId) throws RemoteException {
        checkPasswordHavePermission(userId);
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                boolean z = true;
                if (this.mSpManager.getCredentialType(getSyntheticPasswordHandleLocked(userId), userId) != 1) {
                    z = false;
                }
            } else {
                return this.mStorage.hasPattern(userId);
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x001a, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isUserSecure(int userId) {
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                boolean z = this.mSpManager.getCredentialType(getSyntheticPasswordHandleLocked(userId), userId) != -1;
            } else {
                return this.mStorage.hasCredential(userId);
            }
        }
    }

    protected void setKeystorePassword(String password, int userHandle) {
        KeyStore.getInstance().onUserPasswordChanged(userHandle, password);
    }

    private void unlockKeystore(String password, int userHandle) {
        KeyStore.getInstance().unlock(userHandle, password);
    }

    @VisibleForTesting
    protected String getDecryptedPasswordForTiedProfile(int userId) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, CertificateException, IOException {
        byte[] storedData = this.mStorage.readChildProfileLock(userId);
        if (storedData != null) {
            byte[] iv = Arrays.copyOfRange(storedData, null, 12);
            byte[] encryptedPassword = Arrays.copyOfRange(storedData, 12, storedData.length);
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("profile_key_name_decrypt_");
            stringBuilder.append(userId);
            SecretKey decryptionKey = (SecretKey) keyStore.getKey(stringBuilder.toString(), null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(2, decryptionKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encryptedPassword), StandardCharsets.UTF_8);
        }
        throw new FileNotFoundException("Child profile lock file not found");
    }

    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x000f A:{Splitter: B:0:0x0000, ExcHandler: java.security.UnrecoverableKeyException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:2:0x000f, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:4:0x0012, code:
            if ((r0 instanceof java.io.FileNotFoundException) != false) goto L_0x0014;
     */
    /* JADX WARNING: Missing block: B:5:0x0014, code:
            android.util.Slog.i(TAG, "Child profile key not found");
     */
    /* JADX WARNING: Missing block: B:6:0x001c, code:
            if (r10 == false) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:9:0x0022, code:
            android.util.Slog.i(TAG, "Parent keystore seems locked, ignoring");
     */
    /* JADX WARNING: Missing block: B:10:0x002a, code:
            android.util.Slog.e(TAG, "Failed to decrypt child profile key", r0);
     */
    /* JADX WARNING: Missing block: B:11:?, code:
            return;
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            return;
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void unlockChildProfile(int profileHandle, boolean ignoreUserNotAuthenticated) throws RemoteException {
        try {
            doVerifyCredential(getDecryptedPasswordForTiedProfile(profileHandle), 2, false, 0, profileHandle, null);
        } catch (Exception e) {
        }
    }

    private void unlockUser(int userId, byte[] token, byte[] secret) {
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            this.mActivityManager.unlockUser(userId, token, secret, new IProgressListener.Stub() {
                public void onStarted(int id, Bundle extras) throws RemoteException {
                    Log.d(LockSettingsService.TAG, "unlockUser started");
                }

                public void onProgress(int id, int progress, Bundle extras) throws RemoteException {
                    String str = LockSettingsService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unlockUser progress ");
                    stringBuilder.append(progress);
                    Log.d(str, stringBuilder.toString());
                }

                public void onFinished(int id, Bundle extras) throws RemoteException {
                    Log.d(LockSettingsService.TAG, "unlockUser finished");
                    latch.countDown();
                }
            });
            try {
                latch.await(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                UserInfo ui = this.mUserManager.getUserInfo(userId);
                if (ui != null && ui.isClonedProfile() && this.mStorage.hasChildProfileLock(userId)) {
                    synchronized (this.mSeparateChallengeLock) {
                        clearUserKeyProtection(userId);
                        getGateKeeperService().clearSecureUserId(userId);
                        this.mStorage.writeCredentialHash(CredentialHash.createEmptyHash(), userId);
                        setKeystorePassword(null, userId);
                        fixateNewestUserKeyAuth(userId);
                        this.mStorage.removeChildProfileLock(userId);
                        removeKeystoreProfileKey(userId);
                        Slog.i(TAG, "finish unlock clone user after ota and remove profile key");
                    }
                }
                if (ui != null && !ui.isManagedProfile() && !ui.isClonedProfile()) {
                    for (UserInfo pi : this.mUserManager.getProfiles(userId)) {
                        if (tiedManagedProfileReadyToUnlock(pi) || (pi.isClonedProfile() && this.mStorage.hasChildProfileLock(pi.id) && this.mUserManager.isUserRunning(pi.id))) {
                            unlockChildProfile(pi.id, false);
                        }
                    }
                }
            } catch (RemoteException e2) {
                Log.d(TAG, "Failed to unlock child profile", e2);
            }
        } catch (RemoteException e22) {
            throw e22.rethrowAsRuntimeException();
        }
    }

    private boolean tiedManagedProfileReadyToUnlock(UserInfo userInfo) {
        return userInfo.isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userInfo.id) && this.mStorage.hasChildProfileLock(userInfo.id) && this.mUserManager.isUserRunning(userInfo.id);
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0044 A:{Splitter: B:10:0x0038, ExcHandler: java.security.KeyStoreException (r6_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:12:0x0044, code:
            r6 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:0x0045, code:
            r7 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("getDecryptedPasswordsForAllTiedProfiles failed for user ");
            r8.append(r5);
            android.util.Slog.e(r7, r8.toString(), r6);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Map<Integer, String> getDecryptedPasswordsForAllTiedProfiles(int userId) {
        if (this.mUserManager.getUserInfo(userId).isManagedProfile()) {
            return null;
        }
        Map<Integer, String> result = new ArrayMap();
        List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
        int size = profiles.size();
        for (int i = 0; i < size; i++) {
            UserInfo profile = (UserInfo) profiles.get(i);
            if (profile.isManagedProfile()) {
                int managedUserId = profile.id;
                if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(managedUserId)) {
                    try {
                        result.put(Integer.valueOf(managedUserId), getDecryptedPasswordForTiedProfile(managedUserId));
                    } catch (Exception e) {
                    }
                }
            }
        }
        return result;
    }

    private void synchronizeUnifiedWorkChallengeForProfiles(int userId, Map<Integer, String> profilePasswordMap) throws RemoteException {
        if (this.mUserManager.getUserInfo(userId) == null || !this.mUserManager.getUserInfo(userId).isManagedProfile()) {
            boolean isSecure = isUserSecure(userId);
            List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
            int size = profiles.size();
            for (int i = 0; i < size; i++) {
                UserInfo profile = (UserInfo) profiles.get(i);
                if (profile.isManagedProfile()) {
                    int managedUserId = profile.id;
                    if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(managedUserId)) {
                        if (isSecure) {
                            tieManagedProfileLockIfNecessary(managedUserId, null);
                        } else {
                            if (profilePasswordMap == null || !profilePasswordMap.containsKey(Integer.valueOf(managedUserId))) {
                                Slog.wtf(TAG, "clear tied profile challenges, but no password supplied.");
                                setLockCredentialInternal(null, -1, null, 0, managedUserId);
                            } else {
                                setLockCredentialInternal(null, -1, (String) profilePasswordMap.get(Integer.valueOf(managedUserId)), 0, managedUserId);
                            }
                            this.mStorage.removeChildProfileLock(managedUserId);
                            removeKeystoreProfileKey(managedUserId);
                        }
                    }
                }
            }
        }
    }

    private boolean isManagedProfileWithUnifiedLock(int userId) {
        return (this.mUserManager.getUserInfo(userId) == null || !this.mUserManager.getUserInfo(userId).isManagedProfile() || this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId)) ? false : true;
    }

    private boolean isManagedProfileWithSeparatedLock(int userId) {
        return this.mUserManager.getUserInfo(userId) != null && this.mUserManager.getUserInfo(userId).isManagedProfile() && this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId);
    }

    public void setLockCredential(String credential, int type, String savedCredential, int requestedQuality, int userId) throws RemoteException {
        checkWritePermission(userId);
        synchronized (this.mSeparateChallengeLock) {
            int oldCredentialType = getOldCredentialType(userId);
            setLockCredentialInternal(credential, type, savedCredential, requestedQuality, userId);
            setSeparateProfileChallengeEnabledLocked(userId, true, null);
            notifyPasswordChanged(userId);
            notifyPasswordStatusChanged(userId, getPasswordStatus(type, oldCredentialType));
            notifyModifyPwdForPrivSpacePwdProtect(credential, savedCredential, userId);
        }
        notifySeparateProfileChallengeChanged(userId);
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0083 A:{Splitter: B:27:0x007c, ExcHandler: java.security.UnrecoverableKeyException (r0_14 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:18:0x0034, code:
            if (r10 != -1) goto L_0x006c;
     */
    /* JADX WARNING: Missing block: B:19:0x0036, code:
            if (r12 == null) goto L_0x003f;
     */
    /* JADX WARNING: Missing block: B:20:0x0038, code:
            android.util.Slog.wtf(TAG, "CredentialType is none, but credential is non-null.");
     */
    /* JADX WARNING: Missing block: B:21:0x003f, code:
            clearUserKeyProtection(r11);
            getGateKeeperService().clearSecureUserId(r11);
            r9.mStorage.writeCredentialHash(com.android.server.locksettings.LockSettingsStorage.CredentialHash.createEmptyHash(), r11);
            setKeystorePassword(null, r11);
            fixateNewestUserKeyAuth(r11);
            synchronizeUnifiedWorkChallengeForProfiles(r11, null);
            notifyActivePasswordMetricsAvailable(null, r11);
            r9.mRecoverableKeyStoreManager.lockScreenSecretChanged(r10, r12, r11);
            android.util.Slog.w(TAG, "setLockPattern to null success");
     */
    /* JADX WARNING: Missing block: B:22:0x006b, code:
            return;
     */
    /* JADX WARNING: Missing block: B:23:0x006c, code:
            if (r12 == null) goto L_0x0147;
     */
    /* JADX WARNING: Missing block: B:24:0x006e, code:
            r14 = r9.mStorage.readCredentialHash(r11);
     */
    /* JADX WARNING: Missing block: B:25:0x0078, code:
            if (isManagedProfileWithUnifiedLock(r11) == false) goto L_0x0097;
     */
    /* JADX WARNING: Missing block: B:26:0x007a, code:
            if (r7 != null) goto L_0x00a6;
     */
    /* JADX WARNING: Missing block: B:28:?, code:
            r0 = getDecryptedPasswordForTiedProfile(r11);
     */
    /* JADX WARNING: Missing block: B:29:0x0081, code:
            r15 = r0;
     */
    /* JADX WARNING: Missing block: B:30:0x0083, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:31:0x0084, code:
            r1 = r0;
            android.util.Slog.e(TAG, "Failed to decrypt child profile key", r0);
     */
    /* JADX WARNING: Missing block: B:32:0x008d, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:33:0x008e, code:
            r1 = r0;
            android.util.Slog.i(TAG, "Child profile key not found");
     */
    /* JADX WARNING: Missing block: B:35:0x0099, code:
            if (r14.hash != null) goto L_0x00a6;
     */
    /* JADX WARNING: Missing block: B:36:0x009b, code:
            if (r7 == null) goto L_0x00a4;
     */
    /* JADX WARNING: Missing block: B:37:0x009d, code:
            android.util.Slog.w(TAG, "Saved credential provided, but none stored");
     */
    /* JADX WARNING: Missing block: B:38:0x00a4, code:
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:49:0x00c9, code:
            r0 = enrollCredential(r14.hash, r15, r12, r11);
     */
    /* JADX WARNING: Missing block: B:50:0x00cf, code:
            if (r0 == null) goto L_0x0119;
     */
    /* JADX WARNING: Missing block: B:51:0x00d1, code:
            r8 = com.android.server.locksettings.LockSettingsStorage.CredentialHash.create(r0, r10);
            r9.mStorage.writeCredentialHash(r8, r11);
            r7 = getGateKeeperService().verifyChallenge(r11, 0, r8.hash, r12.getBytes());
            setUserKeyProtection(r11, r12, convertResponse(r7));
            fixateNewestUserKeyAuth(r11);
            r17 = r7;
            r18 = r8;
            doVerifyCredential(r12, r10, true, 0, r11, null);
            synchronizeUnifiedWorkChallengeForProfiles(r11, null);
            r9.mRecoverableKeyStoreManager.lockScreenSecretChanged(r10, r12, r11);
            android.util.Slog.w(TAG, "set new LockPassword success");
     */
    /* JADX WARNING: Missing block: B:52:0x0118, code:
            return;
     */
    /* JADX WARNING: Missing block: B:53:0x0119, code:
            android.util.Slog.e(TAG, "Failed to enroll password");
            notifyBigDataForPwdProtectFail(r11);
            r2 = new java.lang.StringBuilder();
            r2.append("Failed to enroll ");
     */
    /* JADX WARNING: Missing block: B:54:0x0130, code:
            if (r10 != 2) goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:55:0x0132, code:
            r3 = "password";
     */
    /* JADX WARNING: Missing block: B:56:0x0136, code:
            r3 = "pattern";
     */
    /* JADX WARNING: Missing block: B:57:0x0139, code:
            r2.append(r3);
     */
    /* JADX WARNING: Missing block: B:58:0x0143, code:
            throw new android.os.RemoteException(r2.toString());
     */
    /* JADX WARNING: Missing block: B:64:0x014e, code:
            throw new android.os.RemoteException("Null credential with mismatched credential type");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void setLockCredentialInternal(String credential, int credentialType, String savedCredential, int requestedQuality, int userId) throws RemoteException {
        int i = credentialType;
        int i2 = userId;
        String savedCredential2 = TextUtils.isEmpty(savedCredential) ? null : savedCredential;
        String credential2 = TextUtils.isEmpty(credential) ? null : credential;
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(i2)) {
                spBasedSetLockCredentialInternalLocked(credential2, i, savedCredential2, requestedQuality, i2);
                return;
            }
        }
        String savedCredential3 = savedCredential2;
        synchronized (this.mSpManager) {
            if (shouldMigrateToSyntheticPasswordLocked(i2)) {
                initializeSyntheticPasswordLocked(currentHandle.hash, savedCredential3, currentHandle.type, requestedQuality, i2);
                spBasedSetLockCredentialInternalLocked(credential2, i, savedCredential3, requestedQuality, i2);
            }
        }
    }

    private VerifyCredentialResponse convertResponse(GateKeeperResponse gateKeeperResponse) {
        return VerifyCredentialResponse.fromGateKeeperResponse(gateKeeperResponse);
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x0136 A:{Splitter: B:1:0x0006, ExcHandler: java.security.cert.CertificateException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0136 A:{Splitter: B:1:0x0006, ExcHandler: java.security.cert.CertificateException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0136 A:{Splitter: B:1:0x0006, ExcHandler: java.security.cert.CertificateException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0136 A:{Splitter: B:1:0x0006, ExcHandler: java.security.cert.CertificateException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0136 A:{Splitter: B:1:0x0006, ExcHandler: java.security.cert.CertificateException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0136 A:{Splitter: B:1:0x0006, ExcHandler: java.security.cert.CertificateException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0136 A:{Splitter: B:1:0x0006, ExcHandler: java.security.cert.CertificateException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0136 A:{Splitter: B:1:0x0006, ExcHandler: java.security.cert.CertificateException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:25:0x0136, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:27:0x013e, code:
            throw new java.lang.RuntimeException("Failed to encrypt key", r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    protected void tieProfileLockToParent(int userId, String password) {
        byte[] randomLockSeed = password.getBytes(StandardCharsets.UTF_8);
        java.security.KeyStore keyStore;
        StringBuilder stringBuilder;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            stringBuilder = new StringBuilder();
            stringBuilder.append("profile_key_name_encrypt_");
            stringBuilder.append(userId);
            keyStore.setEntry(stringBuilder.toString(), new SecretKeyEntry(secretKey), new KeyProtection.Builder(1).setBlockModes(new String[]{"GCM"}).setEncryptionPaddings(new String[]{"NoPadding"}).build());
            stringBuilder = new StringBuilder();
            stringBuilder.append("profile_key_name_decrypt_");
            stringBuilder.append(userId);
            keyStore.setEntry(stringBuilder.toString(), new SecretKeyEntry(secretKey), new KeyProtection.Builder(2).setBlockModes(new String[]{"GCM"}).setEncryptionPaddings(new String[]{"NoPadding"}).setUserAuthenticationRequired(true).setUserAuthenticationValidityDurationSeconds(30).setCriticalToDeviceEncryption(true).build());
            stringBuilder = new StringBuilder();
            stringBuilder.append("profile_key_name_encrypt_");
            stringBuilder.append(userId);
            SecretKey keyStoreEncryptionKey = (SecretKey) keyStore.getKey(stringBuilder.toString(), null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, keyStoreEncryptionKey);
            byte[] encryptionResult = cipher.doFinal(randomLockSeed);
            byte[] iv = cipher.getIV();
            stringBuilder = new StringBuilder();
            stringBuilder.append("profile_key_name_encrypt_");
            stringBuilder.append(userId);
            keyStore.deleteEntry(stringBuilder.toString());
            keyGenerator = new ByteArrayOutputStream();
            try {
                if (iv.length == 12) {
                    keyGenerator.write(iv);
                    keyGenerator.write(encryptionResult);
                    this.mStorage.writeChildProfileLock(userId, keyGenerator.toByteArray());
                    return;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid iv length: ");
                stringBuilder2.append(iv.length);
                throw new RuntimeException(stringBuilder2.toString());
            } catch (SecretKey secretKey2) {
                throw new RuntimeException("Failed to concatenate byte arrays", secretKey2);
            }
        } catch (Exception e) {
        } catch (Throwable th) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("profile_key_name_encrypt_");
            stringBuilder.append(userId);
            keyStore.deleteEntry(stringBuilder.toString());
        }
    }

    private byte[] enrollCredential(byte[] enrolledHandle, String enrolledCredential, String toEnroll, int userId) throws RemoteException {
        checkWritePermission(userId);
        GateKeeperResponse response = getGateKeeperService().enroll(userId, enrolledHandle, enrolledCredential == null ? null : enrolledCredential.getBytes(), toEnroll == null ? null : toEnroll.getBytes());
        if (response == null) {
            Slog.w(TAG, "enrollCredential response null");
            return null;
        }
        byte[] hash = response.getPayload();
        if (hash != null) {
            setKeystorePassword(toEnroll, userId);
            Slog.w(TAG, "enrollCredential response success");
        } else {
            Slog.e(TAG, "Throttled while enrolling a password");
        }
        return hash;
    }

    private void setAuthlessUserKeyProtection(int userId, byte[] key) throws RemoteException {
        addUserKeyAuth(userId, null, key);
    }

    private void setUserKeyProtection(int userId, String credential, VerifyCredentialResponse vcr) throws RemoteException {
        if (vcr == null) {
            throw new RemoteException("Null response verifying a credential we just set");
        } else if (vcr.getResponseCode() == 0) {
            byte[] token = vcr.getPayload();
            if (token != null) {
                addUserKeyAuth(userId, token, secretFromCredential(credential));
                return;
            }
            throw new RemoteException("Empty payload verifying a credential we just set");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Non-OK response verifying a credential we just set: ");
            stringBuilder.append(vcr.getResponseCode());
            throw new RemoteException(stringBuilder.toString());
        }
    }

    private void clearUserKeyProtection(int userId) throws RemoteException {
        addUserKeyAuth(userId, null, null);
    }

    private static byte[] secretFromCredential(String credential) throws RemoteException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update(Arrays.copyOf("Android FBE credential hash".getBytes(StandardCharsets.UTF_8), 128));
            digest.update(credential.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException for SHA-512");
        }
    }

    private void addUserKeyAuth(int userId, byte[] token, byte[] secret) throws RemoteException {
        UserInfo userInfo = this.mUserManager.getUserInfo(userId);
        if (userId == 2147483646 && userInfo == null) {
            Log.w(TAG, "Parentcontrol doesn't have userinfo , do not addUserKeyAuth!");
            return;
        }
        IStorageManager storageManager = this.mInjector.getStorageManager();
        long callingId = Binder.clearCallingIdentity();
        try {
            storageManager.addUserKeyAuth(userId, userInfo.serialNumber, token, secret);
            ISDCardCryptedHelper helper = HwServiceFactory.getSDCardCryptedHelper();
            if (helper != null) {
                helper.addUserKeyAuth(userId, userInfo.serialNumber, token, secret);
            }
            Binder.restoreCallingIdentity(callingId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void fixateNewestUserKeyAuth(int userId) throws RemoteException {
        if (2147483646 != userId) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fixateNewestUserKeyAuth: user=");
            stringBuilder.append(userId);
            Slog.d(str, stringBuilder.toString());
            IStorageManager storageManager = this.mInjector.getStorageManager();
            long callingId = Binder.clearCallingIdentity();
            try {
                storageManager.fixateNewestUserKeyAuth(userId);
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x004a A:{Catch:{ UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a, UnrecoverableKeyException -> 0x004a }, Splitter: B:10:0x003f, ExcHandler: java.security.UnrecoverableKeyException (r0_11 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:12:0x004a, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:14:0x006d, code:
            android.util.Slog.e(TAG, "Failed to decrypt child profile key", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void resetKeyStore(int userId) throws RemoteException {
        int i = userId;
        checkWritePermission(userId);
        String managedUserDecryptedPassword = null;
        int managedUserId = -1;
        for (UserInfo pi : this.mUserManager.getProfiles(i)) {
            if (pi.isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(pi.id) && this.mStorage.hasChildProfileLock(pi.id)) {
                if (managedUserId == -1) {
                    try {
                        managedUserDecryptedPassword = getDecryptedPasswordForTiedProfile(pi.id);
                        managedUserId = pi.id;
                    } catch (Exception e) {
                    }
                } else {
                    Exception e2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("More than one managed profile, uid1:");
                    stringBuilder.append(managedUserId);
                    stringBuilder.append(", uid2:");
                    stringBuilder.append(pi.id);
                    Slog.e(e2, stringBuilder.toString());
                }
            }
        }
        try {
            for (int profileId : this.mUserManager.getProfileIdsWithDisabled(i)) {
                for (int uid : SYSTEM_CREDENTIAL_UIDS) {
                    this.mKeyStore.clearUid(UserHandle.getUid(profileId, uid));
                }
            }
        } finally {
            if (!(managedUserId == -1 || managedUserDecryptedPassword == null)) {
                tieProfileLockToParent(managedUserId, managedUserDecryptedPassword);
            }
        }
    }

    public VerifyCredentialResponse checkCredential(String credential, int type, int userId, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        checkPasswordReadPermission(userId);
        return doVerifyCredential(credential, type, false, 0, userId, progressCallback);
    }

    public VerifyCredentialResponse verifyCredential(String credential, int type, long challenge, int userId) throws RemoteException {
        checkPasswordReadPermission(userId);
        return doVerifyCredential(credential, type, true, challenge, userId, null);
    }

    private VerifyCredentialResponse doVerifyCredential(String credential, int credentialType, boolean hasChallenge, long challenge, int userId, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        int i = credentialType;
        int i2 = userId;
        String str;
        if (TextUtils.isEmpty(credential)) {
            str = credential;
            this.mLockPatternUtils.monitorCheckPassword(1002, null);
            throw new IllegalArgumentException("Credential can't be null or empty");
        }
        boolean shouldReEnrollBaseZero = false;
        if (i2 != -9999 || Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 0) {
            try {
                VerifyCredentialResponse response = spBasedDoVerifyCredential(credential, credentialType, hasChallenge, challenge, userId, progressCallback);
                if (response != null) {
                    if (response.getResponseCode() == 0) {
                        this.mRecoverableKeyStoreManager.lockScreenSecretAvailable(i, credential, i2);
                    } else {
                        str = credential;
                    }
                    return response;
                }
                str = credential;
                if (i2 == -9999) {
                    Slog.wtf(TAG, "Unexpected FRP credential type, should be SP based.");
                    return VerifyCredentialResponse.ERROR;
                }
                CredentialHash storedHash = this.mStorage.readCredentialHash(i2);
                if (storedHash == null || storedHash.hash == null || storedHash.hash.length == 0) {
                    Slog.w(TAG, "no Pattern saved VerifyPattern success");
                    return VerifyCredentialResponse.OK;
                } else if (storedHash.type != i) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("doVerifyCredential type mismatch with stored credential?? stored: ");
                    stringBuilder.append(storedHash.type);
                    stringBuilder.append(" passed in: ");
                    stringBuilder.append(i);
                    Slog.wtf(str2, stringBuilder.toString());
                    return VerifyCredentialResponse.ERROR;
                } else {
                    String credentialToVerify;
                    if (storedHash.type == 1 && storedHash.isBaseZeroPattern) {
                        shouldReEnrollBaseZero = true;
                    }
                    if (shouldReEnrollBaseZero) {
                        credentialToVerify = LockPatternUtils.patternStringToBaseZero(credential);
                    } else {
                        credentialToVerify = str;
                    }
                    VerifyCredentialResponse response2 = verifyCredential(i2, storedHash, credentialToVerify, hasChallenge, challenge, progressCallback);
                    if (response2.getResponseCode() == 0) {
                        this.mStrongAuth.reportSuccessfulStrongAuthUnlock(i2);
                        if (shouldReEnrollBaseZero) {
                            setLockCredentialInternal(str, storedHash.type, credentialToVerify, 65536, i2);
                        }
                    }
                    return response2;
                }
            } catch (RuntimeException re) {
                str = credential;
                RuntimeException runtimeException = re;
                if (this.mUserManager.getUserInfo(i2).isManagedProfile()) {
                    Throwable e = re.getCause();
                    if (e == null || !(e instanceof UnrecoverableKeyException)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("spBasedDoVerifyCredential failed due to ");
                        stringBuilder2.append(re.toString());
                        Slog.e(TAG, stringBuilder2.toString());
                    } else {
                        Slog.e(TAG, "spBasedDoVerifyCredential failed due to RuntimeException->UnrecoverableKeyException");
                        return VerifyCredentialResponse.ERROR;
                    }
                }
                throw re;
            }
        }
        Slog.e(TAG, "FRP credential can only be verified prior to provisioning.");
        return VerifyCredentialResponse.ERROR;
    }

    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0037 A:{Splitter: B:5:0x0027, ExcHandler: java.security.UnrecoverableKeyException (r0_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:8:0x0037, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:9:0x0038, code:
            android.util.Slog.e(TAG, "Failed to decrypt child profile key", r0);
     */
    /* JADX WARNING: Missing block: B:10:0x0046, code:
            throw new android.os.RemoteException("Unable to get tied profile token");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public VerifyCredentialResponse verifyTiedProfileChallenge(String credential, int type, long challenge, int userId) throws RemoteException {
        int i = userId;
        checkPasswordReadPermission(i);
        if (isManagedProfileWithUnifiedLock(i)) {
            VerifyCredentialResponse parentResponse = doVerifyCredential(credential, type, true, challenge, this.mUserManager.getProfileParent(i).id, null);
            if (parentResponse.getResponseCode() != 0) {
                return parentResponse;
            }
            try {
                return doVerifyCredential(getDecryptedPasswordForTiedProfile(i), 2, true, challenge, i, null);
            } catch (Exception e) {
            }
        } else {
            throw new RemoteException("User id must be managed profile with unified lock");
        }
    }

    private VerifyCredentialResponse verifyCredential(int userId, CredentialHash storedHash, String credential, boolean hasChallenge, long challenge, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        Throwable th;
        int i = userId;
        CredentialHash credentialHash = storedHash;
        String str = credential;
        if ((credentialHash == null || credentialHash.hash.length == 0) && TextUtils.isEmpty(credential)) {
            Slog.w(TAG, "no stored Password/Pattern, verifyCredential success");
            return VerifyCredentialResponse.OK;
        } else if (credentialHash == null || TextUtils.isEmpty(credential)) {
            Slog.w(TAG, "no entered Password/Pattern, verifyCredential ERROR");
            return VerifyCredentialResponse.ERROR;
        } else {
            String str2;
            StringBuilder stringBuilder;
            StrictMode.noteDiskRead();
            if (credentialHash.version == 0) {
                byte[] hash;
                if (credentialHash.type == 1) {
                    hash = LockPatternUtils.patternToHash(LockPatternUtils.stringToPattern(credential));
                } else {
                    hash = this.mLockPatternUtils.legacyPasswordToHash(str, i).getBytes(StandardCharsets.UTF_8);
                }
                if (!Arrays.equals(hash, credentialHash.hash)) {
                    return VerifyCredentialResponse.ERROR;
                }
                if (credentialHash.type == 1) {
                    unlockKeystore(LockPatternUtils.patternStringToBaseZero(credential), i);
                } else {
                    unlockKeystore(str, i);
                }
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unlocking user with fake token: ");
                stringBuilder.append(i);
                Slog.i(str2, stringBuilder.toString());
                byte[] fakeToken = String.valueOf(userId).getBytes();
                unlockUser(i, fakeToken, fakeToken);
                setLockCredentialInternal(str, credentialHash.type, null, credentialHash.type == 1 ? 65536 : 327680, i);
                if (!hasChallenge) {
                    notifyActivePasswordMetricsAvailable(str, i);
                    this.mRecoverableKeyStoreManager.lockScreenSecretAvailable(credentialHash.type, str, i);
                    return VerifyCredentialResponse.OK;
                }
            }
            boolean shouldReEnroll = false;
            try {
                if (getGateKeeperService() == null) {
                    this.mLockPatternUtils.monitorCheckPassword(1006, null);
                    return VerifyCredentialResponse.ERROR;
                }
                VerifyCredentialResponse response;
                GateKeeperResponse gateKeeperResponse = getGateKeeperService().verifyChallenge(i, challenge, credentialHash.hash, credential.getBytes());
                VerifyCredentialResponse response2 = convertResponse(gateKeeperResponse);
                boolean shouldReEnroll2 = gateKeeperResponse.getShouldReEnroll();
                if (response2.getResponseCode() == 0) {
                    int i2;
                    if (progressCallback != null) {
                        progressCallback.onCredentialVerified();
                    }
                    notifyActivePasswordMetricsAvailable(str, i);
                    unlockKeystore(str, i);
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unlocking user ");
                    stringBuilder.append(i);
                    stringBuilder.append(" with token length ");
                    stringBuilder.append(response2.getPayload().length);
                    Slog.i(str2, stringBuilder.toString());
                    unlockUser(i, response2.getPayload(), secretFromCredential(credential));
                    if (isManagedProfileWithSeparatedLock(userId)) {
                        ((TrustManager) this.mContext.getSystemService("trust")).setDeviceLockedForUser(i, false);
                    }
                    int reEnrollQuality = credentialHash.type == 1 ? 65536 : 327680;
                    if (shouldReEnroll2) {
                        setLockCredentialInternal(str, credentialHash.type, str, reEnrollQuality, i);
                        response = response2;
                        i2 = 1;
                    } else {
                        synchronized (this.mSpManager) {
                            try {
                                if (shouldMigrateToSyntheticPasswordLocked(userId)) {
                                    response = response2;
                                    i2 = 1;
                                    try {
                                        activateEscrowTokens(initializeSyntheticPasswordLocked(credentialHash.hash, str, credentialHash.type, reEnrollQuality, i), i);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                }
                                response = response2;
                                i2 = 1;
                            } catch (Throwable th3) {
                                th = th3;
                                response = response2;
                                throw th;
                            }
                        }
                    }
                    this.mRecoverableKeyStoreManager.lockScreenSecretAvailable(credentialHash.type, str, i);
                    if ((getStrongAuthForUser(userId) & i2) != 0) {
                        Slog.w(TAG, "clear BOOT_AUTH flag after verifyCredential");
                        requireStrongAuth(0, i);
                    }
                    Slog.w(TAG, "verifyCredential passed by GateKeeper");
                } else {
                    response = response2;
                    if (response.getResponseCode() == 1 && response.getTimeout() > 0) {
                        requireStrongAuth(8, i);
                    }
                }
                return response;
            } catch (RemoteException re) {
                this.mLockPatternUtils.monitorCheckPassword(1004, re);
                return VerifyCredentialResponse.ERROR;
            }
        }
    }

    private void notifyActivePasswordMetricsAvailable(String password, int userId) {
        PasswordMetrics metrics;
        if (password == null) {
            metrics = new PasswordMetrics();
        } else {
            metrics = PasswordMetrics.computeForPassword(password);
            metrics.quality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(userId);
        }
        this.mHandler.post(new -$$Lambda$LockSettingsService$Hh44Kcp05cKI6Hc6dJfQupn4QY8(this, metrics, userId));
    }

    private void notifyPasswordChanged(int userId) {
        this.mHandler.post(new -$$Lambda$LockSettingsService$cIsW_BZK9p1jhG1yw78i-3W9E4Y(this, userId));
    }

    public boolean checkVoldPassword(int userId) throws RemoteException {
        if (!this.mFirstCallToVold) {
            return false;
        }
        this.mFirstCallToVold = false;
        checkPasswordReadPermission(userId);
        IStorageManager service = this.mInjector.getStorageManager();
        long identity = Binder.clearCallingIdentity();
        try {
            String password = service.getPassword();
            service.clearPassword();
            if (password == null) {
                return false;
            }
            try {
                if (this.mLockPatternUtils.isLockPatternEnabled(userId) && checkCredential(password, 1, userId, null).getResponseCode() == 0) {
                    return true;
                }
            } catch (Exception e) {
            }
            try {
                if (this.mLockPatternUtils.isLockPasswordEnabled(userId) && checkCredential(password, 2, userId, null).getResponseCode() == 0) {
                    return true;
                }
                return false;
            } catch (Exception e2) {
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void removeUser(int userId, boolean unknownUser) {
        this.mSpManager.removeUser(userId);
        this.mStorage.removeUser(userId);
        this.mStrongAuth.removeUser(userId);
        tryRemoveUserFromSpCacheLater(userId);
        KeyStore.getInstance().onUserRemoved(userId);
        try {
            IGateKeeperService gk = getGateKeeperService();
            if (gk != null) {
                gk.clearSecureUserId(userId);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "unable to clear GK secure user id");
        }
        if (unknownUser || this.mUserManager.getUserInfo(userId).isManagedProfile()) {
            removeKeystoreProfileKey(userId);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:2:0x0035 A:{Splitter: B:0:0x0000, ExcHandler: java.security.KeyStoreException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x0035 A:{Splitter: B:0:0x0000, ExcHandler: java.security.KeyStoreException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:2:0x0035 A:{Splitter: B:0:0x0000, ExcHandler: java.security.KeyStoreException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:2:0x0035, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:3:0x0036, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Unable to remove keystore profile key for user:");
            r2.append(r5);
            android.util.Slog.e(r1, r2.toString(), r0);
     */
    /* JADX WARNING: Missing block: B:4:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void removeKeystoreProfileKey(int targetUserId) {
        try {
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("profile_key_name_encrypt_");
            stringBuilder.append(targetUserId);
            keyStore.deleteEntry(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("profile_key_name_decrypt_");
            stringBuilder.append(targetUserId);
            keyStore.deleteEntry(stringBuilder.toString());
        } catch (Exception e) {
        }
    }

    public void registerStrongAuthTracker(IStrongAuthTracker tracker) {
        if (tracker == null) {
            Slog.e(TAG, "IStrongAuthTracker can not be null in methdo registerStrongAuthTracker!");
            return;
        }
        checkPasswordReadPermission(-1);
        this.mStrongAuth.registerStrongAuthTracker(tracker);
    }

    public void unregisterStrongAuthTracker(IStrongAuthTracker tracker) {
        if (tracker == null) {
            Slog.e(TAG, "IStrongAuthTracker can not be null in methdo unregisterStrongAuthTracker!");
            return;
        }
        checkPasswordReadPermission(-1);
        this.mStrongAuth.unregisterStrongAuthTracker(tracker);
    }

    public void requireStrongAuth(int strongAuthReason, int userId) {
        checkWritePermission(userId);
        if ((strongAuthReason & 1) != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("requireStrongAuth for AFTER_BOOT UID: ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(" PID: ");
            stringBuilder.append(Binder.getCallingPid());
            Slog.e(str, stringBuilder.toString());
        }
        this.mStrongAuth.requireStrongAuth(strongAuthReason, userId);
    }

    public void userPresent(int userId) {
        checkWritePermission(userId);
        this.mStrongAuth.reportUnlock(userId);
    }

    public int getStrongAuthForUser(int userId) {
        checkPasswordReadPermission(userId);
        return this.mStrongAuthTracker.getStrongAuthForUser(userId);
    }

    private boolean isCallerShell() {
        int callingUid = Binder.getCallingUid();
        return callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || callingUid == 0;
    }

    private void enforceShell() {
        if (!isCallerShell()) {
            throw new SecurityException("Caller must be shell");
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        enforceShell();
        long origId = Binder.clearCallingIdentity();
        try {
            new LockSettingsShellCommand(this.mContext, new LockPatternUtils(this.mContext)).exec(this, in, out, err, args, callback, resultReceiver);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void initRecoveryServiceWithSigFile(String rootCertificateAlias, byte[] recoveryServiceCertFile, byte[] recoveryServiceSigFile) throws RemoteException {
        this.mRecoverableKeyStoreManager.initRecoveryServiceWithSigFile(rootCertificateAlias, recoveryServiceCertFile, recoveryServiceSigFile);
    }

    public KeyChainSnapshot getKeyChainSnapshot() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getKeyChainSnapshot();
    }

    public void setSnapshotCreatedPendingIntent(PendingIntent intent) throws RemoteException {
        this.mRecoverableKeyStoreManager.setSnapshotCreatedPendingIntent(intent);
    }

    public void setServerParams(byte[] serverParams) throws RemoteException {
        this.mRecoverableKeyStoreManager.setServerParams(serverParams);
    }

    public void setRecoveryStatus(String alias, int status) throws RemoteException {
        this.mRecoverableKeyStoreManager.setRecoveryStatus(alias, status);
    }

    public Map getRecoveryStatus() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getRecoveryStatus();
    }

    public void setRecoverySecretTypes(int[] secretTypes) throws RemoteException {
        this.mRecoverableKeyStoreManager.setRecoverySecretTypes(secretTypes);
    }

    public int[] getRecoverySecretTypes() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getRecoverySecretTypes();
    }

    public byte[] startRecoverySessionWithCertPath(String sessionId, String rootCertificateAlias, RecoveryCertPath verifierCertPath, byte[] vaultParams, byte[] vaultChallenge, List<KeyChainProtectionParams> secrets) throws RemoteException {
        return this.mRecoverableKeyStoreManager.startRecoverySessionWithCertPath(sessionId, rootCertificateAlias, verifierCertPath, vaultParams, vaultChallenge, secrets);
    }

    public Map<String, String> recoverKeyChainSnapshot(String sessionId, byte[] recoveryKeyBlob, List<WrappedApplicationKey> applicationKeys) throws RemoteException {
        return this.mRecoverableKeyStoreManager.recoverKeyChainSnapshot(sessionId, recoveryKeyBlob, applicationKeys);
    }

    public void closeSession(String sessionId) throws RemoteException {
        this.mRecoverableKeyStoreManager.closeSession(sessionId);
    }

    public void removeKey(String alias) throws RemoteException {
        this.mRecoverableKeyStoreManager.removeKey(alias);
    }

    public String generateKey(String alias) throws RemoteException {
        return this.mRecoverableKeyStoreManager.generateKey(alias);
    }

    public String importKey(String alias, byte[] keyBytes) throws RemoteException {
        return this.mRecoverableKeyStoreManager.importKey(alias, keyBytes);
    }

    public String getKey(String alias) throws RemoteException {
        return this.mRecoverableKeyStoreManager.getKey(alias);
    }

    protected synchronized IGateKeeperService getGateKeeperService() throws RemoteException {
        if (this.mGateKeeperService != null) {
            return this.mGateKeeperService;
        }
        IBinder service = ServiceManager.getService("android.service.gatekeeper.IGateKeeperService");
        if (service != null) {
            service.linkToDeath(new GateKeeperDiedRecipient(this, null), 0);
            this.mGateKeeperService = IGateKeeperService.Stub.asInterface(service);
            return this.mGateKeeperService;
        }
        Slog.e(TAG, "Unable to acquire GateKeeperService");
        return null;
    }

    private void onAuthTokenKnownForUser(int userId, AuthenticationToken auth) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Caching SP for user ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        synchronized (this.mSpManager) {
            this.mSpCache.put(userId, auth);
        }
        tryRemoveUserFromSpCacheLater(userId);
        if (this.mAuthSecretService != null && this.mUserManager.getUserInfo(userId).isPrimary()) {
            try {
                byte[] rawSecret = auth.deriveVendorAuthSecret();
                ArrayList<Byte> secret = new ArrayList(rawSecret.length);
                for (byte valueOf : rawSecret) {
                    secret.add(Byte.valueOf(valueOf));
                }
                this.mAuthSecretService.primaryUserCredential(secret);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to pass primary user secret to AuthSecret HAL", e);
            }
        }
    }

    private void tryRemoveUserFromSpCacheLater(int userId) {
        this.mHandler.post(new -$$Lambda$LockSettingsService$lWTrcqR9gZxL-pxwBbtvTGqAifU(this, userId));
    }

    public static /* synthetic */ void lambda$tryRemoveUserFromSpCacheLater$2(LockSettingsService lockSettingsService, int userId) {
        if (!lockSettingsService.shouldCacheSpForUser(userId)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removing SP from cache for user ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
            synchronized (lockSettingsService.mSpManager) {
                lockSettingsService.mSpCache.remove(userId);
            }
        }
    }

    private boolean shouldCacheSpForUser(int userId) {
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, userId) == 0) {
            return true;
        }
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (dpmi == null) {
            return false;
        }
        return dpmi.canUserHaveUntrustedCredentialReset(userId);
    }

    @GuardedBy("mSpManager")
    @VisibleForTesting
    protected AuthenticationToken initializeSyntheticPasswordLocked(byte[] credentialHash, String credential, int credentialType, int requestedQuality, int userId) throws RemoteException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Initialize SyntheticPassword for user: ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        AuthenticationToken auth = this.mSpManager.newSyntheticPasswordAndSid(getGateKeeperService(), credentialHash, credential, userId);
        onAuthTokenKnownForUser(userId, auth);
        if (auth == null) {
            Slog.wtf(TAG, "initializeSyntheticPasswordLocked returns null auth token");
            return null;
        }
        long handle = this.mSpManager.createPasswordBasedSyntheticPassword(getGateKeeperService(), credential, credentialType, auth, requestedQuality, userId);
        if (credential != null) {
            if (credentialHash == null) {
                this.mSpManager.newSidForUser(getGateKeeperService(), auth, userId);
            }
            this.mSpManager.verifyChallenge(getGateKeeperService(), auth, 0, userId);
            setAuthlessUserKeyProtection(userId, auth.deriveDiskEncryptionKey());
            setKeystorePassword(auth.deriveKeyStorePassword(), userId);
        } else {
            clearUserKeyProtection(userId);
            setKeystorePassword(null, userId);
            getGateKeeperService().clearSecureUserId(userId);
        }
        setLong("sp-handle", handle, userId);
        Slog.w(TAG, "initializeSP writeback handle");
        fixateNewestUserKeyAuth(userId);
        return auth;
    }

    private long getSyntheticPasswordHandleLocked(int userId) {
        return getLong("sp-handle", 0, userId);
    }

    private boolean isSyntheticPasswordBasedCredentialLocked(int userId) {
        boolean z = false;
        if (userId == -9999) {
            int type = this.mStorage.readPersistentDataBlock().type;
            if (type == 1 || type == 2) {
                z = true;
            }
            return z;
        }
        long handle = getSyntheticPasswordHandleLocked(userId);
        if (!(getLong("enable-sp", 1, 0) == 0 || handle == 0)) {
            z = true;
        }
        return z;
    }

    @VisibleForTesting
    protected boolean shouldMigrateToSyntheticPasswordLocked(int userId) {
        long handle = getSyntheticPasswordHandleLocked(userId);
        if (getLong("enable-sp", 1, 0) == 0 || handle != 0) {
            return false;
        }
        return true;
    }

    private void enableSyntheticPasswordLocked() {
        setLong("enable-sp", 1, 0);
    }

    /* JADX WARNING: Missing block: B:37:0x0084, code:
            if (r3.getResponseCode() != 0) goto L_0x00df;
     */
    /* JADX WARNING: Missing block: B:38:0x0086, code:
            notifyActivePasswordMetricsAvailable(r14, r13);
            unlockKeystore(r0.authToken.deriveKeyStorePassword(), r13);
            r5 = r0.authToken.deriveDiskEncryptionKey();
            r6 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("Unlocking user ");
            r7.append(r13);
            r7.append(" with secret only, length ");
            r7.append(r5.length);
            android.util.Slog.i(r6, r7.toString());
            unlockUser(r13, r4, r5);
            activateEscrowTokens(r0.authToken, r13);
     */
    /* JADX WARNING: Missing block: B:39:0x00c3, code:
            if (isManagedProfileWithSeparatedLock(r13) == false) goto L_0x00d4;
     */
    /* JADX WARNING: Missing block: B:40:0x00c5, code:
            ((android.app.trust.TrustManager) r1.mContext.getSystemService("trust")).setDeviceLockedForUser(r13, false);
     */
    /* JADX WARNING: Missing block: B:41:0x00d4, code:
            r1.mStrongAuth.reportSuccessfulStrongAuthUnlock(r13);
            onAuthTokenKnownForUser(r13, r0.authToken);
     */
    /* JADX WARNING: Missing block: B:43:0x00e4, code:
            if (r3.getResponseCode() != 1) goto L_0x00f1;
     */
    /* JADX WARNING: Missing block: B:45:0x00ea, code:
            if (r3.getTimeout() <= 0) goto L_0x00f1;
     */
    /* JADX WARNING: Missing block: B:46:0x00ec, code:
            requireStrongAuth(8, r13);
     */
    /* JADX WARNING: Missing block: B:47:0x00f1, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private VerifyCredentialResponse spBasedDoVerifyCredential(String userCredential, int credentialType, boolean hasChallenge, long challenge, int userId, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        Throwable th;
        int i = credentialType;
        int i2 = userId;
        String userCredential2 = i == -1 ? null : userCredential;
        synchronized (this.mSpManager) {
            try {
                if (!isSyntheticPasswordBasedCredentialLocked(i2)) {
                    return null;
                } else if (i2 == -9999) {
                    VerifyCredentialResponse verifyFrpCredential = this.mSpManager.verifyFrpCredential(getGateKeeperService(), userCredential2, i, progressCallback);
                    return verifyFrpCredential;
                } else {
                    AuthenticationResult authResult = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), getSyntheticPasswordHandleLocked(i2), userCredential2, i2, progressCallback);
                    VerifyCredentialResponse verifyCredentialResponse;
                    if (authResult.credentialType != i) {
                        Slog.e(TAG, "Credential type mismatch.");
                        verifyCredentialResponse = VerifyCredentialResponse.ERROR;
                        return verifyCredentialResponse;
                    }
                    verifyCredentialResponse = authResult.gkResponse;
                    byte[] bArr;
                    if (verifyCredentialResponse.getResponseCode() == 0) {
                        bArr = null;
                        verifyCredentialResponse = this.mSpManager.verifyChallenge(getGateKeeperService(), authResult.authToken, challenge, i2);
                        if (verifyCredentialResponse.getResponseCode() != 0) {
                            Slog.wtf(TAG, "verifyChallenge with SP failed.");
                            VerifyCredentialResponse verifyCredentialResponse2 = VerifyCredentialResponse.ERROR;
                            return verifyCredentialResponse2;
                        }
                    }
                    bArr = null;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    @GuardedBy("mSpManager")
    private long setLockCredentialWithAuthTokenLocked(String credential, int credentialType, AuthenticationToken auth, int requestedQuality, int userId) throws RemoteException {
        Map<Integer, String> profilePasswords;
        String str = credential;
        int i = userId;
        long newHandle = this.mSpManager.createPasswordBasedSyntheticPassword(getGateKeeperService(), str, credentialType, auth, requestedQuality, i);
        AuthenticationToken authenticationToken;
        if (str != null) {
            profilePasswords = null;
            if (this.mSpManager.hasSidForUser(i)) {
                this.mSpManager.verifyChallenge(getGateKeeperService(), auth, 0, i);
                authenticationToken = auth;
            } else {
                authenticationToken = auth;
                this.mSpManager.newSidForUser(getGateKeeperService(), authenticationToken, i);
                this.mSpManager.verifyChallenge(getGateKeeperService(), authenticationToken, 0, i);
                setAuthlessUserKeyProtection(i, auth.deriveDiskEncryptionKey());
                setKeystorePassword(auth.deriveKeyStorePassword(), i);
                setLong("sp-handle", newHandle, i);
                fixateNewestUserKeyAuth(i);
            }
        } else {
            authenticationToken = auth;
            profilePasswords = getDecryptedPasswordsForAllTiedProfiles(i);
            this.mSpManager.clearSidForUser(i);
            getGateKeeperService().clearSecureUserId(i);
            clearUserKeyProtection(i);
            fixateNewestUserKeyAuth(i);
            setKeystorePassword(null, i);
            handleUserClearLockForAnti(i);
        }
        setLong("sp-handle", newHandle, i);
        synchronizeUnifiedWorkChallengeForProfiles(i, profilePasswords);
        notifyActivePasswordMetricsAvailable(str, i);
        return newHandle;
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x006b  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00ac  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0088  */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0011 A:{Splitter: B:2:0x000c, ExcHandler: java.security.UnrecoverableKeyException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x0011, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0012, code:
            r1 = r0;
            android.util.Slog.e(TAG, "Failed to decrypt child profile key", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mSpManager")
    private void spBasedSetLockCredentialInternalLocked(String credential, int credentialType, String savedCredential, int requestedQuality, int userId) throws RemoteException {
        String savedCredential2;
        long handle;
        AuthenticationResult authResult;
        VerifyCredentialResponse response;
        AuthenticationToken auth;
        boolean untrustedReset;
        AuthenticationToken auth2;
        boolean untrustedReset2;
        int i = credentialType;
        int i2 = userId;
        if (isManagedProfileWithUnifiedLock(i2)) {
            try {
                savedCredential2 = getDecryptedPasswordForTiedProfile(i2);
            } catch (FileNotFoundException e) {
                FileNotFoundException fileNotFoundException = e;
                Slog.i(TAG, "Child profile key not found");
            } catch (Exception e2) {
            }
            handle = getSyntheticPasswordHandleLocked(i2);
            authResult = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle, savedCredential2, i2, null);
            response = authResult.gkResponse;
            auth = authResult.authToken;
            if (savedCredential2 == null && auth == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to enroll ");
                stringBuilder.append(i == 2 ? "password" : "pattern");
                throw new RemoteException(stringBuilder.toString());
            }
            String str;
            long j;
            int i3;
            untrustedReset = false;
            if (auth == null) {
                onAuthTokenKnownForUser(i2, auth);
            } else if (response == null || response.getResponseCode() != -1) {
                str = credential;
                j = handle;
                i3 = i2;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("spBasedSetLockCredentialInternalLocked: ");
                stringBuilder2.append(response != null ? "rate limit exceeded" : "failed");
                Slog.w(str2, stringBuilder2.toString());
                return;
            } else {
                Slog.w(TAG, "Untrusted credential change invoked");
                auth = (AuthenticationToken) this.mSpCache.get(i2);
                untrustedReset = true;
            }
            auth2 = auth;
            untrustedReset2 = untrustedReset;
            if (auth2 == null) {
                if (untrustedReset2) {
                    this.mSpManager.newSidForUser(getGateKeeperService(), auth2, i2);
                }
                j = handle;
                i3 = i2;
                setLockCredentialWithAuthTokenLocked(credential, i, auth2, requestedQuality, i2);
                this.mSpManager.destroyPasswordBasedSyntheticPassword(j, i3);
                this.mRecoverableKeyStoreManager.lockScreenSecretChanged(i, credential, i3);
                return;
            }
            str = credential;
            j = handle;
            i3 = i2;
            throw new IllegalStateException("Untrusted credential reset not possible without cached SP");
        }
        savedCredential2 = savedCredential;
        handle = getSyntheticPasswordHandleLocked(i2);
        authResult = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle, savedCredential2, i2, null);
        response = authResult.gkResponse;
        auth = authResult.authToken;
        if (savedCredential2 == null) {
        }
        untrustedReset = false;
        if (auth == null) {
        }
        auth2 = auth;
        untrustedReset2 = untrustedReset;
        if (auth2 == null) {
        }
    }

    public byte[] getHashFactor(String currentCredential, int userId) throws RemoteException {
        checkPasswordReadPermission(userId);
        if (TextUtils.isEmpty(currentCredential)) {
            currentCredential = null;
        }
        if (isManagedProfileWithUnifiedLock(userId)) {
            try {
                currentCredential = getDecryptedPasswordForTiedProfile(userId);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to get work profile credential", e);
                return null;
            }
        }
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                AuthenticationResult auth = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), getSyntheticPasswordHandleLocked(userId), currentCredential, userId, null);
                if (auth.authToken == null) {
                    Slog.w(TAG, "Current credential is incorrect");
                    return null;
                }
                byte[] derivePasswordHashFactor = auth.authToken.derivePasswordHashFactor();
                return derivePasswordHashFactor;
            }
            Slog.w(TAG, "Synthetic password not enabled");
            return null;
        }
    }

    private long addEscrowToken(byte[] token, int userId) throws RemoteException {
        long handle;
        synchronized (this.mSpManager) {
            enableSyntheticPasswordLocked();
            AuthenticationToken auth = null;
            if (!isUserSecure(userId)) {
                if (shouldMigrateToSyntheticPasswordLocked(userId)) {
                    auth = initializeSyntheticPasswordLocked(null, null, -1, 0, userId);
                } else {
                    auth = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), getSyntheticPasswordHandleLocked(userId), null, userId, null).authToken;
                }
            }
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                disableEscrowTokenOnNonManagedDevicesIfNeeded(userId);
                if (!this.mSpManager.hasEscrowData(userId)) {
                    throw new SecurityException("Escrow token is disabled on the current user");
                }
            }
            handle = this.mSpManager.createTokenBasedSyntheticPassword(token, userId);
            if (auth != null) {
                this.mSpManager.activateTokenBasedSyntheticPassword(handle, auth, userId);
            }
        }
        return handle;
    }

    private void activateEscrowTokens(AuthenticationToken auth, int userId) {
        synchronized (this.mSpManager) {
            disableEscrowTokenOnNonManagedDevicesIfNeeded(userId);
            for (Long handle : this.mSpManager.getPendingTokensForUser(userId)) {
                long handle2 = handle.longValue();
                Slog.i(TAG, String.format("activateEscrowTokens: %x %d ", new Object[]{Long.valueOf(handle2), Integer.valueOf(userId)}));
                this.mSpManager.activateTokenBasedSyntheticPassword(handle2, auth, userId);
            }
        }
    }

    private boolean isEscrowTokenActive(long handle, int userId) {
        boolean existsHandle;
        synchronized (this.mSpManager) {
            existsHandle = this.mSpManager.existsHandle(handle, userId);
        }
        return existsHandle;
    }

    private boolean removeEscrowToken(long handle, int userId) {
        synchronized (this.mSpManager) {
            if (handle == getSyntheticPasswordHandleLocked(userId)) {
                Slog.w(TAG, "Cannot remove password handle");
                return false;
            } else if (this.mSpManager.removePendingToken(handle, userId)) {
                return true;
            } else if (this.mSpManager.existsHandle(handle, userId)) {
                this.mSpManager.destroyTokenBasedSyntheticPassword(handle, userId);
                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean setLockCredentialWithToken(String credential, int type, long tokenHandle, byte[] token, int requestedQuality, int userId) throws RemoteException {
        boolean result;
        int oldCredentialType = getOldCredentialType(userId);
        synchronized (this.mSpManager) {
            if (this.mSpManager.hasEscrowData(userId)) {
                result = setLockCredentialWithTokenInternal(credential, type, tokenHandle, token, requestedQuality, userId);
            } else {
                throw new SecurityException("Escrow token is disabled on the current user");
            }
        }
        if (result) {
            synchronized (this.mSeparateChallengeLock) {
                setSeparateProfileChallengeEnabledLocked(userId, true, null);
            }
            notifyPasswordChanged(userId);
            notifySeparateProfileChallengeChanged(userId);
            notifyPasswordStatusChanged(userId, getPasswordStatus(type, oldCredentialType));
        }
        return result;
    }

    private boolean setLockCredentialWithTokenInternal(String credential, int type, long tokenHandle, byte[] token, int requestedQuality, int userId) throws RemoteException {
        Throwable th;
        int i = userId;
        synchronized (this.mSpManager) {
            int i2;
            try {
                AuthenticationResult result = this.mSpManager.unwrapTokenBasedSyntheticPassword(getGateKeeperService(), tokenHandle, token, i);
                if (result.authToken == null) {
                    Slog.w(TAG, "Invalid escrow token supplied");
                    return false;
                } else if (result.gkResponse.getResponseCode() != 0) {
                    Slog.e(TAG, "Obsolete token: synthetic password derived but it fails GK verification.");
                    return false;
                } else {
                    i2 = requestedQuality;
                    try {
                        setLong("lockscreen.password_type", (long) i2, i);
                        long oldHandle = getSyntheticPasswordHandleLocked(i);
                        setLockCredentialWithAuthTokenLocked(credential, type, result.authToken, i2, i);
                        this.mSpManager.destroyPasswordBasedSyntheticPassword(oldHandle, i);
                        onAuthTokenKnownForUser(i, result.authToken);
                        return true;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                i2 = requestedQuality;
                throw th;
            }
        }
    }

    private boolean unlockUserWithToken(long tokenHandle, byte[] token, int userId) throws RemoteException {
        synchronized (this.mSpManager) {
            if (this.mSpManager.hasEscrowData(userId)) {
                AuthenticationResult authResult = this.mSpManager.unwrapTokenBasedSyntheticPassword(getGateKeeperService(), tokenHandle, token, userId);
                if (authResult.authToken == null) {
                    Slog.w(TAG, "Invalid escrow token supplied");
                    return false;
                }
                unlockUser(userId, null, authResult.authToken.deriveDiskEncryptionKey());
                onAuthTokenKnownForUser(userId, authResult.authToken);
                return true;
            }
            throw new SecurityException("Escrow token is disabled on the current user");
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("Current lock settings service state:");
            pw.println(String.format("SP Enabled = %b", new Object[]{Boolean.valueOf(this.mLockPatternUtils.isSyntheticPasswordEnabled())}));
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int user = 0; user < users.size(); user++) {
                int userId = ((UserInfo) users.get(user)).id;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("    User ");
                stringBuilder.append(userId);
                pw.println(stringBuilder.toString());
                synchronized (this.mSpManager) {
                    pw.println(String.format("        SP Handle = %x", new Object[]{Long.valueOf(getSyntheticPasswordHandleLocked(userId))}));
                }
                try {
                    pw.println(String.format("        SID = %x", new Object[]{Long.valueOf(getGateKeeperService().getSecureUserId(userId))}));
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void disableEscrowTokenOnNonManagedDevicesIfNeeded(int userId) {
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mUserManager.getUserInfo(userId) == null || !this.mUserManager.getUserInfo(userId).isManagedProfile()) {
                DevicePolicyManager dpm = this.mInjector.getDevicePolicyManager();
                if (dpm.getDeviceOwnerComponentOnAnyUser() != null) {
                    Slog.i(TAG, "Corp-owned device can have escrow token");
                    Binder.restoreCallingIdentity(ident);
                    return;
                } else if (dpm.getProfileOwnerAsUser(userId) != null) {
                    Slog.i(TAG, "User with profile owner can have escrow token");
                    Binder.restoreCallingIdentity(ident);
                    return;
                } else if (!dpm.isDeviceProvisioned()) {
                    Slog.i(TAG, "Postpone disabling escrow tokens until device is provisioned");
                    Binder.restoreCallingIdentity(ident);
                    return;
                } else if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                    Binder.restoreCallingIdentity(ident);
                    return;
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Disabling escrow token on user ");
                    stringBuilder.append(userId);
                    Slog.i(str, stringBuilder.toString());
                    if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                        this.mSpManager.destroyEscrowData(userId);
                    }
                    Binder.restoreCallingIdentity(ident);
                    return;
                }
            }
            Slog.i(TAG, "Managed profile can have escrow token");
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}
