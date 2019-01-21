package com.huawei.zxing.client.result;

import com.huawei.android.smcs.SmartTrimProcessEvent;
import com.huawei.zxing.Result;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VCardResultParser extends ResultParser {
    private static final Pattern BEGIN_VCARD = Pattern.compile("BEGIN:VCARD", 2);
    private static final Pattern COMMA = Pattern.compile(SmartTrimProcessEvent.ST_EVENT_STRING_TOKEN);
    private static final Pattern CR_LF_SPACE_TAB = Pattern.compile("\r\n[ \t]");
    private static final Pattern EQUALS = Pattern.compile("=");
    private static final Pattern NEWLINE_ESCAPE = Pattern.compile("\\\\[nN]");
    private static final Pattern SEMICOLON = Pattern.compile(SmartTrimProcessEvent.ST_EVENT_INTER_STRING_TOKEN);
    private static final Pattern SEMICOLON_OR_COMMA = Pattern.compile("[;,]");
    private static final Pattern UNESCAPED_SEMICOLONS = Pattern.compile("(?<!\\\\);+");
    private static final Pattern VCARD_ESCAPES = Pattern.compile("\\\\([,;\\\\])");
    private static final Pattern VCARD_LIKE_DATE = Pattern.compile("\\d{4}-?\\d{2}-?\\d{2}");

    public AddressBookParsedResult parse(Result result) {
        String rawText = ResultParser.getMassagedText(result);
        Matcher m = BEGIN_VCARD.matcher(rawText);
        Matcher matcher;
        if (!m.find()) {
            matcher = m;
        } else if (m.start() != 0) {
            String str = rawText;
            matcher = m;
        } else {
            String[] toPrimaryValues;
            String[] toPrimaryValues2;
            String[] toTypes;
            String[] toPrimaryValues3;
            String[] toTypes2;
            String toPrimaryValue;
            String toPrimaryValue2;
            String[] toPrimaryValues4;
            String[] toTypes3;
            String toPrimaryValue3;
            String toPrimaryValue4;
            String toPrimaryValue5;
            String[] strArr;
            List<List<String>> names = matchVCardPrefixedField("FN", rawText, true, false);
            if (names == null) {
                names = matchVCardPrefixedField("N", rawText, true, false);
                formatNames(names);
            }
            List<String> nicknameString = matchSingleVCardPrefixedField("NICKNAME", rawText, true, false);
            String[] nicknames = nicknameString == null ? null : COMMA.split((CharSequence) nicknameString.get(0));
            List<List<String>> phoneNumbers = matchVCardPrefixedField("TEL", rawText, true, false);
            List<List<String>> emails = matchVCardPrefixedField("EMAIL", rawText, true, false);
            List<String> note = matchSingleVCardPrefixedField("NOTE", rawText, false, false);
            List<List<String>> addresses = matchVCardPrefixedField("ADR", rawText, true, true);
            List<String> org = matchSingleVCardPrefixedField("ORG", rawText, true, true);
            List<String> birthday = matchSingleVCardPrefixedField("BDAY", rawText, true, false);
            if (!(birthday == null || isLikeVCardDate((CharSequence) birthday.get(0)))) {
                birthday = null;
            }
            List<String> birthday2 = birthday;
            List<String> title = matchSingleVCardPrefixedField("TITLE", rawText, true, false);
            List<List<String>> urls = matchVCardPrefixedField("URL", rawText, true, false);
            List<String> instantMessenger = matchSingleVCardPrefixedField("IMPP", rawText, true, false);
            m = matchSingleVCardPrefixedField("GEO", rawText, true, false);
            String[] geo = m == null ? null : SEMICOLON_OR_COMMA.split((CharSequence) m.get(0));
            if (geo != null) {
                if (geo.length != 2) {
                    rawText = null;
                    toPrimaryValues = toPrimaryValues(names);
                    toPrimaryValues2 = toPrimaryValues(phoneNumbers);
                    toTypes = toTypes(phoneNumbers);
                    toPrimaryValues3 = toPrimaryValues(emails);
                    toTypes2 = toTypes(emails);
                    toPrimaryValue = toPrimaryValue(instantMessenger);
                    toPrimaryValue2 = toPrimaryValue(note);
                    toPrimaryValues4 = toPrimaryValues(addresses);
                    toTypes3 = toTypes(addresses);
                    toPrimaryValue3 = toPrimaryValue(org);
                    toPrimaryValue4 = toPrimaryValue(birthday2);
                    toPrimaryValue5 = toPrimaryValue(title);
                    strArr = toPrimaryValues;
                    return new AddressBookParsedResult(strArr, nicknames, null, toPrimaryValues2, toTypes, toPrimaryValues3, toTypes2, toPrimaryValue, toPrimaryValue2, toPrimaryValues4, toTypes3, toPrimaryValue3, toPrimaryValue4, toPrimaryValue5, toPrimaryValues(urls), rawText);
                }
            }
            rawText = geo;
            toPrimaryValues = toPrimaryValues(names);
            toPrimaryValues2 = toPrimaryValues(phoneNumbers);
            toTypes = toTypes(phoneNumbers);
            toPrimaryValues3 = toPrimaryValues(emails);
            toTypes2 = toTypes(emails);
            toPrimaryValue = toPrimaryValue(instantMessenger);
            toPrimaryValue2 = toPrimaryValue(note);
            toPrimaryValues4 = toPrimaryValues(addresses);
            toTypes3 = toTypes(addresses);
            toPrimaryValue3 = toPrimaryValue(org);
            toPrimaryValue4 = toPrimaryValue(birthday2);
            toPrimaryValue5 = toPrimaryValue(title);
            strArr = toPrimaryValues;
            return new AddressBookParsedResult(strArr, nicknames, null, toPrimaryValues2, toTypes, toPrimaryValues3, toTypes2, toPrimaryValue, toPrimaryValue2, toPrimaryValues4, toTypes3, toPrimaryValue3, toPrimaryValue4, toPrimaryValue5, toPrimaryValues(urls), rawText);
        }
        return null;
    }

    static List<List<String>> matchVCardPrefixedField(CharSequence prefix, String rawText, boolean trim, boolean parseFieldDivider) {
        String str = rawText;
        List<List<String>> matches = null;
        int i = 0;
        int max = rawText.length();
        while (i < max) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("(?:^|\n)");
            stringBuilder.append(prefix);
            stringBuilder.append("(?:;([^:]*))?:");
            Matcher matcher = Pattern.compile(stringBuilder.toString(), 2).matcher(str);
            if (i > 0) {
                i--;
            }
            if (!matcher.find(i)) {
                break;
            }
            int i2;
            int matchStart;
            i = matcher.end(0);
            int i3 = 1;
            String metadataString = matcher.group(1);
            List<String> metadata = null;
            boolean quotedPrintable = false;
            String quotedPrintableCharset = null;
            Matcher matcher2;
            if (metadataString != null) {
                String[] split = SEMICOLON.split(metadataString);
                int length = split.length;
                String quotedPrintableCharset2 = null;
                boolean quotedPrintable2 = false;
                List<String> metadata2 = null;
                int metadata3 = 0;
                while (metadata3 < length) {
                    String metadatum = split[metadata3];
                    if (metadata2 == null) {
                        metadata2 = new ArrayList(i3);
                    }
                    metadata2.add(metadatum);
                    String[] metadatumTokens = EQUALS.split(metadatum, 2);
                    i2 = i;
                    if (metadatumTokens.length > 1) {
                        matcher2 = matcher;
                        matcher = metadatumTokens[0];
                        String value = metadatumTokens[1];
                        if ("ENCODING".equalsIgnoreCase(matcher) != 0 && "QUOTED-PRINTABLE".equalsIgnoreCase(value) != 0) {
                            quotedPrintable2 = true;
                        } else if ("CHARSET".equalsIgnoreCase(matcher) != 0) {
                            quotedPrintableCharset2 = value;
                        }
                    } else {
                        matcher2 = matcher;
                    }
                    metadata3++;
                    i = i2;
                    matcher = matcher2;
                    i3 = 1;
                }
                i2 = i;
                matcher2 = matcher;
                metadata = metadata2;
                quotedPrintable = quotedPrintable2;
                quotedPrintableCharset = quotedPrintableCharset2;
            } else {
                i2 = i;
                matcher2 = matcher;
            }
            i = i2;
            while (true) {
                matchStart = i2;
                int indexOf = str.indexOf(10, i);
                i = indexOf;
                if (indexOf < 0) {
                    break;
                }
                if (i < rawText.length() - 1 && (str.charAt(i + 1) == ' ' || str.charAt(i + 1) == 9)) {
                    i += 2;
                } else if (!quotedPrintable) {
                    break;
                } else {
                    if (i < 1 || str.charAt(i - 1) != '=') {
                        if (i >= 2) {
                            if (str.charAt(i - 2) != '=') {
                                break;
                            }
                        }
                        break;
                    }
                    i++;
                }
                i2 = matchStart;
            }
            if (i < 0) {
                i = max;
            } else if (i > matchStart) {
                int i4;
                if (matches == null) {
                    i4 = 1;
                    matches = new ArrayList(1);
                } else {
                    i4 = 1;
                }
                if (i >= i4 && str.charAt(i - 1) == 13) {
                    i--;
                }
                String element = str.substring(matchStart, i);
                if (trim) {
                    element = element.trim();
                }
                if (quotedPrintable) {
                    element = decodeQuotedPrintable(element, quotedPrintableCharset);
                    if (parseFieldDivider) {
                        element = UNESCAPED_SEMICOLONS.matcher(element).replaceAll("\n").trim();
                    }
                } else {
                    if (parseFieldDivider) {
                        element = UNESCAPED_SEMICOLONS.matcher(element).replaceAll("\n").trim();
                    }
                    element = VCARD_ESCAPES.matcher(NEWLINE_ESCAPE.matcher(CR_LF_SPACE_TAB.matcher(element).replaceAll("")).replaceAll("\n")).replaceAll("$1");
                }
                if (metadata == null) {
                    List<String> match = new ArrayList(1);
                    match.add(element);
                    matches.add(match);
                } else {
                    metadata.add(0, element);
                    matches.add(metadata);
                }
                i++;
            } else {
                i++;
            }
        }
        CharSequence charSequence = prefix;
        return matches;
    }

    private static String decodeQuotedPrintable(CharSequence value, String charset) {
        int length = value.length();
        StringBuilder result = new StringBuilder(length);
        ByteArrayOutputStream fragmentBuffer = new ByteArrayOutputStream();
        int i = 0;
        while (i < length) {
            char c = value.charAt(i);
            if (!(c == 10 || c == 13)) {
                if (c != '=') {
                    maybeAppendFragment(fragmentBuffer, charset, result);
                    result.append(c);
                } else if (i < length - 2) {
                    char nextChar = value.charAt(i + 1);
                    if (!(nextChar == 13 || nextChar == 10)) {
                        char nextNextChar = value.charAt(i + 2);
                        int firstDigit = ResultParser.parseHexDigit(nextChar);
                        int secondDigit = ResultParser.parseHexDigit(nextNextChar);
                        if (firstDigit >= 0 && secondDigit >= 0) {
                            fragmentBuffer.write((firstDigit << 4) + secondDigit);
                        }
                        i += 2;
                    }
                }
            }
            i++;
        }
        maybeAppendFragment(fragmentBuffer, charset, result);
        return result.toString();
    }

    private static void maybeAppendFragment(ByteArrayOutputStream fragmentBuffer, String charset, StringBuilder result) {
        if (fragmentBuffer.size() > 0) {
            String fragment;
            byte[] fragmentBytes = fragmentBuffer.toByteArray();
            if (charset == null) {
                fragment = new String(fragmentBytes, Charset.forName("UTF-8"));
            } else {
                try {
                    fragment = new String(fragmentBytes, charset);
                } catch (UnsupportedEncodingException e) {
                    fragment = new String(fragmentBytes, Charset.forName("UTF-8"));
                }
            }
            fragmentBuffer.reset();
            result.append(fragment);
        }
    }

    static List<String> matchSingleVCardPrefixedField(CharSequence prefix, String rawText, boolean trim, boolean parseFieldDivider) {
        List<List<String>> values = matchVCardPrefixedField(prefix, rawText, trim, parseFieldDivider);
        return (values == null || values.isEmpty()) ? null : (List) values.get(0);
    }

    private static String toPrimaryValue(List<String> list) {
        return (list == null || list.isEmpty()) ? null : (String) list.get(0);
    }

    private static String[] toPrimaryValues(Collection<List<String>> lists) {
        if (lists == null || lists.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList(lists.size());
        for (List<String> list : lists) {
            String value = (String) list.get(0);
            if (!(value == null || value.isEmpty())) {
                result.add(value);
            }
        }
        return (String[]) result.toArray(new String[lists.size()]);
    }

    private static String[] toTypes(Collection<List<String>> lists) {
        if (lists == null || lists.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList(lists.size());
        for (List<String> list : lists) {
            String type = null;
            int i = 1;
            while (i < list.size()) {
                String metadatum = (String) list.get(i);
                int equals = metadatum.indexOf(61);
                if (equals < 0) {
                    type = metadatum;
                    break;
                } else if ("TYPE".equalsIgnoreCase(metadatum.substring(0, equals))) {
                    type = metadatum.substring(equals + 1);
                    break;
                } else {
                    i++;
                }
            }
            result.add(type);
        }
        return (String[]) result.toArray(new String[lists.size()]);
    }

    private static boolean isLikeVCardDate(CharSequence value) {
        return value == null || VCARD_LIKE_DATE.matcher(value).matches();
    }

    private static void formatNames(Iterable<List<String>> names) {
        if (names != null) {
            for (List<String> list : names) {
                String name = (String) list.get(0);
                String[] components = new String[5];
                int start = 0;
                int componentIndex = 0;
                while (componentIndex < components.length - 1) {
                    int indexOf = name.indexOf(59, start);
                    int end = indexOf;
                    if (indexOf < 0) {
                        break;
                    }
                    components[componentIndex] = name.substring(start, end);
                    componentIndex++;
                    start = end + 1;
                }
                components[componentIndex] = name.substring(start);
                StringBuilder newName = new StringBuilder(100);
                maybeAppendComponent(components, 3, newName);
                maybeAppendComponent(components, 1, newName);
                maybeAppendComponent(components, 2, newName);
                maybeAppendComponent(components, 0, newName);
                maybeAppendComponent(components, 4, newName);
                list.set(0, newName.toString().trim());
            }
        }
    }

    private static void maybeAppendComponent(String[] components, int i, StringBuilder newName) {
        if (components[i] != null && !components[i].isEmpty()) {
            if (newName.length() > 0) {
                newName.append(' ');
            }
            newName.append(components[i]);
        }
    }
}
