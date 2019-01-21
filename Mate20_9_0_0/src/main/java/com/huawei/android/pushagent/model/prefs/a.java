package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map.Entry;

public class a extends h {
    private static a ch = null;

    private a(Context context) {
        super(context, "PushRouteInfo");
        la();
    }

    public static synchronized a ff(Context context) {
        synchronized (a.class) {
            a aVar;
            if (ch != null) {
                aVar = ch;
                return aVar;
            }
            ch = new a(context);
            aVar = ch;
            return aVar;
        }
    }

    public long hy() {
        return getLong("pushSrvValidTime", Long.MAX_VALUE);
    }

    public boolean ii(long j) {
        return setValue("pushSrvValidTime", Long.valueOf(j));
    }

    public int getResult() {
        return getInt("result", -1);
    }

    public boolean ih(int i) {
        return setValue("result", Integer.valueOf(i));
    }

    public String getBelongId() {
        return getString("belongId", "-1");
    }

    public int fz() {
        return getInt("isChkToken", 1);
    }

    public int fy() {
        return getInt("isChkNcToken", 1);
    }

    public String getAnalyticUrl() {
        return getString("analyticUrl", null);
    }

    public String fr() {
        return getString("passTrustPkgs", "");
    }

    public String fs() {
        return getString("noticeTrustPkgs", "");
    }

    public String ge() {
        return getString("dawnWhiteList", "");
    }

    public long fv() {
        return getLong("upAnalyticUrlInterval", 345600000);
    }

    public String getConnId() {
        return com.huawei.android.pushagent.utils.e.a.vt(getString("connId", ""));
    }

    public int hb() {
        return getInt("pushConnectLog", 0);
    }

    public int gl() {
        return getInt("heartbeatFailLog", 0);
    }

    public int hx() {
        return getInt("tokenReqLog", 0);
    }

    public int gv() {
        return getInt("onlineStatusLog", 0);
    }

    public int gt() {
        return getInt("msgLog", 0);
    }

    public int fx() {
        return getInt("channelCloseLog", 0);
    }

    public long gs() {
        return getLong("minReportItval", 1800000);
    }

    public long gf() {
        return getLong("fatalMinReportItval", 1200000);
    }

    public int hg() {
        return getInt("reportMaxCount", 200);
    }

    public int hh() {
        return getInt("reportUpperCount", 100);
    }

    public int hf() {
        return getInt("reportMaxByteCount", 2500);
    }

    public String getServerIP() {
        return getString("serverIp", "");
    }

    public int getServerPort() {
        return getInt("serverPort", -1);
    }

    public long hw() {
        return getLong("trsValid_min", 7200);
    }

    public long hv() {
        return getLong("trsValid_max", 2592000);
    }

    public int fw() {
        return getInt("bastetInterval", 3);
    }

    public long getWifiMinHeartbeat() {
        return getLong("wifiMinHeartbeat", 1800);
    }

    public long getWifiMaxHeartbeat() {
        return getLong("wifiMaxHeartbeat", 1800);
    }

    public long get3GMinHeartbeat() {
        return getLong("g3MinHeartbeat", 900);
    }

    public long get3GMaxHeartbeat() {
        return getLong("g3MaxHeartbeat", 1800);
    }

    public long hj() {
        return getLong("serverRec1_min", 3);
    }

    public long hk() {
        return getLong("serverRec2_min", 10);
    }

    public long hl() {
        return getLong("serverRec3_min", 30);
    }

    public long hm() {
        return getLong("serverRec4_min", 300);
    }

    public long hn() {
        return getLong("serverRec5_min", 300);
    }

    public long ho() {
        return getLong("serverRec6_min", 600);
    }

    public long hp() {
        return getLong("serverRec7_min", 900);
    }

    public long hq() {
        return getLong("serverRec8_min", 1800);
    }

    public long fg() {
        return getLong("noNetHeartbeat", 7200);
    }

    public long gd() {
        return getLong("connTrsItval", 300);
    }

    public long gc() {
        return getLong("connTrsErrItval", 1800);
    }

    public long ht() {
        return getLong("SrvMaxFail_times", 6);
    }

    public long gq() {
        return getLong("maxQTRS_times", 6);
    }

    public long hr() {
        return getLong("socketConnTimeOut", 30);
    }

    public long hs() {
        return getLong("socketConnectReadOut", 10);
    }

    public long hc() {
        return getLong("pushLeastRun_time", 30);
    }

    public long gx() {
        return getLong("push1StartInt", 3);
    }

    public long gy() {
        return getLong("push2StartInt", 30);
    }

    public long gz() {
        return getLong("push3StartInt", 600);
    }

    public long ha() {
        return getLong("push4StartInt", 1800);
    }

    public long gg() {
        return getLong("firstQueryTRSDayTimes", 6);
    }

    public long gh() {
        return getLong("firstQueryTRSHourTimes", 2);
    }

    public long gr() {
        return getLong("maxQueryTRSDayTimes", 1);
    }

    public HashMap<Long, Long> gi() {
        return gj("flowcInterval", "flowcVlomes");
    }

    public long hz() {
        return getLong("wifiFirstQueryTRSDayTimes", 18);
    }

    public long ia() {
        return getLong("wifiFirstQueryTRSHourTimes", 6);
    }

    public long ic() {
        return getLong("wifiMaxQueryTRSDayTimes", 3);
    }

    public long hu() {
        return getLong("stopServiceItval", 5);
    }

    public long fm() {
        return getLong("heartBeatRspTimeOut", 10) * 1000;
    }

    public HashMap<Long, Long> ib() {
        return gj("wifiFlowcInterval", "wifiFlowcVlomes");
    }

    public long ga() {
        return getLong("ConnRange", 600) * 1000;
    }

    public long gb() {
        return getLong("ConnRangeIdle", 1800) * 1000;
    }

    public int gp() {
        return getInt("MaxConnTimes", 4);
    }

    public boolean ie() {
        return getInt("allowPry", 0) == 1;
    }

    private HashMap<Long, Long> gj(String str, String str2) {
        String str3 = "\\d{1,3}";
        HashMap hashMap = new HashMap();
        for (String str4 : lb().keySet()) {
            if (str4.matches(str + str3)) {
                hashMap.put(Long.valueOf(getLong(str4, 1)), Long.valueOf(getLong(str4.replace(str, str2), 2147483647L)));
            }
        }
        return hashMap;
    }

    public HashMap<String, String> fn() {
        HashMap hashMap = new HashMap();
        String str = "apn_";
        for (Entry entry : lb().entrySet()) {
            String str2 = (String) entry.getKey();
            if (str2.startsWith(str)) {
                hashMap.put(str2, (String) entry.getValue());
            }
        }
        return hashMap;
    }

    public int gk() {
        return getInt("grpNum", 0);
    }

    public String gw() {
        String str = "CE6935516BA17DB6174D77DAB902ED0F75D8C9B071FD46981BB1D05AA95F14277122B362304D6B3B865D1C00F5D8C6FF8BC2D432B8CDB11CF95B2450B7ADA9E20957068AD84E1BD4666E30BB103C5BCE485643755E7921AE0430A87C71DEB42F764779D4118F9A4183ABB2CBA6C31913AE6141DE168C51A270BADC91518DCE317F3309B50CCFB4B1949DC41520CBB3354C0CA3FC6943FE75DADA3B2A89397A3D68D6DC6AEBA0B6178AC0089FFEF6D2CF6DD36327C5AAB4ECE3A59B7D6B4E250D05746A19E8F052A90AB4A7F41958013E66EB207798DB766342701D0E8F6D5141B910887F7D43EE58A63AC9AF4D7B4A2B27B67C42DBD5142501DB629C3208E760B20BE1775C387F823733E9D5407F291B10C1846F77B7452EEF25B4720A103B90DD19B1B12CD7D0D0A1F7EEAAD0210E2C21494299D1E1E8FC83C088886E03BB1CDFD8D3B0AF28023D0F9E1AB8ACF0D4B5900EC2B5E3BCAE23020B581271136A56FB404CAAECE005D78DBB71ADE08ED965F9304F4F2CB13C6B3242CB04D28A05ED5D75669BEDF0F788AA3D8C1B3FFFEF3D2C0A2700E6E266E33D6ABFD6B7377D65FB60CB1C7288CE12CD584C357E84C446";
        str = getString("publicKey", "CE6935516BA17DB6174D77DAB902ED0F75D8C9B071FD46981BB1D05AA95F14277122B362304D6B3B865D1C00F5D8C6FF8BC2D432B8CDB11CF95B2450B7ADA9E20957068AD84E1BD4666E30BB103C5BCE485643755E7921AE0430A87C71DEB42F764779D4118F9A4183ABB2CBA6C31913AE6141DE168C51A270BADC91518DCE317F3309B50CCFB4B1949DC41520CBB3354C0CA3FC6943FE75DADA3B2A89397A3D68D6DC6AEBA0B6178AC0089FFEF6D2CF6DD36327C5AAB4ECE3A59B7D6B4E250D05746A19E8F052A90AB4A7F41958013E66EB207798DB766342701D0E8F6D5141B910887F7D43EE58A63AC9AF4D7B4A2B27B67C42DBD5142501DB629C3208E760B20BE1775C387F823733E9D5407F291B10C1846F77B7452EEF25B4720A103B90DD19B1B12CD7D0D0A1F7EEAAD0210E2C21494299D1E1E8FC83C088886E03BB1CDFD8D3B0AF28023D0F9E1AB8ACF0D4B5900EC2B5E3BCAE23020B581271136A56FB404CAAECE005D78DBB71ADE08ED965F9304F4F2CB13C6B3242CB04D28A05ED5D75669BEDF0F788AA3D8C1B3FFFEF3D2C0A2700E6E266E33D6ABFD6B7377D65FB60CB1C7288CE12CD584C357E84C446");
        String vt = com.huawei.android.pushagent.utils.e.a.vt(str);
        if (!TextUtils.isEmpty(vt)) {
            return vt;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "public key is empty, use origin.");
        ii(0);
        return str;
    }

    public boolean isValid() {
        if ("".equals(getServerIP()) || -1 == getServerPort() || getResult() != 0) {
            return false;
        }
        return true;
    }

    public boolean isNotAllowedPush() {
        int result = getResult();
        if (25 == result || 26 == result || 27 == result) {
            return true;
        }
        return false;
    }

    public long fo() {
        return getLong("fir3gHb", 150) * 1000;
    }

    public long fp() {
        return getLong("firWifiHb", 150) * 1000;
    }

    public long hd() {
        return getLong("ReConnInterval", 300) * 1000;
    }

    public long he() {
        return getLong("ReConnIntervalIdle", 600) * 1000;
    }

    public long go() {
        return getLong("KeepConnTime", 300) * 1000;
    }

    public static boolean fu(String str) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        try {
            simpleDateFormat.setLenient(false);
            simpleDateFormat.parse(str);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public String gm() {
        String string = getString("idlePeriodBegin", "00:00");
        if (string.length() == "HH:mm".length() && (fu(string) ^ 1) == 0) {
            return string;
        }
        return "00:00";
    }

    public String gn() {
        String string = getString("idlePeriodEnd", "06:00");
        if (string.length() == "HH:mm".length() && (fu(string) ^ 1) == 0) {
            return string;
        }
        return "06:00";
    }

    public long fl() {
        return getLong("wifiHbValid", 604800) * 1000;
    }

    public long fk() {
        return getLong("dataHbValid", 604800) * 1000;
    }

    public int fj() {
        return getInt("bestHBCheckTime", 2);
    }

    public boolean id() {
        if (getInt("allowBastet", 1) == 1) {
            return true;
        }
        return false;
    }

    public boolean fq() {
        if (getInt("needCheckAgreement", 1) == 1) {
            return true;
        }
        return false;
    }

    /* renamed from: if */
    public boolean m2if() {
        if (getInt("needSolinger", 1) == 1) {
            return true;
        }
        return false;
    }

    public long gu() {
        return getLong("msgResponseTimeOut", 3600) * 1000;
    }

    public long ft() {
        return getLong("resetBastetTimeOut", 30) * 1000;
    }

    public long hi() {
        return getLong("responseMsgTimeout", 60) * 1000;
    }

    public long getNextConnectTrsInterval() {
        return getLong("nextConnectInterval", 86400) * 1000;
    }

    public boolean ig(long j) {
        return setValue("nextConnectInterval", Long.valueOf(j));
    }

    public long fi() {
        return getLong("minHeartbeatStep", 30) * 1000;
    }

    public long fh() {
        return getLong("maxHeartbeatStep", 60) * 1000;
    }
}
