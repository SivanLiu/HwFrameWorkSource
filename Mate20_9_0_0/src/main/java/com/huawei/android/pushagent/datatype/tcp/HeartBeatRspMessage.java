package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.b.c;
import java.io.InputStream;

public class HeartBeatRspMessage extends PushMessage {
    private static final long serialVersionUID = 210693033513730317L;

    public static byte ag() {
        return (byte) -37;
    }

    public HeartBeatRspMessage() {
        super(ag());
    }

    public PushMessage a(InputStream inputStream) {
        return this;
    }

    public byte[] b() {
        return new byte[]{this.mCmdId};
    }

    public String toString() {
        return new StringBuffer("HeartBeatRspMessage[").append("cmdId:").append(c.ts(this.mCmdId)).append("]").toString();
    }
}
