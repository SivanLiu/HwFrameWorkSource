package com.android.server.security.tsmagent.logic.card.tsm;

import android.content.Context;
import android.os.Build;
import com.android.server.security.tsmagent.constant.ServiceConfig;
import com.android.server.security.tsmagent.logic.ese.ESEInfoManager;
import com.android.server.security.tsmagent.logic.spi.tsm.laser.LaserTSMServiceImpl;
import com.android.server.security.tsmagent.logic.spi.tsm.request.CommandRequest;
import com.android.server.security.tsmagent.server.card.request.TsmParamQueryRequest;
import com.android.server.security.tsmagent.server.card.response.TsmParamQueryResponse;
import com.android.server.security.tsmagent.utils.HwLog;
import com.android.server.security.tsmagent.utils.PhoneDeviceUtil;
import com.android.server.security.tsmagent.utils.StringUtil;

public abstract class TsmBaseOperator {
    public static final String SIGN_TYPE_SHA256 = "RSA256";
    public static final int TSM_OPERATE_RESULT_FAILED_CONN_UNAVAILABLE = -2;
    public static final int TSM_OPERATE_RESULT_FAILED_CPLC_ERRO = -3;
    public static final int TSM_OPERATE_RESULT_FAILED_NO_NETWORK = -1;
    public static final int TSM_OPERATE_RESULT_FAILED_UNKNOWN_ERROR = -99;
    public static final int TSM_OPERATE_RESULT_SUCCESS = 0;
    private final String mAction;
    protected Context mContext;

    abstract TsmParamQueryResponse queryOperateParams(TsmParamQueryRequest tsmParamQueryRequest);

    TsmBaseOperator(Context context, String action) {
        this.mContext = context;
        this.mAction = action;
    }

    public int excute() {
        HwLog.i("tsm operate.");
        String cplc = ESEInfoManager.getInstance(this.mContext).queryCplc();
        if (StringUtil.isTrimedEmpty(cplc)) {
            return -3;
        }
        TsmParamQueryResponse paramQueryResponse = queryOperateParams(createTsmParamQueryRequest(cplc, PhoneDeviceUtil.getSerialNumber(this.mContext)));
        if (paramQueryResponse == null) {
            HwLog.e("excute, query params failed.");
            return -99;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("excute tsm operate, query params result code:");
        stringBuilder.append(paramQueryResponse.returnCode);
        HwLog.i(stringBuilder.toString());
        int operateResult = -99;
        if (paramQueryResponse.returnCode != 0) {
            return translateReturnCode(paramQueryResponse.returnCode);
        }
        if (StringUtil.isTrimedEmpty(paramQueryResponse.funcID) || StringUtil.isTrimedEmpty(paramQueryResponse.servicID)) {
            HwLog.e("TsmBaseOperator", "funcID or serviceID illegal.");
            return -99;
        }
        CommandRequest commandRequest = createCommandRequest(cplc, paramQueryResponse);
        StringBuilder stringBuilder2 = new StringBuilder(128);
        stringBuilder2.append("excute, funcallID: ");
        stringBuilder2.append(commandRequest.getFuncCall());
        stringBuilder2.append(",serverID: ");
        stringBuilder2.append(commandRequest.getServerID());
        HwLog.d(stringBuilder2.toString());
        int excuteResult = LaserTSMServiceImpl.getInstance(this.mContext).excuteTsmCommand(commandRequest);
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("excute action result: ");
        stringBuilder3.append(String.valueOf(excuteResult));
        HwLog.i(stringBuilder3.toString());
        if (LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS == excuteResult) {
            operateResult = 0;
        } else {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Tsm excute err: 907125806, fail_action: ");
            stringBuilder3.append(this.mAction);
            stringBuilder3.append(", fail_code: ");
            stringBuilder3.append(String.valueOf(excuteResult));
            HwLog.e(stringBuilder3.toString());
        }
        return operateResult;
    }

    private TsmParamQueryRequest createTsmParamQueryRequest(String cplc, String imei) {
        if (imei != null && imei.length() > 16) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("operation code : ");
            stringBuilder.append(imei.length());
            HwLog.d(stringBuilder.toString());
            imei = imei.substring(0, 16);
        }
        return new TsmParamQueryRequest(cplc, ServiceConfig.WALLET_MERCHANT_ID, -1, ServiceConfig.WALLET_MERCHANT_ID, Build.MODEL, imei);
    }

    public CommandRequest createCommandRequest(String cplc, TsmParamQueryResponse paramQueryResponse) {
        return CommandRequest.build(cplc, paramQueryResponse.funcID, paramQueryResponse.servicID);
    }

    public int translateReturnCode(int returnCode) {
        if (-1 == returnCode) {
            return -1;
        }
        if (-2 == returnCode) {
            return -2;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tsm quire param err: 907125805, fail_action: ");
        stringBuilder.append(this.mAction);
        stringBuilder.append(", fail_code: ");
        stringBuilder.append(String.valueOf(returnCode));
        HwLog.e(stringBuilder.toString());
        return -99;
    }
}
