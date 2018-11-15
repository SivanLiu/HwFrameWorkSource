package com.huawei.nb.client.ai;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import com.huawei.nb.ai.AiModelRequest;
import com.huawei.nb.ai.AiModelResponse;
import com.huawei.nb.client.DataServiceProxy;
import com.huawei.nb.client.ServiceConnectCallback;
import com.huawei.nb.client.callback.DeleteResInfoCallBack;
import com.huawei.nb.client.callback.DeleteResInfoCallBackAgent;
import com.huawei.nb.client.callback.UpdatePackageCallBack;
import com.huawei.nb.client.callback.UpdatePackageCallBackAgent;
import com.huawei.nb.client.callback.UpdatePackageCheckCallBack;
import com.huawei.nb.client.callback.UpdatePackageCheckCallBackAgent;
import com.huawei.nb.container.ObjectContainer;
import com.huawei.nb.model.aimodel.AiModel;
import com.huawei.nb.model.coordinator.CoordinatorSwitch;
import com.huawei.nb.model.coordinator.ResourceInformation;
import com.huawei.nb.notification.ModelObserver;
import com.huawei.nb.notification.ObserverType;
import com.huawei.nb.query.Query;
import com.huawei.nb.security.RSAEncryptUtils;
import com.huawei.nb.utils.Waiter;
import com.huawei.nb.utils.logger.DSLog;
import com.huawei.odmf.core.AManagedObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AiModelClientAgent {
    private static final long CONNECT_TIMEOUT = 3000;
    private static final String TAG = "AiModelClientAgent";
    private Context context;
    private final DataServiceProxy dsProxy;
    private boolean inSyncMode = true;
    private final Map<String, Object> keyMap;

    private static class InnerServiceConnectCallback implements ServiceConnectCallback {
        private final Waiter waiter;

        /* synthetic */ InnerServiceConnectCallback(Waiter x0, AnonymousClass1 x1) {
            this(x0);
        }

        private InnerServiceConnectCallback(Waiter waiter) {
            this.waiter = waiter;
        }

        public void onConnect() {
            if (this.waiter != null) {
                this.waiter.signal();
            }
        }

        public void onDisconnect() {
            if (this.waiter != null) {
                this.waiter.signal();
            }
        }
    }

    private static class _DeleteResInfoCallBack implements DeleteResInfoCallBack {
        boolean[] isDelete = null;
        CountDownLatch latch = null;

        _DeleteResInfoCallBack(boolean[] isDelete, CountDownLatch latch) {
            this.isDelete = isDelete;
            this.latch = latch;
        }

        public void onSuccess() {
            this.isDelete[0] = true;
            this.latch.countDown();
        }

        public void onFailure(int errorCode, String errorMessage) {
            if (errorMessage == null) {
                errorMessage = "";
            }
            DSLog.d("AiModelClientAgent Fail to delete res info,error: errorCode = " + errorCode + "errorMessage = " + errorMessage, new Object[0]);
            this.isDelete[0] = false;
            this.latch.countDown();
        }
    }

    private static class _DeleteResInfoCallBackAgent extends DeleteResInfoCallBackAgent {
        private DeleteResInfoCallBack mCb = null;

        _DeleteResInfoCallBackAgent(DeleteResInfoCallBack cb) {
            this.mCb = cb;
        }

        public void onSuccess() {
            this.mCb.onSuccess();
        }

        public void onFailure(int errorCode, String errorMessage) {
            if (errorMessage == null) {
                errorMessage = "";
            }
            this.mCb.onFailure(errorCode, errorMessage);
        }
    }

    final /* bridge */ /* synthetic */ void bridge$lambda$0$AiModelClientAgent(AiModelResponse aiModelResponse) {
        setPrivateKey(aiModelResponse);
    }

    final /* bridge */ /* synthetic */ void bridge$lambda$1$AiModelClientAgent(AiModelRequest aiModelRequest) {
        setPublicKey(aiModelRequest);
    }

    public AiModelClientAgent(Context context) {
        this.dsProxy = new DataServiceProxy(context);
        this.keyMap = RSAEncryptUtils.generateKeyPair();
        this.context = context;
    }

    public boolean connect() {
        if (this.dsProxy.hasConnected()) {
            return true;
        }
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            DSLog.e("AiModelClientAgent!!!!!!AiModelClientAgent connect can't be invoked in Main Thread!!!!!!", new Object[0]);
            return false;
        }
        Waiter waiter = new Waiter();
        if (!this.dsProxy.connect(new InnerServiceConnectCallback(waiter, null))) {
            return false;
        }
        if (waiter.await(CONNECT_TIMEOUT)) {
            return true;
        }
        DSLog.e("AiModelClientAgentFailed to connect to NaturalBase Data Service in %s ms.", Long.valueOf(CONNECT_TIMEOUT));
        return false;
    }

    public boolean connect(ServiceConnectCallback callback) {
        return this.dsProxy.connect(callback);
    }

    public boolean hasConnected() {
        return this.dsProxy.hasConnected();
    }

    public void setExecutionTimeout(long timeout) {
        if (timeout > 0) {
            this.inSyncMode = false;
            this.dsProxy.setExecutionTimeout(timeout);
        }
    }

    public void disconnect() {
        this.dsProxy.disconnect();
    }

    private void setPrivateKey(AiModelResponse response) {
        if (response != null) {
            response.setPrivateKey(RSAEncryptUtils.getPrivateKey(this.keyMap));
        }
    }

    private void setPublicKey(AiModelRequest request) {
        if (request != null) {
            request.setPublicKey(RSAEncryptUtils.getPublicKey(this.keyMap));
        }
    }

    private boolean batchValidRequest(List<AiModelRequest> requestList) {
        for (AiModelRequest request : requestList) {
            if (request != null) {
                if (!request.isValid()) {
                }
            }
            return false;
        }
        return true;
    }

    private void batchSetPrivateKey(List<AiModelResponse> responseList) {
        responseList.forEach(new AiModelClientAgent$$Lambda$0(this));
    }

    private void batchSetPublicKey(List<AiModelRequest> requestList) {
        requestList.forEach(new AiModelClientAgent$$Lambda$1(this));
    }

    public AiModelResponse requestAiModel(AiModelRequest request) {
        List<AiModelResponse> responseList = requestAiModel(Arrays.asList(new AiModelRequest[]{request}));
        return (responseList == null || responseList.isEmpty()) ? null : (AiModelResponse) responseList.get(0);
    }

    public List<AiModelResponse> requestAiModel(List<AiModelRequest> requestList) {
        if (requestList == null || !batchValidRequest(requestList)) {
            DSLog.e("AiModelClientAgentFailed to get ai models, error: invalid parameter.", new Object[0]);
            return null;
        }
        ObjectContainer<AiModelResponse> responseContainer;
        List<AiModelResponse> responseList;
        batchSetPublicKey(requestList);
        ObjectContainer<AiModelRequest> requestContainer = new ObjectContainer(AiModelRequest.class, requestList);
        if (this.inSyncMode) {
            responseContainer = this.dsProxy.requestAiModel(requestContainer);
        } else {
            responseContainer = this.dsProxy.requestAiModelAsync(requestContainer);
        }
        if (responseContainer != null) {
            responseList = responseContainer.get();
        } else {
            responseList = null;
        }
        if (responseList == null || responseList.size() != requestList.size()) {
            int i;
            String str = "AiModelClientAgentFailed to get ai models, error: just get %s for %s requests.";
            Object[] objArr = new Object[2];
            if (responseList == null) {
                i = 0;
            } else {
                i = responseList.size();
            }
            objArr[0] = Integer.valueOf(i);
            objArr[1] = Integer.valueOf(requestList.size());
            DSLog.e(str, objArr);
            return null;
        }
        batchSetPrivateKey(responseList);
        return responseList;
    }

    public ResourceInformation queryResInfo(String resid) {
        if (resid == null || resid.isEmpty()) {
            DSLog.e("AiModelClientAgentFailed to query AiModel ResourceInformation, error: invalid parameter.", new Object[0]);
            return null;
        }
        return (ResourceInformation) this.dsProxy.executeSingleQuery(Query.select(ResourceInformation.class).equalTo("resid", resid));
    }

    public boolean deleteResInfo(ResourceInformation resourceInformation) {
        if (isAllowChangeInfo(resourceInformation)) {
            boolean[] isDelete = new boolean[]{false};
            CountDownLatch latch = new CountDownLatch(1);
            deleteResInfo(Arrays.asList(new ResourceInformation[]{resourceInformation}), new _DeleteResInfoCallBack(isDelete, latch));
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    isDelete[0] = false;
                    DSLog.d("AiModelClientAgent Fail to delete res info,error: wait for callback timeout.", new Object[0]);
                }
            } catch (InterruptedException e) {
                isDelete[0] = false;
            }
            DSLog.d("AiModelClientAgentResourceInformation is deleted, %s.", Boolean.valueOf(isDelete[0]));
            return isDelete[0];
        }
        DSLog.e("AiModelClientAgentFail to delete ResourceInformation, error: not allow to delete.", new Object[0]);
        return false;
    }

    public boolean updateResInfo(ResourceInformation resourceInformation) {
        if (isAllowChangeInfo(resourceInformation)) {
            ResourceInformation resInfo = (ResourceInformation) this.dsProxy.executeSingleQuery(Query.select(ResourceInformation.class).equalTo("resid", resourceInformation.getResid()));
            if (resInfo == null) {
                DSLog.e("AiModelClientAgentFail to update ResourceInformation, error: query resid failed.", new Object[0]);
                return false;
            }
            ResourceInformation queryResult = resInfo;
            queryResult.setAppVersion(resourceInformation.getAppVersion());
            DSLog.d("AiModelClientAgentResourceInformation is updated, %s.", Boolean.valueOf(this.dsProxy.updateResInfoAgent(queryResult)));
            return this.dsProxy.updateResInfoAgent(queryResult);
        }
        DSLog.e("AiModelClientAgentFail to update ResourceInformation, error: not allow to update.", new Object[0]);
        return false;
    }

    public ResourceInformation insertResInfo(ResourceInformation resourceInformation) {
        if (!isResInfoValid(resourceInformation)) {
            DSLog.e("AiModelClientAgentFail to insert ResourceInformation, error: resource information is invalid.", new Object[0]);
            return null;
        } else if (!presetResInfo(resourceInformation)) {
            DSLog.e("AiModelClientAgentFail to insert ResourceInformation, error: can not preset invalid information.", new Object[0]);
            return null;
        } else if (isAllowChangeInfo(resourceInformation)) {
            ObjectContainer insertResult = this.dsProxy.insertResInfoAgent(resourceInformation);
            if (insertResult == null || insertResult.get().isEmpty()) {
                DSLog.e("AiModelClientAgentFail to insert ResourceInformation, error: insert result is empty.", new Object[0]);
                return null;
            } else if (insertResult.get().get(0) instanceof ResourceInformation) {
                return (ResourceInformation) insertResult.get().get(0);
            } else {
                DSLog.e("AiModelClientAgentFail to insert ResourceInformation, error: result instanceof ResourceInformation is false.", new Object[0]);
                return null;
            }
        } else {
            DSLog.e("AiModelClientAgentFail to insert ResourceInformation, error: not allow to insert.", new Object[0]);
            return null;
        }
    }

    private boolean isAllowChangeInfo(ResourceInformation resourceInformation) {
        if (this.context.getPackageName() != null && resourceInformation != null && resourceInformation.getPackageName() != null) {
            return true;
        }
        DSLog.e("AiModelClientAgentFailed to change AiModel ResourceInformation, error: invalid parameter. Context package name is" + this.context.getPackageName(), new Object[0]);
        if (resourceInformation == null) {
            DSLog.e("AiModelClientAgent res info is null.", new Object[0]);
            return false;
        }
        DSLog.e(" res info package name is " + resourceInformation.getPackageName(), new Object[0]);
        return false;
    }

    public CoordinatorSwitch querySwitch(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            DSLog.e("AiModelClientAgentFailed to query AiModel ResourceInformation, error: invalid parameter.", new Object[0]);
            return null;
        }
        CoordinatorSwitch coordinatorSwitch = (CoordinatorSwitch) this.dsProxy.executeSingleQuery(Query.select(CoordinatorSwitch.class).equalTo("serviceName", serviceName));
        if (coordinatorSwitch != null) {
            DSLog.d("AiModelClientAgentCoordinatorSwitch is found, value is %s.", Boolean.valueOf(coordinatorSwitch.getIsSwitchOn()));
            return coordinatorSwitch;
        }
        DSLog.e("AiModelClientAgentCoordinatorSwitch is not found.", new Object[0]);
        return coordinatorSwitch;
    }

    public boolean deleteSwitch(CoordinatorSwitch coordinatorSwitch) {
        if (!isAllowChangeSwitch(coordinatorSwitch)) {
            return false;
        }
        DSLog.d("AiModelClientAgentCoordinatorSwitch is deleted %s.", Boolean.valueOf(this.dsProxy.executeDelete((AManagedObject) coordinatorSwitch)));
        return this.dsProxy.executeDelete((AManagedObject) coordinatorSwitch);
    }

    public boolean updateSwitch(CoordinatorSwitch coordinatorSwitch) {
        if (!isAllowChangeSwitch(coordinatorSwitch)) {
            return false;
        }
        if (this.dsProxy.executeUpdate((AManagedObject) coordinatorSwitch)) {
            DSLog.d("AiModelClientAgentCoordinatorSwitch is update as %s.", Boolean.valueOf(coordinatorSwitch.getIsSwitchOn()));
            return true;
        }
        DSLog.d("AiModelClientAgentFailed to update CoordinatorSwitch as %s.", Boolean.valueOf(coordinatorSwitch.getIsSwitchOn()));
        return false;
    }

    public CoordinatorSwitch insertSwitch(CoordinatorSwitch coordinatorSwitch) {
        if (!isAllowChangeSwitch(coordinatorSwitch)) {
            return null;
        }
        AManagedObject insertedSwitch = this.dsProxy.executeInsert((AManagedObject) coordinatorSwitch);
        if (!(insertedSwitch instanceof CoordinatorSwitch)) {
            return null;
        }
        DSLog.d("AiModelClientAgentCoordinatorSwitch inserted, value is %s.", Boolean.valueOf(coordinatorSwitch.getIsSwitchOn()));
        return (CoordinatorSwitch) insertedSwitch;
    }

    private boolean isAllowChangeSwitch(CoordinatorSwitch coordinatorSwitch) {
        if (this.context.getPackageName() == null || coordinatorSwitch == null || coordinatorSwitch.getPackageName() == null) {
            DSLog.e("AiModelClientAgentFailed to change AiModel CoordinatorSwitch, error: invalid parameter.", new Object[0]);
            return false;
        } else if (this.context.getPackageName().equals(coordinatorSwitch.getPackageName())) {
            return true;
        } else {
            DSLog.e("AiModelClientAgentFailed to change AiModel CoordinatorSwitch, error: permission denied.", new Object[0]);
            return false;
        }
    }

    public boolean subscribe(ModelObserver observer) {
        return this.dsProxy.subscribe(AiModel.class, ObserverType.OBSERVER_MODEL, observer);
    }

    public boolean unSubscribe(ModelObserver observer) {
        return this.dsProxy.unSubscribe(AiModel.class, ObserverType.OBSERVER_MODEL, observer);
    }

    public boolean subscribe(AiModelObserver observer) {
        return this.dsProxy.subscribe(AiModel.class, ObserverType.OBSERVER_RECORD, observer);
    }

    public boolean unSubscribe(AiModelObserver observer) {
        return this.dsProxy.unSubscribe(AiModel.class, ObserverType.OBSERVER_RECORD, observer);
    }

    public DataServiceProxy getDataServiceProxy() {
        return this.dsProxy;
    }

    public void updatePackageCheck(List<ResourceInformation> resources, final UpdatePackageCheckCallBack cb) {
        if (cb == null) {
            DSLog.e("AiModelClientAgent Fail to update package check, error: UpdatePackageCheckCallBack is null.", new Object[0]);
            return;
        }
        this.dsProxy.updatePackageCheckAgent(resources, new UpdatePackageCheckCallBackAgent() {
            public void onFinish(ObjectContainer oc, int type) {
                NetworkType networkType = AiModelClientAgent.this.transferNetworkType(type);
                if (oc == null || oc.get() == null) {
                    DSLog.e("AiModelClientAgent Fail to update package check, error: Response is empty.", new Object[0]);
                    cb.onFinish(new ArrayList(), networkType);
                    return;
                }
                cb.onFinish(oc.get(), networkType);
            }
        });
    }

    public void updatePackage(List<ResourceInformation> resources, final UpdatePackageCallBack cb, long refreshInterval, long refreshBucketSize, boolean wifiOnly) {
        if (cb == null) {
            DSLog.e("AiModelClientAgent  Fail to update package, error: UpdatePackageCallBack is null.", new Object[0]);
            return;
        }
        UpdatePackageCallBackAgent agentCallBack = new UpdatePackageCallBackAgent() {
            public void onRefresh(int status, long totalSize, long downloadedSize, int totalPackages, int downloadedPackages, int errorCode, String errorMessage) {
                cb.onRefresh(AiModelClientAgent.this.transferUpdateStatus(status), totalSize, downloadedSize, totalPackages, downloadedPackages, errorCode, errorMessage);
            }
        };
        cb.onRefresh(UpdateStatus.BEGIN, 0, 0, 0, 0, 0, "");
        this.dsProxy.updatePackageAgent(resources, agentCallBack, refreshInterval, refreshBucketSize, wifiOnly);
    }

    public void deleteResInfo(List<ResourceInformation> resources, DeleteResInfoCallBack cb) {
        if (cb == null) {
            DSLog.e("AiModelClientAgent Fail to delet resourceinfo, error: DeleteResInfoCallBack is null.", new Object[0]);
        } else {
            this.dsProxy.deleteResInfoAgent(resources, new _DeleteResInfoCallBackAgent(cb));
        }
    }

    private NetworkType transferNetworkType(int type) {
        if (type == NetworkType.WIFI.ordinal()) {
            return NetworkType.WIFI;
        }
        if (type == NetworkType.CELLUAR.ordinal()) {
            return NetworkType.CELLUAR;
        }
        return NetworkType.NONE;
    }

    private UpdateStatus transferUpdateStatus(int status) {
        if (status == UpdateStatus.BEGIN.ordinal()) {
            return UpdateStatus.BEGIN;
        }
        if (status == UpdateStatus.SUCCESS.ordinal()) {
            return UpdateStatus.SUCCESS;
        }
        if (status == UpdateStatus.ONGOING.ordinal()) {
            return UpdateStatus.ONGOING;
        }
        return UpdateStatus.FAILURE;
    }

    private boolean isResInfoValid(ResourceInformation resInfo) {
        if (resInfo == null) {
            DSLog.e("AiModelClientAgent ResourceInformation is invalid, error: resInfo is empty.", new Object[0]);
            return false;
        }
        boolean isResInfoValid = true;
        if (TextUtils.isEmpty(resInfo.getResid())) {
            DSLog.e("AiModelClientAgent ResourceInformation is invalid, error: resid is empty.", new Object[0]);
            isResInfoValid = false;
        }
        if (TextUtils.isEmpty(resInfo.getXpu())) {
            DSLog.e("AiModelClientAgent ResourceInformation is invalid, error: resid is empty.", new Object[0]);
            isResInfoValid = false;
        }
        if (TextUtils.isEmpty(resInfo.getAbTest())) {
            DSLog.e("AiModelClientAgent ResourceInformation is invalid, error: abTest is empty.", new Object[0]);
            isResInfoValid = false;
        }
        if (resInfo.getAppVersion() != null) {
            return isResInfoValid;
        }
        DSLog.e("AiModelClientAgent ResourceInformation is invalid, error: appVersion is empty.", new Object[0]);
        return false;
    }

    private boolean presetResInfo(ResourceInformation resInfo) {
        if (resInfo == null) {
            DSLog.e("AiModelClientAgent ResourceInformation is invalid, error: resInfo is empty.", new Object[0]);
            return false;
        }
        resInfo.setIsPreset(Integer.valueOf(0));
        resInfo.setIsExtended(Integer.valueOf(1));
        resInfo.setFileSize(Long.valueOf(0));
        resInfo.setVersionCode(Long.valueOf(0));
        resInfo.setVersionName("");
        resInfo.setEmuiFamily("");
        resInfo.setProductFamily("");
        resInfo.setChipsetVendor("");
        resInfo.setChipset("");
        resInfo.setProduct("");
        resInfo.setProductModel("");
        resInfo.setDistrict("");
        resInfo.setSupportedAppVersion("");
        resInfo.setInterfaceVersion("");
        resInfo.setLatestTimestamp(Long.valueOf(0));
        String packageName = this.context.getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            DSLog.e("AiModelClientAgent Fail to preset ResourceInformation, error: package name is empty.", new Object[0]);
            return false;
        }
        resInfo.setPackageName(packageName);
        return true;
    }
}
