package com.android.server.hidata.wavemapping.entity;

import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.HashMap;
import java.util.Map.Entry;

public class SpaceExpInfo {
    private static final float RSSI_WEIGHT_OLD = 0.8f;
    private long data_rx = 0;
    private long data_tx = 0;
    private HashMap<String, Long> duration_app = new HashMap();
    private long duration_connected = 0;
    private String networkFreq = "";
    private String networkId = "";
    private String networkName = "";
    private long network_type = 8;
    private long power_consumption = 0;
    private HashMap<String, Integer> qoe_app_good = new HashMap();
    private HashMap<String, Integer> qoe_app_poor = new HashMap();
    private int qoe_wifipro_common = 0;
    private int qoe_wifipro_good = 0;
    private int qoe_wifipro_poor = 0;
    private int signal_value = 0;
    private StringBuilder spaceId = new StringBuilder("0");
    private StringBuilder spaceIdMainAp = new StringBuilder("0");
    private int user_pref_opt_in = 0;
    private int user_pref_opt_out = 0;
    private int user_pref_stay = 0;
    private int user_pref_total_count = 0;

    public String getNetworkName() {
        return this.networkName;
    }

    public void setNetworkName(String networkname) {
        this.networkName = networkname;
    }

    public String getNetworkId() {
        return this.networkId;
    }

    public String getNetworkFreq() {
        return this.networkFreq;
    }

    public void setNetworkFreq(String networkfreq) {
        this.networkFreq = networkfreq;
    }

    public String getSpaceID() {
        return this.spaceId.toString();
    }

    public String getSpaceIDMain() {
        return this.spaceIdMainAp.toString();
    }

    public int getQoEWifiProPoor() {
        return this.qoe_wifipro_poor;
    }

    public void setQoEWifiProPoor(int qoe_wifipro_poor) {
        this.qoe_wifipro_poor = qoe_wifipro_poor;
    }

    public int getQoEWifiProCommon() {
        return this.qoe_wifipro_common;
    }

    public void setQoEWifiProCommon(int qoe_wifipro_common) {
        this.qoe_wifipro_common = qoe_wifipro_common;
    }

    public int getQoEWifiProGood() {
        return this.qoe_wifipro_good;
    }

    public void setQoEWifiProGood(int qoe_wifipro_good) {
        this.qoe_wifipro_good = qoe_wifipro_good;
    }

    public int getSignalValue() {
        return this.signal_value;
    }

    public int getUserPrefOptIn() {
        return this.user_pref_opt_in;
    }

    public void accUserPrefOptIn() {
        this.user_pref_opt_in++;
    }

    public int getUserPrefOptOut() {
        return this.user_pref_opt_out;
    }

    public void accUserPrefOptOut() {
        this.user_pref_opt_out++;
    }

    public int getUserPrefStay() {
        return this.user_pref_stay;
    }

    public void accUserPrefStay() {
        this.user_pref_stay++;
    }

    public int getUserPrefTotalCount() {
        return this.user_pref_total_count;
    }

    public void accUserPrefTotalCount() {
        this.user_pref_total_count++;
    }

    public HashMap<String, Integer> getMapAppQoePoor() {
        return this.qoe_app_poor;
    }

    public HashMap<String, Integer> getMapAppQoeGood() {
        return this.qoe_app_good;
    }

    public HashMap<String, Long> getMapAppDuration() {
        return this.duration_app;
    }

    public int getAppQoePoor(String app) {
        if (this.qoe_app_poor.containsKey(app)) {
            return ((Integer) this.qoe_app_poor.get(app)).intValue();
        }
        return 0;
    }

    public int getAppQoeGood(String app) {
        if (this.qoe_app_good.containsKey(app)) {
            return ((Integer) this.qoe_app_good.get(app)).intValue();
        }
        return 0;
    }

    public long getAppDuration(String app) {
        if (this.duration_app.containsKey(app)) {
            return ((Long) this.duration_app.get(app)).longValue();
        }
        return 0;
    }

    public long getDuration() {
        return this.duration_connected;
    }

    public long getNetworkType() {
        return this.network_type;
    }

    public long getPowerConsumption() {
        return this.power_consumption;
    }

    public void accPowerConsumption(long power) {
        this.power_consumption += power;
    }

    public long getDataRx() {
        return this.data_rx;
    }

    public long getDataTx() {
        return this.data_tx;
    }

    public void accDataTraffic(long dRx, long dTx) {
        this.data_rx += dRx;
        this.data_tx += dTx;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update data traffic: acc_rx=");
        stringBuilder.append(this.data_rx);
        stringBuilder.append(", acc_tx=");
        stringBuilder.append(this.data_tx);
        LogUtil.i(stringBuilder.toString());
    }

    public void accQoEWifiProPoor() {
        this.qoe_wifipro_poor++;
    }

    public void accQoEWifiProCommon() {
        this.qoe_wifipro_common++;
    }

    public void accQoEWifiProGood() {
        this.qoe_wifipro_good++;
    }

    public void accAppPoor(String app) {
        if (this.qoe_app_poor.containsKey(app)) {
            this.qoe_app_poor.put(app, Integer.valueOf(((Integer) this.qoe_app_poor.get(app)).intValue() + 1));
            return;
        }
        this.qoe_app_poor.put(app, Integer.valueOf(1));
    }

    public void accAppGood(String app) {
        if (this.qoe_app_good.containsKey(app)) {
            this.qoe_app_good.put(app, Integer.valueOf(((Integer) this.qoe_app_good.get(app)).intValue() + 1));
            return;
        }
        this.qoe_app_good.put(app, Integer.valueOf(1));
    }

    private void accAppDuration(String app, long newDuration) {
        if (this.duration_app.containsKey(app)) {
            this.duration_app.put(app, Long.valueOf(((Long) this.duration_app.get(app)).longValue() + newDuration));
            return;
        }
        this.duration_app.put(app, Long.valueOf(newDuration));
    }

    public int accSignalValue(int newRssi) {
        if (newRssi < 0) {
            if (this.signal_value < 0) {
                this.signal_value = Math.round((((float) this.signal_value) * 0.8f) + (((float) newRssi) * 0.19999999f));
            } else {
                this.signal_value = newRssi;
            }
        }
        return this.signal_value;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0027  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0041  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0031  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0027  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0041  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0031  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void accDuration(String app, long newDuration) {
        Object obj;
        int hashCode = app.hashCode();
        if (hashCode != -2015525726) {
            if (hashCode == 2664213 && app.equals(Constant.USERDB_APP_NAME_WIFI)) {
                obj = null;
                switch (obj) {
                    case null:
                        LogUtil.i("update WIFI duration");
                        this.duration_connected += newDuration;
                        this.network_type = 1;
                        break;
                    case 1:
                        LogUtil.i("update MOBILE duration");
                        this.duration_connected += newDuration;
                        this.network_type = 0;
                        break;
                    default:
                        LogUtil.i("update APP duration");
                        accAppDuration(app, newDuration);
                        return;
                }
            }
        } else if (app.equals(Constant.USERDB_APP_NAME_MOBILE)) {
            obj = 1;
            switch (obj) {
                case null:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        }
        obj = -1;
        switch (obj) {
            case null:
                break;
            case 1:
                break;
            default:
                break;
        }
    }

    public void mergeAllRecords(SpaceExpInfo input) {
        this.qoe_wifipro_poor += input.getQoEWifiProPoor();
        this.qoe_wifipro_common += input.getQoEWifiProCommon();
        this.qoe_wifipro_good += input.getQoEWifiProGood();
        accSignalValue(input.getSignalValue());
        this.user_pref_opt_in += input.getUserPrefOptIn();
        this.user_pref_opt_out += input.getUserPrefOptOut();
        this.user_pref_stay += input.getUserPrefStay();
        this.user_pref_total_count += input.getUserPrefTotalCount();
        this.duration_connected += input.getDuration();
        this.data_rx += input.getDataRx();
        this.data_tx += input.getDataTx();
        this.power_consumption += input.getPowerConsumption();
        if (1 == input.getNetworkType() || 0 == input.getNetworkType()) {
            this.network_type = input.getNetworkType();
        }
        input.getMapAppQoePoor().forEach(new -$$Lambda$SpaceExpInfo$QI-7zLW8XkAu7n4wXBYEAENZDzo(this));
        input.getMapAppQoeGood().forEach(new -$$Lambda$SpaceExpInfo$cPw96sVJ3DDEjslELgkVohq8jSA(this));
        input.getMapAppDuration().forEach(new -$$Lambda$SpaceExpInfo$JzzV2KLKzGPchCYgN8JC_0Y1reM(this));
    }

    public SpaceExpInfo(StringBuilder spaceid, StringBuilder spaceid_mainap, String networkid, String networkname, String networkfreq, int qoe_wifipro_good, int qoe_wifipro_common, int qoe_wifipro_poor, int signal_value, int user_pref_opt_in, int user_pref_opt_out, int user_pref_stay, int user_pref_total_count, long power_consumption, long duration_connected, int nw_type) {
        this.spaceId = spaceid;
        this.spaceIdMainAp = spaceid_mainap;
        this.networkId = networkid;
        this.networkName = networkname;
        this.networkFreq = networkfreq;
        this.qoe_wifipro_good = qoe_wifipro_good;
        this.qoe_wifipro_common = qoe_wifipro_common;
        this.qoe_wifipro_poor = qoe_wifipro_poor;
        this.signal_value = signal_value;
        this.power_consumption = power_consumption;
        this.user_pref_opt_in = user_pref_opt_in;
        this.user_pref_opt_out = user_pref_opt_out;
        this.user_pref_stay = user_pref_stay;
        this.user_pref_total_count = user_pref_total_count;
        this.duration_connected = duration_connected;
        this.network_type = (long) nw_type;
    }

    public SpaceExpInfo(StringBuilder spaceid, StringBuilder spaceid_mainap, String networkid, String networkname, String networkfreq, HashMap<String, Integer> app_poor, HashMap<String, Integer> app_good, HashMap<String, Long> app_duration, int qoe_wifipro_good, int qoe_wifipro_common, int qoe_wifipro_poor, int signal_value, long power_consumption, int user_pref_opt_in, int user_pref_opt_out, int user_pref_stay, int user_pref_total_count, long duration_connected, int nw_type) {
        this.spaceId = spaceid;
        this.spaceIdMainAp = spaceid_mainap;
        this.networkId = networkid;
        this.networkName = networkname;
        this.networkFreq = networkfreq;
        this.qoe_app_poor = app_poor;
        this.qoe_app_good = app_good;
        this.duration_app = app_duration;
        this.qoe_wifipro_good = qoe_wifipro_good;
        this.qoe_wifipro_common = qoe_wifipro_common;
        this.qoe_wifipro_poor = qoe_wifipro_poor;
        this.signal_value = signal_value;
        this.power_consumption = power_consumption;
        this.user_pref_opt_in = user_pref_opt_in;
        this.user_pref_opt_out = user_pref_opt_out;
        this.user_pref_stay = user_pref_stay;
        this.user_pref_total_count = user_pref_total_count;
        this.duration_connected = duration_connected;
        this.network_type = (long) nw_type;
    }

    public SpaceExpInfo(StringBuilder spaceid, StringBuilder spaceid_mainap, String networkid, String networkname, String networkfreq, int nw_type) {
        this.spaceId = spaceid;
        this.spaceIdMainAp = spaceid_mainap;
        this.networkId = networkid;
        this.networkName = networkname;
        this.networkFreq = networkfreq;
        this.network_type = (long) nw_type;
    }

    public String toString() {
        StringBuffer appString = new StringBuffer();
        String app = "";
        int poorCnt = 0;
        int goodCnt = 0;
        for (Entry<String, Long> entry : this.duration_app.entrySet()) {
            app = (String) entry.getKey();
            long duration = ((Long) entry.getValue()).longValue();
            if (this.qoe_app_poor.containsKey(app)) {
                poorCnt = ((Integer) this.qoe_app_poor.get(app)).intValue();
            }
            if (this.qoe_app_good.containsKey(app)) {
                goodCnt = ((Integer) this.qoe_app_good.get(app)).intValue();
            }
            appString.append(", ");
            appString.append(app);
            appString.append(":'");
            appString.append(Constant.USERDB_APP_NAME_DURATION);
            appString.append("'='");
            appString.append(duration);
            appString.append("' ");
            appString.append(Constant.USERDB_APP_NAME_POOR);
            appString.append("'='");
            appString.append(poorCnt);
            appString.append("' ");
            appString.append(Constant.USERDB_APP_NAME_GOOD);
            appString.append("'='");
            appString.append(goodCnt);
            appString.append("' ");
            appString.append("");
        }
        String netIdPrint = "";
        if (LogUtil.getDebug_flag()) {
            netIdPrint = this.networkId;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SpaceExpInfo{spaceid='");
        stringBuilder.append(this.spaceId.toString());
        stringBuilder.append('\'');
        stringBuilder.append(", spaceid_mainap='");
        stringBuilder.append(this.spaceIdMainAp);
        stringBuilder.append('\'');
        stringBuilder.append(", networkname='");
        stringBuilder.append(this.networkName);
        stringBuilder.append('\'');
        stringBuilder.append(", networkid='");
        stringBuilder.append(netIdPrint);
        stringBuilder.append('\'');
        stringBuilder.append(", networkfreq='");
        stringBuilder.append(this.networkFreq);
        stringBuilder.append('\'');
        stringBuilder.append(", qoe_wifipro_poor=");
        stringBuilder.append(this.qoe_wifipro_poor);
        stringBuilder.append(", qoe_wifipro_common=");
        stringBuilder.append(this.qoe_wifipro_common);
        stringBuilder.append(", qoe_wifipro_good=");
        stringBuilder.append(this.qoe_wifipro_good);
        stringBuilder.append(", signal_value=");
        stringBuilder.append(this.signal_value);
        stringBuilder.append(", power_consumption=");
        stringBuilder.append(this.power_consumption);
        stringBuilder.append(", data_rx=");
        stringBuilder.append(this.data_rx);
        stringBuilder.append(", data_tx=");
        stringBuilder.append(this.data_tx);
        stringBuilder.append(", user_pref_opt_in=");
        stringBuilder.append(this.user_pref_opt_in);
        stringBuilder.append(", user_pref_opt_out=");
        stringBuilder.append(this.user_pref_opt_out);
        stringBuilder.append(", user_pref_stay=");
        stringBuilder.append(this.user_pref_stay);
        stringBuilder.append(", user_pref_total_count=");
        stringBuilder.append(this.user_pref_total_count);
        stringBuilder.append(", duration=");
        stringBuilder.append(this.duration_connected);
        stringBuilder.append(", nw_type=");
        stringBuilder.append(this.network_type);
        stringBuilder.append(appString.toString());
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
