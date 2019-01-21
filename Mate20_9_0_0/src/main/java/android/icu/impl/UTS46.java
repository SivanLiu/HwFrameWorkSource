package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.lang.UScript;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.IDNA;
import android.icu.text.IDNA.Error;
import android.icu.text.IDNA.Info;
import android.icu.text.Normalizer2;
import android.icu.text.Normalizer2.Mode;
import android.icu.text.StringPrepParseException;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import java.util.EnumSet;

public final class UTS46 extends IDNA {
    private static final int EN_AN_MASK = (U_MASK(5) | U_MASK(2));
    private static final int ES_CS_ET_ON_BN_NSM_MASK = (((((U_MASK(3) | U_MASK(6)) | U_MASK(4)) | U_MASK(10)) | U_MASK(18)) | U_MASK(17));
    private static final int L_EN_ES_CS_ET_ON_BN_NSM_MASK = (L_EN_MASK | ES_CS_ET_ON_BN_NSM_MASK);
    private static final int L_EN_MASK = (U_MASK(2) | L_MASK);
    private static final int L_MASK = U_MASK(0);
    private static final int L_R_AL_MASK = (L_MASK | R_AL_MASK);
    private static final int R_AL_AN_EN_ES_CS_ET_ON_BN_NSM_MASK = ((R_AL_MASK | EN_AN_MASK) | ES_CS_ET_ON_BN_NSM_MASK);
    private static final int R_AL_AN_MASK = (R_AL_MASK | U_MASK(5));
    private static final int R_AL_EN_AN_MASK = (R_AL_MASK | EN_AN_MASK);
    private static final int R_AL_MASK = (U_MASK(1) | U_MASK(13));
    private static int U_GC_M_MASK = ((U_MASK(6) | U_MASK(7)) | U_MASK(8));
    private static final byte[] asciiData = new byte[]{(byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) 0, (byte) -1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1};
    private static final EnumSet<Error> severeErrors = EnumSet.of(Error.LEADING_COMBINING_MARK, Error.DISALLOWED, Error.PUNYCODE, Error.LABEL_HAS_DOT, Error.INVALID_ACE_LABEL);
    private static final Normalizer2 uts46Norm2 = Normalizer2.getInstance(null, "uts46", Mode.COMPOSE);
    final int options;

    public UTS46(int options) {
        this.options = options;
    }

    public StringBuilder labelToASCII(CharSequence label, StringBuilder dest, Info info) {
        return process(label, true, true, dest, info);
    }

    public StringBuilder labelToUnicode(CharSequence label, StringBuilder dest, Info info) {
        return process(label, true, false, dest, info);
    }

    public StringBuilder nameToASCII(CharSequence name, StringBuilder dest, Info info) {
        process(name, false, true, dest, info);
        if (dest.length() >= 254 && !info.getErrors().contains(Error.DOMAIN_NAME_TOO_LONG) && isASCIIString(dest) && (dest.length() > 254 || dest.charAt(253) != '.')) {
            IDNA.addError(info, Error.DOMAIN_NAME_TOO_LONG);
        }
        return dest;
    }

    public StringBuilder nameToUnicode(CharSequence name, StringBuilder dest, Info info) {
        return process(name, false, false, dest, info);
    }

    private static boolean isASCIIString(CharSequence dest) {
        int length = dest.length();
        for (int i = 0; i < length; i++) {
            if (dest.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    private StringBuilder process(CharSequence src, boolean isLabel, boolean toASCII, StringBuilder dest, Info info) {
        CharSequence charSequence = src;
        StringBuilder stringBuilder = dest;
        Info info2 = info;
        if (stringBuilder != charSequence) {
            int i = 0;
            stringBuilder.delete(0, Integer.MAX_VALUE);
            IDNA.resetInfo(info);
            int srcLength = src.length();
            if (srcLength == 0) {
                IDNA.addError(info2, Error.EMPTY_LABEL);
                return stringBuilder;
            }
            int i2;
            int i3;
            boolean disallowNonLDHDot = (this.options & 2) != 0;
            int labelStart = 0;
            while (true) {
                i2 = i;
                if (i2 == srcLength) {
                    if (toASCII) {
                        if (i2 - labelStart > 63) {
                            IDNA.addLabelError(info2, Error.LABEL_TOO_LONG);
                        }
                        if (!isLabel && i2 >= 254 && (i2 > 254 || labelStart < i2)) {
                            IDNA.addError(info2, Error.DOMAIN_NAME_TOO_LONG);
                        }
                    }
                    IDNA.promoteAndResetLabelErrors(info);
                    return stringBuilder;
                }
                char c = charSequence.charAt(i2);
                if (c > 127) {
                    break;
                }
                int cData = asciiData[c];
                if (cData <= 0) {
                    if (cData < 0 && disallowNonLDHDot) {
                        break;
                    }
                    stringBuilder.append(c);
                    if (c == '-') {
                        if (i2 == labelStart + 3 && charSequence.charAt(i2 - 1) == '-') {
                            i2++;
                            break;
                        }
                        if (i2 == labelStart) {
                            IDNA.addLabelError(info2, Error.LEADING_HYPHEN);
                        }
                        if (i2 + 1 == srcLength || charSequence.charAt(i2 + 1) == '.') {
                            IDNA.addLabelError(info2, Error.TRAILING_HYPHEN);
                        }
                    } else if (c != '.') {
                        i3 = labelStart;
                        labelStart = i3;
                        i = i2 + 1;
                        charSequence = src;
                    } else if (isLabel) {
                        i2++;
                        break;
                    } else {
                        i3 = labelStart;
                        if (i2 == i3) {
                            IDNA.addLabelError(info2, Error.EMPTY_LABEL);
                        }
                        if (toASCII && i2 - i3 > 63) {
                            IDNA.addLabelError(info2, Error.LABEL_TOO_LONG);
                        }
                        IDNA.promoteAndResetLabelErrors(info);
                        labelStart = i2 + 1;
                        i = i2 + 1;
                        charSequence = src;
                    }
                } else {
                    stringBuilder.append((char) (c + 32));
                }
                i3 = labelStart;
                labelStart = i3;
                i = i2 + 1;
                charSequence = src;
            }
            int i4 = i2;
            IDNA.promoteAndResetLabelErrors(info);
            i3 = labelStart;
            processUnicode(charSequence, labelStart, i4, isLabel, toASCII, stringBuilder, info2);
            if (IDNA.isBiDi(info) && !IDNA.hasCertainErrors(info2, severeErrors) && (!IDNA.isOkBiDi(info) || (i3 > 0 && !isASCIIOkBiDi(stringBuilder, i3)))) {
                IDNA.addError(info2, Error.BIDI);
            }
            return stringBuilder;
        }
        throw new IllegalArgumentException();
    }

    private StringBuilder processUnicode(CharSequence src, int labelStart, int mappingStart, boolean isLabel, boolean toASCII, StringBuilder dest, Info info) {
        int labelLimit;
        CharSequence charSequence = src;
        int i = mappingStart;
        StringBuilder stringBuilder = dest;
        if (i == 0) {
            uts46Norm2.normalize(charSequence, stringBuilder);
        } else {
            uts46Norm2.normalizeSecondAndAppend(stringBuilder, charSequence.subSequence(i, src.length()));
        }
        boolean doMapDevChars = false;
        if (toASCII ? (this.options & 16) != 0 : (this.options & 32) != 0) {
            doMapDevChars = true;
        }
        int labelStart2 = labelStart;
        boolean doMapDevChars2 = doMapDevChars;
        int destLength = dest.length();
        int labelLimit2 = labelStart2;
        while (true) {
            labelLimit = labelLimit2;
            if (labelLimit >= destLength) {
                break;
            }
            char c = stringBuilder.charAt(labelLimit);
            if (c != '.' || isLabel) {
                if (223 > c || c > 8205 || !(c == 223 || c == 962 || c >= 8204)) {
                    labelLimit++;
                } else {
                    IDNA.setTransitionalDifferent(info);
                    if (doMapDevChars2) {
                        destLength = mapDevChars(stringBuilder, labelStart2, labelLimit);
                        doMapDevChars2 = false;
                    } else {
                        labelLimit++;
                    }
                }
                labelLimit2 = labelLimit;
            } else {
                int labelLength = labelLimit - labelStart2;
                labelLimit2 = processLabel(stringBuilder, labelStart2, labelLength, toASCII, info);
                IDNA.promoteAndResetLabelErrors(info);
                destLength += labelLimit2 - labelLength;
                int i2 = (labelLimit2 + 1) + labelStart2;
                labelLimit2 = i2;
                labelStart2 = i2;
            }
        }
        if (labelStart2 == 0 || labelStart2 < labelLimit) {
            processLabel(stringBuilder, labelStart2, labelLimit - labelStart2, toASCII, info);
            IDNA.promoteAndResetLabelErrors(info);
        }
        return stringBuilder;
    }

    private int mapDevChars(StringBuilder dest, int labelStart, int mappingStart) {
        boolean didMapDevChars = false;
        int length = dest.length();
        int length2 = mappingStart;
        while (length2 < length) {
            char c = dest.charAt(length2);
            int i;
            if (c == 223) {
                didMapDevChars = true;
                i = length2 + 1;
                dest.setCharAt(length2, 's');
                length2 = i + 1;
                dest.insert(i, 's');
                length++;
            } else if (c != 962) {
                switch (c) {
                    case 8204:
                    case 8205:
                        didMapDevChars = true;
                        dest.delete(length2, length2 + 1);
                        length--;
                        break;
                    default:
                        length2++;
                        break;
                }
            } else {
                didMapDevChars = true;
                i = length2 + 1;
                dest.setCharAt(length2, 963);
                length2 = i;
            }
        }
        if (!didMapDevChars) {
            return length;
        }
        dest.replace(labelStart, Integer.MAX_VALUE, uts46Norm2.normalize(dest.subSequence(labelStart, dest.length())));
        return dest.length();
    }

    private static boolean isNonASCIIDisallowedSTD3Valid(int c) {
        return c == 8800 || c == 8814 || c == 8815;
    }

    private static int replaceLabel(StringBuilder dest, int destLabelStart, int destLabelLength, CharSequence label, int labelLength) {
        if (label != dest) {
            dest.delete(destLabelStart, destLabelStart + destLabelLength).insert(destLabelStart, label);
        }
        return labelLength;
    }

    private int processLabel(StringBuilder dest, int labelStart, int labelLength, boolean toASCII, Info info) {
        StringBuilder labelString;
        boolean wasPunycode;
        int labelStart2;
        StringBuilder stringBuilder = dest;
        int labelLength2 = labelLength;
        Info info2 = info;
        int destLabelStart = labelStart;
        int destLabelLength = labelLength2;
        if (labelLength2 >= 4 && dest.charAt(labelStart) == ULocale.PRIVATE_USE_EXTENSION && stringBuilder.charAt(labelStart + 1) == 'n' && stringBuilder.charAt(labelStart + 2) == '-' && stringBuilder.charAt(labelStart + 3) == '-') {
            boolean wasPunycode2 = true;
            try {
                StringBuilder fromPunycode = Punycode.decode(stringBuilder.subSequence(labelStart + 4, labelStart + labelLength2), null);
                if (uts46Norm2.isNormalized(fromPunycode)) {
                    labelString = fromPunycode;
                    labelLength2 = fromPunycode.length();
                    wasPunycode = wasPunycode2;
                    labelStart2 = 0;
                } else {
                    IDNA.addLabelError(info2, Error.INVALID_ACE_LABEL);
                    return markBadACELabel(dest, labelStart, labelLength, toASCII, info);
                }
            } catch (StringPrepParseException e) {
                IDNA.addLabelError(info2, Error.PUNYCODE);
                return markBadACELabel(dest, labelStart, labelLength, toASCII, info);
            }
        }
        labelString = stringBuilder;
        wasPunycode = false;
        labelStart2 = labelStart;
        StringBuilder labelString2 = labelString;
        if (labelLength2 == 0) {
            IDNA.addLabelError(info2, Error.EMPTY_LABEL);
            return replaceLabel(stringBuilder, destLabelStart, destLabelLength, labelString2, labelLength2);
        }
        char oredChars;
        int labelLength3;
        int destLabelLength2;
        if (labelLength2 >= 4 && labelString2.charAt(labelStart2 + 2) == '-' && labelString2.charAt(labelStart2 + 3) == '-') {
            IDNA.addLabelError(info2, Error.HYPHEN_3_4);
        }
        if (labelString2.charAt(labelStart2) == '-') {
            IDNA.addLabelError(info2, Error.LEADING_HYPHEN);
        }
        if (labelString2.charAt((labelStart2 + labelLength2) - 1) == '-') {
            IDNA.addLabelError(info2, Error.TRAILING_HYPHEN);
        }
        int i = labelStart2;
        int limit = labelStart2 + labelLength2;
        char oredChars2 = 0;
        boolean disallowNonLDHDot = (this.options & 2) != 0;
        while (true) {
            char c = labelString2.charAt(i);
            if (c > 127) {
                oredChars2 = (char) (oredChars2 | c);
                if (disallowNonLDHDot && isNonASCIIDisallowedSTD3Valid(c)) {
                    IDNA.addLabelError(info2, Error.DISALLOWED);
                    labelString2.setCharAt(i, 65533);
                } else if (c == 65533) {
                    IDNA.addLabelError(info2, Error.DISALLOWED);
                }
            } else if (c == '.') {
                IDNA.addLabelError(info2, Error.LABEL_HAS_DOT);
                labelString2.setCharAt(i, 65533);
            } else if (disallowNonLDHDot && asciiData[c] < (byte) 0) {
                IDNA.addLabelError(info2, Error.DISALLOWED);
                labelString2.setCharAt(i, 65533);
            }
            oredChars = oredChars2;
            int i2 = i + 1;
            if (i2 >= limit) {
                break;
            }
            Object obj = null;
            i = i2;
            oredChars2 = oredChars;
        }
        int c2 = labelString2.codePointAt(labelStart2);
        if ((U_GET_GC_MASK(c2) & U_GC_M_MASK) != 0) {
            IDNA.addLabelError(info2, Error.LEADING_COMBINING_MARK);
            labelString2.setCharAt(labelStart2, 65533);
            if (c2 > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                labelString2.deleteCharAt(labelStart2 + 1);
                labelLength2--;
                if (labelString2 == stringBuilder) {
                    destLabelLength--;
                }
            }
        }
        int i3;
        char c3;
        if (!IDNA.hasCertainLabelErrors(info2, severeErrors)) {
            if ((this.options & 4) != 0 && (!IDNA.isBiDi(info) || IDNA.isOkBiDi(info))) {
                checkLabelBiDi(labelString2, labelStart2, labelLength2, info2);
            }
            if (!((this.options & 8) == 0 || (oredChars & 8204) != 8204 || isLabelOkContextJ(labelString2, labelStart2, labelLength2))) {
                IDNA.addLabelError(info2, Error.CONTEXTJ);
            }
            if ((this.options & 64) != 0 && oredChars >= 183) {
                checkLabelContextO(labelString2, labelStart2, labelLength2, info2);
            }
            if (toASCII) {
                if (wasPunycode) {
                    if (destLabelLength > 63) {
                        IDNA.addLabelError(info2, Error.LABEL_TOO_LONG);
                    }
                    return destLabelLength;
                } else if (oredChars >= 128) {
                    try {
                        StringBuilder punycode = Punycode.encode(labelString2.subSequence(labelStart2, labelStart2 + labelLength2), null);
                        punycode.insert(0, "xn--");
                        if (punycode.length() > 63) {
                            IDNA.addLabelError(info2, Error.LABEL_TOO_LONG);
                        }
                        return replaceLabel(stringBuilder, destLabelStart, destLabelLength, punycode, punycode.length());
                    } catch (StringPrepParseException e2) {
                        throw new ICUException(e2);
                    }
                } else if (labelLength2 > 63) {
                    IDNA.addLabelError(info2, Error.LABEL_TOO_LONG);
                }
            }
            labelLength3 = labelLength2;
            destLabelLength2 = destLabelLength;
        } else if (wasPunycode) {
            IDNA.addLabelError(info2, Error.INVALID_ACE_LABEL);
            labelLength3 = labelLength2;
            i3 = c2;
            c3 = oredChars;
            return markBadACELabel(stringBuilder, destLabelStart, destLabelLength, toASCII, info2);
        } else {
            labelLength3 = labelLength2;
            destLabelLength2 = destLabelLength;
            i3 = c2;
            c3 = oredChars;
        }
        return replaceLabel(stringBuilder, destLabelStart, destLabelLength2, labelString2, labelLength3);
    }

    private int markBadACELabel(StringBuilder dest, int labelStart, int labelLength, boolean toASCII, Info info) {
        boolean disallowNonLDHDot = (this.options & 2) != 0;
        boolean isASCII = true;
        boolean onlyLDH = true;
        int i = labelStart + 4;
        int limit = labelStart + labelLength;
        do {
            char c = dest.charAt(i);
            if (c > 127) {
                onlyLDH = false;
                isASCII = false;
            } else if (c == '.') {
                IDNA.addLabelError(info, Error.LABEL_HAS_DOT);
                dest.setCharAt(i, 65533);
                onlyLDH = false;
                isASCII = false;
            } else if (asciiData[c] < (byte) 0) {
                onlyLDH = false;
                if (disallowNonLDHDot) {
                    dest.setCharAt(i, 65533);
                    isASCII = false;
                }
            }
            i++;
        } while (i < limit);
        if (onlyLDH) {
            dest.insert(labelStart + labelLength, 65533);
            return labelLength + 1;
        } else if (!toASCII || !isASCII || labelLength <= 63) {
            return labelLength;
        } else {
            IDNA.addLabelError(info, Error.LABEL_TOO_LONG);
            return labelLength;
        }
    }

    private void checkLabelBiDi(CharSequence label, int labelStart, int labelLength, Info info) {
        int dir;
        int i = labelStart;
        int c = Character.codePointAt(label, i);
        i += Character.charCount(c);
        int firstMask = U_MASK(UBiDiProps.INSTANCE.getClass(c));
        if (((~L_R_AL_MASK) & firstMask) != 0) {
            IDNA.setNotOkBiDi(info);
        }
        int labelLimit = labelStart + labelLength;
        while (i < labelLimit) {
            c = Character.codePointBefore(label, labelLimit);
            labelLimit -= Character.charCount(c);
            dir = UBiDiProps.INSTANCE.getClass(c);
            if (dir != 17) {
                dir = U_MASK(dir);
                break;
            }
        }
        dir = firstMask;
        if ((L_MASK & firstMask) == 0 ? ((~R_AL_EN_AN_MASK) & dir) == 0 : ((~L_EN_MASK) & dir) == 0) {
            IDNA.setNotOkBiDi(info);
        }
        int mask = firstMask | dir;
        while (i < labelLimit) {
            c = Character.codePointAt(label, i);
            i += Character.charCount(c);
            mask |= U_MASK(UBiDiProps.INSTANCE.getClass(c));
        }
        if ((L_MASK & firstMask) == 0) {
            if (((~R_AL_AN_EN_ES_CS_ET_ON_BN_NSM_MASK) & mask) != 0) {
                IDNA.setNotOkBiDi(info);
            }
            if ((EN_AN_MASK & mask) == EN_AN_MASK) {
                IDNA.setNotOkBiDi(info);
            }
        } else if (((~L_EN_ES_CS_ET_ON_BN_NSM_MASK) & mask) != 0) {
            IDNA.setNotOkBiDi(info);
        }
        if ((R_AL_AN_MASK & mask) != 0) {
            IDNA.setBiDi(info);
        }
    }

    private static boolean isASCIIOkBiDi(CharSequence s, int length) {
        int labelStart = 0;
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (i > labelStart) {
                    c = s.charAt(i - 1);
                    if (('a' > c || c > 'z') && ('0' > c || c > '9')) {
                        return false;
                    }
                }
                labelStart = i + 1;
            } else if (i == labelStart) {
                if ('a' > c || c > 'z') {
                    return false;
                }
            } else if (c <= ' ' && (c >= 28 || (9 <= c && c <= 13))) {
                return false;
            }
        }
        return true;
    }

    private boolean isLabelOkContextJ(CharSequence label, int labelStart, int labelLength) {
        int labelLimit = labelStart + labelLength;
        for (int i = labelStart; i < labelLimit; i++) {
            if (label.charAt(i) == 8204) {
                if (i == labelStart) {
                    return false;
                }
                int j = i;
                int c = Character.codePointBefore(label, j);
                j -= Character.charCount(c);
                if (uts46Norm2.getCombiningClass(c) == 9) {
                    continue;
                } else {
                    while (true) {
                        int type = UBiDiProps.INSTANCE.getJoiningType(c);
                        if (type == 5) {
                            if (j == 0) {
                                return false;
                            }
                            c = Character.codePointBefore(label, j);
                            j -= Character.charCount(c);
                        } else if (type != 3 && type != 2) {
                            return false;
                        } else {
                            j = i + 1;
                            while (j != labelLimit) {
                                c = Character.codePointAt(label, j);
                                j += Character.charCount(c);
                                type = UBiDiProps.INSTANCE.getJoiningType(c);
                                if (type != 5) {
                                    if (!(type == 4 || type == 2)) {
                                        return false;
                                    }
                                }
                            }
                            return false;
                        }
                    }
                }
            } else if (label.charAt(i) != 8205) {
                continue;
            } else if (i == labelStart) {
                return false;
            } else {
                if (uts46Norm2.getCombiningClass(Character.codePointBefore(label, i)) != 9) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkLabelContextO(CharSequence label, int labelStart, int labelLength, Info info) {
        int labelEnd = (labelStart + labelLength) - 1;
        int arabicDigits = 0;
        int i = labelStart;
        while (i <= labelEnd) {
            int c = label.charAt(i);
            if (c >= 183) {
                if (c <= 1785) {
                    if (c == 183) {
                        if (labelStart >= i || label.charAt(i - 1) != 'l' || i >= labelEnd || label.charAt(i + 1) != 'l') {
                            IDNA.addLabelError(info, Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (c == 885) {
                        if (i >= labelEnd || 14 != UScript.getScript(Character.codePointAt(label, i + 1))) {
                            IDNA.addLabelError(info, Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (c == 1523 || c == 1524) {
                        if (labelStart >= i || 19 != UScript.getScript(Character.codePointBefore(label, i))) {
                            IDNA.addLabelError(info, Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (1632 <= c) {
                        if (c <= 1641) {
                            if (arabicDigits > 0) {
                                IDNA.addLabelError(info, Error.CONTEXTO_DIGITS);
                            }
                            arabicDigits = -1;
                        } else if (1776 <= c) {
                            if (arabicDigits < 0) {
                                IDNA.addLabelError(info, Error.CONTEXTO_DIGITS);
                            }
                            arabicDigits = 1;
                        }
                    }
                } else if (c == 12539) {
                    c = labelStart;
                    while (c <= labelEnd) {
                        int c2 = Character.codePointAt(label, c);
                        int script = UScript.getScript(c2);
                        if (script == 20 || script == 22 || script == 17) {
                            break;
                        }
                        c += Character.charCount(c2);
                    }
                    IDNA.addLabelError(info, Error.CONTEXTO_PUNCTUATION);
                }
            }
            i++;
        }
    }

    private static int U_MASK(int x) {
        return 1 << x;
    }

    private static int U_GET_GC_MASK(int c) {
        return 1 << UCharacter.getType(c);
    }
}
