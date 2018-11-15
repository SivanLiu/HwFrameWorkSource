package com.huawei.opcollect.collector.receivercollection;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import com.huawei.nb.model.collectencrypt.RawFgAPPEvent;
import com.huawei.opcollect.appchange.AppChangeImpl;
import com.huawei.opcollect.appchange.AppChangeListener;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.AbsActionParam;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Date;

public class HwForegroundAppAction extends Action implements AppChangeListener {
    private static final int APP_CHANGE_MESSAGE = 1;
    private static final String TAG = "HwForegroundAppAction";
    private static HwForegroundAppAction sInstance = null;
    private Handler mHandler = null;
    private String mLastPackageName = null;

    private static class AppActionParam extends AbsActionParam {
        private ComponentName componentName;
        private int pid;
        private int uid;

        AppActionParam(ComponentName componentName, int pid, int uid) {
            this.componentName = componentName;
            this.pid = pid;
            this.uid = uid;
        }

        ComponentName getComponentName() {
            return this.componentName;
        }

        public int getUid() {
            return this.uid;
        }

        public int getPid() {
            return this.pid;
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<HwForegroundAppAction> service;

        MyHandler(HwForegroundAppAction service) {
            this.service = new WeakReference(service);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            OPCollectLog.r("HwForegroundAppAction", "handleMessage ");
            HwForegroundAppAction action = (HwForegroundAppAction) this.service.get();
            if (action != null && msg.what == 1) {
                action.performWithArgs((AppActionParam) msg.obj);
            }
        }
    }

    private HwForegroundAppAction(Context context, String name) {
        super(context, name);
        OPCollectLog.r("HwForegroundAppAction", "HwForegroundAppAction");
        setDailyRecordNum(queryDailyRecordNum(RawFgAPPEvent.class));
    }

    public static synchronized HwForegroundAppAction getInstance(Context context) {
        HwForegroundAppAction hwForegroundAppAction;
        synchronized (HwForegroundAppAction.class) {
            if (sInstance == null) {
                sInstance = new HwForegroundAppAction(context, "HwForegroundAppAction");
            }
            hwForegroundAppAction = sInstance;
        }
        return hwForegroundAppAction;
    }

    public void enable() {
        OPCollectLog.r("HwForegroundAppAction", "FG enable");
        super.enable();
        this.mHandler = new MyHandler(this);
        AppChangeImpl.getInstance(this.mContext).addListener(this);
    }

    protected boolean executeWithArgs(AbsActionParam absActionParam) {
        OPCollectLog.d("HwForegroundAppAction", "FG executeWithArgs");
        if (absActionParam == null || !(absActionParam instanceof AppActionParam)) {
            return false;
        }
        ComponentName componentName = ((AppActionParam) absActionParam).getComponentName();
        if (componentName == null) {
            return false;
        }
        RawFgAPPEvent rawFgAPPEvent = new RawFgAPPEvent();
        rawFgAPPEvent.setMPackageName(componentName.getPackageName());
        rawFgAPPEvent.setMTimeStamp(new Date());
        rawFgAPPEvent.setMReservedText(OPCollectUtils.formatCurrentTime());
        rawFgAPPEvent.setMStatus(Integer.valueOf(1));
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawFgAPPEvent).sendToTarget();
        return true;
    }

    public boolean perform() {
        return super.perform();
    }

    public void disable() {
        OPCollectLog.r("HwForegroundAppAction", "FG disable");
        super.disable();
        AppChangeImpl.getInstance(this.mContext).removeListener(this);
        if (this.mHandler != null) {
            this.mHandler = null;
        }
    }

    public boolean destroy() {
        OPCollectLog.r("HwForegroundAppAction", "FG destroy");
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (HwForegroundAppAction.class) {
            sInstance = null;
        }
    }

    public void onAppChange(int pid, int uid, ComponentName componentName) {
        if (componentName == null) {
            OPCollectLog.e("HwForegroundAppAction", "componentName is null");
            return;
        }
        String pkgName = componentName.getPackageName();
        if (pkgName.equals(this.mLastPackageName)) {
            OPCollectLog.e("HwForegroundAppAction", "duplicate pkgName.");
            return;
        }
        this.mLastPackageName = pkgName;
        AppActionParam actionParam = new AppActionParam(componentName, pid, uid);
        if (this.mHandler != null) {
            this.mHandler.obtainMessage(1, actionParam).sendToTarget();
        }
    }

    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
    }
}
