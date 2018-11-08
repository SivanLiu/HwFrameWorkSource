package com.huawei.android.pushagent.model.prefs;

import android.content.Context;

public class k extends g {
    private static final byte[] dq = new byte[0];
    private static k dr = null;

    private k(Context context) {
        super(context, "pushConfig");
        mb();
    }

    public static k pt(Context context) {
        synchronized (dq) {
            if (dr != null) {
                k kVar = dr;
                return kVar;
            }
            dr = new k(context);
            kVar = dr;
            return kVar;
        }
    }

    public long qg() {
        return getLong("upAnalyticUrlTime", 0);
    }

    public boolean qh(long j) {
        return setValue("upAnalyticUrlTime", Long.valueOf(j));
    }

    public long px() {
        return getLong("run_time_less_times", 0);
    }

    public boolean py(long j) {
        return setValue("run_time_less_times", Long.valueOf(j));
    }

    public int qj() {
        return getInt("lastConnectPushSrvMethodIdx", 0);
    }

    public boolean qm(int i) {
        return setValue("lastConnectPushSrvMethodIdx", Integer.valueOf(i));
    }

    public int ql() {
        return getInt("tryConnectPushSevTimes", 0);
    }

    public boolean qn(int i) {
        return setValue("tryConnectPushSevTimes", Integer.valueOf(i));
    }

    public long qd() {
        return getLong("queryTrsTimes", 0);
    }

    public boolean qe(long j) {
        return setValue("queryTrsTimes", Long.valueOf(j));
    }

    public long qb() {
        return getLong("lastQueryTRSTime", 0);
    }

    public boolean qc(long j) {
        return setValue("lastQueryTRSTime", Long.valueOf(j));
    }

    public long pu() {
        return getLong("lastQueryTRSsucc_time", 0);
    }

    public boolean qf(long j) {
        return setValue("lastQueryTRSsucc_time", Long.valueOf(j));
    }

    public boolean pz() {
        return lz("isBadNetworkMode", false);
    }

    public boolean qa(boolean z) {
        return setValue("isBadNetworkMode", Boolean.valueOf(z));
    }

    public int pv() {
        return getInt("version_config", 0);
    }

    public boolean pw(int i) {
        return setValue("version_config", Integer.valueOf(i));
    }

    public int qk() {
        return getInt("networkPolicySwitch", 1);
    }

    public boolean qi(int i) {
        return setValue("networkPolicySwitch", Integer.valueOf(i));
    }
}
