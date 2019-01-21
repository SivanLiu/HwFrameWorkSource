package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Utility;
import android.icu.util.CaseInsensitiveString;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import android.icu.util.UResourceBundle;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

public abstract class Transliterator implements StringTransform {
    static final boolean DEBUG = false;
    public static final int FORWARD = 0;
    static final char ID_DELIM = ';';
    static final char ID_SEP = '-';
    private static final String RB_DISPLAY_NAME_PATTERN = "TransliteratorNamePattern";
    private static final String RB_DISPLAY_NAME_PREFIX = "%Translit%%";
    private static final String RB_RULE_BASED_IDS = "RuleBasedTransliteratorIDs";
    private static final String RB_SCRIPT_DISPLAY_NAME_PREFIX = "%Translit%";
    public static final int REVERSE = 1;
    private static final String ROOT = "root";
    static final char VARIANT_SEP = '/';
    private static Map<CaseInsensitiveString, String> displayNameCache = Collections.synchronizedMap(new HashMap());
    private static TransliteratorRegistry registry = new TransliteratorRegistry();
    private String ID;
    private UnicodeSet filter;
    private int maximumContextLength = 0;

    public interface Factory {
        Transliterator getInstance(String str);
    }

    public static class Position {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        public int contextLimit;
        public int contextStart;
        public int limit;
        public int start;

        static {
            Class cls = Transliterator.class;
        }

        public Position() {
            this(0, 0, 0, 0);
        }

        public Position(int contextStart, int contextLimit, int start) {
            this(contextStart, contextLimit, start, contextLimit);
        }

        public Position(int contextStart, int contextLimit, int start, int limit) {
            this.contextStart = contextStart;
            this.contextLimit = contextLimit;
            this.start = start;
            this.limit = limit;
        }

        public Position(Position pos) {
            set(pos);
        }

        public void set(Position pos) {
            this.contextStart = pos.contextStart;
            this.contextLimit = pos.contextLimit;
            this.start = pos.start;
            this.limit = pos.limit;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof Position)) {
                return false;
            }
            Position pos = (Position) obj;
            if (this.contextStart == pos.contextStart && this.contextLimit == pos.contextLimit && this.start == pos.start && this.limit == pos.limit) {
                z = true;
            }
            return z;
        }

        @Deprecated
        public int hashCode() {
            return 42;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[cs=");
            stringBuilder.append(this.contextStart);
            stringBuilder.append(", s=");
            stringBuilder.append(this.start);
            stringBuilder.append(", l=");
            stringBuilder.append(this.limit);
            stringBuilder.append(", cl=");
            stringBuilder.append(this.contextLimit);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public final void validate(int length) {
            if (this.contextStart < 0 || this.start < this.contextStart || this.limit < this.start || this.contextLimit < this.limit || length < this.contextLimit) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid Position {cs=");
                stringBuilder.append(this.contextStart);
                stringBuilder.append(", s=");
                stringBuilder.append(this.start);
                stringBuilder.append(", l=");
                stringBuilder.append(this.limit);
                stringBuilder.append(", cl=");
                stringBuilder.append(this.contextLimit);
                stringBuilder.append("}, len=");
                stringBuilder.append(length);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    protected abstract void handleTransliterate(Replaceable replaceable, Position position, boolean z);

    protected Transliterator(String ID, UnicodeFilter filter) {
        if (ID != null) {
            this.ID = ID;
            setFilter(filter);
            return;
        }
        throw new NullPointerException();
    }

    public final int transliterate(Replaceable text, int start, int limit) {
        if (start < 0 || limit < start || text.length() < limit) {
            return -1;
        }
        Position pos = new Position(start, limit, start);
        filteredTransliterate(text, pos, false, true);
        return pos.limit;
    }

    public final void transliterate(Replaceable text) {
        transliterate(text, 0, text.length());
    }

    public final String transliterate(String text) {
        Replaceable result = new ReplaceableString(text);
        transliterate(result);
        return result.toString();
    }

    public final void transliterate(Replaceable text, Position index, String insertion) {
        index.validate(text.length());
        if (insertion != null) {
            text.replace(index.limit, index.limit, insertion);
            index.limit += insertion.length();
            index.contextLimit += insertion.length();
        }
        if (index.limit <= 0 || !UTF16.isLeadSurrogate(text.charAt(index.limit - 1))) {
            filteredTransliterate(text, index, true, true);
        }
    }

    public final void transliterate(Replaceable text, Position index, int insertion) {
        transliterate(text, index, UTF16.valueOf(insertion));
    }

    public final void transliterate(Replaceable text, Position index) {
        transliterate(text, index, null);
    }

    public final void finishTransliteration(Replaceable text, Position index) {
        index.validate(text.length());
        filteredTransliterate(text, index, false, true);
    }

    private void filteredTransliterate(Replaceable text, Position index, boolean incremental, boolean rollback) {
        Replaceable replaceable = text;
        Position position = index;
        if (this.filter != null || rollback) {
            int globalLimit = position.limit;
            StringBuffer log = null;
            while (true) {
                int char32At;
                int c;
                if (this.filter != null) {
                    UnicodeSet unicodeSet;
                    while (position.start < globalLimit) {
                        unicodeSet = this.filter;
                        char32At = replaceable.char32At(position.start);
                        c = char32At;
                        if (unicodeSet.contains(char32At)) {
                            break;
                        }
                        position.start += UTF16.getCharCount(c);
                    }
                    position.limit = position.start;
                    while (position.limit < globalLimit) {
                        unicodeSet = this.filter;
                        char32At = replaceable.char32At(position.limit);
                        c = char32At;
                        if (!unicodeSet.contains(char32At)) {
                            break;
                        }
                        position.limit += UTF16.getCharCount(c);
                    }
                }
                StringBuffer log2;
                if (position.start != position.limit) {
                    char32At = 0;
                    boolean isIncrementalRun = position.limit < globalLimit ? false : incremental;
                    int charLength;
                    if (rollback && isIncrementalRun) {
                        c = position.start;
                        int runLimit = position.limit;
                        int runLength = runLimit - c;
                        int rollbackOrigin = text.length();
                        replaceable.copy(c, runLimit, rollbackOrigin);
                        int passStart = c;
                        int rollbackStart = rollbackOrigin;
                        int passLimit = position.start;
                        int uncommittedLength = 0;
                        while (true) {
                            charLength = UTF16.getCharCount(replaceable.char32At(passLimit));
                            passLimit += charLength;
                            if (passLimit > runLimit) {
                                break;
                            }
                            int runLength2;
                            uncommittedLength += charLength;
                            position.limit = passLimit;
                            handleTransliterate(replaceable, position, 1);
                            charLength = position.limit - passLimit;
                            log2 = log;
                            int runStart = c;
                            if (position.start != position.limit) {
                                log = (rollbackStart + charLength) - (position.limit - passStart);
                                runLength2 = runLength;
                                replaceable.replace(passStart, position.limit, "");
                                replaceable.copy(log, log + uncommittedLength, passStart);
                                position.start = passStart;
                                position.limit = passLimit;
                                position.contextLimit -= charLength;
                            } else {
                                runLength2 = runLength;
                                log = position.start;
                                rollbackStart += charLength + uncommittedLength;
                                runLimit += charLength;
                                char32At += charLength;
                                passStart = log;
                                passLimit = log;
                                uncommittedLength = 0;
                            }
                            log = log2;
                            c = runStart;
                            runLength = runLength2;
                        }
                        rollbackOrigin += char32At;
                        int globalLimit2 = globalLimit + char32At;
                        replaceable.replace(rollbackOrigin, rollbackOrigin + runLength, "");
                        position.start = passStart;
                        log2 = log;
                        globalLimit = globalLimit2;
                    } else {
                        log2 = log;
                        charLength = position.limit;
                        handleTransliterate(replaceable, position, isIncrementalRun);
                        int delta = position.limit - charLength;
                        if (isIncrementalRun || position.start == position.limit) {
                            globalLimit += delta;
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("ERROR: Incomplete non-incremental transliteration by ");
                            stringBuilder.append(getID());
                            throw new RuntimeException(stringBuilder.toString());
                        }
                    }
                    if (this.filter == null || isIncrementalRun) {
                        break;
                    }
                    log = log2;
                } else {
                    log2 = log;
                    break;
                }
            }
            position.limit = globalLimit;
            return;
        }
        handleTransliterate(text, index, incremental);
    }

    public void filteredTransliterate(Replaceable text, Position index, boolean incremental) {
        filteredTransliterate(text, index, incremental, false);
    }

    public final int getMaximumContextLength() {
        return this.maximumContextLength;
    }

    protected void setMaximumContextLength(int a) {
        if (a >= 0) {
            this.maximumContextLength = a;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid context length ");
        stringBuilder.append(a);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public final String getID() {
        return this.ID;
    }

    protected final void setID(String id) {
        this.ID = id;
    }

    public static final String getDisplayName(String ID) {
        return getDisplayName(ID, ULocale.getDefault(Category.DISPLAY));
    }

    public static String getDisplayName(String id, Locale inLocale) {
        return getDisplayName(id, ULocale.forLocale(inLocale));
    }

    public static String getDisplayName(String id, ULocale inLocale) {
        ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_TRANSLIT_BASE_NAME, inLocale);
        String[] stv = TransliteratorIDParser.IDtoSTV(id);
        if (stv == null) {
            return "";
        }
        String ID = new StringBuilder();
        ID.append(stv[0]);
        ID.append(ID_SEP);
        int j = 1;
        ID.append(stv[1]);
        ID = ID.toString();
        if (stv[2] != null && stv[2].length() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ID);
            stringBuilder.append(VARIANT_SEP);
            stringBuilder.append(stv[2]);
            ID = stringBuilder.toString();
        }
        String n = (String) displayNameCache.get(new CaseInsensitiveString(ID));
        if (n != null) {
            return n;
        }
        try {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(RB_DISPLAY_NAME_PREFIX);
            stringBuilder2.append(ID);
            return bundle.getString(stringBuilder2.toString());
        } catch (MissingResourceException e) {
            try {
                String stringBuilder3;
                MessageFormat format = new MessageFormat(bundle.getString(RB_DISPLAY_NAME_PATTERN));
                Object[] args = new Object[]{Integer.valueOf(2), stv[0], stv[1]};
                while (j <= 2) {
                    try {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(RB_SCRIPT_DISPLAY_NAME_PREFIX);
                        stringBuilder4.append((String) args[j]);
                        args[j] = bundle.getString(stringBuilder4.toString());
                    } catch (MissingResourceException e2) {
                    }
                    j++;
                }
                if (stv[2].length() > 0) {
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(format.format(args));
                    stringBuilder5.append(VARIANT_SEP);
                    stringBuilder5.append(stv[2]);
                    stringBuilder3 = stringBuilder5.toString();
                } else {
                    stringBuilder3 = format.format(args);
                }
                return stringBuilder3;
            } catch (MissingResourceException e3) {
                throw new RuntimeException();
            }
        }
    }

    public final UnicodeFilter getFilter() {
        return this.filter;
    }

    public void setFilter(UnicodeFilter filter) {
        if (filter == null) {
            this.filter = null;
            return;
        }
        try {
            this.filter = new UnicodeSet((UnicodeSet) filter).freeze();
        } catch (Exception e) {
            this.filter = new UnicodeSet();
            filter.addMatchSetTo(this.filter);
            this.filter.freeze();
        }
    }

    public static final Transliterator getInstance(String ID) {
        return getInstance(ID, 0);
    }

    public static Transliterator getInstance(String ID, int dir) {
        StringBuffer canonID = new StringBuffer();
        List<SingleID> list = new ArrayList();
        UnicodeSet[] globalFilter = new UnicodeSet[1];
        if (TransliteratorIDParser.parseCompoundID(ID, dir, canonID, list, globalFilter)) {
            Transliterator t;
            List<Transliterator> translits = TransliteratorIDParser.instantiateList(list);
            if (list.size() > 1 || canonID.indexOf(";") >= 0) {
                t = new CompoundTransliterator(translits);
            } else {
                t = (Transliterator) translits.get(0);
            }
            t.setID(canonID.toString());
            if (globalFilter[0] != null) {
                t.setFilter(globalFilter[0]);
            }
            return t;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid ID ");
        stringBuilder.append(ID);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static Transliterator getBasicInstance(String id, String canonID) {
        StringBuffer s = new StringBuffer();
        Transliterator t = registry.get(id, s);
        if (s.length() != 0) {
            t = getInstance(s.toString(), 0);
        }
        if (!(t == null || canonID == null)) {
            t.setID(canonID);
        }
        return t;
    }

    public static final Transliterator createFromRules(String ID, String rules, int dir) {
        TransliteratorParser parser = new TransliteratorParser();
        parser.parse(rules, dir);
        if (parser.idBlockVector.size() == 0 && parser.dataVector.size() == 0) {
            return new NullTransliterator();
        }
        int i = 0;
        if (parser.idBlockVector.size() == 0 && parser.dataVector.size() == 1) {
            return new RuleBasedTransliterator(ID, (Data) parser.dataVector.get(0), parser.compoundFilter);
        }
        Transliterator t;
        if (parser.idBlockVector.size() == 1 && parser.dataVector.size() == 0) {
            if (parser.compoundFilter != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(parser.compoundFilter.toPattern(false));
                stringBuilder.append(";");
                stringBuilder.append((String) parser.idBlockVector.get(0));
                t = getInstance(stringBuilder.toString());
            } else {
                t = getInstance((String) parser.idBlockVector.get(0));
            }
            if (t == null) {
                return t;
            }
            t.setID(ID);
            return t;
        }
        List<Transliterator> transliterators = new ArrayList();
        int passNumber = 1;
        int limit = Math.max(parser.idBlockVector.size(), parser.dataVector.size());
        while (i < limit) {
            if (i < parser.idBlockVector.size()) {
                String idBlock = (String) parser.idBlockVector.get(i);
                if (idBlock.length() > 0 && !(getInstance(idBlock) instanceof NullTransliterator)) {
                    transliterators.add(getInstance(idBlock));
                }
            }
            if (i < parser.dataVector.size()) {
                Data data = (Data) parser.dataVector.get(i);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("%Pass");
                int passNumber2 = passNumber + 1;
                stringBuilder2.append(passNumber);
                transliterators.add(new RuleBasedTransliterator(stringBuilder2.toString(), data, null));
                passNumber = passNumber2;
            }
            i++;
        }
        t = new CompoundTransliterator(transliterators, passNumber - 1);
        t.setID(ID);
        if (parser.compoundFilter == null) {
            return t;
        }
        t.setFilter(parser.compoundFilter);
        return t;
    }

    public String toRules(boolean escapeUnprintable) {
        return baseToRules(escapeUnprintable);
    }

    protected final String baseToRules(boolean escapeUnprintable) {
        if (escapeUnprintable) {
            StringBuffer rulesSource = new StringBuffer();
            String id = getID();
            int i = 0;
            while (i < id.length()) {
                int c = UTF16.charAt(id, i);
                if (!Utility.escapeUnprintable(rulesSource, c)) {
                    UTF16.append(rulesSource, c);
                }
                i += UTF16.getCharCount(c);
            }
            rulesSource.insert(0, "::");
            rulesSource.append(ID_DELIM);
            return rulesSource.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("::");
        stringBuilder.append(getID());
        stringBuilder.append(ID_DELIM);
        return stringBuilder.toString();
    }

    public Transliterator[] getElements() {
        Transliterator[] result;
        int i = 0;
        if (this instanceof CompoundTransliterator) {
            CompoundTransliterator cpd = (CompoundTransliterator) this;
            result = new Transliterator[cpd.getCount()];
            while (i < result.length) {
                result[i] = cpd.getTransliterator(i);
                i++;
            }
        } else {
            result = new Transliterator[]{this};
        }
        return result;
    }

    public final UnicodeSet getSourceSet() {
        UnicodeSet result = new UnicodeSet();
        addSourceTargetSet(getFilterAsUnicodeSet(UnicodeSet.ALL_CODE_POINTS), result, new UnicodeSet());
        return result;
    }

    protected UnicodeSet handleGetSourceSet() {
        return new UnicodeSet();
    }

    public UnicodeSet getTargetSet() {
        UnicodeSet result = new UnicodeSet();
        addSourceTargetSet(getFilterAsUnicodeSet(UnicodeSet.ALL_CODE_POINTS), new UnicodeSet(), result);
        return result;
    }

    @Deprecated
    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        UnicodeSet temp = new UnicodeSet(handleGetSourceSet()).retainAll(getFilterAsUnicodeSet(inputFilter));
        sourceSet.addAll(temp);
        Iterator it = temp.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            CharSequence t = transliterate(s);
            if (!s.equals(t)) {
                targetSet.addAll(t);
            }
        }
    }

    @Deprecated
    public UnicodeSet getFilterAsUnicodeSet(UnicodeSet externalFilter) {
        if (this.filter == null) {
            return externalFilter;
        }
        UnicodeSet temp;
        UnicodeSet filterSet = new UnicodeSet(externalFilter);
        try {
            temp = this.filter;
        } catch (ClassCastException e) {
            UnicodeSet unicodeSet = this.filter;
            UnicodeSet unicodeSet2 = new UnicodeSet();
            UnicodeSet temp2 = unicodeSet2;
            unicodeSet.addMatchSetTo(unicodeSet2);
            temp = temp2;
        }
        return filterSet.retainAll(temp).freeze();
    }

    public final Transliterator getInverse() {
        return getInstance(this.ID, 1);
    }

    public static void registerClass(String ID, Class<? extends Transliterator> transClass, String displayName) {
        registry.put(ID, (Class) transClass, true);
        if (displayName != null) {
            displayNameCache.put(new CaseInsensitiveString(ID), displayName);
        }
    }

    public static void registerFactory(String ID, Factory factory) {
        registry.put(ID, factory, true);
    }

    public static void registerInstance(Transliterator trans) {
        registry.put(trans.getID(), trans, true);
    }

    static void registerInstance(Transliterator trans, boolean visible) {
        registry.put(trans.getID(), trans, visible);
    }

    public static void registerAlias(String aliasID, String realID) {
        registry.put(aliasID, realID, true);
    }

    static void registerSpecialInverse(String target, String inverseTarget, boolean bidirectional) {
        TransliteratorIDParser.registerSpecialInverse(target, inverseTarget, bidirectional);
    }

    public static void unregister(String ID) {
        displayNameCache.remove(new CaseInsensitiveString(ID));
        registry.remove(ID);
    }

    public static final Enumeration<String> getAvailableIDs() {
        return registry.getAvailableIDs();
    }

    public static final Enumeration<String> getAvailableSources() {
        return registry.getAvailableSources();
    }

    public static final Enumeration<String> getAvailableTargets(String source) {
        return registry.getAvailableTargets(source);
    }

    public static final Enumeration<String> getAvailableVariants(String source, String target) {
        return registry.getAvailableVariants(source, target);
    }

    static {
        UResourceBundle transIDs = UResourceBundle.getBundleInstance(ICUData.ICU_TRANSLIT_BASE_NAME, ROOT).get(RB_RULE_BASED_IDS);
        int maxRows = transIDs.getSize();
        for (int row = 0; row < maxRows; row++) {
            UResourceBundle colBund = transIDs.get(row);
            String ID = colBund.getKey();
            if (ID.indexOf("-t-") < 0) {
                UResourceBundle res = colBund.get(0);
                String type = res.getKey();
                if (type.equals("file") || type.equals("internal")) {
                    int dir;
                    String resString = res.getString("resource");
                    String direction = res.getString("direction");
                    char charAt = direction.charAt(0);
                    if (charAt == 'F') {
                        dir = 0;
                    } else if (charAt == 'R') {
                        dir = 1;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Can't parse direction: ");
                        stringBuilder.append(direction);
                        throw new RuntimeException(stringBuilder.toString());
                    }
                    registry.put(ID, resString, dir, 1 ^ type.equals("internal"));
                } else if (type.equals("alias")) {
                    registry.put(ID, res.getString(), true);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unknow type: ");
                    stringBuilder2.append(type);
                    throw new RuntimeException(stringBuilder2.toString());
                }
            }
        }
        registerSpecialInverse("Null", "Null", false);
        registerClass("Any-Null", NullTransliterator.class, null);
        RemoveTransliterator.register();
        EscapeTransliterator.register();
        UnescapeTransliterator.register();
        LowercaseTransliterator.register();
        UppercaseTransliterator.register();
        TitlecaseTransliterator.register();
        CaseFoldTransliterator.register();
        UnicodeNameTransliterator.register();
        NameUnicodeTransliterator.register();
        NormalizationTransliterator.register();
        BreakTransliterator.register();
        AnyTransliterator.register();
    }

    @Deprecated
    public static void registerAny() {
        AnyTransliterator.register();
    }

    public String transform(String source) {
        return transliterate(source);
    }
}
