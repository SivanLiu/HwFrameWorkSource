package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.PrintWriter;

public class PackageUpdateAction extends Action {
    private static final String TAG = "PackageUpdateAction";
    private static PackageUpdateAction sInstance = null;
    private String mPackageName = null;
    private AppChangeReceiver mReceiver = null;

    class AppChangeReceiver extends BroadcastReceiver {
        AppChangeReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    OPCollectLog.r("PackageUpdateAction", "onReceive");
                    if ("android.intent.action.PACKAGE_REPLACED".equals(action)) {
                        PackageUpdateAction.this.mPackageName = intent.getData().getSchemeSpecificPart();
                        PackageUpdateAction.this.perform();
                    }
                }
            }
        }
    }

    public static synchronized PackageUpdateAction getInstance(Context context) {
        PackageUpdateAction packageUpdateAction;
        synchronized (PackageUpdateAction.class) {
            if (sInstance == null) {
                sInstance = new PackageUpdateAction(context, "PackageUpdateAction");
            }
            packageUpdateAction = sInstance;
        }
        return packageUpdateAction;
    }

    private PackageUpdateAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_APP_UPDATE));
        OPCollectLog.r("PackageUpdateAction", "PackageUpdateAction");
    }

    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new AppChangeReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            intentFilter.addDataScheme("package");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, OPCollectUtils.OPCOLLECT_PERMISSION, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("PackageUpdateAction", "enabled");
        }
    }

    protected boolean execute() {
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_APP_UPDATE, this.mPackageName);
        this.mPackageName = null;
        return true;
    }

    public boolean perform() {
        return super.perform();
    }

    public void disable() {
        super.disable();
        if (this.mReceiver != null && this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (PackageUpdateAction.class) {
            sInstance = null;
        }
    }

    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
        if (pw != null) {
            String indent = String.format("%" + indentNum + "s\\-", new Object[]{" "});
            if (this.mReceiver == null) {
                pw.println(indent + "receiver is null");
            } else {
                pw.println(indent + "receiver not null");
            }
        }
    }
}
