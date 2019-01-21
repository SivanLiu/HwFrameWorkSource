package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;

public class VoiceSpecificRegistrationStates implements Parcelable {
    public static final Creator<VoiceSpecificRegistrationStates> CREATOR = new Creator<VoiceSpecificRegistrationStates>() {
        public VoiceSpecificRegistrationStates createFromParcel(Parcel source) {
            return new VoiceSpecificRegistrationStates(source, null);
        }

        public VoiceSpecificRegistrationStates[] newArray(int size) {
            return new VoiceSpecificRegistrationStates[size];
        }
    };
    public final boolean cssSupported;
    public final int defaultRoamingIndicator;
    public final int roamingIndicator;
    public final int systemIsInPrl;

    /* synthetic */ VoiceSpecificRegistrationStates(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    VoiceSpecificRegistrationStates(boolean cssSupported, int roamingIndicator, int systemIsInPrl, int defaultRoamingIndicator) {
        this.cssSupported = cssSupported;
        this.roamingIndicator = roamingIndicator;
        this.systemIsInPrl = systemIsInPrl;
        this.defaultRoamingIndicator = defaultRoamingIndicator;
    }

    private VoiceSpecificRegistrationStates(Parcel source) {
        this.cssSupported = source.readBoolean();
        this.roamingIndicator = source.readInt();
        this.systemIsInPrl = source.readInt();
        this.defaultRoamingIndicator = source.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.cssSupported);
        dest.writeInt(this.roamingIndicator);
        dest.writeInt(this.systemIsInPrl);
        dest.writeInt(this.defaultRoamingIndicator);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VoiceSpecificRegistrationStates { mCssSupported=");
        stringBuilder.append(this.cssSupported);
        stringBuilder.append(" mRoamingIndicator=");
        stringBuilder.append(this.roamingIndicator);
        stringBuilder.append(" mSystemIsInPrl=");
        stringBuilder.append(this.systemIsInPrl);
        stringBuilder.append(" mDefaultRoamingIndicator=");
        stringBuilder.append(this.defaultRoamingIndicator);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Boolean.valueOf(this.cssSupported), Integer.valueOf(this.roamingIndicator), Integer.valueOf(this.systemIsInPrl), Integer.valueOf(this.defaultRoamingIndicator)});
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof VoiceSpecificRegistrationStates)) {
            return false;
        }
        VoiceSpecificRegistrationStates other = (VoiceSpecificRegistrationStates) o;
        if (!(this.cssSupported == other.cssSupported && this.roamingIndicator == other.roamingIndicator && this.systemIsInPrl == other.systemIsInPrl && this.defaultRoamingIndicator == other.defaultRoamingIndicator)) {
            z = false;
        }
        return z;
    }
}
