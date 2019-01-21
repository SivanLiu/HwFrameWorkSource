package android.icu.impl.duration;

import android.icu.text.DurationFormat;
import android.icu.util.ULocale;
import java.text.FieldPosition;
import java.util.Date;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeConstants.Field;
import javax.xml.datatype.Duration;

public class BasicDurationFormat extends DurationFormat {
    private static final long serialVersionUID = -3146984141909457700L;
    transient DurationFormatter formatter;
    transient PeriodFormatter pformatter;
    transient PeriodFormatterService pfs;

    public static BasicDurationFormat getInstance(ULocale locale) {
        return new BasicDurationFormat(locale);
    }

    public StringBuffer format(Object object, StringBuffer toAppend, FieldPosition pos) {
        if (object instanceof Long) {
            toAppend.append(formatDurationFromNow(((Long) object).longValue()));
            return toAppend;
        } else if (object instanceof Date) {
            toAppend.append(formatDurationFromNowTo((Date) object));
            return toAppend;
        } else if (object instanceof Duration) {
            toAppend.append(formatDuration(object));
            return toAppend;
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Duration");
        }
    }

    public BasicDurationFormat() {
        this.pfs = null;
        this.pfs = BasicPeriodFormatterService.getInstance();
        this.formatter = this.pfs.newDurationFormatterFactory().getFormatter();
        this.pformatter = this.pfs.newPeriodFormatterFactory().setDisplayPastFuture(false).getFormatter();
    }

    public BasicDurationFormat(ULocale locale) {
        super(locale);
        this.pfs = null;
        this.pfs = BasicPeriodFormatterService.getInstance();
        this.formatter = this.pfs.newDurationFormatterFactory().setLocale(locale.getName()).getFormatter();
        this.pformatter = this.pfs.newPeriodFormatterFactory().setDisplayPastFuture(false).setLocale(locale.getName()).getFormatter();
    }

    public String formatDurationFrom(long duration, long referenceDate) {
        return this.formatter.formatDurationFrom(duration, referenceDate);
    }

    public String formatDurationFromNow(long duration) {
        return this.formatter.formatDurationFromNow(duration);
    }

    public String formatDurationFromNowTo(Date targetDate) {
        return this.formatter.formatDurationFromNowTo(targetDate);
    }

    public String formatDuration(Object obj) {
        inFields = new Field[6];
        int i = 0;
        inFields[0] = DatatypeConstants.YEARS;
        inFields[1] = DatatypeConstants.MONTHS;
        inFields[2] = DatatypeConstants.DAYS;
        inFields[3] = DatatypeConstants.HOURS;
        inFields[4] = DatatypeConstants.MINUTES;
        inFields[5] = DatatypeConstants.SECONDS;
        TimeUnit[] outFields = new TimeUnit[]{TimeUnit.YEAR, TimeUnit.MONTH, TimeUnit.DAY, TimeUnit.HOUR, TimeUnit.MINUTE, TimeUnit.SECOND};
        Duration inDuration = (Duration) obj;
        Period p = null;
        Duration duration = inDuration;
        boolean inPast = false;
        if (inDuration.getSign() < 0) {
            duration = inDuration.negate();
            inPast = true;
        }
        boolean sawNonZero = false;
        while (i < inFields.length) {
            Field[] inFields;
            if (duration.isSet(inFields[i])) {
                Number n = duration.getField(inFields[i]);
                if (n.intValue() != 0 || sawNonZero) {
                    boolean sawNonZero2;
                    float floatVal = n.floatValue();
                    TimeUnit alternateUnit = null;
                    float alternateVal = 0.0f;
                    if (outFields[i] == TimeUnit.SECOND) {
                        double fullSeconds = (double) floatVal;
                        inFields = inFields;
                        inFields = Math.floor((double) floatVal);
                        sawNonZero2 = true;
                        sawNonZero = (fullSeconds - inFields) * 1000.0d;
                        if (sawNonZero > 0.0d) {
                            alternateUnit = TimeUnit.MILLISECOND;
                            alternateVal = (float) sawNonZero;
                            floatVal = (float) inFields;
                        }
                    } else {
                        inFields = inFields;
                        sawNonZero2 = true;
                        Number number = n;
                    }
                    if (p == null) {
                        inFields = Period.at(floatVal, outFields[i]);
                    } else {
                        inFields = p.and(floatVal, outFields[i]);
                    }
                    if (alternateUnit != null) {
                        inFields = inFields.and(alternateVal, alternateUnit);
                    }
                    p = inFields;
                    sawNonZero = sawNonZero2;
                } else {
                    inFields = inFields;
                }
            } else {
                inFields = inFields;
            }
            i++;
            inFields = inFields;
        }
        if (p == null) {
            return formatDurationFromNow(0);
        }
        Period p2;
        if (inPast) {
            p2 = p.inPast();
        } else {
            p2 = p.inFuture();
        }
        return this.pformatter.format(p2);
    }
}
