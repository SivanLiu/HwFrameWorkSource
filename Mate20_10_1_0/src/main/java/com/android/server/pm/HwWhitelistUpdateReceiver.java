package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.HwFoldScreenState;
import android.os.ServiceManager;
import android.util.Slog;

public class HwWhitelistUpdateReceiver extends BroadcastReceiver {
    private static final String ACTION_CFG_UPDATED = "huawei.android.hwouc.intent.action.CFG_UPDATED";
    private static final String TAG = "HwWhitelistUpdateReceiver";

    public void onReceive(Context context, Intent intent) {
        if (HwFoldScreenState.isFoldScreenDevice()) {
            if (intent == null || context == null) {
                Slog.i(TAG, "intent is " + intent + "context is " + context);
            } else if (ACTION_CFG_UPDATED.equals(intent.getAction())) {
                Slog.i(TAG, "hot update action: " + intent.getAction());
                ServiceManager.getService("package").getHwPMSEx().updateWhitelistByHot();
            }
        }
    }
}
