package com.android.server.wifi.wifipro;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
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
    private volatile ArrayList<String> mAbnormalWifiBlacklist = new ArrayList();
    private volatile ArrayList<String> mCellBlacklist = new ArrayList();
    private HashMap<String, Integer> mTempWifiBlackList = new HashMap();
    private volatile ArrayList<String> mWifiBlacklist = new ArrayList();

    class removeBlacklistTask extends TimerTask {
        String id;
        int type;

        public removeBlacklistTask(String id, int type) {
            this.type = type;
            this.id = id;
        }

        public void run() {
            if (1 == this.type) {
                NetworkBlackListManager.this.removeWifiBlacklist(this.id);
            } else if (2 == this.type) {
                NetworkBlackListManager.this.removeCellBlacklist(this.id);
            }
        }
    }

    private NetworkBlackListManager(Context context) {
    }

    public static NetworkBlackListManager getNetworkBlackListManagerInstance(Context context) {
        if (mNetworkBlackListManager == null) {
            mNetworkBlackListManager = new NetworkBlackListManager(context);
        }
        return mNetworkBlackListManager;
    }

    public ArrayList<String> getWifiBlacklist() {
        return this.mWifiBlacklist;
    }

    public ArrayList<String> getCellBlacklist() {
        return this.mCellBlacklist;
    }

    public ArrayList<String> getAbnormalWifiBlacklist() {
        return this.mAbnormalWifiBlacklist;
    }

    public synchronized void addWifiBlacklist(String bssid) {
        if (!TextUtils.isEmpty(bssid)) {
            this.mWifiBlacklist.add(bssid);
            new Timer().schedule(new removeBlacklistTask(bssid, 1), 120000);
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0051, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isFailedMultiTimes(String bssid) {
        boolean z = false;
        if (TextUtils.isEmpty(bssid)) {
            return false;
        }
        int curCounter = 1;
        if (this.mTempWifiBlackList.containsKey(bssid)) {
            curCounter = ((Integer) this.mTempWifiBlackList.get(bssid)).intValue() + 1;
            this.mTempWifiBlackList.put(bssid, Integer.valueOf(curCounter));
        } else {
            this.mTempWifiBlackList.put(bssid, Integer.valueOf(1));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isFailedMultiTimes curCounter = ");
        stringBuilder.append(curCounter);
        Log.d("WiFi_PRO", stringBuilder.toString());
        if (curCounter >= 2) {
            z = true;
        }
    }

    public synchronized boolean containedInWifiBlacklists(String targetSsid) {
        if (TextUtils.isEmpty(targetSsid)) {
            return false;
        }
        for (int i = 0; i < this.mWifiBlacklist.size(); i++) {
            String str = (String) this.mWifiBlacklist.get(i);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\"");
            stringBuilder.append(targetSsid);
            stringBuilder.append("\"");
            if (str.equals(stringBuilder.toString())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addCellBlacklist(String cellid) {
        if (!TextUtils.isEmpty(cellid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addCellBlacklist + id = ");
            stringBuilder.append(cellid);
            Log.w("WiFi_PRO", stringBuilder.toString());
            this.mCellBlacklist.add(cellid);
            new Timer().schedule(new removeBlacklistTask(cellid, 2), 120000);
        }
    }

    public synchronized void addAbnormalWifiBlacklist(String bssid) {
        if (!TextUtils.isEmpty(bssid)) {
            this.mAbnormalWifiBlacklist.add(bssid);
        }
    }

    private synchronized void removeWifiBlacklist(String bssid) {
        if (this.mWifiBlacklist.contains(bssid)) {
            this.mWifiBlacklist.remove(bssid);
        }
    }

    private synchronized void removeCellBlacklist(String cellid) {
        if (this.mCellBlacklist.contains(cellid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeBlacklistTask + id = ");
            stringBuilder.append(cellid);
            Log.w("WiFi_PRO", stringBuilder.toString());
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
        return this.mWifiBlacklist.contains(bssid);
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

    /* JADX WARNING: Missing block: B:14:0x0023, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isInTempWifiBlackList(String bssid) {
        boolean z = false;
        if (TextUtils.isEmpty(bssid)) {
            return false;
        }
        if (this.mTempWifiBlackList.containsKey(bssid) && ((Integer) this.mTempWifiBlackList.get(bssid)).intValue() > 0) {
            z = true;
        }
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
}
