package android.app;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ProcessMemoryState implements Parcelable {
    public static final Creator<ProcessMemoryState> CREATOR = new Creator<ProcessMemoryState>() {
        public ProcessMemoryState createFromParcel(Parcel in) {
            return new ProcessMemoryState(in, null);
        }

        public ProcessMemoryState[] newArray(int size) {
            return new ProcessMemoryState[size];
        }
    };
    public long cacheInBytes;
    public int oomScore;
    public long pgfault;
    public long pgmajfault;
    public String processName;
    public long rssInBytes;
    public long swapInBytes;
    public int uid;

    /* synthetic */ ProcessMemoryState(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public ProcessMemoryState(int uid, String processName, int oomScore, long pgfault, long pgmajfault, long rssInBytes, long cacheInBytes, long swapInBytes) {
        this.uid = uid;
        this.processName = processName;
        this.oomScore = oomScore;
        this.pgfault = pgfault;
        this.pgmajfault = pgmajfault;
        this.rssInBytes = rssInBytes;
        this.cacheInBytes = cacheInBytes;
        this.swapInBytes = swapInBytes;
    }

    private ProcessMemoryState(Parcel in) {
        this.uid = in.readInt();
        this.processName = in.readString();
        this.oomScore = in.readInt();
        this.pgfault = in.readLong();
        this.pgmajfault = in.readLong();
        this.rssInBytes = in.readLong();
        this.cacheInBytes = in.readLong();
        this.swapInBytes = in.readLong();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.uid);
        parcel.writeString(this.processName);
        parcel.writeInt(this.oomScore);
        parcel.writeLong(this.pgfault);
        parcel.writeLong(this.pgmajfault);
        parcel.writeLong(this.rssInBytes);
        parcel.writeLong(this.cacheInBytes);
        parcel.writeLong(this.swapInBytes);
    }
}
