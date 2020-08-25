package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.powerkit.PowerKitManager;
import com.huawei.opcollect.strategy.AbsActionParam;
import com.huawei.opcollect.utils.OPCollectLog;

public class BatteryAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "BatteryAction";
    private static BatteryAction instance = null;
    /* access modifiers changed from: private */
    public PowerKitManager mPowerKitManager;

    private BatteryAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.BATTERY_LEFT));
        this.mPowerKitManager = PowerKitManager.getInstance(context);
    }

    public static BatteryAction getInstance(Context context) {
        BatteryAction batteryAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new BatteryAction(SysEventUtil.BATTERY_LEFT, context);
            }
            batteryAction = instance;
        }
        return batteryAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new BatteryChangeReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"), null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r(TAG, "enabled");
        }
    }

    class BatteryChangeReceiver extends BroadcastReceiver {
        BatteryChangeReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (BatteryAction.this.mPowerKitManager != null && BatteryAction.this.mPowerKitManager.isUserSleeping()) {
                OPCollectLog.r(BatteryAction.TAG, "user sleep just ignore");
            } else if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r(BatteryAction.TAG, "onReceive action: " + action);
                if ("android.intent.action.BATTERY_CHANGED".equalsIgnoreCase(action)) {
                    int level = intent.getIntExtra("level", -1);
                    int scale = intent.getIntExtra("scale", -1);
                    if (scale != 0) {
                        boolean unused = BatteryAction.this.performWithArgs(new BatteryActionParam(String.valueOf((int) ((((float) level) / ((float) scale)) * 100.0f))));
                    }
                }
            }
        }
    }

    private class BatteryActionParam extends AbsActionParam {
        private String battery;

        BatteryActionParam(String battery2) {
            this.battery = battery2;
        }

        /* access modifiers changed from: package-private */
        public String getBattery() {
            return this.battery;
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean executeWithArgs(AbsActionParam absActionParam) {
        if (absActionParam == null) {
            return true;
        }
        SysEventUtil.collectSysEventData(SysEventUtil.BATTERY_LEFT, ((BatteryActionParam) absActionParam).getBattery());
        SysEventUtil.collectKVSysEventData("battery/battery_left", SysEventUtil.BATTERY_LEFT, ((BatteryActionParam) absActionParam).getBattery());
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyBatteryActionInstance();
        return true;
    }

    private static void destroyBatteryActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
