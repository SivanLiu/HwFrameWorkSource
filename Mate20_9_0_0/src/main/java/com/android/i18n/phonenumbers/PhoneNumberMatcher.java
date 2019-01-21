package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.PhoneNumberUtil.Leniency;
import com.android.i18n.phonenumbers.PhoneNumberUtil.MatchType;
import com.android.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.android.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.android.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource;
import gov.nist.core.Separators;
import java.lang.Character.UnicodeBlock;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PhoneNumberMatcher implements Iterator<PhoneNumberMatch> {
    private static final Pattern[] INNER_MATCHES = new Pattern[]{Pattern.compile("/+(.*)"), Pattern.compile("(\\([^(]*)"), Pattern.compile("(?:\\p{Z}-|-\\p{Z})\\p{Z}*(.+)"), Pattern.compile("[‒-―－]\\p{Z}*(.+)"), Pattern.compile("\\.+\\p{Z}*([^.]+)"), Pattern.compile("\\p{Z}+(\\P{Z}+)")};
    private static final Pattern LEAD_CLASS;
    private static final Pattern MATCHING_BRACKETS;
    private static final Pattern PATTERN;
    private static final Pattern PUB_PAGES = Pattern.compile("\\d{1,5}-+\\d{1,5}\\s{0,4}\\(\\d{1,4}");
    private static final Pattern SLASH_SEPARATED_DATES = Pattern.compile("(?:(?:[0-3]?\\d/[01]?\\d)|(?:[01]?\\d/[0-3]?\\d))/(?:[12]\\d)?\\d{2}");
    private static final Pattern TIME_STAMPS = Pattern.compile("[12]\\d{3}[-/]?[01]\\d[-/]?[0-3]\\d +[0-2]\\d$");
    private static final Pattern TIME_STAMPS_SUFFIX = Pattern.compile(":[0-5]\\d");
    private PhoneNumberMatch lastMatch = null;
    private final Leniency leniency;
    private long maxTries;
    private final PhoneNumberUtil phoneUtil;
    private final String preferredRegion;
    private int searchIndex = 0;
    private State state = State.NOT_READY;
    private final CharSequence text;

    interface NumberGroupingChecker {
        boolean checkGroups(PhoneNumberUtil phoneNumberUtil, PhoneNumber phoneNumber, StringBuilder stringBuilder, String[] strArr);
    }

    private enum State {
        NOT_READY,
        READY,
        DONE
    }

    static {
        String openingParens = "(\\[（［";
        String closingParens = ")\\]）］";
        String nonParens = new StringBuilder();
        nonParens.append("[^");
        nonParens.append(openingParens);
        nonParens.append(closingParens);
        nonParens.append("]");
        nonParens = nonParens.toString();
        String bracketPairLimit = limit(0, 3);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(?:[");
        stringBuilder.append(openingParens);
        stringBuilder.append("])?(?:");
        stringBuilder.append(nonParens);
        stringBuilder.append("+[");
        stringBuilder.append(closingParens);
        stringBuilder.append("])?");
        stringBuilder.append(nonParens);
        stringBuilder.append("+(?:[");
        stringBuilder.append(openingParens);
        stringBuilder.append("]");
        stringBuilder.append(nonParens);
        stringBuilder.append("+[");
        stringBuilder.append(closingParens);
        stringBuilder.append("])");
        stringBuilder.append(bracketPairLimit);
        stringBuilder.append(nonParens);
        stringBuilder.append(Separators.STAR);
        MATCHING_BRACKETS = Pattern.compile(stringBuilder.toString());
        String leadLimit = limit(0, 2);
        String punctuationLimit = limit(0, 4);
        String blockLimit = limit(0, 20);
        String punctuation = new StringBuilder();
        punctuation.append("[-x‐-―−ー－-／  ­​⁠　()（）［］.\\[\\]/~⁓∼～]");
        punctuation.append(punctuationLimit);
        punctuation = punctuation.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("\\p{Nd}");
        stringBuilder2.append(limit(1, 20));
        String digitSequence = stringBuilder2.toString();
        String leadClassChars = new StringBuilder();
        leadClassChars.append(openingParens);
        leadClassChars.append("+＋");
        leadClassChars = leadClassChars.toString();
        String leadClass = new StringBuilder();
        leadClass.append("[");
        leadClass.append(leadClassChars);
        leadClass.append("]");
        leadClass = leadClass.toString();
        LEAD_CLASS = Pattern.compile(leadClass);
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("(?:");
        stringBuilder3.append(leadClass);
        stringBuilder3.append(punctuation);
        stringBuilder3.append(Separators.RPAREN);
        stringBuilder3.append(leadLimit);
        stringBuilder3.append(digitSequence);
        stringBuilder3.append("(?:");
        stringBuilder3.append(punctuation);
        stringBuilder3.append(digitSequence);
        stringBuilder3.append(Separators.RPAREN);
        stringBuilder3.append(blockLimit);
        stringBuilder3.append("(?:");
        stringBuilder3.append(PhoneNumberUtil.EXTN_PATTERNS_FOR_MATCHING);
        stringBuilder3.append(")?");
        PATTERN = Pattern.compile(stringBuilder3.toString(), 66);
    }

    private static String limit(int lower, int upper) {
        if (lower < 0 || upper <= 0 || upper < lower) {
            throw new IllegalArgumentException();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append(lower);
        stringBuilder.append(Separators.COMMA);
        stringBuilder.append(upper);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    PhoneNumberMatcher(PhoneNumberUtil util, CharSequence text, String country, Leniency leniency, long maxTries) {
        if (util == null || leniency == null) {
            throw new NullPointerException();
        } else if (maxTries >= 0) {
            this.phoneUtil = util;
            this.text = text != null ? text : "";
            this.preferredRegion = country;
            this.leniency = leniency;
            this.maxTries = maxTries;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private PhoneNumberMatch find(int index) {
        Matcher matcher = PATTERN.matcher(this.text);
        while (this.maxTries > 0 && matcher.find(index)) {
            int start = matcher.start();
            CharSequence candidate = trimAfterFirstMatch(PhoneNumberUtil.SECOND_NUMBER_START_PATTERN, this.text.subSequence(start, matcher.end()));
            PhoneNumberMatch match = extractMatch(candidate, start);
            if (match != null) {
                return match;
            }
            index = start + candidate.length();
            this.maxTries--;
        }
        return null;
    }

    private static CharSequence trimAfterFirstMatch(Pattern pattern, CharSequence candidate) {
        Matcher trailingCharsMatcher = pattern.matcher(candidate);
        if (trailingCharsMatcher.find()) {
            return candidate.subSequence(0, trailingCharsMatcher.start());
        }
        return candidate;
    }

    static boolean isLatinLetter(char letter) {
        boolean z = false;
        if (!Character.isLetter(letter) && Character.getType(letter) != 6) {
            return false;
        }
        UnicodeBlock block = UnicodeBlock.of(letter);
        if (block.equals(UnicodeBlock.BASIC_LATIN) || block.equals(UnicodeBlock.LATIN_1_SUPPLEMENT) || block.equals(UnicodeBlock.LATIN_EXTENDED_A) || block.equals(UnicodeBlock.LATIN_EXTENDED_ADDITIONAL) || block.equals(UnicodeBlock.LATIN_EXTENDED_B) || block.equals(UnicodeBlock.COMBINING_DIACRITICAL_MARKS)) {
            z = true;
        }
        return z;
    }

    private static boolean isInvalidPunctuationSymbol(char character) {
        return character == '%' || Character.getType(character) == 26;
    }

    private PhoneNumberMatch extractMatch(CharSequence candidate, int offset) {
        if (SLASH_SEPARATED_DATES.matcher(candidate).find()) {
            return null;
        }
        if (TIME_STAMPS.matcher(candidate).find()) {
            if (TIME_STAMPS_SUFFIX.matcher(this.text.toString().substring(candidate.length() + offset)).lookingAt()) {
                return null;
            }
        }
        PhoneNumberMatch match = parseAndVerify(candidate, offset);
        if (match != null) {
            return match;
        }
        return extractInnerMatch(candidate, offset);
    }

    private PhoneNumberMatch extractInnerMatch(CharSequence candidate, int offset) {
        CharSequence charSequence = candidate;
        int i = offset;
        Pattern[] patternArr = INNER_MATCHES;
        int length = patternArr.length;
        int i2 = 0;
        int i3 = 0;
        while (i3 < length) {
            Matcher groupMatcher = patternArr[i3].matcher(charSequence);
            boolean isFirstMatch = true;
            while (groupMatcher.find() && this.maxTries > 0) {
                int i4;
                if (isFirstMatch) {
                    PhoneNumberMatch match = parseAndVerify(trimAfterFirstMatch(PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN, charSequence.subSequence(i2, groupMatcher.start())), i);
                    if (match != null) {
                        return match;
                    }
                    i4 = i3;
                    this.maxTries--;
                    isFirstMatch = false;
                } else {
                    i4 = i3;
                }
                PhoneNumberMatch match2 = parseAndVerify(trimAfterFirstMatch(PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN, groupMatcher.group(1)), groupMatcher.start(1) + i);
                if (match2 != null) {
                    return match2;
                }
                this.maxTries--;
                i3 = i4;
                i2 = 0;
            }
            i3++;
            i2 = 0;
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:32:0x009f, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private PhoneNumberMatch parseAndVerify(CharSequence candidate, int offset) {
        try {
            if (MATCHING_BRACKETS.matcher(candidate).matches()) {
                if (!PUB_PAGES.matcher(candidate).find()) {
                    if (this.leniency.compareTo(Leniency.VALID) >= 0) {
                        if (offset > 0 && !LEAD_CLASS.matcher(candidate).lookingAt()) {
                            char previousChar = this.text.charAt(offset - 1);
                            if (isInvalidPunctuationSymbol(previousChar) || isLatinLetter(previousChar)) {
                                return null;
                            }
                        }
                        int lastCharIndex = candidate.length() + offset;
                        if (lastCharIndex < this.text.length()) {
                            char nextChar = this.text.charAt(lastCharIndex);
                            if (isInvalidPunctuationSymbol(nextChar) || isLatinLetter(nextChar)) {
                                return null;
                            }
                        }
                    }
                    PhoneNumber number = this.phoneUtil.parseAndKeepRawInput(candidate, this.preferredRegion);
                    if (!(this.phoneUtil.getRegionCodeForCountryCode(number.getCountryCode()).equals("IL") && this.phoneUtil.getNationalSignificantNumber(number).length() == 4 && (offset == 0 || (offset > 0 && this.text.charAt(offset - 1) != '*'))) && this.leniency.verify(number, candidate, this.phoneUtil)) {
                        number.clearCountryCodeSource();
                        number.clearRawInput();
                        number.clearPreferredDomesticCarrierCode();
                        return new PhoneNumberMatch(offset, candidate.toString(), number);
                    }
                    return null;
                }
            }
            return null;
        } catch (NumberParseException e) {
        }
    }

    static boolean allNumberGroupsRemainGrouped(PhoneNumberUtil util, PhoneNumber number, StringBuilder normalizedCandidate, String[] formattedNumberGroups) {
        int fromIndex = 0;
        if (number.getCountryCodeSource() != CountryCodeSource.FROM_DEFAULT_COUNTRY) {
            String countryCode = Integer.toString(number.getCountryCode());
            fromIndex = normalizedCandidate.indexOf(countryCode) + countryCode.length();
        }
        int fromIndex2 = fromIndex;
        fromIndex = 0;
        while (fromIndex < formattedNumberGroups.length) {
            fromIndex2 = normalizedCandidate.indexOf(formattedNumberGroups[fromIndex], fromIndex2);
            if (fromIndex2 < 0) {
                return false;
            }
            fromIndex2 += formattedNumberGroups[fromIndex].length();
            if (fromIndex != 0 || fromIndex2 >= normalizedCandidate.length() || util.getNddPrefixForRegion(util.getRegionCodeForCountryCode(number.getCountryCode()), true) == null || !Character.isDigit(normalizedCandidate.charAt(fromIndex2))) {
                fromIndex++;
            } else {
                return normalizedCandidate.substring(fromIndex2 - formattedNumberGroups[fromIndex].length()).startsWith(util.getNationalSignificantNumber(number));
            }
        }
        return normalizedCandidate.substring(fromIndex2).contains(number.getExtension());
    }

    static boolean allNumberGroupsAreExactlyPresent(PhoneNumberUtil util, PhoneNumber number, StringBuilder normalizedCandidate, String[] formattedNumberGroups) {
        String[] candidateGroups = PhoneNumberUtil.NON_DIGITS_PATTERN.split(normalizedCandidate.toString());
        boolean z = true;
        int candidateNumberGroupIndex = number.hasExtension() ? candidateGroups.length - 2 : candidateGroups.length - 1;
        if (candidateGroups.length == 1 || candidateGroups[candidateNumberGroupIndex].contains(util.getNationalSignificantNumber(number))) {
            return true;
        }
        int formattedNumberGroupIndex = formattedNumberGroups.length - 1;
        while (formattedNumberGroupIndex > 0 && candidateNumberGroupIndex >= 0) {
            if (!candidateGroups[candidateNumberGroupIndex].equals(formattedNumberGroups[formattedNumberGroupIndex])) {
                return false;
            }
            formattedNumberGroupIndex--;
            candidateNumberGroupIndex--;
        }
        if (candidateNumberGroupIndex < 0 || !candidateGroups[candidateNumberGroupIndex].endsWith(formattedNumberGroups[0])) {
            z = false;
        }
        return z;
    }

    private static String[] getNationalNumberGroups(PhoneNumberUtil util, PhoneNumber number, NumberFormat formattingPattern) {
        if (formattingPattern != null) {
            return util.formatNsnUsingPattern(util.getNationalSignificantNumber(number), formattingPattern, PhoneNumberFormat.RFC3966).split("-");
        }
        String rfc3966Format = util.format(number, PhoneNumberFormat.RFC3966);
        int endIndex = rfc3966Format.indexOf(59);
        if (endIndex < 0) {
            endIndex = rfc3966Format.length();
        }
        return rfc3966Format.substring(rfc3966Format.indexOf(45) + 1, endIndex).split("-");
    }

    static boolean checkNumberGroupingIsValid(PhoneNumber number, CharSequence candidate, PhoneNumberUtil util, NumberGroupingChecker checker) {
        StringBuilder normalizedCandidate = PhoneNumberUtil.normalizeDigits(candidate, true);
        if (checker.checkGroups(util, number, normalizedCandidate, getNationalNumberGroups(util, number, null))) {
            return true;
        }
        PhoneMetadata alternateFormats = MetadataManager.getAlternateFormatsForCountry(number.getCountryCode());
        if (alternateFormats != null) {
            for (NumberFormat alternateFormat : alternateFormats.numberFormats()) {
                if (checker.checkGroups(util, number, normalizedCandidate, getNationalNumberGroups(util, number, alternateFormat))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean containsMoreThanOneSlashInNationalNumber(PhoneNumber number, String candidate) {
        int firstSlashInBodyIndex = candidate.indexOf(47);
        if (firstSlashInBodyIndex < 0) {
            return false;
        }
        int secondSlashInBodyIndex = candidate.indexOf(47, firstSlashInBodyIndex + 1);
        if (secondSlashInBodyIndex < 0) {
            return false;
        }
        boolean candidateHasCountryCode = number.getCountryCodeSource() == CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN || number.getCountryCodeSource() == CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN;
        if (candidateHasCountryCode && PhoneNumberUtil.normalizeDigitsOnly(candidate.substring(0, firstSlashInBodyIndex)).equals(Integer.toString(number.getCountryCode()))) {
            return candidate.substring(secondSlashInBodyIndex + 1).contains(Separators.SLASH);
        }
        return true;
    }

    static boolean containsOnlyValidXChars(PhoneNumber number, String candidate, PhoneNumberUtil util) {
        int index = 0;
        while (index < candidate.length() - 1) {
            char charAtIndex = candidate.charAt(index);
            if (charAtIndex == 'x' || charAtIndex == 'X') {
                char charAtNextIndex = candidate.charAt(index + 1);
                if (charAtNextIndex == 'x' || charAtNextIndex == 'X') {
                    index++;
                    if (util.isNumberMatch(number, candidate.substring(index)) != MatchType.NSN_MATCH) {
                        return false;
                    }
                } else if (!PhoneNumberUtil.normalizeDigitsOnly(candidate.substring(index)).equals(number.getExtension())) {
                    return false;
                }
            }
            index++;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:18:0x0059, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static boolean isNationalPrefixPresentIfRequired(PhoneNumber number, PhoneNumberUtil util) {
        if (number.getCountryCodeSource() != CountryCodeSource.FROM_DEFAULT_COUNTRY) {
            return true;
        }
        PhoneMetadata metadata = util.getMetadataForRegion(util.getRegionCodeForCountryCode(number.getCountryCode()));
        if (metadata == null) {
            return true;
        }
        NumberFormat formatRule = util.chooseFormattingPatternForNumber(metadata.numberFormats(), util.getNationalSignificantNumber(number));
        if (formatRule == null || formatRule.getNationalPrefixFormattingRule().length() <= 0 || formatRule.getNationalPrefixOptionalWhenFormatting() || PhoneNumberUtil.formattingRuleHasFirstGroupOnly(formatRule.getNationalPrefixFormattingRule())) {
            return true;
        }
        return util.maybeStripNationalPrefixAndCarrierCode(new StringBuilder(PhoneNumberUtil.normalizeDigitsOnly(number.getRawInput())), metadata, null);
    }

    public boolean hasNext() {
        if (this.state == State.NOT_READY) {
            this.lastMatch = find(this.searchIndex);
            if (this.lastMatch == null) {
                this.state = State.DONE;
            } else {
                this.searchIndex = this.lastMatch.end();
                this.state = State.READY;
            }
        }
        return this.state == State.READY;
    }

    public PhoneNumberMatch next() {
        if (hasNext()) {
            PhoneNumberMatch result = this.lastMatch;
            this.lastMatch = null;
            this.state = State.NOT_READY;
            return result;
        }
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
