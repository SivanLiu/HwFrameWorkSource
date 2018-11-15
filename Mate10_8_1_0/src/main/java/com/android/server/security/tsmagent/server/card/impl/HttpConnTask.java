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
        CardServerBaseResponse result;
        Throwable th;
        Exception e;
        if (NetworkUtil.isNetworkConnected(this.mContext)) {
            String requestStr = prepareRequestStr(params);
            if (requestStr == null) {
                HwLog.d("processTask, invalid request params.");
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
                    if (params instanceof TsmParamQueryRequest) {
                        TsmParamQueryRequest request = (TsmParamQueryRequest) params;
                        request.setCplc("***************");
                        request.setTsmParamIMEI("***************");
                    }
                    HwLog.d("processTask request string : " + prepareRequestStr(params));
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
                            result = handleResponse(new String(outputStream.toByteArray(), "UTF-8"));
                            byteArrayOutputStream = outputStream;
                        } catch (MalformedURLException e2) {
                            byteArrayOutputStream = outputStream;
                            dataOutputStream = outStream;
                            try {
                                HwLog.e("processTask url invalid.");
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
                    } else if (503 == resultCode) {
                        result = readErrorResponse(-4);
                    } else {
                        result = readErrorResponse(-2);
                    }
                    closeStream(outStream, is, byteArrayOutputStream, httpURLConnection);
                } catch (MalformedURLException e4) {
                    dataOutputStream = outStream;
                    HwLog.e("processTask url invalid.");
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
                HwLog.e("processTask url invalid.");
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
        HwLog.d("processTask, no network.");
        return readErrorResponse(-1);
    }

    public CardServerBaseResponse handleResponse(String responseStr) {
        NumberFormatException ex;
        JSONObject jSONObject;
        JSONException ex2;
        HwLog.d("handleResponse response str : " + responseStr);
        String returnDesc = null;
        JSONObject jSONObject2 = null;
        if (responseStr == null) {
            return readSuccessResponse(-99, null, null);
        }
        int returnCode;
        try {
            JSONObject responseJson = new JSONObject(responseStr);
            try {
                int keyIndex = JSONHelper.getIntValue(responseJson, "keyIndex");
                String merchantID = JSONHelper.getStringValue(responseJson, "merchantID");
                String errorCode = JSONHelper.getStringValue(responseJson, "errorCode");
                String errorMsg = JSONHelper.getStringValue(responseJson, "errorMsg");
                String responseDataStr = JSONHelper.getStringValue(responseJson, "response");
                if (errorCode != null) {
                    HwLog.w("handleResponse, error code : " + errorCode + ",error msg : " + errorMsg);
                    return readSuccessResponse(Integer.parseInt(errorCode), errorMsg, null);
                } else if (ServiceConfig.WALLET_MERCHANT_ID.equals(merchantID) && -1 == keyIndex && !StringUtil.isTrimedEmpty(responseDataStr)) {
                    HwLog.d("handleResponse, responseDataStr : " + responseDataStr);
                    JSONObject dataObject = new JSONObject(responseDataStr);
                    try {
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
                        jSONObject2 = dataObject;
                        return readSuccessResponse(returnCode, returnDesc, jSONObject2);
                    } catch (NumberFormatException e) {
                        ex = e;
                        jSONObject = responseJson;
                        jSONObject2 = dataObject;
                        HwLog.e("readSuccessResponse, NumberFormatException : " + ex);
                        returnCode = -99;
                        return readSuccessResponse(returnCode, returnDesc, jSONObject2);
                    } catch (JSONException e2) {
                        ex2 = e2;
                        jSONObject = responseJson;
                        jSONObject2 = dataObject;
                        HwLog.e("readSuccessResponse, JSONException : " + ex2);
                        returnCode = -99;
                        return readSuccessResponse(returnCode, returnDesc, jSONObject2);
                    }
                } else {
                    HwLog.d("handleResponse, unexpected error from server.");
                    return readSuccessResponse(-99, null, null);
                }
            } catch (NumberFormatException e3) {
                ex = e3;
                HwLog.e("readSuccessResponse, NumberFormatException : " + ex);
                returnCode = -99;
                return readSuccessResponse(returnCode, returnDesc, jSONObject2);
            } catch (JSONException e4) {
                ex2 = e4;
                jSONObject = responseJson;
                HwLog.e("readSuccessResponse, JSONException : " + ex2);
                returnCode = -99;
                return readSuccessResponse(returnCode, returnDesc, jSONObject2);
            }
        } catch (NumberFormatException e5) {
            ex = e5;
            HwLog.e("readSuccessResponse, NumberFormatException : " + ex);
            returnCode = -99;
            return readSuccessResponse(returnCode, returnDesc, jSONObject2);
        } catch (JSONException e6) {
            ex2 = e6;
            HwLog.e("readSuccessResponse, JSONException : " + ex2);
            returnCode = -99;
            return readSuccessResponse(returnCode, returnDesc, jSONObject2);
        }
    }

    public boolean isNumber(String str) {
        if (!(str == null || ("".equals(str.trim()) ^ 1) == 0 || !Pattern.compile("[0-9]*").matcher(str).matches())) {
            Long number = Long.valueOf(Long.parseLong(str));
            if (number.longValue() <= 2147483647L && number.longValue() >= -2147483648L) {
                return true;
            }
        }
        return false;
    }
}
