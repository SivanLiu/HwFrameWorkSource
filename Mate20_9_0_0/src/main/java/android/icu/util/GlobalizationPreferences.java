package android.icu.util;

import android.icu.impl.Utility;
import android.icu.impl.locale.BaseLocale;
import android.icu.text.BreakIterator;
import android.icu.text.Collator;
import android.icu.text.DateFormat;
import android.icu.text.NumberFormat;
import android.icu.text.SimpleDateFormat;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class GlobalizationPreferences implements Freezable<GlobalizationPreferences> {
    public static final int BI_CHARACTER = 0;
    private static final int BI_LIMIT = 5;
    public static final int BI_LINE = 2;
    public static final int BI_SENTENCE = 3;
    public static final int BI_TITLE = 4;
    public static final int BI_WORD = 1;
    public static final int DF_FULL = 0;
    private static final int DF_LIMIT = 5;
    public static final int DF_LONG = 1;
    public static final int DF_MEDIUM = 2;
    public static final int DF_NONE = 4;
    public static final int DF_SHORT = 3;
    public static final int ID_CURRENCY = 7;
    public static final int ID_CURRENCY_SYMBOL = 8;
    public static final int ID_KEYWORD = 5;
    public static final int ID_KEYWORD_VALUE = 6;
    public static final int ID_LANGUAGE = 1;
    public static final int ID_LOCALE = 0;
    public static final int ID_SCRIPT = 2;
    public static final int ID_TERRITORY = 3;
    public static final int ID_TIMEZONE = 9;
    public static final int ID_VARIANT = 4;
    public static final int NF_CURRENCY = 1;
    public static final int NF_INTEGER = 4;
    private static final int NF_LIMIT = 5;
    public static final int NF_NUMBER = 0;
    public static final int NF_PERCENT = 2;
    public static final int NF_SCIENTIFIC = 3;
    private static final int TYPE_BREAKITERATOR = 5;
    private static final int TYPE_CALENDAR = 1;
    private static final int TYPE_COLLATOR = 4;
    private static final int TYPE_DATEFORMAT = 2;
    private static final int TYPE_GENERIC = 0;
    private static final int TYPE_LIMIT = 6;
    private static final int TYPE_NUMBERFORMAT = 3;
    private static final HashMap<ULocale, BitSet> available_locales = new HashMap();
    private static final String[][] language_territory_hack = new String[][]{new String[]{"af", "ZA"}, new String[]{"am", "ET"}, new String[]{"ar", "SA"}, new String[]{"as", "IN"}, new String[]{"ay", "PE"}, new String[]{"az", "AZ"}, new String[]{"bal", "PK"}, new String[]{"be", "BY"}, new String[]{"bg", "BG"}, new String[]{"bn", "IN"}, new String[]{"bs", "BA"}, new String[]{"ca", "ES"}, new String[]{"ch", "MP"}, new String[]{"cpe", "SL"}, new String[]{"cs", "CZ"}, new String[]{"cy", "GB"}, new String[]{"da", "DK"}, new String[]{"de", "DE"}, new String[]{"dv", "MV"}, new String[]{"dz", "BT"}, new String[]{"el", "GR"}, new String[]{"en", "US"}, new String[]{"es", "ES"}, new String[]{"et", "EE"}, new String[]{"eu", "ES"}, new String[]{"fa", "IR"}, new String[]{"fi", "FI"}, new String[]{"fil", "PH"}, new String[]{"fj", "FJ"}, new String[]{"fo", "FO"}, new String[]{"fr", "FR"}, new String[]{"ga", "IE"}, new String[]{"gd", "GB"}, new String[]{"gl", "ES"}, new String[]{"gn", "PY"}, new String[]{"gu", "IN"}, new String[]{"gv", "GB"}, new String[]{"ha", "NG"}, new String[]{"he", "IL"}, new String[]{"hi", "IN"}, new String[]{"ho", "PG"}, new String[]{"hr", "HR"}, new String[]{"ht", "HT"}, new String[]{"hu", "HU"}, new String[]{"hy", "AM"}, new String[]{"id", "ID"}, new String[]{"is", "IS"}, new String[]{"it", "IT"}, new String[]{"ja", "JP"}, new String[]{"ka", "GE"}, new String[]{"kk", "KZ"}, new String[]{"kl", "GL"}, new String[]{"km", "KH"}, new String[]{"kn", "IN"}, new String[]{"ko", "KR"}, new String[]{"kok", "IN"}, new String[]{"ks", "IN"}, new String[]{"ku", "TR"}, new String[]{"ky", "KG"}, new String[]{"la", "VA"}, new String[]{"lb", "LU"}, new String[]{"ln", "CG"}, new String[]{"lo", "LA"}, new String[]{"lt", "LT"}, new String[]{"lv", "LV"}, new String[]{"mai", "IN"}, new String[]{"men", "GN"}, new String[]{"mg", "MG"}, new String[]{"mh", "MH"}, new String[]{"mk", "MK"}, new String[]{"ml", "IN"}, new String[]{"mn", "MN"}, new String[]{"mni", "IN"}, new String[]{"mo", "MD"}, new String[]{"mr", "IN"}, new String[]{DateFormat.MINUTE_SECOND, "MY"}, new String[]{"mt", "MT"}, new String[]{"my", "MM"}, new String[]{"na", "NR"}, new String[]{"nb", "NO"}, new String[]{"nd", "ZA"}, new String[]{"ne", "NP"}, new String[]{"niu", "NU"}, new String[]{"nl", "NL"}, new String[]{"nn", "NO"}, new String[]{"no", "NO"}, new String[]{"nr", "ZA"}, new String[]{"nso", "ZA"}, new String[]{"ny", "MW"}, new String[]{"om", "KE"}, new String[]{"or", "IN"}, new String[]{"pa", "IN"}, new String[]{"pau", "PW"}, new String[]{"pl", "PL"}, new String[]{"ps", "PK"}, new String[]{"pt", "BR"}, new String[]{"qu", "PE"}, new String[]{"rn", "BI"}, new String[]{"ro", "RO"}, new String[]{"ru", "RU"}, new String[]{"rw", "RW"}, new String[]{"sd", "IN"}, new String[]{"sg", "CF"}, new String[]{"si", "LK"}, new String[]{"sk", "SK"}, new String[]{"sl", "SI"}, new String[]{"sm", "WS"}, new String[]{"so", "DJ"}, new String[]{"sq", "CS"}, new String[]{"sr", "CS"}, new String[]{"ss", "ZA"}, new String[]{"st", "ZA"}, new String[]{"sv", "SE"}, new String[]{"sw", "KE"}, new String[]{"ta", "IN"}, new String[]{"te", "IN"}, new String[]{"tem", "SL"}, new String[]{"tet", "TL"}, new String[]{"th", "TH"}, new String[]{"ti", "ET"}, new String[]{"tg", "TJ"}, new String[]{"tk", "TM"}, new String[]{"tkl", "TK"}, new String[]{"tvl", "TV"}, new String[]{"tl", "PH"}, new String[]{"tn", "ZA"}, new String[]{"to", "TO"}, new String[]{"tpi", "PG"}, new String[]{"tr", "TR"}, new String[]{"ts", "ZA"}, new String[]{"uk", "UA"}, new String[]{"ur", "IN"}, new String[]{"uz", "UZ"}, new String[]{"ve", "ZA"}, new String[]{"vi", "VN"}, new String[]{"wo", "SN"}, new String[]{"xh", "ZA"}, new String[]{"zh", "CN"}, new String[]{"zh_Hant", "TW"}, new String[]{"zu", "ZA"}, new String[]{"aa", "ET"}, new String[]{"byn", "ER"}, new String[]{"eo", "DE"}, new String[]{"gez", "ET"}, new String[]{"haw", "US"}, new String[]{"iu", "CA"}, new String[]{"kw", "GB"}, new String[]{"sa", "IN"}, new String[]{"sh", "HR"}, new String[]{"sid", "ET"}, new String[]{"syr", "SY"}, new String[]{"tig", "ER"}, new String[]{"tt", "RU"}, new String[]{"wal", "ET"}};
    private static final Map<String, String> language_territory_hack_map = new HashMap();
    static final String[][] territory_tzid_hack = new String[][]{new String[]{"AQ", "Antarctica/McMurdo"}, new String[]{"AR", "America/Buenos_Aires"}, new String[]{"AU", "Australia/Sydney"}, new String[]{"BR", "America/Sao_Paulo"}, new String[]{"CA", "America/Toronto"}, new String[]{"CD", "Africa/Kinshasa"}, new String[]{"CL", "America/Santiago"}, new String[]{"CN", "Asia/Shanghai"}, new String[]{"EC", "America/Guayaquil"}, new String[]{"ES", "Europe/Madrid"}, new String[]{"GB", "Europe/London"}, new String[]{"GL", "America/Godthab"}, new String[]{"ID", "Asia/Jakarta"}, new String[]{"ML", "Africa/Bamako"}, new String[]{"MX", "America/Mexico_City"}, new String[]{"MY", "Asia/Kuala_Lumpur"}, new String[]{"NZ", "Pacific/Auckland"}, new String[]{"PT", "Europe/Lisbon"}, new String[]{"RU", "Europe/Moscow"}, new String[]{"UA", "Europe/Kiev"}, new String[]{"US", "America/New_York"}, new String[]{"UZ", "Asia/Tashkent"}, new String[]{"PF", "Pacific/Tahiti"}, new String[]{"FM", "Pacific/Kosrae"}, new String[]{"KI", "Pacific/Tarawa"}, new String[]{"KZ", "Asia/Almaty"}, new String[]{"MH", "Pacific/Majuro"}, new String[]{"MN", "Asia/Ulaanbaatar"}, new String[]{"SJ", "Arctic/Longyearbyen"}, new String[]{"UM", "Pacific/Midway"}};
    static final Map<String, String> territory_tzid_hack_map = new HashMap();
    private BreakIterator[] breakIterators;
    private Calendar calendar;
    private Collator collator;
    private Currency currency;
    private DateFormat[][] dateFormats;
    private volatile boolean frozen;
    private List<ULocale> implicitLocales;
    private List<ULocale> locales;
    private NumberFormat[] numberFormats;
    private String territory;
    private TimeZone timezone;

    public GlobalizationPreferences() {
        reset();
    }

    public GlobalizationPreferences setLocales(List<ULocale> inputLocales) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.locales = processLocales(inputLocales);
        return this;
    }

    public List<ULocale> getLocales() {
        if (this.locales == null) {
            return guessLocales();
        }
        List<ULocale> result = new ArrayList();
        result.addAll(this.locales);
        return result;
    }

    public ULocale getLocale(int index) {
        List<ULocale> lcls = this.locales;
        if (lcls == null) {
            lcls = guessLocales();
        }
        if (index < 0 || index >= lcls.size()) {
            return null;
        }
        return (ULocale) lcls.get(index);
    }

    public GlobalizationPreferences setLocales(ULocale[] uLocales) {
        if (!isFrozen()) {
            return setLocales(Arrays.asList(uLocales));
        }
        throw new UnsupportedOperationException("Attempt to modify immutable object");
    }

    public GlobalizationPreferences setLocale(ULocale uLocale) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        return setLocales(new ULocale[]{uLocale});
    }

    public GlobalizationPreferences setLocales(String acceptLanguageString) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        try {
            return setLocales(ULocale.parseAcceptLanguage(acceptLanguageString, true));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid Accept-Language string");
        }
    }

    public ResourceBundle getResourceBundle(String baseName) {
        return getResourceBundle(baseName, null);
    }

    public ResourceBundle getResourceBundle(String baseName, ClassLoader loader) {
        UResourceBundle urb = null;
        UResourceBundle candidate = null;
        String actualLocaleName = null;
        List<ULocale> fallbacks = getLocales();
        for (int i = 0; i < fallbacks.size(); i++) {
            String localeName = ((ULocale) fallbacks.get(i)).toString();
            if (actualLocaleName != null && localeName.equals(actualLocaleName)) {
                urb = candidate;
                break;
            }
            if (loader == null) {
                try {
                    candidate = UResourceBundle.getBundleInstance(baseName, localeName);
                } catch (MissingResourceException e) {
                    actualLocaleName = null;
                }
            } else {
                candidate = UResourceBundle.getBundleInstance(baseName, localeName, loader);
            }
            if (candidate != null) {
                actualLocaleName = candidate.getULocale().getName();
                if (actualLocaleName.equals(localeName) != null) {
                    urb = candidate;
                    break;
                } else if (urb == null) {
                    urb = candidate;
                }
            } else {
                continue;
            }
        }
        if (urb != null) {
            return urb;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't find bundle for base name ");
        stringBuilder.append(baseName);
        throw new MissingResourceException(stringBuilder.toString(), baseName, "");
    }

    public GlobalizationPreferences setTerritory(String territory) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.territory = territory;
        return this;
    }

    public String getTerritory() {
        if (this.territory == null) {
            return guessTerritory();
        }
        return this.territory;
    }

    public GlobalizationPreferences setCurrency(Currency currency) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.currency = currency;
        return this;
    }

    public Currency getCurrency() {
        if (this.currency == null) {
            return guessCurrency();
        }
        return this.currency;
    }

    public GlobalizationPreferences setCalendar(Calendar calendar) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.calendar = (Calendar) calendar.clone();
        return this;
    }

    public Calendar getCalendar() {
        if (this.calendar == null) {
            return guessCalendar();
        }
        Calendar temp = (Calendar) this.calendar.clone();
        temp.setTimeZone(getTimeZone());
        temp.setTimeInMillis(System.currentTimeMillis());
        return temp;
    }

    public GlobalizationPreferences setTimeZone(TimeZone timezone) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.timezone = (TimeZone) timezone.clone();
        return this;
    }

    public TimeZone getTimeZone() {
        if (this.timezone == null) {
            return guessTimeZone();
        }
        return this.timezone.cloneAsThawed();
    }

    public Collator getCollator() {
        if (this.collator == null) {
            return guessCollator();
        }
        try {
            return (Collator) this.collator.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("Error in cloning collator", e);
        }
    }

    public GlobalizationPreferences setCollator(Collator collator) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        try {
            this.collator = (Collator) collator.clone();
            return this;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("Error in cloning collator", e);
        }
    }

    public BreakIterator getBreakIterator(int type) {
        if (type < 0 || type >= 5) {
            throw new IllegalArgumentException("Illegal break iterator type");
        } else if (this.breakIterators == null || this.breakIterators[type] == null) {
            return guessBreakIterator(type);
        } else {
            return (BreakIterator) this.breakIterators[type].clone();
        }
    }

    public GlobalizationPreferences setBreakIterator(int type, BreakIterator iterator) {
        if (type < 0 || type >= 5) {
            throw new IllegalArgumentException("Illegal break iterator type");
        } else if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        } else {
            if (this.breakIterators == null) {
                this.breakIterators = new BreakIterator[5];
            }
            this.breakIterators[type] = (BreakIterator) iterator.clone();
            return this;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:44:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0120 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getDisplayName(String id, int type) {
        String result = id;
        for (ULocale locale : getLocales()) {
            int i = 0;
            if (isAvailableLocale(locale, 0)) {
                StringBuilder stringBuilder;
                switch (type) {
                    case 0:
                        result = ULocale.getDisplayName(id, locale);
                    case 1:
                        result = ULocale.getDisplayLanguage(id, locale);
                    case 2:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("und-");
                        stringBuilder.append(id);
                        result = ULocale.getDisplayScript(stringBuilder.toString(), locale);
                    case 3:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("und-");
                        stringBuilder.append(id);
                        result = ULocale.getDisplayCountry(stringBuilder.toString(), locale);
                    case 4:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("und-QQ-");
                        stringBuilder.append(id);
                        result = ULocale.getDisplayVariant(stringBuilder.toString(), locale);
                    case 5:
                        result = ULocale.getDisplayKeyword(id, locale);
                    case 6:
                        String[] parts = new String[2];
                        Utility.split(id, '=', parts);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("und@");
                        stringBuilder2.append(id);
                        result = ULocale.getDisplayKeywordValue(stringBuilder2.toString(), parts[0], locale);
                        if (result.equals(parts[1])) {
                            continue;
                        }
                    case 7:
                    case 8:
                        Currency temp = new Currency(id);
                        if (type == 7) {
                            i = 1;
                        }
                        result = temp.getName(locale, i, new boolean[1]);
                        if (!id.equals(result)) {
                            break;
                        }
                        return result;
                    case 9:
                        SimpleDateFormat dtf = new SimpleDateFormat(DateFormat.GENERIC_TZ, locale);
                        dtf.setTimeZone(TimeZone.getFrozenTimeZone(id));
                        result = dtf.format(new Date());
                        boolean isBadStr = false;
                        String teststr = result;
                        int sidx = result.indexOf(40);
                        int eidx = result.indexOf(41);
                        if (!(sidx == -1 || eidx == -1 || eidx - sidx != 3)) {
                            teststr = result.substring(sidx + 1, eidx);
                        }
                        if (teststr.length() == 2) {
                            isBadStr = true;
                            while (i < 2) {
                                char c = teststr.charAt(i);
                                if (c < 'A' || 'Z' < c) {
                                    isBadStr = false;
                                } else {
                                    i++;
                                }
                            }
                        }
                        if (isBadStr) {
                            continue;
                        }
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown type: ");
                        stringBuilder.append(type);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
                if (!id.equals(result)) {
                }
            }
        }
        return result;
    }

    public GlobalizationPreferences setDateFormat(int dateStyle, int timeStyle, DateFormat format) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (this.dateFormats == null) {
            this.dateFormats = (DateFormat[][]) Array.newInstance(DateFormat.class, new int[]{5, 5});
        }
        this.dateFormats[dateStyle][timeStyle] = (DateFormat) format.clone();
        return this;
    }

    public DateFormat getDateFormat(int dateStyle, int timeStyle) {
        if (!(dateStyle == 4 && timeStyle == 4) && dateStyle >= 0 && dateStyle < 5 && timeStyle >= 0 && timeStyle < 5) {
            DateFormat result = null;
            if (this.dateFormats != null) {
                result = this.dateFormats[dateStyle][timeStyle];
            }
            if (result == null) {
                return guessDateFormat(dateStyle, timeStyle);
            }
            result = (DateFormat) result.clone();
            result.setTimeZone(getTimeZone());
            return result;
        }
        throw new IllegalArgumentException("Illegal date format style arguments");
    }

    public NumberFormat getNumberFormat(int style) {
        if (style < 0 || style >= 5) {
            throw new IllegalArgumentException("Illegal number format type");
        }
        NumberFormat result = null;
        if (this.numberFormats != null) {
            result = this.numberFormats[style];
        }
        if (result != null) {
            return (NumberFormat) result.clone();
        }
        return guessNumberFormat(style);
    }

    public GlobalizationPreferences setNumberFormat(int style, NumberFormat format) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (this.numberFormats == null) {
            this.numberFormats = new NumberFormat[5];
        }
        this.numberFormats[style] = (NumberFormat) format.clone();
        return this;
    }

    public GlobalizationPreferences reset() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.locales = null;
        this.territory = null;
        this.calendar = null;
        this.collator = null;
        this.breakIterators = null;
        this.timezone = null;
        this.currency = null;
        this.dateFormats = null;
        this.numberFormats = null;
        this.implicitLocales = null;
        return this;
    }

    protected List<ULocale> processLocales(List<ULocale> inputLocales) {
        ULocale uloc;
        ULocale uloc2;
        List<ULocale> result = new ArrayList();
        int index = 0;
        for (int i = 0; i < inputLocales.size(); i++) {
            uloc = (ULocale) inputLocales.get(i);
            String language = uloc.getLanguage();
            String script = uloc.getScript();
            String country = uloc.getCountry();
            String variant = uloc.getVariant();
            boolean bInserted = false;
            for (int j = 0; j < result.size(); j++) {
                ULocale u = (ULocale) result.get(j);
                if (u.getLanguage().equals(language)) {
                    String s = u.getScript();
                    String c = u.getCountry();
                    String v = u.getVariant();
                    if (!s.equals(script)) {
                        if (s.length() != 0 || c.length() != 0 || v.length() != 0) {
                            if (s.length() != 0 || !c.equals(country)) {
                                if (script.length() == 0 && country.length() > 0 && c.length() == 0) {
                                    result.add(j, uloc);
                                    bInserted = true;
                                    break;
                                }
                            }
                            result.add(j, uloc);
                            bInserted = true;
                            break;
                        }
                        result.add(j, uloc);
                        bInserted = true;
                        break;
                    } else if (c.equals(country) || c.length() != 0 || v.length() != 0) {
                        if (!v.equals(variant) && v.length() == 0) {
                            result.add(j, uloc);
                            bInserted = true;
                            break;
                        }
                    } else {
                        result.add(j, uloc);
                        bInserted = true;
                        break;
                    }
                }
            }
            if (!bInserted) {
                result.add(uloc);
            }
        }
        List<ULocale> list = inputLocales;
        while (index < result.size()) {
            uloc2 = (ULocale) result.get(index);
            while (true) {
                uloc = uloc2.getFallback();
                uloc2 = uloc;
                if (uloc == null || uloc2.getLanguage().length() == 0) {
                    index++;
                } else {
                    index++;
                    result.add(index, uloc2);
                }
            }
            index++;
        }
        index = 0;
        while (index < result.size() - 1) {
            uloc2 = (ULocale) result.get(index);
            boolean bRemoved = false;
            for (int i2 = index + 1; i2 < result.size(); i2++) {
                if (uloc2.equals(result.get(i2))) {
                    result.remove(index);
                    bRemoved = true;
                    break;
                }
            }
            if (!bRemoved) {
                index++;
            }
        }
        return result;
    }

    protected DateFormat guessDateFormat(int dateStyle, int timeStyle) {
        ULocale dfLocale = getAvailableLocale(2);
        if (dfLocale == null) {
            dfLocale = ULocale.ROOT;
        }
        if (timeStyle == 4) {
            return DateFormat.getDateInstance(getCalendar(), dateStyle, dfLocale);
        }
        if (dateStyle == 4) {
            return DateFormat.getTimeInstance(getCalendar(), timeStyle, dfLocale);
        }
        return DateFormat.getDateTimeInstance(getCalendar(), dateStyle, timeStyle, dfLocale);
    }

    protected NumberFormat guessNumberFormat(int style) {
        ULocale nfLocale = getAvailableLocale(3);
        if (nfLocale == null) {
            nfLocale = ULocale.ROOT;
        }
        switch (style) {
            case 0:
                return NumberFormat.getInstance(nfLocale);
            case 1:
                NumberFormat result = NumberFormat.getCurrencyInstance(nfLocale);
                result.setCurrency(getCurrency());
                return result;
            case 2:
                return NumberFormat.getPercentInstance(nfLocale);
            case 3:
                return NumberFormat.getScientificInstance(nfLocale);
            case 4:
                return NumberFormat.getIntegerInstance(nfLocale);
            default:
                throw new IllegalArgumentException("Unknown number format style");
        }
    }

    protected String guessTerritory() {
        String result;
        for (ULocale locale : getLocales()) {
            result = locale.getCountry();
            if (result.length() != 0) {
                return result;
            }
        }
        ULocale firstLocale = getLocale(null);
        String language = firstLocale.getLanguage();
        result = firstLocale.getScript();
        String result2 = null;
        if (result.length() != 0) {
            Map map = language_territory_hack_map;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(language);
            stringBuilder.append(BaseLocale.SEP);
            stringBuilder.append(result);
            result2 = (String) map.get(stringBuilder.toString());
        }
        if (result2 == null) {
            result2 = (String) language_territory_hack_map.get(language);
        }
        if (result2 == null) {
            result2 = "US";
        }
        return result2;
    }

    protected Currency guessCurrency() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("und-");
        stringBuilder.append(getTerritory());
        return Currency.getInstance(new ULocale(stringBuilder.toString()));
    }

    protected List<ULocale> guessLocales() {
        if (this.implicitLocales == null) {
            List<ULocale> result = new ArrayList(1);
            result.add(ULocale.getDefault());
            this.implicitLocales = processLocales(result);
        }
        return this.implicitLocales;
    }

    protected Collator guessCollator() {
        ULocale collLocale = getAvailableLocale(4);
        if (collLocale == null) {
            collLocale = ULocale.ROOT;
        }
        return Collator.getInstance(collLocale);
    }

    protected BreakIterator guessBreakIterator(int type) {
        ULocale brkLocale = getAvailableLocale(5);
        if (brkLocale == null) {
            brkLocale = ULocale.ROOT;
        }
        switch (type) {
            case 0:
                return BreakIterator.getCharacterInstance(brkLocale);
            case 1:
                return BreakIterator.getWordInstance(brkLocale);
            case 2:
                return BreakIterator.getLineInstance(brkLocale);
            case 3:
                return BreakIterator.getSentenceInstance(brkLocale);
            case 4:
                return BreakIterator.getTitleInstance(brkLocale);
            default:
                throw new IllegalArgumentException("Unknown break iterator type");
        }
    }

    protected TimeZone guessTimeZone() {
        String timezoneString = (String) territory_tzid_hack_map.get(getTerritory());
        if (timezoneString == null) {
            String[] attempt = TimeZone.getAvailableIDs(getTerritory());
            if (attempt.length == 0) {
                timezoneString = "Etc/GMT";
            } else {
                int i = 0;
                while (i < attempt.length && attempt[i].indexOf("/") < 0) {
                    i++;
                }
                if (i > attempt.length) {
                    i = 0;
                }
                timezoneString = attempt[i];
            }
        }
        return TimeZone.getTimeZone(timezoneString);
    }

    protected Calendar guessCalendar() {
        ULocale calLocale = getAvailableLocale(1);
        if (calLocale == null) {
            calLocale = ULocale.US;
        }
        return Calendar.getInstance(getTimeZone(), calLocale);
    }

    private ULocale getAvailableLocale(int type) {
        List<ULocale> locs = getLocales();
        for (int i = 0; i < locs.size(); i++) {
            ULocale l = (ULocale) locs.get(i);
            if (isAvailableLocale(l, type)) {
                return l;
            }
        }
        return null;
    }

    private boolean isAvailableLocale(ULocale loc, int type) {
        BitSet bits = (BitSet) available_locales.get(loc);
        if (bits == null || !bits.get(type)) {
            return false;
        }
        return true;
    }

    static {
        int i;
        ULocale[] allLocales = ULocale.getAvailableLocales();
        for (Object put : allLocales) {
            BitSet bits = new BitSet(6);
            available_locales.put(put, bits);
            bits.set(0);
        }
        ULocale[] calLocales = Calendar.getAvailableULocales();
        for (int i2 = 0; i2 < calLocales.length; i2++) {
            BitSet bits2 = (BitSet) available_locales.get(calLocales[i2]);
            if (bits2 == null) {
                bits2 = new BitSet(6);
                available_locales.put(allLocales[i2], bits2);
            }
            bits2.set(1);
        }
        ULocale[] dateLocales = DateFormat.getAvailableULocales();
        for (int i3 = 0; i3 < dateLocales.length; i3++) {
            BitSet bits3 = (BitSet) available_locales.get(dateLocales[i3]);
            if (bits3 == null) {
                bits3 = new BitSet(6);
                available_locales.put(allLocales[i3], bits3);
            }
            bits3.set(2);
        }
        ULocale[] numLocales = NumberFormat.getAvailableULocales();
        for (int i4 = 0; i4 < numLocales.length; i4++) {
            BitSet bits4 = (BitSet) available_locales.get(numLocales[i4]);
            if (bits4 == null) {
                bits4 = new BitSet(6);
                available_locales.put(allLocales[i4], bits4);
            }
            bits4.set(3);
        }
        ULocale[] collLocales = Collator.getAvailableULocales();
        for (int i5 = 0; i5 < collLocales.length; i5++) {
            BitSet bits5 = (BitSet) available_locales.get(collLocales[i5]);
            if (bits5 == null) {
                bits5 = new BitSet(6);
                available_locales.put(allLocales[i5], bits5);
            }
            bits5.set(4);
        }
        ULocale[] brkLocales = BreakIterator.getAvailableULocales();
        for (Object obj : brkLocales) {
            ((BitSet) available_locales.get(obj)).set(5);
        }
        for (i = 0; i < language_territory_hack.length; i++) {
            language_territory_hack_map.put(language_territory_hack[i][0], language_territory_hack[i][1]);
        }
        for (i = 0; i < territory_tzid_hack.length; i++) {
            territory_tzid_hack_map.put(territory_tzid_hack[i][0], territory_tzid_hack[i][1]);
        }
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    public GlobalizationPreferences freeze() {
        this.frozen = true;
        return this;
    }

    public GlobalizationPreferences cloneAsThawed() {
        try {
            GlobalizationPreferences result = (GlobalizationPreferences) clone();
            result.frozen = false;
            return result;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
