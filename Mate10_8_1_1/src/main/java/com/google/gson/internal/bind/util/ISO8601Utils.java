package com.google.gson.internal.bind.util;

import com.android.server.rms.iaware.appmng.AwareAppAssociate;
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
        int i = 0;
        Calendar calendar = new GregorianCalendar(tz, Locale.US);
        calendar.setTime(date);
        int capacity = "yyyy-MM-ddThh:mm:ss".length();
        if (millis) {
            i = ".sss".length();
        }
        capacity += i;
        if (tz.getRawOffset() != 0) {
            i = "+hh:mm".length();
        } else {
            i = "Z".length();
        }
        StringBuilder formatted = new StringBuilder(capacity + i);
        padInt(formatted, calendar.get(1), "yyyy".length());
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
        if (offset == 0) {
            formatted.append('Z');
        } else {
            char c;
            int hours = Math.abs((offset / AwareAppAssociate.ASSOC_REPORT_MIN_TIME) / 60);
            int minutes = Math.abs((offset / AwareAppAssociate.ASSOC_REPORT_MIN_TIME) % 60);
            if (offset >= 0) {
                c = '+';
            } else {
                c = '-';
            }
            formatted.append(c);
            padInt(formatted, hours, "hh".length());
            formatted.append(':');
            padInt(formatted, minutes, "mm".length());
        }
        return formatted.toString();
    }

    public static Date parse(String date, ParsePosition pos) throws ParseException {
        Exception fail;
        String input;
        String msg;
        ParseException ex;
        try {
            int index = pos.getIndex();
            int offset = index + 4;
            int year = parseInt(date, index, offset);
            if (checkOffset(date, offset, '-')) {
                offset++;
            }
            index = offset + 2;
            int month = parseInt(date, offset, index);
            if (checkOffset(date, index, '-')) {
                offset = index + 1;
            } else {
                offset = index;
            }
            index = offset + 2;
            int day = parseInt(date, offset, index);
            int hour = 0;
            int minutes = 0;
            int seconds = 0;
            int milliseconds = 0;
            boolean hasT = checkOffset(date, index, 'T');
            Calendar calendar;
            if (!hasT && date.length() <= index) {
                calendar = new GregorianCalendar(year, month - 1, day);
                pos.setIndex(index);
                return calendar.getTime();
            }
            if (hasT) {
                index++;
                offset = index + 2;
                hour = parseInt(date, index, offset);
                if (checkOffset(date, offset, ':')) {
                    offset++;
                }
                index = offset + 2;
                minutes = parseInt(date, offset, index);
                if (checkOffset(date, index, ':')) {
                    offset = index + 1;
                } else {
                    offset = index;
                }
                if (date.length() <= offset) {
                    index = offset;
                } else {
                    char c = date.charAt(offset);
                    if (c == 'Z' || c == '+' || c == '-') {
                        index = offset;
                    } else {
                        index = offset + 2;
                        seconds = parseInt(date, offset, index);
                        if (seconds > 59 && seconds < 63) {
                            seconds = 59;
                        }
                        if (checkOffset(date, index, '.')) {
                            index++;
                            int endOffset = indexOfNonDigit(date, index + 1);
                            int parseEndOffset = Math.min(endOffset, index + 3);
                            int fraction = parseInt(date, index, parseEndOffset);
                            switch (parseEndOffset - index) {
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
                            index = endOffset;
                        }
                    }
                }
            }
            if (date.length() > index) {
                TimeZone timezone;
                char timezoneIndicator = date.charAt(index);
                if (timezoneIndicator == 'Z') {
                    timezone = TIMEZONE_UTC;
                    index++;
                } else if (timezoneIndicator == '+' || timezoneIndicator == '-') {
                    String timezoneOffset = date.substring(index);
                    if (timezoneOffset.length() < 5) {
                        timezoneOffset = timezoneOffset + "00";
                    }
                    index += timezoneOffset.length();
                    if ("+0000".equals(timezoneOffset) || "+00:00".equals(timezoneOffset)) {
                        timezone = TIMEZONE_UTC;
                    } else {
                        String timezoneId = "GMT" + timezoneOffset;
                        timezone = TimeZone.getTimeZone(timezoneId);
                        String act = timezone.getID();
                        if (!(act.equals(timezoneId) || act.replace(":", "").equals(timezoneId))) {
                            throw new IndexOutOfBoundsException("Mismatching time zone indicator: " + timezoneId + " given, resolves to " + timezone.getID());
                        }
                    }
                } else {
                    throw new IndexOutOfBoundsException("Invalid time zone indicator '" + timezoneIndicator + "'");
                }
                calendar = new GregorianCalendar(timezone);
                calendar.setLenient(false);
                calendar.set(1, year);
                calendar.set(2, month - 1);
                calendar.set(5, day);
                calendar.set(11, hour);
                calendar.set(12, minutes);
                calendar.set(13, seconds);
                calendar.set(14, milliseconds);
                pos.setIndex(index);
                return calendar.getTime();
            }
            throw new IllegalArgumentException("No time zone indicator");
        } catch (Exception e) {
            fail = e;
            input = date != null ? '\"' + date + "'" : null;
            msg = fail.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = "(" + fail.getClass().getName() + ")";
            }
            ex = new ParseException("Failed to parse date [" + input + "]: " + msg, pos.getIndex());
            ex.initCause(fail);
            throw ex;
        } catch (Exception e2) {
            fail = e2;
            if (date != null) {
            }
            msg = fail.getMessage();
            if (msg == null) {
                ex = new ParseException("Failed to parse date [" + input + "]: " + msg, pos.getIndex());
                ex.initCause(fail);
                throw ex;
            }
            msg = "(" + fail.getClass().getName() + ")";
            ex = new ParseException("Failed to parse date [" + input + "]: " + msg, pos.getIndex());
            ex.initCause(fail);
            throw ex;
        } catch (Exception e3) {
            fail = e3;
            if (date != null) {
            }
            msg = fail.getMessage();
            if (msg == null) {
                ex = new ParseException("Failed to parse date [" + input + "]: " + msg, pos.getIndex());
                ex.initCause(fail);
                throw ex;
            }
            msg = "(" + fail.getClass().getName() + ")";
            ex = new ParseException("Failed to parse date [" + input + "]: " + msg, pos.getIndex());
            ex.initCause(fail);
            throw ex;
        }
    }

    private static boolean checkOffset(String value, int offset, char expected) {
        return offset < value.length() && value.charAt(offset) == expected;
    }

    private static int parseInt(String value, int beginIndex, int endIndex) throws NumberFormatException {
        if (beginIndex >= 0 && endIndex <= value.length() && beginIndex <= endIndex) {
            int i;
            int digit;
            int i2 = beginIndex;
            int result = 0;
            if (beginIndex >= endIndex) {
                i = i2;
            } else {
                i2 = beginIndex + 1;
                digit = Character.digit(value.charAt(beginIndex), 10);
                if (digit >= 0) {
                    result = -digit;
                    i = i2;
                } else {
                    throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
                }
            }
            while (i < endIndex) {
                i2 = i + 1;
                digit = Character.digit(value.charAt(i), 10);
                if (digit >= 0) {
                    result = (result * 10) - digit;
                    i = i2;
                } else {
                    throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
                }
            }
            return -result;
        }
        throw new NumberFormatException(value);
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
