package com.huawei.opcollect.collector.receivercollection;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import com.huawei.nb.model.collectencrypt.RawFgAPPEvent;
import com.huawei.opcollect.appchange.AppChangeImpl;
import com.huawei.opcollect.appchange.AppChangeListener;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.odmf.OdmfHelper;
import com.huawei.opcollect.strategy.AbsActionParam;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.lang.ref.WeakReference;
import java.util.Date;

public class HwForegroundAppAction extends Action implements AppChangeListener {
    private static final int APP_CHANGE_MESSAGE = 1;
    private static final Object LOCK = new Object();
    private static final String TAG = "HwForegroundAppAction";
    private static HwForegroundAppAction instance = null;
    private Handler mHandler = null;

    private HwForegroundAppAction(Context context, String name) {
        super(context, name);
        OPCollectLog.r("HwForegroundAppAction", "HwForegroundAppAction");
        setDailyRecordNum(queryDailyRecordNum(RawFgAPPEvent.class));
    }

    public static HwForegroundAppAction getInstance(Context context) {
        HwForegroundAppAction hwForegroundAppAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new HwForegroundAppAction(context, "HwForegroundAppAction");
            }
            hwForegroundAppAction = instance;
        }
        return hwForegroundAppAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        OPCollectLog.r("HwForegroundAppAction", "FG enable");
        super.enable();
        this.mHandler = new MyHandler(this);
        AppChangeImpl.getInstance(this.mContext).addListener(this);
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean executeWithArgs(AbsActionParam absActionParam) {
        ComponentName componentName;
        OPCollectLog.d("HwForegroundAppAction", "FG executeWithArgs");
        if (absActionParam == null || !(absActionParam instanceof AppActionParam) || (componentName = ((AppActionParam) absActionParam).getComponentName()) == null) {
            return false;
        }
        RawFgAPPEvent rawFgAppEvent = new RawFgAPPEvent();
        rawFgAppEvent.setMPackageName(componentName.getPackageName());
        if (OPCollectUtils.checkODMFApiVersion(this.mContext, OdmfHelper.ODMF_API_VERSION_2_11_3)) {
            rawFgAppEvent.setMActivityName(componentName.getClassName());
        }
        rawFgAppEvent.setMTimeStamp(new Date());
        rawFgAppEvent.setMReservedText(OPCollectUtils.formatCurrentTime());
        rawFgAppEvent.setMStatus(1);
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawFgAppEvent).sendToTarget();
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean perform() {
        return super.perform();
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void disable() {
        OPCollectLog.r("HwForegroundAppAction", "FG disable");
        super.disable();
        AppChangeImpl.getInstance(this.mContext).removeListener(this);
        if (this.mHandler != null) {
            this.mHandler = null;
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        OPCollectLog.r("HwForegroundAppAction", "FG destroy");
        super.destroy();
        destroyHwForegroundAppActionInstance();
        return true;
    }

    private static void destroyHwForegroundAppActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    @Override // com.huawei.opcollect.appchange.AppChangeListener
    public void onAppChange(int pid, int uid, ComponentName componentName) {
        if (componentName == null) {
            OPCollectLog.e("HwForegroundAppAction", "componentName is null");
            return;
        }
        AppActionParam actionParam = new AppActionParam(componentName, pid, uid);
        if (this.mHandler != null) {
            this.mHandler.obtainMessage(1, actionParam).sendToTarget();
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<HwForegroundAppAction> service;

        MyHandler(HwForegroundAppAction service2) {
            this.service = new WeakReference<>(service2);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            OPCollectLog.r("HwForegroundAppAction", "handleMessage ");
            HwForegroundAppAction action = this.service.get();
            if (action != null && msg.what == 1) {
                boolean unused = action.performWithArgs((AppActionParam) msg.obj);
            }
        }
    }

    private static class AppActionParam extends AbsActionParam {
        private ComponentName componentName;
        private int pid;
        private int uid;

        AppActionParam(ComponentName componentName2, int pid2, int uid2) {
            this.componentName = componentName2;
            this.pid = pid2;
            this.uid = uid2;
        }

        /* access modifiers changed from: package-private */
        public ComponentName getComponentName() {
            return this.componentName;
        }

        public int getUid() {
            return this.uid;
        }

        public int getPid() {
            return this.pid;
        }
    }
}
