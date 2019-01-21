package android.service.notification;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SystemApi
public final class NotificationStats implements Parcelable {
    public static final Creator<NotificationStats> CREATOR = new Creator<NotificationStats>() {
        public NotificationStats createFromParcel(Parcel in) {
            return new NotificationStats(in);
        }

        public NotificationStats[] newArray(int size) {
            return new NotificationStats[size];
        }
    };
    public static final int DISMISSAL_AOD = 2;
    public static final int DISMISSAL_NOT_DISMISSED = -1;
    public static final int DISMISSAL_OTHER = 0;
    public static final int DISMISSAL_PEEK = 1;
    public static final int DISMISSAL_SHADE = 3;
    private boolean mDirectReplied;
    private int mDismissalSurface = -1;
    private boolean mExpanded;
    private boolean mInteracted;
    private boolean mSeen;
    private boolean mSnoozed;
    private boolean mViewedSettings;

    @Retention(RetentionPolicy.SOURCE)
    public @interface DismissalSurface {
    }

    protected NotificationStats(Parcel in) {
        boolean z = false;
        this.mSeen = in.readByte() != (byte) 0;
        this.mExpanded = in.readByte() != (byte) 0;
        this.mDirectReplied = in.readByte() != (byte) 0;
        this.mSnoozed = in.readByte() != (byte) 0;
        this.mViewedSettings = in.readByte() != (byte) 0;
        if (in.readByte() != (byte) 0) {
            z = true;
        }
        this.mInteracted = z;
        this.mDismissalSurface = in.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) this.mSeen);
        dest.writeByte((byte) this.mExpanded);
        dest.writeByte((byte) this.mDirectReplied);
        dest.writeByte((byte) this.mSnoozed);
        dest.writeByte((byte) this.mViewedSettings);
        dest.writeByte((byte) this.mInteracted);
        dest.writeInt(this.mDismissalSurface);
    }

    public int describeContents() {
        return 0;
    }

    public boolean hasSeen() {
        return this.mSeen;
    }

    public void setSeen() {
        this.mSeen = true;
    }

    public boolean hasExpanded() {
        return this.mExpanded;
    }

    public void setExpanded() {
        this.mExpanded = true;
        this.mInteracted = true;
    }

    public boolean hasDirectReplied() {
        return this.mDirectReplied;
    }

    public void setDirectReplied() {
        this.mDirectReplied = true;
        this.mInteracted = true;
    }

    public boolean hasSnoozed() {
        return this.mSnoozed;
    }

    public void setSnoozed() {
        this.mSnoozed = true;
        this.mInteracted = true;
    }

    public boolean hasViewedSettings() {
        return this.mViewedSettings;
    }

    public void setViewedSettings() {
        this.mViewedSettings = true;
        this.mInteracted = true;
    }

    public boolean hasInteracted() {
        return this.mInteracted;
    }

    public int getDismissalSurface() {
        return this.mDismissalSurface;
    }

    public void setDismissalSurface(int dismissalSurface) {
        this.mDismissalSurface = dismissalSurface;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationStats that = (NotificationStats) o;
        if (this.mSeen != that.mSeen || this.mExpanded != that.mExpanded || this.mDirectReplied != that.mDirectReplied || this.mSnoozed != that.mSnoozed || this.mViewedSettings != that.mViewedSettings || this.mInteracted != that.mInteracted) {
            return false;
        }
        if (this.mDismissalSurface != that.mDismissalSurface) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * this.mSeen) + this.mExpanded)) + this.mDirectReplied)) + this.mSnoozed)) + this.mViewedSettings)) + this.mInteracted)) + this.mDismissalSurface;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("NotificationStats{");
        sb.append("mSeen=");
        sb.append(this.mSeen);
        sb.append(", mExpanded=");
        sb.append(this.mExpanded);
        sb.append(", mDirectReplied=");
        sb.append(this.mDirectReplied);
        sb.append(", mSnoozed=");
        sb.append(this.mSnoozed);
        sb.append(", mViewedSettings=");
        sb.append(this.mViewedSettings);
        sb.append(", mInteracted=");
        sb.append(this.mInteracted);
        sb.append(", mDismissalSurface=");
        sb.append(this.mDismissalSurface);
        sb.append('}');
        return sb.toString();
    }
}
