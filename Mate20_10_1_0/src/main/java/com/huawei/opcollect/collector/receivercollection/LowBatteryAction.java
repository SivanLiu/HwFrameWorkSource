package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class LowBatteryAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "LowBatteryAction";
    private static LowBatteryAction instance = null;

    private LowBatteryAction(Context context, String name) {
        super(context, name);
        OPCollectLog.r("LowBatteryAction", "LowBatteryAction");
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_LOW_POWER));
    }

    public static LowBatteryAction getInstance(Context context) {
        LowBatteryAction lowBatteryAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new LowBatteryAction(context, "LowBatteryAction");
            }
            lowBatteryAction = instance;
        }
        return lowBatteryAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new LowBatteryBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.BATTERY_LOW"), "com.huawei.permission.OP_COLLECT", OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("LowBatteryAction", "enabled");
        }
    }

    class LowBatteryBroadcastReceiver extends BroadcastReceiver {
        LowBatteryBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                OPCollectLog.r("LowBatteryAction", "onReceive action: " + intent.getAction());
                if ("android.intent.action.BATTERY_LOW".equalsIgnoreCase(intent.getAction())) {
                    OPCollectLog.r("LowBatteryAction", "onReceive");
                    LowBatteryAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_LOW_POWER);
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyLowBatteryActionInstance();
        return true;
    }

    private static void destroyLowBatteryActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
