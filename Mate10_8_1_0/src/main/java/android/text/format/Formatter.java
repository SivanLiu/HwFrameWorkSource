package android.text.format;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.DecimalFormat;
import android.icu.text.MeasureFormat;
import android.icu.text.MeasureFormat.FormatWidth;
import android.icu.text.NumberFormat;
import android.icu.text.UnicodeSet;
import android.icu.text.UnicodeSetSpanner;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.net.NetworkUtils;
import android.text.BidiFormatter;
import android.text.TextUtils;
import com.android.internal.R;
import java.lang.reflect.Constructor;
import java.util.Locale;

public final class Formatter {
    public static final int FLAG_CALCULATE_ROUNDED = 2;
    public static final int FLAG_DEFAULT = 0;
    public static final int FLAG_SHORTER = 1;
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final MeasureUnit PETABYTE = createPetaByte();
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final UnicodeSetSpanner SPACES_AND_CONTROLS = new UnicodeSetSpanner(new UnicodeSet("[[:Zs:][:Cf:]]").freeze());

    public static class BytesResult {
        public final long roundedBytes;
        public final String units;
        public final String value;

        public BytesResult(String value, String units, long roundedBytes) {
            this.value = value;
            this.units = units;
            this.roundedBytes = roundedBytes;
        }
    }

    private static class RoundedBytesResult {
        public final int fractionDigits;
        public final long roundedBytes;
        public final MeasureUnit units;
        public final float value;

        private RoundedBytesResult(float value, MeasureUnit units, int fractionDigits, long roundedBytes) {
            this.value = value;
            this.units = units;
            this.fractionDigits = fractionDigits;
            this.roundedBytes = roundedBytes;
        }

        static RoundedBytesResult roundBytes(long sizeBytes, int flags) {
            int roundFactor;
            int roundDigits;
            long roundedBytes;
            boolean isNegative = sizeBytes < 0;
            if (isNegative) {
                sizeBytes = -sizeBytes;
            }
            float result = (float) sizeBytes;
            MeasureUnit units = MeasureUnit.BYTE;
            long mult = 1;
            if (result > 900.0f) {
                units = MeasureUnit.KILOBYTE;
                mult = 1000;
                result /= 1000.0f;
            }
            if (result > 900.0f) {
                units = MeasureUnit.MEGABYTE;
                mult *= 1000;
                result /= 1000.0f;
            }
            if (result > 900.0f) {
                units = MeasureUnit.GIGABYTE;
                mult *= 1000;
                result /= 1000.0f;
            }
            if (result > 900.0f) {
                units = MeasureUnit.TERABYTE;
                mult *= 1000;
                result /= 1000.0f;
            }
            if (result > 900.0f) {
                units = Formatter.PETABYTE;
                mult *= 1000;
                result /= 1000.0f;
            }
            if (mult == 1 || result >= 100.0f) {
                roundFactor = 1;
                roundDigits = 0;
            } else if (result < 1.0f) {
                roundFactor = 100;
                roundDigits = 2;
            } else if (result < 10.0f) {
                if ((flags & 1) != 0) {
                    roundFactor = 10;
                    roundDigits = 1;
                } else {
                    roundFactor = 100;
                    roundDigits = 2;
                }
            } else if ((flags & 1) != 0) {
                roundFactor = 1;
                roundDigits = 0;
            } else {
                roundFactor = 100;
                roundDigits = 2;
            }
            if (isNegative) {
                result = -result;
            }
            if ((flags & 2) == 0) {
                roundedBytes = 0;
            } else {
                roundedBytes = (((long) Math.round(((float) roundFactor) * result)) * mult) / ((long) roundFactor);
            }
            return new RoundedBytesResult(result, units, roundDigits, roundedBytes);
        }
    }

    private static Locale localeFromContext(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    private static String bidiWrap(Context context, String source) {
        if (TextUtils.getLayoutDirectionFromLocale(localeFromContext(context)) == 1) {
            return BidiFormatter.getInstance(true).unicodeWrap(source);
        }
        return source;
    }

    public static String formatFileSize(Context context, long sizeBytes) {
        return formatFileSize(context, sizeBytes, 0);
    }

    public static String formatShortFileSize(Context context, long sizeBytes) {
        return formatFileSize(context, sizeBytes, 1);
    }

    private static String formatFileSize(Context context, long sizeBytes, int flags) {
        if (context == null) {
            return "";
        }
        return bidiWrap(context, formatRoundedBytesResult(context, RoundedBytesResult.roundBytes(sizeBytes, flags)));
    }

    private static String getSuffixOverride(Resources res, MeasureUnit unit) {
        if (unit == MeasureUnit.BYTE) {
            return res.getString(R.string.byteShort);
        }
        return res.getString(R.string.petabyteShort);
    }

    private static NumberFormat getNumberFormatter(Locale locale, int fractionDigits) {
        NumberFormat numberFormatter = NumberFormat.getInstance(locale);
        numberFormatter.setMinimumFractionDigits(fractionDigits);
        numberFormatter.setMaximumFractionDigits(fractionDigits);
        numberFormatter.setGroupingUsed(false);
        if (numberFormatter instanceof DecimalFormat) {
            numberFormatter.setRoundingMode(4);
        }
        return numberFormatter;
    }

    private static String deleteFirstFromString(String source, String toDelete) {
        int location = source.indexOf(toDelete);
        if (location == -1) {
            return source;
        }
        return source.substring(0, location) + source.substring(toDelete.length() + location, source.length());
    }

    private static String formatMeasureShort(Locale locale, NumberFormat numberFormatter, float value, MeasureUnit units) {
        return MeasureFormat.getInstance(locale, FormatWidth.SHORT, numberFormatter).format(new Measure(Float.valueOf(value), units));
    }

    private static String formatRoundedBytesResult(Context context, RoundedBytesResult input) {
        Locale locale = localeFromContext(context);
        NumberFormat numberFormatter = getNumberFormatter(locale, input.fractionDigits);
        if (input.units != MeasureUnit.BYTE && input.units != PETABYTE) {
            return formatMeasureShort(locale, numberFormatter, input.value, input.units);
        }
        return context.getString(R.string.fileSizeSuffix, new Object[]{numberFormatter.format((double) input.value), getSuffixOverride(context.getResources(), input.units)});
    }

    public static BytesResult formatBytes(Resources res, long sizeBytes, int flags) {
        String units;
        RoundedBytesResult rounded = RoundedBytesResult.roundBytes(sizeBytes, flags);
        Locale locale = res.getConfiguration().getLocales().get(0);
        NumberFormat numberFormatter = getNumberFormatter(locale, rounded.fractionDigits);
        String formattedNumber = numberFormatter.format((double) rounded.value);
        if (rounded.units == MeasureUnit.BYTE || rounded.units == PETABYTE) {
            units = getSuffixOverride(res, rounded.units);
        } else {
            units = SPACES_AND_CONTROLS.trim(deleteFirstFromString(formatMeasureShort(locale, numberFormatter, rounded.value, rounded.units), formattedNumber)).toString();
        }
        return new BytesResult(formattedNumber, units, rounded.roundedBytes);
    }

    private static MeasureUnit createPetaByte() {
        try {
            Constructor<MeasureUnit> constructor = MeasureUnit.class.getDeclaredConstructor(new Class[]{String.class, String.class});
            constructor.setAccessible(true);
            return (MeasureUnit) constructor.newInstance(new Object[]{"digital", "petabyte"});
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create petabyte MeasureUnit", e);
        }
    }

    @Deprecated
    public static String formatIpAddress(int ipv4Address) {
        return NetworkUtils.intToInetAddress(ipv4Address).getHostAddress();
    }

    public static String formatShortElapsedTime(Context context, long millis) {
        long secondsLong = millis / 1000;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if (secondsLong >= 86400) {
            days = (int) (secondsLong / 86400);
            secondsLong -= (long) (SECONDS_PER_DAY * days);
        }
        if (secondsLong >= 3600) {
            hours = (int) (secondsLong / 3600);
            secondsLong -= (long) (hours * SECONDS_PER_HOUR);
        }
        if (secondsLong >= 60) {
            minutes = (int) (secondsLong / 60);
            secondsLong -= (long) (minutes * 60);
        }
        int seconds = (int) secondsLong;
        MeasureFormat measureFormat = MeasureFormat.getInstance(localeFromContext(context), FormatWidth.SHORT);
        if (days >= 2) {
            return measureFormat.format(new Measure(Integer.valueOf(days + ((hours + 12) / 24)), MeasureUnit.DAY));
        }
        if (days > 0) {
            return measureFormat.formatMeasures(new Measure[]{new Measure(Integer.valueOf(days), MeasureUnit.DAY), new Measure(Integer.valueOf(hours), MeasureUnit.HOUR)});
        } else if (hours >= 2) {
            return measureFormat.format(new Measure(Integer.valueOf(hours + ((minutes + 30) / 60)), MeasureUnit.HOUR));
        } else {
            if (hours > 0) {
                return measureFormat.formatMeasures(new Measure[]{new Measure(Integer.valueOf(hours), MeasureUnit.HOUR), new Measure(Integer.valueOf(minutes), MeasureUnit.MINUTE)});
            } else if (minutes >= 2) {
                return measureFormat.format(new Measure(Integer.valueOf(minutes + ((seconds + 30) / 60)), MeasureUnit.MINUTE));
            } else {
                if (minutes <= 0) {
                    return measureFormat.format(new Measure(Integer.valueOf(seconds), MeasureUnit.SECOND));
                }
                return measureFormat.formatMeasures(new Measure[]{new Measure(Integer.valueOf(minutes), MeasureUnit.MINUTE), new Measure(Integer.valueOf(seconds), MeasureUnit.SECOND)});
            }
        }
    }

    public static String formatShortElapsedTimeRoundingUpToMinutes(Context context, long millis) {
        long minutesRoundedUp = ((millis + DateUtils.MINUTE_IN_MILLIS) - 1) / DateUtils.MINUTE_IN_MILLIS;
        if (minutesRoundedUp == 0 || minutesRoundedUp == 1) {
            return MeasureFormat.getInstance(localeFromContext(context), FormatWidth.SHORT).format(new Measure(Long.valueOf(minutesRoundedUp), MeasureUnit.MINUTE));
        }
        return formatShortElapsedTime(context, minutesRoundedUp * DateUtils.MINUTE_IN_MILLIS);
    }
}
