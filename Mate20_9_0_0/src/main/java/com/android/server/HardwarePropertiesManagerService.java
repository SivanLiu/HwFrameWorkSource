package com.android.server;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Binder;
import android.os.CpuUsageInfo;
import android.os.IHardwarePropertiesManager.Stub;
import android.os.UserHandle;
import com.android.internal.util.DumpUtils;
import com.android.server.vr.VrManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class HardwarePropertiesManagerService extends Stub {
    private static final String TAG = "HardwarePropertiesManagerService";
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final Object mLock = new Object();

    private static native CpuUsageInfo[] nativeGetCpuUsages();

    private static native float[] nativeGetDeviceTemperatures(int i, int i2);

    private static native float[] nativeGetFanSpeeds();

    private static native void nativeInit();

    public HardwarePropertiesManagerService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        synchronized (this.mLock) {
            nativeInit();
        }
    }

    public float[] getDeviceTemperatures(String callingPackage, int type, int source) throws SecurityException {
        float[] nativeGetDeviceTemperatures;
        enforceHardwarePropertiesRetrievalAllowed(callingPackage);
        synchronized (this.mLock) {
            nativeGetDeviceTemperatures = nativeGetDeviceTemperatures(type, source);
        }
        return nativeGetDeviceTemperatures;
    }

    public CpuUsageInfo[] getCpuUsages(String callingPackage) throws SecurityException {
        CpuUsageInfo[] nativeGetCpuUsages;
        enforceHardwarePropertiesRetrievalAllowed(callingPackage);
        synchronized (this.mLock) {
            nativeGetCpuUsages = nativeGetCpuUsages();
        }
        return nativeGetCpuUsages;
    }

    public float[] getFanSpeeds(String callingPackage) throws SecurityException {
        float[] nativeGetFanSpeeds;
        enforceHardwarePropertiesRetrievalAllowed(callingPackage);
        synchronized (this.mLock) {
            nativeGetFanSpeeds = nativeGetFanSpeeds();
        }
        return nativeGetFanSpeeds;
    }

    private String getCallingPackageName() {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packages == null || packages.length <= 0) {
            return Shell.NIGHT_MODE_STR_UNKNOWN;
        }
        return packages[0];
    }

    private void dumpTempValues(String pkg, PrintWriter pw, int type, String typeLabel) {
        String str = pkg;
        PrintWriter printWriter = pw;
        int i = type;
        String str2 = typeLabel;
        dumpTempValues(str, printWriter, i, str2, "temperatures: ", 0);
        String str3 = pkg;
        PrintWriter printWriter2 = pw;
        int i2 = type;
        String str4 = typeLabel;
        dumpTempValues(str3, printWriter2, i2, str4, "throttling temperatures: ", 1);
        dumpTempValues(str, printWriter, i, str2, "shutdown temperatures: ", 2);
        dumpTempValues(str3, printWriter2, i2, str4, "vr throttling temperatures: ", 3);
    }

    private void dumpTempValues(String pkg, PrintWriter pw, int type, String typeLabel, String subLabel, int valueType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(typeLabel);
        stringBuilder.append(subLabel);
        stringBuilder.append(Arrays.toString(getDeviceTemperatures(pkg, type, valueType)));
        pw.println(stringBuilder.toString());
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("****** Dump of HardwarePropertiesManagerService ******");
            String PKG = getCallingPackageName();
            int i = 0;
            dumpTempValues(PKG, pw, 0, "CPU ");
            dumpTempValues(PKG, pw, 1, "GPU ");
            dumpTempValues(PKG, pw, 2, "Battery ");
            dumpTempValues(PKG, pw, 3, "Skin ");
            float[] fanSpeeds = getFanSpeeds(PKG);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Fan speed: ");
            stringBuilder.append(Arrays.toString(fanSpeeds));
            stringBuilder.append("\n");
            pw.println(stringBuilder.toString());
            CpuUsageInfo[] cpuUsageInfos = getCpuUsages(PKG);
            while (i < cpuUsageInfos.length) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Cpu usage of core: ");
                stringBuilder2.append(i);
                stringBuilder2.append(", active = ");
                stringBuilder2.append(cpuUsageInfos[i].getActive());
                stringBuilder2.append(", total = ");
                stringBuilder2.append(cpuUsageInfos[i].getTotal());
                pw.println(stringBuilder2.toString());
                i++;
            }
            pw.println("****** End of HardwarePropertiesManagerService dump ******");
        }
    }

    private void enforceHardwarePropertiesRetrievalAllowed(String callingPackage) throws SecurityException {
        this.mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        VrManagerInternal vrService = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        if (!((DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class)).isDeviceOwnerApp(callingPackage) && !vrService.isCurrentVrListener(callingPackage, userId) && this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0) {
            throw new SecurityException("The caller is not a device owner, bound VrListenerService, or holding the DEVICE_POWER permission.");
        }
    }
}
