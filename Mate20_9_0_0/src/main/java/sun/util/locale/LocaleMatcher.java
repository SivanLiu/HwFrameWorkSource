package sun.util.locale;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.FilteringMode;
import java.util.Locale.LanguageRange;
import java.util.Map;

public final class LocaleMatcher {
    public static List<Locale> filter(List<LanguageRange> priorityList, Collection<Locale> locales, FilteringMode mode) {
        if (priorityList.isEmpty() || locales.isEmpty()) {
            return new ArrayList();
        }
        List<String> tags = new ArrayList();
        for (Locale locale : locales) {
            tags.add(locale.toLanguageTag());
        }
        List<String> filteredTags = filterTags(priorityList, tags, mode);
        List<Locale> filteredLocales = new ArrayList(filteredTags.size());
        for (String tag : filteredTags) {
            filteredLocales.add(Locale.forLanguageTag(tag));
        }
        return filteredLocales;
    }

    public static List<String> filterTags(List<LanguageRange> priorityList, Collection<String> tags, FilteringMode mode) {
        if (priorityList.isEmpty() || tags.isEmpty()) {
            return new ArrayList();
        }
        if (mode == FilteringMode.EXTENDED_FILTERING) {
            return filterExtended(priorityList, tags);
        }
        ArrayList<LanguageRange> list = new ArrayList();
        for (LanguageRange lr : priorityList) {
            String range = lr.getRange();
            if (!range.startsWith("*-") && range.indexOf("-*") == -1) {
                list.add(lr);
            } else if (mode == FilteringMode.AUTOSELECT_FILTERING) {
                return filterExtended(priorityList, tags);
            } else {
                if (mode == FilteringMode.MAP_EXTENDED_RANGES) {
                    if (range.charAt(0) == '*') {
                        range = "*";
                    } else {
                        range = range.replaceAll("-[*]", "");
                    }
                    list.add(new LanguageRange(range, lr.getWeight()));
                } else if (mode == FilteringMode.REJECT_EXTENDED_RANGES) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("An extended range \"");
                    stringBuilder.append(range);
                    stringBuilder.append("\" found in REJECT_EXTENDED_RANGES mode.");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
        return filterBasic(list, tags);
    }

    private static List<String> filterBasic(List<LanguageRange> priorityList, Collection<String> tags) {
        List<String> list = new ArrayList();
        for (LanguageRange lr : priorityList) {
            String range = lr.getRange();
            if (range.equals("*")) {
                return new ArrayList((Collection) tags);
            }
            for (String tag : tags) {
                String tag2 = tag2.toLowerCase();
                if (tag2.startsWith(range)) {
                    int len = range.length();
                    if ((tag2.length() == len || tag2.charAt(len) == '-') && !list.contains(tag2)) {
                        list.add(tag2);
                    }
                }
            }
        }
        return list;
    }

    private static List<String> filterExtended(List<LanguageRange> priorityList, Collection<String> tags) {
        List<String> list = new ArrayList();
        for (LanguageRange lr : priorityList) {
            String range = lr.getRange();
            if (range.equals("*")) {
                return new ArrayList((Collection) tags);
            }
            String[] rangeSubtags = range.split(LanguageTag.SEP);
            for (String tag : tags) {
                String tag2 = tag2.toLowerCase();
                String[] tagSubtags = tag2.split(LanguageTag.SEP);
                if (rangeSubtags[0].equals(tagSubtags[0]) || rangeSubtags[0].equals("*")) {
                    int rangeIndex = 1;
                    int tagIndex = 1;
                    while (rangeIndex < rangeSubtags.length && tagIndex < tagSubtags.length) {
                        if (!rangeSubtags[rangeIndex].equals("*")) {
                            if (!rangeSubtags[rangeIndex].equals(tagSubtags[tagIndex])) {
                                if (tagSubtags[tagIndex].length() == 1 && !tagSubtags[tagIndex].equals("*")) {
                                    break;
                                }
                                tagIndex++;
                            } else {
                                rangeIndex++;
                                tagIndex++;
                            }
                        } else {
                            rangeIndex++;
                        }
                    }
                    if (rangeSubtags.length == rangeIndex && !list.contains(tag2)) {
                        list.add(tag2);
                    }
                }
            }
        }
        return list;
    }

    public static Locale lookup(List<LanguageRange> priorityList, Collection<Locale> locales) {
        if (priorityList.isEmpty() || locales.isEmpty()) {
            return null;
        }
        List<String> tags = new ArrayList();
        for (Locale locale : locales) {
            tags.add(locale.toLanguageTag());
        }
        String lookedUpTag = lookupTag(priorityList, tags);
        if (lookedUpTag == null) {
            return null;
        }
        return Locale.forLanguageTag(lookedUpTag);
    }

    public static String lookupTag(List<LanguageRange> priorityList, Collection<String> tags) {
        if (priorityList.isEmpty() || tags.isEmpty()) {
            return null;
        }
        for (LanguageRange lr : priorityList) {
            String range = lr.getRange();
            if (!range.equals("*")) {
                String rangeForRegex = range.replace((CharSequence) "*", (CharSequence) "\\p{Alnum}*");
                while (rangeForRegex.length() > 0) {
                    for (String tag : tags) {
                        String tag2 = tag2.toLowerCase();
                        if (tag2.matches(rangeForRegex)) {
                            return tag2;
                        }
                    }
                    int index = rangeForRegex.lastIndexOf(45);
                    if (index >= 0) {
                        rangeForRegex = rangeForRegex.substring(0, index);
                        if (rangeForRegex.lastIndexOf(45) == rangeForRegex.length() - 2) {
                            rangeForRegex = rangeForRegex.substring(0, rangeForRegex.length() - 2);
                        }
                    } else {
                        rangeForRegex = "";
                    }
                }
            }
        }
        return null;
    }

    public static List<LanguageRange> parse(String ranges) {
        String[] strArr;
        String ranges2 = ranges.replace((CharSequence) " ", (CharSequence) "").toLowerCase();
        if (ranges2.startsWith("accept-language:")) {
            ranges2 = ranges2.substring(16);
        }
        String ranges3 = ranges2;
        String[] langRanges = ranges3.split(",");
        ArrayList list = new ArrayList(langRanges.length);
        ArrayList tempList = new ArrayList();
        int length = langRanges.length;
        int i = 0;
        int numOfRanges = 0;
        int numOfRanges2 = 0;
        while (numOfRanges2 < length) {
            String r;
            double w;
            String str;
            int i2;
            String range = langRanges[numOfRanges2];
            int indexOf = range.indexOf(";q=");
            int index = indexOf;
            if (indexOf == -1) {
                r = range;
                w = 1.0d;
            } else {
                r = range.substring(i, index);
                index += 3;
                try {
                    w = Double.parseDouble(range.substring(index));
                    if (w < 0.0d || w > 1.0d) {
                        strArr = langRanges;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("weight=");
                        stringBuilder.append(w);
                        stringBuilder.append(" for language range \"");
                        stringBuilder.append(r);
                        stringBuilder.append("\". It must be between ");
                        stringBuilder.append(0.0d);
                        stringBuilder.append(" and ");
                        stringBuilder.append(1.0d);
                        stringBuilder.append(".");
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } catch (Exception e) {
                    str = ranges3;
                    strArr = langRanges;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("weight=\"");
                    stringBuilder2.append(range.substring(index));
                    stringBuilder2.append("\" for language range \"");
                    stringBuilder2.append(r);
                    stringBuilder2.append("\"");
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            }
            if (!tempList.contains(r)) {
                LanguageRange lr = new LanguageRange(r, w);
                index = numOfRanges;
                for (int j = i; j < numOfRanges; j++) {
                    if (((LanguageRange) list.get(j)).getWeight() < w) {
                        index = j;
                        break;
                    }
                }
                list.add(index, lr);
                numOfRanges++;
                tempList.add(r);
                String equivalentForRegionAndVariant = getEquivalentForRegionAndVariant(r);
                String equivalent = equivalentForRegionAndVariant;
                if (!(equivalentForRegionAndVariant == null || tempList.contains(equivalent))) {
                    list.add(index + 1, new LanguageRange(equivalent, w));
                    numOfRanges++;
                    tempList.add(equivalent);
                }
                String[] equivalentsForLanguage = getEquivalentsForLanguage(r);
                String[] equivalents = equivalentsForLanguage;
                if (equivalentsForLanguage != null) {
                    i = equivalents.length;
                    int numOfRanges3 = numOfRanges;
                    numOfRanges = 0;
                    while (numOfRanges < i) {
                        str = ranges3;
                        ranges3 = equivalents[numOfRanges];
                        if (tempList.contains(ranges3)) {
                            strArr = langRanges;
                            i2 = length;
                        } else {
                            strArr = langRanges;
                            i2 = length;
                            list.add(index + 1, new LanguageRange(ranges3, w));
                            numOfRanges3++;
                            tempList.add(ranges3);
                        }
                        equivalent = getEquivalentForRegionAndVariant(ranges3);
                        if (!(equivalent == null || tempList.contains(equivalent))) {
                            list.add(index + 1, new LanguageRange(equivalent, w));
                            numOfRanges3++;
                            tempList.add(equivalent);
                        }
                        numOfRanges++;
                        ranges3 = str;
                        langRanges = strArr;
                        length = i2;
                    }
                    str = ranges3;
                    strArr = langRanges;
                    i2 = length;
                    numOfRanges = numOfRanges3;
                    numOfRanges2++;
                    ranges3 = str;
                    langRanges = strArr;
                    length = i2;
                    i = 0;
                }
            }
            str = ranges3;
            strArr = langRanges;
            i2 = length;
            numOfRanges2++;
            ranges3 = str;
            langRanges = strArr;
            length = i2;
            i = 0;
        }
        strArr = langRanges;
        return list;
    }

    private static String replaceFirstSubStringMatch(String range, String substr, String replacement) {
        int pos = range.indexOf(substr);
        if (pos == -1) {
            return range;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(range.substring(0, pos));
        stringBuilder.append(replacement);
        stringBuilder.append(range.substring(substr.length() + pos));
        return stringBuilder.toString();
    }

    private static String[] getEquivalentsForLanguage(String range) {
        String r = range;
        while (r.length() > 0) {
            int i = 0;
            if (LocaleEquivalentMaps.singleEquivMap.containsKey(r)) {
                return new String[]{replaceFirstSubStringMatch(range, r, (String) LocaleEquivalentMaps.singleEquivMap.get(r))};
            } else if (LocaleEquivalentMaps.multiEquivsMap.containsKey(r)) {
                String[] equivs = (String[]) LocaleEquivalentMaps.multiEquivsMap.get(r);
                String[] result = new String[equivs.length];
                while (i < equivs.length) {
                    result[i] = replaceFirstSubStringMatch(range, r, equivs[i]);
                    i++;
                }
                return result;
            } else {
                int index = r.lastIndexOf(45);
                if (index == -1) {
                    break;
                }
                r = r.substring(0, index);
            }
        }
        return null;
    }

    private static String getEquivalentForRegionAndVariant(String range) {
        int extensionKeyIndex = getExtentionKeyIndex(range);
        for (String subtag : LocaleEquivalentMaps.regionVariantEquivMap.keySet()) {
            int indexOf = range.indexOf(subtag);
            int index = indexOf;
            if (indexOf != -1) {
                if (extensionKeyIndex == Integer.MIN_VALUE || index <= extensionKeyIndex) {
                    indexOf = subtag.length() + index;
                    if (range.length() == indexOf || range.charAt(indexOf) == '-') {
                        return replaceFirstSubStringMatch(range, subtag, (String) LocaleEquivalentMaps.regionVariantEquivMap.get(subtag));
                    }
                }
            }
        }
        return null;
    }

    private static int getExtentionKeyIndex(String s) {
        char[] c = s.toCharArray();
        int index = Integer.MIN_VALUE;
        for (int i = 1; i < c.length; i++) {
            if (c[i] == '-') {
                if (i - index == 2) {
                    return index;
                }
                index = i;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static List<LanguageRange> mapEquivalents(List<LanguageRange> priorityList, Map<String, List<String>> map) {
        Map<String, List<String>> map2 = map;
        if (priorityList.isEmpty()) {
            return new ArrayList();
        }
        if (map2 == null || map.isEmpty()) {
            return new ArrayList((Collection) priorityList);
        }
        Map<String, String> keyMap = new HashMap();
        for (String key : map.keySet()) {
            keyMap.put(key.toLowerCase(), key);
        }
        List<LanguageRange> list = new ArrayList();
        for (LanguageRange lr : priorityList) {
            String range = lr.getRange();
            String r = range;
            boolean hasEquivalent = false;
            while (r.length() > 0) {
                int len;
                if (keyMap.containsKey(r)) {
                    hasEquivalent = true;
                    List<String> equivalents = (List) map2.get(keyMap.get(r));
                    if (equivalents != null) {
                        len = r.length();
                        for (String equivalent : equivalents) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(equivalent.toLowerCase());
                            stringBuilder.append(range.substring(len));
                            list.add(new LanguageRange(stringBuilder.toString(), lr.getWeight()));
                        }
                    }
                } else {
                    len = r.lastIndexOf(45);
                    if (len == -1) {
                        break;
                    }
                    r = r.substring(0, len);
                }
            }
            if (!hasEquivalent) {
                list.add(lr);
            }
        }
        return list;
    }

    private LocaleMatcher() {
    }
}
