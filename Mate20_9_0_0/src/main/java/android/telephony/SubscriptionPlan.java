package android.telephony;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Range;
import android.util.RecurrenceRule;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Objects;

@SystemApi
public final class SubscriptionPlan implements Parcelable {
    public static final long BYTES_UNKNOWN = -1;
    public static final long BYTES_UNLIMITED = Long.MAX_VALUE;
    public static final Creator<SubscriptionPlan> CREATOR = new Creator<SubscriptionPlan>() {
        public SubscriptionPlan createFromParcel(Parcel source) {
            return new SubscriptionPlan(source, null);
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
            this.plan = new SubscriptionPlan(new RecurrenceRule(start, end, period), null);
        }

        public static Builder createNonrecurring(ZonedDateTime start, ZonedDateTime end) {
            if (end.isAfter(start)) {
                return new Builder(start, end, null);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("End ");
            stringBuilder.append(end);
            stringBuilder.append(" isn't after start ");
            stringBuilder.append(start);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public static Builder createRecurring(ZonedDateTime start, Period period) {
            if (!period.isZero() && !period.isNegative()) {
                return new Builder(start, null, period);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Period ");
            stringBuilder.append(period);
            stringBuilder.append(" must be positive");
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        @SystemApi
        @Deprecated
        public static Builder createRecurringMonthly(ZonedDateTime start) {
            return new Builder(start, null, Period.ofMonths(1));
        }

        @SystemApi
        @Deprecated
        public static Builder createRecurringWeekly(ZonedDateTime start) {
            return new Builder(start, null, Period.ofDays(7));
        }

        @SystemApi
        @Deprecated
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
            } else if (dataLimitBehavior >= 0) {
                this.plan.dataLimitBytes = dataLimitBytes;
                this.plan.dataLimitBehavior = dataLimitBehavior;
                return this;
            } else {
                throw new IllegalArgumentException("Limit behavior must be defined");
            }
        }

        public Builder setDataUsage(long dataUsageBytes, long dataUsageTime) {
            if (dataUsageBytes < 0) {
                throw new IllegalArgumentException("Usage bytes must be positive");
            } else if (dataUsageTime >= 0) {
                this.plan.dataUsageBytes = dataUsageBytes;
                this.plan.dataUsageTime = dataUsageTime;
                return this;
            } else {
                throw new IllegalArgumentException("Usage time must be positive");
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface LimitBehavior {
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
        StringBuilder stringBuilder = new StringBuilder("SubscriptionPlan{");
        stringBuilder.append("cycleRule=");
        stringBuilder.append(this.cycleRule);
        stringBuilder.append(" title=");
        stringBuilder.append(this.title);
        stringBuilder.append(" summary=");
        stringBuilder.append(this.summary);
        stringBuilder.append(" dataLimitBytes=");
        stringBuilder.append(this.dataLimitBytes);
        stringBuilder.append(" dataLimitBehavior=");
        stringBuilder.append(this.dataLimitBehavior);
        stringBuilder.append(" dataUsageBytes=");
        stringBuilder.append(this.dataUsageBytes);
        stringBuilder.append(" dataUsageTime=");
        stringBuilder.append(this.dataUsageTime);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.cycleRule, this.title, this.summary, Long.valueOf(this.dataLimitBytes), Integer.valueOf(this.dataLimitBehavior), Long.valueOf(this.dataUsageBytes), Long.valueOf(this.dataUsageTime)});
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof SubscriptionPlan)) {
            return false;
        }
        SubscriptionPlan other = (SubscriptionPlan) obj;
        if (Objects.equals(this.cycleRule, other.cycleRule) && Objects.equals(this.title, other.title) && Objects.equals(this.summary, other.summary) && this.dataLimitBytes == other.dataLimitBytes && this.dataLimitBehavior == other.dataLimitBehavior && this.dataUsageBytes == other.dataUsageBytes && this.dataUsageTime == other.dataUsageTime) {
            z = true;
        }
        return z;
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

    public Iterator<Range<ZonedDateTime>> cycleIterator() {
        return this.cycleRule.cycleIterator();
    }
}
