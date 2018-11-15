package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.a.a;
import java.io.InputStream;

public class HeartBeatRspMessage extends PushMessage {
    private static final long serialVersionUID = 210693033513730317L;

    public static byte zv() {
        return (byte) -37;
    }

    public HeartBeatRspMessage() {
        super(zv());
    }

    public PushMessage yw(InputStream inputStream) {
        return this;
    }

    public byte[] yp() {
        return new byte[]{this.mCmdId};
    }

    public String toString() {
        return new StringBuffer("HeartBeatRspMessage[").append("cmdId:").append(a.v(this.mCmdId)).append("]").toString();
    }
}
