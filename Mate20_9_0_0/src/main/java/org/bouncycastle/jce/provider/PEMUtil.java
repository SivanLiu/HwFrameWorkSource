package org.bouncycastle.jce.provider;

import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.util.encoders.Base64;

public class PEMUtil {
    private final String _footer1;
    private final String _footer2;
    private final String _header1;
    private final String _header2;

    PEMUtil(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-----BEGIN ");
        stringBuilder.append(str);
        stringBuilder.append("-----");
        this._header1 = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("-----BEGIN X509 ");
        stringBuilder.append(str);
        stringBuilder.append("-----");
        this._header2 = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("-----END ");
        stringBuilder.append(str);
        stringBuilder.append("-----");
        this._footer1 = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("-----END X509 ");
        stringBuilder.append(str);
        stringBuilder.append("-----");
        this._footer2 = stringBuilder.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0027  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0025  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String readLine(InputStream inputStream) throws IOException {
        int read;
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            read = inputStream.read();
            if (read == 13 || read == 10 || read < 0) {
                if (read < 0 || stringBuffer.length() != 0) {
                    return read >= 0 ? null : stringBuffer.toString();
                }
            } else if (read != 13) {
                stringBuffer.append((char) read);
            }
        }
        if (read >= 0) {
        }
    }

    ASN1Sequence readPEMObject(InputStream inputStream) throws IOException {
        String readLine;
        StringBuffer stringBuffer = new StringBuffer();
        do {
            readLine = readLine(inputStream);
            if (readLine == null || readLine.startsWith(this._header1)) {
                while (true) {
                    readLine = readLine(inputStream);
                    stringBuffer.append(readLine);
                }
            }
        } while (!readLine.startsWith(this._header2));
        while (true) {
            readLine = readLine(inputStream);
            if (readLine != null && !readLine.startsWith(this._footer1) && !readLine.startsWith(this._footer2)) {
                stringBuffer.append(readLine);
            }
        }
        if (stringBuffer.length() == 0) {
            return null;
        }
        ASN1Primitive readObject = new ASN1InputStream(Base64.decode(stringBuffer.toString())).readObject();
        if (readObject instanceof ASN1Sequence) {
            return (ASN1Sequence) readObject;
        }
        throw new IOException("malformed PEM data encountered");
    }
}
