package com.android.server.security.securityprofile;

import android.os.Binder;
import com.android.server.LocalServices;

public class SecurityProfileControllerImpl implements ISecurityProfileController {
    public boolean shouldPreventInteraction(int type, String targetPackage, int callerUid, int callerPid, String callerPackage, int userId) {
        SecurityProfileInternal mSecurityProfileInternal = (SecurityProfileInternal) LocalServices.getService(SecurityProfileInternal.class);
        if (mSecurityProfileInternal == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            boolean ret = mSecurityProfileInternal.shouldPreventInteraction(type, targetPackage, callerUid, callerPid, callerPackage, userId);
            Binder.restoreCallingIdentity(token);
            return ret;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            Throwable th2 = th;
        }
    }

    public boolean shouldPreventMediaProjection(int uid) {
        boolean ret = false;
        SecurityProfileInternal mSecurityProfileInternal = (SecurityProfileInternal) LocalServices.getService(SecurityProfileInternal.class);
        if (mSecurityProfileInternal == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            ret = mSecurityProfileInternal.shouldPreventMediaProjection(uid);
            return ret;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void handleActivityResuming(String packageName) {
        SecurityProfileInternal mSecurityProfileInternal = (SecurityProfileInternal) LocalServices.getService(SecurityProfileInternal.class);
        if (mSecurityProfileInternal != null) {
            long token = Binder.clearCallingIdentity();
            try {
                mSecurityProfileInternal.handleActivityResuming(packageName);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public boolean verifyPackage(String packageName, String path) {
        boolean ret = true;
        SecurityProfileInternal mSecurityProfileInternal = (SecurityProfileInternal) LocalServices.getService(SecurityProfileInternal.class);
        if (mSecurityProfileInternal == null) {
            return true;
        }
        long token = Binder.clearCallingIdentity();
        try {
            ret = mSecurityProfileInternal.verifyPackage(packageName, path);
            return ret;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
