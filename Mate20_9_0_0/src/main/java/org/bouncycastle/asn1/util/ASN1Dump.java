package org.bouncycastle.asn1.util;

import java.io.IOException;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1ApplicationSpecific;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1UTCTime;
import org.bouncycastle.asn1.BERApplicationSpecific;
import org.bouncycastle.asn1.BEROctetString;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERApplicationSpecific;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERExternal;
import org.bouncycastle.asn1.DERGraphicString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERT61String;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DERVideotexString;
import org.bouncycastle.asn1.DERVisibleString;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

public class ASN1Dump {
    private static final int SAMPLE_SIZE = 32;
    private static final String TAB = "    ";

    static void _dumpAsString(String str, boolean z, ASN1Primitive aSN1Primitive, StringBuffer stringBuffer) {
        String lineSeparator = Strings.lineSeparator();
        Enumeration objects;
        StringBuilder stringBuilder;
        String stringBuilder2;
        Object nextElement;
        if (aSN1Primitive instanceof ASN1Sequence) {
            objects = ((ASN1Sequence) aSN1Primitive).getObjects();
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(TAB);
            stringBuilder2 = stringBuilder.toString();
            stringBuffer.append(str);
            str = aSN1Primitive instanceof BERSequence ? "BER Sequence" : aSN1Primitive instanceof DERSequence ? "DER Sequence" : "Sequence";
            loop1:
            while (true) {
                stringBuffer.append(str);
                stringBuffer.append(lineSeparator);
                while (objects.hasMoreElements()) {
                    nextElement = objects.nextElement();
                    if (nextElement == null || nextElement.equals(DERNull.INSTANCE)) {
                        stringBuffer.append(stringBuilder2);
                        str = "NULL";
                    } else {
                        _dumpAsString(stringBuilder2, z, nextElement instanceof ASN1Primitive ? (ASN1Primitive) nextElement : ((ASN1Encodable) nextElement).toASN1Primitive(), stringBuffer);
                    }
                }
                break loop1;
            }
        }
        StringBuilder stringBuilder3;
        String stringBuilder4;
        if (aSN1Primitive instanceof ASN1TaggedObject) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append(TAB);
            stringBuilder4 = stringBuilder3.toString();
            stringBuffer.append(str);
            stringBuffer.append(aSN1Primitive instanceof BERTaggedObject ? "BER Tagged [" : "Tagged [");
            ASN1TaggedObject aSN1TaggedObject = (ASN1TaggedObject) aSN1Primitive;
            stringBuffer.append(Integer.toString(aSN1TaggedObject.getTagNo()));
            stringBuffer.append(']');
            if (!aSN1TaggedObject.isExplicit()) {
                stringBuffer.append(" IMPLICIT ");
            }
            stringBuffer.append(lineSeparator);
            if (aSN1TaggedObject.isEmpty()) {
                stringBuffer.append(stringBuilder4);
                stringBuffer.append("EMPTY");
            } else {
                _dumpAsString(stringBuilder4, z, aSN1TaggedObject.getObject(), stringBuffer);
                return;
            }
        } else if (aSN1Primitive instanceof ASN1Set) {
            objects = ((ASN1Set) aSN1Primitive).getObjects();
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(TAB);
            stringBuilder2 = stringBuilder.toString();
            stringBuffer.append(str);
            str = aSN1Primitive instanceof BERSet ? "BER Set" : "DER Set";
            loop3:
            while (true) {
                stringBuffer.append(str);
                stringBuffer.append(lineSeparator);
                while (objects.hasMoreElements()) {
                    nextElement = objects.nextElement();
                    if (nextElement == null) {
                        stringBuffer.append(stringBuilder2);
                        str = "NULL";
                    } else {
                        _dumpAsString(stringBuilder2, z, nextElement instanceof ASN1Primitive ? (ASN1Primitive) nextElement : ((ASN1Encodable) nextElement).toASN1Primitive(), stringBuffer);
                    }
                }
                break loop3;
            }
        } else {
            byte[] octets;
            if (aSN1Primitive instanceof ASN1OctetString) {
                StringBuilder stringBuilder5;
                int length;
                ASN1OctetString aSN1OctetString = (ASN1OctetString) aSN1Primitive;
                if (aSN1Primitive instanceof BEROctetString) {
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(str);
                    stringBuilder5.append("BER Constructed Octet String[");
                    length = aSN1OctetString.getOctets().length;
                } else {
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(str);
                    stringBuilder5.append("DER Octet String[");
                    length = aSN1OctetString.getOctets().length;
                }
                stringBuilder5.append(length);
                stringBuilder5.append("] ");
                stringBuffer.append(stringBuilder5.toString());
                if (z) {
                    octets = aSN1OctetString.getOctets();
                }
            } else {
                StringBuilder stringBuilder6;
                if (aSN1Primitive instanceof ASN1ObjectIdentifier) {
                    stringBuilder6 = new StringBuilder();
                    stringBuilder6.append(str);
                    stringBuilder6.append("ObjectIdentifier(");
                    stringBuilder6.append(((ASN1ObjectIdentifier) aSN1Primitive).getId());
                } else if (aSN1Primitive instanceof ASN1Boolean) {
                    stringBuilder6 = new StringBuilder();
                    stringBuilder6.append(str);
                    stringBuilder6.append("Boolean(");
                    stringBuilder6.append(((ASN1Boolean) aSN1Primitive).isTrue());
                } else {
                    if (aSN1Primitive instanceof ASN1Integer) {
                        stringBuilder6 = new StringBuilder();
                        stringBuilder6.append(str);
                        stringBuilder6.append("Integer(");
                        nextElement = ((ASN1Integer) aSN1Primitive).getValue();
                    } else if (aSN1Primitive instanceof DERBitString) {
                        DERBitString dERBitString = (DERBitString) aSN1Primitive;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(str);
                        stringBuilder3.append("DER Bit String[");
                        stringBuilder3.append(dERBitString.getBytes().length);
                        stringBuilder3.append(", ");
                        stringBuilder3.append(dERBitString.getPadBits());
                        stringBuilder3.append("] ");
                        stringBuffer.append(stringBuilder3.toString());
                        if (z) {
                            octets = dERBitString.getBytes();
                        }
                    } else {
                        if (aSN1Primitive instanceof DERIA5String) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("IA5String(");
                            str = ((DERIA5String) aSN1Primitive).getString();
                        } else if (aSN1Primitive instanceof DERUTF8String) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("UTF8String(");
                            str = ((DERUTF8String) aSN1Primitive).getString();
                        } else if (aSN1Primitive instanceof DERPrintableString) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("PrintableString(");
                            str = ((DERPrintableString) aSN1Primitive).getString();
                        } else if (aSN1Primitive instanceof DERVisibleString) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("VisibleString(");
                            str = ((DERVisibleString) aSN1Primitive).getString();
                        } else if (aSN1Primitive instanceof DERBMPString) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("BMPString(");
                            str = ((DERBMPString) aSN1Primitive).getString();
                        } else if (aSN1Primitive instanceof DERT61String) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("T61String(");
                            str = ((DERT61String) aSN1Primitive).getString();
                        } else if (aSN1Primitive instanceof DERGraphicString) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("GraphicString(");
                            str = ((DERGraphicString) aSN1Primitive).getString();
                        } else if (aSN1Primitive instanceof DERVideotexString) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("VideotexString(");
                            str = ((DERVideotexString) aSN1Primitive).getString();
                        } else if (aSN1Primitive instanceof ASN1UTCTime) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("UTCTime(");
                            str = ((ASN1UTCTime) aSN1Primitive).getTime();
                        } else if (aSN1Primitive instanceof ASN1GeneralizedTime) {
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append(str);
                            stringBuilder6.append("GeneralizedTime(");
                            str = ((ASN1GeneralizedTime) aSN1Primitive).getTime();
                        } else {
                            if (aSN1Primitive instanceof BERApplicationSpecific) {
                                stringBuilder4 = ASN1Encoding.BER;
                            } else if (aSN1Primitive instanceof DERApplicationSpecific) {
                                stringBuilder4 = ASN1Encoding.DER;
                            } else if (aSN1Primitive instanceof ASN1Enumerated) {
                                ASN1Enumerated aSN1Enumerated = (ASN1Enumerated) aSN1Primitive;
                                stringBuilder6 = new StringBuilder();
                                stringBuilder6.append(str);
                                stringBuilder6.append("DER Enumerated(");
                                nextElement = aSN1Enumerated.getValue();
                            } else if (aSN1Primitive instanceof DERExternal) {
                                DERExternal dERExternal = (DERExternal) aSN1Primitive;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(str);
                                stringBuilder3.append("External ");
                                stringBuilder3.append(lineSeparator);
                                stringBuffer.append(stringBuilder3.toString());
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(str);
                                stringBuilder3.append(TAB);
                                str = stringBuilder3.toString();
                                if (dERExternal.getDirectReference() != null) {
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append(str);
                                    stringBuilder3.append("Direct Reference: ");
                                    stringBuilder3.append(dERExternal.getDirectReference().getId());
                                    stringBuilder3.append(lineSeparator);
                                    stringBuffer.append(stringBuilder3.toString());
                                }
                                if (dERExternal.getIndirectReference() != null) {
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append(str);
                                    stringBuilder3.append("Indirect Reference: ");
                                    stringBuilder3.append(dERExternal.getIndirectReference().toString());
                                    stringBuilder3.append(lineSeparator);
                                    stringBuffer.append(stringBuilder3.toString());
                                }
                                if (dERExternal.getDataValueDescriptor() != null) {
                                    _dumpAsString(str, z, dERExternal.getDataValueDescriptor(), stringBuffer);
                                }
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(str);
                                stringBuilder3.append("Encoding: ");
                                stringBuilder3.append(dERExternal.getEncoding());
                                stringBuilder3.append(lineSeparator);
                                stringBuffer.append(stringBuilder3.toString());
                                _dumpAsString(str, z, dERExternal.getExternalContent(), stringBuffer);
                                return;
                            } else {
                                stringBuilder6 = new StringBuilder();
                                stringBuilder6.append(str);
                                str = aSN1Primitive.toString();
                                stringBuilder6.append(str);
                                stringBuilder6.append(lineSeparator);
                                str = stringBuilder6.toString();
                                stringBuffer.append(str);
                                return;
                            }
                            str = outputApplicationSpecific(stringBuilder4, str, z, aSN1Primitive, lineSeparator);
                            stringBuffer.append(str);
                            return;
                        }
                        stringBuilder6.append(str);
                        str = ") ";
                        stringBuilder6.append(str);
                        stringBuilder6.append(lineSeparator);
                        str = stringBuilder6.toString();
                        stringBuffer.append(str);
                        return;
                    }
                    stringBuilder6.append(nextElement);
                }
                str = ")";
                stringBuilder6.append(str);
                stringBuilder6.append(lineSeparator);
                str = stringBuilder6.toString();
                stringBuffer.append(str);
                return;
            }
            str = dumpBinaryDataAsString(str, octets);
            stringBuffer.append(str);
            return;
        }
        stringBuffer.append(lineSeparator);
    }

    private static String calculateAscString(byte[] bArr, int i, int i2) {
        StringBuffer stringBuffer = new StringBuffer();
        int i3 = i;
        while (i3 != i + i2) {
            if (bArr[i3] >= (byte) 32 && bArr[i3] <= (byte) 126) {
                stringBuffer.append((char) bArr[i3]);
            }
            i3++;
        }
        return stringBuffer.toString();
    }

    public static String dumpAsString(Object obj) {
        return dumpAsString(obj, false);
    }

    public static String dumpAsString(Object obj, boolean z) {
        String str;
        ASN1Primitive aSN1Primitive;
        StringBuffer stringBuffer = new StringBuffer();
        if (obj instanceof ASN1Primitive) {
            str = "";
            aSN1Primitive = (ASN1Primitive) obj;
        } else if (obj instanceof ASN1Encodable) {
            str = "";
            aSN1Primitive = ((ASN1Encodable) obj).toASN1Primitive();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown object type ");
            stringBuilder.append(obj.toString());
            return stringBuilder.toString();
        }
        _dumpAsString(str, z, aSN1Primitive, stringBuffer);
        return stringBuffer.toString();
    }

    private static String dumpBinaryDataAsString(String str, byte[] bArr) {
        String lineSeparator = Strings.lineSeparator();
        StringBuffer stringBuffer = new StringBuffer();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(TAB);
        str = stringBuilder.toString();
        stringBuffer.append(lineSeparator);
        for (int i = 0; i < bArr.length; i += 32) {
            String calculateAscString;
            if (bArr.length - i > 32) {
                stringBuffer.append(str);
                stringBuffer.append(Strings.fromByteArray(Hex.encode(bArr, i, 32)));
                stringBuffer.append(TAB);
                calculateAscString = calculateAscString(bArr, i, 32);
            } else {
                stringBuffer.append(str);
                stringBuffer.append(Strings.fromByteArray(Hex.encode(bArr, i, bArr.length - i)));
                for (int length = bArr.length - i; length != 32; length++) {
                    stringBuffer.append("  ");
                }
                stringBuffer.append(TAB);
                calculateAscString = calculateAscString(bArr, i, bArr.length - i);
            }
            stringBuffer.append(calculateAscString);
            stringBuffer.append(lineSeparator);
        }
        return stringBuffer.toString();
    }

    private static String outputApplicationSpecific(String str, String str2, boolean z, ASN1Primitive aSN1Primitive, String str3) {
        ASN1ApplicationSpecific instance = ASN1ApplicationSpecific.getInstance(aSN1Primitive);
        StringBuffer stringBuffer = new StringBuffer();
        if (instance.isConstructed()) {
            try {
                ASN1Sequence instance2 = ASN1Sequence.getInstance(instance.getObject(16));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str2);
                stringBuilder.append(str);
                stringBuilder.append(" ApplicationSpecific[");
                stringBuilder.append(instance.getApplicationTag());
                stringBuilder.append("]");
                stringBuilder.append(str3);
                stringBuffer.append(stringBuilder.toString());
                Enumeration objects = instance2.getObjects();
                while (objects.hasMoreElements()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(str2);
                    stringBuilder2.append(TAB);
                    _dumpAsString(stringBuilder2.toString(), z, (ASN1Primitive) objects.nextElement(), stringBuffer);
                }
            } catch (IOException e) {
                stringBuffer.append(e);
            }
            return stringBuffer.toString();
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(str2);
        stringBuilder3.append(str);
        stringBuilder3.append(" ApplicationSpecific[");
        stringBuilder3.append(instance.getApplicationTag());
        stringBuilder3.append("] (");
        stringBuilder3.append(Strings.fromByteArray(Hex.encode(instance.getContents())));
        stringBuilder3.append(")");
        stringBuilder3.append(str3);
        return stringBuilder3.toString();
    }
}
