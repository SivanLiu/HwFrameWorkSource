package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwBrightnessXmlLoader;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;

public class HwBrightnessBatteryDetection extends BroadcastReceiver {
    private static final boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final int MAXDEFAULTBRIGHTNESS = 255;
    private static final String TAG = "HwBrightnessBatteryDetection";
    private boolean mBatteryModeStatus = false;
    private Callbacks mCallbacks;
    private final Context mContext;
    private final HwBrightnessXmlLoader.Data mData;
    private int mLowBatteryMaxBrightness = 255;

    public interface Callbacks {
        void updateBrightnessFromBattery(int i);
    }

    public HwBrightnessBatteryDetection(Callbacks callbacks, Context context) {
        this.mCallbacks = callbacks;
        this.mContext = context;
        this.mData = HwBrightnessXmlLoader.getData();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(this, filter);
    }

    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            Slog.e(TAG, "Invalid input parameter!");
        } else if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
            int batteryLevel = intent.getIntExtra(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL, 0);
            boolean curBatteryStatus = false;
            if (batteryLevel <= this.mData.batteryLowLevelTh) {
                this.mLowBatteryMaxBrightness = this.mData.batteryLowLevelMaxBrightness;
                curBatteryStatus = true;
            } else {
                this.mLowBatteryMaxBrightness = 255;
            }
            if (DEBUG) {
                Slog.d(TAG, "batteryLevel =" + batteryLevel + ", curBatteryStatus=" + curBatteryStatus + ", BatteryModeStatus =" + this.mBatteryModeStatus);
            }
            if (curBatteryStatus != this.mBatteryModeStatus) {
                this.mCallbacks.updateBrightnessFromBattery(this.mLowBatteryMaxBrightness);
                this.mBatteryModeStatus = curBatteryStatus;
            }
        }
    }
}
