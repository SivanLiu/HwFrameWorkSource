package com.huawei.android.pushagent.utils;

import com.huawei.android.pushagent.utils.f.c;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public abstract class b {
    public static void ff(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                c.eo("PushLog3413", "close IOException");
            }
        }
    }

    public static void fg(HttpURLConnection httpURLConnection) {
        if (httpURLConnection != null) {
            try {
                httpURLConnection.disconnect();
            } catch (Throwable th) {
                c.eo("PushLog3413", "close HttpURLConnection Exception");
            }
        }
    }

    public static String fi(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            int read = inputStream.read();
            if (-1 != read) {
                byteArrayOutputStream.write(read);
            } else {
                String byteArrayOutputStream2 = byteArrayOutputStream.toString("UTF-8");
                ff(inputStream);
                return byteArrayOutputStream2;
            }
        }
    }

    public static String fh(BufferedReader bufferedReader) {
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            int read = bufferedReader.read();
            if (read == -1) {
                break;
            }
            char c = (char) read;
            if (c == 10 || c == 13) {
                break;
            } else if (stringBuffer.length() >= 2097152) {
                c.eq("PushLog3413", "read date exceed the max size!");
                return null;
            } else {
                stringBuffer.append(c);
            }
        }
        return stringBuffer.toString();
    }
}
