package android.service.euicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telephony.euicc.DownloadableSubscription;

public final class GetDefaultDownloadableSubscriptionListResult implements Parcelable {
    public static final Creator<GetDefaultDownloadableSubscriptionListResult> CREATOR = new Creator<GetDefaultDownloadableSubscriptionListResult>() {
        public GetDefaultDownloadableSubscriptionListResult createFromParcel(Parcel in) {
            return new GetDefaultDownloadableSubscriptionListResult(in);
        }

        public GetDefaultDownloadableSubscriptionListResult[] newArray(int size) {
            return new GetDefaultDownloadableSubscriptionListResult[size];
        }
    };
    public final int result;
    public final DownloadableSubscription[] subscriptions;

    public GetDefaultDownloadableSubscriptionListResult(int result, DownloadableSubscription[] subscriptions) {
        this.result = result;
        if (this.result == 0) {
            this.subscriptions = subscriptions;
        } else if (subscriptions != null) {
            throw new IllegalArgumentException("Error result with non-null subscriptions: " + result);
        } else {
            this.subscriptions = null;
        }
    }

    private GetDefaultDownloadableSubscriptionListResult(Parcel in) {
        this.result = in.readInt();
        this.subscriptions = (DownloadableSubscription[]) in.createTypedArray(DownloadableSubscription.CREATOR);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.result);
        dest.writeTypedArray(this.subscriptions, flags);
    }

    public int describeContents() {
        return 0;
    }
}
