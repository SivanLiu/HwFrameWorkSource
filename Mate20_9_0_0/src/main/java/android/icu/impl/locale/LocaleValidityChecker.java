package android.icu.impl.locale;

import android.icu.impl.ValidIdentifiers;
import android.icu.impl.ValidIdentifiers.Datasubtype;
import android.icu.impl.ValidIdentifiers.Datatype;
import android.icu.impl.locale.KeyTypeData.ValueType;
import android.icu.text.DateFormat;
import android.icu.util.IllformedLocaleException;
import android.icu.util.Output;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Builder;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class LocaleValidityChecker {
    static final Set<Datasubtype> REGULAR_ONLY = EnumSet.of(Datasubtype.regular);
    static final Set<String> REORDERING_EXCLUDE = new HashSet(Arrays.asList(new String[]{"zinh", "zyyy"}));
    static final Set<String> REORDERING_INCLUDE = new HashSet(Arrays.asList(new String[]{"space", "punct", "symbol", "currency", "digit", "others", DateFormat.SPECIFIC_TZ}));
    static Pattern SEPARATOR = Pattern.compile("[-_]");
    private static final Pattern VALID_X = Pattern.compile("[a-zA-Z0-9]{2,8}(-[a-zA-Z0-9]{2,8})*");
    private final boolean allowsDeprecated;
    private final Set<Datasubtype> datasubtypes;

    enum SpecialCase {
        normal,
        anything,
        reorder,
        codepoints,
        subdivision,
        rgKey;

        static SpecialCase get(String key) {
            if (key.equals("kr")) {
                return reorder;
            }
            if (key.equals("vt")) {
                return codepoints;
            }
            if (key.equals("sd")) {
                return subdivision;
            }
            if (key.equals("rg")) {
                return rgKey;
            }
            if (key.equals("x0")) {
                return anything;
            }
            return normal;
        }
    }

    public static class Where {
        public String codeFailure;
        public Datatype fieldFailure;

        public boolean set(Datatype datatype, String code) {
            this.fieldFailure = datatype;
            this.codeFailure = code;
            return false;
        }

        public String toString() {
            if (this.fieldFailure == null) {
                return "OK";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            stringBuilder.append(this.fieldFailure);
            stringBuilder.append(", ");
            stringBuilder.append(this.codeFailure);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public LocaleValidityChecker(Set<Datasubtype> datasubtypes) {
        this.datasubtypes = EnumSet.copyOf(datasubtypes);
        this.allowsDeprecated = datasubtypes.contains(Datasubtype.deprecated);
    }

    public LocaleValidityChecker(Datasubtype... datasubtypes) {
        this.datasubtypes = EnumSet.copyOf(Arrays.asList(datasubtypes));
        this.allowsDeprecated = this.datasubtypes.contains(Datasubtype.deprecated);
    }

    public Set<Datasubtype> getDatasubtypes() {
        return EnumSet.copyOf(this.datasubtypes);
    }

    public boolean isValid(ULocale locale, Where where) {
        where.set(null, null);
        String language = locale.getLanguage();
        String script = locale.getScript();
        String region = locale.getCountry();
        String variantString = locale.getVariant();
        Set<Character> extensionKeys = locale.getExtensionKeys();
        if (isValid(Datatype.language, language, where)) {
            if (!isValid(Datatype.script, script, where) || !isValid(Datatype.region, region, where)) {
                return false;
            }
            if (!variantString.isEmpty()) {
                for (String variant : SEPARATOR.split(variantString)) {
                    if (!isValid(Datatype.variant, variant, where)) {
                        return false;
                    }
                }
            }
            for (Character c : extensionKeys) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(c);
                    stringBuilder.append("");
                    Datatype datatype = Datatype.valueOf(stringBuilder.toString());
                    switch (datatype) {
                        case x:
                            return true;
                        case t:
                        case u:
                            if (isValidU(locale, datatype, locale.getExtension(c.charValue()), where)) {
                                break;
                            }
                            return false;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    Datatype datatype2 = Datatype.illegal;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(c);
                    stringBuilder2.append("");
                    return where.set(datatype2, stringBuilder2.toString());
                }
            }
            return true;
        } else if (!language.equals(LanguageTag.PRIVATEUSE)) {
            return false;
        } else {
            where.set(null, null);
            return true;
        }
    }

    private boolean isValidU(ULocale locale, Datatype datatype, String extensionString, Where where) {
        ULocale uLocale;
        Datatype datatype2 = datatype;
        Where where2 = where;
        String key = "";
        int typeCount = 0;
        ValueType valueType = null;
        StringBuilder prefix = new StringBuilder();
        Set<String> seen = new HashSet();
        StringBuilder tBuffer = datatype2 == Datatype.t ? new StringBuilder() : null;
        String[] split = SEPARATOR.split(extensionString);
        int length = split.length;
        SpecialCase specialCase = null;
        String key2 = key;
        int key3 = 0;
        while (key3 < length) {
            String subtag = split[key3];
            String[] strArr = split;
            if (subtag.length() == 2 && (tBuffer == null || subtag.charAt(1) <= '9')) {
                if (tBuffer != null) {
                    if (tBuffer.length() != 0 && !isValidLocale(tBuffer.toString(), where2)) {
                        return false;
                    }
                    tBuffer = null;
                }
                key2 = KeyTypeData.toBcpKey(subtag);
                if (key2 == null) {
                    return where2.set(datatype2, subtag);
                }
                if (!this.allowsDeprecated && KeyTypeData.isDeprecated(key2)) {
                    return where2.set(datatype2, key2);
                }
                valueType = KeyTypeData.getValueType(key2);
                typeCount = 0;
                specialCase = SpecialCase.get(key2);
            } else if (tBuffer != null) {
                if (tBuffer.length() != 0) {
                    tBuffer.append('-');
                }
                tBuffer.append(subtag);
            } else {
                StringBuilder stringBuilder;
                int typeCount2;
                Output<Boolean> isKnownKey = typeCount + 1;
                switch (valueType) {
                    case single:
                        if (isKnownKey > 1) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(key2);
                            stringBuilder.append(LanguageTag.SEP);
                            stringBuilder.append(subtag);
                            return where2.set(datatype2, stringBuilder.toString());
                        }
                        break;
                    case incremental:
                        if (isKnownKey != 1) {
                            prefix.append('-');
                            prefix.append(subtag);
                            subtag = prefix.toString();
                            break;
                        }
                        prefix.setLength(0);
                        prefix.append(subtag);
                        break;
                    case multiple:
                        if (isKnownKey == 1) {
                            seen.clear();
                            break;
                        }
                        break;
                }
                switch (specialCase) {
                    case anything:
                        uLocale = locale;
                        typeCount2 = isKnownKey;
                        break;
                    case codepoints:
                        uLocale = locale;
                        typeCount2 = isKnownKey;
                        try {
                            if (Integer.parseInt(subtag, 16) > 1114111) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(key2);
                                stringBuilder.append(LanguageTag.SEP);
                                stringBuilder.append(subtag);
                                return where2.set(datatype2, stringBuilder.toString());
                            }
                        } catch (NumberFormatException e) {
                            isKnownKey = new StringBuilder();
                            isKnownKey.append(key2);
                            isKnownKey.append(LanguageTag.SEP);
                            isKnownKey.append(subtag);
                            return where2.set(datatype2, isKnownKey.toString());
                        }
                        break;
                    case reorder:
                        uLocale = locale;
                        typeCount2 = isKnownKey;
                        if (seen.add(subtag.equals(DateFormat.SPECIFIC_TZ) != null ? "others" : subtag) == null || !isScriptReorder(subtag)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(key2);
                            stringBuilder.append(LanguageTag.SEP);
                            stringBuilder.append(subtag);
                            return where2.set(datatype2, stringBuilder.toString());
                        }
                    case subdivision:
                        typeCount2 = isKnownKey;
                        if (isSubdivision(locale, subtag) == null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(key2);
                            stringBuilder.append(LanguageTag.SEP);
                            stringBuilder.append(subtag);
                            return where2.set(datatype2, stringBuilder.toString());
                        }
                        break;
                    case rgKey:
                        if (subtag.length() >= 6) {
                            if (subtag.endsWith(DateFormat.SPECIFIC_TZ)) {
                                typeCount2 = isKnownKey;
                                if (isValid(Datatype.region, subtag.substring(0, subtag.length() - 4), where2)) {
                                    uLocale = locale;
                                    break;
                                }
                                return false;
                            }
                            Output<Boolean> output = isKnownKey;
                        } else {
                            typeCount2 = isKnownKey;
                        }
                        return where2.set(datatype2, subtag);
                    default:
                        uLocale = locale;
                        typeCount2 = isKnownKey;
                        isKnownKey = new Output();
                        if (KeyTypeData.toBcpType(key2, subtag, isKnownKey, new Output()) == null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(key2);
                            stringBuilder.append(LanguageTag.SEP);
                            stringBuilder.append(subtag);
                            return where2.set(datatype2, stringBuilder.toString());
                        }
                        if (this.allowsDeprecated == null && KeyTypeData.isDeprecated(key2, subtag)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(key2);
                            stringBuilder.append(LanguageTag.SEP);
                            stringBuilder.append(subtag);
                            return where2.set(datatype2, stringBuilder.toString());
                        }
                }
                typeCount = typeCount2;
                key3++;
                split = strArr;
            }
            uLocale = locale;
            key3++;
            split = strArr;
        }
        uLocale = locale;
        if (tBuffer == null || tBuffer.length() == 0 || isValidLocale(tBuffer.toString(), where2)) {
            return true;
        }
        return false;
    }

    private boolean isSubdivision(ULocale locale, String subtag) {
        int i = 3;
        if (subtag.length() < 3) {
            return false;
        }
        if (subtag.charAt(0) > '9') {
            i = 2;
        }
        String region = subtag.substring(0, i);
        if (ValidIdentifiers.isValid(Datatype.subdivision, this.datasubtypes, region, subtag.substring(region.length())) == null) {
            return false;
        }
        String localeRegion = locale.getCountry();
        if (localeRegion.isEmpty()) {
            localeRegion = ULocale.addLikelySubtags(locale).getCountry();
        }
        if (region.equalsIgnoreCase(localeRegion)) {
            return true;
        }
        return false;
    }

    private boolean isScriptReorder(String subtag) {
        subtag = AsciiUtil.toLowerString(subtag);
        boolean z = true;
        if (REORDERING_INCLUDE.contains(subtag)) {
            return true;
        }
        if (REORDERING_EXCLUDE.contains(subtag)) {
            return false;
        }
        if (ValidIdentifiers.isValid(Datatype.script, REGULAR_ONLY, subtag) == null) {
            z = false;
        }
        return z;
    }

    private boolean isValidLocale(String extensionString, Where where) {
        try {
            return isValid(new Builder().setLanguageTag(extensionString).build(), where);
        } catch (IllformedLocaleException e) {
            return where.set(Datatype.t, SEPARATOR.split(extensionString.substring(e.getErrorIndex()))[0]);
        } catch (Exception e2) {
            return where.set(Datatype.t, e2.getMessage());
        }
    }

    private boolean isValid(Datatype datatype, String code, Where where) {
        boolean z = true;
        if (code.isEmpty()) {
            return true;
        }
        if (datatype == Datatype.variant && "posix".equalsIgnoreCase(code)) {
            return true;
        }
        if (ValidIdentifiers.isValid(datatype, this.datasubtypes, code) == null) {
            z = where == null ? false : where.set(datatype, code);
        }
        return z;
    }
}
