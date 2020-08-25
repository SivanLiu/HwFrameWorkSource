package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;
import java.util.Locale;

public class PowerDisConnectedAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "PowerDisConnectedAction";
    private static PowerDisConnectedAction instance = null;

    private PowerDisConnectedAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_POWER_DISCONNECTED));
        OPCollectLog.r("PowerDisConnectedAction", "PowerDisConnectedAction");
    }

    public static PowerDisConnectedAction getInstance(Context context) {
        PowerDisConnectedAction powerDisConnectedAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new PowerDisConnectedAction(context, "PowerDisConnectedAction");
            }
            powerDisConnectedAction = instance;
        }
        return powerDisConnectedAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new PowerDisConnectedBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED"), null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("PowerDisConnectedAction", "enabled");
        }
    }

    class PowerDisConnectedBroadcastReceiver extends BroadcastReceiver {
        PowerDisConnectedBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("PowerDisConnectedAction", "onReceive action: " + action);
                if ("android.intent.action.ACTION_POWER_DISCONNECTED".equalsIgnoreCase(action)) {
                    PowerDisConnectedAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        Intent batteryIntent = null;
        if (this.mContext != null) {
            batteryIntent = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        }
        SysEventUtil.collectKVSysEventData("battery/charging_status", SysEventUtil.CHARGING_STATUS, SysEventUtil.OFF);
        if (batteryIntent == null) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_POWER_DISCONNECTED);
        } else {
            int level = batteryIntent.getIntExtra("level", -1);
            int scale = batteryIntent.getIntExtra("scale", -1);
            int batteryPct = -1;
            if (scale != 0) {
                batteryPct = (int) ((((float) level) / ((float) scale)) * 100.0f);
            }
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_POWER_DISCONNECTED, String.format(Locale.ROOT, "{level:%d}", Integer.valueOf(batteryPct)));
        }
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyPowerDisConnectedActionInstance();
        return true;
    }

    private static void destroyPowerDisConnectedActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
