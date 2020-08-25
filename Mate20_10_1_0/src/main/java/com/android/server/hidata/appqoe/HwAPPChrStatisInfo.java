package com.android.server.hidata.appqoe;

public class HwAPPChrStatisInfo {
    public static final int CHR_ADD_TYPE_BAD_AFTER_MP = 15;
    public static final int CHR_ADD_TYPE_CELL_PERIOD = 6;
    public static final int CHR_ADD_TYPE_CELL_STALL = 4;
    public static final int CHR_ADD_TYPE_CELL_START = 2;
    public static final int CHR_ADD_TYPE_CH_FAILED = 11;
    public static final int CHR_ADD_TYPE_CLOSE_CELL = 18;
    public static final int CHR_ADD_TYPE_CLOSE_WIFI = 19;
    public static final int CHR_ADD_TYPE_GOOD_AFTER_MP = 14;
    public static final int CHR_ADD_TYPE_HICURE_SUCCESS = 21;
    public static final int CHR_ADD_TYPE_IN_KQI = 16;
    public static final int CHR_ADD_TYPE_MP_FAILED = 12;
    public static final int CHR_ADD_TYPE_MP_SUCCESS = 13;
    public static final int CHR_ADD_TYPE_OUT_KQI = 17;
    public static final int CHR_ADD_TYPE_REASON_1 = 7;
    public static final int CHR_ADD_TYPE_REASON_2 = 8;
    public static final int CHR_ADD_TYPE_REASON_3 = 9;
    public static final int CHR_ADD_TYPE_REASON_4 = 10;
    public static final int CHR_ADD_TYPE_START_HICURE = 20;
    public static final int CHR_ADD_TYPE_TRAFFIC = 22;
    public static final int CHR_ADD_TYPE_WIFI_PERIOD = 5;
    public static final int CHR_ADD_TYPE_WIFI_STALL = 3;
    public static final int CHR_ADD_TYPE_WIFI_START = 1;
    public int afbNum;
    public int afgNum;
    public int appId;
    public int cellStallNum;
    public int cellStartNum;
    public int cellspNum;
    public int chfNum;
    public int closeCellNum;
    public int closeWiFiNum;
    public int hicsNum;
    public int inKQINum;
    public int mpfNum;
    public int mpsNum;
    public int overKQINum;
    public int rn1Num;
    public int rn2Num;
    public int rn3Num;
    public int rn4Num;
    public int scenceId;
    public int startHicNum;
    public int trffic;
    public int wifiStallNum;
    public int wifiStartNum;
    public int wifispNum;

    public void copyInfo(HwAPPChrStatisInfo info) {
        this.appId = info.appId;
        this.scenceId = info.scenceId;
        this.wifiStartNum = info.wifiStartNum;
        this.cellStartNum = info.cellStartNum;
        this.wifiStallNum = info.wifiStallNum;
        this.cellStallNum = info.cellStallNum;
        this.wifispNum = info.wifispNum;
        this.cellspNum = info.cellspNum;
        this.rn1Num = info.rn1Num;
        this.rn2Num = info.rn2Num;
        this.rn3Num = info.rn3Num;
        this.rn4Num = info.rn4Num;
        this.chfNum = info.chfNum;
        this.mpfNum = info.mpfNum;
        this.mpsNum = info.mpsNum;
        this.afgNum = info.afgNum;
        this.afbNum = info.afbNum;
        this.trffic = info.trffic;
        this.inKQINum = info.inKQINum;
        this.overKQINum = info.overKQINum;
        this.closeCellNum = info.closeCellNum;
        this.closeWiFiNum = info.closeWiFiNum;
        this.startHicNum = info.startHicNum;
        this.hicsNum = info.hicsNum;
    }

    public void printfInfo() {
        HwAPPQoEUtils.logD(HwAPPQoEUtils.TAG, false, "HwAPPChrStatisInfo appId = %{public}d scenceId = %{public}d wifiStartNum = %{public}d cellStartNum = %{public}d wifiStallNum = %{public}d cellStallNum = %{public}d wifispNum = %{public}d cellspNum = %{public}d rn1Num = %{public}d rn2Num = %{public}d rn3Num = %{public}d rn4Num = %{public}d chfNum = %{public}d mpfNum = %{public}d mpsNum = %{public}d afgNum = %{public}d afbNum = %{public}d trffic = %{public}d inKQINum = %{public}d overKQINum = %{public}d closeCellNum = %{public}d closeWiFiNum = %{public}d startHicNum = %{public}d hicsNum = %{public}d", Integer.valueOf(this.appId), Integer.valueOf(this.scenceId), Integer.valueOf(this.wifiStartNum), Integer.valueOf(this.cellStartNum), Integer.valueOf(this.wifiStallNum), Integer.valueOf(this.cellStallNum), Integer.valueOf(this.wifispNum), Integer.valueOf(this.cellspNum), Integer.valueOf(this.rn1Num), Integer.valueOf(this.rn2Num), Integer.valueOf(this.rn3Num), Integer.valueOf(this.rn4Num), Integer.valueOf(this.chfNum), Integer.valueOf(this.mpfNum), Integer.valueOf(this.mpsNum), Integer.valueOf(this.afgNum), Integer.valueOf(this.afbNum), Integer.valueOf(this.trffic), Integer.valueOf(this.inKQINum), Integer.valueOf(this.overKQINum), Integer.valueOf(this.closeCellNum), Integer.valueOf(this.closeWiFiNum), Integer.valueOf(this.startHicNum), Integer.valueOf(this.hicsNum));
    }
}
