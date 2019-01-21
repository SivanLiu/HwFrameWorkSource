package com.android.server.security.tsmagent.server.wallet;

import android.content.Context;
import com.android.server.security.tsmagent.server.CardServerBaseRequest;
import com.android.server.security.tsmagent.server.CardServerBaseResponse;
import com.android.server.security.tsmagent.server.HttpConnectionBase;
import com.android.server.security.tsmagent.utils.HwLog;
import com.android.server.security.tsmagent.utils.NetworkUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public abstract class HttpConnTask extends HttpConnectionBase {
    public HttpConnTask(Context context, String url) {
        this.mContext = context;
        this.mUrl = url;
    }

    public HttpConnTask(Context context, String url, int connTimeout, int socketTimeout) {
        this.mContext = context;
        this.mUrl = url;
        this.mConnTimeout = connTimeout;
        this.mSocketTimeout = socketTimeout;
    }

    public CardServerBaseResponse processTask(CardServerBaseRequest params) {
        if (NetworkUtil.isNetworkConnected(this.mContext)) {
            String requestStr = prepareRequestStr(params);
            if (requestStr == null) {
                HwLog.d(" processTask, invalid request params.");
                return readErrorResponse(-3);
            }
            CardServerBaseResponse result;
            HttpURLConnection conn = null;
            DataOutputStream outStream = null;
            InputStream is = null;
            ByteArrayOutputStream outputStream = null;
            try {
                URL url = new URL(this.mUrl);
                if (NetworkCheckerThread.TYPE_HTTPS.equals(url.getProtocol())) {
                    conn = openHttpsConnection(url);
                } else {
                    conn = openHttpConnection(url);
                }
                conn.setConnectTimeout(this.mConnTimeout);
                conn.setReadTimeout(this.mSocketTimeout);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "xml/json");
                conn.setRequestProperty("Charset", "UTF-8");
                conn.connect();
                outStream = new DataOutputStream(conn.getOutputStream());
                outStream.write(requestStr.getBytes("UTF-8"));
                outStream.flush();
                int resultCode = conn.getResponseCode();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processTask connection result code : ");
                stringBuilder.append(resultCode);
                HwLog.d(stringBuilder.toString());
                if (200 == resultCode) {
                    is = conn.getInputStream();
                    outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while (true) {
                        int read = is.read(buffer);
                        len = read;
                        if (read == -1) {
                            break;
                        }
                        outputStream.write(buffer, 0, len);
                    }
                    result = readSuccessResponse(0, new String(outputStream.toByteArray(), "UTF-8"), null);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Service err. resultCode :");
                    stringBuilder2.append(resultCode);
                    HwLog.e(stringBuilder2.toString());
                    result = readErrorResponse(-2);
                }
            } catch (MalformedURLException e) {
                HwLog.e("processTask url invalid: url");
                result = readErrorResponse(-3);
            } catch (IOException | KeyManagementException | NoSuchAlgorithmException e2) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("processTask, Exception : ");
                stringBuilder3.append(e2.getMessage());
                HwLog.e(stringBuilder3.toString());
                result = readErrorResponse(-2);
            } catch (Throwable th) {
                closeStream(null, null, outputStream, null);
            }
            closeStream(outStream, is, outputStream, conn);
            return result;
        }
        HwLog.d(" processTask, no network.");
        return readErrorResponse(-1);
    }
}
