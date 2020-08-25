package com.huawei.hwwifiproservice;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.hwUtil.StringUtilEx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkBlackListManager {
    public static final int BLACKLIST_VALID_TIME = 120000;
    private static final String TAG = "WiFi_PRO";
    private static final int TYPE_CELL = 2;
    private static final int TYPE_WIFI = 1;
    private static NetworkBlackListManager mNetworkBlackListManager;
    private ArrayList<String> mAbnormalWifiBlacklist = new ArrayList<>();
    private ArrayList<String> mCellBlacklist = new ArrayList<>();
    private HashMap<String, Integer> mTempWifiBlackList = new HashMap<>();
    private ArrayList<String> mWifiBlacklist = new ArrayList<>();

    private NetworkBlackListManager(Context context) {
    }

    public static NetworkBlackListManager getNetworkBlackListManagerInstance(Context context) {
        if (mNetworkBlackListManager == null) {
            mNetworkBlackListManager = new NetworkBlackListManager(context);
        }
        return mNetworkBlackListManager;
    }

    public synchronized ArrayList<String> getWifiBlacklist() {
        return this.mWifiBlacklist;
    }

    public synchronized ArrayList<String> getCellBlacklist() {
        return this.mCellBlacklist;
    }

    public synchronized ArrayList<String> getAbnormalWifiBlacklist() {
        return this.mAbnormalWifiBlacklist;
    }

    public synchronized void addWifiBlacklist(String bssid) {
        if (!TextUtils.isEmpty(bssid)) {
            Log.i("WiFi_PRO", "addWifiBlacklist for 2 minutes bssid = " + StringUtilEx.safeDisplayBssid(bssid));
            this.mWifiBlacklist.add(bssid);
            new Timer().schedule(new removeBlacklistTask(bssid, 1), 120000);
        }
    }

    public synchronized boolean isFailedMultiTimes(String bssid) {
        boolean z = false;
        if (TextUtils.isEmpty(bssid)) {
            return false;
        }
        int curCounter = 1;
        if (this.mTempWifiBlackList.containsKey(bssid)) {
            curCounter = this.mTempWifiBlackList.get(bssid).intValue() + 1;
            this.mTempWifiBlackList.put(bssid, Integer.valueOf(curCounter));
        } else {
            this.mTempWifiBlackList.put(bssid, 1);
        }
        Log.i("WiFi_PRO", "isFailedMultiTimes curCounter = " + curCounter);
        if (curCounter >= 2) {
            z = true;
        }
        return z;
    }

    public synchronized boolean containedInWifiBlacklists(String targetSsid) {
        if (TextUtils.isEmpty(targetSsid)) {
            return false;
        }
        for (int i = 0; i < this.mWifiBlacklist.size(); i++) {
            if (this.mWifiBlacklist.get(i).equals("\"" + targetSsid + "\"")) {
                Log.i("WiFi_PRO", "containedInWifiBlacklists targetSsid " + StringUtilEx.safeDisplaySsid(targetSsid));
                return true;
            }
        }
        return false;
    }

    public synchronized void addCellBlacklist(String cellid) {
        if (!TextUtils.isEmpty(cellid)) {
            Log.i("WiFi_PRO", "addCellBlacklist + id = {private}");
            this.mCellBlacklist.add(cellid);
            new Timer().schedule(new removeBlacklistTask(cellid, 2), 120000);
        }
    }

    public synchronized void addAbnormalWifiBlacklist(String bssid) {
        if (!TextUtils.isEmpty(bssid)) {
            this.mAbnormalWifiBlacklist.add(bssid);
        }
    }

    /* access modifiers changed from: private */
    public synchronized void removeWifiBlacklist(String bssid) {
        if (this.mWifiBlacklist.contains(bssid)) {
            this.mWifiBlacklist.remove(bssid);
        }
    }

    /* access modifiers changed from: private */
    public synchronized void removeCellBlacklist(String cellid) {
        if (this.mCellBlacklist.contains(cellid)) {
            Log.i("WiFi_PRO", "removeBlacklistTask + id = {private}");
            this.mCellBlacklist.remove(cellid);
        }
    }

    public synchronized void removeAbnormalWifiBlacklist(String bssid) {
        if (this.mAbnormalWifiBlacklist.contains(bssid)) {
            this.mAbnormalWifiBlacklist.remove(bssid);
        }
    }

    public synchronized boolean isInWifiBlacklist(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            return false;
        }
        if (!this.mWifiBlacklist.contains(bssid)) {
            return false;
        }
        Log.i("WiFi_PRO", "isInWifiBlacklist bssid " + StringUtilEx.safeDisplayBssid(bssid));
        return true;
    }

    public synchronized boolean isInCellBlacklist(String cellid) {
        if (TextUtils.isEmpty(cellid)) {
            return false;
        }
        return this.mCellBlacklist.contains(cellid);
    }

    public synchronized boolean isInAbnormalWifiBlacklist(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            return false;
        }
        return this.mAbnormalWifiBlacklist.contains(bssid);
    }

    public synchronized boolean isInTempWifiBlackList(String bssid) {
        boolean z = false;
        if (TextUtils.isEmpty(bssid)) {
            return false;
        }
        if (this.mTempWifiBlackList.containsKey(bssid) && this.mTempWifiBlackList.get(bssid).intValue() > 0) {
            z = true;
        }
        return z;
    }

    public synchronized void cleanTempWifiBlackList() {
        this.mTempWifiBlackList.clear();
    }

    public synchronized void cleanAbnormalWifiBlacklist() {
        this.mAbnormalWifiBlacklist.clear();
    }

    public synchronized void cleanBlacklist() {
        this.mCellBlacklist.clear();
        this.mWifiBlacklist.clear();
    }

    class removeBlacklistTask extends TimerTask {
        String id;
        int type;

        public removeBlacklistTask(String id2, int type2) {
            this.type = type2;
            this.id = id2;
        }

        public void run() {
            int i = this.type;
            if (1 == i) {
                NetworkBlackListManager.this.removeWifiBlacklist(this.id);
            } else if (2 == i) {
                NetworkBlackListManager.this.removeCellBlacklist(this.id);
            }
        }
    }
}
