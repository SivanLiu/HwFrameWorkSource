package tmsdkobf;

import android.content.Context;
import android.net.Proxy;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.zip.InflaterInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import tmsdk.common.ErrorCode;
import tmsdk.common.exception.NetWorkException;
import tmsdk.common.exception.NetworkOnMainThreadException;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;

public class lx extends lv {
    private Context mContext;
    private boolean mIsCanceled = false;
    private String zA = null;
    private String zB = null;
    private String zC = null;
    private int zD = 0;
    private long zE = 0;
    private long zF = 0;
    private boolean zG = false;
    private HttpGet zy = null;
    private String zz = null;

    public interface a {
        boolean bS(String str);
    }

    public lx(Context context) {
        if (!i.iK() && Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
            throw new NetworkOnMainThreadException();
        }
        this.mContext = context;
        this.zz = context.getCacheDir().getAbsolutePath();
        this.zA = context.getFilesDir().getAbsolutePath();
        this.zy = new HttpGet();
        if (i.iG() == eb.iK) {
            d(Proxy.getDefaultHost(), Proxy.getDefaultPort());
            u(true);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int a(HttpEntity httpEntity, Bundle bundle, boolean z) throws NetWorkException {
        Object -l_9_R;
        Object -l_15_R;
        FileOutputStream fileOutputStream = null;
        InputStream inputStream = null;
        Object -l_8_R = new byte[8192];
        try {
            this.zF = httpEntity.getContentLength() + this.zE;
            int -l_9_I = (int) ((this.zE * 100) / this.zF);
            Object -l_10_R = new File(this.zz, this.zB);
            if (!-l_10_R.exists()) {
                -l_10_R.getParentFile().mkdirs();
                -l_10_R.createNewFile();
            }
            FileOutputStream -l_5_R = new FileOutputStream(-l_10_R, true);
            if (z) {
                inputStream = new InflaterInputStream(httpEntity.getContent());
            } else {
                try {
                    inputStream = httpEntity.getContent();
                } catch (FileNotFoundException e) {
                    -l_9_R = e;
                    fileOutputStream = -l_5_R;
                } catch (SocketException e2) {
                    -l_9_R = e2;
                    fileOutputStream = -l_5_R;
                } catch (SocketTimeoutException e3) {
                    -l_9_R = e3;
                    fileOutputStream = -l_5_R;
                } catch (IOException e4) {
                    -l_9_R = e4;
                    fileOutputStream = -l_5_R;
                } catch (Exception e5) {
                    -l_9_R = e5;
                    fileOutputStream = -l_5_R;
                } catch (Throwable th) {
                    -l_15_R = th;
                    fileOutputStream = -l_5_R;
                }
            }
            int -l_12_I = 0;
            while (true) {
                int -l_11_I = inputStream.read(-l_8_R);
                if (-l_11_I != -1) {
                    if (this.mIsCanceled) {
                        break;
                    }
                    this.zE += (long) -l_11_I;
                    -l_12_I += -l_11_I;
                    int -l_13_I = (int) ((this.zE * 100) / this.zF);
                    if (-l_13_I != -l_9_I) {
                        -l_9_I = -l_13_I;
                        bundle.putInt("key_progress", -l_13_I);
                        a(2, bundle);
                    }
                    -l_5_R.write(-l_8_R, 0, -l_11_I);
                } else {
                    break;
                }
            }
            -l_5_R.flush();
            f.f("HttpBase", "mTotalSize: " + this.zF + ", mCompletedSize: " + this.zE + ", httpEntity.getContentLength(): " + httpEntity.getContentLength());
            int -l_4_I = ((long) -l_12_I) == httpEntity.getContentLength() ? 0 : -7;
            if (inputStream != null) {
                f.f("HttpBase", "is closing file");
                try {
                    inputStream.close();
                } catch (Object -l_9_R2) {
                    -l_4_I = ErrorCode.ERR_FILE_OP;
                    f.e("HttpBase", "is close file error");
                    -l_9_R2.printStackTrace();
                }
            }
            if (-l_5_R != null) {
                f.f("HttpBase", "fos closing file");
                try {
                    -l_5_R.close();
                } catch (Object -l_9_R22) {
                    -l_4_I = ErrorCode.ERR_FILE_OP;
                    f.e("HttpBase", "fos close file error");
                    -l_9_R22.printStackTrace();
                }
            }
            return -l_4_I;
            if (-l_5_R != null) {
                f.f("HttpBase", "fos closing file");
                try {
                    -l_5_R.close();
                } catch (Object -l_14_R) {
                    f.e("HttpBase", "fos close file error");
                    -l_14_R.printStackTrace();
                }
            }
            return -5003;
            return -5003;
        } catch (FileNotFoundException e6) {
            -l_9_R22 = e6;
            f.e("HttpBase", "file not found");
            -l_9_R22.printStackTrace();
            throw new NetWorkException(-7001, -l_9_R22.getMessage());
        } catch (SocketException e7) {
            -l_9_R22 = e7;
            f.e("HttpBase", "socket error:" + -l_9_R22.getMessage());
            -l_9_R22.printStackTrace();
            throw new NetWorkException(-5054, -l_9_R22.getMessage());
        } catch (SocketTimeoutException e8) {
            -l_9_R22 = e8;
            f.e("HttpBase", "socket timeout error:" + -l_9_R22.getMessage());
            -l_9_R22.printStackTrace();
            throw new NetWorkException(-5055, -l_9_R22.getMessage());
        } catch (IOException e9) {
            -l_9_R22 = e9;
            f.e("HttpBase", "socket or file io error");
            -l_9_R22.printStackTrace();
            throw new NetWorkException(-5056, -l_9_R22.getMessage());
        } catch (Exception e10) {
            -l_9_R22 = e10;
            f.e("HttpBase", -l_9_R22.toString());
            f.e("HttpBase", "receive data error");
            -l_9_R22.printStackTrace();
            throw new NetWorkException((int) ErrorCode.ERR_RECEIVE, -l_9_R22.getMessage());
        } catch (Throwable th2) {
            -l_15_R = th2;
            if (inputStream != null) {
                f.f("HttpBase", "is closing file");
                try {
                    inputStream.close();
                } catch (Object -l_16_R) {
                    f.e("HttpBase", "is close file error");
                    -l_16_R.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                f.f("HttpBase", "fos closing file");
                try {
                    fileOutputStream.close();
                } catch (Object -l_16_R2) {
                    f.e("HttpBase", "fos close file error");
                    -l_16_R2.printStackTrace();
                }
            }
            throw -l_15_R;
        }
    }

    private int bR(String str) throws NetWorkException {
        try {
            Object -l_3_R = new URI(str);
            if (-l_3_R == null) {
                return ErrorCode.ERR_OPEN_CONNECTION;
            }
            this.zy.setURI(-l_3_R);
            return 0;
        } catch (Object -l_4_R) {
            f.e("HttpBase", "url error: " + -l_4_R.getMessage());
            -l_4_R.printStackTrace();
            throw new NetWorkException(-1053, -l_4_R.getMessage());
        }
    }

    private int v(boolean z) throws NetWorkException {
        FileInputStream -l_4_R;
        Object -l_6_R;
        Object -l_8_R;
        FileOutputStream fileOutputStream = null;
        FileInputStream -l_4_R2 = null;
        f.f("HttpGetFile", this.zz + File.separator + this.zB);
        f.f("HttpGetFile", this.zA + File.separator + this.zC);
        File file = null;
        try {
            File -l_5_R = new File(this.zz, this.zB);
            try {
                int -l_2_I;
                if (-l_5_R.exists()) {
                    int -l_7_I;
                    FileOutputStream -l_3_R;
                    if (this.zD == 1) {
                        if (this.mContext.getFilesDir().getAbsolutePath().equals(this.zA)) {
                            fileOutputStream = this.mContext.openFileOutput(this.zC, 1);
                            -l_4_R = new FileInputStream(-l_5_R);
                            -l_6_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
                            while (true) {
                                -l_7_I = -l_4_R.read(-l_6_R);
                                if (-l_7_I == -1) {
                                    break;
                                }
                                fileOutputStream.write(-l_6_R, 0, -l_7_I);
                            }
                            -l_2_I = 0;
                            -l_4_R2 = -l_4_R;
                        }
                    }
                    -l_6_R = new File(this.zA + File.separator + this.zC);
                    if (-l_6_R.exists()) {
                        -l_6_R.delete();
                        -l_3_R = new FileOutputStream(-l_6_R);
                    } else {
                        -l_6_R.getParentFile().mkdirs();
                        -l_6_R.createNewFile();
                        -l_3_R = new FileOutputStream(-l_6_R);
                    }
                    fileOutputStream = -l_3_R;
                    try {
                        -l_4_R = new FileInputStream(-l_5_R);
                        try {
                            -l_6_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
                            while (true) {
                                -l_7_I = -l_4_R.read(-l_6_R);
                                if (-l_7_I == -1) {
                                    break;
                                }
                                fileOutputStream.write(-l_6_R, 0, -l_7_I);
                            }
                            -l_2_I = 0;
                            -l_4_R2 = -l_4_R;
                        } catch (FileNotFoundException e) {
                            -l_6_R = e;
                            file = -l_5_R;
                            -l_4_R2 = -l_4_R;
                        } catch (IOException e2) {
                            -l_6_R = e2;
                            file = -l_5_R;
                            -l_4_R2 = -l_4_R;
                        } catch (Exception e3) {
                            -l_6_R = e3;
                            file = -l_5_R;
                            -l_4_R2 = -l_4_R;
                        } catch (Throwable th) {
                            -l_8_R = th;
                            file = -l_5_R;
                            -l_4_R2 = -l_4_R;
                        }
                    } catch (FileNotFoundException e4) {
                        -l_6_R = e4;
                        file = -l_5_R;
                        f.e("HttpBase", "file not found");
                        -l_6_R.printStackTrace();
                        throw new NetWorkException(-7001, -l_6_R.getMessage());
                    } catch (IOException e5) {
                        -l_6_R = e5;
                        file = -l_5_R;
                        f.e("HttpBase", "file io error");
                        -l_6_R.printStackTrace();
                        throw new NetWorkException(-7056, -l_6_R.getMessage());
                    } catch (Exception e6) {
                        -l_6_R = e6;
                        file = -l_5_R;
                        f.e("HttpBase", "file op error");
                        -l_6_R.printStackTrace();
                        throw new NetWorkException((int) ErrorCode.ERR_FILE_OP, -l_6_R.getMessage());
                    } catch (Throwable th2) {
                        -l_8_R = th2;
                        file = -l_5_R;
                        if (-l_4_R2 != null) {
                            try {
                                -l_4_R2.close();
                            } catch (Object -l_9_R) {
                                f.e("HttpBase", "fis close file error");
                                -l_9_R.printStackTrace();
                            }
                        }
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (Object -l_9_R2) {
                                f.e("HttpBase", "fosclose file error");
                                -l_9_R2.printStackTrace();
                            }
                        }
                        file.delete();
                        throw -l_8_R;
                    }
                }
                -l_2_I = -7001;
                if (-l_4_R != null) {
                    try {
                        -l_4_R.close();
                    } catch (Object -l_6_R2) {
                        -l_2_I = ErrorCode.ERR_FILE_OP;
                        f.e("HttpBase", "fis close file error");
                        -l_6_R2.printStackTrace();
                    }
                }
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (Object -l_6_R22) {
                        -l_2_I = ErrorCode.ERR_FILE_OP;
                        f.e("HttpBase", "fosclose file error");
                        -l_6_R22.printStackTrace();
                    }
                }
                if (z && -l_5_R != null && -l_5_R.exists()) {
                    -l_5_R.delete();
                }
                return -l_2_I;
            } catch (FileNotFoundException e7) {
                -l_6_R22 = e7;
                file = -l_5_R;
                f.e("HttpBase", "file not found");
                -l_6_R22.printStackTrace();
                throw new NetWorkException(-7001, -l_6_R22.getMessage());
            } catch (IOException e8) {
                -l_6_R22 = e8;
                file = -l_5_R;
                f.e("HttpBase", "file io error");
                -l_6_R22.printStackTrace();
                throw new NetWorkException(-7056, -l_6_R22.getMessage());
            } catch (Exception e9) {
                -l_6_R22 = e9;
                file = -l_5_R;
                f.e("HttpBase", "file op error");
                -l_6_R22.printStackTrace();
                throw new NetWorkException((int) ErrorCode.ERR_FILE_OP, -l_6_R22.getMessage());
            } catch (Throwable th3) {
                -l_8_R = th3;
                file = -l_5_R;
                if (-l_4_R2 != null) {
                    -l_4_R2.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                if (z && file != null && file.exists()) {
                    file.delete();
                }
                throw -l_8_R;
            }
        } catch (FileNotFoundException e10) {
            -l_6_R22 = e10;
            f.e("HttpBase", "file not found");
            -l_6_R22.printStackTrace();
            throw new NetWorkException(-7001, -l_6_R22.getMessage());
        } catch (IOException e11) {
            -l_6_R22 = e11;
            f.e("HttpBase", "file io error");
            -l_6_R22.printStackTrace();
            throw new NetWorkException(-7056, -l_6_R22.getMessage());
        } catch (Exception e12) {
            -l_6_R22 = e12;
            f.e("HttpBase", "file op error");
            -l_6_R22.printStackTrace();
            throw new NetWorkException((int) ErrorCode.ERR_FILE_OP, -l_6_R22.getMessage());
        } catch (Throwable th4) {
            -l_8_R = th4;
            if (-l_4_R2 != null) {
                -l_4_R2.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            file.delete();
            throw -l_8_R;
        }
    }

    public int a(String -l_10_R, String str, boolean z, a aVar) {
        int -l_5_I = ErrorCode.ERR_GET;
        Object -l_6_R = "";
        HttpClient httpClient = null;
        HttpResponse httpResponse = null;
        Object -l_9_R = new Bundle();
        try {
            httpClient = eH();
            -l_5_I = bR(str);
            if (-l_5_I == 0) {
                if (!this.mIsCanceled) {
                    if (this.zy.getURI() != null) {
                        Object -l_10_R2 = "downloadfile";
                        if (TextUtils.isEmpty(-l_10_R)) {
                            -l_10_R = lu.p(str, null);
                        }
                        this.zB = -l_10_R + ".tmp";
                        f.f("HttpBase", "mTempName: " + this.zB);
                        if (this.zC == null) {
                            this.zC = -l_10_R;
                        }
                        Object -l_11_R = new File(this.zz, this.zB);
                        if (-l_11_R.exists()) {
                            this.zE = -l_11_R.length();
                            this.zy.setHeader("RANGE", "bytes=" + this.zE + "-");
                            this.zG = true;
                        }
                        httpResponse = httpClient.execute(this.zy);
                        int -l_12_I = httpResponse.getStatusLine().getStatusCode();
                        f.f("HttpBase", "statusCode == " + -l_12_I);
                        if (-l_12_I != SmsCheckResult.ESCT_200 && -l_12_I != SmsCheckResult.ESCT_206) {
                            -l_5_I = -3000 - -l_12_I;
                        } else if (!this.mIsCanceled) {
                            Object -l_13_R = httpResponse.getEntity();
                            if (-l_13_R != null) {
                                -l_5_I = a(-l_13_R, -l_9_R, z);
                                if (-l_5_I == 0) {
                                    if (aVar != null) {
                                        if (aVar.bS(this.zz + File.separator + this.zB) == 0) {
                                            -l_5_I = ErrorCode.ERR_FILE_OP;
                                            new File(this.zz + File.separator + this.zB).delete();
                                        }
                                    }
                                    -l_5_I = v(true);
                                    if (-l_5_I == 0) {
                                        -l_5_I = 0;
                                    }
                                }
                            } else {
                                -l_5_I = ErrorCode.ERR_RESPONSE;
                                f.e("HttpBase", "httpEntity == null");
                            }
                        }
                    } else {
                        -l_5_I = -3053;
                        f.e("HttpBase", "url == null");
                    }
                }
                -l_5_I = -3003;
            }
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpResponse != null) {
            }
            if (!(-l_5_I == 0 || -l_5_I == -7)) {
                -l_9_R.putInt("key_errcode", -l_5_I);
                -l_9_R.putString("key_errorMsg", -l_6_R);
                -l_9_R.putInt("key_downSize", (int) this.zE);
                -l_9_R.putInt("key_total", (int) this.zF);
                -l_9_R.putInt("key_sdcardstatus", lu.t(this.zF - this.zE));
                -l_9_R.putByte("key_downType", (byte) (!this.zG ? 0 : 1));
                a(1, -l_9_R);
            }
        } catch (String -l_10_R3) {
            -l_5_I = -3051;
            -l_6_R = -l_10_R3.getMessage();
            f.e("HttpBase", "protocol error:" + -l_10_R3.getMessage());
            -l_10_R3.printStackTrace();
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpResponse != null) {
            }
            -l_9_R.putInt("key_errcode", -3051);
            -l_9_R.putString("key_errorMsg", -l_6_R);
            -l_9_R.putInt("key_downSize", (int) this.zE);
            -l_9_R.putInt("key_total", (int) this.zF);
            -l_9_R.putInt("key_sdcardstatus", lu.t(this.zF - this.zE));
            -l_9_R.putByte("key_downType", (byte) (!this.zG ? 0 : 1));
            a(1, -l_9_R);
        } catch (String -l_10_R32) {
            -l_5_I = -3054;
            -l_6_R = -l_10_R32.getMessage();
            f.e("HttpBase", "socket error:" + -l_10_R32.getMessage());
            -l_10_R32.printStackTrace();
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpResponse != null) {
            }
            -l_9_R.putInt("key_errcode", -3054);
            -l_9_R.putString("key_errorMsg", -l_6_R);
            -l_9_R.putInt("key_downSize", (int) this.zE);
            -l_9_R.putInt("key_total", (int) this.zF);
            -l_9_R.putInt("key_sdcardstatus", lu.t(this.zF - this.zE));
            -l_9_R.putByte("key_downType", (byte) (!this.zG ? 0 : 1));
            a(1, -l_9_R);
        } catch (String -l_10_R322) {
            -l_5_I = -3055;
            -l_6_R = -l_10_R322.getMessage();
            f.e("HttpBase", "socket timeout error:" + -l_10_R322.getMessage());
            -l_10_R322.printStackTrace();
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpResponse != null) {
            }
            -l_9_R.putInt("key_errcode", -3055);
            -l_9_R.putString("key_errorMsg", -l_6_R);
            -l_9_R.putInt("key_downSize", (int) this.zE);
            -l_9_R.putInt("key_total", (int) this.zF);
            -l_9_R.putInt("key_sdcardstatus", lu.t(this.zF - this.zE));
            -l_9_R.putByte("key_downType", (byte) (!this.zG ? 0 : 1));
            a(1, -l_9_R);
        } catch (String -l_10_R3222) {
            -l_5_I = -3056;
            -l_6_R = -l_10_R3222.getMessage();
            f.e("HttpBase", "io error:" + -l_10_R3222.getMessage());
            -l_10_R3222.printStackTrace();
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpResponse != null) {
            }
            -l_9_R.putInt("key_errcode", -3056);
            -l_9_R.putString("key_errorMsg", -l_6_R);
            -l_9_R.putInt("key_downSize", (int) this.zE);
            -l_9_R.putInt("key_total", (int) this.zF);
            -l_9_R.putInt("key_sdcardstatus", lu.t(this.zF - this.zE));
            -l_9_R.putByte("key_downType", (byte) (!this.zG ? 0 : 1));
            a(1, -l_9_R);
        } catch (String -l_10_R32222) {
            -l_5_I = -l_10_R32222.getErrCode();
            -l_6_R = -l_10_R32222.getMessage();
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpResponse != null) {
            }
            if (!(-l_5_I == 0 || -l_5_I == -7)) {
                -l_9_R.putInt("key_errcode", -l_5_I);
                -l_9_R.putString("key_errorMsg", -l_6_R);
                -l_9_R.putInt("key_downSize", (int) this.zE);
                -l_9_R.putInt("key_total", (int) this.zF);
                -l_9_R.putInt("key_sdcardstatus", lu.t(this.zF - this.zE));
                -l_9_R.putByte("key_downType", (byte) (!this.zG ? 0 : 1));
                a(1, -l_9_R);
            }
        } catch (String -l_10_R322222) {
            -l_5_I = ErrorCode.ERR_GET;
            -l_6_R = -l_10_R322222.getMessage();
            f.e("HttpBase", "get error:" + -l_10_R322222.getMessage());
            -l_10_R322222.printStackTrace();
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpResponse != null) {
            }
            -l_9_R.putInt("key_errcode", ErrorCode.ERR_GET);
            -l_9_R.putString("key_errorMsg", -l_6_R);
            -l_9_R.putInt("key_downSize", (int) this.zE);
            -l_9_R.putInt("key_total", (int) this.zF);
            -l_9_R.putInt("key_sdcardstatus", lu.t(this.zF - this.zE));
            -l_9_R.putByte("key_downType", (byte) (!this.zG ? 0 : 1));
            a(1, -l_9_R);
        } catch (Throwable th) {
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpResponse != null) {
            }
            if (!(-l_5_I == 0 || -l_5_I == -7)) {
                -l_9_R.putInt("key_errcode", -l_5_I);
                -l_9_R.putString("key_errorMsg", -l_6_R);
                -l_9_R.putInt("key_downSize", (int) this.zE);
                -l_9_R.putInt("key_total", (int) this.zF);
                -l_9_R.putInt("key_sdcardstatus", lu.t(this.zF - this.zE));
                -l_9_R.putByte("key_downType", (byte) (!this.zG ? 0 : 1));
                a(1, -l_9_R);
            }
        }
        return -l_5_I;
    }

    public void bP(String str) {
        this.zA = str;
    }

    public void bQ(String str) {
        this.zC = str;
    }
}
