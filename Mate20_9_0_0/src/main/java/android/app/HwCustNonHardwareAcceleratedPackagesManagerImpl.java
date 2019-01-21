package android.app;

import android.app.INonHardwareAcceleratedPackagesManager.Stub;
import android.content.ComponentName;
import android.content.HwCustContext;
import android.content.pm.ActivityInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;

public class HwCustNonHardwareAcceleratedPackagesManagerImpl extends HwCustNonHardwareAcceleratedPackagesManager {
    private static boolean HWDBG = false;
    private static boolean HWFLOW = false;
    private static final String TAG = "NonHardAccelPkgs";
    private INonHardwareAcceleratedPackagesManager mService;

    static {
        boolean z = true;
        boolean z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z2;
        if (!(Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)))) {
            z = false;
        }
        HWDBG = z;
    }

    private void getService() {
        this.mService = Stub.asInterface(ServiceManager.getService(HwCustContext.NON_HARD_ACCEL_PKGS_SERVICE));
    }

    public HwCustNonHardwareAcceleratedPackagesManagerImpl() {
        getService();
    }

    public boolean shouldForceEnabled(ActivityInfo ai, ComponentName instrumentationClass) {
        if (!UserHandle.isApp(ai.applicationInfo.uid) || ai.applicationInfo.targetSdkVersion < 5 || (ai.flags & 512) != 0 || instrumentationClass != null) {
            return false;
        }
        String pkgName = ai.applicationInfo.packageName;
        boolean ret = false;
        if (this.mService != null) {
            try {
                if (this.mService.hasPackage(pkgName)) {
                    ret = this.mService.getForceEnabled(pkgName);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "shouldForceEnabled: RemoteException", e);
                getService();
            }
        }
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("shouldForceEnabled: ");
            stringBuilder.append(pkgName);
            stringBuilder.append(" ");
            stringBuilder.append(ret);
            Slog.d(str, stringBuilder.toString());
        }
        return ret;
    }

    public void setForceEnabled(String pkgName, boolean force) {
        if (this.mService != null) {
            try {
                this.mService.setForceEnabled(pkgName, force);
            } catch (RemoteException e) {
                Log.e(TAG, "setForceEnabled: RemoteException", e);
                getService();
            }
        }
    }

    public boolean getForceEnabled(String pkgName) {
        if (this.mService != null) {
            try {
                return this.mService.getForceEnabled(pkgName);
            } catch (RemoteException e) {
                Log.e(TAG, "getForceEnabled: RemoteException", e);
                getService();
            }
        }
        return false;
    }

    public boolean hasPackage(String pkgName) {
        if (this.mService != null) {
            try {
                return this.mService.hasPackage(pkgName);
            } catch (RemoteException e) {
                Log.e(TAG, "hasPackage: RemoteException", e);
                getService();
            }
        }
        return false;
    }

    public void removePackage(String pkgName) {
        if (this.mService != null) {
            try {
                this.mService.removePackage(pkgName);
            } catch (RemoteException e) {
                Log.e(TAG, "removePackage: RemoteException", e);
                getService();
            }
        }
    }
}
