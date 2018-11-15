package com.huawei.nb.coordinator.helper;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.nb.coordinator.helper.CoordinatorRequest.Builder;
import com.huawei.nb.coordinator.helper.http.HttpClient;
import com.huawei.nb.model.coordinator.CoordinatorAudit;
import com.huawei.nb.utils.DeviceUtil;
import com.huawei.nb.utils.JsonUtils;
import com.huawei.nb.utils.logger.DSLog;
import com.huawei.nb.utils.reporter.fault.SDKAPIFault;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CoordinatorClient {
    private static final int DEFAULT_REQUEST = 0;
    private static final int FRAGMENT_LOAD_REQUEST = 1;
    private static final long MAX_DEFAULT_SIZE = 524288000;
    private static final int MAX_TIMES = 3;
    private static final String TAG = "CoordinatorClient";
    private static final int TRANSFER_FILE_REQUEST = 2;
    private long dataTrafficSize = 0;
    private long delayMs = 0;
    private DataRequestListener listener;
    private String mAppId;
    private String mBusinessType;
    private int mConnectTimeout;
    private Context mContext;
    private String mFileName;
    private String mFileSavePath;
    private String mJsonBody;
    private int mReadTimeout;
    private String mUrl;
    private int mVerifyMode;
    private Map<String, String> requestBody;
    private Map<String, String> requestHeader;
    private int requestMode;
    private String requestType;
    private int retryTimes = 0;

    public CoordinatorClient(Context context) {
        this.mContext = context;
        this.requestHeader = new LinkedHashMap();
        this.requestBody = new LinkedHashMap();
        this.mVerifyMode = 0;
        this.requestMode = 0;
        this.requestType = HttpClient.GET_TYPE;
    }

    public CoordinatorClient verifyMode(int verifyMode) {
        this.mVerifyMode = verifyMode;
        return this;
    }

    public CoordinatorClient fileSavePath(String fileSavePath) {
        this.mFileSavePath = fileSavePath;
        return this;
    }

    public CoordinatorClient fileName(String fileName) {
        this.mFileName = fileName;
        return this;
    }

    public CoordinatorClient appId(String appId) {
        this.mAppId = appId;
        return this;
    }

    public CoordinatorClient needTransferFile() {
        this.requestMode = 2;
        return this;
    }

    public CoordinatorClient fragmentLoad() {
        this.requestMode = 1;
        return this;
    }

    public CoordinatorClient businessType(String businessType) {
        this.mBusinessType = businessType;
        return this;
    }

    public CoordinatorClient url(String url) {
        this.mUrl = url;
        return this;
    }

    public CoordinatorClient connectTimeout(int connectTimeout) {
        this.mConnectTimeout = connectTimeout;
        return this;
    }

    public CoordinatorClient readTimeout(int readTimeout) {
        this.mReadTimeout = readTimeout;
        return this;
    }

    public CoordinatorClient jsonBody(String jsonBody) {
        this.mJsonBody = jsonBody;
        return this;
    }

    public CoordinatorClient addRequestHeader(String key, String value) {
        this.requestHeader.put(key, value);
        return this;
    }

    public CoordinatorClient addRequestBody(String key, String value) {
        this.requestBody.put(key, value);
        return this;
    }

    public CoordinatorClient post() {
        this.requestType = HttpClient.POST_TYPE;
        return this;
    }

    public CoordinatorClient get() {
        this.requestType = HttpClient.GET_TYPE;
        return this;
    }

    public CoordinatorClient delete() {
        this.requestType = HttpClient.DELETE_TYPE;
        return this;
    }

    public CoordinatorClient setRetry(int retryTimes, int delayMs) {
        this.retryTimes = retryTimes;
        this.delayMs = (long) delayMs;
        return this;
    }

    public CoordinatorClient setDownloadLimit(long dataTrafficSize) {
        this.dataTrafficSize = dataTrafficSize;
        return this;
    }

    public CoordinatorClient setDataRequestListener(DataRequestListener listener) {
        this.listener = listener;
        return this;
    }

    public void sendAsyncRequest() {
        CoordinatorRequest coordinatorRequest = getCoordinatorRequest();
        if (coordinatorRequest != null) {
            coordinatorRequest.sendAsyncHttpRequest();
        } else {
            DSLog.e("CoordinatorClient coordiantorRequest is null. Stop send async request.", new Object[0]);
        }
    }

    public void sendRequest() {
        CoordinatorRequest coordinatorRequest = getCoordinatorRequest();
        if (coordinatorRequest != null) {
            coordinatorRequest.sendHttpRequest();
        } else {
            DSLog.e("CoordinatorClient coordiantorRequest is null. Stop send request.", new Object[0]);
        }
    }

    private CoordinatorRequest getCoordinatorRequest() {
        Builder requestBuilder = new Builder(this.mContext);
        if (!TextUtils.isEmpty(this.mAppId)) {
            requestBuilder.appId(this.mAppId);
        }
        if (this.mConnectTimeout > 0) {
            requestBuilder.connectTimeout(this.mConnectTimeout);
        }
        if (this.mReadTimeout > 0) {
            requestBuilder.readTimeout(this.mReadTimeout);
        }
        if (TextUtils.isEmpty(this.mUrl)) {
            dealInvalidParams(" url is empty.");
            return null;
        }
        requestBuilder.url(this.mUrl);
        if (TextUtils.isEmpty(this.requestType)) {
            dealInvalidParams(" requestType is empty.");
            return null;
        }
        if (this.requestType.equals(HttpClient.POST_TYPE)) {
            requestBuilder.post();
        } else if (this.requestType.equals(HttpClient.GET_TYPE)) {
            if (!TextUtils.isEmpty(this.mJsonBody)) {
                DSLog.e("JsonBody doesn't work when request type is GET.", new Object[0]);
            }
            requestBuilder.get();
        } else if (this.requestType.equals(HttpClient.DELETE_TYPE)) {
            if (!TextUtils.isEmpty(this.mJsonBody)) {
                DSLog.e("JsonBody doesn't work when request type is DELETE.", new Object[0]);
            }
            requestBuilder.delete();
        } else {
            dealInvalidParams(" requestType is invalid.");
            return null;
        }
        requestBuilder.requestMode(this.requestMode);
        if (!TextUtils.isEmpty(this.mFileSavePath)) {
            requestBuilder.fileSavePath(this.mFileSavePath);
        }
        if (!TextUtils.isEmpty(this.mFileName)) {
            requestBuilder.fileName(this.mFileName);
        }
        if (TextUtils.isEmpty(this.mBusinessType)) {
            DSLog.d(" BusinessType is empty.", new Object[0]);
        } else {
            requestBuilder.businessType(this.mBusinessType);
        }
        if (!TextUtils.isEmpty(this.mJsonBody)) {
            if (JsonUtils.isValidJson(this.mJsonBody)) {
                requestBuilder.addRequestBody(this.mJsonBody);
            } else {
                dealInvalidParams(" JsonBody is invalid.");
                return null;
            }
        }
        if (this.requestBody != null && this.requestBody.size() > 0) {
            if (this.mJsonBody != null) {
                DSLog.i("Both requestBody and JsonBody are not empty. Only JsonBody works.", new Object[0]);
            }
            for (Entry entry : this.requestBody.entrySet()) {
                requestBuilder.addRequestBody((String) entry.getKey(), (String) entry.getValue());
            }
        }
        if (this.requestHeader != null && this.requestHeader.size() > 0) {
            for (Entry entry2 : this.requestHeader.entrySet()) {
                requestBuilder.addRequestHeader((String) entry2.getKey(), (String) entry2.getValue());
            }
        }
        requestBuilder.verifyMode(this.mVerifyMode);
        if (this.retryTimes <= 0) {
            DSLog.i("CoordinatorClient retryTimes is illegal. Reset to 3 times.", new Object[0]);
            this.retryTimes = 3;
        }
        if (this.delayMs < 0) {
            DSLog.i("CoordinatorClient delayMs is illegal. Reset to 0ms.", new Object[0]);
            this.delayMs = 0;
        }
        requestBuilder.retry(this.retryTimes, this.delayMs);
        DSLog.d("CoordinatorClient DataTrafficSize is " + this.dataTrafficSize, new Object[0]);
        if (this.dataTrafficSize <= 0) {
            DSLog.d("CoordinatorClient DataTrafficSize is " + this.dataTrafficSize + ", reset to " + MAX_DEFAULT_SIZE, new Object[0]);
            this.dataTrafficSize = MAX_DEFAULT_SIZE;
        }
        requestBuilder.dataTrafficSize(this.dataTrafficSize);
        CoordinatorRequest request = requestBuilder.build();
        if (this.listener == null) {
            DSLog.e(" listener is invalid, request stop.", new Object[0]);
            SDKAPIFault.report("listener is invalid.");
            return null;
        }
        request.setDataRequestListener(this.listener);
        return request;
    }

    private void dealInvalidParams(String msg) {
        DSLog.e("CoordinatorClient " + msg, new Object[0]);
        if (this.listener != null) {
            RequestResult requestResult = new RequestResult();
            requestResult.setCode("fail code: -2");
            requestResult.setMessage("fail msg: " + msg);
            this.listener.onFailure(requestResult);
        }
        StringBuilder errDetail = new StringBuilder();
        errDetail.append(" Illegal usage, error: input param is invalid.");
        errDetail.append(" PackageName: ");
        errDetail.append(this.mContext.getPackageName());
        errDetail.append(" Version: ");
        errDetail.append(DeviceUtil.getVersionName(this.mContext));
        SDKAPIFault.report(errDetail.toString());
        CoordinatorAudit coordinatorAudit = HelperDatabaseManager.createCoordinatorAudit(this.mContext);
        coordinatorAudit.setIsRequestSuccess(false);
        HelperDatabaseManager.insertCoordinatorAudit(this.mContext, coordinatorAudit);
    }
}
