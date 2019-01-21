package sun.security.x509;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.security.AccessController;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import sun.security.action.GetBooleanAction;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.util.Debug;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class AVA implements DerEncoder {
    static final int DEFAULT = 1;
    private static final boolean PRESERVE_OLD_DC_ENCODING = ((Boolean) AccessController.doPrivileged(new GetBooleanAction("com.sun.security.preserveOldDCEncoding"))).booleanValue();
    static final int RFC1779 = 2;
    static final int RFC2253 = 3;
    private static final Debug debug = Debug.getInstance(X509CertImpl.NAME, "\t[AVA]");
    private static final String escapedDefault = ",+<>;\"";
    private static final String hexDigits = "0123456789ABCDEF";
    private static final String specialChars1779 = ",=\n+<>#;\\\"";
    private static final String specialChars2253 = ",=+<>#;\\\"";
    private static final String specialCharsDefault = ",=\n+<>#;\\\" ";
    final ObjectIdentifier oid;
    final DerValue value;

    public AVA(ObjectIdentifier type, DerValue val) {
        if (type == null || val == null) {
            throw new NullPointerException();
        }
        this.oid = type;
        this.value = val;
    }

    AVA(Reader in) throws IOException {
        this(in, 1);
    }

    AVA(Reader in, Map<String, String> keywordMap) throws IOException {
        this(in, 1, keywordMap);
    }

    AVA(Reader in, int format) throws IOException {
        this(in, format, Collections.emptyMap());
    }

    AVA(Reader in, int format, Map<String, String> keywordMap) throws IOException {
        int c;
        StringBuilder temp = new StringBuilder();
        while (true) {
            c = readChar(in, "Incorrect AVA format");
            if (c == 61) {
                break;
            }
            temp.append((char) c);
        }
        this.oid = AVAKeyword.getOID(temp.toString(), format, keywordMap);
        temp.setLength(0);
        if (format != 3) {
            while (true) {
                c = in.read();
                if (c != 32 && c != 10) {
                    break;
                }
            }
        } else {
            c = in.read();
            if (c == 32) {
                throw new IOException("Incorrect AVA RFC2253 format - leading space must be escaped");
            }
        }
        if (c == -1) {
            this.value = new DerValue("");
            return;
        }
        if (c == 35) {
            this.value = parseHexString(in, format);
        } else if (c != 34 || format == 3) {
            this.value = parseString(in, c, format, temp);
        } else {
            this.value = parseQuotedString(in, temp);
        }
    }

    public ObjectIdentifier getObjectIdentifier() {
        return this.oid;
    }

    public DerValue getDerValue() {
        return this.value;
    }

    public String getValueString() {
        try {
            String s = this.value.getAsString();
            if (s != null) {
                return s;
            }
            throw new RuntimeException("AVA string is null");
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AVA error: ");
            stringBuilder.append(e);
            throw new RuntimeException(stringBuilder.toString(), e);
        }
    }

    private static DerValue parseHexString(Reader in, int format) throws IOException {
        int c;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte b = (byte) 0;
        int cNdx = 0;
        while (true) {
            c = in.read();
            if (isTerminator(c, format)) {
                break;
            } else if (c == 32 || c == 10) {
                do {
                    if (c != 32 || c == 10) {
                        c = in.read();
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("AVA parse, invalid hex digit: ");
                        stringBuilder.append((char) c);
                        throw new IOException(stringBuilder.toString());
                    }
                } while (!isTerminator(c, format));
            } else {
                int cVal = hexDigits.indexOf(Character.toUpperCase((char) c));
                if (cVal != -1) {
                    if (cNdx % 2 == 1) {
                        b = (byte) ((b * 16) + ((byte) cVal));
                        baos.write(b);
                    } else {
                        b = (byte) cVal;
                    }
                    cNdx++;
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("AVA parse, invalid hex digit: ");
                    stringBuilder2.append((char) c);
                    throw new IOException(stringBuilder2.toString());
                }
            }
        }
        do {
            if (c != 32) {
            }
            c = in.read();
        } while (!isTerminator(c, format));
        if (cNdx == 0) {
            throw new IOException("AVA parse, zero hex digits");
        } else if (cNdx % 2 != 1) {
            return new DerValue(baos.toByteArray());
        } else {
            throw new IOException("AVA parse, odd number of hex digits");
        }
    }

    private DerValue parseQuotedString(Reader in, StringBuilder temp) throws IOException {
        int c = readChar(in, "Quoted string did not end in quote");
        List<Byte> embeddedHex = new ArrayList();
        boolean isPrintableString = true;
        while (c != 34) {
            if (c == 92) {
                c = readChar(in, "Quoted string did not end in quote");
                Byte embeddedHexPair = getEmbeddedHexPair(c, in);
                Byte hexByte = embeddedHexPair;
                if (embeddedHexPair != null) {
                    isPrintableString = PRESERVE_OLD_DC_ENCODING;
                    embeddedHex.add(hexByte);
                    c = in.read();
                } else if (specialChars1779.indexOf((char) c) < 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid escaped character in AVA: ");
                    stringBuilder.append((char) c);
                    throw new IOException(stringBuilder.toString());
                }
            }
            if (embeddedHex.size() > 0) {
                temp.append(getEmbeddedHexString(embeddedHex));
                embeddedHex.clear();
            }
            isPrintableString &= DerValue.isPrintableStringChar((char) c);
            temp.append((char) c);
            c = readChar(in, "Quoted string did not end in quote");
        }
        if (embeddedHex.size() > 0) {
            temp.append(getEmbeddedHexString(embeddedHex));
            embeddedHex.clear();
        }
        while (true) {
            c = in.read();
            if (c != 10 && c != 32) {
                break;
            }
        }
        if (c != -1) {
            throw new IOException("AVA had characters other than whitespace after terminating quote");
        } else if (this.oid.equals(PKCS9Attribute.EMAIL_ADDRESS_OID) || (this.oid.equals(X500Name.DOMAIN_COMPONENT_OID) && !PRESERVE_OLD_DC_ENCODING)) {
            return new DerValue((byte) 22, temp.toString());
        } else {
            if (isPrintableString) {
                return new DerValue(temp.toString());
            }
            return new DerValue((byte) 12, temp.toString());
        }
    }

    private DerValue parseString(Reader in, int c, int format, StringBuilder temp) throws IOException {
        Reader reader = in;
        int i = format;
        StringBuilder stringBuilder = temp;
        List<Byte> embeddedHex = new ArrayList();
        boolean isPrintableString = true;
        boolean leadingChar = true;
        int spaceCount = 0;
        int c2 = c;
        do {
            boolean escape = PRESERVE_OLD_DC_ENCODING;
            if (c2 == 92) {
                escape = true;
                c2 = readChar(reader, "Invalid trailing backslash");
                Byte embeddedHexPair = getEmbeddedHexPair(c2, reader);
                Byte hexByte = embeddedHexPair;
                StringBuilder stringBuilder2;
                if (embeddedHexPair != null) {
                    isPrintableString = PRESERVE_OLD_DC_ENCODING;
                    embeddedHex.add(hexByte);
                    c2 = in.read();
                    leadingChar = PRESERVE_OLD_DC_ENCODING;
                } else if (i == 1 && specialCharsDefault.indexOf((char) c2) == -1) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid escaped character in AVA: '");
                    stringBuilder2.append((char) c2);
                    stringBuilder2.append("'");
                    throw new IOException(stringBuilder2.toString());
                } else if (i == 3) {
                    if (c2 == 32) {
                        if (!(leadingChar || trailingSpace(in))) {
                            throw new IOException("Invalid escaped space character in AVA.  Only a leading or trailing space character can be escaped.");
                        }
                    } else if (c2 == 35) {
                        if (!leadingChar) {
                            throw new IOException("Invalid escaped '#' character in AVA.  Only a leading '#' can be escaped.");
                        }
                    } else if (specialChars2253.indexOf((char) c2) == -1) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Invalid escaped character in AVA: '");
                        stringBuilder2.append((char) c2);
                        stringBuilder2.append("'");
                        throw new IOException(stringBuilder2.toString());
                    }
                }
            } else if (i == 3 && specialChars2253.indexOf((char) c2) != -1) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Character '");
                stringBuilder3.append((char) c2);
                stringBuilder3.append("' in AVA appears without escape");
                throw new IOException(stringBuilder3.toString());
            }
            if (embeddedHex.size() > 0) {
                for (int i2 = 0; i2 < spaceCount; i2++) {
                    stringBuilder.append(" ");
                }
                spaceCount = 0;
                stringBuilder.append(getEmbeddedHexString(embeddedHex));
                embeddedHex.clear();
            }
            boolean isPrintableString2 = DerValue.isPrintableStringChar((char) c2) & isPrintableString;
            if (c2 != 32 || escape) {
                for (int i3 = 0; i3 < spaceCount; i3++) {
                    stringBuilder.append(" ");
                }
                spaceCount = 0;
                stringBuilder.append((char) c2);
            } else {
                spaceCount++;
            }
            c2 = in.read();
            leadingChar = PRESERVE_OLD_DC_ENCODING;
            isPrintableString = isPrintableString2;
        } while (!isTerminator(c2, i));
        if (i != 3 || spaceCount <= 0) {
            if (embeddedHex.size() > 0) {
                stringBuilder.append(getEmbeddedHexString(embeddedHex));
                embeddedHex.clear();
            }
            if (this.oid.equals(PKCS9Attribute.EMAIL_ADDRESS_OID) || (this.oid.equals(X500Name.DOMAIN_COMPONENT_OID) && !PRESERVE_OLD_DC_ENCODING)) {
                return new DerValue((byte) 22, temp.toString());
            }
            if (isPrintableString) {
                return new DerValue(temp.toString());
            }
            return new DerValue((byte) 12, temp.toString());
        }
        throw new IOException("Incorrect AVA RFC2253 format - trailing space must be escaped");
    }

    private static Byte getEmbeddedHexPair(int c1, Reader in) throws IOException {
        if (hexDigits.indexOf(Character.toUpperCase((char) c1)) < 0) {
            return null;
        }
        int c2 = readChar(in, "unexpected EOF - escaped hex value must include two valid digits");
        if (hexDigits.indexOf(Character.toUpperCase((char) c2)) >= 0) {
            return new Byte((byte) ((Character.digit((char) c1, 16) << 4) + Character.digit((char) c2, 16)));
        }
        throw new IOException("escaped hex value must include two valid digits");
    }

    private static String getEmbeddedHexString(List<Byte> hexList) throws IOException {
        int n = hexList.size();
        byte[] hexBytes = new byte[n];
        for (int i = 0; i < n; i++) {
            hexBytes[i] = ((Byte) hexList.get(i)).byteValue();
        }
        return new String(hexBytes, "UTF8");
    }

    private static boolean isTerminator(int ch, int format) {
        boolean z = true;
        if (ch != -1) {
            if (ch != 59) {
                switch (ch) {
                    case 43:
                    case 44:
                        break;
                    default:
                        return PRESERVE_OLD_DC_ENCODING;
                }
            }
            if (format == 3) {
                z = PRESERVE_OLD_DC_ENCODING;
            }
            return z;
        }
        return true;
    }

    private static int readChar(Reader in, String errMsg) throws IOException {
        int c = in.read();
        if (c != -1) {
            return c;
        }
        throw new IOException(errMsg);
    }

    private static boolean trailingSpace(Reader in) throws IOException {
        if (!in.markSupported()) {
            return true;
        }
        boolean trailing;
        in.mark(9999);
        while (true) {
            int nextChar = in.read();
            if (nextChar != -1) {
                if (nextChar != 32) {
                    if (nextChar != 92) {
                        trailing = PRESERVE_OLD_DC_ENCODING;
                        break;
                    } else if (in.read() != 32) {
                        trailing = PRESERVE_OLD_DC_ENCODING;
                        break;
                    }
                }
            } else {
                trailing = true;
                break;
            }
        }
        in.reset();
        return trailing;
    }

    AVA(DerValue derval) throws IOException {
        if (derval.tag == (byte) 48) {
            this.oid = X500Name.intern(derval.data.getOID());
            this.value = derval.data.getDerValue();
            if (derval.data.available() != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AVA, extra bytes = ");
                stringBuilder.append(derval.data.available());
                throw new IOException(stringBuilder.toString());
            }
            return;
        }
        throw new IOException("AVA not a sequence");
    }

    AVA(DerInputStream in) throws IOException {
        this(in.getDerValue());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AVA)) {
            return PRESERVE_OLD_DC_ENCODING;
        }
        return toRFC2253CanonicalString().equals(((AVA) obj).toRFC2253CanonicalString());
    }

    public int hashCode() {
        return toRFC2253CanonicalString().hashCode();
    }

    public void encode(DerOutputStream out) throws IOException {
        derEncode(out);
    }

    public void derEncode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        DerOutputStream tmp2 = new DerOutputStream();
        tmp.putOID(this.oid);
        this.value.encode(tmp);
        tmp2.write((byte) 48, tmp);
        out.write(tmp2.toByteArray());
    }

    private String toKeyword(int format, Map<String, String> oidMap) {
        return AVAKeyword.getKeyword(this.oid, format, oidMap);
    }

    public String toString() {
        return toKeywordValueString(toKeyword(1, Collections.emptyMap()));
    }

    public String toRFC1779String() {
        return toRFC1779String(Collections.emptyMap());
    }

    public String toRFC1779String(Map<String, String> oidMap) {
        return toKeywordValueString(toKeyword(2, oidMap));
    }

    public String toRFC2253String() {
        return toRFC2253String(Collections.emptyMap());
    }

    public String toRFC2253String(Map<String, String> oidMap) {
        StringBuilder typeAndValue = new StringBuilder(100);
        typeAndValue.append(toKeyword(3, oidMap));
        typeAndValue.append('=');
        int j = 0;
        if ((typeAndValue.charAt(0) < '0' || typeAndValue.charAt(0) > '9') && isDerString(this.value, PRESERVE_OLD_DC_ENCODING)) {
            String valStr = null;
            try {
                valStr = new String(this.value.getDataBytes(), "UTF8");
                String escapees = ",=+<>#;\"\\";
                StringBuilder sbuffer = new StringBuilder();
                int i = 0;
                while (i < valStr.length()) {
                    char c = valStr.charAt(i);
                    if (DerValue.isPrintableStringChar(c) || ",=+<>#;\"\\".indexOf((int) c) >= 0) {
                        if (",=+<>#;\"\\".indexOf((int) c) >= 0) {
                            sbuffer.append('\\');
                        }
                        sbuffer.append(c);
                    } else if (c == 0) {
                        sbuffer.append("\\00");
                    } else if (debug == null || !Debug.isOn("ava")) {
                        sbuffer.append(c);
                    } else {
                        byte[] valueBytes = null;
                        try {
                            valueBytes = Character.toString(c).getBytes("UTF8");
                            for (int j2 = j; j2 < valueBytes.length; j2++) {
                                sbuffer.append('\\');
                                sbuffer.append(Character.toUpperCase(Character.forDigit((valueBytes[j2] >>> 4) & 15, 16)));
                                sbuffer.append(Character.toUpperCase(Character.forDigit(valueBytes[j2] & 15, 16)));
                            }
                        } catch (IOException e) {
                            throw new IllegalArgumentException("DER Value conversion");
                        }
                    }
                    i++;
                    j = 0;
                }
                char[] chars = sbuffer.toString().toCharArray();
                StringBuilder sbuffer2 = new StringBuilder();
                int lead = 0;
                while (lead < chars.length && (chars[lead] == ' ' || chars[lead] == 13)) {
                    lead++;
                }
                int trail = chars.length - 1;
                while (trail >= 0 && (chars[trail] == ' ' || chars[trail] == 13)) {
                    trail--;
                }
                int i2 = 0;
                while (true) {
                    int i3 = i2;
                    if (i3 >= chars.length) {
                        break;
                    }
                    char c2 = chars[i3];
                    if (i3 < lead || i3 > trail) {
                        sbuffer2.append('\\');
                    }
                    sbuffer2.append(c2);
                    i2 = i3 + 1;
                }
                typeAndValue.append(sbuffer2.toString());
            } catch (IOException e2) {
                throw new IllegalArgumentException("DER Value conversion");
            }
        }
        byte[] data = null;
        try {
            data = this.value.toByteArray();
            typeAndValue.append('#');
            while (j < data.length) {
                byte b = data[j];
                typeAndValue.append(Character.forDigit((b >>> 4) & 15, 16));
                typeAndValue.append(Character.forDigit(15 & b, 16));
                j++;
            }
        } catch (IOException e3) {
            throw new IllegalArgumentException("DER Value conversion");
        }
        return typeAndValue.toString();
    }

    public String toRFC2253CanonicalString() {
        StringBuilder typeAndValue = new StringBuilder(40);
        typeAndValue.append(toKeyword(3, Collections.emptyMap()));
        typeAndValue.append('=');
        int j = 0;
        if ((typeAndValue.charAt(0) < '0' || typeAndValue.charAt(0) > '9') && (isDerString(this.value, true) || this.value.tag == (byte) 20)) {
            String valStr = null;
            try {
                valStr = new String(this.value.getDataBytes(), "UTF8");
                String escapees = ",+<>;\"\\";
                StringBuilder sbuffer = new StringBuilder();
                boolean previousWhite = PRESERVE_OLD_DC_ENCODING;
                int i = 0;
                while (i < valStr.length()) {
                    boolean previousWhite2;
                    char c = valStr.charAt(i);
                    if (DerValue.isPrintableStringChar(c) || ",+<>;\"\\".indexOf((int) c) >= 0 || (i == 0 && c == '#')) {
                        if ((i == 0 && c == '#') || ",+<>;\"\\".indexOf((int) c) >= 0) {
                            sbuffer.append('\\');
                        }
                        if (!Character.isWhitespace(c)) {
                            previousWhite2 = PRESERVE_OLD_DC_ENCODING;
                            sbuffer.append(c);
                        } else if (previousWhite) {
                            i++;
                        } else {
                            previousWhite2 = true;
                            sbuffer.append(c);
                        }
                    } else if (debug == null || !Debug.isOn("ava")) {
                        previousWhite2 = PRESERVE_OLD_DC_ENCODING;
                        sbuffer.append(c);
                    } else {
                        previousWhite = PRESERVE_OLD_DC_ENCODING;
                        byte[] valueBytes = null;
                        try {
                            byte[] valueBytes2 = Character.toString(c).getBytes("UTF8");
                            for (valueBytes = null; valueBytes < valueBytes2.length; valueBytes++) {
                                sbuffer.append('\\');
                                sbuffer.append(Character.forDigit((valueBytes2[valueBytes] >>> 4) & 15, 16));
                                sbuffer.append(Character.forDigit(valueBytes2[valueBytes] & 15, 16));
                            }
                            i++;
                        } catch (IOException e) {
                            throw new IllegalArgumentException("DER Value conversion");
                        }
                    }
                    previousWhite = previousWhite2;
                    i++;
                }
                typeAndValue.append(sbuffer.toString().trim());
            } catch (IOException e2) {
                throw new IllegalArgumentException("DER Value conversion");
            }
        }
        byte[] data = null;
        try {
            data = this.value.toByteArray();
            typeAndValue.append('#');
            while (j < data.length) {
                byte b = data[j];
                typeAndValue.append(Character.forDigit((b >>> 4) & 15, 16));
                typeAndValue.append(Character.forDigit(15 & b, 16));
                j++;
            }
        } catch (IOException e3) {
            throw new IllegalArgumentException("DER Value conversion");
        }
        return Normalizer.normalize(typeAndValue.toString().toUpperCase(Locale.US).toLowerCase(Locale.US), Form.NFKD);
    }

    private static boolean isDerString(DerValue value, boolean canonical) {
        byte b;
        if (canonical) {
            b = value.tag;
            return (b == (byte) 12 || b == (byte) 19) ? true : PRESERVE_OLD_DC_ENCODING;
        } else {
            b = value.tag;
            if (!(b == (byte) 12 || b == (byte) 22 || b == (byte) 27 || b == (byte) 30)) {
                switch (b) {
                    case (byte) 19:
                    case (byte) 20:
                        break;
                    default:
                        return PRESERVE_OLD_DC_ENCODING;
                }
            }
            return true;
        }
    }

    boolean hasRFC2253Keyword() {
        return AVAKeyword.hasKeyword(this.oid, 3);
    }

    private String toKeywordValueString(String keyword) {
        StringBuilder retval = new StringBuilder(40);
        retval.append(keyword);
        retval.append("=");
        try {
            String valStr = this.value.getAsString();
            int i = 15;
            int i2 = 0;
            if (valStr == null) {
                byte[] data = this.value.toByteArray();
                retval.append('#');
                while (i2 < data.length) {
                    retval.append(hexDigits.charAt((data[i2] >> 4) & 15));
                    retval.append(hexDigits.charAt(data[i2] & 15));
                    i2++;
                }
            } else {
                StringBuilder sbuffer = new StringBuilder();
                String escapees = ",+=\n<>#;\\\"";
                int length = valStr.length();
                Object obj = 34;
                boolean alreadyQuoted = (length > 1 && valStr.charAt(0) == '\"' && valStr.charAt(length - 1) == '\"') ? true : PRESERVE_OLD_DC_ENCODING;
                boolean previousWhite = PRESERVE_OLD_DC_ENCODING;
                boolean quoteNeeded = PRESERVE_OLD_DC_ENCODING;
                int i3 = 0;
                while (i3 < length) {
                    char c;
                    String valStr2;
                    int i4;
                    Object obj2;
                    boolean previousWhite2;
                    char c2 = valStr.charAt(i3);
                    if (alreadyQuoted) {
                        if (i3 != 0) {
                            if (i3 != length - 1) {
                                c = c2;
                            }
                        }
                        sbuffer.append(c2);
                        valStr2 = valStr;
                        i4 = i;
                        obj2 = obj;
                        i3++;
                        obj = obj2;
                        i = i4;
                        valStr = valStr2;
                    } else {
                        c = c2;
                    }
                    char c3 = '\\';
                    if (DerValue.isPrintableStringChar(c)) {
                        valStr2 = valStr;
                        i4 = i;
                    } else if (",+=\n<>#;\\\"".indexOf((int) c) >= 0) {
                        valStr2 = valStr;
                        i4 = i;
                    } else {
                        if (debug == null || !Debug.isOn("ava")) {
                            valStr2 = valStr;
                            i4 = i;
                            sbuffer.append(c);
                            previousWhite = null;
                        } else {
                            byte[] valueBytes = Character.toString(c).getBytes("UTF8");
                            int j = 0;
                            while (j < valueBytes.length) {
                                sbuffer.append(c3);
                                sbuffer.append(Character.toUpperCase(Character.forDigit(15 & (valueBytes[j] >>> 4), 16)));
                                valStr2 = valStr;
                                sbuffer.append(Character.toUpperCase(Character.forDigit(15 & valueBytes[j], 16)));
                                j++;
                                i = 15;
                                valStr = valStr2;
                                c3 = '\\';
                            }
                            valStr2 = valStr;
                            i4 = 15;
                            previousWhite = PRESERVE_OLD_DC_ENCODING;
                        }
                        obj2 = 34;
                        i3++;
                        obj = obj2;
                        i = i4;
                        valStr = valStr2;
                    }
                    if (!quoteNeeded && ((i3 == 0 && (c == ' ' || c == 10)) || ",+=\n<>#;\\\"".indexOf((int) c) >= 0)) {
                        quoteNeeded = true;
                    }
                    if (c == ' ' || c == 10) {
                        obj2 = 34;
                        if (!quoteNeeded && previousWhite) {
                            quoteNeeded = true;
                        }
                        previousWhite2 = true;
                    } else {
                        char c4;
                        obj2 = 34;
                        if (c != '\"') {
                            c4 = '\\';
                            if (c == '\\') {
                            }
                            previousWhite2 = PRESERVE_OLD_DC_ENCODING;
                        } else {
                            c4 = '\\';
                        }
                        sbuffer.append(c4);
                        previousWhite2 = PRESERVE_OLD_DC_ENCODING;
                    }
                    sbuffer.append(c);
                    previousWhite = previousWhite2;
                    i3++;
                    obj = obj2;
                    i = i4;
                    valStr = valStr2;
                }
                if (sbuffer.length() > 0) {
                    char trailChar = sbuffer.charAt(sbuffer.length() - 1);
                    if (trailChar == ' ' || trailChar == 10) {
                        quoteNeeded = true;
                    }
                }
                if (alreadyQuoted || !quoteNeeded) {
                    retval.append(sbuffer.toString());
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("\"");
                    stringBuilder.append(sbuffer.toString());
                    stringBuilder.append("\"");
                    retval.append(stringBuilder.toString());
                }
            }
            return retval.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("DER Value conversion");
        }
    }
}
