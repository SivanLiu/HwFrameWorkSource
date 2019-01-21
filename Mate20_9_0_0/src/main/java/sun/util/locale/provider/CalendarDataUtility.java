package sun.util.locale.provider;

import android.icu.text.DateFormatSymbols;
import android.icu.util.ULocale;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CalendarDataUtility {
    private static final String BUDDHIST_CALENDAR = "buddhist";
    private static final String GREGORIAN_CALENDAR = "gregorian";
    private static final String ISLAMIC_CALENDAR = "islamic";
    private static final String JAPANESE_CALENDAR = "japanese";
    private static int[] REST_OF_STYLES = new int[]{Calendar.SHORT_STANDALONE, 2, Calendar.LONG_STANDALONE, 4, Calendar.NARROW_STANDALONE};

    private CalendarDataUtility() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:19:0x0040  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003d  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0040  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003d  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0040  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String retrieveFieldValueName(String id, int field, int value, int style, Locale locale) {
        if (field == 0) {
            Object obj;
            String normalizeCalendarType = normalizeCalendarType(id);
            int hashCode = normalizeCalendarType.hashCode();
            if (hashCode == -1581060683) {
                if (normalizeCalendarType.equals(BUDDHIST_CALENDAR)) {
                    obj = null;
                    switch (obj) {
                        case null:
                        case 1:
                            break;
                        case 2:
                            break;
                    }
                }
            } else if (hashCode == -752730191) {
                if (normalizeCalendarType.equals(JAPANESE_CALENDAR)) {
                    obj = 2;
                    switch (obj) {
                        case null:
                        case 1:
                            break;
                        case 2:
                            break;
                    }
                }
            } else if (hashCode == 2093696456 && normalizeCalendarType.equals(ISLAMIC_CALENDAR)) {
                obj = 1;
                switch (obj) {
                    case null:
                    case 1:
                        value--;
                        break;
                    case 2:
                        value += 231;
                        break;
                }
            }
            obj = -1;
            switch (obj) {
                case null:
                case 1:
                    break;
                case 2:
                    break;
            }
        }
        if (value < 0) {
            return null;
        }
        String[] names = getNames(id, field, style, locale);
        if (value >= names.length) {
            return null;
        }
        return names[value];
    }

    public static String retrieveJavaTimeFieldValueName(String id, int field, int value, int style, Locale locale) {
        return retrieveFieldValueName(id, field, value, style, locale);
    }

    public static Map<String, Integer> retrieveFieldValueNames(String id, int field, int style, Locale locale) {
        Map<String, Integer> names;
        if (style == 0) {
            names = retrieveFieldValueNamesImpl(id, field, 1, locale);
            for (int st : REST_OF_STYLES) {
                names.putAll(retrieveFieldValueNamesImpl(id, field, st, locale));
            }
        } else {
            names = retrieveFieldValueNamesImpl(id, field, style, locale);
        }
        return names.isEmpty() ? null : names;
    }

    public static Map<String, Integer> retrieveJavaTimeFieldValueNames(String id, int field, int style, Locale locale) {
        return retrieveFieldValueNames(id, field, style, locale);
    }

    private static String normalizeCalendarType(String requestID) {
        if (requestID.equals("gregory") || requestID.equals("iso8601")) {
            return GREGORIAN_CALENDAR;
        }
        if (requestID.startsWith(ISLAMIC_CALENDAR)) {
            return ISLAMIC_CALENDAR;
        }
        return requestID;
    }

    private static Map<String, Integer> retrieveFieldValueNamesImpl(String id, int field, int style, Locale locale) {
        String[] names = getNames(id, field, style, locale);
        int skipped = 0;
        int offset = 0;
        if (field == 0) {
            String normalizeCalendarType = normalizeCalendarType(id);
            Object obj = -1;
            int hashCode = normalizeCalendarType.hashCode();
            if (hashCode != -1581060683) {
                if (hashCode != -752730191) {
                    if (hashCode == 2093696456 && normalizeCalendarType.equals(ISLAMIC_CALENDAR)) {
                        obj = 1;
                    }
                } else if (normalizeCalendarType.equals(JAPANESE_CALENDAR)) {
                    obj = 2;
                }
            } else if (normalizeCalendarType.equals(BUDDHIST_CALENDAR)) {
                obj = null;
            }
            switch (obj) {
                case null:
                case 1:
                    offset = 1;
                    break;
                case 2:
                    skipped = 232;
                    offset = -231;
                    break;
            }
        }
        Map<String, Integer> result = new LinkedHashMap();
        int i = skipped;
        while (i < names.length) {
            if (!names[i].isEmpty() && result.put(names[i], Integer.valueOf(i + offset)) != null) {
                return new LinkedHashMap();
            }
            i++;
        }
        return result;
    }

    private static String[] getNames(String id, int field, int style, Locale locale) {
        int context = toContext(style);
        int width = toWidth(style);
        DateFormatSymbols symbols = getDateFormatSymbols(id, locale);
        StringBuilder stringBuilder;
        if (field == 0) {
            switch (width) {
                case 0:
                    return symbols.getEras();
                case 1:
                    return symbols.getEraNames();
                case 2:
                    return symbols.getNarrowEras();
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown width: ");
                    stringBuilder.append(width);
                    throw new UnsupportedOperationException(stringBuilder.toString());
            }
        } else if (field == 2) {
            return symbols.getMonths(context, width);
        } else {
            if (field == 7) {
                return symbols.getWeekdays(context, width);
            }
            if (field == 9) {
                return symbols.getAmPmStrings();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown field: ");
            stringBuilder.append(field);
            throw new UnsupportedOperationException(stringBuilder.toString());
        }
    }

    private static DateFormatSymbols getDateFormatSymbols(String id, Locale locale) {
        return new DateFormatSymbols(ULocale.forLocale(locale), normalizeCalendarType(id));
    }

    private static int toWidth(int style) {
        switch (style) {
            case 1:
            case Calendar.SHORT_STANDALONE /*32769*/:
                return 0;
            case 2:
            case Calendar.LONG_STANDALONE /*32770*/:
                return 1;
            case 4:
            case Calendar.NARROW_STANDALONE /*32772*/:
                return 2;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid style: ");
                stringBuilder.append(style);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int toContext(int style) {
        switch (style) {
            case 1:
            case 2:
            case 4:
                return 0;
            case Calendar.SHORT_STANDALONE /*32769*/:
            case Calendar.LONG_STANDALONE /*32770*/:
            case Calendar.NARROW_STANDALONE /*32772*/:
                return 1;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid style: ");
                stringBuilder.append(style);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }
}
