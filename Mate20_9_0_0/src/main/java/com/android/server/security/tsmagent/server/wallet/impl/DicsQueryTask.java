package com.android.server.security.tsmagent.server.wallet.impl;

import android.content.Context;
import com.android.server.security.tsmagent.constant.ServiceConfig;
import com.android.server.security.tsmagent.server.CardServerBaseRequest;
import com.android.server.security.tsmagent.server.CardServerBaseResponse;
import com.android.server.security.tsmagent.server.wallet.HttpConnTask;
import com.android.server.security.tsmagent.server.wallet.request.QueryDicsRequset;
import com.android.server.security.tsmagent.server.wallet.response.DicItem;
import com.android.server.security.tsmagent.server.wallet.response.QueryDicsResponse;
import com.android.server.security.tsmagent.utils.HwLog;
import com.android.server.security.tsmagent.utils.StringUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DicsQueryTask extends HttpConnTask {
    private static final String QUERY_DICS_COMMANDER = "get.dics";

    public DicsQueryTask(Context context, String url) {
        super(context, url);
    }

    protected String prepareRequestStr(CardServerBaseRequest params) {
        if (!(params instanceof QueryDicsRequset)) {
            return null;
        }
        if (StringUtil.isTrimedEmpty(params.getSrcTransactionID()) || StringUtil.isTrimedEmpty(params.getMerchantID())) {
            HwLog.e("DicsQueryTask prepareRequestStr params error.");
            return null;
        }
        return JSONHelper.createRequestStr(params.getMerchantID(), createDataStr(JSONHelper.createHeaderStr(params.getSrcTransactionID(), QUERY_DICS_COMMANDER), params), this.mContext);
    }

    private JSONObject createDataStr(JSONObject headerObject, CardServerBaseRequest params) {
        if (!(params instanceof QueryDicsRequset)) {
            return null;
        }
        QueryDicsRequset castPara = (QueryDicsRequset) params;
        if (headerObject == null || StringUtil.isTrimedEmpty(castPara.dicName)) {
            HwLog.e("DicsQueryTask createDataStr params error.");
            return null;
        }
        JSONObject jObj;
        try {
            jObj = new JSONObject();
            jObj.put("header", headerObject);
            jObj.put("dicName", castPara.dicName);
            jObj.put("itemName", castPara.itemName);
        } catch (JSONException e) {
            HwLog.e("DicsQueryTask createDataStr JSONException.");
            jObj = null;
        }
        return jObj;
    }

    protected CardServerBaseResponse readErrorResponse(int errorCode) {
        CardServerBaseResponse response = new QueryDicsResponse();
        if (-1 == errorCode) {
            response.returnCode = -1;
        } else if (-3 == errorCode) {
            response.returnCode = 1;
        } else if (-2 == errorCode) {
            response.returnCode = -2;
        }
        return response;
    }

    protected QueryDicsResponse readSuccessResponse(int returnCode, String responseStr, JSONObject dataObject) {
        QueryDicsResponse response = new QueryDicsResponse();
        resolveResponse(response, responseStr);
        return response;
    }

    protected void makeResponseData(CardServerBaseResponse response, JSONObject dataObject) throws JSONException {
        if (response instanceof QueryDicsResponse) {
            QueryDicsResponse resp = (QueryDicsResponse) response;
            JSONArray jsonArray = null;
            if (dataObject.has("data")) {
                jsonArray = dataObject.getJSONArray("data");
            }
            if (jsonArray != null) {
                int size = jsonArray.length();
                for (int i = 0; i < size; i++) {
                    JSONObject dicObject = jsonArray.getJSONObject(i);
                    if (dicObject != null) {
                        DicItem item = new DicItem();
                        item.setParent(JSONHelper.getStringValue(dicObject, "parent"));
                        item.setName(JSONHelper.getStringValue(dicObject, "name"));
                        item.setValue(JSONHelper.getStringValue(dicObject, "value"));
                        resp.dicItems.add(item);
                    }
                }
            }
        }
    }

    protected void resolveResponse(CardServerBaseResponse response, String responseStr) {
        NumberFormatException ex;
        StringBuilder stringBuilder;
        JSONException ex2;
        String str;
        CardServerBaseResponse cardServerBaseResponse = response;
        if (StringUtil.isTrimedEmpty(responseStr)) {
            HwLog.e("responseStr is null");
            cardServerBaseResponse.returnCode = -99;
            return;
        }
        JSONObject responseJson = null;
        try {
            try {
                responseJson = new JSONObject(responseStr);
                String merchantID = JSONHelper.getStringValue(responseJson, "merchantID");
                int keyIndex = JSONHelper.getIntValue(responseJson, "keyIndex");
                String responseDataStr = JSONHelper.getStringValue(responseJson, "response");
                String errorCode = JSONHelper.getStringValue(responseJson, "errorCode");
                String errorMsg = JSONHelper.getStringValue(responseJson, "errorMsg");
                String err;
                if (errorCode != null) {
                    err = new StringBuilder();
                    err.append(" code:");
                    err.append(errorCode);
                    err.append(" msg:");
                    err.append(errorMsg);
                    HwLog.e(err.toString());
                    cardServerBaseResponse.returnCode = Integer.parseInt(errorCode);
                    return;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("merchantID: ");
                stringBuilder2.append(merchantID);
                stringBuilder2.append("\nresponseDataStr: ");
                stringBuilder2.append(responseDataStr);
                HwLog.d(stringBuilder2.toString());
                if (ServiceConfig.WALLET_MERCHANT_ID.equals(merchantID)) {
                    if (!StringUtil.isTrimedEmpty(responseDataStr)) {
                        err = null;
                        if (keyIndex == -1) {
                            err = responseDataStr;
                        }
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("decryptedResponse : ");
                        stringBuilder3.append(err);
                        stringBuilder3.append("keyIndex: ");
                        stringBuilder3.append(keyIndex);
                        HwLog.d(stringBuilder3.toString());
                        JSONObject dataObject = new JSONObject();
                        String returnCodeStr = null;
                        String returnDesc = null;
                        if (err != null) {
                            dataObject = new JSONObject(err);
                            returnCodeStr = JSONHelper.getStringValue(dataObject, "returnCode");
                            returnDesc = JSONHelper.getStringValue(dataObject, "returnDesc");
                        }
                        if (returnCodeStr == null) {
                            HwLog.d("returnCode is invalid.");
                            cardServerBaseResponse.returnCode = -99;
                            return;
                        }
                        cardServerBaseResponse.returnCode = Integer.parseInt(returnCodeStr);
                        if (cardServerBaseResponse.returnCode != 0) {
                            String err2 = new StringBuilder();
                            err2.append(" code:");
                            err2.append(cardServerBaseResponse.returnCode);
                            err2.append(" desc:");
                            err2.append(returnDesc);
                            err2 = err2.toString();
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("returnDesc : ");
                            stringBuilder4.append(err2);
                            HwLog.e(stringBuilder4.toString());
                            return;
                        }
                        try {
                            makeResponseData(cardServerBaseResponse, dataObject);
                        } catch (NumberFormatException e) {
                            ex = e;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("NumberFormatException : ");
                            stringBuilder.append(ex);
                            HwLog.e(stringBuilder.toString());
                            cardServerBaseResponse.returnCode = -99;
                        } catch (JSONException e2) {
                            ex2 = e2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("JSONException : ");
                            stringBuilder.append(ex2);
                            HwLog.e(stringBuilder.toString());
                            cardServerBaseResponse.returnCode = -99;
                        }
                    }
                }
                HwLog.d("unexpected error from server.");
                cardServerBaseResponse.returnCode = -99;
            } catch (NumberFormatException e3) {
                ex = e3;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NumberFormatException : ");
                stringBuilder.append(ex);
                HwLog.e(stringBuilder.toString());
                cardServerBaseResponse.returnCode = -99;
            } catch (JSONException e4) {
                ex2 = e4;
                stringBuilder = new StringBuilder();
                stringBuilder.append("JSONException : ");
                stringBuilder.append(ex2);
                HwLog.e(stringBuilder.toString());
                cardServerBaseResponse.returnCode = -99;
            }
        } catch (NumberFormatException e5) {
            ex = e5;
            str = responseStr;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NumberFormatException : ");
            stringBuilder.append(ex);
            HwLog.e(stringBuilder.toString());
            cardServerBaseResponse.returnCode = -99;
        } catch (JSONException e6) {
            ex2 = e6;
            str = responseStr;
            stringBuilder = new StringBuilder();
            stringBuilder.append("JSONException : ");
            stringBuilder.append(ex2);
            HwLog.e(stringBuilder.toString());
            cardServerBaseResponse.returnCode = -99;
        }
    }
}
