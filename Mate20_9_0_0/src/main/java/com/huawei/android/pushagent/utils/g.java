package com.huawei.android.pushagent.utils;

import com.huawei.android.pushagent.utils.b.a;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public abstract class g {
    public static void zx(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                a.sx("PushLog3414", "close IOException");
            }
        }
    }

    public static void zy(HttpURLConnection httpURLConnection) {
        if (httpURLConnection != null) {
            try {
                httpURLConnection.disconnect();
            } catch (Throwable th) {
                a.sx("PushLog3414", "close HttpURLConnection Exception");
            }
        }
    }

    public static String zw(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            int read = inputStream.read();
            if (-1 != read) {
                byteArrayOutputStream.write(read);
            } else {
                String byteArrayOutputStream2 = byteArrayOutputStream.toString("UTF-8");
                zx(inputStream);
                return byteArrayOutputStream2;
            }
        }
    }

    public static String zz(BufferedReader bufferedReader) {
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
                a.su("PushLog3414", "read date exceed the max size!");
                return null;
            } else {
                stringBuffer.append(c);
            }
        }
        return stringBuffer.toString();
    }
}
