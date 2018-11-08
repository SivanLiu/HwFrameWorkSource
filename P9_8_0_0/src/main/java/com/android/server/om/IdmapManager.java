package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.pm.Installer;
import com.android.server.pm.Installer.InstallerException;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
            Slog.w("OverlayManager", "failed to generate idmap for " + targetPath + " and " + overlayPath + ": " + e.getMessage());
            return false;
        }
    }

    boolean removeIdmap(OverlayInfo oi, int userId) {
        try {
            this.mInstaller.removeIdmap(oi.baseCodePath);
            return true;
        } catch (InstallerException e) {
            Slog.w("OverlayManager", "failed to remove idmap for " + oi.baseCodePath + ": " + e.getMessage());
            return false;
        }
    }

    boolean idmapExists(OverlayInfo oi) {
        return new File(getIdmapPath(oi.baseCodePath)).isFile();
    }

    boolean idmapExists(PackageInfo overlayPackage, int userId) {
        return new File(getIdmapPath(overlayPackage.applicationInfo.getBaseCodePath())).isFile();
    }

    boolean isDangerous(PackageInfo overlayPackage, int userId) {
        return isDangerous(getIdmapPath(overlayPackage.applicationInfo.getBaseCodePath()));
    }

    private String getIdmapPath(String baseCodePath) {
        StringBuilder sb = new StringBuilder("/data/resource-cache/");
        sb.append(baseCodePath.substring(1).replace('/', '@'));
        sb.append("@idmap");
        return sb.toString();
    }

    private boolean isDangerous(String idmapPath) {
        Throwable th;
        boolean z = false;
        Throwable th2 = null;
        DataInputStream dataInputStream = null;
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(idmapPath));
            try {
                int magic = dis.readInt();
                int version = dis.readInt();
                if (dis.readInt() != 0) {
                    z = true;
                }
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 == null) {
                    return z;
                }
                try {
                    throw th2;
                } catch (IOException e) {
                    dataInputStream = dis;
                }
            } catch (Throwable th4) {
                th = th4;
                dataInputStream = dis;
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    try {
                        throw th2;
                    } catch (IOException e2) {
                        return true;
                    }
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (th2 == null) {
                throw th;
            }
            throw th2;
        }
    }
}
