package com.huawei.android.pushagent.model.prefs;

import android.content.Context;

public class l extends n {
    private static final byte[] gh = new byte[0];
    private static l gi = null;

    private l(Context context) {
        super(context, "pushConfig");
        vp();
    }

    public static l ul(Context context) {
        synchronized (gh) {
            l lVar;
            if (gi != null) {
                lVar = gi;
                return lVar;
            }
            gi = new l(context);
            lVar = gi;
            return lVar;
        }
    }

    public long vb() {
        return getLong("upAnalyticUrlTime", 0);
    }

    public boolean vf(long j) {
        return setValue("upAnalyticUrlTime", Long.valueOf(j));
    }

    public long ur() {
        return getLong("run_time_less_times", 0);
    }

    public boolean us(long j) {
        return setValue("run_time_less_times", Long.valueOf(j));
    }

    public int uw() {
        return getInt("lastConnectPushSrvMethodIdx", 0);
    }

    public boolean vc(int i) {
        return setValue("lastConnectPushSrvMethodIdx", Integer.valueOf(i));
    }

    public int va() {
        return getInt("tryConnectPushSevTimes", 0);
    }

    public boolean ve(int i) {
        return setValue("tryConnectPushSevTimes", Integer.valueOf(i));
    }

    public long uz() {
        return getLong("queryTrsTimes", 0);
    }

    public boolean vd(long j) {
        return setValue("queryTrsTimes", Long.valueOf(j));
    }

    public long ux() {
        return getLong("lastQueryTRSTime", 0);
    }

    public boolean ut(long j) {
        return setValue("lastQueryTRSTime", Long.valueOf(j));
    }

    public long uv() {
        return getLong("lastQueryTRSsucc_time", 0);
    }

    public boolean uu(long j) {
        return setValue("lastQueryTRSsucc_time", Long.valueOf(j));
    }

    public boolean up() {
        return vr("isBadNetworkMode", false);
    }

    public boolean uq(boolean z) {
        return setValue("isBadNetworkMode", Boolean.valueOf(z));
    }

    public int um() {
        return getInt("version_config", 0);
    }

    public boolean un(int i) {
        return setValue("version_config", Integer.valueOf(i));
    }

    public int uy() {
        return getInt("networkPolicySwitch", 1);
    }

    public boolean uo(int i) {
        return setValue("networkPolicySwitch", Integer.valueOf(i));
    }
}
