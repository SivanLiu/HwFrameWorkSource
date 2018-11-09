package com.android.server;

import android.content.Context;
import android.content.pm.IPackageMoveObserver;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.storage.DiskInfo;
import android.os.storage.IObbActionListener;
import android.os.storage.IStorageEventListener;
import android.os.storage.IStorageShutdownObserver;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.telephony.HwTelephonyManagerInner;
import android.util.Slog;
import com.android.internal.os.AppFuseMount;
import com.android.internal.util.ArrayUtils;

public class HwStorageManagerService extends StorageManagerService {
    private static final String CREATE_USER_KEY_ISEC = "create_user_key_isec";
    private static final int DEFAULT_PRELOAD_POLICY_FLAG = -1;
    private static final String DESTROY_USER_KEY_ISEC = "destroy_user_key_isec";
    private static final String GET_KEY_DESC = "get_key_desc";
    private static final String GET_PRE_LOAD_POLICY_FLAG = "get_pre_load_policy_flag";
    private static final String LOCK_USER_KEY_ISEC = "lock_user_key_isec";
    private static final String LOCK_USER_SCREEN_ISEC = "lock_user_screen_isec";
    private static final int NATIVE_METHOD_NOT_FOUND = -201;
    private static final int NATIVE_METHOD_NOT_VALID = -200;
    private static final int SCREEN_FLAG_FACIAL_RECOGNITION = 4;
    private static final int SCREEN_FLAG_FINGERPRINT_UNLOCK = 3;
    private static final int SCREEN_FLAG_LOCK_SCREEN = 1;
    private static final int SCREEN_FLAG_PASSWORD_UNLOCK = 2;
    private static final String SET_SCREEN_STATE_FLAG = "set_screen_state_flag";
    private static final String TAG = "HwStorageManagerService";
    private static final int UNLOCK_TYPE_FACIAL_RECOGNITION = 4;
    private static final int UNLOCK_TYPE_FINGERPRINT = 2;
    private static final int UNLOCK_TYPE_PASSWORD_GRAPHICS = 1;
    private static final String UNLOCK_USER_KEY_ISEC = "unlock_user_key_isec";
    private static final String UNLOCK_USER_SCREEN_ISEC = "unlock_user_screen_isec";
    private static boolean mLoadLibraryFailed;
    private NativeDaemonConnector mCryptConnector = getCryptConnector();

    private static native void finalize_native();

    private static native void init_native();

    private static native int nativeGetMaxTimeCost();

    private static native int nativeGetMinTimeCost();

    private static native int nativeGetNotificationLevel();

    private static native int nativeGetPercentComplete();

    private static native int nativeGetUndiscardInfo();

    private static native int nativeStartClean();

    private static native int nativeStopClean();

    public /* bridge */ /* synthetic */ void addUserKeyAuth(int i, int i2, byte[] bArr, byte[] bArr2) {
        super.addUserKeyAuth(i, i2, bArr, bArr2);
    }

    public /* bridge */ /* synthetic */ void allocateBytes(String str, long j, int i, String str2) {
        super.allocateBytes(str, j, i, str2);
    }

    public /* bridge */ /* synthetic */ long benchmark(String str) {
        return super.benchmark(str);
    }

    public /* bridge */ /* synthetic */ int changeEncryptionPassword(int i, String str) {
        return super.changeEncryptionPassword(i, str);
    }

    public /* bridge */ /* synthetic */ void clearPassword() {
        super.clearPassword();
    }

    public /* bridge */ /* synthetic */ int createSecureContainer(String str, int i, String str2, String str3, int i2, boolean z) {
        return super.createSecureContainer(str, i, str2, str3, i2, z);
    }

    public /* bridge */ /* synthetic */ void createUserKey(int i, int i2, boolean z) {
        super.createUserKey(i, i2, z);
    }

    public /* bridge */ /* synthetic */ int decryptStorage(String str) {
        return super.decryptStorage(str);
    }

    public /* bridge */ /* synthetic */ int destroySecureContainer(String str, boolean z) {
        return super.destroySecureContainer(str, z);
    }

    public /* bridge */ /* synthetic */ void destroyUserKey(int i) {
        super.destroyUserKey(i);
    }

    public /* bridge */ /* synthetic */ void destroyUserStorage(String str, int i, int i2) {
        super.destroyUserStorage(str, i, i2);
    }

    public /* bridge */ /* synthetic */ int encryptStorage(int i, String str) {
        return super.encryptStorage(i, str);
    }

    public /* bridge */ /* synthetic */ int finalizeSecureContainer(String str) {
        return super.finalizeSecureContainer(str);
    }

    public /* bridge */ /* synthetic */ void finishMediaUpdate() {
        super.finishMediaUpdate();
    }

    public /* bridge */ /* synthetic */ int fixPermissionsSecureContainer(String str, int i, String str2) {
        return super.fixPermissionsSecureContainer(str, i, str2);
    }

    public /* bridge */ /* synthetic */ void fixateNewestUserKeyAuth(int i) {
        super.fixateNewestUserKeyAuth(i);
    }

    public /* bridge */ /* synthetic */ void forgetAllVolumes() {
        super.forgetAllVolumes();
    }

    public /* bridge */ /* synthetic */ void forgetVolume(String str) {
        super.forgetVolume(str);
    }

    public /* bridge */ /* synthetic */ void format(String str) {
        super.format(str);
    }

    public /* bridge */ /* synthetic */ int formatVolume(String str) {
        return super.formatVolume(str);
    }

    public /* bridge */ /* synthetic */ void fstrim(int i) {
        super.fstrim(i);
    }

    public /* bridge */ /* synthetic */ long getAllocatableBytes(String str, int i, String str2) {
        return super.getAllocatableBytes(str, i, str2);
    }

    public /* bridge */ /* synthetic */ long getCacheQuotaBytes(String str, int i) {
        return super.getCacheQuotaBytes(str, i);
    }

    public /* bridge */ /* synthetic */ long getCacheSizeBytes(String str, int i) {
        return super.getCacheSizeBytes(str, i);
    }

    public /* bridge */ /* synthetic */ NativeDaemonConnector getCryptConnector() {
        return super.getCryptConnector();
    }

    public /* bridge */ /* synthetic */ DiskInfo[] getDisks() {
        return super.getDisks();
    }

    public /* bridge */ /* synthetic */ int getEncryptionState() {
        return super.getEncryptionState();
    }

    public /* bridge */ /* synthetic */ String getField(String str) {
        return super.getField(str);
    }

    public /* bridge */ /* synthetic */ String getMountedObbPath(String str) {
        return super.getMountedObbPath(str);
    }

    public /* bridge */ /* synthetic */ String getPassword() {
        return super.getPassword();
    }

    public /* bridge */ /* synthetic */ int getPasswordType() {
        return super.getPasswordType();
    }

    public /* bridge */ /* synthetic */ String getPrimaryStorageUuid() {
        return super.getPrimaryStorageUuid();
    }

    public /* bridge */ /* synthetic */ int getPrivacySpaceUserId() {
        return super.getPrivacySpaceUserId();
    }

    public /* bridge */ /* synthetic */ String getSecureContainerFilesystemPath(String str) {
        return super.getSecureContainerFilesystemPath(str);
    }

    public /* bridge */ /* synthetic */ String[] getSecureContainerList() {
        return super.getSecureContainerList();
    }

    public /* bridge */ /* synthetic */ String getSecureContainerPath(String str) {
        return super.getSecureContainerPath(str);
    }

    public /* bridge */ /* synthetic */ int[] getStorageUsers(String str) {
        return super.getStorageUsers(str);
    }

    public /* bridge */ /* synthetic */ StorageVolume[] getVolumeList(int i, String str, int i2) {
        return super.getVolumeList(i, str, i2);
    }

    public /* bridge */ /* synthetic */ VolumeRecord[] getVolumeRecords(int i) {
        return super.getVolumeRecords(i);
    }

    public /* bridge */ /* synthetic */ String getVolumeState(String str) {
        return super.getVolumeState(str);
    }

    public /* bridge */ /* synthetic */ VolumeInfo[] getVolumes(int i) {
        return super.getVolumes(i);
    }

    public /* bridge */ /* synthetic */ boolean isConvertibleToFBE() {
        return super.isConvertibleToFBE();
    }

    public /* bridge */ /* synthetic */ boolean isExternalSDcard(VolumeInfo volumeInfo) {
        return super.isExternalSDcard(volumeInfo);
    }

    public /* bridge */ /* synthetic */ boolean isExternalStorageEmulated() {
        return super.isExternalStorageEmulated();
    }

    public /* bridge */ /* synthetic */ boolean isObbMounted(String str) {
        return super.isObbMounted(str);
    }

    public /* bridge */ /* synthetic */ boolean isSecure() {
        return super.isSecure();
    }

    public /* bridge */ /* synthetic */ boolean isSecureContainerMounted(String str) {
        return super.isSecureContainerMounted(str);
    }

    public /* bridge */ /* synthetic */ boolean isSecureEx(int i) {
        return super.isSecureEx(i);
    }

    public /* bridge */ /* synthetic */ boolean isUsbMassStorageConnected() {
        return super.isUsbMassStorageConnected();
    }

    public /* bridge */ /* synthetic */ boolean isUsbMassStorageEnabled() {
        return super.isUsbMassStorageEnabled();
    }

    public /* bridge */ /* synthetic */ boolean isUserKeyUnlocked(int i) {
        return super.isUserKeyUnlocked(i);
    }

    public /* bridge */ /* synthetic */ long lastMaintenance() {
        return super.lastMaintenance();
    }

    public /* bridge */ /* synthetic */ void lockUserKey(int i) {
        super.lockUserKey(i);
    }

    public /* bridge */ /* synthetic */ int mkdirs(String str, String str2) {
        return super.mkdirs(str, str2);
    }

    public /* bridge */ /* synthetic */ void monitor() {
        super.monitor();
    }

    public /* bridge */ /* synthetic */ void mount(String str) {
        super.mount(str);
    }

    public /* bridge */ /* synthetic */ void mountObb(String str, String str2, String str3, IObbActionListener iObbActionListener, int i) {
        super.mountObb(str, str2, str3, iObbActionListener, i);
    }

    public /* bridge */ /* synthetic */ AppFuseMount mountProxyFileDescriptorBridge() {
        return super.mountProxyFileDescriptorBridge();
    }

    public /* bridge */ /* synthetic */ int mountSecureContainer(String str, String str2, int i, boolean z) {
        return super.mountSecureContainer(str, str2, i, z);
    }

    public /* bridge */ /* synthetic */ int mountVolume(String str) {
        return super.mountVolume(str);
    }

    public /* bridge */ /* synthetic */ boolean onCheckHoldWakeLock(int i) {
        return super.onCheckHoldWakeLock(i);
    }

    public /* bridge */ /* synthetic */ void onDaemonConnected() {
        super.onDaemonConnected();
    }

    public /* bridge */ /* synthetic */ boolean onEvent(int i, String str, String[] strArr) {
        return super.onEvent(i, str, strArr);
    }

    public /* bridge */ /* synthetic */ ParcelFileDescriptor openProxyFileDescriptor(int i, int i2, int i3) {
        return super.openProxyFileDescriptor(i, i2, i3);
    }

    public /* bridge */ /* synthetic */ void partitionMixed(String str, int i) {
        super.partitionMixed(str, i);
    }

    public /* bridge */ /* synthetic */ void partitionPrivate(String str) {
        super.partitionPrivate(str);
    }

    public /* bridge */ /* synthetic */ void partitionPublic(String str) {
        super.partitionPublic(str);
    }

    public /* bridge */ /* synthetic */ void prepareUserStorage(String str, int i, int i2, int i3) {
        super.prepareUserStorage(str, i, i2, i3);
    }

    public /* bridge */ /* synthetic */ void registerListener(IStorageEventListener iStorageEventListener) {
        super.registerListener(iStorageEventListener);
    }

    public /* bridge */ /* synthetic */ int renameSecureContainer(String str, String str2) {
        return super.renameSecureContainer(str, str2);
    }

    public /* bridge */ /* synthetic */ int resizeSecureContainer(String str, int i, String str2) {
        return super.resizeSecureContainer(str, i, str2);
    }

    public /* bridge */ /* synthetic */ void runMaintenance() {
        super.runMaintenance();
    }

    public /* bridge */ /* synthetic */ void secdiscard(String str) {
        super.secdiscard(str);
    }

    public /* bridge */ /* synthetic */ void setDebugFlags(int i, int i2) {
        super.setDebugFlags(i, i2);
    }

    public /* bridge */ /* synthetic */ void setField(String str, String str2) {
        super.setField(str, str2);
    }

    public /* bridge */ /* synthetic */ void setPrimaryStorageUuid(String str, IPackageMoveObserver iPackageMoveObserver) {
        super.setPrimaryStorageUuid(str, iPackageMoveObserver);
    }

    public /* bridge */ /* synthetic */ void setUsbMassStorageEnabled(boolean z) {
        super.setUsbMassStorageEnabled(z);
    }

    public /* bridge */ /* synthetic */ void setVolumeNickname(String str, String str2) {
        super.setVolumeNickname(str, str2);
    }

    public /* bridge */ /* synthetic */ void setVolumeUserFlags(String str, int i, int i2) {
        super.setVolumeUserFlags(str, i, i2);
    }

    public /* bridge */ /* synthetic */ void shutdown(IStorageShutdownObserver iStorageShutdownObserver) {
        super.shutdown(iStorageShutdownObserver);
    }

    public /* bridge */ /* synthetic */ void unlockUserKey(int i, int i2, byte[] bArr, byte[] bArr2) {
        super.unlockUserKey(i, i2, bArr, bArr2);
    }

    public /* bridge */ /* synthetic */ void unmount(String str) {
        super.unmount(str);
    }

    public /* bridge */ /* synthetic */ void unmountObb(String str, boolean z, IObbActionListener iObbActionListener, int i) {
        super.unmountObb(str, z, iObbActionListener, i);
    }

    public /* bridge */ /* synthetic */ int unmountSecureContainer(String str, boolean z) {
        return super.unmountSecureContainer(str, z);
    }

    public /* bridge */ /* synthetic */ void unmountVolume(String str, boolean z, boolean z2) {
        super.unmountVolume(str, z, z2);
    }

    public /* bridge */ /* synthetic */ void unregisterListener(IStorageEventListener iStorageEventListener) {
        super.unregisterListener(iStorageEventListener);
    }

    public /* bridge */ /* synthetic */ int verifyEncryptionPassword(String str) {
        return super.verifyEncryptionPassword(str);
    }

    public /* bridge */ /* synthetic */ void waitForAsecScan() {
        super.waitForAsecScan();
    }

    static {
        mLoadLibraryFailed = false;
        try {
            System.loadLibrary("hwstoragemanager_jni");
        } catch (UnsatisfiedLinkError e) {
            mLoadLibraryFailed = true;
            Slog.d(TAG, "hwstoragemanager_jni library not found!");
        }
    }

    public HwStorageManagerService(Context context) {
        super(context);
        if (!mLoadLibraryFailed) {
            init_native();
        }
    }

    protected void finalize() {
        if (!mLoadLibraryFailed) {
            finalize_native();
        }
        try {
            super.finalize();
        } catch (Throwable th) {
        }
    }

    public int startClean() {
        Slog.d(TAG, "startClean:");
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("you have no permission to call startClean from uid:" + Binder.getCallingUid());
        }
        try {
            if (!mLoadLibraryFailed) {
                return nativeStartClean();
            }
            Slog.d(TAG, "nativeStartClean not valid!");
            return NATIVE_METHOD_NOT_VALID;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeStartClean not found!");
            return NATIVE_METHOD_NOT_FOUND;
        }
    }

    public int stopClean() {
        Slog.d(TAG, "stopClean:");
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("you have no permission to call stopClean from uid:" + Binder.getCallingUid());
        }
        try {
            if (!mLoadLibraryFailed) {
                return nativeStopClean();
            }
            Slog.d(TAG, "nativeStopClean not valid!");
            return NATIVE_METHOD_NOT_VALID;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeStopClean not found!");
            return NATIVE_METHOD_NOT_FOUND;
        }
    }

    public int getNotificationLevel() {
        Slog.d(TAG, "getNotificationLevel:");
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("you have no permission to call getNotificationLevel from uid:" + Binder.getCallingUid());
        }
        try {
            if (!mLoadLibraryFailed) {
                return nativeGetNotificationLevel();
            }
            Slog.d(TAG, "nativeGetNotificationLevel not valid!");
            return NATIVE_METHOD_NOT_VALID;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeGetNotificationLevel not found!");
            return NATIVE_METHOD_NOT_FOUND;
        }
    }

    public int getUndiscardInfo() {
        Slog.d(TAG, "getUndiscardInfo:");
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("you have no permission to call getUndiscardInfo from uid:" + Binder.getCallingUid());
        }
        try {
            if (!mLoadLibraryFailed) {
                return nativeGetUndiscardInfo();
            }
            Slog.d(TAG, "nativeGetUndiscardInfo not valid!");
            return NATIVE_METHOD_NOT_VALID;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeGetUndiscardInfo not found!");
            return NATIVE_METHOD_NOT_FOUND;
        }
    }

    public int getMaxTimeCost() {
        Slog.d(TAG, "getMaxTimeCost:");
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("you have no permission to call getMaxTimeCost from uid:" + Binder.getCallingUid());
        }
        try {
            if (!mLoadLibraryFailed) {
                return nativeGetMaxTimeCost();
            }
            Slog.d(TAG, "nativeGetMaxTimeCost not valid!");
            return NATIVE_METHOD_NOT_VALID;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeGetMaxTimeCost not found!");
            return NATIVE_METHOD_NOT_FOUND;
        }
    }

    public int getMinTimeCost() {
        Slog.d(TAG, "getMinTimeCost:");
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("you have no permission to call getMinTimeCost from uid:" + Binder.getCallingUid());
        }
        try {
            if (!mLoadLibraryFailed) {
                return nativeGetMinTimeCost();
            }
            Slog.d(TAG, "nativeGetMinTimeCost not valid!");
            return NATIVE_METHOD_NOT_VALID;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeGetMinTimeCost not found!");
            return NATIVE_METHOD_NOT_FOUND;
        }
    }

    public int getPercentComplete() {
        Slog.d(TAG, "getPercentComplete:");
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("you have no permission to call getPercentComplete from uid:" + Binder.getCallingUid());
        }
        try {
            if (!mLoadLibraryFailed) {
                return nativeGetPercentComplete();
            }
            Slog.d(TAG, "nativeGetPercentComplete not valid!");
            return NATIVE_METHOD_NOT_VALID;
        } catch (UnsatisfiedLinkError e) {
            Slog.d(TAG, "nativeGetPercentComplete not found!");
            return NATIVE_METHOD_NOT_FOUND;
        }
    }

    public void unlockUserKeyISec(int userId, int serialNumber, byte[] token, byte[] secret) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            if (StorageManagerService.sSelf.isSecureEx(userId) && ArrayUtils.isEmpty(secret)) {
                throw new IllegalStateException("Token required to unlock secure user " + userId);
            }
            try {
                this.mCryptConnector.execute("cryptfs", new Object[]{UNLOCK_USER_KEY_ISEC, Integer.valueOf(userId), Integer.valueOf(serialNumber), encodeBytes(token), encodeBytes(secret)});
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
        synchronized (this.mLock) {
            this.mLocalUnlockedUsers = ArrayUtils.appendInt(this.mLocalUnlockedUsers, userId);
        }
    }

    public void lockUserKeyISec(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", new Object[]{LOCK_USER_KEY_ISEC, Integer.valueOf(userId)});
            synchronized (this.mLock) {
                this.mLocalUnlockedUsers = ArrayUtils.removeInt(this.mLocalUnlockedUsers, userId);
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void unlockUserScreenISec(int userId, int serialNumber, byte[] token, byte[] secret, int type) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        if (!StorageManager.isFileEncryptedNativeOrEmulated()) {
            return;
        }
        if (type == 1 || type == 2 || type == 4) {
            try {
                this.mCryptConnector.execute("cryptfs", new Object[]{UNLOCK_USER_SCREEN_ISEC, Integer.valueOf(userId), Integer.valueOf(serialNumber), encodeBytes(token), encodeBytes(secret), Integer.valueOf(type)});
                return;
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
        Slog.e(TAG, "unlockUserScreenISec error, wrong type");
    }

    public void lockUserScreenISec(int userId, int serialNumber) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", new Object[]{LOCK_USER_SCREEN_ISEC, Integer.valueOf(userId), Integer.valueOf(serialNumber)});
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "lockUserScreenISec failed");
        }
    }

    public int getPreLoadPolicyFlag(int userId, int serialNumber) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            try {
                return Integer.parseInt(this.mCryptConnector.execute("cryptfs", new Object[]{GET_PRE_LOAD_POLICY_FLAG, Integer.valueOf(userId), Integer.valueOf(serialNumber)}).getMessage());
            } catch (NumberFormatException e) {
                Slog.e(TAG, "getPreLoadPolicyFlag failed NumberFormatException");
            } catch (NativeDaemonConnectorException e2) {
                Slog.e(TAG, "getPreLoadPolicyFlag failed NativeDaemonConnectorException");
            }
        }
        return -1;
    }

    public boolean setScreenStateFlag(int userId, int serialNumber, int flag) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            if (flag == 1 || flag == 2 || flag == 3 || flag == 4) {
                try {
                    this.mCryptConnector.execute("cryptfs", new Object[]{SET_SCREEN_STATE_FLAG, Integer.valueOf(userId), Integer.valueOf(serialNumber), Integer.valueOf(flag)});
                    return true;
                } catch (NativeDaemonConnectorException e) {
                    Slog.e(TAG, "setScreenStateFlag failed NativeDaemonConnectorException");
                }
            } else {
                Slog.e(TAG, "setScreenStateFlag failed wrong flag input");
            }
        }
        return false;
    }

    public String getKeyDesc(int userId, int serialNumber, int sdpClass) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            try {
                return this.mCryptConnector.execute("cryptfs", new Object[]{GET_KEY_DESC, Integer.valueOf(userId), Integer.valueOf(serialNumber), Integer.valueOf(sdpClass)}).getMessage();
            } catch (NativeDaemonConnectorException e) {
                Slog.e(TAG, "getKeyDesc failed");
            }
        }
        return null;
    }

    public void createUserKeyISec(int userId, int serialNumber, boolean ephemeral) {
        int i = 1;
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mCryptConnector;
            String str = "cryptfs";
            Object[] objArr = new Object[4];
            objArr[0] = CREATE_USER_KEY_ISEC;
            objArr[1] = Integer.valueOf(userId);
            objArr[2] = Integer.valueOf(serialNumber);
            if (!ephemeral) {
                i = 0;
            }
            objArr[3] = Integer.valueOf(i);
            nativeDaemonConnector.execute(str, objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void destroyUserKeyISec(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", new Object[]{DESTROY_USER_KEY_ISEC, Integer.valueOf(userId)});
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void notifyDeviceStateToTelephony(String device, String state, String extras) {
        HwTelephonyManagerInner.getDefault().notifyDeviceState(device, state, extras);
    }
}
