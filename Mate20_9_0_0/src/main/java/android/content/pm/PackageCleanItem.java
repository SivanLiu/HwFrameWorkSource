package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class PackageCleanItem implements Parcelable {
    public static final Creator<PackageCleanItem> CREATOR = new Creator<PackageCleanItem>() {
        public PackageCleanItem createFromParcel(Parcel source) {
            return new PackageCleanItem(source, null);
        }

        public PackageCleanItem[] newArray(int size) {
            return new PackageCleanItem[size];
        }
    };
    public final boolean andCode;
    public final String packageName;
    public final int userId;

    /* synthetic */ PackageCleanItem(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public PackageCleanItem(int userId, String packageName, boolean andCode) {
        this.userId = userId;
        this.packageName = packageName;
        this.andCode = andCode;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj != null) {
            try {
                PackageCleanItem other = (PackageCleanItem) obj;
                if (!(this.userId == other.userId && this.packageName.equals(other.packageName) && this.andCode == other.andCode)) {
                    z = false;
                }
                return z;
            } catch (ClassCastException e) {
            }
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * 17) + this.userId)) + this.packageName.hashCode())) + this.andCode;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(this.userId);
        dest.writeString(this.packageName);
        dest.writeInt(this.andCode);
    }

    private PackageCleanItem(Parcel source) {
        this.userId = source.readInt();
        this.packageName = source.readString();
        this.andCode = source.readInt() != 0;
    }
}
