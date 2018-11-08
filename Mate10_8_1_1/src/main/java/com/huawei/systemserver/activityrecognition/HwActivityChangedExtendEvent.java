package com.huawei.systemserver.activityrecognition;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;

public class HwActivityChangedExtendEvent implements Parcelable {
    public static final Creator<HwActivityChangedExtendEvent> CREATOR = new Creator<HwActivityChangedExtendEvent>() {
        public HwActivityChangedExtendEvent createFromParcel(Parcel source) {
            HwActivityRecognitionExtendEvent[] activityRecognitionEvents = new HwActivityRecognitionExtendEvent[source.readInt()];
            source.readTypedArray(activityRecognitionEvents, HwActivityRecognitionExtendEvent.CREATOR);
            return new HwActivityChangedExtendEvent(activityRecognitionEvents);
        }

        public HwActivityChangedExtendEvent[] newArray(int size) {
            return new HwActivityChangedExtendEvent[size];
        }
    };
    private final List<HwActivityRecognitionExtendEvent> mActivityRecognitionEvents;

    public HwActivityChangedExtendEvent(HwActivityRecognitionExtendEvent[] activityRecognitionEvents) {
        if (activityRecognitionEvents != null) {
            this.mActivityRecognitionEvents = Arrays.asList(activityRecognitionEvents);
            return;
        }
        throw new InvalidParameterException("Parameter 'activityRecognitionEvents' must not be null.");
    }

    public Iterable<HwActivityRecognitionExtendEvent> getActivityRecognitionExtendEvents() {
        return this.mActivityRecognitionEvents;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        HwActivityRecognitionExtendEvent[] activityRecognitionEventArray = (HwActivityRecognitionExtendEvent[]) this.mActivityRecognitionEvents.toArray(new HwActivityRecognitionExtendEvent[0]);
        parcel.writeInt(activityRecognitionEventArray.length);
        parcel.writeTypedArray(activityRecognitionEventArray, flags);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("[ ActivityChangedExtendEvent:");
        for (HwActivityRecognitionExtendEvent event : this.mActivityRecognitionEvents) {
            builder.append("\n    ");
            builder.append(event.toString());
        }
        builder.append("\n]");
        return builder.toString();
    }
}
