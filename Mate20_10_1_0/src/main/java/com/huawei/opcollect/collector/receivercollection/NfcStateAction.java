package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.AbsActionParam;
import com.huawei.opcollect.utils.OPCollectLog;

public class NfcStateAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "NfcStateAction";
    private static NfcStateAction instance = null;

    private NfcStateAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.NFC_STATUS));
    }

    public static NfcStateAction getInstance(Context context) {
        NfcStateAction nfcStateAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new NfcStateAction(SysEventUtil.NFC_STATUS, context);
            }
            nfcStateAction = instance;
        }
        return nfcStateAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new NfcStateReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED"), null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this.mContext);
            SysEventUtil.collectKVSysEventData("device_connection/nfc_status", SysEventUtil.NFC_STATUS, (nfcAdapter == null || !nfcAdapter.isEnabled()) ? SysEventUtil.OFF : SysEventUtil.ON);
            OPCollectLog.r(TAG, "enabled");
        }
    }

    class NfcStateReceiver extends BroadcastReceiver {
        NfcStateReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r(NfcStateAction.TAG, "onReceive action: " + action);
                if ("android.nfc.action.ADAPTER_STATE_CHANGED".equalsIgnoreCase(action)) {
                    int state = intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 1);
                    if (state == 1) {
                        boolean unused = NfcStateAction.this.performWithArgs(new NfcStateActionParam(SysEventUtil.OFF));
                    } else if (state == 3) {
                        boolean unused2 = NfcStateAction.this.performWithArgs(new NfcStateActionParam(SysEventUtil.ON));
                    }
                }
            }
        }
    }

    private class NfcStateActionParam extends AbsActionParam {
        private String state;

        NfcStateActionParam(String state2) {
            this.state = state2;
        }

        /* access modifiers changed from: package-private */
        public String getState() {
            return this.state;
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean executeWithArgs(AbsActionParam absActionParam) {
        if (absActionParam == null) {
            return true;
        }
        SysEventUtil.collectSysEventData(SysEventUtil.NFC_STATUS, ((NfcStateActionParam) absActionParam).getState());
        SysEventUtil.collectKVSysEventData("device_connection/nfc_status", SysEventUtil.NFC_STATUS, ((NfcStateActionParam) absActionParam).getState());
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyNfcStateActionInstance();
        return true;
    }

    private static void destroyNfcStateActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
