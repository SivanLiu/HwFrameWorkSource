package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class PackageUninstallAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "PackageUninstallAction";
    private static PackageUninstallAction instance = null;
    /* access modifiers changed from: private */
    public String mPackageName = null;

    private PackageUninstallAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_APP_UNINSTALL));
        OPCollectLog.r("PackageUninstallAction", "PackageUninstallAction");
    }

    public static PackageUninstallAction getInstance(Context context) {
        PackageUninstallAction packageUninstallAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new PackageUninstallAction(context, "PackageUninstallAction");
            }
            packageUninstallAction = instance;
        }
        return packageUninstallAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new AppUnInstallReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addDataScheme("package");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("PackageUninstallAction", "enabled");
        }
    }

    class AppUnInstallReceiver extends BroadcastReceiver {
        AppUnInstallReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && (action = intent.getAction()) != null) {
                OPCollectLog.r("PackageUninstallAction", "onReceive action: " + action);
                if ("android.intent.action.PACKAGE_REMOVED".equals(action) && intent.getBooleanExtra("android.intent.extra.REPLACING", false) == Boolean.FALSE.booleanValue()) {
                    String unused = PackageUninstallAction.this.mPackageName = intent.getData().getSchemeSpecificPart();
                    PackageUninstallAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_APP_UNINSTALL, this.mPackageName);
        this.mPackageName = null;
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyPackageUninstallActionInstance();
        return true;
    }

    private static void destroyPackageUninstallActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
