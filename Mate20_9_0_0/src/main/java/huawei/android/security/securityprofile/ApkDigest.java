package huawei.android.security.securityprofile;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ApkDigest implements Parcelable {
    public static final Creator<ApkDigest> CREATOR = new Creator<ApkDigest>() {
        public ApkDigest createFromParcel(Parcel in) {
            return new ApkDigest(in, null);
        }

        public ApkDigest[] newArray(int size) {
            return new ApkDigest[size];
        }
    };
    public String apkSignatureScheme;
    public String base64Digest;
    public String digestAlgorithm;

    /* synthetic */ ApkDigest(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.apkSignatureScheme);
        out.writeString(this.digestAlgorithm);
        out.writeString(this.base64Digest);
    }

    private ApkDigest(Parcel in) {
        this.apkSignatureScheme = in.readString();
        this.digestAlgorithm = in.readString();
        this.base64Digest = in.readString();
    }

    public ApkDigest(String apkSignatureScheme, String digestAlgorithm, String base64Digest) {
        this.apkSignatureScheme = apkSignatureScheme;
        this.digestAlgorithm = digestAlgorithm;
        this.base64Digest = base64Digest;
    }
}
