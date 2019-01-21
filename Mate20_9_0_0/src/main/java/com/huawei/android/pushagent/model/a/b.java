package com.huawei.android.pushagent.model.a;

import android.content.Context;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;

public class b {
    public static e fb(Context context, int i, PushDataReqMessage pushDataReqMessage) {
        switch (i) {
            case 0:
                return new d(context, pushDataReqMessage);
            case 1:
                return new c(context, pushDataReqMessage);
            case 2:
                return new f(context, pushDataReqMessage);
            default:
                return null;
        }
    }
}
