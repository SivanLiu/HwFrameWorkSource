package android.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
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
            return new RecurrenceRule(source);
        }

        public RecurrenceRule[] newArray(int size) {
            return new RecurrenceRule[size];
        }
    };
    private static final boolean DEBUG = true;
    private static final String TAG = "RecurrenceRule";
    private static final int VERSION_INIT = 0;
    public static Clock sClock = Clock.systemDefaultZone();
    public final ZonedDateTime end;
    public final Period period;
    public final ZonedDateTime start;

    private class NonrecurringIterator implements Iterator<Pair<ZonedDateTime, ZonedDateTime>> {
        boolean hasNext;
        final /* synthetic */ RecurrenceRule this$0;

        public NonrecurringIterator(RecurrenceRule this$0) {
            boolean z = false;
            this.this$0 = this$0;
            if (!(this$0.start == null || this$0.end == null)) {
                z = true;
            }
            this.hasNext = z;
        }

        public boolean hasNext() {
            return this.hasNext;
        }

        public Pair<ZonedDateTime, ZonedDateTime> next() {
            this.hasNext = false;
            return new Pair(this.this$0.start, this.this$0.end);
        }
    }

    private class RecurringIterator implements Iterator<Pair<ZonedDateTime, ZonedDateTime>> {
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
            Log.d(RecurrenceRule.TAG, "Resolving using anchor " + anchor);
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

        public Pair<ZonedDateTime, ZonedDateTime> next() {
            Log.d(RecurrenceRule.TAG, "Cycle " + this.i + " from " + this.cycleStart + " to " + this.cycleEnd);
            Pair<ZonedDateTime, ZonedDateTime> p = new Pair(this.cycleStart, this.cycleEnd);
            this.i--;
            updateCycle();
            return p;
        }
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
        switch (version) {
            case 0:
                this.start = convertZonedDateTime(BackupUtils.readString(in));
                this.end = convertZonedDateTime(BackupUtils.readString(in));
                this.period = convertPeriod(BackupUtils.readString(in));
                break;
        }
        throw new ProtocolException("Unknown version " + version);
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(0);
        BackupUtils.writeString(out, convertZonedDateTime(this.start));
        BackupUtils.writeString(out, convertZonedDateTime(this.end));
        BackupUtils.writeString(out, convertPeriod(this.period));
    }

    public String toString() {
        return "RecurrenceRule{" + "start=" + this.start + " end=" + this.end + " period=" + this.period + "}";
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
        if (Objects.equals(this.start, other.start) && Objects.equals(this.end, other.end)) {
            z = Objects.equals(this.period, other.period);
        }
        return z;
    }

    @Deprecated
    public boolean isMonthly() {
        if (this.start == null || this.period == null || this.period.getYears() != 0 || this.period.getMonths() != 1) {
            return false;
        }
        return this.period.getDays() == 0;
    }

    public Iterator<Pair<ZonedDateTime, ZonedDateTime>> cycleIterator() {
        if (this.period != null) {
            return new RecurringIterator();
        }
        return new NonrecurringIterator(this);
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
