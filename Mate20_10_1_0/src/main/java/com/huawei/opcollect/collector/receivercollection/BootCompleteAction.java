package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class BootCompleteAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "BootCompleteAction";
    private static BootCompleteAction instance = null;

    private BootCompleteAction(Context context, String name) {
        super(context, name);
        OPCollectLog.r("BootCompleteAction", "BootCompleteAction");
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_BOOT_COMPLETED));
    }

    public static BootCompleteAction getInstance(Context context) {
        BootCompleteAction bootCompleteAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new BootCompleteAction(context, "BootCompleteAction");
            }
            bootCompleteAction = instance;
        }
        return bootCompleteAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new BootBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.BOOT_COMPLETED"), null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("BootCompleteAction", "enabled");
        }
    }

    class BootBroadcastReceiver extends BroadcastReceiver {
        BootBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("BootCompleteAction", "action: " + action);
                if ("android.intent.action.BOOT_COMPLETED".equalsIgnoreCase(action)) {
                    BootCompleteAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_BOOT_COMPLETED);
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyBootCompleteActionInstance();
        return true;
    }

    private static void destroyBootCompleteActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
