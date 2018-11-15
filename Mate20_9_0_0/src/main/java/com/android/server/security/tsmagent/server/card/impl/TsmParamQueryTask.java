package com.android.server.security.tsmagent.server.card.impl;

import android.content.Context;
import com.android.server.security.tsmagent.server.CardServerBaseRequest;
import com.android.server.security.tsmagent.server.card.request.TsmParamQueryRequest;
import com.android.server.security.tsmagent.server.card.response.TsmParamQueryResponse;
import com.android.server.security.tsmagent.server.wallet.impl.JSONHelper;
import com.android.server.security.tsmagent.utils.HwLog;
import com.android.server.security.tsmagent.utils.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class TsmParamQueryTask extends HttpConnTask {
    static final String TASK_COMMANDER_CREATE_SSD = "nfc.get.create.SSD";
    static final String TASK_COMMANDER_DEL_APP = "nfc.get.del.APP";
    static final String TASK_COMMANDER_DEL_SSD = "nfc.get.del.SSD";
    static final String TASK_COMMANDER_INFO_INIT = "nfc.get.NotifyEseInfoSync";
    static final String TASK_COMMANDER_INSTALL_APP = "nfc.get.install.APP";
    private static final int TSM_CHANNEL_HUAWEI = 0;
    private final String paramType;

    public TsmParamQueryTask(Context context, String url, String type) {
        super(context, url);
        this.paramType = type;
    }

    protected String prepareRequestStr(CardServerBaseRequest request) {
        if (!(request instanceof TsmParamQueryRequest)) {
            return null;
        }
        TsmParamQueryRequest castRequest = (TsmParamQueryRequest) request;
        if (StringUtil.isTrimedEmpty(castRequest.getSrcTransactionID()) || StringUtil.isTrimedEmpty(castRequest.getCplc()) || StringUtil.isTrimedEmpty(castRequest.getTerminal()) || StringUtil.isTrimedEmpty(this.paramType)) {
            HwLog.d("prepareRequestStr, params invalid.");
            return null;
        }
        return JSONHelper.createRequestStr(request.getMerchantID(), createDataStr(JSONHelper.createHeaderStr(request.getSrcTransactionID(), this.paramType), request, this.paramType), this.mContext);
    }

    public JSONObject createDataStr(JSONObject headerObject, CardServerBaseRequest request, String requestType) {
        if (headerObject == null || !(request instanceof TsmParamQueryRequest)) {
            return null;
        }
        JSONObject dataJson;
        TsmParamQueryRequest castRequest = (TsmParamQueryRequest) request;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createDataStr headerStr : ");
        stringBuilder.append(headerObject.toString());
        HwLog.d(stringBuilder.toString());
        try {
            dataJson = new JSONObject();
            dataJson.put("header", headerObject);
            dataJson.put("requestId", System.currentTimeMillis());
            if (!StringUtil.isTrimedEmpty(castRequest.getCplc())) {
                dataJson.put("cplc", "***************");
            }
            if (!StringUtil.isTrimedEmpty(castRequest.getAid())) {
                dataJson.put("aid", castRequest.getAid());
            }
            if (!StringUtil.isTrimedEmpty(castRequest.getTerminal())) {
                dataJson.put("terminal", castRequest.getTerminal());
            }
            dataJson.put("tsmChannel", 0);
            String signCommand = SignCommand.getSignCommand(requestType);
            if (signCommand != null) {
                if (!StringUtil.isTrimedEmpty(castRequest.getBankSignResult())) {
                    dataJson.put("sign", castRequest.getBankSignResult());
                }
                if (!StringUtil.isTrimedEmpty(castRequest.getBankSignTime())) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(signCommand);
                    stringBuilder2.append("|");
                    stringBuilder2.append(castRequest.getBankSignTime());
                    dataJson.put("content", stringBuilder2.toString());
                }
            }
            if (!StringUtil.isTrimedEmpty(castRequest.getBankRsaIndex())) {
                dataJson.put("rsaindex", castRequest.getBankRsaIndex());
            }
            if (!StringUtil.isTrimedEmpty(castRequest.getSignType())) {
                dataJson.put("signType", castRequest.getSignType());
            }
            if (castRequest.isDeleteRelatedObjects()) {
                dataJson.put("deleteRelatedObjects", castRequest.isDeleteRelatedObjects());
            }
            StringUtil.isTrimedEmpty(castRequest.getTsmParamIMEI());
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("createDataStr, dataJson: ");
            stringBuilder3.append(dataJson);
            HwLog.d(stringBuilder3.toString());
            if (!StringUtil.isTrimedEmpty(castRequest.getCplc())) {
                dataJson.put("cplc", castRequest.getCplc());
            }
            StringUtil.isTrimedEmpty(castRequest.getTsmParamIMEI());
        } catch (JSONException e) {
            HwLog.e("createDataStr, params invalid.");
            dataJson = null;
        }
        return dataJson;
    }

    protected TsmParamQueryResponse readErrorResponse(int errorCode) {
        TsmParamQueryResponse response = new TsmParamQueryResponse();
        if (-1 == errorCode) {
            response.returnCode = -1;
        } else if (-3 == errorCode) {
            response.returnCode = 1;
        } else if (-2 == errorCode) {
            response.returnCode = -2;
        } else if (-4 == errorCode) {
            response.returnCode = -4;
        }
        return response;
    }

    protected TsmParamQueryResponse readSuccessResponse(int returnCode, String returnDesc, JSONObject dataObject) {
        TsmParamQueryResponse response = new TsmParamQueryResponse();
        response.returnCode = returnCode;
        if (returnCode == 0) {
            try {
                String funcID = JSONHelper.getStringValue(dataObject, "funcID");
                String servicID = JSONHelper.getStringValue(dataObject, "servicID");
                if (StringUtil.isTrimedEmpty(funcID) || StringUtil.isTrimedEmpty(servicID)) {
                    HwLog.d("readSuccessResponse, illegal funcID or servicID");
                    response.returnCode = -99;
                } else {
                    response.funcID = funcID;
                    response.servicID = servicID;
                }
            } catch (JSONException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("readSuccessResponse, JSONException : ");
                stringBuilder.append(e);
                HwLog.e(stringBuilder.toString());
                response.returnCode = -99;
            }
        }
        return response;
    }
}
