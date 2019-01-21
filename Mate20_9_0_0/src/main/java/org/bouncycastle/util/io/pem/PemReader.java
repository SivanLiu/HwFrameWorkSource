package org.bouncycastle.util.io.pem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import org.bouncycastle.util.encoders.Base64;

public class PemReader extends BufferedReader {
    private static final String BEGIN = "-----BEGIN ";
    private static final String END = "-----END ";

    public PemReader(Reader reader) {
        super(reader);
    }

    private PemObject loadObject(String str) throws IOException {
        String readLine;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(END);
        stringBuilder.append(str);
        String stringBuilder2 = stringBuilder.toString();
        StringBuffer stringBuffer = new StringBuffer();
        ArrayList arrayList = new ArrayList();
        while (true) {
            readLine = readLine();
            if (readLine == null) {
                break;
            } else if (readLine.indexOf(":") >= 0) {
                int indexOf = readLine.indexOf(58);
                arrayList.add(new PemHeader(readLine.substring(0, indexOf), readLine.substring(indexOf + 1).trim()));
            } else if (readLine.indexOf(stringBuilder2) != -1) {
                break;
            } else {
                stringBuffer.append(readLine.trim());
            }
        }
        if (readLine != null) {
            return new PemObject(str, arrayList, Base64.decode(stringBuffer.toString()));
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(stringBuilder2);
        stringBuilder3.append(" not found");
        throw new IOException(stringBuilder3.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:6:0x0011  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public PemObject readPemObject() throws IOException {
        String readLine;
        while (true) {
            readLine = readLine();
            if (readLine == null || readLine.startsWith(BEGIN)) {
                if (readLine != null) {
                    readLine = readLine.substring(BEGIN.length());
                    int indexOf = readLine.indexOf(45);
                    readLine = readLine.substring(0, indexOf);
                    if (indexOf > 0) {
                        return loadObject(readLine);
                    }
                }
            }
        }
        if (readLine != null) {
        }
        return null;
    }
}
