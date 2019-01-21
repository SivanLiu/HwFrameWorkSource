package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Printer;

public class ServiceInfo extends ComponentInfo implements Parcelable {
    public static final Creator<ServiceInfo> CREATOR = new Creator<ServiceInfo>() {
        public ServiceInfo createFromParcel(Parcel source) {
            return new ServiceInfo(source, null);
        }

        public ServiceInfo[] newArray(int size) {
            return new ServiceInfo[size];
        }
    };
    public static final int FLAG_EXTERNAL_SERVICE = 4;
    public static final int FLAG_ISOLATED_PROCESS = 2;
    public static final int FLAG_SINGLE_USER = 1073741824;
    public static final int FLAG_STOP_WITH_TASK = 1;
    public static final int FLAG_VISIBLE_TO_INSTANT_APP = 1048576;
    public int flags;
    public String permission;

    /* synthetic */ ServiceInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public ServiceInfo(ServiceInfo orig) {
        super((ComponentInfo) orig);
        this.permission = orig.permission;
        this.flags = orig.flags;
    }

    public void dump(Printer pw, String prefix) {
        dump(pw, prefix, 3);
    }

    void dump(Printer pw, String prefix, int dumpFlags) {
        super.dumpFront(pw, prefix);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("permission=");
        stringBuilder.append(this.permission);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("flags=0x");
        stringBuilder.append(Integer.toHexString(this.flags));
        pw.println(stringBuilder.toString());
        super.dumpBack(pw, prefix, dumpFlags);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ServiceInfo{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.name);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeString(this.permission);
        dest.writeInt(this.flags);
    }

    private ServiceInfo(Parcel source) {
        super(source);
        this.permission = source.readString();
        this.flags = source.readInt();
    }
}
