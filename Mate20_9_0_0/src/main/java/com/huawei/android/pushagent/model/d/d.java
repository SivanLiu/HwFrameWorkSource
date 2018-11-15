package com.huawei.android.pushagent.model.d;

import android.content.Context;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;

public class d {
    public static b zn(Context context, int i, PushDataReqMessage pushDataReqMessage) {
        switch (i) {
            case 0:
                return new e(context, pushDataReqMessage);
            case 1:
                return new f(context, pushDataReqMessage);
            case 2:
                return new c(context, pushDataReqMessage);
            default:
                return null;
        }
    }
}
