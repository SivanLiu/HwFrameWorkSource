package org.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.bouncycastle.util.encoders.Hex;

public class DERApplicationSpecific extends ASN1ApplicationSpecific {
    public DERApplicationSpecific(int i, ASN1Encodable aSN1Encodable) throws IOException {
        this(true, i, aSN1Encodable);
    }

    public DERApplicationSpecific(int i, ASN1EncodableVector aSN1EncodableVector) {
        super(true, i, getEncodedVector(aSN1EncodableVector));
    }

    public DERApplicationSpecific(int i, byte[] bArr) {
        this(false, i, bArr);
    }

    public DERApplicationSpecific(boolean z, int i, ASN1Encodable aSN1Encodable) throws IOException {
        boolean z2 = z || aSN1Encodable.toASN1Primitive().isConstructed();
        super(z2, i, getEncoding(z, aSN1Encodable));
    }

    DERApplicationSpecific(boolean z, int i, byte[] bArr) {
        super(z, i, bArr);
    }

    private static byte[] getEncodedVector(ASN1EncodableVector aSN1EncodableVector) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i = 0;
        while (i != aSN1EncodableVector.size()) {
            try {
                byteArrayOutputStream.write(((ASN1Object) aSN1EncodableVector.get(i)).getEncoded(ASN1Encoding.DER));
                i++;
            } catch (Throwable e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("malformed object: ");
                stringBuilder.append(e);
                throw new ASN1ParsingException(stringBuilder.toString(), e);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] getEncoding(boolean z, ASN1Encodable aSN1Encodable) throws IOException {
        Object encoded = aSN1Encodable.toASN1Primitive().getEncoded(ASN1Encoding.DER);
        if (z) {
            return encoded;
        }
        int lengthOfHeader = ASN1ApplicationSpecific.getLengthOfHeader(encoded);
        Object obj = new byte[(encoded.length - lengthOfHeader)];
        System.arraycopy(encoded, lengthOfHeader, obj, 0, obj.length);
        return obj;
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(this.isConstructed ? 96 : 64, this.tag, this.octets);
    }

    public String toString() {
        String toHexString;
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("[");
        if (isConstructed()) {
            stringBuffer.append("CONSTRUCTED ");
        }
        stringBuffer.append("APPLICATION ");
        stringBuffer.append(Integer.toString(getApplicationTag()));
        stringBuffer.append("]");
        if (this.octets != null) {
            stringBuffer.append(" #");
            toHexString = Hex.toHexString(this.octets);
        } else {
            toHexString = " #null";
        }
        stringBuffer.append(toHexString);
        stringBuffer.append(" ");
        return stringBuffer.toString();
    }
}
