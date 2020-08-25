package com.android.server.displayside;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Slog;
import com.android.server.wm.utils.HwDisplaySizeUtil;

public class HwExtDisplayWhitelistUpdateReceiver extends BroadcastReceiver {
    private static final String ACTION_CFG_UPDATED = "huawei.android.hwouc.intent.action.CFG_UPDATED";
    private static final String TAG = "HwExtDisplayWhitelistUpdateReceiver";

    public void onReceive(Context context, Intent intent) {
        Slog.i(TAG, "HwExtDisplayWhitelistUpdateReceiver enter.");
        if (context == null || intent == null) {
            Slog.i(TAG, "HwExtDisplayWhitelistUpdateReceiver return null.");
        } else if (ACTION_CFG_UPDATED.equals(intent.getAction())) {
            Slog.i(TAG, "receiver update action ACTION_CFG_UPDATED");
            if (HwDisplaySizeUtil.hasSideInScreen()) {
                HwDisplaySideRegionConfig.getInstance().updateWhitelistByOuc();
            }
        }
    }
}
