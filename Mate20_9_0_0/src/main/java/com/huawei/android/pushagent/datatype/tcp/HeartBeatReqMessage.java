package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import java.io.InputStream;

public class HeartBeatReqMessage extends PushMessage {
    private static final long serialVersionUID = 135034166096684602L;
    private byte nextHeartBeatInterval = (byte) 10;

    public HeartBeatReqMessage() {
        super(v());
    }

    private static byte v() {
        return (byte) -38;
    }

    public void u(byte b) {
        this.nextHeartBeatInterval = b;
    }

    public byte[] b() {
        return new byte[]{c(), this.nextHeartBeatInterval};
    }

    public PushMessage a(InputStream inputStream) {
        return null;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(d()).append(" NextHeartBeatInterval:").append(this.nextHeartBeatInterval).toString();
    }
}
