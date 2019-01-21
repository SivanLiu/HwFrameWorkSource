package android.app.assist;

import android.content.ClipData;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class AssistContent implements Parcelable {
    public static final Creator<AssistContent> CREATOR = new Creator<AssistContent>() {
        public AssistContent createFromParcel(Parcel in) {
            return new AssistContent(in);
        }

        public AssistContent[] newArray(int size) {
            return new AssistContent[size];
        }
    };
    private ClipData mClipData;
    private final Bundle mExtras;
    private Intent mIntent;
    private boolean mIsAppProvidedIntent;
    private boolean mIsAppProvidedWebUri;
    private String mStructuredData;
    private Uri mUri;

    public AssistContent() {
        this.mIsAppProvidedIntent = false;
        this.mIsAppProvidedWebUri = false;
        this.mExtras = new Bundle();
    }

    public void setDefaultIntent(Intent intent) {
        this.mIntent = intent;
        this.mIsAppProvidedIntent = false;
        this.mIsAppProvidedWebUri = false;
        this.mUri = null;
        if (intent != null && "android.intent.action.VIEW".equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri == null) {
                return;
            }
            if (IntentFilter.SCHEME_HTTP.equals(uri.getScheme()) || IntentFilter.SCHEME_HTTPS.equals(uri.getScheme())) {
                this.mUri = uri;
            }
        }
    }

    public void setIntent(Intent intent) {
        this.mIsAppProvidedIntent = true;
        this.mIntent = intent;
    }

    public Intent getIntent() {
        return this.mIntent;
    }

    public boolean isAppProvidedIntent() {
        return this.mIsAppProvidedIntent;
    }

    public void setClipData(ClipData clip) {
        this.mClipData = clip;
    }

    public ClipData getClipData() {
        return this.mClipData;
    }

    public void setStructuredData(String structuredData) {
        this.mStructuredData = structuredData;
    }

    public String getStructuredData() {
        return this.mStructuredData;
    }

    public void setWebUri(Uri uri) {
        this.mIsAppProvidedWebUri = true;
        this.mUri = uri;
    }

    public Uri getWebUri() {
        return this.mUri;
    }

    public boolean isAppProvidedWebUri() {
        return this.mIsAppProvidedWebUri;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    AssistContent(Parcel in) {
        boolean z = false;
        this.mIsAppProvidedIntent = false;
        this.mIsAppProvidedWebUri = false;
        if (in.readInt() != 0) {
            this.mIntent = (Intent) Intent.CREATOR.createFromParcel(in);
        }
        if (in.readInt() != 0) {
            this.mClipData = (ClipData) ClipData.CREATOR.createFromParcel(in);
        }
        if (in.readInt() != 0) {
            this.mUri = (Uri) Uri.CREATOR.createFromParcel(in);
        }
        if (in.readInt() != 0) {
            this.mStructuredData = in.readString();
        }
        this.mIsAppProvidedIntent = in.readInt() == 1;
        this.mExtras = in.readBundle();
        if (in.readInt() == 1) {
            z = true;
        }
        this.mIsAppProvidedWebUri = z;
    }

    void writeToParcelInternal(Parcel dest, int flags) {
        if (this.mIntent != null) {
            dest.writeInt(1);
            this.mIntent.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        if (this.mClipData != null) {
            dest.writeInt(1);
            this.mClipData.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        if (this.mUri != null) {
            dest.writeInt(1);
            this.mUri.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        if (this.mStructuredData != null) {
            dest.writeInt(1);
            dest.writeString(this.mStructuredData);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.mIsAppProvidedIntent);
        dest.writeBundle(this.mExtras);
        dest.writeInt(this.mIsAppProvidedWebUri);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        writeToParcelInternal(dest, flags);
    }
}
