package com.huawei.nb.client.callback;

import com.huawei.nb.callback.IDeleteResInfoCallBack.Stub;

public abstract class DeleteResInfoCallBackAgent extends Stub {
    public abstract void onFailure(int i, String str);

    public abstract void onSuccess();
}
