package sun.util.locale;

import java.lang.ref.SoftReference;
import sun.security.x509.PolicyInformation;

public final class BaseLocale {
    private static final Cache CACHE = new Cache();
    public static final String SEP = "_";
    private volatile int hash;
    private final String language;
    private final String region;
    private final String script;
    private final String variant;

    private static final class Key {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private final int hash;
        private final SoftReference<String> lang;
        private final boolean normalized;
        private final SoftReference<String> regn;
        private final SoftReference<String> scrt;
        private final SoftReference<String> vart;

        static {
            Class cls = BaseLocale.class;
        }

        private Key(String language, String region) {
            this.lang = new SoftReference(language);
            this.scrt = new SoftReference("");
            this.regn = new SoftReference(region);
            this.vart = new SoftReference("");
            this.normalized = true;
            int h = language.hashCode();
            if (region != "") {
                for (int i = 0; i < region.length(); i++) {
                    h = (31 * h) + LocaleUtils.toLower(region.charAt(i));
                }
            }
            this.hash = h;
        }

        public Key(String language, String script, String region, String variant) {
            this(language, script, region, variant, false);
        }

        private Key(String language, String script, String region, String variant, boolean normalized) {
            int h;
            int h2 = 0;
            int i = 0;
            if (language != null) {
                this.lang = new SoftReference(language);
                h = 0;
                for (h2 = 0; h2 < language.length(); h2++) {
                    h = (31 * h) + LocaleUtils.toLower(language.charAt(h2));
                }
                h2 = h;
            } else {
                this.lang = new SoftReference("");
            }
            if (script != null) {
                this.scrt = new SoftReference(script);
                h = h2;
                for (h2 = 0; h2 < script.length(); h2++) {
                    h = (31 * h) + LocaleUtils.toLower(script.charAt(h2));
                }
                h2 = h;
            } else {
                this.scrt = new SoftReference("");
            }
            if (region != null) {
                this.regn = new SoftReference(region);
                h = h2;
                for (h2 = 0; h2 < region.length(); h2++) {
                    h = (31 * h) + LocaleUtils.toLower(region.charAt(h2));
                }
                h2 = h;
            } else {
                this.regn = new SoftReference("");
            }
            if (variant != null) {
                this.vart = new SoftReference(variant);
                while (i < variant.length()) {
                    h2 = (31 * h2) + variant.charAt(i);
                    i++;
                }
            } else {
                this.vart = new SoftReference("");
            }
            this.hash = h2;
            this.normalized = normalized;
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if ((obj instanceof Key) && this.hash == ((Key) obj).hash) {
                String tl = (String) this.lang.get();
                String ol = (String) ((Key) obj).lang.get();
                if (!(tl == null || ol == null || !LocaleUtils.caseIgnoreMatch(ol, tl))) {
                    String ts = (String) this.scrt.get();
                    String os = (String) ((Key) obj).scrt.get();
                    if (!(ts == null || os == null || !LocaleUtils.caseIgnoreMatch(os, ts))) {
                        String tr = (String) this.regn.get();
                        String or = (String) ((Key) obj).regn.get();
                        if (!(tr == null || or == null || !LocaleUtils.caseIgnoreMatch(or, tr))) {
                            String tv = (String) this.vart.get();
                            String ov = (String) ((Key) obj).vart.get();
                            if (ov == null || !ov.equals(tv)) {
                                z = false;
                            }
                            return z;
                        }
                    }
                }
            }
            return false;
        }

        public int hashCode() {
            return this.hash;
        }

        public static Key normalize(Key key) {
            if (key.normalized) {
                return key;
            }
            return new Key(LocaleUtils.toLowerString((String) key.lang.get()).intern(), LocaleUtils.toTitleString((String) key.scrt.get()).intern(), LocaleUtils.toUpperString((String) key.regn.get()).intern(), ((String) key.vart.get()).intern(), true);
        }
    }

    private static class Cache extends LocaleObjectCache<Key, BaseLocale> {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = BaseLocale.class;
        }

        protected Key normalizeKey(Key key) {
            return Key.normalize(key);
        }

        protected BaseLocale createObject(Key key) {
            return new BaseLocale((String) key.lang.get(), (String) key.scrt.get(), (String) key.regn.get(), (String) key.vart.get());
        }
    }

    private BaseLocale(String language, String region) {
        this.hash = 0;
        this.language = language;
        this.script = "";
        this.region = region;
        this.variant = "";
    }

    private BaseLocale(String language, String script, String region, String variant) {
        this.hash = 0;
        this.language = language != null ? LocaleUtils.toLowerString(language).intern() : "";
        this.script = script != null ? LocaleUtils.toTitleString(script).intern() : "";
        this.region = region != null ? LocaleUtils.toUpperString(region).intern() : "";
        this.variant = variant != null ? variant.intern() : "";
    }

    public static BaseLocale createInstance(String language, String region) {
        BaseLocale base = new BaseLocale(language, region);
        CACHE.put(new Key(language, region), base);
        return base;
    }

    public static BaseLocale getInstance(String language, String script, String region, String variant) {
        if (language != null) {
            if (LocaleUtils.caseIgnoreMatch(language, "he")) {
                language = "iw";
            } else if (LocaleUtils.caseIgnoreMatch(language, "yi")) {
                language = "ji";
            } else if (LocaleUtils.caseIgnoreMatch(language, PolicyInformation.ID)) {
                language = "in";
            }
        }
        return (BaseLocale) CACHE.get(new Key(language, script, region, variant));
    }

    public String getLanguage() {
        return this.language;
    }

    public String getScript() {
        return this.script;
    }

    public String getRegion() {
        return this.region;
    }

    public String getVariant() {
        return this.variant;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BaseLocale)) {
            return false;
        }
        BaseLocale other = (BaseLocale) obj;
        if (!(this.language == other.language && this.script == other.script && this.region == other.region && this.variant == other.variant)) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (this.language.length() > 0) {
            buf.append("language=");
            buf.append(this.language);
        }
        if (this.script.length() > 0) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append("script=");
            buf.append(this.script);
        }
        if (this.region.length() > 0) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append("region=");
            buf.append(this.region);
        }
        if (this.variant.length() > 0) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append("variant=");
            buf.append(this.variant);
        }
        return buf.toString();
    }

    public int hashCode() {
        int h = this.hash;
        if (h != 0) {
            return h;
        }
        h = (31 * ((31 * ((31 * this.language.hashCode()) + this.script.hashCode())) + this.region.hashCode())) + this.variant.hashCode();
        this.hash = h;
        return h;
    }
}
