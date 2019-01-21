package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import java.util.Objects;

public class PipModeChangeItem extends ClientTransactionItem {
    public static final Creator<PipModeChangeItem> CREATOR = new Creator<PipModeChangeItem>() {
        public PipModeChangeItem createFromParcel(Parcel in) {
            return new PipModeChangeItem(in, null);
        }

        public PipModeChangeItem[] newArray(int size) {
            return new PipModeChangeItem[size];
        }
    };
    private boolean mIsInPipMode;
    private Configuration mOverrideConfig;

    /* synthetic */ PipModeChangeItem(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public void execute(ClientTransactionHandler client, IBinder token, PendingTransactionActions pendingActions) {
        client.handlePictureInPictureModeChanged(token, this.mIsInPipMode, this.mOverrideConfig);
    }

    private PipModeChangeItem() {
    }

    public static PipModeChangeItem obtain(boolean isInPipMode, Configuration overrideConfig) {
        PipModeChangeItem instance = (PipModeChangeItem) ObjectPool.obtain(PipModeChangeItem.class);
        if (instance == null) {
            instance = new PipModeChangeItem();
        }
        instance.mIsInPipMode = isInPipMode;
        instance.mOverrideConfig = overrideConfig;
        return instance;
    }

    public void recycle() {
        this.mIsInPipMode = false;
        this.mOverrideConfig = null;
        ObjectPool.recycle(this);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.mIsInPipMode);
        dest.writeTypedObject(this.mOverrideConfig, flags);
    }

    private PipModeChangeItem(Parcel in) {
        this.mIsInPipMode = in.readBoolean();
        this.mOverrideConfig = (Configuration) in.readTypedObject(Configuration.CREATOR);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipModeChangeItem other = (PipModeChangeItem) o;
        if (!(this.mIsInPipMode == other.mIsInPipMode && Objects.equals(this.mOverrideConfig, other.mOverrideConfig))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * 17) + this.mIsInPipMode)) + this.mOverrideConfig.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PipModeChangeItem{isInPipMode=");
        stringBuilder.append(this.mIsInPipMode);
        stringBuilder.append(",overrideConfig=");
        stringBuilder.append(this.mOverrideConfig);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
