package com.android.org.bouncycastle.util.io.pem;

import com.android.org.bouncycastle.util.encoders.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class PemReader extends BufferedReader {
    private static final String BEGIN = "-----BEGIN ";
    private static final String END = "-----END ";

    public PemReader(Reader reader) {
        super(reader);
    }

    public PemObject readPemObject() throws IOException {
        String line = readLine();
        while (line != null && !line.startsWith(BEGIN)) {
            line = readLine();
        }
        if (line != null) {
            line = line.substring(BEGIN.length());
            int index = line.indexOf(45);
            String type = line.substring(null, index);
            if (index > 0) {
                return loadObject(type);
            }
        }
        return null;
    }

    private PemObject loadObject(String type) throws IOException {
        String line;
        String endMarker = new StringBuilder();
        endMarker.append(END);
        endMarker.append(type);
        endMarker = endMarker.toString();
        StringBuffer buf = new StringBuffer();
        List headers = new ArrayList();
        while (true) {
            String readLine = readLine();
            line = readLine;
            if (readLine == null) {
                break;
            } else if (line.indexOf(":") >= 0) {
                int index = line.indexOf(58);
                headers.add(new PemHeader(line.substring(null, index), line.substring(index + 1).trim()));
            } else if (line.indexOf(endMarker) != -1) {
                break;
            } else {
                buf.append(line.trim());
            }
        }
        if (line != null) {
            return new PemObject(type, headers, Base64.decode(buf.toString()));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(endMarker);
        stringBuilder.append(" not found");
        throw new IOException(stringBuilder.toString());
    }
}
