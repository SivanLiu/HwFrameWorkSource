package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class PackageInstallAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "PackageInstallAction";
    private static PackageInstallAction instance = null;
    /* access modifiers changed from: private */
    public String mPackageName = null;

    private PackageInstallAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_APP_INSTALL));
        OPCollectLog.r("PackageInstallAction", "PackageInstallAction");
    }

    public static PackageInstallAction getInstance(Context context) {
        PackageInstallAction packageInstallAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new PackageInstallAction(context, "PackageInstallAction");
            }
            packageInstallAction = instance;
        }
        return packageInstallAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new AppInstallReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            intentFilter.addDataScheme("package");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("PackageInstallAction", "enabled");
        }
    }

    class AppInstallReceiver extends BroadcastReceiver {
        AppInstallReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && (action = intent.getAction()) != null) {
                OPCollectLog.r("PackageInstallAction", "onReceive action: " + action);
                if ("android.intent.action.PACKAGE_ADDED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    String unused = PackageInstallAction.this.mPackageName = intent.getData().getSchemeSpecificPart();
                    PackageInstallAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_APP_INSTALL, this.mPackageName);
        this.mPackageName = null;
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyPackageInstallActionInstance();
        return true;
    }

    private static void destroyPackageInstallActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
