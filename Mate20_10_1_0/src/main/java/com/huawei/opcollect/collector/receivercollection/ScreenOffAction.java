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

public class ScreenOffAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final int MESSAGE_SCREEN_OFF = 1;
    private static final String TAG = "ScreenOffAction";
    private static ScreenOffAction instance = null;
    /* access modifiers changed from: private */
    public Handler mHandler = null;

    private ScreenOffAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_SCREEN_OFF));
        OPCollectLog.r("ScreenOffAction", "ScreenOffAction");
    }

    public static ScreenOffAction getInstance(Context context) {
        ScreenOffAction screenOffAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new ScreenOffAction("ScreenOffAction", context);
            }
            screenOffAction = instance;
        }
        return screenOffAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        synchronized (this) {
            this.mHandler = new MyHandler(this);
        }
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new ScreenOffBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"), null, OdmfCollectScheduler.getInstance().getRecvHandler());
            OPCollectLog.r("ScreenOffAction", "enabled");
        }
    }

    class ScreenOffBroadcastReceiver extends BroadcastReceiver {
        ScreenOffBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                OPCollectLog.r("ScreenOffAction", "onReceive: " + intent.getAction());
                if ("android.intent.action.SCREEN_OFF".equalsIgnoreCase(intent.getAction())) {
                    synchronized (ScreenOffAction.this) {
                        if (ScreenOffAction.this.mHandler != null) {
                            ScreenOffAction.this.mHandler.sendEmptyMessage(1);
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
        OPCollectLog.d("ScreenOffAction", "EVENT_SCREEN_OFF");
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_SCREEN_OFF);
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
        destroyScreenOffActionInstance();
        return true;
    }

    private static void destroyScreenOffActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<ScreenOffAction> service;

        MyHandler(ScreenOffAction service2) {
            this.service = new WeakReference<>(service2);
        }

        public void handleMessage(Message msg) {
            ScreenOffAction action;
            super.handleMessage(msg);
            if (msg != null && (action = this.service.get()) != null) {
                switch (msg.what) {
                    case 1:
                        action.perform();
                        return;
                    default:
                        OPCollectLog.r("ScreenOffAction", "wrong msg: " + msg.what);
                        return;
                }
            }
        }
    }
}
