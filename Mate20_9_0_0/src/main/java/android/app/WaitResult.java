package android.app;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.io.PrintWriter;

public class WaitResult implements Parcelable {
    public static final Creator<WaitResult> CREATOR = new Creator<WaitResult>() {
        public WaitResult createFromParcel(Parcel source) {
            return new WaitResult(source, null);
        }

        public WaitResult[] newArray(int size) {
            return new WaitResult[size];
        }
    };
    public ComponentName origin;
    public int result;
    public long thisTime;
    public boolean timeout;
    public long totalTime;
    public ComponentName who;

    /* synthetic */ WaitResult(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.result);
        dest.writeInt(this.timeout);
        ComponentName.writeToParcel(this.who, dest);
        dest.writeLong(this.thisTime);
        dest.writeLong(this.totalTime);
        ComponentName.writeToParcel(this.origin, dest);
    }

    private WaitResult(Parcel source) {
        this.result = source.readInt();
        this.timeout = source.readInt() != 0;
        this.who = ComponentName.readFromParcel(source);
        this.thisTime = source.readLong();
        this.totalTime = source.readLong();
        this.origin = ComponentName.readFromParcel(source);
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("WaitResult:");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  result=");
        stringBuilder.append(this.result);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  timeout=");
        stringBuilder.append(this.timeout);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  who=");
        stringBuilder.append(this.who);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  thisTime=");
        stringBuilder.append(this.thisTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  totalTime=");
        stringBuilder.append(this.totalTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  origin=");
        stringBuilder.append(this.origin);
        pw.println(stringBuilder.toString());
    }
}
