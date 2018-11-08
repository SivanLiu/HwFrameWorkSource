package android.app.timezone;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class DistroRulesVersion implements Parcelable {
    public static final Creator<DistroRulesVersion> CREATOR = new Creator<DistroRulesVersion>() {
        public DistroRulesVersion createFromParcel(Parcel in) {
            return new DistroRulesVersion(in.readString(), in.readInt());
        }

        public DistroRulesVersion[] newArray(int size) {
            return new DistroRulesVersion[size];
        }
    };
    private final int mRevision;
    private final String mRulesVersion;

    public DistroRulesVersion(String rulesVersion, int revision) {
        this.mRulesVersion = Utils.validateRulesVersion("rulesVersion", rulesVersion);
        this.mRevision = Utils.validateVersion("revision", revision);
    }

    public String getRulesVersion() {
        return this.mRulesVersion;
    }

    public int getRevision() {
        return this.mRevision;
    }

    public boolean isOlderThan(DistroRulesVersion distroRulesVersion) {
        boolean z = true;
        int rulesComparison = this.mRulesVersion.compareTo(distroRulesVersion.mRulesVersion);
        if (rulesComparison < 0) {
            return true;
        }
        if (rulesComparison > 0) {
            return false;
        }
        if (this.mRevision >= distroRulesVersion.mRevision) {
            z = false;
        }
        return z;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mRulesVersion);
        out.writeInt(this.mRevision);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DistroRulesVersion that = (DistroRulesVersion) o;
        if (this.mRevision != that.mRevision) {
            return false;
        }
        return this.mRulesVersion.equals(that.mRulesVersion);
    }

    public int hashCode() {
        return (this.mRulesVersion.hashCode() * 31) + this.mRevision;
    }

    public String toString() {
        return "DistroRulesVersion{mRulesVersion='" + this.mRulesVersion + '\'' + ", mRevision='" + this.mRevision + '\'' + '}';
    }

    public String toDumpString() {
        return this.mRulesVersion + "," + this.mRevision;
    }
}
