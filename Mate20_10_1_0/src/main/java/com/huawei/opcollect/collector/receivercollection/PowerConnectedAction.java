package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import java.util.Locale;

public class PowerConnectedAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "PowerConnectedAction";
    private static PowerConnectedAction instance = null;

    private PowerConnectedAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_POWER_CONNECTED));
        OPCollectLog.r("PowerConnectedAction", OPCollectConstant.PACKAGE_UPDATE_ACTION_NAME);
    }

    public static PowerConnectedAction getInstance(Context context) {
        PowerConnectedAction powerConnectedAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new PowerConnectedAction(context, "PowerConnectedAction");
            }
            powerConnectedAction = instance;
        }
        return powerConnectedAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new PowerConnectedBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"), null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("PowerConnectedAction", "enabled");
        }
    }

    class PowerConnectedBroadcastReceiver extends BroadcastReceiver {
        PowerConnectedBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("PowerConnectedAction", "onReceive action: " + action);
                if ("android.intent.action.ACTION_POWER_CONNECTED".equalsIgnoreCase(action)) {
                    PowerConnectedAction.this.perform();
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
        SysEventUtil.collectKVSysEventData("battery/charging_status", SysEventUtil.CHARGING_STATUS, SysEventUtil.ON);
        if (batteryIntent == null) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_POWER_CONNECTED);
        } else {
            int chargePlug = batteryIntent.getIntExtra("plugged", -1);
            String chargeType = "";
            if (chargePlug == 2) {
                chargeType = "USB";
            } else if (chargePlug == 1) {
                chargeType = "AC";
            } else if (chargePlug == 4) {
                chargeType = "Wireless";
            }
            int level = batteryIntent.getIntExtra("level", -1);
            int scale = batteryIntent.getIntExtra("scale", -1);
            int batteryPct = -1;
            if (scale != 0) {
                batteryPct = (int) ((((float) level) / ((float) scale)) * 100.0f);
            }
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_POWER_CONNECTED, String.format(Locale.ROOT, "{level:%d, chargeType:%s}", Integer.valueOf(batteryPct), chargeType));
        }
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyPowerConnectedActionInstance();
        return true;
    }

    private static void destroyPowerConnectedActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
