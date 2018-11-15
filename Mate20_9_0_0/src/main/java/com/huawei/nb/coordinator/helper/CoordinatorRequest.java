package com.huawei.nb.coordinator.helper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.text.TextUtils;
import com.huawei.nb.coordinator.NetWorkStateUtil;
import com.huawei.nb.coordinator.common.CoordinatorJsonAnalyzer;
import com.huawei.nb.coordinator.helper.http.HttpClient;
import com.huawei.nb.coordinator.helper.http.HttpRequest;
import com.huawei.nb.coordinator.helper.http.HttpRequestBody;
import com.huawei.nb.coordinator.helper.http.HttpResponse;
import com.huawei.nb.coordinator.helper.http.ResultStatusCodeEnum;
import com.huawei.nb.coordinator.helper.verify.IVerify;
import com.huawei.nb.coordinator.helper.verify.IVerifyVar;
import com.huawei.nb.coordinator.helper.verify.VerifyException;
import com.huawei.nb.coordinator.helper.verify.VerifyFactory;
import com.huawei.nb.coordinator.helper.verify.VerifyInfoHolder;
import com.huawei.nb.coordinator.helper.verify.VerifyInfoHolderFactory;
import com.huawei.nb.model.coordinator.CoordinatorAudit;
import com.huawei.nb.utils.DeviceUtil;
import com.huawei.nb.utils.logger.DSLog;
import com.huawei.nb.utils.reporter.audit.CoordinatorCloudErrorAudit;
import com.huawei.nb.utils.reporter.audit.CoordinatorNetworkErrorAudit;
import com.huawei.nb.utils.reporter.fault.DownloadFault;
import com.huawei.nb.utils.reporter.fault.HttpsFault;
import com.huawei.nb.utils.reporter.fault.SDKAPIFault;
import java.util.concurrent.TimeUnit;

class CoordinatorRequest {
    private static final int REQUEST_SUCCESS = 200;
    private String TAG;
    private String mAppId;
    private String mBusinessType;
    private Context mContext;
    private long mDataTrafficSize;
    private long mDelayMs;
    private String mFileName;
    private String mFileSavePath;
    private DataRequestListener mListener;
    private com.huawei.nb.coordinator.helper.http.HttpRequestBody.Builder mRequestBodyBuilder;
    private com.huawei.nb.coordinator.helper.http.HttpRequest.Builder mRequestBuilder;
    private int mRequestMode;
    private RequestResult mRequestResult;
    private int mRetryTimes;
    private VerifyInfoHolder mVerifyInfoHolder;
    private String requestType;
    private IVerify verify;

    public static class Builder {
        private CoordinatorRequest mCoordinatorRequest = new CoordinatorRequest();

        public Builder(Context context) {
            this.mCoordinatorRequest.mContext = context;
        }

        public Builder get() {
            this.mCoordinatorRequest.requestType = HttpClient.GET_TYPE;
            return this;
        }

        public Builder appId(String appId) {
            this.mCoordinatorRequest.mAppId = appId;
            return this;
        }

        public Builder post() {
            this.mCoordinatorRequest.requestType = HttpClient.POST_TYPE;
            return this;
        }

        public Builder delete() {
            this.mCoordinatorRequest.requestType = HttpClient.DELETE_TYPE;
            return this;
        }

        public Builder url(String url) {
            this.mCoordinatorRequest.mRequestBuilder.url(url);
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.mCoordinatorRequest.mRequestBuilder.connectTimeout(connectTimeout);
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.mCoordinatorRequest.mRequestBuilder.readTimeout(readTimeout);
            return this;
        }

        public Builder verifyMode(int verifyMode) {
            if (verifyMode == 1) {
                verifyMode = 0;
            }
            this.mCoordinatorRequest.mVerifyInfoHolder = VerifyInfoHolderFactory.getVerifyInfoHolder(verifyMode);
            return this;
        }

        public Builder addRequestHeader(String key, String value) {
            this.mCoordinatorRequest.mRequestBuilder.addRequestHeader(key, value);
            return this;
        }

        public Builder businessType(String businessType) {
            this.mCoordinatorRequest.mBusinessType = businessType;
            return this;
        }

        public Builder fileSavePath(String fileSavePath) {
            this.mCoordinatorRequest.mFileSavePath = fileSavePath;
            return this;
        }

        public Builder fileName(String fileName) {
            this.mCoordinatorRequest.mFileName = fileName;
            return this;
        }

        public Builder addRequestBody(String key, String value) {
            this.mCoordinatorRequest.mRequestBodyBuilder.add(key, value);
            return this;
        }

        public Builder addRequestBody(String json) {
            this.mCoordinatorRequest.mRequestBodyBuilder.addJsonBody(json);
            return this;
        }

        public Builder requestMode(int requestMode) {
            this.mCoordinatorRequest.mRequestMode = requestMode;
            return this;
        }

        public Builder retry(int retryTimes, long delayMs) {
            this.mCoordinatorRequest.mRetryTimes = retryTimes;
            this.mCoordinatorRequest.mDelayMs = delayMs;
            return this;
        }

        public Builder dataTrafficSize(long dataTrafficSize) {
            this.mCoordinatorRequest.mDataTrafficSize = dataTrafficSize;
            return this;
        }

        public CoordinatorRequest build() {
            return this.mCoordinatorRequest;
        }
    }

    /* synthetic */ CoordinatorRequest(AnonymousClass1 x0) {
        this();
    }

    private CoordinatorRequest() {
        this.TAG = "CoordinatorRequest";
        this.mAppId = IVerifyVar.APPID_VALUE;
        this.mBusinessType = BusinessTypeEnum.BIZ_TYPE_POLICY;
        this.requestType = HttpClient.GET_TYPE;
        this.mVerifyInfoHolder = VerifyInfoHolderFactory.getVerifyInfoHolder(0);
        this.mRequestBodyBuilder = new com.huawei.nb.coordinator.helper.http.HttpRequestBody.Builder();
        this.mRequestBuilder = new com.huawei.nb.coordinator.helper.http.HttpRequest.Builder();
        this.mRequestResult = new RequestResult();
    }

    public CoordinatorRequest setDataRequestListener(DataRequestListener callbackListener) {
        this.mListener = callbackListener;
        return this;
    }

    public void sendAsyncHttpRequest() {
        Thread requestThread = new Thread(new Runnable() {
            public void run() {
                CoordinatorRequest.this.request();
            }
        });
        requestThread.setPriority(10);
        requestThread.start();
    }

    public void sendHttpRequest() {
        request();
    }

    private void setRequestErrorInfo(String code, String errorMsg) {
        if (this.mRequestResult != null) {
            this.mRequestResult.setCode(code);
            this.mRequestResult.setDesc(errorMsg);
            this.mRequestResult.setMessage(errorMsg);
            if (this.mListener != null) {
                this.mListener.onFailure(this.mRequestResult);
            }
        }
        CoordinatorAudit coordinatorAudit = HelperDatabaseManager.createCoordinatorAudit(this.mContext);
        coordinatorAudit.setIsRequestSuccess(false);
        HelperDatabaseManager.insertCoordinatorAudit(this.mContext, coordinatorAudit);
        DSLog.e(this.TAG + errorMsg, new Object[0]);
    }

    private HttpResponse errResponse(int code, String errorMsg) {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(code);
        response.setResponseMsg(errorMsg);
        return response;
    }

    private void request() {
        try {
            printNetworkInfo();
            if (isServiceSwitchOn()) {
                HttpRequestBody requestBody = this.mRequestBodyBuilder.build();
                if (HttpClient.GET_TYPE.contentEquals(this.requestType)) {
                    this.mRequestBuilder.get(requestBody);
                } else if (HttpClient.POST_TYPE.contentEquals(this.requestType)) {
                    this.mRequestBuilder.post(requestBody);
                } else if (HttpClient.DELETE_TYPE.contentEquals(this.requestType)) {
                    this.mRequestBuilder.delete(requestBody);
                }
                int verifyMode = this.mVerifyInfoHolder.getVerifyMode();
                DSLog.d(this.TAG + " verify mode is " + verifyMode, new Object[0]);
                this.verify = VerifyFactory.getVerify(verifyMode);
                requestWithHttp(this.mRequestBuilder);
                return;
            }
            setRequestErrorInfo("code:-4", " CoordinatorService is not allowed start! ");
        } catch (Throwable throwable) {
            setRequestErrorInfo("code-1", this.TAG + " caught a throwable,message: " + throwable.getMessage() + ", cause:" + throwable.getCause());
        }
    }

    private boolean isServiceSwitchOn() {
        if (this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_COLLECT_POLICY) <= 0 && this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_POLICY) >= 0) {
            String packageName = this.mContext.getPackageName();
            DSLog.i(this.TAG + " Travel assitent business. the request is from : " + packageName, new Object[0]);
            if (TextUtils.isEmpty(packageName) || "com.huawei.nb.service".equals(packageName)) {
                if (!"com.huawei.nb.service".equals(packageName)) {
                    return false;
                }
                DSLog.d(this.TAG + " From ODMF inside, already check switch.", new Object[0]);
                return true;
            } else if (HelperDatabaseManager.getCoordinatorServiceFlag(this.mContext)) {
                DSLog.i(this.TAG + " SWITCH_ON ", new Object[0]);
                return true;
            } else {
                DSLog.i(this.TAG + " SWITCH_OFF ", new Object[0]);
                return false;
            }
        } else if (this.mBusinessType.equals(BusinessTypeEnum.BIZ_TYPE_DEFAULT) || this.mBusinessType.equals(BusinessTypeEnum.BIZ_TYPE_AI_MODEL_RESOURCE)) {
            return true;
        } else {
            DSLog.e(this.TAG + " Not a valid business.", new Object[0]);
            return false;
        }
    }

    private void printNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                int type = networkInfo.getType();
                State state = networkInfo.getState();
                if (state != null) {
                    DSLog.d(this.TAG + " Network Type:" + type + ",Network State:" + state.toString(), new Object[0]);
                }
            }
        }
    }

    private void requestWithHttp(com.huawei.nb.coordinator.helper.http.HttpRequest.Builder builder) {
        HttpResponse result = null;
        DSLog.d(this.TAG + " Request with http.", new Object[0]);
        CoordinatorAudit coordinatorAudit = HelperDatabaseManager.createCoordinatorAudit(this.mContext);
        boolean isTokenExpired = false;
        int lastNetworkType = NetWorkStateUtil.getCurrentNetWorkType(this.mContext);
        if (DeviceUtil.getDistrict().equals("Oversea")) {
            DSLog.e("Error: request occurred oversea,forbidden request.", new Object[0]);
            setRequestErrorInfo("code = -14", " District is oversea, request prohibited.");
            return;
        }
        int i = 1;
        while (i <= this.mRetryTimes) {
            if (this.mVerifyInfoHolder.isHasToken()) {
                String verifyToken = this.mVerifyInfoHolder.getVerifyToken();
                if (TextUtils.isEmpty(verifyToken) || this.mVerifyInfoHolder.isTokenExpired() || isTokenExpired) {
                    DSLog.d(this.TAG + " ready to request with server", new Object[0]);
                    result = requestWithAuth(builder, coordinatorAudit);
                } else {
                    DSLog.d(this.TAG + " ready to request with token", new Object[0]);
                    result = requestWithToken(builder, verifyToken, coordinatorAudit);
                }
            } else {
                DSLog.d(this.TAG + " ready to request with server", new Object[0]);
                result = requestWithAuth(builder, coordinatorAudit);
            }
            if (!isSuccessCode(result)) {
                isTokenExpired = isTokenOrSessionExpired(result);
                if (result.getStatusCode() != -7) {
                    DSLog.e(this.TAG + " Need to retry " + this.mRetryTimes + " times. This is the " + i + " time.", new Object[0]);
                    try {
                        TimeUnit.MILLISECONDS.sleep(this.mDelayMs);
                    } catch (InterruptedException e) {
                        DSLog.e(this.TAG + " retry delay is interrupted.", new Object[0]);
                    }
                    if (!isRetryAllowed(lastNetworkType, result)) {
                        break;
                    }
                    lastNetworkType = NetWorkStateUtil.getCurrentNetWorkType(this.mContext);
                    i++;
                } else {
                    DSLog.e(this.TAG + " Download was interrupt, stop retry!", new Object[0]);
                    break;
                }
            }
            if (i == 1) {
                coordinatorAudit.setIsNeedRetry(Long.valueOf(0));
            } else {
                coordinatorAudit.setIsNeedRetry(Long.valueOf(1));
            }
            onRequestSuccess(result, result.getResponseString(), coordinatorAudit);
            return;
        }
        if (result != null) {
            onRequestFailure(result, result.getResponseString(), coordinatorAudit);
        } else {
            setRequestErrorInfo("code = -2", " Fail to get request result, error: result is null.");
        }
    }

    private HttpResponse requestWithAuth(com.huawei.nb.coordinator.helper.http.HttpRequest.Builder builder, CoordinatorAudit coordinatorAudit) {
        try {
            if (!this.verify.generateAuthorization(this.mContext, builder, this.mAppId)) {
                return errResponse(-3, " verify error.");
            }
            coordinatorAudit.setSuccessVerifyTime(Long.valueOf(System.currentTimeMillis()));
            if (builder == null) {
                return errResponse(-2, "empty requestWithAuthenticate.");
            }
            DSLog.d(this.TAG + "request WithAuthenticate.", new Object[0]);
            HttpResponse response = getRequestResult(builder, coordinatorAudit);
            if (this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_COLLECT_POLICY) > 0 || this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_POLICY) < 0) {
                if (this.mBusinessType.equals(BusinessTypeEnum.BIZ_TYPE_AI_MODEL_RESOURCE)) {
                }
                return response;
            }
            String code = CoordinatorJsonAnalyzer.getJsonValue(response.getResponseString(), CoordinatorJsonAnalyzer.CODE_TYPE);
            if (isSuccessCode(response)) {
                return response;
            }
            if (TextUtils.isEmpty(code) || !(code.equals(IVerifyVar.CLOUD_DEVICE_CA_ERROR) || code.equals(IVerifyVar.SESSION_HAS_EXPIRED))) {
                DSLog.e(this.TAG + ": requestWithAuth failed this time. code: " + response.getStatusCode() + "msg:" + response.getResponseMsg(), new Object[0]);
                return response;
            }
            try {
                if (!this.verify.generateAuthorization(this.mContext, builder, this.mAppId)) {
                    return errResponse(-3, " verify error.");
                }
                coordinatorAudit.setSuccessVerifyTime(Long.valueOf(System.currentTimeMillis()));
                response = getRequestResult(builder, coordinatorAudit);
                this.mVerifyInfoHolder.setDeviceCASentFlag(false);
                return response;
            } catch (VerifyException e) {
                return errResponse(e.getCode(), e.getMsg());
            }
        } catch (VerifyException e2) {
            return errResponse(e2.getCode(), e2.getMsg());
        }
    }

    private boolean isSuccessCode(HttpResponse response) {
        String responseString = response.getResponseString();
        int statusCode = response.getStatusCode();
        if (this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_COLLECT_POLICY) > 0 || this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_POLICY) < 0) {
            if (this.mBusinessType.equals(BusinessTypeEnum.BIZ_TYPE_AI_MODEL_RESOURCE)) {
                if (200 != statusCode) {
                    return false;
                }
                return true;
            } else if (200 != statusCode) {
                return false;
            } else {
                return true;
            }
        } else if (this.mRequestMode != 0) {
            if (200 == statusCode) {
                return true;
            }
            return false;
        } else if (TextUtils.isEmpty(responseString)) {
            DSLog.d(this.TAG + " response string is empty.", new Object[0]);
            return false;
        } else {
            String expectedStatusCode = getStatusCode(this.mBusinessType);
            String code = CoordinatorJsonAnalyzer.getJsonValue(responseString, CoordinatorJsonAnalyzer.CODE_TYPE);
            String msg = CoordinatorJsonAnalyzer.getJsonValue(responseString, CoordinatorJsonAnalyzer.MSG_TYPE);
            DSLog.d(this.TAG + " Response code is " + code, " msg is" + msg);
            if (TextUtils.isEmpty(expectedStatusCode) || TextUtils.isEmpty(code) || !code.equals(expectedStatusCode)) {
                return false;
            }
            return true;
        }
    }

    private String getStatusCode(String businessType) {
        if (BusinessTypeEnum.BIZ_TYPE_POLICY.equals(businessType)) {
            return "PS200";
        }
        if (BusinessTypeEnum.BIZ_TYPE_FENCE.equals(businessType)) {
            return "FS20002";
        }
        if (BusinessTypeEnum.BIZ_TYPE_SMART_TRAVEL.equals(businessType)) {
            return "ST20000";
        }
        if (BusinessTypeEnum.BIZ_TYPE_APPLET.equals(businessType)) {
            return "AL20000";
        }
        if (BusinessTypeEnum.BIZ_TYPE_PUSH.equals(businessType)) {
            return "PU20000";
        }
        if (BusinessTypeEnum.BIZ_TYPE_SCENE_PACKAGE.equals(businessType)) {
            return "SP20000";
        }
        if (BusinessTypeEnum.BIZ_TYPE_COLLECT_POLICY.equals(businessType)) {
            return "CP20000";
        }
        return "200";
    }

    private HttpResponse getRequestResult(com.huawei.nb.coordinator.helper.http.HttpRequest.Builder requestBuilder, CoordinatorAudit coordinatorAudit) {
        HttpRequest httpRequest = requestBuilder.build();
        coordinatorAudit.setUrl(httpRequest.getUrl());
        return new HttpClient(this.mRequestMode, this.mFileSavePath, this.mFileName).newCall(httpRequest).setDataRequestListener(this.mListener).setDataTrafficSize(Long.valueOf(this.mDataTrafficSize)).syncExecute(coordinatorAudit);
    }

    private void onRequestSuccess(HttpResponse response, String responseString, CoordinatorAudit coordinatorAudit) {
        DSLog.d(this.TAG + " business type:" + this.mBusinessType + " request success.", new Object[0]);
        if (this.mVerifyInfoHolder.isHasToken() && this.mVerifyInfoHolder.isTokenExpired()) {
            this.mVerifyInfoHolder.updateToken(response, this.verify);
        }
        if (this.mListener != null) {
            String data = responseString;
            if (this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_COLLECT_POLICY) <= 0 && this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_POLICY) >= 0) {
                data = CoordinatorJsonAnalyzer.getJsonValue(responseString, CoordinatorJsonAnalyzer.DATA_TYPE);
            } else if (this.mBusinessType.equals(BusinessTypeEnum.BIZ_TYPE_AI_MODEL_RESOURCE)) {
                data = responseString;
            }
            this.mListener.onSuccess(data);
        }
        long currentTimeMillis = System.currentTimeMillis();
        coordinatorAudit.setDataSize(Long.valueOf(response.getResponseSize()));
        coordinatorAudit.setSuccessTransferTime(Long.valueOf(currentTimeMillis));
        coordinatorAudit.setIsRequestSuccess(true);
        HelperDatabaseManager.insertCoordinatorAudit(this.mContext, coordinatorAudit);
    }

    private void onRequestFailure(HttpResponse httpResponse, String responseString, CoordinatorAudit coordinatorAudit) {
        DSLog.e(this.TAG + " business type:" + this.mBusinessType + " request failure.", new Object[0]);
        boolean isShieldCloudError = false;
        if (this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_COLLECT_POLICY) > 0 || this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_POLICY) < 0) {
            setFailRequestResult(String.valueOf(httpResponse.getStatusCode()), httpResponse.getResponseMsg(), httpResponse.getHttpExceptionMsg());
        } else {
            String code = CoordinatorJsonAnalyzer.getJsonValue(responseString, CoordinatorJsonAnalyzer.CODE_TYPE);
            if (TextUtils.isEmpty(code)) {
                setFailRequestResult(String.valueOf(httpResponse.getStatusCode()), httpResponse.getResponseMsg(), httpResponse.getHttpExceptionMsg());
            } else {
                setFailRequestResult(code, CoordinatorJsonAnalyzer.getJsonValue(responseString, CoordinatorJsonAnalyzer.DESC_TYPE), CoordinatorJsonAnalyzer.getJsonValue(responseString, CoordinatorJsonAnalyzer.MSG_TYPE));
                isShieldCloudError = isShieldCloudError(code);
            }
        }
        this.mRequestResult.setUrl(httpResponse.getUrl());
        if (this.mListener != null) {
            this.mListener.onFailure(this.mRequestResult);
        } else {
            DSLog.e(this.TAG + " Fail to callback, error: listener is null.", new Object[0]);
        }
        failResultReport(httpResponse, isShieldCloudError, coordinatorAudit);
    }

    private void setFailRequestResult(String code, String desc, String msg) {
        this.mRequestResult.setCode(code);
        this.mRequestResult.setDesc(desc);
        this.mRequestResult.setMessage(msg);
    }

    private void failResultReport(HttpResponse httpResponse, boolean isShieldCloudError, CoordinatorAudit coordinatorAudit) {
        String errDetail = " Cause: " + httpResponse.getResponseMsg() + ", Code:" + httpResponse.getStatusCode() + ", Network:" + NetWorkStateUtil.getCurrentNetWorkType(this.mContext) + ", URL: " + httpResponse.getUrl() + ", PackageName" + this.mContext.getPackageName() + ", Version:" + DeviceUtil.getVersionName(this.mContext);
        switch (getErrorType(httpResponse, isShieldCloudError)) {
            case ResultStatusCodeEnum.TIME_OUT /*-13*/:
                DSLog.e(this.TAG + "Execute response timeout, audit report.", new Object[0]);
                CoordinatorNetworkErrorAudit.report(String.valueOf(httpResponse.getStatusCode()), httpResponse.getResponseMsg(), coordinatorAudit.getAppPackageName(), coordinatorAudit.getUrl(), coordinatorAudit.getNetWorkState(), coordinatorAudit.getRequestDate());
                break;
            case ResultStatusCodeEnum.GET_SIGNATURE_ERROR /*-12*/:
                DSLog.e(this.TAG + " Can not get signature, shield report.", new Object[0]);
                break;
            case ResultStatusCodeEnum.NETWORK_FAULT /*-11*/:
                DSLog.e(this.TAG + " Network is not connected, audit report.", new Object[0]);
                CoordinatorNetworkErrorAudit.report(String.valueOf(httpResponse.getStatusCode()), httpResponse.getResponseMsg(), coordinatorAudit.getAppPackageName(), coordinatorAudit.getUrl(), coordinatorAudit.getNetWorkState(), coordinatorAudit.getRequestDate());
                break;
            case ResultStatusCodeEnum.SHIELD_CLOUD_ERROR /*-10*/:
                DSLog.e(this.TAG + " Cloud error, audit report.", new Object[0]);
                CoordinatorCloudErrorAudit.report(String.valueOf(httpResponse.getStatusCode()), httpResponse.getHttpExceptionMsg(), this.mBusinessType, httpResponse.getResponseMsg(), coordinatorAudit.getAppPackageName(), coordinatorAudit.getUrl(), coordinatorAudit.getNetWorkState(), coordinatorAudit.getRequestDate());
                break;
            case ResultStatusCodeEnum.CONNECT_CLOUD_ERROR /*-9*/:
                DSLog.e(this.TAG + "Failed to connect to cloud, audit report.", new Object[0]);
                CoordinatorNetworkErrorAudit.report(String.valueOf(httpResponse.getStatusCode()), httpResponse.getResponseMsg(), coordinatorAudit.getAppPackageName(), coordinatorAudit.getUrl(), coordinatorAudit.getNetWorkState(), coordinatorAudit.getRequestDate());
                break;
            case ResultStatusCodeEnum.DOWNLOAD_ERROR /*-5*/:
                DownloadFault.report(errDetail);
                break;
            case ResultStatusCodeEnum.VERIFICATION_ERROR_CODE /*-3*/:
                DSLog.e(this.TAG + " verify error, audit report.", new Object[0]);
                break;
            case ResultStatusCodeEnum.INVALID_PARAMS_CODE /*-2*/:
                SDKAPIFault.report(errDetail);
                break;
            default:
                HttpsFault.report(errDetail);
                break;
        }
        coordinatorAudit.setIsRequestSuccess(false);
        HelperDatabaseManager.insertCoordinatorAudit(this.mContext, coordinatorAudit);
    }

    private int getErrorType(HttpResponse httpResponse, boolean isShieldCloudError) {
        if (isShieldCloudError) {
            return -10;
        }
        if (isNetworkFault(httpResponse)) {
            return -11;
        }
        if (isEmptySignatureFault(httpResponse)) {
            return -12;
        }
        return httpResponse.getStatusCode();
    }

    private boolean isShieldCloudError(String code) {
        code = code.trim();
        return IVerifyVar.PUBLICKEY_NOT_EXIST.equals(code) || IVerifyVar.CAN_NOT_CONNECT_TO_LDAP_SERVER.equals(code) || IVerifyVar.PUBLICKEY_NOT_EXIST_IN_DCS.equals(code) || IVerifyVar.PUBLICKEY_FORMAT_ERROR.equals(code) || IVerifyVar.DYNAMIC_CARD_INFO_NULL.equals(code);
    }

    private HttpResponse requestWithToken(com.huawei.nb.coordinator.helper.http.HttpRequest.Builder builder, String verifyToken, CoordinatorAudit coordinatorAudit) {
        HttpRequest request = builder.addRequestHeader(IVerifyVar.AUTHORIZATION_KEY, "PKI " + IVerifyVar.TOKEN_KEY + "=" + verifyToken).build();
        coordinatorAudit.setUrl(request.getUrl());
        return new HttpClient(this.mRequestMode, this.mFileSavePath, this.mFileName).newCall(request).setDataTrafficSize(Long.valueOf(this.mDataTrafficSize)).setDataRequestListener(this.mListener).syncExecute();
    }

    private boolean isTokenOrSessionExpired(HttpResponse result) {
        String responseString = result.getResponseString();
        if (this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_COLLECT_POLICY) <= 0 && this.mBusinessType.compareTo(BusinessTypeEnum.BIZ_TYPE_POLICY) >= 0) {
            String code = CoordinatorJsonAnalyzer.getJsonValue(responseString, CoordinatorJsonAnalyzer.CODE_TYPE).trim();
            if (!TextUtils.isEmpty(code) && (IVerifyVar.TOKEN_HAS_EXPIRED.equals(code) || IVerifyVar.SESSION_HAS_EXPIRED.equals(code))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRetryAllowed(int lastNetworkType, HttpResponse result) {
        int netWorkState = NetWorkStateUtil.getCurrentNetWorkType(this.mContext);
        if (netWorkState == 1) {
            return true;
        }
        if (netWorkState != 3 || lastNetworkType != 3) {
            return false;
        }
        if (result.isDownloadStart()) {
            return false;
        }
        return true;
    }

    private boolean isNetworkFault(HttpResponse httpResponse) {
        return httpResponse.getStatusCode() == -6;
    }

    private boolean isEmptySignatureFault(HttpResponse httpResponse) {
        return httpResponse.getStatusCode() == -8;
    }
}
