package com.huawei.android.pushagent.model.b;

import android.content.Context;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;

public class a {
    public static d rd(Context context, int i, PushDataReqMessage pushDataReqMessage) {
        switch (i) {
            case 0:
                return new c(context, pushDataReqMessage);
            case 1:
                return new b(context, pushDataReqMessage);
            case 2:
                return new e(context, pushDataReqMessage);
            default:
                return null;
        }
    }
}
