package tmsdk.common.module.urlcheck;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;

public class ApkDetail implements Parcelable {
    public static Creator<ApkDetail> CREATOR = new Creator<ApkDetail>() {
        public ApkDetail[] bL(int i) {
            return new ApkDetail[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return p(parcel);
        }

        public /* synthetic */ Object[] newArray(int i) {
            return bL(i);
        }

        public ApkDetail p(Parcel parcel) {
            boolean z = false;
            Object -l_2_R = new ApkDetail();
            -l_2_R.apkPackage = parcel.readString();
            -l_2_R.apkName = parcel.readString();
            -l_2_R.iconUrl = parcel.readString();
            -l_2_R.versionCode = parcel.readInt();
            -l_2_R.versionName = parcel.readString();
            -l_2_R.size = parcel.readLong();
            -l_2_R.official = parcel.readInt();
            -l_2_R.developer = parcel.readString();
            -l_2_R.certMD5 = parcel.readString();
            if (parcel.readInt() != 0) {
                z = true;
            }
            -l_2_R.isInSoftwareDB = z;
            -l_2_R.description = parcel.readString();
            if (-l_2_R.imageUrls == null) {
                -l_2_R.imageUrls = new ArrayList();
            }
            parcel.readStringList(-l_2_R.imageUrls);
            -l_2_R.downloadCount = parcel.readInt();
            -l_2_R.source = parcel.readString();
            if (-l_2_R.sensitivePermissions == null) {
                -l_2_R.sensitivePermissions = new ArrayList();
            }
            parcel.readStringList(-l_2_R.sensitivePermissions);
            -l_2_R.virsusName = parcel.readString();
            -l_2_R.virsusDescription = parcel.readString();
            return -l_2_R;
        }
    };
    public String apkName = "";
    public String apkPackage = "";
    public String certMD5 = "";
    public String description = "";
    public String developer = "";
    public int downloadCount = 0;
    public String iconUrl = "";
    public ArrayList<String> imageUrls = null;
    public boolean isInSoftwareDB = false;
    public int official = 0;
    public ArrayList<String> sensitivePermissions = null;
    public long size = 0;
    public String source = "";
    public int versionCode = 0;
    public String versionName = "";
    public String virsusDescription = "";
    public String virsusName = "";

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        int i2 = 0;
        parcel.writeString(this.apkPackage);
        parcel.writeString(this.apkName);
        parcel.writeString(this.iconUrl);
        parcel.writeInt(this.versionCode);
        parcel.writeString(this.versionName);
        parcel.writeLong(this.size);
        parcel.writeInt(this.official);
        parcel.writeString(this.developer);
        parcel.writeString(this.certMD5);
        if (this.isInSoftwareDB) {
            i2 = 1;
        }
        parcel.writeInt(i2);
        parcel.writeString(this.description);
        parcel.writeStringList(this.imageUrls);
        parcel.writeInt(this.downloadCount);
        parcel.writeString(this.source);
        parcel.writeStringList(this.sensitivePermissions);
        parcel.writeString(this.virsusName);
        parcel.writeString(this.virsusDescription);
    }
}
