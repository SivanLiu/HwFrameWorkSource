package com.google.android.mms.pdu;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

public class PduComposer {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int END_STRING_FLAG = 0;
    private static final int LENGTH_QUOTE = 31;
    private static final String LOG_TAG = "PduComposer";
    private static final int LONG_INTEGER_LENGTH_MAX = 8;
    private static final int PDU_COMPOSER_BLOCK_SIZE = 1024;
    private static final int PDU_COMPOSE_CONTENT_ERROR = 1;
    private static final int PDU_COMPOSE_FIELD_NOT_SET = 2;
    private static final int PDU_COMPOSE_FIELD_NOT_SUPPORTED = 3;
    private static final int PDU_COMPOSE_SUCCESS = 0;
    private static final int PDU_EMAIL_ADDRESS_TYPE = 2;
    private static final int PDU_IPV4_ADDRESS_TYPE = 3;
    private static final int PDU_IPV6_ADDRESS_TYPE = 4;
    private static final int PDU_PHONE_NUMBER_ADDRESS_TYPE = 1;
    private static final int PDU_UNKNOWN_ADDRESS_TYPE = 5;
    private static final int QUOTED_STRING_FLAG = 34;
    static final String REGEXP_EMAIL_ADDRESS_TYPE = "[a-zA-Z| ]*\\<{0,1}[a-zA-Z| ]+@{1}[a-zA-Z| ]+\\.{1}[a-zA-Z| ]+\\>{0,1}";
    static final String REGEXP_IPV4_ADDRESS_TYPE = "[0-9]{1,3}\\.{1}[0-9]{1,3}\\.{1}[0-9]{1,3}\\.{1}[0-9]{1,3}";
    static final String REGEXP_IPV6_ADDRESS_TYPE = "[a-fA-F]{4}\\:{1}[a-fA-F0-9]{4}\\:{1}[a-fA-F0-9]{4}\\:{1}[a-fA-F0-9]{4}\\:{1}[a-fA-F0-9]{4}\\:{1}[a-fA-F0-9]{4}\\:{1}[a-fA-F0-9]{4}\\:{1}[a-fA-F0-9]{4}";
    static final String REGEXP_PHONE_NUMBER_ADDRESS_TYPE = "\\+?[0-9|\\.|\\-]+";
    private static final int SHORT_INTEGER_MAX = 127;
    static final String STRING_IPV4_ADDRESS_TYPE = "/TYPE=IPV4";
    static final String STRING_IPV6_ADDRESS_TYPE = "/TYPE=IPV6";
    static final String STRING_PHONE_NUMBER_ADDRESS_TYPE = "/TYPE=PLMN";
    private static final int TEXT_MAX = 127;
    private static HashMap<String, Integer> mContentTypeMap;
    protected ByteArrayOutputStream mMessage = null;
    private GenericPdu mPdu = null;
    private PduHeaders mPduHeader = null;
    protected int mPosition = 0;
    private final ContentResolver mResolver;
    private BufferStack mStack = null;

    private class BufferStack {
        private LengthRecordNode stack;
        int stackSize;
        private LengthRecordNode toCopy;

        private BufferStack() {
            this.stack = null;
            this.toCopy = null;
            this.stackSize = 0;
        }

        void newbuf() {
            if (this.toCopy == null) {
                LengthRecordNode temp = new LengthRecordNode();
                temp.currentMessage = PduComposer.this.mMessage;
                temp.currentPosition = PduComposer.this.mPosition;
                temp.next = this.stack;
                this.stack = temp;
                this.stackSize++;
                PduComposer.this.mMessage = new ByteArrayOutputStream();
                PduComposer.this.mPosition = 0;
                return;
            }
            throw new RuntimeException("BUG: Invalid newbuf() before copy()");
        }

        void pop() {
            ByteArrayOutputStream currentMessage = PduComposer.this.mMessage;
            int currentPosition = PduComposer.this.mPosition;
            PduComposer.this.mMessage = this.stack.currentMessage;
            PduComposer.this.mPosition = this.stack.currentPosition;
            this.toCopy = this.stack;
            this.stack = this.stack.next;
            this.stackSize--;
            this.toCopy.currentMessage = currentMessage;
            this.toCopy.currentPosition = currentPosition;
        }

        void copy() {
            PduComposer.this.arraycopy(this.toCopy.currentMessage.toByteArray(), 0, this.toCopy.currentPosition);
            this.toCopy = null;
        }

        PositionMarker mark() {
            PositionMarker m = new PositionMarker();
            m.c_pos = PduComposer.this.mPosition;
            m.currentStackSize = this.stackSize;
            return m;
        }
    }

    private static class LengthRecordNode {
        ByteArrayOutputStream currentMessage;
        public int currentPosition;
        public LengthRecordNode next;

        private LengthRecordNode() {
            this.currentMessage = null;
            this.currentPosition = 0;
            this.next = null;
        }
    }

    private class PositionMarker {
        private int c_pos;
        private int currentStackSize;

        private PositionMarker() {
        }

        int getLength() {
            if (this.currentStackSize == PduComposer.this.mStack.stackSize) {
                return PduComposer.this.mPosition - this.c_pos;
            }
            throw new RuntimeException("BUG: Invalid call to getLength()");
        }
    }

    static {
        mContentTypeMap = null;
        mContentTypeMap = new HashMap();
        for (int i = 0; i < PduContentTypes.contentTypes.length; i++) {
            mContentTypeMap.put(PduContentTypes.contentTypes[i], Integer.valueOf(i));
        }
    }

    public PduComposer(Context context, GenericPdu pdu) {
        this.mPdu = pdu;
        this.mResolver = context.getContentResolver();
        this.mPduHeader = pdu.getPduHeaders();
        this.mStack = new BufferStack();
        this.mMessage = new ByteArrayOutputStream();
        this.mPosition = 0;
    }

    public byte[] make() {
        int type = this.mPdu.getMessageType();
        switch (type) {
            case 128:
            case 132:
                if (makeSendRetrievePdu(type) != 0) {
                    return null;
                }
                break;
            case 130:
                if (makeNotifyInd() != 0) {
                    return null;
                }
                break;
            case 131:
                if (makeNotifyResp() != 0) {
                    return null;
                }
                break;
            case 133:
                if (makeAckInd() != 0) {
                    return null;
                }
                break;
            case 135:
                if (makeReadRecInd() != 0) {
                    return null;
                }
                break;
            default:
                return null;
        }
        return this.mMessage.toByteArray();
    }

    protected void arraycopy(byte[] buf, int pos, int length) {
        this.mMessage.write(buf, pos, length);
        this.mPosition += length;
    }

    protected void append(int value) {
        this.mMessage.write(value);
        this.mPosition++;
    }

    protected void appendShortInteger(int value) {
        append((value | 128) & 255);
    }

    protected void appendOctet(int number) {
        append(number);
    }

    protected void appendShortLength(int value) {
        append(value);
    }

    protected void appendLongInteger(long longInt) {
        int i = 0;
        long temp = longInt;
        int size = 0;
        while (temp != 0 && size < 8) {
            temp >>>= 8;
            size++;
        }
        appendShortLength(size);
        int shift = (size - 1) * 8;
        while (i < size) {
            append((int) ((longInt >>> shift) & 255));
            shift -= 8;
            i++;
        }
    }

    protected void appendTextString(byte[] text) {
        if ((text[0] & 255) > 127) {
            append(127);
        }
        arraycopy(text, 0, text.length);
        append(0);
    }

    protected void appendTextString(String str) {
        appendTextString(str.getBytes());
    }

    protected void appendEncodedString(EncodedStringValue enStr) {
        int charset = enStr.getCharacterSet();
        byte[] textString = enStr.getTextString();
        if (textString != null) {
            this.mStack.newbuf();
            PositionMarker start = this.mStack.mark();
            appendShortInteger(charset);
            appendTextString(textString);
            int len = start.getLength();
            this.mStack.pop();
            appendValueLength((long) len);
            this.mStack.copy();
        }
    }

    protected void appendUintvarInteger(long value) {
        long max = 127;
        int i = 0;
        while (i < 5 && value >= max) {
            max = (max << 7) | 127;
            i++;
        }
        while (i > 0) {
            append((int) ((128 | ((value >>> (i * 7)) & 127)) & 255));
            i--;
        }
        append((int) (value & 127));
    }

    protected void appendDateValue(long date) {
        appendLongInteger(date);
    }

    protected void appendValueLength(long value) {
        if (value < 31) {
            appendShortLength((int) value);
            return;
        }
        append(31);
        appendUintvarInteger(value);
    }

    protected void appendQuotedString(byte[] text) {
        append(34);
        arraycopy(text, 0, text.length);
        append(0);
    }

    protected void appendQuotedString(String str) {
        appendQuotedString(str.getBytes());
    }

    private EncodedStringValue appendAddressType(EncodedStringValue address) {
        EncodedStringValue temp = null;
        try {
            int addressType = checkAddressType(address.getString());
            temp = EncodedStringValue.copy(address);
            if (1 == addressType) {
                temp.appendTextString(STRING_PHONE_NUMBER_ADDRESS_TYPE.getBytes());
            } else if (3 == addressType) {
                temp.appendTextString(STRING_IPV4_ADDRESS_TYPE.getBytes());
            } else if (4 == addressType) {
                temp.appendTextString(STRING_IPV6_ADDRESS_TYPE.getBytes());
            }
            return temp;
        } catch (NullPointerException e) {
            return null;
        }
    }

    private int appendHeader(int field) {
        int i;
        EncodedStringValue temp;
        int octet;
        EncodedStringValue from;
        byte[] messageClass;
        switch (field) {
            case 129:
            case 130:
            case 151:
                EncodedStringValue[] addr = this.mPduHeader.getEncodedStringValues(field);
                if (addr != null) {
                    for (EncodedStringValue temp2 : addr) {
                        temp2 = appendAddressType(temp2);
                        if (temp2 == null) {
                            return 1;
                        }
                        appendOctet(field);
                        appendEncodedString(temp2);
                    }
                    break;
                }
                return 2;
            case 133:
                long date = this.mPduHeader.getLongInteger(field);
                if (-1 != date) {
                    appendOctet(field);
                    appendDateValue(date);
                    break;
                }
                return 2;
            case 134:
            case 143:
            case 144:
            case 145:
            case 149:
            case 153:
            case 155:
                octet = this.mPduHeader.getOctet(field);
                if (octet != 0) {
                    appendOctet(field);
                    appendOctet(octet);
                    break;
                }
                return 2;
            case 136:
                long expiry = this.mPduHeader.getLongInteger(field);
                if (-1 != expiry) {
                    appendOctet(field);
                    this.mStack.newbuf();
                    PositionMarker expiryStart = this.mStack.mark();
                    append(129);
                    appendLongInteger(expiry);
                    i = expiryStart.getLength();
                    this.mStack.pop();
                    appendValueLength((long) i);
                    this.mStack.copy();
                    break;
                }
                return 2;
            case 137:
                appendOctet(field);
                from = this.mPduHeader.getEncodedStringValue(field);
                if (from != null && !TextUtils.isEmpty(from.getString()) && !new String(from.getTextString()).equals(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR)) {
                    this.mStack.newbuf();
                    PositionMarker fstart = this.mStack.mark();
                    append(128);
                    temp2 = appendAddressType(from);
                    if (temp2 != null) {
                        appendEncodedString(temp2);
                        int flen = fstart.getLength();
                        this.mStack.pop();
                        appendValueLength((long) flen);
                        this.mStack.copy();
                        break;
                    }
                    return 1;
                }
                append(1);
                append(129);
                break;
                break;
            case 138:
                messageClass = this.mPduHeader.getTextString(field);
                if (messageClass != null) {
                    appendOctet(field);
                    if (!Arrays.equals(messageClass, PduHeaders.MESSAGE_CLASS_ADVERTISEMENT_STR.getBytes())) {
                        if (!Arrays.equals(messageClass, PduHeaders.MESSAGE_CLASS_AUTO_STR.getBytes())) {
                            if (!Arrays.equals(messageClass, PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes())) {
                                if (!Arrays.equals(messageClass, PduHeaders.MESSAGE_CLASS_INFORMATIONAL_STR.getBytes())) {
                                    appendTextString(messageClass);
                                    break;
                                }
                                appendOctet(130);
                                break;
                            }
                            appendOctet(128);
                            break;
                        }
                        appendOctet(131);
                        break;
                    }
                    appendOctet(129);
                    break;
                }
                return 2;
            case 139:
            case 152:
                messageClass = this.mPduHeader.getTextString(field);
                if (messageClass != null) {
                    appendOctet(field);
                    appendTextString(messageClass);
                    break;
                }
                return 2;
            case 141:
                appendOctet(field);
                octet = this.mPduHeader.getOctet(field);
                if (octet != 0) {
                    appendShortInteger(octet);
                    break;
                }
                appendShortInteger(18);
                break;
            case 150:
            case 154:
                from = this.mPduHeader.getEncodedStringValue(field);
                if (from != null) {
                    appendOctet(field);
                    appendEncodedString(from);
                    break;
                }
                return 2;
            default:
                return 3;
        }
        return 0;
    }

    private int makeReadRecInd() {
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(135);
        if (appendHeader(141) != 0 || appendHeader(139) != 0 || appendHeader(151) != 0 || appendHeader(137) != 0) {
            return 1;
        }
        appendHeader(133);
        if (appendHeader(155) != 0) {
            return 1;
        }
        return 0;
    }

    private int makeNotifyInd() {
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(130);
        if (appendHeader(152) != 0 || appendHeader(141) != 0 || appendHeader(138) != 0) {
            return 1;
        }
        appendOctet(142);
        long size = 0;
        if (this.mPdu instanceof NotificationInd) {
            size = ((NotificationInd) this.mPdu).getMessageSize();
        }
        appendLongInteger(size);
        if (appendHeader(136) != 0) {
            return 1;
        }
        appendOctet(131);
        byte[] contentLocation = null;
        if (this.mPdu instanceof NotificationInd) {
            contentLocation = ((NotificationInd) this.mPdu).getContentLocation();
        }
        if (contentLocation != null) {
            Log.d(LOG_TAG, "makeNotifyInd contentLocation != null");
            appendTextString(contentLocation);
        } else {
            Log.d(LOG_TAG, "makeNotifyInd contentLocation  = null");
        }
        appendOctet(150);
        EncodedStringValue subject = null;
        if (this.mPdu instanceof NotificationInd) {
            subject = ((NotificationInd) this.mPdu).getSubject();
        }
        if (subject != null) {
            Log.d(LOG_TAG, "makeNotifyInd subject != null");
            appendEncodedString(subject);
        } else {
            Log.d(LOG_TAG, "makeNotifyInd subject  = null");
        }
        if (appendHeader(137) == 0 && appendHeader(149) == 0) {
            return 0;
        }
        return 1;
    }

    private int makeNotifyResp() {
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(131);
        if (appendHeader(152) != 0 || appendHeader(141) != 0 || appendHeader(149) != 0) {
            return 1;
        }
        appendHeader(145);
        return 0;
    }

    private int makeAckInd() {
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(133);
        if (appendHeader(152) != 0 || appendHeader(141) != 0) {
            return 1;
        }
        appendHeader(145);
        return 0;
    }

    private int makeSendRetrievePdu(int type) {
        if (this.mMessage == null) {
            this.mMessage = new ByteArrayOutputStream();
            this.mPosition = 0;
        }
        appendOctet(140);
        appendOctet(type);
        appendOctet(152);
        byte[] trid = this.mPduHeader.getTextString(152);
        if (trid != null) {
            appendTextString(trid);
            if (appendHeader(141) != 0) {
                return 1;
            }
            appendHeader(133);
            if (appendHeader(137) != 0) {
                return 1;
            }
            boolean recipient = false;
            if (appendHeader(151) != 1) {
                recipient = true;
            }
            if (appendHeader(130) != 1) {
                recipient = true;
            }
            if (appendHeader(129) != 1) {
                recipient = true;
            }
            if (!recipient) {
                return 1;
            }
            appendHeader(150);
            appendHeader(138);
            appendHeader(136);
            appendHeader(143);
            appendHeader(134);
            appendHeader(144);
            if (type == 132) {
                appendHeader(153);
                appendHeader(154);
            }
            appendOctet(132);
            return makeMessageBody(type);
        }
        throw new IllegalArgumentException("Transaction-ID is null.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:162:0x0312 A:{SYNTHETIC, Splitter:B:162:0x0312} */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x02fe A:{SYNTHETIC, Splitter:B:153:0x02fe} */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x02ea A:{SYNTHETIC, Splitter:B:144:0x02ea} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02d7 A:{SYNTHETIC, Splitter:B:136:0x02d7} */
    /* JADX WARNING: Removed duplicated region for block: B:162:0x0312 A:{SYNTHETIC, Splitter:B:162:0x0312} */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x02fe A:{SYNTHETIC, Splitter:B:153:0x02fe} */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x02ea A:{SYNTHETIC, Splitter:B:144:0x02ea} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02d7 A:{SYNTHETIC, Splitter:B:136:0x02d7} */
    /* JADX WARNING: Removed duplicated region for block: B:162:0x0312 A:{SYNTHETIC, Splitter:B:162:0x0312} */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x02fe A:{SYNTHETIC, Splitter:B:153:0x02fe} */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x02ea A:{SYNTHETIC, Splitter:B:144:0x02ea} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02d7 A:{SYNTHETIC, Splitter:B:136:0x02d7} */
    /* JADX WARNING: Removed duplicated region for block: B:162:0x0312 A:{SYNTHETIC, Splitter:B:162:0x0312} */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x02fe A:{SYNTHETIC, Splitter:B:153:0x02fe} */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x02ea A:{SYNTHETIC, Splitter:B:144:0x02ea} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02d7 A:{SYNTHETIC, Splitter:B:136:0x02d7} */
    /* JADX WARNING: Removed duplicated region for block: B:162:0x0312 A:{SYNTHETIC, Splitter:B:162:0x0312} */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x02fe A:{SYNTHETIC, Splitter:B:153:0x02fe} */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x02ea A:{SYNTHETIC, Splitter:B:144:0x02ea} */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02d7 A:{SYNTHETIC, Splitter:B:136:0x02d7} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int makeMessageBody(int type) {
        InputStream cr;
        FileNotFoundException e;
        FileNotFoundException ctStart;
        IOException e2;
        IOException ctStart2;
        InputStream cr2;
        RuntimeException e3;
        RuntimeException ctStart3;
        String str;
        this.mStack.newbuf();
        PositionMarker ctStart4 = this.mStack.mark();
        String contentType = new String(this.mPduHeader.getTextString(132), Charset.defaultCharset());
        Integer contentTypeIdentifier = (Integer) mContentTypeMap.get(contentType);
        int i = 1;
        if (contentTypeIdentifier == null) {
            return 1;
        }
        PduBody body;
        appendShortInteger(contentTypeIdentifier.intValue());
        if (type == 132) {
            body = ((RetrieveConf) this.mPdu).getBody();
        } else {
            body = ((SendReq) this.mPdu).getBody();
        }
        PduBody body2 = body;
        String str2;
        PduBody pduBody;
        Integer num;
        PositionMarker positionMarker;
        if (body2 == null) {
            str2 = contentType;
            pduBody = body2;
            num = contentTypeIdentifier;
        } else if (body2.getPartsNum() == 0) {
            positionMarker = ctStart4;
            str2 = contentType;
            pduBody = body2;
            num = contentTypeIdentifier;
        } else {
            try {
                PduPart part = body2.getPart(0);
                byte[] start = part.getContentId();
                if (start != null) {
                    appendOctet(138);
                    if ((byte) 60 == start[0] && (byte) 62 == start[start.length - 1]) {
                        appendTextString(start);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("<");
                        stringBuilder.append(new String(start, Charset.defaultCharset()));
                        stringBuilder.append(">");
                        appendTextString(stringBuilder.toString());
                    }
                }
                appendOctet(137);
                appendTextString(part.getContentType());
            } catch (ArrayIndexOutOfBoundsException e4) {
                e4.printStackTrace();
            }
            int ctLength = ctStart4.getLength();
            this.mStack.pop();
            appendValueLength((long) ctLength);
            this.mStack.copy();
            int partNum = body2.getPartsNum();
            appendUintvarInteger((long) partNum);
            int i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 < partNum) {
                    PduPart part2 = body2.getPart(i3);
                    this.mStack.newbuf();
                    PositionMarker attachment = this.mStack.mark();
                    this.mStack.newbuf();
                    PositionMarker contentTypeBegin = this.mStack.mark();
                    byte[] partContentType = part2.getContentType();
                    if (partContentType == null) {
                        return i;
                    }
                    Integer partContentTypeIdentifier = (Integer) mContentTypeMap.get(new String(partContentType, Charset.defaultCharset()));
                    if (partContentTypeIdentifier == null) {
                        appendTextString(partContentType);
                    } else {
                        appendShortInteger(partContentTypeIdentifier.intValue());
                    }
                    byte[] name = part2.getName();
                    if (name == null) {
                        name = part2.getFilename();
                        if (name == null) {
                            name = part2.getContentLocation();
                            if (name == null) {
                                name = part2.getContentId();
                                if (name == null) {
                                    return 1;
                                }
                            }
                        }
                    }
                    byte[] name2 = name;
                    appendOctet(133);
                    appendTextString(name2);
                    positionMarker = ctStart4;
                    ctStart4 = part2.getCharset();
                    if (ctStart4 != null) {
                        appendOctet(129);
                        appendShortInteger(ctStart4);
                    }
                    int charset = ctStart4;
                    ctStart4 = contentTypeBegin.getLength();
                    this.mStack.pop();
                    str2 = contentType;
                    pduBody = body2;
                    appendValueLength((long) ctStart4);
                    this.mStack.copy();
                    byte[] contentId = part2.getContentId();
                    int contentTypeLength;
                    if (contentId != null) {
                        appendOctet(192);
                        if ((byte) 60 == contentId[0]) {
                            if ((byte) 62 == contentId[contentId.length - 1]) {
                                appendQuotedString(contentId);
                                PositionMarker positionMarker2 = ctStart4;
                            }
                        }
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("<");
                        contentTypeLength = ctStart4;
                        stringBuilder2.append(new String(contentId, Charset.defaultCharset()));
                        stringBuilder2.append(">");
                        appendQuotedString(stringBuilder2.toString());
                    } else {
                        contentTypeLength = ctStart4;
                    }
                    byte[] ctStart5 = part2.getContentLocation();
                    if (ctStart5 != null) {
                        appendOctet(142);
                        appendTextString(ctStart5);
                    }
                    int headerLength = attachment.getLength();
                    int dataLength = 0;
                    byte[] contentLocation = ctStart5;
                    ctStart4 = part2.getData();
                    PositionMarker positionMarker3;
                    Integer num2;
                    if (ctStart4 != null) {
                        arraycopy(ctStart4, null, ctStart4.length);
                        i2 = ctStart4.length;
                        positionMarker3 = ctStart4;
                        num = contentTypeIdentifier;
                        num2 = partContentTypeIdentifier;
                    } else {
                        contentType = null;
                        try {
                            name = new byte[1024];
                            byte[] partData = ctStart4;
                            try {
                                cr = contentType;
                                try {
                                    contentType = this.mResolver.openInputStream(part2.getDataUri());
                                    ctStart4 = null;
                                    while (true) {
                                        int len = ctStart4;
                                        try {
                                            ctStart4 = contentType.read(name);
                                            int len2 = ctStart4;
                                            num = contentTypeIdentifier;
                                            if (ctStart4 == -1) {
                                                break;
                                            }
                                            try {
                                                num2 = partContentTypeIdentifier;
                                                int len3 = len2;
                                                try {
                                                    this.mMessage.write(name, null, len3);
                                                    this.mPosition += len3;
                                                    dataLength += len3;
                                                    ctStart4 = len3;
                                                    contentTypeIdentifier = num;
                                                    partContentTypeIdentifier = num2;
                                                } catch (FileNotFoundException e5) {
                                                    e = e5;
                                                    ctStart = e;
                                                    if (contentType != null) {
                                                    }
                                                    return 1;
                                                } catch (IOException e6) {
                                                    e2 = e6;
                                                    ctStart2 = e2;
                                                    if (cr2 != null) {
                                                    }
                                                    return 1;
                                                } catch (RuntimeException e7) {
                                                    e3 = e7;
                                                    ctStart3 = e3;
                                                    if (cr2 != null) {
                                                    }
                                                    return 1;
                                                } catch (Throwable th) {
                                                    ctStart4 = th;
                                                    if (cr2 != null) {
                                                    }
                                                    throw ctStart4;
                                                }
                                            } catch (FileNotFoundException e8) {
                                                e = e8;
                                                num2 = partContentTypeIdentifier;
                                                ctStart = e;
                                                if (contentType != null) {
                                                }
                                                return 1;
                                            } catch (IOException e9) {
                                                e2 = e9;
                                                num2 = partContentTypeIdentifier;
                                                ctStart2 = e2;
                                                if (cr2 != null) {
                                                }
                                                return 1;
                                            } catch (RuntimeException e10) {
                                                e3 = e10;
                                                num2 = partContentTypeIdentifier;
                                                ctStart3 = e3;
                                                if (cr2 != null) {
                                                }
                                                return 1;
                                            } catch (Throwable th2) {
                                                num2 = partContentTypeIdentifier;
                                                ctStart4 = th2;
                                                if (cr2 != null) {
                                                }
                                                throw ctStart4;
                                            }
                                        } catch (FileNotFoundException e11) {
                                            e = e11;
                                            num = contentTypeIdentifier;
                                            num2 = partContentTypeIdentifier;
                                            ctStart = e;
                                            if (contentType != null) {
                                                try {
                                                    contentType.close();
                                                } catch (IOException e12) {
                                                }
                                            }
                                            return 1;
                                        } catch (IOException e13) {
                                            e2 = e13;
                                            num = contentTypeIdentifier;
                                            num2 = partContentTypeIdentifier;
                                            ctStart2 = e2;
                                            if (cr2 != null) {
                                                try {
                                                    cr2.close();
                                                } catch (IOException e14) {
                                                }
                                            }
                                            return 1;
                                        } catch (RuntimeException e15) {
                                            e3 = e15;
                                            num = contentTypeIdentifier;
                                            num2 = partContentTypeIdentifier;
                                            ctStart3 = e3;
                                            if (cr2 != null) {
                                                try {
                                                    cr2.close();
                                                } catch (IOException e16) {
                                                }
                                            }
                                            return 1;
                                        } catch (Throwable th22) {
                                            num = contentTypeIdentifier;
                                            num2 = partContentTypeIdentifier;
                                            ctStart4 = th22;
                                            if (cr2 != null) {
                                                try {
                                                    cr2.close();
                                                } catch (IOException e17) {
                                                }
                                            }
                                            throw ctStart4;
                                        }
                                    }
                                    if (contentType != null) {
                                        try {
                                            contentType.close();
                                        } catch (IOException e18) {
                                        }
                                    }
                                    i2 = dataLength;
                                } catch (FileNotFoundException e19) {
                                    e = e19;
                                    num = contentTypeIdentifier;
                                    num2 = partContentTypeIdentifier;
                                    contentType = cr;
                                    ctStart = e;
                                    if (contentType != null) {
                                    }
                                    return 1;
                                } catch (IOException e20) {
                                    e2 = e20;
                                    num = contentTypeIdentifier;
                                    num2 = partContentTypeIdentifier;
                                    cr2 = cr;
                                    ctStart2 = e2;
                                    if (cr2 != null) {
                                    }
                                    return 1;
                                } catch (RuntimeException e21) {
                                    e3 = e21;
                                    num = contentTypeIdentifier;
                                    num2 = partContentTypeIdentifier;
                                    cr2 = cr;
                                    ctStart3 = e3;
                                    if (cr2 != null) {
                                    }
                                    return 1;
                                } catch (Throwable th222) {
                                    num = contentTypeIdentifier;
                                    num2 = partContentTypeIdentifier;
                                    ctStart4 = th222;
                                    cr2 = cr;
                                    if (cr2 != null) {
                                    }
                                    throw ctStart4;
                                }
                            } catch (FileNotFoundException e22) {
                                e = e22;
                                str = contentType;
                                num = contentTypeIdentifier;
                                num2 = partContentTypeIdentifier;
                                ctStart = e;
                                if (contentType != null) {
                                }
                                return 1;
                            } catch (IOException e23) {
                                e2 = e23;
                                str = contentType;
                                num = contentTypeIdentifier;
                                num2 = partContentTypeIdentifier;
                                ctStart2 = e2;
                                if (cr2 != null) {
                                }
                                return 1;
                            } catch (RuntimeException e24) {
                                e3 = e24;
                                str = contentType;
                                num = contentTypeIdentifier;
                                num2 = partContentTypeIdentifier;
                                ctStart3 = e3;
                                if (cr2 != null) {
                                }
                                return 1;
                            } catch (Throwable th2222) {
                                str = contentType;
                                num = contentTypeIdentifier;
                                num2 = partContentTypeIdentifier;
                                ctStart4 = th2222;
                                if (cr2 != null) {
                                }
                                throw ctStart4;
                            }
                        } catch (FileNotFoundException e25) {
                            e = e25;
                            positionMarker3 = ctStart4;
                            cr = contentType;
                            num = contentTypeIdentifier;
                            num2 = partContentTypeIdentifier;
                            ctStart = e;
                            if (contentType != null) {
                            }
                            return 1;
                        } catch (IOException e26) {
                            e2 = e26;
                            positionMarker3 = ctStart4;
                            cr = contentType;
                            num = contentTypeIdentifier;
                            num2 = partContentTypeIdentifier;
                            ctStart2 = e2;
                            if (cr2 != null) {
                            }
                            return 1;
                        } catch (RuntimeException e27) {
                            e3 = e27;
                            positionMarker3 = ctStart4;
                            cr = contentType;
                            num = contentTypeIdentifier;
                            num2 = partContentTypeIdentifier;
                            ctStart3 = e3;
                            if (cr2 != null) {
                            }
                            return 1;
                        } catch (Throwable th22222) {
                            positionMarker3 = ctStart4;
                            str = contentType;
                            num = contentTypeIdentifier;
                            num2 = partContentTypeIdentifier;
                            ctStart4 = th22222;
                            if (cr2 != null) {
                            }
                            throw ctStart4;
                        }
                    }
                    if (i2 == attachment.getLength() - headerLength) {
                        this.mStack.pop();
                        appendUintvarInteger((long) headerLength);
                        appendUintvarInteger((long) i2);
                        this.mStack.copy();
                        i2 = i3 + 1;
                        ctStart4 = positionMarker;
                        contentType = str2;
                        body2 = pduBody;
                        contentTypeIdentifier = num;
                        i = 1;
                    } else {
                        throw new RuntimeException("BUG: Length sanity check failed");
                    }
                }
                str2 = contentType;
                pduBody = body2;
                num = contentTypeIdentifier;
                return 0;
            }
        }
        appendUintvarInteger(0);
        this.mStack.pop();
        this.mStack.copy();
        return 0;
    }

    protected static int checkAddressType(String address) {
        if (address == null) {
            return 5;
        }
        if (address.matches(REGEXP_IPV4_ADDRESS_TYPE)) {
            return 3;
        }
        if (address.matches(REGEXP_PHONE_NUMBER_ADDRESS_TYPE)) {
            return 1;
        }
        if (address.matches(REGEXP_EMAIL_ADDRESS_TYPE)) {
            return 2;
        }
        if (address.matches(REGEXP_IPV6_ADDRESS_TYPE)) {
            return 4;
        }
        return 5;
    }
}
