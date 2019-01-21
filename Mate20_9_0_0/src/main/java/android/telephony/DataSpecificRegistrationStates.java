package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;

public class DataSpecificRegistrationStates implements Parcelable {
    public static final Creator<DataSpecificRegistrationStates> CREATOR = new Creator<DataSpecificRegistrationStates>() {
        public DataSpecificRegistrationStates createFromParcel(Parcel source) {
            return new DataSpecificRegistrationStates(source, null);
        }

        public DataSpecificRegistrationStates[] newArray(int size) {
            return new DataSpecificRegistrationStates[size];
        }
    };
    public final int maxDataCalls;

    /* synthetic */ DataSpecificRegistrationStates(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    DataSpecificRegistrationStates(int maxDataCalls) {
        this.maxDataCalls = maxDataCalls;
    }

    private DataSpecificRegistrationStates(Parcel source) {
        this.maxDataCalls = source.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.maxDataCalls);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DataSpecificRegistrationStates { mMaxDataCalls=");
        stringBuilder.append(this.maxDataCalls);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.maxDataCalls)});
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof DataSpecificRegistrationStates)) {
            return false;
        }
        if (this.maxDataCalls != ((DataSpecificRegistrationStates) o).maxDataCalls) {
            z = false;
        }
        return z;
    }
}
