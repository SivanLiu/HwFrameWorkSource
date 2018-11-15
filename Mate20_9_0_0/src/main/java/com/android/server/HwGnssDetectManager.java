package com.android.server;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationRequest;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.Log;
import com.android.server.LocationManagerService.Receiver;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import java.util.ArrayList;
import java.util.HashMap;

public class HwGnssDetectManager {
    private static long DETECT_INTERVAL = AppHibernateCst.DELAY_ONE_MINS;
    private static String GPS_FAULT_CODE = "631001001";
    private static String GPS_FAULT_DESCRIPTION_CODE = "831001001";
    private static String GPS_FAULT_NAME = "GPS_Switch_Fault";
    private static String GPS_FAULT_SUGGESTION_CODE = "531001001";
    private static String GPS_HANDLE_TYPE = "3";
    private static String KEY_EXTRA_INFO = "EXTRA_INFO";
    private static String KEY_FAULT_CODE = "FAULT_CODE";
    private static String KEY_FAULT_DESCRIPTION = "FAULT_DESCRIPTION";
    private static String KEY_FAULT_NAME = "FAULT_NAME";
    private static String KEY_FAULT_SUGGESTION = "FAULT_SUGGESTION";
    private static String KEY_HANDLE_TYPE = "HANDLE_TYPE";
    private static String KEY_REPAIR_ID = "REPAIR_ID";
    private static String SMART_FAULT_ACTION = "huawei.intent.action.SMART_NOTIFY_FAULT";
    private static final String SMART_FAULT_CLASS_NAME = "com.huawei.hwdetectrepair.smartnotify.eventlistener.InstantMessageReceiver";
    private static final String SMART_FAULT_PACKAGE_NAME = "com.huawei.hwdetectrepair";
    private static final String SMART_FAULT_PERMISSION = "huawei.permission.SMART_NOTIFY_FAULT";
    private static String TAG = "HwGnssDetectManager";
    private static volatile HwGnssDetectManager mHwGnssDetectManager;
    private Context mContext;
    private HashMap<String, Long> mLastDetectTimeOfPkgs = new HashMap();

    private static class GPSREPAIRIDS {
        static String REPAIR_SETTING_GPS_MODE_SWITCH_VALUE1 = "REPAIR_SETTING_GPS_MODE_SWITCH_VALUE1";
        static String REPAIR_SETTING_GPS_MODE_SWITCH_VALUE2 = "REPAIR_SETTING_GPS_MODE_SWITCH_VALUE2";
        static String REPAIR_SETTING_GPS_MODE_SWITCH_VALUE3 = "REPAIR_SETTING_GPS_MODE_SWITCH_VALUE3";
        static String REPAIR_SETTING_GPS_SWITCH_OFF = "REPAIR_SETTING_GPS_SWITCH_OFF";
        static String REPAIR_SETTING_GPS_SWITCH_ON = "REPAIR_SETTING_GPS_SWITCH_ON";
        static String REPAIR_SETTING_WLAN_SCAN_SWITCH_OFF = "REPAIR_SETTING_WLAN_SCAN_SWITCH_OFF";
        static String REPAIR_SETTING_WLAN_SCAN_SWITCH_ON = "REPAIR_SETTING_WLAN_SCAN_SWITCH_ON";

        private GPSREPAIRIDS() {
        }
    }

    public static HwGnssDetectManager getInstance(Context context) {
        if (mHwGnssDetectManager == null) {
            synchronized (HwGnssDetectManager.class) {
                if (mHwGnssDetectManager == null) {
                    mHwGnssDetectManager = new HwGnssDetectManager(context);
                }
            }
        }
        return mHwGnssDetectManager;
    }

    private HwGnssDetectManager(Context context) {
        this.mContext = context;
    }

    public void hwRequestLocationUpdatesLocked(LocationRequest request, Receiver receiver, int pid, int uid, String packageName) {
        if (shouldDetect(request, receiver, pid, uid, packageName)) {
            broadcastDetect(packageName, getCommonDetect());
        }
    }

    public ArrayList<String> gnssDetect(String packageName) {
        return getCommonDetect();
    }

    private boolean shouldDetect(LocationRequest request, Receiver receiver, int pid, int uid, String packageName) {
        String str = packageName;
        if ("passive".equals(request.getProvider())) {
            return false;
        }
        if (!((PowerManager) this.mContext.getSystemService("power")).isScreenOn()) {
            Log.d(TAG, "now is not screen on, need not detect!");
            return false;
        } else if (isForeGroundProc(str)) {
            long now = SystemClock.elapsedRealtime();
            Long lastDetectTime = (Long) this.mLastDetectTimeOfPkgs.get(str);
            if (lastDetectTime != null && now - lastDetectTime.longValue() < DETECT_INTERVAL) {
                return false;
            }
            this.mLastDetectTimeOfPkgs.put(str, Long.valueOf(now));
            return true;
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("package ");
            stringBuilder.append(str);
            stringBuilder.append(" is not foreground, need not detect!");
            Log.d(str2, stringBuilder.toString());
            return false;
        }
    }

    private ArrayList<String> getCommonDetect() {
        boolean isLocationEnabled;
        boolean isHighAcc = false;
        ArrayList<String> commonDetectedRepairIds = new ArrayList();
        boolean z = false;
        int locationMode = Secure.getInt(this.mContext.getContentResolver(), "location_mode", 0);
        if (locationMode == 0) {
            isLocationEnabled = false;
        } else {
            isLocationEnabled = true;
        }
        if (locationMode == 3) {
            isHighAcc = true;
        }
        if (Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) == 1) {
            z = true;
        }
        boolean isWifiScanOpen = z;
        StringBuilder sb = new StringBuilder();
        sb.append("GPS common detect ");
        sb.append(" isLocationEnabled ");
        sb.append(isLocationEnabled);
        sb.append(" locationMode ");
        sb.append(locationMode);
        sb.append(" isHighAcc ");
        sb.append(isHighAcc);
        sb.append(" isWifiScanOpen ");
        sb.append(isWifiScanOpen);
        Log.d(TAG, sb.toString());
        if (!isLocationEnabled) {
            commonDetectedRepairIds.add(GPSREPAIRIDS.REPAIR_SETTING_GPS_SWITCH_ON);
        }
        if (isLocationEnabled && !isHighAcc) {
            commonDetectedRepairIds.add(GPSREPAIRIDS.REPAIR_SETTING_GPS_MODE_SWITCH_VALUE1);
        }
        if (!isWifiScanOpen) {
            commonDetectedRepairIds.add(GPSREPAIRIDS.REPAIR_SETTING_WLAN_SCAN_SWITCH_ON);
        }
        return commonDetectedRepairIds;
    }

    private void broadcastDetect(String extraInfo, ArrayList<String> repairIds) {
        if (repairIds != null && !repairIds.isEmpty()) {
            Intent intent = new Intent(SMART_FAULT_ACTION);
            intent.setClassName(SMART_FAULT_PACKAGE_NAME, SMART_FAULT_CLASS_NAME);
            intent.putExtra(KEY_EXTRA_INFO, extraInfo);
            intent.putExtra(KEY_HANDLE_TYPE, GPS_HANDLE_TYPE);
            intent.putExtra(KEY_FAULT_NAME, GPS_FAULT_NAME);
            intent.putExtra(KEY_FAULT_CODE, GPS_FAULT_CODE);
            intent.putExtra(KEY_FAULT_DESCRIPTION, GPS_FAULT_DESCRIPTION_CODE);
            intent.putExtra(KEY_FAULT_SUGGESTION, GPS_FAULT_SUGGESTION_CODE);
            intent.putStringArrayListExtra(KEY_REPAIR_ID, repairIds);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, SMART_FAULT_PERMISSION);
        }
    }

    private boolean isForeGroundProc(String packageName) {
        return ((ActivityManager) this.mContext.getSystemService("activity")).getPackageImportance(packageName) <= CPUFeature.MSG_SET_CPUSETCONFIG_VR;
    }
}
