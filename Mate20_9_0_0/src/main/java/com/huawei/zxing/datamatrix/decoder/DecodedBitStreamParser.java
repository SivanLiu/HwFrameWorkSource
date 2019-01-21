package com.huawei.zxing.datamatrix.decoder;

import com.huawei.android.app.AppOpsManagerEx;
import com.huawei.immersion.Vibetonz;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.common.BitSource;
import com.huawei.zxing.common.DecoderResult;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class DecodedBitStreamParser {
    private static final char[] C40_BASIC_SET_CHARS = new char[]{'*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    private static final char[] C40_SHIFT2_SET_CHARS = new char[]{'!', '\"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_'};
    private static final char[] TEXT_BASIC_SET_CHARS = new char[]{'*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static final char[] TEXT_SHIFT3_SET_CHARS = new char[]{'`', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '{', '|', '}', '~', 127};

    private enum Mode {
        PAD_ENCODE,
        ASCII_ENCODE,
        C40_ENCODE,
        TEXT_ENCODE,
        ANSIX12_ENCODE,
        EDIFACT_ENCODE,
        BASE256_ENCODE
    }

    private DecodedBitStreamParser() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x0059  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x006f  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x006d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static DecoderResult decode(byte[] bytes) throws FormatException {
        BitSource bits = new BitSource(bytes);
        StringBuilder result = new StringBuilder(100);
        StringBuilder resultTrailer = new StringBuilder(0);
        List<byte[]> byteSegments = new ArrayList(1);
        Mode mode = Mode.ASCII_ENCODE;
        do {
            if (mode == Mode.ASCII_ENCODE) {
                mode = decodeAsciiSegment(bits, result, resultTrailer);
            } else {
                switch (mode) {
                    case C40_ENCODE:
                        decodeC40Segment(bits, result);
                        break;
                    case TEXT_ENCODE:
                        decodeTextSegment(bits, result);
                        break;
                    case ANSIX12_ENCODE:
                        decodeAnsiX12Segment(bits, result);
                        break;
                    case EDIFACT_ENCODE:
                        decodeEdifactSegment(bits, result);
                        break;
                    case BASE256_ENCODE:
                        decodeBase256Segment(bits, result, byteSegments);
                        break;
                    default:
                        throw FormatException.getFormatInstance();
                }
                mode = Mode.ASCII_ENCODE;
            }
            if (mode != Mode.PAD_ENCODE) {
            }
            if (resultTrailer.length() > 0) {
                result.append(resultTrailer.toString());
            }
            return new DecoderResult(bytes, result.toString(), byteSegments.isEmpty() ? null : byteSegments, null);
        } while (bits.available() > 0);
        if (resultTrailer.length() > 0) {
        }
        if (byteSegments.isEmpty()) {
        }
        return new DecoderResult(bytes, result.toString(), byteSegments.isEmpty() ? null : byteSegments, null);
    }

    private static Mode decodeAsciiSegment(BitSource bits, StringBuilder result, StringBuilder resultTrailer) throws FormatException {
        boolean upperShift = false;
        do {
            int oneByte = bits.readBits(8);
            if (oneByte == 0) {
                throw FormatException.getFormatInstance();
            } else if (oneByte <= AppOpsManagerEx.TYPE_MICROPHONE) {
                if (upperShift) {
                    oneByte += AppOpsManagerEx.TYPE_MICROPHONE;
                }
                result.append((char) (oneByte - 1));
                return Mode.ASCII_ENCODE;
            } else if (oneByte == 129) {
                return Mode.PAD_ENCODE;
            } else {
                if (oneByte <= 229) {
                    int value = oneByte - 130;
                    if (value < 10) {
                        result.append('0');
                    }
                    result.append(value);
                } else if (oneByte == 230) {
                    return Mode.C40_ENCODE;
                } else {
                    if (oneByte == 231) {
                        return Mode.BASE256_ENCODE;
                    }
                    if (oneByte == 232) {
                        result.append(29);
                    } else if (!(oneByte == 233 || oneByte == 234)) {
                        if (oneByte == 235) {
                            upperShift = true;
                        } else if (oneByte == 236) {
                            result.append("[)>\u001e05\u001d");
                            resultTrailer.insert(0, "\u001e\u0004");
                        } else if (oneByte == 237) {
                            result.append("[)>\u001e06\u001d");
                            resultTrailer.insert(0, "\u001e\u0004");
                        } else if (oneByte == 238) {
                            return Mode.ANSIX12_ENCODE;
                        } else {
                            if (oneByte == 239) {
                                return Mode.TEXT_ENCODE;
                            }
                            if (oneByte == 240) {
                                return Mode.EDIFACT_ENCODE;
                            }
                            if (!(oneByte == 241 || oneByte < 242 || (oneByte == 254 && bits.available() == 0))) {
                                throw FormatException.getFormatInstance();
                            }
                        }
                    }
                }
            }
        } while (bits.available() > 0);
        return Mode.ASCII_ENCODE;
    }

    private static void decodeC40Segment(BitSource bits, StringBuilder result) throws FormatException {
        int[] cValues = new int[3];
        boolean upperShift = false;
        int shift = 0;
        while (bits.available() != 8) {
            int firstByte = bits.readBits(8);
            if (firstByte != 254) {
                parseTwoBytes(firstByte, bits.readBits(8), cValues);
                boolean upperShift2 = upperShift;
                int shift2 = shift;
                for (shift = 0; shift < 3; shift++) {
                    int cValue = cValues[shift];
                    char c40char;
                    switch (shift2) {
                        case 0:
                            if (cValue < 3) {
                                shift2 = cValue + 1;
                                break;
                            } else if (cValue < C40_BASIC_SET_CHARS.length) {
                                c40char = C40_BASIC_SET_CHARS[cValue];
                                if (!upperShift2) {
                                    result.append(c40char);
                                    break;
                                }
                                result.append((char) (c40char + AppOpsManagerEx.TYPE_MICROPHONE));
                                upperShift2 = false;
                                break;
                            } else {
                                throw FormatException.getFormatInstance();
                            }
                        case 1:
                            if (upperShift2) {
                                result.append((char) (cValue + AppOpsManagerEx.TYPE_MICROPHONE));
                                upperShift2 = false;
                            } else {
                                result.append((char) cValue);
                            }
                            shift2 = 0;
                            break;
                        case 2:
                            if (cValue < C40_SHIFT2_SET_CHARS.length) {
                                c40char = C40_SHIFT2_SET_CHARS[cValue];
                                if (upperShift2) {
                                    result.append((char) (c40char + AppOpsManagerEx.TYPE_MICROPHONE));
                                    upperShift2 = false;
                                } else {
                                    result.append(c40char);
                                }
                            } else if (cValue == 27) {
                                result.append(29);
                            } else if (cValue == 30) {
                                upperShift2 = true;
                            } else {
                                throw FormatException.getFormatInstance();
                            }
                            shift2 = 0;
                            break;
                        case 3:
                            if (upperShift2) {
                                result.append((char) (cValue + 224));
                                upperShift2 = false;
                            } else {
                                result.append((char) (cValue + 96));
                            }
                            shift2 = 0;
                            break;
                        default:
                            throw FormatException.getFormatInstance();
                    }
                }
                if (bits.available() > 0) {
                    shift = shift2;
                    upperShift = upperShift2;
                } else {
                    return;
                }
            }
            return;
        }
    }

    private static void decodeTextSegment(BitSource bits, StringBuilder result) throws FormatException {
        int[] cValues = new int[3];
        boolean upperShift = false;
        int shift = 0;
        while (bits.available() != 8) {
            int firstByte = bits.readBits(8);
            if (firstByte != 254) {
                parseTwoBytes(firstByte, bits.readBits(8), cValues);
                boolean upperShift2 = upperShift;
                int shift2 = shift;
                for (shift = 0; shift < 3; shift++) {
                    int cValue = cValues[shift];
                    char textChar;
                    switch (shift2) {
                        case 0:
                            if (cValue < 3) {
                                shift2 = cValue + 1;
                                break;
                            } else if (cValue < TEXT_BASIC_SET_CHARS.length) {
                                textChar = TEXT_BASIC_SET_CHARS[cValue];
                                if (!upperShift2) {
                                    result.append(textChar);
                                    break;
                                }
                                result.append((char) (textChar + AppOpsManagerEx.TYPE_MICROPHONE));
                                upperShift2 = false;
                                break;
                            } else {
                                throw FormatException.getFormatInstance();
                            }
                        case 1:
                            if (upperShift2) {
                                result.append((char) (cValue + AppOpsManagerEx.TYPE_MICROPHONE));
                                upperShift2 = false;
                            } else {
                                result.append((char) cValue);
                            }
                            shift2 = 0;
                            break;
                        case 2:
                            if (cValue < C40_SHIFT2_SET_CHARS.length) {
                                textChar = C40_SHIFT2_SET_CHARS[cValue];
                                if (upperShift2) {
                                    result.append((char) (textChar + AppOpsManagerEx.TYPE_MICROPHONE));
                                    upperShift2 = false;
                                } else {
                                    result.append(textChar);
                                }
                            } else if (cValue == 27) {
                                result.append(29);
                            } else if (cValue == 30) {
                                upperShift2 = true;
                            } else {
                                throw FormatException.getFormatInstance();
                            }
                            shift2 = 0;
                            break;
                        case 3:
                            if (cValue < TEXT_SHIFT3_SET_CHARS.length) {
                                textChar = TEXT_SHIFT3_SET_CHARS[cValue];
                                if (upperShift2) {
                                    result.append((char) (textChar + AppOpsManagerEx.TYPE_MICROPHONE));
                                    upperShift2 = false;
                                } else {
                                    result.append(textChar);
                                }
                                shift2 = 0;
                                break;
                            }
                            throw FormatException.getFormatInstance();
                        default:
                            throw FormatException.getFormatInstance();
                    }
                }
                if (bits.available() > 0) {
                    shift = shift2;
                    upperShift = upperShift2;
                } else {
                    return;
                }
            }
            return;
        }
    }

    private static void decodeAnsiX12Segment(BitSource bits, StringBuilder result) throws FormatException {
        int[] cValues = new int[3];
        while (bits.available() != 8) {
            int firstByte = bits.readBits(8);
            if (firstByte != 254) {
                parseTwoBytes(firstByte, bits.readBits(8), cValues);
                for (int i = 0; i < 3; i++) {
                    int cValue = cValues[i];
                    if (cValue == 0) {
                        result.append(13);
                    } else if (cValue == 1) {
                        result.append('*');
                    } else if (cValue == 2) {
                        result.append('>');
                    } else if (cValue == 3) {
                        result.append(' ');
                    } else if (cValue < 14) {
                        result.append((char) (cValue + 44));
                    } else if (cValue < 40) {
                        result.append((char) (cValue + 51));
                    } else {
                        throw FormatException.getFormatInstance();
                    }
                }
                if (bits.available() <= 0) {
                    return;
                }
            }
            return;
        }
    }

    private static void parseTwoBytes(int firstByte, int secondByte, int[] result) {
        int fullBitValue = ((firstByte << 8) + secondByte) - 1;
        int temp = fullBitValue / Vibetonz.HAPTIC_EVENT_CONTACT_ALPHA_SWITCH;
        result[0] = temp;
        fullBitValue -= temp * Vibetonz.HAPTIC_EVENT_CONTACT_ALPHA_SWITCH;
        temp = fullBitValue / 40;
        result[1] = temp;
        result[2] = fullBitValue - (temp * 40);
    }

    private static void decodeEdifactSegment(BitSource bits, StringBuilder result) {
        while (bits.available() > 16) {
            for (int i = 0; i < 4; i++) {
                int edifactValue = bits.readBits(6);
                if (edifactValue == 31) {
                    int bitsLeft = 8 - bits.getBitOffset();
                    if (bitsLeft != 8) {
                        bits.readBits(bitsLeft);
                    }
                    return;
                }
                if ((edifactValue & 32) == 0) {
                    edifactValue |= 64;
                }
                result.append((char) edifactValue);
            }
            if (bits.available() <= 0) {
                return;
            }
        }
    }

    private static void decodeBase256Segment(BitSource bits, StringBuilder result, Collection<byte[]> byteSegments) throws FormatException {
        int count;
        int d1 = 1 + bits.getByteOffset();
        int codewordPosition = d1 + 1;
        d1 = unrandomize255State(bits.readBits(8), d1);
        if (d1 == 0) {
            count = bits.available() / 8;
        } else if (d1 < 250) {
            count = d1;
        } else {
            count = (250 * (d1 - 249)) + unrandomize255State(bits.readBits(8), codewordPosition);
            codewordPosition++;
        }
        if (count >= 0) {
            byte[] bytes = new byte[count];
            int i = 0;
            while (i < count) {
                if (bits.available() >= 8) {
                    int codewordPosition2 = codewordPosition + 1;
                    bytes[i] = (byte) unrandomize255State(bits.readBits(8), codewordPosition);
                    i++;
                    codewordPosition = codewordPosition2;
                } else {
                    throw FormatException.getFormatInstance();
                }
            }
            byteSegments.add(bytes);
            try {
                result.append(new String(bytes, "ISO8859_1"));
                return;
            } catch (UnsupportedEncodingException uee) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Platform does not support required encoding: ");
                stringBuilder.append(uee);
                throw new IllegalStateException(stringBuilder.toString());
            }
        }
        throw FormatException.getFormatInstance();
    }

    private static int unrandomize255State(int randomizedBase256Codeword, int base256CodewordPosition) {
        int tempVariable = randomizedBase256Codeword - (((149 * base256CodewordPosition) % 255) + 1);
        return tempVariable >= 0 ? tempVariable : tempVariable + 256;
    }
}
