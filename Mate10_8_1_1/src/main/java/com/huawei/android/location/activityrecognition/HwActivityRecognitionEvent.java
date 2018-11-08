package com.huawei.android.location.activityrecognition;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class HwActivityRecognitionEvent implements Parcelable {
    public static final Creator<HwActivityRecognitionEvent> CREATOR = new Creator<HwActivityRecognitionEvent>() {
        public HwActivityRecognitionEvent createFromParcel(Parcel source) {
            return new HwActivityRecognitionEvent(source.readString(), source.readInt(), source.readLong());
        }

        public HwActivityRecognitionEvent[] newArray(int size) {
            return new HwActivityRecognitionEvent[size];
        }
    };
    private final String mActivity;
    private final int mEventType;
    private final long mTimestampNs;

    public HwActivityRecognitionEvent(String activity, int eventType, long timestampNs) {
        this.mActivity = activity;
        this.mEventType = eventType;
        this.mTimestampNs = timestampNs;
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

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mActivity);
        parcel.writeInt(this.mEventType);
        parcel.writeLong(this.mTimestampNs);
    }

    public String toString() {
        return String.format("Activity='%s', EventType=%s, TimestampNs=%s", new Object[]{this.mActivity, Integer.valueOf(this.mEventType), Long.valueOf(this.mTimestampNs)});
    }
}
