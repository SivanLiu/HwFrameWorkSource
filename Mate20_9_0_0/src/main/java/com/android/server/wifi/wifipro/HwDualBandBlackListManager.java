package com.android.server.wifi.wifipro;

import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class HwDualBandBlackListManager {
    public static final int BLACKLIST_VALID_TIME = 300000;
    private static final int PERMANENT_BLACKLIST_VALID_TIME = 1200000;
    private static final String TAG = "WiFi_PRO";
    private static HwDualBandBlackListManager mHwDualBandBlackListMgr;
    private HashMap<String, DualBandBlackApInfo> mPermanentWifiBlackListApInfo = new HashMap();
    private volatile ArrayList<String> mPermanentWifiBlacklist = new ArrayList();
    private HashMap<String, DualBandBlackApInfo> mWifiBlackListApInfo = new HashMap();
    private volatile ArrayList<String> mWifiBlacklist = new ArrayList();

    static class DualBandBlackApInfo {
        private long mAddTime;
        private int mCounter;
        private long mExpireTime;
        private String mSsid;

        public DualBandBlackApInfo(String ssid, int counter, long addTime, long expireTime) {
            this.mSsid = ssid;
            this.mCounter = counter;
            this.mAddTime = addTime;
            this.mExpireTime = expireTime;
        }

        private String getBlackApSsid() {
            return this.mSsid;
        }

        private int getBlackApCounter() {
            return this.mCounter;
        }

        private void setBlackApCounter(int counter) {
            this.mCounter = counter;
        }

        private long getAddTime() {
            return this.mAddTime;
        }

        private void setAddTime(long addTime) {
            this.mAddTime = addTime;
        }

        private void setExpireTime(long time) {
            this.mExpireTime = time;
        }

        private long getExpireTIme() {
            return this.mExpireTime;
        }
    }

    class RemoveBlacklistTask extends TimerTask {
        String id;

        public RemoveBlacklistTask(String id) {
            this.id = id;
        }

        public void run() {
            HwDualBandBlackListManager.this.removeWifiBlacklist(this.id);
        }
    }

    class RemovePermanentBlacklistTask extends TimerTask {
        String id;

        public RemovePermanentBlacklistTask(String id) {
            this.id = id;
        }

        public void run() {
            HwDualBandBlackListManager.this.removePermanentWifiBlacklist(this.id);
        }
    }

    private HwDualBandBlackListManager() {
    }

    public static HwDualBandBlackListManager getHwDualBandBlackListMgrInstance() {
        if (mHwDualBandBlackListMgr == null) {
            mHwDualBandBlackListMgr = new HwDualBandBlackListManager();
        }
        return mHwDualBandBlackListMgr;
    }

    public ArrayList<String> getWifiBlacklist() {
        return this.mWifiBlacklist;
    }

    /* JADX WARNING: Missing block: B:16:0x00ae, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:0x00b0, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void addWifiBlacklist(String ssid, boolean needReset) {
        if (!TextUtils.isEmpty(ssid)) {
            if (!this.mWifiBlacklist.contains(ssid)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addWifiBlacklist + ssid = ");
                stringBuilder.append(ssid);
                Log.d("WiFi_PRO", stringBuilder.toString());
                this.mWifiBlacklist.add(ssid);
                DualBandBlackApInfo apInfo;
                if (this.mWifiBlackListApInfo.containsKey(ssid)) {
                    apInfo = (DualBandBlackApInfo) this.mWifiBlackListApInfo.get(ssid);
                    int curCounter = needReset ? 0 : apInfo.getBlackApCounter() + 1;
                    int expireTime = 300000 * (curCounter + 1);
                    apInfo.setBlackApCounter(curCounter);
                    apInfo.setAddTime(System.currentTimeMillis());
                    apInfo.setExpireTime((long) expireTime);
                    this.mWifiBlackListApInfo.put(ssid, apInfo);
                    new Timer().schedule(new RemoveBlacklistTask(ssid), (long) expireTime);
                } else {
                    apInfo = new DualBandBlackApInfo(ssid, 0, System.currentTimeMillis(), 300000);
                    this.mWifiBlackListApInfo.put(ssid, apInfo);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Add new DualBandBlackApInfo : ");
                    stringBuilder2.append(apInfo.getBlackApSsid());
                    Log.d("WiFi_PRO", stringBuilder2.toString());
                    new Timer().schedule(new RemoveBlacklistTask(ssid), 300000);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:22:0x00c0, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void addPermanentWifiBlacklist(String ssid, String bssid) {
        if (!TextUtils.isEmpty(ssid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addPermanentWifiBlacklist ssid : ");
            stringBuilder.append(ssid);
            Log.d("WiFi_PRO", stringBuilder.toString());
            if (this.mPermanentWifiBlackListApInfo.containsKey(ssid)) {
                DualBandBlackApInfo apInfo = (DualBandBlackApInfo) this.mPermanentWifiBlackListApInfo.get(ssid);
                int curCounter = apInfo.getBlackApCounter() + 1;
                apInfo.setBlackApCounter(curCounter);
                apInfo.setAddTime(System.currentTimeMillis());
                this.mPermanentWifiBlackListApInfo.put(ssid, apInfo);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("curCounter : ");
                stringBuilder2.append(curCounter);
                Log.d("WiFi_PRO", stringBuilder2.toString());
                if (bssid != null && curCounter >= 2) {
                    WifiProDualBandApInfoRcd mRecrd = HwDualBandInformationManager.getInstance().getDualBandAPInfo(bssid);
                    if (mRecrd != null) {
                        mRecrd.isInBlackList = 1;
                        HwDualBandInformationManager.getInstance().updateAPInfo(mRecrd);
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("removePermanentWifiBlacklist ssid :  ");
                        stringBuilder2.append(ssid);
                        Log.d("WiFi_PRO", stringBuilder2.toString());
                        this.mPermanentWifiBlackListApInfo.remove(ssid);
                    }
                }
            } else {
                this.mPermanentWifiBlackListApInfo.put(ssid, new DualBandBlackApInfo(ssid, 1, System.currentTimeMillis(), 0));
            }
            if (!this.mPermanentWifiBlacklist.contains(ssid)) {
                this.mPermanentWifiBlacklist.add(ssid);
                new Timer().schedule(new RemovePermanentBlacklistTask(ssid), 1200000);
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x004c, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized long getPermanentExpireTimeForRetry(String ssid) {
        long j = 0;
        if (TextUtils.isEmpty(ssid)) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPermanentExpireTime for ssid =");
        stringBuilder.append(ssid);
        Log.d("WiFi_PRO", stringBuilder.toString());
        if (this.mPermanentWifiBlackListApInfo.containsKey(ssid)) {
            long expireTime = 1200000 - (System.currentTimeMillis() - ((DualBandBlackApInfo) this.mPermanentWifiBlackListApInfo.get(ssid)).getAddTime());
            if (expireTime <= 0) {
                removePermanentWifiBlacklist(ssid);
            }
            if (expireTime > 0) {
                j = expireTime;
            }
        } else {
            Log.e("WiFi_PRO", "can not find this ssid in PermanentWifiBlackList");
            removePermanentWifiBlacklist(ssid);
            return 0;
        }
    }

    public synchronized boolean isInPermanentWifiBlacklist(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            return false;
        }
        return this.mPermanentWifiBlacklist.contains(ssid);
    }

    private synchronized void removePermanentWifiBlacklist(String ssid) {
        if (ssid != null) {
            if (this.mPermanentWifiBlacklist.contains(ssid)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removePermanentWifiBlacklist ssid = ");
                stringBuilder.append(ssid);
                Log.d("WiFi_PRO", stringBuilder.toString());
                this.mPermanentWifiBlacklist.remove(ssid);
            }
        }
    }

    private synchronized void removeWifiBlacklist(String ssid) {
        if (ssid != null) {
            if (this.mWifiBlacklist.contains(ssid)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeWifiBlacklist ssid = ");
                stringBuilder.append(ssid);
                Log.d("WiFi_PRO", stringBuilder.toString());
                this.mWifiBlacklist.remove(ssid);
            }
        }
    }

    public synchronized boolean isInWifiBlacklist(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            return false;
        }
        return this.mWifiBlacklist.contains(ssid);
    }

    public synchronized void cleanBlacklist() {
        this.mWifiBlacklist.clear();
    }

    /* JADX WARNING: Missing block: B:17:0x0053, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized long getExpireTimeForRetry(String ssid) {
        long j = 0;
        if (TextUtils.isEmpty(ssid)) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpiretime for ssid =");
        stringBuilder.append(ssid);
        Log.d("WiFi_PRO", stringBuilder.toString());
        if (this.mWifiBlackListApInfo.containsKey(ssid)) {
            DualBandBlackApInfo apInfo = (DualBandBlackApInfo) this.mWifiBlackListApInfo.get(ssid);
            long expireRetryTime = apInfo.getExpireTIme() - (System.currentTimeMillis() - apInfo.getAddTime());
            if (expireRetryTime <= 0) {
                this.mWifiBlackListApInfo.remove(ssid);
                removeWifiBlacklist(ssid);
            }
            if (expireRetryTime > 0) {
                j = expireRetryTime;
            }
        } else {
            Log.e("WiFi_PRO", "can not find this ssid in DualBandBlackList");
            removeWifiBlacklist(ssid);
            return 0;
        }
    }
}
