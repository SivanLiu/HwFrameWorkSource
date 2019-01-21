package android.webkit;

import android.annotation.SystemApi;
import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Base64;

@SystemApi
public final class WebViewProviderInfo implements Parcelable {
    public static final Creator<WebViewProviderInfo> CREATOR = new Creator<WebViewProviderInfo>() {
        public WebViewProviderInfo createFromParcel(Parcel in) {
            return new WebViewProviderInfo(in, null);
        }

        public WebViewProviderInfo[] newArray(int size) {
            return new WebViewProviderInfo[size];
        }
    };
    public final boolean availableByDefault;
    public final String description;
    public final boolean isFallback;
    public final String packageName;
    public final Signature[] signatures;

    /* synthetic */ WebViewProviderInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public WebViewProviderInfo(String packageName, String description, boolean availableByDefault, boolean isFallback, String[] signatures) {
        this.packageName = packageName;
        this.description = description;
        this.availableByDefault = availableByDefault;
        this.isFallback = isFallback;
        if (signatures == null) {
            this.signatures = new Signature[0];
            return;
        }
        this.signatures = new Signature[signatures.length];
        for (int n = 0; n < signatures.length; n++) {
            this.signatures[n] = new Signature(Base64.decode(signatures[n], 0));
        }
    }

    private WebViewProviderInfo(Parcel in) {
        this.packageName = in.readString();
        this.description = in.readString();
        boolean z = false;
        this.availableByDefault = in.readInt() > 0;
        if (in.readInt() > 0) {
            z = true;
        }
        this.isFallback = z;
        this.signatures = (Signature[]) in.createTypedArray(Signature.CREATOR);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.packageName);
        out.writeString(this.description);
        out.writeInt(this.availableByDefault);
        out.writeInt(this.isFallback);
        out.writeTypedArray(this.signatures, 0);
    }
}
