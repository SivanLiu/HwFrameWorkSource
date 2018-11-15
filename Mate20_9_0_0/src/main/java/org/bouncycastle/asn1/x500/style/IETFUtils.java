package org.bouncycastle.asn1.x500.style;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.DERUniversalString;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

public class IETFUtils {
    public static void appendRDN(StringBuffer stringBuffer, RDN rdn, Hashtable hashtable) {
        if (rdn.isMultiValued()) {
            AttributeTypeAndValue[] typesAndValues = rdn.getTypesAndValues();
            Object obj = 1;
            for (int i = 0; i != typesAndValues.length; i++) {
                if (obj != null) {
                    obj = null;
                } else {
                    stringBuffer.append('+');
                }
                appendTypeAndValue(stringBuffer, typesAndValues[i], hashtable);
            }
        } else if (rdn.getFirst() != null) {
            appendTypeAndValue(stringBuffer, rdn.getFirst(), hashtable);
        }
    }

    public static void appendTypeAndValue(StringBuffer stringBuffer, AttributeTypeAndValue attributeTypeAndValue, Hashtable hashtable) {
        String str = (String) hashtable.get(attributeTypeAndValue.getType());
        if (str == null) {
            str = attributeTypeAndValue.getType().getId();
        }
        stringBuffer.append(str);
        stringBuffer.append('=');
        stringBuffer.append(valueToString(attributeTypeAndValue.getValue()));
    }

    private static boolean atvAreEqual(AttributeTypeAndValue attributeTypeAndValue, AttributeTypeAndValue attributeTypeAndValue2) {
        return attributeTypeAndValue == attributeTypeAndValue2 ? true : attributeTypeAndValue != null && attributeTypeAndValue2 != null && attributeTypeAndValue.getType().equals(attributeTypeAndValue2.getType()) && canonicalize(valueToString(attributeTypeAndValue.getValue())).equals(canonicalize(valueToString(attributeTypeAndValue2.getValue())));
    }

    private static String bytesToString(byte[] bArr) {
        char[] cArr = new char[bArr.length];
        for (int i = 0; i != cArr.length; i++) {
            cArr[i] = (char) (bArr[i] & 255);
        }
        return new String(cArr);
    }

    public static String canonicalize(String str) {
        str = Strings.toLowerCase(str);
        int i = 0;
        if (str.length() > 0 && str.charAt(0) == '#') {
            ASN1Primitive decodeObject = decodeObject(str);
            if (decodeObject instanceof ASN1String) {
                str = Strings.toLowerCase(((ASN1String) decodeObject).getString());
            }
        }
        if (str.length() > 1) {
            int i2;
            while (true) {
                i2 = i + 1;
                if (i2 < str.length() && str.charAt(i) == '\\' && str.charAt(i2) == ' ') {
                    i += 2;
                } else {
                    i2 = str.length() - 1;
                }
            }
            i2 = str.length() - 1;
            while (true) {
                int i3 = i2 - 1;
                if (i3 > 0 && str.charAt(i3) == '\\' && str.charAt(i2) == ' ') {
                    i2 -= 2;
                } else if (i > 0 || i2 < str.length() - 1) {
                    str = str.substring(i, i2 + 1);
                }
            }
            str = str.substring(i, i2 + 1);
        }
        return stripInternalSpaces(str);
    }

    private static int convertHex(char c) {
        if ('0' <= c && c <= '9') {
            return c - 48;
        }
        int i = ('a' > c || c > 'f') ? c - 65 : c - 97;
        return i + 10;
    }

    public static ASN1ObjectIdentifier decodeAttrName(String str, Hashtable hashtable) {
        if (Strings.toUpperCase(str).startsWith("OID.")) {
            return new ASN1ObjectIdentifier(str.substring(4));
        }
        if (str.charAt(0) >= '0' && str.charAt(0) <= '9') {
            return new ASN1ObjectIdentifier(str);
        }
        ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) hashtable.get(Strings.toLowerCase(str));
        if (aSN1ObjectIdentifier != null) {
            return aSN1ObjectIdentifier;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown object id - ");
        stringBuilder.append(str);
        stringBuilder.append(" - passed to distinguished name");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static ASN1Primitive decodeObject(String str) {
        try {
            return ASN1Primitive.fromByteArray(Hex.decode(str.substring(1)));
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown encoding in name: ");
            stringBuilder.append(e);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public static String[] findAttrNamesForOID(ASN1ObjectIdentifier aSN1ObjectIdentifier, Hashtable hashtable) {
        Enumeration elements = hashtable.elements();
        int i = 0;
        int i2 = 0;
        while (elements.hasMoreElements()) {
            if (aSN1ObjectIdentifier.equals(elements.nextElement())) {
                i2++;
            }
        }
        String[] strArr = new String[i2];
        Enumeration keys = hashtable.keys();
        while (keys.hasMoreElements()) {
            String str = (String) keys.nextElement();
            if (aSN1ObjectIdentifier.equals(hashtable.get(str))) {
                int i3 = i + 1;
                strArr[i] = str;
                i = i3;
            }
        }
        return strArr;
    }

    private static boolean isHexDigit(char c) {
        return ('0' <= c && c <= '9') || (('a' <= c && c <= 'f') || ('A' <= c && c <= 'F'));
    }

    public static boolean rDNAreEqual(RDN rdn, RDN rdn2) {
        if (!rdn.isMultiValued()) {
            return !rdn2.isMultiValued() ? atvAreEqual(rdn.getFirst(), rdn2.getFirst()) : false;
        } else {
            if (!rdn2.isMultiValued()) {
                return false;
            }
            AttributeTypeAndValue[] typesAndValues = rdn.getTypesAndValues();
            AttributeTypeAndValue[] typesAndValues2 = rdn2.getTypesAndValues();
            if (typesAndValues.length != typesAndValues2.length) {
                return false;
            }
            for (int i = 0; i != typesAndValues.length; i++) {
                if (!atvAreEqual(typesAndValues[i], typesAndValues2[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    public static RDN[] rDNsFromString(String str, X500NameStyle x500NameStyle) {
        X500NameTokenizer x500NameTokenizer = new X500NameTokenizer(str);
        X500NameBuilder x500NameBuilder = new X500NameBuilder(x500NameStyle);
        while (x500NameTokenizer.hasMoreTokens()) {
            String nextToken = x500NameTokenizer.nextToken();
            if (nextToken.indexOf(43) > 0) {
                X500NameTokenizer x500NameTokenizer2 = new X500NameTokenizer(nextToken, '+');
                X500NameTokenizer x500NameTokenizer3 = new X500NameTokenizer(x500NameTokenizer2.nextToken(), '=');
                String nextToken2 = x500NameTokenizer3.nextToken();
                if (x500NameTokenizer3.hasMoreTokens()) {
                    nextToken = x500NameTokenizer3.nextToken();
                    ASN1ObjectIdentifier attrNameToOID = x500NameStyle.attrNameToOID(nextToken2.trim());
                    if (x500NameTokenizer2.hasMoreTokens()) {
                        Vector vector = new Vector();
                        Vector vector2 = new Vector();
                        while (true) {
                            Object attrNameToOID2;
                            vector.addElement(attrNameToOID2);
                            vector2.addElement(unescape(nextToken));
                            if (!x500NameTokenizer2.hasMoreTokens()) {
                                x500NameBuilder.addMultiValuedRDN(toOIDArray(vector), toValueArray(vector2));
                                break;
                            }
                            x500NameTokenizer3 = new X500NameTokenizer(x500NameTokenizer2.nextToken(), '=');
                            nextToken2 = x500NameTokenizer3.nextToken();
                            if (x500NameTokenizer3.hasMoreTokens()) {
                                nextToken = x500NameTokenizer3.nextToken();
                                attrNameToOID2 = x500NameStyle.attrNameToOID(nextToken2.trim());
                            } else {
                                throw new IllegalArgumentException("badly formatted directory string");
                            }
                        }
                    }
                    x500NameBuilder.addRDN(attrNameToOID2, unescape(nextToken));
                } else {
                    throw new IllegalArgumentException("badly formatted directory string");
                }
            }
            X500NameTokenizer x500NameTokenizer4 = new X500NameTokenizer(nextToken, '=');
            nextToken = x500NameTokenizer4.nextToken();
            if (x500NameTokenizer4.hasMoreTokens()) {
                x500NameBuilder.addRDN(x500NameStyle.attrNameToOID(nextToken.trim()), unescape(x500NameTokenizer4.nextToken()));
            } else {
                throw new IllegalArgumentException("badly formatted directory string");
            }
        }
        return x500NameBuilder.build().getRDNs();
    }

    public static String stripInternalSpaces(String str) {
        StringBuffer stringBuffer = new StringBuffer();
        if (str.length() != 0) {
            char charAt = str.charAt(0);
            stringBuffer.append(charAt);
            int i = 1;
            while (i < str.length()) {
                char charAt2 = str.charAt(i);
                if (charAt != ' ' || charAt2 != ' ') {
                    stringBuffer.append(charAt2);
                }
                i++;
                charAt = charAt2;
            }
        }
        return stringBuffer.toString();
    }

    private static ASN1ObjectIdentifier[] toOIDArray(Vector vector) {
        ASN1ObjectIdentifier[] aSN1ObjectIdentifierArr = new ASN1ObjectIdentifier[vector.size()];
        for (int i = 0; i != aSN1ObjectIdentifierArr.length; i++) {
            aSN1ObjectIdentifierArr[i] = (ASN1ObjectIdentifier) vector.elementAt(i);
        }
        return aSN1ObjectIdentifierArr;
    }

    private static String[] toValueArray(Vector vector) {
        String[] strArr = new String[vector.size()];
        for (int i = 0; i != strArr.length; i++) {
            strArr[i] = (String) vector.elementAt(i);
        }
        return strArr;
    }

    private static String unescape(String str) {
        if (str.length() == 0 || (str.indexOf(92) < 0 && str.indexOf(34) < 0)) {
            return str.trim();
        }
        int i;
        char[] toCharArray = str.toCharArray();
        StringBuffer stringBuffer = new StringBuffer(str.length());
        if (toCharArray[0] == '\\' && toCharArray[1] == '#') {
            i = 2;
            stringBuffer.append("\\#");
        } else {
            i = 0;
        }
        int i2 = 0;
        int i3 = i2;
        int i4 = i3;
        char c = i4;
        char c2 = c;
        for (i = 
/*
Method generation error in method: org.bouncycastle.asn1.x500.style.IETFUtils.unescape(java.lang.String):java.lang.String, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r4_4 'i' int) = (r4_2 'i' int), (r4_3 'i' int) binds: {(r4_2 'i' int)=B:10:0x0031, (r4_3 'i' int)=B:11:0x0038} in method: org.bouncycastle.asn1.x500.style.IETFUtils.unescape(java.lang.String):java.lang.String, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:183)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:61)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:173)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 19 more

*/

    public static ASN1Encodable valueFromHexString(String str, int i) throws IOException {
        byte[] bArr = new byte[((str.length() - i) / 2)];
        for (int i2 = 0; i2 != bArr.length; i2++) {
            int i3 = (i2 * 2) + i;
            char charAt = str.charAt(i3);
            int convertHex = convertHex(charAt) << 4;
            bArr[i2] = (byte) (convertHex(str.charAt(i3 + 1)) | convertHex);
        }
        return ASN1Primitive.fromByteArray(bArr);
    }

    public static String valueToString(ASN1Encodable aSN1Encodable) {
        StringBuffer stringBuffer = new StringBuffer();
        int i = 0;
        StringBuilder stringBuilder;
        if (!(aSN1Encodable instanceof ASN1String) || (aSN1Encodable instanceof DERUniversalString)) {
            try {
                stringBuilder = new StringBuilder();
                stringBuilder.append("#");
                stringBuilder.append(bytesToString(Hex.encode(aSN1Encodable.toASN1Primitive().getEncoded(ASN1Encoding.DER))));
                stringBuffer.append(stringBuilder.toString());
            } catch (IOException e) {
                throw new IllegalArgumentException("Other value has no encoded form");
            }
        }
        String string = ((ASN1String) aSN1Encodable).getString();
        if (string.length() > 0 && string.charAt(0) == '#') {
            stringBuilder = new StringBuilder();
            stringBuilder.append("\\");
            stringBuilder.append(string);
            string = stringBuilder.toString();
        }
        stringBuffer.append(string);
        int length = stringBuffer.length();
        int i2 = 2;
        if (!(stringBuffer.length() >= 2 && stringBuffer.charAt(0) == '\\' && stringBuffer.charAt(1) == '#')) {
            i2 = 0;
        }
        while (i2 != length) {
            if (stringBuffer.charAt(i2) == ',' || stringBuffer.charAt(i2) == '\"' || stringBuffer.charAt(i2) == '\\' || stringBuffer.charAt(i2) == '+' || stringBuffer.charAt(i2) == '=' || stringBuffer.charAt(i2) == '<' || stringBuffer.charAt(i2) == '>' || stringBuffer.charAt(i2) == ';') {
                stringBuffer.insert(i2, "\\");
                i2++;
                length++;
            }
            i2++;
        }
        if (stringBuffer.length() > 0) {
            while (stringBuffer.length() > i && stringBuffer.charAt(i) == ' ') {
                stringBuffer.insert(i, "\\");
                i += 2;
            }
        }
        length = stringBuffer.length() - 1;
        while (length >= 0 && stringBuffer.charAt(length) == ' ') {
            stringBuffer.insert(length, '\\');
            length--;
        }
        return stringBuffer.toString();
    }
}
