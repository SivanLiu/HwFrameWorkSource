package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.pm.Installer;
import com.android.server.pm.Installer.InstallerException;
import java.io.File;

class IdmapManager {
    private final Installer mInstaller;

    IdmapManager(Installer installer) {
        this.mInstaller = installer;
    }

    boolean createIdmap(PackageInfo targetPackage, PackageInfo overlayPackage, int userId) {
        int sharedGid = UserHandle.getSharedAppGid(targetPackage.applicationInfo.uid);
        String targetPath = targetPackage.applicationInfo.getBaseCodePath();
        String overlayPath = overlayPackage.applicationInfo.getBaseCodePath();
        try {
            this.mInstaller.idmap(targetPath, overlayPath, sharedGid);
            return true;
        } catch (InstallerException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("failed to generate idmap for ");
            stringBuilder.append(targetPath);
            stringBuilder.append(" and ");
            stringBuilder.append(overlayPath);
            stringBuilder.append(": ");
            stringBuilder.append(e.getMessage());
            Slog.w("OverlayManager", stringBuilder.toString());
            return false;
        }
    }

    boolean removeIdmap(OverlayInfo oi, int userId) {
        try {
            this.mInstaller.removeIdmap(oi.baseCodePath);
            return true;
        } catch (InstallerException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("failed to remove idmap for ");
            stringBuilder.append(oi.baseCodePath);
            stringBuilder.append(": ");
            stringBuilder.append(e.getMessage());
            Slog.w("OverlayManager", stringBuilder.toString());
            return false;
        }
    }

    boolean idmapExists(OverlayInfo oi) {
        return new File(getIdmapPath(oi.baseCodePath)).isFile();
    }

    boolean idmapExists(PackageInfo overlayPackage, int userId) {
        return new File(getIdmapPath(overlayPackage.applicationInfo.getBaseCodePath())).isFile();
    }

    private String getIdmapPath(String baseCodePath) {
        StringBuilder sb = new StringBuilder("/data/resource-cache/");
        sb.append(baseCodePath.substring(1).replace('/', '@'));
        sb.append("@idmap");
        return sb.toString();
    }
}
