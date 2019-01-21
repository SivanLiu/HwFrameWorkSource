package sun.util.locale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InternalLocaleBuilder {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final CaseInsensitiveChar PRIVATEUSE_KEY = new CaseInsensitiveChar(LanguageTag.PRIVATEUSE);
    private Map<CaseInsensitiveChar, String> extensions;
    private String language = "";
    private String region = "";
    private String script = "";
    private Set<CaseInsensitiveString> uattributes;
    private Map<CaseInsensitiveString, String> ukeywords;
    private String variant = "";

    static final class CaseInsensitiveChar {
        private final char ch;
        private final char lowerCh;

        private CaseInsensitiveChar(String s) {
            this(s.charAt(0));
        }

        CaseInsensitiveChar(char c) {
            this.ch = c;
            this.lowerCh = LocaleUtils.toLower(this.ch);
        }

        public char value() {
            return this.ch;
        }

        public int hashCode() {
            return this.lowerCh;
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CaseInsensitiveChar)) {
                return false;
            }
            if (this.lowerCh != ((CaseInsensitiveChar) obj).lowerCh) {
                z = false;
            }
            return z;
        }
    }

    static final class CaseInsensitiveString {
        private final String lowerStr;
        private final String str;

        CaseInsensitiveString(String s) {
            this.str = s;
            this.lowerStr = LocaleUtils.toLowerString(s);
        }

        public String value() {
            return this.str;
        }

        public int hashCode() {
            return this.lowerStr.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof CaseInsensitiveString) {
                return this.lowerStr.equals(((CaseInsensitiveString) obj).lowerStr);
            }
            return false;
        }
    }

    public InternalLocaleBuilder setLanguage(String language) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(language)) {
            this.language = "";
        } else if (LanguageTag.isLanguage(language)) {
            this.language = language;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ill-formed language: ");
            stringBuilder.append(language);
            throw new LocaleSyntaxException(stringBuilder.toString(), 0);
        }
        return this;
    }

    public InternalLocaleBuilder setScript(String script) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(script)) {
            this.script = "";
        } else if (LanguageTag.isScript(script)) {
            this.script = script;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ill-formed script: ");
            stringBuilder.append(script);
            throw new LocaleSyntaxException(stringBuilder.toString(), 0);
        }
        return this;
    }

    public InternalLocaleBuilder setRegion(String region) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(region)) {
            this.region = "";
        } else if (LanguageTag.isRegion(region)) {
            this.region = region;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ill-formed region: ");
            stringBuilder.append(region);
            throw new LocaleSyntaxException(stringBuilder.toString(), 0);
        }
        return this;
    }

    public InternalLocaleBuilder setVariant(String variant) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(variant)) {
            this.variant = "";
        } else {
            String var = variant.replaceAll(LanguageTag.SEP, BaseLocale.SEP);
            int errIdx = checkVariants(var, BaseLocale.SEP);
            if (errIdx == -1) {
                this.variant = var;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ill-formed variant: ");
                stringBuilder.append(variant);
                throw new LocaleSyntaxException(stringBuilder.toString(), errIdx);
            }
        }
        return this;
    }

    public InternalLocaleBuilder addUnicodeLocaleAttribute(String attribute) throws LocaleSyntaxException {
        if (UnicodeLocaleExtension.isAttribute(attribute)) {
            if (this.uattributes == null) {
                this.uattributes = new HashSet(4);
            }
            this.uattributes.add(new CaseInsensitiveString(attribute));
            return this;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ill-formed Unicode locale attribute: ");
        stringBuilder.append(attribute);
        throw new LocaleSyntaxException(stringBuilder.toString());
    }

    public InternalLocaleBuilder removeUnicodeLocaleAttribute(String attribute) throws LocaleSyntaxException {
        if (attribute == null || !UnicodeLocaleExtension.isAttribute(attribute)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ill-formed Unicode locale attribute: ");
            stringBuilder.append(attribute);
            throw new LocaleSyntaxException(stringBuilder.toString());
        }
        if (this.uattributes != null) {
            this.uattributes.remove(new CaseInsensitiveString(attribute));
        }
        return this;
    }

    public InternalLocaleBuilder setUnicodeLocaleKeyword(String key, String type) throws LocaleSyntaxException {
        if (UnicodeLocaleExtension.isKey(key)) {
            CaseInsensitiveString cikey = new CaseInsensitiveString(key);
            if (type != null) {
                if (type.length() != 0) {
                    StringTokenIterator itr = new StringTokenIterator(type.replaceAll(BaseLocale.SEP, LanguageTag.SEP), LanguageTag.SEP);
                    while (!itr.isDone()) {
                        if (UnicodeLocaleExtension.isTypeSubtag(itr.current())) {
                            itr.next();
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Ill-formed Unicode locale keyword type: ");
                            stringBuilder.append(type);
                            throw new LocaleSyntaxException(stringBuilder.toString(), itr.currentStart());
                        }
                    }
                }
                if (this.ukeywords == null) {
                    this.ukeywords = new HashMap(4);
                }
                this.ukeywords.put(cikey, type);
            } else if (this.ukeywords != null) {
                this.ukeywords.remove(cikey);
            }
            return this;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Ill-formed Unicode locale keyword key: ");
        stringBuilder2.append(key);
        throw new LocaleSyntaxException(stringBuilder2.toString());
    }

    public InternalLocaleBuilder setExtension(char singleton, String value) throws LocaleSyntaxException {
        boolean isBcpPrivateuse = LanguageTag.isPrivateusePrefixChar(singleton);
        if (isBcpPrivateuse || LanguageTag.isExtensionSingletonChar(singleton)) {
            boolean remove = LocaleUtils.isEmpty(value);
            CaseInsensitiveChar key = new CaseInsensitiveChar(singleton);
            if (!remove) {
                String val = value.replaceAll(BaseLocale.SEP, LanguageTag.SEP);
                StringTokenIterator itr = new StringTokenIterator(val, LanguageTag.SEP);
                while (!itr.isDone()) {
                    boolean validSubtag;
                    String s = itr.current();
                    if (isBcpPrivateuse) {
                        validSubtag = LanguageTag.isPrivateuseSubtag(s);
                    } else {
                        validSubtag = LanguageTag.isExtensionSubtag(s);
                    }
                    if (validSubtag) {
                        itr.next();
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Ill-formed extension value: ");
                        stringBuilder.append(s);
                        throw new LocaleSyntaxException(stringBuilder.toString(), itr.currentStart());
                    }
                }
                if (UnicodeLocaleExtension.isSingletonChar(key.value())) {
                    setUnicodeLocaleExtension(val);
                } else {
                    if (this.extensions == null) {
                        this.extensions = new HashMap(4);
                    }
                    this.extensions.put(key, val);
                }
            } else if (UnicodeLocaleExtension.isSingletonChar(key.value())) {
                if (this.uattributes != null) {
                    this.uattributes.clear();
                }
                if (this.ukeywords != null) {
                    this.ukeywords.clear();
                }
            } else if (this.extensions != null && this.extensions.containsKey(key)) {
                this.extensions.remove(key);
            }
            return this;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Ill-formed extension key: ");
        stringBuilder2.append(singleton);
        throw new LocaleSyntaxException(stringBuilder2.toString());
    }

    public InternalLocaleBuilder setExtensions(String subtags) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(subtags)) {
            clearExtensions();
            return this;
        }
        String s;
        int start;
        subtags = subtags.replaceAll(BaseLocale.SEP, LanguageTag.SEP);
        StringTokenIterator itr = new StringTokenIterator(subtags, LanguageTag.SEP);
        List<String> extensions = null;
        String privateuse = null;
        int parsed = 0;
        while (!itr.isDone()) {
            s = itr.current();
            if (!LanguageTag.isExtensionSingleton(s)) {
                break;
            }
            start = itr.currentStart();
            String singleton = s;
            StringBuilder sb = new StringBuilder(singleton);
            itr.next();
            while (!itr.isDone()) {
                s = itr.current();
                if (!LanguageTag.isExtensionSubtag(s)) {
                    break;
                }
                sb.append(LanguageTag.SEP);
                sb.append(s);
                parsed = itr.currentEnd();
                itr.next();
            }
            if (parsed >= start) {
                if (extensions == null) {
                    extensions = new ArrayList(4);
                }
                extensions.add(sb.toString());
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Incomplete extension '");
                stringBuilder.append(singleton);
                stringBuilder.append("'");
                throw new LocaleSyntaxException(stringBuilder.toString(), start);
            }
        }
        if (!itr.isDone()) {
            s = itr.current();
            if (LanguageTag.isPrivateusePrefix(s)) {
                start = itr.currentStart();
                StringBuilder sb2 = new StringBuilder(s);
                itr.next();
                while (!itr.isDone()) {
                    s = itr.current();
                    if (!LanguageTag.isPrivateuseSubtag(s)) {
                        break;
                    }
                    sb2.append(LanguageTag.SEP);
                    sb2.append(s);
                    parsed = itr.currentEnd();
                    itr.next();
                }
                if (parsed > start) {
                    privateuse = sb2.toString();
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Incomplete privateuse:");
                    stringBuilder2.append(subtags.substring(start));
                    throw new LocaleSyntaxException(stringBuilder2.toString(), start);
                }
            }
        }
        if (itr.isDone()) {
            return setExtensions(extensions, privateuse);
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Ill-formed extension subtags:");
        stringBuilder3.append(subtags.substring(itr.currentStart()));
        throw new LocaleSyntaxException(stringBuilder3.toString(), itr.currentStart());
    }

    private InternalLocaleBuilder setExtensions(List<String> bcpExtensions, String privateuse) {
        clearExtensions();
        if (!LocaleUtils.isEmpty((List) bcpExtensions)) {
            Set<CaseInsensitiveChar> done = new HashSet(bcpExtensions.size());
            for (String bcpExt : bcpExtensions) {
                CaseInsensitiveChar key = new CaseInsensitiveChar(bcpExt);
                if (!done.contains(key)) {
                    if (UnicodeLocaleExtension.isSingletonChar(key.value())) {
                        setUnicodeLocaleExtension(bcpExt.substring(2));
                    } else {
                        if (this.extensions == null) {
                            this.extensions = new HashMap(4);
                        }
                        this.extensions.put(key, bcpExt.substring(2));
                    }
                }
                done.add(key);
            }
        }
        if (privateuse != null && privateuse.length() > 0) {
            if (this.extensions == null) {
                this.extensions = new HashMap(1);
            }
            this.extensions.put(new CaseInsensitiveChar(privateuse), privateuse.substring(2));
        }
        return this;
    }

    public InternalLocaleBuilder setLanguageTag(LanguageTag langtag) {
        clear();
        if (langtag.getExtlangs().isEmpty()) {
            String lang = langtag.getLanguage();
            if (!lang.equals(LanguageTag.UNDETERMINED)) {
                this.language = lang;
            }
        } else {
            this.language = (String) langtag.getExtlangs().get(0);
        }
        this.script = langtag.getScript();
        this.region = langtag.getRegion();
        List<String> bcpVariants = langtag.getVariants();
        if (!bcpVariants.isEmpty()) {
            StringBuilder var = new StringBuilder((String) bcpVariants.get(0));
            int size = bcpVariants.size();
            for (int i = 1; i < size; i++) {
                var.append(BaseLocale.SEP);
                var.append((String) bcpVariants.get(i));
            }
            this.variant = var.toString();
        }
        setExtensions(langtag.getExtensions(), langtag.getPrivateuse());
        return this;
    }

    public InternalLocaleBuilder setLocale(BaseLocale base, LocaleExtensions localeExtensions) throws LocaleSyntaxException {
        LocaleExtensions localeExtensions2 = localeExtensions;
        String language = base.getLanguage();
        String script = base.getScript();
        String region = base.getRegion();
        String variant = base.getVariant();
        if (language.equals("ja") && region.equals("JP") && variant.equals("JP")) {
            variant = "";
        } else if (language.equals("th") && region.equals("TH") && variant.equals("TH")) {
            variant = "";
        } else if (language.equals("no") && region.equals("NO") && variant.equals("NY")) {
            language = "nn";
            variant = "";
        }
        StringBuilder stringBuilder;
        if (language.length() > 0 && !LanguageTag.isLanguage(language)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ill-formed language: ");
            stringBuilder.append(language);
            throw new LocaleSyntaxException(stringBuilder.toString());
        } else if (script.length() > 0 && !LanguageTag.isScript(script)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ill-formed script: ");
            stringBuilder.append(script);
            throw new LocaleSyntaxException(stringBuilder.toString());
        } else if (region.length() <= 0 || LanguageTag.isRegion(region)) {
            if (variant.length() > 0) {
                variant = variant.replaceAll(LanguageTag.SEP, BaseLocale.SEP);
                int errIdx = checkVariants(variant, BaseLocale.SEP);
                if (errIdx != -1) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Ill-formed variant: ");
                    stringBuilder2.append(variant);
                    throw new LocaleSyntaxException(stringBuilder2.toString(), errIdx);
                }
            }
            this.language = language;
            this.script = script;
            this.region = region;
            this.variant = variant;
            clearExtensions();
            Set<Character> extKeys = localeExtensions2 == null ? null : localeExtensions.getKeys();
            if (extKeys != null) {
                for (Character key : extKeys) {
                    Extension e = localeExtensions2.getExtension(key);
                    int i = 4;
                    if (e instanceof UnicodeLocaleExtension) {
                        UnicodeLocaleExtension ue = (UnicodeLocaleExtension) e;
                        for (String uatr : ue.getUnicodeLocaleAttributes()) {
                            if (this.uattributes == null) {
                                this.uattributes = new HashSet(4);
                            }
                            this.uattributes.add(new CaseInsensitiveString(uatr));
                        }
                        for (String uatr2 : ue.getUnicodeLocaleKeys()) {
                            if (this.ukeywords == null) {
                                this.ukeywords = new HashMap(i);
                            }
                            this.ukeywords.put(new CaseInsensitiveString(uatr2), ue.getUnicodeLocaleType(uatr2));
                            i = 4;
                        }
                    } else {
                        if (this.extensions == null) {
                            this.extensions = new HashMap(4);
                        }
                        this.extensions.put(new CaseInsensitiveChar(key.charValue()), e.getValue());
                    }
                }
            }
            return this;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ill-formed region: ");
            stringBuilder.append(region);
            throw new LocaleSyntaxException(stringBuilder.toString());
        }
    }

    public InternalLocaleBuilder clear() {
        this.language = "";
        this.script = "";
        this.region = "";
        this.variant = "";
        clearExtensions();
        return this;
    }

    public InternalLocaleBuilder clearExtensions() {
        if (this.extensions != null) {
            this.extensions.clear();
        }
        if (this.uattributes != null) {
            this.uattributes.clear();
        }
        if (this.ukeywords != null) {
            this.ukeywords.clear();
        }
        return this;
    }

    public BaseLocale getBaseLocale() {
        String language = this.language;
        String script = this.script;
        String region = this.region;
        String variant = this.variant;
        if (this.extensions != null) {
            String privuse = (String) this.extensions.get(PRIVATEUSE_KEY);
            if (privuse != null) {
                StringTokenIterator itr = new StringTokenIterator(privuse, LanguageTag.SEP);
                boolean sawPrefix = false;
                int privVarStart = -1;
                while (!itr.isDone()) {
                    if (sawPrefix) {
                        privVarStart = itr.currentStart();
                        break;
                    }
                    if (LocaleUtils.caseIgnoreMatch(itr.current(), LanguageTag.PRIVUSE_VARIANT_PREFIX)) {
                        sawPrefix = true;
                    }
                    itr.next();
                }
                if (privVarStart != -1) {
                    StringBuilder sb = new StringBuilder(variant);
                    if (sb.length() != 0) {
                        sb.append(BaseLocale.SEP);
                    }
                    sb.append(privuse.substring(privVarStart).replaceAll(LanguageTag.SEP, BaseLocale.SEP));
                    variant = sb.toString();
                }
            }
        }
        return BaseLocale.getInstance(language, script, region, variant);
    }

    public LocaleExtensions getLocaleExtensions() {
        LocaleExtensions localeExtensions = null;
        if (LocaleUtils.isEmpty(this.extensions) && LocaleUtils.isEmpty(this.uattributes) && LocaleUtils.isEmpty(this.ukeywords)) {
            return null;
        }
        LocaleExtensions lext = new LocaleExtensions(this.extensions, this.uattributes, this.ukeywords);
        if (!lext.isEmpty()) {
            localeExtensions = lext;
        }
        return localeExtensions;
    }

    static String removePrivateuseVariant(String privuseVal) {
        StringTokenIterator itr = new StringTokenIterator(privuseVal, LanguageTag.SEP);
        int prefixStart = -1;
        boolean sawPrivuseVar = false;
        while (!itr.isDone()) {
            if (prefixStart != -1) {
                sawPrivuseVar = true;
                break;
            }
            if (LocaleUtils.caseIgnoreMatch(itr.current(), LanguageTag.PRIVUSE_VARIANT_PREFIX)) {
                prefixStart = itr.currentStart();
            }
            itr.next();
        }
        if (!sawPrivuseVar) {
            return privuseVal;
        }
        return prefixStart == 0 ? null : privuseVal.substring(0, prefixStart - 1);
    }

    private int checkVariants(String variants, String sep) {
        StringTokenIterator itr = new StringTokenIterator(variants, sep);
        while (!itr.isDone()) {
            if (!LanguageTag.isVariant(itr.current())) {
                return itr.currentStart();
            }
            itr.next();
        }
        return -1;
    }

    private void setUnicodeLocaleExtension(String subtags) {
        if (this.uattributes != null) {
            this.uattributes.clear();
        }
        if (this.ukeywords != null) {
            this.ukeywords.clear();
        }
        StringTokenIterator itr = new StringTokenIterator(subtags, LanguageTag.SEP);
        while (!itr.isDone() && UnicodeLocaleExtension.isAttribute(itr.current())) {
            if (this.uattributes == null) {
                this.uattributes = new HashSet(4);
            }
            this.uattributes.add(new CaseInsensitiveString(itr.current()));
            itr.next();
        }
        int typeStart = -1;
        CaseInsensitiveString key = null;
        int typeEnd = -1;
        while (!itr.isDone()) {
            if (key != null) {
                if (UnicodeLocaleExtension.isKey(itr.current())) {
                    String type = typeStart == -1 ? "" : subtags.substring(typeStart, typeEnd);
                    if (this.ukeywords == null) {
                        this.ukeywords = new HashMap(4);
                    }
                    this.ukeywords.put(key, type);
                    CaseInsensitiveString tmpKey = new CaseInsensitiveString(itr.current());
                    key = this.ukeywords.containsKey(tmpKey) ? null : tmpKey;
                    typeEnd = -1;
                    typeStart = -1;
                } else {
                    if (typeStart == -1) {
                        typeStart = itr.currentStart();
                    }
                    typeEnd = itr.currentEnd();
                }
            } else if (UnicodeLocaleExtension.isKey(itr.current())) {
                key = new CaseInsensitiveString(itr.current());
                if (this.ukeywords != null && this.ukeywords.containsKey(key)) {
                    key = null;
                }
            }
            if (itr.hasNext()) {
                itr.next();
            } else if (key != null) {
                String type2 = typeStart == -1 ? "" : subtags.substring(typeStart, typeEnd);
                if (this.ukeywords == null) {
                    this.ukeywords = new HashMap(4);
                }
                this.ukeywords.put(key, type2);
                return;
            } else {
                return;
            }
        }
    }
}
