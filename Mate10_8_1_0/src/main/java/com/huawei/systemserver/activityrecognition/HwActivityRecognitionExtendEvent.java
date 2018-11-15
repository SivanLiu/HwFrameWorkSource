package com.huawei.systemserver.activityrecognition;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class HwActivityRecognitionExtendEvent implements Parcelable {
    public static final Creator<HwActivityRecognitionExtendEvent> CREATOR = new Creator<HwActivityRecognitionExtendEvent>() {
        public HwActivityRecognitionExtendEvent createFromParcel(Parcel source) {
            return new HwActivityRecognitionExtendEvent(source.readString(), source.readInt(), source.readLong(), (OtherParameters) source.readParcelable(OtherParameters.class.getClassLoader()));
        }

        public HwActivityRecognitionExtendEvent[] newArray(int size) {
            return new HwActivityRecognitionExtendEvent[size];
        }
    };
    private final String mActivity;
    private final int mEventType;
    private final OtherParameters mOtherParams;
    private final long mTimestampNs;

    public HwActivityRecognitionExtendEvent(String activity, int eventType, long timestampNs, OtherParameters otherParams) {
        this.mActivity = activity;
        this.mEventType = eventType;
        this.mTimestampNs = timestampNs;
        this.mOtherParams = otherParams;
    }

    public String getActivity() {
        return this.mActivity;
    }

    public int getEventType() {
        return this.mEventType;
    }

    public long getTimestampNs() {
        return this.mTimestampNs;
    }

    public OtherParameters getOtherParams() {
        return this.mOtherParams;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mActivity);
        parcel.writeInt(this.mEventType);
        parcel.writeLong(this.mTimestampNs);
        parcel.writeParcelable(this.mOtherParams, flags);
    }

    public String toString() {
        if (this.mOtherParams == null) {
            return String.format("Activity='%s', EventType=%s, TimestampNs=%s", new Object[]{this.mActivity, Integer.valueOf(this.mEventType), Long.valueOf(this.mTimestampNs)});
        }
        return String.format("Activity='%s', EventType=%s, TimestampNs=%s, Param1=%s, Param2=%s, Param3=%s, Param4=%s, Param5=%s", new Object[]{this.mActivity, Integer.valueOf(this.mEventType), Long.valueOf(this.mTimestampNs), Double.valueOf(this.mOtherParams.getmParam1()), Double.valueOf(this.mOtherParams.getmParam2()), Double.valueOf(this.mOtherParams.getmParam3()), Double.valueOf(this.mOtherParams.getmParam4()), this.mOtherParams.getmParam5()});
    }
}
