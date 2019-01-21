package android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class WindowAnimationFrameStats extends FrameStats implements Parcelable {
    public static final Creator<WindowAnimationFrameStats> CREATOR = new Creator<WindowAnimationFrameStats>() {
        public WindowAnimationFrameStats createFromParcel(Parcel parcel) {
            return new WindowAnimationFrameStats(parcel, null);
        }

        public WindowAnimationFrameStats[] newArray(int size) {
            return new WindowAnimationFrameStats[size];
        }
    };

    /* synthetic */ WindowAnimationFrameStats(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public void init(long refreshPeriodNano, long[] framesPresentedTimeNano) {
        this.mRefreshPeriodNano = refreshPeriodNano;
        this.mFramesPresentedTimeNano = framesPresentedTimeNano;
    }

    private WindowAnimationFrameStats(Parcel parcel) {
        this.mRefreshPeriodNano = parcel.readLong();
        this.mFramesPresentedTimeNano = parcel.createLongArray();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(this.mRefreshPeriodNano);
        parcel.writeLongArray(this.mFramesPresentedTimeNano);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WindowAnimationFrameStats[");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("frameCount:");
        stringBuilder.append(getFrameCount());
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", fromTimeNano:");
        stringBuilder.append(getStartTimeNano());
        builder.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", toTimeNano:");
        stringBuilder.append(getEndTimeNano());
        builder.append(stringBuilder.toString());
        builder.append(']');
        return builder.toString();
    }
}
