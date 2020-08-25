package com.huawei.hwwifiproservice;

public class WifiProApQualityRcd {
    public static final int BLOB_RCD_DOUBLE_LEN = 368;
    public static final int BLOB_RCD_LONG_LEN = 368;
    private static final int LENGTH_OF_DOUBLE = 8;
    private static final int LENGTH_OF_LONG = 8;
    private static final int RECORD_COUNT = 46;
    private static final String TAG = "WifiProApQualityRcd";
    public String apBSSID;
    public byte[] mHistoryAvgRtt;
    public byte[] mOtaBadPktProduct;
    public byte[] mOtaLostRateValue;
    public byte[] mOtaPktVolume;
    public byte[] mRttPacketVolume;
    public byte[] mRttProduct;

    public WifiProApQualityRcd(String bssid) {
        resetAllParameters(bssid);
    }

    private void resetAllParameters(String bssid) {
        this.apBSSID = "DEAULT_STR";
        if (bssid != null) {
            this.apBSSID = bssid;
        }
        this.mRttProduct = new byte[368];
        this.mRttPacketVolume = new byte[368];
        this.mHistoryAvgRtt = new byte[368];
        this.mOtaLostRateValue = new byte[368];
        this.mOtaPktVolume = new byte[368];
        this.mOtaBadPktProduct = new byte[368];
    }

    public static byte[] getByteArray(long l) {
        return new byte[]{(byte) ((int) ((l >> 56) & 255)), (byte) ((int) ((l >> 48) & 255)), (byte) ((int) ((l >> 40) & 255)), (byte) ((int) ((l >> 32) & 255)), (byte) ((int) ((l >> 24) & 255)), (byte) ((int) ((l >> 16) & 255)), (byte) ((int) ((l >> 8) & 255)), (byte) ((int) (l & 255))};
    }

    public static long getLong(byte[] arr, int index) {
        int offset = index * 8;
        return ((((long) arr[offset + 0]) << 56) & -72057594037927936L) | ((((long) arr[offset + 1]) << 48) & 71776119061217280L) | ((((long) arr[offset + 2]) << 40) & 280375465082880L) | ((((long) arr[offset + 3]) << 32) & 1095216660480L) | ((((long) arr[offset + 4]) << 24) & 4278190080L) | ((((long) arr[offset + 5]) << 16) & 16711680) | ((((long) arr[offset + 6]) << 8) & 65280) | (((long) arr[offset + 7]) & 255);
    }

    public static byte[] getByteArray(double d) {
        return getByteArray(Double.doubleToLongBits(d));
    }

    public static double getDouble(byte[] arr, int index) {
        return Double.longBitsToDouble(getLong(arr, index));
    }

    private void putValueToRecord(byte[] dst, byte[] src, int offset) {
        System.arraycopy(src, 0, dst, offset, src.length);
    }

    public long getAvgRttFromRecord(int index) {
        return getLong(this.mHistoryAvgRtt, index);
    }

    public void putAvgRttToRecord(long rtt, int index) {
        putValueToRecord(this.mHistoryAvgRtt, getByteArray(rtt), index * 8);
    }

    public long getRttProductFromRecord(int index) {
        return getLong(this.mRttProduct, index);
    }

    public void putRttProductToRecord(long product, int index) {
        putValueToRecord(this.mRttProduct, getByteArray(product), index * 8);
    }

    public long getRttVolumeFromRecord(int index) {
        return getLong(this.mRttPacketVolume, index);
    }

    public void putRttVolumeToRecord(long volume, int index) {
        putValueToRecord(this.mRttPacketVolume, getByteArray(volume), index * 8);
    }

    public double getLostRateFromRecord(int index) {
        return getDouble(this.mOtaLostRateValue, index);
    }

    public void putLostRateToRecord(double lostRate, int index) {
        putValueToRecord(this.mOtaLostRateValue, getByteArray(lostRate), index * 8);
    }

    public double getLostVolumeFromRecord(int index) {
        return getDouble(this.mOtaPktVolume, index);
    }

    public void putLostVolumeToRecord(double volume, int index) {
        putValueToRecord(this.mOtaPktVolume, getByteArray(volume), index * 8);
    }

    public double getLostProductFromRecord(int index) {
        return getDouble(this.mOtaBadPktProduct, index);
    }

    public void putLostProductToRecord(double product, int index) {
        putValueToRecord(this.mOtaBadPktProduct, getByteArray(product), index * 8);
    }
}
