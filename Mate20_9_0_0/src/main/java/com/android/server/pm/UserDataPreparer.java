package com.android.server.pm;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IPowerManager.Stub;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings.Secure;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserDataPreparer {
    private static final int MAX_PREPARE_USER_DATA_TIMES = 2;
    private static final String REBOOT_TIMES_WHEN_PREPARE_USER_DATA_KEY = "reboot_times_when_prepare_user_data";
    private static final String TAG = "UserDataPreparer";
    private static final String XATTR_SERIAL = "user.serial";
    private final Context mContext;
    private final Object mInstallLock;
    private final Installer mInstaller;
    private final SparseBooleanArray mInvalidUserIds = new SparseBooleanArray();
    private final boolean mOnlyCore;

    UserDataPreparer(Installer installer, Object installLock, Context context, boolean onlyCore) {
        this.mInstallLock = installLock;
        this.mContext = context;
        this.mOnlyCore = onlyCore;
        this.mInstaller = installer;
    }

    void prepareUserData(int userId, int userSerial, int flags) {
        synchronized (this.mInstallLock) {
            for (VolumeInfo vol : ((StorageManager) this.mContext.getSystemService(StorageManager.class)).getWritablePrivateVolumes()) {
                prepareUserDataLI(vol.getFsUuid(), userId, userSerial, flags, true);
            }
        }
    }

    private void prepareUserDataLI(String volumeUuid, int userId, int userSerial, int flags, boolean allowRecover) {
        StringBuilder stringBuilder;
        try {
            ((StorageManager) this.mContext.getSystemService(StorageManager.class)).prepareUserStorage(volumeUuid, userId, userSerial, flags);
            if (!((flags & 1) == 0 || this.mOnlyCore)) {
                enforceSerialNumber(getDataUserDeDirectory(volumeUuid, userId), userSerial);
                if (Objects.equals(volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL)) {
                    enforceSerialNumber(getDataSystemDeDirectory(userId), userSerial);
                }
            }
            if (!((flags & 2) == 0 || this.mOnlyCore)) {
                enforceSerialNumber(getDataUserCeDirectory(volumeUuid, userId), userSerial);
                if (Objects.equals(volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL)) {
                    enforceSerialNumber(getDataSystemCeDirectory(userId), userSerial);
                }
            }
            this.mInstaller.createUserData(volumeUuid, userId, userSerial, flags);
            resetRebootTimes();
            if ((flags & 2) != 0 && userId == 0) {
                String propertyName = new StringBuilder();
                propertyName.append("sys.user.");
                propertyName.append(userId);
                propertyName.append(".ce_available");
                propertyName = propertyName.toString();
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Setting property: ");
                stringBuilder.append(propertyName);
                stringBuilder.append("=true");
                Slog.d(str, stringBuilder.toString());
                SystemProperties.set(propertyName, "true");
            }
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Destroying user ");
            stringBuilder.append(userId);
            stringBuilder.append(" on volume ");
            stringBuilder.append(volumeUuid);
            stringBuilder.append(" because we failed to prepare: ");
            stringBuilder.append(e);
            PackageManagerServiceUtils.logCriticalInfo(5, stringBuilder.toString());
            UserInfo info = UserManager.get(this.mContext).getUserInfo(userId);
            if (info == null || info.isPrimary()) {
                tryToReboot(userId);
            } else if (allowRecover) {
                prepareUserDataLI(volumeUuid, userId, userSerial, flags | 1, false);
            }
        }
    }

    void destroyUserData(int userId, int flags) {
        synchronized (this.mInstallLock) {
            for (VolumeInfo vol : ((StorageManager) this.mContext.getSystemService(StorageManager.class)).getWritablePrivateVolumes()) {
                destroyUserDataLI(vol.getFsUuid(), userId, flags);
            }
        }
    }

    void destroyUserDataLI(String volumeUuid, int userId, int flags) {
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        try {
            this.mInstaller.destroyUserData(volumeUuid, userId, flags);
            if (Objects.equals(volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL)) {
                if ((flags & 1) != 0) {
                    FileUtils.deleteContentsAndDir(getUserSystemDirectory(userId));
                    FileUtils.deleteContentsAndDir(getDataSystemDeDirectory(userId));
                }
                if ((flags & 2) != 0) {
                    FileUtils.deleteContentsAndDir(getDataSystemCeDirectory(userId));
                }
            }
            storage.destroyUserStorage(volumeUuid, userId, flags);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to destroy user ");
            stringBuilder.append(userId);
            stringBuilder.append(" on volume ");
            stringBuilder.append(volumeUuid);
            stringBuilder.append(": ");
            stringBuilder.append(e);
            PackageManagerServiceUtils.logCriticalInfo(5, stringBuilder.toString());
            addInvalidUserIds(userId);
        }
    }

    void reconcileUsers(String volumeUuid, List<UserInfo> validUsersList) {
        List<File> files = new ArrayList();
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataUserDeDirectory(volumeUuid)));
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataUserCeDirectory(volumeUuid)));
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataSystemDeDirectory()));
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataSystemCeDirectory()));
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataMiscCeDirectory()));
        reconcileUsers(volumeUuid, validUsersList, files);
    }

    @VisibleForTesting
    void reconcileUsers(String volumeUuid, List<UserInfo> validUsersList, List<File> files) {
        int userCount = validUsersList.size();
        SparseArray<UserInfo> users = new SparseArray(userCount);
        for (int i = 0; i < userCount; i++) {
            UserInfo user = (UserInfo) validUsersList.get(i);
            users.put(user.id, user);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                try {
                    int userId = Integer.parseInt(file.getName());
                    UserInfo info = (UserInfo) users.get(userId);
                    boolean destroyUser = false;
                    if (info == null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Destroying user directory ");
                        stringBuilder.append(file);
                        stringBuilder.append(" because no matching user was found");
                        PackageManagerServiceUtils.logCriticalInfo(5, stringBuilder.toString());
                        if (userId == 2147483646) {
                            Log.w(TAG, "Parentcontrol doesn't have userinfo , do not destroy this user dir!");
                        } else {
                            destroyUser = true;
                        }
                    } else if (!this.mOnlyCore) {
                        try {
                            enforceSerialNumber(file, info.serialNumber);
                        } catch (IOException e) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Destroying user directory ");
                            stringBuilder2.append(file);
                            stringBuilder2.append(" because we failed to enforce serial number: ");
                            stringBuilder2.append(e);
                            PackageManagerServiceUtils.logCriticalInfo(5, stringBuilder2.toString());
                            destroyUser = true;
                        }
                    }
                    if (destroyUser) {
                        synchronized (this.mInstallLock) {
                            destroyUserDataLI(volumeUuid, userId, 3);
                        }
                    }
                } catch (NumberFormatException e2) {
                    String str = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Invalid user directory ");
                    stringBuilder3.append(file);
                    Slog.w(str, stringBuilder3.toString());
                }
            }
        }
    }

    @VisibleForTesting
    protected File getDataMiscCeDirectory(int userId) {
        return Environment.getDataMiscCeDirectory(userId);
    }

    @VisibleForTesting
    protected File getDataSystemCeDirectory(int userId) {
        return Environment.getDataSystemCeDirectory(userId);
    }

    @VisibleForTesting
    protected File getDataMiscDeDirectory(int userId) {
        return Environment.getDataMiscDeDirectory(userId);
    }

    @VisibleForTesting
    protected File getUserSystemDirectory(int userId) {
        return Environment.getUserSystemDirectory(userId);
    }

    @VisibleForTesting
    protected File getDataUserCeDirectory(String volumeUuid, int userId) {
        return Environment.getDataUserCeDirectory(volumeUuid, userId);
    }

    @VisibleForTesting
    protected File getDataSystemDeDirectory(int userId) {
        return Environment.getDataSystemDeDirectory(userId);
    }

    @VisibleForTesting
    protected File getDataUserDeDirectory(String volumeUuid, int userId) {
        return Environment.getDataUserDeDirectory(volumeUuid, userId);
    }

    @VisibleForTesting
    protected boolean isFileEncryptedEmulatedOnly() {
        return StorageManager.isFileEncryptedEmulatedOnly();
    }

    void enforceSerialNumber(File file, int serialNumber) throws IOException {
        if (isFileEncryptedEmulatedOnly()) {
            Slog.w(TAG, "Device is emulating FBE; assuming current serial number is valid");
            return;
        }
        int foundSerial = getSerialNumber(file);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Found ");
        stringBuilder.append(file);
        stringBuilder.append(" with serial number ");
        stringBuilder.append(foundSerial);
        Slog.v(str, stringBuilder.toString());
        if (foundSerial == -1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Serial number missing on ");
            stringBuilder.append(file);
            stringBuilder.append("; assuming current is valid");
            Slog.d(str, stringBuilder.toString());
            try {
                setSerialNumber(file, serialNumber);
            } catch (IOException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to set serial number on ");
                stringBuilder2.append(file);
                Slog.w(str2, stringBuilder2.toString(), e);
            }
        } else if (foundSerial != serialNumber) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Found serial number ");
            stringBuilder.append(foundSerial);
            stringBuilder.append(" doesn't match expected ");
            stringBuilder.append(serialNumber);
            throw new IOException(stringBuilder.toString());
        }
    }

    private static void setSerialNumber(File file, int serialNumber) throws IOException {
        try {
            Os.setxattr(file.getAbsolutePath(), XATTR_SERIAL, Integer.toString(serialNumber).getBytes(StandardCharsets.UTF_8), OsConstants.XATTR_CREATE);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    @VisibleForTesting
    static int getSerialNumber(File file) throws IOException {
        String serial;
        try {
            serial = new String(Os.getxattr(file.getAbsolutePath(), XATTR_SERIAL));
            return Integer.parseInt(serial);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad serial number: ");
            stringBuilder.append(serial);
            throw new IOException(stringBuilder.toString());
        } catch (ErrnoException e2) {
            if (e2.errno == OsConstants.ENODATA) {
                return -1;
            }
            throw e2.rethrowAsIOException();
        }
    }

    private boolean checkWhetherToReboot() {
        int times = Secure.getInt(this.mContext.getContentResolver(), REBOOT_TIMES_WHEN_PREPARE_USER_DATA_KEY, 0);
        String str;
        if (times < 2) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("check result is to go to reboot, times:");
            stringBuilder.append(times);
            Slog.i(str, stringBuilder.toString());
            Secure.putInt(this.mContext.getContentResolver(), REBOOT_TIMES_WHEN_PREPARE_USER_DATA_KEY, times + 1);
            return true;
        }
        str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("check result is to go to eRecovery, times:");
        stringBuilder2.append(times);
        Slog.i(str, stringBuilder2.toString());
        return false;
    }

    protected void tryToReboot(int userId) {
        StringBuilder stringBuilder;
        try {
            if (checkWhetherToReboot()) {
                File dataDirectory = Environment.getDataDirectory();
                stringBuilder = new StringBuilder();
                stringBuilder.append("system");
                stringBuilder.append(File.separator);
                stringBuilder.append(SoundModelContract.KEY_USERS);
                File usersDir = new File(dataDirectory, stringBuilder.toString());
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(userId);
                stringBuilder2.append(".xml");
                new AtomicFile(new File(usersDir, stringBuilder2.toString())).delete();
                Thread.sleep(1000);
                Stub.asInterface(ServiceManager.getService("power")).reboot(false, "prepare user data failed! try to reboot...", false);
                return;
            }
            SystemProperties.set("sys.userstorage_block", "1");
        } catch (Exception e) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("try to reboot error, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            SystemProperties.set("sys.userstorage_block", "1");
        }
    }

    protected void resetRebootTimes() {
        Secure.putInt(this.mContext.getContentResolver(), REBOOT_TIMES_WHEN_PREPARE_USER_DATA_KEY, 0);
    }

    private void addInvalidUserIds(int userId) {
        synchronized (this.mInvalidUserIds) {
            this.mInvalidUserIds.put(userId, true);
        }
    }

    boolean isUserIdInvalid(int userId) {
        boolean z;
        synchronized (this.mInvalidUserIds) {
            z = this.mInvalidUserIds.get(userId);
        }
        return z;
    }
}
