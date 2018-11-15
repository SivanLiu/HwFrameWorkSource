package com.android.server.accounts;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManagerInternal;
import android.accounts.AccountManagerInternal.OnAppPermissionChangeListener;
import android.accounts.AccountManagerResponse;
import android.accounts.AuthenticatorDescription;
import android.accounts.CantAddAccountActivity;
import android.accounts.ChooseAccountActivity;
import android.accounts.GrantCredentialsPermissionActivity;
import android.accounts.IAccountAuthenticator;
import android.accounts.IAccountAuthenticatorResponse;
import android.accounts.IAccountManager.Stub;
import android.accounts.IAccountManagerResponse;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OnOpChangedInternalListener;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.RegisteredServicesCache.ServiceInfo;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class AccountManagerService extends Stub implements RegisteredServicesCacheListener<AuthenticatorDescription> {
    private static final Intent ACCOUNTS_CHANGED_INTENT = new Intent("android.accounts.LOGIN_ACCOUNTS_CHANGED");
    private static final Account[] EMPTY_ACCOUNT_ARRAY = new Account[0];
    private static final int MESSAGE_COPY_SHARED_ACCOUNT = 4;
    private static final int MESSAGE_TIMED_OUT = 3;
    private static final String PRE_N_DATABASE_NAME = "accounts.db";
    private static final int SIGNATURE_CHECK_MATCH = 1;
    private static final int SIGNATURE_CHECK_MISMATCH = 0;
    private static final int SIGNATURE_CHECK_UID_MATCH = 2;
    private static final String TAG = "AccountManagerService";
    private static AtomicReference<AccountManagerService> sThis = new AtomicReference();
    private final AppOpsManager mAppOpsManager;
    private CopyOnWriteArrayList<OnAppPermissionChangeListener> mAppPermissionChangeListeners = new CopyOnWriteArrayList();
    private final IAccountAuthenticatorCache mAuthenticatorCache;
    final Context mContext;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final MessageHandler mHandler;
    private final Injector mInjector;
    private final SparseBooleanArray mLocalUnlockedUsers = new SparseBooleanArray();
    private final PackageManager mPackageManager;
    private final LinkedHashMap<String, Session> mSessions = new LinkedHashMap();
    private UserManager mUserManager;
    private final SparseArray<UserAccounts> mUsers = new SparseArray();

    /* renamed from: com.android.server.accounts.AccountManagerService$1LogRecordTask */
    class AnonymousClass1LogRecordTask implements Runnable {
        private final long accountId;
        private final String action;
        private final int callingUid;
        private final String tableName;
        private final UserAccounts userAccount;
        private final long userDebugDbInsertionPoint;

        AnonymousClass1LogRecordTask(String action, String tableName, long accountId, UserAccounts userAccount, int callingUid, long userDebugDbInsertionPoint) {
            this.action = action;
            this.tableName = tableName;
            this.accountId = accountId;
            this.userAccount = userAccount;
            this.callingUid = callingUid;
            this.userDebugDbInsertionPoint = userDebugDbInsertionPoint;
        }

        public void run() {
            SQLiteStatement logStatement = this.userAccount.statementForLogging;
            logStatement.bindLong(1, this.accountId);
            logStatement.bindString(2, this.action);
            logStatement.bindString(3, AccountManagerService.this.mDateFormat.format(new Date()));
            logStatement.bindLong(4, (long) this.callingUid);
            logStatement.bindString(5, this.tableName);
            logStatement.bindLong(6, this.userDebugDbInsertionPoint);
            try {
                logStatement.execute();
            } catch (IllegalStateException e) {
                String str = AccountManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to insert a log record. accountId=");
                stringBuilder.append(this.accountId);
                stringBuilder.append(" action=");
                stringBuilder.append(this.action);
                stringBuilder.append(" tableName=");
                stringBuilder.append(this.tableName);
                stringBuilder.append(" Error: ");
                stringBuilder.append(e);
                Slog.w(str, stringBuilder.toString());
            } catch (Throwable th) {
                logStatement.clearBindings();
            }
            logStatement.clearBindings();
        }
    }

    private final class AccountManagerInternalImpl extends AccountManagerInternal {
        @GuardedBy("mLock")
        private AccountManagerBackupHelper mBackupHelper;
        private final Object mLock;

        private AccountManagerInternalImpl() {
            this.mLock = new Object();
        }

        /* synthetic */ AccountManagerInternalImpl(AccountManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void requestAccountAccess(Account account, String packageName, int userId, RemoteCallback callback) {
            if (account == null) {
                Slog.w(AccountManagerService.TAG, "account cannot be null");
            } else if (packageName == null) {
                Slog.w(AccountManagerService.TAG, "packageName cannot be null");
            } else if (userId < 0) {
                Slog.w(AccountManagerService.TAG, "user id must be concrete");
            } else if (callback == null) {
                Slog.w(AccountManagerService.TAG, "callback cannot be null");
            } else if (AccountManagerService.this.resolveAccountVisibility(account, packageName, AccountManagerService.this.getUserAccounts(userId)).intValue() == 3) {
                Slog.w(AccountManagerService.TAG, "requestAccountAccess: account is hidden");
            } else if (AccountManagerService.this.hasAccountAccess(account, packageName, new UserHandle(userId))) {
                Bundle result = new Bundle();
                result.putBoolean("booleanResult", true);
                callback.sendResult(result);
            } else {
                try {
                    UserAccounts userAccounts;
                    int uid = AccountManagerService.this.mPackageManager.getPackageUidAsUser(packageName, userId);
                    Intent intent = AccountManagerService.this.newRequestAccountAccessIntent(account, packageName, uid, callback);
                    synchronized (AccountManagerService.this.mUsers) {
                        userAccounts = (UserAccounts) AccountManagerService.this.mUsers.get(userId);
                    }
                    SystemNotificationChannels.createAccountChannelForPackage(packageName, uid, AccountManagerService.this.mContext);
                    AccountManagerService.this.doNotification(userAccounts, account, null, intent, packageName, userId);
                } catch (NameNotFoundException e) {
                    String str = AccountManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown package ");
                    stringBuilder.append(packageName);
                    Slog.e(str, stringBuilder.toString());
                }
            }
        }

        public void addOnAppPermissionChangeListener(OnAppPermissionChangeListener listener) {
            AccountManagerService.this.mAppPermissionChangeListeners.add(listener);
        }

        public boolean hasAccountAccess(Account account, int uid) {
            return AccountManagerService.this.hasAccountAccess(account, null, uid);
        }

        public byte[] backupAccountAccessPermissions(int userId) {
            byte[] backupAccountAccessPermissions;
            synchronized (this.mLock) {
                if (this.mBackupHelper == null) {
                    this.mBackupHelper = new AccountManagerBackupHelper(AccountManagerService.this, this);
                }
                backupAccountAccessPermissions = this.mBackupHelper.backupAccountAccessPermissions(userId);
            }
            return backupAccountAccessPermissions;
        }

        public void restoreAccountAccessPermissions(byte[] data, int userId) {
            synchronized (this.mLock) {
                if (this.mBackupHelper == null) {
                    this.mBackupHelper = new AccountManagerBackupHelper(AccountManagerService.this, this);
                }
                this.mBackupHelper.restoreAccountAccessPermissions(data, userId);
            }
        }
    }

    @VisibleForTesting
    static class Injector {
        private final Context mContext;

        public Injector(Context context) {
            this.mContext = context;
        }

        Looper getMessageHandlerLooper() {
            ServiceThread serviceThread = new ServiceThread(AccountManagerService.TAG, -2, true);
            serviceThread.start();
            return serviceThread.getLooper();
        }

        Context getContext() {
            return this.mContext;
        }

        void addLocalService(AccountManagerInternal service) {
            LocalServices.addService(AccountManagerInternal.class, service);
        }

        String getDeDatabaseName(int userId) {
            return new File(Environment.getDataSystemDeDirectory(userId), "accounts_de.db").getPath();
        }

        String getCeDatabaseName(int userId) {
            return new File(Environment.getDataSystemCeDirectory(userId), "accounts_ce.db").getPath();
        }

        String getPreNDatabaseName(int userId) {
            File systemDir = Environment.getDataSystemDirectory();
            File databaseFile = new File(Environment.getUserSystemDirectory(userId), AccountManagerService.PRE_N_DATABASE_NAME);
            if (userId == 0) {
                File oldFile = new File(systemDir, AccountManagerService.PRE_N_DATABASE_NAME);
                if (oldFile.exists() && !databaseFile.exists()) {
                    File userDir = Environment.getUserSystemDirectory(userId);
                    StringBuilder stringBuilder;
                    if (!userDir.exists() && !userDir.mkdirs()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("User dir cannot be created: ");
                        stringBuilder.append(userDir);
                        throw new IllegalStateException(stringBuilder.toString());
                    } else if (!oldFile.renameTo(databaseFile)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("User dir cannot be migrated: ");
                        stringBuilder.append(databaseFile);
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                }
            }
            return databaseFile.getPath();
        }

        IAccountAuthenticatorCache getAccountAuthenticatorCache() {
            return new AccountAuthenticatorCache(this.mContext);
        }

        INotificationManager getNotificationManager() {
            return NotificationManager.getService();
        }
    }

    class MessageHandler extends Handler {
        MessageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    msg.obj.onTimedOut();
                    return;
                case 4:
                    AccountManagerService.this.copyAccountToUser(null, (Account) msg.obj, msg.arg1, msg.arg2);
                    return;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unhandled message: ");
                    stringBuilder.append(msg.what);
                    throw new IllegalStateException(stringBuilder.toString());
            }
        }
    }

    private static class NotificationId {
        private final int mId;
        final String mTag;

        NotificationId(String tag, int type) {
            this.mTag = tag;
            this.mId = type;
        }
    }

    private abstract class Session extends IAccountAuthenticatorResponse.Stub implements DeathRecipient, ServiceConnection {
        final String mAccountName;
        final String mAccountType;
        protected final UserAccounts mAccounts;
        final boolean mAuthDetailsRequired;
        IAccountAuthenticator mAuthenticator;
        final long mCreationTime;
        final boolean mExpectActivityLaunch;
        private int mNumErrors;
        private int mNumRequestContinued;
        public int mNumResults;
        IAccountManagerResponse mResponse;
        private final boolean mStripAuthTokenFromResult;
        final boolean mUpdateLastAuthenticatedTime;

        public abstract void run() throws RemoteException;

        public Session(AccountManagerService accountManagerService, UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, boolean stripAuthTokenFromResult, String accountName, boolean authDetailsRequired) {
            this(accounts, response, accountType, expectActivityLaunch, stripAuthTokenFromResult, accountName, authDetailsRequired, false);
        }

        public Session(UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, boolean stripAuthTokenFromResult, String accountName, boolean authDetailsRequired, boolean updateLastAuthenticatedTime) {
            this.mNumResults = 0;
            this.mNumRequestContinued = 0;
            this.mNumErrors = 0;
            this.mAuthenticator = null;
            if (accountType != null) {
                this.mAccounts = accounts;
                this.mStripAuthTokenFromResult = stripAuthTokenFromResult;
                this.mResponse = response;
                this.mAccountType = accountType;
                this.mExpectActivityLaunch = expectActivityLaunch;
                this.mCreationTime = SystemClock.elapsedRealtime();
                this.mAccountName = accountName;
                this.mAuthDetailsRequired = authDetailsRequired;
                this.mUpdateLastAuthenticatedTime = updateLastAuthenticatedTime;
                synchronized (AccountManagerService.this.mSessions) {
                    AccountManagerService.this.mSessions.put(toString(), this);
                }
                if (response != null) {
                    try {
                        response.asBinder().linkToDeath(this, 0);
                        return;
                    } catch (RemoteException e) {
                        this.mResponse = null;
                        binderDied();
                        return;
                    }
                }
                return;
            }
            throw new IllegalArgumentException("accountType is null");
        }

        IAccountManagerResponse getResponseAndClose() {
            if (this.mResponse == null) {
                return null;
            }
            IAccountManagerResponse response = this.mResponse;
            close();
            return response;
        }

        protected boolean checkKeyIntent(int authUid, Intent intent) {
            Throwable th;
            Intent intent2 = intent;
            intent2.setFlags(intent.getFlags() & -196);
            long bid = Binder.clearCallingIdentity();
            int i;
            try {
                ResolveInfo resolveInfo = AccountManagerService.this.mContext.getPackageManager().resolveActivityAsUser(intent2, 0, this.mAccounts.userId);
                if (resolveInfo == null) {
                    Binder.restoreCallingIdentity(bid);
                    return false;
                }
                ActivityInfo targetActivityInfo = resolveInfo.activityInfo;
                int targetUid = targetActivityInfo.applicationInfo.uid;
                PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                if (isExportedSystemActivity(targetActivityInfo)) {
                    i = authUid;
                } else {
                    try {
                        if (!pmi.hasSignatureCapability(targetUid, authUid, 16)) {
                            String pkgName = targetActivityInfo.packageName;
                            String activityName = targetActivityInfo.name;
                            Log.e(AccountManagerService.TAG, String.format("KEY_INTENT resolved to an Activity (%s) in a package (%s) that does not share a signature with the supplying authenticator (%s).", new Object[]{activityName, pkgName, this.mAccountType}));
                            Binder.restoreCallingIdentity(bid);
                            return false;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(bid);
                        throw th;
                    }
                }
                Binder.restoreCallingIdentity(bid);
                return true;
            } catch (Throwable th3) {
                th = th3;
                i = authUid;
                Binder.restoreCallingIdentity(bid);
                throw th;
            }
        }

        private boolean isExportedSystemActivity(ActivityInfo activityInfo) {
            String className = activityInfo.name;
            return PackageManagerService.PLATFORM_PACKAGE_NAME.equals(activityInfo.packageName) && (GrantCredentialsPermissionActivity.class.getName().equals(className) || CantAddAccountActivity.class.getName().equals(className));
        }

        /* JADX WARNING: Missing block: B:9:0x001c, code:
            if (r3.mResponse == null) goto L_0x002b;
     */
        /* JADX WARNING: Missing block: B:10:0x001e, code:
            r3.mResponse.asBinder().unlinkToDeath(r3, 0);
            r3.mResponse = null;
     */
        /* JADX WARNING: Missing block: B:11:0x002b, code:
            cancelTimeout();
            unbind();
     */
        /* JADX WARNING: Missing block: B:12:0x0031, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void close() {
            synchronized (AccountManagerService.this.mSessions) {
                if (AccountManagerService.this.mSessions.remove(toString()) == null) {
                }
            }
        }

        public void binderDied() {
            this.mResponse = null;
            close();
        }

        protected String toDebugString() {
            return toDebugString(SystemClock.elapsedRealtime());
        }

        protected String toDebugString(long now) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Session: expectLaunch ");
            stringBuilder.append(this.mExpectActivityLaunch);
            stringBuilder.append(", connected ");
            stringBuilder.append(this.mAuthenticator != null);
            stringBuilder.append(", stats (");
            stringBuilder.append(this.mNumResults);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.mNumRequestContinued);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.mNumErrors);
            stringBuilder.append("), lifetime ");
            stringBuilder.append(((double) (now - this.mCreationTime)) / 1000.0d);
            return stringBuilder.toString();
        }

        void bind() {
            String str;
            StringBuilder stringBuilder;
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                str = AccountManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("initiating bind to authenticator type ");
                stringBuilder.append(this.mAccountType);
                Log.v(str, stringBuilder.toString());
            }
            if (!bindToAuthenticator(this.mAccountType)) {
                str = AccountManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("bind attempt failed for ");
                stringBuilder.append(toDebugString());
                Log.d(str, stringBuilder.toString());
                onError(1, "bind failure");
            }
        }

        private void unbind() {
            if (this.mAuthenticator != null) {
                this.mAuthenticator = null;
                AccountManagerService.this.mContext.unbindService(this);
            }
        }

        public void cancelTimeout() {
            AccountManagerService.this.mHandler.removeMessages(3, this);
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mAuthenticator = IAccountAuthenticator.Stub.asInterface(service);
            try {
                run();
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            this.mAuthenticator = null;
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                try {
                    response.onError(1, "disconnected");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onServiceDisconnected: caught RemoteException while responding", e);
                    }
                }
            }
        }

        public void onTimedOut() {
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                try {
                    response.onError(1, "timeout");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onTimedOut: caught RemoteException while responding", e);
                    }
                }
            }
        }

        public void onResult(Bundle result) {
            IAccountManagerResponse response;
            boolean needUpdate = true;
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            Intent intent = null;
            if (result != null) {
                boolean isSuccessfulConfirmCreds = result.getBoolean("booleanResult", false);
                boolean isSuccessfulUpdateCredsOrAddAccount = result.containsKey("authAccount") && result.containsKey("accountType");
                if (!(this.mUpdateLastAuthenticatedTime && (isSuccessfulConfirmCreds || isSuccessfulUpdateCredsOrAddAccount))) {
                    needUpdate = false;
                }
                if (needUpdate || this.mAuthDetailsRequired) {
                    boolean accountPresent = AccountManagerService.this.isAccountPresentForCaller(this.mAccountName, this.mAccountType);
                    if (needUpdate && accountPresent) {
                        AccountManagerService.this.updateLastAuthenticatedTime(new Account(this.mAccountName, this.mAccountType));
                    }
                    if (this.mAuthDetailsRequired) {
                        long lastAuthenticatedTime = -1;
                        if (accountPresent) {
                            lastAuthenticatedTime = this.mAccounts.accountsDb.findAccountLastAuthenticatedTime(new Account(this.mAccountName, this.mAccountType));
                        }
                        result.putLong("lastAuthenticatedTime", lastAuthenticatedTime);
                    }
                }
            }
            if (result != null) {
                Intent intent2 = (Intent) result.getParcelable(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT);
                intent = intent2;
                if (!(intent2 == null || checkKeyIntent(Binder.getCallingUid(), intent))) {
                    onError(5, "invalid intent in bundle returned");
                    return;
                }
            }
            if (!(result == null || TextUtils.isEmpty(result.getString("authtoken")))) {
                String accountName = result.getString("authAccount");
                String accountType = result.getString("accountType");
                if (!(TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType))) {
                    AccountManagerService.this.cancelNotification(AccountManagerService.this.getSigninRequiredNotificationId(this.mAccounts, new Account(accountName, accountType)), new UserHandle(this.mAccounts.userId));
                }
            }
            if (this.mExpectActivityLaunch && result != null && result.containsKey(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT)) {
                response = this.mResponse;
            } else {
                response = getResponseAndClose();
            }
            if (response != null) {
                if (result == null) {
                    try {
                        if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                            String str = AccountManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(getClass().getSimpleName());
                            stringBuilder.append(" calling onError() on response ");
                            stringBuilder.append(response);
                            Log.v(str, stringBuilder.toString());
                        }
                        response.onError(5, "null bundle returned");
                    } catch (RemoteException e) {
                        if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                            Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                        }
                    }
                } else {
                    if (this.mStripAuthTokenFromResult) {
                        result.remove("authtoken");
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        String str2 = AccountManagerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(getClass().getSimpleName());
                        stringBuilder2.append(" calling onResult() on response ");
                        stringBuilder2.append(response);
                        Log.v(str2, stringBuilder2.toString());
                    }
                    if (result.getInt("errorCode", -1) <= 0 || intent != null) {
                        response.onResult(result);
                    } else {
                        response.onError(result.getInt("errorCode"), result.getString("errorMessage"));
                    }
                }
            }
        }

        public void onRequestContinued() {
            this.mNumRequestContinued++;
        }

        public void onError(int errorCode, String errorMessage) {
            this.mNumErrors++;
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    String str = AccountManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(getClass().getSimpleName());
                    stringBuilder.append(" calling onError() on response ");
                    stringBuilder.append(response);
                    Log.v(str, stringBuilder.toString());
                }
                try {
                    response.onError(errorCode, errorMessage);
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onError: caught RemoteException while responding", e);
                    }
                }
            } else if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "Session.onError: already closed");
            }
        }

        private boolean bindToAuthenticator(String authenticatorType) {
            ServiceInfo<AuthenticatorDescription> authenticatorInfo = AccountManagerService.this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(authenticatorType), this.mAccounts.userId);
            String str;
            StringBuilder stringBuilder;
            if (authenticatorInfo == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    str = AccountManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("there is no authenticator for ");
                    stringBuilder.append(authenticatorType);
                    stringBuilder.append(", bailing out");
                    Log.v(str, stringBuilder.toString());
                }
                return false;
            } else if (AccountManagerService.this.isLocalUnlockedUser(this.mAccounts.userId) || authenticatorInfo.componentInfo.directBootAware) {
                StringBuilder stringBuilder2;
                Intent intent = new Intent();
                intent.setAction("android.accounts.AccountAuthenticator");
                intent.setComponent(authenticatorInfo.componentName);
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    String str2 = AccountManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("performing bindService to ");
                    stringBuilder2.append(authenticatorInfo.componentName);
                    Log.v(str2, stringBuilder2.toString());
                }
                int flags = 1;
                if (AccountManagerService.this.mAuthenticatorCache.getBindInstantServiceAllowed(this.mAccounts.userId)) {
                    flags = 1 | DumpState.DUMP_CHANGES;
                }
                if (AccountManagerService.this.mContext.bindServiceAsUser(intent, this, flags, UserHandle.of(this.mAccounts.userId))) {
                    return true;
                }
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    str = AccountManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("bindService to ");
                    stringBuilder2.append(authenticatorInfo.componentName);
                    stringBuilder2.append(" failed");
                    Log.v(str, stringBuilder2.toString());
                }
                return false;
            } else {
                str = AccountManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Blocking binding to authenticator ");
                stringBuilder.append(authenticatorInfo.componentName);
                stringBuilder.append(" which isn't encryption aware");
                Slog.w(str, stringBuilder.toString());
                return false;
            }
        }
    }

    static class UserAccounts {
        final HashMap<String, Account[]> accountCache = new LinkedHashMap();
        private final TokenCache accountTokenCaches = new TokenCache();
        final AccountsDb accountsDb;
        private final Map<Account, Map<String, String>> authTokenCache = new HashMap();
        final Object cacheLock = new Object();
        private final HashMap<Pair<Pair<Account, String>, Integer>, NotificationId> credentialsPermissionNotificationIds = new HashMap();
        final Object dbLock = new Object();
        private int debugDbInsertionPoint = -1;
        private final Map<String, Map<String, Integer>> mReceiversForType = new HashMap();
        private final HashMap<Account, AtomicReference<String>> previousNameCache = new HashMap();
        private final HashMap<Account, NotificationId> signinRequiredNotificationIds = new HashMap();
        private SQLiteStatement statementForLogging;
        private final Map<Account, Map<String, String>> userDataCache = new HashMap();
        private final int userId;
        private final Map<Account, Map<String, Integer>> visibilityCache = new HashMap();

        UserAccounts(Context context, int userId, File preNDbFile, File deDbFile) {
            this.userId = userId;
            synchronized (this.dbLock) {
                synchronized (this.cacheLock) {
                    this.accountsDb = AccountsDb.create(context, userId, preNDbFile, deDbFile);
                }
            }
        }
    }

    private class GetAccountsByTypeAndFeatureSession extends Session {
        private volatile Account[] mAccountsOfType = null;
        private volatile ArrayList<Account> mAccountsWithFeatures = null;
        private final int mCallingUid;
        private volatile int mCurrentAccount = 0;
        private final String[] mFeatures;
        private final boolean mIncludeManagedNotVisible;
        private final String mPackageName;
        final /* synthetic */ AccountManagerService this$0;

        public GetAccountsByTypeAndFeatureSession(AccountManagerService accountManagerService, UserAccounts accounts, IAccountManagerResponse response, String type, String[] features, int callingUid, String packageName, boolean includeManagedNotVisible) {
            AccountManagerService accountManagerService2 = accountManagerService;
            this.this$0 = accountManagerService2;
            super(accountManagerService2, accounts, response, type, false, true, null, false);
            this.mCallingUid = callingUid;
            this.mFeatures = features;
            this.mPackageName = packageName;
            this.mIncludeManagedNotVisible = includeManagedNotVisible;
        }

        public void run() throws RemoteException {
            this.mAccountsOfType = this.this$0.getAccountsFromCache(this.mAccounts, this.mAccountType, this.mCallingUid, this.mPackageName, this.mIncludeManagedNotVisible);
            this.mAccountsWithFeatures = new ArrayList(this.mAccountsOfType.length);
            this.mCurrentAccount = 0;
            checkAccount();
        }

        public void checkAccount() {
            if (this.mCurrentAccount >= this.mAccountsOfType.length) {
                sendResult();
                return;
            }
            IAccountAuthenticator accountAuthenticator = this.mAuthenticator;
            if (accountAuthenticator == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    String str = AccountManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("checkAccount: aborting session since we are no longer connected to the authenticator, ");
                    stringBuilder.append(toDebugString());
                    Log.v(str, stringBuilder.toString());
                }
                return;
            }
            try {
                accountAuthenticator.hasFeatures(this, this.mAccountsOfType[this.mCurrentAccount], this.mFeatures);
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        public void onResult(Bundle result) {
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            if (result == null) {
                onError(5, "null bundle");
                return;
            }
            if (result.getBoolean("booleanResult", false)) {
                this.mAccountsWithFeatures.add(this.mAccountsOfType[this.mCurrentAccount]);
            }
            this.mCurrentAccount++;
            checkAccount();
        }

        public void sendResult() {
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                try {
                    Account[] accounts = new Account[this.mAccountsWithFeatures.size()];
                    for (int i = 0; i < accounts.length; i++) {
                        accounts[i] = (Account) this.mAccountsWithFeatures.get(i);
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        String str = AccountManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(getClass().getSimpleName());
                        stringBuilder.append(" calling onResult() on response ");
                        stringBuilder.append(response);
                        Log.v(str, stringBuilder.toString());
                    }
                    Bundle result = new Bundle();
                    result.putParcelableArray("accounts", accounts);
                    response.onResult(result);
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
            }
        }

        protected String toDebugString(long now) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(super.toDebugString(now));
            stringBuilder.append(", getAccountsByTypeAndFeatures, ");
            stringBuilder.append(this.mFeatures != null ? TextUtils.join(",", this.mFeatures) : null);
            return stringBuilder.toString();
        }
    }

    public static class Lifecycle extends SystemService {
        private AccountManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            this.mService = new AccountManagerService(new Injector(getContext()));
            publishBinderService("account", this.mService);
        }

        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }

        public void onStopUser(int userHandle) {
            String str = AccountManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onStopUser ");
            stringBuilder.append(userHandle);
            Slog.i(str, stringBuilder.toString());
            this.mService.purgeUserData(userHandle);
        }
    }

    private class RemoveAccountSession extends Session {
        final Account mAccount;

        public RemoveAccountSession(UserAccounts accounts, IAccountManagerResponse response, Account account, boolean expectActivityLaunch) {
            super(AccountManagerService.this, accounts, response, account.type, expectActivityLaunch, true, account.name, false);
            this.mAccount = account;
        }

        protected String toDebugString(long now) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(super.toDebugString(now));
            stringBuilder.append(", removeAccount, account ");
            stringBuilder.append(this.mAccount);
            return stringBuilder.toString();
        }

        public void run() throws RemoteException {
            this.mAuthenticator.getAccountRemovalAllowed(this, this.mAccount);
        }

        public void onResult(Bundle result) {
            Bundle.setDefusable(result, true);
            if (!(result == null || !result.containsKey("booleanResult") || result.containsKey(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT))) {
                if (result.getBoolean("booleanResult")) {
                    AccountManagerService.this.removeAccountInternal(this.mAccounts, this.mAccount, getCallingUid());
                }
                IAccountManagerResponse response = getResponseAndClose();
                if (response != null) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        String str = AccountManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(getClass().getSimpleName());
                        stringBuilder.append(" calling onResult() on response ");
                        stringBuilder.append(response);
                        Log.v(str, stringBuilder.toString());
                    }
                    try {
                        response.onResult(result);
                    } catch (RemoteException e) {
                        Slog.e(AccountManagerService.TAG, "Error calling onResult()", e);
                    }
                }
            }
            super.onResult(result);
        }
    }

    private abstract class StartAccountSession extends Session {
        private final boolean mIsPasswordForwardingAllowed;
        final /* synthetic */ AccountManagerService this$0;

        public StartAccountSession(AccountManagerService accountManagerService, UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, String accountName, boolean authDetailsRequired, boolean updateLastAuthenticationTime, boolean isPasswordForwardingAllowed) {
            AccountManagerService accountManagerService2 = accountManagerService;
            this.this$0 = accountManagerService2;
            super(accounts, response, accountType, expectActivityLaunch, true, accountName, authDetailsRequired, updateLastAuthenticationTime);
            this.mIsPasswordForwardingAllowed = isPasswordForwardingAllowed;
        }

        public void onResult(Bundle result) {
            IAccountManagerResponse response;
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            Intent intent = null;
            if (result != null) {
                Intent intent2 = (Intent) result.getParcelable(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT);
                intent = intent2;
                if (!(intent2 == null || checkKeyIntent(Binder.getCallingUid(), intent))) {
                    onError(5, "invalid intent in bundle returned");
                    return;
                }
            }
            if (this.mExpectActivityLaunch && result != null && result.containsKey(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT)) {
                response = this.mResponse;
            } else {
                response = getResponseAndClose();
            }
            if (response != null) {
                String str;
                StringBuilder stringBuilder;
                if (result == null) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        str = AccountManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(getClass().getSimpleName());
                        stringBuilder.append(" calling onError() on response ");
                        stringBuilder.append(response);
                        Log.v(str, stringBuilder.toString());
                    }
                    this.this$0.sendErrorResponse(response, 5, "null bundle returned");
                } else if (result.getInt("errorCode", -1) <= 0 || intent != null) {
                    if (!this.mIsPasswordForwardingAllowed) {
                        result.remove("password");
                    }
                    result.remove("authtoken");
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        str = AccountManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(getClass().getSimpleName());
                        stringBuilder.append(" calling onResult() on response ");
                        stringBuilder.append(response);
                        Log.v(str, stringBuilder.toString());
                    }
                    Bundle sessionBundle = result.getBundle("accountSessionBundle");
                    if (sessionBundle != null) {
                        String accountType = sessionBundle.getString("accountType");
                        if (TextUtils.isEmpty(accountType) || !this.mAccountType.equalsIgnoreCase(accountType)) {
                            Log.w(AccountManagerService.TAG, "Account type in session bundle doesn't match request.");
                        }
                        sessionBundle.putString("accountType", this.mAccountType);
                        try {
                            result.putBundle("accountSessionBundle", CryptoHelper.getInstance().encryptBundle(sessionBundle));
                        } catch (GeneralSecurityException e) {
                            if (Log.isLoggable(AccountManagerService.TAG, 3)) {
                                Log.v(AccountManagerService.TAG, "Failed to encrypt session bundle!", e);
                            }
                            this.this$0.sendErrorResponse(response, 5, "failed to encrypt session bundle");
                            return;
                        }
                    }
                    this.this$0.sendResponse(response, result);
                } else {
                    this.this$0.sendErrorResponse(response, result.getInt("errorCode"), result.getString("errorMessage"));
                }
            }
        }
    }

    private class TestFeaturesSession extends Session {
        private final Account mAccount;
        private final String[] mFeatures;

        public TestFeaturesSession(UserAccounts accounts, IAccountManagerResponse response, Account account, String[] features) {
            super(AccountManagerService.this, accounts, response, account.type, false, true, account.name, false);
            this.mFeatures = features;
            this.mAccount = account;
        }

        public void run() throws RemoteException {
            try {
                this.mAuthenticator.hasFeatures(this, this.mAccount, this.mFeatures);
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        public void onResult(Bundle result) {
            RemoteException e;
            Bundle.setDefusable(result, true);
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                if (result == null) {
                    try {
                        response.onError(5, "null bundle");
                    } catch (RemoteException e2) {
                        if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                            Log.v(AccountManagerService.TAG, "failure while notifying response", e2);
                        }
                    }
                } else {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        String str = AccountManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(getClass().getSimpleName());
                        stringBuilder.append(" calling onResult() on response ");
                        stringBuilder.append(response);
                        Log.v(str, stringBuilder.toString());
                    }
                    e2 = new Bundle();
                    e2.putBoolean("booleanResult", result.getBoolean("booleanResult", false));
                    response.onResult(e2);
                }
            }
        }

        protected String toDebugString(long now) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(super.toDebugString(now));
            stringBuilder.append(", hasFeatures, ");
            stringBuilder.append(this.mAccount);
            stringBuilder.append(", ");
            stringBuilder.append(this.mFeatures != null ? TextUtils.join(",", this.mFeatures) : null);
            return stringBuilder.toString();
        }
    }

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        */
    private void dumpUser(com.android.server.accounts.AccountManagerService.UserAccounts r17, java.io.FileDescriptor r18, java.io.PrintWriter r19, java.lang.String[] r20, boolean r21) {
        /*
        r16 = this;
        r7 = r16;
        r8 = r17;
        r9 = r19;
        if (r21 == 0) goto L_0x001a;
    L_0x0008:
        r1 = r8.dbLock;
        monitor-enter(r1);
        r0 = r8.accountsDb;
        r0.dumpDeAccountsTable(r9);
        monitor-exit(r1);
        r4 = r18;
        r5 = r20;
        goto L_0x0159;
    L_0x0017:
        r0 = move-exception;
        monitor-exit(r1);
        throw r0;
    L_0x001a:
        r3 = 0;
        r4 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r5 = 0;
        r6 = 0;
        r1 = r7;
        r2 = r8;
        r1 = r1.getAccountsFromCache(r2, r3, r4, r5, r6);
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r2 = "Accounts: ";
        r0.append(r2);
        r2 = r1.length;
        r0.append(r2);
        r0 = r0.toString();
        r9.println(r0);
        r0 = r1.length;
        r2 = 0;
    L_0x003c:
        if (r2 >= r0) goto L_0x0057;
    L_0x003e:
        r3 = r1[r2];
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "  ";
        r4.append(r5);
        r4.append(r3);
        r4 = r4.toString();
        r9.println(r4);
        r2 = r2 + 1;
        goto L_0x003c;
    L_0x0057:
        r19.println();
        r2 = r8.dbLock;
        monitor-enter(r2);
        r0 = r8.accountsDb;	 Catch:{ all -> 0x0169 }
        r0.dumpDebugTable(r9);	 Catch:{ all -> 0x0169 }
        monitor-exit(r2);	 Catch:{ all -> 0x0169 }
        r19.println();
        r3 = r7.mSessions;
        monitor-enter(r3);
        r4 = android.os.SystemClock.elapsedRealtime();	 Catch:{ all -> 0x0160 }
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0160 }
        r0.<init>();	 Catch:{ all -> 0x0160 }
        r2 = "Active Sessions: ";	 Catch:{ all -> 0x0160 }
        r0.append(r2);	 Catch:{ all -> 0x0160 }
        r2 = r7.mSessions;	 Catch:{ all -> 0x0160 }
        r2 = r2.size();	 Catch:{ all -> 0x0160 }
        r0.append(r2);	 Catch:{ all -> 0x0160 }
        r0 = r0.toString();	 Catch:{ all -> 0x0160 }
        r9.println(r0);	 Catch:{ all -> 0x0160 }
        r0 = r7.mSessions;	 Catch:{ all -> 0x0160 }
        r0 = r0.values();	 Catch:{ all -> 0x0160 }
        r0 = r0.iterator();	 Catch:{ all -> 0x0160 }
    L_0x0091:
        r2 = r0.hasNext();	 Catch:{ all -> 0x0160 }
        if (r2 == 0) goto L_0x00b6;	 Catch:{ all -> 0x0160 }
    L_0x0097:
        r2 = r0.next();	 Catch:{ all -> 0x0160 }
        r2 = (com.android.server.accounts.AccountManagerService.Session) r2;	 Catch:{ all -> 0x0160 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0160 }
        r6.<init>();	 Catch:{ all -> 0x0160 }
        r10 = "  ";	 Catch:{ all -> 0x0160 }
        r6.append(r10);	 Catch:{ all -> 0x0160 }
        r10 = r2.toDebugString(r4);	 Catch:{ all -> 0x0160 }
        r6.append(r10);	 Catch:{ all -> 0x0160 }
        r6 = r6.toString();	 Catch:{ all -> 0x0160 }
        r9.println(r6);	 Catch:{ all -> 0x0160 }
        goto L_0x0091;	 Catch:{ all -> 0x0160 }
    L_0x00b6:
        monitor-exit(r3);	 Catch:{ all -> 0x0160 }
        r19.println();
        r0 = r7.mAuthenticatorCache;
        r2 = r17.userId;
        r4 = r18;
        r5 = r20;
        r0.dump(r4, r9, r5, r2);
        r2 = r7.mUsers;
        monitor-enter(r2);
        r0 = r17.userId;
        r0 = r7.isLocalUnlockedUser(r0);
        r3 = r0;
        monitor-exit(r2);
        if (r3 != 0) goto L_0x00d7;
    L_0x00d6:
        return;
    L_0x00d7:
        r19.println();
        r6 = r8.dbLock;
        monitor-enter(r6);
        r0 = r8.accountsDb;
        r0 = r0.findAllVisibilityValues();
        r2 = "Account visibility:";
        r9.println(r2);
        r2 = r0.keySet();
        r2 = r2.iterator();
    L_0x00f0:
        r10 = r2.hasNext();
        if (r10 == 0) goto L_0x0158;
    L_0x00f6:
        r10 = r2.next();
        r10 = (android.accounts.Account) r10;
        r11 = new java.lang.StringBuilder;
        r11.<init>();
        r12 = "  ";
        r11.append(r12);
        r12 = r10.name;
        r11.append(r12);
        r11 = r11.toString();
        r9.println(r11);
        r11 = r0.get(r10);
        r11 = (java.util.Map) r11;
        r12 = r11.entrySet();
        r12 = r12.iterator();
    L_0x0120:
        r13 = r12.hasNext();
        if (r13 == 0) goto L_0x0156;
    L_0x0126:
        r13 = r12.next();
        r13 = (java.util.Map.Entry) r13;
        r14 = new java.lang.StringBuilder;
        r14.<init>();
        r15 = r0;
        r0 = "    ";
        r14.append(r0);
        r0 = r13.getKey();
        r0 = (java.lang.String) r0;
        r14.append(r0);
        r0 = ", ";
        r14.append(r0);
        r0 = r13.getValue();
        r14.append(r0);
        r0 = r14.toString();
        r9.println(r0);
        r0 = r15;
        goto L_0x0120;
    L_0x0156:
        r15 = r0;
        goto L_0x00f0;
    L_0x0158:
        monitor-exit(r6);
    L_0x0159:
        return;
    L_0x015a:
        r0 = move-exception;
        monitor-exit(r6);
        throw r0;
    L_0x015d:
        r0 = move-exception;
        monitor-exit(r2);
        throw r0;
    L_0x0160:
        r0 = move-exception;
        r4 = r18;
        r5 = r20;
    L_0x0165:
        monitor-exit(r3);	 Catch:{ all -> 0x0167 }
        throw r0;
    L_0x0167:
        r0 = move-exception;
        goto L_0x0165;
    L_0x0169:
        r0 = move-exception;
        r4 = r18;
        r5 = r20;
    L_0x016e:
        monitor-exit(r2);	 Catch:{ all -> 0x0170 }
        throw r0;
    L_0x0170:
        r0 = move-exception;
        goto L_0x016e;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.accounts.AccountManagerService.dumpUser(com.android.server.accounts.AccountManagerService$UserAccounts, java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[], boolean):void");
    }

    static {
        ACCOUNTS_CHANGED_INTENT.setFlags(83886080);
    }

    public static AccountManagerService getSingleton() {
        return (AccountManagerService) sThis.get();
    }

    public AccountManagerService(Injector injector) {
        this.mInjector = injector;
        this.mContext = injector.getContext();
        this.mPackageManager = this.mContext.getPackageManager();
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mHandler = new MessageHandler(injector.getMessageHandlerLooper());
        this.mAuthenticatorCache = this.mInjector.getAccountAuthenticatorCache();
        this.mAuthenticatorCache.setListener(this, null);
        sThis.set(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context1, Intent intent) {
                if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    final String removedPackageName = intent.getData().getSchemeSpecificPart();
                    AccountManagerService.this.mHandler.post(new Runnable() {
                        public void run() {
                            AccountManagerService.this.purgeOldGrantsAll();
                            AccountManagerService.this.removeVisibilityValuesForPackage(removedPackageName);
                        }
                    });
                }
            }
        }, intentFilter);
        injector.addLocalService(new AccountManagerInternalImpl(this, null));
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userId >= 1) {
                        String str = AccountManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("User ");
                        stringBuilder.append(userId);
                        stringBuilder.append(" removed");
                        Slog.i(str, stringBuilder.toString());
                        AccountManagerService.this.purgeUserData(userId);
                    }
                }
            }
        }, UserHandle.ALL, userFilter, null, null);
        new PackageMonitor() {
            public void onPackageAdded(String packageName, int uid) {
                AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(uid, true);
            }

            public void onPackageUpdateFinished(String packageName, int uid) {
                AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(uid, true);
            }
        }.register(this.mContext, this.mHandler.getLooper(), UserHandle.ALL, true);
        this.mAppOpsManager.startWatchingMode(62, null, new OnOpChangedInternalListener() {
            public void onOpChanged(int op, String packageName) {
                long identity;
                try {
                    int uid = AccountManagerService.this.mPackageManager.getPackageUidAsUser(packageName, ActivityManager.getCurrentUser());
                    if (AccountManagerService.this.mAppOpsManager.checkOpNoThrow(62, uid, packageName) == 0) {
                        identity = Binder.clearCallingIdentity();
                        AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(packageName, uid, true);
                        Binder.restoreCallingIdentity(identity);
                    }
                } catch (NameNotFoundException e) {
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        });
        this.mPackageManager.addOnPermissionsChangeListener(new -$$Lambda$AccountManagerService$c6GExIY3Vh2fORdBziuAPJbExac(this));
    }

    public static /* synthetic */ void lambda$new$0(AccountManagerService accountManagerService, int uid) {
        Throwable accounts;
        String[] packageNames = accountManagerService.mPackageManager.getPackagesForUid(uid);
        if (packageNames != null) {
            int userId = UserHandle.getUserId(uid);
            long identity = Binder.clearCallingIdentity();
            Account[] accounts2;
            try {
                int length = packageNames.length;
                accounts2 = null;
                int accounts3 = 0;
                while (accounts3 < length) {
                    try {
                        String packageName = packageNames[accounts3];
                        if (accountManagerService.mPackageManager.checkPermission("android.permission.GET_ACCOUNTS", packageName) == 0) {
                            if (accounts2 == null) {
                                accounts2 = accountManagerService.getAccountsAsUser(null, userId, PackageManagerService.PLATFORM_PACKAGE_NAME);
                                if (ArrayUtils.isEmpty(accounts2)) {
                                    Binder.restoreCallingIdentity(identity);
                                    return;
                                }
                            }
                            for (Account account : accounts2) {
                                accountManagerService.cancelAccountAccessRequestNotificationIfNeeded(account, uid, packageName, true);
                            }
                        }
                        accounts3++;
                    } catch (Throwable th) {
                        accounts = th;
                        Binder.restoreCallingIdentity(identity);
                        throw accounts;
                    }
                }
                Binder.restoreCallingIdentity(identity);
                Account[] accountArr = accounts2;
            } catch (Throwable th2) {
                accounts2 = null;
                accounts = th2;
                Binder.restoreCallingIdentity(identity);
                throw accounts;
            }
        }
    }

    boolean getBindInstantServiceAllowed(int userId) {
        return this.mAuthenticatorCache.getBindInstantServiceAllowed(userId);
    }

    void setBindInstantServiceAllowed(int userId, boolean allowed) {
        this.mAuthenticatorCache.setBindInstantServiceAllowed(userId, allowed);
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(int uid, boolean checkAccess) {
        for (Account account : getAccountsAsUser(null, UserHandle.getUserId(uid), PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            cancelAccountAccessRequestNotificationIfNeeded(account, uid, checkAccess);
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(String packageName, int uid, boolean checkAccess) {
        for (Account account : getAccountsAsUser(null, UserHandle.getUserId(uid), PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            cancelAccountAccessRequestNotificationIfNeeded(account, uid, packageName, checkAccess);
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(Account account, int uid, boolean checkAccess) {
        String[] packageNames = this.mPackageManager.getPackagesForUid(uid);
        if (packageNames != null) {
            for (String packageName : packageNames) {
                cancelAccountAccessRequestNotificationIfNeeded(account, uid, packageName, checkAccess);
            }
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(Account account, int uid, String packageName, boolean checkAccess) {
        if (!checkAccess || hasAccountAccess(account, packageName, UserHandle.getUserHandleForUid(uid))) {
            cancelNotification(getCredentialPermissionNotificationId(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", uid), packageName, UserHandle.getUserHandleForUid(uid));
        }
    }

    public boolean addAccountExplicitlyWithVisibility(Account account, String password, Bundle extras, Map packageToVisibility) {
        Account account2 = account;
        Bundle bundle = extras;
        Bundle.setDefusable(bundle, true);
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addAccountExplicitly: , caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        Preconditions.checkNotNull(account2, "account cannot be null");
        if (isAccountManagedByCaller(account2.type, callingUid, userId)) {
            long identityToken = clearCallingIdentity();
            try {
                boolean addAccountInternal = addAccountInternal(getUserAccounts(userId), account2, password, bundle, callingUid, packageToVisibility);
                return addAccountInternal;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new SecurityException(String.format("uid %s cannot explicitly add accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account2.type}));
        }
    }

    public Map<Account, Integer> getAccountsAndVisibilityForPackage(String packageName, String accountType) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        boolean isSystemUid = UserHandle.isSameApp(callingUid, true);
        List<String> managedTypes = getTypesForCaller(callingUid, userId, isSystemUid);
        if ((accountType == null || managedTypes.contains(accountType)) && (accountType != null || isSystemUid)) {
            if (accountType != null) {
                managedTypes = new ArrayList();
                managedTypes.add(accountType);
            }
            long identityToken = clearCallingIdentity();
            try {
                Map<Account, Integer> accountsAndVisibilityForPackage = getAccountsAndVisibilityForPackage(packageName, managedTypes, Integer.valueOf(callingUid), getUserAccounts(userId));
                return accountsAndVisibilityForPackage;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAccountsAndVisibilityForPackage() called from unauthorized uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(" with packageName=");
            stringBuilder.append(packageName);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private Map<Account, Integer> getAccountsAndVisibilityForPackage(String packageName, List<String> accountTypes, Integer callingUid, UserAccounts accounts) {
        if (packageExistsForUser(packageName, accounts.userId)) {
            Map<Account, Integer> result = new LinkedHashMap();
            for (String accountType : accountTypes) {
                synchronized (accounts.dbLock) {
                    synchronized (accounts.cacheLock) {
                        Account[] accountsOfType = (Account[]) accounts.accountCache.get((String) r1.next());
                        if (accountsOfType != null) {
                            for (Account account : accountsOfType) {
                                result.put(account, resolveAccountVisibility(account, packageName, accounts));
                            }
                        }
                    }
                }
            }
            return filterSharedAccounts(accounts, result, callingUid.intValue(), packageName);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Package not found ");
        stringBuilder.append(packageName);
        Log.d(str, stringBuilder.toString());
        return new LinkedHashMap();
    }

    public Map<String, Integer> getPackagesAndVisibilityForAccount(Account account) {
        Preconditions.checkNotNull(account, "account cannot be null");
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (isAccountManagedByCaller(account.type, callingUid, userId) || isSystemUid(callingUid)) {
            long identityToken = clearCallingIdentity();
            try {
                Map<String, Integer> packagesAndVisibilityForAccountLocked;
                UserAccounts accounts = getUserAccounts(userId);
                synchronized (accounts.dbLock) {
                    synchronized (accounts.cacheLock) {
                        packagesAndVisibilityForAccountLocked = getPackagesAndVisibilityForAccountLocked(account, accounts);
                    }
                }
                return packagesAndVisibilityForAccountLocked;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new SecurityException(String.format("uid %s cannot get secrets for account %s", new Object[]{Integer.valueOf(callingUid), account}));
        }
    }

    private Map<String, Integer> getPackagesAndVisibilityForAccountLocked(Account account, UserAccounts accounts) {
        Map<String, Integer> accountVisibility = (Map) accounts.visibilityCache.get(account);
        if (accountVisibility != null) {
            return accountVisibility;
        }
        Log.d(TAG, "Visibility was not initialized");
        HashMap accountVisibility2 = new HashMap();
        accounts.visibilityCache.put(account, accountVisibility2);
        return accountVisibility2;
    }

    public int getAccountVisibility(Account account, String packageName) {
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(packageName, "packageName cannot be null");
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (isAccountManagedByCaller(account.type, callingUid, userId) || isSystemUid(callingUid)) {
            long identityToken = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(userId);
                int visibility = "android:accounts:key_legacy_visible".equals(packageName);
                int visibility2;
                if (visibility != 0) {
                    visibility = getAccountVisibilityFromCache(account, packageName, accounts);
                    if (visibility != 0) {
                        return visibility;
                    }
                    restoreCallingIdentity(identityToken);
                    return 2;
                } else if ("android:accounts:key_legacy_not_visible".equals(packageName)) {
                    visibility2 = getAccountVisibilityFromCache(account, packageName, accounts);
                    if (visibility2 != 0) {
                        restoreCallingIdentity(identityToken);
                        return visibility2;
                    }
                    restoreCallingIdentity(identityToken);
                    return 4;
                } else {
                    visibility2 = resolveAccountVisibility(account, packageName, accounts).intValue();
                    restoreCallingIdentity(identityToken);
                    return visibility2;
                }
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new SecurityException(String.format("uid %s cannot get secrets for accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
        }
    }

    private int getAccountVisibilityFromCache(Account account, String packageName, UserAccounts accounts) {
        int intValue;
        synchronized (accounts.cacheLock) {
            Integer visibility = (Integer) getPackagesAndVisibilityForAccountLocked(account, accounts).get(packageName);
            intValue = visibility != null ? visibility.intValue() : 0;
        }
        return intValue;
    }

    private Integer resolveAccountVisibility(Account account, String packageName, UserAccounts accounts) {
        Preconditions.checkNotNull(packageName, "packageName cannot be null");
        long identityToken;
        try {
            identityToken = clearCallingIdentity();
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, accounts.userId);
            restoreCallingIdentity(identityToken);
            if (UserHandle.isSameApp(uid, 1000)) {
                return Integer.valueOf(1);
            }
            identityToken = checkPackageSignature(account.type, uid, accounts.userId);
            if (identityToken == 2) {
                return Integer.valueOf(1);
            }
            int visibility = getAccountVisibilityFromCache(account, packageName, accounts);
            if (visibility != 0) {
                return Integer.valueOf(visibility);
            }
            boolean isPrivileged = isPermittedForPackage(packageName, uid, accounts.userId, "android.permission.GET_ACCOUNTS_PRIVILEGED");
            if (isProfileOwner(uid)) {
                return Integer.valueOf(1);
            }
            boolean preO = isPreOApplication(packageName);
            if (identityToken != null || ((preO && checkGetAccountsPermission(packageName, uid, accounts.userId)) || ((checkReadContactsPermission(packageName, uid, accounts.userId) && accountTypeManagesContacts(account.type, accounts.userId)) || isPrivileged))) {
                visibility = getAccountVisibilityFromCache(account, "android:accounts:key_legacy_visible", accounts);
                if (visibility == 0) {
                    visibility = 2;
                }
            } else {
                visibility = getAccountVisibilityFromCache(account, "android:accounts:key_legacy_not_visible", accounts);
                if (visibility == 0) {
                    visibility = 4;
                }
            }
            return Integer.valueOf(visibility);
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package not found ");
            stringBuilder.append(e.getMessage());
            Log.d(str, stringBuilder.toString());
            return Integer.valueOf(3);
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    private boolean isPreOApplication(String packageName) {
        boolean z = true;
        long identityToken;
        try {
            identityToken = clearCallingIdentity();
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 0);
            restoreCallingIdentity(identityToken);
            if (applicationInfo == null) {
                return true;
            }
            if (applicationInfo.targetSdkVersion >= 26) {
                z = false;
            }
            return z;
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package not found ");
            stringBuilder.append(e.getMessage());
            Log.d(str, stringBuilder.toString());
            return true;
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean setAccountVisibility(Account account, String packageName, int newVisibility) {
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(packageName, "packageName cannot be null");
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (isAccountManagedByCaller(account.type, callingUid, userId) || isSystemUid(callingUid)) {
            long identityToken = clearCallingIdentity();
            try {
                boolean accountVisibility = setAccountVisibility(account, packageName, newVisibility, true, getUserAccounts(userId));
                return accountVisibility;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new SecurityException(String.format("uid %s cannot get secrets for accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
        }
    }

    private boolean isVisible(int visibility) {
        return visibility == 1 || visibility == 2;
    }

    private boolean setAccountVisibility(Account account, String packageName, int newVisibility, boolean notify, UserAccounts accounts) {
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                Map<String, Integer> packagesToVisibility;
                List<String> accountRemovedReceivers;
                if (notify) {
                    if (isSpecialPackageKey(packageName)) {
                        packagesToVisibility = getRequestingPackages(account, accounts);
                        accountRemovedReceivers = getAccountRemovedReceivers(account, accounts);
                    } else if (packageExistsForUser(packageName, accounts.userId)) {
                        packagesToVisibility = new HashMap();
                        packagesToVisibility.put(packageName, resolveAccountVisibility(account, packageName, accounts));
                        accountRemovedReceivers = new ArrayList();
                        if (shouldNotifyPackageOnAccountRemoval(account, packageName, accounts)) {
                            accountRemovedReceivers.add(packageName);
                        }
                    } else {
                        return false;
                    }
                } else if (isSpecialPackageKey(packageName) || packageExistsForUser(packageName, accounts.userId)) {
                    packagesToVisibility = Collections.emptyMap();
                    accountRemovedReceivers = Collections.emptyList();
                } else {
                    return false;
                }
                if (updateAccountVisibilityLocked(account, packageName, newVisibility, accounts)) {
                    if (notify) {
                        for (Entry<String, Integer> packageToVisibility : packagesToVisibility.entrySet()) {
                            if (isVisible(((Integer) packageToVisibility.getValue()).intValue()) != isVisible(resolveAccountVisibility(account, packageName, accounts).intValue())) {
                                notifyPackage((String) packageToVisibility.getKey(), accounts);
                            }
                        }
                        for (String packageNameToNotify : accountRemovedReceivers) {
                            sendAccountRemovedBroadcast(account, packageNameToNotify, accounts.userId);
                        }
                        sendAccountsChangedBroadcast(accounts.userId);
                    }
                    return true;
                }
                return false;
            }
        }
    }

    private boolean updateAccountVisibilityLocked(Account account, String packageName, int newVisibility, UserAccounts accounts) {
        long accountId = accounts.accountsDb.findDeAccountId(account);
        if (accountId < 0) {
            return false;
        }
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            if (!accounts.accountsDb.setAccountVisibility(accountId, packageName, newVisibility)) {
                return false;
            }
            StrictMode.setThreadPolicy(oldPolicy);
            getPackagesAndVisibilityForAccountLocked(account, accounts).put(packageName, Integer.valueOf(newVisibility));
            return true;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    public void registerAccountListener(String[] accountTypes, String opPackageName) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), opPackageName);
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            registerAccountListener(accountTypes, opPackageName, getUserAccounts(userId));
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void registerAccountListener(String[] accountTypes, String opPackageName, UserAccounts accounts) {
        synchronized (accounts.mReceiversForType) {
            if (accountTypes == null) {
                accountTypes = new String[]{null};
            }
            for (String type : accountTypes) {
                Map<String, Integer> receivers = (Map) accounts.mReceiversForType.get(type);
                if (receivers == null) {
                    receivers = new HashMap();
                    accounts.mReceiversForType.put(type, receivers);
                }
                Integer cnt = (Integer) receivers.get(opPackageName);
                int i = 1;
                if (cnt != null) {
                    i = 1 + cnt.intValue();
                }
                receivers.put(opPackageName, Integer.valueOf(i));
            }
        }
    }

    public void unregisterAccountListener(String[] accountTypes, String opPackageName) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), opPackageName);
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            unregisterAccountListener(accountTypes, opPackageName, getUserAccounts(userId));
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void unregisterAccountListener(String[] accountTypes, String opPackageName, UserAccounts accounts) {
        synchronized (accounts.mReceiversForType) {
            if (accountTypes == null) {
                accountTypes = new String[]{null};
            }
            for (String type : accountTypes) {
                Map<String, Integer> receivers = (Map) accounts.mReceiversForType.get(type);
                if (receivers == null || receivers.get(opPackageName) == null) {
                    throw new IllegalArgumentException("attempt to unregister wrong receiver");
                }
                Integer cnt = (Integer) receivers.get(opPackageName);
                if (cnt.intValue() == 1) {
                    receivers.remove(opPackageName);
                } else {
                    receivers.put(opPackageName, Integer.valueOf(cnt.intValue() - 1));
                }
            }
        }
    }

    private void sendNotificationAccountUpdated(Account account, UserAccounts accounts) {
        for (Entry<String, Integer> packageToVisibility : getRequestingPackages(account, accounts).entrySet()) {
            if (!(((Integer) packageToVisibility.getValue()).intValue() == 3 || ((Integer) packageToVisibility.getValue()).intValue() == 4)) {
                notifyPackage((String) packageToVisibility.getKey(), accounts);
            }
        }
    }

    private void notifyPackage(String packageName, UserAccounts accounts) {
        Intent intent = new Intent("android.accounts.action.VISIBLE_ACCOUNTS_CHANGED");
        intent.setPackage(packageName);
        intent.setFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(accounts.userId));
    }

    private Map<String, Integer> getRequestingPackages(Account account, UserAccounts accounts) {
        Set<String> packages = new HashSet();
        synchronized (accounts.mReceiversForType) {
            r2 = new String[2];
            int i = 0;
            r2[0] = account.type;
            r2[1] = null;
            int length = r2.length;
            while (i < length) {
                Map<String, Integer> receivers = (Map) accounts.mReceiversForType.get(r2[i]);
                if (receivers != null) {
                    packages.addAll(receivers.keySet());
                }
                i++;
            }
        }
        Map<String, Integer> result = new HashMap();
        for (String packageName : packages) {
            result.put(packageName, resolveAccountVisibility(account, packageName, accounts));
        }
        return result;
    }

    private List<String> getAccountRemovedReceivers(Account account, UserAccounts accounts) {
        Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
        intent.setFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        List<ResolveInfo> receivers = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 0, accounts.userId);
        List<String> result = new ArrayList();
        if (receivers == null) {
            return result;
        }
        for (ResolveInfo resolveInfo : receivers) {
            String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
            int visibility = resolveAccountVisibility(account, packageName, accounts).intValue();
            if (visibility == 1 || visibility == 2) {
                result.add(packageName);
            }
        }
        return result;
    }

    private boolean shouldNotifyPackageOnAccountRemoval(Account account, String packageName, UserAccounts accounts) {
        int visibility = resolveAccountVisibility(account, packageName, accounts).intValue();
        boolean z = true;
        if (visibility != 1 && visibility != 2) {
            return false;
        }
        Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
        intent.setFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.setPackage(packageName);
        List<ResolveInfo> receivers = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 0, accounts.userId);
        if (receivers == null || receivers.size() <= 0) {
            z = false;
        }
        return z;
    }

    private boolean packageExistsForUser(String packageName, int userId) {
        long identityToken;
        try {
            identityToken = clearCallingIdentity();
            this.mPackageManager.getPackageUidAsUser(packageName, userId);
            restoreCallingIdentity(identityToken);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    private boolean isSpecialPackageKey(String packageName) {
        return "android:accounts:key_legacy_visible".equals(packageName) || "android:accounts:key_legacy_not_visible".equals(packageName);
    }

    private void sendAccountsChangedBroadcast(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("the accounts changed, sending broadcast of ");
        stringBuilder.append(ACCOUNTS_CHANGED_INTENT.getAction());
        Log.i(str, stringBuilder.toString());
        this.mContext.sendBroadcastAsUser(ACCOUNTS_CHANGED_INTENT, new UserHandle(userId));
    }

    private void sendAccountRemovedBroadcast(Account account, String packageName, int userId) {
        Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
        intent.setFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.setPackage(packageName);
        intent.putExtra("authAccount", account.name);
        intent.putExtra("accountType", account.type);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(userId));
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Account Manager Crash", e);
            }
            throw e;
        }
    }

    private UserManager getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = UserManager.get(this.mContext);
        }
        return this.mUserManager;
    }

    public void validateAccounts(int userId) {
        try {
            validateAccountsInternal(getUserAccounts(userId), true);
        } catch (SQLiteException e) {
            Log.e(TAG, "validateAccounts ret got err:", e);
            HwBootFail.brokenFileBootFail(83886086, "/data/system_de/0/accounts_de.db/ or /data/system_ce/0/accounts_ce.db", new Throwable());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:130:0x0368 A:{SYNTHETIC, Splitter: B:130:0x0368} */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x0368 A:{SYNTHETIC, Splitter: B:130:0x0368} */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x0373 A:{SYNTHETIC, Splitter: B:138:0x0373} */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x0373 A:{SYNTHETIC, Splitter: B:138:0x0373} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void validateAccountsInternal(UserAccounts accounts, boolean invalidateAuthenticatorCache) {
        String str;
        boolean accountDeleted;
        Throwable th;
        AccountsDb accountsDb;
        Map<Long, Account> accountsDb2;
        SQLiteDiskIOException ex;
        UserAccounts userAccounts = accounts;
        if (Log.isLoggable(TAG, 3)) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("validateAccountsInternal ");
            stringBuilder.append(accounts.userId);
            stringBuilder.append(" isCeDatabaseAttached=");
            stringBuilder.append(userAccounts.accountsDb.isCeDatabaseAttached());
            stringBuilder.append(" userLocked=");
            stringBuilder.append(this.mLocalUnlockedUsers.get(accounts.userId));
            Log.d(str, stringBuilder.toString());
        }
        if (invalidateAuthenticatorCache) {
            this.mAuthenticatorCache.invalidateCache(accounts.userId);
        }
        HashMap<String, Integer> knownAuth = getAuthenticatorTypeAndUIDForUser(this.mAuthenticatorCache, accounts.userId);
        boolean userUnlocked = isLocalUnlockedUser(accounts.userId);
        synchronized (userAccounts.dbLock) {
            HashMap<String, Integer> hashMap;
            boolean z;
            try {
                synchronized (userAccounts.cacheLock) {
                    accountDeleted = false;
                    try {
                        Iterator it;
                        AccountsDb accountsDb3 = userAccounts.accountsDb;
                        Map<String, Integer> metaAuthUid = accountsDb3.findMetaAuthUid();
                        HashSet<String> obsoleteAuthType = Sets.newHashSet();
                        SparseBooleanArray knownUids = null;
                        for (Entry<String, Integer> authToUidEntry : metaAuthUid.entrySet()) {
                            try {
                                SparseBooleanArray knownUids2;
                                String type = (String) authToUidEntry.getKey();
                                int uid = ((Integer) authToUidEntry.getValue()).intValue();
                                Integer knownUid = (Integer) knownAuth.get(type);
                                if (knownUid != null) {
                                    if (uid == knownUid.intValue()) {
                                        knownAuth.remove(type);
                                    }
                                }
                                if (knownUids == null) {
                                    knownUids2 = getUidsOfInstalledOrUpdatedPackagesAsUser(accounts.userId);
                                } else {
                                    knownUids2 = knownUids;
                                }
                                if (!knownUids2.get(uid)) {
                                    obsoleteAuthType.add(type);
                                    accountsDb3.deleteMetaByAuthTypeAndUid(type, uid);
                                }
                                knownUids = knownUids2;
                            } catch (Throwable th2) {
                                th = th2;
                                hashMap = knownAuth;
                                z = userUnlocked;
                            }
                        }
                        for (Entry<String, Integer> entry : knownAuth.entrySet()) {
                            accountsDb3.insertOrReplaceMetaAuthTypeAndUid((String) entry.getKey(), ((Integer) entry.getValue()).intValue());
                        }
                        Map<Long, Account> accountsMap = accountsDb3.findAllDeAccounts();
                        HashSet<String> hashSet;
                        Map<String, Integer> map;
                        Map<Long, Account> map2;
                        AccountsDb accountsDb4;
                        int access$800;
                        try {
                            userAccounts.accountCache.clear();
                            HashMap<String, ArrayList<String>> accountNamesByType = new LinkedHashMap();
                            it = accountsMap.entrySet().iterator();
                            while (it.hasNext()) {
                                HashMap<String, ArrayList<String>> accountNamesByType2;
                                Entry<Long, Account> accountEntry = (Entry) it.next();
                                long accountId = ((Long) accountEntry.getKey()).longValue();
                                Account account = (Account) accountEntry.getValue();
                                Iterator it2 = it;
                                long accountId2;
                                if (obsoleteAuthType.contains(account.type)) {
                                    str = TAG;
                                    Entry<Long, Account> accountEntry2 = accountEntry;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    Map<Long, Account> accountsMap2 = accountsMap;
                                    try {
                                        stringBuilder2.append("deleting account because type ");
                                        stringBuilder2.append(account.type);
                                        stringBuilder2.append("'s registered authenticator no longer exist.");
                                        Slog.w(str, stringBuilder2.toString());
                                        accountsMap = getRequestingPackages(account, userAccounts);
                                        Map<String, Integer> accountEntry3 = getAccountRemovedReceivers(account, userAccounts);
                                        accountsDb3.beginTransaction();
                                        hashMap = knownAuth;
                                        accountId2 = accountId;
                                        HashMap<String, ArrayList<String>> hashMap2;
                                        try {
                                            accountsDb3.deleteDeAccount(accountId2);
                                            if (userUnlocked) {
                                                try {
                                                    accountsDb3.deleteCeAccount(accountId2);
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    hashMap2 = accountNamesByType;
                                                    hashSet = obsoleteAuthType;
                                                    z = userUnlocked;
                                                    accountsDb = accountsDb3;
                                                    map = metaAuthUid;
                                                    Entry<Long, Account> entry2 = accountEntry2;
                                                    map2 = accountsMap2;
                                                    userUnlocked = account;
                                                    metaAuthUid = accountEntry3;
                                                    accountsDb2 = accountsMap;
                                                }
                                            }
                                            accountsDb3.setTransactionSuccessful();
                                            try {
                                                accountsDb3.endTransaction();
                                                try {
                                                    z = userUnlocked;
                                                    userUnlocked = account;
                                                    map = metaAuthUid;
                                                    metaAuthUid = accountEntry3;
                                                    hashMap2 = accountNamesByType;
                                                    accountsDb = accountsDb3;
                                                    map2 = accountsMap2;
                                                    accountsDb2 = accountsMap;
                                                    hashSet = obsoleteAuthType;
                                                } catch (SQLiteDiskIOException e) {
                                                    ex = e;
                                                    hashSet = obsoleteAuthType;
                                                    z = userUnlocked;
                                                    map = metaAuthUid;
                                                    map2 = accountsMap2;
                                                    accountsDb4 = accountsDb3;
                                                    accountDeleted = true;
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    hashSet = obsoleteAuthType;
                                                    z = userUnlocked;
                                                    map = metaAuthUid;
                                                    map2 = accountsMap2;
                                                    accountsDb4 = accountsDb3;
                                                    accountDeleted = true;
                                                }
                                                try {
                                                    logRecord(AccountsDb.DEBUG_ACTION_AUTHENTICATOR_REMOVE, "accounts", accountId2, userAccounts);
                                                    accounts.userDataCache.remove(userUnlocked);
                                                    accounts.authTokenCache.remove(userUnlocked);
                                                    accounts.accountTokenCaches.remove(userUnlocked);
                                                    accounts.visibilityCache.remove(userUnlocked);
                                                    for (Entry<String, Integer> packageToVisibility : accountsDb2.entrySet()) {
                                                        if (isVisible(((Integer) packageToVisibility.getValue()).intValue())) {
                                                            notifyPackage((String) packageToVisibility.getKey(), userAccounts);
                                                        }
                                                    }
                                                    it = metaAuthUid.iterator();
                                                    while (it.hasNext()) {
                                                        sendAccountRemovedBroadcast(userUnlocked, (String) it.next(), accounts.userId);
                                                    }
                                                    accountDeleted = true;
                                                    accountNamesByType2 = hashMap2;
                                                    accountsDb4 = accountsDb;
                                                } catch (SQLiteDiskIOException e2) {
                                                    ex = e2;
                                                    accountDeleted = true;
                                                    accountsDb4 = accountsDb;
                                                    try {
                                                        Log.w(TAG, "validateAccountsInternal ret got err:", ex);
                                                        if (accountDeleted) {
                                                        }
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        if (accountDeleted) {
                                                        }
                                                        throw th;
                                                    }
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    accountDeleted = true;
                                                    accountsDb4 = accountsDb;
                                                    if (accountDeleted) {
                                                    }
                                                    throw th;
                                                }
                                            } catch (SQLiteDiskIOException e3) {
                                                ex = e3;
                                                hashSet = obsoleteAuthType;
                                                z = userUnlocked;
                                                map = metaAuthUid;
                                                map2 = accountsMap2;
                                                accountsDb4 = accountsDb3;
                                            } catch (Throwable th7) {
                                                th = th7;
                                                hashSet = obsoleteAuthType;
                                                z = userUnlocked;
                                                map = metaAuthUid;
                                                map2 = accountsMap2;
                                            }
                                        } catch (Throwable th8) {
                                            th = th8;
                                            hashMap2 = accountNamesByType;
                                            hashSet = obsoleteAuthType;
                                            z = userUnlocked;
                                            accountsDb = accountsDb3;
                                            map = metaAuthUid;
                                            accountId = accountEntry2;
                                            map2 = accountsMap2;
                                            userUnlocked = account;
                                            metaAuthUid = accountEntry3;
                                            accountsDb2 = accountsMap;
                                        }
                                    } catch (SQLiteDiskIOException e4) {
                                        ex = e4;
                                        hashSet = obsoleteAuthType;
                                        hashMap = knownAuth;
                                        z = userUnlocked;
                                        accountsDb4 = accountsDb3;
                                        map = metaAuthUid;
                                        map2 = accountsMap2;
                                    } catch (Throwable th9) {
                                        th = th9;
                                        hashSet = obsoleteAuthType;
                                        hashMap = knownAuth;
                                        z = userUnlocked;
                                        accountsDb4 = accountsDb3;
                                        map = metaAuthUid;
                                        map2 = accountsMap2;
                                    }
                                } else {
                                    map2 = accountsMap;
                                    hashMap = knownAuth;
                                    z = userUnlocked;
                                    map = metaAuthUid;
                                    accountId2 = accountId;
                                    Account account2 = account;
                                    hashSet = obsoleteAuthType;
                                    accountsDb4 = accountsDb3;
                                    accountNamesByType2 = accountNamesByType;
                                    ArrayList<String> accountNames = (ArrayList) accountNamesByType2.get(account2.type);
                                    if (accountNames == null) {
                                        accountNames = new ArrayList();
                                        accountNamesByType2.put(account2.type, accountNames);
                                    }
                                    accountNames.add(account2.name);
                                }
                                accountsDb3 = accountsDb4;
                                accountNamesByType = accountNamesByType2;
                                accountsMap = map2;
                                obsoleteAuthType = hashSet;
                                it = it2;
                                knownAuth = hashMap;
                                userUnlocked = z;
                                metaAuthUid = map;
                            }
                            hashSet = obsoleteAuthType;
                            hashMap = knownAuth;
                            z = userUnlocked;
                            accountsDb4 = accountsDb3;
                            map = metaAuthUid;
                            for (Entry<String, ArrayList<String>> cur : accountNamesByType.entrySet()) {
                                String accountType = (String) cur.getKey();
                                ArrayList obsoleteAuthType2 = (ArrayList) cur.getValue();
                                Account[] accountsForType = new Account[obsoleteAuthType2.size()];
                                for (int i = 0; i < accountsForType.length; i++) {
                                    accountsForType[i] = new Account((String) obsoleteAuthType2.get(i), accountType, UUID.randomUUID().toString());
                                }
                                userAccounts.accountCache.put(accountType, accountsForType);
                            }
                            accounts.visibilityCache.putAll(accountsDb4.findAllVisibilityValues());
                            if (accountDeleted) {
                                access$800 = accounts.userId;
                                sendAccountsChangedBroadcast(access$800);
                            }
                        } catch (SQLiteDiskIOException e5) {
                            ex = e5;
                            map2 = accountsMap;
                            hashSet = obsoleteAuthType;
                            hashMap = knownAuth;
                            z = userUnlocked;
                            accountsDb4 = accountsDb3;
                            map = metaAuthUid;
                            Log.w(TAG, "validateAccountsInternal ret got err:", ex);
                            if (accountDeleted) {
                                access$800 = accounts.userId;
                                sendAccountsChangedBroadcast(access$800);
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            map2 = accountsMap;
                            hashSet = obsoleteAuthType;
                            hashMap = knownAuth;
                            z = userUnlocked;
                            accountsDb4 = accountsDb3;
                            map = metaAuthUid;
                            if (accountDeleted) {
                                sendAccountsChangedBroadcast(accounts.userId);
                            }
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        throw th;
                    }
                }
            } catch (Throwable th12) {
                th = th12;
                throw th;
            }
        }
        try {
            accountsDb.endTransaction();
            throw th;
        } catch (SQLiteDiskIOException e6) {
            ex = e6;
            Log.w(TAG, "validateAccountsInternal ret got err:", ex);
            if (accountDeleted) {
            }
        }
    }

    private SparseBooleanArray getUidsOfInstalledOrUpdatedPackagesAsUser(int userId) {
        List<PackageInfo> pkgsWithData = this.mPackageManager.getInstalledPackagesAsUser(8192, userId);
        SparseBooleanArray knownUids = new SparseBooleanArray(pkgsWithData.size());
        for (PackageInfo pkgInfo : pkgsWithData) {
            if (!(pkgInfo.applicationInfo == null || (pkgInfo.applicationInfo.flags & DumpState.DUMP_VOLUMES) == 0)) {
                knownUids.put(pkgInfo.applicationInfo.uid, true);
            }
        }
        return knownUids;
    }

    static HashMap<String, Integer> getAuthenticatorTypeAndUIDForUser(Context context, int userId) {
        return getAuthenticatorTypeAndUIDForUser(new AccountAuthenticatorCache(context), userId);
    }

    private static HashMap<String, Integer> getAuthenticatorTypeAndUIDForUser(IAccountAuthenticatorCache authCache, int userId) {
        HashMap<String, Integer> knownAuth = new LinkedHashMap();
        for (ServiceInfo<AuthenticatorDescription> service : authCache.getAllServices(userId)) {
            knownAuth.put(((AuthenticatorDescription) service.type).type, Integer.valueOf(service.uid));
        }
        return knownAuth;
    }

    private UserAccounts getUserAccountsForCaller() {
        return getUserAccounts(UserHandle.getCallingUserId());
    }

    protected UserAccounts getUserAccounts(int userId) {
        UserAccounts accounts;
        synchronized (this.mUsers) {
            accounts = (UserAccounts) this.mUsers.get(userId);
            boolean validateAccounts = false;
            if (accounts == null) {
                accounts = new UserAccounts(this.mContext, userId, new File(this.mInjector.getPreNDatabaseName(userId)), new File(this.mInjector.getDeDatabaseName(userId)));
                try {
                    initializeDebugDbSizeAndCompileSqlStatementForLogging(accounts);
                } catch (SQLiteException e) {
                    Log.e(TAG, "initializeDebugDbSizeAndCompileSqlStatementForLogging got err:", e);
                }
                this.mUsers.append(userId, accounts);
                purgeOldGrants(accounts);
                validateAccounts = true;
            }
            if (!accounts.accountsDb.isCeDatabaseAttached() && this.mLocalUnlockedUsers.get(userId)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("User ");
                stringBuilder.append(userId);
                stringBuilder.append(" is unlocked - opening CE database");
                Log.i(str, stringBuilder.toString());
                synchronized (accounts.dbLock) {
                    synchronized (accounts.cacheLock) {
                        try {
                            accounts.accountsDb.attachCeDatabase(new File(this.mInjector.getCeDatabaseName(userId)));
                        } catch (SQLiteException e2) {
                            Log.e(TAG, "attachCeDatabase got err:", e2);
                        }
                    }
                }
                syncDeCeAccountsLocked(accounts);
            }
            if (validateAccounts) {
                validateAccountsInternal(accounts, true);
            }
        }
        return accounts;
    }

    private void syncDeCeAccountsLocked(UserAccounts accounts) {
        Preconditions.checkState(Thread.holdsLock(this.mUsers), "mUsers lock must be held");
        List<Account> accountsToRemove = accounts.accountsDb.findCeAccountsNotInDe();
        if (!accountsToRemove.isEmpty()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Accounts ");
            stringBuilder.append(accountsToRemove);
            stringBuilder.append(" were previously deleted while user ");
            stringBuilder.append(accounts.userId);
            stringBuilder.append(" was locked. Removing accounts from CE tables");
            Slog.i(str, stringBuilder.toString());
            logRecord(accounts, AccountsDb.DEBUG_ACTION_SYNC_DE_CE_ACCOUNTS, "accounts");
            for (Account account : accountsToRemove) {
                removeAccountInternal(accounts, account, 1000);
            }
        }
    }

    private void purgeOldGrantsAll() {
        synchronized (this.mUsers) {
            for (int i = 0; i < this.mUsers.size(); i++) {
                purgeOldGrants((UserAccounts) this.mUsers.valueAt(i));
            }
        }
    }

    private void purgeOldGrants(UserAccounts accounts) {
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                try {
                    for (Integer uid : accounts.accountsDb.findAllUidGrants()) {
                        int uid2 = uid.intValue();
                        if (!(this.mPackageManager.getPackagesForUid(uid2) != null)) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("deleting grants for UID ");
                            stringBuilder.append(uid2);
                            stringBuilder.append(" because its package is no longer installed");
                            Log.d(str, stringBuilder.toString());
                            accounts.accountsDb.deleteGrantsByUid(uid2);
                        }
                    }
                } catch (SQLiteException e) {
                    Log.e(TAG, "purgeOldGrants got err:", e);
                }
            }
        }
    }

    private void removeVisibilityValuesForPackage(String packageName) {
        if (!isSpecialPackageKey(packageName)) {
            synchronized (this.mUsers) {
                int numberOfUsers = this.mUsers.size();
                for (int i = 0; i < numberOfUsers; i++) {
                    UserAccounts accounts = (UserAccounts) this.mUsers.valueAt(i);
                    try {
                        this.mPackageManager.getPackageUidAsUser(packageName, accounts.userId);
                    } catch (NameNotFoundException e) {
                        accounts.accountsDb.deleteAccountVisibilityForPackage(packageName);
                        synchronized (accounts.dbLock) {
                            synchronized (accounts.cacheLock) {
                                for (Account account : accounts.visibilityCache.keySet()) {
                                    getPackagesAndVisibilityForAccountLocked(account, accounts).remove(packageName);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void purgeUserData(int userId) {
        UserAccounts accounts;
        synchronized (this.mUsers) {
            accounts = (UserAccounts) this.mUsers.get(userId);
            this.mUsers.remove(userId);
            this.mLocalUnlockedUsers.delete(userId);
        }
        if (accounts != null) {
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    accounts.statementForLogging.close();
                    accounts.accountsDb.close();
                }
            }
        }
    }

    @VisibleForTesting
    void onUserUnlocked(Intent intent) {
        onUnlockUser(intent.getIntExtra("android.intent.extra.user_handle", -1));
    }

    void onUnlockUser(int userId) {
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onUserUnlocked ");
            stringBuilder.append(userId);
            Log.v(str, stringBuilder.toString());
        }
        synchronized (this.mUsers) {
            this.mLocalUnlockedUsers.put(userId, true);
        }
        if (userId >= 1) {
            this.mHandler.post(new -$$Lambda$AccountManagerService$ncg6hlXg7I0Ee1EZqbXw8fQH9bY(this, userId));
        }
    }

    private void syncSharedAccounts(int userId) {
        try {
            Account[] sharedAccounts = getSharedAccountsAsUser(userId);
            if (sharedAccounts != null && sharedAccounts.length != 0) {
                int parentUserId;
                Account[] accounts = getAccountsAsUser(null, userId, this.mContext.getOpPackageName());
                int i = 0;
                if (UserManager.isSplitSystemUser()) {
                    parentUserId = getUserManager().getUserInfo(userId).restrictedProfileParentId;
                } else {
                    parentUserId = 0;
                }
                if (parentUserId < 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("User ");
                    stringBuilder.append(userId);
                    stringBuilder.append(" has shared accounts, but no parent user");
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                int length = sharedAccounts.length;
                while (i < length) {
                    Account sa = sharedAccounts[i];
                    if (!ArrayUtils.contains(accounts, sa)) {
                        copyAccountToUser(null, sa, parentUserId, userId);
                    }
                    i++;
                }
            }
        } catch (SQLiteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("syncSharedAccounts error userId: ");
            stringBuilder2.append(userId);
            Slog.e(str2, stringBuilder2.toString(), e);
        }
    }

    public void onServiceChanged(AuthenticatorDescription desc, int userId, boolean removed) {
        try {
            validateAccountsInternal(getUserAccounts(userId), false);
        } catch (SQLiteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceChanged userId: ");
            stringBuilder.append(userId);
            stringBuilder.append(", removed: ");
            stringBuilder.append(removed);
            Slog.e(str, stringBuilder.toString());
        }
    }

    public String getPassword(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPassword, caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        if (account != null) {
            int userId = UserHandle.getCallingUserId();
            if (isAccountManagedByCaller(account.type, callingUid, userId)) {
                long identityToken = clearCallingIdentity();
                try {
                    String readPasswordInternal = readPasswordInternal(getUserAccounts(userId), account);
                    return readPasswordInternal;
                } finally {
                    restoreCallingIdentity(identityToken);
                }
            } else {
                throw new SecurityException(String.format("uid %s cannot get secrets for accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
            }
        }
        throw new IllegalArgumentException("account is null");
    }

    private String readPasswordInternal(UserAccounts accounts, Account account) {
        if (account == null) {
            return null;
        }
        if (isLocalUnlockedUser(accounts.userId)) {
            String findAccountPasswordByNameAndType;
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    findAccountPasswordByNameAndType = accounts.accountsDb.findAccountPasswordByNameAndType(account.name, account.type);
                }
            }
            return findAccountPasswordByNameAndType;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Password is not available - user ");
        stringBuilder.append(accounts.userId);
        stringBuilder.append(" data is locked");
        Log.w(str, stringBuilder.toString());
        return null;
    }

    public String getPreviousName(Account account) {
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPreviousName, caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            String readPreviousNameInternal = readPreviousNameInternal(getUserAccounts(userId), account);
            return readPreviousNameInternal;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private String readPreviousNameInternal(UserAccounts accounts, Account account) {
        if (account == null) {
            return null;
        }
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                AtomicReference<String> previousNameRef = (AtomicReference) accounts.previousNameCache.get(account);
                String previousName;
                if (previousNameRef == null) {
                    previousName = accounts.accountsDb.findDeAccountPreviousName(account);
                    accounts.previousNameCache.put(account, new AtomicReference(previousName));
                    return previousName;
                }
                previousName = (String) previousNameRef.get();
                return previousName;
            }
        }
    }

    public String getUserData(Account account, String key) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, String.format("getUserData( callerUid: %s, pid: %s", new Object[]{Integer.valueOf(callingUid), Integer.valueOf(Binder.getCallingPid())}));
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(key, "key cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (isAccountManagedByCaller(account.type, callingUid, userId)) {
            String str = null;
            if (isLocalUnlockedUser(userId)) {
                long identityToken = clearCallingIdentity();
                try {
                    UserAccounts accounts = getUserAccounts(userId);
                    if (!accountExistsCache(accounts, account)) {
                        return str;
                    }
                    str = readUserDataInternal(accounts, account, key);
                    restoreCallingIdentity(identityToken);
                    return str;
                } finally {
                    restoreCallingIdentity(identityToken);
                }
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("User ");
                stringBuilder.append(userId);
                stringBuilder.append(" data is locked. callingUid ");
                stringBuilder.append(callingUid);
                Log.w(str2, stringBuilder.toString());
                return null;
            }
        }
        throw new SecurityException(String.format("uid %s cannot get user data for accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
    }

    public AuthenticatorDescription[] getAuthenticatorTypes(int userId) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAuthenticatorTypes: for user id ");
            stringBuilder.append(userId);
            stringBuilder.append(" caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s tying to get authenticator types for %s", new Object[]{Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)}));
        }
        long identityToken = clearCallingIdentity();
        try {
            AuthenticatorDescription[] authenticatorTypesInternal = getAuthenticatorTypesInternal(userId);
            return authenticatorTypesInternal;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private AuthenticatorDescription[] getAuthenticatorTypesInternal(int userId) {
        this.mAuthenticatorCache.updateServices(userId);
        Collection<ServiceInfo<AuthenticatorDescription>> authenticatorCollection = this.mAuthenticatorCache.getAllServices(userId);
        AuthenticatorDescription[] types = new AuthenticatorDescription[authenticatorCollection.size()];
        int i = 0;
        for (ServiceInfo<AuthenticatorDescription> authenticator : authenticatorCollection) {
            types[i] = (AuthenticatorDescription) authenticator.type;
            i++;
        }
        return types;
    }

    private boolean isCrossUser(int callingUid, int userId) {
        return (userId == UserHandle.getCallingUserId() || callingUid == 1000 || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) ? false : true;
    }

    public boolean addAccountExplicitly(Account account, String password, Bundle extras) {
        return addAccountExplicitlyWithVisibility(account, password, extras, null);
    }

    public void copyAccountToUser(IAccountManagerResponse response, Account account, int userFrom, int userTo) {
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        Account account2 = account;
        int i = userFrom;
        int i2 = userTo;
        int callingUid = Binder.getCallingUid();
        if (isCrossUser(callingUid, -1)) {
            throw new SecurityException("Calling copyAccountToUser requires android.permission.INTERACT_ACROSS_USERS_FULL");
        }
        UserAccounts fromAccounts = getUserAccounts(i);
        UserAccounts toAccounts = getUserAccounts(i2);
        int i3;
        if (fromAccounts == null) {
        } else if (toAccounts == null) {
            i3 = callingUid;
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Copying account  from user ");
            stringBuilder.append(i);
            stringBuilder.append(" to user ");
            stringBuilder.append(i2);
            Slog.d(str, stringBuilder.toString());
            long identityToken2 = clearCallingIdentity();
            try {
                String str2 = account2.type;
                AnonymousClass5 anonymousClass5 = anonymousClass5;
                UserAccounts userAccounts = fromAccounts;
                String str3 = account2.name;
                long identityToken3 = identityToken2;
                final Account account3 = account2;
                final IAccountManagerResponse iAccountManagerResponse2 = iAccountManagerResponse;
                final UserAccounts userAccounts2 = toAccounts;
                final int i4 = userFrom;
                try {
                    new Session(userAccounts, iAccountManagerResponse, str2, false, false, str3, false) {
                        protected String toDebugString(long now) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(super.toDebugString(now));
                            stringBuilder.append(", getAccountCredentialsForClone, ");
                            stringBuilder.append(account3.type);
                            return stringBuilder.toString();
                        }

                        public void run() throws RemoteException {
                            this.mAuthenticator.getAccountCredentialsForCloning(this, account3);
                        }

                        public void onResult(Bundle result) {
                            Bundle.setDefusable(result, true);
                            if (result == null || !result.getBoolean("booleanResult", false)) {
                                super.onResult(result);
                                return;
                            }
                            AccountManagerService.this.completeCloningAccount(iAccountManagerResponse2, result, account3, userAccounts2, i4);
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken3);
                    return;
                } catch (Throwable th2) {
                    th = th2;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken = identityToken2;
                i3 = callingUid;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        }
        if (iAccountManagerResponse != null) {
            Bundle result = new Bundle();
            result.putBoolean("booleanResult", false);
            try {
                iAccountManagerResponse.onResult(result);
            } catch (RemoteException e) {
                RemoteException remoteException = e;
                String str4 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to report error back to the client.");
                stringBuilder2.append(e);
                Slog.w(str4, stringBuilder2.toString());
            }
        }
    }

    public boolean accountAuthenticated(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, String.format("accountAuthenticated( callerUid: %s)", new Object[]{Integer.valueOf(callingUid)}));
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        int userId = UserHandle.getCallingUserId();
        boolean isAccountManagedByCaller = isAccountManagedByCaller(account.type, callingUid, userId);
        if (!isAccountManagedByCaller) {
            throw new SecurityException(String.format("uid %s cannot notify authentication for accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
        } else if (!canUserModifyAccounts(userId, callingUid) || !canUserModifyAccountsForType(userId, account.type, callingUid)) {
            return false;
        } else {
            long identityToken = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(userId);
                isAccountManagedByCaller = updateLastAuthenticatedTime(account);
                return isAccountManagedByCaller;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
    }

    private boolean updateLastAuthenticatedTime(Account account) {
        boolean updateAccountLastAuthenticatedTime;
        UserAccounts accounts = getUserAccountsForCaller();
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                updateAccountLastAuthenticatedTime = accounts.accountsDb.updateAccountLastAuthenticatedTime(account);
            }
        }
        return updateAccountLastAuthenticatedTime;
    }

    private void completeCloningAccount(IAccountManagerResponse response, Bundle accountCredentials, Account account, UserAccounts targetUser, int parentUserId) {
        Throwable th;
        long id;
        Account account2 = account;
        Bundle bundle = accountCredentials;
        Bundle.setDefusable(bundle, true);
        long id2 = clearCallingIdentity();
        try {
            AnonymousClass6 anonymousClass6 = anonymousClass6;
            final Account account3 = account2;
            long id3 = id2;
            final int i = parentUserId;
            final Bundle bundle2 = bundle;
            try {
                new Session(targetUser, response, account2.type, false, false, account2.name, false) {
                    protected String toDebugString(long now) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(super.toDebugString(now));
                        stringBuilder.append(", getAccountCredentialsForClone, ");
                        stringBuilder.append(account3.type);
                        return stringBuilder.toString();
                    }

                    public void run() throws RemoteException {
                        for (Account acc : AccountManagerService.this.getAccounts(i, AccountManagerService.this.mContext.getOpPackageName())) {
                            if (acc.equals(account3)) {
                                this.mAuthenticator.addAccountFromCredentials(this, account3, bundle2);
                                return;
                            }
                        }
                    }

                    public void onResult(Bundle result) {
                        Bundle.setDefusable(result, true);
                        super.onResult(result);
                    }

                    public void onError(int errorCode, String errorMessage) {
                        super.onError(errorCode, errorMessage);
                    }
                }.bind();
                restoreCallingIdentity(id3);
            } catch (Throwable th2) {
                th = th2;
                id = id3;
                restoreCallingIdentity(id);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            id = id2;
            restoreCallingIdentity(id);
            throw th;
        }
    }

    /* JADX WARNING: Missing block: B:77:0x01cb, code:
            if (getUserManager().getUserInfo(com.android.server.accounts.AccountManagerService.UserAccounts.access$800(r20)).canHaveProfile() == false) goto L_0x01d4;
     */
    /* JADX WARNING: Missing block: B:78:0x01cd, code:
            addAccountToLinkedRestrictedUsers(r10, com.android.server.accounts.AccountManagerService.UserAccounts.access$800(r20));
     */
    /* JADX WARNING: Missing block: B:79:0x01d4, code:
            sendNotificationAccountUpdated(r10, r9);
            sendAccountsChangedBroadcast(com.android.server.accounts.AccountManagerService.UserAccounts.access$800(r20));
     */
    /* JADX WARNING: Missing block: B:80:0x01df, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean addAccountInternal(UserAccounts accounts, Account account, String password, Bundle extras, int callingUid, Map<String, Integer> packageToVisibility) {
        Throwable th;
        String str;
        UserAccounts userAccounts = accounts;
        Account account2 = account;
        Bundle bundle = extras;
        int i = callingUid;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(i, 0, BehaviorId.ACCOUNTMANAGER_ADDACCOUNTINTERNAL);
        Bundle.setDefusable(bundle, true);
        if (account2 == null) {
            return false;
        }
        String str2;
        StringBuilder stringBuilder;
        if (isLocalUnlockedUser(accounts.userId)) {
            synchronized (userAccounts.dbLock) {
                try {
                    synchronized (userAccounts.cacheLock) {
                        try {
                            userAccounts.accountsDb.beginTransaction();
                            try {
                                if (userAccounts.accountsDb.findCeAccountId(account2) >= 0) {
                                    str2 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("insertAccountIntoDatabase: ");
                                    stringBuilder.append(account2);
                                    stringBuilder.append(", skipping since the account already exists");
                                    Log.w(str2, stringBuilder.toString());
                                    userAccounts.accountsDb.endTransaction();
                                    return false;
                                }
                                try {
                                    long accountId = userAccounts.accountsDb.insertCeAccount(account2, password);
                                    if (accountId < 0) {
                                        str2 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("insertAccountIntoDatabase: ");
                                        stringBuilder.append(account2);
                                        stringBuilder.append(", skipping the DB insert failed");
                                        Log.w(str2, stringBuilder.toString());
                                        userAccounts.accountsDb.endTransaction();
                                        return false;
                                    }
                                    String str3 = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("insert CE accountId = ");
                                    stringBuilder2.append(accountId);
                                    Log.e(str3, stringBuilder2.toString());
                                    if (userAccounts.accountsDb.insertDeAccount(account2, accountId) < 0) {
                                        str2 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("insertAccountIntoDatabase: ");
                                        stringBuilder.append(account2);
                                        stringBuilder.append(", skipping the DB insert failed");
                                        Log.w(str2, stringBuilder.toString());
                                        userAccounts.accountsDb.endTransaction();
                                        return false;
                                    }
                                    long accountId2;
                                    str2 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("insert DE accountId = ");
                                    stringBuilder.append(accountId);
                                    Log.e(str2, stringBuilder.toString());
                                    if (bundle != null) {
                                        for (String str32 : extras.keySet()) {
                                            if (userAccounts.accountsDb.insertExtra(accountId, str32, bundle.getString(str32)) < 0) {
                                                str2 = TAG;
                                                StringBuilder stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("insertAccountIntoDatabase: ");
                                                stringBuilder3.append(account2);
                                                stringBuilder3.append(", skipping since insertExtra failed for key ");
                                                stringBuilder3.append(str32);
                                                Log.w(str2, stringBuilder3.toString());
                                                userAccounts.accountsDb.endTransaction();
                                                return false;
                                            }
                                        }
                                    }
                                    if (packageToVisibility != null) {
                                        for (Entry<String, Integer> entry : packageToVisibility.entrySet()) {
                                            accountId2 = accountId;
                                            setAccountVisibility(account2, (String) entry.getKey(), ((Integer) entry.getValue()).intValue(), false, userAccounts);
                                            accountId = accountId2;
                                        }
                                    }
                                    accountId2 = accountId;
                                    userAccounts.accountsDb.setTransactionSuccessful();
                                    logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_ADD, "accounts", accountId2, userAccounts, i);
                                    insertAccountIntoCacheLocked(accounts, account);
                                    userAccounts.accountsDb.endTransaction();
                                } catch (Throwable th2) {
                                    th = th2;
                                    userAccounts.accountsDb.endTransaction();
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                str = password;
                                userAccounts.accountsDb.endTransaction();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            throw th;
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                    throw th;
                }
            }
        }
        str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Account ");
        stringBuilder.append(account2);
        stringBuilder.append(" cannot be added - user ");
        stringBuilder.append(accounts.userId);
        stringBuilder.append(" is locked. callingUid=");
        stringBuilder.append(i);
        Log.w(str2, stringBuilder.toString());
        return false;
    }

    private boolean isLocalUnlockedUser(int userId) {
        boolean z;
        synchronized (this.mUsers) {
            z = this.mLocalUnlockedUsers.get(userId);
        }
        return z;
    }

    private void addAccountToLinkedRestrictedUsers(Account account, int parentUserId) {
        for (UserInfo user : getUserManager().getUsers()) {
            if (user.isRestricted() && parentUserId == user.restrictedProfileParentId) {
                addSharedAccountAsUser(account, user.id);
                if (isLocalUnlockedUser(user.id)) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(4, parentUserId, user.id, account));
                }
            }
        }
    }

    public void hasFeatures(IAccountManagerResponse response, Account account, String[] features, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hasFeatures, response ");
            stringBuilder.append(response);
            stringBuilder.append(", features ");
            stringBuilder.append(Arrays.toString(features));
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        boolean z = false;
        Preconditions.checkArgument(account != null, "account cannot be null");
        Preconditions.checkArgument(response != null, "response cannot be null");
        if (features != null) {
            z = true;
        }
        Preconditions.checkArgument(z, "features cannot be null");
        int userId = UserHandle.getCallingUserId();
        checkReadAccountsPermitted(callingUid, account.type, userId, opPackageName);
        long identityToken = clearCallingIdentity();
        try {
            new TestFeaturesSession(getUserAccounts(userId), response, account, features).bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void renameAccount(IAccountManagerResponse response, Account accountToRename, String newName) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("renameAccount, caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        if (accountToRename != null) {
            int userId = UserHandle.getCallingUserId();
            if (isAccountManagedByCaller(accountToRename.type, callingUid, userId)) {
                long identityToken = clearCallingIdentity();
                try {
                    Account resultingAccount = renameAccountInternal(getUserAccounts(userId), accountToRename, newName);
                    Bundle result = new Bundle();
                    result.putString("authAccount", resultingAccount.name);
                    result.putString("accountType", resultingAccount.type);
                    result.putString("accountAccessId", resultingAccount.getAccessId());
                    response.onResult(result);
                } catch (RemoteException e) {
                    Log.w(TAG, e.getMessage());
                } catch (Throwable th) {
                    restoreCallingIdentity(identityToken);
                }
                restoreCallingIdentity(identityToken);
                return;
            }
            throw new SecurityException(String.format("uid %s cannot rename accounts of type: %s", new Object[]{Integer.valueOf(callingUid), accountToRename.type}));
        }
        throw new IllegalArgumentException("account is null");
    }

    /* JADX WARNING: Missing block: B:27:0x0091, code:
            return r10;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Account renameAccountInternal(UserAccounts accounts, Account accountToRename, String newName) {
        Account account;
        UserAccounts userAccounts = accounts;
        Account account2 = accountToRename;
        String str = newName;
        cancelNotification(getSigninRequiredNotificationId(accounts, accountToRename), new UserHandle(accounts.userId));
        synchronized (accounts.credentialsPermissionNotificationIds) {
            for (Pair<Pair<Account, String>, Integer> pair : accounts.credentialsPermissionNotificationIds.keySet()) {
                if (account2.equals(((Pair) pair.first).first)) {
                    cancelNotification((NotificationId) accounts.credentialsPermissionNotificationIds.get(pair), new UserHandle(accounts.userId));
                }
            }
        }
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                List<String> accountRemovedReceivers = getAccountRemovedReceivers(account2, userAccounts);
                userAccounts.accountsDb.beginTransaction();
                Account renamedAccount = new Account(str, account2.type);
                try {
                    account = null;
                    if (userAccounts.accountsDb.findCeAccountId(renamedAccount) >= 0) {
                        Log.e(TAG, "renameAccount failed - account with new name already exists");
                    } else {
                        long accountId = userAccounts.accountsDb.findDeAccountId(account2);
                        if (accountId >= 0) {
                            userAccounts.accountsDb.renameCeAccount(accountId, str);
                            if (userAccounts.accountsDb.renameDeAccount(accountId, str, account2.name)) {
                                userAccounts.accountsDb.setTransactionSuccessful();
                                userAccounts.accountsDb.endTransaction();
                                Account renamedAccount2 = insertAccountIntoCacheLocked(userAccounts, renamedAccount);
                                Map renamedAccount3 = (Map) accounts.userDataCache.get(account2);
                                Map<String, String> tmpTokens = (Map) accounts.authTokenCache.get(account2);
                                Map<String, Integer> tmpVisibility = (Map) accounts.visibilityCache.get(account2);
                                removeAccountFromCacheLocked(accounts, accountToRename);
                                accounts.userDataCache.put(renamedAccount2, renamedAccount3);
                                accounts.authTokenCache.put(renamedAccount2, tmpTokens);
                                accounts.visibilityCache.put(renamedAccount2, tmpVisibility);
                                accounts.previousNameCache.put(renamedAccount2, new AtomicReference(account2.name));
                                Account resultAccount = renamedAccount2;
                                int parentUserId = accounts.userId;
                                if (canHaveProfile(parentUserId)) {
                                    for (UserInfo user : getUserManager().getUsers(true)) {
                                        Account renamedAccount4;
                                        if (user.isRestricted()) {
                                            renamedAccount4 = renamedAccount2;
                                            if (user.restrictedProfileParentId == parentUserId) {
                                                renameSharedAccountAsUser(account2, str, user.id);
                                            }
                                        } else {
                                            renamedAccount4 = renamedAccount2;
                                        }
                                        renamedAccount2 = renamedAccount4;
                                    }
                                }
                                sendNotificationAccountUpdated(resultAccount, userAccounts);
                                sendAccountsChangedBroadcast(accounts.userId);
                                for (String packageName : accountRemovedReceivers) {
                                    sendAccountRemovedBroadcast(account2, packageName, accounts.userId);
                                }
                                return resultAccount;
                            }
                            Log.e(TAG, "renameAccount failed");
                            userAccounts.accountsDb.endTransaction();
                            return null;
                        }
                        Log.e(TAG, "renameAccount failed - old account does not exist");
                        userAccounts.accountsDb.endTransaction();
                        return null;
                    }
                } finally {
                    account = userAccounts.accountsDb;
                    account.endTransaction();
                }
            }
        }
    }

    private boolean canHaveProfile(int parentUserId) {
        UserInfo userInfo = getUserManager().getUserInfo(parentUserId);
        return userInfo != null && userInfo.canHaveProfile();
    }

    public void removeAccount(IAccountManagerResponse response, Account account, boolean expectActivityLaunch) {
        removeAccountAsUser(response, account, expectActivityLaunch, UserHandle.getCallingUserId());
    }

    /* JADX WARNING: Missing block: B:50:0x0108, code:
            r18 = r7;
            logRecord(com.android.server.accounts.AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_REMOVE, "accounts", r7.accountsDb.findDeAccountId(r10), r7, r12);
     */
    /* JADX WARNING: Missing block: B:52:?, code:
            new com.android.server.accounts.AccountManagerService.RemoveAccountSession(r8, r18, r9, r10, r22).bind();
     */
    /* JADX WARNING: Missing block: B:55:0x0132, code:
            restoreCallingIdentity(r14);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeAccountAsUser(IAccountManagerResponse response, Account account, boolean expectActivityLaunch, int userId) {
        Throwable th;
        UserAccounts userAccounts;
        IAccountManagerResponse iAccountManagerResponse = response;
        Account account2 = account;
        int i = userId;
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeAccount, response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", for user id ");
            stringBuilder.append(i);
            Log.v(str, stringBuilder.toString());
        }
        Preconditions.checkArgument(account2 != null, "account cannot be null");
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        if (isCrossUser(callingUid, i)) {
            throw new SecurityException(String.format("User %s tying remove account for %s", new Object[]{Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)}));
        }
        UserHandle user = UserHandle.of(userId);
        if (!isAccountManagedByCaller(account2.type, callingUid, user.getIdentifier()) && !isSystemUid(callingUid) && !isProfileOwner(callingUid)) {
            throw new SecurityException(String.format("uid %s cannot remove accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account2.type}));
        } else if (!canUserModifyAccounts(i, callingUid)) {
            try {
                iAccountManagerResponse.onError(100, "User cannot modify accounts");
            } catch (RemoteException e) {
            }
        } else if (canUserModifyAccountsForType(i, account2.type, callingUid)) {
            long identityToken = clearCallingIdentity();
            UserAccounts accounts = getUserAccounts(i);
            cancelNotification(getSigninRequiredNotificationId(accounts, account2), user);
            synchronized (accounts.credentialsPermissionNotificationIds) {
                try {
                    for (Pair<Pair<Account, String>, Integer> pair : accounts.credentialsPermissionNotificationIds.keySet()) {
                        try {
                            if (account2.equals(((Pair) pair.first).first)) {
                                cancelNotification((NotificationId) accounts.credentialsPermissionNotificationIds.get(pair), user);
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            userAccounts = accounts;
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
                    userAccounts = accounts;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        } else {
            try {
                iAccountManagerResponse.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
        }
    }

    public boolean removeAccountExplicitly(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeAccountExplicitly, caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        int userId = Binder.getCallingUserHandle().getIdentifier();
        if (account == null) {
            Log.e(TAG, "account is null");
            return false;
        } else if (isAccountManagedByCaller(account.type, callingUid, userId)) {
            UserAccounts accounts = getUserAccountsForCaller();
            logRecord(AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_REMOVE, "accounts", accounts.accountsDb.findDeAccountId(account), accounts, callingUid);
            long identityToken = clearCallingIdentity();
            try {
                boolean removeAccountInternal = removeAccountInternal(accounts, account, callingUid);
                return removeAccountInternal;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new SecurityException(String.format("uid %s cannot explicitly remove accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
        }
    }

    @VisibleForTesting
    protected void removeAccountInternal(Account account) {
        removeAccountInternal(getUserAccountsForCaller(), account, getCallingUid());
    }

    /* JADX WARNING: Missing block: B:52:0x0107, code:
            r1 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:54:?, code:
            r3 = com.android.server.accounts.AccountManagerService.UserAccounts.access$800(r20);
     */
    /* JADX WARNING: Missing block: B:55:0x0113, code:
            if (canHaveProfile(r3) == false) goto L_0x013d;
     */
    /* JADX WARNING: Missing block: B:56:0x0115, code:
            r4 = getUserManager().getUsers(true).iterator();
     */
    /* JADX WARNING: Missing block: B:58:0x0125, code:
            if (r4.hasNext() == false) goto L_0x013d;
     */
    /* JADX WARNING: Missing block: B:59:0x0127, code:
            r5 = (android.content.pm.UserInfo) r4.next();
     */
    /* JADX WARNING: Missing block: B:60:0x0131, code:
            if (r5.isRestricted() == false) goto L_0x013c;
     */
    /* JADX WARNING: Missing block: B:62:0x0135, code:
            if (r3 != r5.restrictedProfileParentId) goto L_0x013c;
     */
    /* JADX WARNING: Missing block: B:63:0x0137, code:
            removeSharedAccountAsUser(r9, r5.id, r10);
     */
    /* JADX WARNING: Missing block: B:65:0x013d, code:
            android.os.Binder.restoreCallingIdentity(r1);
     */
    /* JADX WARNING: Missing block: B:66:0x0141, code:
            if (r16 == false) goto L_0x0192;
     */
    /* JADX WARNING: Missing block: B:67:0x0143, code:
            r3 = com.android.server.accounts.AccountManagerService.UserAccounts.access$1700(r20);
     */
    /* JADX WARNING: Missing block: B:68:0x0147, code:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:70:?, code:
            r0 = com.android.server.accounts.AccountManagerService.UserAccounts.access$1700(r20).keySet().iterator();
     */
    /* JADX WARNING: Missing block: B:72:0x0158, code:
            if (r0.hasNext() == false) goto L_0x018d;
     */
    /* JADX WARNING: Missing block: B:73:0x015a, code:
            r4 = (android.util.Pair) r0.next();
     */
    /* JADX WARNING: Missing block: B:74:0x016a, code:
            if (r9.equals(((android.util.Pair) r4.first).first) == false) goto L_0x018c;
     */
    /* JADX WARNING: Missing block: B:76:0x0178, code:
            if ("com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE".equals(((android.util.Pair) r4.first).second) == false) goto L_0x018c;
     */
    /* JADX WARNING: Missing block: B:77:0x017a, code:
            r7.mHandler.post(new com.android.server.accounts.-$$Lambda$AccountManagerService$lqbNdAUKUSipmpqby9oIO8JlNTQ(r7, r9, ((java.lang.Integer) r4.second).intValue()));
     */
    /* JADX WARNING: Missing block: B:79:0x018d, code:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:83:0x0192, code:
            return r16;
     */
    /* JADX WARNING: Missing block: B:85:0x0194, code:
            android.os.Binder.restoreCallingIdentity(r1);
     */
    /* JADX WARNING: Missing block: B:87:0x0198, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:88:0x0199, code:
            r1 = r16;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean removeAccountInternal(UserAccounts accounts, Account account, int callingUid) {
        Throwable th;
        UserAccounts userAccounts = accounts;
        Account account2 = account;
        int i = callingUid;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(i, 0, BehaviorId.ACCOUNTMANAGER_REMOVEACCOUNTINTERNAL);
        boolean isChanged = false;
        boolean userUnlocked = isLocalUnlockedUser(accounts.userId);
        if (!userUnlocked) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removing account ");
            stringBuilder.append(account2);
            stringBuilder.append(" while user ");
            stringBuilder.append(accounts.userId);
            stringBuilder.append(" is still locked. CE data will be removed later");
            Slog.i(str, stringBuilder.toString());
        }
        synchronized (userAccounts.dbLock) {
            try {
                synchronized (userAccounts.cacheLock) {
                    try {
                        long accountId;
                        boolean isChanged2;
                        Map<String, Integer> packagesToVisibility = getRequestingPackages(account2, userAccounts);
                        List<String> accountRemovedReceivers = getAccountRemovedReceivers(account2, userAccounts);
                        userAccounts.accountsDb.beginTransaction();
                        try {
                            accountId = userAccounts.accountsDb.findDeAccountId(account2);
                            if (accountId >= 0) {
                                try {
                                    isChanged = userAccounts.accountsDb.deleteDeAccount(accountId);
                                } catch (Throwable th2) {
                                    th = th2;
                                    isChanged2 = false;
                                    userAccounts.accountsDb.endTransaction();
                                    throw th;
                                }
                            }
                            isChanged2 = isChanged;
                            if (userUnlocked) {
                                try {
                                    long ceAccountId = userAccounts.accountsDb.findCeAccountId(account2);
                                    if (ceAccountId >= 0) {
                                        userAccounts.accountsDb.deleteCeAccount(ceAccountId);
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    userAccounts.accountsDb.endTransaction();
                                    throw th;
                                }
                            }
                            try {
                                userAccounts.accountsDb.setTransactionSuccessful();
                            } catch (Throwable th4) {
                                th = th4;
                                long j = accountId;
                                userAccounts.accountsDb.endTransaction();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            isChanged2 = false;
                            accountId = -1;
                            userAccounts.accountsDb.endTransaction();
                            throw th;
                        }
                        try {
                            userAccounts.accountsDb.endTransaction();
                            if (isChanged2) {
                                String str2;
                                removeAccountFromCacheLocked(accounts, account);
                                for (Entry<String, Integer> packageToVisibility : packagesToVisibility.entrySet()) {
                                    if (((Integer) packageToVisibility.getValue()).intValue() == 1 || ((Integer) packageToVisibility.getValue()).intValue() == 2) {
                                        notifyPackage((String) packageToVisibility.getKey(), userAccounts);
                                    }
                                }
                                sendAccountsChangedBroadcast(accounts.userId);
                                for (String packageName : accountRemovedReceivers) {
                                    sendAccountRemovedBroadcast(account2, packageName, accounts.userId);
                                }
                                if (userUnlocked) {
                                    str2 = AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE;
                                } else {
                                    str2 = AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE_DE;
                                }
                                logRecord(str2, "accounts", accountId, userAccounts);
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            isChanged = isChanged2;
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        throw th;
                    }
                }
            } catch (Throwable th8) {
                th = th8;
                throw th;
            }
        }
    }

    public void invalidateAuthToken(String accountType, String authToken) {
        int callerUid = Binder.getCallingUid();
        Preconditions.checkNotNull(accountType, "accountType cannot be null");
        Preconditions.checkNotNull(authToken, "authToken cannot be null");
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalidateAuthToken , caller's uid ");
            stringBuilder.append(callerUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            synchronized (accounts.dbLock) {
                accounts.accountsDb.beginTransaction();
                try {
                    List<Pair<Account, String>> deletedTokens = invalidateAuthTokenLocked(accounts, accountType, authToken);
                    accounts.accountsDb.setTransactionSuccessful();
                    accounts.accountsDb.endTransaction();
                    synchronized (accounts.cacheLock) {
                        for (Pair<Account, String> tokenInfo : deletedTokens) {
                            writeAuthTokenIntoCacheLocked(accounts, tokenInfo.first, tokenInfo.second, null);
                        }
                        accounts.accountTokenCaches.remove(accountType, authToken);
                    }
                } catch (Throwable th) {
                    accounts.accountsDb.endTransaction();
                }
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private List<Pair<Account, String>> invalidateAuthTokenLocked(UserAccounts accounts, String accountType, String authToken) {
        List<Pair<Account, String>> results = new ArrayList();
        Cursor cursor = accounts.accountsDb.findAuthtokenForAllAccounts(accountType, authToken);
        while (cursor.moveToNext()) {
            try {
                String authTokenId = cursor.getString(null);
                String accountName = cursor.getString(1);
                String authTokenType = cursor.getString(2);
                accounts.accountsDb.deleteAuthToken(authTokenId);
                results.add(Pair.create(new Account(accountName, accountType), authTokenType));
            } finally {
                cursor.close();
            }
        }
        return results;
    }

    private void saveCachedToken(UserAccounts accounts, Account account, String callerPkg, byte[] callerSigDigest, String tokenType, String token, long expiryMillis) {
        UserAccounts userAccounts;
        if (account == null || tokenType == null || callerPkg == null || callerSigDigest == null) {
            userAccounts = accounts;
            return;
        }
        cancelNotification(getSigninRequiredNotificationId(accounts, account), UserHandle.of(accounts.userId));
        userAccounts = accounts;
        synchronized (userAccounts.cacheLock) {
            userAccounts.accountTokenCaches.put(account, token, tokenType, callerPkg, callerSigDigest, expiryMillis);
        }
    }

    /* JADX WARNING: Missing block: B:25:0x003f, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:44:0x0069, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean saveAuthTokenToDatabase(UserAccounts accounts, Account account, String type, String authToken) {
        Throwable th;
        if (account == null || type == null) {
            return false;
        }
        cancelNotification(getSigninRequiredNotificationId(accounts, account), UserHandle.of(accounts.userId));
        synchronized (accounts.dbLock) {
            accounts.accountsDb.beginTransaction();
            boolean updateCache = false;
            try {
                long accountId = accounts.accountsDb.findDeAccountId(account);
                if (accountId < 0) {
                    accounts.accountsDb.endTransaction();
                    if (updateCache) {
                        synchronized (accounts.cacheLock) {
                            try {
                                writeAuthTokenIntoCacheLocked(accounts, account, type, authToken);
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                    }
                }
                accounts.accountsDb.deleteAuthtokensByAccountIdAndType(accountId, type);
                if (accounts.accountsDb.insertAuthToken(accountId, type, authToken) >= 0) {
                    accounts.accountsDb.setTransactionSuccessful();
                    accounts.accountsDb.endTransaction();
                    if (true) {
                        synchronized (accounts.cacheLock) {
                            writeAuthTokenIntoCacheLocked(accounts, account, type, authToken);
                        }
                    }
                } else {
                    accounts.accountsDb.endTransaction();
                    if (updateCache) {
                        synchronized (accounts.cacheLock) {
                            try {
                                writeAuthTokenIntoCacheLocked(accounts, account, type, authToken);
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        }
                    }
                    return false;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    public String peekAuthToken(Account account, String authTokenType) {
        StringBuilder stringBuilder;
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("peekAuthToken , authTokenType ");
            stringBuilder.append(authTokenType);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(authTokenType, "authTokenType cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            throw new SecurityException(String.format("uid %s cannot peek the authtokens associated with accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
        } else if (isLocalUnlockedUser(userId)) {
            long identityToken = clearCallingIdentity();
            try {
                String readAuthTokenInternal = readAuthTokenInternal(getUserAccounts(userId), account, authTokenType);
                return readAuthTokenInternal;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Authtoken not available - user ");
            stringBuilder.append(userId);
            stringBuilder.append(" data is locked. callingUid ");
            stringBuilder.append(callingUid);
            Log.w(str2, stringBuilder.toString());
            return null;
        }
    }

    public void setAuthToken(Account account, String authTokenType, String authToken) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAuthToken , authTokenType ");
            stringBuilder.append(authTokenType);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(authTokenType, "authTokenType cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (isAccountManagedByCaller(account.type, callingUid, userId)) {
            long identityToken = clearCallingIdentity();
            try {
                saveAuthTokenToDatabase(getUserAccounts(userId), account, authTokenType, authToken);
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new SecurityException(String.format("uid %s cannot set auth tokens associated with accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
        }
    }

    public void setPassword(Account account, String password) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAuthToken , caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (isAccountManagedByCaller(account.type, callingUid, userId)) {
            long identityToken = clearCallingIdentity();
            try {
                setPasswordInternal(getUserAccounts(userId), account, password, callingUid);
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new SecurityException(String.format("uid %s cannot set secrets for accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x007e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setPasswordInternal(UserAccounts accounts, Account account, String password, int callingUid) {
        Throwable th;
        UserAccounts userAccounts = accounts;
        Account account2 = account;
        String str = password;
        if (account2 != null) {
            boolean isChanged = false;
            synchronized (userAccounts.dbLock) {
                synchronized (userAccounts.cacheLock) {
                    userAccounts.accountsDb.beginTransaction();
                    try {
                        long accountId = userAccounts.accountsDb.findDeAccountId(account2);
                        if (accountId >= 0) {
                            String str2;
                            userAccounts.accountsDb.updateCeAccountPassword(accountId, str);
                            userAccounts.accountsDb.deleteAuthTokensByAccountId(accountId);
                            accounts.authTokenCache.remove(account2);
                            accounts.accountTokenCaches.remove(account2);
                            userAccounts.accountsDb.setTransactionSuccessful();
                            if (str != null) {
                                try {
                                    if (password.length() != 0) {
                                        str2 = AccountsDb.DEBUG_ACTION_SET_PASSWORD;
                                        logRecord(str2, "accounts", accountId, userAccounts, callingUid);
                                        isChanged = true;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    isChanged = true;
                                    userAccounts.accountsDb.endTransaction();
                                    if (isChanged) {
                                    }
                                    throw th;
                                }
                            }
                            str2 = AccountsDb.DEBUG_ACTION_CLEAR_PASSWORD;
                            logRecord(str2, "accounts", accountId, userAccounts, callingUid);
                            isChanged = true;
                        }
                        userAccounts.accountsDb.endTransaction();
                        if (isChanged) {
                            sendNotificationAccountUpdated(account2, userAccounts);
                            sendAccountsChangedBroadcast(accounts.userId);
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        userAccounts.accountsDb.endTransaction();
                        if (isChanged) {
                            sendNotificationAccountUpdated(account2, userAccounts);
                            sendAccountsChangedBroadcast(accounts.userId);
                        }
                        throw th;
                    }
                }
            }
        }
    }

    public void clearPassword(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("clearPassword , caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (isAccountManagedByCaller(account.type, callingUid, userId)) {
            long identityToken = clearCallingIdentity();
            try {
                setPasswordInternal(getUserAccounts(userId), account, null, callingUid);
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            throw new SecurityException(String.format("uid %s cannot clear passwords for accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
        }
    }

    public void setUserData(Account account, String key, String value) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setUserData: ");
            stringBuilder.append(account);
            stringBuilder.append(", key ");
            stringBuilder.append(key);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        } else if (account != null) {
            int userId = UserHandle.getCallingUserId();
            if (isAccountManagedByCaller(account.type, callingUid, userId)) {
                long identityToken = clearCallingIdentity();
                try {
                    UserAccounts accounts = getUserAccounts(userId);
                    if (accountExistsCache(accounts, account)) {
                        setUserdataInternal(accounts, account, key, value);
                        restoreCallingIdentity(identityToken);
                    }
                } finally {
                    restoreCallingIdentity(identityToken);
                }
            } else {
                throw new SecurityException(String.format("uid %s cannot set user data for accounts of type: %s", new Object[]{Integer.valueOf(callingUid), account.type}));
            }
        } else {
            throw new IllegalArgumentException("account is null");
        }
    }

    /* JADX WARNING: Missing block: B:14:0x002f, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean accountExistsCache(UserAccounts accounts, Account account) {
        synchronized (accounts.cacheLock) {
            if (accounts.accountCache.containsKey(account.type)) {
                for (Account acc : (Account[]) accounts.accountCache.get(account.type)) {
                    if (acc.name.equals(account.name)) {
                        return true;
                    }
                }
            }
        }
    }

    private void setUserdataInternal(UserAccounts accounts, Account account, String key, String value) {
        synchronized (accounts.dbLock) {
            accounts.accountsDb.beginTransaction();
            try {
                long accountId = accounts.accountsDb.findDeAccountId(account);
                if (accountId < 0) {
                    accounts.accountsDb.endTransaction();
                    return;
                }
                long extrasId = accounts.accountsDb.findExtrasIdByAccountId(accountId, key);
                if (extrasId < 0) {
                    if (accounts.accountsDb.insertExtra(accountId, key, value) < 0) {
                        accounts.accountsDb.endTransaction();
                        return;
                    }
                } else if (!accounts.accountsDb.updateExtra(extrasId, value)) {
                    accounts.accountsDb.endTransaction();
                    return;
                }
                accounts.accountsDb.setTransactionSuccessful();
                accounts.accountsDb.endTransaction();
                synchronized (accounts.cacheLock) {
                    writeUserDataIntoCacheLocked(accounts, account, key, value);
                }
            } catch (Throwable th) {
                accounts.accountsDb.endTransaction();
            }
        }
    }

    private void onResult(IAccountManagerResponse response, Bundle result) {
        if (result == null) {
            Log.e(TAG, "the result is unexpectedly null", new Exception());
        }
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getClass().getSimpleName());
            stringBuilder.append(" calling onResult() on response ");
            stringBuilder.append(response);
            Log.v(str, stringBuilder.toString());
        }
        try {
            response.onResult(result);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }

    public void getAuthTokenLabel(IAccountManagerResponse response, String accountType, String authTokenType) throws RemoteException {
        Throwable th;
        long identityToken;
        boolean z = false;
        Preconditions.checkArgument(accountType != null, "accountType cannot be null");
        if (authTokenType != null) {
            z = true;
        }
        Preconditions.checkArgument(z, "authTokenType cannot be null");
        int callingUid = getCallingUid();
        clearCallingIdentity();
        if (UserHandle.getAppId(callingUid) == 1000) {
            int userId = UserHandle.getUserId(callingUid);
            long identityToken2 = clearCallingIdentity();
            try {
                AnonymousClass7 anonymousClass7 = anonymousClass7;
                long identityToken3 = identityToken2;
                final String str = accountType;
                final String str2 = authTokenType;
                try {
                    new Session(getUserAccounts(userId), response, accountType, false, false, null, false) {
                        protected String toDebugString(long now) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(super.toDebugString(now));
                            stringBuilder.append(", getAuthTokenLabel, ");
                            stringBuilder.append(str);
                            stringBuilder.append(", authTokenType ");
                            stringBuilder.append(str2);
                            return stringBuilder.toString();
                        }

                        public void run() throws RemoteException {
                            this.mAuthenticator.getAuthTokenLabel(this, str2);
                        }

                        public void onResult(Bundle result) {
                            Bundle.setDefusable(result, true);
                            if (result != null) {
                                String label = result.getString("authTokenLabelKey");
                                Bundle bundle = new Bundle();
                                bundle.putString("authTokenLabelKey", label);
                                super.onResult(bundle);
                                return;
                            }
                            super.onResult(result);
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken3);
                    return;
                } catch (Throwable th2) {
                    th = th2;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken = identityToken2;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        }
        throw new SecurityException("can only call from system");
    }

    public void getAuthToken(IAccountManagerResponse response, Account account, String authTokenType, boolean notifyOnAuthFailure, boolean expectActivityLaunch, Bundle loginOptions) {
        StringBuilder stringBuilder;
        int i;
        boolean z;
        boolean customTokens;
        boolean z2;
        String callerPkg;
        List<String> callerOwnedPackageNames;
        long j;
        ServiceInfo<AuthenticatorDescription> serviceInfo;
        UserAccounts userAccounts;
        List<String> accounts;
        String callerPkg2;
        long ident;
        Throwable th;
        IAccountManagerResponse iAccountManagerResponse = response;
        Account account2 = account;
        String str = authTokenType;
        boolean z3 = notifyOnAuthFailure;
        Bundle bundle = loginOptions;
        Bundle.setDefusable(bundle, true);
        int isLoggable = Log.isLoggable(TAG, 2);
        if (isLoggable != null) {
            isLoggable = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getAuthToken , response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", authTokenType ");
            stringBuilder.append(str);
            stringBuilder.append(", notifyOnAuthFailure ");
            stringBuilder.append(z3);
            stringBuilder.append(", expectActivityLaunch ");
            stringBuilder.append(expectActivityLaunch);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(isLoggable, stringBuilder.toString());
        } else {
            boolean z4 = expectActivityLaunch;
        }
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        if (account2 == null) {
            try {
                Slog.w(TAG, "getAuthToken called with null account");
                iAccountManagerResponse.onError(7, "account is null");
            } catch (RemoteException e) {
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to report error back to the client.");
                stringBuilder.append(e);
                Slog.w(str2, stringBuilder.toString());
            }
        } else if (str == null) {
            Slog.w(TAG, "getAuthToken called with null authTokenType");
            iAccountManagerResponse.onError(7, "authTokenType is null");
        } else {
            UserAccounts accounts2;
            ServiceInfo<AuthenticatorDescription> authenticatorInfo;
            boolean permissionGranted;
            int userId = UserHandle.getCallingUserId();
            long ident2 = Binder.clearCallingIdentity();
            try {
                accounts2 = getUserAccounts(userId);
                authenticatorInfo = this.mAuthenticatorCache;
                isLoggable = authenticatorInfo.getServiceInfo(AuthenticatorDescription.newKey(account2.type), accounts2.userId);
            } finally {
                userId = 
/*
Method generation error in method: com.android.server.accounts.AccountManagerService.getAuthToken(android.accounts.IAccountManagerResponse, android.accounts.Account, java.lang.String, boolean, boolean, android.os.Bundle):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r8_3 'userId' int) = (r8_0 'userId' int), (r1_35 'isLoggable' int) in method: com.android.server.accounts.AccountManagerService.getAuthToken(android.accounts.IAccountManagerResponse, android.accounts.Account, java.lang.String, boolean, boolean, android.os.Bundle):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:205)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:100)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:50)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:298)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:128)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:143)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:124)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:57)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:173)
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
	... 28 more

*/

    private byte[] calculatePackageSignatureDigest(String callerPkg) {
        MessageDigest digester;
        try {
            digester = MessageDigest.getInstance("SHA-256");
            for (Signature sig : this.mPackageManager.getPackageInfo(callerPkg, 64).signatures) {
                digester.update(sig.toByteArray());
            }
        } catch (NoSuchAlgorithmException x) {
            Log.wtf(TAG, "SHA-256 should be available", x);
            digester = null;
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not find packageinfo for: ");
            stringBuilder.append(callerPkg);
            Log.w(str, stringBuilder.toString());
            digester = null;
        }
        if (digester == null) {
            return null;
        }
        return digester.digest();
    }

    private void createNoCredentialsPermissionNotification(Account account, Intent intent, String packageName, int userId) {
        Account account2 = account;
        Intent intent2 = intent;
        int uid = intent2.getIntExtra("uid", -1);
        String authTokenType = intent2.getStringExtra("authTokenType");
        String titleAndSubtitle = this.mContext.getString(17040780, new Object[]{account2.name});
        int index = titleAndSubtitle.indexOf(10);
        String title = titleAndSubtitle;
        String subtitle = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (index > 0) {
            title = titleAndSubtitle.substring(0, index);
            subtitle = titleAndSubtitle.substring(index + 1);
        }
        String title2 = title;
        String subtitle2 = subtitle;
        UserHandle user = UserHandle.of(userId);
        Context contextForUser = getContextForUser(user);
        installNotification(getCredentialPermissionNotificationId(account2, authTokenType, uid), new Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setSmallIcon(17301642).setWhen(0).setColor(contextForUser.getColor(17170784)).setContentTitle(title2).setContentText(subtitle2).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent2, 268435456, null, user)).build(), packageName, user.getIdentifier());
    }

    private Intent newGrantCredentialsPermissionIntent(Account account, String packageName, int uid, AccountAuthenticatorResponse response, String authTokenType, boolean startInNewTask) {
        Intent intent = new Intent(this.mContext, GrantCredentialsPermissionActivity.class);
        if (startInNewTask) {
            intent.setFlags(268435456);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getCredentialPermissionNotificationId(account, authTokenType, uid).mTag);
        stringBuilder.append(packageName != null ? packageName : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        intent.addCategory(stringBuilder.toString());
        intent.putExtra("account", account);
        intent.putExtra("authTokenType", authTokenType);
        intent.putExtra("response", response);
        intent.putExtra("uid", uid);
        return intent;
    }

    private NotificationId getCredentialPermissionNotificationId(Account account, String authTokenType, int uid) {
        NotificationId nId;
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.credentialsPermissionNotificationIds) {
            Pair<Pair<Account, String>, Integer> key = new Pair(new Pair(account, authTokenType), Integer.valueOf(uid));
            nId = (NotificationId) accounts.credentialsPermissionNotificationIds.get(key);
            if (nId == null) {
                String tag = new StringBuilder();
                tag.append("AccountManagerService:38:");
                tag.append(account.hashCode());
                tag.append(":");
                tag.append(authTokenType.hashCode());
                nId = new NotificationId(tag.toString(), 38);
                accounts.credentialsPermissionNotificationIds.put(key, nId);
            }
        }
        return nId;
    }

    private NotificationId getSigninRequiredNotificationId(UserAccounts accounts, Account account) {
        NotificationId nId;
        synchronized (accounts.signinRequiredNotificationIds) {
            nId = (NotificationId) accounts.signinRequiredNotificationIds.get(account);
            if (nId == null) {
                String tag = new StringBuilder();
                tag.append("AccountManagerService:37:");
                tag.append(account.hashCode());
                nId = new NotificationId(tag.toString(), 37);
                accounts.signinRequiredNotificationIds.put(account, nId);
            }
        }
        return nId;
    }

    public void addAccount(IAccountManagerResponse response, String accountType, String authTokenType, String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn) {
        boolean z;
        String str;
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        String str2 = accountType;
        Bundle bundle = optionsIn;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.ACCOUNTMANAGER_ADDACCOUNT);
        Bundle.setDefusable(bundle, true);
        if (Log.isLoggable(TAG, 2)) {
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addAccount , response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", authTokenType ");
            stringBuilder.append(authTokenType);
            stringBuilder.append(", requiredFeatures ");
            stringBuilder.append(Arrays.toString(requiredFeatures));
            stringBuilder.append(", expectActivityLaunch ");
            z = expectActivityLaunch;
            stringBuilder.append(z);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str3, stringBuilder.toString());
        } else {
            str = authTokenType;
            z = expectActivityLaunch;
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        } else if (str2 != null) {
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(uid);
            if (!canUserModifyAccounts(userId, uid)) {
                try {
                    iAccountManagerResponse.onError(100, "User is not allowed to add an account!");
                } catch (RemoteException e) {
                }
                showCantAddAccount(100, userId);
            } else if (canUserModifyAccountsForType(userId, str2, uid)) {
                int pid = Binder.getCallingPid();
                Bundle options = bundle == null ? new Bundle() : bundle;
                options.putInt("callerUid", uid);
                options.putInt("callerPid", pid);
                int usrId = UserHandle.getCallingUserId();
                long identityToken2 = clearCallingIdentity();
                try {
                    UserAccounts accounts = getUserAccounts(usrId);
                    logRecordWithUid(accounts, AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_ADD, "accounts", uid);
                    AnonymousClass9 anonymousClass9 = anonymousClass9;
                    AnonymousClass9 anonymousClass92 = anonymousClass9;
                    long identityToken3 = identityToken2;
                    IAccountManagerResponse iAccountManagerResponse2 = iAccountManagerResponse;
                    String str4 = str2;
                    str = authTokenType;
                    final String[] strArr = requiredFeatures;
                    final Bundle bundle2 = options;
                    final String str5 = accountType;
                    try {
                        anonymousClass9 = new Session(accounts, iAccountManagerResponse2, str4, z, true, null, false, true) {
                            public void run() throws RemoteException {
                                this.mAuthenticator.addAccount(this, this.mAccountType, str, strArr, bundle2);
                            }

                            protected String toDebugString(long now) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(super.toDebugString(now));
                                stringBuilder.append(", addAccount, accountType ");
                                stringBuilder.append(str5);
                                stringBuilder.append(", requiredFeatures ");
                                stringBuilder.append(Arrays.toString(strArr));
                                return stringBuilder.toString();
                            }
                        };
                        anonymousClass92.bind();
                        restoreCallingIdentity(identityToken3);
                    } catch (Throwable th2) {
                        th = th2;
                        identityToken = identityToken3;
                        restoreCallingIdentity(identityToken);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    identityToken = identityToken2;
                    int i = usrId;
                    Bundle bundle3 = options;
                    int i2 = pid;
                    int i3 = userId;
                    int i4 = uid;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } else {
                try {
                    iAccountManagerResponse.onError(101, "User cannot modify accounts of this type (policy).");
                } catch (RemoteException e2) {
                }
                showCantAddAccount(101, userId);
            }
        } else {
            throw new IllegalArgumentException("accountType is null");
        }
    }

    public void addAccountAsUser(IAccountManagerResponse response, String accountType, String authTokenType, String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn, int userId) {
        boolean z;
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        String str = accountType;
        Bundle bundle = optionsIn;
        int i = userId;
        Bundle.setDefusable(bundle, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addAccount, response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", authTokenType ");
            stringBuilder.append(authTokenType);
            stringBuilder.append(", requiredFeatures ");
            stringBuilder.append(Arrays.toString(requiredFeatures));
            stringBuilder.append(", expectActivityLaunch ");
            z = expectActivityLaunch;
            stringBuilder.append(z);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", for user id ");
            stringBuilder.append(i);
            Log.v(str2, stringBuilder.toString());
        } else {
            String str3 = authTokenType;
            z = expectActivityLaunch;
        }
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        Preconditions.checkArgument(str != null, "accountType cannot be null");
        if (isCrossUser(callingUid, i)) {
            throw new SecurityException(String.format("User %s trying to add account for %s", new Object[]{Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)}));
        } else if (!canUserModifyAccounts(i, callingUid)) {
            try {
                iAccountManagerResponse.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, i);
        } else if (canUserModifyAccountsForType(i, str, callingUid)) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            Bundle options = bundle == null ? new Bundle() : bundle;
            options.putInt("callerUid", uid);
            options.putInt("callerPid", pid);
            long identityToken2 = clearCallingIdentity();
            Bundle options2;
            try {
                UserAccounts accounts = getUserAccounts(i);
                logRecordWithUid(accounts, AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_ADD, "accounts", i);
                AnonymousClass10 anonymousClass10 = anonymousClass10;
                AnonymousClass10 anonymousClass102 = anonymousClass10;
                long identityToken3 = identityToken2;
                IAccountManagerResponse iAccountManagerResponse2 = iAccountManagerResponse;
                options2 = options;
                String str4 = str;
                final String str5 = authTokenType;
                final String[] strArr = requiredFeatures;
                final Bundle bundle2 = options2;
                final String str6 = accountType;
                try {
                    anonymousClass10 = new Session(accounts, iAccountManagerResponse2, str4, z, true, null, false, true) {
                        public void run() throws RemoteException {
                            this.mAuthenticator.addAccount(this, this.mAccountType, str5, strArr, bundle2);
                        }

                        protected String toDebugString(long now) {
                            String join;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(super.toDebugString(now));
                            stringBuilder.append(", addAccount, accountType ");
                            stringBuilder.append(str6);
                            stringBuilder.append(", requiredFeatures ");
                            if (strArr != null) {
                                join = TextUtils.join(",", strArr);
                            } else {
                                join = null;
                            }
                            stringBuilder.append(join);
                            return stringBuilder.toString();
                        }
                    };
                    anonymousClass102.bind();
                    restoreCallingIdentity(identityToken3);
                } catch (Throwable th2) {
                    th = th2;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken = identityToken2;
                options2 = options;
                int i2 = uid;
                int i3 = pid;
                int i4 = callingUid;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } else {
            try {
                iAccountManagerResponse.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, i);
        }
    }

    public void startAddAccountSession(IAccountManagerResponse response, String accountType, String authTokenType, String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn) {
        boolean z;
        String str;
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        String str2 = accountType;
        Bundle bundle = optionsIn;
        boolean z2 = true;
        Bundle.setDefusable(bundle, true);
        if (Log.isLoggable(TAG, 2)) {
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startAddAccountSession: accountType ");
            stringBuilder.append(str2);
            stringBuilder.append(", response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", authTokenType ");
            stringBuilder.append(authTokenType);
            stringBuilder.append(", requiredFeatures ");
            stringBuilder.append(Arrays.toString(requiredFeatures));
            stringBuilder.append(", expectActivityLaunch ");
            z = expectActivityLaunch;
            stringBuilder.append(z);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str3, stringBuilder.toString());
        } else {
            str = authTokenType;
            z = expectActivityLaunch;
        }
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        if (str2 == null) {
            z2 = false;
        }
        Preconditions.checkArgument(z2, "accountType cannot be null");
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (!canUserModifyAccounts(userId, uid)) {
            try {
                iAccountManagerResponse.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
        } else if (canUserModifyAccountsForType(userId, str2, uid)) {
            int pid = Binder.getCallingPid();
            Bundle options = bundle == null ? new Bundle() : bundle;
            options.putInt("callerUid", uid);
            options.putInt("callerPid", pid);
            String callerPkg = bundle.getString("androidPackageName");
            boolean isPasswordForwardingAllowed = isPermitted(callerPkg, uid, "android.permission.GET_PASSWORD");
            long identityToken2 = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(userId);
                logRecordWithUid(accounts, AccountsDb.DEBUG_ACTION_CALLED_START_ACCOUNT_ADD, "accounts", uid);
                AnonymousClass11 anonymousClass11 = anonymousClass11;
                AnonymousClass11 anonymousClass112 = anonymousClass11;
                long identityToken3 = identityToken2;
                IAccountManagerResponse iAccountManagerResponse2 = iAccountManagerResponse;
                callerPkg = str2;
                str = authTokenType;
                final String[] strArr = requiredFeatures;
                final Bundle bundle2 = options;
                final String str4 = accountType;
                try {
                    anonymousClass11 = new StartAccountSession(accounts, iAccountManagerResponse2, callerPkg, z, null, false, true, isPasswordForwardingAllowed) {
                        public void run() throws RemoteException {
                            this.mAuthenticator.startAddAccountSession(this, this.mAccountType, str, strArr, bundle2);
                        }

                        protected String toDebugString(long now) {
                            String requiredFeaturesStr = TextUtils.join(",", strArr);
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(super.toDebugString(now));
                            stringBuilder.append(", startAddAccountSession, accountType ");
                            stringBuilder.append(str4);
                            stringBuilder.append(", requiredFeatures ");
                            stringBuilder.append(strArr != null ? requiredFeaturesStr : null);
                            return stringBuilder.toString();
                        }
                    };
                    anonymousClass112.bind();
                    restoreCallingIdentity(identityToken3);
                } catch (Throwable th2) {
                    th = th2;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken = identityToken2;
                String str5 = callerPkg;
                Bundle bundle3 = options;
                int i = pid;
                int i2 = userId;
                int i3 = uid;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } else {
            try {
                iAccountManagerResponse.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:61:0x014d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void finishSessionAsUser(IAccountManagerResponse response, Bundle sessionBundle, boolean expectActivityLaunch, Bundle appInfo, int userId) {
        boolean z;
        GeneralSecurityException e;
        int i;
        int i2;
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        Bundle bundle = sessionBundle;
        Bundle bundle2 = appInfo;
        int i3 = userId;
        Bundle.setDefusable(bundle, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("finishSession: response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", expectActivityLaunch ");
            z = expectActivityLaunch;
            stringBuilder.append(z);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", caller's user id ");
            stringBuilder.append(UserHandle.getCallingUserId());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", for user id ");
            stringBuilder.append(i3);
            Log.v(str, stringBuilder.toString());
        } else {
            z = expectActivityLaunch;
        }
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        if (bundle == null || sessionBundle.size() == 0) {
            throw new IllegalArgumentException("sessionBundle is empty");
        } else if (isCrossUser(callingUid, i3)) {
            throw new SecurityException(String.format("User %s trying to finish session for %s without cross user permission", new Object[]{Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)}));
        } else if (canUserModifyAccounts(i3, callingUid)) {
            int pid = Binder.getCallingPid();
            try {
                Bundle decryptedBundle = CryptoHelper.getInstance().decryptBundle(bundle);
                if (decryptedBundle == null) {
                    try {
                        sendErrorResponse(iAccountManagerResponse, 8, "failed to decrypt session bundle");
                        return;
                    } catch (GeneralSecurityException e2) {
                        e = e2;
                        i = pid;
                        i2 = callingUid;
                        if (Log.isLoggable(TAG, 3)) {
                            Log.v(TAG, "Failed to decrypt session bundle!", e);
                        }
                        sendErrorResponse(iAccountManagerResponse, 8, "failed to decrypt session bundle");
                    }
                }
                String accountType = decryptedBundle.getString("accountType");
                if (TextUtils.isEmpty(accountType)) {
                    sendErrorResponse(iAccountManagerResponse, 7, "accountType is empty");
                    return;
                }
                if (bundle2 != null) {
                    decryptedBundle.putAll(bundle2);
                }
                decryptedBundle.putInt("callerUid", callingUid);
                decryptedBundle.putInt("callerPid", pid);
                if (canUserModifyAccountsForType(i3, accountType, callingUid)) {
                    long identityToken2 = clearCallingIdentity();
                    try {
                        UserAccounts accounts = getUserAccounts(i3);
                        logRecordWithUid(accounts, AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_SESSION_FINISH, "accounts", callingUid);
                        AnonymousClass12 anonymousClass12 = anonymousClass12;
                        AnonymousClass12 anonymousClass122 = anonymousClass12;
                        long identityToken3 = identityToken2;
                        final Bundle bundle3 = decryptedBundle;
                        final String str2 = accountType;
                        try {
                            anonymousClass12 = new Session(accounts, iAccountManagerResponse, accountType, z, true, null, false, true) {
                                public void run() throws RemoteException {
                                    this.mAuthenticator.finishSession(this, this.mAccountType, bundle3);
                                }

                                protected String toDebugString(long now) {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(super.toDebugString(now));
                                    stringBuilder.append(", finishSession, accountType ");
                                    stringBuilder.append(str2);
                                    return stringBuilder.toString();
                                }
                            };
                            anonymousClass122.bind();
                            restoreCallingIdentity(identityToken3);
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            identityToken = identityToken3;
                            restoreCallingIdentity(identityToken);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        identityToken = identityToken2;
                        String str3 = accountType;
                        Bundle bundle4 = decryptedBundle;
                        i = pid;
                        i2 = callingUid;
                        restoreCallingIdentity(identityToken);
                        throw th;
                    }
                }
                sendErrorResponse(iAccountManagerResponse, 101, "User cannot modify accounts of this type (policy).");
                showCantAddAccount(101, i3);
            } catch (GeneralSecurityException e3) {
                e = e3;
                i = pid;
                i2 = callingUid;
                if (Log.isLoggable(TAG, 3)) {
                }
                sendErrorResponse(iAccountManagerResponse, 8, "failed to decrypt session bundle");
            }
        } else {
            sendErrorResponse(iAccountManagerResponse, 100, "User is not allowed to add an account!");
            showCantAddAccount(100, i3);
        }
    }

    private void showCantAddAccount(int errorCode, int userId) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        Intent intent = null;
        if (dpmi == null) {
            intent = getDefaultCantAddAccountIntent(errorCode);
        } else if (errorCode == 100) {
            intent = dpmi.createUserRestrictionSupportIntent(userId, "no_modify_accounts");
        } else if (errorCode == 101) {
            intent = dpmi.createShowAdminSupportIntent(userId, false);
        }
        if (intent == null) {
            intent = getDefaultCantAddAccountIntent(errorCode);
        }
        long identityToken = clearCallingIdentity();
        try {
            this.mContext.startActivityAsUser(intent, new UserHandle(userId));
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private Intent getDefaultCantAddAccountIntent(int errorCode) {
        Intent cantAddAccount = new Intent(this.mContext, CantAddAccountActivity.class);
        cantAddAccount.putExtra("android.accounts.extra.ERROR_CODE", errorCode);
        cantAddAccount.addFlags(268435456);
        return cantAddAccount;
    }

    public void confirmCredentialsAsUser(IAccountManagerResponse response, Account account, Bundle options, boolean expectActivityLaunch, int userId) {
        boolean z;
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        Account account2 = account;
        int i = userId;
        Bundle.setDefusable(options, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("confirmCredentials , response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", expectActivityLaunch ");
            z = expectActivityLaunch;
            stringBuilder.append(z);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        } else {
            z = expectActivityLaunch;
        }
        if (isCrossUser(callingUid, i)) {
            throw new SecurityException(String.format("User %s trying to confirm account credentials for %s", new Object[]{Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)}));
        } else if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        } else if (account2 != null) {
            long identityToken2 = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(i);
                AnonymousClass13 anonymousClass13 = anonymousClass13;
                long identityToken3 = identityToken2;
                final Account account3 = account2;
                final Bundle bundle = options;
                try {
                    new Session(accounts, iAccountManagerResponse, account2.type, z, true, account2.name, true, true) {
                        public void run() throws RemoteException {
                            this.mAuthenticator.confirmCredentials(this, account3, bundle);
                        }

                        protected String toDebugString(long now) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(super.toDebugString(now));
                            stringBuilder.append(", confirmCredentials, ");
                            stringBuilder.append(account3);
                            return stringBuilder.toString();
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken3);
                } catch (Throwable th2) {
                    th = th2;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken = identityToken2;
                int i2 = callingUid;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } else {
            throw new IllegalArgumentException("account is null");
        }
    }

    public void updateCredentials(IAccountManagerResponse response, Account account, String authTokenType, boolean expectActivityLaunch, Bundle loginOptions) {
        boolean z;
        String str;
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        Account account2 = account;
        Bundle.setDefusable(loginOptions, true);
        if (Log.isLoggable(TAG, 2)) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateCredentials , response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", authTokenType ");
            stringBuilder.append(authTokenType);
            stringBuilder.append(", expectActivityLaunch ");
            z = expectActivityLaunch;
            stringBuilder.append(z);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str2, stringBuilder.toString());
        } else {
            str = authTokenType;
            z = expectActivityLaunch;
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        } else if (account2 != null) {
            int userId = UserHandle.getCallingUserId();
            long identityToken2 = clearCallingIdentity();
            try {
                AnonymousClass14 anonymousClass14 = anonymousClass14;
                boolean z2 = z;
                long identityToken3 = identityToken2;
                final Account account3 = account2;
                str = authTokenType;
                final Bundle bundle = loginOptions;
                try {
                    new Session(getUserAccounts(userId), iAccountManagerResponse, account2.type, z2, true, account2.name, false, true) {
                        public void run() throws RemoteException {
                            this.mAuthenticator.updateCredentials(this, account3, str, bundle);
                        }

                        protected String toDebugString(long now) {
                            if (bundle != null) {
                                bundle.keySet();
                            }
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(super.toDebugString(now));
                            stringBuilder.append(", updateCredentials, ");
                            stringBuilder.append(account3);
                            stringBuilder.append(", authTokenType ");
                            stringBuilder.append(str);
                            stringBuilder.append(", loginOptions ");
                            stringBuilder.append(bundle);
                            return stringBuilder.toString();
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken3);
                } catch (Throwable th2) {
                    th = th2;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken = identityToken2;
                int i = userId;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } else {
            throw new IllegalArgumentException("account is null");
        }
    }

    public void startUpdateCredentialsSession(IAccountManagerResponse response, Account account, String authTokenType, boolean expectActivityLaunch, Bundle loginOptions) {
        boolean z;
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        Account account2 = account;
        Bundle bundle = loginOptions;
        Bundle.setDefusable(bundle, true);
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startUpdateCredentialsSession: ");
            stringBuilder.append(account2);
            stringBuilder.append(", response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", authTokenType ");
            stringBuilder.append(authTokenType);
            stringBuilder.append(", expectActivityLaunch ");
            z = expectActivityLaunch;
            stringBuilder.append(z);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        } else {
            String str2 = authTokenType;
            z = expectActivityLaunch;
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        } else if (account2 != null) {
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getCallingUserId();
            String callerPkg = bundle.getString("androidPackageName");
            boolean isPasswordForwardingAllowed = isPermitted(callerPkg, uid, "android.permission.GET_PASSWORD");
            long identityToken2 = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(userId);
                AnonymousClass15 anonymousClass15 = anonymousClass15;
                String str3 = account2.name;
                long identityToken3 = identityToken2;
                String str4 = account2.type;
                callerPkg = str3;
                final Account account3 = account2;
                final String str5 = authTokenType;
                final Bundle bundle2 = loginOptions;
                try {
                    new StartAccountSession(accounts, iAccountManagerResponse, str4, z, callerPkg, false, true, isPasswordForwardingAllowed) {
                        public void run() throws RemoteException {
                            this.mAuthenticator.startUpdateCredentialsSession(this, account3, str5, bundle2);
                        }

                        protected String toDebugString(long now) {
                            if (bundle2 != null) {
                                bundle2.keySet();
                            }
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(super.toDebugString(now));
                            stringBuilder.append(", startUpdateCredentialsSession, ");
                            stringBuilder.append(account3);
                            stringBuilder.append(", authTokenType ");
                            stringBuilder.append(str5);
                            stringBuilder.append(", loginOptions ");
                            stringBuilder.append(bundle2);
                            return stringBuilder.toString();
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken3);
                } catch (Throwable th2) {
                    th = th2;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken = identityToken2;
                String str6 = callerPkg;
                int i = userId;
                int i2 = uid;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } else {
            throw new IllegalArgumentException("account is null");
        }
    }

    public void isCredentialsUpdateSuggested(IAccountManagerResponse response, Account account, String statusToken) {
        Throwable th;
        long identityToken;
        IAccountManagerResponse iAccountManagerResponse = response;
        Account account2 = account;
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isCredentialsUpdateSuggested: ");
            stringBuilder.append(account2);
            stringBuilder.append(", response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str, stringBuilder.toString());
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        } else if (account2 == null) {
            throw new IllegalArgumentException("account is null");
        } else if (TextUtils.isEmpty(statusToken)) {
            throw new IllegalArgumentException("status token is empty");
        } else {
            int usrId = UserHandle.getCallingUserId();
            long identityToken2 = clearCallingIdentity();
            try {
                AnonymousClass16 anonymousClass16 = anonymousClass16;
                long identityToken3 = identityToken2;
                final Account account3 = account2;
                final String str2 = statusToken;
                try {
                    new Session(getUserAccounts(usrId), iAccountManagerResponse, account2.type, false, false, account2.name, false) {
                        protected String toDebugString(long now) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(super.toDebugString(now));
                            stringBuilder.append(", isCredentialsUpdateSuggested, ");
                            stringBuilder.append(account3);
                            return stringBuilder.toString();
                        }

                        public void run() throws RemoteException {
                            this.mAuthenticator.isCredentialsUpdateSuggested(this, account3, str2);
                        }

                        public void onResult(Bundle result) {
                            Bundle.setDefusable(result, true);
                            IAccountManagerResponse response = getResponseAndClose();
                            if (response != null) {
                                if (result == null) {
                                    AccountManagerService.this.sendErrorResponse(response, 5, "null bundle");
                                    return;
                                }
                                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                                    String str = AccountManagerService.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(getClass().getSimpleName());
                                    stringBuilder.append(" calling onResult() on response ");
                                    stringBuilder.append(response);
                                    Log.v(str, stringBuilder.toString());
                                }
                                if (result.getInt("errorCode", -1) > 0) {
                                    AccountManagerService.this.sendErrorResponse(response, result.getInt("errorCode"), result.getString("errorMessage"));
                                } else if (result.containsKey("booleanResult")) {
                                    Bundle newResult = new Bundle();
                                    newResult.putBoolean("booleanResult", result.getBoolean("booleanResult", false));
                                    AccountManagerService.this.sendResponse(response, newResult);
                                } else {
                                    AccountManagerService.this.sendErrorResponse(response, 5, "no result in response");
                                }
                            }
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken3);
                } catch (Throwable th2) {
                    th = th2;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                identityToken = identityToken2;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        }
    }

    public void editProperties(IAccountManagerResponse response, String accountType, boolean expectActivityLaunch) {
        boolean z;
        Throwable th;
        IAccountManagerResponse iAccountManagerResponse = response;
        String str = accountType;
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("editProperties , response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", expectActivityLaunch ");
            z = expectActivityLaunch;
            stringBuilder.append(z);
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str2, stringBuilder.toString());
        } else {
            z = expectActivityLaunch;
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        } else if (str != null) {
            int userId = UserHandle.getCallingUserId();
            if (isAccountManagedByCaller(str, callingUid, userId) || isSystemUid(callingUid)) {
                long identityToken = clearCallingIdentity();
                long identityToken2;
                try {
                    UserAccounts accounts = getUserAccounts(userId);
                    AnonymousClass17 anonymousClass17 = anonymousClass17;
                    identityToken2 = identityToken;
                    userId = str;
                    try {
                        new Session(accounts, iAccountManagerResponse, str, z, true, null, false) {
                            public void run() throws RemoteException {
                                this.mAuthenticator.editProperties(this, this.mAccountType);
                            }

                            protected String toDebugString(long now) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(super.toDebugString(now));
                                stringBuilder.append(", editProperties, accountType ");
                                stringBuilder.append(userId);
                                return stringBuilder.toString();
                            }
                        }.bind();
                        restoreCallingIdentity(identityToken2);
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        restoreCallingIdentity(identityToken2);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    identityToken2 = identityToken;
                    int i = userId;
                    restoreCallingIdentity(identityToken2);
                    throw th;
                }
            }
            throw new SecurityException(String.format("uid %s cannot edit authenticator properites for account type: %s", new Object[]{Integer.valueOf(callingUid), str}));
        } else {
            throw new IllegalArgumentException("accountType is null");
        }
    }

    public boolean hasAccountAccess(Account account, String packageName, UserHandle userHandle) {
        if (UserHandle.getAppId(Binder.getCallingUid()) == 1000) {
            Preconditions.checkNotNull(account, "account cannot be null");
            Preconditions.checkNotNull(packageName, "packageName cannot be null");
            Preconditions.checkNotNull(userHandle, "userHandle cannot be null");
            int userId = userHandle.getIdentifier();
            Preconditions.checkArgumentInRange(userId, 0, HwBootFail.STAGE_BOOT_SUCCESS, "user must be concrete");
            try {
                return hasAccountAccess(account, packageName, this.mPackageManager.getPackageUidAsUser(packageName, userId));
            } catch (NameNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Package not found ");
                stringBuilder.append(e.getMessage());
                Log.d(str, stringBuilder.toString());
                return false;
            }
        }
        throw new SecurityException("Can be called only by system UID");
    }

    private String getPackageNameForUid(int uid) {
        String[] packageNames = this.mPackageManager.getPackagesForUid(uid);
        if (ArrayUtils.isEmpty(packageNames)) {
            return null;
        }
        String packageName = packageNames[0];
        if (packageNames.length == 1) {
            return packageName;
        }
        int oldestVersion = HwBootFail.STAGE_BOOT_SUCCESS;
        String packageName2 = packageName;
        for (String name : packageNames) {
            try {
                ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(name, 0);
                if (applicationInfo != null) {
                    int version = applicationInfo.targetSdkVersion;
                    if (version < oldestVersion) {
                        oldestVersion = version;
                        packageName2 = name;
                    }
                }
            } catch (NameNotFoundException e) {
            }
        }
        return packageName2;
    }

    private boolean hasAccountAccess(Account account, String packageName, int uid) {
        boolean z = false;
        if (packageName == null) {
            packageName = getPackageNameForUid(uid);
            if (packageName == null) {
                return false;
            }
        }
        if (permissionIsGranted(account, null, uid, UserHandle.getUserId(uid))) {
            return true;
        }
        int visibility = resolveAccountVisibility(account, packageName, getUserAccounts(UserHandle.getUserId(uid))).intValue();
        if (visibility == 1 || visibility == 2) {
            z = true;
        }
        return z;
    }

    public IntentSender createRequestAccountAccessIntentSenderAsUser(Account account, String packageName, UserHandle userHandle) {
        if (UserHandle.getAppId(Binder.getCallingUid()) == 1000) {
            Preconditions.checkNotNull(account, "account cannot be null");
            Preconditions.checkNotNull(packageName, "packageName cannot be null");
            Preconditions.checkNotNull(userHandle, "userHandle cannot be null");
            int userId = userHandle.getIdentifier();
            Preconditions.checkArgumentInRange(userId, 0, HwBootFail.STAGE_BOOT_SUCCESS, "user must be concrete");
            try {
                Intent intent = newRequestAccountAccessIntent(account, packageName, this.mPackageManager.getPackageUidAsUser(packageName, userId), null);
                long identity = Binder.clearCallingIdentity();
                try {
                    IntentSender intentSender = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, null, new UserHandle(userId)).getIntentSender();
                    return intentSender;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            } catch (NameNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown package ");
                stringBuilder.append(packageName);
                Slog.e(str, stringBuilder.toString());
                return null;
            }
        }
        throw new SecurityException("Can be called only by system UID");
    }

    private Intent newRequestAccountAccessIntent(Account account, String packageName, int uid, RemoteCallback callback) {
        final Account account2 = account;
        final int i = uid;
        final String str = packageName;
        final RemoteCallback remoteCallback = callback;
        return newGrantCredentialsPermissionIntent(account, packageName, uid, new AccountAuthenticatorResponse(new IAccountAuthenticatorResponse.Stub() {
            public void onResult(Bundle value) throws RemoteException {
                handleAuthenticatorResponse(true);
            }

            public void onRequestContinued() {
            }

            public void onError(int errorCode, String errorMessage) throws RemoteException {
                handleAuthenticatorResponse(false);
            }

            private void handleAuthenticatorResponse(boolean accessGranted) throws RemoteException {
                AccountManagerService.this.cancelNotification(AccountManagerService.this.getCredentialPermissionNotificationId(account2, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", i), str, UserHandle.getUserHandleForUid(i));
                if (remoteCallback != null) {
                    Bundle result = new Bundle();
                    result.putBoolean("booleanResult", accessGranted);
                    remoteCallback.sendResult(result);
                }
            }
        }), "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", false);
    }

    public boolean someUserHasAccount(Account account) {
        if (UserHandle.isSameApp(1000, Binder.getCallingUid())) {
            long token = Binder.clearCallingIdentity();
            try {
                AccountAndUser[] allAccounts = getAllAccounts();
                for (int i = allAccounts.length - 1; i >= 0; i--) {
                    if (allAccounts[i].account.equals(account)) {
                        return true;
                    }
                }
                Binder.restoreCallingIdentity(token);
                return false;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            throw new SecurityException("Only system can check for accounts across users");
        }
    }

    public Account[] getAccounts(int userId, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        List<String> visibleAccountTypes = getTypesVisibleToCaller(callingUid, userId, opPackageName);
        if (visibleAccountTypes.isEmpty()) {
            return EMPTY_ACCOUNT_ARRAY;
        }
        long identityToken = clearCallingIdentity();
        try {
            Account[] accountsInternal = getAccountsInternal(getUserAccounts(userId), callingUid, opPackageName, visibleAccountTypes, false);
            return accountsInternal;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public AccountAndUser[] getRunningAccounts() {
        try {
            return getAccounts(ActivityManager.getService().getRunningUserIds());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public AccountAndUser[] getAllAccounts() {
        List<UserInfo> users = getUserManager().getUsers(true);
        int[] userIds = new int[users.size()];
        for (int i = 0; i < userIds.length; i++) {
            userIds[i] = ((UserInfo) users.get(i)).id;
        }
        return getAccounts(userIds);
    }

    private AccountAndUser[] getAccounts(int[] userIds) {
        ArrayList<AccountAndUser> runningAccounts = Lists.newArrayList();
        for (int userId : userIds) {
            try {
                UserAccounts userAccounts = getUserAccounts(userId);
                if (userAccounts != null) {
                    for (Account account : getAccountsFromCache(userAccounts, null, Binder.getCallingUid(), null, false)) {
                        runningAccounts.add(new AccountAndUser(account, userId));
                    }
                }
            } catch (SQLiteCantOpenDatabaseException e) {
                Slog.e(TAG, e.getMessage(), new Throwable());
            }
        }
        return (AccountAndUser[]) runningAccounts.toArray(new AccountAndUser[runningAccounts.size()]);
    }

    public Account[] getAccountsAsUser(String type, int userId, String opPackageName) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), opPackageName);
        return getAccountsAsUserForPackage(type, userId, opPackageName, -1, opPackageName, false);
    }

    private Account[] getAccountsAsUserForPackage(String type, int userId, String callingPackage, int packageUid, String opPackageName, boolean includeUserManagedNotVisible) {
        Throwable th;
        String str = type;
        int i = userId;
        int callingUid = Binder.getCallingUid();
        if (i == UserHandle.getCallingUserId() || callingUid == 1000 || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            String opPackageName2;
            int callingUid2;
            if (Log.isLoggable(TAG, 2)) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getAccounts: accountType ");
                stringBuilder.append(str);
                stringBuilder.append(", caller's uid ");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(", pid ");
                stringBuilder.append(Binder.getCallingPid());
                Log.v(str2, stringBuilder.toString());
            }
            List<String> managedTypes = getTypesManagedByCaller(callingUid, UserHandle.getUserId(callingUid));
            int i2 = packageUid;
            if (i2 == -1 || (!UserHandle.isSameApp(callingUid, 1000) && (str == null || !managedTypes.contains(str)))) {
                opPackageName2 = opPackageName;
                callingUid2 = callingUid;
            } else {
                callingUid2 = i2;
                opPackageName2 = callingPackage;
            }
            List<String> visibleAccountTypes = getTypesVisibleToCaller(callingUid2, i, opPackageName2);
            if (visibleAccountTypes.isEmpty() || (str != null && !visibleAccountTypes.contains(str))) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            if (visibleAccountTypes.contains(str)) {
                visibleAccountTypes = new ArrayList();
                visibleAccountTypes.add(str);
            }
            List<String> visibleAccountTypes2 = visibleAccountTypes;
            long identityToken = clearCallingIdentity();
            long identityToken2;
            try {
                UserAccounts accounts = getUserAccounts(i);
                identityToken2 = identityToken;
                try {
                    Account[] accountsInternal = getAccountsInternal(accounts, callingUid2, opPackageName2, visibleAccountTypes2, includeUserManagedNotVisible);
                    restoreCallingIdentity(identityToken2);
                    return accountsInternal;
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
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("User ");
        stringBuilder2.append(UserHandle.getCallingUserId());
        stringBuilder2.append(" trying to get account for ");
        stringBuilder2.append(i);
        throw new SecurityException(stringBuilder2.toString());
    }

    private Account[] getAccountsInternal(UserAccounts userAccounts, int callingUid, String callingPackage, List<String> visibleAccountTypes, boolean includeUserManagedNotVisible) {
        int i = 0;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(callingUid, 0, BehaviorId.ACCOUNTMANAGER_GETACCOUNTSINTERNAL);
        ArrayList<Account> visibleAccounts = new ArrayList();
        for (String visibleType : visibleAccountTypes) {
            Account[] accountsForType = getAccountsFromCache(userAccounts, visibleType, callingUid, callingPackage, includeUserManagedNotVisible);
            if (accountsForType != null) {
                visibleAccounts.addAll(Arrays.asList(accountsForType));
            }
        }
        Account[] result = new Account[visibleAccounts.size()];
        while (i < visibleAccounts.size()) {
            result[i] = (Account) visibleAccounts.get(i);
            i++;
        }
        return result;
    }

    public void addSharedAccountsFromParentUser(int parentUserId, int userId, String opPackageName) {
        checkManageOrCreateUsersPermission("addSharedAccountsFromParentUser");
        for (Account account : getAccountsAsUser(null, parentUserId, opPackageName)) {
            addSharedAccountAsUser(account, userId);
        }
    }

    private boolean addSharedAccountAsUser(Account account, int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        accounts.accountsDb.deleteSharedAccount(account);
        long accountId = accounts.accountsDb.insertSharedAccount(account);
        if (accountId < 0) {
            Log.w(TAG, "insertAccountIntoDatabase , skipping the DB insert failed");
            return false;
        }
        logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_ADD, "shared_accounts", accountId, accounts);
        return true;
    }

    public boolean renameSharedAccountAsUser(Account account, String newName, int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        long sharedTableAccountId = accounts.accountsDb.findSharedAccountId(account);
        int r = accounts.accountsDb.renameSharedAccount(account, newName);
        if (r > 0) {
            logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_RENAME, "shared_accounts", sharedTableAccountId, accounts, getCallingUid());
            renameAccountInternal(accounts, account, newName);
        }
        return r > 0;
    }

    public boolean removeSharedAccountAsUser(Account account, int userId) {
        return removeSharedAccountAsUser(account, userId, getCallingUid());
    }

    private boolean removeSharedAccountAsUser(Account account, int userId, int callingUid) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        long sharedTableAccountId = accounts.accountsDb.findSharedAccountId(account);
        boolean deleted = accounts.accountsDb.deleteSharedAccount(account);
        if (deleted) {
            logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE, "shared_accounts", sharedTableAccountId, accounts, callingUid);
            removeAccountInternal(accounts, account, callingUid);
        }
        return deleted;
    }

    public Account[] getSharedAccountsAsUser(int userId) {
        Account[] accountArray;
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        synchronized (accounts.dbLock) {
            List<Account> accountList = accounts.accountsDb.getSharedAccounts();
            accountArray = new Account[accountList.size()];
            accountList.toArray(accountArray);
        }
        return accountArray;
    }

    public Account[] getAccounts(String type, String opPackageName) {
        return getAccountsAsUser(type, UserHandle.getCallingUserId(), opPackageName);
    }

    public Account[] getAccountsForPackage(String packageName, int uid, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.isSameApp(callingUid, 1000)) {
            return getAccountsAsUserForPackage(null, UserHandle.getCallingUserId(), packageName, uid, opPackageName, true);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAccountsForPackage() called from unauthorized uid ");
        stringBuilder.append(callingUid);
        stringBuilder.append(" with uid=");
        stringBuilder.append(uid);
        throw new SecurityException(stringBuilder.toString());
    }

    public Account[] getAccountsByTypeForPackage(String type, String packageName, String opPackageName) {
        String str = type;
        String str2 = packageName;
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        String str3 = opPackageName;
        this.mAppOpsManager.checkPackage(callingUid, str3);
        int packageUid = -1;
        try {
            int packageUid2 = this.mPackageManager.getPackageUidAsUser(str2, userId);
            if (!UserHandle.isSameApp(callingUid, 1000) && str != null && !isAccountManagedByCaller(str, callingUid, userId)) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            if (!UserHandle.isSameApp(callingUid, 1000) && str == null) {
                return getAccountsAsUserForPackage(str, userId, str2, packageUid2, str3, false);
            }
            return getAccountsAsUserForPackage(str, userId, str2, packageUid2, opPackageName, 1);
        } catch (NameNotFoundException re) {
            int i = userId;
            int i2 = callingUid;
            String str4 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't determine the packageUid for ");
            stringBuilder.append(str2);
            stringBuilder.append(re);
            Slog.e(str4, stringBuilder.toString());
            return EMPTY_ACCOUNT_ARRAY;
        }
    }

    private boolean needToStartChooseAccountActivity(Account[] accounts, String callingPackage) {
        if (accounts.length < 1) {
            return false;
        }
        return accounts.length > 1 || resolveAccountVisibility(accounts[0], callingPackage, getUserAccounts(UserHandle.getCallingUserId())).intValue() == 4;
    }

    private void startChooseAccountActivityWithAccounts(IAccountManagerResponse response, Account[] accounts, String callingPackage) {
        Intent intent = new Intent(this.mContext, ChooseAccountActivity.class);
        intent.putExtra("accounts", accounts);
        intent.putExtra("accountManagerResponse", new AccountManagerResponse(response));
        intent.putExtra("androidPackageName", callingPackage);
        this.mContext.startActivityAsUser(intent, UserHandle.of(UserHandle.getCallingUserId()));
    }

    private void handleGetAccountsResult(IAccountManagerResponse response, Account[] accounts, String callingPackage) {
        if (needToStartChooseAccountActivity(accounts, callingPackage)) {
            startChooseAccountActivityWithAccounts(response, accounts, callingPackage);
        } else if (accounts.length == 1) {
            Bundle bundle = new Bundle();
            bundle.putString("authAccount", accounts[0].name);
            bundle.putString("accountType", accounts[0].type);
            onResult(response, bundle);
        } else {
            onResult(response, new Bundle());
        }
    }

    public void getAccountByTypeAndFeatures(IAccountManagerResponse response, String accountType, String[] features, String opPackageName) {
        Throwable th;
        long identityToken;
        int i;
        int i2;
        final IAccountManagerResponse iAccountManagerResponse = response;
        String str = accountType;
        final String str2 = opPackageName;
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, str2);
        if (Log.isLoggable(TAG, 2)) {
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAccount: accountType ");
            stringBuilder.append(str);
            stringBuilder.append(", response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", features ");
            stringBuilder.append(Arrays.toString(features));
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str3, stringBuilder.toString());
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        } else if (str != null) {
            int userId = UserHandle.getCallingUserId();
            long identityToken2 = clearCallingIdentity();
            try {
                UserAccounts userAccounts = getUserAccounts(userId);
                if (ArrayUtils.isEmpty(features)) {
                    try {
                        handleGetAccountsResult(iAccountManagerResponse, getAccountsFromCache(userAccounts, str, callingUid, str2, true), str2);
                        restoreCallingIdentity(identityToken2);
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        identityToken = identityToken2;
                        i = userId;
                        i2 = callingUid;
                        restoreCallingIdentity(identityToken);
                        throw th;
                    }
                }
                GetAccountsByTypeAndFeatureSession getAccountsByTypeAndFeatureSession = getAccountsByTypeAndFeatureSession;
                long identityToken3 = identityToken2;
                try {
                    new GetAccountsByTypeAndFeatureSession(this, userAccounts, new IAccountManagerResponse.Stub() {
                        public void onResult(Bundle value) throws RemoteException {
                            Parcelable[] parcelables = value.getParcelableArray("accounts");
                            Account[] accounts = new Account[parcelables.length];
                            for (int i = 0; i < parcelables.length; i++) {
                                accounts[i] = (Account) parcelables[i];
                            }
                            AccountManagerService.this.handleGetAccountsResult(iAccountManagerResponse, accounts, str2);
                        }

                        public void onError(int errorCode, String errorMessage) throws RemoteException {
                        }
                    }, str, features, callingUid, str2, 1).bind();
                    restoreCallingIdentity(identityToken3);
                } catch (Throwable th3) {
                    th = th3;
                    identityToken = identityToken3;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                identityToken = identityToken2;
                i = userId;
                i2 = callingUid;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } else {
            throw new IllegalArgumentException("accountType is null");
        }
    }

    public void getAccountsByFeatures(IAccountManagerResponse response, String type, String[] features, String opPackageName) {
        long identityToken;
        Throwable th;
        IAccountManagerResponse iAccountManagerResponse = response;
        String str = type;
        String[] strArr = features;
        String str2 = opPackageName;
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, str2);
        if (Log.isLoggable(TAG, 2)) {
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAccounts: accountType ");
            stringBuilder.append(str);
            stringBuilder.append(", response ");
            stringBuilder.append(iAccountManagerResponse);
            stringBuilder.append(", features ");
            stringBuilder.append(Arrays.toString(features));
            stringBuilder.append(", caller's uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(", pid ");
            stringBuilder.append(Binder.getCallingPid());
            Log.v(str3, stringBuilder.toString());
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        } else if (str != null) {
            int userId = UserHandle.getCallingUserId();
            List<String> visibleAccountTypes = getTypesVisibleToCaller(callingUid, userId, str2);
            if (visibleAccountTypes.contains(str)) {
                long identityToken2 = clearCallingIdentity();
                List<String> list;
                int i;
                try {
                    UserAccounts userAccounts = getUserAccounts(userId);
                    if (strArr == null) {
                        identityToken = identityToken2;
                        list = visibleAccountTypes;
                        i = userId;
                    } else if (strArr.length == 0) {
                        identityToken = identityToken2;
                        list = visibleAccountTypes;
                        i = userId;
                    } else {
                        GetAccountsByTypeAndFeatureSession getAccountsByTypeAndFeatureSession = getAccountsByTypeAndFeatureSession;
                        long identityToken3 = identityToken2;
                        try {
                            new GetAccountsByTypeAndFeatureSession(this, userAccounts, iAccountManagerResponse, str, strArr, callingUid, str2, 0).bind();
                            restoreCallingIdentity(identityToken3);
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            identityToken = identityToken3;
                            restoreCallingIdentity(identityToken);
                            throw th;
                        }
                    }
                    try {
                        Account[] accounts = getAccountsFromCache(userAccounts, str, callingUid, str2, false);
                        Bundle result = new Bundle();
                        result.putParcelableArray("accounts", accounts);
                        onResult(iAccountManagerResponse, result);
                        restoreCallingIdentity(identityToken);
                        return;
                    } catch (Throwable th3) {
                        th = th3;
                        restoreCallingIdentity(identityToken);
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    identityToken = identityToken2;
                    list = visibleAccountTypes;
                    i = userId;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            }
            Bundle result2 = new Bundle();
            result2.putParcelableArray("accounts", EMPTY_ACCOUNT_ARRAY);
            try {
                iAccountManagerResponse.onResult(result2);
            } catch (RemoteException e) {
                RemoteException remoteException = e;
                Log.e(TAG, "Cannot respond to caller do to exception.", e);
            }
        } else {
            throw new IllegalArgumentException("accountType is null");
        }
    }

    public void onAccountAccessed(String token) throws RemoteException {
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) != 1000) {
            int userId = UserHandle.getCallingUserId();
            long identity = Binder.clearCallingIdentity();
            try {
                for (Account account : getAccounts(userId, this.mContext.getOpPackageName())) {
                    if (Objects.equals(account.getAccessId(), token) && !hasAccountAccess(account, null, uid)) {
                        updateAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", uid, true);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new AccountManagerServiceShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    private void logRecord(UserAccounts accounts, String action, String tableName) {
        logRecord(action, tableName, -1, accounts);
    }

    private void logRecordWithUid(UserAccounts accounts, String action, String tableName, int uid) {
        logRecord(action, tableName, -1, accounts, uid);
    }

    private void logRecord(String action, String tableName, long accountId, UserAccounts userAccount) {
        logRecord(action, tableName, accountId, userAccount, getCallingUid());
    }

    private void logRecord(String action, String tableName, long accountId, UserAccounts userAccount, int callingUid) {
        AnonymousClass1LogRecordTask logTask = new AnonymousClass1LogRecordTask(action, tableName, accountId, userAccount, callingUid, (long) userAccount.debugDbInsertionPoint);
        userAccount.debugDbInsertionPoint = (userAccount.debugDbInsertionPoint + 1) % 64;
        this.mHandler.post(logTask);
    }

    private void initializeDebugDbSizeAndCompileSqlStatementForLogging(UserAccounts userAccount) {
        userAccount.debugDbInsertionPoint = userAccount.accountsDb.calculateDebugTableInsertionPoint();
        userAccount.statementForLogging = userAccount.accountsDb.compileSqlStatementForLogging();
    }

    public IBinder onBind(Intent intent) {
        return asBinder();
    }

    private static boolean scanArgs(String[] args, String value) {
        if (args != null) {
            for (String arg : args) {
                if (value.equals(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, fout)) {
            boolean z = scanArgs(args, "--checkin") || scanArgs(args, "-c");
            boolean isCheckinRequest = z;
            PrintWriter ipw = new IndentingPrintWriter(fout, "  ");
            for (UserInfo user : getUserManager().getUsers()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("User ");
                stringBuilder.append(user);
                stringBuilder.append(":");
                ipw.println(stringBuilder.toString());
                ipw.increaseIndent();
                dumpUser(getUserAccounts(user.id), fd, ipw, args, isCheckinRequest);
                ipw.println();
                ipw.decreaseIndent();
            }
        }
    }

    private void doNotification(UserAccounts accounts, Account account, CharSequence message, Intent intent, String packageName, int userId) {
        Account account2 = account;
        CharSequence charSequence = message;
        Intent intent2 = intent;
        String str = packageName;
        int i = userId;
        long identityToken = clearCallingIdentity();
        try {
            if (Log.isLoggable(TAG, 2)) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("doNotification: ");
                stringBuilder.append(charSequence);
                stringBuilder.append(" intent:");
                stringBuilder.append(intent2);
                Log.v(str2, stringBuilder.toString());
            }
            if (intent.getComponent() == null || !GrantCredentialsPermissionActivity.class.getName().equals(intent.getComponent().getClassName())) {
                Context contextForUser = getContextForUser(new UserHandle(i));
                NotificationId id = getSigninRequiredNotificationId(accounts, account);
                intent2.addCategory(id.mTag);
                String notificationTitleFormat = contextForUser.getText(17040607).toString();
                Bitmap bmp = BitmapFactory.decodeResource(this.mContext.getResources(), 33751687);
                Builder contentText = new Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setWhen(0).setSmallIcon(17301642).setLargeIcon(bmp).setColor(contextForUser.getColor(17170784)).setContentTitle(String.format(notificationTitleFormat, new Object[]{account2.name})).setContentText(charSequence);
                Builder builder = contentText;
                installNotification(id, builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent2, 268435456, null, new UserHandle(i))).build(), str, i);
            } else {
                createNoCredentialsPermissionNotification(account2, intent2, str, i);
            }
            restoreCallingIdentity(identityToken);
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
    }

    private void installNotification(NotificationId id, Notification notification, String packageName, int userId) {
        long token = clearCallingIdentity();
        try {
            try {
                this.mInjector.getNotificationManager().enqueueNotificationWithTag(packageName, packageName, id.mTag, id.mId, notification, userId);
            } catch (RemoteException e) {
            }
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void cancelNotification(NotificationId id, UserHandle user) {
        cancelNotification(id, this.mContext.getPackageName(), user);
    }

    private void cancelNotification(NotificationId id, String packageName, UserHandle user) {
        long identityToken = clearCallingIdentity();
        try {
            this.mInjector.getNotificationManager().cancelNotificationWithTag(packageName, id.mTag, id.mId, user.getIdentifier());
        } catch (RemoteException e) {
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
        }
        restoreCallingIdentity(identityToken);
    }

    private boolean isPermittedForPackage(String packageName, int uid, int userId, String... permissions) {
        long identity = Binder.clearCallingIdentity();
        try {
            IPackageManager pm = ActivityThread.getPackageManager();
            for (String perm : permissions) {
                if (pm.checkPermission(perm, packageName, userId) == 0) {
                    int opCode = AppOpsManager.permissionToOpCode(perm);
                    if (opCode == -1 || this.mAppOpsManager.noteOpNoThrow(opCode, uid, packageName) == 0) {
                        Binder.restoreCallingIdentity(identity);
                        return true;
                    }
                }
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
        Binder.restoreCallingIdentity(identity);
        return false;
    }

    private boolean isPermitted(String opPackageName, int callingUid, String... permissions) {
        for (String perm : permissions) {
            if (this.mContext.checkCallingOrSelfPermission(perm) == 0) {
                if (Log.isLoggable(TAG, 2)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  caller uid ");
                    stringBuilder.append(callingUid);
                    stringBuilder.append(" has ");
                    stringBuilder.append(perm);
                    Log.v(str, stringBuilder.toString());
                }
                int opCode = AppOpsManager.permissionToOpCode(perm);
                if (opCode == -1 || this.mAppOpsManager.noteOpNoThrow(opCode, callingUid, opPackageName) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int handleIncomingUser(int userId) {
        try {
            return ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, null);
        } catch (RemoteException e) {
            return userId;
        }
    }

    private boolean isPrivileged(int callingUid) {
        long identityToken = Binder.clearCallingIdentity();
        try {
            String[] packages = this.mPackageManager.getPackagesForUid(callingUid);
            if (packages == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No packages for callingUid ");
                stringBuilder.append(callingUid);
                Log.d(str, stringBuilder.toString());
                Binder.restoreCallingIdentity(identityToken);
                return false;
            }
            for (String name : packages) {
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(name, 0);
                if (!(packageInfo == null || (packageInfo.applicationInfo.privateFlags & 8) == 0)) {
                    Binder.restoreCallingIdentity(identityToken);
                    return true;
                }
            }
            Binder.restoreCallingIdentity(identityToken);
            return false;
        } catch (NameNotFoundException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Package not found ");
            stringBuilder2.append(e.getMessage());
            Log.d(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private boolean permissionIsGranted(Account account, String authTokenType, int callerUid, int userId) {
        String str;
        StringBuilder stringBuilder;
        if (UserHandle.getAppId(callerUid) == 1000) {
            if (Log.isLoggable(TAG, 2)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Access to ");
                stringBuilder.append(account);
                stringBuilder.append(" granted calling uid is system");
                Log.v(str, stringBuilder.toString());
            }
            return true;
        } else if (isPrivileged(callerUid)) {
            if (Log.isLoggable(TAG, 2)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Access to ");
                stringBuilder.append(account);
                stringBuilder.append(" granted calling uid ");
                stringBuilder.append(callerUid);
                stringBuilder.append(" privileged");
                Log.v(str, stringBuilder.toString());
            }
            return true;
        } else if (account != null && isAccountManagedByCaller(account.type, callerUid, userId)) {
            if (Log.isLoggable(TAG, 2)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Access to ");
                stringBuilder.append(account);
                stringBuilder.append(" granted calling uid ");
                stringBuilder.append(callerUid);
                stringBuilder.append(" manages the account");
                Log.v(str, stringBuilder.toString());
            }
            return true;
        } else if (account == null || !hasExplicitlyGrantedPermission(account, authTokenType, callerUid)) {
            if (Log.isLoggable(TAG, 2)) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Access to ");
                stringBuilder2.append(account);
                stringBuilder2.append(" not granted for uid ");
                stringBuilder2.append(callerUid);
                Log.v(str, stringBuilder2.toString());
            }
            return false;
        } else {
            if (Log.isLoggable(TAG, 2)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Access to ");
                stringBuilder.append(account);
                stringBuilder.append(" granted calling uid ");
                stringBuilder.append(callerUid);
                stringBuilder.append(" user granted access");
                Log.v(str, stringBuilder.toString());
            }
            return true;
        }
    }

    private boolean isAccountVisibleToCaller(String accountType, int callingUid, int userId, String opPackageName) {
        if (accountType == null) {
            return false;
        }
        return getTypesVisibleToCaller(callingUid, userId, opPackageName).contains(accountType);
    }

    private boolean checkGetAccountsPermission(String packageName, int uid, int userId) {
        return isPermittedForPackage(packageName, uid, userId, "android.permission.GET_ACCOUNTS", "android.permission.GET_ACCOUNTS_PRIVILEGED");
    }

    private boolean checkReadContactsPermission(String packageName, int uid, int userId) {
        return isPermittedForPackage(packageName, uid, userId, "android.permission.READ_CONTACTS");
    }

    private boolean accountTypeManagesContacts(String accountType, int userId) {
        if (accountType == null) {
            return false;
        }
        long identityToken = Binder.clearCallingIdentity();
        try {
            Collection<ServiceInfo<AuthenticatorDescription>> serviceInfos = this.mAuthenticatorCache.getAllServices(userId);
            for (ServiceInfo<AuthenticatorDescription> serviceInfo : serviceInfos) {
                if (accountType.equals(((AuthenticatorDescription) serviceInfo.type).type)) {
                    return isPermittedForPackage(((AuthenticatorDescription) serviceInfo.type).packageName, serviceInfo.uid, userId, "android.permission.WRITE_CONTACTS");
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private int checkPackageSignature(String accountType, int callingUid, int userId) {
        if (accountType == null) {
            return 0;
        }
        long identityToken = Binder.clearCallingIdentity();
        try {
            Collection<ServiceInfo<AuthenticatorDescription>> serviceInfos = this.mAuthenticatorCache.getAllServices(userId);
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            for (ServiceInfo<AuthenticatorDescription> serviceInfo : serviceInfos) {
                if (accountType.equals(((AuthenticatorDescription) serviceInfo.type).type)) {
                    if (serviceInfo.uid == callingUid) {
                        return 2;
                    }
                    if (pmi.hasSignatureCapability(serviceInfo.uid, callingUid, 16)) {
                        return 1;
                    }
                }
            }
            return 0;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private boolean isAccountManagedByCaller(String accountType, int callingUid, int userId) {
        if (accountType == null) {
            return false;
        }
        return getTypesManagedByCaller(callingUid, userId).contains(accountType);
    }

    private List<String> getTypesVisibleToCaller(int callingUid, int userId, String opPackageName) {
        return getTypesForCaller(callingUid, userId, true);
    }

    private List<String> getTypesManagedByCaller(int callingUid, int userId) {
        return getTypesForCaller(callingUid, userId, false);
    }

    private List<String> getTypesForCaller(int callingUid, int userId, boolean isOtherwisePermitted) {
        List<String> managedAccountTypes = new ArrayList();
        long identityToken = Binder.clearCallingIdentity();
        try {
            Collection<ServiceInfo<AuthenticatorDescription>> serviceInfos = this.mAuthenticatorCache.getAllServices(userId);
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            for (ServiceInfo<AuthenticatorDescription> serviceInfo : serviceInfos) {
                if (isOtherwisePermitted || pmi.hasSignatureCapability(serviceInfo.uid, callingUid, 16)) {
                    managedAccountTypes.add(((AuthenticatorDescription) serviceInfo.type).type);
                }
            }
            return managedAccountTypes;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private boolean isAccountPresentForCaller(String accountName, String accountType) {
        if (getUserAccountsForCaller().accountCache.containsKey(accountType)) {
            for (Account account : (Account[]) getUserAccountsForCaller().accountCache.get(accountType)) {
                if (account.name.equals(accountName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void checkManageUsersPermission(String message) {
        if (ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", Binder.getCallingUid(), -1, true) != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("You need MANAGE_USERS permission to: ");
            stringBuilder.append(message);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private static void checkManageOrCreateUsersPermission(String message) {
        if (ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", Binder.getCallingUid(), -1, true) != 0 && ActivityManager.checkComponentPermission("android.permission.CREATE_USERS", Binder.getCallingUid(), -1, true) != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("You need MANAGE_USERS or CREATE_USERS permission to: ");
            stringBuilder.append(message);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private boolean hasExplicitlyGrantedPermission(Account account, String authTokenType, int callerUid) {
        if (UserHandle.getAppId(callerUid) == 1000) {
            return true;
        }
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(callerUid));
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                long grantsCount;
                if (authTokenType != null) {
                    grantsCount = accounts.accountsDb.findMatchingGrantsCount(callerUid, authTokenType, account);
                } else {
                    grantsCount = accounts.accountsDb.findMatchingGrantsCountAnyToken(callerUid, account);
                }
                boolean permissionGranted = grantsCount > 0;
                if (permissionGranted || !ActivityManager.isRunningInTestHarness()) {
                    return permissionGranted;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("no credentials permission for usage of ");
                stringBuilder.append(account);
                stringBuilder.append(", ");
                stringBuilder.append(authTokenType);
                stringBuilder.append(" by uid ");
                stringBuilder.append(callerUid);
                stringBuilder.append(" but ignoring since device is in test harness.");
                Log.d(str, stringBuilder.toString());
                return true;
            }
        }
    }

    private boolean isSystemUid(int callingUid) {
        long ident = Binder.clearCallingIdentity();
        try {
            String[] packages = this.mPackageManager.getPackagesForUid(callingUid);
            if (packages != null) {
                for (String name : packages) {
                    PackageInfo packageInfo = this.mPackageManager.getPackageInfo(name, 0);
                    if (!(packageInfo == null || (packageInfo.applicationInfo.flags & 1) == 0)) {
                        Binder.restoreCallingIdentity(ident);
                        return true;
                    }
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No known packages with uid ");
                stringBuilder.append(callingUid);
                Log.w(str, stringBuilder.toString());
            }
            Binder.restoreCallingIdentity(ident);
            return false;
        } catch (NameNotFoundException e) {
            Log.w(TAG, String.format("Could not find package [%s]", new Object[]{name}), e);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void checkReadAccountsPermitted(int callingUid, String accountType, int userId, String opPackageName) {
        if (!isAccountVisibleToCaller(accountType, callingUid, userId, opPackageName)) {
            String msg = String.format("caller uid %s cannot access %s accounts", new Object[]{Integer.valueOf(callingUid), accountType});
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  ");
            stringBuilder.append(msg);
            Log.w(TAG, stringBuilder.toString());
            throw new SecurityException(msg);
        }
    }

    private boolean canUserModifyAccounts(int userId, int callingUid) {
        if (!isProfileOwner(callingUid) && getUserManager().getUserRestrictions(new UserHandle(userId)).getBoolean("no_modify_accounts")) {
            return false;
        }
        return true;
    }

    private boolean canUserModifyAccountsForType(int userId, String accountType, int callingUid) {
        if (isProfileOwner(callingUid)) {
            return true;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (dpm == null) {
            return true;
        }
        String[] typesArray = dpm.getAccountTypesWithManagementDisabledAsUser(userId);
        if (typesArray == null) {
            return true;
        }
        for (String forbiddenType : typesArray) {
            if (forbiddenType.equals(accountType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isProfileOwner(int uid) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        return dpmi != null && dpmi.isActiveAdminWithPolicy(uid, -1);
    }

    public void updateAppPermission(Account account, String authTokenType, int uid, boolean value) throws RemoteException {
        if (UserHandle.getAppId(getCallingUid()) != 1000) {
            throw new SecurityException();
        } else if (value) {
            grantAppPermission(account, authTokenType, uid);
        } else {
            revokeAppPermission(account, authTokenType, uid);
        }
    }

    void grantAppPermission(Account account, String authTokenType, int uid) {
        if (account == null || authTokenType == null) {
            Log.e(TAG, "grantAppPermission: called with invalid arguments", new Exception());
            return;
        }
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                long accountId = accounts.accountsDb.findDeAccountId(account);
                if (accountId >= 0) {
                    accounts.accountsDb.insertGrant(accountId, authTokenType, uid);
                }
                cancelNotification(getCredentialPermissionNotificationId(account, authTokenType, uid), UserHandle.of(accounts.userId));
                cancelAccountAccessRequestNotificationIfNeeded(account, uid, true);
            }
        }
        Iterator it = this.mAppPermissionChangeListeners.iterator();
        while (it.hasNext()) {
            this.mHandler.post(new -$$Lambda$AccountManagerService$nCdu9dc3c8qBwJIwS0ZQk2waXfY((OnAppPermissionChangeListener) it.next(), account, uid));
        }
    }

    private void revokeAppPermission(Account account, String authTokenType, int uid) {
        if (account == null || authTokenType == null) {
            Log.e(TAG, "revokeAppPermission: called with invalid arguments", new Exception());
            return;
        }
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                accounts.accountsDb.beginTransaction();
                try {
                    long accountId = accounts.accountsDb.findDeAccountId(account);
                    if (accountId >= 0) {
                        accounts.accountsDb.deleteGrantsByAccountIdAuthTokenTypeAndUid(accountId, authTokenType, (long) uid);
                        accounts.accountsDb.setTransactionSuccessful();
                    }
                    cancelNotification(getCredentialPermissionNotificationId(account, authTokenType, uid), UserHandle.of(accounts.userId));
                } finally {
                    accounts.accountsDb.endTransaction();
                }
            }
        }
        Iterator it = this.mAppPermissionChangeListeners.iterator();
        while (it.hasNext()) {
            this.mHandler.post(new -$$Lambda$AccountManagerService$b-wmW_X7TIC2Bc_zEKaPtyELmHY((OnAppPermissionChangeListener) it.next(), account, uid));
        }
    }

    private void removeAccountFromCacheLocked(UserAccounts accounts, Account account) {
        Account[] oldAccountsForType = (Account[]) accounts.accountCache.get(account.type);
        if (oldAccountsForType != null) {
            ArrayList<Account> newAccountsList = new ArrayList();
            for (Account curAccount : oldAccountsForType) {
                if (!curAccount.equals(account)) {
                    newAccountsList.add(curAccount);
                }
            }
            if (newAccountsList.isEmpty()) {
                accounts.accountCache.remove(account.type);
            } else {
                accounts.accountCache.put(account.type, (Account[]) newAccountsList.toArray(new Account[newAccountsList.size()]));
            }
        }
        accounts.userDataCache.remove(account);
        accounts.authTokenCache.remove(account);
        accounts.previousNameCache.remove(account);
        accounts.visibilityCache.remove(account);
    }

    private Account insertAccountIntoCacheLocked(UserAccounts accounts, Account account) {
        String token;
        Account[] accountsForType = (Account[]) accounts.accountCache.get(account.type);
        int oldLength = accountsForType != null ? accountsForType.length : 0;
        Account[] newAccountsForType = new Account[(oldLength + 1)];
        if (accountsForType != null) {
            System.arraycopy(accountsForType, 0, newAccountsForType, 0, oldLength);
        }
        if (account.getAccessId() != null) {
            token = account.getAccessId();
        } else {
            token = UUID.randomUUID().toString();
        }
        newAccountsForType[oldLength] = new Account(account, token);
        accounts.accountCache.put(account.type, newAccountsForType);
        return newAccountsForType[oldLength];
    }

    private Account[] filterAccounts(UserAccounts accounts, Account[] unfiltered, int callingUid, String callingPackage, boolean includeManagedNotVisible) {
        String visibilityFilterPackage = callingPackage;
        if (visibilityFilterPackage == null) {
            visibilityFilterPackage = getPackageNameForUid(callingUid);
        }
        Map<Account, Integer> firstPass = new LinkedHashMap();
        for (Account account : unfiltered) {
            int visibility = resolveAccountVisibility(account, visibilityFilterPackage, accounts).intValue();
            if (visibility == 1 || visibility == 2 || (includeManagedNotVisible && visibility == 4)) {
                firstPass.put(account, Integer.valueOf(visibility));
            }
        }
        Map<Account, Integer> secondPass = filterSharedAccounts(accounts, firstPass, callingUid, callingPackage);
        return (Account[]) secondPass.keySet().toArray(new Account[secondPass.size()]);
    }

    private Map<Account, Integer> filterSharedAccounts(UserAccounts userAccounts, Map<Account, Integer> unfiltered, int callingUid, String callingPackage) {
        int i = callingUid;
        String str = callingPackage;
        if (getUserManager() == null || userAccounts == null || userAccounts.userId < 0 || i == 1000) {
            return unfiltered;
        }
        UserInfo user = getUserManager().getUserInfo(userAccounts.userId);
        if (user == null || !user.isRestricted()) {
            return unfiltered;
        }
        String packageName;
        String[] packages = this.mPackageManager.getPackagesForUid(i);
        if (packages == null) {
            packages = new String[0];
        }
        String[] packages2 = packages;
        String visibleList = this.mContext.getResources().getString(17039766);
        for (String packageName2 : packages2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(";");
            stringBuilder.append(packageName2);
            stringBuilder.append(";");
            if (visibleList.contains(stringBuilder.toString())) {
                return unfiltered;
            }
        }
        Account[] sharedAccounts = getSharedAccountsAsUser(userAccounts.userId);
        if (ArrayUtils.isEmpty(sharedAccounts)) {
            return unfiltered;
        }
        packageName2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (str == null) {
            for (String packageName3 : packages2) {
                PackageInfo pi = this.mPackageManager.getPackageInfo(packageName3, 0);
                if (pi != null && pi.restrictedAccountType != null) {
                    packageName2 = pi.restrictedAccountType;
                    break;
                }
            }
        } else {
            try {
                PackageInfo pi2 = this.mPackageManager.getPackageInfo(str, 0);
                if (!(pi2 == null || pi2.restrictedAccountType == null)) {
                    packageName2 = pi2.restrictedAccountType;
                }
            } catch (NameNotFoundException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Package not found ");
                stringBuilder2.append(e.getMessage());
                Log.d(str2, stringBuilder2.toString());
            }
        }
        Map<Account, Integer> filtered = new LinkedHashMap();
        for (Entry<Account, Integer> entry : unfiltered.entrySet()) {
            Account account = (Account) entry.getKey();
            if (account.type.equals(packageName2)) {
                filtered.put(account, (Integer) entry.getValue());
            } else {
                boolean found = false;
                int length = sharedAccounts.length;
                int i2 = 0;
                while (i2 < length) {
                    if (sharedAccounts[i2].equals(account)) {
                        found = true;
                        break;
                    }
                    i2++;
                    i = callingUid;
                }
                if (!found) {
                    filtered.put(account, (Integer) entry.getValue());
                }
            }
            i = callingUid;
        }
        return filtered;
    }

    protected Account[] getAccountsFromCache(UserAccounts userAccounts, String accountType, int callingUid, String callingPackage, boolean includeManagedNotVisible) {
        Preconditions.checkState(Thread.holdsLock(userAccounts.cacheLock) ^ 1, "Method should not be called with cacheLock");
        if (accountType != null) {
            Account[] accounts;
            synchronized (userAccounts.cacheLock) {
                accounts = (Account[]) userAccounts.accountCache.get(accountType);
            }
            if (accounts == null) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            return filterAccounts(userAccounts, (Account[]) Arrays.copyOf(accounts, accounts.length), callingUid, callingPackage, includeManagedNotVisible);
        }
        int totalLength = 0;
        synchronized (userAccounts.cacheLock) {
            for (Account[] accounts2 : userAccounts.accountCache.values()) {
                totalLength += accounts2.length;
            }
            if (totalLength == 0) {
                Account[] accountArr = EMPTY_ACCOUNT_ARRAY;
                return accountArr;
            }
            Account[] accountsArray = new Account[totalLength];
            totalLength = 0;
            for (Account[] accounts22 : userAccounts.accountCache.values()) {
                System.arraycopy(accounts22, 0, accountsArray, totalLength, accounts22.length);
                totalLength += accounts22.length;
            }
            return filterAccounts(userAccounts, accountsArray, callingUid, callingPackage, includeManagedNotVisible);
        }
    }

    protected void writeUserDataIntoCacheLocked(UserAccounts accounts, Account account, String key, String value) {
        Map<String, String> userDataForAccount = (Map) accounts.userDataCache.get(account);
        if (userDataForAccount == null) {
            userDataForAccount = accounts.accountsDb.findUserExtrasForAccount(account);
            accounts.userDataCache.put(account, userDataForAccount);
        }
        if (value == null) {
            userDataForAccount.remove(key);
        } else {
            userDataForAccount.put(key, value);
        }
    }

    protected String readCachedTokenInternal(UserAccounts accounts, Account account, String tokenType, String callingPackage, byte[] pkgSigDigest) {
        String str;
        synchronized (accounts.cacheLock) {
            str = accounts.accountTokenCaches.get(account, tokenType, callingPackage, pkgSigDigest);
        }
        return str;
    }

    protected void writeAuthTokenIntoCacheLocked(UserAccounts accounts, Account account, String key, String value) {
        Map<String, String> authTokensForAccount = (Map) accounts.authTokenCache.get(account);
        if (authTokensForAccount == null) {
            authTokensForAccount = accounts.accountsDb.findAuthTokensByAccount(account);
            accounts.authTokenCache.put(account, authTokensForAccount);
        }
        if (value == null) {
            authTokensForAccount.remove(key);
        } else {
            authTokensForAccount.put(key, value);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0018, code:
            r1 = r5.dbLock;
     */
    /* JADX WARNING: Missing block: B:10:0x001a, code:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            r0 = r5.cacheLock;
     */
    /* JADX WARNING: Missing block: B:13:0x001d, code:
            monitor-enter(r0);
     */
    /* JADX WARNING: Missing block: B:15:?, code:
            r2 = (java.util.Map) com.android.server.accounts.AccountManagerService.UserAccounts.access$1200(r5).get(r6);
     */
    /* JADX WARNING: Missing block: B:16:0x0028, code:
            if (r2 != null) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:17:0x002a, code:
            r2 = r5.accountsDb.findAuthTokensByAccount(r6);
            com.android.server.accounts.AccountManagerService.UserAccounts.access$1200(r5).put(r6, r2);
     */
    /* JADX WARNING: Missing block: B:18:0x0038, code:
            r3 = (java.lang.String) r2.get(r7);
     */
    /* JADX WARNING: Missing block: B:19:0x003e, code:
            monitor-exit(r0);
     */
    /* JADX WARNING: Missing block: B:21:?, code:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:22:0x0040, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected String readAuthTokenInternal(UserAccounts accounts, Account account, String authTokenType) {
        synchronized (accounts.cacheLock) {
            Map<String, String> authTokensForAccount = (Map) accounts.authTokenCache.get(account);
            if (authTokensForAccount != null) {
                String str = (String) authTokensForAccount.get(authTokenType);
                return str;
            }
        }
    }

    private String readUserDataInternal(UserAccounts accounts, Account account, String key) {
        Map<String, String> userDataForAccount;
        synchronized (accounts.cacheLock) {
            userDataForAccount = (Map) accounts.userDataCache.get(account);
        }
        if (userDataForAccount == null) {
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    userDataForAccount = (Map) accounts.userDataCache.get(account);
                    if (userDataForAccount == null) {
                        userDataForAccount = accounts.accountsDb.findUserExtrasForAccount(account);
                        accounts.userDataCache.put(account, userDataForAccount);
                    }
                }
            }
        }
        return (String) userDataForAccount.get(key);
    }

    private Context getContextForUser(UserHandle user) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
        } catch (NameNotFoundException e) {
            return this.mContext;
        }
    }

    private void sendResponse(IAccountManagerResponse response, Bundle result) {
        try {
            response.onResult(result);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }

    private void sendErrorResponse(IAccountManagerResponse response, int errorCode, String errorMessage) {
        try {
            response.onError(errorCode, errorMessage);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }
}
