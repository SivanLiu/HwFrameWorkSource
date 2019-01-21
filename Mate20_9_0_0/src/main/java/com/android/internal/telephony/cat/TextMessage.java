package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class TextMessage implements Parcelable {
    public static final Creator<TextMessage> CREATOR = new Creator<TextMessage>() {
        public TextMessage createFromParcel(Parcel in) {
            return new TextMessage(in, null);
        }

        public TextMessage[] newArray(int size) {
            return new TextMessage[size];
        }
    };
    public Duration duration;
    public Bitmap icon;
    public boolean iconSelfExplanatory;
    public boolean isHighPriority;
    public boolean responseNeeded;
    public String text;
    public String title;
    public boolean userClear;

    /* synthetic */ TextMessage(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    TextMessage() {
        this.title = "";
        this.text = null;
        this.icon = null;
        this.iconSelfExplanatory = false;
        this.isHighPriority = false;
        this.responseNeeded = true;
        this.userClear = false;
        this.duration = null;
    }

    private TextMessage(Parcel in) {
        this.title = "";
        this.text = null;
        this.icon = null;
        boolean z = false;
        this.iconSelfExplanatory = false;
        this.isHighPriority = false;
        this.responseNeeded = true;
        this.userClear = false;
        this.duration = null;
        this.title = in.readString();
        this.text = in.readString();
        this.icon = (Bitmap) in.readParcelable(null);
        this.iconSelfExplanatory = in.readInt() == 1;
        this.isHighPriority = in.readInt() == 1;
        this.responseNeeded = in.readInt() == 1;
        if (in.readInt() == 1) {
            z = true;
        }
        this.userClear = z;
        this.duration = (Duration) in.readParcelable(null);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.text);
        dest.writeParcelable(this.icon, 0);
        dest.writeInt(this.iconSelfExplanatory);
        dest.writeInt(this.isHighPriority);
        dest.writeInt(this.responseNeeded);
        dest.writeInt(this.userClear);
        dest.writeParcelable(this.duration, 0);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("title=");
        stringBuilder.append(this.title);
        stringBuilder.append(" text=");
        stringBuilder.append(this.text);
        stringBuilder.append(" icon=");
        stringBuilder.append(this.icon);
        stringBuilder.append(" iconSelfExplanatory=");
        stringBuilder.append(this.iconSelfExplanatory);
        stringBuilder.append(" isHighPriority=");
        stringBuilder.append(this.isHighPriority);
        stringBuilder.append(" responseNeeded=");
        stringBuilder.append(this.responseNeeded);
        stringBuilder.append(" userClear=");
        stringBuilder.append(this.userClear);
        stringBuilder.append(" duration=");
        stringBuilder.append(this.duration);
        return stringBuilder.toString();
    }
}
