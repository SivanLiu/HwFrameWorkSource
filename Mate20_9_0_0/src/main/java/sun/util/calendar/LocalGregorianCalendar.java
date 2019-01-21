package sun.util.calendar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class LocalGregorianCalendar extends BaseCalendar {
    private Era[] eras;
    private String name;

    public static class Date extends sun.util.calendar.BaseCalendar.Date {
        private int gregorianYear = Integer.MIN_VALUE;

        protected Date() {
        }

        protected Date(TimeZone zone) {
            super(zone);
        }

        public Date setEra(Era era) {
            if (getEra() != era) {
                super.setEra(era);
                this.gregorianYear = Integer.MIN_VALUE;
            }
            return this;
        }

        public Date addYear(int localYear) {
            super.addYear(localYear);
            this.gregorianYear += localYear;
            return this;
        }

        public Date setYear(int localYear) {
            if (getYear() != localYear) {
                super.setYear(localYear);
                this.gregorianYear = Integer.MIN_VALUE;
            }
            return this;
        }

        public int getNormalizedYear() {
            return this.gregorianYear;
        }

        public void setNormalizedYear(int normalizedYear) {
            this.gregorianYear = normalizedYear;
        }

        void setLocalEra(Era era) {
            super.setEra(era);
        }

        void setLocalYear(int year) {
            super.setYear(year);
        }

        public String toString() {
            String time = super.toString();
            time = time.substring(time.indexOf(84));
            StringBuffer sb = new StringBuffer();
            Era era = getEra();
            if (era != null) {
                String abbr = era.getAbbreviation();
                if (abbr != null) {
                    sb.append(abbr);
                }
            }
            sb.append(getYear());
            sb.append('.');
            CalendarUtils.sprintf0d(sb, getMonth(), 2).append('.');
            CalendarUtils.sprintf0d(sb, getDayOfMonth(), 2);
            sb.append(time);
            return sb.toString();
        }
    }

    static LocalGregorianCalendar getLocalGregorianCalendar(String name) {
        String str = name;
        try {
            Properties calendarProps = CalendarSystem.getCalendarProperties();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calendar.");
            stringBuilder.append(str);
            stringBuilder.append(".eras");
            String props = calendarProps.getProperty(stringBuilder.toString());
            LocalGregorianCalendar localGregorianCalendar = null;
            if (props == null) {
                return null;
            }
            String str2;
            StringTokenizer stringTokenizer;
            List<Era> eras = new ArrayList();
            StringTokenizer eraTokens = new StringTokenizer(props, ";");
            while (eraTokens.hasMoreTokens()) {
                Properties calendarProps2;
                StringTokenizer itemTokens = new StringTokenizer(eraTokens.nextToken().trim(), ",");
                String eraName = null;
                boolean localTime = true;
                long since = 0;
                String abbr = localGregorianCalendar;
                while (itemTokens.hasMoreTokens()) {
                    String item = itemTokens.nextToken();
                    int index = item.indexOf(61);
                    if (index == -1) {
                        return localGregorianCalendar;
                    }
                    String key = item.substring(0, index);
                    String value = item.substring(index + 1);
                    calendarProps2 = calendarProps;
                    if ("name".equals(key) != null) {
                        eraName = value;
                    } else if ("since".equals(key) != null) {
                        if (value.endsWith("u") != null) {
                            boolean localTime2 = null;
                            since = Long.parseLong(value.substring(null, value.length() - 1));
                            localTime = localTime2;
                        } else {
                            since = Long.parseLong(value);
                        }
                    } else if ("abbr".equals(key) != null) {
                        abbr = value;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown key word: ");
                        stringBuilder.append(key);
                        throw new RuntimeException(stringBuilder.toString());
                    }
                    calendarProps = calendarProps2;
                    localGregorianCalendar = null;
                }
                calendarProps2 = calendarProps;
                str2 = props;
                stringTokenizer = eraTokens;
                eras.add(new Era(eraName, abbr, since, localTime));
                calendarProps = calendarProps2;
                localGregorianCalendar = null;
            }
            str2 = props;
            stringTokenizer = eraTokens;
            if (eras.isEmpty()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("No eras for ");
                stringBuilder.append(str);
                throw new RuntimeException(stringBuilder.toString());
            }
            Era[] eraArray = new Era[eras.size()];
            eras.toArray(eraArray);
            return new LocalGregorianCalendar(str, eraArray);
        } catch (IOException | IllegalArgumentException e) {
            Throwable th = e;
            throw new InternalError(e);
        }
    }

    private LocalGregorianCalendar(String name, Era[] eras) {
        this.name = name;
        this.eras = eras;
        setEras(eras);
    }

    public String getName() {
        return this.name;
    }

    public Date getCalendarDate() {
        return getCalendarDate(System.currentTimeMillis(), newCalendarDate());
    }

    public Date getCalendarDate(long millis) {
        return getCalendarDate(millis, newCalendarDate());
    }

    public Date getCalendarDate(long millis, TimeZone zone) {
        return getCalendarDate(millis, newCalendarDate(zone));
    }

    public Date getCalendarDate(long millis, CalendarDate date) {
        Date ldate = (Date) super.getCalendarDate(millis, date);
        return adjustYear(ldate, millis, ldate.getZoneOffset());
    }

    private Date adjustYear(Date ldate, long millis, int zoneOffset) {
        int i = this.eras.length - 1;
        while (i >= 0) {
            Era era = this.eras[i];
            long since = era.getSince(null);
            if (era.isLocalTime()) {
                since -= (long) zoneOffset;
            }
            if (millis >= since) {
                ldate.setLocalEra(era);
                ldate.setLocalYear((ldate.getNormalizedYear() - era.getSinceDate().getYear()) + 1);
                break;
            }
            i--;
        }
        if (i < 0) {
            ldate.setLocalEra(null);
            ldate.setLocalYear(ldate.getNormalizedYear());
        }
        ldate.setNormalized(true);
        return ldate;
    }

    public Date newCalendarDate() {
        return new Date();
    }

    public Date newCalendarDate(TimeZone zone) {
        return new Date(zone);
    }

    public boolean validate(CalendarDate date) {
        Date ldate = (Date) date;
        Era era = ldate.getEra();
        if (era != null) {
            if (!validateEra(era)) {
                return false;
            }
            ldate.setNormalizedYear((era.getSinceDate().getYear() + ldate.getYear()) - 1);
            Date tmp = newCalendarDate(date.getZone());
            tmp.setEra(era).setDate(date.getYear(), date.getMonth(), date.getDayOfMonth());
            normalize(tmp);
            if (tmp.getEra() != era) {
                return false;
            }
        } else if (date.getYear() >= this.eras[0].getSinceDate().getYear()) {
            return false;
        } else {
            ldate.setNormalizedYear(ldate.getYear());
        }
        return super.validate(ldate);
    }

    private boolean validateEra(Era era) {
        for (Era era2 : this.eras) {
            if (era == era2) {
                return true;
            }
        }
        return false;
    }

    public boolean normalize(CalendarDate date) {
        if (date.isNormalized()) {
            return true;
        }
        normalizeYear(date);
        Date ldate = (Date) date;
        super.normalize(ldate);
        boolean hasMillis = false;
        long millis = 0;
        int year = ldate.getNormalizedYear();
        Era era = null;
        int i = this.eras.length - 1;
        while (i >= 0) {
            era = this.eras[i];
            if (era.isLocalTime()) {
                CalendarDate sinceDate = era.getSinceDate();
                int sinceYear = sinceDate.getYear();
                if (year > sinceYear) {
                    break;
                } else if (year == sinceYear) {
                    int month = ldate.getMonth();
                    int sinceMonth = sinceDate.getMonth();
                    if (month > sinceMonth) {
                        break;
                    } else if (month == sinceMonth) {
                        int day = ldate.getDayOfMonth();
                        int sinceDay = sinceDate.getDayOfMonth();
                        if (day > sinceDay) {
                            break;
                        } else if (day == sinceDay) {
                            if (ldate.getTimeOfDay() < sinceDate.getTimeOfDay()) {
                                i--;
                            }
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } else {
                if (!hasMillis) {
                    millis = super.getTime(date);
                    hasMillis = true;
                }
                if (millis >= era.getSince(date.getZone())) {
                    break;
                }
            }
            i--;
        }
        if (i >= 0) {
            ldate.setLocalEra(era);
            ldate.setLocalYear((ldate.getNormalizedYear() - era.getSinceDate().getYear()) + 1);
        } else {
            ldate.setEra(null);
            ldate.setLocalYear(year);
            ldate.setNormalizedYear(year);
        }
        ldate.setNormalized(true);
        return true;
    }

    void normalizeMonth(CalendarDate date) {
        normalizeYear(date);
        super.normalizeMonth(date);
    }

    void normalizeYear(CalendarDate date) {
        Date ldate = (Date) date;
        Era era = ldate.getEra();
        if (era == null || !validateEra(era)) {
            ldate.setNormalizedYear(ldate.getYear());
        } else {
            ldate.setNormalizedYear((era.getSinceDate().getYear() + ldate.getYear()) - 1);
        }
    }

    public boolean isLeapYear(int gregorianYear) {
        return CalendarUtils.isGregorianLeapYear(gregorianYear);
    }

    public boolean isLeapYear(Era era, int year) {
        if (era == null) {
            return isLeapYear(year);
        }
        return isLeapYear((era.getSinceDate().getYear() + year) - 1);
    }

    public void getCalendarDateFromFixedDate(CalendarDate date, long fixedDate) {
        Date ldate = (Date) date;
        super.getCalendarDateFromFixedDate(ldate, fixedDate);
        adjustYear(ldate, (fixedDate - 719163) * 86400000, 0);
    }
}
