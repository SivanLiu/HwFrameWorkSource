package sun.util.locale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.security.x509.PolicyInformation;

public class LanguageTag {
    private static final Map<String, String[]> GRANDFATHERED = new HashMap();
    public static final String PRIVATEUSE = "x";
    public static final String PRIVUSE_VARIANT_PREFIX = "lvariant";
    public static final String SEP = "-";
    public static final String UNDETERMINED = "und";
    private List<String> extensions = Collections.emptyList();
    private List<String> extlangs = Collections.emptyList();
    private String language = "";
    private String privateuse = "";
    private String region = "";
    private String script = "";
    private List<String> variants = Collections.emptyList();

    static {
        for (String[] e : new String[][]{new String[]{"art-lojban", "jbo"}, new String[]{"cel-gaulish", "xtg-x-cel-gaulish"}, new String[]{"en-GB-oed", "en-GB-x-oed"}, new String[]{"i-ami", "ami"}, new String[]{"i-bnn", "bnn"}, new String[]{"i-default", "en-x-i-default"}, new String[]{"i-enochian", "und-x-i-enochian"}, new String[]{"i-hak", "hak"}, new String[]{"i-klingon", "tlh"}, new String[]{"i-lux", "lb"}, new String[]{"i-mingo", "see-x-i-mingo"}, new String[]{"i-navajo", "nv"}, new String[]{"i-pwn", "pwn"}, new String[]{"i-tao", "tao"}, new String[]{"i-tay", "tay"}, new String[]{"i-tsu", "tsu"}, new String[]{"no-bok", "nb"}, new String[]{"no-nyn", "nn"}, new String[]{"sgn-BE-FR", "sfb"}, new String[]{"sgn-BE-NL", "vgt"}, new String[]{"sgn-CH-DE", "sgg"}, new String[]{"zh-guoyu", "cmn"}, new String[]{"zh-hakka", "hak"}, new String[]{"zh-min", "nan-x-zh-min"}, new String[]{"zh-min-nan", "nan"}, new String[]{"zh-xiang", "hsn"}}) {
            GRANDFATHERED.put(LocaleUtils.toLowerString(e[0]), e);
        }
    }

    private LanguageTag() {
    }

    public static LanguageTag parse(String languageTag, ParseStatus sts) {
        StringTokenIterator itr;
        if (sts == null) {
            sts = new ParseStatus();
        } else {
            sts.reset();
        }
        String[] gfmap = (String[]) GRANDFATHERED.get(LocaleUtils.toLowerString(languageTag));
        if (gfmap != null) {
            itr = new StringTokenIterator(gfmap[1], SEP);
        } else {
            itr = new StringTokenIterator(languageTag, SEP);
        }
        LanguageTag tag = new LanguageTag();
        if (tag.parseLanguage(itr, sts)) {
            tag.parseExtlangs(itr, sts);
            tag.parseScript(itr, sts);
            tag.parseRegion(itr, sts);
            tag.parseVariants(itr, sts);
            tag.parseExtensions(itr, sts);
        }
        tag.parsePrivateuse(itr, sts);
        if (!(itr.isDone() || sts.isError())) {
            String s = itr.current();
            sts.errorIndex = itr.currentStart();
            if (s.length() == 0) {
                sts.errorMsg = "Empty subtag";
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid subtag: ");
                stringBuilder.append(s);
                sts.errorMsg = stringBuilder.toString();
            }
        }
        return tag;
    }

    private boolean parseLanguage(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }
        boolean found = false;
        String s = itr.current();
        if (isLanguage(s)) {
            found = true;
            this.language = s;
            sts.parseLength = itr.currentEnd();
            itr.next();
        }
        return found;
    }

    private boolean parseExtlangs(StringTokenIterator itr, ParseStatus sts) {
        boolean found = false;
        if (itr.isDone() || sts.isError()) {
            return false;
        }
        boolean found2;
        while (true) {
            found2 = found;
            if (itr.isDone()) {
                break;
            }
            String s = itr.current();
            if (!isExtlang(s)) {
                break;
            }
            found2 = true;
            if (this.extlangs.isEmpty()) {
                this.extlangs = new ArrayList(3);
            }
            this.extlangs.add(s);
            sts.parseLength = itr.currentEnd();
            itr.next();
            if (this.extlangs.size() == 3) {
                break;
            }
            found = true;
        }
        return found2;
    }

    private boolean parseScript(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }
        boolean found = false;
        String s = itr.current();
        if (isScript(s)) {
            found = true;
            this.script = s;
            sts.parseLength = itr.currentEnd();
            itr.next();
        }
        return found;
    }

    private boolean parseRegion(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }
        boolean found = false;
        String s = itr.current();
        if (isRegion(s)) {
            found = true;
            this.region = s;
            sts.parseLength = itr.currentEnd();
            itr.next();
        }
        return found;
    }

    private boolean parseVariants(StringTokenIterator itr, ParseStatus sts) {
        boolean found = false;
        if (itr.isDone() || sts.isError()) {
            return false;
        }
        boolean found2;
        while (true) {
            found2 = found;
            if (itr.isDone()) {
                break;
            }
            String s = itr.current();
            if (!isVariant(s)) {
                break;
            }
            if (this.variants.isEmpty()) {
                this.variants = new ArrayList(3);
            }
            this.variants.add(s);
            sts.parseLength = itr.currentEnd();
            itr.next();
            found = true;
        }
        return found2;
    }

    private boolean parseExtensions(StringTokenIterator itr, ParseStatus sts) {
        boolean found = false;
        if (itr.isDone() || sts.isError()) {
            return false;
        }
        boolean found2;
        while (true) {
            found2 = found;
            if (!itr.isDone()) {
                String s = itr.current();
                if (!isExtensionSingleton(s)) {
                    break;
                }
                int start = itr.currentStart();
                String singleton = s;
                StringBuilder sb = new StringBuilder(singleton);
                itr.next();
                while (!itr.isDone()) {
                    s = itr.current();
                    if (!isExtensionSubtag(s)) {
                        break;
                    }
                    sb.append(SEP);
                    sb.append(s);
                    sts.parseLength = itr.currentEnd();
                    itr.next();
                }
                if (sts.parseLength <= start) {
                    sts.errorIndex = start;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Incomplete extension '");
                    stringBuilder.append(singleton);
                    stringBuilder.append("'");
                    sts.errorMsg = stringBuilder.toString();
                    break;
                }
                if (this.extensions.isEmpty()) {
                    this.extensions = new ArrayList(4);
                }
                this.extensions.add(sb.toString());
                found = true;
            } else {
                break;
            }
        }
        return found2;
    }

    private boolean parsePrivateuse(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }
        boolean found = false;
        String s = itr.current();
        if (isPrivateusePrefix(s)) {
            int start = itr.currentStart();
            StringBuilder sb = new StringBuilder(s);
            itr.next();
            while (!itr.isDone()) {
                s = itr.current();
                if (!isPrivateuseSubtag(s)) {
                    break;
                }
                sb.append(SEP);
                sb.append(s);
                sts.parseLength = itr.currentEnd();
                itr.next();
            }
            if (sts.parseLength <= start) {
                sts.errorIndex = start;
                sts.errorMsg = "Incomplete privateuse";
            } else {
                this.privateuse = sb.toString();
                found = true;
            }
        }
        return found;
    }

    public static LanguageTag parseLocale(BaseLocale baseLocale, LocaleExtensions localeExtensions) {
        List<String> variants;
        StringBuilder buf;
        LocaleExtensions localeExtensions2 = localeExtensions;
        LanguageTag tag = new LanguageTag();
        String language = baseLocale.getLanguage();
        String script = baseLocale.getScript();
        String region = baseLocale.getRegion();
        String variant = baseLocale.getVariant();
        boolean hasSubtag = false;
        String privuseVar = null;
        if (isLanguage(language)) {
            if (language.equals("iw")) {
                language = "he";
            } else if (language.equals("ji")) {
                language = "yi";
            } else if (language.equals("in")) {
                language = PolicyInformation.ID;
            }
            tag.language = language;
        }
        if (isScript(script)) {
            tag.script = canonicalizeScript(script);
            hasSubtag = true;
        }
        if (isRegion(region)) {
            tag.region = canonicalizeRegion(region);
            hasSubtag = true;
        }
        if (tag.language.equals("no") && tag.region.equals("NO") && variant.equals("NY")) {
            tag.language = "nn";
            variant = "";
        }
        if (variant.length() > 0) {
            variants = null;
            StringTokenIterator varitr = new StringTokenIterator(variant, BaseLocale.SEP);
            while (!varitr.isDone()) {
                String var = varitr.current();
                if (!isVariant(var)) {
                    break;
                }
                if (variants == null) {
                    variants = new ArrayList();
                }
                variants.add(var);
                varitr.next();
            }
            if (variants != null) {
                tag.variants = variants;
                hasSubtag = true;
            }
            if (!varitr.isDone()) {
                buf = new StringBuilder();
                while (!varitr.isDone()) {
                    String prvv = varitr.current();
                    if (!isPrivateuseSubtag(prvv)) {
                        break;
                    }
                    if (buf.length() > 0) {
                        buf.append(SEP);
                    }
                    buf.append(prvv);
                    varitr.next();
                }
                if (buf.length() > 0) {
                    privuseVar = buf.toString();
                }
            }
        }
        variants = null;
        String privateuse = null;
        if (localeExtensions2 != null) {
            for (Character locextKey : localeExtensions.getKeys()) {
                Extension ext = localeExtensions2.getExtension(locextKey);
                if (isPrivateusePrefixChar(locextKey.charValue())) {
                    privateuse = ext.getValue();
                } else {
                    if (variants == null) {
                        variants = new ArrayList();
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(locextKey.toString());
                    stringBuilder.append(SEP);
                    stringBuilder.append(ext.getValue());
                    variants.add(stringBuilder.toString());
                }
            }
        }
        if (variants != null) {
            tag.extensions = variants;
            hasSubtag = true;
        }
        if (privuseVar != null) {
            if (privateuse == null) {
                buf = new StringBuilder();
                buf.append("lvariant-");
                buf.append(privuseVar);
                privateuse = buf.toString();
            } else {
                buf = new StringBuilder();
                buf.append(privateuse);
                buf.append(SEP);
                buf.append(PRIVUSE_VARIANT_PREFIX);
                buf.append(SEP);
                buf.append(privuseVar.replace(BaseLocale.SEP, SEP));
                privateuse = buf.toString();
            }
        }
        if (privateuse != null) {
            tag.privateuse = privateuse;
        }
        if (tag.language.length() == 0 && (hasSubtag || privateuse == null)) {
            tag.language = UNDETERMINED;
        }
        return tag;
    }

    public String getLanguage() {
        return this.language;
    }

    public List<String> getExtlangs() {
        if (this.extlangs.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.extlangs);
    }

    public String getScript() {
        return this.script;
    }

    public String getRegion() {
        return this.region;
    }

    public List<String> getVariants() {
        if (this.variants.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.variants);
    }

    public List<String> getExtensions() {
        if (this.extensions.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.extensions);
    }

    public String getPrivateuse() {
        return this.privateuse;
    }

    public static boolean isLanguage(String s) {
        int len = s.length();
        return len >= 2 && len <= 8 && LocaleUtils.isAlphaString(s);
    }

    public static boolean isExtlang(String s) {
        return s.length() == 3 && LocaleUtils.isAlphaString(s);
    }

    public static boolean isScript(String s) {
        return s.length() == 4 && LocaleUtils.isAlphaString(s);
    }

    public static boolean isRegion(String s) {
        return (s.length() == 2 && LocaleUtils.isAlphaString(s)) || (s.length() == 3 && LocaleUtils.isNumericString(s));
    }

    public static boolean isVariant(String s) {
        int len = s.length();
        if (len >= 5 && len <= 8) {
            return LocaleUtils.isAlphaNumericString(s);
        }
        boolean z = false;
        if (len != 4) {
            return false;
        }
        if (LocaleUtils.isNumeric(s.charAt(0)) && LocaleUtils.isAlphaNumeric(s.charAt(1)) && LocaleUtils.isAlphaNumeric(s.charAt(2)) && LocaleUtils.isAlphaNumeric(s.charAt(3))) {
            z = true;
        }
        return z;
    }

    public static boolean isExtensionSingleton(String s) {
        if (s.length() == 1 && LocaleUtils.isAlphaString(s) && !LocaleUtils.caseIgnoreMatch(PRIVATEUSE, s)) {
            return true;
        }
        return false;
    }

    public static boolean isExtensionSingletonChar(char c) {
        return isExtensionSingleton(String.valueOf(c));
    }

    public static boolean isExtensionSubtag(String s) {
        int len = s.length();
        return len >= 2 && len <= 8 && LocaleUtils.isAlphaNumericString(s);
    }

    public static boolean isPrivateusePrefix(String s) {
        if (s.length() == 1 && LocaleUtils.caseIgnoreMatch(PRIVATEUSE, s)) {
            return true;
        }
        return false;
    }

    public static boolean isPrivateusePrefixChar(char c) {
        return LocaleUtils.caseIgnoreMatch(PRIVATEUSE, String.valueOf(c));
    }

    public static boolean isPrivateuseSubtag(String s) {
        int len = s.length();
        return len >= 1 && len <= 8 && LocaleUtils.isAlphaNumericString(s);
    }

    public static String canonicalizeLanguage(String s) {
        return LocaleUtils.toLowerString(s);
    }

    public static String canonicalizeExtlang(String s) {
        return LocaleUtils.toLowerString(s);
    }

    public static String canonicalizeScript(String s) {
        return LocaleUtils.toTitleString(s);
    }

    public static String canonicalizeRegion(String s) {
        return LocaleUtils.toUpperString(s);
    }

    public static String canonicalizeVariant(String s) {
        return LocaleUtils.toLowerString(s);
    }

    public static String canonicalizeExtension(String s) {
        return LocaleUtils.toLowerString(s);
    }

    public static String canonicalizeExtensionSingleton(String s) {
        return LocaleUtils.toLowerString(s);
    }

    public static String canonicalizeExtensionSubtag(String s) {
        return LocaleUtils.toLowerString(s);
    }

    public static String canonicalizePrivateuse(String s) {
        return LocaleUtils.toLowerString(s);
    }

    public static String canonicalizePrivateuseSubtag(String s) {
        return LocaleUtils.toLowerString(s);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.language.length() > 0) {
            sb.append(this.language);
            for (String extlang : this.extlangs) {
                sb.append(SEP);
                sb.append(extlang);
            }
            if (this.script.length() > 0) {
                sb.append(SEP);
                sb.append(this.script);
            }
            if (this.region.length() > 0) {
                sb.append(SEP);
                sb.append(this.region);
            }
            for (String extlang2 : this.variants) {
                sb.append(SEP);
                sb.append(extlang2);
            }
            for (String extlang22 : this.extensions) {
                sb.append(SEP);
                sb.append(extlang22);
            }
        }
        if (this.privateuse.length() > 0) {
            if (sb.length() > 0) {
                sb.append(SEP);
            }
            sb.append(this.privateuse);
        }
        return sb.toString();
    }
}
