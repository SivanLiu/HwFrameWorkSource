package com.android.org.bouncycastle.asn1.util;

import com.android.org.bouncycastle.asn1.ASN1ApplicationSpecific;
import com.android.org.bouncycastle.asn1.ASN1Boolean;
import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1Enumerated;
import com.android.org.bouncycastle.asn1.ASN1GeneralizedTime;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1Set;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.ASN1UTCTime;
import com.android.org.bouncycastle.asn1.BERApplicationSpecific;
import com.android.org.bouncycastle.asn1.BEROctetString;
import com.android.org.bouncycastle.asn1.BERSequence;
import com.android.org.bouncycastle.asn1.BERSet;
import com.android.org.bouncycastle.asn1.BERTaggedObject;
import com.android.org.bouncycastle.asn1.DERApplicationSpecific;
import com.android.org.bouncycastle.asn1.DERBMPString;
import com.android.org.bouncycastle.asn1.DERBitString;
import com.android.org.bouncycastle.asn1.DERExternal;
import com.android.org.bouncycastle.asn1.DERGraphicString;
import com.android.org.bouncycastle.asn1.DERIA5String;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.DERPrintableString;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERT61String;
import com.android.org.bouncycastle.asn1.DERUTF8String;
import com.android.org.bouncycastle.asn1.DERVideotexString;
import com.android.org.bouncycastle.asn1.DERVisibleString;
import com.android.org.bouncycastle.util.Strings;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.io.IOException;
import java.util.Enumeration;

public class ASN1Dump {
    private static final int SAMPLE_SIZE = 32;
    private static final String TAB = "    ";

    static void _dumpAsString(String indent, boolean verbose, ASN1Primitive obj, StringBuffer buf) {
        String nl = Strings.lineSeparator();
        Enumeration e;
        String tab;
        Object o;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        if (obj instanceof ASN1Sequence) {
            e = ((ASN1Sequence) obj).getObjects();
            tab = new StringBuilder();
            tab.append(indent);
            tab.append(TAB);
            tab = tab.toString();
            buf.append(indent);
            if (obj instanceof BERSequence) {
                buf.append("BER Sequence");
            } else if (obj instanceof DERSequence) {
                buf.append("DER Sequence");
            } else {
                buf.append("Sequence");
            }
            buf.append(nl);
            while (e.hasMoreElements()) {
                o = e.nextElement();
                if (o == null || o.equals(DERNull.INSTANCE)) {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(nl);
                } else if (o instanceof ASN1Primitive) {
                    _dumpAsString(tab, verbose, (ASN1Primitive) o, buf);
                } else {
                    _dumpAsString(tab, verbose, ((ASN1Encodable) o).toASN1Primitive(), buf);
                }
            }
        } else if (obj instanceof ASN1TaggedObject) {
            String tab2 = new StringBuilder();
            tab2.append(indent);
            tab2.append(TAB);
            tab2 = tab2.toString();
            buf.append(indent);
            if (obj instanceof BERTaggedObject) {
                buf.append("BER Tagged [");
            } else {
                buf.append("Tagged [");
            }
            ASN1TaggedObject o2 = (ASN1TaggedObject) obj;
            buf.append(Integer.toString(o2.getTagNo()));
            buf.append(']');
            if (!o2.isExplicit()) {
                buf.append(" IMPLICIT ");
            }
            buf.append(nl);
            if (o2.isEmpty()) {
                buf.append(tab2);
                buf.append("EMPTY");
                buf.append(nl);
                return;
            }
            _dumpAsString(tab2, verbose, o2.getObject(), buf);
        } else if (obj instanceof ASN1Set) {
            e = ((ASN1Set) obj).getObjects();
            tab = new StringBuilder();
            tab.append(indent);
            tab.append(TAB);
            tab = tab.toString();
            buf.append(indent);
            if (obj instanceof BERSet) {
                buf.append("BER Set");
            } else {
                buf.append("DER Set");
            }
            buf.append(nl);
            while (e.hasMoreElements()) {
                o = e.nextElement();
                if (o == null) {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(nl);
                } else if (o instanceof ASN1Primitive) {
                    _dumpAsString(tab, verbose, (ASN1Primitive) o, buf);
                } else {
                    _dumpAsString(tab, verbose, ((ASN1Encodable) o).toASN1Primitive(), buf);
                }
            }
        } else if (obj instanceof ASN1OctetString) {
            ASN1OctetString oct = (ASN1OctetString) obj;
            if (obj instanceof BEROctetString) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(indent);
                stringBuilder.append("BER Constructed Octet String[");
                stringBuilder.append(oct.getOctets().length);
                stringBuilder.append("] ");
                buf.append(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(indent);
                stringBuilder.append("DER Octet String[");
                stringBuilder.append(oct.getOctets().length);
                stringBuilder.append("] ");
                buf.append(stringBuilder.toString());
            }
            if (verbose) {
                buf.append(dumpBinaryDataAsString(indent, oct.getOctets()));
            } else {
                buf.append(nl);
            }
        } else if (obj instanceof ASN1ObjectIdentifier) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("ObjectIdentifier(");
            stringBuilder2.append(((ASN1ObjectIdentifier) obj).getId());
            stringBuilder2.append(")");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof ASN1Boolean) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("Boolean(");
            stringBuilder2.append(((ASN1Boolean) obj).isTrue());
            stringBuilder2.append(")");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof ASN1Integer) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("Integer(");
            stringBuilder2.append(((ASN1Integer) obj).getValue());
            stringBuilder2.append(")");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof DERBitString) {
            DERBitString bt = (DERBitString) obj;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("DER Bit String[");
            stringBuilder.append(bt.getBytes().length);
            stringBuilder.append(", ");
            stringBuilder.append(bt.getPadBits());
            stringBuilder.append("] ");
            buf.append(stringBuilder.toString());
            if (verbose) {
                buf.append(dumpBinaryDataAsString(indent, bt.getBytes()));
            } else {
                buf.append(nl);
            }
        } else if (obj instanceof DERIA5String) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("IA5String(");
            stringBuilder2.append(((DERIA5String) obj).getString());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof DERUTF8String) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("UTF8String(");
            stringBuilder2.append(((DERUTF8String) obj).getString());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof DERPrintableString) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("PrintableString(");
            stringBuilder2.append(((DERPrintableString) obj).getString());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof DERVisibleString) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("VisibleString(");
            stringBuilder2.append(((DERVisibleString) obj).getString());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof DERBMPString) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("BMPString(");
            stringBuilder2.append(((DERBMPString) obj).getString());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof DERT61String) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("T61String(");
            stringBuilder2.append(((DERT61String) obj).getString());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof DERGraphicString) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("GraphicString(");
            stringBuilder2.append(((DERGraphicString) obj).getString());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof DERVideotexString) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("VideotexString(");
            stringBuilder2.append(((DERVideotexString) obj).getString());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof ASN1UTCTime) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("UTCTime(");
            stringBuilder2.append(((ASN1UTCTime) obj).getTime());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof ASN1GeneralizedTime) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("GeneralizedTime(");
            stringBuilder2.append(((ASN1GeneralizedTime) obj).getTime());
            stringBuilder2.append(") ");
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        } else if (obj instanceof BERApplicationSpecific) {
            buf.append(outputApplicationSpecific(ASN1Encoding.BER, indent, verbose, obj, nl));
        } else if (obj instanceof DERApplicationSpecific) {
            buf.append(outputApplicationSpecific(ASN1Encoding.DER, indent, verbose, obj, nl));
        } else if (obj instanceof ASN1Enumerated) {
            ASN1Enumerated en = (ASN1Enumerated) obj;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("DER Enumerated(");
            stringBuilder.append(en.getValue());
            stringBuilder.append(")");
            stringBuilder.append(nl);
            buf.append(stringBuilder.toString());
        } else if (obj instanceof DERExternal) {
            StringBuilder stringBuilder3;
            DERExternal ext = (DERExternal) obj;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("External ");
            stringBuilder.append(nl);
            buf.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(TAB);
            tab = stringBuilder.toString();
            if (ext.getDirectReference() != null) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(tab);
                stringBuilder3.append("Direct Reference: ");
                stringBuilder3.append(ext.getDirectReference().getId());
                stringBuilder3.append(nl);
                buf.append(stringBuilder3.toString());
            }
            if (ext.getIndirectReference() != null) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(tab);
                stringBuilder3.append("Indirect Reference: ");
                stringBuilder3.append(ext.getIndirectReference().toString());
                stringBuilder3.append(nl);
                buf.append(stringBuilder3.toString());
            }
            if (ext.getDataValueDescriptor() != null) {
                _dumpAsString(tab, verbose, ext.getDataValueDescriptor(), buf);
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(tab);
            stringBuilder3.append("Encoding: ");
            stringBuilder3.append(ext.getEncoding());
            stringBuilder3.append(nl);
            buf.append(stringBuilder3.toString());
            _dumpAsString(tab, verbose, ext.getExternalContent(), buf);
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append(obj.toString());
            stringBuilder2.append(nl);
            buf.append(stringBuilder2.toString());
        }
    }

    private static String outputApplicationSpecific(String type, String indent, boolean verbose, ASN1Primitive obj, String nl) {
        ASN1ApplicationSpecific app = ASN1ApplicationSpecific.getInstance(obj);
        StringBuffer buf = new StringBuffer();
        if (app.isConstructed()) {
            try {
                ASN1Sequence s = ASN1Sequence.getInstance(app.getObject(16));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(indent);
                stringBuilder.append(type);
                stringBuilder.append(" ApplicationSpecific[");
                stringBuilder.append(app.getApplicationTag());
                stringBuilder.append("]");
                stringBuilder.append(nl);
                buf.append(stringBuilder.toString());
                Enumeration e = s.getObjects();
                while (e.hasMoreElements()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(indent);
                    stringBuilder2.append(TAB);
                    _dumpAsString(stringBuilder2.toString(), verbose, (ASN1Primitive) e.nextElement(), buf);
                }
            } catch (IOException e2) {
                buf.append(e2);
            }
            return buf.toString();
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(indent);
        stringBuilder3.append(type);
        stringBuilder3.append(" ApplicationSpecific[");
        stringBuilder3.append(app.getApplicationTag());
        stringBuilder3.append("] (");
        stringBuilder3.append(Strings.fromByteArray(Hex.encode(app.getContents())));
        stringBuilder3.append(")");
        stringBuilder3.append(nl);
        return stringBuilder3.toString();
    }

    public static String dumpAsString(Object obj) {
        return dumpAsString(obj, false);
    }

    public static String dumpAsString(Object obj, boolean verbose) {
        StringBuffer buf = new StringBuffer();
        if (obj instanceof ASN1Primitive) {
            _dumpAsString("", verbose, (ASN1Primitive) obj, buf);
        } else if (obj instanceof ASN1Encodable) {
            _dumpAsString("", verbose, ((ASN1Encodable) obj).toASN1Primitive(), buf);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown object type ");
            stringBuilder.append(obj.toString());
            return stringBuilder.toString();
        }
        return buf.toString();
    }

    private static String dumpBinaryDataAsString(String indent, byte[] bytes) {
        String nl = Strings.lineSeparator();
        StringBuffer buf = new StringBuffer();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(indent);
        stringBuilder.append(TAB);
        indent = stringBuilder.toString();
        buf.append(nl);
        for (int i = 0; i < bytes.length; i += 32) {
            if (bytes.length - i > 32) {
                buf.append(indent);
                buf.append(Strings.fromByteArray(Hex.encode(bytes, i, 32)));
                buf.append(TAB);
                buf.append(calculateAscString(bytes, i, 32));
                buf.append(nl);
            } else {
                buf.append(indent);
                buf.append(Strings.fromByteArray(Hex.encode(bytes, i, bytes.length - i)));
                for (int j = bytes.length - i; j != 32; j++) {
                    buf.append("  ");
                }
                buf.append(TAB);
                buf.append(calculateAscString(bytes, i, bytes.length - i));
                buf.append(nl);
            }
        }
        return buf.toString();
    }

    private static String calculateAscString(byte[] bytes, int off, int len) {
        StringBuffer buf = new StringBuffer();
        int i = off;
        while (i != off + len) {
            if (bytes[i] >= (byte) 32 && bytes[i] <= (byte) 126) {
                buf.append((char) bytes[i]);
            }
            i++;
        }
        return buf.toString();
    }
}
