package android.content.pm;

import android.content.pm.PackageParser.SigningDetails;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class SigningInfo implements Parcelable {
    public static final Creator<SigningInfo> CREATOR = new Creator<SigningInfo>() {
        public SigningInfo createFromParcel(Parcel source) {
            return new SigningInfo(source, null);
        }

        public SigningInfo[] newArray(int size) {
            return new SigningInfo[size];
        }
    };
    private final SigningDetails mSigningDetails;

    /* synthetic */ SigningInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public SigningInfo() {
        this.mSigningDetails = SigningDetails.UNKNOWN;
    }

    public SigningInfo(SigningDetails signingDetails) {
        this.mSigningDetails = new SigningDetails(signingDetails);
    }

    public SigningInfo(SigningInfo orig) {
        this.mSigningDetails = new SigningDetails(orig.mSigningDetails);
    }

    private SigningInfo(Parcel source) {
        this.mSigningDetails = (SigningDetails) SigningDetails.CREATOR.createFromParcel(source);
    }

    public boolean hasMultipleSigners() {
        return this.mSigningDetails.signatures != null && this.mSigningDetails.signatures.length > 1;
    }

    public boolean hasPastSigningCertificates() {
        return (this.mSigningDetails.signatures == null || this.mSigningDetails.pastSigningCertificates == null) ? false : true;
    }

    public Signature[] getSigningCertificateHistory() {
        if (hasMultipleSigners()) {
            return null;
        }
        if (hasPastSigningCertificates()) {
            return this.mSigningDetails.pastSigningCertificates;
        }
        return this.mSigningDetails.signatures;
    }

    public Signature[] getApkContentsSigners() {
        return this.mSigningDetails.signatures;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        this.mSigningDetails.writeToParcel(dest, parcelableFlags);
    }
}
