package com.android.server.camera;

import com.huawei.annotation.HwSystemApi;

@HwSystemApi
public interface IHwCameraServiceProxy {
    void binderDied();

    void notifyCameraStateChange(String str, int i, int i2, String str2);
}
