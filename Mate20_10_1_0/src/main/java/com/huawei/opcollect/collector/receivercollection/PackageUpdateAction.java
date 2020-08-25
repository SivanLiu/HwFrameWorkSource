package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class PackageUpdateAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "PackageUpdateAction";
    private static PackageUpdateAction instance = null;
    /* access modifiers changed from: private */
    public String mPackageName = null;

    private PackageUpdateAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_APP_UPDATE));
        OPCollectLog.r("PackageUpdateAction", "PackageUpdateAction");
    }

    public static PackageUpdateAction getInstance(Context context) {
        PackageUpdateAction packageUpdateAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new PackageUpdateAction(context, "PackageUpdateAction");
            }
            packageUpdateAction = instance;
        }
        return packageUpdateAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new AppUpdateReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            intentFilter.addDataScheme("package");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("PackageUpdateAction", "enabled");
        }
    }

    class AppUpdateReceiver extends BroadcastReceiver {
        AppUpdateReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && (action = intent.getAction()) != null) {
                OPCollectLog.r("PackageUpdateAction", "onReceive");
                if ("android.intent.action.PACKAGE_REPLACED".equals(action)) {
                    String unused = PackageUpdateAction.this.mPackageName = intent.getData().getSchemeSpecificPart();
                    PackageUpdateAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_APP_UPDATE, this.mPackageName);
        this.mPackageName = null;
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyPackageUpdateActionInstance();
        return true;
    }

    private static void destroyPackageUpdateActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
