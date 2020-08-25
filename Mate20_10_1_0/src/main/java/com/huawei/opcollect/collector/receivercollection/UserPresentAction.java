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

public class UserPresentAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final int MESSAGE_USER_PRESENT = 1;
    private static final String TAG = "UserPresentAction";
    private static UserPresentAction instance = null;
    /* access modifiers changed from: private */
    public Handler mHandler = null;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();

    private UserPresentAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_USER_PRESENT));
        OPCollectLog.r("UserPresentAction", "UserPresentAction");
    }

    public static UserPresentAction getInstance(Context context) {
        UserPresentAction userPresentAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new UserPresentAction("UserPresentAction", context);
            }
            userPresentAction = instance;
        }
        return userPresentAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        synchronized (this.mLock) {
            this.mHandler = new MyHandler(this);
        }
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new UserPresentBroadcastReceiver(this);
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.USER_PRESENT"), null, OdmfCollectScheduler.getInstance().getRecvHandler());
            OPCollectLog.r("UserPresentAction", "enabled");
        }
    }

    private static class UserPresentBroadcastReceiver extends BroadcastReceiver {
        private final WeakReference<UserPresentAction> service;

        UserPresentBroadcastReceiver(UserPresentAction service2) {
            this.service = new WeakReference<>(service2);
        }

        public void onReceive(Context context, Intent intent) {
            UserPresentAction userPresentAction;
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("UserPresentAction", "onReceive: " + action);
                if ("android.intent.action.USER_PRESENT".equalsIgnoreCase(action) && (userPresentAction = this.service.get()) != null) {
                    synchronized (userPresentAction.mLock) {
                        if (userPresentAction.mHandler != null) {
                            userPresentAction.mHandler.sendEmptyMessage(1);
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
        OPCollectLog.d("UserPresentAction", "execute");
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_USER_PRESENT);
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action, com.huawei.opcollect.collector.receivercollection.ReceiverAction
    public void disable() {
        super.disable();
        synchronized (this.mLock) {
            if (this.mHandler != null) {
                this.mHandler.removeMessages(1);
                this.mHandler = null;
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyUserPresentActionInstance();
        return true;
    }

    private static void destroyUserPresentActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<UserPresentAction> service;

        MyHandler(UserPresentAction service2) {
            this.service = new WeakReference<>(service2);
        }

        public void handleMessage(Message msg) {
            UserPresentAction action;
            super.handleMessage(msg);
            if (msg != null && (action = this.service.get()) != null) {
                switch (msg.what) {
                    case 1:
                        action.perform();
                        return;
                    default:
                        OPCollectLog.r("UserPresentAction", "wrong msg: " + msg.what);
                        return;
                }
            }
        }
    }
}
