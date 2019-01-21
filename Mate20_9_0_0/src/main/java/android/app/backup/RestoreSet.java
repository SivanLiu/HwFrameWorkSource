package android.app.backup;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

@SystemApi
public class RestoreSet implements Parcelable {
    public static final Creator<RestoreSet> CREATOR = new Creator<RestoreSet>() {
        public RestoreSet createFromParcel(Parcel in) {
            return new RestoreSet(in, null);
        }

        public RestoreSet[] newArray(int size) {
            return new RestoreSet[size];
        }
    };
    public String device;
    public String name;
    public long token;

    /* synthetic */ RestoreSet(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public RestoreSet(String _name, String _dev, long _token) {
        this.name = _name;
        this.device = _dev;
        this.token = _token;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.name);
        out.writeString(this.device);
        out.writeLong(this.token);
    }

    private RestoreSet(Parcel in) {
        this.name = in.readString();
        this.device = in.readString();
        this.token = in.readLong();
    }
}
