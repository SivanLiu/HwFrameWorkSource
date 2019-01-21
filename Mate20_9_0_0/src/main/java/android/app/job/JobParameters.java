package android.app.job;

import android.app.job.IJobCallback.Stub;
import android.content.ClipData;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.PersistableBundle;
import android.os.RemoteException;

public class JobParameters implements Parcelable {
    public static final Creator<JobParameters> CREATOR = new Creator<JobParameters>() {
        public JobParameters createFromParcel(Parcel in) {
            return new JobParameters(in, null);
        }

        public JobParameters[] newArray(int size) {
            return new JobParameters[size];
        }
    };
    public static final int REASON_CANCELED = 0;
    public static final int REASON_CONSTRAINTS_NOT_SATISFIED = 1;
    public static final int REASON_DEVICE_IDLE = 4;
    public static final int REASON_PREEMPT = 2;
    public static final int REASON_TIMEOUT = 3;
    private final IBinder callback;
    private final ClipData clipData;
    private final int clipGrantFlags;
    private String debugStopReason;
    private final PersistableBundle extras;
    private final int jobId;
    private final String[] mTriggeredContentAuthorities;
    private final Uri[] mTriggeredContentUris;
    private final Network network;
    private final boolean overrideDeadlineExpired;
    private int stopReason;
    private final Bundle transientExtras;

    /* synthetic */ JobParameters(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public static String getReasonName(int reason) {
        switch (reason) {
            case 0:
                return "canceled";
            case 1:
                return "constraints";
            case 2:
                return "preempt";
            case 3:
                return "timeout";
            case 4:
                return "device_idle";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown:");
                stringBuilder.append(reason);
                return stringBuilder.toString();
        }
    }

    public JobParameters(IBinder callback, int jobId, PersistableBundle extras, Bundle transientExtras, ClipData clipData, int clipGrantFlags, boolean overrideDeadlineExpired, Uri[] triggeredContentUris, String[] triggeredContentAuthorities, Network network) {
        this.jobId = jobId;
        this.extras = extras;
        this.transientExtras = transientExtras;
        this.clipData = clipData;
        this.clipGrantFlags = clipGrantFlags;
        this.callback = callback;
        this.overrideDeadlineExpired = overrideDeadlineExpired;
        this.mTriggeredContentUris = triggeredContentUris;
        this.mTriggeredContentAuthorities = triggeredContentAuthorities;
        this.network = network;
    }

    public int getJobId() {
        return this.jobId;
    }

    public int getStopReason() {
        return this.stopReason;
    }

    public String getDebugStopReason() {
        return this.debugStopReason;
    }

    public PersistableBundle getExtras() {
        return this.extras;
    }

    public Bundle getTransientExtras() {
        return this.transientExtras;
    }

    public ClipData getClipData() {
        return this.clipData;
    }

    public int getClipGrantFlags() {
        return this.clipGrantFlags;
    }

    public boolean isOverrideDeadlineExpired() {
        return this.overrideDeadlineExpired;
    }

    public Uri[] getTriggeredContentUris() {
        return this.mTriggeredContentUris;
    }

    public String[] getTriggeredContentAuthorities() {
        return this.mTriggeredContentAuthorities;
    }

    public Network getNetwork() {
        return this.network;
    }

    public JobWorkItem dequeueWork() {
        try {
            return getCallback().dequeueWork(getJobId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void completeWork(JobWorkItem work) {
        try {
            if (!getCallback().completeWork(getJobId(), work.getWorkId())) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Given work is not active: ");
                stringBuilder.append(work);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public IJobCallback getCallback() {
        return Stub.asInterface(this.callback);
    }

    private JobParameters(Parcel in) {
        this.jobId = in.readInt();
        this.extras = in.readPersistableBundle();
        this.transientExtras = in.readBundle();
        boolean z = false;
        if (in.readInt() != 0) {
            this.clipData = (ClipData) ClipData.CREATOR.createFromParcel(in);
            this.clipGrantFlags = in.readInt();
        } else {
            this.clipData = null;
            this.clipGrantFlags = 0;
        }
        this.callback = in.readStrongBinder();
        if (in.readInt() == 1) {
            z = true;
        }
        this.overrideDeadlineExpired = z;
        this.mTriggeredContentUris = (Uri[]) in.createTypedArray(Uri.CREATOR);
        this.mTriggeredContentAuthorities = in.createStringArray();
        if (in.readInt() != 0) {
            this.network = (Network) Network.CREATOR.createFromParcel(in);
        } else {
            this.network = null;
        }
        this.stopReason = in.readInt();
        this.debugStopReason = in.readString();
    }

    public void setStopReason(int reason, String debugStopReason) {
        this.stopReason = reason;
        this.debugStopReason = debugStopReason;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.jobId);
        dest.writePersistableBundle(this.extras);
        dest.writeBundle(this.transientExtras);
        if (this.clipData != null) {
            dest.writeInt(1);
            this.clipData.writeToParcel(dest, flags);
            dest.writeInt(this.clipGrantFlags);
        } else {
            dest.writeInt(0);
        }
        dest.writeStrongBinder(this.callback);
        dest.writeInt(this.overrideDeadlineExpired);
        dest.writeTypedArray(this.mTriggeredContentUris, flags);
        dest.writeStringArray(this.mTriggeredContentAuthorities);
        if (this.network != null) {
            dest.writeInt(1);
            this.network.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.stopReason);
        dest.writeString(this.debugStopReason);
    }
}
