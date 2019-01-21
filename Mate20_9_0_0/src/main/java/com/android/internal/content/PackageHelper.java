package com.android.internal.content;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.dex.DexMetadataHelper;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IStorageManager;
import android.os.storage.IStorageManager.Stub;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.provider.Settings.Global;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.NativeLibraryHelper.Handle;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Objects;
import libcore.io.IoUtils;

public class PackageHelper {
    public static final int APP_INSTALL_AUTO = 0;
    public static final int APP_INSTALL_EXTERNAL = 2;
    public static final int APP_INSTALL_INTERNAL = 1;
    public static final int RECOMMEND_FAILED_ALREADY_EXISTS = -4;
    public static final int RECOMMEND_FAILED_INSUFFICIENT_STORAGE = -1;
    public static final int RECOMMEND_FAILED_INVALID_APK = -2;
    public static final int RECOMMEND_FAILED_INVALID_LOCATION = -3;
    public static final int RECOMMEND_FAILED_INVALID_URI = -6;
    public static final int RECOMMEND_FAILED_VERSION_DOWNGRADE = -7;
    public static final int RECOMMEND_INSTALL_EPHEMERAL = 3;
    public static final int RECOMMEND_INSTALL_EXTERNAL = 2;
    public static final int RECOMMEND_INSTALL_INTERNAL = 1;
    public static final int RECOMMEND_MEDIA_UNAVAILABLE = -5;
    private static final String TAG = "PackageHelper";
    private static HwCustPackageHelper mCustPkgHelper = ((HwCustPackageHelper) HwCustUtils.createObj(HwCustPackageHelper.class, new Object[0]));
    private static TestableInterface sDefaultTestableInterface = null;

    public static abstract class TestableInterface {
        public abstract boolean getAllow3rdPartyOnInternalConfig(Context context);

        public abstract File getDataDirectory();

        public abstract ApplicationInfo getExistingAppInfo(Context context, String str);

        public abstract boolean getForceAllowOnExternalSetting(Context context);

        public abstract StorageManager getStorageManager(Context context);
    }

    public static IStorageManager getStorageManager() throws RemoteException {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return Stub.asInterface(service);
        }
        Log.e(TAG, "Can't get storagemanager service");
        throw new RemoteException("Could not contact storagemanager service");
    }

    private static synchronized TestableInterface getDefaultTestableInterface() {
        TestableInterface testableInterface;
        synchronized (PackageHelper.class) {
            if (sDefaultTestableInterface == null) {
                sDefaultTestableInterface = new TestableInterface() {
                    public StorageManager getStorageManager(Context context) {
                        return (StorageManager) context.getSystemService(StorageManager.class);
                    }

                    public boolean getForceAllowOnExternalSetting(Context context) {
                        return Global.getInt(context.getContentResolver(), "force_allow_on_external", 0) != 0;
                    }

                    public boolean getAllow3rdPartyOnInternalConfig(Context context) {
                        return context.getResources().getBoolean(17956869);
                    }

                    public ApplicationInfo getExistingAppInfo(Context context, String packageName) {
                        try {
                            return context.getPackageManager().getApplicationInfo(packageName, 4194304);
                        } catch (NameNotFoundException e) {
                            Log.e(PackageHelper.TAG, "ApplicationInfo is not found in function getExistingAppInfo");
                            return null;
                        }
                    }

                    public File getDataDirectory() {
                        return Environment.getDataDirectory();
                    }
                };
            }
            testableInterface = sDefaultTestableInterface;
        }
        return testableInterface;
    }

    @VisibleForTesting
    @Deprecated
    public static String resolveInstallVolume(Context context, String packageName, int installLocation, long sizeBytes, TestableInterface testInterface) throws IOException {
        SessionParams params = new SessionParams(-1);
        params.appPackageName = packageName;
        params.installLocation = installLocation;
        params.sizeBytes = sizeBytes;
        return resolveInstallVolume(context, params, testInterface);
    }

    public static String resolveInstallVolume(Context context, SessionParams params) throws IOException {
        return resolveInstallVolume(context, params.appPackageName, params.installLocation, params.sizeBytes, getDefaultTestableInterface());
    }

    @VisibleForTesting
    public static String resolveInstallVolume(Context context, SessionParams params, TestableInterface testInterface) throws IOException {
        Context context2 = context;
        SessionParams sessionParams = params;
        TestableInterface testableInterface = testInterface;
        StorageManager storageManager = testableInterface.getStorageManager(context2);
        boolean forceAllowOnExternal = testableInterface.getForceAllowOnExternalSetting(context2);
        boolean allow3rdPartyOnInternal = testableInterface.getAllow3rdPartyOnInternalConfig(context2);
        ApplicationInfo existingInfo = testableInterface.getExistingAppInfo(context2, sessionParams.appPackageName);
        ArraySet<String> allCandidates = new ArraySet();
        boolean fitsOnInternal = false;
        VolumeInfo bestCandidate = null;
        long bestCandidateAvailBytes = Long.MIN_VALUE;
        for (VolumeInfo vol : storageManager.getVolumes()) {
            StorageManager storageManager2;
            if (vol.type == 1 && vol.isMountedWritable()) {
                boolean isInternalStorage = "private".equals(vol.id);
                long availBytes = storageManager.getAllocatableBytes(storageManager.getUuidForPath(new File(vol.path)), translateAllocateFlags(sessionParams.installFlags));
                if (isInternalStorage) {
                    storageManager2 = storageManager;
                    fitsOnInternal = sessionParams.sizeBytes <= availBytes;
                } else {
                    storageManager2 = storageManager;
                }
                if (!isInternalStorage || allow3rdPartyOnInternal) {
                    if (availBytes >= sessionParams.sizeBytes) {
                        allCandidates.add(vol.fsUuid);
                    }
                    if (availBytes >= bestCandidateAvailBytes) {
                        bestCandidateAvailBytes = availBytes;
                        bestCandidate = vol;
                    }
                }
            } else {
                storageManager2 = storageManager;
            }
            storageManager = storageManager2;
            context2 = context;
            testableInterface = testInterface;
        }
        StringBuilder stringBuilder;
        if (existingInfo == null || !existingInfo.isSystemApp()) {
            if (forceAllowOnExternal || sessionParams.installLocation != 1) {
                if (existingInfo != null) {
                    if (Objects.equals(existingInfo.volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL) && fitsOnInternal) {
                        return StorageManager.UUID_PRIVATE_INTERNAL;
                    }
                    if (allCandidates.contains(existingInfo.volumeUuid)) {
                        return existingInfo.volumeUuid;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Not enough space on existing volume ");
                    stringBuilder.append(existingInfo.volumeUuid);
                    stringBuilder.append(" for ");
                    stringBuilder.append(sessionParams.appPackageName);
                    stringBuilder.append(" upgrade");
                    throw new IOException(stringBuilder.toString());
                } else if (bestCandidate != null) {
                    return bestCandidate.fsUuid;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No special requests, but no room on allowed volumes.  allow3rdPartyOnInternal? ");
                    stringBuilder.append(allow3rdPartyOnInternal);
                    throw new IOException(stringBuilder.toString());
                }
            } else if (existingInfo != null && !Objects.equals(existingInfo.volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot automatically move ");
                stringBuilder.append(sessionParams.appPackageName);
                stringBuilder.append(" from ");
                stringBuilder.append(existingInfo.volumeUuid);
                stringBuilder.append(" to internal storage");
                throw new IOException(stringBuilder.toString());
            } else if (!allow3rdPartyOnInternal) {
                throw new IOException("Not allowed to install non-system apps on internal storage");
            } else if (fitsOnInternal) {
                return StorageManager.UUID_PRIVATE_INTERNAL;
            } else {
                throw new IOException("Requested internal only, but not enough space");
            }
        } else if (fitsOnInternal) {
            return StorageManager.UUID_PRIVATE_INTERNAL;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not enough space on existing volume ");
            stringBuilder.append(existingInfo.volumeUuid);
            stringBuilder.append(" for system app ");
            stringBuilder.append(sessionParams.appPackageName);
            stringBuilder.append(" upgrade");
            throw new IOException(stringBuilder.toString());
        }
    }

    public static boolean fitsOnInternal(Context context, SessionParams params) throws IOException {
        StorageManager storage = (StorageManager) context.getSystemService(StorageManager.class);
        return params.sizeBytes <= storage.getAllocatableBytes(storage.getUuidForPath(Environment.getDataDirectory()), translateAllocateFlags(params.installFlags));
    }

    public static boolean fitsOnExternal(Context context, SessionParams params) {
        StorageManager storage = (StorageManager) context.getSystemService(StorageManager.class);
        StorageVolume primary = storage.getPrimaryVolume();
        return params.sizeBytes > 0 && !primary.isEmulated() && "mounted".equals(primary.getState()) && params.sizeBytes <= storage.getStorageBytesUntilLow(primary.getPathFile());
    }

    @Deprecated
    public static int resolveInstallLocation(Context context, String packageName, int installLocation, long sizeBytes, int installFlags) {
        SessionParams params = new SessionParams(-1);
        params.appPackageName = packageName;
        params.installLocation = installLocation;
        params.sizeBytes = sizeBytes;
        params.installFlags = installFlags;
        try {
            return resolveInstallLocation(context, params);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int resolveInstallLocation(Context context, SessionParams params) throws IOException {
        int prefer;
        boolean checkBoth;
        ApplicationInfo existingInfo = null;
        try {
            existingInfo = context.getPackageManager().getApplicationInfo(params.appPackageName, 4194304);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "ApplicationInfo is not found in function resolveInstallLocation");
        }
        boolean ephemeral = false;
        int i = 1;
        if ((params.installFlags & 2048) != 0) {
            prefer = 1;
            ephemeral = true;
            checkBoth = false;
        } else if ((params.installFlags & 16) != 0) {
            prefer = 1;
            checkBoth = false;
        } else if ((params.installFlags & 8) != 0) {
            prefer = 2;
            checkBoth = false;
        } else if (params.installLocation == 1) {
            prefer = 1;
            checkBoth = false;
        } else if (params.installLocation == 2) {
            prefer = 2;
            checkBoth = true;
        } else if (params.installLocation == 0) {
            if (existingInfo == null) {
                prefer = 1;
            } else if ((existingInfo.flags & 262144) != 0) {
                prefer = 2;
            } else {
                prefer = 1;
            }
            checkBoth = true;
        } else {
            prefer = 1;
            checkBoth = false;
        }
        boolean fitsOnInternal = false;
        if (checkBoth || prefer == 1) {
            fitsOnInternal = fitsOnInternal(context, params);
        }
        boolean fitsOnExternal = false;
        if (checkBoth || prefer == 2) {
            fitsOnExternal = fitsOnExternal(context, params);
        }
        if (prefer == 1) {
            if (fitsOnInternal) {
                if (ephemeral) {
                    i = 3;
                }
                return i;
            }
        } else if (prefer == 2 && fitsOnExternal) {
            return 2;
        }
        if (checkBoth) {
            if (fitsOnInternal) {
                return 1;
            }
            if (fitsOnExternal) {
                return 2;
            }
        }
        return -1;
    }

    @Deprecated
    public static long calculateInstalledSize(PackageLite pkg, boolean isForwardLocked, String abiOverride) throws IOException {
        return calculateInstalledSize(pkg, abiOverride);
    }

    public static long calculateInstalledSize(PackageLite pkg, String abiOverride) throws IOException {
        return calculateInstalledSize(pkg, abiOverride, null);
    }

    public static long calculateInstalledSize(PackageLite pkg, String abiOverride, FileDescriptor fd) throws IOException {
        Handle createFd;
        if (fd != null) {
            try {
                createFd = Handle.createFd(pkg, fd);
            } catch (Throwable th) {
                IoUtils.closeQuietly(null);
            }
        } else {
            createFd = Handle.create(pkg);
        }
        Handle handle = createFd;
        long calculateInstalledSize = calculateInstalledSize(pkg, handle, abiOverride);
        IoUtils.closeQuietly(handle);
        return calculateInstalledSize;
    }

    @Deprecated
    public static long calculateInstalledSize(PackageLite pkg, boolean isForwardLocked, Handle handle, String abiOverride) throws IOException {
        return calculateInstalledSize(pkg, handle, abiOverride);
    }

    public static long calculateInstalledSize(PackageLite pkg, Handle handle, String abiOverride) throws IOException {
        long sizeBytes = 0;
        for (String codePath : pkg.getAllCodePaths()) {
            sizeBytes += new File(codePath).length();
        }
        return (sizeBytes + DexMetadataHelper.getPackageDexMetadataSize(pkg)) + NativeLibraryHelper.sumNativeBinariesWithOverride(handle, abiOverride);
    }

    public static String replaceEnd(String str, String before, String after) {
        if (str.endsWith(before)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str.substring(0, str.length() - before.length()));
            stringBuilder.append(after);
            return stringBuilder.toString();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Expected ");
        stringBuilder2.append(str);
        stringBuilder2.append(" to end with ");
        stringBuilder2.append(before);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    public static int translateAllocateFlags(int installFlags) {
        if ((32768 & installFlags) != 0) {
            return 1;
        }
        return 0;
    }
}
