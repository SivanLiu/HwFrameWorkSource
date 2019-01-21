package java.time.format;

import android.icu.impl.ICUResourceBundle;
import android.icu.util.UResourceBundle;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalField;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.util.locale.provider.CalendarDataUtility;

class DateTimeTextProvider {
    private static final ConcurrentMap<Entry<TemporalField, Locale>, Object> CACHE = new ConcurrentHashMap(16, 0.75f, 2);
    private static final Comparator<Entry<String, Long>> COMPARATOR = new Comparator<Entry<String, Long>>() {
        public int compare(Entry<String, Long> obj1, Entry<String, Long> obj2) {
            return ((String) obj2.getKey()).length() - ((String) obj1.getKey()).length();
        }
    };

    static final class LocaleStore {
        private final Map<TextStyle, List<Entry<String, Long>>> parsable;
        private final Map<TextStyle, Map<Long, String>> valueTextMap;

        LocaleStore(Map<TextStyle, Map<Long, String>> valueTextMap) {
            this.valueTextMap = valueTextMap;
            Map<TextStyle, List<Entry<String, Long>>> map = new HashMap();
            List<Entry<String, Long>> allList = new ArrayList();
            for (Entry<TextStyle, Map<Long, String>> vtmEntry : valueTextMap.entrySet()) {
                Map<String, Entry<String, Long>> reverse = new HashMap();
                for (Entry<Long, String> entry : ((Map) vtmEntry.getValue()).entrySet()) {
                    if (reverse.put((String) entry.getValue(), DateTimeTextProvider.createEntry((String) entry.getValue(), (Long) entry.getKey())) != null) {
                    }
                }
                List<Entry<String, Long>> list = new ArrayList(reverse.values());
                Collections.sort(list, DateTimeTextProvider.COMPARATOR);
                map.put((TextStyle) vtmEntry.getKey(), list);
                allList.addAll(list);
                map.put(null, allList);
            }
            Collections.sort(allList, DateTimeTextProvider.COMPARATOR);
            this.parsable = map;
        }

        String getText(long value, TextStyle style) {
            Map<Long, String> map = (Map) this.valueTextMap.get(style);
            return map != null ? (String) map.get(Long.valueOf(value)) : null;
        }

        Iterator<Entry<String, Long>> getTextIterator(TextStyle style) {
            List<Entry<String, Long>> list = (List) this.parsable.get(style);
            return list != null ? list.iterator() : null;
        }
    }

    DateTimeTextProvider() {
    }

    static DateTimeTextProvider getInstance() {
        return new DateTimeTextProvider();
    }

    public String getText(TemporalField field, long value, TextStyle style, Locale locale) {
        Object store = findStore(field, locale);
        if (store instanceof LocaleStore) {
            return ((LocaleStore) store).getText(value, style);
        }
        return null;
    }

    public String getText(Chronology chrono, TemporalField field, long value, TextStyle style, Locale locale) {
        if (chrono == IsoChronology.INSTANCE || !(field instanceof ChronoField)) {
            return getText(field, value, style, locale);
        }
        int fieldIndex;
        int fieldValue;
        if (field == ChronoField.ERA) {
            fieldIndex = 0;
            if (chrono != JapaneseChronology.INSTANCE) {
                fieldValue = (int) value;
            } else if (value == -999) {
                fieldValue = 0;
            } else {
                fieldValue = ((int) value) + 2;
            }
        } else if (field == ChronoField.MONTH_OF_YEAR) {
            fieldIndex = 2;
            fieldValue = ((int) value) - 1;
        } else if (field == ChronoField.DAY_OF_WEEK) {
            fieldIndex = 7;
            fieldValue = ((int) value) + 1;
            if (fieldValue > 7) {
                fieldValue = 1;
            }
        } else if (field != ChronoField.AMPM_OF_DAY) {
            return null;
        } else {
            fieldIndex = 9;
            fieldValue = (int) value;
        }
        return CalendarDataUtility.retrieveJavaTimeFieldValueName(chrono.getCalendarType(), fieldIndex, fieldValue, style.toCalendarStyle(), locale);
    }

    public Iterator<Entry<String, Long>> getTextIterator(TemporalField field, TextStyle style, Locale locale) {
        Object store = findStore(field, locale);
        if (store instanceof LocaleStore) {
            return ((LocaleStore) store).getTextIterator(style);
        }
        return null;
    }

    public Iterator<Entry<String, Long>> getTextIterator(Chronology chrono, TemporalField field, TextStyle style, Locale locale) {
        if (chrono == IsoChronology.INSTANCE || !(field instanceof ChronoField)) {
            return getTextIterator(field, style, locale);
        }
        int fieldIndex;
        switch ((ChronoField) field) {
            case ERA:
                fieldIndex = 0;
                break;
            case MONTH_OF_YEAR:
                fieldIndex = 2;
                break;
            case DAY_OF_WEEK:
                fieldIndex = 7;
                break;
            case AMPM_OF_DAY:
                fieldIndex = 9;
                break;
            default:
                return null;
        }
        Map<String, Integer> map = CalendarDataUtility.retrieveJavaTimeFieldValueNames(chrono.getCalendarType(), fieldIndex, style == null ? 0 : style.toCalendarStyle(), locale);
        if (map == null) {
            return null;
        }
        List<Entry<String, Long>> list = new ArrayList(map.size());
        if (fieldIndex == 0) {
            for (Entry<String, Integer> entry : map.entrySet()) {
                int era = ((Integer) entry.getValue()).intValue();
                if (chrono == JapaneseChronology.INSTANCE) {
                    if (era == 0) {
                        era = -999;
                    } else {
                        era -= 2;
                    }
                }
                list.add(createEntry((String) entry.getKey(), Long.valueOf((long) era)));
            }
        } else if (fieldIndex == 2) {
            for (Entry<String, Integer> entry2 : map.entrySet()) {
                list.add(createEntry((String) entry2.getKey(), Long.valueOf((long) (((Integer) entry2.getValue()).intValue() + 1))));
            }
        } else if (fieldIndex != 7) {
            for (Entry<String, Integer> entry22 : map.entrySet()) {
                list.add(createEntry((String) entry22.getKey(), Long.valueOf((long) ((Integer) entry22.getValue()).intValue())));
            }
        } else {
            for (Entry<String, Integer> entry222 : map.entrySet()) {
                list.add(createEntry((String) entry222.getKey(), Long.valueOf((long) toWeekDay(((Integer) entry222.getValue()).intValue()))));
            }
        }
        return list.iterator();
    }

    private Object findStore(TemporalField field, Locale locale) {
        Entry<TemporalField, Locale> key = createEntry(field, locale);
        Object store = CACHE.get(key);
        if (store != null) {
            return store;
        }
        CACHE.putIfAbsent(key, createStore(field, locale));
        return CACHE.get(key);
    }

    private static int toWeekDay(int calWeekDay) {
        if (calWeekDay == 1) {
            return 7;
        }
        return calWeekDay - 1;
    }

    private Object createStore(TemporalField field, Locale locale) {
        TemporalField temporalField = field;
        Locale locale2 = locale;
        Map<TextStyle, Map<Long, String>> styleMap = new HashMap();
        int i = 0;
        int length;
        int length2;
        TextStyle textStyle;
        Map<String, Integer> displayNames;
        Map<Long, String> map;
        TextStyle[] values;
        if (temporalField == ChronoField.ERA) {
            for (TextStyle textStyle2 : TextStyle.values()) {
                if (!textStyle2.isStandalone()) {
                    displayNames = CalendarDataUtility.retrieveJavaTimeFieldValueNames("gregory", 0, textStyle2.toCalendarStyle(), locale2);
                    if (displayNames != null) {
                        map = new HashMap();
                        for (Entry<String, Integer> entry : displayNames.entrySet()) {
                            map.put(Long.valueOf((long) ((Integer) entry.getValue()).intValue()), (String) entry.getKey());
                        }
                        if (!map.isEmpty()) {
                            styleMap.put(textStyle2, map);
                        }
                    }
                }
            }
            return new LocaleStore(styleMap);
        } else if (temporalField == ChronoField.MONTH_OF_YEAR) {
            for (TextStyle textStyle3 : TextStyle.values()) {
                Map<String, Integer> displayNames2 = CalendarDataUtility.retrieveJavaTimeFieldValueNames("gregory", 2, textStyle3.toCalendarStyle(), locale2);
                Map<Long, String> map2 = new HashMap();
                if (displayNames2 != null) {
                    for (Entry<String, Integer> entry2 : displayNames2.entrySet()) {
                        map2.put(Long.valueOf((long) (((Integer) entry2.getValue()).intValue() + 1)), (String) entry2.getKey());
                    }
                } else {
                    for (int month = 0; month <= 11; month++) {
                        String name = CalendarDataUtility.retrieveJavaTimeFieldValueName("gregory", 2, month, textStyle3.toCalendarStyle(), locale2);
                        if (name == null) {
                            break;
                        }
                        map2.put(Long.valueOf((long) (month + 1)), name);
                    }
                }
                if (!map2.isEmpty()) {
                    styleMap.put(textStyle3, map2);
                }
            }
            return new LocaleStore(styleMap);
        } else if (temporalField == ChronoField.DAY_OF_WEEK) {
            values = TextStyle.values();
            length = values.length;
            while (i < length) {
                textStyle2 = values[i];
                displayNames = CalendarDataUtility.retrieveJavaTimeFieldValueNames("gregory", 7, textStyle2.toCalendarStyle(), locale2);
                map = new HashMap();
                if (displayNames != null) {
                    for (Entry<String, Integer> entry3 : displayNames.entrySet()) {
                        map.put(Long.valueOf((long) toWeekDay(((Integer) entry3.getValue()).intValue())), (String) entry3.getKey());
                    }
                } else {
                    for (int wday = 1; wday <= 7; wday++) {
                        String name2 = CalendarDataUtility.retrieveJavaTimeFieldValueName("gregory", 7, wday, textStyle2.toCalendarStyle(), locale2);
                        if (name2 == null) {
                            break;
                        }
                        map.put(Long.valueOf((long) toWeekDay(wday)), name2);
                    }
                }
                if (!map.isEmpty()) {
                    styleMap.put(textStyle2, map);
                }
                i++;
            }
            return new LocaleStore(styleMap);
        } else if (temporalField == ChronoField.AMPM_OF_DAY) {
            values = TextStyle.values();
            length2 = values.length;
            while (i < length2) {
                TextStyle textStyle4 = values[i];
                if (!textStyle4.isStandalone()) {
                    Map<String, Integer> displayNames3 = CalendarDataUtility.retrieveJavaTimeFieldValueNames("gregory", 9, textStyle4.toCalendarStyle(), locale2);
                    if (displayNames3 != null) {
                        Map<Long, String> map3 = new HashMap();
                        for (Entry<String, Integer> entry4 : displayNames3.entrySet()) {
                            map3.put(Long.valueOf((long) ((Integer) entry4.getValue()).intValue()), (String) entry4.getKey());
                        }
                        if (!map3.isEmpty()) {
                            styleMap.put(textStyle4, map3);
                        }
                    }
                }
                i++;
            }
            return new LocaleStore(styleMap);
        } else if (temporalField != IsoFields.QUARTER_OF_YEAR) {
            return "";
        } else {
            ICUResourceBundle quartersRb = ((ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt60b", locale2)).getWithFallback("calendar/gregorian/quarters");
            ICUResourceBundle formatRb = quartersRb.getWithFallback("format");
            ICUResourceBundle standaloneRb = quartersRb.getWithFallback("stand-alone");
            styleMap.put(TextStyle.FULL, extractQuarters(formatRb, "wide"));
            styleMap.put(TextStyle.FULL_STANDALONE, extractQuarters(standaloneRb, "wide"));
            styleMap.put(TextStyle.SHORT, extractQuarters(formatRb, "abbreviated"));
            styleMap.put(TextStyle.SHORT_STANDALONE, extractQuarters(standaloneRb, "abbreviated"));
            styleMap.put(TextStyle.NARROW, extractQuarters(formatRb, "narrow"));
            styleMap.put(TextStyle.NARROW_STANDALONE, extractQuarters(standaloneRb, "narrow"));
            return new LocaleStore(styleMap);
        }
    }

    private static Map<Long, String> extractQuarters(ICUResourceBundle rb, String key) {
        String[] names = rb.getWithFallback(key).getStringArray();
        Map<Long, String> map = new HashMap();
        for (int q = 0; q < names.length; q++) {
            map.put(Long.valueOf((long) (q + 1)), names[q]);
        }
        return map;
    }

    private static <A, B> Entry<A, B> createEntry(A text, B field) {
        return new SimpleImmutableEntry(text, field);
    }
}
