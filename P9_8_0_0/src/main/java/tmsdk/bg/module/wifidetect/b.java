package tmsdk.bg.module.wifidetect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.qq.taf.jce.JceStruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import tmsdk.bg.creator.BaseManagerB;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.ScriptHelper;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;
import tmsdk.common.utils.n;
import tmsdk.common.utils.q;
import tmsdk.common.utils.t;
import tmsdkobf.db;
import tmsdkobf.dc;
import tmsdkobf.dd;
import tmsdkobf.im;
import tmsdkobf.jy;
import tmsdkobf.kr;
import tmsdkobf.kt;
import tmsdkobf.kz;
import tmsdkobf.lf;
import tmsdkobf.lg;
import tmsdkobf.ob;

class b extends BaseManagerB {
    private Context mContext;
    private Handler vW;
    private WifiManager wR;
    private ob wS;

    b() {
    }

    private String a(InputStream inputStream) {
        String -l_2_R = null;
        Object -l_3_R = b(inputStream);
        f.d("WifiDetectManager", "parsePage-pageContent:[" + -l_3_R + "]");
        Object -l_4_R = new String[]{"http-equiv\\s*=\\s*[\"']*refresh[\"']*\\s*content\\s*=\\s*[\"']*[^;]*;\\s*url\\s*=\\s*[\"']*([^\"'\\s>]+)", "[^\\w](?:location.href\\s*=|location\\s*=|location.replace\\s*\\()\\s*[\"']*([^\"'>]+)", "<NextURL>([^<]+)", "\\s+action\\s*=\\s*[\"']*([^\"'>]+)[\"'>\\s]*.*submit", "<LoginURL>([^<]+)"};
        int -l_5_I = -l_4_R.length;
        for (int -l_7_I = 0; -l_7_I < -l_5_I && -l_2_R == null; -l_7_I++) {
            Object -l_6_R = Pattern.compile(-l_4_R[-l_7_I], 2).matcher(-l_3_R);
            while (-l_6_R.find() && r0 == null) {
                -l_2_R = -l_6_R.group(-l_6_R.groupCount());
                if (!(-l_2_R == null || -l_2_R.trim().toLowerCase().startsWith("http"))) {
                    -l_2_R = null;
                }
            }
        }
        f.d("WifiDetectManager", "parsePage-location:[" + -l_2_R + "]");
        return -l_2_R;
    }

    private String a(String str, HttpURLConnection httpURLConnection) {
        String str2 = null;
        InputStream inputStream = null;
        try {
            Object -l_5_R = new URL(str).getHost();
            Object -l_6_R = httpURLConnection.getURL().getHost();
            f.d("WifiDetectManager", "urlHost:[" + -l_5_R + "]httpHost:[" + -l_6_R + "]");
            if (!-l_5_R.equals(-l_6_R)) {
                str2 = httpURLConnection.getURL().toExternalForm();
            }
            if (str2 == null && httpURLConnection.getHeaderField("Location") != null) {
                str2 = httpURLConnection.getHeaderField("Location");
                f.d("WifiDetectManager", "111location:[" + str2 + "]");
            }
            if (str2 == null) {
                if (httpURLConnection.getHeaderField("Refresh") != null) {
                    Object -l_7_R = httpURLConnection.getHeaderField("Refresh").split(";");
                    if (-l_7_R.length == 2) {
                        str2 = -l_7_R[1].trim();
                    }
                    f.d("WifiDetectManager", "222location:[" + str2 + "]");
                }
            }
            if (str2 == null) {
                inputStream = httpURLConnection.getInputStream();
                if (inputStream != null) {
                    str2 = a(inputStream);
                }
                f.d("WifiDetectManager", "333location:[" + str2 + "]");
            }
            if (-l_4_R != null) {
                try {
                    -l_4_R.close();
                } catch (IOException e) {
                    return str2;
                }
            }
        } catch (IOException e2) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    return str2;
                }
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                }
            }
        }
        return str2;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String b(InputStream inputStream) {
        Object -l_2_R = new BufferedReader(new InputStreamReader(inputStream));
        Object -l_3_R = new StringBuilder();
        while (true) {
            try {
                Object -l_4_R = -l_2_R.readLine();
                if (-l_4_R == null) {
                    try {
                        break;
                    } catch (Object -l_5_R) {
                        -l_5_R.printStackTrace();
                    }
                } else {
                    -l_3_R.append(-l_4_R);
                }
            } catch (Object -l_5_R2) {
                -l_5_R2.printStackTrace();
            } catch (Throwable th) {
                try {
                    -l_2_R.close();
                } catch (Object -l_7_R) {
                    -l_7_R.printStackTrace();
                }
            }
        }
        -l_2_R.close();
        return -l_3_R.toString();
    }

    private void m(String str, String str2) {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        Object -l_5_R;
        try {
            -l_5_R = TMSDKContext.getCurrentContext();
            if (-l_5_R == null) {
                -l_5_R = this.mContext;
            }
            Object -l_3_R = -l_5_R.getAssets().open(str, 1);
            Object -l_4_R = -l_5_R.openFileOutput(str2, 0);
            Object -l_7_R = new byte[8192];
            while (true) {
                int -l_8_I = -l_3_R.read(-l_7_R);
                if (-l_8_I <= 0) {
                    break;
                }
                -l_4_R.write(-l_7_R, 0, -l_8_I);
            }
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (IOException e) {
                }
            }
            if (-l_4_R != null) {
                try {
                    -l_4_R.close();
                } catch (IOException e2) {
                }
            }
        } catch (Object -l_5_R2) {
            f.d("WifiDetectManager", "IOException:[" + -l_5_R2 + "]");
            -l_5_R2.printStackTrace();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e4) {
                }
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e5) {
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e6) {
                }
            }
        }
    }

    public int detectARP(String str) {
        int -l_3_I = 0;
        kt.aE(29978);
        f.d("WifiDetectManager", "[Beg]detectARP-binaryName:[" + str + "]");
        if (q.cK(str)) {
            f.d("WifiDetectManager", "[End]detectARP 参数不对");
            return -2;
        }
        int -l_2_I = 261;
        f.d("WifiDetectManager", "[Beg]ScriptHelper.acquireRoot");
        if (ScriptHelper.acquireRoot() == 0) {
            -l_3_I = 1;
        }
        f.d("WifiDetectManager", "[End]ScriptHelper.acquireRoot:[" + -l_3_I + "]");
        if (-l_3_I != 0) {
            final Object -l_4_R = a.dw();
            Object -l_5_R = new File(this.mContext.getFilesDir(), str);
            if (!-l_5_R.exists()) {
                f.h("WifiDetectManager", "从包的asset中copy[" + str + "]to[" + -l_5_R.getAbsolutePath() + "]");
                m(str, str);
            }
            final Object -l_6_R = -l_5_R.getAbsolutePath();
            if (-l_5_R.exists()) {
                Object -l_7_R = new Thread(new Runnable(this) {
                    final /* synthetic */ b wT;

                    public void run() {
                        try {
                            Thread.sleep(500);
                        } catch (Object -l_1_R) {
                            -l_1_R.printStackTrace();
                        }
                        -l_4_R.bp(-l_6_R);
                    }
                });
                -l_7_R.setName("nativeArp");
                -l_7_R.start();
                -l_2_I = -l_4_R.dx();
            } else {
                f.h("WifiDetectManager", "binaryFile not exist:[" + -l_6_R + "]");
                return -3;
            }
        }
        f.d("WifiDetectManager", "[End]detectARP-nRetState:[" + -l_2_I + "]");
        if (-l_2_I == 262) {
            kt.e(1320068, "ARP_FAKE");
        }
        return -l_2_I;
    }

    public int detectDnsAndPhishing(IWifiDetectListener iWifiDetectListener, long j) {
        kt.aE(29979);
        if (i.K(this.mContext)) {
            f.d("WifiDetectManager", "[Beg]detectDnsAndPhishinglistener:[" + iWifiDetectListener + "]");
            f.d("WifiDetectManager", "[GUID] " + this.wS.b());
            Object -l_5_R = ((WifiManager) this.mContext.getSystemService("wifi")).getConnectionInfo();
            if (-l_5_R == null) {
                return -2;
            }
            Object -l_6_R = -l_5_R.getSSID();
            Object -l_7_R = -l_5_R.getBSSID();
            f.d("WifiDetectManager", "ssid:[" + -l_6_R + "]bssid:[" + -l_7_R + "]");
            if (q.cK(-l_6_R) || q.cK(-l_7_R)) {
                f.d("WifiDetectManager", "[End]detectDnsAndPhishing 参数不对");
                return -3;
            }
            String str = null;
            String -l_10_R = null;
            try {
                Object -l_11_R = this.wR.getDhcpInfo();
                if (-l_11_R != null) {
                    str = t.I((long) -l_11_R.dns1);
                    -l_10_R = t.I((long) -l_11_R.dns2);
                }
            } catch (Exception e) {
            }
            f.d("WifiDetectManager", "dns1:[" + str + "]dns2:[" + -l_10_R + "]");
            JceStruct -l_11_R2 = new db();
            -l_11_R2.gj = new dd();
            -l_11_R2.gj.go = -l_6_R;
            -l_11_R2.gj.gp = -l_7_R;
            -l_11_R2.gk = new ArrayList();
            if (str != null && str.length() > 0) {
                -l_11_R2.gk.add(str);
            }
            if (-l_10_R != null && -l_10_R.length() > 0) {
                -l_11_R2.gk.add(-l_10_R);
            }
            final IWifiDetectListener iWifiDetectListener2 = iWifiDetectListener;
            this.wS.a(794, -l_11_R2, new dc(), 0, new jy(this) {
                final /* synthetic */ b wT;

                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    int -l_7_I;
                    f.d("WifiDetectManager", "onFinish retCode:[" + i3 + "]" + "dataRetCode:[" + i4 + "]");
                    Object -l_6_R = this.wT.vW.obtainMessage(4097);
                    if (i3 == 0 && jceStruct != null && (jceStruct instanceof dc)) {
                        dc -l_8_R = (dc) jceStruct;
                        if (-l_8_R.gn == 2) {
                            f.d("WifiDetectManager", "[CLOUND_CHECK_DNS_FAKE]ESafeType.EST_DnsException");
                            kt.e(1320068, "DNS_FAKE");
                            -l_7_I = 18;
                        } else if (-l_8_R.gn != 3) {
                            f.d("WifiDetectManager", "[CLOUND_CHECK_NO_FAKE]safeType:[" + -l_8_R.gn + "]");
                            -l_7_I = 17;
                        } else {
                            f.d("WifiDetectManager", "[CLOUND_CHECK_PHISHING_FAKE]ESafeType.EST_Phishing");
                            kt.e(1320068, "PHISHING_FAKE");
                            -l_7_I = 19;
                        }
                    } else {
                        f.d("WifiDetectManager", "[CLOUND_CHECK_NETWORK_ERROR]");
                        -l_7_I = 16;
                    }
                    -l_6_R.arg1 = -l_7_I;
                    -l_6_R.obj = iWifiDetectListener2;
                    this.wT.vW.sendMessage(-l_6_R);
                }
            }, j);
            kr.dz();
            Object -l_12_R = kz.aJ(90);
            if (-l_12_R != null && -l_12_R.xZ) {
                lf.bG(-l_7_R);
            }
            -l_12_R = kz.aJ(33);
            if (-l_12_R != null && -l_12_R.xZ) {
                lg.ev();
                lg.eq();
            }
            f.d("WifiDetectManager", "[End]detectDnsAndPhishing-ssid:[" + -l_6_R + "]bssid:[" + -l_7_R + "]");
            return 0;
        }
        f.d("WifiDetectManager", "[WifiConnected false]");
        iWifiDetectListener.onResult(-1);
        return -1;
    }

    public int detectSecurity(ScanResult scanResult) {
        kt.aE(29980);
        int -l_2_I = 256;
        f.d("WifiDetectManager", "[Beg]detectSecurity-AP:[" + scanResult + "]");
        if (scanResult == null || scanResult.capabilities == null) {
            return -2;
        }
        if (scanResult.capabilities.contains("WEP")) {
            -l_2_I = 257;
        } else if (scanResult.capabilities.contains("PSK")) {
            -l_2_I = 258;
        } else if (scanResult.capabilities.contains("EAP")) {
            -l_2_I = 259;
        }
        f.d("WifiDetectManager", "[End]detectSecurity-Ret:[" + -l_2_I + "]");
        return -l_2_I;
    }

    public int getSingletonType() {
        return 2;
    }

    public int l(String str, String str2) {
        kt.aE(29981);
        int -l_3_I = 2;
        f.d("WifiDetectManager", "[Beg]detectNetworkState-urlApprove:[" + str + "]customHeader:[" + str2 + "]");
        Object obj = null;
        int -l_5_I = 0;
        HttpURLConnection httpURLConnection = null;
        try {
            long -l_7_J = System.currentTimeMillis();
            f.d("WifiDetectManager", "openConnection");
            httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            if (n.iX() < 8) {
                System.setProperty("http.keepAlive", "false");
            }
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestProperty("Pragma", "no-cache");
            httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
            httpURLConnection.setInstanceFollowRedirects(false);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setReadTimeout(30000);
            int -l_9_I = httpURLConnection.getResponseCode();
            f.d("WifiDetectManager", "getResponseCode:[" + -l_9_I + "]timeMillis:[" + (System.currentTimeMillis() - -l_7_J) + "]");
            if (-l_9_I != SmsCheckResult.ESCT_200) {
                if (-l_9_I >= SmsCheckResult.ESCT_301) {
                    if (-l_9_I > SmsCheckResult.ESCT_305) {
                    }
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (-l_5_I == 0 || r5 == null) {
                    if (-l_5_I != 0 && r5 == null) {
                        -l_3_I = 1;
                    }
                    f.d("WifiDetectManager", "[End]detectNetworkState-nRet:[" + -l_3_I + "]");
                    return -l_3_I;
                }
                -l_3_I = 3;
                f.d("WifiDetectManager", "[End]detectNetworkState-nRet:[" + -l_3_I + "]");
                return -l_3_I;
            }
            Object -l_10_R = httpURLConnection.getHeaderField(str2);
            f.d("WifiDetectManager", "customHeader: " + -l_10_R);
            if (-l_10_R != null) {
                if (-l_10_R.equals(str2)) {
                    -l_5_I = 1;
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    if (-l_5_I == 0) {
                        -l_3_I = 3;
                        f.d("WifiDetectManager", "[End]detectNetworkState-nRet:[" + -l_3_I + "]");
                        return -l_3_I;
                    }
                    -l_3_I = 1;
                    f.d("WifiDetectManager", "[End]detectNetworkState-nRet:[" + -l_3_I + "]");
                    return -l_3_I;
                }
            }
            obj = a(str, httpURLConnection);
            -l_5_I = 1;
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (-l_5_I == 0) {
                -l_3_I = 3;
                f.d("WifiDetectManager", "[End]detectNetworkState-nRet:[" + -l_3_I + "]");
                return -l_3_I;
            }
            -l_3_I = 1;
        } catch (Object -l_7_R) {
            f.d("WifiDetectManager", "IOException:[" + -l_7_R + "]");
            -l_7_R.printStackTrace();
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        } catch (Throwable th) {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        f.d("WifiDetectManager", "[End]detectNetworkState-nRet:[" + -l_3_I + "]");
        return -l_3_I;
    }

    public void onCreate(Context context) {
        f.d("WifiDetectManager", "OnCreate-context:[" + context + "]");
        this.mContext = context;
        this.wR = (WifiManager) context.getSystemService("wifi");
        this.wS = im.bK();
        this.vW = new Handler(this, Looper.getMainLooper()) {
            final /* synthetic */ b wT;

            public void handleMessage(Message message) {
                if (message.what == 4097) {
                    f.d("WifiDetectManager", "onResult-CLOUND_CHECK:[" + message.arg1 + "]");
                    IWifiDetectListener -l_2_R = (IWifiDetectListener) message.obj;
                    if (-l_2_R != null) {
                        -l_2_R.onResult(message.arg1);
                    }
                }
            }
        };
    }
}
