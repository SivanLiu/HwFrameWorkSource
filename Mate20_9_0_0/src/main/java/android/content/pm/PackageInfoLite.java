package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class PackageInfoLite implements Parcelable {
    public static final Creator<PackageInfoLite> CREATOR = new Creator<PackageInfoLite>() {
        public PackageInfoLite createFromParcel(Parcel source) {
            return new PackageInfoLite(source, null);
        }

        public PackageInfoLite[] newArray(int size) {
            return new PackageInfoLite[size];
        }
    };
    public int baseRevisionCode;
    public int installLocation;
    public boolean multiArch;
    public String packageName;
    public int recommendedInstallLocation;
    public String[] splitNames;
    public int[] splitRevisionCodes;
    public VerifierInfo[] verifiers;
    @Deprecated
    public int versionCode;
    public int versionCodeMajor;

    /* synthetic */ PackageInfoLite(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public long getLongVersionCode() {
        return PackageInfo.composeLongVersionCode(this.versionCodeMajor, this.versionCode);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PackageInfoLite{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.packageName);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeString(this.packageName);
        dest.writeStringArray(this.splitNames);
        dest.writeInt(this.versionCode);
        dest.writeInt(this.versionCodeMajor);
        dest.writeInt(this.baseRevisionCode);
        dest.writeIntArray(this.splitRevisionCodes);
        dest.writeInt(this.recommendedInstallLocation);
        dest.writeInt(this.installLocation);
        dest.writeInt(this.multiArch);
        if (this.verifiers == null || this.verifiers.length == 0) {
            dest.writeInt(0);
            return;
        }
        dest.writeInt(this.verifiers.length);
        dest.writeTypedArray(this.verifiers, parcelableFlags);
    }

    private PackageInfoLite(Parcel source) {
        this.packageName = source.readString();
        this.splitNames = source.createStringArray();
        this.versionCode = source.readInt();
        this.versionCodeMajor = source.readInt();
        this.baseRevisionCode = source.readInt();
        this.splitRevisionCodes = source.createIntArray();
        this.recommendedInstallLocation = source.readInt();
        this.installLocation = source.readInt();
        this.multiArch = source.readInt() != 0;
        int verifiersLength = source.readInt();
        if (verifiersLength == 0) {
            this.verifiers = new VerifierInfo[0];
            return;
        }
        this.verifiers = new VerifierInfo[verifiersLength];
        source.readTypedArray(this.verifiers, VerifierInfo.CREATOR);
    }
}
