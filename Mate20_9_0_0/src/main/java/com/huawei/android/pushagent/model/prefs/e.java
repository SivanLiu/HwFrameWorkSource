package com.huawei.android.pushagent.model.prefs;

import android.content.Context;

public class e extends h {
    private static final byte[] cr = new byte[0];
    private static e cs = null;

    private e(Context context) {
        super(context, "pushConfig");
        la();
    }

    public static e jj(Context context) {
        synchronized (cr) {
            e eVar;
            if (cs != null) {
                eVar = cs;
                return eVar;
            }
            cs = new e(context);
            eVar = cs;
            return eVar;
        }
    }

    public long ju() {
        return getLong("upAnalyticUrlTime", 0);
    }

    public boolean kc(long j) {
        return setValue("upAnalyticUrlTime", Long.valueOf(j));
    }

    public long jt() {
        return getLong("run_time_less_times", 0);
    }

    public boolean kb(long j) {
        return setValue("run_time_less_times", Long.valueOf(j));
    }

    public int jk() {
        return getInt("lastConnectPushSrvMethodIdx", 0);
    }

    public boolean jn(int i) {
        return setValue("lastConnectPushSrvMethodIdx", Integer.valueOf(i));
    }

    public int jl() {
        return getInt("tryConnectPushSevTimes", 0);
    }

    public boolean jm(int i) {
        return setValue("tryConnectPushSevTimes", Integer.valueOf(i));
    }

    public long js() {
        return getLong("queryTrsTimes", 0);
    }

    public boolean ka(long j) {
        return setValue("queryTrsTimes", Long.valueOf(j));
    }

    public long jq() {
        return getLong("lastQueryTRSTime", 0);
    }

    public boolean jz(long j) {
        return setValue("lastQueryTRSTime", Long.valueOf(j));
    }

    public long jp() {
        return getLong("lastQueryTRSsucc_time", 0);
    }

    public boolean jy(long j) {
        return setValue("lastQueryTRSsucc_time", Long.valueOf(j));
    }

    public boolean jw() {
        return lc("isBadNetworkMode", false);
    }

    public boolean jx(boolean z) {
        return setValue("isBadNetworkMode", Boolean.valueOf(z));
    }

    public int jv() {
        return getInt("version_config", 0);
    }

    public boolean kd(int i) {
        return setValue("version_config", Integer.valueOf(i));
    }

    public int jr() {
        return getInt("networkPolicySwitch", 1);
    }

    public boolean jo(int i) {
        return setValue("networkPolicySwitch", Integer.valueOf(i));
    }
}
