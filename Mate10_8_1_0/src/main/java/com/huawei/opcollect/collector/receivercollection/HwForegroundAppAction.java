package com.huawei.opcollect.collector.receivercollection;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import com.huawei.nb.model.collectencrypt.RawFgAPPEvent;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import com.huawei.pgmng.plug.PGSdk;
import com.huawei.pgmng.plug.PGSdk.Sink;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Date;

public class HwForegroundAppAction extends Action {
    private static final int ACTION_STATE_BASE = 10000;
    private static final int EVENT_TYPE_EXIT = 2;
    private static final int RE_CONNECT_DELAY_TIME_SEC = 2000;
    private static final int RE_CONNECT_PG_SERVER = 1;
    private static final String TAG = "HwForegroundAppAction";
    private static HwForegroundAppAction sInstance = null;
    private Handler mHandler = null;
    private PGSdk mPGSdk = null;
    private String mPackageName = null;
    private MySystemStateListener mSystemStateListener;

    private static class MyHandler extends Handler {
        private final WeakReference<HwForegroundAppAction> service;

        MyHandler(HwForegroundAppAction action) {
            this.service = new WeakReference(action);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            HwForegroundAppAction action = (HwForegroundAppAction) this.service.get();
            if (action != null) {
                switch (msg.what) {
                    case 1:
                        OPCollectLog.r("HwForegroundAppAction", "msg: " + msg.what);
                        action.getPGSdk();
                        break;
                    default:
                        OPCollectLog.r("HwForegroundAppAction", "wrong msg: " + msg.what);
                        break;
                }
            }
        }
    }

    private static class MySystemStateListener implements Sink {
        private final WeakReference<HwForegroundAppAction> mService;

        MySystemStateListener(HwForegroundAppAction action) {
            this.mService = new WeakReference(action);
        }

        public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
            OPCollectLog.d("HwForegroundAppAction", "onStateChanged stateType=" + stateType + " eventType=" + eventType + ",pkg=" + pkg);
            if (eventType != 2) {
                HwForegroundAppAction action = (HwForegroundAppAction) this.mService.get();
                if (action != null) {
                    if (pkg == null || pkg.trim().isEmpty()) {
                        OPCollectLog.e("HwForegroundAppAction", "null == pkg || pkg.trim().isEmpty()");
                        return;
                    }
                    action.mPackageName = pkg;
                    action.perform();
                }
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
        super.enable();
        this.mHandler = new MyHandler(this);
        if (this.mSystemStateListener == null) {
            this.mSystemStateListener = new MySystemStateListener(this);
        }
        OPCollectLog.r("HwForegroundAppAction", "FG enable");
        getPGSdk();
    }

    private void getPGSdk() {
        OPCollectLog.r("HwForegroundAppAction", "getPGSdk.");
        if (this.mPGSdk != null) {
            if (this.mHandler != null) {
                this.mHandler.removeMessages(1);
            }
            return;
        }
        this.mPGSdk = PGSdk.getInstance();
        if (this.mPGSdk == null) {
            OPCollectLog.e("HwForegroundAppAction", "connect to PGSDK server failed.");
            if (this.mHandler != null) {
                this.mHandler.sendEmptyMessageDelayed(1, 2000);
            }
            return;
        }
        if (this.mHandler != null) {
            this.mHandler.removeMessages(1);
        }
        callPGregisterListener();
    }

    private void callPGregisterListener() {
        if (this.mPGSdk == null) {
            OPCollectLog.e("HwForegroundAppAction", "PGServer is null.");
            return;
        }
        try {
            OPCollectLog.r("HwForegroundAppAction", "FG callPGregisterListener");
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, ACTION_STATE_BASE);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10002);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10011);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10001);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10003);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10004);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10007);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10008);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10009);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10010);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10013);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10015);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10016);
            this.mPGSdk.enableStateEvent(this.mSystemStateListener, 10017);
        } catch (RemoteException e) {
            this.mPGSdk = null;
            OPCollectLog.e("HwForegroundAppAction", "mPGSdk registerSink happend RemoteException ");
        }
    }

    private void callPGunRegisterListener() {
        OPCollectLog.r("HwForegroundAppAction", "callPGunRegisterListener.");
        if (this.mPGSdk != null) {
            try {
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, ACTION_STATE_BASE);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10002);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10011);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10001);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10003);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10004);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10007);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10008);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10009);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10010);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10013);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10015);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10016);
                this.mPGSdk.disableStateEvent(this.mSystemStateListener, 10017);
            } catch (RemoteException e) {
                OPCollectLog.e("HwForegroundAppAction", "callPG unRegisterListener  happend RemoteException ");
            }
        }
    }

    protected boolean execute() {
        OPCollectLog.d("HwForegroundAppAction", "FG execute");
        RawFgAPPEvent rawFgAPPEvent = new RawFgAPPEvent();
        rawFgAPPEvent.setMPackageName(this.mPackageName);
        rawFgAPPEvent.setMTimeStamp(new Date());
        rawFgAPPEvent.setMReservedText(OPCollectUtils.formatCurrentTime());
        rawFgAPPEvent.setMStatus(Integer.valueOf(1));
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawFgAPPEvent).sendToTarget();
        this.mPackageName = null;
        return true;
    }

    public boolean perform() {
        return super.perform();
    }

    public void disable() {
        super.disable();
        OPCollectLog.r("HwForegroundAppAction", "FG disable");
        if (this.mPGSdk != null) {
            callPGunRegisterListener();
            this.mPGSdk = null;
        }
        if (this.mSystemStateListener != null) {
            this.mSystemStateListener = null;
        }
        if (this.mHandler != null) {
            this.mHandler.removeMessages(1);
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

    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
        if (pw != null) {
            String indent = String.format("%" + indentNum + "s\\-", new Object[]{" "});
            if (this.mPGSdk == null) {
                pw.println(indent + "PGSdk is null");
            } else {
                pw.println(indent + "PGSdk not null");
            }
            if (this.mSystemStateListener == null) {
                pw.println(indent + "listener is null");
            } else {
                pw.println(indent + "listener not null");
            }
        }
    }
}
