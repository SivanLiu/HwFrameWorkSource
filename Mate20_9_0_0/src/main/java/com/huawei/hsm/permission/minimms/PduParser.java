package com.huawei.hsm.permission.minimms;

import com.huawei.connectivitylog.ConnectivityLogManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;

public class PduParser {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int END_STRING_FLAG = 0;
    private static final int LENGTH_QUOTE = 31;
    private static final boolean LOCAL_LOGV = false;
    private static final String LOG_TAG = "PduParser";
    private static final int LONG_INTEGER_LENGTH_MAX = 8;
    private static final int QUOTE = 127;
    private static final int QUOTED_STRING_FLAG = 34;
    private static final int SHORT_INTEGER_MAX = 127;
    private static final int SHORT_LENGTH_MAX = 30;
    private static final int TEXT_MAX = 127;
    private static final int TEXT_MIN = 32;
    private static final int TYPE_QUOTED_STRING = 1;
    private static final int TYPE_TEXT_STRING = 0;
    private static final int TYPE_TOKEN_STRING = 2;
    private PduHeaders mHeaders = null;
    private ByteArrayInputStream mPduDataStream = null;

    public PduParser(byte[] pduDataStream) {
        this.mPduDataStream = new ByteArrayInputStream(pduDataStream);
    }

    public int getTargetCount() {
        int sendToCount = 1;
        if (this.mPduDataStream == null) {
            return 1;
        }
        this.mHeaders = parseHeaders(this.mPduDataStream);
        if (this.mHeaders == null) {
            return 1;
        }
        EncodedStringValue[] diTo = this.mHeaders.getEncodedStringValues(151);
        if (diTo != null) {
            sendToCount = diTo.length;
        }
        return sendToCount;
    }

    private PduHeaders parseHeaders(ByteArrayInputStream pduDataStream) {
        if (pduDataStream == null) {
            return null;
        }
        boolean keepParsing = true;
        PduHeaders headers = new PduHeaders();
        while (keepParsing && pduDataStream.available() > 0) {
            pduDataStream.mark(1);
            int headerField = extractByteValue(pduDataStream);
            if (headerField < 32 || headerField > 127) {
                EncodedStringValue value;
                byte[] address;
                int token;
                switch (headerField) {
                    case 129:
                    case 130:
                    case 151:
                        value = parseEncodedStringValue(pduDataStream);
                        if (value != null) {
                            address = value.getTextString();
                            if (address != null) {
                                String str = new String(address, Charset.defaultCharset());
                                int endIndex = str.indexOf("/");
                                if (endIndex > 0) {
                                    str = str.substring(0, endIndex);
                                }
                                try {
                                    value.setTextString(str.getBytes(Charset.defaultCharset()));
                                } catch (NullPointerException e) {
                                    return null;
                                }
                            }
                            try {
                                headers.appendEncodedStringValue(value, headerField);
                                break;
                            } catch (NullPointerException e2) {
                                break;
                            } catch (RuntimeException e3) {
                                return null;
                            }
                        }
                        break;
                    case 131:
                    case 139:
                    case PduHeaders.TRANSACTION_ID /*152*/:
                    case PduHeaders.REPLY_CHARGING_ID /*158*/:
                    case PduHeaders.APPLIC_ID /*183*/:
                    case PduHeaders.REPLY_APPLIC_ID /*184*/:
                    case PduHeaders.AUX_APPLIC_ID /*185*/:
                    case PduHeaders.REPLACE_ID /*189*/:
                    case PduHeaders.CANCEL_ID /*190*/:
                        byte[] value2 = parseWapString(pduDataStream, 0);
                        if (value2 != null) {
                            try {
                                headers.setTextString(value2, headerField);
                                break;
                            } catch (NullPointerException e4) {
                                break;
                            } catch (RuntimeException e5) {
                                return null;
                            }
                        }
                        break;
                    case 132:
                        address = parseContentType(pduDataStream, new HashMap());
                        if (address != null) {
                            try {
                                headers.setTextString(address, 132);
                            } catch (NullPointerException e6) {
                            } catch (RuntimeException e7) {
                                return null;
                            }
                        }
                        keepParsing = false;
                        break;
                    case 133:
                    case 142:
                    case PduHeaders.REPLY_CHARGING_SIZE /*159*/:
                        try {
                            headers.setLongInteger(parseLongInteger(pduDataStream), headerField);
                            break;
                        } catch (RuntimeException e8) {
                            return null;
                        }
                    case PduHeaders.DELIVERY_REPORT /*134*/:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 148:
                    case 149:
                    case 153:
                    case PduHeaders.READ_STATUS /*155*/:
                    case PduHeaders.REPLY_CHARGING /*156*/:
                    case PduHeaders.STORE /*162*/:
                    case PduHeaders.MM_STATE /*163*/:
                    case PduHeaders.STORE_STATUS /*165*/:
                    case PduHeaders.STORED /*167*/:
                    case PduHeaders.TOTALS /*169*/:
                    case PduHeaders.QUOTAS /*171*/:
                    case PduHeaders.DISTRIBUTION_INDICATOR /*177*/:
                    case PduHeaders.RECOMMENDED_RETRIEVAL_MODE /*180*/:
                    case PduHeaders.CONTENT_CLASS /*186*/:
                    case PduHeaders.DRM_CONTENT /*187*/:
                    case PduHeaders.ADAPTATION_ALLOWED /*188*/:
                    case PduHeaders.CANCEL_STATUS /*191*/:
                        try {
                            headers.setOctet(extractByteValue(pduDataStream), headerField);
                            break;
                        } catch (InvalidHeaderValueException e9) {
                            return null;
                        } catch (RuntimeException e10) {
                            return null;
                        }
                    case 135:
                    case 136:
                    case PduHeaders.REPLY_CHARGING_DEADLINE /*157*/:
                        parseValueLength(pduDataStream);
                        token = extractByteValue(pduDataStream);
                        try {
                            long timeValue = parseLongInteger(pduDataStream);
                            if (129 == token) {
                                timeValue += System.currentTimeMillis() / 1000;
                            }
                            try {
                                headers.setLongInteger(timeValue, headerField);
                                break;
                            } catch (RuntimeException e11) {
                                return null;
                            }
                        } catch (RuntimeException e12) {
                            return null;
                        }
                    case 137:
                        parseValueLength(pduDataStream);
                        if (128 == extractByteValue(pduDataStream)) {
                            value = parseEncodedStringValue(pduDataStream);
                            if (value != null) {
                                byte[] address2 = value.getTextString();
                                if (address2 != null) {
                                    String str2 = new String(address2, Charset.defaultCharset());
                                    int endIndex2 = str2.indexOf("/");
                                    if (endIndex2 > 0) {
                                        str2 = str2.substring(0, endIndex2);
                                    }
                                    try {
                                        value.setTextString(str2.getBytes(Charset.defaultCharset()));
                                    } catch (NullPointerException e13) {
                                        return null;
                                    }
                                }
                            }
                        }
                        try {
                            value = new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes(Charset.defaultCharset()));
                        } catch (NullPointerException e14) {
                            return null;
                        }
                        try {
                            headers.setEncodedStringValue(value, 137);
                            break;
                        } catch (NullPointerException e15) {
                            break;
                        } catch (RuntimeException e16) {
                            return null;
                        }
                    case 138:
                        pduDataStream.mark(1);
                        token = extractByteValue(pduDataStream);
                        if (token >= 128) {
                            if (128 != token) {
                                if (129 != token) {
                                    if (130 != token) {
                                        if (131 == token) {
                                            headers.setTextString(PduHeaders.MESSAGE_CLASS_AUTO_STR.getBytes(Charset.defaultCharset()), 138);
                                            break;
                                        }
                                    }
                                    headers.setTextString(PduHeaders.MESSAGE_CLASS_INFORMATIONAL_STR.getBytes(Charset.defaultCharset()), 138);
                                    break;
                                }
                                headers.setTextString(PduHeaders.MESSAGE_CLASS_ADVERTISEMENT_STR.getBytes(Charset.defaultCharset()), 138);
                                break;
                            }
                            try {
                                headers.setTextString(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes(Charset.defaultCharset()), 138);
                                break;
                            } catch (NullPointerException e17) {
                                break;
                            } catch (RuntimeException e18) {
                                return null;
                            }
                        }
                        pduDataStream.reset();
                        address = parseWapString(pduDataStream, 0);
                        if (address != null) {
                            try {
                                headers.setTextString(address, 138);
                                break;
                            } catch (NullPointerException e19) {
                                break;
                            } catch (RuntimeException e20) {
                                return null;
                            }
                        }
                        break;
                    case 140:
                        token = extractByteValue(pduDataStream);
                        switch (token) {
                            case 137:
                            case 138:
                            case 139:
                            case 140:
                            case 141:
                            case 142:
                            case 143:
                            case 144:
                            case 145:
                            case 146:
                            case 147:
                            case 148:
                            case 149:
                            case 150:
                            case 151:
                                return null;
                            default:
                                try {
                                    headers.setOctet(token, headerField);
                                    break;
                                } catch (InvalidHeaderValueException e21) {
                                    return null;
                                } catch (RuntimeException e22) {
                                    return null;
                                }
                        }
                    case 141:
                        try {
                            headers.setOctet(parseShortInteger(pduDataStream), 141);
                            break;
                        } catch (InvalidHeaderValueException e23) {
                            return null;
                        } catch (RuntimeException e24) {
                            return null;
                        }
                    case 147:
                    case 150:
                    case PduHeaders.RETRIEVE_TEXT /*154*/:
                    case PduHeaders.STORE_STATUS_TEXT /*166*/:
                    case PduHeaders.RECOMMENDED_RETRIEVAL_MODE_TEXT /*181*/:
                    case PduHeaders.STATUS_TEXT /*182*/:
                        if (150 == headerField) {
                            pduDataStream.mark(1);
                            if ((pduDataStream.read() & 255) != 0) {
                                pduDataStream.reset();
                            }
                        }
                        value = parseEncodedStringValue(pduDataStream);
                        if (value != null) {
                            try {
                                headers.setEncodedStringValue(value, headerField);
                                break;
                            } catch (NullPointerException e25) {
                                break;
                            } catch (RuntimeException e26) {
                                return null;
                            }
                        }
                        break;
                    case PduHeaders.PREVIOUSLY_SENT_BY /*160*/:
                        parseValueLength(pduDataStream);
                        try {
                            parseIntegerValue(pduDataStream);
                            value = parseEncodedStringValue(pduDataStream);
                            if (value != null) {
                                try {
                                    headers.setEncodedStringValue(value, PduHeaders.PREVIOUSLY_SENT_BY);
                                    break;
                                } catch (NullPointerException e27) {
                                    break;
                                } catch (RuntimeException e28) {
                                    return null;
                                }
                            }
                        } catch (RuntimeException e29) {
                            return null;
                        }
                        break;
                    case PduHeaders.PREVIOUSLY_SENT_DATE /*161*/:
                        parseValueLength(pduDataStream);
                        try {
                            parseIntegerValue(pduDataStream);
                            try {
                                headers.setLongInteger(parseLongInteger(pduDataStream), PduHeaders.PREVIOUSLY_SENT_DATE);
                                break;
                            } catch (RuntimeException e30) {
                                return null;
                            }
                        } catch (RuntimeException e31) {
                            return null;
                        }
                    case PduHeaders.MM_FLAGS /*164*/:
                        parseValueLength(pduDataStream);
                        extractByteValue(pduDataStream);
                        parseEncodedStringValue(pduDataStream);
                        break;
                    case PduHeaders.MBOX_TOTALS /*170*/:
                    case PduHeaders.MBOX_QUOTAS /*172*/:
                        parseValueLength(pduDataStream);
                        extractByteValue(pduDataStream);
                        try {
                            parseIntegerValue(pduDataStream);
                            break;
                        } catch (RuntimeException e32) {
                            return null;
                        }
                    case PduHeaders.MESSAGE_COUNT /*173*/:
                    case PduHeaders.START /*175*/:
                    case PduHeaders.LIMIT /*179*/:
                        try {
                            headers.setLongInteger(parseIntegerValue(pduDataStream), headerField);
                            break;
                        } catch (RuntimeException e33) {
                            return null;
                        }
                    case PduHeaders.ELEMENT_DESCRIPTOR /*178*/:
                        parseContentType(pduDataStream, null);
                        break;
                }
            }
            pduDataStream.reset();
            parseWapString(pduDataStream, 0);
        }
        return headers;
    }

    private static int parseUnsignedInt(ByteArrayInputStream pduDataStream) {
        int result = 0;
        int temp = pduDataStream.read();
        if (temp == -1) {
            return temp;
        }
        while ((temp & 128) != 0) {
            result = (result << 7) | (temp & 127);
            temp = pduDataStream.read();
            if (temp == -1) {
                return temp;
            }
        }
        return (result << 7) | (temp & 127);
    }

    private static int parseValueLength(ByteArrayInputStream pduDataStream) {
        int first = pduDataStream.read() & 255;
        if (first <= 30) {
            return first;
        }
        if (first == 31) {
            return parseUnsignedInt(pduDataStream);
        }
        throw new RuntimeException("Value length > LENGTH_QUOTE!");
    }

    private static EncodedStringValue parseEncodedStringValue(ByteArrayInputStream pduDataStream) {
        pduDataStream.mark(1);
        int charset = 0;
        int first = pduDataStream.read() & 255;
        if (first == 0) {
            return new EncodedStringValue("");
        }
        EncodedStringValue returnValue;
        pduDataStream.reset();
        if (first < 32) {
            parseValueLength(pduDataStream);
            charset = parseShortInteger(pduDataStream);
        }
        byte[] textString = parseWapString(pduDataStream, null);
        if (charset != 0) {
            try {
                returnValue = new EncodedStringValue(charset, textString);
            } catch (Exception e) {
                return null;
            }
        }
        returnValue = new EncodedStringValue(textString);
        return returnValue;
    }

    private static byte[] parseWapString(ByteArrayInputStream pduDataStream, int stringType) {
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        if (1 == stringType && 34 == temp) {
            pduDataStream.mark(1);
        } else if (stringType == 0 && 127 == temp) {
            pduDataStream.mark(1);
        } else {
            pduDataStream.reset();
        }
        return getWapString(pduDataStream, stringType);
    }

    /* JADX WARNING: Missing block: B:20:0x002a, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isTokenCharacter(int ch) {
        if (!(ch < 33 || ch > ConnectivityLogManager.WIFI_WIFIPRO_DUALBAND_AP_INFO_EVENT || ch == 34 || ch == 44 || ch == 47 || ch == 123 || ch == ConnectivityLogManager.WIFI_WIFIPRO_DUALBAND_EXCEPTION_EVENT)) {
            switch (ch) {
                case 40:
                case 41:
                    break;
                default:
                    switch (ch) {
                        case 58:
                        case 59:
                        case 60:
                        case 61:
                        case 62:
                        case 63:
                        case 64:
                            break;
                        default:
                            switch (ch) {
                                case 91:
                                case 92:
                                case 93:
                                    break;
                                default:
                                    return true;
                            }
                    }
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:8:0x0011, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isText(int ch) {
        if ((ch < 32 || ch > ConnectivityLogManager.WIFI_WIFIPRO_DUALBAND_AP_INFO_EVENT) && ((ch < 128 || ch > 255) && ch != 13)) {
            switch (ch) {
                case 9:
                case 10:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private static byte[] getWapString(ByteArrayInputStream pduDataStream, int stringType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int temp = pduDataStream.read();
        while (-1 != temp && temp != 0) {
            if (stringType == 2) {
                if (isTokenCharacter(temp)) {
                    out.write(temp);
                }
            } else if (isText(temp)) {
                out.write(temp);
            }
            temp = pduDataStream.read();
        }
        if (out.size() > 0) {
            return out.toByteArray();
        }
        return null;
    }

    private static int extractByteValue(ByteArrayInputStream pduDataStream) {
        return pduDataStream.read() & 255;
    }

    private static int parseShortInteger(ByteArrayInputStream pduDataStream) {
        return pduDataStream.read() & 127;
    }

    private static long parseLongInteger(ByteArrayInputStream pduDataStream) {
        int count = pduDataStream.read() & 255;
        if (count <= 8) {
            long result = 0;
            for (int i = 0; i < count; i++) {
                result = (result << 8) + ((long) (pduDataStream.read() & 255));
            }
            return result;
        }
        throw new RuntimeException("Octet count greater than 8 and I can't represent that!");
    }

    private static long parseIntegerValue(ByteArrayInputStream pduDataStream) {
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        pduDataStream.reset();
        if (temp > 127) {
            return (long) parseShortInteger(pduDataStream);
        }
        return parseLongInteger(pduDataStream);
    }

    private static int skipWapValue(ByteArrayInputStream pduDataStream, int length) {
        int readLen = pduDataStream.read(new byte[length], 0, length);
        if (readLen < length) {
            return -1;
        }
        return readLen;
    }

    private static void parseContentTypeParams(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map, Integer length) {
        int startPos = pduDataStream.available();
        int lastLen = length.intValue();
        while (lastLen > 0) {
            int index;
            int param = pduDataStream.read();
            lastLen--;
            int lastLen2;
            if (param != 129) {
                byte[] name;
                if (param != 131) {
                    if (param == 133 || param == 151) {
                        name = parseWapString(pduDataStream, 0);
                        if (!(name == null || map == null)) {
                            map.put(Integer.valueOf(151), name);
                        }
                        lastLen2 = length.intValue() - (startPos - pduDataStream.available());
                    } else {
                        if (param != 153) {
                            switch (param) {
                                case 137:
                                    break;
                                case 138:
                                    break;
                                default:
                                    if (-1 != skipWapValue(pduDataStream, lastLen)) {
                                        lastLen = 0;
                                        break;
                                    }
                                    break;
                            }
                        }
                        name = parseWapString(pduDataStream, 0);
                        if (!(name == null || map == null)) {
                            map.put(Integer.valueOf(153), name);
                        }
                        lastLen2 = length.intValue() - (startPos - pduDataStream.available());
                    }
                    lastLen = lastLen2;
                }
                pduDataStream.mark(1);
                lastLen2 = extractByteValue(pduDataStream);
                pduDataStream.reset();
                if (lastLen2 > 127) {
                    index = parseShortInteger(pduDataStream);
                    if (index < PduContentTypes.contentTypes.length) {
                        byte[] type = PduContentTypes.contentTypes[index].getBytes(Charset.defaultCharset());
                        if (map != null) {
                            map.put(Integer.valueOf(131), type);
                        }
                    }
                } else {
                    name = parseWapString(pduDataStream, 0);
                    if (!(name == null || map == null)) {
                        map.put(Integer.valueOf(131), name);
                    }
                }
                index = length.intValue() - (startPos - pduDataStream.available());
            } else {
                pduDataStream.mark(1);
                lastLen2 = extractByteValue(pduDataStream);
                pduDataStream.reset();
                if ((lastLen2 <= 32 || lastLen2 >= 127) && lastLen2 != 0) {
                    index = (int) parseIntegerValue(pduDataStream);
                    if (map != null) {
                        map.put(Integer.valueOf(129), Integer.valueOf(index));
                    }
                } else {
                    try {
                        int charsetInt = CharacterSets.getMibEnumValue(new String(parseWapString(pduDataStream, 0), Charset.defaultCharset()));
                        if (map != null) {
                            map.put(Integer.valueOf(129), Integer.valueOf(charsetInt));
                        }
                    } catch (UnsupportedEncodingException e) {
                        if (map != null) {
                            map.put(Integer.valueOf(129), Integer.valueOf(0));
                        }
                    }
                }
                index = length.intValue() - (startPos - pduDataStream.available());
            }
            lastLen = index;
        }
    }

    private static byte[] parseContentType(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map) {
        byte[] contentType;
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        pduDataStream.reset();
        int cur = temp & 255;
        if (cur < 32) {
            int length = parseValueLength(pduDataStream);
            int startPos = pduDataStream.available();
            pduDataStream.mark(1);
            temp = pduDataStream.read();
            pduDataStream.reset();
            int first = temp & 255;
            if (first >= 32 && first <= 127) {
                contentType = parseWapString(pduDataStream, 0);
            } else if (first <= 127) {
                return PduContentTypes.contentTypes[0].getBytes(Charset.defaultCharset());
            } else {
                int index = parseShortInteger(pduDataStream);
                if (index < PduContentTypes.contentTypes.length) {
                    contentType = PduContentTypes.contentTypes[index].getBytes(Charset.defaultCharset());
                } else {
                    pduDataStream.reset();
                    contentType = parseWapString(pduDataStream, 0);
                }
            }
            int parameterLen = length - (startPos - pduDataStream.available());
            if (parameterLen > 0) {
                parseContentTypeParams(pduDataStream, map, Integer.valueOf(parameterLen));
            }
            if (parameterLen < 0) {
                return PduContentTypes.contentTypes[0].getBytes(Charset.defaultCharset());
            }
        } else if (cur <= 127) {
            contentType = parseWapString(pduDataStream, 0);
        } else {
            contentType = PduContentTypes.contentTypes[parseShortInteger(pduDataStream)].getBytes(Charset.defaultCharset());
        }
        return contentType;
    }
}
