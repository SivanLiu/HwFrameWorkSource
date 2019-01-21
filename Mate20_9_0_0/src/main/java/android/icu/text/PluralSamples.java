package android.icu.text;

import android.icu.text.PluralRules.FixedDecimal;
import android.icu.text.PluralRules.IFixedDecimal;
import android.icu.text.PluralRules.KeywordStatus;
import android.icu.util.Output;
import dalvik.system.VMRuntime;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

@Deprecated
public class PluralSamples {
    private static final int LIMIT_FRACTION_SAMPLES = 3;
    private static final int[] TENS = new int[]{1, 10, 100, 1000, VMRuntime.SDK_VERSION_CUR_DEVELOPMENT, 100000, 1000000};
    private final Set<FixedDecimal> _fractionSamples;
    private final Map<String, Set<FixedDecimal>> _keyFractionSamplesMap;
    @Deprecated
    public final Map<String, Boolean> _keyLimitedMap;
    private final Map<String, List<Double>> _keySamplesMap;
    private PluralRules pluralRules;

    @Deprecated
    public PluralSamples(PluralRules pluralRules) {
        int keywordsRemaining;
        Map<String, Set<FixedDecimal>> sampleFractionMap;
        Set<FixedDecimal> mentioned;
        Map<String, Set<FixedDecimal>> foundKeywords;
        PluralRules pluralRules2 = pluralRules;
        this.pluralRules = pluralRules2;
        Set<String> keywords = pluralRules.getKeywords();
        HashMap temp = new HashMap();
        for (String k : keywords) {
            temp.put(k, pluralRules2.isLimited(k));
        }
        this._keyLimitedMap = temp;
        Map sampleMap = new HashMap();
        int i = 0;
        int keywordsRemaining2 = keywords.size();
        while (true) {
            int i2 = i;
            if (keywordsRemaining2 <= 0 || i2 >= 128) {
                keywordsRemaining = addSimpleSamples(pluralRules2, 3, sampleMap, keywordsRemaining2, 1000000.0d);
                sampleFractionMap = new HashMap();
                mentioned = new TreeSet();
                foundKeywords = new HashMap();
            } else {
                keywordsRemaining2 = addSimpleSamples(pluralRules2, 3, sampleMap, keywordsRemaining2, ((double) i2) / 2.0d);
                i = i2 + 1;
            }
        }
        keywordsRemaining = addSimpleSamples(pluralRules2, 3, sampleMap, keywordsRemaining2, 1000000.0d);
        sampleFractionMap = new HashMap();
        mentioned = new TreeSet();
        foundKeywords = new HashMap();
        for (IFixedDecimal s : mentioned) {
            addRelation(foundKeywords, pluralRules2.select(s), s);
        }
        if (foundKeywords.size() != keywords.size()) {
            int i3;
            for (i3 = 1; i3 < 1000; i3++) {
                if (addIfNotPresent((double) i3, mentioned, foundKeywords)) {
                    break;
                }
            }
            for (i3 = 10; i3 < 1000; i3++) {
                if (addIfNotPresent(((double) i3) / 10.0d, mentioned, foundKeywords)) {
                    break;
                }
            }
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to find sample for each keyword: ");
            stringBuilder.append(foundKeywords);
            stringBuilder.append("\n\t");
            stringBuilder.append(pluralRules2);
            stringBuilder.append("\n\t");
            stringBuilder.append(mentioned);
            printStream.println(stringBuilder.toString());
        }
        mentioned.add(new FixedDecimal(0));
        mentioned.add(new FixedDecimal(1));
        mentioned.add(new FixedDecimal(2));
        mentioned.add(new FixedDecimal(0.1d, 1));
        mentioned.add(new FixedDecimal(1.99d, 2));
        mentioned.addAll(fractions(mentioned));
        for (IFixedDecimal s2 : mentioned) {
            String keyword = pluralRules2.select(s2);
            Set<FixedDecimal> list = (Set) sampleFractionMap.get(keyword);
            if (list == null) {
                list = new LinkedHashSet();
                sampleFractionMap.put(keyword, list);
            }
            list.add(s2);
        }
        if (keywordsRemaining > 0) {
            for (String k2 : keywords) {
                if (!sampleMap.containsKey(k2)) {
                    sampleMap.put(k2, Collections.emptyList());
                }
                if (!sampleFractionMap.containsKey(k2)) {
                    sampleFractionMap.put(k2, Collections.emptySet());
                }
            }
        }
        for (Entry<String, List<Double>> entry : sampleMap.entrySet()) {
            sampleMap.put((String) entry.getKey(), Collections.unmodifiableList((List) entry.getValue()));
        }
        for (Entry<String, Set<FixedDecimal>> entry2 : sampleFractionMap.entrySet()) {
            sampleFractionMap.put((String) entry2.getKey(), Collections.unmodifiableSet((Set) entry2.getValue()));
        }
        this._keySamplesMap = sampleMap;
        this._keyFractionSamplesMap = sampleFractionMap;
        this._fractionSamples = Collections.unmodifiableSet(mentioned);
    }

    private int addSimpleSamples(PluralRules pluralRules, int MAX_SAMPLES, Map<String, List<Double>> sampleMap, int keywordsRemaining, double val) {
        String keyword = pluralRules.select(val);
        boolean keyIsLimited = ((Boolean) this._keyLimitedMap.get(keyword)).booleanValue();
        List<Double> list = (List) sampleMap.get(keyword);
        if (list == null) {
            list = new ArrayList(MAX_SAMPLES);
            sampleMap.put(keyword, list);
        } else if (!keyIsLimited && list.size() == MAX_SAMPLES) {
            return keywordsRemaining;
        }
        list.add(Double.valueOf(val));
        if (!keyIsLimited && list.size() == MAX_SAMPLES) {
            keywordsRemaining--;
        }
        return keywordsRemaining;
    }

    private void addRelation(Map<String, Set<FixedDecimal>> foundKeywords, String keyword, FixedDecimal s) {
        Set<FixedDecimal> set = (Set) foundKeywords.get(keyword);
        if (set == null) {
            HashSet hashSet = new HashSet();
            set = hashSet;
            foundKeywords.put(keyword, hashSet);
        }
        set.add(s);
    }

    private boolean addIfNotPresent(double d, Set<FixedDecimal> mentioned, Map<String, Set<FixedDecimal>> foundKeywords) {
        IFixedDecimal numberInfo = new FixedDecimal(d);
        String keyword = this.pluralRules.select(numberInfo);
        if (!foundKeywords.containsKey(keyword) || keyword.equals(PluralRules.KEYWORD_OTHER)) {
            addRelation(foundKeywords, keyword, numberInfo);
            mentioned.add(numberInfo);
            if (keyword.equals(PluralRules.KEYWORD_OTHER) && ((Set) foundKeywords.get(PluralRules.KEYWORD_OTHER)).size() > 1) {
                return true;
            }
        }
        return false;
    }

    private Set<FixedDecimal> fractions(Set<FixedDecimal> original) {
        List<Integer> ints;
        Set<FixedDecimal> toAddTo = new HashSet();
        Set<Integer> result = new HashSet();
        for (FixedDecimal base1 : original) {
            result.add(Integer.valueOf((int) base1.integerValue));
        }
        List<Integer> ints2 = new ArrayList(result);
        Set<String> keywords = new HashSet();
        int j = 0;
        while (j < ints2.size()) {
            Integer base = (Integer) ints2.get(j);
            String keyword = this.pluralRules.select((double) base.intValue());
            if (!keywords.contains(keyword)) {
                keywords.add(keyword);
                int i = 1;
                toAddTo.add(new FixedDecimal((double) base.intValue(), 1));
                toAddTo.add(new FixedDecimal((double) base.intValue(), 2));
                Integer fract = getDifferentCategory(ints2, keyword);
                if (fract.intValue() >= TENS[2]) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(base);
                    stringBuilder.append(".");
                    stringBuilder.append(fract);
                    toAddTo.add(new FixedDecimal(stringBuilder.toString()));
                } else {
                    int visibleFractions = 1;
                    while (visibleFractions < 3) {
                        int i2 = i;
                        while (i2 <= visibleFractions) {
                            Set<Integer> result2;
                            if (fract.intValue() >= TENS[i2]) {
                                result2 = result;
                                ints = ints2;
                            } else {
                                result2 = result;
                                ints = ints2;
                                toAddTo.add(new FixedDecimal(((double) base.intValue()) + (((double) fract.intValue()) / ((double) TENS[i2])), visibleFractions));
                            }
                            i2++;
                            result = result2;
                            ints2 = ints;
                        }
                        ints = ints2;
                        visibleFractions++;
                        i = 1;
                    }
                }
            }
            j++;
            result = result;
            ints2 = ints2;
        }
        ints = ints2;
        return toAddTo;
    }

    private Integer getDifferentCategory(List<Integer> ints, String keyword) {
        for (int i = ints.size() - 1; i >= 0; i--) {
            Integer other = (Integer) ints.get(i);
            if (!this.pluralRules.select((double) other.intValue()).equals(keyword)) {
                return other;
            }
        }
        return Integer.valueOf(37);
    }

    @Deprecated
    public KeywordStatus getStatus(String keyword, int offset, Set<Double> explicits, Output<Double> uniqueValue) {
        if (uniqueValue != null) {
            uniqueValue.value = null;
        }
        if (!this.pluralRules.getKeywords().contains(keyword)) {
            return KeywordStatus.INVALID;
        }
        Collection<Double> values = this.pluralRules.getAllKeywordValues(keyword);
        if (values == null) {
            return KeywordStatus.UNBOUNDED;
        }
        int originalSize = values.size();
        if (explicits == null) {
            explicits = Collections.emptySet();
        }
        if (originalSize <= explicits.size()) {
            HashSet<Double> subtractedSet = new HashSet(values);
            for (Double explicit : explicits) {
                subtractedSet.remove(Double.valueOf(explicit.doubleValue() - ((double) offset)));
            }
            if (subtractedSet.size() == 0) {
                return KeywordStatus.SUPPRESSED;
            }
            if (uniqueValue != null && subtractedSet.size() == 1) {
                uniqueValue.value = subtractedSet.iterator().next();
            }
            return originalSize == 1 ? KeywordStatus.UNIQUE : KeywordStatus.BOUNDED;
        } else if (originalSize != 1) {
            return KeywordStatus.BOUNDED;
        } else {
            if (uniqueValue != null) {
                uniqueValue.value = values.iterator().next();
            }
            return KeywordStatus.UNIQUE;
        }
    }

    Map<String, List<Double>> getKeySamplesMap() {
        return this._keySamplesMap;
    }

    Map<String, Set<FixedDecimal>> getKeyFractionSamplesMap() {
        return this._keyFractionSamplesMap;
    }

    Set<FixedDecimal> getFractionSamples() {
        return this._fractionSamples;
    }

    Collection<Double> getAllKeywordValues(String keyword) {
        if (!this.pluralRules.getKeywords().contains(keyword)) {
            return Collections.emptyList();
        }
        Collection<Double> result = (Collection) getKeySamplesMap().get(keyword);
        if (result.size() <= 2 || ((Boolean) this._keyLimitedMap.get(keyword)).booleanValue()) {
            return result;
        }
        return null;
    }
}
