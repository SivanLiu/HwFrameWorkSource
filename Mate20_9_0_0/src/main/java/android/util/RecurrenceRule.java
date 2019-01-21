package android.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.android.internal.annotations.VisibleForTesting;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.time.Clock;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Objects;

public class RecurrenceRule implements Parcelable {
    public static final Creator<RecurrenceRule> CREATOR = new Creator<RecurrenceRule>() {
        public RecurrenceRule createFromParcel(Parcel source) {
            return new RecurrenceRule(source, null);
        }

        public RecurrenceRule[] newArray(int size) {
            return new RecurrenceRule[size];
        }
    };
    private static final boolean LOGD = Log.isLoggable(TAG, 3);
    private static final String TAG = "RecurrenceRule";
    private static final int VERSION_INIT = 0;
    @VisibleForTesting
    public static Clock sClock = Clock.systemDefaultZone();
    public final ZonedDateTime end;
    public final Period period;
    public final ZonedDateTime start;

    private class NonrecurringIterator implements Iterator<Range<ZonedDateTime>> {
        boolean hasNext;

        public NonrecurringIterator() {
            boolean z = (RecurrenceRule.this.start == null || RecurrenceRule.this.end == null) ? false : true;
            this.hasNext = z;
        }

        public boolean hasNext() {
            return this.hasNext;
        }

        public Range<ZonedDateTime> next() {
            this.hasNext = false;
            return new Range(RecurrenceRule.this.start, RecurrenceRule.this.end);
        }
    }

    private class RecurringIterator implements Iterator<Range<ZonedDateTime>> {
        ZonedDateTime cycleEnd;
        ZonedDateTime cycleStart;
        int i;

        public RecurringIterator() {
            ZonedDateTime anchor;
            if (RecurrenceRule.this.end != null) {
                anchor = RecurrenceRule.this.end;
            } else {
                anchor = ZonedDateTime.now(RecurrenceRule.sClock).withZoneSameInstant(RecurrenceRule.this.start.getZone());
            }
            if (RecurrenceRule.LOGD) {
                String str = RecurrenceRule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Resolving using anchor ");
                stringBuilder.append(anchor);
                Log.d(str, stringBuilder.toString());
            }
            updateCycle();
            while (anchor.toEpochSecond() > this.cycleEnd.toEpochSecond()) {
                this.i++;
                updateCycle();
            }
            while (anchor.toEpochSecond() <= this.cycleStart.toEpochSecond()) {
                this.i--;
                updateCycle();
            }
        }

        private void updateCycle() {
            this.cycleStart = roundBoundaryTime(RecurrenceRule.this.start.plus(RecurrenceRule.this.period.multipliedBy(this.i)));
            this.cycleEnd = roundBoundaryTime(RecurrenceRule.this.start.plus(RecurrenceRule.this.period.multipliedBy(this.i + 1)));
        }

        private ZonedDateTime roundBoundaryTime(ZonedDateTime boundary) {
            if (!RecurrenceRule.this.isMonthly() || boundary.getDayOfMonth() >= RecurrenceRule.this.start.getDayOfMonth()) {
                return boundary;
            }
            return ZonedDateTime.of(boundary.toLocalDate(), LocalTime.MAX, RecurrenceRule.this.start.getZone());
        }

        public boolean hasNext() {
            return this.cycleStart.toEpochSecond() >= RecurrenceRule.this.start.toEpochSecond();
        }

        public Range<ZonedDateTime> next() {
            if (RecurrenceRule.LOGD) {
                String str = RecurrenceRule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cycle ");
                stringBuilder.append(this.i);
                stringBuilder.append(" from ");
                stringBuilder.append(this.cycleStart);
                stringBuilder.append(" to ");
                stringBuilder.append(this.cycleEnd);
                Log.d(str, stringBuilder.toString());
            }
            Range<ZonedDateTime> r = new Range(this.cycleStart, this.cycleEnd);
            this.i--;
            updateCycle();
            return r;
        }
    }

    /* synthetic */ RecurrenceRule(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public RecurrenceRule(ZonedDateTime start, ZonedDateTime end, Period period) {
        this.start = start;
        this.end = end;
        this.period = period;
    }

    @Deprecated
    public static RecurrenceRule buildNever() {
        return new RecurrenceRule(null, null, null);
    }

    @Deprecated
    public static RecurrenceRule buildRecurringMonthly(int dayOfMonth, ZoneId zone) {
        return new RecurrenceRule(ZonedDateTime.of(ZonedDateTime.now(sClock).withZoneSameInstant(zone).toLocalDate().minusYears(1).withMonth(1).withDayOfMonth(dayOfMonth), LocalTime.MIDNIGHT, zone), null, Period.ofMonths(1));
    }

    private RecurrenceRule(Parcel source) {
        this.start = convertZonedDateTime(source.readString());
        this.end = convertZonedDateTime(source.readString());
        this.period = convertPeriod(source.readString());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(convertZonedDateTime(this.start));
        dest.writeString(convertZonedDateTime(this.end));
        dest.writeString(convertPeriod(this.period));
    }

    public RecurrenceRule(DataInputStream in) throws IOException {
        int version = in.readInt();
        if (version == 0) {
            this.start = convertZonedDateTime(BackupUtils.readString(in));
            this.end = convertZonedDateTime(BackupUtils.readString(in));
            this.period = convertPeriod(BackupUtils.readString(in));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown version ");
        stringBuilder.append(version);
        throw new ProtocolException(stringBuilder.toString());
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(0);
        BackupUtils.writeString(out, convertZonedDateTime(this.start));
        BackupUtils.writeString(out, convertZonedDateTime(this.end));
        BackupUtils.writeString(out, convertPeriod(this.period));
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("RecurrenceRule{");
        stringBuilder.append("start=");
        stringBuilder.append(this.start);
        stringBuilder.append(" end=");
        stringBuilder.append(this.end);
        stringBuilder.append(" period=");
        stringBuilder.append(this.period);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.start, this.end, this.period});
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof RecurrenceRule)) {
            return false;
        }
        RecurrenceRule other = (RecurrenceRule) obj;
        if (Objects.equals(this.start, other.start) && Objects.equals(this.end, other.end) && Objects.equals(this.period, other.period)) {
            z = true;
        }
        return z;
    }

    public boolean isRecurring() {
        return this.period != null;
    }

    @Deprecated
    public boolean isMonthly() {
        if (this.start != null && this.period != null && this.period.getYears() == 0 && this.period.getMonths() == 1 && this.period.getDays() == 0) {
            return true;
        }
        return false;
    }

    public Iterator<Range<ZonedDateTime>> cycleIterator() {
        if (this.period != null) {
            return new RecurringIterator();
        }
        return new NonrecurringIterator();
    }

    public static String convertZonedDateTime(ZonedDateTime time) {
        return time != null ? time.toString() : null;
    }

    public static ZonedDateTime convertZonedDateTime(String time) {
        return time != null ? ZonedDateTime.parse(time) : null;
    }

    public static String convertPeriod(Period period) {
        return period != null ? period.toString() : null;
    }

    public static Period convertPeriod(String period) {
        return period != null ? Period.parse(period) : null;
    }
}
