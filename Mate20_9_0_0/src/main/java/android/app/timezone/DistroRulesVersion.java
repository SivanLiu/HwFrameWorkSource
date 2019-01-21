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
        int rulesComparison = this.mRulesVersion.compareTo(distroRulesVersion.mRulesVersion);
        boolean z = true;
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
        return (31 * this.mRulesVersion.hashCode()) + this.mRevision;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DistroRulesVersion{mRulesVersion='");
        stringBuilder.append(this.mRulesVersion);
        stringBuilder.append('\'');
        stringBuilder.append(", mRevision='");
        stringBuilder.append(this.mRevision);
        stringBuilder.append('\'');
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public String toDumpString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mRulesVersion);
        stringBuilder.append(",");
        stringBuilder.append(this.mRevision);
        return stringBuilder.toString();
    }
}
