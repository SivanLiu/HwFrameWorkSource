package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.PatternMatcher;
import android.util.Printer;

public final class ProviderInfo extends ComponentInfo implements Parcelable {
    public static final Creator<ProviderInfo> CREATOR = new Creator<ProviderInfo>() {
        public ProviderInfo createFromParcel(Parcel in) {
            return new ProviderInfo(in, null);
        }

        public ProviderInfo[] newArray(int size) {
            return new ProviderInfo[size];
        }
    };
    public static final int FLAG_SINGLE_USER = 1073741824;
    public static final int FLAG_VISIBLE_TO_INSTANT_APP = 1048576;
    public String authority;
    public int flags;
    public boolean grantUriPermissions;
    public int initOrder;
    @Deprecated
    public boolean isSyncable;
    public boolean multiprocess;
    public PathPermission[] pathPermissions;
    public String readPermission;
    public PatternMatcher[] uriPermissionPatterns;
    public String writePermission;

    /* synthetic */ ProviderInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public ProviderInfo() {
        this.authority = null;
        this.readPermission = null;
        this.writePermission = null;
        this.grantUriPermissions = false;
        this.uriPermissionPatterns = null;
        this.pathPermissions = null;
        this.multiprocess = false;
        this.initOrder = 0;
        this.flags = 0;
        this.isSyncable = false;
    }

    public ProviderInfo(ProviderInfo orig) {
        super((ComponentInfo) orig);
        this.authority = null;
        this.readPermission = null;
        this.writePermission = null;
        this.grantUriPermissions = false;
        this.uriPermissionPatterns = null;
        this.pathPermissions = null;
        this.multiprocess = false;
        this.initOrder = 0;
        this.flags = 0;
        this.isSyncable = false;
        this.authority = orig.authority;
        this.readPermission = orig.readPermission;
        this.writePermission = orig.writePermission;
        this.grantUriPermissions = orig.grantUriPermissions;
        this.uriPermissionPatterns = orig.uriPermissionPatterns;
        this.pathPermissions = orig.pathPermissions;
        this.multiprocess = orig.multiprocess;
        this.initOrder = orig.initOrder;
        this.flags = orig.flags;
        this.isSyncable = orig.isSyncable;
    }

    public void dump(Printer pw, String prefix) {
        dump(pw, prefix, 3);
    }

    public void dump(Printer pw, String prefix, int dumpFlags) {
        super.dumpFront(pw, prefix);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("authority=");
        stringBuilder.append(this.authority);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("flags=0x");
        stringBuilder.append(Integer.toHexString(this.flags));
        pw.println(stringBuilder.toString());
        super.dumpBack(pw, prefix, dumpFlags);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int parcelableFlags) {
        super.writeToParcel(out, parcelableFlags);
        out.writeString(this.authority);
        out.writeString(this.readPermission);
        out.writeString(this.writePermission);
        out.writeInt(this.grantUriPermissions);
        out.writeTypedArray(this.uriPermissionPatterns, parcelableFlags);
        out.writeTypedArray(this.pathPermissions, parcelableFlags);
        out.writeInt(this.multiprocess);
        out.writeInt(this.initOrder);
        out.writeInt(this.flags);
        out.writeInt(this.isSyncable);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ContentProviderInfo{name=");
        stringBuilder.append(this.authority);
        stringBuilder.append(" className=");
        stringBuilder.append(this.name);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private ProviderInfo(Parcel in) {
        super(in);
        this.authority = null;
        this.readPermission = null;
        this.writePermission = null;
        boolean z = false;
        this.grantUriPermissions = false;
        this.uriPermissionPatterns = null;
        this.pathPermissions = null;
        this.multiprocess = false;
        this.initOrder = 0;
        this.flags = 0;
        this.isSyncable = false;
        this.authority = in.readString();
        this.readPermission = in.readString();
        this.writePermission = in.readString();
        this.grantUriPermissions = in.readInt() != 0;
        this.uriPermissionPatterns = (PatternMatcher[]) in.createTypedArray(PatternMatcher.CREATOR);
        this.pathPermissions = (PathPermission[]) in.createTypedArray(PathPermission.CREATOR);
        this.multiprocess = in.readInt() != 0;
        this.initOrder = in.readInt();
        this.flags = in.readInt();
        if (in.readInt() != 0) {
            z = true;
        }
        this.isSyncable = z;
    }
}
