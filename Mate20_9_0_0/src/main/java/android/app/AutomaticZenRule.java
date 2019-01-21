package android.app;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;

public final class AutomaticZenRule implements Parcelable {
    public static final Creator<AutomaticZenRule> CREATOR = new Creator<AutomaticZenRule>() {
        public AutomaticZenRule createFromParcel(Parcel source) {
            return new AutomaticZenRule(source);
        }

        public AutomaticZenRule[] newArray(int size) {
            return new AutomaticZenRule[size];
        }
    };
    private Uri conditionId;
    private long creationTime;
    private boolean enabled;
    private int interruptionFilter;
    private String name;
    private ComponentName owner;

    public AutomaticZenRule(String name, ComponentName owner, Uri conditionId, int interruptionFilter, boolean enabled) {
        this.enabled = false;
        this.name = name;
        this.owner = owner;
        this.conditionId = conditionId;
        this.interruptionFilter = interruptionFilter;
        this.enabled = enabled;
    }

    public AutomaticZenRule(String name, ComponentName owner, Uri conditionId, int interruptionFilter, boolean enabled, long creationTime) {
        this(name, owner, conditionId, interruptionFilter, enabled);
        this.creationTime = creationTime;
    }

    public AutomaticZenRule(Parcel source) {
        boolean z = false;
        this.enabled = false;
        if (source.readInt() == 1) {
            z = true;
        }
        this.enabled = z;
        if (source.readInt() == 1) {
            this.name = source.readString();
        }
        this.interruptionFilter = source.readInt();
        this.conditionId = (Uri) source.readParcelable(null);
        this.owner = (ComponentName) source.readParcelable(null);
        this.creationTime = source.readLong();
    }

    public ComponentName getOwner() {
        return this.owner;
    }

    public Uri getConditionId() {
        return this.conditionId;
    }

    public int getInterruptionFilter() {
        return this.interruptionFilter;
    }

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public void setConditionId(Uri conditionId) {
        this.conditionId = conditionId;
    }

    public void setInterruptionFilter(int interruptionFilter) {
        this.interruptionFilter = interruptionFilter;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.enabled);
        if (this.name != null) {
            dest.writeInt(1);
            dest.writeString(this.name);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.interruptionFilter);
        dest.writeParcelable(this.conditionId, 0);
        dest.writeParcelable(this.owner, 0);
        dest.writeLong(this.creationTime);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(AutomaticZenRule.class.getSimpleName());
        stringBuilder.append('[');
        stringBuilder.append("enabled=");
        stringBuilder.append(this.enabled);
        stringBuilder.append(",name=");
        stringBuilder.append(this.name);
        stringBuilder.append(",interruptionFilter=");
        stringBuilder.append(this.interruptionFilter);
        stringBuilder.append(",conditionId=");
        stringBuilder.append(this.conditionId);
        stringBuilder.append(",owner=");
        stringBuilder.append(this.owner);
        stringBuilder.append(",creationTime=");
        stringBuilder.append(this.creationTime);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    public boolean equals(Object o) {
        if (!(o instanceof AutomaticZenRule)) {
            return false;
        }
        boolean z = true;
        if (o == this) {
            return true;
        }
        AutomaticZenRule other = (AutomaticZenRule) o;
        if (!(other.enabled == this.enabled && Objects.equals(other.name, this.name) && other.interruptionFilter == this.interruptionFilter && Objects.equals(other.conditionId, this.conditionId) && Objects.equals(other.owner, this.owner) && other.creationTime == this.creationTime)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Boolean.valueOf(this.enabled), this.name, Integer.valueOf(this.interruptionFilter), this.conditionId, this.owner, Long.valueOf(this.creationTime)});
    }
}
