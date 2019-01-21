package android.telephony.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

@SystemApi
public final class EuiccNotification implements Parcelable {
    public static final int ALL_EVENTS = 15;
    public static final Creator<EuiccNotification> CREATOR = new Creator<EuiccNotification>() {
        public EuiccNotification createFromParcel(Parcel source) {
            return new EuiccNotification(source, null);
        }

        public EuiccNotification[] newArray(int size) {
            return new EuiccNotification[size];
        }
    };
    public static final int EVENT_DELETE = 8;
    public static final int EVENT_DISABLE = 4;
    public static final int EVENT_ENABLE = 2;
    public static final int EVENT_INSTALL = 1;
    private final byte[] mData;
    private final int mEvent;
    private final int mSeq;
    private final String mTargetAddr;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Event {
    }

    /* synthetic */ EuiccNotification(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public EuiccNotification(int seq, String targetAddr, int event, byte[] data) {
        this.mSeq = seq;
        this.mTargetAddr = targetAddr;
        this.mEvent = event;
        this.mData = data;
    }

    public int getSeq() {
        return this.mSeq;
    }

    public String getTargetAddr() {
        return this.mTargetAddr;
    }

    public int getEvent() {
        return this.mEvent;
    }

    public byte[] getData() {
        return this.mData;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EuiccNotification that = (EuiccNotification) obj;
        if (!(this.mSeq == that.mSeq && Objects.equals(this.mTargetAddr, that.mTargetAddr) && this.mEvent == that.mEvent && Arrays.equals(this.mData, that.mData))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * 1) + this.mSeq)) + Objects.hashCode(this.mTargetAddr))) + this.mEvent)) + Arrays.hashCode(this.mData);
    }

    public String toString() {
        String str;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("EuiccNotification (seq=");
        stringBuilder.append(this.mSeq);
        stringBuilder.append(", targetAddr=");
        stringBuilder.append(this.mTargetAddr);
        stringBuilder.append(", event=");
        stringBuilder.append(this.mEvent);
        stringBuilder.append(", data=");
        if (this.mData == null) {
            str = "null";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("byte[");
            stringBuilder2.append(this.mData.length);
            stringBuilder2.append("]");
            str = stringBuilder2.toString();
        }
        stringBuilder.append(str);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSeq);
        dest.writeString(this.mTargetAddr);
        dest.writeInt(this.mEvent);
        dest.writeByteArray(this.mData);
    }

    private EuiccNotification(Parcel source) {
        this.mSeq = source.readInt();
        this.mTargetAddr = source.readString();
        this.mEvent = source.readInt();
        this.mData = source.createByteArray();
    }
}
