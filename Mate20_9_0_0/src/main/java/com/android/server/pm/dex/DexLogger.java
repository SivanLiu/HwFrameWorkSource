package com.android.server.pm.dex;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.ByteStringUtils;
import android.util.EventLog;
import android.util.PackageUtils;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.pm.Installer;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.dex.DexManager.Listener;
import com.android.server.pm.dex.PackageDexUsage.DexUseInfo;
import java.io.File;
import java.util.Set;

public class DexLogger implements Listener {
    private static final String DCL_SUBTAG = "dcl";
    private static final int SNET_TAG = 1397638484;
    private static final String TAG = "DexLogger";
    private final Object mInstallLock;
    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    private final IPackageManager mPackageManager;

    public static Listener getListener(IPackageManager pms, Installer installer, Object installLock) {
        return new DexLogger(pms, installer, installLock);
    }

    @VisibleForTesting
    DexLogger(IPackageManager pms, Installer installer, Object installLock) {
        this.mPackageManager = pms;
        this.mInstaller = installer;
        this.mInstallLock = installLock;
    }

    public void onReconcileSecondaryDexFile(ApplicationInfo appInfo, DexUseInfo dexUseInfo, String dexPath, int storageFlags) {
        String str;
        StringBuilder stringBuilder;
        int ownerUid = appInfo.uid;
        byte[] hash = null;
        synchronized (this.mInstallLock) {
            try {
                hash = this.mInstaller.hashSecondaryDexFile(dexPath, appInfo.packageName, ownerUid, appInfo.volumeUuid, storageFlags);
            } catch (InstallerException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Got InstallerException when hashing dex ");
                stringBuilder.append(dexPath);
                stringBuilder.append(" : ");
                stringBuilder.append(e.getMessage());
                Slog.e(str, stringBuilder.toString());
            }
        }
        if (hash != null) {
            str = PackageUtils.computeSha256Digest(new File(dexPath).getName().getBytes());
            if (hash.length == 32) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(' ');
                stringBuilder.append(ByteStringUtils.toHexString(hash));
                str = stringBuilder.toString();
            }
            writeDclEvent(ownerUid, str);
            if (dexUseInfo.isUsedByOtherApps()) {
                Set<String> otherPackages = dexUseInfo.getLoadingPackages();
                Set<Integer> otherUids = new ArraySet(otherPackages.size());
                for (String otherPackageName : otherPackages) {
                    try {
                        int otherUid = this.mPackageManager.getPackageUid(otherPackageName, 0, dexUseInfo.getOwnerUserId());
                        if (!(otherUid == -1 || otherUid == ownerUid)) {
                            otherUids.add(Integer.valueOf(otherUid));
                        }
                    } catch (RemoteException e2) {
                    }
                }
                for (Integer otherUid2 : otherUids) {
                    writeDclEvent(otherUid2.intValue(), str);
                }
            }
        }
    }

    @VisibleForTesting
    void writeDclEvent(int uid, String message) {
        EventLog.writeEvent(SNET_TAG, new Object[]{DCL_SUBTAG, Integer.valueOf(uid), message});
    }
}
