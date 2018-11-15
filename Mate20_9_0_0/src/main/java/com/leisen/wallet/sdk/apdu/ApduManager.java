package com.leisen.wallet.sdk.apdu;

import android.content.Context;
import com.android.server.security.tsmagent.logic.spi.tsm.laser.LaserTSMServiceImpl;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.leisen.wallet.sdk.AppConfig;
import com.leisen.wallet.sdk.bean.CommonRequestParams;
import com.leisen.wallet.sdk.bean.OperAppletReqParams;
import com.leisen.wallet.sdk.business.ApduBean;
import com.leisen.wallet.sdk.business.ApduResBean;
import com.leisen.wallet.sdk.business.BaseBusinessForResp;
import com.leisen.wallet.sdk.business.BaseResponse;
import com.leisen.wallet.sdk.newhttp.AsyncHttpClientX;
import com.leisen.wallet.sdk.newhttp.SimpleResponseHandlerX;
import com.leisen.wallet.sdk.oma.SmartCard;
import com.leisen.wallet.sdk.tsm.TSMOperator;
import com.leisen.wallet.sdk.tsm.TSMOperatorResponse;
import com.leisen.wallet.sdk.util.AppJsonUtil;
import com.leisen.wallet.sdk.util.LogUtil;
import java.util.ArrayList;
import java.util.List;

public class ApduManager extends SimpleResponseHandlerX {
    private static final String BOUNDARY = "==>";
    private static final int BUSINESS_TYPE_ACTIVATE = 35;
    private static final int BUSINESS_TYPE_APPLETOPER = 30;
    private static final int BUSINESS_TYPE_COMMON_METHOD = 40;
    private static final int BUSINESS_TYPE_GPACOPER = 32;
    private static final int BUSINESS_TYPE_INFOINIT = 34;
    private static final int BUSINESS_TYPE_INFOSYNC = 33;
    private static final int BUSINESS_TYPE_SSDOPER = 31;
    private static final int FLAG_ACTIVATE_APPLET = 9;
    private static final int FLAG_ESEINFOSYNC = 2;
    private static final int FLAG_GETCIN = 7;
    private static final int FLAG_GETCPLC = 6;
    private static final int FLAG_GETIIN = 8;
    private static final int FLAG_INFOINIT = 1;
    private static final int FLAG_OPERAPPLET = 4;
    private static final int FLAG_OPERGPAC = 5;
    private static final int FLAG_OPERSSD = 3;
    public static final int SEND_TYPE_FIRST = 1;
    public static final int SEND_TYPE_NEXT = 2;
    private static final String TAG = "ApduManager";
    private ApduResponseHandler mApduResponseHandler = new ApduResponseHandler() {
        public void onSuccess(String response) {
            if (ApduManager.this.mTsmOperatorResponse != null) {
                ApduManager.this.mTsmOperatorResponse.onOperSuccess(response);
            }
            ApduManager.this.clearData();
        }

        public void onSendNext(int result, int index, String rapdu, String sw) {
            ApduManager.this.mCurrentTaskIndex = ApduManager.this.mCurrentTaskIndex + 1;
            ApduManager.this.sendNextApdu(result, index, rapdu, sw);
        }

        public void OnSendNextError(int result, int index, String rapdu, String sw, Error e) {
            ApduManager.this.mErrorMessage = e.getMessage();
            ApduManager.this.mCurrentTaskIndex = ApduManager.this.mCurrentTaskIndex + 1;
            ApduManager.this.sendNextApdu(result, index, rapdu, sw);
        }

        public void onFailure(int result, Error e) {
            if (ApduManager.this.mTsmOperatorResponse != null) {
                ApduManager.this.mTsmOperatorResponse.onOperFailure(result, e);
            }
            ApduManager.this.clearData();
        }
    };
    private ApduSmartCardRequest mApduSmartCardRequest;
    private AsyncHttpClientX mAsyncHttpClient;
    private int mBusinessType = -1;
    private CommonRequestParams mCommonRequestParams;
    private Context mContext;
    private int mCurrentTaskIndex = 1;
    private String mErrorMessage;
    private TSMOperatorResponse mTsmOperatorResponse;

    public ApduManager(Context context) {
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mAsyncHttpClient = new AsyncHttpClientX(true);
        this.mApduSmartCardRequest = new ApduSmartCardRequest(this.mContext, this.mApduResponseHandler);
    }

    public void requestInfoInit(CommonRequestParams params) {
        this.mBusinessType = 34;
        this.mApduSmartCardRequest.setFlag(1);
        this.mCommonRequestParams = params;
        sendFirstApdu(AppJsonUtil.getBaseReqJsonResult(params, this.mBusinessType, this.mCurrentTaskIndex));
    }

    public void requestEseInfoSync(CommonRequestParams params) {
        this.mBusinessType = 33;
        this.mApduSmartCardRequest.setFlag(2);
        this.mCommonRequestParams = params;
        sendFirstApdu(AppJsonUtil.getBaseReqJsonResult(params, this.mBusinessType, this.mCurrentTaskIndex));
    }

    public void requestOperSSD(int operType, CommonRequestParams params, String ssdAid) {
        this.mBusinessType = 31;
        this.mApduSmartCardRequest.setFlag(3);
        this.mCommonRequestParams = params;
        sendFirstApdu(AppJsonUtil.getOperSSDJsonResult(params, this.mBusinessType, operType, ssdAid, this.mCurrentTaskIndex));
    }

    public void requestOperApplet(int operType, CommonRequestParams params, OperAppletReqParams reqParams) {
        this.mBusinessType = 30;
        this.mApduSmartCardRequest.setFlag(4);
        this.mCommonRequestParams = params;
        sendFirstApdu(AppJsonUtil.getOperAppletJsonResult(params, this.mBusinessType, operType, reqParams, this.mCurrentTaskIndex));
    }

    public void requestactivateApplet(CommonRequestParams params, String appletAid) {
        this.mBusinessType = 35;
        this.mApduSmartCardRequest.setFlag(9);
        this.mCommonRequestParams = params;
        sendFirstApdu(AppJsonUtil.getActivateAppletJsonResult(params, this.mBusinessType, appletAid, this.mCurrentTaskIndex));
    }

    public void requestCommonMethod(CommonRequestParams params) {
        this.mBusinessType = 40;
        this.mApduSmartCardRequest.setFlag(-1);
        this.mCommonRequestParams = params;
        sendFirstApdu(AppJsonUtil.getBaseReqJsonResult(params, this.mBusinessType, this.mCurrentTaskIndex));
    }

    public void requestOperGPAC(int operType, CommonRequestParams params, String appletAid) {
        this.mBusinessType = 32;
        this.mApduSmartCardRequest.setFlag(5);
        this.mCommonRequestParams = params;
        sendFirstApdu(AppJsonUtil.getOperGPACJsonResult(params, this.mBusinessType, operType, appletAid, this.mCurrentTaskIndex));
    }

    public void requestGetCPLC(String aid) {
        this.mApduSmartCardRequest.setFlag(6);
        this.mApduSmartCardRequest.isGetLocalData(true);
        this.mApduSmartCardRequest.setGetLocalDataApdu(AppConfig.APDU_GETCPLC, aid);
        sendRequestToSmartCard();
    }

    public void requestGetCIN(String aid) {
        this.mApduSmartCardRequest.setFlag(7);
        this.mApduSmartCardRequest.isGetLocalData(true);
        this.mApduSmartCardRequest.setGetLocalDataApdu(AppConfig.APDU_GETCIN, aid);
        sendRequestToSmartCard();
    }

    public void requestGetIIN(String aid) {
        this.mApduSmartCardRequest.setFlag(8);
        this.mApduSmartCardRequest.isGetLocalData(true);
        this.mApduSmartCardRequest.setGetLocalDataApdu(AppConfig.APDU_GETIIN, aid);
        sendRequestToSmartCard();
    }

    public void requestSelectSSD(String aid) {
        this.mApduSmartCardRequest.isGetLocalData(true);
        String[] strArr = new String[2];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("00A40400");
        Object[] objArr = new Object[1];
        int i = 0;
        objArr[0] = Integer.valueOf(aid.length() / 2);
        stringBuilder.append(String.format("%02X", objArr));
        stringBuilder.append(aid);
        strArr[0] = stringBuilder.toString();
        strArr[1] = "";
        String[] capduArr = strArr;
        List<ApduBean> capdus = new ArrayList();
        while (true) {
            int i2 = i;
            if (i2 < capduArr.length) {
                capdus.add(new ApduBean(capduArr[i2]));
                i = i2 + 1;
            } else {
                this.mApduSmartCardRequest.setCapduList(capdus);
                sendRequestToSmartCard();
                return;
            }
        }
    }

    public void requestActivateApplet(String apdu) {
        this.mApduSmartCardRequest.isGetLocalData(true);
        capduArr = new String[2];
        int i = 0;
        capduArr[0] = "00A4040008A00000015143525300";
        capduArr[1] = apdu;
        List<ApduBean> capdus = new ArrayList();
        while (true) {
            int i2 = i;
            if (i2 < capduArr.length) {
                capdus.add(new ApduBean(capduArr[i2]));
                i = i2 + 1;
            } else {
                this.mApduSmartCardRequest.setCapduList(capdus);
                sendRequestToSmartCard();
                return;
            }
        }
    }

    public void requestdeactivateApplet(String apdu) {
        this.mApduSmartCardRequest.isGetLocalData(true);
        capduArr = new String[2];
        int i = 0;
        capduArr[0] = "00A4040008A00000015143525300";
        capduArr[1] = apdu;
        List<ApduBean> capdus = new ArrayList();
        while (true) {
            int i2 = i;
            if (i2 < capduArr.length) {
                capdus.add(new ApduBean(capduArr[i2]));
                i = i2 + 1;
            } else {
                this.mApduSmartCardRequest.setCapduList(capdus);
                sendRequestToSmartCard();
                return;
            }
        }
    }

    private void sendFirstApdu(String request) {
        sendApduToServer(request);
    }

    private void sendNextApdu(int result, int index, String rapdu, String sw) {
        ApduResBean rapduList = new ApduResBean();
        rapduList.setIndex(index);
        rapduList.setApdu(rapdu);
        rapduList.setSw(sw);
        sendApduToServer(AppJsonUtil.getReqNextJsonResult(this.mCommonRequestParams, this.mBusinessType, rapduList, result, this.mCurrentTaskIndex));
    }

    private void sendApduToServer(String request) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("==>request url:");
        stringBuilder.append(AppConfig.STREAMURL);
        LogUtil.i(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("==>request:");
        stringBuilder.append(AppJsonUtil.removeSensitiveInfo(request));
        LogUtil.i(str, stringBuilder.toString());
        if (request != null) {
            try {
                this.mAsyncHttpClient.post(this.mContext, AppConfig.STREAMURL, request, this);
            } catch (Exception e) {
                this.mApduResponseHandler.sendFailureMessage(TSMOperator.RETURN_UNKNOW_ERROR, new Error(e.getMessage()));
            }
        }
    }

    public void setTsmOperatorResponse(TSMOperatorResponse tsmOperatorResponse) {
        this.mTsmOperatorResponse = tsmOperatorResponse;
    }

    private void sendRequestToSmartCard() {
        if (this.mApduSmartCardRequest != null) {
            this.mApduSmartCardRequest.run();
        }
    }

    private void clearData() {
        this.mCurrentTaskIndex = 1;
        this.mBusinessType = -1;
        this.mCommonRequestParams = null;
        SmartCard.getInstance().closeService();
    }

    public void onSuccess(String responseString) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("==>response:");
        stringBuilder.append(responseString);
        LogUtil.i(str, stringBuilder.toString());
        BaseResponse<BaseBusinessForResp> response = null;
        try {
            response = (BaseResponse) new Gson().fromJson(responseString, new TypeToken<BaseResponse<BaseBusinessForResp>>() {
            }.getType());
        } catch (JsonSyntaxException e) {
            this.mApduResponseHandler.sendFailureMessage(TSMOperator.RETURN_RESPONSE_PARSE_ERROR, new Error("response data parse failure"));
        }
        if (response == null) {
            this.mApduResponseHandler.sendFailureMessage(TSMOperator.RETURN_RESPONSE_PARSE_ERROR, new Error("response data is empty"));
        } else if (((BaseBusinessForResp) response.getBusiness()).getOperationResult() != LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS) {
            String operationDes = ((BaseBusinessForResp) response.getBusiness()).getOperationDes();
            if (!(this.mErrorMessage == null || "".equals(this.mErrorMessage))) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(operationDes);
                stringBuilder2.append(":");
                stringBuilder2.append(this.mErrorMessage);
                operationDes = stringBuilder2.toString();
                this.mErrorMessage = null;
            }
            this.mApduResponseHandler.sendFailureMessage(TSMOperator.RETURN_SERVER_ERROR, new Error(operationDes));
        } else {
            if (((BaseBusinessForResp) response.getBusiness()).getFinishFlag() == 0) {
                this.mApduResponseHandler.sendSuccessMessage(null);
            } else {
                this.mApduSmartCardRequest.setCapduList(((BaseBusinessForResp) response.getBusiness()).getCapduList());
                sendRequestToSmartCard();
            }
        }
    }

    public void OnFailure(String responseString, Throwable error) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("==>response:");
        stringBuilder.append(responseString);
        LogUtil.e(str, stringBuilder.toString());
        this.mApduResponseHandler.sendFailureMessage(TSMOperator.RETURN_NETWORK_ERROR, new Error(error.getMessage()));
    }
}
