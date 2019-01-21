package com.vzw.nfc;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;

public final class RouteEntry implements Parcelable {
    public static final Creator<RouteEntry> CREATOR = new Creator<RouteEntry>() {
        public RouteEntry createFromParcel(Parcel in) {
            boolean z = true;
            if (((byte) in.readInt()) != (byte) 1) {
                z = false;
            }
            boolean allowed = z;
            int location = in.readInt();
            int powerState = in.readInt();
            byte[] aid = new byte[in.readInt()];
            in.readByteArray(aid);
            return new RouteEntry(aid, powerState, location, allowed);
        }

        public RouteEntry[] newArray(int size) {
            return new RouteEntry[size];
        }
    };
    byte[] mAid;
    boolean mAllowed;
    int mLocation;
    int mPowerState;

    public RouteEntry(byte[] Aid, int PowerState, int Location, boolean allowed) {
        this.mAid = Aid;
        this.mPowerState = PowerState;
        this.mLocation = Location;
        this.mAllowed = allowed;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("constructor mPowerState");
        stringBuilder.append(PowerState);
        Log.d("RouteEntry", stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("constructor mPowerState");
        stringBuilder.append(this.mPowerState);
        Log.d("RouteEntry", stringBuilder.toString());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i = 1;
        if (true != this.mAllowed) {
            i = 0;
        }
        dest.writeInt(i);
        dest.writeInt(this.mLocation);
        dest.writeInt(this.mPowerState);
        dest.writeInt(this.mAid.length);
        dest.writeByteArray(this.mAid);
    }

    public byte[] getAid() {
        return this.mAid;
    }

    public int getPowerState() {
        return this.mPowerState;
    }

    public int getLocation() {
        return this.mLocation;
    }

    public boolean isAllowed() {
        return this.mAllowed;
    }
}
