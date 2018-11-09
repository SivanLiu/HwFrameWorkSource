package android.service.euicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class GetEuiccProfileInfoListResult implements Parcelable {
    public static final Creator<GetEuiccProfileInfoListResult> CREATOR = new Creator<GetEuiccProfileInfoListResult>() {
        public GetEuiccProfileInfoListResult createFromParcel(Parcel in) {
            return new GetEuiccProfileInfoListResult(in);
        }

        public GetEuiccProfileInfoListResult[] newArray(int size) {
            return new GetEuiccProfileInfoListResult[size];
        }
    };
    public final boolean isRemovable;
    public final EuiccProfileInfo[] profiles;
    public final int result;

    public GetEuiccProfileInfoListResult(int result, EuiccProfileInfo[] profiles, boolean isRemovable) {
        this.result = result;
        this.isRemovable = isRemovable;
        if (this.result == 0) {
            this.profiles = profiles;
        } else if (profiles != null) {
            throw new IllegalArgumentException("Error result with non-null profiles: " + result);
        } else {
            this.profiles = null;
        }
    }

    private GetEuiccProfileInfoListResult(Parcel in) {
        this.result = in.readInt();
        this.profiles = (EuiccProfileInfo[]) in.createTypedArray(EuiccProfileInfo.CREATOR);
        this.isRemovable = in.readBoolean();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.result);
        dest.writeTypedArray(this.profiles, flags);
        dest.writeBoolean(this.isRemovable);
    }

    public int describeContents() {
        return 0;
    }
}
