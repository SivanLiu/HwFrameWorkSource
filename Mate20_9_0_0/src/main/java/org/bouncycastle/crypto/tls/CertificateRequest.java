package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x500.X500Name;

public class CertificateRequest {
    protected Vector certificateAuthorities;
    protected short[] certificateTypes;
    protected Vector supportedSignatureAlgorithms;

    public CertificateRequest(short[] sArr, Vector vector, Vector vector2) {
        this.certificateTypes = sArr;
        this.supportedSignatureAlgorithms = vector;
        this.certificateAuthorities = vector2;
    }

    public static CertificateRequest parse(TlsContext tlsContext, InputStream inputStream) throws IOException {
        short readUint8 = TlsUtils.readUint8(inputStream);
        short[] sArr = new short[readUint8];
        for (short s = (short) 0; s < readUint8; s++) {
            sArr[s] = TlsUtils.readUint8(inputStream);
        }
        Vector vector = null;
        if (TlsUtils.isTLSv12(tlsContext)) {
            vector = TlsUtils.parseSupportedSignatureAlgorithms(false, inputStream);
        }
        Vector vector2 = new Vector();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(TlsUtils.readOpaque16(inputStream));
        while (byteArrayInputStream.available() > 0) {
            vector2.addElement(X500Name.getInstance(TlsUtils.readDERObject(TlsUtils.readOpaque16(byteArrayInputStream))));
        }
        return new CertificateRequest(sArr, vector, vector2);
    }

    public void encode(OutputStream outputStream) throws IOException {
        int i = 0;
        if (this.certificateTypes == null || this.certificateTypes.length == 0) {
            TlsUtils.writeUint8(0, outputStream);
        } else {
            TlsUtils.writeUint8ArrayWithUint8Length(this.certificateTypes, outputStream);
        }
        if (this.supportedSignatureAlgorithms != null) {
            TlsUtils.encodeSupportedSignatureAlgorithms(this.supportedSignatureAlgorithms, false, outputStream);
        }
        if (this.certificateAuthorities == null || this.certificateAuthorities.isEmpty()) {
            TlsUtils.writeUint16(0, outputStream);
            return;
        }
        Vector vector = new Vector(this.certificateAuthorities.size());
        int i2 = 0;
        int i3 = i2;
        while (i2 < this.certificateAuthorities.size()) {
            byte[] encoded = ((X500Name) this.certificateAuthorities.elementAt(i2)).getEncoded(ASN1Encoding.DER);
            vector.addElement(encoded);
            i3 += encoded.length + 2;
            i2++;
        }
        TlsUtils.checkUint16(i3);
        TlsUtils.writeUint16(i3, outputStream);
        while (i < vector.size()) {
            TlsUtils.writeOpaque16((byte[]) vector.elementAt(i), outputStream);
            i++;
        }
    }

    public Vector getCertificateAuthorities() {
        return this.certificateAuthorities;
    }

    public short[] getCertificateTypes() {
        return this.certificateTypes;
    }

    public Vector getSupportedSignatureAlgorithms() {
        return this.supportedSignatureAlgorithms;
    }
}
