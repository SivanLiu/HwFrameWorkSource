package android.service.euicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;

public final class EuiccProfileInfo implements Parcelable {
    public static final Creator<EuiccProfileInfo> CREATOR = new Creator<EuiccProfileInfo>() {
        public EuiccProfileInfo createFromParcel(Parcel in) {
            return new EuiccProfileInfo(in);
        }

        public EuiccProfileInfo[] newArray(int size) {
            return new EuiccProfileInfo[size];
        }
    };
    public final UiccAccessRule[] accessRules;
    public final String iccid;
    public final String nickname;

    public EuiccProfileInfo(String iccid, UiccAccessRule[] accessRules, String nickname) {
        if (TextUtils.isDigitsOnly(iccid)) {
            this.iccid = iccid;
            this.accessRules = accessRules;
            this.nickname = nickname;
            return;
        }
        throw new IllegalArgumentException("iccid contains invalid characters: " + iccid);
    }

    private EuiccProfileInfo(Parcel in) {
        this.iccid = in.readString();
        this.accessRules = (UiccAccessRule[]) in.createTypedArray(UiccAccessRule.CREATOR);
        this.nickname = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.iccid);
        dest.writeTypedArray(this.accessRules, flags);
        dest.writeString(this.nickname);
    }

    public int describeContents() {
        return 0;
    }
}
