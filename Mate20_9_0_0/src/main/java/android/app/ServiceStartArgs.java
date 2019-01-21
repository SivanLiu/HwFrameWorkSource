package android.app;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ServiceStartArgs implements Parcelable {
    public static final Creator<ServiceStartArgs> CREATOR = new Creator<ServiceStartArgs>() {
        public ServiceStartArgs createFromParcel(Parcel in) {
            return new ServiceStartArgs(in);
        }

        public ServiceStartArgs[] newArray(int size) {
            return new ServiceStartArgs[size];
        }
    };
    public final Intent args;
    public final int flags;
    public final int startId;
    public final boolean taskRemoved;

    public ServiceStartArgs(boolean _taskRemoved, int _startId, int _flags, Intent _args) {
        this.taskRemoved = _taskRemoved;
        this.startId = _startId;
        this.flags = _flags;
        this.args = _args;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ServiceStartArgs{taskRemoved=");
        stringBuilder.append(this.taskRemoved);
        stringBuilder.append(", startId=");
        stringBuilder.append(this.startId);
        stringBuilder.append(", flags=0x");
        stringBuilder.append(Integer.toHexString(this.flags));
        stringBuilder.append(", args=");
        stringBuilder.append(this.args);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.taskRemoved);
        out.writeInt(this.startId);
        out.writeInt(flags);
        if (this.args != null) {
            out.writeInt(1);
            this.args.writeToParcel(out, 0);
            return;
        }
        out.writeInt(0);
    }

    public ServiceStartArgs(Parcel in) {
        this.taskRemoved = in.readInt() != 0;
        this.startId = in.readInt();
        this.flags = in.readInt();
        if (in.readInt() != 0) {
            this.args = (Intent) Intent.CREATOR.createFromParcel(in);
        } else {
            this.args = null;
        }
    }
}
