package com.android.server.pm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Slog;
import com.android.server.am.HwActivityManagerService;
import java.io.File;

public class HwUserManagerService extends UserManagerService {
    private static final int CREATE_USER_STATUS = 0;
    private static final int DELETE_USER_STATUS = 1;
    private static final String TAG = "HwUserManagerService";
    private static boolean isSupportJni;
    private static HwUserManagerService mInstance = null;
    private Context mContext;

    private native void nativeSendUserChangedNotification(int i, int i2);

    static {
        isSupportJni = false;
        try {
            System.loadLibrary("hwtee_jni");
            isSupportJni = true;
        } catch (UnsatisfiedLinkError e) {
            Slog.e(TAG, "can not find lib hwtee_jni");
            isSupportJni = false;
        }
    }

    public HwUserManagerService(Context context, PackageManagerService pm, UserDataPreparer userDataPreparer, Object packagesLock) {
        super(context, pm, userDataPreparer, packagesLock);
        this.mContext = context;
        mInstance = this;
    }

    public static synchronized HwUserManagerService getInstance() {
        HwUserManagerService hwUserManagerService;
        synchronized (HwUserManagerService.class) {
            hwUserManagerService = mInstance;
        }
        return hwUserManagerService;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != 1001) {
            return super.onTransact(code, data, reply, flags);
        }
        Flog.i(900, "onTransact MKDIR_FOR_USER_TRANSACTION.");
        createUserDir(data.readInt());
        return true;
    }

    public UserInfo createProfileForUser(String name, int flags, int userId, String[] disallowedPackages) {
        if (isStorageLow()) {
            return null;
        }
        boolean isClonedProfile = (67108864 & flags) != 0;
        UserInfo parent = null;
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && isClonedProfile) {
            if ("1".equals(SystemProperties.get("persist.sys.primarysd", "0"))) {
                Slog.i(TAG, "current default location is external sdcard and forbid to create user");
                return null;
            } else if (userId != 0) {
                return null;
            } else {
                for (UserInfo user : super.getProfiles(userId, true)) {
                    if (user.isClonedProfile()) {
                        return null;
                    }
                    if (user.id == userId) {
                        parent = user;
                        if (!parent.canHaveProfile()) {
                            return null;
                        }
                    }
                }
            }
        }
        UserInfo ui = super.createProfileForUser(name, flags, userId, disallowedPackages);
        if (!(!isClonedProfile || parent == null || ui == null)) {
            pretreatClonedProfile(this.mPm, parent.id, ui.id);
        }
        if (ui != null) {
            hwCreateUser(ui.id);
        }
        return ui;
    }

    public UserInfo createUser(String name, int flags) {
        if (isStorageLow()) {
            return null;
        }
        if ((33554432 & flags) != 0) {
            for (UserInfo info : getUsers(true)) {
                if (info.isHwHiddenSpace()) {
                    Slog.e(TAG, "Hidden space already exist!");
                    return null;
                }
            }
        }
        UserInfo ui = super.createUser(name, flags);
        if (ui == null) {
            return null;
        }
        hwCreateUser(ui.id);
        setDeviceProvisioned(ui.id);
        return ui;
    }

    void setDeviceProvisioned(int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwUserManagerService setDeviceProvisioned, userId ");
        stringBuilder.append(userId);
        Flog.i(900, stringBuilder.toString());
        ContentResolver cr = this.mContext.getContentResolver();
        long identity = Binder.clearCallingIdentity();
        try {
            if ((Global.getInt(cr, "device_provisioned", 0) == 0 || Secure.getIntForUser(cr, "user_setup_complete", 0, userId) == 0) && ((PackageManagerService) ServiceManager.getService("package")).isSetupDisabled()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Setup is disabled putInt USER_SETUP_COMPLETE for userId ");
                stringBuilder2.append(userId);
                Flog.i(900, stringBuilder2.toString());
                Global.putInt(cr, "device_provisioned", 1);
                Secure.putIntForUser(cr, "user_setup_complete", 1, userId);
            }
            Binder.restoreCallingIdentity(identity);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    void finishRemoveUser(int userHandle) {
        super.finishRemoveUser(userHandle);
        hwRemoveUser(userHandle);
    }

    private void hwCreateUser(int userid) {
        if (userid > 0 && isSupportJni) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("native create user ");
            stringBuilder.append(userid);
            Slog.i(str, stringBuilder.toString());
            nativeSendUserChangedNotification(0, userid);
        }
    }

    private void hwRemoveUser(int userid) {
        if (userid > 0 && isSupportJni) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("native remove user ");
            stringBuilder.append(userid);
            Slog.i(str, stringBuilder.toString());
            nativeSendUserChangedNotification(1, userid);
        }
    }

    private void createUserDir(int userId) {
        File userDir = Environment.getUserSystemDirectory(userId);
        if (!userDir.exists() && !userDir.mkdir()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create user directory for ");
            stringBuilder.append(userId);
            Slog.w(str, stringBuilder.toString());
        }
    }

    private boolean isStorageLow() {
        boolean isStorageLow = ((PackageManagerService) ServiceManager.getService("package")).isStorageLow();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PackageManagerService.isStorageLow() = ");
        stringBuilder.append(isStorageLow);
        Slog.i(str, stringBuilder.toString());
        return isStorageLow;
    }

    private void pretreatClonedProfile(PackageManagerService pm, int parentUserId, int clonedProfileUserId) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP) {
            long callingId = Binder.clearCallingIdentity();
            try {
                restoreDataForClone(pm, parentUserId, clonedProfileUserId);
                pm.deleteNonRequiredAppsForClone(clonedProfileUserId);
                pm.flushPackageRestrictionsAsUser(clonedProfileUserId);
                super.setUserRestriction("no_outgoing_calls", false, clonedProfileUserId);
                super.setUserRestriction("no_sms", false, clonedProfileUserId);
                Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, clonedProfileUserId);
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
    }

    private void restoreDataForClone(PackageManagerService pm, int parentUserId, int clonedProfileUserId) {
        if (parentUserId == 0) {
            String cloneAppList = Secure.getStringForUser(this.mContext.getContentResolver(), "clone_app_list", parentUserId);
            if (!TextUtils.isEmpty(cloneAppList)) {
                for (String pkg : cloneAppList.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER)) {
                    if (!(TextUtils.isEmpty(pkg) || pm.getPackageInfo(pkg, 0, parentUserId) == null)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Install existing package [");
                        stringBuilder.append(pkg);
                        stringBuilder.append("] as user ");
                        stringBuilder.append(clonedProfileUserId);
                        Slog.i(str, stringBuilder.toString());
                        pm.installExistingPackageAsUser(pkg, clonedProfileUserId, 0, 0);
                        pm.setPackageStoppedState(pkg, false, clonedProfileUserId);
                        pm.restoreAppDataForClone(pkg, parentUserId, clonedProfileUserId);
                    }
                }
            }
        }
    }

    public boolean isClonedProfile(int userId) {
        if (userId == 0) {
            return false;
        }
        boolean isClonedProfile = false;
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo ui = super.getUserInfo(userId);
            if (ui != null) {
                isClonedProfile = ui.isClonedProfile();
            }
            Binder.restoreCallingIdentity(ident);
            return isClonedProfile;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public UserInfo getUserInfo(int userId) {
        int callingUserId = UserHandle.getUserId(Binder.getCallingUid());
        if (!isClonedProfile(userId) && !isClonedProfile(callingUserId)) {
            return super.getUserInfo(userId);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = super.getUserInfo(userId);
            return userInfo;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}
