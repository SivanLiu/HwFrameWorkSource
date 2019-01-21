package android.icu.text;

import android.icu.impl.Normalizer2Impl.Hangul;
import android.icu.lang.UCharacter;
import android.icu.text.UTF16.StringComparator;
import android.icu.util.LocaleData;
import android.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class AlphabeticIndex<V> implements Iterable<Bucket<V>> {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String BASE = "﷐";
    private static final char CGJ = '͏';
    private static final int GC_CN_MASK = 1;
    private static final int GC_LL_MASK = 4;
    private static final int GC_LM_MASK = 16;
    private static final int GC_LO_MASK = 32;
    private static final int GC_LT_MASK = 8;
    private static final int GC_LU_MASK = 2;
    private static final int GC_L_MASK = 62;
    private static final Comparator<String> binaryCmp = new StringComparator(true, false, 0);
    private BucketList<V> buckets;
    private RuleBasedCollator collatorExternal;
    private final RuleBasedCollator collatorOriginal;
    private final RuleBasedCollator collatorPrimaryOnly;
    private final List<String> firstCharsInScripts;
    private String inflowLabel;
    private final UnicodeSet initialLabels;
    private List<Record<V>> inputList;
    private int maxLabelCount;
    private String overflowLabel;
    private final Comparator<Record<V>> recordComparator;
    private String underflowLabel;

    public static class Bucket<V> implements Iterable<Record<V>> {
        private Bucket<V> displayBucket;
        private int displayIndex;
        private final String label;
        private final LabelType labelType;
        private final String lowerBoundary;
        private List<Record<V>> records;

        public enum LabelType {
            NORMAL,
            UNDERFLOW,
            INFLOW,
            OVERFLOW
        }

        /* synthetic */ Bucket(String x0, String x1, LabelType x2, AnonymousClass1 x3) {
            this(x0, x1, x2);
        }

        private Bucket(String label, String lowerBoundary, LabelType labelType) {
            this.label = label;
            this.lowerBoundary = lowerBoundary;
            this.labelType = labelType;
        }

        public String getLabel() {
            return this.label;
        }

        public LabelType getLabelType() {
            return this.labelType;
        }

        public int size() {
            return this.records == null ? 0 : this.records.size();
        }

        public Iterator<Record<V>> iterator() {
            if (this.records == null) {
                return Collections.emptyList().iterator();
            }
            return this.records.iterator();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{labelType=");
            stringBuilder.append(this.labelType);
            stringBuilder.append(", lowerBoundary=");
            stringBuilder.append(this.lowerBoundary);
            stringBuilder.append(", label=");
            stringBuilder.append(this.label);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private static class BucketList<V> implements Iterable<Bucket<V>> {
        private final ArrayList<Bucket<V>> bucketList;
        private final List<Bucket<V>> immutableVisibleList;

        /* synthetic */ BucketList(ArrayList x0, ArrayList x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private BucketList(ArrayList<Bucket<V>> bucketList, ArrayList<Bucket<V>> publicBucketList) {
            this.bucketList = bucketList;
            int displayIndex = 0;
            Iterator it = publicBucketList.iterator();
            while (it.hasNext()) {
                int displayIndex2 = displayIndex + 1;
                ((Bucket) it.next()).displayIndex = displayIndex;
                displayIndex = displayIndex2;
            }
            this.immutableVisibleList = Collections.unmodifiableList(publicBucketList);
        }

        private int getBucketCount() {
            return this.immutableVisibleList.size();
        }

        private int getBucketIndex(CharSequence name, Collator collatorPrimaryOnly) {
            int start = 0;
            int limit = this.bucketList.size();
            while (start + 1 < limit) {
                int i = (start + limit) / 2;
                if (collatorPrimaryOnly.compare((Object) name, ((Bucket) this.bucketList.get(i)).lowerBoundary) < 0) {
                    limit = i;
                } else {
                    start = i;
                }
            }
            Bucket<V> bucket = (Bucket) this.bucketList.get(start);
            if (bucket.displayBucket != null) {
                bucket = bucket.displayBucket;
            }
            return bucket.displayIndex;
        }

        private Iterator<Bucket<V>> fullIterator() {
            return this.bucketList.iterator();
        }

        public Iterator<Bucket<V>> iterator() {
            return this.immutableVisibleList.iterator();
        }
    }

    public static final class ImmutableIndex<V> implements Iterable<Bucket<V>> {
        private final BucketList<V> buckets;
        private final Collator collatorPrimaryOnly;

        /* synthetic */ ImmutableIndex(BucketList x0, Collator x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private ImmutableIndex(BucketList<V> bucketList, Collator collatorPrimaryOnly) {
            this.buckets = bucketList;
            this.collatorPrimaryOnly = collatorPrimaryOnly;
        }

        public int getBucketCount() {
            return this.buckets.getBucketCount();
        }

        public int getBucketIndex(CharSequence name) {
            return this.buckets.getBucketIndex(name, this.collatorPrimaryOnly);
        }

        public Bucket<V> getBucket(int index) {
            if (index < 0 || index >= this.buckets.getBucketCount()) {
                return null;
            }
            return (Bucket) this.buckets.immutableVisibleList.get(index);
        }

        public Iterator<Bucket<V>> iterator() {
            return this.buckets.iterator();
        }
    }

    public static class Record<V> {
        private final V data;
        private final CharSequence name;

        /* synthetic */ Record(CharSequence x0, Object x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private Record(CharSequence name, V data) {
            this.name = name;
            this.data = data;
        }

        public CharSequence getName() {
            return this.name;
        }

        public V getData() {
            return this.data;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.name);
            stringBuilder.append("=");
            stringBuilder.append(this.data);
            return stringBuilder.toString();
        }
    }

    public AlphabeticIndex(ULocale locale) {
        this(locale, null);
    }

    public AlphabeticIndex(Locale locale) {
        this(ULocale.forLocale(locale), null);
    }

    public AlphabeticIndex(RuleBasedCollator collator) {
        this(null, collator);
    }

    private AlphabeticIndex(ULocale locale, RuleBasedCollator collator) {
        this.recordComparator = new Comparator<Record<V>>() {
            public int compare(Record<V> o1, Record<V> o2) {
                return AlphabeticIndex.this.collatorOriginal.compare((Object) o1.name, (Object) o2.name);
            }
        };
        this.initialLabels = new UnicodeSet();
        this.overflowLabel = "…";
        this.underflowLabel = "…";
        this.inflowLabel = "…";
        this.maxLabelCount = 99;
        this.collatorOriginal = collator != null ? collator : (RuleBasedCollator) Collator.getInstance(locale);
        try {
            this.collatorPrimaryOnly = this.collatorOriginal.cloneAsThawed();
            this.collatorPrimaryOnly.setStrength(0);
            this.collatorPrimaryOnly.freeze();
            this.firstCharsInScripts = getFirstCharactersInScripts();
            Collections.sort(this.firstCharsInScripts, this.collatorPrimaryOnly);
            while (!this.firstCharsInScripts.isEmpty()) {
                if (this.collatorPrimaryOnly.compare((String) this.firstCharsInScripts.get(0), "") == 0) {
                    this.firstCharsInScripts.remove(0);
                } else if (!addChineseIndexCharacters() && locale != null) {
                    addIndexExemplars(locale);
                    return;
                } else {
                    return;
                }
            }
            throw new IllegalArgumentException("AlphabeticIndex requires some non-ignorable script boundary strings");
        } catch (Exception e) {
            throw new IllegalStateException("Collator cannot be cloned", e);
        }
    }

    public AlphabeticIndex<V> addLabels(UnicodeSet additions) {
        this.initialLabels.addAll(additions);
        this.buckets = null;
        return this;
    }

    public AlphabeticIndex<V> addLabels(ULocale... additions) {
        for (ULocale addition : additions) {
            addIndexExemplars(addition);
        }
        this.buckets = null;
        return this;
    }

    public AlphabeticIndex<V> addLabels(Locale... additions) {
        for (Locale addition : additions) {
            addIndexExemplars(ULocale.forLocale(addition));
        }
        this.buckets = null;
        return this;
    }

    public AlphabeticIndex<V> setOverflowLabel(String overflowLabel) {
        this.overflowLabel = overflowLabel;
        this.buckets = null;
        return this;
    }

    public String getUnderflowLabel() {
        return this.underflowLabel;
    }

    public AlphabeticIndex<V> setUnderflowLabel(String underflowLabel) {
        this.underflowLabel = underflowLabel;
        this.buckets = null;
        return this;
    }

    public String getOverflowLabel() {
        return this.overflowLabel;
    }

    public AlphabeticIndex<V> setInflowLabel(String inflowLabel) {
        this.inflowLabel = inflowLabel;
        this.buckets = null;
        return this;
    }

    public String getInflowLabel() {
        return this.inflowLabel;
    }

    public int getMaxLabelCount() {
        return this.maxLabelCount;
    }

    public AlphabeticIndex<V> setMaxLabelCount(int maxLabelCount) {
        this.maxLabelCount = maxLabelCount;
        this.buckets = null;
        return this;
    }

    private List<String> initLabels() {
        Normalizer2 nfkdNormalizer = Normalizer2.getNFKDInstance();
        List<String> indexCharacters = new ArrayList();
        String firstScriptBoundary = (String) this.firstCharsInScripts.get(0);
        String overflowBoundary = (String) this.firstCharsInScripts.get(this.firstCharsInScripts.size() - 1);
        Iterator it = this.initialLabels.iterator();
        while (it.hasNext()) {
            String item = (String) it.next();
            boolean checkDistinct;
            if (!UTF16.hasMoreCodePointsThan(item, 1)) {
                checkDistinct = false;
            } else if (item.charAt(item.length() - 1) != '*' || item.charAt(item.length() - 2) == '*') {
                checkDistinct = true;
            } else {
                item = item.substring(0, item.length() - 1);
                checkDistinct = false;
            }
            if (this.collatorPrimaryOnly.compare(item, firstScriptBoundary) >= 0 && this.collatorPrimaryOnly.compare(item, overflowBoundary) < 0 && !(checkDistinct && this.collatorPrimaryOnly.compare(item, separated(item)) == 0)) {
                int insertionPoint = Collections.binarySearch(indexCharacters, item, this.collatorPrimaryOnly);
                if (insertionPoint < 0) {
                    indexCharacters.add(~insertionPoint, item);
                } else if (isOneLabelBetterThanOther(nfkdNormalizer, item, (String) indexCharacters.get(insertionPoint))) {
                    indexCharacters.set(insertionPoint, item);
                }
            }
        }
        int size = indexCharacters.size() - 1;
        if (size > this.maxLabelCount) {
            int count = 0;
            int old = -1;
            Iterator<String> it2 = indexCharacters.iterator();
            while (it2.hasNext()) {
                count++;
                it2.next();
                int bump = (this.maxLabelCount * count) / size;
                if (bump == old) {
                    it2.remove();
                } else {
                    old = bump;
                }
            }
        }
        return indexCharacters;
    }

    private static String fixLabel(String current) {
        if (!current.startsWith(BASE)) {
            return current;
        }
        int rest = current.charAt(BASE.length());
        if (10240 >= rest || rest > 10495) {
            return current.substring(BASE.length());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(rest - 10240);
        stringBuilder.append("劃");
        return stringBuilder.toString();
    }

    private void addIndexExemplars(ULocale locale) {
        UnicodeSet exemplars = LocaleData.getExemplarSet(locale, 0, 2);
        if (exemplars == null || exemplars.isEmpty()) {
            UnicodeSet exemplars2 = LocaleData.getExemplarSet(locale, 0, 0).cloneAsThawed();
            if (exemplars2.containsSome(97, 122) || exemplars2.size() == 0) {
                exemplars2.addAll(97, 122);
            }
            if (exemplars2.containsSome(Hangul.HANGUL_BASE, Hangul.HANGUL_END)) {
                exemplars2.remove(Hangul.HANGUL_BASE, Hangul.HANGUL_END).add((int) Hangul.HANGUL_BASE).add(45208).add(45796).add(46972).add(47560).add(48148).add(49324).add(50500).add(51088).add(52264).add(52852).add(53440).add(54028).add(54616);
            }
            if (exemplars2.containsSome(4608, 4991)) {
                UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]"));
                while (it.next() && it.codepoint != UnicodeSetIterator.IS_STRING) {
                    if ((it.codepoint & 7) != 0) {
                        exemplars2.remove(it.codepoint);
                    }
                }
            }
            Iterator it2 = exemplars2.iterator();
            while (it2.hasNext()) {
                this.initialLabels.add(UCharacter.toUpperCase(locale, (String) it2.next()));
            }
            return;
        }
        this.initialLabels.addAll(exemplars);
    }

    private boolean addChineseIndexCharacters() {
        UnicodeSet contractions = new UnicodeSet();
        try {
            this.collatorPrimaryOnly.internalAddContractions(BASE.charAt(0), contractions);
            if (contractions.isEmpty()) {
                return false;
            }
            this.initialLabels.addAll(contractions);
            Iterator it = contractions.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                char c = s.charAt(s.length() - 1);
                if ('A' <= c && c <= 'Z') {
                    this.initialLabels.add(65, 90);
                    break;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String separated(String item) {
        StringBuilder result = new StringBuilder();
        char last = item.charAt(0);
        result.append(last);
        for (int i = 1; i < item.length(); i++) {
            char ch = item.charAt(i);
            if (!UCharacter.isHighSurrogate(last) || !UCharacter.isLowSurrogate(ch)) {
                result.append(CGJ);
            }
            result.append(ch);
            last = ch;
        }
        return result.toString();
    }

    public ImmutableIndex<V> buildImmutableIndex() {
        BucketList<V> immutableBucketList;
        if (this.inputList == null || this.inputList.isEmpty()) {
            if (this.buckets == null) {
                this.buckets = createBucketList();
            }
            immutableBucketList = this.buckets;
        } else {
            immutableBucketList = createBucketList();
        }
        return new ImmutableIndex(immutableBucketList, this.collatorPrimaryOnly, null);
    }

    public List<String> getBucketLabels() {
        initBuckets();
        ArrayList<String> result = new ArrayList();
        Iterator it = this.buckets.iterator();
        while (it.hasNext()) {
            result.add(((Bucket) it.next()).getLabel());
        }
        return result;
    }

    public RuleBasedCollator getCollator() {
        if (this.collatorExternal == null) {
            try {
                this.collatorExternal = (RuleBasedCollator) this.collatorOriginal.clone();
            } catch (Exception e) {
                throw new IllegalStateException("Collator cannot be cloned", e);
            }
        }
        return this.collatorExternal;
    }

    public AlphabeticIndex<V> addRecord(CharSequence name, V data) {
        this.buckets = null;
        if (this.inputList == null) {
            this.inputList = new ArrayList();
        }
        this.inputList.add(new Record(name, data, null));
        return this;
    }

    public int getBucketIndex(CharSequence name) {
        initBuckets();
        return this.buckets.getBucketIndex(name, this.collatorPrimaryOnly);
    }

    public AlphabeticIndex<V> clearRecords() {
        if (!(this.inputList == null || this.inputList.isEmpty())) {
            this.inputList.clear();
            this.buckets = null;
        }
        return this;
    }

    public int getBucketCount() {
        initBuckets();
        return this.buckets.getBucketCount();
    }

    public int getRecordCount() {
        return this.inputList != null ? this.inputList.size() : 0;
    }

    public Iterator<Bucket<V>> iterator() {
        initBuckets();
        return this.buckets.iterator();
    }

    private void initBuckets() {
        if (this.buckets == null) {
            this.buckets = createBucketList();
            if (this.inputList != null && !this.inputList.isEmpty()) {
                Bucket nextBucket;
                String upperBoundary;
                Collections.sort(this.inputList, this.recordComparator);
                Iterator<Bucket<V>> bucketIterator = this.buckets.fullIterator();
                Bucket<V> currentBucket = (Bucket) bucketIterator.next();
                if (bucketIterator.hasNext()) {
                    nextBucket = (Bucket) bucketIterator.next();
                    upperBoundary = nextBucket.lowerBoundary;
                } else {
                    nextBucket = null;
                    upperBoundary = null;
                }
                for (Record<V> r : this.inputList) {
                    while (upperBoundary != null && this.collatorPrimaryOnly.compare((Object) r.name, (Object) upperBoundary) >= 0) {
                        currentBucket = nextBucket;
                        if (bucketIterator.hasNext()) {
                            nextBucket = (Bucket) bucketIterator.next();
                            upperBoundary = nextBucket.lowerBoundary;
                        } else {
                            upperBoundary = null;
                        }
                    }
                    Bucket<V> bucket = currentBucket;
                    if (bucket.displayBucket != null) {
                        bucket = bucket.displayBucket;
                    }
                    if (bucket.records == null) {
                        bucket.records = new ArrayList();
                    }
                    bucket.records.add(r);
                }
            }
        }
    }

    private static boolean isOneLabelBetterThanOther(Normalizer2 nfkdNormalizer, String one, String other) {
        String n1 = nfkdNormalizer.normalize(one);
        String n2 = nfkdNormalizer.normalize(other);
        boolean z = false;
        int result = n1.codePointCount(0, n1.length()) - n2.codePointCount(0, n2.length());
        if (result != 0) {
            if (result < 0) {
                z = true;
            }
            return z;
        }
        result = binaryCmp.compare(n1, n2);
        if (result != 0) {
            if (result < 0) {
                z = true;
            }
            return z;
        }
        if (binaryCmp.compare(one, other) < 0) {
            z = true;
        }
        return z;
    }

    private BucketList<V> createBucketList() {
        long variableTop;
        boolean z;
        int i;
        Bucket<V> singleBucket;
        long variableTop2;
        List<String> indexCharacters = initLabels();
        if (this.collatorPrimaryOnly.isAlternateHandlingShifted()) {
            variableTop = ((long) this.collatorPrimaryOnly.getVariableTop()) & 4294967295L;
        } else {
            variableTop = 0;
        }
        boolean hasInvisibleBuckets = false;
        Bucket<V>[] asciiBuckets = new Bucket[26];
        Bucket<V>[] pinyinBuckets = new Bucket[26];
        boolean hasPinyin = false;
        ArrayList<Bucket<V>> bucketList = new ArrayList();
        bucketList.add(new Bucket(getUnderflowLabel(), "", LabelType.UNDERFLOW, null));
        int scriptIndex = -1;
        String scriptUpperBoundary = "";
        Iterator it = indexCharacters.iterator();
        while (true) {
            int i2 = 1;
            if (!it.hasNext()) {
                break;
            }
            List<String> indexCharacters2;
            char charAt;
            char c;
            String current = (String) it.next();
            if (this.collatorPrimaryOnly.compare(current, scriptUpperBoundary) >= 0) {
                String scriptUpperBoundary2;
                String inflowBoundary = scriptUpperBoundary;
                int scriptIndex2 = scriptIndex;
                boolean skippedScript = false;
                while (true) {
                    scriptIndex2 += i2;
                    scriptUpperBoundary2 = (String) this.firstCharsInScripts.get(scriptIndex2);
                    if (this.collatorPrimaryOnly.compare(current, scriptUpperBoundary2) < 0) {
                        break;
                    }
                    z = hasInvisibleBuckets;
                    boolean z2 = skippedScript;
                    skippedScript = true;
                    String str = scriptUpperBoundary2;
                    i2 = 1;
                }
                if (skippedScript) {
                    indexCharacters2 = indexCharacters;
                    if (bucketList.size() > 1) {
                        z = hasInvisibleBuckets;
                        bucketList.add(new Bucket(getInflowLabel(), inflowBoundary, LabelType.INFLOW, false));
                    } else {
                        z = hasInvisibleBuckets;
                    }
                } else {
                    indexCharacters2 = indexCharacters;
                    z = hasInvisibleBuckets;
                }
                scriptIndex = scriptIndex2;
                scriptUpperBoundary = scriptUpperBoundary2;
            } else {
                indexCharacters2 = indexCharacters;
                z = hasInvisibleBuckets;
            }
            indexCharacters = new Bucket(fixLabel(current), current, LabelType.NORMAL, null);
            bucketList.add(indexCharacters);
            if (current.length() == 1) {
                charAt = current.charAt(0);
                c = charAt;
                if ('A' <= charAt && c <= 'Z') {
                    asciiBuckets[c - 65] = indexCharacters;
                    if (current.startsWith(BASE) && hasMultiplePrimaryWeights(this.collatorPrimaryOnly, variableTop, current) && !current.endsWith("￿")) {
                        i = bucketList.size() - 2;
                        while (true) {
                            singleBucket = (Bucket) bucketList.get(i);
                            if (singleBucket.labelType == LabelType.NORMAL) {
                                if (singleBucket.displayBucket == null && !hasMultiplePrimaryWeights(this.collatorPrimaryOnly, variableTop, singleBucket.lowerBoundary)) {
                                    List<String> bucket = indexCharacters;
                                    indexCharacters = new StringBuilder();
                                    indexCharacters.append(current);
                                    variableTop2 = variableTop;
                                    indexCharacters.append("￿");
                                    Bucket indexCharacters3 = new Bucket("", indexCharacters.toString(), LabelType.NORMAL, null);
                                    indexCharacters3.displayBucket = singleBucket;
                                    bucketList.add(indexCharacters3);
                                    hasInvisibleBuckets = 1;
                                    break;
                                }
                                i--;
                                indexCharacters = indexCharacters;
                                variableTop = variableTop;
                            } else {
                                variableTop2 = variableTop;
                                break;
                            }
                        }
                        indexCharacters = indexCharacters2;
                        variableTop = variableTop2;
                    } else {
                        variableTop2 = variableTop;
                    }
                    hasInvisibleBuckets = z;
                    indexCharacters = indexCharacters2;
                    variableTop = variableTop2;
                }
            }
            if (current.length() == BASE.length() + 1 && current.startsWith(BASE)) {
                charAt = current.charAt(BASE.length());
                c = charAt;
                if ('A' <= charAt && c <= 'Z') {
                    pinyinBuckets[c - 65] = indexCharacters;
                    hasPinyin = true;
                }
            }
            if (current.startsWith(BASE)) {
            }
            variableTop2 = variableTop;
            hasInvisibleBuckets = z;
            indexCharacters = indexCharacters2;
            variableTop = variableTop2;
        }
        variableTop2 = variableTop;
        z = hasInvisibleBuckets;
        i = 0;
        if (bucketList.size() == 1) {
            return new BucketList(bucketList, bucketList, null);
        }
        bucketList.add(new Bucket(getOverflowLabel(), scriptUpperBoundary, LabelType.OVERFLOW, null));
        if (hasPinyin) {
            Bucket<V> asciiBucket = null;
            while (true) {
                int i3 = i;
                if (i3 >= 26) {
                    break;
                }
                if (asciiBuckets[i3] != null) {
                    asciiBucket = asciiBuckets[i3];
                }
                if (!(pinyinBuckets[i3] == null || asciiBucket == null)) {
                    pinyinBuckets[i3].displayBucket = asciiBucket;
                    z = true;
                }
                i = i3 + 1;
            }
        }
        if (!z) {
            return new BucketList(bucketList, bucketList, null);
        }
        int i4 = bucketList.size() - 1;
        Bucket<V> nextBucket = (Bucket) bucketList.get(i4);
        while (true) {
            i4--;
            if (i4 <= 0) {
                break;
            }
            Bucket<V> bucket2 = (Bucket) bucketList.get(i4);
            if (bucket2.displayBucket == null) {
                if (bucket2.labelType != LabelType.INFLOW || nextBucket.labelType == LabelType.NORMAL) {
                    nextBucket = bucket2;
                } else {
                    bucket2.displayBucket = nextBucket;
                }
            }
        }
        ArrayList<Bucket<V>> publicBucketList = new ArrayList();
        Iterator it2 = bucketList.iterator();
        while (it2.hasNext()) {
            singleBucket = (Bucket) it2.next();
            if (singleBucket.displayBucket == null) {
                publicBucketList.add(singleBucket);
            }
        }
        return new BucketList(bucketList, publicBucketList, null);
    }

    private static boolean hasMultiplePrimaryWeights(RuleBasedCollator coll, long variableTop, String s) {
        long[] ces = coll.internalGetCEs(s);
        boolean seenPrimary = false;
        for (long ce : ces) {
            if ((ce >>> 32) > variableTop) {
                if (seenPrimary) {
                    return true;
                }
                seenPrimary = true;
            }
        }
        return false;
    }

    @Deprecated
    public List<String> getFirstCharactersInScripts() {
        List<String> dest = new ArrayList(200);
        UnicodeSet set = new UnicodeSet();
        this.collatorPrimaryOnly.internalAddContractions(64977, set);
        if (set.isEmpty()) {
            throw new UnsupportedOperationException("AlphabeticIndex requires script-first-primary contractions");
        }
        Iterator it = set.iterator();
        while (it.hasNext()) {
            String boundary = (String) it.next();
            if (((1 << UCharacter.getType(boundary.codePointAt(1))) & 63) != 0) {
                dest.add(boundary);
            }
        }
        return dest;
    }
}
