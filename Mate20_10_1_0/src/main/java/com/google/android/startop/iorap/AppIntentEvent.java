package com.google.android.startop.iorap;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public class AppIntentEvent implements Parcelable {
    public static final Creator<AppIntentEvent> CREATOR = new Creator<AppIntentEvent>() {
        /* class com.google.android.startop.iorap.AppIntentEvent.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AppIntentEvent createFromParcel(Parcel in) {
            return new AppIntentEvent(in);
        }

        @Override // android.os.Parcelable.Creator
        public AppIntentEvent[] newArray(int size) {
            return new AppIntentEvent[size];
        }
    };
    public static final int TYPE_DEFAULT_INTENT_CHANGED = 0;
    private static final int TYPE_MAX = 0;
    public final ActivityInfo newActivityInfo;
    public final ActivityInfo oldActivityInfo;
    public final int type;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public static AppIntentEvent createDefaultIntentChanged(ActivityInfo oldActivityInfo2, ActivityInfo newActivityInfo2) {
        return new AppIntentEvent(0, oldActivityInfo2, newActivityInfo2);
    }

    private AppIntentEvent(int type2, ActivityInfo oldActivityInfo2, ActivityInfo newActivityInfo2) {
        this.type = type2;
        this.oldActivityInfo = oldActivityInfo2;
        this.newActivityInfo = newActivityInfo2;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        CheckHelpers.checkTypeInRange(this.type, 0);
        Objects.requireNonNull(this.oldActivityInfo, "oldActivityInfo");
        Objects.requireNonNull(this.oldActivityInfo, "newActivityInfo");
    }

    public String toString() {
        return String.format("{oldActivityInfo: %s, newActivityInfo: %s}", this.oldActivityInfo, this.newActivityInfo);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof AppIntentEvent) {
            return equals((AppIntentEvent) other);
        }
        return false;
    }

    private boolean equals(AppIntentEvent other) {
        return this.type == other.type && Objects.equals(this.oldActivityInfo, other.oldActivityInfo) && Objects.equals(this.newActivityInfo, other.newActivityInfo);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.type);
        this.oldActivityInfo.writeToParcel(out, flags);
        this.newActivityInfo.writeToParcel(out, flags);
    }

    private AppIntentEvent(Parcel in) {
        this.type = in.readInt();
        this.oldActivityInfo = ActivityInfo.CREATOR.createFromParcel(in);
        this.newActivityInfo = ActivityInfo.CREATOR.createFromParcel(in);
        checkConstructorArguments();
    }

    public int describeContents() {
        return 0;
    }
}
