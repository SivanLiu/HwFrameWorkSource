package android.os;

import android.os.Parcelable.Creator;

public class JankAppInfo implements Parcelable {
    public static final Creator<JankAppInfo> CREATOR = new Creator<JankAppInfo>() {
        public JankAppInfo createFromParcel(Parcel in) {
            return new JankAppInfo(in, null);
        }

        public JankAppInfo[] newArray(int size) {
            return new JankAppInfo[size];
        }
    };
    public boolean coreApp;
    public int flags;
    public String packageName;
    public boolean systemApp;
    public int versionCode;
    public String versionName;

    /* synthetic */ JankAppInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    private JankAppInfo(Parcel in) {
        this.packageName = in.readString();
        this.versionCode = in.readInt();
        this.versionName = in.readString();
        boolean z = false;
        this.coreApp = in.readInt() != 0;
        if (in.readInt() != 0) {
            z = true;
        }
        this.systemApp = z;
        this.flags = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(this.packageName);
        dest.writeInt(this.versionCode);
        dest.writeString(this.versionName);
        dest.writeInt(this.coreApp);
        dest.writeInt(this.systemApp);
        dest.writeInt(this.flags);
    }
}
