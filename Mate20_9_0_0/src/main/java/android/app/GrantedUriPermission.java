package android.app;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class GrantedUriPermission implements Parcelable {
    public static final Creator<GrantedUriPermission> CREATOR = new Creator<GrantedUriPermission>() {
        public GrantedUriPermission createFromParcel(Parcel in) {
            return new GrantedUriPermission(in, null);
        }

        public GrantedUriPermission[] newArray(int size) {
            return new GrantedUriPermission[size];
        }
    };
    public final String packageName;
    public final Uri uri;

    public GrantedUriPermission(Uri uri, String packageName) {
        this.uri = uri;
        this.packageName = packageName;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.packageName);
        stringBuilder.append(":");
        stringBuilder.append(this.uri);
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(this.uri, flags);
        out.writeString(this.packageName);
    }

    private GrantedUriPermission(Parcel in) {
        this.uri = (Uri) in.readParcelable(null);
        this.packageName = in.readString();
    }
}
