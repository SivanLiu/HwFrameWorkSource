package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class HeadsetPlugStateAction extends ReceiverAction {
    private static final int HEADSET_PLUGGED = 1;
    private static final int HEADSET_UNPLUGGED = 0;
    private static final Object LOCK = new Object();
    private static final String TAG = "HeadsetPlugStateAction";
    private static HeadsetPlugStateAction instance = null;
    /* access modifiers changed from: private */
    public int mState = -1;

    private HeadsetPlugStateAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_HEADSET_PLUG) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_HEADSET_UNPLUG));
        OPCollectLog.r("HeadsetPlugStateAction", "HeadsetPlugStateAction");
    }

    public static HeadsetPlugStateAction getInstance(Context context) {
        HeadsetPlugStateAction headsetPlugStateAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new HeadsetPlugStateAction(context, "HeadsetPlugStateAction");
            }
            headsetPlugStateAction = instance;
        }
        return headsetPlugStateAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new HeadSetStateReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.HEADSET_PLUG");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            SysEventUtil.collectKVSysEventData("sound/headset_connect_status", SysEventUtil.HEADSET_CONNECT_STATUS, SysEventUtil.OFF);
            OPCollectLog.r("HeadsetPlugStateAction", "enabled");
        }
    }

    class HeadSetStateReceiver extends BroadcastReceiver {
        HeadSetStateReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && (action = intent.getAction()) != null) {
                OPCollectLog.r("HeadsetPlugStateAction", "onReceive action: " + action);
                if ("android.intent.action.HEADSET_PLUG".equals(action) && intent.hasExtra("state")) {
                    int unused = HeadsetPlugStateAction.this.mState = intent.getIntExtra("state", -1);
                    HeadsetPlugStateAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mState == 1) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_HEADSET_PLUG);
            SysEventUtil.collectKVSysEventData("sound/headset_connect_status", SysEventUtil.HEADSET_CONNECT_STATUS, SysEventUtil.ON);
        } else if (this.mState == 0) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_HEADSET_UNPLUG);
            SysEventUtil.collectKVSysEventData("sound/headset_connect_status", SysEventUtil.HEADSET_CONNECT_STATUS, SysEventUtil.OFF);
        }
        this.mState = -1;
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyHeadsetPlugStateActionInstance();
        return true;
    }

    private static void destroyHeadsetPlugStateActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
