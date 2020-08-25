package com.google.android.startop.iorap;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.server.zrhung.IZRHungService;
import java.util.Objects;

public class ActivityInfo implements Parcelable {
    public static final Creator<ActivityInfo> CREATOR = new Creator<ActivityInfo>() {
        /* class com.google.android.startop.iorap.ActivityInfo.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public ActivityInfo createFromParcel(Parcel in) {
            return new ActivityInfo(in);
        }

        @Override // android.os.Parcelable.Creator
        public ActivityInfo[] newArray(int size) {
            return new ActivityInfo[size];
        }
    };
    public final String activityName;
    public final String packageName;

    public ActivityInfo(String packageName2, String activityName2) {
        this.packageName = packageName2;
        this.activityName = activityName2;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        Objects.requireNonNull(this.packageName, IZRHungService.PARA_PACKAGENAME);
        Objects.requireNonNull(this.activityName, "activityName");
    }

    public String toString() {
        return String.format("{packageName: %s, activityName: %s}", this.packageName, this.activityName);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ActivityInfo) {
            return equals((ActivityInfo) other);
        }
        return false;
    }

    private boolean equals(ActivityInfo other) {
        return Objects.equals(this.packageName, other.packageName) && Objects.equals(this.activityName, other.activityName);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.packageName);
        out.writeString(this.activityName);
    }

    private ActivityInfo(Parcel in) {
        this.packageName = in.readString();
        this.activityName = in.readString();
        checkConstructorArguments();
    }

    public int describeContents() {
        return 0;
    }
}
