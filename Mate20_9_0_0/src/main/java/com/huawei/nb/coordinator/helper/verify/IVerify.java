package com.huawei.nb.coordinator.helper.verify;

import android.content.Context;
import com.huawei.nb.coordinator.helper.http.HttpRequest.Builder;

public interface IVerify extends IVerifyVar {
    boolean generateAuthorization(Context context, Builder builder, String str) throws VerifyException;

    String verifyTokenHeader();
}
