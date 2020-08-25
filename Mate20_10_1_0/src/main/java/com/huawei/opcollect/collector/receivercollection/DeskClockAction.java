package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class DeskClockAction extends ReceiverAction {
    private static final String BROADCAST_PERMISSION = "com.huawei.deskclock.broadcast.permission";
    private static final String CLOCK_ACTION_STRING = "huawei.deskclock.ALARM_ALERT_CONFLICT";
    private static final Object LOCK = new Object();
    private static final String TAG = "DeskClockAction";
    private static DeskClockAction instance = null;

    private DeskClockAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_DESKCLOCK_ALARM));
    }

    public static DeskClockAction getInstance(Context context) {
        DeskClockAction deskClockAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new DeskClockAction("DeskClockAction", context);
            }
            deskClockAction = instance;
        }
        return deskClockAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new DeskClockReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter(CLOCK_ACTION_STRING), BROADCAST_PERMISSION, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("DeskClockAction", "enabled");
        }
    }

    class DeskClockReceiver extends BroadcastReceiver {
        DeskClockReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("DeskClockAction", "onReceive action: " + action);
                if (DeskClockAction.CLOCK_ACTION_STRING.equalsIgnoreCase(action)) {
                    DeskClockAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_DESKCLOCK_ALARM);
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyDeskClockActionInstance();
        return true;
    }

    private static void destroyDeskClockActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
