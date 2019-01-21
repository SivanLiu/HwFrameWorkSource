package android.service.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;
import java.util.List;

@SystemApi
public final class GetEuiccProfileInfoListResult implements Parcelable {
    public static final Creator<GetEuiccProfileInfoListResult> CREATOR = new Creator<GetEuiccProfileInfoListResult>() {
        public GetEuiccProfileInfoListResult createFromParcel(Parcel in) {
            return new GetEuiccProfileInfoListResult(in, null);
        }

        public GetEuiccProfileInfoListResult[] newArray(int size) {
            return new GetEuiccProfileInfoListResult[size];
        }
    };
    private final boolean mIsRemovable;
    private final EuiccProfileInfo[] mProfiles;
    @Deprecated
    public final int result;

    /* synthetic */ GetEuiccProfileInfoListResult(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public int getResult() {
        return this.result;
    }

    public List<EuiccProfileInfo> getProfiles() {
        if (this.mProfiles == null) {
            return null;
        }
        return Arrays.asList(this.mProfiles);
    }

    public boolean getIsRemovable() {
        return this.mIsRemovable;
    }

    public GetEuiccProfileInfoListResult(int result, EuiccProfileInfo[] profiles, boolean isRemovable) {
        this.result = result;
        this.mIsRemovable = isRemovable;
        if (this.result == 0) {
            this.mProfiles = profiles;
        } else if (profiles == null) {
            this.mProfiles = null;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error result with non-null profiles: ");
            stringBuilder.append(result);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private GetEuiccProfileInfoListResult(Parcel in) {
        this.result = in.readInt();
        this.mProfiles = (EuiccProfileInfo[]) in.createTypedArray(EuiccProfileInfo.CREATOR);
        this.mIsRemovable = in.readBoolean();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.result);
        dest.writeTypedArray(this.mProfiles, flags);
        dest.writeBoolean(this.mIsRemovable);
    }

    public int describeContents() {
        return 0;
    }
}
