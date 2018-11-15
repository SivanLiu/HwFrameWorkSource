package org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.util.encoders.Base64;

class PEMUtil {
    private final String _footer1;
    private final String _footer2;
    private final String _footer3;
    private final String _header1;
    private final String _header2;
    private final String _header3 = "-----BEGIN PKCS7-----";

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
        this._footer3 = "-----END PKCS7-----";
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0024  */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0022  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String readLine(InputStream inputStream) throws IOException {
        int read;
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            read = inputStream.read();
            if (read != 13 && read != 10 && read >= 0) {
                stringBuffer.append((char) read);
            } else if (read < 0 || stringBuffer.length() != 0) {
                if (read >= 0) {
                    return null;
                }
                if (read == 13) {
                    inputStream.mark(1);
                    int read2 = inputStream.read();
                    if (read2 == 10) {
                        inputStream.mark(1);
                    }
                    if (read2 > 0) {
                        inputStream.reset();
                    }
                }
                return stringBuffer.toString();
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
            if (readLine == null || readLine.startsWith(this._header1) || readLine.startsWith(this._header2)) {
                while (true) {
                    readLine = readLine(inputStream);
                    stringBuffer.append(readLine);
                }
            }
        } while (!readLine.startsWith(this._header3));
        while (true) {
            readLine = readLine(inputStream);
            if (readLine != null && !readLine.startsWith(this._footer1) && !readLine.startsWith(this._footer2) && !readLine.startsWith(this._footer3)) {
                stringBuffer.append(readLine);
            }
        }
        if (stringBuffer.length() == 0) {
            return null;
        }
        try {
            return ASN1Sequence.getInstance(Base64.decode(stringBuffer.toString()));
        } catch (Exception e) {
            throw new IOException("malformed PEM data encountered");
        }
    }
}
