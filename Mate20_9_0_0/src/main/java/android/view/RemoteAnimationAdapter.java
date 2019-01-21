package android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.view.IRemoteAnimationRunner.Stub;

public class RemoteAnimationAdapter implements Parcelable {
    public static final Creator<RemoteAnimationAdapter> CREATOR = new Creator<RemoteAnimationAdapter>() {
        public RemoteAnimationAdapter createFromParcel(Parcel in) {
            return new RemoteAnimationAdapter(in);
        }

        public RemoteAnimationAdapter[] newArray(int size) {
            return new RemoteAnimationAdapter[size];
        }
    };
    private int mCallingPid;
    private final long mDuration;
    private final IRemoteAnimationRunner mRunner;
    private final long mStatusBarTransitionDelay;

    public RemoteAnimationAdapter(IRemoteAnimationRunner runner, long duration, long statusBarTransitionDelay) {
        this.mRunner = runner;
        this.mDuration = duration;
        this.mStatusBarTransitionDelay = statusBarTransitionDelay;
    }

    public RemoteAnimationAdapter(Parcel in) {
        this.mRunner = Stub.asInterface(in.readStrongBinder());
        this.mDuration = in.readLong();
        this.mStatusBarTransitionDelay = in.readLong();
    }

    public IRemoteAnimationRunner getRunner() {
        return this.mRunner;
    }

    public long getDuration() {
        return this.mDuration;
    }

    public long getStatusBarTransitionDelay() {
        return this.mStatusBarTransitionDelay;
    }

    public void setCallingPid(int pid) {
        this.mCallingPid = pid;
    }

    public int getCallingPid() {
        return this.mCallingPid;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongInterface(this.mRunner);
        dest.writeLong(this.mDuration);
        dest.writeLong(this.mStatusBarTransitionDelay);
    }
}
