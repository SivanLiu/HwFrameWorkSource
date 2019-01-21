package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.util.encoders.Base64;
import java.io.IOException;
import java.io.InputStream;

public class PEMUtil {
    private final String _footer1;
    private final String _footer2;
    private final String _header1;
    private final String _header2;

    PEMUtil(String type) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-----BEGIN ");
        stringBuilder.append(type);
        stringBuilder.append("-----");
        this._header1 = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("-----BEGIN X509 ");
        stringBuilder.append(type);
        stringBuilder.append("-----");
        this._header2 = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("-----END ");
        stringBuilder.append(type);
        stringBuilder.append("-----");
        this._footer1 = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("-----END X509 ");
        stringBuilder.append(type);
        stringBuilder.append("-----");
        this._footer2 = stringBuilder.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0028  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0026  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String readLine(InputStream in) throws IOException {
        int c;
        StringBuffer l = new StringBuffer();
        while (true) {
            int read = in.read();
            c = read;
            if (read == 13 || c == 10 || c < 0) {
                if (c < 0 || l.length() != 0) {
                    if (c >= 0) {
                        return null;
                    }
                    return l.toString();
                }
            } else if (c != 13) {
                l.append((char) c);
            }
        }
        if (c >= 0) {
        }
    }

    ASN1Sequence readPEMObject(InputStream in) throws IOException {
        String line;
        String readLine;
        StringBuffer pemBuf = new StringBuffer();
        do {
            readLine = readLine(in);
            line = readLine;
            if (readLine == null || line.startsWith(this._header1)) {
                while (true) {
                    readLine = readLine(in);
                    line = readLine;
                    pemBuf.append(line);
                }
            }
        } while (!line.startsWith(this._header2));
        while (true) {
            readLine = readLine(in);
            line = readLine;
            if (readLine != null && !line.startsWith(this._footer1) && !line.startsWith(this._footer2)) {
                pemBuf.append(line);
            }
        }
        if (pemBuf.length() == 0) {
            return null;
        }
        ASN1Primitive o = new ASN1InputStream(Base64.decode(pemBuf.toString())).readObject();
        if (o instanceof ASN1Sequence) {
            return (ASN1Sequence) o;
        }
        throw new IOException("malformed PEM data encountered");
    }
}
