package com.android.server.security.securityprofile;

import android.os.Binder;
import android.os.SystemProperties;
import com.android.server.LocalServices;
import com.android.server.wifipro.WifiProCommonUtils;
import java.io.File;

public class SecurityProfileControllerImpl implements ISecurityProfileController {
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final boolean SUPPORT_HW_SEAPP = "true".equalsIgnoreCase(SystemProperties.get("ro.config.support_iseapp", "false"));

    public boolean shouldPreventInteraction(int type, String targetPackage, IntentCaller caller, int userId) {
        SecurityProfileInternal securityProfileInternal = (SecurityProfileInternal) LocalServices.getService(SecurityProfileInternal.class);
        if (securityProfileInternal == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            return securityProfileInternal.shouldPreventInteraction(type, targetPackage, caller, userId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean shouldPreventMediaProjection(int uid) {
        return false;
    }

    public void handleActivityResuming(String packageName) {
    }

    public boolean verifyPackage(String packageName, File path) {
        boolean result = true;
        SecurityProfileInternal securityProfileInternal = (SecurityProfileInternal) LocalServices.getService(SecurityProfileInternal.class);
        if (securityProfileInternal != null) {
            long token = Binder.clearCallingIdentity();
            try {
                if (SUPPORT_HW_SEAPP && IS_CHINA_AREA) {
                    result = securityProfileInternal.verifyPackage(packageName, path);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return result;
    }
}
