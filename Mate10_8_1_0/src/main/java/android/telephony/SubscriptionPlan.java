package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Pair;
import android.util.RecurrenceRule;
import com.android.internal.util.Preconditions;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Iterator;

public final class SubscriptionPlan implements Parcelable {
    public static final long BYTES_UNKNOWN = -1;
    public static final long BYTES_UNLIMITED = Long.MAX_VALUE;
    public static final Creator<SubscriptionPlan> CREATOR = new Creator<SubscriptionPlan>() {
        public SubscriptionPlan createFromParcel(Parcel source) {
            return new SubscriptionPlan(source);
        }

        public SubscriptionPlan[] newArray(int size) {
            return new SubscriptionPlan[size];
        }
    };
    public static final int LIMIT_BEHAVIOR_BILLED = 1;
    public static final int LIMIT_BEHAVIOR_DISABLED = 0;
    public static final int LIMIT_BEHAVIOR_THROTTLED = 2;
    public static final int LIMIT_BEHAVIOR_UNKNOWN = -1;
    public static final long TIME_UNKNOWN = -1;
    private final RecurrenceRule cycleRule;
    private int dataLimitBehavior;
    private long dataLimitBytes;
    private long dataUsageBytes;
    private long dataUsageTime;
    private CharSequence summary;
    private CharSequence title;

    public static class Builder {
        private final SubscriptionPlan plan;

        public Builder(ZonedDateTime start, ZonedDateTime end, Period period) {
            this.plan = new SubscriptionPlan(new RecurrenceRule(start, end, period));
        }

        public static Builder createNonrecurring(ZonedDateTime start, ZonedDateTime end) {
            if (end.isAfter(start)) {
                return new Builder(start, end, null);
            }
            throw new IllegalArgumentException("End " + end + " isn't after start " + start);
        }

        public static Builder createRecurringMonthly(ZonedDateTime start) {
            return new Builder(start, null, Period.ofMonths(1));
        }

        public static Builder createRecurringWeekly(ZonedDateTime start) {
            return new Builder(start, null, Period.ofDays(7));
        }

        public static Builder createRecurringDaily(ZonedDateTime start) {
            return new Builder(start, null, Period.ofDays(1));
        }

        public SubscriptionPlan build() {
            return this.plan;
        }

        public Builder setTitle(CharSequence title) {
            this.plan.title = title;
            return this;
        }

        public Builder setSummary(CharSequence summary) {
            this.plan.summary = summary;
            return this;
        }

        public Builder setDataLimit(long dataLimitBytes, int dataLimitBehavior) {
            if (dataLimitBytes < 0) {
                throw new IllegalArgumentException("Limit bytes must be positive");
            } else if (dataLimitBehavior < 0) {
                throw new IllegalArgumentException("Limit behavior must be defined");
            } else {
                this.plan.dataLimitBytes = dataLimitBytes;
                this.plan.dataLimitBehavior = dataLimitBehavior;
                return this;
            }
        }

        public Builder setDataUsage(long dataUsageBytes, long dataUsageTime) {
            if (dataUsageBytes < 0) {
                throw new IllegalArgumentException("Usage bytes must be positive");
            } else if (dataUsageTime < 0) {
                throw new IllegalArgumentException("Usage time must be positive");
            } else {
                this.plan.dataUsageBytes = dataUsageBytes;
                this.plan.dataUsageTime = dataUsageTime;
                return this;
            }
        }
    }

    private SubscriptionPlan(RecurrenceRule cycleRule) {
        this.dataLimitBytes = -1;
        this.dataLimitBehavior = -1;
        this.dataUsageBytes = -1;
        this.dataUsageTime = -1;
        this.cycleRule = (RecurrenceRule) Preconditions.checkNotNull(cycleRule);
    }

    private SubscriptionPlan(Parcel source) {
        this.dataLimitBytes = -1;
        this.dataLimitBehavior = -1;
        this.dataUsageBytes = -1;
        this.dataUsageTime = -1;
        this.cycleRule = (RecurrenceRule) source.readParcelable(null);
        this.title = source.readCharSequence();
        this.summary = source.readCharSequence();
        this.dataLimitBytes = source.readLong();
        this.dataLimitBehavior = source.readInt();
        this.dataUsageBytes = source.readLong();
        this.dataUsageTime = source.readLong();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.cycleRule, flags);
        dest.writeCharSequence(this.title);
        dest.writeCharSequence(this.summary);
        dest.writeLong(this.dataLimitBytes);
        dest.writeInt(this.dataLimitBehavior);
        dest.writeLong(this.dataUsageBytes);
        dest.writeLong(this.dataUsageTime);
    }

    public String toString() {
        return "SubscriptionPlan{" + "cycleRule=" + this.cycleRule + " title=" + this.title + " summary=" + this.summary + " dataLimitBytes=" + this.dataLimitBytes + " dataLimitBehavior=" + this.dataLimitBehavior + " dataUsageBytes=" + this.dataUsageBytes + " dataUsageTime=" + this.dataUsageTime + "}";
    }

    public RecurrenceRule getCycleRule() {
        return this.cycleRule;
    }

    public CharSequence getTitle() {
        return this.title;
    }

    public CharSequence getSummary() {
        return this.summary;
    }

    public long getDataLimitBytes() {
        return this.dataLimitBytes;
    }

    public int getDataLimitBehavior() {
        return this.dataLimitBehavior;
    }

    public long getDataUsageBytes() {
        return this.dataUsageBytes;
    }

    public long getDataUsageTime() {
        return this.dataUsageTime;
    }

    public Iterator<Pair<ZonedDateTime, ZonedDateTime>> cycleIterator() {
        return this.cycleRule.cycleIterator();
    }
}
