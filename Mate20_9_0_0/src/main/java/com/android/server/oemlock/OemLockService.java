package com.android.server.oemlock;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.oemlock.V1_0.IOemLock;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.UserManagerInternal.UserRestrictionsListener;
import android.service.oemlock.IOemLockService.Stub;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.PersistentDataBlockManagerInternal;
import com.android.server.SystemService;
import com.android.server.pm.UserRestrictionsUtils;

public class OemLockService extends SystemService {
    private static final String FLASH_LOCK_PROP = "ro.boot.flash.locked";
    private static final String FLASH_LOCK_UNLOCKED = "0";
    private static final String TAG = "OemLock";
    private Context mContext;
    private OemLock mOemLock;
    private final IBinder mService;
    private final UserRestrictionsListener mUserRestrictionsListener;

    public static boolean isHalPresent() {
        return VendorLock.getOemLockHalService() != null;
    }

    private static OemLock getOemLock(Context context) {
        IOemLock oemLockHal = VendorLock.getOemLockHalService();
        if (oemLockHal != null) {
            Slog.i(TAG, "Using vendor lock via the HAL");
            return new VendorLock(context, oemLockHal);
        }
        Slog.i(TAG, "Using persistent data block based lock");
        return new PersistentDataBlockLock(context);
    }

    public OemLockService(Context context) {
        this(context, getOemLock(context));
    }

    OemLockService(Context context, OemLock oemLock) {
        super(context);
        this.mUserRestrictionsListener = new UserRestrictionsListener() {
            public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
                if (UserRestrictionsUtils.restrictionsChanged(prevRestrictions, newRestrictions, "no_factory_reset") && !(newRestrictions.getBoolean("no_factory_reset") ^ 1)) {
                    OemLockService.this.mOemLock.setOemUnlockAllowedByDevice(false);
                    OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(false);
                }
            }
        };
        this.mService = new Stub() {
            public void setOemUnlockAllowedByCarrier(boolean allowed, byte[] signature) {
                OemLockService.this.enforceManageCarrierOemUnlockPermission();
                OemLockService.this.enforceUserIsAdmin();
                long token = Binder.clearCallingIdentity();
                try {
                    OemLockService.this.mOemLock.setOemUnlockAllowedByCarrier(allowed, signature);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public boolean isOemUnlockAllowedByCarrier() {
                OemLockService.this.enforceManageCarrierOemUnlockPermission();
                long token = Binder.clearCallingIdentity();
                try {
                    boolean isOemUnlockAllowedByCarrier = OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier();
                    return isOemUnlockAllowedByCarrier;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public void setOemUnlockAllowedByUser(boolean allowedByUser) {
                if (!ActivityManager.isUserAMonkey()) {
                    OemLockService.this.enforceManageUserOemUnlockPermission();
                    OemLockService.this.enforceUserIsAdmin();
                    long token = Binder.clearCallingIdentity();
                    try {
                        if (!OemLockService.this.isOemUnlockAllowedByAdmin()) {
                            throw new SecurityException("Admin does not allow OEM unlock");
                        } else if (OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier()) {
                            OemLockService.this.mOemLock.setOemUnlockAllowedByDevice(allowedByUser);
                            OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(allowedByUser);
                        } else {
                            throw new SecurityException("Carrier does not allow OEM unlock");
                        }
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                }
            }

            public boolean isOemUnlockAllowedByUser() {
                OemLockService.this.enforceManageUserOemUnlockPermission();
                long token = Binder.clearCallingIdentity();
                try {
                    boolean isOemUnlockAllowedByDevice = OemLockService.this.mOemLock.isOemUnlockAllowedByDevice();
                    return isOemUnlockAllowedByDevice;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public boolean isOemUnlockAllowed() {
                OemLockService.this.enforceOemUnlockReadPermission();
                long token = Binder.clearCallingIdentity();
                try {
                    boolean allowed = OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier() && OemLockService.this.mOemLock.isOemUnlockAllowedByDevice();
                    OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(allowed);
                    return allowed;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public boolean isDeviceOemUnlocked() {
                OemLockService.this.enforceOemUnlockReadPermission();
                String locked = SystemProperties.get(OemLockService.FLASH_LOCK_PROP);
                boolean z = (locked.hashCode() == 48 && locked.equals(OemLockService.FLASH_LOCK_UNLOCKED)) ? false : true;
                if (z) {
                    return false;
                }
                return true;
            }
        };
        this.mContext = context;
        this.mOemLock = oemLock;
        ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).addUserRestrictionsListener(this.mUserRestrictionsListener);
    }

    public void onStart() {
        publishBinderService("oem_lock", this.mService);
    }

    private void setPersistentDataBlockOemUnlockAllowedBit(boolean allowed) {
        PersistentDataBlockManagerInternal pdbmi = (PersistentDataBlockManagerInternal) LocalServices.getService(PersistentDataBlockManagerInternal.class);
        if (pdbmi != null && !(this.mOemLock instanceof PersistentDataBlockLock)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Update OEM Unlock bit in pst partition to ");
            stringBuilder.append(allowed);
            Slog.i(str, stringBuilder.toString());
            pdbmi.forceOemUnlockEnabled(allowed);
        }
    }

    private boolean isOemUnlockAllowedByAdmin() {
        return UserManager.get(this.mContext).hasUserRestriction("no_factory_reset", UserHandle.SYSTEM) ^ 1;
    }

    private void enforceManageCarrierOemUnlockPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_CARRIER_OEM_UNLOCK_STATE", "Can't manage OEM unlock allowed by carrier");
    }

    private void enforceManageUserOemUnlockPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USER_OEM_UNLOCK_STATE", "Can't manage OEM unlock allowed by user");
    }

    private void enforceOemUnlockReadPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_OEM_UNLOCK_STATE") == -1 && this.mContext.checkCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE") == -1) {
            throw new SecurityException("Can't access OEM unlock state. Requires READ_OEM_UNLOCK_STATE or OEM_UNLOCK_STATE permission.");
        }
    }

    private void enforceUserIsAdmin() {
        int userId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            if (!UserManager.get(this.mContext).isUserAdmin(userId)) {
                throw new SecurityException("Must be an admin user");
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
