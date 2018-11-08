package tmsdk.common.module.urlcheck;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class WebPageDetail implements Parcelable {
    public static Creator<WebPageDetail> CREATOR = new Creator<WebPageDetail>() {
        public WebPageDetail[] bO(int i) {
            return new WebPageDetail[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return s(parcel);
        }

        public /* synthetic */ Object[] newArray(int i) {
            return bO(i);
        }

        public WebPageDetail s(Parcel parcel) {
            Object -l_2_R = new WebPageDetail();
            -l_2_R.title = parcel.readString();
            -l_2_R.description = parcel.readString();
            -l_2_R.webIconUrl = parcel.readString();
            -l_2_R.screenshotUrl = parcel.readString();
            -l_2_R.maliceType = parcel.readLong();
            -l_2_R.maliceTitle = parcel.readString();
            -l_2_R.maliceBody = parcel.readString();
            -l_2_R.flawName = parcel.readString();
            return -l_2_R;
        }
    };
    public String description = "";
    public String flawName = "";
    public String maliceBody = "";
    public String maliceTitle = "";
    public long maliceType = 0;
    public String screenshotUrl = "";
    public String title = "";
    public String webIconUrl = "";

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.title);
        parcel.writeString(this.description);
        parcel.writeString(this.webIconUrl);
        parcel.writeString(this.screenshotUrl);
        parcel.writeLong(this.maliceType);
        parcel.writeString(this.maliceTitle);
        parcel.writeString(this.maliceBody);
        parcel.writeString(this.flawName);
    }
}
