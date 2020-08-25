package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;
import java.lang.ref.WeakReference;

public class ScreenOnAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final int MESSAGE_SCREEN_ON = 1;
    private static final String TAG = "ScreenOnAction";
    private static ScreenOnAction instance = null;
    /* access modifiers changed from: private */
    public Handler mHandler = null;

    private ScreenOnAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_SCREEN_ON));
        OPCollectLog.r("ScreenOnAction", "ScreenOnAction");
    }

    public static ScreenOnAction getInstance(Context context) {
        ScreenOnAction screenOnAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new ScreenOnAction("ScreenOnAction", context);
            }
            screenOnAction = instance;
        }
        return screenOnAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        synchronized (this) {
            this.mHandler = new MyHandler(this);
        }
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new ScreenOnBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SCREEN_ON"), null, OdmfCollectScheduler.getInstance().getRecvHandler());
            OPCollectLog.r("ScreenOnAction", "enabled");
        }
    }

    class ScreenOnBroadcastReceiver extends BroadcastReceiver {
        ScreenOnBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("ScreenOnAction", "onReceive: " + action);
                if ("android.intent.action.SCREEN_ON".equalsIgnoreCase(action)) {
                    synchronized (ScreenOnAction.this) {
                        if (ScreenOnAction.this.mHandler != null) {
                            ScreenOnAction.this.mHandler.sendEmptyMessage(1);
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        super.execute();
        OPCollectLog.d("ScreenOnAction", "EVENT_SCREEN_ON");
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_SCREEN_ON);
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action, com.huawei.opcollect.collector.receivercollection.ReceiverAction
    public void disable() {
        super.disable();
        synchronized (this) {
            if (this.mHandler != null) {
                this.mHandler.removeMessages(1);
                this.mHandler = null;
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyScreenOnActionInstance();
        return true;
    }

    private static void destroyScreenOnActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<ScreenOnAction> service;

        MyHandler(ScreenOnAction service2) {
            this.service = new WeakReference<>(service2);
        }

        public void handleMessage(Message msg) {
            ScreenOnAction action;
            super.handleMessage(msg);
            if (msg != null && (action = this.service.get()) != null) {
                switch (msg.what) {
                    case 1:
                        action.perform();
                        return;
                    default:
                        OPCollectLog.r("ScreenOnAction", "wrong msg: " + msg.what);
                        return;
                }
            }
        }
    }
}
