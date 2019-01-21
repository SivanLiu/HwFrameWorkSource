package org.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.bouncycastle.asn1.eac.CertificateBody;

public abstract class DERGenerator extends ASN1Generator {
    private boolean _isExplicit;
    private int _tagNo;
    private boolean _tagged;

    protected DERGenerator(OutputStream outputStream) {
        super(outputStream);
        this._tagged = false;
    }

    public DERGenerator(OutputStream outputStream, int i, boolean z) {
        super(outputStream);
        this._tagged = false;
        this._tagged = true;
        this._isExplicit = z;
        this._tagNo = i;
    }

    private void writeLength(OutputStream outputStream, int i) throws IOException {
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
            outputStream.write((byte) (i3 | 128));
            for (i3 = (i3 - 1) * 8; i3 >= 0; i3 -= 8) {
                outputStream.write((byte) (i >> i3));
            }
            return;
        }
        outputStream.write((byte) i);
    }

    void writeDEREncoded(int i, byte[] bArr) throws IOException {
        if (this._tagged) {
            OutputStream outputStream;
            int i2 = this._tagNo | 128;
            if (this._isExplicit) {
                i2 = (this._tagNo | 32) | 128;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                writeDEREncoded(byteArrayOutputStream, i, bArr);
                outputStream = this._out;
                bArr = byteArrayOutputStream.toByteArray();
            } else if ((i & 32) != 0) {
                outputStream = this._out;
                i2 |= 32;
            } else {
                outputStream = this._out;
            }
            writeDEREncoded(outputStream, i2, bArr);
            return;
        }
        writeDEREncoded(this._out, i, bArr);
    }

    void writeDEREncoded(OutputStream outputStream, int i, byte[] bArr) throws IOException {
        outputStream.write(i);
        writeLength(outputStream, bArr.length);
        outputStream.write(bArr);
    }
}
