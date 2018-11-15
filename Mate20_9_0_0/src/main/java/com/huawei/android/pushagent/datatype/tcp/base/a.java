package com.huawei.android.pushagent.datatype.tcp.base;

import com.huawei.android.pushagent.datatype.tcp.DecoupledPushMessage;
import com.huawei.android.pushagent.datatype.tcp.DeviceRegisterReqMessage;
import com.huawei.android.pushagent.datatype.tcp.DeviceRegisterRspMessage;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatReqMessage;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatRspMessage;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.datatype.tcp.PushDataRspMessage;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.f.c;
import java.io.InputStream;
import java.util.HashMap;

public class a {
    private static HashMap<Byte, Class> bz = new HashMap();

    static {
        bz.put(Byte.valueOf((byte) -38), HeartBeatReqMessage.class);
        bz.put(Byte.valueOf((byte) -37), HeartBeatRspMessage.class);
        bz.put(Byte.valueOf((byte) 64), DeviceRegisterReqMessage.class);
        bz.put(Byte.valueOf((byte) 65), DeviceRegisterRspMessage.class);
        bz.put(Byte.valueOf((byte) 68), PushDataReqMessage.class);
        bz.put(Byte.valueOf((byte) 69), PushDataRspMessage.class);
        bz.put(Byte.valueOf((byte) 66), DecoupledPushMessage.class);
        bz.put(Byte.valueOf((byte) 67), DecoupledPushMessage.class);
    }

    public static PushMessage iv(byte b, InputStream inputStream) {
        if (bz.containsKey(Byte.valueOf(b))) {
            PushMessage pushMessage = (PushMessage) ((Class) bz.get(Byte.valueOf(b))).newInstance();
            if (pushMessage.it() == (byte) -1) {
                pushMessage.jb(b);
            }
            PushMessage jc = pushMessage.jc(inputStream);
            if (jc != null) {
                c.er("PushLog3413", "after decode msg:" + b.em(jc.it()));
            } else {
                c.eq("PushLog3413", "call " + pushMessage.getClass().getSimpleName() + " decode failed!");
            }
            return jc;
        }
        c.eq("PushLog3413", "cmdId:" + b + " is not exist, all:" + bz.keySet());
        throw new InstantiationException("cmdId:" + b + " is not register");
    }
}
