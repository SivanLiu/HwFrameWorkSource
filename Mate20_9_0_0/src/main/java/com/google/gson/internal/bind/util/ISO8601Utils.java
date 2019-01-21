package com.google.gson.internal.bind.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class ISO8601Utils {
    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone(UTC_ID);
    private static final String UTC_ID = "UTC";

    public static String format(Date date) {
        return format(date, false, TIMEZONE_UTC);
    }

    public static String format(Date date, boolean millis) {
        return format(date, millis, TIMEZONE_UTC);
    }

    public static String format(Date date, boolean millis, TimeZone tz) {
        Calendar calendar = new GregorianCalendar(tz, Locale.US);
        calendar.setTime(date);
        StringBuilder formatted = new StringBuilder(("yyyy-MM-ddThh:mm:ss".length() + (millis ? ".sss".length() : 0)) + (tz.getRawOffset() == 0 ? "Z" : "+hh:mm").length());
        padInt(formatted, calendar.get(1), "yyyy".length());
        char c = '-';
        formatted.append('-');
        padInt(formatted, calendar.get(2) + 1, "MM".length());
        formatted.append('-');
        padInt(formatted, calendar.get(5), "dd".length());
        formatted.append('T');
        padInt(formatted, calendar.get(11), "hh".length());
        formatted.append(':');
        padInt(formatted, calendar.get(12), "mm".length());
        formatted.append(':');
        padInt(formatted, calendar.get(13), "ss".length());
        if (millis) {
            formatted.append('.');
            padInt(formatted, calendar.get(14), "sss".length());
        }
        int offset = tz.getOffset(calendar.getTimeInMillis());
        if (offset != 0) {
            int hours = Math.abs((offset / 60000) / 60);
            int minutes = Math.abs((offset / 60000) % 60);
            if (offset >= 0) {
                c = '+';
            }
            formatted.append(c);
            padInt(formatted, hours, "hh".length());
            formatted.append(':');
            padInt(formatted, minutes, "mm".length());
        } else {
            formatted.append('Z');
        }
        return formatted.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static Date parse(String date, ParsePosition pos) throws ParseException {
        Exception fail;
        Exception exception;
        String str;
        String input;
        String msg;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        ParseException ex;
        String str2 = date;
        ParsePosition parsePosition = pos;
        Exception fail2 = null;
        try {
            int year = pos.getIndex();
            int month = year + 4;
            year = parseInt(str2, year, month);
            if (checkOffset(str2, month, '-')) {
                month++;
            }
            int day = month + 2;
            month = parseInt(str2, month, day);
            if (checkOffset(str2, day, '-')) {
                day++;
            }
            int offset = day + 2;
            day = parseInt(str2, day, offset);
            int hour = 0;
            int minutes = 0;
            int seconds = 0;
            int milliseconds = 0;
            boolean hasT = checkOffset(str2, offset, true);
            if (!hasT) {
                try {
                    if (date.length() <= offset) {
                        Calendar calendar = new GregorianCalendar(year, month - 1, day);
                        parsePosition.setIndex(offset);
                        return calendar.getTime();
                    }
                } catch (IndexOutOfBoundsException e) {
                    fail = e;
                    exception = fail2;
                    if (str2 == null) {
                        str = null;
                    } else {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append('\"');
                        stringBuilder3.append(str2);
                        stringBuilder3.append("'");
                        str = stringBuilder3.toString();
                    }
                    input = str;
                    msg = fail.getMessage();
                    if (msg == null || msg.isEmpty()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("(");
                        stringBuilder.append(fail.getClass().getName());
                        stringBuilder.append(")");
                        msg = stringBuilder.toString();
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to parse date [");
                    stringBuilder2.append(input);
                    stringBuilder2.append("]: ");
                    stringBuilder2.append(msg);
                    ex = new ParseException(stringBuilder2.toString(), pos.getIndex());
                    ex.initCause(fail);
                    throw ex;
                } catch (NumberFormatException e2) {
                    fail = e2;
                    exception = fail2;
                    if (str2 == null) {
                    }
                    input = str;
                    msg = fail.getMessage();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("(");
                    stringBuilder.append(fail.getClass().getName());
                    stringBuilder.append(")");
                    msg = stringBuilder.toString();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to parse date [");
                    stringBuilder2.append(input);
                    stringBuilder2.append("]: ");
                    stringBuilder2.append(msg);
                    ex = new ParseException(stringBuilder2.toString(), pos.getIndex());
                    ex.initCause(fail);
                    throw ex;
                } catch (IllegalArgumentException e3) {
                    fail = e3;
                    exception = fail2;
                    if (str2 == null) {
                    }
                    input = str;
                    msg = fail.getMessage();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("(");
                    stringBuilder.append(fail.getClass().getName());
                    stringBuilder.append(")");
                    msg = stringBuilder.toString();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to parse date [");
                    stringBuilder2.append(input);
                    stringBuilder2.append("]: ");
                    stringBuilder2.append(msg);
                    ex = new ParseException(stringBuilder2.toString(), pos.getIndex());
                    ex.initCause(fail);
                    throw ex;
                }
            }
            if (hasT) {
                offset++;
                int offset2 = offset + 2;
                hour = parseInt(str2, offset, offset2);
                if (checkOffset(str2, offset2, ':')) {
                    offset2++;
                }
                int offset3 = offset2 + 2;
                minutes = parseInt(str2, offset2, offset3);
                if (checkOffset(str2, offset3, ':')) {
                    offset3++;
                }
                offset = offset3;
                if (date.length() > offset) {
                    char c = str2.charAt(offset);
                    if (!(c == 'Z' || c == '+' || c == '-')) {
                        offset3 = offset + 2;
                        offset = parseInt(str2, offset, offset3);
                        if (offset > 59 && offset < 63) {
                            offset = 59;
                        }
                        seconds = offset;
                        if (checkOffset(str2, offset3, '.')) {
                            offset3++;
                            offset = indexOfNonDigit(str2, offset3 + 1);
                            int parseEndOffset = Math.min(offset, offset3 + 3);
                            int fraction = parseInt(str2, offset3, parseEndOffset);
                            switch (parseEndOffset - offset3) {
                                case 1:
                                    milliseconds = fraction * 100;
                                    break;
                                case 2:
                                    milliseconds = fraction * 10;
                                    break;
                                default:
                                    milliseconds = fraction;
                                    break;
                            }
                        }
                        offset = offset3;
                    }
                }
            }
            if (date.length() > offset) {
                TimeZone timezone;
                char timezoneIndicator = str2.charAt(offset);
                char c2;
                if (timezoneIndicator == 'Z') {
                    timezone = TIMEZONE_UTC;
                    offset++;
                    exception = fail2;
                    c2 = timezoneIndicator;
                } else {
                    String str3;
                    TimeZone timezone2;
                    if (timezoneIndicator == '+') {
                        timezone2 = null;
                    } else if (timezoneIndicator == '-') {
                        timezone2 = null;
                    } else {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        timezone2 = null;
                        stringBuilder4.append("Invalid time zone indicator '");
                        stringBuilder4.append(timezoneIndicator);
                        stringBuilder4.append("'");
                        throw new IndexOutOfBoundsException(stringBuilder4.toString());
                    }
                    input = str2.substring(offset);
                    if (input.length() >= 5) {
                        str3 = input;
                    } else {
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append(input);
                        stringBuilder5.append("00");
                        str3 = stringBuilder5.toString();
                    }
                    input = str3;
                    offset += input.length();
                    String str4;
                    if ("+0000".equals(input)) {
                        str4 = input;
                        exception = fail2;
                        c2 = timezoneIndicator;
                    } else if ("+00:00".equals(input)) {
                        str4 = input;
                        exception = fail2;
                        c2 = timezoneIndicator;
                    } else {
                        str3 = new StringBuilder();
                        str3.append("GMT");
                        str3.append(input);
                        str3 = str3.toString();
                        TimeZone timezone3 = TimeZone.getTimeZone(str3);
                        input = timezone3.getID();
                        if (input.equals(str3)) {
                            c2 = timezoneIndicator;
                        } else {
                            try {
                                fail2 = input.replace(":", "");
                                if (!fail2.equals(str3)) {
                                    String act = input;
                                    input = new StringBuilder();
                                    Exception cleaned = fail2;
                                    input.append("Mismatching time zone indicator: ");
                                    input.append(str3);
                                    input.append(" given, resolves to ");
                                    input.append(timezone3.getID());
                                    throw new IndexOutOfBoundsException(input.toString());
                                }
                            } catch (IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException e4) {
                                fail = e4;
                                if (str2 == null) {
                                }
                                input = str;
                                msg = fail.getMessage();
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("(");
                                stringBuilder.append(fail.getClass().getName());
                                stringBuilder.append(")");
                                msg = stringBuilder.toString();
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Failed to parse date [");
                                stringBuilder2.append(input);
                                stringBuilder2.append("]: ");
                                stringBuilder2.append(msg);
                                ex = new ParseException(stringBuilder2.toString(), pos.getIndex());
                                ex.initCause(fail);
                                throw ex;
                            }
                        }
                        timezone = timezone3;
                    }
                    timezone = TIMEZONE_UTC;
                }
                fail2 = new GregorianCalendar(timezone);
                fail2.setLenient(false);
                fail2.set(1, year);
                fail2.set(2, month - 1);
                fail2.set(5, day);
                fail2.set(11, hour);
                fail2.set(12, minutes);
                fail2.set(13, seconds);
                fail2.set(14, milliseconds);
                parsePosition.setIndex(offset);
                return fail2.getTime();
            }
            throw new IllegalArgumentException("No time zone indicator");
        } catch (IndexOutOfBoundsException e5) {
            fail = e5;
            exception = fail2;
            if (str2 == null) {
            }
            input = str;
            msg = fail.getMessage();
            stringBuilder = new StringBuilder();
            stringBuilder.append("(");
            stringBuilder.append(fail.getClass().getName());
            stringBuilder.append(")");
            msg = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to parse date [");
            stringBuilder2.append(input);
            stringBuilder2.append("]: ");
            stringBuilder2.append(msg);
            ex = new ParseException(stringBuilder2.toString(), pos.getIndex());
            ex.initCause(fail);
            throw ex;
        } catch (NumberFormatException e6) {
            fail = e6;
            exception = fail2;
            if (str2 == null) {
            }
            input = str;
            msg = fail.getMessage();
            stringBuilder = new StringBuilder();
            stringBuilder.append("(");
            stringBuilder.append(fail.getClass().getName());
            stringBuilder.append(")");
            msg = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to parse date [");
            stringBuilder2.append(input);
            stringBuilder2.append("]: ");
            stringBuilder2.append(msg);
            ex = new ParseException(stringBuilder2.toString(), pos.getIndex());
            ex.initCause(fail);
            throw ex;
        } catch (IllegalArgumentException e7) {
            fail = e7;
            exception = fail2;
            if (str2 == null) {
            }
            input = str;
            msg = fail.getMessage();
            stringBuilder = new StringBuilder();
            stringBuilder.append("(");
            stringBuilder.append(fail.getClass().getName());
            stringBuilder.append(")");
            msg = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to parse date [");
            stringBuilder2.append(input);
            stringBuilder2.append("]: ");
            stringBuilder2.append(msg);
            ex = new ParseException(stringBuilder2.toString(), pos.getIndex());
            ex.initCause(fail);
            throw ex;
        }
    }

    private static boolean checkOffset(String value, int offset, char expected) {
        return offset < value.length() && value.charAt(offset) == expected;
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x003c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int parseInt(String value, int beginIndex, int endIndex) throws NumberFormatException {
        if (beginIndex < 0 || endIndex > value.length() || beginIndex > endIndex) {
            throw new NumberFormatException(value);
        }
        int i;
        StringBuilder stringBuilder;
        int i2 = beginIndex;
        int result = 0;
        if (i2 < endIndex) {
            i = i2 + 1;
            i2 = Character.digit(value.charAt(i2), 10);
            if (i2 >= 0) {
                result = -i2;
                i2 = i;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid number: ");
                stringBuilder.append(value.substring(beginIndex, endIndex));
                throw new NumberFormatException(stringBuilder.toString());
            }
        }
        if (i2 >= endIndex) {
            i = i2 + 1;
            i2 = Character.digit(value.charAt(i2), 10);
            if (i2 >= 0) {
                result = (result * 10) - i2;
                i2 = i;
                if (i2 >= endIndex) {
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid number: ");
            stringBuilder.append(value.substring(beginIndex, endIndex));
            throw new NumberFormatException(stringBuilder.toString());
        }
        return -result;
    }

    private static void padInt(StringBuilder buffer, int value, int length) {
        String strValue = Integer.toString(value);
        for (int i = length - strValue.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(strValue);
    }

    private static int indexOfNonDigit(String string, int offset) {
        for (int i = offset; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return i;
            }
        }
        return string.length();
    }
}
