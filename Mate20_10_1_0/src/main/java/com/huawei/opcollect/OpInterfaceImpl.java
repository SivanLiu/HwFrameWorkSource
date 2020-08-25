package com.huawei.opcollect;

import android.content.Context;
import android.os.Handler;
import com.huawei.opcollect.config.CollectConfigManager;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.PrintWriter;

public class OpInterfaceImpl implements OpInterface {
    private static final String TAG = "OpInterfaceImpl";
    private boolean isModuleCanBeStarted = true;
    private Context mContext;

    public OpInterfaceImpl() {
        OPCollectLog.r(TAG, "OpInterface impl");
    }

    @Override // com.huawei.opcollect.OpInterface
    public void initialize(Context context) {
        OPCollectLog.r(TAG, "OpInterface initialize impl: " + context);
        this.mContext = context;
        this.isModuleCanBeStarted = CollectConfigManager.getInstance().isModuleCanBeStarted();
        if (this.isModuleCanBeStarted) {
            OPCollectLog.r(TAG, "the switch is on");
            OdmfCollectScheduler.getInstance().initialize(context);
            return;
        }
        OPCollectLog.e(TAG, "the switch is off");
    }

    @Override // com.huawei.opcollect.OpInterface
    public void switchOn() {
        OPCollectLog.r(TAG, "OpInterface impl switch on.");
        if (!OPCollectUtils.isPkgInstalled(this.mContext, OPCollectUtils.ODMF_PACKAGE_NAME)) {
            OPCollectLog.e(TAG, "odmf is not installed.");
            return;
        }
        Handler handler = OdmfCollectScheduler.getInstance().getCtrlHandler();
        if (handler != null) {
            handler.removeMessages(2);
            handler.sendEmptyMessageDelayed(2, 0);
        }
    }

    @Override // com.huawei.opcollect.OpInterface
    public void switchOff() {
        OPCollectLog.r(TAG, "OpInterface impl switch off.");
        if (!OPCollectUtils.isPkgInstalled(this.mContext, OPCollectUtils.ODMF_PACKAGE_NAME)) {
            OPCollectLog.e(TAG, "odmf is not installed.");
            return;
        }
        Handler handler = OdmfCollectScheduler.getInstance().getCtrlHandler();
        if (handler != null) {
            handler.removeMessages(3);
            handler.sendEmptyMessageDelayed(3, 0);
        }
    }

    @Override // com.huawei.opcollect.OpInterface
    public void dump(PrintWriter pw) {
        OPCollectLog.r(TAG, "OpInterface dump impl");
        if (!OPCollectUtils.isPkgInstalled(this.mContext, OPCollectUtils.ODMF_PACKAGE_NAME)) {
            OPCollectLog.e(TAG, "odmf is not installed.");
        } else if (this.isModuleCanBeStarted) {
            OdmfCollectScheduler.dump(pw);
        }
    }
}
