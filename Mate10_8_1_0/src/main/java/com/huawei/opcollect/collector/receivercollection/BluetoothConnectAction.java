package com.huawei.opcollect.collector.receivercollection;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.EventIdConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class BluetoothConnectAction extends Action {
    private static final int DEVICE_CONNECTED = 1;
    private static final int DEVICE_DISCONNECTED = 0;
    private static final String TAG = "BluetoothConnectAction";
    private static BluetoothConnectAction sInstance = null;
    private String mBluetoothInfo = EventIdConstant.PURPOSE_STR_BLANK;
    private int mDeviceState = -1;
    private Map<Integer, String> mDeviceTypeList = null;
    private BluetoothBroadcastReceiver mReceiver = null;

    class BluetoothBroadcastReceiver extends BroadcastReceiver {
        BluetoothBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    OPCollectLog.r("BluetoothConnectAction", "onReceive");
                    if ("android.bluetooth.device.action.ACL_CONNECTED".equals(action)) {
                        BluetoothConnectAction.this.mDeviceState = 1;
                    } else if ("android.bluetooth.device.action.ACL_DISCONNECTED".equals(action)) {
                        BluetoothConnectAction.this.mDeviceState = 0;
                    } else {
                        OPCollectLog.r("BluetoothConnectAction", "Unexpected Action");
                        return;
                    }
                    BluetoothConnectAction.this.saveBluetoothDeviceInfo((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE"));
                    BluetoothConnectAction.this.perform();
                }
            }
        }
    }

    public static synchronized BluetoothConnectAction getInstance(Context context) {
        BluetoothConnectAction bluetoothConnectAction;
        synchronized (BluetoothConnectAction.class) {
            if (sInstance == null) {
                sInstance = new BluetoothConnectAction(context, "BluetoothConnectAction");
            }
            bluetoothConnectAction = sInstance;
        }
        return bluetoothConnectAction;
    }

    private BluetoothConnectAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_BLUETOOTH_CONNECTED) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_BLUETOOTH_DISCONNECTED));
    }

    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new BluetoothBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
            intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
        }
        initDeviceTypeMap();
    }

    protected boolean execute() {
        if (1 == this.mDeviceState) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_BLUETOOTH_CONNECTED, this.mBluetoothInfo);
        } else if (this.mDeviceState == 0) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_BLUETOOTH_DISCONNECTED, this.mBluetoothInfo);
        }
        return true;
    }

    public boolean perform() {
        return super.perform();
    }

    public void disable() {
        super.disable();
        if (this.mReceiver != null && this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
    }

    public boolean destroy() {
        super.destroy();
        this.mDeviceTypeList = null;
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (BluetoothConnectAction.class) {
            sInstance = null;
        }
    }

    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
        if (pw != null) {
            String indent = String.format("%" + indentNum + "s\\-", new Object[]{" "});
            if (this.mReceiver == null) {
                pw.println(indent + "receiver is null");
            } else {
                pw.println(indent + "receiver not null");
            }
        }
    }

    private void saveBluetoothDeviceInfo(BluetoothDevice device) {
        JSONObject object = new JSONObject();
        if (device == null) {
            this.mBluetoothInfo = EventIdConstant.PURPOSE_STR_BLANK;
            return;
        }
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass == null) {
            this.mBluetoothInfo = EventIdConstant.PURPOSE_STR_BLANK;
            return;
        }
        String deviceType = getDeviceTypeString(bluetoothClass.getMajorDeviceClass());
        try {
            object.put("address", device.getAddress());
            object.put("name", device.getName());
            object.put("type", deviceType);
            this.mBluetoothInfo = object.toString();
        } catch (JSONException e) {
            this.mBluetoothInfo = EventIdConstant.PURPOSE_STR_BLANK;
        }
    }

    private String getDeviceTypeString(int type) {
        String typeStr = EventIdConstant.PURPOSE_STR_BLANK;
        if (this.mDeviceTypeList == null || type >= this.mDeviceTypeList.size() || type < 0) {
            return typeStr;
        }
        return (String) this.mDeviceTypeList.get(Integer.valueOf(type));
    }

    private void initDeviceTypeMap() {
        if (this.mDeviceTypeList == null) {
            this.mDeviceTypeList = new HashMap();
            this.mDeviceTypeList.put(Integer.valueOf(0), "misc");
            this.mDeviceTypeList.put(Integer.valueOf(256), "computer");
            this.mDeviceTypeList.put(Integer.valueOf(512), "phone");
            this.mDeviceTypeList.put(Integer.valueOf(768), "networking");
            this.mDeviceTypeList.put(Integer.valueOf(1024), "audio_video");
            this.mDeviceTypeList.put(Integer.valueOf(1280), "peripheral");
            this.mDeviceTypeList.put(Integer.valueOf(1536), "imaging");
            this.mDeviceTypeList.put(Integer.valueOf(1792), "wearable");
            this.mDeviceTypeList.put(Integer.valueOf(2048), "toy");
            this.mDeviceTypeList.put(Integer.valueOf(2304), "health");
            this.mDeviceTypeList.put(Integer.valueOf(7936), "uncategorized");
        }
    }
}
