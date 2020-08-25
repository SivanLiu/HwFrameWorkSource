package com.android.server.hidata.mplink;

import android.os.Parcel;

public class HwMpLinkInterDisturbInfo {
    public static final int INVALID_VALUE = 0;
    public int mRat = 0;
    public int mUlbw = 0;
    public int mUlfreq = 0;

    public HwMpLinkInterDisturbInfo() {
    }

    public HwMpLinkInterDisturbInfo(Parcel parcel) {
        if (parcel != null) {
            this.mRat = parcel.readInt();
            this.mUlfreq = parcel.readInt();
            this.mUlbw = parcel.readInt();
        }
    }
}
