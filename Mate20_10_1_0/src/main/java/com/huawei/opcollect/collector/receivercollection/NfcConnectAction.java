package com.huawei.opcollect.collector.receivercollection;

import android.content.Context;
import com.huawei.opcollect.strategy.Action;

public class NfcConnectAction extends Action {
    private static final Object LOCK = new Object();
    private static final String TAG = "NfcConnectAction";
    private static NfcConnectAction instance = null;

    private NfcConnectAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.NFC_CONNECTION_STATUS));
    }

    public static NfcConnectAction getInstance(Context context) {
        NfcConnectAction nfcConnectAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new NfcConnectAction(SysEventUtil.NFC_CONNECTION_STATUS, context);
            }
            nfcConnectAction = instance;
        }
        return nfcConnectAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyNfcConnectActionInstance();
        return true;
    }

    private static void destroyNfcConnectActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
