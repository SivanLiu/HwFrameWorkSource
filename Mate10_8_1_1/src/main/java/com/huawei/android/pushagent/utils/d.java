package com.huawei.android.pushagent.utils;

import com.huawei.android.pushagent.utils.a.b;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public abstract class d {
    public static void fk(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                b.ab("PushLog2976", "close IOException");
            }
        }
    }

    public static void fl(HttpURLConnection httpURLConnection) {
        if (httpURLConnection != null) {
            try {
                httpURLConnection.disconnect();
            } catch (Throwable th) {
                b.ab("PushLog2976", "close HttpURLConnection Exception");
            }
        }
    }

    public static String fj(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            int read = inputStream.read();
            if (-1 != read) {
                byteArrayOutputStream.write(read);
            } else {
                String byteArrayOutputStream2 = byteArrayOutputStream.toString("UTF-8");
                fk(inputStream);
                return byteArrayOutputStream2;
            }
        }
    }

    public static String fm(BufferedReader bufferedReader) {
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            int read = bufferedReader.read();
            if (read == -1) {
                break;
            }
            char c = (char) read;
            if (c == '\n' || c == '\r') {
                break;
            } else if (stringBuffer.length() >= 2097152) {
                b.y("PushLog2976", "read date exceed the max size!");
                return null;
            } else {
                stringBuffer.append(c);
            }
        }
        return stringBuffer.toString();
    }
}
