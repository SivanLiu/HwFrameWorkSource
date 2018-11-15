package com.huawei.nb.client.callback;

import com.huawei.nb.callback.IUpdatePackageCallBack.Stub;

public abstract class UpdatePackageCallBackAgent extends Stub {
    public abstract void onRefresh(int i, long j, long j2, int i2, int i3, int i4, String str);
}
