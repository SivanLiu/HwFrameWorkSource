package android.icu.impl.locale;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.util.Output;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.regex.Pattern;

public class KeyTypeData {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static Map<String, Set<String>> BCP47_KEYS;
    static Set<String> DEPRECATED_KEYS = Collections.emptySet();
    static Map<String, Set<String>> DEPRECATED_KEY_TYPES = Collections.emptyMap();
    private static final Map<String, KeyData> KEYMAP = new HashMap();
    private static final Object[][] KEY_DATA = new Object[0][];
    static Map<String, ValueType> VALUE_TYPES = Collections.emptyMap();

    /* renamed from: android.icu.impl.locale.KeyTypeData$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$icu$impl$locale$KeyTypeData$TypeInfoType = new int[TypeInfoType.values().length];

        static {
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$TypeInfoType[TypeInfoType.deprecated.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            $SwitchMap$android$icu$impl$locale$KeyTypeData$KeyInfoType = new int[KeyInfoType.values().length];
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$KeyInfoType[KeyInfoType.deprecated.ordinal()] = 1;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$KeyInfoType[KeyInfoType.valueType.ordinal()] = 2;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private static class KeyData {
        String bcpId;
        String legacyId;
        EnumSet<SpecialType> specialTypes;
        Map<String, Type> typeMap;

        KeyData(String legacyId, String bcpId, Map<String, Type> typeMap, EnumSet<SpecialType> specialTypes) {
            this.legacyId = legacyId;
            this.bcpId = bcpId;
            this.typeMap = typeMap;
            this.specialTypes = specialTypes;
        }
    }

    private enum KeyInfoType {
        deprecated,
        valueType
    }

    private enum SpecialType {
        CODEPOINTS(new CodepointsTypeHandler()),
        REORDER_CODE(new ReorderCodeTypeHandler()),
        RG_KEY_VALUE(new RgKeyValueTypeHandler()),
        SUBDIVISION_CODE(new SubdivisionKeyValueTypeHandler()),
        PRIVATE_USE(new PrivateUseKeyValueTypeHandler());
        
        SpecialTypeHandler handler;

        private SpecialType(SpecialTypeHandler handler) {
            this.handler = handler;
        }
    }

    private static abstract class SpecialTypeHandler {
        abstract boolean isWellFormed(String str);

        private SpecialTypeHandler() {
        }

        /* synthetic */ SpecialTypeHandler(AnonymousClass1 x0) {
            this();
        }

        String canonicalize(String value) {
            return AsciiUtil.toLowerString(value);
        }
    }

    private static class Type {
        String bcpId;
        String legacyId;

        Type(String legacyId, String bcpId) {
            this.legacyId = legacyId;
            this.bcpId = bcpId;
        }
    }

    private enum TypeInfoType {
        deprecated
    }

    public enum ValueType {
        single,
        multiple,
        incremental,
        any
    }

    private static class CodepointsTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("[0-9a-fA-F]{4,6}(-[0-9a-fA-F]{4,6})*");

        private CodepointsTypeHandler() {
            super();
        }

        /* synthetic */ CodepointsTypeHandler(AnonymousClass1 x0) {
            this();
        }

        boolean isWellFormed(String value) {
            return pat.matcher(value).matches();
        }
    }

    private static class PrivateUseKeyValueTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("[a-zA-Z0-9]{3,8}(-[a-zA-Z0-9]{3,8})*");

        private PrivateUseKeyValueTypeHandler() {
            super();
        }

        /* synthetic */ PrivateUseKeyValueTypeHandler(AnonymousClass1 x0) {
            this();
        }

        boolean isWellFormed(String value) {
            return pat.matcher(value).matches();
        }
    }

    private static class ReorderCodeTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("[a-zA-Z]{3,8}(-[a-zA-Z]{3,8})*");

        private ReorderCodeTypeHandler() {
            super();
        }

        /* synthetic */ ReorderCodeTypeHandler(AnonymousClass1 x0) {
            this();
        }

        boolean isWellFormed(String value) {
            return pat.matcher(value).matches();
        }
    }

    private static class RgKeyValueTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("([a-zA-Z]{2}|[0-9]{3})[zZ]{4}");

        private RgKeyValueTypeHandler() {
            super();
        }

        /* synthetic */ RgKeyValueTypeHandler(AnonymousClass1 x0) {
            this();
        }

        boolean isWellFormed(String value) {
            return pat.matcher(value).matches();
        }
    }

    private static class SubdivisionKeyValueTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("([a-zA-Z]{2}|[0-9]{3})");

        private SubdivisionKeyValueTypeHandler() {
            super();
        }

        /* synthetic */ SubdivisionKeyValueTypeHandler(AnonymousClass1 x0) {
            this();
        }

        boolean isWellFormed(String value) {
            return pat.matcher(value).matches();
        }
    }

    static {
        initFromResourceBundle();
    }

    public static String toBcpKey(String key) {
        KeyData keyData = (KeyData) KEYMAP.get(AsciiUtil.toLowerString(key));
        if (keyData != null) {
            return keyData.bcpId;
        }
        return null;
    }

    public static String toLegacyKey(String key) {
        KeyData keyData = (KeyData) KEYMAP.get(AsciiUtil.toLowerString(key));
        if (keyData != null) {
            return keyData.legacyId;
        }
        return null;
    }

    public static String toBcpType(String key, String type, Output<Boolean> isKnownKey, Output<Boolean> isSpecialType) {
        if (isKnownKey != null) {
            isKnownKey.value = Boolean.valueOf(false);
        }
        if (isSpecialType != null) {
            isSpecialType.value = Boolean.valueOf(false);
        }
        key = AsciiUtil.toLowerString(key);
        type = AsciiUtil.toLowerString(type);
        KeyData keyData = (KeyData) KEYMAP.get(key);
        if (keyData != null) {
            if (isKnownKey != null) {
                isKnownKey.value = Boolean.TRUE;
            }
            Type t = (Type) keyData.typeMap.get(type);
            if (t != null) {
                return t.bcpId;
            }
            if (keyData.specialTypes != null) {
                Iterator it = keyData.specialTypes.iterator();
                while (it.hasNext()) {
                    SpecialType st = (SpecialType) it.next();
                    if (st.handler.isWellFormed(type)) {
                        if (isSpecialType != null) {
                            isSpecialType.value = Boolean.valueOf(true);
                        }
                        return st.handler.canonicalize(type);
                    }
                }
            }
        }
        return null;
    }

    public static String toLegacyType(String key, String type, Output<Boolean> isKnownKey, Output<Boolean> isSpecialType) {
        if (isKnownKey != null) {
            isKnownKey.value = Boolean.valueOf(false);
        }
        if (isSpecialType != null) {
            isSpecialType.value = Boolean.valueOf(false);
        }
        key = AsciiUtil.toLowerString(key);
        type = AsciiUtil.toLowerString(type);
        KeyData keyData = (KeyData) KEYMAP.get(key);
        if (keyData != null) {
            if (isKnownKey != null) {
                isKnownKey.value = Boolean.TRUE;
            }
            Type t = (Type) keyData.typeMap.get(type);
            if (t != null) {
                return t.legacyId;
            }
            if (keyData.specialTypes != null) {
                Iterator it = keyData.specialTypes.iterator();
                while (it.hasNext()) {
                    SpecialType st = (SpecialType) it.next();
                    if (st.handler.isWellFormed(type)) {
                        if (isSpecialType != null) {
                            isSpecialType.value = Boolean.valueOf(true);
                        }
                        return st.handler.canonicalize(type);
                    }
                }
            }
        }
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:95:0x0218  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void initFromResourceBundle() {
        UResourceBundle keyTypeDataRes = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "keyTypeData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        getKeyInfo(keyTypeDataRes.get("keyInfo"));
        getTypeInfo(keyTypeDataRes.get("typeInfo"));
        UResourceBundle keyMapRes = keyTypeDataRes.get("keyMap");
        UResourceBundle typeMapRes = keyTypeDataRes.get("typeMap");
        UResourceBundle typeAliasRes = null;
        UResourceBundle bcpTypeAliasRes = null;
        try {
            typeAliasRes = keyTypeDataRes.get("typeAlias");
        } catch (MissingResourceException e) {
        }
        try {
            bcpTypeAliasRes = keyTypeDataRes.get("bcpTypeAlias");
        } catch (MissingResourceException e2) {
        }
        UResourceBundleIterator keyMapItr = keyMapRes.getIterator();
        Map<String, Set<String>> _Bcp47Keys = new LinkedHashMap();
        while (true) {
            Map<String, Set<String>> _Bcp47Keys2 = _Bcp47Keys;
            UResourceBundle keyMapRes2;
            UResourceBundle typeAliasRes2;
            UResourceBundle typeMapRes2;
            UResourceBundle bcpTypeAliasRes2;
            if (keyMapItr.hasNext()) {
                UResourceBundle typeAliasResByKey;
                UResourceBundleIterator typeAliasResItr;
                UResourceBundle keyTypeDataRes2;
                String from;
                UResourceBundle keyMapEntry = keyMapItr.next();
                String legacyKeyId = keyMapEntry.getKey();
                String bcpKeyId = keyMapEntry.getString();
                String bcpKeyId2 = null;
                if (bcpKeyId.length() == 0) {
                    bcpKeyId = legacyKeyId;
                    bcpKeyId2 = true;
                }
                boolean hasSameKey = bcpKeyId2;
                bcpKeyId2 = bcpKeyId;
                LinkedHashSet<String> _bcp47Types = new LinkedHashSet();
                _Bcp47Keys2.put(bcpKeyId2, Collections.unmodifiableSet(_bcp47Types));
                boolean isTZ = legacyKeyId.equals("timezone");
                Map<String, Set<String>> typeAliasMap = null;
                if (typeAliasRes != null) {
                    UResourceBundle typeAliasResByKey2 = null;
                    try {
                        typeAliasResByKey = typeAliasRes.get(legacyKeyId);
                    } catch (MissingResourceException e3) {
                        typeAliasResByKey = typeAliasResByKey2;
                    }
                    if (typeAliasResByKey != null) {
                        typeAliasMap = new HashMap();
                        typeAliasResItr = typeAliasResByKey.getIterator();
                        while (typeAliasResItr.hasNext()) {
                            UResourceBundleIterator typeAliasResItr2;
                            UResourceBundle typeAliasResByKey3 = typeAliasResByKey;
                            typeAliasResByKey = typeAliasResItr.next();
                            keyTypeDataRes2 = keyTypeDataRes;
                            keyTypeDataRes = typeAliasResByKey.getKey();
                            keyMapRes2 = keyMapRes;
                            String to = typeAliasResByKey.getString();
                            if (isTZ) {
                                typeAliasRes2 = typeAliasRes;
                                typeAliasResItr2 = typeAliasResItr;
                                keyTypeDataRes = keyTypeDataRes.replace(':', '/');
                            } else {
                                typeAliasRes2 = typeAliasRes;
                                typeAliasResItr2 = typeAliasResItr;
                            }
                            Set<String> aliasSet = (Set) typeAliasMap.get(to);
                            if (aliasSet == null) {
                                aliasSet = new HashSet();
                                typeAliasMap.put(to, aliasSet);
                            }
                            aliasSet.add(keyTypeDataRes);
                            typeAliasResByKey = typeAliasResByKey3;
                            keyTypeDataRes = keyTypeDataRes2;
                            keyMapRes = keyMapRes2;
                            typeAliasRes = typeAliasRes2;
                            typeAliasResItr = typeAliasResItr2;
                        }
                    }
                }
                keyTypeDataRes2 = keyTypeDataRes;
                keyMapRes2 = keyMapRes;
                typeAliasRes2 = typeAliasRes;
                keyTypeDataRes = null;
                if (bcpTypeAliasRes != null) {
                    typeAliasRes = null;
                    try {
                        typeAliasRes = bcpTypeAliasRes.get(bcpKeyId2);
                    } catch (MissingResourceException e4) {
                    }
                    if (typeAliasRes != null) {
                        keyTypeDataRes = new HashMap();
                        UResourceBundleIterator bcpTypeAliasResItr = typeAliasRes.getIterator();
                        while (bcpTypeAliasResItr.hasNext()) {
                            Set<String> aliasSet2;
                            keyMapRes = bcpTypeAliasResItr.next();
                            from = keyMapRes.getKey();
                            UResourceBundleIterator bcpTypeAliasResItr2 = bcpTypeAliasResItr;
                            bcpKeyId = keyMapRes.getString();
                            Set<String> aliasSet3 = (Set) keyTypeDataRes.get(bcpKeyId);
                            if (aliasSet3 == null) {
                                aliasSet2 = new HashSet();
                                keyTypeDataRes.put(bcpKeyId, aliasSet2);
                            } else {
                                aliasSet2 = aliasSet3;
                            }
                            aliasSet2.add(from);
                            bcpTypeAliasResItr = bcpTypeAliasResItr2;
                        }
                    }
                }
                HashMap typeDataMap = new HashMap();
                EnumSet<SpecialType> specialTypeSet = null;
                UResourceBundle typeMapResByKey = null;
                try {
                    typeAliasResByKey = typeMapRes.get(legacyKeyId);
                } catch (MissingResourceException e5) {
                    MissingResourceException missingResourceException = e5;
                    typeAliasResByKey = typeMapResByKey;
                }
                if (typeAliasResByKey != null) {
                    EnumSet<SpecialType> specialTypeSet2;
                    UResourceBundleIterator typeMapResByKeyItr = typeAliasResByKey.getIterator();
                    while (true) {
                        typeAliasResItr = typeMapResByKeyItr;
                        if (!typeAliasResItr.hasNext()) {
                            break;
                        }
                        UResourceBundle typeMapResByKey2 = typeAliasResByKey;
                        typeAliasResByKey = typeAliasResItr.next();
                        typeMapRes2 = typeMapRes;
                        String legacyTypeId = typeAliasResByKey.getKey();
                        UResourceBundleIterator typeMapResByKeyItr2 = typeAliasResItr;
                        from = typeAliasResByKey.getString();
                        UResourceBundle typeMapEntry = typeAliasResByKey;
                        bcpTypeAliasRes2 = bcpTypeAliasRes;
                        char first = legacyTypeId.charAt(null);
                        typeAliasResByKey = ('9' >= first || first >= 'a' || from.length() != null) ? null : true;
                        if (typeAliasResByKey != null) {
                            boolean isSpecialType;
                            if (specialTypeSet == null) {
                                isSpecialType = typeAliasResByKey;
                                specialTypeSet = EnumSet.noneOf(SpecialType.class);
                            } else {
                                isSpecialType = typeAliasResByKey;
                            }
                            specialTypeSet.add(SpecialType.valueOf(legacyTypeId));
                            _bcp47Types.add(legacyTypeId);
                            typeAliasResByKey = typeMapResByKey2;
                            typeMapRes = typeMapRes2;
                            typeMapResByKeyItr = typeMapResByKeyItr2;
                            bcpTypeAliasRes = bcpTypeAliasRes2;
                        } else {
                            boolean hasSameType;
                            String legacyTypeId2;
                            UResourceBundle uResourceBundle = typeAliasResByKey;
                            if (isTZ) {
                                specialTypeSet2 = specialTypeSet;
                                char c = first;
                                legacyTypeId = legacyTypeId.replace(':', '/');
                            } else {
                                specialTypeSet2 = specialTypeSet;
                            }
                            typeAliasResByKey = null;
                            if (from.length() == 0) {
                                from = legacyTypeId;
                                typeAliasResByKey = true;
                            }
                            _bcp47Types.add(from);
                            Type t = new Type(legacyTypeId, from);
                            typeDataMap.put(AsciiUtil.toLowerString(legacyTypeId), t);
                            if (typeAliasResByKey == null) {
                                typeDataMap.put(AsciiUtil.toLowerString(from), t);
                            }
                            if (typeAliasMap != null) {
                                Set<String> typeAliasSet = (Set) typeAliasMap.get(legacyTypeId);
                                if (typeAliasSet != null) {
                                    hasSameType = typeAliasResByKey;
                                    typeAliasResByKey = typeAliasSet.iterator();
                                    while (typeAliasResByKey.hasNext()) {
                                        UResourceBundle uResourceBundle2 = typeAliasResByKey;
                                        legacyTypeId2 = legacyTypeId;
                                        typeDataMap.put(AsciiUtil.toLowerString((String) typeAliasResByKey.next()), t);
                                        typeAliasResByKey = uResourceBundle2;
                                        legacyTypeId = legacyTypeId2;
                                    }
                                    if (keyTypeDataRes != null) {
                                        UResourceBundle<String> typeMapResByKey3 = (Set) keyTypeDataRes.get(from);
                                        if (typeMapResByKey3 != null) {
                                            for (String alias : typeMapResByKey3) {
                                                UResourceBundle bcpTypeAliasSet = typeAliasResByKey;
                                                typeDataMap.put(AsciiUtil.toLowerString(alias), t);
                                                typeAliasResByKey = bcpTypeAliasSet;
                                            }
                                        }
                                    }
                                    typeAliasResByKey = typeMapResByKey2;
                                    typeMapRes = typeMapRes2;
                                    typeMapResByKeyItr = typeMapResByKeyItr2;
                                    bcpTypeAliasRes = bcpTypeAliasRes2;
                                    specialTypeSet = specialTypeSet2;
                                }
                            }
                            hasSameType = typeAliasResByKey;
                            legacyTypeId2 = legacyTypeId;
                            if (keyTypeDataRes != null) {
                            }
                            typeAliasResByKey = typeMapResByKey2;
                            typeMapRes = typeMapRes2;
                            typeMapResByKeyItr = typeMapResByKeyItr2;
                            bcpTypeAliasRes = bcpTypeAliasRes2;
                            specialTypeSet = specialTypeSet2;
                        }
                    }
                    typeMapRes2 = typeMapRes;
                    specialTypeSet2 = specialTypeSet;
                    bcpTypeAliasRes2 = bcpTypeAliasRes;
                } else {
                    typeMapRes2 = typeMapRes;
                    bcpTypeAliasRes2 = bcpTypeAliasRes;
                }
                KeyData keyData = new KeyData(legacyKeyId, bcpKeyId2, typeDataMap, specialTypeSet);
                KEYMAP.put(AsciiUtil.toLowerString(legacyKeyId), keyData);
                if (!hasSameKey) {
                    KEYMAP.put(AsciiUtil.toLowerString(bcpKeyId2), keyData);
                }
                _Bcp47Keys = _Bcp47Keys2;
                keyTypeDataRes = keyTypeDataRes2;
                keyMapRes = keyMapRes2;
                typeAliasRes = typeAliasRes2;
                typeMapRes = typeMapRes2;
                bcpTypeAliasRes = bcpTypeAliasRes2;
            } else {
                keyMapRes2 = keyMapRes;
                typeMapRes2 = typeMapRes;
                typeAliasRes2 = typeAliasRes;
                bcpTypeAliasRes2 = bcpTypeAliasRes;
                BCP47_KEYS = Collections.unmodifiableMap(_Bcp47Keys2);
                return;
            }
        }
    }

    private static void getKeyInfo(UResourceBundle keyInfoRes) {
        Set<String> _deprecatedKeys = new LinkedHashSet();
        Map<String, ValueType> _valueTypes = new LinkedHashMap();
        UResourceBundleIterator keyInfoIt = keyInfoRes.getIterator();
        while (keyInfoIt.hasNext()) {
            UResourceBundle keyInfoEntry = keyInfoIt.next();
            KeyInfoType keyInfo = KeyInfoType.valueOf(keyInfoEntry.getKey());
            UResourceBundleIterator keyInfoIt2 = keyInfoEntry.getIterator();
            while (keyInfoIt2.hasNext()) {
                UResourceBundle keyInfoEntry2 = keyInfoIt2.next();
                String key2 = keyInfoEntry2.getKey();
                String value2 = keyInfoEntry2.getString();
                switch (keyInfo) {
                    case deprecated:
                        _deprecatedKeys.add(key2);
                        break;
                    case valueType:
                        _valueTypes.put(key2, ValueType.valueOf(value2));
                        break;
                    default:
                        break;
                }
            }
        }
        DEPRECATED_KEYS = Collections.unmodifiableSet(_deprecatedKeys);
        VALUE_TYPES = Collections.unmodifiableMap(_valueTypes);
    }

    private static void getTypeInfo(UResourceBundle typeInfoRes) {
        Map<String, Set<String>> _deprecatedKeyTypes = new LinkedHashMap();
        UResourceBundleIterator keyInfoIt = typeInfoRes.getIterator();
        while (keyInfoIt.hasNext()) {
            UResourceBundle keyInfoEntry = keyInfoIt.next();
            TypeInfoType typeInfo = TypeInfoType.valueOf(keyInfoEntry.getKey());
            UResourceBundleIterator keyInfoIt2 = keyInfoEntry.getIterator();
            while (keyInfoIt2.hasNext()) {
                UResourceBundle keyInfoEntry2 = keyInfoIt2.next();
                String key2 = keyInfoEntry2.getKey();
                Set<String> _deprecatedTypes = new LinkedHashSet();
                UResourceBundleIterator keyInfoIt3 = keyInfoEntry2.getIterator();
                while (keyInfoIt3.hasNext()) {
                    String key3 = keyInfoIt3.next().getKey();
                    if (AnonymousClass1.$SwitchMap$android$icu$impl$locale$KeyTypeData$TypeInfoType[typeInfo.ordinal()] == 1) {
                        _deprecatedTypes.add(key3);
                    }
                }
                _deprecatedKeyTypes.put(key2, Collections.unmodifiableSet(_deprecatedTypes));
            }
        }
        DEPRECATED_KEY_TYPES = Collections.unmodifiableMap(_deprecatedKeyTypes);
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:0x00e5  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0164 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void initFromTables() {
        Object[][] objArr = KEY_DATA;
        int length = objArr.length;
        int i = 0;
        int i2 = 0;
        while (i2 < length) {
            Object[][] objArr2;
            String from;
            int i3;
            Object[] keyDataEntry;
            String[][] typeData;
            String[][] typeAliasData;
            Object[] keyDataEntry2 = objArr[i2];
            String legacyKeyId = keyDataEntry2[i];
            int i4 = 1;
            String bcpKeyId = keyDataEntry2[1];
            String[][] typeData2 = keyDataEntry2[2];
            String[][] typeAliasData2 = keyDataEntry2[3];
            String[][] bcpTypeAliasData = keyDataEntry2[4];
            boolean hasSameKey = false;
            if (bcpKeyId == null) {
                bcpKeyId = legacyKeyId;
                hasSameKey = true;
            }
            Map<String, Set<String>> typeAliasMap = null;
            if (typeAliasData2 != null) {
                typeAliasMap = new HashMap();
                int length2 = typeAliasData2.length;
                int i5 = i;
                while (i5 < length2) {
                    Set<String> aliasSet;
                    String[] typeAliasDataEntry = typeAliasData2[i5];
                    objArr2 = objArr;
                    from = typeAliasDataEntry[i];
                    String to = typeAliasDataEntry[i4];
                    Set<String> aliasSet2 = (Set) typeAliasMap.get(to);
                    if (aliasSet2 == null) {
                        aliasSet = new HashSet();
                        typeAliasMap.put(to, aliasSet);
                    } else {
                        aliasSet = aliasSet2;
                    }
                    aliasSet.add(from);
                    i5++;
                    objArr = objArr2;
                    i = 0;
                    i4 = 1;
                }
            }
            objArr2 = objArr;
            Map<String, Set<String>> bcpTypeAliasMap = null;
            if (bcpTypeAliasData != null) {
                bcpTypeAliasMap = new HashMap();
                i = bcpTypeAliasData.length;
                i4 = 0;
                while (i4 < i) {
                    int i6;
                    String[] bcpTypeAliasDataEntry = bcpTypeAliasData[i4];
                    String from2 = bcpTypeAliasDataEntry[0];
                    i3 = length;
                    String to2 = bcpTypeAliasDataEntry[1];
                    Set<String> aliasSet3 = (Set) bcpTypeAliasMap.get(to2);
                    if (aliasSet3 == null) {
                        i6 = i;
                        aliasSet3 = new HashSet();
                        bcpTypeAliasMap.put(to2, aliasSet3);
                    } else {
                        i6 = i;
                    }
                    aliasSet3.add(from2);
                    i4++;
                    length = i3;
                    i = i6;
                }
            }
            i3 = length;
            Map<String, Type> typeDataMap = new HashMap();
            i4 = typeData2.length;
            Set<SpecialType> specialTypeSet = null;
            i = 0;
            while (i < i4) {
                String[] typeDataEntry = typeData2[i];
                keyDataEntry = keyDataEntry2;
                String legacyTypeId = typeDataEntry[0];
                String bcpTypeId = typeDataEntry[1];
                boolean isSpecialType = false;
                SpecialType[] values = SpecialType.values();
                int i7 = i4;
                i4 = values.length;
                typeData = typeData2;
                int typeData3 = 0;
                while (typeData3 < i4) {
                    int i8 = i4;
                    SpecialType st = values[typeData3];
                    typeAliasData = typeAliasData2;
                    if (legacyTypeId.equals(st.toString())) {
                        isSpecialType = true;
                        if (specialTypeSet == null) {
                            specialTypeSet = new HashSet();
                        }
                        specialTypeSet.add(st);
                        if (isSpecialType) {
                            String bcpTypeId2 = null;
                            if (bcpTypeId == null) {
                                bcpTypeId = legacyTypeId;
                                bcpTypeId2 = true;
                            }
                            boolean hasSameType = bcpTypeId2;
                            bcpTypeId2 = bcpTypeId;
                            Type t = new Type(legacyTypeId, bcpTypeId2);
                            typeDataMap.put(AsciiUtil.toLowerString(legacyTypeId), t);
                            if (!hasSameType) {
                                typeDataMap.put(AsciiUtil.toLowerString(bcpTypeId2), t);
                            }
                            Set<String> typeAliasSet = (Set) typeAliasMap.get(legacyTypeId);
                            boolean hasSameType2;
                            if (typeAliasSet != null) {
                                legacyTypeId = typeAliasSet.iterator();
                                while (legacyTypeId.hasNext()) {
                                    String str = legacyTypeId;
                                    hasSameType2 = hasSameType;
                                    typeDataMap.put(AsciiUtil.toLowerString((String) legacyTypeId.next()), t);
                                    legacyTypeId = str;
                                    hasSameType = hasSameType2;
                                }
                            } else {
                                hasSameType2 = hasSameType;
                            }
                            Set<String> bcpTypeAliasSet = (Set) bcpTypeAliasMap.get(bcpTypeId2);
                            if (bcpTypeAliasSet != null) {
                                for (String from3 : bcpTypeAliasSet) {
                                    Map<String, Set<String>> bcpTypeAliasMap2 = bcpTypeAliasMap;
                                    Set<String> bcpTypeAliasSet2 = bcpTypeAliasSet;
                                    typeDataMap.put(AsciiUtil.toLowerString(from3), t);
                                    bcpTypeAliasMap = bcpTypeAliasMap2;
                                    bcpTypeAliasSet = bcpTypeAliasSet2;
                                }
                            }
                        }
                        i++;
                        keyDataEntry2 = keyDataEntry;
                        i4 = i7;
                        typeData2 = typeData;
                        typeAliasData2 = typeAliasData;
                        bcpTypeAliasMap = bcpTypeAliasMap;
                    } else {
                        typeData3++;
                        i4 = i8;
                        typeAliasData2 = typeAliasData;
                    }
                }
                typeAliasData = typeAliasData2;
                if (isSpecialType) {
                }
                i++;
                keyDataEntry2 = keyDataEntry;
                i4 = i7;
                typeData2 = typeData;
                typeAliasData2 = typeAliasData;
                bcpTypeAliasMap = bcpTypeAliasMap;
            }
            keyDataEntry = keyDataEntry2;
            typeData = typeData2;
            typeAliasData = typeAliasData2;
            EnumSet<SpecialType> specialTypes = null;
            if (specialTypeSet != null) {
                specialTypes = EnumSet.copyOf(specialTypeSet);
            }
            KeyData keyData = new KeyData(legacyKeyId, bcpKeyId, typeDataMap, specialTypes);
            KEYMAP.put(AsciiUtil.toLowerString(legacyKeyId), keyData);
            if (!hasSameKey) {
                KEYMAP.put(AsciiUtil.toLowerString(bcpKeyId), keyData);
            }
            i2++;
            objArr = objArr2;
            length = i3;
            i = 0;
        }
    }

    public static Set<String> getBcp47Keys() {
        return BCP47_KEYS.keySet();
    }

    public static Set<String> getBcp47KeyTypes(String key) {
        return (Set) BCP47_KEYS.get(key);
    }

    public static boolean isDeprecated(String key) {
        return DEPRECATED_KEYS.contains(key);
    }

    public static boolean isDeprecated(String key, String type) {
        Set<String> deprecatedTypes = (Set) DEPRECATED_KEY_TYPES.get(key);
        if (deprecatedTypes == null) {
            return false;
        }
        return deprecatedTypes.contains(type);
    }

    public static ValueType getValueType(String key) {
        ValueType type = (ValueType) VALUE_TYPES.get(key);
        return type == null ? ValueType.single : type;
    }
}
