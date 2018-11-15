package com.android.server.wifi.HwQoE;

public class HiDataTracfficInfo {
    public int mAPPType;
    public long mDuration;
    public String mIMSI;
    public long mThoughtput;
    public long mTimestamp;

    public static class HiDataApInfo {
        public int mApType;
        public int mAppType;
        public int mAuthType;
        public int mBlackCount;
        public String mSsid;

        public HiDataApInfo() {
            this.mSsid = "none";
            this.mAuthType = 0;
            this.mApType = 0;
            this.mAppType = 0;
            this.mBlackCount = 0;
        }

        public HiDataApInfo(String ssid, int authType, int apType, int appType, int blackCount) {
            this.mSsid = ssid;
            this.mAuthType = authType;
            this.mApType = apType;
            this.mAppType = appType;
            this.mBlackCount = blackCount;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("ssid: ").append(this.mSsid).append(" ,authType: ").append(this.mAuthType).append(" apType: ").append(this.mApType).append(" , appType: ").append(this.mAppType).append(" blackCount ").append(this.mBlackCount);
            return sb.toString();
        }
    }

    public static class WeChatMobileTrafficInfo {
        long avgTraffic;
        int counter;
        long totalTime;
        long totalTraffic;
        int wechaType;

        public void clean() {
            this.counter = 0;
            this.wechaType = 0;
            this.totalTime = 0;
            this.avgTraffic = 0;
            this.totalTraffic = 0;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("counter: ").append(this.counter).append(" ,wechaType: ").append(this.wechaType).append(" totalTime: ").append(this.totalTime).append(" s, avgTraffic: ").append(this.avgTraffic).append(" Bytes, totalTraffic: ").append(this.totalTraffic).append(" Bytes");
            return sb.toString();
        }
    }

    public HiDataTracfficInfo() {
        this.mIMSI = "none";
        this.mAPPType = 0;
        this.mThoughtput = 0;
        this.mTimestamp = 0;
        this.mDuration = 0;
    }

    public HiDataTracfficInfo(String dataImsi, int type, long traffic, long duration) {
        this.mIMSI = dataImsi;
        this.mAPPType = type;
        this.mThoughtput = traffic;
        this.mDuration = duration;
        this.mTimestamp = System.currentTimeMillis();
    }
}
