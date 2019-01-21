package android.app;

import android.content.ContentProviderNative;
import android.content.IContentProvider;
import android.content.pm.ProviderInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ContentProviderHolder implements Parcelable {
    public static final Creator<ContentProviderHolder> CREATOR = new Creator<ContentProviderHolder>() {
        public ContentProviderHolder createFromParcel(Parcel source) {
            return new ContentProviderHolder(source, null);
        }

        public ContentProviderHolder[] newArray(int size) {
            return new ContentProviderHolder[size];
        }
    };
    public IBinder connection;
    public final ProviderInfo info;
    public boolean noReleaseNeeded;
    public IContentProvider provider;

    /* synthetic */ ContentProviderHolder(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public ContentProviderHolder(ProviderInfo _info) {
        this.info = _info;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        this.info.writeToParcel(dest, 0);
        if (this.provider != null) {
            dest.writeStrongBinder(this.provider.asBinder());
        } else {
            dest.writeStrongBinder(null);
        }
        dest.writeStrongBinder(this.connection);
        dest.writeInt(this.noReleaseNeeded);
    }

    private ContentProviderHolder(Parcel source) {
        this.info = (ProviderInfo) ProviderInfo.CREATOR.createFromParcel(source);
        this.provider = ContentProviderNative.asInterface(source.readStrongBinder());
        this.connection = source.readStrongBinder();
        this.noReleaseNeeded = source.readInt() != 0;
    }
}
