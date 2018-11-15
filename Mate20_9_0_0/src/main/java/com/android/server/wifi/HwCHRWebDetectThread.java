package com.android.server.wifi;

import android.util.Log;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class HwCHRWebDetectThread extends Thread {
    private static final String BACKUP_SERVER_URL = "http://www.youku.com";
    private static final int ERROR_PORTAL = 2;
    private static final int ERROR_UNKNOW = 0;
    private static final String MAIN_SERVER_URL = "http://www.baidu.com";
    private static final int MY_HTTP_ERR = -1;
    private static final int MY_HTTP_OK = 200;
    private static final int SOCKET_TIMEOUT_MS = 4000;
    private static final String TAG = "HwCHRWebDetectThread";
    private static int isRedirect = 0;
    private static boolean mEnableCheck = false;
    private static boolean mFirstDetect = false;
    private int errorCode = 0;
    private HwWifiCHRService mHwWifiCHRService;
    private int mReason = 0;

    public HwCHRWebDetectThread(int reason) {
        this.mReason = reason;
        this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
    }

    public static void setEnableCheck(boolean enableCheck) {
        mEnableCheck = enableCheck;
    }

    public static void setFirstDetect(boolean firstDetect) {
        mFirstDetect = firstDetect;
    }

    public void run() {
        if (!isInternetConnected() && this.mHwWifiCHRService != null) {
            this.mHwWifiCHRService.updateAccessWebException(this.mReason, this.errorCode == 2 ? "ERROR_PORTAL" : "OTHER");
        }
    }

    public boolean isInternetConnected() {
        boolean ret = false;
        String[] srvUrls = new String[]{"http://www.baidu.com", BACKUP_SERVER_URL};
        boolean z = false;
        if (!mEnableCheck) {
            return false;
        }
        int respCode;
        String str;
        StringBuilder stringBuilder;
        this.errorCode = 0;
        int IsRedirect = 1;
        for (int i = 0; i < srvUrls.length; i++) {
            if (i > 0) {
                HwWiFiLogUtils logUtils = HwWiFiLogUtils.getDefault();
                logUtils.startLinkLayerLog();
                respCode = doHttpRequest(srvUrls[i]);
                logUtils.stopLinkLayerLog();
            } else {
                respCode = doHttpRequest(srvUrls[i]);
            }
            IsRedirect &= isRedirect();
            if (respCode == 200) {
                ret = true;
                break;
            }
        }
        if (mFirstDetect) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("first Detect after connect, IsPortalConnection=");
            stringBuilder.append(IsRedirect);
            Log.d(str, stringBuilder.toString());
            updatePortalConnection(IsRedirect);
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("connect web mReason=");
        stringBuilder.append(this.mReason);
        stringBuilder.append(" ret=");
        stringBuilder.append(ret);
        Log.d(str, stringBuilder.toString());
        HwWifiCHRService hwWifiCHRService = this.mHwWifiCHRService;
        respCode = this.mReason;
        if (this.errorCode == 2) {
            z = true;
        }
        hwWifiCHRService.incrAccessWebRecord(respCode, ret, z);
        return ret;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    /* JADX WARNING: Missing block: B:9:0x0070, code:
            if (r1 != null) goto L_0x0072;
     */
    /* JADX WARNING: Missing block: B:10:0x0072, code:
            android.util.Log.d(TAG, "doHttpRequest: Disconnect URLConnection");
            r1.disconnect();
     */
    /* JADX WARNING: Missing block: B:15:0x0083, code:
            if (r1 == null) goto L_0x00df;
     */
    /* JADX WARNING: Missing block: B:18:0x00a0, code:
            if (r1 == null) goto L_0x00df;
     */
    /* JADX WARNING: Missing block: B:21:0x00bd, code:
            if (r1 == null) goto L_0x00df;
     */
    /* JADX WARNING: Missing block: B:24:0x00dc, code:
            if (r1 == null) goto L_0x00df;
     */
    /* JADX WARNING: Missing block: B:25:0x00df, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int doHttpRequest(String urlAddr) {
        int resCode = -1;
        HttpURLConnection urlConnection = null;
        String str;
        StringBuilder stringBuilder;
        try {
            urlConnection = (HttpURLConnection) new URL(urlAddr).openConnection();
            urlConnection.setConnectTimeout(4000);
            urlConnection.setReadTimeout(4000);
            urlConnection.setRequestMethod("GET");
            urlConnection.setUseCaches(false);
            urlConnection.setInstanceFollowRedirects(false);
            resCode = urlConnection.getResponseCode();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("browse the web ");
            stringBuilder.append(urlAddr);
            stringBuilder.append(",  returns responsecode =");
            stringBuilder.append(resCode);
            Log.d(str, stringBuilder.toString());
            if (isRedirectedRespCode(resCode)) {
                this.errorCode = 2;
                str = urlConnection.getHeaderField("Location");
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Location=");
                stringBuilder2.append(str == null ? "null" : str);
                Log.d(str2, stringBuilder2.toString());
            }
        } catch (MalformedURLException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("URL ");
            stringBuilder.append(urlAddr);
            stringBuilder.append(" is not available.");
            Log.d(str, stringBuilder.toString());
        } catch (UnknownHostException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("doHttpRequest: UnknownHostException: ");
            stringBuilder.append(e2);
            Log.d(str, stringBuilder.toString());
            e2.printStackTrace();
        } catch (IOException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("doHttpRequest: IOException: ");
            stringBuilder.append(e3);
            Log.d(str, stringBuilder.toString());
            e3.printStackTrace();
        } catch (Exception e4) {
            e4.printStackTrace();
        } catch (Throwable th) {
            if (urlConnection != null) {
                Log.d(TAG, "doHttpRequest: Disconnect URLConnection");
                urlConnection.disconnect();
            }
        }
    }

    private static boolean isRedirectedRespCode(int respCode) {
        int i = (respCode < 300 || respCode > 307) ? 0 : 1;
        isRedirect = i;
        if (isRedirect == 1) {
            return true;
        }
        return false;
    }

    public static int isRedirect() {
        return isRedirect;
    }

    private void updatePortalConnection(int isPortalconnection) {
        this.mHwWifiCHRService.updatePortalConnection(isPortalconnection);
    }
}
