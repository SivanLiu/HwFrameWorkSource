package android.hardware.location;

import android.annotation.SystemApi;
import android.app.backup.FullBackup;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

@SystemApi
public class MemoryRegion implements Parcelable {
    public static final Creator<MemoryRegion> CREATOR = new Creator<MemoryRegion>() {
        public MemoryRegion createFromParcel(Parcel in) {
            return new MemoryRegion(in);
        }

        public MemoryRegion[] newArray(int size) {
            return new MemoryRegion[size];
        }
    };
    private boolean mIsExecutable;
    private boolean mIsReadable;
    private boolean mIsWritable;
    private int mSizeBytes;
    private int mSizeBytesFree;

    public int getCapacityBytes() {
        return this.mSizeBytes;
    }

    public int getFreeCapacityBytes() {
        return this.mSizeBytesFree;
    }

    public boolean isReadable() {
        return this.mIsReadable;
    }

    public boolean isWritable() {
        return this.mIsWritable;
    }

    public boolean isExecutable() {
        return this.mIsExecutable;
    }

    public String toString() {
        StringBuilder stringBuilder;
        String mask = "";
        if (isReadable()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(mask);
            stringBuilder.append(FullBackup.ROOT_TREE_TOKEN);
            mask = stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(mask);
            stringBuilder.append("-");
            mask = stringBuilder.toString();
        }
        if (isWritable()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(mask);
            stringBuilder.append("w");
            mask = stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(mask);
            stringBuilder.append("-");
            mask = stringBuilder.toString();
        }
        if (isExecutable()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(mask);
            stringBuilder.append("x");
            mask = stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(mask);
            stringBuilder.append("-");
            mask = stringBuilder.toString();
        }
        String retVal = new StringBuilder();
        retVal.append("[ ");
        retVal.append(this.mSizeBytesFree);
        retVal.append("/ ");
        retVal.append(this.mSizeBytes);
        retVal.append(" ] : ");
        retVal.append(mask);
        return retVal.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSizeBytes);
        dest.writeInt(this.mSizeBytesFree);
        dest.writeInt(this.mIsReadable);
        dest.writeInt(this.mIsWritable);
        dest.writeInt(this.mIsExecutable);
    }

    public MemoryRegion(Parcel source) {
        this.mSizeBytes = source.readInt();
        this.mSizeBytesFree = source.readInt();
        boolean z = false;
        this.mIsReadable = source.readInt() != 0;
        this.mIsWritable = source.readInt() != 0;
        if (source.readInt() != 0) {
            z = true;
        }
        this.mIsExecutable = z;
    }
}
