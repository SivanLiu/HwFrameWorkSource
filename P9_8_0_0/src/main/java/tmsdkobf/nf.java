package tmsdkobf;

import android.content.Context;
import android.os.Build.VERSION;
import android.text.TextUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.exception.NetWorkException;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.i;
import tmsdk.common.utils.n;
import tmsdkobf.nw.f;

public class nf {
    private static String TAG = "HttpNetwork";
    private final int CP = 3;
    private final int CQ = 3;
    private String CR = "POST";
    private HttpURLConnection CS;
    private nl CT;
    private om CU;
    private String CV;
    private int CW = 0;
    private boolean CX = false;

    public interface a {
        void b(int i, byte[] bArr);
    }

    public nf(Context context, nl nlVar, om omVar, boolean z) {
        this.CT = nlVar;
        this.CU = omVar;
        this.CX = z;
    }

    private int a(byte[] bArr, AtomicReference<byte[]> atomicReference) {
        Object -l_3_R;
        Object -l_5_R;
        mb.n(TAG, "[http_control]doSend()");
        if (this.CS == null) {
            return -10000;
        }
        try {
            if (!"GET".equalsIgnoreCase(this.CR)) {
                this.CS.setRequestProperty("Content-length", "" + bArr.length);
            }
            try {
                if (VERSION.SDK != null) {
                    if (VERSION.SDK_INT > 13) {
                        this.CS.setRequestProperty("Connection", "close");
                    }
                }
            } catch (Exception e) {
            }
            mb.n(TAG, "[http_control]doSend(), bf [http send] bytes: " + bArr.length);
            -l_3_R = this.CS.getOutputStream();
            -l_3_R.write(bArr);
            -l_3_R.flush();
            -l_3_R.close();
            mb.d(TAG, "[flow_control][http_control]doSend(), [http send] bytes: " + bArr.length);
            int -l_4_I = this.CS.getResponseCode();
            if (bl(-l_4_I)) {
                this.CV = eM();
                this.CW++;
                mb.d(TAG, "[http_control]doSend()，需重定向, mRedirectUrl: " + this.CV + " mRedirectTimes: " + this.CW);
                return -60000;
            }
            fw();
            mb.n(TAG, "[http_control]doSend(), resposeCode: " + -l_4_I);
            try {
                if (mb.isEnable()) {
                    mb.n(TAG, "[http_control]doSend(), HeaderFields: " + this.CS.getHeaderFields());
                }
                -l_5_R = this.CS.getHeaderField("Server");
                if (TextUtils.isEmpty(-l_5_R)) {
                    mb.o(TAG, "[http_control]doSend(), getHeaderField('BACK_KEY') should be 'QBServer', actually return: " + -l_5_R);
                    return -170000;
                } else if (-l_5_R.equals("QBServer")) {
                    Object -l_7_R = d(this.CS.getInputStream());
                    atomicReference.set(-l_7_R);
                    if (-l_7_R != null) {
                        mb.d(TAG, "[flow_control][http_control]doSend(), [http receive] bytes: " + -l_7_R.length);
                    }
                    return 0;
                } else {
                    mb.o(TAG, "[http_control]doSend(), getHeaderField('BACK_KEY') should be 'QBServer', actually return: " + -l_5_R);
                    return -560000;
                }
            } catch (Object -l_5_R2) {
                mb.e(TAG, -l_5_R2);
                return -40000;
            }
        } catch (Object -l_3_R2) {
            mb.c(TAG, "doSend(), UnknownHostException: ", -l_3_R2);
            return -70000;
        } catch (Object -l_3_R22) {
            mb.c(TAG, "doSend(), IllegalAccessError: ", -l_3_R22);
            return -80000;
        } catch (Object -l_3_R222) {
            mb.c(TAG, "doSend(), IllegalStateException: ", -l_3_R222);
            return -90000;
        } catch (Object -l_3_R2222) {
            mb.c(TAG, "doSend(), ProtocolException: ", -l_3_R2222);
            return -100000;
        } catch (Object -l_3_R22222) {
            mb.c(TAG, "doSend(), ClientProtocolException: ", -l_3_R22222);
            return -110000;
        } catch (Object -l_3_R222222) {
            mb.c(TAG, "doSend(), ConnectException: ", -l_3_R222222);
            return ne.f(-l_3_R222222.toString(), -500000);
        } catch (Object -l_3_R2222222) {
            mb.c(TAG, "doSend(), SocketException: ", -l_3_R2222222);
            return ne.f(-l_3_R2222222.toString(), -420000);
        } catch (Object -l_3_R22222222) {
            mb.c(TAG, "doSend(), SecurityException: ", -l_3_R22222222);
            return ne.f(-l_3_R22222222.toString(), -440000);
        } catch (Object -l_3_R222222222) {
            mb.c(TAG, "doSend(), SocketTimeoutException: ", -l_3_R222222222);
            return -130000;
        } catch (Object -l_3_R2222222222) {
            mb.c(TAG, "doSend(), IOException: ", -l_3_R2222222222);
            return -140000;
        } catch (Object -l_3_R22222222222) {
            mb.c(TAG, "doSend(), Exception: ", -l_3_R22222222222);
            return -150000;
        }
    }

    private boolean bl(int i) {
        return i >= SmsCheckResult.ESCT_301 && i <= SmsCheckResult.ESCT_305;
    }

    private int cb(String -l_3_R) {
        mb.n(TAG, "[http_control]start()");
        if (this.CW >= 3) {
            fw();
        }
        if (!TextUtils.isEmpty(this.CV)) {
            -l_3_R = this.CV;
        }
        try {
            Object -l_4_R = new URL(-l_3_R);
            int -l_2_I;
            try {
                eb -l_5_R = i.iG();
                if (eb.iH != -l_5_R) {
                    if (eb.iK != -l_5_R) {
                        this.CS = (HttpURLConnection) -l_4_R.openConnection();
                        this.CS.setReadTimeout(15000);
                        this.CS.setConnectTimeout(15000);
                    } else {
                        this.CS = (HttpURLConnection) -l_4_R.openConnection(new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(i.iI(), i.iJ())));
                    }
                    if (n.iX() < 8) {
                        System.setProperty("http.keepAlive", "false");
                    }
                    this.CS.setUseCaches(false);
                    this.CS.setRequestProperty("Pragma", "no-cache");
                    this.CS.setRequestProperty("Cache-Control", "no-cache");
                    this.CS.setInstanceFollowRedirects(false);
                    if ("GET".equalsIgnoreCase(this.CR)) {
                        this.CS.setRequestMethod("GET");
                    } else {
                        this.CS.setRequestMethod("POST");
                        this.CS.setDoOutput(true);
                        this.CS.setDoInput(true);
                        this.CS.setRequestProperty("Accept", "*/*");
                        this.CS.setRequestProperty("Accept-Charset", "utf-8");
                        this.CS.setRequestProperty("Content-Type", "application/octet-stream");
                    }
                    -l_2_I = 0;
                    return -l_2_I;
                }
                mb.s(TAG, "[http_control]start() no network");
                return -220000;
            } catch (Object -l_5_R2) {
                -l_5_R2.printStackTrace();
                -l_2_I = -520000;
            } catch (Object -l_5_R22) {
                -l_5_R22.printStackTrace();
                -l_2_I = -240000;
            } catch (Object -l_5_R222) {
                -l_5_R222.printStackTrace();
                -l_2_I = -440000;
            } catch (Object -l_5_R2222) {
                -l_5_R2222.printStackTrace();
                -l_2_I = -140000;
            }
        } catch (Object -l_5_R22222) {
            -l_5_R22222.printStackTrace();
            mb.o(TAG, "[http_control]start() MalformedURLException e:" + -l_5_R22222.toString());
            return -510000;
        }
    }

    private byte[] d(InputStream inputStream) throws NetWorkException {
        Object -l_4_R = new byte[2048];
        Object -l_5_R = new ByteArrayOutputStream();
        while (true) {
            try {
                int -l_2_I = inputStream.read(-l_4_R);
                if (-l_2_I == -1) {
                    break;
                }
                -l_5_R.write(-l_4_R, 0, -l_2_I);
            } catch (Object -l_6_R) {
                throw new NetWorkException(-56, "get Bytes from inputStream when read buffer: " + -l_6_R.getMessage());
            } catch (Throwable th) {
                try {
                    -l_5_R.close();
                } catch (Object -l_8_R) {
                    mb.e(TAG, -l_8_R);
                }
            }
        }
        Object -l_3_R = -l_5_R.toByteArray();
        try {
            -l_5_R.close();
        } catch (Object -l_6_R2) {
            mb.e(TAG, -l_6_R2);
        }
        return -l_3_R;
    }

    private String eM() {
        mb.d(TAG, "[http_control]getRedirectUrl()");
        try {
            return this.CS.getHeaderField("Location");
        } catch (Object -l_1_R) {
            mb.o(TAG, "getRedirectUrl() e: " + -l_1_R.toString());
            return null;
        }
    }

    private boolean fv() {
        mb.n(TAG, "[http_control]stop()");
        if (this.CS == null) {
            return false;
        }
        try {
            this.CS.disconnect();
            this.CS = null;
        } catch (Throwable th) {
        }
        return true;
    }

    private void fw() {
        this.CV = null;
        this.CW = 0;
    }

    synchronized int a(f fVar, byte[] bArr, AtomicReference<byte[]> atomicReference) {
        if (bArr == null || fVar == null) {
            return -10;
        }
        int -l_4_I;
        int -l_5_I;
        int -l_6_I;
        int -l_7_I;
        Object -l_10_R;
        mb.n(TAG, "[http_control]sendData()");
        if (fVar.Fh == 2048) {
            if (!this.CX) {
                -l_4_I = 1;
                -l_5_I = 3;
                if (-l_4_I != 0) {
                    -l_5_I = 1;
                }
                -l_6_I = -1;
                -l_7_I = 0;
                while (-l_7_I < -l_5_I) {
                    if (eb.iH == i.iG()) {
                        mb.s(TAG, "[http_control]sendData() no network");
                        return -220000;
                    } else if (fVar.gp()) {
                        mb.d(TAG, "[http_control][time_out]sendData(), send time out");
                        return -17;
                    } else if (lw.eJ() == 0) {
                        if (-l_4_I != 0) {
                            -l_10_R = this.CU.fJ();
                        } else {
                            -l_10_R = nj.a(this.CT);
                            if (-l_10_R != null) {
                                if (-l_10_R.length() < "http://".length() || !-l_10_R.substring(0, "http://".length()).equalsIgnoreCase("http://")) {
                                    -l_10_R = "http://" + -l_10_R;
                                }
                            }
                        }
                        -l_6_I = cb(-l_10_R);
                        mb.n(TAG, "[http_control]start(), ret: " + -l_6_I + " httpUrl: " + -l_10_R);
                        if (-l_6_I == 0) {
                            fVar.Fw = true;
                            -l_6_I = a(bArr, atomicReference);
                        }
                        fv();
                        if (-l_6_I == 0 || -l_6_I == -220000) {
                            mb.n(TAG, "[http_control]sendData() 发包成功或无网络，不重试， ret: " + -l_6_I);
                            break;
                        } else if (-l_6_I != -60000 && nu.ch("http send")) {
                            -l_6_I = -160000;
                            mb.n(TAG, "[http_control]sendData() 需要wifi认证，不重试");
                            break;
                        } else {
                            if (-l_4_I == 0 && -l_6_I != -60000) {
                                this.CU.C(false);
                            }
                            if (-l_7_I >= -l_5_I - 1) {
                                try {
                                    Thread.sleep(300);
                                } catch (Object -l_11_R) {
                                    mb.o(TAG, "[http_control]sendData() InterruptedException e: " + -l_11_R.toString());
                                }
                            }
                            -l_7_I++;
                        }
                    } else {
                        return -7;
                    }
                }
                mb.d(TAG, "[http_control]sendData() ret: " + -l_6_I);
                return -l_6_I;
            }
        }
        -l_4_I = 0;
        -l_5_I = 3;
        if (-l_4_I != 0) {
            -l_5_I = 1;
        }
        -l_6_I = -1;
        -l_7_I = 0;
        while (-l_7_I < -l_5_I) {
            if (eb.iH == i.iG()) {
                if (fVar.gp()) {
                    if (lw.eJ() == 0) {
                        if (-l_4_I != 0) {
                            -l_10_R = nj.a(this.CT);
                            if (-l_10_R != null) {
                                if (-l_10_R.length() < "http://".length()) {
                                }
                                -l_10_R = "http://" + -l_10_R;
                            }
                        } else {
                            -l_10_R = this.CU.fJ();
                        }
                        -l_6_I = cb(-l_10_R);
                        mb.n(TAG, "[http_control]start(), ret: " + -l_6_I + " httpUrl: " + -l_10_R);
                        if (-l_6_I == 0) {
                            fVar.Fw = true;
                            -l_6_I = a(bArr, atomicReference);
                        }
                        fv();
                        if (-l_6_I == 0) {
                            if (-l_6_I != -60000) {
                                -l_6_I = -160000;
                                mb.n(TAG, "[http_control]sendData() 需要wifi认证，不重试");
                                break;
                            }
                            this.CU.C(false);
                            if (-l_7_I >= -l_5_I - 1) {
                                Thread.sleep(300);
                            }
                            -l_7_I++;
                        }
                        mb.n(TAG, "[http_control]sendData() 发包成功或无网络，不重试， ret: " + -l_6_I);
                        break;
                    }
                    return -7;
                }
                mb.d(TAG, "[http_control][time_out]sendData(), send time out");
                return -17;
            }
            mb.s(TAG, "[http_control]sendData() no network");
            return -220000;
        }
        mb.d(TAG, "[http_control]sendData() ret: " + -l_6_I);
        return -l_6_I;
    }
}
