package com.android.server.rms.iaware.dev;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.rms.iaware.dev.DevSchedFeatureBase.ScreenState;
import com.android.server.rms.iaware.memory.data.content.AttrSegments;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BLESchedFeatureRT extends DevSchedFeatureBase {
    private static final int BLE_MODE_FIRST = -1;
    private static final int BLE_MODE_LAST = 3;
    private static final int BLE_MODE_LOW = 1;
    private static final int BLE_MODE_NORAML = 2;
    private static final int BLE_MODE_STOP = 0;
    private static final String DEVICE_NAME = "ble_iconnect_nearby";
    private static final int INVALID_MODE = -2;
    private static final String ITEM_MODE = "mode";
    private static final String TAG = "BLESchedFeatureRT";
    private static final List<String> mExceptAppList = new ArrayList();
    private static final List<SceneInfo> mSceneList = new ArrayList();
    private AtomicBoolean mBleEnable = new AtomicBoolean(false);
    private final BroadcastReceiver mBleReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                AwareLog.e(BLESchedFeatureRT.TAG, "intent is null");
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                AwareLog.e(BLESchedFeatureRT.TAG, "action is null");
                return;
            }
            if (action.equals("android.bluetooth.adapter.action.BLE_STATE_CHANGED")) {
                int newState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 10);
                String str = BLESchedFeatureRT.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("newState: ");
                stringBuilder.append(newState);
                AwareLog.d(str, stringBuilder.toString());
                if (10 == newState) {
                    BLESchedFeatureRT.this.mBleEnable.set(false);
                    BLESchedFeatureRT.this.removeBleControl();
                } else if (12 == newState) {
                    BLESchedFeatureRT.this.mBleEnable.set(true);
                }
                String str2 = BLESchedFeatureRT.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mBleEnable: ");
                stringBuilder2.append(BLESchedFeatureRT.this.mBleEnable.get());
                AwareLog.d(str2, stringBuilder2.toString());
            }
        }
    };
    private Context mContext;
    private final DevXmlConfig mDevXmlConfig = new DevXmlConfig();

    public BLESchedFeatureRT(Context context, String name) {
        super(context);
        this.mContext = context;
        readCustConfig();
        init();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("create ");
        stringBuilder.append(name);
        stringBuilder.append("BLESchedFeatureRT success");
        AwareLog.d(str, stringBuilder.toString());
    }

    private void init() {
        if (this.mContext == null) {
            AwareLog.e(TAG, "mContext is null, error!");
            return;
        }
        this.mContext.registerReceiver(this.mBleReceiver, new IntentFilter("android.bluetooth.adapter.action.BLE_STATE_CHANGED"));
        BluetoothManager bleManager = (BluetoothManager) this.mContext.getSystemService("bluetooth");
        if (bleManager == null) {
            AwareLog.e(TAG, "bleManager is null, error!");
            return;
        }
        BluetoothAdapter bleAdapter = bleManager.getAdapter();
        if (bleAdapter == null) {
            AwareLog.e(TAG, "bleAdapter is null, error!");
        } else {
            this.mBleEnable.set(bleAdapter.isEnabled());
        }
    }

    private void readCustConfig() {
        this.mDevXmlConfig.readSceneInfos(DEVICE_NAME, mSceneList);
        this.mDevXmlConfig.readExceptApps(DEVICE_NAME, mExceptAppList);
        this.mDeviceId = this.mDevXmlConfig.readDeviceId(DEVICE_NAME);
    }

    public boolean handleResAppData(long timestamp, int event, AttrSegments attrSegments) {
        if (event != 15020) {
            return false;
        }
        if (!this.mBleEnable.get()) {
            AwareLog.d(TAG, "ble off, return!");
            return false;
        } else if (ScreenState.ScreenOff == mScreenState) {
            AwareLog.d(TAG, "screen off, return!");
            return false;
        } else {
            handleAppToTopEvent(attrSegments, event);
            return true;
        }
    }

    public boolean handScreenStateChange(ScreenState state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mScreenState : ");
        stringBuilder.append(mScreenState);
        AwareLog.d(str, stringBuilder.toString());
        if (state == ScreenState.ScreenOff) {
            removeBleControl();
        } else if (state == ScreenState.ScreenOn) {
            sendCurrentDeviceMode();
        }
        return true;
    }

    private void handleAppToTopEvent(AttrSegments attrSegments, int event) {
        if (attrSegments == null || !attrSegments.isValid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("attrSegments is Illegal, attrSegments is ");
            stringBuilder.append(attrSegments);
            AwareLog.e(str, stringBuilder.toString());
            return;
        }
        ArrayMap<String, String> appInfo = attrSegments.getSegment("calledApp");
        if (appInfo == null) {
            AwareLog.i(TAG, "appInfo is NULL");
            return;
        }
        try {
            handleTopApp((String) appInfo.get("processName"), Integer.parseInt((String) appInfo.get("uid")), Integer.parseInt((String) appInfo.get("pid")));
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "get uid or pid fail, happend NumberFormatException");
        }
    }

    private void handleTopApp(String processName, int uid, int pid) {
        String str;
        StringBuilder stringBuilder;
        if (processName == null || pid < 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("invalid appinfo, processName :");
            stringBuilder.append(processName);
            stringBuilder.append(", uid :");
            stringBuilder.append(uid);
            stringBuilder.append(", pid :");
            stringBuilder.append(pid);
            AwareLog.i(str, stringBuilder.toString());
        } else if (mExceptAppList.contains(processName)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(processName);
            stringBuilder.append(" in exceptList, out of control.");
            AwareLog.i(str, stringBuilder.toString());
        } else {
            SceneInfo scene = DevSchedUtil.getSceneInfo(15020, mSceneList);
            if (scene == null) {
                AwareLog.i(TAG, "no EVENT_APP_TO_TOP object.");
                return;
            }
            int index = scene.isMatch(new Object[]{processName, Integer.valueOf(uid), Integer.valueOf(pid)});
            if (index >= 0) {
                int mode = getMode(scene, index);
                String str2;
                StringBuilder stringBuilder2;
                if (mode <= -1 || mode >= 3) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getMode error! mode:");
                    stringBuilder2.append(mode);
                    AwareLog.e(str2, stringBuilder2.toString());
                    return;
                }
                DevSchedCallbackManager.getInstance().sendDeviceMode(this.mDeviceId, processName, uid, mode, null);
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sendDeviceMode,  packageName:");
                stringBuilder2.append(processName);
                stringBuilder2.append(", uid:");
                stringBuilder2.append(uid);
                stringBuilder2.append(", mode:");
                stringBuilder2.append(mode);
                AwareLog.d(str2, stringBuilder2.toString());
            } else {
                removeBleControl();
            }
        }
    }

    private int getMode(SceneInfo scene, int index) {
        if (scene == null) {
            AwareLog.e(TAG, "scene is null, error!");
            return -2;
        }
        String modeOrg = scene.getRuleItemValue("mode", index);
        if (modeOrg == null) {
            AwareLog.e(TAG, "mode is null, error!");
            return -2;
        }
        int mode = -2;
        try {
            return Integer.parseInt(modeOrg.trim());
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mode is not Integer, error, mode:");
            stringBuilder.append(modeOrg);
            AwareLog.e(str, stringBuilder.toString());
            return -2;
        }
    }

    public void sendCurrentDeviceMode() {
        if (!this.mBleEnable.get()) {
            AwareLog.d(TAG, "ble off, return!");
        } else if (ScreenState.ScreenOff == mScreenState) {
            AwareLog.d(TAG, "screen off, return!");
        } else {
            String topApp = DevSchedUtil.getTopFrontApp(this.mContext);
            if (topApp == null) {
                AwareLog.i(TAG, "topApp is null.");
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendCurrentDeviceMode, topApp:");
            stringBuilder.append(topApp);
            AwareLog.d(str, stringBuilder.toString());
            handleTopApp(topApp, DevSchedUtil.getUidByPkgName(topApp), 0);
        }
    }

    public boolean handleUpdateCustConfig() {
        removeBleControl();
        clearCacheInfo();
        readCustConfig();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ble cust config update completed, mDeviceId:");
        stringBuilder.append(this.mDeviceId);
        stringBuilder.append(", mSceneList :");
        stringBuilder.append(mSceneList);
        stringBuilder.append(", mExceptAppList :");
        stringBuilder.append(mExceptAppList);
        AwareLog.d(str, stringBuilder.toString());
        return true;
    }

    private void removeBleControl() {
        DevSchedCallbackManager.getInstance().sendDeviceMode(this.mDeviceId, null, 0, 2, null);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeBleControl,  mDeviceId:");
        stringBuilder.append(this.mDeviceId);
        AwareLog.d(str, stringBuilder.toString());
    }

    private void clearCacheInfo() {
        mSceneList.clear();
        mExceptAppList.clear();
    }

    public boolean handlerNaviStatus(boolean isInNavi) {
        return true;
    }
}
