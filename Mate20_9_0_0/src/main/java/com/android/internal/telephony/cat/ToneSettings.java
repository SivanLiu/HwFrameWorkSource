package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ToneSettings implements Parcelable {
    public static final Creator<ToneSettings> CREATOR = new Creator<ToneSettings>() {
        public ToneSettings createFromParcel(Parcel in) {
            return new ToneSettings(in, null);
        }

        public ToneSettings[] newArray(int size) {
            return new ToneSettings[size];
        }
    };
    public Duration duration;
    public Tone tone;
    public boolean vibrate;

    /* synthetic */ ToneSettings(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public ToneSettings(Duration duration, Tone tone, boolean vibrate) {
        this.duration = duration;
        this.tone = tone;
        this.vibrate = vibrate;
    }

    private ToneSettings(Parcel in) {
        this.duration = (Duration) in.readParcelable(null);
        this.tone = (Tone) in.readParcelable(null);
        boolean z = true;
        if (in.readInt() != 1) {
            z = false;
        }
        this.vibrate = z;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.duration, 0);
        dest.writeParcelable(this.tone, 0);
        dest.writeInt(this.vibrate);
    }
}
