package com.android.org.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

public class BERConstructedOctetString extends BEROctetString {
    private static final int MAX_LENGTH = 1000;
    private Vector octs;

    private static byte[] toBytes(Vector octs) {
        StringBuilder stringBuilder;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        int i = 0;
        while (i != octs.size()) {
            try {
                bOut.write(((DEROctetString) octs.elementAt(i)).getOctets());
                i++;
            } catch (ClassCastException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(octs.elementAt(i).getClass().getName());
                stringBuilder.append(" found in input should only contain DEROctetString");
                throw new IllegalArgumentException(stringBuilder.toString());
            } catch (IOException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception converting octets ");
                stringBuilder.append(e2.toString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return bOut.toByteArray();
    }

    public BERConstructedOctetString(byte[] string) {
        super(string);
    }

    public BERConstructedOctetString(Vector octs) {
        super(toBytes(octs));
        this.octs = octs;
    }

    public BERConstructedOctetString(ASN1Primitive obj) {
        super(toByteArray(obj));
    }

    private static byte[] toByteArray(ASN1Primitive obj) {
        try {
            return obj.getEncoded();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to encode object");
        }
    }

    public BERConstructedOctetString(ASN1Encodable obj) {
        this(obj.toASN1Primitive());
    }

    public byte[] getOctets() {
        return this.string;
    }

    public Enumeration getObjects() {
        if (this.octs == null) {
            return generateOcts().elements();
        }
        return this.octs.elements();
    }

    private Vector generateOcts() {
        Vector vec = new Vector();
        for (int i = 0; i < this.string.length; i += MAX_LENGTH) {
            int end;
            if (i + MAX_LENGTH > this.string.length) {
                end = this.string.length;
            } else {
                end = i + MAX_LENGTH;
            }
            byte[] nStr = new byte[(end - i)];
            System.arraycopy(this.string, i, nStr, 0, nStr.length);
            vec.addElement(new DEROctetString(nStr));
        }
        return vec;
    }

    public static BEROctetString fromSequence(ASN1Sequence seq) {
        Vector v = new Vector();
        Enumeration e = seq.getObjects();
        while (e.hasMoreElements()) {
            v.addElement(e.nextElement());
        }
        return new BERConstructedOctetString(v);
    }
}
