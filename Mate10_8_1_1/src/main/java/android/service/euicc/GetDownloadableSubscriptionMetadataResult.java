package android.service.euicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telephony.euicc.DownloadableSubscription;

public final class GetDownloadableSubscriptionMetadataResult implements Parcelable {
    public static final Creator<GetDownloadableSubscriptionMetadataResult> CREATOR = new Creator<GetDownloadableSubscriptionMetadataResult>() {
        public GetDownloadableSubscriptionMetadataResult createFromParcel(Parcel in) {
            return new GetDownloadableSubscriptionMetadataResult(in);
        }

        public GetDownloadableSubscriptionMetadataResult[] newArray(int size) {
            return new GetDownloadableSubscriptionMetadataResult[size];
        }
    };
    public final int result;
    public final DownloadableSubscription subscription;

    public GetDownloadableSubscriptionMetadataResult(int result, DownloadableSubscription subscription) {
        this.result = result;
        if (this.result == 0) {
            this.subscription = subscription;
        } else if (subscription != null) {
            throw new IllegalArgumentException("Error result with non-null subscription: " + result);
        } else {
            this.subscription = null;
        }
    }

    private GetDownloadableSubscriptionMetadataResult(Parcel in) {
        this.result = in.readInt();
        this.subscription = (DownloadableSubscription) in.readTypedObject(DownloadableSubscription.CREATOR);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.result);
        dest.writeTypedObject(this.subscription, flags);
    }

    public int describeContents() {
        return 0;
    }
}
