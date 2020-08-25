package com.huawei.nb.client.callback;

import com.huawei.nb.client.ai.UpdateStatus;

public interface UpdatePackageControllableCallBack extends UpdatePackageCallBack {
    int onControllableRefresh(UpdateStatus updateStatus, long j, long j2, int i, int i2, int i3, String str);

    @Override // com.huawei.nb.client.callback.UpdatePackageCallBack
    default void onRefresh(UpdateStatus updateStatus, long totalSize, long downloadedSize, int totalPackages, int downloadedPackages, int errorCode, String errorMessage) {
    }
}
