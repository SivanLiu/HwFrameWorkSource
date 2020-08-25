package com.google.android.startop.iorap;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public class ActivityHintEvent implements Parcelable {
    public static final Creator<ActivityHintEvent> CREATOR = new Creator<ActivityHintEvent>() {
        /* class com.google.android.startop.iorap.ActivityHintEvent.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public ActivityHintEvent createFromParcel(Parcel in) {
            return new ActivityHintEvent(in);
        }

        @Override // android.os.Parcelable.Creator
        public ActivityHintEvent[] newArray(int size) {
            return new ActivityHintEvent[size];
        }
    };
    public static final int TYPE_CANCELLED = 1;
    public static final int TYPE_COMPLETED = 2;
    private static final int TYPE_MAX = 3;
    public static final int TYPE_POST_COMPLETED = 3;
    public static final int TYPE_STARTED = 0;
    public final ActivityInfo activityInfo;
    public final int type;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public ActivityHintEvent(int type2, ActivityInfo activityInfo2) {
        this.type = type2;
        this.activityInfo = activityInfo2;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        CheckHelpers.checkTypeInRange(this.type, 3);
        Objects.requireNonNull(this.activityInfo, "activityInfo");
    }

    public String toString() {
        return String.format("{type: %d, activityInfo: %s}", Integer.valueOf(this.type), this.activityInfo);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ActivityHintEvent) {
            return equals((ActivityHintEvent) other);
        }
        return false;
    }

    private boolean equals(ActivityHintEvent other) {
        return this.type == other.type && Objects.equals(this.activityInfo, other.activityInfo);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.type);
        this.activityInfo.writeToParcel(out, flags);
    }

    private ActivityHintEvent(Parcel in) {
        this.type = in.readInt();
        this.activityInfo = ActivityInfo.CREATOR.createFromParcel(in);
        checkConstructorArguments();
    }

    public int describeContents() {
        return 0;
    }
}
