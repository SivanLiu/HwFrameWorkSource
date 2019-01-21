package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConstants.Field;
import javax.xml.namespace.QName;

public abstract class Duration {
    public abstract Duration add(Duration duration);

    public abstract void addTo(Calendar calendar);

    public abstract int compare(Duration duration);

    public abstract Number getField(Field field);

    public abstract int getSign();

    public abstract int hashCode();

    public abstract boolean isSet(Field field);

    public abstract Duration multiply(BigDecimal bigDecimal);

    public abstract Duration negate();

    public abstract Duration normalizeWith(Calendar calendar);

    public QName getXMLSchemaType() {
        boolean yearSet = isSet(DatatypeConstants.YEARS);
        boolean monthSet = isSet(DatatypeConstants.MONTHS);
        boolean daySet = isSet(DatatypeConstants.DAYS);
        boolean hourSet = isSet(DatatypeConstants.HOURS);
        boolean minuteSet = isSet(DatatypeConstants.MINUTES);
        boolean secondSet = isSet(DatatypeConstants.SECONDS);
        if (yearSet && monthSet && daySet && hourSet && minuteSet && secondSet) {
            return DatatypeConstants.DURATION;
        }
        if (!yearSet && !monthSet && daySet && hourSet && minuteSet && secondSet) {
            return DatatypeConstants.DURATION_DAYTIME;
        }
        if (yearSet && monthSet && !daySet && !hourSet && !minuteSet && !secondSet) {
            return DatatypeConstants.DURATION_YEARMONTH;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("javax.xml.datatype.Duration#getXMLSchemaType(): this Duration does not match one of the XML Schema date/time datatypes: year set = ");
        stringBuilder.append(yearSet);
        stringBuilder.append(" month set = ");
        stringBuilder.append(monthSet);
        stringBuilder.append(" day set = ");
        stringBuilder.append(daySet);
        stringBuilder.append(" hour set = ");
        stringBuilder.append(hourSet);
        stringBuilder.append(" minute set = ");
        stringBuilder.append(minuteSet);
        stringBuilder.append(" second set = ");
        stringBuilder.append(secondSet);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public int getYears() {
        return getFieldValueAsInt(DatatypeConstants.YEARS);
    }

    public int getMonths() {
        return getFieldValueAsInt(DatatypeConstants.MONTHS);
    }

    public int getDays() {
        return getFieldValueAsInt(DatatypeConstants.DAYS);
    }

    public int getHours() {
        return getFieldValueAsInt(DatatypeConstants.HOURS);
    }

    public int getMinutes() {
        return getFieldValueAsInt(DatatypeConstants.MINUTES);
    }

    public int getSeconds() {
        return getFieldValueAsInt(DatatypeConstants.SECONDS);
    }

    public long getTimeInMillis(Calendar startInstant) {
        Calendar cal = (Calendar) startInstant.clone();
        addTo(cal);
        return getCalendarTimeInMillis(cal) - getCalendarTimeInMillis(startInstant);
    }

    public long getTimeInMillis(Date startInstant) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(startInstant);
        addTo(cal);
        return getCalendarTimeInMillis(cal) - startInstant.getTime();
    }

    private int getFieldValueAsInt(Field field) {
        Number n = getField(field);
        if (n != null) {
            return n.intValue();
        }
        return 0;
    }

    public void addTo(Date date) {
        if (date != null) {
            Calendar cal = new GregorianCalendar();
            cal.setTime(date);
            addTo(cal);
            date.setTime(getCalendarTimeInMillis(cal));
            return;
        }
        throw new NullPointerException("date == null");
    }

    public Duration subtract(Duration rhs) {
        return add(rhs.negate());
    }

    public Duration multiply(int factor) {
        return multiply(BigDecimal.valueOf((long) factor));
    }

    public boolean isLongerThan(Duration duration) {
        return compare(duration) == 1;
    }

    public boolean isShorterThan(Duration duration) {
        return compare(duration) == -1;
    }

    public boolean equals(Object duration) {
        boolean z = true;
        if (duration == this) {
            return true;
        }
        if (!(duration instanceof Duration)) {
            return false;
        }
        if (compare((Duration) duration) != 0) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (getSign() < 0) {
            buf.append('-');
        }
        buf.append('P');
        BigInteger years = (BigInteger) getField(DatatypeConstants.YEARS);
        if (years != null) {
            buf.append(years);
            buf.append('Y');
        }
        BigInteger months = (BigInteger) getField(DatatypeConstants.MONTHS);
        if (months != null) {
            buf.append(months);
            buf.append('M');
        }
        BigInteger days = (BigInteger) getField(DatatypeConstants.DAYS);
        if (days != null) {
            buf.append(days);
            buf.append('D');
        }
        BigInteger hours = (BigInteger) getField(DatatypeConstants.HOURS);
        BigInteger minutes = (BigInteger) getField(DatatypeConstants.MINUTES);
        BigDecimal seconds = (BigDecimal) getField(DatatypeConstants.SECONDS);
        if (!(hours == null && minutes == null && seconds == null)) {
            buf.append('T');
            if (hours != null) {
                buf.append(hours);
                buf.append('H');
            }
            if (minutes != null) {
                buf.append(minutes);
                buf.append('M');
            }
            if (seconds != null) {
                buf.append(toString(seconds));
                buf.append('S');
            }
        }
        return buf.toString();
    }

    private String toString(BigDecimal bd) {
        String intString = bd.unscaledValue().toString();
        int scale = bd.scale();
        if (scale == 0) {
            return intString;
        }
        int insertionPoint = intString.length() - scale;
        StringBuilder stringBuilder;
        if (insertionPoint == 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("0.");
            stringBuilder.append(intString);
            return stringBuilder.toString();
        }
        if (insertionPoint > 0) {
            stringBuilder = new StringBuilder(intString);
            stringBuilder.insert(insertionPoint, '.');
        } else {
            stringBuilder = new StringBuilder((3 - insertionPoint) + intString.length());
            stringBuilder.append("0.");
            for (int i = 0; i < (-insertionPoint); i++) {
                stringBuilder.append('0');
            }
            stringBuilder.append(intString);
        }
        return stringBuilder.toString();
    }

    private static long getCalendarTimeInMillis(Calendar cal) {
        return cal.getTime().getTime();
    }
}
