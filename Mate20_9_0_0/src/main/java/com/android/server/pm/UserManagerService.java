package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IStopUserCallback;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IProgressListener.Stub;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManager.EnforcingUser;
import android.os.UserManagerInternal;
import android.os.UserManagerInternal.UserRestrictionsListener;
import android.os.storage.StorageManager;
import android.provider.Settings.Secure;
import android.security.GateKeeper;
import android.service.gatekeeper.IGateKeeperService;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Flog;
import android.util.IntArray;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IAppOpsService;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.SystemService;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class UserManagerService extends AbsUserManagerService {
    private static final int ALLOWED_FLAGS_FOR_CREATE_USERS_PERMISSION = 812;
    private static final String ATTR_CREATION_TIME = "created";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_GUEST_TO_REMOVE = "guestToRemove";
    private static final String ATTR_ICON_PATH = "icon";
    private static final String ATTR_ID = "id";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_LAST_LOGGED_IN_FINGERPRINT = "lastLoggedInFingerprint";
    private static final String ATTR_LAST_LOGGED_IN_TIME = "lastLoggedIn";
    private static final String ATTR_MULTIPLE = "m";
    private static final String ATTR_NEXT_SERIAL_NO = "nextSerialNumber";
    private static final String ATTR_PARTIAL = "partial";
    private static final String ATTR_PROFILE_BADGE = "profileBadge";
    private static final String ATTR_PROFILE_GROUP_ID = "profileGroupId";
    private static final String ATTR_RESTRICTED_PROFILE_PARENT_ID = "restrictedProfileParentId";
    private static final String ATTR_SEED_ACCOUNT_NAME = "seedAccountName";
    private static final String ATTR_SEED_ACCOUNT_TYPE = "seedAccountType";
    private static final String ATTR_SERIAL_NO = "serialNumber";
    private static final String ATTR_TYPE_BOOLEAN = "b";
    private static final String ATTR_TYPE_BUNDLE = "B";
    private static final String ATTR_TYPE_BUNDLE_ARRAY = "BA";
    private static final String ATTR_TYPE_INTEGER = "i";
    private static final String ATTR_TYPE_STRING = "s";
    private static final String ATTR_TYPE_STRING_ARRAY = "sa";
    private static final String ATTR_USER_VERSION = "version";
    private static final String ATTR_VALUE_TYPE = "type";
    static final boolean DBG = false;
    private static final boolean DBG_WITH_STACKTRACE = false;
    private static final long EPOCH_PLUS_30_YEARS = 946080000000L;
    private static final String LOG_TAG = "UserManagerService";
    @VisibleForTesting
    static final int MAX_MANAGED_PROFILES = 1;
    @VisibleForTesting
    static final int MAX_RECENTLY_REMOVED_IDS_SIZE = 100;
    @VisibleForTesting
    static final int MAX_USER_ID = 21474;
    @VisibleForTesting
    static final int MIN_USER_ID = 10;
    private static final boolean RELEASE_DELETED_USER_ID = false;
    private static final int REPAIR_MODE_USER_ID = 127;
    private static final String RESTRICTIONS_FILE_PREFIX = "res_";
    private static final String SUW_FRP_STATE = "hw_suw_frp_state";
    private static final String TAG_ACCOUNT = "account";
    private static final String TAG_DEVICE_OWNER_USER_ID = "deviceOwnerUserId";
    private static final String TAG_DEVICE_POLICY_GLOBAL_RESTRICTIONS = "device_policy_global_restrictions";
    private static final String TAG_DEVICE_POLICY_RESTRICTIONS = "device_policy_restrictions";
    private static final String TAG_ENTRY = "entry";
    private static final String TAG_GLOBAL_RESTRICTION_OWNER_ID = "globalRestrictionOwnerUserId";
    private static final String TAG_GUEST_RESTRICTIONS = "guestRestrictions";
    private static final String TAG_NAME = "name";
    private static final String TAG_RESTRICTIONS = "restrictions";
    private static final String TAG_SEED_ACCOUNT_OPTIONS = "seedAccountOptions";
    private static final String TAG_USER = "user";
    private static final String TAG_USERS = "users";
    private static final String TAG_VALUE = "value";
    private static final String TRON_DEMO_CREATED = "users_demo_created";
    private static final String TRON_GUEST_CREATED = "users_guest_created";
    private static final String TRON_USER_CREATED = "users_user_created";
    private static final String USER_INFO_DIR;
    private static final String USER_LIST_FILENAME = "userlist.xml";
    private static final String USER_PHOTO_FILENAME = "photo.png";
    private static final String USER_PHOTO_FILENAME_TMP = "photo.png.tmp";
    private static final int USER_VERSION = 7;
    static final int WRITE_USER_DELAY = 2000;
    static final int WRITE_USER_MSG = 1;
    private static final String XML_SUFFIX = ".xml";
    private static final IBinder mUserRestriconToken = new Binder();
    private static UserManagerService sInstance;
    private final String ACTION_DISABLE_QUIET_MODE_AFTER_UNLOCK;
    private final boolean DEFAULT_VALUE;
    private final String PROPERTIES_PRIVACY_SUPPORT_IUDF;
    private boolean isOwnerNameChanged;
    boolean isSupportISec;
    private IAppOpsService mAppOpsService;
    private final Object mAppRestrictionsLock;
    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mAppliedUserRestrictions;
    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mBaseUserRestrictions;
    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mCachedEffectiveUserRestrictions;
    private final LinkedList<Integer> mClonedProfileRecentlyRemovedIds;
    private final Context mContext;
    @GuardedBy("mRestrictionsLock")
    private int mDeviceOwnerUserId;
    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mDevicePolicyGlobalUserRestrictions;
    @GuardedBy("mRestrictionsLock")
    private final SparseArray<Bundle> mDevicePolicyLocalUserRestrictions;
    private final BroadcastReceiver mDisableQuietModeCallback;
    @GuardedBy("mUsersLock")
    private boolean mForceEphemeralUsers;
    @GuardedBy("mGuestRestrictions")
    private final Bundle mGuestRestrictions;
    private final Handler mHandler;
    private boolean mHasClonedProfile;
    @GuardedBy("mUsersLock")
    private boolean mIsDeviceManaged;
    @GuardedBy("mUsersLock")
    private final SparseBooleanArray mIsUserManaged;
    private final LocalService mLocalService;
    private final LockPatternUtils mLockPatternUtils;
    @GuardedBy("mPackagesLock")
    private int mNextSerialNumber;
    private final Object mPackagesLock;
    protected final PackageManagerService mPm;
    @GuardedBy("mUsersLock")
    private final LinkedList<Integer> mRecentlyRemovedIds;
    @GuardedBy("mUsersLock")
    private final SparseBooleanArray mRemovingUserIds;
    private final Object mRestrictionsLock;
    private final UserDataPreparer mUserDataPreparer;
    @GuardedBy("mUsersLock")
    private int[] mUserIds;
    private final File mUserListFile;
    @GuardedBy("mUserRestrictionsListeners")
    private final ArrayList<UserRestrictionsListener> mUserRestrictionsListeners;
    @GuardedBy("mUserStates")
    private final SparseIntArray mUserStates;
    private int mUserVersion;
    @GuardedBy("mUsersLock")
    private final SparseArray<UserData> mUsers;
    private final File mUsersDir;
    private final Object mUsersLock;

    private class DisableQuietModeUserUnlockedCallback extends Stub {
        private final IntentSender mTarget;

        public DisableQuietModeUserUnlockedCallback(IntentSender target) {
            Preconditions.checkNotNull(target);
            this.mTarget = target;
        }

        public void onStarted(int id, Bundle extras) {
        }

        public void onProgress(int id, int progress, Bundle extras) {
        }

        public void onFinished(int id, Bundle extras) {
            try {
                UserManagerService.this.mContext.startIntentSender(this.mTarget, null, 0, 0, 0);
            } catch (SendIntentException e) {
                Slog.e(UserManagerService.LOG_TAG, "Failed to start the target in the callback", e);
            }
        }
    }

    private class LocalService extends UserManagerInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(UserManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void setDevicePolicyUserRestrictions(int userId, Bundle restrictions, boolean isDeviceOwner, int cameraRestrictionScope) {
            UserManagerService.this.setDevicePolicyUserRestrictionsInner(userId, restrictions, isDeviceOwner, cameraRestrictionScope);
        }

        public Bundle getBaseUserRestrictions(int userId) {
            Bundle bundle;
            synchronized (UserManagerService.this.mRestrictionsLock) {
                bundle = (Bundle) UserManagerService.this.mBaseUserRestrictions.get(userId);
            }
            return bundle;
        }

        public void setBaseUserRestrictionsByDpmsForMigration(int userId, Bundle baseRestrictions) {
            synchronized (UserManagerService.this.mRestrictionsLock) {
                if (UserManagerService.this.updateRestrictionsIfNeededLR(userId, new Bundle(baseRestrictions), UserManagerService.this.mBaseUserRestrictions)) {
                    UserManagerService.this.invalidateEffectiveUserRestrictionsLR(userId);
                }
            }
            UserData userData = UserManagerService.this.getUserDataNoChecks(userId);
            synchronized (UserManagerService.this.mPackagesLock) {
                if (userData != null) {
                    UserManagerService.this.writeUserLP(userData);
                } else {
                    String str = UserManagerService.LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("UserInfo not found for ");
                    stringBuilder.append(userId);
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }

        public boolean getUserRestriction(int userId, String key) {
            return UserManagerService.this.getUserRestrictions(userId).getBoolean(key);
        }

        public void addUserRestrictionsListener(UserRestrictionsListener listener) {
            synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                UserManagerService.this.mUserRestrictionsListeners.add(listener);
            }
        }

        public void removeUserRestrictionsListener(UserRestrictionsListener listener) {
            synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                UserManagerService.this.mUserRestrictionsListeners.remove(listener);
            }
        }

        public void setDeviceManaged(boolean isManaged) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mIsDeviceManaged = isManaged;
            }
        }

        public void setUserManaged(int userId, boolean isManaged) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mIsUserManaged.put(userId, isManaged);
            }
        }

        public void setUserIcon(int userId, Bitmap bitmap) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (UserManagerService.this.mPackagesLock) {
                    UserData userData = UserManagerService.this.getUserDataNoChecks(userId);
                    if (userData == null || userData.info.partial) {
                        String str = UserManagerService.LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setUserIcon: unknown user #");
                        stringBuilder.append(userId);
                        Slog.w(str, stringBuilder.toString());
                        Binder.restoreCallingIdentity(ident);
                        return;
                    }
                    UserManagerService.this.writeBitmapLP(userData.info, bitmap);
                    UserManagerService.this.writeUserLP(userData);
                    UserManagerService.this.sendUserInfoChangedBroadcast(userId);
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setForceEphemeralUsers(boolean forceEphemeralUsers) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mForceEphemeralUsers = forceEphemeralUsers;
            }
        }

        public void removeAllUsers() {
            if (ActivityManager.getCurrentUser() == 0) {
                UserManagerService.this.removeNonSystemUsers();
                return;
            }
            BroadcastReceiver userSwitchedReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (intent.getIntExtra("android.intent.extra.user_handle", -10000) == 0) {
                        UserManagerService.this.mContext.unregisterReceiver(this);
                        UserManagerService.this.removeNonSystemUsers();
                    }
                }
            };
            IntentFilter userSwitchedFilter = new IntentFilter();
            userSwitchedFilter.addAction("android.intent.action.USER_SWITCHED");
            UserManagerService.this.mContext.registerReceiver(userSwitchedReceiver, userSwitchedFilter, null, UserManagerService.this.mHandler);
            ((ActivityManager) UserManagerService.this.mContext.getSystemService("activity")).switchUser(0);
        }

        public void onEphemeralUserStop(int userId) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo userInfo = UserManagerService.this.getUserInfoLU(userId);
                if (userInfo != null && userInfo.isEphemeral()) {
                    userInfo.flags |= 64;
                    if (userInfo.isGuest()) {
                        userInfo.guestToRemove = true;
                    }
                }
            }
        }

        public UserInfo createUserEvenWhenDisallowed(String name, int flags, String[] disallowedPackages) {
            UserInfo user = UserManagerService.this.createUserInternalUnchecked(name, flags, -10000, disallowedPackages);
            if (!(user == null || user.isAdmin() || user.isDemo())) {
                UserManagerService.this.setUserRestriction("no_sms", true, user.id);
                UserManagerService.this.setUserRestriction("no_outgoing_calls", true, user.id);
            }
            return user;
        }

        public boolean removeUserEvenWhenDisallowed(int userId) {
            return UserManagerService.this.removeUserUnchecked(userId);
        }

        public boolean isUserRunning(int userId) {
            boolean z;
            synchronized (UserManagerService.this.mUserStates) {
                z = UserManagerService.this.mUserStates.get(userId, -1) >= 0;
            }
            return z;
        }

        public void setUserState(int userId, int userState) {
            synchronized (UserManagerService.this.mUserStates) {
                UserManagerService.this.mUserStates.put(userId, userState);
            }
        }

        public void removeUserState(int userId) {
            synchronized (UserManagerService.this.mUserStates) {
                UserManagerService.this.mUserStates.delete(userId);
            }
        }

        public int[] getUserIds() {
            return UserManagerService.this.getUserIds();
        }

        public boolean isUserUnlockingOrUnlocked(int userId) {
            int state;
            synchronized (UserManagerService.this.mUserStates) {
                state = UserManagerService.this.mUserStates.get(userId, -1);
            }
            if (state == 4 || state == 5) {
                return StorageManager.isUserKeyUnlocked(userId);
            }
            boolean z = state == 2 || state == 3;
            return z;
        }

        public boolean isUserUnlocked(int userId) {
            int state;
            synchronized (UserManagerService.this.mUserStates) {
                state = UserManagerService.this.mUserStates.get(userId, -1);
            }
            if (state == 4 || state == 5) {
                return StorageManager.isUserKeyUnlocked(userId);
            }
            return state == 3;
        }

        public boolean isUserInitialized(int userId) {
            return (getUserInfo(userId).flags & 16) != 0;
        }

        public boolean exists(int userId) {
            return UserManagerService.this.getUserInfoNoChecks(userId) != null;
        }

        /* JADX WARNING: Missing block: B:29:0x007c, code:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean isProfileAccessible(int callingUserId, int targetUserId, String debugMsg, boolean throwSecurityException) {
            if (targetUserId == callingUserId) {
                return true;
            }
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo callingUserInfo = UserManagerService.this.getUserInfoLU(callingUserId);
                if ((callingUserInfo == null || callingUserInfo.isManagedProfile()) && throwSecurityException) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(debugMsg);
                    stringBuilder.append(" for another profile ");
                    stringBuilder.append(targetUserId);
                    stringBuilder.append(" from ");
                    stringBuilder.append(callingUserId);
                    throw new SecurityException(stringBuilder.toString());
                }
                UserInfo targetUserInfo = UserManagerService.this.getUserInfoLU(targetUserId);
                if (targetUserInfo == null || !targetUserInfo.isEnabled()) {
                    if (throwSecurityException) {
                        String str = UserManagerService.LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(debugMsg);
                        stringBuilder2.append(" for disabled profile ");
                        stringBuilder2.append(targetUserId);
                        stringBuilder2.append(" from ");
                        stringBuilder2.append(callingUserId);
                        Slog.w(str, stringBuilder2.toString());
                    }
                } else if (targetUserInfo.profileGroupId != -10000 && targetUserInfo.profileGroupId == callingUserInfo.profileGroupId) {
                    return true;
                } else if (throwSecurityException) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(debugMsg);
                    stringBuilder3.append(" for unrelated profile ");
                    stringBuilder3.append(targetUserId);
                    throw new SecurityException(stringBuilder3.toString());
                } else {
                    return false;
                }
            }
        }

        public int getProfileParentId(int userId) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo profileParent = UserManagerService.this.getProfileParentLU(userId);
                if (profileParent == null) {
                    return userId;
                }
                int i = profileParent.id;
                return i;
            }
        }

        public boolean isSettingRestrictedForUser(String setting, int userId, String value, int callingUid) {
            return UserRestrictionsUtils.isSettingRestrictedForUser(UserManagerService.this.mContext, setting, userId, value, callingUid);
        }

        public boolean isClonedProfile(int userId) {
            boolean z;
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo userInfo = UserManagerService.this.getUserInfoLU(userId);
                z = userInfo != null && userInfo.isClonedProfile();
            }
            return z;
        }

        public UserInfo getUserInfo(int userId) {
            UserInfo access$2300;
            synchronized (UserManagerService.this.mUsersLock) {
                access$2300 = UserManagerService.this.getUserInfoLU(userId);
            }
            return access$2300;
        }

        public boolean hasClonedProfile() {
            return UserManagerService.this.mHasClonedProfile;
        }

        public UserInfo findClonedProfile() {
            synchronized (UserManagerService.this.mUsersLock) {
                int size = UserManagerService.this.mUsers.size();
                int i = 0;
                while (i < size) {
                    UserInfo user = ((UserData) UserManagerService.this.mUsers.valueAt(i)).info;
                    if (!user.isClonedProfile() || UserManagerService.this.mRemovingUserIds.get(user.id)) {
                        i++;
                    } else {
                        return user;
                    }
                }
                return null;
            }
        }

        public boolean isRemovingUser(int userId) {
            return UserManagerService.this.mRemovingUserIds.get(userId) && userId >= 128 && userId < 148;
        }

        /* JADX WARNING: Missing block: B:23:0x0052, code:
            return r2;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean isSameGroupForClone(int callingUserId, int targetUserId) {
            synchronized (UserManagerService.this.mUsersLock) {
                int size = UserManagerService.this.mUsers.size();
                boolean z = false;
                int i = 0;
                while (i < size) {
                    UserInfo user = ((UserData) UserManagerService.this.mUsers.valueAt(i)).info;
                    if (!user.isClonedProfile() || UserManagerService.this.mRemovingUserIds.get(user.id)) {
                        i++;
                    } else if ((callingUserId == user.id && targetUserId == user.profileGroupId) || ((targetUserId == user.id && callingUserId == user.profileGroupId) || (callingUserId == user.id && targetUserId == user.id))) {
                        z = true;
                    }
                }
                return false;
            }
        }
    }

    final class MainHandler extends Handler {
        MainHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                removeMessages(1, msg.obj);
                synchronized (UserManagerService.this.mPackagesLock) {
                    UserData userData = UserManagerService.this.getUserDataNoChecks(((UserData) msg.obj).info.id);
                    if (userData != null) {
                        UserManagerService.this.writeUserLP(userData);
                    }
                }
            }
        }
    }

    private class Shell extends ShellCommand {
        private Shell() {
        }

        /* synthetic */ Shell(UserManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public int onCommand(String cmd) {
            return UserManagerService.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("User manager (user) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            pw.println("  list");
            pw.println("    Prints all users on the system.");
        }
    }

    @VisibleForTesting
    static class UserData {
        String account;
        UserInfo info;
        boolean persistSeedData;
        String seedAccountName;
        PersistableBundle seedAccountOptions;
        String seedAccountType;
        long startRealtime;
        long unlockRealtime;

        UserData() {
        }

        void clearSeedAccountData() {
            this.seedAccountName = null;
            this.seedAccountType = null;
            this.seedAccountOptions = null;
            this.persistSeedData = false;
        }
    }

    public static class LifeCycle extends SystemService {
        private UserManagerService mUms;

        public LifeCycle(Context context) {
            super(context);
        }

        public void onStart() {
            this.mUms = UserManagerService.getInstance();
            publishBinderService(UserManagerService.TAG_USER, this.mUms);
        }

        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mUms.cleanupPartialUsers();
            }
        }

        public void onStartUser(int userHandle) {
            synchronized (this.mUms.mUsersLock) {
                UserData user = this.mUms.getUserDataLU(userHandle);
                if (user != null) {
                    user.startRealtime = SystemClock.elapsedRealtime();
                }
            }
        }

        public void onUnlockUser(int userHandle) {
            synchronized (this.mUms.mUsersLock) {
                UserData user = this.mUms.getUserDataLU(userHandle);
                if (user != null) {
                    user.unlockRealtime = SystemClock.elapsedRealtime();
                }
            }
        }

        public void onStopUser(int userHandle) {
            synchronized (this.mUms.mUsersLock) {
                UserData user = this.mUms.getUserDataLU(userHandle);
                if (user != null) {
                    user.startRealtime = 0;
                    user.unlockRealtime = 0;
                }
            }
        }
    }

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        */
    private android.content.pm.UserInfo createUserInternalUnchecked(java.lang.String r31, int r32, int r33, java.lang.String[] r34) {
        /*
        r30 = this;
        r1 = r30;
        r2 = r32;
        r3 = r33;
        r0 = com.android.server.storage.DeviceStorageMonitorInternal.class;
        r0 = com.android.server.LocalServices.getService(r0);
        r4 = r0;
        r4 = (com.android.server.storage.DeviceStorageMonitorInternal) r4;
        r0 = r4.isMemoryLow();
        r5 = 0;
        if (r0 == 0) goto L_0x001e;
    L_0x0016:
        r0 = "UserManagerService";
        r6 = "Cannot add user. Not enough space on disk.";
        android.util.Log.w(r0, r6);
        return r5;
    L_0x001e:
        r0 = r2 & 4;
        if (r0 == 0) goto L_0x0024;
    L_0x0022:
        r0 = 1;
        goto L_0x0025;
    L_0x0024:
        r0 = 0;
    L_0x0025:
        r8 = r0;
        r0 = r2 & 32;
        if (r0 == 0) goto L_0x002c;
    L_0x002a:
        r0 = 1;
        goto L_0x002d;
    L_0x002c:
        r0 = 0;
    L_0x002d:
        r9 = r0;
        r0 = r2 & 8;
        if (r0 == 0) goto L_0x0034;
    L_0x0032:
        r0 = 1;
        goto L_0x0035;
    L_0x0034:
        r0 = 0;
    L_0x0035:
        r10 = r0;
        r0 = r2 & 512;
        if (r0 == 0) goto L_0x003c;
    L_0x003a:
        r0 = 1;
        goto L_0x003d;
    L_0x003c:
        r0 = 0;
    L_0x003d:
        r11 = r0;
        r0 = 134217728; // 0x8000000 float:3.85186E-34 double:6.63123685E-316;
        r0 = r0 & r2;
        if (r0 == 0) goto L_0x0045;
    L_0x0043:
        r0 = 1;
        goto L_0x0046;
    L_0x0045:
        r0 = 0;
    L_0x0046:
        r12 = r0;
        r13 = android.os.Binder.clearCallingIdentity();
        r0 = 67108864; // 0x4000000 float:1.5046328E-36 double:3.31561842E-316;
        r0 = r0 & r2;
        if (r0 == 0) goto L_0x0052;
    L_0x0050:
        r0 = 1;
        goto L_0x0053;
    L_0x0052:
        r0 = 0;
    L_0x0053:
        r15 = r0;
        r0 = 900; // 0x384 float:1.261E-42 double:4.447E-321;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r7 = "Create user internal, flags= ";
        r5.append(r7);
        r7 = java.lang.Integer.toHexString(r32);
        r5.append(r7);
        r7 = " parentId= ";
        r5.append(r7);
        r5.append(r3);
        r7 = " isGuest= ";
        r5.append(r7);
        r5.append(r8);
        r7 = " isManagedProfile= ";
        r5.append(r7);
        r5.append(r9);
        r5 = r5.toString();
        android.util.Flog.i(r0, r5);
        r5 = r1.mPackagesLock;	 Catch:{ all -> 0x0437 }
        monitor-enter(r5);	 Catch:{ all -> 0x0437 }
        r7 = 0;
        r0 = r1.mContext;	 Catch:{ all -> 0x0423 }
        r0 = r0.getContentResolver();	 Catch:{ all -> 0x0423 }
        r6 = "hw_suw_frp_state";	 Catch:{ all -> 0x0423 }
        r16 = r4;
        r4 = 0;
        r0 = android.provider.Settings.Secure.getInt(r0, r6, r4);	 Catch:{ all -> 0x0416 }
        r4 = 1;
        if (r0 != r4) goto L_0x00c7;
    L_0x009c:
        r0 = r1.mContext;	 Catch:{ all -> 0x00b9 }
        r0 = r0.getContentResolver();	 Catch:{ all -> 0x00b9 }
        r4 = "device_provisioned";	 Catch:{ all -> 0x00b9 }
        r6 = 0;	 Catch:{ all -> 0x00b9 }
        r0 = android.provider.Settings.Global.getInt(r0, r4, r6);	 Catch:{ all -> 0x00b9 }
        r4 = 1;	 Catch:{ all -> 0x00b9 }
        if (r0 == r4) goto L_0x00c7;	 Catch:{ all -> 0x00b9 }
    L_0x00ac:
        r0 = "UserManagerService";	 Catch:{ all -> 0x00b9 }
        r4 = "can not create new user before FRP unlock";	 Catch:{ all -> 0x00b9 }
        android.util.Log.w(r0, r4);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x00b9:
        r0 = move-exception;
        r20 = r2;
        r25 = r11;
        r26 = r12;
        r1 = r13;
        r14 = r31;
        r11 = r34;
        goto L_0x0431;
    L_0x00c7:
        if (r12 == 0) goto L_0x00e6;
    L_0x00c9:
        r0 = r1.mContext;	 Catch:{ all -> 0x00b9 }
        r0 = r0.getContentResolver();	 Catch:{ all -> 0x00b9 }
        r4 = "device_provisioned";	 Catch:{ all -> 0x00b9 }
        r6 = 0;	 Catch:{ all -> 0x00b9 }
        r0 = android.provider.Settings.Global.getInt(r0, r4, r6);	 Catch:{ all -> 0x00b9 }
        r4 = 1;	 Catch:{ all -> 0x00b9 }
        if (r0 == r4) goto L_0x00e6;	 Catch:{ all -> 0x00b9 }
    L_0x00d9:
        r0 = "UserManagerService";	 Catch:{ all -> 0x00b9 }
        r4 = "can not create repair mode user during start-up guide process";	 Catch:{ all -> 0x00b9 }
        android.util.Log.w(r0, r4);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x00e6:
        r0 = -10000; // 0xffffffffffffd8f0 float:NaN double:NaN;
        if (r3 == r0) goto L_0x00fe;
    L_0x00ea:
        r4 = r1.mUsersLock;	 Catch:{ all -> 0x00b9 }
        monitor-enter(r4);	 Catch:{ all -> 0x00b9 }
        r6 = r1.getUserDataLU(r3);	 Catch:{ all -> 0x00b9 }
        r7 = r6;	 Catch:{ all -> 0x00b9 }
        monitor-exit(r4);	 Catch:{ all -> 0x00b9 }
        if (r7 != 0) goto L_0x00fe;
    L_0x00f5:
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x00fb:
        r0 = move-exception;
        monitor-exit(r4);	 Catch:{ all -> 0x00b9 }
        throw r0;	 Catch:{ all -> 0x00b9 }
    L_0x00fe:
        if (r9 == 0) goto L_0x0123;	 Catch:{ all -> 0x00b9 }
    L_0x0100:
        r4 = 0;	 Catch:{ all -> 0x00b9 }
        r6 = r1.canAddMoreManagedProfiles(r3, r4);	 Catch:{ all -> 0x00b9 }
        if (r6 != 0) goto L_0x0123;	 Catch:{ all -> 0x00b9 }
    L_0x0107:
        r0 = "UserManagerService";	 Catch:{ all -> 0x00b9 }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00b9 }
        r4.<init>();	 Catch:{ all -> 0x00b9 }
        r6 = "Cannot add more managed profiles for user ";	 Catch:{ all -> 0x00b9 }
        r4.append(r6);	 Catch:{ all -> 0x00b9 }
        r4.append(r3);	 Catch:{ all -> 0x00b9 }
        r4 = r4.toString();	 Catch:{ all -> 0x00b9 }
        android.util.Log.e(r0, r4);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x0123:
        if (r12 != 0) goto L_0x0139;
    L_0x0125:
        if (r8 != 0) goto L_0x0139;
    L_0x0127:
        if (r9 != 0) goto L_0x0139;
    L_0x0129:
        if (r11 != 0) goto L_0x0139;
    L_0x012b:
        r4 = r30.isUserLimitReached();	 Catch:{ all -> 0x00b9 }
        if (r4 == 0) goto L_0x0139;	 Catch:{ all -> 0x00b9 }
    L_0x0131:
        if (r15 != 0) goto L_0x0139;	 Catch:{ all -> 0x00b9 }
    L_0x0133:
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x0139:
        if (r8 == 0) goto L_0x0147;
    L_0x013b:
        r4 = r30.findCurrentGuestUser();	 Catch:{ all -> 0x00b9 }
        if (r4 == 0) goto L_0x0147;	 Catch:{ all -> 0x00b9 }
    L_0x0141:
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x0147:
        if (r10 == 0) goto L_0x015e;
    L_0x0149:
        r4 = android.os.UserManager.isSplitSystemUser();	 Catch:{ all -> 0x00b9 }
        if (r4 != 0) goto L_0x015e;	 Catch:{ all -> 0x00b9 }
    L_0x014f:
        if (r3 == 0) goto L_0x015e;	 Catch:{ all -> 0x00b9 }
    L_0x0151:
        r0 = "UserManagerService";	 Catch:{ all -> 0x00b9 }
        r4 = "Cannot add restricted profile - parent user must be owner";	 Catch:{ all -> 0x00b9 }
        android.util.Log.w(r0, r4);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x015e:
        if (r10 == 0) goto L_0x0199;
    L_0x0160:
        r4 = android.os.UserManager.isSplitSystemUser();	 Catch:{ all -> 0x00b9 }
        if (r4 == 0) goto L_0x0199;	 Catch:{ all -> 0x00b9 }
    L_0x0166:
        if (r7 != 0) goto L_0x0175;	 Catch:{ all -> 0x00b9 }
    L_0x0168:
        r0 = "UserManagerService";	 Catch:{ all -> 0x00b9 }
        r4 = "Cannot add restricted profile - parent user must be specified";	 Catch:{ all -> 0x00b9 }
        android.util.Log.w(r0, r4);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x0175:
        r4 = r7.info;	 Catch:{ all -> 0x00b9 }
        r4 = r4.canHaveProfile();	 Catch:{ all -> 0x00b9 }
        if (r4 != 0) goto L_0x0199;	 Catch:{ all -> 0x00b9 }
    L_0x017d:
        r0 = "UserManagerService";	 Catch:{ all -> 0x00b9 }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00b9 }
        r4.<init>();	 Catch:{ all -> 0x00b9 }
        r6 = "Cannot add restricted profile - profiles cannot be created for the specified parent user id ";	 Catch:{ all -> 0x00b9 }
        r4.append(r6);	 Catch:{ all -> 0x00b9 }
        r4.append(r3);	 Catch:{ all -> 0x00b9 }
        r4 = r4.toString();	 Catch:{ all -> 0x00b9 }
        android.util.Log.w(r0, r4);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r5);	 Catch:{ all -> 0x00b9 }
        android.os.Binder.restoreCallingIdentity(r13);
        r0 = 0;
        return r0;
    L_0x0199:
        r4 = android.os.UserManager.isSplitSystemUser();	 Catch:{ all -> 0x0416 }
        if (r4 == 0) goto L_0x01b9;
    L_0x019f:
        if (r8 != 0) goto L_0x01b9;
    L_0x01a1:
        if (r9 != 0) goto L_0x01b9;
    L_0x01a3:
        r4 = r30.getPrimaryUser();	 Catch:{ all -> 0x00b9 }
        if (r4 != 0) goto L_0x01b9;	 Catch:{ all -> 0x00b9 }
    L_0x01a9:
        r2 = r2 | 1;	 Catch:{ all -> 0x00b9 }
        r4 = r1.mUsersLock;	 Catch:{ all -> 0x00b9 }
        monitor-enter(r4);	 Catch:{ all -> 0x00b9 }
        r6 = r1.mIsDeviceManaged;	 Catch:{ all -> 0x00b9 }
        if (r6 != 0) goto L_0x01b4;	 Catch:{ all -> 0x00b9 }
    L_0x01b2:
        r2 = r2 | 2;	 Catch:{ all -> 0x00b9 }
    L_0x01b4:
        monitor-exit(r4);	 Catch:{ all -> 0x00b9 }
        goto L_0x01b9;	 Catch:{ all -> 0x00b9 }
    L_0x01b6:
        r0 = move-exception;	 Catch:{ all -> 0x00b9 }
        monitor-exit(r4);	 Catch:{ all -> 0x00b9 }
        throw r0;	 Catch:{ all -> 0x00b9 }
    L_0x01b9:
        if (r12 == 0) goto L_0x01be;
    L_0x01bb:
        r4 = 127; // 0x7f float:1.78E-43 double:6.27E-322;
        goto L_0x01c2;
    L_0x01be:
        r4 = r1.getNextAvailableId(r15);	 Catch:{ all -> 0x0408 }
    L_0x01c2:
        r6 = android.os.Environment.getUserSystemDirectory(r4);	 Catch:{ all -> 0x0408 }
        r6.mkdirs();	 Catch:{ all -> 0x0408 }
        r6 = android.content.res.Resources.getSystem();	 Catch:{ all -> 0x0408 }
        r0 = 17956979; // 0x1120073 float:2.6816287E-38 double:8.8719264E-317;	 Catch:{ all -> 0x0408 }
        r0 = r6.getBoolean(r0);	 Catch:{ all -> 0x0408 }
        r6 = r0;
        r17 = r13;
        r13 = r1.mUsersLock;	 Catch:{ all -> 0x03f9 }
        monitor-enter(r13);	 Catch:{ all -> 0x03f9 }
        if (r8 == 0) goto L_0x01de;
    L_0x01dc:
        if (r6 != 0) goto L_0x01fe;
    L_0x01de:
        r0 = r1.mForceEphemeralUsers;	 Catch:{ all -> 0x03e5 }
        if (r0 != 0) goto L_0x01fe;
    L_0x01e2:
        if (r7 == 0) goto L_0x0201;
    L_0x01e4:
        r0 = r7.info;	 Catch:{ all -> 0x01ed }
        r0 = r0.isEphemeral();	 Catch:{ all -> 0x01ed }
        if (r0 == 0) goto L_0x0201;
    L_0x01ec:
        goto L_0x01fe;
    L_0x01ed:
        r0 = move-exception;
        r14 = r31;
        r20 = r2;
        r19 = r6;
        r25 = r11;
        r26 = r12;
        r1 = r17;
        r11 = r34;
        goto L_0x03f5;
    L_0x01fe:
        r0 = r2 | 256;
        r2 = r0;
    L_0x0201:
        r0 = new android.content.pm.UserInfo;	 Catch:{ all -> 0x03d5 }
        r14 = r31;
        r19 = r6;
        r6 = 0;
        r0.<init>(r4, r14, r6, r2);	 Catch:{ all -> 0x03c9 }
        r6 = r1.mNextSerialNumber;	 Catch:{ all -> 0x03c9 }
        r20 = r2;
        r2 = r6 + 1;
        r1.mNextSerialNumber = r2;	 Catch:{ all -> 0x03bf }
        r0.serialNumber = r6;	 Catch:{ all -> 0x03bf }
        r21 = java.lang.System.currentTimeMillis();	 Catch:{ all -> 0x03bf }
        r23 = 946080000000; // 0xdc46c32800 float:24980.0 double:4.674256262175E-312;
        r2 = (r21 > r23 ? 1 : (r21 == r23 ? 0 : -1));
        if (r2 <= 0) goto L_0x0229;
    L_0x0222:
        r25 = r11;
        r26 = r12;
        r11 = r21;
        goto L_0x0231;
    L_0x0229:
        r23 = 0;
        r25 = r11;
        r26 = r12;
        r11 = r23;
    L_0x0231:
        r0.creationTime = r11;	 Catch:{ all -> 0x03b9 }
        r2 = 1;	 Catch:{ all -> 0x03b9 }
        r0.partial = r2;	 Catch:{ all -> 0x03b9 }
        r2 = android.os.Build.FINGERPRINT;	 Catch:{ all -> 0x03b9 }
        r0.lastLoggedInFingerprint = r2;	 Catch:{ all -> 0x03b9 }
        if (r9 == 0) goto L_0x024e;
    L_0x023c:
        r2 = -10000; // 0xffffffffffffd8f0 float:NaN double:NaN;
        if (r3 == r2) goto L_0x024e;
    L_0x0240:
        r2 = r1.getFreeProfileBadgeLU(r3);	 Catch:{ all -> 0x0247 }
        r0.profileBadge = r2;	 Catch:{ all -> 0x0247 }
        goto L_0x024e;
    L_0x0247:
        r0 = move-exception;
        r11 = r34;
        r1 = r17;
        goto L_0x03f5;
    L_0x024e:
        r2 = new com.android.server.pm.UserManagerService$UserData;	 Catch:{ all -> 0x03b9 }
        r2.<init>();	 Catch:{ all -> 0x03b9 }
        r2.info = r0;	 Catch:{ all -> 0x03b9 }
        r6 = r1.mUsers;	 Catch:{ all -> 0x03b9 }
        r6.put(r4, r2);	 Catch:{ all -> 0x03b9 }
        if (r15 == 0) goto L_0x0266;
    L_0x025c:
        r6 = "UserManagerService";	 Catch:{ all -> 0x0247 }
        r11 = "create cloned profile user, set mHasClonedProfile true.";	 Catch:{ all -> 0x0247 }
        android.util.Slog.i(r6, r11);	 Catch:{ all -> 0x0247 }
        r6 = 1;	 Catch:{ all -> 0x0247 }
        r1.mHasClonedProfile = r6;	 Catch:{ all -> 0x0247 }
    L_0x0266:
        monitor-exit(r13);	 Catch:{ all -> 0x03b9 }
        r1.writeUserLP(r2);	 Catch:{ all -> 0x03b2 }
        r30.writeUserListLP();	 Catch:{ all -> 0x03b2 }
        if (r7 == 0) goto L_0x02b1;
    L_0x026f:
        if (r9 != 0) goto L_0x0290;
    L_0x0271:
        if (r15 == 0) goto L_0x0274;
    L_0x0273:
        goto L_0x0290;
    L_0x0274:
        if (r10 == 0) goto L_0x02b1;
    L_0x0276:
        r6 = r7.info;	 Catch:{ all -> 0x02aa }
        r6 = r6.restrictedProfileParentId;	 Catch:{ all -> 0x02aa }
        r11 = -10000; // 0xffffffffffffd8f0 float:NaN double:NaN;	 Catch:{ all -> 0x02aa }
        if (r6 != r11) goto L_0x0289;	 Catch:{ all -> 0x02aa }
    L_0x027e:
        r6 = r7.info;	 Catch:{ all -> 0x02aa }
        r11 = r7.info;	 Catch:{ all -> 0x02aa }
        r11 = r11.id;	 Catch:{ all -> 0x02aa }
        r6.restrictedProfileParentId = r11;	 Catch:{ all -> 0x02aa }
        r1.writeUserLP(r7);	 Catch:{ all -> 0x02aa }
    L_0x0289:
        r6 = r7.info;	 Catch:{ all -> 0x02aa }
        r6 = r6.restrictedProfileParentId;	 Catch:{ all -> 0x02aa }
        r0.restrictedProfileParentId = r6;	 Catch:{ all -> 0x02aa }
        goto L_0x02b1;	 Catch:{ all -> 0x02aa }
    L_0x0290:
        r6 = r7.info;	 Catch:{ all -> 0x02aa }
        r6 = r6.profileGroupId;	 Catch:{ all -> 0x02aa }
        r11 = -10000; // 0xffffffffffffd8f0 float:NaN double:NaN;	 Catch:{ all -> 0x02aa }
        if (r6 != r11) goto L_0x02a3;	 Catch:{ all -> 0x02aa }
    L_0x0298:
        r6 = r7.info;	 Catch:{ all -> 0x02aa }
        r11 = r7.info;	 Catch:{ all -> 0x02aa }
        r11 = r11.id;	 Catch:{ all -> 0x02aa }
        r6.profileGroupId = r11;	 Catch:{ all -> 0x02aa }
        r1.writeUserLP(r7);	 Catch:{ all -> 0x02aa }
    L_0x02a3:
        r6 = r7.info;	 Catch:{ all -> 0x02aa }
        r6 = r6.profileGroupId;	 Catch:{ all -> 0x02aa }
        r0.profileGroupId = r6;	 Catch:{ all -> 0x02aa }
        goto L_0x02b1;
    L_0x02aa:
        r0 = move-exception;
        r11 = r34;
        r1 = r17;
        goto L_0x0431;
    L_0x02b1:
        monitor-exit(r5);	 Catch:{ all -> 0x03b2 }
        r5 = r0;
        r0 = r1.mContext;	 Catch:{ all -> 0x03ab }
        r6 = android.os.storage.StorageManager.class;	 Catch:{ all -> 0x03ab }
        r0 = r0.getSystemService(r6);	 Catch:{ all -> 0x03ab }
        r0 = (android.os.storage.StorageManager) r0;	 Catch:{ all -> 0x03ab }
        r6 = r0;	 Catch:{ all -> 0x03ab }
        r0 = r1.isSupportISec;	 Catch:{ all -> 0x03ab }
        if (r0 == 0) goto L_0x02d3;
    L_0x02c2:
        r0 = r5.serialNumber;	 Catch:{ all -> 0x02cc }
        r7 = r5.isEphemeral();	 Catch:{ all -> 0x02cc }
        r6.createUserKeyISec(r4, r0, r7);	 Catch:{ all -> 0x02cc }
        goto L_0x02dc;
    L_0x02cc:
        r0 = move-exception;
        r11 = r34;
    L_0x02cf:
        r1 = r17;
        goto L_0x0445;
    L_0x02d3:
        r0 = r5.serialNumber;	 Catch:{ all -> 0x03ab }
        r7 = r5.isEphemeral();	 Catch:{ all -> 0x03ab }
        r6.createUserKey(r4, r0, r7);	 Catch:{ all -> 0x03ab }
    L_0x02dc:
        r0 = r1.mUserDataPreparer;	 Catch:{ all -> 0x03ab }
        r7 = r5.serialNumber;	 Catch:{ all -> 0x03ab }
        r11 = 3;	 Catch:{ all -> 0x03ab }
        r0.prepareUserData(r4, r7, r11);	 Catch:{ all -> 0x03ab }
        r0 = r1.mPm;	 Catch:{ all -> 0x03ab }
        r11 = r34;
        r0.createNewUser(r4, r11);	 Catch:{ all -> 0x03a9 }
        r0 = 0;	 Catch:{ all -> 0x03a9 }
        r5.partial = r0;	 Catch:{ all -> 0x03a9 }
        r7 = r1.mPackagesLock;	 Catch:{ all -> 0x03a9 }
        monitor-enter(r7);	 Catch:{ all -> 0x03a9 }
        r1.writeUserLP(r2);	 Catch:{ all -> 0x03a0, all -> 0x0397, all -> 0x0433 }
        monitor-exit(r7);	 Catch:{ all -> 0x03a0, all -> 0x0397, all -> 0x0433 }
        r30.updateUserIds();	 Catch:{ all -> 0x03a9 }
        r0 = new android.os.Bundle;	 Catch:{ all -> 0x03a9 }
        r0.<init>();	 Catch:{ all -> 0x03a9 }
        r7 = r0;
        if (r8 == 0) goto L_0x030f;
    L_0x0300:
        r12 = r1.mGuestRestrictions;	 Catch:{ all -> 0x030d }
        monitor-enter(r12);	 Catch:{ all -> 0x030d }
        r0 = r1.mGuestRestrictions;	 Catch:{ all -> 0x030d }
        r7.putAll(r0);	 Catch:{ all -> 0x030d }
        monitor-exit(r12);	 Catch:{ all -> 0x030d }
        goto L_0x030f;	 Catch:{ all -> 0x030d }
    L_0x030a:
        r0 = move-exception;	 Catch:{ all -> 0x030d }
        monitor-exit(r12);	 Catch:{ all -> 0x030d }
        throw r0;	 Catch:{ all -> 0x030d }
    L_0x030d:
        r0 = move-exception;
        goto L_0x02cf;
    L_0x030f:
        r12 = r1.mRestrictionsLock;	 Catch:{ all -> 0x03a9 }
        monitor-enter(r12);	 Catch:{ all -> 0x03a9 }
        r0 = r1.mBaseUserRestrictions;	 Catch:{ all -> 0x03a0, all -> 0x0397, all -> 0x0433 }
        r0.append(r4, r7);	 Catch:{ all -> 0x03a0, all -> 0x0397, all -> 0x0433 }
        monitor-exit(r12);	 Catch:{ all -> 0x03a0, all -> 0x0397, all -> 0x0433 }
        r0 = r1.mPm;	 Catch:{ all -> 0x0392 }
        r0.onNewUserCreated(r4);	 Catch:{ all -> 0x0392 }
        r0 = r1.mContext;	 Catch:{ all -> 0x0392 }
        r12 = 0;	 Catch:{ all -> 0x0392 }
        android.hwtheme.HwThemeManager.applyDefaultHwTheme(r12, r0, r4);	 Catch:{ all -> 0x0392 }
        r0 = new android.content.Intent;	 Catch:{ all -> 0x0392 }
        r12 = "android.intent.action.USER_ADDED";	 Catch:{ all -> 0x0392 }
        r0.<init>(r12);	 Catch:{ all -> 0x0392 }
        r12 = "android.intent.extra.user_handle";	 Catch:{ all -> 0x0392 }
        r0.putExtra(r12, r4);	 Catch:{ all -> 0x0392 }
        r12 = r1.mContext;	 Catch:{ all -> 0x0392 }
        r13 = android.os.UserHandle.ALL;	 Catch:{ all -> 0x0392 }
        r3 = "android.permission.MANAGE_USERS";	 Catch:{ all -> 0x0392 }
        r12.sendBroadcastAsUser(r0, r13, r3);	 Catch:{ all -> 0x0392 }
        r3 = r1.mContext;	 Catch:{ all -> 0x0392 }
        if (r8 == 0) goto L_0x0340;
    L_0x033c:
        r12 = "users_guest_created";	 Catch:{ all -> 0x030d }
        goto L_0x0349;	 Catch:{ all -> 0x030d }
    L_0x0340:
        if (r25 == 0) goto L_0x0346;	 Catch:{ all -> 0x030d }
    L_0x0342:
        r12 = "users_demo_created";	 Catch:{ all -> 0x030d }
        goto L_0x0349;
    L_0x0346:
        r12 = "users_user_created";	 Catch:{ all -> 0x0392 }
    L_0x0349:
        r13 = 1;	 Catch:{ all -> 0x0392 }
        com.android.internal.logging.MetricsLogger.count(r3, r12, r13);	 Catch:{ all -> 0x0392 }
        r3 = "ro.sf.real_lcd_density";	 Catch:{ all -> 0x0392 }
        r12 = "ro.sf.lcd_density";	 Catch:{ all -> 0x0392 }
        r13 = 0;	 Catch:{ all -> 0x0392 }
        r12 = android.os.SystemProperties.getInt(r12, r13);	 Catch:{ all -> 0x0392 }
        r3 = android.os.SystemProperties.getInt(r3, r12);	 Catch:{ all -> 0x0392 }
        r12 = "persist.sys.dpi";	 Catch:{ all -> 0x0392 }
        r12 = android.os.SystemProperties.getInt(r12, r3);	 Catch:{ all -> 0x0392 }
        r13 = "persist.sys.realdpi";	 Catch:{ all -> 0x0392 }
        r13 = android.os.SystemProperties.getInt(r13, r12);	 Catch:{ all -> 0x0392 }
        r27 = r0;	 Catch:{ all -> 0x0392 }
        r0 = r1.mContext;	 Catch:{ all -> 0x0392 }
        r0 = r0.getContentResolver();	 Catch:{ all -> 0x0392 }
        r1 = "display_density_forced";	 Catch:{ all -> 0x0392 }
        r28 = r3;	 Catch:{ all -> 0x0392 }
        r3 = java.lang.Integer.toString(r13);	 Catch:{ all -> 0x0392 }
        android.provider.Settings.Secure.putStringForUser(r0, r1, r3, r4);	 Catch:{ all -> 0x0392 }
        if (r26 == 0) goto L_0x0388;
    L_0x037f:
        r0 = "persist.sys.RepairMode";	 Catch:{ all -> 0x030d }
        r1 = "true";	 Catch:{ all -> 0x030d }
        android.os.SystemProperties.set(r0, r1);	 Catch:{ all -> 0x030d }
    L_0x0388:
        r6 = r17;
        android.os.Binder.restoreCallingIdentity(r6);
        r0 = r5;
        r1 = r2;
        r2 = r4;
        return r0;
    L_0x0392:
        r0 = move-exception;
        r1 = r17;
        goto L_0x0445;
    L_0x0397:
        r0 = move-exception;
        r29 = r2;
        r1 = r17;
    L_0x039c:
        monitor-exit(r12);	 Catch:{ all -> 0x039e }
        throw r0;	 Catch:{ all -> 0x03a0, all -> 0x0397, all -> 0x0433 }
    L_0x039e:
        r0 = move-exception;
        goto L_0x039c;
    L_0x03a0:
        r0 = move-exception;
        r29 = r2;
        r1 = r17;
    L_0x03a5:
        monitor-exit(r7);	 Catch:{ all -> 0x03a7 }
        throw r0;	 Catch:{ all -> 0x03a0, all -> 0x0397, all -> 0x0433 }
    L_0x03a7:
        r0 = move-exception;
        goto L_0x03a5;
    L_0x03a9:
        r0 = move-exception;
        goto L_0x03ae;
    L_0x03ab:
        r0 = move-exception;
        r11 = r34;
    L_0x03ae:
        r1 = r17;
        goto L_0x0445;
    L_0x03b2:
        r0 = move-exception;
        r11 = r34;
        r1 = r17;
        goto L_0x0431;
    L_0x03b9:
        r0 = move-exception;
        r11 = r34;
        r1 = r17;
        goto L_0x03f5;
    L_0x03bf:
        r0 = move-exception;
        r25 = r11;
        r26 = r12;
        r1 = r17;
        r11 = r34;
        goto L_0x03f5;
    L_0x03c9:
        r0 = move-exception;
        r20 = r2;
        r25 = r11;
        r26 = r12;
        r1 = r17;
        r11 = r34;
        goto L_0x03f5;
    L_0x03d5:
        r0 = move-exception;
        r14 = r31;
        r20 = r2;
        r19 = r6;
        r25 = r11;
        r26 = r12;
        r1 = r17;
        r11 = r34;
        goto L_0x03f5;
    L_0x03e5:
        r0 = move-exception;
        r14 = r31;
        r3 = r2;
        r19 = r6;
        r25 = r11;
        r26 = r12;
        r1 = r17;
        r11 = r34;
        r20 = r3;
    L_0x03f5:
        monitor-exit(r13);	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        throw r0;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
    L_0x03f7:
        r0 = move-exception;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        goto L_0x03f5;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
    L_0x03f9:
        r0 = move-exception;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r14 = r31;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r3 = r2;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r25 = r11;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r26 = r12;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r1 = r17;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r11 = r34;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r20 = r3;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        goto L_0x0431;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
    L_0x0408:
        r0 = move-exception;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r3 = r2;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r25 = r11;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r26 = r12;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r1 = r13;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r14 = r31;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r11 = r34;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r20 = r3;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        goto L_0x0431;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
    L_0x0416:
        r0 = move-exception;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r25 = r11;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r26 = r12;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r1 = r13;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r14 = r31;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r11 = r34;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r20 = r32;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        goto L_0x0431;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
    L_0x0423:
        r0 = move-exception;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r16 = r4;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r25 = r11;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r26 = r12;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r1 = r13;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r14 = r31;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r11 = r34;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        r20 = r32;	 Catch:{ all -> 0x03f7, all -> 0x0435 }
    L_0x0431:
        monitor-exit(r5);	 Catch:{ all -> 0x03f7, all -> 0x0435 }
        throw r0;	 Catch:{ all -> 0x03a0, all -> 0x0397, all -> 0x0433 }
    L_0x0433:
        r0 = move-exception;
        goto L_0x0445;
    L_0x0435:
        r0 = move-exception;
        goto L_0x0431;
    L_0x0437:
        r0 = move-exception;
        r16 = r4;
        r25 = r11;
        r26 = r12;
        r1 = r13;
        r14 = r31;
        r11 = r34;
        r20 = r32;
    L_0x0445:
        android.os.Binder.restoreCallingIdentity(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.UserManagerService.createUserInternalUnchecked(java.lang.String, int, int, java.lang.String[]):android.content.pm.UserInfo");
    }

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        */
    protected void dump(java.io.FileDescriptor r23, java.io.PrintWriter r24, java.lang.String[] r25) {
        /*
        r22 = this;
        r1 = r22;
        r10 = r24;
        r0 = r1.mContext;
        r2 = "UserManagerService";
        r0 = com.android.internal.util.DumpUtils.checkDumpPermission(r0, r2, r10);
        if (r0 != 0) goto L_0x000f;
    L_0x000e:
        return;
    L_0x000f:
        r11 = java.lang.System.currentTimeMillis();
        r13 = android.os.SystemClock.elapsedRealtime();
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r15 = r1.mPackagesLock;
        monitor-enter(r15);
        r8 = r1.mUsersLock;	 Catch:{ all -> 0x02c5 }
        monitor-enter(r8);	 Catch:{ all -> 0x02c5 }
        r0 = "Users:";	 Catch:{ all -> 0x02bc }
        r10.println(r0);	 Catch:{ all -> 0x02bc }
        r0 = 0;	 Catch:{ all -> 0x02bc }
    L_0x0028:
        r9 = r0;	 Catch:{ all -> 0x02bc }
        r0 = r1.mUsers;	 Catch:{ all -> 0x02bc }
        r0 = r0.size();	 Catch:{ all -> 0x02bc }
        if (r9 >= r0) goto L_0x01d6;	 Catch:{ all -> 0x02bc }
    L_0x0031:
        r0 = r1.mUsers;	 Catch:{ all -> 0x02bc }
        r0 = r0.valueAt(r9);	 Catch:{ all -> 0x02bc }
        r0 = (com.android.server.pm.UserManagerService.UserData) r0;	 Catch:{ all -> 0x02bc }
        r6 = r0;	 Catch:{ all -> 0x02bc }
        if (r6 != 0) goto L_0x0045;	 Catch:{ all -> 0x02bc }
        r20 = r8;	 Catch:{ all -> 0x02bc }
        r21 = r9;	 Catch:{ all -> 0x02bc }
        r18 = r13;	 Catch:{ all -> 0x02bc }
        goto L_0x01ac;	 Catch:{ all -> 0x02bc }
    L_0x0045:
        r0 = r6.info;	 Catch:{ all -> 0x02bc }
        r7 = r0;	 Catch:{ all -> 0x02bc }
        r0 = r7.id;	 Catch:{ all -> 0x02bc }
        r4 = r0;	 Catch:{ all -> 0x02bc }
        r0 = "  ";	 Catch:{ all -> 0x02bc }
        r10.print(r0);	 Catch:{ all -> 0x02bc }
        r10.print(r7);	 Catch:{ all -> 0x02bc }
        r0 = " serialNo=";	 Catch:{ all -> 0x02bc }
        r10.print(r0);	 Catch:{ all -> 0x02bc }
        r0 = r7.serialNumber;	 Catch:{ all -> 0x02bc }
        r10.print(r0);	 Catch:{ all -> 0x02bc }
        r0 = r1.mRemovingUserIds;	 Catch:{ all -> 0x02bc }
        r0 = r0.get(r4);	 Catch:{ all -> 0x02bc }
        if (r0 == 0) goto L_0x0072;
    L_0x0065:
        r0 = " <removing> ";	 Catch:{ all -> 0x006b }
        r10.print(r0);	 Catch:{ all -> 0x006b }
        goto L_0x0072;
    L_0x006b:
        r0 = move-exception;
        r20 = r8;
        r18 = r13;
        goto L_0x02c1;
    L_0x0072:
        r0 = r7.partial;	 Catch:{ all -> 0x02bc }
        if (r0 == 0) goto L_0x007b;
    L_0x0076:
        r0 = " <partial>";	 Catch:{ all -> 0x006b }
        r10.print(r0);	 Catch:{ all -> 0x006b }
    L_0x007b:
        r24.println();	 Catch:{ all -> 0x02bc }
        r0 = "    State: ";	 Catch:{ all -> 0x02bc }
        r10.print(r0);	 Catch:{ all -> 0x02bc }
        r2 = r1.mUserStates;	 Catch:{ all -> 0x02bc }
        monitor-enter(r2);	 Catch:{ all -> 0x02bc }
        r0 = r1.mUserStates;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = -1;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = r0.get(r4, r5);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r0;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        monitor-exit(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = com.android.server.am.UserState.stateToString(r5);	 Catch:{ all -> 0x02bc }
        r10.println(r0);	 Catch:{ all -> 0x02bc }
        r0 = "    Created: ";	 Catch:{ all -> 0x02bc }
        r10.print(r0);	 Catch:{ all -> 0x02bc }
        r0 = r7.creationTime;	 Catch:{ all -> 0x01c0 }
        r2 = r10;
        r16 = r4;
        r17 = r5;
        r4 = r11;
        r18 = r13;
        r13 = r6;
        r14 = r7;
        r6 = r0;
        dumpTimeAgo(r2, r3, r4, r6);	 Catch:{ all -> 0x01b9 }
        r0 = "    Last logged in: ";	 Catch:{ all -> 0x01b9 }
        r10.print(r0);	 Catch:{ all -> 0x01b9 }
        r0 = r14.lastLoggedInTime;	 Catch:{ all -> 0x01b9 }
        r4 = r10;
        r5 = r3;
        r6 = r11;
        r20 = r8;
        r21 = r9;
        r8 = r0;
        dumpTimeAgo(r4, r5, r6, r8);	 Catch:{ all -> 0x01b7 }
        r0 = "    Last logged in fingerprint: ";	 Catch:{ all -> 0x01b7 }
        r10.print(r0);	 Catch:{ all -> 0x01b7 }
        r0 = r14.lastLoggedInFingerprint;	 Catch:{ all -> 0x01b7 }
        r10.println(r0);	 Catch:{ all -> 0x01b7 }
        r0 = "    Start time: ";	 Catch:{ all -> 0x01b7 }
        r10.print(r0);	 Catch:{ all -> 0x01b7 }
        r8 = r13.startRealtime;	 Catch:{ all -> 0x01b7 }
        r4 = r10;	 Catch:{ all -> 0x01b7 }
        r5 = r3;	 Catch:{ all -> 0x01b7 }
        r6 = r18;	 Catch:{ all -> 0x01b7 }
        dumpTimeAgo(r4, r5, r6, r8);	 Catch:{ all -> 0x01b7 }
        r0 = "    Unlock time: ";	 Catch:{ all -> 0x01b7 }
        r10.print(r0);	 Catch:{ all -> 0x01b7 }
        r8 = r13.unlockRealtime;	 Catch:{ all -> 0x01b7 }
        r4 = r10;	 Catch:{ all -> 0x01b7 }
        r5 = r3;	 Catch:{ all -> 0x01b7 }
        r6 = r18;	 Catch:{ all -> 0x01b7 }
        dumpTimeAgo(r4, r5, r6, r8);	 Catch:{ all -> 0x01b7 }
        r0 = "    Has profile owner: ";	 Catch:{ all -> 0x01b7 }
        r10.print(r0);	 Catch:{ all -> 0x01b7 }
        r1 = r22;
        r0 = r1.mIsUserManaged;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r4 = r16;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = r0.get(r4);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.println(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = "    Restrictions:";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.println(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r2 = r1.mRestrictionsLock;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        monitor-enter(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = "      ";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r1.mBaseUserRestrictions;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r6 = r14.id;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r5.get(r6);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = (android.os.Bundle) r5;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        com.android.server.pm.UserRestrictionsUtils.dumpRestrictions(r10, r0, r5);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = "    Device policy global restrictions:";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.println(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = "      ";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r1.mDevicePolicyGlobalUserRestrictions;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r6 = r14.id;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r5.get(r6);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = (android.os.Bundle) r5;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        com.android.server.pm.UserRestrictionsUtils.dumpRestrictions(r10, r0, r5);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = "    Device policy local restrictions:";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.println(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = "      ";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r1.mDevicePolicyLocalUserRestrictions;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r6 = r14.id;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r5.get(r6);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = (android.os.Bundle) r5;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        com.android.server.pm.UserRestrictionsUtils.dumpRestrictions(r10, r0, r5);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = "    Effective restrictions:";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.println(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = "      ";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r1.mCachedEffectiveUserRestrictions;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r6 = r14.id;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = r5.get(r6);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r5 = (android.os.Bundle) r5;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        com.android.server.pm.UserRestrictionsUtils.dumpRestrictions(r10, r0, r5);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        monitor-exit(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = r13.account;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        if (r0 == 0) goto L_0x0166;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x014d:
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.<init>();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r2 = "    Account name: ";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.append(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r2 = r13.account;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.append(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = r0.toString();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.print(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r24.println();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x0166:
        r0 = r13.seedAccountName;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        if (r0 == 0) goto L_0x01ac;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x016a:
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.<init>();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r2 = "    Seed account name: ";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.append(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r2 = r13.seedAccountName;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.append(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = r0.toString();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.print(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r24.println();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = r13.seedAccountType;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        if (r0 == 0) goto L_0x01a0;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x0187:
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.<init>();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r2 = "         account type: ";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.append(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r2 = r13.seedAccountType;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0.append(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r0 = r0.toString();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.print(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r24.println();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x01a0:
        r0 = r13.seedAccountOptions;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        if (r0 == 0) goto L_0x01ac;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x01a4:
        r0 = "         account options exist";	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r10.print(r0);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r24.println();	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x01ac:
        r0 = r21 + 1;
        r13 = r18;
        r8 = r20;
        goto L_0x0028;
    L_0x01b4:
        r0 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        throw r0;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x01b7:
        r0 = move-exception;
        goto L_0x01bc;
    L_0x01b9:
        r0 = move-exception;
        r20 = r8;
    L_0x01bc:
        r1 = r22;
        goto L_0x02c1;
    L_0x01c0:
        r0 = move-exception;
        r20 = r8;
        r18 = r13;
        r1 = r22;
        goto L_0x02c1;
    L_0x01c9:
        r0 = move-exception;
        r20 = r8;
        r21 = r9;
        r18 = r13;
        r13 = r6;
        r14 = r7;
    L_0x01d2:
        monitor-exit(r2);	 Catch:{ all -> 0x01d4 }
        throw r0;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x01d4:
        r0 = move-exception;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        goto L_0x01d2;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
    L_0x01d6:
        r20 = r8;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r18 = r13;	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        monitor-exit(r20);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        r24.println();	 Catch:{ all -> 0x02ca }
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02ca }
        r0.<init>();	 Catch:{ all -> 0x02ca }
        r2 = "  Device owner id:";	 Catch:{ all -> 0x02ca }
        r0.append(r2);	 Catch:{ all -> 0x02ca }
        r2 = r1.mDeviceOwnerUserId;	 Catch:{ all -> 0x02ca }
        r0.append(r2);	 Catch:{ all -> 0x02ca }
        r0 = r0.toString();	 Catch:{ all -> 0x02ca }
        r10.println(r0);	 Catch:{ all -> 0x02ca }
        r24.println();	 Catch:{ all -> 0x02ca }
        r0 = "  Guest restrictions:";	 Catch:{ all -> 0x02ca }
        r10.println(r0);	 Catch:{ all -> 0x02ca }
        r2 = r1.mGuestRestrictions;	 Catch:{ all -> 0x02ca }
        monitor-enter(r2);	 Catch:{ all -> 0x02ca }
        r0 = "    ";	 Catch:{ all -> 0x02ca }
        r4 = r1.mGuestRestrictions;	 Catch:{ all -> 0x02ca }
        com.android.server.pm.UserRestrictionsUtils.dumpRestrictions(r10, r0, r4);	 Catch:{ all -> 0x02ca }
        monitor-exit(r2);	 Catch:{ all -> 0x02ca }
        r2 = r1.mUsersLock;	 Catch:{ all -> 0x02ca }
        monitor-enter(r2);	 Catch:{ all -> 0x02ca }
        r24.println();	 Catch:{ all -> 0x02ca }
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02ca }
        r0.<init>();	 Catch:{ all -> 0x02ca }
        r4 = "  Device managed: ";	 Catch:{ all -> 0x02ca }
        r0.append(r4);	 Catch:{ all -> 0x02ca }
        r4 = r1.mIsDeviceManaged;	 Catch:{ all -> 0x02ca }
        r0.append(r4);	 Catch:{ all -> 0x02ca }
        r0 = r0.toString();	 Catch:{ all -> 0x02ca }
        r10.println(r0);	 Catch:{ all -> 0x02ca }
        r0 = r1.mRemovingUserIds;	 Catch:{ all -> 0x02ca }
        r0 = r0.size();	 Catch:{ all -> 0x02ca }
        if (r0 <= 0) goto L_0x0244;	 Catch:{ all -> 0x02ca }
    L_0x022b:
        r24.println();	 Catch:{ all -> 0x02ca }
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02ca }
        r0.<init>();	 Catch:{ all -> 0x02ca }
        r4 = "  Recently removed userIds: ";	 Catch:{ all -> 0x02ca }
        r0.append(r4);	 Catch:{ all -> 0x02ca }
        r4 = r1.mRecentlyRemovedIds;	 Catch:{ all -> 0x02ca }
        r0.append(r4);	 Catch:{ all -> 0x02ca }
        r0 = r0.toString();	 Catch:{ all -> 0x02ca }
        r10.println(r0);	 Catch:{ all -> 0x02ca }
    L_0x0244:
        monitor-exit(r2);	 Catch:{ all -> 0x02ca }
        r2 = r1.mUserStates;	 Catch:{ all -> 0x02ca }
        monitor-enter(r2);	 Catch:{ all -> 0x02ca }
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02ca }
        r0.<init>();	 Catch:{ all -> 0x02ca }
        r4 = "  Started users state: ";	 Catch:{ all -> 0x02ca }
        r0.append(r4);	 Catch:{ all -> 0x02ca }
        r4 = r1.mUserStates;	 Catch:{ all -> 0x02ca }
        r0.append(r4);	 Catch:{ all -> 0x02ca }
        r0 = r0.toString();	 Catch:{ all -> 0x02ca }
        r10.println(r0);	 Catch:{ all -> 0x02ca }
        monitor-exit(r2);	 Catch:{ all -> 0x02ca }
        r24.println();	 Catch:{ all -> 0x02ca }
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02ca }
        r0.<init>();	 Catch:{ all -> 0x02ca }
        r2 = "  Max users: ";	 Catch:{ all -> 0x02ca }
        r0.append(r2);	 Catch:{ all -> 0x02ca }
        r2 = android.os.UserManager.getMaxSupportedUsers();	 Catch:{ all -> 0x02ca }
        r0.append(r2);	 Catch:{ all -> 0x02ca }
        r0 = r0.toString();	 Catch:{ all -> 0x02ca }
        r10.println(r0);	 Catch:{ all -> 0x02ca }
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02ca }
        r0.<init>();	 Catch:{ all -> 0x02ca }
        r2 = "  Supports switchable users: ";	 Catch:{ all -> 0x02ca }
        r0.append(r2);	 Catch:{ all -> 0x02ca }
        r2 = android.os.UserManager.supportsMultipleUsers();	 Catch:{ all -> 0x02ca }
        r0.append(r2);	 Catch:{ all -> 0x02ca }
        r0 = r0.toString();	 Catch:{ all -> 0x02ca }
        r10.println(r0);	 Catch:{ all -> 0x02ca }
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02ca }
        r0.<init>();	 Catch:{ all -> 0x02ca }
        r2 = "  All guests ephemeral: ";	 Catch:{ all -> 0x02ca }
        r0.append(r2);	 Catch:{ all -> 0x02ca }
        r2 = android.content.res.Resources.getSystem();	 Catch:{ all -> 0x02ca }
        r4 = 17956979; // 0x1120073 float:2.6816287E-38 double:8.8719264E-317;	 Catch:{ all -> 0x02ca }
        r2 = r2.getBoolean(r4);	 Catch:{ all -> 0x02ca }
        r0.append(r2);	 Catch:{ all -> 0x02ca }
        r0 = r0.toString();	 Catch:{ all -> 0x02ca }
        r10.println(r0);	 Catch:{ all -> 0x02ca }
        monitor-exit(r15);	 Catch:{ all -> 0x02ca }
        return;
    L_0x02b3:
        r0 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x02ca }
        throw r0;	 Catch:{ all -> 0x02ca }
    L_0x02b6:
        r0 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x02ca }
        throw r0;	 Catch:{ all -> 0x02ca }
    L_0x02b9:
        r0 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x02ca }
        throw r0;	 Catch:{ all -> 0x02ca }
    L_0x02bc:
        r0 = move-exception;
        r20 = r8;
        r18 = r13;
    L_0x02c1:
        monitor-exit(r20);	 Catch:{ all -> 0x01c9, all -> 0x02c3 }
        throw r0;	 Catch:{ all -> 0x02ca }
    L_0x02c3:
        r0 = move-exception;	 Catch:{ all -> 0x02ca }
        goto L_0x02c1;	 Catch:{ all -> 0x02ca }
    L_0x02c5:
        r0 = move-exception;	 Catch:{ all -> 0x02ca }
        r18 = r13;	 Catch:{ all -> 0x02ca }
    L_0x02c8:
        monitor-exit(r15);	 Catch:{ all -> 0x02ca }
        throw r0;
    L_0x02ca:
        r0 = move-exception;
        goto L_0x02c8;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.UserManagerService.dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):void");
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("system");
        stringBuilder.append(File.separator);
        stringBuilder.append("users");
        USER_INFO_DIR = stringBuilder.toString();
    }

    public static UserManagerService getInstance() {
        UserManagerService userManagerService;
        synchronized (UserManagerService.class) {
            userManagerService = sInstance;
        }
        return userManagerService;
    }

    @VisibleForTesting
    UserManagerService(Context context) {
        this(context, null, null, new Object(), context.getCacheDir());
    }

    UserManagerService(Context context, PackageManagerService pm, UserDataPreparer userDataPreparer, Object packagesLock) {
        this(context, pm, userDataPreparer, packagesLock, Environment.getDataDirectory());
    }

    private UserManagerService(Context context, PackageManagerService pm, UserDataPreparer userDataPreparer, Object packagesLock, File dataDir) {
        this.mUsersLock = LockGuard.installNewLock(2);
        this.mRestrictionsLock = new Object();
        this.mAppRestrictionsLock = new Object();
        this.DEFAULT_VALUE = false;
        this.PROPERTIES_PRIVACY_SUPPORT_IUDF = "ro.config.support_iudf";
        this.isSupportISec = SystemProperties.getBoolean("ro.config.support_iudf", false);
        this.mHasClonedProfile = false;
        this.mUsers = new SparseArray();
        this.mBaseUserRestrictions = new SparseArray();
        this.mCachedEffectiveUserRestrictions = new SparseArray();
        this.mAppliedUserRestrictions = new SparseArray();
        this.mDevicePolicyGlobalUserRestrictions = new SparseArray();
        this.mDeviceOwnerUserId = -10000;
        this.mDevicePolicyLocalUserRestrictions = new SparseArray();
        this.mGuestRestrictions = new Bundle();
        this.mRemovingUserIds = new SparseBooleanArray();
        this.mRecentlyRemovedIds = new LinkedList();
        this.mClonedProfileRecentlyRemovedIds = new LinkedList();
        this.mUserVersion = 0;
        this.mIsUserManaged = new SparseBooleanArray();
        this.mUserRestrictionsListeners = new ArrayList();
        this.ACTION_DISABLE_QUIET_MODE_AFTER_UNLOCK = "com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK";
        this.mDisableQuietModeCallback = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK".equals(intent.getAction())) {
                    IntentSender target = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
                    BackgroundThread.getHandler().post(new -$$Lambda$UserManagerService$1$DQ_02g7kZ7QrJXO6aCATwE6DYCE(this, intent.getIntExtra("android.intent.extra.USER_ID", -10000), target));
                }
            }
        };
        this.mUserStates = new SparseIntArray();
        this.isOwnerNameChanged = false;
        this.mContext = context;
        this.mPm = pm;
        this.mPackagesLock = packagesLock;
        this.mHandler = new MainHandler();
        this.mUserDataPreparer = userDataPreparer;
        synchronized (this.mPackagesLock) {
            this.mUsersDir = new File(dataDir, USER_INFO_DIR);
            this.mUsersDir.mkdirs();
            new File(this.mUsersDir, String.valueOf(0)).mkdirs();
            FileUtils.setPermissions(this.mUsersDir.toString(), 509, -1, -1);
            this.mUserListFile = new File(this.mUsersDir, USER_LIST_FILENAME);
            initDefaultGuestRestrictions();
            readUserListLP();
            sInstance = this;
            UserInfo info = getUserInfoLU(0);
            if (!(info == null || info.name == null)) {
                this.isOwnerNameChanged = info.name.equals(this.mContext.getResources().getString(17040626)) ^ 1;
            }
        }
        this.mLocalService = new LocalService(this, null);
        LocalServices.addService(UserManagerInternal.class, this.mLocalService);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mUserStates.put(0, 0);
    }

    void systemReady() {
        this.mAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        synchronized (this.mRestrictionsLock) {
            applyUserRestrictionsLR(0);
        }
        UserInfo currentGuestUser = findCurrentGuestUser();
        if (!(currentGuestUser == null || hasUserRestriction("no_config_wifi", currentGuestUser.id))) {
            setUserRestriction("no_config_wifi", true, currentGuestUser.id);
        }
        this.mContext.registerReceiver(this.mDisableQuietModeCallback, new IntentFilter("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK"), null, this.mHandler);
    }

    void cleanupPartialUsers() {
        int userSize;
        int i;
        ArrayList<UserInfo> partials = new ArrayList();
        synchronized (this.mUsersLock) {
            userSize = this.mUsers.size();
            i = 0;
            int i2 = 0;
            while (i2 < userSize) {
                UserInfo ui = ((UserData) this.mUsers.valueAt(i2)).info;
                if ((ui.partial || ui.guestToRemove || ui.isEphemeral()) && i2 != 0) {
                    partials.add(ui);
                    addRemovingUserIdLocked(ui.id);
                    ui.partial = true;
                }
                i2++;
            }
        }
        int partialsSize = partials.size();
        while (true) {
            userSize = i;
            if (userSize < partialsSize) {
                UserInfo ui2 = (UserInfo) partials.get(userSize);
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Removing partially created user ");
                stringBuilder.append(ui2.id);
                stringBuilder.append(" (name=");
                stringBuilder.append(ui2.name);
                stringBuilder.append(")");
                Slog.w(str, stringBuilder.toString());
                removeUserState(ui2.id);
                i = userSize + 1;
            } else {
                return;
            }
        }
    }

    public String getUserAccount(int userId) {
        String str;
        checkManageUserAndAcrossUsersFullPermission("get user account");
        synchronized (this.mUsersLock) {
            str = ((UserData) this.mUsers.get(userId)).account;
        }
        return str;
    }

    /* JADX WARNING: Missing block: B:18:0x003c, code:
            if (r0 == null) goto L_0x0041;
     */
    /* JADX WARNING: Missing block: B:20:?, code:
            writeUserLP(r0);
     */
    /* JADX WARNING: Missing block: B:22:0x0042, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setUserAccount(int userId, String accountName) {
        checkManageUserAndAcrossUsersFullPermission("set user account");
        UserData userToUpdate = null;
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = (UserData) this.mUsers.get(userId);
                if (userData == null) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("User not found for setting user account: u");
                    stringBuilder.append(userId);
                    Slog.e(str, stringBuilder.toString());
                } else if (!Objects.equals(userData.account, accountName)) {
                    userData.account = accountName;
                    userToUpdate = userData;
                }
            }
        }
    }

    public UserInfo getPrimaryUser() {
        checkManageUsersPermission("query users");
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            int i = 0;
            while (i < userSize) {
                UserInfo ui = ((UserData) this.mUsers.valueAt(i)).info;
                if (!ui.isPrimary() || this.mRemovingUserIds.get(ui.id)) {
                    i++;
                } else {
                    return ui;
                }
            }
            return null;
        }
    }

    public List<UserInfo> getUsers(boolean excludeDying) {
        ArrayList<UserInfo> users;
        checkManageOrCreateUsersPermission("query users");
        synchronized (this.mUsersLock) {
            users = new ArrayList(this.mUsers.size());
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = ((UserData) this.mUsers.valueAt(i)).info;
                if (!ui.partial) {
                    if (!(excludeDying && this.mRemovingUserIds.get(ui.id))) {
                        users.add(userWithName(ui));
                    }
                    if (ui.id == 0 && !this.isOwnerNameChanged) {
                        boolean nameChanged = false;
                        String ownerName = this.mContext.getResources().getString(17040626);
                        if (!(TextUtils.isEmpty(ui.name) || ui.name.equals(ownerName))) {
                            nameChanged = true;
                        }
                        ui.name = ownerName;
                        if (nameChanged) {
                            UserData userData = getUserDataNoChecks(ui.id);
                            if (userData != null) {
                                userData.info = ui;
                                writeUserLP(userData);
                            }
                        }
                    }
                }
            }
        }
        return users;
    }

    public List<UserInfo> getProfiles(int userId, boolean enabledOnly) {
        boolean returnFullInfo = true;
        if (userId != UserHandle.getCallingUserId()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getting profiles related to user ");
            stringBuilder.append(userId);
            checkManageOrCreateUsersPermission(stringBuilder.toString());
        } else {
            returnFullInfo = hasManageUsersPermission();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            List<UserInfo> profilesLU;
            synchronized (this.mUsersLock) {
                profilesLU = getProfilesLU(userId, enabledOnly, returnFullInfo);
            }
            Binder.restoreCallingIdentity(ident);
            return profilesLU;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int[] getProfileIds(int userId, boolean enabledOnly) {
        if (userId != UserHandle.getCallingUserId()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getting profiles related to user ");
            stringBuilder.append(userId);
            checkManageOrCreateUsersPermission(stringBuilder.toString());
        }
        long ident = Binder.clearCallingIdentity();
        try {
            int[] toArray;
            synchronized (this.mUsersLock) {
                toArray = getProfileIdsLU(userId, enabledOnly).toArray();
            }
            Binder.restoreCallingIdentity(ident);
            return toArray;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private List<UserInfo> getProfilesLU(int userId, boolean enabledOnly, boolean fullInfo) {
        IntArray profileIds = getProfileIdsLU(userId, enabledOnly);
        ArrayList<UserInfo> users = new ArrayList(profileIds.size());
        for (int i = 0; i < profileIds.size(); i++) {
            UserInfo userInfo = ((UserData) this.mUsers.get(profileIds.get(i))).info;
            if (fullInfo) {
                userInfo = userWithName(userInfo);
            } else {
                userInfo = new UserInfo(userInfo);
                userInfo.name = null;
                userInfo.iconPath = null;
            }
            users.add(userInfo);
        }
        return users;
    }

    private IntArray getProfileIdsLU(int userId, boolean enabledOnly) {
        UserInfo user = getUserInfoLU(userId);
        IntArray result = new IntArray(this.mUsers.size());
        if (user == null) {
            return result;
        }
        int userSize = this.mUsers.size();
        for (int i = 0; i < userSize; i++) {
            UserInfo profile = ((UserData) this.mUsers.valueAt(i)).info;
            if (isProfileOf(user, profile) && !((enabledOnly && !profile.isEnabled()) || this.mRemovingUserIds.get(profile.id) || profile.partial)) {
                result.add(profile.id);
            }
        }
        return result;
    }

    public int getCredentialOwnerProfile(int userHandle) {
        checkManageUsersPermission("get the credential owner");
        if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userHandle)) {
            synchronized (this.mUsersLock) {
                UserInfo profileParent = getProfileParentLU(userHandle);
                if (profileParent != null) {
                    int i = profileParent.id;
                    return i;
                }
            }
        }
        return userHandle;
    }

    public boolean isSameProfileGroup(int userId, int otherUserId) {
        if (userId == otherUserId) {
            return true;
        }
        checkManageUsersPermission("check if in the same profile group");
        return isSameProfileGroupNoChecks(userId, otherUserId);
    }

    /* JADX WARNING: Missing block: B:15:0x0025, code:
            return r2;
     */
    /* JADX WARNING: Missing block: B:17:0x0027, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:19:0x0029, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isSameProfileGroupNoChecks(int userId, int otherUserId) {
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            boolean z = false;
            if (userInfo == null || userInfo.profileGroupId == -10000) {
            } else {
                UserInfo otherUserInfo = getUserInfoLU(otherUserId);
                if (otherUserInfo == null || otherUserInfo.profileGroupId == -10000) {
                } else if (userInfo.profileGroupId == otherUserInfo.profileGroupId) {
                    z = true;
                }
            }
        }
    }

    public UserInfo getProfileParent(int userHandle) {
        UserInfo profileParentLU;
        checkManageUsersPermission("get the profile parent");
        synchronized (this.mUsersLock) {
            profileParentLU = getProfileParentLU(userHandle);
        }
        return profileParentLU;
    }

    public int getProfileParentId(int userHandle) {
        checkManageUsersPermission("get the profile parent");
        return this.mLocalService.getProfileParentId(userHandle);
    }

    private UserInfo getProfileParentLU(int userHandle) {
        UserInfo profile = getUserInfoLU(userHandle);
        if (profile == null) {
            return null;
        }
        int parentUserId = profile.profileGroupId;
        if (parentUserId == userHandle || parentUserId == -10000) {
            return null;
        }
        return getUserInfoLU(parentUserId);
    }

    private static boolean isProfileOf(UserInfo user, UserInfo profile) {
        return user.id == profile.id || (user.profileGroupId != -10000 && user.profileGroupId == profile.profileGroupId);
    }

    private void broadcastProfileAvailabilityChanges(UserHandle profileHandle, UserHandle parentHandle, boolean inQuietMode) {
        Intent intent = new Intent();
        if (inQuietMode) {
            intent.setAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        } else {
            intent.setAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        }
        intent.putExtra("android.intent.extra.QUIET_MODE", inQuietMode);
        intent.putExtra("android.intent.extra.USER", profileHandle);
        intent.putExtra("android.intent.extra.user_handle", profileHandle.getIdentifier());
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, parentHandle);
    }

    public boolean requestQuietModeEnabled(String callingPackage, boolean enableQuietMode, int userHandle, IntentSender target) {
        Preconditions.checkNotNull(callingPackage);
        if (!enableQuietMode || target == null) {
            ensureCanModifyQuietMode(callingPackage, Binder.getCallingUid(), target != null);
            long identity = Binder.clearCallingIdentity();
            if (enableQuietMode) {
                try {
                    setQuietModeEnabled(userHandle, true, target);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                boolean needToShowConfirmCredential = this.mLockPatternUtils.isSecure(userHandle) && !StorageManager.isUserKeyUnlocked(userHandle);
                if (needToShowConfirmCredential) {
                    showConfirmCredentialToDisableQuietMode(userHandle, target);
                    Binder.restoreCallingIdentity(identity);
                    return false;
                }
                setQuietModeEnabled(userHandle, false, target);
                Binder.restoreCallingIdentity(identity);
                return true;
            }
            return true;
        }
        throw new IllegalArgumentException("target should only be specified when we are disabling quiet mode.");
    }

    private void ensureCanModifyQuietMode(String callingPackage, int callingUid, boolean startIntent) {
        if (!hasManageUsersPermission()) {
            if (startIntent) {
                throw new SecurityException("MANAGE_USERS permission is required to start intent after disabling quiet mode.");
            } else if (!hasPermissionGranted("android.permission.MODIFY_QUIET_MODE", callingUid)) {
                verifyCallingPackage(callingPackage, callingUid);
                ShortcutServiceInternal shortcutInternal = (ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class);
                if (shortcutInternal == null || !shortcutInternal.isForegroundDefaultLauncher(callingPackage, callingUid)) {
                    throw new SecurityException("Can't modify quiet mode, caller is neither foreground default launcher nor has MANAGE_USERS/MODIFY_QUIET_MODE permission");
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x003e, code:
            r4 = r6.mPackagesLock;
     */
    /* JADX WARNING: Missing block: B:15:0x0040, code:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:17:?, code:
            writeUserLP(r3);
     */
    /* JADX WARNING: Missing block: B:18:0x0044, code:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:19:0x0045, code:
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:20:0x0046, code:
            if (r8 == false) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:22:?, code:
            android.app.ActivityManager.getService().stopUser(r7, true, null);
            ((android.app.ActivityManagerInternal) com.android.server.LocalServices.getService(android.app.ActivityManagerInternal.class)).killForegroundAppsForUser(r7);
     */
    /* JADX WARNING: Missing block: B:23:0x005c, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:24:0x005e, code:
            if (r9 == null) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:25:0x0060, code:
            r0 = new com.android.server.pm.UserManagerService.DisableQuietModeUserUnlockedCallback(r6, r9);
     */
    /* JADX WARNING: Missing block: B:26:0x0067, code:
            android.app.ActivityManager.getService().startUserInBackgroundWithListener(r7, r0);
     */
    /* JADX WARNING: Missing block: B:27:0x006f, code:
            r0.rethrowAsRuntimeException();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setQuietModeEnabled(int userHandle, boolean enableQuietMode, IntentSender target) {
        UserInfo profile;
        UserInfo parent;
        synchronized (this.mUsersLock) {
            profile = getUserInfoLU(userHandle);
            parent = getProfileParentLU(userHandle);
            StringBuilder stringBuilder;
            if (profile == null || !profile.isManagedProfile()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("User ");
                stringBuilder.append(userHandle);
                stringBuilder.append(" is not a profile");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (profile.isQuietModeEnabled() == enableQuietMode) {
                String str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Quiet mode is already ");
                stringBuilder.append(enableQuietMode);
                Slog.i(str, stringBuilder.toString());
                return;
            } else {
                profile.flags ^= 128;
                UserData profileUserData = getUserDataLU(profile.id);
            }
        }
        broadcastProfileAvailabilityChanges(profile.getUserHandle(), parent.getUserHandle(), enableQuietMode);
    }

    public boolean isQuietModeEnabled(int userHandle) {
        synchronized (this.mPackagesLock) {
            UserInfo info;
            synchronized (this.mUsersLock) {
                info = getUserInfoLU(userHandle);
            }
            if (info != null) {
                if (info.isManagedProfile()) {
                    boolean isQuietModeEnabled = info.isQuietModeEnabled();
                    return isQuietModeEnabled;
                }
            }
            return false;
        }
    }

    private void showConfirmCredentialToDisableQuietMode(int userHandle, IntentSender target) {
        Intent unlockIntent = ((KeyguardManager) this.mContext.getSystemService("keyguard")).createConfirmDeviceCredentialIntent(null, null, userHandle);
        if (unlockIntent != null) {
            Intent callBackIntent = new Intent("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK");
            if (target != null) {
                callBackIntent.putExtra("android.intent.extra.INTENT", target);
            }
            callBackIntent.putExtra("android.intent.extra.USER_ID", userHandle);
            callBackIntent.setPackage(this.mContext.getPackageName());
            callBackIntent.addFlags(268435456);
            unlockIntent.putExtra("android.intent.extra.INTENT", PendingIntent.getBroadcast(this.mContext, 0, callBackIntent, 1409286144).getIntentSender());
            unlockIntent.setFlags(276824064);
            this.mContext.startActivity(unlockIntent);
        }
    }

    public void setUserEnabled(int userId) {
        checkManageUsersPermission("enable user");
        synchronized (this.mPackagesLock) {
            UserInfo info;
            synchronized (this.mUsersLock) {
                info = getUserInfoLU(userId);
            }
            if (info != null) {
                if (!info.isEnabled()) {
                    info.flags ^= 64;
                    writeUserLP(getUserDataLU(info.id));
                }
            }
        }
    }

    public void setUserAdmin(int userId) {
        checkManageUserAndAcrossUsersFullPermission("set user admin");
        synchronized (this.mPackagesLock) {
            UserInfo info;
            synchronized (this.mUsersLock) {
                info = getUserInfoLU(userId);
            }
            if (info != null) {
                if (!info.isAdmin()) {
                    info.flags ^= 2;
                    writeUserLP(getUserDataLU(info.id));
                    setUserRestriction("no_sms", false, userId);
                    setUserRestriction("no_outgoing_calls", false, userId);
                    return;
                }
            }
        }
    }

    public void evictCredentialEncryptionKey(int userId) {
        checkManageUsersPermission("evict CE key");
        IActivityManager am = ActivityManagerNative.getDefault();
        long identity = Binder.clearCallingIdentity();
        try {
            am.restartUserInBackground(userId);
            Binder.restoreCallingIdentity(identity);
        } catch (RemoteException re) {
            throw re.rethrowAsRuntimeException();
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public UserInfo getUserInfo(int userId) {
        UserInfo userWithName;
        checkManageOrCreateUsersPermission("query user");
        synchronized (this.mUsersLock) {
            userWithName = userWithName(getUserInfoLU(userId));
        }
        return userWithName;
    }

    private UserInfo userWithName(UserInfo orig) {
        if (orig == null || orig.name != null || orig.id != 0) {
            return orig;
        }
        UserInfo withName = new UserInfo(orig);
        withName.name = getOwnerName();
        return withName;
    }

    public int getManagedProfileBadge(int userId) {
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId == userId || hasManageUsersPermission() || isSameProfileGroupNoChecks(callingUserId, userId)) {
            int i;
            synchronized (this.mUsersLock) {
                UserInfo userInfo = getUserInfoLU(userId);
                i = userInfo != null ? userInfo.profileBadge : 0;
            }
            return i;
        }
        throw new SecurityException("You need MANAGE_USERS permission to: check if specified user a managed profile outside your profile group");
    }

    public boolean isManagedProfile(int userId) {
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId == userId || hasManageUsersPermission() || isSameProfileGroupNoChecks(callingUserId, userId)) {
            boolean z;
            synchronized (this.mUsersLock) {
                UserInfo userInfo = getUserInfoLU(userId);
                z = userInfo != null && userInfo.isManagedProfile();
            }
            return z;
        }
        throw new SecurityException("You need MANAGE_USERS permission to: check if specified user a managed profile outside your profile group");
    }

    public boolean isUserUnlockingOrUnlocked(int userId) {
        checkManageOrInteractPermIfCallerInOtherProfileGroup(userId, "isUserUnlockingOrUnlocked");
        return this.mLocalService.isUserUnlockingOrUnlocked(userId);
    }

    public boolean isUserUnlocked(int userId) {
        checkManageOrInteractPermIfCallerInOtherProfileGroup(userId, "isUserUnlocked");
        return this.mLocalService.isUserUnlocked(userId);
    }

    public boolean isUserRunning(int userId) {
        checkManageOrInteractPermIfCallerInOtherProfileGroup(userId, "isUserRunning");
        return this.mLocalService.isUserRunning(userId);
    }

    public long getUserStartRealtime() {
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mUsersLock) {
            UserData user = getUserDataLU(userId);
            if (user != null) {
                long j = user.startRealtime;
                return j;
            }
            return 0;
        }
    }

    public long getUserUnlockRealtime() {
        synchronized (this.mUsersLock) {
            UserData user = getUserDataLU(UserHandle.getUserId(Binder.getCallingUid()));
            if (user != null) {
                long j = user.unlockRealtime;
                return j;
            }
            return 0;
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0037, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void checkManageOrInteractPermIfCallerInOtherProfileGroup(int userId, String name) {
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId != userId && !isSameProfileGroupNoChecks(callingUserId, userId) && !hasManageUsersPermission() && !hasPermissionGranted("android.permission.INTERACT_ACROSS_USERS", Binder.getCallingUid())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("You need INTERACT_ACROSS_USERS or MANAGE_USERS permission to: check ");
            stringBuilder.append(name);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public boolean isDemoUser(int userId) {
        if (UserHandle.getCallingUserId() == userId || hasManageUsersPermission()) {
            boolean z;
            synchronized (this.mUsersLock) {
                UserInfo userInfo = getUserInfoLU(userId);
                z = userInfo != null && userInfo.isDemo();
            }
            return z;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("You need MANAGE_USERS permission to query if u=");
        stringBuilder.append(userId);
        stringBuilder.append(" is a demo user");
        throw new SecurityException(stringBuilder.toString());
    }

    public boolean isRestricted() {
        boolean isRestricted;
        synchronized (this.mUsersLock) {
            isRestricted = getUserInfoLU(UserHandle.getCallingUserId()).isRestricted();
        }
        return isRestricted;
    }

    /* JADX WARNING: Missing block: B:17:0x002d, code:
            return r2;
     */
    /* JADX WARNING: Missing block: B:19:0x002f, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean canHaveRestrictedProfile(int userId) {
        checkManageUsersPermission("canHaveRestrictedProfile");
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            boolean z = false;
            if (userInfo == null || !userInfo.canHaveProfile()) {
            } else if (!userInfo.isAdmin()) {
                return false;
            } else if (!(this.mIsDeviceManaged || this.mIsUserManaged.get(userId))) {
                z = true;
            }
        }
    }

    public boolean hasRestrictedProfiles() {
        checkManageUsersPermission("hasRestrictedProfiles");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            int i = 0;
            while (i < userSize) {
                UserInfo profile = ((UserData) this.mUsers.valueAt(i)).info;
                if (callingUserId == profile.id || profile.restrictedProfileParentId != callingUserId) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    private UserInfo getUserInfoLU(int userId) {
        UserData userData = (UserData) this.mUsers.get(userId);
        UserInfo userInfo = null;
        if (userData == null || !userData.info.partial || this.mRemovingUserIds.get(userId)) {
            if (userData != null) {
                userInfo = userData.info;
            }
            return userInfo;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getUserInfo: unknown user #");
        stringBuilder.append(userId);
        Slog.w(str, stringBuilder.toString());
        return null;
    }

    private UserData getUserDataLU(int userId) {
        UserData userData = (UserData) this.mUsers.get(userId);
        if (userData == null || !userData.info.partial || this.mRemovingUserIds.get(userId)) {
            return userData;
        }
        return null;
    }

    private UserInfo getUserInfoNoChecks(int userId) {
        UserInfo userInfo;
        synchronized (this.mUsersLock) {
            UserData userData = (UserData) this.mUsers.get(userId);
            userInfo = userData != null ? userData.info : null;
        }
        return userInfo;
    }

    private UserData getUserDataNoChecks(int userId) {
        UserData userData;
        synchronized (this.mUsersLock) {
            userData = (UserData) this.mUsers.get(userId);
        }
        return userData;
    }

    public boolean exists(int userId) {
        return this.mLocalService.exists(userId);
    }

    /* JADX WARNING: Missing block: B:20:0x0052, code:
            if (r0 == false) goto L_0x0057;
     */
    /* JADX WARNING: Missing block: B:21:0x0054, code:
            sendUserInfoChangedBroadcast(r7);
     */
    /* JADX WARNING: Missing block: B:22:0x0057, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setUserName(int userId, String name) {
        checkManageUsersPermission("rename users");
        boolean changed = false;
        synchronized (this.mPackagesLock) {
            UserData userData = getUserDataNoChecks(userId);
            if (userData == null || userData.info.partial) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setUserName: unknown user #");
                stringBuilder.append(userId);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            if (!(name == null || name.equals(userData.info.name))) {
                userData.info.name = name;
                writeUserLP(userData);
                changed = true;
            }
            if (name != null && userId == 0) {
                if (this.mContext.getResources() == null || !name.equals(this.mContext.getResources().getString(17040626))) {
                    this.isOwnerNameChanged = true;
                } else {
                    this.isOwnerNameChanged = false;
                }
            }
        }
    }

    public void setUserIcon(int userId, Bitmap bitmap) {
        checkManageUsersPermission("update users");
        if (hasUserRestriction("no_set_user_icon", userId)) {
            Log.w(LOG_TAG, "Cannot set user icon. DISALLOW_SET_USER_ICON is enabled.");
        } else {
            this.mLocalService.setUserIcon(userId, bitmap);
        }
    }

    private void sendUserInfoChangedBroadcast(int userId) {
        Intent changedIntent = new Intent("android.intent.action.USER_INFO_CHANGED");
        changedIntent.putExtra("android.intent.extra.user_handle", userId);
        changedIntent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(changedIntent, UserHandle.ALL);
    }

    public ParcelFileDescriptor getUserIcon(int targetUserId) {
        synchronized (this.mPackagesLock) {
            UserInfo targetUserInfo = getUserInfoNoChecks(targetUserId);
            if (targetUserInfo == null || targetUserInfo.partial) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getUserIcon: unknown user #");
                stringBuilder.append(targetUserId);
                Slog.w(str, stringBuilder.toString());
                return null;
            }
            int callingUserId = UserHandle.getCallingUserId();
            int callingGroupId = getUserInfoNoChecks(callingUserId).profileGroupId;
            boolean sameGroup = callingGroupId != -10000 && callingGroupId == targetUserInfo.profileGroupId;
            if (!(callingUserId == targetUserId || sameGroup)) {
                checkManageUsersPermission("get the icon of a user who is not related");
            }
            if (targetUserInfo.iconPath == null) {
                return null;
            }
            String targetUserInfo2 = targetUserInfo.iconPath;
            try {
                return ParcelFileDescriptor.open(new File(targetUserInfo2), 268435456);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "Couldn't find icon file", e);
                return null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x002d, code:
            r1 = r2;
     */
    /* JADX WARNING: Missing block: B:12:0x002e, code:
            if (r0 == false) goto L_0x0033;
     */
    /* JADX WARNING: Missing block: B:13:0x0030, code:
            scheduleWriteUser(r1);
     */
    /* JADX WARNING: Missing block: B:14:0x0033, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void makeInitialized(int userId) {
        checkManageUsersPermission("makeInitialized");
        boolean scheduleWriteUser = false;
        synchronized (this.mUsersLock) {
            UserData userData = (UserData) this.mUsers.get(userId);
            if (userData == null || userData.info.partial) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("makeInitialized: unknown user #");
                stringBuilder.append(userId);
                Slog.w(str, stringBuilder.toString());
            } else if ((userData.info.flags & 16) == 0) {
                UserInfo userInfo = userData.info;
                userInfo.flags |= 16;
                scheduleWriteUser = true;
            }
        }
    }

    private void initDefaultGuestRestrictions() {
        synchronized (this.mGuestRestrictions) {
            if (this.mGuestRestrictions.isEmpty()) {
                this.mGuestRestrictions.putBoolean("no_config_wifi", true);
                this.mGuestRestrictions.putBoolean("no_install_unknown_sources", true);
                this.mGuestRestrictions.putBoolean("no_outgoing_calls", true);
                this.mGuestRestrictions.putBoolean("no_sms", true);
            }
        }
    }

    public Bundle getDefaultGuestRestrictions() {
        Bundle bundle;
        checkManageUsersPermission("getDefaultGuestRestrictions");
        synchronized (this.mGuestRestrictions) {
            bundle = new Bundle(this.mGuestRestrictions);
        }
        return bundle;
    }

    public void setDefaultGuestRestrictions(Bundle restrictions) {
        checkManageUsersPermission("setDefaultGuestRestrictions");
        synchronized (this.mGuestRestrictions) {
            this.mGuestRestrictions.clear();
            this.mGuestRestrictions.putAll(restrictions);
        }
        synchronized (this.mPackagesLock) {
            writeUserListLP();
        }
    }

    private void setDevicePolicyUserRestrictionsInner(int userId, Bundle restrictions, boolean isDeviceOwner, int cameraRestrictionScope) {
        boolean globalChanged;
        boolean localChanged;
        Bundle global = new Bundle();
        Bundle local = new Bundle();
        UserRestrictionsUtils.sortToGlobalAndLocal(restrictions, isDeviceOwner, cameraRestrictionScope, global, local);
        synchronized (this.mRestrictionsLock) {
            globalChanged = updateRestrictionsIfNeededLR(userId, global, this.mDevicePolicyGlobalUserRestrictions);
            localChanged = updateRestrictionsIfNeededLR(userId, local, this.mDevicePolicyLocalUserRestrictions);
            if (isDeviceOwner) {
                this.mDeviceOwnerUserId = userId;
            } else if (this.mDeviceOwnerUserId == userId) {
                this.mDeviceOwnerUserId = -10000;
            }
        }
        synchronized (this.mPackagesLock) {
            if (localChanged || globalChanged) {
                writeUserLP(getUserDataNoChecks(userId));
            }
        }
        synchronized (this.mRestrictionsLock) {
            if (globalChanged) {
                applyUserRestrictionsForAllUsersLR();
            } else if (localChanged) {
                applyUserRestrictionsLR(userId);
            }
        }
    }

    private boolean updateRestrictionsIfNeededLR(int userId, Bundle restrictions, SparseArray<Bundle> restrictionsArray) {
        boolean changed = UserRestrictionsUtils.areEqual((Bundle) restrictionsArray.get(userId), restrictions) ^ 1;
        if (changed) {
            if (UserRestrictionsUtils.isEmpty(restrictions)) {
                restrictionsArray.delete(userId);
            } else {
                restrictionsArray.put(userId, restrictions);
            }
        }
        return changed;
    }

    @GuardedBy("mRestrictionsLock")
    private Bundle computeEffectiveUserRestrictionsLR(int userId) {
        Bundle baseRestrictions = UserRestrictionsUtils.nonNull((Bundle) this.mBaseUserRestrictions.get(userId));
        Bundle global = UserRestrictionsUtils.mergeAll(this.mDevicePolicyGlobalUserRestrictions);
        Bundle local = (Bundle) this.mDevicePolicyLocalUserRestrictions.get(userId);
        if (UserRestrictionsUtils.isEmpty(global) && UserRestrictionsUtils.isEmpty(local)) {
            return baseRestrictions;
        }
        Bundle effective = UserRestrictionsUtils.clone(baseRestrictions);
        UserRestrictionsUtils.merge(effective, global);
        UserRestrictionsUtils.merge(effective, local);
        return effective;
    }

    @GuardedBy("mRestrictionsLock")
    private void invalidateEffectiveUserRestrictionsLR(int userId) {
        this.mCachedEffectiveUserRestrictions.remove(userId);
    }

    private Bundle getEffectiveUserRestrictions(int userId) {
        Bundle restrictions;
        synchronized (this.mRestrictionsLock) {
            restrictions = (Bundle) this.mCachedEffectiveUserRestrictions.get(userId);
            if (restrictions == null) {
                restrictions = computeEffectiveUserRestrictionsLR(userId);
                this.mCachedEffectiveUserRestrictions.put(userId, restrictions);
            }
        }
        return restrictions;
    }

    public boolean hasUserRestriction(String restrictionKey, int userId) {
        boolean z = false;
        if (!UserRestrictionsUtils.isValidRestriction(restrictionKey)) {
            return false;
        }
        Bundle restrictions = getEffectiveUserRestrictions(userId);
        if (restrictions != null && restrictions.getBoolean(restrictionKey)) {
            z = true;
        }
        return z;
    }

    public boolean hasUserRestrictionOnAnyUser(String restrictionKey) {
        if (!UserRestrictionsUtils.isValidRestriction(restrictionKey)) {
            return false;
        }
        List<UserInfo> users = getUsers(true);
        for (int i = 0; i < users.size(); i++) {
            Bundle restrictions = getEffectiveUserRestrictions(((UserInfo) users.get(i)).id);
            if (restrictions != null && restrictions.getBoolean(restrictionKey)) {
                return true;
            }
        }
        return false;
    }

    public int getUserRestrictionSource(String restrictionKey, int userId) {
        List<EnforcingUser> enforcingUsers = getUserRestrictionSources(restrictionKey, userId);
        int result = 0;
        for (int i = enforcingUsers.size() - 1; i >= 0; i--) {
            result |= ((EnforcingUser) enforcingUsers.get(i)).getUserRestrictionSource();
        }
        return result;
    }

    public List<EnforcingUser> getUserRestrictionSources(String restrictionKey, int userId) {
        checkManageUsersPermission("getUserRestrictionSource");
        if (!hasUserRestriction(restrictionKey, userId)) {
            return Collections.emptyList();
        }
        List<EnforcingUser> result = new ArrayList();
        if (hasBaseUserRestriction(restrictionKey, userId)) {
            result.add(new EnforcingUser(-10000, 1));
        }
        synchronized (this.mRestrictionsLock) {
            if (UserRestrictionsUtils.contains((Bundle) this.mDevicePolicyLocalUserRestrictions.get(userId), restrictionKey)) {
                result.add(getEnforcingUserLocked(userId));
            }
            int i = this.mDevicePolicyGlobalUserRestrictions.size() - 1;
            while (true) {
                int i2 = i;
                if (i2 >= 0) {
                    Bundle globalRestrictions = (Bundle) this.mDevicePolicyGlobalUserRestrictions.valueAt(i2);
                    int profileUserId = this.mDevicePolicyGlobalUserRestrictions.keyAt(i2);
                    if (UserRestrictionsUtils.contains(globalRestrictions, restrictionKey)) {
                        result.add(getEnforcingUserLocked(profileUserId));
                    }
                    i = i2 - 1;
                }
            }
        }
        return result;
    }

    @GuardedBy("mRestrictionsLock")
    private EnforcingUser getEnforcingUserLocked(int userId) {
        int source;
        if (this.mDeviceOwnerUserId == userId) {
            source = 2;
        } else {
            source = 4;
        }
        return new EnforcingUser(userId, source);
    }

    public Bundle getUserRestrictions(int userId) {
        return UserRestrictionsUtils.clone(getEffectiveUserRestrictions(userId));
    }

    public boolean hasBaseUserRestriction(String restrictionKey, int userId) {
        checkManageUsersPermission("hasBaseUserRestriction");
        boolean z = false;
        if (!UserRestrictionsUtils.isValidRestriction(restrictionKey)) {
            return false;
        }
        synchronized (this.mRestrictionsLock) {
            Bundle bundle = (Bundle) this.mBaseUserRestrictions.get(userId);
            if (bundle != null && bundle.getBoolean(restrictionKey, false)) {
                z = true;
            }
        }
        return z;
    }

    public void setUserRestriction(String key, boolean value, int userId) {
        checkManageUsersPermission("setUserRestriction");
        if (UserRestrictionsUtils.isValidRestriction(key)) {
            synchronized (this.mRestrictionsLock) {
                Bundle newRestrictions = UserRestrictionsUtils.clone((Bundle) this.mBaseUserRestrictions.get(userId));
                newRestrictions.putBoolean(key, value);
                updateUserRestrictionsInternalLR(newRestrictions, userId);
            }
        }
    }

    @GuardedBy("mRestrictionsLock")
    private void updateUserRestrictionsInternalLR(Bundle newBaseRestrictions, int userId) {
        Bundle prevAppliedRestrictions = UserRestrictionsUtils.nonNull((Bundle) this.mAppliedUserRestrictions.get(userId));
        if (newBaseRestrictions != null) {
            boolean z = false;
            Preconditions.checkState(((Bundle) this.mBaseUserRestrictions.get(userId)) != newBaseRestrictions);
            if (this.mCachedEffectiveUserRestrictions.get(userId) != newBaseRestrictions) {
                z = true;
            }
            Preconditions.checkState(z);
            if (updateRestrictionsIfNeededLR(userId, newBaseRestrictions, this.mBaseUserRestrictions)) {
                scheduleWriteUser(getUserDataNoChecks(userId));
            }
        }
        Bundle effective = computeEffectiveUserRestrictionsLR(userId);
        this.mCachedEffectiveUserRestrictions.put(userId, effective);
        if (this.mAppOpsService != null) {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mAppOpsService.setUserRestrictions(effective, mUserRestriconToken, userId);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "Unable to notify AppOpsService of UserRestrictions");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
            Binder.restoreCallingIdentity(identity);
        }
        propagateUserRestrictionsLR(userId, effective, prevAppliedRestrictions);
        this.mAppliedUserRestrictions.put(userId, new Bundle(effective));
    }

    private void propagateUserRestrictionsLR(final int userId, Bundle newRestrictions, Bundle prevRestrictions) {
        if (!UserRestrictionsUtils.areEqual(newRestrictions, prevRestrictions)) {
            final Bundle newRestrictionsFinal = new Bundle(newRestrictions);
            final Bundle prevRestrictionsFinal = new Bundle(prevRestrictions);
            this.mHandler.post(new Runnable() {
                public void run() {
                    UserRestrictionsListener[] listeners;
                    UserRestrictionsUtils.applyUserRestrictions(UserManagerService.this.mContext, userId, newRestrictionsFinal, prevRestrictionsFinal);
                    synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                        listeners = new UserRestrictionsListener[UserManagerService.this.mUserRestrictionsListeners.size()];
                        UserManagerService.this.mUserRestrictionsListeners.toArray(listeners);
                    }
                    for (UserRestrictionsListener onUserRestrictionsChanged : listeners) {
                        onUserRestrictionsChanged.onUserRestrictionsChanged(userId, newRestrictionsFinal, prevRestrictionsFinal);
                    }
                    UserManagerService.this.mContext.sendBroadcastAsUser(new Intent("android.os.action.USER_RESTRICTIONS_CHANGED").setFlags(1073741824), UserHandle.of(userId));
                }
            });
        }
    }

    void applyUserRestrictionsLR(int userId) {
        updateUserRestrictionsInternalLR(null, userId);
    }

    @GuardedBy("mRestrictionsLock")
    void applyUserRestrictionsForAllUsersLR() {
        this.mCachedEffectiveUserRestrictions.clear();
        this.mHandler.post(new Runnable() {
            public void run() {
                try {
                    int[] runningUsers = ActivityManager.getService().getRunningUserIds();
                    synchronized (UserManagerService.this.mRestrictionsLock) {
                        for (int applyUserRestrictionsLR : runningUsers) {
                            UserManagerService.this.applyUserRestrictionsLR(applyUserRestrictionsLR);
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(UserManagerService.LOG_TAG, "Unable to access ActivityManagerService");
                }
            }
        });
    }

    private boolean isUserLimitReached() {
        int count;
        synchronized (this.mUsersLock) {
            count = getAliveUsersExcludingGuestsCountLU();
        }
        return count >= UserManager.getMaxSupportedUsers();
    }

    /* JADX WARNING: Missing block: B:33:0x0071, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:35:0x0073, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean canAddMoreManagedProfiles(int userId, boolean allowedToRemoveOne) {
        checkManageUsersPermission("check if more managed profiles can be added.");
        boolean z = false;
        if (ActivityManager.isLowRamDeviceStatic() || !this.mContext.getPackageManager().hasSystemFeature("android.software.managed_users")) {
            return false;
        }
        List<UserInfo> profiles = getProfiles(userId, false);
        Iterator<UserInfo> iterator = profiles.iterator();
        while (iterator.hasNext()) {
            if (((UserInfo) iterator.next()).isClonedProfile()) {
                iterator.remove();
            }
        }
        int managedProfilesCount = profiles.size() - 1;
        int profilesRemovedCount = (managedProfilesCount <= 0 || !allowedToRemoveOne) ? 0 : 1;
        if (managedProfilesCount - profilesRemovedCount >= getMaxManagedProfiles()) {
            return false;
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            if (userInfo == null || !userInfo.canHaveProfile()) {
            } else {
                int usersCountAfterRemoving = getAliveUsersExcludingGuestsCountLU() - profilesRemovedCount;
                if (usersCountAfterRemoving == 1 || usersCountAfterRemoving < UserManager.getMaxSupportedUsers()) {
                    z = true;
                }
            }
        }
    }

    private int getAliveUsersExcludingGuestsCountLU() {
        int aliveUserCount = 0;
        int totalUserCount = this.mUsers.size();
        for (int i = 0; i < totalUserCount; i++) {
            UserInfo user = ((UserData) this.mUsers.valueAt(i)).info;
            if (!(this.mRemovingUserIds.get(user.id) || user.isGuest() || user.isClonedProfile())) {
                aliveUserCount++;
            }
        }
        return aliveUserCount;
    }

    private static final void checkManageUserAndAcrossUsersFullPermission(String message) {
        int uid = Binder.getCallingUid();
        if (uid != 1000 && uid != 0) {
            if (!hasPermissionGranted("android.permission.MANAGE_USERS", uid) || !hasPermissionGranted("android.permission.INTERACT_ACROSS_USERS_FULL", uid)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("You need MANAGE_USERS and INTERACT_ACROSS_USERS_FULL permission to: ");
                stringBuilder.append(message);
                throw new SecurityException(stringBuilder.toString());
            }
        }
    }

    private static boolean hasPermissionGranted(String permission, int uid) {
        return ActivityManager.checkComponentPermission(permission, uid, -1, true) == 0;
    }

    private static final void checkManageUsersPermission(String message) {
        if (!hasManageUsersPermission()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("You need MANAGE_USERS permission to: ");
            stringBuilder.append(message);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private static final void checkManageOrCreateUsersPermission(String message) {
        if (!hasManageOrCreateUsersPermission()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("You either need MANAGE_USERS or CREATE_USERS permission to: ");
            stringBuilder.append(message);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private static final void checkManageOrCreateUsersPermission(int creationFlags) {
        StringBuilder stringBuilder;
        if ((creationFlags & -813) == 0) {
            if (!hasManageOrCreateUsersPermission()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("You either need MANAGE_USERS or CREATE_USERS permission to create an user with flags: ");
                stringBuilder.append(creationFlags);
                throw new SecurityException(stringBuilder.toString());
            }
        } else if (!hasManageUsersPermission()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("You need MANAGE_USERS permission to create an user  with flags: ");
            stringBuilder.append(creationFlags);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private static final boolean hasManageUsersPermission() {
        int callingUid = Binder.getCallingUid();
        return UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || hasPermissionGranted("android.permission.MANAGE_USERS", callingUid);
    }

    private static final boolean hasManageOrCreateUsersPermission() {
        int callingUid = Binder.getCallingUid();
        return UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || hasPermissionGranted("android.permission.MANAGE_USERS", callingUid) || hasPermissionGranted("android.permission.CREATE_USERS", callingUid);
    }

    private static void checkSystemOrRoot(String message) {
        int uid = Binder.getCallingUid();
        if (!UserHandle.isSameApp(uid, 1000) && uid != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Only system may: ");
            stringBuilder.append(message);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void writeBitmapLP(UserInfo info, Bitmap bitmap) {
        try {
            File dir = new File(this.mUsersDir, Integer.toString(info.id));
            File file = new File(dir, USER_PHOTO_FILENAME);
            File tmp = new File(dir, USER_PHOTO_FILENAME_TMP);
            if (!dir.exists()) {
                dir.mkdir();
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            CompressFormat compressFormat = CompressFormat.PNG;
            OutputStream fileOutputStream = new FileOutputStream(tmp);
            OutputStream os = fileOutputStream;
            if (bitmap.compress(compressFormat, 100, fileOutputStream) && tmp.renameTo(file) && SELinux.restorecon(file)) {
                info.iconPath = file.getAbsolutePath();
            }
            try {
                os.close();
            } catch (IOException e) {
            }
            tmp.delete();
        } catch (FileNotFoundException e2) {
            Slog.w(LOG_TAG, "Error setting photo for user ", e2);
        }
    }

    public int[] getUserIds() {
        int[] iArr;
        synchronized (this.mUsersLock) {
            iArr = this.mUserIds;
        }
        return iArr;
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0042  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0034 A:{Catch:{ IOException -> 0x0168, IOException -> 0x0168, Exception -> 0x014d }} */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0168 A:{Catch:{ all -> 0x014b }, PHI: r0 , Splitter: B:5:0x0014, ExcHandler: java.io.IOException (e java.io.IOException)} */
    /* JADX WARNING: Missing block: B:90:0x0169, code:
            fallbackToSingleUserLP();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readUserListLP() {
        if (this.mUserListFile.exists()) {
            FileInputStream fis = null;
            String lastSerialNumber;
            try {
                int type;
                fis = new AtomicFile(this.mUserListFile).openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 2 || type == 1) {
                        if (type == 2) {
                            Slog.e(LOG_TAG, "Unable to read user list");
                            fallbackToSingleUserLP();
                            IoUtils.closeQuietly(fis);
                            return;
                        }
                        String versionNumber;
                        this.mNextSerialNumber = -1;
                        if (parser.getName().equals("users")) {
                            lastSerialNumber = parser.getAttributeValue(null, ATTR_NEXT_SERIAL_NO);
                            if (lastSerialNumber != null) {
                                this.mNextSerialNumber = Integer.parseInt(lastSerialNumber);
                            }
                            versionNumber = parser.getAttributeValue(null, ATTR_USER_VERSION);
                            if (versionNumber != null) {
                                this.mUserVersion = Integer.parseInt(versionNumber);
                            }
                        }
                        Bundle oldDevicePolicyGlobalUserRestrictions = null;
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == 1) {
                                updateUserIds();
                                upgradeIfNecessaryLP(oldDevicePolicyGlobalUserRestrictions);
                                break;
                            } else if (type == 2) {
                                versionNumber = parser.getName();
                                if (versionNumber.equals(TAG_USER)) {
                                    UserData userData = readUserLP(Integer.parseInt(parser.getAttributeValue(null, ATTR_ID)));
                                    if (userData != null) {
                                        synchronized (this.mUsersLock) {
                                            this.mUsers.put(userData.info.id, userData);
                                            if (userData.info.isClonedProfile()) {
                                                Slog.i(LOG_TAG, "read user list, set mHasClonedProfile true.");
                                                this.mHasClonedProfile = true;
                                            }
                                            if (this.mNextSerialNumber < 0 || this.mNextSerialNumber <= userData.info.id) {
                                                this.mNextSerialNumber = userData.info.id + 1;
                                            }
                                        }
                                    } else if (this.mUsers.size() == 0) {
                                        Slog.e(LOG_TAG, "Unable to read user 0 --try fallback");
                                        fallbackToSingleUserLP();
                                        IoUtils.closeQuietly(fis);
                                        return;
                                    }
                                } else if (versionNumber.equals(TAG_GUEST_RESTRICTIONS)) {
                                    do {
                                        int next3 = parser.next();
                                        type = next3;
                                        if (next3 == 1 || type == 3) {
                                            break;
                                        }
                                    } while (type != 2);
                                    if (parser.getName().equals(TAG_RESTRICTIONS)) {
                                        synchronized (this.mGuestRestrictions) {
                                            UserRestrictionsUtils.readRestrictions(parser, this.mGuestRestrictions);
                                        }
                                    }
                                } else if (versionNumber.equals(TAG_DEVICE_OWNER_USER_ID) || versionNumber.equals(TAG_GLOBAL_RESTRICTION_OWNER_ID)) {
                                    String ownerUserId = parser.getAttributeValue(null, ATTR_ID);
                                    if (ownerUserId != null) {
                                        this.mDeviceOwnerUserId = Integer.parseInt(ownerUserId);
                                    }
                                } else if (versionNumber.equals(TAG_DEVICE_POLICY_RESTRICTIONS)) {
                                    oldDevicePolicyGlobalUserRestrictions = UserRestrictionsUtils.readRestrictions(parser);
                                }
                            }
                        }
                        IoUtils.closeQuietly(fis);
                        return;
                    }
                }
                if (type == 2) {
                }
            } catch (IOException e) {
            } catch (Exception e2) {
                try {
                    lastSerialNumber = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to read user list, error: ");
                    stringBuilder.append(e2);
                    Slog.e(lastSerialNumber, stringBuilder.toString());
                    fallbackToSingleUserLP();
                } catch (Throwable th) {
                    IoUtils.closeQuietly(fis);
                }
            }
        } else {
            fallbackToSingleUserLP();
        }
    }

    private void upgradeIfNecessaryLP(Bundle oldGlobalUserRestrictions) {
        UserData userData;
        int originalVersion = this.mUserVersion;
        int userVersion = this.mUserVersion;
        if (userVersion < 1) {
            userData = getUserDataNoChecks(0);
            if ("Primary".equals(userData.info.name)) {
                userData.info.name = this.mContext.getResources().getString(17040626);
                scheduleWriteUser(userData);
            }
            userVersion = 1;
        }
        if (userVersion < 2) {
            userData = getUserDataNoChecks(0);
            if ((userData.info.flags & 16) == 0) {
                UserInfo userInfo = userData.info;
                userInfo.flags |= 16;
                scheduleWriteUser(userData);
            }
            userVersion = 2;
        }
        if (userVersion < 4) {
            userVersion = 4;
        }
        if (userVersion < 5) {
            initDefaultGuestRestrictions();
            userVersion = 5;
        }
        if (userVersion < 6) {
            boolean splitSystemUser = UserManager.isSplitSystemUser();
            synchronized (this.mUsersLock) {
                for (int i = 0; i < this.mUsers.size(); i++) {
                    UserData userData2 = (UserData) this.mUsers.valueAt(i);
                    if (!splitSystemUser && userData2.info.isRestricted() && userData2.info.restrictedProfileParentId == -10000) {
                        userData2.info.restrictedProfileParentId = 0;
                        scheduleWriteUser(userData2);
                    }
                }
            }
            userVersion = 6;
        }
        if (userVersion < 7) {
            synchronized (this.mRestrictionsLock) {
                if (!(UserRestrictionsUtils.isEmpty(oldGlobalUserRestrictions) || this.mDeviceOwnerUserId == -10000)) {
                    this.mDevicePolicyGlobalUserRestrictions.put(this.mDeviceOwnerUserId, oldGlobalUserRestrictions);
                }
                UserRestrictionsUtils.moveRestriction("ensure_verify_apps", this.mDevicePolicyLocalUserRestrictions, this.mDevicePolicyGlobalUserRestrictions);
            }
            userVersion = 7;
        }
        if (userVersion < 7) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User version ");
            stringBuilder.append(this.mUserVersion);
            stringBuilder.append(" didn't upgrade as expected to ");
            stringBuilder.append(7);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        this.mUserVersion = userVersion;
        if (originalVersion < this.mUserVersion) {
            writeUserListLP();
        }
    }

    private void fallbackToSingleUserLP() {
        int flags = 16;
        if (!UserManager.isSplitSystemUser()) {
            flags = 16 | 3;
        }
        UserData userData = putUserInfo(new UserInfo(0, null, null, flags));
        this.mNextSerialNumber = 10;
        this.mUserVersion = 7;
        Bundle restrictions = new Bundle();
        try {
            for (String userRestriction : this.mContext.getResources().getStringArray(17235998)) {
                if (UserRestrictionsUtils.isValidRestriction(userRestriction)) {
                    restrictions.putBoolean(userRestriction, true);
                }
            }
        } catch (NotFoundException e) {
            Log.e(LOG_TAG, "Couldn't find resource: config_defaultFirstUserRestrictions", e);
        }
        if (!restrictions.isEmpty()) {
            synchronized (this.mRestrictionsLock) {
                this.mBaseUserRestrictions.append(0, restrictions);
            }
        }
        updateUserIds();
        initDefaultGuestRestrictions();
        writeUserLP(userData);
        writeUserListLP();
    }

    private String getOwnerName() {
        return this.mContext.getResources().getString(17040626);
    }

    private void scheduleWriteUser(UserData UserData) {
        if (!this.mHandler.hasMessages(1, UserData)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, UserData), 2000);
        }
    }

    private void writeUserLP(UserData userData) {
        FileOutputStream fos = null;
        File file = this.mUsersDir;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(userData.info.id);
        stringBuilder.append(XML_SUFFIX);
        AtomicFile userFile = new AtomicFile(new File(file, stringBuilder.toString()));
        try {
            fos = userFile.startWrite();
            writeUserLP(userData, new BufferedOutputStream(fos));
            userFile.finishWrite(fos);
        } catch (Exception ioe) {
            String str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error writing user info ");
            stringBuilder.append(userData.info.id);
            Slog.e(str, stringBuilder.toString(), ioe);
            userFile.failWrite(fos);
        }
    }

    @VisibleForTesting
    void writeUserLP(UserData userData, OutputStream os) throws IOException, XmlPullParserException {
        XmlSerializer serializer = new FastXmlSerializer();
        serializer.setOutput(os, StandardCharsets.UTF_8.name());
        serializer.startDocument(null, Boolean.valueOf(true));
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        UserInfo userInfo = userData.info;
        serializer.startTag(null, TAG_USER);
        serializer.attribute(null, ATTR_ID, Integer.toString(userInfo.id));
        serializer.attribute(null, ATTR_SERIAL_NO, Integer.toString(userInfo.serialNumber));
        serializer.attribute(null, ATTR_FLAGS, Integer.toString(userInfo.flags));
        serializer.attribute(null, ATTR_CREATION_TIME, Long.toString(userInfo.creationTime));
        serializer.attribute(null, ATTR_LAST_LOGGED_IN_TIME, Long.toString(userInfo.lastLoggedInTime));
        if (userInfo.lastLoggedInFingerprint != null) {
            serializer.attribute(null, ATTR_LAST_LOGGED_IN_FINGERPRINT, userInfo.lastLoggedInFingerprint);
        }
        if (userInfo.iconPath != null) {
            serializer.attribute(null, ATTR_ICON_PATH, userInfo.iconPath);
        }
        if (userInfo.partial) {
            serializer.attribute(null, ATTR_PARTIAL, "true");
        }
        if (userInfo.guestToRemove) {
            serializer.attribute(null, ATTR_GUEST_TO_REMOVE, "true");
        }
        if (userInfo.profileGroupId != -10000) {
            serializer.attribute(null, ATTR_PROFILE_GROUP_ID, Integer.toString(userInfo.profileGroupId));
        }
        serializer.attribute(null, ATTR_PROFILE_BADGE, Integer.toString(userInfo.profileBadge));
        if (userInfo.restrictedProfileParentId != -10000) {
            serializer.attribute(null, ATTR_RESTRICTED_PROFILE_PARENT_ID, Integer.toString(userInfo.restrictedProfileParentId));
        }
        if (userData.persistSeedData) {
            if (userData.seedAccountName != null) {
                serializer.attribute(null, ATTR_SEED_ACCOUNT_NAME, userData.seedAccountName);
            }
            if (userData.seedAccountType != null) {
                serializer.attribute(null, ATTR_SEED_ACCOUNT_TYPE, userData.seedAccountType);
            }
        }
        if (userInfo.name != null) {
            serializer.startTag(null, "name");
            serializer.text(userInfo.name);
            serializer.endTag(null, "name");
        }
        synchronized (this.mRestrictionsLock) {
            UserRestrictionsUtils.writeRestrictions(serializer, (Bundle) this.mBaseUserRestrictions.get(userInfo.id), TAG_RESTRICTIONS);
            UserRestrictionsUtils.writeRestrictions(serializer, (Bundle) this.mDevicePolicyLocalUserRestrictions.get(userInfo.id), TAG_DEVICE_POLICY_RESTRICTIONS);
            UserRestrictionsUtils.writeRestrictions(serializer, (Bundle) this.mDevicePolicyGlobalUserRestrictions.get(userInfo.id), TAG_DEVICE_POLICY_GLOBAL_RESTRICTIONS);
        }
        if (userData.account != null) {
            serializer.startTag(null, TAG_ACCOUNT);
            serializer.text(userData.account);
            serializer.endTag(null, TAG_ACCOUNT);
        }
        if (userData.persistSeedData && userData.seedAccountOptions != null) {
            serializer.startTag(null, TAG_SEED_ACCOUNT_OPTIONS);
            userData.seedAccountOptions.saveToXml(serializer);
            serializer.endTag(null, TAG_SEED_ACCOUNT_OPTIONS);
        }
        serializer.endTag(null, TAG_USER);
        serializer.endDocument();
    }

    private void writeUserListLP() {
        FileOutputStream fos = null;
        AtomicFile userListFile = new AtomicFile(this.mUserListFile);
        try {
            int[] userIdsToWrite;
            int i;
            int i2;
            fos = userListFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(bos, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "users");
            serializer.attribute(null, ATTR_NEXT_SERIAL_NO, Integer.toString(this.mNextSerialNumber));
            serializer.attribute(null, ATTR_USER_VERSION, Integer.toString(this.mUserVersion));
            serializer.startTag(null, TAG_GUEST_RESTRICTIONS);
            synchronized (this.mGuestRestrictions) {
                UserRestrictionsUtils.writeRestrictions(serializer, this.mGuestRestrictions, TAG_RESTRICTIONS);
            }
            serializer.endTag(null, TAG_GUEST_RESTRICTIONS);
            serializer.startTag(null, TAG_DEVICE_OWNER_USER_ID);
            serializer.attribute(null, ATTR_ID, Integer.toString(this.mDeviceOwnerUserId));
            serializer.endTag(null, TAG_DEVICE_OWNER_USER_ID);
            synchronized (this.mUsersLock) {
                userIdsToWrite = new int[this.mUsers.size()];
                i = 0;
                for (i2 = 0; i2 < userIdsToWrite.length; i2++) {
                    userIdsToWrite[i2] = ((UserData) this.mUsers.valueAt(i2)).info.id;
                }
            }
            int[] userIdsToWrite2 = userIdsToWrite;
            int length = userIdsToWrite2.length;
            while (i < length) {
                i2 = userIdsToWrite2[i];
                serializer.startTag(null, TAG_USER);
                serializer.attribute(null, ATTR_ID, Integer.toString(i2));
                serializer.endTag(null, TAG_USER);
                i++;
            }
            serializer.endTag(null, "users");
            serializer.endDocument();
            userListFile.finishWrite(fos);
        } catch (Exception e) {
            userListFile.failWrite(fos);
            Slog.e(LOG_TAG, "Error writing user list");
        }
    }

    private UserData readUserLP(int id) {
        FileInputStream fis = null;
        try {
            File file = this.mUsersDir;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Integer.toString(id));
            stringBuilder.append(XML_SUFFIX);
            fis = new AtomicFile(new File(file, stringBuilder.toString())).openRead();
            UserData readUserLP = readUserLP(id, fis);
            IoUtils.closeQuietly(fis);
            return readUserLP;
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error reading user list");
        } catch (XmlPullParserException e2) {
            Slog.e(LOG_TAG, "Error reading user list");
        } catch (Throwable th) {
            IoUtils.closeQuietly(fis);
        }
        IoUtils.closeQuietly(fis);
        return null;
    }

    /* JADX WARNING: Missing block: B:93:0x0290, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    UserData readUserLP(int id, InputStream is) throws IOException, XmlPullParserException {
        int next;
        Bundle globalRestrictions;
        Bundle baseRestrictions;
        Throwable th;
        Bundle bundle;
        int i = id;
        boolean partial = false;
        boolean guestToRemove = false;
        boolean persistSeedData = false;
        PersistableBundle seedAccountOptions = null;
        Bundle baseRestrictions2 = null;
        Bundle localRestrictions = null;
        Bundle globalRestrictions2 = null;
        int serialNumber = i;
        XmlPullParser parser = Xml.newPullParser();
        int flags = 0;
        String name = null;
        parser.setInput(is, StandardCharsets.UTF_8.name());
        while (true) {
            next = parser.next();
            int type = next;
            if (next == 2) {
                next = type;
                break;
            }
            next = type;
            if (next == 1) {
                break;
            }
            InputStream inputStream = is;
        }
        String str;
        String account;
        String iconPath;
        if (next != 2) {
            str = LOG_TAG;
            account = null;
            StringBuilder stringBuilder = new StringBuilder();
            iconPath = null;
            stringBuilder.append("Unable to read user ");
            stringBuilder.append(i);
            Slog.e(str, stringBuilder.toString());
            return null;
        }
        int serialNumber2;
        int restrictedProfileParentId;
        int profileBadge;
        int restrictedProfileParentId2;
        boolean guestToRemove2;
        boolean persistSeedData2;
        String seedAccountName;
        String seedAccountType;
        PersistableBundle seedAccountOptions2;
        Bundle baseRestrictions3;
        Bundle localRestrictions2;
        String account2;
        String iconPath2;
        String lastLoggedInFingerprint;
        long lastLoggedInTime;
        long creationTime;
        int serialNumber3;
        account = null;
        iconPath = null;
        int i2;
        if (next == 2 && parser.getName().equals(TAG_USER)) {
            int storedId = readIntAttribute(parser, ATTR_ID, -1);
            if (storedId != i) {
                Slog.e(LOG_TAG, "User id does not match the file name");
                return null;
            }
            String iconPath3;
            serialNumber2 = readIntAttribute(parser, ATTR_SERIAL_NO, i);
            int flags2 = readIntAttribute(parser, ATTR_FLAGS, 0);
            next = parser.getAttributeValue(0, ATTR_ICON_PATH);
            int flags3 = flags2;
            int serialNumber4 = serialNumber2;
            long creationTime2 = readLongAttribute(parser, ATTR_CREATION_TIME, 0);
            long lastLoggedInTime2 = readLongAttribute(parser, ATTR_LAST_LOGGED_IN_TIME, 0);
            String lastLoggedInFingerprint2 = parser.getAttributeValue(null, ATTR_LAST_LOGGED_IN_FINGERPRINT);
            int profileGroupId = readIntAttribute(parser, ATTR_PROFILE_GROUP_ID, -10000);
            int profileBadge2 = readIntAttribute(parser, ATTR_PROFILE_BADGE, 0);
            restrictedProfileParentId = readIntAttribute(parser, ATTR_RESTRICTED_PROFILE_PARENT_ID, -10000);
            if ("true".equals(parser.getAttributeValue(null, ATTR_PARTIAL))) {
                partial = true;
            }
            str = parser.getAttributeValue(null, ATTR_GUEST_TO_REMOVE);
            if ("true".equals(str)) {
                guestToRemove = true;
            }
            String seedAccountName2 = parser.getAttributeValue(null, ATTR_SEED_ACCOUNT_NAME);
            String seedAccountType2 = parser.getAttributeValue(null, ATTR_SEED_ACCOUNT_TYPE);
            if (!(seedAccountName2 == null && seedAccountType2 == null)) {
                persistSeedData = true;
            }
            flags2 = parser.getDepth();
            while (true) {
                serialNumber2 = parser.next();
                int type2 = serialNumber2;
                iconPath3 = next;
                if (serialNumber2 == 1) {
                    i2 = type2;
                    break;
                }
                next = type2;
                if (next == 3 && parser.getDepth() <= flags2) {
                    i2 = next;
                    break;
                }
                String valueString;
                if (next == 3) {
                    i2 = next;
                    valueString = str;
                } else if (next == 4) {
                    i2 = next;
                    valueString = str;
                } else {
                    String tag = parser.getName();
                    i2 = next;
                    if ("name".equals(tag) != 0) {
                        next = parser.next();
                        valueString = str;
                        if (next == 4) {
                            name = parser.getText();
                        }
                    } else {
                        valueString = str;
                        if (TAG_RESTRICTIONS.equals(tag) != 0) {
                            baseRestrictions2 = UserRestrictionsUtils.readRestrictions(parser);
                        } else if (TAG_DEVICE_POLICY_RESTRICTIONS.equals(tag) != 0) {
                            localRestrictions = UserRestrictionsUtils.readRestrictions(parser);
                        } else if (TAG_DEVICE_POLICY_GLOBAL_RESTRICTIONS.equals(tag) != 0) {
                            globalRestrictions2 = UserRestrictionsUtils.readRestrictions(parser);
                        } else if (TAG_ACCOUNT.equals(tag) != 0) {
                            next = parser.next();
                            if (next == 4) {
                                account = parser.getText();
                            }
                        } else if (TAG_SEED_ACCOUNT_OPTIONS.equals(tag) != 0) {
                            seedAccountOptions = PersistableBundle.restoreFromXml(parser);
                            persistSeedData = true;
                        }
                        next = iconPath3;
                        str = valueString;
                    }
                    next = iconPath3;
                    str = valueString;
                }
                next = iconPath3;
                int i3 = i2;
                str = valueString;
            }
            XmlPullParser xmlPullParser = parser;
            profileBadge = profileBadge2;
            restrictedProfileParentId2 = restrictedProfileParentId;
            guestToRemove2 = guestToRemove;
            persistSeedData2 = persistSeedData;
            seedAccountName = seedAccountName2;
            seedAccountType = seedAccountType2;
            seedAccountOptions2 = seedAccountOptions;
            baseRestrictions3 = baseRestrictions2;
            localRestrictions2 = localRestrictions;
            globalRestrictions = globalRestrictions2;
            str = name;
            account2 = account;
            serialNumber2 = flags3;
            iconPath2 = iconPath3;
            restrictedProfileParentId = profileGroupId;
            lastLoggedInFingerprint = lastLoggedInFingerprint2;
            lastLoggedInTime = lastLoggedInTime2;
            creationTime = creationTime2;
            serialNumber3 = serialNumber4;
        } else {
            profileBadge = 0;
            restrictedProfileParentId2 = -10000;
            guestToRemove2 = false;
            persistSeedData2 = false;
            seedAccountName = null;
            seedAccountType = null;
            seedAccountOptions2 = null;
            baseRestrictions3 = null;
            localRestrictions2 = null;
            globalRestrictions = null;
            serialNumber2 = flags;
            str = name;
            account2 = account;
            iconPath2 = iconPath;
            i2 = next;
            restrictedProfileParentId = -10000;
            lastLoggedInFingerprint = null;
            lastLoggedInTime = 0;
            creationTime = 0;
            serialNumber3 = serialNumber;
        }
        UserInfo userInfo = new UserInfo(i, str, iconPath2, serialNumber2);
        userInfo.serialNumber = serialNumber3;
        userInfo.creationTime = creationTime;
        userInfo.lastLoggedInTime = lastLoggedInTime;
        userInfo.lastLoggedInFingerprint = lastLoggedInFingerprint;
        userInfo.partial = partial;
        userInfo.guestToRemove = guestToRemove2;
        userInfo.profileGroupId = restrictedProfileParentId;
        userInfo.profileBadge = profileBadge;
        userInfo.restrictedProfileParentId = restrictedProfileParentId2;
        int restrictedProfileParentId3 = restrictedProfileParentId2;
        UserData userData = new UserData();
        userData.info = userInfo;
        String account3 = account2;
        userData.account = account3;
        account3 = seedAccountName;
        userData.seedAccountName = account3;
        account3 = seedAccountType;
        userData.seedAccountType = account3;
        boolean persistSeedData3 = persistSeedData2;
        userData.persistSeedData = persistSeedData3;
        PersistableBundle seedAccountOptions3 = seedAccountOptions2;
        userData.seedAccountOptions = seedAccountOptions3;
        synchronized (this.mRestrictionsLock) {
            baseRestrictions = baseRestrictions3;
            if (baseRestrictions != null) {
                try {
                    this.mBaseUserRestrictions.put(i, baseRestrictions);
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            baseRestrictions = localRestrictions2;
            if (baseRestrictions != null) {
                try {
                    this.mDevicePolicyLocalUserRestrictions.put(i, baseRestrictions);
                } catch (Throwable th3) {
                    th = th3;
                    bundle = baseRestrictions;
                }
            }
            bundle = baseRestrictions;
            baseRestrictions = globalRestrictions;
            if (baseRestrictions != null) {
                try {
                    this.mDevicePolicyGlobalUserRestrictions.put(i, baseRestrictions);
                } catch (Throwable th4) {
                    th = th4;
                    throw th;
                }
            }
        }
        baseRestrictions = globalRestrictions;
        throw th;
    }

    private int readIntAttribute(XmlPullParser parser, String attr, int defaultValue) {
        String valueString = parser.getAttributeValue(null, attr);
        if (valueString == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(valueString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long readLongAttribute(XmlPullParser parser, String attr, long defaultValue) {
        String valueString = parser.getAttributeValue(null, attr);
        if (valueString == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(valueString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static void cleanAppRestrictionsForPackageLAr(String pkg, int userId) {
        File resFile = new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(pkg));
        if (resFile.exists()) {
            resFile.delete();
        }
    }

    public UserInfo createProfileForUser(String name, int flags, int userId, String[] disallowedPackages) {
        checkManageOrCreateUsersPermission(flags);
        return createUserInternal(name, flags, userId, disallowedPackages);
    }

    public UserInfo createProfileForUserEvenWhenDisallowed(String name, int flags, int userId, String[] disallowedPackages) {
        checkManageOrCreateUsersPermission(flags);
        return createUserInternalUnchecked(name, flags, userId, disallowedPackages);
    }

    public boolean removeUserEvenWhenDisallowed(int userHandle) {
        checkManageOrCreateUsersPermission("Only the system can remove users");
        return removeUserUnchecked(userHandle);
    }

    public UserInfo createUser(String name, int flags) {
        checkManageOrCreateUsersPermission(flags);
        return createUserInternal(name, flags, -10000);
    }

    private UserInfo createUserInternal(String name, int flags, int parentId) {
        return createUserInternal(name, flags, parentId, null);
    }

    private UserInfo createUserInternal(String name, int flags, int parentId, String[] disallowedPackages) {
        String restriction;
        if ((flags & 32) != 0) {
            restriction = "no_add_managed_profile";
        } else {
            restriction = "no_add_user";
        }
        if (!hasUserRestriction(restriction, UserHandle.getCallingUserId())) {
            return createUserInternalUnchecked(name, flags, parentId, disallowedPackages);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot add user. ");
        stringBuilder.append(restriction);
        stringBuilder.append(" is enabled.");
        Log.w(str, stringBuilder.toString());
        return null;
    }

    @VisibleForTesting
    UserData putUserInfo(UserInfo userInfo) {
        UserData userData = new UserData();
        userData.info = userInfo;
        synchronized (this.mUsers) {
            this.mUsers.put(userInfo.id, userData);
        }
        return userData;
    }

    @VisibleForTesting
    void removeUserInfo(int userId) {
        synchronized (this.mUsers) {
            this.mUsers.remove(userId);
        }
    }

    public UserInfo createRestrictedProfile(String name, int parentUserId) {
        checkManageOrCreateUsersPermission("setupRestrictedProfile");
        UserInfo user = createProfileForUser(name, 8, parentUserId, null);
        if (user == null) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            setUserRestriction("no_modify_accounts", true, user.id);
            Secure.putIntForUser(this.mContext.getContentResolver(), "location_mode", 0, user.id);
            setUserRestriction("no_share_location", true, user.id);
            return user;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private UserInfo findCurrentGuestUser() {
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            int i = 0;
            while (i < size) {
                UserInfo user = ((UserData) this.mUsers.valueAt(i)).info;
                if (!user.isGuest() || user.guestToRemove || this.mRemovingUserIds.get(user.id)) {
                    i++;
                } else {
                    return user;
                }
            }
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0045, code:
            if (r5.info.isGuest() != false) goto L_0x004c;
     */
    /* JADX WARNING: Missing block: B:24:?, code:
            r5.info.guestToRemove = true;
            r2 = r5.info;
            r2.flags |= 64;
            writeUserLP(r5);
     */
    /* JADX WARNING: Missing block: B:26:0x005d, code:
            android.os.Binder.restoreCallingIdentity(r0);
     */
    /* JADX WARNING: Missing block: B:27:0x0061, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean markGuestForDeletion(int userHandle) {
        checkManageUsersPermission("Only the system can remove users");
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean("no_remove_user", false)) {
            Log.w(LOG_TAG, "Cannot remove user. DISALLOW_REMOVE_USER is enabled.");
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    UserData userData = (UserData) this.mUsers.get(userHandle);
                    if (userHandle == 0 || userData == null || this.mRemovingUserIds.get(userHandle)) {
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
        return false;
    }

    public boolean removeUser(int userHandle) {
        boolean isManagedProfile;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeUser u");
        stringBuilder.append(userHandle);
        Slog.i(str, stringBuilder.toString());
        checkManageOrCreateUsersPermission("Only the system can remove users");
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userHandle);
            boolean z = userInfo != null && userInfo.isManagedProfile();
            isManagedProfile = z;
        }
        str = isManagedProfile ? "no_remove_managed_profile" : "no_remove_user";
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean(str, false)) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Cannot remove user. ");
            stringBuilder2.append(str);
            stringBuilder2.append(" is enabled.");
            Log.w(str2, stringBuilder2.toString());
            return false;
        }
        if (isClonedProfile(userHandle)) {
            this.mHasClonedProfile = false;
        }
        return removeUserUnchecked(userHandle);
    }

    /* JADX WARNING: Missing block: B:22:?, code:
            r6.info.partial = true;
            r5 = r6.info;
            r5.flags |= 64;
            writeUserLP(r6);
     */
    /* JADX WARNING: Missing block: B:25:?, code:
            r10.mAppOpsService.removeUser(r11);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean removeUserUnchecked(int userHandle) {
        boolean z;
        UserData userData;
        long ident = Binder.clearCallingIdentity();
        try {
            z = false;
            if (ActivityManager.getCurrentUser() == userHandle) {
                Log.w(LOG_TAG, "Current user cannot be removed");
                Binder.restoreCallingIdentity(ident);
                return false;
            }
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    userData = (UserData) this.mUsers.get(userHandle);
                    if (userHandle == 0 || userData == null || this.mRemovingUserIds.get(userHandle)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Removing user stopped, userHandle ");
                        stringBuilder.append(userHandle);
                        stringBuilder.append(" user ");
                        stringBuilder.append(userData);
                        Flog.i(900, stringBuilder.toString());
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                    addRemovingUserIdLocked(userHandle);
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Unable to notify AppOpsService of removing user", e);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
        if (userData.info.profileGroupId != -10000 && userData.info.isManagedProfile()) {
            sendProfileRemovedBroadcast(userData.info.profileGroupId, userData.info.id);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Stopping user ");
        stringBuilder2.append(userHandle);
        Flog.i(900, stringBuilder2.toString());
        try {
            int res = ActivityManager.getService().stopUser(userHandle, true, new IStopUserCallback.Stub() {
                public void userStopped(int userId) {
                    UserManagerService.this.finishRemoveUser(userId);
                }

                public void userStopAborted(int userId) {
                }
            });
            if (res == 0 && userHandle == REPAIR_MODE_USER_ID) {
                SystemProperties.set("persist.sys.RepairMode", "false");
            }
            if (res == 0) {
                z = true;
            }
            Binder.restoreCallingIdentity(ident);
            return z;
        } catch (RemoteException e2) {
            Binder.restoreCallingIdentity(ident);
            return false;
        }
    }

    @GuardedBy("mUsersLock")
    @VisibleForTesting
    void addRemovingUserIdLocked(int userId) {
        this.mRemovingUserIds.put(userId, true);
        if (userId < 128 || userId >= 148) {
            this.mRecentlyRemovedIds.add(Integer.valueOf(userId));
            if (this.mRecentlyRemovedIds.size() > 100) {
                this.mRecentlyRemovedIds.removeFirst();
            }
            return;
        }
        this.mClonedProfileRecentlyRemovedIds.add(Integer.valueOf(userId));
        if (this.mClonedProfileRecentlyRemovedIds.size() >= 20) {
            this.mClonedProfileRecentlyRemovedIds.removeFirst();
        }
    }

    void finishRemoveUser(final int userHandle) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("finishRemoveUser ");
        stringBuilder.append(userHandle);
        Flog.i(900, stringBuilder.toString());
        long ident = Binder.clearCallingIdentity();
        try {
            Intent addedIntent = new Intent("android.intent.action.USER_REMOVED");
            addedIntent.putExtra("android.intent.extra.user_handle", userHandle);
            this.mContext.sendOrderedBroadcastAsUser(addedIntent, UserHandle.ALL, "android.permission.MANAGE_USERS", new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("USER_REMOVED broadcast sent, cleaning up user data ");
                    stringBuilder.append(userHandle);
                    Flog.i(900, stringBuilder.toString());
                    new Thread() {
                        public void run() {
                            ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).onUserRemoved(userHandle);
                            UserManagerService.this.removeUserState(userHandle);
                        }
                    }.start();
                }
            }, null, -1, null, null);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void removeUserState(int userHandle) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("remove user state:");
        stringBuilder.append(userHandle);
        Slog.i(str, stringBuilder.toString());
        try {
            if (this.isSupportISec) {
                ((StorageManager) this.mContext.getSystemService(StorageManager.class)).destroyUserKeyISec(userHandle);
            } else {
                ((StorageManager) this.mContext.getSystemService(StorageManager.class)).destroyUserKey(userHandle);
            }
        } catch (IllegalStateException e) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Destroying key for user ");
            stringBuilder2.append(userHandle);
            stringBuilder2.append(" failed, continuing anyway");
            Slog.i(str2, stringBuilder2.toString(), e);
        }
        try {
            IGateKeeperService gk = GateKeeper.getService();
            if (gk != null) {
                gk.clearSecureUserId(userHandle);
            }
        } catch (Exception e2) {
            Slog.w(LOG_TAG, "unable to clear GK secure user id");
        }
        this.mPm.cleanUpUser(this, userHandle);
        this.mUserDataPreparer.destroyUserData(userHandle, 3);
        synchronized (this.mUsersLock) {
            this.mUsers.remove(userHandle);
            this.mIsUserManaged.delete(userHandle);
        }
        synchronized (this.mUserStates) {
            this.mUserStates.delete(userHandle);
        }
        synchronized (this.mRestrictionsLock) {
            this.mBaseUserRestrictions.remove(userHandle);
            this.mAppliedUserRestrictions.remove(userHandle);
            this.mCachedEffectiveUserRestrictions.remove(userHandle);
            this.mDevicePolicyLocalUserRestrictions.remove(userHandle);
            if (this.mDevicePolicyGlobalUserRestrictions.get(userHandle) != null) {
                this.mDevicePolicyGlobalUserRestrictions.remove(userHandle);
                applyUserRestrictionsForAllUsersLR();
            }
        }
        synchronized (this.mPackagesLock) {
            writeUserListLP();
        }
        File file = this.mUsersDir;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(userHandle);
        stringBuilder3.append(XML_SUFFIX);
        new AtomicFile(new File(file, stringBuilder3.toString())).delete();
        updateUserIds();
    }

    private void sendProfileRemovedBroadcast(int parentUserId, int removedUserId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendProfileRemovedBroadcast parentUserId=  ");
        stringBuilder.append(parentUserId);
        stringBuilder.append(" removedUserId= ");
        stringBuilder.append(removedUserId);
        Flog.i(900, stringBuilder.toString());
        Intent managedProfileIntent = new Intent("android.intent.action.MANAGED_PROFILE_REMOVED");
        managedProfileIntent.addFlags(1342177280);
        managedProfileIntent.putExtra("android.intent.extra.USER", new UserHandle(removedUserId));
        managedProfileIntent.putExtra("android.intent.extra.user_handle", removedUserId);
        this.mContext.sendBroadcastAsUser(managedProfileIntent, new UserHandle(parentUserId), null);
    }

    public Bundle getApplicationRestrictions(String packageName) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.USERMANAGER_GETAPPLICATIONRESTRICTIONS);
        return getApplicationRestrictionsForUser(packageName, UserHandle.getCallingUserId());
    }

    public Bundle getApplicationRestrictionsForUser(String packageName, int userId) {
        Bundle readApplicationRestrictionsLAr;
        if (!(UserHandle.getCallingUserId() == userId && UserHandle.isSameApp(Binder.getCallingUid(), getUidForPackage(packageName)))) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get application restrictions for other user/app ");
            stringBuilder.append(packageName);
            checkSystemOrRoot(stringBuilder.toString());
        }
        synchronized (this.mAppRestrictionsLock) {
            readApplicationRestrictionsLAr = readApplicationRestrictionsLAr(packageName, userId);
        }
        return readApplicationRestrictionsLAr;
    }

    public void setApplicationRestrictions(String packageName, Bundle restrictions, int userId) {
        checkSystemOrRoot("set application restrictions");
        if (restrictions != null) {
            restrictions.setDefusable(true);
        }
        synchronized (this.mAppRestrictionsLock) {
            if (restrictions != null) {
                if (!restrictions.isEmpty()) {
                    writeApplicationRestrictionsLAr(packageName, restrictions, userId);
                }
            }
            cleanAppRestrictionsForPackageLAr(packageName, userId);
        }
        Intent changeIntent = new Intent("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED");
        changeIntent.setPackage(packageName);
        changeIntent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(changeIntent, UserHandle.of(userId));
    }

    private int getUidForPackage(String packageName) {
        long ident = Binder.clearCallingIdentity();
        int i;
        int nnfe;
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            i = DumpState.DUMP_CHANGES;
            nnfe = packageManager.getApplicationInfo(packageName, i).uid;
            return nnfe;
        } catch (NameNotFoundException e) {
            nnfe = e;
            i = -1;
            return i;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    @GuardedBy("mAppRestrictionsLock")
    private static Bundle readApplicationRestrictionsLAr(String packageName, int userId) {
        return readApplicationRestrictionsLAr(new AtomicFile(new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(packageName))));
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0062 A:{PHI: r2 , Splitter: B:4:0x0016, ExcHandler: java.io.IOException (r3_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:17:0x0062, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:19:?, code:
            r4 = LOG_TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Error parsing ");
            r5.append(r7.getBaseFile());
            android.util.Log.w(r4, r5.toString(), r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mAppRestrictionsLock")
    @VisibleForTesting
    static Bundle readApplicationRestrictionsLAr(AtomicFile restrictionsFile) {
        Bundle restrictions = new Bundle();
        ArrayList<String> values = new ArrayList();
        if (!restrictionsFile.getBaseFile().exists()) {
            return restrictions;
        }
        FileInputStream fis = null;
        try {
            fis = restrictionsFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            XmlUtils.nextElement(parser);
            if (parser.getEventType() != 2) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to read restrictions file ");
                stringBuilder.append(restrictionsFile.getBaseFile());
                Slog.e(str, stringBuilder.toString());
                IoUtils.closeQuietly(fis);
                return restrictions;
            }
            while (parser.next() != 1) {
                readEntry(restrictions, values, parser);
            }
            IoUtils.closeQuietly(fis);
            return restrictions;
        } catch (Exception e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(fis);
        }
    }

    private static void readEntry(Bundle restrictions, ArrayList<String> values, XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() == 2 && parser.getName().equals(TAG_ENTRY)) {
            String key = parser.getAttributeValue(null, ATTR_KEY);
            String valType = parser.getAttributeValue(null, "type");
            String multiple = parser.getAttributeValue(null, ATTR_MULTIPLE);
            if (multiple != null) {
                values.clear();
                int count = Integer.parseInt(multiple);
                while (count > 0) {
                    int next = parser.next();
                    int type = next;
                    if (next == 1) {
                        break;
                    } else if (type == 2 && parser.getName().equals(TAG_VALUE)) {
                        values.add(parser.nextText().trim());
                        count--;
                    }
                }
                String[] valueStrings = new String[values.size()];
                values.toArray(valueStrings);
                restrictions.putStringArray(key, valueStrings);
            } else if (ATTR_TYPE_BUNDLE.equals(valType)) {
                restrictions.putBundle(key, readBundleEntry(parser, values));
            } else if (ATTR_TYPE_BUNDLE_ARRAY.equals(valType)) {
                int outerDepth = parser.getDepth();
                ArrayList<Bundle> bundleList = new ArrayList();
                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                    bundleList.add(readBundleEntry(parser, values));
                }
                restrictions.putParcelableArray(key, (Parcelable[]) bundleList.toArray(new Bundle[bundleList.size()]));
            } else {
                String value = parser.nextText().trim();
                if (ATTR_TYPE_BOOLEAN.equals(valType)) {
                    restrictions.putBoolean(key, Boolean.parseBoolean(value));
                } else if (ATTR_TYPE_INTEGER.equals(valType)) {
                    restrictions.putInt(key, Integer.parseInt(value));
                } else {
                    restrictions.putString(key, value);
                }
            }
        }
    }

    private static Bundle readBundleEntry(XmlPullParser parser, ArrayList<String> values) throws IOException, XmlPullParserException {
        Bundle childBundle = new Bundle();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            readEntry(childBundle, values, parser);
        }
        return childBundle;
    }

    @GuardedBy("mAppRestrictionsLock")
    private static void writeApplicationRestrictionsLAr(String packageName, Bundle restrictions, int userId) {
        writeApplicationRestrictionsLAr(restrictions, new AtomicFile(new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(packageName))));
    }

    @GuardedBy("mAppRestrictionsLock")
    @VisibleForTesting
    static void writeApplicationRestrictionsLAr(Bundle restrictions, AtomicFile restrictionsFile) {
        FileOutputStream fos = null;
        try {
            fos = restrictionsFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(bos, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, TAG_RESTRICTIONS);
            writeBundle(restrictions, serializer);
            serializer.endTag(null, TAG_RESTRICTIONS);
            serializer.endDocument();
            restrictionsFile.finishWrite(fos);
        } catch (Exception e) {
            restrictionsFile.failWrite(fos);
            Slog.e(LOG_TAG, "Error writing application restrictions list", e);
        }
    }

    private static void writeBundle(Bundle restrictions, XmlSerializer serializer) throws IOException {
        for (String key : restrictions.keySet()) {
            Object value = restrictions.get(key);
            serializer.startTag(null, TAG_ENTRY);
            serializer.attribute(null, ATTR_KEY, key);
            if (value instanceof Boolean) {
                serializer.attribute(null, "type", ATTR_TYPE_BOOLEAN);
                serializer.text(value.toString());
            } else if (value instanceof Integer) {
                serializer.attribute(null, "type", ATTR_TYPE_INTEGER);
                serializer.text(value.toString());
            } else if (value == null || (value instanceof String)) {
                serializer.attribute(null, "type", ATTR_TYPE_STRING);
                serializer.text(value != null ? (String) value : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            } else if (value instanceof Bundle) {
                serializer.attribute(null, "type", ATTR_TYPE_BUNDLE);
                writeBundle((Bundle) value, serializer);
            } else {
                int i = 0;
                int length;
                if (value instanceof Parcelable[]) {
                    serializer.attribute(null, "type", ATTR_TYPE_BUNDLE_ARRAY);
                    Parcelable[] array = (Parcelable[]) value;
                    length = array.length;
                    while (i < length) {
                        Parcelable parcelable = array[i];
                        if (parcelable instanceof Bundle) {
                            serializer.startTag(null, TAG_ENTRY);
                            serializer.attribute(null, "type", ATTR_TYPE_BUNDLE);
                            writeBundle((Bundle) parcelable, serializer);
                            serializer.endTag(null, TAG_ENTRY);
                            i++;
                        } else {
                            throw new IllegalArgumentException("bundle-array can only hold Bundles");
                        }
                    }
                    continue;
                } else {
                    serializer.attribute(null, "type", ATTR_TYPE_STRING_ARRAY);
                    String[] values = (String[]) value;
                    serializer.attribute(null, ATTR_MULTIPLE, Integer.toString(values.length));
                    length = values.length;
                    while (i < length) {
                        String choice = values[i];
                        serializer.startTag(null, TAG_VALUE);
                        serializer.text(choice != null ? choice : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        serializer.endTag(null, TAG_VALUE);
                        i++;
                    }
                }
            }
            serializer.endTag(null, TAG_ENTRY);
        }
    }

    public int getUserSerialNumber(int userHandle) {
        synchronized (this.mUsersLock) {
            if (exists(userHandle)) {
                int i = getUserInfoLU(userHandle).serialNumber;
                return i;
            }
            return -1;
        }
    }

    public boolean isUserNameSet(int userHandle) {
        boolean z;
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userHandle);
            z = (userInfo == null || userInfo.name == null) ? false : true;
        }
        return z;
    }

    public int getUserHandle(int userSerialNumber) {
        synchronized (this.mUsersLock) {
            int[] iArr = this.mUserIds;
            int length = iArr.length;
            int i = 0;
            while (i < length) {
                int userId = iArr[i];
                UserInfo info = getUserInfoLU(userId);
                if (info == null || info.serialNumber != userSerialNumber) {
                    i++;
                } else {
                    return userId;
                }
            }
            return -1;
        }
    }

    public long getUserCreationTime(int userHandle) {
        int callingUserId = UserHandle.getCallingUserId();
        UserInfo userInfo = null;
        synchronized (this.mUsersLock) {
            if (callingUserId == userHandle) {
                userInfo = getUserInfoLU(userHandle);
            } else {
                UserInfo parent = getProfileParentLU(userHandle);
                if (parent != null && parent.id == callingUserId) {
                    userInfo = getUserInfoLU(userHandle);
                }
            }
        }
        if (userInfo != null) {
            return userInfo.creationTime;
        }
        throw new SecurityException("userHandle can only be the calling user or a managed profile associated with this user");
    }

    private void updateUserIds() {
        synchronized (this.mUsersLock) {
            int num;
            int i;
            try {
                int userSize = this.mUsers.size();
                int i2 = 0;
                num = 0;
                for (i = 0; i < userSize; i++) {
                    if (!((UserData) this.mUsers.valueAt(i)).info.partial) {
                        num++;
                    }
                }
                i = new int[num];
                int n = 0;
                while (i2 < userSize) {
                    if (!((UserData) this.mUsers.valueAt(i2)).info.partial) {
                        int n2 = n + 1;
                        i[n] = this.mUsers.keyAt(i2);
                        n = n2;
                    }
                    i2++;
                }
                this.mUserIds = i;
            } catch (Throwable th) {
                i = th;
                throw i;
            }
        }
    }

    public void onBeforeStartUser(int userId) {
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo != null) {
            boolean migrateAppsData = Build.FINGERPRINT.equals(userInfo.lastLoggedInFingerprint) ^ true;
            this.mUserDataPreparer.prepareUserData(userId, userInfo.serialNumber, 1);
            this.mPm.reconcileAppsData(userId, 1, migrateAppsData);
            if (userId != 0) {
                synchronized (this.mRestrictionsLock) {
                    applyUserRestrictionsLR(userId);
                }
            }
        }
    }

    public void onBeforeUnlockUser(int userId) {
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo != null) {
            boolean migrateAppsData = Build.FINGERPRINT.equals(userInfo.lastLoggedInFingerprint) ^ 1;
            this.mUserDataPreparer.prepareUserData(userId, userInfo.serialNumber, 2);
            this.mPm.reconcileAppsData(userId, 2, migrateAppsData);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Prepare app storage finished onBeforeUnlockUser ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
    }

    void reconcileUsers(String volumeUuid) {
        this.mUserDataPreparer.reconcileUsers(volumeUuid, getUsers(true));
    }

    public void onUserLoggedIn(int userId) {
        UserData userData = getUserDataNoChecks(userId);
        if (userData == null || userData.info.partial) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("userForeground: unknown user #");
            stringBuilder.append(userId);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        long now = System.currentTimeMillis();
        if (now > EPOCH_PLUS_30_YEARS) {
            userData.info.lastLoggedInTime = now;
        }
        userData.info.lastLoggedInFingerprint = Build.FINGERPRINT;
        scheduleWriteUser(userData);
    }

    /* JADX WARNING: Missing block: B:28:0x0076, code:
            r0 = r1;
     */
    /* JADX WARNING: Missing block: B:29:0x0077, code:
            if (r0 < 0) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:30:0x0079, code:
            return r0;
     */
    /* JADX WARNING: Missing block: B:32:0x0081, code:
            throw new java.lang.IllegalStateException("No user id available!");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    int getNextAvailableId(boolean isClonedProfile) {
        synchronized (this.mUsersLock) {
            int nextId = scanNextAvailableIdLocked(isClonedProfile);
            if (nextId >= 0) {
                return nextId;
            } else if (this.mRemovingUserIds.size() > 0) {
                Slog.i(LOG_TAG, "All available IDs are used. Recycling LRU ids.");
                Iterator it;
                if (isClonedProfile) {
                    for (int i = 128; i < 148; i++) {
                        if (this.mRemovingUserIds.get(i)) {
                            this.mRemovingUserIds.delete(i);
                        }
                    }
                    it = this.mClonedProfileRecentlyRemovedIds.iterator();
                    while (it.hasNext()) {
                        this.mRemovingUserIds.put(((Integer) it.next()).intValue(), true);
                    }
                } else {
                    this.mRemovingUserIds.clear();
                    it = this.mRecentlyRemovedIds.iterator();
                    while (it.hasNext()) {
                        this.mRemovingUserIds.put(((Integer) it.next()).intValue(), true);
                    }
                }
                nextId = scanNextAvailableIdLocked(isClonedProfile);
            }
        }
    }

    @GuardedBy("mUsersLock")
    private int scanNextAvailableIdLocked(boolean isClonedProfile) {
        int minUserId = isClonedProfile ? 128 : 10;
        int maxUserId = isClonedProfile ? 148 : MAX_USER_ID;
        int i = minUserId;
        while (i < maxUserId) {
            if (this.mUsers.indexOfKey(i) < 0 && !this.mRemovingUserIds.get(i) && i != REPAIR_MODE_USER_ID && !this.mUserDataPreparer.isUserIdInvalid(i) && (isClonedProfile || i < 128 || i > 148)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static String packageToRestrictionsFileName(String packageName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RESTRICTIONS_FILE_PREFIX);
        stringBuilder.append(packageName);
        stringBuilder.append(XML_SUFFIX);
        return stringBuilder.toString();
    }

    /* JADX WARNING: Missing block: B:16:0x0033, code:
            if (r11 == false) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:18:?, code:
            writeUserLP(r2);
     */
    /* JADX WARNING: Missing block: B:20:0x0039, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setSeedAccountData(int userId, String accountName, String accountType, PersistableBundle accountOptions, boolean persist) {
        checkManageUsersPermission("Require MANAGE_USERS permission to set user seed data");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = getUserDataLU(userId);
                if (userData == null) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No such user for settings seed data u=");
                    stringBuilder.append(userId);
                    Slog.e(str, stringBuilder.toString());
                    return;
                }
                userData.seedAccountName = accountName;
                userData.seedAccountType = accountType;
                userData.seedAccountOptions = accountOptions;
                userData.persistSeedData = persist;
            }
        }
    }

    public String getSeedAccountName() throws RemoteException {
        String str;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            str = getUserDataLU(UserHandle.getCallingUserId()).seedAccountName;
        }
        return str;
    }

    public String getSeedAccountType() throws RemoteException {
        String str;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            str = getUserDataLU(UserHandle.getCallingUserId()).seedAccountType;
        }
        return str;
    }

    public PersistableBundle getSeedAccountOptions() throws RemoteException {
        PersistableBundle persistableBundle;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            persistableBundle = getUserDataLU(UserHandle.getCallingUserId()).seedAccountOptions;
        }
        return persistableBundle;
    }

    public void clearSeedAccountData() throws RemoteException {
        checkManageUsersPermission("Cannot clear seed account information");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = getUserDataLU(UserHandle.getCallingUserId());
                if (userData == null) {
                    return;
                }
                userData.clearSeedAccountData();
                writeUserLP(userData);
            }
        }
    }

    public boolean someUserHasSeedAccount(String accountName, String accountType) throws RemoteException {
        checkManageUsersPermission("Cannot check seed account information");
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            int i = 0;
            while (i < userSize) {
                UserData data = (UserData) this.mUsers.valueAt(i);
                if (data.info.isInitialized() || data.seedAccountName == null || !data.seedAccountName.equals(accountName) || data.seedAccountType == null || !data.seedAccountType.equals(accountType)) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new Shell(this, null).exec(this, in, out, err, args, callback, resultReceiver);
    }

    int onShellCommand(Shell shell, String cmd) {
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        try {
            int i = (cmd.hashCode() == 3322014 && cmd.equals("list")) ? 0 : -1;
            if (i == 0) {
                return runList(pw);
            }
        } catch (RemoteException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remote exception: ");
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
        }
        return -1;
    }

    private int runList(PrintWriter pw) throws RemoteException {
        IActivityManager am = ActivityManager.getService();
        List<UserInfo> users = getUsers(false);
        if (users == null) {
            pw.println("Error: couldn't get users");
            return 1;
        }
        pw.println("Users:");
        for (int i = 0; i < users.size(); i++) {
            String running = am.isUserRunning(((UserInfo) users.get(i)).id, 0) ? " running" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\t");
            stringBuilder.append(((UserInfo) users.get(i)).toString());
            stringBuilder.append(running);
            pw.println(stringBuilder.toString());
        }
        return 0;
    }

    private static void dumpTimeAgo(PrintWriter pw, StringBuilder sb, long nowTime, long time) {
        if (time == 0) {
            pw.println("<unknown>");
            return;
        }
        sb.setLength(0);
        TimeUtils.formatDuration(nowTime - time, sb);
        sb.append(" ago");
        pw.println(sb);
    }

    boolean isUserInitialized(int userId) {
        return this.mLocalService.isUserInitialized(userId);
    }

    private void removeNonSystemUsers() {
        ArrayList<UserInfo> usersToRemove = new ArrayList();
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = ((UserData) this.mUsers.valueAt(i)).info;
                if (ui.id != 0) {
                    usersToRemove.add(ui);
                }
            }
        }
        Iterator it = usersToRemove.iterator();
        while (it.hasNext()) {
            removeUser(((UserInfo) it.next()).id);
        }
    }

    private static void debug(String message) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        Log.d(str, stringBuilder.toString());
    }

    @VisibleForTesting
    static int getMaxManagedProfiles() {
        if (Build.IS_DEBUGGABLE) {
            return SystemProperties.getInt("persist.sys.max_profiles", 1);
        }
        return 1;
    }

    @VisibleForTesting
    int getFreeProfileBadgeLU(int parentUserId) {
        int i;
        int maxManagedProfiles = getMaxManagedProfiles();
        boolean[] usedBadges = new boolean[maxManagedProfiles];
        int userSize = this.mUsers.size();
        for (i = 0; i < userSize; i++) {
            UserInfo ui = ((UserData) this.mUsers.valueAt(i)).info;
            if (ui.isManagedProfile() && ui.profileGroupId == parentUserId && !this.mRemovingUserIds.get(ui.id) && ui.profileBadge < maxManagedProfiles) {
                usedBadges[ui.profileBadge] = true;
            }
        }
        for (i = 0; i < maxManagedProfiles; i++) {
            if (!usedBadges[i]) {
                return i;
            }
        }
        return 0;
    }

    boolean hasManagedProfile(int userId) {
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            int userSize = this.mUsers.size();
            int i = 0;
            while (i < userSize) {
                UserInfo profile = ((UserData) this.mUsers.valueAt(i)).info;
                if (userId == profile.id || !isProfileOf(userInfo, profile)) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    private void verifyCallingPackage(String callingPackage, int callingUid) {
        if (this.mPm.getPackageUid(callingPackage, 0, UserHandle.getUserId(callingUid)) != callingUid) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Specified package ");
            stringBuilder.append(callingPackage);
            stringBuilder.append(" does not match the calling uid ");
            stringBuilder.append(callingUid);
            throw new SecurityException(stringBuilder.toString());
        }
    }
}
