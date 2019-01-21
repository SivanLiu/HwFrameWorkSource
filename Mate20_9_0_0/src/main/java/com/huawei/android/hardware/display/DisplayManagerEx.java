package com.huawei.android.hardware.display;

import android.hardware.display.DisplayManagerGlobal;
import android.os.Process;
import android.util.Log;

public class DisplayManagerEx {
    private static final String TAG = "DisplayManagerEx";

    private static DisplayManagerGlobal getDisplayManagerGlobal() {
        return DisplayManagerGlobal.getInstance();
    }

    public static void startWifiDisplayScan(int channelId) {
        if (getDisplayManagerGlobal() != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startWifiDisplayScan, pid:");
            stringBuilder.append(Process.myPid());
            stringBuilder.append(", tid:");
            stringBuilder.append(Process.myTid());
            stringBuilder.append(", uid:");
            stringBuilder.append(Process.myUid());
            Log.d(str, stringBuilder.toString());
            getDisplayManagerGlobal().startWifiDisplayScan(channelId);
        }
    }

    public static void connectWifiDisplay(String deviceAddress, String verificaitonCode) {
        if (getDisplayManagerGlobal() != null) {
            getDisplayManagerGlobal().connectWifiDisplay(deviceAddress, verificaitonCode);
        }
    }

    public static boolean checkVerificationResult(boolean isRight) {
        if (getDisplayManagerGlobal() == null) {
            return false;
        }
        getDisplayManagerGlobal().checkVerificationResult(isRight);
        return true;
    }
}
