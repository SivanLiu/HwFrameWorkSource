package android.telephony.ims;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

@SystemApi
public final class ImsSsInfo implements Parcelable {
    public static final Creator<ImsSsInfo> CREATOR = new Creator<ImsSsInfo>() {
        public ImsSsInfo createFromParcel(Parcel in) {
            return new ImsSsInfo(in, null);
        }

        public ImsSsInfo[] newArray(int size) {
            return new ImsSsInfo[size];
        }
    };
    public static final int DISABLED = 0;
    public static final int ENABLED = 1;
    public static final int NOT_REGISTERED = -1;
    public String mIcbNum;
    public int mStatus;

    public ImsSsInfo(int status, String icbNum) {
        this.mStatus = status;
        this.mIcbNum = icbNum;
    }

    private ImsSsInfo(Parcel in) {
        readFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mStatus);
        out.writeString(this.mIcbNum);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append(", Status: ");
        stringBuilder.append(this.mStatus == 0 ? "disabled" : "enabled");
        return stringBuilder.toString();
    }

    private void readFromParcel(Parcel in) {
        this.mStatus = in.readInt();
        this.mIcbNum = in.readString();
    }

    public int getStatus() {
        return this.mStatus;
    }

    public String getIcbNum() {
        return this.mIcbNum;
    }
}
