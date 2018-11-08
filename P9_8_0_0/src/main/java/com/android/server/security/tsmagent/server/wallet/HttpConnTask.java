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
        CardServerBaseResponse result;
        Throwable th;
        Exception e;
        if (NetworkUtil.isNetworkConnected(this.mContext)) {
            String requestStr = prepareRequestStr(params);
            if (requestStr == null) {
                HwLog.d(" processTask, invalid request params.");
                return readErrorResponse(-3);
            }
            HttpURLConnection httpURLConnection = null;
            DataOutputStream dataOutputStream = null;
            InputStream is = null;
            ByteArrayOutputStream byteArrayOutputStream = null;
            try {
                URL url = new URL(this.mUrl);
                if ("https".equals(url.getProtocol())) {
                    httpURLConnection = openHttpsConnection(url);
                } else {
                    httpURLConnection = openHttpConnection(url);
                }
                httpURLConnection.setConnectTimeout(this.mConnTimeout);
                httpURLConnection.setReadTimeout(this.mSocketTimeout);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "xml/json");
                httpURLConnection.setRequestProperty("Charset", "UTF-8");
                httpURLConnection.connect();
                DataOutputStream outStream = new DataOutputStream(httpURLConnection.getOutputStream());
                try {
                    outStream.write(requestStr.getBytes("UTF-8"));
                    outStream.flush();
                    int resultCode = httpURLConnection.getResponseCode();
                    HwLog.d("processTask connection result code : " + resultCode);
                    if (200 == resultCode) {
                        is = httpURLConnection.getInputStream();
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        try {
                            byte[] buffer = new byte[1024];
                            while (true) {
                                int len = is.read(buffer);
                                if (len == -1) {
                                    break;
                                }
                                outputStream.write(buffer, 0, len);
                            }
                            result = readSuccessResponse(0, new String(outputStream.toByteArray(), "UTF-8"), null);
                            byteArrayOutputStream = outputStream;
                        } catch (MalformedURLException e2) {
                            byteArrayOutputStream = outputStream;
                            dataOutputStream = outStream;
                            try {
                                HwLog.e("processTask url invalid: url");
                                result = readErrorResponse(-3);
                                closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                                return result;
                            } catch (Throwable th2) {
                                th = th2;
                                closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                                throw th;
                            }
                        } catch (IOException e3) {
                            e = e3;
                            byteArrayOutputStream = outputStream;
                            dataOutputStream = outStream;
                            HwLog.e("processTask, Exception : " + e.getMessage());
                            result = readErrorResponse(-2);
                            closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                            return result;
                        } catch (Throwable th3) {
                            th = th3;
                            byteArrayOutputStream = outputStream;
                            dataOutputStream = outStream;
                            closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                            throw th;
                        }
                    }
                    HwLog.e("Service err. resultCode :" + resultCode);
                    result = readErrorResponse(-2);
                    closeStream(outStream, is, byteArrayOutputStream, httpURLConnection);
                } catch (MalformedURLException e4) {
                    dataOutputStream = outStream;
                    HwLog.e("processTask url invalid: url");
                    result = readErrorResponse(-3);
                    closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                    return result;
                } catch (IOException e5) {
                    e = e5;
                    dataOutputStream = outStream;
                    HwLog.e("processTask, Exception : " + e.getMessage());
                    result = readErrorResponse(-2);
                    closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                    return result;
                } catch (Throwable th4) {
                    th = th4;
                    dataOutputStream = outStream;
                    closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                    throw th;
                }
            } catch (MalformedURLException e6) {
                HwLog.e("processTask url invalid: url");
                result = readErrorResponse(-3);
                closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                return result;
            } catch (IOException e7) {
                e = e7;
                HwLog.e("processTask, Exception : " + e.getMessage());
                result = readErrorResponse(-2);
                closeStream(dataOutputStream, is, byteArrayOutputStream, httpURLConnection);
                return result;
            }
            return result;
        }
        HwLog.d(" processTask, no network.");
        return readErrorResponse(-1);
    }
}
