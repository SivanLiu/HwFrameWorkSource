package android.icu.impl;

import android.icu.impl.locale.AsciiUtil;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ValidIdentifiers {

    public enum Datasubtype {
        deprecated,
        private_use,
        regular,
        special,
        unknown,
        macroregion
    }

    public enum Datatype {
        currency,
        language,
        region,
        script,
        subdivision,
        unit,
        variant,
        u,
        t,
        x,
        illegal
    }

    private static class ValidityData {
        static final Map<Datatype, Map<Datasubtype, ValiditySet>> data;

        private ValidityData() {
        }

        static {
            Map<Datatype, Map<Datasubtype, ValiditySet>> _data = new EnumMap(Datatype.class);
            UResourceBundle suppData = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundleIterator datatypeIterator = suppData.get("idValidity").getIterator();
            while (datatypeIterator.hasNext()) {
                UResourceBundle suppData2;
                UResourceBundle datatype = datatypeIterator.next();
                Datatype key = Datatype.valueOf(datatype.getKey());
                Map<Datasubtype, ValiditySet> values = new EnumMap(Datasubtype.class);
                UResourceBundleIterator datasubtypeIterator = datatype.getIterator();
                while (datasubtypeIterator.hasNext()) {
                    UResourceBundle datasubtype = datasubtypeIterator.next();
                    Datasubtype subkey = Datasubtype.valueOf(datasubtype.getKey());
                    Set<String> subvalues = new HashSet();
                    if (datasubtype.getType() == 0) {
                        addRange(datasubtype.getString(), subvalues);
                        suppData2 = suppData;
                    } else {
                        String[] stringArray = datasubtype.getStringArray();
                        int length = stringArray.length;
                        int i = 0;
                        while (i < length) {
                            suppData2 = suppData;
                            addRange(stringArray[i], subvalues);
                            i++;
                            suppData = suppData2;
                        }
                        suppData2 = suppData;
                    }
                    values.put(subkey, new ValiditySet(subvalues, key == Datatype.subdivision));
                    suppData = suppData2;
                }
                suppData2 = suppData;
                _data.put(key, Collections.unmodifiableMap(values));
                suppData = suppData2;
            }
            data = Collections.unmodifiableMap(_data);
        }

        private static void addRange(String string, Set<String> subvalues) {
            string = AsciiUtil.toLowerString(string);
            int pos = string.indexOf(126);
            if (pos < 0) {
                subvalues.add(string);
            } else {
                StringRange.expand(string.substring(0, pos), string.substring(pos + 1), false, subvalues);
            }
        }
    }

    public static class ValiditySet {
        public final Set<String> regularData;
        public final Map<String, Set<String>> subdivisionData;

        public ValiditySet(Set<String> plainData, boolean makeMap) {
            if (makeMap) {
                HashMap<String, Set<String>> _subdivisionData = new HashMap();
                for (String s : plainData) {
                    int pos = s.indexOf(45);
                    int pos2 = pos + 1;
                    if (pos < 0) {
                        int i = s.charAt(0) < 'A' ? 3 : 2;
                        pos = i;
                        pos2 = i;
                    }
                    String key = s.substring(0, pos);
                    String subdivision = s.substring(pos2);
                    Set<String> oldSet = (Set) _subdivisionData.get(key);
                    if (oldSet == null) {
                        HashSet hashSet = new HashSet();
                        oldSet = hashSet;
                        _subdivisionData.put(key, hashSet);
                    }
                    oldSet.add(subdivision);
                }
                this.regularData = null;
                HashMap<String, Set<String>> _subdivisionData2 = new HashMap();
                for (Entry<String, Set<String>> e : _subdivisionData.entrySet()) {
                    Set<String> set;
                    Set<String> value = (Set) e.getValue();
                    if (value.size() == 1) {
                        set = Collections.singleton((String) value.iterator().next());
                    } else {
                        set = Collections.unmodifiableSet(value);
                    }
                    _subdivisionData2.put((String) e.getKey(), set);
                }
                this.subdivisionData = Collections.unmodifiableMap(_subdivisionData2);
                return;
            }
            this.regularData = Collections.unmodifiableSet(plainData);
            this.subdivisionData = null;
        }

        public boolean contains(String code) {
            if (this.regularData != null) {
                return this.regularData.contains(code);
            }
            int pos = code.indexOf(45);
            return contains(code.substring(null, pos), code.substring(pos + 1));
        }

        public boolean contains(String key, String value) {
            Set<String> oldSet = (Set) this.subdivisionData.get(key);
            return oldSet != null && oldSet.contains(value);
        }

        public String toString() {
            if (this.regularData != null) {
                return this.regularData.toString();
            }
            return this.subdivisionData.toString();
        }
    }

    public static Map<Datatype, Map<Datasubtype, ValiditySet>> getData() {
        return ValidityData.data;
    }

    public static Datasubtype isValid(Datatype datatype, Set<Datasubtype> datasubtypes, String code) {
        Map<Datasubtype, ValiditySet> subtable = (Map) ValidityData.data.get(datatype);
        if (subtable != null) {
            for (Datasubtype datasubtype : datasubtypes) {
                ValiditySet validitySet = (ValiditySet) subtable.get(datasubtype);
                if (validitySet != null && validitySet.contains(AsciiUtil.toLowerString(code))) {
                    return datasubtype;
                }
            }
        }
        return null;
    }

    public static Datasubtype isValid(Datatype datatype, Set<Datasubtype> datasubtypes, String code, String value) {
        Map<Datasubtype, ValiditySet> subtable = (Map) ValidityData.data.get(datatype);
        if (subtable != null) {
            code = AsciiUtil.toLowerString(code);
            value = AsciiUtil.toLowerString(value);
            for (Datasubtype datasubtype : datasubtypes) {
                ValiditySet validitySet = (ValiditySet) subtable.get(datasubtype);
                if (validitySet != null && validitySet.contains(code, value)) {
                    return datasubtype;
                }
            }
        }
        return null;
    }
}
