package tmsdkobf;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.InflaterInputStream;
import tmsdk.common.ErrorCode;
import tmsdk.common.exception.NetWorkException;
import tmsdk.common.exception.NetworkOnMainThreadException;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.i;
import tmsdk.common.utils.n;

public final class lw {
    private static volatile boolean zp = false;
    private static volatile String zq = null;
    private static volatile boolean zr = false;
    private static volatile boolean zs = false;
    private static volatile long zt = 0;
    private static volatile long zu = 0;
    private static Object zv = new Object();
    private static volatile String zw;
    private static ArrayList<Pair<Integer, Long>> zx = new ArrayList();
    private byte[] mPostData;
    private String mUrl;
    private String yY;
    private int yZ;
    private String zb = "GET";
    private HttpURLConnection zc;
    private int zd = -1;
    private Hashtable<String, String> ze = new Hashtable(0);
    private boolean zf = false;
    private boolean zg = true;
    private byte zh = (byte) 0;
    private byte zi = (byte) 0;
    private byte zj = (byte) 0;
    private int zk = 30000;
    private int zl = 30000;
    private boolean zm = true;
    private boolean zn;
    private boolean zo;

    private lw(String str) {
        this.mUrl = str;
        eI();
    }

    private void a(String str, eb ebVar) throws NetWorkException {
        Object -l_4_R;
        try {
            getHostAddress();
            if (eb.iH != ebVar) {
                if (eb.iK != ebVar) {
                    long -l_4_J = System.currentTimeMillis();
                    this.zc = (HttpURLConnection) new URL(str).openConnection();
                    mb.n("HttpConnection", "initConnection() openTimeMillis: " + (System.currentTimeMillis() - -l_4_J));
                    this.zf = false;
                    this.zc.setReadTimeout(this.zl);
                    this.zc.setConnectTimeout(this.zk);
                    return;
                }
                -l_4_R = new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(i.iI(), i.iJ()));
                long -l_5_J = System.currentTimeMillis();
                this.zc = (HttpURLConnection) new URL(str).openConnection(-l_4_R);
                mb.n("HttpConnection", "initConnection() proxy openTimeMillis: " + (System.currentTimeMillis() - -l_5_J));
                this.zf = true;
            }
        } catch (Object -l_4_R2) {
            throw new NetWorkException(-1057, "IllegalArgumentException : " + -l_4_R2.getMessage());
        } catch (Object -l_4_R22) {
            throw new NetWorkException(-1058, "SecurityException: " + -l_4_R22.getMessage());
        } catch (Object -l_4_R222) {
            throw new NetWorkException(-1059, "UnsupportedOperationException: " + -l_4_R222.getMessage());
        } catch (Object -l_4_R2222) {
            throw new NetWorkException(-1056, "IOException : " + -l_4_R2222.getMessage());
        }
    }

    private void a(String str, boolean z, String str2, int i) throws NetWorkException {
        try {
            getHostAddress();
            if (z) {
                if (str2 == null) {
                    str2 = "10.0.0.172";
                }
                if (i < 0) {
                    i = 80;
                }
                this.zc = (HttpURLConnection) new URL(str).openConnection(new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(str2, i)));
                this.zf = true;
            } else {
                this.zc = (HttpURLConnection) new URL(str).openConnection();
                this.zf = false;
            }
            this.zc.setReadTimeout(30000);
            this.zc.setConnectTimeout(this.zk);
        } catch (Object -l_6_R) {
            throw new NetWorkException(-1057, "IllegalArgumentException : " + -l_6_R.getMessage());
        } catch (Object -l_6_R2) {
            throw new NetWorkException(-1058, "SecurityException: " + -l_6_R2.getMessage());
        } catch (Object -l_6_R22) {
            throw new NetWorkException(-1059, "UnsupportedOperationException: " + -l_6_R22.getMessage());
        } catch (Object -l_6_R222) {
            throw new NetWorkException(-1056, "IOException : " + -l_6_R222.getMessage());
        }
    }

    public static lw bO(String str) throws NetWorkException {
        return e(str, true);
    }

    private byte[] d(InputStream inputStream) throws NetWorkException {
        Object -l_2_R = new BufferedInputStream(inputStream);
        Object -l_3_R = new byte[2048];
        Object -l_4_R = new ByteArrayOutputStream();
        while (true) {
            try {
                int -l_5_I = inputStream.read(-l_3_R);
                if (-l_5_I == -1) {
                    break;
                }
                -l_4_R.write(-l_3_R, 0, -l_5_I);
            } catch (Object -l_7_R) {
                throw new NetWorkException(-56, "get Bytes from inputStream when read buffer: " + -l_7_R.getMessage());
            } catch (Throwable th) {
                try {
                    -l_2_R.close();
                } catch (Object -l_9_R) {
                    -l_9_R.printStackTrace();
                }
                try {
                    -l_4_R.close();
                } catch (Object -l_9_R2) {
                    -l_9_R2.printStackTrace();
                }
            }
        }
        Object -l_6_R = -l_4_R.toByteArray();
        try {
            -l_2_R.close();
        } catch (Object -l_7_R2) {
            -l_7_R2.printStackTrace();
        }
        try {
            -l_4_R.close();
        } catch (Object -l_7_R22) {
            -l_7_R22.printStackTrace();
        }
        return -l_6_R;
    }

    public static lw e(String str, boolean z) throws NetWorkException {
        eI();
        if (!i.iK() && Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
            throw new NetworkOnMainThreadException();
        } else if (str == null || str.length() == 0) {
            throw new NetWorkException((int) ErrorCode.ERR_OPEN_CONNECTION, "url is null!");
        } else {
            Object -l_3_R = new lw(str);
            -l_3_R.zn = false;
            -l_3_R.zm = z;
            eb -l_4_R = i.iG();
            if (eb.iH != -l_4_R) {
                -l_3_R.a(-l_3_R.mUrl, -l_4_R);
                return -l_3_R;
            }
            throw new NetWorkException(-1052, "no connecition!");
        }
    }

    public static void eI() {
        Object -l_0_R = new md("wup");
        zu = -l_0_R.getLong("dnc", 0);
        zw = -l_0_R.getString("cn_t_a", "");
        if (!TextUtils.isEmpty(zw)) {
            Object -l_1_R = zw.split("\\|");
            for (Object -l_2_R : -l_1_R) {
                if (!TextUtils.isEmpty(-l_2_R)) {
                    Object -l_4_R = -l_2_R.split(",");
                    try {
                        synchronized (zx) {
                            if (zx.size() <= 4) {
                                zx.add(new Pair(Integer.valueOf(-l_4_R[0]), Long.valueOf(-l_4_R[1])));
                            }
                        }
                    } catch (Object -l_5_R) {
                        -l_5_R.printStackTrace();
                    }
                }
            }
        }
    }

    public static boolean eJ() {
        mb.d("HttpConnection", " couldNotConnect()");
        synchronized (zv) {
            long -l_1_J = System.currentTimeMillis();
            long -l_3_J = zu - -l_1_J;
            mb.d("HttpConnection", " couldNotConnect() diff: " + -l_3_J);
            int -l_5_I = !((-l_3_J > 5184000 ? 1 : (-l_3_J == 5184000 ? 0 : -1)) <= 0) ? 1 : 0;
            int -l_6_I = !(((zu - -l_1_J) > 0 ? 1 : ((zu - -l_1_J) == 0 ? 0 : -1)) < 0) ? 1 : 0;
            if (-l_5_I == 0 && -l_6_I != 0) {
                mb.d("HttpConnection", " couldNotConnect() true");
                return true;
            }
            mb.d("HttpConnection", " couldNotConnect() false");
            return false;
        }
    }

    private int eL() throws NetWorkException {
        close();
        if (this.zn) {
            a(this.mUrl, this.zo, this.yY, this.yZ);
        } else if (i.iG().value() != 0) {
            a(this.mUrl, i.iG());
        } else {
            throw new NetWorkException(-1052, "no connecition!");
        }
        setRequestMethod(this.zb);
        if ("POST".equalsIgnoreCase(this.zb) && this.mPostData != null) {
            setPostData(this.mPostData);
        }
        a(this.ze);
        return eK();
    }

    private boolean isConnected() {
        return this.zd == SmsCheckResult.ESCT_200 || this.zd == SmsCheckResult.ESCT_206;
    }

    private String[] split(String str) {
        Object -l_2_R = new String[2];
        int -l_3_I = str.indexOf("://");
        if (-1 != -l_3_I) {
            str = str.substring(-l_3_I + 3);
        }
        -l_3_I = str.indexOf("/");
        if (-1 == -l_3_I) {
            -l_2_R[0] = str;
            -l_2_R[1] = "";
        } else {
            -l_2_R[0] = str.substring(0, -l_3_I);
            -l_2_R[1] = str.substring(-l_3_I);
        }
        return -l_2_R;
    }

    public int a(boolean z, AtomicReference<byte[]> atomicReference) throws NetWorkException {
        Object -l_5_R;
        if (this.zc == null || !isConnected()) {
            return ErrorCode.ERR_RESPONSE;
        }
        if (z) {
            -l_5_R = new InflaterInputStream(this.zc.getInputStream());
        } else {
            try {
                -l_5_R = this.zc.getInputStream();
            } catch (Object -l_5_R2) {
                throw new NetWorkException(-l_5_R2.getErrCode() + ErrorCode.ERR_RESPONSE, "get response exception : " + -l_5_R2.getMessage());
            } catch (Object -l_5_R22) {
                throw new NetWorkException(-4002, "get response exception : " + -l_5_R22.getMessage());
            }
        }
        atomicReference.set(d(-l_5_R22));
        return 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void a(Hashtable<String, String> hashtable) {
        if (!(hashtable == null || hashtable.size() == 0 || this.zc == null)) {
            for (Entry -l_4_R : hashtable.entrySet()) {
                this.zc.setRequestProperty((String) -l_4_R.getKey(), (String) -l_4_R.getValue());
            }
        }
    }

    public void close() {
        if (this.zc != null) {
            this.zc.disconnect();
            this.zc = null;
        }
    }

    public int eK() throws NetWorkException {
        byte b;
        Object -l_3_R;
        int -l_1_I = eJ();
        mb.d("sendRequest", " sendRequest() couldNotConnect:" + -l_1_I);
        if (-l_1_I == 0) {
            int -l_2_I = 0;
            try {
                if (n.iX() < 8) {
                    System.setProperty("http.keepAlive", "false");
                }
                this.zc.setUseCaches(false);
                this.zc.setRequestProperty("Pragma", "no-cache");
                this.zc.setRequestProperty("Cache-Control", "no-cache");
                this.zc.setInstanceFollowRedirects(false);
                if ("GET".equalsIgnoreCase(this.zb)) {
                    -l_2_I = ErrorCode.ERR_GET;
                    this.zc.setRequestMethod("GET");
                } else {
                    -l_2_I = ErrorCode.ERR_POST;
                    this.zc.setRequestMethod("POST");
                    if (!this.ze.containsKey("Accept")) {
                        this.zc.setRequestProperty("Accept", "*/*");
                        this.zc.setRequestProperty("Accept-Charset", "utf-8");
                    }
                    this.zc.setDoOutput(true);
                    this.zc.setDoInput(true);
                    if (this.mPostData != null) {
                        if (!this.ze.containsKey("Content-Type")) {
                            this.zc.setRequestProperty("Content-Type", "application/octet-stream");
                        }
                        this.zc.setRequestProperty("Content-length", "" + this.mPostData.length);
                        long -l_3_J = System.currentTimeMillis();
                        Object -l_5_R = this.zc.getOutputStream();
                        long -l_8_J = System.currentTimeMillis() - -l_3_J;
                        mb.n("HttpConnection", "sendRequest() connectTimeMillis: " + -l_8_J);
                        if ((-l_8_J <= 0 ? 1 : null) == null) {
                            if ((-l_8_J >= 60000 ? 1 : null) == null && zx.size() <= 4) {
                                synchronized (zx) {
                                    zx.add(new Pair(Integer.valueOf(ln.yR), Long.valueOf(-l_8_J)));
                                    if (4 == zx.size()) {
                                        Object -l_12_R = new StringBuilder();
                                        for (int -l_13_I = 0; -l_13_I < zx.size(); -l_13_I++) {
                                            Pair -l_11_R = (Pair) zx.get(-l_13_I);
                                            if (-l_11_R != null) {
                                                -l_12_R.append(-l_11_R.first);
                                                -l_12_R.append(",");
                                                -l_12_R.append(-l_11_R.second);
                                                if (zx.size() - 1 != -l_13_I) {
                                                    -l_12_R.append("|");
                                                }
                                            }
                                        }
                                        zw = -l_12_R.toString();
                                        new md("wup").a("cn_t_a", zw, true);
                                        mb.n("HttpConnection", "sendRequest() mConnectTimeMillisAll: " + zw);
                                    }
                                }
                            }
                        }
                        -l_5_R.write(this.mPostData);
                        -l_5_R.flush();
                        -l_5_R.close();
                    }
                }
                this.zd = this.zc.getResponseCode();
                mb.d("HttpConnection", "HttpUrlConn.getResponseCode : " + this.zd);
                if (this.zd >= 301) {
                    if (this.zd <= 305) {
                        b = this.zh;
                        this.zh = (byte) ((byte) (b + 1));
                        if (b < (byte) 3) {
                            this.mUrl = eM();
                            return eL();
                        }
                        if (this.zd == 206 || this.zd == 200) {
                            return this.zd;
                        }
                        b = this.zj;
                        this.zj = (byte) ((byte) (b + 1));
                        if (b < (byte) 2) {
                            throw new NetWorkException(-l_2_I + this.zd, "response code is unnormal: " + this.zd + " SDK Version:" + n.iX());
                        }
                        if (-l_2_I == -1) {
                            if ("true".equals(System.getProperty("http.keepAlive"))) {
                                System.setProperty("http.keepAlive", "false");
                            }
                        }
                        return eL();
                    }
                }
                if (this.zd == 200) {
                    -l_3_R = getContentType();
                    if (!(!this.zf || -l_3_R == null || -l_3_R.toLowerCase().indexOf("vnd.wap.wml") == -1)) {
                        b = this.zi;
                        this.zi = (byte) ((byte) (b + 1));
                        if (b < (byte) 1) {
                            return eL();
                        }
                    }
                }
                if (this.zd == 206) {
                    b = this.zj;
                    this.zj = (byte) ((byte) (b + 1));
                    if (b < (byte) 2) {
                        if (-l_2_I == -1) {
                            if ("true".equals(System.getProperty("http.keepAlive"))) {
                                System.setProperty("http.keepAlive", "false");
                            }
                        }
                        return eL();
                    }
                    throw new NetWorkException(-l_2_I + this.zd, "response code is unnormal: " + this.zd + " SDK Version:" + n.iX());
                }
                return this.zd;
            } catch (Object -l_3_R2) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b >= (byte) 2) {
                    if (this.zg) {
                        this.zg = false;
                        close();
                        if (this.zf) {
                            a(this.mUrl, eb.iL);
                        } else if (eb.iJ == i.iG()) {
                            Object -l_4_R = i.iI();
                            if (-l_4_R != null && -l_4_R.length() > 0 && i.iJ() > 0) {
                                a(this.mUrl, eb.iK);
                            } else {
                                throw new NetWorkException(-l_2_I - 62, "sendRequest UnknownHostException: " + -l_3_R2.getMessage() + " networktype:" + i.iG());
                            }
                        }
                        if (this.zc != null) {
                            setRequestMethod(this.zb);
                            if ("POST".equalsIgnoreCase(this.zb) && this.mPostData != null) {
                                setPostData(this.mPostData);
                            }
                            a(this.ze);
                            return eK();
                        }
                    }
                    throw new NetWorkException(-l_2_I - 62, "sendRequest UnknownHostException: " + -l_3_R2.getMessage() + " networktype:" + i.iG());
                }
                getHostAddress();
                return eL();
            } catch (Object -l_3_R22) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b < (byte) 2) {
                    return eL();
                }
                throw new NetWorkException(-l_2_I - 60, "sendRequest IllegalAccessError: " + -l_3_R22.getMessage());
            } catch (Object -l_3_R222) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b < (byte) 2) {
                    return eL();
                }
                throw new NetWorkException(-l_2_I - 61, "sendRequest IllegalStateException: " + -l_3_R222.getMessage());
            } catch (Object -l_3_R2222) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b < (byte) 2) {
                    return eL();
                }
                throw new NetWorkException(-l_2_I - 51, "sendRequest ProtocolException: " + -l_3_R2222.getMessage());
            } catch (Object -l_3_R22222) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b < (byte) 2) {
                    return eL();
                }
                throw new NetWorkException(-l_2_I - 51, "sendRequest ClientProtocolException: " + -l_3_R22222.getMessage());
            } catch (Object -l_3_R222222) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b < (byte) 2) {
                    return eL();
                }
                throw new NetWorkException(-l_2_I - 54, "sendRequest SocketException: " + -l_3_R222222.getMessage());
            } catch (Object -l_3_R2222222) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b >= (byte) 2) {
                    throw new NetWorkException(-l_2_I - 55, "sendRequest" + -l_3_R2222222.getMessage());
                }
                this.zk = 60000;
                this.zl = 60000;
                return eL();
            } catch (Object -l_3_R22222222) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b < (byte) 2) {
                    return eL();
                }
                throw new NetWorkException(-l_2_I - 56, "sendRequest NetWorkException: " + -l_3_R22222222.getMessage());
            } catch (Object -l_3_R222222222) {
                b = this.zj;
                this.zj = (byte) ((byte) (b + 1));
                if (b < (byte) 2) {
                    return eL();
                }
                throw new NetWorkException(-l_2_I, "sendRequest " + -l_3_R222222222.getClass().getName() + " : " + -l_3_R222222222.getMessage());
            }
        }
        throw new NetWorkException(ErrorCode.ERR_OPEN_CONNECTION, "sendRequest() is forbidden couldNotConnect");
    }

    public String eM() throws NetWorkException {
        try {
            return this.zc.getHeaderField("Location");
        } catch (Object -l_1_R) {
            throw new NetWorkException(-56, "get redirect url: " + -l_1_R.getMessage());
        }
    }

    public String getContentType() throws NetWorkException {
        try {
            return this.zc.getHeaderField("Content-Type");
        } catch (Object -l_1_R) {
            throw new NetWorkException(-56, "get content type: " + -l_1_R.getMessage());
        }
    }

    public String getHostAddress() {
        if (this.mUrl == null) {
            return "";
        }
        Object -l_1_R = this.zc == null ? split(this.mUrl)[0] : this.zc.getURL().getHost();
        if (-l_1_R == null || -l_1_R.length() == 0) {
            if (this.zc == null) {
                -l_1_R = split(this.mUrl)[0];
            }
        }
        Object -l_2_R;
        try {
            -l_2_R = InetAddress.getByName(-l_1_R);
            if (-l_2_R != null) {
                return -l_2_R.getHostAddress();
            }
        } catch (Object -l_2_R2) {
            -l_2_R2.printStackTrace();
        } catch (Object -l_2_R22) {
            -l_2_R22.printStackTrace();
        }
        return "";
    }

    public int getResponseCode() {
        return this.zd;
    }

    public void setPostData(byte[] bArr) {
        this.mPostData = bArr;
    }

    public void setRequestMethod(String str) {
        String str2;
        this.zb = str;
        if ("GET".equalsIgnoreCase(str)) {
            str2 = "GET";
        } else if ("POST".equalsIgnoreCase(str)) {
            str2 = "POST";
        } else {
            return;
        }
        this.zb = str2;
    }
}
