package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.NumberParseException.ErrorType;
import com.android.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.android.i18n.phonenumbers.Phonemetadata.NumberFormat.Builder;
import com.android.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.android.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource;
import com.android.i18n.phonenumbers.internal.MatcherApi;
import com.android.i18n.phonenumbers.internal.RegexBasedMatcher;
import com.android.i18n.phonenumbers.internal.RegexCache;
import gov.nist.core.Separators;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sip.header.WarningHeader;

public class PhoneNumberUtil {
    private static final Map<Character, Character> ALL_PLUS_NUMBER_GROUPING_SYMBOLS;
    private static final Map<Character, Character> ALPHA_MAPPINGS;
    private static final Map<Character, Character> ALPHA_PHONE_MAPPINGS;
    private static final Pattern CAPTURING_DIGIT_PATTERN = Pattern.compile("(\\p{Nd})");
    private static final String CAPTURING_EXTN_DIGITS = "(\\p{Nd}{1,7})";
    private static final String CC_STRING = "$CC";
    private static final String COLOMBIA_MOBILE_TO_FIXED_LINE_PREFIX = "3";
    private static final String DEFAULT_EXTN_PREFIX = " ext. ";
    private static final Map<Character, Character> DIALLABLE_CHAR_MAPPINGS;
    private static final String DIGITS = "\\p{Nd}";
    private static final Pattern EXTN_PATTERN;
    static final String EXTN_PATTERNS_FOR_MATCHING;
    private static final String EXTN_PATTERNS_FOR_PARSING;
    private static final String FG_STRING = "$FG";
    private static final Pattern FIRST_GROUP_ONLY_PREFIX_PATTERN = Pattern.compile("\\(?\\$1\\)?");
    private static final Pattern FIRST_GROUP_PATTERN = Pattern.compile("(\\$\\d)");
    private static final Set<Integer> GEO_MOBILE_COUNTRIES;
    private static final Set<Integer> GEO_MOBILE_COUNTRIES_WITHOUT_MOBILE_AREA_CODES;
    private static final int MAX_INPUT_STRING_LENGTH = 250;
    static final int MAX_LENGTH_COUNTRY_CODE = 3;
    static final int MAX_LENGTH_FOR_NSN = 17;
    private static final int MIN_LENGTH_FOR_NSN = 2;
    private static final Map<Integer, String> MOBILE_TOKEN_MAPPINGS;
    private static final int NANPA_COUNTRY_CODE = 1;
    static final Pattern NON_DIGITS_PATTERN = Pattern.compile("(\\D+)");
    private static final String NP_STRING = "$NP";
    static final String PLUS_CHARS = "+＋";
    static final Pattern PLUS_CHARS_PATTERN = Pattern.compile("[+＋]+");
    static final char PLUS_SIGN = '+';
    static final int REGEX_FLAGS = 66;
    public static final String REGION_CODE_FOR_NON_GEO_ENTITY = "001";
    private static final String RFC3966_EXTN_PREFIX = ";ext=";
    private static final String RFC3966_ISDN_SUBADDRESS = ";isub=";
    private static final String RFC3966_PHONE_CONTEXT = ";phone-context=";
    private static final String RFC3966_PREFIX = "tel:";
    private static final String SECOND_NUMBER_START = "[\\\\/] *x";
    static final Pattern SECOND_NUMBER_START_PATTERN = Pattern.compile(SECOND_NUMBER_START);
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[-x‐-―−ー－-／  ­​⁠　()（）［］.\\[\\]/~⁓∼～]+");
    private static final Pattern SINGLE_INTERNATIONAL_PREFIX = Pattern.compile("[\\d]+(?:[~⁓∼～][\\d]+)?");
    private static final char STAR_SIGN = '*';
    private static final String UNKNOWN_REGION = "ZZ";
    private static final String UNWANTED_END_CHARS = "[[\\P{N}&&\\P{L}]&&[^#]]+$";
    static final Pattern UNWANTED_END_CHAR_PATTERN = Pattern.compile(UNWANTED_END_CHARS);
    private static final String VALID_ALPHA;
    private static final Pattern VALID_ALPHA_PHONE_PATTERN = Pattern.compile("(?:.*?[A-Za-z]){3}.*");
    private static final String VALID_PHONE_NUMBER;
    private static final Pattern VALID_PHONE_NUMBER_PATTERN;
    static final String VALID_PUNCTUATION = "-x‐-―−ー－-／  ­​⁠　()（）［］.\\[\\]/~⁓∼～";
    private static final String VALID_START_CHAR = "[+＋\\p{Nd}]";
    private static final Pattern VALID_START_CHAR_PATTERN = Pattern.compile(VALID_START_CHAR);
    private static PhoneNumberUtil instance = null;
    private static final Logger logger = Logger.getLogger(PhoneNumberUtil.class.getName());
    private final Map<Integer, List<String>> countryCallingCodeToRegionCodeMap;
    private final Set<Integer> countryCodesForNonGeographicalRegion = new HashSet();
    private final MatcherApi matcherApi = RegexBasedMatcher.create();
    private final MetadataSource metadataSource;
    private final Set<String> nanpaRegions = new HashSet(35);
    private final RegexCache regexCache = new RegexCache(100);
    private final Set<String> supportedRegions = new HashSet(320);

    /* renamed from: com.android.i18n.phonenumbers.PhoneNumberUtil$2 */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType = new int[PhoneNumberType.values().length];

        static {
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.PREMIUM_RATE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.TOLL_FREE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.MOBILE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.FIXED_LINE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.FIXED_LINE_OR_MOBILE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.SHARED_COST.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.VOIP.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.PERSONAL_NUMBER.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.PAGER.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.UAN.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[PhoneNumberType.VOICEMAIL.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberFormat = new int[PhoneNumberFormat.values().length];
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberFormat[PhoneNumberFormat.E164.ordinal()] = 1;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberFormat[PhoneNumberFormat.INTERNATIONAL.ordinal()] = 2;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberFormat[PhoneNumberFormat.RFC3966.ordinal()] = 3;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberFormat[PhoneNumberFormat.NATIONAL.ordinal()] = 4;
            } catch (NoSuchFieldError e15) {
            }
            $SwitchMap$com$google$i18n$phonenumbers$Phonenumber$PhoneNumber$CountryCodeSource = new int[CountryCodeSource.values().length];
            try {
                $SwitchMap$com$google$i18n$phonenumbers$Phonenumber$PhoneNumber$CountryCodeSource[CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN.ordinal()] = 1;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$Phonenumber$PhoneNumber$CountryCodeSource[CountryCodeSource.FROM_NUMBER_WITH_IDD.ordinal()] = 2;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$Phonenumber$PhoneNumber$CountryCodeSource[CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN.ordinal()] = 3;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$com$google$i18n$phonenumbers$Phonenumber$PhoneNumber$CountryCodeSource[CountryCodeSource.FROM_DEFAULT_COUNTRY.ordinal()] = 4;
            } catch (NoSuchFieldError e19) {
            }
        }
    }

    public enum Leniency {
        POSSIBLE {
            boolean verify(PhoneNumber number, CharSequence candidate, PhoneNumberUtil util) {
                return util.isPossibleNumber(number);
            }
        },
        VALID {
            boolean verify(PhoneNumber number, CharSequence candidate, PhoneNumberUtil util) {
                if (util.isValidNumber(number) && PhoneNumberMatcher.containsOnlyValidXChars(number, candidate.toString(), util)) {
                    return PhoneNumberMatcher.isNationalPrefixPresentIfRequired(number, util);
                }
                return false;
            }
        },
        STRICT_GROUPING {
            boolean verify(PhoneNumber number, CharSequence candidate, PhoneNumberUtil util) {
                String candidateString = candidate.toString();
                if (util.isValidNumber(number) && PhoneNumberMatcher.containsOnlyValidXChars(number, candidateString, util) && !PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidateString) && PhoneNumberMatcher.isNationalPrefixPresentIfRequired(number, util)) {
                    return PhoneNumberMatcher.checkNumberGroupingIsValid(number, candidate, util, new NumberGroupingChecker() {
                        public boolean checkGroups(PhoneNumberUtil util, PhoneNumber number, StringBuilder normalizedCandidate, String[] expectedNumberGroups) {
                            return PhoneNumberMatcher.allNumberGroupsRemainGrouped(util, number, normalizedCandidate, expectedNumberGroups);
                        }
                    });
                }
                return false;
            }
        },
        EXACT_GROUPING {
            boolean verify(PhoneNumber number, CharSequence candidate, PhoneNumberUtil util) {
                String candidateString = candidate.toString();
                if (util.isValidNumber(number) && PhoneNumberMatcher.containsOnlyValidXChars(number, candidateString, util) && !PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidateString) && PhoneNumberMatcher.isNationalPrefixPresentIfRequired(number, util)) {
                    return PhoneNumberMatcher.checkNumberGroupingIsValid(number, candidate, util, new NumberGroupingChecker() {
                        public boolean checkGroups(PhoneNumberUtil util, PhoneNumber number, StringBuilder normalizedCandidate, String[] expectedNumberGroups) {
                            return PhoneNumberMatcher.allNumberGroupsAreExactlyPresent(util, number, normalizedCandidate, expectedNumberGroups);
                        }
                    });
                }
                return false;
            }
        };

        abstract boolean verify(PhoneNumber phoneNumber, CharSequence charSequence, PhoneNumberUtil phoneNumberUtil);
    }

    public enum MatchType {
        NOT_A_NUMBER,
        NO_MATCH,
        SHORT_NSN_MATCH,
        NSN_MATCH,
        EXACT_MATCH
    }

    public enum PhoneNumberFormat {
        E164,
        INTERNATIONAL,
        NATIONAL,
        RFC3966
    }

    public enum PhoneNumberType {
        FIXED_LINE,
        MOBILE,
        FIXED_LINE_OR_MOBILE,
        TOLL_FREE,
        PREMIUM_RATE,
        SHARED_COST,
        VOIP,
        PERSONAL_NUMBER,
        PAGER,
        UAN,
        VOICEMAIL,
        UNKNOWN
    }

    public enum ValidationResult {
        IS_POSSIBLE,
        IS_POSSIBLE_LOCAL_ONLY,
        INVALID_COUNTRY_CODE,
        TOO_SHORT,
        INVALID_LENGTH,
        TOO_LONG
    }

    static {
        HashMap<Integer, String> mobileTokenMap = new HashMap();
        mobileTokenMap.put(Integer.valueOf(52), "1");
        mobileTokenMap.put(Integer.valueOf(54), "9");
        MOBILE_TOKEN_MAPPINGS = Collections.unmodifiableMap(mobileTokenMap);
        HashSet<Integer> geoMobileCountriesWithoutMobileAreaCodes = new HashSet();
        geoMobileCountriesWithoutMobileAreaCodes.add(Integer.valueOf(86));
        GEO_MOBILE_COUNTRIES_WITHOUT_MOBILE_AREA_CODES = Collections.unmodifiableSet(geoMobileCountriesWithoutMobileAreaCodes);
        HashSet<Integer> geoMobileCountries = new HashSet();
        geoMobileCountries.add(Integer.valueOf(52));
        geoMobileCountries.add(Integer.valueOf(54));
        geoMobileCountries.add(Integer.valueOf(55));
        geoMobileCountries.add(Integer.valueOf(62));
        geoMobileCountries.addAll(geoMobileCountriesWithoutMobileAreaCodes);
        GEO_MOBILE_COUNTRIES = Collections.unmodifiableSet(geoMobileCountries);
        HashMap<Character, Character> asciiDigitMappings = new HashMap();
        asciiDigitMappings.put(Character.valueOf('0'), Character.valueOf('0'));
        asciiDigitMappings.put(Character.valueOf('1'), Character.valueOf('1'));
        asciiDigitMappings.put(Character.valueOf('2'), Character.valueOf('2'));
        asciiDigitMappings.put(Character.valueOf('3'), Character.valueOf('3'));
        asciiDigitMappings.put(Character.valueOf('4'), Character.valueOf('4'));
        asciiDigitMappings.put(Character.valueOf('5'), Character.valueOf('5'));
        asciiDigitMappings.put(Character.valueOf('6'), Character.valueOf('6'));
        asciiDigitMappings.put(Character.valueOf('7'), Character.valueOf('7'));
        asciiDigitMappings.put(Character.valueOf('8'), Character.valueOf('8'));
        asciiDigitMappings.put(Character.valueOf('9'), Character.valueOf('9'));
        HashMap<Character, Character> alphaMap = new HashMap(40);
        alphaMap.put(Character.valueOf('A'), Character.valueOf('2'));
        alphaMap.put(Character.valueOf('B'), Character.valueOf('2'));
        alphaMap.put(Character.valueOf('C'), Character.valueOf('2'));
        alphaMap.put(Character.valueOf('D'), Character.valueOf('3'));
        alphaMap.put(Character.valueOf('E'), Character.valueOf('3'));
        alphaMap.put(Character.valueOf('F'), Character.valueOf('3'));
        alphaMap.put(Character.valueOf('G'), Character.valueOf('4'));
        alphaMap.put(Character.valueOf('H'), Character.valueOf('4'));
        alphaMap.put(Character.valueOf('I'), Character.valueOf('4'));
        alphaMap.put(Character.valueOf('J'), Character.valueOf('5'));
        alphaMap.put(Character.valueOf('K'), Character.valueOf('5'));
        alphaMap.put(Character.valueOf('L'), Character.valueOf('5'));
        alphaMap.put(Character.valueOf('M'), Character.valueOf('6'));
        alphaMap.put(Character.valueOf('N'), Character.valueOf('6'));
        alphaMap.put(Character.valueOf('O'), Character.valueOf('6'));
        alphaMap.put(Character.valueOf('P'), Character.valueOf('7'));
        alphaMap.put(Character.valueOf('Q'), Character.valueOf('7'));
        alphaMap.put(Character.valueOf('R'), Character.valueOf('7'));
        alphaMap.put(Character.valueOf('S'), Character.valueOf('7'));
        alphaMap.put(Character.valueOf('T'), Character.valueOf('8'));
        alphaMap.put(Character.valueOf('U'), Character.valueOf('8'));
        alphaMap.put(Character.valueOf('V'), Character.valueOf('8'));
        alphaMap.put(Character.valueOf('W'), Character.valueOf('9'));
        alphaMap.put(Character.valueOf('X'), Character.valueOf('9'));
        alphaMap.put(Character.valueOf('Y'), Character.valueOf('9'));
        alphaMap.put(Character.valueOf('Z'), Character.valueOf('9'));
        ALPHA_MAPPINGS = Collections.unmodifiableMap(alphaMap);
        HashMap<Character, Character> combinedMap = new HashMap(100);
        combinedMap.putAll(ALPHA_MAPPINGS);
        combinedMap.putAll(asciiDigitMappings);
        ALPHA_PHONE_MAPPINGS = Collections.unmodifiableMap(combinedMap);
        HashMap<Character, Character> diallableCharMap = new HashMap();
        diallableCharMap.putAll(asciiDigitMappings);
        diallableCharMap.put(Character.valueOf(PLUS_SIGN), Character.valueOf(PLUS_SIGN));
        diallableCharMap.put(Character.valueOf(STAR_SIGN), Character.valueOf(STAR_SIGN));
        diallableCharMap.put(Character.valueOf('#'), Character.valueOf('#'));
        DIALLABLE_CHAR_MAPPINGS = Collections.unmodifiableMap(diallableCharMap);
        HashMap<Character, Character> allPlusNumberGroupings = new HashMap();
        for (Character c : ALPHA_MAPPINGS.keySet()) {
            char c2 = c.charValue();
            allPlusNumberGroupings.put(Character.valueOf(Character.toLowerCase(c2)), Character.valueOf(c2));
            allPlusNumberGroupings.put(Character.valueOf(c2), Character.valueOf(c2));
        }
        allPlusNumberGroupings.putAll(asciiDigitMappings);
        allPlusNumberGroupings.put(Character.valueOf('-'), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf(65293), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf(8208), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf(8209), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf(8210), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf(8211), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf(8212), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf(8213), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf(8722), Character.valueOf('-'));
        allPlusNumberGroupings.put(Character.valueOf('/'), Character.valueOf('/'));
        allPlusNumberGroupings.put(Character.valueOf(65295), Character.valueOf('/'));
        allPlusNumberGroupings.put(Character.valueOf(' '), Character.valueOf(' '));
        allPlusNumberGroupings.put(Character.valueOf(12288), Character.valueOf(' '));
        allPlusNumberGroupings.put(Character.valueOf(8288), Character.valueOf(' '));
        allPlusNumberGroupings.put(Character.valueOf('.'), Character.valueOf('.'));
        allPlusNumberGroupings.put(Character.valueOf(65294), Character.valueOf('.'));
        ALL_PLUS_NUMBER_GROUPING_SYMBOLS = Collections.unmodifiableMap(allPlusNumberGroupings);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).replaceAll("[, \\[\\]]", ""));
        stringBuilder.append(Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).toLowerCase().replaceAll("[, \\[\\]]", ""));
        VALID_ALPHA = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("\\p{Nd}{2}|[+＋]*+(?:[-x‐-―−ー－-／  ­​⁠　()（）［］.\\[\\]/~⁓∼～*]*\\p{Nd}){3,}[-x‐-―−ー－-／  ­​⁠　()（）［］.\\[\\]/~⁓∼～*");
        stringBuilder.append(VALID_ALPHA);
        stringBuilder.append(DIGITS);
        stringBuilder.append("]*");
        VALID_PHONE_NUMBER = stringBuilder.toString();
        String singleExtnSymbolsForMatching = "xｘ#＃~～";
        String singleExtnSymbolsForParsing = new StringBuilder();
        singleExtnSymbolsForParsing.append(",;");
        singleExtnSymbolsForParsing.append(singleExtnSymbolsForMatching);
        EXTN_PATTERNS_FOR_PARSING = createExtnPattern(singleExtnSymbolsForParsing.toString());
        EXTN_PATTERNS_FOR_MATCHING = createExtnPattern(singleExtnSymbolsForMatching);
        stringBuilder = new StringBuilder();
        stringBuilder.append("(?:");
        stringBuilder.append(EXTN_PATTERNS_FOR_PARSING);
        stringBuilder.append(")$");
        EXTN_PATTERN = Pattern.compile(stringBuilder.toString(), REGEX_FLAGS);
        stringBuilder = new StringBuilder();
        stringBuilder.append(VALID_PHONE_NUMBER);
        stringBuilder.append("(?:");
        stringBuilder.append(EXTN_PATTERNS_FOR_PARSING);
        stringBuilder.append(")?");
        VALID_PHONE_NUMBER_PATTERN = Pattern.compile(stringBuilder.toString(), REGEX_FLAGS);
    }

    private static String createExtnPattern(String singleExtnSymbols) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(";ext=(\\p{Nd}{1,7})|[  \\t,]*(?:e?xt(?:ensi(?:ó?|ó))?n?|ｅ?ｘｔｎ?|[");
        stringBuilder.append(singleExtnSymbols);
        stringBuilder.append("]|int|anexo|ｉｎｔ)[:\\.．]?[  \\t,-]*");
        stringBuilder.append(CAPTURING_EXTN_DIGITS);
        stringBuilder.append("#?|[- ]+(");
        stringBuilder.append(DIGITS);
        stringBuilder.append("{1,5})#");
        return stringBuilder.toString();
    }

    PhoneNumberUtil(MetadataSource metadataSource, Map<Integer, List<String>> countryCallingCodeToRegionCodeMap) {
        this.metadataSource = metadataSource;
        this.countryCallingCodeToRegionCodeMap = countryCallingCodeToRegionCodeMap;
        for (Entry<Integer, List<String>> entry : countryCallingCodeToRegionCodeMap.entrySet()) {
            List<String> regionCodes = (List) entry.getValue();
            if (regionCodes.size() == 1 && REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCodes.get(0))) {
                this.countryCodesForNonGeographicalRegion.add(entry.getKey());
            } else {
                this.supportedRegions.addAll(regionCodes);
            }
        }
        if (this.supportedRegions.remove(REGION_CODE_FOR_NON_GEO_ENTITY)) {
            logger.log(Level.WARNING, "invalid metadata (country calling code was mapped to the non-geo entity as well as specific region(s))");
        }
        this.nanpaRegions.addAll((Collection) countryCallingCodeToRegionCodeMap.get(Integer.valueOf(1)));
    }

    static CharSequence extractPossibleNumber(CharSequence number) {
        Matcher m = VALID_START_CHAR_PATTERN.matcher(number);
        if (!m.find()) {
            return "";
        }
        number = number.subSequence(m.start(), number.length());
        Matcher trailingCharsMatcher = UNWANTED_END_CHAR_PATTERN.matcher(number);
        if (trailingCharsMatcher.find()) {
            number = number.subSequence(0, trailingCharsMatcher.start());
        }
        Matcher secondNumber = SECOND_NUMBER_START_PATTERN.matcher(number);
        if (secondNumber.find()) {
            number = number.subSequence(0, secondNumber.start());
        }
        return number;
    }

    static boolean isViablePhoneNumber(CharSequence number) {
        if (number.length() < 2) {
            return false;
        }
        return VALID_PHONE_NUMBER_PATTERN.matcher(number).matches();
    }

    static StringBuilder normalize(StringBuilder number) {
        if (VALID_ALPHA_PHONE_PATTERN.matcher(number).matches()) {
            number.replace(0, number.length(), normalizeHelper(number, ALPHA_PHONE_MAPPINGS, true));
        } else {
            number.replace(0, number.length(), normalizeDigitsOnly(number));
        }
        return number;
    }

    public static String normalizeDigitsOnly(CharSequence number) {
        return normalizeDigits(number, false).toString();
    }

    static StringBuilder normalizeDigits(CharSequence number, boolean keepNonDigits) {
        StringBuilder normalizedDigits = new StringBuilder(number.length());
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                normalizedDigits.append(digit);
            } else if (keepNonDigits) {
                normalizedDigits.append(c);
            }
        }
        return normalizedDigits;
    }

    public static String normalizeDiallableCharsOnly(CharSequence number) {
        return normalizeHelper(number, DIALLABLE_CHAR_MAPPINGS, true);
    }

    public static String convertAlphaCharactersInNumber(CharSequence number) {
        return normalizeHelper(number, ALPHA_PHONE_MAPPINGS, false);
    }

    public int getLengthOfGeographicalAreaCode(PhoneNumber number) {
        PhoneMetadata metadata = getMetadataForRegion(getRegionCodeForNumber(number));
        if (metadata == null) {
            return 0;
        }
        if (!metadata.hasNationalPrefix() && !number.isItalianLeadingZero()) {
            return 0;
        }
        PhoneNumberType type = getNumberType(number);
        int countryCallingCode = number.getCountryCode();
        if ((type != PhoneNumberType.MOBILE || !GEO_MOBILE_COUNTRIES_WITHOUT_MOBILE_AREA_CODES.contains(Integer.valueOf(countryCallingCode))) && isNumberGeographical(type, countryCallingCode)) {
            return getLengthOfNationalDestinationCode(number);
        }
        return 0;
    }

    public int getLengthOfNationalDestinationCode(PhoneNumber number) {
        PhoneNumber copiedProto;
        if (number.hasExtension()) {
            copiedProto = new PhoneNumber();
            copiedProto.mergeFrom(number);
            copiedProto.clearExtension();
        } else {
            copiedProto = number;
        }
        String[] numberGroups = NON_DIGITS_PATTERN.split(format(copiedProto, PhoneNumberFormat.INTERNATIONAL));
        if (numberGroups.length <= 3) {
            return 0;
        }
        if (getNumberType(number) != PhoneNumberType.MOBILE || getCountryMobileToken(number.getCountryCode()).equals("")) {
            return numberGroups[2].length();
        }
        return numberGroups[2].length() + numberGroups[3].length();
    }

    public static String getCountryMobileToken(int countryCallingCode) {
        if (MOBILE_TOKEN_MAPPINGS.containsKey(Integer.valueOf(countryCallingCode))) {
            return (String) MOBILE_TOKEN_MAPPINGS.get(Integer.valueOf(countryCallingCode));
        }
        return "";
    }

    private static String normalizeHelper(CharSequence number, Map<Character, Character> normalizationReplacements, boolean removeNonMatches) {
        StringBuilder normalizedNumber = new StringBuilder(number.length());
        for (int i = 0; i < number.length(); i++) {
            char character = number.charAt(i);
            Character newDigit = (Character) normalizationReplacements.get(Character.valueOf(Character.toUpperCase(character)));
            if (newDigit != null) {
                normalizedNumber.append(newDigit);
            } else if (!removeNonMatches) {
                normalizedNumber.append(character);
            }
        }
        return normalizedNumber.toString();
    }

    static synchronized void setInstance(PhoneNumberUtil util) {
        synchronized (PhoneNumberUtil.class) {
            instance = util;
        }
    }

    public Set<String> getSupportedRegions() {
        return Collections.unmodifiableSet(this.supportedRegions);
    }

    public Set<Integer> getSupportedGlobalNetworkCallingCodes() {
        return Collections.unmodifiableSet(this.countryCodesForNonGeographicalRegion);
    }

    public Set<Integer> getSupportedCallingCodes() {
        return Collections.unmodifiableSet(this.countryCallingCodeToRegionCodeMap.keySet());
    }

    private static boolean descHasPossibleNumberData(PhoneNumberDesc desc) {
        return (desc.getPossibleLengthCount() == 1 && desc.getPossibleLength(0) == -1) ? false : true;
    }

    private static boolean descHasData(PhoneNumberDesc desc) {
        return desc.hasExampleNumber() || descHasPossibleNumberData(desc) || desc.hasNationalNumberPattern();
    }

    private Set<PhoneNumberType> getSupportedTypesForMetadata(PhoneMetadata metadata) {
        Set<PhoneNumberType> types = new TreeSet();
        for (PhoneNumberType type : PhoneNumberType.values()) {
            if (!(type == PhoneNumberType.FIXED_LINE_OR_MOBILE || type == PhoneNumberType.UNKNOWN || !descHasData(getNumberDescByType(metadata, type)))) {
                types.add(type);
            }
        }
        return Collections.unmodifiableSet(types);
    }

    public Set<PhoneNumberType> getSupportedTypesForRegion(String regionCode) {
        if (isValidRegionCode(regionCode)) {
            return getSupportedTypesForMetadata(getMetadataForRegion(regionCode));
        }
        Logger logger = logger;
        Level level = Level.WARNING;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid or unknown region code provided: ");
        stringBuilder.append(regionCode);
        logger.log(level, stringBuilder.toString());
        return Collections.unmodifiableSet(new TreeSet());
    }

    public Set<PhoneNumberType> getSupportedTypesForNonGeoEntity(int countryCallingCode) {
        PhoneMetadata metadata = getMetadataForNonGeographicalRegion(countryCallingCode);
        if (metadata != null) {
            return getSupportedTypesForMetadata(metadata);
        }
        Logger logger = logger;
        Level level = Level.WARNING;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown country calling code for a non-geographical entity provided: ");
        stringBuilder.append(countryCallingCode);
        logger.log(level, stringBuilder.toString());
        return Collections.unmodifiableSet(new TreeSet());
    }

    public static synchronized PhoneNumberUtil getInstance() {
        PhoneNumberUtil phoneNumberUtil;
        synchronized (PhoneNumberUtil.class) {
            if (instance == null) {
                setInstance(createInstance(MetadataManager.DEFAULT_METADATA_LOADER));
            }
            phoneNumberUtil = instance;
        }
        return phoneNumberUtil;
    }

    public static PhoneNumberUtil createInstance(MetadataLoader metadataLoader) {
        if (metadataLoader != null) {
            return createInstance(new MultiFileMetadataSourceImpl(metadataLoader));
        }
        throw new IllegalArgumentException("metadataLoader could not be null.");
    }

    private static PhoneNumberUtil createInstance(MetadataSource metadataSource) {
        if (metadataSource != null) {
            return new PhoneNumberUtil(metadataSource, CountryCodeToRegionCodeMap.getCountryCodeToRegionCodeMap());
        }
        throw new IllegalArgumentException("metadataSource could not be null.");
    }

    static boolean formattingRuleHasFirstGroupOnly(String nationalPrefixFormattingRule) {
        return nationalPrefixFormattingRule.length() == 0 || FIRST_GROUP_ONLY_PREFIX_PATTERN.matcher(nationalPrefixFormattingRule).matches();
    }

    public boolean isNumberGeographical(PhoneNumber phoneNumber) {
        return isNumberGeographical(getNumberType(phoneNumber), phoneNumber.getCountryCode());
    }

    public boolean isNumberGeographical(PhoneNumberType phoneNumberType, int countryCallingCode) {
        return phoneNumberType == PhoneNumberType.FIXED_LINE || phoneNumberType == PhoneNumberType.FIXED_LINE_OR_MOBILE || (GEO_MOBILE_COUNTRIES.contains(Integer.valueOf(countryCallingCode)) && phoneNumberType == PhoneNumberType.MOBILE);
    }

    private boolean isValidRegionCode(String regionCode) {
        return regionCode != null && this.supportedRegions.contains(regionCode);
    }

    private boolean hasValidCountryCallingCode(int countryCallingCode) {
        return this.countryCallingCodeToRegionCodeMap.containsKey(Integer.valueOf(countryCallingCode));
    }

    public String format(PhoneNumber number, PhoneNumberFormat numberFormat) {
        if (number.getNationalNumber() == 0 && number.hasRawInput()) {
            String rawInput = number.getRawInput();
            if (rawInput.length() > 0) {
                return rawInput;
            }
        }
        StringBuilder formattedNumber = new StringBuilder(20);
        format(number, numberFormat, formattedNumber);
        return formattedNumber.toString();
    }

    public void format(PhoneNumber number, PhoneNumberFormat numberFormat, StringBuilder formattedNumber) {
        formattedNumber.setLength(0);
        int countryCallingCode = number.getCountryCode();
        String nationalSignificantNumber = getNationalSignificantNumber(number);
        if (numberFormat == PhoneNumberFormat.E164) {
            formattedNumber.append(nationalSignificantNumber);
            prefixNumberWithCountryCallingCode(countryCallingCode, PhoneNumberFormat.E164, formattedNumber);
        } else if (hasValidCountryCallingCode(countryCallingCode)) {
            PhoneMetadata metadata = getMetadataForRegionOrCallingCode(countryCallingCode, getRegionCodeForCountryCode(countryCallingCode));
            formattedNumber.append(formatNsn(nationalSignificantNumber, metadata, numberFormat));
            maybeAppendFormattedExtension(number, metadata, numberFormat, formattedNumber);
            prefixNumberWithCountryCallingCode(countryCallingCode, numberFormat, formattedNumber);
        } else {
            formattedNumber.append(nationalSignificantNumber);
        }
    }

    public String formatByPattern(PhoneNumber number, PhoneNumberFormat numberFormat, List<NumberFormat> userDefinedFormats) {
        int countryCallingCode = number.getCountryCode();
        String nationalSignificantNumber = getNationalSignificantNumber(number);
        if (!hasValidCountryCallingCode(countryCallingCode)) {
            return nationalSignificantNumber;
        }
        PhoneMetadata metadata = getMetadataForRegionOrCallingCode(countryCallingCode, getRegionCodeForCountryCode(countryCallingCode));
        StringBuilder formattedNumber = new StringBuilder(20);
        NumberFormat formattingPattern = chooseFormattingPatternForNumber(userDefinedFormats, nationalSignificantNumber);
        if (formattingPattern == null) {
            formattedNumber.append(nationalSignificantNumber);
        } else {
            Builder numFormatCopy = NumberFormat.newBuilder();
            numFormatCopy.mergeFrom(formattingPattern);
            String nationalPrefixFormattingRule = formattingPattern.getNationalPrefixFormattingRule();
            if (nationalPrefixFormattingRule.length() > 0) {
                String nationalPrefix = metadata.getNationalPrefix();
                if (nationalPrefix.length() > 0) {
                    numFormatCopy.setNationalPrefixFormattingRule(nationalPrefixFormattingRule.replace(NP_STRING, nationalPrefix).replace(FG_STRING, "$1"));
                } else {
                    numFormatCopy.clearNationalPrefixFormattingRule();
                }
            }
            formattedNumber.append(formatNsnUsingPattern(nationalSignificantNumber, numFormatCopy, numberFormat));
        }
        maybeAppendFormattedExtension(number, metadata, numberFormat, formattedNumber);
        prefixNumberWithCountryCallingCode(countryCallingCode, numberFormat, formattedNumber);
        return formattedNumber.toString();
    }

    public String formatNationalNumberWithCarrierCode(PhoneNumber number, CharSequence carrierCode) {
        int countryCallingCode = number.getCountryCode();
        String nationalSignificantNumber = getNationalSignificantNumber(number);
        if (!hasValidCountryCallingCode(countryCallingCode)) {
            return nationalSignificantNumber;
        }
        PhoneMetadata metadata = getMetadataForRegionOrCallingCode(countryCallingCode, getRegionCodeForCountryCode(countryCallingCode));
        StringBuilder formattedNumber = new StringBuilder(20);
        formattedNumber.append(formatNsn(nationalSignificantNumber, metadata, PhoneNumberFormat.NATIONAL, carrierCode));
        maybeAppendFormattedExtension(number, metadata, PhoneNumberFormat.NATIONAL, formattedNumber);
        prefixNumberWithCountryCallingCode(countryCallingCode, PhoneNumberFormat.NATIONAL, formattedNumber);
        return formattedNumber.toString();
    }

    private PhoneMetadata getMetadataForRegionOrCallingCode(int countryCallingCode, String regionCode) {
        if (REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCode)) {
            return getMetadataForNonGeographicalRegion(countryCallingCode);
        }
        return getMetadataForRegion(regionCode);
    }

    public String formatNationalNumberWithPreferredCarrierCode(PhoneNumber number, CharSequence fallbackCarrierCode) {
        CharSequence preferredDomesticCarrierCode;
        if (number.getPreferredDomesticCarrierCode().length() > 0) {
            preferredDomesticCarrierCode = number.getPreferredDomesticCarrierCode();
        } else {
            preferredDomesticCarrierCode = fallbackCarrierCode;
        }
        return formatNationalNumberWithCarrierCode(number, preferredDomesticCarrierCode);
    }

    public String formatNumberForMobileDialing(PhoneNumber number, String regionCallingFrom, boolean withFormatting) {
        int countryCallingCode = number.getCountryCode();
        if (hasValidCountryCallingCode(countryCallingCode)) {
            String format;
            String formattedNumber = "";
            PhoneNumber numberNoExt = new PhoneNumber().mergeFrom(number).clearExtension();
            String regionCode = getRegionCodeForCountryCode(countryCallingCode);
            PhoneNumberType numberType = getNumberType(numberNoExt);
            boolean isFixedLineOrMobile = false;
            boolean isValidNumber = numberType != PhoneNumberType.UNKNOWN;
            if (regionCallingFrom.equals(regionCode)) {
                if (numberType == PhoneNumberType.FIXED_LINE || numberType == PhoneNumberType.MOBILE || numberType == PhoneNumberType.FIXED_LINE_OR_MOBILE) {
                    isFixedLineOrMobile = true;
                }
                if (regionCode.equals("CO") && numberType == PhoneNumberType.FIXED_LINE) {
                    formattedNumber = formatNationalNumberWithCarrierCode(numberNoExt, COLOMBIA_MOBILE_TO_FIXED_LINE_PREFIX);
                } else if (regionCode.equals("BR") && isFixedLineOrMobile) {
                    String formatNationalNumberWithPreferredCarrierCode;
                    if (numberNoExt.getPreferredDomesticCarrierCode().length() > 0) {
                        formatNationalNumberWithPreferredCarrierCode = formatNationalNumberWithPreferredCarrierCode(numberNoExt, "");
                        formattedNumber = formatNationalNumberWithPreferredCarrierCode;
                    } else {
                        formatNationalNumberWithPreferredCarrierCode = "";
                    }
                    formattedNumber = formatNationalNumberWithPreferredCarrierCode;
                } else if (isValidNumber && regionCode.equals("HU")) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(getNddPrefixForRegion(regionCode, true));
                    stringBuilder.append(Separators.SP);
                    stringBuilder.append(format(numberNoExt, PhoneNumberFormat.NATIONAL));
                    formattedNumber = stringBuilder.toString();
                } else if (countryCallingCode == 1) {
                    formattedNumber = (!canBeInternationallyDialled(numberNoExt) || testNumberLength(getNationalSignificantNumber(numberNoExt), getMetadataForRegion(regionCallingFrom)) == ValidationResult.TOO_SHORT) ? format(numberNoExt, PhoneNumberFormat.NATIONAL) : format(numberNoExt, PhoneNumberFormat.INTERNATIONAL);
                } else {
                    formattedNumber = ((regionCode.equals(REGION_CODE_FOR_NON_GEO_ENTITY) || ((regionCode.equals("MX") || regionCode.equals("CL")) && isFixedLineOrMobile)) && canBeInternationallyDialled(numberNoExt)) ? format(numberNoExt, PhoneNumberFormat.INTERNATIONAL) : format(numberNoExt, PhoneNumberFormat.NATIONAL);
                }
            } else if (isValidNumber && canBeInternationallyDialled(numberNoExt)) {
                if (withFormatting) {
                    format = format(numberNoExt, PhoneNumberFormat.INTERNATIONAL);
                } else {
                    format = format(numberNoExt, PhoneNumberFormat.E164);
                }
                return format;
            }
            if (withFormatting) {
                format = formattedNumber;
            } else {
                format = normalizeDiallableCharsOnly(formattedNumber);
            }
            return format;
        }
        return number.hasRawInput() ? number.getRawInput() : "";
    }

    public String formatOutOfCountryCallingNumber(PhoneNumber number, String regionCallingFrom) {
        StringBuilder stringBuilder;
        if (isValidRegionCode(regionCallingFrom)) {
            int countryCallingCode = number.getCountryCode();
            String nationalSignificantNumber = getNationalSignificantNumber(number);
            if (!hasValidCountryCallingCode(countryCallingCode)) {
                return nationalSignificantNumber;
            }
            if (countryCallingCode == 1) {
                if (isNANPACountry(regionCallingFrom)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(countryCallingCode);
                    stringBuilder.append(Separators.SP);
                    stringBuilder.append(format(number, PhoneNumberFormat.NATIONAL));
                    return stringBuilder.toString();
                }
            } else if (countryCallingCode == getCountryCodeForValidRegion(regionCallingFrom)) {
                return format(number, PhoneNumberFormat.NATIONAL);
            }
            PhoneMetadata metadataForRegionCallingFrom = getMetadataForRegion(regionCallingFrom);
            String internationalPrefix = metadataForRegionCallingFrom.getInternationalPrefix();
            String internationalPrefixForFormatting = "";
            if (SINGLE_INTERNATIONAL_PREFIX.matcher(internationalPrefix).matches()) {
                internationalPrefixForFormatting = internationalPrefix;
            } else if (metadataForRegionCallingFrom.hasPreferredInternationalPrefix()) {
                internationalPrefixForFormatting = metadataForRegionCallingFrom.getPreferredInternationalPrefix();
            }
            PhoneMetadata metadataForRegion = getMetadataForRegionOrCallingCode(countryCallingCode, getRegionCodeForCountryCode(countryCallingCode));
            StringBuilder formattedNumber = new StringBuilder(formatNsn(nationalSignificantNumber, metadataForRegion, PhoneNumberFormat.INTERNATIONAL));
            maybeAppendFormattedExtension(number, metadataForRegion, PhoneNumberFormat.INTERNATIONAL, formattedNumber);
            if (internationalPrefixForFormatting.length() > 0) {
                formattedNumber.insert(0, Separators.SP).insert(0, countryCallingCode).insert(0, Separators.SP).insert(0, internationalPrefixForFormatting);
            } else {
                prefixNumberWithCountryCallingCode(countryCallingCode, PhoneNumberFormat.INTERNATIONAL, formattedNumber);
            }
            return formattedNumber.toString();
        }
        Logger logger = logger;
        Level level = Level.WARNING;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Trying to format number from invalid region ");
        stringBuilder.append(regionCallingFrom);
        stringBuilder.append(". International formatting applied.");
        logger.log(level, stringBuilder.toString());
        return format(number, PhoneNumberFormat.INTERNATIONAL);
    }

    public String formatInOriginalFormat(PhoneNumber number, String regionCallingFrom) {
        if (number.hasRawInput() && !hasFormattingPatternForNumber(number)) {
            return number.getRawInput();
        }
        if (!number.hasCountryCodeSource()) {
            return format(number, PhoneNumberFormat.NATIONAL);
        }
        String regionCode;
        String formattedNumber;
        switch (number.getCountryCodeSource()) {
            case FROM_NUMBER_WITH_PLUS_SIGN:
                regionCode = format(number, PhoneNumberFormat.INTERNATIONAL);
                break;
            case FROM_NUMBER_WITH_IDD:
                regionCode = formatOutOfCountryCallingNumber(number, regionCallingFrom);
                break;
            case FROM_NUMBER_WITHOUT_PLUS_SIGN:
                regionCode = format(number, PhoneNumberFormat.INTERNATIONAL).substring(1);
                break;
            default:
                regionCode = getRegionCodeForCountryCode(number.getCountryCode());
                String nationalPrefix = getNddPrefixForRegion(regionCode, true);
                String nationalFormat = format(number, PhoneNumberFormat.NATIONAL);
                if (nationalPrefix != null && nationalPrefix.length() != 0) {
                    if (!rawInputContainsNationalPrefix(number.getRawInput(), nationalPrefix, regionCode)) {
                        PhoneMetadata metadata = getMetadataForRegion(regionCode);
                        NumberFormat formatRule = chooseFormattingPatternForNumber(metadata.numberFormats(), getNationalSignificantNumber(number));
                        if (formatRule != null) {
                            String candidateNationalPrefixRule = formatRule.getNationalPrefixFormattingRule();
                            int indexOfFirstGroup = candidateNationalPrefixRule.indexOf("$1");
                            if (indexOfFirstGroup > 0) {
                                if (normalizeDigitsOnly(candidateNationalPrefixRule.substring(0, indexOfFirstGroup)).length() != 0) {
                                    Builder numFormatCopy = NumberFormat.newBuilder();
                                    numFormatCopy.mergeFrom(formatRule);
                                    numFormatCopy.clearNationalPrefixFormattingRule();
                                    ArrayList numberFormats = new ArrayList(1);
                                    numberFormats.add(numFormatCopy);
                                    regionCode = formatByPattern(number, PhoneNumberFormat.NATIONAL, numberFormats);
                                    break;
                                }
                                formattedNumber = nationalFormat;
                                break;
                            }
                            formattedNumber = nationalFormat;
                            break;
                        }
                        formattedNumber = nationalFormat;
                        break;
                    }
                    formattedNumber = nationalFormat;
                    break;
                }
                formattedNumber = nationalFormat;
                break;
                break;
        }
        formattedNumber = regionCode;
        regionCode = formattedNumber;
        formattedNumber = number.getRawInput();
        if (!(regionCode == null || formattedNumber.length() <= 0 || normalizeDiallableCharsOnly(regionCode).equals(normalizeDiallableCharsOnly(formattedNumber)))) {
            regionCode = formattedNumber;
        }
        return regionCode;
    }

    private boolean rawInputContainsNationalPrefix(String rawInput, String nationalPrefix, String regionCode) {
        String normalizedNationalNumber = normalizeDigitsOnly(rawInput);
        if (!normalizedNationalNumber.startsWith(nationalPrefix)) {
            return false;
        }
        try {
            return isValidNumber(parse(normalizedNationalNumber.substring(nationalPrefix.length()), regionCode));
        } catch (NumberParseException e) {
            return false;
        }
    }

    private boolean hasFormattingPatternForNumber(PhoneNumber number) {
        int countryCallingCode = number.getCountryCode();
        PhoneMetadata metadata = getMetadataForRegionOrCallingCode(countryCallingCode, getRegionCodeForCountryCode(countryCallingCode));
        boolean z = false;
        if (metadata == null) {
            return false;
        }
        if (chooseFormattingPatternForNumber(metadata.numberFormats(), getNationalSignificantNumber(number)) != null) {
            z = true;
        }
        return z;
    }

    public String formatOutOfCountryKeepingAlphaChars(PhoneNumber number, String regionCallingFrom) {
        String rawInput = number.getRawInput();
        if (rawInput.length() == 0) {
            return formatOutOfCountryCallingNumber(number, regionCallingFrom);
        }
        int countryCode = number.getCountryCode();
        if (!hasValidCountryCallingCode(countryCode)) {
            return rawInput;
        }
        rawInput = normalizeHelper(rawInput, ALL_PLUS_NUMBER_GROUPING_SYMBOLS, true);
        String nationalNumber = getNationalSignificantNumber(number);
        if (nationalNumber.length() > 3) {
            int firstNationalNumberDigit = rawInput.indexOf(nationalNumber.substring(0, 3));
            if (firstNationalNumberDigit != -1) {
                rawInput = rawInput.substring(firstNationalNumberDigit);
            }
        }
        PhoneMetadata metadataForRegionCallingFrom = getMetadataForRegion(regionCallingFrom);
        if (countryCode == 1) {
            if (isNANPACountry(regionCallingFrom)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(countryCode);
                stringBuilder.append(Separators.SP);
                stringBuilder.append(rawInput);
                return stringBuilder.toString();
            }
        } else if (metadataForRegionCallingFrom != null && countryCode == getCountryCodeForValidRegion(regionCallingFrom)) {
            NumberFormat formattingPattern = chooseFormattingPatternForNumber(metadataForRegionCallingFrom.numberFormats(), nationalNumber);
            if (formattingPattern == null) {
                return rawInput;
            }
            Builder newFormat = NumberFormat.newBuilder();
            newFormat.mergeFrom(formattingPattern);
            newFormat.setPattern("(\\d+)(.*)");
            newFormat.setFormat("$1$2");
            return formatNsnUsingPattern(rawInput, newFormat, PhoneNumberFormat.NATIONAL);
        }
        String internationalPrefixForFormatting = "";
        if (metadataForRegionCallingFrom != null) {
            String internationalPrefix = metadataForRegionCallingFrom.getInternationalPrefix();
            internationalPrefixForFormatting = SINGLE_INTERNATIONAL_PREFIX.matcher(internationalPrefix).matches() ? internationalPrefix : metadataForRegionCallingFrom.getPreferredInternationalPrefix();
        }
        StringBuilder formattedNumber = new StringBuilder(rawInput);
        maybeAppendFormattedExtension(number, getMetadataForRegionOrCallingCode(countryCode, getRegionCodeForCountryCode(countryCode)), PhoneNumberFormat.INTERNATIONAL, formattedNumber);
        if (internationalPrefixForFormatting.length() > 0) {
            formattedNumber.insert(0, Separators.SP).insert(0, countryCode).insert(0, Separators.SP).insert(0, internationalPrefixForFormatting);
        } else {
            if (!isValidRegionCode(regionCallingFrom)) {
                Logger logger = logger;
                Level level = Level.WARNING;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Trying to format number from invalid region ");
                stringBuilder2.append(regionCallingFrom);
                stringBuilder2.append(". International formatting applied.");
                logger.log(level, stringBuilder2.toString());
            }
            prefixNumberWithCountryCallingCode(countryCode, PhoneNumberFormat.INTERNATIONAL, formattedNumber);
        }
        return formattedNumber.toString();
    }

    public String getNationalSignificantNumber(PhoneNumber number) {
        StringBuilder nationalNumber = new StringBuilder();
        if (number.isItalianLeadingZero() && number.getNumberOfLeadingZeros() > 0) {
            char[] zeros = new char[number.getNumberOfLeadingZeros()];
            Arrays.fill(zeros, '0');
            nationalNumber.append(new String(zeros));
        }
        nationalNumber.append(number.getNationalNumber());
        return nationalNumber.toString();
    }

    private void prefixNumberWithCountryCallingCode(int countryCallingCode, PhoneNumberFormat numberFormat, StringBuilder formattedNumber) {
        switch (numberFormat) {
            case E164:
                formattedNumber.insert(0, countryCallingCode).insert(0, PLUS_SIGN);
                return;
            case INTERNATIONAL:
                formattedNumber.insert(0, Separators.SP).insert(0, countryCallingCode).insert(0, PLUS_SIGN);
                return;
            case RFC3966:
                formattedNumber.insert(0, "-").insert(0, countryCallingCode).insert(0, PLUS_SIGN).insert(0, RFC3966_PREFIX);
                return;
            default:
                return;
        }
    }

    private String formatNsn(String number, PhoneMetadata metadata, PhoneNumberFormat numberFormat) {
        return formatNsn(number, metadata, numberFormat, null);
    }

    private String formatNsn(String number, PhoneMetadata metadata, PhoneNumberFormat numberFormat, CharSequence carrierCode) {
        List<NumberFormat> availableFormats;
        if (metadata.intlNumberFormats().size() == 0 || numberFormat == PhoneNumberFormat.NATIONAL) {
            availableFormats = metadata.numberFormats();
        } else {
            availableFormats = metadata.intlNumberFormats();
        }
        NumberFormat formattingPattern = chooseFormattingPatternForNumber(availableFormats, number);
        if (formattingPattern == null) {
            return number;
        }
        return formatNsnUsingPattern(number, formattingPattern, numberFormat, carrierCode);
    }

    NumberFormat chooseFormattingPatternForNumber(List<NumberFormat> availableFormats, String nationalNumber) {
        for (NumberFormat numFormat : availableFormats) {
            int size = numFormat.leadingDigitsPatternSize();
            if ((size == 0 || this.regexCache.getPatternForRegex(numFormat.getLeadingDigitsPattern(size - 1)).matcher(nationalNumber).lookingAt()) && this.regexCache.getPatternForRegex(numFormat.getPattern()).matcher(nationalNumber).matches()) {
                return numFormat;
            }
        }
        return null;
    }

    String formatNsnUsingPattern(String nationalNumber, NumberFormat formattingPattern, PhoneNumberFormat numberFormat) {
        return formatNsnUsingPattern(nationalNumber, formattingPattern, numberFormat, null);
    }

    private String formatNsnUsingPattern(String nationalNumber, NumberFormat formattingPattern, PhoneNumberFormat numberFormat, CharSequence carrierCode) {
        String numberFormatRule = formattingPattern.getFormat();
        Matcher m = this.regexCache.getPatternForRegex(formattingPattern.getPattern()).matcher(nationalNumber);
        String formattedNationalNumber = "";
        if (numberFormat != PhoneNumberFormat.NATIONAL || carrierCode == null || carrierCode.length() <= 0 || formattingPattern.getDomesticCarrierCodeFormattingRule().length() <= 0) {
            String nationalPrefixFormattingRule = formattingPattern.getNationalPrefixFormattingRule();
            if (numberFormat != PhoneNumberFormat.NATIONAL || nationalPrefixFormattingRule == null || nationalPrefixFormattingRule.length() <= 0) {
                formattedNationalNumber = m.replaceAll(numberFormatRule);
            } else {
                formattedNationalNumber = m.replaceAll(FIRST_GROUP_PATTERN.matcher(numberFormatRule).replaceFirst(nationalPrefixFormattingRule));
            }
        } else {
            formattedNationalNumber = m.replaceAll(FIRST_GROUP_PATTERN.matcher(numberFormatRule).replaceFirst(formattingPattern.getDomesticCarrierCodeFormattingRule().replace(CC_STRING, carrierCode)));
        }
        if (numberFormat != PhoneNumberFormat.RFC3966) {
            return formattedNationalNumber;
        }
        Matcher matcher = SEPARATOR_PATTERN.matcher(formattedNationalNumber);
        if (matcher.lookingAt()) {
            formattedNationalNumber = matcher.replaceFirst("");
        }
        return matcher.reset(formattedNationalNumber).replaceAll("-");
    }

    public PhoneNumber getExampleNumber(String regionCode) {
        return getExampleNumberForType(regionCode, PhoneNumberType.FIXED_LINE);
    }

    public PhoneNumber getInvalidExampleNumber(String regionCode) {
        if (isValidRegionCode(regionCode)) {
            PhoneNumberDesc desc = getNumberDescByType(getMetadataForRegion(regionCode), PhoneNumberType.FIXED_LINE);
            if (!desc.hasExampleNumber()) {
                return null;
            }
            String exampleNumber = desc.getExampleNumber();
            int phoneNumberLength = exampleNumber.length();
            while (true) {
                phoneNumberLength--;
                if (phoneNumberLength < 2) {
                    return null;
                }
                try {
                    PhoneNumber possiblyValidNumber = parse(exampleNumber.substring(null, phoneNumberLength), regionCode);
                    if (!isValidNumber(possiblyValidNumber)) {
                        return possiblyValidNumber;
                    }
                } catch (NumberParseException e) {
                }
            }
        } else {
            Logger logger = logger;
            Level level = Level.WARNING;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid or unknown region code provided: ");
            stringBuilder.append(regionCode);
            logger.log(level, stringBuilder.toString());
            return null;
        }
    }

    public PhoneNumber getExampleNumberForType(String regionCode, PhoneNumberType type) {
        if (isValidRegionCode(regionCode)) {
            PhoneNumberDesc desc = getNumberDescByType(getMetadataForRegion(regionCode), type);
            try {
                if (desc.hasExampleNumber()) {
                    return parse(desc.getExampleNumber(), regionCode);
                }
            } catch (NumberParseException e) {
                logger.log(Level.SEVERE, e.toString());
            }
            return null;
        }
        Logger logger = logger;
        Level level = Level.WARNING;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid or unknown region code provided: ");
        stringBuilder.append(regionCode);
        logger.log(level, stringBuilder.toString());
        return null;
    }

    public PhoneNumber getExampleNumberForType(PhoneNumberType type) {
        for (String regionCode : getSupportedRegions()) {
            PhoneNumber exampleNumber = getExampleNumberForType(regionCode, type);
            if (exampleNumber != null) {
                return exampleNumber;
            }
        }
        for (Integer countryCallingCode : getSupportedGlobalNetworkCallingCodes()) {
            int countryCallingCode2 = countryCallingCode.intValue();
            PhoneNumberDesc desc = getNumberDescByType(getMetadataForNonGeographicalRegion(countryCallingCode2), type);
            try {
                if (desc.hasExampleNumber()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("+");
                    stringBuilder.append(countryCallingCode2);
                    stringBuilder.append(desc.getExampleNumber());
                    return parse(stringBuilder.toString(), UNKNOWN_REGION);
                }
            } catch (NumberParseException e) {
                logger.log(Level.SEVERE, e.toString());
            }
        }
        return null;
    }

    public PhoneNumber getExampleNumberForNonGeoEntity(int countryCallingCode) {
        StringBuilder stringBuilder;
        if (getMetadataForNonGeographicalRegion(countryCallingCode) != null) {
            for (PhoneNumberDesc desc : Arrays.asList(new PhoneNumberDesc[]{getMetadataForNonGeographicalRegion(countryCallingCode).getMobile(), getMetadataForNonGeographicalRegion(countryCallingCode).getTollFree(), getMetadataForNonGeographicalRegion(countryCallingCode).getSharedCost(), getMetadataForNonGeographicalRegion(countryCallingCode).getVoip(), getMetadataForNonGeographicalRegion(countryCallingCode).getVoicemail(), getMetadataForNonGeographicalRegion(countryCallingCode).getUan(), getMetadataForNonGeographicalRegion(countryCallingCode).getPremiumRate()})) {
                if (desc != null) {
                    try {
                        if (desc.hasExampleNumber()) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("+");
                            stringBuilder.append(countryCallingCode);
                            stringBuilder.append(desc.getExampleNumber());
                            return parse(stringBuilder.toString(), UNKNOWN_REGION);
                        }
                    } catch (NumberParseException e) {
                        logger.log(Level.SEVERE, e.toString());
                    }
                }
            }
        } else {
            Logger logger = logger;
            Level level = Level.WARNING;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid or unknown country calling code provided: ");
            stringBuilder.append(countryCallingCode);
            logger.log(level, stringBuilder.toString());
        }
        return null;
    }

    private void maybeAppendFormattedExtension(PhoneNumber number, PhoneMetadata metadata, PhoneNumberFormat numberFormat, StringBuilder formattedNumber) {
        if (number.hasExtension() && number.getExtension().length() > 0) {
            if (numberFormat == PhoneNumberFormat.RFC3966) {
                formattedNumber.append(RFC3966_EXTN_PREFIX);
                formattedNumber.append(number.getExtension());
            } else if (metadata.hasPreferredExtnPrefix()) {
                formattedNumber.append(metadata.getPreferredExtnPrefix());
                formattedNumber.append(number.getExtension());
            } else {
                formattedNumber.append(DEFAULT_EXTN_PREFIX);
                formattedNumber.append(number.getExtension());
            }
        }
    }

    PhoneNumberDesc getNumberDescByType(PhoneMetadata metadata, PhoneNumberType type) {
        switch (AnonymousClass2.$SwitchMap$com$google$i18n$phonenumbers$PhoneNumberUtil$PhoneNumberType[type.ordinal()]) {
            case 1:
                return metadata.getPremiumRate();
            case 2:
                return metadata.getTollFree();
            case 3:
                return metadata.getMobile();
            case 4:
            case 5:
                return metadata.getFixedLine();
            case 6:
                return metadata.getSharedCost();
            case 7:
                return metadata.getVoip();
            case 8:
                return metadata.getPersonalNumber();
            case 9:
                return metadata.getPager();
            case WarningHeader.ATTRIBUTE_NOT_UNDERSTOOD /*10*/:
                return metadata.getUan();
            case 11:
                return metadata.getVoicemail();
            default:
                return metadata.getGeneralDesc();
        }
    }

    public PhoneNumberType getNumberType(PhoneNumber number) {
        String regionCode = getRegionCodeForNumber(number);
        PhoneMetadata metadata = getMetadataForRegionOrCallingCode(number.getCountryCode(), regionCode);
        if (metadata == null) {
            return PhoneNumberType.UNKNOWN;
        }
        PhoneNumberType pNT = getNumberTypeHelper(getNationalSignificantNumber(number), metadata);
        if (pNT != PhoneNumberType.UNKNOWN || regionCode == null || !regionCode.equals("ME")) {
            return pNT;
        }
        logger.log(Level.WARNING, "pNT == PhoneNumberType.UNKNOWN and regionCode.equals(ME)");
        return PhoneNumberType.FIXED_LINE;
    }

    private PhoneNumberType getNumberTypeHelper(String nationalNumber, PhoneMetadata metadata) {
        if (!isNumberMatchingDesc(nationalNumber, metadata.getGeneralDesc())) {
            return PhoneNumberType.UNKNOWN;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getPremiumRate())) {
            return PhoneNumberType.PREMIUM_RATE;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getTollFree())) {
            return PhoneNumberType.TOLL_FREE;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getSharedCost())) {
            return PhoneNumberType.SHARED_COST;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getVoip())) {
            return PhoneNumberType.VOIP;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getPersonalNumber())) {
            return PhoneNumberType.PERSONAL_NUMBER;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getPager())) {
            return PhoneNumberType.PAGER;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getUan())) {
            return PhoneNumberType.UAN;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getVoicemail())) {
            return PhoneNumberType.VOICEMAIL;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getFixedLine())) {
            if (metadata.getSameMobileAndFixedLinePattern()) {
                return PhoneNumberType.FIXED_LINE_OR_MOBILE;
            }
            if (isNumberMatchingDesc(nationalNumber, metadata.getMobile())) {
                return PhoneNumberType.FIXED_LINE_OR_MOBILE;
            }
            return PhoneNumberType.FIXED_LINE;
        } else if (metadata.getSameMobileAndFixedLinePattern() || !isNumberMatchingDesc(nationalNumber, metadata.getMobile())) {
            return PhoneNumberType.UNKNOWN;
        } else {
            return PhoneNumberType.MOBILE;
        }
    }

    PhoneMetadata getMetadataForRegion(String regionCode) {
        if (isValidRegionCode(regionCode)) {
            return this.metadataSource.getMetadataForRegion(regionCode);
        }
        return null;
    }

    PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode) {
        if (this.countryCallingCodeToRegionCodeMap.containsKey(Integer.valueOf(countryCallingCode))) {
            return this.metadataSource.getMetadataForNonGeographicalRegion(countryCallingCode);
        }
        return null;
    }

    boolean isNumberMatchingDesc(String nationalNumber, PhoneNumberDesc numberDesc) {
        int actualLength = nationalNumber.length();
        List<Integer> possibleLengths = numberDesc.getPossibleLengthList();
        if (possibleLengths.size() <= 0 || possibleLengths.contains(Integer.valueOf(actualLength))) {
            return this.matcherApi.matchNationalNumber(nationalNumber, numberDesc, false);
        }
        return false;
    }

    public boolean isValidNumber(PhoneNumber number) {
        return isValidNumberForRegion(number, getRegionCodeForNumber(number));
    }

    public boolean isValidNumberForRegion(PhoneNumber number, String regionCode) {
        int countryCode = number.getCountryCode();
        PhoneMetadata metadata = getMetadataForRegionOrCallingCode(countryCode, regionCode);
        boolean z = false;
        if (metadata == null || (!REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCode) && countryCode != getCountryCodeForValidRegion(regionCode))) {
            return false;
        }
        if (getNumberTypeHelper(getNationalSignificantNumber(number), metadata) != PhoneNumberType.UNKNOWN) {
            z = true;
        }
        return z;
    }

    public String getRegionCodeForNumber(PhoneNumber number) {
        int countryCode = number.getCountryCode();
        List<String> regions = (List) this.countryCallingCodeToRegionCodeMap.get(Integer.valueOf(countryCode));
        if (regions == null) {
            Logger logger = logger;
            Level level = Level.INFO;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Missing/invalid country_code (");
            stringBuilder.append(countryCode);
            stringBuilder.append(Separators.RPAREN);
            logger.log(level, stringBuilder.toString());
            return null;
        } else if (regions.size() == 1) {
            return (String) regions.get(0);
        } else {
            return getRegionCodeForNumberFromRegionList(number, regions);
        }
    }

    private String getRegionCodeForNumberFromRegionList(PhoneNumber number, List<String> regionCodes) {
        String nationalNumber = getNationalSignificantNumber(number);
        for (String regionCode : regionCodes) {
            PhoneMetadata metadata = getMetadataForRegion(regionCode);
            if (metadata.hasLeadingDigits()) {
                if (this.regexCache.getPatternForRegex(metadata.getLeadingDigits()).matcher(nationalNumber).lookingAt()) {
                    return regionCode;
                }
            } else if (getNumberTypeHelper(nationalNumber, metadata) != PhoneNumberType.UNKNOWN) {
                return regionCode;
            }
        }
        return null;
    }

    public String getRegionCodeForCountryCode(int countryCallingCode) {
        List<String> regionCodes = (List) this.countryCallingCodeToRegionCodeMap.get(Integer.valueOf(countryCallingCode));
        return regionCodes == null ? UNKNOWN_REGION : (String) regionCodes.get(0);
    }

    public List<String> getRegionCodesForCountryCode(int countryCallingCode) {
        List<String> regionCodes = (List) this.countryCallingCodeToRegionCodeMap.get(Integer.valueOf(countryCallingCode));
        return Collections.unmodifiableList(regionCodes == null ? new ArrayList(0) : regionCodes);
    }

    public int getCountryCodeForRegion(String regionCode) {
        if (isValidRegionCode(regionCode)) {
            return getCountryCodeForValidRegion(regionCode);
        }
        Logger logger = logger;
        Level level = Level.WARNING;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid or missing region code (");
        stringBuilder.append(regionCode == null ? "null" : regionCode);
        stringBuilder.append(") provided.");
        logger.log(level, stringBuilder.toString());
        return 0;
    }

    private int getCountryCodeForValidRegion(String regionCode) {
        PhoneMetadata metadata = getMetadataForRegion(regionCode);
        if (metadata != null) {
            return metadata.getCountryCode();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid region code: ");
        stringBuilder.append(regionCode);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public String getNddPrefixForRegion(String regionCode, boolean stripNonDigits) {
        PhoneMetadata metadata = getMetadataForRegion(regionCode);
        if (metadata == null) {
            Logger logger = logger;
            Level level = Level.WARNING;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid or missing region code (");
            stringBuilder.append(regionCode == null ? "null" : regionCode);
            stringBuilder.append(") provided.");
            logger.log(level, stringBuilder.toString());
            return null;
        }
        String nationalPrefix = metadata.getNationalPrefix();
        if (nationalPrefix.length() == 0) {
            return null;
        }
        if (stripNonDigits) {
            nationalPrefix = nationalPrefix.replace("~", "");
        }
        return nationalPrefix;
    }

    public boolean isNANPACountry(String regionCode) {
        return this.nanpaRegions.contains(regionCode);
    }

    public boolean isAlphaNumber(CharSequence number) {
        if (!isViablePhoneNumber(number)) {
            return false;
        }
        StringBuilder strippedNumber = new StringBuilder(number);
        maybeStripExtension(strippedNumber);
        return VALID_ALPHA_PHONE_PATTERN.matcher(strippedNumber).matches();
    }

    public boolean isPossibleNumber(PhoneNumber number) {
        ValidationResult result = isPossibleNumberWithReason(number);
        return result == ValidationResult.IS_POSSIBLE || result == ValidationResult.IS_POSSIBLE_LOCAL_ONLY;
    }

    public boolean isPossibleNumberForType(PhoneNumber number, PhoneNumberType type) {
        ValidationResult result = isPossibleNumberForTypeWithReason(number, type);
        return result == ValidationResult.IS_POSSIBLE || result == ValidationResult.IS_POSSIBLE_LOCAL_ONLY;
    }

    private ValidationResult testNumberLength(CharSequence number, PhoneMetadata metadata) {
        return testNumberLength(number, metadata, PhoneNumberType.UNKNOWN);
    }

    private ValidationResult testNumberLength(CharSequence number, PhoneMetadata metadata, PhoneNumberType type) {
        PhoneNumberDesc descForType = getNumberDescByType(metadata, type);
        List<Integer> possibleLengths = descForType.getPossibleLengthList().isEmpty() ? metadata.getGeneralDesc().getPossibleLengthList() : descForType.getPossibleLengthList();
        List<Integer> localLengths = descForType.getPossibleLengthLocalOnlyList();
        if (type == PhoneNumberType.FIXED_LINE_OR_MOBILE) {
            if (!descHasPossibleNumberData(getNumberDescByType(metadata, PhoneNumberType.FIXED_LINE))) {
                return testNumberLength(number, metadata, PhoneNumberType.MOBILE);
            }
            PhoneNumberDesc mobileDesc = getNumberDescByType(metadata, PhoneNumberType.MOBILE);
            if (descHasPossibleNumberData(mobileDesc)) {
                Collection possibleLengthList;
                possibleLengths = new ArrayList(possibleLengths);
                if (mobileDesc.getPossibleLengthList().size() == 0) {
                    possibleLengthList = metadata.getGeneralDesc().getPossibleLengthList();
                } else {
                    possibleLengthList = mobileDesc.getPossibleLengthList();
                }
                possibleLengths.addAll(possibleLengthList);
                Collections.sort(possibleLengths);
                if (localLengths.isEmpty()) {
                    localLengths = mobileDesc.getPossibleLengthLocalOnlyList();
                } else {
                    localLengths = new ArrayList(localLengths);
                    localLengths.addAll(mobileDesc.getPossibleLengthLocalOnlyList());
                    Collections.sort(localLengths);
                }
            }
        }
        if (((Integer) possibleLengths.get(0)).intValue() == -1) {
            return ValidationResult.INVALID_LENGTH;
        }
        int actualLength = number.length();
        if (localLengths.contains(Integer.valueOf(actualLength))) {
            return ValidationResult.IS_POSSIBLE_LOCAL_ONLY;
        }
        int minimumLength = ((Integer) possibleLengths.get(0)).intValue();
        if (minimumLength == actualLength) {
            return ValidationResult.IS_POSSIBLE;
        }
        if (minimumLength > actualLength) {
            return ValidationResult.TOO_SHORT;
        }
        if (((Integer) possibleLengths.get(possibleLengths.size() - 1)).intValue() < actualLength) {
            return ValidationResult.TOO_LONG;
        }
        return possibleLengths.subList(1, possibleLengths.size()).contains(Integer.valueOf(actualLength)) ? ValidationResult.IS_POSSIBLE : ValidationResult.INVALID_LENGTH;
    }

    public ValidationResult isPossibleNumberWithReason(PhoneNumber number) {
        return isPossibleNumberForTypeWithReason(number, PhoneNumberType.UNKNOWN);
    }

    public ValidationResult isPossibleNumberForTypeWithReason(PhoneNumber number, PhoneNumberType type) {
        String nationalNumber = getNationalSignificantNumber(number);
        int countryCode = number.getCountryCode();
        if (hasValidCountryCallingCode(countryCode)) {
            return testNumberLength(nationalNumber, getMetadataForRegionOrCallingCode(countryCode, getRegionCodeForCountryCode(countryCode)), type);
        }
        return ValidationResult.INVALID_COUNTRY_CODE;
    }

    public boolean isPossibleNumber(CharSequence number, String regionDialingFrom) {
        try {
            return isPossibleNumber(parse(number, regionDialingFrom));
        } catch (NumberParseException e) {
            return false;
        }
    }

    public boolean truncateTooLongNumber(PhoneNumber number) {
        if (isValidNumber(number)) {
            return true;
        }
        PhoneNumber numberCopy = new PhoneNumber();
        numberCopy.mergeFrom(number);
        long nationalNumber = number.getNationalNumber();
        do {
            nationalNumber /= 10;
            numberCopy.setNationalNumber(nationalNumber);
            if (isPossibleNumberWithReason(numberCopy) == ValidationResult.TOO_SHORT || nationalNumber == 0) {
                return false;
            }
        } while (!isValidNumber(numberCopy));
        number.setNationalNumber(nationalNumber);
        return true;
    }

    public AsYouTypeFormatter getAsYouTypeFormatter(String regionCode) {
        return new AsYouTypeFormatter(regionCode);
    }

    int extractCountryCode(StringBuilder fullNumber, StringBuilder nationalNumber) {
        if (fullNumber.length() == 0 || fullNumber.charAt(0) == '0') {
            return 0;
        }
        int numberLength = fullNumber.length();
        int i = 1;
        while (i <= 3 && i <= numberLength) {
            int potentialCountryCode = Integer.parseInt(fullNumber.substring(0, i));
            if (this.countryCallingCodeToRegionCodeMap.containsKey(Integer.valueOf(potentialCountryCode))) {
                nationalNumber.append(fullNumber.substring(i));
                return potentialCountryCode;
            }
            i++;
        }
        return 0;
    }

    int maybeExtractCountryCode(CharSequence number, PhoneMetadata defaultRegionMetadata, StringBuilder nationalNumber, boolean keepRawInput, PhoneNumber phoneNumber) throws NumberParseException {
        PhoneMetadata phoneMetadata = defaultRegionMetadata;
        StringBuilder stringBuilder = nationalNumber;
        PhoneNumber phoneNumber2 = phoneNumber;
        if (number.length() == 0) {
            return 0;
        }
        StringBuilder fullNumber = new StringBuilder(number);
        String possibleCountryIddPrefix = "NonMatch";
        if (phoneMetadata != null) {
            possibleCountryIddPrefix = defaultRegionMetadata.getInternationalPrefix();
        }
        CountryCodeSource countryCodeSource = maybeStripInternationalPrefixAndNormalize(fullNumber, possibleCountryIddPrefix);
        if (keepRawInput) {
            phoneNumber2.setCountryCodeSource(countryCodeSource);
        }
        if (countryCodeSource == CountryCodeSource.FROM_DEFAULT_COUNTRY) {
            if (phoneMetadata != null) {
                int defaultCountryCode = defaultRegionMetadata.getCountryCode();
                String defaultCountryCodeString = String.valueOf(defaultCountryCode);
                String normalizedNumber = fullNumber.toString();
                if (normalizedNumber.startsWith(defaultCountryCodeString)) {
                    StringBuilder potentialNationalNumber = new StringBuilder(normalizedNumber.substring(defaultCountryCodeString.length()));
                    PhoneNumberDesc generalDesc = defaultRegionMetadata.getGeneralDesc();
                    maybeStripNationalPrefixAndCarrierCode(potentialNationalNumber, phoneMetadata, null);
                    if ((!this.matcherApi.matchNationalNumber(fullNumber, generalDesc, false) && this.matcherApi.matchNationalNumber(potentialNationalNumber, generalDesc, false)) || testNumberLength(fullNumber, phoneMetadata) == ValidationResult.TOO_LONG) {
                        stringBuilder.append(potentialNationalNumber);
                        if (keepRawInput) {
                            phoneNumber2.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN);
                        }
                        phoneNumber2.setCountryCode(defaultCountryCode);
                        return defaultCountryCode;
                    }
                }
            }
            phoneNumber2.setCountryCode(0);
            return 0;
        } else if (fullNumber.length() > 2) {
            int potentialCountryCode = extractCountryCode(fullNumber, stringBuilder);
            if (potentialCountryCode != 0) {
                phoneNumber2.setCountryCode(potentialCountryCode);
                return potentialCountryCode;
            }
            throw new NumberParseException(ErrorType.INVALID_COUNTRY_CODE, "Country calling code supplied was not recognised.");
        } else {
            throw new NumberParseException(ErrorType.TOO_SHORT_AFTER_IDD, "Phone number had an IDD, but after this was not long enough to be a viable phone number.");
        }
    }

    private boolean parsePrefixAsIdd(Pattern iddPattern, StringBuilder number) {
        Matcher m = iddPattern.matcher(number);
        if (!m.lookingAt()) {
            return false;
        }
        int matchEnd = m.end();
        Matcher digitMatcher = CAPTURING_DIGIT_PATTERN.matcher(number.substring(matchEnd));
        if (digitMatcher.find() && normalizeDigitsOnly(digitMatcher.group(1)).equals("0")) {
            return false;
        }
        number.delete(0, matchEnd);
        return true;
    }

    CountryCodeSource maybeStripInternationalPrefixAndNormalize(StringBuilder number, String possibleIddPrefix) {
        if (number.length() == 0) {
            return CountryCodeSource.FROM_DEFAULT_COUNTRY;
        }
        Matcher m = PLUS_CHARS_PATTERN.matcher(number);
        if (m.lookingAt()) {
            number.delete(0, m.end());
            normalize(number);
            return CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN;
        }
        CountryCodeSource countryCodeSource;
        Pattern iddPattern = this.regexCache.getPatternForRegex(possibleIddPrefix);
        normalize(number);
        if (parsePrefixAsIdd(iddPattern, number)) {
            countryCodeSource = CountryCodeSource.FROM_NUMBER_WITH_IDD;
        } else {
            countryCodeSource = CountryCodeSource.FROM_DEFAULT_COUNTRY;
        }
        return countryCodeSource;
    }

    boolean maybeStripNationalPrefixAndCarrierCode(StringBuilder number, PhoneMetadata metadata, StringBuilder carrierCode) {
        int numberLength = number.length();
        String possibleNationalPrefix = metadata.getNationalPrefixForParsing();
        if (numberLength == 0 || possibleNationalPrefix.length() == 0) {
            return false;
        }
        Matcher prefixMatcher = this.regexCache.getPatternForRegex(possibleNationalPrefix).matcher(number);
        if (!prefixMatcher.lookingAt()) {
            return false;
        }
        PhoneNumberDesc generalDesc = metadata.getGeneralDesc();
        boolean isViableOriginalNumber = this.matcherApi.matchNationalNumber(number, generalDesc, false);
        int numOfGroups = prefixMatcher.groupCount();
        String transformRule = metadata.getNationalPrefixTransformRule();
        if (transformRule != null && transformRule.length() != 0 && prefixMatcher.group(numOfGroups) != null) {
            StringBuilder transformedNumber = new StringBuilder(number);
            transformedNumber.replace(0, numberLength, prefixMatcher.replaceFirst(transformRule));
            if (isViableOriginalNumber && !this.matcherApi.matchNationalNumber(transformedNumber.toString(), generalDesc, false)) {
                return false;
            }
            if (carrierCode != null && numOfGroups > 1) {
                carrierCode.append(prefixMatcher.group(1));
            }
            number.replace(0, number.length(), transformedNumber.toString());
            return true;
        } else if (isViableOriginalNumber && !this.matcherApi.matchNationalNumber(number.substring(prefixMatcher.end()), generalDesc, false)) {
            return false;
        } else {
            if (!(carrierCode == null || numOfGroups <= 0 || prefixMatcher.group(numOfGroups) == null)) {
                carrierCode.append(prefixMatcher.group(1));
            }
            number.delete(0, prefixMatcher.end());
            return true;
        }
    }

    String maybeStripExtension(StringBuilder number) {
        Matcher m = EXTN_PATTERN.matcher(number);
        if (m.find() && isViablePhoneNumber(number.substring(0, m.start()))) {
            int length = m.groupCount();
            for (int i = 1; i <= length; i++) {
                if (m.group(i) != null) {
                    String extension = m.group(i);
                    number.delete(m.start(), number.length());
                    return extension;
                }
            }
        }
        return "";
    }

    private boolean checkRegionForParsing(CharSequence numberToParse, String defaultRegion) {
        if (isValidRegionCode(defaultRegion) || (numberToParse != null && numberToParse.length() != 0 && PLUS_CHARS_PATTERN.matcher(numberToParse).lookingAt())) {
            return true;
        }
        return false;
    }

    public PhoneNumber parse(CharSequence numberToParse, String defaultRegion) throws NumberParseException {
        PhoneNumber phoneNumber = new PhoneNumber();
        parse(numberToParse, defaultRegion, phoneNumber);
        return phoneNumber;
    }

    public void parse(CharSequence numberToParse, String defaultRegion, PhoneNumber phoneNumber) throws NumberParseException {
        parseHelper(numberToParse, defaultRegion, true, true, phoneNumber);
    }

    public PhoneNumber parseAndKeepRawInput(CharSequence numberToParse, String defaultRegion) throws NumberParseException {
        PhoneNumber phoneNumber = new PhoneNumber();
        parseAndKeepRawInput(numberToParse, defaultRegion, phoneNumber);
        return phoneNumber;
    }

    public void parseAndKeepRawInput(CharSequence numberToParse, String defaultRegion, PhoneNumber phoneNumber) throws NumberParseException {
        parseHelper(numberToParse, defaultRegion, true, true, phoneNumber);
    }

    public Iterable<PhoneNumberMatch> findNumbers(CharSequence text, String defaultRegion) {
        return findNumbers(text, defaultRegion, Leniency.VALID, Long.MAX_VALUE);
    }

    public Iterable<PhoneNumberMatch> findNumbers(CharSequence text, String defaultRegion, Leniency leniency, long maxTries) {
        final CharSequence charSequence = text;
        final String str = defaultRegion;
        final Leniency leniency2 = leniency;
        final long j = maxTries;
        return new Iterable<PhoneNumberMatch>() {
            public Iterator<PhoneNumberMatch> iterator() {
                return new PhoneNumberMatcher(PhoneNumberUtil.this, charSequence, str, leniency2, j);
            }
        };
    }

    static void setItalianLeadingZerosForPhoneNumber(CharSequence nationalNumber, PhoneNumber phoneNumber) {
        if (nationalNumber.length() > 1 && nationalNumber.charAt(0) == '0') {
            phoneNumber.setItalianLeadingZero(true);
            int numberOfLeadingZeros = 1;
            while (numberOfLeadingZeros < nationalNumber.length() - 1 && nationalNumber.charAt(numberOfLeadingZeros) == '0') {
                numberOfLeadingZeros++;
            }
            if (numberOfLeadingZeros != 1) {
                phoneNumber.setNumberOfLeadingZeros(numberOfLeadingZeros);
            }
        }
    }

    private void parseHelper(CharSequence numberToParse, String defaultRegion, boolean keepRawInput, boolean checkRegion, PhoneNumber phoneNumber) throws NumberParseException {
        Matcher matcher;
        int countryCode;
        String str = defaultRegion;
        PhoneNumber phoneNumber2 = phoneNumber;
        if (numberToParse == null) {
            throw new NumberParseException(ErrorType.NOT_A_NUMBER, "The phone number supplied was null.");
        } else if (numberToParse.length() <= MAX_INPUT_STRING_LENGTH) {
            StringBuilder nationalNumber = new StringBuilder();
            String numberBeingParsed = numberToParse.toString();
            buildNationalNumberForParsing(numberBeingParsed, nationalNumber);
            if (!isViablePhoneNumber(nationalNumber)) {
                throw new NumberParseException(ErrorType.NOT_A_NUMBER, "The string supplied did not seem to be a phone number.");
            } else if (!checkRegion || checkRegionForParsing(nationalNumber, str)) {
                int countryCode2;
                PhoneMetadata phoneMetadata;
                StringBuilder stringBuilder;
                if (keepRawInput) {
                    phoneNumber2.setRawInput(numberBeingParsed);
                }
                String extension = maybeStripExtension(nationalNumber);
                if (extension.length() > 0) {
                    phoneNumber2.setExtension(extension);
                }
                PhoneMetadata regionMetadata = getMetadataForRegion(str);
                StringBuilder normalizedNationalNumber = new StringBuilder();
                int countryCode3 = 0;
                StringBuilder normalizedNationalNumber2 = normalizedNationalNumber;
                PhoneMetadata regionMetadata2 = regionMetadata;
                try {
                    countryCode2 = maybeExtractCountryCode(nationalNumber, regionMetadata, normalizedNationalNumber, keepRawInput, phoneNumber2);
                } catch (NumberParseException e) {
                    NumberParseException numberParseException = e;
                    matcher = PLUS_CHARS_PATTERN.matcher(nationalNumber);
                    if (e.getErrorType() == ErrorType.INVALID_COUNTRY_CODE && matcher.lookingAt()) {
                        countryCode = maybeExtractCountryCode(nationalNumber.substring(matcher.end()), regionMetadata2, normalizedNationalNumber2, keepRawInput, phoneNumber2);
                        if (countryCode != 0) {
                            countryCode2 = countryCode;
                        } else {
                            phoneMetadata = regionMetadata2;
                            throw new NumberParseException(ErrorType.INVALID_COUNTRY_CODE, "Could not interpret numbers after plus-sign.");
                        }
                    }
                    stringBuilder = normalizedNationalNumber2;
                    phoneMetadata = regionMetadata2;
                    throw new NumberParseException(e.getErrorType(), e.getMessage());
                }
                if (countryCode2 != 0) {
                    String phoneNumberRegion = getRegionCodeForCountryCode(countryCode2);
                    if (phoneNumberRegion.equals(str)) {
                        matcher = regionMetadata2;
                    } else {
                        matcher = getMetadataForRegionOrCallingCode(countryCode2, phoneNumberRegion);
                    }
                    phoneMetadata = matcher;
                    stringBuilder = normalizedNationalNumber2;
                } else {
                    stringBuilder = normalizedNationalNumber2;
                    stringBuilder.append(normalize(nationalNumber));
                    if (str != null) {
                        phoneMetadata = regionMetadata2;
                        phoneNumber2.setCountryCode(phoneMetadata.getCountryCode());
                    } else {
                        phoneMetadata = regionMetadata2;
                        if (keepRawInput) {
                            phoneNumber.clearCountryCodeSource();
                        }
                    }
                }
                if (stringBuilder.length() >= 2) {
                    if (phoneMetadata != null) {
                        StringBuilder carrierCode = new StringBuilder();
                        normalizedNationalNumber = new StringBuilder(stringBuilder);
                        maybeStripNationalPrefixAndCarrierCode(normalizedNationalNumber, phoneMetadata, carrierCode);
                        matcher = testNumberLength(normalizedNationalNumber, phoneMetadata);
                        if (!(matcher == ValidationResult.TOO_SHORT || matcher == ValidationResult.IS_POSSIBLE_LOCAL_ONLY || matcher == ValidationResult.INVALID_LENGTH)) {
                            stringBuilder = normalizedNationalNumber;
                            if (keepRawInput && carrierCode.length() > 0) {
                                phoneNumber2.setPreferredDomesticCarrierCode(carrierCode.toString());
                            }
                        }
                    }
                    countryCode = stringBuilder.length();
                    if (countryCode < 2) {
                        throw new NumberParseException(ErrorType.TOO_SHORT_NSN, "The string supplied is too short to be a phone number.");
                    } else if (countryCode <= MAX_LENGTH_FOR_NSN) {
                        setItalianLeadingZerosForPhoneNumber(stringBuilder, phoneNumber2);
                        phoneNumber2.setNationalNumber(Long.parseLong(stringBuilder.toString()));
                        return;
                    } else {
                        throw new NumberParseException(ErrorType.TOO_LONG, "The string supplied is too long to be a phone number.");
                    }
                }
                throw new NumberParseException(ErrorType.TOO_SHORT_NSN, "The string supplied is too short to be a phone number.");
            } else {
                throw new NumberParseException(ErrorType.INVALID_COUNTRY_CODE, "Missing or invalid default region.");
            }
        } else {
            throw new NumberParseException(ErrorType.TOO_LONG, "The string supplied was too long to parse.");
        }
    }

    private void buildNationalNumberForParsing(String numberToParse, StringBuilder nationalNumber) {
        int phoneContextStart;
        int indexOfPhoneContext = numberToParse.indexOf(RFC3966_PHONE_CONTEXT);
        if (indexOfPhoneContext >= 0) {
            int phoneContextEnd;
            phoneContextStart = RFC3966_PHONE_CONTEXT.length() + indexOfPhoneContext;
            if (phoneContextStart < numberToParse.length() - 1 && numberToParse.charAt(phoneContextStart) == PLUS_SIGN) {
                phoneContextEnd = numberToParse.indexOf(59, phoneContextStart);
                if (phoneContextEnd > 0) {
                    nationalNumber.append(numberToParse.substring(phoneContextStart, phoneContextEnd));
                } else {
                    nationalNumber.append(numberToParse.substring(phoneContextStart));
                }
            }
            phoneContextEnd = numberToParse.indexOf(RFC3966_PREFIX);
            nationalNumber.append(numberToParse.substring(phoneContextEnd >= 0 ? RFC3966_PREFIX.length() + phoneContextEnd : 0, indexOfPhoneContext));
        } else {
            nationalNumber.append(extractPossibleNumber(numberToParse));
        }
        phoneContextStart = nationalNumber.indexOf(RFC3966_ISDN_SUBADDRESS);
        if (phoneContextStart > 0) {
            nationalNumber.delete(phoneContextStart, nationalNumber.length());
        }
    }

    private static PhoneNumber copyCoreFieldsOnly(PhoneNumber phoneNumberIn) {
        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setCountryCode(phoneNumberIn.getCountryCode());
        phoneNumber.setNationalNumber(phoneNumberIn.getNationalNumber());
        if (phoneNumberIn.getExtension().length() > 0) {
            phoneNumber.setExtension(phoneNumberIn.getExtension());
        }
        if (phoneNumberIn.isItalianLeadingZero()) {
            phoneNumber.setItalianLeadingZero(true);
            phoneNumber.setNumberOfLeadingZeros(phoneNumberIn.getNumberOfLeadingZeros());
        }
        return phoneNumber;
    }

    public MatchType isNumberMatch(PhoneNumber firstNumberIn, PhoneNumber secondNumberIn) {
        PhoneNumber firstNumber = copyCoreFieldsOnly(firstNumberIn);
        PhoneNumber secondNumber = copyCoreFieldsOnly(secondNumberIn);
        if (firstNumber.hasExtension() && secondNumber.hasExtension() && !firstNumber.getExtension().equals(secondNumber.getExtension())) {
            return MatchType.NO_MATCH;
        }
        int firstNumberCountryCode = firstNumber.getCountryCode();
        int secondNumberCountryCode = secondNumber.getCountryCode();
        if (firstNumberCountryCode == 0 || secondNumberCountryCode == 0) {
            firstNumber.setCountryCode(secondNumberCountryCode);
            if (firstNumber.exactlySameAs(secondNumber)) {
                return MatchType.NSN_MATCH;
            }
            if (isNationalNumberSuffixOfTheOther(firstNumber, secondNumber)) {
                return MatchType.SHORT_NSN_MATCH;
            }
            return MatchType.NO_MATCH;
        } else if (firstNumber.exactlySameAs(secondNumber)) {
            return MatchType.EXACT_MATCH;
        } else {
            if (firstNumberCountryCode == secondNumberCountryCode && isNationalNumberSuffixOfTheOther(firstNumber, secondNumber)) {
                return MatchType.SHORT_NSN_MATCH;
            }
            return MatchType.NO_MATCH;
        }
    }

    private boolean isNationalNumberSuffixOfTheOther(PhoneNumber firstNumber, PhoneNumber secondNumber) {
        String firstNumberNationalNumber = String.valueOf(firstNumber.getNationalNumber());
        String secondNumberNationalNumber = String.valueOf(secondNumber.getNationalNumber());
        return firstNumberNationalNumber.endsWith(secondNumberNationalNumber) || secondNumberNationalNumber.endsWith(firstNumberNationalNumber);
    }

    public MatchType isNumberMatch(CharSequence firstNumber, CharSequence secondNumber) {
        try {
            return isNumberMatch(parse(firstNumber, UNKNOWN_REGION), secondNumber);
        } catch (NumberParseException e) {
            if (e.getErrorType() == ErrorType.INVALID_COUNTRY_CODE) {
                try {
                    return isNumberMatch(parse(secondNumber, UNKNOWN_REGION), firstNumber);
                } catch (NumberParseException e2) {
                    if (e2.getErrorType() == ErrorType.INVALID_COUNTRY_CODE) {
                        try {
                            PhoneNumber firstNumberProto = new PhoneNumber();
                            PhoneNumber secondNumberProto = new PhoneNumber();
                            parseHelper(firstNumber, null, false, false, firstNumberProto);
                            parseHelper(secondNumber, null, false, false, secondNumberProto);
                            return isNumberMatch(firstNumberProto, secondNumberProto);
                        } catch (NumberParseException e3) {
                            return MatchType.NOT_A_NUMBER;
                        }
                    }
                    return MatchType.NOT_A_NUMBER;
                }
            }
            return MatchType.NOT_A_NUMBER;
        }
    }

    public MatchType isNumberMatch(PhoneNumber firstNumber, CharSequence secondNumber) {
        try {
            return isNumberMatch(firstNumber, parse(secondNumber, UNKNOWN_REGION));
        } catch (NumberParseException e) {
            if (e.getErrorType() == ErrorType.INVALID_COUNTRY_CODE) {
                String firstNumberRegion = getRegionCodeForCountryCode(firstNumber.getCountryCode());
                try {
                    if (firstNumberRegion.equals(UNKNOWN_REGION)) {
                        PhoneNumber secondNumberProto = new PhoneNumber();
                        parseHelper(secondNumber, null, false, false, secondNumberProto);
                        return isNumberMatch(firstNumber, secondNumberProto);
                    }
                    MatchType match = isNumberMatch(firstNumber, parse(secondNumber, firstNumberRegion));
                    if (match == MatchType.EXACT_MATCH) {
                        return MatchType.NSN_MATCH;
                    }
                    return match;
                } catch (NumberParseException e2) {
                    return MatchType.NOT_A_NUMBER;
                }
            }
            return MatchType.NOT_A_NUMBER;
        }
    }

    public boolean canBeInternationallyDialled(PhoneNumber number) {
        PhoneMetadata metadata = getMetadataForRegion(getRegionCodeForNumber(number));
        if (metadata == null) {
            return true;
        }
        return 1 ^ isNumberMatchingDesc(getNationalSignificantNumber(number), metadata.getNoInternationalDialling());
    }

    public boolean isMobileNumberPortableRegion(String regionCode) {
        PhoneMetadata metadata = getMetadataForRegion(regionCode);
        if (metadata != null) {
            return metadata.isMobileNumberPortableRegion();
        }
        Logger logger = logger;
        Level level = Level.WARNING;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid or unknown region code provided: ");
        stringBuilder.append(regionCode);
        logger.log(level, stringBuilder.toString());
        return false;
    }
}
