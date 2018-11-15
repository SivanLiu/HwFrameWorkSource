package com.huawei.systemserver.activityrecognition;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;

public class HwEnvironmentChangedEvent implements Parcelable {
    public static final Creator<HwEnvironmentChangedEvent> CREATOR = new Creator<HwEnvironmentChangedEvent>() {
        public HwEnvironmentChangedEvent createFromParcel(Parcel source) {
            HwActivityRecognitionExtendEvent[] activityRecognitionEvents = new HwActivityRecognitionExtendEvent[source.readInt()];
            source.readTypedArray(activityRecognitionEvents, HwActivityRecognitionExtendEvent.CREATOR);
            return new HwEnvironmentChangedEvent(activityRecognitionEvents);
        }

        public HwEnvironmentChangedEvent[] newArray(int size) {
            return new HwEnvironmentChangedEvent[size];
        }
    };
    private final List<HwActivityRecognitionExtendEvent> mEnvironmentRecognitionEvents;

    public HwEnvironmentChangedEvent(HwActivityRecognitionExtendEvent[] environmentRecognitionEvents) {
        if (environmentRecognitionEvents != null) {
            this.mEnvironmentRecognitionEvents = Arrays.asList(environmentRecognitionEvents);
            return;
        }
        throw new InvalidParameterException("Parameter 'environmentRecognitionEvents' must not be null.");
    }

    public Iterable<HwActivityRecognitionExtendEvent> getEnvironmentRecognitionEvents() {
        return this.mEnvironmentRecognitionEvents;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        HwActivityRecognitionExtendEvent[] activityRecognitionEventArray = (HwActivityRecognitionExtendEvent[]) this.mEnvironmentRecognitionEvents.toArray(new HwActivityRecognitionExtendEvent[0]);
        parcel.writeInt(activityRecognitionEventArray.length);
        parcel.writeTypedArray(activityRecognitionEventArray, flags);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("[ EnvironmentChangedEvent:");
        for (HwActivityRecognitionExtendEvent event : this.mEnvironmentRecognitionEvents) {
            builder.append("\n    ");
            builder.append(event.toString());
        }
        builder.append("\n]");
        return builder.toString();
    }
}
