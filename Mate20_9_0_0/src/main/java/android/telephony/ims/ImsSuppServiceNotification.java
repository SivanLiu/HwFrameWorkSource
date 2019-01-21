package android.telephony.ims;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telephony.PhoneNumberUtils;
import java.util.Arrays;

@SystemApi
public final class ImsSuppServiceNotification implements Parcelable {
    public static final Creator<ImsSuppServiceNotification> CREATOR = new Creator<ImsSuppServiceNotification>() {
        public ImsSuppServiceNotification createFromParcel(Parcel in) {
            return new ImsSuppServiceNotification(in);
        }

        public ImsSuppServiceNotification[] newArray(int size) {
            return new ImsSuppServiceNotification[size];
        }
    };
    private static final String TAG = "ImsSuppServiceNotification";
    public final int code;
    public final String[] history;
    public final int index;
    public final int notificationType;
    public final String number;
    public final int type;

    public ImsSuppServiceNotification(int notificationType, int code, int index, int type, String number, String[] history) {
        this.notificationType = notificationType;
        this.code = code;
        this.index = index;
        this.type = type;
        this.number = number;
        this.history = history;
    }

    public ImsSuppServiceNotification(Parcel in) {
        this.notificationType = in.readInt();
        this.code = in.readInt();
        this.index = in.readInt();
        this.type = in.readInt();
        this.number = in.readString();
        this.history = in.createStringArray();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ notificationType=");
        stringBuilder.append(this.notificationType);
        stringBuilder.append(", code=");
        stringBuilder.append(this.code);
        stringBuilder.append(", index=");
        stringBuilder.append(this.index);
        stringBuilder.append(", type=");
        stringBuilder.append(this.type);
        stringBuilder.append(", number=");
        stringBuilder.append(PhoneNumberUtils.toLogSafePhoneNumber(this.number));
        stringBuilder.append(", history=");
        stringBuilder.append(PhoneNumberUtils.toLogSafePhoneNumber(Arrays.toString(this.history)));
        stringBuilder.append(" }");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.notificationType);
        out.writeInt(this.code);
        out.writeInt(this.index);
        out.writeInt(this.type);
        out.writeString(this.number);
        out.writeStringArray(this.history);
    }
}
