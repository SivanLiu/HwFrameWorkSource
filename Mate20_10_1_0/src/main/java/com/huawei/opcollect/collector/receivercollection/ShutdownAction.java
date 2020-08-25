package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class ShutdownAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";
    private static final String TAG = "ShutdownAction";
    private static ShutdownAction instance = null;

    private ShutdownAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_SHUTDOWN_PHONE));
        OPCollectLog.r("ShutdownAction", "ShutdownAction");
    }

    public static ShutdownAction getInstance(Context context) {
        ShutdownAction shutdownAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new ShutdownAction(context, "ShutdownAction");
            }
            shutdownAction = instance;
        }
        return shutdownAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new ShutdownBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"), "com.huawei.permission.OP_COLLECT", OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("ShutdownAction", "enabled");
        }
    }

    class ShutdownBroadcastReceiver extends BroadcastReceiver {
        ShutdownBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("ShutdownAction", "onReceiver action: " + action);
                if ("android.intent.action.ACTION_SHUTDOWN".equalsIgnoreCase(action)) {
                    ShutdownAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_SHUTDOWN_PHONE, SystemPropertiesEx.get(SHUTDOWN_ACTION_PROPERTY, ""));
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyShutdownActionInstance();
        return true;
    }

    private static void destroyShutdownActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
