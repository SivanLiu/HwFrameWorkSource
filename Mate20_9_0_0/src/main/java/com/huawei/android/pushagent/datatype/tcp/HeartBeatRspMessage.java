package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.f.b;
import java.io.InputStream;

public class HeartBeatRspMessage extends PushMessage {
    private static final long serialVersionUID = 210693033513730317L;

    public static byte jl() {
        return (byte) -37;
    }

    public HeartBeatRspMessage() {
        super(jl());
    }

    public PushMessage jc(InputStream inputStream) {
        return this;
    }

    public byte[] is() {
        return new byte[]{this.mCmdId};
    }

    public String toString() {
        return new StringBuffer("HeartBeatRspMessage[").append("cmdId:").append(b.em(this.mCmdId)).append("]").toString();
    }
}
