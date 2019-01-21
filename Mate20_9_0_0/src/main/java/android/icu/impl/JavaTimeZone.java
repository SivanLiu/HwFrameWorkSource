package android.icu.impl;

import android.icu.util.TimeZone;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TreeSet;

public class JavaTimeZone extends TimeZone {
    private static final TreeSet<String> AVAILABLESET = new TreeSet();
    private static Method mObservesDaylightTime = null;
    private static final long serialVersionUID = 6977448185543929364L;
    private volatile transient boolean isFrozen;
    private transient Calendar javacal;
    private java.util.TimeZone javatz;

    static {
        String[] availableIds = java.util.TimeZone.getAvailableIDs();
        for (Object add : availableIds) {
            AVAILABLESET.add(add);
        }
        try {
            mObservesDaylightTime = java.util.TimeZone.class.getMethod("observesDaylightTime", (Class[]) null);
        } catch (NoSuchMethodException | SecurityException e) {
        }
    }

    public JavaTimeZone() {
        this(java.util.TimeZone.getDefault(), null);
    }

    public JavaTimeZone(java.util.TimeZone jtz, String id) {
        this.isFrozen = false;
        if (id == null) {
            id = jtz.getID();
        }
        this.javatz = jtz;
        setID(id);
        this.javacal = new GregorianCalendar(this.javatz);
    }

    public static JavaTimeZone createTimeZone(String id) {
        java.util.TimeZone jtz = null;
        if (AVAILABLESET.contains(id)) {
            jtz = java.util.TimeZone.getTimeZone(id);
        }
        if (jtz == null) {
            boolean[] isSystemID = new boolean[1];
            String canonicalID = TimeZone.getCanonicalID(id, isSystemID);
            if (isSystemID[0] && AVAILABLESET.contains(canonicalID)) {
                jtz = java.util.TimeZone.getTimeZone(canonicalID);
            }
        }
        if (jtz == null) {
            return null;
        }
        return new JavaTimeZone(jtz, id);
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return this.javatz.getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }

    public void getOffset(long date, boolean local, int[] offsets) {
        long j = date;
        synchronized (this.javacal) {
            int sec1 = 1;
            if (local) {
                int doy1 = 6;
                try {
                    int hour;
                    int[] fields = new int[6];
                    Grego.timeToFields(j, fields);
                    int tmp = fields[5];
                    int mil = tmp % 1000;
                    tmp /= 1000;
                    int sec = tmp % 60;
                    tmp /= 60;
                    int min = tmp % 60;
                    int hour2 = tmp / 60;
                    this.javacal.clear();
                    int hour3 = hour2;
                    int min2 = min;
                    this.javacal.set(fields[0], fields[1], fields[2], hour3, min2, sec);
                    this.javacal.set(14, mil);
                    doy1 = this.javacal.get(6);
                    int hour1 = this.javacal.get(11);
                    int min1 = this.javacal.get(12);
                    sec1 = this.javacal.get(13);
                    hour2 = this.javacal.get(14);
                    if (fields[4] == doy1) {
                        hour = hour3;
                        if (hour == hour1) {
                            min = min2;
                            if (min == min1 && sec == sec1) {
                                if (mil != hour2) {
                                }
                            }
                        } else {
                            min = min2;
                        }
                    } else {
                        hour = hour3;
                        min = min2;
                    }
                    int dayDelta = Math.abs(doy1 - fields[4]) > 1 ? 1 : doy1 - fields[4];
                    this.javacal.setTimeInMillis((this.javacal.getTimeInMillis() - ((long) ((((((((((((dayDelta * 24) + hour1) - hour) * 60) + min1) - min) * 60) + sec1) - sec) * 1000) + hour2) - mil))) - 1);
                } finally {
                }
            } else {
                this.javacal.setTimeInMillis(j);
            }
            offsets[0] = this.javacal.get(15);
            offsets[1] = this.javacal.get(16);
        }
    }

    public int getRawOffset() {
        return this.javatz.getRawOffset();
    }

    public boolean inDaylightTime(Date date) {
        return this.javatz.inDaylightTime(date);
    }

    public void setRawOffset(int offsetMillis) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen JavaTimeZone instance.");
        }
        this.javatz.setRawOffset(offsetMillis);
    }

    public boolean useDaylightTime() {
        return this.javatz.useDaylightTime();
    }

    public boolean observesDaylightTime() {
        if (mObservesDaylightTime != null) {
            try {
                return ((Boolean) mObservesDaylightTime.invoke(this.javatz, (Object[]) null)).booleanValue();
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            }
        }
        return super.observesDaylightTime();
    }

    public int getDSTSavings() {
        return this.javatz.getDSTSavings();
    }

    public java.util.TimeZone unwrap() {
        return this.javatz;
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    public int hashCode() {
        return super.hashCode() + this.javatz.hashCode();
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.javacal = new GregorianCalendar(this.javatz);
    }

    public boolean isFrozen() {
        return this.isFrozen;
    }

    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    public TimeZone cloneAsThawed() {
        JavaTimeZone tz = (JavaTimeZone) super.cloneAsThawed();
        tz.javatz = (java.util.TimeZone) this.javatz.clone();
        tz.javacal = new GregorianCalendar(this.javatz);
        tz.isFrozen = false;
        return tz;
    }
}
