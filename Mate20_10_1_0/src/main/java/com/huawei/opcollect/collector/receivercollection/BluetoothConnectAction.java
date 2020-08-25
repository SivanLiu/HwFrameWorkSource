package com.huawei.opcollect.collector.receivercollection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class BluetoothConnectAction extends ReceiverAction {
    private static final int DEVICE_CONNECTED = 1;
    private static final int DEVICE_DISCONNECTED = 0;
    private static final Object LOCK = new Object();
    private static final String TAG = "BluetoothConnectAction";
    private static BluetoothConnectAction instance = null;
    private String mBluetoothInfo = "";
    /* access modifiers changed from: private */
    public int mDeviceState = -1;
    private Map<Integer, String> mDeviceTypeList = null;

    private BluetoothConnectAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_BLUETOOTH_CONNECTED) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_BLUETOOTH_DISCONNECTED));
    }

    public static BluetoothConnectAction getInstance(Context context) {
        BluetoothConnectAction bluetoothConnectAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new BluetoothConnectAction(context, "BluetoothConnectAction");
            }
            bluetoothConnectAction = instance;
        }
        return bluetoothConnectAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        int state;
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new BluetoothBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
            intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, "android.permission.BLUETOOTH", OdmfCollectScheduler.getInstance().getCtrlHandler());
            BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
            String value = SysEventUtil.OFF;
            if (blueAdapter != null && ((state = blueAdapter.getState()) == 1 || state == 2)) {
                value = SysEventUtil.ON;
            }
            SysEventUtil.collectKVSysEventData("device_connection/bluetooth_connect_status", SysEventUtil.BLUETOOTH_CONNECT_STATUS, value);
        }
        initDeviceTypeMap();
    }

    class BluetoothBroadcastReceiver extends BroadcastReceiver {
        BluetoothBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && (action = intent.getAction()) != null) {
                OPCollectLog.r("BluetoothConnectAction", "onReceive");
                if ("android.bluetooth.device.action.ACL_CONNECTED".equals(action)) {
                    int unused = BluetoothConnectAction.this.mDeviceState = 1;
                } else if ("android.bluetooth.device.action.ACL_DISCONNECTED".equals(action)) {
                    int unused2 = BluetoothConnectAction.this.mDeviceState = 0;
                } else {
                    OPCollectLog.r("BluetoothConnectAction", "Unexpected Action");
                    return;
                }
                BluetoothConnectAction.this.saveBluetoothDeviceInfo((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE"));
                BluetoothConnectAction.this.perform();
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean perform() {
        return super.perform();
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mDeviceState == 1) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_BLUETOOTH_CONNECTED, this.mBluetoothInfo);
            SysEventUtil.collectKVSysEventData("device_connection/bluetooth_connect_status", SysEventUtil.BLUETOOTH_CONNECT_STATUS, SysEventUtil.ON);
        } else if (this.mDeviceState == 0) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_BLUETOOTH_DISCONNECTED, this.mBluetoothInfo);
            SysEventUtil.collectKVSysEventData("device_connection/bluetooth_connect_status", SysEventUtil.BLUETOOTH_CONNECT_STATUS, SysEventUtil.OFF);
        }
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        this.mDeviceTypeList = null;
        destroyBluetoothConnectActionInstance();
        return true;
    }

    private static void destroyBluetoothConnectActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    /* access modifiers changed from: private */
    public void saveBluetoothDeviceInfo(BluetoothDevice device) {
        JSONObject object = new JSONObject();
        if (device == null) {
            this.mBluetoothInfo = "";
            return;
        }
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass == null) {
            this.mBluetoothInfo = "";
            return;
        }
        String deviceType = getDeviceTypeString(bluetoothClass.getMajorDeviceClass());
        try {
            object.put("address", device.getAddress());
            object.put(OPCollectConstant.WIFI_NAME, device.getName());
            object.put("type", deviceType);
            this.mBluetoothInfo = object.toString();
        } catch (JSONException e) {
            this.mBluetoothInfo = "";
        }
    }

    private String getDeviceTypeString(int type) {
        if (this.mDeviceTypeList == null || type >= this.mDeviceTypeList.size() || type < 0) {
            return "";
        }
        return this.mDeviceTypeList.get(Integer.valueOf(type));
    }

    private void initDeviceTypeMap() {
        if (this.mDeviceTypeList == null) {
            this.mDeviceTypeList = new HashMap();
            this.mDeviceTypeList.put(0, "misc");
            this.mDeviceTypeList.put(256, "computer");
            this.mDeviceTypeList.put(512, "phone");
            this.mDeviceTypeList.put(768, "networking");
            this.mDeviceTypeList.put(1024, "audio_video");
            this.mDeviceTypeList.put(1280, "peripheral");
            this.mDeviceTypeList.put(1536, "imaging");
            this.mDeviceTypeList.put(1792, "wearable");
            this.mDeviceTypeList.put(Integer.valueOf((int) AwarenessConstants.HIACTION_EXPRESS_ACTION), "toy");
            this.mDeviceTypeList.put(2304, "health");
            this.mDeviceTypeList.put(7936, "uncategorized");
        }
    }
}
