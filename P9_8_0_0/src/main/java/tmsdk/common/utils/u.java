package tmsdk.common.utils;

import android.net.wifi.WifiManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.exception.WifiApproveException;
import tmsdk.common.module.intelli_sms.SmsCheckResult;

public class u {
    public static String Mg;

    public interface a {
        void d(boolean z, boolean z2);
    }

    private static String a(InputStream inputStream) throws WifiApproveException {
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
        if (-l_1_R != null) {
            return -l_1_R;
        }
        throw new WifiApproveException("0725SSID:" + getSSID() + " page head content: " + -l_2_R);
    }

    private static String a(HttpURLConnection httpURLConnection) throws WifiApproveException {
        String str = null;
        Object -l_2_R = null;
        Object -l_3_R;
        try {
            if (!new URL("http://tools.3g.qq.com/cw.html").getHost().equals(httpURLConnection.getURL().getHost())) {
                str = httpURLConnection.getURL().toExternalForm();
            }
            if (str == null && httpURLConnection.getHeaderField("Location") != null) {
                str = httpURLConnection.getHeaderField("Location");
            }
            if (str == null && httpURLConnection.getHeaderField("Refresh") != null) {
                -l_3_R = httpURLConnection.getHeaderField("Refresh").split(";");
                if (-l_3_R.length == 2) {
                    str = -l_3_R[1].trim();
                }
            }
            if (str == null) {
                InputStream -l_2_R2 = httpURLConnection.getInputStream();
                if (-l_2_R2 != null) {
                    String -l_3_R2 = a(-l_2_R2);
                    if (-l_3_R2 != null) {
                        str = -l_3_R2;
                    }
                }
            }
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (IOException e) {
                    return str;
                }
            }
        } catch (IOException e2) {
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (IOException e3) {
                    return str;
                }
            }
        } catch (Object -l_3_R3) {
            throw -l_3_R3;
        } catch (Exception e4) {
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (IOException e5) {
                    return str;
                }
            }
        } catch (Throwable th) {
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (IOException e6) {
                }
            }
        }
        return str;
    }

    public static String a(a aVar) throws WifiApproveException {
        String -l_1_R = null;
        int -l_2_I = 0;
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL("http://tools.3g.qq.com/cw.html").openConnection();
            if (n.iX() < 8) {
                System.setProperty("http.keepAlive", "false");
            }
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestProperty("Pragma", "no-cache");
            httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
            httpURLConnection.setInstanceFollowRedirects(false);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setReadTimeout(5000);
            int -l_4_I = httpURLConnection.getResponseCode();
            if (-l_4_I != SmsCheckResult.ESCT_200) {
                if (-l_4_I >= SmsCheckResult.ESCT_301) {
                    if (-l_4_I > SmsCheckResult.ESCT_305) {
                    }
                }
                -l_2_I = 1;
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (-l_1_R != null) {
                    Mg = -l_1_R;
                    aVar.d(true, -l_2_I);
                } else {
                    aVar.d(false, -l_2_I);
                }
                return -l_1_R;
            }
            Object -l_5_R = httpURLConnection.getHeaderField("Meri");
            if (-l_5_R != null) {
                if (-l_5_R.equals("Meri")) {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    if (-l_1_R != null) {
                        aVar.d(false, -l_2_I);
                    } else {
                        Mg = -l_1_R;
                        aVar.d(true, -l_2_I);
                    }
                    return -l_1_R;
                }
            }
            -l_1_R = a(httpURLConnection);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (-l_1_R != null) {
                Mg = -l_1_R;
                aVar.d(true, -l_2_I);
            } else {
                aVar.d(false, -l_2_I);
            }
        } catch (IOException e) {
            if (-l_3_R != null) {
                -l_3_R.disconnect();
            }
            if (null != null) {
                Mg = null;
                aVar.d(true, false);
            } else {
                aVar.d(false, false);
            }
        } catch (Object -l_4_R) {
            throw -l_4_R;
        } catch (Exception e2) {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (null != null) {
                Mg = null;
                aVar.d(true, false);
            } else {
                aVar.d(false, false);
            }
        } catch (Throwable th) {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (null != null) {
                Mg = null;
                aVar.d(true, false);
            } else {
                aVar.d(false, false);
            }
        }
        return -l_1_R;
    }

    public static int aK(int i) {
        if (!jh()) {
            return -1;
        }
        Object -l_2_R = ((WifiManager) TMSDKContext.getApplicaionContext().getSystemService("wifi")).getConnectionInfo();
        return -l_2_R == null ? -1 : WifiManager.calculateSignalLevel(-l_2_R.getRssi(), i);
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
                    } catch (Object -l_4_R) {
                        -l_4_R.printStackTrace();
                    }
                } else {
                    -l_2_R.append(-l_3_R);
                }
            } catch (Object -l_4_R2) {
                -l_4_R2.printStackTrace();
            } catch (Throwable th) {
                try {
                    -l_1_R.close();
                } catch (Object -l_6_R) {
                    -l_6_R.printStackTrace();
                }
            }
        }
        -l_1_R.close();
        return -l_2_R.toString();
    }

    public static String getSSID() {
        try {
            WifiManager -l_0_R = (WifiManager) TMSDKContext.getApplicaionContext().getSystemService("wifi");
            if (-l_0_R != null) {
                Object -l_1_R = -l_0_R.getConnectionInfo();
                if (-l_1_R != null) {
                    return -l_1_R.getSSID();
                }
            }
        } catch (Object -l_0_R2) {
            f.e("WifiUtil", "getSSID: " + -l_0_R2);
        }
        return "";
    }

    public static boolean jh() {
        Object -l_0_R = null;
        try {
            -l_0_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
        } catch (Object -l_1_R) {
            f.g("getActiveNetworkInfo", " getActiveNetworkInfo NullPointerException--- \n" + -l_1_R.getMessage());
        }
        return -l_0_R != null && -l_0_R.getType() == 1;
    }
}
