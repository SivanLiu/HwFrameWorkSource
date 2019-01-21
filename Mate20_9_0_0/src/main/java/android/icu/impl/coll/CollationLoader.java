package android.icu.impl.coll;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUResourceBundle.OpenType;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.Output;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.MissingResourceException;

public final class CollationLoader {
    private static volatile String rootRules = null;

    private static final class ASCII {
        private ASCII() {
        }

        static String toLowerCase(String s) {
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if ('A' > c || c > 'Z') {
                    i++;
                } else {
                    StringBuilder sb = new StringBuilder(s.length());
                    sb.append(s, 0, i);
                    sb.append((char) (c + 32));
                    while (true) {
                        i++;
                        if (i >= s.length()) {
                            return sb.toString();
                        }
                        char c2 = s.charAt(i);
                        if ('A' <= c2 && c2 <= 'Z') {
                            c2 = (char) (c2 + 32);
                        }
                        sb.append(c2);
                    }
                }
            }
            return s;
        }
    }

    private CollationLoader() {
    }

    private static void loadRootRules() {
        if (rootRules == null) {
            synchronized (CollationLoader.class) {
                if (rootRules == null) {
                    rootRules = UResourceBundle.getBundleInstance(ICUData.ICU_COLLATION_BASE_NAME, ULocale.ROOT).getString("UCARules");
                }
            }
        }
    }

    public static String getRootRules() {
        loadRootRules();
        return rootRules;
    }

    static String loadRules(ULocale locale, String collationType) {
        ICUResourceBundle data = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_COLLATION_BASE_NAME, locale);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("collations/");
        stringBuilder.append(ASCII.toLowerCase(collationType));
        return data.getWithFallback(stringBuilder.toString()).getString("Sequence");
    }

    private static final UResourceBundle findWithFallback(UResourceBundle table, String entryName) {
        return ((ICUResourceBundle) table).findWithFallback(entryName);
    }

    public static CollationTailoring loadTailoring(ULocale locale, Output<ULocale> outValidLocale) {
        ULocale uLocale = locale;
        Output<ULocale> output = outValidLocale;
        CollationTailoring root = CollationRoot.getRoot();
        String localeName = locale.getName();
        String str;
        if (localeName.length() == 0) {
        } else if (localeName.equals("root")) {
            str = localeName;
        } else {
            UResourceBundle bundle = null;
            try {
                bundle = ICUResourceBundle.getBundleInstance(ICUData.ICU_COLLATION_BASE_NAME, uLocale, OpenType.LOCALE_ROOT);
                ULocale validLocale = bundle.getULocale();
                String validLocaleName = validLocale.getName();
                if (validLocaleName.length() == 0 || validLocaleName.equals("root")) {
                    validLocale = ULocale.ROOT;
                }
                ULocale validLocale2 = validLocale;
                output.value = validLocale2;
                try {
                    UResourceBundle collations = bundle.get("collations");
                    if (collations == null) {
                        return root;
                    }
                    String type = uLocale.getKeywordValue("collation");
                    String defaultType = "standard";
                    String defT = ((ICUResourceBundle) collations).findStringWithFallback("default");
                    if (defT != null) {
                        defaultType = defT;
                    }
                    if (type == null || type.equals("default")) {
                        type = defaultType;
                    } else {
                        type = ASCII.toLowerCase(type);
                    }
                    UResourceBundle data = findWithFallback(collations, type);
                    if (data == null && type.length() > 6 && type.startsWith("search")) {
                        type = "search";
                        data = findWithFallback(collations, type);
                    }
                    if (data == null && !type.equals(defaultType)) {
                        type = defaultType;
                        data = findWithFallback(collations, type);
                    }
                    if (data == null && !type.equals("standard")) {
                        type = "standard";
                        data = findWithFallback(collations, type);
                    }
                    String type2 = type;
                    if (data == null) {
                        return root;
                    }
                    validLocale = data.getULocale();
                    String actualLocaleName = validLocale.getName();
                    if (actualLocaleName.length() == 0 || actualLocaleName.equals("root")) {
                        validLocale = ULocale.ROOT;
                        if (type2.equals("standard") != null) {
                            return root;
                        }
                    }
                    ULocale actualLocale = validLocale;
                    CollationTailoring t = new CollationTailoring(root.settings);
                    t.actualLocale = actualLocale;
                    UResourceBundle binary = data.get("%%CollationBin");
                    ByteBuffer inBytes = binary.getBinary();
                    try {
                        CollationDataReader.read(root, inBytes, t);
                        try {
                            t.setRulesResource(data.get("Sequence"));
                        } catch (MissingResourceException e) {
                        }
                        if (!type2.equals(defaultType)) {
                            output.value = validLocale2.setKeywordValue("collation", type2);
                        }
                        if (actualLocale.equals(validLocale2)) {
                        } else {
                            UResourceBundle actualBundle = UResourceBundle.getBundleInstance(ICUData.ICU_COLLATION_BASE_NAME, actualLocale);
                            defT = ((ICUResourceBundle) actualBundle).findStringWithFallback("collations/default");
                            if (defT != null) {
                                defaultType = defT;
                            }
                        }
                        if (!type2.equals(defaultType)) {
                            t.actualLocale = t.actualLocale.setKeywordValue("collation", type2);
                        }
                        return t;
                    } catch (IOException e2) {
                        ByteBuffer byteBuffer = inBytes;
                        IOException inBytes2 = e2;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to load collation tailoring data for locale:");
                        stringBuilder.append(actualLocale);
                        stringBuilder.append(" type:");
                        stringBuilder.append(type2);
                        throw new ICUUncheckedIOException(stringBuilder.toString(), e2);
                    }
                } catch (MissingResourceException e3) {
                    str = localeName;
                    UResourceBundle uResourceBundle = bundle;
                    return root;
                }
            } catch (MissingResourceException e4) {
                str = localeName;
                output.value = ULocale.ROOT;
                return root;
            }
        }
        output.value = ULocale.ROOT;
        return root;
    }
}
