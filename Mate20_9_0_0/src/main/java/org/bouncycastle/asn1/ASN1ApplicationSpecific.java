package org.bouncycastle.asn1;

import java.io.IOException;
import org.bouncycastle.asn1.eac.CertificateBody;
import org.bouncycastle.util.Arrays;

public abstract class ASN1ApplicationSpecific extends ASN1Primitive {
    protected final boolean isConstructed;
    protected final byte[] octets;
    protected final int tag;

    ASN1ApplicationSpecific(boolean z, int i, byte[] bArr) {
        this.isConstructed = z;
        this.tag = i;
        this.octets = Arrays.clone(bArr);
    }

    public static ASN1ApplicationSpecific getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1ApplicationSpecific)) {
            return (ASN1ApplicationSpecific) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to construct object from byte[]: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("unknown object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    protected static int getLengthOfHeader(byte[] bArr) {
        int i = bArr[1] & 255;
        if (i == 128 || i <= CertificateBody.profileType) {
            return 2;
        }
        i &= CertificateBody.profileType;
        if (i <= 4) {
            return i + 2;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DER length more than 4 bytes: ");
        stringBuilder.append(i);
        throw new IllegalStateException(stringBuilder.toString());
    }

    private byte[] replaceTagNumber(int i, byte[] bArr) throws IOException {
        int i2;
        if ((bArr[0] & 31) == 31) {
            i2 = 2;
            int i3 = bArr[1] & 255;
            if ((i3 & CertificateBody.profileType) != 0) {
                while (i3 >= 0 && (i3 & 128) != 0) {
                    int i4 = i2 + 1;
                    i3 = bArr[i2] & 255;
                    i2 = i4;
                }
            } else {
                throw new ASN1ParsingException("corrupted stream - invalid high tag number found");
            }
        }
        i2 = 1;
        byte[] bArr2 = new byte[((bArr.length - i2) + 1)];
        System.arraycopy(bArr, i2, bArr2, 1, bArr2.length - 1);
        bArr2[0] = (byte) i;
        return bArr2;
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        boolean z = false;
        if (!(aSN1Primitive instanceof ASN1ApplicationSpecific)) {
            return false;
        }
        ASN1ApplicationSpecific aSN1ApplicationSpecific = (ASN1ApplicationSpecific) aSN1Primitive;
        if (this.isConstructed == aSN1ApplicationSpecific.isConstructed && this.tag == aSN1ApplicationSpecific.tag && Arrays.areEqual(this.octets, aSN1ApplicationSpecific.octets)) {
            z = true;
        }
        return z;
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(this.isConstructed ? 96 : 64, this.tag, this.octets);
    }

    int encodedLength() throws IOException {
        return (StreamUtil.calculateTagLength(this.tag) + StreamUtil.calculateBodyLength(this.octets.length)) + this.octets.length;
    }

    public int getApplicationTag() {
        return this.tag;
    }

    public byte[] getContents() {
        return Arrays.clone(this.octets);
    }

    public ASN1Primitive getObject() throws IOException {
        return ASN1Primitive.fromByteArray(getContents());
    }

    public ASN1Primitive getObject(int i) throws IOException {
        if (i < 31) {
            byte[] encoded = getEncoded();
            byte[] replaceTagNumber = replaceTagNumber(i, encoded);
            if ((encoded[0] & 32) != 0) {
                replaceTagNumber[0] = (byte) (replaceTagNumber[0] | 32);
            }
            return ASN1Primitive.fromByteArray(replaceTagNumber);
        }
        throw new IOException("unsupported tag number");
    }

    public int hashCode() {
        return (this.isConstructed ^ this.tag) ^ Arrays.hashCode(this.octets);
    }

    public boolean isConstructed() {
        return this.isConstructed;
    }
}
