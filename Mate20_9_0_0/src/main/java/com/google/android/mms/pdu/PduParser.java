package com.google.android.mms.pdu;

import android.os.Bundle;
import android.util.Log;
import com.android.internal.telephony.HwTelephonyChrManager.Scenario;
import com.android.internal.telephony.HwTelephonyFactory;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

public class PduParser {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final boolean DEBUG = false;
    private static final int END_STRING_FLAG = 0;
    private static final int LENGTH_QUOTE = 31;
    private static final boolean LOCAL_LOGV = false;
    private static final String LOG_TAG = "PduParser";
    private static final int LONG_INTEGER_LENGTH_MAX = 8;
    private static final byte PDU_BODY_NULL = (byte) 2;
    private static final byte PDU_CHECK_MANDATORY_HEADER = (byte) 4;
    private static final byte PDU_CONTENT_TYPE_NULL = (byte) 3;
    private static final byte PDU_DATA_STREAM_NULL = (byte) 0;
    private static final byte PDU_HEADER_NULL = (byte) 1;
    private static final int QUOTE = 127;
    private static final int QUOTED_STRING_FLAG = 34;
    private static final int SHORT_INTEGER_MAX = 127;
    private static final int SHORT_LENGTH_MAX = 30;
    private static final int TEXT_MAX = 127;
    private static final int TEXT_MIN = 32;
    private static final int THE_FIRST_PART = 0;
    private static final int THE_LAST_PART = 1;
    private static final int TYPE_QUOTED_STRING = 1;
    private static final int TYPE_TEXT_STRING = 0;
    private static final int TYPE_TOKEN_STRING = 2;
    private static byte[] mStartParam = null;
    private static byte[] mTypeParam = null;
    private PduBody mBody = null;
    private PduHeaders mHeaders = null;
    private final boolean mParseContentDisposition;
    private ByteArrayInputStream mPduDataStream = null;

    public PduParser(byte[] pduDataStream, boolean parseContentDisposition) {
        this.mPduDataStream = new ByteArrayInputStream(pduDataStream);
        this.mParseContentDisposition = parseContentDisposition;
    }

    public GenericPdu parse() {
        if (this.mPduDataStream == null) {
            Log.v(LOG_TAG, "mPduDataStream is null");
            reportChrSmsEvent(PDU_DATA_STREAM_NULL);
            return null;
        }
        this.mHeaders = parseHeaders(this.mPduDataStream);
        if (this.mHeaders == null) {
            Log.v(LOG_TAG, "mHeaders is null");
            reportChrSmsEvent(PDU_HEADER_NULL);
            return null;
        }
        int messageType = this.mHeaders.getOctet(140);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("messageType ");
        stringBuilder.append(messageType);
        Log.v(str, stringBuilder.toString());
        if (checkMandatoryHeader(this.mHeaders)) {
            boolean readreportasMessage = false;
            if (128 == messageType || 132 == messageType) {
                byte[] messageClass = this.mHeaders.getTextString(138);
                byte[] contentType = this.mHeaders.getTextString(132);
                String contentTypeStr = null;
                if (contentType != null) {
                    contentTypeStr = new String(contentType);
                }
                if (messageType == 132 && messageClass != null && Arrays.equals(messageClass, PduHeaders.MESSAGE_CLASS_AUTO_STR.getBytes(Charset.defaultCharset())) && contentTypeStr != null && contentTypeStr.equals(ContentType.TEXT_PLAIN)) {
                    this.mBody = parseReadReport(this.mPduDataStream);
                    readreportasMessage = true;
                } else {
                    this.mBody = parseParts(this.mPduDataStream);
                }
                if (this.mBody == null) {
                    Log.v(LOG_TAG, "mBody is null");
                    reportChrSmsEvent(PDU_BODY_NULL);
                    return null;
                }
            }
            switch (messageType) {
                case 128:
                    return new SendReq(this.mHeaders, this.mBody);
                case 129:
                    return new SendConf(this.mHeaders);
                case 130:
                    return new NotificationInd(this.mHeaders);
                case 131:
                    return new NotifyRespInd(this.mHeaders);
                case 132:
                    RetrieveConf retrieveConf = new RetrieveConf(this.mHeaders, this.mBody);
                    byte[] contentType2 = retrieveConf.getContentType();
                    if (contentType2 == null) {
                        Log.v(LOG_TAG, "contentType is null");
                        reportChrSmsEvent(PDU_CONTENT_TYPE_NULL);
                        return null;
                    }
                    String ctTypeStr = new String(contentType2);
                    if (ctTypeStr.equals(ContentType.MULTIPART_MIXED) || ctTypeStr.equals(ContentType.MULTIPART_RELATED) || ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                        return retrieveConf;
                    }
                    if (ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                        PduPart firstPart = this.mBody.getPart(0);
                        this.mBody.removeAll();
                        this.mBody.addPart(0, firstPart);
                        return retrieveConf;
                    } else if (readreportasMessage) {
                        return retrieveConf;
                    } else {
                        return null;
                    }
                case 133:
                    return new AcknowledgeInd(this.mHeaders);
                case 134:
                    return new DeliveryInd(this.mHeaders);
                case 135:
                    return new ReadRecInd(this.mHeaders);
                case 136:
                    return new ReadOrigInd(this.mHeaders);
                default:
                    log("Parser doesn't support this message type in this version!");
                    return null;
            }
        }
        Log.v(LOG_TAG, "check mandatory headers failed!");
        reportChrSmsEvent(PDU_CHECK_MANDATORY_HEADER);
        return null;
    }

    protected PduHeaders parseHeaders(ByteArrayInputStream pduDataStream) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3;
        if (pduDataStream == null) {
            return null;
        }
        boolean keepParsing = true;
        PduHeaders headers = new PduHeaders();
        while (keepParsing && pduDataStream.available() > 0) {
            pduDataStream.mark(1);
            int headerField = extractByteValue(pduDataStream);
            byte[] value;
            if (headerField < 32 || headerField > 127) {
                EncodedStringValue value2;
                byte[] address;
                int value3;
                switch (headerField) {
                    case 129:
                    case 130:
                    case 151:
                        value2 = parseEncodedStringValue(pduDataStream);
                        if (value2 != null) {
                            address = value2.getTextString();
                            if (address != null) {
                                String str = new String(address);
                                int endIndex = str.indexOf("/");
                                if (endIndex > 0) {
                                    str = str.substring(0, endIndex);
                                }
                                try {
                                    value2.setTextString(str.getBytes());
                                } catch (NullPointerException e) {
                                    log("null pointer error!");
                                    return null;
                                }
                            }
                            try {
                                headers.appendEncodedStringValue(value2, headerField);
                                break;
                            } catch (NullPointerException e2) {
                                log("null pointer error!");
                                break;
                            } catch (RuntimeException e3) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(headerField);
                                stringBuilder.append("is not Encoded-String-Value header field!");
                                log(stringBuilder.toString());
                                return null;
                            }
                        }
                        break;
                    case 131:
                    case 139:
                    case 152:
                    case PduHeaders.REPLY_CHARGING_ID /*158*/:
                    case PduHeaders.APPLIC_ID /*183*/:
                    case PduHeaders.REPLY_APPLIC_ID /*184*/:
                    case PduHeaders.AUX_APPLIC_ID /*185*/:
                    case PduHeaders.REPLACE_ID /*189*/:
                    case PduHeaders.CANCEL_ID /*190*/:
                        value = parseWapString(pduDataStream, 0);
                        if (value != null) {
                            try {
                                headers.setTextString(value, headerField);
                                break;
                            } catch (NullPointerException e4) {
                                log("null pointer error!");
                                break;
                            } catch (RuntimeException e5) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(headerField);
                                stringBuilder2.append("is not Text-String header field!");
                                log(stringBuilder2.toString());
                                return null;
                            }
                        }
                        break;
                    case 132:
                        HashMap<Integer, Object> map = new HashMap();
                        byte[] contentType = parseContentType(pduDataStream, map);
                        if (contentType != null) {
                            try {
                                headers.setTextString(contentType, 132);
                            } catch (NullPointerException e6) {
                                log("null pointer error!");
                            } catch (RuntimeException e7) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(headerField);
                                stringBuilder.append("is not Text-String header field!");
                                log(stringBuilder.toString());
                                return null;
                            }
                        }
                        mStartParam = (byte[]) map.get(Integer.valueOf(153));
                        mTypeParam = (byte[]) map.get(Integer.valueOf(131));
                        keepParsing = false;
                        break;
                    case 133:
                    case 142:
                    case PduHeaders.REPLY_CHARGING_SIZE /*159*/:
                        try {
                            headers.setLongInteger(parseLongInteger(pduDataStream), headerField);
                            break;
                        } catch (RuntimeException e8) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(headerField);
                            stringBuilder3.append("is not Long-Integer header field!");
                            log(stringBuilder3.toString());
                            return null;
                        }
                    case 134:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 148:
                    case 149:
                    case 153:
                    case 155:
                    case 156:
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
                        value3 = extractByteValue(pduDataStream);
                        try {
                            headers.setOctet(value3, headerField);
                            break;
                        } catch (InvalidHeaderValueException e9) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Set invalid Octet value: ");
                            stringBuilder2.append(value3);
                            stringBuilder2.append(" into the header filed: ");
                            stringBuilder2.append(headerField);
                            log(stringBuilder2.toString());
                            return null;
                        } catch (RuntimeException e10) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(headerField);
                            stringBuilder2.append("is not Octet header field!");
                            log(stringBuilder2.toString());
                            return null;
                        }
                    case 135:
                    case 136:
                    case 157:
                        parseValueLength(pduDataStream);
                        value3 = extractByteValue(pduDataStream);
                        try {
                            long timeValue = parseLongInteger(pduDataStream);
                            if (129 == value3) {
                                timeValue += System.currentTimeMillis() / 1000;
                            }
                            try {
                                headers.setLongInteger(timeValue, headerField);
                                break;
                            } catch (RuntimeException e11) {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append(headerField);
                                stringBuilder4.append("is not Long-Integer header field!");
                                log(stringBuilder4.toString());
                                return null;
                            }
                        } catch (RuntimeException e12) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(headerField);
                            stringBuilder2.append("is not Long-Integer header field!");
                            log(stringBuilder2.toString());
                            return null;
                        }
                    case 137:
                        value2 = null;
                        try {
                            parseValueLength(pduDataStream);
                        } catch (RuntimeException e13) {
                            e13.printStackTrace();
                        }
                        if (128 == extractByteValue(pduDataStream)) {
                            value2 = parseEncodedStringValue(pduDataStream);
                            if (value2 != null) {
                                byte[] address2 = value2.getTextString();
                                if (address2 != null) {
                                    String str2 = new String(address2);
                                    int endIndex2 = str2.indexOf("/");
                                    if (endIndex2 > 0) {
                                        str2 = str2.substring(0, endIndex2);
                                    }
                                    try {
                                        value2.setTextString(str2.getBytes());
                                    } catch (NullPointerException e14) {
                                        log("null pointer error!");
                                        return null;
                                    }
                                }
                            }
                        }
                        try {
                            value2 = new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes());
                        } catch (NullPointerException e15) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(headerField);
                            stringBuilder.append("is not Encoded-String-Value header field!");
                            log(stringBuilder.toString());
                            return null;
                        }
                        try {
                            headers.setEncodedStringValue(value2, 137);
                            break;
                        } catch (NullPointerException e16) {
                            log("null pointer error!");
                            break;
                        } catch (RuntimeException e17) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(headerField);
                            stringBuilder.append("is not Encoded-String-Value header field!");
                            log(stringBuilder.toString());
                            return null;
                        }
                    case 138:
                        pduDataStream.mark(1);
                        value3 = extractByteValue(pduDataStream);
                        if (value3 >= 128) {
                            if (128 != value3) {
                                if (129 != value3) {
                                    if (130 != value3) {
                                        if (131 == value3) {
                                            headers.setTextString(PduHeaders.MESSAGE_CLASS_AUTO_STR.getBytes(), 138);
                                            break;
                                        }
                                    }
                                    headers.setTextString(PduHeaders.MESSAGE_CLASS_INFORMATIONAL_STR.getBytes(), 138);
                                    break;
                                }
                                headers.setTextString(PduHeaders.MESSAGE_CLASS_ADVERTISEMENT_STR.getBytes(), 138);
                                break;
                            }
                            try {
                                headers.setTextString(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes(), 138);
                                break;
                            } catch (NullPointerException e18) {
                                log("null pointer error!");
                                break;
                            } catch (RuntimeException e19) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(headerField);
                                stringBuilder2.append("is not Text-String header field!");
                                log(stringBuilder2.toString());
                                return null;
                            }
                        }
                        pduDataStream.reset();
                        address = parseWapString(pduDataStream, 0);
                        if (address != null) {
                            try {
                                headers.setTextString(address, 138);
                                break;
                            } catch (NullPointerException e20) {
                                log("null pointer error!");
                                break;
                            } catch (RuntimeException e21) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(headerField);
                                stringBuilder.append("is not Text-String header field!");
                                log(stringBuilder.toString());
                                return null;
                            }
                        }
                        break;
                    case 140:
                        value3 = extractByteValue(pduDataStream);
                        switch (value3) {
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
                                    headers.setOctet(value3, headerField);
                                    break;
                                } catch (InvalidHeaderValueException e22) {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Set invalid Octet value: ");
                                    stringBuilder2.append(value3);
                                    stringBuilder2.append(" into the header filed: ");
                                    stringBuilder2.append(headerField);
                                    log(stringBuilder2.toString());
                                    return null;
                                } catch (RuntimeException e23) {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(headerField);
                                    stringBuilder2.append("is not Octet header field!");
                                    log(stringBuilder2.toString());
                                    return null;
                                }
                        }
                    case 141:
                        value3 = parseShortInteger(pduDataStream);
                        try {
                            headers.setOctet(value3, 141);
                            break;
                        } catch (InvalidHeaderValueException e24) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Set invalid Octet value: ");
                            stringBuilder2.append(value3);
                            stringBuilder2.append(" into the header filed: ");
                            stringBuilder2.append(headerField);
                            log(stringBuilder2.toString());
                            return null;
                        } catch (RuntimeException e25) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(headerField);
                            stringBuilder2.append("is not Octet header field!");
                            log(stringBuilder2.toString());
                            return null;
                        }
                    case 147:
                    case 150:
                    case 154:
                    case PduHeaders.STORE_STATUS_TEXT /*166*/:
                    case PduHeaders.RECOMMENDED_RETRIEVAL_MODE_TEXT /*181*/:
                    case PduHeaders.STATUS_TEXT /*182*/:
                        if (150 == headerField) {
                            pduDataStream.mark(1);
                            if ((pduDataStream.read() & 255) != 0) {
                                pduDataStream.reset();
                            }
                        }
                        value2 = parseEncodedStringValue(pduDataStream);
                        if (value2 != null) {
                            try {
                                headers.setEncodedStringValue(value2, headerField);
                                break;
                            } catch (NullPointerException e26) {
                                log("null pointer error!");
                                break;
                            } catch (RuntimeException e27) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(headerField);
                                stringBuilder2.append("is not Encoded-String-Value header field!");
                                log(stringBuilder2.toString());
                                return null;
                            }
                        }
                        break;
                    case 160:
                        parseValueLength(pduDataStream);
                        try {
                            parseIntegerValue(pduDataStream);
                            value2 = parseEncodedStringValue(pduDataStream);
                            if (value2 != null) {
                                try {
                                    headers.setEncodedStringValue(value2, 160);
                                    break;
                                } catch (NullPointerException e28) {
                                    log("null pointer error!");
                                    break;
                                } catch (RuntimeException e29) {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(headerField);
                                    stringBuilder2.append("is not Encoded-String-Value header field!");
                                    log(stringBuilder2.toString());
                                    return null;
                                }
                            }
                        } catch (RuntimeException e30) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(headerField);
                            stringBuilder3.append(" is not Integer-Value");
                            log(stringBuilder3.toString());
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
                            } catch (RuntimeException e31) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(headerField);
                                stringBuilder3.append("is not Long-Integer header field!");
                                log(stringBuilder3.toString());
                                return null;
                            }
                        } catch (RuntimeException e32) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(headerField);
                            stringBuilder3.append(" is not Integer-Value");
                            log(stringBuilder3.toString());
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
                        } catch (RuntimeException e33) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(headerField);
                            stringBuilder3.append(" is not Integer-Value");
                            log(stringBuilder3.toString());
                            return null;
                        }
                    case PduHeaders.MESSAGE_COUNT /*173*/:
                    case PduHeaders.START /*175*/:
                    case PduHeaders.LIMIT /*179*/:
                        try {
                            headers.setLongInteger(parseIntegerValue(pduDataStream), headerField);
                            break;
                        } catch (RuntimeException e34) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(headerField);
                            stringBuilder3.append("is not Long-Integer header field!");
                            log(stringBuilder3.toString());
                            return null;
                        }
                    case PduHeaders.ELEMENT_DESCRIPTOR /*178*/:
                        parseContentType(pduDataStream, null);
                        break;
                    default:
                        log("Unknown header");
                        break;
                }
            }
            pduDataStream.reset();
            value = parseWapString(pduDataStream, 0);
        }
        return headers;
    }

    protected static PduBody parseReadReport(ByteArrayInputStream pduDataStream) {
        PduBody body = new PduBody();
        PduPart part = new PduPart();
        part.setContentType(PduContentTypes.contentTypes[3].getBytes(Charset.defaultCharset()));
        part.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes(Charset.defaultCharset()));
        int dataLength = pduDataStream.available();
        if (dataLength > 0) {
            byte[] partData = new byte[dataLength];
            pduDataStream.read(partData, 0, dataLength);
            part.setData(partData);
        }
        body.addPart(part);
        return body;
    }

    protected PduBody parseParts(ByteArrayInputStream pduDataStream) {
        PduParser pduParser = this;
        ByteArrayInputStream byteArrayInputStream = pduDataStream;
        PduBody pduBody = null;
        if (byteArrayInputStream == null) {
            return null;
        }
        int count = parseUnsignedInt(pduDataStream);
        PduBody body = new PduBody();
        int i = 0;
        int i2 = 0;
        while (i2 < count) {
            int headerLength = parseUnsignedInt(pduDataStream);
            int dataLength = parseUnsignedInt(pduDataStream);
            PduPart part = new PduPart();
            int startPos = pduDataStream.available();
            if (startPos <= 0) {
                return pduBody;
            }
            int count2;
            PduBody pduBody2;
            int i3;
            HashMap<Integer, Object> map = new HashMap();
            byte[] contentType = parseContentType(byteArrayInputStream, map);
            if (contentType != null) {
                part.setContentType(contentType);
            } else {
                part.setContentType(PduContentTypes.contentTypes[i].getBytes());
            }
            byte[] name = (byte[]) map.get(Integer.valueOf(151));
            if (name != null) {
                part.setName(name);
            }
            Integer charset = (Integer) map.get(Integer.valueOf(129));
            if (charset != null) {
                part.setCharset(charset.intValue());
            }
            i = headerLength - (startPos - pduDataStream.available());
            if (i > 0) {
                if (!pduParser.parsePartHeaders(byteArrayInputStream, part, i)) {
                    return pduBody;
                }
            } else if (i < 0) {
                return pduBody;
            }
            if (part.getContentLocation() == null && part.getName() == null && part.getFilename() == null && part.getContentId() == null) {
                count2 = count;
                part.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes());
            } else {
                count2 = count;
            }
            if (dataLength > 0) {
                byte[] partData = new byte[dataLength];
                count = new String(part.getContentType());
                byteArrayInputStream.read(partData, 0, dataLength);
                if (count.equalsIgnoreCase(ContentType.MULTIPART_ALTERNATIVE) != 0) {
                    part = pduParser.parseParts(new ByteArrayInputStream(partData)).getPart(0);
                    pduBody2 = null;
                } else {
                    byte[] partDataEncoding = part.getContentTransferEncoding();
                    if (partDataEncoding != null) {
                        i = new String(partDataEncoding);
                        if (i.equalsIgnoreCase(PduPart.P_BASE64)) {
                            partData = Base64.decodeBase64(partData);
                        } else if (i.equalsIgnoreCase(PduPart.P_QUOTED_PRINTABLE)) {
                            partData = QuotedPrintable.decodeQuotedPrintable(partData);
                        }
                    }
                    if (partData == null) {
                        log("Decode part data error!");
                        return null;
                    }
                    pduBody2 = null;
                    part.setData(partData);
                }
            } else {
                pduBody2 = null;
            }
            if (checkPartPosition(part) == 0) {
                i3 = 0;
                body.addPart(0, part);
            } else {
                i3 = 0;
                body.addPart(part);
            }
            i2++;
            i = i3;
            count = count2;
            pduBody = pduBody2;
            pduParser = this;
        }
        return body;
    }

    private static void log(String text) {
    }

    protected static int parseUnsignedInt(ByteArrayInputStream pduDataStream) {
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

    protected static int parseValueLength(ByteArrayInputStream pduDataStream) {
        int first = pduDataStream.read() & 255;
        if (first <= 30) {
            return first;
        }
        if (first == 31) {
            return parseUnsignedInt(pduDataStream);
        }
        throw new RuntimeException("Value length > LENGTH_QUOTE!");
    }

    protected static EncodedStringValue parseEncodedStringValue(ByteArrayInputStream pduDataStream) {
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

    protected static byte[] parseWapString(ByteArrayInputStream pduDataStream, int stringType) {
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
    protected static boolean isTokenCharacter(int ch) {
        if (!(ch < 33 || ch > 126 || ch == 34 || ch == 44 || ch == 47 || ch == 123 || ch == 125)) {
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
    protected static boolean isText(int ch) {
        if ((ch < 32 || ch > 126) && ((ch < 128 || ch > 255) && ch != 13)) {
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

    protected static byte[] getWapString(ByteArrayInputStream pduDataStream, int stringType) {
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

    protected static int extractByteValue(ByteArrayInputStream pduDataStream) {
        return pduDataStream.read() & 255;
    }

    protected static int parseShortInteger(ByteArrayInputStream pduDataStream) {
        return pduDataStream.read() & 127;
    }

    protected static long parseLongInteger(ByteArrayInputStream pduDataStream) {
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

    protected static long parseIntegerValue(ByteArrayInputStream pduDataStream) {
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        pduDataStream.reset();
        if (temp > 127) {
            return (long) parseShortInteger(pduDataStream);
        }
        return parseLongInteger(pduDataStream);
    }

    protected static int skipWapValue(ByteArrayInputStream pduDataStream, int length) {
        int readLen = pduDataStream.read(new byte[length], 0, length);
        if (readLen < length) {
            return -1;
        }
        return readLen;
    }

    protected static void parseContentTypeParams(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map, Integer length) {
        int startPos = pduDataStream.available();
        int lastLen = length.intValue();
        while (lastLen > 0) {
            int index;
            int param = pduDataStream.read();
            lastLen--;
            byte[] name;
            int lastLen2;
            if (param != 129) {
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
                                    } else {
                                        Log.e(LOG_TAG, "Corrupt Content-Type");
                                        break;
                                    }
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
                        map.put(Integer.valueOf(131), PduContentTypes.contentTypes[index].getBytes());
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
                    name = parseWapString(pduDataStream, 0);
                    try {
                        map.put(Integer.valueOf(129), Integer.valueOf(CharacterSets.getMibEnumValue(new String(name))));
                    } catch (UnsupportedEncodingException e) {
                        Log.e(LOG_TAG, Arrays.toString(name), e);
                        map.put(Integer.valueOf(129), Integer.valueOf(0));
                    }
                }
                index = length.intValue() - (startPos - pduDataStream.available());
            }
            lastLen = index;
        }
        if (lastLen != 0) {
            Log.e(LOG_TAG, "Corrupt Content-Type");
        }
    }

    protected static byte[] parseContentType(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map) {
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
            } else if (first > 127) {
                int index = parseShortInteger(pduDataStream);
                if (index < PduContentTypes.contentTypes.length) {
                    contentType = PduContentTypes.contentTypes[index].getBytes();
                } else {
                    pduDataStream.reset();
                    contentType = parseWapString(pduDataStream, 0);
                }
            } else {
                Log.e(LOG_TAG, "Corrupt content-type");
                return PduContentTypes.contentTypes[0].getBytes();
            }
            int parameterLen = length - (startPos - pduDataStream.available());
            if (parameterLen > 0) {
                parseContentTypeParams(pduDataStream, map, Integer.valueOf(parameterLen));
            }
            if (parameterLen < 0) {
                Log.e(LOG_TAG, "Corrupt MMS message");
                return PduContentTypes.contentTypes[0].getBytes();
            }
        } else if (cur <= 127) {
            contentType = parseWapString(pduDataStream, 0);
        } else {
            contentType = PduContentTypes.contentTypes[parseShortInteger(pduDataStream)].getBytes();
        }
        return contentType;
    }

    protected boolean parsePartHeaders(ByteArrayInputStream pduDataStream, PduPart part, int length) {
        int startPos = pduDataStream.available();
        int tempPos = 0;
        int lastLen = length;
        while (lastLen > 0) {
            int header = pduDataStream.read();
            lastLen--;
            if (header > 127) {
                byte[] contentId;
                if (header != 142) {
                    if (header != 174) {
                        if (header == 192) {
                            contentId = parseWapString(pduDataStream, 1);
                            if (contentId != null) {
                                part.setContentId(contentId);
                            }
                            lastLen = length - (startPos - pduDataStream.available());
                        } else if (header != PduPart.P_CONTENT_DISPOSITION) {
                            if (-1 == skipWapValue(pduDataStream, lastLen)) {
                                Log.e(LOG_TAG, "Corrupt Part headers");
                                return false;
                            }
                            lastLen = 0;
                        }
                    }
                    if (this.mParseContentDisposition) {
                        int len = parseValueLength(pduDataStream);
                        pduDataStream.mark(1);
                        int thisStartPos = pduDataStream.available();
                        int value = pduDataStream.read();
                        if (value == 128) {
                            part.setContentDisposition(PduPart.DISPOSITION_FROM_DATA);
                        } else if (value == 129) {
                            part.setContentDisposition(PduPart.DISPOSITION_ATTACHMENT);
                        } else if (value == 130) {
                            part.setContentDisposition(PduPart.DISPOSITION_INLINE);
                        } else {
                            pduDataStream.reset();
                            part.setContentDisposition(parseWapString(pduDataStream, 0));
                        }
                        if (thisStartPos - pduDataStream.available() < len) {
                            if (pduDataStream.read() == 152) {
                                part.setFilename(parseWapString(pduDataStream, 0));
                            }
                            int thisEndPos = pduDataStream.available();
                            if (thisStartPos - thisEndPos < len) {
                                int last = len - (thisStartPos - thisEndPos);
                                pduDataStream.read(new byte[last], 0, last);
                            }
                        }
                        lastLen = length - (startPos - pduDataStream.available());
                    }
                } else {
                    contentId = parseWapString(pduDataStream, 0);
                    if (contentId != null) {
                        part.setContentLocation(contentId);
                    }
                    lastLen = length - (startPos - pduDataStream.available());
                }
            } else if (header >= 32 && header <= 127) {
                byte[] tempHeader = parseWapString(pduDataStream, 0);
                byte[] tempValue = parseWapString(pduDataStream, 0);
                if (true == PduPart.CONTENT_TRANSFER_ENCODING.equalsIgnoreCase(new String(tempHeader))) {
                    part.setContentTransferEncoding(tempValue);
                }
                lastLen = length - (startPos - pduDataStream.available());
            } else if (-1 == skipWapValue(pduDataStream, lastLen)) {
                Log.e(LOG_TAG, "Corrupt Part headers");
                return false;
            } else {
                lastLen = 0;
            }
        }
        if (lastLen == 0) {
            return true;
        }
        Log.e(LOG_TAG, "Corrupt Part headers");
        return false;
    }

    private static int checkPartPosition(PduPart part) {
        if (mTypeParam == null && mStartParam == null) {
            return 1;
        }
        byte[] contentId;
        if (mStartParam != null) {
            contentId = part.getContentId();
            if (contentId == null || true != Arrays.equals(mStartParam, contentId)) {
                return 1;
            }
            return 0;
        }
        if (mTypeParam != null) {
            contentId = part.getContentType();
            if (contentId == null || true != Arrays.equals(mTypeParam, contentId)) {
                return 1;
            }
            return 0;
        }
        return 1;
    }

    protected static boolean checkMandatoryHeader(PduHeaders headers) {
        if (headers == null) {
            return false;
        }
        int messageType = headers.getOctet(140);
        if (headers.getOctet(141) == 0) {
            return false;
        }
        switch (messageType) {
            case 128:
                if (headers.getTextString(132) == null || headers.getEncodedStringValue(137) == null || headers.getTextString(152) == null) {
                    return false;
                }
            case 129:
                if (headers.getOctet(146) == 0 || headers.getTextString(152) == null) {
                    return false;
                }
            case 130:
                if (headers.getTextString(131) == null || -1 == headers.getLongInteger(136) || headers.getTextString(138) == null || -1 == headers.getLongInteger(142) || headers.getTextString(152) == null) {
                    return false;
                }
            case 131:
                if (headers.getOctet(149) == 0 || headers.getTextString(152) == null) {
                    return false;
                }
            case 132:
                if (headers.getTextString(132) == null || -1 == headers.getLongInteger(133)) {
                    return false;
                }
            case 133:
                if (headers.getTextString(152) == null) {
                    return false;
                }
                break;
            case 134:
                if (-1 == headers.getLongInteger(133) || headers.getTextString(139) == null || headers.getOctet(149) == 0 || headers.getEncodedStringValues(151) == null) {
                    return false;
                }
            case 135:
                if (headers.getEncodedStringValue(137) == null || headers.getTextString(139) == null || headers.getOctet(155) == 0 || headers.getEncodedStringValues(151) == null) {
                    return false;
                }
            case 136:
                if (-1 == headers.getLongInteger(133) || headers.getEncodedStringValue(137) == null || headers.getTextString(139) == null || headers.getOctet(155) == 0 || headers.getEncodedStringValues(151) == null) {
                    return false;
                }
            default:
                return false;
        }
        return true;
    }

    private void reportChrSmsEvent(byte failCause) {
        Log.v(LOG_TAG, "repor chr parse fail");
        Bundle data = new Bundle();
        data.putString("EventScenario", Scenario.SMS);
        data.putInt("EventFailCause", 2001);
        data.putByte("SMS.PDUPARSE.parseFailCause", failCause);
        HwTelephonyFactory.getHwTelephonyChrManager().sendTelephonyChrBroadcast(data);
    }
}
