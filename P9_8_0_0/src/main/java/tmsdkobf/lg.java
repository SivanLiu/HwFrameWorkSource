package tmsdkobf;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.json.JSONObject;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.i;
import tmsdk.common.utils.n;

public class lg {

    static class a {
        public String go = null;
        public String gp = null;
        int level = -1;
        public int yj = 0;
        public int yk = -1;
        public int yl = 3;
        int ym = -1;
        int yn = 0;

        a() {
        }
    }

    private static int a(ScanResult scanResult) {
        return (scanResult == null || scanResult.capabilities == null) ? -1 : !scanResult.capabilities.contains("WEP") ? !scanResult.capabilities.contains("PSK") ? !scanResult.capabilities.contains("EAP") ? 0 : 3 : 2 : 1;
    }

    private static String a(InputStream inputStream) {
        String -l_1_R = null;
        Object -l_2_R = b(inputStream);
        Object -l_3_R = new String[]{"http-equiv\\s*=\\s*[\"']*refresh[\"']*\\s*content\\s*=\\s*[\"']*[^;]*;\\s*url\\s*=\\s*[\"']*([^\"'\\s>]+)", "[^\\w](?:location.href\\s*=|location\\s*=|location.replace\\s*\\()\\s*[\"']*([^\"'>]+)", "<NextURL>([^<]+)", "\\s+action\\s*=\\s*[\"']*([^\"'>]+)[\"'>\\s]*.*submit", "<LoginURL>([^<]+)"};
        int -l_4_I = -l_3_R.length;
        for (int -l_6_I = 0; -l_6_I < -l_4_I && -l_1_R == null; -l_6_I++) {
            Object -l_5_R = Pattern.compile(-l_3_R[-l_6_I], 2).matcher(-l_2_R);
            while (-l_5_R.find() && r0 == null) {
                -l_1_R = -l_5_R.group(-l_5_R.groupCount());
                if (!(-l_1_R == null || -l_1_R.trim().toLowerCase().startsWith("http"))) {
                    -l_1_R = null;
                }
            }
        }
        return -l_1_R;
    }

    private static String a(String str, HttpURLConnection httpURLConnection) {
        String str2 = null;
        Object -l_3_R = null;
        try {
            if (!new URL(str).getHost().equals(httpURLConnection.getURL().getHost())) {
                str2 = httpURLConnection.getURL().toExternalForm();
            }
            if (str2 == null && httpURLConnection.getHeaderField("Location") != null) {
                str2 = httpURLConnection.getHeaderField("Location");
            }
            if (str2 == null && httpURLConnection.getHeaderField("Refresh") != null) {
                Object -l_6_R = httpURLConnection.getHeaderField("Refresh").split(";");
                if (-l_6_R.length == 2) {
                    str2 = -l_6_R[1].trim();
                }
            }
            if (str2 == null) {
                InputStream -l_3_R2 = httpURLConnection.getInputStream();
                if (-l_3_R2 != null) {
                    str2 = a(-l_3_R2);
                }
            }
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (IOException e) {
                    return str2;
                }
            }
        } catch (Throwable th) {
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (IOException e2) {
                }
            }
        }
        return str2;
    }

    static synchronized void a(a aVar) {
        synchronized (lg.class) {
            try {
                Object -l_1_R = new JSONObject();
                -l_1_R.put("maincode", String.valueOf(5));
                -l_1_R.put("time", String.valueOf(System.currentTimeMillis()));
                -l_1_R.put("bssid", aVar.gp);
                -l_1_R.put("ssid", aVar.go);
                -l_1_R.put("reportWifiType", String.valueOf(aVar.yj));
                -l_1_R.put("reportSecurityType", String.valueOf(aVar.yk));
                -l_1_R.put("subType", String.valueOf(aVar.yl));
                -l_1_R.put("wifiGradeLevel", String.valueOf(aVar.ym));
                -l_1_R.put("remark", String.valueOf("http://tools.3g.qq.com/wifi/cw.html"));
                -l_1_R.put("sessionkey", String.valueOf(-1));
                -l_1_R.put("connectsource", String.valueOf(4));
                -l_1_R.put("wifiType", String.valueOf(aVar.yn));
                la.a(-l_1_R.toString(), getPath(), 33);
            } catch (Throwable th) {
            }
        }
    }

    static int aK(int i) {
        Object -l_2_R = ((WifiManager) TMSDKContext.getApplicaionContext().getSystemService("wifi")).getConnectionInfo();
        return -l_2_R == null ? -1 : WifiManager.calculateSignalLevel(-l_2_R.getRssi(), i);
    }

    static int aL(int i) {
        if (((WifiManager) TMSDKContext.getApplicaionContext().getApplicationContext().getSystemService("wifi")) != null) {
            try {
                return WifiManager.calculateSignalLevel(i, 100) + 1;
            } catch (Throwable th) {
            }
        }
        return -1;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String b(InputStream inputStream) {
        Object -l_1_R = new BufferedReader(new InputStreamReader(inputStream));
        Object -l_2_R = new StringBuilder();
        while (true) {
            try {
                Object -l_3_R = -l_1_R.readLine();
                if (-l_3_R == null) {
                    try {
                        break;
                    } catch (IOException e) {
                        return -l_2_R.toString();
                    }
                }
                -l_2_R.append(-l_3_R);
            } catch (IOException e2) {
            } catch (Throwable th) {
                try {
                    -l_1_R.close();
                } catch (IOException e3) {
                }
            }
        }
        -l_1_R.close();
        return -l_2_R.toString();
    }

    public static void eq() {
        try {
            Object -l_0_R = la.bD(getPath());
            if (-l_0_R != null && !-l_0_R.isEmpty()) {
                JceStruct -l_1_R = new ao(33, new ArrayList());
                Object -l_2_R = -l_0_R.iterator();
                while (-l_2_R.hasNext()) {
                    Object -l_4_R = new JSONObject((String) -l_2_R.next());
                    Object -l_5_R = new ap(new HashMap());
                    -l_5_R.bG.put(Integer.valueOf(1), -l_4_R.getString("maincode"));
                    -l_5_R.bG.put(Integer.valueOf(2), -l_4_R.getString("time"));
                    -l_5_R.bG.put(Integer.valueOf(3), -l_4_R.getString("bssid"));
                    -l_5_R.bG.put(Integer.valueOf(4), -l_4_R.getString("ssid"));
                    -l_5_R.bG.put(Integer.valueOf(5), -l_4_R.getString("reportWifiType"));
                    -l_5_R.bG.put(Integer.valueOf(6), -l_4_R.getString("reportSecurityType"));
                    -l_5_R.bG.put(Integer.valueOf(7), -l_4_R.getString("subType"));
                    -l_5_R.bG.put(Integer.valueOf(8), -l_4_R.getString("wifiGradeLevel"));
                    -l_5_R.bG.put(Integer.valueOf(9), -l_4_R.getString("remark"));
                    -l_5_R.bG.put(Integer.valueOf(13), -l_4_R.getString("sessionkey"));
                    -l_5_R.bG.put(Integer.valueOf(14), -l_4_R.getString("connectsource"));
                    -l_5_R.bG.put(Integer.valueOf(15), -l_4_R.getString("wifiType"));
                    -l_1_R.bD.add(-l_5_R);
                }
                -l_2_R = im.bK();
                if (-l_1_R.bD.size() > 0 && -l_2_R != null) {
                    -l_2_R.a(4060, -l_1_R, null, 0, new jy() {
                        public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                            if (i3 == 0 && i4 == 0) {
                                la.bF(lg.getPath());
                            }
                        }
                    });
                }
            }
        } catch (Throwable th) {
        }
    }

    public static synchronized void ev() {
        synchronized (lg.class) {
            try {
                final Object -l_0_R = ew();
                if (-l_0_R != null) {
                    JceStruct -l_1_R = new ao(33, new ArrayList());
                    Object -l_2_R = new ap(new HashMap());
                    -l_2_R.bG.put(Integer.valueOf(1), String.valueOf(5));
                    -l_2_R.bG.put(Integer.valueOf(2), String.valueOf(System.currentTimeMillis()));
                    -l_2_R.bG.put(Integer.valueOf(3), -l_0_R.gp);
                    -l_2_R.bG.put(Integer.valueOf(4), -l_0_R.go);
                    -l_2_R.bG.put(Integer.valueOf(5), String.valueOf(-l_0_R.yj));
                    -l_2_R.bG.put(Integer.valueOf(6), String.valueOf(-l_0_R.yk));
                    -l_2_R.bG.put(Integer.valueOf(7), String.valueOf(-l_0_R.yl));
                    -l_2_R.bG.put(Integer.valueOf(8), String.valueOf(-l_0_R.ym));
                    -l_2_R.bG.put(Integer.valueOf(9), String.valueOf("Meri"));
                    -l_2_R.bG.put(Integer.valueOf(13), String.valueOf(-1));
                    -l_2_R.bG.put(Integer.valueOf(14), String.valueOf(4));
                    -l_2_R.bG.put(Integer.valueOf(15), String.valueOf(-l_0_R.yn));
                    -l_1_R.bD.add(-l_2_R);
                    Object -l_3_R = im.bK();
                    if (-l_1_R.bD.size() > 0 && -l_3_R != null) {
                        -l_3_R.a(4060, -l_1_R, null, 0, new jy() {
                            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                                if (i3 != 0 || i4 != 0) {
                                    lg.a(-l_0_R);
                                }
                            }
                        });
                    }
                } else {
                    return;
                }
            } catch (Throwable th) {
            }
        }
    }

    static a ew() {
        if (!i.K(TMSDKContext.getApplicaionContext())) {
            return null;
        }
        WifiManager -l_2_R = (WifiManager) TMSDKContext.getApplicaionContext().getSystemService("wifi");
        Object -l_3_R = -l_2_R.getConnectionInfo();
        Object -l_1_R = -l_3_R.getBSSID();
        Object -l_0_R = -l_3_R.getSSID();
        if (TextUtils.isEmpty(-l_0_R) || TextUtils.isEmpty(-l_1_R)) {
            return null;
        }
        int -l_5_I = -1;
        int -l_6_I = ex();
        int -l_7_I = -1;
        Object<ScanResult> -l_8_R = -l_2_R.getScanResults();
        if (-l_8_R != null) {
            for (ScanResult -l_10_R : -l_8_R) {
                if (-l_3_R.getBSSID().compareTo(-l_10_R.BSSID) == 0) {
                    -l_5_I = a(-l_10_R);
                    -l_7_I = aK(-l_10_R.level) + 1;
                }
            }
        }
        int -l_9_I = aL(-l_7_I);
        Object -l_11_R = new a();
        -l_11_R.gp = -l_1_R;
        -l_11_R.go = -l_0_R;
        -l_11_R.yj = 0;
        -l_11_R.yk = -l_5_I;
        -l_11_R.yl = -l_6_I;
        -l_11_R.ym = -l_9_I;
        -l_11_R.yn = 0;
        return -l_11_R;
    }

    private static int ex() {
        Object obj = null;
        int -l_2_I = 0;
        HttpURLConnection httpURLConnection = null;
        try {
            long -l_4_J = System.currentTimeMillis();
            httpURLConnection = (HttpURLConnection) new URL("http://tools.3g.qq.com/wifi/cw.html").openConnection();
            if (n.iX() < 8) {
                System.setProperty("http.keepAlive", "false");
            }
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestProperty("Pragma", "no-cache");
            httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
            httpURLConnection.setInstanceFollowRedirects(false);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setReadTimeout(30000);
            int -l_6_I = httpURLConnection.getResponseCode();
            if (-l_6_I != SmsCheckResult.ESCT_200) {
                if (-l_6_I >= SmsCheckResult.ESCT_301) {
                    if (-l_6_I > SmsCheckResult.ESCT_305) {
                    }
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (-l_2_I == 0 || obj == null) {
                    return (-l_2_I == 0 && obj == null) ? 4 : 3;
                } else {
                    return 1;
                }
            }
            Object -l_7_R = httpURLConnection.getHeaderField("Meri");
            if (-l_7_R != null) {
                if (-l_7_R.equals("Meri")) {
                    -l_2_I = 1;
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    if (-l_2_I == 0) {
                        return 1;
                    }
                    if (-l_2_I == 0) {
                        return 3;
                    }
                }
            }
            obj = a("http://tools.3g.qq.com/wifi/cw.html", httpURLConnection);
            -l_2_I = 1;
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (-l_2_I == 0) {
                return 1;
            }
            if (-l_2_I == 0) {
                return 3;
            }
        } catch (Throwable th) {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    public static String getPath() {
        return TMSDKContext.getApplicaionContext().getFilesDir().getAbsolutePath() + File.separator + "d_" + String.valueOf(33);
    }
}
