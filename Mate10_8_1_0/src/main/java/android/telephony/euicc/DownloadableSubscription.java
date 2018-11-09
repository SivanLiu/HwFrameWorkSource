package android.telephony.euicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telephony.UiccAccessRule;
import com.android.internal.util.Preconditions;

public final class DownloadableSubscription implements Parcelable {
    public static final Creator<DownloadableSubscription> CREATOR = new Creator<DownloadableSubscription>() {
        public DownloadableSubscription createFromParcel(Parcel in) {
            return new DownloadableSubscription(in);
        }

        public DownloadableSubscription[] newArray(int size) {
            return new DownloadableSubscription[size];
        }
    };
    private UiccAccessRule[] accessRules;
    private String carrierName;
    public final String encodedActivationCode;

    private DownloadableSubscription(String encodedActivationCode) {
        this.encodedActivationCode = encodedActivationCode;
    }

    private DownloadableSubscription(Parcel in) {
        this.encodedActivationCode = in.readString();
        this.carrierName = in.readString();
        this.accessRules = (UiccAccessRule[]) in.createTypedArray(UiccAccessRule.CREATOR);
    }

    public static DownloadableSubscription forActivationCode(String encodedActivationCode) {
        Preconditions.checkNotNull(encodedActivationCode, "Activation code may not be null");
        return new DownloadableSubscription(encodedActivationCode);
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getCarrierName() {
        return this.carrierName;
    }

    public UiccAccessRule[] getAccessRules() {
        return this.accessRules;
    }

    public void setAccessRules(UiccAccessRule[] accessRules) {
        this.accessRules = accessRules;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.encodedActivationCode);
        dest.writeString(this.carrierName);
        dest.writeTypedArray(this.accessRules, flags);
    }

    public int describeContents() {
        return 0;
    }
}
