package com.android.server.rms.dump;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.rms.HwSysResImpl;
import android.rms.HwSysResource;
import android.util.Log;
import com.android.server.security.tsmagent.logic.spi.tsm.laser.LaserTSMServiceImpl;

public final class DumpResource {
    public static final void dumpNotificationWhiteList(Context context) {
        HwSysResource sysResource = HwSysResImpl.getResource(10);
        String[] INTEREST = new String[]{"com.whatsapp", "com.tencent.mm", "com.google.android.gm"};
        if (sysResource != null) {
            int length = INTEREST.length;
            int uid = LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS;
            int strategy = 1;
            int strategy2 = 0;
            while (strategy2 < length) {
                String pkg = INTEREST[strategy2];
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DumpResource !pkg");
                stringBuilder.append(pkg);
                Log.i("RMS.Dump", stringBuilder.toString());
                int strategy3 = strategy;
                for (strategy = 0; strategy < 50; strategy++) {
                    strategy3 = sysResource.acquire(uid, pkg, -1);
                }
                if (1 != strategy3) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("dumpNotificationWhiteList fails:pkg:");
                    stringBuilder.append(pkg);
                    Log.e("RMS.Dump", stringBuilder.toString());
                }
                sysResource.clear(uid, pkg, -1);
                uid++;
                strategy2++;
                strategy = strategy3;
            }
            Log.i("RMS.Dump", "dumpNotificationWhiteList pass !");
            strategy2 = strategy;
        }
    }

    public static final void dumpContentObserver(Context context, String[] args) {
        if (args.length == 2 && args[1] != null) {
            String pkg = args[1];
            int uid = LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS;
            HwSysResource sysResource = HwSysResImpl.getResource(29);
            if (!(sysResource == null || pkg == null)) {
                try {
                    uid = context.getPackageManager().getApplicationInfo(pkg, 1).uid;
                } catch (NameNotFoundException e) {
                    Log.w("RMS.Dump", "get packagemanager failed!");
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("begin DumpResource contentObserver, 115, packageName: ");
                stringBuilder.append(pkg);
                stringBuilder.append(", uid: ");
                stringBuilder.append(uid);
                Log.i("RMS.Dump", stringBuilder.toString());
                for (int i = 0; i < 115; i++) {
                    sysResource.acquire(uid, pkg, -1);
                }
            }
        }
    }
}
