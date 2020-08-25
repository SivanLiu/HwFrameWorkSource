package com.huawei.opcollect.collector.receivercollection;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class BluetoothStateAction extends ReceiverAction {
    private static final String BLE_ACTION = "android.bluetooth.adapter.action.STATE_CHANGED";
    private static final Object LOCK = new Object();
    private static final String TAG = "BluetoothStateAction";
    private static BluetoothStateAction instance = null;
    /* access modifiers changed from: private */
    public String mEventType = null;

    private BluetoothStateAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_BLUETOOTH_ON) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_BLUETOOTH_OFF));
    }

    public static BluetoothStateAction getInstance(Context context) {
        BluetoothStateAction bluetoothStateAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new BluetoothStateAction(context, "BluetoothStateAction");
            }
            bluetoothStateAction = instance;
        }
        return bluetoothStateAction;
    }

    class BluetoothStateBroadcastReceiver extends BroadcastReceiver {
        BluetoothStateBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("BluetoothStateAction", "onReceive action: " + action);
                if (BluetoothStateAction.BLE_ACTION.equals(action)) {
                    int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
                    if (state == 12) {
                        String unused = BluetoothStateAction.this.mEventType = SysEventUtil.EVENT_BLUETOOTH_ON;
                        BluetoothStateAction.this.perform();
                    } else if (state == 10) {
                        String unused2 = BluetoothStateAction.this.mEventType = SysEventUtil.EVENT_BLUETOOTH_OFF;
                        BluetoothStateAction.this.perform();
                    }
                }
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new BluetoothStateBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter(BLE_ACTION), "android.permission.BLUETOOTH", OdmfCollectScheduler.getInstance().getCtrlHandler());
            BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
            String value = SysEventUtil.OFF;
            if (blueAdapter != null && blueAdapter.getState() == 12) {
                value = SysEventUtil.ON;
            }
            SysEventUtil.collectKVSysEventData("device_connection/bluetooth_status", SysEventUtil.BLUETOOTH_STATUS, value);
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyBluetoothStateActionInstance();
        return true;
    }

    private static void destroyBluetoothStateActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        super.execute();
        if (TextUtils.isEmpty(this.mEventType)) {
            return false;
        }
        SysEventUtil.collectSysEventData(this.mEventType);
        SysEventUtil.collectKVSysEventData("device_connection/bluetooth_status", SysEventUtil.BLUETOOTH_STATUS, SysEventUtil.EVENT_BLUETOOTH_ON.equals(this.mEventType) ? SysEventUtil.ON : SysEventUtil.OFF);
        this.mEventType = null;
        return true;
    }
}
