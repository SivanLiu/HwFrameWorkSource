package com.huawei.android.pushagent.datatype.tcp.base;

import com.huawei.android.pushagent.datatype.tcp.DecoupledPushMessage;
import com.huawei.android.pushagent.datatype.tcp.DeviceRegisterReqMessage;
import com.huawei.android.pushagent.datatype.tcp.DeviceRegisterRspMessage;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatReqMessage;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatRspMessage;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.datatype.tcp.PushDataRspMessage;
import com.huawei.android.pushagent.utils.a.b;
import java.io.InputStream;
import java.util.HashMap;

public class a {
    private static HashMap<Byte, Class> hw = new HashMap();

    static {
        hw.put(Byte.valueOf((byte) -38), HeartBeatReqMessage.class);
        hw.put(Byte.valueOf((byte) -37), HeartBeatRspMessage.class);
        hw.put(Byte.valueOf((byte) 64), DeviceRegisterReqMessage.class);
        hw.put(Byte.valueOf((byte) 65), DeviceRegisterRspMessage.class);
        hw.put(Byte.valueOf((byte) 68), PushDataReqMessage.class);
        hw.put(Byte.valueOf((byte) 69), PushDataRspMessage.class);
        hw.put(Byte.valueOf((byte) 66), DecoupledPushMessage.class);
        hw.put(Byte.valueOf((byte) 67), DecoupledPushMessage.class);
    }

    public static PushMessage yo(byte b, InputStream inputStream) {
        if (hw.containsKey(Byte.valueOf(b))) {
            PushMessage pushMessage = (PushMessage) ((Class) hw.get(Byte.valueOf(b))).newInstance();
            if (pushMessage.yq() == (byte) -1) {
                pushMessage.yv(b);
            }
            PushMessage yw = pushMessage.yw(inputStream);
            if (yw != null) {
                b.x("PushLog2976", "after decode msg:" + com.huawei.android.pushagent.utils.a.a.v(yw.yq()));
            } else {
                b.y("PushLog2976", "call " + pushMessage.getClass().getSimpleName() + " decode failed!");
            }
            return yw;
        }
        b.y("PushLog2976", "cmdId:" + b + " is not exist, all:" + hw.keySet());
        throw new InstantiationException("cmdId:" + b + " is not register");
    }
}
