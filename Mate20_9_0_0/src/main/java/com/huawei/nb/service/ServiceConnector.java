package com.huawei.nb.service;

import android.os.IInterface;

public abstract class ServiceConnector<I extends IInterface> {
    private I remoteService;

    public void setRemoteService(I remoteService) {
        if (remoteService != null) {
            this.remoteService = remoteService;
        }
    }

    public void unsetRemoteService() {
        this.remoteService = null;
    }

    public I getRemoteService() {
        return this.remoteService;
    }
}
