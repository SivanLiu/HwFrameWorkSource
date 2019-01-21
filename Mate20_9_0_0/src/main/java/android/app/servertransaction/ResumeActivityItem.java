package android.app.servertransaction;

import android.app.ActivityManager;
import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.os.Trace;

public class ResumeActivityItem extends ActivityLifecycleItem {
    public static final Creator<ResumeActivityItem> CREATOR = new Creator<ResumeActivityItem>() {
        public ResumeActivityItem createFromParcel(Parcel in) {
            return new ResumeActivityItem(in, null);
        }

        public ResumeActivityItem[] newArray(int size) {
            return new ResumeActivityItem[size];
        }
    };
    private static final String TAG = "ResumeActivityItem";
    private boolean mIsForward;
    private int mProcState;
    private boolean mUpdateProcState;

    /* synthetic */ ResumeActivityItem(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public void preExecute(ClientTransactionHandler client, IBinder token) {
        if (this.mUpdateProcState) {
            client.updateProcessState(this.mProcState, false);
        }
    }

    public void execute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        Trace.traceBegin(64, "activityResume");
        client.handleResumeActivity(token, true, this.mIsForward, "RESUME_ACTIVITY");
        Trace.traceEnd(64);
    }

    public void postExecute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        try {
            ActivityManager.getService().activityResumed(token);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public int getTargetState() {
        return 3;
    }

    private ResumeActivityItem() {
    }

    public static ResumeActivityItem obtain(int procState, boolean isForward) {
        ResumeActivityItem instance = (ResumeActivityItem) ObjectPool.obtain(ResumeActivityItem.class);
        if (instance == null) {
            instance = new ResumeActivityItem();
        }
        instance.mProcState = procState;
        instance.mUpdateProcState = true;
        instance.mIsForward = isForward;
        return instance;
    }

    public static ResumeActivityItem obtain(boolean isForward) {
        ResumeActivityItem instance = (ResumeActivityItem) ObjectPool.obtain(ResumeActivityItem.class);
        if (instance == null) {
            instance = new ResumeActivityItem();
        }
        instance.mProcState = -1;
        instance.mUpdateProcState = false;
        instance.mIsForward = isForward;
        return instance;
    }

    public void recycle() {
        super.recycle();
        this.mProcState = -1;
        this.mUpdateProcState = false;
        this.mIsForward = false;
        ObjectPool.recycle(this);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mProcState);
        dest.writeBoolean(this.mUpdateProcState);
        dest.writeBoolean(this.mIsForward);
    }

    private ResumeActivityItem(Parcel in) {
        this.mProcState = in.readInt();
        this.mUpdateProcState = in.readBoolean();
        this.mIsForward = in.readBoolean();
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResumeActivityItem other = (ResumeActivityItem) o;
        if (!(this.mProcState == other.mProcState && this.mUpdateProcState == other.mUpdateProcState && this.mIsForward == other.mIsForward)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * 17) + this.mProcState)) + this.mUpdateProcState)) + this.mIsForward;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ResumeActivityItem{procState=");
        stringBuilder.append(this.mProcState);
        stringBuilder.append(",updateProcState=");
        stringBuilder.append(this.mUpdateProcState);
        stringBuilder.append(",isForward=");
        stringBuilder.append(this.mIsForward);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
