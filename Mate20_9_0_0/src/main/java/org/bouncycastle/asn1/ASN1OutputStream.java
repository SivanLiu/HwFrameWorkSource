package org.bouncycastle.asn1;

import java.io.IOException;
import java.io.OutputStream;
import org.bouncycastle.asn1.eac.CertificateBody;

public class ASN1OutputStream {
    private OutputStream os;

    private class ImplicitOutputStream extends ASN1OutputStream {
        private boolean first = true;

        public ImplicitOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        public void write(int i) throws IOException {
            if (this.first) {
                this.first = false;
            } else {
                super.write(i);
            }
        }
    }

    public ASN1OutputStream(OutputStream outputStream) {
        this.os = outputStream;
    }

    public void close() throws IOException {
        this.os.close();
    }

    public void flush() throws IOException {
        this.os.flush();
    }

    ASN1OutputStream getDERSubStream() {
        return new DEROutputStream(this.os);
    }

    ASN1OutputStream getDLSubStream() {
        return new DLOutputStream(this.os);
    }

    void write(int i) throws IOException {
        this.os.write(i);
    }

    void write(byte[] bArr) throws IOException {
        this.os.write(bArr);
    }

    void write(byte[] bArr, int i, int i2) throws IOException {
        this.os.write(bArr, i, i2);
    }

    void writeEncoded(int i, int i2, byte[] bArr) throws IOException {
        writeTag(i, i2);
        writeLength(bArr.length);
        write(bArr);
    }

    void writeEncoded(int i, byte[] bArr) throws IOException {
        write(i);
        writeLength(bArr.length);
        write(bArr);
    }

    void writeImplicitObject(ASN1Primitive aSN1Primitive) throws IOException {
        if (aSN1Primitive != null) {
            aSN1Primitive.encode(new ImplicitOutputStream(this.os));
            return;
        }
        throw new IOException("null object detected");
    }

    void writeLength(int i) throws IOException {
        if (i > CertificateBody.profileType) {
            int i2 = i;
            int i3 = 1;
            while (true) {
                i2 >>>= 8;
                if (i2 == 0) {
                    break;
                }
                i3++;
            }
            write((byte) (i3 | 128));
            for (i3 = (i3 - 1) * 8; i3 >= 0; i3 -= 8) {
                write((byte) (i >> i3));
            }
            return;
        }
        write((byte) i);
    }

    protected void writeNull() throws IOException {
        this.os.write(5);
        this.os.write(0);
    }

    public void writeObject(ASN1Encodable aSN1Encodable) throws IOException {
        if (aSN1Encodable != null) {
            aSN1Encodable.toASN1Primitive().encode(this);
            return;
        }
        throw new IOException("null object detected");
    }

    void writeTag(int i, int i2) throws IOException {
        if (i2 < 31) {
            write(i | i2);
            return;
        }
        write(i | 31);
        if (i2 < 128) {
            write(i2);
            return;
        }
        byte[] bArr = new byte[5];
        int length = bArr.length - 1;
        bArr[length] = (byte) (i2 & CertificateBody.profileType);
        do {
            i2 >>= 7;
            length--;
            bArr[length] = (byte) ((i2 & CertificateBody.profileType) | 128);
        } while (i2 > CertificateBody.profileType);
        write(bArr, length, bArr.length - length);
    }
}
