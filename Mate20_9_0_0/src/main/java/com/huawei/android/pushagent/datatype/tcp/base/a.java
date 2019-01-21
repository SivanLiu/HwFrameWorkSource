package com.huawei.android.pushagent.datatype.tcp.base;

import com.huawei.android.pushagent.datatype.tcp.DecoupledPushMessage;
import com.huawei.android.pushagent.datatype.tcp.DeviceRegisterReqMessage;
import com.huawei.android.pushagent.datatype.tcp.DeviceRegisterRspMessage;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatReqMessage;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatRspMessage;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.datatype.tcp.PushDataRspMessage;
import com.huawei.android.pushagent.utils.b.c;
import java.io.InputStream;
import java.util.HashMap;

public class a {
    private static HashMap<Byte, Class> a = new HashMap();

    static {
        a.put(Byte.valueOf((byte) -38), HeartBeatReqMessage.class);
        a.put(Byte.valueOf((byte) -37), HeartBeatRspMessage.class);
        a.put(Byte.valueOf((byte) 64), DeviceRegisterReqMessage.class);
        a.put(Byte.valueOf((byte) 65), DeviceRegisterRspMessage.class);
        a.put(Byte.valueOf((byte) 68), PushDataReqMessage.class);
        a.put(Byte.valueOf((byte) 69), PushDataRspMessage.class);
        a.put(Byte.valueOf((byte) 66), DecoupledPushMessage.class);
        a.put(Byte.valueOf((byte) 67), DecoupledPushMessage.class);
    }

    public static PushMessage l(byte b, InputStream inputStream) {
        if (a.containsKey(Byte.valueOf(b))) {
            PushMessage pushMessage = (PushMessage) ((Class) a.get(Byte.valueOf(b))).newInstance();
            if (pushMessage.c() == (byte) -1) {
                pushMessage.j(b);
            }
            PushMessage a = pushMessage.a(inputStream);
            if (a != null) {
                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "after decode msg:" + c.ts(a.c()));
            } else {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "call " + pushMessage.getClass().getSimpleName() + " decode failed!");
            }
            return a;
        }
        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "cmdId:" + b + " is not exist, all:" + a.keySet());
        throw new InstantiationException("cmdId:" + b + " is not register");
    }
}
