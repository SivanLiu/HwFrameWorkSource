package android.emcom;

import android.util.Log;

public class SmartcareInfos {
    private static final String LOG_TAG = "SmartcareInfos";
    public BrowserInfo browserInfo;
    public EmailInfo emailInfo;
    public FwkNetworkInfo fwkNetworkInfo;
    public GameInfo gameInfo;
    public HttpInfo httpInfo;
    public TcpStatusInfo tcpStatusInfo;
    public VideoInfo videoInfo;
    public WechatInfo wechatInfo;

    public static class SmartcareBaseInfo {
        public String pkgName = "";
        public SmartcareInfos smarcareInfos;

        public SmartcareBaseInfo() {
            if (Log.HWINFO) {
                String str = SmartcareInfos.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("construct<>: ");
                stringBuilder.append(this);
                Log.d(str, stringBuilder.toString());
            }
        }

        public void addToInfos(SmartcareInfos is) {
            this.smarcareInfos = is;
            if (Log.HWINFO) {
                String str = SmartcareInfos.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addToInfos is: ");
                stringBuilder.append(is);
                stringBuilder.append(",this: ");
                stringBuilder.append(this);
                stringBuilder.append(", ");
                stringBuilder.append(this.smarcareInfos);
                stringBuilder.append(",");
                stringBuilder.append(this.pkgName);
                Log.d(str, stringBuilder.toString());
            }
        }

        public void recycle() {
            this.pkgName = "";
            if (Log.HWINFO) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("recycle: ");
                stringBuilder.append(this);
                Log.d("is", stringBuilder.toString());
            }
        }
    }

    public static class BrowserInfo extends SmartcareBaseInfo {
        public String appName = "";
        public short connectLatency = (short) -1;
        public byte connectSuccessFlag = (byte) -1;
        public short dnsLatency = (short) -1;
        public byte dnsSuccessFlag = (byte) -1;
        public int downloadAvgThput = -1;
        public long pageId = -1;
        public int pageLatency = -1;
        public boolean result = true;
        public short rspCode = (short) -1;

        public void addToInfos(SmartcareInfos is) {
            super.addToInfos(is);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BrowserInfo: ");
            stringBuilder.append(this);
            stringBuilder.append(",is: ");
            stringBuilder.append(is);
            Log.d("is", stringBuilder.toString());
            is.browserInfo = this;
        }

        public BrowserInfo copyFrom(BrowserInfo wci) {
            this.pageId = wci.pageId;
            this.appName = wci.appName;
            this.pageLatency = wci.pageLatency;
            this.result = wci.result;
            this.rspCode = wci.rspCode;
            this.dnsLatency = wci.dnsLatency;
            this.connectLatency = wci.connectLatency;
            this.dnsSuccessFlag = wci.dnsSuccessFlag;
            this.connectSuccessFlag = wci.connectSuccessFlag;
            this.downloadAvgThput = wci.downloadAvgThput;
            return this;
        }

        public void recycle() {
            super.recycle();
            this.pageId = -1;
            this.appName = "";
            this.pageLatency = -1;
            this.result = true;
            this.rspCode = (short) -1;
            this.dnsLatency = (short) -1;
            this.connectLatency = (short) -1;
            this.dnsSuccessFlag = (byte) -1;
            this.connectSuccessFlag = (byte) -1;
            this.downloadAvgThput = -1;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("BrowserInfo:");
            sb.append(", hash: ");
            sb.append(hashCode());
            sb.append(", pageId: ");
            sb.append(this.pageId);
            sb.append(", pageLatency: ");
            sb.append(this.pageLatency);
            sb.append(", rspCode: ");
            sb.append(this.rspCode);
            sb.append(", dnsLatency: ");
            sb.append(this.dnsLatency);
            sb.append(", connectLatency: ");
            sb.append(this.connectLatency);
            sb.append(",");
            sb.append(", dnsSuccessFlag: ");
            sb.append(this.dnsSuccessFlag);
            sb.append(",");
            sb.append(", connectSuccessFlag: ");
            sb.append(this.connectSuccessFlag);
            sb.append(",");
            sb.append(", downloadAvgThput: ");
            sb.append(this.downloadAvgThput);
            sb.append(",");
            return sb.toString();
        }
    }

    public static class FwkNetworkInfo extends SmartcareBaseInfo {
        public short mcc;
        public short mnc;
        public byte rat;
        public byte rsrp;
        public byte rsrq;
        public int sinr;
        public String timeAndCid;
        public String wlanBssid;
        public byte wlanSignalStrength;
        public String wlanSsid;

        public FwkNetworkInfo copyFrom(FwkNetworkInfo fci) {
            this.mcc = fci.mcc;
            this.mnc = fci.mnc;
            this.rat = fci.rat;
            this.timeAndCid = fci.timeAndCid;
            this.rsrp = fci.rsrp;
            this.rsrq = fci.rsrq;
            this.sinr = fci.sinr;
            this.wlanSignalStrength = fci.wlanSignalStrength;
            this.wlanBssid = fci.wlanBssid;
            this.wlanSsid = fci.wlanSsid;
            return this;
        }

        public void addToInfos(SmartcareInfos is) {
            super.addToInfos(is);
            is.fwkNetworkInfo = this;
        }

        public void recycle() {
            super.recycle();
            this.mcc = (short) 0;
            this.mnc = (short) 0;
            this.rat = (byte) 0;
            this.timeAndCid = "";
            this.rsrp = (byte) -1;
            this.rsrq = (byte) -1;
            this.sinr = -1;
            this.wlanSignalStrength = (byte) -1;
            this.wlanBssid = "";
            this.wlanSsid = "";
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FwkNetworkInfo:");
            sb.append(", hash: ");
            sb.append(hashCode());
            sb.append(", rat: ");
            sb.append(this.rat);
            sb.append(", rsrp: ");
            sb.append(this.rsrp);
            sb.append(", rsrq: ");
            sb.append(this.rsrq);
            sb.append(", sinr: ");
            sb.append(this.sinr);
            sb.append(", wlanSignalStrength: ");
            sb.append(this.wlanSignalStrength);
            sb.append(",");
            return sb.toString();
        }
    }

    public static class HttpInfo extends SmartcareBaseInfo {
        public String appName;
        public int endTime;
        public String host;
        public int numStreams;
        public int startDate;
        public int startTime;
        public int uid;

        public HttpInfo copyFrom(HttpInfo sci) {
            this.host = sci.host;
            this.startDate = sci.startDate;
            this.startTime = sci.startTime;
            this.endTime = sci.endTime;
            this.numStreams = sci.numStreams;
            this.uid = sci.uid;
            this.appName = sci.appName;
            return this;
        }

        public void recycle() {
            super.recycle();
            this.host = "";
            this.startDate = 0;
            this.startTime = 0;
            this.endTime = 0;
            this.numStreams = 0;
            this.uid = 0;
            this.appName = "";
        }

        public void addToInfos(SmartcareInfos is) {
            super.addToInfos(is);
            is.httpInfo = this;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("HttpInfo:");
            sb.append(", hash: ");
            sb.append(hashCode());
            sb.append(", startDate: ");
            sb.append(this.startDate);
            sb.append(", startTime: ");
            sb.append(this.startTime);
            sb.append(", endTime: ");
            sb.append(this.endTime);
            sb.append(",");
            return sb.toString();
        }
    }

    public static class TcpStatusInfo extends SmartcareBaseInfo {
        public int dnsDelay;
        public short synRtrans;
        public int synRtt;
        public short tcpDLWinZeroCount;
        public short tcpDlDisorderPkts;
        public int tcpDlPackages;
        public short tcpDlThreeDupAcks;
        public short tcpULWinZeroCount;
        public short tcpUlFastRetrans;
        public int tcpUlPackages;
        public short tcpUlTimeoutRetrans;

        public void addToInfos(SmartcareInfos is) {
            super.addToInfos(is);
            is.tcpStatusInfo = this;
        }

        public TcpStatusInfo copyFrom(TcpStatusInfo tsi) {
            this.tcpUlPackages = tsi.tcpUlPackages;
            this.tcpDlPackages = tsi.tcpDlPackages;
            this.synRtrans = tsi.synRtrans;
            this.tcpDLWinZeroCount = tsi.tcpDLWinZeroCount;
            this.tcpUlTimeoutRetrans = tsi.tcpUlTimeoutRetrans;
            this.tcpULWinZeroCount = tsi.tcpULWinZeroCount;
            this.tcpDlThreeDupAcks = tsi.tcpDlThreeDupAcks;
            this.tcpDlDisorderPkts = tsi.tcpDlDisorderPkts;
            this.dnsDelay = tsi.dnsDelay;
            this.synRtt = tsi.synRtt;
            this.tcpUlFastRetrans = tsi.tcpUlFastRetrans;
            return this;
        }

        public void recycle() {
            super.recycle();
            this.tcpUlPackages = 0;
            this.tcpDlPackages = 0;
            this.synRtrans = (short) 0;
            this.tcpDLWinZeroCount = (short) 0;
            this.tcpUlTimeoutRetrans = (short) 0;
            this.tcpULWinZeroCount = (short) 0;
            this.tcpDlThreeDupAcks = (short) 0;
            this.tcpDlDisorderPkts = (short) 0;
            this.dnsDelay = 0;
            this.synRtt = 0;
            this.tcpUlFastRetrans = (short) 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TcpStatusInfo:, hash: ");
            stringBuilder.append(hashCode());
            stringBuilder.append("dnsDelay: ");
            stringBuilder.append(this.dnsDelay);
            return stringBuilder.toString();
        }
    }

    public static class WechatInfo extends SmartcareBaseInfo {
        public int endTime = 0;
        public String host;
        public int latancy = -1;
        public int startDate = 0;
        public int startTime = 0;
        public byte successFlag = (byte) 1;
        public int type = 0;

        public void addToInfos(SmartcareInfos is) {
            super.addToInfos(is);
            is.wechatInfo = this;
        }

        public WechatInfo copyFrom(WechatInfo wci) {
            this.successFlag = wci.successFlag;
            this.latancy = wci.latancy;
            this.type = wci.type;
            this.host = wci.host;
            this.startDate = wci.startDate;
            this.startTime = wci.startTime;
            this.endTime = wci.endTime;
            return this;
        }

        public void recycle() {
            super.recycle();
            this.successFlag = (byte) 1;
            this.latancy = -1;
            this.type = -1;
            this.host = "";
            this.startDate = 0;
            this.startTime = 0;
            this.endTime = 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WechatInfo: , hash: ");
            stringBuilder.append(hashCode());
            stringBuilder.append(", latancy = ");
            stringBuilder.append(this.latancy);
            stringBuilder.append(",successFlag = ");
            stringBuilder.append(this.successFlag);
            stringBuilder.append(",type = ");
            stringBuilder.append(this.type);
            return stringBuilder.toString();
        }
    }

    public void recycle() {
        if (this.fwkNetworkInfo != null) {
            this.fwkNetworkInfo.recycle();
        }
        if (this.gameInfo != null) {
            this.gameInfo.recycle();
        }
        if (this.videoInfo != null) {
            this.videoInfo.recycle();
        }
        if (this.emailInfo != null) {
            this.emailInfo.recycle();
        }
        if (this.wechatInfo != null) {
            this.wechatInfo.recycle();
        }
        if (this.browserInfo != null) {
            this.browserInfo.recycle();
        }
        if (this.httpInfo != null) {
            this.httpInfo.recycle();
        }
        if (this.tcpStatusInfo != null) {
            this.tcpStatusInfo.recycle();
        }
    }

    public SmartcareInfos copyFrom(SmartcareInfos is) {
        if (is.browserInfo != null) {
            if (this.browserInfo == null) {
                new BrowserInfo().addToInfos(this);
            }
            this.browserInfo.copyFrom(is.browserInfo);
        }
        if (is.httpInfo != null) {
            if (this.httpInfo == null) {
                new HttpInfo().addToInfos(this);
            }
            this.httpInfo.copyFrom(is.httpInfo);
        }
        if (is.tcpStatusInfo != null) {
            if (this.tcpStatusInfo == null) {
                new TcpStatusInfo().addToInfos(this);
            }
            this.tcpStatusInfo.copyFrom(is.tcpStatusInfo);
        }
        if (is.fwkNetworkInfo != null) {
            if (this.fwkNetworkInfo == null) {
                new FwkNetworkInfo().addToInfos(this);
            }
            this.fwkNetworkInfo.copyFrom(is.fwkNetworkInfo);
        }
        return this;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SmartcareInfos:  hash: ");
        stringBuilder.append(hashCode());
        stringBuilder.append(" gameInfo: ");
        stringBuilder.append(this.gameInfo);
        stringBuilder.append(" browserInfo: ");
        stringBuilder.append(this.browserInfo);
        stringBuilder.append(" videoInfo: ");
        stringBuilder.append(this.videoInfo);
        stringBuilder.append(" emailInfo: ");
        stringBuilder.append(this.emailInfo);
        stringBuilder.append(" httpInfo: ");
        stringBuilder.append(this.httpInfo);
        stringBuilder.append(" fwkNetworkInfo: ");
        stringBuilder.append(this.fwkNetworkInfo);
        stringBuilder.append(" tcpStatusInfo: ");
        stringBuilder.append(this.tcpStatusInfo);
        return stringBuilder.toString();
    }
}
