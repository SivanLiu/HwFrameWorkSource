package com.android.server.power.batterysaver;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings.Global;
import com.android.server.power.batterysaver.BatterySaverController.Plugin;

public class BatterySaverLocationPlugin implements Plugin {
    private static final boolean DEBUG = false;
    private static final String TAG = "BatterySaverLocationPlugin";
    private final Context mContext;

    public BatterySaverLocationPlugin(Context context) {
        this.mContext = context;
    }

    public void onBatterySaverChanged(BatterySaverController caller) {
        updateLocationState(caller);
    }

    public void onSystemReady(BatterySaverController caller) {
        updateLocationState(caller);
    }

    private void updateLocationState(BatterySaverController caller) {
        int i = 0;
        boolean kill = caller.getBatterySaverPolicy().getGpsMode() == 2 && caller.isEnabled() && !caller.isInteractive();
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = "location_global_kill_switch";
        if (kill) {
            i = 1;
        }
        Global.putInt(contentResolver, str, i);
    }
}
