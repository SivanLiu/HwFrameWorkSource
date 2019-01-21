package com.android.server.security.tsmagent.server.card.impl;

import android.content.Context;
import com.android.server.security.tsmagent.constant.ServiceConfig;
import com.android.server.security.tsmagent.server.CardServerBaseRequest;
import com.android.server.security.tsmagent.server.CardServerBaseResponse;
import com.android.server.security.tsmagent.server.HttpConnectionBase;
import com.android.server.security.tsmagent.server.card.request.TsmParamQueryRequest;
import com.android.server.security.tsmagent.server.wallet.impl.JSONHelper;
import com.android.server.security.tsmagent.utils.HwLog;
import com.android.server.security.tsmagent.utils.NetworkUtil;
import com.android.server.security.tsmagent.utils.StringUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class HttpConnTask extends HttpConnectionBase {
    protected abstract CardServerBaseResponse readSuccessResponse(int i, String str, JSONObject jSONObject);

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
        CardServerBaseRequest cardServerBaseRequest = params;
        if (NetworkUtil.isNetworkConnected(this.mContext)) {
            String requestStr = prepareRequestStr(params);
            if (requestStr == null) {
                HwLog.d("processTask, invalid request params.");
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
                if (cardServerBaseRequest instanceof TsmParamQueryRequest) {
                    TsmParamQueryRequest request = (TsmParamQueryRequest) cardServerBaseRequest;
                    request.setCplc("***************");
                    request.setTsmParamIMEI("***************");
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processTask request string : ");
                stringBuilder.append(prepareRequestStr(params));
                HwLog.d(stringBuilder.toString());
                outStream.write(requestStr.getBytes("UTF-8"));
                outStream.flush();
                int resultCode = conn.getResponseCode();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("processTask connection result code : ");
                stringBuilder2.append(resultCode);
                HwLog.d(stringBuilder2.toString());
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
                    result = handleResponse(new String(outputStream.toByteArray(), "UTF-8"));
                } else if (HttpConnectionBase.SERVER_OVERLOAD_ERRORCODE == resultCode) {
                    result = readErrorResponse(-4);
                } else {
                    result = readErrorResponse(-2);
                }
            } catch (MalformedURLException e) {
                HwLog.e("processTask url invalid.");
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
        HwLog.d("processTask, no network.");
        return readErrorResponse(-1);
    }

    public CardServerBaseResponse handleResponse(String responseStr) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("handleResponse response str : ");
        stringBuilder2.append(responseStr);
        HwLog.d(stringBuilder2.toString());
        String returnDesc = null;
        JSONObject dataObject = null;
        if (responseStr == null) {
            return readSuccessResponse(-99, null, null);
        }
        JSONObject responseJson = null;
        int returnCode;
        try {
            responseJson = new JSONObject(responseStr);
            int keyIndex = JSONHelper.getIntValue(responseJson, "keyIndex");
            String merchantID = JSONHelper.getStringValue(responseJson, "merchantID");
            String errorCode = JSONHelper.getStringValue(responseJson, "errorCode");
            String errorMsg = JSONHelper.getStringValue(responseJson, "errorMsg");
            String responseDataStr = JSONHelper.getStringValue(responseJson, "response");
            StringBuilder stringBuilder3;
            if (errorCode != null) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("handleResponse, error code : ");
                stringBuilder3.append(errorCode);
                stringBuilder3.append(",error msg : ");
                stringBuilder3.append(errorMsg);
                HwLog.w(stringBuilder3.toString());
                return readSuccessResponse(Integer.parseInt(errorCode), errorMsg, null);
            }
            if (ServiceConfig.WALLET_MERCHANT_ID.equals(merchantID) && -1 == keyIndex) {
                if (!StringUtil.isTrimedEmpty(responseDataStr)) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("handleResponse, responseDataStr : ");
                    stringBuilder3.append(responseDataStr);
                    HwLog.d(stringBuilder3.toString());
                    dataObject = new JSONObject(responseDataStr);
                    String returnCodeStr = JSONHelper.getStringValue(dataObject, "returnCode");
                    if (returnCodeStr == null) {
                        HwLog.d("handleResponse, returnCode is invalid.");
                        return readSuccessResponse(-99, null, null);
                    }
                    if (isNumber(returnCodeStr)) {
                        returnCode = Integer.parseInt(returnCodeStr);
                    } else {
                        returnCode = -98;
                    }
                    returnDesc = JSONHelper.getStringValue(dataObject, "returnDesc");
                    return readSuccessResponse(returnCode, returnDesc, dataObject);
                }
            }
            HwLog.d("handleResponse, unexpected error from server.");
            return readSuccessResponse(-99, null, null);
        } catch (NumberFormatException ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("readSuccessResponse, NumberFormatException : ");
            stringBuilder.append(ex);
            HwLog.e(stringBuilder.toString());
            returnCode = -99;
        } catch (JSONException ex2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("readSuccessResponse, JSONException : ");
            stringBuilder.append(ex2);
            HwLog.e(stringBuilder.toString());
            returnCode = -99;
        }
    }

    public boolean isNumber(String str) {
        if (!(str == null || "".equals(str.trim()) || !Pattern.compile("[0-9]*").matcher(str).matches())) {
            Long number = Long.valueOf(Long.parseLong(str));
            if (number.longValue() <= 2147483647L && number.longValue() >= -2147483648L) {
                return true;
            }
        }
        return false;
    }
}
