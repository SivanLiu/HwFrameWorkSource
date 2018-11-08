package android.app;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Slog;
import java.io.IOException;

public class ProfilerInfo implements Parcelable {
    public static final Creator<ProfilerInfo> CREATOR = new Creator<ProfilerInfo>() {
        public ProfilerInfo createFromParcel(Parcel in) {
            return new ProfilerInfo(in);
        }

        public ProfilerInfo[] newArray(int size) {
            return new ProfilerInfo[size];
        }
    };
    private static final String TAG = "ProfilerInfo";
    public final String agent;
    public final boolean autoStopProfiler;
    public ParcelFileDescriptor profileFd;
    public final String profileFile;
    public final int samplingInterval;
    public final boolean streamingOutput;

    public ProfilerInfo(String filename, ParcelFileDescriptor fd, int interval, boolean autoStop, boolean streaming, String agent) {
        this.profileFile = filename;
        this.profileFd = fd;
        this.samplingInterval = interval;
        this.autoStopProfiler = autoStop;
        this.streamingOutput = streaming;
        this.agent = agent;
    }

    public ProfilerInfo(ProfilerInfo in) {
        this.profileFile = in.profileFile;
        this.profileFd = in.profileFd;
        this.samplingInterval = in.samplingInterval;
        this.autoStopProfiler = in.autoStopProfiler;
        this.streamingOutput = in.streamingOutput;
        this.agent = in.agent;
    }

    public void closeFd() {
        if (this.profileFd != null) {
            try {
                this.profileFd.close();
            } catch (IOException e) {
                Slog.w(TAG, "Failure closing profile fd", e);
            }
            this.profileFd = null;
        }
    }

    public int describeContents() {
        if (this.profileFd != null) {
            return this.profileFd.describeContents();
        }
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        int i;
        int i2 = 1;
        out.writeString(this.profileFile);
        if (this.profileFd != null) {
            out.writeInt(1);
            this.profileFd.writeToParcel(out, flags);
        } else {
            out.writeInt(0);
        }
        out.writeInt(this.samplingInterval);
        if (this.autoStopProfiler) {
            i = 1;
        } else {
            i = 0;
        }
        out.writeInt(i);
        if (!this.streamingOutput) {
            i2 = 0;
        }
        out.writeInt(i2);
        out.writeString(this.agent);
    }

    private ProfilerInfo(Parcel in) {
        boolean z;
        boolean z2 = true;
        this.profileFile = in.readString();
        this.profileFd = in.readInt() != 0 ? (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(in) : null;
        this.samplingInterval = in.readInt();
        if (in.readInt() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.autoStopProfiler = z;
        if (in.readInt() == 0) {
            z2 = false;
        }
        this.streamingOutput = z2;
        this.agent = in.readString();
    }
}
