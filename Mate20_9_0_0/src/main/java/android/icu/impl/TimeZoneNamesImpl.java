package android.icu.impl;

import android.icu.impl.TextTrieMap.ResultHandler;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.impl.UResource.Table;
import android.icu.impl.UResource.Value;
import android.icu.text.TimeZoneNames;
import android.icu.text.TimeZoneNames.MatchInfo;
import android.icu.text.TimeZoneNames.NameType;
import android.icu.util.TimeZone;
import android.icu.util.TimeZone.SystemTimeZoneType;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import libcore.icu.RelativeDateTimeFormatter;

public class TimeZoneNamesImpl extends TimeZoneNames {
    private static final Pattern LOC_EXCLUSION_PATTERN = Pattern.compile("Etc/.*|SystemV/.*|.*/Riyadh8[7-9]");
    private static volatile Set<String> METAZONE_IDS = null;
    private static final String MZ_PREFIX = "meta:";
    private static final MZ2TZsCache MZ_TO_TZS_CACHE = new MZ2TZsCache();
    private static final TZ2MZsCache TZ_TO_MZS_CACHE = new TZ2MZsCache();
    private static final String ZONE_STRINGS_BUNDLE = "zoneStrings";
    private static final long serialVersionUID = -2179814848495897472L;
    private transient ConcurrentHashMap<String, ZNames> _mzNamesMap;
    private transient boolean _namesFullyLoaded;
    private transient TextTrieMap<NameInfo> _namesTrie;
    private transient boolean _namesTrieFullyLoaded;
    private transient ConcurrentHashMap<String, ZNames> _tzNamesMap;
    private transient ICUResourceBundle _zoneStrings;

    private static class MZMapEntry {
        private long _from;
        private String _mzID;
        private long _to;

        MZMapEntry(String mzID, long from, long to) {
            this._mzID = mzID;
            this._from = from;
            this._to = to;
        }

        String mzID() {
            return this._mzID;
        }

        long from() {
            return this._from;
        }

        long to() {
            return this._to;
        }
    }

    private static class NameInfo {
        String mzID;
        NameType type;
        String tzID;

        private NameInfo() {
        }
    }

    private static class ZNames {
        static final ZNames EMPTY_ZNAMES = new ZNames(null);
        private static final int EX_LOC_INDEX = NameTypeIndex.EXEMPLAR_LOCATION.ordinal();
        public static final int NUM_NAME_TYPES = 7;
        private String[] _names;
        private boolean didAddIntoTrie;

        private enum NameTypeIndex {
            EXEMPLAR_LOCATION,
            LONG_GENERIC,
            LONG_STANDARD,
            LONG_DAYLIGHT,
            SHORT_GENERIC,
            SHORT_STANDARD,
            SHORT_DAYLIGHT;
            
            static final NameTypeIndex[] values = null;

            static {
                values = values();
            }
        }

        private static int getNameTypeIndex(NameType type) {
            switch (type) {
                case EXEMPLAR_LOCATION:
                    return NameTypeIndex.EXEMPLAR_LOCATION.ordinal();
                case LONG_GENERIC:
                    return NameTypeIndex.LONG_GENERIC.ordinal();
                case LONG_STANDARD:
                    return NameTypeIndex.LONG_STANDARD.ordinal();
                case LONG_DAYLIGHT:
                    return NameTypeIndex.LONG_DAYLIGHT.ordinal();
                case SHORT_GENERIC:
                    return NameTypeIndex.SHORT_GENERIC.ordinal();
                case SHORT_STANDARD:
                    return NameTypeIndex.SHORT_STANDARD.ordinal();
                case SHORT_DAYLIGHT:
                    return NameTypeIndex.SHORT_DAYLIGHT.ordinal();
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No NameTypeIndex match for ");
                    stringBuilder.append(type);
                    throw new AssertionError(stringBuilder.toString());
            }
        }

        private static NameType getNameType(int index) {
            switch (NameTypeIndex.values[index]) {
                case EXEMPLAR_LOCATION:
                    return NameType.EXEMPLAR_LOCATION;
                case LONG_GENERIC:
                    return NameType.LONG_GENERIC;
                case LONG_STANDARD:
                    return NameType.LONG_STANDARD;
                case LONG_DAYLIGHT:
                    return NameType.LONG_DAYLIGHT;
                case SHORT_GENERIC:
                    return NameType.SHORT_GENERIC;
                case SHORT_STANDARD:
                    return NameType.SHORT_STANDARD;
                case SHORT_DAYLIGHT:
                    return NameType.SHORT_DAYLIGHT;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No NameType match for ");
                    stringBuilder.append(index);
                    throw new AssertionError(stringBuilder.toString());
            }
        }

        protected ZNames(String[] names) {
            this._names = names;
            this.didAddIntoTrie = names == null;
        }

        public static ZNames createMetaZoneAndPutInCache(Map<String, ZNames> cache, String[] names, String mzID) {
            ZNames value;
            String key = mzID.intern();
            if (names == null) {
                value = EMPTY_ZNAMES;
            } else {
                value = new ZNames(names);
            }
            cache.put(key, value);
            return value;
        }

        public static ZNames createTimeZoneAndPutInCache(Map<String, ZNames> cache, String[] names, String tzID) {
            names = names == null ? new String[(EX_LOC_INDEX + 1)] : names;
            if (names[EX_LOC_INDEX] == null) {
                names[EX_LOC_INDEX] = TimeZoneNamesImpl.getDefaultExemplarLocationName(tzID);
            }
            String key = tzID.intern();
            ZNames value = new ZNames(names);
            cache.put(key, value);
            return value;
        }

        public String getName(NameType type) {
            int index = getNameTypeIndex(type);
            if (this._names == null || index >= this._names.length) {
                return null;
            }
            return this._names[index];
        }

        public void addAsMetaZoneIntoTrie(String mzID, TextTrieMap<NameInfo> trie) {
            addNamesIntoTrie(mzID, null, trie);
        }

        public void addAsTimeZoneIntoTrie(String tzID, TextTrieMap<NameInfo> trie) {
            addNamesIntoTrie(null, tzID, trie);
        }

        private void addNamesIntoTrie(String mzID, String tzID, TextTrieMap<NameInfo> trie) {
            if (this._names != null && !this.didAddIntoTrie) {
                this.didAddIntoTrie = true;
                for (int i = 0; i < this._names.length; i++) {
                    String name = this._names[i];
                    if (name != null) {
                        NameInfo info = new NameInfo();
                        info.mzID = mzID;
                        info.tzID = tzID;
                        info.type = getNameType(i);
                        trie.put(name, info);
                    }
                }
            }
        }
    }

    private static class NameSearchHandler implements ResultHandler<NameInfo> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private Collection<MatchInfo> _matches;
        private int _maxMatchLen;
        private EnumSet<NameType> _nameTypes;

        static {
            Class cls = TimeZoneNamesImpl.class;
        }

        NameSearchHandler(EnumSet<NameType> nameTypes) {
            this._nameTypes = nameTypes;
        }

        public boolean handlePrefixMatch(int matchLength, Iterator<NameInfo> values) {
            while (values.hasNext()) {
                NameInfo ninfo = (NameInfo) values.next();
                if (this._nameTypes == null || this._nameTypes.contains(ninfo.type)) {
                    MatchInfo minfo;
                    if (ninfo.tzID != null) {
                        minfo = new MatchInfo(ninfo.type, ninfo.tzID, null, matchLength);
                    } else {
                        minfo = new MatchInfo(ninfo.type, null, ninfo.mzID, matchLength);
                    }
                    if (this._matches == null) {
                        this._matches = new LinkedList();
                    }
                    this._matches.add(minfo);
                    if (matchLength > this._maxMatchLen) {
                        this._maxMatchLen = matchLength;
                    }
                }
            }
            return true;
        }

        public Collection<MatchInfo> getMatches() {
            if (this._matches == null) {
                return Collections.emptyList();
            }
            return this._matches;
        }

        public int getMaxMatchLen() {
            return this._maxMatchLen;
        }

        public void resetResults() {
            this._matches = null;
            this._maxMatchLen = 0;
        }
    }

    private static final class ZNamesLoader extends Sink {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static ZNamesLoader DUMMY_LOADER = new ZNamesLoader();
        private String[] names;

        static {
            Class cls = TimeZoneNamesImpl.class;
        }

        private ZNamesLoader() {
        }

        void loadMetaZone(ICUResourceBundle zoneStrings, String mzID) {
            String key = new StringBuilder();
            key.append(TimeZoneNamesImpl.MZ_PREFIX);
            key.append(mzID);
            loadNames(zoneStrings, key.toString());
        }

        void loadTimeZone(ICUResourceBundle zoneStrings, String tzID) {
            loadNames(zoneStrings, tzID.replace('/', ':'));
        }

        void loadNames(ICUResourceBundle zoneStrings, String key) {
            this.names = null;
            try {
                zoneStrings.getAllItemsWithFallback(key, this);
            } catch (MissingResourceException e) {
            }
        }

        private static NameTypeIndex nameTypeIndexFromKey(Key key) {
            NameTypeIndex nameTypeIndex = null;
            if (key.length() != 2) {
                return null;
            }
            char c0 = key.charAt(0);
            char c1 = key.charAt(1);
            if (c0 == 'l') {
                if (c1 == 'g') {
                    nameTypeIndex = NameTypeIndex.LONG_GENERIC;
                } else if (c1 == 's') {
                    nameTypeIndex = NameTypeIndex.LONG_STANDARD;
                } else if (c1 == 'd') {
                    nameTypeIndex = NameTypeIndex.LONG_DAYLIGHT;
                }
                return nameTypeIndex;
            } else if (c0 == 's') {
                if (c1 == 'g') {
                    nameTypeIndex = NameTypeIndex.SHORT_GENERIC;
                } else if (c1 == 's') {
                    nameTypeIndex = NameTypeIndex.SHORT_STANDARD;
                } else if (c1 == 'd') {
                    nameTypeIndex = NameTypeIndex.SHORT_DAYLIGHT;
                }
                return nameTypeIndex;
            } else if (c0 == 'e' && c1 == 'c') {
                return NameTypeIndex.EXEMPLAR_LOCATION;
            } else {
                return null;
            }
        }

        private void setNameIfEmpty(Key key, Value value) {
            if (this.names == null) {
                this.names = new String[7];
            }
            NameTypeIndex index = nameTypeIndexFromKey(key);
            if (index != null && this.names[index.ordinal()] == null) {
                this.names[index.ordinal()] = value.getString();
            }
        }

        public void put(Key key, Value value, boolean noFallback) {
            Table namesTable = value.getTable();
            for (int i = 0; namesTable.getKeyAndValue(i, key, value); i++) {
                setNameIfEmpty(key, value);
            }
        }

        private String[] getNames() {
            if (Utility.sameObjects(this.names, null)) {
                return null;
            }
            String[] result;
            int length = 0;
            for (int i = 0; i < 7; i++) {
                String name = this.names[i];
                if (name != null) {
                    if (name.equals(ICUResourceBundle.NO_INHERITANCE_MARKER)) {
                        this.names[i] = null;
                    } else {
                        length = i + 1;
                    }
                }
            }
            if (length == 7) {
                result = this.names;
            } else if (length == 0) {
                result = null;
            } else {
                result = (String[]) Arrays.copyOfRange(this.names, 0, length);
            }
            return result;
        }
    }

    private final class ZoneStringsLoader extends Sink {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final int INITIAL_NUM_ZONES = 300;
        private HashMap<Key, ZNamesLoader> keyToLoader;
        private StringBuilder sb;

        static {
            Class cls = TimeZoneNamesImpl.class;
        }

        private ZoneStringsLoader() {
            this.keyToLoader = new HashMap(300);
            this.sb = new StringBuilder(32);
        }

        void load() {
            TimeZoneNamesImpl.this._zoneStrings.getAllItemsWithFallback("", this);
            for (Entry<Key, ZNamesLoader> entry : this.keyToLoader.entrySet()) {
                ZNamesLoader loader = (ZNamesLoader) entry.getValue();
                if (loader != ZNamesLoader.DUMMY_LOADER) {
                    Key key = (Key) entry.getKey();
                    if (isMetaZone(key)) {
                        ZNames.createMetaZoneAndPutInCache(TimeZoneNamesImpl.this._mzNamesMap, loader.getNames(), mzIDFromKey(key));
                    } else {
                        ZNames.createTimeZoneAndPutInCache(TimeZoneNamesImpl.this._tzNamesMap, loader.getNames(), tzIDFromKey(key));
                    }
                }
            }
        }

        public void put(Key key, Value value, boolean noFallback) {
            Table timeZonesTable = value.getTable();
            for (int j = 0; timeZonesTable.getKeyAndValue(j, key, value); j++) {
                if (value.getType() == 2) {
                    consumeNamesTable(key, value, noFallback);
                }
            }
        }

        private void consumeNamesTable(Key key, Value value, boolean noFallback) {
            ZNamesLoader loader = (ZNamesLoader) this.keyToLoader.get(key);
            if (loader == null) {
                if (isMetaZone(key)) {
                    if (TimeZoneNamesImpl.this._mzNamesMap.containsKey(mzIDFromKey(key))) {
                        loader = ZNamesLoader.DUMMY_LOADER;
                    } else {
                        loader = new ZNamesLoader();
                    }
                } else {
                    if (TimeZoneNamesImpl.this._tzNamesMap.containsKey(tzIDFromKey(key))) {
                        loader = ZNamesLoader.DUMMY_LOADER;
                    } else {
                        loader = new ZNamesLoader();
                    }
                }
                this.keyToLoader.put(createKey(key), loader);
            }
            if (loader != ZNamesLoader.DUMMY_LOADER) {
                loader.put(key, value, noFallback);
            }
        }

        Key createKey(Key key) {
            return key.clone();
        }

        boolean isMetaZone(Key key) {
            return key.startsWith(TimeZoneNamesImpl.MZ_PREFIX);
        }

        private String mzIDFromKey(Key key) {
            this.sb.setLength(0);
            for (int i = TimeZoneNamesImpl.MZ_PREFIX.length(); i < key.length(); i++) {
                this.sb.append(key.charAt(i));
            }
            return this.sb.toString();
        }

        private String tzIDFromKey(Key key) {
            int i = 0;
            this.sb.setLength(0);
            while (true) {
                int i2 = i;
                if (i2 >= key.length()) {
                    return this.sb.toString();
                }
                char c = key.charAt(i2);
                if (c == ':') {
                    c = '/';
                }
                this.sb.append(c);
                i = i2 + 1;
            }
        }
    }

    private static class MZ2TZsCache extends SoftCache<String, Map<String, String>, String> {
        private MZ2TZsCache() {
        }

        protected Map<String, String> createInstance(String key, String data) {
            try {
                UResourceBundle regionMap = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "metaZones").get("mapTimezones").get(key);
                Set<String> regions = regionMap.keySet();
                HashMap map = new HashMap(regions.size());
                for (String region : regions) {
                    map.put(region.intern(), regionMap.getString(region).intern());
                }
                return map;
            } catch (MissingResourceException e) {
                return Collections.emptyMap();
            }
        }
    }

    private static class TZ2MZsCache extends SoftCache<String, List<MZMapEntry>, String> {
        private TZ2MZsCache() {
        }

        protected List<MZMapEntry> createInstance(String key, String data) {
            try {
                UResourceBundle zoneBundle = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "metaZones").get("metazoneInfo").get(data.replace('/', ':'));
                ArrayList mzMaps = new ArrayList(zoneBundle.getSize());
                int i = 0;
                int idx = 0;
                while (idx < zoneBundle.getSize()) {
                    UResourceBundle mz = zoneBundle.get(idx);
                    String mzid = mz.getString(i);
                    String toStr = "1970-01-01 00:00";
                    String toStr2 = "9999-12-31 23:59";
                    if (mz.getSize() == 3) {
                        toStr = mz.getString(1);
                        toStr2 = mz.getString(2);
                    }
                    String fromStr = toStr;
                    toStr = toStr2;
                    mzMaps.add(new MZMapEntry(mzid, parseDate(fromStr), parseDate(toStr)));
                    idx++;
                    i = 0;
                }
                return mzMaps;
            } catch (MissingResourceException e) {
                return Collections.emptyList();
            }
        }

        private static long parseDate(String text) {
            int idx;
            int n;
            int year = 0;
            int month = 0;
            int day = 0;
            int hour = 0;
            int min = 0;
            for (idx = 0; idx <= 3; idx++) {
                n = text.charAt(idx) - 48;
                if (n < 0 || n >= 10) {
                    throw new IllegalArgumentException("Bad year");
                }
                year = (10 * year) + n;
            }
            for (idx = 5; idx <= 6; idx++) {
                n = text.charAt(idx) - 48;
                if (n < 0 || n >= 10) {
                    throw new IllegalArgumentException("Bad month");
                }
                month = (10 * month) + n;
            }
            for (idx = 8; idx <= 9; idx++) {
                n = text.charAt(idx) - 48;
                if (n < 0 || n >= 10) {
                    throw new IllegalArgumentException("Bad day");
                }
                day = (10 * day) + n;
            }
            for (idx = 11; idx <= 12; idx++) {
                n = text.charAt(idx) - 48;
                if (n < 0 || n >= 10) {
                    throw new IllegalArgumentException("Bad hour");
                }
                hour = (10 * hour) + n;
            }
            for (idx = 14; idx <= 15; idx++) {
                n = text.charAt(idx) - 48;
                if (n < 0 || n >= 10) {
                    throw new IllegalArgumentException("Bad minute");
                }
                min = (10 * min) + n;
            }
            return ((Grego.fieldsToDay(year, month - 1, day) * 86400000) + (((long) hour) * RelativeDateTimeFormatter.HOUR_IN_MILLIS)) + (((long) min) * RelativeDateTimeFormatter.MINUTE_IN_MILLIS);
        }
    }

    public TimeZoneNamesImpl(ULocale locale) {
        initialize(locale);
    }

    public Set<String> getAvailableMetaZoneIDs() {
        return _getAvailableMetaZoneIDs();
    }

    static Set<String> _getAvailableMetaZoneIDs() {
        if (METAZONE_IDS == null) {
            synchronized (TimeZoneNamesImpl.class) {
                if (METAZONE_IDS == null) {
                    METAZONE_IDS = Collections.unmodifiableSet(UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "metaZones").get("mapTimezones").keySet());
                }
            }
        }
        return METAZONE_IDS;
    }

    public Set<String> getAvailableMetaZoneIDs(String tzID) {
        return _getAvailableMetaZoneIDs(tzID);
    }

    static Set<String> _getAvailableMetaZoneIDs(String tzID) {
        if (tzID == null || tzID.length() == 0) {
            return Collections.emptySet();
        }
        List<MZMapEntry> maps = (List) TZ_TO_MZS_CACHE.getInstance(tzID, tzID);
        if (maps.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> mzIDs = new HashSet(maps.size());
        for (MZMapEntry map : maps) {
            mzIDs.add(map.mzID());
        }
        return Collections.unmodifiableSet(mzIDs);
    }

    public String getMetaZoneID(String tzID, long date) {
        return _getMetaZoneID(tzID, date);
    }

    static String _getMetaZoneID(String tzID, long date) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        String mzID = null;
        for (MZMapEntry map : (List) TZ_TO_MZS_CACHE.getInstance(tzID, tzID)) {
            if (date >= map.from() && date < map.to()) {
                mzID = map.mzID();
                break;
            }
        }
        return mzID;
    }

    public String getReferenceZoneID(String mzID, String region) {
        return _getReferenceZoneID(mzID, region);
    }

    static String _getReferenceZoneID(String mzID, String region) {
        if (mzID == null || mzID.length() == 0) {
            return null;
        }
        String refID = null;
        Map<String, String> regionTzMap = (Map) MZ_TO_TZS_CACHE.getInstance(mzID, mzID);
        if (!regionTzMap.isEmpty()) {
            refID = (String) regionTzMap.get(region);
            if (refID == null) {
                refID = (String) regionTzMap.get("001");
            }
        }
        return refID;
    }

    public String getMetaZoneDisplayName(String mzID, NameType type) {
        if (mzID == null || mzID.length() == 0) {
            return null;
        }
        return loadMetaZoneNames(mzID).getName(type);
    }

    public String getTimeZoneDisplayName(String tzID, NameType type) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        return loadTimeZoneNames(tzID).getName(type);
    }

    public String getExemplarLocationName(String tzID) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        return loadTimeZoneNames(tzID).getName(NameType.EXEMPLAR_LOCATION);
    }

    public synchronized Collection<MatchInfo> find(CharSequence text, int start, EnumSet<NameType> nameTypes) {
        if (text != null) {
            if (text.length() != 0 && start >= 0 && start < text.length()) {
                NameSearchHandler handler = new NameSearchHandler(nameTypes);
                Collection<MatchInfo> matches = doFind(handler, text, start);
                if (matches != null) {
                    return matches;
                }
                addAllNamesIntoTrie();
                matches = doFind(handler, text, start);
                if (matches != null) {
                    return matches;
                }
                internalLoadAllDisplayNames();
                for (String tzID : TimeZone.getAvailableIDs(SystemTimeZoneType.CANONICAL, null, null)) {
                    if (!this._tzNamesMap.containsKey(tzID)) {
                        ZNames.createTimeZoneAndPutInCache(this._tzNamesMap, null, tzID);
                    }
                }
                addAllNamesIntoTrie();
                this._namesTrieFullyLoaded = true;
                return doFind(handler, text, start);
            }
        }
        throw new IllegalArgumentException("bad input text or range");
    }

    private Collection<MatchInfo> doFind(NameSearchHandler handler, CharSequence text, int start) {
        handler.resetResults();
        this._namesTrie.find(text, start, (ResultHandler) handler);
        if (handler.getMaxMatchLen() == text.length() - start || this._namesTrieFullyLoaded) {
            return handler.getMatches();
        }
        return null;
    }

    public synchronized void loadAllDisplayNames() {
        internalLoadAllDisplayNames();
    }

    public void getDisplayNames(String tzID, NameType[] types, long date, String[] dest, int destOffset) {
        if (tzID != null && tzID.length() != 0) {
            ZNames tzNames = loadTimeZoneNames(tzID);
            ZNames mzNames = null;
            for (int i = 0; i < types.length; i++) {
                NameType type = types[i];
                String name = tzNames.getName(type);
                if (name == null) {
                    if (mzNames == null) {
                        String mzID = getMetaZoneID(tzID, date);
                        if (mzID == null || mzID.length() == 0) {
                            mzNames = ZNames.EMPTY_ZNAMES;
                        } else {
                            mzNames = loadMetaZoneNames(mzID);
                        }
                    }
                    name = mzNames.getName(type);
                }
                dest[destOffset + i] = name;
            }
        }
    }

    private void internalLoadAllDisplayNames() {
        if (!this._namesFullyLoaded) {
            this._namesFullyLoaded = true;
            new ZoneStringsLoader().load();
        }
    }

    private void addAllNamesIntoTrie() {
        for (Entry<String, ZNames> entry : this._tzNamesMap.entrySet()) {
            ((ZNames) entry.getValue()).addAsTimeZoneIntoTrie((String) entry.getKey(), this._namesTrie);
        }
        for (Entry<String, ZNames> entry2 : this._mzNamesMap.entrySet()) {
            ((ZNames) entry2.getValue()).addAsMetaZoneIntoTrie((String) entry2.getKey(), this._namesTrie);
        }
    }

    private void initialize(ULocale locale) {
        this._zoneStrings = (ICUResourceBundle) ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_ZONE_BASE_NAME, locale)).get(ZONE_STRINGS_BUNDLE);
        this._tzNamesMap = new ConcurrentHashMap();
        this._mzNamesMap = new ConcurrentHashMap();
        this._namesFullyLoaded = false;
        this._namesTrie = new TextTrieMap(true);
        this._namesTrieFullyLoaded = false;
        String tzCanonicalID = ZoneMeta.getCanonicalCLDRID(TimeZone.getDefault());
        if (tzCanonicalID != null) {
            loadStrings(tzCanonicalID);
        }
    }

    /* JADX WARNING: Missing block: B:17:0x002b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void loadStrings(String tzCanonicalID) {
        if (tzCanonicalID != null) {
            if (tzCanonicalID.length() != 0) {
                loadTimeZoneNames(tzCanonicalID);
                for (String mzID : getAvailableMetaZoneIDs(tzCanonicalID)) {
                    loadMetaZoneNames(mzID);
                }
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(this._zoneStrings.getULocale());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        initialize((ULocale) in.readObject());
    }

    private synchronized ZNames loadMetaZoneNames(String mzID) {
        ZNames mznames;
        mznames = (ZNames) this._mzNamesMap.get(mzID);
        if (mznames == null) {
            ZNamesLoader loader = new ZNamesLoader();
            loader.loadMetaZone(this._zoneStrings, mzID);
            mznames = ZNames.createMetaZoneAndPutInCache(this._mzNamesMap, loader.getNames(), mzID);
        }
        return mznames;
    }

    private synchronized ZNames loadTimeZoneNames(String tzID) {
        ZNames tznames;
        tznames = (ZNames) this._tzNamesMap.get(tzID);
        if (tznames == null) {
            ZNamesLoader loader = new ZNamesLoader();
            loader.loadTimeZone(this._zoneStrings, tzID);
            tznames = ZNames.createTimeZoneAndPutInCache(this._tzNamesMap, loader.getNames(), tzID);
        }
        return tznames;
    }

    public static String getDefaultExemplarLocationName(String tzID) {
        if (tzID == null || tzID.length() == 0 || LOC_EXCLUSION_PATTERN.matcher(tzID).matches()) {
            return null;
        }
        String location = null;
        int sep = tzID.lastIndexOf(47);
        if (sep > 0 && sep + 1 < tzID.length()) {
            location = tzID.substring(sep + 1).replace('_', ' ');
        }
        return location;
    }
}
