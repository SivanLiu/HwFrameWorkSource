package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectConstant;

public class RebootAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static RebootAction instance = null;

    private RebootAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_REBOOT));
    }

    public static RebootAction getInstance(Context context) {
        RebootAction rebootAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new RebootAction(context, OPCollectConstant.REBOOT_ACTION_NAME);
            }
            rebootAction = instance;
        }
        return rebootAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null) {
            this.mReceiver = new BootBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.REBOOT"), "com.huawei.permission.OP_COLLECT", OdmfCollectScheduler.getInstance().getCtrlHandler());
        }
    }

    static class BootBroadcastReceiver extends BroadcastReceiver {
        BootBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && "android.intent.action.REBOOT".equalsIgnoreCase(intent.getAction())) {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_REBOOT);
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyRebootActionInstance();
        return true;
    }

    private static void destroyRebootActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
