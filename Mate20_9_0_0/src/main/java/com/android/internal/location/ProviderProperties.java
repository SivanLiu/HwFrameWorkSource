package com.android.internal.location;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class ProviderProperties implements Parcelable {
    public static final Creator<ProviderProperties> CREATOR = new Creator<ProviderProperties>() {
        public ProviderProperties createFromParcel(Parcel in) {
            return new ProviderProperties(in.readInt() == 1, in.readInt() == 1, in.readInt() == 1, in.readInt() == 1, in.readInt() == 1, in.readInt() == 1, in.readInt() == 1, in.readInt(), in.readInt());
        }

        public ProviderProperties[] newArray(int size) {
            return new ProviderProperties[size];
        }
    };
    public final int mAccuracy;
    public final boolean mHasMonetaryCost;
    public final int mPowerRequirement;
    public final boolean mRequiresCell;
    public final boolean mRequiresNetwork;
    public final boolean mRequiresSatellite;
    public final boolean mSupportsAltitude;
    public final boolean mSupportsBearing;
    public final boolean mSupportsSpeed;

    public ProviderProperties(boolean mRequiresNetwork, boolean mRequiresSatellite, boolean mRequiresCell, boolean mHasMonetaryCost, boolean mSupportsAltitude, boolean mSupportsSpeed, boolean mSupportsBearing, int mPowerRequirement, int mAccuracy) {
        this.mRequiresNetwork = mRequiresNetwork;
        this.mRequiresSatellite = mRequiresSatellite;
        this.mRequiresCell = mRequiresCell;
        this.mHasMonetaryCost = mHasMonetaryCost;
        this.mSupportsAltitude = mSupportsAltitude;
        this.mSupportsSpeed = mSupportsSpeed;
        this.mSupportsBearing = mSupportsBearing;
        this.mPowerRequirement = mPowerRequirement;
        this.mAccuracy = mAccuracy;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.mRequiresNetwork);
        parcel.writeInt(this.mRequiresSatellite);
        parcel.writeInt(this.mRequiresCell);
        parcel.writeInt(this.mHasMonetaryCost);
        parcel.writeInt(this.mSupportsAltitude);
        parcel.writeInt(this.mSupportsSpeed);
        parcel.writeInt(this.mSupportsBearing);
        parcel.writeInt(this.mPowerRequirement);
        parcel.writeInt(this.mAccuracy);
    }
}
