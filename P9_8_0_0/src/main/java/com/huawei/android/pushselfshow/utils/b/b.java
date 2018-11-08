package com.huawei.android.pushselfshow.utils.b;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.utils.a;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

public class b {
    public Handler a;
    public Context b;
    public String c;
    public String d;
    public boolean e;
    private boolean f;
    private Runnable g;

    public b() {
        this.a = null;
        this.e = false;
        this.g = new c(this);
        this.f = false;
    }

    public b(Handler handler, Context context, String str, String str2) {
        this.a = null;
        this.e = false;
        this.g = new c(this);
        this.a = handler;
        this.b = context;
        this.c = str;
        this.d = str2;
        this.f = false;
    }

    public static String a(Context context) {
        return b(context) + File.separator + "richpush";
    }

    public static void a(HttpClient httpClient) {
        if (httpClient != null) {
            try {
                httpClient.getConnectionManager().shutdown();
            } catch (Object -l_1_R) {
                c.d("PushSelfShowLog", "close input stream failed." + -l_1_R.getMessage(), -l_1_R);
            }
        }
    }

    public static void a(HttpRequestBase httpRequestBase) {
        if (httpRequestBase != null) {
            try {
                httpRequestBase.abort();
            } catch (Object -l_1_R) {
                c.d("PushSelfShowLog", "close input stream failed." + -l_1_R.getMessage(), -l_1_R);
            }
        }
    }

    public static String b(Context context) {
        Object -l_1_R = "";
        try {
            -l_1_R = Environment.getExternalStorageState().equals("mounted") != 0 ? a.l(context) : context.getFilesDir().getPath();
        } catch (Object -l_2_R) {
            c.d("PushSelfShowLog", "getLocalFileHeader failed ", -l_2_R);
        }
        -l_1_R = -l_1_R + File.separator + "PushService";
        c.a("PushSelfShowLog", "getFileHeader:" + -l_1_R);
        return -l_1_R;
    }

    public static String c(Context context) {
        Object -l_1_R = "";
        try {
            if (Environment.getExternalStorageState().equals("mounted") == 0) {
                return null;
            }
            -l_1_R = a.l(context);
            return -l_1_R + File.separator + "PushService";
        } catch (Object -l_2_R) {
            c.d("PushSelfShowLog", "getLocalFileHeader failed ", -l_2_R);
        }
    }

    public String a(Context context, String str, String str2) {
        Object -l_4_R;
        try {
            -l_4_R = "" + System.currentTimeMillis();
            Object -l_5_R = -l_4_R + str2;
            Object -l_6_R = a(context) + File.separator + -l_4_R;
            Object -l_7_R = -l_6_R + File.separator + -l_5_R;
            File -l_8_R = new File(-l_6_R);
            if (-l_8_R.exists()) {
                a.a(-l_8_R);
            } else {
                c.a("PushSelfShowLog", "dir is not exists()," + -l_8_R.getAbsolutePath());
            }
            if (-l_8_R.mkdirs()) {
                c.a("PushSelfShowLog", "dir.mkdirs() success  ");
            }
            c.a("PushSelfShowLog", "begin to download zip file, path is " + -l_7_R + ",dir is " + -l_8_R.getAbsolutePath());
            if (b(context, str, -l_7_R)) {
                return -l_7_R;
            }
            c.a("PushSelfShowLog", "download richpush file failed，clear temp file");
            if (-l_8_R.exists()) {
                a.a(-l_8_R);
            }
            return null;
        } catch (Object -l_4_R2) {
            c.a("PushSelfShowLog", "download err" + -l_4_R2.toString());
        }
    }

    public void a() {
        this.f = true;
    }

    public void a(String str) {
        Object -l_2_R = new Message();
        -l_2_R.what = 1;
        -l_2_R.obj = str;
        c.a("PushSelfShowLog", "mDownloadHandler = " + this.a);
        if (this.a != null) {
            this.a.sendMessageDelayed(-l_2_R, 1);
        }
    }

    public void b() {
        new Thread(this.g).start();
    }

    public boolean b(Context context, String str, String str2) {
        Object -l_10_R;
        Object -l_15_R;
        BufferedOutputStream bufferedOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        HttpClient httpClient = null;
        OutputStream outputStream = null;
        HttpRequestBase httpRequestBase = null;
        try {
            HttpClient -l_7_R = new DefaultHttpClient();
            try {
                HttpRequestBase httpGet = new HttpGet(str);
                try {
                    try {
                        Object -l_4_R = new d(context).a(str, -l_7_R, (HttpGet) httpGet);
                        if (-l_4_R != null) {
                            if (-l_4_R.getStatusLine() != null) {
                                if (-l_4_R.getStatusLine().getStatusCode() != 200) {
                                    c.a("PushSelfShowLog", "fail, httprespone status is " + -l_4_R.getStatusLine().getStatusCode());
                                    a(httpGet);
                                    a(-l_7_R);
                                    if (null != null) {
                                        try {
                                            bufferedOutputStream.close();
                                        } catch (Object -l_12_R) {
                                            c.d("PushSelfShowLog", " bos download  error" + -l_12_R.toString(), -l_12_R);
                                        }
                                    }
                                    if (null != null) {
                                        try {
                                            bufferedInputStream.close();
                                        } catch (Object -l_12_R2) {
                                            c.d("PushSelfShowLog", " bis download  error" + -l_12_R2.toString(), -l_12_R2);
                                        }
                                    }
                                    if (null != null) {
                                        try {
                                            outputStream.close();
                                        } catch (Object -l_12_R22) {
                                            c.d("PushSelfShowLog", "out download  error" + -l_12_R22.toString(), -l_12_R22);
                                        }
                                    }
                                    return false;
                                }
                            }
                            BufferedInputStream bufferedInputStream2 = new BufferedInputStream(-l_4_R.getEntity().getContent());
                            try {
                                BufferedOutputStream -l_5_R;
                                c.a("PushSelfShowLog", "begin to write content to " + str2);
                                if (new File(str2).exists()) {
                                    c.a("PushSelfShowLog", "file delete " + new File(str2).delete());
                                }
                                OutputStream fileOutputStream = new FileOutputStream(str2);
                                try {
                                    try {
                                        -l_5_R = new BufferedOutputStream(fileOutputStream);
                                    } catch (IOException e) {
                                        -l_10_R = e;
                                        httpRequestBase = httpGet;
                                        outputStream = fileOutputStream;
                                        httpClient = -l_7_R;
                                        bufferedInputStream = bufferedInputStream2;
                                        try {
                                            c.d("PushSelfShowLog", "downLoadSgThread download  error" + -l_10_R.toString(), -l_10_R);
                                            a(httpRequestBase);
                                            a(httpClient);
                                            if (bufferedOutputStream != null) {
                                                try {
                                                    bufferedOutputStream.close();
                                                } catch (Object -l_10_R2) {
                                                    c.d("PushSelfShowLog", " bos download  error" + -l_10_R2.toString(), -l_10_R2);
                                                }
                                            }
                                            if (bufferedInputStream != null) {
                                                try {
                                                    bufferedInputStream.close();
                                                } catch (Object -l_10_R22) {
                                                    c.d("PushSelfShowLog", " bis download  error" + -l_10_R22.toString(), -l_10_R22);
                                                }
                                            }
                                            if (outputStream != null) {
                                                try {
                                                    outputStream.close();
                                                } catch (Object -l_10_R222) {
                                                    c.d("PushSelfShowLog", "out download  error" + -l_10_R222.toString(), -l_10_R222);
                                                }
                                            }
                                            this.e = false;
                                            return false;
                                        } catch (Throwable th) {
                                            -l_15_R = th;
                                            a(httpRequestBase);
                                            a(httpClient);
                                            if (bufferedOutputStream != null) {
                                                try {
                                                    bufferedOutputStream.close();
                                                } catch (Object -l_16_R) {
                                                    c.d("PushSelfShowLog", " bos download  error" + -l_16_R.toString(), -l_16_R);
                                                }
                                            }
                                            if (bufferedInputStream != null) {
                                                try {
                                                    bufferedInputStream.close();
                                                } catch (Object -l_16_R2) {
                                                    c.d("PushSelfShowLog", " bis download  error" + -l_16_R2.toString(), -l_16_R2);
                                                }
                                            }
                                            if (outputStream != null) {
                                                try {
                                                    outputStream.close();
                                                } catch (Object -l_16_R22) {
                                                    c.d("PushSelfShowLog", "out download  error" + -l_16_R22.toString(), -l_16_R22);
                                                }
                                            }
                                            throw -l_15_R;
                                        }
                                    } catch (Throwable th2) {
                                        -l_15_R = th2;
                                        httpRequestBase = httpGet;
                                        outputStream = fileOutputStream;
                                        httpClient = -l_7_R;
                                        bufferedInputStream = bufferedInputStream2;
                                        a(httpRequestBase);
                                        a(httpClient);
                                        if (bufferedOutputStream != null) {
                                            bufferedOutputStream.close();
                                        }
                                        if (bufferedInputStream != null) {
                                            bufferedInputStream.close();
                                        }
                                        if (outputStream != null) {
                                            outputStream.close();
                                        }
                                        throw -l_15_R;
                                    }
                                } catch (IOException e2) {
                                    -l_10_R222 = e2;
                                    httpRequestBase = httpGet;
                                    outputStream = fileOutputStream;
                                    httpClient = -l_7_R;
                                    bufferedInputStream = bufferedInputStream2;
                                    c.d("PushSelfShowLog", "downLoadSgThread download  error" + -l_10_R222.toString(), -l_10_R222);
                                    a(httpRequestBase);
                                    a(httpClient);
                                    if (bufferedOutputStream != null) {
                                        bufferedOutputStream.close();
                                    }
                                    if (bufferedInputStream != null) {
                                        bufferedInputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    this.e = false;
                                    return false;
                                } catch (Throwable th3) {
                                    -l_15_R = th3;
                                    httpRequestBase = httpGet;
                                    outputStream = fileOutputStream;
                                    httpClient = -l_7_R;
                                    bufferedInputStream = bufferedInputStream2;
                                    a(httpRequestBase);
                                    a(httpClient);
                                    if (bufferedOutputStream != null) {
                                        bufferedOutputStream.close();
                                    }
                                    if (bufferedInputStream != null) {
                                        bufferedInputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    throw -l_15_R;
                                }
                                try {
                                    Object -l_11_R = new byte[32768];
                                    do {
                                        int -l_12_I = bufferedInputStream2.read(-l_11_R);
                                        if (-l_12_I >= 0) {
                                            this.e = true;
                                            -l_5_R.write(-l_11_R, 0, -l_12_I);
                                        } else {
                                            c.a("PushSelfShowLog", "downLoad success ");
                                            this.e = false;
                                            a(httpGet);
                                            a(-l_7_R);
                                            if (-l_5_R != null) {
                                                try {
                                                    -l_5_R.close();
                                                } catch (Object -l_14_R) {
                                                    c.d("PushSelfShowLog", " bos download  error" + -l_14_R.toString(), -l_14_R);
                                                }
                                            }
                                            if (bufferedInputStream2 != null) {
                                                try {
                                                    bufferedInputStream2.close();
                                                } catch (Object -l_14_R2) {
                                                    c.d("PushSelfShowLog", " bis download  error" + -l_14_R2.toString(), -l_14_R2);
                                                }
                                            }
                                            if (fileOutputStream != null) {
                                                try {
                                                    fileOutputStream.close();
                                                } catch (Object -l_14_R22) {
                                                    c.d("PushSelfShowLog", "out download  error" + -l_14_R22.toString(), -l_14_R22);
                                                }
                                                return true;
                                            }
                                            outputStream = fileOutputStream;
                                            return true;
                                        }
                                    } while (!this.f);
                                    a(httpGet);
                                    a(-l_7_R);
                                    if (-l_5_R != null) {
                                        try {
                                            -l_5_R.close();
                                        } catch (Object -l_10_R2222) {
                                            c.d("PushSelfShowLog", " bos download  error" + -l_10_R2222.toString(), -l_10_R2222);
                                        }
                                    }
                                    if (bufferedInputStream2 != null) {
                                        try {
                                            bufferedInputStream2.close();
                                        } catch (Object -l_10_R22222) {
                                            c.d("PushSelfShowLog", " bis download  error" + -l_10_R22222.toString(), -l_10_R22222);
                                        }
                                    }
                                    if (fileOutputStream == null) {
                                        outputStream = fileOutputStream;
                                    } else {
                                        try {
                                            fileOutputStream.close();
                                        } catch (Object -l_10_R222222) {
                                            c.d("PushSelfShowLog", "out download  error" + -l_10_R222222.toString(), -l_10_R222222);
                                            httpRequestBase = httpGet;
                                            outputStream = fileOutputStream;
                                            httpClient = -l_7_R;
                                            bufferedInputStream = bufferedInputStream2;
                                            bufferedOutputStream = -l_5_R;
                                        }
                                    }
                                    httpRequestBase = httpGet;
                                    httpClient = -l_7_R;
                                    bufferedInputStream = bufferedInputStream2;
                                    bufferedOutputStream = -l_5_R;
                                } catch (IOException e3) {
                                    -l_10_R222222 = e3;
                                    httpRequestBase = httpGet;
                                    outputStream = fileOutputStream;
                                    httpClient = -l_7_R;
                                    bufferedInputStream = bufferedInputStream2;
                                    bufferedOutputStream = -l_5_R;
                                    c.d("PushSelfShowLog", "downLoadSgThread download  error" + -l_10_R222222.toString(), -l_10_R222222);
                                    a(httpRequestBase);
                                    a(httpClient);
                                    if (bufferedOutputStream != null) {
                                        bufferedOutputStream.close();
                                    }
                                    if (bufferedInputStream != null) {
                                        bufferedInputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    this.e = false;
                                    return false;
                                } catch (Throwable th4) {
                                    -l_15_R = th4;
                                    httpRequestBase = httpGet;
                                    outputStream = fileOutputStream;
                                    httpClient = -l_7_R;
                                    bufferedInputStream = bufferedInputStream2;
                                    bufferedOutputStream = -l_5_R;
                                    a(httpRequestBase);
                                    a(httpClient);
                                    if (bufferedOutputStream != null) {
                                        bufferedOutputStream.close();
                                    }
                                    if (bufferedInputStream != null) {
                                        bufferedInputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                    throw -l_15_R;
                                }
                            } catch (IOException e4) {
                                -l_10_R222222 = e4;
                                httpRequestBase = httpGet;
                                httpClient = -l_7_R;
                                bufferedInputStream = bufferedInputStream2;
                                c.d("PushSelfShowLog", "downLoadSgThread download  error" + -l_10_R222222.toString(), -l_10_R222222);
                                a(httpRequestBase);
                                a(httpClient);
                                if (bufferedOutputStream != null) {
                                    bufferedOutputStream.close();
                                }
                                if (bufferedInputStream != null) {
                                    bufferedInputStream.close();
                                }
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                                this.e = false;
                                return false;
                            } catch (Throwable th5) {
                                -l_15_R = th5;
                                httpRequestBase = httpGet;
                                httpClient = -l_7_R;
                                bufferedInputStream = bufferedInputStream2;
                                a(httpRequestBase);
                                a(httpClient);
                                if (bufferedOutputStream != null) {
                                    bufferedOutputStream.close();
                                }
                                if (bufferedInputStream != null) {
                                    bufferedInputStream.close();
                                }
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                                throw -l_15_R;
                            }
                            this.e = false;
                            return false;
                        }
                        c.a("PushSelfShowLog", "fail, httprespone  is null");
                        a(httpGet);
                        a(-l_7_R);
                        if (null != null) {
                            try {
                                bufferedOutputStream.close();
                            } catch (Object -l_12_R222) {
                                c.d("PushSelfShowLog", " bos download  error" + -l_12_R222.toString(), -l_12_R222);
                            }
                        }
                        if (null != null) {
                            try {
                                bufferedInputStream.close();
                            } catch (Object -l_12_R2222) {
                                c.d("PushSelfShowLog", " bis download  error" + -l_12_R2222.toString(), -l_12_R2222);
                            }
                        }
                        if (null != null) {
                            try {
                                outputStream.close();
                            } catch (Object -l_12_R22222) {
                                c.d("PushSelfShowLog", "out download  error" + -l_12_R22222.toString(), -l_12_R22222);
                            }
                        }
                        return false;
                    } catch (IOException e5) {
                        -l_10_R222222 = e5;
                        httpRequestBase = httpGet;
                        httpClient = -l_7_R;
                        c.d("PushSelfShowLog", "downLoadSgThread download  error" + -l_10_R222222.toString(), -l_10_R222222);
                        a(httpRequestBase);
                        a(httpClient);
                        if (bufferedOutputStream != null) {
                            bufferedOutputStream.close();
                        }
                        if (bufferedInputStream != null) {
                            bufferedInputStream.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        this.e = false;
                        return false;
                    } catch (Throwable th6) {
                        -l_15_R = th6;
                        httpRequestBase = httpGet;
                        httpClient = -l_7_R;
                        a(httpRequestBase);
                        a(httpClient);
                        if (bufferedOutputStream != null) {
                            bufferedOutputStream.close();
                        }
                        if (bufferedInputStream != null) {
                            bufferedInputStream.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        throw -l_15_R;
                    }
                } catch (IOException e6) {
                    -l_10_R222222 = e6;
                    httpRequestBase = httpGet;
                    httpClient = -l_7_R;
                    c.d("PushSelfShowLog", "downLoadSgThread download  error" + -l_10_R222222.toString(), -l_10_R222222);
                    a(httpRequestBase);
                    a(httpClient);
                    if (bufferedOutputStream != null) {
                        bufferedOutputStream.close();
                    }
                    if (bufferedInputStream != null) {
                        bufferedInputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    this.e = false;
                    return false;
                } catch (Throwable th7) {
                    -l_15_R = th7;
                    httpRequestBase = httpGet;
                    httpClient = -l_7_R;
                    a(httpRequestBase);
                    a(httpClient);
                    if (bufferedOutputStream != null) {
                        bufferedOutputStream.close();
                    }
                    if (bufferedInputStream != null) {
                        bufferedInputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    throw -l_15_R;
                }
            } catch (IOException e7) {
                -l_10_R222222 = e7;
                httpClient = -l_7_R;
                c.d("PushSelfShowLog", "downLoadSgThread download  error" + -l_10_R222222.toString(), -l_10_R222222);
                a(httpRequestBase);
                a(httpClient);
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                this.e = false;
                return false;
            } catch (Throwable th8) {
                -l_15_R = th8;
                httpClient = -l_7_R;
                a(httpRequestBase);
                a(httpClient);
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                throw -l_15_R;
            }
        } catch (IOException e8) {
            -l_10_R222222 = e8;
            c.d("PushSelfShowLog", "downLoadSgThread download  error" + -l_10_R222222.toString(), -l_10_R222222);
            a(httpRequestBase);
            a(httpClient);
            if (bufferedOutputStream != null) {
                bufferedOutputStream.close();
            }
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            this.e = false;
            return false;
        }
    }

    public void c() {
        Object -l_1_R = new Message();
        -l_1_R.what = 2;
        c.a("PushSelfShowLog", "mDownloadHandler = " + this.a);
        if (this.a != null) {
            this.a.sendMessageDelayed(-l_1_R, 1);
        }
    }
}
