package android.icu.util;

public class TimeZoneTransition {
    private final TimeZoneRule from;
    private final long time;
    private final TimeZoneRule to;

    public TimeZoneTransition(long time, TimeZoneRule from, TimeZoneRule to) {
        this.time = time;
        this.from = from;
        this.to = to;
    }

    public long getTime() {
        return this.time;
    }

    public TimeZoneRule getTo() {
        return this.to;
    }

    public TimeZoneRule getFrom() {
        return this.from;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("time=");
        stringBuilder.append(this.time);
        buf.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", from={");
        stringBuilder.append(this.from);
        stringBuilder.append("}");
        buf.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", to={");
        stringBuilder.append(this.to);
        stringBuilder.append("}");
        buf.append(stringBuilder.toString());
        return buf.toString();
    }
}
