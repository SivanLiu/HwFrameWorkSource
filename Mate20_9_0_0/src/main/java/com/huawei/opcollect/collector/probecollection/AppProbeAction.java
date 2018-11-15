package com.huawei.opcollect.collector.probecollection;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import com.huawei.nb.model.collectencrypt.RawAppProbe;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.lang.ref.WeakReference;

public class AppProbeAction extends Action {
    private static final int MESSAGE_ON_CHANGE = 1;
    private static AppProbeAction sInstance;
    private Handler mHandler = null;
    private RawAppProbe mRawAppProbe = null;

    private static class AppProbeHandler extends Handler {
        private final WeakReference<AppProbeAction> service;

        AppProbeHandler(AppProbeAction service) {
            this.service = new WeakReference(service);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                AppProbeAction appProbeAction = (AppProbeAction) this.service.get();
                if (appProbeAction != null) {
                    appProbeAction.mRawAppProbe = (RawAppProbe) msg.obj;
                    appProbeAction.perform();
                }
            }
        }
    }

    public static synchronized AppProbeAction getInstance(Context context) {
        AppProbeAction appProbeAction;
        synchronized (AppProbeAction.class) {
            if (sInstance == null) {
                sInstance = new AppProbeAction(context, OPCollectConstant.APP_PROBE_ACTION_NAME);
            }
            appProbeAction = sInstance;
        }
        return appProbeAction;
    }

    private AppProbeAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(queryDailyRecordNum(RawAppProbe.class));
    }

    private void initializeRawAppProbe(String pkgName, int eventID, String message) {
        if (this.mHandler != null) {
            RawAppProbe rawAppProbe = new RawAppProbe();
            rawAppProbe.setMTimeStamp(OPCollectUtils.getCurrentTime());
            rawAppProbe.setMContent(message);
            rawAppProbe.setMPackageName(pkgName);
            rawAppProbe.setMEventID(Integer.valueOf(eventID));
            rawAppProbe.setMAppVersion(OPCollectUtils.getVersionName(this.mContext, pkgName));
            rawAppProbe.setMReservedInt(Integer.valueOf(0));
            rawAppProbe.setMReservedText("");
            this.mHandler.obtainMessage(1, rawAppProbe).sendToTarget();
        }
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (AppProbeAction.class) {
            sInstance = null;
        }
    }

    protected boolean execute() {
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, this.mRawAppProbe).sendToTarget();
        return true;
    }

    public void enable() {
        super.enable();
        if (this.mHandler == null) {
            this.mHandler = new AppProbeHandler(this);
        }
    }

    public void disable() {
        super.disable();
        if (this.mHandler != null) {
            this.mHandler = null;
        }
    }

    public static void saveRawAppProbe(String pkgName, int eventID, String message) {
        AppProbeAction instance;
        synchronized (AppProbeAction.class) {
            instance = sInstance;
        }
        if (instance != null) {
            instance.initializeRawAppProbe(pkgName, eventID, message);
        }
    }
}
