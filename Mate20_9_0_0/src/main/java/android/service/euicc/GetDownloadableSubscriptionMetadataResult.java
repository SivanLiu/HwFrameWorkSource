package android.service.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telephony.euicc.DownloadableSubscription;

@SystemApi
public final class GetDownloadableSubscriptionMetadataResult implements Parcelable {
    public static final Creator<GetDownloadableSubscriptionMetadataResult> CREATOR = new Creator<GetDownloadableSubscriptionMetadataResult>() {
        public GetDownloadableSubscriptionMetadataResult createFromParcel(Parcel in) {
            return new GetDownloadableSubscriptionMetadataResult(in, null);
        }

        public GetDownloadableSubscriptionMetadataResult[] newArray(int size) {
            return new GetDownloadableSubscriptionMetadataResult[size];
        }
    };
    private final DownloadableSubscription mSubscription;
    @Deprecated
    public final int result;

    public int getResult() {
        return this.result;
    }

    public DownloadableSubscription getDownloadableSubscription() {
        return this.mSubscription;
    }

    public GetDownloadableSubscriptionMetadataResult(int result, DownloadableSubscription subscription) {
        this.result = result;
        if (this.result == 0) {
            this.mSubscription = subscription;
        } else if (subscription == null) {
            this.mSubscription = null;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error result with non-null subscription: ");
            stringBuilder.append(result);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private GetDownloadableSubscriptionMetadataResult(Parcel in) {
        this.result = in.readInt();
        this.mSubscription = (DownloadableSubscription) in.readTypedObject(DownloadableSubscription.CREATOR);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.result);
        dest.writeTypedObject(this.mSubscription, flags);
    }

    public int describeContents() {
        return 0;
    }
}
