package com.android.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class CallForwardInfo implements Parcelable {
    public static final Creator<CallForwardInfo> CREATOR = new Creator<CallForwardInfo>() {
        public CallForwardInfo createFromParcel(Parcel source) {
            CallForwardInfo cFInfor = new CallForwardInfo();
            cFInfor.readFromParcel(source);
            return cFInfor;
        }

        public CallForwardInfo[] newArray(int size) {
            return new CallForwardInfo[size];
        }
    };
    private static final String TAG = "CallForwardInfo";
    public int endHour;
    public int endMinute;
    public String number;
    public int reason;
    public int serviceClass;
    public int startHour;
    public int startMinute;
    public int status;
    public int timeSeconds;
    public int toa;

    public String toString() {
        return "[CallForwardInfo: status=" + (this.status == 0 ? " not active " : " active ") + " reason: " + this.reason + " serviceClass: " + this.serviceClass + this.timeSeconds + " seconds" + ", startHour=" + this.startHour + ", startMinute=" + this.startMinute + ", endHour=" + this.endHour + ", endMinute=" + this.endMinute;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.status);
        dest.writeInt(this.reason);
        dest.writeInt(this.serviceClass);
        dest.writeInt(this.toa);
        dest.writeString(this.number);
        dest.writeInt(this.timeSeconds);
        dest.writeInt(this.startHour);
        dest.writeInt(this.startMinute);
        dest.writeInt(this.endHour);
        dest.writeInt(this.endMinute);
    }

    private void readFromParcel(Parcel source) {
        this.status = source.readInt();
        this.reason = source.readInt();
        this.serviceClass = source.readInt();
        this.toa = source.readInt();
        this.number = source.readString();
        this.timeSeconds = source.readInt();
        this.startHour = source.readInt();
        this.startMinute = source.readInt();
        this.endHour = source.readInt();
        this.endMinute = source.readInt();
    }
}
