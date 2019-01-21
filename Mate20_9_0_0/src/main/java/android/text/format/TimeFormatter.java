package android.text.format;

import android.content.res.Resources;
import java.nio.CharBuffer;
import java.util.Formatter;
import java.util.Locale;
import libcore.icu.LocaleData;
import libcore.util.ZoneInfo;
import libcore.util.ZoneInfo.WallTime;

class TimeFormatter {
    private static final int DAYSPERLYEAR = 366;
    private static final int DAYSPERNYEAR = 365;
    private static final int DAYSPERWEEK = 7;
    private static final int FORCE_LOWER_CASE = -1;
    private static final int HOURSPERDAY = 24;
    private static final int MINSPERHOUR = 60;
    private static final int MONSPERYEAR = 12;
    private static final int SECSPERMIN = 60;
    private static String sDateOnlyFormat;
    private static String sDateTimeFormat;
    private static Locale sLocale;
    private static LocaleData sLocaleData;
    private static String sTimeOnlyFormat;
    private final String dateOnlyFormat;
    private final String dateTimeFormat;
    private final LocaleData localeData;
    private Formatter numberFormatter;
    private StringBuilder outputBuilder;
    private final String timeOnlyFormat;

    public TimeFormatter() {
        synchronized (TimeFormatter.class) {
            Locale locale = Locale.getDefault();
            if (sLocale == null || !locale.equals(sLocale)) {
                sLocale = locale;
                sLocaleData = LocaleData.get(locale);
                Resources r = Resources.getSystem();
                sTimeOnlyFormat = r.getString(17041243);
                sDateOnlyFormat = r.getString(17040537);
                sDateTimeFormat = r.getString(17039890);
            }
            this.dateTimeFormat = sDateTimeFormat;
            this.timeOnlyFormat = sTimeOnlyFormat;
            this.dateOnlyFormat = sDateOnlyFormat;
            this.localeData = sLocaleData;
        }
    }

    public String format(String pattern, WallTime wallTime, ZoneInfo zoneInfo) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            this.outputBuilder = stringBuilder;
            this.numberFormatter = new Formatter(stringBuilder, Locale.US);
            formatInternal(pattern, wallTime, zoneInfo);
            String result = stringBuilder.toString();
            if (this.localeData.zeroDigit != '0') {
                result = localizeDigits(result);
            }
            this.outputBuilder = null;
            this.numberFormatter = null;
            return result;
        } catch (Throwable th) {
            this.outputBuilder = null;
            this.numberFormatter = null;
        }
    }

    private String localizeDigits(String s) {
        int length = s.length();
        int offsetToLocalizedDigits = this.localeData.zeroDigit - 48;
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') {
                ch = (char) (ch + offsetToLocalizedDigits);
            }
            result.append(ch);
        }
        return result.toString();
    }

    private void formatInternal(String pattern, WallTime wallTime, ZoneInfo zoneInfo) {
        CharBuffer formatBuffer = CharBuffer.wrap(pattern);
        while (formatBuffer.remaining() > 0) {
            boolean outputCurrentChar = true;
            if (formatBuffer.get(formatBuffer.position()) == '%') {
                outputCurrentChar = handleToken(formatBuffer, wallTime, zoneInfo);
            }
            if (outputCurrentChar) {
                this.outputBuilder.append(formatBuffer.get(formatBuffer.position()));
            }
            formatBuffer.position(formatBuffer.position() + 1);
        }
    }

    /* JADX WARNING: Missing block: B:72:0x01e8, code skipped:
            if (r21.getMonth() < 0) goto L_0x01fc;
     */
    /* JADX WARNING: Missing block: B:74:0x01ee, code skipped:
            if (r21.getMonth() < 12) goto L_0x01f1;
     */
    /* JADX WARNING: Missing block: B:75:0x01f1, code skipped:
            r7 = r0.localeData.shortMonthNames[r21.getMonth()];
     */
    /* JADX WARNING: Missing block: B:76:0x01fc, code skipped:
            r7 = "?";
     */
    /* JADX WARNING: Missing block: B:77:0x01fe, code skipped:
            modifyAndAppend(r7, r5);
     */
    /* JADX WARNING: Missing block: B:78:0x0201, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:87:0x0221, code skipped:
            r5 = r6;
     */
    /* JADX WARNING: Missing block: B:232:0x000a, code skipped:
            continue;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean handleToken(CharBuffer formatBuffer, WallTime wallTime, ZoneInfo zoneInfo) {
        char currentChar;
        int day;
        int i;
        int weekDay;
        boolean z;
        CharBuffer charBuffer = formatBuffer;
        WallTime wallTime2 = wallTime;
        ZoneInfo zoneInfo2 = zoneInfo;
        boolean z2 = false;
        int modifier = 0;
        while (true) {
            boolean isDst = true;
            if (formatBuffer.remaining() <= 1) {
                return true;
            }
            charBuffer.position(formatBuffer.position() + 1);
            currentChar = charBuffer.get(formatBuffer.position());
            day = 7;
            i = 12;
            CharSequence charSequence;
            switch (currentChar) {
                case 'A':
                    charSequence = (wallTime.getWeekDay() < 0 || wallTime.getWeekDay() >= 7) ? "?" : this.localeData.longWeekdayNames[wallTime.getWeekDay() + 1];
                    modifyAndAppend(charSequence, modifier);
                    return false;
                case 'B':
                    if (modifier == 45) {
                        if (wallTime.getMonth() < 0 || wallTime.getMonth() >= 12) {
                            charSequence = "?";
                        } else {
                            charSequence = this.localeData.longStandAloneMonthNames[wallTime.getMonth()];
                        }
                        modifyAndAppend(charSequence, modifier);
                    } else {
                        charSequence = (wallTime.getMonth() < 0 || wallTime.getMonth() >= 12) ? "?" : this.localeData.longMonthNames[wallTime.getMonth()];
                        modifyAndAppend(charSequence, modifier);
                    }
                    return false;
                case 'C':
                    outputYear(wallTime.getYear(), true, false, modifier);
                    return false;
                case 'D':
                    formatInternal("%m/%d/%y", wallTime2, zoneInfo2);
                    return false;
                case 'E':
                    break;
                case 'F':
                    formatInternal("%Y-%m-%d", wallTime2, zoneInfo2);
                    return false;
                case 'G':
                    break;
                case 'H':
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(wallTime.getHour())});
                    return false;
                case 'I':
                    if (wallTime.getHour() % 12 != 0) {
                        i = wallTime.getHour() % 12;
                    }
                    day = i;
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(day)});
                    return false;
                default:
                    CharSequence charSequence2;
                    switch (currentChar) {
                        case 'O':
                            break;
                        case 'P':
                            if (wallTime.getHour() >= 12) {
                                charSequence2 = this.localeData.amPm[1];
                            } else {
                                charSequence2 = this.localeData.amPm[0];
                            }
                            modifyAndAppend(charSequence2, -1);
                            return false;
                        default:
                            switch (currentChar) {
                                case 'R':
                                    formatInternal(DateUtils.HOUR_MINUTE_24, wallTime2, zoneInfo2);
                                    return false;
                                case 'S':
                                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(wallTime.getSecond())});
                                    return false;
                                case 'T':
                                    formatInternal("%H:%M:%S", wallTime2, zoneInfo2);
                                    return false;
                                case 'U':
                                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(((wallTime.getYearDay() + 7) - wallTime.getWeekDay()) / 7)});
                                    return false;
                                case 'V':
                                    break;
                                case 'W':
                                    i = wallTime.getYearDay() + 7;
                                    if (wallTime.getWeekDay() != 0) {
                                        weekDay = wallTime.getWeekDay() - 1;
                                    } else {
                                        weekDay = 6;
                                    }
                                    i = (i - weekDay) / 7;
                                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(i)});
                                    return false;
                                case 'X':
                                    formatInternal(this.timeOnlyFormat, wallTime2, zoneInfo2);
                                    return false;
                                case 'Y':
                                    outputYear(wallTime.getYear(), true, true, modifier);
                                    return false;
                                case 'Z':
                                    if (wallTime.getIsDst() < 0) {
                                        return false;
                                    }
                                    if (wallTime.getIsDst() == 0) {
                                        isDst = false;
                                    }
                                    modifyAndAppend(zoneInfo2.getDisplayName(isDst, 0), modifier);
                                    return false;
                                default:
                                    switch (currentChar) {
                                        case '^':
                                        case '_':
                                            break;
                                        default:
                                            switch (currentChar) {
                                                case 'a':
                                                    charSequence2 = (wallTime.getWeekDay() < 0 || wallTime.getWeekDay() >= 7) ? "?" : this.localeData.shortWeekdayNames[wallTime.getWeekDay() + 1];
                                                    modifyAndAppend(charSequence2, modifier);
                                                    return false;
                                                case 'b':
                                                    break;
                                                case 'c':
                                                    formatInternal(this.dateTimeFormat, wallTime2, zoneInfo2);
                                                    return false;
                                                case 'd':
                                                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(wallTime.getMonthDay())});
                                                    return false;
                                                case 'e':
                                                    this.numberFormatter.format(getFormat(modifier, "%2d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(wallTime.getMonthDay())});
                                                    return false;
                                                default:
                                                    switch (currentChar) {
                                                        case 'g':
                                                            break;
                                                        case 'h':
                                                            break;
                                                        default:
                                                            switch (currentChar) {
                                                                case 'j':
                                                                    day = wallTime.getYearDay() + 1;
                                                                    this.numberFormatter.format(getFormat(modifier, "%03d", "%3d", "%d", "%03d"), new Object[]{Integer.valueOf(day)});
                                                                    return false;
                                                                case 'k':
                                                                    this.numberFormatter.format(getFormat(modifier, "%2d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(wallTime.getHour())});
                                                                    return false;
                                                                case 'l':
                                                                    if (wallTime.getHour() % 12 != 0) {
                                                                        i = wallTime.getHour() % 12;
                                                                    }
                                                                    day = i;
                                                                    this.numberFormatter.format(getFormat(modifier, "%2d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(day)});
                                                                    return false;
                                                                case 'm':
                                                                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(wallTime.getMonth() + 1)});
                                                                    return false;
                                                                case 'n':
                                                                    this.outputBuilder.append(10);
                                                                    return false;
                                                                default:
                                                                    switch (currentChar) {
                                                                        case 'r':
                                                                            formatInternal("%I:%M:%S %p", wallTime2, zoneInfo2);
                                                                            return false;
                                                                        case 's':
                                                                            this.outputBuilder.append(Integer.toString(wallTime.mktime(zoneInfo)));
                                                                            return false;
                                                                        case 't':
                                                                            this.outputBuilder.append(9);
                                                                            return false;
                                                                        case 'u':
                                                                            if (wallTime.getWeekDay() != 0) {
                                                                                day = wallTime.getWeekDay();
                                                                            }
                                                                            this.numberFormatter.format("%d", new Object[]{Integer.valueOf(day)});
                                                                            return false;
                                                                        case 'v':
                                                                            formatInternal("%e-%b-%Y", wallTime2, zoneInfo2);
                                                                            return false;
                                                                        case 'w':
                                                                            this.numberFormatter.format("%d", new Object[]{Integer.valueOf(wallTime.getWeekDay())});
                                                                            return false;
                                                                        case 'x':
                                                                            formatInternal(this.dateOnlyFormat, wallTime2, zoneInfo2);
                                                                            return false;
                                                                        case 'y':
                                                                            outputYear(wallTime.getYear(), false, true, modifier);
                                                                            return false;
                                                                        case 'z':
                                                                            if (wallTime.getIsDst() < 0) {
                                                                                return false;
                                                                            }
                                                                            char sign;
                                                                            day = wallTime.getGmtOffset();
                                                                            if (day < 0) {
                                                                                sign = '-';
                                                                                day = -day;
                                                                            } else {
                                                                                sign = '+';
                                                                            }
                                                                            this.outputBuilder.append(sign);
                                                                            day /= 60;
                                                                            weekDay = ((day / 60) * 100) + (day % 60);
                                                                            this.numberFormatter.format(getFormat(modifier, "%04d", "%4d", "%d", "%04d"), new Object[]{Integer.valueOf(weekDay)});
                                                                            return false;
                                                                        default:
                                                                            switch (currentChar) {
                                                                                case '#':
                                                                                case '-':
                                                                                case '0':
                                                                                    break;
                                                                                case '+':
                                                                                    formatInternal("%a %b %e %H:%M:%S %Z %Y", wallTime2, zoneInfo2);
                                                                                    return false;
                                                                                case 'M':
                                                                                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(wallTime.getMinute())});
                                                                                    return false;
                                                                                case 'p':
                                                                                    if (wallTime.getHour() >= 12) {
                                                                                        charSequence2 = this.localeData.amPm[1];
                                                                                    } else {
                                                                                        charSequence2 = this.localeData.amPm[0];
                                                                                    }
                                                                                    modifyAndAppend(charSequence2, modifier);
                                                                                    return false;
                                                                                default:
                                                                                    return true;
                                                                            }
                                                                    }
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
        }
        i = wallTime.getYear();
        weekDay = wallTime.getYearDay();
        int wday = wallTime.getWeekDay();
        while (true) {
            int len = isLeap(i) ? 366 : 365;
            int bot = (((weekDay + 11) - wday) % 7) - 3;
            int top = bot - (len % 7);
            if (top < -3) {
                top += 7;
            }
            if (weekDay >= top + len) {
                i++;
                day = 1;
            } else if (weekDay >= bot) {
                day = 1 + ((weekDay - bot) / 7);
            } else {
                i--;
                weekDay += isLeap(i) ? 366 : 365;
                z2 = false;
            }
        }
        if (currentChar == 'V') {
            Formatter formatter = this.numberFormatter;
            String format = getFormat(modifier, "%02d", "%2d", "%d", "%02d");
            Object[] objArr = new Object[1];
            z = false;
            objArr[0] = Integer.valueOf(day);
            formatter.format(format, objArr);
        } else {
            z = z2;
            if (currentChar == 'g') {
                outputYear(i, z, true, modifier);
            } else {
                outputYear(i, true, true, modifier);
            }
        }
        return z;
    }

    private void modifyAndAppend(CharSequence str, int modifier) {
        int i = 0;
        int i2;
        if (modifier == -1) {
            while (true) {
                i2 = i;
                if (i2 < str.length()) {
                    this.outputBuilder.append(brokenToLower(str.charAt(i2)));
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        } else if (modifier == 35) {
            while (true) {
                i2 = i;
                if (i2 < str.length()) {
                    char c = str.charAt(i2);
                    if (brokenIsUpper(c)) {
                        c = brokenToLower(c);
                    } else if (brokenIsLower(c)) {
                        c = brokenToUpper(c);
                    }
                    this.outputBuilder.append(c);
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        } else if (modifier != 94) {
            this.outputBuilder.append(str);
        } else {
            while (true) {
                i2 = i;
                if (i2 < str.length()) {
                    this.outputBuilder.append(brokenToUpper(str.charAt(i2)));
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private void outputYear(int value, boolean outputTop, boolean outputBottom, int modifier) {
        int trail = value % 100;
        int lead = (value / 100) + (trail / 100);
        trail %= 100;
        if (trail < 0 && lead > 0) {
            trail += 100;
            lead--;
        } else if (lead < 0 && trail > 0) {
            trail -= 100;
            lead++;
        }
        if (outputTop) {
            if (lead != 0 || trail >= 0) {
                this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(lead)});
            } else {
                this.outputBuilder.append("-0");
            }
        }
        if (outputBottom) {
            int n = trail < 0 ? -trail : trail;
            this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), new Object[]{Integer.valueOf(n)});
        }
    }

    private static String getFormat(int modifier, String normal, String underscore, String dash, String zero) {
        if (modifier == 45) {
            return dash;
        }
        if (modifier == 48) {
            return zero;
        }
        if (modifier != 95) {
            return normal;
        }
        return underscore;
    }

    private static boolean isLeap(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    private static boolean brokenIsUpper(char toCheck) {
        return toCheck >= DateFormat.CAPITAL_AM_PM && toCheck <= 'Z';
    }

    private static boolean brokenIsLower(char toCheck) {
        return toCheck >= DateFormat.AM_PM && toCheck <= DateFormat.TIME_ZONE;
    }

    private static char brokenToLower(char input) {
        if (input < DateFormat.CAPITAL_AM_PM || input > 'Z') {
            return input;
        }
        return (char) ((input - 65) + 97);
    }

    private static char brokenToUpper(char input) {
        if (input < DateFormat.AM_PM || input > DateFormat.TIME_ZONE) {
            return input;
        }
        return (char) ((input - 97) + 65);
    }
}
