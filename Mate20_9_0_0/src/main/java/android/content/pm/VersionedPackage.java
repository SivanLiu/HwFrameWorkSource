package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class VersionedPackage implements Parcelable {
    public static final Creator<VersionedPackage> CREATOR = new Creator<VersionedPackage>() {
        public VersionedPackage createFromParcel(Parcel source) {
            return new VersionedPackage(source, null);
        }

        public VersionedPackage[] newArray(int size) {
            return new VersionedPackage[size];
        }
    };
    private final String mPackageName;
    private final long mVersionCode;

    @Retention(RetentionPolicy.SOURCE)
    public @interface VersionCode {
    }

    public VersionedPackage(String packageName, int versionCode) {
        this.mPackageName = packageName;
        this.mVersionCode = (long) versionCode;
    }

    public VersionedPackage(String packageName, long versionCode) {
        this.mPackageName = packageName;
        this.mVersionCode = versionCode;
    }

    private VersionedPackage(Parcel parcel) {
        this.mPackageName = parcel.readString();
        this.mVersionCode = parcel.readLong();
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    @Deprecated
    public int getVersionCode() {
        return (int) (this.mVersionCode & 2147483647L);
    }

    public long getLongVersionCode() {
        return this.mVersionCode;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VersionedPackage[");
        stringBuilder.append(this.mPackageName);
        stringBuilder.append("/");
        stringBuilder.append(this.mVersionCode);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mPackageName);
        parcel.writeLong(this.mVersionCode);
    }
}
