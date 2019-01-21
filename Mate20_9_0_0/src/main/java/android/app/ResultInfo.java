package android.app;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;

public class ResultInfo implements Parcelable {
    public static final Creator<ResultInfo> CREATOR = new Creator<ResultInfo>() {
        public ResultInfo createFromParcel(Parcel in) {
            return new ResultInfo(in);
        }

        public ResultInfo[] newArray(int size) {
            return new ResultInfo[size];
        }
    };
    public final Intent mData;
    public final int mRequestCode;
    public final int mResultCode;
    public final String mResultWho;

    public ResultInfo(String resultWho, int requestCode, int resultCode, Intent data) {
        this.mResultWho = resultWho;
        this.mRequestCode = requestCode;
        this.mResultCode = resultCode;
        this.mData = data;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ResultInfo{who=");
        stringBuilder.append(this.mResultWho);
        stringBuilder.append(", request=");
        stringBuilder.append(this.mRequestCode);
        stringBuilder.append(", result=");
        stringBuilder.append(this.mResultCode);
        stringBuilder.append(", data=");
        stringBuilder.append(this.mData);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mResultWho);
        out.writeInt(this.mRequestCode);
        out.writeInt(this.mResultCode);
        if (this.mData != null) {
            out.writeInt(1);
            this.mData.writeToParcel(out, 0);
            return;
        }
        out.writeInt(0);
    }

    public ResultInfo(Parcel in) {
        this.mResultWho = in.readString();
        this.mRequestCode = in.readInt();
        this.mResultCode = in.readInt();
        if (in.readInt() != 0) {
            this.mData = (Intent) Intent.CREATOR.createFromParcel(in);
        } else {
            this.mData = null;
        }
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null || !(obj instanceof ResultInfo)) {
            return false;
        }
        ResultInfo other = (ResultInfo) obj;
        boolean intentsEqual = this.mData == null ? other.mData == null : this.mData.filterEquals(other.mData);
        if (intentsEqual && Objects.equals(this.mResultWho, other.mResultWho) && this.mResultCode == other.mResultCode && this.mRequestCode == other.mRequestCode) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        int result = (31 * ((31 * ((31 * 17) + this.mRequestCode)) + this.mResultCode)) + Objects.hashCode(this.mResultWho);
        if (this.mData != null) {
            return (31 * result) + this.mData.filterHashCode();
        }
        return result;
    }
}
