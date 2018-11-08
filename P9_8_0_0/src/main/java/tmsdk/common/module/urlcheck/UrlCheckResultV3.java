package tmsdk.common.module.urlcheck;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import tmsdkobf.ct;
import tmsdkobf.cx;
import tmsdkobf.cy;

public class UrlCheckResultV3 implements Parcelable {
    public static Creator<UrlCheckResultV3> CREATOR = new Creator<UrlCheckResultV3>() {
        public UrlCheckResultV3[] bN(int i) {
            return new UrlCheckResultV3[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return r(parcel);
        }

        public /* synthetic */ Object[] newArray(int i) {
            return bN(i);
        }

        public UrlCheckResultV3 r(Parcel parcel) {
            Object -l_2_R = new UrlCheckResultV3();
            -l_2_R.url = parcel.readString();
            -l_2_R.level = parcel.readInt();
            -l_2_R.linkType = parcel.readInt();
            -l_2_R.riskType = parcel.readInt();
            if (-l_2_R.linkType != 0) {
                -l_2_R.apkDetail = (ApkDetail) parcel.readParcelable(ApkDetail.class.getClassLoader());
            } else {
                -l_2_R.webPageDetail = (WebPageDetail) parcel.readParcelable(WebPageDetail.class.getClassLoader());
            }
            -l_2_R.mErrCode = parcel.readInt();
            return -l_2_R;
        }
    };
    public ApkDetail apkDetail;
    public int level;
    public int linkType;
    public int mErrCode;
    public int riskType;
    public String url;
    public WebPageDetail webPageDetail;

    private UrlCheckResultV3() {
        this.level = -1;
        this.linkType = -1;
        this.riskType = -1;
        this.webPageDetail = null;
        this.apkDetail = null;
        this.mErrCode = 0;
    }

    public UrlCheckResultV3(int i) {
        this.level = -1;
        this.linkType = -1;
        this.riskType = -1;
        this.webPageDetail = null;
        this.apkDetail = null;
        this.mErrCode = 0;
        this.mErrCode = i;
    }

    public UrlCheckResultV3(String str, cx cxVar) {
        this.level = -1;
        this.linkType = -1;
        this.riskType = -1;
        this.webPageDetail = null;
        this.apkDetail = null;
        this.mErrCode = 0;
        this.url = str;
        this.level = cxVar.level;
        this.linkType = cxVar.linkType;
        this.riskType = cxVar.riskType;
        if (cxVar.gd != null) {
            this.webPageDetail = a(cxVar.gd);
        }
        if (cxVar.ge != null) {
            this.apkDetail = a(cxVar.ge);
        }
    }

    private ApkDetail a(ct ctVar) {
        Object -l_2_R = new ApkDetail();
        -l_2_R.apkName = ctVar.apkName;
        -l_2_R.apkPackage = ctVar.apkPackage;
        -l_2_R.iconUrl = ctVar.iconUrl;
        -l_2_R.versionCode = ctVar.versionCode;
        -l_2_R.versionName = ctVar.versionName;
        -l_2_R.size = ctVar.size;
        -l_2_R.official = ctVar.official;
        -l_2_R.developer = ctVar.developer;
        -l_2_R.certMD5 = ctVar.certMD5;
        -l_2_R.isInSoftwareDB = ctVar.isInSoftwareDB;
        -l_2_R.description = ctVar.description;
        -l_2_R.imageUrls = ctVar.imageUrls;
        -l_2_R.downloadCount = ctVar.downloadCount;
        -l_2_R.source = ctVar.source;
        -l_2_R.sensitivePermissions = ctVar.sensitivePermissions;
        -l_2_R.virsusName = ctVar.virsusName;
        -l_2_R.virsusDescription = ctVar.virsusDescription;
        return -l_2_R;
    }

    private WebPageDetail a(cy cyVar) {
        Object -l_2_R = new WebPageDetail();
        -l_2_R.title = cyVar.title;
        -l_2_R.description = cyVar.description;
        -l_2_R.webIconUrl = cyVar.webIconUrl;
        -l_2_R.screenshotUrl = cyVar.screenshotUrl;
        -l_2_R.maliceType = cyVar.maliceType;
        -l_2_R.maliceTitle = cyVar.maliceTitle;
        -l_2_R.maliceBody = cyVar.maliceBody;
        -l_2_R.flawName = cyVar.flawName;
        return -l_2_R;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.url);
        parcel.writeInt(this.level);
        parcel.writeInt(this.linkType);
        parcel.writeInt(this.riskType);
        if (this.linkType != 0) {
            parcel.writeParcelable(this.apkDetail, 0);
        } else {
            parcel.writeParcelable(this.webPageDetail, 0);
        }
        parcel.writeInt(this.mErrCode);
    }
}
