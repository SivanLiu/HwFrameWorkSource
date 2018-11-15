package com.huawei.opcollect.collector.probecollection;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import com.huawei.nb.model.collectencrypt.RawAppProbeCount;
import com.huawei.nb.query.Query;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.EventIdConstant;
import com.huawei.opcollect.utils.OPCollectConstant;
import java.lang.ref.WeakReference;
import java.util.List;

public class AppProbeCountAction extends Action {
    private static final int MESSAGE_ON_CHANGE = 1;
    private static AppProbeCountAction sInstance;
    private Handler mHandler = null;
    private RawAppProbeCount mRawAppProbeCount = null;

    private static class AppProbeCountHandler extends Handler {
        private final WeakReference<AppProbeCountAction> service;

        AppProbeCountHandler(AppProbeCountAction service) {
            this.service = new WeakReference(service);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                AppProbeCountAction action = (AppProbeCountAction) this.service.get();
                if (action != null) {
                    action.mRawAppProbeCount = (RawAppProbeCount) msg.obj;
                    action.perform();
                }
            }
        }
    }

    public static synchronized AppProbeCountAction getInstance(Context context) {
        AppProbeCountAction appProbeCountAction;
        synchronized (AppProbeCountAction.class) {
            if (sInstance == null) {
                sInstance = new AppProbeCountAction(context, OPCollectConstant.APP_PROBE_COUNT_ACTION_NAME);
            }
            appProbeCountAction = sInstance;
        }
        return appProbeCountAction;
    }

    private AppProbeCountAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(queryDailyRecordNum(RawAppProbeCount.class));
    }

    private void initializeRawAppProbe(String pkgName, int eventID, int count) {
        if (this.mHandler != null) {
            RawAppProbeCount rawAppProbeCount = new RawAppProbeCount();
            rawAppProbeCount.setMCount(Integer.valueOf(count));
            rawAppProbeCount.setMEventID(Integer.valueOf(eventID));
            rawAppProbeCount.setMPackageName(pkgName);
            rawAppProbeCount.setMReservedInt(Integer.valueOf(0));
            rawAppProbeCount.setMReservedText(EventIdConstant.PURPOSE_STR_BLANK);
            this.mHandler.obtainMessage(1, rawAppProbeCount).sendToTarget();
        }
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (AppProbeCountAction.class) {
            sInstance = null;
        }
    }

    protected boolean execute() {
        if (this.mRawAppProbeCount == null) {
            return false;
        }
        List<AManagedObject> lists = OdmfCollectScheduler.getInstance().getOdmfHelper().queryManageObject(Query.select(RawAppProbeCount.class).equalTo("mEventID", this.mRawAppProbeCount.getMEventID()).equalTo("mPackageName", this.mRawAppProbeCount.getMPackageName()));
        if (lists == null || lists.size() == 0) {
            OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, this.mRawAppProbeCount).sendToTarget();
        } else {
            this.mRawAppProbeCount.setMCount(Integer.valueOf(((RawAppProbeCount) lists.get(0)).getMCount().intValue() + this.mRawAppProbeCount.getMCount().intValue()));
            OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(5, this.mRawAppProbeCount).sendToTarget();
        }
        return true;
    }

    public void enable() {
        super.enable();
        if (this.mHandler == null) {
            this.mHandler = new AppProbeCountHandler(this);
        }
    }

    public void disable() {
        super.disable();
        if (this.mHandler != null) {
            this.mHandler = null;
        }
    }

    public static void saveRawAppProbeCount(String pkgName, int eventID, int count) {
        synchronized (AppProbeCountAction.class) {
            AppProbeCountAction instance = sInstance;
        }
        if (instance != null) {
            instance.initializeRawAppProbe(pkgName, eventID, count);
        }
    }
}
