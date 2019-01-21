package android.media;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HwCustMediaPlayerImpl extends HwCustMediaPlayer {
    private static final boolean DEBUG = false;
    private static boolean HWFLOW = false;
    private static final boolean HWLOGW_E = true;
    private static final int MAX_DNS_WAIT_TIME_MILLISECOND = 2000;
    private static final String SETTING_KEY_PROXY_HOST = "rtsp_proxy_host";
    private static final String SETTING_KEY_PROXY_PORT = "rtsp_proxy_port";
    private static final String TAG = "HwCustMediaPlayerImpl";
    private static final String TAG_FLOW = "HwCustMediaPlayerImpl_FLOW";

    private class URLConvertThread implements Runnable {
        private String mIPAddr;
        private boolean mIsStop;
        private String mURL;

        private URLConvertThread() {
            this.mIPAddr = "ERROR";
            this.mIsStop = HwCustMediaPlayerImpl.DEBUG;
        }

        public void setURL(String url) {
            this.mURL = url;
        }

        public String getIPAddr() {
            return this.mIPAddr;
        }

        public boolean isStop() {
            return this.mIsStop;
        }

        public void run() {
            this.mIsStop = HwCustMediaPlayerImpl.DEBUG;
            try {
                this.mIPAddr = InetAddress.getByName(this.mURL).getHostAddress();
            } catch (Exception ee) {
                String str = HwCustMediaPlayerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DNS convert error, bypass the proxy!  ");
                stringBuilder.append(ee.getMessage());
                Log.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                this.mIsStop = HwCustMediaPlayerImpl.HWLOGW_E;
            }
            this.mIsStop = HwCustMediaPlayerImpl.HWLOGW_E;
        }
    }

    static {
        boolean z = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4))) ? HWLOGW_E : DEBUG;
        HWFLOW = z;
    }

    private boolean isIpAddrValidate(String ipAddress) {
        return Pattern.compile("((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})").matcher(ipAddress).matches();
    }

    private boolean isPortValidate(String port) {
        return Pattern.compile("(\\d){1,5}").matcher(port).matches();
    }

    private void fillHeader(Map<String, String> headers, String key, String value) {
        if (value == null || "".equals(value.trim()) || headers == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fillHeader: cannot fill key=");
            stringBuilder.append(key);
            stringBuilder.append(", value=");
            stringBuilder.append(value);
            Log.w(str, stringBuilder.toString());
            return;
        }
        headers.put(key, value);
    }

    private boolean isConnectToNetWorkType(Context context, int type) {
        NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.getType() != type) {
            return DEBUG;
        }
        return HWLOGW_E;
    }

    private Map<String, String> setProxyHeaders(Context context, Uri uri, Map<String, String> headers) {
        if (isConnectToNetWorkType(context, 1) == HWLOGW_E) {
            if (HWFLOW) {
                Log.i(TAG_FLOW, "Bypass proxyHeaders for wifi-connected");
            }
        } else if ("rtsp".equals(uri.getScheme().toLowerCase())) {
            String httpProxyHost = System.getString(context.getContentResolver(), SETTING_KEY_PROXY_HOST);
            String httpProxyPort = System.getString(context.getContentResolver(), SETTING_KEY_PROXY_PORT);
            httpProxyHost.trim();
            httpProxyPort.trim();
            if (!isIpAddrValidate(httpProxyHost)) {
                URLConvertThread urlConvertThread = new URLConvertThread();
                Thread thread = new Thread(urlConvertThread);
                urlConvertThread.setURL(httpProxyHost);
                thread.start();
                int lAlltime = 0;
                int lThisTurnWaitTime = 50;
                while (lAlltime < MAX_DNS_WAIT_TIME_MILLISECOND) {
                    try {
                        Thread.sleep((long) lThisTurnWaitTime);
                        if (urlConvertThread.isStop() == HWLOGW_E) {
                            break;
                        }
                        lAlltime += lThisTurnWaitTime;
                        lThisTurnWaitTime = 100;
                    } catch (InterruptedException e) {
                    }
                }
                httpProxyHost = urlConvertThread.getIPAddr();
            }
            if (!isIpAddrValidate(httpProxyHost) || !isPortValidate(httpProxyPort)) {
                Log.e(TAG, "Bypass proxyHeaders because address or port invalidate!");
            } else if (!"0.0.0.0".equals(httpProxyHost)) {
                StringBuilder sb = new StringBuilder();
                sb.append(httpProxyHost);
                sb.append(":");
                sb.append(httpProxyPort);
                fillHeader(headers, "hw-use-proxy", sb.toString());
            } else if (HWFLOW) {
                Log.i(TAG_FLOW, "Bypass proxyHeaders for 0.0.0.0");
            }
        } else if (HWFLOW) {
            Log.i(TAG_FLOW, "Bypass proxyHeaders only for rtsp protocal");
        }
        return headers;
    }

    private Map<String, String> setNetWorkTypeHeaders(Context context, Map<String, String> headers) {
        String tm;
        String networkType;
        NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == 1) {
            tm = "WIFI";
        } else if (networkInfo == null || networkInfo.getType() != 6) {
            int dataNetworkType = ((TelephonyManager) context.getSystemService("phone")).getDataNetworkType();
            if (dataNetworkType != 13) {
                switch (dataNetworkType) {
                    case 5:
                        networkType = "EVDO";
                        break;
                    case 6:
                        networkType = "DORA";
                        break;
                    case 7:
                        networkType = "IS2000";
                        break;
                    default:
                        tm = "";
                        break;
                }
            }
            networkType = "LTE";
            fillHeader(headers, "x-network-type", networkType);
            return headers;
        } else {
            tm = "WIMAX";
        }
        networkType = tm;
        fillHeader(headers, "x-network-type", networkType);
        return headers;
    }

    public Map<String, String> setStreamingMediaHeaders(Context context, Uri uri, Map<String, String> headers) {
        boolean isSprintPhone = ("237".equals(SystemProperties.get("ro.config.hw_opta", "0")) && "840".equals(SystemProperties.get("ro.config.hw_optb", "0"))) ? HWLOGW_E : DEBUG;
        if (!isSprintPhone) {
            return headers;
        }
        if (headers == null) {
            headers = new HashMap();
        }
        try {
            headers = setProxyHeaders(context, uri, headers);
            return setNetWorkTypeHeaders(context, headers);
        } catch (Exception ee) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Proxy or networkType  function  error, bypass the header settings!  errMsg:");
            stringBuilder.append(ee.getMessage());
            Log.e(str, stringBuilder.toString());
            return headers;
        }
    }
}
