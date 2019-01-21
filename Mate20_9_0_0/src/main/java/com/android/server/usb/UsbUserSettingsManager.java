package com.android.server.usb;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.pm.PackageManagerService;
import java.util.HashMap;
import java.util.Iterator;

class UsbUserSettingsManager {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbUserSettingsManager";
    private final HashMap<UsbAccessory, SparseBooleanArray> mAccessoryPermissionMap = new HashMap();
    private final HashMap<String, SparseBooleanArray> mDevicePermissionMap = new HashMap();
    private final boolean mDisablePermissionDialogs;
    private final Object mLock = new Object();
    private final PackageManager mPackageManager;
    private final UserHandle mUser;
    private final Context mUserContext;

    public UsbUserSettingsManager(Context context, UserHandle user) {
        try {
            this.mUserContext = context.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, user);
            this.mPackageManager = this.mUserContext.getPackageManager();
            this.mUser = user;
            this.mDisablePermissionDialogs = context.getResources().getBoolean(17956931);
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Missing android package");
        }
    }

    void removeDevicePermissions(UsbDevice device) {
        synchronized (this.mLock) {
            this.mDevicePermissionMap.remove(device.getDeviceName());
        }
    }

    void removeAccessoryPermissions(UsbAccessory accessory) {
        synchronized (this.mLock) {
            this.mAccessoryPermissionMap.remove(accessory);
        }
    }

    private boolean isCameraDevicePresent(UsbDevice device) {
        if (device.getDeviceClass() == 14) {
            return true;
        }
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            if (device.getInterface(i).getInterfaceClass() == 14) {
                return true;
            }
        }
        return false;
    }

    private boolean isCameraPermissionGranted(String packageName, int uid) {
        int targetSdkVersion = 28;
        try {
            ApplicationInfo aInfo = this.mPackageManager.getApplicationInfo(packageName, 0);
            if (aInfo.uid != uid) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(packageName);
                stringBuilder.append(" does not match caller's uid ");
                stringBuilder.append(uid);
                Slog.i(str, stringBuilder.toString());
                return false;
            } else if (aInfo.targetSdkVersion < 28 || -1 != this.mUserContext.checkCallingPermission("android.permission.CAMERA")) {
                return true;
            } else {
                Slog.i(TAG, "Camera permission required for USB video class devices");
                return false;
            }
        } catch (NameNotFoundException e) {
            Slog.i(TAG, "Package not found, likely due to invalid package name!");
            return false;
        }
    }

    public boolean hasPermission(UsbDevice device, String packageName, int uid) {
        synchronized (this.mLock) {
            if (!isCameraDevicePresent(device) || isCameraPermissionGranted(packageName, uid)) {
                if (uid != 1000) {
                    if (!this.mDisablePermissionDialogs) {
                        SparseBooleanArray uidList = (SparseBooleanArray) this.mDevicePermissionMap.get(device.getDeviceName());
                        if (uidList == null) {
                            return false;
                        }
                        boolean z = uidList.get(uid);
                        return z;
                    }
                }
                return true;
            }
            return false;
        }
    }

    public boolean hasPermission(UsbAccessory accessory) {
        synchronized (this.mLock) {
            int uid = Binder.getCallingUid();
            if (uid != 1000) {
                if (!this.mDisablePermissionDialogs) {
                    SparseBooleanArray uidList = (SparseBooleanArray) this.mAccessoryPermissionMap.get(accessory);
                    if (uidList == null) {
                        return false;
                    }
                    boolean z = uidList.get(uid);
                    return z;
                }
            }
            return true;
        }
    }

    public void checkPermission(UsbDevice device, String packageName, int uid) {
        if (!hasPermission(device, packageName, uid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User has not given permission to device ");
            stringBuilder.append(device);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public void checkPermission(UsbAccessory accessory) {
        if (!hasPermission(accessory)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User has not given permission to accessory ");
            stringBuilder.append(accessory);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void requestPermissionDialog(Intent intent, String packageName, PendingIntent pi) {
        int uid = Binder.getCallingUid();
        StringBuilder stringBuilder;
        try {
            if (this.mPackageManager.getApplicationInfo(packageName, 0).uid == uid) {
                ApplicationInfo aInfo = Binder.clearCallingIdentity();
                intent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbPermissionActivity");
                intent.addFlags(268435456);
                intent.putExtra("android.intent.extra.INTENT", pi);
                intent.putExtra("package", packageName);
                intent.putExtra("android.intent.extra.UID", uid);
                try {
                    this.mUserContext.startActivityAsUser(intent, this.mUser);
                } catch (ActivityNotFoundException e) {
                    Slog.e(TAG, "unable to start UsbPermissionActivity");
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(aInfo);
                }
                Binder.restoreCallingIdentity(aInfo);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" does not match caller's uid ");
            stringBuilder.append(uid);
            throw new IllegalArgumentException(stringBuilder.toString());
        } catch (NameNotFoundException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" not found");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void requestPermission(UsbDevice device, String packageName, PendingIntent pi, int uid) {
        Intent intent = new Intent();
        if (hasPermission(device, packageName, uid)) {
            intent.putExtra("device", device);
            intent.putExtra("permission", true);
            try {
                pi.send(this.mUserContext, 0, intent);
            } catch (CanceledException e) {
            }
        } else if (!isCameraDevicePresent(device) || isCameraPermissionGranted(packageName, uid)) {
            intent.putExtra("device", device);
            requestPermissionDialog(intent, packageName, pi);
        } else {
            intent.putExtra("device", device);
            intent.putExtra("permission", false);
            try {
                pi.send(this.mUserContext, 0, intent);
            } catch (CanceledException e2) {
            }
        }
    }

    public void requestPermission(UsbAccessory accessory, String packageName, PendingIntent pi) {
        Intent intent = new Intent();
        if (hasPermission(accessory)) {
            intent.putExtra("accessory", accessory);
            intent.putExtra("permission", true);
            try {
                pi.send(this.mUserContext, 0, intent);
            } catch (CanceledException e) {
            }
            return;
        }
        intent.putExtra("accessory", accessory);
        requestPermissionDialog(intent, packageName, pi);
    }

    public void grantDevicePermission(UsbDevice device, int uid) {
        synchronized (this.mLock) {
            String deviceName = device.getDeviceName();
            SparseBooleanArray uidList = (SparseBooleanArray) this.mDevicePermissionMap.get(deviceName);
            if (uidList == null) {
                uidList = new SparseBooleanArray(1);
                this.mDevicePermissionMap.put(deviceName, uidList);
            }
            uidList.put(uid, true);
        }
    }

    public void grantAccessoryPermission(UsbAccessory accessory, int uid) {
        synchronized (this.mLock) {
            SparseBooleanArray uidList = (SparseBooleanArray) this.mAccessoryPermissionMap.get(accessory);
            if (uidList == null) {
                uidList = new SparseBooleanArray(1);
                this.mAccessoryPermissionMap.put(accessory, uidList);
            }
            uidList.put(uid, true);
        }
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        DualDumpOutputStream dualDumpOutputStream = dump;
        long token = dump.start(idName, id);
        synchronized (this.mLock) {
            long j;
            long devicePermissionToken;
            int i;
            dualDumpOutputStream.write("user_id", 1120986464257L, this.mUser.getIdentifier());
            Iterator it = this.mDevicePermissionMap.keySet().iterator();
            while (true) {
                j = 1138166333441L;
                if (!it.hasNext()) {
                    break;
                }
                String deviceName = (String) it.next();
                devicePermissionToken = dualDumpOutputStream.start("device_permissions", 2246267895810L);
                dualDumpOutputStream.write("device_name", 1138166333441L, deviceName);
                SparseBooleanArray uidList = (SparseBooleanArray) this.mDevicePermissionMap.get(deviceName);
                int count = uidList.size();
                for (i = 0; i < count; i++) {
                    dualDumpOutputStream.write("uids", 2220498092034L, uidList.keyAt(i));
                }
                dualDumpOutputStream.end(devicePermissionToken);
            }
            for (UsbAccessory accessory : this.mAccessoryPermissionMap.keySet()) {
                devicePermissionToken = dualDumpOutputStream.start("accessory_permissions", 2246267895811L);
                dualDumpOutputStream.write("accessory_description", j, accessory.getDescription());
                SparseBooleanArray uidList2 = (SparseBooleanArray) this.mAccessoryPermissionMap.get(accessory);
                int count2 = uidList2.size();
                int i2 = 0;
                while (true) {
                    i = i2;
                    if (i >= count2) {
                        break;
                    }
                    dualDumpOutputStream.write("uids", 2220498092034L, uidList2.keyAt(i));
                    i2 = i + 1;
                }
                dualDumpOutputStream.end(devicePermissionToken);
                j = 1138166333441L;
            }
        }
        dualDumpOutputStream.end(token);
    }
}
