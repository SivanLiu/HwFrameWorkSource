package com.huawei.systemmanager.power;

import com.huawei.pgmng.plug.DetailBatterySipper;
import com.huawei.pgmng.plug.DetailBatterySipper.NetEntry;

public class HwDetailBatterySipper {
    public static final int BG_CPU_TIME = 1;
    public static final int BG_GPS_TIME = 4;
    public static final int BG_Wl_TIME = 0;
    public static final int FG_CPU_TIME = 2;
    public static final int FG_GPS_TIME = 3;
    public static final int FG_SCREEN_TIME = 5;
    public static final int MOBILE_ENTRY = 11;
    public static final int NET_ENTRY_RX_BYTES = 0;
    public static final int NET_ENTRY_RX_PACKETS = 2;
    public static final int NET_ENTRY_TX_BYTES = 1;
    public static final int NET_ENTRY_TX_PACKETS = 3;
    public static final int WIFI_ENTRY = 12;
    private DetailBatterySipper mLocalDetailBatterySipper = null;

    public static class NetEntryEx {
        private NetEntry mLocalNetEntry;

        public NetEntryEx(NetEntry netEntry) {
            this.mLocalNetEntry = netEntry;
        }

        public long getItem(int index) {
            return this.mLocalNetEntry.mItem[index];
        }
    }

    public HwDetailBatterySipper(DetailBatterySipper sipper) {
        this.mLocalDetailBatterySipper = sipper;
    }

    public float getTotalPower() {
        return this.mLocalDetailBatterySipper.getTotalPower();
    }

    public float getDistributedPower() {
        return this.mLocalDetailBatterySipper.getDistributedPower();
    }

    public int getUid() {
        return this.mLocalDetailBatterySipper.mUid;
    }

    public String getName() {
        return this.mLocalDetailBatterySipper.mName;
    }

    public long getTimeOfSipper(int index) {
        switch (index) {
            case 0:
                return this.mLocalDetailBatterySipper.mBgWlTime;
            case 1:
                return this.mLocalDetailBatterySipper.mBgCpuTime;
            case 2:
                return this.mLocalDetailBatterySipper.mFgCpuTime;
            case 3:
                return this.mLocalDetailBatterySipper.mFgGpsTime;
            case 4:
                return this.mLocalDetailBatterySipper.mBgGpsTime;
            case 5:
                return this.mLocalDetailBatterySipper.mFgScreenTime;
            default:
                return -1;
        }
    }

    public NetEntryEx[] getEntryArray(int index) {
        NetEntry[] netarray = null;
        if (index == 11) {
            netarray = this.mLocalDetailBatterySipper.mMobileEntry;
        } else if (index == 12) {
            netarray = this.mLocalDetailBatterySipper.mWifiEntry;
        }
        if (netarray == null) {
            return null;
        }
        NetEntryEx[] array = new NetEntryEx[netarray.length];
        for (int i = 0; i < netarray.length; i++) {
            array[i] = new NetEntryEx(netarray[i]);
        }
        return array;
    }
}
