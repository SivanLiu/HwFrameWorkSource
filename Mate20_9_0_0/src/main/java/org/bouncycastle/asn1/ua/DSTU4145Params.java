package org.bouncycastle.asn1.ua;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.Arrays;

public class DSTU4145Params extends ASN1Object {
    private static final byte[] DEFAULT_DKE = new byte[]{(byte) -87, (byte) -42, (byte) -21, (byte) 69, (byte) -15, (byte) 60, (byte) 112, (byte) -126, Byte.MIN_VALUE, (byte) -60, (byte) -106, (byte) 123, (byte) 35, (byte) 31, (byte) 94, (byte) -83, (byte) -10, (byte) 88, (byte) -21, (byte) -92, (byte) -64, (byte) 55, (byte) 41, (byte) 29, (byte) 56, (byte) -39, (byte) 107, (byte) -16, (byte) 37, (byte) -54, (byte) 78, (byte) 23, (byte) -8, (byte) -23, (byte) 114, (byte) 13, (byte) -58, (byte) 21, (byte) -76, (byte) 58, (byte) 40, (byte) -105, (byte) 95, (byte) 11, (byte) -63, (byte) -34, (byte) -93, (byte) 100, (byte) 56, (byte) -75, (byte) 100, (byte) -22, (byte) 44, (byte) 23, (byte) -97, (byte) -48, (byte) 18, (byte) 62, (byte) 109, (byte) -72, (byte) -6, (byte) -59, (byte) 121, (byte) 4};
    private byte[] dke = DEFAULT_DKE;
    private DSTU4145ECBinary ecbinary;
    private ASN1ObjectIdentifier namedCurve;

    public DSTU4145Params(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        this.namedCurve = aSN1ObjectIdentifier;
    }

    public DSTU4145Params(ASN1ObjectIdentifier aSN1ObjectIdentifier, byte[] bArr) {
        this.namedCurve = aSN1ObjectIdentifier;
        this.dke = Arrays.clone(bArr);
    }

    public DSTU4145Params(DSTU4145ECBinary dSTU4145ECBinary) {
        this.ecbinary = dSTU4145ECBinary;
    }

    public static byte[] getDefaultDKE() {
        return DEFAULT_DKE;
    }

    public static DSTU4145Params getInstance(Object obj) {
        if (obj instanceof DSTU4145Params) {
            return (DSTU4145Params) obj;
        }
        if (obj != null) {
            ASN1Sequence instance = ASN1Sequence.getInstance(obj);
            DSTU4145Params dSTU4145Params = instance.getObjectAt(0) instanceof ASN1ObjectIdentifier ? new DSTU4145Params(ASN1ObjectIdentifier.getInstance(instance.getObjectAt(0))) : new DSTU4145Params(DSTU4145ECBinary.getInstance(instance.getObjectAt(0)));
            if (instance.size() != 2) {
                return dSTU4145Params;
            }
            dSTU4145Params.dke = ASN1OctetString.getInstance(instance.getObjectAt(1)).getOctets();
            if (dSTU4145Params.dke.length == DEFAULT_DKE.length) {
                return dSTU4145Params;
            }
            throw new IllegalArgumentException("object parse error");
        }
        throw new IllegalArgumentException("object parse error");
    }

    public byte[] getDKE() {
        return this.dke;
    }

    public DSTU4145ECBinary getECBinary() {
        return this.ecbinary;
    }

    public ASN1ObjectIdentifier getNamedCurve() {
        return this.namedCurve;
    }

    public boolean isNamedCurve() {
        return this.namedCurve != null;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.namedCurve != null ? this.namedCurve : this.ecbinary);
        if (!Arrays.areEqual(this.dke, DEFAULT_DKE)) {
            aSN1EncodableVector.add(new DEROctetString(this.dke));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
