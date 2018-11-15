package com.android.server.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.IUsbManager.Stub;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.SystemService;
import com.android.server.usb.HwUsbServiceFactory.IHwUsbDeviceManager;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;

public class UsbService extends Stub {
    private static final String TAG = "UsbService";
    private final UsbAlsaManager mAlsaManager;
    private final Context mContext;
    @GuardedBy("mLock")
    private int mCurrentUserId;
    private UsbDeviceManager mDeviceManager;
    private UsbHostManager mHostManager;
    private final Object mLock = new Object();
    private UsbPortManager mPortManager;
    private final UsbSettingsManager mSettingsManager;
    private final UserManager mUserManager;

    public static class Lifecycle extends SystemService {
        private UsbService mUsbService;

        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            this.mUsbService = new UsbService(getContext());
            publishBinderService("usb", this.mUsbService);
        }

        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mUsbService.systemReady();
            } else if (phase == 1000) {
                this.mUsbService.bootCompleted();
            }
        }

        public void onSwitchUser(int newUserId) {
            this.mUsbService.onSwitchUser(newUserId);
        }

        public void onStopUser(int userHandle) {
            this.mUsbService.onStopUser(UserHandle.of(userHandle));
        }

        public void onUnlockUser(int userHandle) {
            this.mUsbService.onUnlockUser(userHandle);
        }
    }

    private UsbUserSettingsManager getSettingsForUser(int userIdInt) {
        return this.mSettingsManager.getSettingsForUser(userIdInt);
    }

    public UsbService(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mSettingsManager = new UsbSettingsManager(context);
        this.mAlsaManager = new UsbAlsaManager(context);
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.usb.host")) {
            this.mHostManager = new UsbHostManager(context, this.mAlsaManager, this.mSettingsManager);
        }
        if (new File("/sys/class/android_usb").exists()) {
            IHwUsbDeviceManager iudm = HwUsbServiceFactory.getHuaweiUsbDeviceManager();
            if (iudm != null) {
                this.mDeviceManager = iudm.getInstance(context, this.mAlsaManager, this.mSettingsManager);
            } else {
                this.mDeviceManager = new UsbDeviceManager(context, this.mAlsaManager, this.mSettingsManager);
            }
        }
        if (!(this.mHostManager == null && this.mDeviceManager == null)) {
            this.mPortManager = new UsbPortManager(context);
        }
        onSwitchUser(0);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction()) && UsbService.this.mDeviceManager != null) {
                    UsbService.this.mDeviceManager.updateUserRestrictions();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.setPriority(1000);
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiver(receiver, filter, null, null);
    }

    private void onSwitchUser(int newUserId) {
        synchronized (this.mLock) {
            this.mCurrentUserId = newUserId;
            UsbProfileGroupSettingsManager settings = this.mSettingsManager.getSettingsForProfileGroup(UserHandle.of(newUserId));
            if (this.mHostManager != null) {
                this.mHostManager.setCurrentUserSettings(settings);
            }
            if (this.mDeviceManager != null) {
                this.mDeviceManager.setCurrentUser(newUserId, settings);
            }
        }
    }

    private void onStopUser(UserHandle stoppedUser) {
        this.mSettingsManager.remove(stoppedUser);
    }

    public void systemReady() {
        this.mAlsaManager.systemReady();
        if (this.mDeviceManager != null) {
            this.mDeviceManager.systemReady();
        }
        if (this.mHostManager != null) {
            this.mHostManager.systemReady();
        }
        if (this.mPortManager != null) {
            this.mPortManager.systemReady();
        }
    }

    public void bootCompleted() {
        if (this.mDeviceManager != null) {
            this.mDeviceManager.bootCompleted();
        }
    }

    public void onUnlockUser(int user) {
        if (this.mDeviceManager != null) {
            this.mDeviceManager.onUnlockUser(user);
        }
    }

    public void getDeviceList(Bundle devices) {
        if (this.mHostManager != null) {
            this.mHostManager.getDeviceList(devices);
        }
    }

    @GuardedBy("mLock")
    private boolean isCallerInCurrentUserProfileGroupLocked() {
        int userIdInt = UserHandle.getCallingUserId();
        long ident = clearCallingIdentity();
        try {
            boolean isSameProfileGroup = this.mUserManager.isSameProfileGroup(userIdInt, this.mCurrentUserId);
            return isSameProfileGroup;
        } finally {
            restoreCallingIdentity(ident);
        }
    }

    public ParcelFileDescriptor openDevice(String deviceName, String packageName) {
        ParcelFileDescriptor fd = null;
        if (this.mHostManager != null) {
            synchronized (this.mLock) {
                if (deviceName != null) {
                    int userIdInt = UserHandle.getCallingUserId();
                    if (isCallerInCurrentUserProfileGroupLocked()) {
                        fd = this.mHostManager.openDevice(deviceName, getSettingsForUser(userIdInt), packageName, Binder.getCallingUid());
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot open ");
                        stringBuilder.append(deviceName);
                        stringBuilder.append(" for user ");
                        stringBuilder.append(userIdInt);
                        stringBuilder.append(" as user is not active.");
                        Slog.w(str, stringBuilder.toString());
                    }
                }
            }
        }
        return fd;
    }

    public UsbAccessory getCurrentAccessory() {
        if (this.mDeviceManager != null) {
            return this.mDeviceManager.getCurrentAccessory();
        }
        return null;
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory accessory) {
        if (this.mDeviceManager != null) {
            int userIdInt = UserHandle.getCallingUserId();
            synchronized (this.mLock) {
                if (isCallerInCurrentUserProfileGroupLocked()) {
                    ParcelFileDescriptor openAccessory = this.mDeviceManager.openAccessory(accessory, getSettingsForUser(userIdInt));
                    return openAccessory;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot open ");
                stringBuilder.append(accessory);
                stringBuilder.append(" for user ");
                stringBuilder.append(userIdInt);
                stringBuilder.append(" as user is not active.");
                Slog.w(str, stringBuilder.toString());
            }
        }
        return null;
    }

    public ParcelFileDescriptor getControlFd(long function) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_MTP", null);
        return this.mDeviceManager.getControlFd(function);
    }

    public void setDevicePackage(UsbDevice device, String packageName, int userId) {
        device = (UsbDevice) Preconditions.checkNotNull(device);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle user = UserHandle.of(userId);
        this.mSettingsManager.getSettingsForProfileGroup(user).setDevicePackage(device, packageName, user);
    }

    public void setAccessoryPackage(UsbAccessory accessory, String packageName, int userId) {
        accessory = (UsbAccessory) Preconditions.checkNotNull(accessory);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle user = UserHandle.of(userId);
        this.mSettingsManager.getSettingsForProfileGroup(user).setAccessoryPackage(accessory, packageName, user);
    }

    public boolean hasDevicePermission(UsbDevice device, String packageName) {
        return getSettingsForUser(UserHandle.getCallingUserId()).hasPermission(device, packageName, Binder.getCallingUid());
    }

    public boolean hasAccessoryPermission(UsbAccessory accessory) {
        return getSettingsForUser(UserHandle.getCallingUserId()).hasPermission(accessory);
    }

    public void requestDevicePermission(UsbDevice device, String packageName, PendingIntent pi) {
        getSettingsForUser(UserHandle.getCallingUserId()).requestPermission(device, packageName, pi, Binder.getCallingUid());
    }

    public void requestAccessoryPermission(UsbAccessory accessory, String packageName, PendingIntent pi) {
        getSettingsForUser(UserHandle.getCallingUserId()).requestPermission(accessory, packageName, pi);
    }

    public void grantDevicePermission(UsbDevice device, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        getSettingsForUser(UserHandle.getUserId(uid)).grantDevicePermission(device, uid);
    }

    public void grantAccessoryPermission(UsbAccessory accessory, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        getSettingsForUser(UserHandle.getUserId(uid)).grantAccessoryPermission(accessory, uid);
    }

    public boolean hasDefaults(String packageName, int userId) {
        packageName = (String) Preconditions.checkStringNotEmpty(packageName);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle user = UserHandle.of(userId);
        return this.mSettingsManager.getSettingsForProfileGroup(user).hasDefaults(packageName, user);
    }

    public void clearDefaults(String packageName, int userId) {
        packageName = (String) Preconditions.checkStringNotEmpty(packageName);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle user = UserHandle.of(userId);
        this.mSettingsManager.getSettingsForProfileGroup(user).clearDefaults(packageName, user);
    }

    public void setCurrentFunctions(long functions) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkArgument(UsbManager.areSettableFunctions(functions));
        Preconditions.checkState(this.mDeviceManager != null);
        this.mDeviceManager.setCurrentFunctions(functions);
    }

    public void setCurrentFunction(String functions, boolean usbDataUnlocked) {
        setCurrentFunctions(UsbManager.usbFunctionsFromString(functions));
    }

    public boolean isFunctionEnabled(String function) {
        return (getCurrentFunctions() & UsbManager.usbFunctionsFromString(function)) != 0;
    }

    public long getCurrentFunctions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkState(this.mDeviceManager != null);
        return this.mDeviceManager.getCurrentFunctions();
    }

    public void setScreenUnlockedFunctions(long functions) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkArgument(UsbManager.areSettableFunctions(functions));
        Preconditions.checkState(this.mDeviceManager != null);
        this.mDeviceManager.setScreenUnlockedFunctions(functions);
    }

    public long getScreenUnlockedFunctions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkState(this.mDeviceManager != null);
        return this.mDeviceManager.getScreenUnlockedFunctions();
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.allowUsbDebugging(alwaysAllow, publicKey);
    }

    public void denyUsbDebugging() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.denyUsbDebugging();
    }

    public void clearUsbDebuggingKeys() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.clearUsbDebuggingKeys();
    }

    public UsbPort[] getPorts() {
        UsbPort[] usbPortArr = null;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mPortManager != null) {
                usbPortArr = this.mPortManager.getPorts();
            }
            Binder.restoreCallingIdentity(ident);
            return usbPortArr;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public UsbPortStatus getPortStatus(String portId) {
        Preconditions.checkNotNull(portId, "portId must not be null");
        UsbPortStatus usbPortStatus = null;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mPortManager != null) {
                usbPortStatus = this.mPortManager.getPortStatus(portId);
            }
            Binder.restoreCallingIdentity(ident);
            return usbPortStatus;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setPortRoles(String portId, int powerRole, int dataRole) {
        Preconditions.checkNotNull(portId, "portId must not be null");
        UsbPort.checkRoles(powerRole, dataRole);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mPortManager != null) {
                this.mPortManager.setPortRoles(portId, powerRole, dataRole, null);
            }
            Binder.restoreCallingIdentity(ident);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setUsbDeviceConnectionHandler(ComponentName usbDeviceConnectionHandler) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        synchronized (this.mLock) {
            if (this.mCurrentUserId == UserHandle.getCallingUserId()) {
                if (this.mHostManager != null) {
                    this.mHostManager.setUsbDeviceConnectionHandler(usbDeviceConnectionHandler);
                }
            } else {
                throw new IllegalArgumentException("Only the current user can register a usb connection handler");
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:40:0x009e A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x00a6 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00a4 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00a2 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d3 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00b1 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00e0 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00e7 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x00e5 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00e3 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00ee A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x009e A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x00a6 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00a4 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00a2 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00b1 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d3 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00e0 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00e7 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x00e5 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00e3 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00ee A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x009e A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x00a6 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00a4 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00a2 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d3 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00b1 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00e0 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00e7 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x00e5 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00e3 A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00ee A:{Catch:{ all -> 0x013d }} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x027a  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x0281  */
    /* JADX WARNING: Removed duplicated region for block: B:172:0x027f  */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x0299 A:{SYNTHETIC, Splitter: B:181:0x0299} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0292 A:{SYNTHETIC, Splitter: B:179:0x0292} */
    /* JADX WARNING: Removed duplicated region for block: B:192:0x02b5 A:{SYNTHETIC, Splitter: B:192:0x02b5} */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x02a4  */
    /* JADX WARNING: Removed duplicated region for block: B:197:0x02c2  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x02c9  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x02c7  */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x02fc  */
    /* JADX WARNING: Removed duplicated region for block: B:206:0x02d2 A:{Catch:{ all -> 0x0302 }} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x027a  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x0281  */
    /* JADX WARNING: Removed duplicated region for block: B:172:0x027f  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0292 A:{SYNTHETIC, Splitter: B:179:0x0292} */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x0299 A:{SYNTHETIC, Splitter: B:181:0x0299} */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x02a4  */
    /* JADX WARNING: Removed duplicated region for block: B:192:0x02b5 A:{SYNTHETIC, Splitter: B:192:0x02b5} */
    /* JADX WARNING: Removed duplicated region for block: B:197:0x02c2  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x02c9  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x02c7  */
    /* JADX WARNING: Removed duplicated region for block: B:206:0x02d2 A:{Catch:{ all -> 0x0302 }} */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x02fc  */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0233  */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x023a  */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x0238  */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x0251 A:{SYNTHETIC, Splitter: B:153:0x0251} */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x024a A:{SYNTHETIC, Splitter: B:151:0x024a} */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x026a A:{SYNTHETIC, Splitter: B:163:0x026a} */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x027a  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x0281  */
    /* JADX WARNING: Removed duplicated region for block: B:172:0x027f  */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x0299 A:{SYNTHETIC, Splitter: B:181:0x0299} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0292 A:{SYNTHETIC, Splitter: B:179:0x0292} */
    /* JADX WARNING: Removed duplicated region for block: B:192:0x02b5 A:{SYNTHETIC, Splitter: B:192:0x02b5} */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x02a4  */
    /* JADX WARNING: Removed duplicated region for block: B:197:0x02c2  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x02c9  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x02c7  */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x02fc  */
    /* JADX WARNING: Removed duplicated region for block: B:206:0x02d2 A:{Catch:{ all -> 0x0302 }} */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x0233  */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x023a  */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x0238  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x024a A:{SYNTHETIC, Splitter: B:151:0x024a} */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x0251 A:{SYNTHETIC, Splitter: B:153:0x0251} */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x026a A:{SYNTHETIC, Splitter: B:163:0x026a} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x027a  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x0281  */
    /* JADX WARNING: Removed duplicated region for block: B:172:0x027f  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0292 A:{SYNTHETIC, Splitter: B:179:0x0292} */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x0299 A:{SYNTHETIC, Splitter: B:181:0x0299} */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x02a4  */
    /* JADX WARNING: Removed duplicated region for block: B:192:0x02b5 A:{SYNTHETIC, Splitter: B:192:0x02b5} */
    /* JADX WARNING: Removed duplicated region for block: B:197:0x02c2  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x02c9  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x02c7  */
    /* JADX WARNING: Removed duplicated region for block: B:206:0x02d2 A:{Catch:{ all -> 0x0302 }} */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x02fc  */
    /* JADX WARNING: Removed duplicated region for block: B:275:0x050a A:{Catch:{ all -> 0x0575 }} */
    /* JADX WARNING: Removed duplicated region for block: B:269:0x04f9 A:{Catch:{ all -> 0x04f1, all -> 0x0506 }} */
    /* JADX WARNING: Removed duplicated region for block: B:278:0x0521 A:{Catch:{ all -> 0x0575 }} */
    /* JADX WARNING: Removed duplicated region for block: B:281:0x0536 A:{Catch:{ all -> 0x0575 }} */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0546 A:{Catch:{ all -> 0x0575 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        long ident;
        FileDescriptor fileDescriptor;
        Throwable th;
        FileDescriptor fileDescriptor2 = fd;
        Writer writer2 = writer;
        String[] strArr = args;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer2)) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer2, "  ");
            long ident2 = Binder.clearCallingIdentity();
            try {
                DualDumpOutputStream dump;
                ArraySet<String> argsSet = new ArraySet();
                Collections.addAll(argsSet, strArr);
                boolean dumpAsProto = false;
                if (argsSet.contains(PriorityDump.PROTO_ARG)) {
                    dumpAsProto = true;
                }
                boolean dumpAsProto2 = dumpAsProto;
                if (strArr != null) {
                    try {
                        if (!(strArr.length == 0 || strArr[0].equals("-a"))) {
                            if (dumpAsProto2) {
                                ident = ident2;
                                if (dumpAsProto2) {
                                    fileDescriptor = fd;
                                    pw.println("USB MANAGER STATE (dumpsys usb):");
                                    dump = new DualDumpOutputStream(new IndentingPrintWriter(pw, "  "));
                                } else {
                                    fileDescriptor = fd;
                                    try {
                                        dump = new DualDumpOutputStream(new ProtoOutputStream(fileDescriptor));
                                    } catch (Throwable th2) {
                                        th = th2;
                                        Binder.restoreCallingIdentity(ident);
                                        throw th;
                                    }
                                }
                                if (this.mDeviceManager != null) {
                                    this.mDeviceManager.dump(dump, "device_manager", 1146756268033L);
                                    this.mDeviceManager.dump(fileDescriptor, pw, strArr);
                                }
                                if (this.mHostManager != null) {
                                    this.mHostManager.dump(dump, "host_manager", 1146756268034L);
                                }
                                if (this.mPortManager != null) {
                                    this.mPortManager.dump(dump, "port_manager", 1146756268035L);
                                }
                                this.mAlsaManager.dump(dump, "alsa_manager", 1146756268036L);
                                this.mSettingsManager.dump(dump, "settings_manager", 1146756268037L);
                                dump.flush();
                                Binder.restoreCallingIdentity(ident);
                                return;
                            }
                            String portId;
                            int hashCode;
                            int i;
                            StringBuilder stringBuilder;
                            String str;
                            int dataRole;
                            int i2 = -1;
                            if ("set-port-roles".equals(strArr[0])) {
                                try {
                                    if (strArr.length == 4) {
                                        portId = strArr[1];
                                        String str2 = strArr[2];
                                        hashCode = str2.hashCode();
                                        if (hashCode != -896505829) {
                                            if (hashCode != -440560135) {
                                                if (hashCode == 3530387 && str2.equals("sink")) {
                                                    i = 1;
                                                    switch (i) {
                                                        case 0:
                                                            i = 1;
                                                            break;
                                                        case 1:
                                                            i = 2;
                                                            break;
                                                        case 2:
                                                            i = 0;
                                                            break;
                                                        default:
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("Invalid power role: ");
                                                            stringBuilder.append(strArr[2]);
                                                            pw.println(stringBuilder.toString());
                                                            Binder.restoreCallingIdentity(ident2);
                                                            return;
                                                    }
                                                    str = strArr[3];
                                                    hashCode = str.hashCode();
                                                    if (hashCode == -1335157162) {
                                                        if (hashCode != 3208616) {
                                                            if (hashCode == 2063627318 && str.equals("no-data")) {
                                                                i2 = 2;
                                                            }
                                                        } else if (str.equals(WatchlistEventKeys.HOST)) {
                                                            i2 = 0;
                                                        }
                                                    } else if (str.equals("device")) {
                                                        i2 = 1;
                                                    }
                                                    switch (i2) {
                                                        case 0:
                                                            dataRole = 1;
                                                            break;
                                                        case 1:
                                                            dataRole = 2;
                                                            break;
                                                        case 2:
                                                            dataRole = 0;
                                                            break;
                                                        default:
                                                            StringBuilder stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("Invalid data role: ");
                                                            stringBuilder2.append(strArr[3]);
                                                            pw.println(stringBuilder2.toString());
                                                            Binder.restoreCallingIdentity(ident2);
                                                            return;
                                                    }
                                                    if (this.mPortManager != null) {
                                                        this.mPortManager.setPortRoles(portId, i, dataRole, pw);
                                                        pw.println();
                                                        this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0);
                                                    }
                                                    fileDescriptor = fileDescriptor2;
                                                    ident = ident2;
                                                    Binder.restoreCallingIdentity(ident);
                                                    return;
                                                }
                                            } else if (str2.equals("no-power")) {
                                                i = 2;
                                                switch (i) {
                                                    case 0:
                                                        break;
                                                    case 1:
                                                        break;
                                                    case 2:
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                str = strArr[3];
                                                hashCode = str.hashCode();
                                                if (hashCode == -1335157162) {
                                                }
                                                switch (i2) {
                                                    case 0:
                                                        break;
                                                    case 1:
                                                        break;
                                                    case 2:
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                if (this.mPortManager != null) {
                                                }
                                                fileDescriptor = fileDescriptor2;
                                                ident = ident2;
                                                Binder.restoreCallingIdentity(ident);
                                                return;
                                            }
                                        } else if (str2.equals("source")) {
                                            i = 0;
                                            switch (i) {
                                                case 0:
                                                    break;
                                                case 1:
                                                    break;
                                                case 2:
                                                    break;
                                                default:
                                                    break;
                                            }
                                            str = strArr[3];
                                            hashCode = str.hashCode();
                                            if (hashCode == -1335157162) {
                                            }
                                            switch (i2) {
                                                case 0:
                                                    break;
                                                case 1:
                                                    break;
                                                case 2:
                                                    break;
                                                default:
                                                    break;
                                            }
                                            if (this.mPortManager != null) {
                                            }
                                            fileDescriptor = fileDescriptor2;
                                            ident = ident2;
                                            Binder.restoreCallingIdentity(ident);
                                            return;
                                        }
                                        i = -1;
                                        switch (i) {
                                            case 0:
                                                break;
                                            case 1:
                                                break;
                                            case 2:
                                                break;
                                            default:
                                                break;
                                        }
                                        str = strArr[3];
                                        hashCode = str.hashCode();
                                        if (hashCode == -1335157162) {
                                        }
                                        switch (i2) {
                                            case 0:
                                                break;
                                            case 1:
                                                break;
                                            case 2:
                                                break;
                                            default:
                                                break;
                                        }
                                        if (this.mPortManager != null) {
                                        }
                                        fileDescriptor = fileDescriptor2;
                                        ident = ident2;
                                        Binder.restoreCallingIdentity(ident);
                                        return;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    fileDescriptor = fileDescriptor2;
                                    ident = ident2;
                                }
                            }
                            if ("add-port".equals(strArr[0])) {
                                if (strArr.length == 3) {
                                    portId = strArr[1];
                                    str = strArr[2];
                                    hashCode = str.hashCode();
                                    if (hashCode != 99374) {
                                        if (hashCode != 115711) {
                                            if (hashCode != 3094652) {
                                                if (hashCode == 3387192 && str.equals("none")) {
                                                    i2 = 3;
                                                }
                                            } else if (str.equals("dual")) {
                                                i2 = 2;
                                            }
                                        } else if (str.equals("ufp")) {
                                            i2 = 0;
                                        }
                                    } else if (str.equals("dfp")) {
                                        i2 = 1;
                                    }
                                    switch (i2) {
                                        case 0:
                                            dataRole = 1;
                                            break;
                                        case 1:
                                            dataRole = 2;
                                            break;
                                        case 2:
                                            dataRole = 3;
                                            break;
                                        case 3:
                                            dataRole = 0;
                                            break;
                                        default:
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Invalid mode: ");
                                            stringBuilder.append(strArr[2]);
                                            pw.println(stringBuilder.toString());
                                            Binder.restoreCallingIdentity(ident2);
                                            return;
                                    }
                                    if (this.mPortManager != null) {
                                        this.mPortManager.addSimulatedPort(portId, dataRole, pw);
                                        pw.println();
                                        this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0);
                                    }
                                    fileDescriptor = fileDescriptor2;
                                    ident = ident2;
                                    Binder.restoreCallingIdentity(ident);
                                    return;
                                }
                            }
                            String removeLastChar;
                            if ("connect-port".equals(strArr[0]) && strArr.length == 5) {
                                Object obj;
                                int mode;
                                boolean canChangePowerRole;
                                String removeLastChar2;
                                Object obj2;
                                boolean canChangeDataRole;
                                long ident3;
                                int dataRole2;
                                portId = strArr[1];
                                boolean canChangeMode = strArr[2].endsWith("?");
                                String removeLastChar3 = canChangeMode ? removeLastChar(strArr[2]) : strArr[2];
                                int hashCode2 = removeLastChar3.hashCode();
                                if (hashCode2 != 99374) {
                                    if (hashCode2 == 115711) {
                                        if (removeLastChar3.equals("ufp")) {
                                            StringBuilder stringBuilder3;
                                            obj = null;
                                            switch (obj) {
                                                case null:
                                                    dataRole = 1;
                                                    break;
                                                case 1:
                                                    dataRole = 2;
                                                    break;
                                                default:
                                                    ident = ident2;
                                                    stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("Invalid mode: ");
                                                    stringBuilder3.append(strArr[2]);
                                                    pw.println(stringBuilder3.toString());
                                                    Binder.restoreCallingIdentity(ident);
                                                    return;
                                            }
                                            mode = dataRole;
                                            canChangePowerRole = strArr[3].endsWith("?");
                                            removeLastChar2 = canChangePowerRole ? removeLastChar(strArr[3]) : strArr[3];
                                            i = removeLastChar2.hashCode();
                                            if (i != -896505829) {
                                                if (i == 3530387) {
                                                    if (removeLastChar2.equals("sink")) {
                                                        obj2 = 1;
                                                        switch (obj2) {
                                                            case null:
                                                                dataRole = 1;
                                                                break;
                                                            case 1:
                                                                dataRole = 2;
                                                                break;
                                                            default:
                                                                ident = ident2;
                                                                stringBuilder3 = new StringBuilder();
                                                                stringBuilder3.append("Invalid power role: ");
                                                                stringBuilder3.append(strArr[3]);
                                                                pw.println(stringBuilder3.toString());
                                                                Binder.restoreCallingIdentity(ident);
                                                                return;
                                                        }
                                                        i = dataRole;
                                                        ident = 0;
                                                        canChangeDataRole = strArr[4].endsWith("?");
                                                        removeLastChar = canChangeDataRole ? removeLastChar(strArr[4]) : strArr[4];
                                                        hashCode2 = removeLastChar.hashCode();
                                                        if (hashCode2 != -1335157162) {
                                                            if (hashCode2 == 3208616) {
                                                                if (removeLastChar.equals(WatchlistEventKeys.HOST)) {
                                                                    i2 = 0;
                                                                }
                                                            }
                                                        } else if (removeLastChar.equals("device")) {
                                                            i2 = 1;
                                                        }
                                                        switch (i2) {
                                                            case 0:
                                                                dataRole = 1;
                                                                break;
                                                            case 1:
                                                                dataRole = 2;
                                                                break;
                                                            default:
                                                                ident3 = ident2;
                                                                try {
                                                                    stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append("Invalid data role: ");
                                                                    stringBuilder3.append(strArr[4]);
                                                                    pw.println(stringBuilder3.toString());
                                                                    Binder.restoreCallingIdentity(ident3);
                                                                    return;
                                                                } catch (Throwable th4) {
                                                                    th = th4;
                                                                    ident = ident3;
                                                                    fileDescriptor = fd;
                                                                    break;
                                                                }
                                                        }
                                                        dataRole2 = dataRole;
                                                        if (this.mPortManager != null) {
                                                            ident3 = ident2;
                                                            try {
                                                                this.mPortManager.connectSimulatedPort(portId, mode, canChangeMode, i, canChangePowerRole, dataRole2, canChangeDataRole, pw);
                                                                pw.println();
                                                                this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0);
                                                            } catch (Throwable th5) {
                                                                th = th5;
                                                                ident = ident3;
                                                            }
                                                        } else {
                                                            ident3 = ident2;
                                                        }
                                                        ident = ident3;
                                                    }
                                                }
                                            } else if (removeLastChar2.equals("source")) {
                                                obj2 = null;
                                                switch (obj2) {
                                                    case null:
                                                        break;
                                                    case 1:
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                i = dataRole;
                                                ident = 0;
                                                canChangeDataRole = strArr[4].endsWith("?");
                                                if (canChangeDataRole) {
                                                }
                                                hashCode2 = removeLastChar.hashCode();
                                                if (hashCode2 != -1335157162) {
                                                }
                                                switch (i2) {
                                                    case 0:
                                                        break;
                                                    case 1:
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                dataRole2 = dataRole;
                                                if (this.mPortManager != null) {
                                                }
                                                ident = ident3;
                                            }
                                            obj2 = -1;
                                            switch (obj2) {
                                                case null:
                                                    break;
                                                case 1:
                                                    break;
                                                default:
                                                    break;
                                            }
                                            i = dataRole;
                                            ident = 0;
                                            canChangeDataRole = strArr[4].endsWith("?");
                                            if (canChangeDataRole) {
                                            }
                                            hashCode2 = removeLastChar.hashCode();
                                            if (hashCode2 != -1335157162) {
                                            }
                                            switch (i2) {
                                                case 0:
                                                    break;
                                                case 1:
                                                    break;
                                                default:
                                                    break;
                                            }
                                            dataRole2 = dataRole;
                                            if (this.mPortManager != null) {
                                            }
                                            ident = ident3;
                                        }
                                    }
                                } else if (removeLastChar3.equals("dfp")) {
                                    obj = 1;
                                    switch (obj) {
                                        case null:
                                            break;
                                        case 1:
                                            break;
                                        default:
                                            break;
                                    }
                                    mode = dataRole;
                                    canChangePowerRole = strArr[3].endsWith("?");
                                    if (canChangePowerRole) {
                                    }
                                    i = removeLastChar2.hashCode();
                                    if (i != -896505829) {
                                    }
                                    obj2 = -1;
                                    switch (obj2) {
                                        case null:
                                            break;
                                        case 1:
                                            break;
                                        default:
                                            break;
                                    }
                                    i = dataRole;
                                    ident = 0;
                                    canChangeDataRole = strArr[4].endsWith("?");
                                    if (canChangeDataRole) {
                                    }
                                    hashCode2 = removeLastChar.hashCode();
                                    if (hashCode2 != -1335157162) {
                                    }
                                    switch (i2) {
                                        case 0:
                                            break;
                                        case 1:
                                            break;
                                        default:
                                            break;
                                    }
                                    dataRole2 = dataRole;
                                    if (this.mPortManager != null) {
                                    }
                                    ident = ident3;
                                }
                                obj = -1;
                                switch (obj) {
                                    case null:
                                        break;
                                    case 1:
                                        break;
                                    default:
                                        break;
                                }
                                mode = dataRole;
                                canChangePowerRole = strArr[3].endsWith("?");
                                if (canChangePowerRole) {
                                }
                                i = removeLastChar2.hashCode();
                                if (i != -896505829) {
                                }
                                obj2 = -1;
                                switch (obj2) {
                                    case null:
                                        break;
                                    case 1:
                                        break;
                                    default:
                                        break;
                                }
                                i = dataRole;
                                ident = 0;
                                canChangeDataRole = strArr[4].endsWith("?");
                                if (canChangeDataRole) {
                                }
                                hashCode2 = removeLastChar.hashCode();
                                if (hashCode2 != -1335157162) {
                                }
                                switch (i2) {
                                    case 0:
                                        break;
                                    case 1:
                                        break;
                                    default:
                                        break;
                                }
                                dataRole2 = dataRole;
                                try {
                                    if (this.mPortManager != null) {
                                    }
                                    ident = ident3;
                                } catch (Throwable th6) {
                                    th = th6;
                                    ident = ident2;
                                    fileDescriptor = fd;
                                    Binder.restoreCallingIdentity(ident);
                                    throw th;
                                }
                            }
                            ident = ident2;
                            long j = 0;
                            if ("disconnect-port".equals(strArr[0]) && strArr.length == 2) {
                                removeLastChar = strArr[1];
                                if (this.mPortManager != null) {
                                    this.mPortManager.disconnectSimulatedPort(removeLastChar, pw);
                                    pw.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, j);
                                }
                            } else if ("remove-port".equals(strArr[0]) && strArr.length == 2) {
                                removeLastChar = strArr[1];
                                if (this.mPortManager != null) {
                                    this.mPortManager.removeSimulatedPort(removeLastChar, pw);
                                    pw.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, j);
                                }
                            } else if ("reset".equals(strArr[0]) && strArr.length == 1) {
                                if (this.mPortManager != null) {
                                    this.mPortManager.resetSimulation(pw);
                                    pw.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, j);
                                }
                            } else if ("ports".equals(strArr[0]) && strArr.length == 1) {
                                if (this.mPortManager != null) {
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, j);
                                }
                            } else if ("dump-descriptors".equals(strArr[0])) {
                                this.mHostManager.dumpDescriptors(pw, strArr);
                            } else {
                                pw.println("Dump current USB state or issue command:");
                                pw.println("  ports");
                                pw.println("  set-port-roles <id> <source|sink|no-power> <host|device|no-data>");
                                pw.println("  add-port <id> <ufp|dfp|dual|none>");
                                pw.println("  connect-port <id> <ufp|dfp><?> <source|sink><?> <host|device><?>");
                                pw.println("    (add ? suffix if mode, power role, or data role can be changed)");
                                pw.println("  disconnect-port <id>");
                                pw.println("  remove-port <id>");
                                pw.println("  reset");
                                pw.println();
                                pw.println("Example USB type C port role switch:");
                                pw.println("  dumpsys usb set-port-roles \"default\" source device");
                                pw.println();
                                pw.println("Example USB type C port simulation with full capabilities:");
                                pw.println("  dumpsys usb add-port \"matrix\" dual");
                                pw.println("  dumpsys usb connect-port \"matrix\" ufp? sink? device?");
                                pw.println("  dumpsys usb ports");
                                pw.println("  dumpsys usb disconnect-port \"matrix\"");
                                pw.println("  dumpsys usb remove-port \"matrix\"");
                                pw.println("  dumpsys usb reset");
                                pw.println();
                                pw.println("Example USB type C port where only power role can be changed:");
                                pw.println("  dumpsys usb add-port \"matrix\" dual");
                                pw.println("  dumpsys usb connect-port \"matrix\" dfp source? host");
                                pw.println("  dumpsys usb reset");
                                pw.println();
                                pw.println("Example USB OTG port where id pin determines function:");
                                pw.println("  dumpsys usb add-port \"matrix\" dual");
                                pw.println("  dumpsys usb connect-port \"matrix\" dfp source host");
                                pw.println("  dumpsys usb reset");
                                pw.println();
                                pw.println("Example USB device-only port:");
                                pw.println("  dumpsys usb add-port \"matrix\" ufp");
                                pw.println("  dumpsys usb connect-port \"matrix\" ufp sink device");
                                pw.println("  dumpsys usb reset");
                                pw.println();
                                pw.println("Example USB device descriptors:");
                                pw.println("  dumpsys usb dump-descriptors -dump-short");
                                pw.println("  dumpsys usb dump-descriptors -dump-tree");
                                pw.println("  dumpsys usb dump-descriptors -dump-list");
                                pw.println("  dumpsys usb dump-descriptors -dump-raw");
                            }
                            fileDescriptor = fd;
                            Binder.restoreCallingIdentity(ident);
                            return;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                    }
                }
                ident = ident2;
                if (dumpAsProto2) {
                }
                if (this.mDeviceManager != null) {
                }
                if (this.mHostManager != null) {
                }
                if (this.mPortManager != null) {
                }
                this.mAlsaManager.dump(dump, "alsa_manager", 1146756268036L);
                this.mSettingsManager.dump(dump, "settings_manager", 1146756268037L);
                dump.flush();
                Binder.restoreCallingIdentity(ident);
                return;
            } catch (Throwable th8) {
                th = th8;
                fileDescriptor = fileDescriptor2;
                ident = ident2;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
        return;
        fileDescriptor = fd;
        Binder.restoreCallingIdentity(ident);
        throw th;
    }

    private static String removeLastChar(String value) {
        return value.substring(0, value.length() - 1);
    }
}
