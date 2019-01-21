package android.telephony;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

public final class LocationAccessPolicy {
    private static final String LOG_TAG = LocationAccessPolicy.class.getSimpleName();

    public static boolean canAccessCellLocation(Context context, String pkgName, int uid, int pid, boolean throwOnDeniedPermission) throws SecurityException {
        Trace.beginSection("TelephonyLohcationCheck");
        boolean z = true;
        if (uid == 1001) {
            Trace.endSection();
            return true;
        }
        if (throwOnDeniedPermission) {
            context.enforcePermission("android.permission.ACCESS_COARSE_LOCATION", pid, uid, "canAccessCellLocation");
        } else if (context.checkPermission("android.permission.ACCESS_COARSE_LOCATION", pid, uid) == -1) {
            Trace.endSection();
            return false;
        }
        try {
            int opCode = AppOpsManager.permissionToOpCode("android.permission.ACCESS_COARSE_LOCATION");
            if (opCode != -1 && ((AppOpsManager) context.getSystemService(AppOpsManager.class)).noteOpNoThrow(opCode, uid, pkgName) != 0) {
                Trace.endSection();
                return false;
            } else if (isLocationModeEnabled(context, UserHandle.getUserId(uid))) {
                if (!(isCurrentProfile(context, uid) || checkInteractAcrossUsersFull(context))) {
                    z = false;
                }
                Trace.endSection();
                return z;
            } else {
                Trace.endSection();
                return false;
            }
        } catch (Throwable th) {
            Trace.endSection();
        }
    }

    private static boolean isLocationModeEnabled(Context context, int userId) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LocationManager.class);
        if (locationManager != null) {
            return locationManager.isLocationEnabledForUser(UserHandle.of(userId));
        }
        Log.w(LOG_TAG, "Couldn't get location manager, denying location access");
        return false;
    }

    private static boolean checkInteractAcrossUsersFull(Context context) {
        return context.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0;
    }

    private static boolean isCurrentProfile(Context context, int uid) {
        long token = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            int callingUserId = UserHandle.getUserId(uid);
            boolean z = true;
            if (callingUserId == currentUser) {
                return z;
            }
            for (UserInfo user : ((UserManager) context.getSystemService(UserManager.class)).getProfiles(currentUser)) {
                if (user.id == callingUserId) {
                    Binder.restoreCallingIdentity(token);
                    return z;
                }
            }
            Binder.restoreCallingIdentity(token);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
